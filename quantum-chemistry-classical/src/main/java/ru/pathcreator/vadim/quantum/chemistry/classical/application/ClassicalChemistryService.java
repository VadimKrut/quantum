/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationPlan;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationStep;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticSet;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticTarget;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalBackendProfile;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalCapability;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalPreflightResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalPreflightStatus;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.resource.ClassicalResourceEstimate;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind;

/**
 * Основной application service для проверки, планирования и оценки classical chemistry задач.
 */
public final class ClassicalChemistryService {

  private final ClassicalSubjectAnalyzer subjectAnalyzer;
  private final ClassicalResourceEstimator resourceEstimator;

  public ClassicalChemistryService() {
    this(
        new ClassicalSubjectAnalyzer(),
        new ClassicalResourceEstimator());
  }

  public ClassicalChemistryService(
      final ClassicalSubjectAnalyzer subjectAnalyzer,
      final ClassicalResourceEstimator resourceEstimator
  ) {
    if (subjectAnalyzer == null) {
      throw new IllegalArgumentException("Classical chemistry subject analyzer must not be null.");
    }
    if (resourceEstimator == null) {
      throw new IllegalArgumentException("Classical chemistry resource estimator must not be null.");
    }
    this.subjectAnalyzer = subjectAnalyzer;
    this.resourceEstimator = resourceEstimator;
  }

  public ClassicalPreflightResult preflight(
      final ClassicalCalculationRequest request,
      final ClassicalBackendProfile profile
  ) {
    if (request == null) {
      throw new IllegalArgumentException("Classical preflight request must not be null.");
    }
    if (profile == null) {
      throw new IllegalArgumentException("Classical preflight profile must not be null.");
    }
    final ClassicalSubjectSize size = this.subjectAnalyzer.analyze(request.subject());
    final ArrayList<ClassicalDiagnostic> diagnostics = new ArrayList<ClassicalDiagnostic>();
    this.validateSubject(request, profile, size, diagnostics);
    this.validateCalculationKinds(request, profile, diagnostics);
    this.validateMethod(request, profile, diagnostics);
    this.validateBasisSet(request, profile, diagnostics);
    this.validateActiveSpace(request, profile, diagnostics);
    this.validateGeometry(request, profile, size, diagnostics);
    final ClassicalDiagnosticSet diagnosticSet = ClassicalDiagnosticSet.of(diagnostics);
    return ClassicalPreflightResult.of(
        this.status(diagnosticSet),
        profile,
        diagnosticSet);
  }

  public ClassicalCalculationPlan plan(
      final ClassicalCalculationRequest request,
      final ClassicalBackendProfile profile
  ) {
    final ClassicalPreflightResult preflightResult = this.preflight(request, profile);
    final ClassicalSubjectSize size = this.subjectAnalyzer.analyze(request.subject());
    final ClassicalResourceEstimate totalEstimate = this.resourceEstimator.estimate(request, size);
    final ArrayList<ClassicalCalculationStep> steps = new ArrayList<ClassicalCalculationStep>();
    final List<ClassicalCalculationKind> kinds = request.calculationKinds();
    for (int i = 0; i < kinds.size(); ++i) {
      final ClassicalCalculationKind kind = kinds.get(i);
      steps.add(
          ClassicalCalculationStep.of(
              i,
              kind,
              this.stepName(kind),
              this.requiredCapabilities(kind),
              totalEstimate));
    }
    return ClassicalCalculationPlan.of(
        request,
        preflightResult,
        steps,
        totalEstimate);
  }

  public ClassicalSubjectSize analyzeSubject(final ClassicalCalculationRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Classical subject analysis request must not be null.");
    }
    return this.subjectAnalyzer.analyze(request.subject());
  }

  public ClassicalResourceEstimate estimateResources(final ClassicalCalculationRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Classical resource estimate request must not be null.");
    }
    return this.resourceEstimator.estimate(
        request,
        this.subjectAnalyzer.analyze(request.subject()));
  }

  private void validateSubject(
      final ClassicalCalculationRequest request,
      final ClassicalBackendProfile profile,
      final ClassicalSubjectSize size,
      final List<ClassicalDiagnostic> diagnostics
  ) {
    if (!profile.supportsSubjectKind(request.subject().subjectKind())) {
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.SUBJECT_KIND_UNSUPPORTED,
              ClassicalDiagnosticTarget.of(
                  "subject",
                  request.subject().stableId()),
              "Backend profile does not support this chemistry subject kind."));
    }
    if (size.atomCount() > profile.maxAtoms()) {
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.MOLECULE_TOO_LARGE,
              ClassicalDiagnosticTarget.of(
                  "subject",
                  request.subject().stableId()),
              "Subject atom count exceeds backend profile limit."));
    }
    if (size.bondCount() > profile.maxBonds()) {
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.MOLECULE_TOO_LARGE,
              ClassicalDiagnosticTarget.of(
                  "subject",
                  request.subject().stableId()),
              "Subject bond count exceeds backend profile limit."));
    }
    if (size.electronCount() > profile.maxElectrons()) {
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.ELECTRON_COUNT_TOO_LARGE,
              ClassicalDiagnosticTarget.of(
                  "subject",
                  request.subject().stableId()),
              "Subject electron count exceeds backend profile limit."));
    }
    if (request.subject().subjectKind() == ChemistrySubjectKind.REACTION
        && size.participantCount() > profile.maxAtoms()) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.REACTION_TOO_LARGE,
              ClassicalDiagnosticTarget.of(
                  "subject",
                  request.subject().stableId()),
              "Reaction has many participants for this backend profile."));
    }
  }

  private void validateCalculationKinds(
      final ClassicalCalculationRequest request,
      final ClassicalBackendProfile profile,
      final List<ClassicalDiagnostic> diagnostics
  ) {
    final List<ClassicalCalculationKind> kinds = request.calculationKinds();
    for (int i = 0; i < kinds.size(); ++i) {
      final ClassicalCalculationKind kind = kinds.get(i);
      if (profile.supportsCalculationKind(kind)) {
        continue;
      }
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.CALCULATION_KIND_UNSUPPORTED,
              ClassicalDiagnosticTarget.of(
                  "calculationKind",
                  kind.name()),
              "Backend profile does not support requested calculation kind."));
    }
  }

  private void validateMethod(
      final ClassicalCalculationRequest request,
      final ClassicalBackendProfile profile,
      final List<ClassicalDiagnostic> diagnostics
  ) {
    if (profile.supportsMethodType(request.method().type())) {
      return;
    }
    diagnostics.add(
        ClassicalDiagnostic.error(
            ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED,
            ClassicalDiagnosticTarget.of(
                "method",
                request.method().type().name()),
            "Backend profile does not support requested electronic method type."));
  }

  private void validateBasisSet(
      final ClassicalCalculationRequest request,
      final ClassicalBackendProfile profile,
      final List<ClassicalDiagnostic> diagnostics
  ) {
    if (profile.supportsBasisSet(request.basisSet().name().value())) {
      return;
    }
    diagnostics.add(
        ClassicalDiagnostic.error(
            ClassicalDiagnosticCode.BASIS_SET_UNSUPPORTED,
            ClassicalDiagnosticTarget.of(
                "basisSet",
                request.basisSet().name().value()),
            "Backend profile does not support requested basis set."));
  }

  private void validateActiveSpace(
      final ClassicalCalculationRequest request,
      final ClassicalBackendProfile profile,
      final List<ClassicalDiagnostic> diagnostics
  ) {
    if (!request.hasActiveSpace()) {
      if (this.requiresActiveSpace(request)) {
        diagnostics.add(
            ClassicalDiagnostic.error(
                ClassicalDiagnosticCode.ACTIVE_SPACE_REQUIRED,
                ClassicalDiagnosticTarget.REQUEST,
                "Requested quantum-preparation calculation requires active space."));
      }
      return;
    }
    if (!profile.hasCapability(ClassicalCapability.ACTIVE_SPACE)) {
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.ACTIVE_SPACE_UNSUPPORTED,
              ClassicalDiagnosticTarget.REQUEST,
              "Backend profile does not support active-space calculations."));
    }
    if ((long) request.activeSpace().orbitalCount() > profile.maxActiveOrbitals()) {
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.ACTIVE_SPACE_UNSUPPORTED,
              ClassicalDiagnosticTarget.REQUEST,
              "Active-space orbital count exceeds backend profile limit."));
    }
  }

  private void validateGeometry(
      final ClassicalCalculationRequest request,
      final ClassicalBackendProfile profile,
      final ClassicalSubjectSize size,
      final List<ClassicalDiagnostic> diagnostics
  ) {
    if (!this.requiresGeometry(request)) {
      return;
    }
    if (!size.completeGeometry()) {
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.GEOMETRY_REQUIRED,
              ClassicalDiagnosticTarget.of(
                  "subject",
                  request.subject().stableId()),
              "Requested calculation requires complete 3D atom coordinates."));
    }
    if (!profile.hasCapability(ClassicalCapability.THREE_DIMENSIONAL_GEOMETRY)) {
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.GEOMETRY_REQUIRED,
              ClassicalDiagnosticTarget.of(
                  "profile",
                  profile.id()),
              "Backend profile does not support three-dimensional geometry."));
    }
  }

  private ClassicalPreflightStatus status(final ClassicalDiagnosticSet diagnostics) {
    if (diagnostics.hasErrors()) {
      return ClassicalPreflightStatus.UNSUPPORTED;
    }
    if (diagnostics.hasWarnings()) {
      return ClassicalPreflightStatus.NEEDS_APPROXIMATION;
    }
    return ClassicalPreflightStatus.SUPPORTED;
  }

  private boolean requiresGeometry(final ClassicalCalculationRequest request) {
    final List<ClassicalCalculationKind> kinds = request.calculationKinds();
    for (int i = 0; i < kinds.size(); ++i) {
      final ClassicalCalculationKind kind = kinds.get(i);
      if (kind == ClassicalCalculationKind.GEOMETRY_OPTIMIZATION
          || kind == ClassicalCalculationKind.FREQUENCY_ANALYSIS
          || kind == ClassicalCalculationKind.CONFORMER_SEARCH
          || kind == ClassicalCalculationKind.REACTION_PATH
          || kind == ClassicalCalculationKind.TRANSITION_STATE_SEARCH) {
        return true;
      }
    }
    return false;
  }

  private boolean requiresActiveSpace(final ClassicalCalculationRequest request) {
    final List<ClassicalCalculationKind> kinds = request.calculationKinds();
    for (int i = 0; i < kinds.size(); ++i) {
      final ClassicalCalculationKind kind = kinds.get(i);
      if (kind == ClassicalCalculationKind.ELECTRONIC_STRUCTURE_PREPARATION
          || kind == ClassicalCalculationKind.HAMILTONIAN_PREPARATION) {
        return true;
      }
    }
    return false;
  }

  private String stepName(final ClassicalCalculationKind kind) {
    switch (kind) {
      case SINGLE_POINT_ENERGY:
        return "Single-point energy";
      case GEOMETRY_OPTIMIZATION:
        return "Geometry optimization";
      case FREQUENCY_ANALYSIS:
        return "Frequency analysis";
      case THERMODYNAMIC_ANALYSIS:
        return "Thermodynamic analysis";
      case KINETIC_ANALYSIS:
        return "Kinetic analysis";
      case SPECTROSCOPY_PREDICTION:
        return "Spectroscopy prediction";
      case DESCRIPTOR_ANALYSIS:
        return "Descriptor analysis";
      case CONFORMER_SEARCH:
        return "Conformer search";
      case REACTION_ENERGY:
        return "Reaction energy";
      case REACTION_PATH:
        return "Reaction path";
      case TRANSITION_STATE_SEARCH:
        return "Transition-state search";
      case ELECTRONIC_STRUCTURE_PREPARATION:
        return "Electronic-structure preparation";
      case HAMILTONIAN_PREPARATION:
        return "Hamiltonian preparation";
      default:
        return "Classical calculation";
    }
  }

  private List<ClassicalCapability> requiredCapabilities(final ClassicalCalculationKind kind) {
    final ArrayList<ClassicalCapability> capabilities = new ArrayList<ClassicalCapability>();
    if (kind == ClassicalCalculationKind.GEOMETRY_OPTIMIZATION
        || kind == ClassicalCalculationKind.FREQUENCY_ANALYSIS
        || kind == ClassicalCalculationKind.CONFORMER_SEARCH
        || kind == ClassicalCalculationKind.REACTION_PATH
        || kind == ClassicalCalculationKind.TRANSITION_STATE_SEARCH) {
      capabilities.add(ClassicalCapability.THREE_DIMENSIONAL_GEOMETRY);
    }
    if (kind == ClassicalCalculationKind.GEOMETRY_OPTIMIZATION
        || kind == ClassicalCalculationKind.REACTION_PATH
        || kind == ClassicalCalculationKind.TRANSITION_STATE_SEARCH) {
      capabilities.add(ClassicalCapability.GEOMETRY_GRADIENT);
    }
    if (kind == ClassicalCalculationKind.FREQUENCY_ANALYSIS
        || kind == ClassicalCalculationKind.TRANSITION_STATE_SEARCH) {
      capabilities.add(ClassicalCapability.HESSIAN);
    }
    if (kind == ClassicalCalculationKind.HAMILTONIAN_PREPARATION
        || kind == ClassicalCalculationKind.ELECTRONIC_STRUCTURE_PREPARATION) {
      capabilities.add(ClassicalCapability.ACTIVE_SPACE);
      capabilities.add(ClassicalCapability.ELECTRONIC_HAMILTONIAN);
    }
    if (kind == ClassicalCalculationKind.SPECTROSCOPY_PREDICTION) {
      capabilities.add(ClassicalCapability.SPECTROSCOPY);
    }
    if (kind == ClassicalCalculationKind.KINETIC_ANALYSIS) {
      capabilities.add(ClassicalCapability.KINETICS);
    }
    if (kind == ClassicalCalculationKind.THERMODYNAMIC_ANALYSIS) {
      capabilities.add(ClassicalCapability.THERMODYNAMICS);
    }
    return capabilities;
  }
}
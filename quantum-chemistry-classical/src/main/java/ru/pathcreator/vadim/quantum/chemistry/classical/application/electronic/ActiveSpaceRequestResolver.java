/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticTarget;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** Подбирает active space из Java-native orbital spectrum и возвращает заявку, пригодную для Hamiltonian/VQE. */
public final class ActiveSpaceRequestResolver {

  public static final String ACTIVE_SPACE_FIRST_ORBITAL_OPTION = "active_space_first_orbital";

  private final ElectronicStructurePreparationPipeline preparationPipeline;
  private final ActiveSpaceSelector selector;

  public ActiveSpaceRequestResolver() {
    this(new ElectronicStructurePreparationPipeline(), new ActiveSpaceSelector());
  }

  public ActiveSpaceRequestResolver(
      final ElectronicStructurePreparationPipeline preparationPipeline,
      final ActiveSpaceSelector selector) {
    if (preparationPipeline == null) {
      throw new IllegalArgumentException(
          "Active-space request resolver pipeline must not be null.");
    }
    if (selector == null) {
      throw new IllegalArgumentException(
          "Active-space request resolver selector must not be null.");
    }
    this.preparationPipeline = preparationPipeline;
    this.selector = selector;
  }

  public ResolvedActiveSpaceRequest resolve(
      final ClassicalCalculationRequest request,
      final List<ClassicalDiagnostic> diagnostics,
      final String calculationKind) {
    if (request == null) {
      throw new IllegalArgumentException("Active-space request resolver request must not be null.");
    }
    if (diagnostics == null) {
      throw new IllegalArgumentException(
          "Active-space request resolver diagnostics target must not be null.");
    }
    if (request.hasActiveSpace()) {
      final ActiveSpace activeSpace = request.activeSpace();
      final int firstOrbital =
          ActiveSpaceRequestResolver.integerOption(
              request.options(), ACTIVE_SPACE_FIRST_ORBITAL_OPTION, 0);
      final ActiveSpaceSelectionResult explicitSelection =
          ActiveSpaceRequestResolver.explicitSelection(activeSpace, firstOrbital);
      return ResolvedActiveSpaceRequest.of(
          ActiveSpaceRequestResolver.withActiveSpace(request, activeSpace, firstOrbital),
          explicitSelection,
          false);
    }
    if (request.subject().subjectKind() != ChemistrySubjectKind.MOLECULE) {
      throw new IllegalArgumentException(
          "Automatic active-space selection requires molecule subject.");
    }
    final ElectronicStructurePreparationResult preparationResult =
        this.referencePreparation(request, diagnostics, calculationKind);
    final double[] orbitalEnergies = ActiveSpaceRequestResolver.orbitalEnergies(preparationResult);
    final ActiveSpaceSelectionResult selection =
        this.selectActiveSpace(
            preparationResult,
            orbitalEnergies,
            ActiveSpaceSelectionOptions.fromOptions(request.options()));
    diagnostics.add(
        ClassicalDiagnostic.info(
            ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
            ClassicalDiagnosticTarget.of("calculationKind", calculationKind),
            "Active space was selected automatically from Java-native orbital energies."));
    return ResolvedActiveSpaceRequest.of(
        ActiveSpaceRequestResolver.withActiveSpace(
            request, selection.activeSpace(), selection.firstActiveOrbital()),
        selection,
        true);
  }

  private ElectronicStructurePreparationResult referencePreparation(
      final ClassicalCalculationRequest request,
      final List<ClassicalDiagnostic> diagnostics,
      final String calculationKind) {
    final String unsupportedReason = this.preparationPipeline.unsupportedReason(request);
    if (ActiveSpaceRequestResolver.requiresReferenceOnlyPreparation(request)) {
      return this.referenceOnlyPreparation(
          request, diagnostics, calculationKind, unsupportedReason);
    }
    if (unsupportedReason == null) {
      return this.preparationPipeline.prepare(request);
    }
    if (this.preparationPipeline.oneElectronUnsupportedReason(request) != null) {
      throw new IllegalArgumentException(unsupportedReason);
    }
    diagnostics.add(
        ClassicalDiagnostic.info(
            ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
            ClassicalDiagnosticTarget.of("calculationKind", calculationKind),
            "Direct SCF orbital reference is used because full ERI tensor preparation is unavailable: "
                + unsupportedReason));
    final Molecule molecule = (Molecule) request.subject();
    if (molecule.electronicConfiguration().closedShell()) {
      return this.preparationPipeline.prepareDirectRestrictedHartreeFock(request);
    }
    return this.preparationPipeline.prepareDirectUnrestrictedHartreeFock(request);
  }

  private ElectronicStructurePreparationResult referenceOnlyPreparation(
      final ClassicalCalculationRequest request,
      final List<ClassicalDiagnostic> diagnostics,
      final String calculationKind,
      final String unsupportedReason) {
    if (unsupportedReason != null) {
      diagnostics.add(
          ClassicalDiagnostic.info(
              ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
              ClassicalDiagnosticTarget.of("calculationKind", calculationKind),
              "Reference SCF orbitals are used for active-space selection because the requested method "
                  + request.method().name().value()
                  + " requires an active space before it can run: "
                  + unsupportedReason));
    } else {
      diagnostics.add(
          ClassicalDiagnostic.info(
              ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
              ClassicalDiagnosticTarget.of("calculationKind", calculationKind),
              "Reference SCF orbitals are used for active-space selection before running "
                  + request.method().name().value()
                  + "."));
    }
    final Molecule molecule = (Molecule) request.subject();
    if (molecule.electronicConfiguration().closedShell()) {
      return this.preparationPipeline.prepareDirectRestrictedHartreeFock(request);
    }
    return this.preparationPipeline.prepareDirectUnrestrictedHartreeFock(request);
  }

  private static boolean requiresReferenceOnlyPreparation(
      final ClassicalCalculationRequest request) {
    final ElectronicStructureMethodType type = request.method().type();
    return type == ElectronicStructureMethodType.MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD
        || type == ElectronicStructureMethodType.FULL_CONFIGURATION_INTERACTION
        || type == ElectronicStructureMethodType.QUANTUM_VARIATIONAL
        || type == ElectronicStructureMethodType.QUANTUM_PHASE_ESTIMATION;
  }

  private ActiveSpaceSelectionResult selectActiveSpace(
      final ElectronicStructurePreparationResult preparationResult,
      final double[] orbitalEnergies,
      final ActiveSpaceSelectionOptions options) {
    if (preparationResult.hasUhfReference()) {
      return this.selector.selectSpinResolved(
          preparationResult.electronCount(),
          orbitalEnergies,
          preparationResult.uhfResult().alphaElectronCount(),
          preparationResult.uhfResult().betaElectronCount(),
          options);
    }
    if (preparationResult.hasRohfReference()) {
      return this.selector.selectSpinResolved(
          preparationResult.electronCount(),
          orbitalEnergies,
          preparationResult.rohfResult().alphaElectronCount(),
          preparationResult.rohfResult().betaElectronCount(),
          options);
    }
    if (preparationResult.hasKohnShamDftReference()
        && preparationResult.kohnShamDftResult().unrestricted()) {
      return this.selector.selectSpinResolved(
          preparationResult.electronCount(),
          orbitalEnergies,
          preparationResult.kohnShamDftResult().alphaElectronCount(),
          preparationResult.kohnShamDftResult().betaElectronCount(),
          options);
    }
    return this.selector.select(preparationResult.electronCount(), orbitalEnergies, options);
  }

  static double[] orbitalEnergies(final ElectronicStructurePreparationResult preparationResult) {
    if (preparationResult.hasRhfReference()) {
      return preparationResult.rhfResult().orbitalEnergiesCopy();
    }
    if (preparationResult.hasRohfReference()) {
      return preparationResult.rohfResult().orbitalEnergiesCopy();
    }
    if (preparationResult.hasKohnShamDftReference()) {
      if (preparationResult.kohnShamDftResult().unrestricted()) {
        return ActiveSpaceRequestResolver.averageSpinOrbitalEnergies(
            preparationResult.kohnShamDftResult().alphaOrbitalEnergiesCopy(),
            preparationResult.kohnShamDftResult().betaOrbitalEnergiesCopy());
      }
      return preparationResult.kohnShamDftResult().orbitalEnergiesCopy();
    }
    if (preparationResult.hasUhfReference()) {
      return ActiveSpaceRequestResolver.averageSpinOrbitalEnergies(
          preparationResult.uhfResult().alphaOrbitalEnergiesCopy(),
          preparationResult.uhfResult().betaOrbitalEnergiesCopy());
    }
    throw new IllegalArgumentException(
        "Active-space selection requires RHF, ROHF, UHF or Kohn-Sham DFT orbital energies.");
  }

  private static double[] averageSpinOrbitalEnergies(final double[] alpha, final double[] beta) {
    final int count = Math.min(alpha.length, beta.length);
    final double[] averaged = new double[count];
    for (int orbitalIndex = 0; orbitalIndex < count; ++orbitalIndex) {
      averaged[orbitalIndex] = (alpha[orbitalIndex] + beta[orbitalIndex]) * 0.5;
    }
    return averaged;
  }

  private static ActiveSpaceSelectionResult explicitSelection(
      final ActiveSpace activeSpace, final int firstOrbital) {
    return ActiveSpaceSelectionResult.of(
        activeSpace,
        firstOrbital,
        firstOrbital + activeSpace.orbitalCount(),
        Math.max(0, firstOrbital + Math.max(0, activeSpace.electronCount() / 2) - 1),
        firstOrbital + Math.max(0, activeSpace.electronCount() / 2),
        0.0,
        0.0,
        0.0,
        Math.max(1, activeSpace.electronCount() / 2),
        Math.max(1, activeSpace.orbitalCount() - Math.max(1, activeSpace.electronCount() / 2)),
        1.0,
        1.0,
        1.0,
        "EXPLICIT",
        List.of("Active space was supplied explicitly."),
        List.of());
  }

  private static ClassicalCalculationRequest withActiveSpace(
      final ClassicalCalculationRequest request,
      final ActiveSpace activeSpace,
      final int firstActiveOrbital) {
    return ClassicalCalculationRequest.of(
        request.id(),
        request.subject(),
        request.method(),
        request.basisSet(),
        activeSpace,
        request.calculationKinds(),
        ActiveSpaceRequestResolver.withIntegerOption(
            request.options(), ACTIVE_SPACE_FIRST_ORBITAL_OPTION, firstActiveOrbital),
        request.metadata());
  }

  private static List<ClassicalCalculationOption> withIntegerOption(
      final List<ClassicalCalculationOption> options, final String name, final int value) {
    final ArrayList<ClassicalCalculationOption> result =
        new ArrayList<ClassicalCalculationOption>();
    if (options != null) {
      for (int optionIndex = 0; optionIndex < options.size(); ++optionIndex) {
        final ClassicalCalculationOption option = options.get(optionIndex);
        if (!name.equals(option.name())) {
          result.add(option);
        }
      }
    }
    result.add(ClassicalCalculationOption.integer(name, value));
    return List.copyOf(result);
  }

  private static int integerOption(
      final List<ClassicalCalculationOption> options, final String name, final int defaultValue) {
    if (options == null) {
      return defaultValue;
    }
    for (int optionIndex = 0; optionIndex < options.size(); ++optionIndex) {
      final ClassicalCalculationOption option = options.get(optionIndex);
      if (name.equals(option.name())) {
        return Integer.parseInt(option.value());
      }
    }
    return defaultValue;
  }
}

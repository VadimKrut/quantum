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
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffectAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffectProfile;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioEvaluationPlan;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioEvaluator;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioPlan;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.coverage.ClassicalCalculationCoverageAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ElectronicStructurePreparationPipeline;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.feature.MolecularFeatureAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.OrganicReactionCandidateScorer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.OrganicReactionPredictionService;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionPredictionCalibrationApplier;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionPredictionElectronicReranker;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.RuleBasedOrganicReactionCandidateGenerator;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationPlan;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationStep;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.coverage.ClassicalCalculationCoverageReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticSet;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticTarget;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalBackendProfile;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalCapability;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalPreflightResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalPreflightStatus;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.resource.ClassicalResourceEstimate;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.BuiltInOrganicReactionRuleLibraries;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.BuiltInReactionOutcomeCalibrationLibraries;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodType;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionResult;

/**
 * Application service для проверки, планирования и выполнения classical chemistry задач.
 */
public final class ClassicalChemistryService {

  private static final String SOLVENT_OPTION = "solvent_model";
  private static final String SOLVENT_SHORT_OPTION = "solvent";
  private static final String PERIODIC_OPTION = "periodic_boundary_conditions";
  private static final String PERIODIC_SHORT_OPTION = "periodic";
  private static final String RELATIVISTIC_OPTION = "relativistic";
  private static final String RELATIVISTIC_MODEL_OPTION = "relativistic_model";
  private static final String SINGLE_POINT_BACKEND_OPTION = "single_point_backend";
  private static final String GEOMETRY_GRADIENT_BACKEND_OPTION = "geometry_gradient_backend";
  private static final String GRADIENT_BACKEND_OPTION = "gradient_backend";
  private static final String GEOMETRY_OPTIMIZATION_BACKEND_OPTION =
      "geometry_optimization_backend";
  private static final String OPTIMIZATION_BACKEND_OPTION = "optimization_backend";
  private static final String HESSIAN_BACKEND_OPTION = "hessian_backend";
  private static final String FREQUENCY_BACKEND_OPTION = "frequency_backend";
  private static final String VIBRATIONAL_BACKEND_OPTION = "vibrational_backend";
  private static final String EXCITED_STATE_BACKEND_OPTION = "excited_state_backend";
  private static final String SPECTROSCOPY_BACKEND_OPTION = "spectroscopy_backend";
  private static final String PROPERTY_BACKEND_OPTION = "property_backend";
  private static final String MOLECULAR_PROPERTY_BACKEND_OPTION = "molecular_property_backend";
  private static final String DIPOLE_BACKEND_OPTION = "dipole_backend";
  private static final String CHARGE_BACKEND_OPTION = "charge_backend";
  private static final String REACTION_PREDICTION_APPLY_CALIBRATION_PRIORS_OPTION =
      "reaction_prediction_apply_calibration_priors";
  private static final String ADVANCED_PREDICTION_APPLY_CALIBRATION_PRIORS_OPTION =
      "advanced_prediction_apply_calibration_priors";
  private static final String PYSCF_BACKEND = "pyscf";

  private final ClassicalSubjectAnalyzer subjectAnalyzer;
  private final ClassicalResourceEstimator resourceEstimator;
  private final ClassicalCalculationExecutor calculationExecutor;
  private final OrganicReactionPredictionService reactionPredictionService;
  private final ReactionPredictionElectronicReranker reactionPredictionElectronicReranker;
  private final ReactionPredictionCalibrationApplier reactionPredictionCalibrationApplier;
  private final ReactionConditionScenarioEvaluator conditionScenarioEvaluator;
  private final ReactionConditionEffectAnalyzer conditionEffectAnalyzer;
  private final ClassicalCalculationCoverageAnalyzer coverageAnalyzer;

  public ClassicalChemistryService() {
    this(new ElectronicStructurePreparationPipeline());
  }

  public ClassicalChemistryService(
      final ElectronicStructurePreparationPipeline preparationPipeline) {
    this(
        new ClassicalSubjectAnalyzer(),
        new ClassicalResourceEstimator(),
        new ClassicalCalculationExecutor(preparationPipeline),
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.comprehensiveOrganicProductPrediction()),
            new OrganicReactionCandidateScorer(new MolecularFeatureAnalyzer())),
        new ReactionPredictionElectronicReranker(),
        new ReactionPredictionCalibrationApplier(
            BuiltInReactionOutcomeCalibrationLibraries.heterocycleProductDistributions()),
        new ReactionConditionScenarioEvaluator(),
        new ReactionConditionEffectAnalyzer(),
        new ClassicalCalculationCoverageAnalyzer(preparationPipeline));
  }

  public ClassicalChemistryService(
      final ClassicalSubjectAnalyzer subjectAnalyzer,
      final ClassicalResourceEstimator resourceEstimator,
      final ClassicalCalculationExecutor calculationExecutor,
      final OrganicReactionPredictionService reactionPredictionService,
      final ReactionPredictionElectronicReranker reactionPredictionElectronicReranker,
      final ReactionPredictionCalibrationApplier reactionPredictionCalibrationApplier,
      final ReactionConditionScenarioEvaluator conditionScenarioEvaluator,
      final ReactionConditionEffectAnalyzer conditionEffectAnalyzer,
      final ClassicalCalculationCoverageAnalyzer coverageAnalyzer) {
    if (subjectAnalyzer == null) {
      throw new IllegalArgumentException("Classical chemistry subject analyzer must not be null.");
    }
    if (resourceEstimator == null) {
      throw new IllegalArgumentException(
          "Classical chemistry resource estimator must not be null.");
    }
    if (calculationExecutor == null) {
      throw new IllegalArgumentException("Classical calculation executor must not be null.");
    }
    if (reactionPredictionService == null) {
      throw new IllegalArgumentException("Classical reaction prediction service must not be null.");
    }
    if (reactionPredictionElectronicReranker == null) {
      throw new IllegalArgumentException(
          "Reaction prediction electronic reranker must not be null.");
    }
    if (reactionPredictionCalibrationApplier == null) {
      throw new IllegalArgumentException(
          "Reaction prediction calibration applier must not be null.");
    }
    if (conditionScenarioEvaluator == null) {
      throw new IllegalArgumentException(
          "Classical condition scenario evaluator must not be null.");
    }
    if (conditionEffectAnalyzer == null) {
      throw new IllegalArgumentException("Classical condition effect analyzer must not be null.");
    }
    if (coverageAnalyzer == null) {
      throw new IllegalArgumentException("Classical coverage analyzer must not be null.");
    }
    this.subjectAnalyzer = subjectAnalyzer;
    this.resourceEstimator = resourceEstimator;
    this.calculationExecutor = calculationExecutor;
    this.reactionPredictionService = reactionPredictionService;
    this.reactionPredictionElectronicReranker = reactionPredictionElectronicReranker;
    this.reactionPredictionCalibrationApplier = reactionPredictionCalibrationApplier;
    this.conditionScenarioEvaluator = conditionScenarioEvaluator;
    this.conditionEffectAnalyzer = conditionEffectAnalyzer;
    this.coverageAnalyzer = coverageAnalyzer;
  }

  public ClassicalPreflightResult preflight(
      final ClassicalCalculationRequest request, final ClassicalBackendProfile profile) {
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
    this.validateReactionPredictionSubject(request, diagnostics);
    this.validateMethod(request, profile, diagnostics);
    this.validateBasisSet(request, profile, diagnostics);
    this.validateActiveSpace(request, profile, diagnostics);
    this.validateGeometry(request, profile, size, diagnostics);
    this.validateCapabilityCoverage(request, profile, diagnostics);
    this.validateRequestedOptions(request, profile, diagnostics);
    this.validateDftAccuracy(request, profile, diagnostics);
    diagnostics.addAll(this.coverageAnalyzer.analyze(request, profile).diagnostics().diagnostics());
    final ClassicalDiagnosticSet diagnosticSet = ClassicalDiagnosticSet.of(diagnostics);
    return ClassicalPreflightResult.of(this.status(diagnosticSet), profile, diagnosticSet);
  }

  public ClassicalCalculationPlan plan(
      final ClassicalCalculationRequest request, final ClassicalBackendProfile profile) {
    final ClassicalPreflightResult preflightResult = this.preflight(request, profile);
    final ClassicalSubjectSize size = this.subjectAnalyzer.analyze(request.subject());
    final ClassicalResourceEstimate totalEstimate = this.resourceEstimator.estimate(request, size);
    final ArrayList<ClassicalCalculationStep> steps = new ArrayList<ClassicalCalculationStep>();
    final List<ClassicalCalculationKind> kinds = request.calculationKinds();
    for (int i = 0; i < kinds.size(); ++i) {
      final ClassicalCalculationKind kind = kinds.get(i);
      steps.add(
          ClassicalCalculationStep.of(
              i, kind, this.stepName(kind), this.requiredCapabilities(kind), totalEstimate));
    }
    return ClassicalCalculationPlan.of(request, preflightResult, steps, totalEstimate);
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
        request, this.subjectAnalyzer.analyze(request.subject()));
  }

  public ClassicalCalculationCoverageReport analyzeCoverage(
      final ClassicalCalculationRequest request, final ClassicalBackendProfile profile) {
    if (request == null) {
      throw new IllegalArgumentException("Classical coverage request must not be null.");
    }
    if (profile == null) {
      throw new IllegalArgumentException("Classical coverage profile must not be null.");
    }
    return this.coverageAnalyzer.analyze(request, profile);
  }

  public ClassicalCalculationResult execute(final ClassicalCalculationRequest request) {
    if (request == null) {
      throw new IllegalArgumentException(
          "Classical calculation execution request must not be null.");
    }
    return this.calculationExecutor.execute(request);
  }

  public ReactionPredictionResult predictReactionProducts(final ReactionPredictionRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction prediction request must not be null.");
    }
    return this.reactionPredictionCalibrationApplier.apply(
        this.reactionPredictionService.predict(request), false);
  }

  public ReactionPredictionResult predictReactionProducts(
      final ClassicalCalculationRequest calculationRequest,
      final ReactionPredictionRequest request,
      final List<ClassicalDiagnostic> diagnostics) {
    if (calculationRequest == null) {
      throw new IllegalArgumentException(
          "Reaction prediction calculation request must not be null.");
    }
    if (request == null) {
      throw new IllegalArgumentException("Reaction prediction request must not be null.");
    }
    if (diagnostics == null) {
      throw new IllegalArgumentException("Reaction prediction diagnostics must not be null.");
    }
    final ReactionPredictionResult predicted = this.reactionPredictionService.predict(request);
    final ReactionPredictionResult reranked =
        this.reactionPredictionElectronicReranker.rerank(
            calculationRequest, predicted, diagnostics);
    return this.reactionPredictionCalibrationApplier.apply(
        reranked, ClassicalChemistryService.calibrationPriorsEnabled(calculationRequest));
  }

  public ReactionConditionEffectProfile analyzeReactionConditionEffects(
      final ReactionPredictionRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction prediction request must not be null.");
    }
    return this.conditionEffectAnalyzer.analyze(request);
  }

  public ReactionConditionScenarioPlan planReactionConditionScenarios(
      final ReactionPredictionRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction prediction request must not be null.");
    }
    return this.conditionScenarioEvaluator.plan(request);
  }

  public ReactionConditionScenarioEvaluationPlan evaluateReactionConditionScenarios(
      final ReactionPredictionRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction prediction request must not be null.");
    }
    return this.conditionScenarioEvaluator.evaluate(request);
  }

  public ReactionConditionScenarioEvaluationPlan evaluateReactionConditionScenarios(
      final ReactionConditionScenarioPlan plan, final ReactionPredictionRequest request) {
    return this.conditionScenarioEvaluator.evaluate(plan, request);
  }

  private void validateSubject(
      final ClassicalCalculationRequest request,
      final ClassicalBackendProfile profile,
      final ClassicalSubjectSize size,
      final List<ClassicalDiagnostic> diagnostics) {
    if (!profile.supportsSubjectKind(request.subject().subjectKind())) {
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.SUBJECT_KIND_UNSUPPORTED,
              ClassicalDiagnosticTarget.of("subject", request.subject().stableId()),
              "Backend profile does not support this chemistry subject kind."));
    }
    if (size.atomCount() > profile.maxAtoms()) {
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.MOLECULE_TOO_LARGE,
              ClassicalDiagnosticTarget.of("subject", request.subject().stableId()),
              "Subject atom count exceeds backend profile limit."));
    }
    if (size.bondCount() > profile.maxBonds()) {
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.MOLECULE_TOO_LARGE,
              ClassicalDiagnosticTarget.of("subject", request.subject().stableId()),
              "Subject bond count exceeds backend profile limit."));
    }
    if (size.electronCount() > profile.maxElectrons()) {
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.ELECTRON_COUNT_TOO_LARGE,
              ClassicalDiagnosticTarget.of("subject", request.subject().stableId()),
              "Subject electron count exceeds backend profile limit."));
    }
    if (request.subject().subjectKind() == ChemistrySubjectKind.REACTION
        && size.participantCount() > profile.maxParticipants()) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.REACTION_TOO_LARGE,
              ClassicalDiagnosticTarget.of("subject", request.subject().stableId()),
              "Reaction has many participants for this backend profile."));
    }
  }

  private void validateCalculationKinds(
      final ClassicalCalculationRequest request,
      final ClassicalBackendProfile profile,
      final List<ClassicalDiagnostic> diagnostics) {
    final List<ClassicalCalculationKind> kinds = request.calculationKinds();
    for (int i = 0; i < kinds.size(); ++i) {
      final ClassicalCalculationKind kind = kinds.get(i);
      if (profile.supportsCalculationKind(kind)) {
        continue;
      }
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.CALCULATION_KIND_UNSUPPORTED,
              ClassicalDiagnosticTarget.of("calculationKind", kind.name()),
              "Backend profile does not support requested calculation kind."));
    }
  }

  private void validateReactionPredictionSubject(
      final ClassicalCalculationRequest request, final List<ClassicalDiagnostic> diagnostics) {
    final List<ClassicalCalculationKind> kinds = request.calculationKinds();
    for (int i = 0; i < kinds.size(); ++i) {
      if (kinds.get(i) != ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION) {
        continue;
      }
      if (request.subject().subjectKind() == ChemistrySubjectKind.REACTION) {
        return;
      }
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.PREDICTION_REQUIRES_REACTION_SUBJECT,
              ClassicalDiagnosticTarget.of("subject", request.subject().stableId()),
              "Reaction product prediction requires reaction subject."));
      return;
    }
  }

  private void validateMethod(
      final ClassicalCalculationRequest request,
      final ClassicalBackendProfile profile,
      final List<ClassicalDiagnostic> diagnostics) {
    if (profile.supportsMethodType(request.method().type())) {
      return;
    }
    if (request.method().type() == ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY
        && this.pyScfBackendRequested(request)) {
      return;
    }
    diagnostics.add(
        ClassicalDiagnostic.error(
            ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED,
            ClassicalDiagnosticTarget.of("method", request.method().type().name()),
            "Backend profile does not support requested electronic method type."));
  }

  private void validateBasisSet(
      final ClassicalCalculationRequest request,
      final ClassicalBackendProfile profile,
      final List<ClassicalDiagnostic> diagnostics) {
    if (request.basisSet().hasStructuredDefinition() || request.basisSet().hasDefinitionText()) {
      return;
    }
    if (profile.supportsBasisSet(request.basisSet().name().value())) {
      return;
    }
    diagnostics.add(
        ClassicalDiagnostic.error(
            ClassicalDiagnosticCode.BASIS_SET_UNSUPPORTED,
            ClassicalDiagnosticTarget.of("basisSet", request.basisSet().name().value()),
            "Backend profile does not support requested basis set."));
  }

  private void validateActiveSpace(
      final ClassicalCalculationRequest request,
      final ClassicalBackendProfile profile,
      final List<ClassicalDiagnostic> diagnostics) {
    if (!request.hasActiveSpace()) {
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
      final List<ClassicalDiagnostic> diagnostics) {
    if (!this.requiresGeometry(request)) {
      return;
    }
    if (!size.completeGeometry()) {
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.GEOMETRY_REQUIRED,
              ClassicalDiagnosticTarget.of("subject", request.subject().stableId()),
              "Requested calculation requires complete 3D atom coordinates."));
    }
    if (!profile.hasCapability(ClassicalCapability.THREE_DIMENSIONAL_GEOMETRY)) {
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.GEOMETRY_REQUIRED,
              ClassicalDiagnosticTarget.of("profile", profile.id()),
              "Backend profile does not support three-dimensional geometry."));
    }
  }

  private void validateCapabilityCoverage(
      final ClassicalCalculationRequest request,
      final ClassicalBackendProfile profile,
      final List<ClassicalDiagnostic> diagnostics) {
    final List<ClassicalCalculationKind> kinds = request.calculationKinds();
    for (int kindIndex = 0; kindIndex < kinds.size(); ++kindIndex) {
      final ClassicalCalculationKind kind = kinds.get(kindIndex);
      final List<ClassicalCapability> capabilities = this.requiredCapabilities(kind);
      for (int capabilityIndex = 0; capabilityIndex < capabilities.size(); ++capabilityIndex) {
        final ClassicalCapability capability = capabilities.get(capabilityIndex);
        if (profile.hasCapability(capability)) {
          continue;
        }
        diagnostics.add(
            ClassicalDiagnostic.error(
                this.missingCapabilityCode(capability),
                ClassicalDiagnosticTarget.of("capability", capability.name()),
                "Backend profile does not provide capability required by " + kind.name() + "."));
      }
    }
  }

  private void validateRequestedOptions(
      final ClassicalCalculationRequest request,
      final ClassicalBackendProfile profile,
      final List<ClassicalDiagnostic> diagnostics) {
    if (this.hasTextOption(request, ClassicalChemistryService.SOLVENT_OPTION)
        || this.hasTextOption(request, ClassicalChemistryService.SOLVENT_SHORT_OPTION)) {
      this.requireCapability(
          profile,
          ClassicalCapability.SOLVENT_MODEL,
          ClassicalDiagnosticCode.SOLVENT_MODEL_UNSUPPORTED,
          "solvent",
          "Backend profile does not support explicit solvent model.",
          diagnostics);
    }
    if (this.optionEnabled(request, ClassicalChemistryService.PERIODIC_OPTION)
        || this.optionEnabled(request, ClassicalChemistryService.PERIODIC_SHORT_OPTION)) {
      this.requireCapability(
          profile,
          ClassicalCapability.PERIODIC_BOUNDARY_CONDITIONS,
          ClassicalDiagnosticCode.PERIODIC_MODEL_UNSUPPORTED,
          "periodic",
          "Backend profile does not support periodic boundary conditions.",
          diagnostics);
    }
    if (this.optionEnabled(request, ClassicalChemistryService.RELATIVISTIC_OPTION)
        || this.hasTextOption(request, ClassicalChemistryService.RELATIVISTIC_MODEL_OPTION)) {
      this.requireCapability(
          profile,
          ClassicalCapability.RELATIVISTIC_EFFECTS,
          ClassicalDiagnosticCode.RELATIVISTIC_MODEL_UNSUPPORTED,
          "relativistic",
          "Backend profile does not support relativistic effects.",
          diagnostics);
    }
  }

  private void validateDftAccuracy(
      final ClassicalCalculationRequest request,
      final ClassicalBackendProfile profile,
      final List<ClassicalDiagnostic> diagnostics) {
    if (request.method().type() != ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY) {
      return;
    }
    if (this.pyScfBackendRequested(request)) {
      return;
    }
    if (profile.hasCapability(ClassicalCapability.SELF_CONSISTENT_DENSITY_FUNCTIONAL_THEORY)) {
      return;
    }
    diagnostics.add(
        ClassicalDiagnostic.error(
            ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED,
            ClassicalDiagnosticTarget.of("method", request.method().name().value()),
            "Backend profile does not provide self-consistent Kohn-Sham DFT capability."));
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
          || kind == ClassicalCalculationKind.GEOMETRY_GRADIENT
          || kind == ClassicalCalculationKind.FREQUENCY_ANALYSIS
          || kind == ClassicalCalculationKind.CONFORMER_SEARCH
          || kind == ClassicalCalculationKind.PERIODIC_BOUNDARY_ANALYSIS
          || kind == ClassicalCalculationKind.REACTION_PATH
          || kind == ClassicalCalculationKind.TRANSITION_STATE_SEARCH) {
        return true;
      }
      if (kind == ClassicalCalculationKind.MOLECULAR_PROPERTY_ANALYSIS
          && this.pyScfBackendRequested(request)) {
        return true;
      }
    }
    return false;
  }

  private ClassicalDiagnosticCode missingCapabilityCode(final ClassicalCapability capability) {
    if (capability == ClassicalCapability.THREE_DIMENSIONAL_GEOMETRY) {
      return ClassicalDiagnosticCode.GEOMETRY_REQUIRED;
    }
    if (capability == ClassicalCapability.GEOMETRY_GRADIENT) {
      return ClassicalDiagnosticCode.GRADIENT_REQUIRED;
    }
    if (capability == ClassicalCapability.HESSIAN) {
      return ClassicalDiagnosticCode.HESSIAN_REQUIRED;
    }
    if (capability == ClassicalCapability.SOLVENT_MODEL) {
      return ClassicalDiagnosticCode.SOLVENT_MODEL_UNSUPPORTED;
    }
    if (capability == ClassicalCapability.PERIODIC_BOUNDARY_CONDITIONS) {
      return ClassicalDiagnosticCode.PERIODIC_MODEL_UNSUPPORTED;
    }
    if (capability == ClassicalCapability.RELATIVISTIC_EFFECTS) {
      return ClassicalDiagnosticCode.RELATIVISTIC_MODEL_UNSUPPORTED;
    }
    if (capability == ClassicalCapability.ACTIVE_SPACE) {
      return ClassicalDiagnosticCode.ACTIVE_SPACE_UNSUPPORTED;
    }
    if (capability == ClassicalCapability.ACTIVE_SPACE_SELECTION) {
      return ClassicalDiagnosticCode.ACTIVE_SPACE_UNSUPPORTED;
    }
    if (capability == ClassicalCapability.QUANTUM_PROGRAM_PREPARATION) {
      return ClassicalDiagnosticCode.CALCULATION_KIND_UNSUPPORTED;
    }
    return ClassicalDiagnosticCode.CALCULATION_KIND_UNSUPPORTED;
  }

  private void requireCapability(
      final ClassicalBackendProfile profile,
      final ClassicalCapability capability,
      final ClassicalDiagnosticCode code,
      final String targetName,
      final String message,
      final List<ClassicalDiagnostic> diagnostics) {
    if (profile.hasCapability(capability)) {
      return;
    }
    diagnostics.add(
        ClassicalDiagnostic.error(
            code, ClassicalDiagnosticTarget.of(targetName, capability.name()), message));
  }

  private boolean optionEnabled(
      final ClassicalCalculationRequest request, final String optionName) {
    final List<ClassicalCalculationOption> options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (!option.name().equals(optionName)) {
        continue;
      }
      return Boolean.parseBoolean(option.value());
    }
    return false;
  }

  private static boolean calibrationPriorsEnabled(final ClassicalCalculationRequest request) {
    final List<ClassicalCalculationOption> options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (ClassicalChemistryService.REACTION_PREDICTION_APPLY_CALIBRATION_PRIORS_OPTION.equals(
              option.name())
          || ClassicalChemistryService.ADVANCED_PREDICTION_APPLY_CALIBRATION_PRIORS_OPTION.equals(
              option.name())) {
        return Boolean.parseBoolean(option.value());
      }
    }
    return false;
  }

  private boolean hasTextOption(
      final ClassicalCalculationRequest request, final String optionName) {
    final List<ClassicalCalculationOption> options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (!option.name().equals(optionName)) {
        continue;
      }
      return !option.value().isBlank();
    }
    return false;
  }

  private boolean pyScfSinglePointBackendRequested(final ClassicalCalculationRequest request) {
    final List<ClassicalCalculationOption> options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (!option.name().equals(ClassicalChemistryService.SINGLE_POINT_BACKEND_OPTION)) {
        continue;
      }
      return option.value().equalsIgnoreCase(ClassicalChemistryService.PYSCF_BACKEND);
    }
    return false;
  }

  private boolean pyScfBackendRequested(final ClassicalCalculationRequest request) {
    return this.pyScfSinglePointBackendRequested(request)
        || this.pyScfOptionRequested(
            request, ClassicalChemistryService.GEOMETRY_GRADIENT_BACKEND_OPTION)
        || this.pyScfOptionRequested(request, ClassicalChemistryService.GRADIENT_BACKEND_OPTION)
        || this.pyScfOptionRequested(
            request, ClassicalChemistryService.GEOMETRY_OPTIMIZATION_BACKEND_OPTION)
        || this.pyScfOptionRequested(request, ClassicalChemistryService.OPTIMIZATION_BACKEND_OPTION)
        || this.pyScfOptionRequested(request, ClassicalChemistryService.HESSIAN_BACKEND_OPTION)
        || this.pyScfOptionRequested(request, ClassicalChemistryService.FREQUENCY_BACKEND_OPTION)
        || this.pyScfOptionRequested(request, ClassicalChemistryService.VIBRATIONAL_BACKEND_OPTION)
        || this.pyScfOptionRequested(
            request, ClassicalChemistryService.EXCITED_STATE_BACKEND_OPTION)
        || this.pyScfOptionRequested(request, ClassicalChemistryService.SPECTROSCOPY_BACKEND_OPTION)
        || this.pyScfOptionRequested(request, ClassicalChemistryService.PROPERTY_BACKEND_OPTION)
        || this.pyScfOptionRequested(
            request, ClassicalChemistryService.MOLECULAR_PROPERTY_BACKEND_OPTION)
        || this.pyScfOptionRequested(request, ClassicalChemistryService.DIPOLE_BACKEND_OPTION)
        || this.pyScfOptionRequested(request, ClassicalChemistryService.CHARGE_BACKEND_OPTION);
  }

  private boolean pyScfOptionRequested(
      final ClassicalCalculationRequest request, final String optionName) {
    final List<ClassicalCalculationOption> options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (!option.name().equals(optionName)) {
        continue;
      }
      return option.value().equalsIgnoreCase(ClassicalChemistryService.PYSCF_BACKEND);
    }
    return false;
  }

  private String stepName(final ClassicalCalculationKind kind) {
    switch (kind) {
      case SINGLE_POINT_ENERGY:
        return "Single-point energy";
      case GEOMETRY_GRADIENT:
        return "Geometry gradient";
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
      case NMR_PREDICTION:
        return "NMR prediction";
      case IR_RAMAN_PREDICTION:
        return "IR/Raman prediction";
      case MASS_SPECTROMETRY_PREDICTION:
        return "Mass spectrometry prediction";
      case EXCITED_STATE_ANALYSIS:
        return "Excited-state analysis";
      case DESCRIPTOR_ANALYSIS:
        return "Descriptor analysis";
      case MOLECULAR_PROPERTY_ANALYSIS:
        return "Molecular property analysis";
      case CONFORMER_SEARCH:
        return "Conformer search";
      case STEREOCHEMICAL_OUTCOME_ANALYSIS:
        return "Stereochemical outcome analysis";
      case ENANTIOMER_DIASTEREOMER_ANALYSIS:
        return "Enantiomer/diastereomer analysis";
      case SOLVATION_MODELING:
        return "Solvation modeling";
      case PKA_PREDICTION:
        return "pKa prediction";
      case REDOX_POTENTIAL:
        return "Redox potential";
      case REACTION_PRODUCT_PREDICTION:
        return "Reaction product prediction";
      case REACTION_ENERGY:
        return "Reaction energy";
      case REACTION_PATH:
        return "Reaction path";
      case IRC_ANALYSIS:
        return "IRC analysis";
      case TRANSITION_STATE_SEARCH:
        return "Transition-state search";
      case SELECTIVITY_ANALYSIS:
        return "Selectivity analysis";
      case COMPETING_PATHWAY_ANALYSIS:
        return "Competing-pathway analysis";
      case REACTION_CONDITION_SCREENING:
        return "Reaction condition screening";
      case REACTION_CONDITION_OPTIMIZATION:
        return "Reaction condition optimization";
      case ACTIVE_SPACE_SELECTION:
        return "Active-space selection";
      case ORBITAL_OPTIMIZATION:
        return "Orbital optimization";
      case ELECTRONIC_STRUCTURE_PREPARATION:
        return "Electronic-structure preparation";
      case HAMILTONIAN_PREPARATION:
        return "Hamiltonian preparation";
      case QUANTUM_PROGRAM_PREPARATION:
        return "Quantum program preparation";
      case QUANTUM_VQE_OPTIMIZATION:
        return "Quantum VQE optimization";
      case RELATIVISTIC_CORRECTION:
        return "Relativistic correction";
      case PERIODIC_BOUNDARY_ANALYSIS:
        return "Periodic boundary analysis";
      case SOLVENT_SCREENING:
        return "Solvent screening";
      case CATALYST_SCREENING:
        return "Catalyst screening";
      case CONCENTRATION_SCREENING:
        return "Concentration screening";
      case BASIS_SET_CONVERGENCE:
        return "Basis-set convergence";
      case METHOD_BENCHMARK:
        return "Method benchmark";
      case UNCERTAINTY_ANALYSIS:
        return "Uncertainty analysis";
      case MECHANISM_ENUMERATION:
        return "Mechanism enumeration";
      case REACTION_HAZARD_AND_STABILITY_ANALYSIS:
        return "Reaction hazard and stability analysis";
      case PRODUCT_DISTRIBUTION_ANALYSIS:
        return "Product distribution analysis";
      case YIELD_WINDOW_ESTIMATION:
        return "Yield window estimation";
      case IMPURITY_AND_SIDE_PRODUCT_ANALYSIS:
        return "Impurity and side-product analysis";
      case SCALE_UP_RISK_ANALYSIS:
        return "Scale-up risk analysis";
      case QUENCH_AND_WORKUP_ANALYSIS:
        return "Quench and workup analysis";
      case REACTION_HISTORY_TIMELINE_ANALYSIS:
        return "Reaction history timeline analysis";
      case REACTION_NETWORK_ANALYSIS:
        return "Reaction network analysis";
      case MICROKINETIC_MODELING:
        return "Microkinetic modeling";
      case RETROSYNTHESIS_PLANNING:
        return "Retrosynthesis planning";
      case TRANSITION_STATE_ENSEMBLE_ANALYSIS:
        return "Transition-state ensemble analysis";
      default:
        return "Classical calculation";
    }
  }

  private List<ClassicalCapability> requiredCapabilities(final ClassicalCalculationKind kind) {
    final ArrayList<ClassicalCapability> capabilities = new ArrayList<ClassicalCapability>();
    if (kind == ClassicalCalculationKind.GEOMETRY_OPTIMIZATION
        || kind == ClassicalCalculationKind.GEOMETRY_GRADIENT
        || kind == ClassicalCalculationKind.FREQUENCY_ANALYSIS
        || kind == ClassicalCalculationKind.CONFORMER_SEARCH
        || kind == ClassicalCalculationKind.REACTION_PATH
        || kind == ClassicalCalculationKind.TRANSITION_STATE_SEARCH) {
      capabilities.add(ClassicalCapability.THREE_DIMENSIONAL_GEOMETRY);
    }
    if (kind == ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION
        || kind == ClassicalCalculationKind.REACTION_ENERGY
        || kind == ClassicalCalculationKind.REACTION_PATH
        || kind == ClassicalCalculationKind.TRANSITION_STATE_SEARCH
        || kind == ClassicalCalculationKind.IRC_ANALYSIS
        || kind == ClassicalCalculationKind.KINETIC_ANALYSIS
        || kind == ClassicalCalculationKind.SELECTIVITY_ANALYSIS
        || kind == ClassicalCalculationKind.COMPETING_PATHWAY_ANALYSIS
        || kind == ClassicalCalculationKind.REACTION_CONDITION_SCREENING
        || kind == ClassicalCalculationKind.REACTION_CONDITION_OPTIMIZATION
        || kind == ClassicalCalculationKind.SOLVENT_SCREENING
        || kind == ClassicalCalculationKind.CATALYST_SCREENING
        || kind == ClassicalCalculationKind.CONCENTRATION_SCREENING
        || kind == ClassicalCalculationKind.MECHANISM_ENUMERATION
        || kind == ClassicalCalculationKind.REACTION_HAZARD_AND_STABILITY_ANALYSIS
        || kind == ClassicalCalculationKind.PRODUCT_DISTRIBUTION_ANALYSIS
        || kind == ClassicalCalculationKind.YIELD_WINDOW_ESTIMATION
        || kind == ClassicalCalculationKind.IMPURITY_AND_SIDE_PRODUCT_ANALYSIS
        || kind == ClassicalCalculationKind.SCALE_UP_RISK_ANALYSIS
        || kind == ClassicalCalculationKind.QUENCH_AND_WORKUP_ANALYSIS
        || kind == ClassicalCalculationKind.REACTION_HISTORY_TIMELINE_ANALYSIS
        || kind == ClassicalCalculationKind.REACTION_NETWORK_ANALYSIS
        || kind == ClassicalCalculationKind.MICROKINETIC_MODELING
        || kind == ClassicalCalculationKind.RETROSYNTHESIS_PLANNING
        || kind == ClassicalCalculationKind.TRANSITION_STATE_ENSEMBLE_ANALYSIS) {
      capabilities.add(ClassicalCapability.ORGANIC_REACTION_PREDICTION);
      capabilities.add(ClassicalCapability.REACTION_INPUT);
    }
    if (kind == ClassicalCalculationKind.GEOMETRY_OPTIMIZATION
        || kind == ClassicalCalculationKind.GEOMETRY_GRADIENT
        || kind == ClassicalCalculationKind.REACTION_PATH
        || kind == ClassicalCalculationKind.TRANSITION_STATE_SEARCH) {
      capabilities.add(ClassicalCapability.GEOMETRY_GRADIENT);
    }
    if (kind == ClassicalCalculationKind.FREQUENCY_ANALYSIS
        || kind == ClassicalCalculationKind.TRANSITION_STATE_SEARCH) {
      capabilities.add(ClassicalCapability.HESSIAN);
    }
    if (kind == ClassicalCalculationKind.ELECTRONIC_STRUCTURE_PREPARATION
        || kind == ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION
        || kind == ClassicalCalculationKind.QUANTUM_VQE_OPTIMIZATION) {
      capabilities.add(ClassicalCapability.ACTIVE_SPACE);
    }
    if (kind == ClassicalCalculationKind.HAMILTONIAN_PREPARATION
        || kind == ClassicalCalculationKind.ELECTRONIC_STRUCTURE_PREPARATION
        || kind == ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION
        || kind == ClassicalCalculationKind.QUANTUM_VQE_OPTIMIZATION) {
      capabilities.add(ClassicalCapability.ELECTRONIC_HAMILTONIAN);
    }
    if (kind == ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION
        || kind == ClassicalCalculationKind.QUANTUM_VQE_OPTIMIZATION) {
      capabilities.add(ClassicalCapability.QUANTUM_PROGRAM_PREPARATION);
    }
    if (kind == ClassicalCalculationKind.QUANTUM_VQE_OPTIMIZATION) {
      capabilities.add(ClassicalCapability.QUANTUM_VQE_OPTIMIZATION);
    }
    if (kind == ClassicalCalculationKind.ACTIVE_SPACE_SELECTION) {
      capabilities.add(ClassicalCapability.ACTIVE_SPACE_SELECTION);
      capabilities.add(ClassicalCapability.ACTIVE_SPACE);
    }
    if (kind == ClassicalCalculationKind.ORBITAL_OPTIMIZATION) {
      capabilities.add(ClassicalCapability.ACTIVE_SPACE);
      capabilities.add(ClassicalCapability.ELECTRONIC_HAMILTONIAN);
    }
    if (kind == ClassicalCalculationKind.MP2_CORRELATION
        || kind == ClassicalCalculationKind.CISD_CORRELATION
        || kind == ClassicalCalculationKind.CCSD_CORRELATION
        || kind == ClassicalCalculationKind.FCI_DIAGONALIZATION
        || kind == ClassicalCalculationKind.POST_HF_CORRELATION) {
      capabilities.add(ClassicalCapability.POST_HF_CORRELATION);
      capabilities.add(ClassicalCapability.ELECTRONIC_HAMILTONIAN);
    }
    if (kind == ClassicalCalculationKind.SPECTROSCOPY_PREDICTION
        || kind == ClassicalCalculationKind.NMR_PREDICTION
        || kind == ClassicalCalculationKind.IR_RAMAN_PREDICTION
        || kind == ClassicalCalculationKind.MASS_SPECTROMETRY_PREDICTION
        || kind == ClassicalCalculationKind.EXCITED_STATE_ANALYSIS) {
      capabilities.add(ClassicalCapability.SPECTROSCOPY);
    }
    if (kind == ClassicalCalculationKind.DESCRIPTOR_ANALYSIS
        || kind == ClassicalCalculationKind.MOLECULAR_PROPERTY_ANALYSIS
        || kind == ClassicalCalculationKind.PKA_PREDICTION
        || kind == ClassicalCalculationKind.REDOX_POTENTIAL) {
      capabilities.add(ClassicalCapability.DESCRIPTOR_ANALYSIS);
    }
    if (kind == ClassicalCalculationKind.SOLVENT_SCREENING
        || kind == ClassicalCalculationKind.SOLVATION_MODELING) {
      capabilities.add(ClassicalCapability.SOLVENT_MODEL);
    }
    if (kind == ClassicalCalculationKind.KINETIC_ANALYSIS) {
      capabilities.add(ClassicalCapability.KINETICS);
    }
    if (kind == ClassicalCalculationKind.THERMODYNAMIC_ANALYSIS) {
      capabilities.add(ClassicalCapability.THERMODYNAMICS);
    }
    if (kind == ClassicalCalculationKind.CONFORMER_SEARCH) {
      capabilities.add(ClassicalCapability.CONFORMER_SEARCH);
    }
    if (kind == ClassicalCalculationKind.RELATIVISTIC_CORRECTION) {
      capabilities.add(ClassicalCapability.RELATIVISTIC_EFFECTS);
    }
    if (kind == ClassicalCalculationKind.PERIODIC_BOUNDARY_ANALYSIS) {
      capabilities.add(ClassicalCapability.PERIODIC_BOUNDARY_CONDITIONS);
      capabilities.add(ClassicalCapability.THREE_DIMENSIONAL_GEOMETRY);
    }
    return capabilities;
  }
}

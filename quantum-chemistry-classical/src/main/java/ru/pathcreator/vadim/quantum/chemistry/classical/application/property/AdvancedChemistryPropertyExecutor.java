/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.property;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioEvaluation;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioEvaluationPlan;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioEvaluator;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffectAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffectProfile;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.ClassicalSubjectAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.ClassicalSubjectSize;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.QuantumProgramPreparationExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.QuantumVqeOptimizationExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.feature.MolecularFeatureAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.OrganicReactionPredictionService;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionMechanismCatalogCoverageAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionPredictionApplicabilityAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionPredictionCalibrationApplier;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionPredictionElectronicReranker;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionOutcomeCalibrationCoverageAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.RuleBasedOrganicReactionCandidateGenerator;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.result.ClassicalResultQualityValues;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticTarget;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeature;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureSet;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism.ReactionMechanismCatalogCoverageReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeCalibrationCoverageReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeCalibrationLibrary;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.prediction.ReactionPredictionApplicabilityReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultSection;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValue;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValueKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.BuiltInOrganicReactionRuleLibraries;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.BuiltInReactionMechanismPatternCatalog;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.BuiltInReactionOutcomeCalibrationLibraries;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.composition.MolecularComposition;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionMechanismChannelCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionScoreComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionScoreComponentKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionId;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.StoichiometricCoefficient;

/**
 * Выполняет быстрые Java-native оценки свойств и маршрутов, которые нужны для сложных органических задач.
 */
public final class AdvancedChemistryPropertyExecutor {
  private static final String USE_PRODUCT_SIDE_AS_SEED_OPTION =
      "advanced_prediction_use_product_side_as_seed";
  private static final String ELECTRONIC_RERANK_OPTION = "advanced_prediction_electronic_rerank";
  private static final String APPLY_CALIBRATION_PRIORS_OPTION =
      "advanced_prediction_apply_calibration_priors";
  private static final String PREPARE_QUANTUM_WORKFLOWS_OPTION =
      "advanced_prediction_prepare_quantum_workflows";
  private static final String PREPARE_QUANTUM_WORKFLOWS_MAX_CANDIDATES_OPTION =
      "advanced_prediction_prepare_quantum_workflows_max_candidates";
  private static final String PREPARE_QUANTUM_WORKFLOWS_MAX_PARTICIPANTS_OPTION =
      "advanced_prediction_prepare_quantum_workflows_max_participants";
  private static final String EXECUTE_QUANTUM_WORKFLOW_VQE_OPTION =
      "advanced_prediction_execute_quantum_workflow_vqe";

  private final ClassicalSubjectAnalyzer subjectAnalyzer;
  private final MolecularFeatureAnalyzer featureAnalyzer;
  private final OrganicReactionPredictionService predictionService;
  private final ReactionPredictionElectronicReranker electronicReranker;
  private final ReactionPredictionCalibrationApplier calibrationApplier;
  private final ReactionOutcomeCalibrationLibrary calibrationLibrary;
  private final ReactionConditionScenarioEvaluator conditionScenarioEvaluator;
  private final ReactionConditionEffectAnalyzer conditionEffectAnalyzer;
  private final ReactionPredictionApplicabilityAnalyzer applicabilityAnalyzer;
  private final QuantumProgramPreparationExecutor quantumProgramPreparationExecutor;

  public AdvancedChemistryPropertyExecutor() {
    this(new ClassicalSubjectAnalyzer(), new MolecularFeatureAnalyzer());
  }

  public AdvancedChemistryPropertyExecutor(
      final ClassicalSubjectAnalyzer subjectAnalyzer,
      final MolecularFeatureAnalyzer featureAnalyzer) {
    this(subjectAnalyzer, featureAnalyzer, new QuantumProgramPreparationExecutor());
  }

  public AdvancedChemistryPropertyExecutor(
      final ClassicalSubjectAnalyzer subjectAnalyzer,
      final MolecularFeatureAnalyzer featureAnalyzer,
      final QuantumProgramPreparationExecutor quantumProgramPreparationExecutor) {
    if (subjectAnalyzer == null) {
      throw new IllegalArgumentException("Advanced property subject analyzer must not be null.");
    }
    if (featureAnalyzer == null) {
      throw new IllegalArgumentException("Advanced property feature analyzer must not be null.");
    }
    if (quantumProgramPreparationExecutor == null) {
      throw new IllegalArgumentException(
          "Advanced property quantum program preparation executor must not be null.");
    }
    this.subjectAnalyzer = subjectAnalyzer;
    this.featureAnalyzer = featureAnalyzer;
    this.predictionService =
        new OrganicReactionPredictionService(
            featureAnalyzer,
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.comprehensiveOrganicProductPrediction()));
    this.electronicReranker = new ReactionPredictionElectronicReranker();
    this.calibrationLibrary =
        BuiltInReactionOutcomeCalibrationLibraries.heterocycleProductDistributions();
    this.calibrationApplier = new ReactionPredictionCalibrationApplier(this.calibrationLibrary);
    this.conditionScenarioEvaluator =
        new ReactionConditionScenarioEvaluator(
            new ru.pathcreator.vadim.quantum.chemistry.classical.application.condition
                .ReactionConditionScenarioPlanner(),
            this.predictionService);
    this.conditionEffectAnalyzer = new ReactionConditionEffectAnalyzer();
    this.applicabilityAnalyzer = new ReactionPredictionApplicabilityAnalyzer(featureAnalyzer);
    this.quantumProgramPreparationExecutor = quantumProgramPreparationExecutor;
  }

  public ClassicalResultSection execute(
      final ClassicalCalculationRequest request,
      final ClassicalCalculationKind kind,
      final List<ClassicalDiagnostic> diagnostics) {
    if (request == null) {
      throw new IllegalArgumentException("Advanced property request must not be null.");
    }
    if (kind == null) {
      throw new IllegalArgumentException("Advanced property calculation kind must not be null.");
    }
    if (diagnostics == null) {
      throw new IllegalArgumentException("Advanced property diagnostics must not be null.");
    }
    diagnostics.add(
        ClassicalDiagnostic.warning(
            ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
            ClassicalDiagnosticTarget.of("calculationKind", kind.name()),
            "This Java-native calculation is a deterministic chemistry model; validate critical decisions with experimental or high-level backend data."));
    if (request.subject().subjectKind() == ChemistrySubjectKind.MOLECULE) {
      return this.moleculeSection((Molecule) request.subject(), kind);
    }
    if (request.subject().subjectKind() == ChemistrySubjectKind.REACTION) {
      return this.reactionSection(request, (Reaction) request.subject(), kind, diagnostics);
    }
    diagnostics.add(
        ClassicalDiagnostic.error(
            ClassicalDiagnosticCode.SUBJECT_KIND_UNSUPPORTED,
            ClassicalDiagnosticTarget.of("subject", request.subject().subjectKind().name()),
            "Advanced chemistry property calculation requires molecule or reaction subject."));
    return null;
  }

  public boolean supports(final ClassicalCalculationKind kind) {
    return kind == ClassicalCalculationKind.HESSIAN_ANALYSIS
        || kind == ClassicalCalculationKind.MOLECULAR_PROPERTY_ANALYSIS
        || kind == ClassicalCalculationKind.NMR_PREDICTION
        || kind == ClassicalCalculationKind.IR_RAMAN_PREDICTION
        || kind == ClassicalCalculationKind.MASS_SPECTROMETRY_PREDICTION
        || kind == ClassicalCalculationKind.EXCITED_STATE_ANALYSIS
        || kind == ClassicalCalculationKind.STEREOCHEMICAL_OUTCOME_ANALYSIS
        || kind == ClassicalCalculationKind.ENANTIOMER_DIASTEREOMER_ANALYSIS
        || kind == ClassicalCalculationKind.SOLVATION_MODELING
        || kind == ClassicalCalculationKind.PKA_PREDICTION
        || kind == ClassicalCalculationKind.REDOX_POTENTIAL
        || kind == ClassicalCalculationKind.IRC_ANALYSIS
        || kind == ClassicalCalculationKind.SELECTIVITY_ANALYSIS
        || kind == ClassicalCalculationKind.COMPETING_PATHWAY_ANALYSIS
        || kind == ClassicalCalculationKind.REACTION_CONDITION_SCREENING
        || kind == ClassicalCalculationKind.REACTION_CONDITION_OPTIMIZATION
        || kind == ClassicalCalculationKind.SOLVENT_SCREENING
        || kind == ClassicalCalculationKind.CATALYST_SCREENING
        || kind == ClassicalCalculationKind.CONCENTRATION_SCREENING
        || kind == ClassicalCalculationKind.BASIS_SET_CONVERGENCE
        || kind == ClassicalCalculationKind.METHOD_BENCHMARK
        || kind == ClassicalCalculationKind.UNCERTAINTY_ANALYSIS
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
        || kind == ClassicalCalculationKind.TRANSITION_STATE_ENSEMBLE_ANALYSIS
        || kind == ClassicalCalculationKind.RETROSYNTHESIS_PLANNING;
  }

  private ClassicalResultSection moleculeSection(
      final Molecule molecule, final ClassicalCalculationKind kind) {
    final MolecularComposition composition = molecule.composition();
    final MolecularFeatureSet features = this.featureAnalyzer.analyze(molecule);
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addApproximate(
        values,
        "java_native",
        this.modelName(kind),
        "molecule_descriptor_and_property_prediction",
        "Deterministic molecular descriptors and proxy models; not a full ab initio observable calculation.");
    values.add(ClassicalResultValue.text("model", this.modelName(kind)));
    values.add(ClassicalResultValue.scalar("atom_count", composition.atomCount(), "count"));
    values.add(
        ClassicalResultValue.scalar("heavy_atom_count", composition.heavyAtomCount(), "count"));
    values.add(
        ClassicalResultValue.scalar("hetero_atom_count", composition.heteroAtomCount(), "count"));
    values.add(ClassicalResultValue.scalar("formal_charge", molecule.charge().value(), "charge"));
    values.add(
        ClassicalResultValue.scalar(
            "spin_multiplicity", molecule.spinMultiplicity().value(), "count"));
    values.add(ClassicalResultValue.text("dominant_features", this.featureSummary(features)));
    this.addMoleculeKindValues(values, composition, molecule, features, kind);
    return ClassicalResultSection.of(kind, this.title(kind), values);
  }

  private ClassicalResultSection reactionSection(
      final ClassicalCalculationRequest request,
      final Reaction reaction,
      final ClassicalCalculationKind kind,
      final List<ClassicalDiagnostic> diagnostics) {
    final ClassicalSubjectSize size = this.subjectAnalyzer.analyze(reaction);
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addApproximate(
        values,
        "java_native",
        this.modelName(kind),
        "reaction_descriptor_mechanism_condition_prediction",
        "Mechanism, topology, condition and optional electronic-rerank model; not an experimental outcome lookup.");
    values.add(ClassicalResultValue.text("model", this.modelName(kind)));
    values.add(ClassicalResultValue.scalar("participant_count", size.participantCount(), "count"));
    values.add(ClassicalResultValue.scalar("atom_count", size.atomCount(), "count"));
    values.add(ClassicalResultValue.scalar("bond_count", size.bondCount(), "count"));
    values.add(ClassicalResultValue.scalar("electron_count", size.electronCount(), "count"));
    values.add(
        ClassicalResultValue.text(
            "atom_balanced", Boolean.toString(reaction.balance().atomBalanced())));
    values.add(
        ClassicalResultValue.text(
            "charge_balanced", Boolean.toString(reaction.balance().chargeBalanced())));
    this.addReactionKindValues(values, size, request, reaction, kind, diagnostics);
    return ClassicalResultSection.of(kind, this.title(kind), values);
  }

  private void addMoleculeKindValues(
      final List<ClassicalResultValue> values,
      final MolecularComposition composition,
      final Molecule molecule,
      final MolecularFeatureSet features,
      final ClassicalCalculationKind kind) {
    final double heteroRatio =
        composition.heavyAtomCount() == 0
            ? 0.0
            : (double) composition.heteroAtomCount() / (double) composition.heavyAtomCount();
    if (kind == ClassicalCalculationKind.SOLVATION_MODELING) {
      values.add(ClassicalResultValue.scalar("polarity_index", heteroRatio, "fraction"));
      values.add(
          ClassicalResultValue.scalar("estimated_solvation_penalty", heteroRatio * 12.5, "kJ/mol"));
      values.add(
          ClassicalResultValue.text(
              "solvent_sensitivity", heteroRatio > 0.25 ? "high" : "moderate"));
      return;
    }
    if (kind == ClassicalCalculationKind.MOLECULAR_PROPERTY_ANALYSIS) {
      values.add(
          ClassicalResultValue.scalar(
              "electron_count", molecule.electronicConfiguration().electronCount(), "count"));
      values.add(ClassicalResultValue.scalar("bond_count", molecule.bondCount(), "count"));
      values.add(ClassicalResultValue.scalar("polarity_proxy", heteroRatio, "fraction"));
      values.add(
          ClassicalResultValue.scalar(
              "frontier_complexity_proxy",
              Math.max(
                  1.0, composition.heavyAtomCount() + molecule.spinMultiplicity().value() - 1.0),
              "score"));
      values.add(
          ClassicalResultValue.of(
              "property_proxy_rows",
              ClassicalResultValueKind.TABLE,
              "",
              this.moleculePropertyProxyRows(composition, molecule, features)));
      return;
    }
    if (kind == ClassicalCalculationKind.PKA_PREDICTION) {
      values.add(ClassicalResultValue.scalar("acidic_site_score", heteroRatio * 10.0, "score"));
      values.add(
          ClassicalResultValue.scalar(
              "basic_site_score",
              Math.max(0.0, molecule.spinMultiplicity().value() - 1.0) + heteroRatio * 8.0,
              "score"));
      return;
    }
    if (kind == ClassicalCalculationKind.REDOX_POTENTIAL) {
      values.add(
          ClassicalResultValue.scalar(
              "oxidation_susceptibility",
              molecule.electronicConfiguration().electronCount() * (0.01 + heteroRatio),
              "score"));
      values.add(
          ClassicalResultValue.scalar(
              "reduction_susceptibility",
              composition.heavyAtomCount() * (0.1 + heteroRatio),
              "score"));
      return;
    }
    if (kind == ClassicalCalculationKind.BASIS_SET_CONVERGENCE) {
      values.add(
          ClassicalResultValue.scalar(
              "basis_sensitivity_score",
              Math.max(1.0, composition.heavyAtomCount() * (0.25 + heteroRatio)),
              "score"));
      values.add(
          ClassicalResultValue.of(
              "basis_convergence_rows",
              ClassicalResultValueKind.TABLE,
              "",
              this.basisConvergenceRows(composition)));
      return;
    }
    if (kind == ClassicalCalculationKind.METHOD_BENCHMARK) {
      values.add(
          ClassicalResultValue.scalar(
              "method_sensitivity_score",
              Math.max(
                  1.0, molecule.electronicConfiguration().electronCount() * 0.015 + heteroRatio),
              "score"));
      values.add(
          ClassicalResultValue.of(
              "method_benchmark_rows",
              ClassicalResultValueKind.TABLE,
              "",
              this.methodBenchmarkRows(
                  composition, molecule.electronicConfiguration().electronCount())));
      return;
    }
    if (kind == ClassicalCalculationKind.UNCERTAINTY_ANALYSIS) {
      values.add(
          ClassicalResultValue.scalar(
              "uncertainty_axis_count",
              Math.max(3.0, composition.heavyAtomCount() + features.features().size()),
              "count"));
      values.add(
          ClassicalResultValue.of(
              "uncertainty_rows",
              ClassicalResultValueKind.TABLE,
              "",
              this.moleculeUncertaintyRows(features)));
      return;
    }
    if (kind == ClassicalCalculationKind.NMR_PREDICTION) {
      values.add(
          ClassicalResultValue.scalar(
              "proton_environment_count",
              Math.max(1.0, composition.atomCount() - composition.heavyAtomCount()),
              "count"));
      values.add(
          ClassicalResultValue.scalar(
              "carbon_environment_count", Math.max(1.0, composition.heavyAtomCount()), "count"));
      return;
    }
    if (kind == ClassicalCalculationKind.IR_RAMAN_PREDICTION) {
      values.add(
          ClassicalResultValue.scalar(
              "vibrational_mode_estimate",
              Math.max(1.0, 3.0 * composition.atomCount() - 6.0),
              "count"));
      values.add(ClassicalResultValue.text("strong_band_drivers", this.featureSummary(features)));
      return;
    }
    if (kind == ClassicalCalculationKind.MASS_SPECTROMETRY_PREDICTION) {
      values.add(
          ClassicalResultValue.scalar(
              "nominal_parent_mass", composition.mass().nominalMassNumber(), "Da"));
      values.add(
          ClassicalResultValue.scalar(
              "fragmentation_complexity", Math.max(1.0, molecule.bondCount()), "score"));
      return;
    }
    if (kind == ClassicalCalculationKind.EXCITED_STATE_ANALYSIS) {
      values.add(
          ClassicalResultValue.scalar(
              "chromophore_score",
              Math.max(0.0, composition.heavyAtomCount() * heteroRatio),
              "score"));
      values.add(
          ClassicalResultValue.text(
              "recommended_method", "TD-DFT or multireference backend validation"));
      return;
    }
    if (kind == ClassicalCalculationKind.HESSIAN_ANALYSIS) {
      values.add(
          ClassicalResultValue.scalar(
              "hessian_dimension", 3.0 * composition.atomCount(), "coordinate"));
      values.add(
          ClassicalResultValue.scalar(
              "normal_mode_count", Math.max(1.0, 3.0 * composition.atomCount() - 6.0), "count"));
      return;
    }
    if (kind == ClassicalCalculationKind.STEREOCHEMICAL_OUTCOME_ANALYSIS
        || kind == ClassicalCalculationKind.ENANTIOMER_DIASTEREOMER_ANALYSIS) {
      final int stereocenterCount = molecule.stereochemistry().centers().size();
      final int torsionCount = molecule.conformation().torsionAngles().size();
      values.add(
          ClassicalResultValue.scalar(
              "stereo_complexity_score", stereocenterCount + torsionCount, "score"));
      values.add(
          ClassicalResultValue.text(
              "stereo_validation", "compare enantiomeric and diastereomeric conformer ensembles"));
      return;
    }
    values.add(ClassicalResultValue.text("analysis", "molecule-level model completed"));
  }

  private void addReactionKindValues(
      final List<ClassicalResultValue> values,
      final ClassicalSubjectSize size,
      final ClassicalCalculationRequest request,
      final Reaction reaction,
      final ClassicalCalculationKind kind,
      final List<ClassicalDiagnostic> diagnostics) {
    final double balancePenalty =
        reaction.balance().atomBalanced() && reaction.balance().chargeBalanced() ? 0.0 : 1.0;
    if (kind == ClassicalCalculationKind.SELECTIVITY_ANALYSIS) {
      final double triggerCount = this.mechanismTriggerCount(reaction);
      values.add(
          ClassicalResultValue.scalar(
              "regioselectivity_conflict_score",
              balancePenalty + size.participantCount() * 0.2 + triggerCount * 0.08,
              "score"));
      values.add(
          ClassicalResultValue.scalar(
              "stereoselectivity_conflict_score",
              Math.max(0.0, size.bondCount() * 0.01 + triggerCount * 0.03),
              "score"));
      values.add(
          ClassicalResultValue.scalar("competing_mechanism_trigger_count", triggerCount, "count"));
      values.add(
          ClassicalResultValue.of(
              "selectivity_axes",
              ClassicalResultValueKind.TABLE,
              "",
              this.selectivityAxisRows(reaction)));
      return;
    }
    if (kind == ClassicalCalculationKind.COMPETING_PATHWAY_ANALYSIS) {
      final double triggerCount = this.mechanismTriggerCount(reaction);
      values.add(
          ClassicalResultValue.scalar(
              "competing_pathway_count_estimate",
              Math.max(1.0, triggerCount + size.participantCount()),
              "count"));
      values.add(
          ClassicalResultValue.scalar(
              "pathway_disambiguation_priority",
              balancePenalty + triggerCount * 0.12 + size.bondCount() * 0.004,
              "score"));
      values.add(
          ClassicalResultValue.of(
              "competing_pathway_rows",
              ClassicalResultValueKind.TABLE,
              "",
              this.competingPathwayRows(reaction)));
      return;
    }
    if (kind == ClassicalCalculationKind.REACTION_CONDITION_SCREENING) {
      final ReactionConditionScenarioEvaluationPlan scenarioEvaluation =
          this.conditionScenarioEvaluation(request, reaction, diagnostics);
      values.add(
          ClassicalResultValue.scalar(
              "condition_sensitivity",
              this.conditionSensitivityScore(scenarioEvaluation),
              "score"));
      values.add(
          ClassicalResultValue.scalar(
              "condition_matrix_size_estimate",
              this.conditionMatrixSizeEstimate(reaction),
              "scenario"));
      values.add(
          ClassicalResultValue.scalar(
              "condition_evaluated_scenario_count",
              scenarioEvaluation.evaluations().size(),
              "count"));
      values.add(
          ClassicalResultValue.text(
              "recommended_screen", this.bestConditionScenarioLabel(scenarioEvaluation)));
      this.addConditionScenarioValues(values, scenarioEvaluation);
      values.add(
          ClassicalResultValue.of(
              "condition_screen_rows",
              ClassicalResultValueKind.TABLE,
              "",
              this.conditionScreenRows(scenarioEvaluation)));
      return;
    }
    if (kind == ClassicalCalculationKind.REACTION_CONDITION_OPTIMIZATION) {
      final ReactionConditionScenarioEvaluationPlan scenarioEvaluation =
          this.conditionScenarioEvaluation(request, reaction, diagnostics);
      values.add(
          ClassicalResultValue.scalar(
              "condition_optimization_axis_count",
              Math.max(
                  3.0,
                  reaction.conditions().components().size()
                      + reaction.conditions().features().size()),
              "count"));
      values.add(
          ClassicalResultValue.scalar(
              "condition_optimization_design_size",
              this.conditionMatrixSizeEstimate(reaction) * 2.0,
              "scenario"));
      values.add(
          ClassicalResultValue.scalar(
              "condition_optimization_best_score",
              scenarioEvaluation.empty() ? 0.0 : scenarioEvaluation.bestEvaluation().rankingScore(),
              "score"));
      this.addConditionScenarioValues(values, scenarioEvaluation);
      values.add(
          ClassicalResultValue.of(
              "condition_optimization_rows",
              ClassicalResultValueKind.TABLE,
              "",
              this.conditionOptimizationRows(scenarioEvaluation)));
      return;
    }
    if (kind == ClassicalCalculationKind.SOLVENT_SCREENING) {
      final ReactionConditionScenarioEvaluationPlan scenarioEvaluation =
          this.conditionScenarioEvaluation(request, reaction, diagnostics);
      values.add(ClassicalResultValue.scalar("solvent_screen_axis_count", 5.0, "count"));
      values.add(
          ClassicalResultValue.scalar(
              "solvent_polarity_conflict_score",
              balancePenalty + this.conditionSensitivityScore(scenarioEvaluation) * 0.2,
              "score"));
      values.add(
          ClassicalResultValue.of(
              "solvent_screen_rows",
              ClassicalResultValueKind.TABLE,
              "",
              this.solventScreenRows(reaction, scenarioEvaluation)));
      return;
    }
    if (kind == ClassicalCalculationKind.CATALYST_SCREENING) {
      final ReactionConditionScenarioEvaluationPlan scenarioEvaluation =
          this.conditionScenarioEvaluation(request, reaction, diagnostics);
      values.add(ClassicalResultValue.scalar("catalyst_family_count", 6.0, "count"));
      values.add(
          ClassicalResultValue.scalar(
              "catalyst_sensitivity_score",
              balancePenalty
                  + this.conditionSensitivityScore(scenarioEvaluation) * 0.25
                  + size.participantCount() * 0.05,
              "score"));
      values.add(
          ClassicalResultValue.of(
              "catalyst_screen_rows",
              ClassicalResultValueKind.TABLE,
              "",
              this.catalystScreenRows(reaction, scenarioEvaluation)));
      return;
    }
    if (kind == ClassicalCalculationKind.CONCENTRATION_SCREENING) {
      final ReactionConditionScenarioEvaluationPlan scenarioEvaluation =
          this.conditionScenarioEvaluation(request, reaction, diagnostics);
      values.add(ClassicalResultValue.scalar("concentration_regime_count", 4.0, "count"));
      values.add(
          ClassicalResultValue.scalar(
              "intermolecular_competition_score",
              Math.max(
                  0.0,
                  size.participantCount() - 1.0 + reaction.conditions().components().size() * 0.1),
              "score"));
      values.add(
          ClassicalResultValue.of(
              "concentration_screen_rows",
              ClassicalResultValueKind.TABLE,
              "",
              this.concentrationScreenRows(reaction, scenarioEvaluation)));
      return;
    }
    if (kind == ClassicalCalculationKind.BASIS_SET_CONVERGENCE) {
      values.add(
          ClassicalResultValue.scalar(
              "basis_sensitivity_score",
              Math.max(1.0, size.electronCount() * 0.01 + size.participantCount()),
              "score"));
      values.add(
          ClassicalResultValue.of(
              "basis_convergence_rows",
              ClassicalResultValueKind.TABLE,
              "",
              this.reactionBasisRows(reaction)));
      return;
    }
    if (kind == ClassicalCalculationKind.METHOD_BENCHMARK) {
      values.add(ClassicalResultValue.scalar("method_ladder_size", 6.0, "count"));
      values.add(
          ClassicalResultValue.scalar(
              "correlation_sensitivity_score",
              Math.max(
                  1.0, size.electronCount() * 0.012 + this.mechanismTriggerCount(reaction) * 0.2),
              "score"));
      values.add(
          ClassicalResultValue.of(
              "method_benchmark_rows",
              ClassicalResultValueKind.TABLE,
              "",
              this.reactionMethodRows(reaction)));
      return;
    }
    if (kind == ClassicalCalculationKind.UNCERTAINTY_ANALYSIS) {
      values.add(
          ClassicalResultValue.scalar(
              "uncertainty_axis_count",
              Math.max(
                  4.0,
                  this.mechanismTriggerCount(reaction)
                      + reaction.conditions().components().size()
                      + 2.0),
              "count"));
      values.add(
          ClassicalResultValue.of(
              "uncertainty_rows",
              ClassicalResultValueKind.TABLE,
              "",
              this.reactionUncertaintyRows(reaction)));
      return;
    }
    if (kind == ClassicalCalculationKind.MECHANISM_ENUMERATION) {
      values.add(
          ClassicalResultValue.scalar(
              "mechanism_candidate_count",
              Math.max(
                  1.0,
                  this.mechanismTriggerCount(reaction)
                      + reaction.conditions().features().size()
                      + 1.0),
              "count"));
      values.add(
          ClassicalResultValue.of(
              "mechanism_enumeration_rows",
              ClassicalResultValueKind.TABLE,
              "",
              this.mechanismEnumerationRows(reaction)));
      return;
    }
    if (kind == ClassicalCalculationKind.REACTION_HAZARD_AND_STABILITY_ANALYSIS) {
      values.add(
          ClassicalResultValue.scalar(
              "hazard_signal_count", this.hazardSignalCount(reaction), "count"));
      values.add(
          ClassicalResultValue.scalar(
              "stability_review_priority",
              Math.max(
                  1.0,
                  this.hazardSignalCount(reaction) + this.mechanismTriggerCount(reaction) * 0.5),
              "score"));
      values.add(
          ClassicalResultValue.of(
              "hazard_stability_rows",
              ClassicalResultValueKind.TABLE,
              "",
              this.hazardRows(reaction)));
      return;
    }
    if (kind == ClassicalCalculationKind.PRODUCT_DISTRIBUTION_ANALYSIS) {
      final ReactionPredictionResult prediction =
          this.predictProducts(request, reaction, diagnostics);
      final ReactionPredictionApplicabilityReport applicability =
          this.applicabilityReport(request, reaction, prediction);
      final double triggerCount = this.mechanismTriggerCount(reaction);
      values.add(
          ClassicalResultValue.text("distribution_model_source", "reaction_prediction_service"));
      values.add(
          ClassicalResultValue.text(
              "product_side_seed_used",
              Boolean.toString(AdvancedChemistryPropertyExecutor.useProductSideAsSeed(request))));
      values.add(
          ClassicalResultValue.scalar(
              "prediction_candidate_count", prediction.candidates().size(), "count"));
      values.add(
          ClassicalResultValue.scalar(
              "prediction_probability_mass", prediction.probabilityMass(), "fraction"));
      values.add(
          ClassicalResultValue.scalar(
              "major_product_probability_estimate",
              this.majorProductProbability(prediction, triggerCount, balancePenalty),
              "fraction"));
      values.add(
          ClassicalResultValue.scalar(
              "minor_product_family_count",
              this.minorProductFamilyCount(prediction, triggerCount, size.participantCount()),
              "count"));
      values.add(
          ClassicalResultValue.text(
              "prediction_best_products", this.bestPredictionProducts(prediction)));
      this.addPredictionWorkflowValues(values, request, prediction, diagnostics);
      this.addProductDistributionSignalValues(values, prediction);
      this.addProductDistributionAuditValues(values, reaction, prediction);
      this.addApplicabilityValues(values, applicability);
      values.add(
          ClassicalResultValue.of(
              "product_distribution_rows",
              ClassicalResultValueKind.TABLE,
              "",
              this.productDistributionRows(reaction, prediction)));
      values.add(
          ClassicalResultValue.of(
              "product_distribution_audit_rows",
              ClassicalResultValueKind.TABLE,
              "",
              this.productDistributionAuditRows(reaction, prediction)));
      values.add(
          ClassicalResultValue.of(
              "product_competing_pathway_rows",
              ClassicalResultValueKind.TABLE,
              "",
              this.competingPathwayRows(reaction)));
      return;
    }
    if (kind == ClassicalCalculationKind.YIELD_WINDOW_ESTIMATION) {
      final ReactionPredictionResult prediction =
          this.predictProducts(request, reaction, diagnostics);
      final ReactionPredictionApplicabilityReport applicability =
          this.applicabilityReport(request, reaction, prediction);
      final double conditionAxisCount =
          reaction.conditions().empty()
              ? 1.0
              : reaction.conditions().components().size() + reaction.conditions().features().size();
      values.add(
          ClassicalResultValue.text(
              "product_side_seed_used",
              Boolean.toString(AdvancedChemistryPropertyExecutor.useProductSideAsSeed(request))));
      values.add(
          ClassicalResultValue.scalar(
              "prediction_candidate_count", prediction.candidates().size(), "count"));
      values.add(
          ClassicalResultValue.scalar(
              "prediction_major_product_probability",
              this.majorProductProbability(prediction, 0.0, 0.0),
              "fraction"));
      this.addPredictionWorkflowValues(values, request, prediction, diagnostics);
      this.addProductDistributionSignalValues(values, prediction);
      this.addProductDistributionAuditValues(values, reaction, prediction);
      this.addApplicabilityValues(values, applicability);
      values.add(
          ClassicalResultValue.scalar(
              "conservative_yield_floor",
              this.conservativeYieldFloor(
                  prediction, balancePenalty, this.hazardSignalCount(reaction)),
              "fraction"));
      values.add(
          ClassicalResultValue.scalar(
              "optimistic_yield_ceiling",
              this.optimisticYieldCeiling(prediction, conditionAxisCount, balancePenalty),
              "fraction"));
      values.add(
          ClassicalResultValue.of(
              "yield_window_rows",
              ClassicalResultValueKind.TABLE,
              "",
              this.yieldWindowRows(reaction, prediction)));
      return;
    }
    if (kind == ClassicalCalculationKind.IMPURITY_AND_SIDE_PRODUCT_ANALYSIS) {
      final ReactionPredictionResult prediction =
          this.predictProducts(request, reaction, diagnostics);
      final ReactionPredictionApplicabilityReport applicability =
          this.applicabilityReport(request, reaction, prediction);
      values.add(
          ClassicalResultValue.text(
              "product_side_seed_used",
              Boolean.toString(AdvancedChemistryPropertyExecutor.useProductSideAsSeed(request))));
      values.add(
          ClassicalResultValue.scalar(
              "prediction_candidate_count", prediction.candidates().size(), "count"));
      this.addPredictionWorkflowValues(values, request, prediction, diagnostics);
      this.addProductDistributionSignalValues(values, prediction);
      this.addProductDistributionAuditValues(values, reaction, prediction);
      this.addApplicabilityValues(values, applicability);
      values.add(
          ClassicalResultValue.scalar(
              "impurity_family_count_estimate",
              this.impurityFamilyCount(prediction, reaction),
              "count"));
      values.add(
          ClassicalResultValue.scalar(
              "side_product_review_priority",
              Math.max(
                  1.0,
                  size.participantCount() * 0.4
                      + this.mechanismTriggerCount(reaction) * 0.6
                      + prediction.diagnosticCount() * 0.1),
              "score"));
      values.add(
          ClassicalResultValue.of(
              "side_product_rows",
              ClassicalResultValueKind.TABLE,
              "",
              this.sideProductRows(reaction, prediction)));
      return;
    }
    if (kind == ClassicalCalculationKind.SCALE_UP_RISK_ANALYSIS) {
      final ReactionPredictionResult prediction =
          this.predictProducts(request, reaction, diagnostics);
      final ReactionPredictionApplicabilityReport applicability =
          this.applicabilityReport(request, reaction, prediction);
      final double totalGramScale = this.totalGramScale(reaction);
      final double solventGramScale = this.roleGramScale(reaction, ReactionComponentRole.SOLVENT);
      final double largestComponentGramScale = this.largestComponentGramScale(reaction);
      final double quantifiedComponentCount = this.quantifiedComponentCount(reaction);
      final double viscosityProxy = this.viscosityProxy(reaction, solventGramScale);
      final double heatTransferProxy = this.heatTransferProxy(reaction, totalGramScale);
      final double massTransferProxy =
          Math.max(
              0.0,
              size.participantCount() * 0.10
                  + quantifiedComponentCount * 0.04
                  + Math.min(0.45, totalGramScale / 250.0)
                  + viscosityProxy * 0.35);
      values.add(ClassicalResultValue.scalar("total_quantified_mass", totalGramScale, "g"));
      values.add(ClassicalResultValue.scalar("solvent_quantified_mass", solventGramScale, "g"));
      values.add(
          ClassicalResultValue.scalar("largest_component_mass", largestComponentGramScale, "g"));
      values.add(
          ClassicalResultValue.scalar(
              "quantified_component_count", quantifiedComponentCount, "count"));
      values.add(
          ClassicalResultValue.scalar(
              "thermal_runaway_proxy",
              Math.max(
                  0.0,
                  this.hazardSignalCount(reaction) * 0.18
                      + size.bondCount() * 0.002
                      + heatTransferProxy * 0.25),
              "score"));
      values.add(
          ClassicalResultValue.scalar("mixing_mass_transfer_proxy", massTransferProxy, "score"));
      values.add(ClassicalResultValue.scalar("viscosity_solvent_proxy", viscosityProxy, "score"));
      values.add(ClassicalResultValue.scalar("heat_transfer_proxy", heatTransferProxy, "score"));
      values.add(
          ClassicalResultValue.scalar(
              "prediction_close_competitor_count", applicability.closeCompetitorCount(), "count"));
      values.add(
          ClassicalResultValue.text(
              "prediction_readiness_level", applicability.readinessLevel().name()));
      this.addPredictionWorkflowValues(values, request, prediction, diagnostics);
      this.addProductDistributionSignalValues(values, prediction);
      this.addProductDistributionAuditValues(values, reaction, prediction);
      values.add(
          ClassicalResultValue.of(
              "scale_up_risk_rows",
              ClassicalResultValueKind.TABLE,
              "",
              this.scaleUpRiskRows(reaction)));
      return;
    }
    if (kind == ClassicalCalculationKind.QUENCH_AND_WORKUP_ANALYSIS) {
      final ReactionPredictionResult prediction =
          this.predictProducts(request, reaction, diagnostics);
      final ReactionPredictionApplicabilityReport applicability =
          this.applicabilityReport(request, reaction, prediction);
      values.add(
          ClassicalResultValue.scalar(
              "workup_axis_count",
              Math.max(
                  1.0, reaction.conditions().features().size() + this.hazardSignalCount(reaction)),
              "count"));
      values.add(
          ClassicalResultValue.scalar(
              "quench_sensitivity_score",
              Math.max(0.0, balancePenalty + this.hazardSignalCount(reaction) * 0.25),
              "score"));
      values.add(
          ClassicalResultValue.text(
              "prediction_readiness_level", applicability.readinessLevel().name()));
      values.add(
          ClassicalResultValue.scalar(
              "prediction_applicability_score", applicability.applicabilityScore(), "score"));
      this.addPredictionWorkflowValues(values, request, prediction, diagnostics);
      this.addProductDistributionSignalValues(values, prediction);
      this.addProductDistributionAuditValues(values, reaction, prediction);
      values.add(
          ClassicalResultValue.of(
              "quench_workup_rows",
              ClassicalResultValueKind.TABLE,
              "",
              this.conditionWorkupRows(reaction)));
      return;
    }
    if (kind == ClassicalCalculationKind.REACTION_HISTORY_TIMELINE_ANALYSIS) {
      values.add(
          ClassicalResultValue.scalar(
              "timeline_event_count_estimate",
              Math.max(
                  3.0,
                  reaction.conditions().components().size()
                      + reaction.conditions().features().size()
                      + 2.0),
              "count"));
      values.add(
          ClassicalResultValue.scalar(
              "sampling_checkpoint_count",
              Math.max(2.0, this.mechanismTriggerCount(reaction) + 2.0),
              "count"));
      values.add(
          ClassicalResultValue.of(
              "reaction_timeline_rows",
              ClassicalResultValueKind.TABLE,
              "",
              this.conditionWorkupRows(reaction)));
      return;
    }
    if (kind == ClassicalCalculationKind.REACTION_NETWORK_ANALYSIS) {
      final double triggerCount = this.mechanismTriggerCount(reaction);
      final ReactionPredictionResult prediction =
          this.predictProducts(request, reaction, diagnostics);
      final ReactionPredictionApplicabilityReport applicability =
          this.applicabilityReport(request, reaction, prediction);
      values.add(
          ClassicalResultValue.text(
              "product_side_seed_used",
              Boolean.toString(AdvancedChemistryPropertyExecutor.useProductSideAsSeed(request))));
      values.add(
          ClassicalResultValue.scalar(
              "network_node_estimate",
              Math.max(
                  2.0,
                  size.participantCount() * 3.0
                      + triggerCount * 2.0
                      + prediction.candidates().size()),
              "count"));
      values.add(
          ClassicalResultValue.scalar(
              "branching_risk",
              balancePenalty + size.bondCount() * 0.005 + triggerCount * 0.06,
              "score"));
      values.add(ClassicalResultValue.scalar("mechanism_trigger_count", triggerCount, "count"));
      values.add(
          ClassicalResultValue.scalar(
              "network_predicted_branch_count", prediction.candidates().size(), "count"));
      values.add(
          ClassicalResultValue.scalar(
              "network_close_competitor_count", applicability.closeCompetitorCount(), "count"));
      this.addPredictionWorkflowValues(values, request, prediction, diagnostics);
      this.addProductDistributionSignalValues(values, prediction);
      this.addProductDistributionAuditValues(values, reaction, prediction);
      values.add(
          ClassicalResultValue.of(
              "network_node_rows",
              ClassicalResultValueKind.TABLE,
              "",
              this.reactionNetworkRows(reaction, prediction)));
      return;
    }
    if (kind == ClassicalCalculationKind.MICROKINETIC_MODELING) {
      final double triggerCount = this.mechanismTriggerCount(reaction);
      final ReactionPredictionResult prediction =
          this.predictProducts(request, reaction, diagnostics);
      values.add(
          ClassicalResultValue.scalar(
              "elementary_step_count_estimate",
              Math.max(2.0, 2.0 + triggerCount * 2.0 + size.participantCount()),
              "count"));
      values.add(
          ClassicalResultValue.scalar(
              "rate_law_parameter_count",
              Math.max(2.0, triggerCount + reaction.conditions().components().size() + 1.0),
              "count"));
      values.add(
          ClassicalResultValue.scalar(
              "microkinetic_predicted_branch_count", prediction.candidates().size(), "count"));
      this.addPredictionWorkflowValues(values, request, prediction, diagnostics);
      this.addProductDistributionSignalValues(values, prediction);
      this.addProductDistributionAuditValues(values, reaction, prediction);
      values.add(
          ClassicalResultValue.of(
              "microkinetic_step_rows",
              ClassicalResultValueKind.TABLE,
              "",
              this.microkineticRows(reaction, prediction)));
      return;
    }
    if (kind == ClassicalCalculationKind.TRANSITION_STATE_ENSEMBLE_ANALYSIS) {
      values.add(
          ClassicalResultValue.scalar(
              "transition_state_candidate_count",
              Math.max(1.0, this.mechanismTriggerCount(reaction) + size.participantCount()),
              "count"));
      values.add(
          ClassicalResultValue.scalar(
              "ensemble_conformer_axis_count", Math.max(1.0, size.bondCount() / 4.0), "count"));
      values.add(
          ClassicalResultValue.of(
              "transition_state_ensemble_rows",
              ClassicalResultValueKind.TABLE,
              "",
              this.transitionStateEnsembleRows(reaction)));
      return;
    }
    if (kind == ClassicalCalculationKind.RETROSYNTHESIS_PLANNING) {
      final ReactionPredictionResult prediction =
          this.predictProducts(request, reaction, diagnostics);
      final ReactionPredictionApplicabilityReport applicability =
          this.applicabilityReport(request, reaction, prediction);
      values.add(
          ClassicalResultValue.text(
              "product_side_seed_used",
              Boolean.toString(AdvancedChemistryPropertyExecutor.useProductSideAsSeed(request))));
      final double functionalDisconnections = Math.max(1.0, size.bondCount() / 2.0);
      final double conditionDisconnections = Math.max(0.0, this.mechanismTriggerCount(reaction));
      values.add(
          ClassicalResultValue.scalar(
              "disconnection_candidate_count",
              functionalDisconnections + conditionDisconnections + prediction.candidates().size(),
              "count"));
      values.add(
          ClassicalResultValue.text(
              "planning_axis", "functional group interconversion + strategic bond disconnection"));
      values.add(
          ClassicalResultValue.text(
              "prediction_readiness_level", applicability.readinessLevel().name()));
      this.addPredictionWorkflowValues(values, request, prediction, diagnostics);
      this.addProductDistributionSignalValues(values, prediction);
      this.addProductDistributionAuditValues(values, reaction, prediction);
      values.add(
          ClassicalResultValue.of(
              "retrosynthesis_disconnection_rows",
              ClassicalResultValueKind.TABLE,
              "",
              this.retrosynthesisRows(reaction, prediction)));
      return;
    }
    if (kind == ClassicalCalculationKind.IRC_ANALYSIS) {
      values.add(
          ClassicalResultValue.scalar(
              "path_segment_estimate", Math.max(3.0, size.bondCount() / 3.0), "count"));
      values.add(
          ClassicalResultValue.text("path_direction", "reactant <- transition-state -> product"));
      return;
    }
    if (kind == ClassicalCalculationKind.STEREOCHEMICAL_OUTCOME_ANALYSIS
        || kind == ClassicalCalculationKind.ENANTIOMER_DIASTEREOMER_ANALYSIS) {
      values.add(
          ClassicalResultValue.scalar(
              "stereo_path_count_estimate", Math.max(1.0, size.participantCount() * 2.0), "count"));
      values.add(
          ClassicalResultValue.text(
              "stereo_axis", "face selectivity, conformer approach and catalyst environment"));
      return;
    }
    values.add(ClassicalResultValue.text("analysis", "reaction-level model completed"));
  }

  private double mechanismTriggerCount(final Reaction reaction) {
    double count = 0.0;
    final List<ReactionParticipant> participants = reaction.reactants().participants();
    for (int i = 0; i < participants.size(); ++i) {
      final MolecularFeatureSet features =
          this.featureAnalyzer.analyze(participants.get(i).molecule());
      count += AdvancedChemistryPropertyExecutor.structuralMechanismTriggerCount(features);
    }
    final List<ReactionConditionComponent> components = reaction.conditions().components();
    for (int i = 0; i < components.size(); ++i) {
      final String name = components.get(i).name().toLowerCase(java.util.Locale.ROOT);
      if (name.contains("pd")
          || name.contains("nickel")
          || name.contains("copper")
          || name.contains("boronic")) {
        ++count;
      }
      if (name.contains("ozone")
          || name.contains("peracid")
          || name.contains("mcpba")
          || name.contains("permanganate")) {
        ++count;
      }
      if (name.contains("alcl3")
          || name.contains("nitric")
          || name.contains("hno3")
          || name.contains("lewis acid")) {
        ++count;
      }
      if (name.contains("diene")
          || name.contains("dienophile")
          || name.contains("azide")
          || name.contains("oxime")) {
        ++count;
      }
    }
    return count;
  }

  private static double structuralMechanismTriggerCount(final MolecularFeatureSet features) {
    double count = 0.0;
    count += Math.min(3L, features.countOf(MolecularFeatureKind.HETEROARYL_HALIDE));
    count += Math.min(2L, features.countOf(MolecularFeatureKind.ELECTRON_POOR_HETEROARENE));
    count += Math.min(2L, features.countOf(MolecularFeatureKind.PRIMARY_OR_SECONDARY_AMINE));
    count += Math.min(2L, features.countOf(MolecularFeatureKind.ACTIVATED_NUCLEOPHILE));
    count += Math.min(2L, features.countOf(MolecularFeatureKind.NITRILE));
    count += Math.min(2L, features.countOf(MolecularFeatureKind.CARBONYL));
    count += Math.min(2L, features.countOf(MolecularFeatureKind.CARBOXYL_DERIVATIVE));
    count += Math.min(2L, features.countOf(MolecularFeatureKind.EPOXIDE));
    count += Math.min(2L, features.countOf(MolecularFeatureKind.ALKENE));
    count += Math.min(2L, features.countOf(MolecularFeatureKind.ALKYNE));
    count += Math.min(2L, features.countOf(MolecularFeatureKind.NITRO));
    count += Math.min(2L, features.countOf(MolecularFeatureKind.RADICAL_CENTER));
    count += Math.min(2L, features.countOf(MolecularFeatureKind.BENZYL_OR_BENZYLIC_FRAGMENT));
    return count;
  }

  private double conditionMatrixSizeEstimate(final Reaction reaction) {
    if (reaction.conditions().empty()) {
      return 16.0;
    }
    final double componentAxes = Math.max(1.0, reaction.conditions().components().size());
    final double featureAxes = Math.max(1.0, reaction.conditions().features().size());
    return Math.min(1024.0, componentAxes * featureAxes * 4.0);
  }

  private List<String> selectivityAxisRows(final Reaction reaction) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add("axis\tscore_driver\taction");
    rows.add(
        "regioselectivity\tfunctional group and leaving-group competition\tcompare candidate sites");
    rows.add(
        "stereoselectivity\tconformer and approach-face competition\tscreen stereochemical constraints");
    rows.add(
        "mechanism\t"
            + this.mechanismTriggerCount(reaction)
            + " structure/condition triggers\tseparate incompatible pathways");
    return List.copyOf(rows);
  }

  private List<String> conditionScreenRows(
      final ReactionConditionScenarioEvaluationPlan scenarioEvaluation) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add(
        "rank\tscenario\tkind\tranking_score\tbest_probability\texpected_yield"
            + "\tprobability_delta\tyield_delta\tprobability_mass\tdiagnostics"
            + "\telectronic_weight\tcalibration_weight\tprimary_mechanism\tbest_products\trationale");
    final List<ReactionConditionScenarioEvaluation> ranked = scenarioEvaluation.rankedEvaluations();
    for (int i = 0; i < ranked.size(); ++i) {
      final ReactionConditionScenarioEvaluation evaluation = ranked.get(i);
      rows.add(
          (i + 1)
              + "\t"
              + evaluation.scenario().id()
              + "\t"
              + evaluation.scenario().kind().name()
              + "\t"
              + evaluation.rankingScore()
              + "\t"
              + evaluation.bestProbability()
              + "\t"
              + evaluation.bestExpectedYieldPercent()
              + "\t"
              + evaluation.baselineProbabilityDelta()
              + "\t"
              + evaluation.baselineExpectedYieldDelta()
              + "\t"
              + evaluation.probabilityMass()
              + "\t"
              + evaluation.diagnosticCount()
              + "\t"
              + AdvancedChemistryPropertyExecutor.bestCandidateEvidenceWeight(
                  evaluation, ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR)
              + "\t"
              + AdvancedChemistryPropertyExecutor.bestCandidateEvidenceWeight(
                  evaluation, ReactionPredictionEvidenceKind.CALIBRATION_PRIOR)
              + "\t"
              + AdvancedChemistryPropertyExecutor.bestCandidateMechanism(evaluation)
              + "\t"
              + this.bestPredictionProducts(evaluation.predictionResult())
              + "\t"
              + evaluation.scenario().rationale());
    }
    return List.copyOf(rows);
  }

  private List<String> competingPathwayRows(final Reaction reaction) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add("pathway\tcompetition_driver\trecommended_check");
    rows.add("declared-context\treaction record context\tgeometry + reaction energy");
    rows.add("side-route\tcondition-triggered alternative families\tselectivity + kinetics");
    rows.add(
        "background-route\tuncatalyzed or solvent-assisted chemistry\tcondition omission screen");
    rows.add(
        "decomposition-route\theat, light, redox or radical triggers\tstability and spectroscopy check");
    rows.add(
        "trigger-count\t"
            + this.mechanismTriggerCount(reaction)
            + "\tprioritize named trigger families");
    return List.copyOf(rows);
  }

  private List<String> conditionOptimizationRows(
      final ReactionConditionScenarioEvaluationPlan scenarioEvaluation) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add(
        "rank\tscenario\tkind\tranking_score\tbest_probability\texpected_yield"
            + "\tprobability_delta\tyield_delta\telectronic_weight\tcalibration_weight"
            + "\tprimary_mechanism\tbest_products");
    final List<ReactionConditionScenarioEvaluation> ranked = scenarioEvaluation.rankedEvaluations();
    for (int i = 0; i < ranked.size(); ++i) {
      final ReactionConditionScenarioEvaluation evaluation = ranked.get(i);
      rows.add(
          (i + 1)
              + "\t"
              + evaluation.scenario().id()
              + "\t"
              + evaluation.scenario().kind().name()
              + "\t"
              + evaluation.rankingScore()
              + "\t"
              + evaluation.bestProbability()
              + "\t"
              + evaluation.bestExpectedYieldPercent()
              + "\t"
              + evaluation.baselineProbabilityDelta()
              + "\t"
              + evaluation.baselineExpectedYieldDelta()
              + "\t"
              + AdvancedChemistryPropertyExecutor.bestCandidateEvidenceWeight(
                  evaluation, ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR)
              + "\t"
              + AdvancedChemistryPropertyExecutor.bestCandidateEvidenceWeight(
                  evaluation, ReactionPredictionEvidenceKind.CALIBRATION_PRIOR)
              + "\t"
              + AdvancedChemistryPropertyExecutor.bestCandidateMechanism(evaluation)
              + "\t"
              + this.bestPredictionProducts(evaluation.predictionResult()));
    }
    return List.copyOf(rows);
  }

  private List<String> conditionWorkupRows(final Reaction reaction) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add("axis\tcurrent_signal\trecommended_variation");
    if (reaction.conditions().empty()) {
      rows.add("temperature\tmissing\tlow, room, reflux");
      rows.add("solvent\tmissing\tpolar aprotic, protic, nonpolar");
      rows.add("catalyst\tmissing\tuncatalyzed, Lewis acid, transition metal");
      return List.copyOf(rows);
    }
    final List<ReactionConditionComponent> components = reaction.conditions().components();
    for (int i = 0; i < components.size(); ++i) {
      final ReactionConditionComponent component = components.get(i);
      rows.add(
          component.role().name() + "\t" + component.name() + "\tvary loading, equivalent or omit");
    }
    rows.add(
        "temperature\t"
            + (reaction.conditions().temperature() == null
                ? "missing"
                : reaction.conditions().temperature().kelvin() + "K")
            + "\t-20K, current, +20K");
    rows.add(
        "duration\t"
            + (reaction.conditions().hasDuration()
                ? reaction.conditions().duration().hours() + "h"
                : "missing")
            + "\tshort, current, extended");
    return List.copyOf(rows);
  }

  private List<String> microkineticRows(
      final Reaction reaction, final ReactionPredictionResult prediction) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add("step\tkind\tprobability_or_weight\tyield_percent\tmechanism\tmodel_input");
    rows.add(
        "activation\tpre-equilibrium\t1.0\t0.0\tcondition_window\tconditions + reactant features");
    rows.add(
        "bond-change\trate-determining candidate\t1.0\t0.0\ttransition_state\ttransition-state ensemble");
    rows.add(
        "product-release\tpost-transition event\t1.0\t0.0\tproduct_release\tproduct stability");
    rows.add(
        "side-path\tcompetitive branch\t"
            + this.mechanismTriggerCount(reaction)
            + "\t0.0\tcompeting_pathway\tmechanism triggers");
    rows.add(
        "deactivation\tcatalyst/reagent loss\t1.0\t0.0\tcondition_decay\tcondition component stability");
    final List<ReactionProductCandidate> candidates = prediction.candidates();
    for (int i = 0; i < candidates.size(); ++i) {
      final ReactionProductCandidate candidate = candidates.get(i);
      rows.add(
          "candidate-"
              + (i + 1)
              + "\tpredicted-product-channel\t"
              + candidate.probability()
              + "\t"
              + candidate.expectedYieldPercent()
              + "\t"
              + AdvancedChemistryPropertyExecutor.primaryMechanism(candidate)
              + "\t"
              + AdvancedChemistryPropertyExecutor.microkineticCandidateBasis(candidate));
    }
    return List.copyOf(rows);
  }

  private List<String> transitionStateEnsembleRows(final Reaction reaction) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add("candidate\taxis\twhy");
    rows.add("lowest-barrier\tbond-change coordinate\tprimary pathway check");
    rows.add("regioisomeric\talternative reactive site\tregioselectivity check");
    rows.add("stereoisomeric\tapproach face/conformer\tstereochemical outcome check");
    rows.add("condition-bound\tsolvent/catalyst association\tcondition dependence");
    rows.add("side-reaction\ttriggered mechanism family\tcompeting pathway validation");
    return List.copyOf(rows);
  }

  private List<String> reactionNetworkRows(
      final Reaction reaction, final ReactionPredictionResult prediction) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add("node\tkind\tmechanism\tprobability\tyield_percent\tbasis");
    rows.add(
        "reactants\tinput\tnone\t1.0\t0.0\t"
            + reaction.reactants().participantCount()
            + " participants");
    rows.add(
        "products\tobserved-or-target\tnone\t1.0\t0.0\t"
            + reaction.products().participantCount()
            + " participants");
    rows.add(
        "conditions\tmodifier\tnone\t1.0\t0.0\t"
            + reaction.conditions().components().size()
            + " components");
    rows.add(
        "side-paths\tpredicted\tcompeting_pathway\t"
            + this.mechanismTriggerCount(reaction)
            + "\t0.0\tmechanism triggers");
    for (int i = 0; i < prediction.candidates().size(); ++i) {
      final ReactionProductCandidate candidate = prediction.candidates().get(i);
      rows.add(
          "predicted-product-"
              + (i + 1)
              + "\tcandidate\t"
              + AdvancedChemistryPropertyExecutor.primaryMechanism(candidate)
              + "\t"
              + candidate.probability()
              + "\t"
              + candidate.expectedYieldPercent()
              + "\tproducts="
              + candidate.products().participantCount());
    }
    return List.copyOf(rows);
  }

  private List<String> retrosynthesisRows(
      final Reaction reaction, final ReactionPredictionResult prediction) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add("strategy\tpriority\treason");
    rows.add("strategic-bond-disconnection\thigh\tproduct bond graph defines candidate cuts");
    rows.add(
        "functional-group-interconversion\tmedium\treaction conditions imply transformable groups");
    rows.add(
        "condition-removal-control\tmedium\t"
            + reaction.conditions().components().size()
            + " condition components may be optional or catalytic");
    rows.add("stereochemical-backtrace\tconditional\trequires stereocenter and conformer evidence");
    for (int i = 0; i < prediction.candidates().size(); ++i) {
      final ReactionProductCandidate candidate = prediction.candidates().get(i);
      rows.add(
          "predicted-product-disconnection-"
              + (i + 1)
              + "\t"
              + (candidate.probability() >= 0.35 ? "high" : "medium")
              + "\tpredicted candidate probability="
              + candidate.probability()
              + ", product participants="
              + candidate.products().participantCount());
    }
    return List.copyOf(rows);
  }

  private ReactionPredictionResult predictProducts(
      final ClassicalCalculationRequest request,
      final Reaction reaction,
      final List<ClassicalDiagnostic> diagnostics) {
    final ReactionPredictionResult prediction =
        this.predictionService.predict(
            this.predictionRequest(request, reaction, "_advanced_distribution"));
    final ReactionPredictionResult electronicallyRanked =
        AdvancedChemistryPropertyExecutor.electronicRerankEnabled(request)
            ? this.electronicReranker.rerank(request, prediction, diagnostics)
            : prediction;
    return this.calibrationApplier.apply(
        electronicallyRanked, AdvancedChemistryPropertyExecutor.calibrationPriorsEnabled(request));
  }

  private ReactionPredictionApplicabilityReport applicabilityReport(
      final ClassicalCalculationRequest request,
      final Reaction reaction,
      final ReactionPredictionResult prediction) {
    final ReactionPredictionRequest predictionRequest =
        this.predictionRequest(request, reaction, "_advanced_applicability");
    final ReactionConditionEffectProfile conditionEffectProfile =
        this.conditionEffectAnalyzer.analyze(predictionRequest);
    return this.applicabilityAnalyzer.analyze(prediction, conditionEffectProfile);
  }

  private void addApplicabilityValues(
      final List<ClassicalResultValue> values,
      final ReactionPredictionApplicabilityReport applicability) {
    values.add(
        ClassicalResultValue.text(
            "prediction_readiness_level", applicability.readinessLevel().name()));
    values.add(
        ClassicalResultValue.scalar(
            "prediction_applicability_score", applicability.applicabilityScore(), "score"));
    values.add(
        ClassicalResultValue.text(
            "quantum_followup_recommended",
            Boolean.toString(applicability.quantumFollowupRecommended())));
    values.add(
        ClassicalResultValue.text(
            "electronic_rerank_recommended",
            Boolean.toString(applicability.electronicRerankRecommended())));
    values.add(
        ClassicalResultValue.scalar(
            "close_competitor_count", applicability.closeCompetitorCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "missing_critical_condition_count",
            applicability.missingCriticalConditionCount(),
            "count"));
    values.add(
        ClassicalResultValue.of(
            "prediction_applicability_trace",
            ClassicalResultValueKind.TABLE,
            "",
            applicability.traceRows()));
  }

  private ReactionConditionScenarioEvaluationPlan conditionScenarioEvaluation(
      final ClassicalCalculationRequest request,
      final Reaction reaction,
      final List<ClassicalDiagnostic> diagnostics) {
    final ReactionConditionScenarioEvaluationPlan rawEvaluation =
        this.conditionScenarioEvaluator.evaluate(
            this.predictionRequest(request, reaction, "_condition_screen"));
    final ArrayList<ReactionConditionScenarioEvaluation> rerankedEvaluations =
        new ArrayList<ReactionConditionScenarioEvaluation>(rawEvaluation.evaluations().size());
    for (int i = 0; i < rawEvaluation.evaluations().size(); ++i) {
      final ReactionConditionScenarioEvaluation evaluation = rawEvaluation.evaluations().get(i);
      final ClassicalCalculationRequest scenarioRequest =
          AdvancedChemistryPropertyExecutor.scenarioCalculationRequest(
              request, reaction, evaluation);
      final ReactionPredictionResult scenarioPrediction =
          AdvancedChemistryPropertyExecutor.electronicRerankEnabled(request)
              ? this.electronicReranker.rerank(
                  scenarioRequest, evaluation.predictionResult(), diagnostics)
              : evaluation.predictionResult();
      rerankedEvaluations.add(
          ReactionConditionScenarioEvaluation.of(
              evaluation.scenario(),
              this.calibrationApplier.apply(
                  scenarioPrediction,
                  AdvancedChemistryPropertyExecutor.calibrationPriorsEnabled(request))));
    }
    return ReactionConditionScenarioEvaluationPlan.of(
        rawEvaluation.requestId(), rerankedEvaluations);
  }

  private void addPredictionWorkflowValues(
      final List<ClassicalResultValue> values,
      final ClassicalCalculationRequest request,
      final ReactionPredictionResult prediction,
      final List<ClassicalDiagnostic> diagnostics) {
    final ReactionOutcomeCalibrationCoverageReport calibrationCoverageReport =
        new ReactionOutcomeCalibrationCoverageAnalyzer().analyze(this.calibrationLibrary);
    values.add(
        ClassicalResultValue.text(
            "advanced_prediction_electronic_rerank_enabled",
            Boolean.toString(AdvancedChemistryPropertyExecutor.electronicRerankEnabled(request))));
    values.add(
        ClassicalResultValue.text(
            "advanced_prediction_execution_mode",
            AdvancedChemistryPropertyExecutor.predictionExecutionMode(request)));
    values.add(
        ClassicalResultValue.text(
            "advanced_prediction_electronic_rerank_opt_in_required",
            Boolean.toString(!AdvancedChemistryPropertyExecutor.electronicRerankEnabled(request))));
    values.add(
        ClassicalResultValue.text(
            "advanced_prediction_calibration_priors_enabled",
            Boolean.toString(AdvancedChemistryPropertyExecutor.calibrationPriorsEnabled(request))));
    values.add(
        ClassicalResultValue.scalar(
            "advanced_calibration_library_observation_count",
            calibrationCoverageReport.observationCount(),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "advanced_calibration_library_dataset_count",
            calibrationCoverageReport.datasetCount(),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "advanced_calibration_library_exact_observation_count",
            calibrationCoverageReport.exactObservationCount(),
            "count"));
    values.add(
        ClassicalResultValue.text(
            "advanced_calibration_library_empirical_prediction_ready",
            Boolean.toString(calibrationCoverageReport.productionReadyForEmpiricalPrediction())));
    values.add(
        ClassicalResultValue.text(
            "advanced_calibration_library_applied_to_prediction",
            Boolean.toString(
                AdvancedChemistryPropertyExecutor.calibrationPriorsEnabled(request)
                    && calibrationCoverageReport.hasExactExperimentalCalibration())));
    values.add(
        ClassicalResultValue.scalar(
            "advanced_prediction_rule_generated_candidate_count",
            AdvancedChemistryPropertyExecutor.ruleGeneratedProductChannelCount(prediction),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "advanced_prediction_user_seed_candidate_count",
            AdvancedChemistryPropertyExecutor.userSeedProductChannelCount(prediction),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "advanced_prediction_merged_candidate_count",
            AdvancedChemistryPropertyExecutor.mergedProductChannelCount(prediction),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "advanced_prediction_surrogate_candidate_count",
            AdvancedChemistryPropertyExecutor.surrogateProductChannelCount(prediction),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "advanced_prediction_explicit_product_graph_candidate_count",
            AdvancedChemistryPropertyExecutor.explicitProductGraphCandidateCount(prediction),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "advanced_prediction_mechanism_only_candidate_count",
            prediction.mechanismChannels().size()
                + AdvancedChemistryPropertyExecutor.surrogateProductChannelCount(prediction),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "advanced_prediction_unmaterialized_mechanism_channel_count",
            prediction.mechanismChannels().size(),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "advanced_prediction_explicit_product_graph_coverage_ratio",
            AdvancedChemistryPropertyExecutor.explicitProductGraphCoverageRatio(prediction),
            "fraction"));
    values.add(
        ClassicalResultValue.scalar(
            "advanced_prediction_candidate_ready_for_electronic_rerank_count",
            AdvancedChemistryPropertyExecutor.candidateReadyForElectronicRerankCount(prediction),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "advanced_prediction_candidate_ready_for_quantum_workflow_count",
            AdvancedChemistryPropertyExecutor.candidateReadyForQuantumWorkflowCount(prediction),
            "count"));
    values.add(
        ClassicalResultValue.text(
            "advanced_candidate_quantum_workflow_preparation_enabled",
            Boolean.toString(AdvancedChemistryPropertyExecutor.prepareQuantumWorkflows(request))));
    values.add(
        ClassicalResultValue.of(
            "advanced_candidate_quantum_workflow_preparation",
            ClassicalResultValueKind.TABLE,
            "",
            this.candidateQuantumWorkflowPreparationRows(request, prediction, diagnostics)));
    values.add(
        ClassicalResultValue.text(
            "advanced_prediction_product_graph_coverage_status",
            AdvancedChemistryPropertyExecutor.productGraphCoverageStatus(prediction)));
    values.add(
        ClassicalResultValue.text(
            "advanced_prediction_product_graph_followup_recommended",
            Boolean.toString(
                AdvancedChemistryPropertyExecutor.productGraphFollowupRecommended(prediction))));
    values.add(
        ClassicalResultValue.text(
            "advanced_prediction_product_graph_followup_reason",
            AdvancedChemistryPropertyExecutor.productGraphFollowupReason(prediction)));
    values.add(
        ClassicalResultValue.of(
            "advanced_prediction_followup_rows",
            ClassicalResultValueKind.TABLE,
            "",
            AdvancedChemistryPropertyExecutor.predictionFollowupRows(prediction)));
    values.add(
        ClassicalResultValue.scalar(
            "advanced_prediction_electronic_evidence_weight",
            AdvancedChemistryPropertyExecutor.totalEvidenceWeight(
                prediction, ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR),
            "weight"));
    values.add(
        ClassicalResultValue.scalar(
            "advanced_prediction_calibration_evidence_weight",
            AdvancedChemistryPropertyExecutor.totalEvidenceWeight(
                prediction, ReactionPredictionEvidenceKind.CALIBRATION_PRIOR),
            "weight"));
    values.add(
        ClassicalResultValue.scalar(
            "advanced_prediction_positive_score_weight",
            AdvancedChemistryPropertyExecutor.totalPositiveScoreWeight(prediction),
            "weight"));
    values.add(
        ClassicalResultValue.scalar(
            "advanced_prediction_negative_score_weight",
            AdvancedChemistryPropertyExecutor.totalNegativeScoreWeight(prediction),
            "weight"));
    values.add(
        ClassicalResultValue.scalar(
            "advanced_prediction_selectivity_score_weight",
            AdvancedChemistryPropertyExecutor.totalScoreComponentWeight(
                prediction, ReactionPredictionScoreComponentKind.SELECTIVITY),
            "weight"));
  }

  private void addProductDistributionSignalValues(
      final List<ClassicalResultValue> values, final ReactionPredictionResult prediction) {
    values.add(
        ClassicalResultValue.scalar(
            "prediction_entropy",
            AdvancedChemistryPropertyExecutor.productDistributionEntropy(prediction),
            "nat"));
    values.add(
        ClassicalResultValue.scalar(
            "prediction_major_minor_gap",
            AdvancedChemistryPropertyExecutor.majorMinorGap(prediction),
            "fraction"));
    values.add(
        ClassicalResultValue.scalar(
            "prediction_close_product_competitor_count",
            AdvancedChemistryPropertyExecutor.closeProductCompetitorCount(prediction),
            "count"));
    values.add(
        ClassicalResultValue.text(
            "prediction_distribution_shape",
            AdvancedChemistryPropertyExecutor.distributionShape(prediction)));
  }

  private void addProductDistributionAuditValues(
      final List<ClassicalResultValue> values,
      final Reaction reaction,
      final ReactionPredictionResult prediction) {
    values.add(
        ClassicalResultValue.scalar(
            "product_distribution_kinetic_weight",
            AdvancedChemistryPropertyExecutor.totalEvidenceWeight(
                prediction, ReactionPredictionEvidenceKind.KINETIC_FACTOR),
            "weight"));
    values.add(
        ClassicalResultValue.scalar(
            "product_distribution_thermodynamic_weight",
            AdvancedChemistryPropertyExecutor.totalEvidenceWeight(
                prediction, ReactionPredictionEvidenceKind.THERMODYNAMIC_FACTOR),
            "weight"));
    values.add(
        ClassicalResultValue.scalar(
            "product_distribution_temperature_weight",
            AdvancedChemistryPropertyExecutor.totalEvidenceWeight(
                prediction, ReactionPredictionEvidenceKind.TEMPERATURE_EFFECT),
            "weight"));
    values.add(
        ClassicalResultValue.scalar(
            "product_distribution_solvent_weight",
            AdvancedChemistryPropertyExecutor.totalEvidenceWeight(
                prediction, ReactionPredictionEvidenceKind.SOLVENT_EFFECT),
            "weight"));
    values.add(
        ClassicalResultValue.scalar(
            "product_distribution_catalyst_weight",
            AdvancedChemistryPropertyExecutor.totalEvidenceWeight(
                prediction, ReactionPredictionEvidenceKind.CATALYST_EFFECT),
            "weight"));
    values.add(
        ClassicalResultValue.scalar(
            "product_distribution_competing_pathway_weight",
            AdvancedChemistryPropertyExecutor.totalEvidenceWeight(
                prediction, ReactionPredictionEvidenceKind.COMPETING_PATHWAY),
            "weight"));
    values.add(
        ClassicalResultValue.scalar(
            "product_distribution_condition_component_count",
            reaction.conditions().components().size(),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "product_distribution_condition_feature_count",
            reaction.conditions().features().size(),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "product_distribution_quantified_component_count",
            this.quantifiedComponentCount(reaction),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "product_distribution_gram_scale_total", this.totalGramScale(reaction), "g"));
    values.add(
        ClassicalResultValue.text(
            "product_distribution_audit_status",
            AdvancedChemistryPropertyExecutor.productDistributionAuditStatus(
                reaction, prediction)));
  }

  private static boolean electronicRerankEnabled(final ClassicalCalculationRequest request) {
    return AdvancedChemistryPropertyExecutor.booleanOptionDefault(
        request, ELECTRONIC_RERANK_OPTION, true);
  }

  private static boolean calibrationPriorsEnabled(final ClassicalCalculationRequest request) {
    return AdvancedChemistryPropertyExecutor.booleanOptionDefault(
        request, APPLY_CALIBRATION_PRIORS_OPTION, false);
  }

  private static String predictionExecutionMode(final ClassicalCalculationRequest request) {
    if (AdvancedChemistryPropertyExecutor.electronicRerankEnabled(request)) {
      return "ELECTRONIC_RERANK";
    }
    return "FAST_MECHANISTIC";
  }

  private static boolean booleanOptionDefault(
      final ClassicalCalculationRequest request, final String name, final boolean defaultValue) {
    final List<ClassicalCalculationOption> options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (!name.equals(option.name())) {
        continue;
      }
      return Boolean.parseBoolean(option.value());
    }
    return defaultValue;
  }

  private static double totalEvidenceWeight(
      final ReactionPredictionResult prediction, final ReactionPredictionEvidenceKind kind) {
    double weight = 0.0;
    for (int i = 0; i < prediction.candidates().size(); ++i) {
      weight += prediction.candidates().get(i).evidenceWeight(kind);
    }
    return weight;
  }

  private static int ruleGeneratedProductChannelCount(final ReactionPredictionResult prediction) {
    int count = 0;
    final List<ReactionProductCandidate> candidates = prediction.candidates();
    for (int i = 0; i < candidates.size(); ++i) {
      final ReactionProductCandidate candidate = candidates.get(i);
      if (candidate.hasEvidence(ReactionPredictionEvidenceKind.USER_RULE)
          && !candidate.hasEvidence(ReactionPredictionEvidenceKind.USER_SEED)
          && !candidate.hasEvidence(ReactionPredictionEvidenceKind.SURROGATE_PRODUCT)) {
        ++count;
      }
    }
    return count;
  }

  private static double totalPositiveScoreWeight(final ReactionPredictionResult prediction) {
    double weight = 0.0;
    final List<ReactionProductCandidate> candidates = prediction.candidates();
    for (int i = 0; i < candidates.size(); ++i) {
      weight += candidates.get(i).scoreBreakdown().positiveWeight();
    }
    return weight;
  }

  private static double totalNegativeScoreWeight(final ReactionPredictionResult prediction) {
    double weight = 0.0;
    final List<ReactionProductCandidate> candidates = prediction.candidates();
    for (int i = 0; i < candidates.size(); ++i) {
      weight += candidates.get(i).scoreBreakdown().negativeWeight();
    }
    return weight;
  }

  private static double totalScoreComponentWeight(
      final ReactionPredictionResult prediction, final ReactionPredictionScoreComponentKind kind) {
    double weight = 0.0;
    final List<ReactionProductCandidate> candidates = prediction.candidates();
    for (int i = 0; i < candidates.size(); ++i) {
      weight += candidates.get(i).scoreBreakdown().componentWeight(kind);
    }
    return weight;
  }

  private static String productDistributionAuditStatus(
      final Reaction reaction, final ReactionPredictionResult prediction) {
    if (!prediction.hasCandidates()) {
      return "NO_PRODUCT_CANDIDATES";
    }
    final boolean hasElectronic =
        AdvancedChemistryPropertyExecutor.totalEvidenceWeight(
                prediction, ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR)
            != 0.0;
    final boolean hasKinetic =
        AdvancedChemistryPropertyExecutor.totalEvidenceWeight(
                prediction, ReactionPredictionEvidenceKind.KINETIC_FACTOR)
            != 0.0;
    final boolean hasThermodynamic =
        AdvancedChemistryPropertyExecutor.totalEvidenceWeight(
                prediction, ReactionPredictionEvidenceKind.THERMODYNAMIC_FACTOR)
            != 0.0;
    final boolean quantified =
        reaction.conditions().components().size() > 0 && prediction.probabilityMass() > 0.0;
    if (hasElectronic && hasKinetic && hasThermodynamic && quantified) {
      return "ELECTRONIC_KINETIC_THERMODYNAMIC_DISTRIBUTION";
    }
    if (hasKinetic && hasThermodynamic) {
      return "KINETIC_THERMODYNAMIC_DISTRIBUTION";
    }
    if (hasElectronic) {
      return "ELECTRONICALLY_RERANKED_DISTRIBUTION";
    }
    return "RULE_AND_CONDITION_DISTRIBUTION";
  }

  private static int userSeedProductChannelCount(final ReactionPredictionResult prediction) {
    int count = 0;
    final List<ReactionProductCandidate> candidates = prediction.candidates();
    for (int i = 0; i < candidates.size(); ++i) {
      if (candidates.get(i).hasEvidence(ReactionPredictionEvidenceKind.USER_SEED)) {
        ++count;
      }
    }
    return count;
  }

  private static int mergedProductChannelCount(final ReactionPredictionResult prediction) {
    int count = 0;
    final List<ReactionProductCandidate> candidates = prediction.candidates();
    for (int i = 0; i < candidates.size(); ++i) {
      if (AdvancedChemistryPropertyExecutor.hasEvidenceDescription(
          candidates.get(i), "Merged duplicate product channel")) {
        ++count;
      }
    }
    return count;
  }

  private static int surrogateProductChannelCount(final ReactionPredictionResult prediction) {
    int count = 0;
    final List<ReactionProductCandidate> candidates = prediction.candidates();
    for (int i = 0; i < candidates.size(); ++i) {
      if (candidates.get(i).hasEvidence(ReactionPredictionEvidenceKind.SURROGATE_PRODUCT)) {
        ++count;
      }
    }
    return count;
  }

  private static int explicitProductGraphCandidateCount(final ReactionPredictionResult prediction) {
    int count = 0;
    final List<ReactionProductCandidate> candidates = prediction.candidates();
    for (int i = 0; i < candidates.size(); ++i) {
      if (AdvancedChemistryPropertyExecutor.readyForElectronicRerank(candidates.get(i))) {
        ++count;
      }
    }
    return count;
  }

  private static double explicitProductGraphCoverageRatio(
      final ReactionPredictionResult prediction) {
    if (prediction.candidates().isEmpty()) {
      return 0.0;
    }
    return (double) AdvancedChemistryPropertyExecutor.explicitProductGraphCandidateCount(prediction)
        / (double) prediction.candidates().size();
  }

  private static int candidateReadyForElectronicRerankCount(
      final ReactionPredictionResult prediction) {
    int count = 0;
    final List<ReactionProductCandidate> candidates = prediction.candidates();
    for (int i = 0; i < candidates.size(); ++i) {
      if (AdvancedChemistryPropertyExecutor.readyForElectronicRerank(candidates.get(i))) {
        ++count;
      }
    }
    return count;
  }

  private static int candidateReadyForQuantumWorkflowCount(
      final ReactionPredictionResult prediction) {
    int count = 0;
    final List<ReactionProductCandidate> candidates = prediction.candidates();
    for (int i = 0; i < candidates.size(); ++i) {
      if (AdvancedChemistryPropertyExecutor.readyForQuantumWorkflow(candidates.get(i))) {
        ++count;
      }
    }
    return count;
  }

  private static boolean prepareQuantumWorkflows(final ClassicalCalculationRequest request) {
    return AdvancedChemistryPropertyExecutor.booleanOption(
        request, PREPARE_QUANTUM_WORKFLOWS_OPTION);
  }

  private static boolean executeQuantumWorkflowVqe(final ClassicalCalculationRequest request) {
    return AdvancedChemistryPropertyExecutor.booleanOption(
        request, EXECUTE_QUANTUM_WORKFLOW_VQE_OPTION);
  }

  private static int prepareQuantumWorkflowMaxCandidates(
      final ClassicalCalculationRequest request) {
    return AdvancedChemistryPropertyExecutor.integerOption(
        request, PREPARE_QUANTUM_WORKFLOWS_MAX_CANDIDATES_OPTION, 2);
  }

  private static int prepareQuantumWorkflowMaxParticipants(
      final ClassicalCalculationRequest request) {
    return AdvancedChemistryPropertyExecutor.integerOption(
        request, PREPARE_QUANTUM_WORKFLOWS_MAX_PARTICIPANTS_OPTION, 4);
  }

  private static boolean booleanOption(
      final ClassicalCalculationRequest request, final String name) {
    final List<ClassicalCalculationOption> options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (name.equals(option.name())) {
        return Boolean.parseBoolean(option.value());
      }
    }
    return false;
  }

  private static int integerOption(
      final ClassicalCalculationRequest request, final String name, final int defaultValue) {
    final List<ClassicalCalculationOption> options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (!name.equals(option.name())) {
        continue;
      }
      try {
        final int parsed = Integer.parseInt(option.value());
        if (parsed <= 0) {
          return defaultValue;
        }
        return parsed;
      } catch (final NumberFormatException exception) {
        return defaultValue;
      }
    }
    return defaultValue;
  }

  private List<String> candidateQuantumWorkflowPreparationRows(
      final ClassicalCalculationRequest request,
      final ReactionPredictionResult prediction,
      final List<ClassicalDiagnostic> diagnostics) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add(
        "rank\tparticipant\tmolecule\tstatus\tpreparation_mode\thamiltonian_completeness"
            + "\trequired_qubits\tpauli_terms\tvqe_ready\tvqe_status\tvqe_energy\tvqe_iterations"
            + "\tqpe_available\tworkflow_status\treason");
    if (!AdvancedChemistryPropertyExecutor.prepareQuantumWorkflows(request)) {
      rows.add(
          "0\t0\tnone\tdisabled\tnone\tnone\t0\t0\tfalse\tnone\tnone\t0\tfalse\tnone\tSet "
              + PREPARE_QUANTUM_WORKFLOWS_OPTION
              + "=true to prepare advanced prediction quantum workflows.");
      return List.copyOf(rows);
    }
    final List<ReactionProductCandidate> candidates = prediction.candidates();
    final int candidateLimit =
        Math.min(
            candidates.size(),
            AdvancedChemistryPropertyExecutor.prepareQuantumWorkflowMaxCandidates(request));
    final int participantLimit =
        AdvancedChemistryPropertyExecutor.prepareQuantumWorkflowMaxParticipants(request);
    for (int candidateIndex = 0; candidateIndex < candidates.size(); ++candidateIndex) {
      final ReactionProductCandidate candidate = candidates.get(candidateIndex);
      if (candidateIndex >= candidateLimit
          || !AdvancedChemistryPropertyExecutor.readyForQuantumWorkflow(candidate)) {
        rows.add(
            (candidateIndex + 1)
                + "\t0\t"
                + AdvancedChemistryPropertyExecutor.productSummary(candidate)
                + "\tskipped\tnone\tnone\t0\t0\tfalse\tnone\tnone\t0\tfalse\tnone\t"
                + AdvancedChemistryPropertyExecutor.followupBlockingReason(
                    candidate,
                    AdvancedChemistryPropertyExecutor.readyForElectronicRerank(candidate),
                    AdvancedChemistryPropertyExecutor.readyForQuantumWorkflow(candidate)));
        continue;
      }
      this.addCandidateQuantumWorkflowRows(
          rows, request, candidate, candidateIndex, participantLimit, diagnostics);
    }
    if (rows.size() == 1) {
      rows.add(
          "0\t0\tnone\tskipped\tnone\tnone\t0\t0\tfalse\tnone\tnone\t0\tfalse\tnone"
              + "\tNo product candidates were generated.");
    }
    return List.copyOf(rows);
  }

  private void addCandidateQuantumWorkflowRows(
      final ArrayList<String> rows,
      final ClassicalCalculationRequest request,
      final ReactionProductCandidate candidate,
      final int candidateIndex,
      final int participantLimit,
      final List<ClassicalDiagnostic> diagnostics) {
    final List<ReactionParticipant> participants = candidate.products().participants();
    final int checkedParticipantLimit = Math.min(participants.size(), participantLimit);
    for (int participantIndex = 0; participantIndex < participants.size(); ++participantIndex) {
      final Molecule molecule = participants.get(participantIndex).molecule();
      if (participantIndex >= checkedParticipantLimit) {
        rows.add(
            (candidateIndex + 1)
                + "\t"
                + (participantIndex + 1)
                + "\t"
                + molecule.displayName()
                + "\tskipped\tnone\tnone\t0\t0\tfalse\tnone\tnone\t0\tfalse\tnone"
                + "\tParticipant is outside configured preparation limit.");
        continue;
      }
      final ClassicalCalculationRequest preparationRequest =
          AdvancedChemistryPropertyExecutor.quantumPreparationRequest(
              request, molecule, candidateIndex, participantIndex);
      final ArrayList<ClassicalDiagnostic> localDiagnostics = new ArrayList<ClassicalDiagnostic>();
      final ClassicalResultSection section =
          this.quantumProgramPreparationExecutor.execute(preparationRequest, localDiagnostics);
      diagnostics.addAll(localDiagnostics);
      if (section == null) {
        rows.add(
            (candidateIndex + 1)
                + "\t"
                + (participantIndex + 1)
                + "\t"
                + molecule.displayName()
                + "\tfailed\tnone\tnone\t0\t0\tfalse\tnone\tnone\t0\tfalse\tnone\t"
                + AdvancedChemistryPropertyExecutor.diagnosticSummary(localDiagnostics));
        continue;
      }
      final ArrayList<ClassicalDiagnostic> vqeDiagnostics = new ArrayList<ClassicalDiagnostic>();
      final ClassicalResultSection vqeSection =
          AdvancedChemistryPropertyExecutor.executeCandidateVqeSection(
              request, preparationRequest, section, vqeDiagnostics);
      diagnostics.addAll(vqeDiagnostics);
      rows.add(
          (candidateIndex + 1)
              + "\t"
              + (participantIndex + 1)
              + "\t"
              + molecule.displayName()
              + "\tprepared\t"
              + AdvancedChemistryPropertyExecutor.resultText(section, "preparation_mode")
              + "\t"
              + AdvancedChemistryPropertyExecutor.resultText(section, "hamiltonian_completeness")
              + "\t"
              + AdvancedChemistryPropertyExecutor.resultText(
                  section, "requested_mapping_required_qubits")
              + "\t"
              + AdvancedChemistryPropertyExecutor.resultText(
                  section, "requested_mapping_pauli_term_count")
              + "\t"
              + AdvancedChemistryPropertyExecutor.resultText(section, "vqe_ready")
              + "\t"
              + AdvancedChemistryPropertyExecutor.resultText(vqeSection, "vqe_status")
              + "\t"
              + AdvancedChemistryPropertyExecutor.resultText(vqeSection, "vqe_best_energy")
              + "\t"
              + AdvancedChemistryPropertyExecutor.resultText(vqeSection, "vqe_iteration_count")
              + "\t"
              + AdvancedChemistryPropertyExecutor.resultText(section, "qpe_available")
              + "\t"
              + AdvancedChemistryPropertyExecutor.resultText(
                  section, "quantum_workflow_coverage_status")
              + "\t"
              + AdvancedChemistryPropertyExecutor.diagnosticSummary(
                  AdvancedChemistryPropertyExecutor.combinedDiagnostics(
                      localDiagnostics, vqeDiagnostics)));
    }
  }

  private static ClassicalResultSection executeCandidateVqeSection(
      final ClassicalCalculationRequest originalRequest,
      final ClassicalCalculationRequest preparationRequest,
      final ClassicalResultSection preparationSection,
      final List<ClassicalDiagnostic> diagnostics) {
    if (!AdvancedChemistryPropertyExecutor.executeQuantumWorkflowVqe(originalRequest)) {
      return null;
    }
    if (!"true"
        .equalsIgnoreCase(
            AdvancedChemistryPropertyExecutor.resultText(preparationSection, "vqe_ready"))) {
      return null;
    }
    final ClassicalCalculationRequest vqeRequest =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of(preparationRequest.id().value() + "_vqe"),
            preparationRequest.subject(),
            preparationRequest.method(),
            preparationRequest.basisSet(),
            preparationRequest.activeSpace(),
            List.of(ClassicalCalculationKind.QUANTUM_VQE_OPTIMIZATION),
            preparationRequest.options(),
            preparationRequest.metadata());
    return new QuantumVqeOptimizationExecutor().execute(vqeRequest, diagnostics);
  }

  private static List<ClassicalDiagnostic> combinedDiagnostics(
      final List<ClassicalDiagnostic> first, final List<ClassicalDiagnostic> second) {
    if (second.isEmpty()) {
      return first;
    }
    final ArrayList<ClassicalDiagnostic> combined =
        new ArrayList<ClassicalDiagnostic>(first.size() + second.size());
    combined.addAll(first);
    combined.addAll(second);
    return List.copyOf(combined);
  }

  private static ClassicalCalculationRequest quantumPreparationRequest(
      final ClassicalCalculationRequest request,
      final Molecule molecule,
      final int candidateIndex,
      final int participantIndex) {
    return ClassicalCalculationRequest.of(
        ClassicalCalculationId.of(
            request.id().value()
                + "_advanced_candidate_"
                + (candidateIndex + 1)
                + "_product_"
                + (participantIndex + 1)
                + "_quantum_preparation"),
        molecule,
        request.method(),
        request.basisSet(),
        null,
        List.of(ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION),
        request.options(),
        request.metadata());
  }

  private static String resultText(final ClassicalResultSection section, final String name) {
    if (section == null) {
      if ("vqe_iteration_count".equals(name)) {
        return "0";
      }
      return "none";
    }
    final ClassicalResultValue value = section.valueOrNull(name);
    if (value == null || value.valueCount() == 0) {
      return "missing";
    }
    return value.firstValue();
  }

  private static String diagnosticSummary(final List<ClassicalDiagnostic> diagnostics) {
    if (diagnostics.isEmpty()) {
      return "ok";
    }
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < diagnostics.size(); ++i) {
      if (i > 0) {
        builder.append("; ");
      }
      builder
          .append(diagnostics.get(i).code().name())
          .append(": ")
          .append(diagnostics.get(i).message());
    }
    return builder.toString();
  }

  private static boolean readyForElectronicRerank(final ReactionProductCandidate candidate) {
    return !candidate.hasEvidence(ReactionPredictionEvidenceKind.SURROGATE_PRODUCT)
        && candidate.products().participantCount() > 0;
  }

  private static boolean readyForQuantumWorkflow(final ReactionProductCandidate candidate) {
    return AdvancedChemistryPropertyExecutor.readyForElectronicRerank(candidate)
        && candidate.hasPredictedMechanisms()
        && candidate.hasScoreBreakdown();
  }

  private static String productGraphCoverageStatus(final ReactionPredictionResult prediction) {
    if (prediction.candidates().isEmpty() && prediction.mechanismChannels().isEmpty()) {
      return "EMPTY";
    }
    if (prediction.candidates().isEmpty()) {
      return "MECHANISM_CHANNELS_WITHOUT_PRODUCT_GRAPHS";
    }
    final int explicitCount =
        AdvancedChemistryPropertyExecutor.explicitProductGraphCandidateCount(prediction);
    if (explicitCount == 0) {
      return "MECHANISM_ONLY";
    }
    if (explicitCount == prediction.candidates().size()) {
      return "COMPLETE";
    }
    return "PARTIAL";
  }

  private static boolean productGraphFollowupRecommended(
      final ReactionPredictionResult prediction) {
    return !prediction.mechanismChannels().isEmpty()
        || (!prediction.candidates().isEmpty()
            && AdvancedChemistryPropertyExecutor.explicitProductGraphCandidateCount(prediction)
                < prediction.candidates().size());
  }

  private static String productGraphFollowupReason(final ReactionPredictionResult prediction) {
    if (prediction.candidates().isEmpty() && prediction.mechanismChannels().isEmpty()) {
      return "No product candidates were generated.";
    }
    if (prediction.candidates().isEmpty()) {
      return "Recognized mechanism channels do not yet have explicit product graphs; provide graph transforms, seed products, or drawing-captured products before strict electronic rerank.";
    }
    if (!AdvancedChemistryPropertyExecutor.productGraphFollowupRecommended(prediction)) {
      return "All product channels have explicit product graphs.";
    }
    if (!prediction.mechanismChannels().isEmpty()) {
      return "Some recognized mechanism channels still need explicit product graphs, and candidate channels should be checked before strict electronic rerank.";
    }
    return "Some product channels are mechanism-only; provide graph transforms, seed products, or drawing-captured products before strict electronic rerank.";
  }

  private static List<String> predictionFollowupRows(final ReactionPredictionResult prediction) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add(
        "rank\tproducts\tprobability\texplicit_product_graph\telectronic_rerank\tquantum_workflow\tnext_action\tblocking_reason");
    final List<ReactionProductCandidate> candidates = prediction.candidates();
    for (int i = 0; i < candidates.size(); ++i) {
      final ReactionProductCandidate candidate = candidates.get(i);
      final boolean explicitGraph =
          AdvancedChemistryPropertyExecutor.readyForElectronicRerank(candidate);
      final boolean electronicReady =
          AdvancedChemistryPropertyExecutor.readyForElectronicRerank(candidate);
      final boolean quantumReady =
          AdvancedChemistryPropertyExecutor.readyForQuantumWorkflow(candidate);
      rows.add(
          Integer.toString(i + 1)
              + "\t"
              + AdvancedChemistryPropertyExecutor.productSummary(candidate)
              + "\t"
              + candidate.probability()
              + "\t"
              + Boolean.toString(explicitGraph)
              + "\t"
              + AdvancedChemistryPropertyExecutor.followupElectronicAction(electronicReady)
              + "\t"
              + AdvancedChemistryPropertyExecutor.followupQuantumAction(quantumReady)
              + "\t"
              + AdvancedChemistryPropertyExecutor.followupNextAction(
                  candidate, electronicReady, quantumReady)
              + "\t"
              + AdvancedChemistryPropertyExecutor.followupBlockingReason(
                  candidate, electronicReady, quantumReady));
    }
    final List<ReactionMechanismChannelCandidate> channels = prediction.mechanismChannels();
    for (int i = 0; i < channels.size(); ++i) {
      final ReactionMechanismChannelCandidate channel = channels.get(i);
      rows.add(
          "mechanism-"
              + (i + 1)
              + "\t"
              + channel.channelId()
              + "\t"
              + channel.pathwayProbability()
              + "\tfalse\tPRODUCT_GRAPH_REQUIRED\tMECHANISM_TRACE_ONLY\tmaterialize product graph\t"
              + channel.materializationRequirement());
    }
    return List.copyOf(rows);
  }

  private static String followupElectronicAction(final boolean ready) {
    return ready ? "REACTION_ENERGY_RERANK_READY" : "PRODUCT_GRAPH_REQUIRED";
  }

  private static String followupQuantumAction(final boolean ready) {
    return ready ? "HAMILTONIAN_AND_QUANTUM_PROGRAM_PREP_READY" : "MECHANISM_TRACE_REQUIRED";
  }

  private static String followupNextAction(
      final ReactionProductCandidate candidate,
      final boolean electronicReady,
      final boolean quantumReady) {
    if (quantumReady) {
      return "prepare electronic Hamiltonian, choose active space, then emit Quantum IR/VQE/QPE workflow";
    }
    if (electronicReady) {
      return "run Java-native reaction-energy rerank, then attach mechanism score breakdown";
    }
    if (candidate.hasEvidence(ReactionPredictionEvidenceKind.SURROGATE_PRODUCT)) {
      return "replace mechanism-only channel with explicit product graph or user-seeded product";
    }
    return "add mechanism and score trace before strict downstream execution";
  }

  private static String followupBlockingReason(
      final ReactionProductCandidate candidate,
      final boolean electronicReady,
      final boolean quantumReady) {
    if (quantumReady) {
      return "ready";
    }
    if (candidate.hasEvidence(ReactionPredictionEvidenceKind.SURROGATE_PRODUCT)) {
      return "candidate has no explicit product graph";
    }
    if (candidate.products().participantCount() == 0) {
      return "candidate has no product molecule graph";
    }
    if (!candidate.hasPredictedMechanisms()) {
      return "candidate has no mechanism trace";
    }
    if (!candidate.hasScoreBreakdown()) {
      return "candidate has no score breakdown";
    }
    if (!electronicReady) {
      return "candidate is not ready for electronic rerank";
    }
    return "candidate needs quantum workflow preparation metadata";
  }

  private static String productSummary(final ReactionProductCandidate candidate) {
    final StringBuilder builder = new StringBuilder();
    final List<ReactionParticipant> participants = candidate.products().participants();
    for (int i = 0; i < participants.size(); ++i) {
      if (i > 0) {
        builder.append(" + ");
      }
      final ReactionParticipant participant = participants.get(i);
      builder.append(participant.coefficient().value());
      builder.append("*");
      builder.append(participant.molecule().formula().hillNotation());
    }
    if (builder.length() == 0) {
      return "none";
    }
    return builder.toString();
  }

  private static double productDistributionEntropy(final ReactionPredictionResult prediction) {
    double entropy = 0.0;
    for (int i = 0; i < prediction.candidates().size(); ++i) {
      final double probability = Math.max(0.0, prediction.candidates().get(i).probability());
      if (probability <= 0.0) {
        continue;
      }
      entropy -= probability * Math.log(probability);
    }
    return entropy;
  }

  private static double majorMinorGap(final ReactionPredictionResult prediction) {
    if (prediction.candidates().isEmpty()) {
      return 0.0;
    }
    final double major = prediction.candidates().get(0).probability();
    if (prediction.candidates().size() == 1) {
      return major;
    }
    return Math.max(0.0, major - prediction.candidates().get(1).probability());
  }

  private static double closeProductCompetitorCount(final ReactionPredictionResult prediction) {
    if (prediction.candidates().size() <= 1) {
      return 0.0;
    }
    final double major = prediction.candidates().get(0).probability();
    int count = 0;
    for (int i = 1; i < prediction.candidates().size(); ++i) {
      if (major - prediction.candidates().get(i).probability() <= 0.15) {
        ++count;
      }
    }
    return count;
  }

  private static String distributionShape(final ReactionPredictionResult prediction) {
    if (prediction.candidates().isEmpty()) {
      return "empty";
    }
    final double gap = AdvancedChemistryPropertyExecutor.majorMinorGap(prediction);
    if (gap >= 0.45) {
      return "dominant_major_product";
    }
    if (gap >= 0.18) {
      return "major_with_competitors";
    }
    return "competitive_distribution";
  }

  private static ClassicalCalculationRequest scenarioCalculationRequest(
      final ClassicalCalculationRequest request,
      final Reaction reaction,
      final ReactionConditionScenarioEvaluation evaluation) {
    final Reaction scenarioReaction =
        Reaction.of(
            ReactionId.of(
                IdentifierValue.fromText(reaction.id().value() + "_" + evaluation.scenario().id())),
            reaction.displayName() + " / " + evaluation.scenario().kind().name(),
            reaction.reactants(),
            reaction.products(),
            evaluation.scenario().conditions(),
            reaction.metadata());
    return ClassicalCalculationRequest.of(
        request.id(),
        scenarioReaction,
        request.method(),
        request.basisSet(),
        request.activeSpace(),
        request.calculationKinds(),
        request.options(),
        request.metadata());
  }

  private ReactionPredictionRequest predictionRequest(
      final ClassicalCalculationRequest request, final Reaction reaction, final String idSuffix) {
    return ReactionPredictionRequest.of(
        reaction.id().value() + idSuffix,
        reaction.reactants(),
        reaction.conditions(),
        this.predictionDomains(request, reaction),
        AdvancedChemistryPropertyExecutor.seedProducts(request, reaction),
        "Generated by advanced reaction planning.");
  }

  private double conditionSensitivityScore(
      final ReactionConditionScenarioEvaluationPlan scenarioEvaluation) {
    if (scenarioEvaluation.evaluations().isEmpty()) {
      return 0.0;
    }
    double minimumProbability = Double.POSITIVE_INFINITY;
    double maximumProbability = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < scenarioEvaluation.evaluations().size(); ++i) {
      final double probability = scenarioEvaluation.evaluations().get(i).bestProbability();
      minimumProbability = Math.min(minimumProbability, probability);
      maximumProbability = Math.max(maximumProbability, probability);
    }
    if (!Double.isFinite(minimumProbability) || !Double.isFinite(maximumProbability)) {
      return 0.0;
    }
    return Math.max(0.0, maximumProbability - minimumProbability);
  }

  private void addConditionScenarioValues(
      final List<ClassicalResultValue> values,
      final ReactionConditionScenarioEvaluationPlan scenarioEvaluation) {
    if (scenarioEvaluation.empty()) {
      values.add(ClassicalResultValue.text("condition_best_scenario_id", "none"));
      values.add(ClassicalResultValue.text("condition_best_scenario_kind", "none"));
      values.add(ClassicalResultValue.scalar("condition_best_probability", 0.0, "fraction"));
      values.add(ClassicalResultValue.scalar("condition_best_expected_yield", 0.0, "percent"));
      values.add(ClassicalResultValue.scalar("condition_best_electronic_weight", 0.0, "weight"));
      values.add(ClassicalResultValue.scalar("condition_best_calibration_weight", 0.0, "weight"));
      values.add(ClassicalResultValue.text("condition_best_primary_mechanism", "none"));
      return;
    }
    final ReactionConditionScenarioEvaluation bestEvaluation = scenarioEvaluation.bestEvaluation();
    values.add(
        ClassicalResultValue.text("condition_best_scenario_id", bestEvaluation.scenario().id()));
    values.add(
        ClassicalResultValue.text(
            "condition_best_scenario_kind", bestEvaluation.scenario().kind().name()));
    values.add(
        ClassicalResultValue.scalar(
            "condition_best_probability", bestEvaluation.bestProbability(), "fraction"));
    values.add(
        ClassicalResultValue.scalar(
            "condition_best_expected_yield", bestEvaluation.bestExpectedYieldPercent(), "percent"));
    values.add(
        ClassicalResultValue.scalar(
            "condition_best_electronic_weight",
            AdvancedChemistryPropertyExecutor.bestCandidateEvidenceWeight(
                bestEvaluation, ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR),
            "weight"));
    values.add(
        ClassicalResultValue.scalar(
            "condition_best_calibration_weight",
            AdvancedChemistryPropertyExecutor.bestCandidateEvidenceWeight(
                bestEvaluation, ReactionPredictionEvidenceKind.CALIBRATION_PRIOR),
            "weight"));
    values.add(
        ClassicalResultValue.text(
            "condition_best_primary_mechanism",
            AdvancedChemistryPropertyExecutor.bestCandidateMechanism(bestEvaluation)));
  }

  private String bestConditionScenarioLabel(
      final ReactionConditionScenarioEvaluationPlan scenarioEvaluation) {
    if (scenarioEvaluation.empty()) {
      return "no condition scenario produced a product prediction";
    }
    final ReactionConditionScenarioEvaluation bestEvaluation = scenarioEvaluation.bestEvaluation();
    return bestEvaluation.scenario().kind().name()
        + " probability="
        + bestEvaluation.bestProbability()
        + " yield="
        + bestEvaluation.bestExpectedYieldPercent()
        + "%";
  }

  private static List<ReactionProductCandidate> seedProducts(
      final ClassicalCalculationRequest request, final Reaction reaction) {
    if (!AdvancedChemistryPropertyExecutor.useProductSideAsSeed(request)) {
      return List.of();
    }
    if (reaction.products().participants().isEmpty()) {
      return List.of();
    }
    return List.of(
        ReactionProductCandidate.of(
            reaction.products(),
            0.25,
            25.0,
            "Declared product side used as user seed for advanced distribution analysis.",
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.USER_RULE,
                    "Advanced distribution product seed comes from the reaction product side.",
                    0.15))));
  }

  private static boolean useProductSideAsSeed(final ClassicalCalculationRequest request) {
    for (int i = 0; i < request.options().size(); ++i) {
      if (USE_PRODUCT_SIDE_AS_SEED_OPTION.equals(request.options().get(i).name())) {
        return Boolean.parseBoolean(request.options().get(i).value());
      }
    }
    return false;
  }

  private List<OrganicReactionDomain> predictionDomains(
      final ClassicalCalculationRequest request, final Reaction reaction) {
    final ArrayList<OrganicReactionDomain> domains = new ArrayList<OrganicReactionDomain>(4);
    if (this.sideContainsFeature(
            reaction.reactants(),
            ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureKind
                .HETEROCYCLE)
        || (AdvancedChemistryPropertyExecutor.useProductSideAsSeed(request)
            && this.sideContainsFeature(
                reaction.products(),
                ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureKind
                    .HETEROCYCLE))) {
      domains.add(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS);
      domains.add(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION);
    }
    if (this.hasAnyConditionName(
        reaction, "pd", "palladium", "nickel", "copper", "boronic", "stannane", "organozinc")) {
      domains.add(OrganicReactionDomain.CROSS_COUPLING);
    }
    if (this.hasAnyConditionName(reaction, "base", "koh", "k2co3", "carbonate", "alkoxide")
        || reaction.conditions().hasFeature(ReactionConditionFeatureKind.BASIC_MEDIUM)) {
      domains.add(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION);
    }
    if (this.hasAnyConditionName(reaction, "peroxide", "peracid", "ozone", "permanganate")
        || reaction.conditions().hasFeature(ReactionConditionFeatureKind.PHOTOCHEMICAL)
        || reaction.conditions().hasFeature(ReactionConditionFeatureKind.ELECTROCHEMICAL)) {
      domains.add(OrganicReactionDomain.OXIDATION_REDUCTION);
    }
    if (domains.isEmpty()) {
      domains.add(OrganicReactionDomain.UNKNOWN);
    }
    return List.copyOf(domains);
  }

  private boolean sideContainsFeature(
      final ReactionSide side,
      final ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureKind
          kind) {
    final List<ReactionParticipant> participants = side.participants();
    for (int i = 0; i < participants.size(); ++i) {
      if (this.featureAnalyzer.analyze(participants.get(i).molecule()).contains(kind)) {
        return true;
      }
    }
    return false;
  }

  private double majorProductProbability(
      final ReactionPredictionResult prediction,
      final double triggerCount,
      final double balancePenalty) {
    if (prediction.hasCandidates()) {
      return prediction.bestCandidate().probability();
    }
    return Math.max(0.05, 0.82 - triggerCount * 0.035 - balancePenalty * 0.2);
  }

  private double minorProductFamilyCount(
      final ReactionPredictionResult prediction,
      final double triggerCount,
      final long participantCount) {
    if (prediction.candidates().size() > 1) {
      return prediction.candidates().size() - 1.0;
    }
    return Math.max(1.0, triggerCount + participantCount - 1.0);
  }

  private double conservativeYieldFloor(
      final ReactionPredictionResult prediction,
      final double balancePenalty,
      final double hazardSignalCount) {
    final double predictedMajor = this.majorProductProbability(prediction, 0.0, 0.0);
    return Math.max(0.02, predictedMajor * 0.55 - balancePenalty * 0.2 - hazardSignalCount * 0.03);
  }

  private double optimisticYieldCeiling(
      final ReactionPredictionResult prediction,
      final double conditionAxisCount,
      final double balancePenalty) {
    final double predictedMajor = this.majorProductProbability(prediction, 0.0, 0.0);
    return Math.min(
        0.96,
        Math.max(0.2, predictedMajor + 0.17 + conditionAxisCount * 0.015 - balancePenalty * 0.1));
  }

  private double impurityFamilyCount(
      final ReactionPredictionResult prediction, final Reaction reaction) {
    if (prediction.candidates().size() > 1) {
      return prediction.candidates().size() - 1.0 + prediction.diagnosticCount() * 0.25;
    }
    return Math.max(1.0, this.mechanismTriggerCount(reaction) + this.hazardSignalCount(reaction));
  }

  private String bestPredictionProducts(final ReactionPredictionResult prediction) {
    if (!prediction.hasCandidates()) {
      return "none";
    }
    return this.reactionSideFormulaSummary(prediction.bestCandidate().products());
  }

  private List<String> productDistributionRows(
      final Reaction reaction, final ReactionPredictionResult prediction) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add(
        "rank\tprobability\tyield_percent\tproducts\tsource\tmechanism_count\tevidence_count"
            + "\telectronic_weight\tcalibration_weight\twarning_weight\tprimary_mechanism"
            + "\traw_score\tpositive_score_weight\tnegative_score_weight\tselectivity_score_weight"
            + "\tmerged_duplicate\tsurrogate_removed_signal\tevidence_kinds\tscore_components");
    final List<ReactionProductCandidate> candidates = prediction.candidates();
    for (int i = 0; i < candidates.size(); ++i) {
      final ReactionProductCandidate candidate = candidates.get(i);
      rows.add(
          (i + 1)
              + "\t"
              + candidate.probability()
              + "\t"
              + candidate.expectedYieldPercent()
              + "\t"
              + this.reactionSideFormulaSummary(candidate.products())
              + "\t"
              + AdvancedChemistryPropertyExecutor.candidateSource(candidate)
              + "\t"
              + candidate.predictedMechanismCount()
              + "\t"
              + candidate.evidence().size()
              + "\t"
              + candidate.evidenceWeight(ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR)
              + "\t"
              + candidate.evidenceWeight(ReactionPredictionEvidenceKind.CALIBRATION_PRIOR)
              + "\t"
              + candidate.evidenceWeight(ReactionPredictionEvidenceKind.WARNING)
              + "\t"
              + AdvancedChemistryPropertyExecutor.primaryMechanism(candidate)
              + "\t"
              + candidate.scoreBreakdown().rawScore()
              + "\t"
              + candidate.scoreBreakdown().positiveWeight()
              + "\t"
              + candidate.scoreBreakdown().negativeWeight()
              + "\t"
              + candidate
                  .scoreBreakdown()
                  .componentWeight(ReactionPredictionScoreComponentKind.SELECTIVITY)
              + "\t"
              + Boolean.toString(
                  AdvancedChemistryPropertyExecutor.hasEvidenceDescription(
                      candidate, "Merged duplicate product channel"))
              + "\t"
              + Boolean.toString(
                  candidate.hasEvidence(ReactionPredictionEvidenceKind.SURROGATE_PRODUCT))
              + "\t"
              + AdvancedChemistryPropertyExecutor.evidenceKinds(candidate)
              + "\t"
              + AdvancedChemistryPropertyExecutor.scoreComponentKinds(candidate));
    }
    return List.copyOf(rows);
  }

  private List<String> productDistributionAuditRows(
      final Reaction reaction, final ReactionPredictionResult prediction) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add(
        "rank\tproducts\tprobability\tyield_percent\tkinetic_weight\tthermodynamic_weight"
            + "\telectronic_weight\tselectivity_weight\tcalibration_weight\twarning_weight"
            + "\tcondition_fit\tmechanism_detail\tmass_balance\tconfidence\tlimiting_risk");
    final List<ReactionProductCandidate> candidates = prediction.candidates();
    for (int i = 0; i < candidates.size(); ++i) {
      final ReactionProductCandidate candidate = candidates.get(i);
      rows.add(
          (i + 1)
              + "\t"
              + this.reactionSideFormulaSummary(candidate.products())
              + "\t"
              + candidate.probability()
              + "\t"
              + candidate.expectedYieldPercent()
              + "\t"
              + candidate.evidenceWeight(ReactionPredictionEvidenceKind.KINETIC_FACTOR)
              + "\t"
              + candidate.evidenceWeight(ReactionPredictionEvidenceKind.THERMODYNAMIC_FACTOR)
              + "\t"
              + candidate.evidenceWeight(ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR)
              + "\t"
              + candidate
                  .scoreBreakdown()
                  .componentWeight(ReactionPredictionScoreComponentKind.SELECTIVITY)
              + "\t"
              + candidate.evidenceWeight(ReactionPredictionEvidenceKind.CALIBRATION_PRIOR)
              + "\t"
              + candidate.evidenceWeight(ReactionPredictionEvidenceKind.WARNING)
              + "\t"
              + AdvancedChemistryPropertyExecutor.conditionFitLabel(reaction, candidate)
              + "\t"
              + AdvancedChemistryPropertyExecutor.mechanismDetailLabel(candidate)
              + "\t"
              + AdvancedChemistryPropertyExecutor.massBalanceLabel(candidate)
              + "\t"
              + AdvancedChemistryPropertyExecutor.candidateConfidenceLabel(candidate)
              + "\t"
              + AdvancedChemistryPropertyExecutor.limitingRiskLabel(candidate));
    }
    if (candidates.isEmpty()) {
      rows.add(
          "none\tnone\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\tmissing\tmissing\tmissing\tlow\tno candidate");
    }
    return List.copyOf(rows);
  }

  private static String conditionFitLabel(
      final Reaction reaction, final ReactionProductCandidate candidate) {
    final double conditionWeight =
        candidate.evidenceWeight(ReactionPredictionEvidenceKind.SOLVENT_EFFECT)
            + candidate.evidenceWeight(ReactionPredictionEvidenceKind.TEMPERATURE_EFFECT)
            + candidate.evidenceWeight(ReactionPredictionEvidenceKind.CATALYST_EFFECT);
    if (conditionWeight > 0.18) {
      return "strong";
    }
    if (conditionWeight > 0.02 || reaction.conditions().components().size() > 2) {
      return "moderate";
    }
    if (reaction.conditions().empty()) {
      return "missing_conditions";
    }
    return "weak";
  }

  private static String mechanismDetailLabel(final ReactionProductCandidate candidate) {
    if (!candidate.hasPredictedMechanisms()) {
      return "none";
    }
    int detailedEventCount = 0;
    for (int i = 0; i < candidate.predictedMechanisms().size(); ++i) {
      if (candidate.predictedMechanisms().get(i).hasDetailedMechanism()) {
        detailedEventCount +=
            candidate.predictedMechanisms().get(i).detailedMechanism().eventCount();
      }
    }
    if (detailedEventCount >= 4) {
      return "detailed:" + detailedEventCount;
    }
    if (detailedEventCount > 0) {
      return "partial:" + detailedEventCount;
    }
    return "family_only:" + candidate.predictedMechanismCount();
  }

  private static String massBalanceLabel(final ReactionProductCandidate candidate) {
    if (AdvancedChemistryPropertyExecutor.hasEvidenceDescription(
        candidate, "conserves nearly all")) {
      return "strong";
    }
    if (AdvancedChemistryPropertyExecutor.hasEvidenceDescription(candidate, "conserves most")) {
      return "partial";
    }
    if (AdvancedChemistryPropertyExecutor.hasEvidenceDescription(candidate, "unexplained")) {
      return "incomplete";
    }
    return "not_assessed";
  }

  private static String candidateConfidenceLabel(final ReactionProductCandidate candidate) {
    final double positive = candidate.scoreBreakdown().positiveWeight();
    final double negative = Math.abs(candidate.scoreBreakdown().negativeWeight());
    final double gap = positive - negative;
    if (candidate.probability() >= 0.55 && gap > 0.20) {
      return "high";
    }
    if (candidate.probability() >= 0.25 && gap > -0.10) {
      return "medium";
    }
    return "low";
  }

  private static String limitingRiskLabel(final ReactionProductCandidate candidate) {
    if (candidate.hasEvidence(ReactionPredictionEvidenceKind.SURROGATE_PRODUCT)) {
      return "surrogate";
    }
    if (candidate.evidenceWeight(ReactionPredictionEvidenceKind.WARNING) < -0.25) {
      return "warning_limited";
    }
    if (candidate.evidenceWeight(ReactionPredictionEvidenceKind.COMPETING_PATHWAY) < -0.15) {
      return "competing_pathway";
    }
    if (candidate.scoreBreakdown().componentWeight(ReactionPredictionScoreComponentKind.SELECTIVITY)
        < -0.10) {
      return "selectivity_limited";
    }
    return "none";
  }

  private static String candidateSource(final ReactionProductCandidate candidate) {
    if (candidate.hasEvidence(ReactionPredictionEvidenceKind.SURROGATE_PRODUCT)) {
      return "surrogate";
    }
    if (AdvancedChemistryPropertyExecutor.hasEvidenceDescription(
        candidate, "Merged duplicate product channel")) {
      return "merged_rule_or_seed";
    }
    if (candidate.hasEvidence(ReactionPredictionEvidenceKind.USER_SEED)) {
      return "user_seed";
    }
    if (candidate.hasEvidence(ReactionPredictionEvidenceKind.USER_RULE)) {
      return "rule_generated_graph";
    }
    if (candidate.hasPredictedMechanisms()) {
      return "mechanism_scored";
    }
    return "seed_or_external_candidate";
  }

  private static boolean hasEvidenceDescription(
      final ReactionProductCandidate candidate, final String fragment) {
    final List<ReactionPredictionEvidence> evidence = candidate.evidence();
    for (int i = 0; i < evidence.size(); ++i) {
      if (evidence.get(i).description().contains(fragment)) {
        return true;
      }
    }
    return false;
  }

  private static String primaryMechanism(final ReactionProductCandidate candidate) {
    if (!candidate.hasPredictedMechanisms()) {
      return "none";
    }
    return candidate.predictedMechanisms().get(0).family().name();
  }

  private static double bestCandidateEvidenceWeight(
      final ReactionConditionScenarioEvaluation evaluation,
      final ReactionPredictionEvidenceKind kind) {
    if (!evaluation.hasCandidates()) {
      return 0.0;
    }
    return evaluation.predictionResult().bestCandidate().evidenceWeight(kind);
  }

  private static String bestCandidateMechanism(
      final ReactionConditionScenarioEvaluation evaluation) {
    if (!evaluation.hasCandidates()) {
      return "none";
    }
    return AdvancedChemistryPropertyExecutor.primaryMechanism(
        evaluation.predictionResult().bestCandidate());
  }

  private static String evidenceKinds(final ReactionProductCandidate candidate) {
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < candidate.evidence().size(); ++i) {
      if (i > 0) {
        builder.append(",");
      }
      builder.append(candidate.evidence().get(i).kind().name());
    }
    if (builder.length() == 0) {
      return "none";
    }
    return builder.toString();
  }

  private static String scoreComponentKinds(final ReactionProductCandidate candidate) {
    final StringBuilder builder = new StringBuilder();
    final List<ReactionPredictionScoreComponent> components =
        candidate.scoreBreakdown().components();
    for (int i = 0; i < components.size(); ++i) {
      if (i > 0) {
        builder.append(",");
      }
      final ReactionPredictionScoreComponent component = components.get(i);
      builder.append(component.kind().name());
      builder.append(":");
      builder.append(component.weight());
    }
    if (builder.length() == 0) {
      return "none";
    }
    return builder.toString();
  }

  private static String microkineticCandidateBasis(final ReactionProductCandidate candidate) {
    final StringBuilder builder = new StringBuilder();
    builder.append("products=");
    builder.append(candidate.products().participantCount());
    builder.append(", positiveScore=");
    builder.append(candidate.scoreBreakdown().positiveWeight());
    builder.append(", negativeScore=");
    builder.append(candidate.scoreBreakdown().negativeWeight());
    builder.append(", evidence=");
    builder.append(AdvancedChemistryPropertyExecutor.evidenceKinds(candidate));
    return builder.toString();
  }

  private List<String> yieldWindowRows(
      final Reaction reaction, final ReactionPredictionResult prediction) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add("axis\tvalue\tbasis");
    rows.add(
        "major_probability\t"
            + this.majorProductProbability(prediction, 0.0, 0.0)
            + "\treaction prediction ranking");
    rows.add(
        "probability_mass\t"
            + prediction.probabilityMass()
            + "\tnormalized candidate distribution");
    rows.add("diagnostic_count\t" + prediction.diagnosticCount() + "\tprediction audit warnings");
    rows.add(
        "condition_axes\t"
            + this.conditionMatrixSizeEstimate(reaction)
            + "\tscreening matrix size");
    rows.add("hazard_signals\t" + this.hazardSignalCount(reaction) + "\tyield floor penalty");
    return List.copyOf(rows);
  }

  private List<String> sideProductRows(
      final Reaction reaction, final ReactionPredictionResult prediction) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add(
        "family\tprobability_or_signal\tyield_percent\tprimary_mechanism\telectronic_weight\tcalibration_weight\tbasis");
    final List<ReactionProductCandidate> candidates = prediction.candidates();
    for (int i = 1; i < candidates.size(); ++i) {
      final ReactionProductCandidate candidate = candidates.get(i);
      rows.add(
          "candidate_"
              + (i + 1)
              + "\t"
              + candidate.probability()
              + "\t"
              + candidate.expectedYieldPercent()
              + "\t"
              + AdvancedChemistryPropertyExecutor.primaryMechanism(candidate)
              + "\t"
              + candidate.evidenceWeight(ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR)
              + "\t"
              + candidate.evidenceWeight(ReactionPredictionEvidenceKind.CALIBRATION_PRIOR)
              + "\t"
              + this.reactionSideFormulaSummary(candidate.products()));
    }
    rows.add(
        "diagnostics\t"
            + prediction.diagnosticCount()
            + "\t0.0\tnone\t0.0\t0.0\tprediction uncertainty and missing metadata");
    rows.add(
        "condition_triggered\t"
            + this.mechanismTriggerCount(reaction)
            + "\t0.0\tnone\t0.0\t0.0\tnamed mechanism alternatives");
    rows.add(
        "hazard_linked\t"
            + this.hazardSignalCount(reaction)
            + "\t0.0\tnone\t0.0\t0.0\tdecomposition or unsafe side routes");
    rows.add(
        "unassigned_background_risk\t"
            + Math.max(1.0, reaction.conditions().components().size())
            + "\t0.0\tnone\t0.0\t0.0\tcondition-sensitive risk bucket, not a generated product candidate");
    return List.copyOf(rows);
  }

  private String reactionSideFormulaSummary(final ReactionSide side) {
    final StringBuilder builder = new StringBuilder();
    final List<ReactionParticipant> participants = side.participants();
    for (int i = 0; i < participants.size(); ++i) {
      if (i > 0) {
        builder.append(" + ");
      }
      final ReactionParticipant participant = participants.get(i);
      builder.append(participant.coefficient().value());
      builder.append("*");
      builder.append(participant.molecule().formula().hillNotation());
    }
    return builder.toString();
  }

  private List<String> basisConvergenceRows(final MolecularComposition composition) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add("basis\trole\twhy");
    rows.add("minimal\tfast sanity check\t" + composition.atomCount() + " atoms");
    rows.add(
        "split-valence\tmain organic estimate\t" + composition.heavyAtomCount() + " heavy atoms");
    rows.add(
        "polarized\tbonding and stereochemistry check\t"
            + Math.max(1L, composition.atomCount() + composition.heavyAtomCount())
            + " atom-derived scale");
    rows.add(
        "diffuse\tanion, lone-pair or excited-state sensitivity\t"
            + composition.heteroAtomCount()
            + " hetero atoms");
    return List.copyOf(rows);
  }

  private List<String> moleculePropertyProxyRows(
      final MolecularComposition composition,
      final Molecule molecule,
      final MolecularFeatureSet features) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add("property\tproxy\trecommended_route");
    rows.add(
        "dipole\t"
            + composition.heteroAtomCount()
            + " hetero atoms\tMOLECULAR_PROPERTY_ANALYSIS java_native SCF dipole estimate");
    rows.add(
        "partial_charges\t"
            + molecule.bondCount()
            + " bonds\tMOLECULAR_PROPERTY_ANALYSIS java_native Mulliken population");
    rows.add(
        "frontier_orbitals\t"
            + molecule.electronicConfiguration().electronCount()
            + " electrons\tMOLECULAR_PROPERTY_ANALYSIS SCF molecular orbital spectrum");
    rows.add(
        "reactive_features\t"
            + this.featureSummary(features)
            + "\tcombine feature analysis with native molecular properties");
    return List.copyOf(rows);
  }

  private List<String> methodBenchmarkRows(
      final MolecularComposition composition, final int electronCount) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add("method\trole\twhy");
    rows.add("HF\treference mean-field\t" + electronCount + " electrons");
    rows.add("DFT\tpractical organic estimate\tfunctional sensitivity");
    rows.add("MP2\tdispersion and correlation probe\tclosed-shell benchmark");
    rows.add("CCSD/FCI\thigh-level small active-space reference\tcritical ranking only");
    return List.copyOf(rows);
  }

  private List<String> moleculeUncertaintyRows(final MolecularFeatureSet features) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add("axis\tcurrent_signal\trecommended_check");
    rows.add("geometry\tconformer dependent\tconformer ensemble");
    rows.add("method\tfunctional/correlation dependent\tmethod benchmark");
    rows.add("basis\tbasis dependent\tbasis-set convergence");
    rows.add("features\t" + this.featureSummary(features) + "\tdescriptor sensitivity");
    return List.copyOf(rows);
  }

  private List<String> solventScreenRows(
      final Reaction reaction, final ReactionConditionScenarioEvaluationPlan scenarioEvaluation) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add("solvent_class\tobjective\tcomment");
    rows.add(
        "polar_aprotic\tincrease nucleophilicity and ion separation\t"
            + reaction.conditions().components().size()
            + " current components");
    rows.add("polar_protic\tstabilize ionic/proton-transfer paths\tcheck solvolysis risk");
    rows.add("nonpolar\tfavor neutral/pericyclic paths\tcheck solubility");
    rows.add("coordinating\tmodify metal/cation aggregation\tcheck catalyst inhibition");
    rows.add("green_or_scaleup\tpractical process screen\tcheck boiling point and safety");
    rows.add(
        "prediction_best_scenario\t"
            + this.bestConditionScenarioLabel(scenarioEvaluation)
            + "\tcomputed by condition scenario prediction");
    return List.copyOf(rows);
  }

  private List<String> catalystScreenRows(
      final Reaction reaction, final ReactionConditionScenarioEvaluationPlan scenarioEvaluation) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add("catalyst_class\tobjective\tcomment");
    rows.add("none\tbackground reaction baseline\tuncatalyzed control");
    rows.add("acid_or_lewis_acid\tactivate electrophiles\twatch rearrangement and polymerization");
    rows.add("base_or_organocatalyst\tactivate nucleophiles/enolates\twatch epimerization");
    rows.add(
        "pd_ni_cu\tcross-coupling and C-H activation\t"
            + this.mechanismTriggerCount(reaction)
            + " trigger signals");
    rows.add("photoredox_or_electrochemical\tsingle-electron pathway\tcheck redox window");
    rows.add(
        "prediction_best_scenario\t"
            + this.bestConditionScenarioLabel(scenarioEvaluation)
            + "\tcomputed by condition scenario prediction");
    return List.copyOf(rows);
  }

  private List<String> concentrationScreenRows(
      final Reaction reaction, final ReactionConditionScenarioEvaluationPlan scenarioEvaluation) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add("regime\tobjective\tcomment");
    rows.add("high_dilution\tfavor intramolecular pathways\tcyclization and macrocyclization");
    rows.add("standard\tbaseline\tcurrent condition reference");
    rows.add("concentrated\tfavor intermolecular collision\tpolymerization/oligomer risk");
    rows.add(
        "slow_addition\tcontrol reactive intermediate concentration\t"
            + reaction.conditions().features().size()
            + " condition features");
    rows.add(
        "prediction_best_scenario\t"
            + this.bestConditionScenarioLabel(scenarioEvaluation)
            + "\tcomputed by condition scenario prediction");
    return List.copyOf(rows);
  }

  private List<String> reactionBasisRows(final Reaction reaction) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add("basis\treaction_role\tcomment");
    rows.add(
        "minimal\tfast route ordering\t" + reaction.reactants().participantCount() + " reactants");
    rows.add(
        "split-valence\tmain product ranking\t"
            + reaction.products().participantCount()
            + " products");
    rows.add("polarized\ttransition state and selectivity\tbond-change sensitive");
    rows.add("diffuse\tcharged/radical/anion intermediates\tcondition dependent");
    return List.copyOf(rows);
  }

  private List<String> reactionMethodRows(final Reaction reaction) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add("method\treaction_role\tcomment");
    rows.add("HF\tmean-field baseline\tcheck severe qualitative errors");
    rows.add("DFT\tpractical route estimate\tfunctional sensitivity");
    rows.add("MP2\tdispersion/correlation check\tclosed-shell organic routes");
    rows.add(
        "CCSD/FCI\tcritical small active-space reference\t"
            + this.mechanismTriggerCount(reaction)
            + " mechanism triggers");
    return List.copyOf(rows);
  }

  private List<String> reactionUncertaintyRows(final Reaction reaction) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add("axis\tsource\treduction");
    rows.add("structure\tconformer and stereoisomer choice\tconformer + stereo ensemble");
    rows.add("conditions\tsolvent/catalyst/temperature\tcondition optimization");
    rows.add("mechanism\tcompeting named paths\tmechanism enumeration");
    rows.add("electronic method\tbasis/method dependence\tbasis convergence + method benchmark");
    rows.add(
        "experiment\tpurity, time and workup metadata\tstore measured outcome in reaction record");
    return List.copyOf(rows);
  }

  private List<String> mechanismEnumerationRows(final Reaction reaction) {
    final ArrayList<String> rows = new ArrayList<String>();
    final ReactionMechanismCatalogCoverageReport coverage =
        new ReactionMechanismCatalogCoverageAnalyzer()
            .analyze(BuiltInReactionMechanismPatternCatalog.organicAndPhysicalChemistry());
    rows.add("family_bucket\treason\tfollow_up");
    rows.add(
        "catalog_taxonomy\tcomplete="
            + coverage.taxonomyComplete()
            + "\tuse missing/duplicate/event diagnostics if false");
    rows.add(
        "catalog_production_detail\t"
            + coverage.productionDetailedPatternCount()
            + "/"
            + coverage.familyCount()
            + "\tproduction detailed pattern coverage ratio="
            + coverage.productionCoverageRatio());
    rows.add(
        "catalog_usable_detail\t"
            + coverage.chemicallyUsablePatternCount()
            + "/"
            + coverage.familyCount()
            + "\tchemically usable pattern coverage ratio="
            + coverage.chemicallyUsableCoverageRatio());
    rows.add(
        "catalog_generic_fallback\t"
            + coverage.syntheticGenericPatternCount()
            + " families\t"
            + AdvancedChemistryPropertyExecutor.textSummary(coverage.syntheticGenericPatternIds()));
    rows.add("declared_route\treactant/product context\ttransition-state search");
    rows.add(
        "condition_triggered\t"
            + reaction.conditions().components().size()
            + " condition components\tcompeting pathway analysis");
    rows.add("redox_radical\tlight/electrochemistry/oxidant signals\tmicrokinetic branch model");
    rows.add("acid_base_or_solvation\tmedium and proton-transfer signals\tpKa and solvent screen");
    rows.add(
        "decomposition_or_side_path\theat, light and instability signals\thazard/stability check");
    return List.copyOf(rows);
  }

  private static String textSummary(final List<String> values) {
    if (values.isEmpty()) {
      return "none";
    }
    final StringBuilder builder = new StringBuilder();
    final int limit = Math.min(values.size(), 12);
    for (int i = 0; i < limit; ++i) {
      if (i > 0) {
        builder.append(", ");
      }
      builder.append(values.get(i));
    }
    if (values.size() > limit) {
      builder.append(", ... ");
      builder.append(values.size() - limit);
      builder.append(" more");
    }
    return builder.toString();
  }

  private List<String> hazardRows(final Reaction reaction) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add("hazard_axis\tsignal\taction");
    rows.add(
        "thermal\t"
            + (reaction.conditions().temperature() == null
                ? "missing"
                : reaction.conditions().temperature().kelvin() + "K")
            + "\tthermal stability screen");
    rows.add(
        "redox\t"
            + this.hasAnyConditionName(
                reaction, "peroxide", "peracid", "ozone", "permanganate", "nitric")
            + "\toxidizer/reducer compatibility");
    rows.add("gas_pressure\tsealed or volatile conditions\tpressure and venting review");
    rows.add(
        "photochemical\t"
            + reaction
                .conditions()
                .hasFeature(
                    ru.pathcreator.vadim.quantum.chemistry.domain.reaction
                        .ReactionConditionFeatureKind.PHOTOCHEMICAL)
            + "\tlight/radical side-path review");
    rows.add(
        "scaleup\t"
            + reaction.conditions().components().size()
            + " condition components\tcalorimetry and addition-rate plan");
    return List.copyOf(rows);
  }

  private List<String> scaleUpRiskRows(final Reaction reaction) {
    final ArrayList<String> rows = new ArrayList<String>();
    final double totalGramScale = this.totalGramScale(reaction);
    final double solventGramScale = this.roleGramScale(reaction, ReactionComponentRole.SOLVENT);
    rows.add("axis\tsignal\treview");
    rows.add(
        "mass_scale\t"
            + totalGramScale
            + " g\tcheck heat removal, addition rate and sampling plan");
    rows.add(
        "solvent_load\t"
            + solventGramScale
            + " g\tcheck viscosity, stirring efficiency and phase behavior");
    rows.add(
        "largest_component\t"
            + this.largestComponentGramScale(reaction)
            + " g\tcheck local concentration and feed strategy");
    rows.add(
        "quantified_components\t"
            + this.quantifiedComponentCount(reaction)
            + "\tensure all reactants, solvent and additives have traceable recipe amounts");
    rows.add(
        "viscosity_proxy\t"
            + this.viscosityProxy(reaction, solventGramScale)
            + "\tpolyether/high-boiling media need mass-transfer review");
    rows.add(
        "heat_transfer_proxy\t"
            + this.heatTransferProxy(reaction, totalGramScale)
            + "\theated gram-scale recipes need thermal window review");
    return List.copyOf(rows);
  }

  private double totalGramScale(final Reaction reaction) {
    double result = 0.0;
    final List<ReactionConditionComponent> components = reaction.conditions().components();
    for (int i = 0; i < components.size(); ++i) {
      final ReactionConditionComponent component = components.get(i);
      if (!component.hasAmount() || !component.amount().massQuantity()) {
        continue;
      }
      result += component.amount().gramValue();
    }
    return result;
  }

  private double roleGramScale(final Reaction reaction, final ReactionComponentRole role) {
    double result = 0.0;
    final List<ReactionConditionComponent> components = reaction.conditions().components();
    for (int i = 0; i < components.size(); ++i) {
      final ReactionConditionComponent component = components.get(i);
      if (component.role() != role
          || !component.hasAmount()
          || !component.amount().massQuantity()) {
        continue;
      }
      result += component.amount().gramValue();
    }
    return result;
  }

  private double largestComponentGramScale(final Reaction reaction) {
    double result = 0.0;
    final List<ReactionConditionComponent> components = reaction.conditions().components();
    for (int i = 0; i < components.size(); ++i) {
      final ReactionConditionComponent component = components.get(i);
      if (!component.hasAmount() || !component.amount().massQuantity()) {
        continue;
      }
      result = Math.max(result, component.amount().gramValue());
    }
    return result;
  }

  private double quantifiedComponentCount(final Reaction reaction) {
    double result = 0.0;
    final List<ReactionConditionComponent> components = reaction.conditions().components();
    for (int i = 0; i < components.size(); ++i) {
      if (components.get(i).hasAmount()) {
        result += 1.0;
      }
    }
    return result;
  }

  private double viscosityProxy(final Reaction reaction, final double solventGramScale) {
    double result = Math.min(0.35, solventGramScale / 250.0);
    final List<ReactionConditionComponent> components = reaction.conditions().components();
    for (int i = 0; i < components.size(); ++i) {
      final String name = components.get(i).name().toLowerCase(java.util.Locale.ROOT);
      if (name.contains("peg")
          || name.contains("polyethylene glycol")
          || name.contains("polyether")
          || name.contains("glyme")
          || name.contains("viscous")) {
        result += 0.45;
      }
      if (name.contains("dmso")
          || name.contains("dmf")
          || name.contains("nmp")
          || name.contains("high-boiling")
          || name.contains("high boiling")) {
        result += 0.15;
      }
    }
    return Math.min(1.0, result);
  }

  private double heatTransferProxy(final Reaction reaction, final double totalGramScale) {
    double result = Math.min(0.40, totalGramScale / 300.0);
    if (reaction.conditions().temperature() != null
        && reaction.conditions().temperature().kelvin() >= 373.15) {
      result += 0.30;
    }
    if (reaction.conditions().hasFeature(ReactionConditionFeatureKind.HEATING)) {
      result += 0.12;
    }
    if (reaction.conditions().hasFeature(ReactionConditionFeatureKind.SEALED_VESSEL)
        || reaction.conditions().hasFeature(ReactionConditionFeatureKind.REFLUX)) {
      result += 0.18;
    }
    return Math.min(1.0, result);
  }

  private double hazardSignalCount(final Reaction reaction) {
    double count = 0.0;
    if (reaction.conditions().temperature() != null
        && reaction.conditions().temperature().kelvin() > 333.15) {
      ++count;
    }
    if (reaction
            .conditions()
            .hasFeature(
                ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind
                    .PHOTOCHEMICAL)
        || reaction
            .conditions()
            .hasFeature(
                ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind
                    .ELECTROCHEMICAL)
        || reaction
            .conditions()
            .hasFeature(
                ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind
                    .SEALED_VESSEL)) {
      ++count;
    }
    if (this.hasAnyConditionName(
        reaction, "peroxide", "peracid", "ozone", "permanganate", "nitric", "diazo", "azide")) {
      ++count;
    }
    return count;
  }

  private boolean hasAnyConditionName(final Reaction reaction, final String... fragments) {
    final List<ReactionConditionComponent> components = reaction.conditions().components();
    for (int componentIndex = 0; componentIndex < components.size(); ++componentIndex) {
      final String name = components.get(componentIndex).name().toLowerCase(java.util.Locale.ROOT);
      for (int fragmentIndex = 0; fragmentIndex < fragments.length; ++fragmentIndex) {
        if (name.contains(fragments[fragmentIndex])) {
          return true;
        }
      }
    }
    return false;
  }

  private String title(final ClassicalCalculationKind kind) {
    return kind.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
  }

  private String modelName(final ClassicalCalculationKind kind) {
    return "java_native_" + kind.name().toLowerCase(java.util.Locale.ROOT);
  }

  private String featureSummary(final MolecularFeatureSet features) {
    final StringBuilder builder = new StringBuilder();
    final List<MolecularFeature> items = features.features();
    for (int i = 0; i < items.size(); ++i) {
      final MolecularFeature feature = items.get(i);
      if (!feature.present()) {
        continue;
      }
      if (builder.length() > 0) {
        builder.append("; ");
      }
      builder.append(feature.kind().name());
      builder.append("=");
      builder.append(feature.count());
    }
    return builder.length() == 0 ? "none" : builder.toString();
  }
}

/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffect;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffectAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffectProfile;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenario;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioEvaluation;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioEvaluationPlan;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioEvaluator;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioPlan;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioPlanner;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.QuantumProgramPreparationExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.QuantumVqeOptimizationExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.feature.MolecularFeatureAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.reaction.analysis.ReactionPathwayAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.reaction.model.ReactionPathwayEstimate;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.result.ClassicalResultQualityValues;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.stoichiometry.ReactionStoichiometryAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.stoichiometry.ReactionStoichiometryComponent;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.stoichiometry.ReactionStoichiometryProfile;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticTarget;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureSet;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultSection;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValue;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValueKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism.ReactionMechanismCatalogCoverageReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism.ReactionMechanismPattern;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism.ReactionMechanismPatternCatalog;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism.ReactionMechanismPatternStep;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeCalibrationCoverageReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeBenchmarkMatch;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeBenchmarkReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeCalibrationLibrary;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.prediction.ReactionPredictionApplicabilityReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.prediction.ReactionPredictionUncertaintyReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.BuiltInOrganicReactionRuleLibraries;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.BuiltInReactionMechanismPatternCatalog;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.BuiltInReactionOutcomeCalibrationLibraries;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.PredictedReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionMechanismChannelCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionConditionWindow;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPathwayAssessment;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPathwayStep;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionExplanation;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionScoreComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionScoreComponentKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionId;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/**
 * Выполняет локальный прогноз продуктов реакции и упаковывает ранжирование, объяснение и риски в result section.
 */
public final class ReactionProductPredictionExecutor {

  private static final String USE_PRODUCT_SIDE_AS_SEED_OPTION =
      "reaction_prediction_use_product_side_as_seed";
  private static final String INCLUDE_BENCHMARK_AUDIT_OPTION =
      "reaction_prediction_include_benchmark_audit";
  private static final String APPLY_CALIBRATION_PRIORS_OPTION =
      "reaction_prediction_apply_calibration_priors";
  private static final String PREPARE_QUANTUM_WORKFLOWS_OPTION =
      "reaction_prediction_prepare_quantum_workflows";
  private static final String PREPARE_QUANTUM_WORKFLOWS_MAX_CANDIDATES_OPTION =
      "reaction_prediction_prepare_quantum_workflows_max_candidates";
  private static final String PREPARE_QUANTUM_WORKFLOWS_MAX_PARTICIPANTS_OPTION =
      "reaction_prediction_prepare_quantum_workflows_max_participants";
  private static final String EXECUTE_QUANTUM_WORKFLOW_VQE_OPTION =
      "reaction_prediction_execute_quantum_workflow_vqe";
  private static final int ELECTRONIC_RERANK_PREFLIGHT_ROW_LIMIT = 64;
  private final OrganicReactionPredictionService predictionService;
  private final MolecularFeatureAnalyzer featureAnalyzer;
  private final ReactionPredictionExplainer explainer;
  private final ReactionMechanismPatternCatalog mechanismCatalog;
  private final ReactionPredictionElectronicReranker electronicReranker;
  private final QuantumProgramPreparationExecutor quantumProgramPreparationExecutor;
  private final ReactionPathwayAnalyzer pathwayAnalyzer;
  private final ReactionOutcomeCalibrationLibrary benchmarkAuditLibrary;
  private final ReactionConditionEffectAnalyzer conditionEffectAnalyzer;
  private final ReactionPredictionApplicabilityAnalyzer applicabilityAnalyzer;
  private final ReactionStoichiometryAnalyzer stoichiometryAnalyzer;
  private final ReactionPredictionFollowUpPlanner followUpPlanner;
  private final ReactionPredictionUncertaintyAnalyzer uncertaintyAnalyzer;
  private final ReactionConditionScenarioPlanner conditionScenarioPlanner;
  private final ReactionConditionScenarioEvaluator conditionScenarioEvaluator;

  public ReactionProductPredictionExecutor() {
    this(
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.comprehensiveOrganicProductPrediction()),
            new OrganicReactionCandidateScorer(new MolecularFeatureAnalyzer())),
        new MolecularFeatureAnalyzer(),
        new ReactionPredictionExplainer(),
        BuiltInReactionMechanismPatternCatalog.organicAndPhysicalChemistry(),
        new ReactionPredictionElectronicReranker(),
        new QuantumProgramPreparationExecutor(),
        BuiltInReactionOutcomeCalibrationLibraries.heterocycleProductDistributions());
  }

  public ReactionProductPredictionExecutor(
      final OrganicReactionPredictionService predictionService,
      final MolecularFeatureAnalyzer featureAnalyzer) {
    this(
        predictionService,
        featureAnalyzer,
        new ReactionPredictionExplainer(),
        BuiltInReactionMechanismPatternCatalog.organicAndPhysicalChemistry(),
        new ReactionPredictionElectronicReranker(),
        new QuantumProgramPreparationExecutor(),
        ReactionOutcomeCalibrationLibrary.EMPTY);
  }

  public ReactionProductPredictionExecutor(
      final OrganicReactionPredictionService predictionService,
      final MolecularFeatureAnalyzer featureAnalyzer,
      final ReactionPredictionExplainer explainer) {
    this(
        predictionService,
        featureAnalyzer,
        explainer,
        BuiltInReactionMechanismPatternCatalog.organicAndPhysicalChemistry(),
        new ReactionPredictionElectronicReranker(),
        new QuantumProgramPreparationExecutor(),
        ReactionOutcomeCalibrationLibrary.EMPTY);
  }

  public ReactionProductPredictionExecutor(
      final OrganicReactionPredictionService predictionService,
      final MolecularFeatureAnalyzer featureAnalyzer,
      final ReactionPredictionExplainer explainer,
      final ReactionMechanismPatternCatalog mechanismCatalog) {
    this(
        predictionService,
        featureAnalyzer,
        explainer,
        mechanismCatalog,
        new ReactionPredictionElectronicReranker(),
        new QuantumProgramPreparationExecutor(),
        ReactionOutcomeCalibrationLibrary.EMPTY);
  }

  public ReactionProductPredictionExecutor(
      final OrganicReactionPredictionService predictionService,
      final MolecularFeatureAnalyzer featureAnalyzer,
      final ReactionPredictionExplainer explainer,
      final ReactionMechanismPatternCatalog mechanismCatalog,
      final ReactionPredictionElectronicReranker electronicReranker) {
    this(
        predictionService,
        featureAnalyzer,
        explainer,
        mechanismCatalog,
        electronicReranker,
        new QuantumProgramPreparationExecutor(),
        ReactionOutcomeCalibrationLibrary.EMPTY);
  }

  public ReactionProductPredictionExecutor(
      final OrganicReactionPredictionService predictionService,
      final MolecularFeatureAnalyzer featureAnalyzer,
      final ReactionPredictionExplainer explainer,
      final ReactionMechanismPatternCatalog mechanismCatalog,
      final ReactionPredictionElectronicReranker electronicReranker,
      final ReactionOutcomeCalibrationLibrary benchmarkAuditLibrary) {
    this(
        predictionService,
        featureAnalyzer,
        explainer,
        mechanismCatalog,
        electronicReranker,
        new QuantumProgramPreparationExecutor(),
        benchmarkAuditLibrary);
  }

  public ReactionProductPredictionExecutor(
      final OrganicReactionPredictionService predictionService,
      final MolecularFeatureAnalyzer featureAnalyzer,
      final ReactionPredictionExplainer explainer,
      final ReactionMechanismPatternCatalog mechanismCatalog,
      final ReactionPredictionElectronicReranker electronicReranker,
      final QuantumProgramPreparationExecutor quantumProgramPreparationExecutor,
      final ReactionOutcomeCalibrationLibrary benchmarkAuditLibrary) {
    if (predictionService == null) {
      throw new IllegalArgumentException("Reaction product prediction service must not be null.");
    }
    if (featureAnalyzer == null) {
      throw new IllegalArgumentException("Reaction prediction feature analyzer must not be null.");
    }
    if (explainer == null) {
      throw new IllegalArgumentException("Reaction prediction explainer must not be null.");
    }
    if (mechanismCatalog == null) {
      throw new IllegalArgumentException("Reaction mechanism pattern catalog must not be null.");
    }
    if (electronicReranker == null) {
      throw new IllegalArgumentException(
          "Reaction prediction electronic reranker must not be null.");
    }
    if (quantumProgramPreparationExecutor == null) {
      throw new IllegalArgumentException(
          "Reaction prediction quantum preparation executor must not be null.");
    }
    if (benchmarkAuditLibrary == null) {
      throw new IllegalArgumentException("Reaction benchmark audit library must not be null.");
    }
    this.predictionService = predictionService;
    this.featureAnalyzer = featureAnalyzer;
    this.explainer = explainer;
    this.mechanismCatalog = mechanismCatalog;
    this.electronicReranker = electronicReranker;
    this.quantumProgramPreparationExecutor = quantumProgramPreparationExecutor;
    this.pathwayAnalyzer = new ReactionPathwayAnalyzer();
    this.benchmarkAuditLibrary = benchmarkAuditLibrary;
    this.conditionEffectAnalyzer = new ReactionConditionEffectAnalyzer();
    this.applicabilityAnalyzer =
        new ReactionPredictionApplicabilityAnalyzer(featureAnalyzer, mechanismCatalog);
    this.stoichiometryAnalyzer = new ReactionStoichiometryAnalyzer();
    this.followUpPlanner = new ReactionPredictionFollowUpPlanner();
    this.uncertaintyAnalyzer = new ReactionPredictionUncertaintyAnalyzer();
    this.conditionScenarioPlanner = new ReactionConditionScenarioPlanner();
    this.conditionScenarioEvaluator =
        new ReactionConditionScenarioEvaluator(
            this.conditionScenarioPlanner, this.predictionService);
  }

  public ClassicalResultSection execute(
      final ClassicalCalculationRequest request, final List<ClassicalDiagnostic> diagnostics) {
    if (request == null) {
      throw new IllegalArgumentException(
          "Reaction prediction calculation request must not be null.");
    }
    if (diagnostics == null) {
      throw new IllegalArgumentException("Reaction prediction diagnostic list must not be null.");
    }
    if (request.subject().subjectKind() != ChemistrySubjectKind.REACTION) {
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.PREDICTION_REQUIRES_REACTION_SUBJECT,
              ClassicalDiagnosticTarget.of("subject", request.subject().stableId()),
              "Reaction product prediction requires reaction subject."));
      return null;
    }
    final Reaction reaction = (Reaction) request.subject();
    final ReactionPredictionResult initialPredictionResult =
        this.predictionService.predict(this.requestFromReaction(request, reaction));
    final ReactionPredictionResult predictionResult =
        this.applyCalibrationPriors(
            request, this.electronicReranker.rerank(request, initialPredictionResult, diagnostics));
    this.addPredictionDiagnostics(predictionResult, diagnostics);
    if (!predictionResult.hasCandidates()) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
              ClassicalDiagnosticTarget.of(
                  "calculationKind", ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION.name()),
              "Reaction product prediction produced no ranked candidates."));
    }
    return this.resultSection(
        request,
        predictionResult,
        this.explainer.explain(predictionResult),
        diagnostics,
        ReactionProductPredictionExecutor.includeBenchmarkAudit(request),
        ReactionProductPredictionExecutor.applyCalibrationPriors(request));
  }

  private void addPredictionDiagnostics(
      final ReactionPredictionResult result, final List<ClassicalDiagnostic> diagnostics) {
    final List<ReactionPredictionEvidence> predictionDiagnostics = result.diagnostics();
    for (int i = 0; i < predictionDiagnostics.size(); ++i) {
      final ReactionPredictionEvidence evidence = predictionDiagnostics.get(i);
      if (evidence.weight() >= 0.0) {
        continue;
      }
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
              ClassicalDiagnosticTarget.of("prediction", evidence.kind().name()),
              evidence.description()));
    }
  }

  private ReactionPredictionRequest requestFromReaction(
      final ClassicalCalculationRequest request, final Reaction reaction) {
    return ReactionPredictionRequest.of(
        request.id().value() + "_product_prediction",
        reaction.reactants(),
        reaction.conditions(),
        this.domains(request, reaction),
        ReactionProductPredictionExecutor.useProductSideAsSeed(request)
            ? this.seedCandidates(reaction)
            : List.of(),
        "Generated from classical reaction calculation request.");
  }

  private static boolean useProductSideAsSeed(final ClassicalCalculationRequest request) {
    return ReactionProductPredictionExecutor.booleanOption(
        request, USE_PRODUCT_SIDE_AS_SEED_OPTION);
  }

  private static boolean includeBenchmarkAudit(final ClassicalCalculationRequest request) {
    return ReactionProductPredictionExecutor.booleanOption(request, INCLUDE_BENCHMARK_AUDIT_OPTION);
  }

  private static boolean applyCalibrationPriors(final ClassicalCalculationRequest request) {
    return ReactionProductPredictionExecutor.booleanOptionDefault(
        request, APPLY_CALIBRATION_PRIORS_OPTION, false);
  }

  private static boolean booleanOption(
      final ClassicalCalculationRequest request, final String name) {
    final List<ClassicalCalculationOption> options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (!name.equals(option.name())) {
        continue;
      }
      return Boolean.parseBoolean(option.value());
    }
    return false;
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

  private static boolean electronicRerankEnabled(final ClassicalCalculationRequest request) {
    return ReactionProductPredictionExecutor.booleanOptionDefault(
        request,
        "reaction_prediction_electronic_rerank",
        ReactionProductPredictionExecutor.booleanOptionDefault(
            request, "advanced_prediction_electronic_rerank", false));
  }

  private static int electronicRerankMaxCandidates(final ClassicalCalculationRequest request) {
    return ReactionProductPredictionExecutor.integerOptionDefault(
        request,
        "reaction_prediction_electronic_rerank_max_candidates",
        ReactionProductPredictionExecutor.integerOptionDefault(
            request, "advanced_prediction_electronic_rerank_max_candidates", 6));
  }

  private static int electronicRerankMaxAtoms(final ClassicalCalculationRequest request) {
    return ReactionProductPredictionExecutor.integerOptionDefault(
        request,
        "reaction_prediction_electronic_rerank_max_atoms",
        ReactionProductPredictionExecutor.integerOptionDefault(
            request, "advanced_prediction_electronic_rerank_max_atoms", 48));
  }

  private static boolean prepareQuantumWorkflows(final ClassicalCalculationRequest request) {
    return ReactionProductPredictionExecutor.booleanOptionDefault(
        request,
        PREPARE_QUANTUM_WORKFLOWS_OPTION,
        ReactionProductPredictionExecutor.booleanOptionDefault(
            request, "advanced_prediction_prepare_quantum_workflows", false));
  }

  private static boolean executeQuantumWorkflowVqe(final ClassicalCalculationRequest request) {
    return ReactionProductPredictionExecutor.booleanOptionDefault(
        request,
        EXECUTE_QUANTUM_WORKFLOW_VQE_OPTION,
        ReactionProductPredictionExecutor.booleanOptionDefault(
            request, "advanced_prediction_execute_quantum_workflow_vqe", false));
  }

  private static int prepareQuantumWorkflowMaxCandidates(
      final ClassicalCalculationRequest request) {
    return ReactionProductPredictionExecutor.integerOptionDefault(
        request,
        PREPARE_QUANTUM_WORKFLOWS_MAX_CANDIDATES_OPTION,
        ReactionProductPredictionExecutor.integerOptionDefault(
            request, "advanced_prediction_prepare_quantum_workflows_max_candidates", 2));
  }

  private static int prepareQuantumWorkflowMaxParticipants(
      final ClassicalCalculationRequest request) {
    return ReactionProductPredictionExecutor.integerOptionDefault(
        request,
        PREPARE_QUANTUM_WORKFLOWS_MAX_PARTICIPANTS_OPTION,
        ReactionProductPredictionExecutor.integerOptionDefault(
            request, "advanced_prediction_prepare_quantum_workflows_max_participants", 4));
  }

  private static int integerOptionDefault(
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

  private ReactionPredictionResult applyCalibrationPriors(
      final ClassicalCalculationRequest calculationRequest, final ReactionPredictionResult result) {
    return new ReactionPredictionCalibrationApplier(this.benchmarkAuditLibrary)
        .apply(
            result, ReactionProductPredictionExecutor.applyCalibrationPriors(calculationRequest));
  }

  private List<OrganicReactionDomain> domains(
      final ClassicalCalculationRequest request, final Reaction reaction) {
    final ArrayList<OrganicReactionDomain> domains = new ArrayList<OrganicReactionDomain>(4);
    final boolean productSideAvailable =
        ReactionProductPredictionExecutor.useProductSideAsSeed(request);
    if (this.containsHeterocycle(reaction.reactants())
        || (productSideAvailable && this.containsHeterocycle(reaction.products()))) {
      domains.add(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS);
      domains.add(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION);
    }
    if (ReactionProductPredictionExecutor.hasCrossCouplingConditionSignal(reaction)
        || (this.containsAnyFeature(
                reaction.reactants(),
                MolecularFeatureKind.ARYL_HALIDE,
                MolecularFeatureKind.HETEROARYL_HALIDE)
            && this.containsAnyFeature(
                reaction.reactants(),
                MolecularFeatureKind.BORONIC_ACID_OR_BORONATE,
                MolecularFeatureKind.ORGANOMETALLIC_REAGENT))) {
      ReactionProductPredictionExecutor.addIfMissing(domains, OrganicReactionDomain.CROSS_COUPLING);
    }
    if (ReactionProductPredictionExecutor.hasSubstitutionConditionSignal(reaction)
        && (this.containsHalogenatedMolecule(reaction.reactants())
            || (productSideAvailable && this.containsHalogenatedMolecule(reaction.products())))) {
      ReactionProductPredictionExecutor.addIfMissing(
          domains, OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION);
    }
    if (ReactionProductPredictionExecutor.hasElectrophilicAromaticConditionSignal(reaction)
        || this.containsAnyFeature(
            reaction.reactants(),
            MolecularFeatureKind.AROMATIC_RING,
            MolecularFeatureKind.PHENOL,
            MolecularFeatureKind.AMINE)) {
      ReactionProductPredictionExecutor.addIfMissing(
          domains, OrganicReactionDomain.ELECTROPHILIC_SUBSTITUTION);
    }
    if (this.containsAnyFeature(
        reaction.reactants(),
        MolecularFeatureKind.CARBONYL,
        MolecularFeatureKind.ALDEHYDE,
        MolecularFeatureKind.KETONE,
        MolecularFeatureKind.ACYL_HALIDE,
        MolecularFeatureKind.ANHYDRIDE,
        MolecularFeatureKind.ESTER,
        MolecularFeatureKind.AMIDE,
        MolecularFeatureKind.NITRILE,
        MolecularFeatureKind.ALKENE,
        MolecularFeatureKind.ALKYNE,
        MolecularFeatureKind.EPOXIDE)) {
      ReactionProductPredictionExecutor.addIfMissing(domains, OrganicReactionDomain.ADDITION);
      ReactionProductPredictionExecutor.addIfMissing(domains, OrganicReactionDomain.CYCLIZATION);
    }
    if (this.containsAnyFeature(
        reaction.reactants(),
        MolecularFeatureKind.ALKENE,
        MolecularFeatureKind.ALKYNE,
        MolecularFeatureKind.HALOGENATED)) {
      ReactionProductPredictionExecutor.addIfMissing(domains, OrganicReactionDomain.ELIMINATION);
    }
    if (this.containsAnyFeature(reaction.reactants(), MolecularFeatureKind.RADICAL_CENTER)
        || reaction.conditions().hasFeature(ReactionConditionFeatureKind.PHOTOCHEMICAL)
        || reaction.conditions().hasFeature(ReactionConditionFeatureKind.ELECTROCHEMICAL)
        || ReactionProductPredictionExecutor.hasRadicalConditionSignal(reaction)) {
      ReactionProductPredictionExecutor.addIfMissing(
          domains, OrganicReactionDomain.RADICAL_FUNCTIONALIZATION);
    }
    if (this.containsAnyFeature(
            reaction.reactants(),
            MolecularFeatureKind.ALCOHOL,
            MolecularFeatureKind.PHENOL,
            MolecularFeatureKind.AMINE,
            MolecularFeatureKind.CARBOXYLIC_ACID_OR_CARBOXYLATE)
        || reaction.conditions().hasFeature(ReactionConditionFeatureKind.ACIDIC_MEDIUM)
        || reaction.conditions().hasFeature(ReactionConditionFeatureKind.BASIC_MEDIUM)) {
      ReactionProductPredictionExecutor.addIfMissing(domains, OrganicReactionDomain.ACID_BASE);
    }
    if (ReactionProductPredictionExecutor.hasRedoxConditionSignal(reaction)
        || this.containsAnyFeature(
            reaction.reactants(),
            MolecularFeatureKind.NITRO,
            MolecularFeatureKind.SULFUR_OXIDE,
            MolecularFeatureKind.CARBONYL,
            MolecularFeatureKind.NITRILE)) {
      ReactionProductPredictionExecutor.addIfMissing(
          domains, OrganicReactionDomain.OXIDATION_REDUCTION);
    }
    if (ReactionProductPredictionExecutor.hasProtectionConditionSignal(reaction)
        || this.containsAnyFeature(
            reaction.reactants(),
            MolecularFeatureKind.ALCOHOL,
            MolecularFeatureKind.PHENOL,
            MolecularFeatureKind.AMINE,
            MolecularFeatureKind.CARBONYL)) {
      ReactionProductPredictionExecutor.addIfMissing(
          domains, OrganicReactionDomain.PROTECTION_DEPROTECTION);
    }
    if (ReactionProductPredictionExecutor.hasRearrangementConditionSignal(reaction)
        || this.containsAnyFeature(
            reaction.reactants(),
            MolecularFeatureKind.EPOXIDE,
            MolecularFeatureKind.IMINE,
            MolecularFeatureKind.NITRILE,
            MolecularFeatureKind.HETEROCYCLE)) {
      ReactionProductPredictionExecutor.addIfMissing(domains, OrganicReactionDomain.REARRANGEMENT);
    }
    if (domains.isEmpty()) {
      return List.of(OrganicReactionDomain.UNKNOWN);
    }
    return List.copyOf(domains);
  }

  private static void addIfMissing(
      final List<OrganicReactionDomain> domains, final OrganicReactionDomain domain) {
    if (!domains.contains(domain)) {
      domains.add(domain);
    }
  }

  private boolean containsHeterocycle(final ReactionSide side) {
    final List<ReactionParticipant> participants = side.participants();
    for (int i = 0; i < participants.size(); ++i) {
      final MolecularFeatureSet features =
          this.featureAnalyzer.analyze(participants.get(i).molecule());
      if (features.contains(MolecularFeatureKind.HETEROCYCLE)
          || features.contains(MolecularFeatureKind.NITROGEN_HETEROCYCLE)
          || features.contains(MolecularFeatureKind.OXYGEN_HETEROCYCLE)
          || features.contains(MolecularFeatureKind.SULFUR_HETEROCYCLE)) {
        return true;
      }
    }
    return false;
  }

  private boolean containsHalogenatedMolecule(final ReactionSide side) {
    final List<ReactionParticipant> participants = side.participants();
    for (int i = 0; i < participants.size(); ++i) {
      final MolecularFeatureSet features =
          this.featureAnalyzer.analyze(participants.get(i).molecule());
      if (features.contains(MolecularFeatureKind.HALOGENATED)) {
        return true;
      }
    }
    return false;
  }

  private boolean containsAnyFeature(final ReactionSide side, final MolecularFeatureKind... kinds) {
    final List<ReactionParticipant> participants = side.participants();
    for (int i = 0; i < participants.size(); ++i) {
      final MolecularFeatureSet features =
          this.featureAnalyzer.analyze(participants.get(i).molecule());
      for (int kindIndex = 0; kindIndex < kinds.length; ++kindIndex) {
        if (features.contains(kinds[kindIndex])) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean hasCrossCouplingConditionSignal(final Reaction reaction) {
    final List<ReactionConditionComponent> catalysts =
        reaction.conditions().componentsByRole(ReactionComponentRole.CATALYST);
    for (int i = 0; i < catalysts.size(); ++i) {
      final String catalyst = catalysts.get(i).name().toLowerCase(Locale.ROOT);
      if (catalyst.contains("pd")
          || catalyst.contains("palladium")
          || catalyst.contains("ni")
          || catalyst.contains("nickel")
          || catalyst.contains("cu")
          || catalyst.contains("copper")) {
        return true;
      }
    }
    return reaction.conditions().hasFeature(ReactionConditionFeatureKind.CATALYST_PRESENT);
  }

  private static boolean hasSubstitutionConditionSignal(final Reaction reaction) {
    return reaction.conditions().hasFeature(ReactionConditionFeatureKind.BASIC_MEDIUM)
        || !reaction.conditions().componentsByRole(ReactionComponentRole.BASE).isEmpty()
        || reaction.conditions().hasFeature(ReactionConditionFeatureKind.HEATING);
  }

  private static boolean hasRedoxConditionSignal(final Reaction reaction) {
    return ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        reaction.conditions(),
        "oxid",
        "peroxide",
        "mcpba",
        "nbs",
        "kmno4",
        "chrom",
        "pcc",
        "dess",
        "martin",
        "reduce",
        "reduct",
        "hydride",
        "borohydride",
        "lialh4",
        "h2",
        "hydrogen");
  }

  private static boolean hasRadicalConditionSignal(final Reaction reaction) {
    return ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        reaction.conditions(),
        "radical",
        "aibn",
        "peroxide",
        "benzoyl peroxide",
        "photoinitiator",
        "persulfate",
        "minisci",
        "tributyltin hydride",
        "bu3snh",
        "thiol ene",
        "thiol-ene");
  }

  private static boolean hasElectrophilicAromaticConditionSignal(final Reaction reaction) {
    return ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        reaction.conditions(),
        "nitration",
        "nitric",
        "hno3",
        "nitronium",
        "sulfonation",
        "so3",
        "friedel",
        "alcl3",
        "fecl3",
        "bromination",
        "chlorination",
        "iodination",
        "halogenation",
        "diazonium",
        "azo coupling");
  }

  private static boolean hasProtectionConditionSignal(final Reaction reaction) {
    return ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        reaction.conditions(),
        "protect",
        "deprotect",
        "boc",
        "cbz",
        "fmoc",
        "tosyl",
        "silyl",
        "tbs",
        "tbdms",
        "acetal",
        "ketal",
        "hydrolysis");
  }

  private static boolean hasRearrangementConditionSignal(final Reaction reaction) {
    return ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        reaction.conditions(),
        "rearrangement",
        "beckmann",
        "baeyer",
        "villiger",
        "pinacol",
        "claisen",
        "cope",
        "sigmatropic",
        "photochemical",
        "ring opening",
        "ring closing",
        "anrorc");
  }

  private List<ReactionProductCandidate> seedCandidates(final Reaction reaction) {
    return List.of(
        ReactionProductCandidate.of(
            reaction.products(),
            0.82,
            70.0,
            "Product side supplied by reaction subject.",
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.USER_RULE,
                    "Reaction subject product side is used as an explicitly requested user-declared seed candidate.",
                    0.28)),
            List.of(
                PredictedReactionMechanism.of(
                    reaction.id().value() + "_subject_pathway",
                    this.mechanismFamily(reaction),
                    0.82,
                    70.0))));
  }

  private ReactionMechanismFamily mechanismFamily(final Reaction reaction) {
    final ReactionMechanismFamily namedFamily =
        ReactionProductPredictionExecutor.namedMechanismFamily(reaction.conditions());
    if (namedFamily != ReactionMechanismFamily.UNKNOWN) {
      return namedFamily;
    }
    if (ReactionProductPredictionExecutor.hasSubstitutionConditionSignal(reaction)
        && (this.containsHeterocycle(reaction.reactants())
            || this.containsHeterocycle(reaction.products()))
        && (this.containsHalogenatedMolecule(reaction.reactants())
            || this.containsHalogenatedMolecule(reaction.products()))) {
      return ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION;
    }
    if (ReactionProductPredictionExecutor.hasCrossCouplingConditionSignal(reaction)) {
      return ReactionMechanismFamily.ORGANOMETALLIC_CROSS_COUPLING;
    }
    if (ReactionProductPredictionExecutor.hasSubstitutionConditionSignal(reaction)) {
      return ReactionMechanismFamily.NUCLEOPHILIC_SUBSTITUTION_SN2;
    }
    if (reaction.conditions().hasFeature(ReactionConditionFeatureKind.PHOTOCHEMICAL)) {
      return ReactionMechanismFamily.PHOTOCHEMICAL;
    }
    if (reaction.conditions().hasFeature(ReactionConditionFeatureKind.ELECTROCHEMICAL)) {
      return ReactionMechanismFamily.ELECTROCHEMICAL;
    }
    if (this.containsHeterocycle(reaction.reactants())
        || this.containsHeterocycle(reaction.products())) {
      return ReactionMechanismFamily.HETEROCYCLE_FUNCTIONALIZATION;
    }
    return ReactionMechanismFamily.UNKNOWN;
  }

  private static ReactionMechanismFamily namedMechanismFamily(final ReactionConditions conditions) {
    if (conditions == null || conditions.empty()) {
      return ReactionMechanismFamily.UNKNOWN;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "boronic", "boronate", "suzuki")) {
      return ReactionMechanismFamily.SUZUKI_COUPLING;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "terminal alkyne", "cui", "sonogashira")) {
      return ReactionMechanismFamily.SONOGASHIRA_COUPLING;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "organozinc", "negishi")) {
      return ReactionMechanismFamily.NEGISHI_COUPLING;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "stannane", "stille")) {
      return ReactionMechanismFamily.STILLE_COUPLING;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "alkene", "heck")) {
      return ReactionMechanismFamily.HECK_COUPLING;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "buchwald", "hartwig", "palladium amination", "pd amination")) {
      return ReactionMechanismFamily.BUCHWALD_HARTWIG_AMINATION;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "grignard", "organolithium", "lithium reagent", "rmgx")) {
      return ReactionMechanismFamily.GRIGNARD_OR_ORGANOLITHIUM_ADDITION;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "wittig", "phosphonium ylide", "ylide")) {
      return ReactionMechanismFamily.WITTIG_REACTION;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "horner", "wadsworth", "emmons", "phosphonate")) {
      return ReactionMechanismFamily.HORNER_WADSWORTH_EMMONS;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "mitsunobu", "diad", "dead", "pph3")) {
      return ReactionMechanismFamily.MITSUNOBU_REACTION;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "mannich", "formaldehyde", "iminium")) {
      return ReactionMechanismFamily.MANNICH_REACTION;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "strecker", "cyanide", "hcn")) {
      return ReactionMechanismFamily.STRECKER_SYNTHESIS;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "ugi", "isocyanide")) {
      return ReactionMechanismFamily.UGI_REACTION;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(conditions, "passerini")) {
      return ReactionMechanismFamily.PASSERINI_REACTION;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "ozone", "o3", "ozonolysis")) {
      return ReactionMechanismFamily.OZONOLYSIS;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "mcpba", "peracid", "baeyer")) {
      return ReactionMechanismFamily.BAEYER_VILLIGER;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "epoxidation", "oxone", "dmdo")) {
      return ReactionMechanismFamily.EPOXIDATION;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "swern", "dmso oxalyl chloride")) {
      return ReactionMechanismFamily.SWERN_OXIDATION;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "dess", "martin", "dmp")) {
      return ReactionMechanismFamily.DESS_MARTIN_OXIDATION;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "pcc", "pyridinium chlorochromate")) {
      return ReactionMechanismFamily.PCC_OXIDATION;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "jones", "chromic acid", "cro3")) {
      return ReactionMechanismFamily.JONES_OXIDATION;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "nabh4", "lialh4", "dibal", "hydride")) {
      return ReactionMechanismFamily.HYDRIDE_REDUCTION;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "wolff", "kishner", "hydrazine")) {
      return ReactionMechanismFamily.WOLFF_KISHNER_REDUCTION;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "clemmensen", "zn hg", "znhg")) {
      return ReactionMechanismFamily.CLEMMENSEN_REDUCTION;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "hydroboration", "bh3", "9-bbn")) {
      return ReactionMechanismFamily.HYDROBORATION_OXIDATION;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "osmium", "oso4", "dihydroxylation")) {
      return ReactionMechanismFamily.DIHYDROXYLATION;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "nitric", "hno3", "nitronium")) {
      return ReactionMechanismFamily.NITRATION;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "sulfuric", "so3", "sulfonation")) {
      return ReactionMechanismFamily.SULFONATION;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "friedel", "alcl3", "fecl3")) {
      return ReactionMechanismFamily.FRIEDEL_CRAFTS_ACYLATION;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "diazonium", "nano2", "sandmeyer")) {
      return ReactionMechanismFamily.SANDMEYER;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "diels", "dienophile")) {
      return ReactionMechanismFamily.DIELS_ALDER;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "azide", "alkyne", "click")) {
      return ReactionMechanismFamily.CLICK_CYCLOADDITION;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "metathesis", "grubbs", "hoveyda")) {
      return ReactionMechanismFamily.OLEFIN_METATHESIS;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "acetal", "ketal")) {
      return ReactionMechanismFamily.ACETAL_KETAL_FORMATION;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(conditions, "biginelli")) {
      return ReactionMechanismFamily.BIGINELLI_REACTION;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(conditions, "hantzsch")) {
      return ReactionMechanismFamily.HANTZSCH_SYNTHESIS;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(conditions, "skraup")) {
      return ReactionMechanismFamily.SKRAUP_SYNTHESIS;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "fischer indole", "phenylhydrazine")) {
      return ReactionMechanismFamily.FISCHER_INDOLE_SYNTHESIS;
    }
    if (ReactionProductPredictionExecutor.hasAnyComponentNameContaining(
        conditions, "oxime", "beckmann")) {
      return ReactionMechanismFamily.BECKMANN_REARRANGEMENT;
    }
    return ReactionMechanismFamily.UNKNOWN;
  }

  private static boolean hasAnyComponentNameContaining(
      final ReactionConditions conditions, final String... fragments) {
    for (int i = 0; i < fragments.length; ++i) {
      if (ReactionProductPredictionExecutor.hasComponentNameContaining(conditions, fragments[i])) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasComponentNameContaining(
      final ReactionConditions conditions, final String fragment) {
    final String expected = fragment.toLowerCase(Locale.ROOT);
    final List<ReactionConditionComponent> components = conditions.components();
    for (int i = 0; i < components.size(); ++i) {
      if (components.get(i).name().toLowerCase(Locale.ROOT).contains(expected)) {
        return true;
      }
    }
    return false;
  }

  private ClassicalResultSection resultSection(
      final ClassicalCalculationRequest calculationRequest,
      final ReactionPredictionResult result,
      final ReactionPredictionExplanation explanation,
      final List<ClassicalDiagnostic> diagnostics,
      final boolean includeBenchmarkAudit,
      final boolean applyCalibrationPriors) {
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    final ReactionPathwayAssessment pathwayAssessment = explanation.pathwayAssessment();
    final ReactionConditionEffectProfile conditionEffectProfile =
        this.conditionEffectAnalyzer.analyze(result.request());
    final ReactionPredictionApplicabilityReport applicabilityReport =
        this.applicabilityAnalyzer.analyze(result, conditionEffectProfile);
    final ReactionStoichiometryProfile stoichiometryProfile =
        this.stoichiometryAnalyzer.analyze(result.request());
    final ReactionPredictionFollowUpPlan followUpPlan =
        this.followUpPlanner.plan(result, applicabilityReport, stoichiometryProfile);
    final ReactionConditionScenarioPlan conditionScenarioPlan =
        this.conditionScenarioPlanner.plan(result.request());
    final ReactionConditionScenarioEvaluationPlan conditionScenarioEvaluationPlan =
        this.conditionScenarioEvaluator.evaluate(conditionScenarioPlan, result.request());
    final ReactionMechanismCatalogCoverageReport mechanismCoverageReport =
        new ReactionMechanismCatalogCoverageAnalyzer().analyze(this.mechanismCatalog);
    final ReactionOutcomeCalibrationCoverageReport calibrationCoverageReport =
        new ReactionOutcomeCalibrationCoverageAnalyzer().analyze(this.benchmarkAuditLibrary);
    final ReactionPredictionUncertaintyReport uncertaintyReport =
        this.uncertaintyAnalyzer.analyze(result, applicabilityReport);
    final boolean effectiveCalibrationPriors =
        applyCalibrationPriors && !this.benchmarkAuditLibrary.empty();
    final ReactionOutcomeBenchmarkReport benchmarkReport =
        includeBenchmarkAudit
            ? new ReactionOutcomeBenchmarkEvaluator().evaluate(result, this.benchmarkAuditLibrary)
            : null;
    ClassicalResultQualityValues.addApproximate(
        values,
        "java_native",
        result.methodType().name(),
        "staged_mechanism_topology_condition_prediction",
        "Predictive ranking from reactants, conditions, mechanisms, stoichiometry and optional native electronic rerank; benchmark data is audit-only unless calibration priors are explicitly enabled.");
    values.add(ClassicalResultValue.text("method_type", result.methodType().name()));
    values.add(
        ClassicalResultValue.text(
            "prediction_source_contract",
            "reactants_conditions_staged_mechanisms_stoichiometry_and_optional_native_electronic_rerank"));
    values.add(
        ClassicalResultValue.text(
            "prediction_outcome_lookup_used",
            Boolean.toString(
                ReactionProductPredictionExecutor.calibrationPriorEvidenceCount(result.candidates())
                    > 0)));
    values.add(
        ClassicalResultValue.text(
            "prediction_benchmark_role",
            ReactionProductPredictionExecutor.benchmarkRole(
                includeBenchmarkAudit, effectiveCalibrationPriors)));
    values.add(
        ClassicalResultValue.text(
            "prediction_benchmark_data_flow",
            ReactionProductPredictionExecutor.benchmarkDataFlow(
                includeBenchmarkAudit, effectiveCalibrationPriors)));
    values.add(
        ClassicalResultValue.text(
            "prediction_calibration_prior_enabled", Boolean.toString(effectiveCalibrationPriors)));
    values.add(
        ClassicalResultValue.scalar(
            "calibration_library_observation_count",
            calibrationCoverageReport.observationCount(),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "calibration_library_dataset_count",
            calibrationCoverageReport.datasetCount(),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "calibration_library_exact_observation_count",
            calibrationCoverageReport.exactObservationCount(),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "calibration_library_externally_verified_exact_count",
            calibrationCoverageReport.externallyVerifiedExactObservationCount(),
            "count"));
    values.add(
        ClassicalResultValue.text(
            "calibration_library_empirical_prediction_ready",
            Boolean.toString(calibrationCoverageReport.productionReadyForEmpiricalPrediction())));
    values.add(
        ClassicalResultValue.text(
            "calibration_library_applied_to_prediction",
            Boolean.toString(
                effectiveCalibrationPriors
                    && calibrationCoverageReport.hasExactExperimentalCalibration())));
    values.add(
        ClassicalResultValue.text(
            "electronic_rerank_enabled",
            Boolean.toString(
                ReactionProductPredictionExecutor.electronicRerankEnabled(calculationRequest))));
    values.add(
        ClassicalResultValue.text(
            "prediction_execution_mode",
            ReactionProductPredictionExecutor.predictionExecutionMode(calculationRequest)));
    values.add(
        ClassicalResultValue.text(
            "electronic_rerank_opt_in_required",
            Boolean.toString(
                !ReactionProductPredictionExecutor.electronicRerankEnabled(calculationRequest))));
    values.add(
        ClassicalResultValue.scalar(
            "electronic_rerank_max_candidates",
            ReactionProductPredictionExecutor.electronicRerankMaxCandidates(calculationRequest),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "electronic_rerank_max_atoms",
            ReactionProductPredictionExecutor.electronicRerankMaxAtoms(calculationRequest),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "electronic_reranked_candidate_count",
            ReactionProductPredictionExecutor.candidateWithEvidenceKindCount(
                result.candidates(), ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "electronic_rerank_warning_candidate_count",
            ReactionProductPredictionExecutor.electronicRerankWarningCandidateCount(
                result.candidates()),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "electronic_rerank_skipped_by_limit_count",
            ReactionProductPredictionExecutor.electronicRerankSkippedByLimitCount(
                calculationRequest, result.candidates()),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "prediction_calibration_prior_evidence_count",
            ReactionProductPredictionExecutor.calibrationPriorEvidenceCount(result.candidates()),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "prediction_calibration_prior_weight",
            ReactionProductPredictionExecutor.calibrationPriorEvidenceWeight(result.candidates()),
            "score"));
    values.add(ClassicalResultValue.scalar("candidate_count", result.candidates().size(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "seed_candidate_count", result.request().seedCandidates().size(), "count"));
    values.add(
        ClassicalResultValue.text(
            "product_side_seed_used",
            Boolean.toString(!result.request().seedCandidates().isEmpty())));
    values.add(
        ClassicalResultValue.scalar(
            "generated_candidate_count",
            ReactionProductPredictionExecutor.ruleGeneratedProductChannelCount(result.candidates()),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "user_seed_candidate_count",
            ReactionProductPredictionExecutor.userSeedProductChannelCount(result.candidates()),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "surrogate_candidate_count",
            ReactionProductPredictionExecutor.surrogateProductChannelCount(result.candidates()),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "explicit_product_graph_candidate_count",
            ReactionProductPredictionExecutor.explicitProductGraphCandidateCount(
                result.candidates()),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "mechanism_only_candidate_count",
            result.mechanismChannels().size()
                + ReactionProductPredictionExecutor.surrogateProductChannelCount(
                    result.candidates()),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "unmaterialized_mechanism_channel_count", result.mechanismChannels().size(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "explicit_product_graph_coverage_ratio",
            ReactionProductPredictionExecutor.explicitProductGraphCoverageRatio(
                result.candidates()),
            "fraction"));
    values.add(
        ClassicalResultValue.text(
            "product_graph_coverage_status",
            ReactionProductPredictionExecutor.productGraphCoverageStatus(
                result.candidates(), result.mechanismChannels())));
    values.add(
        ClassicalResultValue.text(
            "product_graph_followup_recommended",
            Boolean.toString(
                ReactionProductPredictionExecutor.productGraphFollowupRecommended(
                    result.candidates(), result.mechanismChannels()))));
    values.add(
        ClassicalResultValue.text(
            "product_graph_followup_reason",
            ReactionProductPredictionExecutor.productGraphFollowupReason(
                result.candidates(), result.mechanismChannels())));
    values.add(
        ClassicalResultValue.scalar(
            "mechanism_scored_candidate_count",
            ReactionProductPredictionExecutor.mechanismScoredProductChannelCount(
                result.candidates()),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "merged_duplicate_product_channel_count",
            ReactionProductPredictionExecutor.mergedDuplicateProductChannelCount(
                result.candidates()),
            "count"));
    values.add(
        ClassicalResultValue.scalar("diagnostic_count", result.diagnostics().size(), "count"));
    values.add(
        ClassicalResultValue.scalar("probability_mass", result.probabilityMass(), "fraction"));
    values.add(
        ClassicalResultValue.scalar(
            "missing_probability_mass", result.missingProbabilityMass(), "fraction"));
    values.add(
        ClassicalResultValue.scalar(
            "product_distribution_entropy",
            ReactionProductPredictionExecutor.productDistributionEntropy(result.candidates()),
            "nat"));
    values.add(
        ClassicalResultValue.scalar(
            "effective_product_channel_count",
            ReactionProductPredictionExecutor.effectiveProductChannelCount(result.candidates()),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "dominant_to_second_probability_ratio",
            ReactionProductPredictionExecutor.dominantToSecondProbabilityRatio(result.candidates()),
            "ratio"));
    values.add(
        ClassicalResultValue.scalar(
            "prediction_uncertainty_score", uncertaintyReport.uncertaintyScore(), "fraction"));
    values.add(
        ClassicalResultValue.scalar(
            "best_probability_lower_bound",
            uncertaintyReport.topCandidateLowerBound(),
            "fraction"));
    values.add(
        ClassicalResultValue.scalar(
            "best_probability_upper_bound",
            uncertaintyReport.topCandidateUpperBound(),
            "fraction"));
    values.add(
        ClassicalResultValue.scalar(
            "uncertainty_risk_evidence_count", uncertaintyReport.riskEvidenceCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "uncertainty_warning_count", uncertaintyReport.warningCount(), "count"));
    values.add(
        ClassicalResultValue.scalar("followup_step_count", followUpPlan.stepCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "condition_scenario_plan_count", conditionScenarioPlan.scenarios().size(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "condition_scenario_evaluation_count",
            conditionScenarioEvaluationPlan.evaluations().size(),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "condition_scenario_improving_count",
            conditionScenarioEvaluationPlan.improvingEvaluations().size(),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "condition_scenario_worsening_count",
            conditionScenarioEvaluationPlan.worseningEvaluations().size(),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "condition_scenario_axis_count",
            ReactionProductPredictionExecutor.conditionScenarioAxisCount(conditionScenarioPlan),
            "count"));
    values.add(
        ClassicalResultValue.text(
            "condition_scenario_coverage_status",
            ReactionProductPredictionExecutor.conditionScenarioCoverageStatus(
                conditionScenarioPlan)));
    if (!conditionScenarioEvaluationPlan.empty()) {
      final ReactionConditionScenarioEvaluation bestConditionScenario =
          conditionScenarioEvaluationPlan.bestEvaluation();
      values.add(
          ClassicalResultValue.text(
              "best_condition_scenario_id", bestConditionScenario.scenario().id()));
      values.add(
          ClassicalResultValue.text(
              "best_condition_scenario_kind", bestConditionScenario.scenario().kind().name()));
      values.add(
          ClassicalResultValue.scalar(
              "best_condition_scenario_probability",
              bestConditionScenario.bestProbability(),
              "fraction"));
      values.add(
          ClassicalResultValue.scalar(
              "best_condition_scenario_expected_yield",
              bestConditionScenario.bestExpectedYieldPercent(),
              "percent"));
      values.add(
          ClassicalResultValue.scalar(
              "best_condition_scenario_probability_delta",
              bestConditionScenario.baselineProbabilityDelta(),
              "fraction"));
      values.add(
          ClassicalResultValue.scalar(
              "best_condition_scenario_yield_delta",
              bestConditionScenario.baselineExpectedYieldDelta(),
              "percent"));
    }
    values.add(
        ClassicalResultValue.text(
            "followup_has_required_steps", Boolean.toString(followUpPlan.hasRequiredSteps())));
    values.add(
        ClassicalResultValue.scalar(
            "candidate_with_score_breakdown_count",
            ReactionProductPredictionExecutor.candidateWithScoreBreakdownCount(result.candidates()),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "candidate_with_stoichiometry_breakdown_count",
            ReactionProductPredictionExecutor.candidateWithStoichiometryBreakdownCount(
                result.candidates()),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "candidate_with_mechanism_count",
            ReactionProductPredictionExecutor.candidateWithMechanismCount(result.candidates()),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "candidate_ready_for_electronic_rerank_count",
            ReactionProductPredictionExecutor.candidateReadyForElectronicRerankCount(
                result.candidates()),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "candidate_ready_for_quantum_workflow_count",
            ReactionProductPredictionExecutor.candidateReadyForQuantumWorkflowCount(
                result.candidates()),
            "count"));
    values.add(
        ClassicalResultValue.text(
            "candidate_quantum_workflow_preparation_enabled",
            Boolean.toString(
                ReactionProductPredictionExecutor.prepareQuantumWorkflows(calculationRequest))));
    values.add(
        ClassicalResultValue.text(
            "candidate_quantum_workflow_vqe_execution_enabled",
            Boolean.toString(
                ReactionProductPredictionExecutor.executeQuantumWorkflowVqe(calculationRequest))));
    values.add(
        ClassicalResultValue.scalar(
            "candidate_quantum_workflow_preparation_candidate_limit",
            ReactionProductPredictionExecutor.prepareQuantumWorkflowMaxCandidates(
                calculationRequest),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "candidate_quantum_workflow_preparation_participant_limit",
            ReactionProductPredictionExecutor.prepareQuantumWorkflowMaxParticipants(
                calculationRequest),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "candidate_with_production_detailed_mechanism_count",
            this.candidateWithMechanismQualityCount(
                result.candidates(), MechanismQuality.PRODUCTION_DETAILED),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "candidate_with_usable_mechanism_count",
            this.candidateWithMechanismQualityCount(
                result.candidates(), MechanismQuality.CHEMICALLY_USABLE),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "candidate_with_generic_mechanism_count",
            this.candidateWithMechanismQualityCount(
                result.candidates(), MechanismQuality.SYNTHETIC_GENERIC),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "candidate_with_risk_evidence_count",
            ReactionProductPredictionExecutor.candidateWithRiskEvidenceCount(result.candidates()),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "positive_candidate_evidence_count",
            ReactionProductPredictionExecutor.candidateEvidenceCount(result.candidates(), true),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "negative_candidate_evidence_count",
            ReactionProductPredictionExecutor.candidateEvidenceCount(result.candidates(), false),
            "count"));
    values.add(
        ClassicalResultValue.text(
            "candidate_evidence_kind_histogram",
            ReactionProductPredictionExecutor.evidenceKindHistogram(result.candidates())));
    values.add(
        ClassicalResultValue.text(
            "benchmark_audit_enabled", Boolean.toString(includeBenchmarkAudit)));
    values.add(
        ClassicalResultValue.text(
            "benchmark_audit_only",
            Boolean.toString(includeBenchmarkAudit && !applyCalibrationPriors)));
    values.add(
        ClassicalResultValue.scalar(
            "benchmark_audit_probability_effect",
            ReactionProductPredictionExecutor.calibrationPriorEvidenceWeight(result.candidates()),
            "fraction"));
    if (includeBenchmarkAudit) {
      values.add(
          ClassicalResultValue.scalar(
              "benchmark_exact_observation_count",
              benchmarkReport.exactObservationCount(),
              "count"));
      values.add(
          ClassicalResultValue.scalar(
              "benchmark_comparable_observation_count",
              benchmarkReport.comparableObservationCount(),
              "count"));
      values.add(
          ClassicalResultValue.scalar(
              "benchmark_unmatched_observation_count",
              benchmarkReport.unmatchedObservationCount(),
              "count"));
      values.add(
          ClassicalResultValue.scalar(
              "benchmark_comparable_coverage", benchmarkReport.comparableCoverage(), "fraction"));
      values.add(
          ClassicalResultValue.scalar(
              "benchmark_mae", benchmarkReport.meanAbsoluteError(), "fraction"));
      values.add(
          ClassicalResultValue.scalar(
              "benchmark_weighted_mae", benchmarkReport.weightedMeanAbsoluteError(), "fraction"));
    }
    values.add(
        ClassicalResultValue.text(
            "predicted_mechanism_family_histogram",
            ReactionProductPredictionExecutor.mechanismFamilyHistogram(
                result.candidates(), result.mechanismChannels())));
    values.add(
        ClassicalResultValue.text(
            "mechanism_taxonomy_complete",
            Boolean.toString(mechanismCoverageReport.taxonomyComplete())));
    values.add(
        ClassicalResultValue.text(
            "mechanism_production_detailed_complete",
            Boolean.toString(mechanismCoverageReport.productionDetailedComplete())));
    values.add(
        ClassicalResultValue.scalar(
            "mechanism_pattern_count", mechanismCoverageReport.patternCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "mechanism_synthetic_generic_count",
            mechanismCoverageReport.syntheticGenericPatternCount(),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "mechanism_chemically_usable_count",
            mechanismCoverageReport.chemicallyUsablePatternCount(),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "mechanism_production_detailed_count",
            mechanismCoverageReport.productionDetailedPatternCount(),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "mechanism_under_specified_count",
            mechanismCoverageReport.underSpecifiedPatternCount(),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "mechanism_usable_coverage_ratio",
            mechanismCoverageReport.chemicallyUsableCoverageRatio(),
            "fraction"));
    values.add(
        ClassicalResultValue.scalar(
            "mechanism_production_coverage_ratio",
            mechanismCoverageReport.productionCoverageRatio(),
            "fraction"));
    values.add(
        ClassicalResultValue.text(
            "mechanism_synthetic_generic_families",
            ReactionProductPredictionExecutor.textSummary(
                mechanismCoverageReport.syntheticGenericPatternIds())));
    values.add(
        ClassicalResultValue.text(
            "mechanism_under_specified_families",
            ReactionProductPredictionExecutor.textSummary(
                mechanismCoverageReport.underSpecifiedPatternIds())));
    values.add(
        ClassicalResultValue.text(
            "activated_reaction_domains",
            ReactionProductPredictionExecutor.domainSummary(result.request().domains())));
    values.add(
        ClassicalResultValue.scalar(
            "reactant_heterocycle_feature_count",
            this.reactantFeatureCount(
                result.request().reactants(), MolecularFeatureKind.HETEROCYCLE),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "reactant_radical_center_count",
            this.reactantFeatureCount(
                result.request().reactants(), MolecularFeatureKind.RADICAL_CENTER),
            "count"));
    values.add(
        ClassicalResultValue.text(
            "recommended_calculation_followup",
            this.recommendedCalculationFollowup(result.request().reactants())));
    values.add(
        ClassicalResultValue.text(
            "prediction_coverage_status",
            ReactionProductPredictionExecutor.coverageStatus(result)));
    values.add(ClassicalResultValue.text("confidence_level", explanation.confidenceLevel().name()));
    values.add(
        ClassicalResultValue.scalar("probability_gap", explanation.probabilityGap(), "fraction"));
    values.add(
        ClassicalResultValue.text(
            "condition_aware", Boolean.toString(explanation.conditionAware())));
    values.add(
        ClassicalResultValue.scalar(
            "condition_effect_count", conditionEffectProfile.effects().size(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "condition_effect_support_count", conditionEffectProfile.supportCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "condition_effect_risk_count", conditionEffectProfile.riskCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "condition_effect_score_adjustment",
            conditionEffectProfile.scoreAdjustment(),
            "score"));
    values.add(
        ClassicalResultValue.text(
            "prediction_readiness_level", applicabilityReport.readinessLevel().name()));
    values.add(
        ClassicalResultValue.scalar(
            "prediction_applicability_score",
            applicabilityReport.applicabilityScore(),
            "fraction"));
    values.add(
        ClassicalResultValue.text(
            "quantum_followup_recommended",
            Boolean.toString(applicabilityReport.quantumFollowupRecommended())));
    values.add(
        ClassicalResultValue.text(
            "electronic_rerank_recommended",
            Boolean.toString(applicabilityReport.electronicRerankRecommended())));
    values.add(
        ClassicalResultValue.scalar(
            "close_competitor_count", applicabilityReport.closeCompetitorCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "missing_critical_condition_count",
            applicabilityReport.missingCriticalConditionCount(),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "applicability_production_detailed_mechanism_count",
            applicabilityReport.productionDetailedMechanismCount(),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "applicability_chemically_usable_mechanism_count",
            applicabilityReport.chemicallyUsableMechanismCount(),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "applicability_generic_fallback_mechanism_count",
            applicabilityReport.genericFallbackMechanismCount(),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "applicability_unknown_mechanism_count",
            applicabilityReport.unknownMechanismCount(),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "condition_suitability_score", explanation.conditionSuitability().score(), "fraction"));
    values.add(
        ClassicalResultValue.text(
            "condition_strongly_incompatible",
            Boolean.toString(explanation.conditionSuitability().stronglyIncompatible())));
    values.add(
        ClassicalResultValue.text(
            "condition_supporting_evidence",
            this.evidenceSummary(explanation.conditionSuitability().supportingEvidence())));
    values.add(
        ClassicalResultValue.text(
            "condition_limiting_evidence",
            this.evidenceSummary(explanation.conditionSuitability().limitingEvidence())));
    values.add(
        ClassicalResultValue.text("mechanism_class", pathwayAssessment.mechanismClass().name()));
    values.add(
        ClassicalResultValue.text(
            "regioselectivity_class", pathwayAssessment.regioselectivityClass().name()));
    values.add(
        ClassicalResultValue.scalar(
            "pathway_step_count", pathwayAssessment.pathwaySteps().size(), "count"));
    values.add(
        ClassicalResultValue.text(
            "pathway_step_sequence", this.pathwayStepSummary(pathwayAssessment.pathwaySteps())));
    values.add(
        ClassicalResultValue.text(
            "condition_window",
            ReactionProductPredictionExecutor.conditionWindowSummary(
                pathwayAssessment.conditionWindow())));
    values.add(
        ClassicalResultValue.scalar(
            "positive_evidence_count", explanation.positiveEvidenceCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "negative_evidence_count", explanation.negativeEvidenceCount(), "count"));
    values.add(
        ClassicalResultValue.text(
            "dominant_evidence", this.evidenceSummary(explanation.dominantEvidence())));
    values.add(
        ClassicalResultValue.text(
            "risk_evidence", this.evidenceSummary(explanation.riskEvidence())));
    if (result.hasCandidates()) {
      final ReactionProductCandidate bestCandidate = result.bestCandidate();
      values.add(
          ClassicalResultValue.scalar("best_probability", bestCandidate.probability(), "fraction"));
      values.add(
          ClassicalResultValue.scalar(
              "best_expected_yield", bestCandidate.expectedYieldPercent(), "percent"));
      values.add(
          ClassicalResultValue.text(
              "best_products", this.formulaSummary(bestCandidate.products())));
      values.add(
          ClassicalResultValue.text("best_selectivity_note", this.selectivityNote(bestCandidate)));
      values.add(
          ClassicalResultValue.text(
              "best_evidence", this.evidenceSummary(bestCandidate.evidence())));
      values.add(
          ClassicalResultValue.text(
              "best_predicted_mechanisms",
              this.mechanismSummary(bestCandidate.predictedMechanisms())));
      values.add(
          ClassicalResultValue.text(
              "best_mechanism_steps",
              this.mechanismStepSummary(bestCandidate.predictedMechanisms())));
    }
    values.add(
        ClassicalResultValue.of(
            "candidates",
            ClassicalResultValueKind.TABLE,
            "",
            this.candidateRows(result.candidates())));
    values.add(
        ClassicalResultValue.of(
            "candidate_distribution_trace",
            ClassicalResultValueKind.TABLE,
            "",
            this.candidateDistributionTraceRows(result.candidates())));
    values.add(
        ClassicalResultValue.of(
            "candidate_evidence_trace",
            ClassicalResultValueKind.TABLE,
            "",
            this.candidateEvidenceTraceRows(result.candidates())));
    values.add(
        ClassicalResultValue.of(
            "candidate_factor_matrix",
            ClassicalResultValueKind.TABLE,
            "",
            this.candidateFactorMatrixRows(result.candidates())));
    values.add(
        ClassicalResultValue.of(
            "candidate_readiness_matrix",
            ClassicalResultValueKind.TABLE,
            "",
            this.candidateReadinessMatrixRows(result.candidates())));
    values.add(
        ClassicalResultValue.of(
            "unmaterialized_mechanism_channels",
            ClassicalResultValueKind.TABLE,
            "",
            ReactionProductPredictionExecutor.mechanismChannelRows(result.mechanismChannels())));
    values.add(
        ClassicalResultValue.of(
            "candidate_electronic_rerank_preflight",
            ClassicalResultValueKind.TABLE,
            "",
            this.candidateElectronicRerankPreflightRows(calculationRequest, result.candidates())));
    values.add(
        ClassicalResultValue.of(
            "candidate_quantum_workflow_preparation",
            ClassicalResultValueKind.TABLE,
            "",
            this.candidateQuantumWorkflowPreparationRows(
                calculationRequest, result.candidates(), diagnostics)));
    values.add(
        ClassicalResultValue.of(
            "condition_effect_trace",
            ClassicalResultValueKind.TABLE,
            "",
            ReactionProductPredictionExecutor.conditionEffectRows(conditionEffectProfile)));
    values.add(
        ClassicalResultValue.of(
            "reactant_stoichiometry_trace",
            ClassicalResultValueKind.TABLE,
            "",
            ReactionProductPredictionExecutor.stoichiometryRows(stoichiometryProfile)));
    values.add(
        ClassicalResultValue.of(
            "prediction_applicability_trace",
            ClassicalResultValueKind.TABLE,
            "",
            applicabilityReport.traceRows()));
    values.add(
        ClassicalResultValue.of(
            "prediction_followup_plan",
            ClassicalResultValueKind.TABLE,
            "",
            ReactionProductPredictionExecutor.followUpRows(followUpPlan)));
    values.add(
        ClassicalResultValue.of(
            "prediction_computational_roadmap",
            ClassicalResultValueKind.TABLE,
            "",
            this.computationalRoadmapRows(result, pathwayAssessment)));
    values.add(
        ClassicalResultValue.of(
            "candidate_pathway_barrier_preflight",
            ClassicalResultValueKind.TABLE,
            "",
            this.candidatePathwayBarrierPreflightRows(result)));
    values.add(
        ClassicalResultValue.of(
            "condition_scenario_plan",
            ClassicalResultValueKind.TABLE,
            "",
            ReactionProductPredictionExecutor.conditionScenarioRows(conditionScenarioPlan)));
    values.add(
        ClassicalResultValue.of(
            "condition_scenario_coverage",
            ClassicalResultValueKind.TABLE,
            "",
            ReactionProductPredictionExecutor.conditionScenarioCoverageRows(
                conditionScenarioPlan)));
    values.add(
        ClassicalResultValue.of(
            "condition_scenario_evaluation",
            ClassicalResultValueKind.TABLE,
            "",
            ReactionProductPredictionExecutor.conditionScenarioEvaluationRows(
                conditionScenarioEvaluationPlan)));
    values.add(
        ClassicalResultValue.of(
            "prediction_uncertainty_trace",
            ClassicalResultValueKind.TABLE,
            "",
            uncertaintyReport.rows()));
    if (includeBenchmarkAudit) {
      values.add(
          ClassicalResultValue.of(
              "benchmark_observation_trace",
              ClassicalResultValueKind.TABLE,
              "",
              ReactionProductPredictionExecutor.benchmarkRows(benchmarkReport)));
    }
    values.add(
        ClassicalResultValue.of(
            "reactant_feature_bridge",
            ClassicalResultValueKind.TABLE,
            "",
            this.reactantFeatureRows(result.request().reactants())));
    return ClassicalResultSection.of(
        ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION,
        "Reaction product prediction",
        values);
  }

  private static List<String> stoichiometryRows(final ReactionStoichiometryProfile profile) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add(
        "component\tmolecule\tamount_mol\tequivalent_ratio\tunmatched_count\ttotal_gram_scale");
    final List<ReactionStoichiometryComponent> components = profile.matchedComponents();
    if (components.isEmpty()) {
      rows.add(
          "none\tnone\t0.0\t0.0\t"
              + profile.unmatchedReactantCount()
              + "\t"
              + profile.totalGramScale());
      return List.copyOf(rows);
    }
    for (int i = 0; i < components.size(); ++i) {
      final ReactionStoichiometryComponent component = components.get(i);
      rows.add(
          component.component().name()
              + "\t"
              + component.participant().molecule().displayName()
              + "\t"
              + component.moleAmount()
              + "\t"
              + component.equivalentRatio()
              + "\t"
              + profile.unmatchedReactantCount()
              + "\t"
              + profile.totalGramScale());
    }
    return List.copyOf(rows);
  }

  private static List<String> followUpRows(final ReactionPredictionFollowUpPlan plan) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add("order\tcalculation_kind\turgency\treason\texpected_decision");
    final List<ReactionPredictionFollowUpStep> steps = plan.steps();
    if (steps.isEmpty()) {
      rows.add(
          "0\tnone\tOPTIONAL\tNo follow-up calculation required by current decision rules.\tPrediction can be stored as-is.");
      return List.copyOf(rows);
    }
    for (int i = 0; i < steps.size(); ++i) {
      final ReactionPredictionFollowUpStep step = steps.get(i);
      rows.add(
          step.order()
              + "\t"
              + step.calculationKind().name()
              + "\t"
              + step.urgency().name()
              + "\t"
              + step.reason()
              + "\t"
              + step.expectedDecision());
    }
    return List.copyOf(rows);
  }

  private static List<String> conditionScenarioRows(final ReactionConditionScenarioPlan plan) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add("order\tscenario_id\tkind\trationale");
    final List<ReactionConditionScenario> scenarios = plan.scenarios();
    if (scenarios.isEmpty()) {
      rows.add("0\tnone\tBASELINE\tNo condition scenario was generated.");
      return List.copyOf(rows);
    }
    for (int i = 0; i < scenarios.size(); ++i) {
      final ReactionConditionScenario scenario = scenarios.get(i);
      rows.add(
          (i + 1)
              + "\t"
              + scenario.id()
              + "\t"
              + scenario.kind().name()
              + "\t"
              + scenario.rationale());
    }
    return List.copyOf(rows);
  }

  private static List<String> conditionScenarioCoverageRows(
      final ReactionConditionScenarioPlan plan) {
    final String[] axes =
        new String[] {
          "thermal",
          "atmosphere",
          "acid_base",
          "redox_activation",
          "solvent",
          "operational",
          "concentration",
          "stoichiometry",
          "duration"
        };
    final int[] counts = new int[axes.length];
    final List<ReactionConditionScenario> scenarios = plan.scenarios();
    for (int i = 0; i < scenarios.size(); ++i) {
      final int axisIndex =
          ReactionProductPredictionExecutor.conditionScenarioAxisIndex(scenarios.get(i).kind());
      if (axisIndex >= 0) {
        ++counts[axisIndex];
      }
    }
    final ArrayList<String> rows = new ArrayList<String>(axes.length + 1);
    rows.add("axis\tcovered\tscenario_count");
    for (int i = 0; i < axes.length; ++i) {
      rows.add(axes[i] + "\t" + Boolean.toString(counts[i] > 0) + "\t" + counts[i]);
    }
    return List.copyOf(rows);
  }

  private static List<String> conditionScenarioEvaluationRows(
      final ReactionConditionScenarioEvaluationPlan plan) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add(
        "rank\tscenario_id\tkind\tbest_probability\tbest_yield_percent\tprobability_delta\tyield_delta\tprobability_mass\tdiagnostics\tranking_score\tbaseline_direction");
    final List<ReactionConditionScenarioEvaluation> evaluations = plan.rankedEvaluations();
    if (evaluations.isEmpty()) {
      rows.add("0\tnone\tBASELINE\t0.0\t0.0\t0.0\t0.0\t0.0\t0\t0.0\tflat");
      return List.copyOf(rows);
    }
    for (int i = 0; i < evaluations.size(); ++i) {
      final ReactionConditionScenarioEvaluation evaluation = evaluations.get(i);
      rows.add(
          i
              + "\t"
              + evaluation.scenario().id()
              + "\t"
              + evaluation.scenario().kind().name()
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
              + evaluation.rankingScore()
              + "\t"
              + ReactionProductPredictionExecutor.conditionScenarioDirection(evaluation));
    }
    return List.copyOf(rows);
  }

  private static String conditionScenarioDirection(
      final ReactionConditionScenarioEvaluation evaluation) {
    if (evaluation.improvesBaseline()) {
      return "improves";
    }
    if (evaluation.worsensBaseline()) {
      return "worsens";
    }
    return "flat";
  }

  private static int conditionScenarioAxisCount(final ReactionConditionScenarioPlan plan) {
    final boolean[] covered = new boolean[9];
    final List<ReactionConditionScenario> scenarios = plan.scenarios();
    for (int i = 0; i < scenarios.size(); ++i) {
      final int axisIndex =
          ReactionProductPredictionExecutor.conditionScenarioAxisIndex(scenarios.get(i).kind());
      if (axisIndex >= 0) {
        covered[axisIndex] = true;
      }
    }
    int count = 0;
    for (int i = 0; i < covered.length; ++i) {
      if (covered[i]) {
        ++count;
      }
    }
    return count;
  }

  private static String conditionScenarioCoverageStatus(final ReactionConditionScenarioPlan plan) {
    final int axisCount = ReactionProductPredictionExecutor.conditionScenarioAxisCount(plan);
    if (axisCount >= 8) {
      return "BROAD";
    }
    if (axisCount >= 5) {
      return "PARTIAL";
    }
    if (axisCount > 0) {
      return "NARROW";
    }
    return "EMPTY";
  }

  private static int conditionScenarioAxisIndex(final ReactionConditionScenarioKind kind) {
    if (kind == ReactionConditionScenarioKind.ROOM_TEMPERATURE
        || kind == ReactionConditionScenarioKind.HEATED
        || kind == ReactionConditionScenarioKind.COOLED) {
      return 0;
    }
    if (kind == ReactionConditionScenarioKind.INERT_ANHYDROUS
        || kind == ReactionConditionScenarioKind.MOISTURE_CONTROL
        || kind == ReactionConditionScenarioKind.DEHYDRATION_CONTROL) {
      return 1;
    }
    if (kind == ReactionConditionScenarioKind.ACID_ONLY
        || kind == ReactionConditionScenarioKind.BASE_ONLY
        || kind == ReactionConditionScenarioKind.MILD_BASE_CONTROL
        || kind == ReactionConditionScenarioKind.STRONG_BASE_CONTROL
        || kind == ReactionConditionScenarioKind.ACID_SCAVENGER_CONTROL) {
      return 2;
    }
    if (kind == ReactionConditionScenarioKind.OXIDANT_CONTROL
        || kind == ReactionConditionScenarioKind.REDUCTANT_CONTROL
        || kind == ReactionConditionScenarioKind.HYDROGENATION_CONTROL
        || kind == ReactionConditionScenarioKind.PHOTOCHEMICAL_CONTROL
        || kind == ReactionConditionScenarioKind.ELECTROCHEMICAL_CONTROL
        || kind == ReactionConditionScenarioKind.CATALYST_SIGNAL) {
      return 3;
    }
    if (kind == ReactionConditionScenarioKind.POLAR_APROTIC_SOLVENT
        || kind == ReactionConditionScenarioKind.POLAR_PROTIC_SOLVENT
        || kind == ReactionConditionScenarioKind.NONPOLAR_SOLVENT
        || kind == ReactionConditionScenarioKind.POLYETHER_HIGH_BOILING_SOLVENT) {
      return 4;
    }
    if (kind == ReactionConditionScenarioKind.REFLUX_CONDITION
        || kind == ReactionConditionScenarioKind.SEALED_HEATED_CONDITION
        || kind == ReactionConditionScenarioKind.MICROWAVE_HEATED_CONDITION
        || kind == ReactionConditionScenarioKind.FLOW_CONDITION
        || kind == ReactionConditionScenarioKind.LARGE_SCALE_MASS_TRANSFER) {
      return 5;
    }
    if (kind == ReactionConditionScenarioKind.DILUTE_REGIME
        || kind == ReactionConditionScenarioKind.CONCENTRATED_REGIME) {
      return 6;
    }
    if (kind == ReactionConditionScenarioKind.NUCLEOPHILE_EXCESS
        || kind == ReactionConditionScenarioKind.ELECTROPHILE_EXCESS
        || kind == ReactionConditionScenarioKind.BASE_EXCESS) {
      return 7;
    }
    if (kind == ReactionConditionScenarioKind.SHORT_DURATION
        || kind == ReactionConditionScenarioKind.LONG_DURATION) {
      return 8;
    }
    return -1;
  }

  private static double productDistributionEntropy(
      final List<ReactionProductCandidate> candidates) {
    double entropy = 0.0;
    for (int i = 0; i < candidates.size(); ++i) {
      final double probability = candidates.get(i).probability();
      if (probability <= 0.0 || !Double.isFinite(probability)) {
        continue;
      }
      entropy -= probability * Math.log(probability);
    }
    return entropy;
  }

  private static double effectiveProductChannelCount(
      final List<ReactionProductCandidate> candidates) {
    final double entropy = ReactionProductPredictionExecutor.productDistributionEntropy(candidates);
    if (!Double.isFinite(entropy) || entropy <= 0.0) {
      return candidates.isEmpty() ? 0.0 : 1.0;
    }
    return Math.exp(entropy);
  }

  private static double dominantToSecondProbabilityRatio(
      final List<ReactionProductCandidate> candidates) {
    double best = 0.0;
    double second = 0.0;
    for (int i = 0; i < candidates.size(); ++i) {
      final double probability = candidates.get(i).probability();
      if (probability > best) {
        second = best;
        best = probability;
      } else if (probability > second) {
        second = probability;
      }
    }
    if (best <= 0.0) {
      return 0.0;
    }
    if (second <= 0.0) {
      return best;
    }
    return best / second;
  }

  private static List<String> conditionEffectRows(final ReactionConditionEffectProfile profile) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add("kind\tevidence_kind\tscore_adjustment\trisk\tdescription");
    final List<ReactionConditionEffect> effects = profile.effects();
    for (int i = 0; i < effects.size(); ++i) {
      final ReactionConditionEffect effect = effects.get(i);
      rows.add(
          effect.kind().name()
              + "\t"
              + effect.evidenceKind().name()
              + "\t"
              + effect.scoreAdjustment()
              + "\t"
              + effect.risk()
              + "\t"
              + effect.description());
    }
    return List.copyOf(rows);
  }

  private long reactantFeatureCount(final ReactionSide reactants, final MolecularFeatureKind kind) {
    long count = 0L;
    final List<ReactionParticipant> participants = reactants.participants();
    for (int i = 0; i < participants.size(); ++i) {
      count += this.featureAnalyzer.analyze(participants.get(i).molecule()).countOf(kind);
    }
    return count;
  }

  private String recommendedCalculationFollowup(final ReactionSide reactants) {
    final long radicalCenterCount =
        this.reactantFeatureCount(reactants, MolecularFeatureKind.RADICAL_CENTER);
    final long heterocycleCount =
        this.reactantFeatureCount(reactants, MolecularFeatureKind.HETEROCYCLE);
    if (radicalCenterCount > 0L && heterocycleCount > 0L) {
      return "OPEN_SHELL_DFT_OR_UHF_REACTION_ENERGETICS_THEN_HAMILTONIAN_AND_QUANTUM_PROGRAM_PREPARATION";
    }
    if (radicalCenterCount > 0L) {
      return "OPEN_SHELL_DFT_OR_UHF_REACTION_ENERGETICS";
    }
    if (heterocycleCount > 0L) {
      return "DFT_REACTION_ENERGETICS_AND_OPTIONAL_HAMILTONIAN_PREPARATION";
    }
    return "REACTION_ENERGETICS_SCREENING";
  }

  private List<String> reactantFeatureRows(final ReactionSide reactants) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add("participant_index\tmolecule_id\tfeature\tcount\tconfidence\tcalculation_followup");
    final List<ReactionParticipant> participants = reactants.participants();
    for (int participantIndex = 0; participantIndex < participants.size(); ++participantIndex) {
      final ReactionParticipant participant = participants.get(participantIndex);
      final MolecularFeatureSet featureSet = this.featureAnalyzer.analyze(participant.molecule());
      final List<MolecularFeatureKind> trackedKinds =
          List.of(
              MolecularFeatureKind.HETEROCYCLE,
              MolecularFeatureKind.NITROGEN_HETEROCYCLE,
              MolecularFeatureKind.OXYGEN_HETEROCYCLE,
              MolecularFeatureKind.SULFUR_HETEROCYCLE,
              MolecularFeatureKind.RADICAL_CENTER,
              MolecularFeatureKind.HALOGENATED,
              MolecularFeatureKind.AROMATIC_RING);
      for (int kindIndex = 0; kindIndex < trackedKinds.size(); ++kindIndex) {
        final MolecularFeatureKind kind = trackedKinds.get(kindIndex);
        final long count = featureSet.countOf(kind);
        if (count <= 0L) {
          continue;
        }
        rows.add(
            participantIndex
                + "\t"
                + participant.molecule().id().value()
                + "\t"
                + kind.name()
                + "\t"
                + count
                + "\t"
                + ReactionProductPredictionExecutor.featureConfidence(featureSet, kind)
                + "\t"
                + this.recommendedCalculationFollowup(ReactionSide.of(List.of(participant))));
      }
    }
    return rows;
  }

  private static double featureConfidence(
      final MolecularFeatureSet featureSet, final MolecularFeatureKind kind) {
    double confidence = 0.0;
    final List<ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeature>
        features = featureSet.features();
    for (int i = 0; i < features.size(); ++i) {
      if (features.get(i).kind() == kind) {
        confidence = Math.max(confidence, features.get(i).confidence());
      }
    }
    return confidence;
  }

  private static String domainSummary(final List<OrganicReactionDomain> domains) {
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < domains.size(); ++i) {
      if (i > 0) {
        builder.append(',');
      }
      builder.append(domains.get(i).name());
    }
    return builder.toString();
  }

  private static int candidateWithScoreBreakdownCount(
      final List<ReactionProductCandidate> candidates) {
    int count = 0;
    for (int i = 0; i < candidates.size(); ++i) {
      if (candidates.get(i).hasScoreBreakdown()) {
        ++count;
      }
    }
    return count;
  }

  private static int candidateWithStoichiometryBreakdownCount(
      final List<ReactionProductCandidate> candidates) {
    int count = 0;
    for (int i = 0; i < candidates.size(); ++i) {
      if (candidates.get(i).hasScoreBreakdown()
          && candidates
              .get(i)
              .scoreBreakdown()
              .hasComponent(ReactionPredictionScoreComponentKind.STOICHIOMETRY)) {
        ++count;
      }
    }
    return count;
  }

  private static int candidateWithMechanismCount(final List<ReactionProductCandidate> candidates) {
    int count = 0;
    for (int i = 0; i < candidates.size(); ++i) {
      if (candidates.get(i).hasPredictedMechanisms()) {
        ++count;
      }
    }
    return count;
  }

  private static int candidateReadyForElectronicRerankCount(
      final List<ReactionProductCandidate> candidates) {
    int count = 0;
    for (int i = 0; i < candidates.size(); ++i) {
      if (ReactionProductPredictionExecutor.readyForElectronicRerank(candidates.get(i))) {
        ++count;
      }
    }
    return count;
  }

  private static int candidateReadyForQuantumWorkflowCount(
      final List<ReactionProductCandidate> candidates) {
    int count = 0;
    for (int i = 0; i < candidates.size(); ++i) {
      if (ReactionProductPredictionExecutor.readyForQuantumWorkflow(candidates.get(i))) {
        ++count;
      }
    }
    return count;
  }

  private static boolean readyForElectronicRerank(final ReactionProductCandidate candidate) {
    return !candidate.hasEvidence(ReactionPredictionEvidenceKind.SURROGATE_PRODUCT)
        && candidate.products().participantCount() > 0;
  }

  private static boolean readyForQuantumWorkflow(final ReactionProductCandidate candidate) {
    return ReactionProductPredictionExecutor.readyForElectronicRerank(candidate)
        && candidate.hasPredictedMechanisms()
        && candidate.hasScoreBreakdown();
  }

  private int candidateWithMechanismQualityCount(
      final List<ReactionProductCandidate> candidates, final MechanismQuality quality) {
    int count = 0;
    for (int i = 0; i < candidates.size(); ++i) {
      if (this.hasMechanismQuality(candidates.get(i), quality)) {
        ++count;
      }
    }
    return count;
  }

  private boolean hasMechanismQuality(
      final ReactionProductCandidate candidate, final MechanismQuality quality) {
    final List<PredictedReactionMechanism> mechanisms = candidate.predictedMechanisms();
    for (int i = 0; i < mechanisms.size(); ++i) {
      final ReactionMechanismPattern pattern =
          this.mechanismCatalog.find(mechanisms.get(i).family());
      if (pattern == null) {
        continue;
      }
      if (quality == MechanismQuality.PRODUCTION_DETAILED && pattern.productionDetailedPattern()) {
        return true;
      }
      if (quality == MechanismQuality.CHEMICALLY_USABLE && pattern.chemicallyUsablePattern()) {
        return true;
      }
      if (quality == MechanismQuality.SYNTHETIC_GENERIC && pattern.syntheticGenericPattern()) {
        return true;
      }
    }
    return false;
  }

  private static int candidateWithRiskEvidenceCount(
      final List<ReactionProductCandidate> candidates) {
    int count = 0;
    for (int i = 0; i < candidates.size(); ++i) {
      final List<ReactionPredictionEvidence> evidence = candidates.get(i).evidence();
      for (int evidenceIndex = 0; evidenceIndex < evidence.size(); ++evidenceIndex) {
        if (evidence.get(evidenceIndex).weight() < 0.0) {
          ++count;
          break;
        }
      }
    }
    return count;
  }

  private static int candidateWithEvidenceKindCount(
      final List<ReactionProductCandidate> candidates, final ReactionPredictionEvidenceKind kind) {
    int count = 0;
    for (int i = 0; i < candidates.size(); ++i) {
      if (candidates.get(i).hasEvidence(kind)) {
        ++count;
      }
    }
    return count;
  }

  private static int electronicRerankWarningCandidateCount(
      final List<ReactionProductCandidate> candidates) {
    int count = 0;
    for (int i = 0; i < candidates.size(); ++i) {
      final List<ReactionPredictionEvidence> evidence = candidates.get(i).evidence();
      for (int evidenceIndex = 0; evidenceIndex < evidence.size(); ++evidenceIndex) {
        final ReactionPredictionEvidence item = evidence.get(evidenceIndex);
        if (item.kind() == ReactionPredictionEvidenceKind.WARNING
            && item.description().contains("electronic rerank")) {
          ++count;
          break;
        }
      }
    }
    return count;
  }

  private static int electronicRerankSkippedByLimitCount(
      final ClassicalCalculationRequest request, final List<ReactionProductCandidate> candidates) {
    if (!ReactionProductPredictionExecutor.electronicRerankEnabled(request)) {
      return candidates.size();
    }
    final int maxCandidates =
        ReactionProductPredictionExecutor.electronicRerankMaxCandidates(request);
    int skipped = 0;
    for (int i = 0; i < candidates.size(); ++i) {
      if (i >= maxCandidates
          || !candidates.get(i).hasEvidence(ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR)) {
        ++skipped;
      }
    }
    return skipped;
  }

  private static int candidateEvidenceCount(
      final List<ReactionProductCandidate> candidates, final boolean positive) {
    int count = 0;
    for (int i = 0; i < candidates.size(); ++i) {
      final List<ReactionPredictionEvidence> evidence = candidates.get(i).evidence();
      for (int evidenceIndex = 0; evidenceIndex < evidence.size(); ++evidenceIndex) {
        final double weight = evidence.get(evidenceIndex).weight();
        if (positive && weight > 0.0) {
          ++count;
        }
        if (!positive && weight < 0.0) {
          ++count;
        }
      }
    }
    return count;
  }

  private static int calibrationPriorEvidenceCount(
      final List<ReactionProductCandidate> candidates) {
    int count = 0;
    for (int i = 0; i < candidates.size(); ++i) {
      final List<ReactionPredictionEvidence> evidence = candidates.get(i).evidence();
      for (int evidenceIndex = 0; evidenceIndex < evidence.size(); ++evidenceIndex) {
        if (evidence.get(evidenceIndex).kind()
            == ReactionPredictionEvidenceKind.CALIBRATION_PRIOR) {
          ++count;
        }
      }
    }
    return count;
  }

  private static double calibrationPriorEvidenceWeight(
      final List<ReactionProductCandidate> candidates) {
    double weight = 0.0;
    for (int i = 0; i < candidates.size(); ++i) {
      final List<ReactionPredictionEvidence> evidence = candidates.get(i).evidence();
      for (int evidenceIndex = 0; evidenceIndex < evidence.size(); ++evidenceIndex) {
        final ReactionPredictionEvidence item = evidence.get(evidenceIndex);
        if (item.kind() == ReactionPredictionEvidenceKind.CALIBRATION_PRIOR) {
          weight += item.weight();
        }
      }
    }
    return weight;
  }

  private static String benchmarkRole(
      final boolean includeBenchmarkAudit, final boolean applyCalibrationPriors) {
    if (applyCalibrationPriors && includeBenchmarkAudit) {
      return "soft_prior_before_ranking_and_audit_after_prediction";
    }
    if (applyCalibrationPriors) {
      return "soft_prior_before_ranking";
    }
    if (includeBenchmarkAudit) {
      return "audit_only_after_prediction";
    }
    return "disabled";
  }

  private static String benchmarkDataFlow(
      final boolean includeBenchmarkAudit, final boolean applyCalibrationPriors) {
    if (applyCalibrationPriors && includeBenchmarkAudit) {
      return "mechanistic_prediction_electronic_rerank_soft_calibration_reweight_then_audit";
    }
    if (applyCalibrationPriors) {
      return "mechanistic_prediction_electronic_rerank_then_soft_calibration_reweight";
    }
    if (includeBenchmarkAudit) {
      return "evaluate_finished_prediction_without_reweighting";
    }
    return "not_read";
  }

  private static String evidenceKindHistogram(final List<ReactionProductCandidate> candidates) {
    final ReactionPredictionEvidenceKind[] kinds = ReactionPredictionEvidenceKind.values();
    final int[] counts = new int[kinds.length];
    for (int i = 0; i < candidates.size(); ++i) {
      final List<ReactionPredictionEvidence> evidence = candidates.get(i).evidence();
      for (int evidenceIndex = 0; evidenceIndex < evidence.size(); ++evidenceIndex) {
        ++counts[evidence.get(evidenceIndex).kind().ordinal()];
      }
    }
    return ReactionProductPredictionExecutor.histogram(kinds, counts);
  }

  private static int mergedDuplicateProductChannelCount(
      final List<ReactionProductCandidate> candidates) {
    int count = 0;
    for (int i = 0; i < candidates.size(); ++i) {
      final List<ReactionPredictionEvidence> evidence = candidates.get(i).evidence();
      for (int evidenceIndex = 0; evidenceIndex < evidence.size(); ++evidenceIndex) {
        if (evidence
            .get(evidenceIndex)
            .description()
            .contains("Merged duplicate product channel")) {
          ++count;
          break;
        }
      }
    }
    return count;
  }

  private static int ruleGeneratedProductChannelCount(
      final List<ReactionProductCandidate> candidates) {
    int count = 0;
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

  private static int userSeedProductChannelCount(final List<ReactionProductCandidate> candidates) {
    int count = 0;
    for (int i = 0; i < candidates.size(); ++i) {
      if (candidates.get(i).hasEvidence(ReactionPredictionEvidenceKind.USER_SEED)) {
        ++count;
      }
    }
    return count;
  }

  private static int surrogateProductChannelCount(final List<ReactionProductCandidate> candidates) {
    int count = 0;
    for (int i = 0; i < candidates.size(); ++i) {
      if (candidates.get(i).hasEvidence(ReactionPredictionEvidenceKind.SURROGATE_PRODUCT)) {
        ++count;
      }
    }
    return count;
  }

  private static int explicitProductGraphCandidateCount(
      final List<ReactionProductCandidate> candidates) {
    int count = 0;
    for (int i = 0; i < candidates.size(); ++i) {
      if (ReactionProductPredictionExecutor.readyForElectronicRerank(candidates.get(i))) {
        ++count;
      }
    }
    return count;
  }

  private static double explicitProductGraphCoverageRatio(
      final List<ReactionProductCandidate> candidates) {
    if (candidates.isEmpty()) {
      return 0.0;
    }
    return (double) ReactionProductPredictionExecutor.explicitProductGraphCandidateCount(candidates)
        / (double) candidates.size();
  }

  private static String predictionExecutionMode(final ClassicalCalculationRequest request) {
    if (ReactionProductPredictionExecutor.electronicRerankEnabled(request)) {
      return "ELECTRONIC_RERANK";
    }
    return "FAST_MECHANISTIC";
  }

  private static String productGraphCoverageStatus(
      final List<ReactionProductCandidate> candidates,
      final List<ReactionMechanismChannelCandidate> mechanismChannels) {
    if (candidates.isEmpty() && mechanismChannels.isEmpty()) {
      return "EMPTY";
    }
    if (candidates.isEmpty()) {
      return "MECHANISM_CHANNELS_WITHOUT_PRODUCT_GRAPHS";
    }
    final int explicitCount =
        ReactionProductPredictionExecutor.explicitProductGraphCandidateCount(candidates);
    if (explicitCount == 0) {
      return "MECHANISM_ONLY";
    }
    if (explicitCount == candidates.size()) {
      return "COMPLETE";
    }
    return "PARTIAL";
  }

  private static boolean productGraphFollowupRecommended(
      final List<ReactionProductCandidate> candidates,
      final List<ReactionMechanismChannelCandidate> mechanismChannels) {
    return !mechanismChannels.isEmpty()
        || (!candidates.isEmpty()
            && ReactionProductPredictionExecutor.explicitProductGraphCandidateCount(candidates)
                < candidates.size());
  }

  private static String productGraphFollowupReason(
      final List<ReactionProductCandidate> candidates,
      final List<ReactionMechanismChannelCandidate> mechanismChannels) {
    if (candidates.isEmpty() && mechanismChannels.isEmpty()) {
      return "No product candidates were generated.";
    }
    if (candidates.isEmpty()) {
      return "Recognized mechanism channels do not yet have explicit product graphs; provide graph transforms, seed products, or drawing-captured products before strict electronic rerank.";
    }
    if (!ReactionProductPredictionExecutor.productGraphFollowupRecommended(
        candidates, mechanismChannels)) {
      return "All product channels have explicit product graphs.";
    }
    if (!mechanismChannels.isEmpty()) {
      return "Some recognized mechanism channels still need explicit product graphs, and candidate channels should be checked before strict electronic rerank.";
    }
    return "Some product channels are mechanism-only; provide graph transforms, seed products, or drawing-captured products before strict electronic rerank.";
  }

  private static int mechanismScoredProductChannelCount(
      final List<ReactionProductCandidate> candidates) {
    int count = 0;
    for (int i = 0; i < candidates.size(); ++i) {
      if (candidates.get(i).hasPredictedMechanisms()) {
        ++count;
      }
    }
    return count;
  }

  private static String mechanismFamilyHistogram(
      final List<ReactionProductCandidate> candidates,
      final List<ReactionMechanismChannelCandidate> mechanismChannels) {
    final ReactionMechanismFamily[] families = ReactionMechanismFamily.values();
    final int[] counts = new int[families.length];
    for (int i = 0; i < candidates.size(); ++i) {
      final List<PredictedReactionMechanism> mechanisms = candidates.get(i).predictedMechanisms();
      for (int mechanismIndex = 0; mechanismIndex < mechanisms.size(); ++mechanismIndex) {
        ++counts[mechanisms.get(mechanismIndex).family().ordinal()];
      }
    }
    for (int i = 0; i < mechanismChannels.size(); ++i) {
      ++counts[mechanismChannels.get(i).family().ordinal()];
    }
    return ReactionProductPredictionExecutor.histogram(families, counts);
  }

  private static String textSummary(final List<String> values) {
    if (values.isEmpty()) {
      return "none";
    }
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < values.size(); ++i) {
      if (i > 0) {
        builder.append(", ");
      }
      builder.append(values.get(i));
    }
    return builder.toString();
  }

  private static String coverageStatus(final ReactionPredictionResult result) {
    if (!result.hasCandidates()) {
      if (result.hasMechanismChannels()) {
        return "MECHANISM_CHANNELS_WITHOUT_PRODUCT_GRAPHS";
      }
      return "NO_CANDIDATES";
    }
    if (!result.probabilityMassComplete(1.0E-9)) {
      return "INCOMPLETE_PROBABILITY_MASS";
    }
    if (ReactionProductPredictionExecutor.candidateWithMechanismCount(result.candidates()) == 0) {
      return "NO_MECHANISM_ASSIGNMENT";
    }
    if (ReactionProductPredictionExecutor.candidateWithScoreBreakdownCount(result.candidates())
        < result.candidates().size()) {
      return "PARTIAL_SCORE_BREAKDOWN";
    }
    return "FULLY_EXPLAINED";
  }

  private static String histogram(final Enum<?>[] values, final int[] counts) {
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < values.length; ++i) {
      if (counts[i] == 0) {
        continue;
      }
      if (builder.length() > 0) {
        builder.append("; ");
      }
      builder.append(values[i].name());
      builder.append("=");
      builder.append(counts[i]);
    }
    if (builder.length() == 0) {
      return "none";
    }
    return builder.toString();
  }

  private static List<String> benchmarkRows(final ReactionOutcomeBenchmarkReport report) {
    final List<ReactionOutcomeBenchmarkMatch> matches = report.matches();
    final ArrayList<String> rows = new ArrayList<String>(matches.size() + 1);
    rows.add(
        "observation\tchannel\tmatched\tobserved_probability\tpredicted_probability\tabsolute_error\treason");
    for (int i = 0; i < matches.size(); ++i) {
      final ReactionOutcomeBenchmarkMatch match = matches.get(i);
      rows.add(
          match.observationId()
              + "\t"
              + match.channelLabel()
              + "\t"
              + match.matched()
              + "\t"
              + match.observedProbability()
              + "\t"
              + match.predictedProbability()
              + "\t"
              + match.absoluteError()
              + "\t"
              + match.reason());
    }
    return List.copyOf(rows);
  }

  private List<String> candidateRows(final List<ReactionProductCandidate> candidates) {
    if (candidates.isEmpty()) {
      return List.of(
          "rank\tprobability\tyield_percent\tproducts\tsource\tprimary_mechanism\tmechanisms\tevidence");
    }
    final ArrayList<String> rows = new ArrayList<String>(candidates.size() + 1);
    rows.add(
        "rank\tprobability\tyield_percent\tproducts\tsource\tprimary_mechanism\tmechanisms\tevidence");
    for (int i = 0; i < candidates.size(); ++i) {
      final ReactionProductCandidate candidate = candidates.get(i);
      rows.add(
          Integer.toString(i + 1)
              + "\t"
              + candidate.probability()
              + "\t"
              + candidate.expectedYieldPercent()
              + "\t"
              + this.formulaSummary(candidate.products())
              + "\t"
              + ReactionProductPredictionExecutor.candidateSource(candidate)
              + "\t"
              + ReactionProductPredictionExecutor.primaryMechanism(candidate)
              + "\t"
              + this.mechanismSummary(candidate.predictedMechanisms())
              + "\t"
              + this.evidenceSummary(candidate.evidence()));
    }
    return List.copyOf(rows);
  }

  private List<String> candidateDistributionTraceRows(
      final List<ReactionProductCandidate> candidates) {
    final ArrayList<String> rows = new ArrayList<String>(candidates.size() + 1);
    rows.add(
        "rank\tprobability\tyield_percent\tproducts\tsource\tprimary_mechanism"
            + "\tmechanism_count\trisk_count\tcalibration_weight\telectronic_weight"
            + "\tmerged_duplicate\tsurrogate_removed_signal"
            + "\tpositive_evidence_weight\tnegative_evidence_weight\tscore_breakdown");
    for (int i = 0; i < candidates.size(); ++i) {
      final ReactionProductCandidate candidate = candidates.get(i);
      rows.add(
          Integer.toString(i + 1)
              + "\t"
              + candidate.probability()
              + "\t"
              + candidate.expectedYieldPercent()
              + "\t"
              + this.formulaSummary(candidate.products())
              + "\t"
              + ReactionProductPredictionExecutor.candidateSource(candidate)
              + "\t"
              + ReactionProductPredictionExecutor.primaryMechanism(candidate)
              + "\t"
              + candidate.predictedMechanismCount()
              + "\t"
              + ReactionProductPredictionExecutor.riskEvidenceCount(candidate)
              + "\t"
              + candidate.evidenceWeight(ReactionPredictionEvidenceKind.CALIBRATION_PRIOR)
              + "\t"
              + candidate.evidenceWeight(ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR)
              + "\t"
              + Boolean.toString(
                  ReactionProductPredictionExecutor.hasEvidenceDescription(
                      candidate, "Merged duplicate product channel"))
              + "\t"
              + Boolean.toString(
                  candidate.hasEvidence(ReactionPredictionEvidenceKind.SURROGATE_PRODUCT))
              + "\t"
              + ReactionProductPredictionExecutor.evidenceWeight(candidate, true)
              + "\t"
              + ReactionProductPredictionExecutor.evidenceWeight(candidate, false)
              + "\t"
              + ReactionProductPredictionExecutor.scoreBreakdownSummary(candidate));
    }
    return List.copyOf(rows);
  }

  private List<String> candidateEvidenceTraceRows(final List<ReactionProductCandidate> candidates) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add("rank\tproducts\tevidence_index\tevidence_kind\tweight\tdirection\tdescription");
    if (candidates.isEmpty()) {
      rows.add("0\tnone\t0\tNONE\t0.0\tneutral\tNo product candidates were generated.");
      return List.copyOf(rows);
    }
    for (int candidateIndex = 0; candidateIndex < candidates.size(); ++candidateIndex) {
      final ReactionProductCandidate candidate = candidates.get(candidateIndex);
      final List<ReactionPredictionEvidence> evidence = candidate.evidence();
      if (evidence.isEmpty()) {
        rows.add(
            (candidateIndex + 1)
                + "\t"
                + this.formulaSummary(candidate.products())
                + "\t0\tNONE\t0.0\tneutral\tCandidate has no traceable evidence.");
        continue;
      }
      for (int evidenceIndex = 0; evidenceIndex < evidence.size(); ++evidenceIndex) {
        final ReactionPredictionEvidence item = evidence.get(evidenceIndex);
        rows.add(
            (candidateIndex + 1)
                + "\t"
                + this.formulaSummary(candidate.products())
                + "\t"
                + (evidenceIndex + 1)
                + "\t"
                + item.kind().name()
                + "\t"
                + item.weight()
                + "\t"
                + ReactionProductPredictionExecutor.evidenceDirection(item.weight())
                + "\t"
                + item.description());
      }
    }
    return List.copyOf(rows);
  }

  private static List<String> mechanismChannelRows(
      final List<ReactionMechanismChannelCandidate> mechanismChannels) {
    final ArrayList<String> rows = new ArrayList<String>(mechanismChannels.size() + 1);
    rows.add(
        "rank\tchannel_id\tname\tfamily\tprobability\tyield_percent\texplicit_product_graph\texplanation\tmaterialization_requirement");
    if (mechanismChannels.isEmpty()) {
      rows.add(
          "0\tnone\tnone\tUNKNOWN\t0.0\t0.0\tfalse\tNo unmaterialized mechanism channels were detected.\tnone");
      return List.copyOf(rows);
    }
    for (int i = 0; i < mechanismChannels.size(); ++i) {
      final ReactionMechanismChannelCandidate channel = mechanismChannels.get(i);
      rows.add(
          (i + 1)
              + "\t"
              + channel.channelId()
              + "\t"
              + channel.displayName()
              + "\t"
              + channel.family().name()
              + "\t"
              + channel.pathwayProbability()
              + "\t"
              + channel.expectedYieldPercent()
              + "\t"
              + Boolean.toString(channel.explicitProductGraphAvailable())
              + "\t"
              + channel.explanation()
              + "\t"
              + channel.materializationRequirement());
    }
    return List.copyOf(rows);
  }

  private List<String> computationalRoadmapRows(
      final ReactionPredictionResult result, final ReactionPathwayAssessment pathwayAssessment) {
    final int expectedRows = result.candidates().size() + result.mechanismChannels().size() + 2;
    final ArrayList<String> rows = new ArrayList<String>(expectedRows);
    rows.add(
        "rank\tchannel_type\tchannel_id\tproducts_or_channel\tfamily\tprobability\texplicit_product_graph"
            + "\tpathway_status\trecommended_calculation\tquantum_program_preparable\tblocking_reason");
    if (!result.hasCandidates() && !result.hasMechanismChannels()) {
      rows.add(
          "0\tnone\tnone\tnone\tUNKNOWN\t0.0\tfalse\tNO_CHANNELS\tNONE\tfalse\tNo reaction channels were predicted.");
      return List.copyOf(rows);
    }
    for (int i = 0; i < result.candidates().size(); ++i) {
      final ReactionProductCandidate candidate = result.candidates().get(i);
      rows.add(this.candidateRoadmapRow(i + 1, candidate, pathwayAssessment));
    }
    for (int i = 0; i < result.mechanismChannels().size(); ++i) {
      final ReactionMechanismChannelCandidate channel = result.mechanismChannels().get(i);
      rows.add(
          ReactionProductPredictionExecutor.mechanismChannelRoadmapRow(
              result.candidates().size() + i + 1, channel, pathwayAssessment));
    }
    return List.copyOf(rows);
  }

  private String candidateRoadmapRow(
      final int rank,
      final ReactionProductCandidate candidate,
      final ReactionPathwayAssessment pathwayAssessment) {
    final boolean explicitGraph =
        ReactionProductPredictionExecutor.readyForElectronicRerank(candidate);
    final boolean mechanismTraced = candidate.hasPredictedMechanisms();
    final String family =
        mechanismTraced
            ? ReactionProductPredictionExecutor.candidateMechanismFamilies(candidate)
            : "UNKNOWN";
    final String calculation =
        explicitGraph
            ? "REACTION_PATH_AND_ELECTRONIC_RERANK_AND_QUANTUM_PROGRAM_PREPARATION"
            : "PRODUCT_GRAPH_MATERIALIZATION";
    final String blockingReason =
        explicitGraph
            ? ReactionProductPredictionExecutor.pathwayBlockingReason(pathwayAssessment)
            : "Explicit product graph is required before electronic rerank and Hamiltonian preparation.";
    return rank
        + "\tproduct\tcandidate-"
        + rank
        + "\t"
        + this.formulaSummary(candidate.products())
        + "\t"
        + family
        + "\t"
        + candidate.probability()
        + "\t"
        + Boolean.toString(explicitGraph)
        + "\t"
        + ReactionProductPredictionExecutor.pathwayStatus(
            pathwayAssessment, explicitGraph, mechanismTraced)
        + "\t"
        + calculation
        + "\t"
        + Boolean.toString(explicitGraph)
        + "\t"
        + blockingReason;
  }

  private static String mechanismChannelRoadmapRow(
      final int rank,
      final ReactionMechanismChannelCandidate channel,
      final ReactionPathwayAssessment pathwayAssessment) {
    return rank
        + "\tmechanism\t"
        + channel.channelId()
        + "\t"
        + channel.displayName()
        + "\t"
        + channel.family().name()
        + "\t"
        + channel.pathwayProbability()
        + "\t"
        + Boolean.toString(channel.explicitProductGraphAvailable())
        + "\t"
        + ReactionProductPredictionExecutor.pathwayStatus(
            pathwayAssessment, channel.explicitProductGraphAvailable(), true)
        + "\t"
        + (channel.explicitProductGraphAvailable()
            ? "REACTION_PATH_AND_ELECTRONIC_RERANK"
            : "PRODUCT_GRAPH_MATERIALIZATION")
        + "\t"
        + Boolean.toString(channel.explicitProductGraphAvailable())
        + "\t"
        + channel.materializationRequirement();
  }

  private static String candidateMechanismFamilies(final ReactionProductCandidate candidate) {
    final StringBuilder builder = new StringBuilder();
    final List<PredictedReactionMechanism> mechanisms = candidate.predictedMechanisms();
    for (int i = 0; i < mechanisms.size(); ++i) {
      if (i > 0) {
        builder.append('|');
      }
      builder.append(mechanisms.get(i).family().name());
    }
    return builder.toString();
  }

  private static String pathwayStatus(
      final ReactionPathwayAssessment pathwayAssessment,
      final boolean explicitGraph,
      final boolean mechanismTraced) {
    if (!explicitGraph) {
      return "WAITING_FOR_PRODUCT_GRAPH";
    }
    if (!mechanismTraced) {
      return "WAITING_FOR_MECHANISM_ASSIGNMENT";
    }
    if (pathwayAssessment != null && pathwayAssessment.hasPathwaySteps()) {
      return "PATHWAY_STAGED";
    }
    return "PATHWAY_READY_FOR_NUMERICAL_SCAN";
  }

  private static String pathwayBlockingReason(final ReactionPathwayAssessment pathwayAssessment) {
    if (pathwayAssessment != null && pathwayAssessment.hasLimitations()) {
      return "Pathway has limiting evidence; inspect limiting_evidence before committing backend resources.";
    }
    return "none";
  }

  private List<String> candidatePathwayBarrierPreflightRows(final ReactionPredictionResult result) {
    final ArrayList<String> rows = new ArrayList<String>(result.candidates().size() + 1);
    rows.add(
        "rank\tproducts\tprobability\tstatus\tforward_barrier_kj_per_mol\treverse_barrier_kj_per_mol"
            + "\treaction_energy_kj_per_mol\tforward_rate\treverse_rate\tpathway_band\tblocking_reason");
    if (!result.hasCandidates()) {
      rows.add(
          "0\tnone\t0.0\tNO_EXPLICIT_PRODUCT_CANDIDATES\t0.0\t0.0\t0.0\t0.0\t0.0\tnone\tExplicit product graphs are required.");
      return List.copyOf(rows);
    }
    for (int i = 0; i < result.candidates().size(); ++i) {
      final ReactionProductCandidate candidate = result.candidates().get(i);
      if (!ReactionProductPredictionExecutor.readyForElectronicRerank(candidate)) {
        rows.add(
            (i + 1)
                + "\t"
                + this.formulaSummary(candidate.products())
                + "\t"
                + candidate.probability()
                + "\tWAITING_FOR_PRODUCT_GRAPH\t0.0\t0.0\t0.0\t0.0\t0.0\tnone\tExplicit product graph is required.");
        continue;
      }
      final Reaction candidateReaction =
          Reaction.of(
              ReactionId.of(result.request().id() + "_candidate_" + (i + 1) + "_pathway"),
              "Predicted product candidate pathway " + (i + 1),
              result.request().reactants(),
              candidate.products(),
              result.request().conditions(),
              ChemistryMetadata.EMPTY);
      final ReactionPathwayEstimate estimate = this.pathwayAnalyzer.analyze(candidateReaction);
      rows.add(
          (i + 1)
              + "\t"
              + this.formulaSummary(candidate.products())
              + "\t"
              + candidate.probability()
              + "\tREADY\t"
              + estimate.forwardBarrier()
              + "\t"
              + estimate.reverseBarrier()
              + "\t"
              + estimate.reactionEnergy()
              + "\t"
              + estimate.forwardRate()
              + "\t"
              + estimate.reverseRate()
              + "\t"
              + estimate.pathwayBand()
              + "\tnone");
    }
    return List.copyOf(rows);
  }

  private List<String> candidateFactorMatrixRows(final List<ReactionProductCandidate> candidates) {
    final ReactionPredictionEvidenceKind[] kinds = ReactionPredictionEvidenceKind.values();
    final ArrayList<String> rows = new ArrayList<String>(candidates.size() + 1);
    rows.add(
        "rank\tproducts\tprobability\t"
            + ReactionProductPredictionExecutor.factorMatrixHeader(kinds)
            + "\tpositive_weight\tnegative_weight\tnet_weight");
    if (candidates.isEmpty()) {
      rows.add(
          "0\tnone\t0.0\t"
              + ReactionProductPredictionExecutor.zeroFactorMatrix(kinds)
              + "\t0.0\t0.0\t0.0");
      return List.copyOf(rows);
    }
    for (int candidateIndex = 0; candidateIndex < candidates.size(); ++candidateIndex) {
      final ReactionProductCandidate candidate = candidates.get(candidateIndex);
      final StringBuilder row = new StringBuilder();
      row.append(candidateIndex + 1)
          .append('\t')
          .append(this.formulaSummary(candidate.products()))
          .append('\t')
          .append(candidate.probability());
      double positive = 0.0;
      double negative = 0.0;
      for (int kindIndex = 0; kindIndex < kinds.length; ++kindIndex) {
        final double weight = candidate.evidenceWeight(kinds[kindIndex]);
        row.append('\t').append(weight);
        if (weight > 0.0) {
          positive += weight;
        } else if (weight < 0.0) {
          negative += weight;
        }
      }
      row.append('\t')
          .append(positive)
          .append('\t')
          .append(negative)
          .append('\t')
          .append(positive + negative);
      rows.add(row.toString());
    }
    return List.copyOf(rows);
  }

  private List<String> candidateReadinessMatrixRows(
      final List<ReactionProductCandidate> candidates) {
    final ArrayList<String> rows = new ArrayList<String>(candidates.size() + 1);
    rows.add(
        "rank\tproducts\texplicit_product_graph\tmechanism_traced\tscore_traced"
            + "\telectronic_rerank_ready\tquantum_workflow_ready\tblocking_reason");
    if (candidates.isEmpty()) {
      rows.add("0\tnone\tfalse\tfalse\tfalse\tfalse\tfalse\tNo product candidates were generated.");
      return List.copyOf(rows);
    }
    for (int i = 0; i < candidates.size(); ++i) {
      final ReactionProductCandidate candidate = candidates.get(i);
      final boolean explicitGraph =
          ReactionProductPredictionExecutor.readyForElectronicRerank(candidate);
      final boolean mechanismTraced = candidate.hasPredictedMechanisms();
      final boolean scoreTraced = candidate.hasScoreBreakdown();
      rows.add(
          Integer.toString(i + 1)
              + "\t"
              + this.formulaSummary(candidate.products())
              + "\t"
              + Boolean.toString(explicitGraph)
              + "\t"
              + Boolean.toString(mechanismTraced)
              + "\t"
              + Boolean.toString(scoreTraced)
              + "\t"
              + Boolean.toString(
                  ReactionProductPredictionExecutor.readyForElectronicRerank(candidate))
              + "\t"
              + Boolean.toString(
                  ReactionProductPredictionExecutor.readyForQuantumWorkflow(candidate))
              + "\t"
              + ReactionProductPredictionExecutor.candidateReadinessBlockingReason(candidate));
    }
    return List.copyOf(rows);
  }

  private List<String> candidateElectronicRerankPreflightRows(
      final ClassicalCalculationRequest calculationRequest,
      final List<ReactionProductCandidate> candidates) {
    final ArrayList<String> rows =
        new ArrayList<String>(
            Math.min(candidates.size(), ELECTRONIC_RERANK_PREFLIGHT_ROW_LIMIT) + 2);
    rows.add(
        "rank\tproducts\tstatus\trequested_method\trequested_basis\tprepared_method\tprepared_basis\tcalculation_kind\treason");
    if (!ReactionProductPredictionExecutor.electronicRerankEnabled(calculationRequest)) {
      rows.add(
          "0\tnone\tdisabled\t"
              + calculationRequest.method().name().value()
              + "\t"
              + calculationRequest.basisSet().name().value()
              + "\tnone\tnone\tnone\tSet reaction_prediction_electronic_rerank=true to prepare candidate energy rerank requests.");
      return List.copyOf(rows);
    }
    if (candidates.isEmpty()) {
      rows.add(
          "0\tnone\tskipped\t"
              + calculationRequest.method().name().value()
              + "\t"
              + calculationRequest.basisSet().name().value()
              + "\tnone\tnone\tnone\tNo product candidates were generated.");
      return List.copyOf(rows);
    }
    final int rowLimit = Math.min(candidates.size(), ELECTRONIC_RERANK_PREFLIGHT_ROW_LIMIT);
    for (int i = 0; i < rowLimit; ++i) {
      final ReactionProductCandidate candidate = candidates.get(i);
      if (!ReactionProductPredictionExecutor.readyForElectronicRerank(candidate)) {
        rows.add(
            (i + 1)
                + "\t"
                + this.formulaSummary(candidate.products())
                + "\tblocked\t"
                + calculationRequest.method().name().value()
                + "\t"
                + calculationRequest.basisSet().name().value()
                + "\tnone\tnone\tnone\t"
                + ReactionProductPredictionExecutor.candidateReadinessBlockingReason(candidate));
        continue;
      }
      try {
        final ClassicalCalculationRequest energyRequest =
            this.electronicReranker.prepareEnergyRequest(calculationRequest, candidate, i);
        rows.add(
            (i + 1)
                + "\t"
                + this.formulaSummary(candidate.products())
                + "\tready\t"
                + calculationRequest.method().name().value()
                + "\t"
                + calculationRequest.basisSet().name().value()
                + "\t"
                + energyRequest.method().name().value()
                + "\t"
                + energyRequest.basisSet().name().value()
                + "\t"
                + energyRequest.calculationKinds().get(0).name()
                + "\tCandidate energy request can be prepared without running the expensive electronic calculation.");
      } catch (final IllegalArgumentException exception) {
        rows.add(
            (i + 1)
                + "\t"
                + this.formulaSummary(candidate.products())
                + "\tblocked\t"
                + calculationRequest.method().name().value()
                + "\t"
                + calculationRequest.basisSet().name().value()
                + "\tnone\tnone\tnone\t"
                + exception.getMessage());
      }
    }
    if (candidates.size() > rowLimit) {
      rows.add(
          "more\t"
              + (candidates.size() - rowLimit)
              + " candidates\ttruncated\t"
              + calculationRequest.method().name().value()
              + "\t"
              + calculationRequest.basisSet().name().value()
              + "\tnone\tnone\tnone\tElectronic rerank preflight rows are capped for UI responsiveness.");
    }
    return List.copyOf(rows);
  }

  private List<String> candidateQuantumWorkflowPreparationRows(
      final ClassicalCalculationRequest calculationRequest,
      final List<ReactionProductCandidate> candidates,
      final List<ClassicalDiagnostic> diagnostics) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add(
        "rank\tparticipant\tmolecule\tstatus\tprepared_method\tprepared_quality\tpreparation_mode\thamiltonian_completeness"
            + "\tprepared_basis\tgeometry_source\trequired_qubits\tpauli_terms\tvqe_ready\tvqe_status\tvqe_energy\tvqe_iterations\tqpe_available\tworkflow_status\treason");
    if (!ReactionProductPredictionExecutor.prepareQuantumWorkflows(calculationRequest)) {
      rows.add(
          "0\t0\tnone\tdisabled\tnone\tnone\tnone\tnone\tnone\tnone\t0\t0\tfalse\tnone\tnone\t0\tfalse\tnone\tSet "
              + PREPARE_QUANTUM_WORKFLOWS_OPTION
              + "=true to prepare candidate product quantum workflows.");
      return List.copyOf(rows);
    }
    if (candidates.isEmpty()) {
      rows.add(
          "0\t0\tnone\tskipped\tnone\tnone\tnone\tnone\tnone\tnone\t0\t0\tfalse\tnone\tnone\t0\tfalse\tnone\tNo product candidates were generated.");
      return List.copyOf(rows);
    }
    final int candidateLimit =
        Math.min(
            candidates.size(),
            ReactionProductPredictionExecutor.prepareQuantumWorkflowMaxCandidates(
                calculationRequest));
    final int participantLimit =
        ReactionProductPredictionExecutor.prepareQuantumWorkflowMaxParticipants(calculationRequest);
    for (int candidateIndex = 0; candidateIndex < candidates.size(); ++candidateIndex) {
      final ReactionProductCandidate candidate = candidates.get(candidateIndex);
      if (candidateIndex >= candidateLimit) {
        rows.add(
            (candidateIndex + 1)
                + "\t0\t"
                + this.formulaSummary(candidate.products())
                + "\tskipped\tnone\tnone\tnone\tnone\tnone\tnone\t0\t0\tfalse\tnone\tnone\t0\tfalse\tnone\tCandidate is outside configured quantum workflow preparation limit.");
        continue;
      }
      if (!ReactionProductPredictionExecutor.readyForQuantumWorkflow(candidate)) {
        rows.add(
            (candidateIndex + 1)
                + "\t0\t"
                + this.formulaSummary(candidate.products())
                + "\tskipped\tnone\tnone\tnone\tnone\tnone\tnone\t0\t0\tfalse\tnone\tnone\t0\tfalse\tnone\t"
                + ReactionProductPredictionExecutor.candidateReadinessBlockingReason(candidate));
        continue;
      }
      this.addCandidateQuantumWorkflowRows(
          rows, calculationRequest, candidate, candidateIndex, participantLimit, diagnostics);
    }
    return List.copyOf(rows);
  }

  private void addCandidateQuantumWorkflowRows(
      final ArrayList<String> rows,
      final ClassicalCalculationRequest calculationRequest,
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
                + "\tskipped\tnone\tnone\tnone\tnone\tnone\tnone\t0\t0\tfalse\tnone\tnone\t0\tfalse\tnone\tParticipant is outside configured quantum workflow preparation limit.");
        continue;
      }
      final ArrayList<ClassicalDiagnostic> localDiagnostics = new ArrayList<ClassicalDiagnostic>();
      final ClassicalCalculationRequest preparationRequest =
          this.electronicReranker.prepareQuantumPreparationRequest(
              calculationRequest, molecule, candidateIndex, participantIndex);
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
                + "\tfailed\tnone\tnone\tnone\tnone\tnone\tnone\t0\t0\tfalse\tnone\tnone\t0\tfalse\tnone\t"
                + ReactionProductPredictionExecutor.diagnosticSummary(localDiagnostics));
        continue;
      }
      final ArrayList<ClassicalDiagnostic> vqeDiagnostics = new ArrayList<ClassicalDiagnostic>();
      final ClassicalResultSection vqeSection =
          ReactionProductPredictionExecutor.executeCandidateVqeSection(
              calculationRequest, preparationRequest, section, vqeDiagnostics);
      diagnostics.addAll(vqeDiagnostics);
      rows.add(
          (candidateIndex + 1)
              + "\t"
              + (participantIndex + 1)
              + "\t"
              + molecule.displayName()
              + "\tprepared\t"
              + ReactionProductPredictionExecutor.resultText(section, "method")
              + "\t"
              + ReactionProductPredictionExecutor.resultText(section, "quality_level")
              + "\t"
              + ReactionProductPredictionExecutor.resultText(section, "preparation_mode")
              + "\t"
              + ReactionProductPredictionExecutor.resultText(section, "hamiltonian_completeness")
              + "\t"
              + ReactionProductPredictionExecutor.resultText(section, "basis_set")
              + "\t"
              + ReactionProductPredictionExecutor.resultText(section, "geometry_source")
              + "\t"
              + ReactionProductPredictionExecutor.resultText(
                  section, "requested_mapping_required_qubits")
              + "\t"
              + ReactionProductPredictionExecutor.resultText(
                  section, "requested_mapping_pauli_term_count")
              + "\t"
              + ReactionProductPredictionExecutor.resultText(section, "vqe_ready")
              + "\t"
              + ReactionProductPredictionExecutor.resultText(vqeSection, "vqe_status")
              + "\t"
              + ReactionProductPredictionExecutor.resultText(vqeSection, "vqe_best_energy")
              + "\t"
              + ReactionProductPredictionExecutor.resultText(vqeSection, "vqe_iteration_count")
              + "\t"
              + ReactionProductPredictionExecutor.resultText(section, "qpe_available")
              + "\t"
              + ReactionProductPredictionExecutor.resultText(
                  section, "quantum_workflow_coverage_status")
              + "\t"
              + ReactionProductPredictionExecutor.diagnosticSummary(localDiagnostics));
    }
  }

  private static ClassicalCalculationRequest quantumPreparationRequest(
      final ClassicalCalculationRequest calculationRequest,
      final Molecule molecule,
      final int candidateIndex,
      final int participantIndex) {
    return ClassicalCalculationRequest.of(
        ClassicalCalculationId.of(
            calculationRequest.id().value()
                + "_candidate_"
                + (candidateIndex + 1)
                + "_product_"
                + (participantIndex + 1)
                + "_quantum_preparation"),
        molecule,
        calculationRequest.method(),
        calculationRequest.basisSet(),
        null,
        List.of(ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION),
        calculationRequest.options(),
        calculationRequest.metadata());
  }

  private static ClassicalResultSection executeCandidateVqeSection(
      final ClassicalCalculationRequest originalRequest,
      final ClassicalCalculationRequest preparationRequest,
      final ClassicalResultSection preparationSection,
      final List<ClassicalDiagnostic> diagnostics) {
    if (!ReactionProductPredictionExecutor.executeQuantumWorkflowVqe(originalRequest)) {
      return null;
    }
    if (!"true"
        .equalsIgnoreCase(
            ReactionProductPredictionExecutor.resultText(preparationSection, "vqe_ready"))) {
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

  private static String candidateReadinessBlockingReason(final ReactionProductCandidate candidate) {
    if (candidate.hasEvidence(ReactionPredictionEvidenceKind.SURROGATE_PRODUCT)) {
      return "mechanism-only product channel has no explicit product graph";
    }
    if (candidate.products().participantCount() == 0) {
      return "candidate has no product molecule graph";
    }
    if (!candidate.hasPredictedMechanisms()) {
      return "candidate has no traced mechanism";
    }
    if (!candidate.hasScoreBreakdown()) {
      return "candidate has no score breakdown";
    }
    return "ready";
  }

  private static String evidenceDirection(final double weight) {
    if (weight > 0.0) {
      return "support";
    }
    if (weight < 0.0) {
      return "risk";
    }
    return "audit";
  }

  private static String factorMatrixHeader(final ReactionPredictionEvidenceKind[] kinds) {
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < kinds.length; ++i) {
      if (i > 0) {
        builder.append('\t');
      }
      builder.append(kinds[i].name().toLowerCase(Locale.ROOT));
    }
    return builder.toString();
  }

  private static String zeroFactorMatrix(final ReactionPredictionEvidenceKind[] kinds) {
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < kinds.length; ++i) {
      if (i > 0) {
        builder.append('\t');
      }
      builder.append("0.0");
    }
    return builder.toString();
  }

  private static String candidateSource(final ReactionProductCandidate candidate) {
    if (candidate.hasEvidence(ReactionPredictionEvidenceKind.SURROGATE_PRODUCT)) {
      return "surrogate";
    }
    if (ReactionProductPredictionExecutor.hasEvidenceDescription(
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

  private static String primaryMechanism(final ReactionProductCandidate candidate) {
    if (!candidate.hasPredictedMechanisms()) {
      return "none";
    }
    final PredictedReactionMechanism mechanism = candidate.predictedMechanisms().get(0);
    return ReactionProductPredictionExecutor.stableMechanismTraceId(mechanism.mechanismId())
        + ":"
        + mechanism.family().name();
  }

  private static String stableMechanismTraceId(final String mechanismId) {
    final String marker = "_product_prediction_";
    final int markerIndex = mechanismId.indexOf(marker);
    if (markerIndex < 0) {
      return mechanismId;
    }
    return mechanismId.substring(markerIndex + marker.length());
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

  private static int riskEvidenceCount(final ReactionProductCandidate candidate) {
    int count = 0;
    final List<ReactionPredictionEvidence> evidence = candidate.evidence();
    for (int i = 0; i < evidence.size(); ++i) {
      final ReactionPredictionEvidenceKind kind = evidence.get(i).kind();
      if (kind == ReactionPredictionEvidenceKind.WARNING
          || kind == ReactionPredictionEvidenceKind.COMPETING_PATHWAY) {
        ++count;
      }
    }
    return count;
  }

  private static double evidenceWeight(
      final ReactionProductCandidate candidate, final boolean positive) {
    double weight = 0.0;
    final List<ReactionPredictionEvidence> evidence = candidate.evidence();
    for (int i = 0; i < evidence.size(); ++i) {
      final double itemWeight = evidence.get(i).weight();
      if (positive && itemWeight > 0.0) {
        weight += itemWeight;
      } else if (!positive && itemWeight < 0.0) {
        weight += itemWeight;
      }
    }
    return weight;
  }

  private static String scoreBreakdownSummary(final ReactionProductCandidate candidate) {
    if (!candidate.hasScoreBreakdown()) {
      return "none";
    }
    return "positive="
        + candidate.scoreBreakdown().positiveWeight()
        + "; negative="
        + candidate.scoreBreakdown().negativeWeight()
        + "; normalized="
        + candidate.scoreBreakdown().normalizedProbability()
        + "; components="
        + ReactionProductPredictionExecutor.scoreComponentSummary(candidate);
  }

  private static String scoreComponentSummary(final ReactionProductCandidate candidate) {
    final List<ReactionPredictionScoreComponent> components =
        candidate.scoreBreakdown().components();
    if (components.isEmpty()) {
      return "none";
    }
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < components.size(); ++i) {
      if (i > 0) {
        builder.append(",");
      }
      builder
          .append(components.get(i).kind().name())
          .append(":")
          .append(components.get(i).weight());
    }
    return builder.toString();
  }

  private String formulaSummary(final ReactionSide side) {
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

  private String mechanismStepSummary(final List<PredictedReactionMechanism> mechanisms) {
    if (mechanisms.isEmpty()) {
      return "none";
    }
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < mechanisms.size(); ++i) {
      final PredictedReactionMechanism mechanism = mechanisms.get(i);
      final ReactionMechanismPattern pattern = this.mechanismCatalog.find(mechanism.family());
      if (pattern == null) {
        continue;
      }
      if (builder.length() > 0) {
        builder.append("; ");
      }
      builder.append(mechanism.mechanismId());
      builder.append("=");
      final List<ReactionMechanismPatternStep> steps = pattern.detailedSteps();
      for (int stepIndex = 0; stepIndex < steps.size(); ++stepIndex) {
        if (stepIndex > 0) {
          builder.append(" -> ");
        }
        builder.append(steps.get(stepIndex).eventKind().name());
      }
    }
    if (builder.length() == 0) {
      return "none";
    }
    return builder.toString();
  }

  private String pathwayStepSummary(final List<ReactionPathwayStep> steps) {
    if (steps.isEmpty()) {
      return "none";
    }
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < steps.size(); ++i) {
      final ReactionPathwayStep step = steps.get(i);
      if (i > 0) {
        builder.append(" -> ");
      }
      builder.append(step.mechanismId());
      builder.append("#");
      builder.append(step.orderIndex());
      builder.append(":");
      builder.append(step.eventKind().name());
      builder.append(":p=");
      builder.append(step.probabilityContribution());
    }
    return builder.toString();
  }

  private String selectivityNote(final ReactionProductCandidate candidate) {
    if (candidate.hasSelectivityNote()) {
      return candidate.selectivityNote();
    }
    return "not_specified";
  }

  private String mechanismSummary(final List<PredictedReactionMechanism> mechanisms) {
    if (mechanisms.isEmpty()) {
      return "none";
    }
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < mechanisms.size(); ++i) {
      final PredictedReactionMechanism mechanism = mechanisms.get(i);
      if (i > 0) {
        builder.append("; ");
      }
      builder.append(mechanism.mechanismId());
      builder.append(":");
      builder.append(mechanism.family());
      builder.append(":p=");
      builder.append(mechanism.pathwayProbability());
    }
    return builder.toString();
  }

  private static String conditionWindowSummary(final ReactionConditionWindow window) {
    final StringBuilder builder = new StringBuilder();
    builder.append("temperature=");
    builder.append(window.minimumTemperatureKelvin());
    builder.append("..");
    builder.append(window.maximumTemperatureKelvin());
    builder.append("K; duration=");
    builder.append(window.minimumDurationHours());
    builder.append("..");
    builder.append(window.maximumDurationHours());
    builder.append("h");
    if (window.hasRequiredConditionSignals()) {
      builder.append("; signals=");
      builder.append(String.join(",", window.requiredConditionSignals()));
    }
    if (window.note() != null) {
      builder.append("; note=");
      builder.append(window.note());
    }
    return builder.toString();
  }

  private String evidenceSummary(final List<ReactionPredictionEvidence> evidence) {
    if (evidence.isEmpty()) {
      return "none";
    }
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < evidence.size(); ++i) {
      final ReactionPredictionEvidence item = evidence.get(i);
      if (i > 0) {
        builder.append("; ");
      }
      builder.append(item.kind().name());
      builder.append("(");
      builder.append(item.weight());
      builder.append("): ");
      builder.append(item.description());
    }
    return builder.toString();
  }

  private enum MechanismQuality {
    PRODUCTION_DETAILED,
    CHEMICALLY_USABLE,
    SYNTHETIC_GENERIC
  }
}

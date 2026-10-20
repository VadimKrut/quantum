/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.ClassicalChemistryService;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.feature.MolecularFeatureAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultSection;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeCalibrationLibrary;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeCalibrationObservation;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeDistributionDataset;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeSourceQuality;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeBenchmarkReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureSet;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism.ReactionMechanismPattern;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism.ReactionMechanismPatternCatalog;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.prediction.ReactionPredictionApplicabilityReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.prediction.ReactionPredictionUncertaintyReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalCapability;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalPreflightStatus;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRule;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleCategory;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleLibrary;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleMatch;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.GeneratedReactionCandidateSet;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.template.OrganicReactionTemplateLibrary;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.OrganicReactionCandidateScorer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.OrganicReactionDomainConditionAdvisor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.OrganicReactionPredictionService;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionCandidateEnsembleNormalizer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionCandidateScore;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionPredictionApplicabilityAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionOutcomeBenchmarkEvaluator;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionOutcomeCalibrationScorer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionPredictionExplainer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionPredictionCalibrationApplier;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionPredictionElectronicReranker;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionPredictionUncertaintyAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionProductPredictionExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffectProfile;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffectAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffectKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioPlanner;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionSiteSelectivityAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionSiteSelectivityAssessment;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.RuleBasedOrganicReactionCandidateGenerator;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.rule.OrganicReactionConditionAssessment;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.rule.OrganicReactionConditionMatcher;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.rule.CuratedProductOrganicReactionRule;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.transform.BondAdditionTransformer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.transform.MolecularTransformProduct;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.BuiltInClassicalBackendProfiles;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.BuiltInOrganicReactionRuleLibraries;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.BuiltInOrganicReactionTemplateLibraries;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.BuiltInReactionMechanismPatternCatalog;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.BuiltInReactionOutcomeCalibrationLibraries;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.BondAdditionTransform;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.ReactantAtomSelector;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryValidationResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethod;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanisticEventKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.PredictedReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionMechanismClass;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionExplanation;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionMethodType;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionScoreComponentKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionMechanismChannelCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionRegioselectivityClass;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentLoading;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentPurity;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeature;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionId;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionPhase;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.StoichiometricEquivalent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.StoichiometricCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.FormalCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.RadicalState;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Temperature;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TemperatureUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TimeDuration;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TimeDurationUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.SubstanceAmount;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.SubstanceAmountUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.validation.ChemistryCoreValidator;

final class OrganicReactionPredictionServiceTest {

  @Test
  void matchedRuleWithoutProductGraphIsPreservedAsMechanismChannel() {
    final OrganicReactionRule mechanismOnlyRule =
        OrganicReactionPredictionServiceTest.mechanismOnlyRule();
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                OrganicReactionRuleLibrary.of(List.of(mechanismOnlyRule))));
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "mechanism_only_channel",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        OrganicReactionPredictionServiceTest.ringMolecule(
                            "mechanism_only_pyridine", "N"),
                        StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(120.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(3.0, TimeDurationUnit.HOUR),
                List.of(),
                List.of(ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING)),
                "mechanism-only channel test"),
            List.of(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            List.of(),
            "The mechanism channel must survive even when no product graph is generated.");

    final ReactionPredictionResult result = service.predict(request);

    Assertions.assertFalse(result.hasCandidates());
    Assertions.assertTrue(result.hasMechanismChannels());
    Assertions.assertEquals(1, result.mechanismChannels().size());
    Assertions.assertEquals(1, result.predictedMechanismCount());
    Assertions.assertEquals(
        ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION,
        result.mechanismChannels().get(0).family());
    Assertions.assertEquals(0.73, result.mechanismChannels().get(0).pathwayProbability(), 1.0E-12);
    Assertions.assertFalse(result.mechanismChannels().get(0).explicitProductGraphAvailable());
    Assertions.assertTrue(
        result.mechanismChannels().get(0).materializationRequirement().contains("product graph"));
    Assertions.assertTrue(result.hasDiagnostic(ReactionPredictionEvidenceKind.WARNING));
  }

  @Test
  void executorReportsUnmaterializedMechanismChannelsInResultSection() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                OrganicReactionRuleLibrary.of(
                    List.of(OrganicReactionPredictionServiceTest.mechanismOnlyRule()))));
    final ReactionProductPredictionExecutor executor =
        new ReactionProductPredictionExecutor(service, new MolecularFeatureAnalyzer());
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("mechanism_only_executor_reaction"),
            "Mechanism-only executor reaction",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        OrganicReactionPredictionServiceTest.ringMolecule(
                            "mechanism_only_executor_pyridine", "N"),
                        StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        OrganicReactionPredictionServiceTest.ringMolecule(
                            "mechanism_only_executor_declared_product", "N"),
                        StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(120.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(3.0, TimeDurationUnit.HOUR),
                List.of(),
                List.of(ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING)),
                "executor mechanism-only channel test"),
            ChemistryMetadata.EMPTY);
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("mechanism_only_executor"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION),
            List.of(),
            ChemistryMetadata.EMPTY);
    final ArrayList<ClassicalDiagnostic> diagnostics = new ArrayList<ClassicalDiagnostic>();

    final ClassicalResultSection section = executor.execute(request, diagnostics);

    Assertions.assertNotNull(section, diagnostics.toString());
    Assertions.assertEquals(
        "1.0", section.requireFirstValue("unmaterialized_mechanism_channel_count"));
    Assertions.assertEquals(
        "MECHANISM_CHANNELS_WITHOUT_PRODUCT_GRAPHS",
        section.requireFirstValue("prediction_coverage_status"));
    Assertions.assertEquals(
        "MECHANISM_CHANNELS_WITHOUT_PRODUCT_GRAPHS",
        section.requireFirstValue("product_graph_coverage_status"));
    Assertions.assertEquals(
        "true", section.requireFirstValue("product_graph_followup_recommended"));
    Assertions.assertTrue(
        section
            .requireFirstValue("product_graph_followup_reason")
            .contains("explicit product graphs"),
        section.requireFirstValue("product_graph_followup_reason"));
    Assertions.assertTrue(
        section.requireValues("prediction_followup_plan").stream()
            .anyMatch(
                row ->
                    row.contains("REACTION_PRODUCT_PREDICTION")
                        && row.contains("REQUIRED")
                        && row.contains("no explicit product graph")),
        section.requireValues("prediction_followup_plan").toString());
    Assertions.assertTrue(
        section.requireValues("candidate_electronic_rerank_preflight").stream()
            .anyMatch(
                row ->
                    row.contains("disabled")
                        && row.contains("reaction_prediction_electronic_rerank=true")),
        section.requireValues("candidate_electronic_rerank_preflight").toString());
    Assertions.assertTrue(
        section.requireValues("unmaterialized_mechanism_channels").stream()
            .anyMatch(
                row ->
                    row.contains("template_only_snar_probe")
                        && row.contains("NUCLEOPHILIC_AROMATIC_SUBSTITUTION")
                        && row.contains("product graph")),
        section.requireValues("unmaterialized_mechanism_channels").toString());
    Assertions.assertTrue(
        section.requireValues("prediction_computational_roadmap").stream()
            .anyMatch(
                row ->
                    row.contains("template_only_snar_probe")
                        && row.contains("NUCLEOPHILIC_AROMATIC_SUBSTITUTION")
                        && row.contains("WAITING_FOR_PRODUCT_GRAPH")
                        && row.contains("PRODUCT_GRAPH_MATERIALIZATION")),
        section.requireValues("prediction_computational_roadmap").toString());
    Assertions.assertTrue(
        section.requireValues("candidate_pathway_barrier_preflight").stream()
            .anyMatch(
                row ->
                    row.contains("NO_EXPLICIT_PRODUCT_CANDIDATES")
                        && row.contains("Explicit product graphs are required")),
        section.requireValues("candidate_pathway_barrier_preflight").toString());
  }

  @Test
  void executorReportsPathwayBarrierPreflightForExplicitProductGraph() {
    final ReactionProductPredictionExecutor executor = new ReactionProductPredictionExecutor();
    final Molecule hydrogen =
        OrganicReactionPredictionServiceTest.twoAtomMolecule(
            "pathway_preflight_h2", "H", "H", BondType.SINGLE);
    final Molecule chlorine =
        OrganicReactionPredictionServiceTest.twoAtomMolecule(
            "pathway_preflight_cl2", "Cl", "Cl", BondType.SINGLE);
    final Molecule hydrogenChloride =
        OrganicReactionPredictionServiceTest.twoAtomMolecule(
            "pathway_preflight_hcl", "H", "Cl", BondType.SINGLE);
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("pathway_preflight_seed_reaction"),
            "Pathway preflight seed reaction",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(hydrogen, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(chlorine, StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(hydrogenChloride, StoichiometricCoefficient.of(2)))));
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("pathway_preflight_seed_prediction"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION),
            List.of(
                ClassicalCalculationOption.bool(
                    "reaction_prediction_use_product_side_as_seed", true)),
            ChemistryMetadata.EMPTY);

    final ClassicalResultSection section =
        executor.execute(request, new ArrayList<ClassicalDiagnostic>());

    Assertions.assertTrue(
        section.requireValues("candidate_pathway_barrier_preflight").stream()
            .anyMatch(row -> row.contains("READY") && !row.contains("WAITING_FOR_PRODUCT_GRAPH")),
        section.requireValues("candidate_pathway_barrier_preflight").toString());
    Assertions.assertTrue(
        section.requireValues("prediction_computational_roadmap").stream()
            .anyMatch(
                row ->
                    row.contains("product") && row.contains("REACTION_PATH_AND_ELECTRONIC_RERANK")),
        section.requireValues("prediction_computational_roadmap").toString());
  }

  @Test
  void applicabilityAndUncertaintyUseMechanismPatternQuality() {
    final ReactionMechanismPatternCatalog catalog =
        BuiltInReactionMechanismPatternCatalog.organicAndPhysicalChemistry();
    final ReactionMechanismPattern productionPattern =
        OrganicReactionPredictionServiceTest.firstProductionDetailedPattern(catalog);
    final ReactionMechanismPattern genericPattern =
        ReactionMechanismPattern.of(
            ReactionMechanismFamily.UNKNOWN,
            "Synthetic fallback quality probe",
            List.of(MechanisticEventKind.UNKNOWN),
            List.of("insufficient evidence"),
            List.of("manual review required"));
    final ReactionMechanismPatternCatalog qualityCatalog =
        ReactionMechanismPatternCatalog.of(List.of(productionPattern, genericPattern));
    final Molecule productionProduct =
        OrganicReactionPredictionServiceTest.ringMolecule("quality_production_product", "N");
    final Molecule genericProduct =
        OrganicReactionPredictionServiceTest.ringMolecule("quality_generic_product", "C");
    final ReactionProductCandidate productionCandidate =
        OrganicReactionPredictionServiceTest.candidate(
                productionProduct, 0.58, "production detailed channel")
            .withPredictedMechanisms(
                List.of(
                    PredictedReactionMechanism.of(
                        "quality_production_mechanism", productionPattern.family(), 0.58, 58.0)));
    final ReactionProductCandidate genericCandidate =
        OrganicReactionPredictionServiceTest.candidate(
                genericProduct, 0.42, "generic fallback channel")
            .withPredictedMechanisms(
                List.of(
                    PredictedReactionMechanism.of(
                        "quality_generic_mechanism", genericPattern.family(), 0.42, 42.0)));
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "mechanism_quality_applicability",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        OrganicReactionPredictionServiceTest.ringMolecule("quality_reactant", "N"),
                        StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(120.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(3.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.of(
                        ReactionComponentRole.SOLVENT,
                        "PEG4000",
                        SubstanceAmount.of(70.0, SubstanceAmountUnit.GRAM),
                        null,
                        null,
                        null,
                        ReactionComponentPurity.percent(99.0),
                        ReactionPhase.LIQUID,
                        "polar polymer solvent")),
                List.of(ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM)),
                "quality test conditions"),
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(),
            "Mechanism quality must be visible in applicability and uncertainty.");
    final ReactionPredictionResult result =
        ReactionPredictionResult.of(
            request,
            ReactionPredictionMethodType.HYBRID,
            List.of(productionCandidate, genericCandidate),
            List.of());
    final ReactionPredictionApplicabilityAnalyzer applicabilityAnalyzer =
        new ReactionPredictionApplicabilityAnalyzer(new MolecularFeatureAnalyzer(), qualityCatalog);
    final ReactionPredictionApplicabilityReport applicability =
        applicabilityAnalyzer.analyze(result, ReactionConditionEffectProfile.EMPTY);
    final ReactionPredictionUncertaintyReport uncertainty =
        new ReactionPredictionUncertaintyAnalyzer().analyze(result, applicability);

    Assertions.assertTrue(applicability.productionDetailedMechanismCount() > 0);
    Assertions.assertTrue(applicability.genericFallbackMechanismCount() > 0);
    Assertions.assertTrue(applicability.applicabilityScore() > 0.0);
    Assertions.assertTrue(uncertainty.uncertaintyScore() > 0.0);
    Assertions.assertTrue(
        applicability.traceRows().stream()
            .anyMatch(row -> row.contains("production_detailed_mechanism_count")));
    Assertions.assertTrue(
        applicability.traceRows().stream()
            .anyMatch(row -> row.contains("generic_fallback_mechanism_count")));
    Assertions.assertTrue(
        uncertainty.rows().stream()
            .anyMatch(row -> row.contains("generic_fallback_mechanism_count")));
  }

  @Test
  void conditionMatcherInfersSupportiveFeaturesFromPhysicalContext() {
    final OrganicReactionConditionMatcher matcher = new OrganicReactionConditionMatcher();
    final ReactionConditions conditions =
        ReactionConditions.of(
            Temperature.of(120.0, TemperatureUnit.CELSIUS),
            null,
            TimeDuration.of(3.0, TimeDurationUnit.HOUR),
            List.of(
                ReactionConditionComponent.of(
                    ReactionComponentRole.SOLVENT,
                    "PEG4000",
                    SubstanceAmount.of(70.0, SubstanceAmountUnit.GRAM),
                    null,
                    null,
                    null,
                    ReactionComponentPurity.percent(99.0),
                    ReactionPhase.LIQUID,
                    "polar aprotic polymer solvent"),
                ReactionConditionComponent.named(ReactionComponentRole.BASE, "K2CO3")),
            List.of(),
            "sealed microwave reaction under nitrogen atmosphere");
    final OrganicReactionConditionAssessment assessment =
        matcher.assess(
            conditions,
            List.of(
                ReactionConditionFeatureKind.HEATING,
                ReactionConditionFeatureKind.BASIC_MEDIUM,
                ReactionConditionFeatureKind.INERT_ATMOSPHERE,
                ReactionConditionFeatureKind.MICROWAVE,
                ReactionConditionFeatureKind.SEALED_VESSEL));
    Assertions.assertFalse(assessment.stronglyIncompatible());
    Assertions.assertTrue(assessment.scoreAdjustment() > 0.09);
    Assertions.assertEquals(
        5L,
        assessment.evidence().stream()
            .filter(evidence -> evidence.description().contains("inferred context"))
            .count());
  }

  @Test
  void conditionScenarioPlannerCoversOrganicActivationAndMoistureControls() {
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "condition_activation_matrix",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        OrganicReactionPredictionServiceTest.carbonylSubtypeAndOxygenAcidProbe(),
                        StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(120.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(3.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.of(
                        ReactionComponentRole.SOLVENT,
                        "PEG4000",
                        SubstanceAmount.of(70.0, SubstanceAmountUnit.GRAM),
                        null,
                        null,
                        null,
                        ReactionComponentPurity.percent(99.0),
                        ReactionPhase.LIQUID,
                        "high-boiling polar medium")),
                List.of(),
                "Scenario planner must expose condition branches for organic prediction."),
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(),
            "Condition scenario coverage test.");

    final List<ReactionConditionScenarioKind> kinds =
        new ReactionConditionScenarioPlanner()
            .plan(request).scenarios().stream().map(scenario -> scenario.kind()).toList();

    Assertions.assertTrue(kinds.contains(ReactionConditionScenarioKind.OXIDANT_CONTROL));
    Assertions.assertTrue(kinds.contains(ReactionConditionScenarioKind.REDUCTANT_CONTROL));
    Assertions.assertTrue(kinds.contains(ReactionConditionScenarioKind.HYDROGENATION_CONTROL));
    Assertions.assertTrue(kinds.contains(ReactionConditionScenarioKind.PHOTOCHEMICAL_CONTROL));
    Assertions.assertTrue(kinds.contains(ReactionConditionScenarioKind.ELECTROCHEMICAL_CONTROL));
    Assertions.assertTrue(kinds.contains(ReactionConditionScenarioKind.MOISTURE_CONTROL));
    Assertions.assertTrue(kinds.contains(ReactionConditionScenarioKind.DEHYDRATION_CONTROL));
  }

  @Test
  void conditionEffectAnalyzerUsesStructuredRedoxAndHydrogenationFeatures() {
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "structured_redox_feature_effects",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        OrganicReactionPredictionServiceTest.carbonylSubtypeAndOxygenAcidProbe(),
                        StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(25.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(1.0, TimeDurationUnit.HOUR),
                List.of(),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.OXIDIZING_MEDIUM),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.REDUCING_MEDIUM),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HYDROGENATION),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.PHOTOCHEMICAL),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.ELECTROCHEMICAL)),
                "Structured condition features should drive condition effects without text parsing."),
            List.of(OrganicReactionDomain.OXIDATION_REDUCTION),
            List.of(),
            "Structured feature effect test.");

    final ReactionConditionEffectProfile profile =
        new ReactionConditionEffectAnalyzer().analyze(request);

    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.OXIDANT_PRESENT));
    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.REDUCTANT_PRESENT));
    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.HYDROGENATION_CATALYSIS));
    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.PHOTOCHEMICAL_ACTIVATION));
    Assertions.assertTrue(
        profile.hasEffect(ReactionConditionEffectKind.ELECTROCHEMICAL_ACTIVATION));
  }

  @Test
  void classicalServiceRanksHeterocycleCandidateAheadOfHydrocarbonCandidate() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Molecule pyridine = OrganicReactionPredictionServiceTest.ringMolecule("pyridine", "N");
    final Molecule benzene = OrganicReactionPredictionServiceTest.ringMolecule("benzene", "C");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "heterocycle_screen",
            ReactionSide.of(
                List.of(ReactionParticipant.of(pyridine, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                null,
                null,
                TimeDuration.of(12.0, TimeDurationUnit.HOUR),
                List.of(),
                List.of(ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS)),
                "screen"),
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(
                OrganicReactionPredictionServiceTest.candidate(benzene, 0.5, "hydrocarbon"),
                OrganicReactionPredictionServiceTest.candidate(pyridine, 0.5, "heterocycle")),
            null);

    final ReactionPredictionResult result = service.predictReactionProducts(request);

    Assertions.assertTrue(result.hasCandidates());
    Assertions.assertEquals(1.0, result.probabilityMass(), 1.0E-12);
    Assertions.assertTrue(result.probabilityMassComplete(1.0E-12));
    Assertions.assertEquals(0.0, result.missingProbabilityMass(), 1.0E-12);
    Assertions.assertTrue(result.hasDiagnostics());
    Assertions.assertTrue(result.diagnosticCount() > 0);
    Assertions.assertEquals(
        result.diagnosticCount(ReactionPredictionEvidenceKind.WARNING),
        result.diagnostics(ReactionPredictionEvidenceKind.WARNING).size());
    Assertions.assertTrue(
        result.candidates().stream()
            .anyMatch(
                candidate ->
                    candidate.products().participants().get(0).molecule().equals(pyridine)));
    Assertions.assertTrue(result.bestCandidate().probability() > 0.0);
    Assertions.assertTrue(result.bestCandidate().hasScoreBreakdown());
    Assertions.assertEquals(
        result.bestCandidate().probability(),
        result.bestCandidate().scoreBreakdown().normalizedProbability(),
        1.0E-12);
    Assertions.assertTrue(result.bestCandidate().hasEvidence());
    Assertions.assertTrue(
        result.bestCandidate().hasEvidence(ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN));
    Assertions.assertTrue(
        result.bestCandidate().hasEvidence(ReactionPredictionEvidenceKind.THERMODYNAMIC_FACTOR));
    Assertions.assertTrue(
        result.bestCandidate().hasEvidence(ReactionPredictionEvidenceKind.KINETIC_FACTOR));
    Assertions.assertTrue(
        result.bestCandidate().hasEvidence(ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR));
    Assertions.assertTrue(
        result.bestCandidate().hasEvidence(ReactionPredictionEvidenceKind.COMPETING_PATHWAY));
    Assertions.assertTrue(
        result.bestCandidate().hasEvidence(ReactionPredictionEvidenceKind.USER_RULE));
    Assertions.assertTrue(
        result.bestCandidate().evidenceWeight(ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN)
            > 0.0);
    Assertions.assertTrue(
        result.bestCandidate().evidence().stream()
            .anyMatch(evidence -> evidence.description().contains("bond event(s): broken=")));
    Assertions.assertTrue(
        result.bestCandidate().evidence().stream()
            .anyMatch(evidence -> evidence.description().contains("Pathway thermal model: T=")));
    Assertions.assertTrue(
        result.bestCandidate().evidence().stream()
            .anyMatch(evidence -> evidence.description().contains("Forward/reverse rate ratio=")));
    Assertions.assertTrue(result.bestCandidate().scoreBreakdown().positiveWeight() > 0.0);
    Assertions.assertTrue(
        result
            .bestCandidate()
            .scoreBreakdown()
            .hasComponent(ReactionPredictionScoreComponentKind.HETEROCYCLE));
    Assertions.assertTrue(
        result
            .bestCandidate()
            .scoreBreakdown()
            .hasComponent(ReactionPredictionScoreComponentKind.THERMODYNAMIC));
    Assertions.assertTrue(
        result
            .bestCandidate()
            .scoreBreakdown()
            .hasComponent(ReactionPredictionScoreComponentKind.KINETIC));
    Assertions.assertTrue(
        result
            .bestCandidate()
            .scoreBreakdown()
            .hasComponent(ReactionPredictionScoreComponentKind.ELECTRONIC));
    Assertions.assertTrue(
        result
            .bestCandidate()
            .scoreBreakdown()
            .hasComponent(ReactionPredictionScoreComponentKind.SELECTIVITY));
    Assertions.assertTrue(
        result
            .bestCandidate()
            .scoreBreakdown()
            .hasComponent(ReactionPredictionScoreComponentKind.MECHANISM_MATCH));
    Assertions.assertTrue(
        result
                .bestCandidate()
                .scoreBreakdown()
                .componentWeight(ReactionPredictionScoreComponentKind.HETEROCYCLE)
            > 0.0);
    Assertions.assertFalse(
        result
            .candidatesWithEvidence(ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN)
            .isEmpty());
    Assertions.assertTrue(
        result.bestCandidate().scoreBreakdown().components().stream()
            .anyMatch(
                component -> component.kind() == ReactionPredictionScoreComponentKind.HETEROCYCLE));
    Assertions.assertTrue(
        result.bestCandidate().evidence().stream()
            .anyMatch(
                evidence -> evidence.kind() == ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN));
  }

  @Test
  void classicalServiceDoesNotApplyCalibrationPriorsByDefault() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Molecule chloroAzaHeterocycle =
        OrganicReactionPredictionServiceTest.selectivityProbeHeterocycle();
    final Molecule benzylamineLikeNucleophile =
        OrganicReactionPredictionServiceTest.benzylamine("default_prior_off_benzylamine");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "default_mechanistic_prediction_without_calibration_prior",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(chloroAzaHeterocycle, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(
                        benzylamineLikeNucleophile, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(120.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(3.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.named(ReactionComponentRole.SOLVENT, "PEG4000"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.BASE, "inorganic carbonate base")),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS)),
                "heated viscous polyether aza-heteroaryl amination screen"),
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            List.of(),
            "Default service prediction must not use outcome history as prior.");

    final ReactionPredictionResult result = service.predictReactionProducts(request);

    Assertions.assertTrue(result.hasCandidates());
    Assertions.assertTrue(
        result.candidates().stream()
            .flatMap(candidate -> candidate.evidence().stream())
            .noneMatch(
                evidence -> evidence.kind() == ReactionPredictionEvidenceKind.CALIBRATION_PRIOR),
        result.candidates().toString());
  }

  @Test
  void ensembleNormalizerDemotesMechanismOnlySurrogateBelowConcreteProduct() {
    final Molecule concreteProduct =
        OrganicReactionPredictionServiceTest.ringMolecule("concrete_surrogate_rank_product", "N");
    final Molecule surrogateProduct =
        OrganicReactionPredictionServiceTest.ringMolecule("surrogate_rank_product", "C");
    final ReactionProductCandidate concreteCandidate =
        OrganicReactionPredictionServiceTest.candidate(
            concreteProduct, 0.5, "concrete graph product");
    final ReactionProductCandidate surrogateCandidate =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(ReactionParticipant.of(surrogateProduct, StoichiometricCoefficient.ONE))),
            0.5,
            50.0,
            "mechanism only surrogate",
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.SURROGATE_PRODUCT,
                    "Mechanism-only surrogate retained because no graph product was generated.",
                    -0.08)));
    final ReactionCandidateEnsembleNormalizer normalizer =
        new ReactionCandidateEnsembleNormalizer();

    final List<ReactionCandidateScore> normalized =
        normalizer.adjust(
            List.of(
                ReactionCandidateScore.of(concreteCandidate, concreteCandidate.evidence(), 1.0, 0),
                ReactionCandidateScore.of(
                    surrogateCandidate, surrogateCandidate.evidence(), 1.0, 1)));

    Assertions.assertTrue(normalized.get(0).score() > normalized.get(1).score());
  }

  @Test
  void electronicRerankerSkipsMechanismOnlySurrogateProductChannels() {
    final Molecule reactant =
        OrganicReactionPredictionServiceTest.ringMolecule("rerank_surrogate_reactant", "N");
    final Molecule surrogateProduct =
        OrganicReactionPredictionServiceTest.ringMolecule("rerank_surrogate_product", "C");
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("rerank_surrogate_reaction"),
            "Rerank surrogate reaction",
            ReactionSide.of(
                List.of(ReactionParticipant.of(reactant, StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(ReactionParticipant.of(surrogateProduct, StoichiometricCoefficient.ONE))));
    final ReactionProductCandidate surrogateCandidate =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(ReactionParticipant.of(surrogateProduct, StoichiometricCoefficient.ONE))),
            1.0,
            100.0,
            "mechanism only surrogate",
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.SURROGATE_PRODUCT,
                    "Mechanism-only surrogate retained because no graph product was generated.",
                    -0.08)));
    final ReactionPredictionRequest predictionRequest =
        ReactionPredictionRequest.of(
            "rerank_surrogate_prediction",
            reaction.reactants(),
            ReactionConditions.EMPTY,
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(),
            "Electronic rerank must not score mechanism-only channels.");
    final ReactionPredictionResult predictionResult =
        ReactionPredictionResult.of(
            predictionRequest,
            ReactionPredictionMethodType.HYBRID,
            List.of(surrogateCandidate),
            List.of());
    final ClassicalCalculationRequest calculationRequest =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("rerank_surrogate_calculation"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION),
            List.of(ClassicalCalculationOption.bool("reaction_prediction_electronic_rerank", true)),
            ChemistryMetadata.EMPTY);

    final ReactionPredictionResult reranked =
        new ReactionPredictionElectronicReranker()
            .rerank(calculationRequest, predictionResult, new java.util.ArrayList<>());

    Assertions.assertFalse(
        reranked.candidates().get(0).hasEvidence(ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR));
    Assertions.assertTrue(
        reranked.candidates().get(0).evidence().stream()
            .anyMatch(
                evidence -> evidence.description().contains("mechanism-only product channel")));
  }

  @Test
  void electronicRerankerPreparesGeneratedBasisForHalogenProductsAndKeepsMechanismChannels() {
    final Molecule hydrogen =
        OrganicReactionPredictionServiceTest.twoAtomMolecule(
            "rerank_generated_basis_h2", "H", "H", BondType.SINGLE);
    final Molecule bromine =
        OrganicReactionPredictionServiceTest.twoAtomMolecule(
            "rerank_generated_basis_br2", "Br", "Br", BondType.SINGLE);
    final Molecule hydrogenBromide =
        OrganicReactionPredictionServiceTest.twoAtomMolecule(
            "rerank_generated_basis_hbr", "H", "Br", BondType.SINGLE);
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("rerank_generated_basis_reaction"),
            "Rerank generated basis reaction",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(hydrogen, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(bromine, StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(ReactionParticipant.of(hydrogenBromide, StoichiometricCoefficient.of(2)))));
    final ReactionProductCandidate productCandidate =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(ReactionParticipant.of(hydrogenBromide, StoichiometricCoefficient.of(2)))),
            1.0,
            100.0,
            "explicit graph halogen product",
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.USER_RULE,
                    "explicit halogen product graph",
                    0.3)));
    final ReactionMechanismChannelCandidate channel =
        ReactionMechanismChannelCandidate.of(
            "rerank_generated_basis_channel",
            "Generated basis channel",
            ReactionMechanismFamily.RADICAL_SUBSTITUTION,
            0.41,
            38.0,
            false,
            "parallel radical channel without explicit graph",
            "Provide explicit product graph before candidate-level scoring.");
    final ReactionPredictionRequest predictionRequest =
        ReactionPredictionRequest.of(
            "rerank_generated_basis_prediction",
            reaction.reactants(),
            ReactionConditions.EMPTY,
            List.of(OrganicReactionDomain.RADICAL_FUNCTIONALIZATION),
            List.of(),
            "Electronic rerank must keep mechanism channels.");
    final ReactionPredictionResult predictionResult =
        ReactionPredictionResult.of(
            predictionRequest,
            ReactionPredictionMethodType.HYBRID,
            List.of(productCandidate),
            List.of(channel),
            List.of());
    final ClassicalCalculationRequest calculationRequest =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("rerank_generated_basis_calculation"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION),
            List.of(
                ClassicalCalculationOption.bool("reaction_prediction_electronic_rerank", true),
                ClassicalCalculationOption.text(
                    "reaction_prediction_electronic_rerank_max_atoms", "8")),
            ChemistryMetadata.EMPTY);

    final ReactionPredictionElectronicReranker reranker =
        new ReactionPredictionElectronicReranker();
    final ClassicalCalculationRequest energyRequest =
        reranker.prepareEnergyRequest(calculationRequest, productCandidate, 0);
    final ReactionPredictionResult skippedByBudget =
        reranker.rerank(
            ClassicalCalculationRequest.of(
                ClassicalCalculationId.of("rerank_generated_basis_skip_calculation"),
                reaction,
                ElectronicStructureMethod.HARTREE_FOCK,
                BasisSet.STO_3G,
                null,
                List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION),
                List.of(
                    ClassicalCalculationOption.bool("reaction_prediction_electronic_rerank", true),
                    ClassicalCalculationOption.text(
                        "reaction_prediction_electronic_rerank_max_atoms", "1")),
                ChemistryMetadata.EMPTY),
            predictionResult,
            new ArrayList<ClassicalDiagnostic>());

    Assertions.assertEquals(ElectronicStructureMethod.HARTREE_FOCK, energyRequest.method());
    Assertions.assertEquals("AUTO-EVEN-TEMPERED-COMPACT", energyRequest.basisSet().name().value());
    Assertions.assertEquals(
        List.of(ClassicalCalculationKind.REACTION_ENERGY), energyRequest.calculationKinds());
    Assertions.assertEquals(1, skippedByBudget.mechanismChannels().size());
    Assertions.assertEquals(channel, skippedByBudget.mechanismChannels().get(0));
    Assertions.assertTrue(
        skippedByBudget.candidates().stream()
            .anyMatch(
                candidate ->
                    OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                        candidate, "reaction_prediction_electronic_rerank_max_atoms")),
        skippedByBudget.candidates().toString());
  }

  @Test
  void predictionServiceRemovesSurrogateProductInsteadOfReturningFakeProduct() {
    final Molecule reactant =
        OrganicReactionPredictionServiceTest.ringMolecule("legacy_fallback_reactant", "N");
    final Molecule placeholderProduct =
        OrganicReactionPredictionServiceTest.ringMolecule("legacy_fallback_placeholder", "C");
    final ReactionProductCandidate fallbackCandidate =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(ReactionParticipant.of(placeholderProduct, StoichiometricCoefficient.ONE))),
            0.4,
            40.0,
            "legacy placeholder",
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.SURROGATE_PRODUCT,
                    "Fallback single-site surrogate retained only when graph-level channel cannot be used.",
                    -0.12)));
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            request -> GeneratedReactionCandidateSet.of(List.of(fallbackCandidate), List.of()));
    final ReactionPredictionRequest request =
        OrganicReactionPredictionServiceTest.request(
            "legacy_fallback_removed",
            reactant,
            List.of(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            List.of(ReactionConditionFeatureKind.HEATING));

    final ReactionPredictionResult result = service.predict(request);

    Assertions.assertFalse(result.hasCandidates());
    Assertions.assertTrue(
        result.diagnostics().stream()
            .anyMatch(
                evidence ->
                    evidence
                        .description()
                        .contains("Only surrogate or mechanism-only products were generated")),
        result.diagnostics().toString());
  }

  @Test
  void predictionServiceRemovesCalibrationEvidenceFromSeedCandidates() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(new MolecularFeatureAnalyzer());
    final Molecule pyridine =
        OrganicReactionPredictionServiceTest.ringMolecule("seed_calibration_pyridine", "N");
    final ReactionProductCandidate seed =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(ReactionParticipant.of(pyridine, StoichiometricCoefficient.ONE))),
            0.7,
            70.0,
            "seed candidate with benchmark-only evidence",
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.CALIBRATION_PRIOR,
                    "Benchmark observation matched without changing prediction probability: external row.",
                    0.0),
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.USER_RULE,
                    "User supplied candidate topology.",
                    0.05)));
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "seed_calibration_evidence_stripped",
            ReactionSide.of(
                List.of(ReactionParticipant.of(pyridine, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                null,
                null,
                TimeDuration.of(2.0, TimeDurationUnit.HOUR),
                List.of(),
                List.of(),
                "calibration seed stripping"),
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(seed),
            "Benchmark evidence must not survive inside prediction candidates.");

    final ReactionPredictionResult result = service.predict(request);

    Assertions.assertTrue(result.hasCandidates());
    Assertions.assertTrue(
        result.candidates().stream()
            .flatMap(candidate -> candidate.evidence().stream())
            .noneMatch(
                evidence -> evidence.kind() == ReactionPredictionEvidenceKind.CALIBRATION_PRIOR));
    Assertions.assertTrue(
        result.candidates().stream()
            .flatMap(candidate -> candidate.evidence().stream())
            .anyMatch(evidence -> evidence.kind() == ReactionPredictionEvidenceKind.USER_RULE));
    Assertions.assertTrue(
        result.candidates().stream()
            .filter(ReactionProductCandidate::hasScoreBreakdown)
            .flatMap(candidate -> candidate.scoreBreakdown().components().stream())
            .noneMatch(
                component -> component.kind() == ReactionPredictionScoreComponentKind.CALIBRATION));
  }

  @Test
  void predictionServiceRemovesCalibrationEvidenceFromGeneratedCandidates() {
    final Molecule reactant =
        OrganicReactionPredictionServiceTest.halogenatedNitrogenHeterocycle(
            "generated_calibration_reactant", "Cl");
    final Molecule generatedProduct =
        OrganicReactionPredictionServiceTest.dehalogenatedScaffold(
            reactant, "generated_calibration_product");
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                OrganicReactionRuleLibrary.of(
                    List.of(
                        new OrganicReactionPredictionServiceTest.CalibrationContaminatedRule(
                            generatedProduct)))));
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "generated_calibration_evidence_stripped",
            ReactionSide.of(
                List.of(ReactionParticipant.of(reactant, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(120.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(3.0, TimeDurationUnit.HOUR),
                List.of(ReactionConditionComponent.named(ReactionComponentRole.BASE, "amine base")),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM)),
                "generated rule benchmark evidence stripping"),
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            List.of(),
            "Generated benchmark evidence must be audit-only and never rank products.");

    final ReactionPredictionResult result = service.predict(request);

    Assertions.assertTrue(result.hasCandidates());
    Assertions.assertTrue(
        result.candidates().stream()
            .flatMap(candidate -> candidate.evidence().stream())
            .noneMatch(
                evidence -> evidence.kind() == ReactionPredictionEvidenceKind.CALIBRATION_PRIOR));
    Assertions.assertTrue(
        result.candidates().stream()
            .filter(ReactionProductCandidate::hasScoreBreakdown)
            .flatMap(candidate -> candidate.scoreBreakdown().components().stream())
            .noneMatch(
                component -> component.kind() == ReactionPredictionScoreComponentKind.CALIBRATION));
    Assertions.assertTrue(
        result.candidates().stream()
            .flatMap(candidate -> candidate.evidence().stream())
            .anyMatch(evidence -> evidence.description().contains("generated structural product")));
  }

  @Test
  void userSeedDoesNotOverrideMechanisticCandidateDistribution() {
    final Molecule reactant =
        OrganicReactionPredictionServiceTest.halogenatedNitrogenHeterocycle(
            "seed_bias_reactant", "Cl");
    final Molecule weakSeed =
        OrganicReactionPredictionServiceTest.ringMolecule("weak_user_seed_benzene", "C");
    final Molecule generatedProduct =
        OrganicReactionPredictionServiceTest.dehalogenatedScaffold(
            reactant, "generated_heterocycle_candidate");
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                OrganicReactionRuleLibrary.of(
                    List.of(
                        new OrganicReactionPredictionServiceTest.FixedProductRule(
                            generatedProduct)))));
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "user_seed_must_not_be_lookup_answer",
            ReactionSide.of(
                List.of(ReactionParticipant.of(reactant, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(90.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(3.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.named(
                        ReactionComponentRole.BASE, "tertiary amine base")),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM)),
                "user seed competes with mechanism-generated product"),
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            List.of(
                OrganicReactionPredictionServiceTest.candidate(
                    weakSeed,
                    0.90,
                    "user-declared product seed for validation, not a lookup answer")),
            null);

    final ReactionPredictionResult result = service.predict(request);
    final ReactionProductCandidate weakSeedCandidate =
        result.candidates().stream()
            .filter(
                candidate -> candidate.products().participants().get(0).molecule().equals(weakSeed))
            .findFirst()
            .orElseThrow();

    Assertions.assertEquals(1.0, result.probabilityMass(), 1.0E-12);
    Assertions.assertTrue(
        result.bestCandidate().products().participants().get(0).molecule().equals(generatedProduct),
        OrganicReactionPredictionServiceTest.candidateSummary(result));
    Assertions.assertTrue(result.bestCandidate().probability() > weakSeedCandidate.probability());
    Assertions.assertTrue(
        result.diagnostics().stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.WARNING
                        && evidence.description().contains("User-seeded product 1")
                        && evidence.description().contains("C (required 6, supplied 5)")));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasEvidenceContaining(
            weakSeedCandidate, "candidate hypothesis"));
  }

  @Test
  void scorerAddsStepwiseMechanismEvidenceForBalancedBondChangeCandidate() {
    final Molecule reactant =
        OrganicReactionPredictionServiceTest.twoCarbonMolecule("screen_c2_single", BondType.SINGLE);
    final Molecule product =
        OrganicReactionPredictionServiceTest.twoCarbonMolecule("screen_c2_double", BondType.DOUBLE);
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "balanced_bond_change_screen",
            ReactionSide.of(
                List.of(ReactionParticipant.of(reactant, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(80.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(2.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.of(
                        ReactionComponentRole.CATALYST,
                        "photoredox catalyst",
                        null,
                        null,
                        ReactionPhase.SOLUTION,
                        "activation")),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.CATALYST_PRESENT),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.PHOTOCHEMICAL)),
                "balanced structural candidate"),
            List.of(OrganicReactionDomain.OXIDATION_REDUCTION),
            List.of(
                OrganicReactionPredictionServiceTest.candidate(product, 0.5, "balanced candidate")),
            null);

    final ReactionCandidateScore score =
        new OrganicReactionCandidateScorer().score(request, request.seedCandidates().get(0), 0);

    Assertions.assertTrue(score.score() > 0.0);
    Assertions.assertTrue(
        score.evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR
                        && evidence.description().contains("bond event(s): broken=")));
    Assertions.assertTrue(
        score.evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.KINETIC_FACTOR
                        && evidence.description().contains("Pathway thermal model: T=")));
    Assertions.assertTrue(
        score.evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.COMPETING_PATHWAY
                        && evidence.description().contains("Forward/reverse rate ratio=")));
    Assertions.assertTrue(
        score.evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.USER_RULE
                        && evidence.description().contains("Mechanism")
                        && evidence.description().contains("event")));
  }

  @Test
  void scorerCountsStoichiometricCoefficientsWhenCheckingMaterialBalance() {
    final Molecule carbon =
        OrganicReactionPredictionServiceTest.singleAtomMolecule("coefficient_carbon", "C");
    final Molecule dicarbon =
        OrganicReactionPredictionServiceTest.twoCarbonMolecule(
            "coefficient_dicarbon", BondType.SINGLE);
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "stoichiometric_material_balance",
            ReactionSide.of(
                List.of(ReactionParticipant.of(carbon, StoichiometricCoefficient.of(2)))),
            ReactionConditions.EMPTY,
            List.of(OrganicReactionDomain.CROSS_COUPLING),
            List.of(
                ReactionProductCandidate.of(
                    ReactionSide.of(
                        List.of(ReactionParticipant.of(dicarbon, StoichiometricCoefficient.ONE))),
                    0.5,
                    50.0,
                    "generic dimer candidate",
                    List.of(
                        ReactionPredictionEvidence.of(
                            ReactionPredictionEvidenceKind.USER_RULE,
                            "Candidate generated from a generic coupling transform.",
                            0.05)))),
            null);

    final ReactionCandidateScore score =
        new OrganicReactionCandidateScorer().score(request, request.seedCandidates().get(0), 0);

    Assertions.assertTrue(
        score.evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR
                        && evidence
                            .description()
                            .contains("conserves nearly all non-hydrogen atoms")));
    Assertions.assertFalse(
        score.evidence().stream()
            .anyMatch(evidence -> evidence.description().contains("creates non-hydrogen atoms")));
  }

  @Test
  void scorerPenalizesUnexplainedProductAtomsWithoutUsingHistoricalLookup() {
    final Molecule carbon =
        OrganicReactionPredictionServiceTest.singleAtomMolecule("unexplained_carbon", "C");
    final Molecule carbonMonoxide =
        OrganicReactionPredictionServiceTest.twoAtomMolecule(
            "unexplained_carbon_oxygen", "C", "O", BondType.DOUBLE);
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "unexplained_product_atom",
            ReactionSide.of(List.of(ReactionParticipant.of(carbon, StoichiometricCoefficient.ONE))),
            ReactionConditions.EMPTY,
            List.of(OrganicReactionDomain.OXIDATION_REDUCTION),
            List.of(
                OrganicReactionPredictionServiceTest.candidate(
                    carbonMonoxide, 0.5, "oxidized candidate")),
            null);

    final ReactionCandidateScore score =
        new OrganicReactionCandidateScorer().score(request, request.seedCandidates().get(0), 0);

    Assertions.assertTrue(
        score.evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.WARNING
                        && evidence.description().contains("creates non-hydrogen atoms")));
    Assertions.assertTrue(
        score.evidence().stream()
            .filter(evidence -> evidence.description().contains("creates non-hydrogen atoms"))
            .anyMatch(evidence -> evidence.weight() <= -0.22));
  }

  @Test
  void scorerUsesConditionComponentsAsGenericMaterialSourcesButNotCalibration() {
    final Molecule carbon =
        OrganicReactionPredictionServiceTest.singleAtomMolecule("water_source_carbon", "C");
    final Molecule carbonMonoxide =
        OrganicReactionPredictionServiceTest.twoAtomMolecule(
            "water_source_carbon_oxygen", "C", "O", BondType.DOUBLE);
    final ReactionPredictionRequest withoutSource =
        ReactionPredictionRequest.of(
            "oxygen_without_condition_source",
            ReactionSide.of(List.of(ReactionParticipant.of(carbon, StoichiometricCoefficient.ONE))),
            ReactionConditions.EMPTY,
            List.of(OrganicReactionDomain.OXIDATION_REDUCTION),
            List.of(
                OrganicReactionPredictionServiceTest.candidate(
                    carbonMonoxide, 0.5, "oxidized candidate")),
            null);
    final ReactionPredictionRequest withSource =
        ReactionPredictionRequest.of(
            "oxygen_with_condition_source",
            ReactionSide.of(List.of(ReactionParticipant.of(carbon, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                null,
                null,
                TimeDuration.of(1.0, TimeDurationUnit.HOUR),
                List.of(ReactionConditionComponent.named(ReactionComponentRole.ADDITIVE, "water")),
                List.of(),
                "oxygen source declared as reaction component"),
            List.of(OrganicReactionDomain.OXIDATION_REDUCTION),
            List.of(
                OrganicReactionPredictionServiceTest.candidate(
                    carbonMonoxide, 0.5, "oxidized candidate")),
            null);
    final OrganicReactionCandidateScorer scorer = new OrganicReactionCandidateScorer();

    final ReactionCandidateScore unsupported =
        scorer.score(withoutSource, withoutSource.seedCandidates().get(0), 0);
    final ReactionCandidateScore supported =
        scorer.score(withSource, withSource.seedCandidates().get(0), 0);

    final double unsupportedMaterialBalanceWeight =
        OrganicReactionPredictionServiceTest.evidenceWeightContaining(
            unsupported, "creates non-hydrogen atoms");
    final double supportedMaterialBalanceWeight =
        OrganicReactionPredictionServiceTest.evidenceWeightContaining(
            supported, "plausible material source");

    Assertions.assertTrue(supportedMaterialBalanceWeight > unsupportedMaterialBalanceWeight);
    Assertions.assertTrue(
        supported.evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.WARNING
                        && evidence.description().contains("plausible material source")));
    Assertions.assertFalse(
        supported.evidence().stream()
            .anyMatch(
                evidence -> evidence.kind() == ReactionPredictionEvidenceKind.CALIBRATION_PRIOR));
  }

  @Test
  void scorerParsesFormulaLikeConditionComponentsAsMaterialSources() {
    final Molecule carbon =
        OrganicReactionPredictionServiceTest.singleAtomMolecule("formula_source_carbon", "C");
    final Molecule carbonMonoxide =
        OrganicReactionPredictionServiceTest.twoAtomMolecule(
            "formula_source_carbon_oxygen", "C", "O", BondType.DOUBLE);
    final ReactionPredictionRequest withCarbonate =
        ReactionPredictionRequest.of(
            "oxygen_from_formula_component",
            ReactionSide.of(List.of(ReactionParticipant.of(carbon, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                null,
                null,
                TimeDuration.of(1.0, TimeDurationUnit.HOUR),
                List.of(ReactionConditionComponent.named(ReactionComponentRole.BASE, "K2CO3")),
                List.of(),
                "formula component source"),
            List.of(OrganicReactionDomain.OXIDATION_REDUCTION),
            List.of(
                OrganicReactionPredictionServiceTest.candidate(
                    carbonMonoxide, 0.5, "oxygenated candidate")),
            null);
    final ReactionPredictionRequest withWaterFormula =
        ReactionPredictionRequest.of(
            "oxygen_from_water_formula",
            ReactionSide.of(List.of(ReactionParticipant.of(carbon, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                null,
                null,
                TimeDuration.of(1.0, TimeDurationUnit.HOUR),
                List.of(ReactionConditionComponent.named(ReactionComponentRole.ADDITIVE, "H2O")),
                List.of(),
                "formula component source"),
            List.of(OrganicReactionDomain.OXIDATION_REDUCTION),
            List.of(
                OrganicReactionPredictionServiceTest.candidate(
                    carbonMonoxide, 0.5, "oxygenated candidate")),
            null);
    final OrganicReactionCandidateScorer scorer = new OrganicReactionCandidateScorer();

    final ReactionCandidateScore carbonateScore =
        scorer.score(withCarbonate, withCarbonate.seedCandidates().get(0), 0);
    final ReactionCandidateScore waterScore =
        scorer.score(withWaterFormula, withWaterFormula.seedCandidates().get(0), 0);

    Assertions.assertTrue(
        carbonateScore.evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.WARNING
                        && evidence.description().contains("plausible material source")));
    Assertions.assertTrue(
        waterScore.evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.WARNING
                        && evidence.description().contains("plausible material source")));
  }

  @Test
  void predictionResultContainsRequestAuditDiagnosticsForRiskyOrganicSetup() {
    final OrganicReactionPredictionService service = new OrganicReactionPredictionService();
    final Molecule benzene =
        OrganicReactionPredictionServiceTest.ringMolecule("audit_benzene", "C");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "audit_risky_heterocycle_setup",
            ReactionSide.of(
                List.of(ReactionParticipant.of(benzene, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                null,
                null,
                null,
                List.of(),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.COOLING),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.AIR_SENSITIVE)),
                "contradictory condition audit"),
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.CROSS_COUPLING),
            List.of(OrganicReactionPredictionServiceTest.candidate(benzene, 0.4, "risk candidate")),
            null);

    final ReactionPredictionResult result = service.predict(request);

    Assertions.assertTrue(result.hasCandidates());
    Assertions.assertTrue(result.hasDiagnostics());
    Assertions.assertTrue(result.diagnosticCount() > 0);
    Assertions.assertTrue(result.hasDiagnostic(ReactionPredictionEvidenceKind.WARNING));
    Assertions.assertFalse(result.diagnostics(ReactionPredictionEvidenceKind.WARNING).isEmpty());
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasDiagnosticContaining(
            result, "reactants contain no detected heterocycle"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasDiagnosticContaining(
            result, "no catalyst component"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasDiagnosticContaining(
            result, "both COOLING and HEATING"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasDiagnosticContaining(
            result, "without INERT_ATMOSPHERE"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasDiagnosticContaining(
            result, "unassigned probability mass"));
  }

  @Test
  void conditionRichRequestAddsTemperatureCatalystSolventAndKineticEvidence() {
    final OrganicReactionPredictionService service = new OrganicReactionPredictionService();
    final Molecule pyridine =
        OrganicReactionPredictionServiceTest.ringMolecule("condition_pyridine", "N");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "condition_rich_heterocycle_screen",
            ReactionSide.of(
                List.of(ReactionParticipant.of(pyridine, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(80.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(6.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.of(
                        ReactionComponentRole.SOLVENT,
                        "DMF",
                        null,
                        null,
                        ReactionPhase.SOLUTION,
                        "polar aprotic solvent"),
                    ReactionConditionComponent.of(
                        ReactionComponentRole.CATALYST,
                        "Pd(PPh3)4",
                        null,
                        null,
                        null,
                        ReactionComponentLoading.percent(5.0),
                        null,
                        ReactionPhase.SOLID,
                        "cross-coupling catalyst"),
                    ReactionConditionComponent.named(ReactionComponentRole.LIGAND, "PPh3")),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.INERT_ATMOSPHERE),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.CATALYST_PRESENT)),
                "condition-rich heterocycle screen"),
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(
                OrganicReactionPredictionServiceTest.candidate(pyridine, 0.6, "condition rich")),
            null);

    final ReactionPredictionResult result = service.predict(request);

    Assertions.assertTrue(result.hasCandidates());
    Assertions.assertEquals(353.15, request.conditions().temperature().kelvin(), 1.0E-12);
    Assertions.assertTrue(
        result.bestCandidate().evidence().stream()
            .anyMatch(
                evidence -> evidence.kind() == ReactionPredictionEvidenceKind.TEMPERATURE_EFFECT));
    Assertions.assertTrue(
        result.bestCandidate().evidence().stream()
            .anyMatch(
                evidence -> evidence.kind() == ReactionPredictionEvidenceKind.CATALYST_EFFECT));
    Assertions.assertTrue(
        result.bestCandidate().evidence().stream()
            .anyMatch(
                evidence -> evidence.kind() == ReactionPredictionEvidenceKind.SOLVENT_EFFECT));
    Assertions.assertTrue(
        result.bestCandidate().evidence().stream()
            .anyMatch(
                evidence -> evidence.kind() == ReactionPredictionEvidenceKind.KINETIC_FACTOR));
    Assertions.assertTrue(
        result.bestCandidate().evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.description().contains("Heterocycle functionalization mechanism")
                        && evidence.description().contains("detailed staged events")
                        && evidence.description().contains("Heteroatom protonation")));
    Assertions.assertTrue(
        result.bestCandidate().scoreBreakdown().components().stream()
            .anyMatch(
                component ->
                    component.kind() == ReactionPredictionScoreComponentKind.MECHANISM_MATCH
                        && component
                            .description()
                            .contains("Heterocycle functionalization mechanism")));
  }

  @Test
  void predictionNormalizationKeepsPredictedMechanismPathways() {
    final OrganicReactionPredictionService service = new OrganicReactionPredictionService();
    final Molecule pyridine =
        OrganicReactionPredictionServiceTest.ringMolecule("mechanism_pyridine", "N");
    final ReactionProductCandidate candidate =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(ReactionParticipant.of(pyridine, StoichiometricCoefficient.ONE))),
            0.7,
            55.0,
            "mechanism candidate",
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.USER_RULE, "seeded mechanism pathway", 0.2)),
            List.of(
                PredictedReactionMechanism.of(
                    "snar_pathway",
                    ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION,
                    0.7,
                    55.0)));
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "mechanism_preservation",
            ReactionSide.of(
                List.of(ReactionParticipant.of(pyridine, StoichiometricCoefficient.ONE))),
            ReactionConditions.EMPTY,
            List.of(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            List.of(candidate),
            null);

    final ReactionPredictionResult result = service.predict(request);

    Assertions.assertTrue(result.bestCandidate().hasPredictedMechanisms());
    Assertions.assertEquals(
        ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION,
        result.bestCandidate().predictedMechanisms().get(0).family());
    Assertions.assertTrue(
        result.bestCandidate().evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.description().contains("snar_pathway") && evidence.weight() > 0.0));
    Assertions.assertTrue(
        result.bestCandidate().scoreBreakdown().components().stream()
            .anyMatch(
                component ->
                    component.kind() == ReactionPredictionScoreComponentKind.MECHANISM_MATCH
                        && component.description().contains("snar_pathway")));
  }

  @Test
  void predictionMergesDuplicateSeedAndGeneratedProductChannels() {
    final Molecule substrate =
        OrganicReactionPredictionServiceTest.halogenatedNitrogenHeterocycle(
            "duplicate_substrate", "Cl");
    final Molecule product =
        OrganicReactionPredictionServiceTest.dehalogenatedScaffold(
            substrate, "duplicate_scaffold_product");
    final ReactionProductCandidate seedCandidate =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(ReactionParticipant.of(product, StoichiometricCoefficient.ONE))),
            0.30,
            30.0,
            "seed heterocycle product channel",
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.USER_RULE,
                    "seed supplied candidate evidence",
                    0.10)),
            List.of(
                PredictedReactionMechanism.of(
                    "seed_mechanism",
                    ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION,
                    0.3,
                    30.0)));
    final OrganicReactionRule duplicateRule =
        new OrganicReactionRule() {
          public OrganicReactionRuleId id() {
            return OrganicReactionRuleId.of("duplicate_generated_rule");
          }

          public String displayName() {
            return "Duplicate generated heterocycle product rule";
          }

          public OrganicReactionRuleCategory category() {
            return OrganicReactionRuleCategory.HETEROCYCLE_FUNCTIONALIZATION;
          }

          public OrganicReactionRuleMatch match(final ReactionPredictionRequest request) {
            return OrganicReactionRuleMatch.matched(0.72, "duplicate product channel matched");
          }

          public List<ReactionProductCandidate> generate(
              final ReactionPredictionRequest request, final OrganicReactionRuleMatch match) {
            return List.of(
                ReactionProductCandidate.of(
                    seedCandidate.products(),
                    0.40,
                    44.0,
                    "generated heterocycle product channel",
                    List.of(
                        ReactionPredictionEvidence.of(
                            ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN,
                            "generated rule evidence",
                            0.16))));
          }
        };
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                OrganicReactionRuleLibrary.of(List.of(duplicateRule))));
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "duplicate_channel_merge",
            ReactionSide.of(
                List.of(ReactionParticipant.of(substrate, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(90.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(4.0, TimeDurationUnit.HOUR),
                List.of(),
                List.of(ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING)),
                "duplicate channel merge screen"),
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(seedCandidate),
            null);

    final ReactionPredictionResult result = service.predict(request);

    Assertions.assertEquals(1, result.candidates().size());
    Assertions.assertEquals(1.0, result.probabilityMass(), 1.0E-12);
    Assertions.assertTrue(result.bestCandidate().hasPredictedMechanisms());
    Assertions.assertTrue(
        result.bestCandidate().predictedMechanisms().stream()
            .anyMatch(mechanism -> mechanism.mechanismId().equals("seed_mechanism")));
    Assertions.assertTrue(
        result.bestCandidate().predictedMechanisms().stream()
            .anyMatch(mechanism -> mechanism.mechanismId().equals("duplicate_generated_rule")));
    Assertions.assertTrue(
        result.bestCandidate().evidence().stream()
            .anyMatch(
                evidence -> evidence.description().contains("seed supplied candidate evidence")));
    Assertions.assertTrue(
        result.bestCandidate().evidence().stream()
            .anyMatch(evidence -> evidence.description().contains("generated rule evidence")));
    Assertions.assertTrue(
        result.bestCandidate().evidence().stream()
            .anyMatch(
                evidence -> evidence.description().contains("Merged duplicate product channel")));
  }

  @Test
  void mechanismPatternAssessmentRewardsMatchedApplicabilitySignals() {
    final OrganicReactionCandidateScorer scorer = new OrganicReactionCandidateScorer();
    final Molecule substrate =
        OrganicReactionPredictionServiceTest.halogenatedNitrogenHeterocycle(
            "coupling_substrate", "Br");
    final ReactionProductCandidate candidate =
        OrganicReactionPredictionServiceTest.candidate(substrate, 0.45, "same coupling product");
    final ReactionPredictionRequest supportedRequest =
        ReactionPredictionRequest.of(
            "supported_cross_coupling",
            ReactionSide.of(
                List.of(ReactionParticipant.of(substrate, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(90.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(8.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.named(ReactionComponentRole.CATALYST, "Pd(PPh3)4"),
                    ReactionConditionComponent.named(ReactionComponentRole.BASE, "K2CO3")),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.INERT_ATMOSPHERE)),
                "aryl halide with palladium catalyst and base"),
            List.of(OrganicReactionDomain.CROSS_COUPLING),
            List.of(candidate),
            null);
    final ReactionPredictionRequest unsupportedRequest =
        ReactionPredictionRequest.of(
            "unsupported_cross_coupling",
            supportedRequest.reactants(),
            ReactionConditions.EMPTY,
            List.of(OrganicReactionDomain.CROSS_COUPLING),
            List.of(candidate),
            null);

    final ReactionCandidateScore supported = scorer.score(supportedRequest, candidate, 0);
    final ReactionCandidateScore unsupported = scorer.score(unsupportedRequest, candidate, 0);

    Assertions.assertTrue(supported.score() > unsupported.score());
    Assertions.assertTrue(
        supported.evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.description().contains("Organometallic cross-coupling")
                        && evidence.description().contains("applicability=")
                        && !evidence.description().contains("applicability=none")
                        && evidence.description().contains("risks=none")));
    Assertions.assertTrue(
        unsupported.evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.description().contains("Organometallic cross-coupling")
                        && evidence.description().contains("applicability=none")));
  }

  @Test
  void builtInMechanismCatalogCoversEveryRegisteredMechanismFamily() {
    final ReactionMechanismPatternCatalog catalog =
        BuiltInReactionMechanismPatternCatalog.organicAndPhysicalChemistry();

    for (final ReactionMechanismFamily family : ReactionMechanismFamily.values()) {
      Assertions.assertNotNull(catalog.find(family), family.name());
      Assertions.assertFalse(catalog.require(family).eventSequence().isEmpty());
      Assertions.assertEquals(
          catalog.require(family).eventSequence().size(),
          catalog.require(family).detailedSteps().size(),
          family.name());
      Assertions.assertEquals(
          1.0, catalog.require(family).pathwayProbabilityContribution(), 1.0E-12, family.name());
      if (family != ReactionMechanismFamily.UNKNOWN) {
        Assertions.assertTrue(catalog.require(family).eventSequence().size() > 1, family.name());
        Assertions.assertFalse(
            catalog.require(family).applicabilitySignals().isEmpty(), family.name());
        Assertions.assertTrue(
            catalog.require(family).detailedSteps().get(0).hasApplicabilitySignals(),
            family.name());
      }
    }
  }

  @Test
  void builtInMechanismCatalogContainsDetailedModernOrganicMechanisms() {
    final ReactionMechanismPatternCatalog catalog =
        BuiltInReactionMechanismPatternCatalog.organicAndPhysicalChemistry();

    OrganicReactionPredictionServiceTest.assertMechanismPattern(
        catalog, ReactionMechanismFamily.DIAZOTIZATION, MechanisticEventKind.DIAZONIUM_FORMATION);
    OrganicReactionPredictionServiceTest.assertMechanismPattern(
        catalog, ReactionMechanismFamily.SANDMEYER, MechanisticEventKind.DIAZONIUM_SUBSTITUTION);
    OrganicReactionPredictionServiceTest.assertMechanismPattern(
        catalog,
        ReactionMechanismFamily.GRIGNARD_OR_ORGANOLITHIUM_ADDITION,
        MechanisticEventKind.ORGANOMETALLIC_ADDITION);
    OrganicReactionPredictionServiceTest.assertMechanismPattern(
        catalog, ReactionMechanismFamily.ESTERIFICATION, MechanisticEventKind.ESTERIFICATION);
    OrganicReactionPredictionServiceTest.assertMechanismPattern(
        catalog,
        ReactionMechanismFamily.TRANSESTERIFICATION,
        MechanisticEventKind.TRANSESTERIFICATION);
    OrganicReactionPredictionServiceTest.assertMechanismPattern(
        catalog, ReactionMechanismFamily.HYDROLYSIS, MechanisticEventKind.HYDROLYSIS);
    OrganicReactionPredictionServiceTest.assertMechanismPattern(
        catalog, ReactionMechanismFamily.SOLVOLYSIS, MechanisticEventKind.SOLVOLYSIS);
    OrganicReactionPredictionServiceTest.assertMechanismPattern(
        catalog, ReactionMechanismFamily.FRAGMENTATION, MechanisticEventKind.FRAGMENTATION);
    OrganicReactionPredictionServiceTest.assertMechanismPattern(
        catalog,
        ReactionMechanismFamily.RADICAL_CYCLIZATION,
        MechanisticEventKind.RADICAL_CYCLIZATION);
    OrganicReactionPredictionServiceTest.assertMechanismPattern(
        catalog, ReactionMechanismFamily.C_H_ACTIVATION, MechanisticEventKind.C_H_ACTIVATION);
    OrganicReactionPredictionServiceTest.assertMechanismPattern(
        catalog,
        ReactionMechanismFamily.POLYMERIZATION_CHAIN_GROWTH,
        MechanisticEventKind.CHAIN_GROWTH_PROPAGATION);
    OrganicReactionPredictionServiceTest.assertMechanismPattern(
        catalog,
        ReactionMechanismFamily.POLYMERIZATION_STEP_GROWTH,
        MechanisticEventKind.STEP_GROWTH_COUPLING);
    OrganicReactionPredictionServiceTest.assertMechanismPattern(
        catalog, ReactionMechanismFamily.OZONOLYSIS, MechanisticEventKind.OZONIDE_CLEAVAGE);
    OrganicReactionPredictionServiceTest.assertMechanismPattern(
        catalog, ReactionMechanismFamily.BAEYER_VILLIGER, MechanisticEventKind.ALKYL_MIGRATION);
    OrganicReactionPredictionServiceTest.assertMechanismPattern(
        catalog, ReactionMechanismFamily.NITRATION, MechanisticEventKind.NITRONIUM_FORMATION);
    OrganicReactionPredictionServiceTest.assertMechanismPattern(
        catalog,
        ReactionMechanismFamily.SUZUKI_COUPLING,
        MechanisticEventKind.PALLADIUM_TRANSMETALATION);
    OrganicReactionPredictionServiceTest.assertMechanismPattern(
        catalog,
        ReactionMechanismFamily.SONOGASHIRA_COUPLING,
        MechanisticEventKind.COPPER_ACETYLIDE_FORMATION);
    OrganicReactionPredictionServiceTest.assertMechanismPattern(
        catalog,
        ReactionMechanismFamily.BUCHWALD_HARTWIG_AMINATION,
        MechanisticEventKind.AMINATION_LIGAND_EXCHANGE);
    OrganicReactionPredictionServiceTest.assertMechanismPattern(
        catalog,
        ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION,
        MechanisticEventKind.OXIDATIVE_ADDITION);
    OrganicReactionPredictionServiceTest.assertMechanismPattern(
        catalog,
        ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION,
        MechanisticEventKind.MIGRATORY_INSERTION);
    OrganicReactionPredictionServiceTest.assertMechanismPattern(
        catalog,
        ReactionMechanismFamily.DIELS_ALDER,
        MechanisticEventKind.DIASTEREOSELECTIVE_APPROACH);
    OrganicReactionPredictionServiceTest.assertMechanismPattern(
        catalog,
        ReactionMechanismFamily.BECKMANN_REARRANGEMENT,
        MechanisticEventKind.BECKMANN_MIGRATION);
  }

  @Test
  void conditionSignalsInferDetailedModernMechanismFamiliesForScoring() {
    final OrganicReactionPredictionService service = new OrganicReactionPredictionService();
    final Molecule diazine =
        OrganicReactionPredictionServiceTest.ringMolecule("condition_inferred_diazine", "N");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "condition_inferred_modern_mechanisms",
            ReactionSide.of(
                List.of(ReactionParticipant.of(diazine, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(5.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(2.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.named(ReactionComponentRole.ACID, "HCl"),
                    ReactionConditionComponent.named(ReactionComponentRole.BASE, "NaOH"),
                    ReactionConditionComponent.named(ReactionComponentRole.CATALYST, "Pd(OAc)2"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "sodium nitrite"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "phenyl lithium")),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.CATALYST_PRESENT),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.DEHYDRATING_AGENT),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.WORKUP),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.FLOW_REACTOR)),
                "condition-rich mechanism inference"),
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(
                OrganicReactionPredictionServiceTest.candidate(
                    diazine, 0.55, "condition inferred")),
            null);

    final ReactionPredictionResult result = service.predict(request);

    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "Aromatic diazotization"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "Sandmeyer substitution"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "Grignard or organolithium addition"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "Esterification"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "Hydrolysis"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "C-H activation"));
    Assertions.assertTrue(result.bestCandidate().hasScoreBreakdown());
  }

  @Test
  void conditionSignalsInferNamedOrganicMechanismsForScoring() {
    final OrganicReactionPredictionService service = new OrganicReactionPredictionService();
    final Molecule arene =
        OrganicReactionPredictionServiceTest.ringMolecule("condition_inferred_named_arene", "C");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "condition_inferred_named_mechanisms",
            ReactionSide.of(List.of(ReactionParticipant.of(arene, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(70.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(5.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.named(ReactionComponentRole.CATALYST, "Pd(PPh3)4"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.CATALYST, "AlCl3 Lewis acid"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "boronic acid"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "terminal alkyne CuI"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "mCPBA peracid"),
                    ReactionConditionComponent.named(ReactionComponentRole.ADDITIVE, "ozone O3"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "nitric acid HNO3"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "diene dienophile"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "oxime Beckmann"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "allyl vinyl ether Claisen"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "formaldehyde iminium Mannich"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "cyanide Strecker"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "isocyanide Ugi Passerini"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "phosphonium ylide Wittig"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "phosphonate Horner Wadsworth Emmons"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "Mitsunobu DEAD triphenylphosphine"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "DMSO oxalyl chloride Swern"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "Dess-Martin hypervalent iodine"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "osmium tetroxide dihydroxylation"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "Sharpless tartrate titanium isopropoxide"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "Pauson Khand carbon monoxide"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "Nazarov divinyl ketone"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "phenylhydrazine Fischer indole"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "Skraup quinoline glycerol"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "Hantzsch Biginelli beta-ketoester urea")),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.CATALYST_PRESENT),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING)),
                "named mechanism inference"),
            List.of(
                OrganicReactionDomain.CROSS_COUPLING,
                OrganicReactionDomain.ELECTROPHILIC_SUBSTITUTION,
                OrganicReactionDomain.CYCLIZATION),
            List.of(OrganicReactionPredictionServiceTest.candidate(arene, 0.60, "named inferred")),
            null);

    final ReactionPredictionResult result = service.predict(request);

    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "Suzuki-Miyaura coupling"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "Sonogashira coupling"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "Baeyer-Villiger oxidation"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "Ozonolysis"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "Aromatic nitration"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "Friedel-Crafts"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "Diels-Alder"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "Beckmann rearrangement"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "Claisen rearrangement"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(result, "Mannich"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "Strecker"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(result, "Ugi"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "Passerini"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(result, "Wittig"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "Horner-Wadsworth-Emmons"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "Mitsunobu"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(result, "Swern"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "Dess-Martin"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "dihydroxylation"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "Sharpless"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "Pauson-Khand"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(result, "Nazarov"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "Fischer indole"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(result, "Skraup"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "Hantzsch"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "Biginelli"));
    Assertions.assertTrue(
        result.bestCandidate().evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.COMPETING_PATHWAY
                        && evidence.weight() < 0.0
                        && evidence.description().contains("Multiple named mechanism families")));
  }

  @Test
  void conditionSignalsInferProtectionCondensationReductionAndCatalysisMechanisms() {
    final OrganicReactionPredictionService service = new OrganicReactionPredictionService();
    final Molecule alkene =
        OrganicReactionPredictionServiceTest.ringMolecule("expanded_named_reaction_probe", "C");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "expanded_named_reaction_inference",
            ReactionSide.of(List.of(ReactionParticipant.of(alkene, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(45.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(2.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "ethylene glycol acetal"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "epoxide oxirane"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "halohydrin NBS water"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "HBr hydrohalogenation"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "oxymercuration hydration"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "Claisen condensation beta-keto ester"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "Dieckmann intramolecular Claisen"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "Knoevenagel active methylene"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "Henry reaction nitroaldol"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "Cannizzaro non-enolizable aldehyde"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "Birch sodium ammonia"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "Jones CrO3 chromium VI"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "PCC pyridinium chlorochromate"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "NaBH4 LiAlH4 hydride reduction"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "Curtius acyl azide"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "Hofmann rearrangement hypobromite amide"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "aryne benzyne sodium amide aryl halide"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "carbene cyclopropanation Simmons Smith"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "nitrene insertion iminoiodinane"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "hydroformylation syngas CO H2 rhodium"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "Grubbs olefin metathesis Ru carbene"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "thiol-ene photoinitiator thiol")),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.CATALYST_PRESENT),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING)),
                "expanded named mechanism inference"),
            List.of(
                OrganicReactionDomain.ADDITION,
                OrganicReactionDomain.CYCLIZATION,
                OrganicReactionDomain.OXIDATION_REDUCTION,
                OrganicReactionDomain.PROTECTION_DEPROTECTION,
                OrganicReactionDomain.REARRANGEMENT),
            List.of(
                OrganicReactionPredictionServiceTest.candidate(
                    alkene, 0.51, "expanded named inferred")),
            null);

    final ReactionPredictionResult result = service.predict(request);

    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(result, "Acetal"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(result, "Epoxide"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "Halohydrin"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "Hydrohalogenation"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "Hydration"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "Claisen condensation"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "Dieckmann"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "Knoevenagel"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(result, "Henry"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "Cannizzaro"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(result, "Birch"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(result, "Jones"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(result, "PCC"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(result, "Hydride"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(result, "Curtius"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(result, "Hofmann"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(result, "Aryne"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(result, "Carbene"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(result, "Nitrene"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "Hydroformylation"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "metathesis"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBestCandidateEvidenceContaining(
            result, "Thiol-ene"));
    Assertions.assertTrue(
        result.bestCandidate().evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.COMPETING_PATHWAY
                        && evidence.weight() < 0.0));
  }

  @Test
  void explainerReportsConditionSuitabilityForRichAndContradictoryConditions() {
    final OrganicReactionPredictionService service = new OrganicReactionPredictionService();
    final ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction
            .ReactionPredictionExplainer
        explainer =
            new ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction
                .ReactionPredictionExplainer();
    final Molecule pyridine =
        OrganicReactionPredictionServiceTest.ringMolecule("suitability_pyridine", "N");
    final ReactionPredictionRequest richRequest =
        ReactionPredictionRequest.of(
            "rich_condition_suitability",
            ReactionSide.of(
                List.of(ReactionParticipant.of(pyridine, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(95.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(8.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.named(ReactionComponentRole.SOLVENT, "DMF"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.CATALYST, "Pd catalyst")),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.CATALYST_PRESENT),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS)),
                "rich suitability"),
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(
                OrganicReactionPredictionServiceTest.candidate(
                    pyridine, 0.55, "rich suitability candidate")),
            null);
    final ReactionPredictionRequest contradictoryRequest =
        ReactionPredictionRequest.of(
            "contradictory_condition_suitability",
            richRequest.reactants(),
            ReactionConditions.of(
                Temperature.of(20.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(15.0, TimeDurationUnit.MINUTE),
                List.of(),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.COOLING),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.AIR_SENSITIVE)),
                "contradictory suitability"),
            richRequest.domains(),
            richRequest.seedCandidates(),
            null);

    final ReactionPredictionResult richResult = service.predict(richRequest);
    final ReactionPredictionResult contradictoryResult = service.predict(contradictoryRequest);

    Assertions.assertTrue(
        explainer.explain(richResult).conditionSuitability().explicitConditions());
    Assertions.assertFalse(
        explainer.explain(richResult).conditionSuitability().stronglyIncompatible());
    Assertions.assertTrue(explainer.explain(richResult).conditionSuitability().score() > 0.45);
    Assertions.assertFalse(
        explainer.explain(richResult).conditionSuitability().supportingEvidence().isEmpty());
    Assertions.assertTrue(
        explainer.explain(contradictoryResult).conditionSuitability().stronglyIncompatible());
    Assertions.assertTrue(
        explainer.explain(contradictoryResult).conditionSuitability().score() < 0.45);
    Assertions.assertFalse(
        explainer.explain(contradictoryResult).conditionSuitability().limitingEvidence().isEmpty());
  }

  @Test
  void conditionMatcherExplainsMatchedMissingAndContradictoryReactionFeatures() {
    final OrganicReactionConditionMatcher matcher = new OrganicReactionConditionMatcher();
    final ReactionConditions conditions =
        ReactionConditions.of(
            Temperature.of(20.0, TemperatureUnit.CELSIUS),
            null,
            TimeDuration.of(30.0, TimeDurationUnit.MINUTE),
            List.of(),
            List.of(
                ReactionConditionFeature.named(ReactionConditionFeatureKind.COOLING),
                ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                ReactionConditionFeature.named(ReactionConditionFeatureKind.AIR_SENSITIVE)),
            "contradictory rule-condition probe");

    final OrganicReactionConditionAssessment assessment =
        matcher.assess(
            conditions,
            List.of(
                ReactionConditionFeatureKind.COOLING,
                ReactionConditionFeatureKind.INERT_ATMOSPHERE,
                ReactionConditionFeatureKind.ANHYDROUS));

    Assertions.assertTrue(assessment.stronglyIncompatible());
    Assertions.assertTrue(assessment.scoreAdjustment() < 0.0);
    Assertions.assertTrue(
        assessment.evidence().stream()
            .anyMatch(evidence -> evidence.description().contains("COOLING")));
    Assertions.assertTrue(
        assessment.evidence().stream()
            .anyMatch(evidence -> evidence.description().contains("INERT_ATMOSPHERE")));
    Assertions.assertTrue(
        assessment.evidence().stream()
            .anyMatch(evidence -> evidence.description().contains("Cooling and heating")));
    Assertions.assertTrue(
        assessment.evidence().stream()
            .anyMatch(evidence -> evidence.description().contains("lacks inert atmosphere")));
  }

  @Test
  void predictionServiceGeneratesCandidatesFromRuleLibraryWhenSeedCandidatesAreAbsent() {
    final Molecule pyridine = OrganicReactionPredictionServiceTest.ringMolecule("pyridine", "N");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "heterocycle_rule_generation",
            ReactionSide.of(
                List.of(ReactionParticipant.of(pyridine, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                null,
                null,
                TimeDuration.of(2.0, TimeDurationUnit.HOUR),
                List.of(),
                List.of(ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING)),
                "rule generation"),
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(),
            null);
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                OrganicReactionRuleLibrary.of(List.of(new FixedProductRule(pyridine)))));

    final ReactionPredictionResult result = service.predict(request);

    Assertions.assertTrue(result.hasCandidates());
    Assertions.assertEquals(
        pyridine, result.bestCandidate().products().participants().get(0).molecule());
    Assertions.assertTrue(
        result.diagnostics().stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.USER_RULE
                        && evidence.description().contains("Rule matched")));
    Assertions.assertTrue(
        result.bestCandidate().evidence().stream()
            .anyMatch(
                evidence -> evidence.kind() == ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN));
  }

  @Test
  void builtInTemplateLibraryIsConvertedToAdvisoryRuleLibrary() {
    final OrganicReactionRuleLibrary rules =
        BuiltInOrganicReactionRuleLibraries.fromTemplates(
            BuiltInOrganicReactionTemplateLibraries.heterocyclePlanning());

    Assertions.assertFalse(rules.empty());
    Assertions.assertEquals(
        BuiltInOrganicReactionTemplateLibraries.heterocyclePlanning().size(), rules.size());
  }

  @Test
  void heteroarylHydroxylationWithoutOxygenSourceRemainsDiagnostic() {
    final Molecule reactant =
        OrganicReactionPredictionServiceTest.ringMolecule("pyridine_product_source", "N");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "heterocycle_product_transform",
            ReactionSide.of(
                List.of(ReactionParticipant.of(reactant, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(90.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(8.0, TimeDurationUnit.HOUR),
                List.of(),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.CATALYST_PRESENT)),
                "heterocycle product transform"),
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(),
            null);
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));

    final ReactionPredictionResult result = service.predict(request);

    assertDiagnosticOnly(result, "transform_heteroaryl_hydroxylation");
    Assertions.assertFalse(result.hasCandidates());
    Assertions.assertEquals(6, reactant.atomCount());
    Assertions.assertEquals(6, reactant.bondCount());
  }

  @Test
  void builtInAmineReplacementGeneratesAllActivatedHeteroarylChlorideSites() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final Molecule scaffold = OrganicReactionPredictionServiceTest.dichloroHeterocycle();
    final Molecule amine = OrganicReactionPredictionServiceTest.primaryAmine("benzylamine_probe");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "dichloro_heterocycle_amine_site_expansion",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(scaffold, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(amine, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(120.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(3.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.named(ReactionComponentRole.BASE, "amine excess")),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING)),
                "Dichloro heterocycle amination site expansion"),
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            List.of(),
            null);

    final ReactionPredictionResult result = service.predict(request);
    final long snarCandidateCount =
        result.candidates().stream()
            .filter(
                candidate ->
                    OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                        candidate, "Generated SNAr site combination"))
            .count();

    Assertions.assertTrue(snarCandidateCount >= 2L);
    Assertions.assertTrue(
        result.candidates().stream()
            .anyMatch(
                candidate ->
                    OrganicReactionPredictionServiceTest.hasEvidenceContaining(candidate, "a2")));
    Assertions.assertTrue(
        result.candidates().stream()
            .anyMatch(
                candidate ->
                    OrganicReactionPredictionServiceTest.hasEvidenceContaining(candidate, "a4")));
  }

  @Test
  void builtInAmineReplacementGeneratesGraphProductsForHalogenSeries() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final List<String> halogens = List.of("F", "Cl", "Br", "I");
    for (int i = 0; i < halogens.size(); ++i) {
      final String halogen = halogens.get(i);
      final Molecule scaffold =
          OrganicReactionPredictionServiceTest.halogenatedNitrogenHeterocycle(
              "halogen_series_" + halogen.toLowerCase() + "_heterocycle", halogen);
      final Molecule amine =
          OrganicReactionPredictionServiceTest.primaryAmine("amine_for_" + halogen.toLowerCase());
      final ReactionPredictionResult result =
          service.predict(
              ReactionPredictionRequest.of(
                  "halogen_series_amine_fragment_" + halogen.toLowerCase(),
                  ReactionSide.of(
                      List.of(
                          ReactionParticipant.of(scaffold, StoichiometricCoefficient.ONE),
                          ReactionParticipant.of(amine, StoichiometricCoefficient.ONE))),
                  ReactionConditions.of(
                      Temperature.of(120.0, TemperatureUnit.CELSIUS),
                      null,
                      TimeDuration.of(3.0, TimeDurationUnit.HOUR),
                      List.of(
                          ReactionConditionComponent.named(
                              ReactionComponentRole.BASE, "carbonate base")),
                      List.of(
                          ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM),
                          ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                          ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS)),
                      "halogen series graph-derived amination"),
                  List.of(
                      OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                      OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
                  List.of(),
                  null));

      Assertions.assertTrue(result.hasCandidates(), halogen);
      Assertions.assertTrue(
          result.candidates().stream()
              .flatMap(candidate -> candidate.evidence().stream())
              .noneMatch(
                  evidence -> evidence.kind() == ReactionPredictionEvidenceKind.CALIBRATION_PRIOR),
          "Built-in prediction rules must not import benchmark/calibration observations into prediction path.");
      Assertions.assertTrue(
          result.candidates().stream()
              .anyMatch(
                  candidate ->
                      OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                              candidate, "Leaving-group adjustment=")
                          && candidate.products().participants().stream()
                              .anyMatch(
                                  participant ->
                                      participant
                                          .molecule()
                                          .id()
                                          .value()
                                          .contains("_leaving_h" + halogen.toLowerCase()))
                          && candidate.products().participants().stream()
                              .anyMatch(
                                  participant ->
                                      participant
                                          .molecule()
                                          .id()
                                          .value()
                                          .contains("_amine_coupled"))),
          result.candidates().toString());
    }
  }

  @Test
  void builtInAnrorcAmineChannelTransfersCompleteAmineFragmentWithoutLookup() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final Molecule scaffold =
        OrganicReactionPredictionServiceTest.halogenatedNitrogenHeterocycle(
            "anrorc_graph_chloro_heterocycle", "Cl");
    final Molecule amine =
        OrganicReactionPredictionServiceTest.primaryAmine("anrorc_graph_benzylamine");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "anrorc_amine_fragment_transfer_probe",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(scaffold, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(amine, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(120.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(3.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.named(ReactionComponentRole.BASE, "amine excess")),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS)),
                "heated basic aza-heterocycle amination with possible ANRORC competition"),
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION,
                OrganicReactionDomain.REARRANGEMENT),
            List.of(),
            null);

    final ReactionPredictionResult result = service.predict(request);

    Assertions.assertTrue(result.hasCandidates());
    Assertions.assertTrue(
        result.candidates().stream()
            .anyMatch(
                candidate ->
                    candidate.predictedMechanisms().stream()
                            .anyMatch(
                                mechanism ->
                                    mechanism.family()
                                        == ReactionMechanismFamily
                                            .ADDITION_RING_OPENING_RING_CLOSURE)
                        && OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                            candidate, "Generated ANRORC site combination")
                        && candidate.products().participants().stream()
                            .anyMatch(
                                participant ->
                                    participant.molecule().id().value().contains("_amine_coupled")
                                        && participant.molecule().atomCount()
                                            > scaffold.atomCount())),
        result.candidates().toString());
    Assertions.assertTrue(
        result.candidates().stream()
            .flatMap(candidate -> candidate.evidence().stream())
            .noneMatch(
                evidence -> evidence.kind() == ReactionPredictionEvidenceKind.CALIBRATION_PRIOR),
        "ANRORC amine-fragment product construction must not use benchmark product lookup.");
  }

  @Test
  void builtInCrossCouplingTransfersCarbonFragmentFromReactantGraph() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final Molecule scaffold =
        OrganicReactionPredictionServiceTest.halogenatedNitrogenHeterocycle(
            "graph_coupling_bromo_heterocycle", "Br");
    final Molecule fragment =
        OrganicReactionPredictionServiceTest.alkylHalideFragment(
            "graph_coupling_ethyl_bromide", "Br");

    final ReactionPredictionResult result =
        service.predict(
            ReactionPredictionRequest.of(
                "graph_fragment_cross_coupling",
                ReactionSide.of(
                    List.of(
                        ReactionParticipant.of(scaffold, StoichiometricCoefficient.ONE),
                        ReactionParticipant.of(fragment, StoichiometricCoefficient.ONE))),
                ReactionConditions.of(
                    Temperature.of(90.0, TemperatureUnit.CELSIUS),
                    null,
                    TimeDuration.of(4.0, TimeDurationUnit.HOUR),
                    List.of(
                        ReactionConditionComponent.named(
                            ReactionComponentRole.CATALYST, "palladium catalyst"),
                        ReactionConditionComponent.named(
                            ReactionComponentRole.BASE, "carbonate base")),
                    List.of(
                        ReactionConditionFeature.named(
                            ReactionConditionFeatureKind.CATALYST_PRESENT),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM),
                        ReactionConditionFeature.named(
                            ReactionConditionFeatureKind.INERT_ATMOSPHERE),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING)),
                    "graph-derived cross-coupling validation"),
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                    OrganicReactionDomain.CROSS_COUPLING),
                List.of(),
                null));

    Assertions.assertTrue(result.hasCandidates());
    Assertions.assertTrue(
        result.candidates().stream()
            .anyMatch(
                candidate -> {
                  final Molecule product = candidate.products().participants().get(0).molecule();
                  return OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                          candidate, "graph fragment transfer")
                      && OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                          candidate, "not from a benchmark outcome lookup")
                      && OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                          candidate, "explicit molecular graph changes")
                      && OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                          candidate,
                          "Candidate consumes an activated carbon-halogen leaving group.")
                      && product.id().value().contains("carbon_fragment_coupled")
                      && product.atoms().size() == 8
                      && product.bonds().size() == 8;
                }),
        OrganicReactionPredictionServiceTest.candidateSummary(result));
    Assertions.assertFalse(
        result.candidates().stream()
            .anyMatch(
                candidate ->
                    OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                        candidate, "CALIBRATION_PRIOR")));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasEvidenceContaining(
            result.bestCandidate(), "graph fragment transfer"));
  }

  @Test
  void builtInAmineReplacementDoesNotUseOtherScaffoldsAsAmineSources() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final Molecule firstScaffold = OrganicReactionPredictionServiceTest.dichloroHeterocycle();
    final Molecule secondScaffold =
        OrganicReactionPredictionServiceTest.chlorinatedAminoNitrogenHeterocycle(
            "second_scaffold_not_amine");
    final Molecule amine = OrganicReactionPredictionServiceTest.primaryAmine("benzylamine_probe");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "multi_scaffold_amine_site_filtering",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(firstScaffold, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(secondScaffold, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(amine, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(120.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(3.0, TimeDurationUnit.HOUR),
                List.of(ReactionConditionComponent.named(ReactionComponentRole.BASE, "K2CO3")),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING)),
                "multi-scaffold amine source filtering"),
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            List.of(),
            null);

    final ReactionPredictionResult result = service.predict(request);
    final List<ReactionProductCandidate> snarCandidates =
        result.candidates().stream()
            .filter(
                candidate ->
                    OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                        candidate, "Generated SNAr site combination"))
            .toList();

    Assertions.assertTrue(result.hasCandidates());
    Assertions.assertTrue(snarCandidates.size() >= 2, result.candidates().toString());
    Assertions.assertTrue(
        snarCandidates.stream()
            .allMatch(
                candidate ->
                    candidate.products().participants().stream()
                        .noneMatch(
                            participant ->
                                participant
                                    .molecule()
                                    .id()
                                    .value()
                                    .contains("second_scaffold_not_amine_nh_amine_coupled"))),
        result.candidates().toString());
  }

  @Test
  void conditionCompatibilityUsesReactantAmineGraphAndBaseIdentityWithoutBenchmarkLookup() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final Molecule scaffold = OrganicReactionPredictionServiceTest.dichloroHeterocycle();
    final Molecule amine =
        OrganicReactionPredictionServiceTest.primaryAmine("benzylamine_graph_source");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "structure_and_condition_driven_amination",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(scaffold, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(amine, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(120.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(3.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.named(ReactionComponentRole.BASE, "K2CO3"),
                    ReactionConditionComponent.named(ReactionComponentRole.SOLVENT, "PEG4000")),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING)),
                "Graph-derived amine nucleophile with carbonate base"),
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            List.of(),
            null);

    final ReactionPredictionResult result = service.predict(request);
    final ReactionProductCandidate best = result.bestCandidate();

    Assertions.assertTrue(result.hasCandidates());
    Assertions.assertTrue(
        best.evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.description().contains("amine-like nitrogen nucleophile source")),
        best.evidence().toString());
    Assertions.assertTrue(
        best.evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.description().contains("Inorganic carbonate/hydroxide base supplies")),
        best.evidence().toString());
    Assertions.assertTrue(
        best.evidence().stream()
            .noneMatch(
                evidence -> evidence.kind() == ReactionPredictionEvidenceKind.CALIBRATION_PRIOR),
        best.evidence().toString());
  }

  @Test
  void builtInReplacementRulesGenerateCandidatesForEveryMatchingReactantInMixture() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final Molecule first =
        OrganicReactionPredictionServiceTest.halogenatedNitrogenHeterocycle(
            "first_chloro_heterocycle", "Cl");
    final Molecule second =
        OrganicReactionPredictionServiceTest.halogenatedNitrogenHeterocycle(
            "second_chloro_heterocycle", "Cl");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "multi_substrate_replacement_generation",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(first, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(second, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(90.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(2.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.named(
                        ReactionComponentRole.CATALYST, "palladium on carbon")),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.CATALYST_PRESENT),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.WORKUP)),
                "multi-substrate hydrodehalogenation screen"),
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION,
                OrganicReactionDomain.CROSS_COUPLING),
            List.of(),
            null);

    final ReactionPredictionResult result = service.predict(request);

    Assertions.assertTrue(
        result.candidates().stream()
            .anyMatch(
                candidate ->
                    candidate.products().participants().stream()
                        .anyMatch(
                            participant ->
                                participant
                                    .molecule()
                                    .id()
                                    .value()
                                    .contains("first_chloro_heterocycle"))),
        result.candidates().toString());
    Assertions.assertTrue(
        result.candidates().stream()
            .anyMatch(
                candidate ->
                    candidate.products().participants().stream()
                        .anyMatch(
                            participant ->
                                participant
                                    .molecule()
                                    .id()
                                    .value()
                                    .contains("second_chloro_heterocycle"))),
        result.candidates().toString());
  }

  @Test
  void borylationBrominationAndSulfurOxidationNeedExplicitReagents() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final Molecule pyridine =
        OrganicReactionPredictionServiceTest.ringMolecule("branch_pyridine", "N");
    final Molecule sulfurHeterocycle =
        OrganicReactionPredictionServiceTest.ringMolecule("branch_sulfur_heterocycle", "S");

    final ReactionPredictionResult borylation =
        service.predict(
            OrganicReactionPredictionServiceTest.request(
                "branch_borylation",
                pyridine,
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                    OrganicReactionDomain.CROSS_COUPLING),
                List.of(
                    ReactionConditionFeatureKind.CATALYST_PRESENT,
                    ReactionConditionFeatureKind.INERT_ATMOSPHERE,
                    ReactionConditionFeatureKind.ANHYDROUS,
                    ReactionConditionFeatureKind.HEATING)));
    final ReactionPredictionResult bromination =
        service.predict(
            OrganicReactionPredictionServiceTest.request(
                "branch_bromination",
                pyridine,
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                    OrganicReactionDomain.ELECTROPHILIC_SUBSTITUTION),
                List.of(
                    ReactionConditionFeatureKind.COOLING,
                    ReactionConditionFeatureKind.ADDITION_RATE,
                    ReactionConditionFeatureKind.ACIDIC_MEDIUM)));
    final ReactionPredictionResult sulfurOxidation =
        service.predict(
            OrganicReactionPredictionServiceTest.request(
                "branch_sulfur_oxidation",
                sulfurHeterocycle,
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                    OrganicReactionDomain.OXIDATION_REDUCTION),
                List.of(
                    ReactionConditionFeatureKind.COOLING,
                    ReactionConditionFeatureKind.ADDITION_RATE)));

    assertDiagnosticOnly(borylation, "transform_heteroaryl_borylation");
    assertDiagnosticOnly(bromination, "transform_heteroaryl_bromination");
    assertDiagnosticOnly(sulfurOxidation, "transform_sulfur_heterocycle_oxidation");
    Assertions.assertEquals(0, atomCount(pyridine, "B"));
    Assertions.assertEquals(0, atomCount(pyridine, "Br"));
    Assertions.assertEquals(0, atomCount(sulfurHeterocycle, "O"));
  }

  @Test
  void nitrogenAndHalogenReplacementScreensRequireExplicitPartners() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final Molecule pyridine =
        OrganicReactionPredictionServiceTest.ringMolecule("n_branch_pyridine", "N");
    final Molecule bromoHeterocycle =
        OrganicReactionPredictionServiceTest.halogenatedNitrogenHeterocycle(
            "bromo_heterocycle", "Br");
    final Molecule fluoroHeterocycle =
        OrganicReactionPredictionServiceTest.halogenatedNitrogenHeterocycle(
            "fluoro_heterocycle", "F");

    final ReactionPredictionResult nOxidation =
        service.predict(
            OrganicReactionPredictionServiceTest.request(
                "branch_n_oxidation",
                pyridine,
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                    OrganicReactionDomain.OXIDATION_REDUCTION),
                List.of(
                    ReactionConditionFeatureKind.COOLING,
                    ReactionConditionFeatureKind.ADDITION_RATE,
                    ReactionConditionFeatureKind.ANHYDROUS)));
    final ReactionPredictionResult nAlkylation =
        service.predict(
            OrganicReactionPredictionServiceTest.request(
                "branch_n_alkylation",
                pyridine,
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                    OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
                List.of(
                    ReactionConditionFeatureKind.BASIC_MEDIUM,
                    ReactionConditionFeatureKind.HEATING,
                    ReactionConditionFeatureKind.ADDITION_RATE)));
    final ReactionPredictionResult suzukiReplacement =
        service.predict(
            OrganicReactionPredictionServiceTest.request(
                "branch_suzuki_replacement",
                bromoHeterocycle,
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                    OrganicReactionDomain.CROSS_COUPLING),
                List.of(
                    ReactionConditionFeatureKind.CATALYST_PRESENT,
                    ReactionConditionFeatureKind.INERT_ATMOSPHERE,
                    ReactionConditionFeatureKind.ANHYDROUS,
                    ReactionConditionFeatureKind.HEATING)));
    final ReactionPredictionResult alkoxyReplacement =
        service.predict(
            OrganicReactionPredictionServiceTest.request(
                "branch_alkoxy_replacement",
                fluoroHeterocycle,
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                    OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
                List.of(
                    ReactionConditionFeatureKind.BASIC_MEDIUM,
                    ReactionConditionFeatureKind.HEATING,
                    ReactionConditionFeatureKind.ANHYDROUS)));

    assertDiagnosticOnly(nOxidation, "transform_nitrogen_heterocycle_n_oxide");
    assertDiagnosticOnly(nAlkylation, "transform_nitrogen_heterocycle_n_alkylation");
    assertDiagnosticOnly(suzukiReplacement, "replace_heteroaryl_bromide_suzuki_carbon");
    assertDiagnosticOnly(alkoxyReplacement, "replace_heteroaryl_fluoride_alkoxy_snar");
    Assertions.assertEquals(1, atomCount(bromoHeterocycle, "Br"));
    Assertions.assertEquals(1, atomCount(fluoroHeterocycle, "F"));
  }

  @Test
  void heteroarylSideChannelsWithoutPartnersAreNotProductGraphs() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final Molecule bromoHeterocycle =
        OrganicReactionPredictionServiceTest.halogenatedNitrogenHeterocycle(
            "side_channel_bromo_heterocycle", "Br");
    final Molecule iodoHeterocycle =
        OrganicReactionPredictionServiceTest.halogenatedNitrogenHeterocycle(
            "side_channel_iodo_heterocycle", "I");
    final Molecule chloroHeterocycle =
        OrganicReactionPredictionServiceTest.halogenatedNitrogenHeterocycle(
            "side_channel_chloro_heterocycle", "Cl");
    final Molecule borylHeterocycle =
        OrganicReactionPredictionServiceTest.halogenatedNitrogenHeterocycle(
            "side_channel_boryl_heterocycle", "B");
    final Molecule nitrileHeterocycle =
        OrganicReactionPredictionServiceTest.nitrileNitrogenHeterocycle();

    final ReactionPredictionResult couplingScreen =
        service.predict(
            OrganicReactionPredictionServiceTest.request(
                "side_channel_cross_coupling_screen",
                bromoHeterocycle,
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                    OrganicReactionDomain.CROSS_COUPLING),
                List.of(
                    ReactionConditionFeatureKind.CATALYST_PRESENT,
                    ReactionConditionFeatureKind.INERT_ATMOSPHERE,
                    ReactionConditionFeatureKind.ANHYDROUS,
                    ReactionConditionFeatureKind.HEATING)));
    final ReactionPredictionResult solvolysisScreen =
        service.predict(
            OrganicReactionPredictionServiceTest.request(
                "side_channel_solvolysis_screen",
                chloroHeterocycle,
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                    OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
                List.of(
                    ReactionConditionFeatureKind.HEATING, ReactionConditionFeatureKind.WORKUP)));
    final ReactionPredictionResult iodoCouplingScreen =
        service.predict(
            OrganicReactionPredictionServiceTest.request(
                "side_channel_iodo_cross_coupling_screen",
                iodoHeterocycle,
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                    OrganicReactionDomain.CROSS_COUPLING),
                List.of(
                    ReactionConditionFeatureKind.CATALYST_PRESENT,
                    ReactionConditionFeatureKind.INERT_ATMOSPHERE,
                    ReactionConditionFeatureKind.ANHYDROUS,
                    ReactionConditionFeatureKind.HEATING)));
    final ReactionPredictionResult protodeboronationScreen =
        service.predict(
            OrganicReactionPredictionServiceTest.request(
                "side_channel_protodeboronation_screen",
                borylHeterocycle,
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                    OrganicReactionDomain.CROSS_COUPLING),
                List.of(
                    ReactionConditionFeatureKind.BASIC_MEDIUM,
                    ReactionConditionFeatureKind.HEATING,
                    ReactionConditionFeatureKind.WORKUP)));
    final ReactionPredictionResult nitrileWorkupScreen =
        service.predict(
            OrganicReactionPredictionServiceTest.request(
                "side_channel_nitrile_workup_screen",
                nitrileHeterocycle,
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                    OrganicReactionDomain.ADDITION,
                    OrganicReactionDomain.OXIDATION_REDUCTION),
                List.of(
                    ReactionConditionFeatureKind.ACIDIC_MEDIUM,
                    ReactionConditionFeatureKind.CATALYST_PRESENT,
                    ReactionConditionFeatureKind.HEATING,
                    ReactionConditionFeatureKind.WORKUP)));

    assertDiagnosticOnly(couplingScreen, "replace_heteroaryl_bromide_suzuki_carbon");
    assertDiagnosticOnly(couplingScreen, "side_heteroaryl_hydrodehalogenation");
    assertDiagnosticOnly(iodoCouplingScreen, "replace_heteroaryl_iodide_suzuki_carbon");
    assertDiagnosticOnly(solvolysisScreen, "side_heteroaryl_solvolysis_hydroxy_replacement");
    assertDiagnosticOnly(solvolysisScreen, "side_heteroaryl_chloride_hydrodehalogenation");
    assertDiagnosticOnly(protodeboronationScreen, "side_heteroaryl_protodeboronation");
    assertDiagnosticOnly(nitrileWorkupScreen, "side_heteroaryl_nitrile_hydration");
    assertDiagnosticOnly(nitrileWorkupScreen, "side_heteroaryl_nitrile_reduction");
    Assertions.assertEquals(1, atomCount(bromoHeterocycle, "Br"));
    Assertions.assertEquals(1, atomCount(iodoHeterocycle, "I"));
    Assertions.assertEquals(1, atomCount(chloroHeterocycle, "Cl"));
    Assertions.assertEquals(1, atomCount(borylHeterocycle, "B"));
    Assertions.assertEquals(2, atomCount(nitrileHeterocycle, "N"));
  }

  @Test
  void broadFunctionalGroupScreensDoNotInventReagentsOrRestoreSurrogates() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final Molecule rich = OrganicReactionPredictionServiceTest.functionalGroupRichMolecule();
    final Molecule radical = OrganicReactionPredictionServiceTest.functionalizedHeterocycle();

    final ReactionPredictionResult dryAddition =
        service.predict(
            OrganicReactionPredictionServiceTest.request(
                "broad_dry_addition",
                rich,
                List.of(OrganicReactionDomain.ADDITION, OrganicReactionDomain.OXIDATION_REDUCTION),
                List.of(
                    ReactionConditionFeatureKind.ANHYDROUS,
                    ReactionConditionFeatureKind.INERT_ATMOSPHERE,
                    ReactionConditionFeatureKind.COOLING,
                    ReactionConditionFeatureKind.ADDITION_RATE)));
    final ReactionPredictionResult basicSubstitution =
        service.predict(
            OrganicReactionPredictionServiceTest.request(
                "broad_basic_substitution",
                rich,
                List.of(
                    OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION,
                    OrganicReactionDomain.PROTECTION_DEPROTECTION),
                List.of(
                    ReactionConditionFeatureKind.BASIC_MEDIUM,
                    ReactionConditionFeatureKind.ANHYDROUS,
                    ReactionConditionFeatureKind.ADDITION_RATE)));
    final ReactionPredictionResult acidicWorkup =
        service.predict(
            OrganicReactionPredictionServiceTest.request(
                "broad_acidic_workup",
                rich,
                List.of(
                    OrganicReactionDomain.PROTECTION_DEPROTECTION,
                    OrganicReactionDomain.REARRANGEMENT,
                    OrganicReactionDomain.OXIDATION_REDUCTION),
                List.of(
                    ReactionConditionFeatureKind.ACIDIC_MEDIUM,
                    ReactionConditionFeatureKind.WORKUP,
                    ReactionConditionFeatureKind.COOLING)));
    final ReactionPredictionResult photochemicalRadical =
        service.predict(
            OrganicReactionPredictionServiceTest.request(
                "broad_radical",
                radical,
                List.of(OrganicReactionDomain.ADDITION),
                List.of(
                    ReactionConditionFeatureKind.PHOTOCHEMICAL,
                    ReactionConditionFeatureKind.INERT_ATMOSPHERE,
                    ReactionConditionFeatureKind.ADDITION_RATE)));

    assertDiagnosticOnly(dryAddition, "transform_carbonyl_organometallic_addition");
    assertDiagnosticOnly(basicSubstitution, "transform_acyl_substitution_aminolysis");
    assertDiagnosticOnly(basicSubstitution, "transform_alcohol_protection");
    assertDiagnosticOnly(acidicWorkup, "transform_ether_deprotection");
    assertDiagnosticOnly(photochemicalRadical, "transform_radical_carbon_functionalization");
    for (final String removed :
        List.of(
            "transform_alkene_epoxidation",
            "transform_alkene_hydroboration_oxidation",
            "transform_baeyer_villiger_oxygen_insertion")) {
      Assertions.assertTrue(
          BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction().rules().stream()
              .noneMatch(rule -> rule.id().value().equals(removed)),
          removed);
      assertNoCandidateEvidenceContaining(dryAddition, removed);
      assertNoCandidateEvidenceContaining(acidicWorkup, removed);
    }
  }

  @Test
  void builtInProductPredictionRulesCoverNamedCouplingAndPericyclicBranches() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final Molecule bromoHeterocycle =
        OrganicReactionPredictionServiceTest.halogenatedNitrogenHeterocycle(
            "named_coupling_bromo_heterocycle", "Br");
    final Molecule rich = OrganicReactionPredictionServiceTest.functionalGroupRichMolecule();
    final Molecule terminalAlkene =
        OrganicReactionPredictionServiceTest.terminalAlkeneWithHydrogen(
            "named_coupling_terminal_alkene");
    final Molecule terminalAlkyne =
        OrganicReactionPredictionServiceTest.terminalAlkyneWithHydrogen(
            "named_coupling_terminal_alkyne");
    final Molecule boronate =
        OrganicReactionPredictionServiceTest.organicBoronatePartner("named_coupling_boronate");
    final Molecule amine =
        OrganicReactionPredictionServiceTest.primaryAmine("named_coupling_amine");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "named_coupling_pericyclic_branches",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(bromoHeterocycle, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(rich, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(terminalAlkene, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(terminalAlkyne, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(boronate, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(amine, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(95.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(5.0, TimeDurationUnit.HOUR),
                List.of(),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.CATALYST_PRESENT),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.INERT_ATMOSPHERE),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.ADDITION_RATE)),
                "named coupling and pericyclic branch screening"),
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.CROSS_COUPLING,
                OrganicReactionDomain.ADDITION,
                OrganicReactionDomain.CYCLIZATION,
                OrganicReactionDomain.HETEROCYCLE_SYNTHESIS),
            List.of(),
            null);

    final ReactionPredictionResult result = service.predict(request);

    OrganicReactionPredictionServiceTest.assertGeneratedBranch(
        result, "transform_heteroaryl_heck_alkenylation", "C", bromoHeterocycle.atomCount() + 1);
    OrganicReactionPredictionServiceTest.assertGeneratedBranch(
        result,
        "fragment_transfer_heteroaryl_alkene_heck_coupling",
        "C",
        bromoHeterocycle.atomCount() + terminalAlkene.atomCount() - 2);
    OrganicReactionPredictionServiceTest.assertGeneratedBranch(
        result,
        "transform_heteroaryl_sonogashira_alkynylation",
        "C",
        bromoHeterocycle.atomCount() + 1);
    OrganicReactionPredictionServiceTest.assertGeneratedBranch(
        result,
        "fragment_transfer_heteroaryl_alkyne_sonogashira_coupling",
        "C",
        bromoHeterocycle.atomCount() + terminalAlkyne.atomCount() - 2);
    OrganicReactionPredictionServiceTest.assertGeneratedBranch(
        result,
        "transform_heteroaryl_buchwald_ullmann_amination",
        "N",
        bromoHeterocycle.atomCount() + 1);
    OrganicReactionPredictionServiceTest.assertGeneratedBranch(
        result,
        "fragment_transfer_heteroaryl_amine_buchwald_ullmann_coupling",
        "N",
        bromoHeterocycle.atomCount() + amine.atomCount() - 2);
    OrganicReactionPredictionServiceTest.assertGeneratedBranch(
        result,
        "fragment_transfer_heteroaryl_boronate_suzuki_coupling",
        "C",
        bromoHeterocycle.atomCount() + boronate.atomCount() - 4);
    Assertions.assertTrue(
        result.candidates().stream()
            .anyMatch(
                candidate -> {
                  if (!OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                      candidate, "fragment_transfer_heteroaryl_boronate_suzuki_coupling")) {
                    return false;
                  }
                  return OrganicReactionPredictionServiceTest.totalAtomCount(candidate.products())
                          == bromoHeterocycle.atomCount() + boronate.atomCount()
                      && candidate.products().participants().stream()
                          .map(ReactionParticipant::molecule)
                          .anyMatch(
                              coproduct ->
                                  coproduct.id().value().contains("_coproduct")
                                      && OrganicReactionPredictionServiceTest.atomCount(
                                              coproduct, "Br")
                                          == 1
                                      && OrganicReactionPredictionServiceTest.atomCount(
                                              coproduct, "B")
                                          == 1
                                      && OrganicReactionPredictionServiceTest.atomCount(
                                              coproduct, "O")
                                          == 2);
                }),
        OrganicReactionPredictionServiceTest.candidateSummary(result));
    OrganicReactionPredictionServiceTest.assertGeneratedBranch(
        result, "bridge_addition_diels_alder_cycloaddition", "C", rich.atomCount() + 1);
    OrganicReactionPredictionServiceTest.assertGeneratedBranch(
        result, "transform_aza_diels_alder_cycloaddition", "N", rich.atomCount() + 1);
  }

  @Test
  void heteroarylChlorideBenzylaminePegHeatingPredictsAminationMainChannel() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final Molecule scaffold = OrganicReactionPredictionServiceTest.chloroDimethylNicotinonitrile();
    final Molecule benzylamine =
        OrganicReactionPredictionServiceTest.benzylamine("benzylamine_reagent");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "chloro_dimethyl_nicotinonitrile_benzylamine_peg",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(scaffold, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(benzylamine, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(120.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(3.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.named(ReactionComponentRole.SOLVENT, "PEG4000"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.REACTANT, "benzylamine")),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS)),
                "PEG4000 heated benzylamine substitution screen"),
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            List.of(),
            null);

    final ReactionPredictionResult result = service.predict(request);

    Assertions.assertTrue(
        result.bestCandidate().evidence().stream()
            .anyMatch(
                evidence ->
                    evidence
                        .description()
                        .contains("replace_heteroaryl_halide_with_amine_fragment")),
        OrganicReactionPredictionServiceTest.candidateSummary(result));
    Assertions.assertTrue(
        result.bestCandidate().probability() > 0.04,
        OrganicReactionPredictionServiceTest.candidateSummary(result));
    Assertions.assertTrue(
        result.bestCandidate().probability()
            > OrganicReactionPredictionServiceTest.highestCandidateProbabilityWithoutEvidence(
                result, "replace_heteroaryl_halide_with_amine_fragment"),
        OrganicReactionPredictionServiceTest.candidateSummary(result));
    final Molecule product = result.bestCandidate().products().participants().get(0).molecule();
    Assertions.assertEquals(
        0L,
        OrganicReactionPredictionServiceTest.atomCount(product, "Cl"),
        OrganicReactionPredictionServiceTest.candidateSummary(result));
    Assertions.assertEquals(
        1L,
        OrganicReactionPredictionServiceTest.tripleBondCount(product, "C", "N"),
        OrganicReactionPredictionServiceTest.candidateSummary(result));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasBondToElement(product, "N", "C", BondType.SINGLE),
        OrganicReactionPredictionServiceTest.candidateSummary(result));
    OrganicReactionPredictionServiceTest.assertNoCandidateEvidenceKind(
        result, ReactionPredictionEvidenceKind.SURROGATE_PRODUCT);
  }

  @Test
  void builtInNitroReductionGeneratesGraphAmineProductWithoutSurrogateLookup() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    // A complete C-nitro substrate and an explicit hydrogen reagent are required.
    final Molecule rich =
        Molecule.of(
            MoleculeId.of("nitromethane"),
            "Nitromethane",
            List.of(
                Atom.of(AtomId.of("c"), ElementSymbol.of("C"), null),
                Atom.of(
                    AtomId.of("a9"),
                    ElementSymbol.of("N"),
                    null,
                    FormalCharge.of(1),
                    null,
                    RadicalState.CLOSED_SHELL,
                    ChemistryMetadata.EMPTY),
                Atom.of(
                    AtomId.of("o1"),
                    ElementSymbol.of("O"),
                    null,
                    FormalCharge.of(-1),
                    null,
                    RadicalState.CLOSED_SHELL,
                    ChemistryMetadata.EMPTY),
                Atom.of(AtomId.of("o2"), ElementSymbol.of("O"), null),
                Atom.of(AtomId.of("h1"), ElementSymbol.of("H"), null),
                Atom.of(AtomId.of("h2"), ElementSymbol.of("H"), null),
                Atom.of(AtomId.of("h3"), ElementSymbol.of("H"), null)),
            List.of(
                Bond.of(AtomId.of("c"), AtomId.of("a9"), BondType.SINGLE),
                Bond.of(AtomId.of("a9"), AtomId.of("o1"), BondType.SINGLE),
                Bond.of(AtomId.of("a9"), AtomId.of("o2"), BondType.DOUBLE),
                Bond.of(AtomId.of("c"), AtomId.of("h1"), BondType.SINGLE),
                Bond.of(AtomId.of("c"), AtomId.of("h2"), BondType.SINGLE),
                Bond.of(AtomId.of("c"), AtomId.of("h3"), BondType.SINGLE)),
            MolecularCharge.NEUTRAL,
            SpinMultiplicity.SINGLET);
    final Molecule hydrogen =
        Molecule.of(
            MoleculeId.of("hydrogen"),
            "Hydrogen",
            List.of(
                Atom.of(AtomId.of("h1"), ElementSymbol.of("H"), null),
                Atom.of(AtomId.of("h2"), ElementSymbol.of("H"), null)),
            List.of(Bond.of(AtomId.of("h1"), AtomId.of("h2"), BondType.SINGLE)),
            MolecularCharge.NEUTRAL,
            SpinMultiplicity.SINGLET);
    final ReactionPredictionRequest withoutReagent =
        OrganicReactionPredictionServiceTest.request(
            "graph_nitro_reduction",
            rich,
            List.of(OrganicReactionDomain.OXIDATION_REDUCTION),
            List.of(
                ReactionConditionFeatureKind.REDUCING_MEDIUM,
                ReactionConditionFeatureKind.HYDROGENATION,
                ReactionConditionFeatureKind.CATALYST_PRESENT));
    Assertions.assertTrue(
        service.predict(withoutReagent).candidates().stream()
            .noneMatch(
                candidate ->
                    candidate.products().participants().stream()
                        .anyMatch(
                            participant ->
                                participant
                                    .molecule()
                                    .id()
                                    .value()
                                    .contains("nitro_reduced_amine"))));
    final ReactionPredictionResult result =
        service.predict(
            ReactionPredictionRequest.of(
                withoutReagent.id(),
                ReactionSide.of(
                    List.of(
                        ReactionParticipant.of(rich, StoichiometricCoefficient.ONE),
                        ReactionParticipant.of(hydrogen, StoichiometricCoefficient.of(3)))),
                withoutReagent.conditions(),
                withoutReagent.domains(),
                List.of(),
                null));

    Assertions.assertTrue(
        result.candidates().stream()
            .anyMatch(
                candidate -> {
                  if (!OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                      candidate, "graph_nitro_reduction_to_amine")) {
                    return false;
                  }
                  final Molecule product = candidate.products().participants().get(0).molecule();
                  return product.id().value().contains("nitro_reduced_amine")
                      && product.atomCount() == rich.atomCount()
                      && OrganicReactionPredictionServiceTest.atomCount(product, "O")
                          == OrganicReactionPredictionServiceTest.atomCount(rich, "O") - 2
                      && OrganicReactionPredictionServiceTest.atomCount(product, "H")
                          == OrganicReactionPredictionServiceTest.atomCount(rich, "H") + 2
                      && OrganicReactionPredictionServiceTest.hasBond(
                          product, "a9", "a9_reduced_h1", BondType.SINGLE)
                      && OrganicReactionPredictionServiceTest.hasBond(
                          product, "a9", "a9_reduced_h2", BondType.SINGLE);
                }),
        OrganicReactionPredictionServiceTest.candidateSummary(result));
    OrganicReactionPredictionServiceTest.assertNoCandidateEvidenceKind(
        result, ReactionPredictionEvidenceKind.SURROGATE_PRODUCT);
  }

  @Test
  void conditionCompatibilityScoringAddsMechanismSpecificEvidenceToGeneratedCandidates() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final Molecule bromoHeterocycle =
        OrganicReactionPredictionServiceTest.halogenatedNitrogenHeterocycle(
            "condition_bromo_heterocycle", "Br");
    final Molecule fluoroHeterocycle =
        OrganicReactionPredictionServiceTest.halogenatedNitrogenHeterocycle(
            "condition_fluoro_heterocycle", "F");
    final ReactionPredictionRequest suzukiRequest =
        OrganicReactionPredictionServiceTest.request(
            "condition_suzuki",
            bromoHeterocycle,
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.CROSS_COUPLING),
            List.of(
                ReactionConditionFeatureKind.CATALYST_PRESENT,
                ReactionConditionFeatureKind.INERT_ATMOSPHERE,
                ReactionConditionFeatureKind.ANHYDROUS,
                ReactionConditionFeatureKind.HEATING));
    final ReactionPredictionRequest snarRequest =
        ReactionPredictionRequest.of(
            "condition_snar",
            ReactionSide.of(
                List.of(ReactionParticipant.of(fluoroHeterocycle, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(95.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(8.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.named(ReactionComponentRole.SOLVENT, "DMF"),
                    ReactionConditionComponent.named(ReactionComponentRole.BASE, "K2CO3")),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS)),
                "polar aprotic SNAr screen"),
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            List.of(),
            null);

    final ReactionProductCandidate suzukiCandidate =
        OrganicReactionPredictionServiceTest.candidateByEvidence(
            service.predict(
                withExplicitPartner(suzukiRequest, organicBoronatePartner("condition_boronate"))),
            "fragment_transfer_heteroaryl_boronate_suzuki_coupling");
    final ReactionProductCandidate snarCandidate =
        OrganicReactionPredictionServiceTest.candidateByEvidence(
            service.predict(withExplicitPartner(snarRequest, primaryAmine("condition_amine"))),
            "replace_heteroaryl_halide_with_amine_fragment");
    final ReactionPredictionRequest acidicProticSnarRequest =
        ReactionPredictionRequest.of(
            "condition_snar_acidic_protic_conflict",
            ReactionSide.of(
                List.of(ReactionParticipant.of(fluoroHeterocycle, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(75.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(6.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.named(ReactionComponentRole.SOLVENT, "methanol"),
                    ReactionConditionComponent.named(ReactionComponentRole.ACID, "acetic acid")),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.ACIDIC_MEDIUM),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING)),
                "acidic protic SNAr conflict screen"),
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            List.of(),
            null);
    final ReactionProductCandidate conflictedSnarCandidate =
        OrganicReactionPredictionServiceTest.candidateByEvidence(
            service.predict(
                withExplicitPartner(acidicProticSnarRequest, primaryAmine("condition_amine"))),
            "replace_heteroaryl_halide_with_amine_fragment");

    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasEvidenceContaining(
            suzukiCandidate, "cross-coupling evidence is compatible"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasEvidenceContaining(
            snarCandidate, "SNAr/leaving-group evidence is compatible"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasEvidenceContaining(
            conflictedSnarCandidate, "direct retained-ring substitution is weakened"),
        conflictedSnarCandidate.evidence().toString());
    Assertions.assertTrue(suzukiCandidate.probability() > 0.0);
    Assertions.assertTrue(snarCandidate.probability() > 0.0);
  }

  @Test
  void heatedPegAzaHeteroarylAminePredictionIsMechanisticAndNotBenchmarkLookup() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final Molecule chloroAzaHeterocycle =
        OrganicReactionPredictionServiceTest.selectivityProbeHeterocycle();
    final Molecule benzylamineLikeNucleophile =
        OrganicReactionPredictionServiceTest.benzylamine("heated_peg_benzylamine_nucleophile");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "heated_peg_aza_heteroaryl_amine_prediction",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(chloroAzaHeterocycle, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(
                        benzylamineLikeNucleophile, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(120.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(3.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.of(
                        ReactionComponentRole.REACTANT,
                        chloroAzaHeterocycle.id().value(),
                        SubstanceAmount.of(10.0, SubstanceAmountUnit.GRAM),
                        null,
                        StoichiometricEquivalent.of(1.0),
                        null,
                        ReactionComponentPurity.percent(98.0),
                        ReactionPhase.SOLID,
                        "activated chlorocyanopyridine-like limiting reagent"),
                    ReactionConditionComponent.of(
                        ReactionComponentRole.REACTANT,
                        benzylamineLikeNucleophile.id().value(),
                        SubstanceAmount.of(6.5, SubstanceAmountUnit.GRAM),
                        null,
                        null,
                        null,
                        ReactionComponentPurity.percent(99.0),
                        ReactionPhase.LIQUID,
                        "benzylamine nucleophile loading"),
                    ReactionConditionComponent.named(ReactionComponentRole.SOLVENT, "PEG4000"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.BASE, "inorganic carbonate base")),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS)),
                "heated viscous polyether aza-heteroaryl amination screen"),
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION,
                OrganicReactionDomain.REARRANGEMENT),
            List.of(),
            null);

    final ReactionPredictionResult result = service.predict(request);

    Assertions.assertTrue(result.hasCandidates());
    Assertions.assertTrue(
        result.candidates().stream()
            .anyMatch(
                candidate ->
                    OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                            candidate, "amine_fragment_snar")
                        && OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                            candidate, "SNAr/leaving-group evidence is compatible")
                        && OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                            candidate, "Viscous polyether/polyol medium")
                        && OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                            candidate,
                            "Condition mechanism profile supports NUCLEOPHILIC_AROMATIC_SUBSTITUTION")),
        OrganicReactionPredictionServiceTest.candidateSummary(result));
    Assertions.assertTrue(
        result.candidates().stream()
            .anyMatch(
                candidate ->
                    OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                            candidate, "Benzylic amine environment")
                        && OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                            candidate, "Mass/mole recipe matched")
                        && candidate.predictedMechanisms().stream()
                            .anyMatch(
                                mechanism ->
                                    mechanism.family()
                                            == ReactionMechanismFamily
                                                .NUCLEOPHILIC_AROMATIC_SUBSTITUTION
                                        && mechanism.hasDetailedMechanism()
                                        && !mechanism.detailedMechanism().steps().isEmpty()
                                        && mechanism
                                                .detailedMechanism()
                                                .steps()
                                                .get(0)
                                                .events()
                                                .size()
                                            >= 5)),
        OrganicReactionPredictionServiceTest.candidateSummary(result));
    Assertions.assertTrue(
        result.candidates().stream()
            .anyMatch(
                candidate ->
                    OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                            candidate, "amine_fragment_anrorc")
                        && OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                            candidate, "addition/ring-opening/ring-closure pathway is compatible")
                        && OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                            candidate,
                            "Condition mechanism profile supports ADDITION_RING_OPENING_RING_CLOSURE")),
        OrganicReactionPredictionServiceTest.candidateSummary(result));
    Assertions.assertTrue(
        result.candidates().stream()
            .flatMap(candidate -> candidate.evidence().stream())
            .noneMatch(
                evidence -> evidence.kind() == ReactionPredictionEvidenceKind.CALIBRATION_PRIOR));
    Assertions.assertTrue(
        result.candidates().stream()
            .anyMatch(
                candidate ->
                    OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                            candidate, "Contextual kinetic model")
                        && OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                            candidate, "conversion over declared duration")),
        OrganicReactionPredictionServiceTest.candidateSummary(result));
    Assertions.assertTrue(
        result.bestCandidate().evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.KINETIC_FACTOR
                        || evidence.kind() == ReactionPredictionEvidenceKind.SOLVENT_EFFECT
                        || evidence.kind() == ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN),
        result.bestCandidate().evidence().toString());
    Assertions.assertTrue(
        result.candidates().stream()
            .anyMatch(
                candidate ->
                    candidate
                        .scoreBreakdown()
                        .hasComponent(ReactionPredictionScoreComponentKind.CONDITION_MATCH)),
        OrganicReactionPredictionServiceTest.candidateSummary(result));
  }

  @Test
  void builtInProductPredictionRulesTransferAmineFragmentWithoutMutatingReactants() {
    final Molecule reactant = OrganicReactionPredictionServiceTest.functionalizedHeterocycle();
    final Molecule amine =
        OrganicReactionPredictionServiceTest.primaryAmine("functionalized_heterocycle_amine");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "heterocycle_replacement_transform",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(reactant, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(amine, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(110.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(10.0, TimeDurationUnit.HOUR),
                List.of(),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS)),
                "heterocycle replacement transform"),
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            List.of(),
            null);
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));

    final ReactionPredictionResult result = service.predict(request);

    Assertions.assertTrue(result.hasCandidates());
    final ReactionProductCandidate replacementCandidate =
        result.candidates().stream()
            .filter(
                candidate ->
                    candidate.evidence().stream()
                        .anyMatch(
                            evidence ->
                                evidence.description().contains("Generated SNAr site combination")))
            .findFirst()
            .orElseThrow();
    final Molecule product =
        replacementCandidate.products().participants().stream()
            .map(ReactionParticipant::molecule)
            .filter(molecule -> molecule.id().value().contains("_amine_coupled"))
            .findFirst()
            .orElseThrow();
    Assertions.assertEquals(reactant.atomCount() + amine.atomCount() - 2, product.atomCount());
    Assertions.assertTrue(product.bondCount() > reactant.bondCount());
    Assertions.assertEquals(1, OrganicReactionPredictionServiceTest.atomCount(reactant, "Cl"));
    Assertions.assertEquals(0, OrganicReactionPredictionServiceTest.atomCount(product, "Cl"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.atomCount(product, "N")
            > OrganicReactionPredictionServiceTest.atomCount(reactant, "N"));
    Assertions.assertTrue(
        replacementCandidate.products().participants().stream()
            .map(ReactionParticipant::molecule)
            .anyMatch(molecule -> molecule.id().value().contains("_leaving_hcl")));
  }

  @Test
  void builtInReplacementRuleGeneratesEveryRegioisomericLeavingGroupSite() {
    final Molecule reactant = OrganicReactionPredictionServiceTest.dichloroHeterocycle();
    final Molecule amine =
        OrganicReactionPredictionServiceTest.primaryAmine("regioisomeric_replacement_amine");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "heterocycle_regioisomeric_replacement_transform",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(reactant, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(amine, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(110.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(10.0, TimeDurationUnit.HOUR),
                List.of(),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS)),
                "regioisomeric heterocycle replacement transform"),
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            List.of(),
            null);
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));

    final ReactionPredictionResult result = service.predict(request);
    final List<ReactionProductCandidate> replacementCandidates =
        result.candidates().stream()
            .filter(OrganicReactionPredictionServiceTest::hasBuiltInReplacementEvidence)
            .toList();

    Assertions.assertEquals(2, replacementCandidates.size());
    Assertions.assertTrue(
        replacementCandidates.stream()
            .anyMatch(
                candidate ->
                    OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                        candidate, " at a6")));
    Assertions.assertTrue(
        replacementCandidates.stream()
            .anyMatch(
                candidate ->
                    OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                        candidate, " at a7")));
    for (int i = 0; i < replacementCandidates.size(); ++i) {
      final ReactionProductCandidate candidate = replacementCandidates.get(i);
      final Molecule product =
          candidate.products().participants().stream()
              .map(ReactionParticipant::molecule)
              .filter(molecule -> !molecule.id().value().contains("_leaving_group_coproduct_"))
              .findFirst()
              .orElseThrow();
      Assertions.assertEquals(reactant.atomCount() + amine.atomCount() - 2, product.atomCount());
      Assertions.assertTrue(product.bondCount() > reactant.bondCount());
      Assertions.assertEquals(1, OrganicReactionPredictionServiceTest.atomCount(product, "Cl"));
      Assertions.assertTrue(
          OrganicReactionPredictionServiceTest.atomCount(product, "N")
              > OrganicReactionPredictionServiceTest.atomCount(reactant, "N"));
      Assertions.assertTrue(
          candidate.products().participants().stream()
              .map(ReactionParticipant::molecule)
              .anyMatch(
                  molecule ->
                      (molecule.id().value().contains("_leaving_group_coproduct_")
                              || molecule.id().value().contains("_leaving_hcl"))
                          && OrganicReactionPredictionServiceTest.atomCount(molecule, "Cl") == 1));
    }
    Assertions.assertEquals(8, reactant.atomCount());
    Assertions.assertEquals(8, reactant.bondCount());
    Assertions.assertEquals(2, OrganicReactionPredictionServiceTest.atomCount(reactant, "Cl"));
  }

  @Test
  void builtInReplacementRuleRanksRegioisomericSitesByLocalHeterocycleEnvironment() {
    final Molecule reactant = OrganicReactionPredictionServiceTest.asymmetricDichloroHeterocycle();
    final Molecule amine =
        OrganicReactionPredictionServiceTest.primaryAmine("site_selective_replacement_amine");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "site_selective_heterocycle_replacement_transform",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(reactant, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(amine, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(105.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(6.0, TimeDurationUnit.HOUR),
                List.of(),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS)),
                "asymmetric regioisomeric heterocycle replacement transform"),
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            List.of(),
            null);
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));

    final ReactionPredictionResult result = service.predict(request);
    final List<ReactionProductCandidate> replacementCandidates =
        result.candidates().stream()
            .filter(OrganicReactionPredictionServiceTest::hasBuiltInReplacementEvidence)
            .toList();

    Assertions.assertEquals(2, replacementCandidates.size());
    final ReactionProductCandidate nitrogenAdjacent =
        OrganicReactionPredictionServiceTest.candidateByEvidence(replacementCandidates, " at a6");
    final ReactionProductCandidate remote =
        OrganicReactionPredictionServiceTest.candidateByEvidence(replacementCandidates, " at a7");
    Assertions.assertTrue(nitrogenAdjacent.probability() > remote.probability());
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasEvidenceContaining(
            nitrogenAdjacent, "Site-selectivity multiplier="));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasEvidenceContaining(
            nitrogenAdjacent, "Electron-poor heteroaryl site is activated"));
  }

  @Test
  void builtInHeterocycleTemplateLibraryCoversMainPlanningFamilies() {
    final OrganicReactionTemplateLibrary templates =
        BuiltInOrganicReactionTemplateLibraries.heterocyclePlanning();

    Assertions.assertTrue(templates.size() >= 13);
    Assertions.assertTrue(
        templates.templates().stream()
            .anyMatch(template -> template.id().value().equals("template_heteroaryl_snar")));
    Assertions.assertTrue(
        templates.templates().stream()
            .anyMatch(
                template -> template.id().value().equals("template_heterocycle_n_oxidation")));
    Assertions.assertTrue(
        templates.templates().stream()
            .anyMatch(
                template ->
                    template.id().value().equals("template_directed_heteroarene_metalation")));
    Assertions.assertTrue(
        templates.templates().stream()
            .anyMatch(
                template ->
                    template.id().value().equals("template_paal_knorr_heterocycle_synthesis")));
    Assertions.assertTrue(
        templates.templates().stream()
            .anyMatch(
                template -> template.id().value().equals("template_redox_sensitive_heterocycle")));
  }

  @Test
  void defaultClassicalServiceKeepsMissingReagentAndTemplateDiagnostics() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Molecule probe = OrganicReactionPredictionServiceTest.functionalizedHeterocycle();
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "template_advisory",
            ReactionSide.of(List.of(ReactionParticipant.of(probe, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                null,
                null,
                TimeDuration.of(3.0, TimeDurationUnit.HOUR),
                List.of(),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.CATALYST_PRESENT)),
                "template advisory"),
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(),
            null);

    final ReactionPredictionResult result = service.predictReactionProducts(request);

    assertDiagnosticOnly(result, "transform_heteroaryl_hydroxylation");
    assertDiagnosticOnly(result, "template_heteroarene_functionalization");
    Assertions.assertFalse(result.hasCandidates());
  }

  @Test
  void builtInTemplatesDoNotMatchWhenRequiredReactantFeaturesAreAbsent() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Molecule benzene =
        OrganicReactionPredictionServiceTest.ringMolecule("benzene_probe", "C");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "template_negative",
            ReactionSide.of(
                List.of(ReactionParticipant.of(benzene, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                null,
                null,
                TimeDuration.of(3.0, TimeDurationUnit.HOUR),
                List.of(),
                List.of(ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS)),
                "template negative"),
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(),
            null);

    final ReactionPredictionResult result = service.predictReactionProducts(request);

    Assertions.assertFalse(result.hasCandidates());
    Assertions.assertTrue(
        result.diagnostics().stream()
            .noneMatch(
                evidence ->
                    evidence.description().contains("Matched rule produced no candidates")));
  }

  @Test
  void curatedProductRuleGeneratesExplicitCandidateAndKeepsCoreValidationClean() {
    final Molecule pyridine =
        OrganicReactionPredictionServiceTest.ringMolecule("curated_pyridine", "N");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "curated_product_generation",
            ReactionSide.of(
                List.of(ReactionParticipant.of(pyridine, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                null,
                null,
                TimeDuration.of(6.0, TimeDurationUnit.HOUR),
                List.of(),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING)),
                "curated product"),
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(),
            null);
    final OrganicReactionRule curatedRule =
        CuratedProductOrganicReactionRule.of(
            OrganicReactionRuleId.of("curated_pyridine_product"),
            "Curated pyridine product",
            OrganicReactionRuleCategory.HETEROCYCLE_FUNCTIONALIZATION,
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(MolecularFeatureKind.HETEROCYCLE, MolecularFeatureKind.AROMATIC_RING),
            List.of(ReactionConditionFeatureKind.ANHYDROUS),
            ReactionSide.of(
                List.of(ReactionParticipant.of(pyridine, StoichiometricCoefficient.ONE))),
            0.62,
            62.0,
            "curated balanced candidate",
            "Explicit curated product side is available for this heterocycle case.");
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.fromRulesAndTemplates(
                    List.of(curatedRule),
                    BuiltInOrganicReactionTemplateLibraries.heterocyclePlanning())));

    final ReactionPredictionResult result = service.predict(request);
    final ChemistryValidationResult validation =
        new ChemistryCoreValidator()
            .validateReactionPredictionRequest(
                ReactionPredictionRequest.of(
                    request.id(),
                    request.reactants(),
                    request.conditions(),
                    request.domains(),
                    result.candidates(),
                    null));

    Assertions.assertTrue(result.hasCandidates());
    Assertions.assertEquals(
        pyridine, result.bestCandidate().products().participants().get(0).molecule());
    Assertions.assertTrue(result.bestCandidate().expectedYieldPercent() > 0.0);
    Assertions.assertTrue(validation.valid());
    Assertions.assertTrue(
        result.bestCandidate().evidence().stream()
            .anyMatch(evidence -> evidence.description().contains("curated_pyridine_product")));
    Assertions.assertTrue(
        result.diagnostics().stream()
            .anyMatch(evidence -> evidence.description().contains("Rule matched")));
  }

  @Test
  void curatedProductRuleDoesNotGenerateCandidateWhenRequiredFeaturesAreMissing() {
    final Molecule benzene =
        OrganicReactionPredictionServiceTest.ringMolecule("curated_benzene", "C");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "curated_product_negative",
            ReactionSide.of(
                List.of(ReactionParticipant.of(benzene, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                null,
                null,
                TimeDuration.of(6.0, TimeDurationUnit.HOUR),
                List.of(),
                List.of(ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS)),
                "curated product negative"),
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(),
            null);
    final OrganicReactionRule curatedRule =
        CuratedProductOrganicReactionRule.of(
            OrganicReactionRuleId.of("curated_requires_heterocycle"),
            "Curated requires heterocycle",
            OrganicReactionRuleCategory.HETEROCYCLE_FUNCTIONALIZATION,
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(MolecularFeatureKind.HETEROCYCLE),
            List.of(ReactionConditionFeatureKind.ANHYDROUS),
            ReactionSide.of(
                List.of(ReactionParticipant.of(benzene, StoichiometricCoefficient.ONE))),
            0.5,
            50.0,
            "must not appear",
            "This rule requires a heterocycle feature.");
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                OrganicReactionRuleLibrary.of(List.of(curatedRule))));

    final ReactionPredictionResult result = service.predict(request);

    Assertions.assertFalse(result.hasCandidates());
    Assertions.assertTrue(
        result.diagnostics().stream()
            .anyMatch(evidence -> evidence.description().contains("No product candidates")));
  }

  @Test
  void productPredictionIsClassicalPlanningCapability() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Molecule pyridine = OrganicReactionPredictionServiceTest.ringMolecule("pyridine", "N");
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("heterocycle_reaction"),
            "Heterocycle reaction",
            ReactionSide.of(
                List.of(ReactionParticipant.of(pyridine, StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(ReactionParticipant.of(pyridine, StoichiometricCoefficient.ONE))));
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("heterocycle_prediction"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION));

    final ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation
            .ClassicalCalculationPlan
        plan = service.plan(request, BuiltInClassicalBackendProfiles.universalPlanning());

    Assertions.assertEquals(ClassicalPreflightStatus.SUPPORTED, plan.preflightResult().status());
    Assertions.assertTrue(
        plan.steps()
            .get(0)
            .requiredCapabilities()
            .contains(ClassicalCapability.ORGANIC_REACTION_PREDICTION));
  }

  @Test
  void classicalServiceExecutionReturnsDecisionReadyHeteroarylAminationPredictionTables() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Molecule scaffold = OrganicReactionPredictionServiceTest.dichloroHeterocycle();
    final Molecule amine =
        OrganicReactionPredictionServiceTest.primaryAmine("executor_benzylamine_graph_source");
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("executor_heteroaryl_amination"),
            "Executor heteroaryl amination",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(scaffold, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(amine, StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(ReactionParticipant.of(scaffold, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(120.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(3.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.named(ReactionComponentRole.BASE, "K2CO3"),
                    ReactionConditionComponent.named(ReactionComponentRole.SOLVENT, "PEG4000")),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS)),
                "Executor-level PEG4000 carbonate amination screen"),
            ChemistryMetadata.EMPTY);
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("executor_heteroaryl_amination_prediction"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION),
            List.of(
                ClassicalCalculationOption.bool("reaction_prediction_electronic_rerank", false)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);
    final ClassicalResultSection section =
        result.requireSection(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION);

    Assertions.assertFalse(result.failed(), result.diagnostics().toString());
    Assertions.assertFalse(result.unsupported(), result.diagnostics().toString());
    Assertions.assertTrue(section.requireScalar("candidate_count") > 0.0);
    Assertions.assertTrue(section.requireScalar("generated_candidate_count") > 0.0);
    Assertions.assertTrue(section.requireScalar("explicit_product_graph_candidate_count") > 0.0);
    Assertions.assertTrue(section.requireScalar("mechanism_only_candidate_count") >= 0.0);
    Assertions.assertTrue(section.requireScalar("explicit_product_graph_coverage_ratio") > 0.0);
    Assertions.assertTrue(
        section.requireScalar("candidate_ready_for_electronic_rerank_count") > 0.0);
    Assertions.assertTrue(
        section.requireScalar("candidate_ready_for_quantum_workflow_count") > 0.0);
    Assertions.assertEquals(
        "FAST_MECHANISTIC", section.requireFirstValue("prediction_execution_mode"));
    Assertions.assertEquals("true", section.requireFirstValue("electronic_rerank_opt_in_required"));
    Assertions.assertNotEquals("EMPTY", section.requireFirstValue("product_graph_coverage_status"));
    Assertions.assertFalse(section.requireFirstValue("product_graph_followup_reason").isBlank());
    Assertions.assertTrue(section.requireScalar("calibration_library_observation_count") >= 0.0);
    Assertions.assertTrue(
        section.requireScalar("calibration_library_exact_observation_count") >= 0.0);
    Assertions.assertEquals(
        "false", section.requireFirstValue("calibration_library_applied_to_prediction"));
    Assertions.assertTrue(section.requireScalar("condition_effect_count") > 0.0);
    Assertions.assertTrue(section.requireScalar("mechanism_pattern_count") > 0.0);
    Assertions.assertTrue(section.requireScalar("mechanism_chemically_usable_count") > 0.0);
    Assertions.assertTrue(section.requireScalar("condition_scenario_plan_count") > 0.0);
    Assertions.assertTrue(section.requireScalar("condition_scenario_axis_count") >= 5.0);
    Assertions.assertNotEquals(
        "EMPTY", section.requireFirstValue("condition_scenario_coverage_status"));
    Assertions.assertTrue(section.requireValue("candidates").valueCount() > 1);
    Assertions.assertTrue(section.requireValue("candidate_evidence_trace").valueCount() > 1);
    Assertions.assertTrue(section.requireValue("candidate_factor_matrix").valueCount() > 1);
    Assertions.assertTrue(section.requireValue("candidate_readiness_matrix").valueCount() > 1);
    Assertions.assertTrue(
        section.requireValues("candidate_readiness_matrix").stream()
            .anyMatch(row -> row.contains("true")));
    Assertions.assertTrue(section.requireValue("condition_effect_trace").valueCount() > 1);
    Assertions.assertTrue(section.requireValue("prediction_followup_plan").valueCount() > 1);
    Assertions.assertTrue(section.requireValue("condition_scenario_plan").valueCount() > 1);
    Assertions.assertTrue(section.requireValue("condition_scenario_coverage").valueCount() > 1);
    Assertions.assertTrue(
        section.requireValues("condition_scenario_coverage").stream()
            .anyMatch(row -> row.contains("solvent\ttrue")));
    Assertions.assertTrue(
        section
            .requireFirstValue("activated_reaction_domains")
            .contains("HETEROCYCLE_FUNCTIONALIZATION"));
    Assertions.assertTrue(
        section
            .requireFirstValue("activated_reaction_domains")
            .contains("NUCLEOPHILIC_SUBSTITUTION"));
    Assertions.assertTrue(
        section.requireValues("candidate_evidence_trace").stream()
            .anyMatch(row -> row.contains("amine-like nitrogen nucleophile source")));
    Assertions.assertTrue(
        section.requireValues("candidate_evidence_trace").stream()
            .anyMatch(row -> row.contains("Inorganic carbonate/hydroxide base supplies")));
  }

  @Test
  void classicalServiceExecutionCanElectronicallyRerankGeneratedGraphProducts() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Molecule scaffold = OrganicReactionPredictionServiceTest.chloroDimethylNicotinonitrile();
    final Molecule amine =
        OrganicReactionPredictionServiceTest.primaryAmine("executor_rerank_amine_graph_source");
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("executor_graph_product_electronic_rerank"),
            "Executor graph product electronic rerank",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(scaffold, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(amine, StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(ReactionParticipant.of(scaffold, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(120.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(3.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.named(ReactionComponentRole.BASE, "K2CO3"),
                    ReactionConditionComponent.named(ReactionComponentRole.SOLVENT, "PEG4000")),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS)),
                "Executor-level electronic rerank amination screen"),
            ChemistryMetadata.EMPTY);
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("executor_graph_product_electronic_rerank"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION),
            List.of(
                ClassicalCalculationOption.bool("reaction_prediction_electronic_rerank", true),
                ClassicalCalculationOption.text(
                    "reaction_prediction_electronic_rerank_max_candidates", "2"),
                ClassicalCalculationOption.text(
                    "reaction_prediction_electronic_rerank_max_atoms", "64")),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);
    final ClassicalResultSection section =
        result.requireSection(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION);

    Assertions.assertFalse(result.failed(), result.diagnostics().toString());
    Assertions.assertTrue(
        section
            .requireFirstValue("candidate_evidence_kind_histogram")
            .contains("ELECTRONIC_FACTOR="),
        section.requireFirstValue("candidate_evidence_kind_histogram"));
    Assertions.assertTrue(
        section.requireValues("candidate_distribution_trace").stream()
            .anyMatch(row -> row.contains("ELECTRONIC:")),
        section.requireValues("candidate_distribution_trace").toString());
    Assertions.assertTrue(
        section.requireValues("candidate_readiness_matrix").stream()
            .anyMatch(
                row -> row.endsWith("\tready") && row.contains("\ttrue\ttrue\ttrue\ttrue\ttrue\t")),
        section.requireValues("candidate_readiness_matrix").toString());
  }

  @Test
  void classicalServiceExecutionCanPrepareQuantumWorkflowForGeneratedProductCandidates() {
    final Molecule atomOne =
        OrganicReactionPredictionServiceTest.singleAtomMolecule(
            "executor_quantum_workflow_h_atom_one", "H");
    final Molecule atomTwo =
        OrganicReactionPredictionServiceTest.singleAtomMolecule(
            "executor_quantum_workflow_h_atom_two", "H");
    final Molecule generatedProduct =
        OrganicReactionPredictionServiceTest.twoAtomMolecule(
            "executor_quantum_workflow_generated_h2", "H", "H", BondType.SINGLE);
    final OrganicReactionRule generatedProductRule =
        new OrganicReactionRule() {
          public OrganicReactionRuleId id() {
            return OrganicReactionRuleId.of("executor_quantum_workflow_generated_h2_rule");
          }

          public String displayName() {
            return "Executor quantum workflow generated hydrogen product rule";
          }

          public OrganicReactionRuleCategory category() {
            return OrganicReactionRuleCategory.REDUCTION;
          }

          public OrganicReactionRuleMatch match(final ReactionPredictionRequest request) {
            return OrganicReactionRuleMatch.matched(
                0.90, "small generated product rule for quantum workflow preparation");
          }

          public List<ReactionProductCandidate> generate(
              final ReactionPredictionRequest request, final OrganicReactionRuleMatch match) {
            return List.of(
                ReactionProductCandidate.of(
                    ReactionSide.of(
                        List.of(
                            ReactionParticipant.of(
                                generatedProduct, StoichiometricCoefficient.ONE))),
                    0.80,
                    80.0,
                    "small generated product with explicit graph",
                    List.of(
                        ReactionPredictionEvidence.of(
                            ReactionPredictionEvidenceKind.USER_RULE,
                            "generated structural product for quantum workflow preparation",
                            0.40))));
          }
        };
    final ReactionProductPredictionExecutor executor =
        new ReactionProductPredictionExecutor(
            new OrganicReactionPredictionService(
                new MolecularFeatureAnalyzer(),
                new RuleBasedOrganicReactionCandidateGenerator(
                    OrganicReactionRuleLibrary.of(List.of(generatedProductRule)))),
            new MolecularFeatureAnalyzer());
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("executor_graph_product_quantum_workflow"),
            "Executor graph product quantum workflow",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(atomOne, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(atomTwo, StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(ReactionParticipant.of(generatedProduct, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(25.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(1.0, TimeDurationUnit.HOUR),
                List.of(),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.REDUCING_MEDIUM)),
                "Executor-level product quantum workflow preparation screen"),
            ChemistryMetadata.EMPTY);
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("executor_graph_product_quantum_workflow"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION),
            List.of(
                ClassicalCalculationOption.bool(
                    "reaction_prediction_prepare_quantum_workflows", true),
                ClassicalCalculationOption.text(
                    "reaction_prediction_prepare_quantum_workflows_max_candidates", "1"),
                ClassicalCalculationOption.text(
                    "reaction_prediction_prepare_quantum_workflows_max_participants", "1")),
            ChemistryMetadata.EMPTY);
    final ArrayList<ClassicalDiagnostic> diagnostics = new ArrayList<ClassicalDiagnostic>();

    final ClassicalResultSection section = executor.execute(request, diagnostics);

    Assertions.assertNotNull(section, diagnostics.toString());
    Assertions.assertEquals(
        "true", section.requireFirstValue("candidate_quantum_workflow_preparation_enabled"));
    Assertions.assertTrue(
        section
            .requireValues("candidate_quantum_workflow_preparation")
            .get(0)
            .contains("vqe_status"),
        section.requireValues("candidate_quantum_workflow_preparation").toString());
    Assertions.assertTrue(
        section.requireValues("candidate_quantum_workflow_preparation").stream()
            .anyMatch(
                row ->
                    row.contains("\tprepared\t")
                        && row.contains("\texact\t")
                        && row.contains("\ttrue\tnone\tnone\t0\ttrue\t")
                        && row.contains("VQE")
                        && !row.contains("\tmissing\t")),
        section.requireValues("candidate_quantum_workflow_preparation").toString());
  }

  @Test
  void quantumWorkflowPreparationUsesCompactGeneratedBasisForHeavyGeneratedProducts() {
    final Molecule reactant =
        OrganicReactionPredictionServiceTest.halogenatedNitrogenHeterocycle(
            "executor_quantum_workflow_iodine_source", "I");
    final Molecule generatedProduct =
        OrganicReactionPredictionServiceTest.halogenatedNitrogenHeterocycle(
            "executor_quantum_workflow_iodo_product", "I");
    final OrganicReactionRule generatedProductRule =
        new OrganicReactionRule() {
          public OrganicReactionRuleId id() {
            return OrganicReactionRuleId.of("executor_quantum_workflow_generated_iodo_rule");
          }

          public String displayName() {
            return "Executor quantum workflow generated iodo heterocycle product rule";
          }

          public OrganicReactionRuleCategory category() {
            return OrganicReactionRuleCategory.HETEROCYCLE_FUNCTIONALIZATION;
          }

          public OrganicReactionRuleMatch match(final ReactionPredictionRequest request) {
            return OrganicReactionRuleMatch.matched(
                0.82, "heavy generated product rule for quantum workflow preparation");
          }

          public List<ReactionProductCandidate> generate(
              final ReactionPredictionRequest request, final OrganicReactionRuleMatch match) {
            return List.of(
                ReactionProductCandidate.of(
                    ReactionSide.of(
                        List.of(
                            ReactionParticipant.of(
                                generatedProduct, StoichiometricCoefficient.ONE))),
                    0.77,
                    62.0,
                    "generated heavy explicit graph product",
                    List.of(
                        ReactionPredictionEvidence.of(
                            ReactionPredictionEvidenceKind.USER_RULE,
                            "generated heavy structural product for quantum workflow preparation",
                            0.41))));
          }
        };
    final ReactionProductPredictionExecutor executor =
        new ReactionProductPredictionExecutor(
            new OrganicReactionPredictionService(
                new MolecularFeatureAnalyzer(),
                new RuleBasedOrganicReactionCandidateGenerator(
                    OrganicReactionRuleLibrary.of(List.of(generatedProductRule)))),
            new MolecularFeatureAnalyzer());
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("executor_heavy_graph_product_quantum_workflow"),
            "Executor heavy graph product quantum workflow",
            ReactionSide.of(
                List.of(ReactionParticipant.of(reactant, StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(ReactionParticipant.of(generatedProduct, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(80.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(2.0, TimeDurationUnit.HOUR),
                List.of(),
                List.of(ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING)),
                "Executor-level heavy product quantum workflow preparation screen"),
            ChemistryMetadata.EMPTY);
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("executor_heavy_graph_product_quantum_workflow"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION),
            List.of(
                ClassicalCalculationOption.bool(
                    "reaction_prediction_prepare_quantum_workflows", true),
                ClassicalCalculationOption.text(
                    "reaction_prediction_prepare_quantum_workflows_max_candidates", "1"),
                ClassicalCalculationOption.text(
                    "reaction_prediction_prepare_quantum_workflows_max_participants", "1")),
            ChemistryMetadata.EMPTY);
    final ArrayList<ClassicalDiagnostic> diagnostics = new ArrayList<ClassicalDiagnostic>();

    final ClassicalResultSection section = executor.execute(request, diagnostics);

    Assertions.assertNotNull(section, diagnostics.toString());
    Assertions.assertTrue(
        section.requireValues("candidate_quantum_workflow_preparation").stream()
            .anyMatch(
                row ->
                    row.contains("\tprepared\t")
                        && row.contains("\tnative-valence-semiempirical\t")
                        && row.contains("\tfallback\t")
                        && row.contains("\tFULL_TWO_ELECTRON\t")
                        && row.contains("\ttrue\tnone\tnone\t0\ttrue\t")),
        section.requireValues("candidate_quantum_workflow_preparation").toString());
  }

  @Test
  void featureAnalyzerDetectsOrganicReactionRelevantMotifs() {
    final MolecularFeatureAnalyzer analyzer = new MolecularFeatureAnalyzer();
    final Molecule probe = OrganicReactionPredictionServiceTest.functionalizedHeterocycle();

    final MolecularFeatureSet features = analyzer.analyze(probe);

    Assertions.assertEquals(1L, features.countOf(MolecularFeatureKind.HETEROCYCLE));
    Assertions.assertEquals(1L, features.countOf(MolecularFeatureKind.NITROGEN_HETEROCYCLE));
    Assertions.assertEquals(1L, features.countOf(MolecularFeatureKind.AROMATIC_RING));
    Assertions.assertEquals(1L, features.countOf(MolecularFeatureKind.CARBONYL));
    Assertions.assertEquals(1L, features.countOf(MolecularFeatureKind.HALOGENATED));
    Assertions.assertEquals(1L, features.countOf(MolecularFeatureKind.CHARGED_CENTER));
    Assertions.assertEquals(1L, features.countOf(MolecularFeatureKind.RADICAL_CENTER));
  }

  @Test
  void featureAnalyzerDetectsBroadFunctionalGroupSet() {
    final MolecularFeatureAnalyzer analyzer = new MolecularFeatureAnalyzer();
    final Molecule probe = OrganicReactionPredictionServiceTest.functionalGroupRichMolecule();

    final MolecularFeatureSet features = analyzer.analyze(probe);

    Assertions.assertEquals(2L, features.countOf(MolecularFeatureKind.CARBONYL));
    Assertions.assertEquals(2L, features.countOf(MolecularFeatureKind.CARBOXYL_DERIVATIVE));
    Assertions.assertEquals(1L, features.countOf(MolecularFeatureKind.ESTER));
    Assertions.assertEquals(1L, features.countOf(MolecularFeatureKind.AMIDE));
    Assertions.assertEquals(1L, features.countOf(MolecularFeatureKind.ETHER));
    Assertions.assertEquals(1L, features.countOf(MolecularFeatureKind.AMINE));
    Assertions.assertEquals(1L, features.countOf(MolecularFeatureKind.IMINE));
    Assertions.assertEquals(1L, features.countOf(MolecularFeatureKind.NITRILE));
    Assertions.assertEquals(1L, features.countOf(MolecularFeatureKind.NITRO));
    Assertions.assertEquals(1L, features.countOf(MolecularFeatureKind.ALKENE));
    Assertions.assertEquals(1L, features.countOf(MolecularFeatureKind.ALKYNE));
    Assertions.assertEquals(2L, features.countOf(MolecularFeatureKind.THIOL_OR_THIOETHER));
    Assertions.assertEquals(1L, features.countOf(MolecularFeatureKind.SULFUR_OXIDE));
  }

  @Test
  void featureAnalyzerDetectsCarbonylSubtypesAndOxygenAcids() {
    final MolecularFeatureAnalyzer analyzer = new MolecularFeatureAnalyzer();
    final Molecule probe = OrganicReactionPredictionServiceTest.carbonylSubtypeAndOxygenAcidProbe();

    final MolecularFeatureSet features = analyzer.analyze(probe);

    Assertions.assertEquals(5L, features.countOf(MolecularFeatureKind.CARBONYL));
    Assertions.assertEquals(1L, features.countOf(MolecularFeatureKind.ALDEHYDE));
    Assertions.assertEquals(1L, features.countOf(MolecularFeatureKind.KETONE));
    Assertions.assertEquals(1L, features.countOf(MolecularFeatureKind.ACYL_HALIDE));
    Assertions.assertEquals(2L, features.countOf(MolecularFeatureKind.ANHYDRIDE));
    Assertions.assertEquals(1L, features.countOf(MolecularFeatureKind.ALCOHOL));
    Assertions.assertEquals(1L, features.countOf(MolecularFeatureKind.PHENOL));
  }

  @Test
  void candidateScorerExplainsHeterocycleConditionAndFunctionalGroupContributions() {
    final OrganicReactionCandidateScorer scorer =
        new OrganicReactionCandidateScorer(new MolecularFeatureAnalyzer());
    final Molecule product = OrganicReactionPredictionServiceTest.functionalizedHeterocycle();
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "scorer_explanation",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        OrganicReactionPredictionServiceTest.ringMolecule("scorer_pyridine", "N"),
                        StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(90.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(10.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.named(ReactionComponentRole.SOLVENT, "DMSO"),
                    ReactionConditionComponent.named(ReactionComponentRole.BASE, "K2CO3")),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM)),
                "condition-aware scorer"),
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(),
            null);

    final ReactionCandidateScore score =
        scorer.score(
            request,
            OrganicReactionPredictionServiceTest.candidate(product, 0.35, "scorer candidate"),
            0);

    Assertions.assertTrue(score.score() > 0.35);
    Assertions.assertTrue(
        score.evidence().stream()
            .anyMatch(
                evidence -> evidence.kind() == ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN));
    Assertions.assertTrue(
        score.evidence().stream()
            .anyMatch(
                evidence -> evidence.kind() == ReactionPredictionEvidenceKind.TEMPERATURE_EFFECT));
    Assertions.assertTrue(
        score.evidence().stream()
            .anyMatch(
                evidence -> evidence.kind() == ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR));
    Assertions.assertTrue(
        score.evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH));
  }

  @Test
  void candidateScorerUsesReactionPlausibilityForLeavingGroupAndNoConversionCases() {
    final OrganicReactionCandidateScorer scorer =
        new OrganicReactionCandidateScorer(new MolecularFeatureAnalyzer());
    final Molecule substrate = OrganicReactionPredictionServiceTest.dichloroHeterocycle();
    final Molecule substitutionProduct =
        OrganicReactionPredictionServiceTest.halogenatedNitrogenHeterocycle(
            "single_chloro_substitution_product", "Cl");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "plausibility_snar",
            ReactionSide.of(
                List.of(ReactionParticipant.of(substrate, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(80.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(4.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.named(ReactionComponentRole.BASE, "K2CO3"),
                    ReactionConditionComponent.named(ReactionComponentRole.SOLVENT, "DMSO")),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING)),
                "SNAr plausibility with base and leaving-group context"),
            List.of(
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION,
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(),
            null);

    final ReactionCandidateScore plausibleScore =
        scorer.score(
            request,
            OrganicReactionPredictionServiceTest.candidate(
                substitutionProduct, 0.35, "leaving group consumed"),
            0);
    final ReactionCandidateScore unchangedScore =
        scorer.score(
            request,
            OrganicReactionPredictionServiceTest.candidate(substrate, 0.35, "unchanged substrate"),
            1);

    Assertions.assertTrue(plausibleScore.score() > unchangedScore.score());
    Assertions.assertTrue(
        plausibleScore.evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH
                        && evidence.description().contains("leaving-group handle")));
    Assertions.assertTrue(
        unchangedScore.evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.WARNING
                        && evidence.description().contains("same molecular participants")));
  }

  @Test
  void candidateScorerPenalizesMissingHeterocycleForHeterocycleDomain() {
    final OrganicReactionCandidateScorer scorer =
        new OrganicReactionCandidateScorer(new MolecularFeatureAnalyzer());
    final Molecule benzene =
        OrganicReactionPredictionServiceTest.ringMolecule("scorer_benzene", "C");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "scorer_negative",
            ReactionSide.of(
                List.of(ReactionParticipant.of(benzene, StoichiometricCoefficient.ONE))),
            ReactionConditions.EMPTY,
            List.of(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS),
            List.of(),
            null);

    final ReactionCandidateScore score =
        scorer.score(
            request,
            OrganicReactionPredictionServiceTest.candidate(
                benzene, 0.5, "negative scorer candidate"),
            0);

    Assertions.assertTrue(score.score() < 0.61);
    Assertions.assertTrue(
        score.evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.WARNING
                        && evidence.description().contains("does not contain a heterocycle")));
  }

  @Test
  void predictionServiceKeepsSnarPathwayEvidenceAfterScoringAndNormalization() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(new MolecularFeatureAnalyzer());
    final ReactionPredictionExplainer explainer = new ReactionPredictionExplainer();
    final Molecule substrate = OrganicReactionPredictionServiceTest.dichloroHeterocycle();
    final Molecule aminoProduct =
        OrganicReactionPredictionServiceTest.ringMolecule("normalized_snar_amino", "N");
    final ReactionProductCandidate primary =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(ReactionParticipant.of(aminoProduct, StoichiometricCoefficient.ONE))),
            0.40,
            45.0,
            "leaving group directed amino product",
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
                    "SNAr chloride leaving group replacement on halogenated nitrogen heterocycle.",
                    0.18)));
    final ReactionProductCandidate side =
        OrganicReactionPredictionServiceTest.candidate(
            OrganicReactionPredictionServiceTest.ringMolecule("normalized_snar_side", "C"),
            0.08,
            "side product");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "normalized_snar_prediction",
            ReactionSide.of(
                List.of(ReactionParticipant.of(substrate, StoichiometricCoefficient.ONE))),
            ReactionConditions.EMPTY,
            List.of(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            List.of(primary, side),
            "prediction service should preserve mechanistic evidence");

    final ReactionPredictionResult result = service.predict(request);
    final ReactionPredictionExplanation explanation = explainer.explain(result);

    Assertions.assertEquals(1.0, result.probabilityMass(), 1.0E-12);
    Assertions.assertTrue(result.bestCandidate().hasSelectivityNote());
    Assertions.assertEquals(
        ReactionMechanismClass.NUCLEOPHILIC_AROMATIC_SUBSTITUTION,
        explanation.pathwayAssessment().mechanismClass());
    Assertions.assertEquals(
        ReactionRegioselectivityClass.LEAVING_GROUP_DIRECTED,
        explanation.pathwayAssessment().regioselectivityClass());
    Assertions.assertTrue(
        result.bestCandidate().evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR
                        && evidence.description().contains("SNAr")));
  }

  @Test
  void domainConditionAdvisorExplainsCrossCouplingSetupCompleteness() {
    final OrganicReactionDomainConditionAdvisor advisor =
        new OrganicReactionDomainConditionAdvisor(new MolecularFeatureAnalyzer());
    final Molecule substrate = OrganicReactionPredictionServiceTest.dichloroHeterocycle();
    final ReactionPredictionRequest completeRequest =
        ReactionPredictionRequest.of(
            "complete_cross_coupling",
            ReactionSide.of(
                List.of(ReactionParticipant.of(substrate, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(85.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(16.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.named(ReactionComponentRole.SOLVENT, "DMSO"),
                    ReactionConditionComponent.named(ReactionComponentRole.CATALYST, "Pd(PPh3)4"),
                    ReactionConditionComponent.named(ReactionComponentRole.BASE, "K2CO3")),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.INERT_ATMOSPHERE)),
                "complete cross-coupling setup"),
            List.of(OrganicReactionDomain.CROSS_COUPLING),
            List.of(),
            null);
    final ReactionPredictionRequest incompleteRequest =
        ReactionPredictionRequest.of(
            "incomplete_cross_coupling",
            completeRequest.reactants(),
            ReactionConditions.of(
                Temperature.of(25.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(2.0, TimeDurationUnit.HOUR),
                List.of(),
                List.of(),
                "missing cross-coupling controls"),
            completeRequest.domains(),
            List.of(),
            null);

    final List<ReactionPredictionEvidence> completeEvidence = advisor.advise(completeRequest);
    final List<ReactionPredictionEvidence> incompleteEvidence = advisor.advise(incompleteRequest);

    Assertions.assertTrue(
        completeEvidence.stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.CATALYST_EFFECT
                        && evidence.weight() > 0.0));
    Assertions.assertTrue(
        completeEvidence.stream()
            .anyMatch(
                evidence ->
                    evidence.description().contains("halide-like handle")
                        && evidence.weight() > 0.0));
    Assertions.assertTrue(
        incompleteEvidence.stream()
            .anyMatch(
                evidence ->
                    evidence.description().contains("no explicit catalyst")
                        && evidence.weight() < 0.0));
    Assertions.assertTrue(
        incompleteEvidence.stream()
            .anyMatch(
                evidence ->
                    evidence.description().contains("no base component")
                        && evidence.weight() < 0.0));
  }

  @Test
  void predictionAuditIncludesDomainSpecificConditionEvidence() {
    final OrganicReactionPredictionService service = new OrganicReactionPredictionService();
    final Molecule substrate = OrganicReactionPredictionServiceTest.dichloroHeterocycle();
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "domain_condition_audit",
            ReactionSide.of(
                List.of(ReactionParticipant.of(substrate, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(60.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(4.0, TimeDurationUnit.HOUR),
                List.of(),
                List.of(ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING)),
                "cross-coupling without catalyst and base"),
            List.of(
                OrganicReactionDomain.CROSS_COUPLING,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            List.of(
                OrganicReactionPredictionServiceTest.candidate(substrate, 0.4, "audit candidate")),
            null);

    final ReactionPredictionResult result = service.predict(request);

    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasDiagnosticContaining(
            result, "Cross-coupling request has no explicit catalyst"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasDiagnosticContaining(
            result, "Cross-coupling request has no base component"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasDiagnosticContaining(
            result, "Nucleophilic substitution request has a leaving-group"));
  }

  @Test
  void siteSelectivityFavorsElectronPoorHeteroarylLeavingGroupWithoutBenchmarkLookup() {
    final ReactionSiteSelectivityAnalyzer analyzer = new ReactionSiteSelectivityAnalyzer();
    final Molecule substrate = OrganicReactionPredictionServiceTest.selectivityProbeHeterocycle();
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "site_selectivity_probe",
            ReactionSide.of(
                List.of(ReactionParticipant.of(substrate, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(120.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(3.0, TimeDurationUnit.HOUR),
                List.of(ReactionConditionComponent.named(ReactionComponentRole.BASE, "amine")),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM)),
                "heated basic heteroaryl substitution probe"),
            List.of(
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION,
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(),
            null);
    final ReactionSiteSelectivityAssessment activated =
        analyzer.assess(
            request,
            substrate,
            MolecularTransformProduct.of(
                OrganicReactionPredictionServiceTest.ringMolecule("activated_site_product", "N"),
                AtomId.of("a2"),
                AtomId.of("cl2")),
            0.20,
            35.0);
    final ReactionSiteSelectivityAssessment remote =
        analyzer.assess(
            request,
            substrate,
            MolecularTransformProduct.of(
                OrganicReactionPredictionServiceTest.ringMolecule("remote_site_product", "N"),
                AtomId.of("a4"),
                AtomId.of("cl4")),
            0.20,
            35.0);

    Assertions.assertTrue(activated.probability() > remote.probability());
    Assertions.assertTrue(activated.expectedYieldPercent() > remote.expectedYieldPercent());
    Assertions.assertTrue(
        activated.evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.description().contains("electron-withdrawing")
                        || evidence.description().contains("Electron-poor heteroaryl")));
  }

  @Test
  void scorerRecognizesCombesQuinolineAnnulationAsNamedMechanisticContext() {
    final OrganicReactionCandidateScorer scorer =
        new OrganicReactionCandidateScorer(new MolecularFeatureAnalyzer());
    final Molecule reactant =
        OrganicReactionPredictionServiceTest.ringMolecule("combes_aniline_probe", "C");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "combes_named_context",
            ReactionSide.of(
                List.of(ReactionParticipant.of(reactant, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(110.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(4.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "trifluoromethyl beta-diketone"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.CATALYST,
                        "polyphosphoric acid PPA Combes annulation")),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.ACIDIC_MEDIUM),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.DEHYDRATING_AGENT),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING)),
                "Combes quinoline annulation probe"),
            List.of(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS),
            List.of(),
            null);
    final ReactionProductCandidate candidate =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        OrganicReactionPredictionServiceTest.ringMolecule(
                            "combes_quinoline_product", "N"),
                        StoichiometricCoefficient.ONE))),
            0.22,
            42.0,
            "Combes quinoline candidate",
            List.of(),
            List.of(
                PredictedReactionMechanism.of(
                    "combes_route",
                    ReactionMechanismFamily.COMBES_QUINOLINE_ANNULATION,
                    0.62,
                    42.0)));

    final ReactionCandidateScore score = scorer.score(request, candidate, 0);

    Assertions.assertTrue(score.score() > candidate.probability());
    Assertions.assertTrue(
        score.evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.description().contains("COMBES_QUINOLINE_ANNULATION")
                        && evidence.description().contains("explicitly aligned")));
  }

  @Test
  void calibrationScorerRequiresMatchingConditionsAndMechanismChannel() {
    final Molecule pyridine =
        OrganicReactionPredictionServiceTest.ringMolecule("calibration_pyridine", "N");
    final ReactionProductCandidate candidate =
        OrganicReactionPredictionServiceTest.candidate(pyridine, 0.4, "calibrated")
            .withPredictedMechanisms(
                List.of(
                    PredictedReactionMechanism.of(
                        "calibrated_radical_substitution",
                        ReactionMechanismFamily.RADICAL_SUBSTITUTION,
                        0.4,
                        40.0)));
    final ReactionOutcomeCalibrationLibrary library =
        ReactionOutcomeCalibrationLibrary.of(
            List.of(
                ReactionOutcomeCalibrationObservation.of(
                    "minisci_channel",
                    List.of(
                        OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                        OrganicReactionDomain.RADICAL_FUNCTIONALIZATION),
                    ReactionMechanismFamily.RADICAL_SUBSTITUTION,
                    List.of(
                        ReactionConditionFeatureKind.ACIDIC_MEDIUM,
                        ReactionConditionFeatureKind.PHOTOCHEMICAL),
                    ReactionOutcomeCalibrationScorer.productFormulaSignature(candidate),
                    "c2_radical_regioisomer",
                    0.62,
                    55.0,
                    0.80,
                    "synthetic calibration row",
                    "Radical heterocycle channel with matching photochemical acidic conditions.")));
    final ReactionOutcomeCalibrationScorer scorer = new ReactionOutcomeCalibrationScorer(library);
    final java.util.ArrayList<ReactionPredictionEvidence> evidence =
        new java.util.ArrayList<ReactionPredictionEvidence>();

    scorer.recordBenchmarkEvidence(
        OrganicReactionPredictionServiceTest.request(
            "calibration_match",
            pyridine,
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.RADICAL_FUNCTIONALIZATION),
            List.of(
                ReactionConditionFeatureKind.ACIDIC_MEDIUM,
                ReactionConditionFeatureKind.PHOTOCHEMICAL)),
        candidate,
        evidence);

    Assertions.assertTrue(
        evidence.stream()
            .anyMatch(item -> item.description().contains("channel=c2_radical_regioisomer")));
    Assertions.assertTrue(
        evidence.stream()
            .filter(item -> item.kind() == ReactionPredictionEvidenceKind.CALIBRATION_PRIOR)
            .allMatch(item -> item.weight() == 0.0));
    Assertions.assertEquals(
        0.0,
        scorer.score(
            OrganicReactionPredictionServiceTest.request(
                "calibration_missing_condition",
                pyridine,
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                    OrganicReactionDomain.RADICAL_FUNCTIONALIZATION),
                List.of(ReactionConditionFeatureKind.ACIDIC_MEDIUM)),
            candidate,
            new java.util.ArrayList<ReactionPredictionEvidence>()));
  }

  @Test
  void benchmarkEvaluatorMeasuresPredictionErrorWithoutFeedingPrediction() {
    final Molecule pyridine =
        OrganicReactionPredictionServiceTest.ringMolecule("benchmark_evaluator_pyridine", "N");
    final ReactionProductCandidate candidate =
        OrganicReactionPredictionServiceTest.candidate(pyridine, 0.50, "2-cf3 quinoline channel");
    final String signature = ReactionOutcomeCalibrationScorer.productFormulaSignature(candidate);
    final ReactionPredictionRequest request =
        OrganicReactionPredictionServiceTest.request(
            "benchmark_error_measurement",
            pyridine,
            List.of(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS),
            List.of(ReactionConditionFeatureKind.HEATING));
    final ReactionPredictionResult prediction =
        ReactionPredictionResult.of(
            request,
            ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionMethodType
                .HYBRID,
            List.of(candidate),
            List.of());
    final ReactionOutcomeCalibrationLibrary benchmarks =
        ReactionOutcomeCalibrationLibrary.of(
            List.of(
                ReactionOutcomeCalibrationObservation.of(
                    "matched_2cf3_row",
                    List.of(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS),
                    ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION,
                    List.of(ReactionConditionFeatureKind.HEATING),
                    signature,
                    "2cf3",
                    ReactionRegioselectivityClass.COMPETING_REGIOISOMERS,
                    List.of("quinoline"),
                    List.of("2-cf3"),
                    0.60,
                    60.0,
                    0.95,
                    ReactionOutcomeSourceQuality.EXACT_EXPERIMENTAL_PRODUCT_DISTRIBUTION,
                    "doi:10.example/exact",
                    "table_1_entry_1",
                    true,
                    "Synthetic exact benchmark row.",
                    "Matched row must measure error only."),
                ReactionOutcomeCalibrationObservation.of(
                    "unmatched_4cf3_row",
                    List.of(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS),
                    ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION,
                    List.of(ReactionConditionFeatureKind.HEATING),
                    signature,
                    "4cf3",
                    ReactionRegioselectivityClass.COMPETING_REGIOISOMERS,
                    List.of("quinoline"),
                    List.of("4-cf3"),
                    0.40,
                    40.0,
                    0.95,
                    ReactionOutcomeSourceQuality.EXACT_EXPERIMENTAL_PRODUCT_DISTRIBUTION,
                    "doi:10.example/exact",
                    "table_1_entry_1",
                    true,
                    "Synthetic exact benchmark row.",
                    "Unmatched row must stay unmatched when channel hints do not exist.")));

    final ReactionOutcomeBenchmarkReport report =
        new ReactionOutcomeBenchmarkEvaluator().evaluate(prediction, benchmarks);

    Assertions.assertEquals(2, report.exactObservationCount());
    Assertions.assertEquals(1, report.comparableObservationCount());
    Assertions.assertEquals(1, report.unmatchedObservationCount());
    Assertions.assertEquals(0.25, report.meanAbsoluteError(), 1.0E-12);
    Assertions.assertEquals(0.22, report.weightedMeanAbsoluteError(), 1.0E-12);
  }

  @Test
  void calibrationScorerSkipsMismatchedMechanismFamily() {
    final Molecule pyridine =
        OrganicReactionPredictionServiceTest.ringMolecule("calibration_mismatch_pyridine", "N");
    final ReactionProductCandidate candidate =
        OrganicReactionPredictionServiceTest.candidate(pyridine, 0.4, "mismatched")
            .withPredictedMechanisms(
                List.of(
                    PredictedReactionMechanism.of(
                        "mismatched_snar",
                        ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION,
                        0.4,
                        40.0)));
    final ReactionOutcomeCalibrationLibrary library =
        ReactionOutcomeCalibrationLibrary.of(
            List.of(
                ReactionOutcomeCalibrationObservation.of(
                    "radical_only_channel",
                    List.of(OrganicReactionDomain.RADICAL_FUNCTIONALIZATION),
                    ReactionMechanismFamily.RADICAL_SUBSTITUTION,
                    List.of(ReactionConditionFeatureKind.PHOTOCHEMICAL),
                    ReactionOutcomeCalibrationScorer.productFormulaSignature(candidate),
                    "radical_channel",
                    0.6,
                    50.0,
                    0.8,
                    "synthetic calibration row",
                    "Should not match an SNAr candidate.")));

    final double score =
        new ReactionOutcomeCalibrationScorer(library)
            .score(
                OrganicReactionPredictionServiceTest.request(
                    "calibration_mechanism_mismatch",
                    pyridine,
                    List.of(OrganicReactionDomain.RADICAL_FUNCTIONALIZATION),
                    List.of(ReactionConditionFeatureKind.PHOTOCHEMICAL)),
                candidate,
                new java.util.ArrayList<ReactionPredictionEvidence>());

    Assertions.assertEquals(0.0, score);
  }

  @Test
  void calibrationScorerUsesSourceQualityAndChannelHints() {
    final Molecule pyridine =
        OrganicReactionPredictionServiceTest.ringMolecule("calibration_channel_pyridine", "N");
    final ReactionProductCandidate candidate =
        OrganicReactionPredictionServiceTest.candidate(
                pyridine, 0.4, "C4 blocking-group regioselective Minisci product")
            .withPredictedMechanisms(
                List.of(
                    PredictedReactionMechanism.of(
                        "c4_radical_substitution",
                        ReactionMechanismFamily.RADICAL_SUBSTITUTION,
                        0.4,
                        40.0)));
    final String signature = ReactionOutcomeCalibrationScorer.productFormulaSignature(candidate);
    final ReactionOutcomeCalibrationLibrary library =
        ReactionOutcomeCalibrationLibrary.of(
            List.of(
                ReactionOutcomeCalibrationObservation.of(
                    "exact_c4_channel",
                    List.of(
                        OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                        OrganicReactionDomain.RADICAL_FUNCTIONALIZATION),
                    ReactionMechanismFamily.RADICAL_SUBSTITUTION,
                    List.of(ReactionConditionFeatureKind.ACIDIC_MEDIUM),
                    signature,
                    "c4_blocked_channel",
                    ReactionRegioselectivityClass.STERICALLY_CONTROLLED,
                    List.of("pyridine", "pyridinium", "blocking_group"),
                    List.of("c4", "blocking-group"),
                    0.64,
                    61.0,
                    0.90,
                    ReactionOutcomeSourceQuality.EXACT_EXPERIMENTAL_PRODUCT_DISTRIBUTION,
                    "synthetic exact distribution row",
                    "Exact row should match the candidate channel."),
                ReactionOutcomeCalibrationObservation.of(
                    "wrong_channel_same_formula",
                    List.of(
                        OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                        OrganicReactionDomain.RADICAL_FUNCTIONALIZATION),
                    ReactionMechanismFamily.RADICAL_SUBSTITUTION,
                    List.of(ReactionConditionFeatureKind.ACIDIC_MEDIUM),
                    signature,
                    "c2_c6_competing_channel",
                    ReactionRegioselectivityClass.COMPETING_REGIOISOMERS,
                    List.of("pyridine", "pyridinium"),
                    List.of("c2", "c6"),
                    0.50,
                    50.0,
                    0.80,
                    ReactionOutcomeSourceQuality.EXPERIMENTAL_YIELD_TABLE,
                    "synthetic competing distribution row",
                    "Same formula must not match when the channel hint is incompatible.")));
    final java.util.ArrayList<ReactionPredictionEvidence> evidence =
        new java.util.ArrayList<ReactionPredictionEvidence>();

    final double score =
        new ReactionOutcomeCalibrationScorer(library)
            .score(
                OrganicReactionPredictionServiceTest.request(
                    "calibration_channel_hints",
                    pyridine,
                    List.of(
                        OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                        OrganicReactionDomain.RADICAL_FUNCTIONALIZATION),
                    List.of(ReactionConditionFeatureKind.ACIDIC_MEDIUM)),
                candidate,
                evidence);

    Assertions.assertEquals(0.0, score, 1.0E-12);
    Assertions.assertTrue(
        evidence.stream()
            .anyMatch(
                item ->
                    item.description().contains("exact_c4_channel")
                        && item.description()
                            .contains("quality=EXACT_EXPERIMENTAL_PRODUCT_DISTRIBUTION")));
    Assertions.assertFalse(
        evidence.stream()
            .anyMatch(item -> item.description().contains("wrong_channel_same_formula")));
    Assertions.assertEquals(
        0.0,
        new ReactionOutcomeCalibrationScorer(library)
            .score(
                OrganicReactionPredictionServiceTest.request(
                    "calibration_channel_hydrocarbon",
                    OrganicReactionPredictionServiceTest.ringMolecule("benzene_like_screen", "C"),
                    List.of(
                        OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                        OrganicReactionDomain.RADICAL_FUNCTIONALIZATION),
                    List.of(ReactionConditionFeatureKind.ACIDIC_MEDIUM)),
                candidate,
                new java.util.ArrayList<ReactionPredictionEvidence>()));
  }

  @Test
  void calibrationScorerScalesTransferabilityByEvidenceSpecificity() {
    final Molecule pyridine =
        OrganicReactionPredictionServiceTest.ringMolecule("specificity_calibration_pyridine", "N");
    final ReactionProductCandidate candidate =
        OrganicReactionPredictionServiceTest.candidate(
                pyridine, 0.4, "C4 blocking-group pyridinium radical product")
            .withPredictedMechanisms(
                List.of(
                    PredictedReactionMechanism.of(
                        "specificity_radical_substitution",
                        ReactionMechanismFamily.RADICAL_SUBSTITUTION,
                        0.4,
                        40.0)));
    final String signature = ReactionOutcomeCalibrationScorer.productFormulaSignature(candidate);
    final ReactionOutcomeCalibrationLibrary library =
        ReactionOutcomeCalibrationLibrary.of(
            List.of(
                ReactionOutcomeCalibrationObservation.of(
                    "specific_exact_sparse_hint_match",
                    List.of(
                        OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                        OrganicReactionDomain.RADICAL_FUNCTIONALIZATION),
                    ReactionMechanismFamily.RADICAL_SUBSTITUTION,
                    List.of(ReactionConditionFeatureKind.ACIDIC_MEDIUM),
                    signature,
                    "specific_c4_channel",
                    ReactionRegioselectivityClass.STERICALLY_CONTROLLED,
                    List.of("pyridine", "pyridinium", "silver_salt", "blocking_group"),
                    List.of("c4", "blocking-group"),
                    0.64,
                    61.0,
                    0.90,
                    ReactionOutcomeSourceQuality.EXACT_EXPERIMENTAL_PRODUCT_DISTRIBUTION,
                    "synthetic exact distribution row",
                    "Exact row should be weakened when only part of the structural context is present."),
                ReactionOutcomeCalibrationObservation.of(
                    "specific_review_full_hint_match",
                    List.of(
                        OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                        OrganicReactionDomain.RADICAL_FUNCTIONALIZATION),
                    ReactionMechanismFamily.RADICAL_SUBSTITUTION,
                    List.of(ReactionConditionFeatureKind.ACIDIC_MEDIUM),
                    signature,
                    "review_c4_channel",
                    ReactionRegioselectivityClass.COMPETING_REGIOISOMERS,
                    List.of("pyridine"),
                    List.of("c4"),
                    0.64,
                    61.0,
                    0.90,
                    ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
                    "synthetic broad review row",
                    "Broad review prior should stay transferable but weaker than exact data.")));
    final java.util.ArrayList<ReactionPredictionEvidence> evidence =
        new java.util.ArrayList<ReactionPredictionEvidence>();

    final double score =
        new ReactionOutcomeCalibrationScorer(library)
            .score(
                OrganicReactionPredictionServiceTest.request(
                    "specificity_calibration_match",
                    pyridine,
                    List.of(
                        OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                        OrganicReactionDomain.RADICAL_FUNCTIONALIZATION),
                    List.of(ReactionConditionFeatureKind.ACIDIC_MEDIUM)),
                candidate,
                evidence);

    Assertions.assertEquals(0.0, score, 1.0E-12);
    final ReactionPredictionEvidence exact =
        OrganicReactionPredictionServiceTest.evidenceContaining(
            evidence, "specific_exact_sparse_hint_match");
    final ReactionPredictionEvidence review =
        OrganicReactionPredictionServiceTest.evidenceContaining(
            evidence, "specific_review_full_hint_match");
    Assertions.assertEquals(0.0, exact.weight());
    Assertions.assertEquals(0.0, review.weight());
    Assertions.assertTrue(exact.description().contains("auditSimilarityScore="));
    Assertions.assertTrue(review.description().contains("auditSimilarityScore="));
    Assertions.assertTrue(exact.description().contains("substrateCompleteness=0.25"));
    Assertions.assertTrue(review.description().contains("transferability="));
  }

  @Test
  void calibrationScorerPenalizesContradictoryReactionConditionContext() {
    final Molecule pyridine =
        OrganicReactionPredictionServiceTest.ringMolecule(
            "condition_conflict_calibration_pyridine", "N");
    final ReactionProductCandidate candidate =
        OrganicReactionPredictionServiceTest.candidate(
                pyridine, 0.4, "C4 pyridinium acidic radical product")
            .withPredictedMechanisms(
                List.of(
                    PredictedReactionMechanism.of(
                        "condition_conflict_radical_substitution",
                        ReactionMechanismFamily.RADICAL_SUBSTITUTION,
                        0.4,
                        40.0)));
    final ReactionOutcomeCalibrationLibrary library =
        ReactionOutcomeCalibrationLibrary.of(
            List.of(
                ReactionOutcomeCalibrationObservation.of(
                    "acidic_radical_c4_channel",
                    List.of(
                        OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                        OrganicReactionDomain.RADICAL_FUNCTIONALIZATION),
                    ReactionMechanismFamily.RADICAL_SUBSTITUTION,
                    List.of(ReactionConditionFeatureKind.ACIDIC_MEDIUM),
                    ReactionOutcomeCalibrationScorer.productFormulaSignature(candidate),
                    "acidic_c4_channel",
                    ReactionRegioselectivityClass.CONDITION_CONTROLLED,
                    List.of("pyridine"),
                    List.of("c4"),
                    0.70,
                    68.0,
                    0.90,
                    ReactionOutcomeSourceQuality.EXACT_EXPERIMENTAL_PRODUCT_DISTRIBUTION,
                    "synthetic acidic distribution row",
                    "Acidic calibration should be weakened by contradictory base context.")));
    final java.util.ArrayList<ReactionPredictionEvidence> acidicEvidence =
        new java.util.ArrayList<ReactionPredictionEvidence>();
    final double acidicOnly =
        new ReactionOutcomeCalibrationScorer(library)
            .score(
                OrganicReactionPredictionServiceTest.request(
                    "condition_conflict_acidic_only",
                    pyridine,
                    List.of(
                        OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                        OrganicReactionDomain.RADICAL_FUNCTIONALIZATION),
                    List.of(ReactionConditionFeatureKind.ACIDIC_MEDIUM)),
                candidate,
                acidicEvidence);
    final java.util.ArrayList<ReactionPredictionEvidence> contradictoryEvidence =
        new java.util.ArrayList<ReactionPredictionEvidence>();
    final double contradictory =
        new ReactionOutcomeCalibrationScorer(library)
            .score(
                OrganicReactionPredictionServiceTest.request(
                    "condition_conflict_acidic_and_basic",
                    pyridine,
                    List.of(
                        OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                        OrganicReactionDomain.RADICAL_FUNCTIONALIZATION),
                    List.of(
                        ReactionConditionFeatureKind.ACIDIC_MEDIUM,
                        ReactionConditionFeatureKind.BASIC_MEDIUM)),
                candidate,
                contradictoryEvidence);

    Assertions.assertEquals(0.0, acidicOnly, 1.0E-12);
    Assertions.assertEquals(0.0, contradictory, 1.0E-12);
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.evidenceContaining(
                contradictoryEvidence, "acidic_radical_c4_channel")
            .description()
            .contains("conditionConflictMultiplier=0.55"));
  }

  @Test
  void calibrationLibraryFlattensDistributionDatasetsAndPreservesProbabilityMass() {
    final Molecule pyridine =
        OrganicReactionPredictionServiceTest.ringMolecule("dataset_calibration_pyridine", "N");
    final ReactionProductCandidate candidate =
        OrganicReactionPredictionServiceTest.candidate(
                pyridine, 0.4, "C4 regioselective pyridinium radical channel")
            .withPredictedMechanisms(
                List.of(
                    PredictedReactionMechanism.of(
                        "dataset_radical_substitution",
                        ReactionMechanismFamily.RADICAL_SUBSTITUTION,
                        0.4,
                        40.0)));
    final ReactionOutcomeCalibrationObservation c4Channel =
        ReactionOutcomeCalibrationObservation.of(
            "dataset_c4_channel",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.RADICAL_FUNCTIONALIZATION),
            ReactionMechanismFamily.RADICAL_SUBSTITUTION,
            List.of(ReactionConditionFeatureKind.ACIDIC_MEDIUM),
            ReactionOutcomeCalibrationScorer.productFormulaSignature(candidate),
            "dataset_c4_product",
            ReactionRegioselectivityClass.STERICALLY_CONTROLLED,
            List.of("pyridine", "pyridinium"),
            List.of("c4", "regioselective"),
            0.61,
            58.0,
            0.88,
            ReactionOutcomeSourceQuality.EXACT_EXPERIMENTAL_PRODUCT_DISTRIBUTION,
            "synthetic dataset row",
            "Dataset-backed channel should be flattened into the scorer.");
    final ReactionOutcomeDistributionDataset dataset =
        ReactionOutcomeDistributionDataset.of(
            "dataset_pyridinium_regioselectivity",
            "Synthetic pyridinium regioselectivity distribution",
            "synthetic exact distribution",
            ReactionOutcomeSourceQuality.EXACT_EXPERIMENTAL_PRODUCT_DISTRIBUTION,
            List.of(c4Channel));
    final ReactionOutcomeCalibrationLibrary library =
        ReactionOutcomeCalibrationLibrary.ofDatasets(List.of(dataset));
    final java.util.ArrayList<ReactionPredictionEvidence> evidence =
        new java.util.ArrayList<ReactionPredictionEvidence>();

    final double score =
        new ReactionOutcomeCalibrationScorer(library)
            .score(
                OrganicReactionPredictionServiceTest.request(
                    "dataset_calibration_match",
                    pyridine,
                    List.of(
                        OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                        OrganicReactionDomain.RADICAL_FUNCTIONALIZATION),
                    List.of(ReactionConditionFeatureKind.ACIDIC_MEDIUM)),
                candidate,
                evidence);

    Assertions.assertEquals(1, library.datasets().size());
    Assertions.assertEquals(1, library.observations().size());
    Assertions.assertEquals(0.61, dataset.observedProbabilityMass(), 1.0E-12);
    Assertions.assertEquals(0.39, dataset.unassignedProbabilityMass(), 1.0E-12);
    Assertions.assertTrue(dataset.hasUnassignedProbabilityMass(0.01));
    Assertions.assertEquals(0.0, score, 1.0E-12);
    Assertions.assertTrue(
        evidence.stream()
            .anyMatch(
                item ->
                    item.kind() == ReactionPredictionEvidenceKind.CALIBRATION_PRIOR
                        && item.description().contains("dataset_c4_channel")));
  }

  @Test
  void calibrationLibraryRejectsAmbiguousDuplicateObservationAndDatasetIds() {
    final ReactionOutcomeCalibrationObservation observation =
        ReactionOutcomeCalibrationObservation.of(
            "duplicate_calibration_row",
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            "C5H5N",
            0.5,
            50.0,
            "synthetic row",
            "synthetic note");
    final ReactionOutcomeDistributionDataset dataset =
        ReactionOutcomeDistributionDataset.of(
            "duplicate_dataset",
            "duplicate dataset",
            "synthetic dataset",
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            List.of(observation));

    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> ReactionOutcomeCalibrationLibrary.of(List.of(observation), List.of(dataset)));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> ReactionOutcomeCalibrationLibrary.of(List.of(), List.of(dataset, dataset)));
  }

  @Test
  void builtInCalibrationContainsPhotochemicalHeterocyclePopulationChannel() {
    final ReactionOutcomeCalibrationLibrary library =
        BuiltInReactionOutcomeCalibrationLibraries.heterocycleProductDistributions();
    final Molecule thiophenoneIsomer =
        Molecule.of(
            MoleculeId.of("thiophenone_isomer"),
            "thiophenone_isomer",
            List.of(
                OrganicReactionPredictionServiceTest.atom("a0", "C"),
                OrganicReactionPredictionServiceTest.atom("a1", "C"),
                OrganicReactionPredictionServiceTest.atom("a2", "C"),
                OrganicReactionPredictionServiceTest.atom("a3", "C"),
                OrganicReactionPredictionServiceTest.atom("a4", "O"),
                OrganicReactionPredictionServiceTest.atom("a5", "S"),
                OrganicReactionPredictionServiceTest.atom("a6", "H"),
                OrganicReactionPredictionServiceTest.atom("a7", "H"),
                OrganicReactionPredictionServiceTest.atom("a8", "H"),
                OrganicReactionPredictionServiceTest.atom("a9", "H")),
            List.of(
                Bond.of(AtomId.of("a0"), AtomId.of("a1"), BondType.SINGLE),
                Bond.of(AtomId.of("a1"), AtomId.of("a2"), BondType.DOUBLE),
                Bond.of(AtomId.of("a2"), AtomId.of("a3"), BondType.SINGLE),
                Bond.of(AtomId.of("a3"), AtomId.of("a5"), BondType.SINGLE),
                Bond.of(AtomId.of("a5"), AtomId.of("a0"), BondType.SINGLE),
                Bond.of(AtomId.of("a3"), AtomId.of("a4"), BondType.DOUBLE)),
            MolecularCharge.NEUTRAL,
            SpinMultiplicity.SINGLET);
    final ReactionProductCandidate candidate =
        OrganicReactionPredictionServiceTest.candidate(
                thiophenoneIsomer, 0.25, "photochemical isomer")
            .withPredictedMechanisms(
                List.of(
                    PredictedReactionMechanism.of(
                        "photochemical_isomer_population",
                        ReactionMechanismFamily.PHOTOCHEMICAL,
                        0.5,
                        50.0)));
    final ReactionOutcomeCalibrationScorer scorer = new ReactionOutcomeCalibrationScorer(library);
    final java.util.ArrayList<ReactionPredictionEvidence> evidence =
        new java.util.ArrayList<ReactionPredictionEvidence>();

    final double score =
        scorer.score(
            OrganicReactionPredictionServiceTest.request(
                "photochemical_population_match",
                thiophenoneIsomer,
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                    OrganicReactionDomain.REARRANGEMENT),
                List.of(ReactionConditionFeatureKind.PHOTOCHEMICAL)),
            candidate,
            evidence);

    Assertions.assertTrue(library.datasets().size() >= 164);
    Assertions.assertTrue(
        library.datasets().stream()
            .anyMatch(
                dataset ->
                    dataset
                        .id()
                        .equals("pyridinium_radical_regioselectivity_distribution_priors")));
    Assertions.assertTrue(
        library.datasets().stream()
            .anyMatch(
                dataset ->
                    dataset.id().equals("pyridinium_nucleophilic_addition_regioselectivity_priors")
                        && Math.abs(dataset.observedProbabilityMass() - 1.0) < 1.0E-12));
    Assertions.assertTrue(
        library.datasets().stream()
            .anyMatch(
                dataset ->
                    dataset.id().equals("pyridinium_amine_ring_opening_distribution_priors")
                        && Math.abs(dataset.observedProbabilityMass() - 1.0) < 1.0E-12));
    Assertions.assertTrue(
        library.datasets().stream()
            .anyMatch(
                dataset ->
                    dataset.id().equals("dipolar_heterocycle_construction_distribution_priors")));
    Assertions.assertTrue(
        library.datasets().stream()
            .anyMatch(
                dataset ->
                    dataset.id().equals("modified_combes_quinoline_regioisomer_distribution_priors")
                        && Math.abs(dataset.observedProbabilityMass() - 1.0) < 1.0E-12));
    Assertions.assertTrue(
        library.datasets().stream()
            .anyMatch(
                dataset ->
                    dataset.id().equals("soai_pyrimidyl_alcohol_enantiomer_distribution_priors")
                        && Math.abs(dataset.observedProbabilityMass() - 1.0) < 1.0E-12));
    Assertions.assertTrue(
        library.datasets().stream()
            .anyMatch(
                dataset ->
                    dataset.id().equals("paal_knorr_knorr_heterocycle_distribution_priors")
                        && Math.abs(dataset.observedProbabilityMass() - 1.0) < 1.0E-12));
    Assertions.assertTrue(
        library.datasets().stream()
            .anyMatch(
                dataset ->
                    dataset.id().equals("larock_indole_heteroannulation_distribution_priors")));
    Assertions.assertTrue(
        library.datasets().stream()
            .anyMatch(
                dataset ->
                    dataset.id().equals("hurd_mori_thiadiazole_regioisomer_distribution_priors")));
    Assertions.assertTrue(
        library.datasets().stream()
            .anyMatch(
                dataset ->
                    dataset.id().equals("larock_indole_heteroannulation_distribution_priors")
                        && Math.abs(dataset.observedProbabilityMass() - 1.0) < 1.0E-12));
    Assertions.assertTrue(
        library.datasets().stream()
            .anyMatch(
                dataset ->
                    dataset.id().equals("hurd_mori_thiadiazole_regioisomer_distribution_priors")
                        && Math.abs(dataset.observedProbabilityMass() - 1.0) < 1.0E-12));
    Assertions.assertTrue(
        library.datasets().stream()
            .anyMatch(
                dataset ->
                    dataset.id().equals("fischer_indole_rearrangement_distribution_priors")
                        && Math.abs(dataset.observedProbabilityMass() - 1.0) < 1.0E-12));
    Assertions.assertTrue(
        library.datasets().stream()
            .anyMatch(
                dataset ->
                    dataset.id().equals("biginelli_dihydropyrimidinone_distribution_priors")
                        && Math.abs(dataset.observedProbabilityMass() - 1.0) < 1.0E-12));
    Assertions.assertTrue(
        library.datasets().stream()
            .anyMatch(
                dataset ->
                    dataset.id().equals("pictet_spengler_beta_carboline_distribution_priors")
                        && Math.abs(dataset.observedProbabilityMass() - 1.0) < 1.0E-12));
    Assertions.assertTrue(
        library.datasets().stream()
            .anyMatch(
                dataset ->
                    dataset.id().equals("friedlaender_quinoline_regioisomer_distribution_priors")
                        && Math.abs(dataset.observedProbabilityMass() - 1.0) < 1.0E-12));
    Assertions.assertTrue(
        library.datasets().stream()
            .anyMatch(
                dataset ->
                    dataset.id().equals("chichibabin_pyridine_amination_distribution_priors")
                        && Math.abs(dataset.observedProbabilityMass() - 1.0) < 1.0E-12));
    Assertions.assertTrue(
        library.datasets().stream()
            .anyMatch(
                dataset ->
                    dataset.id().equals("gewald_aminothiophene_distribution_priors")
                        && Math.abs(dataset.observedProbabilityMass() - 1.0) < 1.0E-12));
    Assertions.assertTrue(
        library.datasets().stream()
            .anyMatch(
                dataset ->
                    dataset.id().equals("indole_c3_n_functionalization_distribution_priors")
                        && Math.abs(dataset.observedProbabilityMass() - 1.0) < 1.0E-12));
    Assertions.assertTrue(
        library.datasets().stream()
            .anyMatch(
                dataset ->
                    dataset.id().equals("diazine_triazine_snar_side_channel_distribution_priors")
                        && Math.abs(dataset.observedProbabilityMass() - 1.0) < 1.0E-12));
    Assertions.assertTrue(
        library.datasets().stream()
            .anyMatch(
                dataset ->
                    dataset
                            .id()
                            .equals("quinazolinone_quinazoline_construction_distribution_priors")
                        && Math.abs(dataset.observedProbabilityMass() - 1.0) < 1.0E-12));
    Assertions.assertTrue(
        library.datasets().stream()
            .anyMatch(
                dataset ->
                    dataset.id().equals("hetaryne_regioisomer_distribution_priors")
                        && Math.abs(dataset.observedProbabilityMass() - 1.0) < 1.0E-12));
    Assertions.assertTrue(
        library.datasets().stream()
            .anyMatch(
                dataset ->
                    dataset.id().equals("heteroaryl_c_h_activation_annulation_distribution_priors")
                        && Math.abs(dataset.observedProbabilityMass() - 1.0) < 1.0E-12));
    Assertions.assertTrue(
        library.datasets().stream()
            .anyMatch(
                dataset ->
                    dataset.id().equals("buchwald_hartwig_heteroaryl_amination_distribution_priors")
                        && Math.abs(dataset.observedProbabilityMass() - 1.0) < 1.0E-12));
    Assertions.assertTrue(
        library.datasets().stream()
            .anyMatch(
                dataset ->
                    dataset.id().equals("suzuki_heterobiaryl_coupling_distribution_priors")
                        && Math.abs(dataset.observedProbabilityMass() - 1.0) < 1.0E-12));
    Assertions.assertTrue(
        library.datasets().stream()
            .anyMatch(
                dataset ->
                    dataset.id().equals("heteroarene_n_oxide_functionalization_distribution_priors")
                        && Math.abs(dataset.observedProbabilityMass() - 1.0) < 1.0E-12));
    Assertions.assertTrue(
        library.datasets().stream()
            .anyMatch(dataset -> dataset.hasUnassignedProbabilityMass(0.01)));
    Assertions.assertEquals(0.0, score, 1.0E-12);
    Assertions.assertTrue(
        evidence.stream()
            .anyMatch(
                item ->
                    item.kind() == ReactionPredictionEvidenceKind.CALIBRATION_PRIOR
                        && item.description()
                            .contains("photochemical_heterocycle_episulfide_population")));
  }

  @Test
  void calibrationScorerAppliesBroaderIndoleAndHetarynePriorsOnlyWithChannelHints() {
    final Molecule indoleLikeProduct =
        Molecule.of(
            MoleculeId.of("indole_c3_electrophilic_major_product"),
            "indole c3 electrophilic major product",
            List.of(
                OrganicReactionPredictionServiceTest.atom("a0", "C"),
                OrganicReactionPredictionServiceTest.atom("a1", "N"),
                OrganicReactionPredictionServiceTest.atom("a2", "C"),
                OrganicReactionPredictionServiceTest.atom("a3", "H")),
            List.of(
                Bond.of(AtomId.of("a0"), AtomId.of("a1"), BondType.SINGLE),
                Bond.of(AtomId.of("a1"), AtomId.of("a2"), BondType.SINGLE),
                Bond.of(AtomId.of("a2"), AtomId.of("a0"), BondType.DOUBLE)),
            MolecularCharge.NEUTRAL,
            SpinMultiplicity.SINGLET);
    final Molecule genericProduct =
        Molecule.of(
            MoleculeId.of("generic_heterocycle_product"),
            "generic heterocycle product",
            indoleLikeProduct.atoms(),
            indoleLikeProduct.bonds(),
            MolecularCharge.NEUTRAL,
            SpinMultiplicity.SINGLET);
    final ReactionOutcomeCalibrationScorer scorer =
        new ReactionOutcomeCalibrationScorer(
            BuiltInReactionOutcomeCalibrationLibraries.heterocycleProductDistributions());
    final java.util.ArrayList<ReactionPredictionEvidence> matchedEvidence =
        new java.util.ArrayList<ReactionPredictionEvidence>();
    final java.util.ArrayList<ReactionPredictionEvidence> weakEvidence =
        new java.util.ArrayList<ReactionPredictionEvidence>();
    final ReactionProductCandidate matched =
        OrganicReactionPredictionServiceTest.candidate(
                indoleLikeProduct, 0.35, "indole c3 electrophilic major channel")
            .withPredictedMechanisms(
                List.of(
                    PredictedReactionMechanism.of(
                        "indole_c3_electrophilic",
                        ReactionMechanismFamily.ELECTROPHILIC_AROMATIC_SUBSTITUTION,
                        0.7,
                        70.0)));
    final ReactionProductCandidate weak =
        OrganicReactionPredictionServiceTest.candidate(
                genericProduct, 0.35, "generic heterocycle channel")
            .withPredictedMechanisms(
                List.of(
                    PredictedReactionMechanism.of(
                        "generic_electrophilic",
                        ReactionMechanismFamily.ELECTROPHILIC_AROMATIC_SUBSTITUTION,
                        0.7,
                        70.0)));
    final ReactionPredictionRequest request =
        OrganicReactionPredictionServiceTest.request(
            "indole_electrophilic_prior_match",
            indoleLikeProduct,
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.ELECTROPHILIC_SUBSTITUTION),
            List.of(ReactionConditionFeatureKind.ACIDIC_MEDIUM));

    final double matchedScore = scorer.score(request, matched, matchedEvidence);
    final double weakScore = scorer.score(request, weak, weakEvidence);

    Assertions.assertEquals(0.0, matchedScore, 1.0E-12);
    Assertions.assertEquals(0.0, weakScore);
    Assertions.assertTrue(
        matchedEvidence.stream()
            .anyMatch(
                item ->
                    item.description().contains("indole_c3_electrophilic_substitution_major_prior")
                        && item.description().contains("observedProbability=0.82")));
  }

  @Test
  void calibrationScorerRewardsTraceableExactProductDistributionRows() {
    final Molecule product =
        Molecule.of(
            MoleculeId.of("traceable_snar_major_product"),
            "traceable snar major product",
            List.of(
                OrganicReactionPredictionServiceTest.atom("a0", "C"),
                OrganicReactionPredictionServiceTest.atom("a1", "N"),
                OrganicReactionPredictionServiceTest.atom("a2", "H")),
            List.of(Bond.of(AtomId.of("a0"), AtomId.of("a1"), BondType.SINGLE)),
            MolecularCharge.NEUTRAL,
            SpinMultiplicity.SINGLET);
    final ReactionProductCandidate candidate =
        OrganicReactionPredictionServiceTest.candidate(product, 0.5, "traceable snar major")
            .withPredictedMechanisms(
                List.of(
                    PredictedReactionMechanism.of(
                        "traceable_snar",
                        ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION,
                        0.8,
                        80.0)));
    final ReactionPredictionRequest request =
        OrganicReactionPredictionServiceTest.request(
            "traceable_exact_calibration",
            product,
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(ReactionConditionFeatureKind.HEATING));
    final ReactionOutcomeCalibrationObservation unreferenced =
        ReactionOutcomeCalibrationObservation.of(
            "unreferenced_exact",
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION,
            List.of(ReactionConditionFeatureKind.HEATING),
            "snar_product_variable_formula",
            "traceable_major",
            ReactionRegioselectivityClass.LEAVING_GROUP_DIRECTED,
            List.of("traceable"),
            List.of("traceable", "snar", "major"),
            0.80,
            80.0,
            0.95,
            ReactionOutcomeSourceQuality.EXACT_EXPERIMENTAL_PRODUCT_DISTRIBUTION,
            "Unreferenced exact row used to verify traceability penalty.",
            "Exact quality without source metadata must not get full empirical weight.");
    final ReactionOutcomeCalibrationObservation traceable =
        ReactionOutcomeCalibrationObservation.of(
            "traceable_exact",
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION,
            List.of(ReactionConditionFeatureKind.HEATING),
            "snar_product_variable_formula",
            "traceable_major",
            ReactionRegioselectivityClass.LEAVING_GROUP_DIRECTED,
            List.of("traceable"),
            List.of("traceable", "snar", "major"),
            0.80,
            80.0,
            0.95,
            ReactionOutcomeSourceQuality.EXACT_EXPERIMENTAL_PRODUCT_DISTRIBUTION,
            "doi:10.example/traceable-snar",
            "table_1_entry_2",
            true,
            "Traceable exact row used to verify source metadata.",
            "Exact quality with source metadata gets full empirical weight.");
    final double unreferencedScore =
        new ReactionOutcomeCalibrationScorer(
                ReactionOutcomeCalibrationLibrary.of(List.of(unreferenced)))
            .score(request, candidate, new java.util.ArrayList<ReactionPredictionEvidence>());
    final java.util.ArrayList<ReactionPredictionEvidence> traceableEvidence =
        new java.util.ArrayList<ReactionPredictionEvidence>();
    final double traceableScore =
        new ReactionOutcomeCalibrationScorer(
                ReactionOutcomeCalibrationLibrary.of(List.of(traceable)))
            .score(request, candidate, traceableEvidence);

    Assertions.assertEquals(0.0, unreferencedScore, 1.0E-12);
    Assertions.assertEquals(0.0, traceableScore, 1.0E-12);
    Assertions.assertTrue(
        traceableEvidence.stream()
            .anyMatch(
                item ->
                    item.description().contains("sourceIdentifier=doi:10.example/traceable-snar")
                        && item.description().contains("tableReference=table_1_entry_2")
                        && item.description().contains("exactProductDistributionRow=true")));
  }

  @Test
  void calibrationScorerAppliesVariableFormulaHeterocycleClassPriorsWhenHintsMatch() {
    final ReactionOutcomeCalibrationScorer scorer =
        new ReactionOutcomeCalibrationScorer(
            BuiltInReactionOutcomeCalibrationLibraries.heterocycleProductDistributions());
    final Molecule reactant =
        OrganicReactionPredictionServiceTest.ringMolecule(
            "aniline_beta_diketone_trifluoromethyl_polyphosphoric_ester_substrate", "N");
    final Molecule product =
        OrganicReactionPredictionServiceTest.ringMolecule("two_cf3_quinoline_product", "N");
    final ReactionProductCandidate candidate =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(ReactionParticipant.of(product, StoichiometricCoefficient.ONE))),
            0.5,
            50.0,
            "Combes quinoline 2-CF3 regioisomer product channel.",
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.USER_RULE,
                    "combes quinoline 2-cf3 regioisomer candidate",
                    0.1)),
            List.of(
                PredictedReactionMechanism.of(
                    "modified_combes_quinoline",
                    ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION,
                    0.7,
                    70.0)));
    final java.util.ArrayList<ReactionPredictionEvidence> evidence =
        new java.util.ArrayList<ReactionPredictionEvidence>();

    final double score =
        scorer.score(
            OrganicReactionPredictionServiceTest.request(
                "modified_combes_quinoline_regioisomer",
                reactant,
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_SYNTHESIS, OrganicReactionDomain.CYCLIZATION),
                List.of(
                    ReactionConditionFeatureKind.ACIDIC_MEDIUM,
                    ReactionConditionFeatureKind.HEATING)),
            candidate,
            evidence);

    Assertions.assertEquals(0.0, score, 1.0E-12);
    Assertions.assertTrue(
        evidence.stream()
            .anyMatch(
                item ->
                    item.kind() == ReactionPredictionEvidenceKind.CALIBRATION_PRIOR
                        && item.description()
                            .contains("combes_modified_two_cf3_quinoline_regioisomer_prior")));
  }

  @Test
  void calibrationScorerRequiresChannelSpecificEvidenceForVariableFormulaPriors() {
    final ReactionOutcomeCalibrationScorer scorer =
        new ReactionOutcomeCalibrationScorer(
            BuiltInReactionOutcomeCalibrationLibraries.heterocycleProductDistributions());
    final Molecule reactant =
        OrganicReactionPredictionServiceTest.ringMolecule(
            "n_alkyl_pyridinium_carbon_nucleophile_heteroarene_screen", "N");
    final Molecule product =
        OrganicReactionPredictionServiceTest.ringMolecule(
            "one_four_dihydropyridine_major_product", "N");
    final Molecule unmarkedProduct =
        OrganicReactionPredictionServiceTest.ringMolecule(
            "unmarked_pyridinium_addition_product", "N");
    final ReactionProductCandidate matchedCandidate =
        OrganicReactionPredictionServiceTest.candidate(
                product, 0.5, "pyridinium 1,4 c4 dihydropyridine major channel")
            .withPredictedMechanisms(
                List.of(
                    PredictedReactionMechanism.of(
                        "pyridinium_one_four_addition",
                        ReactionMechanismFamily.ADDITION,
                        0.6,
                        60.0)));
    final ReactionProductCandidate weakCandidate =
        OrganicReactionPredictionServiceTest.candidate(
            unmarkedProduct, 0.5, "generic pyridinium candidate");
    final java.util.ArrayList<ReactionPredictionEvidence> matchedEvidence =
        new java.util.ArrayList<ReactionPredictionEvidence>();
    final java.util.ArrayList<ReactionPredictionEvidence> weakEvidence =
        new java.util.ArrayList<ReactionPredictionEvidence>();
    final ReactionPredictionRequest request =
        OrganicReactionPredictionServiceTest.request(
            "pyridinium_addition_channel_specificity",
            reactant,
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.ADDITION),
            List.of(
                ReactionConditionFeatureKind.ACIDIC_MEDIUM,
                ReactionConditionFeatureKind.CATALYST_PRESENT));

    final double matchedScore = scorer.score(request, matchedCandidate, matchedEvidence);
    final double weakScore = scorer.score(request, weakCandidate, weakEvidence);

    Assertions.assertEquals(0.0, matchedScore, 1.0E-12);
    Assertions.assertEquals(0.0, weakScore, 1.0E-12);
    Assertions.assertTrue(
        matchedEvidence.stream()
            .anyMatch(
                item ->
                    item.kind() == ReactionPredictionEvidenceKind.CALIBRATION_PRIOR
                        && item.description()
                            .contains("n_alkyl_pyridinium_one_four_addition_major_prior")
                        && item.description().contains("observedProbability=0.6")));
    Assertions.assertTrue(
        weakEvidence.isEmpty(),
        weakEvidence.stream().map(ReactionPredictionEvidence::description).toList().toString());
  }

  @Test
  void calibrationScorerAppliesNamedHeterocycleConstructionDistributionPriors() {
    final ReactionOutcomeCalibrationScorer scorer =
        new ReactionOutcomeCalibrationScorer(
            BuiltInReactionOutcomeCalibrationLibraries.heterocycleProductDistributions());
    final Molecule fischerReactant =
        OrganicReactionPredictionServiceTest.ringMolecule(
            "arylhydrazone_ketone_acid_hydrazone_fischer_screen", "N");
    final ReactionProductCandidate fischerCandidate =
        OrganicReactionPredictionServiceTest.candidate(
                OrganicReactionPredictionServiceTest.ringMolecule(
                    "fischer_indole_major_rearranged_product", "N"),
                0.5,
                "fischer indole major rearranged regioisomer")
            .withPredictedMechanisms(
                List.of(
                    PredictedReactionMechanism.of(
                        "fischer_indole",
                        ReactionMechanismFamily.FISCHER_INDOLE_SYNTHESIS,
                        0.7,
                        74.0)));
    final java.util.ArrayList<ReactionPredictionEvidence> fischerEvidence =
        new java.util.ArrayList<ReactionPredictionEvidence>();

    final double fischerScore =
        scorer.score(
            OrganicReactionPredictionServiceTest.request(
                "fischer_indole_prior_match",
                fischerReactant,
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
                    OrganicReactionDomain.REARRANGEMENT,
                    OrganicReactionDomain.CYCLIZATION),
                List.of(
                    ReactionConditionFeatureKind.ACIDIC_MEDIUM,
                    ReactionConditionFeatureKind.HEATING)),
            fischerCandidate,
            fischerEvidence);

    final Molecule biginelliReactant =
        OrganicReactionPredictionServiceTest.ringMolecule(
            "aldehyde_urea_beta_ketoester_acid_biginelli_screen", "N");
    final ReactionProductCandidate biginelliCandidate =
        OrganicReactionPredictionServiceTest.candidate(
                OrganicReactionPredictionServiceTest.ringMolecule(
                    "biginelli_dihydropyrimidinone_major_product", "N"),
                0.5,
                "biginelli dihydropyrimidinone major product")
            .withPredictedMechanisms(
                List.of(
                    PredictedReactionMechanism.of(
                        "biginelli_dhpm", ReactionMechanismFamily.BIGINELLI_REACTION, 0.7, 71.0)));
    final java.util.ArrayList<ReactionPredictionEvidence> biginelliEvidence =
        new java.util.ArrayList<ReactionPredictionEvidence>();

    final double biginelliScore =
        scorer.score(
            OrganicReactionPredictionServiceTest.request(
                "biginelli_dihydropyrimidinone_prior_match",
                biginelliReactant,
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
                    OrganicReactionDomain.CYCLIZATION,
                    OrganicReactionDomain.ADDITION),
                List.of(
                    ReactionConditionFeatureKind.ACIDIC_MEDIUM,
                    ReactionConditionFeatureKind.HEATING)),
            biginelliCandidate,
            biginelliEvidence);

    final Molecule pictetReactant =
        OrganicReactionPredictionServiceTest.ringMolecule(
            "tryptamine_aldehyde_imine_acid_beta_carboline_screen", "N");
    final ReactionProductCandidate pictetCandidate =
        OrganicReactionPredictionServiceTest.candidate(
                OrganicReactionPredictionServiceTest.ringMolecule(
                    "pictet_spengler_tetrahydro_beta_carboline_major_diastereomer", "N"),
                0.5,
                "pictet-spengler tetrahydro-beta-carboline major diastereomer")
            .withPredictedMechanisms(
                List.of(
                    PredictedReactionMechanism.of(
                        "pictet_spengler",
                        ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION,
                        0.7,
                        67.0)));
    final java.util.ArrayList<ReactionPredictionEvidence> pictetEvidence =
        new java.util.ArrayList<ReactionPredictionEvidence>();

    final double pictetScore =
        scorer.score(
            OrganicReactionPredictionServiceTest.request(
                "pictet_spengler_prior_match",
                pictetReactant,
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_SYNTHESIS, OrganicReactionDomain.CYCLIZATION),
                List.of(
                    ReactionConditionFeatureKind.ACIDIC_MEDIUM,
                    ReactionConditionFeatureKind.COOLING)),
            pictetCandidate,
            pictetEvidence);

    Assertions.assertEquals(0.0, fischerScore, 1.0E-12);
    Assertions.assertTrue(
        fischerEvidence.stream()
            .anyMatch(
                item ->
                    item.kind() == ReactionPredictionEvidenceKind.CALIBRATION_PRIOR
                        && item.description()
                            .contains("fischer_indole_major_rearranged_product_prior")
                        && item.description().contains("substrateHints=arylhydrazone")
                        && item.description()
                            .contains("channelHints=fischer,indole,major,rearranged")));
    Assertions.assertEquals(0.0, biginelliScore, 1.0E-12);
    Assertions.assertTrue(
        biginelliEvidence.stream()
            .anyMatch(
                item ->
                    item.kind() == ReactionPredictionEvidenceKind.CALIBRATION_PRIOR
                        && item.description()
                            .contains("biginelli_dihydropyrimidinone_major_channel_prior")
                        && item.description().contains("substrateHints=aldehyde")
                        && item.description()
                            .contains("channelHints=biginelli,dihydropyrimidinone,major")));
    Assertions.assertEquals(0.0, pictetScore, 1.0E-12);
    Assertions.assertTrue(
        pictetEvidence.stream()
            .anyMatch(
                item ->
                    item.kind() == ReactionPredictionEvidenceKind.CALIBRATION_PRIOR
                        && item.description()
                            .contains("pictet_spengler_tetrahydro_beta_carboline_major_prior")
                        && item.description().contains("substrateHints=tryptamine")
                        && item.description()
                            .contains(
                                "channelHints=pictet-spengler,tetrahydro-beta-carboline,major")));
  }

  @Test
  void calibrationScorerAppliesBroaderHeterocycleDistributionPriors() {
    final ReactionOutcomeCalibrationScorer scorer =
        new ReactionOutcomeCalibrationScorer(
            BuiltInReactionOutcomeCalibrationLibraries.heterocycleProductDistributions());
    final Molecule friedlaenderReactant =
        OrganicReactionPredictionServiceTest.ringMolecule(
            "ortho_aminobenzaldehyde_ketone_quinoline_condensation_screen", "N");
    final ReactionProductCandidate friedlaenderCandidate =
        OrganicReactionPredictionServiceTest.candidate(
                OrganicReactionPredictionServiceTest.ringMolecule(
                    "friedlaender_quinoline_major_condensation_product", "N"),
                0.5,
                "friedlaender quinoline major condensation channel")
            .withPredictedMechanisms(
                List.of(
                    PredictedReactionMechanism.of(
                        "friedlaender_quinoline",
                        ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION,
                        0.7,
                        69.0)));
    final java.util.ArrayList<ReactionPredictionEvidence> friedlaenderEvidence =
        new java.util.ArrayList<ReactionPredictionEvidence>();

    final double friedlaenderScore =
        scorer.score(
            OrganicReactionPredictionServiceTest.request(
                "friedlaender_quinoline_prior_match",
                friedlaenderReactant,
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_SYNTHESIS, OrganicReactionDomain.CYCLIZATION),
                List.of(
                    ReactionConditionFeatureKind.BASIC_MEDIUM,
                    ReactionConditionFeatureKind.HEATING)),
            friedlaenderCandidate,
            friedlaenderEvidence);

    final Molecule chichibabinReactant =
        OrganicReactionPredictionServiceTest.ringMolecule(
            "pyridine_sodium_amide_electron_deficient_heteroarene_amination_screen", "N");
    final ReactionProductCandidate chichibabinCandidate =
        OrganicReactionPredictionServiceTest.candidate(
                OrganicReactionPredictionServiceTest.ringMolecule(
                    "chichibabin_c2_aminopyridine_major_product", "N"),
                0.5,
                "chichibabin c2 aminopyridine major amination channel")
            .withPredictedMechanisms(
                List.of(
                    PredictedReactionMechanism.of(
                        "chichibabin_amination",
                        ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION,
                        0.7,
                        74.0)));
    final java.util.ArrayList<ReactionPredictionEvidence> chichibabinEvidence =
        new java.util.ArrayList<ReactionPredictionEvidence>();

    final double chichibabinScore =
        scorer.score(
            OrganicReactionPredictionServiceTest.request(
                "chichibabin_pyridine_prior_match",
                chichibabinReactant,
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                    OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
                List.of(
                    ReactionConditionFeatureKind.BASIC_MEDIUM,
                    ReactionConditionFeatureKind.HEATING)),
            chichibabinCandidate,
            chichibabinEvidence);

    final Molecule gewaldReactant =
        OrganicReactionPredictionServiceTest.ringMolecule(
            "ketone_nitrile_sulfur_gewald_aminothiophene_screen", "S");
    final ReactionProductCandidate gewaldCandidate =
        OrganicReactionPredictionServiceTest.candidate(
                OrganicReactionPredictionServiceTest.ringMolecule(
                    "gewald_aminothiophene_major_cyclization_product", "S"),
                0.5,
                "gewald aminothiophene major cyclization channel")
            .withPredictedMechanisms(
                List.of(
                    PredictedReactionMechanism.of(
                        "gewald_aminothiophene",
                        ReactionMechanismFamily.MULTICOMPONENT_CONDENSATION,
                        0.7,
                        76.0)));
    final java.util.ArrayList<ReactionPredictionEvidence> gewaldEvidence =
        new java.util.ArrayList<ReactionPredictionEvidence>();

    final double gewaldScore =
        scorer.score(
            OrganicReactionPredictionServiceTest.request(
                "gewald_aminothiophene_prior_match",
                gewaldReactant,
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
                    OrganicReactionDomain.CYCLIZATION,
                    OrganicReactionDomain.ADDITION),
                List.of(
                    ReactionConditionFeatureKind.BASIC_MEDIUM,
                    ReactionConditionFeatureKind.HEATING)),
            gewaldCandidate,
            gewaldEvidence);

    Assertions.assertEquals(0.0, friedlaenderScore, 1.0E-12);
    Assertions.assertTrue(
        friedlaenderEvidence.stream()
            .anyMatch(
                item ->
                    item.kind() == ReactionPredictionEvidenceKind.CALIBRATION_PRIOR
                        && item.description()
                            .contains("friedlaender_quinoline_major_condensation_prior")));
    Assertions.assertEquals(0.0, chichibabinScore, 1.0E-12);
    Assertions.assertTrue(
        chichibabinEvidence.stream()
            .anyMatch(
                item ->
                    item.kind() == ReactionPredictionEvidenceKind.CALIBRATION_PRIOR
                        && item.description().contains("chichibabin_pyridine_c2_amination_prior")));
    Assertions.assertEquals(0.0, gewaldScore, 1.0E-12);
    Assertions.assertTrue(
        gewaldEvidence.stream()
            .anyMatch(
                item ->
                    item.kind() == ReactionPredictionEvidenceKind.CALIBRATION_PRIOR
                        && item.description()
                            .contains("gewald_aminothiophene_major_channel_prior")));
  }

  @Test
  void calibrationScorerDoesNotApplyHeterocyclePriorWithoutRequiredConditionContext() {
    final ReactionOutcomeCalibrationScorer scorer =
        new ReactionOutcomeCalibrationScorer(
            BuiltInReactionOutcomeCalibrationLibraries.heterocycleProductDistributions());
    final Molecule reactant =
        OrganicReactionPredictionServiceTest.ringMolecule(
            "aldehyde_urea_beta_ketoester_neutral_biginelli_probe", "N");
    final ReactionProductCandidate candidate =
        OrganicReactionPredictionServiceTest.candidate(
                OrganicReactionPredictionServiceTest.ringMolecule(
                    "biginelli_dihydropyrimidinone_major_product_without_acid", "N"),
                0.5,
                "biginelli dihydropyrimidinone major product")
            .withPredictedMechanisms(
                List.of(
                    PredictedReactionMechanism.of(
                        "biginelli_dhpm_without_required_context",
                        ReactionMechanismFamily.BIGINELLI_REACTION,
                        0.7,
                        71.0)));
    final java.util.ArrayList<ReactionPredictionEvidence> evidence =
        new java.util.ArrayList<ReactionPredictionEvidence>();

    final double score =
        scorer.score(
            OrganicReactionPredictionServiceTest.request(
                "biginelli_prior_without_required_conditions",
                reactant,
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
                    OrganicReactionDomain.CYCLIZATION,
                    OrganicReactionDomain.ADDITION),
                List.of(ReactionConditionFeatureKind.COOLING)),
            candidate,
            evidence);

    Assertions.assertEquals(0.0, score, 1.0E-12);
    Assertions.assertTrue(
        evidence.isEmpty(),
        evidence.stream().map(ReactionPredictionEvidence::description).toList().toString());
  }

  @Test
  void calibrationScorerAppliesIndoleAndThiadiazoleRegioisomerPriorsWhenHintsMatch() {
    final ReactionOutcomeCalibrationScorer scorer =
        new ReactionOutcomeCalibrationScorer(
            BuiltInReactionOutcomeCalibrationLibraries.heterocycleProductDistributions());
    final Molecule larockReactant =
        OrganicReactionPredictionServiceTest.ringMolecule(
            "ortho_iodoaniline_alkyne_palladium_heteroannulation_screen", "N");
    final Molecule larockProduct =
        OrganicReactionPredictionServiceTest.ringMolecule(
            "larock_indole_major_regioisomer_product", "N");
    final ReactionProductCandidate larockCandidate =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(ReactionParticipant.of(larockProduct, StoichiometricCoefficient.ONE))),
            0.5,
            50.0,
            "Larock indole major regioisomer heteroannulation product.",
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.USER_RULE,
                    "larock indole major regioisomer heteroannulation candidate",
                    0.1)),
            List.of(
                PredictedReactionMechanism.of(
                    "larock_indole_heteroannulation",
                    ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION,
                    0.7,
                    68.0)));
    final java.util.ArrayList<ReactionPredictionEvidence> larockEvidence =
        new java.util.ArrayList<ReactionPredictionEvidence>();

    final double larockScore =
        scorer.score(
            OrganicReactionPredictionServiceTest.request(
                "larock_indole_regioisomer_prior_match",
                larockReactant,
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_SYNTHESIS, OrganicReactionDomain.CYCLIZATION),
                List.of(
                    ReactionConditionFeatureKind.CATALYST_PRESENT,
                    ReactionConditionFeatureKind.HEATING)),
            larockCandidate,
            larockEvidence);

    final Molecule hurdMoriReactant =
        OrganicReactionPredictionServiceTest.ringMolecule(
            "acylhydrazone_tosylhydrazone_thionyl_chloride_bicyclic_screen", "S");
    final Molecule hurdMoriProduct =
        OrganicReactionPredictionServiceTest.ringMolecule(
            "hurd_mori_thiadiazole_bicyclic_regioisomer_product", "S");
    final ReactionProductCandidate hurdMoriCandidate =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(ReactionParticipant.of(hurdMoriProduct, StoichiometricCoefficient.ONE))),
            0.5,
            50.0,
            "Hurd-Mori bicyclic thiadiazole regioselectivity channel.",
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.USER_RULE,
                    "hurd-mori thiadiazole regioselectivity bicyclic channel",
                    0.1)),
            List.of(
                PredictedReactionMechanism.of(
                    "hurd_mori_thiadiazole_cyclization",
                    ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION,
                    0.6,
                    57.0)));
    final java.util.ArrayList<ReactionPredictionEvidence> hurdMoriEvidence =
        new java.util.ArrayList<ReactionPredictionEvidence>();

    final double hurdMoriScore =
        scorer.score(
            OrganicReactionPredictionServiceTest.request(
                "hurd_mori_thiadiazole_regioisomer_prior_match",
                hurdMoriReactant,
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_SYNTHESIS, OrganicReactionDomain.CYCLIZATION),
                List.of(
                    ReactionConditionFeatureKind.HEATING,
                    ReactionConditionFeatureKind.DEHYDRATING_AGENT)),
            hurdMoriCandidate,
            hurdMoriEvidence);

    Assertions.assertEquals(0.0, larockScore, 1.0E-12);
    Assertions.assertEquals(0.0, hurdMoriScore, 1.0E-12);
    Assertions.assertTrue(
        larockEvidence.stream()
            .anyMatch(
                item ->
                    item.kind() == ReactionPredictionEvidenceKind.CALIBRATION_PRIOR
                        && item.description()
                            .contains("larock_indole_heteroannulation_major_regioisomer_prior")));
    Assertions.assertTrue(
        hurdMoriEvidence.stream()
            .anyMatch(
                item ->
                    item.kind() == ReactionPredictionEvidenceKind.CALIBRATION_PRIOR
                        && item.description()
                            .contains("hurd_mori_bicyclic_thiadiazole_regioselectivity_prior")));
  }

  @Test
  void calibrationScorerAppliesModernHeteroarylFunctionalizationPriorsWhenHintsMatch() {
    final ReactionOutcomeCalibrationScorer scorer =
        new ReactionOutcomeCalibrationScorer(
            BuiltInReactionOutcomeCalibrationLibraries.heterocycleProductDistributions());
    final Molecule chActivationReactant =
        OrganicReactionPredictionServiceTest.ringMolecule(
            "pyridine_quinoline_heteroaryl_c_h_directing_group_rhodium_palladium_screen", "N");
    final ReactionProductCandidate chActivationCandidate =
        OrganicReactionPredictionServiceTest.candidate(
                OrganicReactionPredictionServiceTest.ringMolecule(
                    "directed_c_h_annulation_major_heteroarene_product", "N"),
                0.5,
                "c-h activation annulation directed major heteroarene channel")
            .withPredictedMechanisms(
                List.of(
                    PredictedReactionMechanism.of(
                        "directed_c_h_annulation",
                        ReactionMechanismFamily.C_H_ACTIVATION,
                        0.7,
                        67.0)));
    final java.util.ArrayList<ReactionPredictionEvidence> chActivationEvidence =
        new java.util.ArrayList<ReactionPredictionEvidence>();
    final double chActivationScore =
        scorer.score(
            OrganicReactionPredictionServiceTest.request(
                "heteroaryl_c_h_activation_prior_match",
                chActivationReactant,
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                    OrganicReactionDomain.CROSS_COUPLING,
                    OrganicReactionDomain.CYCLIZATION),
                List.of(
                    ReactionConditionFeatureKind.CATALYST_PRESENT,
                    ReactionConditionFeatureKind.HEATING)),
            chActivationCandidate,
            chActivationEvidence);

    final Molecule buchwaldReactant =
        OrganicReactionPredictionServiceTest.ringMolecule(
            "heteroaryl_halide_amine_palladium_ligand_base_screen", "N");
    final ReactionProductCandidate buchwaldCandidate =
        OrganicReactionPredictionServiceTest.candidate(
                OrganicReactionPredictionServiceTest.ringMolecule(
                    "buchwald_heteroaryl_amination_major_amine_product", "N"),
                0.5,
                "buchwald heteroaryl amination major amine channel")
            .withPredictedMechanisms(
                List.of(
                    PredictedReactionMechanism.of(
                        "buchwald_heteroaryl_amination",
                        ReactionMechanismFamily.BUCHWALD_HARTWIG_AMINATION,
                        0.7,
                        70.0)));
    final java.util.ArrayList<ReactionPredictionEvidence> buchwaldEvidence =
        new java.util.ArrayList<ReactionPredictionEvidence>();
    final double buchwaldScore =
        scorer.score(
            OrganicReactionPredictionServiceTest.request(
                "buchwald_heteroaryl_amination_prior_match",
                buchwaldReactant,
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                    OrganicReactionDomain.CROSS_COUPLING,
                    OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
                List.of(
                    ReactionConditionFeatureKind.CATALYST_PRESENT,
                    ReactionConditionFeatureKind.BASIC_MEDIUM,
                    ReactionConditionFeatureKind.HEATING)),
            buchwaldCandidate,
            buchwaldEvidence);

    final Molecule suzukiReactant =
        OrganicReactionPredictionServiceTest.ringMolecule(
            "heteroaryl_halide_boronic_acid_boronate_palladium_base_screen", "N");
    final ReactionProductCandidate suzukiCandidate =
        OrganicReactionPredictionServiceTest.candidate(
                OrganicReactionPredictionServiceTest.ringMolecule(
                    "suzuki_heterobiaryl_major_cross_coupling_product", "N"),
                0.5,
                "suzuki heterobiaryl major cross-coupling channel")
            .withPredictedMechanisms(
                List.of(
                    PredictedReactionMechanism.of(
                        "suzuki_heterobiaryl",
                        ReactionMechanismFamily.SUZUKI_COUPLING,
                        0.7,
                        73.0)));
    final java.util.ArrayList<ReactionPredictionEvidence> suzukiEvidence =
        new java.util.ArrayList<ReactionPredictionEvidence>();
    final double suzukiScore =
        scorer.score(
            OrganicReactionPredictionServiceTest.request(
                "suzuki_heterobiaryl_prior_match",
                suzukiReactant,
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                    OrganicReactionDomain.CROSS_COUPLING),
                List.of(
                    ReactionConditionFeatureKind.CATALYST_PRESENT,
                    ReactionConditionFeatureKind.BASIC_MEDIUM,
                    ReactionConditionFeatureKind.HEATING)),
            suzukiCandidate,
            suzukiEvidence);

    final Molecule nOxideReactant =
        OrganicReactionPredictionServiceTest.ringMolecule(
            "pyridine_n_oxide_quinoline_n_oxide_heteroarene_n_oxide_c2_directed_screen", "N");
    final ReactionProductCandidate nOxideCandidate =
        OrganicReactionPredictionServiceTest.candidate(
                OrganicReactionPredictionServiceTest.ringMolecule(
                    "n_oxide_c2_functionalization_major_heteroarene_product", "N"),
                0.5,
                "n-oxide c2 functionalization major heteroarene channel")
            .withPredictedMechanisms(
                List.of(
                    PredictedReactionMechanism.of(
                        "n_oxide_directed_functionalization",
                        ReactionMechanismFamily.OXIDATION,
                        0.7,
                        69.0)));
    final java.util.ArrayList<ReactionPredictionEvidence> nOxideEvidence =
        new java.util.ArrayList<ReactionPredictionEvidence>();
    final double nOxideScore =
        scorer.score(
            OrganicReactionPredictionServiceTest.request(
                "heteroarene_n_oxide_prior_match",
                nOxideReactant,
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                    OrganicReactionDomain.OXIDATION_REDUCTION),
                List.of(
                    ReactionConditionFeatureKind.CATALYST_PRESENT,
                    ReactionConditionFeatureKind.HEATING)),
            nOxideCandidate,
            nOxideEvidence);

    Assertions.assertEquals(0.0, chActivationScore, 1.0E-12);
    Assertions.assertEquals(0.0, buchwaldScore, 1.0E-12);
    Assertions.assertEquals(0.0, suzukiScore, 1.0E-12);
    Assertions.assertEquals(0.0, nOxideScore, 1.0E-12);
    Assertions.assertTrue(
        chActivationEvidence.stream()
            .anyMatch(
                item ->
                    item.description()
                        .contains("heteroaryl_ch_activation_annulation_major_prior")));
    Assertions.assertTrue(
        buchwaldEvidence.stream()
            .anyMatch(
                item ->
                    item.description()
                        .contains("buchwald_hartwig_heteroaryl_amination_major_prior")));
    Assertions.assertTrue(
        suzukiEvidence.stream()
            .anyMatch(
                item -> item.description().contains("suzuki_heterobiaryl_coupling_major_prior")));
    Assertions.assertTrue(
        nOxideEvidence.stream()
            .anyMatch(
                item ->
                    item.description()
                        .contains("heteroarene_n_oxide_c2_functionalization_major_prior")));
  }

  @Test
  void builtInRulesGeneratePhotochemicalHeterocycleRearrangementWithoutSeedProduct() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()),
            new OrganicReactionCandidateScorer(new MolecularFeatureAnalyzer()));
    final Molecule sulfurHeterocycle =
        OrganicReactionPredictionServiceTest.ringMolecule("photochemical_sulfur_heterocycle", "S");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "photochemical_heterocycle_rearrangement",
            ReactionSide.of(
                List.of(ReactionParticipant.of(sulfurHeterocycle, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(25.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(30.0, TimeDurationUnit.MINUTE),
                List.of(),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.PHOTOCHEMICAL),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.INERT_ATMOSPHERE)),
                "photochemical heterocycle screen"),
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.REARRANGEMENT),
            List.of(),
            null);

    final ReactionPredictionResult result = service.predict(request);

    Assertions.assertTrue(result.hasCandidates());
    Assertions.assertTrue(
        result.candidates().stream()
            .anyMatch(
                candidate ->
                    candidate.predictedMechanisms().stream()
                            .anyMatch(
                                mechanism ->
                                    mechanism.family() == ReactionMechanismFamily.PHOTOCHEMICAL)
                        && candidate.products().participants().stream()
                            .anyMatch(
                                participant ->
                                    participant
                                        .molecule()
                                        .id()
                                        .value()
                                        .contains("photochemical_rearranged"))),
        result.candidates().toString());
    Assertions.assertTrue(
        result.candidates().stream()
            .anyMatch(
                candidate ->
                    OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                        candidate, "same-formula photochemical bond-order isomer")));
  }

  @Test
  void heterocyclePlanningChannelsRemainSeparateFromExplicitAnnulationProduct() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()),
            new OrganicReactionCandidateScorer(new MolecularFeatureAnalyzer()));

    final ReactionPredictionResult dipolar =
        service.predict(
            OrganicReactionPredictionServiceTest.request(
                "dipolar_heterocycle_construction",
                OrganicReactionPredictionServiceTest.functionalGroupRichMolecule(),
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
                    OrganicReactionDomain.CYCLIZATION,
                    OrganicReactionDomain.ADDITION),
                List.of(
                    ReactionConditionFeatureKind.CATALYST_PRESENT,
                    ReactionConditionFeatureKind.ADDITION_RATE,
                    ReactionConditionFeatureKind.ANHYDROUS)));
    final ReactionPredictionResult hantzsch =
        service.predict(
            OrganicReactionPredictionServiceTest.request(
                "hantzsch_like_pyridine_construction",
                OrganicReactionPredictionServiceTest.functionalGroupRichMolecule(),
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_SYNTHESIS, OrganicReactionDomain.CYCLIZATION),
                List.of(
                    ReactionConditionFeatureKind.HEATING,
                    ReactionConditionFeatureKind.ADDITION_RATE,
                    ReactionConditionFeatureKind.WORKUP)));
    final ReactionPredictionResult paalKnorr =
        service.predict(
            OrganicReactionPredictionServiceTest.request(
                "paal_knorr_heterocycle_construction",
                OrganicReactionPredictionServiceTest.functionalGroupRichMolecule(),
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
                    OrganicReactionDomain.CYCLIZATION,
                    OrganicReactionDomain.ADDITION),
                List.of(
                    ReactionConditionFeatureKind.ACIDIC_MEDIUM,
                    ReactionConditionFeatureKind.DEHYDRATING_AGENT,
                    ReactionConditionFeatureKind.HEATING)));
    final ReactionPredictionResult biginelli =
        service.predict(
            OrganicReactionPredictionServiceTest.request(
                "biginelli_dihydropyrimidinone_construction",
                OrganicReactionPredictionServiceTest.functionalGroupRichMolecule(),
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
                    OrganicReactionDomain.CYCLIZATION,
                    OrganicReactionDomain.ADDITION),
                List.of(
                    ReactionConditionFeatureKind.ACIDIC_MEDIUM,
                    ReactionConditionFeatureKind.HEATING,
                    ReactionConditionFeatureKind.ADDITION_RATE)));
    final ReactionPredictionResult gewald =
        service.predict(
            OrganicReactionPredictionServiceTest.request(
                "gewald_thiophene_construction",
                OrganicReactionPredictionServiceTest.functionalGroupRichMolecule(),
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
                    OrganicReactionDomain.CYCLIZATION,
                    OrganicReactionDomain.ADDITION),
                List.of(
                    ReactionConditionFeatureKind.BASIC_MEDIUM,
                    ReactionConditionFeatureKind.HEATING,
                    ReactionConditionFeatureKind.ADDITION_RATE)));
    final ReactionPredictionResult fischer =
        service.predict(
            OrganicReactionPredictionServiceTest.request(
                "fischer_indole_construction",
                OrganicReactionPredictionServiceTest.functionalGroupRichMolecule(),
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
                    OrganicReactionDomain.REARRANGEMENT,
                    OrganicReactionDomain.CYCLIZATION),
                List.of(
                    ReactionConditionFeatureKind.ACIDIC_MEDIUM,
                    ReactionConditionFeatureKind.HEATING,
                    ReactionConditionFeatureKind.WORKUP)));
    final ReactionPredictionResult pictetSpengler =
        service.predict(
            OrganicReactionPredictionServiceTest.request(
                "pictet_spengler_cyclization",
                OrganicReactionPredictionServiceTest.functionalGroupRichMolecule(),
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
                    OrganicReactionDomain.CYCLIZATION,
                    OrganicReactionDomain.ADDITION),
                List.of(
                    ReactionConditionFeatureKind.ACIDIC_MEDIUM,
                    ReactionConditionFeatureKind.COOLING,
                    ReactionConditionFeatureKind.ADDITION_RATE)));
    final ReactionPredictionResult krohnke =
        service.predict(
            OrganicReactionPredictionServiceTest.request(
                "krohnke_like_pyridine_construction",
                OrganicReactionPredictionServiceTest.heterocycleCarbonylAlkeneMolecule(),
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
                    OrganicReactionDomain.CYCLIZATION,
                    OrganicReactionDomain.ADDITION),
                List.of(
                    ReactionConditionFeatureKind.BASIC_MEDIUM,
                    ReactionConditionFeatureKind.HEATING,
                    ReactionConditionFeatureKind.ADDITION_RATE)));
    final ReactionPredictionResult dearomatization =
        service.predict(
            ReactionPredictionRequest.of(
                "dearomatization_annulation",
                ReactionSide.of(
                    List.of(
                        ReactionParticipant.of(
                            OrganicReactionPredictionServiceTest.ringMolecule(
                                "indole_like_n_heterocycle", "N"),
                            StoichiometricCoefficient.ONE),
                        ReactionParticipant.of(
                            OrganicReactionPredictionServiceTest.ringMolecule(
                                "annulation_carbon_source", "C"),
                            StoichiometricCoefficient.ONE))),
                ReactionConditions.of(
                    Temperature.of(80.0, TemperatureUnit.CELSIUS),
                    null,
                    TimeDuration.of(6.0, TimeDurationUnit.HOUR),
                    List.of(),
                    List.of(
                        ReactionConditionFeature.named(
                            ReactionConditionFeatureKind.CATALYST_PRESENT),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.COOLING),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS)),
                    "dearomatization annulation"),
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                    OrganicReactionDomain.REARRANGEMENT),
                List.of(),
                null));

    assertDiagnosticOnly(dipolar, "transform_one_three_dipolar_heterocycle_construction");
    assertDiagnosticOnly(hantzsch, "transform_hantzsch_like_pyridine_construction");
    assertDiagnosticOnly(paalKnorr, "transform_paal_knorr_heterocycle_construction");
    assertDiagnosticOnly(biginelli, "transform_biginelli_dihydropyrimidinone_construction");
    assertDiagnosticOnly(gewald, "transform_gewald_thiophene_construction");
    assertDiagnosticOnly(fischer, "transform_fischer_indole_construction");
    assertDiagnosticOnly(pictetSpengler, "transform_pictet_spengler_cyclization");
    assertDiagnosticOnly(krohnke, "transform_krohnke_like_pyridine_construction");
    Assertions.assertTrue(
        dearomatization.candidates().stream()
            .anyMatch(
                candidate ->
                    OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                        candidate, "transform_indole_dearomatization_annulation")),
        dearomatization.candidates().toString());
  }

  @Test
  void builtInRulesGenerateAnrorcChannelForActivatedAzaHeterocycle() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final Molecule substrate =
        OrganicReactionPredictionServiceTest.halogenatedNitrogenHeterocycle(
            "anrorc_chloro_pyridine", "Cl");
    final Molecule amine =
        OrganicReactionPredictionServiceTest.primaryAmine("anrorc_channel_amine");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "anrorc_competing_channel",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(substrate, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(amine, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(120.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(3.0, TimeDurationUnit.HOUR),
                List.of(),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS)),
                "basic heated aza-heterocycle substitution screen"),
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION,
                OrganicReactionDomain.REARRANGEMENT),
            List.of(),
            null);

    final ReactionPredictionResult result = service.predict(request);

    Assertions.assertTrue(result.hasCandidates());
    Assertions.assertTrue(
        result.candidates().stream()
            .anyMatch(
                candidate ->
                    candidate.predictedMechanisms().stream()
                            .anyMatch(
                                mechanism ->
                                    mechanism.family()
                                        == ReactionMechanismFamily
                                            .ADDITION_RING_OPENING_RING_CLOSURE)
                        && OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                            candidate, "Generated ANRORC site combination")),
        result.candidates().toString());
  }

  @Test
  void namedSodiumAmideDoesNotSupplyAminationAtoms() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final Molecule pyridine =
        OrganicReactionPredictionServiceTest.ringMolecule("chichibabin_pyridine", "N");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "chichibabin_rule_generation",
            ReactionSide.of(
                List.of(ReactionParticipant.of(pyridine, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(80.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(1.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.named(ReactionComponentRole.BASE, "sodium amide")),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS)),
                "sodium amide aza-heteroarene amination"),
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            List.of(),
            null);

    final ReactionPredictionResult result = service.predict(request);

    assertDiagnosticOnly(result, "transform_chichibabin_aza_heteroarene_amination");
    Assertions.assertFalse(result.hasCandidates());
    Assertions.assertEquals(2, atomCount(pyridine, "N"));
  }

  @Test
  void builtInAnrorcRuleGeneratesChannelsForEveryActivatedAzaHeterocycleInMixture() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final Molecule first =
        OrganicReactionPredictionServiceTest.halogenatedNitrogenHeterocycle(
            "anrorc_first_chloro_pyridine", "Cl");
    final Molecule second =
        OrganicReactionPredictionServiceTest.halogenatedNitrogenHeterocycle(
            "anrorc_second_chloro_pyridine", "Cl");
    final Molecule amine =
        OrganicReactionPredictionServiceTest.primaryAmine("anrorc_multi_substrate_amine");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "anrorc_multi_substrate_channel",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(first, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(second, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(amine, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(120.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(3.0, TimeDurationUnit.HOUR),
                List.of(),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS)),
                "basic heated aza-heterocycle mixture"),
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION,
                OrganicReactionDomain.REARRANGEMENT),
            List.of(),
            null);

    final ReactionPredictionResult result = service.predict(request);

    Assertions.assertTrue(
        result.candidates().stream()
            .anyMatch(
                candidate ->
                    candidate.products().participants().stream()
                        .anyMatch(
                            participant ->
                                participant
                                    .molecule()
                                    .id()
                                    .value()
                                    .contains("anrorc_first_chloro_pyridine"))),
        result.candidates().toString());
    Assertions.assertTrue(
        result.candidates().stream()
            .anyMatch(
                candidate ->
                    candidate.products().participants().stream()
                        .anyMatch(
                            participant ->
                                participant
                                    .molecule()
                                    .id()
                                    .value()
                                    .contains("anrorc_second_chloro_pyridine"))),
        result.candidates().toString());
  }

  @Test
  void minisciLikeRuleGeneratesNitrogenHeterocycleRadicalFunctionalizationCandidate() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final Molecule pyridine =
        OrganicReactionPredictionServiceTest.ringMolecule("minisci_pyridine", "N");
    final Molecule carbonSource =
        OrganicReactionPredictionServiceTest.ringMolecule("minisci_carbon_source", "C");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "minisci_like_prediction",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(pyridine, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(carbonSource, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(60.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(4.0, TimeDurationUnit.HOUR),
                List.of(),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.ACIDIC_MEDIUM),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.PHOTOCHEMICAL)),
                "acidic photochemical radical heterocycle functionalization"),
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.RADICAL_FUNCTIONALIZATION),
            List.of(),
            null);

    final ReactionPredictionResult result = service.predict(request);

    Assertions.assertTrue(result.hasCandidates());
    Assertions.assertTrue(
        result.candidates().stream()
            .anyMatch(
                candidate ->
                    OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                        candidate,
                        "transform_minisci_nitrogen_heterocycle_carbon_functionalization")),
        result.candidates().toString());
    Assertions.assertTrue(
        result.candidates().stream()
                .filter(
                    candidate ->
                        OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                            candidate,
                            "transform_minisci_nitrogen_heterocycle_carbon_functionalization"))
                .count()
            > 1L,
        result.candidates().toString());
    Assertions.assertTrue(
        result.candidates().stream()
            .filter(
                candidate ->
                    OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                        candidate,
                        "transform_minisci_nitrogen_heterocycle_carbon_functionalization"))
            .allMatch(
                candidate ->
                    candidate.predictedMechanisms().stream()
                        .anyMatch(
                            mechanism ->
                                mechanism.family()
                                    == ReactionMechanismFamily.RADICAL_SUBSTITUTION)),
        result.candidates().toString());
  }

  @Test
  void builtInRulesGenerateCommonOrganicTransformChannels() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final Molecule acidSubstrate =
        OrganicReactionPredictionServiceTest.carboxylAmineAlkeneMolecule();
    final Molecule baseSubstrate =
        OrganicReactionPredictionServiceTest.functionalGroupRichMolecule();
    final ReactionPredictionRequest acidControlledRequest =
        ReactionPredictionRequest.of(
            "common_organic_transform_acid_screen",
            ReactionSide.of(
                List.of(ReactionParticipant.of(acidSubstrate, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(90.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(5.0, TimeDurationUnit.HOUR),
                List.of(),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.ACIDIC_MEDIUM),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.WORKUP),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.ADDITION_RATE),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.CATALYST_PRESENT)),
                "broad common organic acid-controlled transform screen"),
            List.of(
                OrganicReactionDomain.ADDITION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION,
                OrganicReactionDomain.ACID_BASE),
            List.of(),
            null);
    final ReactionPredictionRequest baseControlledRequest =
        ReactionPredictionRequest.of(
            "common_organic_transform_base_screen",
            ReactionSide.of(
                List.of(ReactionParticipant.of(baseSubstrate, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(90.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(5.0, TimeDurationUnit.HOUR),
                List.of(),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.WORKUP),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.ADDITION_RATE),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.CATALYST_PRESENT)),
                "broad common organic base-controlled transform screen"),
            List.of(
                OrganicReactionDomain.ADDITION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION,
                OrganicReactionDomain.ACID_BASE),
            List.of(),
            null);

    final ReactionPredictionResult acidResult = service.predict(acidControlledRequest);
    final ReactionPredictionResult baseResult = service.predict(baseControlledRequest);

    Assertions.assertTrue(acidResult.hasCandidates() || acidResult.hasMechanismChannels());
    Assertions.assertTrue(baseResult.hasCandidates() || baseResult.hasMechanismChannels());
    OrganicReactionPredictionServiceTest.assertRecognizedChannel(
        acidResult, "transform_carboxyl_esterification");
    OrganicReactionPredictionServiceTest.assertRecognizedChannel(
        acidResult, "transform_mannich_aminomethylation");
    OrganicReactionPredictionServiceTest.assertRecognizedChannel(
        acidResult, "bond_addition_alkene_hydration");
    OrganicReactionPredictionServiceTest.assertRecognizedChannel(
        baseResult, "transform_ester_transesterification");
    OrganicReactionPredictionServiceTest.assertRecognizedChannel(
        baseResult, "transform_knoevenagel_condensation");
    OrganicReactionPredictionServiceTest.assertRecognizedChannel(
        baseResult, "transform_henry_nitroaldol");
    Assertions.assertFalse(
        new ru.pathcreator.vadim.quantum.chemistry.classical.application.rule
                .EpoxideHydrolysisOrganicReactionRule()
            .match(baseControlledRequest)
            .matched(),
        "An alkene-containing fixture without a C-O-C three-membered ring is not an epoxide.");
    Assertions.assertTrue(
        baseResult.candidates().stream()
            .noneMatch(
                candidate ->
                    candidate.evidence().stream()
                        .anyMatch(
                            evidence ->
                                evidence.description().contains("transform_epoxide_opening"))));
  }

  @Test
  void alkeneAdditionLabelsCannotReplaceExplicitDonorGraphs() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final Molecule alkene = OrganicReactionPredictionServiceTest.carboxylAmineAlkeneMolecule();
    final ReactionPredictionResult hydrogenationResult =
        service.predict(
            ReactionPredictionRequest.of(
                "alkene_graph_hydrogenation_screen",
                ReactionSide.of(
                    List.of(ReactionParticipant.of(alkene, StoichiometricCoefficient.ONE))),
                ReactionConditions.of(
                    Temperature.of(55.0, TemperatureUnit.CELSIUS),
                    null,
                    TimeDuration.of(2.0, TimeDurationUnit.HOUR),
                    List.of(
                        ReactionConditionComponent.named(
                            ReactionComponentRole.CATALYST, "acid catalyst")),
                    List.of(
                        ReactionConditionFeature.named(
                            ReactionConditionFeatureKind.CATALYST_PRESENT),
                        ReactionConditionFeature.named(
                            ReactionConditionFeatureKind.INERT_ATMOSPHERE),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING)),
                    "graph hydrogenation through alkene double bond"),
                List.of(OrganicReactionDomain.ADDITION, OrganicReactionDomain.OXIDATION_REDUCTION),
                List.of(),
                null));
    final ReactionPredictionResult hydrationResult =
        service.predict(
            ReactionPredictionRequest.of(
                "alkene_graph_hydration_screen",
                ReactionSide.of(
                    List.of(ReactionParticipant.of(alkene, StoichiometricCoefficient.ONE))),
                ReactionConditions.of(
                    Temperature.of(25.0, TemperatureUnit.CELSIUS),
                    null,
                    TimeDuration.of(1.0, TimeDurationUnit.HOUR),
                    List.of(
                        ReactionConditionComponent.named(
                            ReactionComponentRole.CATALYST, "acid catalyst")),
                    List.of(
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.ACIDIC_MEDIUM),
                        ReactionConditionFeature.named(
                            ReactionConditionFeatureKind.CATALYST_PRESENT),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.WORKUP)),
                    "graph hydration through alkene double bond"),
                List.of(OrganicReactionDomain.ADDITION),
                List.of(),
                null));
    final ReactionPredictionResult hydrohalogenationResult =
        service.predict(
            ReactionPredictionRequest.of(
                "alkene_graph_hydrohalogenation_screen",
                ReactionSide.of(
                    List.of(ReactionParticipant.of(alkene, StoichiometricCoefficient.ONE))),
                ReactionConditions.of(
                    Temperature.of(5.0, TemperatureUnit.CELSIUS),
                    null,
                    TimeDuration.of(1.0, TimeDurationUnit.HOUR),
                    List.of(
                        ReactionConditionComponent.named(ReactionComponentRole.ADDITIVE, "HCl")),
                    List.of(
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.ACIDIC_MEDIUM),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.COOLING),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.ADDITION_RATE)),
                    "graph hydrohalogenation through alkene double bond"),
                List.of(OrganicReactionDomain.ADDITION),
                List.of(),
                null));
    final ReactionPredictionResult hydroborationResult =
        service.predict(
            ReactionPredictionRequest.of(
                "alkene_graph_hydroboration_screen",
                ReactionSide.of(
                    List.of(ReactionParticipant.of(alkene, StoichiometricCoefficient.ONE))),
                ReactionConditions.of(
                    Temperature.of(20.0, TemperatureUnit.CELSIUS),
                    null,
                    TimeDuration.of(2.0, TimeDurationUnit.HOUR),
                    List.of(
                        ReactionConditionComponent.named(
                            ReactionComponentRole.OTHER, "borane oxidant sequence")),
                    List.of(
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.WORKUP),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.ADDITION_RATE)),
                    "graph hydroboration oxidation through alkene double bond"),
                List.of(OrganicReactionDomain.ADDITION, OrganicReactionDomain.OXIDATION_REDUCTION),
                List.of(),
                null));
    final ReactionPredictionResult thiolEneResult =
        service.predict(
            ReactionPredictionRequest.of(
                "alkene_graph_thiol_ene_screen",
                ReactionSide.of(
                    List.of(ReactionParticipant.of(alkene, StoichiometricCoefficient.ONE))),
                ReactionConditions.of(
                    Temperature.of(25.0, TemperatureUnit.CELSIUS),
                    null,
                    TimeDuration.of(1.0, TimeDurationUnit.HOUR),
                    List.of(
                        ReactionConditionComponent.named(
                            ReactionComponentRole.ADDITIVE, "thiol source")),
                    List.of(
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.PHOTOCHEMICAL),
                        ReactionConditionFeature.named(
                            ReactionConditionFeatureKind.INERT_ATMOSPHERE),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.ADDITION_RATE)),
                    "graph thiol-ene through alkene double bond"),
                List.of(
                    OrganicReactionDomain.ADDITION,
                    OrganicReactionDomain.RADICAL_FUNCTIONALIZATION),
                List.of(),
                null));
    final ReactionPredictionResult epoxidationResult =
        service.predict(
            ReactionPredictionRequest.of(
                "alkene_graph_epoxidation_screen",
                ReactionSide.of(
                    List.of(ReactionParticipant.of(alkene, StoichiometricCoefficient.ONE))),
                ReactionConditions.of(
                    Temperature.of(5.0, TemperatureUnit.CELSIUS),
                    null,
                    TimeDuration.of(1.0, TimeDurationUnit.HOUR),
                    List.of(
                        ReactionConditionComponent.named(
                            ReactionComponentRole.ADDITIVE, "peracid")),
                    List.of(
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.COOLING),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.ADDITION_RATE),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS)),
                    "graph epoxidation through alkene double bond"),
                List.of(OrganicReactionDomain.ADDITION, OrganicReactionDomain.OXIDATION_REDUCTION),
                List.of(),
                null));
    final ReactionPredictionResult aziridinationResult =
        service.predict(
            ReactionPredictionRequest.of(
                "alkene_graph_aziridination_screen",
                ReactionSide.of(
                    List.of(ReactionParticipant.of(alkene, StoichiometricCoefficient.ONE))),
                ReactionConditions.of(
                    Temperature.of(20.0, TemperatureUnit.CELSIUS),
                    null,
                    TimeDuration.of(2.0, TimeDurationUnit.HOUR),
                    List.of(
                        ReactionConditionComponent.named(
                            ReactionComponentRole.ADDITIVE, "nitrene precursor")),
                    List.of(
                        ReactionConditionFeature.named(
                            ReactionConditionFeatureKind.CATALYST_PRESENT),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.ADDITION_RATE)),
                    "graph aziridination through alkene double bond"),
                List.of(
                    OrganicReactionDomain.ADDITION, OrganicReactionDomain.HETEROCYCLE_SYNTHESIS),
                List.of(),
                null));

    assertDiagnosticOnly(hydrogenationResult, "bond_addition_alkene_hydrogenation");
    assertDiagnosticOnly(hydrationResult, "bond_addition_alkene_hydration");
    assertDiagnosticOnly(hydrohalogenationResult, "bond_addition_alkene_hydrohalogenation");
    assertDiagnosticOnly(thiolEneResult, "bond_addition_alkene_thiol_ene_click");
    assertDiagnosticOnly(aziridinationResult, "bridge_addition_alkene_aziridination");
    assertNoCandidateEvidenceContaining(
        hydroborationResult, "bond_addition_alkene_hydroboration_oxidation");
    assertNoCandidateEvidenceContaining(epoxidationResult, "bridge_addition_alkene_epoxidation");
    Assertions.assertTrue(
        BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction().rules().stream()
            .anyMatch(
                rule -> rule.id().value().equals("bond_addition_alkene_hydroboration_oxidation")));
    Assertions.assertEquals(0, atomCount(alkene, "H"));
  }

  @Test
  void bondAdditionTransformPreservesChargedOpenShellState() {
    final Molecule alkene = OrganicReactionPredictionServiceTest.chargedRadicalAlkene();
    final BondAdditionTransform transform =
        BondAdditionTransform.of(
            "charged_radical_hydrogenation",
            "charged radical hydrogenated graph analogue",
            BondType.DOUBLE,
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            ReactantAtomSelector.element("C"),
            ElementSymbol.of("H"),
            ElementSymbol.of("H"),
            BondType.SINGLE,
            BondType.SINGLE,
            "Adds hydrogen across a charged radical alkene without changing the molecular charge/spin model.");
    final List<MolecularTransformProduct> products =
        new BondAdditionTransformer().transformAll(alkene, transform);

    Assertions.assertEquals(1, products.size());
    final Molecule product = products.get(0).product();
    Assertions.assertEquals(alkene.charge(), product.charge());
    Assertions.assertEquals(alkene.spinMultiplicity(), product.spinMultiplicity());
    Assertions.assertEquals(alkene.stereochemistry(), product.stereochemistry());
    Assertions.assertEquals(alkene.conformation(), product.conformation());
    Assertions.assertEquals(alkene.opticalRotation(), product.opticalRotation());
    Assertions.assertEquals(alkene.symmetry(), product.symmetry());
  }

  @Test
  void nitrileHydrationAndReductionNeedExplicitAtomSources() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final Molecule substrate = OrganicReactionPredictionServiceTest.nitrileNitrogenHeterocycle();
    final ReactionPredictionResult hydrationResult =
        service.predict(
            ReactionPredictionRequest.of(
                "nitrile_graph_hydration_screen",
                ReactionSide.of(
                    List.of(ReactionParticipant.of(substrate, StoichiometricCoefficient.ONE))),
                ReactionConditions.of(
                    Temperature.of(95.0, TemperatureUnit.CELSIUS),
                    null,
                    TimeDuration.of(3.0, TimeDurationUnit.HOUR),
                    List.of(),
                    List.of(
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.ACIDIC_MEDIUM),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.WORKUP)),
                    "wet heated nitrile hydration graph screen"),
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                    OrganicReactionDomain.ADDITION),
                List.of(),
                null));
    final ReactionPredictionResult reductionResult =
        service.predict(
            ReactionPredictionRequest.of(
                "nitrile_graph_reduction_screen",
                ReactionSide.of(
                    List.of(ReactionParticipant.of(substrate, StoichiometricCoefficient.ONE))),
                ReactionConditions.of(
                    Temperature.of(70.0, TemperatureUnit.CELSIUS),
                    null,
                    TimeDuration.of(2.0, TimeDurationUnit.HOUR),
                    List.of(
                        ReactionConditionComponent.named(
                            ReactionComponentRole.CATALYST, "hydrogenation catalyst")),
                    List.of(
                        ReactionConditionFeature.named(
                            ReactionConditionFeatureKind.CATALYST_PRESENT),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                        ReactionConditionFeature.named(
                            ReactionConditionFeatureKind.INERT_ATMOSPHERE)),
                    "catalytic nitrile reduction graph screen"),
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                    OrganicReactionDomain.OXIDATION_REDUCTION),
                List.of(),
                null));

    assertDiagnosticOnly(hydrationResult, "bond_addition_heteroaryl_nitrile_hydration");
    assertDiagnosticOnly(reductionResult, "bond_addition_heteroaryl_nitrile_reduction");
  }

  @Test
  void builtInNAlkylationTransfersAlkylatingFragmentGraph() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final Molecule scaffold =
        OrganicReactionPredictionServiceTest.ringMolecule("n_alkylation_pyridine", "N");
    final Molecule alkylatingFragment =
        OrganicReactionPredictionServiceTest.alkylHalideFragment(
            "n_alkylation_ethyl_bromide", "Br");
    final ReactionPredictionResult result =
        service.predict(
            ReactionPredictionRequest.of(
                "n_alkylation_graph_fragment_attachment",
                ReactionSide.of(
                    List.of(
                        ReactionParticipant.of(scaffold, StoichiometricCoefficient.ONE),
                        ReactionParticipant.of(alkylatingFragment, StoichiometricCoefficient.ONE))),
                ReactionConditions.of(
                    Temperature.of(80.0, TemperatureUnit.CELSIUS),
                    null,
                    TimeDuration.of(4.0, TimeDurationUnit.HOUR),
                    List.of(
                        ReactionConditionComponent.named(ReactionComponentRole.BASE, "mild base")),
                    List.of(
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.ADDITION_RATE)),
                    "graph N-alkylation fragment attachment"),
                List.of(
                    OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                    OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION,
                    OrganicReactionDomain.ACID_BASE),
                List.of(),
                null));

    Assertions.assertTrue(
        result.candidates().stream()
            .anyMatch(
                candidate -> {
                  if (!OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                          candidate, "fragment_attachment_nitrogen_heterocycle_n_alkylation")
                      || !OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                          candidate, "graph fragment attachment")) {
                    return false;
                  }
                  final Molecule product = candidate.products().participants().get(0).molecule();
                  return product.id().value().contains("n_alkyl_graph_product")
                      && candidate.products().participants().stream()
                          .map(ReactionParticipant::molecule)
                          .anyMatch(
                              coproduct ->
                                  coproduct.id().value().contains("_fragment_leaving_coproduct_")
                                      && OrganicReactionPredictionServiceTest.atomCount(
                                              coproduct, "Br")
                                          == 1)
                      && product.atomCount()
                          == scaffold.atomCount() + alkylatingFragment.atomCount() - 1
                      && product.bondCount()
                          == scaffold.bondCount() + alkylatingFragment.bondCount()
                      && OrganicReactionPredictionServiceTest.atomCount(product, "Br") == 0
                      && OrganicReactionPredictionServiceTest.atomCount(product, "C") == 6;
                }),
        OrganicReactionPredictionServiceTest.candidateSummary(result));
    OrganicReactionPredictionServiceTest.assertNoCandidateEvidenceContaining(
        result, "Fallback single-site surrogate");
  }

  @Test
  void carbonylAdditionWithoutExplicitReagentRemainsDiagnostic() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final Molecule carbonyl = OrganicReactionPredictionServiceTest.carboxylAmineAlkeneMolecule();
    final ReactionPredictionResult result =
        service.predict(
            ReactionPredictionRequest.of(
                "carbonyl_graph_organometallic_addition_screen",
                ReactionSide.of(
                    List.of(ReactionParticipant.of(carbonyl, StoichiometricCoefficient.ONE))),
                ReactionConditions.of(
                    Temperature.of(0.0, TemperatureUnit.CELSIUS),
                    null,
                    TimeDuration.of(1.5, TimeDurationUnit.HOUR),
                    List.of(
                        ReactionConditionComponent.named(
                            ReactionComponentRole.OTHER, "organometallic reagent")),
                    List.of(
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS),
                        ReactionConditionFeature.named(
                            ReactionConditionFeatureKind.INERT_ATMOSPHERE),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.COOLING)),
                    "graph addition through carbonyl double bond"),
                List.of(OrganicReactionDomain.ADDITION),
                List.of(),
                null));

    assertDiagnosticOnly(result, "bond_addition_carbonyl_organometallic_addition");
  }

  @Test
  void namedHydrideDoesNotCreateAlcoholFromDisconnectedAcylFixture() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final Molecule carbonyl = OrganicReactionPredictionServiceTest.carboxylAmineAlkeneMolecule();
    final ReactionPredictionResult result =
        service.predict(
            ReactionPredictionRequest.of(
                "carbonyl_graph_hydride_reduction_screen",
                ReactionSide.of(
                    List.of(ReactionParticipant.of(carbonyl, StoichiometricCoefficient.ONE))),
                ReactionConditions.of(
                    Temperature.of(20.0, TemperatureUnit.CELSIUS),
                    null,
                    TimeDuration.of(2.0, TimeDurationUnit.HOUR),
                    List.of(
                        ReactionConditionComponent.named(
                            ReactionComponentRole.OTHER, "hydride donor")),
                    List.of(
                        ReactionConditionFeature.named(
                            ReactionConditionFeatureKind.REDUCING_MEDIUM),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.WORKUP)),
                    "graph hydride reduction through carbonyl double bond"),
                List.of(OrganicReactionDomain.OXIDATION_REDUCTION),
                List.of(),
                null));

    Assertions.assertFalse(result.hasCandidates());
    Assertions.assertTrue(
        BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction().rules().stream()
            .noneMatch(
                rule -> rule.id().value().equals("bond_addition_carbonyl_hydride_reduction")),
        "The removed surrogate hydride rule must not return; explicit H2 has a separate supported rule.");
  }

  @Test
  void reductiveAminationLabelCannotSupplyNitrogenAndHydrogen() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final Molecule carbonylAmine =
        OrganicReactionPredictionServiceTest.carboxylAmineAlkeneMolecule();
    final ReactionPredictionResult result =
        service.predict(
            ReactionPredictionRequest.of(
                "carbonyl_graph_reductive_amination_screen",
                ReactionSide.of(
                    List.of(ReactionParticipant.of(carbonylAmine, StoichiometricCoefficient.ONE))),
                ReactionConditions.of(
                    Temperature.of(25.0, TemperatureUnit.CELSIUS),
                    null,
                    TimeDuration.of(4.0, TimeDurationUnit.HOUR),
                    List.of(
                        ReactionConditionComponent.named(
                            ReactionComponentRole.OTHER, "mild hydride donor")),
                    List.of(
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.ACIDIC_MEDIUM),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.ADDITION_RATE),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.WORKUP)),
                    "graph reductive amination through carbonyl double bond"),
                List.of(OrganicReactionDomain.ADDITION, OrganicReactionDomain.OXIDATION_REDUCTION),
                List.of(),
                null));

    assertDiagnosticOnly(result, "bond_addition_carbonyl_reductive_amination");
  }

  @Test
  void builtInCarbonylAmineCyclizationGeneratesIntramolecularBondProduct() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final Molecule carbonylAmine =
        OrganicReactionPredictionServiceTest.carboxylAmineAlkeneMolecule();
    final ReactionPredictionResult result =
        service.predict(
            ReactionPredictionRequest.of(
                "carbonyl_amine_graph_cyclization_screen",
                ReactionSide.of(
                    List.of(ReactionParticipant.of(carbonylAmine, StoichiometricCoefficient.ONE))),
                ReactionConditions.of(
                    Temperature.of(80.0, TemperatureUnit.CELSIUS),
                    null,
                    TimeDuration.of(3.0, TimeDurationUnit.HOUR),
                    List.of(
                        ReactionConditionComponent.named(ReactionComponentRole.ACID, "mild acid")),
                    List.of(
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.ACIDIC_MEDIUM),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.WORKUP)),
                    "intramolecular carbonyl amine ring closure"),
                List.of(
                    OrganicReactionDomain.CYCLIZATION, OrganicReactionDomain.HETEROCYCLE_SYNTHESIS),
                List.of(),
                null));

    Assertions.assertTrue(
        result.candidates().stream()
            .anyMatch(
                candidate -> {
                  if (!OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                          candidate, "intramolecular_bond_carbonyl_amine_cyclization")
                      || !OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                          candidate, "graph intramolecular bond formation")) {
                    return false;
                  }
                  final Molecule product = candidate.products().participants().get(0).molecule();
                  return product.id().value().contains("carbonyl_amine_cyclized_graph")
                      && product.atomCount() == carbonylAmine.atomCount()
                      && product.bondCount() == carbonylAmine.bondCount() + 1
                      && OrganicReactionPredictionServiceTest.hasBond(
                          product, "a0", "a3", BondType.SINGLE);
                }),
        OrganicReactionPredictionServiceTest.candidateSummary(result));
    OrganicReactionPredictionServiceTest.assertNoCandidateEvidenceKind(
        result, ReactionPredictionEvidenceKind.SURROGATE_PRODUCT);
  }

  @Test
  void builtInBaeyerVilligerGeneratesOxygenInsertionGraphProduct() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final Molecule substrate = explicitHydrogens(heterocycleCarbonylAlkeneMolecule());
    final Molecule peracid =
        explicitHydrogens(
            Molecule.of(
                MoleculeId.of("peracetic_acid"),
                "Peracetic acid",
                List.of(
                    atom("d0", "C"),
                    atom("d1", "C"),
                    atom("d2", "O"),
                    atom("d3", "O"),
                    atom("d4", "O")),
                List.of(
                    Bond.of(AtomId.of("d0"), AtomId.of("d1"), BondType.SINGLE),
                    Bond.of(AtomId.of("d1"), AtomId.of("d2"), BondType.DOUBLE),
                    Bond.of(AtomId.of("d1"), AtomId.of("d3"), BondType.SINGLE),
                    Bond.of(AtomId.of("d3"), AtomId.of("d4"), BondType.SINGLE)),
                MolecularCharge.NEUTRAL,
                SpinMultiplicity.SINGLET));
    final ReactionConditions conditions =
        ReactionConditions.of(
            Temperature.of(5.0, TemperatureUnit.CELSIUS),
            null,
            TimeDuration.of(3.0, TimeDurationUnit.HOUR),
            List.of(
                ReactionConditionComponent.named(
                    ReactionComponentRole.ADDITIVE, "peracid oxidant")),
            List.of(
                ReactionConditionFeature.named(ReactionConditionFeatureKind.COOLING),
                ReactionConditionFeature.named(ReactionConditionFeatureKind.ADDITION_RATE),
                ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS)),
            "graph oxygen transfer, not an experimental yield prediction");
    final List<OrganicReactionDomain> domains =
        List.of(OrganicReactionDomain.OXIDATION_REDUCTION, OrganicReactionDomain.REARRANGEMENT);
    final ReactionSide reactants =
        ReactionSide.of(
            List.of(
                ReactionParticipant.of(substrate, StoichiometricCoefficient.ONE),
                ReactionParticipant.of(peracid, StoichiometricCoefficient.ONE)));
    final ReactionPredictionResult result =
        service.predict(
            ReactionPredictionRequest.of(
                "baeyer_villiger_explicit_peracid",
                reactants,
                conditions,
                domains,
                List.of(),
                null));
    final String ruleId = "bond_insertion_baeyer_villiger_oxygen";
    final var candidates =
        result.candidates().stream()
            .filter(candidate -> hasEvidenceContaining(candidate, ruleId))
            .toList();
    Assertions.assertFalse(candidates.isEmpty(), candidateSummary(result));
    Assertions.assertTrue(
        candidates.stream()
            .anyMatch(
                candidate -> {
                  final Molecule product =
                      candidate.products().participants().getFirst().molecule();
                  final AtomId oxygen = firstAtomIdByPrefix(product, "donor_", "O");
                  return !hasBond(product, "a6", "a8", BondType.SINGLE)
                      && hasBond(product, "a6", oxygen.value(), BondType.SINGLE)
                      && hasBond(product, oxygen.value(), "a8", BondType.SINGLE);
                }),
        "O must be inserted into the C-C bond adjacent to the carbonyl.");
    for (final var candidate : candidates) {
      Assertions.assertEquals(
          2, candidate.products().participants().size(), "Retain the acid coproduct.");
      final Molecule product = candidate.products().participants().getFirst().molecule();
      final Molecule acid = candidate.products().participants().getLast().molecule();
      Assertions.assertEquals(substrate.atomCount() + 1, product.atomCount());
      Assertions.assertEquals(substrate.bondCount() + 1, product.bondCount());
      Assertions.assertEquals(peracid.atomCount() - 1, acid.atomCount());
      Assertions.assertTrue(
          hasBond(product, "a6", "a7", BondType.DOUBLE), "Carbonyl is preserved.");
      Assertions.assertTrue(
          hasBond(product, "a8", "a9", BondType.DOUBLE),
          "Remote alkene is preserved by this channel.");
      Assertions.assertTrue(hasBond(acid, "d1", "d3", BondType.SINGLE));
      Assertions.assertTrue(
          acid.atoms().stream().noneMatch(atom -> atom.id().value().equals("d4")));
      final var balance =
          ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction.of(
                  ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionId.of(
                      "baeyer_balance"),
                  "Baeyer-Villiger",
                  reactants,
                  candidate.products())
              .balance();
      Assertions.assertTrue(
          balance.atomBalanced() && balance.chargeBalanced() && balance.massNumberBalanced());
      for (final var participant : candidate.products().participants()) {
        final var molecule = participant.molecule();
        Assertions.assertTrue(molecule.graph().connected());
        Assertions.assertTrue(
            molecule.atoms().stream().allMatch(atom -> atom.coordinate() == null));
        for (final var atom : molecule.atoms()) {
          final int valence =
              molecule.bonds().stream()
                  .filter(
                      bond ->
                          bond.firstAtomId().equals(atom.id())
                              || bond.secondAtomId().equals(atom.id()))
                  .mapToInt(OrganicReactionPredictionServiceTest::fixtureBondOrderTwice)
                  .sum();
          Assertions.assertEquals(
              switch (atom.element().symbol().value()) {
                    case "C" -> 4;
                    case "N" -> 3;
                    case "O" -> 2;
                    default -> 1;
                  }
                  * 2,
              valence);
        }
      }
    }
    final ReactionPredictionResult missingDonor =
        service.predict(
            ReactionPredictionRequest.of(
                "baeyer_villiger_named_reagent_only",
                ReactionSide.of(
                    List.of(ReactionParticipant.of(substrate, StoichiometricCoefficient.ONE))),
                conditions,
                domains,
                List.of(),
                null));
    Assertions.assertTrue(
        missingDonor.candidates().stream()
            .noneMatch(candidate -> hasEvidenceContaining(candidate, ruleId)),
        "A reagent name must not supply oxygen atoms.");
    Assertions.assertTrue(maxProbabilityWithEvidence(result, ruleId) > 0.0);
    assertNoCandidateEvidenceKind(result, ReactionPredictionEvidenceKind.SURROGATE_PRODUCT);
  }

  private static Molecule explicitHydrogens(final Molecule skeleton) {
    final ArrayList<Atom> atoms = new ArrayList<>(skeleton.atoms());
    final ArrayList<Bond> bonds = new ArrayList<>(skeleton.bonds());
    for (final var atom : skeleton.atoms()) {
      final int target =
          switch (atom.element().symbol().value()) {
            case "C" -> 4;
            case "N" -> 3;
            case "O" -> 2;
            default -> throw new IllegalArgumentException("Unsupported fixture element");
          };
      final int valence =
          skeleton.bonds().stream()
              .filter(
                  bond ->
                      bond.firstAtomId().equals(atom.id()) || bond.secondAtomId().equals(atom.id()))
              .mapToInt(OrganicReactionPredictionServiceTest::fixtureBondOrderTwice)
              .sum();
      Assertions.assertTrue(
          valence <= target * 2 && valence % 2 == 0, "Invalid explicit-H fixture valence");
      for (int count = valence / 2; count < target; count++) {
        final String hydrogen = "explicit_h" + atoms.size();
        atoms.add(atom(hydrogen, "H"));
        bonds.add(Bond.of(atom.id(), AtomId.of(hydrogen), BondType.SINGLE));
      }
    }
    return Molecule.of(
        skeleton.id(),
        skeleton.displayName(),
        atoms,
        bonds,
        skeleton.charge(),
        skeleton.spinMultiplicity());
  }

  private static int fixtureBondOrderTwice(final Bond bond) {
    return switch (bond.type()) {
      case SINGLE -> 2;
      case DOUBLE -> 4;
      case TRIPLE -> 6;
      case AROMATIC -> 3;
      default -> throw new IllegalArgumentException("Unsupported fixture bond");
    };
  }

  @Test
  void builtInAcylAminolysisGeneratesFragmentTransferAmideGraphProduct() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final Molecule acylScaffold =
        OrganicReactionPredictionServiceTest.carboxylAmineAlkeneMolecule();
    final Molecule amineFragment =
        OrganicReactionPredictionServiceTest.primaryAmine("aminolysis_ethylamine_fragment");
    final ReactionPredictionResult result =
        service.predict(
            ReactionPredictionRequest.of(
                "acyl_aminolysis_graph_fragment_transfer_screen",
                ReactionSide.of(
                    List.of(
                        ReactionParticipant.of(acylScaffold, StoichiometricCoefficient.ONE),
                        ReactionParticipant.of(amineFragment, StoichiometricCoefficient.ONE))),
                ReactionConditions.of(
                    Temperature.of(35.0, TemperatureUnit.CELSIUS),
                    null,
                    TimeDuration.of(2.0, TimeDurationUnit.HOUR),
                    List.of(
                        ReactionConditionComponent.named(
                            ReactionComponentRole.BASE, "tertiary amine base")),
                    List.of(
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.ADDITION_RATE)),
                    "graph acyl aminolysis product construction"),
                List.of(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
                List.of(),
                null));

    Assertions.assertTrue(
        result.candidates().stream()
            .anyMatch(
                candidate -> {
                  if (!OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                          candidate, "fragment_transfer_acyl_aminolysis_amide")
                      || !OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                          candidate, "graph fragment transfer")) {
                    return false;
                  }
                  final Molecule product = candidate.products().participants().get(0).molecule();
                  return product.id().value().contains("amide_graph_product")
                      && product.atomCount()
                          == acylScaffold.atomCount() + amineFragment.atomCount() - 2
                      && OrganicReactionPredictionServiceTest.totalAtomCount(candidate.products())
                          == acylScaffold.atomCount() + amineFragment.atomCount()
                      && product.bondCount() == 6
                      && candidate.products().participants().stream()
                          .anyMatch(
                              participant ->
                                  participant.molecule().id().value().contains("coproduct")
                                      && participant.molecule().atomCount() == 2
                                      && participant.molecule().bondCount() == 1)
                      && OrganicReactionPredictionServiceTest.hasBond(
                          product, "scaffold_a0", "fragment_n0", BondType.SINGLE)
                      && OrganicReactionPredictionServiceTest.atomCount(product, "N") >= 2;
                }),
        OrganicReactionPredictionServiceTest.candidateSummary(result));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.maxProbabilityWithEvidence(
                result, "fragment_transfer_acyl_aminolysis_amide")
            > 0.0);
    OrganicReactionPredictionServiceTest.assertNoCandidateEvidenceKind(
        result, ReactionPredictionEvidenceKind.SURROGATE_PRODUCT);
  }

  @Test
  void builtInCarbonylAmineCondensationKeepsEliminatedPairAsCoproduct() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final Molecule carbonyl = OrganicReactionPredictionServiceTest.carboxylAmineAlkeneMolecule();
    final Molecule amine =
        OrganicReactionPredictionServiceTest.primaryAmine("imine_condensation_amine_fragment");
    final ReactionPredictionResult result =
        service.predict(
            ReactionPredictionRequest.of(
                "carbonyl_amine_imine_condensation_graph_screen",
                ReactionSide.of(
                    List.of(
                        ReactionParticipant.of(carbonyl, StoichiometricCoefficient.ONE),
                        ReactionParticipant.of(amine, StoichiometricCoefficient.ONE))),
                ReactionConditions.of(
                    Temperature.of(80.0, TemperatureUnit.CELSIUS),
                    null,
                    TimeDuration.of(4.0, TimeDurationUnit.HOUR),
                    List.of(
                        ReactionConditionComponent.named(
                            ReactionComponentRole.ACID, "dry acid catalyst")),
                    List.of(
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.ACIDIC_MEDIUM),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS)),
                    "graph imine condensation should keep water-like coproduct"),
                List.of(
                    OrganicReactionDomain.ADDITION,
                    OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
                List.of(),
                null));

    Assertions.assertTrue(
        result.candidates().stream()
            .anyMatch(
                candidate -> {
                  if (!OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                      candidate, "fragment_transfer_carbonyl_amine_imine_condensation")) {
                    return false;
                  }
                  final Molecule product = candidate.products().participants().get(0).molecule();
                  return product.id().value().contains("imine_condensation_graph_product")
                      && product.atomCount() == carbonyl.atomCount() + amine.atomCount() - 2
                      && OrganicReactionPredictionServiceTest.totalAtomCount(candidate.products())
                          == carbonyl.atomCount() + amine.atomCount()
                      && OrganicReactionPredictionServiceTest.hasBond(
                          product, "scaffold_a0", "fragment_n0", BondType.DOUBLE)
                      && candidate.products().participants().stream()
                          .anyMatch(
                              participant ->
                                  participant.molecule().id().value().contains("coproduct")
                                      && participant.molecule().atomCount() == 2
                                      && participant.molecule().bondCount() == 1);
                }),
        OrganicReactionPredictionServiceTest.candidateSummary(result));
    OrganicReactionPredictionServiceTest.assertNoCandidateEvidenceKind(
        result, ReactionPredictionEvidenceKind.SURROGATE_PRODUCT);
  }

  @Test
  void builtInEtherDeprotectionGeneratesGraphBondCleavageProduct() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final Molecule etherSubstrate =
        OrganicReactionPredictionServiceTest.functionalGroupRichMolecule();
    final ReactionPredictionResult result =
        service.predict(
            ReactionPredictionRequest.of(
                "ether_graph_deprotection_cleavage_screen",
                ReactionSide.of(
                    List.of(ReactionParticipant.of(etherSubstrate, StoichiometricCoefficient.ONE))),
                ReactionConditions.of(
                    Temperature.of(25.0, TemperatureUnit.CELSIUS),
                    null,
                    TimeDuration.of(1.0, TimeDurationUnit.HOUR),
                    List.of(
                        ReactionConditionComponent.named(
                            ReactionComponentRole.ACID, "aqueous acid")),
                    List.of(
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.ACIDIC_MEDIUM),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.WORKUP)),
                    "graph ether deprotection cleavage"),
                List.of(OrganicReactionDomain.PROTECTION_DEPROTECTION),
                List.of(),
                null));

    Assertions.assertTrue(
        result.candidates().stream()
            .anyMatch(
                candidate -> {
                  if (!OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                          candidate, "bond_cleavage_ether_deprotection")
                      || !OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                          candidate, "graph bond cleavage")) {
                    return false;
                  }
                  return candidate.products().participants().stream()
                          .anyMatch(
                              participant ->
                                  participant
                                      .molecule()
                                      .id()
                                      .value()
                                      .contains("ether_cleaved_graph"))
                      && OrganicReactionPredictionServiceTest.totalAtomCount(candidate.products())
                          == etherSubstrate.atomCount()
                      && OrganicReactionPredictionServiceTest.totalBondCount(candidate.products())
                          == etherSubstrate.bondCount() - 1
                      && !OrganicReactionPredictionServiceTest.hasBond(
                          candidate.products(), "a2", "a3", BondType.SINGLE);
                }),
        OrganicReactionPredictionServiceTest.candidateSummary(result));
    OrganicReactionPredictionServiceTest.assertNoCandidateEvidenceKind(
        result, ReactionPredictionEvidenceKind.SURROGATE_PRODUCT);
  }

  @Test
  void builtInBetaEliminationGeneratesGraphUnsaturationProduct() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final Molecule alkylChloride =
        OrganicReactionPredictionServiceTest.betaEliminationAlkylChloride();
    final ReactionPredictionResult result =
        service.predict(
            ReactionPredictionRequest.of(
                "beta_elimination_graph_screen",
                ReactionSide.of(
                    List.of(ReactionParticipant.of(alkylChloride, StoichiometricCoefficient.ONE))),
                ReactionConditions.of(
                    Temperature.of(90.0, TemperatureUnit.CELSIUS),
                    null,
                    TimeDuration.of(2.0, TimeDurationUnit.HOUR),
                    List.of(
                        ReactionConditionComponent.named(ReactionComponentRole.BASE, "dry base")),
                    List.of(
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS)),
                    "graph beta-elimination from alkyl chloride"),
                List.of(OrganicReactionDomain.ELIMINATION),
                List.of(),
                null));

    OrganicReactionPredictionServiceTest.assertGraphBetaEliminationCandidate(result, alkylChloride);
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.maxProbabilityWithEvidence(
                result, "graph_beta_elimination_unsaturation")
            > 0.0);
    OrganicReactionPredictionServiceTest.assertNoCandidateEvidenceContaining(
        result, "Fallback single-site surrogate");
  }

  @Test
  void builtInRulesGenerateElectrophilicDiazoniumRedoxHydrolysisAndEliminationChannels() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final Molecule arylAmine =
        OrganicReactionPredictionServiceTest.chlorinatedAminoNitrogenHeterocycle(
            "aryl_amine_mechanism_probe");
    final ReactionPredictionResult acidElectrophilicResult =
        service.predict(
            OrganicReactionPredictionServiceTest.request(
                "acid_electrophilic_diazonium_screen",
                arylAmine,
                List.of(
                    OrganicReactionDomain.ELECTROPHILIC_SUBSTITUTION,
                    OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
                List.of(
                    ReactionConditionFeatureKind.ACIDIC_MEDIUM,
                    ReactionConditionFeatureKind.COOLING,
                    ReactionConditionFeatureKind.ADDITION_RATE,
                    ReactionConditionFeatureKind.CATALYST_PRESENT,
                    ReactionConditionFeatureKind.ANHYDROUS,
                    ReactionConditionFeatureKind.WORKUP)));
    final ReactionPredictionResult basicAzoResult =
        service.predict(
            OrganicReactionPredictionServiceTest.request(
                "basic_azo_screen",
                arylAmine,
                List.of(OrganicReactionDomain.ELECTROPHILIC_SUBSTITUTION),
                List.of(
                    ReactionConditionFeatureKind.BASIC_MEDIUM,
                    ReactionConditionFeatureKind.COOLING,
                    ReactionConditionFeatureKind.ADDITION_RATE)));
    final ReactionPredictionResult eliminationResult =
        service.predict(
            OrganicReactionPredictionServiceTest.request(
                "basic_elimination_screen",
                OrganicReactionPredictionServiceTest.betaEliminationAlkylChloride(),
                List.of(OrganicReactionDomain.ELIMINATION),
                List.of(
                    ReactionConditionFeatureKind.BASIC_MEDIUM,
                    ReactionConditionFeatureKind.HEATING,
                    ReactionConditionFeatureKind.ANHYDROUS)));
    final ReactionPredictionResult reductionResult =
        service.predict(
            OrganicReactionPredictionServiceTest.request(
                "functional_group_reduction_screen",
                OrganicReactionPredictionServiceTest.functionalGroupRichMolecule(),
                List.of(OrganicReactionDomain.OXIDATION_REDUCTION),
                List.of(
                    ReactionConditionFeatureKind.CATALYST_PRESENT,
                    ReactionConditionFeatureKind.HEATING,
                    ReactionConditionFeatureKind.INERT_ATMOSPHERE)));
    final ReactionPredictionResult oxidationResult =
        service.predict(
            OrganicReactionPredictionServiceTest.request(
                "functional_group_oxidation_screen",
                OrganicReactionPredictionServiceTest.functionalGroupRichMolecule(),
                List.of(OrganicReactionDomain.OXIDATION_REDUCTION),
                List.of(
                    ReactionConditionFeatureKind.COOLING,
                    ReactionConditionFeatureKind.ADDITION_RATE,
                    ReactionConditionFeatureKind.WORKUP)));
    final ReactionPredictionResult hydrolysisResult =
        service.predict(
            OrganicReactionPredictionServiceTest.request(
                "functional_group_hydrolysis_screen",
                OrganicReactionPredictionServiceTest.functionalGroupRichMolecule(),
                List.of(OrganicReactionDomain.ADDITION, OrganicReactionDomain.ACID_BASE),
                List.of(
                    ReactionConditionFeatureKind.ACIDIC_MEDIUM,
                    ReactionConditionFeatureKind.HEATING,
                    ReactionConditionFeatureKind.WORKUP)));

    Assertions.assertTrue(
        acidElectrophilicResult.hasCandidates() || acidElectrophilicResult.hasMechanismChannels());
    Assertions.assertTrue(basicAzoResult.hasCandidates() || basicAzoResult.hasMechanismChannels());
    Assertions.assertTrue(
        eliminationResult.hasCandidates() || eliminationResult.hasMechanismChannels());
    Assertions.assertTrue(
        reductionResult.hasCandidates() || reductionResult.hasMechanismChannels());
    Assertions.assertTrue(
        oxidationResult.hasCandidates() || oxidationResult.hasMechanismChannels());
    Assertions.assertTrue(
        hydrolysisResult.hasCandidates() || hydrolysisResult.hasMechanismChannels());
    OrganicReactionPredictionServiceTest.assertRecognizedChannel(
        acidElectrophilicResult, "transform_aromatic_nitration");
    OrganicReactionPredictionServiceTest.assertRecognizedChannel(
        acidElectrophilicResult, "transform_aromatic_sulfonation");
    OrganicReactionPredictionServiceTest.assertRecognizedChannel(
        acidElectrophilicResult, "transform_friedel_crafts_alkylation");
    OrganicReactionPredictionServiceTest.assertRecognizedChannel(
        acidElectrophilicResult, "replace_aryl_diazonium_hydroxy");
    OrganicReactionPredictionServiceTest.assertRecognizedChannel(
        acidElectrophilicResult, "replace_aryl_diazonium_halide");
    OrganicReactionPredictionServiceTest.assertRecognizedChannel(
        basicAzoResult, "transform_azo_coupling_nitrogen_attachment");
    OrganicReactionPredictionServiceTest.assertRecognizedChannel(
        eliminationResult, "graph_beta_elimination_unsaturation");
    OrganicReactionPredictionServiceTest.assertRecognizedChannel(
        reductionResult, "transform_nitro_reduction");
    OrganicReactionPredictionServiceTest.assertRecognizedChannel(
        oxidationResult, "transform_alcohol_oxidation");
    OrganicReactionPredictionServiceTest.assertRecognizedChannel(
        hydrolysisResult, "transform_amide_hydrolysis");
    Assertions.assertTrue(
        acidElectrophilicResult.candidates().stream()
            .flatMap(candidate -> candidate.evidence().stream())
            .noneMatch(
                evidence -> evidence.kind() == ReactionPredictionEvidenceKind.CALIBRATION_PRIOR));
    Assertions.assertTrue(
        reductionResult.candidates().stream()
            .flatMap(candidate -> candidate.evidence().stream())
            .noneMatch(
                evidence -> evidence.kind() == ReactionPredictionEvidenceKind.CALIBRATION_PRIOR));
    Assertions.assertTrue(
        oxidationResult.candidates().stream()
            .flatMap(candidate -> candidate.evidence().stream())
            .noneMatch(
                evidence -> evidence.kind() == ReactionPredictionEvidenceKind.CALIBRATION_PRIOR));
    Assertions.assertTrue(
        hydrolysisResult.candidates().stream()
            .flatMap(candidate -> candidate.evidence().stream())
            .noneMatch(
                evidence -> evidence.kind() == ReactionPredictionEvidenceKind.CALIBRATION_PRIOR));
  }

  @Test
  void ensembleNormalizerBoostsRelativeKineticThermodynamicChannel() {
    final Molecule fastProduct =
        OrganicReactionPredictionServiceTest.twoCarbonMolecule("fast_product", BondType.DOUBLE);
    final Molecule slowProduct =
        OrganicReactionPredictionServiceTest.twoCarbonMolecule("slow_product", BondType.SINGLE);
    final ReactionProductCandidate fastCandidate =
        OrganicReactionPredictionServiceTest.candidate(fastProduct, 0.5, "fast channel");
    final ReactionProductCandidate slowCandidate =
        OrganicReactionPredictionServiceTest.candidate(slowProduct, 0.5, "slow channel");
    final ReactionCandidateScore fastScore =
        ReactionCandidateScore.of(
            fastCandidate,
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.THERMODYNAMIC_FACTOR, "exergonic channel", 0.12),
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.KINETIC_FACTOR, "low barrier channel", 0.10),
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
                    "matched polar electronics",
                    0.08)),
            1.0,
            0);
    final ReactionCandidateScore slowScore =
        ReactionCandidateScore.of(
            slowCandidate,
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.THERMODYNAMIC_FACTOR,
                    "endergonic channel",
                    -0.10),
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.KINETIC_FACTOR, "high barrier channel", -0.09),
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.COMPETING_PATHWAY,
                    "competing pathway dominates",
                    -0.06)),
            1.0,
            1);

    final List<ReactionCandidateScore> adjusted =
        new ReactionCandidateEnsembleNormalizer().adjust(List.of(fastScore, slowScore));

    Assertions.assertEquals(2, adjusted.size());
    Assertions.assertTrue(adjusted.get(0).score() > adjusted.get(1).score());
    Assertions.assertTrue(
        adjusted.get(0).score() / fastScore.score() > adjusted.get(1).score() / slowScore.score());
    Assertions.assertTrue(
        adjusted.get(0).evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.QUANTUM_ENERGY_MODEL
                        && evidence.description().contains("Candidate ensemble adjustment")));
    Assertions.assertTrue(
        adjusted.get(1).evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.QUANTUM_ENERGY_MODEL
                        && evidence.description().contains("Candidate ensemble adjustment")));
  }

  @Test
  void ensembleNormalizerDoesNotUseBenchmarkObservationsAsPredictionAnswers() {
    final Molecule literatureMajorProduct =
        OrganicReactionPredictionServiceTest.twoCarbonMolecule(
            "literature_major_product", BondType.DOUBLE);
    final Molecule uncalibratedFastProduct =
        OrganicReactionPredictionServiceTest.twoCarbonMolecule(
            "uncalibrated_fast_product", BondType.SINGLE);
    final ReactionProductCandidate literatureMajorCandidate =
        OrganicReactionPredictionServiceTest.candidate(
            literatureMajorProduct, 0.5, "literature major channel");
    final ReactionProductCandidate uncalibratedFastCandidate =
        OrganicReactionPredictionServiceTest.candidate(
            uncalibratedFastProduct, 0.5, "uncalibrated fast channel");
    final ReactionCandidateScore literatureMajorScore =
        ReactionCandidateScore.of(
            literatureMajorCandidate,
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.CALIBRATION_PRIOR,
                    "Experimental heterocycle product distribution reports this as the major channel.",
                    0.38),
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.KINETIC_FACTOR,
                    "Moderate barrier channel.",
                    0.01)),
            1.0,
            0);
    final ReactionCandidateScore uncalibratedFastScore =
        ReactionCandidateScore.of(
            uncalibratedFastCandidate,
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.KINETIC_FACTOR,
                    "Low barrier but no product-distribution calibration.",
                    0.10),
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.THERMODYNAMIC_FACTOR,
                    "Mildly favorable bond-change thermodynamics.",
                    0.03)),
            1.0,
            1);

    final List<ReactionCandidateScore> adjusted =
        new ReactionCandidateEnsembleNormalizer()
            .adjust(List.of(literatureMajorScore, uncalibratedFastScore));

    Assertions.assertEquals(2, adjusted.size());
    Assertions.assertTrue(
        adjusted.get(1).score() > adjusted.get(0).score(),
        "External product-distribution observations are benchmarks, not hidden ranking answers.");
    Assertions.assertTrue(
        adjusted.get(1).evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.QUANTUM_ENERGY_MODEL
                        && evidence.description().contains("mechanism-aware logit")));
  }

  @Test
  void ensembleNormalizerSoftensCloseCompetitiveSelectivityChannels() {
    final Molecule firstProduct =
        OrganicReactionPredictionServiceTest.twoCarbonMolecule(
            "close_competition_first", BondType.DOUBLE);
    final Molecule secondProduct =
        OrganicReactionPredictionServiceTest.twoCarbonMolecule(
            "close_competition_second", BondType.SINGLE);
    final ReactionCandidateScore firstScore =
        ReactionCandidateScore.of(
            OrganicReactionPredictionServiceTest.candidate(firstProduct, 0.5, "first regioisomer"),
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.KINETIC_FACTOR,
                    "Local site accessibility supports this regioisomer.",
                    0.035)),
            1.0,
            0);
    final ReactionCandidateScore secondScore =
        ReactionCandidateScore.of(
            OrganicReactionPredictionServiceTest.candidate(
                secondProduct, 0.5, "second regioisomer"),
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.COMPETING_PATHWAY,
                    "Side-product and regioisomer competition remains under the declared conditions.",
                    -0.015)),
            1.0,
            1);

    final List<ReactionCandidateScore> adjusted =
        new ReactionCandidateEnsembleNormalizer().adjust(List.of(firstScore, secondScore));

    Assertions.assertEquals(2, adjusted.size());
    Assertions.assertTrue(adjusted.get(0).score() > adjusted.get(1).score());
    Assertions.assertTrue(adjusted.get(0).score() / adjusted.get(1).score() < 1.35);
    Assertions.assertTrue(
        adjusted.get(0).evidence().stream()
            .anyMatch(evidence -> evidence.description().contains("ensemble temperature=1.35")));
    Assertions.assertTrue(
        adjusted.get(1).evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.WARNING
                        && evidence.description().contains("Competitive product ensemble")));
  }

  @Test
  void ensembleNormalizerUsesQuantitativeRecipeEvidenceAsMechanisticSignal() {
    final Molecule matchedProduct =
        OrganicReactionPredictionServiceTest.twoCarbonMolecule(
            "quant_recipe_matched_product", BondType.DOUBLE);
    final Molecule unmatchedProduct =
        OrganicReactionPredictionServiceTest.twoCarbonMolecule(
            "quant_recipe_unmatched_product", BondType.SINGLE);
    final ReactionCandidateScore matchedScore =
        ReactionCandidateScore.of(
            OrganicReactionPredictionServiceTest.candidate(
                matchedProduct, 0.5, "matched recipe channel"),
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.KINETIC_FACTOR,
                    "Quantified reactant recipe is fully matched to molecular reactants; limiting-reagent context is available.",
                    0.055)),
            1.0,
            0);
    final ReactionCandidateScore unmatchedScore =
        ReactionCandidateScore.of(
            OrganicReactionPredictionServiceTest.candidate(
                unmatchedProduct, 0.5, "unmatched recipe channel"),
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.WARNING,
                    "Quantified recipe is only partially matched to reactant molecules.",
                    -0.045)),
            1.0,
            1);

    final List<ReactionCandidateScore> adjusted =
        new ReactionCandidateEnsembleNormalizer().adjust(List.of(matchedScore, unmatchedScore));

    Assertions.assertTrue(adjusted.get(0).score() > adjusted.get(1).score());
    Assertions.assertTrue(
        adjusted.get(0).evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.QUANTUM_ENERGY_MODEL
                        && evidence
                            .description()
                            .contains("relative kinetic/thermodynamic share")));
  }

  @Test
  void ensembleNormalizerUsesPredictedMechanismRoutesAsMechanisticSignal() {
    final Molecule routeSupportedProduct =
        OrganicReactionPredictionServiceTest.twoCarbonMolecule(
            "route_supported_product", BondType.DOUBLE);
    final Molecule unsupportedProduct =
        OrganicReactionPredictionServiceTest.twoCarbonMolecule(
            "route_unsupported_product", BondType.SINGLE);
    final ReactionProductCandidate routeSupportedCandidate =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(routeSupportedProduct, StoichiometricCoefficient.ONE))),
            0.5,
            50.0,
            "mechanism route supported channel",
            List.of(),
            List.of(
                PredictedReactionMechanism.of(
                    "route_supported_snar",
                    ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION,
                    0.72,
                    63.0),
                PredictedReactionMechanism.of(
                    "route_supported_anrorc",
                    ReactionMechanismFamily.ADDITION_RING_OPENING_RING_CLOSURE,
                    0.18,
                    11.0)));
    final ReactionCandidateScore routeSupportedScore =
        ReactionCandidateScore.of(routeSupportedCandidate, List.of(), 1.0, 0);
    final ReactionCandidateScore unsupportedScore =
        ReactionCandidateScore.of(
            OrganicReactionPredictionServiceTest.candidate(
                unsupportedProduct, 0.5, "unsupported route channel"),
            List.of(),
            1.0,
            1);

    final List<ReactionCandidateScore> adjusted =
        new ReactionCandidateEnsembleNormalizer()
            .adjust(List.of(routeSupportedScore, unsupportedScore));

    Assertions.assertTrue(adjusted.get(0).score() > adjusted.get(1).score());
    Assertions.assertTrue(
        adjusted.get(0).evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.QUANTUM_ENERGY_MODEL
                        && evidence.description().contains("mechanism-aware logit")));
  }

  @Test
  void builtInHeterocycleLibraryDoesNotUseCuratedOutcomeRules() {
    final OrganicReactionRuleLibrary library =
        BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction();

    Assertions.assertFalse(
        library.rules().stream().anyMatch(CuratedProductOrganicReactionRule.class::isInstance),
        "Built-in product prediction must use generic mechanism and graph rules, not curated outcome lookup.");
  }

  @Test
  void candidateScorerIgnoresCalibrationEvidenceWeightInPredictionScore() {
    final Molecule reactant =
        OrganicReactionPredictionServiceTest.twoCarbonMolecule(
            "calibration_score_reactant", BondType.SINGLE);
    final Molecule product =
        OrganicReactionPredictionServiceTest.twoCarbonMolecule(
            "calibration_score_product", BondType.DOUBLE);
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "calibration_weight_is_audit_only",
            ReactionSide.of(
                List.of(ReactionParticipant.of(reactant, StoichiometricCoefficient.ONE))),
            ReactionConditions.EMPTY,
            List.of(OrganicReactionDomain.UNKNOWN),
            List.of(),
            "Benchmark rows validate predictions, but they do not create prediction probability.");
    final ReactionProductCandidate benchmarkCandidate =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(ReactionParticipant.of(product, StoichiometricCoefficient.ONE))),
            0.5,
            50.0,
            "candidate with external benchmark trace",
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.CALIBRATION_PRIOR,
                    "Synthetic benchmark trace with intentionally huge non-production weight.",
                    0.95)));
    final ReactionProductCandidate uncalibratedCandidate =
        ReactionProductCandidate.of(
            benchmarkCandidate.products(),
            0.5,
            50.0,
            "candidate without benchmark trace",
            List.of());
    final OrganicReactionCandidateScorer scorer = new OrganicReactionCandidateScorer();
    final ReactionCandidateScore benchmarkScore = scorer.score(request, benchmarkCandidate, 0);
    final ReactionCandidateScore uncalibratedScore =
        scorer.score(request, uncalibratedCandidate, 0);

    Assertions.assertEquals(uncalibratedScore.score(), benchmarkScore.score(), 1.0E-12);
    Assertions.assertEquals(
        0.0,
        OrganicReactionCandidateScorer.predictionWeight(benchmarkCandidate.evidence().get(0)),
        1.0E-12);
  }

  @Test
  void candidateScorerDoesNotAttachCalibrationPriorFromSeparateAuditLibrary() {
    final Molecule reactant =
        OrganicReactionPredictionServiceTest.twoCarbonMolecule(
            "audit_only_reactant", BondType.SINGLE);
    final Molecule product =
        OrganicReactionPredictionServiceTest.twoCarbonMolecule(
            "audit_only_product", BondType.DOUBLE);
    final ReactionProductCandidate candidate =
        OrganicReactionPredictionServiceTest.candidate(product, 0.5, "mechanistic candidate");
    final ReactionOutcomeCalibrationLibrary library =
        ReactionOutcomeCalibrationLibrary.of(
            List.of(
                ReactionOutcomeCalibrationObservation.of(
                    "audit_only_observation",
                    List.of(OrganicReactionDomain.UNKNOWN),
                    ReactionOutcomeCalibrationScorer.productFormulaSignature(candidate),
                    0.8,
                    70.0,
                    "synthetic audit row",
                    "Observed distribution must stay outside prediction evidence.")));
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "audit_library_not_prediction_input",
            ReactionSide.of(
                List.of(ReactionParticipant.of(reactant, StoichiometricCoefficient.ONE))),
            ReactionConditions.EMPTY,
            List.of(OrganicReactionDomain.UNKNOWN),
            List.of(),
            "Prediction scorer must not use observed outcome rows as answers.");
    final OrganicReactionCandidateScorer scorer =
        new OrganicReactionCandidateScorer(new MolecularFeatureAnalyzer());

    final ReactionCandidateScore score = scorer.score(request, candidate, 0);

    Assertions.assertFalse(library.observations().isEmpty());
    Assertions.assertTrue(score.score() > 0.0);
    Assertions.assertTrue(
        score.evidence().stream()
            .noneMatch(
                evidence -> evidence.kind() == ReactionPredictionEvidenceKind.CALIBRATION_PRIOR));
  }

  @Test
  void calibrationApplierReweightsMatchingCandidateAndKeepsScoreBreakdownConsistent() {
    final Molecule reactant =
        OrganicReactionPredictionServiceTest.ringMolecule("calibration_applier_reactant", "N");
    final Molecule matchedProduct =
        OrganicReactionPredictionServiceTest.ringMolecule(
            "calibration_applier_matched_product", "N");
    final Molecule competingProduct =
        OrganicReactionPredictionServiceTest.twoCarbonMolecule(
            "calibration_applier_competing_product", BondType.SINGLE);
    final ReactionProductCandidate matchedCandidate =
        OrganicReactionPredictionServiceTest.candidate(
            matchedProduct, 0.5, "matched calibration candidate");
    final ReactionProductCandidate competingCandidate =
        OrganicReactionPredictionServiceTest.candidate(
            competingProduct, 0.5, "competing candidate");
    final ReactionPredictionRequest request =
        OrganicReactionPredictionServiceTest.request(
            "calibration_applier_request",
            reactant,
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(ReactionConditionFeatureKind.HEATING));
    final ReactionPredictionResult result =
        ReactionPredictionResult.of(
            request,
            ReactionPredictionMethodType.HYBRID,
            List.of(matchedCandidate, competingCandidate),
            List.of());
    final ReactionOutcomeCalibrationLibrary library =
        ReactionOutcomeCalibrationLibrary.of(
            List.of(
                ReactionOutcomeCalibrationObservation.of(
                    "calibration_applier_observation",
                    List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
                    ReactionOutcomeCalibrationScorer.productFormulaSignature(matchedCandidate),
                    0.82,
                    78.0,
                    "Synthetic local calibration fixture.",
                    "Calibration should softly affect probabilities through applier.")));

    final ReactionPredictionResult calibrated =
        new ReactionPredictionCalibrationApplier(library).apply(result, true);

    Assertions.assertEquals(1.0, calibrated.probabilityMass(), 1.0E-12);
    Assertions.assertTrue(calibrated.candidates().get(0).probability() > 0.5);
    Assertions.assertTrue(calibrated.candidates().get(1).probability() < 0.5);
    Assertions.assertEquals(
        calibrated.candidates().get(0).probability(),
        calibrated.candidates().get(0).scoreBreakdown().normalizedProbability(),
        1.0E-12);
    Assertions.assertTrue(
        calibrated
            .candidates()
            .get(0)
            .hasEvidence(ReactionPredictionEvidenceKind.CALIBRATION_PRIOR));
  }

  @Test
  void candidateScorerUsesStructuredStoichiometryWithoutProductLookup() {
    final Molecule limitingReactant =
        OrganicReactionPredictionServiceTest.twoCarbonMolecule(
            "limiting_reactant", BondType.SINGLE);
    final Molecule excessReactant =
        OrganicReactionPredictionServiceTest.twoCarbonMolecule("excess_reactant", BondType.DOUBLE);
    final Molecule product =
        OrganicReactionPredictionServiceTest.twoCarbonMolecule(
            "stoichiometry_ranked_product", BondType.SINGLE);
    final ReactionPredictionRequest matchedRequest =
        ReactionPredictionRequest.of(
            "structured_stoichiometry_matched",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(limitingReactant, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(excessReactant, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(120.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(3.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.of(
                        ReactionComponentRole.REACTANT,
                        "limiting_reactant",
                        SubstanceAmount.of(10.0, SubstanceAmountUnit.GRAM),
                        null,
                        StoichiometricEquivalent.of(1.0),
                        null,
                        ReactionComponentPurity.percent(98.0),
                        ReactionPhase.LIQUID,
                        "quantified limiting reagent"),
                    ReactionConditionComponent.of(
                        ReactionComponentRole.REACTANT,
                        "excess_reactant",
                        SubstanceAmount.of(16.0, SubstanceAmountUnit.GRAM),
                        null,
                        StoichiometricEquivalent.of(1.4),
                        null,
                        ReactionComponentPurity.percent(97.0),
                        ReactionPhase.LIQUID,
                        "quantified excess reagent"),
                    ReactionConditionComponent.of(
                        ReactionComponentRole.SOLVENT,
                        "PEG4000",
                        SubstanceAmount.of(70.0, SubstanceAmountUnit.GRAM),
                        null,
                        null,
                        null,
                        null,
                        ReactionPhase.LIQUID,
                        "structured solvent mass")),
                List.of(ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING)),
                "structured amount/equivalent recipe"),
            List.of(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            List.of(),
            "Structured stoichiometry must influence confidence, but must not identify the product.");
    final ReactionPredictionRequest unmatchedRequest =
        ReactionPredictionRequest.of(
            "structured_stoichiometry_unmatched",
            matchedRequest.reactants(),
            ReactionConditions.of(
                matchedRequest.conditions().temperature(),
                null,
                matchedRequest.conditions().duration(),
                List.of(
                    ReactionConditionComponent.of(
                        ReactionComponentRole.REACTANT,
                        "unknown_external_reagent",
                        SubstanceAmount.of(10.0, SubstanceAmountUnit.GRAM),
                        null,
                        StoichiometricEquivalent.of(1.0),
                        null,
                        ReactionComponentPurity.percent(88.0),
                        ReactionPhase.LIQUID,
                        "unmatched quantified reagent")),
                matchedRequest.conditions().features(),
                "unmatched recipe"),
            matchedRequest.domains(),
            List.of(),
            "Unmatched recipe quantities must lower confidence.");
    final OrganicReactionCandidateScorer scorer = new OrganicReactionCandidateScorer();
    final ReactionProductCandidate candidate =
        OrganicReactionPredictionServiceTest.candidate(product, 0.5, "stoichiometry candidate");
    final ReactionPredictionRequest serviceRequest =
        ReactionPredictionRequest.of(
            "structured_stoichiometry_result_breakdown",
            matchedRequest.reactants(),
            matchedRequest.conditions(),
            matchedRequest.domains(),
            List.of(candidate),
            "Structured stoichiometry must appear in public result breakdown.");

    final ReactionCandidateScore matchedScore = scorer.score(matchedRequest, candidate, 0);
    final ReactionCandidateScore unmatchedScore = scorer.score(unmatchedRequest, candidate, 0);
    final ReactionPredictionResult result =
        new OrganicReactionPredictionService(new MolecularFeatureAnalyzer())
            .predict(serviceRequest);

    Assertions.assertTrue(matchedScore.score() > unmatchedScore.score());
    Assertions.assertTrue(
        matchedScore.evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.KINETIC_FACTOR
                        && evidence
                            .description()
                            .contains("Quantified reactant recipe is fully matched")));
    Assertions.assertTrue(
        matchedScore.evidence().stream()
            .anyMatch(
                evidence -> evidence.description().contains("Gram-scale recipe is represented")));
    Assertions.assertTrue(
        unmatchedScore.evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.WARNING
                        && evidence.description().contains("only partially matched")));
    Assertions.assertTrue(
        result
            .bestCandidate()
            .scoreBreakdown()
            .hasComponent(ReactionPredictionScoreComponentKind.STOICHIOMETRY));
  }

  @Test
  void candidateScorerInfersMechanismFamiliesFromPhysicalRegimeAndConditionNote() {
    final Molecule reactant =
        OrganicReactionPredictionServiceTest.twoCarbonMolecule(
            "regime_note_reactant", BondType.SINGLE);
    final Molecule product =
        OrganicReactionPredictionServiceTest.twoCarbonMolecule(
            "regime_note_product", BondType.DOUBLE);
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "physical_regime_note_inferred_mechanisms",
            ReactionSide.of(
                List.of(ReactionParticipant.of(reactant, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(120.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(3.0, TimeDurationUnit.HOUR),
                List.of(),
                List.of(),
                "PEG4000 sealed microwave reaction with polar aprotic medium and prolonged heating"),
            List.of(OrganicReactionDomain.UNKNOWN),
            List.of(),
            "Physical regime should infer mechanism families without manual domain preselection.");
    final ReactionProductCandidate candidate =
        OrganicReactionPredictionServiceTest.candidate(product, 0.45, "regime candidate");

    final ReactionCandidateScore score =
        new OrganicReactionCandidateScorer().score(request, candidate, 0);

    Assertions.assertTrue(score.score() > candidate.probability());
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasEvidenceText(
            score, "Nucleophilic aromatic substitution mechanism contributes"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasEvidenceText(
            score, "Heterocycle construction mechanism contributes"));
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.hasEvidenceText(
            score, "Tautomerization mechanism contributes"));
  }

  @Test
  void predictionServiceEnrichesUnknownDomainFromStructureAndConditions() {
    final Molecule reactant =
        OrganicReactionPredictionServiceTest.chlorinatedAminoNitrogenHeterocycle(
            "auto_domain_chloro_heterocycle");
    final Molecule product =
        OrganicReactionPredictionServiceTest.ringMolecule("auto_domain_predicted_product", "N");
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                OrganicReactionRuleLibrary.of(List.of(new FixedProductRule(product)))));
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "auto_domain_prediction_request",
            ReactionSide.of(
                List.of(ReactionParticipant.of(reactant, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(120.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(3.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.of(
                        ReactionComponentRole.SOLVENT,
                        "PEG4000",
                        SubstanceAmount.of(70.0, SubstanceAmountUnit.GRAM),
                        null,
                        null,
                        null,
                        ReactionComponentPurity.percent(99.0),
                        ReactionPhase.LIQUID,
                        "polar aprotic polymer solvent"),
                    ReactionConditionComponent.named(ReactionComponentRole.BASE, "K2CO3")),
                List.of(ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING)),
                "benzylamine-like nucleophile under prolonged heated PEG medium"),
            List.of(OrganicReactionDomain.UNKNOWN),
            List.of(),
            "The prediction service must infer practical organic domains from the input graph.");

    final ReactionPredictionResult result = service.predict(request);

    Assertions.assertTrue(
        result.request().hasDomain(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION));
    Assertions.assertTrue(
        result.request().hasDomain(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION));
    Assertions.assertTrue(result.request().hasDomain(OrganicReactionDomain.ACID_BASE));
    Assertions.assertTrue(result.request().hasDomain(OrganicReactionDomain.CYCLIZATION));
    Assertions.assertTrue(result.request().hasDomain(OrganicReactionDomain.ELIMINATION));
    Assertions.assertFalse(result.request().hasDomain(OrganicReactionDomain.CROSS_COUPLING));
    Assertions.assertFalse(result.request().hasDomain(OrganicReactionDomain.UNKNOWN));
    Assertions.assertTrue(result.hasCandidates());
    Assertions.assertTrue(
        result.diagnostics().stream()
            .noneMatch(evidence -> evidence.description().contains("domain is UNKNOWN")));
  }

  @Test
  void predictionServiceInfersCrossCouplingOnlyFromPartnerOrCatalystSignals() {
    final Molecule scaffold = OrganicReactionPredictionServiceTest.dichloroHeterocycle();
    final Molecule organoboron =
        OrganicReactionPredictionServiceTest.twoAtomMolecule(
            "auto_domain_boronic_partner", "C", "B", BondType.SINGLE);
    final Molecule product =
        OrganicReactionPredictionServiceTest.dehalogenatedScaffold(
            scaffold, "auto_domain_cross_coupling_product");
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                OrganicReactionRuleLibrary.of(List.of(new FixedProductRule(product)))));
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "auto_domain_cross_coupling_request",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(scaffold, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(organoboron, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(80.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(4.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.named(ReactionComponentRole.CATALYST, "Pd catalyst"),
                    ReactionConditionComponent.named(ReactionComponentRole.BASE, "K2CO3")),
                List.of(),
                "Suzuki-like palladium ligand screen"),
            List.of(OrganicReactionDomain.UNKNOWN),
            List.of(),
            "Cross-coupling inference requires organoboron/organometallic partner or catalyst signal.");

    final ReactionPredictionResult result = service.predict(request);

    Assertions.assertTrue(result.request().hasDomain(OrganicReactionDomain.CROSS_COUPLING));
    Assertions.assertTrue(
        result.request().hasDomain(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION));
    Assertions.assertTrue(
        result.request().hasDomain(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION));
    Assertions.assertFalse(result.request().hasDomain(OrganicReactionDomain.UNKNOWN));
    Assertions.assertTrue(result.hasCandidates());
  }

  /** Inventory-compatible synthetic graph for ranking/merging tests, not a reaction model. */
  private static Molecule dehalogenatedScaffold(final Molecule source, final String id) {
    final var atoms =
        source.atoms().stream()
            .filter(
                atom -> !List.of("F", "Cl", "Br", "I").contains(atom.element().symbol().value()))
            .toList();
    final var ids = atoms.stream().map(Atom::id).toList();
    final var bonds =
        source.bonds().stream()
            .filter(bond -> ids.contains(bond.firstAtomId()) && ids.contains(bond.secondAtomId()))
            .toList();
    return Molecule.of(
        MoleculeId.of(id), id, atoms, bonds, source.charge(), source.spinMultiplicity());
  }

  private static ReactionProductCandidate candidate(
      final Molecule molecule, final double probability, final String note) {
    return ReactionProductCandidate.of(
        ReactionSide.of(List.of(ReactionParticipant.of(molecule, StoichiometricCoefficient.ONE))),
        probability,
        probability * 100.0,
        note,
        List.of(
            ReactionPredictionEvidence.of(
                ReactionPredictionEvidenceKind.USER_RULE, "candidate supplied by screen", 0.1)));
  }

  private static boolean hasEvidenceText(
      final ReactionCandidateScore score, final String expected) {
    final List<ReactionPredictionEvidence> evidence = score.evidence();
    for (int i = 0; i < evidence.size(); ++i) {
      if (evidence.get(i).description().contains(expected)) {
        return true;
      }
    }
    return false;
  }

  private static double evidenceWeightContaining(
      final ReactionCandidateScore score, final String text) {
    return score.evidence().stream()
        .filter(evidence -> evidence.description().contains(text))
        .mapToDouble(ReactionPredictionEvidence::weight)
        .findFirst()
        .orElseThrow();
  }

  private static ReactionPredictionRequest request(
      final String id,
      final Molecule reactant,
      final List<OrganicReactionDomain> domains,
      final List<ReactionConditionFeatureKind> conditionFeatures) {
    final java.util.ArrayList<ReactionConditionFeature> features =
        new java.util.ArrayList<ReactionConditionFeature>(conditionFeatures.size());
    for (int i = 0; i < conditionFeatures.size(); ++i) {
      features.add(ReactionConditionFeature.named(conditionFeatures.get(i)));
    }
    return ReactionPredictionRequest.of(
        id,
        ReactionSide.of(List.of(ReactionParticipant.of(reactant, StoichiometricCoefficient.ONE))),
        ReactionConditions.of(
            Temperature.of(80.0, TemperatureUnit.CELSIUS),
            null,
            TimeDuration.of(6.0, TimeDurationUnit.HOUR),
            List.of(),
            List.copyOf(features),
            id),
        domains,
        List.of(),
        null);
  }

  private static ReactionPredictionRequest withExplicitPartner(
      final ReactionPredictionRequest request, final Molecule partner) {
    final var participants = new java.util.ArrayList<>(request.reactants().participants());
    participants.add(ReactionParticipant.of(partner, StoichiometricCoefficient.ONE));
    return ReactionPredictionRequest.of(
        request.id(),
        ReactionSide.of(participants),
        request.conditions(),
        request.domains(),
        request.seedCandidates(),
        request.note());
  }

  private static void assertDiagnosticOnly(
      final ReactionPredictionResult result, final String ruleId) {
    final var channels =
        result.mechanismChannels().stream()
            .filter(channel -> channel.channelId().equals(ruleId))
            .toList();
    Assertions.assertEquals(1, channels.size(), "Expected one diagnostic-only channel: " + ruleId);
    Assertions.assertFalse(channels.getFirst().explicitProductGraphAvailable(), ruleId);
    Assertions.assertFalse(channels.getFirst().materializationRequirement().isBlank(), ruleId);
    Assertions.assertTrue(
        result.diagnostics().stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.WARNING
                        && evidence.description().contains(ruleId)),
        "Missing explanation for withheld product: " + ruleId);
    assertNoCandidateEvidenceContaining(result, ruleId);
  }

  private static void assertGeneratedBranch(
      final ReactionPredictionResult result,
      final String ruleId,
      final String expectedElementSymbol,
      final int expectedAtomCount) {
    final ReactionProductCandidate candidate =
        result.candidates().stream()
            .filter(
                candidateItem ->
                    OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                        candidateItem, ruleId))
            .findFirst()
            .orElseThrow(
                () ->
                    new AssertionError(
                        "Missing generated branch "
                            + ruleId
                            + " in candidates: "
                            + OrganicReactionPredictionServiceTest.candidateEvidenceSummary(
                                result)));
    final Molecule product = candidate.products().participants().get(0).molecule();
    Assertions.assertEquals(expectedAtomCount, product.atomCount());
    Assertions.assertTrue(
        OrganicReactionPredictionServiceTest.atomCount(product, expectedElementSymbol) > 0);
    Assertions.assertTrue(candidate.probability() > 0.0);
    Assertions.assertTrue(candidate.expectedYieldPercent() > 0.0);
  }

  private static void assertRecognizedChannel(
      final ReactionPredictionResult result, final String ruleId) {
    if (result.mechanismChannels().stream()
        .anyMatch(
            channel ->
                channel.channelId().equals(ruleId)
                    && !channel.explicitProductGraphAvailable()
                    && !channel.materializationRequirement().isBlank())) {
      Assertions.assertFalse(
          result.candidates().stream()
              .anyMatch(
                  candidate ->
                      OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                          candidate, ruleId)),
          "A mechanism-only rule must not also supply a placeholder product: " + ruleId);
      return;
    }
    OrganicReactionPredictionServiceTest.assertGeneratedChannel(result, ruleId);
  }

  private static void assertGeneratedChannel(
      final ReactionPredictionResult result, final String ruleId) {
    Assertions.assertTrue(
        result.candidates().stream()
            .anyMatch(
                candidate ->
                    OrganicReactionPredictionServiceTest.hasEvidenceContaining(candidate, ruleId)),
        OrganicReactionPredictionServiceTest.candidateEvidenceSummary(result));
  }

  private static String candidateEvidenceSummary(final ReactionPredictionResult result) {
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < result.candidates().size(); ++i) {
      final ReactionProductCandidate candidate = result.candidates().get(i);
      builder.append("#").append(i).append(" ");
      for (int j = 0; j < candidate.evidence().size(); ++j) {
        builder.append(candidate.evidence().get(j).description()).append(" | ");
      }
      builder.append('\n');
    }
    return builder.toString();
  }

  private static void assertGraphAdditionCandidate(
      final ReactionPredictionResult result,
      final String ruleId,
      final String productIdPart,
      final String firstAddedElement,
      final String secondAddedElement,
      final String firstReducedAtomId,
      final String secondReducedAtomId) {
    OrganicReactionPredictionServiceTest.assertGraphAdditionCandidate(
        result,
        ruleId,
        productIdPart,
        firstAddedElement,
        secondAddedElement,
        firstReducedAtomId,
        secondReducedAtomId,
        BondType.SINGLE,
        9,
        6);
  }

  private static void assertGraphAdditionCandidate(
      final ReactionPredictionResult result,
      final String ruleId,
      final String productIdPart,
      final String firstAddedElement,
      final String secondAddedElement,
      final String firstReducedAtomId,
      final String secondReducedAtomId,
      final BondType expectedProductBondType,
      final int expectedAtomCount,
      final int expectedBondCount) {
    Assertions.assertTrue(
        result.candidates().stream()
            .anyMatch(
                candidate -> {
                  if (!OrganicReactionPredictionServiceTest.hasEvidenceContaining(candidate, ruleId)
                      || !OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                          candidate, "graph bond-addition")) {
                    return false;
                  }
                  final Molecule product = candidate.products().participants().get(0).molecule();
                  return product.id().value().contains(productIdPart)
                      && product.atomCount() == expectedAtomCount
                      && product.bondCount() == expectedBondCount
                      && OrganicReactionPredictionServiceTest.hasBond(
                          product, firstReducedAtomId, secondReducedAtomId, expectedProductBondType)
                      && OrganicReactionPredictionServiceTest.atomCount(product, firstAddedElement)
                          >= 1
                      && OrganicReactionPredictionServiceTest.atomCount(product, secondAddedElement)
                          >= 1;
                }),
        result.candidates().toString());
  }

  private static double maxProbabilityWithEvidence(
      final ReactionPredictionResult result, final String expectedText) {
    return result.candidates().stream()
        .filter(
            candidate ->
                OrganicReactionPredictionServiceTest.hasEvidenceContaining(candidate, expectedText))
        .mapToDouble(ReactionProductCandidate::probability)
        .max()
        .orElse(0.0);
  }

  private static long candidateCountWithEvidence(
      final ReactionPredictionResult result, final String expectedText) {
    return result.candidates().stream()
        .filter(
            candidate ->
                OrganicReactionPredictionServiceTest.hasEvidenceContaining(candidate, expectedText))
        .count();
  }

  private static void assertBridgeAdditionCandidate(
      final ReactionPredictionResult result,
      final String ruleId,
      final String productIdPart,
      final String bridgeElement,
      final String firstAnchorAtomId,
      final String secondAnchorAtomId,
      final BondType expectedProductBondType,
      final int expectedAtomCount,
      final int expectedBondCount) {
    Assertions.assertTrue(
        result.candidates().stream()
            .anyMatch(
                candidate -> {
                  if (!OrganicReactionPredictionServiceTest.hasEvidenceContaining(candidate, ruleId)
                      || !OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                          candidate, "graph bridge-addition")) {
                    return false;
                  }
                  final Molecule product = candidate.products().participants().get(0).molecule();
                  final AtomId bridgeAtomId =
                      OrganicReactionPredictionServiceTest.firstAddedAtomId(product, bridgeElement);
                  return product.id().value().contains(productIdPart)
                      && product.atomCount() == expectedAtomCount
                      && product.bondCount() == expectedBondCount
                      && OrganicReactionPredictionServiceTest.hasBond(
                          product, firstAnchorAtomId, secondAnchorAtomId, expectedProductBondType)
                      && OrganicReactionPredictionServiceTest.hasBond(
                          product, firstAnchorAtomId, bridgeAtomId.value(), BondType.SINGLE)
                      && OrganicReactionPredictionServiceTest.hasBond(
                          product, secondAnchorAtomId, bridgeAtomId.value(), BondType.SINGLE);
                }),
        result.candidates().toString());
  }

  private static void assertBondInsertionCandidate(
      final ReactionPredictionResult result,
      final String ruleId,
      final String productIdPart,
      final String insertedElement,
      final String firstAnchorAtomId,
      final String secondAnchorAtomId,
      final BondType expectedInsertedBondType,
      final int expectedAtomCount,
      final int expectedBondCount) {
    Assertions.assertTrue(
        result.candidates().stream()
            .anyMatch(
                candidate -> {
                  if (!OrganicReactionPredictionServiceTest.hasEvidenceContaining(candidate, ruleId)
                      || !OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                          candidate, "graph bond-insertion")) {
                    return false;
                  }
                  final Molecule product = candidate.products().participants().get(0).molecule();
                  final AtomId insertedAtomId =
                      OrganicReactionPredictionServiceTest.firstAtomIdByPrefix(
                          product, "insert_", insertedElement);
                  return product.id().value().contains(productIdPart)
                      && product.atomCount() == expectedAtomCount
                      && product.bondCount() == expectedBondCount
                      && !OrganicReactionPredictionServiceTest.hasBond(
                          product, firstAnchorAtomId, secondAnchorAtomId, expectedInsertedBondType)
                      && OrganicReactionPredictionServiceTest.hasBond(
                          product,
                          firstAnchorAtomId,
                          insertedAtomId.value(),
                          expectedInsertedBondType)
                      && OrganicReactionPredictionServiceTest.hasBond(
                          product,
                          insertedAtomId.value(),
                          secondAnchorAtomId,
                          expectedInsertedBondType);
                }),
        result.candidates().toString());
  }

  private static boolean hasBond(
      final Molecule molecule, final String first, final String second, final BondType type) {
    return molecule.bonds().stream()
        .anyMatch(
            bond -> bond.type() == type && bond.connects(AtomId.of(first), AtomId.of(second)));
  }

  private static boolean hasBond(
      final ReactionSide side, final String first, final String second, final BondType type) {
    return side.participants().stream()
        .anyMatch(
            participant ->
                OrganicReactionPredictionServiceTest.hasBond(
                    participant.molecule(), first, second, type));
  }

  private static int totalAtomCount(final ReactionSide side) {
    return side.participants().stream()
        .mapToInt(participant -> participant.molecule().atomCount())
        .sum();
  }

  private static int totalBondCount(final ReactionSide side) {
    return side.participants().stream()
        .mapToInt(participant -> participant.molecule().bondCount())
        .sum();
  }

  private static AtomId firstAddedAtomId(final Molecule molecule, final String elementSymbol) {
    return OrganicReactionPredictionServiceTest.firstAtomIdByPrefix(
        molecule, "bridge_", elementSymbol);
  }

  private static AtomId firstAtomIdByPrefix(
      final Molecule molecule, final String atomIdPrefix, final String elementSymbol) {
    for (int i = 0; i < molecule.atoms().size(); ++i) {
      final Atom atom = molecule.atoms().get(i);
      if (atom.id().value().startsWith(atomIdPrefix)
          && atom.element().symbol().value().equals(elementSymbol)) {
        return atom.id();
      }
    }
    throw new AssertionError(
        "Missing atom with prefix "
            + atomIdPrefix
            + " and element "
            + elementSymbol
            + " in "
            + molecule.atoms());
  }

  private static void assertGraphBetaEliminationCandidate(
      final ReactionPredictionResult result, final Molecule reactant) {
    Assertions.assertTrue(
        result.candidates().stream()
            .anyMatch(
                candidate -> {
                  if (!OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                          candidate, "graph_beta_elimination_unsaturation")
                      || !OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                          candidate, "graph beta-elimination")) {
                    return false;
                  }
                  final Molecule product = candidate.products().participants().get(0).molecule();
                  return product.id().value().contains("beta_elimination_graph_alkene")
                      && candidate.products().participants().stream()
                          .map(ReactionParticipant::molecule)
                          .anyMatch(
                              coproduct ->
                                  coproduct.id().value().contains("_beta_leaving_coproduct_")
                                      && OrganicReactionPredictionServiceTest.atomCount(
                                              coproduct, "Cl")
                                          == 1)
                      && OrganicReactionPredictionServiceTest.totalAtomCount(candidate.products())
                          == reactant.atomCount()
                      && OrganicReactionPredictionServiceTest.totalBondCount(candidate.products())
                          == reactant.bondCount() - 1
                      && product.atomCount() == reactant.atomCount() - 1
                      && product.bondCount() == reactant.bondCount() - 1
                      && OrganicReactionPredictionServiceTest.atomCount(product, "Cl") == 0
                      && OrganicReactionPredictionServiceTest.hasBond(
                          product, "a0", "a1", BondType.DOUBLE);
                }),
        result.candidates().toString());
  }

  private static void assertReplacementBranch(
      final ReactionPredictionResult result,
      final String ruleId,
      final String leavingElementSymbol,
      final String replacementElementSymbol,
      final Molecule reactant) {
    final ReactionProductCandidate candidate =
        result.candidates().stream()
            .filter(
                candidateItem ->
                    OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                        candidateItem, ruleId))
            .findFirst()
            .orElseThrow();
    final Molecule product = candidate.products().participants().get(0).molecule();
    Assertions.assertEquals(reactant.atomCount(), product.atomCount());
    Assertions.assertEquals(reactant.bondCount(), product.bondCount());
    Assertions.assertEquals(
        OrganicReactionPredictionServiceTest.atomCount(reactant, leavingElementSymbol) - 1,
        OrganicReactionPredictionServiceTest.atomCount(product, leavingElementSymbol));
    Assertions.assertEquals(
        OrganicReactionPredictionServiceTest.atomCount(reactant, replacementElementSymbol) + 1,
        OrganicReactionPredictionServiceTest.atomCount(product, replacementElementSymbol));
    Assertions.assertTrue(candidate.probability() > 0.0);
    Assertions.assertTrue(candidate.expectedYieldPercent() > 0.0);
  }

  private static void assertMechanismPattern(
      final ReactionMechanismPatternCatalog catalog,
      final ReactionMechanismFamily family,
      final MechanisticEventKind expectedEvent) {
    Assertions.assertTrue(catalog.require(family).eventSequence().contains(expectedEvent));
    Assertions.assertTrue(catalog.require(family).detailedSteps().size() >= 4, family.name());
    Assertions.assertFalse(catalog.require(family).applicabilitySignals().isEmpty(), family.name());
    Assertions.assertFalse(catalog.require(family).riskSignals().isEmpty(), family.name());
    Assertions.assertEquals(
        1.0, catalog.require(family).pathwayProbabilityContribution(), 1.0E-12, family.name());
  }

  private static ReactionMechanismPattern firstProductionDetailedPattern(
      final ReactionMechanismPatternCatalog catalog) {
    final List<ReactionMechanismPattern> patterns = catalog.patterns();
    for (int i = 0; i < patterns.size(); ++i) {
      if (patterns.get(i).productionDetailedPattern()) {
        return patterns.get(i);
      }
    }
    throw new AssertionError(
        "Built-in mechanism catalog must contain a production-detailed pattern.");
  }

  private static ReactionProductCandidate candidateByEvidence(
      final ReactionPredictionResult result, final String expectedText) {
    return result.candidates().stream()
        .filter(
            candidate ->
                OrganicReactionPredictionServiceTest.hasEvidenceContaining(candidate, expectedText))
        .findFirst()
        .orElseThrow();
  }

  private static ReactionProductCandidate candidateByEvidence(
      final List<ReactionProductCandidate> candidates, final String expectedText) {
    return candidates.stream()
        .filter(
            candidate ->
                OrganicReactionPredictionServiceTest.hasEvidenceContaining(candidate, expectedText))
        .findFirst()
        .orElseThrow();
  }

  private static int atomCount(final Molecule molecule, final String elementSymbol) {
    int count = 0;
    for (int i = 0; i < molecule.atoms().size(); ++i) {
      if (molecule.atoms().get(i).element().symbol().value().equals(elementSymbol)) {
        ++count;
      }
    }
    return count;
  }

  private static double highestCandidateProbabilityWithoutEvidence(
      final ReactionPredictionResult result, final String evidenceText) {
    double highestProbability = 0.0;
    for (int i = 0; i < result.candidates().size(); ++i) {
      final ReactionProductCandidate candidate = result.candidates().get(i);
      if (OrganicReactionPredictionServiceTest.hasEvidenceContaining(candidate, evidenceText)) {
        continue;
      }
      highestProbability = Math.max(highestProbability, candidate.probability());
    }
    return highestProbability;
  }

  private static int tripleBondCount(
      final Molecule molecule, final String firstElementSymbol, final String secondElementSymbol) {
    int count = 0;
    for (int i = 0; i < molecule.bonds().size(); ++i) {
      final Bond bond = molecule.bonds().get(i);
      if (bond.type() == BondType.TRIPLE
          && OrganicReactionPredictionServiceTest.hasBondElements(
              molecule, bond, firstElementSymbol, secondElementSymbol)) {
        ++count;
      }
    }
    return count;
  }

  private static boolean hasBondToElement(
      final Molecule molecule,
      final String firstElementSymbol,
      final String secondElementSymbol,
      final BondType bondType) {
    for (int i = 0; i < molecule.bonds().size(); ++i) {
      final Bond bond = molecule.bonds().get(i);
      if (bond.type() == bondType
          && OrganicReactionPredictionServiceTest.hasBondElements(
              molecule, bond, firstElementSymbol, secondElementSymbol)) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasBondElements(
      final Molecule molecule,
      final Bond bond,
      final String firstElementSymbol,
      final String secondElementSymbol) {
    final String first =
        OrganicReactionPredictionServiceTest.atom(molecule, bond.firstAtomId())
            .element()
            .symbol()
            .value();
    final String second =
        OrganicReactionPredictionServiceTest.atom(molecule, bond.secondAtomId())
            .element()
            .symbol()
            .value();
    return first.equals(firstElementSymbol) && second.equals(secondElementSymbol)
        || first.equals(secondElementSymbol) && second.equals(firstElementSymbol);
  }

  private static Atom atom(final Molecule molecule, final AtomId atomId) {
    for (int i = 0; i < molecule.atoms().size(); ++i) {
      final Atom atom = molecule.atoms().get(i);
      if (atom.id().equals(atomId)) {
        return atom;
      }
    }
    throw new IllegalArgumentException("Test molecule does not contain requested atom.");
  }

  private static boolean hasBuiltInReplacementEvidence(final ReactionProductCandidate candidate) {
    return OrganicReactionPredictionServiceTest.hasEvidenceContaining(
        candidate, "Generated SNAr site combination");
  }

  private static boolean hasEvidenceContaining(
      final ReactionProductCandidate candidate, final String expectedText) {
    return candidate.evidence().stream()
        .anyMatch(evidence -> evidence.description().contains(expectedText));
  }

  private static void assertNoCandidateEvidenceContaining(
      final ReactionPredictionResult result, final String forbiddenText) {
    Assertions.assertTrue(
        result.candidates().stream()
            .noneMatch(
                candidate ->
                    OrganicReactionPredictionServiceTest.hasEvidenceContaining(
                        candidate, forbiddenText)),
        result.candidates().toString());
  }

  private static void assertNoCandidateEvidenceKind(
      final ReactionPredictionResult result, final ReactionPredictionEvidenceKind forbiddenKind) {
    Assertions.assertTrue(
        result.candidates().stream()
            .flatMap(candidate -> candidate.evidence().stream())
            .noneMatch(evidence -> evidence.kind() == forbiddenKind),
        result.candidates().toString());
  }

  private static OrganicReactionRule mechanismOnlyRule() {
    return new OrganicReactionRule() {
      public OrganicReactionRuleId id() {
        return OrganicReactionRuleId.of("template_only_snar_probe");
      }

      public String displayName() {
        return "Template-only SNAr probe";
      }

      public OrganicReactionRuleCategory category() {
        return OrganicReactionRuleCategory.SUBSTITUTION;
      }

      public OrganicReactionRuleMatch match(final ReactionPredictionRequest request) {
        return OrganicReactionRuleMatch.matched(
            0.73, "Electron-poor heteroaryl scaffold accepts a nucleophilic substitution channel.");
      }

      public List<ReactionProductCandidate> generate(
          final ReactionPredictionRequest request, final OrganicReactionRuleMatch match) {
        return List.of();
      }
    };
  }

  private static String candidateSummary(final ReactionPredictionResult result) {
    final StringBuilder builder = new StringBuilder();
    for (final ReactionProductCandidate candidate : result.candidates()) {
      final Molecule product = candidate.products().participants().get(0).molecule();
      builder
          .append(System.lineSeparator())
          .append("candidate")
          .append(" probability=")
          .append(candidate.probability())
          .append(" product=")
          .append(product.id().value())
          .append(" atoms=")
          .append(product.atomCount())
          .append(" bonds=")
          .append(product.bondCount())
          .append(" C=")
          .append(OrganicReactionPredictionServiceTest.atomCount(product, "C"))
          .append(" Br=")
          .append(OrganicReactionPredictionServiceTest.atomCount(product, "Br"))
          .append(" evidence=")
          .append(
              candidate.evidence().stream().map(ReactionPredictionEvidence::description).toList());
    }
    return builder.toString();
  }

  private static ReactionPredictionEvidence evidenceContaining(
      final List<ReactionPredictionEvidence> evidence, final String expectedText) {
    return evidence.stream()
        .filter(item -> item.description().contains(expectedText))
        .findFirst()
        .orElseThrow();
  }

  private static boolean hasBestCandidateEvidenceContaining(
      final ReactionPredictionResult result, final String expectedText) {
    return OrganicReactionPredictionServiceTest.hasEvidenceContaining(
        result.bestCandidate(), expectedText);
  }

  private static boolean hasDiagnosticContaining(
      final ReactionPredictionResult result, final String expectedText) {
    return result.diagnostics().stream()
        .anyMatch(evidence -> evidence.description().contains(expectedText));
  }

  private static Molecule ringMolecule(final String id, final String firstAtomSymbol) {
    return Molecule.of(
        MoleculeId.of(id),
        id,
        List.of(
            OrganicReactionPredictionServiceTest.atom("a0", firstAtomSymbol),
            OrganicReactionPredictionServiceTest.atom("a1", "C"),
            OrganicReactionPredictionServiceTest.atom("a2", "C"),
            OrganicReactionPredictionServiceTest.atom(
                "a3", "N".equals(firstAtomSymbol) ? "N" : "C"),
            OrganicReactionPredictionServiceTest.atom("a4", "C"),
            OrganicReactionPredictionServiceTest.atom("a5", "C")),
        List.of(
            OrganicReactionPredictionServiceTest.bond("a0", "a1"),
            OrganicReactionPredictionServiceTest.bond("a1", "a2"),
            OrganicReactionPredictionServiceTest.bond("a2", "a3"),
            OrganicReactionPredictionServiceTest.bond("a3", "a4"),
            OrganicReactionPredictionServiceTest.bond("a4", "a5"),
            OrganicReactionPredictionServiceTest.bond("a5", "a0")),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule functionalizedHeterocycle() {
    return Molecule.of(
        MoleculeId.of("functionalized_heterocycle"),
        "Functionalized heterocycle",
        List.of(
            OrganicReactionPredictionServiceTest.atom(
                "a0", "N", FormalCharge.of(1), RadicalState.CLOSED_SHELL),
            OrganicReactionPredictionServiceTest.atom("a1", "C"),
            OrganicReactionPredictionServiceTest.atom("a2", "C"),
            OrganicReactionPredictionServiceTest.atom(
                "a3", "C", FormalCharge.NEUTRAL, RadicalState.of(1)),
            OrganicReactionPredictionServiceTest.atom("a4", "C"),
            OrganicReactionPredictionServiceTest.atom("a5", "C"),
            OrganicReactionPredictionServiceTest.atom("a6", "O"),
            OrganicReactionPredictionServiceTest.atom("a7", "Cl")),
        List.of(
            OrganicReactionPredictionServiceTest.bond("a0", "a1"),
            OrganicReactionPredictionServiceTest.bond("a1", "a2"),
            OrganicReactionPredictionServiceTest.bond("a2", "a3"),
            OrganicReactionPredictionServiceTest.bond("a3", "a4"),
            OrganicReactionPredictionServiceTest.bond("a4", "a5"),
            OrganicReactionPredictionServiceTest.bond("a5", "a0"),
            Bond.of(AtomId.of("a3"), AtomId.of("a6"), BondType.DOUBLE),
            Bond.of(AtomId.of("a4"), AtomId.of("a7"), BondType.SINGLE)),
        MolecularCharge.of(1),
        SpinMultiplicity.of(2));
  }

  private static Molecule dichloroHeterocycle() {
    return Molecule.of(
        MoleculeId.of("dichloro_heterocycle"),
        "Dichloro heterocycle",
        List.of(
            OrganicReactionPredictionServiceTest.atom("a0", "N"),
            OrganicReactionPredictionServiceTest.atom("a1", "C"),
            OrganicReactionPredictionServiceTest.atom("a2", "C"),
            OrganicReactionPredictionServiceTest.atom("a3", "C"),
            OrganicReactionPredictionServiceTest.atom("a4", "C"),
            OrganicReactionPredictionServiceTest.atom("a5", "C"),
            OrganicReactionPredictionServiceTest.atom("a6", "Cl"),
            OrganicReactionPredictionServiceTest.atom("a7", "Cl")),
        List.of(
            OrganicReactionPredictionServiceTest.bond("a0", "a1"),
            OrganicReactionPredictionServiceTest.bond("a1", "a2"),
            OrganicReactionPredictionServiceTest.bond("a2", "a3"),
            OrganicReactionPredictionServiceTest.bond("a3", "a4"),
            OrganicReactionPredictionServiceTest.bond("a4", "a5"),
            OrganicReactionPredictionServiceTest.bond("a5", "a0"),
            Bond.of(AtomId.of("a2"), AtomId.of("a6"), BondType.SINGLE),
            Bond.of(AtomId.of("a4"), AtomId.of("a7"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule chloroDimethylNicotinonitrile() {
    return Molecule.of(
        MoleculeId.of("chloro_dimethyl_nicotinonitrile"),
        "2-Chloro-4,6-dimethyl-3-pyridinecarbonitrile",
        List.of(
            OrganicReactionPredictionServiceTest.atom("a0", "N"),
            OrganicReactionPredictionServiceTest.atom("a1", "C"),
            OrganicReactionPredictionServiceTest.atom("a2", "C"),
            OrganicReactionPredictionServiceTest.atom("a3", "C"),
            OrganicReactionPredictionServiceTest.atom("a4", "C"),
            OrganicReactionPredictionServiceTest.atom("a5", "C"),
            OrganicReactionPredictionServiceTest.atom("cl", "Cl"),
            OrganicReactionPredictionServiceTest.atom("cn", "C"),
            OrganicReactionPredictionServiceTest.atom("nn", "N"),
            OrganicReactionPredictionServiceTest.atom("m4", "C"),
            OrganicReactionPredictionServiceTest.atom("m6", "C")),
        List.of(
            OrganicReactionPredictionServiceTest.bond("a0", "a1"),
            OrganicReactionPredictionServiceTest.bond("a1", "a2"),
            OrganicReactionPredictionServiceTest.bond("a2", "a3"),
            OrganicReactionPredictionServiceTest.bond("a3", "a4"),
            OrganicReactionPredictionServiceTest.bond("a4", "a5"),
            OrganicReactionPredictionServiceTest.bond("a5", "a0"),
            Bond.of(AtomId.of("a1"), AtomId.of("cl"), BondType.SINGLE),
            Bond.of(AtomId.of("a2"), AtomId.of("cn"), BondType.SINGLE),
            Bond.of(AtomId.of("cn"), AtomId.of("nn"), BondType.TRIPLE),
            Bond.of(AtomId.of("a3"), AtomId.of("m4"), BondType.SINGLE),
            Bond.of(AtomId.of("a5"), AtomId.of("m6"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule primaryAmine(final String id) {
    return Molecule.of(
        MoleculeId.of(id),
        id,
        List.of(
            OrganicReactionPredictionServiceTest.atom("n0", "N"),
            OrganicReactionPredictionServiceTest.atom("h1", "H"),
            OrganicReactionPredictionServiceTest.atom("c2", "C"),
            OrganicReactionPredictionServiceTest.atom("c3", "C")),
        List.of(
            Bond.of(AtomId.of("n0"), AtomId.of("h1"), BondType.SINGLE),
            Bond.of(AtomId.of("n0"), AtomId.of("c2"), BondType.SINGLE),
            Bond.of(AtomId.of("c2"), AtomId.of("c3"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule benzylamine(final String id) {
    return Molecule.of(
        MoleculeId.of(id),
        id,
        List.of(
            OrganicReactionPredictionServiceTest.atom("n0", "N"),
            OrganicReactionPredictionServiceTest.atom("h1", "H"),
            OrganicReactionPredictionServiceTest.atom("b0", "C"),
            OrganicReactionPredictionServiceTest.atom("p0", "C"),
            OrganicReactionPredictionServiceTest.atom("p1", "C"),
            OrganicReactionPredictionServiceTest.atom("p2", "C"),
            OrganicReactionPredictionServiceTest.atom("p3", "C"),
            OrganicReactionPredictionServiceTest.atom("p4", "C"),
            OrganicReactionPredictionServiceTest.atom("p5", "C")),
        List.of(
            Bond.of(AtomId.of("n0"), AtomId.of("h1"), BondType.SINGLE),
            Bond.of(AtomId.of("n0"), AtomId.of("b0"), BondType.SINGLE),
            Bond.of(AtomId.of("b0"), AtomId.of("p0"), BondType.SINGLE),
            Bond.of(AtomId.of("p0"), AtomId.of("p1"), BondType.AROMATIC),
            Bond.of(AtomId.of("p1"), AtomId.of("p2"), BondType.AROMATIC),
            Bond.of(AtomId.of("p2"), AtomId.of("p3"), BondType.AROMATIC),
            Bond.of(AtomId.of("p3"), AtomId.of("p4"), BondType.AROMATIC),
            Bond.of(AtomId.of("p4"), AtomId.of("p5"), BondType.AROMATIC),
            Bond.of(AtomId.of("p5"), AtomId.of("p0"), BondType.AROMATIC)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule selectivityProbeHeterocycle() {
    return Molecule.of(
        MoleculeId.of("selectivity_probe_heterocycle"),
        "Selectivity probe heterocycle",
        List.of(
            OrganicReactionPredictionServiceTest.atom("a0", "N"),
            OrganicReactionPredictionServiceTest.atom("a1", "C"),
            OrganicReactionPredictionServiceTest.atom("a2", "C"),
            OrganicReactionPredictionServiceTest.atom("a3", "C"),
            OrganicReactionPredictionServiceTest.atom("a4", "C"),
            OrganicReactionPredictionServiceTest.atom("a5", "C"),
            OrganicReactionPredictionServiceTest.atom("cl2", "Cl"),
            OrganicReactionPredictionServiceTest.atom("cl4", "Cl"),
            OrganicReactionPredictionServiceTest.atom("cn", "C"),
            OrganicReactionPredictionServiceTest.atom("nitrile_n", "N")),
        List.of(
            OrganicReactionPredictionServiceTest.bond("a0", "a1"),
            OrganicReactionPredictionServiceTest.bond("a1", "a2"),
            OrganicReactionPredictionServiceTest.bond("a2", "a3"),
            OrganicReactionPredictionServiceTest.bond("a3", "a4"),
            OrganicReactionPredictionServiceTest.bond("a4", "a5"),
            OrganicReactionPredictionServiceTest.bond("a5", "a0"),
            Bond.of(AtomId.of("a2"), AtomId.of("cl2"), BondType.SINGLE),
            Bond.of(AtomId.of("a4"), AtomId.of("cl4"), BondType.SINGLE),
            Bond.of(AtomId.of("a2"), AtomId.of("cn"), BondType.SINGLE),
            Bond.of(AtomId.of("cn"), AtomId.of("nitrile_n"), BondType.TRIPLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule halogenatedNitrogenHeterocycle(
      final String id, final String halogenSymbol) {
    return Molecule.of(
        MoleculeId.of(id),
        id,
        List.of(
            OrganicReactionPredictionServiceTest.atom("a0", "N"),
            OrganicReactionPredictionServiceTest.atom("a1", "C"),
            OrganicReactionPredictionServiceTest.atom("a2", "C"),
            OrganicReactionPredictionServiceTest.atom("a3", "C"),
            OrganicReactionPredictionServiceTest.atom("a4", "C"),
            OrganicReactionPredictionServiceTest.atom("a5", "C"),
            OrganicReactionPredictionServiceTest.atom("a6", halogenSymbol)),
        List.of(
            OrganicReactionPredictionServiceTest.bond("a0", "a1"),
            OrganicReactionPredictionServiceTest.bond("a1", "a2"),
            OrganicReactionPredictionServiceTest.bond("a2", "a3"),
            OrganicReactionPredictionServiceTest.bond("a3", "a4"),
            OrganicReactionPredictionServiceTest.bond("a4", "a5"),
            OrganicReactionPredictionServiceTest.bond("a5", "a0"),
            Bond.of(AtomId.of("a2"), AtomId.of("a6"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule alkylHalideFragment(final String id, final String halogenSymbol) {
    return Molecule.of(
        MoleculeId.of(id),
        id,
        List.of(
            OrganicReactionPredictionServiceTest.atom("f0", "C"),
            OrganicReactionPredictionServiceTest.atom("f1", "C"),
            OrganicReactionPredictionServiceTest.atom("fx", halogenSymbol)),
        List.of(
            Bond.of(AtomId.of("f0"), AtomId.of("f1"), BondType.SINGLE),
            Bond.of(AtomId.of("f0"), AtomId.of("fx"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule betaEliminationAlkylChloride() {
    return Molecule.of(
        MoleculeId.of("beta_elimination_alkyl_chloride"),
        "Beta elimination alkyl chloride",
        List.of(
            OrganicReactionPredictionServiceTest.atom("a0", "C"),
            OrganicReactionPredictionServiceTest.atom("a1", "C"),
            OrganicReactionPredictionServiceTest.atom("a2", "Cl")),
        List.of(
            Bond.of(AtomId.of("a0"), AtomId.of("a1"), BondType.SINGLE),
            Bond.of(AtomId.of("a0"), AtomId.of("a2"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule chlorinatedAminoNitrogenHeterocycle(final String id) {
    return Molecule.of(
        MoleculeId.of(id),
        id,
        List.of(
            OrganicReactionPredictionServiceTest.atom("a0", "N"),
            OrganicReactionPredictionServiceTest.atom("a1", "C"),
            OrganicReactionPredictionServiceTest.atom("a2", "C"),
            OrganicReactionPredictionServiceTest.atom("a3", "C"),
            OrganicReactionPredictionServiceTest.atom("a4", "C"),
            OrganicReactionPredictionServiceTest.atom("a5", "C"),
            OrganicReactionPredictionServiceTest.atom("cl", "Cl"),
            OrganicReactionPredictionServiceTest.atom("nh", "N"),
            OrganicReactionPredictionServiceTest.atom("hh", "H")),
        List.of(
            OrganicReactionPredictionServiceTest.bond("a0", "a1"),
            OrganicReactionPredictionServiceTest.bond("a1", "a2"),
            OrganicReactionPredictionServiceTest.bond("a2", "a3"),
            OrganicReactionPredictionServiceTest.bond("a3", "a4"),
            OrganicReactionPredictionServiceTest.bond("a4", "a5"),
            OrganicReactionPredictionServiceTest.bond("a5", "a0"),
            Bond.of(AtomId.of("a2"), AtomId.of("cl"), BondType.SINGLE),
            Bond.of(AtomId.of("a4"), AtomId.of("nh"), BondType.SINGLE),
            Bond.of(AtomId.of("nh"), AtomId.of("hh"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule asymmetricDichloroHeterocycle() {
    return Molecule.of(
        MoleculeId.of("asymmetric_dichloro_heterocycle"),
        "Asymmetric dichloro heterocycle",
        List.of(
            OrganicReactionPredictionServiceTest.atom("a0", "N"),
            OrganicReactionPredictionServiceTest.atom("a1", "C"),
            OrganicReactionPredictionServiceTest.atom("a2", "C"),
            OrganicReactionPredictionServiceTest.atom("a3", "C"),
            OrganicReactionPredictionServiceTest.atom("a4", "C"),
            OrganicReactionPredictionServiceTest.atom("a5", "C"),
            OrganicReactionPredictionServiceTest.atom("a6", "Cl"),
            OrganicReactionPredictionServiceTest.atom("a7", "Cl")),
        List.of(
            OrganicReactionPredictionServiceTest.bond("a0", "a1"),
            OrganicReactionPredictionServiceTest.bond("a1", "a2"),
            OrganicReactionPredictionServiceTest.bond("a2", "a3"),
            OrganicReactionPredictionServiceTest.bond("a3", "a4"),
            OrganicReactionPredictionServiceTest.bond("a4", "a5"),
            OrganicReactionPredictionServiceTest.bond("a5", "a0"),
            Bond.of(AtomId.of("a1"), AtomId.of("a6"), BondType.SINGLE),
            Bond.of(AtomId.of("a4"), AtomId.of("a7"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule nitrileNitrogenHeterocycle() {
    return Molecule.of(
        MoleculeId.of("nitrile_nitrogen_heterocycle"),
        "Nitrile nitrogen heterocycle",
        List.of(
            OrganicReactionPredictionServiceTest.atom("a0", "N"),
            OrganicReactionPredictionServiceTest.atom("a1", "C"),
            OrganicReactionPredictionServiceTest.atom("a2", "C"),
            OrganicReactionPredictionServiceTest.atom("a3", "C"),
            OrganicReactionPredictionServiceTest.atom("a4", "C"),
            OrganicReactionPredictionServiceTest.atom("a5", "C"),
            OrganicReactionPredictionServiceTest.atom("a6", "C"),
            OrganicReactionPredictionServiceTest.atom("a7", "N")),
        List.of(
            OrganicReactionPredictionServiceTest.bond("a0", "a1"),
            OrganicReactionPredictionServiceTest.bond("a1", "a2"),
            OrganicReactionPredictionServiceTest.bond("a2", "a3"),
            OrganicReactionPredictionServiceTest.bond("a3", "a4"),
            OrganicReactionPredictionServiceTest.bond("a4", "a5"),
            OrganicReactionPredictionServiceTest.bond("a5", "a0"),
            Bond.of(AtomId.of("a2"), AtomId.of("a6"), BondType.SINGLE),
            Bond.of(AtomId.of("a6"), AtomId.of("a7"), BondType.TRIPLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule twoCarbonMolecule(final String id, final BondType bondType) {
    return Molecule.of(
        MoleculeId.of(id),
        id,
        List.of(
            OrganicReactionPredictionServiceTest.atom("a0", "C"),
            OrganicReactionPredictionServiceTest.atom("a1", "C")),
        List.of(Bond.of(AtomId.of("a0"), AtomId.of("a1"), bondType)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule singleAtomMolecule(final String id, final String symbol) {
    return Molecule.of(
        MoleculeId.of(id),
        id,
        List.of(OrganicReactionPredictionServiceTest.atom("a0", symbol)),
        List.of(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule twoAtomMolecule(
      final String id,
      final String firstSymbol,
      final String secondSymbol,
      final BondType bondType) {
    return Molecule.of(
        MoleculeId.of(id),
        id,
        List.of(
            OrganicReactionPredictionServiceTest.atom("a0", firstSymbol),
            OrganicReactionPredictionServiceTest.atom("a1", secondSymbol)),
        List.of(Bond.of(AtomId.of("a0"), AtomId.of("a1"), bondType)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule heterocycleCarbonylAlkeneMolecule() {
    return Molecule.of(
        MoleculeId.of("heterocycle_carbonyl_alkene"),
        "Heterocycle carbonyl alkene",
        List.of(
            OrganicReactionPredictionServiceTest.atom("a0", "N"),
            OrganicReactionPredictionServiceTest.atom("a1", "C"),
            OrganicReactionPredictionServiceTest.atom("a2", "C"),
            OrganicReactionPredictionServiceTest.atom("a3", "C"),
            OrganicReactionPredictionServiceTest.atom("a4", "C"),
            OrganicReactionPredictionServiceTest.atom("a5", "C"),
            OrganicReactionPredictionServiceTest.atom("a6", "C"),
            OrganicReactionPredictionServiceTest.atom("a7", "O"),
            OrganicReactionPredictionServiceTest.atom("a8", "C"),
            OrganicReactionPredictionServiceTest.atom("a9", "C")),
        List.of(
            OrganicReactionPredictionServiceTest.bond("a0", "a1"),
            OrganicReactionPredictionServiceTest.bond("a1", "a2"),
            OrganicReactionPredictionServiceTest.bond("a2", "a3"),
            OrganicReactionPredictionServiceTest.bond("a3", "a4"),
            OrganicReactionPredictionServiceTest.bond("a4", "a5"),
            OrganicReactionPredictionServiceTest.bond("a5", "a0"),
            Bond.of(AtomId.of("a2"), AtomId.of("a6"), BondType.SINGLE),
            Bond.of(AtomId.of("a6"), AtomId.of("a7"), BondType.DOUBLE),
            Bond.of(AtomId.of("a6"), AtomId.of("a8"), BondType.SINGLE),
            Bond.of(AtomId.of("a8"), AtomId.of("a9"), BondType.DOUBLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule functionalGroupRichMolecule() {
    return Molecule.of(
        MoleculeId.of("functional_group_rich"),
        "Functional group rich molecule",
        List.of(
            OrganicReactionPredictionServiceTest.atom("a0", "C"),
            OrganicReactionPredictionServiceTest.atom("a1", "O"),
            OrganicReactionPredictionServiceTest.atom("a2", "O"),
            OrganicReactionPredictionServiceTest.atom("a3", "C"),
            OrganicReactionPredictionServiceTest.atom("a4", "C"),
            OrganicReactionPredictionServiceTest.atom("a5", "O"),
            OrganicReactionPredictionServiceTest.atom("a6", "N"),
            OrganicReactionPredictionServiceTest.atom("a7", "C"),
            OrganicReactionPredictionServiceTest.atom("a8", "N"),
            OrganicReactionPredictionServiceTest.atom("a9", "N"),
            OrganicReactionPredictionServiceTest.atom("a10", "O"),
            OrganicReactionPredictionServiceTest.atom("a11", "O"),
            OrganicReactionPredictionServiceTest.atom("a12", "C"),
            OrganicReactionPredictionServiceTest.atom("a13", "C"),
            OrganicReactionPredictionServiceTest.atom("a14", "C"),
            OrganicReactionPredictionServiceTest.atom("a15", "C"),
            OrganicReactionPredictionServiceTest.atom("a16", "C"),
            OrganicReactionPredictionServiceTest.atom("a17", "S"),
            OrganicReactionPredictionServiceTest.atom("a18", "C"),
            OrganicReactionPredictionServiceTest.atom("a19", "S"),
            OrganicReactionPredictionServiceTest.atom("a20", "O"),
            OrganicReactionPredictionServiceTest.atom("a21", "C")),
        List.of(
            Bond.of(AtomId.of("a0"), AtomId.of("a1"), BondType.DOUBLE),
            Bond.of(AtomId.of("a0"), AtomId.of("a2"), BondType.SINGLE),
            Bond.of(AtomId.of("a2"), AtomId.of("a3"), BondType.SINGLE),
            Bond.of(AtomId.of("a4"), AtomId.of("a5"), BondType.DOUBLE),
            Bond.of(AtomId.of("a4"), AtomId.of("a6"), BondType.SINGLE),
            Bond.of(AtomId.of("a7"), AtomId.of("a8"), BondType.TRIPLE),
            Bond.of(AtomId.of("a9"), AtomId.of("a10"), BondType.SINGLE),
            Bond.of(AtomId.of("a9"), AtomId.of("a11"), BondType.DOUBLE),
            Bond.of(AtomId.of("a12"), AtomId.of("a13"), BondType.DOUBLE),
            Bond.of(AtomId.of("a14"), AtomId.of("a15"), BondType.TRIPLE),
            Bond.of(AtomId.of("a16"), AtomId.of("a17"), BondType.SINGLE),
            Bond.of(AtomId.of("a17"), AtomId.of("a18"), BondType.SINGLE),
            Bond.of(AtomId.of("a19"), AtomId.of("a20"), BondType.DOUBLE),
            Bond.of(AtomId.of("a19"), AtomId.of("a21"), BondType.SINGLE),
            Bond.of(AtomId.of("a6"), AtomId.of("a21"), BondType.DOUBLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule carbonylSubtypeAndOxygenAcidProbe() {
    return Molecule.of(
        MoleculeId.of("carbonyl_subtype_oxygen_acid_probe"),
        "Carbonyl subtype and oxygen acid probe",
        List.of(
            OrganicReactionPredictionServiceTest.atom("a0", "C"),
            OrganicReactionPredictionServiceTest.atom("a1", "O"),
            OrganicReactionPredictionServiceTest.atom("a2", "H"),
            OrganicReactionPredictionServiceTest.atom("a3", "C"),
            OrganicReactionPredictionServiceTest.atom("a4", "C"),
            OrganicReactionPredictionServiceTest.atom("a5", "O"),
            OrganicReactionPredictionServiceTest.atom("a6", "C"),
            OrganicReactionPredictionServiceTest.atom("a7", "C"),
            OrganicReactionPredictionServiceTest.atom("a8", "O"),
            OrganicReactionPredictionServiceTest.atom("a9", "Cl"),
            OrganicReactionPredictionServiceTest.atom("b0", "C"),
            OrganicReactionPredictionServiceTest.atom("b1", "O"),
            OrganicReactionPredictionServiceTest.atom("b2", "O"),
            OrganicReactionPredictionServiceTest.atom("b3", "C"),
            OrganicReactionPredictionServiceTest.atom("b4", "O"),
            OrganicReactionPredictionServiceTest.atom("b5", "C"),
            OrganicReactionPredictionServiceTest.atom("b6", "O"),
            OrganicReactionPredictionServiceTest.atom("b7", "H"),
            OrganicReactionPredictionServiceTest.atom("c0", "C"),
            OrganicReactionPredictionServiceTest.atom("c1", "C"),
            OrganicReactionPredictionServiceTest.atom("c2", "C"),
            OrganicReactionPredictionServiceTest.atom("c3", "C"),
            OrganicReactionPredictionServiceTest.atom("c4", "C"),
            OrganicReactionPredictionServiceTest.atom("c5", "C"),
            OrganicReactionPredictionServiceTest.atom("c6", "O"),
            OrganicReactionPredictionServiceTest.atom("c7", "H")),
        List.of(
            Bond.of(AtomId.of("a0"), AtomId.of("a1"), BondType.DOUBLE),
            Bond.of(AtomId.of("a0"), AtomId.of("a2"), BondType.SINGLE),
            Bond.of(AtomId.of("a0"), AtomId.of("a3"), BondType.SINGLE),
            Bond.of(AtomId.of("a4"), AtomId.of("a5"), BondType.DOUBLE),
            Bond.of(AtomId.of("a4"), AtomId.of("a6"), BondType.SINGLE),
            Bond.of(AtomId.of("a4"), AtomId.of("a7"), BondType.SINGLE),
            Bond.of(AtomId.of("a7"), AtomId.of("a8"), BondType.DOUBLE),
            Bond.of(AtomId.of("a7"), AtomId.of("a9"), BondType.SINGLE),
            Bond.of(AtomId.of("b0"), AtomId.of("b1"), BondType.DOUBLE),
            Bond.of(AtomId.of("b0"), AtomId.of("b2"), BondType.SINGLE),
            Bond.of(AtomId.of("b2"), AtomId.of("b3"), BondType.SINGLE),
            Bond.of(AtomId.of("b3"), AtomId.of("b4"), BondType.DOUBLE),
            Bond.of(AtomId.of("b5"), AtomId.of("b6"), BondType.SINGLE),
            Bond.of(AtomId.of("b6"), AtomId.of("b7"), BondType.SINGLE),
            Bond.of(AtomId.of("c0"), AtomId.of("c1"), BondType.AROMATIC),
            Bond.of(AtomId.of("c1"), AtomId.of("c2"), BondType.AROMATIC),
            Bond.of(AtomId.of("c2"), AtomId.of("c3"), BondType.AROMATIC),
            Bond.of(AtomId.of("c3"), AtomId.of("c4"), BondType.AROMATIC),
            Bond.of(AtomId.of("c4"), AtomId.of("c5"), BondType.AROMATIC),
            Bond.of(AtomId.of("c5"), AtomId.of("c0"), BondType.AROMATIC),
            Bond.of(AtomId.of("c0"), AtomId.of("c6"), BondType.SINGLE),
            Bond.of(AtomId.of("c6"), AtomId.of("c7"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule carboxylAmineAlkeneMolecule() {
    return Molecule.of(
        MoleculeId.of("carboxyl_amine_alkene"),
        "Carboxyl amine alkene molecule",
        List.of(
            OrganicReactionPredictionServiceTest.atom("a0", "C"),
            OrganicReactionPredictionServiceTest.atom("a1", "O"),
            OrganicReactionPredictionServiceTest.atom("a2", "O"),
            OrganicReactionPredictionServiceTest.atom("a3", "N"),
            OrganicReactionPredictionServiceTest.atom("a4", "C"),
            OrganicReactionPredictionServiceTest.atom("a5", "C"),
            OrganicReactionPredictionServiceTest.atom("a6", "C")),
        List.of(
            Bond.of(AtomId.of("a0"), AtomId.of("a1"), BondType.DOUBLE),
            Bond.of(AtomId.of("a0"), AtomId.of("a2"), BondType.SINGLE),
            Bond.of(AtomId.of("a3"), AtomId.of("a4"), BondType.SINGLE),
            Bond.of(AtomId.of("a5"), AtomId.of("a6"), BondType.DOUBLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule terminalAlkeneWithHydrogen(final String id) {
    return Molecule.of(
        MoleculeId.of(id),
        id,
        List.of(
            OrganicReactionPredictionServiceTest.atom("a0", "C"),
            OrganicReactionPredictionServiceTest.atom("a1", "C"),
            OrganicReactionPredictionServiceTest.atom("h2", "H"),
            OrganicReactionPredictionServiceTest.atom("c3", "C")),
        List.of(
            Bond.of(AtomId.of("a0"), AtomId.of("a1"), BondType.DOUBLE),
            Bond.of(AtomId.of("a0"), AtomId.of("h2"), BondType.SINGLE),
            Bond.of(AtomId.of("a1"), AtomId.of("c3"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule terminalAlkyneWithHydrogen(final String id) {
    return Molecule.of(
        MoleculeId.of(id),
        id,
        List.of(
            OrganicReactionPredictionServiceTest.atom("a0", "C"),
            OrganicReactionPredictionServiceTest.atom("a1", "C"),
            OrganicReactionPredictionServiceTest.atom("h2", "H"),
            OrganicReactionPredictionServiceTest.atom("c3", "C")),
        List.of(
            Bond.of(AtomId.of("a0"), AtomId.of("a1"), BondType.TRIPLE),
            Bond.of(AtomId.of("a0"), AtomId.of("h2"), BondType.SINGLE),
            Bond.of(AtomId.of("a1"), AtomId.of("c3"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule organicBoronatePartner(final String id) {
    return Molecule.of(
        MoleculeId.of(id),
        id,
        List.of(
            OrganicReactionPredictionServiceTest.atom("a0", "C"),
            OrganicReactionPredictionServiceTest.atom("b1", "B"),
            OrganicReactionPredictionServiceTest.atom("o2", "O"),
            OrganicReactionPredictionServiceTest.atom("o3", "O"),
            OrganicReactionPredictionServiceTest.atom("c4", "C")),
        List.of(
            Bond.of(AtomId.of("a0"), AtomId.of("b1"), BondType.SINGLE),
            Bond.of(AtomId.of("b1"), AtomId.of("o2"), BondType.SINGLE),
            Bond.of(AtomId.of("b1"), AtomId.of("o3"), BondType.SINGLE),
            Bond.of(AtomId.of("a0"), AtomId.of("c4"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule chargedRadicalAlkene() {
    return Molecule.of(
        MoleculeId.of("charged_radical_alkene"),
        "Charged radical alkene",
        List.of(
            OrganicReactionPredictionServiceTest.atom(
                "a0", "C", FormalCharge.of(1), RadicalState.CLOSED_SHELL),
            OrganicReactionPredictionServiceTest.atom(
                "a1", "C", FormalCharge.NEUTRAL, RadicalState.of(1))),
        List.of(Bond.of(AtomId.of("a0"), AtomId.of("a1"), BondType.DOUBLE)),
        MolecularCharge.of(1),
        SpinMultiplicity.of(2));
  }

  private static Atom atom(final String id, final String symbol) {
    return OrganicReactionPredictionServiceTest.atom(
        id, symbol, FormalCharge.NEUTRAL, RadicalState.CLOSED_SHELL);
  }

  private static Atom atom(
      final String id,
      final String symbol,
      final FormalCharge formalCharge,
      final RadicalState radicalState) {
    final int index = id.charAt(1) - '0';
    return Atom.of(
        AtomId.of(id),
        ElementSymbol.of(symbol),
        Coordinate3D.of(
            Math.cos(index * Math.PI / 3.0),
            Math.sin(index * Math.PI / 3.0),
            0.0,
            LengthUnit.ANGSTROM),
        formalCharge,
        null,
        radicalState,
        ChemistryMetadata.EMPTY);
  }

  private static Bond bond(final String first, final String second) {
    return Bond.of(AtomId.of(first), AtomId.of(second), BondType.AROMATIC);
  }

  private static final class FixedProductRule implements OrganicReactionRule {

    private final Molecule product;

    private FixedProductRule(final Molecule product) {
      this.product = product;
    }

    public OrganicReactionRuleId id() {
      return OrganicReactionRuleId.of("fixed_heterocycle_product");
    }

    public String displayName() {
      return "Fixed heterocycle product";
    }

    public OrganicReactionRuleCategory category() {
      return OrganicReactionRuleCategory.HETEROCYCLE_FUNCTIONALIZATION;
    }

    public OrganicReactionRuleMatch match(final ReactionPredictionRequest request) {
      if (request.hasDomain(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION)) {
        return OrganicReactionRuleMatch.matched(0.9, "heterocycle functionalization domain");
      }
      return OrganicReactionRuleMatch.NO_MATCH;
    }

    public List<ReactionProductCandidate> generate(
        final ReactionPredictionRequest request, final OrganicReactionRuleMatch match) {
      return List.of(
          OrganicReactionPredictionServiceTest.candidate(
              this.product, match.confidence(), "generated by fixed rule"));
    }
  }

  private static final class CalibrationContaminatedRule implements OrganicReactionRule {

    private final Molecule product;

    private CalibrationContaminatedRule(final Molecule product) {
      this.product = product;
    }

    public OrganicReactionRuleId id() {
      return OrganicReactionRuleId.of("calibration_contaminated_rule");
    }

    public String displayName() {
      return "Calibration contaminated rule";
    }

    public OrganicReactionRuleCategory category() {
      return OrganicReactionRuleCategory.HETEROCYCLE_FUNCTIONALIZATION;
    }

    public OrganicReactionRuleMatch match(final ReactionPredictionRequest request) {
      if (request.hasDomain(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION)) {
        return OrganicReactionRuleMatch.matched(0.8, "heterocycle functionalization domain");
      }
      return OrganicReactionRuleMatch.NO_MATCH;
    }

    public List<ReactionProductCandidate> generate(
        final ReactionPredictionRequest request, final OrganicReactionRuleMatch match) {
      return List.of(
          ReactionProductCandidate.of(
              ReactionSide.of(
                  List.of(ReactionParticipant.of(this.product, StoichiometricCoefficient.ONE))),
              match.confidence(),
              60.0,
              "generated structural product",
              List.of(
                  ReactionPredictionEvidence.of(
                      ReactionPredictionEvidenceKind.CALIBRATION_PRIOR,
                      "Benchmark observation from external validation row must not affect prediction.",
                      1.0),
                  ReactionPredictionEvidence.of(
                      ReactionPredictionEvidenceKind.USER_RULE,
                      "Rule generated structural product from request graph and domain.",
                      0.12))));
    }
  }
}

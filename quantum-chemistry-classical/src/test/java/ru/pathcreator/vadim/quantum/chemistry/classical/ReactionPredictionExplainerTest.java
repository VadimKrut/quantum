/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical;

import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionPredictionExplainer;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism.ReactionMechanismPattern;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism.ReactionMechanismPatternCatalog;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism.ReactionMechanismPatternStep;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.BuiltInReactionMechanismPatternCatalog;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.PredictedReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionConfidenceLevel;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionExplanation;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionMethodType;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionScoreBreakdown;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionScoreComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionScoreComponentKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionMechanismClass;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionRegioselectivityClass;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanisticEventKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeature;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.StoichiometricCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;

final class ReactionPredictionExplainerTest {

  @Test
  void explainerClassifiesConditionAwareDominantAndRiskEvidence() {
    final ReactionProductCandidate primary =
        ReactionPredictionExplainerTest.candidate(
            "primary_heterocycle",
            0.70,
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN,
                    "Heterocycle retained.",
                    0.30),
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.CATALYST_EFFECT,
                    "Catalyst is compatible.",
                    0.20)));
    final ReactionProductCandidate sideProduct =
        ReactionPredictionExplainerTest.candidate(
            "side_product",
            0.20,
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.WARNING,
                    "Competing regioisomer is plausible.",
                    -0.10)));
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "explain_heterocycle",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ReactionPredictionExplainerTest.molecule("reactant"),
                        StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                null,
                null,
                null,
                List.of(),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.CATALYST_PRESENT)),
                "condition-aware explanation"),
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(primary, sideProduct),
            "explanation request");
    final ReactionPredictionResult result =
        ReactionPredictionResult.of(
            request,
            ReactionPredictionMethodType.HYBRID,
            List.of(primary, sideProduct),
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.WARNING,
                    "No quantum refinement has been run yet.",
                    -0.05)));

    final ReactionPredictionExplanation explanation =
        new ReactionPredictionExplainer().explain(result);

    Assertions.assertEquals(ReactionPredictionConfidenceLevel.HIGH, explanation.confidenceLevel());
    Assertions.assertEquals(0.70, explanation.topProbability());
    Assertions.assertEquals(0.20, explanation.runnerUpProbability());
    Assertions.assertEquals(0.50, explanation.probabilityGap(), 1.0E-12);
    Assertions.assertTrue(explanation.conditionAware());
    Assertions.assertEquals(
        ReactionMechanismClass.HETEROCYCLE_FUNCTIONALIZATION,
        explanation.pathwayAssessment().mechanismClass());
    Assertions.assertEquals(
        ReactionRegioselectivityClass.HETEROATOM_DIRECTED,
        explanation.pathwayAssessment().regioselectivityClass());
    Assertions.assertTrue(
        explanation.pathwayAssessment().conditionWindow().hasRequiredConditionSignals());
    Assertions.assertEquals(2, explanation.positiveEvidenceCount());
    Assertions.assertEquals(1, explanation.negativeEvidenceCount());
    Assertions.assertFalse(explanation.dominantEvidence().isEmpty());
    Assertions.assertTrue(explanation.hasRisks());
  }

  @Test
  void explainerKeepsEnsembleEnergyEvidenceVisibleWhenDominantListIsCrowded() {
    final ReactionProductCandidate primary =
        ReactionPredictionExplainerTest.candidate(
            "energy_ranked_heterocycle",
            0.64,
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN,
                    "Activated heterocycle pattern matched.",
                    0.90),
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
                    "Nucleophile and leaving group are compatible.",
                    0.80),
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.KINETIC_FACTOR,
                    "Low-barrier channel is available.",
                    0.70),
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.THERMODYNAMIC_FACTOR,
                    "Product channel is thermodynamically accessible.",
                    0.60),
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
                    "Electronic activation favors this site.",
                    0.50),
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.QUANTUM_ENERGY_MODEL,
                    "Candidate ensemble adjustment: relative kinetic/thermodynamic share=0.450000, multiplier=1.120000.",
                    0.12)));
    final ReactionProductCandidate competing =
        ReactionPredictionExplainerTest.candidate("minor_heterocycle", 0.18, List.of());
    final ReactionPredictionResult result =
        ReactionPredictionResult.of(
            ReactionPredictionExplainerTest.request(
                "energy_evidence_visibility",
                ReactionConditions.of(
                    null, null, null, List.of(), List.of(), "energy evidence visibility"),
                List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
                List.of(primary, competing)),
            ReactionPredictionMethodType.HYBRID,
            List.of(primary, competing),
            List.of());

    final ReactionPredictionExplanation explanation =
        new ReactionPredictionExplainer().explain(result);

    Assertions.assertTrue(
        explanation.dominantEvidence().stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.QUANTUM_ENERGY_MODEL));
    Assertions.assertEquals(5, explanation.dominantEvidence().size());
  }

  @Test
  void explainerUsesScoreBreakdownComponentsAsPredictionEvidence() {
    final ReactionProductCandidate primary =
        ReactionPredictionExplainerTest.candidateWithScoreBreakdown(
            "component_explained_product",
            0.58,
            List.of(),
            ReactionPredictionScoreBreakdown.of(
                1.70,
                0.58,
                List.of(
                    ReactionPredictionScoreComponent.of(
                        ReactionPredictionScoreComponentKind.ELECTRONIC,
                        "Java-native frontier orbital alignment favors this channel.",
                        0.42),
                    ReactionPredictionScoreComponent.of(
                        ReactionPredictionScoreComponentKind.KINETIC,
                        "Mechanistic barrier is lower than competing channel.",
                        0.24))));
    final ReactionProductCandidate competing =
        ReactionPredictionExplainerTest.candidate("component_minor_product", 0.31, List.of());
    final ReactionPredictionResult result =
        ReactionPredictionResult.of(
            ReactionPredictionExplainerTest.request(
                "score_component_explanation",
                ReactionConditions.EMPTY,
                List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
                List.of(primary, competing)),
            ReactionPredictionMethodType.HYBRID,
            List.of(primary, competing),
            List.of());

    final ReactionPredictionExplanation explanation =
        new ReactionPredictionExplainer().explain(result);

    Assertions.assertTrue(
        explanation.dominantEvidence().stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR
                        && evidence.description().contains("frontier orbital")));
    Assertions.assertTrue(
        explanation.dominantEvidence().stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.KINETIC_FACTOR
                        && evidence.description().contains("barrier")));
    Assertions.assertTrue(explanation.positiveEvidenceCount() >= 2);
  }

  @Test
  void explainerDoesNotUseCalibrationScoreComponentAsPredictionEvidence() {
    final ReactionProductCandidate primary =
        ReactionPredictionExplainerTest.candidateWithScoreBreakdown(
            "calibration_component_product",
            0.58,
            List.of(),
            ReactionPredictionScoreBreakdown.of(
                2.50,
                0.58,
                List.of(
                    ReactionPredictionScoreComponent.of(
                        ReactionPredictionScoreComponentKind.CALIBRATION,
                        "Historical benchmark row matched this product.",
                        2.0))));
    final ReactionProductCandidate competing =
        ReactionPredictionExplainerTest.candidate("calibration_component_minor", 0.31, List.of());
    final ReactionPredictionResult result =
        ReactionPredictionResult.of(
            ReactionPredictionExplainerTest.request(
                "calibration_component_ignored",
                ReactionConditions.EMPTY,
                List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
                List.of(primary, competing)),
            ReactionPredictionMethodType.HYBRID,
            List.of(primary, competing),
            List.of());
    final ReactionProductCandidate baselinePrimary =
        ReactionPredictionExplainerTest.candidate("calibration_component_product", 0.58, List.of());
    final ReactionPredictionResult baseline =
        ReactionPredictionResult.of(
            ReactionPredictionExplainerTest.request(
                "calibration_component_baseline",
                ReactionConditions.EMPTY,
                List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
                List.of(baselinePrimary, competing)),
            ReactionPredictionMethodType.HYBRID,
            List.of(baselinePrimary, competing),
            List.of());

    final ReactionPredictionExplanation explanation =
        new ReactionPredictionExplainer().explain(result);
    final ReactionPredictionExplanation baselineExplanation =
        new ReactionPredictionExplainer().explain(baseline);

    Assertions.assertTrue(explanation.dominantEvidence().isEmpty());
    Assertions.assertTrue(explanation.riskEvidence().isEmpty());
    Assertions.assertEquals(0, explanation.positiveEvidenceCount());
    Assertions.assertEquals(baselineExplanation.confidenceLevel(), explanation.confidenceLevel());
    Assertions.assertEquals(
        baselineExplanation.probabilityGap(), explanation.probabilityGap(), 1.0E-12);
  }

  @Test
  void explainerDetectsCompetingRegioisomersWhenCandidateGapIsSmall() {
    final ReactionProductCandidate primary =
        ReactionPredictionExplainerTest.candidate(
            "ortho_like",
            0.42,
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
                    "Electronic activation favors the first site.",
                    0.12)));
    final ReactionProductCandidate competing =
        ReactionPredictionExplainerTest.candidate(
            "para_like",
            0.36,
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.STERIC_FACTOR,
                    "Steric access keeps the second site competitive.",
                    0.08)));
    final ReactionPredictionResult result =
        ReactionPredictionResult.of(
            ReactionPredictionExplainerTest.request(
                "competing_regioisomers",
                ReactionConditions.EMPTY,
                List.of(OrganicReactionDomain.ELECTROPHILIC_SUBSTITUTION),
                List.of(primary, competing)),
            ReactionPredictionMethodType.RULE_BASED,
            List.of(primary, competing),
            List.of());

    final ReactionPredictionExplanation explanation =
        new ReactionPredictionExplainer().explain(result);

    Assertions.assertEquals(
        ReactionMechanismClass.ELECTROPHILIC_AROMATIC_SUBSTITUTION,
        explanation.pathwayAssessment().mechanismClass());
    Assertions.assertEquals(
        ReactionRegioselectivityClass.COMPETING_REGIOISOMERS,
        explanation.pathwayAssessment().regioselectivityClass());
    Assertions.assertFalse(explanation.pathwayAssessment().supportingEvidence().isEmpty());
  }

  @Test
  void explainerDowngradesConfidenceWhenProbabilityMassIsIncomplete() {
    final ReactionProductCandidate primary =
        ReactionPredictionExplainerTest.candidate(
            "incomplete_mass_product",
            0.72,
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN,
                    "Dominant product is plausible, but side products remain unassigned.",
                    0.20)));
    final ReactionPredictionResult result =
        ReactionPredictionResult.of(
            ReactionPredictionExplainerTest.request(
                "incomplete_probability_mass",
                ReactionConditions.EMPTY,
                List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
                List.of(primary)),
            ReactionPredictionMethodType.USER_SUPPLIED,
            List.of(primary),
            List.of());

    final ReactionPredictionExplanation explanation =
        new ReactionPredictionExplainer().explain(result);

    Assertions.assertEquals(
        ReactionPredictionConfidenceLevel.MEDIUM, explanation.confidenceLevel());
    Assertions.assertEquals(0.28, result.missingProbabilityMass(), 1.0E-12);
  }

  @Test
  void explainerDowngradesConfidenceWhenRiskEvidenceIsDominant() {
    final ReactionProductCandidate primary =
        ReactionPredictionExplainerTest.candidate(
            "risk_dominated_product",
            0.78,
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN,
                    "Main heterocycle pattern matches.",
                    0.18)));
    final ReactionPredictionResult result =
        ReactionPredictionResult.of(
            ReactionPredictionExplainerTest.request(
                "risk_dominated_prediction",
                ReactionConditions.EMPTY,
                List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
                List.of(primary)),
            ReactionPredictionMethodType.HYBRID,
            List.of(primary),
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.WARNING,
                    "Condition set is contradictory and unresolved.",
                    -0.35)));

    final ReactionPredictionExplanation explanation =
        new ReactionPredictionExplainer().explain(result);

    Assertions.assertEquals(ReactionPredictionConfidenceLevel.LOW, explanation.confidenceLevel());
    Assertions.assertTrue(explanation.hasRisks());
  }

  @Test
  void explainerDetectsCatalystAndLigandDrivenCrossCouplingWindow() {
    final ReactionProductCandidate primary =
        ReactionPredictionExplainerTest.candidate(
            "coupled_product",
            0.66,
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.CATALYST_EFFECT,
                    "Palladium catalyst and ligand are compatible with cross-coupling.",
                    0.18)));
    final ReactionConditions conditions =
        ReactionConditions.of(
            null,
            null,
            null,
            List.of(
                ReactionConditionComponent.named(ReactionComponentRole.CATALYST, "Pd(PPh3)4"),
                ReactionConditionComponent.named(ReactionComponentRole.LIGAND, "PPh3")),
            List.of(ReactionConditionFeature.named(ReactionConditionFeatureKind.INERT_ATMOSPHERE)),
            "cross-coupling screen");
    final ReactionPredictionResult result =
        ReactionPredictionResult.of(
            ReactionPredictionExplainerTest.request(
                "cross_coupling_pathway",
                conditions,
                List.of(OrganicReactionDomain.UNKNOWN),
                List.of(primary)),
            ReactionPredictionMethodType.HYBRID,
            List.of(primary),
            List.of());

    final ReactionPredictionExplanation explanation =
        new ReactionPredictionExplainer().explain(result);

    Assertions.assertEquals(
        ReactionMechanismClass.CROSS_COUPLING, explanation.pathwayAssessment().mechanismClass());
    Assertions.assertTrue(
        explanation.pathwayAssessment().conditionWindow().hasRequiredConditionSignals());
    Assertions.assertTrue(
        explanation.pathwayAssessment().conditionWindow().requiredConditionSignals().stream()
            .anyMatch(signal -> signal.contains("CATALYST")));
  }

  @Test
  void explainerRecognizesLeavingGroupDirectedSnarContext() {
    final ReactionProductCandidate primary =
        ReactionPredictionExplainerTest.candidate(
            "amino_heteroaryl_product",
            0.64,
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
                    "Halogenated nitrogen heterocycle undergoes SNAr chloride leaving group replacement.",
                    0.21)));
    final ReactionConditions conditions =
        ReactionConditions.of(
            null,
            null,
            null,
            List.of(ReactionConditionComponent.named(ReactionComponentRole.BASE, "K2CO3")),
            List.of(
                ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM),
                ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING)),
            "basic heated SNAr screen");
    final ReactionPredictionResult result =
        ReactionPredictionResult.of(
            ReactionPredictionExplainerTest.request(
                "snar_pathway",
                conditions,
                List.of(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
                List.of(primary)),
            ReactionPredictionMethodType.HYBRID,
            List.of(primary),
            List.of());

    final ReactionPredictionExplanation explanation =
        new ReactionPredictionExplainer().explain(result);

    Assertions.assertEquals(
        ReactionMechanismClass.NUCLEOPHILIC_AROMATIC_SUBSTITUTION,
        explanation.pathwayAssessment().mechanismClass());
    Assertions.assertEquals(
        ReactionRegioselectivityClass.LEAVING_GROUP_DIRECTED,
        explanation.pathwayAssessment().regioselectivityClass());
    Assertions.assertTrue(explanation.conditionSuitability().explicitConditions());
  }

  @Test
  void explainerSeparatesSulfurHeterocycleOxidationFromGenericRedox() {
    final ReactionProductCandidate primary =
        ReactionPredictionExplainerTest.candidate(
            "s_oxide_product",
            0.58,
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.THERMODYNAMIC_FACTOR,
                    "Sulfur heterocycle oxidation gives S-oxide analogue under controlled oxidant addition.",
                    0.24)));
    final ReactionPredictionResult result =
        ReactionPredictionResult.of(
            ReactionPredictionExplainerTest.request(
                "sulfur_oxidation_pathway",
                ReactionConditions.of(
                    null,
                    null,
                    null,
                    List.of(),
                    List.of(
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.ADDITION_RATE)),
                    "controlled oxidant addition"),
                List.of(OrganicReactionDomain.OXIDATION_REDUCTION),
                List.of(primary)),
            ReactionPredictionMethodType.RULE_BASED,
            List.of(primary),
            List.of());

    final ReactionPredictionExplanation explanation =
        new ReactionPredictionExplainer().explain(result);

    Assertions.assertEquals(
        ReactionMechanismClass.OXIDATION, explanation.pathwayAssessment().mechanismClass());
    Assertions.assertTrue(
        explanation.pathwayAssessment().supportingEvidence().stream()
            .anyMatch(evidence -> evidence.description().contains("S-oxide")));
  }

  @Test
  void explainerMarksCloseConditionSensitiveCandidatesAsConditionControlled() {
    final ReactionProductCandidate firstSite =
        ReactionPredictionExplainerTest.candidate(
            "heated_site",
            0.52,
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.TEMPERATURE_EFFECT,
                    "Heating favors the kinetically accessible heteroaryl site.",
                    0.09)));
    final ReactionProductCandidate secondSite =
        ReactionPredictionExplainerTest.candidate(
            "cooled_site",
            0.25,
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.STERIC_FACTOR,
                    "Steric access keeps the second site plausible.",
                    0.07)));
    final ReactionConditions conditions =
        ReactionConditions.of(
            null,
            null,
            null,
            List.of(ReactionConditionComponent.named(ReactionComponentRole.SOLVENT, "DMSO")),
            List.of(ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING)),
            "condition-controlled regioselectivity screen");
    final ReactionPredictionResult result =
        ReactionPredictionResult.of(
            ReactionPredictionExplainerTest.request(
                "condition_controlled_regioselectivity",
                conditions,
                List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
                List.of(firstSite, secondSite)),
            ReactionPredictionMethodType.HYBRID,
            List.of(firstSite, secondSite),
            List.of());

    final ReactionPredictionExplanation explanation =
        new ReactionPredictionExplainer().explain(result);

    Assertions.assertEquals(
        ReactionRegioselectivityClass.CONDITION_CONTROLLED,
        explanation.pathwayAssessment().regioselectivityClass());
    Assertions.assertTrue(
        explanation.pathwayAssessment().conditionWindow().hasRequiredConditionSignals());
  }

  @Test
  void explainerCarriesDetailedMechanismRouteFromBestCandidate() {
    final ReactionProductCandidate primary =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ReactionPredictionExplainerTest.molecule("sn2_product"),
                        StoichiometricCoefficient.ONE))),
            0.74,
            62.0,
            "leaving-group directed substitution",
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
                    "Halogenated substrate and base support SN2 displacement.",
                    0.20)),
            List.of(
                PredictedReactionMechanism.of(
                    "sn2_route",
                    ReactionMechanismFamily.NUCLEOPHILIC_SUBSTITUTION_SN2,
                    0.74,
                    62.0)));
    final ReactionPredictionResult result =
        ReactionPredictionResult.of(
            ReactionPredictionExplainerTest.request(
                "sn2_route_explanation",
                ReactionConditions.of(
                    null,
                    null,
                    null,
                    List.of(ReactionConditionComponent.named(ReactionComponentRole.BASE, "K2CO3")),
                    List.of(
                        ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM)),
                    "basic substitution"),
                List.of(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
                List.of(primary)),
            ReactionPredictionMethodType.HYBRID,
            List.of(primary),
            List.of());

    final ReactionPredictionExplanation explanation =
        new ReactionPredictionExplainer().explain(result);

    Assertions.assertTrue(explanation.pathwayAssessment().hasPathwaySteps());
    Assertions.assertTrue(
        explanation.pathwayAssessment().pathwaySteps().stream()
            .anyMatch(step -> step.eventKind() == MechanisticEventKind.NUCLEOPHILIC_ATTACK));
    Assertions.assertTrue(
        explanation.pathwayAssessment().pathwaySteps().stream()
            .allMatch(step -> step.mechanismId().equals("sn2_route")));
  }

  @Test
  void builtInMechanismCatalogCoversEveryFamilyWithDetailedProbabilitySteps() {
    final ReactionMechanismPatternCatalog catalog =
        BuiltInReactionMechanismPatternCatalog.organicAndPhysicalChemistry();
    final EnumSet<ReactionMechanismFamily> covered = EnumSet.noneOf(ReactionMechanismFamily.class);

    for (int i = 0; i < catalog.patterns().size(); ++i) {
      final ReactionMechanismPattern pattern = catalog.patterns().get(i);
      covered.add(pattern.family());

      Assertions.assertFalse(pattern.displayName().isBlank());
      Assertions.assertFalse(pattern.eventSequence().isEmpty());
      Assertions.assertFalse(pattern.detailedSteps().isEmpty());
      Assertions.assertTrue(pattern.pathwayProbabilityContribution() > 0.0);
      Assertions.assertTrue(pattern.pathwayProbabilityContribution() <= 1.0);
      Assertions.assertFalse(pattern.applicabilitySignals().isEmpty());
      Assertions.assertFalse(pattern.riskSignals().isEmpty());

      for (int stepIndex = 0; stepIndex < pattern.detailedSteps().size(); ++stepIndex) {
        final ReactionMechanismPatternStep step = pattern.detailedSteps().get(stepIndex);
        Assertions.assertEquals(stepIndex, step.orderIndex());
        Assertions.assertNotNull(step.eventKind());
        Assertions.assertFalse(step.stageName().isBlank());
        Assertions.assertFalse(step.description().isBlank());
        Assertions.assertTrue(step.probabilityContribution() > 0.0);
        Assertions.assertTrue(step.probabilityContribution() <= 1.0);
        Assertions.assertTrue(step.hasApplicabilitySignals());
        Assertions.assertTrue(step.hasRiskSignals());
      }
    }

    Assertions.assertEquals(EnumSet.allOf(ReactionMechanismFamily.class), covered);
    ReactionPredictionExplainerTest.assertDetailedRoute(
        catalog, ReactionMechanismFamily.NUCLEOPHILIC_SUBSTITUTION_SN1);
    ReactionPredictionExplainerTest.assertDetailedRoute(
        catalog, ReactionMechanismFamily.NUCLEOPHILIC_SUBSTITUTION_SN2);
    ReactionPredictionExplainerTest.assertDetailedRoute(
        catalog, ReactionMechanismFamily.ORGANOMETALLIC_CROSS_COUPLING);
    ReactionPredictionExplainerTest.assertDetailedRoute(
        catalog, ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION);
    ReactionPredictionExplainerTest.assertDetailedRoute(
        catalog, ReactionMechanismFamily.HETEROCYCLE_FUNCTIONALIZATION);
    ReactionPredictionExplainerTest.assertDetailedRoute(
        catalog, ReactionMechanismFamily.PHOTOCHEMICAL);
    ReactionPredictionExplainerTest.assertDetailedRoute(
        catalog, ReactionMechanismFamily.ELECTROCHEMICAL);
    ReactionPredictionExplainerTest.assertDetailedRoute(
        catalog, ReactionMechanismFamily.REDUCTIVE_AMINATION);
    ReactionPredictionExplainerTest.assertDetailedRoute(
        catalog, ReactionMechanismFamily.OLEFINATION);
    ReactionPredictionExplainerTest.assertDetailedRoute(
        catalog, ReactionMechanismFamily.HYDROBORATION_OXIDATION);
    ReactionPredictionExplainerTest.assertDetailedRoute(
        catalog, ReactionMechanismFamily.EPOXIDATION);
    ReactionPredictionExplainerTest.assertDetailedRoute(
        catalog, ReactionMechanismFamily.OZONOLYSIS);
    ReactionPredictionExplainerTest.assertDetailedRoute(
        catalog, ReactionMechanismFamily.BAEYER_VILLIGER);
    ReactionPredictionExplainerTest.assertDetailedRoute(
        catalog, ReactionMechanismFamily.SUZUKI_COUPLING);
    ReactionPredictionExplainerTest.assertDetailedRoute(
        catalog, ReactionMechanismFamily.HECK_COUPLING);
    ReactionPredictionExplainerTest.assertDetailedRoute(
        catalog, ReactionMechanismFamily.SONOGASHIRA_COUPLING);
    ReactionPredictionExplainerTest.assertDetailedRoute(
        catalog, ReactionMechanismFamily.BUCHWALD_HARTWIG_AMINATION);
    ReactionPredictionExplainerTest.assertDetailedRoute(
        catalog, ReactionMechanismFamily.DIELS_ALDER);
    ReactionPredictionExplainerTest.assertDetailedRoute(
        catalog, ReactionMechanismFamily.BECKMANN_REARRANGEMENT);
    ReactionPredictionExplainerTest.assertDetailedRoute(
        catalog, ReactionMechanismFamily.CLICK_CYCLOADDITION);
    ReactionPredictionExplainerTest.assertDetailedRoute(
        catalog, ReactionMechanismFamily.MULTICOMPONENT_CONDENSATION);
    ReactionPredictionExplainerTest.assertDetailedRoute(
        catalog, ReactionMechanismFamily.ORGANOCATALYSIS);
    ReactionPredictionExplainerTest.assertProductionRoute(
        catalog, ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION);
    ReactionPredictionExplainerTest.assertProductionRoute(
        catalog, ReactionMechanismFamily.HETEROCYCLE_FUNCTIONALIZATION);
    ReactionPredictionExplainerTest.assertProductionRoute(
        catalog, ReactionMechanismFamily.REDUCTIVE_AMINATION);
    ReactionPredictionExplainerTest.assertProductionRoute(
        catalog, ReactionMechanismFamily.SUZUKI_COUPLING);
    ReactionPredictionExplainerTest.assertProductionRoute(
        catalog, ReactionMechanismFamily.SONOGASHIRA_COUPLING);
    ReactionPredictionExplainerTest.assertProductionRoute(
        catalog, ReactionMechanismFamily.BUCHWALD_HARTWIG_AMINATION);
    ReactionPredictionExplainerTest.assertProductionRoute(
        catalog, ReactionMechanismFamily.DIELS_ALDER);
    ReactionPredictionExplainerTest.assertProductionRoute(
        catalog, ReactionMechanismFamily.CLICK_CYCLOADDITION);
    ReactionPredictionExplainerTest.assertProductionRoute(
        catalog, ReactionMechanismFamily.RADICAL_CYCLIZATION);
    ReactionPredictionExplainerTest.assertProductionRoute(
        catalog, ReactionMechanismFamily.C_H_ACTIVATION);
    ReactionPredictionExplainerTest.assertProductionRoute(
        catalog, ReactionMechanismFamily.FISCHER_INDOLE_SYNTHESIS);
    ReactionPredictionExplainerTest.assertProductionRoute(
        catalog, ReactionMechanismFamily.SKRAUP_SYNTHESIS);
    ReactionPredictionExplainerTest.assertProductionRoute(
        catalog, ReactionMechanismFamily.COMBES_QUINOLINE_ANNULATION);
    ReactionPredictionExplainerTest.assertProductionRoute(
        catalog, ReactionMechanismFamily.HANTZSCH_SYNTHESIS);
    ReactionPredictionExplainerTest.assertProductionRoute(
        catalog, ReactionMechanismFamily.BIGINELLI_REACTION);
  }

  private static void assertDetailedRoute(
      final ReactionMechanismPatternCatalog catalog, final ReactionMechanismFamily family) {
    Assertions.assertTrue(catalog.require(family).detailedSteps().size() >= 3);
  }

  private static void assertProductionRoute(
      final ReactionMechanismPatternCatalog catalog, final ReactionMechanismFamily family) {
    final ReactionMechanismPattern pattern = catalog.require(family);
    Assertions.assertTrue(pattern.detailedSteps().size() >= 5, family.name());
    Assertions.assertTrue(pattern.pathwayProbabilityContribution() >= 0.95, family.name());
    for (int stepIndex = 0; stepIndex < pattern.detailedSteps().size(); ++stepIndex) {
      final ReactionMechanismPatternStep step = pattern.detailedSteps().get(stepIndex);
      Assertions.assertFalse(step.stageName().startsWith("Canonical "), family.name());
      Assertions.assertFalse(step.description().startsWith("Canonical "), family.name());
    }
  }

  private static ReactionProductCandidate candidate(
      final String id, final double probability, final List<ReactionPredictionEvidence> evidence) {
    return ReactionPredictionExplainerTest.candidateWithScoreBreakdown(
        id, probability, evidence, ReactionPredictionScoreBreakdown.empty(probability));
  }

  private static ReactionProductCandidate candidateWithScoreBreakdown(
      final String id,
      final double probability,
      final List<ReactionPredictionEvidence> evidence,
      final ReactionPredictionScoreBreakdown scoreBreakdown) {
    return ReactionProductCandidate.of(
        ReactionSide.of(
            List.of(
                ReactionParticipant.of(
                    ReactionPredictionExplainerTest.molecule(id), StoichiometricCoefficient.ONE))),
        probability,
        probability * 100.0,
        id,
        evidence,
        List.of(),
        scoreBreakdown);
  }

  private static ReactionPredictionRequest request(
      final String id,
      final ReactionConditions conditions,
      final List<OrganicReactionDomain> domains,
      final List<ReactionProductCandidate> candidates) {
    return ReactionPredictionRequest.of(
        id,
        ReactionSide.of(
            List.of(
                ReactionParticipant.of(
                    ReactionPredictionExplainerTest.molecule(id + "_reactant"),
                    StoichiometricCoefficient.ONE))),
        conditions,
        domains,
        candidates,
        id + " request");
  }

  private static Molecule molecule(final String id) {
    return Molecule.of(
        MoleculeId.of(id),
        id,
        List.of(
            Atom.of(
                AtomId.of(id + "_c0"),
                ElementSymbol.of("C"),
                Coordinate3D.of(0.0, 0.0, 0.0, LengthUnit.ANGSTROM))),
        List.of(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }
}

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
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionMechanismConditionAlignmentScorer;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.PredictedReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
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
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Temperature;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TemperatureUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TimeDuration;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TimeDurationUnit;

final class ReactionMechanismConditionAlignmentScorerTest {

  @Test
  void polarBasicHeatedPolyetherConditionsSupportSnarAndAnrorcButPenalizeUncatalyzedCoupling() {
    final ReactionMechanismConditionAlignmentScorer scorer =
        new ReactionMechanismConditionAlignmentScorer();
    final ReactionConditions conditions =
        ReactionConditions.of(
            Temperature.of(120.0, TemperatureUnit.CELSIUS),
            null,
            TimeDuration.of(3.0, TimeDurationUnit.HOUR),
            List.of(
                ReactionConditionComponent.named(ReactionComponentRole.SOLVENT, "PEG4000"),
                ReactionConditionComponent.named(ReactionComponentRole.BASE, "carbonate base")),
            List.of(
                ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM),
                ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS),
                ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING)),
            "heated viscous polar aprotic polyether heteroaryl amination screen");
    final ArrayList<ReactionPredictionEvidence> snarEvidence =
        new ArrayList<ReactionPredictionEvidence>();
    final ArrayList<ReactionPredictionEvidence> anrorcEvidence =
        new ArrayList<ReactionPredictionEvidence>();
    final ArrayList<ReactionPredictionEvidence> couplingEvidence =
        new ArrayList<ReactionPredictionEvidence>();

    final double snarScore =
        scorer.score(
            conditions,
            ReactionMechanismConditionAlignmentScorerTest.candidate(
                ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION),
            snarEvidence);
    final double anrorcScore =
        scorer.score(
            conditions,
            ReactionMechanismConditionAlignmentScorerTest.candidate(
                ReactionMechanismFamily.ADDITION_RING_OPENING_RING_CLOSURE),
            anrorcEvidence);
    final double couplingScore =
        scorer.score(
            conditions,
            ReactionMechanismConditionAlignmentScorerTest.candidate(
                ReactionMechanismFamily.SUZUKI_COUPLING),
            couplingEvidence);

    Assertions.assertTrue(snarScore > 0.0, snarEvidence.toString());
    Assertions.assertTrue(anrorcScore > 0.0, anrorcEvidence.toString());
    Assertions.assertTrue(couplingScore < 0.0, couplingEvidence.toString());
    Assertions.assertTrue(
        snarEvidence.stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.KINETIC_FACTOR
                        && evidence.description().contains("NUCLEOPHILIC_AROMATIC_SUBSTITUTION")));
    Assertions.assertTrue(
        couplingEvidence.stream()
            .anyMatch(
                evidence ->
                    evidence.kind() == ReactionPredictionEvidenceKind.COMPETING_PATHWAY
                        && evidence.description().contains("SUZUKI_COUPLING")));
  }

  @Test
  void catalyticDryBasicConditionsSupportCrossCouplingMoreThanDirectSnar() {
    final ReactionMechanismConditionAlignmentScorer scorer =
        new ReactionMechanismConditionAlignmentScorer();
    final ReactionConditions conditions =
        ReactionConditions.of(
            Temperature.of(85.0, TemperatureUnit.CELSIUS),
            null,
            TimeDuration.of(8.0, TimeDurationUnit.HOUR),
            List.of(
                ReactionConditionComponent.named(
                    ReactionComponentRole.CATALYST, "palladium catalyst"),
                ReactionConditionComponent.named(
                    ReactionComponentRole.LIGAND, "bulky phosphine ligand"),
                ReactionConditionComponent.named(ReactionComponentRole.BASE, "carbonate base"),
                ReactionConditionComponent.named(ReactionComponentRole.SOLVENT, "toluene")),
            List.of(
                ReactionConditionFeature.named(ReactionConditionFeatureKind.CATALYST_PRESENT),
                ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM),
                ReactionConditionFeature.named(ReactionConditionFeatureKind.INERT_ATMOSPHERE),
                ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS),
                ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING)),
            "dry ligand-supported palladium coupling screen");
    final ArrayList<ReactionPredictionEvidence> couplingEvidence =
        new ArrayList<ReactionPredictionEvidence>();
    final ArrayList<ReactionPredictionEvidence> snarEvidence =
        new ArrayList<ReactionPredictionEvidence>();

    final double couplingScore =
        scorer.score(
            conditions,
            ReactionMechanismConditionAlignmentScorerTest.candidate(
                ReactionMechanismFamily.BUCHWALD_HARTWIG_AMINATION),
            couplingEvidence);
    final double snarScore =
        scorer.score(
            conditions,
            ReactionMechanismConditionAlignmentScorerTest.candidate(
                ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION),
            snarEvidence);

    Assertions.assertTrue(couplingScore > snarScore, couplingEvidence + " / " + snarEvidence);
    Assertions.assertTrue(
        couplingEvidence.stream()
            .anyMatch(evidence -> evidence.description().contains("BUCHWALD_HARTWIG_AMINATION")));
  }

  @Test
  void redoxAndRadicalSignalsSeparateOxidationReductionAndRadicalRoutes() {
    final ReactionMechanismConditionAlignmentScorer scorer =
        new ReactionMechanismConditionAlignmentScorer();
    final ReactionConditions conditions =
        ReactionConditions.of(
            Temperature.of(25.0, TemperatureUnit.CELSIUS),
            null,
            TimeDuration.of(2.0, TimeDurationUnit.HOUR),
            List.of(
                ReactionConditionComponent.named(ReactionComponentRole.ADDITIVE, "mCPBA oxidant"),
                ReactionConditionComponent.named(
                    ReactionComponentRole.ADDITIVE, "blue light photoredox initiator")),
            List.of(
                ReactionConditionFeature.named(ReactionConditionFeatureKind.OXIDIZING_MEDIUM),
                ReactionConditionFeature.named(ReactionConditionFeatureKind.PHOTOCHEMICAL)),
            "oxidative photoredox screen");
    final ArrayList<ReactionPredictionEvidence> oxidationEvidence =
        new ArrayList<ReactionPredictionEvidence>();
    final ArrayList<ReactionPredictionEvidence> reductionEvidence =
        new ArrayList<ReactionPredictionEvidence>();
    final ArrayList<ReactionPredictionEvidence> radicalEvidence =
        new ArrayList<ReactionPredictionEvidence>();

    final double oxidationScore =
        scorer.score(
            conditions,
            ReactionMechanismConditionAlignmentScorerTest.candidate(
                ReactionMechanismFamily.OXIDATION),
            oxidationEvidence);
    final double reductionScore =
        scorer.score(
            conditions,
            ReactionMechanismConditionAlignmentScorerTest.candidate(
                ReactionMechanismFamily.REDUCTION),
            reductionEvidence);
    final double radicalScore =
        scorer.score(
            conditions,
            ReactionMechanismConditionAlignmentScorerTest.candidate(
                ReactionMechanismFamily.RADICAL_CHAIN),
            radicalEvidence);

    Assertions.assertTrue(oxidationScore > 0.0, oxidationEvidence.toString());
    Assertions.assertTrue(radicalScore > 0.0, radicalEvidence.toString());
    Assertions.assertTrue(reductionScore < 0.0, reductionEvidence.toString());
  }

  private static ReactionProductCandidate candidate(final ReactionMechanismFamily family) {
    return ReactionProductCandidate.of(
        ReactionSide.of(
            List.of(
                ReactionParticipant.of(
                    ReactionMechanismConditionAlignmentScorerTest.singleCarbonMolecule(),
                    StoichiometricCoefficient.ONE))),
        0.5,
        50.0,
        "Mechanism condition alignment probe.",
        List.of(),
        List.of(
            PredictedReactionMechanism.of(
                "alignment_probe_" + family.name().toLowerCase(), family, 0.5, 50.0)));
  }

  private static Molecule singleCarbonMolecule() {
    return Molecule.of(
        MoleculeId.of("alignment_probe_molecule"),
        "Alignment probe molecule",
        List.of(Atom.of(AtomId.of("c0"), ElementSymbol.of("C"), null)),
        List.of(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }
}

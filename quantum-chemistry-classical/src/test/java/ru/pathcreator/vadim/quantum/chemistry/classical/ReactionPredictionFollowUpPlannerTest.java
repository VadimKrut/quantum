/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionPredictionFollowUpPlan;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionPredictionFollowUpPlanner;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionPredictionFollowUpUrgency;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.stoichiometry.ReactionStoichiometryProfile;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.prediction.ReactionPredictionApplicabilityReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.prediction.ReactionPredictionReadinessLevel;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionMethodType;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
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

final class ReactionPredictionFollowUpPlannerTest {

  @Test
  void plannerAddsConditionScreeningWhenMechanismIsPenalizedByConditionProfile() {
    final ReactionProductCandidate candidate =
        ReactionProductCandidate.of(
            ReactionPredictionFollowUpPlannerTest.side("condition_conflict_product"),
            0.65,
            55.0,
            "Condition/mechanism conflict probe.",
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.COMPETING_PATHWAY,
                    "Condition mechanism profile penalizes SUZUKI_COUPLING using solvent/catalyst/temperature/redox/radical signals.",
                    -0.05)));
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "condition_conflict_follow_up",
            ReactionPredictionFollowUpPlannerTest.side("condition_conflict_reactant"),
            ReactionConditions.EMPTY,
            List.of(OrganicReactionDomain.CROSS_COUPLING),
            List.of(),
            null);
    final ReactionPredictionResult result =
        ReactionPredictionResult.of(
            request, ReactionPredictionMethodType.HYBRID, List.of(candidate), List.of());
    final ReactionPredictionApplicabilityReport applicability =
        ReactionPredictionApplicabilityReport.of(
            ReactionPredictionReadinessLevel.READY_FOR_DECISION_SUPPORT,
            0.82,
            false,
            false,
            0,
            0,
            1,
            0,
            0,
            0,
            List.of());

    final ReactionPredictionFollowUpPlan plan =
        new ReactionPredictionFollowUpPlanner()
            .plan(result, applicability, ReactionStoichiometryProfile.EMPTY);

    Assertions.assertTrue(
        plan.steps().stream()
            .anyMatch(
                step ->
                    step.calculationKind() == ClassicalCalculationKind.REACTION_CONDITION_SCREENING
                        && step.urgency() == ReactionPredictionFollowUpUrgency.RECOMMENDED
                        && step.reason().contains("penalized")),
        plan.steps().toString());
  }

  private static ReactionSide side(final String id) {
    return ReactionSide.of(
        List.of(
            ReactionParticipant.of(
                ReactionPredictionFollowUpPlannerTest.singleCarbonMolecule(id),
                StoichiometricCoefficient.ONE)));
  }

  private static Molecule singleCarbonMolecule(final String id) {
    return Molecule.of(
        MoleculeId.of(id),
        id,
        List.of(Atom.of(AtomId.of("c0"), ElementSymbol.of("C"), null)),
        List.of(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }
}

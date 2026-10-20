/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ElementaryReactionStep;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanisticEvent;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanisticEventKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.PredictedReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionConditionWindow;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionMechanismClass;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPathwayAssessment;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionConfidenceLevel;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionMethodType;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionRegioselectivityClass;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeature;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionId;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.StoichiometricCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TimeDuration;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TimeDurationUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.validation.ChemistryCoreValidator;

final class ChemistryCorePredictionTest {

  @Test
  void reactionConditionsKeepDurationAndStructuredFeatures() {
    final ReactionConditions conditions =
        ReactionConditions.of(
            null,
            null,
            TimeDuration.of(18.0, TimeDurationUnit.HOUR),
            List.of(),
            List.of(
                ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS),
                ReactionConditionFeature.of(
                    ReactionConditionFeatureKind.INERT_ATMOSPHERE, "nitrogen", "Schlenk line")),
            "heterocycle coupling screen");

    Assertions.assertTrue(conditions.hasDuration());
    Assertions.assertEquals(64800.0, conditions.duration().seconds());
    Assertions.assertTrue(conditions.hasFeature(ReactionConditionFeatureKind.ANHYDROUS));
    Assertions.assertEquals(2, conditions.features().size());
    Assertions.assertThrows(
        UnsupportedOperationException.class, () -> conditions.features().clear());
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ReactionConditions.of(
                null,
                null,
                null,
                List.of(),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS)),
                null));
  }

  @Test
  void predictionRequestValidatesCandidatesAndKeepsImmutableCollections() {
    final Molecule pyridine = ChemistryCorePredictionTest.ringMolecule("pyridine", "N");
    final ReactionProductCandidate candidate =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(ReactionParticipant.of(pyridine, StoichiometricCoefficient.ONE))),
            0.75,
            68.0,
            "major heterocycle candidate",
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN,
                    "six-membered nitrogen heterocycle",
                    0.4)),
            List.of(
                PredictedReactionMechanism.of(
                    "heterocycle_pathway",
                    ReactionMechanismFamily.HETEROCYCLE_FUNCTIONALIZATION,
                    0.75,
                    68.0)));
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "heterocycle_prediction",
            ReactionSide.of(
                List.of(ReactionParticipant.of(pyridine, StoichiometricCoefficient.ONE))),
            ReactionConditions.EMPTY,
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(candidate),
            "screen");

    Assertions.assertTrue(request.hasSeedCandidates());
    Assertions.assertTrue(request.seedCandidates().get(0).hasPredictedMechanisms());
    Assertions.assertThrows(
        UnsupportedOperationException.class, () -> request.seedCandidates().clear());
    Assertions.assertTrue(
        new ChemistryCoreValidator().validateReactionPredictionRequest(request).valid());
  }

  @Test
  void reactionMechanismKeepsDetailedEventsAndValidatorReportsMissingOnes() {
    final Molecule pyridine = ChemistryCorePredictionTest.ringMolecule("pyridine", "N");
    final Reaction identityReaction =
        Reaction.of(
            ReactionId.of("pyridine_identity"),
            "Pyridine identity probe",
            ReactionSide.of(
                List.of(ReactionParticipant.of(pyridine, StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(ReactionParticipant.of(pyridine, StoichiometricCoefficient.ONE))));
    final ElementaryReactionStep eventStep =
        ElementaryReactionStep.of(
            "association",
            identityReaction,
            null,
            null,
            null,
            null,
            List.of(
                MechanisticEvent.of(
                    "complexation",
                    0,
                    MechanisticEventKind.COMPLEXATION,
                    "Nitrogen lone pair coordinates a transient electrophile.",
                    List.of("pyridine.a0"),
                    0.62)));
    final ReactionMechanism mechanism =
        ReactionMechanism.of(
            "pyridine_complexation",
            ReactionMechanismFamily.HETEROCYCLE_FUNCTIONALIZATION,
            identityReaction,
            List.of(eventStep),
            List.of());
    final ReactionMechanism noEventMechanism =
        ReactionMechanism.of(
            "pyridine_no_event",
            ReactionMechanismFamily.UNKNOWN,
            identityReaction,
            List.of(ElementaryReactionStep.of("silent", identityReaction, null, null, null, null)),
            List.of());

    Assertions.assertEquals(1, mechanism.eventCount());
    Assertions.assertEquals(
        ReactionMechanismFamily.HETEROCYCLE_FUNCTIONALIZATION, mechanism.family());
    Assertions.assertTrue(
        new ChemistryCoreValidator()
            .validateReactionMechanism(noEventMechanism).diagnostics().stream()
                .anyMatch(
                    diagnostic ->
                        diagnostic.code()
                            == ChemistryDiagnosticCode.REACTION_MECHANISM_STEP_HAS_NO_EVENTS));
  }

  @Test
  void predictionValidatorReportsMissingAndUnbalancedCandidates() {
    final Molecule pyridine = ChemistryCorePredictionTest.ringMolecule("pyridine", "N");
    final Molecule benzene = ChemistryCorePredictionTest.ringMolecule("benzene", "C");
    final ReactionPredictionRequest emptyRequest =
        ReactionPredictionRequest.of(
            "empty_prediction",
            ReactionSide.of(
                List.of(ReactionParticipant.of(pyridine, StoichiometricCoefficient.ONE))),
            ReactionConditions.EMPTY,
            List.of(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS),
            List.of(),
            null);
    final ReactionPredictionRequest unbalancedRequest =
        ReactionPredictionRequest.of(
            "unbalanced_prediction",
            ReactionSide.of(
                List.of(ReactionParticipant.of(pyridine, StoichiometricCoefficient.ONE))),
            ReactionConditions.EMPTY,
            List.of(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS),
            List.of(
                ReactionProductCandidate.of(
                    ReactionSide.of(
                        List.of(ReactionParticipant.of(benzene, StoichiometricCoefficient.ONE))),
                    0.5,
                    50.0,
                    null,
                    List.of())),
            null);

    Assertions.assertEquals(
        ChemistryDiagnosticCode.REACTION_PREDICTION_HAS_NO_CANDIDATES,
        new ChemistryCoreValidator()
            .validateReactionPredictionRequest(emptyRequest)
            .diagnostics()
            .get(0)
            .code());
    Assertions.assertTrue(
        new ChemistryCoreValidator()
            .validateReactionPredictionRequest(unbalancedRequest).diagnostics().stream()
                .anyMatch(
                    diagnostic ->
                        diagnostic.code()
                            == ChemistryDiagnosticCode
                                .REACTION_PREDICTION_CANDIDATE_ATOMS_NOT_BALANCED));
  }

  @Test
  void predictionResultAggregatesAuditEvidenceWithoutTreatingZeroPriorAsWeightedSignal() {
    final Molecule pyridine = ChemistryCorePredictionTest.ringMolecule("audit_pyridine", "N");
    final ReactionProductCandidate candidate =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(ReactionParticipant.of(pyridine, StoichiometricCoefficient.ONE))),
            0.75,
            64.0,
            "audit candidate",
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.CALIBRATION_PRIOR,
                    "External benchmark row matched only after prediction.",
                    0.0),
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN,
                    "Generic heterocycle transform created this candidate.",
                    0.31)));
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "audit_prediction",
            ReactionSide.of(
                List.of(ReactionParticipant.of(pyridine, StoichiometricCoefficient.ONE))),
            ReactionConditions.EMPTY,
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(candidate),
            null);
    final ReactionPredictionResult result =
        ReactionPredictionResult.of(
            request,
            ReactionPredictionMethodType.HYBRID,
            List.of(candidate),
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.WARNING, "Audit warning.", -0.05)));

    Assertions.assertEquals(
        1, result.evidenceCount(ReactionPredictionEvidenceKind.CALIBRATION_PRIOR));
    Assertions.assertFalse(
        candidate.hasWeightedEvidence(ReactionPredictionEvidenceKind.CALIBRATION_PRIOR));
    Assertions.assertEquals(
        0.0, result.evidenceWeight(ReactionPredictionEvidenceKind.CALIBRATION_PRIOR));
    Assertions.assertFalse(
        result.hasWeightedEvidence(ReactionPredictionEvidenceKind.CALIBRATION_PRIOR));
    Assertions.assertEquals(
        0.31, result.evidenceWeight(ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN));
    Assertions.assertEquals(-0.05, result.evidenceWeight(ReactionPredictionEvidenceKind.WARNING));
  }

  @Test
  void pathwayAssessmentValidatesConditionWindowAndEvidenceCollections() {
    final ReactionPredictionEvidence evidence =
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN,
            "Heteroatom-directed regioselectivity signal.",
            0.25);
    final ReactionConditionWindow window =
        ReactionConditionWindow.of(
            273.15,
            373.15,
            1.0,
            24.0,
            List.of("feature:ANHYDROUS", "component:CATALYST:CuI"),
            "screening window");
    final ReactionPathwayAssessment assessment =
        ReactionPathwayAssessment.of(
            ReactionMechanismClass.HETEROCYCLE_FUNCTIONALIZATION,
            ReactionRegioselectivityClass.HETEROATOM_DIRECTED,
            window,
            ReactionPredictionConfidenceLevel.HIGH,
            List.of(evidence),
            List.of());

    Assertions.assertTrue(assessment.conditionWindow().containsTemperatureKelvin(298.15));
    Assertions.assertTrue(assessment.conditionWindow().containsDurationHours(8.0));
    Assertions.assertTrue(assessment.conditionWindow().hasRequiredConditionSignals());
    Assertions.assertFalse(assessment.hasLimitations());
    Assertions.assertThrows(
        UnsupportedOperationException.class, () -> assessment.supportingEvidence().clear());
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> ReactionConditionWindow.of(373.15, 273.15, 1.0, 2.0, List.of(), null));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> {
          final ArrayList<ReactionPredictionEvidence> invalidEvidence =
              new ArrayList<ReactionPredictionEvidence>();
          invalidEvidence.add(evidence);
          invalidEvidence.add(null);
          ReactionPathwayAssessment.of(
              ReactionMechanismClass.UNKNOWN,
              ReactionRegioselectivityClass.UNKNOWN,
              window,
              ReactionPredictionConfidenceLevel.LOW,
              invalidEvidence,
              List.of());
        });
  }

  private static Molecule ringMolecule(final String id, final String firstAtomSymbol) {
    return Molecule.of(
        MoleculeId.of(id),
        id,
        List.of(
            ChemistryCorePredictionTest.atom("a0", firstAtomSymbol),
            ChemistryCorePredictionTest.atom("a1", "C"),
            ChemistryCorePredictionTest.atom("a2", "C"),
            ChemistryCorePredictionTest.atom("a3", "N".equals(firstAtomSymbol) ? "N" : "C"),
            ChemistryCorePredictionTest.atom("a4", "C"),
            ChemistryCorePredictionTest.atom("a5", "C")),
        List.of(
            ChemistryCorePredictionTest.bond("a0", "a1"),
            ChemistryCorePredictionTest.bond("a1", "a2"),
            ChemistryCorePredictionTest.bond("a2", "a3"),
            ChemistryCorePredictionTest.bond("a3", "a4"),
            ChemistryCorePredictionTest.bond("a4", "a5"),
            ChemistryCorePredictionTest.bond("a5", "a0")),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Atom atom(final String id, final String symbol) {
    final int index = id.charAt(1) - '0';
    return Atom.of(
        AtomId.of(id),
        ElementSymbol.of(symbol),
        Coordinate3D.of(
            Math.cos(index * Math.PI / 3.0),
            Math.sin(index * Math.PI / 3.0),
            0.0,
            LengthUnit.ANGSTROM));
  }

  private static Bond bond(final String first, final String second) {
    return Bond.of(AtomId.of(first), AtomId.of(second), BondType.AROMATIC);
  }
}

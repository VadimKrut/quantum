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
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionSubstrateReactivityAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionTopologyChangeScorer;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeature;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
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
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.RadicalState;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;

final class ReactionSubstrateReactivityAnalyzerTest {

  @Test
  void activatedHeteroarylAminationEvidenceComesFromStructureAndConditions() {
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "activated_heteroaryl_amination_probe",
            ReactionSide.of(
                List.of(
                    ReactionSubstrateReactivityAnalyzerTest.participant(
                        ReactionSubstrateReactivityAnalyzerTest.chloroCyanopyridine()),
                    ReactionSubstrateReactivityAnalyzerTest.participant(
                        ReactionSubstrateReactivityAnalyzerTest.methylamine()))),
            ReactionSubstrateReactivityAnalyzerTest.basicHeatedConditions(),
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            List.of(),
            null);
    final ReactionProductCandidate candidate =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(
                    ReactionSubstrateReactivityAnalyzerTest.participant(
                        ReactionSubstrateReactivityAnalyzerTest.aminoCyanopyridine()))),
            0.50,
            50.0,
            "graph-derived amination candidate",
            List.of());
    final ArrayList<ReactionPredictionEvidence> evidence =
        new ArrayList<ReactionPredictionEvidence>();

    final double score =
        new ReactionSubstrateReactivityAnalyzer().score(request, candidate, evidence);

    Assertions.assertTrue(score > 0.40);
    Assertions.assertTrue(
        ReactionSubstrateReactivityAnalyzerTest.hasEvidence(
            evidence, "activated heteroaryl C-halogen"));
    Assertions.assertTrue(
        ReactionSubstrateReactivityAnalyzerTest.hasEvidence(
            evidence, "leaving-group identity changes substitution/coupling kinetics"));
    Assertions.assertTrue(
        ReactionSubstrateReactivityAnalyzerTest.hasEvidence(evidence, "N/O/S nucleophile centers"));
    Assertions.assertTrue(
        ReactionSubstrateReactivityAnalyzerTest.hasEvidence(
            evidence, "electron-withdrawing handles"));
    Assertions.assertTrue(
        ReactionSubstrateReactivityAnalyzerTest.hasEvidence(
            evidence, "basic or heated conditions"));
    Assertions.assertTrue(
        ReactionSubstrateReactivityAnalyzerTest.hasEvidence(
            evidence, "forms an additional C-N single bond"));
    Assertions.assertTrue(
        ReactionSubstrateReactivityAnalyzerTest.hasEvidence(
            evidence, "jointly polarize the leaving-group carbon"));
    Assertions.assertTrue(
        ReactionSubstrateReactivityAnalyzerTest.hasEvidence(evidence, "local ring electronics"));
    Assertions.assertTrue(
        ReactionSubstrateReactivityAnalyzerTest.hasEvidence(
            evidence, "Complete heteroaryl substitution triad"));
    Assertions.assertTrue(
        ReactionSubstrateReactivityAnalyzerTest.hasEvidence(
            evidence, "consumes leaving group count consistently"));
    Assertions.assertTrue(
        ReactionSubstrateReactivityAnalyzerTest.hasEvidence(
            evidence, "Product distribution model recognizes a substitution-like channel"));
    Assertions.assertTrue(
        ReactionSubstrateReactivityAnalyzerTest.hasEvidence(
            evidence,
            "supports heteroatom substitution over simple hydrolysis or unchanged starting material"));
    Assertions.assertTrue(
        ReactionSubstrateReactivityAnalyzerTest.hasEvidence(
            evidence, "Other polar handles remain orthogonal"));
  }

  @Test
  void multisiteHeteroarylSubstitutionKeepsCompetingRegioisomerRiskExplicit() {
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "multisite_heteroaryl_substitution_probe",
            ReactionSide.of(
                List.of(
                    ReactionSubstrateReactivityAnalyzerTest.participant(
                        ReactionSubstrateReactivityAnalyzerTest.dichloroCyanopyridine()),
                    ReactionSubstrateReactivityAnalyzerTest.participant(
                        ReactionSubstrateReactivityAnalyzerTest.methylamine()))),
            ReactionSubstrateReactivityAnalyzerTest.basicHeatedConditions(),
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            List.of(),
            null);
    final ReactionProductCandidate candidate =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(
                    ReactionSubstrateReactivityAnalyzerTest.participant(
                        ReactionSubstrateReactivityAnalyzerTest.chloroAminoCyanopyridine()))),
            0.50,
            50.0,
            "single-site amination candidate from a multisite heteroaryl halide",
            List.of());
    final ArrayList<ReactionPredictionEvidence> evidence =
        new ArrayList<ReactionPredictionEvidence>();

    final double score =
        new ReactionSubstrateReactivityAnalyzer().score(request, candidate, evidence);

    Assertions.assertTrue(score > 0.40);
    Assertions.assertTrue(
        ReactionSubstrateReactivityAnalyzerTest.hasEvidence(
            evidence, "Multiple activated heteroaryl leaving-group sites remain"));
    Assertions.assertTrue(
        ReactionSubstrateReactivityAnalyzerTest.hasEvidence(
            evidence, "over-substitution/regioisomer competition"));
    Assertions.assertTrue(
        ReactionSubstrateReactivityAnalyzerTest.hasEvidence(
            evidence, "instead of a single looked-up product"));
  }

  @Test
  void inactiveArylChlorideWithoutNucleophileDoesNotReceiveSnArSupport() {
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "inactive_aryl_chloride_probe",
            ReactionSide.of(
                List.of(
                    ReactionSubstrateReactivityAnalyzerTest.participant(
                        ReactionSubstrateReactivityAnalyzerTest.chlorobenzene()))),
            ReactionConditions.EMPTY,
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            List.of(),
            null);
    final ReactionProductCandidate candidate =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(
                    ReactionSubstrateReactivityAnalyzerTest.participant(
                        ReactionSubstrateReactivityAnalyzerTest.chlorobenzene()))),
            0.50,
            50.0,
            "unchanged aryl chloride",
            List.of());
    final ArrayList<ReactionPredictionEvidence> evidence =
        new ArrayList<ReactionPredictionEvidence>();

    final double score =
        new ReactionSubstrateReactivityAnalyzer().score(request, candidate, evidence);

    Assertions.assertEquals(0.0, score);
    Assertions.assertFalse(
        ReactionSubstrateReactivityAnalyzerTest.hasEvidence(
            evidence, "activated heteroaryl C-halogen"));
  }

  @Test
  void radicalHeterocycleFunctionalizationRequiresScaffoldActivationAndBondChange() {
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "radical_heterocycle_functionalization_probe",
            ReactionSide.of(
                List.of(
                    ReactionSubstrateReactivityAnalyzerTest.participant(
                        ReactionSubstrateReactivityAnalyzerTest.pyridine()),
                    ReactionSubstrateReactivityAnalyzerTest.participant(
                        ReactionSubstrateReactivityAnalyzerTest.methylRadical()))),
            ReactionSubstrateReactivityAnalyzerTest.acidicPhotochemicalConditions(),
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.RADICAL_FUNCTIONALIZATION),
            List.of(),
            null);
    final ReactionProductCandidate candidate =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(
                    ReactionSubstrateReactivityAnalyzerTest.participant(
                        ReactionSubstrateReactivityAnalyzerTest.methylPyridine()))),
            0.40,
            40.0,
            "graph-derived heteroaryl alkylation candidate",
            List.of());
    final ArrayList<ReactionPredictionEvidence> evidence =
        new ArrayList<ReactionPredictionEvidence>();

    final double score =
        new ReactionSubstrateReactivityAnalyzer().score(request, candidate, evidence);

    Assertions.assertTrue(score >= 0.37);
    Assertions.assertTrue(
        ReactionSubstrateReactivityAnalyzerTest.hasEvidence(evidence, "N-heterocycle scaffold"));
    Assertions.assertTrue(
        ReactionSubstrateReactivityAnalyzerTest.hasEvidence(
            evidence, "radical heteroarene functionalization"));
    Assertions.assertTrue(
        ReactionSubstrateReactivityAnalyzerTest.hasEvidence(
            evidence, "additional C-C or C-N sigma bonds"));
  }

  @Test
  void unchangedNitrogenHeterocycleDoesNotReceiveRadicalFunctionalizationSupport() {
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "unchanged_radical_heterocycle_probe",
            ReactionSide.of(
                List.of(
                    ReactionSubstrateReactivityAnalyzerTest.participant(
                        ReactionSubstrateReactivityAnalyzerTest.pyridine()))),
            ReactionSubstrateReactivityAnalyzerTest.acidicPhotochemicalConditions(),
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(),
            null);
    final ReactionProductCandidate candidate =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(
                    ReactionSubstrateReactivityAnalyzerTest.participant(
                        ReactionSubstrateReactivityAnalyzerTest.pyridine()))),
            0.40,
            40.0,
            "unchanged N-heterocycle",
            List.of());
    final ArrayList<ReactionPredictionEvidence> evidence =
        new ArrayList<ReactionPredictionEvidence>();

    final double score =
        new ReactionSubstrateReactivityAnalyzer().score(request, candidate, evidence);

    Assertions.assertEquals(0.0, score);
    Assertions.assertFalse(
        ReactionSubstrateReactivityAnalyzerTest.hasEvidence(evidence, "N-heterocycle scaffold"));
  }

  @Test
  void heatingAloneDoesNotActivateRadicalHeterocycleFunctionalization() {
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "heated_heterocycle_without_radical_activation_probe",
            ReactionSide.of(
                List.of(
                    ReactionSubstrateReactivityAnalyzerTest.participant(
                        ReactionSubstrateReactivityAnalyzerTest.pyridine()))),
            ReactionSubstrateReactivityAnalyzerTest.heatedConditions(),
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.RADICAL_FUNCTIONALIZATION),
            List.of(),
            null);
    final ReactionProductCandidate candidate =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(
                    ReactionSubstrateReactivityAnalyzerTest.participant(
                        ReactionSubstrateReactivityAnalyzerTest.methylPyridine()))),
            0.40,
            40.0,
            "heated heteroaryl alkylation without radical activation",
            List.of());
    final ArrayList<ReactionPredictionEvidence> evidence =
        new ArrayList<ReactionPredictionEvidence>();

    final double score =
        new ReactionSubstrateReactivityAnalyzer().score(request, candidate, evidence);

    Assertions.assertEquals(0.0, score);
    Assertions.assertFalse(
        ReactionSubstrateReactivityAnalyzerTest.hasEvidence(
            evidence, "radical heteroarene functionalization"));
  }

  @Test
  void heteroarylTopologyScoringRequiresRealCarbonHalogenReplacement() {
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "heteroaryl_topology_probe",
            ReactionSide.of(
                List.of(
                    ReactionSubstrateReactivityAnalyzerTest.participant(
                        ReactionSubstrateReactivityAnalyzerTest.chloroCyanopyridine()),
                    ReactionSubstrateReactivityAnalyzerTest.participant(
                        ReactionSubstrateReactivityAnalyzerTest.methylamine()))),
            ReactionSubstrateReactivityAnalyzerTest.basicHeatedConditions(),
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            List.of(),
            null);
    final ReactionProductCandidate coupledCandidate =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(
                    ReactionSubstrateReactivityAnalyzerTest.participant(
                        ReactionSubstrateReactivityAnalyzerTest.aminoCyanopyridine()))),
            0.50,
            50.0,
            "graph-derived heteroaryl amination candidate",
            List.of());
    final ReactionProductCandidate unchangedCandidate =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(
                    ReactionSubstrateReactivityAnalyzerTest.participant(
                        ReactionSubstrateReactivityAnalyzerTest.chloroCyanopyridine()),
                    ReactionSubstrateReactivityAnalyzerTest.participant(
                        ReactionSubstrateReactivityAnalyzerTest.methylamine()))),
            0.50,
            50.0,
            "unchanged heteroaryl halide and amine candidate",
            List.of());
    final ArrayList<ReactionPredictionEvidence> coupledEvidence =
        new ArrayList<ReactionPredictionEvidence>();
    final ArrayList<ReactionPredictionEvidence> unchangedEvidence =
        new ArrayList<ReactionPredictionEvidence>();
    final ReactionTopologyChangeScorer scorer = new ReactionTopologyChangeScorer();

    final double coupledScore = scorer.score(request, coupledCandidate, coupledEvidence);
    final double unchangedScore = scorer.score(request, unchangedCandidate, unchangedEvidence);

    Assertions.assertTrue(coupledScore > unchangedScore);
    Assertions.assertTrue(
        ReactionSubstrateReactivityAnalyzerTest.hasEvidence(
            coupledEvidence, "replaces a carbon-halogen bond with a carbon-heteroatom bond"));
    Assertions.assertTrue(
        ReactionSubstrateReactivityAnalyzerTest.hasEvidence(
            coupledEvidence, "preserves the heteroaromatic scaffold"));
    Assertions.assertFalse(
        ReactionSubstrateReactivityAnalyzerTest.hasEvidence(
            unchangedEvidence, "replaces a carbon-halogen bond with a carbon-heteroatom bond"));
  }

  @Test
  void cycloadditionFmoEvidenceComesFromGenericGraphFeatures() {
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "generic_fmo_cycloaddition_probe",
            ReactionSide.of(
                List.of(
                    ReactionSubstrateReactivityAnalyzerTest.participant(
                        ReactionSubstrateReactivityAnalyzerTest.activatedAlkene()),
                    ReactionSubstrateReactivityAnalyzerTest.participant(
                        ReactionSubstrateReactivityAnalyzerTest.nitrileOxideLikeDipole()))),
            ReactionSubstrateReactivityAnalyzerTest.heatedConditions(),
            List.of(
                OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
                OrganicReactionDomain.CYCLIZATION,
                OrganicReactionDomain.ADDITION),
            List.of(),
            null);
    final ReactionProductCandidate candidate =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(
                    ReactionSubstrateReactivityAnalyzerTest.participant(
                        ReactionSubstrateReactivityAnalyzerTest.isoxazolineLikeRing()))),
            0.45,
            45.0,
            "graph-derived cycloaddition candidate",
            List.of());
    final ArrayList<ReactionPredictionEvidence> evidence =
        new ArrayList<ReactionPredictionEvidence>();

    final double score =
        new ReactionSubstrateReactivityAnalyzer().score(request, candidate, evidence);

    Assertions.assertTrue(score > 0.25);
    Assertions.assertTrue(
        ReactionSubstrateReactivityAnalyzerTest.hasEvidence(
            evidence, "dipole-like heteroatom multiple-bond handles"));
    Assertions.assertTrue(
        ReactionSubstrateReactivityAnalyzerTest.hasEvidence(evidence, "Electron-poor pi bonds"));
    Assertions.assertTrue(
        ReactionSubstrateReactivityAnalyzerTest.hasEvidence(evidence, "cycloaddition/cyclization"));
    Assertions.assertTrue(
        ReactionSubstrateReactivityAnalyzerTest.hasEvidence(evidence, "not by an outcome lookup"));
  }

  private static ReactionConditions basicHeatedConditions() {
    return ReactionConditions.of(
        null,
        null,
        null,
        List.of(),
        List.of(
            ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM),
            ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING)),
        "base and heating");
  }

  private static ReactionConditions acidicPhotochemicalConditions() {
    return ReactionConditions.of(
        null,
        null,
        null,
        List.of(),
        List.of(
            ReactionConditionFeature.named(ReactionConditionFeatureKind.ACIDIC_MEDIUM),
            ReactionConditionFeature.named(ReactionConditionFeatureKind.PHOTOCHEMICAL)),
        "acidic photochemical radical conditions");
  }

  private static ReactionConditions heatedConditions() {
    return ReactionConditions.of(
        null,
        null,
        null,
        List.of(),
        List.of(ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING)),
        "heating without radical activation");
  }

  private static Molecule chloroCyanopyridine() {
    return ReactionSubstrateReactivityAnalyzerTest.molecule(
        "chloro_cyanopyridine",
        "Chloro cyanopyridine",
        List.of(
            ReactionSubstrateReactivityAnalyzerTest.atom("a0", "N"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a1", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a2", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a3", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a4", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a5", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("cl", "Cl"),
            ReactionSubstrateReactivityAnalyzerTest.atom("cn", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("nn", "N")),
        List.of(
            Bond.of(AtomId.of("a0"), AtomId.of("a1"), BondType.AROMATIC),
            Bond.of(AtomId.of("a1"), AtomId.of("a2"), BondType.AROMATIC),
            Bond.of(AtomId.of("a2"), AtomId.of("a3"), BondType.AROMATIC),
            Bond.of(AtomId.of("a3"), AtomId.of("a4"), BondType.AROMATIC),
            Bond.of(AtomId.of("a4"), AtomId.of("a5"), BondType.AROMATIC),
            Bond.of(AtomId.of("a5"), AtomId.of("a0"), BondType.AROMATIC),
            Bond.of(AtomId.of("a2"), AtomId.of("cl"), BondType.SINGLE),
            Bond.of(AtomId.of("a2"), AtomId.of("cn"), BondType.SINGLE),
            Bond.of(AtomId.of("cn"), AtomId.of("nn"), BondType.TRIPLE)));
  }

  private static Molecule aminoCyanopyridine() {
    return ReactionSubstrateReactivityAnalyzerTest.molecule(
        "amino_cyanopyridine",
        "Amino cyanopyridine",
        List.of(
            ReactionSubstrateReactivityAnalyzerTest.atom("a0", "N"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a1", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a2", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a3", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a4", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a5", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("n6", "N"),
            ReactionSubstrateReactivityAnalyzerTest.atom("c7", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("cn", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("nn", "N")),
        List.of(
            Bond.of(AtomId.of("a0"), AtomId.of("a1"), BondType.AROMATIC),
            Bond.of(AtomId.of("a1"), AtomId.of("a2"), BondType.AROMATIC),
            Bond.of(AtomId.of("a2"), AtomId.of("a3"), BondType.AROMATIC),
            Bond.of(AtomId.of("a3"), AtomId.of("a4"), BondType.AROMATIC),
            Bond.of(AtomId.of("a4"), AtomId.of("a5"), BondType.AROMATIC),
            Bond.of(AtomId.of("a5"), AtomId.of("a0"), BondType.AROMATIC),
            Bond.of(AtomId.of("a2"), AtomId.of("n6"), BondType.SINGLE),
            Bond.of(AtomId.of("n6"), AtomId.of("c7"), BondType.SINGLE),
            Bond.of(AtomId.of("a2"), AtomId.of("cn"), BondType.SINGLE),
            Bond.of(AtomId.of("cn"), AtomId.of("nn"), BondType.TRIPLE)));
  }

  private static Molecule dichloroCyanopyridine() {
    return ReactionSubstrateReactivityAnalyzerTest.molecule(
        "dichloro_cyanopyridine",
        "Dichloro cyanopyridine",
        List.of(
            ReactionSubstrateReactivityAnalyzerTest.atom("a0", "N"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a1", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a2", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a3", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a4", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a5", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("cl2", "Cl"),
            ReactionSubstrateReactivityAnalyzerTest.atom("cl4", "Cl"),
            ReactionSubstrateReactivityAnalyzerTest.atom("cn", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("nn", "N")),
        List.of(
            Bond.of(AtomId.of("a0"), AtomId.of("a1"), BondType.AROMATIC),
            Bond.of(AtomId.of("a1"), AtomId.of("a2"), BondType.AROMATIC),
            Bond.of(AtomId.of("a2"), AtomId.of("a3"), BondType.AROMATIC),
            Bond.of(AtomId.of("a3"), AtomId.of("a4"), BondType.AROMATIC),
            Bond.of(AtomId.of("a4"), AtomId.of("a5"), BondType.AROMATIC),
            Bond.of(AtomId.of("a5"), AtomId.of("a0"), BondType.AROMATIC),
            Bond.of(AtomId.of("a2"), AtomId.of("cl2"), BondType.SINGLE),
            Bond.of(AtomId.of("a4"), AtomId.of("cl4"), BondType.SINGLE),
            Bond.of(AtomId.of("a2"), AtomId.of("cn"), BondType.SINGLE),
            Bond.of(AtomId.of("cn"), AtomId.of("nn"), BondType.TRIPLE)));
  }

  private static Molecule chloroAminoCyanopyridine() {
    return ReactionSubstrateReactivityAnalyzerTest.molecule(
        "chloro_amino_cyanopyridine",
        "Chloro amino cyanopyridine",
        List.of(
            ReactionSubstrateReactivityAnalyzerTest.atom("a0", "N"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a1", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a2", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a3", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a4", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a5", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("cl4", "Cl"),
            ReactionSubstrateReactivityAnalyzerTest.atom("n6", "N"),
            ReactionSubstrateReactivityAnalyzerTest.atom("c7", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("cn", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("nn", "N")),
        List.of(
            Bond.of(AtomId.of("a0"), AtomId.of("a1"), BondType.AROMATIC),
            Bond.of(AtomId.of("a1"), AtomId.of("a2"), BondType.AROMATIC),
            Bond.of(AtomId.of("a2"), AtomId.of("a3"), BondType.AROMATIC),
            Bond.of(AtomId.of("a3"), AtomId.of("a4"), BondType.AROMATIC),
            Bond.of(AtomId.of("a4"), AtomId.of("a5"), BondType.AROMATIC),
            Bond.of(AtomId.of("a5"), AtomId.of("a0"), BondType.AROMATIC),
            Bond.of(AtomId.of("a4"), AtomId.of("cl4"), BondType.SINGLE),
            Bond.of(AtomId.of("a2"), AtomId.of("n6"), BondType.SINGLE),
            Bond.of(AtomId.of("n6"), AtomId.of("c7"), BondType.SINGLE),
            Bond.of(AtomId.of("a2"), AtomId.of("cn"), BondType.SINGLE),
            Bond.of(AtomId.of("cn"), AtomId.of("nn"), BondType.TRIPLE)));
  }

  private static Molecule methylamine() {
    return ReactionSubstrateReactivityAnalyzerTest.molecule(
        "methylamine",
        "Methylamine",
        List.of(
            ReactionSubstrateReactivityAnalyzerTest.atom("n0", "N"),
            ReactionSubstrateReactivityAnalyzerTest.atom("c1", "C")),
        List.of(Bond.of(AtomId.of("n0"), AtomId.of("c1"), BondType.SINGLE)));
  }

  private static Molecule pyridine() {
    return ReactionSubstrateReactivityAnalyzerTest.molecule(
        "pyridine",
        "Pyridine",
        List.of(
            ReactionSubstrateReactivityAnalyzerTest.atom("a0", "N"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a1", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a2", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a3", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a4", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a5", "C")),
        List.of(
            Bond.of(AtomId.of("a0"), AtomId.of("a1"), BondType.AROMATIC),
            Bond.of(AtomId.of("a1"), AtomId.of("a2"), BondType.AROMATIC),
            Bond.of(AtomId.of("a2"), AtomId.of("a3"), BondType.AROMATIC),
            Bond.of(AtomId.of("a3"), AtomId.of("a4"), BondType.AROMATIC),
            Bond.of(AtomId.of("a4"), AtomId.of("a5"), BondType.AROMATIC),
            Bond.of(AtomId.of("a5"), AtomId.of("a0"), BondType.AROMATIC)));
  }

  private static Molecule methylPyridine() {
    return ReactionSubstrateReactivityAnalyzerTest.molecule(
        "methyl_pyridine",
        "Methyl pyridine",
        List.of(
            ReactionSubstrateReactivityAnalyzerTest.atom("a0", "N"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a1", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a2", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a3", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a4", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a5", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("m0", "C")),
        List.of(
            Bond.of(AtomId.of("a0"), AtomId.of("a1"), BondType.AROMATIC),
            Bond.of(AtomId.of("a1"), AtomId.of("a2"), BondType.AROMATIC),
            Bond.of(AtomId.of("a2"), AtomId.of("a3"), BondType.AROMATIC),
            Bond.of(AtomId.of("a3"), AtomId.of("a4"), BondType.AROMATIC),
            Bond.of(AtomId.of("a4"), AtomId.of("a5"), BondType.AROMATIC),
            Bond.of(AtomId.of("a5"), AtomId.of("a0"), BondType.AROMATIC),
            Bond.of(AtomId.of("a2"), AtomId.of("m0"), BondType.SINGLE)));
  }

  private static Molecule methylRadical() {
    return ReactionSubstrateReactivityAnalyzerTest.molecule(
        "methyl_radical",
        "Methyl radical",
        List.of(ReactionSubstrateReactivityAnalyzerTest.radicalAtom("c0", "C")),
        List.of());
  }

  private static Molecule chlorobenzene() {
    return ReactionSubstrateReactivityAnalyzerTest.molecule(
        "chlorobenzene",
        "Chlorobenzene",
        List.of(
            ReactionSubstrateReactivityAnalyzerTest.atom("a0", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a1", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a2", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a3", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a4", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("a5", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("cl", "Cl")),
        List.of(
            Bond.of(AtomId.of("a0"), AtomId.of("a1"), BondType.AROMATIC),
            Bond.of(AtomId.of("a1"), AtomId.of("a2"), BondType.AROMATIC),
            Bond.of(AtomId.of("a2"), AtomId.of("a3"), BondType.AROMATIC),
            Bond.of(AtomId.of("a3"), AtomId.of("a4"), BondType.AROMATIC),
            Bond.of(AtomId.of("a4"), AtomId.of("a5"), BondType.AROMATIC),
            Bond.of(AtomId.of("a5"), AtomId.of("a0"), BondType.AROMATIC),
            Bond.of(AtomId.of("a0"), AtomId.of("cl"), BondType.SINGLE)));
  }

  private static Molecule activatedAlkene() {
    return ReactionSubstrateReactivityAnalyzerTest.molecule(
        "activated_alkene",
        "Activated alkene",
        List.of(
            ReactionSubstrateReactivityAnalyzerTest.atom("c0", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("c1", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("c2", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("o3", "O")),
        List.of(
            Bond.of(AtomId.of("c0"), AtomId.of("c1"), BondType.DOUBLE),
            Bond.of(AtomId.of("c1"), AtomId.of("c2"), BondType.SINGLE),
            Bond.of(AtomId.of("c2"), AtomId.of("o3"), BondType.DOUBLE)));
  }

  private static Molecule nitrileOxideLikeDipole() {
    return ReactionSubstrateReactivityAnalyzerTest.molecule(
        "nitrile_oxide_like_dipole",
        "Nitrile oxide like dipole",
        List.of(
            ReactionSubstrateReactivityAnalyzerTest.atom("c0", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("n1", "N"),
            ReactionSubstrateReactivityAnalyzerTest.atom("o2", "O")),
        List.of(
            Bond.of(AtomId.of("c0"), AtomId.of("n1"), BondType.TRIPLE),
            Bond.of(AtomId.of("n1"), AtomId.of("o2"), BondType.SINGLE)));
  }

  private static Molecule isoxazolineLikeRing() {
    return ReactionSubstrateReactivityAnalyzerTest.molecule(
        "isoxazoline_like_ring",
        "Isoxazoline like ring",
        List.of(
            ReactionSubstrateReactivityAnalyzerTest.atom("c0", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("c1", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("c2", "C"),
            ReactionSubstrateReactivityAnalyzerTest.atom("n3", "N"),
            ReactionSubstrateReactivityAnalyzerTest.atom("o4", "O")),
        List.of(
            Bond.of(AtomId.of("c0"), AtomId.of("c1"), BondType.SINGLE),
            Bond.of(AtomId.of("c1"), AtomId.of("c2"), BondType.SINGLE),
            Bond.of(AtomId.of("c2"), AtomId.of("n3"), BondType.SINGLE),
            Bond.of(AtomId.of("n3"), AtomId.of("o4"), BondType.SINGLE),
            Bond.of(AtomId.of("o4"), AtomId.of("c0"), BondType.SINGLE)));
  }

  private static Molecule molecule(
      final String id, final String name, final List<Atom> atoms, final List<Bond> bonds) {
    return Molecule.of(
        MoleculeId.of(id), name, atoms, bonds, MolecularCharge.NEUTRAL, SpinMultiplicity.SINGLET);
  }

  private static Atom atom(final String id, final String symbol) {
    return Atom.of(AtomId.of(id), ElementSymbol.of(symbol), null);
  }

  private static Atom radicalAtom(final String id, final String symbol) {
    return Atom.of(
        AtomId.of(id), ElementSymbol.of(symbol), null, null, null, RadicalState.of(1), null);
  }

  private static ReactionParticipant participant(final Molecule molecule) {
    return ReactionParticipant.of(molecule, StoichiometricCoefficient.ONE);
  }

  private static boolean hasEvidence(
      final List<ReactionPredictionEvidence> evidence, final String text) {
    for (int i = 0; i < evidence.size(); ++i) {
      if (evidence.get(i).description().contains(text)) {
        return true;
      }
    }
    return false;
  }
}

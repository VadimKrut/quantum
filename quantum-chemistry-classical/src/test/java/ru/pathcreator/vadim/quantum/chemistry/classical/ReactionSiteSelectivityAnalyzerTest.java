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
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionSiteSelectivityAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionSiteSelectivityAssessment;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.transform.MolecularTransformProduct;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
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
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;

final class ReactionSiteSelectivityAnalyzerTest {

  @Test
  void activatedHeteroarylSnarUsesContextSensitiveLeavingGroupOrder() {
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "activated_heteroaryl_site_probe",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ReactionSiteSelectivityAnalyzerTest.activatedHalopyridine("F"),
                        StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                null,
                null,
                null,
                List.of(),
                List.of(ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM)),
                "generic SNAr selectivity context"),
            List.of(
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION,
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(),
            null);
    final ReactionSiteSelectivityAssessment fluoroAssessment =
        new ReactionSiteSelectivityAnalyzer()
            .assess(
                request,
                ReactionSiteSelectivityAnalyzerTest.activatedHalopyridine("F"),
                ReactionSiteSelectivityAnalyzerTest.product("F"),
                0.40,
                40.0);
    final ReactionSiteSelectivityAssessment iodoAssessment =
        new ReactionSiteSelectivityAnalyzer()
            .assess(
                request,
                ReactionSiteSelectivityAnalyzerTest.activatedHalopyridine("I"),
                ReactionSiteSelectivityAnalyzerTest.product("I"),
                0.40,
                40.0);

    Assertions.assertTrue(fluoroAssessment.probability() > iodoAssessment.probability());
    Assertions.assertTrue(
        fluoroAssessment.evidence().stream()
            .anyMatch(
                evidence ->
                    evidence.description().contains("context-sensitive leaving-group strength")));
  }

  @Test
  void solventBaseAndNucleophileContextShiftSiteSelectivityWithoutLookup() {
    final ReactionPredictionRequest pegBasicRequest =
        ReactionSiteSelectivityAnalyzerTest.requestWithConditions(
            "PEG4000",
            "K2CO3",
            "benzylamine excess",
            List.of(
                ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM),
                ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING)));
    final ReactionPredictionRequest proticRequest =
        ReactionSiteSelectivityAnalyzerTest.requestWithConditions(
            "water ethanol",
            "acetic acid",
            "benzylamine",
            List.of(ReactionConditionFeature.named(ReactionConditionFeatureKind.ACIDIC_MEDIUM)));
    final ReactionSiteSelectivityAnalyzer analyzer = new ReactionSiteSelectivityAnalyzer();
    final ReactionSiteSelectivityAssessment pegAssessment =
        analyzer.assess(
            pegBasicRequest,
            ReactionSiteSelectivityAnalyzerTest.activatedHalopyridine("Cl"),
            ReactionSiteSelectivityAnalyzerTest.product("Cl"),
            0.40,
            40.0);
    final ReactionSiteSelectivityAssessment proticAssessment =
        analyzer.assess(
            proticRequest,
            ReactionSiteSelectivityAnalyzerTest.activatedHalopyridine("Cl"),
            ReactionSiteSelectivityAnalyzerTest.product("Cl"),
            0.40,
            40.0);

    Assertions.assertTrue(pegAssessment.probability() > proticAssessment.probability());
    Assertions.assertTrue(
        pegAssessment.evidence().stream()
            .anyMatch(evidence -> evidence.description().contains("solvent/base/nucleophile")));
  }

  private static ReactionPredictionRequest requestWithConditions(
      final String solvent,
      final String base,
      final String reactant,
      final List<ReactionConditionFeature> features) {
    return ReactionPredictionRequest.of(
        "condition_selectivity_probe",
        ReactionSide.of(
            List.of(
                ReactionParticipant.of(
                    ReactionSiteSelectivityAnalyzerTest.activatedHalopyridine("Cl"),
                    StoichiometricCoefficient.ONE))),
        ReactionConditions.of(
            null,
            null,
            null,
            List.of(
                ReactionConditionComponent.named(ReactionComponentRole.SOLVENT, solvent),
                ReactionConditionComponent.named(ReactionComponentRole.BASE, base),
                ReactionConditionComponent.named(ReactionComponentRole.REACTANT, reactant)),
            features,
            "site selectivity condition probe"),
        List.of(
            OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION,
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
        List.of(),
        null);
  }

  private static Molecule activatedHalopyridine(final String halogen) {
    return Molecule.of(
        MoleculeId.of("activated_" + halogen.toLowerCase() + "_pyridine"),
        "Activated " + halogen + " pyridine",
        List.of(
            ReactionSiteSelectivityAnalyzerTest.atom("n0", "N"),
            ReactionSiteSelectivityAnalyzerTest.atom("c1", "C"),
            ReactionSiteSelectivityAnalyzerTest.atom("c2", "C"),
            ReactionSiteSelectivityAnalyzerTest.atom("c3", "C"),
            ReactionSiteSelectivityAnalyzerTest.atom("c4", "C"),
            ReactionSiteSelectivityAnalyzerTest.atom("c5", "C"),
            ReactionSiteSelectivityAnalyzerTest.atom("x0", halogen),
            ReactionSiteSelectivityAnalyzerTest.atom("nitrileN", "N")),
        List.of(
            Bond.of(AtomId.of("n0"), AtomId.of("c1"), BondType.AROMATIC),
            Bond.of(AtomId.of("c1"), AtomId.of("c2"), BondType.AROMATIC),
            Bond.of(AtomId.of("c2"), AtomId.of("c3"), BondType.AROMATIC),
            Bond.of(AtomId.of("c3"), AtomId.of("c4"), BondType.AROMATIC),
            Bond.of(AtomId.of("c4"), AtomId.of("c5"), BondType.AROMATIC),
            Bond.of(AtomId.of("c5"), AtomId.of("n0"), BondType.AROMATIC),
            Bond.of(AtomId.of("c1"), AtomId.of("x0"), BondType.SINGLE),
            Bond.of(AtomId.of("c2"), AtomId.of("nitrileN"), BondType.TRIPLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static MolecularTransformProduct product(final String halogen) {
    return MolecularTransformProduct.of(
        ReactionSiteSelectivityAnalyzerTest.activatedHalopyridine(halogen),
        AtomId.of("c1"),
        AtomId.of("x0"));
  }

  private static Atom atom(final String id, final String symbol) {
    return Atom.of(AtomId.of(id), ElementSymbol.of(symbol), null);
  }
}

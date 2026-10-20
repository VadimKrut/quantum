/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.rule;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleMatch;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.StoichiometricCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.FormalCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.RadicalState;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;

public final class RadicalRecombinationOrganicReactionRuleTest {

  @Test
  public void recombinesTwoAtomLevelRadicalsIntoClosedShellDiatomicProduct() {
    final ReactionPredictionRequest request =
        RadicalRecombinationOrganicReactionRuleTest.request(
            "atom_level_radical_recombination",
            ReactionParticipant.of(
                RadicalRecombinationOrganicReactionRuleTest.radicalAtomMolecule("h_left", "H", 0.0),
                StoichiometricCoefficient.ONE),
            ReactionParticipant.of(
                RadicalRecombinationOrganicReactionRuleTest.radicalAtomMolecule(
                    "h_right", "H", 0.74),
                StoichiometricCoefficient.ONE));

    final RadicalRecombinationOrganicReactionRule rule =
        RadicalRecombinationOrganicReactionRule.diatomicSingleBond();
    final OrganicReactionRuleMatch match = rule.match(request);
    final List<ReactionProductCandidate> candidates = rule.generate(request, match);

    Assertions.assertTrue(match.matched());
    Assertions.assertEquals(1, candidates.size());
    RadicalRecombinationOrganicReactionRuleTest.assertClosedShellDiatomic(candidates.get(0));
  }

  @Test
  public void recombinesSpinDerivedSingleAtomRadicalCoefficientIntoClosedShellDiatomicProduct() {
    final ReactionPredictionRequest request =
        RadicalRecombinationOrganicReactionRuleTest.request(
            "spin_radical_recombination",
            ReactionParticipant.of(
                RadicalRecombinationOrganicReactionRuleTest.spinRadicalAtomMolecule("h_spin", "H"),
                StoichiometricCoefficient.of(2)));

    final RadicalRecombinationOrganicReactionRule rule =
        RadicalRecombinationOrganicReactionRule.diatomicSingleBond();
    final OrganicReactionRuleMatch match = rule.match(request);
    final List<ReactionProductCandidate> candidates = rule.generate(request, match);

    Assertions.assertTrue(match.matched());
    Assertions.assertEquals(1, candidates.size());
    RadicalRecombinationOrganicReactionRuleTest.assertClosedShellDiatomic(candidates.get(0));
  }

  private static void assertClosedShellDiatomic(final ReactionProductCandidate candidate) {
    final Molecule product = candidate.products().participants().get(0).molecule();
    Assertions.assertEquals(2, product.atomCount());
    Assertions.assertEquals(1, product.bondCount());
    Assertions.assertEquals(BondType.SINGLE, product.bonds().get(0).type());
    Assertions.assertEquals(SpinMultiplicity.SINGLET, product.spinMultiplicity());
    Assertions.assertEquals(MolecularCharge.NEUTRAL, product.charge());
    Assertions.assertFalse(product.atoms().get(0).radicalState().radical());
    Assertions.assertFalse(product.atoms().get(1).radicalState().radical());
    Assertions.assertTrue(candidate.hasPredictedMechanisms());
  }

  private static ReactionPredictionRequest request(
      final String id, final ReactionParticipant... participants) {
    return ReactionPredictionRequest.of(
        id,
        ReactionSide.of(List.of(participants)),
        ReactionConditions.EMPTY,
        List.of(OrganicReactionDomain.UNKNOWN),
        List.of(),
        "radical recombination test");
  }

  private static Molecule radicalAtomMolecule(
      final String id, final String symbol, final double z) {
    return Molecule.of(
        MoleculeId.of(id),
        id,
        List.of(
            Atom.of(
                AtomId.of("a0"),
                ElementSymbol.of(symbol),
                Coordinate3D.of(0.0, 0.0, z, LengthUnit.ANGSTROM),
                FormalCharge.NEUTRAL,
                null,
                RadicalState.of(1),
                ChemistryMetadata.EMPTY)),
        List.of(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.of(2));
  }

  private static Molecule spinRadicalAtomMolecule(final String id, final String symbol) {
    return Molecule.of(
        MoleculeId.of(id),
        id,
        List.of(
            Atom.of(
                AtomId.of("a0"),
                ElementSymbol.of(symbol),
                Coordinate3D.of(0.0, 0.0, 0.0, LengthUnit.ANGSTROM))),
        List.of(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.of(2));
  }
}

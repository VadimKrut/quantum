/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.rule;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRule;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleCategory;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleMatch;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.PredictedReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.StoichiometricCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.FormalCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.RadicalState;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;

/**
 * Строит product candidate для рекомбинации двух одноатомных радикалов в ковалентную двухатомную молекулу.
 */
public final class RadicalRecombinationOrganicReactionRule implements OrganicReactionRule {

  private static final OrganicReactionRuleId ID =
      OrganicReactionRuleId.of("radical_recombination_diatomic_single_bond");
  private static final double BASE_PROBABILITY = 0.72;
  private static final double EXPECTED_YIELD_PERCENT = 72.0;

  public static RadicalRecombinationOrganicReactionRule diatomicSingleBond() {
    return new RadicalRecombinationOrganicReactionRule();
  }

  private RadicalRecombinationOrganicReactionRule() {}

  public OrganicReactionRuleId id() {
    return ID;
  }

  public String displayName() {
    return "Radical recombination to diatomic single-bond product";
  }

  public OrganicReactionRuleCategory category() {
    return OrganicReactionRuleCategory.RADICAL_FUNCTIONALIZATION;
  }

  public ReactionMechanismFamily mechanismFamily() {
    return ReactionMechanismFamily.RADICAL_CHAIN;
  }

  public OrganicReactionRuleMatch match(final ReactionPredictionRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction prediction request must not be null.");
    }
    final List<Atom> radicalAtoms = RadicalRecombinationOrganicReactionRule.radicalAtoms(request);
    if (radicalAtoms.size() != 2) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    if (!RadicalRecombinationOrganicReactionRule.compatibleRadicals(
        radicalAtoms.get(0), radicalAtoms.get(1))) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    return OrganicReactionRuleMatch.matched(
        BASE_PROBABILITY,
        "Two compatible one-atom radicals can terminate by forming a covalent single bond.");
  }

  public List<ReactionProductCandidate> generate(
      final ReactionPredictionRequest request, final OrganicReactionRuleMatch match) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction prediction request must not be null.");
    }
    if (match == null || !match.matched()) {
      return List.of();
    }
    final List<Atom> radicalAtoms = RadicalRecombinationOrganicReactionRule.radicalAtoms(request);
    if (radicalAtoms.size() != 2
        || !RadicalRecombinationOrganicReactionRule.compatibleRadicals(
            radicalAtoms.get(0), radicalAtoms.get(1))) {
      return List.of();
    }
    final Molecule product =
        RadicalRecombinationOrganicReactionRule.product(
            radicalAtoms.get(0), radicalAtoms.get(1), request.id());
    return List.of(
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(ReactionParticipant.of(product, StoichiometricCoefficient.ONE))),
            BASE_PROBABILITY,
            EXPECTED_YIELD_PERCENT,
            "Radical termination product from two compatible one-atom radicals.",
            RadicalRecombinationOrganicReactionRule.evidence(match),
            List.of(
                PredictedReactionMechanism.of(
                    ID.value(),
                    ReactionMechanismFamily.RADICAL_CHAIN,
                    BASE_PROBABILITY,
                    EXPECTED_YIELD_PERCENT))));
  }

  private static List<Atom> radicalAtoms(final ReactionPredictionRequest request) {
    final ArrayList<Atom> atoms = new ArrayList<Atom>(2);
    final List<ReactionParticipant> participants = request.reactants().participants();
    for (int i = 0; i < participants.size(); ++i) {
      final ReactionParticipant participant = participants.get(i);
      final Molecule molecule = participant.molecule();
      if (molecule.atomCount() != 1 || molecule.bondCount() != 0) {
        return List.of();
      }
      final Atom atom = RadicalRecombinationOrganicReactionRule.effectiveRadicalAtom(molecule);
      if (!atom.radicalState().radical()) {
        return List.of();
      }
      final int copies = participant.coefficient().value();
      if (copies < 1 || copies > 2 || atoms.size() + copies > 2) {
        return List.of();
      }
      for (int copy = 0; copy < copies; ++copy) {
        atoms.add(atom);
      }
    }
    return List.copyOf(atoms);
  }

  private static boolean compatibleRadicals(final Atom first, final Atom second) {
    return first.formalCharge().value() + second.formalCharge().value() == 0
        && first.radicalState().unpairedElectrons() == 1
        && second.radicalState().unpairedElectrons() == 1;
  }

  private static Atom effectiveRadicalAtom(final Molecule molecule) {
    final Atom atom = molecule.atoms().get(0);
    if (atom.radicalState().radical()) {
      return atom;
    }
    if (molecule.spinMultiplicity().value() <= 1) {
      return atom;
    }
    return Atom.of(
        atom.id(),
        atom.element().symbol(),
        atom.coordinate(),
        atom.formalCharge(),
        atom.isotope(),
        RadicalState.of(1),
        atom.metadata());
  }

  private static Molecule product(final Atom first, final Atom second, final String requestId) {
    final Atom firstProductAtom =
        RadicalRecombinationOrganicReactionRule.productAtom(
            first, "r0", RadicalRecombinationOrganicReactionRule.firstCoordinate(first));
    final Atom secondProductAtom =
        RadicalRecombinationOrganicReactionRule.productAtom(
            second,
            "r1",
            RadicalRecombinationOrganicReactionRule.secondCoordinate(
                first.coordinate(), second.coordinate()));
    return Molecule.of(
        MoleculeId.of(requestId + "_radical_recombination_product"),
        first.element().symbol().value()
            + second.element().symbol().value()
            + " radical recombination product",
        List.of(firstProductAtom, secondProductAtom),
        List.of(Bond.of(firstProductAtom.id(), secondProductAtom.id(), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET,
        ChemistryMetadata.EMPTY);
  }

  private static Atom productAtom(
      final Atom source, final String id, final Coordinate3D coordinate) {
    return Atom.of(
        AtomId.of(id),
        source.element().symbol(),
        coordinate,
        FormalCharge.NEUTRAL,
        source.isotope(),
        RadicalState.CLOSED_SHELL,
        source.metadata());
  }

  private static Coordinate3D secondCoordinate(
      final Coordinate3D first, final Coordinate3D second) {
    if (first == null) {
      return second == null ? Coordinate3D.of(0.0, 0.0, 0.74, LengthUnit.ANGSTROM) : second;
    }
    if (second != null
        && (Double.compare(first.x(), second.x()) != 0
            || Double.compare(first.y(), second.y()) != 0
            || Double.compare(first.z(), second.z()) != 0)) {
      return second;
    }
    return Coordinate3D.of(first.x(), first.y(), first.z() + 0.74, first.unit());
  }

  private static Coordinate3D firstCoordinate(final Atom atom) {
    if (atom.hasCoordinate()) {
      return atom.coordinate();
    }
    return Coordinate3D.of(0.0, 0.0, 0.0, LengthUnit.ANGSTROM);
  }

  private static List<ReactionPredictionEvidence> evidence(final OrganicReactionRuleMatch match) {
    return List.of(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.USER_RULE,
            "Two one-electron radicals were converted into a closed-shell single-bond product.",
            match.confidence()),
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.KINETIC_FACTOR,
            "Mechanism trace: radical encounter -> spin-paired bond formation -> closed-shell diatomic product.",
            0.08));
  }
}

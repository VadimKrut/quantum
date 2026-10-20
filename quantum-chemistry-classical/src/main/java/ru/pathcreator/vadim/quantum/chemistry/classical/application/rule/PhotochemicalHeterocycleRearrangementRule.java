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
import ru.pathcreator.vadim.quantum.chemistry.classical.application.feature.MolecularFeatureAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureSet;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRule;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleCategory;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleMatch;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.PredictedReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;

/** Правило фотохимической перестройки гетероциклов с сохранением формулы и изменением bond-order канала. */
public final class PhotochemicalHeterocycleRearrangementRule implements OrganicReactionRule {

  private static final double BASE_PROBABILITY = 0.30;
  private static final double EXPECTED_YIELD_PERCENT = 35.0;

  private final OrganicReactionRuleId id;
  private final String displayName;
  private final MolecularFeatureAnalyzer featureAnalyzer;
  private final OrganicReactionConditionMatcher conditionMatcher;

  private PhotochemicalHeterocycleRearrangementRule(
      final OrganicReactionRuleId id, final String displayName) {
    this.id = id;
    this.displayName = displayName;
    this.featureAnalyzer = new MolecularFeatureAnalyzer();
    this.conditionMatcher = new OrganicReactionConditionMatcher();
  }

  public static PhotochemicalHeterocycleRearrangementRule heterocycleBondOrderIsomerization() {
    return new PhotochemicalHeterocycleRearrangementRule(
        OrganicReactionRuleId.of("photochemical_heterocycle_bond_order_isomerization"),
        "Photochemical heterocycle bond-order isomerization");
  }

  public OrganicReactionRuleId id() {
    return this.id;
  }

  public String displayName() {
    return this.displayName;
  }

  public OrganicReactionRuleCategory category() {
    return OrganicReactionRuleCategory.REARRANGEMENT;
  }

  public ReactionMechanismFamily mechanismFamily() {
    return ReactionMechanismFamily.PHOTOCHEMICAL;
  }

  public OrganicReactionRuleMatch match(final ReactionPredictionRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction prediction request must not be null.");
    }
    if (!this.domainMatches(request)
        || !request.conditions().hasFeature(ReactionConditionFeatureKind.PHOTOCHEMICAL)) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    final Molecule substrate = this.matchingSubstrate(request);
    if (substrate == null || this.rearrangeableBondIndex(substrate) < 0) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    final OrganicReactionConditionAssessment conditionAssessment =
        this.conditionMatcher.assess(
            request.conditions(),
            List.of(
                ReactionConditionFeatureKind.PHOTOCHEMICAL,
                ReactionConditionFeatureKind.INERT_ATMOSPHERE,
                ReactionConditionFeatureKind.ADDITION_RATE));
    if (conditionAssessment.stronglyIncompatible()) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    return OrganicReactionRuleMatch.matched(
        Math.min(0.90, BASE_PROBABILITY + conditionAssessment.scoreAdjustment()),
        "Photochemical heterocycle context supports excited-state bond-order rearrangement channels.");
  }

  public List<ReactionProductCandidate> generate(
      final ReactionPredictionRequest request, final OrganicReactionRuleMatch match) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction prediction request must not be null.");
    }
    if (match == null || !match.matched()) {
      return List.of();
    }
    final Molecule substrate = this.matchingSubstrate(request);
    if (substrate == null) {
      return List.of();
    }
    final int bondIndex = this.rearrangeableBondIndex(substrate);
    if (bondIndex < 0) {
      return List.of();
    }
    final OrganicReactionConditionAssessment conditionAssessment =
        this.conditionMatcher.assess(
            request.conditions(),
            List.of(
                ReactionConditionFeatureKind.PHOTOCHEMICAL,
                ReactionConditionFeatureKind.INERT_ATMOSPHERE,
                ReactionConditionFeatureKind.ADDITION_RATE));
    final Molecule product = this.rearrangedProduct(substrate, bondIndex);
    final double probability =
        Math.min(0.90, Math.max(0.03, BASE_PROBABILITY + conditionAssessment.scoreAdjustment()));
    final ArrayList<ReactionPredictionEvidence> evidence =
        new ArrayList<ReactionPredictionEvidence>(5 + conditionAssessment.evidence().size());
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN,
            "Detected photoreactive heterocycle substrate for competing isomer/rearrangement channels.",
            0.20));
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
            "Generated same-formula photochemical bond-order isomer from molecular graph rather than a fixed product seed.",
            0.18));
    evidence.addAll(conditionAssessment.evidence());
    return List.of(
        ReactionProductCandidate.of(
            ReactionProductSideFactory.fromSingleProduct(product),
            probability,
            EXPECTED_YIELD_PERCENT,
            "Photochemical heterocycle rearrangement candidate; distribution should be calibrated by observed product channels.",
            List.copyOf(evidence),
            List.of(
                PredictedReactionMechanism.of(
                    request.id() + "_photochemical_heterocycle_isomerization",
                    ReactionMechanismFamily.PHOTOCHEMICAL,
                    probability,
                    EXPECTED_YIELD_PERCENT))));
  }

  private boolean domainMatches(final ReactionPredictionRequest request) {
    return request.hasDomain(OrganicReactionDomain.UNKNOWN)
        || request.hasDomain(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION)
        || request.hasDomain(OrganicReactionDomain.REARRANGEMENT)
        || request.hasDomain(OrganicReactionDomain.RADICAL_FUNCTIONALIZATION);
  }

  private Molecule matchingSubstrate(final ReactionPredictionRequest request) {
    final List<ReactionParticipant> participants = request.reactants().participants();
    for (int i = 0; i < participants.size(); ++i) {
      final Molecule molecule = participants.get(i).molecule();
      final MolecularFeatureSet features = this.featureAnalyzer.analyze(molecule);
      if (!features.contains(MolecularFeatureKind.HETEROCYCLE)) {
        continue;
      }
      if (features.contains(MolecularFeatureKind.SULFUR_HETEROCYCLE)
          || features.contains(MolecularFeatureKind.NITROGEN_HETEROCYCLE)
          || features.contains(MolecularFeatureKind.OXYGEN_HETEROCYCLE)) {
        return molecule;
      }
    }
    return null;
  }

  private int rearrangeableBondIndex(final Molecule molecule) {
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      final Atom first =
          PhotochemicalHeterocycleRearrangementRule.atom(molecule, bond.firstAtomId().value());
      final Atom second =
          PhotochemicalHeterocycleRearrangementRule.atom(molecule, bond.secondAtomId().value());
      if (first == null || second == null) {
        continue;
      }
      if (PhotochemicalHeterocycleRearrangementRule.isHeteroatom(first)
          || PhotochemicalHeterocycleRearrangementRule.isHeteroatom(second)) {
        return i;
      }
    }
    return -1;
  }

  private Molecule rearrangedProduct(final Molecule substrate, final int changedBondIndex) {
    final List<Bond> sourceBonds = substrate.bonds();
    final ArrayList<Bond> bonds = new ArrayList<Bond>(sourceBonds.size());
    for (int i = 0; i < sourceBonds.size(); ++i) {
      final Bond bond = sourceBonds.get(i);
      if (i == changedBondIndex) {
        bonds.add(
            Bond.of(
                bond.firstAtomId(),
                bond.secondAtomId(),
                PhotochemicalHeterocycleRearrangementRule.rearrangedType(bond.type())));
        continue;
      }
      bonds.add(bond);
    }
    return Molecule.of(
        MoleculeId.of(substrate.id().value() + "_photochemical_rearranged"),
        substrate.displayName() + " photochemical rearranged isomer",
        substrate.atoms(),
        bonds,
        substrate.charge(),
        substrate.spinMultiplicity(),
        substrate.stereochemistry(),
        substrate.conformation(),
        substrate.opticalRotation(),
        substrate.symmetry(),
        substrate.metadata());
  }

  private static BondType rearrangedType(final BondType type) {
    if (type == BondType.AROMATIC) {
      return BondType.SINGLE;
    }
    if (type == BondType.SINGLE) {
      return BondType.DOUBLE;
    }
    if (type == BondType.DOUBLE) {
      return BondType.SINGLE;
    }
    return BondType.SINGLE;
  }

  private static Atom atom(final Molecule molecule, final String atomId) {
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      if (atom.id().value().equals(atomId)) {
        return atom;
      }
    }
    return null;
  }

  private static boolean isHeteroatom(final Atom atom) {
    final String symbol = atom.element().symbol().value();
    return "N".equals(symbol) || "O".equals(symbol) || "S".equals(symbol);
  }
}

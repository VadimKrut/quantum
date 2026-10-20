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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import ru.pathcreator.vadim.quantum.chemistry.domain.conformation.MolecularConformation;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereochemistry;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.OpticalRotation;
import ru.pathcreator.vadim.quantum.chemistry.domain.symmetry.MolecularSymmetry;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.FormalCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.RadicalState;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRule;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleCategory;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleMatch;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;

/** Правило графового восстановления nitro-группы в amine-подобный продукт без табличного lookup продукта. */
public final class NitroReductionOrganicReactionRule implements OrganicReactionRule {

  private static final double BASE_PROBABILITY = 0.42;
  private static final double EXPECTED_YIELD_PERCENT = 60.0;

  private final OrganicReactionConditionMatcher conditionMatcher;

  private NitroReductionOrganicReactionRule() {
    this.conditionMatcher = new OrganicReactionConditionMatcher();
  }

  public static NitroReductionOrganicReactionRule graphNitroToAmine() {
    return new NitroReductionOrganicReactionRule();
  }

  public OrganicReactionRuleId id() {
    return OrganicReactionRuleId.of("graph_nitro_reduction_to_amine");
  }

  public String displayName() {
    return "Graph nitro reduction to amine";
  }

  public OrganicReactionRuleCategory category() {
    return OrganicReactionRuleCategory.REDUCTION;
  }

  public OrganicReactionRuleMatch match(final ReactionPredictionRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction prediction request must not be null.");
    }
    if (!this.domainMatches(request)) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    final Molecule substrate = this.matchingSubstrate(request);
    if (substrate == null || NitroReductionOrganicReactionRule.nitroSite(substrate) == null) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    final OrganicReactionConditionAssessment conditionAssessment =
        this.conditionMatcher.assess(
            request.conditions(),
            List.of(
                ReactionConditionFeatureKind.REDUCING_MEDIUM,
                ReactionConditionFeatureKind.HYDROGENATION,
                ReactionConditionFeatureKind.CATALYST_PRESENT));
    if (conditionAssessment.stronglyIncompatible()) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    return OrganicReactionRuleMatch.matched(
        Math.min(0.90, BASE_PROBABILITY + conditionAssessment.scoreAdjustment()),
        "Nitro functionality under reducing conditions supports graph-level nitro-to-amine conversion.");
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
    final NitroSite site = NitroReductionOrganicReactionRule.nitroSite(substrate);
    if (site == null) {
      return List.of();
    }
    final OrganicReactionConditionAssessment conditionAssessment =
        this.conditionMatcher.assess(
            request.conditions(),
            List.of(
                ReactionConditionFeatureKind.REDUCING_MEDIUM,
                ReactionConditionFeatureKind.HYDROGENATION,
                ReactionConditionFeatureKind.CATALYST_PRESENT));
    final double probability =
        Math.min(0.90, Math.max(0.03, BASE_PROBABILITY + conditionAssessment.scoreAdjustment()));
    final Molecule product = NitroReductionOrganicReactionRule.reducedProduct(substrate, site);
    final ArrayList<ReactionPredictionEvidence> evidence =
        new ArrayList<ReactionPredictionEvidence>(4 + conditionAssessment.evidence().size());
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.USER_RULE,
            "Graph nitro reduction rule matched: " + this.id().value(),
            match.confidence()));
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
            "Detected a C-bound nitro group with terminal oxygens and generated an amine graph hypothesis.",
            0.20));
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
            "Product removes nitro oxygens and adds explicit N-H bonds instead of using a surrogate product channel.",
            0.15));
    evidence.addAll(conditionAssessment.evidence());
    return List.of(
        ReactionProductCandidate.of(
            ReactionProductSideFactory.fromSingleProduct(product),
            probability,
            EXPECTED_YIELD_PERCENT,
            "Main-product graph hypothesis only: reducing reagent, coproduct balance, selectivity and geometry require validation; no complete balanced reaction is inferred.",
            List.copyOf(evidence)));
  }

  private boolean domainMatches(final ReactionPredictionRequest request) {
    return request.hasDomain(OrganicReactionDomain.UNKNOWN)
        || request.hasDomain(OrganicReactionDomain.OXIDATION_REDUCTION)
        || request.hasDomain(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION);
  }

  private Molecule matchingSubstrate(final ReactionPredictionRequest request) {
    final List<ReactionParticipant> participants = request.reactants().participants();
    for (int i = 0; i < participants.size(); ++i) {
      final Molecule molecule = participants.get(i).molecule();
      if (nitroSite(molecule) != null) {
        return molecule;
      }
    }
    return null;
  }

  private static Molecule reducedProduct(final Molecule substrate, final NitroSite site) {
    final ArrayList<Atom> atoms = new ArrayList<Atom>(substrate.atoms().size());
    final Set<AtomId> occupied = new HashSet<>();
    substrate.atoms().forEach(atom -> occupied.add(atom.id()));
    final AtomId firstHydrogenId = uniqueId(site.nitrogenId().value() + "_reduced_h1", occupied);
    final AtomId secondHydrogenId = uniqueId(site.nitrogenId().value() + "_reduced_h2", occupied);
    final List<Atom> sourceAtoms = substrate.atoms();
    for (int i = 0; i < sourceAtoms.size(); ++i) {
      final Atom atom = sourceAtoms.get(i);
      if (!atom.id().equals(site.firstOxygenId()) && !atom.id().equals(site.secondOxygenId())) {
        atoms.add(
            atom.id().equals(site.nitrogenId())
                ? Atom.of(
                    atom.id(),
                    atom.element().symbol(),
                    atom.coordinate(),
                    FormalCharge.NEUTRAL,
                    atom.isotope(),
                    atom.radicalState(),
                    atom.metadata())
                : atom);
      }
    }
    atoms.add(Atom.of(firstHydrogenId, ElementSymbol.of("H"), null));
    atoms.add(Atom.of(secondHydrogenId, ElementSymbol.of("H"), null));
    final ArrayList<Bond> bonds = new ArrayList<Bond>(substrate.bonds().size());
    final List<Bond> sourceBonds = substrate.bonds();
    for (int i = 0; i < sourceBonds.size(); ++i) {
      final Bond bond = sourceBonds.get(i);
      if (!bond.firstAtomId().equals(site.firstOxygenId())
          && !bond.secondAtomId().equals(site.firstOxygenId())
          && !bond.firstAtomId().equals(site.secondOxygenId())
          && !bond.secondAtomId().equals(site.secondOxygenId())) {
        bonds.add(bond);
      }
    }
    bonds.add(Bond.of(site.nitrogenId(), firstHydrogenId, BondType.SINGLE));
    bonds.add(Bond.of(site.nitrogenId(), secondHydrogenId, BondType.SINGLE));
    final var attributes = new LinkedHashMap<>(substrate.metadata().attributes());
    if (!substrate.stereochemistry().empty()) {
      attributes.put(
          Stereochemistry.REVIEW_ATTRIBUTE,
          "Graph transformation: verify stereochemical priorities.");
    }
    return Molecule.of(
        MoleculeId.of(substrate.id().value() + "_nitro_reduced_amine"),
        substrate.displayName() + " nitro reduced amine",
        atoms,
        bonds,
        substrate.charge(),
        substrate.spinMultiplicity(),
        Stereochemistry.of(
            substrate.stereochemistry().centers().stream()
                .filter(
                    center ->
                        !center.references(site.firstOxygenId())
                            && !center.references(site.secondOxygenId())
                            && !center.primaryAtomId().equals(site.nitrogenId()))
                .toList()),
        MolecularConformation.of(
            substrate.conformation().torsionAngles().stream()
                .filter(
                    angle ->
                        !angle.references(site.firstOxygenId())
                            && !angle.references(site.secondOxygenId())
                            && !angle.references(site.nitrogenId()))
                .toList()),
        OpticalRotation.UNKNOWN,
        MolecularSymmetry.C1,
        ChemistryMetadata.of(
            substrate.metadata().source(), substrate.metadata().location(), attributes));
  }

  private static AtomId uniqueId(final String base, final Set<AtomId> occupied) {
    AtomId result = AtomId.of(base);
    for (int suffix = 2; !occupied.add(result); suffix++) {
      result = AtomId.of(base + "_" + suffix);
    }
    return result;
  }

  private static NitroSite nitroSite(final Molecule molecule) {
    // This amine rule supports C-nitro groups, not nitrates, nitramines or free NO2.
    for (final Atom nitrogen : molecule.atoms()) {
      if (!"N".equals(nitrogen.element().symbol().value())
          || nitrogen.radicalState() != RadicalState.CLOSED_SHELL
          || molecule.graph().degreeOf(nitrogen.id()) != 3) {
        continue;
      }
      final List<AtomId> oxygens = new ArrayList<>(2);
      int carbons = 0;
      int doubleOxygens = 0;
      int negativeSingleOxygens = 0;
      for (final Bond bond : molecule.bonds()) {
        final AtomId neighborId;
        if (bond.firstAtomId().equals(nitrogen.id())) {
          neighborId = bond.secondAtomId();
        } else if (bond.secondAtomId().equals(nitrogen.id())) {
          neighborId = bond.firstAtomId();
        } else {
          continue;
        }
        final Atom neighbor = atom(molecule, neighborId);
        if ("C".equals(neighbor.element().symbol().value()) && bond.type() == BondType.SINGLE) {
          carbons++;
        }
        if (!"O".equals(neighbor.element().symbol().value())
            || molecule.graph().degreeOf(neighborId) != 1
            || neighbor.radicalState() != RadicalState.CLOSED_SHELL) {
          continue;
        }
        if (bond.type() == BondType.DOUBLE && neighbor.formalCharge().value() == 0) {
          doubleOxygens++;
          oxygens.add(neighborId);
        } else if (bond.type() == BondType.SINGLE && neighbor.formalCharge().value() == -1) {
          negativeSingleOxygens++;
          oxygens.add(neighborId);
        }
      }
      final boolean chargeSeparated =
          nitrogen.formalCharge().value() == 1 && doubleOxygens == 1 && negativeSingleOxygens == 1;
      final boolean legacyDoubleBonds = nitrogen.formalCharge().value() == 0 && doubleOxygens == 2;
      if (carbons == 1 && oxygens.size() == 2 && (chargeSeparated || legacyDoubleBonds)) {
        return new NitroSite(nitrogen.id(), oxygens.get(0), oxygens.get(1));
      }
    }
    return null;
  }

  private static Atom atom(final Molecule molecule, final AtomId atomId) {
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      if (atom.id().equals(atomId)) {
        return atom;
      }
    }
    throw new IllegalArgumentException("Nitro reduction molecule bond references unknown atom.");
  }

  private static final class NitroSite {

    private final AtomId nitrogenId;
    private final AtomId firstOxygenId;
    private final AtomId secondOxygenId;

    private NitroSite(
        final AtomId nitrogenId, final AtomId firstOxygenId, final AtomId secondOxygenId) {
      this.nitrogenId = nitrogenId;
      this.firstOxygenId = firstOxygenId;
      this.secondOxygenId = secondOxygenId;
    }

    private AtomId nitrogenId() {
      return this.nitrogenId;
    }

    private AtomId firstOxygenId() {
      return this.firstOxygenId;
    }

    private AtomId secondOxygenId() {
      return this.secondOxygenId;
    }
  }
}

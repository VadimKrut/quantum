/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import ru.pathcreator.vadim.quantum.chemistry.domain.graph.MolecularRing;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** Оценивает, насколько кандидат объясняется реальным изменением молекулярного графа. */
public final class ReactionTopologyChangeScorer {

  private static final double GRAPH_CHANGE_SUPPORT = 0.060;
  private static final double LEAVING_GROUP_SUPPORT = 0.055;
  private static final double UNSATURATION_SUPPORT = 0.050;
  private static final double SATURATION_SUPPORT = 0.045;
  private static final double HETEROARYL_COUPLING_TOPOLOGY_SUPPORT = 0.120;
  private static final double HETEROARYL_SCAFFOLD_RETENTION_SUPPORT = 0.070;
  private static final double GRAPHLESS_TRANSFORM_PENALTY = -0.040;

  public double score(
      final ReactionPredictionRequest request,
      final ReactionProductCandidate candidate,
      final List<ReactionPredictionEvidence> evidence) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction topology request must not be null.");
    }
    if (candidate == null) {
      throw new IllegalArgumentException("Reaction topology candidate must not be null.");
    }
    if (evidence == null) {
      throw new IllegalArgumentException("Reaction topology evidence list must not be null.");
    }
    final GraphDelta delta =
        ReactionTopologyChangeScorer.delta(request.reactants(), candidate.products());
    double score = 0.0;
    if (delta.changed()) {
      score +=
          ReactionTopologyChangeScorer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Candidate has explicit molecular graph changes: broken="
                  + delta.brokenBondCount
                  + ", formed="
                  + delta.formedBondCount
                  + ", orderIncreases="
                  + delta.bondOrderIncreaseCount
                  + ", orderDecreases="
                  + delta.bondOrderDecreaseCount
                  + ".",
              GRAPH_CHANGE_SUPPORT);
    } else if (ReactionTopologyChangeScorer.claimsTransform(candidate)) {
      score +=
          ReactionTopologyChangeScorer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.WARNING,
              "Candidate claims a transform but product graph does not show a corresponding topology change.",
              GRAPHLESS_TRANSFORM_PENALTY);
    }
    if (delta.leavingGroupLossCount > 0
        && ReactionTopologyChangeScorer.leavingGroupContext(request)) {
      score +=
          ReactionTopologyChangeScorer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
              "Candidate removes explicit halogen leaving-group atoms in a compatible reaction domain.",
              LEAVING_GROUP_SUPPORT * Math.min(2, delta.leavingGroupLossCount));
    }
    if (delta.carbonHalogenBondLossCount > 0
        && delta.formedCarbonHeteroatomBondCount > 0
        && ReactionTopologyChangeScorer.heteroarylSubstitutionContext(request)) {
      score +=
          ReactionTopologyChangeScorer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN,
              "Candidate replaces a carbon-halogen bond with a carbon-heteroatom bond under heteroaryl substitution or functionalization context.",
              HETEROARYL_COUPLING_TOPOLOGY_SUPPORT);
    }
    if (delta.heteroaromaticRingCount > 0
        && delta.productHeteroaromaticRingCount >= delta.heteroaromaticRingCount
        && ReactionTopologyChangeScorer.heteroarylSubstitutionContext(request)) {
      score +=
          ReactionTopologyChangeScorer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN,
              "Candidate preserves the heteroaromatic scaffold while changing substituent topology.",
              HETEROARYL_SCAFFOLD_RETENTION_SUPPORT);
    }
    if (delta.bondOrderIncreaseCount > 0 && request.hasDomain(OrganicReactionDomain.ELIMINATION)) {
      score +=
          ReactionTopologyChangeScorer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
              "Candidate forms a higher-order bond under elimination/unsaturation context.",
              UNSATURATION_SUPPORT * Math.min(2, delta.bondOrderIncreaseCount));
    }
    if (delta.bondOrderDecreaseCount > 0 && request.hasDomain(OrganicReactionDomain.ADDITION)) {
      score +=
          ReactionTopologyChangeScorer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
              "Candidate lowers bond order under addition/saturation context.",
              SATURATION_SUPPORT * Math.min(2, delta.bondOrderDecreaseCount));
    }
    return score;
  }

  private static GraphDelta delta(final ReactionSide reactants, final ReactionSide products) {
    final HashMap<String, Integer> reactantBonds =
        ReactionTopologyChangeScorer.bondSignatures(reactants);
    final HashMap<String, Integer> productBonds =
        ReactionTopologyChangeScorer.bondSignatures(products);
    final HashMap<String, Integer> reactantElements =
        ReactionTopologyChangeScorer.elementCounts(reactants);
    final HashMap<String, Integer> productElements =
        ReactionTopologyChangeScorer.elementCounts(products);
    int brokenBondCount = 0;
    int formedBondCount = 0;
    int orderIncreaseCount = 0;
    int orderDecreaseCount = 0;
    for (final Map.Entry<String, Integer> entry : reactantBonds.entrySet()) {
      final int productCount = productBonds.getOrDefault(entry.getKey(), 0);
      if (entry.getValue() > productCount) {
        brokenBondCount += entry.getValue() - productCount;
      }
    }
    for (final Map.Entry<String, Integer> entry : productBonds.entrySet()) {
      final int reactantCount = reactantBonds.getOrDefault(entry.getKey(), 0);
      if (entry.getValue() > reactantCount) {
        formedBondCount += entry.getValue() - reactantCount;
      }
    }
    for (final Map.Entry<String, Integer> entry : reactantBonds.entrySet()) {
      final BondCoreSignature core = BondCoreSignature.parse(entry.getKey());
      if (core == null) {
        continue;
      }
      final int reactantOrder = core.order;
      final int productOrder =
          ReactionTopologyChangeScorer.maxOrderForCore(productBonds, core.core);
      if (productOrder > reactantOrder) {
        orderIncreaseCount +=
            Math.min(entry.getValue(), productBonds.getOrDefault(core.withOrder(productOrder), 0));
      } else if (productOrder > 0 && productOrder < reactantOrder) {
        orderDecreaseCount +=
            Math.min(entry.getValue(), productBonds.getOrDefault(core.withOrder(productOrder), 0));
      }
    }
    return new GraphDelta(
        brokenBondCount,
        formedBondCount,
        orderIncreaseCount,
        orderDecreaseCount,
        ReactionTopologyChangeScorer.leavingGroupLossCount(reactantElements, productElements),
        ReactionTopologyChangeScorer.carbonHalogenBondLossCount(reactantBonds, productBonds),
        ReactionTopologyChangeScorer.formedCarbonHeteroatomBondCount(reactantBonds, productBonds),
        ReactionTopologyChangeScorer.heteroaromaticRingCount(reactants),
        ReactionTopologyChangeScorer.heteroaromaticRingCount(products));
  }

  private static HashMap<String, Integer> bondSignatures(final ReactionSide side) {
    final HashMap<String, Integer> signatures = new HashMap<String, Integer>();
    final List<ReactionParticipant> participants = side.participants();
    for (int i = 0; i < participants.size(); ++i) {
      final ReactionParticipant participant = participants.get(i);
      final int coefficient = participant.coefficient().value();
      final Molecule molecule = participant.molecule();
      final List<Bond> bonds = molecule.bonds();
      for (int j = 0; j < bonds.size(); ++j) {
        signatures.merge(
            ReactionTopologyChangeScorer.bondSignature(molecule, bonds.get(j)),
            coefficient,
            Integer::sum);
      }
    }
    return signatures;
  }

  private static HashMap<String, Integer> elementCounts(final ReactionSide side) {
    final HashMap<String, Integer> counts = new HashMap<String, Integer>();
    final List<ReactionParticipant> participants = side.participants();
    for (int i = 0; i < participants.size(); ++i) {
      final ReactionParticipant participant = participants.get(i);
      final int coefficient = participant.coefficient().value();
      final List<Atom> atoms = participant.molecule().atoms();
      for (int j = 0; j < atoms.size(); ++j) {
        counts.merge(atoms.get(j).element().symbol().value(), coefficient, Integer::sum);
      }
    }
    return counts;
  }

  private static String bondSignature(final Molecule molecule, final Bond bond) {
    final String first = ReactionTopologyChangeScorer.elementSymbol(molecule, bond.firstAtomId());
    final String second = ReactionTopologyChangeScorer.elementSymbol(molecule, bond.secondAtomId());
    final String core = first.compareTo(second) <= 0 ? first + "-" + second : second + "-" + first;
    return core + ":" + ReactionTopologyChangeScorer.order(bond.type());
  }

  private static String elementSymbol(final Molecule molecule, final AtomId atomId) {
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      if (atoms.get(i).id().equals(atomId)) {
        return atoms.get(i).element().symbol().value();
      }
    }
    throw new IllegalArgumentException("Topology scoring bond references unknown atom.");
  }

  private static int maxOrderForCore(final HashMap<String, Integer> signatures, final String core) {
    int max = 0;
    for (final String signature : signatures.keySet()) {
      final BondCoreSignature parsed = BondCoreSignature.parse(signature);
      if (parsed == null || !parsed.core.equals(core)) {
        continue;
      }
      max = Math.max(max, parsed.order);
    }
    return max;
  }

  private static int leavingGroupLossCount(
      final HashMap<String, Integer> reactants, final HashMap<String, Integer> products) {
    return ReactionTopologyChangeScorer.elementLoss(reactants, products, "F")
        + ReactionTopologyChangeScorer.elementLoss(reactants, products, "Cl")
        + ReactionTopologyChangeScorer.elementLoss(reactants, products, "Br")
        + ReactionTopologyChangeScorer.elementLoss(reactants, products, "I");
  }

  private static int carbonHalogenBondLossCount(
      final HashMap<String, Integer> reactants, final HashMap<String, Integer> products) {
    return ReactionTopologyChangeScorer.bondLoss(reactants, products, "C-Cl")
        + ReactionTopologyChangeScorer.bondLoss(reactants, products, "Br-C")
        + ReactionTopologyChangeScorer.bondLoss(reactants, products, "C-F")
        + ReactionTopologyChangeScorer.bondLoss(reactants, products, "C-I");
  }

  private static int formedCarbonHeteroatomBondCount(
      final HashMap<String, Integer> reactants, final HashMap<String, Integer> products) {
    return ReactionTopologyChangeScorer.bondGain(reactants, products, "C-N")
        + ReactionTopologyChangeScorer.bondGain(reactants, products, "C-O")
        + ReactionTopologyChangeScorer.bondGain(reactants, products, "C-S");
  }

  private static int bondLoss(
      final HashMap<String, Integer> reactants,
      final HashMap<String, Integer> products,
      final String core) {
    int loss = 0;
    for (int order = 1; order <= 3; ++order) {
      final String signature = core + ":" + order;
      loss +=
          Math.max(0, reactants.getOrDefault(signature, 0) - products.getOrDefault(signature, 0));
    }
    return loss;
  }

  private static int bondGain(
      final HashMap<String, Integer> reactants,
      final HashMap<String, Integer> products,
      final String core) {
    int gain = 0;
    for (int order = 1; order <= 3; ++order) {
      final String signature = core + ":" + order;
      gain +=
          Math.max(0, products.getOrDefault(signature, 0) - reactants.getOrDefault(signature, 0));
    }
    return gain;
  }

  private static int elementLoss(
      final HashMap<String, Integer> reactants,
      final HashMap<String, Integer> products,
      final String symbol) {
    return Math.max(0, reactants.getOrDefault(symbol, 0) - products.getOrDefault(symbol, 0));
  }

  private static boolean leavingGroupContext(final ReactionPredictionRequest request) {
    return request.hasDomain(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION)
        || request.hasDomain(OrganicReactionDomain.CROSS_COUPLING)
        || request.hasDomain(OrganicReactionDomain.ELIMINATION)
        || request.hasDomain(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION);
  }

  private static boolean heteroarylSubstitutionContext(final ReactionPredictionRequest request) {
    return request.hasDomain(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION)
        || request.hasDomain(OrganicReactionDomain.CROSS_COUPLING)
        || request.hasDomain(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION);
  }

  private static int heteroaromaticRingCount(final ReactionSide side) {
    int count = 0;
    final List<ReactionParticipant> participants = side.participants();
    for (int i = 0; i < participants.size(); ++i) {
      count +=
          participants.get(i).coefficient().value()
              * ReactionTopologyChangeScorer.heteroaromaticRingCount(
                  participants.get(i).molecule());
    }
    return count;
  }

  private static int heteroaromaticRingCount(final Molecule molecule) {
    int count = 0;
    final List<MolecularRing> rings = molecule.graph().rings();
    for (int i = 0; i < rings.size(); ++i) {
      final MolecularRing ring = rings.get(i);
      if (ReactionTopologyChangeScorer.hasRingHeteroAtom(molecule, ring)
          && ReactionTopologyChangeScorer.hasRingAromaticBond(molecule, ring)) {
        ++count;
      }
    }
    return count;
  }

  private static boolean hasRingHeteroAtom(final Molecule molecule, final MolecularRing ring) {
    final List<AtomId> atomIds = ring.atomIds();
    for (int i = 0; i < atomIds.size(); ++i) {
      final String symbol = ReactionTopologyChangeScorer.elementSymbol(molecule, atomIds.get(i));
      if ("N".equals(symbol) || "O".equals(symbol) || "S".equals(symbol)) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasRingAromaticBond(final Molecule molecule, final MolecularRing ring) {
    return molecule.aromaticity().isAromaticRing(ring);
  }

  private static boolean claimsTransform(final ReactionProductCandidate candidate) {
    final List<ReactionPredictionEvidence> evidence = candidate.evidence();
    for (int i = 0; i < evidence.size(); ++i) {
      final String text = evidence.get(i).description();
      if (text.contains("transform")
          || text.contains("graph")
          || text.contains("replacement")
          || text.contains("addition")
          || text.contains("elimination")) {
        return true;
      }
    }
    return false;
  }

  private static int order(final BondType type) {
    if (type == BondType.TRIPLE) {
      return 3;
    }
    if (type == BondType.DOUBLE || type == BondType.AROMATIC) {
      return 2;
    }
    if (type == BondType.UNKNOWN) {
      return 0;
    }
    return 1;
  }

  private static double addEvidence(
      final List<ReactionPredictionEvidence> evidence,
      final ReactionPredictionEvidenceKind kind,
      final String description,
      final double weight) {
    evidence.add(ReactionPredictionEvidence.of(kind, description, weight));
    return weight;
  }

  private static final class GraphDelta {

    private final int brokenBondCount;
    private final int formedBondCount;
    private final int bondOrderIncreaseCount;
    private final int bondOrderDecreaseCount;
    private final int leavingGroupLossCount;
    private final int carbonHalogenBondLossCount;
    private final int formedCarbonHeteroatomBondCount;
    private final int heteroaromaticRingCount;
    private final int productHeteroaromaticRingCount;

    private GraphDelta(
        final int brokenBondCount,
        final int formedBondCount,
        final int bondOrderIncreaseCount,
        final int bondOrderDecreaseCount,
        final int leavingGroupLossCount,
        final int carbonHalogenBondLossCount,
        final int formedCarbonHeteroatomBondCount,
        final int heteroaromaticRingCount,
        final int productHeteroaromaticRingCount) {
      this.brokenBondCount = brokenBondCount;
      this.formedBondCount = formedBondCount;
      this.bondOrderIncreaseCount = bondOrderIncreaseCount;
      this.bondOrderDecreaseCount = bondOrderDecreaseCount;
      this.leavingGroupLossCount = leavingGroupLossCount;
      this.carbonHalogenBondLossCount = carbonHalogenBondLossCount;
      this.formedCarbonHeteroatomBondCount = formedCarbonHeteroatomBondCount;
      this.heteroaromaticRingCount = heteroaromaticRingCount;
      this.productHeteroaromaticRingCount = productHeteroaromaticRingCount;
    }

    private boolean changed() {
      return this.brokenBondCount > 0
          || this.formedBondCount > 0
          || this.bondOrderIncreaseCount > 0
          || this.bondOrderDecreaseCount > 0
          || this.leavingGroupLossCount > 0;
    }
  }

  private static final class BondCoreSignature {

    private final String core;
    private final int order;

    private BondCoreSignature(final String core, final int order) {
      this.core = core;
      this.order = order;
    }

    private static BondCoreSignature parse(final String signature) {
      final int separator = signature.lastIndexOf(':');
      if (separator < 0 || separator == signature.length() - 1) {
        return null;
      }
      return new BondCoreSignature(
          signature.substring(0, separator), Integer.parseInt(signature.substring(separator + 1)));
    }

    private String withOrder(final int nextOrder) {
      return this.core + ":" + nextOrder;
    }
  }
}

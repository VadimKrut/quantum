/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.feature.MolecularFeatureAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;

/**
 * Оценивает, насколько заданные условия подходят выбранным органическим доменам реакции.
 */
public final class OrganicReactionDomainConditionAdvisor {

  private final MolecularFeatureAnalyzer featureAnalyzer;

  public OrganicReactionDomainConditionAdvisor() {
    this(new MolecularFeatureAnalyzer());
  }

  public OrganicReactionDomainConditionAdvisor(final MolecularFeatureAnalyzer featureAnalyzer) {
    if (featureAnalyzer == null) {
      throw new IllegalArgumentException(
          "Organic reaction domain advisor feature analyzer must not be null.");
    }
    this.featureAnalyzer = featureAnalyzer;
  }

  public List<ReactionPredictionEvidence> advise(final ReactionPredictionRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction prediction request must not be null.");
    }
    final ArrayList<ReactionPredictionEvidence> evidence =
        new ArrayList<ReactionPredictionEvidence>(request.domains().size() * 3);
    for (int i = 0; i < request.domains().size(); ++i) {
      this.adviseDomain(request.domains().get(i), request, evidence);
    }
    return List.copyOf(evidence);
  }

  private void adviseDomain(
      final OrganicReactionDomain domain,
      final ReactionPredictionRequest request,
      final List<ReactionPredictionEvidence> evidence) {
    if (domain == OrganicReactionDomain.HETEROCYCLE_SYNTHESIS) {
      this.adviseHeterocycleSynthesis(request, evidence);
    } else if (domain == OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION) {
      this.adviseHeterocycleFunctionalization(request, evidence);
    } else if (domain == OrganicReactionDomain.CROSS_COUPLING) {
      this.adviseCrossCoupling(request, evidence);
    } else if (domain == OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION) {
      this.adviseNucleophilicSubstitution(request, evidence);
    } else if (domain == OrganicReactionDomain.ELECTROPHILIC_SUBSTITUTION) {
      this.adviseElectrophilicSubstitution(request, evidence);
    } else if (domain == OrganicReactionDomain.ADDITION) {
      this.adviseAddition(request, evidence);
    } else if (domain == OrganicReactionDomain.ELIMINATION) {
      this.adviseElimination(request, evidence);
    } else if (domain == OrganicReactionDomain.CYCLIZATION) {
      this.adviseCyclization(request, evidence);
    } else if (domain == OrganicReactionDomain.OXIDATION_REDUCTION) {
      this.adviseOxidationReduction(request, evidence);
    } else if (domain == OrganicReactionDomain.ACID_BASE) {
      this.adviseAcidBase(request, evidence);
    } else if (domain == OrganicReactionDomain.PROTECTION_DEPROTECTION) {
      this.adviseProtection(request, evidence);
    } else if (domain == OrganicReactionDomain.REARRANGEMENT) {
      this.adviseRearrangement(request, evidence);
    }
  }

  private void adviseHeterocycleSynthesis(
      final ReactionPredictionRequest request, final List<ReactionPredictionEvidence> evidence) {
    final long heteroAtoms =
        this.reactantFeatureCount(request, MolecularFeatureKind.HETERO_ATOM_COUNT);
    final long carbonyls = this.reactantFeatureCount(request, MolecularFeatureKind.CARBONYL);
    final ReactionConditions conditions = request.conditions();
    if (heteroAtoms > 0L && carbonyls > 0L) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
              "Heterocycle synthesis request contains hetero atoms and carbonyl electrophile context.",
              0.07));
    } else {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.WARNING,
              "Heterocycle synthesis usually needs explicit heteroatom and carbonyl/imine building blocks.",
              -0.10));
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.DEHYDRATING_AGENT)
        || conditions.hasFeature(ReactionConditionFeatureKind.ACIDIC_MEDIUM)
        || conditions.hasFeature(ReactionConditionFeatureKind.HEATING)) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.THERMODYNAMIC_FACTOR,
              "Heterocycle synthesis has activation or water-removal condition signal.",
              0.05));
    }
  }

  private void adviseHeterocycleFunctionalization(
      final ReactionPredictionRequest request, final List<ReactionPredictionEvidence> evidence) {
    final long heterocycles = this.reactantFeatureCount(request, MolecularFeatureKind.HETEROCYCLE);
    final long leavingGroups = this.reactantFeatureCount(request, MolecularFeatureKind.HALOGENATED);
    if (heterocycles > 0L) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN,
              "Heterocycle functionalization request contains a heterocyclic reactant.",
              0.08));
    }
    if (leavingGroups > 0L) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
              "Heterocycle functionalization has halogen/leaving-group signal for substitution or coupling.",
              0.06));
    } else {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.WARNING,
              "Heterocycle functionalization has no detected leaving-group/halogen handle.",
              -0.08));
    }
  }

  private void adviseCrossCoupling(
      final ReactionPredictionRequest request, final List<ReactionPredictionEvidence> evidence) {
    final ReactionConditions conditions = request.conditions();
    if (this.hasRole(conditions, ReactionComponentRole.CATALYST)) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.CATALYST_EFFECT,
              "Cross-coupling request has an explicit catalyst component.",
              0.08));
    } else {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.CATALYST_EFFECT,
              "Cross-coupling request has no explicit catalyst component.",
              -0.16));
    }
    if (this.hasRole(conditions, ReactionComponentRole.BASE)) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
              "Cross-coupling request has an explicit base component.",
              0.04));
    } else {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.WARNING,
              "Cross-coupling request has no base component; transmetalation/reductive-elimination context is incomplete.",
              -0.08));
    }
    if (this.reactantFeatureCount(request, MolecularFeatureKind.HALOGENATED) > 0L) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
              "Cross-coupling request contains an aryl/heteroaryl halide-like handle.",
              0.05));
    }
  }

  private void adviseNucleophilicSubstitution(
      final ReactionPredictionRequest request, final List<ReactionPredictionEvidence> evidence) {
    final ReactionConditions conditions = request.conditions();
    if (this.reactantFeatureCount(request, MolecularFeatureKind.HALOGENATED) > 0L) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
              "Nucleophilic substitution request has a leaving-group/halogen handle.",
              0.05));
    } else {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.WARNING,
              "Nucleophilic substitution request has no detected leaving group.",
              -0.10));
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.BASIC_MEDIUM)
        || this.hasRole(conditions, ReactionComponentRole.BASE)) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
              "Nucleophilic substitution request has basic/nucleophile-supporting condition signal.",
              0.04));
    }
    if (this.hasPolarAproticSolvent(conditions)) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.SOLVENT_EFFECT,
              "Nucleophilic substitution request uses a polar aprotic solvent signal.",
              0.04));
    }
  }

  private void adviseElectrophilicSubstitution(
      final ReactionPredictionRequest request, final List<ReactionPredictionEvidence> evidence) {
    if (this.reactantFeatureCount(request, MolecularFeatureKind.AROMATIC_RING) > 0L) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
              "Electrophilic substitution request contains aromatic ring context.",
              0.04));
    }
    if (request.conditions().hasFeature(ReactionConditionFeatureKind.ACIDIC_MEDIUM)
        || this.hasRole(request.conditions(), ReactionComponentRole.ACID)) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
              "Electrophilic substitution request has acid/electrophile activation signal.",
              0.04));
    }
  }

  private void adviseAddition(
      final ReactionPredictionRequest request, final List<ReactionPredictionEvidence> evidence) {
    final long unsaturation =
        this.reactantFeatureCount(request, MolecularFeatureKind.ALKENE)
            + this.reactantFeatureCount(request, MolecularFeatureKind.ALKYNE)
            + this.reactantFeatureCount(request, MolecularFeatureKind.CARBONYL);
    if (unsaturation > 0L) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
              "Addition request contains unsaturated or carbonyl acceptor context.",
              0.05));
    } else {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.WARNING,
              "Addition request has no detected alkene, alkyne or carbonyl acceptor.",
              -0.08));
    }
  }

  private void adviseElimination(
      final ReactionPredictionRequest request, final List<ReactionPredictionEvidence> evidence) {
    final ReactionConditions conditions = request.conditions();
    if (this.reactantFeatureCount(request, MolecularFeatureKind.HALOGENATED) > 0L
        || this.reactantFeatureCount(request, MolecularFeatureKind.HYDROXYL_OR_ALKOXY) > 0L) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
              "Elimination request contains leaving-group or alcohol/alkoxy context.",
              0.04));
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.BASIC_MEDIUM)
        || this.hasRole(conditions, ReactionComponentRole.BASE)) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
              "Elimination request has base condition signal.",
              0.04));
    }
  }

  private void adviseCyclization(
      final ReactionPredictionRequest request, final List<ReactionPredictionEvidence> evidence) {
    if (this.reactantFeatureCount(request, MolecularFeatureKind.HETERO_ATOM_COUNT) > 0L
        && this.reactantFeatureCount(request, MolecularFeatureKind.CARBONYL) > 0L) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
              "Cyclization request contains heteroatom and electrophile context for ring closure.",
              0.05));
    }
    if (request.conditions().hasFeature(ReactionConditionFeatureKind.ADDITION_RATE)
        || request.conditions().hasFeature(ReactionConditionFeatureKind.FLOW_REACTOR)) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Cyclization request has dilution/addition-rate or flow signal for intramolecular selectivity.",
              0.04));
    }
  }

  private void adviseOxidationReduction(
      final ReactionPredictionRequest request, final List<ReactionPredictionEvidence> evidence) {
    if (request.conditions().hasFeature(ReactionConditionFeatureKind.ELECTROCHEMICAL)) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
              "Oxidation-reduction request has electrochemical activation signal.",
              0.05));
    }
    if (this.hasRole(request.conditions(), ReactionComponentRole.ADDITIVE)
        || this.hasRole(request.conditions(), ReactionComponentRole.CATALYST)) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.THERMODYNAMIC_FACTOR,
              "Oxidation-reduction request has reagent/catalyst component context.",
              0.04));
    }
  }

  private void adviseAcidBase(
      final ReactionPredictionRequest request, final List<ReactionPredictionEvidence> evidence) {
    if (this.hasRole(request.conditions(), ReactionComponentRole.ACID)
        || this.hasRole(request.conditions(), ReactionComponentRole.BASE)
        || request.conditions().hasFeature(ReactionConditionFeatureKind.ACIDIC_MEDIUM)
        || request.conditions().hasFeature(ReactionConditionFeatureKind.BASIC_MEDIUM)) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
              "Acid-base request has explicit proton-transfer condition signal.",
              0.05));
    } else {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.WARNING,
              "Acid-base request has no acid/base component or medium feature.",
              -0.08));
    }
  }

  private void adviseProtection(
      final ReactionPredictionRequest request, final List<ReactionPredictionEvidence> evidence) {
    if (this.reactantFeatureCount(request, MolecularFeatureKind.HYDROXYL_OR_ALKOXY) > 0L
        || this.reactantFeatureCount(request, MolecularFeatureKind.AMINE) > 0L) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
              "Protection/deprotection request contains alcohol/alkoxy or amine functionality.",
              0.05));
    }
    if (request.conditions().hasFeature(ReactionConditionFeatureKind.ANHYDROUS)) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.SOLVENT_EFFECT,
              "Protection/deprotection request has anhydrous condition signal.",
              0.03));
    }
  }

  private void adviseRearrangement(
      final ReactionPredictionRequest request, final List<ReactionPredictionEvidence> evidence) {
    if (request.conditions().hasFeature(ReactionConditionFeatureKind.HEATING)
        || request.conditions().hasFeature(ReactionConditionFeatureKind.PHOTOCHEMICAL)
        || request.conditions().hasFeature(ReactionConditionFeatureKind.ACIDIC_MEDIUM)) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Rearrangement request has thermal, photochemical or acid activation signal.",
              0.04));
    } else {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.WARNING,
              "Rearrangement request has no explicit activation condition.",
              -0.06));
    }
  }

  private long reactantFeatureCount(
      final ReactionPredictionRequest request, final MolecularFeatureKind kind) {
    long count = 0L;
    final List<ReactionParticipant> participants = request.reactants().participants();
    for (int i = 0; i < participants.size(); ++i) {
      final MolecularFeatureSet features =
          this.featureAnalyzer.analyze(participants.get(i).molecule());
      count += features.countOf(kind);
    }
    return count;
  }

  private boolean hasPolarAproticSolvent(final ReactionConditions conditions) {
    final List<ReactionConditionComponent> solvents =
        conditions.componentsByRole(ReactionComponentRole.SOLVENT);
    for (int i = 0; i < solvents.size(); ++i) {
      final String name = solvents.get(i).name().toLowerCase(Locale.ROOT);
      if (name.contains("dmf")
          || name.contains("dmso")
          || name.contains("nmp")
          || name.contains("acetonitrile")
          || name.contains("mecn")
          || name.contains("thf")) {
        return true;
      }
    }
    return false;
  }

  private boolean hasRole(final ReactionConditions conditions, final ReactionComponentRole role) {
    return conditions != null && !conditions.componentsByRole(role).isEmpty();
  }
}

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
import ru.pathcreator.vadim.quantum.chemistry.classical.application.transform.MolecularTransformProduct;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/**
 * Оценивает локальное реакционное окружение выбранного atom-site без привязки к конкретной статье.
 */
public final class ReactionSiteSelectivityAnalyzer {

  private static final double MIN_MULTIPLIER = 0.35;
  private static final double MAX_MULTIPLIER = 1.85;

  public ReactionSiteSelectivityAssessment assess(
      final ReactionPredictionRequest request,
      final Molecule reactant,
      final MolecularTransformProduct product,
      final double baseProbability,
      final double expectedYieldPercent) {
    return this.assess(
        request, reactant, reactant, product, baseProbability, expectedYieldPercent, false);
  }

  public ReactionSiteSelectivityAssessment assess(
      final ReactionPredictionRequest request,
      final Molecule siteReactant,
      final Molecule leavingGroupReactant,
      final MolecularTransformProduct product,
      final double baseProbability,
      final double expectedYieldPercent) {
    return this.assess(
        request,
        siteReactant,
        leavingGroupReactant,
        product,
        baseProbability,
        expectedYieldPercent,
        false);
  }

  public ReactionSiteSelectivityAssessment assess(
      final ReactionPredictionRequest request,
      final Molecule siteReactant,
      final Molecule leavingGroupReactant,
      final MolecularTransformProduct product,
      final double baseProbability,
      final double expectedYieldPercent,
      final boolean antiMarkovnikovAddition) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction site-selectivity request must not be null.");
    }
    if (siteReactant == null) {
      throw new IllegalArgumentException(
          "Reaction site-selectivity site reactant must not be null.");
    }
    if (leavingGroupReactant == null) {
      throw new IllegalArgumentException(
          "Reaction site-selectivity leaving-group reactant must not be null.");
    }
    if (product == null) {
      throw new IllegalArgumentException("Reaction site-selectivity product must not be null.");
    }
    final Atom site =
        ReactionSiteSelectivityAnalyzer.requiredAtom(siteReactant, product.anchorAtomId());
    final SiteEnvironment environment =
        ReactionSiteSelectivityAnalyzer.environment(siteReactant, site.id());
    double multiplier = 1.0;
    final ArrayList<ReactionPredictionEvidence> evidence =
        new ArrayList<ReactionPredictionEvidence>(6);
    multiplier += this.electronPoorHeteroarylContribution(request, environment, evidence);
    multiplier +=
        this.leavingGroupContribution(
            request, leavingGroupReactant, product, environment, evidence);
    multiplier += this.electronWithdrawingContribution(request, environment, evidence);
    multiplier += this.radicalContribution(request, environment, evidence);
    multiplier += this.stericContribution(environment, evidence);
    multiplier += this.chargeContribution(environment, evidence);
    multiplier += this.conditionContribution(request, environment, evidence);
    multiplier +=
        this.partnerRegioselectivityContribution(
            siteReactant, product, environment, antiMarkovnikovAddition, evidence);
    final double clampedMultiplier = Math.max(MIN_MULTIPLIER, Math.min(MAX_MULTIPLIER, multiplier));
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
            "Site-selectivity multiplier="
                + clampedMultiplier
                + " for "
                + product.mappingDescription()
                + " using hetero-neighbor count="
                + environment.heteroNeighborCount
                + ", heavy degree="
                + environment.heavyDegree
                + ", second-sphere hetero atoms="
                + environment.secondSphereHeteroAtomCount
                + ", electron-withdrawing neighbors="
                + environment.electronWithdrawingNeighborCount
                + ", unsaturation="
                + environment.unsaturatedNeighborCount
                + ".",
            Math.max(-0.24, Math.min(0.34, (clampedMultiplier - 1.0) * 0.25))));
    return ReactionSiteSelectivityAssessment.of(
        ReactionSiteSelectivityAnalyzer.clampProbability(baseProbability * clampedMultiplier),
        ReactionSiteSelectivityAnalyzer.clampYield(
            expectedYieldPercent * Math.sqrt(clampedMultiplier)),
        evidence);
  }

  private double electronPoorHeteroarylContribution(
      final ReactionPredictionRequest request,
      final SiteEnvironment environment,
      final List<ReactionPredictionEvidence> evidence) {
    if (!request.hasDomain(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION)
        && !request.hasDomain(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION)) {
      return 0.0;
    }
    if (environment.heteroNeighborCount <= 0
        && environment.secondSphereHeteroAtomCount <= 0
        && !environment.adjacentToChargedCenter) {
      return 0.0;
    }
    final double contribution =
        Math.min(
            0.42,
            0.18 * environment.heteroNeighborCount
                + 0.10 * environment.secondSphereHeteroAtomCount
                + (environment.adjacentToChargedCenter ? 0.10 : 0.0));
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
            "Electron-poor heteroaryl site is activated by nearby heteroatom/charged centers.",
            contribution));
    return contribution;
  }

  private double leavingGroupContribution(
      final ReactionPredictionRequest request,
      final Molecule reactant,
      final MolecularTransformProduct product,
      final SiteEnvironment environment,
      final List<ReactionPredictionEvidence> evidence) {
    if (!product.hasLeavingGroupAtom()) {
      return 0.0;
    }
    final double leavingGroupStrength =
        ReactionSiteSelectivityAnalyzer.leavingGroupStrength(
            request, reactant, product, environment);
    double contribution = 0.14 + leavingGroupStrength;
    if (request.conditions().hasFeature(ReactionConditionFeatureKind.BASIC_MEDIUM)) {
      contribution += 0.06;
    }
    if (request.hasDomain(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION)
        && environment.electronWithdrawingNeighborCount > 0) {
      contribution += Math.min(0.10, 0.04 * environment.electronWithdrawingNeighborCount);
    }
    if (environment.leavingGroupNeighborCount > 1) {
      contribution -= 0.04 * (environment.leavingGroupNeighborCount - 1);
    }
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
            "Site has an explicit consumed leaving group under substitution/coupling context"
                + " with context-sensitive leaving-group strength="
                + leavingGroupStrength
                + ".",
            contribution));
    return contribution;
  }

  private double electronWithdrawingContribution(
      final ReactionPredictionRequest request,
      final SiteEnvironment environment,
      final List<ReactionPredictionEvidence> evidence) {
    if (!request.hasDomain(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION)
        && !request.hasDomain(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION)
        && !request.hasDomain(OrganicReactionDomain.ELECTROPHILIC_SUBSTITUTION)) {
      return 0.0;
    }
    if (environment.electronWithdrawingNeighborCount <= 0 && environment.piAcceptorBondCount <= 0) {
      return 0.0;
    }
    double contribution =
        Math.min(
            0.30,
            0.10 * environment.electronWithdrawingNeighborCount
                + 0.09 * environment.piAcceptorBondCount);
    if (request.hasDomain(OrganicReactionDomain.ELECTROPHILIC_SUBSTITUTION)) {
      contribution = -Math.min(0.18, contribution);
    }
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
            "Local electron-withdrawing/pi-acceptor environment changes site reactivity.",
            contribution));
    return contribution;
  }

  private double radicalContribution(
      final ReactionPredictionRequest request,
      final SiteEnvironment environment,
      final List<ReactionPredictionEvidence> evidence) {
    if (!request.hasDomain(OrganicReactionDomain.RADICAL_FUNCTIONALIZATION)
        && !request.conditions().hasFeature(ReactionConditionFeatureKind.PHOTOCHEMICAL)) {
      return 0.0;
    }
    final double contribution =
        environment.radicalCenter
            ? 0.22
            : Math.min(0.18, 0.06 * environment.unsaturatedNeighborCount);
    if (contribution <= 0.0) {
      return 0.0;
    }
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.KINETIC_FACTOR,
            "Radical/photochemical context favors unsaturated or radical-stabilized site.",
            contribution));
    return contribution;
  }

  private double stericContribution(
      final SiteEnvironment environment, final List<ReactionPredictionEvidence> evidence) {
    if (environment.heavyDegree <= 2) {
      return 0.0;
    }
    final double penalty = -Math.min(0.30, 0.09 * (environment.heavyDegree - 2));
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.STERIC_FACTOR,
            "Crowded site receives a steric penalty based on heavy-atom degree.",
            penalty));
    return penalty;
  }

  private double chargeContribution(
      final SiteEnvironment environment, final List<ReactionPredictionEvidence> evidence) {
    if (environment.siteCharge == 0) {
      return 0.0;
    }
    final double contribution = environment.siteCharge > 0 ? 0.12 : -0.10;
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
            "Formal charge at reactive site changes local electrophilic/nucleophilic accessibility.",
            contribution));
    return contribution;
  }

  private double conditionContribution(
      final ReactionPredictionRequest request,
      final SiteEnvironment environment,
      final List<ReactionPredictionEvidence> evidence) {
    double contribution = 0.0;
    final boolean substitution =
        request.hasDomain(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION)
            || request.hasDomain(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION);
    if (request.conditions().hasFeature(ReactionConditionFeatureKind.HEATING)
        && environment.heavyDegree <= 2) {
      contribution += 0.04;
    }
    if (request.conditions().hasFeature(ReactionConditionFeatureKind.COOLING)
        && environment.heavyDegree > 2) {
      contribution -= 0.04;
    }
    if (request.conditions().hasFeature(ReactionConditionFeatureKind.CATALYST_PRESENT)
        && environment.unsaturatedNeighborCount > 0) {
      contribution += 0.05;
    }
    if (substitution
        && ReactionSiteSelectivityAnalyzer.polarAproticOrPolyetherMedium(request)
        && environment.heavyDegree <= 2
        && (environment.leavingGroupNeighborCount > 0
            || environment.electronWithdrawingNeighborCount > 0
            || environment.piAcceptorBondCount > 0)) {
      contribution += 0.08;
    }
    if (substitution
        && ReactionSiteSelectivityAnalyzer.proticOrWaterRichMedium(request)
        && environment.leavingGroupNeighborCount > 0) {
      contribution -= 0.07;
    }
    if (substitution
        && ReactionSiteSelectivityAnalyzer.strongBaseOrExcessNucleophile(request)
        && environment.leavingGroupNeighborCount > 1) {
      contribution -= 0.06 * Math.min(3, environment.leavingGroupNeighborCount - 1);
    }
    if (substitution
        && ReactionSiteSelectivityAnalyzer.strongBaseOrExcessNucleophile(request)
        && environment.leavingGroupNeighborCount == 1
        && environment.electronWithdrawingNeighborCount > 0) {
      contribution += 0.05;
    }
    if (contribution == 0.0) {
      return 0.0;
    }
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.CATALYST_EFFECT,
            "Declared solvent/base/nucleophile/temperature conditions shift local site accessibility and regioselectivity.",
            contribution));
    return contribution;
  }

  private double partnerRegioselectivityContribution(
      final Molecule molecule,
      final MolecularTransformProduct product,
      final SiteEnvironment anchorEnvironment,
      final boolean antiMarkovnikovAddition,
      final List<ReactionPredictionEvidence> evidence) {
    if (!product.hasPartnerAtom()) {
      return 0.0;
    }
    final SiteEnvironment partnerEnvironment =
        ReactionSiteSelectivityAnalyzer.environment(molecule, product.partnerAtomId());
    final int substitutionDelta = anchorEnvironment.heavyDegree - partnerEnvironment.heavyDegree;
    if (substitutionDelta == 0) {
      return 0.0;
    }
    final double direction = antiMarkovnikovAddition ? -1.0 : 1.0;
    final double contribution =
        Math.max(-0.20, Math.min(0.20, direction * substitutionDelta * 0.08));
    if (contribution == 0.0) {
      return 0.0;
    }
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
            (antiMarkovnikovAddition ? "Anti-Markovnikov" : "Markovnikov-like")
                + " two-site addition ranking compares anchor heavy degree="
                + anchorEnvironment.heavyDegree
                + " with partner heavy degree="
                + partnerEnvironment.heavyDegree
                + ".",
            contribution));
    return contribution;
  }

  private static boolean polarAproticOrPolyetherMedium(final ReactionPredictionRequest request) {
    return ReactionSiteSelectivityAnalyzer.componentContextContains(
        request,
        ReactionComponentRole.SOLVENT,
        List.of(
            "dmf",
            "dmso",
            "nmp",
            "acetonitrile",
            "peg",
            "polyethylene glycol",
            "polyether",
            "glyme",
            "diglyme",
            "triglyme",
            "tetraglyme"));
  }

  private static boolean proticOrWaterRichMedium(final ReactionPredictionRequest request) {
    return ReactionSiteSelectivityAnalyzer.componentContextContains(
        request,
        ReactionComponentRole.SOLVENT,
        List.of(
            "water",
            "aqueous",
            "methanol",
            "ethanol",
            "propanol",
            "butanol",
            "acid",
            "hcl",
            "acetic"));
  }

  private static boolean strongBaseOrExcessNucleophile(final ReactionPredictionRequest request) {
    if (request.conditions().hasFeature(ReactionConditionFeatureKind.BASIC_MEDIUM)) {
      return true;
    }
    return ReactionSiteSelectivityAnalyzer.componentContextContains(
            request,
            ReactionComponentRole.BASE,
            List.of(
                "koh",
                "naoh",
                "tert-butoxide",
                "tbuok",
                "k2co3",
                "cs2co3",
                "carbonate",
                "hydride",
                "lda",
                "dbu",
                "dbaa",
                "triethylamine"))
        || ReactionSiteSelectivityAnalyzer.componentContextContains(
            request,
            ReactionComponentRole.REACTANT,
            List.of(
                "excess",
                "amine",
                "nucleophile",
                "benzylamine",
                "aniline",
                "hydrazine",
                "thiolate",
                "alkoxide"));
  }

  private static boolean componentContextContains(
      final ReactionPredictionRequest request,
      final ReactionComponentRole role,
      final List<String> tokens) {
    final List<ReactionConditionComponent> components = request.conditions().componentsByRole(role);
    for (int i = 0; i < components.size(); ++i) {
      final String context =
          (components.get(i).name()
                  + " "
                  + (components.get(i).hasNote() ? components.get(i).note() : ""))
              .toLowerCase(java.util.Locale.ROOT);
      for (int j = 0; j < tokens.size(); ++j) {
        if (context.contains(tokens.get(j))) {
          return true;
        }
      }
    }
    return false;
  }

  private static SiteEnvironment environment(final Molecule molecule, final AtomId siteId) {
    final Atom site = ReactionSiteSelectivityAnalyzer.requiredAtom(molecule, siteId);
    int heavyDegree = 0;
    int heteroNeighborCount = 0;
    int secondSphereHeteroAtomCount = 0;
    int electronWithdrawingNeighborCount = 0;
    int piAcceptorBondCount = 0;
    int leavingGroupNeighborCount = 0;
    int unsaturatedNeighborCount = 0;
    boolean adjacentToChargedCenter = false;
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (!bond.firstAtomId().equals(siteId) && !bond.secondAtomId().equals(siteId)) {
        continue;
      }
      final Atom neighbor =
          ReactionSiteSelectivityAnalyzer.requiredAtom(
              molecule,
              bond.firstAtomId().equals(siteId) ? bond.secondAtomId() : bond.firstAtomId());
      final String symbol = neighbor.element().symbol().value();
      if (!"H".equals(symbol)) {
        ++heavyDegree;
      }
      if (ReactionSiteSelectivityAnalyzer.heteroAtom(symbol)) {
        ++heteroNeighborCount;
      }
      if (ReactionSiteSelectivityAnalyzer.electronWithdrawingAtom(symbol)) {
        ++electronWithdrawingNeighborCount;
      }
      if (ReactionSiteSelectivityAnalyzer.leavingGroup(symbol)) {
        ++leavingGroupNeighborCount;
      }
      if (bond.type() == BondType.DOUBLE
          || bond.type() == BondType.TRIPLE
          || molecule.aromaticity().isAromaticBond(bond)) {
        ++unsaturatedNeighborCount;
      }
      if (ReactionSiteSelectivityAnalyzer.piAcceptorBond(molecule, bond, neighbor)) {
        ++piAcceptorBondCount;
      }
      if (neighbor.formalCharge().value() != 0) {
        adjacentToChargedCenter = true;
      }
      secondSphereHeteroAtomCount +=
          ReactionSiteSelectivityAnalyzer.secondSphereHeteroAtoms(molecule, siteId, neighbor.id());
    }
    return new SiteEnvironment(
        heavyDegree,
        heteroNeighborCount,
        secondSphereHeteroAtomCount,
        electronWithdrawingNeighborCount,
        piAcceptorBondCount,
        leavingGroupNeighborCount,
        unsaturatedNeighborCount,
        adjacentToChargedCenter,
        site.formalCharge().value(),
        site.radicalState().radical());
  }

  private static Atom requiredAtom(final Molecule molecule, final AtomId atomId) {
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      if (atom.id().equals(atomId)) {
        return atom;
      }
    }
    throw new IllegalArgumentException("Reaction site-selectivity references unknown atom.");
  }

  private static boolean heteroAtom(final String symbol) {
    return "N".equals(symbol)
        || "O".equals(symbol)
        || "S".equals(symbol)
        || "P".equals(symbol)
        || "B".equals(symbol);
  }

  private static boolean electronWithdrawingAtom(final String symbol) {
    return "N".equals(symbol)
        || "O".equals(symbol)
        || "S".equals(symbol)
        || "F".equals(symbol)
        || "Cl".equals(symbol)
        || "Br".equals(symbol)
        || "I".equals(symbol);
  }

  private static boolean leavingGroup(final String symbol) {
    return "F".equals(symbol) || "Cl".equals(symbol) || "Br".equals(symbol) || "I".equals(symbol);
  }

  private static boolean piAcceptorBond(
      final Molecule molecule, final Bond bond, final Atom neighbor) {
    if (bond.type() != BondType.DOUBLE && bond.type() != BondType.TRIPLE) {
      return false;
    }
    final String symbol = neighbor.element().symbol().value();
    if (ReactionSiteSelectivityAnalyzer.electronWithdrawingAtom(symbol)) {
      return true;
    }
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond candidate = bonds.get(i);
      if (!candidate.firstAtomId().equals(neighbor.id())
          && !candidate.secondAtomId().equals(neighbor.id())) {
        continue;
      }
      final Atom adjacent =
          ReactionSiteSelectivityAnalyzer.requiredAtom(
              molecule,
              candidate.firstAtomId().equals(neighbor.id())
                  ? candidate.secondAtomId()
                  : candidate.firstAtomId());
      if (candidate.type() == BondType.TRIPLE
          && ReactionSiteSelectivityAnalyzer.electronWithdrawingAtom(
              adjacent.element().symbol().value())) {
        return true;
      }
    }
    return false;
  }

  private static int secondSphereHeteroAtoms(
      final Molecule molecule, final AtomId siteId, final AtomId firstSphereAtomId) {
    int count = 0;
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (!bond.firstAtomId().equals(firstSphereAtomId)
          && !bond.secondAtomId().equals(firstSphereAtomId)) {
        continue;
      }
      final AtomId secondSphereAtomId =
          bond.firstAtomId().equals(firstSphereAtomId) ? bond.secondAtomId() : bond.firstAtomId();
      if (secondSphereAtomId.equals(siteId)) {
        continue;
      }
      final Atom atom = ReactionSiteSelectivityAnalyzer.requiredAtom(molecule, secondSphereAtomId);
      if (ReactionSiteSelectivityAnalyzer.heteroAtom(atom.element().symbol().value())) {
        ++count;
      }
    }
    return count;
  }

  private static double leavingGroupStrength(
      final ReactionPredictionRequest request,
      final Molecule reactant,
      final MolecularTransformProduct product,
      final SiteEnvironment environment) {
    final Atom leavingGroup =
        ReactionSiteSelectivityAnalyzer.requiredAtom(reactant, product.leavingGroupAtomId());
    final String symbol = leavingGroup.element().symbol().value();
    if (ReactionSiteSelectivityAnalyzer.activatedHeteroarylSnArContext(request, environment)) {
      return ReactionSiteSelectivityAnalyzer.activatedHeteroarylLeavingGroupStrength(symbol);
    }
    if ("I".equals(symbol)) {
      return 0.16;
    }
    if ("Br".equals(symbol)) {
      return 0.13;
    }
    if ("Cl".equals(symbol)) {
      return 0.10;
    }
    if ("F".equals(symbol)) {
      return 0.08;
    }
    return 0.04;
  }

  private static boolean activatedHeteroarylSnArContext(
      final ReactionPredictionRequest request, final SiteEnvironment environment) {
    return request.hasDomain(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION)
        && (environment.heteroNeighborCount > 0
            || environment.secondSphereHeteroAtomCount > 0
            || environment.electronWithdrawingNeighborCount > 0
            || environment.piAcceptorBondCount > 0
            || environment.adjacentToChargedCenter);
  }

  private static double activatedHeteroarylLeavingGroupStrength(final String symbol) {
    if ("F".equals(symbol)) {
      return 0.18;
    }
    if ("Cl".equals(symbol)) {
      return 0.15;
    }
    if ("Br".equals(symbol)) {
      return 0.13;
    }
    if ("I".equals(symbol)) {
      return 0.11;
    }
    return 0.04;
  }

  private static double clampProbability(final double value) {
    if (value <= 0.0) {
      return 0.01;
    }
    return Math.min(1.0, value);
  }

  private static double clampYield(final double value) {
    if (value <= 0.0) {
      return 0.0;
    }
    return Math.min(100.0, value);
  }

  private static final class SiteEnvironment {

    private final int heavyDegree;
    private final int heteroNeighborCount;
    private final int secondSphereHeteroAtomCount;
    private final int electronWithdrawingNeighborCount;
    private final int piAcceptorBondCount;
    private final int leavingGroupNeighborCount;
    private final int unsaturatedNeighborCount;
    private final boolean adjacentToChargedCenter;
    private final int siteCharge;
    private final boolean radicalCenter;

    private SiteEnvironment(
        final int heavyDegree,
        final int heteroNeighborCount,
        final int secondSphereHeteroAtomCount,
        final int electronWithdrawingNeighborCount,
        final int piAcceptorBondCount,
        final int leavingGroupNeighborCount,
        final int unsaturatedNeighborCount,
        final boolean adjacentToChargedCenter,
        final int siteCharge,
        final boolean radicalCenter) {
      this.heavyDegree = heavyDegree;
      this.heteroNeighborCount = heteroNeighborCount;
      this.secondSphereHeteroAtomCount = secondSphereHeteroAtomCount;
      this.electronWithdrawingNeighborCount = electronWithdrawingNeighborCount;
      this.piAcceptorBondCount = piAcceptorBondCount;
      this.leavingGroupNeighborCount = leavingGroupNeighborCount;
      this.unsaturatedNeighborCount = unsaturatedNeighborCount;
      this.adjacentToChargedCenter = adjacentToChargedCenter;
      this.siteCharge = siteCharge;
      this.radicalCenter = radicalCenter;
    }
  }
}

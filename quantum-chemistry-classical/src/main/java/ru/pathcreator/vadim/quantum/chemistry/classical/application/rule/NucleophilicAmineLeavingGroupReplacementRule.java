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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.feature.MolecularFeatureAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.stoichiometry.ReactionStoichiometryAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.stoichiometry.ReactionStoichiometryComponent;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.stoichiometry.ReactionStoichiometryProfile;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureSet;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRule;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleCategory;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleMatch;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ElementaryReactionStep;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanisticEvent;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanisticEventKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.PredictedReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
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

/**
 * Структурное правило SNAr-аминирования: переносит аминный фрагмент нуклеофила в продукт, а не заменяет leaving group одним атомом.
 */
public final class NucleophilicAmineLeavingGroupReplacementRule implements OrganicReactionRule {

  private static final ElementSymbol CARBON = ElementSymbol.of("C");
  private static final ElementSymbol CHLORINE = ElementSymbol.of("Cl");
  private static final ElementSymbol FLUORINE = ElementSymbol.of("F");
  private static final ElementSymbol BROMINE = ElementSymbol.of("Br");
  private static final ElementSymbol IODINE = ElementSymbol.of("I");
  private static final ElementSymbol HYDROGEN = ElementSymbol.of("H");
  private static final ElementSymbol NITROGEN = ElementSymbol.of("N");

  private final OrganicReactionRuleId id;
  private final String displayName;
  private final OrganicReactionRuleCategory category;
  private final ReactionMechanismFamily mechanismFamily;
  private final double baseProbability;
  private final double maxProbability;
  private final double expectedYieldLimit;
  private final String matchDescription;
  private final String mechanismDescription;
  private final String siteCombinationDescription;
  private final String candidateDescription;
  private final String mechanismIdPrefix;
  private final String productDescription;
  private final MolecularFeatureAnalyzer featureAnalyzer;
  private final OrganicReactionConditionMatcher conditionMatcher;
  private final ReactionStoichiometryAnalyzer stoichiometryAnalyzer;

  private NucleophilicAmineLeavingGroupReplacementRule(
      final OrganicReactionRuleId id,
      final String displayName,
      final OrganicReactionRuleCategory category,
      final ReactionMechanismFamily mechanismFamily,
      final double baseProbability,
      final double maxProbability,
      final double expectedYieldLimit,
      final String matchDescription,
      final String mechanismDescription,
      final String siteCombinationDescription,
      final String candidateDescription,
      final String mechanismIdPrefix,
      final String productDescription) {
    this.id = id;
    this.displayName = displayName;
    this.category = category;
    this.mechanismFamily = mechanismFamily;
    this.baseProbability = baseProbability;
    this.maxProbability = maxProbability;
    this.expectedYieldLimit = expectedYieldLimit;
    this.matchDescription = matchDescription;
    this.mechanismDescription = mechanismDescription;
    this.siteCombinationDescription = siteCombinationDescription;
    this.candidateDescription = candidateDescription;
    this.mechanismIdPrefix = mechanismIdPrefix;
    this.productDescription = productDescription;
    this.featureAnalyzer = new MolecularFeatureAnalyzer();
    this.conditionMatcher = new OrganicReactionConditionMatcher();
    this.stoichiometryAnalyzer = new ReactionStoichiometryAnalyzer();
  }

  public static NucleophilicAmineLeavingGroupReplacementRule heteroarylHalideAmination() {
    return new NucleophilicAmineLeavingGroupReplacementRule(
        OrganicReactionRuleId.of("replace_heteroaryl_halide_with_amine_fragment"),
        "Heteroaryl halide amination with amine fragment transfer",
        OrganicReactionRuleCategory.SUBSTITUTION,
        ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION,
        0.72,
        0.95,
        92.0,
        "Activated halogenated nitrogen heterocycle and amine nucleophile support SNAr amination.",
        "Generated C-N coupled heteroaryl amine plus hydrogen halide from reactant molecular graphs.",
        "Generated SNAr site combination",
        "SNAr amination candidate generated from scaffold and amine reactant graphs.",
        "amine_fragment_snar",
        "SNAr amination product");
  }

  public static NucleophilicAmineLeavingGroupReplacementRule anrorcAzaHeterocycleAmination() {
    return new NucleophilicAmineLeavingGroupReplacementRule(
        OrganicReactionRuleId.of("anrorc_aza_heterocycle_amine_fragment_transfer"),
        "ANRORC aza-heterocycle amination with amine fragment transfer",
        OrganicReactionRuleCategory.REARRANGEMENT,
        ReactionMechanismFamily.ADDITION_RING_OPENING_RING_CLOSURE,
        0.58,
        0.88,
        82.0,
        "Activated halogenated aza-heterocycle and amine nucleophile support an ANRORC competing channel.",
        "Generated C-N coupled aza-heterocycle amine plus hydrogen halide through an addition/ring-opening/ring-closure mechanism from reactant molecular graphs.",
        "Generated ANRORC site combination",
        "ANRORC amination candidate generated from scaffold and amine reactant graphs.",
        "amine_fragment_anrorc",
        "ANRORC amination product");
  }

  public OrganicReactionRuleId id() {
    return this.id;
  }

  public String displayName() {
    return this.displayName;
  }

  public OrganicReactionRuleCategory category() {
    return this.category;
  }

  public ReactionMechanismFamily mechanismFamily() {
    return this.mechanismFamily;
  }

  public OrganicReactionRuleMatch match(final ReactionPredictionRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction prediction request must not be null.");
    }
    if (!this.domainMatches(request)) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    final List<ScaffoldSite> scaffoldSites = this.scaffoldSites(request);
    if (scaffoldSites.isEmpty()) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    final List<AmineSite> amineSites = this.amineSites(request, scaffoldSites);
    if (amineSites.isEmpty()) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    final OrganicReactionConditionAssessment conditionAssessment =
        this.conditionMatcher.assess(
            request.conditions(),
            List.of(
                ReactionConditionFeatureKind.BASIC_MEDIUM,
                ReactionConditionFeatureKind.HEATING,
                ReactionConditionFeatureKind.ANHYDROUS));
    if (conditionAssessment.stronglyIncompatible()) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    return OrganicReactionRuleMatch.matched(
        Math.min(
            this.maxProbability,
            this.baseProbability - 0.10 + conditionAssessment.scoreAdjustment()),
        this.matchDescription);
  }

  public List<ReactionProductCandidate> generate(
      final ReactionPredictionRequest request, final OrganicReactionRuleMatch match) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction prediction request must not be null.");
    }
    if (match == null || !match.matched()) {
      return List.of();
    }
    if (!this.domainMatches(request)) {
      return List.of();
    }
    final List<ScaffoldSite> scaffoldSites = this.scaffoldSites(request);
    if (scaffoldSites.isEmpty()) {
      return List.of();
    }
    final List<AmineSite> amineSites = this.amineSites(request, scaffoldSites);
    if (amineSites.isEmpty()) {
      return List.of();
    }
    final OrganicReactionConditionAssessment conditionAssessment =
        this.conditionMatcher.assess(
            request.conditions(),
            List.of(
                ReactionConditionFeatureKind.BASIC_MEDIUM,
                ReactionConditionFeatureKind.HEATING,
                ReactionConditionFeatureKind.ANHYDROUS));
    final ReactionStoichiometryProfile stoichiometryProfile =
        this.stoichiometryAnalyzer.analyze(request);
    final ArrayList<ReactionProductCandidate> candidates =
        new ArrayList<ReactionProductCandidate>(scaffoldSites.size() * amineSites.size());
    for (int scaffoldIndex = 0; scaffoldIndex < scaffoldSites.size(); ++scaffoldIndex) {
      final ScaffoldSite scaffoldSite = scaffoldSites.get(scaffoldIndex);
      for (int amineIndex = 0; amineIndex < amineSites.size(); ++amineIndex) {
        final AmineSite amineSite = amineSites.get(amineIndex);
        final Molecule coupledProduct = this.coupledProduct(scaffoldSite, amineSite);
        final Molecule acidProduct = this.leavingGroupCoproduct(scaffoldSite, amineSite);
        final double scaffoldActivation =
            NucleophilicAmineLeavingGroupReplacementRule.scaffoldActivationAdjustment(
                scaffoldSite.molecule(), scaffoldSite.anchorId());
        final double leavingGroupAdjustment =
            NucleophilicAmineLeavingGroupReplacementRule.leavingGroupAdjustment(
                scaffoldSite.molecule(), scaffoldSite.leavingGroupId());
        final double amineNucleophilicityAdjustment =
            NucleophilicAmineLeavingGroupReplacementRule.amineNucleophilicityAdjustment(
                amineSite.molecule(), amineSite.nitrogenId());
        final double benzylicAmineAdjustment =
            NucleophilicAmineLeavingGroupReplacementRule.benzylicAmineAdjustment(
                amineSite.molecule(), amineSite.nitrogenId());
        final double stoichiometryAdjustment =
            NucleophilicAmineLeavingGroupReplacementRule.stoichiometryAdjustment(
                stoichiometryProfile, scaffoldSite.molecule(), amineSite.molecule());
        final double siteSelectivityMultiplier =
            Math.max(
                0.45,
                Math.min(
                    1.95,
                    1.0
                        + scaffoldActivation
                        + leavingGroupAdjustment
                        + amineNucleophilicityAdjustment
                        + benzylicAmineAdjustment
                        + stoichiometryAdjustment));
        final double probability =
            Math.min(
                this.maxProbability,
                Math.max(
                    0.05,
                    (this.baseProbability
                            + conditionAssessment.scoreAdjustment()
                            - 0.010 * amineIndex)
                        * siteSelectivityMultiplier));
        final ArrayList<ReactionPredictionEvidence> evidence =
            new ArrayList<ReactionPredictionEvidence>(6 + conditionAssessment.evidence().size());
        evidence.add(
            ReactionPredictionEvidence.of(
                ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
                "Detected carbon-bound halogen leaving group on an activated nitrogen heterocycle at "
                    + scaffoldSite.anchorId().value()
                    + ".",
                0.24));
        evidence.add(
            ReactionPredictionEvidence.of(
                ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
                (amineSite.removedHydrogenId() == null
                        ? "Detected implicit-H amine nucleophile at "
                        : "Detected N-H amine nucleophile at ")
                    + amineSite.nitrogenId().value()
                    + " and transferred the complete amine substituent fragment.",
                0.23));
        evidence.add(
            ReactionPredictionEvidence.of(
                ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN,
                this.mechanismDescription,
                0.18));
        evidence.add(
            ReactionPredictionEvidence.of(
                ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
                this.siteCombinationDescription
                    + " scaffoldSite="
                    + scaffoldIndex
                    + ", amineSite="
                    + amineIndex
                    + " without using an observed product lookup.",
                Math.max(-0.08, 0.03 + scaffoldActivation - 0.005 * amineIndex)));
        evidence.add(
            ReactionPredictionEvidence.of(
                ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
                "Site-selectivity multiplier="
                    + siteSelectivityMultiplier
                    + " for scaffold anchor "
                    + scaffoldSite.anchorId().value()
                    + " and leaving group "
                    + scaffoldSite.leavingGroupId().value()
                    + ".",
                Math.max(-0.18, Math.min(0.28, (siteSelectivityMultiplier - 1.0) * 0.18))));
        if (scaffoldActivation > 0.0) {
          evidence.add(
              ReactionPredictionEvidence.of(
                  ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
                  "Electron-poor heteroaryl site is activated by nearby heteroatom, charged or pi-accepting environment.",
                  Math.max(0.02, Math.min(0.18, scaffoldActivation))));
        }
        evidence.add(
            ReactionPredictionEvidence.of(
                ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
                "SNAr scaffold activation adjustment="
                    + scaffoldActivation
                    + " from local heteroatom/electron-withdrawing environment around "
                    + scaffoldSite.anchorId().value()
                    + ".",
                Math.max(-0.12, Math.min(0.18, scaffoldActivation))));
        evidence.add(
            ReactionPredictionEvidence.of(
                ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
                "Leaving-group adjustment="
                    + leavingGroupAdjustment
                    + " for "
                    + NucleophilicAmineLeavingGroupReplacementRule.atom(
                            scaffoldSite.molecule(), scaffoldSite.leavingGroupId())
                        .element()
                        .symbol()
                        .value()
                    + " at "
                    + scaffoldSite.leavingGroupId().value()
                    + ".",
                Math.max(-0.10, Math.min(0.10, leavingGroupAdjustment))));
        evidence.add(
            ReactionPredictionEvidence.of(
                ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
                "Amine nucleophilicity adjustment="
                    + amineNucleophilicityAdjustment
                    + " from N formal charge, heavy-atom degree, neighboring unsaturation and heteroatom withdrawal around "
                    + amineSite.nitrogenId().value()
                    + ".",
                Math.max(-0.12, Math.min(0.12, amineNucleophilicityAdjustment))));
        if (benzylicAmineAdjustment > 0.0) {
          evidence.add(
              ReactionPredictionEvidence.of(
                  ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
                  "Benzylic amine environment supports a soft neutral nucleophile for activated heteroaryl substitution without treating the product as a lookup.",
                  Math.min(0.06, benzylicAmineAdjustment)));
        }
        NucleophilicAmineLeavingGroupReplacementRule.addStoichiometryEvidence(
            evidence,
            stoichiometryProfile,
            scaffoldSite.molecule(),
            amineSite.molecule(),
            stoichiometryAdjustment);
        evidence.addAll(conditionAssessment.evidence());
        final ReactionSide products =
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(coupledProduct, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(acidProduct, StoichiometricCoefficient.ONE)));
        final ReactionMechanism detailedMechanism =
            this.detailedMechanism(
                request, products, scaffoldSite, amineSite, probability, scaffoldIndex, amineIndex);
        candidates.add(
            ReactionProductCandidate.of(
                products,
                probability,
                Math.min(this.expectedYieldLimit, probability * 100.0),
                this.candidateDescription,
                List.copyOf(evidence),
                List.of(
                    PredictedReactionMechanism.of(
                        request.id()
                            + "_"
                            + this.mechanismIdPrefix
                            + "_"
                            + scaffoldIndex
                            + "_"
                            + amineIndex,
                        this.mechanismFamily,
                        probability,
                        Math.min(this.expectedYieldLimit, probability * 100.0),
                        detailedMechanism))));
      }
    }
    return List.copyOf(candidates);
  }

  private ReactionMechanism detailedMechanism(
      final ReactionPredictionRequest request,
      final ReactionSide products,
      final ScaffoldSite scaffoldSite,
      final AmineSite amineSite,
      final double probability,
      final int scaffoldIndex,
      final int amineIndex) {
    final String mechanismId =
        request.id()
            + "_"
            + this.mechanismIdPrefix
            + "_"
            + scaffoldIndex
            + "_"
            + amineIndex
            + "_mechanism";
    final Reaction reaction =
        Reaction.of(
            ReactionId.of(mechanismId + "_overall"),
            this.displayName + " detailed elementary pathway",
            NucleophilicAmineLeavingGroupReplacementRule.participatingReactants(
                scaffoldSite, amineSite),
            products,
            request.conditions(),
            ChemistryMetadata.EMPTY);
    final List<MechanisticEvent> events =
        List.of(
            MechanisticEvent.of(
                "event_0",
                0,
                MechanisticEventKind.DIFFUSION_LIMITED_ENCOUNTER,
                "Amine nucleophile approaches the activated heteroaryl carbon bearing the leaving group.",
                List.of(scaffoldSite.anchorId().value(), amineSite.nitrogenId().value()),
                Math.min(1.0, probability + 0.08)),
            MechanisticEvent.of(
                "event_1",
                1,
                MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                "Nitrogen lone pair attacks the electron-poor heteroaryl carbon to form a sigma-adduct.",
                List.of(scaffoldSite.anchorId().value(), amineSite.nitrogenId().value()),
                Math.min(1.0, probability + 0.04)),
            MechanisticEvent.of(
                "event_2",
                2,
                MechanisticEventKind.SIGMA_COMPLEX_FORMATION,
                "Meisenheimer-like sigma complex is stabilized by heteroarene and electron-withdrawing substituent context.",
                List.of(scaffoldSite.anchorId().value()),
                Math.min(1.0, probability)),
            MechanisticEvent.of(
                "event_3",
                3,
                MechanisticEventKind.LEAVING_GROUP_DEPARTURE,
                "Carbon-halogen bond collapses with leaving-group departure and aromaticity recovery.",
                List.of(scaffoldSite.leavingGroupId().value()),
                Math.max(0.0, probability - 0.03)),
            MechanisticEvent.of(
                "event_4",
                4,
                MechanisticEventKind.DEPROTONATION,
                amineSite.removedHydrogenId() == null
                    ? "Implicit amine proton transfer gives the neutral coupled heteroaryl amine."
                    : "Transferred amine proton forms the hydrogen halide coproduct.",
                amineSite.removedHydrogenId() == null
                    ? List.of(amineSite.nitrogenId().value())
                    : List.of(
                        amineSite.nitrogenId().value(), amineSite.removedHydrogenId().value()),
                Math.max(0.0, probability - 0.02)));
    final ElementaryReactionStep step =
        ElementaryReactionStep.of("step_0", reaction, null, null, null, null, events);
    return ReactionMechanism.of(
        mechanismId, this.mechanismFamily, reaction, List.of(step), List.of());
  }

  private static ReactionSide participatingReactants(
      final ScaffoldSite scaffoldSite, final AmineSite amineSite) {
    return ReactionSide.of(
        List.of(
            ReactionParticipant.of(scaffoldSite.molecule(), StoichiometricCoefficient.ONE),
            ReactionParticipant.of(amineSite.molecule(), StoichiometricCoefficient.ONE)));
  }

  private static double stoichiometryAdjustment(
      final ReactionStoichiometryProfile profile,
      final Molecule scaffoldMolecule,
      final Molecule amineMolecule) {
    if (profile == null || !profile.hasMatchedQuantities()) {
      return 0.0;
    }
    final double scaffoldEquivalent =
        NucleophilicAmineLeavingGroupReplacementRule.equivalentFor(profile, scaffoldMolecule);
    final double amineEquivalent =
        NucleophilicAmineLeavingGroupReplacementRule.equivalentFor(profile, amineMolecule);
    double adjustment = 0.0;
    if (scaffoldEquivalent > 0.0 && amineEquivalent > 0.0) {
      final double ratio = amineEquivalent / scaffoldEquivalent;
      if (ratio >= 1.05 && ratio <= 2.80) {
        adjustment += Math.min(0.13, (ratio - 1.0) * 0.11);
      } else if (ratio < 0.85) {
        adjustment -= Math.min(0.12, (0.85 - ratio) * 0.16);
      } else if (ratio > 4.0) {
        adjustment -= Math.min(0.07, (ratio - 4.0) * 0.02);
      }
    }
    if (profile.unmatchedReactantCount() > 0) {
      adjustment -= Math.min(0.06, profile.unmatchedReactantCount() * 0.025);
    }
    if (profile.totalGramScale() >= 50.0) {
      adjustment -= 0.015;
    }
    return Math.max(-0.18, Math.min(0.16, adjustment));
  }

  private static void addStoichiometryEvidence(
      final List<ReactionPredictionEvidence> evidence,
      final ReactionStoichiometryProfile profile,
      final Molecule scaffoldMolecule,
      final Molecule amineMolecule,
      final double adjustment) {
    if (profile == null || !profile.hasMatchedQuantities()) {
      return;
    }
    final double scaffoldEquivalent =
        NucleophilicAmineLeavingGroupReplacementRule.equivalentFor(profile, scaffoldMolecule);
    final double amineEquivalent =
        NucleophilicAmineLeavingGroupReplacementRule.equivalentFor(profile, amineMolecule);
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.KINETIC_FACTOR,
            "Mass/mole recipe matched "
                + profile.matchedReactantCount()
                + " reactant components; scaffold equiv="
                + scaffoldEquivalent
                + ", amine equiv="
                + amineEquivalent
                + ", adjustment="
                + adjustment
                + ".",
            adjustment));
    if (scaffoldEquivalent > 0.0 && amineEquivalent > 0.0) {
      final double ratio = amineEquivalent / scaffoldEquivalent;
      if (ratio >= 1.05) {
        evidence.add(
            ReactionPredictionEvidence.of(
                ReactionPredictionEvidenceKind.THERMODYNAMIC_FACTOR,
                "Amine nucleophile is present in mass-derived excess relative to activated scaffold; conversion of the C-leaving-group channel is favored.",
                Math.min(0.08, (ratio - 1.0) * 0.06)));
      } else if (ratio < 0.85) {
        evidence.add(
            ReactionPredictionEvidence.of(
                ReactionPredictionEvidenceKind.COMPETING_PATHWAY,
                "Mass-derived amine loading is below scaffold equivalents, so unreacted scaffold and incomplete substitution channels remain plausible.",
                -Math.min(0.08, (0.85 - ratio) * 0.10)));
      }
    }
    if (profile.unmatchedReactantCount() > 0) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.WARNING,
              "Some quantified reactant components did not match molecular graph participants; selectivity is kept conservative.",
              -Math.min(0.05, profile.unmatchedReactantCount() * 0.02)));
    }
  }

  private static double equivalentFor(
      final ReactionStoichiometryProfile profile, final Molecule molecule) {
    final List<ReactionStoichiometryComponent> components = profile.matchedComponents();
    for (int i = 0; i < components.size(); ++i) {
      final ReactionStoichiometryComponent component = components.get(i);
      if (component.participant().molecule().equals(molecule)) {
        return component.equivalentRatio();
      }
    }
    return 0.0;
  }

  private List<ScaffoldSite> scaffoldSites(final ReactionPredictionRequest request) {
    final ArrayList<ScaffoldSite> sites = new ArrayList<ScaffoldSite>();
    final List<ReactionParticipant> participants = request.reactants().participants();
    for (int i = 0; i < participants.size(); ++i) {
      final Molecule molecule = participants.get(i).molecule();
      final MolecularFeatureSet features = this.featureAnalyzer.analyze(molecule);
      if (!features.contains(MolecularFeatureKind.HETEROCYCLE)
          || !features.contains(MolecularFeatureKind.NITROGEN_HETEROCYCLE)
          || !features.contains(MolecularFeatureKind.HALOGENATED)) {
        continue;
      }
      final List<Bond> bonds = molecule.bonds();
      for (int j = 0; j < bonds.size(); ++j) {
        final Bond bond = bonds.get(j);
        final Atom first =
            NucleophilicAmineLeavingGroupReplacementRule.atom(molecule, bond.firstAtomId());
        final Atom second =
            NucleophilicAmineLeavingGroupReplacementRule.atom(molecule, bond.secondAtomId());
        if (NucleophilicAmineLeavingGroupReplacementRule.isHalogenLeavingGroup(first)
            && NucleophilicAmineLeavingGroupReplacementRule.isElement(second, CARBON)) {
          sites.add(new ScaffoldSite(molecule, second.id(), first.id()));
        }
        if (NucleophilicAmineLeavingGroupReplacementRule.isHalogenLeavingGroup(second)
            && NucleophilicAmineLeavingGroupReplacementRule.isElement(first, CARBON)) {
          sites.add(new ScaffoldSite(molecule, first.id(), second.id()));
        }
      }
    }
    return List.copyOf(sites);
  }

  private List<AmineSite> amineSites(
      final ReactionPredictionRequest request, final List<ScaffoldSite> excludedScaffoldSites) {
    if (excludedScaffoldSites == null) {
      throw new IllegalArgumentException("Excluded scaffold site list must not be null.");
    }
    final ArrayList<AmineSite> sites = new ArrayList<AmineSite>();
    final ArrayList<AtomId> usedNitrogens = new ArrayList<AtomId>();
    final List<ReactionParticipant> participants = request.reactants().participants();
    for (int i = 0; i < participants.size(); ++i) {
      final Molecule molecule = participants.get(i).molecule();
      if (NucleophilicAmineLeavingGroupReplacementRule.isScaffoldMolecule(
          molecule, excludedScaffoldSites)) {
        continue;
      }
      final List<Bond> bonds = molecule.bonds();
      for (int j = 0; j < bonds.size(); ++j) {
        final Bond bond = bonds.get(j);
        final Atom first =
            NucleophilicAmineLeavingGroupReplacementRule.atom(molecule, bond.firstAtomId());
        final Atom second =
            NucleophilicAmineLeavingGroupReplacementRule.atom(molecule, bond.secondAtomId());
        if (NucleophilicAmineLeavingGroupReplacementRule.isElement(first, NITROGEN)
            && NucleophilicAmineLeavingGroupReplacementRule.isElement(second, HYDROGEN)) {
          NucleophilicAmineLeavingGroupReplacementRule.addAmineSite(
              sites, usedNitrogens, molecule, first.id(), second.id());
        }
        if (NucleophilicAmineLeavingGroupReplacementRule.isElement(second, NITROGEN)
            && NucleophilicAmineLeavingGroupReplacementRule.isElement(first, HYDROGEN)) {
          NucleophilicAmineLeavingGroupReplacementRule.addAmineSite(
              sites, usedNitrogens, molecule, second.id(), first.id());
        }
      }
      this.addImplicitHydrogenAmineSites(sites, usedNitrogens, molecule);
    }
    return List.copyOf(sites);
  }

  private void addImplicitHydrogenAmineSites(
      final List<AmineSite> sites, final List<AtomId> usedNitrogens, final Molecule molecule) {
    final MolecularFeatureSet features = this.featureAnalyzer.analyze(molecule);
    if (!features.contains(MolecularFeatureKind.PRIMARY_OR_SECONDARY_AMINE)
        && !features.contains(MolecularFeatureKind.ACTIVATED_NUCLEOPHILE)
        && !features.contains(MolecularFeatureKind.AMINE)) {
      return;
    }
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      if (!NucleophilicAmineLeavingGroupReplacementRule.isElement(atom, NITROGEN)) {
        continue;
      }
      if (usedNitrogens.contains(atom.id())) {
        continue;
      }
      if (atom.formalCharge().value() > 0) {
        continue;
      }
      final int heavyNeighborCount =
          NucleophilicAmineLeavingGroupReplacementRule.heavyNeighborCount(molecule, atom.id());
      if (heavyNeighborCount <= 0 || heavyNeighborCount >= 3) {
        continue;
      }
      NucleophilicAmineLeavingGroupReplacementRule.addAmineSite(
          sites, usedNitrogens, molecule, atom.id(), null);
    }
  }

  private static boolean isScaffoldMolecule(
      final Molecule molecule, final List<ScaffoldSite> scaffoldSites) {
    for (int i = 0; i < scaffoldSites.size(); ++i) {
      if (molecule.equals(scaffoldSites.get(i).molecule())) {
        return true;
      }
    }
    return false;
  }

  private static void addAmineSite(
      final List<AmineSite> sites,
      final List<AtomId> usedNitrogens,
      final Molecule molecule,
      final AtomId nitrogenId,
      final AtomId removedHydrogenId) {
    if (usedNitrogens.contains(nitrogenId)) {
      return;
    }
    usedNitrogens.add(nitrogenId);
    sites.add(new AmineSite(molecule, nitrogenId, removedHydrogenId));
  }

  private Molecule coupledProduct(final ScaffoldSite scaffoldSite, final AmineSite amineSite) {
    final String productId =
        scaffoldSite.molecule().id().value()
            + "_"
            + scaffoldSite.anchorId().value()
            + "_"
            + scaffoldSite.leavingGroupId().value()
            + "_"
            + amineSite.molecule().id().value()
            + "_"
            + amineSite.nitrogenId().value()
            + "_amine_coupled";
    final HashMap<AtomId, AtomId> scaffoldIdMap = new HashMap<AtomId, AtomId>();
    final HashMap<AtomId, AtomId> amineIdMap = new HashMap<AtomId, AtomId>();
    final ArrayList<Atom> atoms =
        new ArrayList<Atom>(
            scaffoldSite.molecule().atoms().size() + amineSite.molecule().atoms().size());
    this.copyAtoms(
        scaffoldSite.molecule(),
        "scaffold",
        scaffoldSite.leavingGroupId(),
        null,
        scaffoldIdMap,
        atoms);
    this.copyAtoms(
        amineSite.molecule(),
        "nucleophile",
        amineSite.removedHydrogenId(),
        null,
        amineIdMap,
        atoms);
    final ArrayList<Bond> bonds =
        new ArrayList<Bond>(
            scaffoldSite.molecule().bonds().size() + amineSite.molecule().bonds().size() + 1);
    this.copyBonds(scaffoldSite.molecule(), scaffoldSite.leavingGroupId(), scaffoldIdMap, bonds);
    this.copyBonds(amineSite.molecule(), amineSite.removedHydrogenId(), amineIdMap, bonds);
    bonds.add(
        Bond.of(
            scaffoldIdMap.get(scaffoldSite.anchorId()),
            amineIdMap.get(amineSite.nitrogenId()),
            BondType.SINGLE));
    return Molecule.of(
        MoleculeId.of(productId),
        scaffoldSite.molecule().displayName()
            + " "
            + amineSite.molecule().displayName()
            + " "
            + this.productDescription,
        atoms,
        bonds,
        MolecularCharge.of(
            scaffoldSite.molecule().charge().value() + amineSite.molecule().charge().value()),
        SpinMultiplicity.of(
            Math.max(
                scaffoldSite.molecule().spinMultiplicity().value(),
                amineSite.molecule().spinMultiplicity().value())),
        ChemistryMetadata.EMPTY);
  }

  private boolean domainMatches(final ReactionPredictionRequest request) {
    if (request.hasDomain(OrganicReactionDomain.UNKNOWN)
        || request.hasDomain(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION)) {
      return true;
    }
    if (this.mechanismFamily == ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION) {
      return request.hasDomain(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION);
    }
    return request.hasDomain(OrganicReactionDomain.REARRANGEMENT)
        || request.hasDomain(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION);
  }

  private void copyAtoms(
      final Molecule molecule,
      final String prefix,
      final AtomId removedAtomId,
      final AtomId unusedAtomId,
      final Map<AtomId, AtomId> idMap,
      final List<Atom> target) {
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      if (atom.id().equals(removedAtomId) || atom.id().equals(unusedAtomId)) {
        continue;
      }
      final AtomId copiedId = AtomId.of(prefix + "_" + atom.id().value());
      idMap.put(atom.id(), copiedId);
      target.add(
          Atom.of(
              copiedId,
              atom.element().symbol(),
              atom.coordinate(),
              atom.formalCharge(),
              atom.isotope(),
              atom.radicalState(),
              atom.metadata()));
    }
  }

  private void copyBonds(
      final Molecule molecule,
      final AtomId removedAtomId,
      final Map<AtomId, AtomId> idMap,
      final List<Bond> target) {
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (bond.firstAtomId().equals(removedAtomId) || bond.secondAtomId().equals(removedAtomId)) {
        continue;
      }
      target.add(
          Bond.of(idMap.get(bond.firstAtomId()), idMap.get(bond.secondAtomId()), bond.type()));
    }
  }

  private Molecule leavingGroupCoproduct(
      final ScaffoldSite scaffoldSite, final AmineSite amineSite) {
    final Atom leavingGroup =
        NucleophilicAmineLeavingGroupReplacementRule.atom(
            scaffoldSite.molecule(), scaffoldSite.leavingGroupId());
    final ElementSymbol halogen = leavingGroup.element().symbol();
    final String symbol = halogen.value().toLowerCase();
    if (amineSite.removedHydrogenId() == null) {
      final Atom x = Atom.of(AtomId.of(symbol + "_x"), halogen, null);
      return Molecule.of(
          MoleculeId.of(scaffoldSite.molecule().id().value() + "_leaving_" + symbol),
          "Leaving " + halogen.value(),
          List.of(x),
          List.of(),
          MolecularCharge.NEUTRAL,
          SpinMultiplicity.of(2));
    }
    final Atom h = Atom.of(AtomId.of("h" + symbol + "_h"), HYDROGEN, null);
    final Atom x = Atom.of(AtomId.of("h" + symbol + "_x"), halogen, null);
    return Molecule.of(
        MoleculeId.of(scaffoldSite.molecule().id().value() + "_leaving_h" + symbol),
        "Hydrogen " + halogen.value(),
        List.of(h, x),
        List.of(Bond.of(h.id(), x.id(), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static double scaffoldActivationAdjustment(
      final Molecule molecule, final AtomId anchorId) {
    double adjustment = 0.0;
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (!bond.firstAtomId().equals(anchorId) && !bond.secondAtomId().equals(anchorId)) {
        continue;
      }
      final Atom neighbor =
          NucleophilicAmineLeavingGroupReplacementRule.atom(
              molecule,
              bond.firstAtomId().equals(anchorId) ? bond.secondAtomId() : bond.firstAtomId());
      final String symbol = neighbor.element().symbol().value();
      if (NucleophilicAmineLeavingGroupReplacementRule.isHeteroAtom(symbol)) {
        adjustment += 0.105;
      }
      if (NucleophilicAmineLeavingGroupReplacementRule.isElectronWithdrawingAtom(symbol)) {
        adjustment += 0.055;
      }
      if (bond.type() == BondType.DOUBLE
          || bond.type() == BondType.TRIPLE
          || molecule.aromaticity().isAromaticBond(bond)) {
        adjustment += 0.025;
      }
      if (neighbor.formalCharge().value() > 0) {
        adjustment += 0.035;
      } else if (neighbor.formalCharge().value() < 0) {
        adjustment -= 0.030;
      }
      adjustment +=
          0.012
              * NucleophilicAmineLeavingGroupReplacementRule.secondSphereHeteroAtoms(
                  molecule, anchorId, neighbor.id());
    }
    return Math.max(-0.12, Math.min(0.26, adjustment));
  }

  private static int secondSphereHeteroAtoms(
      final Molecule molecule, final AtomId anchorId, final AtomId firstSphereAtomId) {
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
      if (secondSphereAtomId.equals(anchorId)) {
        continue;
      }
      final Atom atom =
          NucleophilicAmineLeavingGroupReplacementRule.atom(molecule, secondSphereAtomId);
      if (NucleophilicAmineLeavingGroupReplacementRule.isHeteroAtom(
          atom.element().symbol().value())) {
        ++count;
      }
    }
    return count;
  }

  private static Atom atom(final Molecule molecule, final AtomId atomId) {
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      if (atom.id().equals(atomId)) {
        return atom;
      }
    }
    throw new IllegalArgumentException("Molecule bond references unknown atom.");
  }

  private static boolean isElement(final Atom atom, final ElementSymbol symbol) {
    return atom.element().symbol().equals(symbol);
  }

  private static boolean isHalogenLeavingGroup(final Atom atom) {
    return NucleophilicAmineLeavingGroupReplacementRule.isElement(atom, FLUORINE)
        || NucleophilicAmineLeavingGroupReplacementRule.isElement(atom, CHLORINE)
        || NucleophilicAmineLeavingGroupReplacementRule.isElement(atom, BROMINE)
        || NucleophilicAmineLeavingGroupReplacementRule.isElement(atom, IODINE);
  }

  private static double leavingGroupAdjustment(
      final Molecule molecule, final AtomId leavingGroupId) {
    final Atom leavingGroup =
        NucleophilicAmineLeavingGroupReplacementRule.atom(molecule, leavingGroupId);
    if (NucleophilicAmineLeavingGroupReplacementRule.isElement(leavingGroup, FLUORINE)) {
      return 0.030;
    }
    if (NucleophilicAmineLeavingGroupReplacementRule.isElement(leavingGroup, CHLORINE)) {
      return 0.015;
    }
    if (NucleophilicAmineLeavingGroupReplacementRule.isElement(leavingGroup, BROMINE)) {
      return 0.005;
    }
    if (NucleophilicAmineLeavingGroupReplacementRule.isElement(leavingGroup, IODINE)) {
      return -0.010;
    }
    return 0.0;
  }

  private static double amineNucleophilicityAdjustment(
      final Molecule molecule, final AtomId nitrogenId) {
    final Atom nitrogen = NucleophilicAmineLeavingGroupReplacementRule.atom(molecule, nitrogenId);
    double adjustment = 0.0;
    if (nitrogen.formalCharge().value() < 0) {
      adjustment += 0.075;
    } else if (nitrogen.formalCharge().value() > 0) {
      adjustment -= 0.14;
    }
    int heavyNeighborCount = 0;
    int electronWithdrawingNeighborCount = 0;
    int unsaturatedNeighborCount = 0;
    int carbonNeighborCount = 0;
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (!bond.firstAtomId().equals(nitrogenId) && !bond.secondAtomId().equals(nitrogenId)) {
        continue;
      }
      final AtomId neighborId =
          bond.firstAtomId().equals(nitrogenId) ? bond.secondAtomId() : bond.firstAtomId();
      final Atom neighbor = NucleophilicAmineLeavingGroupReplacementRule.atom(molecule, neighborId);
      final String neighborSymbol = neighbor.element().symbol().value();
      if (!NucleophilicAmineLeavingGroupReplacementRule.isElement(neighbor, HYDROGEN)) {
        ++heavyNeighborCount;
      }
      if (NucleophilicAmineLeavingGroupReplacementRule.isElement(neighbor, CARBON)) {
        ++carbonNeighborCount;
      }
      if (bond.type() == BondType.DOUBLE
          || bond.type() == BondType.TRIPLE
          || molecule.aromaticity().isAromaticBond(bond)) {
        ++unsaturatedNeighborCount;
      }
      if (NucleophilicAmineLeavingGroupReplacementRule.isElectronWithdrawingAtom(neighborSymbol)
          && !NucleophilicAmineLeavingGroupReplacementRule.isElement(neighbor, HYDROGEN)) {
        ++electronWithdrawingNeighborCount;
      }
      if (NucleophilicAmineLeavingGroupReplacementRule.neighborHasPiAcceptorBond(
          molecule, nitrogenId, neighborId)) {
        ++electronWithdrawingNeighborCount;
      }
    }
    adjustment += Math.min(0.045, 0.018 * carbonNeighborCount);
    adjustment -= Math.min(0.080, 0.030 * Math.max(0, heavyNeighborCount - 2));
    adjustment -= Math.min(0.090, 0.040 * electronWithdrawingNeighborCount);
    adjustment -= Math.min(0.070, 0.035 * unsaturatedNeighborCount);
    return Math.max(-0.16, Math.min(0.12, adjustment));
  }

  private static double benzylicAmineAdjustment(final Molecule molecule, final AtomId nitrogenId) {
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (!bond.firstAtomId().equals(nitrogenId) && !bond.secondAtomId().equals(nitrogenId)) {
        continue;
      }
      final AtomId carbonId =
          bond.firstAtomId().equals(nitrogenId) ? bond.secondAtomId() : bond.firstAtomId();
      final Atom carbon = NucleophilicAmineLeavingGroupReplacementRule.atom(molecule, carbonId);
      if (!NucleophilicAmineLeavingGroupReplacementRule.isElement(carbon, CARBON)) {
        continue;
      }
      if (NucleophilicAmineLeavingGroupReplacementRule.hasAromaticCarbonNeighbor(
          molecule, carbonId, nitrogenId)) {
        return 0.030;
      }
    }
    return 0.0;
  }

  private static boolean hasAromaticCarbonNeighbor(
      final Molecule molecule, final AtomId carbonId, final AtomId excludedAtomId) {
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (!bond.firstAtomId().equals(carbonId) && !bond.secondAtomId().equals(carbonId)) {
        continue;
      }
      final AtomId neighborId =
          bond.firstAtomId().equals(carbonId) ? bond.secondAtomId() : bond.firstAtomId();
      if (neighborId.equals(excludedAtomId)) {
        continue;
      }
      if (!molecule.aromaticity().isAromaticBond(bond) && bond.type() != BondType.SINGLE) {
        continue;
      }
      final Atom neighbor = NucleophilicAmineLeavingGroupReplacementRule.atom(molecule, neighborId);
      if (NucleophilicAmineLeavingGroupReplacementRule.isElement(neighbor, CARBON)
          && NucleophilicAmineLeavingGroupReplacementRule.aromaticDegree(molecule, neighborId)
              >= 2) {
        return true;
      }
    }
    return false;
  }

  private static int aromaticDegree(final Molecule molecule, final AtomId atomId) {
    int degree = 0;
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if ((bond.firstAtomId().equals(atomId) || bond.secondAtomId().equals(atomId))
          && molecule.aromaticity().isAromaticBond(bond)) {
        ++degree;
      }
    }
    return degree;
  }

  private static int heavyNeighborCount(final Molecule molecule, final AtomId atomId) {
    int count = 0;
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (!bond.firstAtomId().equals(atomId) && !bond.secondAtomId().equals(atomId)) {
        continue;
      }
      final AtomId neighborId =
          bond.firstAtomId().equals(atomId) ? bond.secondAtomId() : bond.firstAtomId();
      if (!NucleophilicAmineLeavingGroupReplacementRule.isElement(
          NucleophilicAmineLeavingGroupReplacementRule.atom(molecule, neighborId), HYDROGEN)) {
        ++count;
      }
    }
    return count;
  }

  private static boolean neighborHasPiAcceptorBond(
      final Molecule molecule, final AtomId nitrogenId, final AtomId neighborId) {
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (!bond.firstAtomId().equals(neighborId) && !bond.secondAtomId().equals(neighborId)) {
        continue;
      }
      final AtomId otherId =
          bond.firstAtomId().equals(neighborId) ? bond.secondAtomId() : bond.firstAtomId();
      if (otherId.equals(nitrogenId)) {
        continue;
      }
      if (bond.type() != BondType.DOUBLE && bond.type() != BondType.TRIPLE) {
        continue;
      }
      final Atom other = NucleophilicAmineLeavingGroupReplacementRule.atom(molecule, otherId);
      final String symbol = other.element().symbol().value();
      if ("O".equals(symbol) || "N".equals(symbol) || "S".equals(symbol)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isHeteroAtom(final String symbol) {
    return "N".equals(symbol)
        || "O".equals(symbol)
        || "S".equals(symbol)
        || "P".equals(symbol)
        || "B".equals(symbol);
  }

  private static boolean isElectronWithdrawingAtom(final String symbol) {
    return "N".equals(symbol)
        || "O".equals(symbol)
        || "S".equals(symbol)
        || "F".equals(symbol)
        || "Cl".equals(symbol)
        || "Br".equals(symbol)
        || "I".equals(symbol);
  }

  private static final class ScaffoldSite {

    private final Molecule molecule;
    private final AtomId anchorId;
    private final AtomId leavingGroupId;

    private ScaffoldSite(
        final Molecule molecule, final AtomId anchorId, final AtomId leavingGroupId) {
      this.molecule = molecule;
      this.anchorId = anchorId;
      this.leavingGroupId = leavingGroupId;
    }

    private Molecule molecule() {
      return this.molecule;
    }

    private AtomId anchorId() {
      return this.anchorId;
    }

    private AtomId leavingGroupId() {
      return this.leavingGroupId;
    }
  }

  private static final class AmineSite {

    private final Molecule molecule;
    private final AtomId nitrogenId;
    private final AtomId removedHydrogenId;

    private AmineSite(
        final Molecule molecule, final AtomId nitrogenId, final AtomId removedHydrogenId) {
      this.molecule = molecule;
      this.nitrogenId = nitrogenId;
      this.removedHydrogenId = removedHydrogenId;
    }

    private Molecule molecule() {
      return this.molecule;
    }

    private AtomId nitrogenId() {
      return this.nitrogenId;
    }

    private AtomId removedHydrogenId() {
      return this.removedHydrogenId;
    }
  }
}

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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.reaction.ReactionKineticsModel;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffect;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffectAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffectProfile;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.feature.MolecularFeatureAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.reaction.analysis.ReactionPathwayAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.reaction.model.ReactionBondChangeSet;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.reaction.model.ReactionKineticEstimate;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.reaction.model.ReactionPathwayEstimate;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.stoichiometry.ReactionStoichiometryAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.stoichiometry.ReactionStoichiometryComponent;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.stoichiometry.ReactionStoichiometryProfile;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureSet;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism.ReactionMechanismPattern;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism.ReactionMechanismPatternAssessment;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism.ReactionMechanismPatternCatalog;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.BuiltInReactionMechanismPatternCatalog;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanisticEvent;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.PredictedReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionId;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.PeriodicTable;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Temperature;

/**
 * Детерминированная scoring-модель для ранжирования кандидатов органических продуктов.
 */
public final class OrganicReactionCandidateScorer {

  private static final double MIN_PROBABILITY = 0.01;
  private static final double ROOM_TEMPERATURE_KELVIN = 298.15;
  private static final double COOLING_TEMPERATURE_KELVIN = 283.15;
  private static final double HEATING_TEMPERATURE_KELVIN = 323.15;
  private static final double REFLUX_TEMPERATURE_KELVIN = 373.15;
  private static final double MATERIAL_BALANCE_SUPPORT = 0.10;
  private static final double MATERIAL_BALANCE_WEAK_SUPPORT = 0.035;
  private static final double PRODUCT_EXCESS_ATOM_PENALTY = -0.22;
  private static final double PRODUCT_EXCESS_ATOM_WITH_CONDITION_SOURCE_PENALTY = -0.045;
  private static final double STOICHIOMETRY_COMPLETE_SUPPORT = 0.055;
  private static final double STOICHIOMETRY_PARTIAL_SUPPORT = 0.022;
  private static final double STOICHIOMETRY_UNMATCHED_PENALTY = -0.045;
  private static final double STOICHIOMETRY_EXCESS_SUPPORT = 0.018;
  private static final double STOICHIOMETRY_EXCESS_PENALTY = -0.028;
  private static final double GRAM_SCALE_RECIPE_SUPPORT = 0.018;
  private static final double LARGE_SCALE_COMPLEXITY_PENALTY = -0.018;
  private final MolecularFeatureAnalyzer featureAnalyzer;
  private final ReactionConditionEffectAnalyzer conditionEffectAnalyzer;
  private final OrganicReactionConditionCompatibilityScorer conditionCompatibilityScorer;
  private final ReactionCandidatePlausibilityAnalyzer plausibilityAnalyzer;
  private final ReactionTopologyChangeScorer topologyChangeScorer;
  private final ReactionSubstrateReactivityAnalyzer substrateReactivityAnalyzer;
  private final ReactionMechanismPatternAssessor mechanismPatternAssessor;
  private final ReactionMechanismConditionAlignmentScorer mechanismConditionAlignmentScorer;
  private final ReactionMechanismPatternCatalog mechanismCatalog;
  private final ReactionPathwayAnalyzer pathwayAnalyzer;
  private final ReactionKineticsModel kineticsModel;
  private final ReactionStoichiometryAnalyzer stoichiometryAnalyzer;

  public OrganicReactionCandidateScorer() {
    this(new MolecularFeatureAnalyzer(), new ReactionConditionEffectAnalyzer());
  }

  public OrganicReactionCandidateScorer(final MolecularFeatureAnalyzer featureAnalyzer) {
    this(featureAnalyzer, new ReactionConditionEffectAnalyzer());
  }

  public OrganicReactionCandidateScorer(
      final MolecularFeatureAnalyzer featureAnalyzer,
      final ReactionConditionEffectAnalyzer conditionEffectAnalyzer) {
    this(
        featureAnalyzer,
        conditionEffectAnalyzer,
        BuiltInReactionMechanismPatternCatalog.organicAndPhysicalChemistry());
  }

  public OrganicReactionCandidateScorer(
      final MolecularFeatureAnalyzer featureAnalyzer,
      final ReactionConditionEffectAnalyzer conditionEffectAnalyzer,
      final ReactionMechanismPatternCatalog mechanismCatalog) {
    if (featureAnalyzer == null) {
      throw new IllegalArgumentException("Molecular feature analyzer must not be null.");
    }
    if (conditionEffectAnalyzer == null) {
      throw new IllegalArgumentException("Reaction condition effect analyzer must not be null.");
    }
    if (mechanismCatalog == null) {
      throw new IllegalArgumentException("Reaction mechanism pattern catalog must not be null.");
    }
    this.featureAnalyzer = featureAnalyzer;
    this.conditionEffectAnalyzer = conditionEffectAnalyzer;
    this.conditionCompatibilityScorer = new OrganicReactionConditionCompatibilityScorer();
    this.plausibilityAnalyzer = new ReactionCandidatePlausibilityAnalyzer(featureAnalyzer);
    this.topologyChangeScorer = new ReactionTopologyChangeScorer();
    this.substrateReactivityAnalyzer = new ReactionSubstrateReactivityAnalyzer();
    this.mechanismPatternAssessor = new ReactionMechanismPatternAssessor(featureAnalyzer);
    this.mechanismConditionAlignmentScorer = new ReactionMechanismConditionAlignmentScorer();
    this.mechanismCatalog = mechanismCatalog;
    this.pathwayAnalyzer = new ReactionPathwayAnalyzer();
    this.kineticsModel = new ReactionKineticsModel();
    this.stoichiometryAnalyzer = new ReactionStoichiometryAnalyzer();
  }

  public ReactionCandidateScore score(
      final ReactionPredictionRequest request,
      final ReactionProductCandidate candidate,
      final int sourceIndex) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction prediction request must not be null.");
    }
    if (candidate == null) {
      throw new IllegalArgumentException("Reaction product candidate must not be null.");
    }
    double score = MIN_PROBABILITY + candidate.probability();
    final ArrayList<ReactionPredictionEvidence> evidence =
        OrganicReactionCandidateScorer.predictionEvidence(candidate.evidence());
    for (int i = 0; i < evidence.size(); ++i) {
      score += OrganicReactionCandidateScorer.predictionWeight(evidence.get(i));
    }
    score += OrganicReactionCandidateScorer.userSeedPriorityWeight(request, sourceIndex, evidence);
    score += this.heterocycleDomainWeight(request, candidate, evidence);
    score += this.conditionWeight(request.conditions(), evidence);
    score += this.stoichiometryWeight(request, candidate, evidence);
    final ReactionConditionEffectProfile conditionEffectProfile =
        this.conditionEffectProfile(request, evidence);
    score += conditionEffectProfile.scoreAdjustment();
    score += this.mechanismPatternWeight(request, candidate, evidence);
    score += OrganicReactionCandidateScorer.predictedMechanismWeight(candidate, evidence);
    score +=
        OrganicReactionCandidateScorer.mechanismConditionAlignmentWeight(
            request, candidate, evidence);
    score +=
        this.mechanismConditionAlignmentScorer.score(request.conditions(), candidate, evidence);
    score += this.conditionCompatibilityScorer.score(candidate, conditionEffectProfile, evidence);
    score += this.functionalGroupWeight(candidate, evidence);
    score += this.functionalGroupContinuityWeight(request, candidate, evidence);
    score += this.competingFunctionalCenterPenalty(request, candidate, evidence);
    score += this.activatedHeteroarylAminationWeight(request, candidate, evidence);
    score += this.plausibilityAnalyzer.analyze(request, candidate, evidence);
    score += this.topologyChangeScorer.score(request, candidate, evidence);
    score += this.substrateReactivityAnalyzer.score(request, candidate, evidence);
    final PathwayScoringResult pathwayScoring =
        this.pathwayEnergeticsWeight(request, candidate, sourceIndex, evidence);
    score += pathwayScoring.weight();
    score +=
        OrganicReactionCandidateScorer.heavyAtomConservationWeight(request, candidate, evidence);
    score +=
        OrganicReactionCandidateScorer.leavingGroupConsumptionWeight(request, candidate, evidence);
    return ReactionCandidateScore.of(
        pathwayScoring.withDetailedMechanism(candidate),
        evidence,
        Math.max(MIN_PROBABILITY, score) - sourceIndex * 1.0E-12,
        sourceIndex);
  }

  public static double predictionWeight(final ReactionPredictionEvidence evidence) {
    if (evidence == null) {
      throw new IllegalArgumentException("Reaction prediction evidence must not be null.");
    }
    if (evidence.kind() == ReactionPredictionEvidenceKind.CALIBRATION_PRIOR) {
      return 0.0;
    }
    return evidence.weight();
  }

  private static ArrayList<ReactionPredictionEvidence> predictionEvidence(
      final List<ReactionPredictionEvidence> evidence) {
    final ArrayList<ReactionPredictionEvidence> result =
        new ArrayList<ReactionPredictionEvidence>(evidence.size());
    for (int i = 0; i < evidence.size(); ++i) {
      final ReactionPredictionEvidence item = evidence.get(i);
      if (item.kind() != ReactionPredictionEvidenceKind.CALIBRATION_PRIOR) {
        result.add(item);
      }
    }
    return result;
  }

  private PathwayScoringResult pathwayEnergeticsWeight(
      final ReactionPredictionRequest request,
      final ReactionProductCandidate candidate,
      final int sourceIndex,
      final List<ReactionPredictionEvidence> evidence) {
    final Reaction reaction =
        Reaction.of(
            ReactionId.of(request.id() + "_candidate_" + sourceIndex),
            "Predicted candidate pathway " + sourceIndex,
            request.reactants(),
            candidate.products(),
            request.conditions(),
            ChemistryMetadata.EMPTY);
    final ReactionPathwayEstimate estimate = this.pathwayAnalyzer.analyze(reaction);
    final ReactionKineticEstimate kineticEstimate =
        this.kineticsModel.estimate(estimate, request.conditions());
    final double thermodynamicWeight =
        OrganicReactionCandidateScorer.thermodynamicPathwayWeight(estimate.reactionEnergy());
    final double kineticWeight =
        OrganicReactionCandidateScorer.kineticPathwayWeight(estimate.forwardBarrier());
    final double contextualKineticWeight =
        OrganicReactionCandidateScorer.addContextualKineticEvidence(kineticEstimate, evidence);
    final double mechanisticWeight =
        OrganicReactionCandidateScorer.addMechanisticPathwayEvidence(estimate, evidence);
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.THERMODYNAMIC_FACTOR,
            "Candidate pathway reaction energy="
                + estimate.reactionEnergy()
                + " kJ/mol from Java-native bond-change pathway model.",
            thermodynamicWeight));
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.KINETIC_FACTOR,
            "Candidate pathway forward barrier="
                + estimate.forwardBarrier()
                + " kJ/mol, reverse barrier="
                + estimate.reverseBarrier()
                + " kJ/mol, band="
                + estimate.pathwayBand()
                + ".",
            kineticWeight));
    if (estimate.hasMechanism()) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.USER_RULE,
              "Candidate pathway has staged mechanism "
                  + estimate.mechanism().family().name()
                  + " with "
                  + estimate.mechanism().steps().size()
                  + " elementary steps.",
              0.04));
      return PathwayScoringResult.of(
          thermodynamicWeight + kineticWeight + contextualKineticWeight + mechanisticWeight + 0.04,
          estimate.mechanism());
    }
    return PathwayScoringResult.of(
        thermodynamicWeight + kineticWeight + contextualKineticWeight + mechanisticWeight, null);
  }

  private static double leavingGroupConsumptionWeight(
      final ReactionPredictionRequest request,
      final ReactionProductCandidate candidate,
      final List<ReactionPredictionEvidence> evidence) {
    if (!request.hasDomain(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION)
        && !request.hasDomain(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION)) {
      return 0.0;
    }
    final int reactantCarbonHalogenBonds =
        OrganicReactionCandidateScorer.carbonHalogenBondCount(request.reactants().participants());
    if (reactantCarbonHalogenBonds == 0) {
      return 0.0;
    }
    final int productCarbonHalogenBonds =
        OrganicReactionCandidateScorer.carbonHalogenBondCount(candidate.products().participants());
    if (productCarbonHalogenBonds < reactantCarbonHalogenBonds) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN,
              "Candidate consumes an activated carbon-halogen leaving group.",
              0.24));
      return 0.24;
    }
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.COMPETING_PATHWAY,
            "Candidate leaves the activated carbon-halogen bond unconsumed under substitution conditions.",
            -0.32));
    return -0.32;
  }

  private static double heavyAtomConservationWeight(
      final ReactionPredictionRequest request,
      final ReactionProductCandidate candidate,
      final List<ReactionPredictionEvidence> evidence) {
    final Map<String, Integer> reactantAtoms =
        OrganicReactionCandidateScorer.heavyAtomCounts(request.reactants().participants());
    if (reactantAtoms.isEmpty()) {
      return 0.0;
    }
    final Map<String, Integer> productAtoms =
        OrganicReactionCandidateScorer.heavyAtomCounts(candidate.products().participants());
    final int reactantTotal = OrganicReactionCandidateScorer.totalCount(reactantAtoms);
    final int explainedTotal =
        OrganicReactionCandidateScorer.explainedCount(reactantAtoms, productAtoms);
    final double ratio = (double) explainedTotal / (double) reactantTotal;
    final int productExcessAtomCount =
        OrganicReactionCandidateScorer.productExcessAtomCount(reactantAtoms, productAtoms);
    if (productExcessAtomCount > 0) {
      final boolean conditionSuppliesAtoms =
          OrganicReactionCandidateScorer.conditionSuppliesProductExcessAtoms(
              request.conditions(), reactantAtoms, productAtoms);
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.WARNING,
              conditionSuppliesAtoms
                  ? "Product candidate contains non-hydrogen atoms beyond reactants, but condition components provide a plausible material source."
                  : "Product candidate creates non-hydrogen atoms not present in reactants or declared condition components.",
              (conditionSuppliesAtoms
                      ? PRODUCT_EXCESS_ATOM_WITH_CONDITION_SOURCE_PENALTY
                      : PRODUCT_EXCESS_ATOM_PENALTY)
                  * Math.min(3, productExcessAtomCount)));
      return (conditionSuppliesAtoms
              ? PRODUCT_EXCESS_ATOM_WITH_CONDITION_SOURCE_PENALTY
              : PRODUCT_EXCESS_ATOM_PENALTY)
          * Math.min(3, productExcessAtomCount);
    }
    final boolean userSeed = OrganicReactionCandidateScorer.hasUserDeclaredSeedEvidence(evidence);
    if (ratio >= 0.98) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
              "Product candidate conserves nearly all non-hydrogen atoms from reactants.",
              MATERIAL_BALANCE_SUPPORT));
      return MATERIAL_BALANCE_SUPPORT;
    }
    if (ratio >= 0.90) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
              "Product candidate conserves most non-hydrogen atoms from reactants.",
              MATERIAL_BALANCE_WEAK_SUPPORT));
      return MATERIAL_BALANCE_WEAK_SUPPORT;
    }
    if (ratio < 0.75) {
      if (userSeed) {
        evidence.add(
            ReactionPredictionEvidence.of(
                ReactionPredictionEvidenceKind.WARNING,
                "User-declared product seed leaves non-hydrogen atoms unexplained; treated as a main-product channel with omitted coproducts.",
                -0.04));
        return -0.04;
      }
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.WARNING,
              "Product candidate leaves a large non-hydrogen reactant fragment unexplained.",
              -0.42));
      return -0.42;
    }
    if (userSeed) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.WARNING,
              "User-declared product seed leaves part of the reactant framework unexplained; treated as an incomplete product-side listing.",
              -0.02));
      return -0.02;
    }
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.WARNING,
            "Product candidate leaves part of the non-hydrogen reactant framework unexplained.",
            -0.18));
    return -0.18;
  }

  private static boolean hasUserDeclaredSeedEvidence(
      final List<ReactionPredictionEvidence> evidence) {
    for (int i = 0; i < evidence.size(); ++i) {
      final ReactionPredictionEvidence item = evidence.get(i);
      if (item.kind() == ReactionPredictionEvidenceKind.USER_RULE
          && item.description().toLowerCase(java.util.Locale.ROOT).contains("product seed")) {
        return true;
      }
    }
    return false;
  }

  private static Map<String, Integer> heavyAtomCounts(
      final List<ReactionParticipant> participants) {
    final HashMap<String, Integer> counts = new HashMap<String, Integer>();
    for (int i = 0; i < participants.size(); ++i) {
      final int coefficient = participants.get(i).coefficient().value();
      final Molecule molecule = participants.get(i).molecule();
      final List<Atom> atoms = molecule.atoms();
      for (int j = 0; j < atoms.size(); ++j) {
        final String symbol = atoms.get(j).element().symbol().value();
        if ("H".equals(symbol)) {
          continue;
        }
        counts.put(symbol, counts.getOrDefault(symbol, 0) + coefficient);
      }
    }
    return Map.copyOf(counts);
  }

  private static int productExcessAtomCount(
      final Map<String, Integer> reactantAtoms, final Map<String, Integer> productAtoms) {
    int excessCount = 0;
    for (final Map.Entry<String, Integer> entry : productAtoms.entrySet()) {
      final int available = reactantAtoms.getOrDefault(entry.getKey(), 0);
      if (entry.getValue() > available) {
        excessCount += entry.getValue() - available;
      }
    }
    return excessCount;
  }

  private static boolean conditionSuppliesProductExcessAtoms(
      final ReactionConditions conditions,
      final Map<String, Integer> reactantAtoms,
      final Map<String, Integer> productAtoms) {
    final Map<String, Integer> conditionAtoms =
        OrganicReactionCandidateScorer.conditionSuppliedHeavyAtomCounts(conditions);
    for (final Map.Entry<String, Integer> entry : productAtoms.entrySet()) {
      if (entry.getValue() > reactantAtoms.getOrDefault(entry.getKey(), 0)
          && conditionAtoms.getOrDefault(entry.getKey(), 0) > 0) {
        return true;
      }
    }
    return false;
  }

  private static Map<String, Integer> conditionSuppliedHeavyAtomCounts(
      final ReactionConditions conditions) {
    if (conditions == null || conditions.empty()) {
      return Map.of();
    }
    final HashMap<String, Integer> counts = new HashMap<String, Integer>();
    final List<ReactionConditionComponent> components = conditions.components();
    for (int i = 0; i < components.size(); ++i) {
      final ReactionConditionComponent component = components.get(i);
      if (component.role() == ReactionComponentRole.CATALYST
          || component.role() == ReactionComponentRole.LIGAND
          || component.role() == ReactionComponentRole.ATMOSPHERE) {
        continue;
      }
      OrganicReactionCandidateScorer.addConditionElementHints(counts, component.name());
      if (component.hasNote()) {
        OrganicReactionCandidateScorer.addConditionElementHints(counts, component.note());
      }
    }
    return Map.copyOf(counts);
  }

  private static void addConditionElementHints(
      final HashMap<String, Integer> counts, final String text) {
    OrganicReactionCandidateScorer.addFormulaElementHints(counts, text);
    final String normalized = OrganicReactionCandidateScorer.normalizedConditionText(text);
    OrganicReactionCandidateScorer.addElementHint(
        counts,
        normalized,
        "O",
        "water",
        "h2o",
        "oxygen",
        "oxide",
        "hydroxy",
        "hydroxide",
        "alcohol",
        "peroxide",
        "carbonate",
        "dmso",
        "peg");
    OrganicReactionCandidateScorer.addElementHint(
        counts,
        normalized,
        "N",
        "amine",
        "ammonia",
        "nitrogen",
        "azide",
        "nitrite",
        "nitrate",
        "hydrazine",
        "cyanide",
        "nitrile");
    OrganicReactionCandidateScorer.addElementHint(
        counts,
        normalized,
        "S",
        "sulfur",
        "sulphur",
        "thiol",
        "thio",
        "sulfide",
        "sulfoxide",
        "sulfone",
        "dmso");
    OrganicReactionCandidateScorer.addElementHint(
        counts, normalized, "Cl", "chloride", "chlorine", "hcl");
    OrganicReactionCandidateScorer.addElementHint(
        counts, normalized, "Br", "bromide", "bromine", "hbr");
    OrganicReactionCandidateScorer.addElementHint(
        counts, normalized, "I", "iodide", "iodine", "hi");
    OrganicReactionCandidateScorer.addElementHint(
        counts, normalized, "F", "fluoride", "fluorine", "hf");
    OrganicReactionCandidateScorer.addElementHint(
        counts, normalized, "B", "boronic", "boronate", "borane", "diboron");
    OrganicReactionCandidateScorer.addElementHint(
        counts, normalized, "P", "phosphine", "phosphate", "phosphorus");
    OrganicReactionCandidateScorer.addElementHint(
        counts, normalized, "Si", "silane", "silyl", "silicon");
  }

  private static void addFormulaElementHints(
      final HashMap<String, Integer> counts, final String text) {
    int index = 0;
    while (index < text.length()) {
      final int tokenStart = OrganicReactionCandidateScorer.nextFormulaTokenStart(text, index);
      if (tokenStart < 0) {
        return;
      }
      final int tokenEnd = OrganicReactionCandidateScorer.formulaTokenEnd(text, tokenStart);
      OrganicReactionCandidateScorer.addFormulaTokenElementHints(
          counts, text.substring(tokenStart, tokenEnd));
      index = tokenEnd;
    }
  }

  private static int nextFormulaTokenStart(final String text, final int fromIndex) {
    for (int i = fromIndex; i < text.length(); ++i) {
      if (Character.isUpperCase(text.charAt(i))) {
        return i;
      }
    }
    return -1;
  }

  private static int formulaTokenEnd(final String text, final int tokenStart) {
    int index = tokenStart;
    while (index < text.length()) {
      final char current = text.charAt(index);
      if (Character.isLetterOrDigit(current) || current == '(' || current == ')') {
        ++index;
        continue;
      }
      break;
    }
    return index;
  }

  private static void addFormulaTokenElementHints(
      final HashMap<String, Integer> counts, final String token) {
    if (token.length() < 2 || !OrganicReactionCandidateScorer.looksLikeFormulaToken(token)) {
      return;
    }
    final HashMap<String, Integer> parsedCounts = new HashMap<String, Integer>();
    final int parsedEnd =
        OrganicReactionCandidateScorer.parseFormulaGroup(token, 0, parsedCounts, false);
    if (parsedEnd != token.length() || parsedCounts.isEmpty()) {
      return;
    }
    for (final Map.Entry<String, Integer> entry : parsedCounts.entrySet()) {
      if (!"H".equals(entry.getKey())) {
        counts.put(entry.getKey(), counts.getOrDefault(entry.getKey(), 0) + entry.getValue());
      }
    }
  }

  private static boolean looksLikeFormulaToken(final String token) {
    boolean hasKnownElement = false;
    boolean hasDigitOrGroup = false;
    for (int i = 0; i < token.length(); ++i) {
      final char current = token.charAt(i);
      hasDigitOrGroup |= Character.isDigit(current) || current == '(' || current == ')';
      if (!Character.isUpperCase(current)) {
        continue;
      }
      final String oneLetter = String.valueOf(current);
      if (PeriodicTable.contains(ElementSymbol.of(oneLetter))) {
        hasKnownElement = true;
      }
      if (i + 1 < token.length() && Character.isLowerCase(token.charAt(i + 1))) {
        final String twoLetter = token.substring(i, i + 2);
        if (PeriodicTable.contains(ElementSymbol.of(twoLetter))) {
          hasKnownElement = true;
        }
      }
    }
    return hasKnownElement && hasDigitOrGroup;
  }

  private static int parseFormulaGroup(
      final String token,
      final int start,
      final HashMap<String, Integer> counts,
      final boolean stopAtClosingParenthesis) {
    int index = start;
    while (index < token.length()) {
      final char current = token.charAt(index);
      if (current == ')') {
        return stopAtClosingParenthesis ? index + 1 : -1;
      }
      if (current == '(') {
        final HashMap<String, Integer> groupCounts = new HashMap<String, Integer>();
        final int groupEnd =
            OrganicReactionCandidateScorer.parseFormulaGroup(token, index + 1, groupCounts, true);
        if (groupEnd < 0 || groupCounts.isEmpty()) {
          return -1;
        }
        final FormulaNumber multiplier =
            OrganicReactionCandidateScorer.parseFormulaNumber(token, groupEnd);
        OrganicReactionCandidateScorer.mergeFormulaCounts(counts, groupCounts, multiplier.value());
        index = multiplier.nextIndex();
        continue;
      }
      if (!Character.isUpperCase(current)) {
        return -1;
      }
      final ElementParseResult element =
          OrganicReactionCandidateScorer.parseElementSymbol(token, index);
      if (!element.valid()) {
        return -1;
      }
      final FormulaNumber count =
          OrganicReactionCandidateScorer.parseFormulaNumber(token, element.nextIndex());
      counts.put(element.symbol(), counts.getOrDefault(element.symbol(), 0) + count.value());
      index = count.nextIndex();
    }
    return stopAtClosingParenthesis ? -1 : index;
  }

  private static ElementParseResult parseElementSymbol(final String token, final int index) {
    if (index + 1 < token.length() && Character.isLowerCase(token.charAt(index + 1))) {
      final String twoLetter = token.substring(index, index + 2);
      if (PeriodicTable.contains(ElementSymbol.of(twoLetter))) {
        return ElementParseResult.valid(twoLetter, index + 2);
      }
    }
    final String oneLetter = token.substring(index, index + 1);
    if (PeriodicTable.contains(ElementSymbol.of(oneLetter))) {
      return ElementParseResult.valid(oneLetter, index + 1);
    }
    return ElementParseResult.INVALID;
  }

  private static FormulaNumber parseFormulaNumber(final String token, final int start) {
    int index = start;
    int value = 0;
    while (index < token.length() && Character.isDigit(token.charAt(index))) {
      value = Math.addExact(Math.multiplyExact(value, 10), token.charAt(index) - '0');
      ++index;
    }
    return new FormulaNumber(value == 0 ? 1 : value, index);
  }

  private static void mergeFormulaCounts(
      final HashMap<String, Integer> counts,
      final HashMap<String, Integer> groupCounts,
      final int multiplier) {
    for (final Map.Entry<String, Integer> entry : groupCounts.entrySet()) {
      counts.put(
          entry.getKey(),
          counts.getOrDefault(entry.getKey(), 0)
              + Math.multiplyExact(entry.getValue(), multiplier));
    }
  }

  private static String normalizedConditionText(final String text) {
    return " "
        + text.toLowerCase(Locale.ROOT)
            .replace('(', ' ')
            .replace(')', ' ')
            .replace('[', ' ')
            .replace(']', ' ')
            .replace('{', ' ')
            .replace('}', ' ')
            .replace(',', ' ')
            .replace(';', ' ')
            .replace(':', ' ')
            .replace('/', ' ')
            .replace('\\', ' ')
            .replace('-', ' ')
            .replace('_', ' ')
            .replace('+', ' ')
            .replace('.', ' ')
        + " ";
  }

  private static void addElementHint(
      final HashMap<String, Integer> counts,
      final String text,
      final String symbol,
      final String... hints) {
    for (int i = 0; i < hints.length; ++i) {
      if (text.contains(" " + hints[i] + " ")) {
        counts.put(symbol, counts.getOrDefault(symbol, 0) + 1);
        return;
      }
    }
  }

  private static int explainedCount(
      final Map<String, Integer> reactantAtoms, final Map<String, Integer> productAtoms) {
    int count = 0;
    for (final Map.Entry<String, Integer> entry : reactantAtoms.entrySet()) {
      count += Math.min(entry.getValue(), productAtoms.getOrDefault(entry.getKey(), 0));
    }
    return count;
  }

  private static int totalCount(final Map<String, Integer> counts) {
    int total = 0;
    for (final int count : counts.values()) {
      total += count;
    }
    return total;
  }

  private static int carbonHalogenBondCount(final List<ReactionParticipant> participants) {
    int count = 0;
    for (int i = 0; i < participants.size(); ++i) {
      count +=
          OrganicReactionCandidateScorer.carbonHalogenBondCount(participants.get(i).molecule());
    }
    return count;
  }

  private static int carbonHalogenBondCount(final Molecule molecule) {
    final HashMap<AtomId, String> symbols = new HashMap<AtomId, String>();
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      symbols.put(atoms.get(i).id(), atoms.get(i).element().symbol().value());
    }
    int count = 0;
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      final String first = symbols.get(bond.firstAtomId());
      final String second = symbols.get(bond.secondAtomId());
      if (OrganicReactionCandidateScorer.isCarbonHalogenPair(first, second)) {
        ++count;
      }
    }
    return count;
  }

  private static boolean isCarbonHalogenPair(final String first, final String second) {
    return "C".equals(first) && OrganicReactionCandidateScorer.isHalogen(second)
        || "C".equals(second) && OrganicReactionCandidateScorer.isHalogen(first);
  }

  private static boolean isHalogen(final String symbol) {
    return "F".equals(symbol) || "Cl".equals(symbol) || "Br".equals(symbol) || "I".equals(symbol);
  }

  private static double addMechanisticPathwayEvidence(
      final ReactionPathwayEstimate estimate, final List<ReactionPredictionEvidence> evidence) {
    double weight = 0.0;
    final ReactionBondChangeSet bondChanges = estimate.bondChanges();
    final int totalBondEvents = bondChanges.brokenBondEvents() + bondChanges.formedBondEvents();
    final double structuralWeight =
        OrganicReactionCandidateScorer.structuralPathwayWeight(totalBondEvents);
    final ReactionPredictionEvidenceKind structuralEvidenceKind =
        totalBondEvents == 0
            ? ReactionPredictionEvidenceKind.WARNING
            : ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR;
    evidence.add(
        ReactionPredictionEvidence.of(
            structuralEvidenceKind,
            "Candidate pathway changes "
                + totalBondEvents
                + " bond event(s): broken="
                + bondChanges.brokenBondEvents()
                + ", formed="
                + bondChanges.formedBondEvents()
                + ", broken-energy="
                + bondChanges.brokenBondEnergy()
                + " kJ/mol, formed-energy="
                + bondChanges.formedBondEnergy()
                + " kJ/mol.",
            structuralWeight));
    weight += structuralWeight;
    final double conditionWeight =
        OrganicReactionCandidateScorer.conditionActivationPathwayWeight(
            estimate.conditionActivationShift());
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.KINETIC_FACTOR,
            "Pathway thermal model: T="
                + estimate.temperatureKelvin()
                + " K, thermal energy="
                + estimate.thermalEnergy()
                + " kJ/mol, condition activation shift="
                + estimate.conditionActivationShift()
                + " kJ/mol.",
            conditionWeight));
    weight += conditionWeight;
    final double rateRatioWeight =
        OrganicReactionCandidateScorer.forwardReverseRateRatioWeight(
            estimate.forwardReverseRateRatio());
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.COMPETING_PATHWAY,
            "Forward/reverse rate ratio="
                + estimate.forwardReverseRateRatio()
                + " from Java-native Eyring-like pathway screening.",
            rateRatioWeight));
    weight += rateRatioWeight;
    if (!estimate.hasMechanism()) {
      return weight;
    }
    final ReactionMechanism mechanism = estimate.mechanism();
    weight += OrganicReactionCandidateScorer.addMechanismEventEvidence(mechanism, evidence);
    return weight;
  }

  private static double structuralPathwayWeight(final int totalBondEvents) {
    if (totalBondEvents <= 0) {
      return -1.00;
    }
    if (totalBondEvents <= 2) {
      return 0.04;
    }
    if (totalBondEvents <= 5) {
      return 0.02;
    }
    return -0.03;
  }

  private static double conditionActivationPathwayWeight(final double activationShift) {
    if (activationShift >= 20.0) {
      return 0.05;
    }
    if (activationShift >= 8.0) {
      return 0.03;
    }
    if (activationShift > 0.0) {
      return 0.015;
    }
    if (activationShift < 0.0) {
      return -0.015;
    }
    return 0.0;
  }

  private static double forwardReverseRateRatioWeight(final double ratio) {
    if (ratio >= 100.0) {
      return 0.04;
    }
    if (ratio >= 3.0) {
      return 0.02;
    }
    if (ratio >= 0.33) {
      return 0.0;
    }
    if (ratio >= 0.01) {
      return -0.03;
    }
    return -0.06;
  }

  private static double addMechanismEventEvidence(
      final ReactionMechanism mechanism, final List<ReactionPredictionEvidence> evidence) {
    double weight = 0.0;
    for (int stepIndex = 0; stepIndex < mechanism.steps().size(); ++stepIndex) {
      final List<MechanisticEvent> events = mechanism.steps().get(stepIndex).events();
      for (int eventIndex = 0; eventIndex < events.size(); ++eventIndex) {
        final MechanisticEvent event = events.get(eventIndex);
        final double eventWeight =
            OrganicReactionCandidateScorer.mechanisticEventWeight(event.probabilityWeight());
        evidence.add(
            ReactionPredictionEvidence.of(
                ReactionPredictionEvidenceKind.USER_RULE,
                "Mechanism "
                    + mechanism.family().name()
                    + " step "
                    + stepIndex
                    + " event "
                    + event.orderIndex()
                    + " "
                    + event.kind().name()
                    + ": "
                    + event.description(),
                eventWeight));
        weight += eventWeight;
      }
    }
    return weight;
  }

  private static double mechanisticEventWeight(final double confidence) {
    if (confidence >= 0.85) {
      return 0.025;
    }
    if (confidence >= 0.70) {
      return 0.015;
    }
    return 0.005;
  }

  private static double thermodynamicPathwayWeight(final double reactionEnergyKilojoulePerMole) {
    if (reactionEnergyKilojoulePerMole <= -80.0) {
      return 0.12;
    }
    if (reactionEnergyKilojoulePerMole <= -25.0) {
      return 0.08;
    }
    if (reactionEnergyKilojoulePerMole <= 25.0) {
      return 0.03;
    }
    if (reactionEnergyKilojoulePerMole <= 80.0) {
      return -0.04;
    }
    return -0.10;
  }

  private static double kineticPathwayWeight(final double forwardBarrierKilojoulePerMole) {
    if (forwardBarrierKilojoulePerMole <= 35.0) {
      return 0.10;
    }
    if (forwardBarrierKilojoulePerMole <= 70.0) {
      return 0.06;
    }
    if (forwardBarrierKilojoulePerMole <= 110.0) {
      return 0.01;
    }
    if (forwardBarrierKilojoulePerMole <= 160.0) {
      return -0.04;
    }
    return -0.09;
  }

  private static double addContextualKineticEvidence(
      final ReactionKineticEstimate estimate, final List<ReactionPredictionEvidence> evidence) {
    final double weight =
        estimate.hasDurationConversion()
            ? OrganicReactionCandidateScorer.durationConversionWeight(
                estimate.durationConversionFraction())
            : OrganicReactionCandidateScorer.firstOrderHalfLifeWeight(
                estimate.firstOrderHalfLifeSeconds());
    final StringBuilder description = new StringBuilder(192);
    description.append("Contextual kinetic model ");
    description.append(estimate.kineticModel());
    description.append(": Arrhenius k=");
    description.append(estimate.arrheniusForwardRateConstant());
    description.append(' ');
    description.append(estimate.arrheniusUnit());
    description.append(", Eyring k=");
    description.append(estimate.eyringForwardRateConstant());
    description.append(", half-life=");
    description.append(estimate.firstOrderHalfLifeSeconds());
    description.append(" s");
    if (estimate.hasDurationConversion()) {
      description.append(", conversion over declared duration=");
      description.append(estimate.durationConversionFraction());
    }
    description.append(". Context=");
    description.append(estimate.kineticContext());
    description.append('.');
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.KINETIC_FACTOR, description.toString(), weight));
    return weight;
  }

  private static double durationConversionWeight(final double conversionFraction) {
    if (conversionFraction >= 0.95) {
      return 0.11;
    }
    if (conversionFraction >= 0.65) {
      return 0.075;
    }
    if (conversionFraction >= 0.20) {
      return 0.035;
    }
    if (conversionFraction >= 0.03) {
      return -0.015;
    }
    return -0.07;
  }

  private static double firstOrderHalfLifeWeight(final double halfLifeSeconds) {
    if (halfLifeSeconds <= 900.0) {
      return 0.08;
    }
    if (halfLifeSeconds <= 21600.0) {
      return 0.04;
    }
    if (halfLifeSeconds <= 172800.0) {
      return 0.0;
    }
    return -0.05;
  }

  private double heterocycleDomainWeight(
      final ReactionPredictionRequest request,
      final ReactionProductCandidate candidate,
      final List<ReactionPredictionEvidence> evidence) {
    if (!request.hasDomain(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS)
        && !request.hasDomain(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION)) {
      return 0.0;
    }
    final long heterocycleScore = this.heterocycleScore(candidate);
    if (heterocycleScore > 0L) {
      final double weight = 0.15 + Math.min(heterocycleScore, 4L) * 0.05;
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN,
              "Candidate contains heterocycle features detected from molecular graph.",
              weight));
      return weight;
    }
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.WARNING,
            "Candidate does not contain a heterocycle pattern for requested domain.",
            -0.15));
    return -0.15;
  }

  private long heterocycleScore(final ReactionProductCandidate candidate) {
    long score = 0L;
    final List<ReactionParticipant> participants = candidate.products().participants();
    for (int i = 0; i < participants.size(); ++i) {
      final MolecularFeatureSet features =
          this.featureAnalyzer.analyze(participants.get(i).molecule());
      final long heterocycleCount = features.countOf(MolecularFeatureKind.HETEROCYCLE);
      final long heteroAtomRingCount =
          features.countOf(MolecularFeatureKind.NITROGEN_HETEROCYCLE)
              + features.countOf(MolecularFeatureKind.OXYGEN_HETEROCYCLE)
              + features.countOf(MolecularFeatureKind.SULFUR_HETEROCYCLE);
      score += heterocycleCount;
      score += heteroAtomRingCount;
      if (heterocycleCount > 0L && features.contains(MolecularFeatureKind.AROMATIC_RING)) {
        ++score;
      }
    }
    return score;
  }

  private double conditionWeight(
      final ReactionConditions conditions, final List<ReactionPredictionEvidence> evidence) {
    if (conditions == null || conditions.empty()) {
      return 0.0;
    }
    double score = 0.0;
    score += OrganicReactionCandidateScorer.temperatureWeight(conditions, evidence);
    score += OrganicReactionCandidateScorer.durationWeight(conditions, evidence);
    score += OrganicReactionCandidateScorer.featureWeight(conditions, evidence);
    score += OrganicReactionCandidateScorer.componentWeight(conditions, evidence);
    return score;
  }

  private ReactionConditionEffectProfile conditionEffectProfile(
      final ReactionPredictionRequest request, final List<ReactionPredictionEvidence> evidence) {
    final ReactionConditionEffectProfile profile = this.conditionEffectAnalyzer.analyze(request);
    if (profile.empty()) {
      return ReactionConditionEffectProfile.EMPTY;
    }
    final List<ReactionConditionEffect> effects = profile.effects();
    for (int i = 0; i < effects.size(); ++i) {
      final ReactionConditionEffect effect = effects.get(i);
      evidence.add(
          ReactionPredictionEvidence.of(
              effect.evidenceKind(), effect.description(), effect.scoreAdjustment()));
    }
    return profile;
  }

  private static double temperatureWeight(
      final ReactionConditions conditions, final List<ReactionPredictionEvidence> evidence) {
    final Temperature temperature = conditions.temperature();
    if (temperature == null) {
      return 0.0;
    }
    final double kelvin = temperature.kelvin();
    double weight = 0.01;
    String description = "Temperature is represented in reaction scoring context.";
    if (conditions.hasFeature(ReactionConditionFeatureKind.COOLING)
        && kelvin <= COOLING_TEMPERATURE_KELVIN) {
      weight = 0.04;
      description = "Cooling temperature is compatible with a low-temperature reaction context.";
    } else if (conditions.hasFeature(ReactionConditionFeatureKind.HEATING)
        && kelvin >= HEATING_TEMPERATURE_KELVIN) {
      weight = 0.05;
      description = "Heating temperature is compatible with an activated reaction context.";
    } else if (conditions.hasFeature(ReactionConditionFeatureKind.REFLUX)
        && kelvin >= HEATING_TEMPERATURE_KELVIN) {
      weight = 0.055;
      description = "Reflux temperature is compatible with sustained thermal activation.";
    } else if (kelvin >= REFLUX_TEMPERATURE_KELVIN) {
      weight = 0.025;
      description =
          "High temperature is represented without an explicit reflux or sealed-vessel feature.";
    } else if (Math.abs(kelvin - ROOM_TEMPERATURE_KELVIN) <= 10.0) {
      weight = 0.015;
      description = "Near-room-temperature condition is represented in scoring context.";
    }
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.TEMPERATURE_EFFECT, description, weight));
    return weight;
  }

  private static double durationWeight(
      final ReactionConditions conditions, final List<ReactionPredictionEvidence> evidence) {
    if (!conditions.hasDuration()) {
      return 0.0;
    }
    final double hours = conditions.duration().hours();
    final double weight;
    final String description;
    if (hours < 0.25) {
      weight = 0.005;
      description = "Very short reaction duration is represented for fast kinetic screening.";
    } else if (hours <= 48.0) {
      weight = 0.02;
      description = "Reaction duration is available for kinetic ranking.";
    } else {
      weight = 0.015;
      description = "Long reaction duration is represented for slow kinetic pathways.";
    }
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.KINETIC_FACTOR, description, weight));
    return weight;
  }

  private static double featureWeight(
      final ReactionConditions conditions, final List<ReactionPredictionEvidence> evidence) {
    double score = 0.0;
    score +=
        addFeatureEvidence(
            conditions,
            evidence,
            ReactionConditionFeatureKind.ANHYDROUS,
            ReactionPredictionEvidenceKind.SOLVENT_EFFECT,
            "Anhydrous condition is represented in candidate scoring context.",
            0.03);
    score +=
        addFeatureEvidence(
            conditions,
            evidence,
            ReactionConditionFeatureKind.INERT_ATMOSPHERE,
            ReactionPredictionEvidenceKind.SOLVENT_EFFECT,
            "Inert atmosphere is represented for air-sensitive organic chemistry.",
            0.025);
    score +=
        addFeatureEvidence(
            conditions,
            evidence,
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
            "Acidic medium is represented as an electronic activation factor.",
            0.02);
    score +=
        addFeatureEvidence(
            conditions,
            evidence,
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
            "Basic medium is represented as an electronic activation factor.",
            0.02);
    score +=
        addFeatureEvidence(
            conditions,
            evidence,
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionPredictionEvidenceKind.CATALYST_EFFECT,
            "Catalyst-present feature is represented in product ranking.",
            0.03);
    score +=
        addFeatureEvidence(
            conditions,
            evidence,
            ReactionConditionFeatureKind.DEHYDRATING_AGENT,
            ReactionPredictionEvidenceKind.THERMODYNAMIC_FACTOR,
            "Dehydrating condition is represented as a thermodynamic driving context.",
            0.025);
    score +=
        addFeatureEvidence(
            conditions,
            evidence,
            ReactionConditionFeatureKind.PHOTOCHEMICAL,
            ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
            "Photochemical activation is represented in reaction scoring.",
            0.02);
    score +=
        addFeatureEvidence(
            conditions,
            evidence,
            ReactionConditionFeatureKind.ELECTROCHEMICAL,
            ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
            "Electrochemical activation is represented in reaction scoring.",
            0.02);
    score +=
        addFeatureEvidence(
            conditions,
            evidence,
            ReactionConditionFeatureKind.MICROWAVE,
            ReactionPredictionEvidenceKind.KINETIC_FACTOR,
            "Microwave activation is represented as a kinetic acceleration context.",
            0.02);
    score +=
        addFeatureEvidence(
            conditions,
            evidence,
            ReactionConditionFeatureKind.FLOW_REACTOR,
            ReactionPredictionEvidenceKind.KINETIC_FACTOR,
            "Flow reactor mode is represented in kinetic ranking context.",
            0.01);
    score +=
        addFeatureEvidence(
            conditions,
            evidence,
            ReactionConditionFeatureKind.SEALED_VESSEL,
            ReactionPredictionEvidenceKind.THERMODYNAMIC_FACTOR,
            "Sealed vessel is represented for pressure and volatility-sensitive context.",
            0.01);
    score +=
        addFeatureEvidence(
            conditions,
            evidence,
            ReactionConditionFeatureKind.ADDITION_RATE,
            ReactionPredictionEvidenceKind.KINETIC_FACTOR,
            "Addition rate control is represented in kinetic ranking context.",
            0.01);
    return score;
  }

  private static double addFeatureEvidence(
      final ReactionConditions conditions,
      final List<ReactionPredictionEvidence> evidence,
      final ReactionConditionFeatureKind feature,
      final ReactionPredictionEvidenceKind kind,
      final String description,
      final double weight) {
    if (!conditions.hasFeature(feature)) {
      return 0.0;
    }
    evidence.add(ReactionPredictionEvidence.of(kind, description, weight));
    return weight;
  }

  private static double componentWeight(
      final ReactionConditions conditions, final List<ReactionPredictionEvidence> evidence) {
    double score = 0.0;
    score +=
        roleWeight(
            conditions,
            evidence,
            ReactionComponentRole.SOLVENT,
            ReactionPredictionEvidenceKind.SOLVENT_EFFECT,
            0.015,
            "Solvent component is represented in reaction scoring: ");
    score +=
        roleWeight(
            conditions,
            evidence,
            ReactionComponentRole.CATALYST,
            ReactionPredictionEvidenceKind.CATALYST_EFFECT,
            0.04,
            "Catalyst component is represented in reaction scoring: ");
    score +=
        roleWeight(
            conditions,
            evidence,
            ReactionComponentRole.LIGAND,
            ReactionPredictionEvidenceKind.CATALYST_EFFECT,
            0.02,
            "Ligand component is represented in catalyst context: ");
    score +=
        roleWeight(
            conditions,
            evidence,
            ReactionComponentRole.ACID,
            ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
            0.025,
            "Acid component is represented in electronic activation context: ");
    score +=
        roleWeight(
            conditions,
            evidence,
            ReactionComponentRole.BASE,
            ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
            0.025,
            "Base component is represented in electronic activation context: ");
    score +=
        roleWeight(
            conditions,
            evidence,
            ReactionComponentRole.ADDITIVE,
            ReactionPredictionEvidenceKind.THERMODYNAMIC_FACTOR,
            0.01,
            "Additive component is represented in reaction context: ");
    score +=
        roleWeight(
            conditions,
            evidence,
            ReactionComponentRole.REACTANT,
            ReactionPredictionEvidenceKind.KINETIC_FACTOR,
            0.012,
            "Reactant loading component is represented in reaction recipe context: ");
    if (OrganicReactionCandidateScorer.hasQuantifiedComponent(conditions.components())) {
      score += 0.01;
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Quantified component metadata is available for condition-aware ranking.",
              0.01));
    }
    return score;
  }

  private static double roleWeight(
      final ReactionConditions conditions,
      final List<ReactionPredictionEvidence> evidence,
      final ReactionComponentRole role,
      final ReactionPredictionEvidenceKind kind,
      final double weight,
      final String descriptionPrefix) {
    final List<ReactionConditionComponent> components = conditions.componentsByRole(role);
    if (components.isEmpty()) {
      return 0.0;
    }
    evidence.add(
        ReactionPredictionEvidence.of(
            kind,
            descriptionPrefix + OrganicReactionCandidateScorer.componentNames(components) + ".",
            weight));
    return weight;
  }

  private static boolean hasQuantifiedComponent(final List<ReactionConditionComponent> components) {
    for (int i = 0; i < components.size(); ++i) {
      final ReactionConditionComponent component = components.get(i);
      if (component.hasAmount()
          || component.hasConcentration()
          || component.hasEquivalent()
          || component.hasLoading()
          || component.hasPurity()) {
        return true;
      }
    }
    return false;
  }

  private static String componentNames(final List<ReactionConditionComponent> components) {
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < components.size(); ++i) {
      if (i > 0) {
        builder.append(", ");
      }
      builder.append(components.get(i).name());
    }
    return builder.toString();
  }

  /** Оценивает, насколько количественный рецепт поддерживает надежное ранжирование продукта. */
  private double stoichiometryWeight(
      final ReactionPredictionRequest request,
      final ReactionProductCandidate candidate,
      final List<ReactionPredictionEvidence> evidence) {
    final ReactionStoichiometryProfile profile = this.stoichiometryAnalyzer.analyze(request);
    if (profile.quantifiedReactantCount() == 0 && profile.totalGramScale() <= 0.0) {
      return 0.0;
    }
    double score = 0.0;
    score += OrganicReactionCandidateScorer.quantifiedReactantCoverageWeight(profile, evidence);
    score += OrganicReactionCandidateScorer.equivalentWindowWeight(profile, evidence);
    score += OrganicReactionCandidateScorer.scaleWeight(request, candidate, profile, evidence);
    score +=
        OrganicReactionCandidateScorer.componentQualityWeight(
            request.conditions().components(), evidence);
    return score;
  }

  private static double quantifiedReactantCoverageWeight(
      final ReactionStoichiometryProfile profile, final List<ReactionPredictionEvidence> evidence) {
    if (profile.quantifiedReactantCount() == 0) {
      return 0.0;
    }
    if (profile.unmatchedReactantCount() == 0 && profile.hasMatchedQuantities()) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Quantified reactant recipe is fully matched to molecular reactants; limiting-reagent context is available.",
              STOICHIOMETRY_COMPLETE_SUPPORT));
      return STOICHIOMETRY_COMPLETE_SUPPORT;
    }
    final double matchedRatio =
        (double) profile.matchedReactantCount() / (double) profile.quantifiedReactantCount();
    final double support = STOICHIOMETRY_PARTIAL_SUPPORT * Math.max(0.0, matchedRatio);
    final double penalty = STOICHIOMETRY_UNMATCHED_PENALTY * profile.unmatchedReactantCount();
    evidence.add(
        ReactionPredictionEvidence.of(
            penalty < 0.0
                ? ReactionPredictionEvidenceKind.WARNING
                : ReactionPredictionEvidenceKind.KINETIC_FACTOR,
            "Quantified recipe is only partially matched to reactant molecules: matched="
                + profile.matchedReactantCount()
                + ", unmatched="
                + profile.unmatchedReactantCount()
                + ".",
            support + penalty));
    return support + penalty;
  }

  private static double equivalentWindowWeight(
      final ReactionStoichiometryProfile profile, final List<ReactionPredictionEvidence> evidence) {
    if (!profile.hasMatchedQuantities() || profile.minimumEquivalentRatio() <= 0.0) {
      return 0.0;
    }
    final double ratio = profile.maximumEquivalentRatio() / profile.minimumEquivalentRatio();
    if (ratio < 1.25) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Matched reactants are close to stoichiometric balance; side-channel excess pressure is low.",
              STOICHIOMETRY_EXCESS_SUPPORT));
      return STOICHIOMETRY_EXCESS_SUPPORT;
    }
    if (ratio <= 3.0) {
      final double support = STOICHIOMETRY_EXCESS_SUPPORT * 0.5;
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Moderate reactant excess is represented in limiting-reagent ranking: max/min equiv ratio="
                  + ratio
                  + ".",
              support));
      return support;
    }
    final double penalty = STOICHIOMETRY_EXCESS_PENALTY * Math.min(3.0, ratio / 3.0);
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.COMPETING_PATHWAY,
            "Strong reactant excess can bias competing pathways and impurity formation: max/min equiv ratio="
                + ratio
                + ".",
            penalty));
    return penalty;
  }

  private static double scaleWeight(
      final ReactionPredictionRequest request,
      final ReactionProductCandidate candidate,
      final ReactionStoichiometryProfile profile,
      final List<ReactionPredictionEvidence> evidence) {
    if (profile.totalGramScale() <= 0.0) {
      return 0.0;
    }
    double score = 0.0;
    if (profile.totalGramScale() >= 1.0) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Gram-scale recipe is represented in product ranking: total quantified mass="
                  + profile.totalGramScale()
                  + " g.",
              GRAM_SCALE_RECIPE_SUPPORT));
      score += GRAM_SCALE_RECIPE_SUPPORT;
    }
    if (profile.totalGramScale() >= 50.0) {
      final long centers =
          OrganicReactionCandidateScorer.productHeavyAtomCount(candidate)
              + OrganicReactionCandidateScorer.reactantHeavyAtomCount(request);
      final double penalty =
          centers >= 30L ? LARGE_SCALE_COMPLEXITY_PENALTY : LARGE_SCALE_COMPLEXITY_PENALTY * 0.5;
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.COMPETING_PATHWAY,
              "Large quantified mass increases heat/mass-transfer sensitivity for complex organic substrates.",
              penalty));
      score += penalty;
    }
    return score;
  }

  private static double componentQualityWeight(
      final List<ReactionConditionComponent> components,
      final List<ReactionPredictionEvidence> evidence) {
    double score = 0.0;
    for (int i = 0; i < components.size(); ++i) {
      final ReactionConditionComponent component = components.get(i);
      if (component.hasEquivalent()) {
        final double weight =
            component.equivalent().value() >= 0.5 && component.equivalent().value() <= 3.0
                ? 0.006
                : -0.006;
        score += weight;
      }
      if (component.hasLoading()) {
        score += 0.004;
      }
      if (component.hasPurity()) {
        score += component.purity().percent() >= 95.0 ? 0.004 : -0.008;
      }
    }
    if (score != 0.0) {
      evidence.add(
          ReactionPredictionEvidence.of(
              score > 0.0
                  ? ReactionPredictionEvidenceKind.KINETIC_FACTOR
                  : ReactionPredictionEvidenceKind.WARNING,
              "Structured equivalent/loading/purity fields are represented in product ranking.",
              score));
    }
    return score;
  }

  private static long reactantHeavyAtomCount(final ReactionPredictionRequest request) {
    return OrganicReactionCandidateScorer.heavyAtomCount(request.reactants().participants());
  }

  private static long productHeavyAtomCount(final ReactionProductCandidate candidate) {
    return OrganicReactionCandidateScorer.heavyAtomCount(candidate.products().participants());
  }

  private static long heavyAtomCount(final List<ReactionParticipant> participants) {
    long count = 0L;
    for (int i = 0; i < participants.size(); ++i) {
      count +=
          OrganicReactionCandidateScorer.totalCount(
              OrganicReactionCandidateScorer.heavyAtomCounts(List.of(participants.get(i))));
    }
    return count;
  }

  private double functionalGroupWeight(
      final ReactionProductCandidate candidate, final List<ReactionPredictionEvidence> evidence) {
    long count = 0L;
    final List<ReactionParticipant> participants = candidate.products().participants();
    for (int i = 0; i < participants.size(); ++i) {
      final MolecularFeatureSet features =
          this.featureAnalyzer.analyze(participants.get(i).molecule());
      count += features.countOf(MolecularFeatureKind.CARBONYL);
      count += features.countOf(MolecularFeatureKind.ALDEHYDE);
      count += features.countOf(MolecularFeatureKind.KETONE);
      count += features.countOf(MolecularFeatureKind.CARBOXYL_DERIVATIVE);
      count += features.countOf(MolecularFeatureKind.ACYL_HALIDE);
      count += features.countOf(MolecularFeatureKind.ANHYDRIDE);
      count += features.countOf(MolecularFeatureKind.ESTER);
      count += features.countOf(MolecularFeatureKind.AMIDE);
      count += features.countOf(MolecularFeatureKind.ALCOHOL);
      count += features.countOf(MolecularFeatureKind.PHENOL);
      count += features.countOf(MolecularFeatureKind.AMINE);
      count += features.countOf(MolecularFeatureKind.IMINE);
      count += features.countOf(MolecularFeatureKind.NITRILE);
      count += features.countOf(MolecularFeatureKind.NITRO);
      count += features.countOf(MolecularFeatureKind.ALKENE);
      count += features.countOf(MolecularFeatureKind.ALKYNE);
      count += features.countOf(MolecularFeatureKind.EPOXIDE);
      count += features.countOf(MolecularFeatureKind.THIOL_OR_THIOETHER);
      count += features.countOf(MolecularFeatureKind.SULFUR_OXIDE);
      count += features.countOf(MolecularFeatureKind.HALOGENATED);
      count += features.countOf(MolecularFeatureKind.BORONIC_ACID_OR_BORONATE);
      count += features.countOf(MolecularFeatureKind.ORGANOMETALLIC_REAGENT);
      count += features.countOf(MolecularFeatureKind.ISOCYANIDE);
      count += features.countOf(MolecularFeatureKind.ISOCYANATE);
      count += features.countOf(MolecularFeatureKind.ACTIVATED_ALKENE);
      count += features.countOf(MolecularFeatureKind.ALPHA_CARBONYL_C_H);
      count += features.countOf(MolecularFeatureKind.GOOD_LEAVING_GROUP);
    }
    if (count == 0L) {
      return 0.0;
    }
    final double weight = Math.min(0.12, count * 0.015);
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
            "Candidate contains functional groups useful for organic reaction reasoning.",
            weight));
    return weight;
  }

  private double functionalGroupContinuityWeight(
      final ReactionPredictionRequest request,
      final ReactionProductCandidate candidate,
      final List<ReactionPredictionEvidence> evidence) {
    final long sourceCenters = this.functionalCenterCount(request.reactants().participants());
    final long productCenters = this.functionalCenterCount(candidate.products().participants());
    if (sourceCenters == 0L || productCenters == 0L) {
      return 0.0;
    }
    final long retainedCenters = Math.min(sourceCenters, productCenters);
    final double weight = Math.min(0.08, retainedCenters * 0.01);
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
            "Candidate preserves a chemically interpretable functional-center context across reactants and products.",
            weight));
    return weight;
  }

  private double competingFunctionalCenterPenalty(
      final ReactionPredictionRequest request,
      final ReactionProductCandidate candidate,
      final List<ReactionPredictionEvidence> evidence) {
    final long sourceCenters = this.functionalCenterCount(request.reactants().participants());
    final long productCenters = this.functionalCenterCount(candidate.products().participants());
    final long totalCenters = sourceCenters + productCenters;
    if (totalCenters <= 8L) {
      return 0.0;
    }
    final double penalty = -Math.min(0.10, (totalCenters - 8L) * 0.01);
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.COMPETING_PATHWAY,
            "Multiple functional centers create competing pathway risk; candidate is penalized unless mechanism evidence overcomes it.",
            penalty));
    return penalty;
  }

  private double activatedHeteroarylAminationWeight(
      final ReactionPredictionRequest request,
      final ReactionProductCandidate candidate,
      final List<ReactionPredictionEvidence> evidence) {
    if (!request.hasDomain(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION)
        && !request.hasDomain(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION)
        && !request.hasDomain(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS)) {
      return 0.0;
    }
    final ReactantFeatureSignal reactantSignal = this.reactantFeatureSignal(request);
    if (!reactantSignal.has(MolecularFeatureKind.HETEROARYL_HALIDE)
        || !reactantSignal.has(MolecularFeatureKind.PRIMARY_OR_SECONDARY_AMINE)) {
      return 0.0;
    }
    final int reactantCarbonHalogenBonds =
        OrganicReactionCandidateScorer.carbonHalogenBondCount(request.reactants().participants());
    final int productCarbonHalogenBonds =
        OrganicReactionCandidateScorer.carbonHalogenBondCount(candidate.products().participants());
    if (reactantCarbonHalogenBonds <= 0
        || productCarbonHalogenBonds >= reactantCarbonHalogenBonds) {
      return 0.0;
    }
    double weight = 0.16;
    final StringBuilder context =
        new StringBuilder("Activated heteroaryl halide amination/SNAr context detected");
    if (reactantSignal.has(MolecularFeatureKind.ELECTRON_POOR_HETEROARENE)) {
      weight += 0.06;
      context.append("; electron-poor heteroarene increases substitution plausibility");
    }
    if (reactantSignal.has(MolecularFeatureKind.NITRILE)
        || reactantSignal.has(MolecularFeatureKind.ELECTRON_WITHDRAWING_GROUP)) {
      weight += 0.05;
      context.append(
          "; electron-withdrawing substituent supports anionic intermediate stabilization");
    }
    if (reactantSignal.has(MolecularFeatureKind.BENZYL_OR_BENZYLIC_FRAGMENT)) {
      weight += 0.025;
      context.append("; benzylic amine fragment is represented as a competent amine nucleophile");
    }
    if (request.conditions().temperature() != null
        && request.conditions().temperature().kelvin() >= HEATING_TEMPERATURE_KELVIN) {
      weight += 0.025;
      context.append("; heated conditions support slower heteroaryl substitution");
    }
    final double boundedWeight = Math.min(0.32, weight);
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN,
            context.append(".").toString(),
            boundedWeight));
    return boundedWeight;
  }

  private ReactantFeatureSignal reactantFeatureSignal(final ReactionPredictionRequest request) {
    final ReactantFeatureSignal signal = new ReactantFeatureSignal();
    final List<ReactionParticipant> participants = request.reactants().participants();
    for (int i = 0; i < participants.size(); ++i) {
      signal.add(this.featureAnalyzer.analyze(participants.get(i).molecule()));
    }
    return signal;
  }

  private long functionalCenterCount(final List<ReactionParticipant> participants) {
    long count = 0L;
    for (int i = 0; i < participants.size(); ++i) {
      count +=
          this.functionalCenterCount(this.featureAnalyzer.analyze(participants.get(i).molecule()));
    }
    return count;
  }

  private long functionalCenterCount(final MolecularFeatureSet features) {
    long count = 0L;
    count += features.countOf(MolecularFeatureKind.CARBONYL);
    count += features.countOf(MolecularFeatureKind.ALDEHYDE);
    count += features.countOf(MolecularFeatureKind.KETONE);
    count += features.countOf(MolecularFeatureKind.CARBOXYL_DERIVATIVE);
    count += features.countOf(MolecularFeatureKind.CARBOXYLIC_ACID_OR_CARBOXYLATE);
    count += features.countOf(MolecularFeatureKind.ACYL_HALIDE);
    count += features.countOf(MolecularFeatureKind.ANHYDRIDE);
    count += features.countOf(MolecularFeatureKind.ESTER);
    count += features.countOf(MolecularFeatureKind.AMIDE);
    count += features.countOf(MolecularFeatureKind.ALCOHOL);
    count += features.countOf(MolecularFeatureKind.PHENOL);
    count += features.countOf(MolecularFeatureKind.HYDROXYL_OR_ALKOXY);
    count += features.countOf(MolecularFeatureKind.ETHER);
    count += features.countOf(MolecularFeatureKind.EPOXIDE);
    count += features.countOf(MolecularFeatureKind.AMINE);
    count += features.countOf(MolecularFeatureKind.IMINE);
    count += features.countOf(MolecularFeatureKind.NITRILE);
    count += features.countOf(MolecularFeatureKind.NITRO);
    count += features.countOf(MolecularFeatureKind.ALKENE);
    count += features.countOf(MolecularFeatureKind.ALKYNE);
    count += features.countOf(MolecularFeatureKind.THIOL_OR_THIOETHER);
    count += features.countOf(MolecularFeatureKind.SULFUR_OXIDE);
    count += features.countOf(MolecularFeatureKind.HALOGENATED);
    count += features.countOf(MolecularFeatureKind.CHARGED_CENTER);
    count += features.countOf(MolecularFeatureKind.RADICAL_CENTER);
    count += features.countOf(MolecularFeatureKind.BORONIC_ACID_OR_BORONATE);
    count += features.countOf(MolecularFeatureKind.ORGANOMETALLIC_REAGENT);
    count += features.countOf(MolecularFeatureKind.ISOCYANIDE);
    count += features.countOf(MolecularFeatureKind.ISOCYANATE);
    count += features.countOf(MolecularFeatureKind.ACTIVATED_ALKENE);
    count += features.countOf(MolecularFeatureKind.ALPHA_CARBONYL_C_H);
    count += features.countOf(MolecularFeatureKind.GOOD_LEAVING_GROUP);
    return count;
  }

  private static double predictedMechanismWeight(
      final ReactionProductCandidate candidate, final List<ReactionPredictionEvidence> evidence) {
    if (!candidate.hasPredictedMechanisms()) {
      return 0.0;
    }
    final List<PredictedReactionMechanism> mechanisms = candidate.predictedMechanisms();
    double weight = 0.0;
    for (int i = 0; i < mechanisms.size(); ++i) {
      final PredictedReactionMechanism mechanism = mechanisms.get(i);
      final double pathwayWeight =
          0.04
              + mechanism.pathwayProbability() * 0.08
              + mechanism.expectedYieldPercent() / 100.0 * 0.04;
      weight += pathwayWeight;
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.USER_RULE,
              "Predicted "
                  + mechanism.family()
                  + " pathway "
                  + mechanism.mechanismId()
                  + " contributes to product probability.",
              pathwayWeight));
    }
    return Math.min(0.28, weight);
  }

  private static double userSeedPriorityWeight(
      final ReactionPredictionRequest request,
      final int sourceIndex,
      final List<ReactionPredictionEvidence> evidence) {
    if (sourceIndex >= request.seedCandidates().size()) {
      return 0.0;
    }
    final double weight = 0.10;
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.USER_RULE,
            "User-declared product seed is treated as a candidate hypothesis; ranking still comes from mechanism, topology, electronics and conditions.",
            weight));
    return weight;
  }

  private static double mechanismConditionAlignmentWeight(
      final ReactionPredictionRequest request,
      final ReactionProductCandidate candidate,
      final List<ReactionPredictionEvidence> evidence) {
    if (!candidate.hasPredictedMechanisms()) {
      return 0.0;
    }
    final List<ReactionMechanismFamily> families =
        OrganicReactionCandidateScorer.mechanismFamilies(request);
    if (families.isEmpty()) {
      return 0.0;
    }
    double weight = 0.0;
    boolean exactFamilyMatched = false;
    boolean crossCouplingExpected = request.hasDomain(OrganicReactionDomain.CROSS_COUPLING);
    boolean crossCouplingCandidate = false;
    final List<PredictedReactionMechanism> mechanisms = candidate.predictedMechanisms();
    for (int i = 0; i < mechanisms.size(); ++i) {
      final ReactionMechanismFamily family = mechanisms.get(i).family();
      if (OrganicReactionCandidateScorer.isCrossCouplingFamily(family)) {
        crossCouplingCandidate = true;
      }
      if (!families.contains(family) || family == ReactionMechanismFamily.UNKNOWN) {
        continue;
      }
      exactFamilyMatched = true;
      final double familyWeight =
          OrganicReactionCandidateScorer.isSpecificNamedFamily(family) ? 0.34 : 0.16;
      weight += familyWeight;
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.USER_RULE,
              "Predicted "
                  + family.name()
                  + " mechanism is explicitly aligned with reaction conditions.",
              familyWeight));
    }
    if (crossCouplingExpected && !crossCouplingCandidate && candidate.hasPredictedMechanisms()) {
      final double penalty = exactFamilyMatched ? 0.0 : -0.16;
      if (penalty < 0.0) {
        evidence.add(
            ReactionPredictionEvidence.of(
                ReactionPredictionEvidenceKind.COMPETING_PATHWAY,
                "Candidate mechanism does not match explicit cross-coupling condition context.",
                penalty));
      }
      weight += penalty;
    }
    return Math.max(-0.18, Math.min(0.42, weight));
  }

  private static boolean isSpecificNamedFamily(final ReactionMechanismFamily family) {
    return family == ReactionMechanismFamily.SUZUKI_COUPLING
        || family == ReactionMechanismFamily.HECK_COUPLING
        || family == ReactionMechanismFamily.SONOGASHIRA_COUPLING
        || family == ReactionMechanismFamily.NEGISHI_COUPLING
        || family == ReactionMechanismFamily.STILLE_COUPLING
        || family == ReactionMechanismFamily.BUCHWALD_HARTWIG_AMINATION
        || family == ReactionMechanismFamily.OZONOLYSIS
        || family == ReactionMechanismFamily.BAEYER_VILLIGER
        || family == ReactionMechanismFamily.NITRATION
        || family == ReactionMechanismFamily.BECKMANN_REARRANGEMENT
        || family == ReactionMechanismFamily.COMBES_QUINOLINE_ANNULATION;
  }

  private static boolean isCrossCouplingFamily(final ReactionMechanismFamily family) {
    return family == ReactionMechanismFamily.ORGANOMETALLIC_CROSS_COUPLING
        || family == ReactionMechanismFamily.SUZUKI_COUPLING
        || family == ReactionMechanismFamily.HECK_COUPLING
        || family == ReactionMechanismFamily.SONOGASHIRA_COUPLING
        || family == ReactionMechanismFamily.NEGISHI_COUPLING
        || family == ReactionMechanismFamily.STILLE_COUPLING
        || family == ReactionMechanismFamily.BUCHWALD_HARTWIG_AMINATION
        || family == ReactionMechanismFamily.ORGANOMETALLIC_INSERTION
        || family == ReactionMechanismFamily.C_H_ACTIVATION;
  }

  private double mechanismPatternWeight(
      final ReactionPredictionRequest request,
      final ReactionProductCandidate candidate,
      final List<ReactionPredictionEvidence> evidence) {
    final List<ReactionMechanismFamily> families =
        OrganicReactionCandidateScorer.mechanismFamilies(request);
    if (families.isEmpty()) {
      return 0.0;
    }
    double weight = 0.0;
    for (int i = 0; i < families.size(); ++i) {
      final ReactionMechanismPattern pattern = this.mechanismCatalog.find(families.get(i));
      if (pattern == null) {
        continue;
      }
      final ReactionMechanismPatternAssessment assessment =
          this.mechanismPatternAssessor.assess(request, candidate, pattern);
      weight += assessment.scoreAdjustment();
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.USER_RULE,
              pattern.displayName()
                  + " mechanism contributes "
                  + pattern.detailedSteps().size()
                  + " detailed staged events: "
                  + OrganicReactionCandidateScorer.mechanismStagePreview(pattern)
                  + "; detail="
                  + OrganicReactionCandidateScorer.mechanismDetailSummary(pattern)
                  + "; applicability="
                  + OrganicReactionCandidateScorer.signalSummary(
                      assessment.matchedApplicabilitySignals())
                  + "; risks="
                  + OrganicReactionCandidateScorer.signalSummary(assessment.matchedRiskSignals())
                  + ".",
              assessment.scoreAdjustment()));
    }
    weight += OrganicReactionCandidateScorer.competingPathwayPenalty(families, evidence);
    return Math.min(0.20, weight);
  }

  private static String mechanismDetailSummary(final ReactionMechanismPattern pattern) {
    if (pattern.productionDetailedPattern()) {
      return "production-detailed";
    }
    if (pattern.chemicallyUsablePattern()) {
      return "usable-staged";
    }
    if (pattern.syntheticGenericPattern()) {
      return "generic-taxonomy-fallback";
    }
    return "under-specified";
  }

  private static String mechanismStagePreview(final ReactionMechanismPattern pattern) {
    final StringBuilder builder = new StringBuilder();
    final int limit = Math.min(3, pattern.detailedSteps().size());
    for (int i = 0; i < limit; ++i) {
      if (i > 0) {
        builder.append(" -> ");
      }
      builder.append(pattern.detailedSteps().get(i).stageName());
    }
    if (pattern.detailedSteps().size() > limit) {
      builder.append(" -> ...");
    }
    return builder.toString();
  }

  private static double competingPathwayPenalty(
      final List<ReactionMechanismFamily> families,
      final List<ReactionPredictionEvidence> evidence) {
    final int namedFamilyCount = OrganicReactionCandidateScorer.namedFamilyCount(families);
    if (namedFamilyCount < 2) {
      return 0.0;
    }
    final double penalty = -Math.min(0.12, (double) (namedFamilyCount - 1) * 0.025);
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.COMPETING_PATHWAY,
            "Multiple named mechanism families are plausible under the same conditions: "
                + OrganicReactionCandidateScorer.familySummary(families)
                + ". Product ranking is penalized until selectivity evidence resolves the competition.",
            penalty));
    return penalty;
  }

  private static int namedFamilyCount(final List<ReactionMechanismFamily> families) {
    int count = 0;
    for (int i = 0; i < families.size(); ++i) {
      if (families.get(i) == ReactionMechanismFamily.UNKNOWN) {
        continue;
      }
      ++count;
    }
    return count;
  }

  private static String familySummary(final List<ReactionMechanismFamily> families) {
    if (families.isEmpty()) {
      return "none";
    }
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < families.size(); ++i) {
      if (i > 0) {
        builder.append(",");
      }
      builder.append(families.get(i).name());
    }
    return builder.toString();
  }

  private static String signalSummary(final List<String> signals) {
    if (signals.isEmpty()) {
      return "none";
    }
    return String.join(",", signals);
  }

  private static List<ReactionMechanismFamily> mechanismFamilies(
      final ReactionPredictionRequest request) {
    final ArrayList<ReactionMechanismFamily> families = new ArrayList<ReactionMechanismFamily>();
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families, request, OrganicReactionDomain.ACID_BASE, ReactionMechanismFamily.ACID_BASE);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families,
        request,
        OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION,
        ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families,
        request,
        OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION,
        ReactionMechanismFamily.NUCLEOPHILIC_SUBSTITUTION_SN1);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families,
        request,
        OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION,
        ReactionMechanismFamily.NUCLEOPHILIC_SUBSTITUTION_SN2);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families,
        request,
        OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION,
        ReactionMechanismFamily.SOLVOLYSIS);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families,
        request,
        OrganicReactionDomain.ELECTROPHILIC_SUBSTITUTION,
        ReactionMechanismFamily.ELECTROPHILIC_AROMATIC_SUBSTITUTION);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families, request, OrganicReactionDomain.ADDITION, ReactionMechanismFamily.ADDITION);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families,
        request,
        OrganicReactionDomain.ADDITION,
        ReactionMechanismFamily.CARBONYL_ADDITION);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families,
        request,
        OrganicReactionDomain.ADDITION,
        ReactionMechanismFamily.ELECTROPHILIC_ADDITION);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families,
        request,
        OrganicReactionDomain.ADDITION,
        ReactionMechanismFamily.MICHAEL_ADDITION);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families, request, OrganicReactionDomain.ADDITION, ReactionMechanismFamily.EPOXIDE_OPENING);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families,
        request,
        OrganicReactionDomain.ELIMINATION,
        ReactionMechanismFamily.ELIMINATION_E2);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families,
        request,
        OrganicReactionDomain.ELIMINATION,
        ReactionMechanismFamily.ELIMINATION_E1);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families,
        request,
        OrganicReactionDomain.REARRANGEMENT,
        ReactionMechanismFamily.REARRANGEMENT);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families,
        request,
        OrganicReactionDomain.REARRANGEMENT,
        ReactionMechanismFamily.TAUTOMERIZATION);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families,
        request,
        OrganicReactionDomain.REARRANGEMENT,
        ReactionMechanismFamily.FRAGMENTATION);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families,
        request,
        OrganicReactionDomain.OXIDATION_REDUCTION,
        ReactionMechanismFamily.OXIDATION);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families,
        request,
        OrganicReactionDomain.OXIDATION_REDUCTION,
        ReactionMechanismFamily.REDUCTION);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families,
        request,
        OrganicReactionDomain.OXIDATION_REDUCTION,
        ReactionMechanismFamily.HYDROGENATION);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families,
        request,
        OrganicReactionDomain.OXIDATION_REDUCTION,
        ReactionMechanismFamily.HYDRIDE_REDUCTION);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families,
        request,
        OrganicReactionDomain.CROSS_COUPLING,
        ReactionMechanismFamily.ORGANOMETALLIC_CROSS_COUPLING);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families,
        request,
        OrganicReactionDomain.CROSS_COUPLING,
        ReactionMechanismFamily.BUCHWALD_HARTWIG_AMINATION);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families,
        request,
        OrganicReactionDomain.CROSS_COUPLING,
        ReactionMechanismFamily.C_H_ACTIVATION);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families,
        request,
        OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
        ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families,
        request,
        OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
        ReactionMechanismFamily.HETEROCYCLE_FUNCTIONALIZATION);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families,
        request,
        OrganicReactionDomain.CYCLIZATION,
        ReactionMechanismFamily.CYCLOADDITION);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families, request, OrganicReactionDomain.CYCLIZATION, ReactionMechanismFamily.PERICYCLIC);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families,
        request,
        OrganicReactionDomain.CYCLIZATION,
        ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families,
        request,
        OrganicReactionDomain.CYCLIZATION,
        ReactionMechanismFamily.RADICAL_CYCLIZATION);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families,
        request,
        OrganicReactionDomain.PROTECTION_DEPROTECTION,
        ReactionMechanismFamily.PROTECTING_GROUP);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families,
        request,
        OrganicReactionDomain.PROTECTION_DEPROTECTION,
        ReactionMechanismFamily.ACETAL_KETAL_FORMATION);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families,
        request,
        OrganicReactionDomain.PROTECTION_DEPROTECTION,
        ReactionMechanismFamily.ACETAL_HYDROLYSIS);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families,
        request,
        OrganicReactionDomain.RADICAL_FUNCTIONALIZATION,
        ReactionMechanismFamily.RADICAL_SUBSTITUTION);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families,
        request,
        OrganicReactionDomain.RADICAL_FUNCTIONALIZATION,
        ReactionMechanismFamily.RADICAL_CHAIN);
    OrganicReactionCandidateScorer.addFamilyForDomain(
        families,
        request,
        OrganicReactionDomain.RADICAL_FUNCTIONALIZATION,
        ReactionMechanismFamily.THIOL_ENE_CLICK);
    OrganicReactionCandidateScorer.addConditionInferredFamilies(families, request.conditions());
    if (request.conditions().hasFeature(ReactionConditionFeatureKind.PHOTOCHEMICAL)) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.PHOTOCHEMICAL);
    }
    if (request.conditions().hasFeature(ReactionConditionFeatureKind.ELECTROCHEMICAL)) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.ELECTROCHEMICAL);
    }
    return List.copyOf(families);
  }

  private static void addConditionInferredFamilies(
      final List<ReactionMechanismFamily> families, final ReactionConditions conditions) {
    if (conditions == null || conditions.empty()) {
      return;
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.DEHYDRATING_AGENT)) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.ESTERIFICATION);
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.IMINE_ENAMINE_FORMATION);
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.MULTICOMPONENT_CONDENSATION);
    }
    OrganicReactionCandidateScorer.addPhysicalRegimeFamilies(families, conditions);
    if (conditions.hasFeature(ReactionConditionFeatureKind.WORKUP)) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.HYDROLYSIS);
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.FLOW_REACTOR)
        || conditions.hasFeature(ReactionConditionFeatureKind.ADDITION_RATE)) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.POLYMERIZATION_CHAIN_GROWTH);
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.CATALYST_PRESENT)
        && !conditions.componentsByRole(ReactionComponentRole.CATALYST).isEmpty()) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.CATALYTIC_CYCLE);
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.C_H_ACTIVATION);
    }
    if (!conditions.componentsByRole(ReactionComponentRole.ACID).isEmpty()) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.ESTERIFICATION);
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.SOLVOLYSIS);
    }
    if (!conditions.componentsByRole(ReactionComponentRole.BASE).isEmpty()) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.TRANSESTERIFICATION);
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.HYDROLYSIS);
    }
    if (OrganicReactionCandidateScorer.hasComponentNameContaining(conditions, "grignard")
        || OrganicReactionCandidateScorer.hasComponentNameContaining(conditions, "organolithium")
        || OrganicReactionCandidateScorer.hasComponentNameContaining(conditions, "lithium")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.GRIGNARD_OR_ORGANOLITHIUM_ADDITION);
    }
    if (OrganicReactionCandidateScorer.hasComponentNameContaining(conditions, "nitrite")
        || OrganicReactionCandidateScorer.hasComponentNameContaining(conditions, "diazonium")) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.DIAZOTIZATION);
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.SANDMEYER);
    }
    OrganicReactionCandidateScorer.addNamedOxidationReductionFamilies(families, conditions);
    OrganicReactionCandidateScorer.addNamedElectrophilicAromaticFamilies(families, conditions);
    OrganicReactionCandidateScorer.addNamedCrossCouplingFamilies(families, conditions);
    OrganicReactionCandidateScorer.addNamedPericyclicAndRearrangementFamilies(families, conditions);
    OrganicReactionCandidateScorer.addNamedCarbonylAndMulticomponentFamilies(families, conditions);
    OrganicReactionCandidateScorer.addNamedOxidationAndHeterocycleFamilies(families, conditions);
    OrganicReactionCandidateScorer.addNamedProtectionCondensationAndAdditionFamilies(
        families, conditions);
    OrganicReactionCandidateScorer.addNamedReductionRearrangementAndCatalysisFamilies(
        families, conditions);
  }

  private static void addPhysicalRegimeFamilies(
      final List<ReactionMechanismFamily> families, final ReactionConditions conditions) {
    if (conditions.temperature() != null) {
      final double kelvin = conditions.temperature().kelvin();
      if (kelvin >= REFLUX_TEMPERATURE_KELVIN
          || conditions.hasFeature(ReactionConditionFeatureKind.REFLUX)) {
        OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.ELIMINATION_E1);
        OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.ELIMINATION_E2);
        OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.REARRANGEMENT);
        OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.FRAGMENTATION);
      }
      if (kelvin >= HEATING_TEMPERATURE_KELVIN
          || conditions.hasFeature(ReactionConditionFeatureKind.HEATING)) {
        OrganicReactionCandidateScorer.addFamily(
            families, ReactionMechanismFamily.CARBONYL_SUBSTITUTION);
        OrganicReactionCandidateScorer.addFamily(
            families, ReactionMechanismFamily.HETEROCYCLE_FUNCTIONALIZATION);
        OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.C_H_ACTIVATION);
      }
      if (kelvin <= COOLING_TEMPERATURE_KELVIN
          || conditions.hasFeature(ReactionConditionFeatureKind.COOLING)) {
        OrganicReactionCandidateScorer.addFamily(
            families, ReactionMechanismFamily.ORGANOMETALLIC_INSERTION);
        OrganicReactionCandidateScorer.addFamily(
            families, ReactionMechanismFamily.GRIGNARD_OR_ORGANOLITHIUM_ADDITION);
      }
    }
    if (conditions.hasDuration()) {
      final double hours = conditions.duration().hours();
      if (hours >= 2.0) {
        OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.HYDROLYSIS);
        OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.SOLVOLYSIS);
        OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.TAUTOMERIZATION);
      }
      if (hours >= 12.0) {
        OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.POLYMERIZATION);
        OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.FRAGMENTATION);
      }
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.ACIDIC_MEDIUM)) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.ACID_BASE);
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.ELECTROPHILIC_ADDITION);
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.ACETAL_KETAL_FORMATION);
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.BASIC_MEDIUM)) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.ENOLATE_ALDOL);
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.MICHAEL_ADDITION);
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.ELIMINATION_E2);
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.OXIDIZING_MEDIUM)) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.OXIDATION);
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.OXIDATIVE_CLEAVAGE);
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.REDUCING_MEDIUM)) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.REDUCTION);
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.HYDRIDE_REDUCTION);
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.HYDROGENATION)) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.HYDROGENATION);
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.CATALYTIC_CYCLE);
    }
    if (OrganicReactionCandidateScorer.hasAnyConditionTextContaining(
        conditions, "peg", "polyethylene glycol", "dmso", "dmf", "dmac", "nmp", "ionic liquid")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION);
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.SOLVOLYSIS);
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.TAUTOMERIZATION);
    }
    if (OrganicReactionCandidateScorer.hasAnyConditionTextContaining(
        conditions, "sealed", "pressure", "autoclave", "microwave")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.MULTICOMPONENT_CONDENSATION);
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION);
    }
  }

  private static void addNamedOxidationReductionFamilies(
      final List<ReactionMechanismFamily> families, final ReactionConditions conditions) {
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "ozone", "o3", "ozonolysis")) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.OZONOLYSIS);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "periodate", "permanganate", "kmno4", "osmium", "osmium tetroxide")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.OXIDATIVE_CLEAVAGE);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "peracid", "mcpba", "peracetic", "baeyer")) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.BAEYER_VILLIGER);
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.EPOXIDATION);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "hydrazine", "koh", "wolff")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.WOLFF_KISHNER_REDUCTION);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "zn/hg", "amalgam", "clemmensen")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.CLEMMENSEN_REDUCTION);
    }
  }

  private static void addNamedElectrophilicAromaticFamilies(
      final List<ReactionMechanismFamily> families, final ReactionConditions conditions) {
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "nitric", "nitrating", "hno3", "nitronium")) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.NITRATION);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "sulfuric", "oleum", "so3", "sulfonating")) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.SULFONATION);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "alcl3", "bf3", "fecl3", "lewis acid", "acid chloride", "acyl chloride")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.FRIEDEL_CRAFTS_ACYLATION);
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.FRIEDEL_CRAFTS_ALKYLATION);
    }
  }

  private static void addNamedCrossCouplingFamilies(
      final List<ReactionMechanismFamily> families, final ReactionConditions conditions) {
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "boronic", "boronate", "suzuki")) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.SUZUKI_COUPLING);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "alkene", "heck")) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.HECK_COUPLING);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "terminal alkyne", "copper iodide", "cui", "sonogashira")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.SONOGASHIRA_COUPLING);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "organozinc", "negishi")) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.NEGISHI_COUPLING);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "organostannane", "stannane", "stille")) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.STILLE_COUPLING);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "amine", "buchwald", "hartwig", "phosphine ligand")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.BUCHWALD_HARTWIG_AMINATION);
    }
  }

  private static void addNamedPericyclicAndRearrangementFamilies(
      final List<ReactionMechanismFamily> families, final ReactionConditions conditions) {
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "diene", "dienophile", "diels")) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.DIELS_ALDER);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "azide", "nitrone", "dipole")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.ONE_THREE_DIPOLAR_CYCLOADDITION);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "oxime", "beckmann")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.BECKMANN_REARRANGEMENT);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "pinacol", "vicinal diol")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.PINACOL_REARRANGEMENT);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "allyl vinyl ether", "claisen")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.CLAISEN_REARRANGEMENT);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "1,5-diene", "cope")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.COPE_REARRANGEMENT);
    }
  }

  private static void addNamedCarbonylAndMulticomponentFamilies(
      final List<ReactionMechanismFamily> families, final ReactionConditions conditions) {
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "mannich", "formaldehyde", "iminium")) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.MANNICH_REACTION);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "strecker", "cyanide", "hcn", "trimethylsilyl cyanide")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.STRECKER_SYNTHESIS);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "ugi", "isocyanide", "isonitrile")) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.UGI_REACTION);
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.PASSERINI_REACTION);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "phosphonium", "ylide", "wittig")) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.WITTIG_REACTION);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "phosphonate", "horner", "wadsworth", "emmons")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.HORNER_WADSWORTH_EMMONS);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "mitsunobu", "azodicarboxylate", "dead", "diad", "triphenylphosphine")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.MITSUNOBU_REACTION);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "robinson", "annulation", "michael acceptor")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.ROBINSON_ANNULATION);
    }
  }

  private static void addNamedOxidationAndHeterocycleFamilies(
      final List<ReactionMechanismFamily> families, final ReactionConditions conditions) {
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "swern", "dmso", "oxalyl chloride")) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.SWERN_OXIDATION);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "dess-martin", "dess martin", "dib", "hypervalent iodine")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.DESS_MARTIN_OXIDATION);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "osmium", "osmium tetroxide", "dihydroxylation")) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.DIHYDROXYLATION);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "sharpless", "tartrate", "titanium isopropoxide")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.SHARPLESS_EPOXIDATION);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "pauson", "khand", "carbon monoxide")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.PAUSON_KHAND_REACTION);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "nazarov", "divinyl ketone")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.NAZAROV_CYCLIZATION);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "fischer indole", "phenylhydrazine", "hydrazine")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.FISCHER_INDOLE_SYNTHESIS);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "skraup", "quinoline", "glycerol")) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.SKRAUP_SYNTHESIS);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions,
        "combes",
        "beta-diketone",
        "trifluoromethyl diketone",
        "polyphosphoric",
        "ppa",
        "polyphosphoric ester")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.COMBES_QUINOLINE_ANNULATION);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "hantzsch", "biginelli", "beta-ketoester", "urea")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.HANTZSCH_SYNTHESIS);
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.BIGINELLI_REACTION);
    }
  }

  private static void addNamedProtectionCondensationAndAdditionFamilies(
      final List<ReactionMechanismFamily> families, final ReactionConditions conditions) {
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "acetal", "ketal", "ethylene glycol", "diol protecting")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.ACETAL_KETAL_FORMATION);
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.ACETAL_HYDROLYSIS);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "epoxide", "oxirane")) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.EPOXIDE_OPENING);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "halohydrin", "nbs water", "bromonium water")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.HALOHYDRIN_FORMATION);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "hbr", "hcl", "hi", "hydrohalogenation")) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.HYDROHALOGENATION);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "hydration", "h2o acid", "mercuric acetate", "oxymercuration")) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.HYDRATION);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "claisen condensation", "alkoxide ester", "beta-keto ester")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.CLAISEN_CONDENSATION);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "dieckmann", "intramolecular claisen", "diester ring closure")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.DIECKMANN_CONDENSATION);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "knoevenagel", "active methylene", "malonate aldehyde")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.KNOEVENAGEL_CONDENSATION);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "henry reaction", "nitroaldol", "nitroalkane aldehyde")) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.HENRY_REACTION);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "cannizzaro", "non-enolizable aldehyde", "benzaldehyde strong base")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.CANNIZZARO_REACTION);
    }
  }

  private static void addNamedReductionRearrangementAndCatalysisFamilies(
      final List<ReactionMechanismFamily> families, final ReactionConditions conditions) {
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "birch", "sodium ammonia", "lithium ammonia")) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.BIRCH_REDUCTION);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "jones", "chromic acid", "cro3", "chromium vi")) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.JONES_OXIDATION);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "pcc", "pyridinium chlorochromate")) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.PCC_OXIDATION);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "nabh4", "lialh4", "dibal", "hydride reduction")) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.HYDRIDE_REDUCTION);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "curtius", "acyl azide", "isocyanate trapping")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.CURTIUS_REARRANGEMENT);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "hofmann rearrangement", "hypobromite amide", "one carbon loss amine")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.HOFMANN_REARRANGEMENT);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "aryne", "benzyne", "sodium amide aryl halide")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.ARYNE_SUBSTITUTION);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "carbene cyclopropanation", "diazomethane alkene", "simmons smith")) {
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.CARBENE_CYCLOPROPANATION);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "nitrene insertion", "iminoiodinane", "aziridination")) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.NITRENE_INSERTION);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "hydroformylation", "syngas", "co h2 rhodium")) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.HYDROFORMYLATION);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "grubbs", "olefin metathesis", "ru carbene")) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.OLEFIN_METATHESIS);
      OrganicReactionCandidateScorer.addFamily(
          families, ReactionMechanismFamily.RING_CLOSING_METATHESIS);
    }
    if (OrganicReactionCandidateScorer.hasAnyComponentNameContaining(
        conditions, "thiol ene", "thiol-ene", "photoinitiator thiol")) {
      OrganicReactionCandidateScorer.addFamily(families, ReactionMechanismFamily.THIOL_ENE_CLICK);
    }
  }

  private static boolean hasAnyComponentNameContaining(
      final ReactionConditions conditions, final String... fragments) {
    return OrganicReactionCandidateScorer.hasAnyConditionTextContaining(conditions, fragments);
  }

  private static boolean hasAnyConditionTextContaining(
      final ReactionConditions conditions, final String... fragments) {
    for (int i = 0; i < fragments.length; ++i) {
      if (OrganicReactionCandidateScorer.hasConditionTextContaining(conditions, fragments[i])) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasComponentNameContaining(
      final ReactionConditions conditions, final String fragment) {
    return OrganicReactionCandidateScorer.hasConditionTextContaining(conditions, fragment);
  }

  private static boolean hasConditionTextContaining(
      final ReactionConditions conditions, final String fragment) {
    final String expected = fragment.toLowerCase(java.util.Locale.ROOT);
    final List<ReactionConditionComponent> components = conditions.components();
    for (int i = 0; i < components.size(); ++i) {
      if (components.get(i).name().toLowerCase(java.util.Locale.ROOT).contains(expected)) {
        return true;
      }
    }
    return conditions.note() != null
        && conditions.note().toLowerCase(java.util.Locale.ROOT).contains(expected);
  }

  private static void addFamilyForDomain(
      final List<ReactionMechanismFamily> families,
      final ReactionPredictionRequest request,
      final OrganicReactionDomain domain,
      final ReactionMechanismFamily family) {
    if (request.hasDomain(domain)) {
      OrganicReactionCandidateScorer.addFamily(families, family);
    }
  }

  private static void addFamily(
      final List<ReactionMechanismFamily> families, final ReactionMechanismFamily family) {
    if (!families.contains(family)) {
      families.add(family);
    }
  }

  private static final class ReactantFeatureSignal {

    private final boolean[] present;

    private ReactantFeatureSignal() {
      this.present = new boolean[MolecularFeatureKind.values().length];
    }

    private void add(final MolecularFeatureSet features) {
      final List<ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeature>
          items = features.features();
      for (int i = 0; i < items.size(); ++i) {
        this.present[items.get(i).kind().ordinal()] = true;
      }
    }

    private boolean has(final MolecularFeatureKind kind) {
      return this.present[kind.ordinal()];
    }
  }

  private static final class PathwayScoringResult {

    private final double weight;
    private final ReactionMechanism detailedMechanism;

    private PathwayScoringResult(final double weight, final ReactionMechanism detailedMechanism) {
      this.weight = weight;
      this.detailedMechanism = detailedMechanism;
    }

    private static PathwayScoringResult of(
        final double weight, final ReactionMechanism detailedMechanism) {
      return new PathwayScoringResult(weight, detailedMechanism);
    }

    private double weight() {
      return this.weight;
    }

    private ReactionProductCandidate withDetailedMechanism(
        final ReactionProductCandidate candidate) {
      if (this.detailedMechanism == null) {
        return candidate;
      }
      final List<PredictedReactionMechanism> mechanisms = candidate.predictedMechanisms();
      final ArrayList<PredictedReactionMechanism> updated =
          new ArrayList<PredictedReactionMechanism>(Math.max(1, mechanisms.size()));
      boolean attached = false;
      for (int i = 0; i < mechanisms.size(); ++i) {
        final PredictedReactionMechanism mechanism = mechanisms.get(i);
        if (!attached
            && mechanism.family() == this.detailedMechanism.family()
            && !mechanism.hasDetailedMechanism()) {
          updated.add(
              PredictedReactionMechanism.of(
                  mechanism.mechanismId(),
                  mechanism.family(),
                  mechanism.pathwayProbability(),
                  mechanism.expectedYieldPercent(),
                  this.detailedMechanism));
          attached = true;
          continue;
        }
        updated.add(mechanism);
      }
      if (!attached) {
        updated.add(
            PredictedReactionMechanism.of(
                this.detailedMechanism.id(),
                this.detailedMechanism.family(),
                candidate.probability(),
                candidate.expectedYieldPercent(),
                this.detailedMechanism));
      }
      return candidate.withPredictedMechanisms(updated);
    }
  }

  private static final class ElementParseResult {

    private static final ElementParseResult INVALID = new ElementParseResult(null, -1);
    private final String symbol;
    private final int nextIndex;

    private ElementParseResult(final String symbol, final int nextIndex) {
      this.symbol = symbol;
      this.nextIndex = nextIndex;
    }

    private static ElementParseResult valid(final String symbol, final int nextIndex) {
      return new ElementParseResult(symbol, nextIndex);
    }

    private boolean valid() {
      return this.symbol != null;
    }

    private String symbol() {
      return this.symbol;
    }

    private int nextIndex() {
      return this.nextIndex;
    }
  }

  private static final class FormulaNumber {

    private final int value;
    private final int nextIndex;

    private FormulaNumber(final int value, final int nextIndex) {
      this.value = value;
      this.nextIndex = nextIndex;
    }

    private int value() {
      return this.value;
    }

    private int nextIndex() {
      return this.nextIndex;
    }
  }
}

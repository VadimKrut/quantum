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
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.GeneratedReactionCandidateSet;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRule;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleLibrary;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleMatch;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionMechanismChannelCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.PredictedReactionMechanism;

/** Применяет Java-native reaction rules и возвращает generated candidates. */
public final class RuleBasedOrganicReactionCandidateGenerator
    implements OrganicReactionCandidateGenerator {

  public static final RuleBasedOrganicReactionCandidateGenerator EMPTY =
      new RuleBasedOrganicReactionCandidateGenerator(OrganicReactionRuleLibrary.EMPTY);

  private final OrganicReactionRuleLibrary ruleLibrary;

  public RuleBasedOrganicReactionCandidateGenerator(final OrganicReactionRuleLibrary ruleLibrary) {
    if (ruleLibrary == null) {
      throw new IllegalArgumentException("Organic reaction rule library must not be null.");
    }
    this.ruleLibrary = ruleLibrary;
  }

  public GeneratedReactionCandidateSet generate(final ReactionPredictionRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction prediction request must not be null.");
    }
    if (this.ruleLibrary.empty()) {
      return GeneratedReactionCandidateSet.EMPTY;
    }
    final ArrayList<ReactionProductCandidate> candidates =
        new ArrayList<ReactionProductCandidate>();
    final ArrayList<ReactionMechanismChannelCandidate> mechanismChannels =
        new ArrayList<ReactionMechanismChannelCandidate>();
    final ArrayList<ReactionPredictionEvidence> diagnostics =
        new ArrayList<ReactionPredictionEvidence>();
    final List<OrganicReactionRule> rules = this.ruleLibrary.rules();
    for (int i = 0; i < rules.size(); ++i) {
      this.applyRule(request, rules.get(i), candidates, mechanismChannels, diagnostics);
    }
    return GeneratedReactionCandidateSet.of(candidates, mechanismChannels, diagnostics);
  }

  private void applyRule(
      final ReactionPredictionRequest request,
      final OrganicReactionRule rule,
      final List<ReactionProductCandidate> candidates,
      final List<ReactionMechanismChannelCandidate> mechanismChannels,
      final List<ReactionPredictionEvidence> diagnostics) {
    final OrganicReactionRuleMatch match = rule.match(request);
    if (match == null || !match.matched()) {
      return;
    }
    final List<ReactionProductCandidate> generated = rule.generate(request, match);
    if (generated == null || generated.isEmpty()) {
      mechanismChannels.add(
          RuleBasedOrganicReactionCandidateGenerator.mechanismChannel(rule, match));
      diagnostics.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.WARNING,
              "Matched rule produced no molecular-graph product candidates: "
                  + rule.id().value()
                  + ". A structural rule pattern matched ("
                  + match.explanation()
                  + "), but this does not establish reaction feasibility. The channel is diagnostic only: "
                  + "check the required explicit reagent graphs and supported transformation. "
                  + "A user-seeded product is a hypothesis, not proof that the reaction occurs.",
              -0.02));
      return;
    }
    final java.util.Map<String, Long> suppliedElements =
        ReactionAtomInventory.of(request.reactants());
    final java.util.Set<String> missingElements = new java.util.TreeSet<>();
    final int before = candidates.size();
    for (int i = 0; i < generated.size(); ++i) {
      final ReactionProductCandidate candidate = generated.get(i);
      if (candidate == null) {
        throw new IllegalArgumentException("Organic reaction rule generated null candidate.");
      }
      final List<String> missing =
          ReactionAtomInventory.excess(suppliedElements, candidate.products());
      if (!missing.isEmpty()) {
        missingElements.addAll(missing);
        continue;
      }
      candidates.add(
          RuleBasedOrganicReactionCandidateGenerator.withRuleMechanism(rule, match, candidate));
    }
    if (!missingElements.isEmpty()) {
      final String explanation =
          "Rule "
              + rule.id().value()
              + " requires additional explicit reactant atoms of "
              + String.join(", ", missingElements)
              + ". Conditions and catalyst labels do not supply atoms; "
              + "products exceeding the supplied stoichiometric atom/isotope inventory were not materialized.";
      diagnostics.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.WARNING, explanation, -0.02));
      if (candidates.size() == before) {
        mechanismChannels.add(
            RuleBasedOrganicReactionCandidateGenerator.mechanismChannel(
                rule,
                OrganicReactionRuleMatch.matched(
                    match.confidence(), match.explanation() + " " + explanation)));
      }
    }
    diagnostics.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.USER_RULE,
            "Rule matched: " + rule.displayName() + " (" + match.explanation() + ")",
            match.confidence()));
  }

  private static ReactionMechanismChannelCandidate mechanismChannel(
      final OrganicReactionRule rule, final OrganicReactionRuleMatch match) {
    return ReactionMechanismChannelCandidate.of(
        rule.id().value(),
        rule.displayName(),
        rule.mechanismFamily(),
        RuleBasedOrganicReactionCandidateGenerator.clampedProbability(match.confidence()),
        RuleBasedOrganicReactionCandidateGenerator.clampedExpectedYield(match.confidence() * 100.0),
        false,
        match.explanation(),
        "Provide an explicit product graph transform, a user-seeded product, or a drawing-captured product side for this matched mechanism channel.");
  }

  private static ReactionProductCandidate withRuleMechanism(
      final OrganicReactionRule rule,
      final OrganicReactionRuleMatch match,
      final ReactionProductCandidate candidate) {
    final ReactionProductCandidate candidateWithEvidence =
        RuleBasedOrganicReactionCandidateGenerator.withRuleTraceEvidence(rule, match, candidate);
    if (candidateWithEvidence.hasPredictedMechanisms()) {
      return candidateWithEvidence;
    }
    return candidateWithEvidence.withPredictedMechanisms(
        List.of(
            PredictedReactionMechanism.of(
                rule.id().value(),
                rule.mechanismFamily(),
                RuleBasedOrganicReactionCandidateGenerator.clampedProbability(match.confidence()),
                candidateWithEvidence.expectedYieldPercent())));
  }

  private static ReactionProductCandidate withRuleTraceEvidence(
      final OrganicReactionRule rule,
      final OrganicReactionRuleMatch match,
      final ReactionProductCandidate candidate) {
    if (RuleBasedOrganicReactionCandidateGenerator.hasRuleEvidence(candidate, rule.id().value())) {
      return candidate;
    }
    final ArrayList<ReactionPredictionEvidence> evidence =
        new ArrayList<ReactionPredictionEvidence>(candidate.evidence().size() + 1);
    evidence.addAll(candidate.evidence());
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.USER_RULE,
            "Generated by built-in rule "
                + rule.id().value()
                + ": "
                + rule.displayName()
                + " ("
                + match.explanation()
                + ").",
            RuleBasedOrganicReactionCandidateGenerator.clampedEvidenceWeight(match.confidence())));
    return ReactionProductCandidate.of(
        candidate.products(),
        candidate.probability(),
        candidate.expectedYieldPercent(),
        candidate.selectivityNote(),
        evidence,
        candidate.predictedMechanisms(),
        candidate.scoreBreakdown());
  }

  private static boolean hasRuleEvidence(
      final ReactionProductCandidate candidate, final String ruleId) {
    final List<ReactionPredictionEvidence> evidence = candidate.evidence();
    for (int i = 0; i < evidence.size(); ++i) {
      if (evidence.get(i).description().contains(ruleId)) {
        return true;
      }
    }
    return false;
  }

  private static double clampedEvidenceWeight(final double value) {
    if (!Double.isFinite(value)) {
      return 0.0;
    }
    if (value > 1.0) {
      return 1.0;
    }
    if (value < -1.0) {
      return -1.0;
    }
    return value;
  }

  private static double clampedProbability(final double value) {
    if (!Double.isFinite(value) || value <= 0.0) {
      return 0.01;
    }
    if (value >= 1.0) {
      return 1.0;
    }
    return value;
  }

  private static double clampedExpectedYield(final double value) {
    if (!Double.isFinite(value) || value <= 0.0) {
      return 1.0;
    }
    if (value >= 100.0) {
      return 100.0;
    }
    return value;
  }
}

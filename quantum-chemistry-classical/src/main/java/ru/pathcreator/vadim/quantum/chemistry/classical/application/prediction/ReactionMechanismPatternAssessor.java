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
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism.ReactionMechanismPattern;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism.ReactionMechanismPatternAssessment;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeature;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;

/**
 * Сопоставляет шаблон механизма с условиями, реагентами и продуктом-кандидатом.
 */
public final class ReactionMechanismPatternAssessor {

  private static final double BASE_MECHANISM_WEIGHT = 0.012;
  private static final double PATHWAY_WEIGHT_LIMIT = 0.030;
  private static final double APPLICABILITY_SIGNAL_WEIGHT = 0.012;
  private static final double RISK_SIGNAL_PENALTY = 0.018;
  private static final double MISSING_APPLICABILITY_PENALTY = 0.020;

  private final MolecularFeatureAnalyzer featureAnalyzer;

  public ReactionMechanismPatternAssessor() {
    this(new MolecularFeatureAnalyzer());
  }

  public ReactionMechanismPatternAssessor(final MolecularFeatureAnalyzer featureAnalyzer) {
    if (featureAnalyzer == null) {
      throw new IllegalArgumentException(
          "Reaction mechanism pattern feature analyzer must not be null.");
    }
    this.featureAnalyzer = featureAnalyzer;
  }

  public ReactionMechanismPatternAssessment assess(
      final ReactionPredictionRequest request,
      final ReactionProductCandidate candidate,
      final ReactionMechanismPattern pattern) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction mechanism assessment request must not be null.");
    }
    if (candidate == null) {
      throw new IllegalArgumentException(
          "Reaction mechanism assessment candidate must not be null.");
    }
    if (pattern == null) {
      throw new IllegalArgumentException("Reaction mechanism assessment pattern must not be null.");
    }
    final String context = this.context(request, candidate);
    final List<String> applicability = this.applicabilitySignals(request, pattern, context);
    final List<String> risks =
        ReactionMechanismPatternAssessor.matchedSignals(pattern.riskSignals(), context);
    double score =
        BASE_MECHANISM_WEIGHT
            + Math.min(
                PATHWAY_WEIGHT_LIMIT,
                pattern.pathwayProbabilityContribution() * PATHWAY_WEIGHT_LIMIT)
            + pattern.detailQualityWeight()
            + applicability.size() * APPLICABILITY_SIGNAL_WEIGHT
            - risks.size() * RISK_SIGNAL_PENALTY;
    if (applicability.isEmpty()) {
      score -= MISSING_APPLICABILITY_PENALTY;
    }
    return ReactionMechanismPatternAssessment.of(pattern, applicability, risks, score);
  }

  private List<String> applicabilitySignals(
      final ReactionPredictionRequest request,
      final ReactionMechanismPattern pattern,
      final String context) {
    final List<String> signals =
        ReactionMechanismPatternAssessor.matchedSignals(pattern.applicabilitySignals(), context);
    if (signals.isEmpty()) {
      return signals;
    }
    if (!ReactionMechanismPatternAssessor.crossCouplingFamily(pattern.family())) {
      return signals;
    }
    if (ReactionMechanismPatternAssessor.crossCouplingConditionSupported(
        request.conditions(), context)) {
      return signals;
    }
    return List.of();
  }

  private static boolean crossCouplingFamily(final ReactionMechanismFamily family) {
    return family == ReactionMechanismFamily.ORGANOMETALLIC_CROSS_COUPLING
        || family == ReactionMechanismFamily.SUZUKI_COUPLING
        || family == ReactionMechanismFamily.HECK_COUPLING
        || family == ReactionMechanismFamily.SONOGASHIRA_COUPLING
        || family == ReactionMechanismFamily.NEGISHI_COUPLING
        || family == ReactionMechanismFamily.STILLE_COUPLING
        || family == ReactionMechanismFamily.BUCHWALD_HARTWIG_AMINATION;
  }

  private static boolean crossCouplingConditionSupported(
      final ReactionConditions conditions, final String context) {
    if (conditions == null || conditions.empty()) {
      return false;
    }
    return context.contains("catalyst")
        || context.contains("ligand")
        || context.contains("palladium")
        || context.contains("nickel")
        || context.contains("copper")
        || context.contains(" pd")
        || context.contains(" ni")
        || context.contains(" cu")
        || context.contains("base")
        || context.contains("boron")
        || context.contains("boronate")
        || context.contains("zinc")
        || context.contains("stannane")
        || context.contains("magnesium")
        || context.contains("inert atmosphere");
  }

  private String context(
      final ReactionPredictionRequest request, final ReactionProductCandidate candidate) {
    final StringBuilder builder = new StringBuilder();
    ReactionMechanismPatternAssessor.appendDomains(builder, request.domains());
    ReactionMechanismPatternAssessor.appendConditions(builder, request.conditions());
    this.appendReactionSide(builder, request.reactants());
    this.appendReactionSide(builder, candidate.products());
    return builder.toString().toLowerCase(Locale.ROOT);
  }

  private static void appendDomains(
      final StringBuilder builder, final List<OrganicReactionDomain> domains) {
    for (int i = 0; i < domains.size(); ++i) {
      builder.append(' ');
      builder.append(domains.get(i).name().replace('_', ' '));
    }
  }

  private static void appendConditions(
      final StringBuilder builder, final ReactionConditions conditions) {
    if (conditions == null || conditions.empty()) {
      return;
    }
    final List<ReactionConditionFeature> features = conditions.features();
    for (int i = 0; i < features.size(); ++i) {
      builder.append(' ');
      builder.append(features.get(i).kind().name().replace('_', ' '));
      if (features.get(i).hasNote()) {
        builder.append(' ');
        builder.append(features.get(i).note());
      }
    }
    final List<ReactionConditionComponent> components = conditions.components();
    for (int i = 0; i < components.size(); ++i) {
      builder.append(' ');
      builder.append(components.get(i).role().name().replace('_', ' '));
      builder.append(' ');
      builder.append(components.get(i).name());
      if (components.get(i).hasNote()) {
        builder.append(' ');
        builder.append(components.get(i).note());
      }
    }
    if (conditions.note() != null) {
      builder.append(' ');
      builder.append(conditions.note());
    }
  }

  private void appendReactionSide(final StringBuilder builder, final ReactionSide side) {
    final List<ReactionParticipant> participants = side.participants();
    for (int i = 0; i < participants.size(); ++i) {
      final MolecularFeatureSet features =
          this.featureAnalyzer.analyze(participants.get(i).molecule());
      for (final MolecularFeatureKind kind : MolecularFeatureKind.values()) {
        if (!features.contains(kind)) {
          continue;
        }
        builder.append(' ');
        builder.append(kind.name().replace('_', ' '));
      }
      builder.append(' ');
      builder.append(participants.get(i).molecule().displayName());
      builder.append(' ');
      builder.append(participants.get(i).molecule().formula().hillNotation());
    }
  }

  private static List<String> matchedSignals(final List<String> signals, final String context) {
    if (signals.isEmpty() || context.isBlank()) {
      return List.of();
    }
    final ArrayList<String> matched = new ArrayList<String>();
    for (int i = 0; i < signals.size(); ++i) {
      final String signal = signals.get(i);
      if (!ReactionMechanismPatternAssessor.signalMatches(signal, context)) {
        continue;
      }
      matched.add(signal);
    }
    return List.copyOf(matched);
  }

  private static boolean signalMatches(final String signal, final String context) {
    final String normalized = signal.toLowerCase(Locale.ROOT).replace('/', ' ');
    if (ReactionMechanismPatternAssessor.negativeSignal(normalized)) {
      return context.contains(normalized)
          || context.contains("missing " + ReactionMechanismPatternAssessor.signalHead(normalized))
          || context.contains("without " + ReactionMechanismPatternAssessor.signalHead(normalized));
    }
    final String[] words = normalized.split("[^a-z0-9]+");
    for (int i = 0; i < words.length; ++i) {
      final String word = words[i];
      if (word.length() < 3) {
        continue;
      }
      if (context.contains(word)) {
        return true;
      }
    }
    return false;
  }

  private static boolean negativeSignal(final String signal) {
    return signal.startsWith("no ")
        || signal.startsWith("poor ")
        || signal.startsWith("missing ")
        || signal.startsWith("wrong ")
        || signal.startsWith("manual ")
        || signal.contains(" absent")
        || signal.contains(" blocked")
        || signal.contains(" poisoning")
        || signal.contains(" mismatch")
        || signal.contains(" limitation");
  }

  private static String signalHead(final String signal) {
    final String[] words = signal.split("[^a-z0-9]+");
    for (int i = 0; i < words.length; ++i) {
      if (words[i].length() >= 3
          && !"poor".equals(words[i])
          && !"missing".equals(words[i])
          && !"wrong".equals(words[i])
          && !"manual".equals(words[i])) {
        return words[i];
      }
    }
    return signal;
  }
}

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
import java.util.Comparator;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionConditionSuitability;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionConfidenceLevel;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionExplanation;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionScoreComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionScoreComponentKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;

/**
 * Строит объяснение прогноза продуктов без изменения самого результата.
 */
public final class ReactionPredictionExplainer {

  private static final int MAX_EXPLANATION_EVIDENCE = 5;
  private static final double MEDIUM_CONFIDENCE_GAP = 0.15;
  private static final double HIGH_CONFIDENCE_GAP = 0.35;
  private static final double DECISIVE_CONFIDENCE_GAP = 0.60;

  private final OrganicReactionPathwayAnalyzer pathwayAnalyzer;

  public ReactionPredictionExplainer() {
    this(new OrganicReactionPathwayAnalyzer());
  }

  public ReactionPredictionExplainer(final OrganicReactionPathwayAnalyzer pathwayAnalyzer) {
    if (pathwayAnalyzer == null) {
      throw new IllegalArgumentException("Organic reaction pathway analyzer must not be null.");
    }
    this.pathwayAnalyzer = pathwayAnalyzer;
  }

  public ReactionPredictionExplanation explain(final ReactionPredictionResult result) {
    if (result == null) {
      throw new IllegalArgumentException("Reaction prediction result must not be null.");
    }
    if (!result.hasCandidates()) {
      final ReactionPredictionConfidenceLevel confidenceLevel =
          ReactionPredictionConfidenceLevel.NONE;
      return ReactionPredictionExplanation.of(
          confidenceLevel,
          0.0,
          0.0,
          0.0,
          0,
          0,
          result.diagnostics().size(),
          result.request().conditions() != null && !result.request().conditions().empty(),
          ReactionPredictionExplainer.conditionSuitability(result, result.diagnostics()),
          this.pathwayAnalyzer.assess(result, confidenceLevel, result.diagnostics()),
          List.of(),
          ReactionPredictionExplainer.sortedLimited(result.diagnostics(), false));
    }
    final List<ReactionProductCandidate> candidates = result.candidates();
    final ReactionProductCandidate best = candidates.get(0);
    final double topProbability = best.probability();
    final double runnerUpProbability =
        candidates.size() > 1 ? candidates.get(1).probability() : 0.0;
    final double probabilityGap = Math.max(0.0, topProbability - runnerUpProbability);
    final ArrayList<ReactionPredictionEvidence> allEvidence =
        new ArrayList<ReactionPredictionEvidence>(
            best.evidence().size()
                + best.scoreBreakdown().components().size()
                + result.diagnostics().size());
    allEvidence.addAll(best.evidence());
    ReactionPredictionExplainer.addScoreComponentEvidence(best, allEvidence);
    allEvidence.addAll(result.diagnostics());
    final ReactionPredictionConfidenceLevel confidenceLevel =
        ReactionPredictionExplainer.confidence(
            probabilityGap, topProbability, result.probabilityMass(), allEvidence);
    return ReactionPredictionExplanation.of(
        confidenceLevel,
        topProbability,
        runnerUpProbability,
        probabilityGap,
        candidates.size(),
        ReactionPredictionExplainer.countPositive(allEvidence),
        ReactionPredictionExplainer.countNegative(allEvidence),
        ReactionPredictionExplainer.conditionAware(result, allEvidence),
        ReactionPredictionExplainer.conditionSuitability(result, allEvidence),
        this.pathwayAnalyzer.assess(result, confidenceLevel, allEvidence),
        ReactionPredictionExplainer.sortedLimited(allEvidence, true),
        ReactionPredictionExplainer.sortedLimited(allEvidence, false));
  }

  private static ReactionPredictionConfidenceLevel confidence(
      final double probabilityGap,
      final double topProbability,
      final double probabilityMass,
      final List<ReactionPredictionEvidence> evidence) {
    if (topProbability <= 0.0) {
      return ReactionPredictionConfidenceLevel.NONE;
    }
    final double missingProbabilityMass = Math.max(0.0, 1.0 - probabilityMass);
    final double negativeWeight = ReactionPredictionExplainer.negativeWeight(evidence);
    final int negativeCount = ReactionPredictionExplainer.countNegative(evidence);
    final int positiveCount = ReactionPredictionExplainer.countPositive(evidence);
    ReactionPredictionConfidenceLevel level;
    if (probabilityGap >= DECISIVE_CONFIDENCE_GAP
        && ReactionPredictionExplainer.countNegative(evidence) == 0) {
      level = ReactionPredictionConfidenceLevel.DECISIVE;
    } else if (probabilityGap >= HIGH_CONFIDENCE_GAP && topProbability >= 0.5) {
      level = ReactionPredictionConfidenceLevel.HIGH;
    } else if (probabilityGap >= MEDIUM_CONFIDENCE_GAP || topProbability >= 0.45) {
      level = ReactionPredictionConfidenceLevel.MEDIUM;
    } else {
      level = ReactionPredictionConfidenceLevel.LOW;
    }
    if (missingProbabilityMass > 0.25) {
      level = ReactionPredictionExplainer.downgrade(level, 2);
    } else if (missingProbabilityMass > 0.15) {
      level = ReactionPredictionExplainer.downgrade(level, 1);
    }
    if (negativeWeight >= 0.60) {
      level = ReactionPredictionExplainer.downgrade(level, 2);
    } else if (negativeWeight >= 0.10 || negativeCount > positiveCount) {
      level = ReactionPredictionExplainer.downgrade(level, 1);
    }
    return level;
  }

  private static ReactionPredictionConfidenceLevel downgrade(
      final ReactionPredictionConfidenceLevel level, final int steps) {
    ReactionPredictionConfidenceLevel current = level;
    for (int i = 0; i < steps; ++i) {
      if (current == ReactionPredictionConfidenceLevel.DECISIVE) {
        current = ReactionPredictionConfidenceLevel.HIGH;
      } else if (current == ReactionPredictionConfidenceLevel.HIGH) {
        current = ReactionPredictionConfidenceLevel.MEDIUM;
      } else if (current == ReactionPredictionConfidenceLevel.MEDIUM) {
        current = ReactionPredictionConfidenceLevel.LOW;
      } else if (current == ReactionPredictionConfidenceLevel.LOW) {
        current = ReactionPredictionConfidenceLevel.NONE;
      }
    }
    return current;
  }

  private static int countPositive(final List<ReactionPredictionEvidence> evidence) {
    int count = 0;
    for (int i = 0; i < evidence.size(); ++i) {
      if (evidence.get(i).weight() > 0.0) {
        ++count;
      }
    }
    return count;
  }

  private static int countNegative(final List<ReactionPredictionEvidence> evidence) {
    int count = 0;
    for (int i = 0; i < evidence.size(); ++i) {
      if (evidence.get(i).weight() < 0.0) {
        ++count;
      }
    }
    return count;
  }

  private static double negativeWeight(final List<ReactionPredictionEvidence> evidence) {
    double weight = 0.0;
    for (int i = 0; i < evidence.size(); ++i) {
      final double value = evidence.get(i).weight();
      if (value < 0.0) {
        weight += Math.abs(value);
      }
    }
    return weight;
  }

  private static boolean conditionAware(
      final ReactionPredictionResult result, final List<ReactionPredictionEvidence> evidence) {
    if (result.request().conditions() != null && !result.request().conditions().empty()) {
      return true;
    }
    for (int i = 0; i < evidence.size(); ++i) {
      if (ReactionPredictionExplainer.isConditionEvidence(evidence.get(i).kind())) {
        return true;
      }
    }
    return false;
  }

  private static boolean isConditionEvidence(final ReactionPredictionEvidenceKind kind) {
    return kind == ReactionPredictionEvidenceKind.SOLVENT_EFFECT
        || kind == ReactionPredictionEvidenceKind.TEMPERATURE_EFFECT
        || kind == ReactionPredictionEvidenceKind.CATALYST_EFFECT
        || kind == ReactionPredictionEvidenceKind.KINETIC_FACTOR
        || kind == ReactionPredictionEvidenceKind.THERMODYNAMIC_FACTOR;
  }

  private static void addScoreComponentEvidence(
      final ReactionProductCandidate candidate, final List<ReactionPredictionEvidence> evidence) {
    final List<ReactionPredictionScoreComponent> components =
        candidate.scoreBreakdown().components();
    for (int i = 0; i < components.size(); ++i) {
      final ReactionPredictionScoreComponent component = components.get(i);
      final ReactionPredictionEvidenceKind kind =
          ReactionPredictionExplainer.evidenceKind(component.kind());
      if (kind == null) {
        continue;
      }
      final String description = "Score component: " + component.description();
      if (ReactionPredictionExplainer.containsEvidence(evidence, kind, description)) {
        continue;
      }
      evidence.add(ReactionPredictionEvidence.of(kind, description, component.weight()));
    }
  }

  private static ReactionPredictionEvidenceKind evidenceKind(
      final ReactionPredictionScoreComponentKind kind) {
    if (kind == ReactionPredictionScoreComponentKind.CALIBRATION) {
      return null;
    }
    if (kind == ReactionPredictionScoreComponentKind.FUNCTIONAL_GROUP) {
      return ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH;
    }
    if (kind == ReactionPredictionScoreComponentKind.HETEROCYCLE) {
      return ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN;
    }
    if (kind == ReactionPredictionScoreComponentKind.STERIC) {
      return ReactionPredictionEvidenceKind.STERIC_FACTOR;
    }
    if (kind == ReactionPredictionScoreComponentKind.ELECTRONIC) {
      return ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR;
    }
    if (kind == ReactionPredictionScoreComponentKind.SOLVENT) {
      return ReactionPredictionEvidenceKind.SOLVENT_EFFECT;
    }
    if (kind == ReactionPredictionScoreComponentKind.TEMPERATURE) {
      return ReactionPredictionEvidenceKind.TEMPERATURE_EFFECT;
    }
    if (kind == ReactionPredictionScoreComponentKind.CATALYST) {
      return ReactionPredictionEvidenceKind.CATALYST_EFFECT;
    }
    if (kind == ReactionPredictionScoreComponentKind.STOICHIOMETRY) {
      return ReactionPredictionEvidenceKind.KINETIC_FACTOR;
    }
    if (kind == ReactionPredictionScoreComponentKind.KINETIC) {
      return ReactionPredictionEvidenceKind.KINETIC_FACTOR;
    }
    if (kind == ReactionPredictionScoreComponentKind.THERMODYNAMIC) {
      return ReactionPredictionEvidenceKind.THERMODYNAMIC_FACTOR;
    }
    if (kind == ReactionPredictionScoreComponentKind.SELECTIVITY) {
      return ReactionPredictionEvidenceKind.COMPETING_PATHWAY;
    }
    if (kind == ReactionPredictionScoreComponentKind.QUANTUM_MODEL) {
      return ReactionPredictionEvidenceKind.QUANTUM_ENERGY_MODEL;
    }
    if (kind == ReactionPredictionScoreComponentKind.WARNING_PENALTY) {
      return ReactionPredictionEvidenceKind.WARNING;
    }
    return ReactionPredictionEvidenceKind.USER_RULE;
  }

  private static boolean containsEvidence(
      final List<ReactionPredictionEvidence> evidence,
      final ReactionPredictionEvidenceKind kind,
      final String description) {
    for (int i = 0; i < evidence.size(); ++i) {
      final ReactionPredictionEvidence item = evidence.get(i);
      if (item.kind() == kind && item.description().equals(description)) {
        return true;
      }
    }
    return false;
  }

  private static ReactionConditionSuitability conditionSuitability(
      final ReactionPredictionResult result, final List<ReactionPredictionEvidence> evidence) {
    final boolean explicitConditions =
        result.request().conditions() != null && !result.request().conditions().empty();
    if (!explicitConditions && evidence.isEmpty()) {
      return ReactionConditionSuitability.unknown();
    }
    final ArrayList<ReactionPredictionEvidence> supporting =
        new ArrayList<ReactionPredictionEvidence>();
    final ArrayList<ReactionPredictionEvidence> limiting =
        new ArrayList<ReactionPredictionEvidence>();
    double positiveWeight = 0.0;
    double negativeWeight = 0.0;
    for (int i = 0; i < evidence.size(); ++i) {
      final ReactionPredictionEvidence item = evidence.get(i);
      if (!ReactionPredictionExplainer.isConditionEvidence(item.kind())
          && !ReactionPredictionExplainer.isConditionWarning(item)) {
        continue;
      }
      if (item.weight() >= 0.0) {
        supporting.add(item);
        positiveWeight += item.weight();
      } else {
        limiting.add(item);
        negativeWeight += Math.abs(item.weight());
      }
    }
    final double rawScore =
        explicitConditions ? 0.45 + positiveWeight - negativeWeight : positiveWeight;
    final boolean stronglyIncompatible =
        negativeWeight >= 0.35
            || limiting.stream()
                .anyMatch(
                    item ->
                        item.description().contains("contradictory")
                            || item.description().contains("lacks inert atmosphere"));
    return ReactionConditionSuitability.of(
        ReactionPredictionExplainer.clamp(rawScore),
        explicitConditions,
        stronglyIncompatible,
        ReactionPredictionExplainer.limitedEvidence(supporting),
        ReactionPredictionExplainer.limitedEvidence(limiting));
  }

  private static List<ReactionPredictionEvidence> limitedEvidence(
      final List<ReactionPredictionEvidence> evidence) {
    evidence.sort(
        Comparator.comparingDouble(
                (final ReactionPredictionEvidence item) -> Math.abs(item.weight()))
            .reversed());
    if (evidence.size() <= MAX_EXPLANATION_EVIDENCE) {
      return List.copyOf(evidence);
    }
    return List.copyOf(evidence.subList(0, MAX_EXPLANATION_EVIDENCE));
  }

  private static boolean isConditionWarning(final ReactionPredictionEvidence evidence) {
    if (evidence.kind() != ReactionPredictionEvidenceKind.WARNING) {
      return false;
    }
    final String description = evidence.description().toLowerCase(java.util.Locale.ROOT);
    return description.contains("condition")
        || description.contains("temperature")
        || description.contains("duration")
        || description.contains("catalyst")
        || description.contains("solvent")
        || description.contains("atmosphere")
        || description.contains("heating")
        || description.contains("cooling")
        || description.contains("acidic")
        || description.contains("basic")
        || description.contains("kinetic");
  }

  private static double clamp(final double value) {
    if (value <= 0.0) {
      return 0.0;
    }
    if (value >= 1.0) {
      return 1.0;
    }
    return value;
  }

  private static List<ReactionPredictionEvidence> sortedLimited(
      final List<ReactionPredictionEvidence> evidence, final boolean positive) {
    final ArrayList<ReactionPredictionEvidence> selected =
        new ArrayList<ReactionPredictionEvidence>();
    for (int i = 0; i < evidence.size(); ++i) {
      final ReactionPredictionEvidence item = evidence.get(i);
      if (positive && item.weight() <= 0.0) {
        continue;
      }
      if (!positive && item.weight() >= 0.0) {
        continue;
      }
      selected.add(item);
    }
    selected.sort(
        Comparator.comparingDouble(
                (final ReactionPredictionEvidence item) -> Math.abs(item.weight()))
            .reversed());
    if (selected.size() <= MAX_EXPLANATION_EVIDENCE) {
      return List.copyOf(selected);
    }
    return ReactionPredictionExplainer.requiredLimited(selected);
  }

  private static List<ReactionPredictionEvidence> requiredLimited(
      final List<ReactionPredictionEvidence> selected) {
    final ArrayList<ReactionPredictionEvidence> limited =
        new ArrayList<ReactionPredictionEvidence>(selected.subList(0, MAX_EXPLANATION_EVIDENCE));
    for (int i = MAX_EXPLANATION_EVIDENCE; i < selected.size(); ++i) {
      final ReactionPredictionEvidence item = selected.get(i);
      if (item.kind() != ReactionPredictionEvidenceKind.QUANTUM_ENERGY_MODEL) {
        continue;
      }
      if (ReactionPredictionExplainer.containsKind(limited, item.kind())) {
        continue;
      }
      limited.set(limited.size() - 1, item);
    }
    limited.sort(
        Comparator.comparingDouble(
                (final ReactionPredictionEvidence item) -> Math.abs(item.weight()))
            .reversed());
    return List.copyOf(limited);
  }

  private static boolean containsKind(
      final List<ReactionPredictionEvidence> evidence, final ReactionPredictionEvidenceKind kind) {
    for (int i = 0; i < evidence.size(); ++i) {
      if (evidence.get(i).kind() == kind) {
        return true;
      }
    }
    return false;
  }
}

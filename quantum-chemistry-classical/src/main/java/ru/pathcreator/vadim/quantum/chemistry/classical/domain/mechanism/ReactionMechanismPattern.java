/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanisticEventKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;

/**
 * Канонический шаблон механизма: семейство, типовые стадии и ограничения применимости.
 */
public final class ReactionMechanismPattern {

  private static final int PRODUCTION_DETAIL_STEP_COUNT = 4;
  private static final int MINIMUM_DETAIL_STEP_COUNT = 3;
  private static final int MINIMUM_SIGNAL_COUNT = 2;

  private final ReactionMechanismFamily family;
  private final String displayName;
  private final List<ReactionMechanismPatternStep> detailedSteps;
  private final List<MechanisticEventKind> eventSequence;
  private final List<String> applicabilitySignals;
  private final List<String> riskSignals;

  private ReactionMechanismPattern(
      final ReactionMechanismFamily family,
      final String displayName,
      final List<ReactionMechanismPatternStep> detailedSteps,
      final List<String> applicabilitySignals,
      final List<String> riskSignals) {
    this.family = family;
    this.displayName = displayName;
    this.detailedSteps = detailedSteps;
    this.eventSequence = ReactionMechanismPattern.eventSequence(detailedSteps);
    this.applicabilitySignals = applicabilitySignals;
    this.riskSignals = riskSignals;
  }

  public static ReactionMechanismPattern of(
      final ReactionMechanismFamily family,
      final String displayName,
      final List<MechanisticEventKind> eventSequence,
      final List<String> applicabilitySignals,
      final List<String> riskSignals) {
    if (family == null) {
      throw new IllegalArgumentException("Reaction mechanism pattern family must not be null.");
    }
    final List<MechanisticEventKind> checkedEvents =
        ReactionMechanismPattern.checkedEvents(eventSequence);
    if (checkedEvents.isEmpty()) {
      throw new IllegalArgumentException(
          "Reaction mechanism pattern event sequence must not be empty.");
    }
    return ReactionMechanismPattern.ofDetailed(
        family,
        displayName,
        ReactionMechanismPattern.stepsFromEvents(
            displayName, checkedEvents, applicabilitySignals, riskSignals),
        applicabilitySignals,
        riskSignals);
  }

  public static ReactionMechanismPattern ofDetailed(
      final ReactionMechanismFamily family,
      final String displayName,
      final List<ReactionMechanismPatternStep> detailedSteps,
      final List<String> applicabilitySignals,
      final List<String> riskSignals) {
    if (family == null) {
      throw new IllegalArgumentException("Reaction mechanism pattern family must not be null.");
    }
    final List<ReactionMechanismPatternStep> checkedSteps =
        ReactionMechanismPattern.checkedDetailedSteps(detailedSteps);
    if (checkedSteps.isEmpty()) {
      throw new IllegalArgumentException(
          "Reaction mechanism pattern detailed steps must not be empty.");
    }
    return new ReactionMechanismPattern(
        family,
        TextValue.requireText(displayName, "Reaction mechanism pattern display name"),
        checkedSteps,
        ReactionMechanismPattern.checkedText(applicabilitySignals, "applicability signal"),
        ReactionMechanismPattern.checkedText(riskSignals, "risk signal"));
  }

  public ReactionMechanismFamily family() {
    return this.family;
  }

  public String displayName() {
    return this.displayName;
  }

  public List<MechanisticEventKind> eventSequence() {
    return this.eventSequence;
  }

  public List<ReactionMechanismPatternStep> detailedSteps() {
    return this.detailedSteps;
  }

  public double pathwayProbabilityContribution() {
    double contribution = 0.0;
    for (int i = 0; i < this.detailedSteps.size(); ++i) {
      contribution += this.detailedSteps.get(i).probabilityContribution();
    }
    return Math.min(1.0, contribution);
  }

  public boolean syntheticGenericPattern() {
    for (int i = 0; i < this.detailedSteps.size(); ++i) {
      final ReactionMechanismPatternStep step = this.detailedSteps.get(i);
      if (!step.stageName().startsWith(this.displayName + " step ")
          || !step.description().startsWith("Canonical ")) {
        return false;
      }
    }
    return true;
  }

  public boolean productionDetailedPattern() {
    return !this.syntheticGenericPattern()
        && this.detailedSteps.size() >= PRODUCTION_DETAIL_STEP_COUNT
        && this.applicabilitySignals.size() >= MINIMUM_SIGNAL_COUNT
        && this.riskSignals.size() >= MINIMUM_SIGNAL_COUNT;
  }

  public boolean chemicallyUsablePattern() {
    return !this.syntheticGenericPattern()
        && this.detailedSteps.size() >= MINIMUM_DETAIL_STEP_COUNT
        && this.applicabilitySignals.size() >= MINIMUM_SIGNAL_COUNT
        && this.riskSignals.size() >= MINIMUM_SIGNAL_COUNT;
  }

  public double detailQualityWeight() {
    if (this.productionDetailedPattern()) {
      return 0.045;
    }
    if (this.chemicallyUsablePattern()) {
      return 0.020;
    }
    if (this.syntheticGenericPattern()) {
      return -0.035;
    }
    return -0.015;
  }

  public List<String> applicabilitySignals() {
    return this.applicabilitySignals;
  }

  public List<String> riskSignals() {
    return this.riskSignals;
  }

  private static List<MechanisticEventKind> checkedEvents(final List<MechanisticEventKind> events) {
    if (events == null || events.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < events.size(); ++i) {
      if (events.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException("Reaction mechanism pattern event must not be null.");
    }
    return List.copyOf(events);
  }

  private static List<ReactionMechanismPatternStep> checkedDetailedSteps(
      final List<ReactionMechanismPatternStep> steps) {
    if (steps == null || steps.isEmpty()) {
      return List.of();
    }
    final ReactionMechanismPatternStep[] checked = new ReactionMechanismPatternStep[steps.size()];
    int previousOrder = -1;
    for (int i = 0; i < steps.size(); ++i) {
      final ReactionMechanismPatternStep step = steps.get(i);
      if (step == null) {
        throw new IllegalArgumentException(
            "Reaction mechanism pattern detailed step must not be null.");
      }
      if (step.orderIndex() <= previousOrder) {
        throw new IllegalArgumentException(
            "Reaction mechanism pattern detailed step order must strictly increase.");
      }
      previousOrder = step.orderIndex();
      checked[i] = step;
    }
    return List.of(checked);
  }

  private static List<ReactionMechanismPatternStep> stepsFromEvents(
      final String displayName,
      final List<MechanisticEventKind> events,
      final List<String> applicabilitySignals,
      final List<String> riskSignals) {
    final ReactionMechanismPatternStep[] steps = new ReactionMechanismPatternStep[events.size()];
    final double contribution = 1.0 / events.size();
    for (int i = 0; i < events.size(); ++i) {
      final MechanisticEventKind event = events.get(i);
      steps[i] =
          ReactionMechanismPatternStep.of(
              i,
              event,
              displayName + " step " + (i + 1),
              "Canonical " + event.name() + " stage in " + displayName + ".",
              contribution,
              applicabilitySignals,
              riskSignals);
    }
    return List.of(steps);
  }

  private static List<MechanisticEventKind> eventSequence(
      final List<ReactionMechanismPatternStep> steps) {
    final MechanisticEventKind[] events = new MechanisticEventKind[steps.size()];
    for (int i = 0; i < steps.size(); ++i) {
      events[i] = steps.get(i).eventKind();
    }
    return List.of(events);
  }

  private static List<String> checkedText(final List<String> values, final String subject) {
    if (values == null || values.isEmpty()) {
      return List.of();
    }
    final String[] checked = new String[values.size()];
    for (int i = 0; i < values.size(); ++i) {
      checked[i] = TextValue.requireText(values.get(i), "Reaction mechanism pattern " + subject);
    }
    return List.of(checked);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionMechanismPattern)) {
      return false;
    }
    final ReactionMechanismPattern pattern = (ReactionMechanismPattern) other;
    return this.family == pattern.family
        && Objects.equals(this.displayName, pattern.displayName)
        && Objects.equals(this.detailedSteps, pattern.detailedSteps)
        && Objects.equals(this.eventSequence, pattern.eventSequence)
        && Objects.equals(this.applicabilitySignals, pattern.applicabilitySignals)
        && Objects.equals(this.riskSignals, pattern.riskSignals);
  }

  public int hashCode() {
    return Objects.hash(
        this.family,
        this.displayName,
        this.detailedSteps,
        this.eventSequence,
        this.applicabilitySignals,
        this.riskSignals);
  }
}

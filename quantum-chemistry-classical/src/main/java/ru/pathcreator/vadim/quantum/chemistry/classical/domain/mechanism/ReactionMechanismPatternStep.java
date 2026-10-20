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

/**
 * Детальная стадия шаблонного механизма с химическим событием, условиями применимости и вкладом в вероятность пути.
 */
public final class ReactionMechanismPatternStep {

  private final int orderIndex;
  private final MechanisticEventKind eventKind;
  private final String stageName;
  private final String description;
  private final double probabilityContribution;
  private final List<String> applicabilitySignals;
  private final List<String> riskSignals;

  private ReactionMechanismPatternStep(
      final int orderIndex,
      final MechanisticEventKind eventKind,
      final String stageName,
      final String description,
      final double probabilityContribution,
      final List<String> applicabilitySignals,
      final List<String> riskSignals) {
    this.orderIndex = orderIndex;
    this.eventKind = eventKind;
    this.stageName = stageName;
    this.description = description;
    this.probabilityContribution = probabilityContribution;
    this.applicabilitySignals = applicabilitySignals;
    this.riskSignals = riskSignals;
  }

  public static ReactionMechanismPatternStep of(
      final int orderIndex,
      final MechanisticEventKind eventKind,
      final String stageName,
      final String description,
      final double probabilityContribution,
      final List<String> applicabilitySignals,
      final List<String> riskSignals) {
    if (orderIndex < 0) {
      throw new IllegalArgumentException(
          "Reaction mechanism pattern step order must be non-negative.");
    }
    if (eventKind == null) {
      throw new IllegalArgumentException(
          "Reaction mechanism pattern step event kind must not be null.");
    }
    if (!Double.isFinite(probabilityContribution)
        || probabilityContribution < 0.0
        || probabilityContribution > 1.0) {
      throw new IllegalArgumentException(
          "Reaction mechanism pattern step probability contribution must be in [0, 1].");
    }
    return new ReactionMechanismPatternStep(
        orderIndex,
        eventKind,
        TextValue.requireText(stageName, "Reaction mechanism pattern step stage name"),
        TextValue.requireText(description, "Reaction mechanism pattern step description"),
        probabilityContribution,
        ReactionMechanismPatternStep.checkedText(applicabilitySignals, "applicability signal"),
        ReactionMechanismPatternStep.checkedText(riskSignals, "risk signal"));
  }

  public int orderIndex() {
    return this.orderIndex;
  }

  public MechanisticEventKind eventKind() {
    return this.eventKind;
  }

  public String stageName() {
    return this.stageName;
  }

  public String description() {
    return this.description;
  }

  public double probabilityContribution() {
    return this.probabilityContribution;
  }

  public List<String> applicabilitySignals() {
    return this.applicabilitySignals;
  }

  public List<String> riskSignals() {
    return this.riskSignals;
  }

  public boolean hasApplicabilitySignals() {
    return !this.applicabilitySignals.isEmpty();
  }

  public boolean hasRiskSignals() {
    return !this.riskSignals.isEmpty();
  }

  private static List<String> checkedText(final List<String> values, final String subjectName) {
    if (values == null || values.isEmpty()) {
      return List.of();
    }
    final String[] checked = new String[values.size()];
    for (int i = 0; i < values.size(); ++i) {
      checked[i] =
          TextValue.requireText(values.get(i), "Reaction mechanism pattern step " + subjectName);
    }
    return List.of(checked);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionMechanismPatternStep)) {
      return false;
    }
    final ReactionMechanismPatternStep step = (ReactionMechanismPatternStep) other;
    return this.orderIndex == step.orderIndex
        && Double.compare(this.probabilityContribution, step.probabilityContribution) == 0
        && this.eventKind == step.eventKind
        && Objects.equals(this.stageName, step.stageName)
        && Objects.equals(this.description, step.description)
        && Objects.equals(this.applicabilitySignals, step.applicabilitySignals)
        && Objects.equals(this.riskSignals, step.riskSignals);
  }

  public int hashCode() {
    return Objects.hash(
        this.orderIndex,
        this.eventKind,
        this.stageName,
        this.description,
        this.probabilityContribution,
        this.applicabilitySignals,
        this.riskSignals);
  }
}

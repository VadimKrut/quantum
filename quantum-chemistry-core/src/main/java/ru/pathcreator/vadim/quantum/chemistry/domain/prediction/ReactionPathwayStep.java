/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.prediction;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanisticEventKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;

/**
 * Один предполагаемый элементарный шаг механизма в объяснении продукта реакции.
 */
public final class ReactionPathwayStep {

  private final String mechanismId;
  private final ReactionMechanismFamily family;
  private final int orderIndex;
  private final MechanisticEventKind eventKind;
  private final String stageName;
  private final String description;
  private final double pathwayProbability;
  private final double probabilityContribution;

  private ReactionPathwayStep(
      final String mechanismId,
      final ReactionMechanismFamily family,
      final int orderIndex,
      final MechanisticEventKind eventKind,
      final String stageName,
      final String description,
      final double pathwayProbability,
      final double probabilityContribution) {
    this.mechanismId = mechanismId;
    this.family = family;
    this.orderIndex = orderIndex;
    this.eventKind = eventKind;
    this.stageName = stageName;
    this.description = description;
    this.pathwayProbability = pathwayProbability;
    this.probabilityContribution = probabilityContribution;
  }

  public static ReactionPathwayStep of(
      final String mechanismId,
      final ReactionMechanismFamily family,
      final int orderIndex,
      final MechanisticEventKind eventKind,
      final String stageName,
      final String description,
      final double pathwayProbability,
      final double probabilityContribution) {
    if (family == null) {
      throw new IllegalArgumentException("Reaction pathway step family must not be null.");
    }
    if (orderIndex < 0) {
      throw new IllegalArgumentException("Reaction pathway step order must be non-negative.");
    }
    if (eventKind == null) {
      throw new IllegalArgumentException("Reaction pathway step event kind must not be null.");
    }
    ReactionPathwayStep.requireProbability(pathwayProbability, "pathway probability");
    ReactionPathwayStep.requireProbability(probabilityContribution, "probability contribution");
    return new ReactionPathwayStep(
        TextValue.requireText(mechanismId, "Reaction pathway step mechanism id"),
        family,
        orderIndex,
        eventKind,
        TextValue.requireText(stageName, "Reaction pathway step stage name"),
        TextValue.requireText(description, "Reaction pathway step description"),
        pathwayProbability,
        probabilityContribution);
  }

  public String mechanismId() {
    return this.mechanismId;
  }

  public ReactionMechanismFamily family() {
    return this.family;
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

  public double pathwayProbability() {
    return this.pathwayProbability;
  }

  public double probabilityContribution() {
    return this.probabilityContribution;
  }

  private static void requireProbability(final double value, final String subjectName) {
    if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
      throw new IllegalArgumentException(
          "Reaction pathway step " + subjectName + " must be in [0, 1].");
    }
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionPathwayStep)) {
      return false;
    }
    final ReactionPathwayStep step = (ReactionPathwayStep) other;
    return this.orderIndex == step.orderIndex
        && Double.compare(this.pathwayProbability, step.pathwayProbability) == 0
        && Double.compare(this.probabilityContribution, step.probabilityContribution) == 0
        && Objects.equals(this.mechanismId, step.mechanismId)
        && this.family == step.family
        && this.eventKind == step.eventKind
        && Objects.equals(this.stageName, step.stageName)
        && Objects.equals(this.description, step.description);
  }

  public int hashCode() {
    return Objects.hash(
        this.mechanismId,
        this.family,
        this.orderIndex,
        this.eventKind,
        this.stageName,
        this.description,
        this.pathwayProbability,
        this.probabilityContribution);
  }
}

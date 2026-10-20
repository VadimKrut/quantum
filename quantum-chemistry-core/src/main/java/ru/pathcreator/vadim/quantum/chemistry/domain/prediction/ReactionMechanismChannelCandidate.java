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
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;

/**
 * Гипотеза реакционного канала, для которого механизм распознан, но явный граф продукта еще не
 * построен.
 */
public final class ReactionMechanismChannelCandidate {

  private final String channelId;
  private final String displayName;
  private final ReactionMechanismFamily family;
  private final double pathwayProbability;
  private final double expectedYieldPercent;
  private final boolean explicitProductGraphAvailable;
  private final String explanation;
  private final String materializationRequirement;

  private ReactionMechanismChannelCandidate(
      final String channelId,
      final String displayName,
      final ReactionMechanismFamily family,
      final double pathwayProbability,
      final double expectedYieldPercent,
      final boolean explicitProductGraphAvailable,
      final String explanation,
      final String materializationRequirement) {
    this.channelId = channelId;
    this.displayName = displayName;
    this.family = family;
    this.pathwayProbability = pathwayProbability;
    this.expectedYieldPercent = expectedYieldPercent;
    this.explicitProductGraphAvailable = explicitProductGraphAvailable;
    this.explanation = explanation;
    this.materializationRequirement = materializationRequirement;
  }

  public static ReactionMechanismChannelCandidate of(
      final String channelId,
      final String displayName,
      final ReactionMechanismFamily family,
      final double pathwayProbability,
      final double expectedYieldPercent,
      final boolean explicitProductGraphAvailable,
      final String explanation,
      final String materializationRequirement) {
    if (family == null) {
      throw new IllegalArgumentException("Reaction mechanism channel family must not be null.");
    }
    if (!Double.isFinite(pathwayProbability)
        || pathwayProbability < 0.0
        || pathwayProbability > 1.0) {
      throw new IllegalArgumentException(
          "Reaction mechanism channel probability must be in [0, 1].");
    }
    if (!Double.isFinite(expectedYieldPercent)
        || expectedYieldPercent < 0.0
        || expectedYieldPercent > 100.0) {
      throw new IllegalArgumentException(
          "Reaction mechanism channel yield percent must be in [0, 100].");
    }
    return new ReactionMechanismChannelCandidate(
        IdentifierValue.requireIdentifier(channelId, "Reaction mechanism channel id"),
        TextValue.requireText(displayName, "Reaction mechanism channel display name"),
        family,
        pathwayProbability,
        expectedYieldPercent,
        explicitProductGraphAvailable,
        TextValue.requireText(explanation, "Reaction mechanism channel explanation"),
        TextValue.requireText(
            materializationRequirement, "Reaction mechanism channel materialization requirement"));
  }

  public String channelId() {
    return this.channelId;
  }

  public String displayName() {
    return this.displayName;
  }

  public ReactionMechanismFamily family() {
    return this.family;
  }

  public double pathwayProbability() {
    return this.pathwayProbability;
  }

  public double expectedYieldPercent() {
    return this.expectedYieldPercent;
  }

  public boolean explicitProductGraphAvailable() {
    return this.explicitProductGraphAvailable;
  }

  public String explanation() {
    return this.explanation;
  }

  public String materializationRequirement() {
    return this.materializationRequirement;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionMechanismChannelCandidate)) {
      return false;
    }
    final ReactionMechanismChannelCandidate candidate = (ReactionMechanismChannelCandidate) other;
    return Double.compare(this.pathwayProbability, candidate.pathwayProbability) == 0
        && Double.compare(this.expectedYieldPercent, candidate.expectedYieldPercent) == 0
        && this.explicitProductGraphAvailable == candidate.explicitProductGraphAvailable
        && Objects.equals(this.channelId, candidate.channelId)
        && Objects.equals(this.displayName, candidate.displayName)
        && this.family == candidate.family
        && Objects.equals(this.explanation, candidate.explanation)
        && Objects.equals(this.materializationRequirement, candidate.materializationRequirement);
  }

  public int hashCode() {
    return Objects.hash(
        this.channelId,
        this.displayName,
        this.family,
        this.pathwayProbability,
        this.expectedYieldPercent,
        this.explicitProductGraphAvailable,
        this.explanation,
        this.materializationRequirement);
  }
}

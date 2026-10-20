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
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;

/**
 * Ссылка кандидата продукта на предполагаемый механизм и его вероятность.
 */
public final class PredictedReactionMechanism {

  private final String mechanismId;
  private final ReactionMechanismFamily family;
  private final double pathwayProbability;
  private final double expectedYieldPercent;
  private final ReactionMechanism detailedMechanism;

  private PredictedReactionMechanism(
      final String mechanismId,
      final ReactionMechanismFamily family,
      final double pathwayProbability,
      final double expectedYieldPercent,
      final ReactionMechanism detailedMechanism) {
    this.mechanismId = mechanismId;
    this.family = family;
    this.pathwayProbability = pathwayProbability;
    this.expectedYieldPercent = expectedYieldPercent;
    this.detailedMechanism = detailedMechanism;
  }

  public static PredictedReactionMechanism of(
      final String mechanismId,
      final ReactionMechanismFamily family,
      final double pathwayProbability,
      final double expectedYieldPercent) {
    if (family == null) {
      throw new IllegalArgumentException("Predicted reaction mechanism family must not be null.");
    }
    if (!Double.isFinite(pathwayProbability)
        || pathwayProbability < 0.0
        || pathwayProbability > 1.0) {
      throw new IllegalArgumentException(
          "Predicted reaction mechanism probability must be in [0, 1].");
    }
    if (!Double.isFinite(expectedYieldPercent)
        || expectedYieldPercent < 0.0
        || expectedYieldPercent > 100.0) {
      throw new IllegalArgumentException(
          "Predicted reaction mechanism expected yield percent must be in [0, 100].");
    }
    return new PredictedReactionMechanism(
        IdentifierValue.requireIdentifier(mechanismId, "Predicted reaction mechanism id"),
        family,
        pathwayProbability,
        expectedYieldPercent,
        null);
  }

  public static PredictedReactionMechanism of(
      final String mechanismId,
      final ReactionMechanismFamily family,
      final double pathwayProbability,
      final double expectedYieldPercent,
      final ReactionMechanism detailedMechanism) {
    if (detailedMechanism == null) {
      return PredictedReactionMechanism.of(
          mechanismId, family, pathwayProbability, expectedYieldPercent);
    }
    if (family != null && detailedMechanism.family() != family) {
      throw new IllegalArgumentException(
          "Predicted reaction mechanism detail family must match predicted family.");
    }
    PredictedReactionMechanism.requireValues(family, pathwayProbability, expectedYieldPercent);
    return new PredictedReactionMechanism(
        IdentifierValue.requireIdentifier(mechanismId, "Predicted reaction mechanism id"),
        family,
        pathwayProbability,
        expectedYieldPercent,
        detailedMechanism);
  }

  private static void requireValues(
      final ReactionMechanismFamily family,
      final double pathwayProbability,
      final double expectedYieldPercent) {
    if (family == null) {
      throw new IllegalArgumentException("Predicted reaction mechanism family must not be null.");
    }
    if (!Double.isFinite(pathwayProbability)
        || pathwayProbability < 0.0
        || pathwayProbability > 1.0) {
      throw new IllegalArgumentException(
          "Predicted reaction mechanism probability must be in [0, 1].");
    }
    if (!Double.isFinite(expectedYieldPercent)
        || expectedYieldPercent < 0.0
        || expectedYieldPercent > 100.0) {
      throw new IllegalArgumentException(
          "Predicted reaction mechanism expected yield percent must be in [0, 100].");
    }
  }

  public String mechanismId() {
    return this.mechanismId;
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

  public boolean hasDetailedMechanism() {
    return this.detailedMechanism != null;
  }

  public ReactionMechanism detailedMechanism() {
    return this.detailedMechanism;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof PredictedReactionMechanism)) {
      return false;
    }
    final PredictedReactionMechanism mechanism = (PredictedReactionMechanism) other;
    return Double.compare(this.pathwayProbability, mechanism.pathwayProbability) == 0
        && Double.compare(this.expectedYieldPercent, mechanism.expectedYieldPercent) == 0
        && Objects.equals(this.mechanismId, mechanism.mechanismId)
        && this.family == mechanism.family
        && Objects.equals(this.detailedMechanism, mechanism.detailedMechanism);
  }

  public int hashCode() {
    return Objects.hash(
        this.mechanismId,
        this.family,
        this.pathwayProbability,
        this.expectedYieldPercent,
        this.detailedMechanism);
  }
}

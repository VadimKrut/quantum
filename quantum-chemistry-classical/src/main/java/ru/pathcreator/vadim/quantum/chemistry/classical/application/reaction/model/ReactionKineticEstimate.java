/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.reaction.model;

/**
 * Расчетная кинетическая оценка реакции при заданных условиях.
 */
public final class ReactionKineticEstimate {

  private final double forwardBarrierKilojoulePerMole;
  private final double reverseBarrierKilojoulePerMole;
  private final double relativeForwardRate;
  private final double relativeReverseRate;
  private final double forwardReverseRateRatio;
  private final double arrheniusPreExponentialFactor;
  private final String arrheniusUnit;
  private final String kineticModel;
  private final String kineticContext;
  private final double arrheniusForwardRateConstant;
  private final double eyringActivationEntropyJoulePerMoleKelvin;
  private final double eyringForwardRateConstant;
  private final double firstOrderHalfLifeSeconds;
  private final Double durationConversionFraction;

  private ReactionKineticEstimate(
      final double forwardBarrierKilojoulePerMole,
      final double reverseBarrierKilojoulePerMole,
      final double relativeForwardRate,
      final double relativeReverseRate,
      final double forwardReverseRateRatio,
      final double arrheniusPreExponentialFactor,
      final String arrheniusUnit,
      final String kineticModel,
      final String kineticContext,
      final double arrheniusForwardRateConstant,
      final double eyringActivationEntropyJoulePerMoleKelvin,
      final double eyringForwardRateConstant,
      final double firstOrderHalfLifeSeconds,
      final Double durationConversionFraction) {
    this.forwardBarrierKilojoulePerMole = forwardBarrierKilojoulePerMole;
    this.reverseBarrierKilojoulePerMole = reverseBarrierKilojoulePerMole;
    this.relativeForwardRate = relativeForwardRate;
    this.relativeReverseRate = relativeReverseRate;
    this.forwardReverseRateRatio = forwardReverseRateRatio;
    this.arrheniusPreExponentialFactor = arrheniusPreExponentialFactor;
    this.arrheniusUnit = arrheniusUnit;
    this.kineticModel = kineticModel;
    this.kineticContext = kineticContext;
    this.arrheniusForwardRateConstant = arrheniusForwardRateConstant;
    this.eyringActivationEntropyJoulePerMoleKelvin = eyringActivationEntropyJoulePerMoleKelvin;
    this.eyringForwardRateConstant = eyringForwardRateConstant;
    this.firstOrderHalfLifeSeconds = firstOrderHalfLifeSeconds;
    this.durationConversionFraction = durationConversionFraction;
  }

  public static ReactionKineticEstimate of(
      final double forwardBarrierKilojoulePerMole,
      final double reverseBarrierKilojoulePerMole,
      final double relativeForwardRate,
      final double relativeReverseRate,
      final double forwardReverseRateRatio,
      final double arrheniusPreExponentialFactor,
      final String arrheniusUnit,
      final String kineticModel,
      final String kineticContext,
      final double arrheniusForwardRateConstant,
      final double eyringActivationEntropyJoulePerMoleKelvin,
      final double eyringForwardRateConstant,
      final double firstOrderHalfLifeSeconds,
      final Double durationConversionFraction) {
    ReactionKineticEstimate.requireFinitePositive(
        forwardBarrierKilojoulePerMole, "Forward barrier");
    ReactionKineticEstimate.requireFinitePositive(
        reverseBarrierKilojoulePerMole, "Reverse barrier");
    ReactionKineticEstimate.requireFiniteNonNegative(relativeForwardRate, "Relative forward rate");
    ReactionKineticEstimate.requireFiniteNonNegative(relativeReverseRate, "Relative reverse rate");
    ReactionKineticEstimate.requireFinitePositive(
        forwardReverseRateRatio, "Forward/reverse rate ratio");
    ReactionKineticEstimate.requireFinitePositive(
        arrheniusPreExponentialFactor, "Arrhenius pre-exponential factor");
    if (arrheniusUnit == null || arrheniusUnit.isBlank()) {
      throw new IllegalArgumentException("Arrhenius unit must not be blank.");
    }
    if (kineticModel == null || kineticModel.isBlank()) {
      throw new IllegalArgumentException("Kinetic model must not be blank.");
    }
    if (kineticContext == null || kineticContext.isBlank()) {
      throw new IllegalArgumentException("Kinetic context must not be blank.");
    }
    ReactionKineticEstimate.requireFinitePositive(
        arrheniusForwardRateConstant, "Arrhenius forward rate constant");
    ReactionKineticEstimate.requireFinite(
        eyringActivationEntropyJoulePerMoleKelvin, "Eyring activation entropy");
    ReactionKineticEstimate.requireFinitePositive(
        eyringForwardRateConstant, "Eyring forward rate constant");
    ReactionKineticEstimate.requireFinitePositive(
        firstOrderHalfLifeSeconds, "First-order half-life");
    if (durationConversionFraction != null) {
      ReactionKineticEstimate.requireFinite(
          durationConversionFraction.doubleValue(), "Duration conversion");
      if (durationConversionFraction.doubleValue() < 0.0
          || durationConversionFraction.doubleValue() > 1.0) {
        throw new IllegalArgumentException("Duration conversion must be in [0, 1].");
      }
    }
    return new ReactionKineticEstimate(
        forwardBarrierKilojoulePerMole,
        reverseBarrierKilojoulePerMole,
        relativeForwardRate,
        relativeReverseRate,
        forwardReverseRateRatio,
        arrheniusPreExponentialFactor,
        arrheniusUnit,
        kineticModel,
        kineticContext,
        arrheniusForwardRateConstant,
        eyringActivationEntropyJoulePerMoleKelvin,
        eyringForwardRateConstant,
        firstOrderHalfLifeSeconds,
        durationConversionFraction);
  }

  public double forwardBarrierKilojoulePerMole() {
    return this.forwardBarrierKilojoulePerMole;
  }

  public double reverseBarrierKilojoulePerMole() {
    return this.reverseBarrierKilojoulePerMole;
  }

  public double relativeForwardRate() {
    return this.relativeForwardRate;
  }

  public double relativeReverseRate() {
    return this.relativeReverseRate;
  }

  public double forwardReverseRateRatio() {
    return this.forwardReverseRateRatio;
  }

  public double arrheniusPreExponentialFactor() {
    return this.arrheniusPreExponentialFactor;
  }

  public String arrheniusUnit() {
    return this.arrheniusUnit;
  }

  public String kineticModel() {
    return this.kineticModel;
  }

  public String kineticContext() {
    return this.kineticContext;
  }

  public double arrheniusForwardRateConstant() {
    return this.arrheniusForwardRateConstant;
  }

  public double eyringActivationEntropyJoulePerMoleKelvin() {
    return this.eyringActivationEntropyJoulePerMoleKelvin;
  }

  public double eyringForwardRateConstant() {
    return this.eyringForwardRateConstant;
  }

  public double firstOrderHalfLifeSeconds() {
    return this.firstOrderHalfLifeSeconds;
  }

  public boolean hasDurationConversion() {
    return this.durationConversionFraction != null;
  }

  public double durationConversionFraction() {
    if (this.durationConversionFraction == null) {
      throw new IllegalStateException("Reaction kinetic estimate has no duration conversion.");
    }
    return this.durationConversionFraction.doubleValue();
  }

  private static void requireFinite(final double value, final String subjectName) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException(subjectName + " must be finite.");
    }
  }

  private static void requireFinitePositive(final double value, final String subjectName) {
    ReactionKineticEstimate.requireFinite(value, subjectName);
    if (value <= 0.0) {
      throw new IllegalArgumentException(subjectName + " must be positive.");
    }
  }

  private static void requireFiniteNonNegative(final double value, final String subjectName) {
    ReactionKineticEstimate.requireFinite(value, subjectName);
    if (value < 0.0) {
      throw new IllegalArgumentException(subjectName + " must not be negative.");
    }
  }
}

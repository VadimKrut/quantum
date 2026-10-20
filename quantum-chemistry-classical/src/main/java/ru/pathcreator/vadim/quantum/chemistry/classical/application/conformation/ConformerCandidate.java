/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.conformation;

/** Результат одной точки torsion scan с относительной оценкой напряжения. */
final class ConformerCandidate {

  private final RotatableTorsion torsion;
  private final double angleDegrees;
  private final double score;
  private final double torsionScore;
  private final double stericScore;
  private final double electronicEnergyHartree;
  private final double relativeEnergyHartree;
  private final double boltzmannWeight;

  private ConformerCandidate(
      final RotatableTorsion torsion,
      final double angleDegrees,
      final double score,
      final double torsionScore,
      final double stericScore,
      final double electronicEnergyHartree,
      final double relativeEnergyHartree,
      final double boltzmannWeight) {
    this.torsion = torsion;
    this.angleDegrees = angleDegrees;
    this.score = score;
    this.torsionScore = torsionScore;
    this.stericScore = stericScore;
    this.electronicEnergyHartree = electronicEnergyHartree;
    this.relativeEnergyHartree = relativeEnergyHartree;
    this.boltzmannWeight = boltzmannWeight;
  }

  static ConformerCandidate of(
      final RotatableTorsion torsion,
      final double angleDegrees,
      final double score,
      final double torsionScore,
      final double stericScore,
      final double electronicEnergyHartree) {
    if (torsion == null) {
      throw new IllegalArgumentException("Conformer candidate torsion must not be null.");
    }
    if (!Double.isFinite(angleDegrees)
        || !Double.isFinite(score)
        || !Double.isFinite(torsionScore)
        || !Double.isFinite(stericScore)
        || !Double.isFinite(electronicEnergyHartree)) {
      throw new IllegalArgumentException("Conformer candidate values must be finite.");
    }
    return new ConformerCandidate(
        torsion, angleDegrees, score, torsionScore, stericScore, electronicEnergyHartree, 0.0, 1.0);
  }

  ConformerCandidate withThermodynamicRanking(
      final double relativeEnergyHartree, final double boltzmannWeight) {
    if (!Double.isFinite(relativeEnergyHartree) || relativeEnergyHartree < 0.0) {
      throw new IllegalArgumentException(
          "Conformer relative energy must be finite and non-negative.");
    }
    if (!Double.isFinite(boltzmannWeight) || boltzmannWeight < 0.0) {
      throw new IllegalArgumentException(
          "Conformer Boltzmann weight must be finite and non-negative.");
    }
    return new ConformerCandidate(
        this.torsion,
        this.angleDegrees,
        this.score,
        this.torsionScore,
        this.stericScore,
        this.electronicEnergyHartree,
        relativeEnergyHartree,
        boltzmannWeight);
  }

  static int compareByScore(final ConformerCandidate first, final ConformerCandidate second) {
    final int scoreComparison = Double.compare(first.score, second.score);
    if (scoreComparison != 0) {
      return scoreComparison;
    }
    return Double.compare(Math.abs(first.angleDegrees), Math.abs(second.angleDegrees));
  }

  RotatableTorsion torsion() {
    return this.torsion;
  }

  double angleDegrees() {
    return this.angleDegrees;
  }

  double score() {
    return this.score;
  }

  double torsionScore() {
    return this.torsionScore;
  }

  double stericScore() {
    return this.stericScore;
  }

  double electronicEnergyHartree() {
    return this.electronicEnergyHartree;
  }

  double relativeEnergyHartree() {
    return this.relativeEnergyHartree;
  }

  double boltzmannWeight() {
    return this.boltzmannWeight;
  }
}

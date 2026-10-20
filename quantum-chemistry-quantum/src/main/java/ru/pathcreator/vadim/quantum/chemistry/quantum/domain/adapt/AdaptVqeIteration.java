/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.domain.adapt;

import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.UccsdExcitation;

/**
 * Один шаг ADAPT-VQE отбора: лучший кандидат, энергия и принятая амплитуда.
 */
public final class AdaptVqeIteration {

  private final int index;
  private final UccsdExcitation selectedExcitation;
  private final double previousEnergy;
  private final double selectedEnergy;
  private final double energyImprovement;
  private final double selectedAmplitude;
  private final boolean accepted;
  private final long evaluatedCandidateCount;

  private AdaptVqeIteration(
      final int index,
      final UccsdExcitation selectedExcitation,
      final double previousEnergy,
      final double selectedEnergy,
      final double energyImprovement,
      final double selectedAmplitude,
      final boolean accepted,
      final long evaluatedCandidateCount) {
    this.index = index;
    this.selectedExcitation = selectedExcitation;
    this.previousEnergy = previousEnergy;
    this.selectedEnergy = selectedEnergy;
    this.energyImprovement = energyImprovement;
    this.selectedAmplitude = selectedAmplitude;
    this.accepted = accepted;
    this.evaluatedCandidateCount = evaluatedCandidateCount;
  }

  public static AdaptVqeIteration of(
      final int index,
      final UccsdExcitation selectedExcitation,
      final double previousEnergy,
      final double selectedEnergy,
      final double energyImprovement,
      final double selectedAmplitude,
      final boolean accepted,
      final long evaluatedCandidateCount) {
    if (index < 0) {
      throw new IllegalArgumentException("ADAPT-VQE iteration index must not be negative.");
    }
    if (selectedExcitation == null && accepted) {
      throw new IllegalArgumentException(
          "Accepted ADAPT-VQE iteration requires selected excitation.");
    }
    if (!Double.isFinite(previousEnergy)) {
      throw new IllegalArgumentException("ADAPT-VQE previous energy must be finite.");
    }
    if (!Double.isFinite(selectedEnergy)) {
      throw new IllegalArgumentException("ADAPT-VQE selected energy must be finite.");
    }
    if (!Double.isFinite(energyImprovement)) {
      throw new IllegalArgumentException("ADAPT-VQE energy improvement must be finite.");
    }
    if (!Double.isFinite(selectedAmplitude)) {
      throw new IllegalArgumentException("ADAPT-VQE selected amplitude must be finite.");
    }
    if (evaluatedCandidateCount < 0L) {
      throw new IllegalArgumentException(
          "ADAPT-VQE evaluated candidate count must not be negative.");
    }
    return new AdaptVqeIteration(
        index,
        selectedExcitation,
        previousEnergy,
        selectedEnergy,
        energyImprovement,
        selectedAmplitude,
        accepted,
        evaluatedCandidateCount);
  }

  public int index() {
    return this.index;
  }

  public UccsdExcitation selectedExcitation() {
    return this.selectedExcitation;
  }

  public double previousEnergy() {
    return this.previousEnergy;
  }

  public double selectedEnergy() {
    return this.selectedEnergy;
  }

  public double energyImprovement() {
    return this.energyImprovement;
  }

  public double selectedAmplitude() {
    return this.selectedAmplitude;
  }

  public boolean accepted() {
    return this.accepted;
  }

  public long evaluatedCandidateCount() {
    return this.evaluatedCandidateCount;
  }
}

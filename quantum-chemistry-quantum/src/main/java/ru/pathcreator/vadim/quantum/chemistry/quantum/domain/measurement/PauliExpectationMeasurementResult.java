/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.domain.measurement;

import java.util.Arrays;

/**
 * Результат восстановления expectation value Hamiltonian из измерений Pauli-термов.
 */
public final class PauliExpectationMeasurementResult {

  private final double energy;
  private final double identityOffset;
  private final double[] termExpectations;
  private final double[] termContributions;
  private final long[] termShots;

  private PauliExpectationMeasurementResult(
      final double energy,
      final double identityOffset,
      final double[] termExpectations,
      final double[] termContributions,
      final long[] termShots) {
    this.energy = energy;
    this.identityOffset = identityOffset;
    this.termExpectations = termExpectations;
    this.termContributions = termContributions;
    this.termShots = termShots;
  }

  public static PauliExpectationMeasurementResult of(
      final double energy,
      final double identityOffset,
      final double[] termExpectations,
      final double[] termContributions,
      final long[] termShots) {
    if (!Double.isFinite(energy)) {
      throw new IllegalArgumentException("Pauli measurement energy must be finite.");
    }
    if (!Double.isFinite(identityOffset)) {
      throw new IllegalArgumentException("Pauli measurement identity offset must be finite.");
    }
    if (termExpectations == null || termContributions == null || termShots == null) {
      throw new IllegalArgumentException("Pauli measurement term arrays must not be null.");
    }
    if (termExpectations.length != termContributions.length
        || termExpectations.length != termShots.length) {
      throw new IllegalArgumentException("Pauli measurement term arrays must have equal length.");
    }
    final double[] checkedExpectations = Arrays.copyOf(termExpectations, termExpectations.length);
    final double[] checkedContributions =
        Arrays.copyOf(termContributions, termContributions.length);
    final long[] checkedShots = Arrays.copyOf(termShots, termShots.length);
    for (int i = 0; i < checkedExpectations.length; ++i) {
      if (!Double.isFinite(checkedExpectations[i])
          || checkedExpectations[i] < -1.0
          || checkedExpectations[i] > 1.0) {
        throw new IllegalArgumentException(
            "Pauli measurement term expectation must be in [-1, 1].");
      }
      if (!Double.isFinite(checkedContributions[i])) {
        throw new IllegalArgumentException("Pauli measurement term contribution must be finite.");
      }
      if (checkedShots[i] <= 0L) {
        throw new IllegalArgumentException("Pauli measurement term shots must be positive.");
      }
    }
    return new PauliExpectationMeasurementResult(
        energy, identityOffset, checkedExpectations, checkedContributions, checkedShots);
  }

  public double energy() {
    return this.energy;
  }

  public double identityOffset() {
    return this.identityOffset;
  }

  public int measuredTermCount() {
    return this.termExpectations.length;
  }

  public double termExpectation(final int index) {
    this.requireIndex(index);
    return this.termExpectations[index];
  }

  public double termContribution(final int index) {
    this.requireIndex(index);
    return this.termContributions[index];
  }

  public long termShots(final int index) {
    this.requireIndex(index);
    return this.termShots[index];
  }

  public double[] termExpectationsCopy() {
    return Arrays.copyOf(this.termExpectations, this.termExpectations.length);
  }

  public double[] termContributionsCopy() {
    return Arrays.copyOf(this.termContributions, this.termContributions.length);
  }

  public long[] termShotsCopy() {
    return Arrays.copyOf(this.termShots, this.termShots.length);
  }

  private void requireIndex(final int index) {
    if (index < 0 || index >= this.termExpectations.length) {
      throw new IndexOutOfBoundsException("Pauli measurement term index is outside result.");
    }
  }
}

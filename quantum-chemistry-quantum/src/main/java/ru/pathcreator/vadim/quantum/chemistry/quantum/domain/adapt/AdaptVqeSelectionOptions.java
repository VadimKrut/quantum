/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.domain.adapt;

/**
 * Неизменяемые настройки отбора операторов ADAPT-VQE.
 */
public final class AdaptVqeSelectionOptions {

  private static final int DEFAULT_MAX_SELECTED_OPERATORS = 12;
  private static final int DEFAULT_MAX_CANDIDATE_OPERATORS = 64;
  private static final int DEFAULT_SHOTS = 4096;
  private static final double DEFAULT_PROBE_AMPLITUDE = 0.05;
  private static final double DEFAULT_ENERGY_IMPROVEMENT_TOLERANCE = 1.0E-6;
  private static final int DEFAULT_MAX_BACKEND_RETRIES = 1;

  private final int maxSelectedOperators;
  private final int maxCandidateOperators;
  private final int shots;
  private final double probeAmplitude;
  private final double energyImprovementTolerance;
  private final int maxBackendRetries;

  private AdaptVqeSelectionOptions(
      final int maxSelectedOperators,
      final int maxCandidateOperators,
      final int shots,
      final double probeAmplitude,
      final double energyImprovementTolerance,
      final int maxBackendRetries) {
    this.maxSelectedOperators = maxSelectedOperators;
    this.maxCandidateOperators = maxCandidateOperators;
    this.shots = shots;
    this.probeAmplitude = probeAmplitude;
    this.energyImprovementTolerance = energyImprovementTolerance;
    this.maxBackendRetries = maxBackendRetries;
  }

  public static AdaptVqeSelectionOptions defaults() {
    return AdaptVqeSelectionOptions.of(
        DEFAULT_MAX_SELECTED_OPERATORS,
        DEFAULT_MAX_CANDIDATE_OPERATORS,
        DEFAULT_SHOTS,
        DEFAULT_PROBE_AMPLITUDE,
        DEFAULT_ENERGY_IMPROVEMENT_TOLERANCE,
        DEFAULT_MAX_BACKEND_RETRIES);
  }

  public static AdaptVqeSelectionOptions of(
      final int maxSelectedOperators,
      final int maxCandidateOperators,
      final int shots,
      final double probeAmplitude,
      final double energyImprovementTolerance,
      final int maxBackendRetries) {
    if (maxSelectedOperators <= 0) {
      throw new IllegalArgumentException("ADAPT-VQE selected operator limit must be positive.");
    }
    if (maxCandidateOperators <= 0) {
      throw new IllegalArgumentException("ADAPT-VQE candidate operator limit must be positive.");
    }
    if (maxCandidateOperators < maxSelectedOperators) {
      throw new IllegalArgumentException(
          "ADAPT-VQE candidate limit must not be smaller than selected limit.");
    }
    if (shots <= 0) {
      throw new IllegalArgumentException("ADAPT-VQE shot count must be positive.");
    }
    if (!Double.isFinite(probeAmplitude) || probeAmplitude == 0.0) {
      throw new IllegalArgumentException("ADAPT-VQE probe amplitude must be finite and non-zero.");
    }
    if (!Double.isFinite(energyImprovementTolerance) || energyImprovementTolerance <= 0.0) {
      throw new IllegalArgumentException(
          "ADAPT-VQE energy improvement tolerance must be finite and positive.");
    }
    if (maxBackendRetries < 0) {
      throw new IllegalArgumentException("ADAPT-VQE backend retry count must not be negative.");
    }
    return new AdaptVqeSelectionOptions(
        maxSelectedOperators,
        maxCandidateOperators,
        shots,
        probeAmplitude,
        energyImprovementTolerance,
        maxBackendRetries);
  }

  public int maxSelectedOperators() {
    return this.maxSelectedOperators;
  }

  public int maxCandidateOperators() {
    return this.maxCandidateOperators;
  }

  public int shots() {
    return this.shots;
  }

  public double probeAmplitude() {
    return this.probeAmplitude;
  }

  public double energyImprovementTolerance() {
    return this.energyImprovementTolerance;
  }

  public int maxBackendRetries() {
    return this.maxBackendRetries;
  }
}

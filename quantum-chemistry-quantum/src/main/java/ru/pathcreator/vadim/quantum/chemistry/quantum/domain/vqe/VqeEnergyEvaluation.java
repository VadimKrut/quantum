/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe;

/**
 * Одно измерение энергии ansatz-программы на backend или локальном estimator.
 */
public final class VqeEnergyEvaluation {

  private final double energy;
  private final double uncertainty;
  private final int shots;
  private final boolean successful;
  private final String diagnostic;

  private VqeEnergyEvaluation(
      final double energy,
      final double uncertainty,
      final int shots,
      final boolean successful,
      final String diagnostic) {
    this.energy = energy;
    this.uncertainty = uncertainty;
    this.shots = shots;
    this.successful = successful;
    this.diagnostic = diagnostic;
  }

  public static VqeEnergyEvaluation success(
      final double energy, final double uncertainty, final int shots) {
    if (!Double.isFinite(energy)) {
      throw new IllegalArgumentException("VQE energy must be finite.");
    }
    if (!Double.isFinite(uncertainty) || uncertainty < 0.0) {
      throw new IllegalArgumentException("VQE uncertainty must be finite and non-negative.");
    }
    if (shots <= 0) {
      throw new IllegalArgumentException("VQE shot count must be positive.");
    }
    return new VqeEnergyEvaluation(energy, uncertainty, shots, true, "ok");
  }

  public static VqeEnergyEvaluation failure(final String diagnostic) {
    if (diagnostic == null || diagnostic.isBlank()) {
      throw new IllegalArgumentException("VQE failure diagnostic must not be blank.");
    }
    return new VqeEnergyEvaluation(Double.NaN, Double.NaN, 0, false, diagnostic);
  }

  public double energy() {
    return this.energy;
  }

  public double uncertainty() {
    return this.uncertainty;
  }

  public int shots() {
    return this.shots;
  }

  public boolean successful() {
    return this.successful;
  }

  public String diagnostic() {
    return this.diagnostic;
  }
}

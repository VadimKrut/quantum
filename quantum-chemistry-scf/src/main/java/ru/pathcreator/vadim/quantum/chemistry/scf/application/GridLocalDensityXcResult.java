/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.scf.application;

/**
 * Результат численного LDA exchange-correlation интегрирования.
 */
public final class GridLocalDensityXcResult {

  private final double exchangeEnergy;
  private final double correlationEnergy;
  private final double integratedElectronCount;
  private final double[] potentialMatrix;

  private GridLocalDensityXcResult(
      final double exchangeEnergy,
      final double correlationEnergy,
      final double integratedElectronCount,
      final double[] potentialMatrix) {
    this.exchangeEnergy = exchangeEnergy;
    this.correlationEnergy = correlationEnergy;
    this.integratedElectronCount = integratedElectronCount;
    this.potentialMatrix = potentialMatrix;
  }

  public static GridLocalDensityXcResult of(
      final double exchangeEnergy,
      final double correlationEnergy,
      final double integratedElectronCount,
      final double[] potentialMatrix) {
    GridLocalDensityXcResult.requireFinite(exchangeEnergy, "DFT grid exchange energy");
    GridLocalDensityXcResult.requireFinite(correlationEnergy, "DFT grid correlation energy");
    GridLocalDensityXcResult.requireFinite(
        integratedElectronCount, "DFT grid integrated electron count");
    if (potentialMatrix == null || potentialMatrix.length == 0) {
      throw new IllegalArgumentException("DFT grid potential matrix must not be empty.");
    }
    for (int i = 0; i < potentialMatrix.length; ++i) {
      GridLocalDensityXcResult.requireFinite(potentialMatrix[i], "DFT grid potential matrix value");
    }
    return new GridLocalDensityXcResult(
        exchangeEnergy, correlationEnergy, integratedElectronCount, potentialMatrix.clone());
  }

  public double exchangeEnergy() {
    return this.exchangeEnergy;
  }

  public double correlationEnergy() {
    return this.correlationEnergy;
  }

  public double exchangeCorrelationEnergy() {
    return this.exchangeEnergy + this.correlationEnergy;
  }

  public double integratedElectronCount() {
    return this.integratedElectronCount;
  }

  public double[] potentialMatrixCopy() {
    return this.potentialMatrix.clone();
  }

  private static void requireFinite(final double value, final String subject) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException(subject + " must be finite.");
    }
  }
}

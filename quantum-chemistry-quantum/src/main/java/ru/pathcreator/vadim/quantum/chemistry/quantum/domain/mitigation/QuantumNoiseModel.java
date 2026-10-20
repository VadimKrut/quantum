/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;

/**
 * Описание шума backend или симуляции без привязки к конкретному поставщику.
 */
public final class QuantumNoiseModel {

  private final QuantumNoiseModelKind kind;
  private final double singleQubitErrorRate;
  private final double twoQubitErrorRate;
  private final double readoutErrorRate;

  private QuantumNoiseModel(
      final QuantumNoiseModelKind kind,
      final double singleQubitErrorRate,
      final double twoQubitErrorRate,
      final double readoutErrorRate) {
    this.kind = kind;
    this.singleQubitErrorRate = singleQubitErrorRate;
    this.twoQubitErrorRate = twoQubitErrorRate;
    this.readoutErrorRate = readoutErrorRate;
  }

  public static QuantumNoiseModel ideal() {
    return QuantumNoiseModel.of(QuantumNoiseModelKind.IDEAL, 0.0, 0.0, 0.0);
  }

  public static QuantumNoiseModel unknownHardware() {
    return QuantumNoiseModel.of(QuantumNoiseModelKind.HARDWARE_UNKNOWN, 0.0, 0.0, 0.0);
  }

  public static QuantumNoiseModel of(
      final QuantumNoiseModelKind kind,
      final double singleQubitErrorRate,
      final double twoQubitErrorRate,
      final double readoutErrorRate) {
    if (kind == null) {
      throw new IllegalArgumentException("Quantum noise model kind must not be null.");
    }
    QuantumNoiseModel.requireProbability(singleQubitErrorRate, "single-qubit error rate");
    QuantumNoiseModel.requireProbability(twoQubitErrorRate, "two-qubit error rate");
    QuantumNoiseModel.requireProbability(readoutErrorRate, "readout error rate");
    return new QuantumNoiseModel(kind, singleQubitErrorRate, twoQubitErrorRate, readoutErrorRate);
  }

  public QuantumNoiseModelKind kind() {
    return this.kind;
  }

  public double singleQubitErrorRate() {
    return this.singleQubitErrorRate;
  }

  public double twoQubitErrorRate() {
    return this.twoQubitErrorRate;
  }

  public double readoutErrorRate() {
    return this.readoutErrorRate;
  }

  public boolean isIdeal() {
    return this.kind == QuantumNoiseModelKind.IDEAL
        && this.singleQubitErrorRate == 0.0
        && this.twoQubitErrorRate == 0.0
        && this.readoutErrorRate == 0.0;
  }

  private static void requireProbability(final double value, final String subject) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException("Quantum noise " + subject + " must be finite.");
    }
    if (value < 0.0 || value > 1.0) {
      throw new IllegalArgumentException("Quantum noise " + subject + " must be in [0, 1].");
    }
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof QuantumNoiseModel)) {
      return false;
    }
    final QuantumNoiseModel model = (QuantumNoiseModel) other;
    return this.kind == model.kind
        && Double.compare(this.singleQubitErrorRate, model.singleQubitErrorRate) == 0
        && Double.compare(this.twoQubitErrorRate, model.twoQubitErrorRate) == 0
        && Double.compare(this.readoutErrorRate, model.readoutErrorRate) == 0;
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.kind);
    result = ChemistryHash.include(result, this.singleQubitErrorRate);
    result = ChemistryHash.include(result, this.twoQubitErrorRate);
    result = ChemistryHash.include(result, this.readoutErrorRate);
    return result;
  }
}

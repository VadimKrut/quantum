/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.reaction;

public final class StoichiometricEquivalent {

  private final double value;

  private StoichiometricEquivalent(final double value) {
    this.value = value;
  }

  public static StoichiometricEquivalent of(final double value) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException("Stoichiometric equivalent must be finite.");
    }
    if (value <= 0.0) {
      throw new IllegalArgumentException("Stoichiometric equivalent must be positive.");
    }
    return new StoichiometricEquivalent(value);
  }

  public double value() {
    return this.value;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof StoichiometricEquivalent)) {
      return false;
    }
    final StoichiometricEquivalent equivalent = (StoichiometricEquivalent) other;
    return Double.compare(this.value, equivalent.value) == 0;
  }

  public int hashCode() {
    return Double.hashCode(this.value);
  }
}
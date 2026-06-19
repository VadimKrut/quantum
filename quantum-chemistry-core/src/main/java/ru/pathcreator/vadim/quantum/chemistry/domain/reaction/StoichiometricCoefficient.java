/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.reaction;

public final class StoichiometricCoefficient {

  public static final StoichiometricCoefficient ONE = new StoichiometricCoefficient(1);
  private static final int MAX_VALUE = 1000000;
  private final int value;

  private StoichiometricCoefficient(final int value) {
    this.value = value;
  }

  public static StoichiometricCoefficient of(final int value) {
    if (value < 1 || value > MAX_VALUE) {
      throw new IllegalArgumentException("Stoichiometric coefficient is outside supported bounds.");
    }
    if (value == 1) {
      return ONE;
    }
    return new StoichiometricCoefficient(value);
  }

  public int value() {
    return this.value;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof StoichiometricCoefficient)) {
      return false;
    }
    final StoichiometricCoefficient coefficient = (StoichiometricCoefficient) other;
    return this.value == coefficient.value;
  }

  public int hashCode() {
    return this.value;
  }
}
/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.acidbase;

public final class PKaValue {

  private static final double MIN_SUPPORTED_PKA = -50.0;
  private static final double MAX_SUPPORTED_PKA = 100.0;
  private final double value;

  private PKaValue(final double value) {
    this.value = value;
  }

  public static PKaValue of(final double value) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException("pKa value must be finite.");
    }
    if (value < MIN_SUPPORTED_PKA || value > MAX_SUPPORTED_PKA) {
      throw new IllegalArgumentException("pKa value is outside supported physical bounds.");
    }
    return new PKaValue(value);
  }

  public double value() {
    return this.value;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof PKaValue)) {
      return false;
    }
    final PKaValue pka = (PKaValue) other;
    return Double.compare(this.value, pka.value) == 0;
  }

  public int hashCode() {
    return Double.hashCode(this.value);
  }
}
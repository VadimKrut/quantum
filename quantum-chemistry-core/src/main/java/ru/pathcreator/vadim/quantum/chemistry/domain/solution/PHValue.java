/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.solution;

public final class PHValue {

  private static final double MIN_REASONABLE_PH = -10.0;
  private static final double MAX_REASONABLE_PH = 30.0;
  private final double value;

  private PHValue(final double value) {
    this.value = value;
  }

  public static PHValue of(final double value) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException("pH value must be finite.");
    }
    if (value < MIN_REASONABLE_PH || value > MAX_REASONABLE_PH) {
      throw new IllegalArgumentException("pH value is outside supported physical bounds.");
    }
    return new PHValue(value);
  }

  public double value() {
    return this.value;
  }

  public boolean acidic() {
    return this.value < 7.0;
  }

  public boolean neutral() {
    return Double.compare(this.value, 7.0) == 0;
  }

  public boolean basic() {
    return this.value > 7.0;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof PHValue)) {
      return false;
    }
    final PHValue ph = (PHValue) other;
    return Double.compare(this.value, ph.value) == 0;
  }

  public int hashCode() {
    return Double.hashCode(this.value);
  }
}
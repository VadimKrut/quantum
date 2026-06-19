/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.structure;

public final class FormalCharge {

  public static final FormalCharge NEUTRAL = new FormalCharge(0);
  private static final int MIN_VALUE = -16;
  private static final int MAX_VALUE = 16;
  private final int value;

  private FormalCharge(final int value) {
    this.value = value;
  }

  public static FormalCharge of(final int value) {
    if (value < MIN_VALUE || value > MAX_VALUE) {
      throw new IllegalArgumentException("Formal charge must be between -16 and 16.");
    }
    if (value == 0) {
      return NEUTRAL;
    }
    return new FormalCharge(value);
  }

  public int value() {
    return this.value;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof FormalCharge)) {
      return false;
    }
    final FormalCharge charge = (FormalCharge) other;
    return this.value == charge.value;
  }

  public int hashCode() {
    return this.value;
  }
}
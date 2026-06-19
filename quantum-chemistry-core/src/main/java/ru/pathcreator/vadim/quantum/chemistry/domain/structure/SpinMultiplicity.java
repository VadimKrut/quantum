/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.structure;

public final class SpinMultiplicity {

  public static final SpinMultiplicity SINGLET = new SpinMultiplicity(1);
  private static final int MIN_VALUE = 1;
  private static final int MAX_VALUE = 64;
  private final int value;

  private SpinMultiplicity(final int value) {
    this.value = value;
  }

  public static SpinMultiplicity of(final int value) {
    if (value < MIN_VALUE || value > MAX_VALUE) {
      throw new IllegalArgumentException("Spin multiplicity is outside supported bounds.");
    }
    if (value == 1) {
      return SINGLET;
    }
    return new SpinMultiplicity(value);
  }

  public int value() {
    return this.value;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof SpinMultiplicity)) {
      return false;
    }
    final SpinMultiplicity multiplicity = (SpinMultiplicity) other;
    return this.value == multiplicity.value;
  }

  public int hashCode() {
    return this.value;
  }
}
/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.structure;

public final class MolecularCharge {

  public static final MolecularCharge NEUTRAL = new MolecularCharge(0);
  private static final int MIN_CHARGE = -128;
  private static final int MAX_CHARGE = 128;
  private final int value;

  private MolecularCharge(final int value) {
    this.value = value;
  }

  public static MolecularCharge of(final int value) {
    if (value < MIN_CHARGE || value > MAX_CHARGE) {
      throw new IllegalArgumentException("Molecular charge is outside supported bounds.");
    }
    if (value == 0) {
      return NEUTRAL;
    }
    return new MolecularCharge(value);
  }

  public int value() {
    return this.value;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MolecularCharge)) {
      return false;
    }
    final MolecularCharge charge = (MolecularCharge) other;
    return this.value == charge.value;
  }

  public int hashCode() {
    return this.value;
  }
}
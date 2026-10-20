/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.fermion.domain.tapering;

/**
 * Фиксированное значение Z2-симметрии на qubit, который можно удалить из Pauli operator.
 */
public final class FixedZ2Symmetry {

  private final int qubit;
  private final int zEigenvalue;

  private FixedZ2Symmetry(final int qubit, final int zEigenvalue) {
    this.qubit = qubit;
    this.zEigenvalue = zEigenvalue;
  }

  public static FixedZ2Symmetry of(final int qubit, final int zEigenvalue) {
    if (qubit < 0) {
      throw new IllegalArgumentException("Fixed Z2 symmetry qubit must not be negative.");
    }
    if (zEigenvalue != -1 && zEigenvalue != 1) {
      throw new IllegalArgumentException("Fixed Z2 symmetry eigenvalue must be -1 or 1.");
    }
    return new FixedZ2Symmetry(qubit, zEigenvalue);
  }

  public int qubit() {
    return this.qubit;
  }

  public int zEigenvalue() {
    return this.zEigenvalue;
  }
}

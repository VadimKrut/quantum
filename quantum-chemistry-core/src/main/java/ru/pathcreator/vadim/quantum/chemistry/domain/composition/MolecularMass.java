/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.composition;

/** Молекулярная масса: средняя атомная масса и целочисленное номинальное массовое число. */
public final class MolecularMass {

  private final double averageAtomicMass;
  private final int nominalMassNumber;

  private MolecularMass(
      final double averageAtomicMass,
      final int nominalMassNumber
  ) {
    this.averageAtomicMass = averageAtomicMass;
    this.nominalMassNumber = nominalMassNumber;
  }

  public static MolecularMass of(
      final double averageAtomicMass,
      final int nominalMassNumber
  ) {
    if (!Double.isFinite(averageAtomicMass)) {
      throw new IllegalArgumentException("Average molecular mass must be finite.");
    }
    if (averageAtomicMass <= 0.0) {
      throw new IllegalArgumentException("Average molecular mass must be positive.");
    }
    if (nominalMassNumber <= 0) {
      throw new IllegalArgumentException("Nominal mass number must be positive.");
    }
    return new MolecularMass(averageAtomicMass, nominalMassNumber);
  }

  public double averageAtomicMass() {
    return this.averageAtomicMass;
  }

  public int nominalMassNumber() {
    return this.nominalMassNumber;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MolecularMass)) {
      return false;
    }
    final MolecularMass mass = (MolecularMass) other;
    return Double.compare(this.averageAtomicMass, mass.averageAtomicMass) == 0
        && this.nominalMassNumber == mass.nominalMassNumber;
  }

  public int hashCode() {
    int result = Double.hashCode(this.averageAtomicMass);
    result = 31 * result + this.nominalMassNumber;
    return result;
  }
}
/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.symmetry;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;

public final class MolecularSymmetry {

  public static final MolecularSymmetry C1 = new MolecularSymmetry(PointGroupName.C1, 1);
  private static final int MIN_SYMMETRY_NUMBER = 1;
  private static final int MAX_SYMMETRY_NUMBER = 1000000;
  private final PointGroupName pointGroupName;
  private final int symmetryNumber;

  private MolecularSymmetry(
      final PointGroupName pointGroupName,
      final int symmetryNumber
  ) {
    this.pointGroupName = pointGroupName;
    this.symmetryNumber = symmetryNumber;
  }

  public static MolecularSymmetry of(
      final PointGroupName pointGroupName,
      final int symmetryNumber
  ) {
    if (pointGroupName == null) {
      throw new IllegalArgumentException("Point group name must not be null.");
    }
    if (symmetryNumber < MIN_SYMMETRY_NUMBER || symmetryNumber > MAX_SYMMETRY_NUMBER) {
      throw new IllegalArgumentException("Symmetry number must be between 1 and 1000000.");
    }
    if (PointGroupName.C1.equals(pointGroupName) && symmetryNumber == 1) {
      return C1;
    }
    return new MolecularSymmetry(pointGroupName, symmetryNumber);
  }

  public PointGroupName pointGroupName() {
    return this.pointGroupName;
  }

  public int symmetryNumber() {
    return this.symmetryNumber;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MolecularSymmetry)) {
      return false;
    }
    final MolecularSymmetry symmetry = (MolecularSymmetry) other;
    return this.symmetryNumber == symmetry.symmetryNumber
        && Objects.equals(this.pointGroupName, symmetry.pointGroupName);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.pointGroupName);
    result = ChemistryHash.include(result, this.symmetryNumber);
    return result;
  }
}
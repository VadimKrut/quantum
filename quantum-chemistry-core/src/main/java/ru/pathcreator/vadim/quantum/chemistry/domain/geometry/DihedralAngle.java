/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.geometry;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;

/** Подписанный торсионный угол first-second-third-fourth в диапазоне [-180, 180]. */
public final class DihedralAngle {

  private final AtomId firstAtomId;
  private final AtomId secondAtomId;
  private final AtomId thirdAtomId;
  private final AtomId fourthAtomId;
  private final double degrees;

  private DihedralAngle(
      final AtomId firstAtomId,
      final AtomId secondAtomId,
      final AtomId thirdAtomId,
      final AtomId fourthAtomId,
      final double degrees) {
    this.firstAtomId = firstAtomId;
    this.secondAtomId = secondAtomId;
    this.thirdAtomId = thirdAtomId;
    this.fourthAtomId = fourthAtomId;
    this.degrees = degrees;
  }

  public static DihedralAngle of(
      final AtomId firstAtomId,
      final AtomId secondAtomId,
      final AtomId thirdAtomId,
      final AtomId fourthAtomId,
      final double degrees) {
    DihedralAngle.requireAtomId(firstAtomId, "First dihedral atom id");
    DihedralAngle.requireAtomId(secondAtomId, "Second dihedral atom id");
    DihedralAngle.requireAtomId(thirdAtomId, "Third dihedral atom id");
    DihedralAngle.requireAtomId(fourthAtomId, "Fourth dihedral atom id");
    if (firstAtomId.equals(secondAtomId)
        || firstAtomId.equals(thirdAtomId)
        || firstAtomId.equals(fourthAtomId)
        || secondAtomId.equals(thirdAtomId)
        || secondAtomId.equals(fourthAtomId)
        || thirdAtomId.equals(fourthAtomId)) {
      throw new IllegalArgumentException("Dihedral angle atoms must be different.");
    }
    if (!Double.isFinite(degrees)) {
      throw new IllegalArgumentException("Dihedral angle degrees must be finite.");
    }
    if (degrees < -180.0 || degrees > 180.0) {
      throw new IllegalArgumentException("Dihedral angle degrees must be between -180 and 180.");
    }
    return new DihedralAngle(firstAtomId, secondAtomId, thirdAtomId, fourthAtomId, degrees);
  }

  public AtomId firstAtomId() {
    return this.firstAtomId;
  }

  public AtomId secondAtomId() {
    return this.secondAtomId;
  }

  public AtomId thirdAtomId() {
    return this.thirdAtomId;
  }

  public AtomId fourthAtomId() {
    return this.fourthAtomId;
  }

  public double degrees() {
    return this.degrees;
  }

  private static void requireAtomId(
      final AtomId atomId,
      final String subjectName
  ) {
    if (atomId == null) {
      throw new IllegalArgumentException(subjectName + " must not be null.");
    }
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof DihedralAngle)) {
      return false;
    }
    final DihedralAngle dihedralAngle = (DihedralAngle) other;
    return Double.compare(this.degrees, dihedralAngle.degrees) == 0
        && Objects.equals(this.firstAtomId, dihedralAngle.firstAtomId)
        && Objects.equals(this.secondAtomId, dihedralAngle.secondAtomId)
        && Objects.equals(this.thirdAtomId, dihedralAngle.thirdAtomId)
        && Objects.equals(this.fourthAtomId, dihedralAngle.fourthAtomId);
  }

  public int hashCode() {
    int result = this.firstAtomId.hashCode();
    result = 31 * result + this.secondAtomId.hashCode();
    result = 31 * result + this.thirdAtomId.hashCode();
    result = 31 * result + this.fourthAtomId.hashCode();
    result = 31 * result + Double.hashCode(this.degrees);
    return result;
  }
}
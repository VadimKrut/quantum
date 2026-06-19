/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.conformation;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;

public final class TorsionAngle {

  private static final double MIN_DEGREES = -180.0;
  private static final double MAX_DEGREES = 180.0;
  private final AtomId firstAtomId;
  private final AtomId secondAtomId;
  private final AtomId thirdAtomId;
  private final AtomId fourthAtomId;
  private final double degrees;

  private TorsionAngle(
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

  public static TorsionAngle of(
      final AtomId firstAtomId,
      final AtomId secondAtomId,
      final AtomId thirdAtomId,
      final AtomId fourthAtomId,
      final double degrees) {
    TorsionAngle.requireAtomId(firstAtomId, "First torsion atom id");
    TorsionAngle.requireAtomId(secondAtomId, "Second torsion atom id");
    TorsionAngle.requireAtomId(thirdAtomId, "Third torsion atom id");
    TorsionAngle.requireAtomId(fourthAtomId, "Fourth torsion atom id");
    TorsionAngle.requireDifferentAtoms(firstAtomId, secondAtomId, thirdAtomId, fourthAtomId);
    if (!Double.isFinite(degrees)) {
      throw new IllegalArgumentException("Torsion angle must be finite.");
    }
    if (degrees < MIN_DEGREES || degrees > MAX_DEGREES) {
      throw new IllegalArgumentException("Torsion angle degrees must be between -180 and 180.");
    }
    return new TorsionAngle(firstAtomId, secondAtomId, thirdAtomId, fourthAtomId, degrees);
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

  public boolean references(final AtomId atomId) {
    return this.firstAtomId.equals(atomId)
        || this.secondAtomId.equals(atomId)
        || this.thirdAtomId.equals(atomId)
        || this.fourthAtomId.equals(atomId);
  }

  private static void requireAtomId(
      final AtomId atomId,
      final String subjectName
  ) {
    if (atomId == null) {
      throw new IllegalArgumentException(subjectName + " must not be null.");
    }
  }

  private static void requireDifferentAtoms(
      final AtomId firstAtomId, final AtomId secondAtomId, final AtomId thirdAtomId, final AtomId fourthAtomId) {
    if (firstAtomId.equals(secondAtomId)
        || firstAtomId.equals(thirdAtomId)
        || firstAtomId.equals(fourthAtomId)
        || secondAtomId.equals(thirdAtomId)
        || secondAtomId.equals(fourthAtomId)
        || thirdAtomId.equals(fourthAtomId)) {
      throw new IllegalArgumentException("Torsion angle atoms must be different.");
    }
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof TorsionAngle)) {
      return false;
    }
    final TorsionAngle torsionAngle = (TorsionAngle) other;
    return Double.compare(this.degrees, torsionAngle.degrees) == 0
        && Objects.equals(this.firstAtomId, torsionAngle.firstAtomId)
        && Objects.equals(this.secondAtomId, torsionAngle.secondAtomId)
        && Objects.equals(this.thirdAtomId, torsionAngle.thirdAtomId)
        && Objects.equals(this.fourthAtomId, torsionAngle.fourthAtomId);
  }

  public int hashCode() {
    return Objects.hash(
        this.firstAtomId, this.secondAtomId, this.thirdAtomId, this.fourthAtomId, this.degrees);
  }
}
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

/** Валентный угол first-center-third в градусах от 0 до 180. */
public final class BondAngle {

  private final AtomId firstAtomId;
  private final AtomId centerAtomId;
  private final AtomId thirdAtomId;
  private final double degrees;

  private BondAngle(
      final AtomId firstAtomId,
      final AtomId centerAtomId,
      final AtomId thirdAtomId,
      final double degrees) {
    this.firstAtomId = firstAtomId;
    this.centerAtomId = centerAtomId;
    this.thirdAtomId = thirdAtomId;
    this.degrees = degrees;
  }

  public static BondAngle of(
      final AtomId firstAtomId,
      final AtomId centerAtomId,
      final AtomId thirdAtomId,
      final double degrees) {
    BondAngle.requireAtomId(firstAtomId, "First bond-angle atom id");
    BondAngle.requireAtomId(centerAtomId, "Center bond-angle atom id");
    BondAngle.requireAtomId(thirdAtomId, "Third bond-angle atom id");
    if (firstAtomId.equals(centerAtomId)
        || firstAtomId.equals(thirdAtomId)
        || centerAtomId.equals(thirdAtomId)) {
      throw new IllegalArgumentException("Bond-angle atoms must be different.");
    }
    if (!Double.isFinite(degrees)) {
      throw new IllegalArgumentException("Bond angle degrees must be finite.");
    }
    if (degrees < 0.0 || degrees > 180.0) {
      throw new IllegalArgumentException("Bond angle degrees must be between 0 and 180.");
    }
    return new BondAngle(firstAtomId, centerAtomId, thirdAtomId, degrees);
  }

  public AtomId firstAtomId() {
    return this.firstAtomId;
  }

  public AtomId centerAtomId() {
    return this.centerAtomId;
  }

  public AtomId thirdAtomId() {
    return this.thirdAtomId;
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
    if (!(other instanceof BondAngle)) {
      return false;
    }
    final BondAngle bondAngle = (BondAngle) other;
    return Double.compare(this.degrees, bondAngle.degrees) == 0
        && Objects.equals(this.firstAtomId, bondAngle.firstAtomId)
        && Objects.equals(this.centerAtomId, bondAngle.centerAtomId)
        && Objects.equals(this.thirdAtomId, bondAngle.thirdAtomId);
  }

  public int hashCode() {
    int result = this.firstAtomId.hashCode();
    result = 31 * result + this.centerAtomId.hashCode();
    result = 31 * result + this.thirdAtomId.hashCode();
    result = 31 * result + Double.hashCode(this.degrees);
    return result;
  }
}
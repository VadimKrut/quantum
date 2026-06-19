/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.property;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;

public final class PartialAtomicCharge {

  private final AtomId atomId;
  private final double elementaryCharge;

  private PartialAtomicCharge(
      final AtomId atomId,
      final double elementaryCharge
  ) {
    this.atomId = atomId;
    this.elementaryCharge = elementaryCharge;
  }

  public static PartialAtomicCharge of(
      final AtomId atomId,
      final double elementaryCharge
  ) {
    if (atomId == null) {
      throw new IllegalArgumentException("Partial charge atom id must not be null.");
    }
    if (!Double.isFinite(elementaryCharge)) {
      throw new IllegalArgumentException("Partial atomic charge must be finite.");
    }
    return new PartialAtomicCharge(atomId, elementaryCharge);
  }

  public AtomId atomId() {
    return this.atomId;
  }

  public double elementaryCharge() {
    return this.elementaryCharge;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof PartialAtomicCharge)) {
      return false;
    }
    final PartialAtomicCharge charge = (PartialAtomicCharge) other;
    return Double.compare(this.elementaryCharge, charge.elementaryCharge) == 0
        && Objects.equals(this.atomId, charge.atomId);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.atomId);
    result = ChemistryHash.include(result, this.elementaryCharge);
    return result;
  }
}
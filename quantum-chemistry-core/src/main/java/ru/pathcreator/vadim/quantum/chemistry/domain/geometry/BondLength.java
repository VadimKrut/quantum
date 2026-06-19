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

/** Длина связи между двумя разными атомами. */
public final class BondLength {

  private final AtomId firstAtomId;
  private final AtomId secondAtomId;
  private final LengthValue length;

  private BondLength(
      final AtomId firstAtomId, final AtomId secondAtomId, final LengthValue length) {
    this.firstAtomId = firstAtomId;
    this.secondAtomId = secondAtomId;
    this.length = length;
  }

  public static BondLength of(
      final AtomId firstAtomId, final AtomId secondAtomId, final LengthValue length) {
    if (firstAtomId == null) {
      throw new IllegalArgumentException("First bond-length atom id must not be null.");
    }
    if (secondAtomId == null) {
      throw new IllegalArgumentException("Second bond-length atom id must not be null.");
    }
    if (firstAtomId.equals(secondAtomId)) {
      throw new IllegalArgumentException("Bond length atoms must be different.");
    }
    if (length == null) {
      throw new IllegalArgumentException("Bond length value must not be null.");
    }
    return new BondLength(firstAtomId, secondAtomId, length);
  }

  public AtomId firstAtomId() {
    return this.firstAtomId;
  }

  public AtomId secondAtomId() {
    return this.secondAtomId;
  }

  public LengthValue length() {
    return this.length;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof BondLength)) {
      return false;
    }
    final BondLength bondLength = (BondLength) other;
    return Objects.equals(this.firstAtomId, bondLength.firstAtomId)
        && Objects.equals(this.secondAtomId, bondLength.secondAtomId)
        && Objects.equals(this.length, bondLength.length);
  }

  public int hashCode() {
    int result = this.firstAtomId.hashCode();
    result = 31 * result + this.secondAtomId.hashCode();
    result = 31 * result + this.length.hashCode();
    return result;
  }
}
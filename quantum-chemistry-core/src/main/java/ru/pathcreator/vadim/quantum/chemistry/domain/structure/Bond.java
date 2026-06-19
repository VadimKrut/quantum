/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.structure;

import java.util.Objects;

/** Химическая связь между двумя разными атомами с неориентированной парой endpoint-ов. */
public final class Bond {

  private final AtomId firstAtomId;
  private final AtomId secondAtomId;
  private final BondType type;

  private Bond(
      final AtomId firstAtomId,
      final AtomId secondAtomId,
      final BondType type
  ) {
    this.firstAtomId = firstAtomId;
    this.secondAtomId = secondAtomId;
    this.type = type;
  }

  public static Bond of(
      final AtomId firstAtomId,
      final AtomId secondAtomId,
      final BondType type
  ) {
    if (firstAtomId == null) {
      throw new IllegalArgumentException("First atom id must not be null.");
    }
    if (secondAtomId == null) {
      throw new IllegalArgumentException("Second atom id must not be null.");
    }
    if (firstAtomId.equals(secondAtomId)) {
      throw new IllegalArgumentException("Bond must connect two different atoms.");
    }
    if (type == null) {
      throw new IllegalArgumentException("Bond type must not be null.");
    }
    return new Bond(firstAtomId, secondAtomId, type);
  }

  public AtomId firstAtomId() {
    return this.firstAtomId;
  }

  public AtomId secondAtomId() {
    return this.secondAtomId;
  }

  public BondType type() {
    return this.type;
  }

  public boolean connects(
      final AtomId first,
      final AtomId second
  ) {
    return firstAtomId.equals(first) && secondAtomId.equals(second)
        || firstAtomId.equals(second) && secondAtomId.equals(first);
  }

  String canonicalEndpointKey() {
    final String first = firstAtomId.value();
    final String second = secondAtomId.value();
    if (first.compareTo(second) <= 0) {
      return first + '\u0000' + second;
    }
    return second + '\u0000' + first;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof Bond)) {
      return false;
    }
    final Bond bond = (Bond) other;
    return Objects.equals(firstAtomId, bond.firstAtomId)
        && Objects.equals(secondAtomId, bond.secondAtomId)
        && type == bond.type;
  }

  public int hashCode() {
    int result = firstAtomId.hashCode();
    result = 31 * result + secondAtomId.hashCode();
    result = 31 * result + type.hashCode();
    return result;
  }
}
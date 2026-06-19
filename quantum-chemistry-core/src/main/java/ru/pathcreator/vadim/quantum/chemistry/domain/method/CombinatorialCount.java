/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.method;

import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;

public final class CombinatorialCount {

  private final long value;
  private final boolean exact;

  private CombinatorialCount(
      final long value,
      final boolean exact
  ) {
    this.value = value;
    this.exact = exact;
  }

  public static CombinatorialCount exact(final long value) {
    if (value < 0L) {
      throw new IllegalArgumentException("Combinatorial count must not be negative.");
    }
    return new CombinatorialCount(value, true);
  }

  public static CombinatorialCount saturated() {
    return new CombinatorialCount(Long.MAX_VALUE, false);
  }

  public long value() {
    return this.value;
  }

  public boolean exact() {
    return this.exact;
  }

  public boolean saturatedValue() {
    return !this.exact;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof CombinatorialCount)) {
      return false;
    }
    final CombinatorialCount count = (CombinatorialCount) other;
    return this.value == count.value && this.exact == count.exact;
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.value);
    result = ChemistryHash.include(result, this.exact);
    return result;
  }
}
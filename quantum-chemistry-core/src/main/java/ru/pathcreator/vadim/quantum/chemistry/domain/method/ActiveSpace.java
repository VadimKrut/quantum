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

public final class ActiveSpace {

  private final int electronCount;
  private final int orbitalCount;

  private ActiveSpace(
      final int electronCount,
      final int orbitalCount
  ) {
    this.electronCount = electronCount;
    this.orbitalCount = orbitalCount;
  }

  public static ActiveSpace of(
      final int electronCount,
      final int orbitalCount
  ) {
    if (electronCount <= 0) {
      throw new IllegalArgumentException("Active space electron count must be positive.");
    }
    if (orbitalCount <= 0) {
      throw new IllegalArgumentException("Active space orbital count must be positive.");
    }
    final long spinOrbitalCapacity = (long) orbitalCount * 2L;
    if ((long) electronCount > spinOrbitalCapacity) {
      throw new IllegalArgumentException(
          "Active space cannot contain more electrons than spin orbitals.");
    }
    return new ActiveSpace(electronCount, orbitalCount);
  }

  public int electronCount() {
    return this.electronCount;
  }

  public int orbitalCount() {
    return this.orbitalCount;
  }

  public int spinOrbitalCount() {
    return Math.multiplyExact(this.orbitalCount, 2);
  }

  public ActiveSpaceResourceEstimate resourceEstimate() {
    return ActiveSpaceResourceEstimate.of(this);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ActiveSpace)) {
      return false;
    }
    final ActiveSpace activeSpace = (ActiveSpace) other;
    return this.electronCount == activeSpace.electronCount
        && this.orbitalCount == activeSpace.orbitalCount;
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.electronCount);
    result = ChemistryHash.include(result, this.orbitalCount);
    return result;
  }
}
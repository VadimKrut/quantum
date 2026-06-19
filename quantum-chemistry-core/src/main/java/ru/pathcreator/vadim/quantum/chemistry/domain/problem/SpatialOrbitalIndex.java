/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.problem;

/** Индекс spatial orbital в молекулярном базисе. */
public final class SpatialOrbitalIndex implements Comparable<SpatialOrbitalIndex> {

  private final int value;

  private SpatialOrbitalIndex(final int value) {
    this.value = value;
  }

  public static SpatialOrbitalIndex of(final int value) {
    if (value < 0) {
      throw new IllegalArgumentException("Spatial orbital index must be non-negative.");
    }
    return new SpatialOrbitalIndex(value);
  }

  public int value() {
    return value;
  }

  public void requireWithin(final int orbitalCount) {
    if (orbitalCount <= 0) {
      throw new IllegalArgumentException("Orbital count must be positive.");
    }
    if (value >= orbitalCount) {
      throw new IllegalArgumentException(
          "Spatial orbital index exceeds active space orbital count.");
    }
  }

  @Override
  public int compareTo(final SpatialOrbitalIndex other) {
    if (other == null) {
      throw new IllegalArgumentException("Other spatial orbital index must not be null.");
    }
    return Integer.compare(value, other.value);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof SpatialOrbitalIndex)) {
      return false;
    }
    final SpatialOrbitalIndex index = (SpatialOrbitalIndex) other;
    return value == index.value;
  }

  public int hashCode() {
    return this.value;
  }
}
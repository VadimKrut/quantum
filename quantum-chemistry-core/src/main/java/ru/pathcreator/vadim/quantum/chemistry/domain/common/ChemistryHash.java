/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.common;

/**
 * Лёгкий helper для hashCode без varargs-массивов, которые создаёт Objects.hash.
 */
public final class ChemistryHash {

  private static final int SEED = 17;
  private static final int MULTIPLIER = 31;

  private ChemistryHash() {
    throw new AssertionError("Chemistry hash utility must not be instantiated.");
  }

  public static int seed() {
    return SEED;
  }

  public static int include(
      final int current,
      final Object value
  ) {
    return MULTIPLIER * current + (value == null ? 0 : value.hashCode());
  }

  public static int include(
      final int current,
      final int value
  ) {
    return MULTIPLIER * current + value;
  }

  public static int include(
      final int current,
      final long value
  ) {
    return MULTIPLIER * current + Long.hashCode(value);
  }

  public static int include(
      final int current,
      final double value
  ) {
    return MULTIPLIER * current + Double.hashCode(value);
  }

  public static int include(
      final int current,
      final boolean value
  ) {
    return MULTIPLIER * current + Boolean.hashCode(value);
  }
}
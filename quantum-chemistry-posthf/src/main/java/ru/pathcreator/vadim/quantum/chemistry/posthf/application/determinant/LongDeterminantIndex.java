/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.posthf.application.determinant;

/**
 * Primitive hash index для determinant basis без Long/Integer boxing.
 */
public final class LongDeterminantIndex {

  private static final float LOAD_FACTOR = 0.65F;

  private final long[] keys;
  private final int[] values;
  private final boolean[] used;
  private final int mask;

  private LongDeterminantIndex(
      final long[] keys, final int[] values, final boolean[] used, final int mask) {
    this.keys = keys;
    this.values = values;
    this.used = used;
    this.mask = mask;
  }

  public static LongDeterminantIndex of(final long[] determinants) {
    if (determinants == null) {
      throw new IllegalArgumentException("Determinant index input must not be null.");
    }
    int capacity = 1;
    while ((int) ((float) capacity * LOAD_FACTOR) < determinants.length) {
      capacity <<= 1;
    }
    final LongDeterminantIndex index =
        new LongDeterminantIndex(
            new long[capacity], new int[capacity], new boolean[capacity], capacity - 1);
    for (int i = 0; i < determinants.length; ++i) {
      index.put(determinants[i], i);
    }
    return index;
  }

  public int indexOf(final long key) {
    int slot = this.slot(key);
    while (this.used[slot]) {
      if (this.keys[slot] == key) {
        return this.values[slot];
      }
      slot = (slot + 1) & this.mask;
    }
    return -1;
  }

  private void put(final long key, final int value) {
    int slot = this.slot(key);
    while (this.used[slot]) {
      if (this.keys[slot] == key) {
        this.values[slot] = value;
        return;
      }
      slot = (slot + 1) & this.mask;
    }
    this.used[slot] = true;
    this.keys[slot] = key;
    this.values[slot] = value;
  }

  private int slot(final long key) {
    long mixed = key;
    mixed ^= mixed >>> 33;
    mixed *= 0xff51afd7ed558ccdL;
    mixed ^= mixed >>> 33;
    mixed *= 0xc4ceb9fe1a85ec53L;
    mixed ^= mixed >>> 33;
    return (int) mixed & this.mask;
  }
}

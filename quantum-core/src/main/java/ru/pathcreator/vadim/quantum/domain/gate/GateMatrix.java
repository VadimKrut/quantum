/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.gate;

import java.util.Arrays;
import java.util.Objects;

/**
 * Символьная матрица gate definition.
 */
public final class GateMatrix {

    private final String[][] entries;

    private GateMatrix(final String[][] entries) {
        this.entries = entries;
    }

    public static GateMatrix of(final String[][] entries) {
        validate(entries);
        return new GateMatrix(copy(entries));
    }

    public int rowCount() {
        return entries.length;
    }

    public int columnCount() {
        return entries[0].length;
    }

    public String entry(
        final int row,
        final int column
    ) {
        return entries[row][column];
    }

    public String[][] entries() {
        return copy(entries);
    }

    private static void validate(final String[][] entries) {
        if (entries == null) {
            throw new IllegalArgumentException("Gate matrix entries must not be null.");
        }
        if (entries.length == 0) {
            throw new IllegalArgumentException("Gate matrix must contain at least one row.");
        }
        if (!isPowerOfTwo(entries.length)) {
            throw new IllegalArgumentException("Gate matrix row count must be a power of two.");
        }
        int columnCount = -1;
        for (int row = 0; row < entries.length; row++) {
            if (entries[row] == null) {
                throw new IllegalArgumentException("Gate matrix row must not be null.");
            }
            if (columnCount < 0) {
                columnCount = entries[row].length;
                if (columnCount == 0) {
                    throw new IllegalArgumentException("Gate matrix must contain at least one column.");
                }
                if (columnCount != entries.length) {
                    throw new IllegalArgumentException("Gate matrix must be square.");
                }
            } else if (entries[row].length != columnCount) {
                throw new IllegalArgumentException("Gate matrix rows must have equal column count.");
            }
            for (int column = 0; column < entries[row].length; column++) {
                if (
                    entries[row][column] == null
                    || entries[row][column].isBlank()
                ) {
                    throw new IllegalArgumentException("Gate matrix entry must not be blank.");
                }
            }
        }
    }

    private static boolean isPowerOfTwo(final int value) {
        return value > 0
            && (value & (value - 1)) == 0;
    }

    private static String[][] copy(final String[][] source) {
        final String[][] result = new String[source.length][];
        for (int i = 0; i < source.length; i++) {
            result[i] = Arrays.copyOf(
                source[i],
                source[i].length
            );
        }
        return result;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GateMatrix matrix)) {
            return false;
        }
        return Arrays.deepEquals(
            entries,
            matrix.entries
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.deepHashCode(entries));
    }
}
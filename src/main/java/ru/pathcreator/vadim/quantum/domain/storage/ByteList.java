/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.storage;

import java.util.Arrays;

/**
 * Простой primitive-list для byte без boxing в горячих хранилищах.
 */
final class ByteList {

    private byte[] values;
    private int size;

    ByteList(final int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Byte list capacity must not be negative.");
        }
        this.values = new byte[capacity];
    }

    void add(final byte value) {
        ensureCapacity(size + 1);
        values[size] = value;
        size++;
    }

    byte[] toArray() {
        return Arrays.copyOf(
            values,
            size
        );
    }

    private void ensureCapacity(final int minimumCapacity) {
        if (minimumCapacity <= values.length) {
            return;
        }
        int newCapacity = values.length == 0 ? 8 : values.length + (values.length >> 1);
        if (newCapacity < minimumCapacity) {
            newCapacity = minimumCapacity;
        }
        values = Arrays.copyOf(
            values,
            newCapacity
        );
    }
}
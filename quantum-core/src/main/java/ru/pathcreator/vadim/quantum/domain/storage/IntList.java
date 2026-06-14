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
 * Простой primitive-list для int без Integer-boxing в плотных хранилищах.
 */
final class IntList {

    private int[] values;
    private int size;

    IntList(final int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Int list capacity must not be negative.");
        }
        this.values = new int[capacity];
    }

    void add(final int value) {
        ensureCapacity(size + 1);
        values[size] = value;
        size++;
    }

    int size() {
        return size;
    }

    int[] toArray() {
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
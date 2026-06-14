/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.register;

import ru.pathcreator.vadim.quantum.domain.bit.ClassicalBit;

/**
 * Классический регистр с фиксированным именем и количеством битов.
 */
public final class ClassicalRegister {

    /**
     * Имя классического регистра.
     */
    private final RegisterName name;

    /**
     * Количество битов в регистре.
     */
    private final int size;

    /**
     * Стабильные объекты битов, заранее созданные для каждого индекса.
     */
    private final ClassicalBit[] bits;

    private ClassicalRegister(
        final RegisterName name,
        final int size
    ) {
        this.name = name;
        this.size = size;
        this.bits = createBits(size);
    }

    /**
     * Создает классический регистр.
     *
     * @param name имя регистра
     * @param size количество битов
     * @return классический регистр
     */
    public static ClassicalRegister create(
        final String name,
        final int size
    ) {
        return create(
            RegisterName.of(name),
            size
        );
    }

    /**
     * Создает классический регистр.
     *
     * @param name проверенное имя регистра
     * @param size количество битов
     * @return классический регистр
     */
    public static ClassicalRegister create(
        final RegisterName name,
        final int size
    ) {
        validate(
            name,
            size
        );
        return new ClassicalRegister(
            name,
            size
        );
    }

    /**
     * Возвращает имя регистра.
     *
     * @return имя регистра
     */
    public RegisterName name() {
        return name;
    }

    /**
     * Возвращает количество битов в регистре.
     *
     * @return количество битов
     */
    public int size() {
        return size;
    }

    /**
     * Возвращает стабильный объект бита по индексу.
     *
     * @param index индекс бита
     * @return бит с указанным индексом
     */
    public ClassicalBit get(final int index) {
        validateIndex(index);
        return bits[index];
    }

    private static void validate(
        final RegisterName name,
        final int size
    ) {
        if (name == null) {
            throw new IllegalArgumentException("Classical register name must not be null.");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Classical register size must be positive.");
        }
    }

    private ClassicalBit[] createBits(final int size) {
        final ClassicalBit[] createdBits = new ClassicalBit[size];
        for (int i = 0; i < size; i++) {
            createdBits[i] = ClassicalBit.of(
                this,
                i
            );
        }
        return createdBits;
    }

    private void validateIndex(final int index) {
        if (
            index < 0
            || index >= size
        ) {
            throw new IllegalArgumentException("Classical bit index is outside of classical register bounds.");
        }
    }

    @Override
    public String toString() {
        return name.value() + "[" + size + "]";
    }
}
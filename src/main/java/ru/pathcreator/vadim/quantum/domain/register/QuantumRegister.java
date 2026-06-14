/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.register;

import ru.pathcreator.vadim.quantum.domain.bit.Qubit;

/**
 * Квантовый регистр с фиксированным именем и количеством кубитов.
 */
public final class QuantumRegister {

    /**
     * Имя квантового регистра.
     */
    private final RegisterName name;

    /**
     * Количество кубитов в регистре.
     */
    private final int size;

    /**
     * Стабильные объекты кубитов, заранее созданные для каждого индекса.
     */
    private final Qubit[] qubits;

    private QuantumRegister(
        final RegisterName name,
        final int size
    ) {
        this.name = name;
        this.size = size;
        this.qubits = createQubits(size);
    }

    /**
     * Создает квантовый регистр.
     *
     * @param name имя регистра
     * @param size количество кубитов
     * @return квантовый регистр
     */
    public static QuantumRegister create(
        final String name,
        final int size
    ) {
        return create(
            RegisterName.of(name),
            size
        );
    }

    /**
     * Создает квантовый регистр.
     *
     * @param name проверенное имя регистра
     * @param size количество кубитов
     * @return квантовый регистр
     */
    public static QuantumRegister create(
        final RegisterName name,
        final int size
    ) {
        validate(
            name,
            size
        );
        return new QuantumRegister(
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
     * Возвращает количество кубитов в регистре.
     *
     * @return количество кубитов
     */
    public int size() {
        return size;
    }

    /**
     * Возвращает стабильный объект кубита по индексу.
     *
     * @param index индекс кубита
     * @return кубит с указанным индексом
     */
    public Qubit get(final int index) {
        validateIndex(index);
        return qubits[index];
    }

    private static void validate(
        final RegisterName name,
        final int size
    ) {
        if (name == null) {
            throw new IllegalArgumentException("Quantum register name must not be null.");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Quantum register size must be positive.");
        }
    }

    private Qubit[] createQubits(final int size) {
        final Qubit[] createdQubits = new Qubit[size];
        for (int i = 0; i < size; i++) {
            createdQubits[i] = Qubit.of(
                this,
                i
            );
        }
        return createdQubits;
    }

    private void validateIndex(final int index) {
        if (
            index < 0
            || index >= size
        ) {
            throw new IllegalArgumentException("Qubit index is outside of quantum register bounds.");
        }
    }

    @Override
    public String toString() {
        return name.value() + "[" + size + "]";
    }
}
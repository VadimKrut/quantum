/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.operation;

import java.util.Objects;

import ru.pathcreator.vadim.quantum.domain.bit.Qubit;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

/**
 * Ссылка на qubit, которая может быть статической или вычисляться classical controller во время исполнения.
 */
public final class QuantumReference {

    /**
     * Тип ссылки.
     */
    private final QuantumReferenceKind kind;

    /**
     * Статический qubit для STATIC_QUBIT.
     */
    private final Qubit qubit;

    /**
     * Quantum register для DYNAMIC_REGISTER_INDEX.
     */
    private final QuantumRegister register;

    /**
     * Classical expression, вычисляющее индекс qubit внутри register.
     */
    private final ClassicalExpression indexExpression;

    /**
     * Индекс физического qubit для HARDWARE_QUBIT.
     */
    private final int hardwareIndex;

    private QuantumReference(
        final QuantumReferenceKind kind,
        final Qubit qubit,
        final QuantumRegister register,
        final ClassicalExpression indexExpression,
        final int hardwareIndex
    ) {
        this.kind = kind;
        this.qubit = qubit;
        this.register = register;
        this.indexExpression = indexExpression;
        this.hardwareIndex = hardwareIndex;
    }

    /**
     * Создает ссылку на конкретный qubit.
     *
     * @param qubit qubit схемы
     * @return статическая ссылка
     */
    public static QuantumReference staticQubit(final Qubit qubit) {
        if (qubit == null) {
            throw new IllegalArgumentException("Static quantum reference qubit must not be null.");
        }
        return new QuantumReference(
            QuantumReferenceKind.STATIC_QUBIT,
            qubit,
            null,
            null,
            -1
        );
    }

    /**
     * Создает runtime-ссылку register[indexExpression].
     *
     * @param register quantum register
     * @param indexExpression classical expression индекса
     * @return динамическая ссылка
     */
    public static QuantumReference dynamicIndex(
        final QuantumRegister register,
        final ClassicalExpression indexExpression
    ) {
        if (register == null) {
            throw new IllegalArgumentException("Dynamic quantum reference register must not be null.");
        }
        if (indexExpression == null) {
            throw new IllegalArgumentException("Dynamic quantum reference index expression must not be null.");
        }
        return new QuantumReference(
            QuantumReferenceKind.DYNAMIC_REGISTER_INDEX,
            null,
            register,
            indexExpression,
            -1
        );
    }

    /**
     * Создает ссылку на физический qubit backend-а.
     *
     * @param index индекс физического qubit
     * @return ссылка на физический qubit
     */
    public static QuantumReference hardwareQubit(final int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Hardware qubit index must not be negative.");
        }
        return new QuantumReference(
            QuantumReferenceKind.HARDWARE_QUBIT,
            null,
            null,
            null,
            index
        );
    }

    /**
     * Возвращает тип ссылки.
     *
     * @return тип ссылки
     */
    public QuantumReferenceKind kind() {
        return kind;
    }

    /**
     * Проверяет, что ссылка является статической.
     *
     * @return true для STATIC_QUBIT
     */
    public boolean isStatic() {
        return kind == QuantumReferenceKind.STATIC_QUBIT;
    }

    /**
     * Возвращает статический qubit.
     *
     * @return qubit
     */
    public Qubit qubit() {
        if (kind != QuantumReferenceKind.STATIC_QUBIT) {
            throw new IllegalStateException("Quantum reference is not a static qubit.");
        }
        return qubit;
    }

    /**
     * Возвращает quantum register динамической ссылки.
     *
     * @return quantum register
     */
    public QuantumRegister register() {
        if (kind != QuantumReferenceKind.DYNAMIC_REGISTER_INDEX) {
            throw new IllegalStateException("Quantum reference is not a dynamic register index.");
        }
        return register;
    }

    /**
     * Возвращает expression индекса динамической ссылки.
     *
     * @return expression индекса
     */
    public ClassicalExpression indexExpression() {
        if (kind != QuantumReferenceKind.DYNAMIC_REGISTER_INDEX) {
            throw new IllegalStateException("Quantum reference is not a dynamic register index.");
        }
        return indexExpression;
    }

    /**
     * Возвращает индекс физического qubit.
     *
     * @return индекс физического qubit
     */
    public int hardwareIndex() {
        if (kind != QuantumReferenceKind.HARDWARE_QUBIT) {
            throw new IllegalStateException("Quantum reference is not a hardware qubit.");
        }
        return hardwareIndex;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof QuantumReference reference)) {
            return false;
        }
        return kind == reference.kind
            && qubit == reference.qubit
            && register == reference.register
            && hardwareIndex == reference.hardwareIndex
            && Objects.equals(
                indexExpression,
                reference.indexExpression
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            kind,
            System.identityHashCode(qubit),
            System.identityHashCode(register),
            indexExpression,
            hardwareIndex
        );
    }
}
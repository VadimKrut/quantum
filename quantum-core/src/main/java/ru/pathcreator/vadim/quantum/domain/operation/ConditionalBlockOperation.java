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

import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;

/**
 * Блочное ветвление по классическому предикату.
 */
public final class ConditionalBlockOperation implements Operation {

    /**
     * Условие выполнения then-блока.
     */
    private final ClassicalPredicate predicate;

    /**
     * Блок, выполняемый при истинном предикате.
     */
    private final OperationBlock thenBlock;

    /**
     * Блок, выполняемый при ложном предикате, или null.
     */
    private final OperationBlock elseBlock;

    /**
     * Создает ветвление.
     *
     * @param predicate классический предикат
     * @param thenBlock then-блок
     * @param elseBlock else-блок или null
     */
    public ConditionalBlockOperation(
        final ClassicalPredicate predicate,
        final OperationBlock thenBlock,
        final OperationBlock elseBlock
    ) {
        if (predicate == null) {
            throw new IllegalArgumentException("Conditional block predicate must not be null.");
        }
        if (thenBlock == null) {
            throw new IllegalArgumentException("Conditional block then body must not be null.");
        }
        this.predicate = predicate;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
    }

    @Override
    public OperationKind kind() {
        return OperationKind.CONDITIONAL_BLOCK;
    }

    /**
     * Возвращает предикат ветвления.
     *
     * @return предикат
     */
    public ClassicalPredicate predicate() {
        return predicate;
    }

    /**
     * Возвращает then-блок.
     *
     * @return then-блок
     */
    public OperationBlock thenBlock() {
        return thenBlock;
    }

    /**
     * Проверяет, есть ли else-блок.
     *
     * @return true, если else-блок задан
     */
    public boolean hasElseBlock() {
        return elseBlock != null;
    }

    /**
     * Возвращает else-блок.
     *
     * @return else-блок
     */
    public OperationBlock elseBlock() {
        if (elseBlock == null) {
            throw new IllegalStateException("Conditional block does not have else body.");
        }
        return elseBlock;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ConditionalBlockOperation operation)) {
            return false;
        }
        return Objects.equals(
            predicate,
            operation.predicate
        )
            && Objects.equals(
                thenBlock,
                operation.thenBlock
            )
            && Objects.equals(
                elseBlock,
                operation.elseBlock
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            predicate,
            thenBlock,
            elseBlock
        );
    }
}
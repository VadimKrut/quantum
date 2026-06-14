/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.callable.template;

import java.util.Objects;

/**
 * Шаблон блочного ветвления внутри callable.
 */
public final class CallableConditionalBlockOperation implements CallableOperation {

    private final CallableClassicalPredicate predicate;
    private final CallableOperationBlock thenBlock;
    private final CallableOperationBlock elseBlock;

    /**
     * Создает шаблон условного блока.
     *
     * @param predicate условие выполнения then-блока
     * @param thenBlock блок для истинного условия
     * @param elseBlock блок для ложного условия или null
     */
    public CallableConditionalBlockOperation(
        final CallableClassicalPredicate predicate,
        final CallableOperationBlock thenBlock,
        final CallableOperationBlock elseBlock
    ) {
        if (predicate == null) {
            throw new IllegalArgumentException("Callable conditional predicate must not be null.");
        }
        if (thenBlock == null) {
            throw new IllegalArgumentException("Callable conditional then body must not be null.");
        }
        this.predicate = predicate;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
    }

    @Override
    public CallableOperationKind kind() {
        return CallableOperationKind.CONDITIONAL_BLOCK;
    }

    /**
     * Возвращает предикат.
     *
     * @return предикат
     */
    public CallableClassicalPredicate predicate() {
        return predicate;
    }

    /**
     * Возвращает then-блок.
     *
     * @return then-блок
     */
    public CallableOperationBlock thenBlock() {
        return thenBlock;
    }

    /**
     * Проверяет наличие else-блока.
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
    public CallableOperationBlock elseBlock() {
        if (elseBlock == null) {
            throw new IllegalStateException("Callable conditional block does not have else body.");
        }
        return elseBlock;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CallableConditionalBlockOperation operation)) {
            return false;
        }
        return Objects.equals(predicate, operation.predicate)
            && Objects.equals(thenBlock, operation.thenBlock)
            && Objects.equals(elseBlock, operation.elseBlock);
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
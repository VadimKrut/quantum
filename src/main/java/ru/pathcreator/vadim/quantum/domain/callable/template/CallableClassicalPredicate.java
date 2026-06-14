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

import ru.pathcreator.vadim.quantum.domain.classical.ClassicalBooleanOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalComparisonOperator;

/**
 * Логический предикат над шаблонными классическими выражениями.
 */
public final class CallableClassicalPredicate {

    /**
     * Тип предиката.
     */
    private final CallableClassicalPredicateKind kind;

    /**
     * Левое выражение для сравнения.
     */
    private final CallableClassicalExpression leftExpression;

    /**
     * Правое выражение для сравнения.
     */
    private final CallableClassicalExpression rightExpression;

    /**
     * Оператор сравнения.
     */
    private final ClassicalComparisonOperator comparisonOperator;

    /**
     * Левый дочерний предикат или operand для NOT.
     */
    private final CallableClassicalPredicate leftPredicate;

    /**
     * Правый дочерний предикат.
     */
    private final CallableClassicalPredicate rightPredicate;

    /**
     * Булев оператор.
     */
    private final ClassicalBooleanOperator booleanOperator;

    private CallableClassicalPredicate(
        final CallableClassicalPredicateKind kind,
        final CallableClassicalExpression leftExpression,
        final CallableClassicalExpression rightExpression,
        final ClassicalComparisonOperator comparisonOperator,
        final CallableClassicalPredicate leftPredicate,
        final CallableClassicalPredicate rightPredicate,
        final ClassicalBooleanOperator booleanOperator
    ) {
        this.kind = kind;
        this.leftExpression = leftExpression;
        this.rightExpression = rightExpression;
        this.comparisonOperator = comparisonOperator;
        this.leftPredicate = leftPredicate;
        this.rightPredicate = rightPredicate;
        this.booleanOperator = booleanOperator;
    }

    /**
     * Создает предикат сравнения.
     *
     * @param left левое выражение
     * @param operator оператор сравнения
     * @param right правое выражение
     * @return предикат
     */
    public static CallableClassicalPredicate compare(
        final CallableClassicalExpression left,
        final ClassicalComparisonOperator operator,
        final CallableClassicalExpression right
    ) {
        if (left == null) {
            throw new IllegalArgumentException("Callable comparison left expression must not be null.");
        }
        if (operator == null) {
            throw new IllegalArgumentException("Callable comparison operator must not be null.");
        }
        if (right == null) {
            throw new IllegalArgumentException("Callable comparison right expression must not be null.");
        }
        return new CallableClassicalPredicate(
            CallableClassicalPredicateKind.COMPARISON,
            left,
            right,
            operator,
            null,
            null,
            null
        );
    }

    /**
     * Создает логическое И.
     *
     * @param left левый предикат
     * @param right правый предикат
     * @return предикат
     */
    public static CallableClassicalPredicate and(
        final CallableClassicalPredicate left,
        final CallableClassicalPredicate right
    ) {
        return booleanPredicate(
            ClassicalBooleanOperator.AND,
            left,
            right
        );
    }

    /**
     * Создает логическое ИЛИ.
     *
     * @param left левый предикат
     * @param right правый предикат
     * @return предикат
     */
    public static CallableClassicalPredicate or(
        final CallableClassicalPredicate left,
        final CallableClassicalPredicate right
    ) {
        return booleanPredicate(
            ClassicalBooleanOperator.OR,
            left,
            right
        );
    }

    /**
     * Создает логическое НЕ.
     *
     * @param predicate исходный предикат
     * @return предикат
     */
    public static CallableClassicalPredicate not(final CallableClassicalPredicate predicate) {
        if (predicate == null) {
            throw new IllegalArgumentException("Callable predicate must not be null.");
        }
        return new CallableClassicalPredicate(
            CallableClassicalPredicateKind.NOT,
            null,
            null,
            null,
            predicate,
            null,
            null
        );
    }

    /**
     * Возвращает тип предиката.
     *
     * @return тип предиката
     */
    public CallableClassicalPredicateKind kind() {
        return kind;
    }

    /**
     * Возвращает левое выражение сравнения.
     *
     * @return левое выражение
     */
    public CallableClassicalExpression leftExpression() {
        return leftExpression;
    }

    /**
     * Возвращает правое выражение сравнения.
     *
     * @return правое выражение
     */
    public CallableClassicalExpression rightExpression() {
        return rightExpression;
    }

    /**
     * Возвращает оператор сравнения.
     *
     * @return оператор сравнения
     */
    public ClassicalComparisonOperator comparisonOperator() {
        return comparisonOperator;
    }

    /**
     * Возвращает левый дочерний предикат.
     *
     * @return левый предикат
     */
    public CallableClassicalPredicate leftPredicate() {
        return leftPredicate;
    }

    /**
     * Возвращает правый дочерний предикат.
     *
     * @return правый предикат
     */
    public CallableClassicalPredicate rightPredicate() {
        return rightPredicate;
    }

    /**
     * Возвращает булев оператор.
     *
     * @return булев оператор
     */
    public ClassicalBooleanOperator booleanOperator() {
        return booleanOperator;
    }

    private static CallableClassicalPredicate booleanPredicate(
        final ClassicalBooleanOperator operator,
        final CallableClassicalPredicate left,
        final CallableClassicalPredicate right
    ) {
        if (left == null) {
            throw new IllegalArgumentException("Callable boolean left predicate must not be null.");
        }
        if (right == null) {
            throw new IllegalArgumentException("Callable boolean right predicate must not be null.");
        }
        return new CallableClassicalPredicate(
            CallableClassicalPredicateKind.BOOLEAN,
            null,
            null,
            null,
            left,
            right,
            operator
        );
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CallableClassicalPredicate predicate)) {
            return false;
        }
        return kind == predicate.kind
            && comparisonOperator == predicate.comparisonOperator
            && booleanOperator == predicate.booleanOperator
            && Objects.equals(leftExpression, predicate.leftExpression)
            && Objects.equals(rightExpression, predicate.rightExpression)
            && Objects.equals(leftPredicate, predicate.leftPredicate)
            && Objects.equals(rightPredicate, predicate.rightPredicate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            kind,
            leftExpression,
            rightExpression,
            comparisonOperator,
            leftPredicate,
            rightPredicate,
            booleanOperator
        );
    }
}
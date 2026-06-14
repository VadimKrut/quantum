/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.classical;

import java.util.Objects;

/**
 * Логический предикат над классическими выражениями.
 */
public final class ClassicalPredicate {

    /**
     * Тип узла предиката.
     */
    private final ClassicalPredicateKind kind;

    /**
     * Левое выражение для сравнения.
     */
    private final ClassicalExpression leftExpression;

    /**
     * Правое выражение для сравнения.
     */
    private final ClassicalExpression rightExpression;

    /**
     * Оператор сравнения для COMPARISON.
     */
    private final ClassicalComparisonOperator comparisonOperator;

    /**
     * Левый дочерний предикат или единственный operand для NOT.
     */
    private final ClassicalPredicate leftPredicate;

    /**
     * Правый дочерний предикат для BOOLEAN.
     */
    private final ClassicalPredicate rightPredicate;

    /**
     * Логический оператор для BOOLEAN.
     */
    private final ClassicalBooleanOperator booleanOperator;

    private ClassicalPredicate(
        final ClassicalPredicateKind kind,
        final ClassicalExpression leftExpression,
        final ClassicalExpression rightExpression,
        final ClassicalComparisonOperator comparisonOperator,
        final ClassicalPredicate leftPredicate,
        final ClassicalPredicate rightPredicate,
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
     * Создает предикат сравнения двух классических выражений.
     *
     * @param left левое выражение
     * @param operator оператор сравнения
     * @param right правое выражение
     * @return предикат сравнения
     */
    public static ClassicalPredicate compare(
        final ClassicalExpression left,
        final ClassicalComparisonOperator operator,
        final ClassicalExpression right
    ) {
        if (left == null) {
            throw new IllegalArgumentException("Classical comparison left expression must not be null.");
        }
        if (operator == null) {
            throw new IllegalArgumentException("Classical comparison operator must not be null.");
        }
        if (right == null) {
            throw new IllegalArgumentException("Classical comparison right expression must not be null.");
        }
        return new ClassicalPredicate(
            ClassicalPredicateKind.COMPARISON,
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
     * @return составной предикат
     */
    public static ClassicalPredicate and(
        final ClassicalPredicate left,
        final ClassicalPredicate right
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
     * @return составной предикат
     */
    public static ClassicalPredicate or(
        final ClassicalPredicate left,
        final ClassicalPredicate right
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
     * @return отрицание предиката
     */
    public static ClassicalPredicate not(final ClassicalPredicate predicate) {
        if (predicate == null) {
            throw new IllegalArgumentException("Classical predicate must not be null.");
        }
        return new ClassicalPredicate(
            ClassicalPredicateKind.NOT,
            null,
            null,
            null,
            predicate,
            null,
            null
        );
    }

    private static ClassicalPredicate booleanPredicate(
        final ClassicalBooleanOperator operator,
        final ClassicalPredicate left,
        final ClassicalPredicate right
    ) {
        if (left == null) {
            throw new IllegalArgumentException("Classical boolean left predicate must not be null.");
        }
        if (right == null) {
            throw new IllegalArgumentException("Classical boolean right predicate must not be null.");
        }
        return new ClassicalPredicate(
            ClassicalPredicateKind.BOOLEAN,
            null,
            null,
            null,
            left,
            right,
            operator
        );
    }

    /**
     * Возвращает тип предиката.
     *
     * @return тип предиката
     */
    public ClassicalPredicateKind kind() {
        return kind;
    }

    /**
     * Возвращает левое выражение сравнения.
     *
     * @return левое выражение
     */
    public ClassicalExpression leftExpression() {
        return leftExpression;
    }

    /**
     * Возвращает правое выражение сравнения.
     *
     * @return правое выражение
     */
    public ClassicalExpression rightExpression() {
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
    public ClassicalPredicate leftPredicate() {
        return leftPredicate;
    }

    /**
     * Возвращает правый дочерний предикат.
     *
     * @return правый предикат
     */
    public ClassicalPredicate rightPredicate() {
        return rightPredicate;
    }

    /**
     * Возвращает логический оператор.
     *
     * @return логический оператор
     */
    public ClassicalBooleanOperator booleanOperator() {
        return booleanOperator;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ClassicalPredicate predicate)) {
            return false;
        }
        return kind == predicate.kind
            && comparisonOperator == predicate.comparisonOperator
            && booleanOperator == predicate.booleanOperator
            && Objects.equals(
                leftExpression,
                predicate.leftExpression
            )
            && Objects.equals(
                rightExpression,
                predicate.rightExpression
            )
            && Objects.equals(
                leftPredicate,
                predicate.leftPredicate
            )
            && Objects.equals(
                rightPredicate,
                predicate.rightPredicate
            );
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
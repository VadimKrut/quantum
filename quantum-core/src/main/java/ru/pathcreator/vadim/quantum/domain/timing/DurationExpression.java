/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.timing;

import java.util.Objects;

import ru.pathcreator.vadim.quantum.domain.naming.IdentifierName;

/**
 * Длительность или stretch-символ в timing IR.
 */
public final class DurationExpression {

    private final double value;
    private final DurationUnit unit;
    private final String symbol;
    private final String expression;

    private DurationExpression(
        final double value,
        final DurationUnit unit,
        final String symbol,
        final String expression
    ) {
        this.value = value;
        this.unit = unit;
        this.symbol = symbol;
        this.expression = expression;
    }

    public static DurationExpression duration(
        final double value,
        final DurationUnit unit
    ) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Duration value must be finite.");
        }
        if (value < 0.0) {
            throw new IllegalArgumentException("Duration value must not be negative.");
        }
        if (unit == null) {
            throw new IllegalArgumentException("Duration unit must not be null.");
        }
        return new DurationExpression(
            value,
            unit,
            null,
            null
        );
    }

    public static DurationExpression stretch(final String symbol) {
        return new DurationExpression(
            0.0,
            null,
            IdentifierName.of(
                symbol,
                "Stretch symbol"
            ).value(),
            null
        );
    }

    public static DurationExpression expression(final String expression) {
        if (
            expression == null
            || expression.isBlank()
        ) {
            throw new IllegalArgumentException("Duration expression text must not be blank.");
        }
        return new DurationExpression(
            0.0,
            null,
            null,
            expression.trim()
        );
    }

    public boolean isStretch() {
        return symbol != null;
    }

    public boolean isExpression() {
        return expression != null;
    }

    public double value() {
        if (
            isStretch()
            || isExpression()
        ) {
            throw new IllegalStateException("Duration expression is not a numeric duration.");
        }
        return value;
    }

    public DurationUnit unit() {
        if (
            isStretch()
            || isExpression()
        ) {
            throw new IllegalStateException("Duration expression is not a numeric duration.");
        }
        return unit;
    }

    public String symbol() {
        if (!isStretch()) {
            throw new IllegalStateException("Duration expression is not a stretch symbol.");
        }
        return symbol;
    }

    public String expression() {
        if (!isExpression()) {
            throw new IllegalStateException("Duration expression is not text expression.");
        }
        return expression;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DurationExpression expression)) {
            return false;
        }
        return Double.compare(
            value,
            expression.value
        ) == 0
            && unit == expression.unit
            && Objects.equals(
                symbol,
                expression.symbol
            )
            && Objects.equals(
                this.expression,
                expression.expression
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            value,
            unit,
            symbol,
            expression
        );
    }
}
/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.gate.modifier;

import java.util.Objects;

import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.naming.IdentifierName;

/**
 * Описание модификатора гейта: обратный гейт, управление, степень, повтор или аннотация.
 */
public final class GateModifier {

    private static final String ANNOTATION_NAME_SUBJECT = "Gate annotation name";

    /**
     * Тип модификатора.
     */
    private final GateModifierKind kind;

    /**
     * Целочисленное значение для количества управляющих кубитов или повторов.
     */
    private final int integerValue;

    /**
     * Вещественное значение для степени гейта.
     */
    private final double doubleValue;

    /**
     * Символическое выражение степени для POWER, если степень не является числом.
     */
    private final ParameterExpression powerExpression;

    /**
     * Имя аннотации для модификатора ANNOTATION.
     */
    private final String annotationName;

    private GateModifier(
        final GateModifierKind kind,
        final int integerValue,
        final double doubleValue,
        final ParameterExpression powerExpression,
        final String annotationName
    ) {
        this.kind = kind;
        this.integerValue = integerValue;
        this.doubleValue = doubleValue;
        this.powerExpression = powerExpression;
        this.annotationName = annotationName;
    }

    /**
     * Создает модификатор обратного гейта.
     *
     * @return модификатор обратного гейта
     */
    public static GateModifier inverse() {
        return new GateModifier(
            GateModifierKind.INVERSE,
            0,
            0.0,
            null,
            null
        );
    }

    /**
     * Создает модификатор квантового управления.
     *
     * @param controlCount количество управляющих кубитов
     * @return модификатор управления
     */
    public static GateModifier controlled(final int controlCount) {
        if (controlCount <= 0) {
            throw new IllegalArgumentException("Quantum control count must be positive.");
        }
        return new GateModifier(
            GateModifierKind.CONTROLLED,
            controlCount,
            0.0,
            null,
            null
        );
    }

    /**
     * Создает модификатор степени гейта.
     *
     * @param exponent конечная степень
     * @return модификатор степени
     */
    public static GateModifier power(final double exponent) {
        if (!Double.isFinite(exponent)) {
            throw new IllegalArgumentException("Gate power exponent must be finite.");
        }
        return new GateModifier(
            GateModifierKind.POWER,
            0,
            exponent,
            null,
            null
        );
    }

    /**
     * Создает модификатор степени gate с выражением, вычисляемым целевой средой.
     *
     * @param expression выражение степени
     * @return модификатор степени
     */
    public static GateModifier power(final ParameterExpression expression) {
        if (expression == null) {
            throw new IllegalArgumentException("Gate power expression must not be null.");
        }
        return new GateModifier(
            GateModifierKind.POWER,
            0,
            0.0,
            expression,
            null
        );
    }

    /**
     * Создает модификатор повторения гейта.
     *
     * @param count количество повторов
     * @return модификатор повторения
     */
    public static GateModifier repeat(final int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Gate repeat count must be positive.");
        }
        return new GateModifier(
            GateModifierKind.REPEAT,
            count,
            0.0,
            null,
            null
        );
    }

    /**
     * Создает именованную аннотацию гейта.
     *
     * @param name имя аннотации
     * @return модификатор-аннотация
     */
    public static GateModifier annotation(final String name) {
        final IdentifierName identifierName = IdentifierName.of(
            name,
            ANNOTATION_NAME_SUBJECT
        );
        return new GateModifier(
            GateModifierKind.ANNOTATION,
            0,
            0.0,
            null,
            identifierName.value()
        );
    }

    /**
     * Возвращает тип модификатора.
     *
     * @return тип модификатора
     */
    public GateModifierKind kind() {
        return kind;
    }

    /**
     * Возвращает целочисленное значение модификатора.
     *
     * @return количество управляющих кубитов или повторов
     */
    public int integerValue() {
        return integerValue;
    }

    /**
     * Возвращает вещественное значение модификатора.
     *
     * @return степень гейта
     */
    public double doubleValue() {
        return doubleValue;
    }

    /**
     * Проверяет, что степень задана выражением, а не числом.
     *
     * @return true, если POWER хранит выражение степени
     */
    public boolean hasPowerExpression() {
        return kind == GateModifierKind.POWER && powerExpression != null;
    }

    /**
     * Возвращает выражение степени.
     *
     * @return выражение степени
     */
    public ParameterExpression powerExpression() {
        if (!hasPowerExpression()) {
            throw new IllegalStateException("Gate modifier does not have a power expression.");
        }
        return powerExpression;
    }

    /**
     * Возвращает имя аннотации.
     *
     * @return имя аннотации
     */
    public String annotationName() {
        if (kind != GateModifierKind.ANNOTATION) {
            throw new IllegalStateException("Gate modifier is not an annotation.");
        }
        return annotationName;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GateModifier modifier)) {
            return false;
        }
        return kind == modifier.kind
            && integerValue == modifier.integerValue
            && Double.compare(
                doubleValue,
                modifier.doubleValue
            ) == 0
            && Objects.equals(
                powerExpression,
                modifier.powerExpression
            )
            && Objects.equals(
                annotationName,
                modifier.annotationName
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            kind,
            integerValue,
            doubleValue,
            powerExpression,
            annotationName
        );
    }
}
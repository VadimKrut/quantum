/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.library.domain;

/**
 * Описание одного настраиваемого параметра алгоритма.
 */
public final class AlgorithmParameterDefinition {

    private final String name;
    private final AlgorithmParameterType type;
    private final String description;
    private final Object defaultValue;
    private final Long minIntegerValue;
    private final Long maxIntegerValue;
    private final Double minDoubleValue;
    private final Double maxDoubleValue;

    private AlgorithmParameterDefinition(
        final String name,
        final AlgorithmParameterType type,
        final String description,
        final Object defaultValue,
        final Long minIntegerValue,
        final Long maxIntegerValue,
        final Double minDoubleValue,
        final Double maxDoubleValue
    ) {
        validateText(
            name,
            "Algorithm parameter name"
        );
        if (type == null) {
            throw new IllegalArgumentException("Algorithm parameter type must not be null.");
        }
        validateText(
            description,
            "Algorithm parameter description"
        );
        this.name = name;
        this.type = type;
        this.description = description;
        this.defaultValue = defaultValue;
        this.minIntegerValue = minIntegerValue;
        this.maxIntegerValue = maxIntegerValue;
        this.minDoubleValue = minDoubleValue;
        this.maxDoubleValue = maxDoubleValue;
        validateValue(defaultValue);
    }

    /**
     * Создает целочисленный параметр с диапазоном.
     *
     * @param name имя параметра
     * @param description описание параметра
     * @param defaultValue значение по умолчанию
     * @param minValue минимальное допустимое значение
     * @param maxValue максимальное допустимое значение
     * @return описание параметра
     */
    public static AlgorithmParameterDefinition integer(
        final String name,
        final String description,
        final int defaultValue,
        final int minValue,
        final int maxValue
    ) {
        return new AlgorithmParameterDefinition(
            name,
            AlgorithmParameterType.INTEGER,
            description,
            Integer.valueOf(defaultValue),
            Long.valueOf(minValue),
            Long.valueOf(maxValue),
            null,
            null
        );
    }

    /**
     * Создает long-параметр с диапазоном.
     *
     * @param name имя параметра
     * @param description описание параметра
     * @param defaultValue значение по умолчанию
     * @param minValue минимальное допустимое значение
     * @param maxValue максимальное допустимое значение
     * @return описание параметра
     */
    public static AlgorithmParameterDefinition longInteger(
        final String name,
        final String description,
        final long defaultValue,
        final long minValue,
        final long maxValue
    ) {
        return new AlgorithmParameterDefinition(
            name,
            AlgorithmParameterType.LONG,
            description,
            Long.valueOf(defaultValue),
            Long.valueOf(minValue),
            Long.valueOf(maxValue),
            null,
            null
        );
    }

    /**
     * Создает double-параметр с диапазоном.
     *
     * @param name имя параметра
     * @param description описание параметра
     * @param defaultValue значение по умолчанию
     * @param minValue минимальное допустимое значение
     * @param maxValue максимальное допустимое значение
     * @return описание параметра
     */
    public static AlgorithmParameterDefinition decimal(
        final String name,
        final String description,
        final double defaultValue,
        final double minValue,
        final double maxValue
    ) {
        return new AlgorithmParameterDefinition(
            name,
            AlgorithmParameterType.DOUBLE,
            description,
            Double.valueOf(defaultValue),
            null,
            null,
            Double.valueOf(minValue),
            Double.valueOf(maxValue)
        );
    }

    /**
     * Создает boolean-параметр.
     *
     * @param name имя параметра
     * @param description описание параметра
     * @param defaultValue значение по умолчанию
     * @return описание параметра
     */
    public static AlgorithmParameterDefinition bool(
        final String name,
        final String description,
        final boolean defaultValue
    ) {
        return new AlgorithmParameterDefinition(
            name,
            AlgorithmParameterType.BOOLEAN,
            description,
            Boolean.valueOf(defaultValue),
            null,
            null,
            null,
            null
        );
    }

    /**
     * Создает строковый параметр.
     *
     * @param name имя параметра
     * @param description описание параметра
     * @param defaultValue значение по умолчанию
     * @return описание параметра
     */
    public static AlgorithmParameterDefinition text(
        final String name,
        final String description,
        final String defaultValue
    ) {
        return new AlgorithmParameterDefinition(
            name,
            AlgorithmParameterType.STRING,
            description,
            defaultValue,
            null,
            null,
            null,
            null
        );
    }

    /**
     * Возвращает имя параметра.
     *
     * @return имя параметра
     */
    public String name() {
        return name;
    }

    /**
     * Возвращает тип параметра.
     *
     * @return тип параметра
     */
    public AlgorithmParameterType type() {
        return type;
    }

    /**
     * Возвращает описание параметра.
     *
     * @return описание параметра
     */
    public String description() {
        return description;
    }

    /**
     * Возвращает значение по умолчанию.
     *
     * @return значение по умолчанию
     */
    public Object defaultValue() {
        return defaultValue;
    }

    /**
     * Проверяет внешнее значение по типу и диапазону.
     *
     * @param value проверяемое значение
     */
    public void validateValue(final Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Algorithm parameter value must not be null: " + name + ".");
        }
        if (type == AlgorithmParameterType.INTEGER) {
            validateIntegerValue(value);
        } else if (type == AlgorithmParameterType.LONG) {
            validateLongValue(value);
        } else if (type == AlgorithmParameterType.DOUBLE) {
            validateDoubleValue(value);
        } else if (type == AlgorithmParameterType.BOOLEAN) {
            if (!(value instanceof Boolean)) {
                throw new IllegalArgumentException("Algorithm parameter must be boolean: " + name + ".");
            }
        } else if (!(value instanceof String)) {
            throw new IllegalArgumentException("Algorithm parameter must be string: " + name + ".");
        }
    }

    private void validateIntegerValue(final Object value) {
        if (!(value instanceof Integer)) {
            throw new IllegalArgumentException("Algorithm parameter must be int: " + name + ".");
        }
        final int intValue = ((Integer) value).intValue();
        validateLongRange(intValue);
    }

    private void validateLongValue(final Object value) {
        if (!(value instanceof Long)) {
            throw new IllegalArgumentException("Algorithm parameter must be long: " + name + ".");
        }
        validateLongRange(((Long) value).longValue());
    }

    private void validateDoubleValue(final Object value) {
        if (!(value instanceof Double)) {
            throw new IllegalArgumentException("Algorithm parameter must be double: " + name + ".");
        }
        final double doubleValue = ((Double) value).doubleValue();
        if (!Double.isFinite(doubleValue)) {
            throw new IllegalArgumentException("Algorithm parameter must be finite: " + name + ".");
        }
        if (
            minDoubleValue != null
            && doubleValue < minDoubleValue.doubleValue()
        ) {
            throw new IllegalArgumentException("Algorithm parameter is below minimum: " + name + ".");
        }
        if (
            maxDoubleValue != null
            && doubleValue > maxDoubleValue.doubleValue()
        ) {
            throw new IllegalArgumentException("Algorithm parameter is above maximum: " + name + ".");
        }
    }

    private void validateLongRange(final long value) {
        if (
            minIntegerValue != null
            && value < minIntegerValue.longValue()
        ) {
            throw new IllegalArgumentException("Algorithm parameter is below minimum: " + name + ".");
        }
        if (
            maxIntegerValue != null
            && value > maxIntegerValue.longValue()
        ) {
            throw new IllegalArgumentException("Algorithm parameter is above maximum: " + name + ".");
        }
    }

    private static void validateText(
        final String value,
        final String subject
    ) {
        if (
            value == null
            || value.isBlank()
        ) {
            throw new IllegalArgumentException(subject + " must not be blank.");
        }
    }
}
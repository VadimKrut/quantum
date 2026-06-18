/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.library.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Неизменяемый набор значений параметров для генератора алгоритма.
 */
public final class AlgorithmParameterSet {

    private final Map<String, Object> values;

    private AlgorithmParameterSet(final Map<String, Object> values) {
        if (values == null) {
            throw new IllegalArgumentException("Algorithm parameter values must not be null.");
        }
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    /**
     * Создает пустой набор параметров.
     *
     * @return пустой набор параметров
     */
    public static AlgorithmParameterSet empty() {
        return new AlgorithmParameterSet(Map.of());
    }

    /**
     * Создает изменяемый builder параметров.
     *
     * @return builder параметров
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Проверяет наличие значения.
     *
     * @param name имя параметра
     * @return true, если значение задано
     */
    public boolean contains(final String name) {
        return values.containsKey(name);
    }

    /**
     * Возвращает сырое значение параметра.
     *
     * @param name имя параметра
     * @return значение параметра
     */
    public Object value(final String name) {
        if (!values.containsKey(name)) {
            throw new IllegalArgumentException("Algorithm parameter is not provided: " + name + ".");
        }
        return values.get(name);
    }

    /**
     * Возвращает int-значение.
     *
     * @param name имя параметра
     * @return int-значение
     */
    public int integer(final String name) {
        final Object value = value(name);
        if (!(value instanceof Integer)) {
            throw new IllegalArgumentException("Algorithm parameter is not int: " + name + ".");
        }
        return ((Integer) value).intValue();
    }

    /**
     * Возвращает long-значение.
     *
     * @param name имя параметра
     * @return long-значение
     */
    public long longInteger(final String name) {
        final Object value = value(name);
        if (value instanceof Long) {
            return ((Long) value).longValue();
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        throw new IllegalArgumentException("Algorithm parameter is not long: " + name + ".");
    }

    /**
     * Возвращает double-значение.
     *
     * @param name имя параметра
     * @return double-значение
     */
    public double decimal(final String name) {
        final Object value = value(name);
        if (!(value instanceof Double)) {
            throw new IllegalArgumentException("Algorithm parameter is not double: " + name + ".");
        }
        return ((Double) value).doubleValue();
    }

    /**
     * Возвращает boolean-значение.
     *
     * @param name имя параметра
     * @return boolean-значение
     */
    public boolean bool(final String name) {
        final Object value = value(name);
        if (!(value instanceof Boolean)) {
            throw new IllegalArgumentException("Algorithm parameter is not boolean: " + name + ".");
        }
        return ((Boolean) value).booleanValue();
    }

    /**
     * Возвращает строковое значение.
     *
     * @param name имя параметра
     * @return строковое значение
     */
    public String text(final String name) {
        final Object value = value(name);
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("Algorithm parameter is not string: " + name + ".");
        }
        return (String) value;
    }

    /**
     * Возвращает immutable snapshot параметров.
     *
     * @return значения параметров
     */
    public Map<String, Object> values() {
        return values;
    }

    /**
     * Builder для набора параметров алгоритма.
     */
    public static final class Builder {

        private final LinkedHashMap<String, Object> values;

        private Builder() {
            values = new LinkedHashMap<>();
        }

        /**
         * Добавляет int-значение.
         *
         * @param name имя параметра
         * @param value значение параметра
         * @return текущий builder
         */
        public Builder integer(
            final String name,
            final int value
        ) {
            return put(
                name,
                Integer.valueOf(value)
            );
        }

        /**
         * Добавляет long-значение.
         *
         * @param name имя параметра
         * @param value значение параметра
         * @return текущий builder
         */
        public Builder longInteger(
            final String name,
            final long value
        ) {
            return put(
                name,
                Long.valueOf(value)
            );
        }

        /**
         * Добавляет double-значение.
         *
         * @param name имя параметра
         * @param value значение параметра
         * @return текущий builder
         */
        public Builder decimal(
            final String name,
            final double value
        ) {
            return put(
                name,
                Double.valueOf(value)
            );
        }

        /**
         * Добавляет boolean-значение.
         *
         * @param name имя параметра
         * @param value значение параметра
         * @return текущий builder
         */
        public Builder bool(
            final String name,
            final boolean value
        ) {
            return put(
                name,
                Boolean.valueOf(value)
            );
        }

        /**
         * Добавляет строковое значение.
         *
         * @param name имя параметра
         * @param value значение параметра
         * @return текущий builder
         */
        public Builder text(
            final String name,
            final String value
        ) {
            return put(
                name,
                value
            );
        }

        /**
         * Создает неизменяемый набор параметров.
         *
         * @return набор параметров
         */
        public AlgorithmParameterSet build() {
            return new AlgorithmParameterSet(values);
        }

        private Builder put(
            final String name,
            final Object value
        ) {
            if (
                name == null
                || name.isBlank()
            ) {
                throw new IllegalArgumentException("Algorithm parameter name must not be blank.");
            }
            if (value == null) {
                throw new IllegalArgumentException("Algorithm parameter value must not be null.");
            }
            values.put(
                name,
                value
            );
            return this;
        }
    }
}
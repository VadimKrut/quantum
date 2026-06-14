/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.parameter;

import java.util.LinkedHashMap;
import java.util.Map;

import ru.pathcreator.vadim.quantum.domain.naming.IdentifierName;

/**
 * Неизменяемый набор числовых значений для символических параметров.
 */
public final class ParameterBindings {

    private static final String SUBJECT_NAME = "Parameter binding name";

    private final Map<String, Double> values;

    private ParameterBindings(final Map<String, Double> values) {
        this.values = values;
    }

    /**
     * Создает пустой набор значений параметров.
     *
     * @return пустой набор
     */
    public static ParameterBindings empty() {
        return new ParameterBindings(Map.of());
    }

    /**
     * Создает builder для набора значений параметров.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Проверяет наличие значения.
     *
     * @param name имя параметра
     * @return true, если значение есть
     */
    public boolean contains(final String name) {
        final IdentifierName identifierName = IdentifierName.of(
            name,
            SUBJECT_NAME
        );
        return values.containsKey(identifierName.value());
    }

    /**
     * Возвращает значение параметра.
     *
     * @param name имя параметра
     * @return значение
     */
    public double value(final String name) {
        final IdentifierName identifierName = IdentifierName.of(
            name,
            SUBJECT_NAME
        );
        final Double value = values.get(identifierName.value());
        if (value == null) {
            throw new IllegalArgumentException("Parameter binding is missing: " + identifierName.value() + ".");
        }
        return value;
    }

    /**
     * Возвращает неизменяемый снимок значений параметров.
     *
     * @return values
     */
    public Map<String, Double> values() {
        return values;
    }

    /**
     * Builder для неизменяемого набора значений параметров.
     */
    public static final class Builder {

        private final LinkedHashMap<String, Double> values;

        private Builder() {
            this.values = new LinkedHashMap<>();
        }

        /**
         * Добавляет значение параметра.
         *
         * @param name имя параметра
         * @param value конечное числовое значение
         * @return текущий builder
         */
        public Builder put(
            final String name,
            final double value
        ) {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("Parameter binding value must be finite.");
            }
            final IdentifierName identifierName = IdentifierName.of(
                name,
                SUBJECT_NAME
            );
            values.put(
                identifierName.value(),
                value
            );
            return this;
        }

        /**
         * Создает неизменяемый набор значений параметров.
         *
         * @return bindings
         */
        public ParameterBindings build() {
            return new ParameterBindings(Map.copyOf(values));
        }
    }
}
/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.library.domain;

import java.util.LinkedHashMap;

import ru.pathcreator.vadim.quantum.api.Quantum;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

/**
 * Исполняемая запись библиотеки: описание, параметры, генератор и встроенная проверка результата.
 */
public final class QuantumAlgorithmEntry {

    private final QuantumAlgorithmDescriptor descriptor;
    private final QuantumAlgorithmGenerator generator;

    /**
     * Создает запись библиотеки.
     *
     * @param descriptor описание алгоритма
     * @param generator генератор Quantum IR
     */
    public QuantumAlgorithmEntry(
        final QuantumAlgorithmDescriptor descriptor,
        final QuantumAlgorithmGenerator generator
    ) {
        if (descriptor == null) {
            throw new IllegalArgumentException("Algorithm descriptor must not be null.");
        }
        if (generator == null) {
            throw new IllegalArgumentException("Algorithm generator must not be null.");
        }
        this.descriptor = descriptor;
        this.generator = generator;
    }

    /**
     * Возвращает описание алгоритма.
     *
     * @return описание алгоритма
     */
    public QuantumAlgorithmDescriptor descriptor() {
        return descriptor;
    }

    /**
     * Создает программу со значениями по умолчанию.
     *
     * @return Quantum IR программа
     */
    public QuantumProgram generate() {
        return generate(defaultParameters());
    }

    /**
     * Создает программу с пользовательскими параметрами.
     *
     * @param parameters пользовательские параметры
     * @return Quantum IR программа
     */
    public QuantumProgram generate(final AlgorithmParameterSet parameters) {
        final AlgorithmParameterSet normalized = normalize(parameters);
        final QuantumProgram program = generator.generate(normalized);
        if (program == null) {
            throw new IllegalStateException("Algorithm generator returned null program: " + descriptor.id() + ".");
        }
        final ValidationResult validationResult = Quantum.validate(program);
        if (!validationResult.isValid()) {
            throw new IllegalStateException("Algorithm generated invalid Quantum IR: " + descriptor.id() + ".");
        }
        return program;
    }

    /**
     * Возвращает параметры по умолчанию.
     *
     * @return набор параметров по умолчанию
     */
    public AlgorithmParameterSet defaultParameters() {
        final AlgorithmParameterSet.Builder builder = AlgorithmParameterSet.builder();
        for (int i = 0; i < descriptor.parameters().size(); i++) {
            final AlgorithmParameterDefinition parameter = descriptor.parameters().get(i);
            putRaw(
                builder,
                parameter.name(),
                parameter.defaultValue()
            );
        }
        return builder.build();
    }

    private AlgorithmParameterSet normalize(final AlgorithmParameterSet parameters) {
        final AlgorithmParameterSet source = parameters == null ? AlgorithmParameterSet.empty() : parameters;
        final LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        rejectUnknownParameters(source);
        for (int i = 0; i < descriptor.parameters().size(); i++) {
            final AlgorithmParameterDefinition parameter = descriptor.parameters().get(i);
            final Object value = source.contains(parameter.name())
                ? source.value(parameter.name())
                : parameter.defaultValue();
            parameter.validateValue(value);
            values.put(
                parameter.name(),
                value
            );
        }
        return copy(values);
    }

    private void rejectUnknownParameters(final AlgorithmParameterSet source) {
        final String[] names = source.values().keySet().toArray(new String[0]);
        for (int i = 0; i < names.length; i++) {
            boolean known = false;
            for (int j = 0; j < descriptor.parameters().size(); j++) {
                if (descriptor.parameters().get(j).name().equals(names[i])) {
                    known = true;
                    break;
                }
            }
            if (!known) {
                throw new IllegalArgumentException("Unknown algorithm parameter: " + names[i] + ".");
            }
        }
    }

    private static AlgorithmParameterSet copy(final LinkedHashMap<String, Object> values) {
        final AlgorithmParameterSet.Builder builder = AlgorithmParameterSet.builder();
        final String[] names = values.keySet().toArray(new String[0]);
        for (int i = 0; i < names.length; i++) {
            putRaw(
                builder,
                names[i],
                values.get(names[i])
            );
        }
        return builder.build();
    }

    private static void putRaw(
        final AlgorithmParameterSet.Builder builder,
        final String name,
        final Object value
    ) {
        if (value instanceof Integer) {
            builder.integer(
                name,
                ((Integer) value).intValue()
            );
        } else if (value instanceof Long) {
            builder.longInteger(
                name,
                ((Long) value).longValue()
            );
        } else if (value instanceof Double) {
            builder.decimal(
                name,
                ((Double) value).doubleValue()
            );
        } else if (value instanceof Boolean) {
            builder.bool(
                name,
                ((Boolean) value).booleanValue()
            );
        } else if (value instanceof String) {
            builder.text(
                name,
                (String) value
            );
        } else {
            throw new IllegalArgumentException("Unsupported algorithm parameter value type.");
        }
    }
}
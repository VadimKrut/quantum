/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.library.builtin;

import java.util.List;

import ru.pathcreator.vadim.quantum.api.Quantum;
import ru.pathcreator.vadim.quantum.api.QuantumCircuitBuilder;
import ru.pathcreator.vadim.quantum.library.domain.AlgorithmCategory;
import ru.pathcreator.vadim.quantum.library.domain.AlgorithmDifficulty;
import ru.pathcreator.vadim.quantum.library.domain.AlgorithmParameterDefinition;
import ru.pathcreator.vadim.quantum.library.domain.QuantumAlgorithmDescriptor;
import ru.pathcreator.vadim.quantum.library.domain.QuantumAlgorithmEntry;
import ru.pathcreator.vadim.quantum.library.domain.QuantumAlgorithmGenerator;

/**
 * Общие factory-методы для встроенных алгоритмов без привязки к конкретной категории.
 */
public final class BuiltInAlgorithmSupport {

    public static final String QUBITS_PARAMETER = "qubits";
    public static final String SECRET_MASK_PARAMETER = "secretMask";
    public static final String BALANCED_PARAMETER = "balanced";
    public static final String THETA_PARAMETER = "theta";
    public static final String MESSAGE_PARAMETER = "message";

    private BuiltInAlgorithmSupport() {
    }

    /**
     * Создает описание алгоритма.
     *
     * @param id стабильный id
     * @param displayName отображаемое имя
     * @param summary краткое описание
     * @param category категория
     * @param difficulty сложность
     * @param tags теги
     * @param parameters параметры
     * @return описание алгоритма
     */
    public static QuantumAlgorithmDescriptor descriptor(
        final String id,
        final String displayName,
        final String summary,
        final AlgorithmCategory category,
        final AlgorithmDifficulty difficulty,
        final List<String> tags,
        final List<AlgorithmParameterDefinition> parameters
    ) {
        return new QuantumAlgorithmDescriptor(
            id,
            displayName,
            summary,
            category,
            difficulty,
            tags,
            parameters
        );
    }

    /**
     * Создает описание алгоритма с reference-ссылками.
     *
     * @param id стабильный id
     * @param displayName отображаемое имя
     * @param summary краткое описание
     * @param category категория
     * @param difficulty сложность
     * @param tags теги
     * @param referenceUris reference-ссылки
     * @param parameters параметры
     * @return описание алгоритма
     */
    public static QuantumAlgorithmDescriptor descriptor(
        final String id,
        final String displayName,
        final String summary,
        final AlgorithmCategory category,
        final AlgorithmDifficulty difficulty,
        final List<String> tags,
        final List<String> referenceUris,
        final List<AlgorithmParameterDefinition> parameters
    ) {
        return new QuantumAlgorithmDescriptor(
            id,
            displayName,
            summary,
            category,
            difficulty,
            tags,
            referenceUris,
            parameters
        );
    }

    /**
     * Создает запись библиотеки.
     *
     * @param descriptor описание алгоритма
     * @param generator генератор программы
     * @return запись библиотеки
     */
    public static QuantumAlgorithmEntry entry(
        final QuantumAlgorithmDescriptor descriptor,
        final QuantumAlgorithmGenerator generator
    ) {
        return new QuantumAlgorithmEntry(
            descriptor,
            generator
        );
    }

    /**
     * Создает схему с q/c регистрами одинакового размера.
     *
     * @param name имя схемы
     * @param qubits размер регистров
     * @return builder схемы
     */
    public static QuantumCircuitBuilder registerOnlyCircuit(
        final String name,
        final int qubits
    ) {
        return Quantum.programBuilder()
            .circuit(name)
            .qreg("q", qubits)
            .creg("c", qubits);
    }

    /**
     * Добавляет измерения q[i] -> c[i].
     *
     * @param circuit схема
     * @param qubits количество qubit
     */
    public static void measureAll(
        final QuantumCircuitBuilder circuit,
        final int qubits
    ) {
        for (int i = 0; i < qubits; i++) {
            circuit.measure(
                q(i),
                c(i)
            );
        }
    }

    /**
     * Добавляет QFT или inverse QFT в существующий регистр q.
     *
     * @param circuit схема
     * @param qubits размер регистра
     * @param inverse true для inverse QFT
     */
    public static void appendQft(
        final QuantumCircuitBuilder circuit,
        final int qubits,
        final boolean inverse
    ) {
        if (inverse) {
            appendReverseSwaps(
                circuit,
                qubits
            );
            for (int target = qubits - 1; target >= 0; target--) {
                for (int control = qubits - 1; control > target; control--) {
                    circuit.cphase(
                        -phaseAngle(control - target),
                        q(control),
                        q(target)
                    );
                }
                circuit.h(q(target));
            }
            return;
        }
        for (int target = 0; target < qubits; target++) {
            circuit.h(q(target));
            for (int control = target + 1; control < qubits; control++) {
                circuit.cphase(
                    phaseAngle(control - target),
                    q(control),
                    q(target)
                );
            }
        }
        appendReverseSwaps(
            circuit,
            qubits
        );
    }

    /**
     * Возвращает ссылку q[index].
     *
     * @param index индекс qubit
     * @return ссылка q[index]
     */
    public static String q(final int index) {
        return "q[" + index + "]";
    }

    /**
     * Возвращает ссылку c[index].
     *
     * @param index индекс classical bit
     * @return ссылка c[index]
     */
    public static String c(final int index) {
        return "c[" + index + "]";
    }

    /**
     * Проверяет, что bitmask помещается в заданное число qubit.
     *
     * @param inputQubits количество входных qubit
     * @param secretMask маска
     */
    public static void validateSecretMask(
        final int inputQubits,
        final long secretMask
    ) {
        if (
            inputQubits < Long.SIZE - 1
            && secretMask >= (1L << inputQubits)
        ) {
            throw new IllegalArgumentException("Secret mask does not fit into requested qubit count.");
        }
    }

    private static void appendReverseSwaps(
        final QuantumCircuitBuilder circuit,
        final int qubits
    ) {
        final int half = qubits / 2;
        for (int i = 0; i < half; i++) {
            circuit.swap(
                q(i),
                q(qubits - 1 - i)
            );
        }
    }

    private static double phaseAngle(final int distance) {
        return Math.PI / (1L << distance);
    }
}
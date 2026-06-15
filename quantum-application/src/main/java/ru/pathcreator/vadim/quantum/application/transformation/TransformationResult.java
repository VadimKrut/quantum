/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.transformation;

import java.util.List;
import java.util.Objects;

import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

/**
 * Immutable результат консервативной трансформации Quantum IR.
 */
public final class TransformationResult {

    /**
     * Исходная программа, переданная пользователем.
     */
    private final QuantumProgram originalProgram;

    /**
     * Новая программа после копирования и применения включенных трансформаций.
     */
    private final QuantumProgram transformedProgram;

    /**
     * Примененные шаги с объяснениями.
     */
    private final List<TransformationStepRecord> appliedSteps;

    /**
     * Пропущенные шаги с объяснениями.
     */
    private final List<TransformationStepRecord> skippedSteps;

    /**
     * Диагностики трансформации.
     */
    private final List<TransformationDiagnostic> diagnostics;

    private TransformationResult(
        final QuantumProgram originalProgram,
        final QuantumProgram transformedProgram,
        final List<TransformationStepRecord> appliedSteps,
        final List<TransformationStepRecord> skippedSteps,
        final List<TransformationDiagnostic> diagnostics
    ) {
        this.originalProgram = originalProgram;
        this.transformedProgram = transformedProgram;
        this.appliedSteps = appliedSteps;
        this.skippedSteps = skippedSteps;
        this.diagnostics = diagnostics;
    }

    /**
     * Создает результат трансформации.
     *
     * @param originalProgram исходная программа
     * @param transformedProgram трансформированная программа
     * @param appliedSteps примененные шаги
     * @param skippedSteps пропущенные шаги
     * @param diagnostics диагностики
     * @return результат трансформации
     */
    public static TransformationResult of(
        final QuantumProgram originalProgram,
        final QuantumProgram transformedProgram,
        final List<TransformationStepRecord> appliedSteps,
        final List<TransformationStepRecord> skippedSteps,
        final List<TransformationDiagnostic> diagnostics
    ) {
        if (originalProgram == null) {
            throw new IllegalArgumentException("Original quantum program must not be null.");
        }
        if (transformedProgram == null) {
            throw new IllegalArgumentException("Transformed quantum program must not be null.");
        }
        if (appliedSteps == null) {
            throw new IllegalArgumentException("Applied transformation steps must not be null.");
        }
        if (skippedSteps == null) {
            throw new IllegalArgumentException("Skipped transformation steps must not be null.");
        }
        if (diagnostics == null) {
            throw new IllegalArgumentException("Transformation diagnostics must not be null.");
        }
        return new TransformationResult(
            originalProgram,
            transformedProgram,
            List.copyOf(appliedSteps),
            List.copyOf(skippedSteps),
            List.copyOf(diagnostics)
        );
    }

    /**
     * Возвращает исходную программу.
     *
     * @return исходная программа
     */
    public QuantumProgram originalProgram() {
        return originalProgram;
    }

    /**
     * Возвращает трансформированную программу.
     *
     * @return трансформированная программа
     */
    public QuantumProgram transformedProgram() {
        return transformedProgram;
    }

    /**
     * Возвращает примененные шаги.
     *
     * @return примененные шаги
     */
    public List<TransformationStepRecord> appliedSteps() {
        return appliedSteps;
    }

    /**
     * Возвращает пропущенные шаги.
     *
     * @return пропущенные шаги
     */
    public List<TransformationStepRecord> skippedSteps() {
        return skippedSteps;
    }

    /**
     * Возвращает диагностики.
     *
     * @return диагностики
     */
    public List<TransformationDiagnostic> diagnostics() {
        return diagnostics;
    }

    /**
     * Проверяет, завершилась ли трансформация без error-диагностик.
     *
     * @return true, если error-диагностик нет
     */
    public boolean isSuccess() {
        return !hasErrors();
    }

    /**
     * Проверяет наличие error-диагностик.
     *
     * @return true, если есть error-диагностика
     */
    public boolean hasErrors() {
        for (int i = 0; i < diagnostics.size(); i++) {
            if (diagnostics.get(i).isError()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TransformationResult result)) {
            return false;
        }
        return originalProgram == result.originalProgram
            && transformedProgram == result.transformedProgram
            && Objects.equals(
                appliedSteps,
                result.appliedSteps
            )
            && Objects.equals(
                skippedSteps,
                result.skippedSteps
            )
            && Objects.equals(
                diagnostics,
                result.diagnostics
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            System.identityHashCode(originalProgram),
            System.identityHashCode(transformedProgram),
            appliedSteps,
            skippedSteps,
            diagnostics
        );
    }
}
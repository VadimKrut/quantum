/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.integration.options;

import java.util.Objects;

import ru.pathcreator.vadim.quantum.application.integration.decomposition.GateDecompositionRegistry;

/**
 * Настройки export из Quantum IR во внешний формат.
 */
public final class ExportOptions {

    /**
     * Нужно ли запускать доменную валидацию перед export.
     */
    private final boolean validateBeforeExport;

    /**
     * Нужно ли считать предупреждения ошибками результата.
     */
    private final boolean failOnWarnings;

    /**
     * Политика обработки gate, которые target adapter не пишет напрямую.
     */
    private final UnsupportedGatePolicy unsupportedGatePolicy;

    /**
     * Режим текстового export.
     */
    private final ExportTextMode textMode;

    /**
     * Registry пользовательских или adapter-level decomposition rules.
     */
    private final GateDecompositionRegistry gateDecompositionRegistry;

    private ExportOptions(
        final boolean validateBeforeExport,
        final boolean failOnWarnings,
        final UnsupportedGatePolicy unsupportedGatePolicy,
        final ExportTextMode textMode,
        final GateDecompositionRegistry gateDecompositionRegistry
    ) {
        if (unsupportedGatePolicy == null) {
            throw new IllegalArgumentException("Unsupported gate policy must not be null.");
        }
        if (textMode == null) {
            throw new IllegalArgumentException("Export text mode must not be null.");
        }
        if (gateDecompositionRegistry == null) {
            throw new IllegalArgumentException("Gate decomposition registry must not be null.");
        }
        this.validateBeforeExport = validateBeforeExport;
        this.failOnWarnings = failOnWarnings;
        this.unsupportedGatePolicy = unsupportedGatePolicy;
        this.textMode = textMode;
        this.gateDecompositionRegistry = gateDecompositionRegistry;
    }

    /**
     * Создает настройки export.
     *
     * @param validateBeforeExport запускать ли доменную валидацию перед export
     * @param failOnWarnings считать ли предупреждения ошибками
     * @return настройки export
     */
    public static ExportOptions of(
        final boolean validateBeforeExport,
        final boolean failOnWarnings
    ) {
        return new ExportOptions(
            validateBeforeExport,
            failOnWarnings,
            UnsupportedGatePolicy.OPAQUE_IF_POSSIBLE,
            ExportTextMode.CANONICAL,
            GateDecompositionRegistry.empty()
        );
    }

    /**
     * Создает расширенные настройки export.
     *
     * @param validateBeforeExport запускать ли доменную валидацию перед export
     * @param failOnWarnings считать ли предупреждения ошибками
     * @param unsupportedGatePolicy политика для неподдерживаемых gate
     * @param gateDecompositionRegistry registry decomposition rules
     * @return настройки export
     */
    public static ExportOptions of(
        final boolean validateBeforeExport,
        final boolean failOnWarnings,
        final UnsupportedGatePolicy unsupportedGatePolicy,
        final GateDecompositionRegistry gateDecompositionRegistry
    ) {
        return new ExportOptions(
            validateBeforeExport,
            failOnWarnings,
            unsupportedGatePolicy,
            ExportTextMode.CANONICAL,
            gateDecompositionRegistry
        );
    }

    public static ExportOptions of(
        final boolean validateBeforeExport,
        final boolean failOnWarnings,
        final UnsupportedGatePolicy unsupportedGatePolicy,
        final ExportTextMode textMode,
        final GateDecompositionRegistry gateDecompositionRegistry
    ) {
        return new ExportOptions(
            validateBeforeExport,
            failOnWarnings,
            unsupportedGatePolicy,
            textMode,
            gateDecompositionRegistry
        );
    }

    /**
     * Создает настройки export по умолчанию.
     *
     * @return настройки export по умолчанию
     */
    public static ExportOptions defaults() {
        return new ExportOptions(
            true,
            false,
            UnsupportedGatePolicy.OPAQUE_IF_POSSIBLE,
            ExportTextMode.CANONICAL,
            GateDecompositionRegistry.empty()
        );
    }

    /**
     * Проверяет, нужно ли запускать доменную валидацию перед export.
     *
     * @return true, если нужна валидация перед export
     */
    public boolean validateBeforeExport() {
        return validateBeforeExport;
    }

    /**
     * Проверяет, нужно ли считать предупреждения ошибками.
     *
     * @return true, если предупреждения должны провалить export
     */
    public boolean failOnWarnings() {
        return failOnWarnings;
    }

    /**
     * Возвращает политику обработки неподдерживаемых gate.
     *
     * @return политика
     */
    public UnsupportedGatePolicy unsupportedGatePolicy() {
        return unsupportedGatePolicy;
    }

    public ExportTextMode textMode() {
        return textMode;
    }

    /**
     * Возвращает registry decomposition rules.
     *
     * @return registry decomposition rules
     */
    public GateDecompositionRegistry gateDecompositionRegistry() {
        return gateDecompositionRegistry;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ExportOptions options)) {
            return false;
        }
        return validateBeforeExport == options.validateBeforeExport
            && failOnWarnings == options.failOnWarnings
            && unsupportedGatePolicy == options.unsupportedGatePolicy
            && textMode == options.textMode
            && Objects.equals(
                gateDecompositionRegistry,
                options.gateDecompositionRegistry
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            validateBeforeExport,
            failOnWarnings,
            unsupportedGatePolicy,
            textMode,
            gateDecompositionRegistry
        );
    }
}
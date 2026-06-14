/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.api;

import java.nio.file.Path;

import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightChecker;
import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightResult;
import ru.pathcreator.vadim.quantum.application.integration.contract.QuantumIntegration;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.integration.options.ExportOptions;
import ru.pathcreator.vadim.quantum.application.integration.options.ImportOptions;
import ru.pathcreator.vadim.quantum.application.integration.result.ExportResult;
import ru.pathcreator.vadim.quantum.application.integration.result.ImportResult;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrFileWriteResult;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrReadResult;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrWriteResult;
import ru.pathcreator.vadim.quantum.api.workflow.QuantumExportWorkflowResult;
import ru.pathcreator.vadim.quantum.api.workflow.QuantumImportJsonWorkflowResult;
import ru.pathcreator.vadim.quantum.api.workflow.QuantumJsonExportWorkflowResult;
import ru.pathcreator.vadim.quantum.domain.model.QuantumComputationModel;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.storage.CompactQuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.validation.QuantumProgramValidator;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

/**
 * Единый публичный фасад для основных сценариев работы с Quantum IR.
 */
public final class Quantum {

    private Quantum() {
    }

    /**
     * Создает gate-based Quantum IR программу.
     *
     * @return новая программа
     */
    public static QuantumProgram gateBasedProgram() {
        return QuantumPrograms.gateBased();
    }

    /**
     * Создает fluent builder для gate-based Quantum IR программы.
     *
     * @return builder программы
     */
    public static QuantumProgramBuilder programBuilder() {
        return QuantumProgramBuilder.gateBased();
    }

    /**
     * Создает fluent builder для Quantum IR программы указанной вычислительной модели.
     *
     * @param computationModel вычислительная модель
     * @return builder программы
     */
    public static QuantumProgramBuilder programBuilder(final QuantumComputationModel computationModel) {
        return QuantumProgramBuilder.create(computationModel);
    }

    /**
     * Создает Quantum IR программу указанной вычислительной модели.
     *
     * @param computationModel вычислительная модель
     * @return новая программа
     */
    public static QuantumProgram program(final QuantumComputationModel computationModel) {
        return QuantumPrograms.create(computationModel);
    }

    /**
     * Возвращает интеграцию по формату.
     *
     * @param format внешний формат
     * @return интеграция
     */
    public static QuantumIntegration integration(final IntegrationFormat format) {
        if (format == null) {
            throw new IllegalArgumentException("Integration format must not be null.");
        }
        return switch (format) {
            case OPENQASM_2 -> QuantumIntegrations.openQasm2();
            case OPENQASM_3 -> QuantumIntegrations.openQasm3();
            case QUIL -> QuantumIntegrations.quil();
        };
    }

    /**
     * Импортирует внешний текст в Quantum IR.
     *
     * @param format внешний формат
     * @param source внешний текст
     * @return результат import
     */
    public static ImportResult importProgram(
        final IntegrationFormat format,
        final String source
    ) {
        return integration(format).importProgram(source);
    }

    /**
     * Импортирует OpenQASM 2 в Quantum IR.
     *
     * @param source OpenQASM 2 текст
     * @return результат import
     */
    public static ImportResult importOpenQasm2(final String source) {
        return importProgram(
            IntegrationFormat.OPENQASM_2,
            source
        );
    }

    /**
     * Импортирует OpenQASM 2 в Quantum IR с явными настройками.
     *
     * @param source OpenQASM 2 текст
     * @param options настройки import
     * @return результат import
     */
    public static ImportResult importOpenQasm2(
        final String source,
        final ImportOptions options
    ) {
        return importProgram(
            IntegrationFormat.OPENQASM_2,
            source,
            options
        );
    }

    /**
     * Импортирует OpenQASM 3 в Quantum IR.
     *
     * @param source OpenQASM 3 текст
     * @return результат import
     */
    public static ImportResult importOpenQasm3(final String source) {
        return importProgram(
            IntegrationFormat.OPENQASM_3,
            source
        );
    }

    /**
     * Импортирует OpenQASM 3 в Quantum IR с явными настройками.
     *
     * @param source OpenQASM 3 текст
     * @param options настройки import
     * @return результат import
     */
    public static ImportResult importOpenQasm3(
        final String source,
        final ImportOptions options
    ) {
        return importProgram(
            IntegrationFormat.OPENQASM_3,
            source,
            options
        );
    }

    /**
     * Импортирует Quil в Quantum IR.
     *
     * @param source Quil текст
     * @return результат import
     */
    public static ImportResult importQuil(final String source) {
        return importProgram(
            IntegrationFormat.QUIL,
            source
        );
    }

    /**
     * Импортирует Quil в Quantum IR с явными настройками.
     *
     * @param source Quil текст
     * @param options настройки import
     * @return результат import
     */
    public static ImportResult importQuil(
        final String source,
        final ImportOptions options
    ) {
        return importProgram(
            IntegrationFormat.QUIL,
            source,
            options
        );
    }

    /**
     * Импортирует внешний текст в Quantum IR с явными настройками.
     *
     * @param format внешний формат
     * @param source внешний текст
     * @param options настройки import
     * @return результат import
     */
    public static ImportResult importProgram(
        final IntegrationFormat format,
        final String source,
        final ImportOptions options
    ) {
        return integration(format).importProgram(
            source,
            options
        );
    }

    /**
     * Экспортирует Quantum IR во внешний формат.
     *
     * @param format внешний формат
     * @param program программа
     * @return результат export
     */
    public static ExportResult exportProgram(
        final IntegrationFormat format,
        final QuantumProgram program
    ) {
        return integration(format).exportProgram(program);
    }

    /**
     * Экспортирует Quantum IR в OpenQASM 2.
     *
     * @param program программа
     * @return результат export
     */
    public static ExportResult exportOpenQasm2(final QuantumProgram program) {
        return exportProgram(
            IntegrationFormat.OPENQASM_2,
            program
        );
    }

    /**
     * Экспортирует Quantum IR в OpenQASM 2 с явными настройками.
     *
     * @param program программа
     * @param options настройки export
     * @return результат export
     */
    public static ExportResult exportOpenQasm2(
        final QuantumProgram program,
        final ExportOptions options
    ) {
        return exportProgram(
            IntegrationFormat.OPENQASM_2,
            program,
            options
        );
    }

    /**
     * Экспортирует Quantum IR в OpenQASM 3.
     *
     * @param program программа
     * @return результат export
     */
    public static ExportResult exportOpenQasm3(final QuantumProgram program) {
        return exportProgram(
            IntegrationFormat.OPENQASM_3,
            program
        );
    }

    /**
     * Экспортирует Quantum IR в OpenQASM 3 с явными настройками.
     *
     * @param program программа
     * @param options настройки export
     * @return результат export
     */
    public static ExportResult exportOpenQasm3(
        final QuantumProgram program,
        final ExportOptions options
    ) {
        return exportProgram(
            IntegrationFormat.OPENQASM_3,
            program,
            options
        );
    }

    /**
     * Экспортирует Quantum IR в Quil.
     *
     * @param program программа
     * @return результат export
     */
    public static ExportResult exportQuil(final QuantumProgram program) {
        return exportProgram(
            IntegrationFormat.QUIL,
            program
        );
    }

    /**
     * Экспортирует Quantum IR в Quil с явными настройками.
     *
     * @param program программа
     * @param options настройки export
     * @return результат export
     */
    public static ExportResult exportQuil(
        final QuantumProgram program,
        final ExportOptions options
    ) {
        return exportProgram(
            IntegrationFormat.QUIL,
            program,
            options
        );
    }

    /**
     * Экспортирует Quantum IR во внешний формат с явными настройками.
     *
     * @param format внешний формат
     * @param program программа
     * @param options настройки export
     * @return результат export
     */
    public static ExportResult exportProgram(
        final IntegrationFormat format,
        final QuantumProgram program,
        final ExportOptions options
    ) {
        return integration(format).exportProgram(
            program,
            options
        );
    }

    /**
     * Проверяет Quantum IR общим доменным валидатором.
     *
     * @param program программа
     * @return результат валидации
     */
    public static ValidationResult validate(final QuantumProgram program) {
        return new QuantumProgramValidator().validate(program);
    }

    /**
     * Собирает circuit в плотное представление для больших потоков gate-based операций.
     *
     * @param circuit схема
     * @return плотное представление circuit
     */
    public static CompactQuantumCircuit compact(final QuantumCircuit circuit) {
        return CompactQuantumCircuit.from(circuit);
    }

    /**
     * Проверяет, можно ли экспортировать Quantum IR в target format.
     *
     * @param format внешний формат
     * @param program программа
     * @return результат preflight
     */
    public static CapabilityPreflightResult preflight(
        final IntegrationFormat format,
        final QuantumProgram program
    ) {
        final QuantumIntegration integration = integration(format);
        return new CapabilityPreflightChecker().check(
            program,
            integration.capabilityProfile()
        );
    }

    /**
     * Выполняет workflow: validation -> preflight -> export.
     *
     * @param format целевой внешний формат
     * @param program Quantum IR программа
     * @return результат workflow
     */
    public static QuantumExportWorkflowResult validatePreflightExport(
        final IntegrationFormat format,
        final QuantumProgram program
    ) {
        return validatePreflightExport(
            format,
            program,
            ExportOptions.defaults()
        );
    }

    /**
     * Выполняет workflow: validation -> preflight -> export с явными настройками export.
     *
     * @param format целевой внешний формат
     * @param program Quantum IR программа
     * @param options настройки export
     * @return результат workflow
     */
    public static QuantumExportWorkflowResult validatePreflightExport(
        final IntegrationFormat format,
        final QuantumProgram program,
        final ExportOptions options
    ) {
        final ValidationResult validationResult = validate(program);
        if (!validationResult.isValid()) {
            return QuantumExportWorkflowResult.of(
                format,
                program,
                validationResult,
                null,
                null
            );
        }
        final CapabilityPreflightResult preflightResult = preflight(
            format,
            program
        );
        if (!preflightResult.isSuccess()) {
            return QuantumExportWorkflowResult.of(
                format,
                program,
                validationResult,
                preflightResult,
                null
            );
        }
        return QuantumExportWorkflowResult.of(
            format,
            program,
            validationResult,
            preflightResult,
            exportProgram(
                format,
                program,
                options
            )
        );
    }

    /**
     * Выполняет workflow: build -> validation -> preflight -> export.
     *
     * @param format целевой внешний формат
     * @param builder builder Quantum IR программы
     * @return результат workflow
     */
    public static QuantumExportWorkflowResult buildValidatePreflightExport(
        final IntegrationFormat format,
        final QuantumProgramBuilder builder
    ) {
        return buildValidatePreflightExport(
            format,
            builder,
            ExportOptions.defaults()
        );
    }

    /**
     * Выполняет workflow: build -> validation -> preflight -> export с явными настройками export.
     *
     * @param format целевой внешний формат
     * @param builder builder Quantum IR программы
     * @param options настройки export
     * @return результат workflow
     */
    public static QuantumExportWorkflowResult buildValidatePreflightExport(
        final IntegrationFormat format,
        final QuantumProgramBuilder builder,
        final ExportOptions options
    ) {
        if (builder == null) {
            throw new IllegalArgumentException("Quantum program builder must not be null.");
        }
        return validatePreflightExport(
            format,
            builder.build(),
            options
        );
    }

    /**
     * Выполняет workflow export в OpenQASM 2.
     *
     * @param program Quantum IR программа
     * @return результат workflow
     */
    public static QuantumExportWorkflowResult validatePreflightExportOpenQasm2(final QuantumProgram program) {
        return validatePreflightExport(
            IntegrationFormat.OPENQASM_2,
            program
        );
    }

    /**
     * Выполняет workflow export в OpenQASM 2 с явными настройками export.
     *
     * @param program Quantum IR программа
     * @param options настройки export
     * @return результат workflow
     */
    public static QuantumExportWorkflowResult validatePreflightExportOpenQasm2(
        final QuantumProgram program,
        final ExportOptions options
    ) {
        return validatePreflightExport(
            IntegrationFormat.OPENQASM_2,
            program,
            options
        );
    }

    /**
     * Выполняет workflow build -> export в OpenQASM 2.
     *
     * @param builder builder Quantum IR программы
     * @return результат workflow
     */
    public static QuantumExportWorkflowResult buildValidatePreflightExportOpenQasm2(
        final QuantumProgramBuilder builder
    ) {
        return buildValidatePreflightExport(
            IntegrationFormat.OPENQASM_2,
            builder
        );
    }

    /**
     * Выполняет workflow build -> export в OpenQASM 2 с явными настройками export.
     *
     * @param builder builder Quantum IR программы
     * @param options настройки export
     * @return результат workflow
     */
    public static QuantumExportWorkflowResult buildValidatePreflightExportOpenQasm2(
        final QuantumProgramBuilder builder,
        final ExportOptions options
    ) {
        return buildValidatePreflightExport(
            IntegrationFormat.OPENQASM_2,
            builder,
            options
        );
    }

    /**
     * Выполняет workflow export в OpenQASM 3.
     *
     * @param program Quantum IR программа
     * @return результат workflow
     */
    public static QuantumExportWorkflowResult validatePreflightExportOpenQasm3(final QuantumProgram program) {
        return validatePreflightExport(
            IntegrationFormat.OPENQASM_3,
            program
        );
    }

    /**
     * Выполняет workflow export в OpenQASM 3 с явными настройками export.
     *
     * @param program Quantum IR программа
     * @param options настройки export
     * @return результат workflow
     */
    public static QuantumExportWorkflowResult validatePreflightExportOpenQasm3(
        final QuantumProgram program,
        final ExportOptions options
    ) {
        return validatePreflightExport(
            IntegrationFormat.OPENQASM_3,
            program,
            options
        );
    }

    /**
     * Выполняет workflow build -> export в OpenQASM 3.
     *
     * @param builder builder Quantum IR программы
     * @return результат workflow
     */
    public static QuantumExportWorkflowResult buildValidatePreflightExportOpenQasm3(
        final QuantumProgramBuilder builder
    ) {
        return buildValidatePreflightExport(
            IntegrationFormat.OPENQASM_3,
            builder
        );
    }

    /**
     * Выполняет workflow build -> export в OpenQASM 3 с явными настройками export.
     *
     * @param builder builder Quantum IR программы
     * @param options настройки export
     * @return результат workflow
     */
    public static QuantumExportWorkflowResult buildValidatePreflightExportOpenQasm3(
        final QuantumProgramBuilder builder,
        final ExportOptions options
    ) {
        return buildValidatePreflightExport(
            IntegrationFormat.OPENQASM_3,
            builder,
            options
        );
    }

    /**
     * Выполняет workflow export в Quil.
     *
     * @param program Quantum IR программа
     * @return результат workflow
     */
    public static QuantumExportWorkflowResult validatePreflightExportQuil(final QuantumProgram program) {
        return validatePreflightExport(
            IntegrationFormat.QUIL,
            program
        );
    }

    /**
     * Выполняет workflow export в Quil с явными настройками export.
     *
     * @param program Quantum IR программа
     * @param options настройки export
     * @return результат workflow
     */
    public static QuantumExportWorkflowResult validatePreflightExportQuil(
        final QuantumProgram program,
        final ExportOptions options
    ) {
        return validatePreflightExport(
            IntegrationFormat.QUIL,
            program,
            options
        );
    }

    /**
     * Выполняет workflow build -> export в Quil.
     *
     * @param builder builder Quantum IR программы
     * @return результат workflow
     */
    public static QuantumExportWorkflowResult buildValidatePreflightExportQuil(
        final QuantumProgramBuilder builder
    ) {
        return buildValidatePreflightExport(
            IntegrationFormat.QUIL,
            builder
        );
    }

    /**
     * Выполняет workflow build -> export в Quil с явными настройками export.
     *
     * @param builder builder Quantum IR программы
     * @param options настройки export
     * @return результат workflow
     */
    public static QuantumExportWorkflowResult buildValidatePreflightExportQuil(
        final QuantumProgramBuilder builder,
        final ExportOptions options
    ) {
        return buildValidatePreflightExport(
            IntegrationFormat.QUIL,
            builder,
            options
        );
    }

    /**
     * Выполняет workflow: import -> validation -> streaming JSON write.
     *
     * @param format исходный внешний формат
     * @param source внешний текст
     * @param path путь JSON-файла
     * @return результат workflow
     */
    public static QuantumImportJsonWorkflowResult importValidateWriteJson(
        final IntegrationFormat format,
        final String source,
        final Path path
    ) {
        return importValidateWriteJson(
            format,
            source,
            path,
            ImportOptions.defaults()
        );
    }

    /**
     * Выполняет workflow: import -> validation -> streaming JSON write с явными настройками import.
     *
     * @param format исходный внешний формат
     * @param source внешний текст
     * @param path путь JSON-файла
     * @param options настройки import
     * @return результат workflow
     */
    public static QuantumImportJsonWorkflowResult importValidateWriteJson(
        final IntegrationFormat format,
        final String source,
        final Path path,
        final ImportOptions options
    ) {
        final ImportResult importResult = importProgram(
            format,
            source,
            options
        );
        if (!importResult.isSuccess()) {
            return QuantumImportJsonWorkflowResult.of(
                format,
                path,
                importResult,
                null,
                null
            );
        }
        final ValidationResult validationResult = validate(importResult.program());
        if (!validationResult.isValid()) {
            return QuantumImportJsonWorkflowResult.of(
                format,
                path,
                importResult,
                validationResult,
                null
            );
        }
        return QuantumImportJsonWorkflowResult.of(
            format,
            path,
            importResult,
            validationResult,
            writeJsonStreaming(
                path,
                importResult.program()
            )
        );
    }

    /**
     * Выполняет workflow import OpenQASM 2 -> validation -> JSON.
     *
     * @param source OpenQASM 2 текст
     * @param path путь JSON-файла
     * @return результат workflow
     */
    public static QuantumImportJsonWorkflowResult importOpenQasm2ValidateWriteJson(
        final String source,
        final Path path
    ) {
        return importValidateWriteJson(
            IntegrationFormat.OPENQASM_2,
            source,
            path
        );
    }

    /**
     * Выполняет workflow import OpenQASM 2 -> validation -> JSON с явными настройками import.
     *
     * @param source OpenQASM 2 текст
     * @param path путь JSON-файла
     * @param options настройки import
     * @return результат workflow
     */
    public static QuantumImportJsonWorkflowResult importOpenQasm2ValidateWriteJson(
        final String source,
        final Path path,
        final ImportOptions options
    ) {
        return importValidateWriteJson(
            IntegrationFormat.OPENQASM_2,
            source,
            path,
            options
        );
    }

    /**
     * Выполняет workflow import OpenQASM 3 -> validation -> JSON.
     *
     * @param source OpenQASM 3 текст
     * @param path путь JSON-файла
     * @return результат workflow
     */
    public static QuantumImportJsonWorkflowResult importOpenQasm3ValidateWriteJson(
        final String source,
        final Path path
    ) {
        return importValidateWriteJson(
            IntegrationFormat.OPENQASM_3,
            source,
            path
        );
    }

    /**
     * Выполняет workflow import OpenQASM 3 -> validation -> JSON с явными настройками import.
     *
     * @param source OpenQASM 3 текст
     * @param path путь JSON-файла
     * @param options настройки import
     * @return результат workflow
     */
    public static QuantumImportJsonWorkflowResult importOpenQasm3ValidateWriteJson(
        final String source,
        final Path path,
        final ImportOptions options
    ) {
        return importValidateWriteJson(
            IntegrationFormat.OPENQASM_3,
            source,
            path,
            options
        );
    }

    /**
     * Выполняет workflow import Quil -> validation -> JSON.
     *
     * @param source Quil текст
     * @param path путь JSON-файла
     * @return результат workflow
     */
    public static QuantumImportJsonWorkflowResult importQuilValidateWriteJson(
        final String source,
        final Path path
    ) {
        return importValidateWriteJson(
            IntegrationFormat.QUIL,
            source,
            path
        );
    }

    /**
     * Выполняет workflow import Quil -> validation -> JSON с явными настройками import.
     *
     * @param source Quil текст
     * @param path путь JSON-файла
     * @param options настройки import
     * @return результат workflow
     */
    public static QuantumImportJsonWorkflowResult importQuilValidateWriteJson(
        final String source,
        final Path path,
        final ImportOptions options
    ) {
        return importValidateWriteJson(
            IntegrationFormat.QUIL,
            source,
            path,
            options
        );
    }

    /**
     * Выполняет workflow: JSON read -> validation -> preflight -> export.
     *
     * @param path путь JSON-файла
     * @param format целевой внешний формат
     * @return результат workflow
     */
    public static QuantumJsonExportWorkflowResult readJsonValidatePreflightExport(
        final Path path,
        final IntegrationFormat format
    ) {
        return readJsonValidatePreflightExport(
            path,
            format,
            ExportOptions.defaults()
        );
    }

    /**
     * Выполняет workflow: JSON read -> validation -> preflight -> export с явными настройками export.
     *
     * @param path путь JSON-файла
     * @param format целевой внешний формат
     * @param options настройки export
     * @return результат workflow
     */
    public static QuantumJsonExportWorkflowResult readJsonValidatePreflightExport(
        final Path path,
        final IntegrationFormat format,
        final ExportOptions options
    ) {
        final QuantumIrReadResult readResult = readJson(path);
        if (!readResult.isSuccess()) {
            return QuantumJsonExportWorkflowResult.of(
                format,
                path,
                readResult,
                null,
                null,
                null
            );
        }
        final ValidationResult validationResult = validate(readResult.program());
        if (!validationResult.isValid()) {
            return QuantumJsonExportWorkflowResult.of(
                format,
                path,
                readResult,
                validationResult,
                null,
                null
            );
        }
        final CapabilityPreflightResult preflightResult = preflight(
            format,
            readResult.program()
        );
        if (!preflightResult.isSuccess()) {
            return QuantumJsonExportWorkflowResult.of(
                format,
                path,
                readResult,
                validationResult,
                preflightResult,
                null
            );
        }
        return QuantumJsonExportWorkflowResult.of(
            format,
            path,
            readResult,
            validationResult,
            preflightResult,
            exportProgram(
                format,
                readResult.program(),
                options
            )
        );
    }

    /**
     * Выполняет workflow JSON -> OpenQASM 2.
     *
     * @param path путь JSON-файла
     * @return результат workflow
     */
    public static QuantumJsonExportWorkflowResult readJsonValidatePreflightExportOpenQasm2(final Path path) {
        return readJsonValidatePreflightExport(
            path,
            IntegrationFormat.OPENQASM_2
        );
    }

    /**
     * Выполняет workflow JSON -> OpenQASM 2 с явными настройками export.
     *
     * @param path путь JSON-файла
     * @param options настройки export
     * @return результат workflow
     */
    public static QuantumJsonExportWorkflowResult readJsonValidatePreflightExportOpenQasm2(
        final Path path,
        final ExportOptions options
    ) {
        return readJsonValidatePreflightExport(
            path,
            IntegrationFormat.OPENQASM_2,
            options
        );
    }

    /**
     * Выполняет workflow JSON -> OpenQASM 3.
     *
     * @param path путь JSON-файла
     * @return результат workflow
     */
    public static QuantumJsonExportWorkflowResult readJsonValidatePreflightExportOpenQasm3(final Path path) {
        return readJsonValidatePreflightExport(
            path,
            IntegrationFormat.OPENQASM_3
        );
    }

    /**
     * Выполняет workflow JSON -> OpenQASM 3 с явными настройками export.
     *
     * @param path путь JSON-файла
     * @param options настройки export
     * @return результат workflow
     */
    public static QuantumJsonExportWorkflowResult readJsonValidatePreflightExportOpenQasm3(
        final Path path,
        final ExportOptions options
    ) {
        return readJsonValidatePreflightExport(
            path,
            IntegrationFormat.OPENQASM_3,
            options
        );
    }

    /**
     * Выполняет workflow JSON -> Quil.
     *
     * @param path путь JSON-файла
     * @return результат workflow
     */
    public static QuantumJsonExportWorkflowResult readJsonValidatePreflightExportQuil(final Path path) {
        return readJsonValidatePreflightExport(
            path,
            IntegrationFormat.QUIL
        );
    }

    /**
     * Выполняет workflow JSON -> Quil с явными настройками export.
     *
     * @param path путь JSON-файла
     * @param options настройки export
     * @return результат workflow
     */
    public static QuantumJsonExportWorkflowResult readJsonValidatePreflightExportQuil(
        final Path path,
        final ExportOptions options
    ) {
        return readJsonValidatePreflightExport(
            path,
            IntegrationFormat.QUIL,
            options
        );
    }

    /**
     * Сериализует Quantum IR в JSON.
     *
     * @param program программа
     * @return результат записи
     */
    public static QuantumIrWriteResult writeJson(final QuantumProgram program) {
        return QuantumIrFiles.writeToString(program);
    }

    /**
     * Читает Quantum IR из JSON.
     *
     * @param content JSON-текст
     * @return результат чтения
     */
    public static QuantumIrReadResult readJson(final String content) {
        return QuantumIrFiles.readFromString(content);
    }

    /**
     * Записывает Quantum IR в JSON-файл.
     *
     * @param path путь к файлу
     * @param program программа
     * @return результат записи
     */
    public static QuantumIrWriteResult writeJson(
        final Path path,
        final QuantumProgram program
    ) {
        return QuantumIrFiles.write(
            path,
            program
        );
    }

    /**
     * Потоково записывает Quantum IR в JSON-файл без удержания полного JSON-текста в памяти.
     *
     * @param path путь к файлу
     * @param program программа
     * @return результат потоковой записи
     */
    public static QuantumIrFileWriteResult writeJsonStreaming(
        final Path path,
        final QuantumProgram program
    ) {
        return QuantumIrFiles.writeToFileStreaming(
            path,
            program
        );
    }

    /**
     * Читает Quantum IR из JSON-файла.
     *
     * @param path путь к файлу
     * @return результат чтения
     */
    public static QuantumIrReadResult readJson(final Path path) {
        return QuantumIrFiles.read(path);
    }
}
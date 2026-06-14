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
/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import ru.pathcreator.vadim.quantum.application.persistence.diagnostic.PersistenceDiagnostic;
import ru.pathcreator.vadim.quantum.application.persistence.diagnostic.PersistenceDiagnosticCode;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrReadResult;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrWriteResult;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.infrastructure.persistence.json.QuantumIrJsonReader;
import ru.pathcreator.vadim.quantum.infrastructure.persistence.json.QuantumIrJsonWriter;

/**
 * Удобный API для сохранения и загрузки родного JSON-формата Quantum IR.
 */
public final class QuantumIrFiles {

    private QuantumIrFiles() {
    }

    /**
     * Записывает программу в JSON-строку родного Quantum IR.
     *
     * @param program программа
     * @return результат записи
     */
    public static QuantumIrWriteResult writeToString(final QuantumProgram program) {
        return new QuantumIrJsonWriter().write(program);
    }

    /**
     * Читает программу из JSON-строки родного Quantum IR.
     *
     * @param content JSON-текст
     * @return результат чтения
     */
    public static QuantumIrReadResult readFromString(final String content) {
        return new QuantumIrJsonReader().read(content);
    }

    /**
     * Записывает программу в UTF-8 JSON-файл.
     *
     * @param path путь к файлу
     * @param program программа
     * @return результат записи
     */
    public static QuantumIrWriteResult write(
        final Path path,
        final QuantumProgram program
    ) {
        if (path == null) {
            return QuantumIrWriteResult.failure(List.of(PersistenceDiagnostic.error(
                PersistenceDiagnosticCode.NULL_INPUT,
                "Quantum IR JSON path must not be null."
            )));
        }
        final QuantumIrWriteResult result = writeToString(program);
        if (!result.isSuccess()) {
            return result;
        }
        try {
            final Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(
                path,
                result.content(),
                StandardCharsets.UTF_8
            );
            return result;
        } catch (final IOException exception) {
            return QuantumIrWriteResult.failure(List.of(PersistenceDiagnostic.error(
                PersistenceDiagnosticCode.IO_ERROR,
                "Quantum IR JSON file could not be written: " + exception.getMessage()
            )));
        }
    }

    /**
     * Читает программу из UTF-8 JSON-файла.
     *
     * @param path путь к файлу
     * @return результат чтения
     */
    public static QuantumIrReadResult read(final Path path) {
        if (path == null) {
            return QuantumIrReadResult.failure(List.of(PersistenceDiagnostic.error(
                PersistenceDiagnosticCode.NULL_INPUT,
                "Quantum IR JSON path must not be null."
            )));
        }
        try {
            return readFromString(Files.readString(
                path,
                StandardCharsets.UTF_8
            ));
        } catch (final IOException exception) {
            return QuantumIrReadResult.failure(List.of(PersistenceDiagnostic.error(
                PersistenceDiagnosticCode.IO_ERROR,
                "Quantum IR JSON file could not be read: " + exception.getMessage()
            )));
        }
    }
}
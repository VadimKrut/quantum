/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.quil.adapter;

import java.util.ArrayList;
import java.util.List;

import ru.pathcreator.vadim.quantum.application.integration.contract.QuantumImporter;
import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnostic;
import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnosticCode;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.integration.options.ImportOptions;
import ru.pathcreator.vadim.quantum.application.integration.result.ImportResult;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.validation.QuantumProgramValidator;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationError;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;
import ru.pathcreator.vadim.quantum.infrastructure.quil.syntax.QuilParser;
import ru.pathcreator.vadim.quantum.infrastructure.quil.syntax.QuilParserResult;

/**
 * Importer Quil в Quantum IR.
 */
public final class QuilImporter implements QuantumImporter {

    private final QuilParser parser;
    private final QuantumProgramValidator validator;

    public QuilImporter() {
        this.parser = new QuilParser();
        this.validator = new QuantumProgramValidator();
    }

    @Override
    public IntegrationFormat format() {
        return IntegrationFormat.QUIL;
    }

    @Override
    public ImportResult importProgram(
        final String source,
        final ImportOptions options
    ) {
        final ArrayList<IntegrationDiagnostic> diagnostics = new ArrayList<>();
        if (options == null) {
            diagnostics.add(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.INVALID_OPTIONS,
                "Import options must not be null."
            ));
            return ImportResult.failure(
                format(),
                diagnostics
            );
        }
        final QuilParserResult parserResult = parser.parse(source);
        diagnostics.addAll(parserResult.diagnostics());
        if (parserResult.hasErrors()) {
            return ImportResult.failure(
                format(),
                diagnostics
            );
        }
        final QuantumProgram program = parserResult.program();
        if (options.validateAfterImport()) {
            appendValidationDiagnostics(
                validator.validate(program),
                diagnostics
            );
        }
        if (hasErrors(diagnostics)) {
            return ImportResult.failure(
                format(),
                diagnostics
            );
        }
        return ImportResult.success(
            format(),
            program,
            diagnostics
        );
    }

    private static void appendValidationDiagnostics(
        final ValidationResult validationResult,
        final ArrayList<IntegrationDiagnostic> diagnostics
    ) {
        for (int i = 0; i < validationResult.errorCount(); i++) {
            final ValidationError error = validationResult.error(i);
            diagnostics.add(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.IMPORT_VALIDATION_FAILED,
                "Imported program failed domain validation: " + error.code() + ": " + error.message()
            ));
        }
    }

    private static boolean hasErrors(final List<IntegrationDiagnostic> diagnostics) {
        for (int i = 0; i < diagnostics.size(); i++) {
            if (diagnostics.get(i).isError()) {
                return true;
            }
        }
        return false;
    }
}
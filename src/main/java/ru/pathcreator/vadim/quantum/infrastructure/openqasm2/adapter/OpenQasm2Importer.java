/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.openqasm2.adapter;

import java.util.ArrayList;
import java.util.List;

import ru.pathcreator.vadim.quantum.application.integration.options.ImportOptions;
import ru.pathcreator.vadim.quantum.application.integration.result.ImportResult;
import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnostic;
import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnosticCode;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.integration.contract.QuantumImporter;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.validation.QuantumProgramValidator;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationError;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;
import ru.pathcreator.vadim.quantum.infrastructure.openqasm2.include.OpenQasm2IncludeResolver;
import ru.pathcreator.vadim.quantum.infrastructure.openqasm2.include.OpenQasm2IncludeResolverResult;
import ru.pathcreator.vadim.quantum.infrastructure.openqasm2.syntax.OpenQasm2Parser;
import ru.pathcreator.vadim.quantum.infrastructure.openqasm2.syntax.OpenQasm2ParserResult;

/**
 * Importer OpenQASM 2.0 в Quantum IR.
 */
public final class OpenQasm2Importer implements QuantumImporter {

    /**
     * Parser OpenQASM 2.0.
     */
    private final OpenQasm2Parser parser;

    /**
     * Доменный валидатор, запускаемый после import.
     */
    private final QuantumProgramValidator validator;

    /**
     * Resolver include sources РґР»СЏ OpenQASM 2.
     */
    private final OpenQasm2IncludeResolver includeResolver;

    /**
     * Создает importer OpenQASM 2.0.
     */
    public OpenQasm2Importer() {
        this.parser = new OpenQasm2Parser();
        this.validator = new QuantumProgramValidator();
        this.includeResolver = new OpenQasm2IncludeResolver();
    }

    @Override
    public IntegrationFormat format() {
        return IntegrationFormat.OPENQASM_2;
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

        final OpenQasm2IncludeResolverResult resolverResult = includeResolver.resolve(options);
        if (!resolverResult.isSuccess()) {
            diagnostics.add(resolverResult.diagnostic());
            return ImportResult.failure(
                format(),
                diagnostics
            );
        }

        final OpenQasm2ParserResult parserResult = parser.parse(
            source,
            resolverResult.sources()
        );
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
            if (hasErrors(diagnostics)) {
                return ImportResult.failure(
                    format(),
                    diagnostics
                );
            }
        }
        if (
            options.failOnWarnings()
            && hasWarnings(diagnostics)
        ) {
            diagnostics.add(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.INVALID_OPTIONS,
                "Import warnings are treated as errors by current import options."
            ));
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

    private static boolean hasWarnings(final List<IntegrationDiagnostic> diagnostics) {
        for (int i = 0; i < diagnostics.size(); i++) {
            if (diagnostics.get(i).isWarning()) {
                return true;
            }
        }
        return false;
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
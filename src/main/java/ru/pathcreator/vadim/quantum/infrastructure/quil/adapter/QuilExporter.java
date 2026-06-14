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
import java.util.EnumSet;
import java.util.List;

import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightChecker;
import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightResult;
import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapability;
import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapabilityProfile;
import ru.pathcreator.vadim.quantum.application.integration.contract.QuantumExporter;
import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnostic;
import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnosticCode;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.integration.options.ExportOptions;
import ru.pathcreator.vadim.quantum.application.integration.result.ExportResult;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.validation.QuantumProgramValidator;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationError;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;
import ru.pathcreator.vadim.quantum.infrastructure.quil.syntax.QuilWriter;
import ru.pathcreator.vadim.quantum.infrastructure.quil.syntax.QuilWriterResult;

/**
 * Exporter Quantum IR в Quil.
 */
public final class QuilExporter implements QuantumExporter {

    private final QuantumProgramValidator validator;
    private final CapabilityPreflightChecker preflightChecker;
    private final QuilWriter writer;

    public QuilExporter() {
        this.validator = new QuantumProgramValidator();
        this.preflightChecker = new CapabilityPreflightChecker();
        this.writer = new QuilWriter();
    }

    @Override
    public IntegrationFormat format() {
        return IntegrationFormat.QUIL;
    }

    @Override
    public IntegrationCapabilityProfile capabilityProfile() {
        return IntegrationCapabilityProfile.of(
            format(),
            EnumSet.of(
                IntegrationCapability.QUANTUM_REGISTERS,
                IntegrationCapability.CLASSICAL_REGISTERS,
                IntegrationCapability.MEASUREMENTS,
                IntegrationCapability.RESET,
                IntegrationCapability.BARRIER,
                IntegrationCapability.CLASSICAL_ASSIGNMENTS,
                IntegrationCapability.CLASSICAL_EXTENDED_EXPRESSIONS,
                IntegrationCapability.STRUCTURED_CONTROL_FLOW,
                IntegrationCapability.INSTRUCTION_CONTROL_FLOW,
                IntegrationCapability.CALIBRATIONS
            )
        );
    }

    @Override
    public ExportResult exportProgram(
        final QuantumProgram program,
        final ExportOptions options
    ) {
        final ArrayList<IntegrationDiagnostic> diagnostics = new ArrayList<>();
        if (program == null) {
            diagnostics.add(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.NULL_PROGRAM,
                "Quantum program must not be null."
            ));
            return ExportResult.failure(
                format(),
                diagnostics
            );
        }
        if (options == null) {
            diagnostics.add(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.INVALID_OPTIONS,
                "Export options must not be null."
            ));
            return ExportResult.failure(
                format(),
                diagnostics
            );
        }
        if (options.validateBeforeExport()) {
            appendValidationDiagnostics(
                validator.validate(program),
                diagnostics
            );
        }
        final CapabilityPreflightResult preflightResult = preflightChecker.check(
            program,
            capabilityProfile()
        );
        diagnostics.addAll(preflightResult.diagnostics());
        if (hasErrors(diagnostics)) {
            return ExportResult.failure(
                format(),
                diagnostics
            );
        }
        final QuilWriterResult writerResult = writer.write(program);
        if (!writerResult.isSuccess()) {
            diagnostics.add(writerResult.diagnostic());
            return ExportResult.failure(
                format(),
                diagnostics
            );
        }
        return ExportResult.success(
            format(),
            writerResult.content(),
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
                IntegrationDiagnosticCode.DOMAIN_VALIDATION_FAILED,
                "Domain validation failed: " + error.code() + ": " + error.message()
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
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
import java.util.EnumSet;
import java.util.List;

import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapability;
import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapabilityProfile;
import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightChecker;
import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightResult;
import ru.pathcreator.vadim.quantum.application.integration.options.ExportOptions;
import ru.pathcreator.vadim.quantum.application.integration.options.ExportTextMode;
import ru.pathcreator.vadim.quantum.application.integration.result.ExportResult;
import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnostic;
import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnosticCode;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.integration.contract.QuantumExporter;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.validation.QuantumProgramValidator;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationError;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;
import ru.pathcreator.vadim.quantum.infrastructure.openqasm2.normalization.OpenQasm2ExportNormalizationResult;
import ru.pathcreator.vadim.quantum.infrastructure.openqasm2.normalization.OpenQasm2ExportNormalizer;
import ru.pathcreator.vadim.quantum.infrastructure.openqasm2.syntax.OpenQasm2Writer;
import ru.pathcreator.vadim.quantum.infrastructure.openqasm2.syntax.OpenQasm2WriterResult;

/**
 * Exporter Quantum IR в OpenQASM 2.0.
 */
public final class OpenQasm2Exporter implements QuantumExporter {

    /**
     * Writer OpenQASM 2.0.
     */
    private final OpenQasm2Writer writer;

    /**
     * Доменный валидатор, запускаемый перед export.
     */
    private final QuantumProgramValidator validator;

    /**
     * Adapter-level lowering перед записью OpenQASM 2.
     */
    private final OpenQasm2ExportNormalizer normalizer;

    /**
     * Generic preflight РїСЂРѕРІРµСЂРєР° РїРµСЂРµРґ adapter-specific lowering.
     */
    private final CapabilityPreflightChecker preflightChecker;

    /**
     * Создает exporter OpenQASM 2.0.
     */
    public OpenQasm2Exporter() {
        this.writer = new OpenQasm2Writer();
        this.validator = new QuantumProgramValidator();
        this.normalizer = new OpenQasm2ExportNormalizer();
        this.preflightChecker = new CapabilityPreflightChecker();
    }

    @Override
    public IntegrationFormat format() {
        return IntegrationFormat.OPENQASM_2;
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
                IntegrationCapability.OPAQUE_GATES,
                IntegrationCapability.COMPOSITE_GATES,
                IntegrationCapability.CLASSICAL_REGISTER_CONDITIONS,
                IntegrationCapability.GATE_DECOMPOSITION,
                IntegrationCapability.GATE_MODIFIERS,
                IntegrationCapability.CLASSICAL_ASSIGNMENTS
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
            if (hasErrors(diagnostics)) {
                return ExportResult.failure(
                    format(),
                    diagnostics
                );
            }
        }
        if (options.textMode() == ExportTextMode.LOSSLESS_WHEN_AVAILABLE) {
            diagnostics.add(IntegrationDiagnostic.warning(
                IntegrationDiagnosticCode.OUTPUT_MODE_DOWNGRADED,
                "OpenQASM 2 export does not have lossless source text attached to Quantum IR; canonical output will be emitted."
            ));
            if (options.failOnWarnings()) {
                diagnostics.add(IntegrationDiagnostic.error(
                    IntegrationDiagnosticCode.OUTPUT_MODE_DOWNGRADED,
                    "Export warnings are treated as errors by current export options."
                ));
                return ExportResult.failure(
                    format(),
                    diagnostics
                );
            }
        }
        final OpenQasm2ExportNormalizationResult normalizationResult = normalizer.normalize(
            program,
            options
        );
        diagnostics.addAll(normalizationResult.diagnostics());
        if (hasErrors(diagnostics)) {
            return ExportResult.failure(
                format(),
                diagnostics
            );
        }
        final CapabilityPreflightResult preflightResult = preflightChecker.check(
            normalizationResult.program(),
            capabilityProfile()
        );
        diagnostics.addAll(preflightResult.diagnostics());
        if (hasErrors(diagnostics)) {
            return ExportResult.failure(
                format(),
                diagnostics
            );
        }

        final OpenQasm2WriterResult writerResult = writer.write(normalizationResult.program());
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
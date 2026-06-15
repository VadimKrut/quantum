/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.verification;

import java.util.ArrayList;
import java.util.Map;

import ru.pathcreator.vadim.quantum.application.integration.contract.QuantumIntegration;
import ru.pathcreator.vadim.quantum.application.integration.options.ExportOptions;
import ru.pathcreator.vadim.quantum.application.integration.options.ImportOptions;
import ru.pathcreator.vadim.quantum.application.integration.result.ExportResult;
import ru.pathcreator.vadim.quantum.application.integration.result.ImportResult;
import ru.pathcreator.vadim.quantum.application.simulation.engine.QuantumSimulator;
import ru.pathcreator.vadim.quantum.application.simulation.options.SimulationOptions;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.validation.QuantumProgramValidator;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

/**
 * Проверяет цепочки внешний формат -> IR -> другой внешний формат -> IR.
 */
public final class CrossFormatVerificationRunner {

    private final QuantumProgramValidator validator;
    private final QuantumSimulator simulator;

    public CrossFormatVerificationRunner() {
        this(
            new QuantumProgramValidator(),
            new QuantumSimulator()
        );
    }

    public CrossFormatVerificationRunner(
        final QuantumProgramValidator validator,
        final QuantumSimulator simulator
    ) {
        if (validator == null) {
            throw new IllegalArgumentException("Cross-format validator must not be null.");
        }
        if (simulator == null) {
            throw new IllegalArgumentException("Cross-format simulator must not be null.");
        }
        this.validator = validator;
        this.simulator = simulator;
    }

    public CrossFormatVerificationReport verify(
        final String source,
        final QuantumIntegration inputIntegration,
        final QuantumIntegration[] targetIntegrations,
        final SimulationOptions simulationOptions
    ) {
        if (source == null) {
            throw new IllegalArgumentException("Cross-format source must not be null.");
        }
        if (inputIntegration == null) {
            throw new IllegalArgumentException("Cross-format input integration must not be null.");
        }
        if (targetIntegrations == null) {
            throw new IllegalArgumentException("Cross-format target integrations must not be null.");
        }
        if (simulationOptions == null) {
            throw new IllegalArgumentException("Cross-format simulation options must not be null.");
        }
        final ImportResult imported = inputIntegration.importProgram(
            source,
            ImportOptions.defaults()
        );
        if (!imported.isSuccess()) {
            return CrossFormatVerificationReport.of(
                inputIntegration.format(),
                false,
                imported.diagnosticCount(),
                null,
                null,
                java.util.List.of()
            );
        }
        final QuantumProgram program = imported.program();
        final ValidationResult validation = validator.validate(program);
        final SimulationResult simulation = simulator.simulate(
            program,
            simulationOptions
        );
        final ArrayList<CrossFormatTargetVerification> targets = new ArrayList<>(targetIntegrations.length);
        for (int i = 0; i < targetIntegrations.length; i++) {
            final QuantumIntegration targetIntegration = targetIntegrations[i];
            if (targetIntegration == null) {
                throw new IllegalArgumentException("Cross-format target integration must not be null.");
            }
            targets.add(verifyTarget(
                program,
                simulation.counts(),
                targetIntegration,
                simulationOptions
            ));
        }
        return CrossFormatVerificationReport.of(
            inputIntegration.format(),
            true,
            imported.diagnosticCount(),
            validation,
            simulation,
            targets
        );
    }

    private CrossFormatTargetVerification verifyTarget(
        final QuantumProgram program,
        final Map<String, Long> referenceCounts,
        final QuantumIntegration targetIntegration,
        final SimulationOptions simulationOptions
    ) {
        final ExportResult exported = targetIntegration.exportProgram(
            program,
            ExportOptions.defaults()
        );
        if (!exported.isSuccess()) {
            return CrossFormatTargetVerification.of(
                targetIntegration.format(),
                false,
                false,
                false,
                false,
                false,
                exported.diagnosticCount(),
                0,
                0
            );
        }
        final ImportResult reimported = targetIntegration.importProgram(
            exported.content(),
            ImportOptions.defaults()
        );
        if (!reimported.isSuccess()) {
            return CrossFormatTargetVerification.of(
                targetIntegration.format(),
                true,
                false,
                false,
                false,
                false,
                exported.diagnosticCount(),
                reimported.diagnosticCount(),
                0
            );
        }
        final ValidationResult validation = validator.validate(reimported.program());
        final SimulationResult simulation = simulator.simulate(
            reimported.program(),
            simulationOptions
        );
        return CrossFormatTargetVerification.of(
            targetIntegration.format(),
            true,
            true,
            validation.isValid(),
            simulation.isSuccess(),
            referenceCounts.equals(simulation.counts()),
            exported.diagnosticCount(),
            reimported.diagnosticCount(),
            validation.errorCount()
        );
    }
}
/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.regression;

import java.util.ArrayList;
import java.util.List;

import ru.pathcreator.vadim.quantum.application.compatibility.ProductCompatibilityMatrix;
import ru.pathcreator.vadim.quantum.application.compatibility.ProductCompatibilityMatrixRunner;
import ru.pathcreator.vadim.quantum.application.integration.contract.QuantumIntegration;
import ru.pathcreator.vadim.quantum.application.integration.options.ImportOptions;
import ru.pathcreator.vadim.quantum.application.integration.result.ImportResult;
import ru.pathcreator.vadim.quantum.application.simulation.options.SimulationOptions;
import ru.pathcreator.vadim.quantum.application.verification.CrossFormatVerificationReport;
import ru.pathcreator.vadim.quantum.application.verification.CrossFormatVerificationRunner;
import ru.pathcreator.vadim.quantum.application.workflow.ProductWorkflowOptions;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.validation.QuantumProgramValidator;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

/**
 * Запускает regression-corpus через import, validation, compatibility и cross-format verification.
 */
public final class CorpusRegressionRunner {

    private final QuantumProgramValidator validator;
    private final ProductCompatibilityMatrixRunner compatibilityRunner;
    private final CrossFormatVerificationRunner crossFormatRunner;

    public CorpusRegressionRunner() {
        this(
            new QuantumProgramValidator(),
            new ProductCompatibilityMatrixRunner(),
            new CrossFormatVerificationRunner()
        );
    }

    public CorpusRegressionRunner(
        final QuantumProgramValidator validator,
        final ProductCompatibilityMatrixRunner compatibilityRunner,
        final CrossFormatVerificationRunner crossFormatRunner
    ) {
        if (validator == null) {
            throw new IllegalArgumentException("Corpus regression validator must not be null.");
        }
        if (compatibilityRunner == null) {
            throw new IllegalArgumentException("Corpus regression compatibility runner must not be null.");
        }
        if (crossFormatRunner == null) {
            throw new IllegalArgumentException("Corpus regression cross-format runner must not be null.");
        }
        this.validator = validator;
        this.compatibilityRunner = compatibilityRunner;
        this.crossFormatRunner = crossFormatRunner;
    }

    public CorpusRegressionReport run(
        final List<CorpusRegressionCase> cases,
        final List<QuantumIntegration> targetIntegrations,
        final ProductWorkflowOptions workflowOptions
    ) {
        if (cases == null) {
            throw new IllegalArgumentException("Corpus regression cases must not be null.");
        }
        if (targetIntegrations == null) {
            throw new IllegalArgumentException("Corpus regression target integrations must not be null.");
        }
        if (workflowOptions == null) {
            throw new IllegalArgumentException("Corpus regression workflow options must not be null.");
        }
        final ArrayList<CorpusRegressionCaseReport> reports = new ArrayList<>(cases.size());
        final QuantumIntegration[] targetArray = targetArray(targetIntegrations);
        for (int i = 0; i < cases.size(); i++) {
            final CorpusRegressionCase regressionCase = cases.get(i);
            if (regressionCase == null) {
                throw new IllegalArgumentException("Corpus regression case must not be null.");
            }
            reports.add(runCase(
                regressionCase,
                targetIntegrations,
                targetArray,
                workflowOptions
            ));
        }
        return CorpusRegressionReport.of(reports);
    }

    private CorpusRegressionCaseReport runCase(
        final CorpusRegressionCase regressionCase,
        final List<QuantumIntegration> targetIntegrations,
        final QuantumIntegration[] targetArray,
        final ProductWorkflowOptions workflowOptions
    ) {
        final ImportResult imported = regressionCase.inputIntegration().importProgram(
            regressionCase.source(),
            ImportOptions.defaults()
        );
        if (!imported.isSuccess()) {
            return CorpusRegressionCaseReport.of(
                regressionCase.name(),
                regressionCase.inputIntegration().format(),
                false,
                imported.diagnosticCount(),
                null,
                null,
                null
            );
        }
        final QuantumProgram program = imported.program();
        final ValidationResult validation = validator.validate(program);
        final ProductCompatibilityMatrix compatibility = compatibilityRunner.run(
            program,
            targetIntegrations,
            workflowOptions
        );
        final SimulationOptions simulationOptions = workflowOptions.simulationOptions();
        final CrossFormatVerificationReport crossFormat = crossFormatRunner.verify(
            regressionCase.source(),
            regressionCase.inputIntegration(),
            targetArray,
            simulationOptions
        );
        return CorpusRegressionCaseReport.of(
            regressionCase.name(),
            regressionCase.inputIntegration().format(),
            true,
            imported.diagnosticCount(),
            validation,
            compatibility,
            crossFormat
        );
    }

    private static QuantumIntegration[] targetArray(final List<QuantumIntegration> targetIntegrations) {
        final QuantumIntegration[] targets = new QuantumIntegration[targetIntegrations.size()];
        for (int i = 0; i < targetIntegrations.size(); i++) {
            final QuantumIntegration integration = targetIntegrations.get(i);
            if (integration == null) {
                throw new IllegalArgumentException("Corpus regression target integration must not be null.");
            }
            targets[i] = integration;
        }
        return targets;
    }
}
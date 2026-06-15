/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.compatibility;

import java.util.ArrayList;
import java.util.List;

import ru.pathcreator.vadim.quantum.application.compiler.CompilerResult;
import ru.pathcreator.vadim.quantum.application.compiler.CompilerResultStatus;
import ru.pathcreator.vadim.quantum.application.compiler.QuantumCompiler;
import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightChecker;
import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightResult;
import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightStatus;
import ru.pathcreator.vadim.quantum.application.integration.contract.QuantumIntegration;
import ru.pathcreator.vadim.quantum.application.inspection.ProgramInspectionResult;
import ru.pathcreator.vadim.quantum.application.inspection.QuantumProgramInspector;
import ru.pathcreator.vadim.quantum.application.resource.ResourceEstimate;
import ru.pathcreator.vadim.quantum.application.resource.ResourceEstimator;
import ru.pathcreator.vadim.quantum.application.simulation.engine.QuantumSimulator;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.application.workflow.ProductWorkflowOptions;
import ru.pathcreator.vadim.quantum.application.workflow.ProductWorkflowReport;
import ru.pathcreator.vadim.quantum.application.workflow.ProductWorkflowRunner;
import ru.pathcreator.vadim.quantum.application.workflow.ProductWorkflowStatus;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.validation.QuantumProgramValidator;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

/**
 * Собирает compatibility matrix через реальные validation/preflight/compile/workflow pipeline.
 */
public final class ProductCompatibilityMatrixRunner {

    private final QuantumProgramValidator validator;
    private final QuantumProgramInspector inspector;
    private final ResourceEstimator resourceEstimator;
    private final QuantumSimulator simulator;
    private final CapabilityPreflightChecker preflightChecker;
    private final QuantumCompiler compiler;
    private final ProductWorkflowRunner workflowRunner;

    public ProductCompatibilityMatrixRunner() {
        this(
            new QuantumProgramValidator(),
            new QuantumProgramInspector(),
            new ResourceEstimator(),
            new QuantumSimulator(),
            new CapabilityPreflightChecker(),
            new QuantumCompiler(),
            new ProductWorkflowRunner()
        );
    }

    public ProductCompatibilityMatrixRunner(
        final QuantumProgramValidator validator,
        final QuantumProgramInspector inspector,
        final ResourceEstimator resourceEstimator,
        final QuantumSimulator simulator,
        final CapabilityPreflightChecker preflightChecker,
        final QuantumCompiler compiler,
        final ProductWorkflowRunner workflowRunner
    ) {
        if (validator == null) {
            throw new IllegalArgumentException("Compatibility validator must not be null.");
        }
        if (inspector == null) {
            throw new IllegalArgumentException("Compatibility inspector must not be null.");
        }
        if (resourceEstimator == null) {
            throw new IllegalArgumentException("Compatibility resource estimator must not be null.");
        }
        if (simulator == null) {
            throw new IllegalArgumentException("Compatibility simulator must not be null.");
        }
        if (preflightChecker == null) {
            throw new IllegalArgumentException("Compatibility preflight checker must not be null.");
        }
        if (compiler == null) {
            throw new IllegalArgumentException("Compatibility compiler must not be null.");
        }
        if (workflowRunner == null) {
            throw new IllegalArgumentException("Compatibility workflow runner must not be null.");
        }
        this.validator = validator;
        this.inspector = inspector;
        this.resourceEstimator = resourceEstimator;
        this.simulator = simulator;
        this.preflightChecker = preflightChecker;
        this.compiler = compiler;
        this.workflowRunner = workflowRunner;
    }

    public ProductCompatibilityMatrix run(
        final QuantumProgram program,
        final List<QuantumIntegration> integrations
    ) {
        return run(
            program,
            integrations,
            ProductWorkflowOptions.defaults()
        );
    }

    public ProductCompatibilityMatrix run(
        final QuantumProgram program,
        final List<QuantumIntegration> integrations,
        final ProductWorkflowOptions options
    ) {
        if (program == null) {
            throw new IllegalArgumentException("Compatibility matrix program must not be null.");
        }
        if (integrations == null) {
            throw new IllegalArgumentException("Compatibility matrix integrations must not be null.");
        }
        if (options == null) {
            throw new IllegalArgumentException("Compatibility matrix options must not be null.");
        }
        final long validationStarted = System.nanoTime();
        final ValidationResult validation = validator.validate(program);
        final CompatibilityCheckResult validationCheck = check(
            "validation",
            validation.isValid()
                ? CompatibilityCheckStatus.SUCCESS
                : CompatibilityCheckStatus.FAILED,
            validation.isValid()
                ? "Program validation passed."
                : "Program validation failed.",
            validation.errorCount(),
            validationStarted
        );
        final ArrayList<ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapabilityProfile> profiles =
            new ArrayList<>(integrations.size());
        for (int i = 0; i < integrations.size(); i++) {
            final QuantumIntegration integration = integrations.get(i);
            if (integration == null) {
                throw new IllegalArgumentException("Compatibility matrix integration must not be null.");
            }
            profiles.add(integration.capabilityProfile());
        }
        final ProgramInspectionResult inspection = inspector.inspect(
            program,
            profiles
        );
        final ResourceEstimate resources = resourceEstimator.estimate(
            program,
            options.resourceMaxQubits()
        );
        final SimulationResult simulation = simulator.simulate(
            program,
            options.simulationOptions()
        );
        final ArrayList<TargetCompatibilityReport> targets = new ArrayList<>(integrations.size());
        for (int i = 0; i < integrations.size(); i++) {
            final QuantumIntegration integration = integrations.get(i);
            targets.add(targetReport(
                program,
                integration,
                options,
                validation,
                validationCheck,
                simulation
            ));
        }
        return ProductCompatibilityMatrix.of(
            validation,
            inspection,
            resources,
            simulation,
            targets
        );
    }

    private TargetCompatibilityReport targetReport(
        final QuantumProgram program,
        final QuantumIntegration integration,
        final ProductWorkflowOptions options,
        final ValidationResult validation,
        final CompatibilityCheckResult validationCheck,
        final SimulationResult simulation
    ) {
        final ArrayList<CompatibilityCheckResult> checks = new ArrayList<>();
        checks.add(validationCheck);
        checks.add(check(
            "simulation",
            simulation.isSuccess()
                ? CompatibilityCheckStatus.SUCCESS
                : CompatibilityCheckStatus.WARNING,
            simulation.isSuccess()
                ? "Local simulation passed."
                : "Local simulation produced diagnostics.",
            simulation.diagnostics().size(),
            0L,
            0L
        ));
        if (!validation.isValid()) {
            checks.add(notRun(
                "preflight",
                "Preflight skipped because validation failed."
            ));
            checks.add(notRun(
                "compile",
                "Compile skipped because validation failed."
            ));
            checks.add(notRun(
                "workflow",
                "Workflow skipped because validation failed."
            ));
            return TargetCompatibilityReport.of(
                integration.format(),
                integration.capabilityProfile(),
                TargetCompatibilityStatus.INVALID_PROGRAM,
                null,
                null,
                null,
                checks
            );
        }
        final long preflightStarted = System.nanoTime();
        final CapabilityPreflightResult preflight = preflightChecker.check(
            program,
            integration.capabilityProfile()
        );
        checks.add(check(
            "preflight",
            preflightStatus(preflight),
            "Preflight status is " + preflight.status() + ".",
            preflight.diagnostics().size(),
            preflightStarted
        ));
        final long compileStarted = System.nanoTime();
        final CompilerResult compile = compiler.compile(
            program,
            integration
        );
        checks.add(check(
            "compile",
            compile.isSuccess()
                ? CompatibilityCheckStatus.SUCCESS
                : CompatibilityCheckStatus.FAILED,
            "Compiler status is " + compile.status() + ".",
            compile.hasExportResult()
                ? compile.exportResult().diagnosticCount()
                : 0,
            compileStarted
        ));
        final long workflowStarted = System.nanoTime();
        final ProductWorkflowReport workflow = workflowRunner.run(
            program,
            integration,
            options
        );
        checks.add(check(
            "workflow",
            workflow.isSuccess()
                ? CompatibilityCheckStatus.SUCCESS
                : CompatibilityCheckStatus.FAILED,
            "Workflow status is " + workflow.status() + ".",
            workflow.preflight() == null
                ? 0
                : workflow.preflight().diagnostics().size(),
            workflowStarted
        ));
        return TargetCompatibilityReport.of(
            integration.format(),
            integration.capabilityProfile(),
            targetStatus(
                preflight,
                compile,
                workflow
            ),
            preflight.status(),
            compile.status(),
            workflow.status(),
            checks
        );
    }

    private static TargetCompatibilityStatus targetStatus(
        final CapabilityPreflightResult preflight,
        final CompilerResult compile,
        final ProductWorkflowReport workflow
    ) {
        if (
            preflight.status() == CapabilityPreflightStatus.UNSUPPORTED_BY_TARGET
            || preflight.status() == CapabilityPreflightStatus.UNSUPPORTED_WITHOUT_LOSS
        ) {
            return TargetCompatibilityStatus.UNSUPPORTED;
        }
        if (preflight.status() == CapabilityPreflightStatus.LOWERING_REQUIRED) {
            return TargetCompatibilityStatus.NEEDS_LOWERING;
        }
        if (!workflow.isSuccess()) {
            return TargetCompatibilityStatus.WORKFLOW_FAILED;
        }
        return compile.isSuccess()
            ? TargetCompatibilityStatus.EXPORTABLE
            : TargetCompatibilityStatus.WORKFLOW_FAILED;
    }

    private static CompatibilityCheckStatus preflightStatus(final CapabilityPreflightResult preflight) {
        if (
            preflight.status() == CapabilityPreflightStatus.UNSUPPORTED_BY_TARGET
            || preflight.status() == CapabilityPreflightStatus.UNSUPPORTED_WITHOUT_LOSS
        ) {
            return CompatibilityCheckStatus.FAILED;
        }
        if (preflight.status() == CapabilityPreflightStatus.LOWERING_REQUIRED) {
            return CompatibilityCheckStatus.WARNING;
        }
        return CompatibilityCheckStatus.SUCCESS;
    }

    private static CompatibilityCheckResult notRun(
        final String name,
        final String message
    ) {
        return CompatibilityCheckResult.of(
            name,
            CompatibilityCheckStatus.NOT_RUN,
            message,
            0,
            0L
        );
    }

    private static CompatibilityCheckResult check(
        final String name,
        final CompatibilityCheckStatus status,
        final String message,
        final int diagnosticCount,
        final long started
    ) {
        return check(
            name,
            status,
            message,
            diagnosticCount,
            started,
            System.nanoTime()
        );
    }

    private static CompatibilityCheckResult check(
        final String name,
        final CompatibilityCheckStatus status,
        final String message,
        final int diagnosticCount,
        final long started,
        final long finished
    ) {
        return CompatibilityCheckResult.of(
            name,
            status,
            message,
            diagnosticCount,
            started == 0L
                ? 0L
                : finished - started
        );
    }
}
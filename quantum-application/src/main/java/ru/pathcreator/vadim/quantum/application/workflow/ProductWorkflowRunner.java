/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.workflow;

import ru.pathcreator.vadim.quantum.application.backend.BackendJobOptions;
import ru.pathcreator.vadim.quantum.application.backend.BackendExecutionResult;
import ru.pathcreator.vadim.quantum.application.backend.BackendSubmissionResult;
import ru.pathcreator.vadim.quantum.application.backend.DryRunQuantumBackend;
import ru.pathcreator.vadim.quantum.application.backend.QuantumBackend;
import ru.pathcreator.vadim.quantum.application.compiler.CompilerResult;
import ru.pathcreator.vadim.quantum.application.compiler.QuantumCompiler;
import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightChecker;
import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightResult;
import ru.pathcreator.vadim.quantum.application.integration.contract.QuantumIntegration;
import ru.pathcreator.vadim.quantum.application.inspection.ProgramInspectionResult;
import ru.pathcreator.vadim.quantum.application.inspection.QuantumProgramInspector;
import ru.pathcreator.vadim.quantum.application.resource.ResourceEstimate;
import ru.pathcreator.vadim.quantum.application.resource.ResourceEstimator;
import ru.pathcreator.vadim.quantum.application.simulation.engine.QuantumSimulator;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.application.visualization.ProgramTimeline;
import ru.pathcreator.vadim.quantum.application.visualization.QuantumProgramTimelineBuilder;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.validation.QuantumProgramValidator;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

public final class ProductWorkflowRunner {

    public ProductWorkflowReport run(
        final QuantumProgram program,
        final QuantumIntegration integration
    ) {
        return run(
            program,
            integration,
            ProductWorkflowOptions.defaults()
        );
    }

    public ProductWorkflowReport run(
        final QuantumProgram program,
        final QuantumIntegration integration,
        final ProductWorkflowOptions options
    ) {
        if (program == null) {
            throw new IllegalArgumentException("Product workflow program must not be null.");
        }
        if (integration == null) {
            throw new IllegalArgumentException("Product workflow integration must not be null.");
        }
        if (options == null) {
            throw new IllegalArgumentException("Product workflow options must not be null.");
        }
        final ProductWorkflowReport.Builder report = ProductWorkflowReport.builder(integration.format())
            .targetProfile(integration.capabilityProfile());
        final ValidationResult validation = options.runValidation()
            ? new QuantumProgramValidator().validate(program)
            : null;
        final ProgramInspectionResult inspection = options.runInspection()
            ? new QuantumProgramInspector().inspect(
                program,
                java.util.List.of(integration.capabilityProfile())
            )
            : null;
        final ResourceEstimate resources = options.runResources()
            ? new ResourceEstimator().estimate(
                program,
                options.resourceMaxQubits()
            )
            : null;
        final ProgramTimeline timeline = options.runTimeline()
            ? new QuantumProgramTimelineBuilder().build(program)
            : null;
        report.validation(validation)
            .inspection(inspection)
            .resources(resources)
            .timeline(timeline);
        if (validation != null && !validation.isValid()) {
            return report.status(ProductWorkflowStatus.VALIDATION_FAILED)
                .build();
        }
        final CapabilityPreflightResult preflight = options.runPreflight()
            ? new CapabilityPreflightChecker().check(
                program,
                integration.capabilityProfile()
            )
            : null;
        final SimulationResult simulation = options.runSimulation()
            ? new QuantumSimulator().simulate(
                program,
                options.simulationOptions()
            )
            : null;
        final CompilerResult compiler = options.runCompiler()
            ? new QuantumCompiler().compile(
                program,
                integration,
                options.compilerOptions()
            )
            : null;
        report.preflight(preflight)
            .simulation(simulation)
            .compiler(compiler);
        ProductWorkflowStatus status = ProductWorkflowStatus.COMPLETED;
        if (preflight != null && !preflight.isSuccess()) {
            status = ProductWorkflowStatus.PREFLIGHT_FAILED;
        } else if (simulation != null && !simulation.isSuccess()) {
            status = ProductWorkflowStatus.SIMULATION_FAILED;
        } else if (compiler != null && !compiler.isSuccess()) {
            status = ProductWorkflowStatus.COMPILE_FAILED;
        }
        if (options.runBackendDryRun()) {
            final QuantumBackend backend = new DryRunQuantumBackend(
                "dry-run-" + integration.format().name().toLowerCase(),
                "Dry Run " + integration.format().name(),
                "1",
                integration
            );
            final BackendSubmissionResult submission = backend.submit(
                program,
                BackendJobOptions.builder()
                    .simulationOptions(options.simulationOptions())
                    .metadata(
                        "origin",
                        "product-workflow"
                    )
                    .build()
            );
            report.backendSubmission(submission);
            BackendExecutionResult backendExecution = null;
            if (submission.isAccepted()) {
                backendExecution = backend.result(submission.jobId());
                report.backendExecution(backendExecution);
            }
            if (
                status == ProductWorkflowStatus.COMPLETED
                && (
                    !submission.isAccepted()
                    || backendExecution == null
                    || !backendExecution.isSuccess()
                )
            ) {
                status = ProductWorkflowStatus.BACKEND_FAILED;
            }
        }
        return report.status(status)
            .build();
    }
}
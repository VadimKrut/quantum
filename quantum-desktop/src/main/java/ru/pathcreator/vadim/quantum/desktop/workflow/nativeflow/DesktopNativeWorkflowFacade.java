/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.workflow.nativeflow;

import static ru.pathcreator.vadim.quantum.desktop.workflow.payload.DesktopWorkflowPayloads.compatibilityPayload;
import static ru.pathcreator.vadim.quantum.desktop.workflow.payload.DesktopWorkflowPayloads.compilerPayload;
import static ru.pathcreator.vadim.quantum.desktop.workflow.payload.DesktopWorkflowPayloads.inspectionPayload;
import static ru.pathcreator.vadim.quantum.desktop.workflow.payload.DesktopWorkflowPayloads.preflightPayload;
import static ru.pathcreator.vadim.quantum.desktop.workflow.payload.DesktopWorkflowPayloads.resourcePayload;
import static ru.pathcreator.vadim.quantum.desktop.workflow.payload.DesktopWorkflowPayloads.transformationPayload;
import static ru.pathcreator.vadim.quantum.desktop.workflow.payload.DesktopWorkflowPayloads.validationPayload;
import static ru.pathcreator.vadim.quantum.desktop.workflow.payload.DesktopWorkflowPayloads.workflowPayload;

import java.util.List;
import java.util.function.Function;

import ru.pathcreator.vadim.quantum.application.compiler.CompilerResult;
import ru.pathcreator.vadim.quantum.application.compatibility.ProductCompatibilityMatrix;
import ru.pathcreator.vadim.quantum.application.inspection.ProgramInspectionResult;
import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightResult;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrWriteResult;
import ru.pathcreator.vadim.quantum.application.resource.ResourceEstimate;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.application.transformation.TransformationResult;
import ru.pathcreator.vadim.quantum.application.visualization.ProgramTimeline;
import ru.pathcreator.vadim.quantum.application.workflow.ProductWorkflowReport;
import ru.pathcreator.vadim.quantum.desktop.ui.render.DesktopSimulationTextRenderer;
import ru.pathcreator.vadim.quantum.desktop.ui.render.DesktopTimelineRenderer;
import ru.pathcreator.vadim.quantum.desktop.workflow.DesktopAction;
import ru.pathcreator.vadim.quantum.desktop.workflow.DesktopExecutionOptions;
import ru.pathcreator.vadim.quantum.desktop.workflow.DesktopWorkflowResult;
import ru.pathcreator.vadim.quantum.desktop.workspace.DesktopIrOperationSpec;
import ru.pathcreator.vadim.quantum.desktop.workspace.DesktopIrWorkspaceService;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

/**
 * Координирует desktop workflow actions для native IR без JavaFX-зависимостей.
 */
public final class DesktopNativeWorkflowFacade {

    private final DesktopIrWorkspaceService workspaceService;
    private final DesktopSimulationTextRenderer simulationTextRenderer;
    private final DesktopTimelineRenderer timelineRenderer;
    private final Function<Object, String> renderer;

    public DesktopNativeWorkflowFacade(
        final DesktopIrWorkspaceService workspaceService,
        final DesktopSimulationTextRenderer simulationTextRenderer,
        final DesktopTimelineRenderer timelineRenderer,
        final Function<Object, String> renderer
    ) {
        this.workspaceService = workspaceService;
        this.simulationTextRenderer = simulationTextRenderer;
        this.timelineRenderer = timelineRenderer;
        this.renderer = renderer;
    }

    public DesktopWorkflowResult validate(final QuantumProgram program) {
        final ValidationResult validation = workspaceService.validate(program);
        return DesktopWorkflowResult.of(
            DesktopAction.VALIDATE,
            validation.isValid(),
            validation.isValid() ? "VALID" : "INVALID",
            "Errors: " + validation.errorCount(),
            renderer.apply(validationPayload(validation))
        );
    }

    public DesktopWorkflowResult inspect(
        final QuantumProgram program,
        final IntegrationFormat targetFormat
    ) {
        final ProgramInspectionResult inspection = workspaceService.inspect(
            program,
            targetFormat
        );
        return DesktopWorkflowResult.of(
            DesktopAction.INSPECT,
            true,
            "INSPECTED",
            "Circuits: " + inspection.circuitSummaries().size(),
            renderer.apply(inspectionPayload(inspection))
        );
    }

    public DesktopWorkflowResult resources(final QuantumProgram program) {
        final ResourceEstimate resources = workspaceService.resources(
            program,
            24
        );
        return DesktopWorkflowResult.of(
            DesktopAction.RESOURCES,
            true,
            "RESOURCES_ESTIMATED",
            "Qubits: " + resources.qubitCount() + ", operations: " + resources.operationCount(),
            renderer.apply(resourcePayload(resources))
        );
    }

    public DesktopWorkflowResult timeline(final QuantumProgram program) {
        final ProgramTimeline timeline = workspaceService.timeline(program);
        return DesktopWorkflowResult.of(
            DesktopAction.CIRCUIT,
            true,
            "TIMELINE_BUILT",
            "Circuits: " + timeline.circuits().size(),
            renderer.apply(timeline),
            timelineRenderer.renderSummary(timeline)
        );
    }

    public DesktopWorkflowResult json(final QuantumProgram program) {
        final QuantumIrWriteResult write = workspaceService.writeJson(program);
        return DesktopWorkflowResult.of(
            DesktopAction.JSON,
            write.isSuccess(),
            write.isSuccess() ? "JSON_WRITTEN" : "JSON_FAILED",
            "Diagnostics: " + write.diagnostics().size(),
            write.hasContent() ? write.content() : renderer.apply(write)
        );
    }

    public DesktopWorkflowResult simulate(
        final QuantumProgram program,
        final int shots,
        final long seed,
        final boolean hideZeroProbability
    ) {
        final SimulationResult simulation = workspaceService.simulate(
            program,
            shots,
            seed
        );
        return DesktopWorkflowResult.of(
            DesktopAction.SIMULATE,
            simulation.isSuccess(),
            simulation.isSuccess() ? "SIMULATED" : "SIMULATION_FAILED",
            "Qubits: " + simulation.qubitCount() + ", shots: " + simulation.shots(),
            simulationTextRenderer.render(
                simulation,
                hideZeroProbability
            )
        );
    }

    public DesktopWorkflowResult export(
        final QuantumProgram program,
        final IntegrationFormat targetFormat,
        final DesktopExecutionOptions executionOptions
    ) {
        final CompilerResult result = workspaceService.export(
            program,
            targetFormat,
            executionOptions
        );
        final String generated = result.hasExportResult()
            && result.exportResult().hasContent()
                ? result.exportResult().content()
                : "";
        return DesktopWorkflowResult.of(
            DesktopAction.COMPILE,
            result.isSuccess(),
            result.status().name(),
            "Stages: " + result.stageRecords().size(),
            renderer.apply(compilerPayload(
                result,
                generated
            )),
            generated
        );
    }

    public DesktopWorkflowResult preflight(
        final QuantumProgram program,
        final IntegrationFormat targetFormat
    ) {
        final CapabilityPreflightResult result = workspaceService.preflight(
            program,
            targetFormat
        );
        return DesktopWorkflowResult.of(
            DesktopAction.PREFLIGHT,
            result.isSuccess(),
            result.status().name(),
            "Diagnostics: " + result.diagnostics().size(),
            renderer.apply(preflightPayload(result))
        );
    }

    public DesktopWorkflowResult compatibility(
        final QuantumProgram program,
        final int shots,
        final long seed,
        final DesktopExecutionOptions executionOptions
    ) {
        final ProductCompatibilityMatrix result = workspaceService.compatibility(
            program,
            shots,
            seed,
            executionOptions
        );
        return DesktopWorkflowResult.of(
            DesktopAction.COMPATIBILITY,
            result.isSuccess(),
            result.isSuccess() ? "COMPATIBLE" : "INCOMPATIBLE",
            "Targets: " + result.targets().size(),
            renderer.apply(compatibilityPayload(result))
        );
    }

    public DesktopWorkflowResult transform(
        final QuantumProgram program,
        final IntegrationFormat targetFormat,
        final boolean canonicalizeParameters,
        final boolean removeIdentity,
        final boolean inlineComposite,
        final boolean targetLowering
    ) {
        final TransformationResult result = workspaceService.transform(
            program,
            targetFormat,
            canonicalizeParameters,
            removeIdentity,
            inlineComposite,
            targetLowering
        );
        return DesktopWorkflowResult.of(
            DesktopAction.COMPILE,
            result.isSuccess(),
            result.isSuccess() ? "TRANSFORMED" : "TRANSFORM_FAILED",
            "Applied: " + result.appliedSteps().size() + ", skipped: " + result.skippedSteps().size(),
            renderer.apply(transformationPayload(result))
        );
    }

    public DesktopWorkflowResult javaDsl(
        final String circuitName,
        final String quantumRegisterName,
        final int quantumRegisterSize,
        final String classicalRegisterName,
        final int classicalRegisterSize,
        final List<DesktopIrOperationSpec> operations
    ) {
        final String code = workspaceService.generateJavaDsl(
            circuitName,
            quantumRegisterName,
            quantumRegisterSize,
            classicalRegisterName,
            classicalRegisterSize,
            operations
        );
        return DesktopWorkflowResult.of(
            DesktopAction.CIRCUIT,
            true,
            "JAVA_DSL_GENERATED",
            "Operations: " + operations.size(),
            code
        );
    }

    public DesktopWorkflowResult workflow(
        final QuantumProgram program,
        final IntegrationFormat targetFormat,
        final int shots,
        final long seed,
        final DesktopExecutionOptions executionOptions
    ) {
        final ProductWorkflowReport result = workspaceService.workflow(
            program,
            targetFormat,
            shots,
            seed,
            executionOptions
        );
        return DesktopWorkflowResult.of(
            DesktopAction.WORKFLOW,
            result.isSuccess(),
            result.status().name(),
            "Validation: " + (result.validation() != null)
                + ", compile: " + (result.compiler() != null)
                + ", backend: " + result.hasBackendExecution(),
            renderer.apply(workflowPayload(result))
        );
    }
}
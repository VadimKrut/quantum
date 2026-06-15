/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.workspace;

import java.util.List;

import ru.pathcreator.vadim.quantum.api.Quantum;
import ru.pathcreator.vadim.quantum.api.QuantumCircuitBuilder;
import ru.pathcreator.vadim.quantum.api.QuantumProgramBuilder;
import ru.pathcreator.vadim.quantum.application.compiler.CompilerResult;
import ru.pathcreator.vadim.quantum.application.compatibility.ProductCompatibilityMatrix;
import ru.pathcreator.vadim.quantum.application.inspection.ProgramInspectionResult;
import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightResult;
import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapabilityProfile;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrReadResult;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrWriteResult;
import ru.pathcreator.vadim.quantum.application.resource.ResourceEstimate;
import ru.pathcreator.vadim.quantum.application.simulation.options.SimulationOptions;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.application.transformation.TransformationOptions;
import ru.pathcreator.vadim.quantum.application.transformation.TransformationResult;
import ru.pathcreator.vadim.quantum.application.visualization.ProgramTimeline;
import ru.pathcreator.vadim.quantum.application.workflow.ProductWorkflowReport;
import ru.pathcreator.vadim.quantum.desktop.workflow.DesktopExecutionOptions;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

/**
 * Application-service native IR workspace: build, inspect, simulate and export without external source format.
 */
public final class DesktopIrWorkspaceService {

    public QuantumProgram buildProgram(
        final String circuitName,
        final String quantumRegisterName,
        final int quantumRegisterSize,
        final String classicalRegisterName,
        final int classicalRegisterSize,
        final List<DesktopIrOperationSpec> operations
    ) {
        if (operations == null) {
            throw new IllegalArgumentException("Desktop IR operations must not be null.");
        }
        final QuantumProgramBuilder program = Quantum.programBuilder();
        final QuantumCircuitBuilder circuit = program.circuit(circuitName)
            .qreg(
                quantumRegisterName,
                quantumRegisterSize
            )
            .creg(
                classicalRegisterName,
                classicalRegisterSize
            );
        for (int i = 0; i < operations.size(); i++) {
            apply(
                circuit,
                operations.get(i)
            );
        }
        return circuit.build();
    }

    public ValidationResult validate(final QuantumProgram program) {
        return Quantum.validate(program);
    }

    public IntegrationCapabilityProfile targetProfile(final IntegrationFormat targetFormat) {
        return Quantum.targetProfile(targetFormat);
    }

    public ProgramInspectionResult inspect(
        final QuantumProgram program,
        final IntegrationFormat targetFormat
    ) {
        return Quantum.inspect(
            program,
            List.of(Quantum.targetProfile(targetFormat))
        );
    }

    public ResourceEstimate resources(
        final QuantumProgram program,
        final int maxLocalSimulationQubits
    ) {
        return Quantum.estimateResources(
            program,
            maxLocalSimulationQubits
        );
    }

    public ProgramTimeline timeline(final QuantumProgram program) {
        return Quantum.timeline(program);
    }

    public CapabilityPreflightResult preflight(
        final QuantumProgram program,
        final IntegrationFormat targetFormat
    ) {
        return Quantum.preflight(
            targetFormat,
            program
        );
    }

    public ProductCompatibilityMatrix compatibility(
        final QuantumProgram program,
        final int shots,
        final long seed,
        final DesktopExecutionOptions options
    ) {
        return Quantum.compatibilityMatrix(
            program,
            options.workflowOptions(
                shots,
                seed
            )
        );
    }

    public TransformationResult transform(
        final QuantumProgram program,
        final IntegrationFormat targetFormat,
        final boolean canonicalizeParameterExpressions,
        final boolean removeIdentityGates,
        final boolean inlineCompositeGates,
        final boolean targetAwareLowering
    ) {
        final TransformationOptions.Builder builder = TransformationOptions.builder();
        if (canonicalizeParameterExpressions) {
            builder.canonicalizeParameterExpressions();
        }
        if (removeIdentityGates) {
            builder.removeIdentityGates();
        }
        if (inlineCompositeGates) {
            builder.inlineCompositeGates();
        }
        if (targetAwareLowering) {
            builder.targetAwareLowering(Quantum.targetProfile(targetFormat));
        }
        return Quantum.transform(
            program,
            builder.build()
        );
    }

    public SimulationResult simulate(
        final QuantumProgram program,
        final int shots,
        final long seed
    ) {
        return Quantum.simulate(
            program,
            SimulationOptions.builder()
                .shots(shots)
                .seed(seed)
                .build()
        );
    }

    public QuantumIrWriteResult writeJson(final QuantumProgram program) {
        return Quantum.writeJson(program);
    }

    public QuantumIrReadResult readJson(final String content) {
        return Quantum.readJson(content);
    }

    public CompilerResult export(
        final QuantumProgram program,
        final IntegrationFormat targetFormat,
        final DesktopExecutionOptions options
    ) {
        return Quantum.compile(
            targetFormat,
            program,
            options.compilerOptions()
        );
    }

    public ProductWorkflowReport workflow(
        final QuantumProgram program,
        final IntegrationFormat targetFormat,
        final int shots,
        final long seed,
        final DesktopExecutionOptions options
    ) {
        return Quantum.runProductWorkflow(
            targetFormat,
            program,
            options.workflowOptions(
                shots,
                seed
            )
        );
    }

    public String generateJavaDsl(
        final String circuitName,
        final String quantumRegisterName,
        final int quantumRegisterSize,
        final String classicalRegisterName,
        final int classicalRegisterSize,
        final List<DesktopIrOperationSpec> operations
    ) {
        final StringBuilder code = new StringBuilder();
        code.append("final QuantumProgram program = Quantum.programBuilder()").append(System.lineSeparator());
        code.append("    .circuit(\"").append(circuitName).append("\")").append(System.lineSeparator());
        code.append("    .qreg(\"").append(quantumRegisterName).append("\", ").append(quantumRegisterSize).append(")").append(System.lineSeparator());
        code.append("    .creg(\"").append(classicalRegisterName).append("\", ").append(classicalRegisterSize).append(")").append(System.lineSeparator());
        for (int i = 0; i < operations.size(); i++) {
            appendJavaOperation(
                code,
                operations.get(i)
            );
        }
        code.append("    .build();");
        return code.toString();
    }

    private static void appendJavaOperation(
        final StringBuilder code,
        final DesktopIrOperationSpec operation
    ) {
        switch (operation.gate()) {
            case "RX", "RY", "RZ", "PHASE" -> code.append("    .")
                .append(javaMethodName(operation.gate()))
                .append("(")
                .append(operation.angle())
                .append(", \"")
                .append(operation.primaryQubit())
                .append("\")")
                .append(System.lineSeparator());
            case "CX", "CY", "CZ", "CH", "SWAP" -> code.append("    .")
                .append(javaMethodName(operation.gate()))
                .append("(\"")
                .append(operation.primaryQubit())
                .append("\", \"")
                .append(operation.secondaryQubit())
                .append("\")")
                .append(System.lineSeparator());
            case "CCX" -> code.append("    .ccx(\"")
                .append(operation.primaryQubit())
                .append("\", \"")
                .append(operation.secondaryQubit())
                .append("\", \"")
                .append(operation.tertiaryQubit())
                .append("\")")
                .append(System.lineSeparator());
            case "MEASURE" -> code.append("    .measure(\"")
                .append(operation.primaryQubit())
                .append("\", \"")
                .append(operation.classicalBit())
                .append("\")")
                .append(System.lineSeparator());
            case "BARRIER" -> code.append("    .barrier(\"")
                .append(operation.primaryQubit())
                .append("\", \"")
                .append(operation.secondaryQubit())
                .append("\")")
                .append(System.lineSeparator());
            default -> code.append("    .")
                .append(javaMethodName(operation.gate()))
                .append("(\"")
                .append(operation.primaryQubit())
                .append("\")")
                .append(System.lineSeparator());
        }
    }

    private static String javaMethodName(final String gate) {
        return switch (gate) {
            case "PHASE" -> "phase";
            default -> gate.toLowerCase();
        };
    }

    private static void apply(
        final QuantumCircuitBuilder circuit,
        final DesktopIrOperationSpec operation
    ) {
        switch (operation.gate()) {
            case "H" -> circuit.h(operation.primaryQubit());
            case "X" -> circuit.x(operation.primaryQubit());
            case "Y" -> circuit.y(operation.primaryQubit());
            case "Z" -> circuit.z(operation.primaryQubit());
            case "S" -> circuit.s(operation.primaryQubit());
            case "T" -> circuit.t(operation.primaryQubit());
            case "RX" -> circuit.rx(
                operation.angle(),
                operation.primaryQubit()
            );
            case "RY" -> circuit.ry(
                operation.angle(),
                operation.primaryQubit()
            );
            case "RZ" -> circuit.rz(
                operation.angle(),
                operation.primaryQubit()
            );
            case "PHASE" -> circuit.phase(
                operation.angle(),
                operation.primaryQubit()
            );
            case "CX" -> circuit.cx(
                operation.primaryQubit(),
                operation.secondaryQubit()
            );
            case "CY" -> circuit.cy(
                operation.primaryQubit(),
                operation.secondaryQubit()
            );
            case "CZ" -> circuit.cz(
                operation.primaryQubit(),
                operation.secondaryQubit()
            );
            case "CH" -> circuit.ch(
                operation.primaryQubit(),
                operation.secondaryQubit()
            );
            case "SWAP" -> circuit.swap(
                operation.primaryQubit(),
                operation.secondaryQubit()
            );
            case "CCX" -> circuit.ccx(
                operation.primaryQubit(),
                operation.secondaryQubit(),
                operation.tertiaryQubit()
            );
            case "MEASURE" -> circuit.measure(
                operation.primaryQubit(),
                operation.classicalBit()
            );
            case "RESET" -> circuit.reset(operation.primaryQubit());
            case "BARRIER" -> circuit.barrier(
                operation.primaryQubit(),
                operation.secondaryQubit()
            );
            default -> throw new IllegalArgumentException("Unsupported desktop IR operation: " + operation.gate() + ".");
        }
    }
}

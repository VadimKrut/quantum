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
import java.util.Locale;

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
import ru.pathcreator.vadim.quantum.desktop.workspace.operation.DesktopCustomOperationRegistry;
import ru.pathcreator.vadim.quantum.domain.bit.ClassicalBit;
import ru.pathcreator.vadim.quantum.domain.bit.Qubit;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.BarrierOperation;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.MeasureOperation;
import ru.pathcreator.vadim.quantum.domain.operation.Operation;
import ru.pathcreator.vadim.quantum.domain.operation.ResetOperation;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

/**
 * Сервис прикладного уровня для native IR workspace: сборка, инспекция, симуляция и экспорт без внешнего исходного формата.
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
        return buildProgram(
            circuitName,
            quantumRegisterName,
            quantumRegisterSize,
            classicalRegisterName,
            classicalRegisterSize,
            operations,
            null
        );
    }

    public QuantumProgram buildProgram(
        final String circuitName,
        final String quantumRegisterName,
        final int quantumRegisterSize,
        final String classicalRegisterName,
        final int classicalRegisterSize,
        final List<DesktopIrOperationSpec> operations,
        final DesktopCustomOperationRegistry customOperations
    ) {
        if (operations == null) {
            throw new IllegalArgumentException("Desktop IR operations must not be null.");
        }
        final List<DesktopIrOperationSpec> effectiveOperations = customOperations == null
            ? operations
            : customOperations.expand(operations);
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
        for (int i = 0; i < effectiveOperations.size(); i++) {
            apply(
                circuit,
                effectiveOperations.get(i)
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

    public DesktopIrProgramSnapshot projectToGraphicalWorkspace(final QuantumProgram program) {
        if (program == null) {
            throw new IllegalArgumentException("Projected program must not be null.");
        }
        if (program.circuitCount() == 0) {
            return new DesktopIrProgramSnapshot(
                "main",
                "q",
                1,
                "c",
                1,
                List.of(),
                List.of("Program does not contain circuits.")
            );
        }
        final QuantumCircuit circuit = program.circuit(0);
        final java.util.ArrayList<DesktopIrOperationSpec> projectedOperations = new java.util.ArrayList<>();
        final java.util.ArrayList<String> diagnostics = new java.util.ArrayList<>();
        if (circuit.quantumRegisterCount() == 0) {
            diagnostics.add("Circuit does not contain quantum registers; graphical editor uses q[1] placeholder.");
            return new DesktopIrProgramSnapshot(
                circuit.name().value(),
                "q",
                1,
                "c",
                1,
                projectedOperations,
                diagnostics
            );
        }
        final QuantumRegister quantumRegister = firstQuantumRegister(circuit);
        final String classicalRegisterName;
        final int classicalRegisterSize;
        if (circuit.classicalRegisterCount() == 0) {
            diagnostics.add("Circuit does not contain classical registers; graphical editor uses c[1] placeholder.");
            classicalRegisterName = "c";
            classicalRegisterSize = 1;
        } else {
            final ClassicalRegister classicalRegister = firstClassicalRegister(circuit);
            classicalRegisterName = classicalRegister.name().value();
            classicalRegisterSize = classicalRegister.size();
        }
        if (program.circuitCount() > 1) {
            diagnostics.add("Only the first circuit is projected into the graphical editor.");
        }
        if (circuit.quantumRegisterCount() > 1) {
            diagnostics.add("Only the first quantum register is projected into the graphical editor.");
        }
        if (circuit.classicalRegisterCount() > 1) {
            diagnostics.add("Only the first classical register is projected into the graphical editor.");
        }
        for (int i = 0; i < circuit.operationCount(); i++) {
            projectOperation(
                circuit.operation(i),
                i,
                projectedOperations,
                diagnostics
            );
        }
        return new DesktopIrProgramSnapshot(
            circuit.name().value(),
            quantumRegister.name().value(),
            quantumRegister.size(),
            classicalRegisterName,
            classicalRegisterSize,
            projectedOperations,
            diagnostics
        );
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
        return generateJavaDsl(
            circuitName,
            quantumRegisterName,
            quantumRegisterSize,
            classicalRegisterName,
            classicalRegisterSize,
            operations,
            null
        );
    }

    public String generateJavaDsl(
        final String circuitName,
        final String quantumRegisterName,
        final int quantumRegisterSize,
        final String classicalRegisterName,
        final int classicalRegisterSize,
        final List<DesktopIrOperationSpec> operations,
        final DesktopCustomOperationRegistry customOperations
    ) {
        final List<DesktopIrOperationSpec> effectiveOperations = customOperations == null
            ? operations
            : customOperations.expand(operations);
        final StringBuilder code = new StringBuilder();
        code.append("final QuantumProgram program = Quantum.programBuilder()").append(System.lineSeparator());
        code.append("    .circuit(\"").append(circuitName).append("\")").append(System.lineSeparator());
        code.append("    .qreg(\"").append(quantumRegisterName).append("\", ").append(quantumRegisterSize).append(")").append(System.lineSeparator());
        code.append("    .creg(\"").append(classicalRegisterName).append("\", ").append(classicalRegisterSize).append(")").append(System.lineSeparator());
        for (int i = 0; i < effectiveOperations.size(); i++) {
            appendJavaOperation(
                code,
                effectiveOperations.get(i)
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

    private static QuantumRegister firstQuantumRegister(final QuantumCircuit circuit) {
        if (circuit.quantumRegisterCount() == 0) {
            throw new IllegalArgumentException("Projected circuit must contain at least one quantum register.");
        }
        return circuit.quantumRegister(0);
    }

    private static ClassicalRegister firstClassicalRegister(final QuantumCircuit circuit) {
        if (circuit.classicalRegisterCount() == 0) {
            throw new IllegalArgumentException("Projected circuit must contain at least one classical register.");
        }
        return circuit.classicalRegister(0);
    }

    private static void projectOperation(
        final Operation operation,
        final int index,
        final java.util.ArrayList<DesktopIrOperationSpec> projectedOperations,
        final java.util.ArrayList<String> diagnostics
    ) {
        switch (operation.kind()) {
            case GATE -> projectGateOperation(
                (GateOperation) operation,
                index,
                projectedOperations,
                diagnostics
            );
            case MEASURE -> projectMeasureOperation(
                (MeasureOperation) operation,
                projectedOperations
            );
            case RESET -> projectedOperations.add(new DesktopIrOperationSpec(
                "RESET",
                qubitReference(((ResetOperation) operation).qubit()),
                "q[0]",
                "q[0]",
                "c[0]",
                Math.PI / 2.0
            ));
            case BARRIER -> projectBarrierOperation(
                (BarrierOperation) operation,
                projectedOperations,
                diagnostics
            );
            default -> diagnostics.add("Operation #" + index + " is not editable in the graphical gate grid: " + operation.kind() + ".");
        }
    }

    private static void projectGateOperation(
        final GateOperation operation,
        final int index,
        final java.util.ArrayList<DesktopIrOperationSpec> projectedOperations,
        final java.util.ArrayList<String> diagnostics
    ) {
        final String gate = operation.gate().gateName().toUpperCase(Locale.ROOT);
        final String desktopGate = switch (gate) {
            case "H", "X", "Y", "Z", "S", "T", "RX", "RY", "RZ", "CX", "CY", "CZ", "CH", "SWAP", "CCX" -> gate;
            case "PHASE" -> "PHASE";
            default -> null;
        };
        if (desktopGate == null) {
            diagnostics.add("Gate operation #" + index + " is not editable in the graphical gate grid: " + operation.gate().gateName() + ".");
            return;
        }
        if (operation.qubitCount() != operation.gate().arity()) {
            diagnostics.add("Gate operation #" + index + " has dynamic or non-canonical qubit references.");
            return;
        }
        final double angle = operation.parameterCount() == 0
            ? Math.PI / 2.0
            : numericAngle(
                operation.parameter(0),
                index,
                diagnostics
            );
        projectedOperations.add(new DesktopIrOperationSpec(
            desktopGate,
            qubitReference(operation.qubit(0)),
            operation.qubitCount() > 1 ? qubitReference(operation.qubit(1)) : "q[0]",
            operation.qubitCount() > 2 ? qubitReference(operation.qubit(2)) : "q[0]",
            "c[0]",
            angle
        ));
    }

    private static void projectMeasureOperation(
        final MeasureOperation operation,
        final java.util.ArrayList<DesktopIrOperationSpec> projectedOperations
    ) {
        projectedOperations.add(new DesktopIrOperationSpec(
            "MEASURE",
            qubitReference(operation.qubit()),
            "q[0]",
            "q[0]",
            bitReference(operation.bit()),
            Math.PI / 2.0
        ));
    }

    private static void projectBarrierOperation(
        final BarrierOperation operation,
        final java.util.ArrayList<DesktopIrOperationSpec> projectedOperations,
        final java.util.ArrayList<String> diagnostics
    ) {
        if (operation.qubitCount() > 2) {
            diagnostics.add("Barrier with more than two qubits is projected to the first two graphical endpoints.");
        }
        projectedOperations.add(new DesktopIrOperationSpec(
            "BARRIER",
            qubitReference(operation.qubit(0)),
            operation.qubitCount() > 1 ? qubitReference(operation.qubit(1)) : qubitReference(operation.qubit(0)),
            "q[0]",
            "c[0]",
            Math.PI / 2.0
        ));
    }

    private static double numericAngle(
        final ParameterExpression expression,
        final int operationIndex,
        final java.util.ArrayList<String> diagnostics
    ) {
        if (expression.isNumeric()) {
            return expression.numericValue();
        }
        diagnostics.add("Gate operation #" + operationIndex + " has symbolic parameter " + expression + "; graphical angle field keeps pi/2 fallback.");
        return Math.PI / 2.0;
    }

    private static String qubitReference(final Qubit qubit) {
        return qubit.register().name().value() + "[" + qubit.index() + "]";
    }

    private static String bitReference(final ClassicalBit bit) {
        return bit.register().name().value() + "[" + bit.index() + "]";
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
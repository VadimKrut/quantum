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
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalAssignment;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalComparisonOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalDeclaration;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpressionKind;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicateKind;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalType;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalTypeKind;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.BarrierOperation;
import ru.pathcreator.vadim.quantum.domain.operation.BlockOperation;
import ru.pathcreator.vadim.quantum.domain.operation.BranchOperation;
import ru.pathcreator.vadim.quantum.domain.operation.CallableInvocationOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalArrayDeclarationOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalAssignmentOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalCondition;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalDeclarationOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicallyControlledOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ConditionalBlockOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ControlledOperation;
import ru.pathcreator.vadim.quantum.domain.operation.DelayOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ForLoopOperation;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.HaltOperation;
import ru.pathcreator.vadim.quantum.domain.operation.LabelOperation;
import ru.pathcreator.vadim.quantum.domain.operation.MeasureOperation;
import ru.pathcreator.vadim.quantum.domain.operation.Operation;
import ru.pathcreator.vadim.quantum.domain.operation.OperationBlock;
import ru.pathcreator.vadim.quantum.domain.operation.ResetOperation;
import ru.pathcreator.vadim.quantum.domain.operation.SymbolicForLoopOperation;
import ru.pathcreator.vadim.quantum.domain.operation.TimingBoxOperation;
import ru.pathcreator.vadim.quantum.domain.operation.WaitOperation;
import ru.pathcreator.vadim.quantum.domain.operation.WhileLoopOperation;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;
import ru.pathcreator.vadim.quantum.domain.timing.DurationExpression;
import ru.pathcreator.vadim.quantum.domain.timing.DurationUnit;
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
            case "U" -> code.append("    .u(ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression.of(")
                .append(operation.angle())
                .append("), ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression.of(")
                .append(operation.secondAngle())
                .append("), ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression.of(")
                .append(operation.thirdAngle())
                .append("), \"")
                .append(operation.primaryQubit())
                .append("\")")
                .append(System.lineSeparator());
            case "CPHASE" -> code.append("    .cphase(")
                .append(operation.angle())
                .append(", \"")
                .append(operation.primaryQubit())
                .append("\", \"")
                .append(operation.secondaryQubit())
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
            case "DELAY" -> code.append("    .delay(ru.pathcreator.vadim.quantum.domain.timing.DurationExpression.duration(")
                .append(operation.durationValue())
                .append(", ru.pathcreator.vadim.quantum.domain.timing.DurationUnit.")
                .append(operation.durationUnit())
                .append("), \"")
                .append(operation.primaryQubit())
                .append("\", \"")
                .append(operation.secondaryQubit())
                .append("\")")
                .append(System.lineSeparator());
            case "LABEL" -> code.append("    .label(\"")
                .append(operation.labelName())
                .append("\")")
                .append(System.lineSeparator());
            case "BRANCH" -> code.append("    .branch(ru.pathcreator.vadim.quantum.domain.operation.BranchOperation.always(\"")
                .append(operation.labelName())
                .append("\"))")
                .append(System.lineSeparator());
            case "TIMING_BOX" -> code.append("    .timingBox(ru.pathcreator.vadim.quantum.domain.timing.DurationExpression.duration(")
                .append(operation.durationValue())
                .append(", ru.pathcreator.vadim.quantum.domain.timing.DurationUnit.")
                .append(operation.durationUnit())
                .append("), ")
                .append(operationBlockExpression(operation.bodyOperations()))
                .append(")")
                .append(System.lineSeparator());
            case "ASSIGN" -> code.append("    .assign(new ru.pathcreator.vadim.quantum.domain.classical.ClassicalAssignment(")
                .append("ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression.bit(bit(\"")
                .append(operation.classicalBit())
                .append("\")), ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression.integer(")
                .append(classicalLong(operation))
                .append("L)))")
                .append(System.lineSeparator());
            case "DECLARE" -> code.append("    .classicalDeclaration(new ru.pathcreator.vadim.quantum.domain.operation.ClassicalDeclarationOperation(")
                .append("new ru.pathcreator.vadim.quantum.domain.classical.ClassicalDeclaration(\"")
                .append(operation.labelName())
                .append("\", ru.pathcreator.vadim.quantum.domain.classical.ClassicalType.of(ru.pathcreator.vadim.quantum.domain.classical.ClassicalTypeKind.BIT)), ")
                .append("ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression.integer(")
                .append(classicalLong(operation))
                .append("L)))")
                .append(System.lineSeparator());
            case "ARRAY" -> code.append("    .classicalArrayDeclaration(new ru.pathcreator.vadim.quantum.domain.operation.ClassicalArrayDeclarationOperation(\"")
                .append(operation.labelName())
                .append("\", ru.pathcreator.vadim.quantum.domain.classical.ClassicalType.of(ru.pathcreator.vadim.quantum.domain.classical.ClassicalTypeKind.BIT), ")
                .append("java.util.List.of(ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression.integer(")
                .append(classicalPositiveLong(operation))
                .append("L)), null))")
                .append(System.lineSeparator());
            case "CALL" -> code.append("    .callableInvocation(new ru.pathcreator.vadim.quantum.domain.operation.CallableInvocationOperation(\"")
                .append(operation.labelName())
                .append("\", null, java.util.List.of(), java.util.List.of()))")
                .append(System.lineSeparator());
            case "IF_X" -> code.append("    .classicallyControlled(")
                .append(predicateJava(operation))
                .append(", ru.pathcreator.vadim.quantum.domain.operation.GateOperation.of(ru.pathcreator.vadim.quantum.domain.gate.StandardGate.X, qubit(\"")
                .append(operation.primaryQubit())
                .append("\")))")
                .append(System.lineSeparator());
            case "CTRL_X" -> code.append("    .controlled(ru.pathcreator.vadim.quantum.domain.operation.ClassicalCondition.equalTo(classicalRegister(\"")
                .append(classicalRegisterName(operation.classicalBit()))
                .append("\"), ")
                .append(classicalLong(operation))
                .append("L), ru.pathcreator.vadim.quantum.domain.operation.GateOperation.of(ru.pathcreator.vadim.quantum.domain.gate.StandardGate.X, qubit(\"")
                .append(operation.primaryQubit())
                .append("\")))")
                .append(System.lineSeparator());
            case "BLOCK" -> code.append("    .block(")
                .append(operationBlockExpression(operation.bodyOperations()))
                .append(")")
                .append(System.lineSeparator());
            case "IF_BLOCK" -> code.append("    .conditionalBlock(")
                .append(predicateJava(operation))
                .append(", ")
                .append(operationBlockExpression(operation.bodyOperations()))
                .append(", ")
                .append(operation.elseOperations().isEmpty()
                    ? "null"
                    : operationBlockExpression(operation.elseOperations()))
                .append(")")
                .append(System.lineSeparator());
            case "FOR" -> code.append("    .forLoop(\"")
                .append(operation.labelName())
                .append("\", 0L, 1L, ")
                .append(classicalPositiveLong(operation))
                .append("L, ")
                .append(operationBlockExpression(operation.bodyOperations()))
                .append(")")
                .append(System.lineSeparator());
            case "SYM_FOR" -> code.append("    .symbolicForLoop(new ru.pathcreator.vadim.quantum.domain.operation.SymbolicForLoopOperation(\"")
                .append(operation.labelName())
                .append("\", \"int\", ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression.integer(0L), ")
                .append("ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression.integer(1L), ")
                .append("ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression.integer(")
                .append(classicalPositiveLong(operation))
                .append("L), ")
                .append(operationBlockExpression(operation.bodyOperations()))
                .append("))")
                .append(System.lineSeparator());
            case "WHILE" -> code.append("    .whileLoop(")
                .append(predicateJava(operation))
                .append(", ")
                .append(operationBlockExpression(operation.bodyOperations()))
                .append(")")
                .append(System.lineSeparator());
            case "HALT" -> code.append("    .halt()")
                .append(System.lineSeparator());
            case "WAIT" -> code.append("    .waitInstruction()")
                .append(System.lineSeparator());
            default -> code.append("    .")
                .append(javaMethodName(operation.gate()))
                .append("(\"")
                .append(operation.primaryQubit())
                .append("\")")
                .append(System.lineSeparator());
        }
    }

    private static String operationBlockExpression(final List<DesktopIrOperationSpec> operations) {
        if (operations.isEmpty()) {
            return "ru.pathcreator.vadim.quantum.domain.operation.OperationBlock.of()";
        }
        final StringBuilder code = new StringBuilder("ru.pathcreator.vadim.quantum.domain.operation.OperationBlock.of(");
        for (int i = 0; i < operations.size(); i++) {
            if (i > 0) {
                code.append(", ");
            }
            code.append(javaOperationExpression(operations.get(i)));
        }
        code.append(")");
        return code.toString();
    }

    private static String javaOperationExpression(final DesktopIrOperationSpec operation) {
        return switch (operation.gate()) {
            case "H", "X", "Y", "Z", "S", "SDG", "T", "TDG", "ID" -> "ru.pathcreator.vadim.quantum.domain.operation.GateOperation.of("
                + "ru.pathcreator.vadim.quantum.domain.gate.StandardGate." + operation.gate()
                + ", qubit(\"" + operation.primaryQubit() + "\"))";
            case "RX", "RY", "RZ", "PHASE" -> "ru.pathcreator.vadim.quantum.domain.operation.GateOperation.parameterized("
                + "ru.pathcreator.vadim.quantum.domain.gate.StandardGate." + operation.gate()
                + ", new ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression[] { "
                + "ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression.of(" + operation.angle() + ") }, "
                + "qubit(\"" + operation.primaryQubit() + "\"))";
            case "U" -> "ru.pathcreator.vadim.quantum.domain.operation.GateOperation.parameterized("
                + "ru.pathcreator.vadim.quantum.domain.gate.StandardGate.U, "
                + "new ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression[] { "
                + "ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression.of(" + operation.angle() + "), "
                + "ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression.of(" + operation.secondAngle() + "), "
                + "ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression.of(" + operation.thirdAngle() + ") }, "
                + "qubit(\"" + operation.primaryQubit() + "\"))";
            case "CX", "CY", "CZ", "CH", "SWAP" -> "ru.pathcreator.vadim.quantum.domain.operation.GateOperation.of("
                + "ru.pathcreator.vadim.quantum.domain.gate.StandardGate." + operation.gate()
                + ", qubit(\"" + operation.primaryQubit() + "\"), qubit(\"" + operation.secondaryQubit() + "\"))";
            case "CCX" -> "ru.pathcreator.vadim.quantum.domain.operation.GateOperation.of("
                + "ru.pathcreator.vadim.quantum.domain.gate.StandardGate.CCX, qubit(\"" + operation.primaryQubit()
                + "\"), qubit(\"" + operation.secondaryQubit() + "\"), qubit(\"" + operation.tertiaryQubit() + "\"))";
            case "CPHASE" -> "ru.pathcreator.vadim.quantum.domain.operation.GateOperation.parameterized("
                + "ru.pathcreator.vadim.quantum.domain.gate.StandardGate.CPHASE, "
                + "new ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression[] { "
                + "ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression.of(" + operation.angle() + ") }, "
                + "qubit(\"" + operation.primaryQubit() + "\"), qubit(\"" + operation.secondaryQubit() + "\"))";
            case "MEASURE" -> "new ru.pathcreator.vadim.quantum.domain.operation.MeasureOperation(qubit(\""
                + operation.primaryQubit() + "\"), bit(\"" + operation.classicalBit() + "\"))";
            case "RESET" -> "new ru.pathcreator.vadim.quantum.domain.operation.ResetOperation(qubit(\""
                + operation.primaryQubit() + "\"))";
            case "BARRIER" -> "new ru.pathcreator.vadim.quantum.domain.operation.BarrierOperation(qubit(\""
                + operation.primaryQubit() + "\"), qubit(\"" + operation.secondaryQubit() + "\"))";
            case "DELAY" -> "new ru.pathcreator.vadim.quantum.domain.operation.DelayOperation("
                + "ru.pathcreator.vadim.quantum.domain.timing.DurationExpression.duration("
                + operation.durationValue() + ", ru.pathcreator.vadim.quantum.domain.timing.DurationUnit."
                + operation.durationUnit() + "), qubit(\"" + operation.primaryQubit() + "\"), qubit(\""
                + operation.secondaryQubit() + "\"))";
            case "LABEL" -> "new ru.pathcreator.vadim.quantum.domain.operation.LabelOperation(\""
                + operation.labelName() + "\")";
            case "BRANCH" -> "ru.pathcreator.vadim.quantum.domain.operation.BranchOperation.always(\""
                + operation.labelName() + "\")";
            case "TIMING_BOX" -> "new ru.pathcreator.vadim.quantum.domain.operation.TimingBoxOperation("
                + "ru.pathcreator.vadim.quantum.domain.timing.DurationExpression.duration("
                + operation.durationValue() + ", ru.pathcreator.vadim.quantum.domain.timing.DurationUnit."
                + operation.durationUnit() + "), " + operationBlockExpression(operation.bodyOperations()) + ")";
            case "ASSIGN" -> "new ru.pathcreator.vadim.quantum.domain.operation.ClassicalAssignmentOperation("
                + "new ru.pathcreator.vadim.quantum.domain.classical.ClassicalAssignment("
                + "ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression.bit(bit(\""
                + operation.classicalBit() + "\")), "
                + "ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression.integer("
                + classicalLong(operation) + "L)))";
            case "DECLARE" -> "new ru.pathcreator.vadim.quantum.domain.operation.ClassicalDeclarationOperation("
                + "new ru.pathcreator.vadim.quantum.domain.classical.ClassicalDeclaration(\""
                + operation.labelName()
                + "\", ru.pathcreator.vadim.quantum.domain.classical.ClassicalType.of("
                + "ru.pathcreator.vadim.quantum.domain.classical.ClassicalTypeKind.BIT)), "
                + "ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression.integer("
                + classicalLong(operation) + "L))";
            case "ARRAY" -> "new ru.pathcreator.vadim.quantum.domain.operation.ClassicalArrayDeclarationOperation(\""
                + operation.labelName()
                + "\", ru.pathcreator.vadim.quantum.domain.classical.ClassicalType.of("
                + "ru.pathcreator.vadim.quantum.domain.classical.ClassicalTypeKind.BIT), java.util.List.of("
                + "ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression.integer("
                + classicalPositiveLong(operation) + "L)), null)";
            case "CALL" -> "new ru.pathcreator.vadim.quantum.domain.operation.CallableInvocationOperation(\""
                + operation.labelName() + "\", null, java.util.List.of(), java.util.List.of())";
            case "IF_X" -> "new ru.pathcreator.vadim.quantum.domain.operation.ClassicallyControlledOperation("
                + predicateJava(operation) + ", "
                + "ru.pathcreator.vadim.quantum.domain.operation.GateOperation.of("
                + "ru.pathcreator.vadim.quantum.domain.gate.StandardGate.X, qubit(\"" + operation.primaryQubit() + "\")))";
            case "CTRL_X" -> "new ru.pathcreator.vadim.quantum.domain.operation.ControlledOperation("
                + "ru.pathcreator.vadim.quantum.domain.operation.ClassicalCondition.equalTo(classicalRegister(\""
                + classicalRegisterName(operation.classicalBit()) + "\"), " + classicalLong(operation) + "L), "
                + "ru.pathcreator.vadim.quantum.domain.operation.GateOperation.of("
                + "ru.pathcreator.vadim.quantum.domain.gate.StandardGate.X, qubit(\"" + operation.primaryQubit() + "\")))";
            case "BLOCK" -> "new ru.pathcreator.vadim.quantum.domain.operation.BlockOperation("
                + operationBlockExpression(operation.bodyOperations()) + ")";
            case "IF_BLOCK" -> "new ru.pathcreator.vadim.quantum.domain.operation.ConditionalBlockOperation("
                + predicateJava(operation) + ", "
                + operationBlockExpression(operation.bodyOperations()) + ", "
                + (operation.elseOperations().isEmpty() ? "null" : operationBlockExpression(operation.elseOperations())) + ")";
            case "FOR" -> "new ru.pathcreator.vadim.quantum.domain.operation.ForLoopOperation(\"" + operation.labelName()
                + "\", 0L, 1L, " + classicalPositiveLong(operation) + "L, "
                + operationBlockExpression(operation.bodyOperations()) + ")";
            case "SYM_FOR" -> "new ru.pathcreator.vadim.quantum.domain.operation.SymbolicForLoopOperation(\"" + operation.labelName()
                + "\", \"int\", ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression.integer(0L), "
                + "ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression.integer(1L), "
                + "ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression.integer(" + classicalPositiveLong(operation) + "L), "
                + operationBlockExpression(operation.bodyOperations()) + ")";
            case "WHILE" -> "new ru.pathcreator.vadim.quantum.domain.operation.WhileLoopOperation("
                + predicateJava(operation) + ", "
                + operationBlockExpression(operation.bodyOperations()) + ")";
            case "HALT" -> "ru.pathcreator.vadim.quantum.domain.operation.HaltOperation.INSTANCE";
            case "WAIT" -> "ru.pathcreator.vadim.quantum.domain.operation.WaitOperation.INSTANCE";
            default -> throw new IllegalArgumentException("Unsupported nested Java DSL operation: " + operation.gate() + ".");
        };
    }

    private static String javaMethodName(final String gate) {
        return switch (gate) {
            case "PHASE" -> "phase";
            default -> gate.toLowerCase();
        };
    }

    private static String predicateJava(final DesktopIrOperationSpec operation) {
        if (operation.predicate() != null) {
            return DesktopClassicalExpressionRenderer.predicateJava(operation.predicate());
        }
        return "ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate.compare("
            + "ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression.bit(bit(\""
            + operation.classicalBit() + "\")), "
            + "ru.pathcreator.vadim.quantum.domain.classical.ClassicalComparisonOperator.EQUAL, "
            + "ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression.integer("
            + classicalLong(operation) + "L))";
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
            case DELAY -> projectDelayOperation(
                (DelayOperation) operation,
                projectedOperations,
                diagnostics
            );
            case LABEL -> projectedOperations.add(new DesktopIrOperationSpec(
                "LABEL",
                "q[0]",
                "q[0]",
                "q[0]",
                "c[0]",
                Math.PI / 2.0,
                0.0,
                0.0,
                20.0,
                "NS",
                ((LabelOperation) operation).name()
            ));
            case BRANCH -> projectedOperations.add(branchOperation((BranchOperation) operation));
            case TIMING_BOX -> projectedOperations.add(timingBoxOperation((TimingBoxOperation) operation));
            case CLASSICAL_ASSIGNMENT -> projectedOperations.add(classicalAssignmentOperation((ClassicalAssignmentOperation) operation));
            case CLASSICAL_DECLARATION -> projectedOperations.add(classicalDeclarationOperation((ClassicalDeclarationOperation) operation));
            case CLASSICAL_ARRAY_DECLARATION -> projectedOperations.add(classicalArrayDeclarationOperation((ClassicalArrayDeclarationOperation) operation));
            case CALLABLE_INVOCATION -> projectedOperations.add(callableInvocationOperation((CallableInvocationOperation) operation));
            case CLASSICALLY_CONTROLLED -> projectedOperations.add(classicallyControlledOperation((ClassicallyControlledOperation) operation));
            case CONTROLLED -> projectedOperations.add(controlledOperation((ControlledOperation) operation));
            case BLOCK -> projectedOperations.add(blockOperation(
                (BlockOperation) operation,
                index
            ));
            case CONDITIONAL_BLOCK -> projectedOperations.add(conditionalBlockOperation(
                (ConditionalBlockOperation) operation,
                index
            ));
            case FOR_LOOP -> projectedOperations.add(forLoopOperation(
                (ForLoopOperation) operation,
                index
            ));
            case SYMBOLIC_FOR_LOOP -> projectedOperations.add(symbolicForLoopOperation(
                (SymbolicForLoopOperation) operation,
                index
            ));
            case WHILE_LOOP -> projectedOperations.add(whileLoopOperation((WhileLoopOperation) operation));
            case HALT -> projectedOperations.add(controlFlowOperation("HALT"));
            case WAIT -> projectedOperations.add(controlFlowOperation("WAIT"));
            default -> projectedOperations.add(readOnlyIrOperation(
                operation,
                index
            ));
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
            case "H", "X", "Y", "Z", "S", "SDG", "T", "TDG", "RX", "RY", "RZ", "U", "CX", "CY", "CZ", "CPHASE", "CH", "SWAP", "CCX", "ID" -> gate;
            case "PHASE" -> "PHASE";
            default -> null;
        };
        if (desktopGate == null) {
            diagnostics.add("Gate operation #" + index + " is not editable in the graphical gate grid: " + operation.gate().gateName() + ".");
            projectedOperations.add(readOnlyIrOperation(
                operation,
                index
            ));
            return;
        }
        if (operation.qubitCount() != operation.gate().arity()) {
            diagnostics.add("Gate operation #" + index + " has dynamic or non-canonical qubit references.");
            projectedOperations.add(readOnlyIrOperation(
                operation,
                index
            ));
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
            angle,
            operation.parameterCount() > 1 && operation.parameter(1).isNumeric() ? operation.parameter(1).numericValue() : 0.0,
            operation.parameterCount() > 2 && operation.parameter(2).isNumeric() ? operation.parameter(2).numericValue() : 0.0,
            20.0,
            "NS",
            "entry"
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

    private static void projectDelayOperation(
        final DelayOperation operation,
        final java.util.ArrayList<DesktopIrOperationSpec> projectedOperations,
        final java.util.ArrayList<String> diagnostics
    ) {
        if (operation.duration().isExpression() || operation.duration().isStretch()) {
            diagnostics.add("Delay with symbolic duration is projected with numeric 20ns fallback.");
        }
        if (operation.qubitCount() > 2) {
            diagnostics.add("Delay with more than two qubits is projected to the first two graphical endpoints.");
        }
        final double value = operation.duration().isExpression() || operation.duration().isStretch()
            ? 20.0
            : operation.duration().value();
        final String unit = operation.duration().isExpression() || operation.duration().isStretch()
            ? "NS"
            : operation.duration().unit().name();
        projectedOperations.add(new DesktopIrOperationSpec(
            "DELAY",
            qubitReference(operation.qubit(0)),
            operation.qubitCount() > 1 ? qubitReference(operation.qubit(1)) : qubitReference(operation.qubit(0)),
            "q[0]",
            "c[0]",
            Math.PI / 2.0,
            0.0,
            0.0,
            value,
            unit,
            "entry"
        ));
    }

    private static DesktopIrOperationSpec controlFlowOperation(final String gate) {
        return new DesktopIrOperationSpec(
            gate,
            "q[0]",
            "q[0]",
            "q[0]",
            "c[0]",
            Math.PI / 2.0,
            0.0,
            0.0,
            20.0,
            "NS",
            "entry"
        );
    }

    private static DesktopIrOperationSpec blockOperation(
        final BlockOperation operation,
        final int operationIndex
    ) {
        return withBody(
            controlFlowOperation("BLOCK"),
            projectBlockOperations(operation.body())
        );
    }

    private static DesktopIrOperationSpec conditionalBlockOperation(
        final ConditionalBlockOperation operation,
        final int operationIndex
    ) {
        if (
            operation.thenBlock() != null
            && (
                !operation.hasElseBlock()
                || operation.elseBlock() != null
            )
        ) {
            final BitEqualsPredicate predicate = bitEqualsPredicate(operation.predicate());
            return withBodyAndElse(
                new DesktopIrOperationSpec(
                    "IF_BLOCK",
                    "q[0]",
                    "q[0]",
                    "q[0]",
                    predicate == null ? "c[0]" : predicate.bitReference(),
                    predicate == null ? 1.0 : predicate.value(),
                    0.0,
                    0.0,
                    20.0,
                    "NS",
                    "if_block",
                    operation.predicate(),
                    List.of(),
                    List.of()
                ),
                projectBlockOperations(operation.thenBlock()),
                operation.hasElseBlock()
                    ? projectBlockOperations(operation.elseBlock())
                    : List.of()
            );
        }
        return readOnlyIrOperation(
            operation,
            operationIndex
        );
    }

    private static BitEqualsPredicate bitEqualsPredicate(final ClassicalPredicate predicate) {
        if (
            predicate.kind() != ClassicalPredicateKind.COMPARISON
            || predicate.comparisonOperator() != ClassicalComparisonOperator.EQUAL
        ) {
            return null;
        }
        final BitEqualsPredicate direct = bitEqualsPredicate(
            predicate.leftExpression(),
            predicate.rightExpression()
        );
        if (direct != null) {
            return direct;
        }
        return bitEqualsPredicate(
            predicate.rightExpression(),
            predicate.leftExpression()
        );
    }

    private static BitEqualsPredicate bitEqualsPredicate(
        final ClassicalExpression bitExpression,
        final ClassicalExpression valueExpression
    ) {
        if (
            bitExpression.kind() != ClassicalExpressionKind.BIT_REFERENCE
            || valueExpression.kind() != ClassicalExpressionKind.INTEGER
        ) {
            return null;
        }
        return new BitEqualsPredicate(
            bitReference(bitExpression.bit()),
            valueExpression.integerValue()
        );
    }

    private static DesktopIrOperationSpec branchOperation(final BranchOperation operation) {
        return new DesktopIrOperationSpec(
            "BRANCH",
            "q[0]",
            "q[0]",
            "q[0]",
            "c[0]",
            Math.PI / 2.0,
            0.0,
            0.0,
            20.0,
            "NS",
            operation.targetLabel()
        );
    }

    private static DesktopIrOperationSpec timingBoxOperation(final TimingBoxOperation operation) {
        final double value = operation.hasDuration()
            && !operation.duration().isExpression()
            && !operation.duration().isStretch()
                ? operation.duration().value()
                : 20.0;
        final String unit = operation.hasDuration()
            && !operation.duration().isExpression()
            && !operation.duration().isStretch()
                ? operation.duration().unit().name()
                : "NS";
        return withBody(
            new DesktopIrOperationSpec(
                "TIMING_BOX",
                "q[0]",
                "q[0]",
                "q[0]",
                "c[0]",
                Math.PI / 2.0,
                0.0,
                0.0,
                value,
                unit,
                "entry"
            ),
            projectBlockOperations(operation.body())
        );
    }

    private static DesktopIrOperationSpec classicalAssignmentOperation(final ClassicalAssignmentOperation operation) {
        return new DesktopIrOperationSpec(
            "ASSIGN",
            "q[0]",
            "q[0]",
            "q[0]",
            classicalTargetLabel(operation.assignment().target()),
            classicalExpressionValue(operation.assignment().value()),
            0.0,
            0.0,
            20.0,
            "NS",
            "assign"
        );
    }

    private static DesktopIrOperationSpec classicalDeclarationOperation(final ClassicalDeclarationOperation operation) {
        return new DesktopIrOperationSpec(
            "DECLARE",
            "q[0]",
            "q[0]",
            "q[0]",
            "c[0]",
            operation.hasInitializer() ? classicalExpressionValue(operation.initializer()) : 0.0,
            0.0,
            0.0,
            20.0,
            "NS",
            operation.declaration().name()
        );
    }

    private static DesktopIrOperationSpec classicalArrayDeclarationOperation(final ClassicalArrayDeclarationOperation operation) {
        return new DesktopIrOperationSpec(
            "ARRAY",
            "q[0]",
            "q[0]",
            "q[0]",
            "c[0]",
            operation.dimensionCount() == 0 ? 1.0 : Math.max(
                1.0,
                classicalExpressionValue(operation.dimension(0))
            ),
            0.0,
            0.0,
            20.0,
            "NS",
            operation.name()
        );
    }

    private static DesktopIrOperationSpec callableInvocationOperation(final CallableInvocationOperation operation) {
        return new DesktopIrOperationSpec(
            "CALL",
            "q[0]",
            "q[0]",
            "q[0]",
            "c[0]",
            Math.PI / 2.0,
            0.0,
            0.0,
            20.0,
            "NS",
            operation.callableName()
        );
    }

    private static DesktopIrOperationSpec classicallyControlledOperation(final ClassicallyControlledOperation operation) {
        final BitEqualsPredicate predicate = bitEqualsPredicate(operation.predicate());
        return new DesktopIrOperationSpec(
            "IF_X",
            controlledXTarget(operation.operation()),
            "q[0]",
            "q[0]",
            predicate == null ? "c[0]" : predicate.bitReference(),
            predicate == null ? 1.0 : predicate.value(),
            0.0,
            0.0,
            20.0,
            "NS",
            "if_x",
            operation.predicate(),
            List.of(),
            List.of()
        );
    }

    private static DesktopIrOperationSpec controlledOperation(final ControlledOperation operation) {
        return new DesktopIrOperationSpec(
            "CTRL_X",
            controlledXTarget(operation.operation()),
            "q[0]",
            "q[0]",
            "c[0]",
            operation.condition().expectedValue(),
            0.0,
            0.0,
            20.0,
            "NS",
            "ctrl_x"
        );
    }

    private static DesktopIrOperationSpec forLoopOperation(
        final ForLoopOperation operation,
        final int operationIndex
    ) {
        return withBody(
            new DesktopIrOperationSpec(
                "FOR",
                "q[0]",
                "q[0]",
                "q[0]",
                "c[0]",
                Math.max(
                    0L,
                    operation.endInclusive()
                ),
                0.0,
                0.0,
                20.0,
                "NS",
                operation.variableName()
            ),
            projectBlockOperations(operation.body())
        );
    }

    private static DesktopIrOperationSpec symbolicForLoopOperation(
        final SymbolicForLoopOperation operation,
        final int operationIndex
    ) {
        return withBody(
            new DesktopIrOperationSpec(
                "SYM_FOR",
                "q[0]",
                "q[0]",
                "q[0]",
                "c[0]",
                classicalExpressionValue(operation.endInclusive()),
                0.0,
                0.0,
                20.0,
                "NS",
                operation.variableName()
            ),
            projectBlockOperations(operation.body())
        );
    }

    private static DesktopIrOperationSpec whileLoopOperation(final WhileLoopOperation operation) {
        final BitEqualsPredicate predicate = bitEqualsPredicate(operation.predicate());
        return withBody(
            new DesktopIrOperationSpec(
                "WHILE",
                "q[0]",
                "q[0]",
                "q[0]",
                predicate == null ? "c[0]" : predicate.bitReference(),
                predicate == null ? Math.PI / 2.0 : predicate.value(),
                0.0,
                0.0,
                20.0,
                "NS",
                "while",
                operation.predicate(),
                List.of(),
                List.of()
            ),
            projectBlockOperations(operation.body())
        );
    }

    private static List<DesktopIrOperationSpec> projectBlockOperations(final OperationBlock block) {
        final java.util.ArrayList<DesktopIrOperationSpec> operations = new java.util.ArrayList<>();
        final java.util.ArrayList<String> diagnostics = new java.util.ArrayList<>();
        for (int i = 0; i < block.operationCount(); i++) {
            projectOperation(
                block.operation(i),
                i,
                operations,
                diagnostics
            );
        }
        return List.copyOf(operations);
    }

    private static DesktopIrOperationSpec withBody(
        final DesktopIrOperationSpec operation,
        final List<DesktopIrOperationSpec> body
    ) {
        return withBodyAndElse(
            operation,
            body,
            List.of()
        );
    }

    private static DesktopIrOperationSpec withBodyAndElse(
        final DesktopIrOperationSpec operation,
        final List<DesktopIrOperationSpec> body,
        final List<DesktopIrOperationSpec> elseBody
    ) {
        return new DesktopIrOperationSpec(
            operation.gate(),
            operation.primaryQubit(),
            operation.secondaryQubit(),
            operation.tertiaryQubit(),
            operation.classicalBit(),
            operation.angle(),
            operation.secondAngle(),
            operation.thirdAngle(),
            operation.durationValue(),
            operation.durationUnit(),
            operation.labelName(),
            operation.predicate(),
            body,
            elseBody
        );
    }

    private static String controlledXTarget(final Operation operation) {
        if (operation instanceof GateOperation gateOperation
            && gateOperation.qubitCount() == 1
            && "x".equalsIgnoreCase(gateOperation.gate().gateName())) {
            return qubitReference(gateOperation.qubit(0));
        }
        return "q[0]";
    }

    private static String classicalTargetLabel(final ClassicalExpression expression) {
        return switch (expression.kind()) {
            case BIT_REFERENCE -> bitReference(expression.bit());
            case REGISTER_REFERENCE -> expression.register().name().value() + "[0]";
            case VARIABLE_REFERENCE -> "c[0]";
            case SYMBOLIC_REFERENCE -> "c[0]";
            default -> "c[0]";
        };
    }

    private static double classicalExpressionValue(final ClassicalExpression expression) {
        return expression.kind() == ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpressionKind.INTEGER
            ? expression.integerValue()
            : 0.0;
    }

    private static DesktopIrOperationSpec readOnlyIrOperation(
        final Operation operation,
        final int operationIndex
    ) {
        return new DesktopIrOperationSpec(
            "IR:" + operation.kind().name(),
            "q[0]",
            "q[0]",
            "q[0]",
            "c[0]",
            Math.PI / 2.0,
            0.0,
            0.0,
            20.0,
            "NS",
            "#" + operationIndex + " " + operation.kind().name()
        );
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
            case "SDG" -> circuit.sdg(operation.primaryQubit());
            case "T" -> circuit.t(operation.primaryQubit());
            case "TDG" -> circuit.tdg(operation.primaryQubit());
            case "ID" -> circuit.id(operation.primaryQubit());
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
            case "U" -> circuit.u(
                ParameterExpression.of(operation.angle()),
                ParameterExpression.of(operation.secondAngle()),
                ParameterExpression.of(operation.thirdAngle()),
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
            case "CPHASE" -> circuit.cphase(
                operation.angle(),
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
            case "DELAY" -> circuit.delay(
                DurationExpression.duration(
                    operation.durationValue(),
                    DurationUnit.valueOf(operation.durationUnit())
                ),
                operation.primaryQubit(),
                operation.secondaryQubit()
            );
            case "LABEL" -> circuit.label(operation.labelName());
            case "BRANCH" -> circuit.branch(BranchOperation.always(operation.labelName()));
            case "TIMING_BOX" -> circuit.timingBox(
                DurationExpression.duration(
                    operation.durationValue(),
                    DurationUnit.valueOf(operation.durationUnit())
                ),
                operationBlock(
                    circuit,
                    operation.bodyOperations()
                )
            );
            case "ASSIGN" -> circuit.assign(new ClassicalAssignment(
                ClassicalExpression.bit(circuit.bit(operation.classicalBit())),
                ClassicalExpression.integer(classicalLong(operation))
            ));
            case "DECLARE" -> circuit.classicalDeclaration(new ClassicalDeclarationOperation(
                new ClassicalDeclaration(
                    operation.labelName(),
                    ClassicalType.of(ClassicalTypeKind.BIT)
                ),
                ClassicalExpression.integer(classicalLong(operation))
            ));
            case "ARRAY" -> circuit.classicalArrayDeclaration(new ClassicalArrayDeclarationOperation(
                operation.labelName(),
                ClassicalType.of(ClassicalTypeKind.BIT),
                List.of(ClassicalExpression.integer(classicalPositiveLong(operation))),
                null
            ));
            case "CALL" -> circuit.callableInvocation(new CallableInvocationOperation(
                operation.labelName(),
                null,
                List.of(),
                List.of()
            ));
            case "IF_X" -> circuit.classicallyControlled(
                predicate(
                    circuit,
                    operation
                ),
                GateOperation.of(
                    StandardGate.X,
                    circuit.qubit(operation.primaryQubit())
                )
            );
            case "CTRL_X" -> circuit.controlled(
                ClassicalCondition.equalTo(
                    circuit.classicalRegister(classicalRegisterName(operation.classicalBit())),
                    classicalLong(operation)
                ),
                GateOperation.of(
                    StandardGate.X,
                    circuit.qubit(operation.primaryQubit())
                )
            );
            case "BLOCK" -> circuit.block(operationBlock(
                circuit,
                operation.bodyOperations()
            ));
            case "IF_BLOCK" -> circuit.conditionalBlock(
                predicate(
                    circuit,
                    operation
                ),
                operationBlock(
                    circuit,
                    operation.bodyOperations()
                ),
                operation.elseOperations().isEmpty()
                    ? null
                    : operationBlock(
                        circuit,
                        operation.elseOperations()
                    )
            );
            case "FOR" -> circuit.forLoop(
                operation.labelName(),
                0L,
                1L,
                classicalPositiveLong(operation),
                operationBlock(
                    circuit,
                    operation.bodyOperations()
                )
            );
            case "SYM_FOR" -> circuit.symbolicForLoop(new SymbolicForLoopOperation(
                operation.labelName(),
                "int",
                ClassicalExpression.integer(0L),
                ClassicalExpression.integer(1L),
                ClassicalExpression.integer(classicalPositiveLong(operation)),
                operationBlock(
                    circuit,
                    operation.bodyOperations()
                )
            ));
            case "WHILE" -> circuit.whileLoop(
                predicate(
                    circuit,
                    operation
                ),
                operationBlock(
                    circuit,
                    operation.bodyOperations()
                )
            );
            case "HALT" -> circuit.halt();
            case "WAIT" -> circuit.waitInstruction();
            default -> throw new IllegalArgumentException("Unsupported desktop IR operation: " + operation.gate() + ".");
        }
    }

    private static OperationBlock operationBlock(
        final QuantumCircuitBuilder circuit,
        final List<DesktopIrOperationSpec> operations
    ) {
        final java.util.ArrayList<Operation> result = new java.util.ArrayList<>();
        for (int i = 0; i < operations.size(); i++) {
            result.add(toOperation(
                circuit,
                operations.get(i)
            ));
        }
        return OperationBlock.of(result);
    }

    private static Operation toOperation(
        final QuantumCircuitBuilder circuit,
        final DesktopIrOperationSpec operation
    ) {
        return switch (operation.gate()) {
            case "H" -> GateOperation.of(
                StandardGate.H,
                circuit.qubit(operation.primaryQubit())
            );
            case "X" -> GateOperation.of(
                StandardGate.X,
                circuit.qubit(operation.primaryQubit())
            );
            case "Y" -> GateOperation.of(
                StandardGate.Y,
                circuit.qubit(operation.primaryQubit())
            );
            case "Z" -> GateOperation.of(
                StandardGate.Z,
                circuit.qubit(operation.primaryQubit())
            );
            case "S" -> GateOperation.of(
                StandardGate.S,
                circuit.qubit(operation.primaryQubit())
            );
            case "SDG" -> GateOperation.of(
                StandardGate.SDG,
                circuit.qubit(operation.primaryQubit())
            );
            case "T" -> GateOperation.of(
                StandardGate.T,
                circuit.qubit(operation.primaryQubit())
            );
            case "TDG" -> GateOperation.of(
                StandardGate.TDG,
                circuit.qubit(operation.primaryQubit())
            );
            case "ID" -> GateOperation.of(
                StandardGate.ID,
                circuit.qubit(operation.primaryQubit())
            );
            case "RX" -> GateOperation.parameterized(
                StandardGate.RX,
                new ParameterExpression[] {
                    ParameterExpression.of(operation.angle())
                },
                circuit.qubit(operation.primaryQubit())
            );
            case "RY" -> GateOperation.parameterized(
                StandardGate.RY,
                new ParameterExpression[] {
                    ParameterExpression.of(operation.angle())
                },
                circuit.qubit(operation.primaryQubit())
            );
            case "RZ" -> GateOperation.parameterized(
                StandardGate.RZ,
                new ParameterExpression[] {
                    ParameterExpression.of(operation.angle())
                },
                circuit.qubit(operation.primaryQubit())
            );
            case "PHASE" -> GateOperation.parameterized(
                StandardGate.PHASE,
                new ParameterExpression[] {
                    ParameterExpression.of(operation.angle())
                },
                circuit.qubit(operation.primaryQubit())
            );
            case "U" -> GateOperation.parameterized(
                StandardGate.U,
                new ParameterExpression[] {
                    ParameterExpression.of(operation.angle()),
                    ParameterExpression.of(operation.secondAngle()),
                    ParameterExpression.of(operation.thirdAngle())
                },
                circuit.qubit(operation.primaryQubit())
            );
            case "CX" -> GateOperation.of(
                StandardGate.CX,
                circuit.qubit(operation.primaryQubit()),
                circuit.qubit(operation.secondaryQubit())
            );
            case "CY" -> GateOperation.of(
                StandardGate.CY,
                circuit.qubit(operation.primaryQubit()),
                circuit.qubit(operation.secondaryQubit())
            );
            case "CZ" -> GateOperation.of(
                StandardGate.CZ,
                circuit.qubit(operation.primaryQubit()),
                circuit.qubit(operation.secondaryQubit())
            );
            case "CPHASE" -> GateOperation.parameterized(
                StandardGate.CPHASE,
                new ParameterExpression[] {
                    ParameterExpression.of(operation.angle())
                },
                circuit.qubit(operation.primaryQubit()),
                circuit.qubit(operation.secondaryQubit())
            );
            case "CH" -> GateOperation.of(
                StandardGate.CH,
                circuit.qubit(operation.primaryQubit()),
                circuit.qubit(operation.secondaryQubit())
            );
            case "SWAP" -> GateOperation.of(
                StandardGate.SWAP,
                circuit.qubit(operation.primaryQubit()),
                circuit.qubit(operation.secondaryQubit())
            );
            case "CCX" -> GateOperation.of(
                StandardGate.CCX,
                circuit.qubit(operation.primaryQubit()),
                circuit.qubit(operation.secondaryQubit()),
                circuit.qubit(operation.tertiaryQubit())
            );
            case "MEASURE" -> new MeasureOperation(
                circuit.qubit(operation.primaryQubit()),
                circuit.bit(operation.classicalBit())
            );
            case "RESET" -> new ResetOperation(circuit.qubit(operation.primaryQubit()));
            case "BARRIER" -> new BarrierOperation(
                circuit.qubit(operation.primaryQubit()),
                circuit.qubit(operation.secondaryQubit())
            );
            case "DELAY" -> new DelayOperation(
                DurationExpression.duration(
                    operation.durationValue(),
                    DurationUnit.valueOf(operation.durationUnit())
                ),
                circuit.qubit(operation.primaryQubit()),
                circuit.qubit(operation.secondaryQubit())
            );
            case "LABEL" -> new LabelOperation(operation.labelName());
            case "BRANCH" -> BranchOperation.always(operation.labelName());
            case "TIMING_BOX" -> new TimingBoxOperation(
                DurationExpression.duration(
                    operation.durationValue(),
                    DurationUnit.valueOf(operation.durationUnit())
                ),
                operationBlock(
                    circuit,
                    operation.bodyOperations()
                )
            );
            case "ASSIGN" -> new ClassicalAssignmentOperation(new ClassicalAssignment(
                ClassicalExpression.bit(circuit.bit(operation.classicalBit())),
                ClassicalExpression.integer(classicalLong(operation))
            ));
            case "DECLARE" -> new ClassicalDeclarationOperation(
                new ClassicalDeclaration(
                    operation.labelName(),
                    ClassicalType.of(ClassicalTypeKind.BIT)
                ),
                ClassicalExpression.integer(classicalLong(operation))
            );
            case "ARRAY" -> new ClassicalArrayDeclarationOperation(
                operation.labelName(),
                ClassicalType.of(ClassicalTypeKind.BIT),
                List.of(ClassicalExpression.integer(classicalPositiveLong(operation))),
                null
            );
            case "CALL" -> new CallableInvocationOperation(
                operation.labelName(),
                null,
                List.of(),
                List.of()
            );
            case "IF_X" -> new ClassicallyControlledOperation(
                predicate(
                    circuit,
                    operation
                ),
                GateOperation.of(
                    StandardGate.X,
                    circuit.qubit(operation.primaryQubit())
                )
            );
            case "CTRL_X" -> new ControlledOperation(
                ClassicalCondition.equalTo(
                    circuit.classicalRegister(classicalRegisterName(operation.classicalBit())),
                    classicalLong(operation)
                ),
                GateOperation.of(
                    StandardGate.X,
                    circuit.qubit(operation.primaryQubit())
                )
            );
            case "BLOCK" -> new BlockOperation(operationBlock(
                circuit,
                operation.bodyOperations()
            ));
            case "IF_BLOCK" -> new ConditionalBlockOperation(
                predicate(
                    circuit,
                    operation
                ),
                operationBlock(
                    circuit,
                    operation.bodyOperations()
                ),
                operation.elseOperations().isEmpty()
                    ? null
                    : operationBlock(
                        circuit,
                        operation.elseOperations()
                    )
            );
            case "FOR" -> new ForLoopOperation(
                operation.labelName(),
                0L,
                1L,
                classicalPositiveLong(operation),
                operationBlock(
                    circuit,
                    operation.bodyOperations()
                )
            );
            case "SYM_FOR" -> new SymbolicForLoopOperation(
                operation.labelName(),
                "int",
                ClassicalExpression.integer(0L),
                ClassicalExpression.integer(1L),
                ClassicalExpression.integer(classicalPositiveLong(operation)),
                operationBlock(
                    circuit,
                    operation.bodyOperations()
                )
            );
            case "WHILE" -> new WhileLoopOperation(
                predicate(
                    circuit,
                    operation
                ),
                operationBlock(
                    circuit,
                    operation.bodyOperations()
                )
            );
            case "HALT" -> HaltOperation.INSTANCE;
            case "WAIT" -> WaitOperation.INSTANCE;
            default -> throw new IllegalArgumentException("Unsupported nested desktop IR operation: " + operation.gate() + ".");
        };
    }

    private static ClassicalPredicate predicate(
        final QuantumCircuitBuilder circuit,
        final DesktopIrOperationSpec operation
    ) {
        if (operation.predicate() != null) {
            return remapPredicate(
                circuit,
                operation.predicate()
            );
        }
        return ClassicalPredicate.compare(
            ClassicalExpression.bit(circuit.bit(operation.classicalBit())),
            ClassicalComparisonOperator.EQUAL,
            ClassicalExpression.integer(classicalLong(operation))
        );
    }

    private static ClassicalPredicate remapPredicate(
        final QuantumCircuitBuilder circuit,
        final ClassicalPredicate predicate
    ) {
        return switch (predicate.kind()) {
            case COMPARISON -> ClassicalPredicate.compare(
                remapExpression(
                    circuit,
                    predicate.leftExpression()
                ),
                predicate.comparisonOperator(),
                remapExpression(
                    circuit,
                    predicate.rightExpression()
                )
            );
            case BOOLEAN -> switch (predicate.booleanOperator()) {
                case AND -> ClassicalPredicate.and(
                    remapPredicate(
                        circuit,
                        predicate.leftPredicate()
                    ),
                    remapPredicate(
                        circuit,
                        predicate.rightPredicate()
                    )
                );
                case OR -> ClassicalPredicate.or(
                    remapPredicate(
                        circuit,
                        predicate.leftPredicate()
                    ),
                    remapPredicate(
                        circuit,
                        predicate.rightPredicate()
                    )
                );
            };
            case NOT -> ClassicalPredicate.not(remapPredicate(
                circuit,
                predicate.leftPredicate()
            ));
        };
    }

    private static ClassicalExpression remapExpression(
        final QuantumCircuitBuilder circuit,
        final ClassicalExpression expression
    ) {
        return switch (expression.kind()) {
            case INTEGER -> ClassicalExpression.integer(expression.integerValue());
            case VARIABLE_REFERENCE -> ClassicalExpression.variable(expression.variableName());
            case BINARY_OPERATION -> ClassicalExpression.binary(
                expression.binaryOperator(),
                remapExpression(
                    circuit,
                    expression.leftExpression()
                ),
                remapExpression(
                    circuit,
                    expression.rightExpression()
                )
            );
            case BIT_REFERENCE -> ClassicalExpression.bit(circuit.bit(bitReference(expression.bit())));
            case REGISTER_REFERENCE -> ClassicalExpression.register(circuit.classicalRegister(
                expression.register().name().value()
            ));
            case SYMBOLIC_REFERENCE -> ClassicalExpression.symbolicReference(expression.symbolicText());
            case CALL -> ClassicalExpression.call(
                expression.callableName(),
                expression.callArguments().stream()
                    .map(argument -> remapExpression(
                        circuit,
                        argument
                    ))
                    .toList()
            );
        };
    }

    private static long classicalLong(final DesktopIrOperationSpec operation) {
        if (!Double.isFinite(operation.angle())) {
            throw new IllegalArgumentException("Classical shortcut value must be finite.");
        }
        return Math.round(operation.angle());
    }

    private static long classicalPositiveLong(final DesktopIrOperationSpec operation) {
        return Math.max(
            1L,
            classicalLong(operation)
        );
    }

    private static String classicalRegisterName(final String bitReference) {
        if (bitReference == null) {
            return "c";
        }
        final int open = bitReference.indexOf('[');
        if (open <= 0) {
            return bitReference;
        }
        return bitReference.substring(
            0,
            open
        );
    }

    /**
     * Упрощенный predicate, который visual builder может редактировать без потери смысла.
     */
    private record BitEqualsPredicate(String bitReference, long value) {
    }
}
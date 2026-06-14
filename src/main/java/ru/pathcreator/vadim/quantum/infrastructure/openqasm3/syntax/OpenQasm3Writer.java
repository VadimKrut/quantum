/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.openqasm3.syntax;

import java.util.LinkedHashMap;
import java.util.Map;

import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnostic;
import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnosticCode;
import ru.pathcreator.vadim.quantum.domain.bit.ClassicalBit;
import ru.pathcreator.vadim.quantum.domain.bit.Qubit;
import ru.pathcreator.vadim.quantum.domain.calibration.CalibrationDefinition;
import ru.pathcreator.vadim.quantum.domain.callable.CallableArgumentKind;
import ru.pathcreator.vadim.quantum.domain.callable.ExternalCallableDeclaration;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalBinaryOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalComparisonOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalAssignment;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalBooleanOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpressionKind;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicateKind;
import ru.pathcreator.vadim.quantum.domain.gate.Gate;
import ru.pathcreator.vadim.quantum.domain.gate.GateBodyOperation;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinition;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinitionKind;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpressionKind;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.BarrierOperation;
import ru.pathcreator.vadim.quantum.domain.operation.BlockOperation;
import ru.pathcreator.vadim.quantum.domain.operation.CallableInvocationOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalArrayDeclarationOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalAssignmentOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicallyControlledOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalDeclarationOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ConditionalBlockOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ControlledOperation;
import ru.pathcreator.vadim.quantum.domain.operation.DelayOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ForLoopOperation;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.MeasureOperation;
import ru.pathcreator.vadim.quantum.domain.operation.Operation;
import ru.pathcreator.vadim.quantum.domain.operation.OperationBlock;
import ru.pathcreator.vadim.quantum.domain.operation.QuantumReference;
import ru.pathcreator.vadim.quantum.domain.operation.QuantumReferenceKind;
import ru.pathcreator.vadim.quantum.domain.operation.ResetOperation;
import ru.pathcreator.vadim.quantum.domain.operation.SymbolicForLoopOperation;
import ru.pathcreator.vadim.quantum.domain.operation.TimingBoxOperation;
import ru.pathcreator.vadim.quantum.domain.operation.WhileLoopOperation;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;
import ru.pathcreator.vadim.quantum.domain.timing.DurationExpression;
import ru.pathcreator.vadim.quantum.infrastructure.openqasm3.mapping.OpenQasm3GateMapper;

/**
 * Writer OpenQASM 3.0 для поддерживаемой части Quantum IR.
 */
public final class OpenQasm3Writer {

    private static final String CALIBRATION_BODY_LANGUAGE = "openqasm3";

    /**
     * Пишет одну схему в OpenQASM 3.0.
     *
     * @param program схема Quantum IR
     * @return результат writer
     */
    public OpenQasm3WriterResult write(final QuantumProgram program) {
        if (program == null) {
            throw new IllegalArgumentException("Quantum program must not be null.");
        }
        if (program.circuitCount() != 1) {
            return OpenQasm3WriterResult.failure(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.UNSUPPORTED_CIRCUIT_STRUCTURE,
                "OpenQASM 3 writer supports exactly one circuit per program."
            ));
        }
        return write(
            program,
            program.circuit(0)
        );
    }

    private OpenQasm3WriterResult write(
        final QuantumProgram program,
        final QuantumCircuit circuit
    ) {
        if (circuit == null) {
            throw new IllegalArgumentException("Quantum circuit must not be null.");
        }
        final Map<String, GateDefinition> definitionsByName = definitionsByName(program);
        final StringBuilder builder = new StringBuilder(estimatedOutputCapacity(circuit));
        builder.append("OPENQASM 3.0;\n");
        builder.append("include \"stdgates.inc\";\n");

        for (int i = 0; i < program.gateDefinitionCount(); i++) {
            final OpenQasm3WriterResult result = appendGateDefinition(
                builder,
                program.gateDefinition(i),
                definitionsByName
            );
            if (!result.isSuccess()) {
                return result;
            }
        }
        final OpenQasm3WriterResult calibrationResult = appendCalibrationDefinitions(
            builder,
            program
        );
        if (!calibrationResult.isSuccess()) {
            return calibrationResult;
        }
        appendExternalCallableDeclarations(
            builder,
            program
        );

        for (int i = 0; i < circuit.quantumRegisterCount(); i++) {
            final QuantumRegister register = circuit.quantumRegister(i);
            builder.append("qubit[")
                .append(register.size())
                .append("] ")
                .append(register.name().value())
                .append(";\n");
        }
        for (int i = 0; i < circuit.classicalRegisterCount(); i++) {
            final ClassicalRegister register = circuit.classicalRegister(i);
            builder.append("bit[")
                .append(register.size())
                .append("] ")
                .append(register.name().value())
                .append(";\n");
        }
        for (int i = 0; i < circuit.operationCount(); i++) {
            final OpenQasm3WriterResult result = appendOperation(
                builder,
                circuit.operation(i),
                definitionsByName,
                ""
            );
            if (!result.isSuccess()) {
                return result;
            }
        }
        return OpenQasm3WriterResult.success(builder.toString());
    }

    private static OpenQasm3WriterResult appendCalibrationDefinitions(
        final StringBuilder builder,
        final QuantumProgram program
    ) {
        for (int i = 0; i < program.calibrationDefinitionCount(); i++) {
            final CalibrationDefinition definition = program.calibrationDefinition(i);
            if (!CALIBRATION_BODY_LANGUAGE.equals(definition.bodyLanguage())) {
                return OpenQasm3WriterResult.failure(IntegrationDiagnostic.error(
                    IntegrationDiagnosticCode.UNSUPPORTED_OPERATION,
                    "OpenQASM 3 export does not support calibration body language: "
                        + definition.bodyLanguage()
                        + "."
                ));
            }
            builder.append(definition.body().trim());
            if (requiresStatementTerminator(definition.body())) {
                builder.append(';');
            }
            builder.append('\n');
        }
        return OpenQasm3WriterResult.success("");
    }

    private static boolean requiresStatementTerminator(final String content) {
        final String trimmed = content.trim();
        return !trimmed.endsWith(";")
            && !trimmed.endsWith("}");
    }

    private static void appendExternalCallableDeclarations(
        final StringBuilder builder,
        final QuantumProgram program
    ) {
        for (int i = 0; i < program.externalCallableDeclarationCount(); i++) {
            final ExternalCallableDeclaration declaration = program.externalCallableDeclaration(i);
            builder.append("extern ")
                .append(declaration.name())
                .append("(");
            for (int j = 0; j < declaration.argumentCount(); j++) {
                if (j > 0) {
                    builder.append(", ");
                }
                if (declaration.argument(j).kind() == CallableArgumentKind.QUBIT) {
                    builder.append("qubit");
                } else {
                    builder.append(formatClassicalType(declaration.argument(j).classicalType()));
                }
            }
            builder.append(")");
            if (declaration.hasReturnType()) {
                builder.append(" -> ")
                    .append(formatClassicalType(declaration.returnType()));
            }
            builder.append(";\n");
        }
    }

    private static OpenQasm3WriterResult appendOperation(
        final StringBuilder builder,
        final Operation operation,
        final Map<String, GateDefinition> definitionsByName,
        final String indent
    ) {
        if (operation instanceof GateOperation gateOperation) {
            final String gateName = openQasmGateName(
                gateOperation.gate(),
                definitionsByName
            );
            if (gateName == null) {
                return OpenQasm3WriterResult.failure(IntegrationDiagnostic.error(
                    IntegrationDiagnosticCode.UNSUPPORTED_GATE,
                    "OpenQASM 3 export does not support gate: " + gateOperation.gate().gateName() + "."
                ));
            }
            builder.append(indent);
            appendGateOperation(
                builder,
                gateName,
                gateOperation
            );
        } else if (operation instanceof MeasureOperation measureOperation) {
            builder.append(indent);
            appendMeasureOperation(
                builder,
                measureOperation
            );
        } else if (operation instanceof ResetOperation resetOperation) {
            builder.append(indent)
                .append("reset ")
                .append(qubitReference(
                    resetOperation.qubitReference()
                ))
                .append(";\n");
        } else if (operation instanceof BarrierOperation barrierOperation) {
            builder.append(indent);
            appendBarrierOperation(
                builder,
                barrierOperation
            );
        } else if (operation instanceof ControlledOperation controlledOperation) {
            return appendControlledOperation(
                builder,
                controlledOperation,
                definitionsByName,
                indent
            );
        } else if (operation instanceof ClassicalAssignmentOperation assignmentOperation) {
            appendClassicalAssignment(
                builder,
                assignmentOperation.assignment(),
                indent
            );
        } else if (operation instanceof ClassicalDeclarationOperation declarationOperation) {
            appendClassicalDeclaration(
                builder,
                declarationOperation,
                indent
            );
        } else if (operation instanceof ClassicalArrayDeclarationOperation arrayOperation) {
            appendClassicalArrayDeclaration(
                builder,
                arrayOperation,
                indent
            );
        } else if (operation instanceof CallableInvocationOperation invocationOperation) {
            appendCallableInvocation(
                builder,
                invocationOperation,
                indent
            );
        } else if (operation instanceof ClassicallyControlledOperation controlledOperation) {
            return appendClassicallyControlledOperation(
                builder,
                controlledOperation,
                definitionsByName,
                indent
            );
        } else if (operation instanceof BlockOperation blockOperation) {
            return appendBlock(
                builder,
                blockOperation.body(),
                definitionsByName,
                indent
            );
        } else if (operation instanceof ConditionalBlockOperation conditionalOperation) {
            return appendConditionalBlockOperation(
                builder,
                conditionalOperation,
                definitionsByName,
                indent
            );
        } else if (operation instanceof ForLoopOperation loopOperation) {
            return appendForLoopOperation(
                builder,
                loopOperation,
                definitionsByName,
                indent
            );
        } else if (operation instanceof SymbolicForLoopOperation loopOperation) {
            return appendSymbolicForLoopOperation(
                builder,
                loopOperation,
                definitionsByName,
                indent
            );
        } else if (operation instanceof WhileLoopOperation loopOperation) {
            return appendWhileLoopOperation(
                builder,
                loopOperation,
                definitionsByName,
                indent
            );
        } else if (operation instanceof DelayOperation delayOperation) {
            appendDelayOperation(
                builder,
                delayOperation,
                indent
            );
        } else if (operation instanceof TimingBoxOperation boxOperation) {
            return appendTimingBoxOperation(
                builder,
                boxOperation,
                definitionsByName,
                indent
            );
        } else {
            return OpenQasm3WriterResult.failure(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.UNSUPPORTED_OPERATION,
                "OpenQASM 3 export does not support operation kind: " + operation.kind() + "."
            ));
        }
        return OpenQasm3WriterResult.success("");
    }

    private static OpenQasm3WriterResult appendGateDefinition(
        final StringBuilder builder,
        final GateDefinition definition,
        final Map<String, GateDefinition> definitionsByName
    ) {
        if (definition.kind() == GateDefinitionKind.INTRINSIC) {
            return OpenQasm3WriterResult.success("");
        }
        if (definition.kind() == GateDefinitionKind.OPAQUE) {
            return OpenQasm3WriterResult.failure(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.UNSUPPORTED_GATE,
                "OpenQASM 3 export cannot emit opaque gate without a gate body or defcal body: "
                    + definition.gateName()
                    + "."
            ));
        }
        builder.append("gate ")
            .append(definition.gateName());
        appendDeclarationParameters(
            builder,
            definition
        );
        appendDeclarationQubits(
            builder,
            definition
        );
        builder.append(" {\n");
        for (int i = 0; i < definition.bodyOperations().size(); i++) {
            final OpenQasm3WriterResult result = appendGateBodyOperation(
                builder,
                definition.bodyOperations().get(i),
                definitionsByName
            );
            if (!result.isSuccess()) {
                return result;
            }
        }
        builder.append("}\n");
        return OpenQasm3WriterResult.success("");
    }

    private static void appendDeclarationParameters(
        final StringBuilder builder,
        final GateDefinition definition
    ) {
        if (!definition.parameterNames().isEmpty()) {
            builder.append("(");
            for (int i = 0; i < definition.parameterNames().size(); i++) {
                if (i > 0) {
                    builder.append(",");
                }
                builder.append(definition.parameterNames().get(i));
            }
            builder.append(")");
        }
    }

    private static void appendDeclarationQubits(
        final StringBuilder builder,
        final GateDefinition definition
    ) {
        builder.append(" ");
        for (int i = 0; i < definition.qubitNames().size(); i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append(definition.qubitNames().get(i));
        }
    }

    private static OpenQasm3WriterResult appendGateBodyOperation(
        final StringBuilder builder,
        final GateBodyOperation operation,
        final Map<String, GateDefinition> definitionsByName
    ) {
        final String gateName = openQasmGateName(
            operation.gate(),
            definitionsByName
        );
        if (gateName == null) {
            return OpenQasm3WriterResult.failure(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.UNSUPPORTED_GATE,
                "OpenQASM 3 export does not support gate inside gate definition: "
                    + operation.gate().gateName()
                    + "."
            ));
        }
        builder.append("  ")
            .append(gateName);
        if (operation.parameterCount() > 0) {
            builder.append("(");
            for (int i = 0; i < operation.parameterCount(); i++) {
                if (i > 0) {
                    builder.append(",");
                }
                builder.append(parameterReference(operation.parameter(i)));
            }
            builder.append(")");
        }
        builder.append(" ");
        for (int i = 0; i < operation.qubitCount(); i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append(operation.qubitName(i));
        }
        builder.append(";\n");
        return OpenQasm3WriterResult.success("");
    }

    private static OpenQasm3WriterResult appendControlledOperation(
        final StringBuilder builder,
        final ControlledOperation operation,
        final Map<String, GateDefinition> definitionsByName,
        final String indent
    ) {
        builder.append(indent)
            .append("if(")
            .append(operation.condition().register().name().value())
            .append("==")
            .append(operation.condition().expectedValue())
            .append(") ");
        final Operation nested = operation.operation();
        if (nested instanceof GateOperation gateOperation) {
            final String gateName = openQasmGateName(
                gateOperation.gate(),
                definitionsByName
            );
            if (gateName == null) {
                return OpenQasm3WriterResult.failure(IntegrationDiagnostic.error(
                    IntegrationDiagnosticCode.UNSUPPORTED_GATE,
                    "OpenQASM 3 export does not support gate: " + gateOperation.gate().gateName() + "."
                ));
            }
            appendGateOperation(
                builder,
                gateName,
                gateOperation
            );
            return OpenQasm3WriterResult.success("");
        }
        if (nested instanceof ResetOperation resetOperation) {
            builder.append("reset ")
                .append(qubitReference(
                    resetOperation.qubitReference()
                ))
                .append(";\n");
            return OpenQasm3WriterResult.success("");
        }
        if (nested instanceof MeasureOperation measureOperation) {
            appendMeasureOperation(
                builder,
                measureOperation
            );
            return OpenQasm3WriterResult.success("");
        }
        if (nested instanceof BarrierOperation barrierOperation) {
            appendBarrierOperation(
                builder,
                barrierOperation
            );
            return OpenQasm3WriterResult.success("");
        }
        return OpenQasm3WriterResult.failure(IntegrationDiagnostic.error(
            IntegrationDiagnosticCode.UNSUPPORTED_OPERATION,
            "OpenQASM 3 export supports classical control only for gate, measure, reset, and barrier operations."
        ));
    }

    private static OpenQasm3WriterResult appendClassicallyControlledOperation(
        final StringBuilder builder,
        final ClassicallyControlledOperation operation,
        final Map<String, GateDefinition> definitionsByName,
        final String indent
    ) {
        builder.append(indent)
            .append("if(")
            .append(formatClassicalPredicate(operation.predicate()))
            .append(") ");
        final Operation nested = operation.operation();
        if (nested instanceof GateOperation gateOperation) {
            final String gateName = openQasmGateName(
                gateOperation.gate(),
                definitionsByName
            );
            if (gateName == null) {
                return OpenQasm3WriterResult.failure(IntegrationDiagnostic.error(
                    IntegrationDiagnosticCode.UNSUPPORTED_GATE,
                    "OpenQASM 3 export does not support gate: " + gateOperation.gate().gateName() + "."
                ));
            }
            appendGateOperation(
                builder,
                gateName,
                gateOperation
            );
            return OpenQasm3WriterResult.success("");
        }
        if (nested instanceof ResetOperation resetOperation) {
            builder.append("reset ")
                .append(qubitReference(
                    resetOperation.qubitReference()
                ))
                .append(";\n");
            return OpenQasm3WriterResult.success("");
        }
        if (nested instanceof MeasureOperation measureOperation) {
            appendMeasureOperation(
                builder,
                measureOperation
            );
            return OpenQasm3WriterResult.success("");
        }
        if (nested instanceof BarrierOperation barrierOperation) {
            appendBarrierOperation(
                builder,
                barrierOperation
            );
            return OpenQasm3WriterResult.success("");
        }
        return OpenQasm3WriterResult.failure(IntegrationDiagnostic.error(
            IntegrationDiagnosticCode.UNSUPPORTED_OPERATION,
            "OpenQASM 3 export supports classical predicates only for gate, measure, reset, and barrier operations."
        ));
    }

    private static void appendGateOperation(
        final StringBuilder builder,
        final String gateName,
        final GateOperation operation
    ) {
        builder.append(gateName);
        if (operation.parameterCount() > 0) {
            builder.append("(");
            for (int i = 0; i < operation.parameterCount(); i++) {
                if (i > 0) {
                    builder.append(",");
                }
                builder.append(parameterReference(operation.parameter(i)));
            }
            builder.append(")");
        }
        builder.append(" ");
        for (int i = 0; i < operation.qubitCount(); i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append(qubitReference(
                operation.qubitReference(i)
            ));
        }
        builder.append(";\n");
    }

    private static void appendMeasureOperation(
        final StringBuilder builder,
        final MeasureOperation operation
    ) {
        builder.append("measure ")
            .append(qubitReference(
                operation.qubitReference()
            ))
            .append(" -> ")
            .append(classicalBitReference(
                operation.bit()
            ))
            .append(";\n");
    }

    private static void appendBarrierOperation(
        final StringBuilder builder,
        final BarrierOperation operation
    ) {
        builder.append("barrier ");
        for (int i = 0; i < operation.qubitCount(); i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append(qubitReference(
                operation.qubit(i)
            ));
        }
        builder.append(";\n");
    }

    private static void appendClassicalAssignment(
        final StringBuilder builder,
        final ClassicalAssignment assignment,
        final String indent
    ) {
        builder.append(indent)
            .append(formatClassicalExpression(assignment.target()))
            .append(" = ")
            .append(formatClassicalExpression(assignment.value()))
            .append(";\n");
    }

    private static void appendClassicalDeclaration(
        final StringBuilder builder,
        final ClassicalDeclarationOperation operation,
        final String indent
    ) {
        builder.append(indent)
            .append(formatClassicalType(operation.declaration().type()))
            .append(" ")
            .append(operation.declaration().name());
        if (operation.hasInitializer()) {
            builder.append(" = ")
                .append(formatClassicalExpression(operation.initializer()));
        }
        builder.append(";\n");
    }

    private static void appendClassicalArrayDeclaration(
        final StringBuilder builder,
        final ClassicalArrayDeclarationOperation operation,
        final String indent
    ) {
        builder.append(indent)
            .append("array[")
            .append(formatClassicalType(operation.elementType()));
        for (int i = 0; i < operation.dimensionCount(); i++) {
            builder.append(", ")
                .append(formatClassicalExpression(operation.dimension(i)));
        }
        builder.append("] ")
            .append(operation.name());
        if (operation.hasInitializerText()) {
            builder.append(" = ")
                .append(operation.initializerText());
        }
        builder.append(";\n");
    }

    private static void appendCallableInvocation(
        final StringBuilder builder,
        final CallableInvocationOperation operation,
        final String indent
    ) {
        builder.append(indent);
        if (operation.hasTarget()) {
            builder.append(formatClassicalExpression(operation.target()))
                .append(" = ");
        }
        builder.append(operation.callableName())
            .append("(");
        boolean needsComma = false;
        for (int i = 0; i < operation.classicalArguments().size(); i++) {
            if (needsComma) {
                builder.append(", ");
            }
            builder.append(formatClassicalExpression(operation.classicalArguments().get(i)));
            needsComma = true;
        }
        for (int i = 0; i < operation.quantumArguments().size(); i++) {
            if (needsComma) {
                builder.append(", ");
            }
            builder.append(qubitReference(operation.quantumArguments().get(i)));
            needsComma = true;
        }
        builder.append(");\n");
    }

    private static OpenQasm3WriterResult appendConditionalBlockOperation(
        final StringBuilder builder,
        final ConditionalBlockOperation operation,
        final Map<String, GateDefinition> definitionsByName,
        final String indent
    ) {
        builder.append(indent)
            .append("if (")
            .append(formatClassicalPredicate(operation.predicate()))
            .append(") {\n");
        OpenQasm3WriterResult result = appendOperationBlock(
            builder,
            operation.thenBlock(),
            definitionsByName,
            indent + "  "
        );
        if (!result.isSuccess()) {
            return result;
        }
        builder.append(indent)
            .append("}");
        if (operation.hasElseBlock()) {
            builder.append(" else {\n");
            result = appendOperationBlock(
                builder,
                operation.elseBlock(),
                definitionsByName,
                indent + "  "
            );
            if (!result.isSuccess()) {
                return result;
            }
            builder.append(indent)
                .append("}");
        }
        builder.append("\n");
        return OpenQasm3WriterResult.success("");
    }

    private static OpenQasm3WriterResult appendForLoopOperation(
        final StringBuilder builder,
        final ForLoopOperation operation,
        final Map<String, GateDefinition> definitionsByName,
        final String indent
    ) {
        builder.append(indent)
            .append("for ")
            .append(operation.variableName())
            .append(" in [")
            .append(operation.startInclusive())
            .append(":")
            .append(operation.step())
            .append(":")
            .append(operation.endInclusive())
            .append("] {\n");
        final OpenQasm3WriterResult result = appendOperationBlock(
            builder,
            operation.body(),
            definitionsByName,
            indent + "  "
        );
        if (!result.isSuccess()) {
            return result;
        }
        builder.append(indent)
            .append("}\n");
        return OpenQasm3WriterResult.success("");
    }

    private static OpenQasm3WriterResult appendSymbolicForLoopOperation(
        final StringBuilder builder,
        final SymbolicForLoopOperation operation,
        final Map<String, GateDefinition> definitionsByName,
        final String indent
    ) {
        builder.append(indent)
            .append("for ");
        if (operation.hasVariableTypeText()) {
            builder.append(operation.variableTypeText())
                .append(" ");
        }
        builder.append(operation.variableName())
            .append(" in [")
            .append(formatClassicalExpression(operation.startInclusive()))
            .append(":")
            .append(formatClassicalExpression(operation.step()))
            .append(":")
            .append(formatClassicalExpression(operation.endInclusive()))
            .append("] {\n");
        final OpenQasm3WriterResult result = appendOperationBlock(
            builder,
            operation.body(),
            definitionsByName,
            indent + "  "
        );
        if (!result.isSuccess()) {
            return result;
        }
        builder.append(indent)
            .append("}\n");
        return OpenQasm3WriterResult.success("");
    }

    private static OpenQasm3WriterResult appendWhileLoopOperation(
        final StringBuilder builder,
        final WhileLoopOperation operation,
        final Map<String, GateDefinition> definitionsByName,
        final String indent
    ) {
        builder.append(indent)
            .append("while (")
            .append(formatClassicalPredicate(operation.predicate()))
            .append(") {\n");
        final OpenQasm3WriterResult result = appendOperationBlock(
            builder,
            operation.body(),
            definitionsByName,
            indent + "  "
        );
        if (!result.isSuccess()) {
            return result;
        }
        builder.append(indent)
            .append("}\n");
        return OpenQasm3WriterResult.success("");
    }

    private static void appendDelayOperation(
        final StringBuilder builder,
        final DelayOperation operation,
        final String indent
    ) {
        builder.append(indent)
            .append("delay[")
            .append(formatDuration(operation.duration()))
            .append("] ");
        for (int i = 0; i < operation.qubitCount(); i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append(qubitReference(operation.reference(i)));
        }
        builder.append(";\n");
    }

    private static OpenQasm3WriterResult appendTimingBoxOperation(
        final StringBuilder builder,
        final TimingBoxOperation operation,
        final Map<String, GateDefinition> definitionsByName,
        final String indent
    ) {
        builder.append(indent)
            .append("box");
        if (operation.hasDuration()) {
            builder.append("[")
                .append(formatDuration(operation.duration()))
                .append("]");
        }
        builder.append(" {\n");
        final OpenQasm3WriterResult result = appendOperationBlock(
            builder,
            operation.body(),
            definitionsByName,
            indent + "  "
        );
        if (!result.isSuccess()) {
            return result;
        }
        builder.append(indent)
            .append("}\n");
        return OpenQasm3WriterResult.success("");
    }

    private static OpenQasm3WriterResult appendBlock(
        final StringBuilder builder,
        final OperationBlock block,
        final Map<String, GateDefinition> definitionsByName,
        final String indent
    ) {
        builder.append(indent)
            .append("{\n");
        final OpenQasm3WriterResult result = appendOperationBlock(
            builder,
            block,
            definitionsByName,
            indent + "  "
        );
        if (!result.isSuccess()) {
            return result;
        }
        builder.append(indent)
            .append("}\n");
        return OpenQasm3WriterResult.success("");
    }

    private static OpenQasm3WriterResult appendOperationBlock(
        final StringBuilder builder,
        final OperationBlock block,
        final Map<String, GateDefinition> definitionsByName,
        final String indent
    ) {
        for (int i = 0; i < block.operationCount(); i++) {
            final OpenQasm3WriterResult result = appendOperation(
                builder,
                block.operation(i),
                definitionsByName,
                indent
            );
            if (!result.isSuccess()) {
                return result;
            }
        }
        return OpenQasm3WriterResult.success("");
    }

    private static String formatClassicalPredicate(final ClassicalPredicate predicate) {
        if (predicate.kind() == ClassicalPredicateKind.COMPARISON) {
            return formatClassicalExpression(predicate.leftExpression())
                + " "
                + comparisonOperator(predicate.comparisonOperator())
                + " "
                + formatClassicalExpression(predicate.rightExpression());
        }
        if (predicate.kind() == ClassicalPredicateKind.NOT) {
            return "!(" + formatClassicalPredicate(predicate.leftPredicate()) + ")";
        }
        final String operator = predicate.booleanOperator() == ClassicalBooleanOperator.AND
            ? "&&"
            : "||";
        return "("
            + formatClassicalPredicate(predicate.leftPredicate())
            + ") "
            + operator
            + " ("
            + formatClassicalPredicate(predicate.rightPredicate())
            + ")";
    }

    private static String formatClassicalExpression(final ClassicalExpression expression) {
        if (expression.kind() == ClassicalExpressionKind.INTEGER) {
            return Long.toString(expression.integerValue());
        }
        if (expression.kind() == ClassicalExpressionKind.VARIABLE_REFERENCE) {
            return expression.variableName();
        }
        if (expression.kind() == ClassicalExpressionKind.BINARY_OPERATION) {
            return "("
                + formatClassicalExpression(expression.leftExpression())
                + " "
                + binaryOperator(expression.binaryOperator())
                + " "
                + formatClassicalExpression(expression.rightExpression())
                + ")";
        }
        if (expression.kind() == ClassicalExpressionKind.BIT_REFERENCE) {
            return classicalBitReference(expression.bit());
        }
        if (expression.kind() == ClassicalExpressionKind.REGISTER_REFERENCE) {
            return expression.register().name().value();
        }
        if (expression.kind() == ClassicalExpressionKind.SYMBOLIC_REFERENCE) {
            return expression.symbolicText();
        }
        final StringBuilder builder = new StringBuilder(expression.callableName())
            .append("(");
        for (int i = 0; i < expression.callArgumentCount(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(formatClassicalExpression(expression.callArgument(i)));
        }
        return builder.append(")").toString();
    }

    private static String formatClassicalType(final ru.pathcreator.vadim.quantum.domain.classical.ClassicalType type) {
        final String base = switch (type.kind()) {
            case BIT -> "bit";
            case BOOLEAN -> "bool";
            case SIGNED_INTEGER -> "int";
            case UNSIGNED_INTEGER -> "uint";
            case FLOAT -> "float";
            case ANGLE -> "angle";
            case DURATION -> "duration";
            case STRETCH -> "stretch";
        };
        if (type.hasBitWidth()) {
            return base + "[" + type.bitWidth() + "]";
        }
        return base;
    }

    private static String binaryOperator(final ClassicalBinaryOperator operator) {
        return switch (operator) {
            case ADD -> "+";
            case SUBTRACT -> "-";
            case MULTIPLY -> "*";
            case DIVIDE -> "/";
            case MODULO -> "%";
            case BITWISE_AND -> "&";
            case BITWISE_OR -> "|";
            case BITWISE_XOR -> "^";
            case SHIFT_LEFT -> "<<";
            case SHIFT_RIGHT -> ">>";
        };
    }

    private static String comparisonOperator(final ClassicalComparisonOperator operator) {
        if (operator == ClassicalComparisonOperator.EQUAL) {
            return "==";
        }
        if (operator == ClassicalComparisonOperator.NOT_EQUAL) {
            return "!=";
        }
        if (operator == ClassicalComparisonOperator.LESS_THAN) {
            return "<";
        }
        if (operator == ClassicalComparisonOperator.LESS_THAN_OR_EQUAL) {
            return "<=";
        }
        if (operator == ClassicalComparisonOperator.GREATER_THAN) {
            return ">";
        }
        return ">=";
    }

    private static String formatDuration(final DurationExpression duration) {
        if (duration.isStretch()) {
            return duration.symbol();
        }
        if (duration.isExpression()) {
            return duration.expression();
        }
        return formatNumber(duration.value()) + duration.unit().symbol();
    }

    private static String parameterReference(final ParameterExpression parameter) {
        return formatParameter(parameter);
    }

    private static String formatParameter(final ParameterExpression parameter) {
        if (parameter.kind() == ParameterExpressionKind.NUMERIC) {
            return formatNumber(parameter.numericValue());
        }
        if (
            parameter.kind() == ParameterExpressionKind.NAMED
            || parameter.kind() == ParameterExpressionKind.KNOWN_CONSTANT
        ) {
            return parameter.name();
        }
        if (parameter.kind() == ParameterExpressionKind.UNARY) {
            return parameter.unaryOperator().symbol() + parenthesizedParameter(parameter.left());
        }
        return parenthesizedParameter(parameter.left())
            + parameter.binaryOperator().symbol()
            + parenthesizedParameter(parameter.right());
    }

    private static String parenthesizedParameter(final ParameterExpression parameter) {
        if (
            parameter.kind() == ParameterExpressionKind.NUMERIC
            || parameter.kind() == ParameterExpressionKind.NAMED
            || parameter.kind() == ParameterExpressionKind.KNOWN_CONSTANT
        ) {
            return formatParameter(parameter);
        }
        return "(" + formatParameter(parameter) + ")";
    }

    private static String formatNumber(final double value) {
        if (value == Math.rint(value)) {
            return Long.toString(Math.round(value));
        }
        return Double.toString(value);
    }

    private static Map<String, GateDefinition> definitionsByName(final QuantumProgram program) {
        final Map<String, GateDefinition> result = new LinkedHashMap<>();
        for (int i = 0; i < program.gateDefinitionCount(); i++) {
            final GateDefinition definition = program.gateDefinition(i);
            result.put(
                definition.gateName(),
                definition
            );
        }
        return result;
    }

    private static String openQasmGateName(
        final Gate gate,
        final Map<String, GateDefinition> definitionsByName
    ) {
        final String standardName = OpenQasm3GateMapper.toOpenQasmName(gate);
        if (standardName != null) {
            return standardName;
        }
        if (gate instanceof GateDefinition definition) {
            final GateDefinition declaredDefinition = definitionsByName.get(definition.gateName());
            if (declaredDefinition != null) {
                if (declaredDefinition.kind() == GateDefinitionKind.INTRINSIC) {
                    return null;
                }
                return declaredDefinition.gateName();
            }
            if (definition.kind() == GateDefinitionKind.INTRINSIC) {
                return null;
            }
            return definition.gateName();
        }
        return null;
    }

    private static OpenQasm3Condition openQasmCondition(final ClassicalPredicate predicate) {
        if (predicate.kind() != ClassicalPredicateKind.COMPARISON) {
            return null;
        }
        if (predicate.comparisonOperator() != ClassicalComparisonOperator.EQUAL) {
            return null;
        }
        final ClassicalExpression left = predicate.leftExpression();
        final ClassicalExpression right = predicate.rightExpression();
        if (
            left.kind() == ClassicalExpressionKind.REGISTER_REFERENCE
            && right.kind() == ClassicalExpressionKind.INTEGER
        ) {
            return new OpenQasm3Condition(
                left.register().name().value(),
                right.integerValue()
            );
        }
        if (
            left.kind() == ClassicalExpressionKind.INTEGER
            && right.kind() == ClassicalExpressionKind.REGISTER_REFERENCE
        ) {
            return new OpenQasm3Condition(
                right.register().name().value(),
                left.integerValue()
            );
        }
        return null;
    }

    private static String qubitReference(final Qubit qubit) {
        return qubit.register().name().value() + "[" + qubit.index() + "]";
    }

    private static String qubitReference(final QuantumReference reference) {
        if (reference.kind() == QuantumReferenceKind.STATIC_QUBIT) {
            return qubitReference(reference.qubit());
        }
        if (reference.kind() == QuantumReferenceKind.HARDWARE_QUBIT) {
            return "$" + reference.hardwareIndex();
        }
        return reference.register().name().value()
            + "["
            + formatClassicalExpression(reference.indexExpression())
            + "]";
    }

    private static String classicalBitReference(final ClassicalBit bit) {
        return bit.register().name().value() + "[" + bit.index() + "]";
    }

    private record OpenQasm3Condition(
        String registerName,
        long expectedValue
    ) {
    }

    private static int estimatedOutputCapacity(final QuantumCircuit circuit) {
        final long estimated = 256L + (long) circuit.operationCount() * 48L;
        return estimated > Integer.MAX_VALUE
            ? Integer.MAX_VALUE
            : (int) estimated;
    }
}
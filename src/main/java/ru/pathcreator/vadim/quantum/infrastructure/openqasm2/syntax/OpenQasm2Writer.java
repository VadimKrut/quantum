/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.openqasm2.syntax;

import java.util.LinkedHashMap;
import java.util.Map;

import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnostic;
import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnosticCode;
import ru.pathcreator.vadim.quantum.domain.bit.ClassicalBit;
import ru.pathcreator.vadim.quantum.domain.bit.Qubit;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalComparisonOperator;
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
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalAssignmentOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicallyControlledOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ControlledOperation;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.MeasureOperation;
import ru.pathcreator.vadim.quantum.domain.operation.Operation;
import ru.pathcreator.vadim.quantum.domain.operation.ResetOperation;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;
import ru.pathcreator.vadim.quantum.infrastructure.openqasm2.mapping.OpenQasm2GateMapper;

/**
 * Writer OpenQASM 2.0 для поддерживаемой части Quantum IR.
 */
public final class OpenQasm2Writer {

    /**
     * Пишет одну схему в OpenQASM 2.0.
     *
     * @param program схема Quantum IR
     * @return результат writer
     */
    public OpenQasm2WriterResult write(final QuantumProgram program) {
        if (program == null) {
            throw new IllegalArgumentException("Quantum program must not be null.");
        }
        if (program.circuitCount() != 1) {
            return OpenQasm2WriterResult.failure(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.UNSUPPORTED_CIRCUIT_STRUCTURE,
                "OpenQASM 2 writer supports exactly one circuit per program."
            ));
        }
        return write(
            program,
            program.circuit(0)
        );
    }

    private OpenQasm2WriterResult write(
        final QuantumProgram program,
        final QuantumCircuit circuit
    ) {
        if (circuit == null) {
            throw new IllegalArgumentException("Quantum circuit must not be null.");
        }
        final Map<String, GateDefinition> definitionsByName = definitionsByName(program);
        final StringBuilder builder = new StringBuilder();
        builder.append("OPENQASM 2.0;\n");
        builder.append("include \"qelib1.inc\";\n");

        for (int i = 0; i < program.gateDefinitionCount(); i++) {
            final OpenQasm2WriterResult result = appendGateDefinition(
                builder,
                program.gateDefinition(i),
                definitionsByName
            );
            if (!result.isSuccess()) {
                return result;
            }
        }

        for (int i = 0; i < circuit.quantumRegisterCount(); i++) {
            final QuantumRegister register = circuit.quantumRegister(i);
            builder.append("qreg ")
                .append(register.name().value())
                .append("[")
                .append(register.size())
                .append("];\n");
        }
        for (int i = 0; i < circuit.classicalRegisterCount(); i++) {
            final ClassicalRegister register = circuit.classicalRegister(i);
            builder.append("creg ")
                .append(register.name().value())
                .append("[")
                .append(register.size())
                .append("];\n");
        }
        for (int i = 0; i < circuit.operationCount(); i++) {
            final Operation operation = circuit.operation(i);
            if (operation instanceof GateOperation gateOperation) {
                final String gateName = openQasmGateName(
                    gateOperation.gate(),
                    definitionsByName
                );
                if (gateName == null) {
                    return OpenQasm2WriterResult.failure(IntegrationDiagnostic.error(
                        IntegrationDiagnosticCode.UNSUPPORTED_GATE,
                        "OpenQASM 2 export does not support gate: " + gateOperation.gate().gateName() + "."
                    ));
                }
                appendGateOperation(
                    builder,
                    gateName,
                    gateOperation
                );
            } else if (operation instanceof MeasureOperation measureOperation) {
                builder.append("measure ")
                    .append(qubitReference(
                        measureOperation.qubit()
                    ))
                    .append(" -> ")
                    .append(classicalBitReference(
                        measureOperation.bit()
                    ))
                    .append(";\n");
            } else if (operation instanceof ResetOperation resetOperation) {
                builder.append("reset ")
                    .append(qubitReference(
                        resetOperation.qubit()
                    ))
                    .append(";\n");
            } else if (operation instanceof BarrierOperation barrierOperation) {
                appendBarrierOperation(
                    builder,
                    barrierOperation
                );
            } else if (operation instanceof ControlledOperation controlledOperation) {
                final OpenQasm2WriterResult result = appendControlledOperation(
                    builder,
                    controlledOperation,
                    definitionsByName
                );
                if (!result.isSuccess()) {
                    return result;
                }
            } else if (operation instanceof ClassicalAssignmentOperation) {
                return OpenQasm2WriterResult.failure(IntegrationDiagnostic.error(
                    IntegrationDiagnosticCode.UNSUPPORTED_OPERATION,
                    "OpenQASM 2 export does not support classical assignment operations."
                ));
            } else if (operation instanceof ClassicallyControlledOperation controlledOperation) {
                final OpenQasm2WriterResult result = appendClassicallyControlledOperation(
                    builder,
                    controlledOperation,
                    definitionsByName
                );
                if (!result.isSuccess()) {
                    return result;
                }
            } else {
                return OpenQasm2WriterResult.failure(IntegrationDiagnostic.error(
                    IntegrationDiagnosticCode.UNSUPPORTED_OPERATION,
                    "OpenQASM 2 export does not support operation kind: " + operation.kind() + "."
                ));
            }
        }
        return OpenQasm2WriterResult.success(builder.toString());
    }

    private static OpenQasm2WriterResult appendGateDefinition(
        final StringBuilder builder,
        final GateDefinition definition,
        final Map<String, GateDefinition> definitionsByName
    ) {
        if (definition.kind() == GateDefinitionKind.INTRINSIC) {
            return OpenQasm2WriterResult.success("");
        }
        if (definition.kind() == GateDefinitionKind.OPAQUE) {
            builder.append("opaque ")
                .append(definition.gateName());
            appendDeclarationParameters(
                builder,
                definition
            );
            appendDeclarationQubits(
                builder,
                definition
            );
            builder.append(";\n");
            return OpenQasm2WriterResult.success("");
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
            final OpenQasm2WriterResult result = appendGateBodyOperation(
                builder,
                definition.bodyOperations().get(i),
                definitionsByName
            );
            if (!result.isSuccess()) {
                return result;
            }
        }
        builder.append("}\n");
        return OpenQasm2WriterResult.success("");
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

    private static OpenQasm2WriterResult appendGateBodyOperation(
        final StringBuilder builder,
        final GateBodyOperation operation,
        final Map<String, GateDefinition> definitionsByName
    ) {
        final String gateName = openQasmGateName(
            operation.gate(),
            definitionsByName
        );
        if (gateName == null) {
            return OpenQasm2WriterResult.failure(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.UNSUPPORTED_GATE,
                "OpenQASM 2 export does not support gate inside gate definition: "
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
        return OpenQasm2WriterResult.success("");
    }

    private static OpenQasm2WriterResult appendControlledOperation(
        final StringBuilder builder,
        final ControlledOperation operation,
        final Map<String, GateDefinition> definitionsByName
    ) {
        builder.append("if(")
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
                return OpenQasm2WriterResult.failure(IntegrationDiagnostic.error(
                    IntegrationDiagnosticCode.UNSUPPORTED_GATE,
                    "OpenQASM 2 export does not support gate: " + gateOperation.gate().gateName() + "."
                ));
            }
            appendGateOperation(
                builder,
                gateName,
                gateOperation
            );
            return OpenQasm2WriterResult.success("");
        }
        if (nested instanceof ResetOperation resetOperation) {
            builder.append("reset ")
                .append(qubitReference(
                    resetOperation.qubit()
                ))
                .append(";\n");
            return OpenQasm2WriterResult.success("");
        }
        if (nested instanceof MeasureOperation measureOperation) {
            appendMeasureOperation(
                builder,
                measureOperation
            );
            return OpenQasm2WriterResult.success("");
        }
        if (nested instanceof BarrierOperation barrierOperation) {
            appendBarrierOperation(
                builder,
                barrierOperation
            );
            return OpenQasm2WriterResult.success("");
        }
        return OpenQasm2WriterResult.failure(IntegrationDiagnostic.error(
            IntegrationDiagnosticCode.UNSUPPORTED_OPERATION,
            "OpenQASM 2 export supports classical control only for gate, measure, reset, and barrier operations."
        ));
    }

    private static OpenQasm2WriterResult appendClassicallyControlledOperation(
        final StringBuilder builder,
        final ClassicallyControlledOperation operation,
        final Map<String, GateDefinition> definitionsByName
    ) {
        final OpenQasm2Condition condition = openQasmCondition(operation.predicate());
        if (condition == null) {
            return OpenQasm2WriterResult.failure(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.UNSUPPORTED_OPERATION,
                "OpenQASM 2 export supports only register equality predicates."
            ));
        }
        builder.append("if(")
            .append(condition.registerName())
            .append("==")
            .append(condition.expectedValue())
            .append(") ");
        final Operation nested = operation.operation();
        if (nested instanceof GateOperation gateOperation) {
            final String gateName = openQasmGateName(
                gateOperation.gate(),
                definitionsByName
            );
            if (gateName == null) {
                return OpenQasm2WriterResult.failure(IntegrationDiagnostic.error(
                    IntegrationDiagnosticCode.UNSUPPORTED_GATE,
                    "OpenQASM 2 export does not support gate: " + gateOperation.gate().gateName() + "."
                ));
            }
            appendGateOperation(
                builder,
                gateName,
                gateOperation
            );
            return OpenQasm2WriterResult.success("");
        }
        if (nested instanceof ResetOperation resetOperation) {
            builder.append("reset ")
                .append(qubitReference(
                    resetOperation.qubit()
                ))
                .append(";\n");
            return OpenQasm2WriterResult.success("");
        }
        if (nested instanceof MeasureOperation measureOperation) {
            appendMeasureOperation(
                builder,
                measureOperation
            );
            return OpenQasm2WriterResult.success("");
        }
        if (nested instanceof BarrierOperation barrierOperation) {
            appendBarrierOperation(
                builder,
                barrierOperation
            );
            return OpenQasm2WriterResult.success("");
        }
        return OpenQasm2WriterResult.failure(IntegrationDiagnostic.error(
            IntegrationDiagnosticCode.UNSUPPORTED_OPERATION,
            "OpenQASM 2 export supports classical predicates only for gate, measure, reset, and barrier operations."
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
                operation.qubit(i)
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
                operation.qubit()
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
        final String standardName = OpenQasm2GateMapper.toOpenQasmName(gate);
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

    private static OpenQasm2Condition openQasmCondition(final ClassicalPredicate predicate) {
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
            return new OpenQasm2Condition(
                left.register().name().value(),
                right.integerValue()
            );
        }
        if (
            left.kind() == ClassicalExpressionKind.INTEGER
            && right.kind() == ClassicalExpressionKind.REGISTER_REFERENCE
        ) {
            return new OpenQasm2Condition(
                right.register().name().value(),
                left.integerValue()
            );
        }
        return null;
    }

    private static String qubitReference(final Qubit qubit) {
        return qubit.register().name().value() + "[" + qubit.index() + "]";
    }

    private static String classicalBitReference(final ClassicalBit bit) {
        return bit.register().name().value() + "[" + bit.index() + "]";
    }

    private record OpenQasm2Condition(
        String registerName,
        long expectedValue
    ) {
    }
}
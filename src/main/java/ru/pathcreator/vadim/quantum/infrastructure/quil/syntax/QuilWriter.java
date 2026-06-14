/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.quil.syntax;

import java.util.LinkedHashMap;

import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnostic;
import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnosticCode;
import ru.pathcreator.vadim.quantum.domain.bit.ClassicalBit;
import ru.pathcreator.vadim.quantum.domain.bit.Qubit;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpressionKind;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinition;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinitionKind;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.BarrierOperation;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.MeasureOperation;
import ru.pathcreator.vadim.quantum.domain.operation.Operation;
import ru.pathcreator.vadim.quantum.domain.operation.QuantumReference;
import ru.pathcreator.vadim.quantum.domain.operation.QuantumReferenceKind;
import ru.pathcreator.vadim.quantum.domain.operation.ResetOperation;
import ru.pathcreator.vadim.quantum.domain.operation.SourceFragmentOperation;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;
import ru.pathcreator.vadim.quantum.infrastructure.quil.mapping.QuilGateMapper;

/**
 * Writer Quantum IR в canonical Quil.
 */
public final class QuilWriter {

    private static final String SOURCE_FRAGMENT_FORMAT = "quil";

    public QuilWriterResult write(final QuantumProgram program) {
        if (program == null) {
            throw new IllegalArgumentException("Quantum program must not be null.");
        }
        if (program.circuitCount() != 1) {
            return QuilWriterResult.failure(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.UNSUPPORTED_CIRCUIT_STRUCTURE,
                "Quil writer supports exactly one circuit per program."
            ));
        }
        return writeCircuit(
            program,
            program.circuit(0)
        );
    }

    private static QuilWriterResult writeCircuit(
        final QuantumProgram program,
        final QuantumCircuit circuit
    ) {
        final StringBuilder builder = new StringBuilder();
        final QuilWriterResult sourceFragmentsResult = appendProgramSourceFragments(
            builder,
            program
        );
        if (!sourceFragmentsResult.isSuccess()) {
            return sourceFragmentsResult;
        }
        final QuilWriterResult definitionsResult = appendGateDefinitions(
            builder,
            program
        );
        if (!definitionsResult.isSuccess()) {
            return definitionsResult;
        }
        final LinkedHashMap<Qubit, Integer> qubitAddresses = qubitAddresses(circuit);
        final LinkedHashMap<ClassicalBit, String> memoryReferences = memoryReferences(
            circuit,
            builder
        );
        for (int i = 0; i < circuit.operationCount(); i++) {
            final QuilWriterResult result = appendOperation(
                builder,
                circuit.operation(i),
                qubitAddresses,
                memoryReferences
            );
            if (!result.isSuccess()) {
                return result;
            }
        }
        return QuilWriterResult.success(builder.toString());
    }

    private static QuilWriterResult appendProgramSourceFragments(
        final StringBuilder builder,
        final QuantumProgram program
    ) {
        for (int i = 0; i < program.sourceFragmentCount(); i++) {
            if (!SOURCE_FRAGMENT_FORMAT.equals(program.sourceFragment(i).format())) {
                return QuilWriterResult.failure(IntegrationDiagnostic.error(
                    IntegrationDiagnosticCode.UNSUPPORTED_OPERATION,
                    "Quil export does not support source fragment format: "
                        + program.sourceFragment(i).format()
                        + "."
                ));
            }
            builder.append(program.sourceFragment(i).content().stripTrailing())
                .append('\n');
        }
        return QuilWriterResult.success("");
    }

    private static QuilWriterResult appendGateDefinitions(
        final StringBuilder builder,
        final QuantumProgram program
    ) {
        for (int i = 0; i < program.gateDefinitionCount(); i++) {
            final GateDefinition definition = program.gateDefinition(i);
            if (definition.kind() != GateDefinitionKind.MATRIX) {
                continue;
            }
            builder.append("DEFGATE ")
                .append(definition.gateName());
            if (!definition.parameterNames().isEmpty()) {
                builder.append('(');
                for (int j = 0; j < definition.parameterNames().size(); j++) {
                    if (j > 0) {
                        builder.append(',');
                    }
                    builder.append('%')
                        .append(definition.parameterNames().get(j));
                }
                builder.append(')');
            }
            builder.append(":\n");
            for (int row = 0; row < definition.matrix().rowCount(); row++) {
                builder.append("    ");
                for (int column = 0; column < definition.matrix().columnCount(); column++) {
                    if (column > 0) {
                        builder.append(", ");
                    }
                    builder.append(definition.matrix().entry(
                        row,
                        column
                    ));
                }
                builder.append('\n');
            }
        }
        return QuilWriterResult.success("");
    }

    private static LinkedHashMap<Qubit, Integer> qubitAddresses(final QuantumCircuit circuit) {
        final LinkedHashMap<Qubit, Integer> addresses = new LinkedHashMap<>();
        int nextAddress = 0;
        for (int i = 0; i < circuit.quantumRegisterCount(); i++) {
            final QuantumRegister register = circuit.quantumRegister(i);
            for (int j = 0; j < register.size(); j++) {
                addresses.put(
                    register.get(j),
                    Integer.valueOf(nextAddress)
                );
                nextAddress++;
            }
        }
        return addresses;
    }

    private static LinkedHashMap<ClassicalBit, String> memoryReferences(
        final QuantumCircuit circuit,
        final StringBuilder builder
    ) {
        final LinkedHashMap<ClassicalBit, String> references = new LinkedHashMap<>();
        for (int i = 0; i < circuit.classicalRegisterCount(); i++) {
            final ClassicalRegister register = circuit.classicalRegister(i);
            builder.append("DECLARE ")
                .append(register.name().value())
                .append(" BIT[")
                .append(register.size())
                .append("]\n");
            for (int j = 0; j < register.size(); j++) {
                references.put(
                    register.get(j),
                    register.name().value() + "[" + j + "]"
                );
            }
        }
        return references;
    }

    private static QuilWriterResult appendOperation(
        final StringBuilder builder,
        final Operation operation,
        final LinkedHashMap<Qubit, Integer> qubitAddresses,
        final LinkedHashMap<ClassicalBit, String> memoryReferences
    ) {
        if (operation instanceof GateOperation gateOperation) {
            return appendGate(
                builder,
                gateOperation,
                qubitAddresses
            );
        }
        if (operation instanceof MeasureOperation measureOperation) {
            if (isDynamicReference(measureOperation.qubitReference())) {
                return unsupportedDynamicQubitReference();
            }
            builder.append("MEASURE ")
                .append(qubitAddress(
                    qubitAddresses,
                    measureOperation.qubit()
                ))
                .append(' ')
                .append(memoryReferences.get(measureOperation.bit()))
                .append('\n');
            return QuilWriterResult.success("");
        }
        if (operation instanceof ResetOperation resetOperation) {
            if (isDynamicReference(resetOperation.qubitReference())) {
                return unsupportedDynamicQubitReference();
            }
            builder.append("RESET ")
                .append(qubitAddress(
                    qubitAddresses,
                    resetOperation.qubit()
                ))
                .append('\n');
            return QuilWriterResult.success("");
        }
        if (operation instanceof BarrierOperation) {
            return QuilWriterResult.success("");
        }
        if (operation instanceof SourceFragmentOperation fragmentOperation) {
            if (!SOURCE_FRAGMENT_FORMAT.equals(fragmentOperation.fragment().format())) {
                return QuilWriterResult.failure(IntegrationDiagnostic.error(
                    IntegrationDiagnosticCode.UNSUPPORTED_OPERATION,
                    "Quil export does not support source fragment format: " + fragmentOperation.fragment().format() + "."
                ));
            }
            builder.append(fragmentOperation.fragment().content().stripTrailing())
                .append('\n');
            return QuilWriterResult.success("");
        }
        return QuilWriterResult.failure(IntegrationDiagnostic.error(
            IntegrationDiagnosticCode.UNSUPPORTED_OPERATION,
            "Quil export does not support operation kind: " + operation.kind() + "."
        ));
    }

    private static QuilWriterResult appendGate(
        final StringBuilder builder,
        final GateOperation operation,
        final LinkedHashMap<Qubit, Integer> qubitAddresses
    ) {
        final String gateName = QuilGateMapper.toQuilName(operation.gate());
        final String resolvedGateName = gateName == null
            ? operation.gate().gateName()
            : gateName;
        if (resolvedGateName == null) {
            return QuilWriterResult.failure(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.UNSUPPORTED_GATE,
                "Quil export does not support gate: " + operation.gate().gateName() + "."
            ));
        }
        if (hasDynamicQubitReference(operation)) {
            return unsupportedDynamicQubitReference();
        }
        builder.append(resolvedGateName);
        if (operation.parameterCount() > 0) {
            builder.append('(');
            for (int i = 0; i < operation.parameterCount(); i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(formatParameter(operation.parameter(i)));
            }
            builder.append(')');
        }
        for (int i = 0; i < operation.qubitCount(); i++) {
            builder.append(' ')
                .append(qubitAddress(
                    qubitAddresses,
                    operation.qubit(i)
                ));
        }
        builder.append('\n');
        return QuilWriterResult.success("");
    }

    private static boolean hasDynamicQubitReference(final GateOperation operation) {
        for (int i = 0; i < operation.qubitCount(); i++) {
            if (isDynamicReference(operation.qubitReference(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDynamicReference(final QuantumReference reference) {
        return reference.kind() == QuantumReferenceKind.DYNAMIC_REGISTER_INDEX;
    }

    private static QuilWriterResult unsupportedDynamicQubitReference() {
        return QuilWriterResult.failure(IntegrationDiagnostic.error(
            IntegrationDiagnosticCode.UNSUPPORTED_TARGET_CAPABILITY,
            "Quil export does not support dynamic qubit references."
        ));
    }

    private static int qubitAddress(
        final LinkedHashMap<Qubit, Integer> qubitAddresses,
        final Qubit qubit
    ) {
        final Integer address = qubitAddresses.get(qubit);
        if (address == null) {
            throw new IllegalArgumentException("Qubit is not mapped to a Quil address.");
        }
        return address.intValue();
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
            return parameter.unaryOperator().symbol() + "(" + formatParameter(parameter.left()) + ")";
        }
        return "("
            + formatParameter(parameter.left())
            + parameter.binaryOperator().symbol()
            + formatParameter(parameter.right())
            + ")";
    }

    private static String formatNumber(final double value) {
        if (value == Math.rint(value)) {
            return Long.toString(Math.round(value));
        }
        return Double.toString(value);
    }
}
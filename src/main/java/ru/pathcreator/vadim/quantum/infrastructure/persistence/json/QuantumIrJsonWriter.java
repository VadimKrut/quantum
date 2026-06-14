/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.persistence.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import ru.pathcreator.vadim.quantum.application.persistence.diagnostic.PersistenceDiagnostic;
import ru.pathcreator.vadim.quantum.application.persistence.diagnostic.PersistenceDiagnosticCode;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrWriteResult;
import ru.pathcreator.vadim.quantum.domain.bit.ClassicalBit;
import ru.pathcreator.vadim.quantum.domain.bit.Qubit;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalAssignment;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.gate.DistinctQubitsGateValidationRule;
import ru.pathcreator.vadim.quantum.domain.gate.Gate;
import ru.pathcreator.vadim.quantum.domain.gate.GateBodyOperation;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinition;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinitionKind;
import ru.pathcreator.vadim.quantum.domain.gate.GateValidationRule;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.gate.modifier.GateModifier;
import ru.pathcreator.vadim.quantum.domain.gate.modifier.GateModifierKind;
import ru.pathcreator.vadim.quantum.domain.gate.modifier.ModifiedGate;
import ru.pathcreator.vadim.quantum.domain.metadata.ExternalSource;
import ru.pathcreator.vadim.quantum.domain.metadata.OperationMetadata;
import ru.pathcreator.vadim.quantum.domain.metadata.SourceLocation;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.BarrierOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalAssignmentOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalCondition;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicallyControlledOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ControlledOperation;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.MeasureOperation;
import ru.pathcreator.vadim.quantum.domain.operation.Operation;
import ru.pathcreator.vadim.quantum.domain.operation.ResetOperation;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

/**
 * Canonical JSON writer родного формата Quantum IR.
 */
public final class QuantumIrJsonWriter {

    public static final String FORMAT_NAME = "pathcreator.quantum-ir";
    public static final int FORMAT_VERSION = 1;

    private final ObjectMapper objectMapper;

    public QuantumIrJsonWriter() {
        this.objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Записывает программу Quantum IR в canonical JSON.
     *
     * @param program программа
     * @return результат записи
     */
    public QuantumIrWriteResult write(final QuantumProgram program) {
        if (program == null) {
            return QuantumIrWriteResult.failure(List.of(PersistenceDiagnostic.error(
                PersistenceDiagnosticCode.NULL_INPUT,
                "Quantum program must not be null."
            )));
        }
        final ArrayList<PersistenceDiagnostic> diagnostics = new ArrayList<>();
        final LinkedHashMap<String, Object> root = new LinkedHashMap<>();
        root.put(
            "format",
            FORMAT_NAME
        );
        root.put(
            "version",
            FORMAT_VERSION
        );
        root.put(
            "program",
            writeProgram(
                program,
                diagnostics
            )
        );
        if (containsErrors(diagnostics)) {
            return QuantumIrWriteResult.failure(diagnostics);
        }
        try {
            return QuantumIrWriteResult.success(
                objectMapper.writeValueAsString(root),
                diagnostics
            );
        } catch (final JsonProcessingException exception) {
            return QuantumIrWriteResult.failure(List.of(PersistenceDiagnostic.error(
                PersistenceDiagnosticCode.INVALID_STRUCTURE,
                "Quantum IR JSON could not be generated: " + exception.getMessage()
            )));
        }
    }

    private static LinkedHashMap<String, Object> writeProgram(
        final QuantumProgram program,
        final ArrayList<PersistenceDiagnostic> diagnostics
    ) {
        final LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put(
            "computationModel",
            program.computationModel().name()
        );
        final ArrayList<Object> gateDefinitions = new ArrayList<>();
        for (int i = 0; i < program.gateDefinitionCount(); i++) {
            gateDefinitions.add(writeGateDefinition(
                program.gateDefinition(i),
                diagnostics
            ));
        }
        json.put(
            "gateDefinitions",
            gateDefinitions
        );
        final ArrayList<Object> circuits = new ArrayList<>();
        for (int i = 0; i < program.circuitCount(); i++) {
            circuits.add(writeCircuit(
                program.circuit(i),
                diagnostics
            ));
        }
        json.put(
            "circuits",
            circuits
        );
        return json;
    }

    private static LinkedHashMap<String, Object> writeCircuit(
        final QuantumCircuit circuit,
        final ArrayList<PersistenceDiagnostic> diagnostics
    ) {
        final LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put(
            "name",
            circuit.name().value()
        );
        final ArrayList<Object> quantumRegisters = new ArrayList<>();
        for (int i = 0; i < circuit.quantumRegisterCount(); i++) {
            final QuantumRegister register = circuit.quantumRegister(i);
            quantumRegisters.add(writeRegister(
                register.name().value(),
                register.size()
            ));
        }
        json.put(
            "quantumRegisters",
            quantumRegisters
        );
        final ArrayList<Object> classicalRegisters = new ArrayList<>();
        for (int i = 0; i < circuit.classicalRegisterCount(); i++) {
            final ClassicalRegister register = circuit.classicalRegister(i);
            classicalRegisters.add(writeRegister(
                register.name().value(),
                register.size()
            ));
        }
        json.put(
            "classicalRegisters",
            classicalRegisters
        );
        final ArrayList<Object> operations = new ArrayList<>();
        for (int i = 0; i < circuit.operationCount(); i++) {
            operations.add(writeOperation(
                circuit.operation(i),
                circuit.operationMetadata(i),
                diagnostics
            ));
        }
        json.put(
            "operations",
            operations
        );
        return json;
    }

    private static LinkedHashMap<String, Object> writeRegister(
        final String name,
        final int size
    ) {
        final LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put(
            "name",
            name
        );
        json.put(
            "size",
            size
        );
        return json;
    }

    private static LinkedHashMap<String, Object> writeGateDefinition(
        final GateDefinition definition,
        final ArrayList<PersistenceDiagnostic> diagnostics
    ) {
        final LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put(
            "name",
            definition.gateName()
        );
        json.put(
            "kind",
            definition.kind().name()
        );
        json.put(
            "arity",
            definition.arity()
        );
        json.put(
            "parameterCount",
            definition.parameterCount()
        );
        json.put(
            "validationRules",
            writeValidationRules(
                definition.validationRules(),
                diagnostics,
                definition.gateName()
            )
        );
        json.put(
            "parameterNames",
            new ArrayList<>(definition.parameterNames())
        );
        json.put(
            "qubitNames",
            new ArrayList<>(definition.qubitNames())
        );
        final ArrayList<Object> bodyOperations = new ArrayList<>();
        for (int i = 0; i < definition.bodyOperations().size(); i++) {
            bodyOperations.add(writeGateBodyOperation(
                definition.bodyOperations().get(i),
                diagnostics
            ));
        }
        json.put(
            "bodyOperations",
            bodyOperations
        );
        return json;
    }

    private static ArrayList<Object> writeValidationRules(
        final List<GateValidationRule> rules,
        final ArrayList<PersistenceDiagnostic> diagnostics,
        final String gateName
    ) {
        final ArrayList<Object> json = new ArrayList<>();
        for (int i = 0; i < rules.size(); i++) {
            final GateValidationRule rule = rules.get(i);
            if (rule == DistinctQubitsGateValidationRule.INSTANCE) {
                json.add("DISTINCT_QUBITS");
            } else {
                diagnostics.add(PersistenceDiagnostic.error(
                    PersistenceDiagnosticCode.UNSUPPORTED_MODEL_FEATURE,
                    "Gate validation rule is not portable to Quantum IR JSON for gate: " + gateName + "."
                ));
            }
        }
        return json;
    }

    private static LinkedHashMap<String, Object> writeGateBodyOperation(
        final GateBodyOperation operation,
        final ArrayList<PersistenceDiagnostic> diagnostics
    ) {
        final LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put(
            "gate",
            writeGate(
                operation.gate(),
                diagnostics
            )
        );
        final ArrayList<Object> parameters = new ArrayList<>();
        for (int i = 0; i < operation.parameterCount(); i++) {
            parameters.add(writeParameterExpression(operation.parameter(i)));
        }
        json.put(
            "parameters",
            parameters
        );
        final ArrayList<Object> qubits = new ArrayList<>();
        for (int i = 0; i < operation.qubitCount(); i++) {
            qubits.add(operation.qubitName(i));
        }
        json.put(
            "qubits",
            qubits
        );
        return json;
    }

    private static LinkedHashMap<String, Object> writeOperation(
        final Operation operation,
        final OperationMetadata metadata,
        final ArrayList<PersistenceDiagnostic> diagnostics
    ) {
        final LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put(
            "kind",
            operation.kind().name()
        );
        if (operation instanceof GateOperation gateOperation) {
            json.put(
                "gate",
                writeGate(
                    gateOperation.gate(),
                    diagnostics
                )
            );
            json.put(
                "parameters",
                writeParameterExpressions(gateOperation.parameters())
            );
            json.put(
                "qubits",
                writeQubits(gateOperation.qubits())
            );
        } else if (operation instanceof MeasureOperation measureOperation) {
            json.put(
                "qubit",
                writeQubit(measureOperation.qubit())
            );
            json.put(
                "bit",
                writeClassicalBit(measureOperation.bit())
            );
        } else if (operation instanceof ResetOperation resetOperation) {
            json.put(
                "qubit",
                writeQubit(resetOperation.qubit())
            );
        } else if (operation instanceof BarrierOperation barrierOperation) {
            json.put(
                "qubits",
                writeQubits(barrierOperation.qubits())
            );
        } else if (operation instanceof ControlledOperation controlledOperation) {
            json.put(
                "condition",
                writeClassicalCondition(controlledOperation.condition())
            );
            json.put(
                "operation",
                writeOperation(
                    controlledOperation.operation(),
                    OperationMetadata.empty(),
                    diagnostics
                )
            );
        } else if (operation instanceof ClassicalAssignmentOperation assignmentOperation) {
            json.put(
                "assignment",
                writeClassicalAssignment(assignmentOperation.assignment())
            );
        } else if (operation instanceof ClassicallyControlledOperation controlledOperation) {
            json.put(
                "predicate",
                writeClassicalPredicate(controlledOperation.predicate())
            );
            json.put(
                "operation",
                writeOperation(
                    controlledOperation.operation(),
                    OperationMetadata.empty(),
                    diagnostics
                )
            );
        } else {
            diagnostics.add(PersistenceDiagnostic.error(
                PersistenceDiagnosticCode.UNSUPPORTED_MODEL_FEATURE,
                "Operation type is not supported by Quantum IR JSON: " + operation.getClass().getName() + "."
            ));
        }
        if (
            metadata != null
            && !metadata.isEmpty()
        ) {
            json.put(
                "metadata",
                writeMetadata(metadata)
            );
        }
        return json;
    }

    private static LinkedHashMap<String, Object> writeGate(
        final Gate gate,
        final ArrayList<PersistenceDiagnostic> diagnostics
    ) {
        final LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        if (gate instanceof StandardGate standardGate) {
            json.put(
                "kind",
                "STANDARD"
            );
            json.put(
                "name",
                standardGate.name()
            );
        } else if (gate instanceof ModifiedGate modifiedGate) {
            json.put(
                "kind",
                "MODIFIED"
            );
            json.put(
                "baseGate",
                writeGate(
                    modifiedGate.baseGate(),
                    diagnostics
                )
            );
            final ArrayList<Object> modifiers = new ArrayList<>();
            for (int i = 0; i < modifiedGate.modifiers().size(); i++) {
                modifiers.add(writeGateModifier(modifiedGate.modifiers().get(i)));
            }
            json.put(
                "modifiers",
                modifiers
            );
        } else if (gate instanceof GateDefinition definition) {
            json.put(
                "kind",
                "GATE_DEFINITION"
            );
            json.put(
                "name",
                definition.gateName()
            );
        } else {
            diagnostics.add(PersistenceDiagnostic.error(
                PersistenceDiagnosticCode.UNSUPPORTED_MODEL_FEATURE,
                "Gate type is not supported by Quantum IR JSON: " + gate.getClass().getName() + "."
            ));
        }
        return json;
    }

    private static LinkedHashMap<String, Object> writeGateModifier(final GateModifier modifier) {
        final LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put(
            "kind",
            modifier.kind().name()
        );
        if (
            modifier.kind() == GateModifierKind.CONTROLLED
            || modifier.kind() == GateModifierKind.REPEAT
        ) {
            json.put(
                "integerValue",
                modifier.integerValue()
            );
        } else if (modifier.kind() == GateModifierKind.POWER) {
            json.put(
                "doubleValue",
                modifier.doubleValue()
            );
        } else if (modifier.kind() == GateModifierKind.ANNOTATION) {
            json.put(
                "annotationName",
                modifier.annotationName()
            );
        }
        return json;
    }

    private static ArrayList<Object> writeParameterExpressions(final ParameterExpression[] expressions) {
        final ArrayList<Object> json = new ArrayList<>();
        for (int i = 0; i < expressions.length; i++) {
            json.add(writeParameterExpression(expressions[i]));
        }
        return json;
    }

    private static LinkedHashMap<String, Object> writeParameterExpression(final ParameterExpression expression) {
        final LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put(
            "kind",
            expression.kind().name()
        );
        if (expression.isNumeric()) {
            json.put(
                "value",
                expression.numericValue()
            );
        } else if (
            expression.isNamed()
            || expression.isKnownConstant()
        ) {
            json.put(
                "name",
                expression.name()
            );
        } else if (expression.isUnary()) {
            json.put(
                "operator",
                expression.unaryOperator().name()
            );
            json.put(
                "operand",
                writeParameterExpression(expression.left())
            );
        } else if (expression.isBinary()) {
            json.put(
                "operator",
                expression.binaryOperator().name()
            );
            json.put(
                "left",
                writeParameterExpression(expression.left())
            );
            json.put(
                "right",
                writeParameterExpression(expression.right())
            );
        }
        return json;
    }

    private static LinkedHashMap<String, Object> writeClassicalCondition(final ClassicalCondition condition) {
        final LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put(
            "register",
            condition.register().name().value()
        );
        json.put(
            "expectedValue",
            condition.expectedValue()
        );
        return json;
    }

    private static LinkedHashMap<String, Object> writeClassicalAssignment(final ClassicalAssignment assignment) {
        final LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put(
            "target",
            writeClassicalExpression(assignment.target())
        );
        json.put(
            "value",
            writeClassicalExpression(assignment.value())
        );
        return json;
    }

    private static LinkedHashMap<String, Object> writeClassicalExpression(final ClassicalExpression expression) {
        final LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put(
            "kind",
            expression.kind().name()
        );
        switch (expression.kind()) {
            case INTEGER -> json.put(
                "value",
                expression.integerValue()
            );
            case BIT_REFERENCE -> json.put(
                "bit",
                writeClassicalBit(expression.bit())
            );
            case REGISTER_REFERENCE -> json.put(
                "register",
                expression.register().name().value()
            );
            default -> throw new IllegalStateException("Unsupported classical expression kind.");
        }
        return json;
    }

    private static LinkedHashMap<String, Object> writeClassicalPredicate(final ClassicalPredicate predicate) {
        final LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put(
            "kind",
            predicate.kind().name()
        );
        switch (predicate.kind()) {
            case COMPARISON -> {
                json.put(
                    "left",
                    writeClassicalExpression(predicate.leftExpression())
                );
                json.put(
                    "operator",
                    predicate.comparisonOperator().name()
                );
                json.put(
                    "right",
                    writeClassicalExpression(predicate.rightExpression())
                );
            }
            case NOT -> json.put(
                "predicate",
                writeClassicalPredicate(predicate.leftPredicate())
            );
            case BOOLEAN -> {
                json.put(
                    "left",
                    writeClassicalPredicate(predicate.leftPredicate())
                );
                json.put(
                    "operator",
                    predicate.booleanOperator().name()
                );
                json.put(
                    "right",
                    writeClassicalPredicate(predicate.rightPredicate())
                );
            }
            default -> throw new IllegalStateException("Unsupported classical predicate kind.");
        }
        return json;
    }

    private static LinkedHashMap<String, Object> writeMetadata(final OperationMetadata metadata) {
        final LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        if (metadata.source() != null) {
            final ExternalSource source = metadata.source();
            final LinkedHashMap<String, Object> sourceJson = new LinkedHashMap<>();
            sourceJson.put(
                "format",
                source.format()
            );
            sourceJson.put(
                "description",
                source.description()
            );
            json.put(
                "source",
                sourceJson
            );
        }
        if (metadata.location() != null) {
            final SourceLocation location = metadata.location();
            final LinkedHashMap<String, Object> locationJson = new LinkedHashMap<>();
            locationJson.put(
                "line",
                location.line()
            );
            locationJson.put(
                "column",
                location.column()
            );
            json.put(
                "location",
                locationJson
            );
        }
        return json;
    }

    private static ArrayList<Object> writeQubits(final Qubit[] qubits) {
        final ArrayList<Object> json = new ArrayList<>();
        for (int i = 0; i < qubits.length; i++) {
            json.add(writeQubit(qubits[i]));
        }
        return json;
    }

    private static String writeQubit(final Qubit qubit) {
        return qubit.register().name().value() + "[" + qubit.index() + "]";
    }

    private static String writeClassicalBit(final ClassicalBit bit) {
        return bit.register().name().value() + "[" + bit.index() + "]";
    }

    private static boolean containsErrors(final List<PersistenceDiagnostic> diagnostics) {
        for (int i = 0; i < diagnostics.size(); i++) {
            if (diagnostics.get(i).isError()) {
                return true;
            }
        }
        return false;
    }
}
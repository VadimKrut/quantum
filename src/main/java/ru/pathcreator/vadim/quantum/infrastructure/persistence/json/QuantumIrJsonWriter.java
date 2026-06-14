/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.persistence.json;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import ru.pathcreator.vadim.quantum.application.persistence.diagnostic.PersistenceDiagnostic;
import ru.pathcreator.vadim.quantum.application.persistence.diagnostic.PersistenceDiagnosticCode;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrFileWriteResult;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrWriteResult;
import ru.pathcreator.vadim.quantum.domain.bit.ClassicalBit;
import ru.pathcreator.vadim.quantum.domain.bit.Qubit;
import ru.pathcreator.vadim.quantum.domain.calibration.CalibrationDefinition;
import ru.pathcreator.vadim.quantum.domain.callable.CallableArgument;
import ru.pathcreator.vadim.quantum.domain.callable.CallableDefinition;
import ru.pathcreator.vadim.quantum.domain.callable.ExternalCallableDeclaration;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableBarrierOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableBlockOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableClassicalAssignment;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableClassicalAssignmentOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableConditionalBlockOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableDelayOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableForLoopOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableGateOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableMeasureOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableOperationBlock;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableResetOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableTimingBoxOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableWhileLoopOperation;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalAssignment;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalBinaryOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalDeclaration;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalType;
import ru.pathcreator.vadim.quantum.domain.gate.DistinctQubitsGateValidationRule;
import ru.pathcreator.vadim.quantum.domain.gate.Gate;
import ru.pathcreator.vadim.quantum.domain.gate.GateBodyOperation;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinition;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinitionKind;
import ru.pathcreator.vadim.quantum.domain.gate.GateMatrix;
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
import ru.pathcreator.vadim.quantum.domain.operation.LabelOperation;
import ru.pathcreator.vadim.quantum.domain.operation.MeasureOperation;
import ru.pathcreator.vadim.quantum.domain.operation.Operation;
import ru.pathcreator.vadim.quantum.domain.operation.OperationKind;
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

    /**
     * Потоково записывает Quantum IR в JSON-файл, не создавая полный JSON-текст в heap.
     *
     * @param path путь к JSON-файлу
     * @param program программа
     * @return результат записи файла
     */
    public QuantumIrFileWriteResult writeStreaming(
        final Path path,
        final QuantumProgram program
    ) {
        if (path == null) {
            return QuantumIrFileWriteResult.failure(List.of(PersistenceDiagnostic.error(
                PersistenceDiagnosticCode.NULL_INPUT,
                "Quantum IR JSON path must not be null."
            )));
        }
        if (program == null) {
            return QuantumIrFileWriteResult.failure(List.of(PersistenceDiagnostic.error(
                PersistenceDiagnosticCode.NULL_INPUT,
                "Quantum program must not be null."
            )));
        }
        final ArrayList<PersistenceDiagnostic> diagnostics = new ArrayList<>();
        final Path parent = path.getParent();
        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (
                BufferedWriter writer = Files.newBufferedWriter(
                    path,
                    StandardCharsets.UTF_8
                );
                JsonGenerator generator = objectMapper.getFactory().createGenerator(writer)
            ) {
                generator.useDefaultPrettyPrinter();
                writeStreamingRoot(
                    generator,
                    program,
                    diagnostics
                );
            }
        } catch (final IOException exception) {
            return QuantumIrFileWriteResult.failure(List.of(PersistenceDiagnostic.error(
                PersistenceDiagnosticCode.IO_ERROR,
                "Quantum IR JSON file could not be written: " + exception.getMessage()
            )));
        }
        if (containsErrors(diagnostics)) {
            return QuantumIrFileWriteResult.failure(diagnostics);
        }
        return QuantumIrFileWriteResult.success(
            path,
            diagnostics
        );
    }

    private void writeStreamingRoot(
        final JsonGenerator generator,
        final QuantumProgram program,
        final ArrayList<PersistenceDiagnostic> diagnostics
    ) throws IOException {
        generator.writeStartObject();
        generator.writeStringField(
            "format",
            FORMAT_NAME
        );
        generator.writeNumberField(
            "version",
            FORMAT_VERSION
        );
        generator.writeFieldName("program");
        writeStreamingProgram(
            generator,
            program,
            diagnostics
        );
        generator.writeEndObject();
    }

    private void writeStreamingProgram(
        final JsonGenerator generator,
        final QuantumProgram program,
        final ArrayList<PersistenceDiagnostic> diagnostics
    ) throws IOException {
        generator.writeStartObject();
        generator.writeStringField(
            "computationModel",
            program.computationModel().name()
        );
        writeObjectArrayField(
            generator,
            "gateDefinitions",
            program.gateDefinitionCount(),
            index -> writeGateDefinition(
                program.gateDefinition(index),
                diagnostics
            )
        );
        writeObjectArrayField(
            generator,
            "classicalDeclarations",
            program.classicalDeclarationCount(),
            index -> writeClassicalDeclaration(program.classicalDeclaration(index))
        );
        writeObjectArrayField(
            generator,
            "callableDefinitions",
            program.callableDefinitionCount(),
            index -> writeCallableDefinition(
                program.callableDefinition(index),
                diagnostics
            )
        );
        writeObjectArrayField(
            generator,
            "externalCallableDeclarations",
            program.externalCallableDeclarationCount(),
            index -> writeExternalCallableDeclaration(program.externalCallableDeclaration(index))
        );
        writeObjectArrayField(
            generator,
            "calibrationDefinitions",
            program.calibrationDefinitionCount(),
            index -> writeCalibrationDefinition(program.calibrationDefinition(index))
        );
        generator.writeFieldName("circuits");
        generator.writeStartArray();
        for (int i = 0; i < program.circuitCount(); i++) {
            writeStreamingCircuit(
                generator,
                program.circuit(i),
                diagnostics
            );
        }
        generator.writeEndArray();
        generator.writeEndObject();
    }

    private void writeStreamingCircuit(
        final JsonGenerator generator,
        final QuantumCircuit circuit,
        final ArrayList<PersistenceDiagnostic> diagnostics
    ) throws IOException {
        generator.writeStartObject();
        generator.writeStringField(
            "name",
            circuit.name().value()
        );
        generator.writeFieldName("quantumRegisters");
        generator.writeStartArray();
        for (int i = 0; i < circuit.quantumRegisterCount(); i++) {
            final QuantumRegister register = circuit.quantumRegister(i);
            objectMapper.writeValue(
                generator,
                writeRegister(
                    register.name().value(),
                    register.size()
                )
            );
        }
        generator.writeEndArray();
        generator.writeFieldName("classicalRegisters");
        generator.writeStartArray();
        for (int i = 0; i < circuit.classicalRegisterCount(); i++) {
            final ClassicalRegister register = circuit.classicalRegister(i);
            objectMapper.writeValue(
                generator,
                writeRegister(
                    register.name().value(),
                    register.size()
                )
            );
        }
        generator.writeEndArray();
        generator.writeFieldName("operations");
        generator.writeStartArray();
        for (int i = 0; i < circuit.operationCount(); i++) {
            objectMapper.writeValue(
                generator,
                writeOperation(
                    circuit.operation(i),
                    circuit.operationMetadata(i),
                    diagnostics
                )
            );
        }
        generator.writeEndArray();
        generator.writeEndObject();
    }

    private void writeObjectArrayField(
        final JsonGenerator generator,
        final String fieldName,
        final int count,
        final JsonObjectFactory factory
    ) throws IOException {
        generator.writeFieldName(fieldName);
        generator.writeStartArray();
        for (int i = 0; i < count; i++) {
            objectMapper.writeValue(
                generator,
                factory.create(i)
            );
        }
        generator.writeEndArray();
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
        final ArrayList<Object> classicalDeclarations = new ArrayList<>();
        for (int i = 0; i < program.classicalDeclarationCount(); i++) {
            classicalDeclarations.add(writeClassicalDeclaration(program.classicalDeclaration(i)));
        }
        json.put(
            "classicalDeclarations",
            classicalDeclarations
        );
        final ArrayList<Object> callableDefinitions = new ArrayList<>();
        for (int i = 0; i < program.callableDefinitionCount(); i++) {
            callableDefinitions.add(writeCallableDefinition(
                program.callableDefinition(i),
                diagnostics
            ));
        }
        json.put(
            "callableDefinitions",
            callableDefinitions
        );
        final ArrayList<Object> externalCallableDeclarations = new ArrayList<>();
        for (int i = 0; i < program.externalCallableDeclarationCount(); i++) {
            externalCallableDeclarations.add(writeExternalCallableDeclaration(program.externalCallableDeclaration(i)));
        }
        json.put(
            "externalCallableDeclarations",
            externalCallableDeclarations
        );
        final ArrayList<Object> calibrationDefinitions = new ArrayList<>();
        for (int i = 0; i < program.calibrationDefinitionCount(); i++) {
            calibrationDefinitions.add(writeCalibrationDefinition(program.calibrationDefinition(i)));
        }
        json.put(
            "calibrationDefinitions",
            calibrationDefinitions
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

    private static LinkedHashMap<String, Object> writeClassicalDeclaration(final ClassicalDeclaration declaration) {
        final LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put(
            "name",
            declaration.name()
        );
        json.put(
            "type",
            writeClassicalType(declaration.type())
        );
        return json;
    }

    private static LinkedHashMap<String, Object> writeClassicalType(final ClassicalType type) {
        final LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put(
            "kind",
            type.kind().name()
        );
        if (type.hasBitWidth()) {
            json.put(
                "bitWidth",
                type.bitWidth()
            );
        }
        return json;
    }

    private static LinkedHashMap<String, Object> writeCallableDefinition(
        final CallableDefinition definition,
        final ArrayList<PersistenceDiagnostic> diagnostics
    ) {
        final LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put(
            "name",
            definition.name()
        );
        json.put(
            "arguments",
            writeCallableArguments(definition.arguments())
        );
        json.put(
            "body",
            writeCallableOperationBlock(
                definition.body(),
                diagnostics
            )
        );
        return json;
    }

    private static ArrayList<Object> writeCallableOperationBlock(
        final CallableOperationBlock block,
        final ArrayList<PersistenceDiagnostic> diagnostics
    ) {
        final ArrayList<Object> operations = new ArrayList<>();
        for (int i = 0; i < block.operationCount(); i++) {
            operations.add(writeCallableOperation(
                block.operation(i),
                diagnostics
            ));
        }
        return operations;
    }

    private static LinkedHashMap<String, Object> writeCallableOperation(
        final CallableOperation operation,
        final ArrayList<PersistenceDiagnostic> diagnostics
    ) {
        final LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put(
            "kind",
            operation.kind().name()
        );
        if (operation instanceof CallableGateOperation gateOperation) {
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
                writeTextArray(gateOperation.qubitNames())
            );
        } else if (operation instanceof CallableMeasureOperation measureOperation) {
            json.put(
                "qubit",
                measureOperation.qubitName()
            );
            json.put(
                "bit",
                measureOperation.classicalName()
            );
        } else if (operation instanceof CallableResetOperation resetOperation) {
            json.put(
                "qubit",
                resetOperation.qubitName()
            );
        } else if (operation instanceof CallableBarrierOperation barrierOperation) {
            json.put(
                "qubits",
                writeTextArray(barrierOperation.qubitNames())
            );
        } else if (operation instanceof CallableClassicalAssignmentOperation assignmentOperation) {
            json.put(
                "assignment",
                writeCallableClassicalAssignment(assignmentOperation.assignment())
            );
        } else if (operation instanceof CallableBlockOperation blockOperation) {
            json.put(
                "body",
                writeCallableOperationBlock(
                    blockOperation.body(),
                    diagnostics
                )
            );
        } else if (operation instanceof CallableConditionalBlockOperation conditionalOperation) {
            json.put(
                "predicate",
                writeCallableClassicalPredicate(conditionalOperation.predicate())
            );
            json.put(
                "then",
                writeCallableOperationBlock(
                    conditionalOperation.thenBlock(),
                    diagnostics
                )
            );
            if (conditionalOperation.hasElseBlock()) {
                json.put(
                    "else",
                    writeCallableOperationBlock(
                        conditionalOperation.elseBlock(),
                        diagnostics
                    )
                );
            }
        } else if (operation instanceof CallableForLoopOperation loopOperation) {
            json.put(
                "variable",
                loopOperation.variableName()
            );
            json.put(
                "startInclusive",
                loopOperation.startInclusive()
            );
            json.put(
                "step",
                loopOperation.step()
            );
            json.put(
                "endInclusive",
                loopOperation.endInclusive()
            );
            json.put(
                "body",
                writeCallableOperationBlock(
                    loopOperation.body(),
                    diagnostics
                )
            );
        } else if (operation instanceof CallableWhileLoopOperation loopOperation) {
            json.put(
                "predicate",
                writeCallableClassicalPredicate(loopOperation.predicate())
            );
            json.put(
                "body",
                writeCallableOperationBlock(
                    loopOperation.body(),
                    diagnostics
                )
            );
        } else if (operation instanceof CallableDelayOperation delayOperation) {
            json.put(
                "duration",
                writeDuration(delayOperation.duration())
            );
            json.put(
                "qubits",
                writeTextArray(delayOperation.qubitNames())
            );
        } else if (operation instanceof CallableTimingBoxOperation boxOperation) {
            if (boxOperation.hasDuration()) {
                json.put(
                    "duration",
                    writeDuration(boxOperation.duration())
                );
            }
            json.put(
                "body",
                writeCallableOperationBlock(
                    boxOperation.body(),
                    diagnostics
                )
            );
        } else {
            diagnostics.add(PersistenceDiagnostic.error(
                PersistenceDiagnosticCode.UNSUPPORTED_MODEL_FEATURE,
                "Callable operation type is not supported by Quantum IR JSON: " + operation.getClass().getName() + "."
            ));
        }
        return json;
    }

    private static LinkedHashMap<String, Object> writeCallableClassicalAssignment(
        final CallableClassicalAssignment assignment
    ) {
        final LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put(
            "target",
            writeCallableClassicalExpression(assignment.target())
        );
        json.put(
            "value",
            writeCallableClassicalExpression(assignment.value())
        );
        return json;
    }

    private static LinkedHashMap<String, Object> writeCallableClassicalExpression(
        final CallableClassicalExpression expression
    ) {
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
            case ARGUMENT_REFERENCE -> json.put(
                "argument",
                expression.argumentName()
            );
            default -> throw new IllegalStateException("Unsupported callable classical expression kind.");
        }
        return json;
    }

    private static LinkedHashMap<String, Object> writeCallableClassicalPredicate(
        final CallableClassicalPredicate predicate
    ) {
        final LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put(
            "kind",
            predicate.kind().name()
        );
        switch (predicate.kind()) {
            case COMPARISON -> {
                json.put(
                    "left",
                    writeCallableClassicalExpression(predicate.leftExpression())
                );
                json.put(
                    "operator",
                    predicate.comparisonOperator().name()
                );
                json.put(
                    "right",
                    writeCallableClassicalExpression(predicate.rightExpression())
                );
            }
            case NOT -> json.put(
                "predicate",
                writeCallableClassicalPredicate(predicate.leftPredicate())
            );
            case BOOLEAN -> {
                json.put(
                    "left",
                    writeCallableClassicalPredicate(predicate.leftPredicate())
                );
                json.put(
                    "operator",
                    predicate.booleanOperator().name()
                );
                json.put(
                    "right",
                    writeCallableClassicalPredicate(predicate.rightPredicate())
                );
            }
            default -> throw new IllegalStateException("Unsupported callable classical predicate kind.");
        }
        return json;
    }

    private static ArrayList<Object> writeTextArray(final String[] values) {
        final ArrayList<Object> json = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            json.add(values[i]);
        }
        return json;
    }

    private static LinkedHashMap<String, Object> writeExternalCallableDeclaration(
        final ExternalCallableDeclaration declaration
    ) {
        final LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put(
            "name",
            declaration.name()
        );
        json.put(
            "arguments",
            writeCallableArguments(declaration.arguments())
        );
        if (declaration.hasReturnType()) {
            json.put(
                "returnType",
                writeClassicalType(declaration.returnType())
            );
        }
        return json;
    }

    private static ArrayList<Object> writeCallableArguments(final List<CallableArgument> arguments) {
        final ArrayList<Object> json = new ArrayList<>();
        for (int i = 0; i < arguments.size(); i++) {
            final CallableArgument argument = arguments.get(i);
            final LinkedHashMap<String, Object> item = new LinkedHashMap<>();
            item.put(
                "name",
                argument.name()
            );
            item.put(
                "kind",
                argument.kind().name()
            );
            if (argument.kind().name().equals("CLASSICAL")) {
                item.put(
                    "classicalType",
                    writeClassicalType(argument.classicalType())
                );
            }
            json.add(item);
        }
        return json;
    }

    private static LinkedHashMap<String, Object> writeCalibrationDefinition(final CalibrationDefinition definition) {
        final LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put(
            "targetName",
            definition.targetName()
        );
        json.put(
            "parameterNames",
            new ArrayList<>(definition.parameterNames())
        );
        json.put(
            "qubitNames",
            new ArrayList<>(definition.qubitNames())
        );
        json.put(
            "bodyLanguage",
            definition.bodyLanguage()
        );
        json.put(
            "body",
            definition.body()
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
        if (definition.kind() == GateDefinitionKind.MATRIX) {
            json.put(
                "matrix",
                writeGateMatrix(definition.matrix())
            );
        }
        return json;
    }

    private static ArrayList<Object> writeGateMatrix(final GateMatrix matrix) {
        final ArrayList<Object> rows = new ArrayList<>();
        for (int row = 0; row < matrix.rowCount(); row++) {
            final ArrayList<Object> columns = new ArrayList<>();
            for (int column = 0; column < matrix.columnCount(); column++) {
                columns.add(matrix.entry(
                    row,
                    column
                ));
            }
            rows.add(columns);
        }
        return rows;
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
                "qubitReferences",
                writeQuantumReferences(gateOperation.qubitReferences())
            );
        } else if (operation instanceof MeasureOperation measureOperation) {
            json.put(
                "qubitReference",
                writeQuantumReference(measureOperation.qubitReference())
            );
            json.put(
                "bit",
                writeClassicalBit(measureOperation.bit())
            );
        } else if (operation instanceof ResetOperation resetOperation) {
            json.put(
                "qubitReference",
                writeQuantumReference(resetOperation.qubitReference())
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
        } else if (operation instanceof ClassicalDeclarationOperation declarationOperation) {
            json.put(
                "declaration",
                writeClassicalDeclaration(declarationOperation.declaration())
            );
            if (declarationOperation.hasInitializer()) {
                json.put(
                    "initializer",
                    writeClassicalExpression(declarationOperation.initializer())
                );
            }
        } else if (operation instanceof ClassicalArrayDeclarationOperation arrayOperation) {
            json.put(
                "name",
                arrayOperation.name()
            );
            json.put(
                "elementType",
                writeClassicalType(arrayOperation.elementType())
            );
            final ArrayList<Object> dimensions = new ArrayList<>();
            for (int i = 0; i < arrayOperation.dimensionCount(); i++) {
                dimensions.add(writeClassicalExpression(arrayOperation.dimension(i)));
            }
            json.put(
                "dimensions",
                dimensions
            );
            if (arrayOperation.hasInitializerText()) {
                json.put(
                    "initializerText",
                    arrayOperation.initializerText()
                );
            }
        } else if (operation instanceof CallableInvocationOperation invocationOperation) {
            json.put(
                "callable",
                invocationOperation.callableName()
            );
            if (invocationOperation.hasTarget()) {
                json.put(
                    "target",
                    writeClassicalExpression(invocationOperation.target())
                );
            }
            final ArrayList<Object> classicalArguments = new ArrayList<>();
            for (int i = 0; i < invocationOperation.classicalArguments().size(); i++) {
                classicalArguments.add(writeClassicalExpression(invocationOperation.classicalArguments().get(i)));
            }
            json.put(
                "classicalArguments",
                classicalArguments
            );
            final ArrayList<Object> quantumArguments = new ArrayList<>();
            for (int i = 0; i < invocationOperation.quantumArguments().size(); i++) {
                quantumArguments.add(writeQuantumReference(invocationOperation.quantumArguments().get(i)));
            }
            json.put(
                "quantumArguments",
                quantumArguments
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
        } else if (operation instanceof BlockOperation blockOperation) {
            json.put(
                "body",
                writeOperationBlock(
                    blockOperation.body(),
                    diagnostics
                )
            );
        } else if (operation instanceof ConditionalBlockOperation conditionalOperation) {
            json.put(
                "predicate",
                writeClassicalPredicate(conditionalOperation.predicate())
            );
            json.put(
                "then",
                writeOperationBlock(
                    conditionalOperation.thenBlock(),
                    diagnostics
                )
            );
            if (conditionalOperation.hasElseBlock()) {
                json.put(
                    "else",
                    writeOperationBlock(
                        conditionalOperation.elseBlock(),
                        diagnostics
                    )
                );
            }
        } else if (operation instanceof ForLoopOperation loopOperation) {
            json.put(
                "variable",
                loopOperation.variableName()
            );
            json.put(
                "startInclusive",
                loopOperation.startInclusive()
            );
            json.put(
                "step",
                loopOperation.step()
            );
            json.put(
                "endInclusive",
                loopOperation.endInclusive()
            );
            json.put(
                "body",
                writeOperationBlock(
                    loopOperation.body(),
                    diagnostics
                )
            );
        } else if (operation instanceof SymbolicForLoopOperation loopOperation) {
            json.put(
                "variable",
                loopOperation.variableName()
            );
            if (loopOperation.hasVariableTypeText()) {
                json.put(
                    "variableType",
                    loopOperation.variableTypeText()
                );
            }
            json.put(
                "startInclusive",
                writeClassicalExpression(loopOperation.startInclusive())
            );
            json.put(
                "step",
                writeClassicalExpression(loopOperation.step())
            );
            json.put(
                "endInclusive",
                writeClassicalExpression(loopOperation.endInclusive())
            );
            json.put(
                "body",
                writeOperationBlock(
                    loopOperation.body(),
                    diagnostics
                )
            );
        } else if (operation instanceof WhileLoopOperation loopOperation) {
            json.put(
                "predicate",
                writeClassicalPredicate(loopOperation.predicate())
            );
            json.put(
                "body",
                writeOperationBlock(
                    loopOperation.body(),
                    diagnostics
                )
            );
        } else if (operation instanceof DelayOperation delayOperation) {
            json.put(
                "duration",
                writeDuration(delayOperation.duration())
            );
            json.put(
                "qubitReferences",
                writeQuantumReferences(delayOperation.references())
            );
        } else if (operation instanceof TimingBoxOperation boxOperation) {
            if (boxOperation.hasDuration()) {
                json.put(
                    "duration",
                    writeDuration(boxOperation.duration())
                );
            }
            json.put(
                "body",
                writeOperationBlock(
                    boxOperation.body(),
                    diagnostics
                )
            );
        } else if (operation instanceof LabelOperation labelOperation) {
            json.put(
                "name",
                labelOperation.name()
            );
        } else if (operation instanceof BranchOperation branchOperation) {
            json.put(
                "targetLabel",
                branchOperation.targetLabel()
            );
            json.put(
                "conditionKind",
                branchOperation.conditionKind().name()
            );
            if (branchOperation.hasCondition()) {
                json.put(
                    "condition",
                    writeClassicalExpression(branchOperation.condition())
                );
            }
        } else if (
            operation.kind() == OperationKind.HALT
            || operation.kind() == OperationKind.WAIT
        ) {
            return json;
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

    private static ArrayList<Object> writeOperationBlock(
        final OperationBlock block,
        final ArrayList<PersistenceDiagnostic> diagnostics
    ) {
        final ArrayList<Object> operations = new ArrayList<>();
        for (int i = 0; i < block.operationCount(); i++) {
            operations.add(writeOperation(
                block.operation(i),
                OperationMetadata.empty(),
                diagnostics
            ));
        }
        return operations;
    }

    private static LinkedHashMap<String, Object> writeDuration(final DurationExpression duration) {
        final LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        if (duration.isStretch()) {
            json.put(
                "kind",
                "STRETCH"
            );
            json.put(
                "symbol",
                duration.symbol()
            );
        } else if (duration.isExpression()) {
            json.put(
                "kind",
                "EXPRESSION"
            );
            json.put(
                "expression",
                duration.expression()
            );
        } else {
            json.put(
                "kind",
                "DURATION"
            );
            json.put(
                "value",
                duration.value()
            );
            json.put(
                "unit",
                duration.unit().name()
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
            json.put(
                "definitionKind",
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
            if (modifier.hasPowerExpression()) {
                json.put(
                    "powerExpression",
                    writeParameterExpression(modifier.powerExpression())
                );
            } else {
                json.put(
                    "doubleValue",
                    modifier.doubleValue()
                );
            }
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
            case VARIABLE_REFERENCE -> json.put(
                "name",
                expression.variableName()
            );
            case BINARY_OPERATION -> {
                json.put(
                    "operator",
                    expression.binaryOperator().name()
                );
                json.put(
                    "left",
                    writeClassicalExpression(expression.leftExpression())
                );
                json.put(
                    "right",
                    writeClassicalExpression(expression.rightExpression())
                );
            }
            case BIT_REFERENCE -> json.put(
                "bit",
                writeClassicalBit(expression.bit())
            );
            case REGISTER_REFERENCE -> json.put(
                "register",
                expression.register().name().value()
            );
            case SYMBOLIC_REFERENCE -> json.put(
                "text",
                expression.symbolicText()
            );
            case CALL -> {
                json.put(
                    "callable",
                    expression.callableName()
                );
                final ArrayList<Object> arguments = new ArrayList<>();
                for (int i = 0; i < expression.callArgumentCount(); i++) {
                    arguments.add(writeClassicalExpression(expression.callArgument(i)));
                }
                json.put(
                    "arguments",
                    arguments
                );
            }
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

    private static ArrayList<Object> writeQuantumReferences(final QuantumReference[] references) {
        final ArrayList<Object> json = new ArrayList<>();
        for (int i = 0; i < references.length; i++) {
            json.add(writeQuantumReference(references[i]));
        }
        return json;
    }

    private static LinkedHashMap<String, Object> writeQuantumReference(final QuantumReference reference) {
        final LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put(
            "kind",
            reference.kind().name()
        );
        if (reference.kind() == QuantumReferenceKind.STATIC_QUBIT) {
            json.put(
                "qubit",
                writeQubit(reference.qubit())
            );
        } else if (reference.kind() == QuantumReferenceKind.DYNAMIC_REGISTER_INDEX) {
            json.put(
                "register",
                reference.register().name().value()
            );
            json.put(
                "index",
                writeClassicalExpression(reference.indexExpression())
            );
        } else {
            json.put(
                "hardwareIndex",
                reference.hardwareIndex()
            );
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

    @FunctionalInterface
    private interface JsonObjectFactory {

        LinkedHashMap<String, Object> create(int index);
    }
}
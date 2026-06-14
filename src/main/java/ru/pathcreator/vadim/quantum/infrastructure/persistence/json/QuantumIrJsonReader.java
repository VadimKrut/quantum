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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ru.pathcreator.vadim.quantum.application.persistence.diagnostic.PersistenceDiagnostic;
import ru.pathcreator.vadim.quantum.application.persistence.diagnostic.PersistenceDiagnosticCode;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrReadResult;
import ru.pathcreator.vadim.quantum.domain.bit.ClassicalBit;
import ru.pathcreator.vadim.quantum.domain.bit.Qubit;
import ru.pathcreator.vadim.quantum.domain.calibration.CalibrationDefinition;
import ru.pathcreator.vadim.quantum.domain.callable.CallableArgument;
import ru.pathcreator.vadim.quantum.domain.callable.CallableArgumentKind;
import ru.pathcreator.vadim.quantum.domain.callable.CallableDefinition;
import ru.pathcreator.vadim.quantum.domain.callable.ExternalCallableDeclaration;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableBarrierOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableBlockOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableClassicalAssignment;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableClassicalAssignmentOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableClassicalExpressionKind;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableClassicalPredicateKind;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableConditionalBlockOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableDelayOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableForLoopOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableGateOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableMeasureOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableOperationBlock;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableOperationKind;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableResetOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableTimingBoxOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableWhileLoopOperation;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalAssignment;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalBinaryOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalBooleanOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalComparisonOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalDeclaration;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpressionKind;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicateKind;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalType;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalTypeKind;
import ru.pathcreator.vadim.quantum.domain.gate.DistinctQubitsGateValidationRule;
import ru.pathcreator.vadim.quantum.domain.gate.Gate;
import ru.pathcreator.vadim.quantum.domain.gate.GateBodyOperation;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinition;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinitionKind;
import ru.pathcreator.vadim.quantum.domain.gate.GateMatrix;
import ru.pathcreator.vadim.quantum.domain.gate.GateValidationRule;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterBinaryOperator;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpressionKind;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterUnaryOperator;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.gate.modifier.GateModifier;
import ru.pathcreator.vadim.quantum.domain.gate.modifier.GateModifierKind;
import ru.pathcreator.vadim.quantum.domain.gate.modifier.ModifiedGate;
import ru.pathcreator.vadim.quantum.domain.metadata.ExternalSource;
import ru.pathcreator.vadim.quantum.domain.metadata.OperationMetadata;
import ru.pathcreator.vadim.quantum.domain.metadata.SourceLocation;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumComputationModel;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.BarrierOperation;
import ru.pathcreator.vadim.quantum.domain.operation.BlockOperation;
import ru.pathcreator.vadim.quantum.domain.operation.BranchConditionKind;
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
import ru.pathcreator.vadim.quantum.domain.operation.OperationKind;
import ru.pathcreator.vadim.quantum.domain.operation.QuantumReference;
import ru.pathcreator.vadim.quantum.domain.operation.QuantumReferenceKind;
import ru.pathcreator.vadim.quantum.domain.operation.ResetOperation;
import ru.pathcreator.vadim.quantum.domain.operation.SymbolicForLoopOperation;
import ru.pathcreator.vadim.quantum.domain.operation.TimingBoxOperation;
import ru.pathcreator.vadim.quantum.domain.operation.WaitOperation;
import ru.pathcreator.vadim.quantum.domain.operation.WhileLoopOperation;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;
import ru.pathcreator.vadim.quantum.domain.timing.DurationExpression;
import ru.pathcreator.vadim.quantum.domain.timing.DurationUnit;
import ru.pathcreator.vadim.quantum.domain.validation.QuantumProgramValidator;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationError;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

/**
 * Strict JSON reader родного формата Quantum IR.
 */
public final class QuantumIrJsonReader {

    private final ObjectMapper objectMapper;

    public QuantumIrJsonReader() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Читает Quantum IR из canonical JSON.
     *
     * @param content JSON-текст
     * @return результат чтения
     */
    public QuantumIrReadResult read(final String content) {
        if (content == null) {
            return QuantumIrReadResult.failure(List.of(PersistenceDiagnostic.error(
                PersistenceDiagnosticCode.NULL_INPUT,
                "Quantum IR JSON content must not be null."
            )));
        }
        if (content.isBlank()) {
            return QuantumIrReadResult.failure(List.of(PersistenceDiagnostic.error(
                PersistenceDiagnosticCode.EMPTY_INPUT,
                "Quantum IR JSON content must not be blank."
            )));
        }
        try {
            final JsonNode root = objectMapper.readTree(content);
            final ReadState state = new ReadState();
            final QuantumProgram program = readRoot(
                root,
                state
            );
            validateProgram(
                program,
                state
            );
            if (state.hasErrors()) {
                return QuantumIrReadResult.failure(state.diagnostics());
            }
            return QuantumIrReadResult.success(
                program,
                state.diagnostics()
            );
        } catch (final JsonProcessingException exception) {
            return QuantumIrReadResult.failure(List.of(PersistenceDiagnostic.error(
                PersistenceDiagnosticCode.MALFORMED_JSON,
                "Quantum IR JSON is malformed: " + exception.getOriginalMessage()
            )));
        } catch (final ReadException exception) {
            return QuantumIrReadResult.failure(List.of(exception.diagnostic()));
        }
    }

    private static QuantumProgram readRoot(
        final JsonNode root,
        final ReadState state
    ) {
        if (
            root == null
            || !root.isObject()
        ) {
            throw error(
                PersistenceDiagnosticCode.INVALID_STRUCTURE,
                "Quantum IR JSON root must be an object."
            );
        }
        final String format = requiredText(
            root,
            "format"
        );
        if (!QuantumIrJsonWriter.FORMAT_NAME.equals(format)) {
            throw error(
                PersistenceDiagnosticCode.UNSUPPORTED_FORMAT,
                "Quantum IR JSON format is not supported: " + format + "."
            );
        }
        final int version = requiredInt(
            root,
            "version"
        );
        if (version != QuantumIrJsonWriter.FORMAT_VERSION) {
            throw error(
                PersistenceDiagnosticCode.UNSUPPORTED_VERSION,
                "Quantum IR JSON version is not supported: " + version + "."
            );
        }
        final JsonNode programNode = requiredObject(
            root,
            "program"
        );
        return readProgram(
            programNode,
            state
        );
    }

    private static QuantumProgram readProgram(
        final JsonNode node,
        final ReadState state
    ) {
        final QuantumComputationModel computationModel = enumValue(
            QuantumComputationModel.class,
            requiredText(
                node,
                "computationModel"
            ),
            "program.computationModel"
        );
        final QuantumProgram program = QuantumProgram.create(computationModel);
        final JsonNode gateDefinitions = requiredArray(
            node,
            "gateDefinitions"
        );
        for (int i = 0; i < gateDefinitions.size(); i++) {
            state.addGateDefinition(readGateDefinitionStub(requiredArrayElementObject(
                gateDefinitions,
                i,
                "program.gateDefinitions"
            )));
        }
        for (int i = 0; i < gateDefinitions.size(); i++) {
            final GateDefinition definition = readGateDefinition(
                requiredArrayElementObject(
                    gateDefinitions,
                    i,
                    "program.gateDefinitions"
                ),
                state
            );
            try {
                program.addGateDefinition(definition);
            } catch (final IllegalArgumentException exception) {
                throw invalidValue(exception);
            }
            state.addGateDefinition(definition);
        }
        readProgramLevelDeclarations(
            node,
            program,
            state
        );
        final JsonNode circuits = requiredArray(
            node,
            "circuits"
        );
        for (int i = 0; i < circuits.size(); i++) {
            readCircuit(
                requiredArrayElementObject(
                    circuits,
                    i,
                    "program.circuits"
                ),
                program,
                state
            );
        }
        return program;
    }

    private static void readProgramLevelDeclarations(
        final JsonNode node,
        final QuantumProgram program,
        final ReadState state
    ) {
        readClassicalDeclarations(
            optionalArray(
                node,
                "classicalDeclarations"
            ),
            program
        );
        readCallableDefinitions(
            optionalArray(
                node,
                "callableDefinitions"
            ),
            program,
            state
        );
        readExternalCallableDeclarations(
            optionalArray(
                node,
                "externalCallableDeclarations"
            ),
            program
        );
        readCalibrationDefinitions(
            optionalArray(
                node,
                "calibrationDefinitions"
            ),
            program
        );
    }

    private static void readClassicalDeclarations(
        final JsonNode node,
        final QuantumProgram program
    ) {
        for (int i = 0; i < node.size(); i++) {
            final JsonNode item = requiredArrayElementObject(
                node,
                i,
                "program.classicalDeclarations"
            );
            try {
                program.addClassicalDeclaration(new ClassicalDeclaration(
                    requiredText(
                        item,
                        "name"
                    ),
                    readClassicalType(requiredObject(
                        item,
                        "type"
                    ))
                ));
            } catch (final IllegalArgumentException exception) {
                throw invalidValue(exception);
            }
        }
    }

    private static void readCallableDefinitions(
        final JsonNode node,
        final QuantumProgram program,
        final ReadState state
    ) {
        for (int i = 0; i < node.size(); i++) {
            final JsonNode item = requiredArrayElementObject(
                node,
                i,
                "program.callableDefinitions"
            );
            try {
                program.addCallableDefinition(new CallableDefinition(
                    requiredText(
                        item,
                        "name"
                    ),
                    readCallableOperationBlock(
                        requiredArray(
                            item,
                            "body"
                        ),
                        state
                    ),
                    readCallableArguments(requiredArray(
                        item,
                        "arguments"
                    ))
                ));
            } catch (final IllegalArgumentException exception) {
                throw invalidValue(exception);
            }
        }
    }

    private static CallableOperationBlock readCallableOperationBlock(
        final JsonNode node,
        final ReadState state
    ) {
        final ArrayList<CallableOperation> operations = new ArrayList<>();
        for (int i = 0; i < node.size(); i++) {
            operations.add(readCallableOperation(
                requiredArrayElementObject(
                    node,
                    i,
                    "callable.body"
                ),
                state
            ));
        }
        return CallableOperationBlock.of(operations);
    }

    private static CallableOperation readCallableOperation(
        final JsonNode node,
        final ReadState state
    ) {
        final CallableOperationKind kind = enumValue(
            CallableOperationKind.class,
            requiredText(
                node,
                "kind"
            ),
            "callableOperation.kind"
        );
        try {
            return switch (kind) {
                case GATE -> CallableGateOperation.parameterized(
                    readGate(
                        requiredObject(
                            node,
                            "gate"
                        ),
                        state
                    ),
                    readParameterExpressions(requiredArray(
                        node,
                        "parameters"
                    )),
                    readTextArray(requiredArray(
                        node,
                        "qubits"
                    ))
                );
                case MEASURE -> new CallableMeasureOperation(
                    requiredText(
                        node,
                        "qubit"
                    ),
                    requiredText(
                        node,
                        "bit"
                    )
                );
                case RESET -> new CallableResetOperation(requiredText(
                    node,
                    "qubit"
                ));
                case BARRIER -> new CallableBarrierOperation(readTextArray(requiredArray(
                    node,
                    "qubits"
                )));
                case CLASSICAL_ASSIGNMENT -> new CallableClassicalAssignmentOperation(
                    readCallableClassicalAssignment(requiredObject(
                        node,
                        "assignment"
                    ))
                );
                case BLOCK -> new CallableBlockOperation(readCallableOperationBlock(
                    requiredArray(
                        node,
                        "body"
                    ),
                    state
                ));
                case CONDITIONAL_BLOCK -> new CallableConditionalBlockOperation(
                    readCallableClassicalPredicate(requiredObject(
                        node,
                        "predicate"
                    )),
                    readCallableOperationBlock(
                        requiredArray(
                            node,
                            "then"
                        ),
                        state
                    ),
                    node.has("else") ? readCallableOperationBlock(
                        requiredArray(
                            node,
                            "else"
                        ),
                        state
                    ) : null
                );
                case FOR_LOOP -> new CallableForLoopOperation(
                    requiredText(
                        node,
                        "variable"
                    ),
                    requiredLong(
                        node,
                        "startInclusive"
                    ),
                    requiredLong(
                        node,
                        "step"
                    ),
                    requiredLong(
                        node,
                        "endInclusive"
                    ),
                    readCallableOperationBlock(
                        requiredArray(
                            node,
                            "body"
                        ),
                        state
                    )
                );
                case WHILE_LOOP -> new CallableWhileLoopOperation(
                    readCallableClassicalPredicate(requiredObject(
                        node,
                        "predicate"
                    )),
                    readCallableOperationBlock(
                        requiredArray(
                            node,
                            "body"
                        ),
                        state
                    )
                );
                case DELAY -> new CallableDelayOperation(
                    readDuration(requiredObject(
                        node,
                        "duration"
                    )),
                    readTextArray(requiredArray(
                        node,
                        "qubits"
                    ))
                );
                case TIMING_BOX -> new CallableTimingBoxOperation(
                    node.has("duration") ? readDuration(requiredObject(
                        node,
                        "duration"
                    )) : null,
                    readCallableOperationBlock(
                        requiredArray(
                            node,
                            "body"
                        ),
                        state
                    )
                );
            };
        } catch (final IllegalArgumentException exception) {
            throw invalidValue(exception);
        }
    }

    private static CallableClassicalAssignment readCallableClassicalAssignment(final JsonNode node) {
        return new CallableClassicalAssignment(
            readCallableClassicalExpression(requiredObject(
                node,
                "target"
            )),
            readCallableClassicalExpression(requiredObject(
                node,
                "value"
            ))
        );
    }

    private static CallableClassicalExpression readCallableClassicalExpression(final JsonNode node) {
        final CallableClassicalExpressionKind kind = enumValue(
            CallableClassicalExpressionKind.class,
            requiredText(
                node,
                "kind"
            ),
            "callableClassicalExpression.kind"
        );
        return switch (kind) {
            case INTEGER -> CallableClassicalExpression.integer(requiredLong(
                node,
                "value"
            ));
            case ARGUMENT_REFERENCE -> CallableClassicalExpression.argument(requiredText(
                node,
                "argument"
            ));
        };
    }

    private static CallableClassicalPredicate readCallableClassicalPredicate(final JsonNode node) {
        final CallableClassicalPredicateKind kind = enumValue(
            CallableClassicalPredicateKind.class,
            requiredText(
                node,
                "kind"
            ),
            "callableClassicalPredicate.kind"
        );
        return switch (kind) {
            case COMPARISON -> CallableClassicalPredicate.compare(
                readCallableClassicalExpression(requiredObject(
                    node,
                    "left"
                )),
                enumValue(
                    ClassicalComparisonOperator.class,
                    requiredText(
                        node,
                        "operator"
                    ),
                    "callableClassicalPredicate.operator"
                ),
                readCallableClassicalExpression(requiredObject(
                    node,
                    "right"
                ))
            );
            case NOT -> CallableClassicalPredicate.not(readCallableClassicalPredicate(requiredObject(
                node,
                "predicate"
            )));
            case BOOLEAN -> readCallableBooleanPredicate(node);
        };
    }

    private static CallableClassicalPredicate readCallableBooleanPredicate(final JsonNode node) {
        final CallableClassicalPredicate left = readCallableClassicalPredicate(requiredObject(
            node,
            "left"
        ));
        final CallableClassicalPredicate right = readCallableClassicalPredicate(requiredObject(
            node,
            "right"
        ));
        final ClassicalBooleanOperator operator = enumValue(
            ClassicalBooleanOperator.class,
            requiredText(
                node,
                "operator"
            ),
            "callableClassicalPredicate.operator"
        );
        if (operator == ClassicalBooleanOperator.AND) {
            return CallableClassicalPredicate.and(
                left,
                right
            );
        }
        return CallableClassicalPredicate.or(
            left,
            right
        );
    }

    private static void readExternalCallableDeclarations(
        final JsonNode node,
        final QuantumProgram program
    ) {
        for (int i = 0; i < node.size(); i++) {
            final JsonNode item = requiredArrayElementObject(
                node,
                i,
                "program.externalCallableDeclarations"
            );
            try {
                program.addExternalCallableDeclaration(new ExternalCallableDeclaration(
                    requiredText(
                        item,
                        "name"
                    ),
                    item.has("returnType")
                        ? readClassicalType(requiredObject(
                            item,
                            "returnType"
                        ))
                        : null,
                    readCallableArguments(requiredArray(
                        item,
                        "arguments"
                    ))
                ));
            } catch (final IllegalArgumentException exception) {
                throw invalidValue(exception);
            }
        }
    }

    private static void readCalibrationDefinitions(
        final JsonNode node,
        final QuantumProgram program
    ) {
        for (int i = 0; i < node.size(); i++) {
            final JsonNode item = requiredArrayElementObject(
                node,
                i,
                "program.calibrationDefinitions"
            );
            try {
                program.addCalibrationDefinition(new CalibrationDefinition(
                    requiredText(
                        item,
                        "targetName"
                    ),
                    readTextList(requiredArray(
                        item,
                        "parameterNames"
                    )),
                    readTextList(requiredArray(
                        item,
                        "qubitNames"
                    )),
                    requiredText(
                        item,
                        "bodyLanguage"
                    ),
                    requiredText(
                        item,
                        "body"
                    )
                ));
            } catch (final IllegalArgumentException exception) {
                throw invalidValue(exception);
            }
        }
    }

    private static ClassicalType readClassicalType(final JsonNode node) {
        final ClassicalTypeKind kind = enumValue(
            ClassicalTypeKind.class,
            requiredText(
                node,
                "kind"
            ),
            "classicalType.kind"
        );
        if (node.has("bitWidth")) {
            return ClassicalType.sized(
                kind,
                requiredInt(
                    node,
                    "bitWidth"
                )
            );
        }
        return ClassicalType.of(kind);
    }

    private static ClassicalDeclaration readClassicalDeclaration(final JsonNode node) {
        return new ClassicalDeclaration(
            requiredText(
                node,
                "name"
            ),
            readClassicalType(requiredObject(
                node,
                "type"
            ))
        );
    }

    private static CallableArgument[] readCallableArguments(final JsonNode node) {
        final CallableArgument[] arguments = new CallableArgument[node.size()];
        for (int i = 0; i < node.size(); i++) {
            final JsonNode item = requiredArrayElementObject(
                node,
                i,
                "callable.arguments"
            );
            final CallableArgumentKind kind = enumValue(
                CallableArgumentKind.class,
                requiredText(
                    item,
                    "kind"
                ),
                "callableArgument.kind"
            );
            if (kind == CallableArgumentKind.QUBIT) {
                arguments[i] = CallableArgument.qubit(requiredText(
                    item,
                    "name"
                ));
            } else {
                arguments[i] = CallableArgument.classical(
                    requiredText(
                        item,
                        "name"
                    ),
                    readClassicalType(requiredObject(
                        item,
                        "classicalType"
                    ))
                );
            }
        }
        return arguments;
    }

    private static GateDefinition readGateDefinitionStub(final JsonNode node) {
        final String name = requiredText(
            node,
            "name"
        );
        final GateDefinitionKind kind = enumValue(
            GateDefinitionKind.class,
            requiredText(
                node,
                "kind"
            ),
            "gateDefinition.kind"
        );
        try {
            if (kind == GateDefinitionKind.INTRINSIC) {
                return GateDefinition.of(
                    name,
                    requiredInt(
                        node,
                        "arity"
                    ),
                    requiredInt(
                        node,
                        "parameterCount"
                    )
                );
            }
            return GateDefinition.of(
                name,
                requiredArray(
                    node,
                    "qubitNames"
                ).size(),
                requiredArray(
                    node,
                    "parameterNames"
                ).size()
            );
        } catch (final IllegalArgumentException exception) {
            throw invalidValue(exception);
        }
    }

    private static GateDefinition readGateDefinition(
        final JsonNode node,
        final ReadState state
    ) {
        final String name = requiredText(
            node,
            "name"
        );
        final GateDefinitionKind kind = enumValue(
            GateDefinitionKind.class,
            requiredText(
                node,
                "kind"
            ),
            "gateDefinition.kind"
        );
        try {
            if (kind == GateDefinitionKind.INTRINSIC) {
                return GateDefinition.of(
                    name,
                    requiredInt(
                        node,
                        "arity"
                    ),
                    requiredInt(
                        node,
                        "parameterCount"
                    ),
                    readValidationRules(requiredArray(
                        node,
                        "validationRules"
                    ))
                );
            }
            final List<String> parameterNames = readTextList(requiredArray(
                node,
                "parameterNames"
            ));
            final List<String> qubitNames = readTextList(requiredArray(
                node,
                "qubitNames"
            ));
            if (kind == GateDefinitionKind.OPAQUE) {
                return GateDefinition.opaque(
                    name,
                    parameterNames,
                    qubitNames
                );
            }
            if (kind == GateDefinitionKind.MATRIX) {
                return GateDefinition.matrix(
                    name,
                    parameterNames,
                    qubitNames,
                    readGateMatrix(requiredArray(
                        node,
                        "matrix"
                    ))
                );
            }
            return GateDefinition.composite(
                name,
                parameterNames,
                qubitNames,
                readGateBodyOperations(
                    requiredArray(
                        node,
                        "bodyOperations"
                    ),
                    state
                )
            );
        } catch (final IllegalArgumentException exception) {
            throw invalidValue(exception);
        }
    }

    private static GateMatrix readGateMatrix(final JsonNode node) {
        final String[][] entries = new String[node.size()][];
        for (int row = 0; row < node.size(); row++) {
            final JsonNode rowNode = requiredArrayElementArray(
                node,
                row,
                "gateDefinition.matrix"
            );
            entries[row] = new String[rowNode.size()];
            for (int column = 0; column < rowNode.size(); column++) {
                final JsonNode item = rowNode.get(column);
                if (!item.isTextual()) {
                    throw invalidStructure("Gate matrix entry must be text.");
                }
                entries[row][column] = item.asText();
            }
        }
        return GateMatrix.of(entries);
    }

    private static List<GateValidationRule> readValidationRules(final JsonNode node) {
        final ArrayList<GateValidationRule> rules = new ArrayList<>();
        for (int i = 0; i < node.size(); i++) {
            final JsonNode item = node.get(i);
            if (!item.isTextual()) {
                throw invalidStructure("validationRules item must be a string.");
            }
            if ("DISTINCT_QUBITS".equals(item.asText())) {
                rules.add(DistinctQubitsGateValidationRule.INSTANCE);
            } else {
                throw error(
                    PersistenceDiagnosticCode.UNSUPPORTED_MODEL_FEATURE,
                    "Unknown portable gate validation rule: " + item.asText() + "."
                );
            }
        }
        return rules;
    }

    private static List<GateBodyOperation> readGateBodyOperations(
        final JsonNode node,
        final ReadState state
    ) {
        final ArrayList<GateBodyOperation> operations = new ArrayList<>();
        for (int i = 0; i < node.size(); i++) {
            operations.add(readGateBodyOperation(
                requiredArrayElementObject(
                    node,
                    i,
                    "gateDefinition.bodyOperations"
                ),
                state
            ));
        }
        return operations;
    }

    private static GateBodyOperation readGateBodyOperation(
        final JsonNode node,
        final ReadState state
    ) {
        final Gate gate = readGate(
            requiredObject(
                node,
                "gate"
            ),
            state
        );
        try {
            return GateBodyOperation.of(
                gate,
                readParameterExpressions(requiredArray(
                    node,
                    "parameters"
                )),
                readTextArray(requiredArray(
                    node,
                    "qubits"
                ))
            );
        } catch (final IllegalArgumentException exception) {
            throw invalidValue(exception);
        }
    }

    private static void readCircuit(
        final JsonNode node,
        final QuantumProgram program,
        final ReadState state
    ) {
        final CircuitState circuitState = new CircuitState();
        final QuantumCircuit circuit;
        try {
            circuit = program.createCircuit(requiredText(
                node,
                "name"
            ));
        } catch (final RuntimeException exception) {
            throw invalidValue(exception);
        }
        readQuantumRegisters(
            requiredArray(
                node,
                "quantumRegisters"
            ),
            circuit,
            circuitState
        );
        readClassicalRegisters(
            requiredArray(
                node,
                "classicalRegisters"
            ),
            circuit,
            circuitState
        );
        final JsonNode operations = requiredArray(
            node,
            "operations"
        );
        for (int i = 0; i < operations.size(); i++) {
            final JsonNode operationNode = requiredArrayElementObject(
                operations,
                i,
                "circuit.operations"
            );
            final Operation operation = readOperation(
                operationNode,
                state,
                circuitState
            );
            appendOperation(
                circuit,
                operation
            );
            if (operationNode.has("metadata")) {
                circuit.setOperationMetadata(
                    circuit.operationCount() - 1,
                    readMetadata(requiredObject(
                        operationNode,
                        "metadata"
                    ))
                );
            }
        }
    }

    private static void readQuantumRegisters(
        final JsonNode node,
        final QuantumCircuit circuit,
        final CircuitState state
    ) {
        for (int i = 0; i < node.size(); i++) {
            final JsonNode item = requiredArrayElementObject(
                node,
                i,
                "circuit.quantumRegisters"
            );
            try {
                final QuantumRegister register = circuit.createQuantumRegister(
                    requiredText(
                        item,
                        "name"
                    ),
                    requiredInt(
                        item,
                        "size"
                    )
                );
                state.addQuantumRegister(register);
            } catch (final IllegalArgumentException exception) {
                throw invalidValue(exception);
            }
        }
    }

    private static void readClassicalRegisters(
        final JsonNode node,
        final QuantumCircuit circuit,
        final CircuitState state
    ) {
        for (int i = 0; i < node.size(); i++) {
            final JsonNode item = requiredArrayElementObject(
                node,
                i,
                "circuit.classicalRegisters"
            );
            try {
                final ClassicalRegister register = circuit.createClassicalRegister(
                    requiredText(
                        item,
                        "name"
                    ),
                    requiredInt(
                        item,
                        "size"
                    )
                );
                state.addClassicalRegister(register);
            } catch (final IllegalArgumentException exception) {
                throw invalidValue(exception);
            }
        }
    }

    private static Operation readOperation(
        final JsonNode node,
        final ReadState state,
        final CircuitState circuitState
    ) {
        final OperationKind kind = enumValue(
            OperationKind.class,
            requiredText(
                node,
                "kind"
            ),
            "operation.kind"
        );
        try {
            return switch (kind) {
                case GATE -> GateOperation.parameterizedReferences(
                    readGate(
                        requiredObject(
                            node,
                            "gate"
                        ),
                        state
                    ),
                    readParameterExpressions(requiredArray(
                        node,
                        "parameters"
                    )),
                    readOperationQuantumReferences(
                        node,
                        circuitState
                    )
                );
                case MEASURE -> new MeasureOperation(
                    readOperationQuantumReference(
                        node,
                        circuitState
                    ),
                    readClassicalBit(
                        requiredText(
                            node,
                            "bit"
                        ),
                        circuitState
                    )
                );
                case RESET -> new ResetOperation(readOperationQuantumReference(
                    node,
                    circuitState
                ));
                case BARRIER -> new BarrierOperation(readQubits(
                    requiredArray(
                        node,
                        "qubits"
                    ),
                    circuitState
                ));
                case CONTROLLED -> new ControlledOperation(
                    readClassicalCondition(
                        requiredObject(
                            node,
                            "condition"
                        ),
                        circuitState
                    ),
                    readOperation(
                        requiredObject(
                            node,
                            "operation"
                        ),
                        state,
                        circuitState
                    )
                );
                case CLASSICAL_ASSIGNMENT -> new ClassicalAssignmentOperation(readClassicalAssignment(
                    requiredObject(
                        node,
                        "assignment"
                    ),
                    circuitState
                ));
                case CLASSICAL_DECLARATION -> new ClassicalDeclarationOperation(
                    readClassicalDeclaration(requiredObject(
                        node,
                        "declaration"
                    )),
                    node.has("initializer")
                        ? readClassicalExpression(
                            requiredObject(
                                node,
                                "initializer"
                            ),
                            circuitState
                        )
                        : null
                );
                case CLASSICAL_ARRAY_DECLARATION -> new ClassicalArrayDeclarationOperation(
                    requiredText(
                        node,
                        "name"
                    ),
                    readClassicalType(requiredObject(
                        node,
                        "elementType"
                    )),
                    readClassicalExpressionList(
                        requiredArray(
                            node,
                            "dimensions"
                        ),
                        circuitState
                    ),
                    node.has("initializerText")
                        ? requiredText(
                            node,
                            "initializerText"
                        )
                        : null
                );
                case CALLABLE_INVOCATION -> new CallableInvocationOperation(
                    requiredText(
                        node,
                        "callable"
                    ),
                    node.has("target")
                        ? readClassicalExpression(
                            requiredObject(
                                node,
                                "target"
                            ),
                            circuitState
                        )
                        : null,
                    readClassicalExpressionList(
                        requiredArray(
                            node,
                            "classicalArguments"
                        ),
                        circuitState
                    ),
                    readQuantumReferenceList(
                        requiredArray(
                            node,
                            "quantumArguments"
                        ),
                        circuitState
                    )
                );
                case CLASSICALLY_CONTROLLED -> new ClassicallyControlledOperation(
                    readClassicalPredicate(
                        requiredObject(
                            node,
                            "predicate"
                        ),
                        circuitState
                    ),
                    readOperation(
                        requiredObject(
                            node,
                            "operation"
                        ),
                        state,
                        circuitState
                    )
                );
                case BLOCK -> new BlockOperation(readOperationBlock(
                    requiredArray(
                        node,
                        "body"
                    ),
                    state,
                    circuitState
                ));
                case CONDITIONAL_BLOCK -> new ConditionalBlockOperation(
                    readClassicalPredicate(
                        requiredObject(
                            node,
                            "predicate"
                        ),
                        circuitState
                    ),
                    readOperationBlock(
                        requiredArray(
                            node,
                            "then"
                        ),
                        state,
                        circuitState
                    ),
                    node.has("else") ? readOperationBlock(
                        requiredArray(
                            node,
                            "else"
                        ),
                        state,
                        circuitState
                    ) : null
                );
                case FOR_LOOP -> new ForLoopOperation(
                    requiredText(
                        node,
                        "variable"
                    ),
                    requiredLong(
                        node,
                        "startInclusive"
                    ),
                    requiredLong(
                        node,
                        "step"
                    ),
                    requiredLong(
                        node,
                        "endInclusive"
                    ),
                    readOperationBlock(
                        requiredArray(
                            node,
                            "body"
                        ),
                        state,
                        circuitState
                    )
                );
                case SYMBOLIC_FOR_LOOP -> new SymbolicForLoopOperation(
                    requiredText(
                        node,
                        "variable"
                    ),
                    node.has("variableType")
                        ? requiredText(
                            node,
                            "variableType"
                        )
                        : null,
                    readClassicalExpression(
                        requiredObject(
                            node,
                            "startInclusive"
                        ),
                        circuitState
                    ),
                    readClassicalExpression(
                        requiredObject(
                            node,
                            "step"
                        ),
                        circuitState
                    ),
                    readClassicalExpression(
                        requiredObject(
                            node,
                            "endInclusive"
                        ),
                        circuitState
                    ),
                    readOperationBlock(
                        requiredArray(
                            node,
                            "body"
                        ),
                        state,
                        circuitState
                    )
                );
                case WHILE_LOOP -> new WhileLoopOperation(
                    readClassicalPredicate(
                        requiredObject(
                            node,
                            "predicate"
                        ),
                        circuitState
                    ),
                    readOperationBlock(
                        requiredArray(
                            node,
                            "body"
                        ),
                        state,
                        circuitState
                    )
                );
                case DELAY -> new DelayOperation(
                    readDuration(requiredObject(
                        node,
                        "duration"
                    )),
                    readOperationQuantumReferences(
                        node,
                        circuitState
                    )
                );
                case TIMING_BOX -> new TimingBoxOperation(
                    node.has("duration") ? readDuration(requiredObject(
                        node,
                        "duration"
                    )) : null,
                    readOperationBlock(
                        requiredArray(
                            node,
                            "body"
                        ),
                        state,
                        circuitState
                    )
                );
                case LABEL -> new LabelOperation(requiredText(
                    node,
                    "name"
                ));
                case BRANCH -> new BranchOperation(
                    requiredText(
                        node,
                        "targetLabel"
                    ),
                    enumValue(
                        BranchConditionKind.class,
                        requiredText(
                            node,
                            "conditionKind"
                        ),
                        "branch.conditionKind"
                    ),
                    node.has("condition")
                        ? readClassicalExpression(
                            requiredObject(
                                node,
                                "condition"
                            ),
                            circuitState
                        )
                        : null
                );
                case HALT -> HaltOperation.INSTANCE;
                case WAIT -> WaitOperation.INSTANCE;
            };
        } catch (final IllegalArgumentException exception) {
            throw invalidValue(exception);
        }
    }

    private static OperationBlock readOperationBlock(
        final JsonNode node,
        final ReadState state,
        final CircuitState circuitState
    ) {
        final ArrayList<Operation> operations = new ArrayList<>();
        for (int i = 0; i < node.size(); i++) {
            operations.add(readOperation(
                requiredArrayElementObject(
                    node,
                    i,
                    "operationBlock"
                ),
                state,
                circuitState
            ));
        }
        return OperationBlock.of(operations);
    }

    private static DurationExpression readDuration(final JsonNode node) {
        final String kind = requiredText(
            node,
            "kind"
        );
        if ("STRETCH".equals(kind)) {
            return DurationExpression.stretch(requiredText(
                node,
                "symbol"
            ));
        }
        if ("EXPRESSION".equals(kind)) {
            return DurationExpression.expression(requiredText(
                node,
                "expression"
            ));
        }
        if ("DURATION".equals(kind)) {
            return DurationExpression.duration(
                requiredDouble(
                    node,
                    "value"
                ),
                enumValue(
                    DurationUnit.class,
                    requiredText(
                        node,
                        "unit"
                    ),
                    "duration.unit"
                )
            );
        }
        throw invalidValue(new IllegalArgumentException("Unknown duration expression kind: " + kind + "."));
    }

    private static void appendOperation(
        final QuantumCircuit circuit,
        final Operation operation
    ) {
        if (operation instanceof GateOperation gateOperation) {
            circuit.parameterizedGateReferences(
                gateOperation.gate(),
                gateOperation.parameters(),
                gateOperation.qubitReferences()
            );
        } else if (operation instanceof MeasureOperation measureOperation) {
            circuit.measureReference(
                measureOperation.qubitReference(),
                measureOperation.bit()
            );
        } else if (operation instanceof ResetOperation resetOperation) {
            circuit.resetReference(resetOperation.qubitReference());
        } else if (operation instanceof BarrierOperation barrierOperation) {
            circuit.barrier(barrierOperation.qubits());
        } else if (operation instanceof ControlledOperation controlledOperation) {
            circuit.controlled(
                controlledOperation.condition(),
                controlledOperation.operation()
            );
        } else if (operation instanceof ClassicalAssignmentOperation assignmentOperation) {
            circuit.assign(assignmentOperation.assignment());
        } else if (operation instanceof ClassicalDeclarationOperation declarationOperation) {
            circuit.classicalDeclaration(declarationOperation);
        } else if (operation instanceof ClassicalArrayDeclarationOperation arrayOperation) {
            circuit.classicalArrayDeclaration(arrayOperation);
        } else if (operation instanceof CallableInvocationOperation invocationOperation) {
            circuit.callableInvocation(invocationOperation);
        } else if (operation instanceof ClassicallyControlledOperation controlledOperation) {
            circuit.classicallyControlled(
                controlledOperation.predicate(),
                controlledOperation.operation()
            );
        } else if (operation instanceof BlockOperation blockOperation) {
            circuit.block(blockOperation.body());
        } else if (operation instanceof ConditionalBlockOperation conditionalOperation) {
            circuit.conditionalBlock(
                conditionalOperation.predicate(),
                conditionalOperation.thenBlock(),
                conditionalOperation.hasElseBlock() ? conditionalOperation.elseBlock() : null
            );
        } else if (operation instanceof ForLoopOperation loopOperation) {
            circuit.forLoop(
                loopOperation.variableName(),
                loopOperation.startInclusive(),
                loopOperation.step(),
                loopOperation.endInclusive(),
                loopOperation.body()
            );
        } else if (operation instanceof SymbolicForLoopOperation loopOperation) {
            circuit.symbolicForLoop(loopOperation);
        } else if (operation instanceof WhileLoopOperation loopOperation) {
            circuit.whileLoop(
                loopOperation.predicate(),
                loopOperation.body()
            );
        } else if (operation instanceof DelayOperation delayOperation) {
            circuit.delay(
                delayOperation.duration(),
                delayOperation.qubits()
            );
        } else if (operation instanceof TimingBoxOperation boxOperation) {
            circuit.timingBox(
                boxOperation.hasDuration() ? boxOperation.duration() : null,
                boxOperation.body()
            );
        } else if (operation instanceof LabelOperation labelOperation) {
            circuit.label(labelOperation.name());
        } else if (operation instanceof BranchOperation branchOperation) {
            circuit.branch(branchOperation);
        } else if (operation instanceof HaltOperation) {
            circuit.halt();
        } else if (operation instanceof WaitOperation) {
            circuit.waitInstruction();
        } else {
            throw error(
                PersistenceDiagnosticCode.UNSUPPORTED_MODEL_FEATURE,
                "Operation implementation is not supported by Quantum IR JSON reader."
            );
        }
    }

    private static Gate readGate(
        final JsonNode node,
        final ReadState state
    ) {
        final String kind = requiredText(
            node,
            "kind"
        );
        if ("STANDARD".equals(kind)) {
            return enumValue(
                StandardGate.class,
                requiredText(
                    node,
                    "name"
                ),
                "gate.name"
            );
        }
        if ("GATE_DEFINITION".equals(kind)) {
            final String name = requiredText(
                node,
                "name"
            );
            final GateDefinition definition = state.gateDefinition(name);
            if (definition == null) {
                if (
                    node.has("arity")
                    && node.has("parameterCount")
                ) {
                    try {
                        return GateDefinition.of(
                            name,
                            requiredInt(
                                node,
                                "arity"
                            ),
                            requiredInt(
                                node,
                                "parameterCount"
                            )
                        );
                    } catch (final IllegalArgumentException exception) {
                        throw invalidValue(exception);
                    }
                }
                throw error(
                    PersistenceDiagnosticCode.UNKNOWN_REFERENCE,
                    "Gate definition reference is unknown: " + name + "."
                );
            }
            return definition;
        }
        if ("MODIFIED".equals(kind)) {
            try {
                return ModifiedGate.of(
                    readGate(
                        requiredObject(
                            node,
                            "baseGate"
                        ),
                        state
                    ),
                    readGateModifiers(requiredArray(
                        node,
                        "modifiers"
                    ))
                );
            } catch (final IllegalArgumentException exception) {
                throw invalidValue(exception);
            }
        }
        throw error(
            PersistenceDiagnosticCode.INVALID_VALUE,
            "Gate kind is not supported: " + kind + "."
        );
    }

    private static List<GateModifier> readGateModifiers(final JsonNode node) {
        final ArrayList<GateModifier> modifiers = new ArrayList<>();
        for (int i = 0; i < node.size(); i++) {
            final JsonNode item = requiredArrayElementObject(
                node,
                i,
                "gate.modifiers"
            );
            modifiers.add(readGateModifier(item));
        }
        return modifiers;
    }

    private static GateModifier readGateModifier(final JsonNode node) {
        final GateModifierKind kind = enumValue(
            GateModifierKind.class,
            requiredText(
                node,
                "kind"
            ),
            "gateModifier.kind"
        );
        try {
            return switch (kind) {
                case INVERSE -> GateModifier.inverse();
                case CONTROLLED -> GateModifier.controlled(requiredInt(
                    node,
                    "integerValue"
                ));
                case POWER -> node.has("powerExpression")
                    ? GateModifier.power(readParameterExpression(requiredObject(
                        node,
                        "powerExpression"
                    )))
                    : GateModifier.power(requiredDouble(
                        node,
                        "doubleValue"
                    ));
                case REPEAT -> GateModifier.repeat(requiredInt(
                    node,
                    "integerValue"
                ));
                case ANNOTATION -> GateModifier.annotation(requiredText(
                    node,
                    "annotationName"
                ));
            };
        } catch (final IllegalArgumentException exception) {
            throw invalidValue(exception);
        }
    }

    private static ParameterExpression[] readParameterExpressions(final JsonNode node) {
        final ParameterExpression[] result = new ParameterExpression[node.size()];
        for (int i = 0; i < node.size(); i++) {
            result[i] = readParameterExpression(requiredArrayElementObject(
                node,
                i,
                "parameters"
            ));
        }
        return result;
    }

    private static ParameterExpression readParameterExpression(final JsonNode node) {
        final ParameterExpressionKind kind = enumValue(
            ParameterExpressionKind.class,
            requiredText(
                node,
                "kind"
            ),
            "parameter.kind"
        );
        try {
            return switch (kind) {
                case NUMERIC -> ParameterExpression.of(requiredDouble(
                    node,
                    "value"
                ));
                case NAMED -> ParameterExpression.named(requiredText(
                    node,
                    "name"
                ));
                case KNOWN_CONSTANT -> ParameterExpression.knownConstant(requiredText(
                    node,
                    "name"
                ));
                case UNARY -> readUnaryParameterExpression(node);
                case BINARY -> readBinaryParameterExpression(node);
            };
        } catch (final IllegalArgumentException exception) {
            throw invalidValue(exception);
        }
    }

    private static ParameterExpression readUnaryParameterExpression(final JsonNode node) {
        final ParameterUnaryOperator operator = enumValue(
            ParameterUnaryOperator.class,
            requiredText(
                node,
                "operator"
            ),
            "parameter.operator"
        );
        final ParameterExpression operand = readParameterExpression(requiredObject(
            node,
            "operand"
        ));
        return switch (operator) {
            case NEGATE -> ParameterExpression.negate(operand);
        };
    }

    private static ParameterExpression readBinaryParameterExpression(final JsonNode node) {
        final ParameterBinaryOperator operator = enumValue(
            ParameterBinaryOperator.class,
            requiredText(
                node,
                "operator"
            ),
            "parameter.operator"
        );
        final ParameterExpression left = readParameterExpression(requiredObject(
            node,
            "left"
        ));
        final ParameterExpression right = readParameterExpression(requiredObject(
            node,
            "right"
        ));
        return switch (operator) {
            case ADD -> ParameterExpression.add(
                left,
                right
            );
            case SUBTRACT -> ParameterExpression.subtract(
                left,
                right
            );
            case MULTIPLY -> ParameterExpression.multiply(
                left,
                right
            );
            case DIVIDE -> ParameterExpression.divide(
                left,
                right
            );
        };
    }

    private static ClassicalCondition readClassicalCondition(
        final JsonNode node,
        final CircuitState state
    ) {
        return ClassicalCondition.equalTo(
            state.classicalRegister(requiredText(
                node,
                "register"
            )),
            requiredLong(
                node,
                "expectedValue"
            )
        );
    }

    private static ClassicalAssignment readClassicalAssignment(
        final JsonNode node,
        final CircuitState state
    ) {
        return new ClassicalAssignment(
            readClassicalExpression(
                requiredObject(
                    node,
                    "target"
                ),
                state
            ),
            readClassicalExpression(
                requiredObject(
                    node,
                    "value"
                ),
                state
            )
        );
    }

    private static ClassicalExpression readClassicalExpression(
        final JsonNode node,
        final CircuitState state
    ) {
        final ClassicalExpressionKind kind = enumValue(
            ClassicalExpressionKind.class,
            requiredText(
                node,
                "kind"
            ),
            "classicalExpression.kind"
        );
        return switch (kind) {
            case INTEGER -> ClassicalExpression.integer(requiredLong(
                node,
                "value"
            ));
            case VARIABLE_REFERENCE -> ClassicalExpression.variable(requiredText(
                node,
                "name"
            ));
            case BINARY_OPERATION -> ClassicalExpression.binary(
                enumValue(
                    ClassicalBinaryOperator.class,
                    requiredText(
                        node,
                        "operator"
                    ),
                    "classicalExpression.operator"
                ),
                readClassicalExpression(
                    requiredObject(
                        node,
                        "left"
                    ),
                    state
                ),
                readClassicalExpression(
                    requiredObject(
                        node,
                        "right"
                    ),
                    state
                )
            );
            case BIT_REFERENCE -> ClassicalExpression.bit(readClassicalBit(
                requiredText(
                    node,
                    "bit"
                ),
                state
            ));
            case REGISTER_REFERENCE -> ClassicalExpression.register(state.classicalRegister(requiredText(
                node,
                "register"
            )));
            case SYMBOLIC_REFERENCE -> ClassicalExpression.symbolicReference(requiredText(
                node,
                "text"
            ));
            case CALL -> ClassicalExpression.call(
                requiredText(
                    node,
                    "callable"
                ),
                readClassicalExpressionList(
                    requiredArray(
                        node,
                        "arguments"
                    ),
                    state
                )
            );
        };
    }

    private static List<ClassicalExpression> readClassicalExpressionList(
        final JsonNode node,
        final CircuitState state
    ) {
        final ArrayList<ClassicalExpression> result = new ArrayList<>();
        for (int i = 0; i < node.size(); i++) {
            result.add(readClassicalExpression(
                requiredArrayElementObject(
                    node,
                    i,
                    "classicalExpressions"
                ),
                state
            ));
        }
        return List.copyOf(result);
    }

    private static List<QuantumReference> readQuantumReferenceList(
        final JsonNode node,
        final CircuitState state
    ) {
        final ArrayList<QuantumReference> result = new ArrayList<>();
        for (int i = 0; i < node.size(); i++) {
            result.add(readOperationQuantumReference(
                requiredArrayElementObject(
                    node,
                    i,
                    "quantumReferences"
                ),
                state
            ));
        }
        return List.copyOf(result);
    }

    private static ClassicalPredicate readClassicalPredicate(
        final JsonNode node,
        final CircuitState state
    ) {
        final ClassicalPredicateKind kind = enumValue(
            ClassicalPredicateKind.class,
            requiredText(
                node,
                "kind"
            ),
            "classicalPredicate.kind"
        );
        return switch (kind) {
            case COMPARISON -> ClassicalPredicate.compare(
                readClassicalExpression(
                    requiredObject(
                        node,
                        "left"
                    ),
                    state
                ),
                enumValue(
                    ClassicalComparisonOperator.class,
                    requiredText(
                        node,
                        "operator"
                    ),
                    "classicalPredicate.operator"
                ),
                readClassicalExpression(
                    requiredObject(
                        node,
                        "right"
                    ),
                    state
                )
            );
            case NOT -> ClassicalPredicate.not(readClassicalPredicate(
                requiredObject(
                    node,
                    "predicate"
                ),
                state
            ));
            case BOOLEAN -> readBooleanPredicate(
                node,
                state
            );
        };
    }

    private static ClassicalPredicate readBooleanPredicate(
        final JsonNode node,
        final CircuitState state
    ) {
        final ClassicalPredicate left = readClassicalPredicate(
            requiredObject(
                node,
                "left"
            ),
            state
        );
        final ClassicalPredicate right = readClassicalPredicate(
            requiredObject(
                node,
                "right"
            ),
            state
        );
        final ClassicalBooleanOperator operator = enumValue(
            ClassicalBooleanOperator.class,
            requiredText(
                node,
                "operator"
            ),
            "classicalPredicate.operator"
        );
        if (operator == ClassicalBooleanOperator.AND) {
            return ClassicalPredicate.and(
                left,
                right
            );
        }
        return ClassicalPredicate.or(
            left,
            right
        );
    }

    private static OperationMetadata readMetadata(final JsonNode node) {
        ExternalSource source = null;
        SourceLocation location = null;
        if (node.has("source")) {
            final JsonNode sourceNode = requiredObject(
                node,
                "source"
            );
            source = new ExternalSource(
                requiredText(
                    sourceNode,
                    "format"
                ),
                requiredText(
                    sourceNode,
                    "description"
                )
            );
        }
        if (node.has("location")) {
            final JsonNode locationNode = requiredObject(
                node,
                "location"
            );
            location = new SourceLocation(
                requiredInt(
                    locationNode,
                    "line"
                ),
                requiredInt(
                    locationNode,
                    "column"
                )
            );
        }
        return new OperationMetadata(
            source,
            location
        );
    }

    private static Qubit[] readQubits(
        final JsonNode node,
        final CircuitState state
    ) {
        final Qubit[] result = new Qubit[node.size()];
        for (int i = 0; i < node.size(); i++) {
            final JsonNode item = node.get(i);
            if (!item.isTextual()) {
                throw invalidStructure("Qubit reference must be a string.");
            }
            result[i] = readQubit(
                item.asText(),
                state
            );
        }
        return result;
    }

    private static QuantumReference[] readOperationQuantumReferences(
        final JsonNode node,
        final CircuitState state
    ) {
        if (node.has("qubitReferences")) {
            return readQuantumReferences(
                requiredArray(
                    node,
                    "qubitReferences"
                ),
                state
            );
        }
        final Qubit[] qubits = readQubits(
            requiredArray(
                node,
                "qubits"
            ),
            state
        );
        final QuantumReference[] references = new QuantumReference[qubits.length];
        for (int i = 0; i < qubits.length; i++) {
            references[i] = QuantumReference.staticQubit(qubits[i]);
        }
        return references;
    }

    private static QuantumReference readOperationQuantumReference(
        final JsonNode node,
        final CircuitState state
    ) {
        if (node.has("qubitReference")) {
            return readQuantumReference(
                requiredObject(
                    node,
                    "qubitReference"
                ),
                state
            );
        }
        return QuantumReference.staticQubit(readQubit(
            requiredText(
                node,
                "qubit"
            ),
            state
        ));
    }

    private static QuantumReference[] readQuantumReferences(
        final JsonNode node,
        final CircuitState state
    ) {
        final QuantumReference[] result = new QuantumReference[node.size()];
        for (int i = 0; i < node.size(); i++) {
            result[i] = readQuantumReference(
                requiredArrayElementObject(
                    node,
                    i,
                    "qubitReferences"
                ),
                state
            );
        }
        return result;
    }

    private static QuantumReference readQuantumReference(
        final JsonNode node,
        final CircuitState state
    ) {
        final QuantumReferenceKind kind = enumValue(
            QuantumReferenceKind.class,
            requiredText(
                node,
                "kind"
            ),
            "quantumReference.kind"
        );
        return switch (kind) {
            case STATIC_QUBIT -> QuantumReference.staticQubit(readQubit(
                requiredText(
                    node,
                    "qubit"
                ),
                state
            ));
            case DYNAMIC_REGISTER_INDEX -> QuantumReference.dynamicIndex(
                state.quantumRegister(requiredText(
                    node,
                    "register"
                )),
                readClassicalExpression(
                    requiredObject(
                        node,
                        "index"
                    ),
                    state
                )
            );
            case HARDWARE_QUBIT -> QuantumReference.hardwareQubit(requiredInt(
                node,
                "hardwareIndex"
            ));
        };
    }

    private static Qubit readQubit(
        final String reference,
        final CircuitState state
    ) {
        final ReferenceParts parts = parseReference(reference);
        final QuantumRegister register = state.quantumRegister(parts.name());
        return Qubit.of(
            register,
            parts.index()
        );
    }

    private static ClassicalBit readClassicalBit(
        final String reference,
        final CircuitState state
    ) {
        final ReferenceParts parts = parseReference(reference);
        final ClassicalRegister register = state.classicalRegister(parts.name());
        return ClassicalBit.of(
            register,
            parts.index()
        );
    }

    private static ReferenceParts parseReference(final String reference) {
        if (
            reference == null
            || reference.isBlank()
        ) {
            throw invalidStructure("Bit reference must not be blank.");
        }
        final int openBracket = reference.indexOf('[');
        final int closeBracket = reference.indexOf(']');
        if (
            openBracket <= 0
            || closeBracket != reference.length() - 1
            || openBracket + 1 >= closeBracket
        ) {
            throw invalidStructure("Bit reference must have form name[index]: " + reference + ".");
        }
        final String name = reference.substring(
            0,
            openBracket
        );
        final long longIndex;
        try {
            longIndex = Long.parseLong(reference.substring(
                openBracket + 1,
                closeBracket
            ));
        } catch (final NumberFormatException exception) {
            throw invalidValue(exception);
        }
        if (
            longIndex < 0
            || longIndex > Integer.MAX_VALUE
        ) {
            throw error(
                PersistenceDiagnosticCode.INVALID_VALUE,
                "Bit reference index is outside Java int range: " + reference + "."
            );
        }
        return new ReferenceParts(
            name,
            (int) longIndex
        );
    }

    private static List<String> readTextList(final JsonNode node) {
        final ArrayList<String> result = new ArrayList<>();
        for (int i = 0; i < node.size(); i++) {
            final JsonNode item = node.get(i);
            if (!item.isTextual()) {
                throw invalidStructure("Array item must be a string.");
            }
            result.add(item.asText());
        }
        return result;
    }

    private static String[] readTextArray(final JsonNode node) {
        final String[] result = new String[node.size()];
        for (int i = 0; i < node.size(); i++) {
            final JsonNode item = node.get(i);
            if (!item.isTextual()) {
                throw invalidStructure("Array item must be a string.");
            }
            result[i] = item.asText();
        }
        return result;
    }

    private static JsonNode requiredObject(
        final JsonNode node,
        final String field
    ) {
        final JsonNode value = requiredField(
            node,
            field
        );
        if (!value.isObject()) {
            throw invalidStructure("Field must be an object: " + field + ".");
        }
        return value;
    }

    private static JsonNode requiredArray(
        final JsonNode node,
        final String field
    ) {
        final JsonNode value = requiredField(
            node,
            field
        );
        if (!value.isArray()) {
            throw invalidStructure("Field must be an array: " + field + ".");
        }
        return value;
    }

    private static JsonNode optionalArray(
        final JsonNode node,
        final String field
    ) {
        final JsonNode value = node.get(field);
        if (value == null) {
            return objectMapperMissingArray();
        }
        if (!value.isArray()) {
            throw invalidStructure("Field must be an array: " + field + ".");
        }
        return value;
    }

    private static JsonNode objectMapperMissingArray() {
        return new ObjectMapper().createArrayNode();
    }

    private static JsonNode requiredArrayElementObject(
        final JsonNode node,
        final int index,
        final String path
    ) {
        final JsonNode value = node.get(index);
        if (
            value == null
            || !value.isObject()
        ) {
            throw invalidStructure(path + "[" + index + "] must be an object.");
        }
        return value;
    }

    private static JsonNode requiredArrayElementArray(
        final JsonNode node,
        final int index,
        final String path
    ) {
        final JsonNode value = node.get(index);
        if (
            value == null
            || !value.isArray()
        ) {
            throw invalidStructure(path + "[" + index + "] must be an array.");
        }
        return value;
    }

    private static String requiredText(
        final JsonNode node,
        final String field
    ) {
        final JsonNode value = requiredField(
            node,
            field
        );
        if (!value.isTextual()) {
            throw invalidStructure("Field must be a string: " + field + ".");
        }
        return value.asText();
    }

    private static int requiredInt(
        final JsonNode node,
        final String field
    ) {
        final long value = requiredLong(
            node,
            field
        );
        if (
            value < Integer.MIN_VALUE
            || value > Integer.MAX_VALUE
        ) {
            throw error(
                PersistenceDiagnosticCode.INVALID_VALUE,
                "Field is outside Java int range: " + field + "."
            );
        }
        return (int) value;
    }

    private static long requiredLong(
        final JsonNode node,
        final String field
    ) {
        final JsonNode value = requiredField(
            node,
            field
        );
        if (!value.canConvertToLong()) {
            throw invalidStructure("Field must be an integer: " + field + ".");
        }
        return value.asLong();
    }

    private static double requiredDouble(
        final JsonNode node,
        final String field
    ) {
        final JsonNode value = requiredField(
            node,
            field
        );
        if (!value.isNumber()) {
            throw invalidStructure("Field must be numeric: " + field + ".");
        }
        final double result = value.asDouble();
        if (!Double.isFinite(result)) {
            throw error(
                PersistenceDiagnosticCode.INVALID_VALUE,
                "Field must be finite: " + field + "."
            );
        }
        return result;
    }

    private static JsonNode requiredField(
        final JsonNode node,
        final String field
    ) {
        if (
            node == null
            || !node.has(field)
            || node.get(field).isNull()
        ) {
            throw invalidStructure("Required field is missing: " + field + ".");
        }
        return node.get(field);
    }

    private static <T extends Enum<T>> T enumValue(
        final Class<T> enumClass,
        final String name,
        final String field
    ) {
        try {
            return Enum.valueOf(
                enumClass,
                name
            );
        } catch (final IllegalArgumentException exception) {
            throw error(
                PersistenceDiagnosticCode.INVALID_VALUE,
                "Unknown enum value for " + field + ": " + name + "."
            );
        }
    }

    private static void validateProgram(
        final QuantumProgram program,
        final ReadState state
    ) {
        final ValidationResult result = new QuantumProgramValidator().validate(program);
        for (int i = 0; i < result.errorCount(); i++) {
            final ValidationError error = result.error(i);
            state.addDiagnostic(PersistenceDiagnostic.error(
                PersistenceDiagnosticCode.INVALID_VALUE,
                "Domain validation failed after Quantum IR JSON read: " + error.message()
            ));
        }
    }

    private static ReadException invalidStructure(final String message) {
        return error(
            PersistenceDiagnosticCode.INVALID_STRUCTURE,
            message
        );
    }

    private static ReadException invalidValue(final Throwable exception) {
        return error(
            PersistenceDiagnosticCode.INVALID_VALUE,
            exception.getMessage()
        );
    }

    private static ReadException error(
        final PersistenceDiagnosticCode code,
        final String message
    ) {
        return new ReadException(PersistenceDiagnostic.error(
            code,
            message
        ));
    }

    private record ReferenceParts(
        String name,
        int index
    ) {
    }

    private static final class ReadState {

        private final LinkedHashMap<String, GateDefinition> gateDefinitions;
        private final ArrayList<PersistenceDiagnostic> diagnostics;

        private ReadState() {
            this.gateDefinitions = new LinkedHashMap<>();
            this.diagnostics = new ArrayList<>();
        }

        private void addGateDefinition(final GateDefinition definition) {
            gateDefinitions.put(
                definition.gateName(),
                definition
            );
        }

        private GateDefinition gateDefinition(final String name) {
            return gateDefinitions.get(name);
        }

        private void addDiagnostic(final PersistenceDiagnostic diagnostic) {
            diagnostics.add(diagnostic);
        }

        private boolean hasErrors() {
            for (int i = 0; i < diagnostics.size(); i++) {
                if (diagnostics.get(i).isError()) {
                    return true;
                }
            }
            return false;
        }

        private List<PersistenceDiagnostic> diagnostics() {
            return diagnostics;
        }
    }

    private static final class CircuitState {

        private final LinkedHashMap<String, QuantumRegister> quantumRegisters;
        private final LinkedHashMap<String, ClassicalRegister> classicalRegisters;

        private CircuitState() {
            this.quantumRegisters = new LinkedHashMap<>();
            this.classicalRegisters = new LinkedHashMap<>();
        }

        private void addQuantumRegister(final QuantumRegister register) {
            quantumRegisters.put(
                register.name().value(),
                register
            );
        }

        private void addClassicalRegister(final ClassicalRegister register) {
            classicalRegisters.put(
                register.name().value(),
                register
            );
        }

        private QuantumRegister quantumRegister(final String name) {
            final QuantumRegister register = quantumRegisters.get(name);
            if (register == null) {
                throw error(
                    PersistenceDiagnosticCode.UNKNOWN_REFERENCE,
                    "Quantum register reference is unknown: " + name + "."
                );
            }
            return register;
        }

        private ClassicalRegister classicalRegister(final String name) {
            final ClassicalRegister register = classicalRegisters.get(name);
            if (register == null) {
                throw error(
                    PersistenceDiagnosticCode.UNKNOWN_REFERENCE,
                    "Classical register reference is unknown: " + name + "."
                );
            }
            return register;
        }
    }

    private static final class ReadException extends RuntimeException {

        private final PersistenceDiagnostic diagnostic;

        private ReadException(final PersistenceDiagnostic diagnostic) {
            super(diagnostic.message());
            this.diagnostic = diagnostic;
        }

        private PersistenceDiagnostic diagnostic() {
            return diagnostic;
        }
    }
}
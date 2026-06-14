/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.openqasm3.normalization;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnostic;
import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnosticCode;
import ru.pathcreator.vadim.quantum.application.integration.decomposition.GateDecomposition;
import ru.pathcreator.vadim.quantum.application.integration.decomposition.GateDecompositionRule;
import ru.pathcreator.vadim.quantum.application.integration.options.ExportOptions;
import ru.pathcreator.vadim.quantum.domain.bit.ClassicalBit;
import ru.pathcreator.vadim.quantum.domain.bit.Qubit;
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
import ru.pathcreator.vadim.quantum.domain.gate.ParameterBinaryOperator;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpressionKind;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumComputationModel;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.BarrierOperation;
import ru.pathcreator.vadim.quantum.domain.operation.BlockOperation;
import ru.pathcreator.vadim.quantum.domain.operation.CallableInvocationOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalArrayDeclarationOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalAssignmentOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalDeclarationOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalCondition;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicallyControlledOperation;
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
import ru.pathcreator.vadim.quantum.infrastructure.openqasm3.mapping.OpenQasm3GateMapper;
import ru.pathcreator.vadim.quantum.infrastructure.openqasm3.mapping.OpenQasm3GateNames;

/**
 * Adapter-level lowering pass, приводящий универсальный Quantum IR к форме, которую умеет записать OpenQASM 3 writer.
 */
public final class OpenQasm3ExportNormalizer {

    private static final String NORMALIZED_CIRCUIT_NAME = "main";

    /**
     * Нормализует программу без изменения исходного IR.
     *
     * @param program исходная программа
     * @return результат нормализации
     */
    public OpenQasm3ExportNormalizationResult normalize(
        final QuantumProgram program,
        final ExportOptions options
    ) {
        if (program == null) {
            throw new IllegalArgumentException("Quantum program must not be null.");
        }
        if (options == null) {
            throw new IllegalArgumentException("Export options must not be null.");
        }
        final ArrayList<IntegrationDiagnostic> diagnostics = new ArrayList<>();
        if (program.computationModel() != QuantumComputationModel.GATE_BASED_CIRCUIT) {
            diagnostics.add(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.UNSUPPORTED_COMPUTATION_MODEL,
                "OpenQASM 3 export supports only GATE_BASED_CIRCUIT programs."
            ));
            return OpenQasm3ExportNormalizationResult.failure(diagnostics);
        }
        if (!registerNamesCanBeMerged(program, diagnostics)) {
            return OpenQasm3ExportNormalizationResult.failure(diagnostics);
        }

        final QuantumProgram normalized = QuantumProgram.gateBased();
        final NormalizationState state = new NormalizationState(
            normalized,
            diagnostics,
            options
        );
        copyProgramLevelDefinitions(
            program,
            state
        );
        if (hasErrors(diagnostics)) {
            return OpenQasm3ExportNormalizationResult.failure(diagnostics);
        }

        final QuantumCircuit target = normalized.createCircuit(NORMALIZED_CIRCUIT_NAME);
        final Remap remap = copyRegisters(
            program,
            target
        );
        for (int i = 0; i < program.circuitCount(); i++) {
            final QuantumCircuit circuit = program.circuit(i);
            for (int j = 0; j < circuit.operationCount(); j++) {
                appendNormalizedOperation(
                    target,
                    circuit.operation(j),
                    remap,
                    state
                );
            }
        }
        if (hasErrors(diagnostics)) {
            return OpenQasm3ExportNormalizationResult.failure(diagnostics);
        }
        return OpenQasm3ExportNormalizationResult.success(
            normalized,
            diagnostics
        );
    }

    private static void copyProgramLevelDefinitions(
        final QuantumProgram program,
        final NormalizationState state
    ) {
        for (int i = 0; i < program.gateDefinitionCount(); i++) {
            final GateDefinition definition = program.gateDefinition(i);
            if (OpenQasm3GateNames.isReservedStdAlias(definition.gateName())) {
                state.diagnostics.add(IntegrationDiagnostic.error(
                    IntegrationDiagnosticCode.UNSUPPORTED_GATE,
                    "OpenQASM 3 export cannot emit custom gate definition with a standard-library name: "
                        + definition.gateName()
                        + "."
                ));
            } else if (definition.kind() == GateDefinitionKind.COMPOSITE) {
                state.addDefinition(definition);
            } else if (definition.kind() == GateDefinitionKind.OPAQUE) {
                state.diagnostics.add(IntegrationDiagnostic.error(
                    IntegrationDiagnosticCode.UNSUPPORTED_GATE,
                    "OpenQASM 3 export cannot emit opaque gate without a gate body or defcal body: "
                        + definition.gateName()
                        + "."
                ));
            }
        }
        for (int i = 0; i < program.calibrationDefinitionCount(); i++) {
            state.normalizedProgram.addCalibrationDefinition(program.calibrationDefinition(i));
        }
        for (int i = 0; i < program.externalCallableDeclarationCount(); i++) {
            state.normalizedProgram.addExternalCallableDeclaration(program.externalCallableDeclaration(i));
        }
    }

    private static boolean registerNamesCanBeMerged(
        final QuantumProgram program,
        final ArrayList<IntegrationDiagnostic> diagnostics
    ) {
        final HashSet<String> names = new HashSet<>();
        for (int i = 0; i < program.circuitCount(); i++) {
            final QuantumCircuit circuit = program.circuit(i);
            for (int j = 0; j < circuit.quantumRegisterCount(); j++) {
                final String name = circuit.quantumRegister(j).name().value();
                if (!names.add(name)) {
                    diagnostics.add(duplicateRegisterDiagnostic(name));
                    return false;
                }
            }
            for (int j = 0; j < circuit.classicalRegisterCount(); j++) {
                final String name = circuit.classicalRegister(j).name().value();
                if (!names.add(name)) {
                    diagnostics.add(duplicateRegisterDiagnostic(name));
                    return false;
                }
            }
        }
        return true;
    }

    private static IntegrationDiagnostic duplicateRegisterDiagnostic(final String name) {
        return IntegrationDiagnostic.error(
            IntegrationDiagnosticCode.UNSUPPORTED_CIRCUIT_STRUCTURE,
            "OpenQASM 3 export cannot merge circuits with duplicate register name: " + name + "."
        );
    }

    private static Remap copyRegisters(
        final QuantumProgram source,
        final QuantumCircuit target
    ) {
        final Remap remap = new Remap();
        for (int i = 0; i < source.circuitCount(); i++) {
            final QuantumCircuit circuit = source.circuit(i);
            for (int j = 0; j < circuit.quantumRegisterCount(); j++) {
                final QuantumRegister sourceRegister = circuit.quantumRegister(j);
                final QuantumRegister targetRegister = target.createQuantumRegister(
                    sourceRegister.name().value(),
                    sourceRegister.size()
                );
                remap.mapQuantumRegister(
                    sourceRegister,
                    targetRegister
                );
            }
            for (int j = 0; j < circuit.classicalRegisterCount(); j++) {
                final ClassicalRegister sourceRegister = circuit.classicalRegister(j);
                final ClassicalRegister targetRegister = target.createClassicalRegister(
                    sourceRegister.name().value(),
                    sourceRegister.size()
                );
                remap.mapClassicalRegister(
                    sourceRegister,
                    targetRegister
                );
            }
        }
        return remap;
    }

    private static void appendNormalizedOperation(
        final QuantumCircuit target,
        final Operation operation,
        final Remap remap,
        final NormalizationState state
    ) {
        if (operation instanceof GateOperation gateOperation) {
            appendNormalizedGateOperation(
                target,
                remapGateOperation(
                    gateOperation,
                    remap
                ),
                state,
                null,
                null,
                new HashSet<>()
            );
        } else if (operation instanceof MeasureOperation measureOperation) {
            appendOperation(
                target,
                new MeasureOperation(
                    remapReference(
                        measureOperation.qubitReference(),
                        remap
                    ),
                    remap.classicalBit(measureOperation.bit())
                ),
                null,
                null
            );
        } else if (operation instanceof ResetOperation resetOperation) {
            appendOperation(
                target,
                new ResetOperation(remapReference(
                    resetOperation.qubitReference(),
                    remap
                )),
                null,
                null
            );
        } else if (operation instanceof BarrierOperation barrierOperation) {
            appendOperation(
                target,
                remapBarrier(barrierOperation, remap),
                null,
                null
            );
        } else if (operation instanceof ControlledOperation controlledOperation) {
            appendControlledNestedOperation(
                target,
                controlledOperation.operation(),
                remap,
                state,
                ClassicalCondition.equalTo(
                    remap.classicalRegister(controlledOperation.condition().register()),
                    controlledOperation.condition().expectedValue()
                ),
                null
            );
        } else if (operation instanceof ClassicallyControlledOperation controlledOperation) {
            appendControlledNestedOperation(
                target,
                controlledOperation.operation(),
                remap,
                state,
                null,
                remapPredicate(
                    controlledOperation.predicate(),
                    remap
                )
            );
        } else if (operation instanceof ClassicalAssignmentOperation assignmentOperation) {
            appendOperation(
                target,
                new ClassicalAssignmentOperation(remapAssignment(
                    assignmentOperation.assignment(),
                    remap
                )),
                null,
                null
            );
        } else if (operation instanceof ClassicalDeclarationOperation declarationOperation) {
            appendOperation(
                target,
                new ClassicalDeclarationOperation(
                    declarationOperation.declaration(),
                    declarationOperation.hasInitializer()
                        ? remapExpression(
                            declarationOperation.initializer(),
                            remap
                        )
                        : null
                ),
                null,
                null
            );
        } else if (operation instanceof ClassicalArrayDeclarationOperation arrayOperation) {
            appendOperation(
                target,
                remapClassicalArrayDeclaration(arrayOperation, remap),
                null,
                null
            );
        } else if (operation instanceof CallableInvocationOperation invocationOperation) {
            appendOperation(
                target,
                remapCallableInvocation(invocationOperation, remap),
                null,
                null
            );
        } else if (operation instanceof BlockOperation blockOperation) {
            appendOperation(
                target,
                new BlockOperation(remapOperationBlock(
                    blockOperation.body(),
                    remap,
                    state
                )),
                null,
                null
            );
        } else if (operation instanceof ConditionalBlockOperation conditionalOperation) {
            appendOperation(
                target,
                new ConditionalBlockOperation(
                    remapPredicate(
                        conditionalOperation.predicate(),
                        remap
                    ),
                    remapOperationBlock(
                        conditionalOperation.thenBlock(),
                        remap,
                        state
                    ),
                    conditionalOperation.hasElseBlock()
                        ? remapOperationBlock(
                            conditionalOperation.elseBlock(),
                            remap,
                            state
                        )
                        : null
                ),
                null,
                null
            );
        } else if (operation instanceof ForLoopOperation loopOperation) {
            appendOperation(
                target,
                new ForLoopOperation(
                    loopOperation.variableName(),
                    loopOperation.startInclusive(),
                    loopOperation.step(),
                    loopOperation.endInclusive(),
                    remapOperationBlock(
                        loopOperation.body(),
                        remap,
                        state
                    )
                ),
                null,
                null
            );
        } else if (operation instanceof WhileLoopOperation loopOperation) {
            appendOperation(
                target,
                new WhileLoopOperation(
                    remapPredicate(
                        loopOperation.predicate(),
                        remap
                    ),
                    remapOperationBlock(
                        loopOperation.body(),
                        remap,
                        state
                    )
                ),
                null,
                null
            );
        } else if (operation instanceof DelayOperation delayOperation) {
            appendOperation(
                target,
                remapDelayOperation(
                    delayOperation,
                    remap
                ),
                null,
                null
            );
        } else if (operation instanceof SymbolicForLoopOperation loopOperation) {
            appendOperation(
                target,
                new SymbolicForLoopOperation(
                    loopOperation.variableName(),
                    loopOperation.hasVariableTypeText()
                        ? loopOperation.variableTypeText()
                        : null,
                    remapExpression(
                        loopOperation.startInclusive(),
                        remap
                    ),
                    remapExpression(
                        loopOperation.step(),
                        remap
                    ),
                    remapExpression(
                        loopOperation.endInclusive(),
                        remap
                    ),
                    remapOperationBlock(
                        loopOperation.body(),
                        remap,
                        state
                    )
                ),
                null,
                null
            );
        } else if (operation instanceof TimingBoxOperation boxOperation) {
            appendOperation(
                target,
                new TimingBoxOperation(
                    boxOperation.hasDuration()
                        ? boxOperation.duration()
                        : null,
                    remapOperationBlock(
                        boxOperation.body(),
                        remap,
                        state
                    )
                ),
                null,
                null
            );
        } else {
            state.diagnostics.add(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.UNSUPPORTED_OPERATION,
                "OpenQASM 3 export does not support operation kind: " + operation.kind() + "."
            ));
        }
    }

    private static void appendControlledNestedOperation(
        final QuantumCircuit target,
        final Operation operation,
        final Remap remap,
        final NormalizationState state,
        final ClassicalCondition condition,
        final ClassicalPredicate predicate
    ) {
        if (operation instanceof GateOperation gateOperation) {
            appendNormalizedGateOperation(
                target,
                remapGateOperation(
                    gateOperation,
                    remap
                ),
                state,
                condition,
                predicate,
                new HashSet<>()
            );
        } else if (operation instanceof MeasureOperation measureOperation) {
            appendOperation(
                target,
                new MeasureOperation(
                    remapReference(
                        measureOperation.qubitReference(),
                        remap
                    ),
                    remap.classicalBit(measureOperation.bit())
                ),
                condition,
                predicate
            );
        } else if (operation instanceof ResetOperation resetOperation) {
            appendOperation(
                target,
                new ResetOperation(remapReference(
                    resetOperation.qubitReference(),
                    remap
                )),
                condition,
                predicate
            );
        } else if (operation instanceof BarrierOperation barrierOperation) {
            appendOperation(
                target,
                remapBarrier(barrierOperation, remap),
                condition,
                predicate
            );
        } else if (operation instanceof ClassicalAssignmentOperation assignmentOperation) {
            appendOperation(
                target,
                new ClassicalAssignmentOperation(remapAssignment(
                    assignmentOperation.assignment(),
                    remap
                )),
                condition,
                predicate
            );
        } else if (operation instanceof ClassicalDeclarationOperation declarationOperation) {
            appendOperation(
                target,
                new ClassicalDeclarationOperation(
                    declarationOperation.declaration(),
                    declarationOperation.hasInitializer()
                        ? remapExpression(
                            declarationOperation.initializer(),
                            remap
                        )
                        : null
                ),
                condition,
                predicate
            );
        } else if (operation instanceof ClassicalArrayDeclarationOperation arrayOperation) {
            appendOperation(
                target,
                remapClassicalArrayDeclaration(arrayOperation, remap),
                condition,
                predicate
            );
        } else if (operation instanceof CallableInvocationOperation invocationOperation) {
            appendOperation(
                target,
                remapCallableInvocation(invocationOperation, remap),
                condition,
                predicate
            );
        } else if (operation instanceof BlockOperation blockOperation) {
            appendOperation(
                target,
                new BlockOperation(remapOperationBlock(
                    blockOperation.body(),
                    remap,
                    state
                )),
                condition,
                predicate
            );
        } else if (operation instanceof ConditionalBlockOperation conditionalOperation) {
            appendOperation(
                target,
                new ConditionalBlockOperation(
                    remapPredicate(
                        conditionalOperation.predicate(),
                        remap
                    ),
                    remapOperationBlock(
                        conditionalOperation.thenBlock(),
                        remap,
                        state
                    ),
                    conditionalOperation.hasElseBlock()
                        ? remapOperationBlock(
                            conditionalOperation.elseBlock(),
                            remap,
                            state
                        )
                        : null
                ),
                condition,
                predicate
            );
        } else if (operation instanceof ForLoopOperation loopOperation) {
            appendOperation(
                target,
                new ForLoopOperation(
                    loopOperation.variableName(),
                    loopOperation.startInclusive(),
                    loopOperation.step(),
                    loopOperation.endInclusive(),
                    remapOperationBlock(
                        loopOperation.body(),
                        remap,
                        state
                    )
                ),
                condition,
                predicate
            );
        } else if (operation instanceof WhileLoopOperation loopOperation) {
            appendOperation(
                target,
                new WhileLoopOperation(
                    remapPredicate(
                        loopOperation.predicate(),
                        remap
                    ),
                    remapOperationBlock(
                        loopOperation.body(),
                        remap,
                        state
                    )
                ),
                condition,
                predicate
            );
        } else if (operation instanceof DelayOperation delayOperation) {
            appendOperation(
                target,
                remapDelayOperation(
                    delayOperation,
                    remap
                ),
                condition,
                predicate
            );
        } else if (operation instanceof TimingBoxOperation boxOperation) {
            appendOperation(
                target,
                new TimingBoxOperation(
                    boxOperation.hasDuration()
                        ? boxOperation.duration()
                        : null,
                    remapOperationBlock(
                        boxOperation.body(),
                        remap,
                        state
                    )
                ),
                condition,
                predicate
            );
        } else {
            state.diagnostics.add(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.UNSUPPORTED_OPERATION,
                "OpenQASM 3 export does not support nested operation kind: " + operation.kind() + "."
            ));
        }
    }

    private static void appendNormalizedGateOperation(
        final QuantumCircuit target,
        final GateOperation operation,
        final NormalizationState state,
        final ClassicalCondition condition,
        final ClassicalPredicate predicate,
        final HashSet<String> decomposingGateNames
    ) {
        final Gate gate = operation.gate();
        if (
            gate instanceof GateDefinition definition
            && definition.kind() == GateDefinitionKind.COMPOSITE
        ) {
            appendCompositeGateOperation(
                target,
                operation,
                definition,
                state,
                condition,
                predicate,
                new HashSet<>(),
                decomposingGateNames
            );
            return;
        }
        if (!OpenQasm3GateMapper.isExportSupported(gate)) {
            final GateDecompositionRule rule = state.options.gateDecompositionRegistry().findRule(gate);
            if (rule != null) {
                appendDecomposedGateOperation(
                    target,
                    operation,
                    rule,
                    state,
                    condition,
                    predicate,
                    decomposingGateNames
                );
                return;
            }
        }
        final Gate exportGate = normalizeLeafGate(
            gate,
            state
        );
        if (exportGate == null) {
            return;
        }
        appendOperation(
            target,
            GateOperation.parameterizedReferences(
                exportGate,
                operation.parameters(),
                operation.qubitReferences()
            ),
            condition,
            predicate
        );
    }

    private static void appendCompositeGateOperation(
        final QuantumCircuit target,
        final GateOperation operation,
        final GateDefinition definition,
        final NormalizationState state,
        final ClassicalCondition condition,
        final ClassicalPredicate predicate,
        final HashSet<String> expandingGateNames,
        final HashSet<String> decomposingGateNames
    ) {
        if (!expandingGateNames.add(definition.gateName())) {
            state.diagnostics.add(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.UNSUPPORTED_GATE,
                "OpenQASM 3 export cannot inline cyclic composite gate: " + definition.gateName() + "."
            ));
            return;
        }
        final Map<String, ParameterExpression> parameterMap = parameterMap(
            definition,
            operation
        );
        final Map<String, QuantumReference> qubitMap = qubitMap(
            definition,
            operation
        );
        for (int i = 0; i < definition.bodyOperations().size(); i++) {
            final GateBodyOperation bodyOperation = definition.bodyOperations().get(i);
            final GateOperation expandedOperation = expandBodyOperation(
                bodyOperation,
                parameterMap,
                qubitMap
            );
            if (
                expandedOperation.gate() instanceof GateDefinition nestedDefinition
                && nestedDefinition.kind() == GateDefinitionKind.COMPOSITE
            ) {
                appendCompositeGateOperation(
                    target,
                    expandedOperation,
                    nestedDefinition,
                    state,
                    condition,
                    predicate,
                    expandingGateNames,
                    decomposingGateNames
                );
            } else {
                appendNormalizedGateOperation(
                    target,
                    expandedOperation,
                    state,
                    condition,
                    predicate,
                    decomposingGateNames
                );
            }
        }
        expandingGateNames.remove(definition.gateName());
    }

    private static void appendDecomposedGateOperation(
        final QuantumCircuit target,
        final GateOperation operation,
        final GateDecompositionRule rule,
        final NormalizationState state,
        final ClassicalCondition condition,
        final ClassicalPredicate predicate,
        final HashSet<String> decomposingGateNames
    ) {
        final String gateName = operation.gate().gateName();
        if (!decomposingGateNames.add(gateName)) {
            state.diagnostics.add(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.UNSUPPORTED_GATE,
                "OpenQASM 3 export detected cyclic decomposition for gate: " + gateName + "."
            ));
            return;
        }
        final GateDecomposition decomposition = rule.decompose(operation);
        if (decomposition == null) {
            state.diagnostics.add(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.UNSUPPORTED_GATE,
                "Gate decomposition rule returned no decomposition for gate: " + gateName + "."
            ));
            decomposingGateNames.remove(gateName);
            return;
        }
        for (int i = 0; i < decomposition.operationCount(); i++) {
            appendNormalizedGateOperation(
                target,
                decomposition.operation(i),
                state,
                condition,
                predicate,
                decomposingGateNames
            );
        }
        decomposingGateNames.remove(gateName);
    }

    private static Gate normalizeLeafGate(
        final Gate gate,
        final NormalizationState state
    ) {
        if (OpenQasm3GateMapper.isExportSupported(gate)) {
            return gate;
        }
        if (gate instanceof GateDefinition definition) {
            if (OpenQasm3GateNames.isReservedStdAlias(definition.gateName())) {
                state.diagnostics.add(IntegrationDiagnostic.error(
                    IntegrationDiagnosticCode.UNSUPPORTED_GATE,
                    "OpenQASM 3 export cannot emit custom gate with a standard-library name: "
                        + definition.gateName()
                        + "."
                ));
                return null;
            }
            if (definition.kind() == GateDefinitionKind.INTRINSIC) {
                state.diagnostics.add(IntegrationDiagnostic.error(
                    IntegrationDiagnosticCode.UNSUPPORTED_GATE,
                    "OpenQASM 3 export requires a decomposition rule for gate: "
                        + definition.gateName()
                        + "."
                ));
                return null;
            }
            state.addDefinition(definition);
            return definition;
        }
        state.diagnostics.add(IntegrationDiagnostic.error(
            IntegrationDiagnosticCode.UNSUPPORTED_GATE,
            "OpenQASM 3 export does not support gate: " + gate.gateName() + "."
        ));
        return null;
    }

    private static void appendOperation(
        final QuantumCircuit target,
        final Operation operation,
        final ClassicalCondition condition,
        final ClassicalPredicate predicate
    ) {
        if (condition != null) {
            target.controlled(
                condition,
                operation
            );
        } else if (predicate != null) {
            target.classicallyControlled(
                predicate,
                operation
            );
        } else if (operation instanceof GateOperation gateOperation) {
            if (gateOperation.parameterCount() == 0) {
                target.gateReferences(
                    gateOperation.gate(),
                    gateOperation.qubitReferences()
                );
            } else {
                target.parameterizedGateReferences(
                    gateOperation.gate(),
                    gateOperation.parameters(),
                    gateOperation.qubitReferences()
                );
            }
        } else if (operation instanceof MeasureOperation measureOperation) {
            target.measureReference(
                measureOperation.qubitReference(),
                measureOperation.bit()
            );
        } else if (operation instanceof ResetOperation resetOperation) {
            target.resetReference(resetOperation.qubitReference());
        } else if (operation instanceof BarrierOperation barrierOperation) {
            target.barrier(barrierOperation.qubits());
        } else if (operation instanceof ClassicalAssignmentOperation assignmentOperation) {
            target.assign(assignmentOperation.assignment());
        } else if (operation instanceof BlockOperation blockOperation) {
            target.block(blockOperation.body());
        } else if (operation instanceof ConditionalBlockOperation conditionalOperation) {
            target.conditionalBlock(
                conditionalOperation.predicate(),
                conditionalOperation.thenBlock(),
                conditionalOperation.hasElseBlock()
                    ? conditionalOperation.elseBlock()
                    : null
            );
        } else if (operation instanceof ForLoopOperation loopOperation) {
            target.forLoop(
                loopOperation.variableName(),
                loopOperation.startInclusive(),
                loopOperation.step(),
                loopOperation.endInclusive(),
                loopOperation.body()
            );
        } else if (operation instanceof WhileLoopOperation loopOperation) {
            target.whileLoop(
                loopOperation.predicate(),
                loopOperation.body()
            );
        } else if (operation instanceof DelayOperation delayOperation) {
            target.delay(
                delayOperation.duration(),
                delayOperation.qubits()
            );
        } else if (operation instanceof TimingBoxOperation boxOperation) {
            target.timingBox(
                boxOperation.hasDuration()
                    ? boxOperation.duration()
                    : null,
                boxOperation.body()
            );
        }
    }

    private static GateOperation remapGateOperation(
        final GateOperation operation,
        final Remap remap
    ) {
        final QuantumReference[] references = new QuantumReference[operation.qubitCount()];
        for (int i = 0; i < operation.qubitCount(); i++) {
            references[i] = remapReference(
                operation.qubitReference(i),
                remap
            );
        }
        return GateOperation.parameterizedReferences(
            operation.gate(),
            operation.parameters(),
            references
        );
    }

    private static QuantumReference remapReference(
        final QuantumReference reference,
        final Remap remap
    ) {
        if (reference.kind() == QuantumReferenceKind.STATIC_QUBIT) {
            return QuantumReference.staticQubit(remap.qubit(reference.qubit()));
        }
        if (reference.kind() == QuantumReferenceKind.HARDWARE_QUBIT) {
            return QuantumReference.hardwareQubit(reference.hardwareIndex());
        }
        return QuantumReference.dynamicIndex(
            remap.quantumRegister(reference.register()),
            remapExpression(
                reference.indexExpression(),
                remap
            )
        );
    }

    private static OperationBlock remapOperationBlock(
        final OperationBlock block,
        final Remap remap,
        final NormalizationState state
    ) {
        final Operation[] operations = new Operation[block.operationCount()];
        for (int i = 0; i < block.operationCount(); i++) {
            operations[i] = remapNestedOperation(
                block.operation(i),
                remap,
                state
            );
        }
        return OperationBlock.of(operations);
    }

    private static Operation remapNestedOperation(
        final Operation operation,
        final Remap remap,
        final NormalizationState state
    ) {
        if (operation instanceof GateOperation gateOperation) {
            final GateOperation remappedOperation = remapGateOperation(
                gateOperation,
                remap
            );
            final Gate exportGate = normalizeLeafGate(
                remappedOperation.gate(),
                state
            );
            if (exportGate == null) {
                return remappedOperation;
            }
            return GateOperation.parameterizedReferences(
                exportGate,
                remappedOperation.parameters(),
                remappedOperation.qubitReferences()
            );
        }
        if (operation instanceof MeasureOperation measureOperation) {
            return new MeasureOperation(
                remapReference(
                    measureOperation.qubitReference(),
                    remap
                ),
                remap.classicalBit(measureOperation.bit())
            );
        }
        if (operation instanceof ResetOperation resetOperation) {
            return new ResetOperation(remapReference(
                resetOperation.qubitReference(),
                remap
            ));
        }
        if (operation instanceof BarrierOperation barrierOperation) {
            return remapBarrier(
                barrierOperation,
                remap
            );
        }
        if (operation instanceof ClassicalAssignmentOperation assignmentOperation) {
            return new ClassicalAssignmentOperation(remapAssignment(
                assignmentOperation.assignment(),
                remap
            ));
        }
        if (operation instanceof ClassicalDeclarationOperation declarationOperation) {
            return new ClassicalDeclarationOperation(
                declarationOperation.declaration(),
                declarationOperation.hasInitializer()
                    ? remapExpression(
                        declarationOperation.initializer(),
                        remap
                    )
                    : null
            );
        }
        if (operation instanceof ClassicalArrayDeclarationOperation arrayOperation) {
            return remapClassicalArrayDeclaration(
                arrayOperation,
                remap
            );
        }
        if (operation instanceof CallableInvocationOperation invocationOperation) {
            return remapCallableInvocation(
                invocationOperation,
                remap
            );
        }
        if (operation instanceof BlockOperation blockOperation) {
            return new BlockOperation(remapOperationBlock(
                blockOperation.body(),
                remap,
                state
            ));
        }
        if (operation instanceof ConditionalBlockOperation conditionalOperation) {
            return new ConditionalBlockOperation(
                remapPredicate(
                    conditionalOperation.predicate(),
                    remap
                ),
                remapOperationBlock(
                    conditionalOperation.thenBlock(),
                    remap,
                    state
                ),
                conditionalOperation.hasElseBlock()
                    ? remapOperationBlock(
                        conditionalOperation.elseBlock(),
                        remap,
                        state
                    )
                    : null
            );
        }
        if (operation instanceof ForLoopOperation loopOperation) {
            return new ForLoopOperation(
                loopOperation.variableName(),
                loopOperation.startInclusive(),
                loopOperation.step(),
                loopOperation.endInclusive(),
                remapOperationBlock(
                    loopOperation.body(),
                    remap,
                    state
                )
            );
        }
        if (operation instanceof WhileLoopOperation loopOperation) {
            return new WhileLoopOperation(
                remapPredicate(
                    loopOperation.predicate(),
                    remap
                ),
                remapOperationBlock(
                    loopOperation.body(),
                    remap,
                    state
                )
            );
        }
        if (operation instanceof SymbolicForLoopOperation loopOperation) {
            return new SymbolicForLoopOperation(
                loopOperation.variableName(),
                loopOperation.hasVariableTypeText()
                    ? loopOperation.variableTypeText()
                    : null,
                remapExpression(
                    loopOperation.startInclusive(),
                    remap
                ),
                remapExpression(
                    loopOperation.step(),
                    remap
                ),
                remapExpression(
                    loopOperation.endInclusive(),
                    remap
                ),
                remapOperationBlock(
                    loopOperation.body(),
                    remap,
                    state
                )
            );
        }
        if (operation instanceof DelayOperation delayOperation) {
            return remapDelayOperation(
                delayOperation,
                remap
            );
        }
        if (operation instanceof TimingBoxOperation boxOperation) {
            return new TimingBoxOperation(
                boxOperation.hasDuration()
                    ? boxOperation.duration()
                    : null,
                remapOperationBlock(
                    boxOperation.body(),
                    remap,
                    state
                )
            );
        }
        state.diagnostics.add(IntegrationDiagnostic.error(
            IntegrationDiagnosticCode.UNSUPPORTED_OPERATION,
            "OpenQASM 3 export does not support operation kind inside block: " + operation.kind() + "."
        ));
        return operation;
    }

    private static DelayOperation remapDelayOperation(
        final DelayOperation operation,
        final Remap remap
    ) {
        final QuantumReference[] references = new QuantumReference[operation.qubitCount()];
        for (int i = 0; i < operation.qubitCount(); i++) {
            references[i] = remapReference(
                operation.reference(i),
                remap
            );
        }
        return new DelayOperation(
            operation.duration(),
            references
        );
    }

    private static ClassicalArrayDeclarationOperation remapClassicalArrayDeclaration(
        final ClassicalArrayDeclarationOperation operation,
        final Remap remap
    ) {
        final ArrayList<ClassicalExpression> dimensions = new ArrayList<>();
        for (int i = 0; i < operation.dimensionCount(); i++) {
            dimensions.add(remapExpression(
                operation.dimension(i),
                remap
            ));
        }
        return new ClassicalArrayDeclarationOperation(
            operation.name(),
            operation.elementType(),
            dimensions,
            operation.hasInitializerText()
                ? operation.initializerText()
                : null
        );
    }

    private static CallableInvocationOperation remapCallableInvocation(
        final CallableInvocationOperation operation,
        final Remap remap
    ) {
        final ArrayList<ClassicalExpression> classicalArguments = new ArrayList<>();
        for (int i = 0; i < operation.classicalArguments().size(); i++) {
            classicalArguments.add(remapExpression(
                operation.classicalArguments().get(i),
                remap
            ));
        }
        final ArrayList<QuantumReference> quantumArguments = new ArrayList<>();
        for (int i = 0; i < operation.quantumArguments().size(); i++) {
            quantumArguments.add(remapReference(
                operation.quantumArguments().get(i),
                remap
            ));
        }
        return new CallableInvocationOperation(
            operation.callableName(),
            operation.hasTarget()
                ? remapExpression(
                    operation.target(),
                    remap
                )
                : null,
            classicalArguments,
            quantumArguments
        );
    }

    private static BarrierOperation remapBarrier(
        final BarrierOperation operation,
        final Remap remap
    ) {
        final Qubit[] qubits = new Qubit[operation.qubitCount()];
        for (int i = 0; i < operation.qubitCount(); i++) {
            qubits[i] = remap.qubit(operation.qubit(i));
        }
        return new BarrierOperation(qubits);
    }

    private static ClassicalAssignment remapAssignment(
        final ClassicalAssignment assignment,
        final Remap remap
    ) {
        return new ClassicalAssignment(
            remapExpression(
                assignment.target(),
                remap
            ),
            remapExpression(
                assignment.value(),
                remap
            )
        );
    }

    private static ClassicalPredicate remapPredicate(
        final ClassicalPredicate predicate,
        final Remap remap
    ) {
        if (predicate.kind() == ClassicalPredicateKind.COMPARISON) {
            return ClassicalPredicate.compare(
                remapExpression(
                    predicate.leftExpression(),
                    remap
                ),
                predicate.comparisonOperator(),
                remapExpression(
                    predicate.rightExpression(),
                    remap
                )
            );
        }
        if (predicate.kind() == ClassicalPredicateKind.NOT) {
            return ClassicalPredicate.not(remapPredicate(
                predicate.leftPredicate(),
                remap
            ));
        }
        if (predicate.booleanOperator() == ClassicalBooleanOperator.AND) {
            return ClassicalPredicate.and(
                remapPredicate(
                    predicate.leftPredicate(),
                    remap
                ),
                remapPredicate(
                    predicate.rightPredicate(),
                    remap
                )
            );
        }
        return ClassicalPredicate.or(
            remapPredicate(
                predicate.leftPredicate(),
                remap
            ),
            remapPredicate(
                predicate.rightPredicate(),
                remap
            )
        );
    }

    private static ClassicalExpression remapExpression(
        final ClassicalExpression expression,
        final Remap remap
    ) {
        if (expression.kind() == ClassicalExpressionKind.INTEGER) {
            return ClassicalExpression.integer(expression.integerValue());
        }
        if (expression.kind() == ClassicalExpressionKind.VARIABLE_REFERENCE) {
            return ClassicalExpression.variable(expression.variableName());
        }
        if (expression.kind() == ClassicalExpressionKind.SYMBOLIC_REFERENCE) {
            return ClassicalExpression.symbolicReference(expression.symbolicText());
        }
        if (expression.kind() == ClassicalExpressionKind.CALL) {
            final ArrayList<ClassicalExpression> arguments = new ArrayList<>();
            for (int i = 0; i < expression.callArgumentCount(); i++) {
                arguments.add(remapExpression(
                    expression.callArgument(i),
                    remap
                ));
            }
            return ClassicalExpression.call(
                expression.callableName(),
                arguments
            );
        }
        if (expression.kind() == ClassicalExpressionKind.BINARY_OPERATION) {
            return ClassicalExpression.binary(
                expression.binaryOperator(),
                remapExpression(
                    expression.leftExpression(),
                    remap
                ),
                remapExpression(
                    expression.rightExpression(),
                    remap
                )
            );
        }
        if (expression.kind() == ClassicalExpressionKind.BIT_REFERENCE) {
            return ClassicalExpression.bit(remap.classicalBit(expression.bit()));
        }
        return ClassicalExpression.register(remap.classicalRegister(expression.register()));
    }

    private static Map<String, ParameterExpression> parameterMap(
        final GateDefinition definition,
        final GateOperation operation
    ) {
        final LinkedHashMap<String, ParameterExpression> result = new LinkedHashMap<>();
        for (int i = 0; i < definition.parameterNames().size(); i++) {
            result.put(
                definition.parameterNames().get(i),
                operation.parameter(i)
            );
        }
        return Map.copyOf(result);
    }

    private static Map<String, QuantumReference> qubitMap(
        final GateDefinition definition,
        final GateOperation operation
    ) {
        final LinkedHashMap<String, QuantumReference> result = new LinkedHashMap<>();
        for (int i = 0; i < definition.qubitNames().size(); i++) {
            result.put(
                definition.qubitNames().get(i),
                operation.qubitReference(i)
            );
        }
        return Map.copyOf(result);
    }

    private static GateOperation expandBodyOperation(
        final GateBodyOperation operation,
        final Map<String, ParameterExpression> parameterMap,
        final Map<String, QuantumReference> qubitMap
    ) {
        final QuantumReference[] references = new QuantumReference[operation.qubitCount()];
        for (int i = 0; i < operation.qubitCount(); i++) {
            final QuantumReference reference = qubitMap.get(operation.qubitName(i));
            if (reference == null) {
                throw new IllegalArgumentException("Composite gate body references an unknown qubit argument.");
            }
            references[i] = reference;
        }
        final ParameterExpression[] parameters = new ParameterExpression[operation.parameterCount()];
        for (int i = 0; i < operation.parameterCount(); i++) {
            parameters[i] = substituteParameters(
                operation.parameter(i),
                parameterMap
            );
        }
        return GateOperation.parameterizedReferences(
            operation.gate(),
            parameters,
            references
        );
    }

    private static ParameterExpression substituteParameters(
        final ParameterExpression expression,
        final Map<String, ParameterExpression> parameterMap
    ) {
        if (expression.kind() == ParameterExpressionKind.NAMED) {
            final ParameterExpression replacement = parameterMap.get(expression.name());
            if (replacement == null) {
                throw new IllegalArgumentException("Composite gate body references an unknown parameter argument.");
            }
            return replacement;
        }
        if (
            expression.kind() == ParameterExpressionKind.NUMERIC
            || expression.kind() == ParameterExpressionKind.KNOWN_CONSTANT
        ) {
            return expression;
        }
        if (expression.kind() == ParameterExpressionKind.UNARY) {
            return ParameterExpression.negate(substituteParameters(
                expression.left(),
                parameterMap
            ));
        }
        return rebuildBinary(
            expression.binaryOperator(),
            substituteParameters(
                expression.left(),
                parameterMap
            ),
            substituteParameters(
                expression.right(),
                parameterMap
            )
        );
    }

    private static ParameterExpression rebuildBinary(
        final ParameterBinaryOperator operator,
        final ParameterExpression left,
        final ParameterExpression right
    ) {
        if (operator == ParameterBinaryOperator.ADD) {
            return ParameterExpression.add(
                left,
                right
            );
        }
        if (operator == ParameterBinaryOperator.SUBTRACT) {
            return ParameterExpression.subtract(
                left,
                right
            );
        }
        if (operator == ParameterBinaryOperator.MULTIPLY) {
            return ParameterExpression.multiply(
                left,
                right
            );
        }
        return ParameterExpression.divide(
            left,
            right
        );
    }

    private static boolean hasErrors(final ArrayList<IntegrationDiagnostic> diagnostics) {
        for (int i = 0; i < diagnostics.size(); i++) {
            if (diagnostics.get(i).isError()) {
                return true;
            }
        }
        return false;
    }

    private static final class NormalizationState {

        private final QuantumProgram normalizedProgram;
        private final ArrayList<IntegrationDiagnostic> diagnostics;
        private final ExportOptions options;
        private final LinkedHashMap<String, GateDefinition> definitionsByName;

        private NormalizationState(
            final QuantumProgram normalizedProgram,
            final ArrayList<IntegrationDiagnostic> diagnostics,
            final ExportOptions options
        ) {
            this.normalizedProgram = normalizedProgram;
            this.diagnostics = diagnostics;
            this.options = options;
            this.definitionsByName = new LinkedHashMap<>();
        }

        private void addDefinition(final GateDefinition definition) {
            if (!definitionsByName.containsKey(definition.gateName())) {
                definitionsByName.put(
                    definition.gateName(),
                    definition
                );
                normalizedProgram.addGateDefinition(definition);
            }
        }

        private GateDefinition definition(final String name) {
            return definitionsByName.get(name);
        }
    }

    private static final class Remap {

        private final LinkedHashMap<QuantumRegister, QuantumRegister> quantumRegisters;
        private final LinkedHashMap<Qubit, Qubit> qubits;
        private final LinkedHashMap<ClassicalBit, ClassicalBit> classicalBits;
        private final LinkedHashMap<ClassicalRegister, ClassicalRegister> classicalRegisters;

        private Remap() {
            this.quantumRegisters = new LinkedHashMap<>();
            this.qubits = new LinkedHashMap<>();
            this.classicalBits = new LinkedHashMap<>();
            this.classicalRegisters = new LinkedHashMap<>();
        }

        private void mapQuantumRegister(
            final QuantumRegister source,
            final QuantumRegister target
        ) {
            quantumRegisters.put(
                source,
                target
            );
            for (int i = 0; i < source.size(); i++) {
                qubits.put(
                    source.get(i),
                    target.get(i)
                );
            }
        }

        private void mapClassicalRegister(
            final ClassicalRegister source,
            final ClassicalRegister target
        ) {
            classicalRegisters.put(
                source,
                target
            );
            for (int i = 0; i < source.size(); i++) {
                classicalBits.put(
                    source.get(i),
                    target.get(i)
                );
            }
        }

        private Qubit qubit(final Qubit source) {
            final Qubit target = qubits.get(source);
            if (target == null) {
                throw new IllegalArgumentException("Missing qubit remap.");
            }
            return target;
        }

        private QuantumRegister quantumRegister(final QuantumRegister source) {
            final QuantumRegister target = quantumRegisters.get(source);
            if (target == null) {
                throw new IllegalArgumentException("Missing quantum register remap.");
            }
            return target;
        }

        private ClassicalBit classicalBit(final ClassicalBit source) {
            final ClassicalBit target = classicalBits.get(source);
            if (target == null) {
                throw new IllegalArgumentException("Missing classical bit remap.");
            }
            return target;
        }

        private ClassicalRegister classicalRegister(final ClassicalRegister source) {
            final ClassicalRegister target = classicalRegisters.get(source);
            if (target == null) {
                throw new IllegalArgumentException("Missing classical register remap.");
            }
            return target;
        }
    }
}
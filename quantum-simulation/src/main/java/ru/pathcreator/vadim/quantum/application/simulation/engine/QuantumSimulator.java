/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.simulation.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ru.pathcreator.vadim.quantum.application.simulation.diagnostic.SimulationDiagnostic;
import ru.pathcreator.vadim.quantum.application.simulation.diagnostic.SimulationDiagnosticCode;
import ru.pathcreator.vadim.quantum.application.simulation.diagnostic.SimulationDiagnosticSeverity;
import ru.pathcreator.vadim.quantum.application.simulation.options.SimulationOptions;
import ru.pathcreator.vadim.quantum.application.simulation.options.SimulationUnsupportedOperationPolicy;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.application.simulation.result.StateVectorAmplitude;
import ru.pathcreator.vadim.quantum.domain.callable.CallableArgumentKind;
import ru.pathcreator.vadim.quantum.domain.callable.CallableDefinition;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableBarrierOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableBlockOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableClassicalAssignmentOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableClassicalExpressionKind;
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
import ru.pathcreator.vadim.quantum.domain.bit.ClassicalBit;
import ru.pathcreator.vadim.quantum.domain.bit.Qubit;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalBinaryOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalBooleanOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalComparisonOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpressionKind;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicateKind;
import ru.pathcreator.vadim.quantum.domain.gate.Gate;
import ru.pathcreator.vadim.quantum.domain.gate.GateBodyOperation;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinition;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinitionKind;
import ru.pathcreator.vadim.quantum.domain.gate.GateMatrix;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.gate.modifier.GateModifier;
import ru.pathcreator.vadim.quantum.domain.gate.modifier.GateModifierKind;
import ru.pathcreator.vadim.quantum.domain.gate.modifier.ModifiedGate;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.BarrierOperation;
import ru.pathcreator.vadim.quantum.domain.operation.BlockOperation;
import ru.pathcreator.vadim.quantum.domain.operation.BranchConditionKind;
import ru.pathcreator.vadim.quantum.domain.operation.BranchOperation;
import ru.pathcreator.vadim.quantum.domain.operation.CallableInvocationOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalArrayDeclarationOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalAssignmentOperation;
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
import ru.pathcreator.vadim.quantum.domain.operation.ResetOperation;
import ru.pathcreator.vadim.quantum.domain.operation.SymbolicForLoopOperation;
import ru.pathcreator.vadim.quantum.domain.operation.TimingBoxOperation;
import ru.pathcreator.vadim.quantum.domain.operation.WaitOperation;
import ru.pathcreator.vadim.quantum.domain.operation.WhileLoopOperation;
import ru.pathcreator.vadim.quantum.domain.parameter.ParameterExpressionEvaluator;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

/**
 * Deterministic local state-vector simulator for static gate-based IR programs.
 */
public final class QuantumSimulator {

    private static final double ONE_OVER_SQRT_TWO = 1.0 / Math.sqrt(2.0);
    private static final double EPSILON = 1.0e-10;
    private static final long MAX_LOOP_ITERATIONS = 1_000_000L;

    private final ParameterExpressionEvaluator parameterEvaluator = new ParameterExpressionEvaluator();

    public SimulationResult simulate(final QuantumProgram program) {
        return simulate(
            program,
            SimulationOptions.defaults()
        );
    }

    public SimulationResult simulate(
        final QuantumProgram program,
        final SimulationOptions options
    ) {
        if (program == null) {
            throw new IllegalArgumentException("Quantum program must not be null.");
        }
        if (options == null) {
            throw new IllegalArgumentException("Simulation options must not be null.");
        }
        final ArrayList<SimulationDiagnostic> diagnostics = new ArrayList<>();
        if (program.circuitCount() == 0) {
            diagnostics.add(SimulationDiagnostic.error(
                SimulationDiagnosticCode.EMPTY_PROGRAM,
                "Simulation requires one circuit.",
                -1,
                -1
            ));
            return emptyResult(
                options,
                diagnostics
            );
        }
        if (program.circuitCount() > 1) {
            diagnostics.add(SimulationDiagnostic.error(
                SimulationDiagnosticCode.MULTIPLE_CIRCUITS,
                "Simulation requires exactly one circuit.",
                -1,
                -1
            ));
            return emptyResult(
                options,
                diagnostics
            );
        }
        final QuantumCircuit circuit = program.circuit(0);
        final Random random = new Random(options.seed());
        final SimulationContext context = SimulationContext.create(
            circuit,
            options,
            diagnostics,
            random,
            true
        );
        if (context.qubitCount > options.maxQubits()) {
            diagnostics.add(SimulationDiagnostic.error(
                SimulationDiagnosticCode.TOO_MANY_QUBITS,
                "State-vector simulation qubit count exceeds configured limit.",
                0,
                -1
            ));
            return emptyResult(
                options,
                diagnostics
            );
        }
        context.initializeState();
        executeOperations(
            context,
            CircuitOperationSequence.of(circuit),
            -1
        );
        if (hasError(diagnostics)) {
            return context.result(Map.of());
        }
        if (
            context.performedMeasurement
            && options.captureStateVector()
        ) {
            diagnostics.add(SimulationDiagnostic.warning(
                SimulationDiagnosticCode.STATE_VECTOR_AFTER_MEASUREMENT,
                "Captured state vector is the first deterministic trajectory after measurement collapse.",
                0,
                SimulationDiagnostic.NO_INDEX
            ));
        }
        return context.result(simulateCounts(
            circuit,
            options,
            diagnostics,
            random,
            context
        ));
    }

    private Map<String, Long> simulateCounts(
        final QuantumCircuit circuit,
        final SimulationOptions options,
        final ArrayList<SimulationDiagnostic> diagnostics,
        final Random random,
        final SimulationContext firstContext
    ) {
        if (options.shots() == 0) {
            return Map.of();
        }
        final LinkedHashMap<String, Long> counts = new LinkedHashMap<>();
        addCount(
            counts,
            firstContext.finalShotBitString()
        );
        for (int shot = 1; shot < options.shots(); shot++) {
            final SimulationContext context = SimulationContext.create(
                circuit,
                options,
                diagnostics,
                random,
                true
            );
            context.initializeState();
            executeOperations(
                context,
                CircuitOperationSequence.of(circuit),
                -1
            );
            if (hasError(diagnostics)) {
                break;
            }
            addCount(
                counts,
                context.finalShotBitString()
            );
        }
        return counts;
    }

    private static boolean hasError(final List<SimulationDiagnostic> diagnostics) {
        for (int i = 0; i < diagnostics.size(); i++) {
            if (diagnostics.get(i).isError()) {
                return true;
            }
        }
        return false;
    }

    private static void addCount(
        final LinkedHashMap<String, Long> counts,
        final String bitString
    ) {
        counts.put(
            bitString,
            counts.getOrDefault(
                bitString,
                0L
            ) + 1L
        );
    }

    private boolean executeOperation(
        final SimulationContext context,
        final Operation operation,
        final int operationIndex
    ) {
        if (operation instanceof GateOperation gateOperation) {
            return executeGate(
                context,
                gateOperation,
                operationIndex
            );
        }
        if (operation instanceof MeasureOperation measureOperation) {
            return executeMeasure(
                context,
                measureOperation,
                operationIndex
            );
        }
        if (operation instanceof ResetOperation resetOperation) {
            return executeReset(
                context,
                resetOperation,
                operationIndex
            );
        }
        if (operation instanceof BarrierOperation) {
            return true;
        }
        if (operation instanceof ClassicalAssignmentOperation assignmentOperation) {
            return executeClassicalAssignment(
                context,
                assignmentOperation,
                operationIndex
            );
        }
        if (operation instanceof ClassicalDeclarationOperation declarationOperation) {
            return executeClassicalDeclaration(
                context,
                declarationOperation,
                operationIndex
            );
        }
        if (operation instanceof ClassicalArrayDeclarationOperation arrayOperation) {
            return executeClassicalArrayDeclaration(
                context,
                arrayOperation,
                operationIndex
            );
        }
        if (operation instanceof ControlledOperation controlledOperation) {
            return executeControlled(
                context,
                controlledOperation,
                operationIndex
            );
        }
        if (operation instanceof ClassicallyControlledOperation controlledOperation) {
            return executeClassicallyControlled(
                context,
                controlledOperation,
                operationIndex
            );
        }
        if (operation instanceof BlockOperation blockOperation) {
            return executeBlock(
                context,
                blockOperation.body(),
                operationIndex
            );
        }
        if (operation instanceof ConditionalBlockOperation conditionalOperation) {
            return executeConditionalBlock(
                context,
                conditionalOperation,
                operationIndex
            );
        }
        if (operation instanceof ForLoopOperation loopOperation) {
            return executeForLoop(
                context,
                loopOperation,
                operationIndex
            );
        }
        if (operation instanceof SymbolicForLoopOperation loopOperation) {
            return executeSymbolicForLoop(
                context,
                loopOperation,
                operationIndex
            );
        }
        if (operation instanceof WhileLoopOperation loopOperation) {
            return executeWhileLoop(
                context,
                loopOperation,
                operationIndex
            );
        }
        if (operation instanceof DelayOperation delayOperation) {
            return executeDelay(
                context,
                delayOperation,
                operationIndex
            );
        }
        if (operation instanceof TimingBoxOperation timingBoxOperation) {
            return executeBlock(
                context,
                timingBoxOperation.body(),
                operationIndex
            );
        }
        if (operation instanceof LabelOperation) {
            return true;
        }
        if (operation instanceof WaitOperation) {
            return true;
        }
        if (operation instanceof HaltOperation) {
            context.halted = true;
            return false;
        }
        if (operation instanceof BranchOperation branchOperation) {
            return executeBranch(
                context,
                branchOperation,
                operationIndex
            );
        }
        if (operation instanceof CallableInvocationOperation invocationOperation) {
            return executeCallableInvocation(
                context,
                invocationOperation,
                operationIndex
            );
        }
        return unsupported(
            context,
            SimulationDiagnosticCode.UNSUPPORTED_OPERATION,
            "Operation kind is not supported by the local state-vector simulator: " + operation.kind() + ".",
            operationIndex
        );
    }

    private boolean executeBlock(
        final SimulationContext context,
        final OperationBlock block,
        final int parentOperationIndex
    ) {
        return executeOperations(
            context,
            BlockOperationSequence.of(block),
            parentOperationIndex
        );
    }

    private boolean executeOperations(
        final SimulationContext context,
        final OperationSequence operations,
        final int parentOperationIndex
    ) {
        final HashMap<String, Integer> labels = collectLabels(operations);
        int index = 0;
        long steps = 0L;
        while (index < operations.operationCount()) {
            if (steps++ >= MAX_LOOP_ITERATIONS) {
                return unsupported(
                    context,
                    SimulationDiagnosticCode.UNSUPPORTED_OPERATION,
                    "Operation sequence execution reached the simulator safety limit.",
                    parentOperationIndex
                );
            }
            context.pendingBranchTarget = null;
            final int operationIndex = parentOperationIndex >= 0
                ? parentOperationIndex
                : index;
            final boolean shouldContinue = executeOperation(
                context,
                operations.operation(index),
                operationIndex
            );
            if (context.halted) {
                return false;
            }
            if (!shouldContinue) {
                return false;
            }
            if (context.pendingBranchTarget != null) {
                final Integer targetIndex = labels.get(context.pendingBranchTarget);
                if (targetIndex == null) {
                    return unsupported(
                        context,
                        SimulationDiagnosticCode.UNSUPPORTED_OPERATION,
                        "Branch target label was not found: " + context.pendingBranchTarget + ".",
                        operationIndex
                    );
                }
                index = targetIndex.intValue();
            } else {
                index++;
            }
        }
        return true;
    }

    private static HashMap<String, Integer> collectLabels(final OperationSequence operations) {
        final HashMap<String, Integer> labels = new HashMap<>();
        for (int i = 0; i < operations.operationCount(); i++) {
            final Operation operation = operations.operation(i);
            if (operation instanceof LabelOperation labelOperation) {
                labels.put(
                    labelOperation.name(),
                    i
                );
            }
        }
        return labels;
    }

    private boolean executeConditionalBlock(
        final SimulationContext context,
        final ConditionalBlockOperation operation,
        final int operationIndex
    ) {
        final Boolean value = evaluatePredicate(
            context,
            operation.predicate()
        );
        if (value == null) {
            return unsupported(
                context,
                SimulationDiagnosticCode.CLASSICAL_CONDITION_UNAVAILABLE,
                "Conditional block predicate cannot be evaluated by the simulator.",
                operationIndex
            );
        }
        if (value.booleanValue()) {
            return executeBlock(
                context,
                operation.thenBlock(),
                operationIndex
            );
        }
        if (operation.hasElseBlock()) {
            return executeBlock(
                context,
                operation.elseBlock(),
                operationIndex
            );
        }
        return true;
    }

    private boolean executeForLoop(
        final SimulationContext context,
        final ForLoopOperation operation,
        final int operationIndex
    ) {
        long iterations = 0L;
        final boolean hadPrevious = context.localClassicalValues.containsKey(operation.variableName());
        final Long previous = context.localClassicalValues.get(operation.variableName());
        if (operation.step() > 0L) {
            for (long value = operation.startInclusive(); value <= operation.endInclusive(); value += operation.step()) {
                context.localClassicalValues.put(
                    operation.variableName(),
                    value
                );
                if (!executeLoopBody(
                    context,
                    operation.body(),
                    operationIndex,
                    iterations++
                )) {
                    return false;
                }
            }
        } else {
            for (long value = operation.startInclusive(); value >= operation.endInclusive(); value += operation.step()) {
                context.localClassicalValues.put(
                    operation.variableName(),
                    value
                );
                if (!executeLoopBody(
                    context,
                    operation.body(),
                    operationIndex,
                    iterations++
                )) {
                    return false;
                }
            }
        }
        restoreLocal(
            context,
            operation.variableName(),
            hadPrevious,
            previous
        );
        return true;
    }

    private boolean executeSymbolicForLoop(
        final SimulationContext context,
        final SymbolicForLoopOperation operation,
        final int operationIndex
    ) {
        final Long start = evaluateClassicalExpression(
            context,
            operation.startInclusive()
        );
        final Long step = evaluateClassicalExpression(
            context,
            operation.step()
        );
        final Long end = evaluateClassicalExpression(
            context,
            operation.endInclusive()
        );
        if (
            start == null
            || step == null
            || end == null
        ) {
            return unsupported(
                context,
                SimulationDiagnosticCode.CLASSICAL_CONDITION_UNAVAILABLE,
                "Symbolic for-loop bounds cannot be evaluated by the simulator.",
                operationIndex
            );
        }
        if (step.longValue() == 0L) {
            return unsupported(
                context,
                SimulationDiagnosticCode.CLASSICAL_CONDITION_UNAVAILABLE,
                "Symbolic for-loop step must not be zero.",
                operationIndex
            );
        }
        long iterations = 0L;
        final boolean hadPrevious = context.localClassicalValues.containsKey(operation.variableName());
        final Long previous = context.localClassicalValues.get(operation.variableName());
        if (step.longValue() > 0L) {
            for (long value = start.longValue(); value <= end.longValue(); value += step.longValue()) {
                context.localClassicalValues.put(
                    operation.variableName(),
                    value
                );
                if (!executeLoopBody(
                    context,
                    operation.body(),
                    operationIndex,
                    iterations++
                )) {
                    return false;
                }
            }
        } else {
            for (long value = start.longValue(); value >= end.longValue(); value += step.longValue()) {
                context.localClassicalValues.put(
                    operation.variableName(),
                    value
                );
                if (!executeLoopBody(
                    context,
                    operation.body(),
                    operationIndex,
                    iterations++
                )) {
                    return false;
                }
            }
        }
        restoreLocal(
            context,
            operation.variableName(),
            hadPrevious,
            previous
        );
        return true;
    }

    private boolean executeWhileLoop(
        final SimulationContext context,
        final WhileLoopOperation operation,
        final int operationIndex
    ) {
        long iterations = 0L;
        while (true) {
            final Boolean value = evaluatePredicate(
                context,
                operation.predicate()
            );
            if (value == null) {
                return unsupported(
                    context,
                    SimulationDiagnosticCode.CLASSICAL_CONDITION_UNAVAILABLE,
                    "While-loop predicate cannot be evaluated by the simulator.",
                    operationIndex
                );
            }
            if (!value.booleanValue()) {
                return true;
            }
            if (!executeLoopBody(
                context,
                operation.body(),
                operationIndex,
                iterations++
            )) {
                return false;
            }
        }
    }

    private boolean executeLoopBody(
        final SimulationContext context,
        final OperationBlock body,
        final int operationIndex,
        final long iteration
    ) {
        if (iteration >= MAX_LOOP_ITERATIONS) {
            return unsupported(
                context,
                SimulationDiagnosticCode.UNSUPPORTED_OPERATION,
                "Loop execution reached the simulator safety limit.",
                operationIndex
            );
        }
        return executeBlock(
            context,
            body,
            operationIndex
        );
    }

    private boolean executeGate(
        final SimulationContext context,
        final GateOperation operation,
        final int operationIndex
    ) {
        final Gate gate = operation.gate();
        if (gate instanceof ModifiedGate modifiedGate) {
            return executeModifiedGate(
                context,
                modifiedGate,
                operation,
                operationIndex
            );
        }
        if (!(gate instanceof StandardGate standardGate)) {
            if (gate instanceof GateDefinition definition) {
                return executeGateDefinition(
                    context,
                    definition,
                    operation,
                    operationIndex
                );
            }
            return unsupported(
                context,
                SimulationDiagnosticCode.UNSUPPORTED_GATE,
                "Only standard gates or executable gate definitions are supported by the local state-vector simulator.",
                operationIndex
            );
        }
        final int[] qubits = resolveQubits(
            context,
            operation.qubitReferences(),
            operationIndex
        );
        if (qubits == null) {
            return false;
        }
        final double[] parameters = resolveParameters(
            context,
            operation.parameters(),
            operationIndex
        );
        if (parameters == null) {
            return false;
        }
        applyStandardGate(
            context,
            standardGate,
            qubits,
            parameters
        );
        return true;
    }

    private boolean executeGateDefinition(
        final SimulationContext context,
        final GateDefinition definition,
        final GateOperation operation,
        final int operationIndex
    ) {
        final int[] qubits = resolveQubits(
            context,
            operation.qubitReferences(),
            operationIndex
        );
        if (qubits == null) {
            return false;
        }
        final double[] parameters = resolveParameters(
            context,
            operation.parameters(),
            operationIndex
        );
        if (parameters == null) {
            return false;
        }
        if (definition.kind() == GateDefinitionKind.COMPOSITE) {
            return executeCompositeGateDefinition(
                context,
                definition,
                qubits,
                parameters,
                operationIndex
            );
        }
        if (definition.kind() == GateDefinitionKind.MATRIX) {
            return executeMatrixGateDefinition(
                context,
                definition.matrix(),
                qubits,
                operationIndex
            );
        }
        return unsupported(
            context,
            SimulationDiagnosticCode.UNSUPPORTED_GATE,
            "Gate definition does not contain executable simulation semantics: " + definition.gateName() + ".",
            operationIndex
        );
    }

    private boolean executeCompositeGateDefinition(
        final SimulationContext context,
        final GateDefinition definition,
        final int[] qubits,
        final double[] parameters,
        final int operationIndex
    ) {
        final HashMap<String, Integer> qubitBindings = new HashMap<>();
        for (int i = 0; i < definition.qubitNames().size(); i++) {
            qubitBindings.put(
                definition.qubitNames().get(i),
                qubits[i]
            );
        }
        final HashMap<String, Double> parameterBindings = new HashMap<>();
        for (int i = 0; i < definition.parameterNames().size(); i++) {
            parameterBindings.put(
                definition.parameterNames().get(i),
                parameters[i]
            );
        }
        for (int i = 0; i < definition.bodyOperations().size(); i++) {
            if (!executeGateBodyOperation(
                context,
                definition.bodyOperations().get(i),
                qubitBindings,
                parameterBindings,
                operationIndex
            )) {
                return false;
            }
        }
        return true;
    }

    private boolean executeGateBodyOperation(
        final SimulationContext context,
        final GateBodyOperation operation,
        final HashMap<String, Integer> qubitBindings,
        final HashMap<String, Double> parameterBindings,
        final int operationIndex
    ) {
        final int[] qubits = new int[operation.qubitCount()];
        for (int i = 0; i < operation.qubitCount(); i++) {
            final Integer qubit = qubitBindings.get(operation.qubitName(i));
            if (qubit == null) {
                return unsupported(
                    context,
                    SimulationDiagnosticCode.NON_STATIC_QUBIT_REFERENCE,
                    "Composite gate body references an unbound qubit argument.",
                    operationIndex
                );
            }
            qubits[i] = qubit.intValue();
        }
        final double[] parameters = new double[operation.parameterCount()];
        for (int i = 0; i < operation.parameterCount(); i++) {
            final Double value = evaluateGateBodyParameter(
                operation.parameter(i),
                parameterBindings
            );
            if (value == null) {
                return unsupported(
                    context,
                    SimulationDiagnosticCode.UNBOUND_PARAMETER,
                    "Composite gate body parameter cannot be evaluated.",
                    operationIndex
                );
            }
            parameters[i] = value.doubleValue();
        }
        if (operation.gate() instanceof ModifiedGate modifiedGate) {
            if (!(modifiedGate.baseGate() instanceof StandardGate standardGate)) {
                return unsupported(
                    context,
                    SimulationDiagnosticCode.UNSUPPORTED_GATE,
                    "Composite gate body modified gate must wrap a standard gate.",
                    operationIndex
                );
            }
            return applyModifiedGate(
                context,
                modifiedGate,
                standardGate,
                qubits,
                parameters,
                operationIndex
            );
        }
        if (operation.gate() instanceof StandardGate standardGate) {
            applyStandardGate(
                context,
                standardGate,
                qubits,
                parameters
            );
            return true;
        }
        if (operation.gate() instanceof GateDefinition nestedDefinition) {
            if (nestedDefinition.kind() == GateDefinitionKind.MATRIX) {
                return executeMatrixGateDefinition(
                    context,
                    nestedDefinition.matrix(),
                    qubits,
                    operationIndex
                );
            }
        }
        return unsupported(
            context,
            SimulationDiagnosticCode.UNSUPPORTED_GATE,
            "Composite gate body contains a gate that cannot be simulated directly.",
            operationIndex
        );
    }

    private boolean executeModifiedGate(
        final SimulationContext context,
        final ModifiedGate gate,
        final GateOperation operation,
        final int operationIndex
    ) {
        if (!(gate.baseGate() instanceof StandardGate standardGate)) {
            return unsupported(
                context,
                SimulationDiagnosticCode.UNSUPPORTED_GATE,
                "Only modified standard gates are supported by the local state-vector simulator.",
                operationIndex
            );
        }
        final int[] qubits = resolveQubits(
            context,
            operation.qubitReferences(),
            operationIndex
        );
        if (qubits == null) {
            return false;
        }
        final double[] parameters = resolveParameters(
            context,
            operation.parameters(),
            operationIndex
        );
        if (parameters == null) {
            return false;
        }
        return applyModifiedGate(
            context,
            gate,
            standardGate,
            qubits,
            parameters,
            operationIndex
        );
    }

    private boolean applyModifiedGate(
        final SimulationContext context,
        final ModifiedGate gate,
        final StandardGate baseGate,
        final int[] qubits,
        final double[] parameters,
        final int operationIndex
    ) {
        int controlCount = 0;
        int repeatCount = 1;
        boolean inverse = false;
        int integerPower = 1;
        for (int i = 0; i < gate.modifiers().size(); i++) {
            final GateModifier modifier = gate.modifiers().get(i);
            if (modifier.kind() == GateModifierKind.ANNOTATION) {
                continue;
            }
            if (modifier.kind() == GateModifierKind.CONTROLLED) {
                controlCount += modifier.integerValue();
            } else if (modifier.kind() == GateModifierKind.REPEAT) {
                repeatCount = Math.multiplyExact(
                    repeatCount,
                    modifier.integerValue()
                );
            } else if (modifier.kind() == GateModifierKind.INVERSE) {
                inverse = !inverse;
            } else if (modifier.kind() == GateModifierKind.POWER) {
                final Double power = resolveGatePower(
                    context,
                    modifier,
                    operationIndex
                );
                if (power == null) {
                    return false;
                }
                final long rounded = Math.round(power.doubleValue());
                if (Math.abs(power.doubleValue() - rounded) > EPSILON) {
                    return unsupported(
                        context,
                        SimulationDiagnosticCode.UNSUPPORTED_GATE,
                        "Non-integer gate power modifiers require explicit decomposition before local simulation.",
                        operationIndex
                    );
                }
                if (
                    rounded < Integer.MIN_VALUE
                    || rounded > Integer.MAX_VALUE
                ) {
                    return unsupported(
                        context,
                        SimulationDiagnosticCode.UNSUPPORTED_GATE,
                        "Gate power exponent is outside simulator repeat bounds.",
                        operationIndex
                    );
                }
                integerPower = Math.multiplyExact(
                    integerPower,
                    (int) rounded
                );
            }
        }
        if (integerPower < 0) {
            inverse = !inverse;
            integerPower = Math.abs(integerPower);
        }
        repeatCount = Math.multiplyExact(
            repeatCount,
            integerPower
        );
        if (
            controlCount > 0
            && baseGate.arity() != 1
        ) {
            return unsupported(
                context,
                SimulationDiagnosticCode.UNSUPPORTED_GATE,
                "Controlled modified gates are supported only for single-qubit base gates.",
                operationIndex
            );
        }
        for (int i = 0; i < repeatCount; i++) {
            if (controlCount == 0) {
                if (inverse) {
                    applyInverseStandardGate(
                        context,
                        baseGate,
                        qubits,
                        parameters
                    );
                } else {
                    applyStandardGate(
                        context,
                        baseGate,
                        qubits,
                        parameters
                    );
                }
            } else {
                applyControlledSingleQubitStandardGate(
                    context,
                    baseGate,
                    qubits,
                    parameters,
                    controlCount,
                    inverse
                );
            }
        }
        return true;
    }

    private Double resolveGatePower(
        final SimulationContext context,
        final GateModifier modifier,
        final int operationIndex
    ) {
        if (!modifier.hasPowerExpression()) {
            return modifier.doubleValue();
        }
        try {
            return parameterEvaluator.evaluate(
                modifier.powerExpression(),
                context.options.parameterBindings()
            );
        } catch (final IllegalArgumentException exception) {
            unsupported(
                context,
                SimulationDiagnosticCode.UNBOUND_PARAMETER,
                "Gate power expression cannot be evaluated: " + exception.getMessage(),
                operationIndex
            );
            return null;
        }
    }

    private Double evaluateGateBodyParameter(
        final ParameterExpression expression,
        final HashMap<String, Double> parameterBindings
    ) {
        return switch (expression.kind()) {
            case NUMERIC -> expression.numericValue();
            case NAMED -> parameterBindings.get(expression.name());
            case KNOWN_CONSTANT -> knownParameterConstant(expression.name());
            case UNARY -> {
                final Double value = evaluateGateBodyParameter(
                    expression.left(),
                    parameterBindings
                );
                yield value == null
                    ? null
                    : -value.doubleValue();
            }
            case BINARY -> {
                final Double left = evaluateGateBodyParameter(
                    expression.left(),
                    parameterBindings
                );
                final Double right = evaluateGateBodyParameter(
                    expression.right(),
                    parameterBindings
                );
                if (
                    left == null
                    || right == null
                ) {
                    yield null;
                }
                yield switch (expression.binaryOperator()) {
                    case ADD -> left + right;
                    case SUBTRACT -> left - right;
                    case MULTIPLY -> left * right;
                    case DIVIDE -> Math.abs(right) <= EPSILON ? null : left / right;
                };
            }
        };
    }

    private static Double knownParameterConstant(final String name) {
        if ("pi".equals(name)) {
            return Math.PI;
        }
        if ("tau".equals(name)) {
            return 2.0 * Math.PI;
        }
        if ("e".equals(name)) {
            return Math.E;
        }
        return null;
    }

    private boolean executeMatrixGateDefinition(
        final SimulationContext context,
        final GateMatrix matrix,
        final int[] qubits,
        final int operationIndex
    ) {
        final int size = 1 << qubits.length;
        final double[] matrixReal = new double[size * size];
        final double[] matrixImaginary = new double[size * size];
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                final ComplexValue value = parseComplex(matrix.entry(
                    row,
                    column
                ));
                if (value == null) {
                    return unsupported(
                        context,
                        SimulationDiagnosticCode.UNSUPPORTED_GATE,
                        "Gate matrix entry cannot be evaluated as a numeric complex value.",
                        operationIndex
                    );
                }
                matrixReal[row * size + column] = value.real;
                matrixImaginary[row * size + column] = value.imaginary;
            }
        }
        applyMatrix(
            context,
            qubits,
            matrixReal,
            matrixImaginary
        );
        return true;
    }

    private void applyMatrix(
        final SimulationContext context,
        final int[] qubits,
        final double[] matrixReal,
        final double[] matrixImaginary
    ) {
        final int localSize = 1 << qubits.length;
        final boolean[] visited = new boolean[context.stateReal.length];
        final int[] masks = new int[qubits.length];
        for (int i = 0; i < qubits.length; i++) {
            masks[i] = 1 << qubits[i];
        }
        final double[] inputReal = new double[localSize];
        final double[] inputImaginary = new double[localSize];
        final double[] outputReal = new double[localSize];
        final double[] outputImaginary = new double[localSize];
        final int[] indexes = new int[localSize];
        for (int basis = 0; basis < context.stateReal.length; basis++) {
            if (visited[basis]) {
                continue;
            }
            final int base = clearMasks(
                basis,
                masks
            );
            for (int local = 0; local < localSize; local++) {
                int index = base;
                for (int bit = 0; bit < qubits.length; bit++) {
                    if ((local & (1 << bit)) != 0) {
                        index |= masks[bit];
                    }
                }
                indexes[local] = index;
                visited[index] = true;
                inputReal[local] = context.stateReal[index];
                inputImaginary[local] = context.stateImaginary[index];
            }
            for (int row = 0; row < localSize; row++) {
                double real = 0.0;
                double imaginary = 0.0;
                for (int column = 0; column < localSize; column++) {
                    final int matrixIndex = row * localSize + column;
                    real += matrixReal[matrixIndex] * inputReal[column]
                        - matrixImaginary[matrixIndex] * inputImaginary[column];
                    imaginary += matrixReal[matrixIndex] * inputImaginary[column]
                        + matrixImaginary[matrixIndex] * inputReal[column];
                }
                outputReal[row] = real;
                outputImaginary[row] = imaginary;
            }
            for (int local = 0; local < localSize; local++) {
                context.stateReal[indexes[local]] = outputReal[local];
                context.stateImaginary[indexes[local]] = outputImaginary[local];
            }
        }
    }

    private static int clearMasks(
        final int value,
        final int[] masks
    ) {
        int result = value;
        for (int i = 0; i < masks.length; i++) {
            result &= ~masks[i];
        }
        return result;
    }

    private static ComplexValue parseComplex(final String text) {
        final String normalized = text.trim().replace(" ", "");
        if ("i".equals(normalized)) {
            return new ComplexValue(
                0.0,
                1.0
            );
        }
        if ("-i".equals(normalized)) {
            return new ComplexValue(
                0.0,
                -1.0
            );
        }
        if (normalized.endsWith("i")) {
            final String withoutI = normalized.substring(
                0,
                normalized.length() - 1
            );
            final int split = complexSplitIndex(withoutI);
            try {
                if (split > 0) {
                    return new ComplexValue(
                        Double.parseDouble(withoutI.substring(
                            0,
                            split
                        )),
                        parseImaginaryCoefficient(withoutI.substring(split))
                    );
                }
                return new ComplexValue(
                    0.0,
                    parseImaginaryCoefficient(withoutI)
                );
            } catch (final NumberFormatException exception) {
                return null;
            }
        }
        try {
            return new ComplexValue(
                Double.parseDouble(normalized),
                0.0
            );
        } catch (final NumberFormatException exception) {
            return null;
        }
    }

    private static int complexSplitIndex(final String value) {
        for (int i = value.length() - 1; i > 0; i--) {
            final char current = value.charAt(i);
            if (
                (current == '+' || current == '-')
                && value.charAt(i - 1) != 'E'
                && value.charAt(i - 1) != 'e'
            ) {
                return i;
            }
        }
        return -1;
    }

    private static double parseImaginaryCoefficient(final String value) {
        if (
            value.isBlank()
            || "+".equals(value)
        ) {
            return 1.0;
        }
        if ("-".equals(value)) {
            return -1.0;
        }
        return Double.parseDouble(value);
    }

    private boolean executeMeasure(
        final SimulationContext context,
        final MeasureOperation operation,
        final int operationIndex
    ) {
        final int qubitIndex = resolveQubit(
            context,
            operation.qubitReference(),
            operationIndex
        );
        if (qubitIndex < 0) {
            return false;
        }
        final int bitIndex = context.classicalBitIndex(operation.bit());
        context.classicalBits[bitIndex] = measureQubit(
            context,
            qubitIndex
        );
        context.performedMeasurement = true;
        return true;
    }

    private boolean executeReset(
        final SimulationContext context,
        final ResetOperation operation,
        final int operationIndex
    ) {
        final int qubitIndex = resolveQubit(
            context,
            operation.qubitReference(),
            operationIndex
        );
        if (qubitIndex < 0) {
            return false;
        }
        final int value = measureQubit(
            context,
            qubitIndex
        );
        context.performedMeasurement = true;
        if (value == 1) {
            moveBasisBitOneToZero(
                context,
                qubitIndex
            );
        }
        return true;
    }

    private boolean executeClassicalAssignment(
        final SimulationContext context,
        final ClassicalAssignmentOperation operation,
        final int operationIndex
    ) {
        final Long value = evaluateClassicalExpression(
            context,
            operation.assignment().value()
        );
        if (value == null) {
            return unsupported(
                context,
                SimulationDiagnosticCode.CLASSICAL_CONDITION_UNAVAILABLE,
                "Classical assignment value cannot be evaluated by the simulator.",
                operationIndex
            );
        }
        final ClassicalExpression target = operation.assignment().target();
        if (target.kind() == ClassicalExpressionKind.BIT_REFERENCE) {
            context.classicalBits[context.classicalBitIndex(target.bit())] = (int) (value & 1L);
            return true;
        }
        if (target.kind() == ClassicalExpressionKind.REGISTER_REFERENCE) {
            writeRegisterValue(
                context,
                target.register(),
                value
            );
            return true;
        }
        if (target.kind() == ClassicalExpressionKind.VARIABLE_REFERENCE) {
            context.localClassicalValues.put(
                target.variableName(),
                value
            );
            return true;
        }
        if (target.kind() == ClassicalExpressionKind.SYMBOLIC_REFERENCE) {
            if (writeSymbolicReference(
                context,
                target.symbolicText(),
                value
            )) {
                return true;
            }
        }
        return unsupported(
            context,
            SimulationDiagnosticCode.CLASSICAL_CONDITION_UNAVAILABLE,
            "Classical assignment target is not a concrete bit or register reference.",
            operationIndex
        );
    }

    private boolean executeClassicalDeclaration(
        final SimulationContext context,
        final ClassicalDeclarationOperation operation,
        final int operationIndex
    ) {
        long value = 0L;
        if (operation.hasInitializer()) {
            final Long evaluated = evaluateClassicalExpression(
                context,
                operation.initializer()
            );
            if (evaluated == null) {
                return unsupported(
                    context,
                    SimulationDiagnosticCode.CLASSICAL_CONDITION_UNAVAILABLE,
                    "Classical declaration initializer cannot be evaluated by the simulator.",
                    operationIndex
                );
            }
            value = evaluated;
        }
        context.localClassicalValues.put(
            operation.declaration().name(),
            value
        );
        return true;
    }

    private boolean executeClassicalArrayDeclaration(
        final SimulationContext context,
        final ClassicalArrayDeclarationOperation operation,
        final int operationIndex
    ) {
        long elementCount = 1L;
        for (int i = 0; i < operation.dimensionCount(); i++) {
            final Long dimension = evaluateClassicalExpression(
                context,
                operation.dimension(i)
            );
            if (
                dimension == null
                || dimension.longValue() < 0L
            ) {
                return unsupported(
                    context,
                    SimulationDiagnosticCode.CLASSICAL_CONDITION_UNAVAILABLE,
                    "Classical array dimension cannot be evaluated by the simulator.",
                    operationIndex
                );
            }
            elementCount = Math.multiplyExact(
                elementCount,
                dimension.longValue()
            );
            if (elementCount > Integer.MAX_VALUE) {
                return unsupported(
                    context,
                    SimulationDiagnosticCode.CLASSICAL_CONDITION_UNAVAILABLE,
                    "Classical array is too large for local simulation storage.",
                    operationIndex
                );
            }
        }
        final long[] values = new long[(int) elementCount];
        if (operation.hasInitializerText()) {
            final long[] initialized = parseArrayInitializer(operation.initializerText());
            if (initialized == null) {
                return unsupported(
                    context,
                    SimulationDiagnosticCode.CLASSICAL_CONDITION_UNAVAILABLE,
                    "Classical array initializer cannot be evaluated by the simulator.",
                    operationIndex
                );
            }
            final int limit = Math.min(
                initialized.length,
                values.length
            );
            System.arraycopy(
                initialized,
                0,
                values,
                0,
                limit
            );
        }
        context.localClassicalArrays.put(
            operation.name(),
            values
        );
        return true;
    }

    private boolean executeDelay(
        final SimulationContext context,
        final DelayOperation operation,
        final int operationIndex
    ) {
        for (int i = 0; i < operation.qubitCount(); i++) {
            if (resolveQubit(
                context,
                operation.reference(i),
                operationIndex
            ) < 0) {
                return false;
            }
        }
        return true;
    }

    private boolean executeCallableInvocation(
        final SimulationContext context,
        final CallableInvocationOperation operation,
        final int operationIndex
    ) {
        final CallableDefinition definition = context.callableDefinitions.get(operation.callableName());
        if (definition == null) {
            if (!operation.hasTarget()) {
                for (int i = 0; i < operation.quantumArguments().size(); i++) {
                    if (resolveQubit(
                        context,
                        operation.quantumArguments().get(i),
                        operationIndex
                    ) < 0) {
                        return false;
                    }
                }
                return true;
            }
            return unsupported(
                context,
                SimulationDiagnosticCode.UNSUPPORTED_OPERATION,
                "External callable invocation with a return target requires a simulator implementation.",
                operationIndex
            );
        }
        final CallableExecutionFrame frame = bindCallableFrame(
            context,
            definition,
            operation,
            operationIndex
        );
        if (frame == null) {
            return false;
        }
        return executeCallableBlock(
            context,
            definition.body(),
            frame,
            operationIndex
        );
    }

    private CallableExecutionFrame bindCallableFrame(
        final SimulationContext context,
        final CallableDefinition definition,
        final CallableInvocationOperation operation,
        final int operationIndex
    ) {
        final HashMap<String, QuantumReference> quantumArguments = new HashMap<>();
        final HashMap<String, ClassicalExpression> classicalArguments = new HashMap<>();
        int quantumIndex = 0;
        int classicalIndex = 0;
        for (int i = 0; i < definition.argumentCount(); i++) {
            if (definition.argument(i).kind() == CallableArgumentKind.QUBIT) {
                if (quantumIndex >= operation.quantumArguments().size()) {
                    unsupported(
                        context,
                        SimulationDiagnosticCode.UNSUPPORTED_OPERATION,
                        "Callable invocation does not provide enough quantum arguments.",
                        operationIndex
                    );
                    return null;
                }
                quantumArguments.put(
                    definition.argument(i).name(),
                    operation.quantumArguments().get(quantumIndex++)
                );
            } else {
                if (classicalIndex >= operation.classicalArguments().size()) {
                    unsupported(
                        context,
                        SimulationDiagnosticCode.UNSUPPORTED_OPERATION,
                        "Callable invocation does not provide enough classical arguments.",
                        operationIndex
                    );
                    return null;
                }
                classicalArguments.put(
                    definition.argument(i).name(),
                    operation.classicalArguments().get(classicalIndex++)
                );
            }
        }
        if (
            quantumIndex != operation.quantumArguments().size()
            || classicalIndex != operation.classicalArguments().size()
        ) {
            unsupported(
                context,
                SimulationDiagnosticCode.UNSUPPORTED_OPERATION,
                "Callable invocation has unused actual arguments.",
                operationIndex
            );
            return null;
        }
        return new CallableExecutionFrame(
            quantumArguments,
            classicalArguments
        );
    }

    private boolean executeCallableBlock(
        final SimulationContext context,
        final CallableOperationBlock block,
        final CallableExecutionFrame frame,
        final int operationIndex
    ) {
        for (int i = 0; i < block.operationCount(); i++) {
            if (!executeCallableOperation(
                context,
                block.operation(i),
                frame,
                operationIndex
            )) {
                return false;
            }
            if (context.halted) {
                return false;
            }
        }
        return true;
    }

    private boolean executeCallableOperation(
        final SimulationContext context,
        final CallableOperation operation,
        final CallableExecutionFrame frame,
        final int operationIndex
    ) {
        if (operation instanceof CallableGateOperation gateOperation) {
            return executeCallableGate(
                context,
                gateOperation,
                frame,
                operationIndex
            );
        }
        if (operation instanceof CallableMeasureOperation measureOperation) {
            final int qubit = resolveCallableQubit(
                context,
                frame,
                measureOperation.qubitName(),
                operationIndex
            );
            final ClassicalExpression target = frame.classicalArguments.get(measureOperation.classicalName());
            if (
                qubit < 0
                || target == null
            ) {
                return false;
            }
            final int value = measureQubit(
                context,
                qubit
            );
            context.performedMeasurement = true;
            return writeClassicalExpression(
                context,
                target,
                value,
                operationIndex
            );
        }
        if (operation instanceof CallableResetOperation resetOperation) {
            final int qubit = resolveCallableQubit(
                context,
                frame,
                resetOperation.qubitName(),
                operationIndex
            );
            if (qubit < 0) {
                return false;
            }
            final int value = measureQubit(
                context,
                qubit
            );
            context.performedMeasurement = true;
            if (value == 1) {
                moveBasisBitOneToZero(
                    context,
                    qubit
                );
            }
            return true;
        }
        if (operation instanceof CallableBarrierOperation barrierOperation) {
            for (int i = 0; i < barrierOperation.qubitCount(); i++) {
                if (resolveCallableQubit(
                    context,
                    frame,
                    barrierOperation.qubitName(i),
                    operationIndex
                ) < 0) {
                    return false;
                }
            }
            return true;
        }
        if (operation instanceof CallableClassicalAssignmentOperation assignmentOperation) {
            final Long value = evaluateCallableClassicalExpression(
                context,
                frame,
                assignmentOperation.assignment().value()
            );
            if (value == null) {
                return unsupported(
                    context,
                    SimulationDiagnosticCode.CLASSICAL_CONDITION_UNAVAILABLE,
                    "Callable classical assignment value cannot be evaluated.",
                    operationIndex
                );
            }
            final ClassicalExpression target = callableClassicalTarget(
                frame,
                assignmentOperation.assignment().target()
            );
            return writeClassicalExpression(
                context,
                target,
                value.longValue(),
                operationIndex
            );
        }
        if (operation instanceof CallableBlockOperation blockOperation) {
            return executeCallableBlock(
                context,
                blockOperation.body(),
                frame,
                operationIndex
            );
        }
        if (operation instanceof CallableConditionalBlockOperation conditionalOperation) {
            final Boolean value = evaluateCallablePredicate(
                context,
                frame,
                conditionalOperation.predicate()
            );
            if (value == null) {
                return unsupported(
                    context,
                    SimulationDiagnosticCode.CLASSICAL_CONDITION_UNAVAILABLE,
                    "Callable conditional predicate cannot be evaluated.",
                    operationIndex
                );
            }
            if (value.booleanValue()) {
                return executeCallableBlock(
                    context,
                    conditionalOperation.thenBlock(),
                    frame,
                    operationIndex
                );
            }
            return !conditionalOperation.hasElseBlock()
                || executeCallableBlock(
                    context,
                    conditionalOperation.elseBlock(),
                    frame,
                    operationIndex
                );
        }
        if (operation instanceof CallableForLoopOperation loopOperation) {
            return executeCallableForLoop(
                context,
                loopOperation,
                frame,
                operationIndex
            );
        }
        if (operation instanceof CallableWhileLoopOperation loopOperation) {
            return executeCallableWhileLoop(
                context,
                loopOperation,
                frame,
                operationIndex
            );
        }
        if (operation instanceof CallableDelayOperation delayOperation) {
            for (int i = 0; i < delayOperation.qubitCount(); i++) {
                if (resolveCallableQubit(
                    context,
                    frame,
                    delayOperation.qubitName(i),
                    operationIndex
                ) < 0) {
                    return false;
                }
            }
            return true;
        }
        if (operation instanceof CallableTimingBoxOperation timingBoxOperation) {
            return executeCallableBlock(
                context,
                timingBoxOperation.body(),
                frame,
                operationIndex
            );
        }
        return unsupported(
            context,
            SimulationDiagnosticCode.UNSUPPORTED_OPERATION,
            "Callable operation kind is not supported by the simulator: " + operation.kind() + ".",
            operationIndex
        );
    }

    private boolean executeCallableGate(
        final SimulationContext context,
        final CallableGateOperation operation,
        final CallableExecutionFrame frame,
        final int operationIndex
    ) {
        final QuantumReference[] references = new QuantumReference[operation.qubitCount()];
        for (int i = 0; i < operation.qubitCount(); i++) {
            references[i] = frame.quantumArguments.get(operation.qubitName(i));
            if (references[i] == null) {
                return unsupported(
                    context,
                    SimulationDiagnosticCode.NON_STATIC_QUBIT_REFERENCE,
                    "Callable gate references an unbound quantum argument.",
                    operationIndex
                );
            }
        }
        final int[] qubits = resolveQubits(
            context,
            references,
            operationIndex
        );
        if (qubits == null) {
            return false;
        }
        final double[] parameters = resolveParameters(
            context,
            operation.parameters(),
            operationIndex
        );
        if (parameters == null) {
            return false;
        }
        if (operation.gate() instanceof ModifiedGate modifiedGate) {
            if (!(modifiedGate.baseGate() instanceof StandardGate standardGate)) {
                return unsupported(
                    context,
                    SimulationDiagnosticCode.UNSUPPORTED_GATE,
                    "Only modified standard gates are supported by callable simulation.",
                    operationIndex
                );
            }
            return applyModifiedGate(
                context,
                modifiedGate,
                standardGate,
                qubits,
                parameters,
                operationIndex
            );
        }
        if (!(operation.gate() instanceof StandardGate standardGate)) {
            return unsupported(
                context,
                SimulationDiagnosticCode.UNSUPPORTED_GATE,
                "Only standard gates are supported by callable simulation.",
                operationIndex
            );
        }
        applyStandardGate(
            context,
            standardGate,
            qubits,
            parameters
        );
        return true;
    }

    private boolean executeBranch(
        final SimulationContext context,
        final BranchOperation operation,
        final int operationIndex
    ) {
        boolean shouldBranch = operation.conditionKind() == BranchConditionKind.ALWAYS;
        if (operation.hasCondition()) {
            final Long value = evaluateClassicalExpression(
                context,
                operation.condition()
            );
            if (value == null) {
                return unsupported(
                    context,
                    SimulationDiagnosticCode.CLASSICAL_CONDITION_UNAVAILABLE,
                    "Branch condition cannot be evaluated by the simulator.",
                    operationIndex
                );
            }
            shouldBranch = operation.conditionKind() == BranchConditionKind.WHEN_TRUE
                ? value.longValue() != 0L
                : value.longValue() == 0L;
        }
        if (shouldBranch) {
            context.pendingBranchTarget = operation.targetLabel();
        }
        return true;
    }

    private boolean executeControlled(
        final SimulationContext context,
        final ControlledOperation operation,
        final int operationIndex
    ) {
        final long actualValue = registerValue(
            context,
            operation.condition().register()
        );
        if (actualValue == operation.condition().expectedValue()) {
            return executeOperation(
                context,
                operation.operation(),
                operationIndex
            );
        }
        return true;
    }

    private boolean executeClassicallyControlled(
        final SimulationContext context,
        final ClassicallyControlledOperation operation,
        final int operationIndex
    ) {
        final Boolean value = evaluatePredicate(
            context,
            operation.predicate()
        );
        if (value == null) {
            return unsupported(
                context,
                SimulationDiagnosticCode.CLASSICAL_CONDITION_UNAVAILABLE,
                "Classical predicate cannot be evaluated by the simulator.",
                operationIndex
            );
        }
        if (value.booleanValue()) {
            return executeOperation(
                context,
                operation.operation(),
                operationIndex
            );
        }
        return true;
    }

    private void applyStandardGate(
        final SimulationContext context,
        final StandardGate gate,
        final int[] qubits,
        final double[] parameters
    ) {
        switch (gate) {
            case ID -> {
            }
            case H -> applySingleQubitMatrix(
                context,
                qubits[0],
                ONE_OVER_SQRT_TWO,
                0.0,
                ONE_OVER_SQRT_TWO,
                0.0,
                ONE_OVER_SQRT_TWO,
                0.0,
                -ONE_OVER_SQRT_TWO,
                0.0
            );
            case X -> applySingleQubitMatrix(
                context,
                qubits[0],
                0.0,
                0.0,
                1.0,
                0.0,
                1.0,
                0.0,
                0.0,
                0.0
            );
            case Y -> applySingleQubitMatrix(
                context,
                qubits[0],
                0.0,
                0.0,
                0.0,
                -1.0,
                0.0,
                1.0,
                0.0,
                0.0
            );
            case Z -> applySingleQubitMatrix(
                context,
                qubits[0],
                1.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                -1.0,
                0.0
            );
            case S -> applyPhase(
                context,
                qubits[0],
                Math.PI / 2.0
            );
            case SDG -> applyPhase(
                context,
                qubits[0],
                -Math.PI / 2.0
            );
            case T -> applyPhase(
                context,
                qubits[0],
                Math.PI / 4.0
            );
            case TDG -> applyPhase(
                context,
                qubits[0],
                -Math.PI / 4.0
            );
            case RX -> applyRx(
                context,
                qubits[0],
                parameters[0]
            );
            case RY -> applyRy(
                context,
                qubits[0],
                parameters[0]
            );
            case RZ -> applyRz(
                context,
                qubits[0],
                parameters[0]
            );
            case PHASE -> applyPhase(
                context,
                qubits[0],
                parameters[0]
            );
            case U -> applyU(
                context,
                qubits[0],
                parameters[0],
                parameters[1],
                parameters[2]
            );
            case CX -> applyControlledSingleTarget(
                context,
                qubits[0],
                qubits[1],
                StandardGate.X,
                0.0
            );
            case CY -> applyControlledSingleTarget(
                context,
                qubits[0],
                qubits[1],
                StandardGate.Y,
                0.0
            );
            case CZ -> applyControlledPhase(
                context,
                qubits[0],
                qubits[1],
                Math.PI
            );
            case CPHASE -> applyControlledPhase(
                context,
                qubits[0],
                qubits[1],
                parameters[0]
            );
            case CH -> applyControlledSingleTarget(
                context,
                qubits[0],
                qubits[1],
                StandardGate.H,
                0.0
            );
            case SWAP -> applySwap(
                context,
                qubits[0],
                qubits[1]
            );
            case CCX -> applyToffoli(
                context,
                qubits[0],
                qubits[1],
                qubits[2]
            );
        }
    }

    private void applyInverseStandardGate(
        final SimulationContext context,
        final StandardGate gate,
        final int[] qubits,
        final double[] parameters
    ) {
        switch (gate) {
            case S -> applyStandardGate(
                context,
                StandardGate.SDG,
                qubits,
                parameters
            );
            case SDG -> applyStandardGate(
                context,
                StandardGate.S,
                qubits,
                parameters
            );
            case T -> applyStandardGate(
                context,
                StandardGate.TDG,
                qubits,
                parameters
            );
            case TDG -> applyStandardGate(
                context,
                StandardGate.T,
                qubits,
                parameters
            );
            case RX -> applyRx(
                context,
                qubits[0],
                -parameters[0]
            );
            case RY -> applyRy(
                context,
                qubits[0],
                -parameters[0]
            );
            case RZ -> applyRz(
                context,
                qubits[0],
                -parameters[0]
            );
            case PHASE -> applyPhase(
                context,
                qubits[0],
                -parameters[0]
            );
            case CPHASE -> applyControlledPhase(
                context,
                qubits[0],
                qubits[1],
                -parameters[0]
            );
            case U -> applyUInverse(
                context,
                qubits[0],
                parameters[0],
                parameters[1],
                parameters[2]
            );
            default -> applyStandardGate(
                context,
                gate,
                qubits,
                parameters
            );
        }
    }

    private void applyControlledSingleQubitStandardGate(
        final SimulationContext context,
        final StandardGate gate,
        final int[] qubits,
        final double[] parameters,
        final int controlCount,
        final boolean inverse
    ) {
        final int target = qubits[controlCount];
        final double[] matrix = singleQubitMatrix(
            gate,
            parameters,
            inverse
        );
        applyControlledSingleQubitMatrix(
            context,
            qubits,
            controlCount,
            target,
            matrix
        );
    }

    private double[] singleQubitMatrix(
        final StandardGate gate,
        final double[] parameters,
        final boolean inverse
    ) {
        final double[] matrix = switch (gate) {
            case ID -> new double[] {1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0};
            case H -> new double[] {
                ONE_OVER_SQRT_TWO, 0.0,
                ONE_OVER_SQRT_TWO, 0.0,
                ONE_OVER_SQRT_TWO, 0.0,
                -ONE_OVER_SQRT_TWO, 0.0
            };
            case X -> new double[] {0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0};
            case Y -> new double[] {0.0, 0.0, 0.0, -1.0, 0.0, 1.0, 0.0, 0.0};
            case Z -> new double[] {1.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0};
            case S -> phaseMatrix(Math.PI / 2.0);
            case SDG -> phaseMatrix(-Math.PI / 2.0);
            case T -> phaseMatrix(Math.PI / 4.0);
            case TDG -> phaseMatrix(-Math.PI / 4.0);
            case RX -> rxMatrix(parameters[0]);
            case RY -> ryMatrix(parameters[0]);
            case RZ -> rzMatrix(parameters[0]);
            case PHASE -> phaseMatrix(parameters[0]);
            case U -> uMatrix(
                parameters[0],
                parameters[1],
                parameters[2]
            );
            default -> throw new IllegalArgumentException("Gate is not single-qubit: " + gate.gateName());
        };
        return inverse ? dagger(matrix) : matrix;
    }

    private static double[] phaseMatrix(final double angle) {
        return new double[] {
            1.0, 0.0,
            0.0, 0.0,
            0.0, 0.0,
            Math.cos(angle), Math.sin(angle)
        };
    }

    private static double[] rxMatrix(final double angle) {
        final double cosine = Math.cos(angle / 2.0);
        final double sine = Math.sin(angle / 2.0);
        return new double[] {
            cosine, 0.0,
            0.0, -sine,
            0.0, -sine,
            cosine, 0.0
        };
    }

    private static double[] ryMatrix(final double angle) {
        final double cosine = Math.cos(angle / 2.0);
        final double sine = Math.sin(angle / 2.0);
        return new double[] {
            cosine, 0.0,
            -sine, 0.0,
            sine, 0.0,
            cosine, 0.0
        };
    }

    private static double[] rzMatrix(final double angle) {
        return new double[] {
            Math.cos(-angle / 2.0), Math.sin(-angle / 2.0),
            0.0, 0.0,
            0.0, 0.0,
            Math.cos(angle / 2.0), Math.sin(angle / 2.0)
        };
    }

    private static double[] uMatrix(
        final double theta,
        final double phi,
        final double lambda
    ) {
        final double cosine = Math.cos(theta / 2.0);
        final double sine = Math.sin(theta / 2.0);
        return new double[] {
            cosine, 0.0,
            -Math.cos(lambda) * sine, -Math.sin(lambda) * sine,
            Math.cos(phi) * sine, Math.sin(phi) * sine,
            Math.cos(phi + lambda) * cosine, Math.sin(phi + lambda) * cosine
        };
    }

    private static double[] dagger(final double[] matrix) {
        return new double[] {
            matrix[0], -matrix[1],
            matrix[4], -matrix[5],
            matrix[2], -matrix[3],
            matrix[6], -matrix[7]
        };
    }

    private void applyRx(
        final SimulationContext context,
        final int qubit,
        final double angle
    ) {
        final double cosine = Math.cos(angle / 2.0);
        final double sine = Math.sin(angle / 2.0);
        applySingleQubitMatrix(
            context,
            qubit,
            cosine,
            0.0,
            0.0,
            -sine,
            0.0,
            -sine,
            cosine,
            0.0
        );
    }

    private void applyRy(
        final SimulationContext context,
        final int qubit,
        final double angle
    ) {
        final double cosine = Math.cos(angle / 2.0);
        final double sine = Math.sin(angle / 2.0);
        applySingleQubitMatrix(
            context,
            qubit,
            cosine,
            0.0,
            -sine,
            0.0,
            sine,
            0.0,
            cosine,
            0.0
        );
    }

    private void applyRz(
        final SimulationContext context,
        final int qubit,
        final double angle
    ) {
        applySingleQubitMatrix(
            context,
            qubit,
            Math.cos(-angle / 2.0),
            Math.sin(-angle / 2.0),
            0.0,
            0.0,
            0.0,
            0.0,
            Math.cos(angle / 2.0),
            Math.sin(angle / 2.0)
        );
    }

    private void applyU(
        final SimulationContext context,
        final int qubit,
        final double theta,
        final double phi,
        final double lambda
    ) {
        final double cosine = Math.cos(theta / 2.0);
        final double sine = Math.sin(theta / 2.0);
        applySingleQubitMatrix(
            context,
            qubit,
            cosine,
            0.0,
            -Math.cos(lambda) * sine,
            -Math.sin(lambda) * sine,
            Math.cos(phi) * sine,
            Math.sin(phi) * sine,
            Math.cos(phi + lambda) * cosine,
            Math.sin(phi + lambda) * cosine
        );
    }

    private void applyUInverse(
        final SimulationContext context,
        final int qubit,
        final double theta,
        final double phi,
        final double lambda
    ) {
        final double[] matrix = dagger(uMatrix(
            theta,
            phi,
            lambda
        ));
        applySingleQubitMatrix(
            context,
            qubit,
            matrix[0],
            matrix[1],
            matrix[2],
            matrix[3],
            matrix[4],
            matrix[5],
            matrix[6],
            matrix[7]
        );
    }

    private void applyPhase(
        final SimulationContext context,
        final int qubit,
        final double angle
    ) {
        final int mask = 1 << qubit;
        final double phaseReal = Math.cos(angle);
        final double phaseImaginary = Math.sin(angle);
        for (int i = 0; i < context.stateReal.length; i++) {
            if ((i & mask) != 0) {
                final double real = context.stateReal[i];
                final double imaginary = context.stateImaginary[i];
                context.stateReal[i] = real * phaseReal - imaginary * phaseImaginary;
                context.stateImaginary[i] = real * phaseImaginary + imaginary * phaseReal;
            }
        }
    }

    private void applySingleQubitMatrix(
        final SimulationContext context,
        final int qubit,
        final double m00Real,
        final double m00Imaginary,
        final double m01Real,
        final double m01Imaginary,
        final double m10Real,
        final double m10Imaginary,
        final double m11Real,
        final double m11Imaginary
    ) {
        final int mask = 1 << qubit;
        for (int i = 0; i < context.stateReal.length; i++) {
            if ((i & mask) == 0) {
                final int oneIndex = i | mask;
                final double zeroReal = context.stateReal[i];
                final double zeroImaginary = context.stateImaginary[i];
                final double oneReal = context.stateReal[oneIndex];
                final double oneImaginary = context.stateImaginary[oneIndex];
                context.stateReal[i] = m00Real * zeroReal - m00Imaginary * zeroImaginary
                    + m01Real * oneReal - m01Imaginary * oneImaginary;
                context.stateImaginary[i] = m00Real * zeroImaginary + m00Imaginary * zeroReal
                    + m01Real * oneImaginary + m01Imaginary * oneReal;
                context.stateReal[oneIndex] = m10Real * zeroReal - m10Imaginary * zeroImaginary
                    + m11Real * oneReal - m11Imaginary * oneImaginary;
                context.stateImaginary[oneIndex] = m10Real * zeroImaginary + m10Imaginary * zeroReal
                    + m11Real * oneImaginary + m11Imaginary * oneReal;
            }
        }
    }

    private void applyControlledSingleTarget(
        final SimulationContext context,
        final int control,
        final int target,
        final StandardGate gate,
        final double parameter
    ) {
        final int controlMask = 1 << control;
        final int targetMask = 1 << target;
        for (int i = 0; i < context.stateReal.length; i++) {
            if (
                (i & controlMask) != 0
                && (i & targetMask) == 0
            ) {
                final int oneIndex = i | targetMask;
                final double zeroReal = context.stateReal[i];
                final double zeroImaginary = context.stateImaginary[i];
                final double oneReal = context.stateReal[oneIndex];
                final double oneImaginary = context.stateImaginary[oneIndex];
                if (gate == StandardGate.X) {
                    context.stateReal[i] = oneReal;
                    context.stateImaginary[i] = oneImaginary;
                    context.stateReal[oneIndex] = zeroReal;
                    context.stateImaginary[oneIndex] = zeroImaginary;
                } else if (gate == StandardGate.Y) {
                    context.stateReal[i] = oneImaginary;
                    context.stateImaginary[i] = -oneReal;
                    context.stateReal[oneIndex] = -zeroImaginary;
                    context.stateImaginary[oneIndex] = zeroReal;
                } else if (gate == StandardGate.H) {
                    context.stateReal[i] = (zeroReal + oneReal) * ONE_OVER_SQRT_TWO;
                    context.stateImaginary[i] = (zeroImaginary + oneImaginary) * ONE_OVER_SQRT_TWO;
                    context.stateReal[oneIndex] = (zeroReal - oneReal) * ONE_OVER_SQRT_TWO;
                    context.stateImaginary[oneIndex] = (zeroImaginary - oneImaginary) * ONE_OVER_SQRT_TWO;
                } else if (gate == StandardGate.PHASE) {
                    final double phaseReal = Math.cos(parameter);
                    final double phaseImaginary = Math.sin(parameter);
                    context.stateReal[oneIndex] = oneReal * phaseReal - oneImaginary * phaseImaginary;
                    context.stateImaginary[oneIndex] = oneReal * phaseImaginary + oneImaginary * phaseReal;
                }
            }
        }
    }

    private void applyControlledSingleQubitMatrix(
        final SimulationContext context,
        final int[] qubits,
        final int controlCount,
        final int target,
        final double[] matrix
    ) {
        int controlMask = 0;
        for (int i = 0; i < controlCount; i++) {
            controlMask |= 1 << qubits[i];
        }
        final int targetMask = 1 << target;
        for (int i = 0; i < context.stateReal.length; i++) {
            if (
                (i & controlMask) == controlMask
                && (i & targetMask) == 0
            ) {
                final int oneIndex = i | targetMask;
                final double zeroReal = context.stateReal[i];
                final double zeroImaginary = context.stateImaginary[i];
                final double oneReal = context.stateReal[oneIndex];
                final double oneImaginary = context.stateImaginary[oneIndex];
                context.stateReal[i] = matrix[0] * zeroReal - matrix[1] * zeroImaginary
                    + matrix[2] * oneReal - matrix[3] * oneImaginary;
                context.stateImaginary[i] = matrix[0] * zeroImaginary + matrix[1] * zeroReal
                    + matrix[2] * oneImaginary + matrix[3] * oneReal;
                context.stateReal[oneIndex] = matrix[4] * zeroReal - matrix[5] * zeroImaginary
                    + matrix[6] * oneReal - matrix[7] * oneImaginary;
                context.stateImaginary[oneIndex] = matrix[4] * zeroImaginary + matrix[5] * zeroReal
                    + matrix[6] * oneImaginary + matrix[7] * oneReal;
            }
        }
    }

    private void applyControlledPhase(
        final SimulationContext context,
        final int control,
        final int target,
        final double angle
    ) {
        final int controlMask = 1 << control;
        final int targetMask = 1 << target;
        final double phaseReal = Math.cos(angle);
        final double phaseImaginary = Math.sin(angle);
        for (int i = 0; i < context.stateReal.length; i++) {
            if (
                (i & controlMask) != 0
                && (i & targetMask) != 0
            ) {
                final double real = context.stateReal[i];
                final double imaginary = context.stateImaginary[i];
                context.stateReal[i] = real * phaseReal - imaginary * phaseImaginary;
                context.stateImaginary[i] = real * phaseImaginary + imaginary * phaseReal;
            }
        }
    }

    private void applySwap(
        final SimulationContext context,
        final int left,
        final int right
    ) {
        final int leftMask = 1 << left;
        final int rightMask = 1 << right;
        for (int i = 0; i < context.stateReal.length; i++) {
            final boolean leftSet = (i & leftMask) != 0;
            final boolean rightSet = (i & rightMask) != 0;
            if (
                leftSet
                && !rightSet
            ) {
                final int swapped = (i ^ leftMask) | rightMask;
                swapAmplitudes(
                    context,
                    i,
                    swapped
                );
            }
        }
    }

    private void applyToffoli(
        final SimulationContext context,
        final int firstControl,
        final int secondControl,
        final int target
    ) {
        final int firstMask = 1 << firstControl;
        final int secondMask = 1 << secondControl;
        final int targetMask = 1 << target;
        for (int i = 0; i < context.stateReal.length; i++) {
            if (
                (i & firstMask) != 0
                && (i & secondMask) != 0
                && (i & targetMask) == 0
            ) {
                swapAmplitudes(
                    context,
                    i,
                    i | targetMask
                );
            }
        }
    }

    private static void swapAmplitudes(
        final SimulationContext context,
        final int left,
        final int right
    ) {
        final double real = context.stateReal[left];
        final double imaginary = context.stateImaginary[left];
        context.stateReal[left] = context.stateReal[right];
        context.stateImaginary[left] = context.stateImaginary[right];
        context.stateReal[right] = real;
        context.stateImaginary[right] = imaginary;
    }

    private static double probabilityOfOne(
        final SimulationContext context,
        final int qubit
    ) {
        final int mask = 1 << qubit;
        double probability = 0.0;
        for (int i = 0; i < context.stateReal.length; i++) {
            if ((i & mask) != 0) {
                probability += context.stateReal[i] * context.stateReal[i]
                    + context.stateImaginary[i] * context.stateImaginary[i];
            }
        }
        return probability;
    }

    private static int measureQubit(
        final SimulationContext context,
        final int qubit
    ) {
        final double probabilityOne = probabilityOfOne(
            context,
            qubit
        );
        final int value = context.random.nextDouble() < probabilityOne ? 1 : 0;
        collapseQubit(
            context,
            qubit,
            value,
            value == 1 ? probabilityOne : 1.0 - probabilityOne
        );
        return value;
    }

    private static void collapseQubit(
        final SimulationContext context,
        final int qubit,
        final int value,
        final double probability
    ) {
        final int mask = 1 << qubit;
        final double scale = probability <= EPSILON ? 0.0 : 1.0 / Math.sqrt(probability);
        for (int i = 0; i < context.stateReal.length; i++) {
            final boolean bitSet = (i & mask) != 0;
            if (bitSet == (value == 1)) {
                context.stateReal[i] *= scale;
                context.stateImaginary[i] *= scale;
            } else {
                context.stateReal[i] = 0.0;
                context.stateImaginary[i] = 0.0;
            }
        }
    }

    private static void moveBasisBitOneToZero(
        final SimulationContext context,
        final int qubit
    ) {
        final int mask = 1 << qubit;
        for (int i = 0; i < context.stateReal.length; i++) {
            if ((i & mask) == 0) {
                final int oneIndex = i | mask;
                context.stateReal[i] = context.stateReal[oneIndex];
                context.stateImaginary[i] = context.stateImaginary[oneIndex];
                context.stateReal[oneIndex] = 0.0;
                context.stateImaginary[oneIndex] = 0.0;
            }
        }
    }

    private int[] resolveQubits(
        final SimulationContext context,
        final QuantumReference[] references,
        final int operationIndex
    ) {
        final int[] qubits = new int[references.length];
        for (int i = 0; i < references.length; i++) {
            final int qubitIndex = resolveQubit(
                context,
                references[i],
                operationIndex
            );
            if (qubitIndex < 0) {
                return null;
            }
            qubits[i] = qubitIndex;
        }
        return qubits;
    }

    private int resolveQubit(
        final SimulationContext context,
        final QuantumReference reference,
        final int operationIndex
    ) {
        if (reference.isStatic()) {
            return context.qubitIndex(reference.qubit());
        }
        if (reference.kind() == ru.pathcreator.vadim.quantum.domain.operation.QuantumReferenceKind.DYNAMIC_REGISTER_INDEX) {
            final Long index = evaluateClassicalExpression(
                context,
                reference.indexExpression()
            );
            if (
                index == null
                || index.longValue() < 0L
                || index.longValue() >= reference.register().size()
                || index.longValue() > Integer.MAX_VALUE
            ) {
                unsupported(
                    context,
                    SimulationDiagnosticCode.NON_STATIC_QUBIT_REFERENCE,
                    "Dynamic qubit reference index cannot be evaluated inside register bounds.",
                    operationIndex
                );
                return -1;
            }
            return context.qubitIndex(reference.register().get((int) index.longValue()));
        }
        if (reference.kind() == ru.pathcreator.vadim.quantum.domain.operation.QuantumReferenceKind.HARDWARE_QUBIT) {
            if (reference.hardwareIndex() >= context.qubitCount) {
                unsupported(
                    context,
                    SimulationDiagnosticCode.NON_STATIC_QUBIT_REFERENCE,
                    "Hardware qubit reference is outside the simulated state-vector width.",
                    operationIndex
                );
                return -1;
            }
            return reference.hardwareIndex();
        }
        unsupported(
            context,
            SimulationDiagnosticCode.NON_STATIC_QUBIT_REFERENCE,
            "Quantum reference kind is not supported by the simulator.",
            operationIndex
        );
        return -1;
    }

    private double[] resolveParameters(
        final SimulationContext context,
        final ParameterExpression[] expressions,
        final int operationIndex
    ) {
        final double[] values = new double[expressions.length];
        for (int i = 0; i < expressions.length; i++) {
            try {
                values[i] = parameterEvaluator.evaluate(
                    expressions[i],
                    context.options.parameterBindings()
                );
            } catch (final IllegalArgumentException exception) {
                unsupported(
                    context,
                    SimulationDiagnosticCode.UNBOUND_PARAMETER,
                    "Gate parameter cannot be evaluated: " + exception.getMessage(),
                    operationIndex
                );
                return null;
            }
        }
        return values;
    }

    private Long evaluateClassicalExpression(
        final SimulationContext context,
        final ClassicalExpression expression
    ) {
        if (expression.kind() == ClassicalExpressionKind.INTEGER) {
            return expression.integerValue();
        }
        if (expression.kind() == ClassicalExpressionKind.BIT_REFERENCE) {
            return (long) context.classicalBits[context.classicalBitIndex(expression.bit())];
        }
        if (expression.kind() == ClassicalExpressionKind.REGISTER_REFERENCE) {
            return registerValue(
                context,
                expression.register()
            );
        }
        if (expression.kind() == ClassicalExpressionKind.VARIABLE_REFERENCE) {
            return context.localClassicalValues.get(expression.variableName());
        }
        if (expression.kind() == ClassicalExpressionKind.SYMBOLIC_REFERENCE) {
            return readSymbolicReference(
                context,
                expression.symbolicText()
            );
        }
        if (expression.kind() == ClassicalExpressionKind.CALL) {
            return evaluateClassicalCall(
                context,
                expression
            );
        }
        if (expression.kind() != ClassicalExpressionKind.BINARY_OPERATION) {
            return null;
        }
        final Long left = evaluateClassicalExpression(
            context,
            expression.leftExpression()
        );
        final Long right = evaluateClassicalExpression(
            context,
            expression.rightExpression()
        );
        if (
            left == null
            || right == null
        ) {
            return null;
        }
        return applyClassicalBinary(
            expression.binaryOperator(),
            left,
            right
        );
    }

    private static Long applyClassicalBinary(
        final ClassicalBinaryOperator operator,
        final long left,
        final long right
    ) {
        return switch (operator) {
            case ADD -> left + right;
            case SUBTRACT -> left - right;
            case MULTIPLY -> left * right;
            case DIVIDE -> right == 0L ? null : left / right;
            case MODULO -> right == 0L ? null : left % right;
            case BITWISE_AND -> left & right;
            case BITWISE_OR -> left | right;
            case BITWISE_XOR -> left ^ right;
            case SHIFT_LEFT -> left << right;
            case SHIFT_RIGHT -> left >> right;
        };
    }

    private Long evaluateClassicalCall(
        final SimulationContext context,
        final ClassicalExpression expression
    ) {
        if (
            "identity".equals(expression.callableName())
            && expression.callArgumentCount() == 1
        ) {
            return evaluateClassicalExpression(
                context,
                expression.callArgument(0)
            );
        }
        if (
            "bit_not".equals(expression.callableName())
            && expression.callArgumentCount() == 1
        ) {
            final Long value = evaluateClassicalExpression(
                context,
                expression.callArgument(0)
            );
            if (value == null) {
                return null;
            }
            return value.longValue() == 0L ? 1L : 0L;
        }
        return null;
    }

    private boolean executeCallableForLoop(
        final SimulationContext context,
        final CallableForLoopOperation operation,
        final CallableExecutionFrame frame,
        final int operationIndex
    ) {
        long iterations = 0L;
        final boolean hadPrevious = context.localClassicalValues.containsKey(operation.variableName());
        final Long previous = context.localClassicalValues.get(operation.variableName());
        if (operation.step() > 0L) {
            for (long value = operation.startInclusive(); value <= operation.endInclusive(); value += operation.step()) {
                context.localClassicalValues.put(
                    operation.variableName(),
                    value
                );
                if (iterations++ >= MAX_LOOP_ITERATIONS) {
                    return unsupported(
                        context,
                        SimulationDiagnosticCode.UNSUPPORTED_OPERATION,
                        "Callable for-loop reached the simulator safety limit.",
                        operationIndex
                    );
                }
                if (!executeCallableBlock(
                    context,
                    operation.body(),
                    frame,
                    operationIndex
                )) {
                    return false;
                }
            }
        } else {
            for (long value = operation.startInclusive(); value >= operation.endInclusive(); value += operation.step()) {
                context.localClassicalValues.put(
                    operation.variableName(),
                    value
                );
                if (iterations++ >= MAX_LOOP_ITERATIONS) {
                    return unsupported(
                        context,
                        SimulationDiagnosticCode.UNSUPPORTED_OPERATION,
                        "Callable for-loop reached the simulator safety limit.",
                        operationIndex
                    );
                }
                if (!executeCallableBlock(
                    context,
                    operation.body(),
                    frame,
                    operationIndex
                )) {
                    return false;
                }
            }
        }
        restoreLocal(
            context,
            operation.variableName(),
            hadPrevious,
            previous
        );
        return true;
    }

    private boolean executeCallableWhileLoop(
        final SimulationContext context,
        final CallableWhileLoopOperation operation,
        final CallableExecutionFrame frame,
        final int operationIndex
    ) {
        long iterations = 0L;
        while (true) {
            final Boolean value = evaluateCallablePredicate(
                context,
                frame,
                operation.predicate()
            );
            if (value == null) {
                return unsupported(
                    context,
                    SimulationDiagnosticCode.CLASSICAL_CONDITION_UNAVAILABLE,
                    "Callable while-loop predicate cannot be evaluated.",
                    operationIndex
                );
            }
            if (!value.booleanValue()) {
                return true;
            }
            if (iterations++ >= MAX_LOOP_ITERATIONS) {
                return unsupported(
                    context,
                    SimulationDiagnosticCode.UNSUPPORTED_OPERATION,
                    "Callable while-loop reached the simulator safety limit.",
                    operationIndex
                );
            }
            if (!executeCallableBlock(
                context,
                operation.body(),
                frame,
                operationIndex
            )) {
                return false;
            }
        }
    }

    private int resolveCallableQubit(
        final SimulationContext context,
        final CallableExecutionFrame frame,
        final String name,
        final int operationIndex
    ) {
        final QuantumReference reference = frame.quantumArguments.get(name);
        if (reference == null) {
            unsupported(
                context,
                SimulationDiagnosticCode.NON_STATIC_QUBIT_REFERENCE,
                "Callable operation references an unbound quantum argument.",
                operationIndex
            );
            return -1;
        }
        return resolveQubit(
            context,
            reference,
            operationIndex
        );
    }

    private Long evaluateCallableClassicalExpression(
        final SimulationContext context,
        final CallableExecutionFrame frame,
        final CallableClassicalExpression expression
    ) {
        if (expression.kind() == CallableClassicalExpressionKind.INTEGER) {
            return expression.integerValue();
        }
        final ClassicalExpression actual = frame.classicalArguments.get(expression.argumentName());
        return actual == null
            ? null
            : evaluateClassicalExpression(
                context,
                actual
            );
    }

    private Boolean evaluateCallablePredicate(
        final SimulationContext context,
        final CallableExecutionFrame frame,
        final CallableClassicalPredicate predicate
    ) {
        return switch (predicate.kind()) {
            case COMPARISON -> {
                final Long left = evaluateCallableClassicalExpression(
                    context,
                    frame,
                    predicate.leftExpression()
                );
                final Long right = evaluateCallableClassicalExpression(
                    context,
                    frame,
                    predicate.rightExpression()
                );
                yield left == null || right == null
                    ? null
                    : compare(
                        predicate.comparisonOperator(),
                        left.longValue(),
                        right.longValue()
                    );
            }
            case NOT -> {
                final Boolean value = evaluateCallablePredicate(
                    context,
                    frame,
                    predicate.leftPredicate()
                );
                yield value == null
                    ? null
                    : !value.booleanValue();
            }
            case BOOLEAN -> {
                final Boolean left = evaluateCallablePredicate(
                    context,
                    frame,
                    predicate.leftPredicate()
                );
                final Boolean right = evaluateCallablePredicate(
                    context,
                    frame,
                    predicate.rightPredicate()
                );
                yield left == null || right == null
                    ? null
                    : predicate.booleanOperator() == ClassicalBooleanOperator.AND
                        ? left.booleanValue() && right.booleanValue()
                        : left.booleanValue() || right.booleanValue();
            }
        };
    }

    private ClassicalExpression callableClassicalTarget(
        final CallableExecutionFrame frame,
        final CallableClassicalExpression expression
    ) {
        if (expression.kind() == CallableClassicalExpressionKind.ARGUMENT_REFERENCE) {
            return frame.classicalArguments.get(expression.argumentName());
        }
        return null;
    }

    private boolean writeClassicalExpression(
        final SimulationContext context,
        final ClassicalExpression target,
        final long value,
        final int operationIndex
    ) {
        if (target == null) {
            return unsupported(
                context,
                SimulationDiagnosticCode.CLASSICAL_CONDITION_UNAVAILABLE,
                "Classical write target is not available.",
                operationIndex
            );
        }
        if (target.kind() == ClassicalExpressionKind.BIT_REFERENCE) {
            context.classicalBits[context.classicalBitIndex(target.bit())] = (int) (value & 1L);
            return true;
        }
        if (target.kind() == ClassicalExpressionKind.REGISTER_REFERENCE) {
            writeRegisterValue(
                context,
                target.register(),
                value
            );
            return true;
        }
        if (target.kind() == ClassicalExpressionKind.VARIABLE_REFERENCE) {
            context.localClassicalValues.put(
                target.variableName(),
                value
            );
            return true;
        }
        if (target.kind() == ClassicalExpressionKind.SYMBOLIC_REFERENCE) {
            return writeSymbolicReference(
                context,
                target.symbolicText(),
                value
            );
        }
        return unsupported(
            context,
            SimulationDiagnosticCode.CLASSICAL_CONDITION_UNAVAILABLE,
            "Classical write target kind is not writable.",
            operationIndex
        );
    }

    private static void restoreLocal(
        final SimulationContext context,
        final String name,
        final boolean hadPrevious,
        final Long previous
    ) {
        if (hadPrevious) {
            context.localClassicalValues.put(
                name,
                previous
            );
        } else {
            context.localClassicalValues.remove(name);
        }
    }

    private Long readSymbolicReference(
        final SimulationContext context,
        final String text
    ) {
        final SymbolicArrayReference reference = parseSymbolicArrayReference(
            context,
            text
        );
        if (reference == null) {
            return context.localClassicalValues.get(text);
        }
        if (
            reference.index < 0L
            || reference.index >= reference.values.length
        ) {
            return null;
        }
        return reference.values[(int) reference.index];
    }

    private boolean writeSymbolicReference(
        final SimulationContext context,
        final String text,
        final long value
    ) {
        final SymbolicArrayReference reference = parseSymbolicArrayReference(
            context,
            text
        );
        if (reference == null) {
            context.localClassicalValues.put(
                text,
                value
            );
            return true;
        }
        if (
            reference.index < 0L
            || reference.index >= reference.values.length
        ) {
            return false;
        }
        reference.values[(int) reference.index] = value;
        return true;
    }

    private SymbolicArrayReference parseSymbolicArrayReference(
        final SimulationContext context,
        final String text
    ) {
        final int open = text.indexOf('[');
        final int close = text.endsWith("]")
            ? text.length() - 1
            : -1;
        if (
            open <= 0
            || close <= open
        ) {
            return null;
        }
        final String name = text.substring(
            0,
            open
        ).trim();
        final long[] values = context.localClassicalArrays.get(name);
        if (values == null) {
            return null;
        }
        final String indexText = text.substring(
            open + 1,
            close
        ).trim();
        final Long index = parseIndex(
            context,
            indexText
        );
        if (index == null) {
            return null;
        }
        return new SymbolicArrayReference(
            values,
            index.longValue()
        );
    }

    private Long parseIndex(
        final SimulationContext context,
        final String text
    ) {
        try {
            return Long.parseLong(text);
        } catch (final NumberFormatException exception) {
            return context.localClassicalValues.get(text);
        }
    }

    private static long[] parseArrayInitializer(final String text) {
        String normalized = text.trim();
        if (
            normalized.startsWith("{")
            && normalized.endsWith("}")
        ) {
            normalized = normalized.substring(
                1,
                normalized.length() - 1
            );
        }
        if (normalized.isBlank()) {
            return new long[0];
        }
        final String[] parts = normalized.split(",");
        final long[] values = new long[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                values[i] = Long.parseLong(parts[i].trim());
            } catch (final NumberFormatException exception) {
                return null;
            }
        }
        return values;
    }

    private Boolean evaluatePredicate(
        final SimulationContext context,
        final ClassicalPredicate predicate
    ) {
        if (predicate.kind() == ClassicalPredicateKind.COMPARISON) {
            final Long left = evaluateClassicalExpression(
                context,
                predicate.leftExpression()
            );
            final Long right = evaluateClassicalExpression(
                context,
                predicate.rightExpression()
            );
            if (
                left == null
                || right == null
            ) {
                return null;
            }
            return compare(
                predicate.comparisonOperator(),
                left,
                right
            );
        }
        if (predicate.kind() == ClassicalPredicateKind.NOT) {
            final Boolean value = evaluatePredicate(
                context,
                predicate.leftPredicate()
            );
            return value == null ? null : !value.booleanValue();
        }
        final Boolean left = evaluatePredicate(
            context,
            predicate.leftPredicate()
        );
        final Boolean right = evaluatePredicate(
            context,
            predicate.rightPredicate()
        );
        if (
            left == null
            || right == null
        ) {
            return null;
        }
        return predicate.booleanOperator() == ClassicalBooleanOperator.AND
            ? left.booleanValue() && right.booleanValue()
            : left.booleanValue() || right.booleanValue();
    }

    private static boolean compare(
        final ClassicalComparisonOperator operator,
        final long left,
        final long right
    ) {
        return switch (operator) {
            case EQUAL -> left == right;
            case NOT_EQUAL -> left != right;
            case LESS_THAN -> left < right;
            case LESS_THAN_OR_EQUAL -> left <= right;
            case GREATER_THAN -> left > right;
            case GREATER_THAN_OR_EQUAL -> left >= right;
        };
    }

    private static long registerValue(
        final SimulationContext context,
        final ClassicalRegister register
    ) {
        final int offset = context.classicalRegisterOffsets.get(register);
        long value = 0L;
        for (int i = 0; i < register.size(); i++) {
            if (context.classicalBits[offset + i] != 0) {
                value |= 1L << i;
            }
        }
        return value;
    }

    private static void writeRegisterValue(
        final SimulationContext context,
        final ClassicalRegister register,
        final long value
    ) {
        final int offset = context.classicalRegisterOffsets.get(register);
        for (int i = 0; i < register.size(); i++) {
            context.classicalBits[offset + i] = ((value & (1L << i)) == 0L) ? 0 : 1;
        }
    }

    private boolean unsupported(
        final SimulationContext context,
        final SimulationDiagnosticCode code,
        final String message,
        final int operationIndex
    ) {
        final SimulationDiagnosticSeverity severity = context.options.unsupportedOperationPolicy()
            == SimulationUnsupportedOperationPolicy.FAIL
                ? SimulationDiagnosticSeverity.ERROR
                : SimulationDiagnosticSeverity.WARNING;
        if (context.diagnosticsEnabled) {
            context.diagnostics.add(SimulationDiagnostic.of(
                severity,
                code,
                message,
                0,
                operationIndex
            ));
        }
        return severity != SimulationDiagnosticSeverity.ERROR;
    }

    private static SimulationResult emptyResult(
        final SimulationOptions options,
        final List<SimulationDiagnostic> diagnostics
    ) {
        return SimulationResult.of(
            0,
            0,
            options.shots(),
            Map.of(),
            List.of(),
            diagnostics
        );
    }

    private interface OperationSequence {

        int operationCount();

        Operation operation(int index);
    }

    private static final class CircuitOperationSequence implements OperationSequence {

        private final QuantumCircuit circuit;

        private CircuitOperationSequence(final QuantumCircuit circuit) {
            this.circuit = circuit;
        }

        private static CircuitOperationSequence of(final QuantumCircuit circuit) {
            return new CircuitOperationSequence(circuit);
        }

        @Override
        public int operationCount() {
            return circuit.operationCount();
        }

        @Override
        public Operation operation(final int index) {
            return circuit.operation(index);
        }
    }

    private static final class BlockOperationSequence implements OperationSequence {

        private final OperationBlock block;

        private BlockOperationSequence(final OperationBlock block) {
            this.block = block;
        }

        private static BlockOperationSequence of(final OperationBlock block) {
            return new BlockOperationSequence(block);
        }

        @Override
        public int operationCount() {
            return block.operationCount();
        }

        @Override
        public Operation operation(final int index) {
            return block.operation(index);
        }
    }

    private static final class SymbolicArrayReference {

        private final long[] values;
        private final long index;

        private SymbolicArrayReference(
            final long[] values,
            final long index
        ) {
            this.values = values;
            this.index = index;
        }
    }

    private static final class CallableExecutionFrame {

        private final HashMap<String, QuantumReference> quantumArguments;
        private final HashMap<String, ClassicalExpression> classicalArguments;

        private CallableExecutionFrame(
            final HashMap<String, QuantumReference> quantumArguments,
            final HashMap<String, ClassicalExpression> classicalArguments
        ) {
            this.quantumArguments = quantumArguments;
            this.classicalArguments = classicalArguments;
        }
    }

    private static final class ComplexValue {

        private final double real;
        private final double imaginary;

        private ComplexValue(
            final double real,
            final double imaginary
        ) {
            this.real = real;
            this.imaginary = imaginary;
        }
    }

    private static final class SimulationContext {

        private final SimulationOptions options;
        private final ArrayList<SimulationDiagnostic> diagnostics;
        private final HashMap<Qubit, Integer> qubitIndexes;
        private final HashMap<ClassicalBit, Integer> classicalBitIndexes;
        private final HashMap<ClassicalRegister, Integer> classicalRegisterOffsets;
        private final HashMap<String, Long> localClassicalValues;
        private final HashMap<String, long[]> localClassicalArrays;
        private final HashMap<String, CallableDefinition> callableDefinitions;
        private final Random random;
        private final boolean diagnosticsEnabled;
        private final int qubitCount;
        private final int classicalBitCount;
        private final int[] classicalBits;
        private boolean performedMeasurement;
        private boolean halted;
        private String pendingBranchTarget;
        private double[] stateReal;
        private double[] stateImaginary;

        private SimulationContext(
            final SimulationOptions options,
            final ArrayList<SimulationDiagnostic> diagnostics,
            final HashMap<Qubit, Integer> qubitIndexes,
            final HashMap<ClassicalBit, Integer> classicalBitIndexes,
            final HashMap<ClassicalRegister, Integer> classicalRegisterOffsets,
            final HashMap<String, CallableDefinition> callableDefinitions,
            final Random random,
            final boolean diagnosticsEnabled,
            final int qubitCount,
            final int classicalBitCount
        ) {
            this.options = options;
            this.diagnostics = diagnostics;
            this.qubitIndexes = qubitIndexes;
            this.classicalBitIndexes = classicalBitIndexes;
            this.classicalRegisterOffsets = classicalRegisterOffsets;
            this.localClassicalValues = new HashMap<>();
            this.localClassicalArrays = new HashMap<>();
            this.callableDefinitions = callableDefinitions;
            this.random = random;
            this.diagnosticsEnabled = diagnosticsEnabled;
            this.qubitCount = qubitCount;
            this.classicalBitCount = classicalBitCount;
            this.classicalBits = new int[classicalBitCount];
        }

        private static SimulationContext create(
            final QuantumCircuit circuit,
            final SimulationOptions options,
            final ArrayList<SimulationDiagnostic> diagnostics,
            final Random random,
            final boolean diagnosticsEnabled
        ) {
            final HashMap<Qubit, Integer> qubitIndexes = new HashMap<>();
            int qubitOffset = 0;
            for (int i = 0; i < circuit.quantumRegisterCount(); i++) {
                final QuantumRegister register = circuit.quantumRegister(i);
                for (int j = 0; j < register.size(); j++) {
                    qubitIndexes.put(
                        register.get(j),
                        qubitOffset + j
                    );
                }
                qubitOffset += register.size();
            }
            final HashMap<ClassicalBit, Integer> classicalBitIndexes = new HashMap<>();
            final HashMap<ClassicalRegister, Integer> classicalRegisterOffsets = new HashMap<>();
            int classicalOffset = 0;
            for (int i = 0; i < circuit.classicalRegisterCount(); i++) {
                final ClassicalRegister register = circuit.classicalRegister(i);
                classicalRegisterOffsets.put(
                    register,
                    classicalOffset
                );
                for (int j = 0; j < register.size(); j++) {
                    classicalBitIndexes.put(
                        register.get(j),
                        classicalOffset + j
                    );
                }
                classicalOffset += register.size();
            }
            final HashMap<String, CallableDefinition> callableDefinitions = new HashMap<>();
            for (int i = 0; i < circuit.program().callableDefinitionCount(); i++) {
                final CallableDefinition definition = circuit.program().callableDefinition(i);
                callableDefinitions.put(
                    definition.name(),
                    definition
                );
            }
            return new SimulationContext(
                options,
                diagnostics,
                qubitIndexes,
                classicalBitIndexes,
                classicalRegisterOffsets,
                callableDefinitions,
                random,
                diagnosticsEnabled,
                qubitOffset,
                classicalOffset
            );
        }

        private void initializeState() {
            final int stateSize = 1 << qubitCount;
            stateReal = new double[stateSize];
            stateImaginary = new double[stateSize];
            stateReal[0] = 1.0;
        }

        private int qubitIndex(final Qubit qubit) {
            return qubitIndexes.get(qubit);
        }

        private int classicalBitIndex(final ClassicalBit bit) {
            return classicalBitIndexes.get(bit);
        }

        private SimulationResult result(final Map<String, Long> counts) {
            return SimulationResult.of(
                qubitCount,
                classicalBitCount,
                options.shots(),
                counts,
                stateVector(),
                diagnostics
            );
        }

        private String finalShotBitString() {
            if (classicalBitCount > 0) {
                final StringBuilder builder = new StringBuilder(classicalBitCount);
                for (int i = classicalBitCount - 1; i >= 0; i--) {
                    builder.append(classicalBits[i]);
                }
                return builder.toString();
            }
            final double[] cumulative = cumulativeProbabilities();
            return basisBitString(
                sampleBasis(
                    cumulative,
                    random.nextDouble()
                ),
                qubitCount
            );
        }

        private double[] cumulativeProbabilities() {
            final double[] cumulative = new double[stateReal.length];
            double sum = 0.0;
            for (int i = 0; i < stateReal.length; i++) {
                sum += stateReal[i] * stateReal[i] + stateImaginary[i] * stateImaginary[i];
                cumulative[i] = sum;
            }
            cumulative[cumulative.length - 1] = 1.0;
            return cumulative;
        }

        private static int sampleBasis(
            final double[] cumulative,
            final double value
        ) {
            int low = 0;
            int high = cumulative.length - 1;
            while (low < high) {
                final int middle = (low + high) >>> 1;
                if (value <= cumulative[middle]) {
                    high = middle;
                } else {
                    low = middle + 1;
                }
            }
            return low;
        }

        private List<StateVectorAmplitude> stateVector() {
            if (!options.captureStateVector()) {
                return List.of();
            }
            final ArrayList<StateVectorAmplitude> amplitudes = new ArrayList<>(stateReal.length);
            for (int i = 0; i < stateReal.length; i++) {
                amplitudes.add(new StateVectorAmplitude(
                    basisBitString(
                        i,
                        qubitCount
                    ),
                    stateReal[i],
                    stateImaginary[i]
                ));
            }
            return amplitudes;
        }

        private static String basisBitString(
            final int basis,
            final int width
        ) {
            final StringBuilder builder = new StringBuilder(width);
            for (int i = width - 1; i >= 0; i--) {
                builder.append((basis & (1 << i)) == 0 ? '0' : '1');
            }
            return builder.toString();
        }
    }
}
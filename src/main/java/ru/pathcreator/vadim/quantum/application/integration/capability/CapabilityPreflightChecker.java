/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.integration.capability;

import java.util.ArrayList;

import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnostic;
import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnosticCode;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpressionKind;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicateKind;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.BarrierOperation;
import ru.pathcreator.vadim.quantum.domain.operation.BlockOperation;
import ru.pathcreator.vadim.quantum.domain.operation.BranchOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalAssignmentOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicallyControlledOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ConditionalBlockOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ControlledOperation;
import ru.pathcreator.vadim.quantum.domain.operation.DelayOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ForLoopOperation;
import ru.pathcreator.vadim.quantum.domain.operation.MeasureOperation;
import ru.pathcreator.vadim.quantum.domain.operation.Operation;
import ru.pathcreator.vadim.quantum.domain.operation.OperationBlock;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.HaltOperation;
import ru.pathcreator.vadim.quantum.domain.operation.LabelOperation;
import ru.pathcreator.vadim.quantum.domain.operation.QuantumReference;
import ru.pathcreator.vadim.quantum.domain.operation.QuantumReferenceKind;
import ru.pathcreator.vadim.quantum.domain.operation.ResetOperation;
import ru.pathcreator.vadim.quantum.domain.operation.TimingBoxOperation;
import ru.pathcreator.vadim.quantum.domain.operation.WaitOperation;
import ru.pathcreator.vadim.quantum.domain.operation.WhileLoopOperation;

/**
 * Generic preflight-проверка IR против target capability profile.
 */
public final class CapabilityPreflightChecker {

    public CapabilityPreflightResult check(
        final QuantumProgram program,
        final IntegrationCapabilityProfile profile
    ) {
        if (program == null) {
            throw new IllegalArgumentException("Quantum program must not be null.");
        }
        if (profile == null) {
            throw new IllegalArgumentException("Integration capability profile must not be null.");
        }
        final ArrayList<IntegrationDiagnostic> diagnostics = new ArrayList<>();
        for (int i = 0; i < program.circuitCount(); i++) {
            final QuantumCircuit circuit = program.circuit(i);
            checkRegisters(
                circuit,
                profile,
                diagnostics
            );
            for (int j = 0; j < circuit.operationCount(); j++) {
                checkOperation(
                    circuit.operation(j),
                    profile,
                    diagnostics
                );
            }
        }
        return CapabilityPreflightResult.of(diagnostics);
    }

    private static void checkRegisters(
        final QuantumCircuit circuit,
        final IntegrationCapabilityProfile profile,
        final ArrayList<IntegrationDiagnostic> diagnostics
    ) {
        if (
            circuit.quantumRegisterCount() > 0
            && !profile.supports(IntegrationCapability.QUANTUM_REGISTERS)
        ) {
            diagnostics.add(unsupportedCapability("quantum registers"));
        }
        if (
            circuit.classicalRegisterCount() > 0
            && !profile.supports(IntegrationCapability.CLASSICAL_REGISTERS)
        ) {
            diagnostics.add(unsupportedCapability("classical registers"));
        }
    }

    private static void checkOperation(
        final Operation operation,
        final IntegrationCapabilityProfile profile,
        final ArrayList<IntegrationDiagnostic> diagnostics
    ) {
        checkDynamicQubitReferences(
            operation,
            profile,
            diagnostics
        );
        if (
            operation instanceof GateOperation
        ) {
            return;
        }
        if (
            operation instanceof MeasureOperation
            && !profile.supports(IntegrationCapability.MEASUREMENTS)
        ) {
            diagnostics.add(unsupportedCapability("measurements"));
        } else if (
            operation instanceof ResetOperation
            && !profile.supports(IntegrationCapability.RESET)
        ) {
            diagnostics.add(unsupportedCapability("reset operations"));
        } else if (
            operation instanceof BarrierOperation
            && !profile.supports(IntegrationCapability.BARRIER)
        ) {
            diagnostics.add(unsupportedCapability("barrier operations"));
        } else if (operation instanceof ControlledOperation controlledOperation) {
            checkControlledOperation(
                controlledOperation.operation(),
                profile,
                diagnostics
            );
        } else if (operation instanceof ClassicallyControlledOperation controlledOperation) {
            checkPredicate(
                controlledOperation.predicate(),
                profile,
                diagnostics
            );
            checkControlledOperation(
                controlledOperation.operation(),
                profile,
                diagnostics
            );
        } else if (operation instanceof ClassicalAssignmentOperation assignmentOperation) {
            if (!profile.supports(IntegrationCapability.CLASSICAL_ASSIGNMENTS)) {
                diagnostics.add(unsupportedCapability("classical assignments"));
            }
            checkExpression(
                assignmentOperation.assignment().target(),
                profile,
                diagnostics
            );
            checkExpression(
                assignmentOperation.assignment().value(),
                profile,
                diagnostics
            );
        } else if (operation instanceof BlockOperation blockOperation) {
            checkStructuredOperation(
                blockOperation.body(),
                profile,
                diagnostics
            );
        } else if (operation instanceof ConditionalBlockOperation conditionalOperation) {
            if (!profile.supports(IntegrationCapability.STRUCTURED_CONTROL_FLOW)) {
                diagnostics.add(unsupportedCapability("structured conditional blocks"));
            }
            checkPredicate(
                conditionalOperation.predicate(),
                profile,
                diagnostics
            );
            checkBlock(
                conditionalOperation.thenBlock(),
                profile,
                diagnostics
            );
            if (conditionalOperation.hasElseBlock()) {
                checkBlock(
                    conditionalOperation.elseBlock(),
                    profile,
                    diagnostics
                );
            }
        } else if (operation instanceof ForLoopOperation loopOperation) {
            checkStructuredOperation(
                loopOperation.body(),
                profile,
                diagnostics
            );
        } else if (operation instanceof WhileLoopOperation loopOperation) {
            checkPredicate(
                loopOperation.predicate(),
                profile,
                diagnostics
            );
            checkStructuredOperation(
                loopOperation.body(),
                profile,
                diagnostics
            );
        } else if (operation instanceof DelayOperation) {
            if (!profile.supports(IntegrationCapability.TIMING_OPERATIONS)) {
                diagnostics.add(unsupportedCapability("timing operations"));
            }
        } else if (operation instanceof TimingBoxOperation boxOperation) {
            if (!profile.supports(IntegrationCapability.TIMING_OPERATIONS)) {
                diagnostics.add(unsupportedCapability("timing operations"));
            }
            checkBlock(
                boxOperation.body(),
                profile,
                diagnostics
            );
        } else if (
            operation instanceof LabelOperation
            || operation instanceof BranchOperation
            || operation instanceof HaltOperation
            || operation instanceof WaitOperation
        ) {
            if (!profile.supports(IntegrationCapability.INSTRUCTION_CONTROL_FLOW)) {
                diagnostics.add(unsupportedCapability("instruction-level control flow"));
            }
        }
    }

    private static void checkDynamicQubitReferences(
        final Operation operation,
        final IntegrationCapabilityProfile profile,
        final ArrayList<IntegrationDiagnostic> diagnostics
    ) {
        if (profile.supports(IntegrationCapability.DYNAMIC_QUBIT_REFERENCES)) {
            return;
        }
        if (operation instanceof GateOperation gateOperation) {
            for (int i = 0; i < gateOperation.qubitCount(); i++) {
                checkDynamicQubitReference(
                    gateOperation.qubitReference(i),
                    profile,
                    diagnostics
                );
            }
        } else if (operation instanceof MeasureOperation measureOperation) {
            checkDynamicQubitReference(
                measureOperation.qubitReference(),
                profile,
                diagnostics
            );
        } else if (operation instanceof ResetOperation resetOperation) {
            checkDynamicQubitReference(
                resetOperation.qubitReference(),
                profile,
                diagnostics
            );
        }
    }

    private static void checkDynamicQubitReference(
        final QuantumReference reference,
        final IntegrationCapabilityProfile profile,
        final ArrayList<IntegrationDiagnostic> diagnostics
    ) {
        if (reference.kind() == QuantumReferenceKind.DYNAMIC_REGISTER_INDEX) {
            diagnostics.add(unsupportedCapability("dynamic qubit references"));
            checkExpression(
                reference.indexExpression(),
                profile,
                diagnostics
            );
        }
    }

    private static void checkPredicate(
        final ClassicalPredicate predicate,
        final IntegrationCapabilityProfile profile,
        final ArrayList<IntegrationDiagnostic> diagnostics
    ) {
        if (predicate.kind() == ClassicalPredicateKind.COMPARISON) {
            checkExpression(
                predicate.leftExpression(),
                profile,
                diagnostics
            );
            checkExpression(
                predicate.rightExpression(),
                profile,
                diagnostics
            );
        } else if (predicate.kind() == ClassicalPredicateKind.NOT) {
            checkPredicate(
                predicate.leftPredicate(),
                profile,
                diagnostics
            );
        } else if (predicate.kind() == ClassicalPredicateKind.BOOLEAN) {
            checkPredicate(
                predicate.leftPredicate(),
                profile,
                diagnostics
            );
            checkPredicate(
                predicate.rightPredicate(),
                profile,
                diagnostics
            );
        }
    }

    private static void checkExpression(
        final ClassicalExpression expression,
        final IntegrationCapabilityProfile profile,
        final ArrayList<IntegrationDiagnostic> diagnostics
    ) {
        if (
            expression.kind() == ClassicalExpressionKind.SYMBOLIC_REFERENCE
            || expression.kind() == ClassicalExpressionKind.CALL
        ) {
            if (!profile.supports(IntegrationCapability.CLASSICAL_EXTENDED_EXPRESSIONS)) {
                diagnostics.add(unsupportedCapability("extended classical expressions"));
            }
        }
        if (expression.kind() == ClassicalExpressionKind.BINARY_OPERATION) {
            checkExpression(
                expression.leftExpression(),
                profile,
                diagnostics
            );
            checkExpression(
                expression.rightExpression(),
                profile,
                diagnostics
            );
        } else if (expression.kind() == ClassicalExpressionKind.CALL) {
            for (int i = 0; i < expression.callArgumentCount(); i++) {
                checkExpression(
                    expression.callArgument(i),
                    profile,
                    diagnostics
                );
            }
        }
    }

    private static void checkStructuredOperation(
        final OperationBlock body,
        final IntegrationCapabilityProfile profile,
        final ArrayList<IntegrationDiagnostic> diagnostics
    ) {
        if (!profile.supports(IntegrationCapability.STRUCTURED_CONTROL_FLOW)) {
            diagnostics.add(unsupportedCapability("structured control flow"));
        }
        checkBlock(
            body,
            profile,
            diagnostics
        );
    }

    private static void checkBlock(
        final OperationBlock body,
        final IntegrationCapabilityProfile profile,
        final ArrayList<IntegrationDiagnostic> diagnostics
    ) {
        for (int i = 0; i < body.operationCount(); i++) {
            checkOperation(
                body.operation(i),
                profile,
                diagnostics
            );
        }
    }

    private static void checkControlledOperation(
        final Operation operation,
        final IntegrationCapabilityProfile profile,
        final ArrayList<IntegrationDiagnostic> diagnostics
    ) {
        if (!profile.supports(IntegrationCapability.CLASSICAL_REGISTER_CONDITIONS)) {
            diagnostics.add(unsupportedCapability("classical register conditions"));
        }
        checkOperation(
            operation,
            profile,
            diagnostics
        );
    }

    private static IntegrationDiagnostic unsupportedCapability(final String featureName) {
        return IntegrationDiagnostic.error(
            IntegrationDiagnosticCode.UNSUPPORTED_TARGET_CAPABILITY,
            "Target capability profile does not support " + featureName + "."
        );
    }
}
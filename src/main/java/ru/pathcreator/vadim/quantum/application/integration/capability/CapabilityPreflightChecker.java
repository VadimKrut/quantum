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
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.BarrierOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicallyControlledOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ControlledOperation;
import ru.pathcreator.vadim.quantum.domain.operation.MeasureOperation;
import ru.pathcreator.vadim.quantum.domain.operation.Operation;
import ru.pathcreator.vadim.quantum.domain.operation.ResetOperation;

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
            checkControlledOperation(
                controlledOperation.operation(),
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
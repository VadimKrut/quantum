/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.integration.capability;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnosticCode;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.QuantumReference;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapabilityPreflightCheckerTest {

    @Test
    void passesWhenProfileSupportsRequiredFeatures() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("main");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.reset(q.get(0));

        final CapabilityPreflightResult result = new CapabilityPreflightChecker().check(
            program,
            IntegrationCapabilityProfile.of(
                IntegrationFormat.OPENQASM_2,
                EnumSet.of(
                    IntegrationCapability.QUANTUM_REGISTERS,
                    IntegrationCapability.RESET
                )
            )
        );

        assertTrue(result.isSuccess());
        assertEquals(
            CapabilityPreflightStatus.EXPORTABLE,
            result.status()
        );
    }

    @Test
    void reportsMissingTargetCapability() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("main");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.reset(q.get(0));

        final CapabilityPreflightResult result = new CapabilityPreflightChecker().check(
            program,
            IntegrationCapabilityProfile.empty(IntegrationFormat.OPENQASM_2)
        );

        assertFalse(result.isSuccess());
        assertEquals(
            CapabilityPreflightStatus.UNSUPPORTED_BY_TARGET,
            result.status()
        );
        assertEquals(
            IntegrationDiagnosticCode.UNSUPPORTED_TARGET_CAPABILITY,
            result.diagnostics().get(0).code()
        );
    }

    @Test
    void reportsDynamicQubitReferencesWhenTargetDoesNotSupportThem() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("main");
        final QuantumRegister buffer = circuit.createQuantumRegister(
            "buffer",
            4
        );
        circuit.gateReferences(
            StandardGate.H,
            QuantumReference.dynamicIndex(
                buffer,
                ClassicalExpression.variable("address")
            )
        );

        final CapabilityPreflightResult result = new CapabilityPreflightChecker().check(
            program,
            IntegrationCapabilityProfile.of(
                IntegrationFormat.QUIL,
                EnumSet.of(IntegrationCapability.QUANTUM_REGISTERS)
            )
        );

        assertFalse(result.isSuccess());
        assertEquals(
            CapabilityPreflightStatus.UNSUPPORTED_BY_TARGET,
            result.status()
        );
        assertEquals(
            IntegrationDiagnosticCode.UNSUPPORTED_TARGET_CAPABILITY,
            result.diagnostics().get(0).code()
        );
        assertTrue(result.diagnostics().get(0).message().contains("dynamic qubit references"));
    }

    @Test
    void reportsSemanticLossForMissingCallableSupport() {
        final QuantumProgram program = QuantumProgram.gateBased();
        program.addExternalCallableDeclaration(new ru.pathcreator.vadim.quantum.domain.callable.ExternalCallableDeclaration(
            "external_job",
            null
        ));

        final CapabilityPreflightResult result = new CapabilityPreflightChecker().check(
            program,
            IntegrationCapabilityProfile.empty(IntegrationFormat.OPENQASM_2)
        );

        assertFalse(result.isSuccess());
        assertEquals(
            CapabilityPreflightStatus.UNSUPPORTED_WITHOUT_LOSS,
            result.status()
        );
    }
}
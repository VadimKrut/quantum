/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.quil.adapter;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.application.integration.contract.QuantumIntegration;
import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnosticCode;
import ru.pathcreator.vadim.quantum.application.integration.result.ExportResult;
import ru.pathcreator.vadim.quantum.application.integration.result.ImportResult;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.MeasureOperation;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinitionKind;
import ru.pathcreator.vadim.quantum.domain.operation.QuantumReference;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuilIntegrationTest {

    @Test
    void exportsBellCircuitToQuil() {
        final ExportResult result = new QuilIntegration().exportProgram(createBellProgram());

        assertTrue(result.isSuccess());
        assertEquals(
            """
            DECLARE ro BIT[2]
            H 0
            CNOT 0 1
            MEASURE 0 ro[0]
            MEASURE 1 ro[1]
            """,
            result.content()
        );
    }

    @Test
    void importsBellCircuitFromQuil() {
        final ImportResult result = new QuilIntegration().importProgram("""
            DECLARE ro BIT[2]
            H 0
            CNOT 0 1
            MEASURE 0 ro[0]
            MEASURE 1 ro[1]
            """);

        assertTrue(result.isSuccess());
        assertEquals(
            4,
            result.program().circuit(0).operationCount()
        );
        final GateOperation firstOperation = assertInstanceOf(
            GateOperation.class,
            result.program().circuit(0).operation(0)
        );
        assertSame(
            StandardGate.H,
            firstOperation.gate()
        );
        assertInstanceOf(
            MeasureOperation.class,
            result.program().circuit(0).operation(2)
        );
    }

    @Test
    void roundTripsBellCircuitThroughQuil() {
        final QuantumIntegration integration = new QuilIntegration();
        final ExportResult exported = integration.exportProgram(createBellProgram());
        final ImportResult imported = integration.importProgram(exported.content());

        assertTrue(exported.isSuccess());
        assertTrue(imported.isSuccess());
        assertEquals(
            4,
            imported.program().circuit(0).operationCount()
        );
    }

    @Test
    void importsStandardGateAliasesWithoutCreatingConflictingExternalDefinitions() {
        final ImportResult result = new QuilIntegration().importProgram("""
            U(0,0,0) 0
            U3(0,0,0) 0
            P(0) 0
            SDG 0
            TDG 0
            CY 0 1
            CH 0 1
            """);

        assertTrue(result.isSuccess());
        assertEquals(
            0,
            result.program().gateDefinitionCount()
        );
        assertEquals(
            7,
            result.program().circuit(0).operationCount()
        );
    }

    @Test
    void plansSparseExternalGateQubitIndexesBeforeParsingOperations() {
        final ImportResult result = new QuilIntegration().importProgram("""
            ctu 3 4
            """);

        assertTrue(result.isSuccess());
        assertEquals(
            1,
            result.program().gateDefinitionCount()
        );
        assertEquals(
            5,
            result.program().circuit(0).quantumRegister(0).size()
        );
    }

    @Test
    void importsMatrixGateDefinitionAsStructuralIr() {
        final ImportResult result = new QuilIntegration().importProgram("""
            DECLARE ro BIT[1]
            DEFGATE CUSTOM:
                1, 0
                0, 1
            """);

        assertTrue(result.isSuccess());
        assertEquals(
            1,
            result.program().gateDefinitionCount()
        );
        assertEquals(
            GateDefinitionKind.MATRIX,
            result.program().gateDefinition(0).kind()
        );
        assertEquals(
            0,
            result.program().circuit(0).operationCount()
        );
        final ExportResult exported = new QuilIntegration().exportProgram(result.program());

        assertTrue(exported.isSuccess());
        assertTrue(exported.content().contains("DEFGATE CUSTOM:"));
    }

    @Test
    void importsCalibrationBlocksAsCalibrationDefinitions() {
        final ImportResult result = new QuilIntegration().importProgram("""
            DEFCAL RX(pi/2) 0:
                FENCE 0
                NONBLOCKING PULSE 0 "rf" drag_gaussian(duration: 1e-08)
                FENCE 0
            """);

        assertTrue(result.isSuccess());
        assertEquals(
            1,
            result.program().calibrationDefinitionCount()
        );
        assertEquals(
            "quil",
            result.program().calibrationDefinition(0).bodyLanguage()
        );

        final ExportResult exported = new QuilIntegration().exportProgram(result.program());

        assertTrue(exported.isSuccess());
        assertTrue(exported.content().contains("DEFCAL RX(pi/2) 0:"));
        assertTrue(exported.content().contains("NONBLOCKING PULSE"));
    }

    @Test
    void rejectsDynamicQubitReferencesDuringExport() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("dynamic");
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

        final ExportResult result = new QuilIntegration().exportProgram(program);

        assertFalse(result.isSuccess());
        assertEquals(
            IntegrationDiagnosticCode.UNSUPPORTED_TARGET_CAPABILITY,
            result.diagnostics().get(0).code()
        );
        assertTrue(result.diagnostics().get(0).message().contains("dynamic qubit references"));
    }

    private static QuantumProgram createBellProgram() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("bell");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            2
        );
        final ClassicalRegister ro = circuit.createClassicalRegister(
            "ro",
            2
        );
        circuit.gate(
            StandardGate.H,
            q.get(0)
        );
        circuit.gate(
            StandardGate.CX,
            q.get(0),
            q.get(1)
        );
        circuit.measure(
            q.get(0),
            ro.get(0)
        );
        circuit.measure(
            q.get(1),
            ro.get(1)
        );
        return program;
    }
}
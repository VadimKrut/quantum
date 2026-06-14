/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.openqasm3.adapter;

import java.util.Map;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.api.QuantumIntegrations;
import ru.pathcreator.vadim.quantum.application.integration.contract.QuantumIntegration;
import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnosticCode;
import ru.pathcreator.vadim.quantum.application.integration.options.ImportOptions;
import ru.pathcreator.vadim.quantum.application.integration.result.ExportResult;
import ru.pathcreator.vadim.quantum.application.integration.result.ImportResult;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalAssignment;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalComparisonOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinition;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalArrayDeclarationOperation;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ForLoopOperation;
import ru.pathcreator.vadim.quantum.domain.operation.MeasureOperation;
import ru.pathcreator.vadim.quantum.domain.operation.OperationBlock;
import ru.pathcreator.vadim.quantum.domain.operation.QuantumReferenceKind;
import ru.pathcreator.vadim.quantum.domain.operation.ResetOperation;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;
import ru.pathcreator.vadim.quantum.domain.timing.DurationExpression;
import ru.pathcreator.vadim.quantum.domain.timing.DurationUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenQasm3IntegrationTest {

    @Test
    void exportsBellCircuitToOpenQasm3() {
        final ExportResult result = QuantumIntegrations.openQasm3().exportProgram(createBellProgram());

        assertTrue(result.isSuccess());
        assertEquals(
            """
            OPENQASM 3.0;
            include "stdgates.inc";
            qubit[2] q;
            bit[2] c;
            h q[0];
            cx q[0],q[1];
            measure q[0] -> c[0];
            measure q[1] -> c[1];
            """,
            result.content()
        );
    }

    @Test
    void importsBellCircuitFromOpenQasm3() {
        final ImportResult result = QuantumIntegrations.openQasm3().importProgram("""
            OPENQASM 3.0;
            include "stdgates.inc";
            qubit[2] q;
            bit[2] c;
            h q[0];
            cx q[0], q[1];
            c[0] = measure q[0];
            c[1] = measure q[1];
            """);

        assertTrue(result.isSuccess());
        assertEquals(
            1,
            result.program().circuitCount()
        );
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
    void roundTripsBellCircuitThroughOpenQasm3() {
        final QuantumIntegration integration = QuantumIntegrations.openQasm3();
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
    void importsCalibrationBlocksAsCalibrationDefinitions() {
        final ImportResult result = QuantumIntegrations.openQasm3().importProgram("""
            OPENQASM 3.0;
            defcalgrammar "openpulse";
            defcal x $0 {
              play drive($0), gaussian(...);
            }
            """);

        assertTrue(result.isSuccess());
        assertEquals(
            2,
            result.program().calibrationDefinitionCount()
        );
        assertEquals(
            "openqasm3",
            result.program().calibrationDefinition(0).bodyLanguage()
        );

        final ExportResult exported = QuantumIntegrations.openQasm3().exportProgram(result.program());

        assertTrue(exported.isSuccess());
        assertTrue(exported.content().contains("defcalgrammar \"openpulse\";"));
        assertTrue(exported.content().contains("defcal x $0"));
    }

    @Test
    void importsScalarQubitAndBitDeclarations() {
        final ImportResult result = QuantumIntegrations.openQasm3().importProgram("""
            OPENQASM 3.0;
            include "stdgates.inc";
            qubit q;
            bit c;
            c = measure q;
            """);

        assertTrue(result.isSuccess());
        assertEquals(
            1,
            result.program().circuit(0).quantumRegister(0).size()
        );
        assertEquals(
            1,
            result.program().circuit(0).classicalRegister(0).size()
        );
    }

    @Test
    void importsCustomIncludeSource() {
        final ImportResult result = QuantumIntegrations.openQasm3().importProgram(
            """
            OPENQASM 3.0;
            include "custom.inc";
            qubit[1] q;
            local_h q[0];
            """,
            ImportOptions.of(
                true,
                false,
                Map.of(
                    "custom.inc",
                    """
                    gate local_h a {
                      h a;
                    }
                    """
                )
            )
        );

        assertTrue(result.isSuccess());
        assertEquals(
            1,
            result.program().gateDefinitionCount()
        );
        final GateOperation operation = assertInstanceOf(
            GateOperation.class,
            result.program().circuit(0).operation(0)
        );
        assertEquals(
            "local_h",
            operation.gate().gateName()
        );
    }

    @Test
    void importsAndExportsDynamicSubroutineQubitIndexes() {
        final QuantumIntegration integration = QuantumIntegrations.openQasm3();
        final ImportResult imported = integration.importProgram("""
            OPENQASM 3.0;
            include "stdgates.inc";
            const int[32] size = 4;
            def apply(int[32] addr, qubit target, qubit[size] buffer) {
              cy buffer[addr], target;
            }
            qubit[1] q;
            qubit[size] buffer;
            int[32] address;
            apply(address, q[0], buffer);
            address += 1;
            """);

        assertTrue(imported.isSuccess());
        final GateOperation operation = assertInstanceOf(
            GateOperation.class,
            imported.program().circuit(0).operation(0)
        );
        assertEquals(
            QuantumReferenceKind.DYNAMIC_REGISTER_INDEX,
            operation.qubitReference(0).kind()
        );

        final ExportResult exported = integration.exportProgram(imported.program());

        assertTrue(exported.isSuccess());
        assertTrue(exported.content().contains("cy buffer[address],q[0];"));
        assertTrue(exported.content().contains("address = (address + 1);"));
    }

    @Test
    void exportsStructuredAndTimingOperationsToOpenQasm3() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("structured");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            2
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            2
        );
        final ClassicalPredicate predicate = ClassicalPredicate.compare(
            ClassicalExpression.bit(c.get(0)),
            ClassicalComparisonOperator.EQUAL,
            ClassicalExpression.integer(1)
        );

        circuit.assign(new ClassicalAssignment(
            ClassicalExpression.bit(c.get(1)),
            ClassicalExpression.integer(0)
        ));
        circuit.conditionalBlock(
            predicate,
            OperationBlock.of(GateOperation.of(
                StandardGate.X,
                q.get(0)
            )),
            OperationBlock.of(new ResetOperation(q.get(1)))
        );
        circuit.forLoop(
            "i",
            0,
            1,
            2,
            OperationBlock.of(GateOperation.of(
                StandardGate.H,
                q.get(0)
            ))
        );
        circuit.whileLoop(
            ClassicalPredicate.compare(
                ClassicalExpression.bit(c.get(1)),
                ClassicalComparisonOperator.NOT_EQUAL,
                ClassicalExpression.integer(1)
            ),
            OperationBlock.of(GateOperation.of(
                StandardGate.Z,
                q.get(1)
            ))
        );
        circuit.delay(
            DurationExpression.duration(
                20,
                DurationUnit.NS
            ),
            q.get(0),
            q.get(1)
        );
        circuit.timingBox(
            DurationExpression.stretch("stretch_t"),
            OperationBlock.of(GateOperation.of(
                StandardGate.S,
                q.get(0)
            ))
        );

        final ExportResult result = QuantumIntegrations.openQasm3().exportProgram(program);

        assertTrue(result.isSuccess());
        assertEquals(
            """
            OPENQASM 3.0;
            include "stdgates.inc";
            qubit[2] q;
            bit[2] c;
            c[1] = 0;
            if (c[0] == 1) {
              x q[0];
            } else {
              reset q[1];
            }
            for i in [0:1:2] {
              h q[0];
            }
            while (c[1] != 1) {
              z q[1];
            }
            delay[20ns] q[0],q[1];
            box[stretch_t] {
              s q[0];
            }
            """,
            result.content()
        );
    }

    @Test
    void rejectsOpenQasm2RegisterSyntaxDuringImport() {
        final ImportResult result = QuantumIntegrations.openQasm3().importProgram("""
            OPENQASM 2.0;
            qreg q[1];
            """);

        assertFalse(result.isSuccess());
        assertEquals(
            IntegrationDiagnosticCode.PARSE_ERROR,
            result.diagnostic(0).code()
        );
    }

    @Test
    void importsStructuredAndTimingStatements() {
        final ImportResult result = QuantumIntegrations.openQasm3().importProgram("""
            OPENQASM 3.0;
            include "stdgates.inc";
            qubit[2] q;
            bit[2] c;
            if (c[0] == 1) {
              x q[0];
            }
            for i in [0:1:2] {
              h q[0];
            }
            while (c[1] != 1) {
              z q[1];
            }
            delay[20ns] q[0], q[1];
            box[stretch_t] {
              s q[0];
            }
            """);

        assertTrue(result.isSuccess());
        assertEquals(
            5,
            result.program().circuit(0).operationCount()
        );
        assertInstanceOf(
            ForLoopOperation.class,
            result.program().circuit(0).operation(1)
        );
    }

    @Test
    void lowersGateBasedSubroutinesLetsAndStaticArrays() {
        final ImportResult result = QuantumIntegrations.openQasm3().importProgram("""
            OPENQASM 3.0;
            include "stdgates.inc";
            const int[32] n = 2;
            array[uint[8], 3] flags = {1, 0, 1};
            def prepare(qubit[2] pair) {
              reset pair;
              h pair[0];
              cx pair[0], pair[1];
            }
            qubit[4] q;
            bit[2] c;
            let first = q[{0, 1}];
            prepare first;
            for uint i in [0:n] {
              if (bool(flags[i])) x q[i];
            }
            measure first -> c;
            """);

        assertTrue(result.isSuccess());
        assertEquals(
            8,
            result.program().circuit(0).operationCount()
        );
        assertInstanceOf(
            ClassicalArrayDeclarationOperation.class,
            result.program().circuit(0).operation(0)
        );
        assertInstanceOf(
            ResetOperation.class,
            result.program().circuit(0).operation(1)
        );
        assertInstanceOf(
            GateOperation.class,
            result.program().circuit(0).operation(3)
        );
        assertInstanceOf(
            MeasureOperation.class,
            result.program().circuit(0).operation(7)
        );
    }

    @Test
    void lowersReturningBitSubroutineIntoGateBasedOperations() {
        final ImportResult result = QuantumIntegrations.openQasm3().importProgram("""
            OPENQASM 3.0;
            include "stdgates.inc";
            qubit[2] q;
            qubit[1] a;
            bit[1] syn;
            def syndrome(qubit[2] d, qubit anc) -> bit {
              bit b;
              cx d[0], anc;
              cx d[1], anc;
              return measure anc;
            }
            syn = syndrome(q, a[0]);
            if (syn == 1) x q[0];
            """);

        assertTrue(result.isSuccess());
        assertEquals(
            4,
            result.program().circuit(0).operationCount()
        );
        assertInstanceOf(
            MeasureOperation.class,
            result.program().circuit(0).operation(2)
        );
    }

    @Test
    void importsConstantParameterFunctionsAndBitstringRegisterInitializers() {
        final ImportResult result = QuantumIntegrations.openQasm3().importProgram("""
            OPENQASM 3.0;
            include "stdgates.inc";
            qubit q;
            bit[2] flags = "11";
            rz(pi - arccos(3 / 5)) q;
            """);

        assertTrue(result.isSuccess());
        assertEquals(
            2,
            result.program().circuit(0).operationCount()
        );
        final GateOperation operation = assertInstanceOf(
            GateOperation.class,
            result.program().circuit(0).operation(1)
        );
        assertSame(
            StandardGate.RZ,
            operation.gate()
        );
    }

    @Test
    void lowersMixedSubroutineCallsWithCompileTimeIntegerArguments() {
        final ImportResult result = QuantumIntegrations.openQasm3().importProgram("""
            OPENQASM 3.0;
            include "stdgates.inc";
            const int[32] buffer_size = 2;
            def ymeasure(qubit q) -> bit {
              s q;
              h q;
              return measure q;
            }
            def Ty(int[32] addr, qubit q, qubit[buffer_size] buffer) {
              bit outcome;
              cy buffer[addr], q;
              outcome = ymeasure(buffer[addr]);
              if(outcome == 1) ry(pi / 2) q;
            }
            qubit[2] buffer;
            qubit[2] q;
            int[32] address = 0;
            Ty(address) q[0], buffer;
            address += 1;
            Ty(address) q[1], buffer;
            """);

        assertTrue(result.isSuccess());
        assertEquals(
            10,
            result.program().circuit(0).operationCount()
        );
        final GateOperation firstConsume = assertInstanceOf(
            GateOperation.class,
            result.program().circuit(0).operation(0)
        );
        final GateOperation secondConsume = assertInstanceOf(
            GateOperation.class,
            result.program().circuit(0).operation(5)
        );
        assertSame(
            StandardGate.CY,
            firstConsume.gate()
        );
        assertEquals(
            0,
            firstConsume.qubit(0).index()
        );
        assertEquals(
            1,
            secondConsume.qubit(0).index()
        );
    }

    @Test
    void rejectsOpaqueGateExportWithoutBodyOrDecomposition() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final GateDefinition opaque = GateDefinition.opaque(
            "external_gate",
            java.util.List.of("theta"),
            java.util.List.of("q")
        );
        program.addGateDefinition(opaque);
        final QuantumCircuit circuit = program.createCircuit("opaque");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.parameterizedGate(
            opaque,
            new ParameterExpression[] {ParameterExpression.of(0.5)},
            q.get(0)
        );

        final ExportResult result = QuantumIntegrations.openQasm3().exportProgram(program);

        assertFalse(result.isSuccess());
        assertEquals(
            IntegrationDiagnosticCode.UNSUPPORTED_GATE,
            result.diagnostic(0).code()
        );
    }

    private static QuantumProgram createBellProgram() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("bell");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            2
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            2
        );

        circuit.h(q.get(0))
            .cx(
                q.get(0),
                q.get(1)
            )
            .measure(
                q.get(0),
                c.get(0)
            )
            .measure(
                q.get(1),
                c.get(1)
        );
        return program;
    }

}
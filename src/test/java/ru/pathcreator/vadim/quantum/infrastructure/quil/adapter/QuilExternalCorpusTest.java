/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.quil.adapter;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.api.QuantumIntegrations;
import ru.pathcreator.vadim.quantum.api.QuantumIrFiles;
import ru.pathcreator.vadim.quantum.application.integration.contract.QuantumIntegration;
import ru.pathcreator.vadim.quantum.application.integration.result.ExportResult;
import ru.pathcreator.vadim.quantum.application.integration.result.ImportResult;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrReadResult;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrWriteResult;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuilExternalCorpusTest {

    @Test
    void importsRealExternalQuilCorpusThroughJsonAndTextAdapters() {
        final List<ExternalQuilCase> cases = List.of(
            qftEightFromPyquilReferenceWavefunction(),
            qaoaSquareFromPyquilReferenceWavefunction(),
            inverseQftThreeFromGrove(),
            qaoaP2BarbellFromGrove(),
            jordanGradientFromGrove()
        );
        final QuantumIntegration quil = QuantumIntegrations.quil();

        for (final ExternalQuilCase testCase : cases) {
            final ImportResult imported = quil.importProgram(testCase.content());

            assertTrue(
                imported.isSuccess(),
                testCase.name() + " Quil import must succeed: " + diagnostics(imported)
            );
            assertEquals(
                testCase.operationCount(),
                operationCount(imported.program()),
                testCase.name() + " operation count must be preserved on import"
            );

            final QuantumIrWriteResult json = QuantumIrFiles.writeToString(imported.program());

            assertTrue(
                json.isSuccess(),
                testCase.name() + " must write to native JSON"
            );
            final QuantumIrReadResult fromJson = QuantumIrFiles.readFromString(json.content());

            assertTrue(
                fromJson.isSuccess(),
                testCase.name() + " must read back from native JSON"
            );
            assertEquals(
                testCase.operationCount(),
                operationCount(fromJson.program()),
                testCase.name() + " operation count must survive JSON"
            );

            verifyExportImport(
                testCase,
                fromJson.program(),
                QuantumIntegrations.openQasm2()
            );
            verifyExportImport(
                testCase,
                fromJson.program(),
                QuantumIntegrations.openQasm3()
            );
            verifyExportImport(
                testCase,
                fromJson.program(),
                quil
            );
        }
    }

    private static void verifyExportImport(
        final ExternalQuilCase testCase,
        final QuantumProgram program,
        final QuantumIntegration integration
    ) {
        final ExportResult exported = integration.exportProgram(program);

        assertTrue(
            exported.isSuccess(),
            testCase.name() + " export to " + integration.format().displayName()
                + " must succeed: " + diagnostics(exported)
        );
        final ImportResult imported = integration.importProgram(exported.content());

        assertTrue(
            imported.isSuccess(),
            testCase.name() + " import from " + integration.format().displayName()
                + " must succeed: " + diagnostics(imported)
        );
        assertEquals(
            operationCount(program),
            operationCount(imported.program()),
            testCase.name() + " operation count must survive " + integration.format().displayName()
        );
    }

    private static ExternalQuilCase qftEightFromPyquilReferenceWavefunction() {
        return new ExternalQuilCase(
            "pyquil_reference_wavefunction_qft_8",
            """
            # Source: rigetti/pyquil test/unit/test_reference_wavefunction.py QFT_8_INSTRUCTIONS.
            H 7
            CPHASE(1.5707963267948966) 6 7
            H 6
            CPHASE(0.7853981633974483) 5 7
            CPHASE(1.5707963267948966) 5 6
            H 5
            CPHASE(0.39269908169872414) 4 7
            CPHASE(0.7853981633974483) 4 6
            CPHASE(1.5707963267948966) 4 5
            H 4
            CPHASE(0.19634954084936207) 3 7
            CPHASE(0.39269908169872414) 3 6
            CPHASE(0.7853981633974483) 3 5
            CPHASE(1.5707963267948966) 3 4
            H 3
            CPHASE(0.09817477042468103) 2 7
            CPHASE(0.19634954084936207) 2 6
            CPHASE(0.39269908169872414) 2 5
            CPHASE(0.7853981633974483) 2 4
            CPHASE(1.5707963267948966) 2 3
            H 2
            CPHASE(0.04908738521234052) 1 7
            CPHASE(0.09817477042468103) 1 6
            CPHASE(0.19634954084936207) 1 5
            CPHASE(0.39269908169872414) 1 4
            CPHASE(0.7853981633974483) 1 3
            CPHASE(1.5707963267948966) 1 2
            H 1
            CPHASE(0.02454369260617026) 0 7
            CPHASE(0.04908738521234052) 0 6
            CPHASE(0.09817477042468103) 0 5
            CPHASE(0.19634954084936207) 0 4
            CPHASE(0.39269908169872414) 0 3
            CPHASE(0.7853981633974483) 0 2
            CPHASE(1.5707963267948966) 0 1
            H 0
            SWAP 0 7
            SWAP 1 6
            SWAP 2 5
            SWAP 3 4
            """,
            40
        );
    }

    private static ExternalQuilCase qaoaSquareFromPyquilReferenceWavefunction() {
        return new ExternalQuilCase(
            "pyquil_reference_wavefunction_square_qaoa",
            """
            # Source: rigetti/pyquil test/unit/test_reference_wavefunction.py square_qaoa_circuit.
            H 0
            H 1
            H 2
            H 3
            X 0
            PHASE(0.3928244130249029) 0
            X 0
            PHASE(0.3928244130249029) 0
            CNOT 0 1
            RZ(0.78564882604980579) 1
            CNOT 0 1
            X 0
            PHASE(0.3928244130249029) 0
            X 0
            PHASE(0.3928244130249029) 0
            CNOT 0 3
            RZ(0.78564882604980579) 3
            CNOT 0 3
            X 0
            PHASE(0.3928244130249029) 0
            X 0
            PHASE(0.3928244130249029) 0
            CNOT 1 2
            RZ(0.78564882604980579) 2
            CNOT 1 2
            X 0
            PHASE(0.3928244130249029) 0
            X 0
            PHASE(0.3928244130249029) 0
            CNOT 2 3
            RZ(0.78564882604980579) 3
            CNOT 2 3
            H 0
            RZ(-0.77868204192240842) 0
            H 0
            H 1
            RZ(-0.77868204192240842) 1
            H 1
            H 2
            RZ(-0.77868204192240842) 2
            H 2
            H 3
            RZ(-0.77868204192240842) 3
            H 3
            """,
            44
        );
    }

    private static ExternalQuilCase inverseQftThreeFromGrove() {
        return new ExternalQuilCase(
            "grove_inverse_qft_three",
            """
            # Source: rigetti/grove grove/tests/qft/test_qft.py test_multi_qubit_qft.
            X 0
            X 1
            X 2
            SWAP 0 2
            H 0
            CPHASE(-1.5707963267948966) 0 1
            CPHASE(-0.7853981633974483) 0 2
            H 1
            CPHASE(-1.5707963267948966) 1 2
            H 2
            """,
            10
        );
    }

    private static ExternalQuilCase qaoaP2BarbellFromGrove() {
        return new ExternalQuilCase(
            "grove_qaoa_p2_barbell",
            """
            # Source: rigetti/grove grove/tests/pyqaoa/test_maxcut.py test_psiref_bar_p2 expected program.
            H 0
            H 1
            CNOT 0 1
            RZ(2.1) 1
            CNOT 0 1
            X 0
            PHASE(1.05) 0
            X 0
            PHASE(1.05) 0
            H 0
            RZ(-2.4) 0
            H 0
            H 1
            RZ(-2.4) 1
            H 1
            CNOT 0 1
            RZ(4.5) 1
            CNOT 0 1
            X 0
            PHASE(2.25) 0
            X 0
            PHASE(2.25) 0
            H 0
            RZ(-6.8) 0
            H 0
            H 1
            RZ(-6.8) 1
            H 1
            """,
            28
        );
    }

    private static ExternalQuilCase jordanGradientFromGrove() {
        return new ExternalQuilCase(
            "grove_jordan_gradient_raw_measure",
            """
            # Source: rigetti/grove grove/tests/jordan_gradient/test_jordan_gradient.py expected suffix.
            H 0
            H 1
            SWAP 0 1
            H 0
            CPHASE(-1.5707963267948966) 0 1
            H 1
            MEASURE 0 0
            MEASURE 1 1
            """,
            8
        );
    }

    private static int operationCount(final QuantumProgram program) {
        int count = 0;
        for (int i = 0; i < program.circuitCount(); i++) {
            count += program.circuit(i).operationCount();
        }
        return count;
    }

    private static String diagnostics(final ImportResult result) {
        final StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < result.diagnosticCount(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(result.diagnostic(i).code())
                .append(": ")
                .append(result.diagnostic(i).message());
        }
        return builder.append(']').toString();
    }

    private static String diagnostics(final ExportResult result) {
        final StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < result.diagnosticCount(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(result.diagnostic(i).code())
                .append(": ")
                .append(result.diagnostic(i).message());
        }
        return builder.append(']').toString();
    }

    private record ExternalQuilCase(
        String name,
        String content,
        int operationCount
    ) {
    }
}
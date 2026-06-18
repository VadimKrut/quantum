/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.library.integration;

import java.util.Map;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.api.Quantum;
import ru.pathcreator.vadim.quantum.application.simulation.options.SimulationOptions;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.library.api.QuantumAlgorithmLibrary;
import ru.pathcreator.vadim.quantum.library.domain.AlgorithmParameterSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuantumAlgorithmSimulationTest {

    private static final int SHOTS = 2048;

    @Test
    void bellStateProducesOnlyCorrelatedOutcomes() {
        final SimulationResult result = simulate(QuantumAlgorithmLibrary.generate("education.bell-state"));

        assertTrue(result.isSuccess());
        assertOnlyKeys(
            result.counts(),
            "00",
            "11"
        );
        assertBalancedPair(
            result.counts(),
            "00",
            "11"
        );
    }

    @Test
    void ghzStateProducesOnlyAllZeroOrAllOneOutcomes() {
        final QuantumProgram program = QuantumAlgorithmLibrary.generate(
            "education.ghz-state",
            AlgorithmParameterSet.builder()
                .integer("qubits", 5)
                .build()
        );
        final SimulationResult result = simulate(program);

        assertTrue(result.isSuccess());
        assertOnlyKeys(
            result.counts(),
            "00000",
            "11111"
        );
        assertBalancedPair(
            result.counts(),
            "00000",
            "11111"
        );
    }

    @Test
    void repetitionEncodingProbeIsDeterministic() {
        final SimulationResult result = simulate(
            QuantumAlgorithmLibrary.generate("education.repetition-encoding-probe")
        );

        assertDeterministic(
            result,
            "111"
        );
    }

    @Test
    void bernsteinVaziraniReturnsSecretMaskInMsbFirstOrder() {
        final QuantumProgram program = QuantumAlgorithmLibrary.generate(
            "oracle.bernstein-vazirani",
            AlgorithmParameterSet.builder()
                .integer("qubits", 4)
                .longInteger("secretMask", 11L)
                .build()
        );
        final SimulationResult result = simulate(program);

        assertDeterministic(
            result,
            "1011"
        );
    }

    @Test
    void twoQubitGroverAmplifiesMarkedState() {
        final SimulationResult result = simulate(
            QuantumAlgorithmLibrary.generate("search.two-qubit-grover")
        );

        assertDeterministic(
            result,
            "11"
        );
    }

    @Test
    void superdenseCodingDecodesConfiguredMessage() {
        final QuantumProgram program = QuantumAlgorithmLibrary.generate(
            "protocol.superdense-coding",
            AlgorithmParameterSet.builder()
                .integer("message", 3)
                .build()
        );
        final SimulationResult result = simulate(program);

        assertDeterministic(
            result,
            "11"
        );
    }

    @Test
    void parameterizedGroverAmplifiesConfiguredMarkedState() {
        final QuantumProgram program = QuantumAlgorithmLibrary.generate(
            "search.parameterized-two-qubit-grover",
            AlgorithmParameterSet.builder()
                .integer("markedState", 2)
                .build()
        );
        final SimulationResult result = simulate(program);

        assertDeterministic(
            result,
            "10"
        );
    }

    @Test
    void arithmeticHalfAdderReturnsInputsSumAndCarry() {
        final SimulationResult result = simulate(QuantumAlgorithmLibrary.generate("arithmetic.half-adder"));

        assertDeterministic(
            result,
            "1011"
        );
    }

    @Test
    void arithmeticParityCheckerReturnsInputAndParity() {
        final SimulationResult result = simulate(QuantumAlgorithmLibrary.generate("arithmetic.parity-checker"));

        assertDeterministic(
            result,
            "0101"
        );
    }

    @Test
    void bitFlipCodeProbeShowsConfiguredSingleFlip() {
        final QuantumProgram program = QuantumAlgorithmLibrary.generate(
            "error-correction.bit-flip-code-probe",
            AlgorithmParameterSet.builder()
                .integer("logicalBit", 1)
                .integer("flipIndex", 1)
                .build()
        );
        final SimulationResult result = simulate(program);

        assertDeterministic(
            result,
            "101"
        );
    }

    @Test
    void phaseFlipEncodingProbeReturnsLogicalCopiesAfterBasisUndo() {
        final SimulationResult result = simulate(
            QuantumAlgorithmLibrary.generate("error-correction.phase-flip-encoding-probe")
        );

        assertDeterministic(
            result,
            "111"
        );
    }

    @Test
    void resetAndIdentityProbeKeepsDeterministicState() {
        final SimulationResult result = simulate(
            QuantumAlgorithmLibrary.generate("diagnostic.reset-identity-probe")
        );

        assertDeterministic(
            result,
            "10"
        );
    }

    @Test
    void basisPreparationUsesMsbFirstClassicalOutput() {
        final SimulationResult result = simulate(
            QuantumAlgorithmLibrary.generate("transform.basis-preparation")
        );

        assertDeterministic(
            result,
            "1010"
        );
    }

    @Test
    void qftRoundTripRestoresBasisState() {
        final SimulationResult result = simulate(
            QuantumAlgorithmLibrary.generate("transform.qft-round-trip")
        );

        assertDeterministic(
            result,
            "1001"
        );
    }

    @Test
    void cliffordEchoRestoresPreparedBasisState() {
        final SimulationResult result = simulate(
            QuantumAlgorithmLibrary.generate("transform.clifford-echo")
        );

        assertDeterministic(
            result,
            "001"
        );
    }

    @Test
    void deutschSingleBitDistinguishesBalancedOracle() {
        final SimulationResult result = simulate(
            QuantumAlgorithmLibrary.generate("oracle.deutsch-single-bit")
        );

        assertDeterministic(
            result,
            "1"
        );
    }

    @Test
    void parityKickbackReturnsConfiguredMask() {
        final SimulationResult result = simulate(
            QuantumAlgorithmLibrary.generate("oracle.parity-kickback")
        );

        assertDeterministic(
            result,
            "101"
        );
    }

    @Test
    void basisStateTeleportationTransfersTargetBit() {
        final SimulationResult result = simulate(
            QuantumAlgorithmLibrary.generate("protocol.basis-state-teleportation")
        );

        assertDeterministic(
            result,
            "1"
        );
    }

    private static SimulationResult simulate(final QuantumProgram program) {
        return Quantum.simulate(
            program,
            SimulationOptions.builder()
                .shots(SHOTS)
                .seed(17L)
                .captureStateVector(true)
                .build()
        );
    }

    private static void assertOnlyKeys(
        final Map<String, Long> counts,
        final String first,
        final String second
    ) {
        assertEquals(
            2,
            counts.size()
        );
        assertTrue(counts.containsKey(first));
        assertTrue(counts.containsKey(second));
    }

    private static void assertBalancedPair(
        final Map<String, Long> counts,
        final String first,
        final String second
    ) {
        assertTrue(counts.get(first).longValue() > SHOTS * 35L / 100L);
        assertTrue(counts.get(second).longValue() > SHOTS * 35L / 100L);
    }

    private static void assertDeterministic(
        final SimulationResult result,
        final String expected
    ) {
        assertTrue(result.isSuccess());
        assertEquals(
            1,
            result.counts().size()
        );
        assertEquals(
            Long.valueOf(SHOTS),
            result.counts().get(expected)
        );
    }
}
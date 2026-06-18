/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.simulation.visualization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.application.simulation.result.StateVectorAmplitude;

/**
 * Строит reduced one-qubit states для phase-disk визуализации из state-vector.
 */
public final class PhaseDiskProjection {

    private static final double EPSILON = 1.0E-12;

    public List<SingleQubitPhaseDiskState> project(
        final SimulationResult simulation,
        final int maxQubitCount
    ) {
        if (simulation == null) {
            throw new IllegalArgumentException("Phase-disk simulation result must not be null.");
        }
        if (maxQubitCount < 0) {
            throw new IllegalArgumentException("Phase-disk qubit limit must not be negative.");
        }
        final int qubitLimit = Math.min(
            simulation.qubitCount(),
            maxQubitCount
        );
        final ArrayList<SingleQubitPhaseDiskState> states = new ArrayList<>(qubitLimit);
        for (int qubitIndex = 0; qubitIndex < qubitLimit; qubitIndex++) {
            states.add(reducedState(
                simulation.stateVector(),
                simulation.qubitCount(),
                qubitIndex
            ));
        }
        return List.copyOf(states);
    }

    private static SingleQubitPhaseDiskState reducedState(
        final List<StateVectorAmplitude> amplitudes,
        final int qubitCount,
        final int qubitIndex
    ) {
        final Map<String, StateVectorAmplitude> amplitudeByBasisState = amplitudeByBasisState(amplitudes);
        double p0 = 0.0;
        double p1 = 0.0;
        double coherenceReal = 0.0;
        double coherenceImaginary = 0.0;
        for (int i = 0; i < amplitudes.size(); i++) {
            final StateVectorAmplitude amplitude = amplitudes.get(i);
            final int bitPosition = qubitIndex;
            if (amplitude.basisState().charAt(bitPosition) == '0') {
                p0 += probability(amplitude);
                final StateVectorAmplitude other = amplitudeByBasisState.get(flipBit(
                    amplitude.basisState(),
                    bitPosition
                ));
                if (other != null) {
                    coherenceReal += amplitude.real() * other.real()
                        + amplitude.imaginary() * other.imaginary();
                    coherenceImaginary += amplitude.imaginary() * other.real()
                        - amplitude.real() * other.imaginary();
                }
            } else {
                p1 += probability(amplitude);
            }
        }
        final double x = 2.0 * coherenceReal;
        final double y = 2.0 * coherenceImaginary;
        final double z = p0 - p1;
        final double purity = Math.min(
            1.0,
            Math.sqrt(x * x + y * y + z * z)
        );
        final double phase = Math.atan2(
            y,
            Math.abs(x) < EPSILON && Math.abs(y) < EPSILON ? EPSILON : x
        );
        return new SingleQubitPhaseDiskState(
            qubitIndex,
            clamp(p1),
            phase,
            purity
        );
    }

    private static Map<String, StateVectorAmplitude> amplitudeByBasisState(
        final List<StateVectorAmplitude> amplitudes
    ) {
        final HashMap<String, StateVectorAmplitude> result = new HashMap<>();
        for (int i = 0; i < amplitudes.size(); i++) {
            result.put(
                amplitudes.get(i).basisState(),
                amplitudes.get(i)
            );
        }
        return result;
    }

    private static String flipBit(
        final String basisState,
        final int bitPosition
    ) {
        final char[] chars = basisState.toCharArray();
        chars[bitPosition] = chars[bitPosition] == '0' ? '1' : '0';
        return new String(chars);
    }

    private static double probability(final StateVectorAmplitude amplitude) {
        return amplitude.real() * amplitude.real()
            + amplitude.imaginary() * amplitude.imaginary();
    }

    private static double clamp(final double value) {
        return Math.max(
            0.0,
            Math.min(
                1.0,
                value
            )
        );
    }
}
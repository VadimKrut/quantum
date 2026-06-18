/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.qsphere;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.application.simulation.result.StateVectorAmplitude;

/**
 * Проецирует амплитуды state-vector на координаты q-sphere.
 */
public final class DesktopQSphereProjection {

    public List<DesktopQSpherePoint> project(final SimulationResult simulation) {
        if (simulation == null) {
            throw new IllegalArgumentException("Q-sphere simulation result must not be null.");
        }
        final int qubitCount = Math.max(
            1,
            simulation.qubitCount()
        );
        final HashMap<Integer, Integer> seenByWeight = new HashMap<>();
        final HashMap<Integer, Integer> totalByWeight = new HashMap<>();
        for (int i = 0; i < simulation.stateVector().size(); i++) {
            final int weight = hammingWeight(simulation.stateVector().get(i).basisState());
            totalByWeight.put(
                weight,
                totalByWeight.getOrDefault(
                    weight,
                    0
                ) + 1
            );
        }
        final ArrayList<DesktopQSpherePoint> points = new ArrayList<>();
        for (int i = 0; i < simulation.stateVector().size(); i++) {
            final StateVectorAmplitude amplitude = simulation.stateVector().get(i);
            final int weight = hammingWeight(amplitude.basisState());
            final int ringIndex = seenByWeight.getOrDefault(
                weight,
                0
            );
            seenByWeight.put(
                weight,
                ringIndex + 1
            );
            final int ringSize = Math.max(
                1,
                totalByWeight.getOrDefault(
                    weight,
                    1
                )
            );
            final double z = qubitCount == 1
                ? (weight == 0 ? 1.0 : -1.0)
                : 1.0 - (2.0 * weight / qubitCount);
            final double radius = Math.sqrt(Math.max(
                0.0,
                1.0 - z * z
            ));
            final double azimuth = 2.0 * Math.PI * ringIndex / ringSize;
            final double real = amplitude.real();
            final double imaginary = amplitude.imaginary();
            points.add(new DesktopQSpherePoint(
                amplitude.basisState(),
                radius * Math.cos(azimuth),
                radius * Math.sin(azimuth),
                z,
                real * real + imaginary * imaginary,
                Math.atan2(
                    imaginary,
                    real
                )
            ));
        }
        return List.copyOf(points);
    }

    private static int hammingWeight(final String basisState) {
        int weight = 0;
        for (int i = 0; i < basisState.length(); i++) {
            if (basisState.charAt(i) == '1') {
                weight++;
            }
        }
        return weight;
    }
}
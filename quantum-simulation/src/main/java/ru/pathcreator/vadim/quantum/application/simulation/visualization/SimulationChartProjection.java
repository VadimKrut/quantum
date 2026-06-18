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
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.application.simulation.result.StateVectorAmplitude;

/**
 * Готовит нормированные строки для графиков и текстового вывода результата симуляции.
 */
public final class SimulationChartProjection {

    public List<MeasurementProbabilityRow> measurementRows(
        final SimulationResult simulation,
        final boolean hideZeroProbability
    ) {
        if (simulation == null) {
            throw new IllegalArgumentException("Simulation result must not be null.");
        }
        final ArrayList<MeasurementProbabilityRow> rows = new ArrayList<>(simulation.counts().size());
        final double shots = Math.max(
            1.0,
            simulation.shots()
        );
        for (final Map.Entry<String, Long> entry : simulation.counts().entrySet()) {
            final double probability = entry.getValue().longValue() / shots;
            if (
                hideZeroProbability
                && probability == 0.0
            ) {
                continue;
            }
            rows.add(new MeasurementProbabilityRow(
                entry.getKey(),
                entry.getValue().longValue(),
                probability
            ));
        }
        rows.sort(Comparator
            .comparingDouble(MeasurementProbabilityRow::probability)
            .reversed()
            .thenComparing(MeasurementProbabilityRow::basisState));
        return List.copyOf(rows);
    }

    public List<StateVectorDisplayRow> stateVectorRows(
        final SimulationResult simulation,
        final boolean hideZeroProbability
    ) {
        if (simulation == null) {
            throw new IllegalArgumentException("Simulation result must not be null.");
        }
        final ArrayList<StateVectorDisplayRow> rows = new ArrayList<>(simulation.stateVector().size());
        for (final StateVectorAmplitude amplitude : simulation.stateVector()) {
            final double magnitude = Math.hypot(
                amplitude.real(),
                amplitude.imaginary()
            );
            final double probability = amplitude.real() * amplitude.real()
                + amplitude.imaginary() * amplitude.imaginary();
            if (
                hideZeroProbability
                && probability == 0.0
            ) {
                continue;
            }
            rows.add(new StateVectorDisplayRow(
                amplitude.basisState(),
                amplitude.real(),
                amplitude.imaginary(),
                magnitude,
                probability,
                Math.atan2(
                    amplitude.imaginary(),
                    amplitude.real()
                )
            ));
        }
        return List.copyOf(rows);
    }
}
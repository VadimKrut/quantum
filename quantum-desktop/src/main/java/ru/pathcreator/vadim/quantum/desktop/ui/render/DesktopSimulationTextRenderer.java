/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.render;

import java.util.Map;

import ru.pathcreator.vadim.quantum.application.simulation.diagnostic.SimulationDiagnosticCode;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;

/**
 * Рендерит результаты локальной симуляции для desktop UI.
 */
public final class DesktopSimulationTextRenderer {

    public String render(
        final SimulationResult simulation,
        final boolean hideZeroProbability
    ) {
        final StringBuilder text = new StringBuilder();
        text.append("Simulation").append(System.lineSeparator());
        text.append("  success: ").append(simulation.isSuccess()).append(System.lineSeparator());
        text.append("  qubits: ").append(simulation.qubitCount()).append(System.lineSeparator());
        text.append("  classical bits: ").append(simulation.classicalBitCount()).append(System.lineSeparator());
        text.append("  shots: ").append(simulation.shots()).append(System.lineSeparator());
        text.append(System.lineSeparator());
        text.append("Counts").append(System.lineSeparator());
        simulation.counts().forEach((state, count) -> text.append("  ")
            .append(state)
            .append(": ")
            .append(count)
            .append(System.lineSeparator()));
        text.append(System.lineSeparator());
        text.append("Measurement frequencies").append(System.lineSeparator());
        appendMeasurementFrequencies(
            text,
            simulation
        );
        text.append(System.lineSeparator());
        if (stateVectorWasCapturedAfterMeasurement(simulation)) {
            text.append("Collapsed trajectory probabilities").append(System.lineSeparator());
            text.append("  State vector was captured after measurement collapse; use Measurement frequencies for shot statistics.")
                .append(System.lineSeparator());
        } else {
            text.append("Exact state-vector probabilities").append(System.lineSeparator());
        }
        for (int i = 0; i < simulation.stateVector().size(); i++) {
            final double real = simulation.stateVector().get(i).real();
            final double imaginary = simulation.stateVector().get(i).imaginary();
            final double probability = real * real + imaginary * imaginary;
            if (
                hideZeroProbability
                && probability == 0.0
            ) {
                continue;
            }
            text.append("  ")
                .append(simulation.stateVector().get(i).basisState())
                .append(": ")
                .append(probability)
                .append(System.lineSeparator());
        }
        text.append(System.lineSeparator());
        text.append("State vector amplitudes").append(System.lineSeparator());
        for (int i = 0; i < simulation.stateVector().size(); i++) {
            final double real = simulation.stateVector().get(i).real();
            final double imaginary = simulation.stateVector().get(i).imaginary();
            final double probability = real * real + imaginary * imaginary;
            if (
                hideZeroProbability
                && probability == 0.0
            ) {
                continue;
            }
            text.append("  ")
                .append(simulation.stateVector().get(i).basisState())
                .append(": ")
                .append(real)
                .append(" + ")
                .append(imaginary)
                .append("i")
                .append(System.lineSeparator());
        }
        if (!simulation.diagnostics().isEmpty()) {
            text.append(System.lineSeparator());
            text.append("Diagnostics").append(System.lineSeparator());
            for (int i = 0; i < simulation.diagnostics().size(); i++) {
                text.append("  ")
                    .append(simulation.diagnostics().get(i).severity())
                    .append(" ")
                    .append(simulation.diagnostics().get(i).code())
                    .append(": ")
                    .append(simulation.diagnostics().get(i).message())
                    .append(System.lineSeparator());
            }
        }
        return text.toString();
    }

    private static void appendMeasurementFrequencies(
        final StringBuilder text,
        final SimulationResult simulation
    ) {
        if (simulation.shots() == 0) {
            text.append("  no shots").append(System.lineSeparator());
            return;
        }
        for (final Map.Entry<String, Long> entry : simulation.counts().entrySet()) {
            text.append("  ")
                .append(entry.getKey())
                .append(": ")
                .append((double) entry.getValue().longValue() / simulation.shots())
                .append(System.lineSeparator());
        }
    }

    private static boolean stateVectorWasCapturedAfterMeasurement(final SimulationResult simulation) {
        for (int i = 0; i < simulation.diagnostics().size(); i++) {
            if (simulation.diagnostics().get(i).code() == SimulationDiagnosticCode.STATE_VECTOR_AFTER_MEASUREMENT) {
                return true;
            }
        }
        return false;
    }
}
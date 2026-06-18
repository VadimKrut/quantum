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
import ru.pathcreator.vadim.quantum.application.simulation.visualization.SimulationChartProjection;
import ru.pathcreator.vadim.quantum.application.simulation.visualization.StateVectorDisplayRow;

/**
 * Рендерит результаты локальной симуляции для desktop UI.
 */
public final class DesktopSimulationTextRenderer {

    private final SimulationChartProjection chartProjection = new SimulationChartProjection();

    public String render(
        final SimulationResult simulation,
        final boolean hideZeroProbability
    ) {
        return render(
            simulation,
            hideZeroProbability,
            false
        );
    }

    public String render(
        final SimulationResult simulation,
        final boolean hideZeroProbability,
        final boolean registerBitOrder
    ) {
        final StringBuilder text = new StringBuilder();
        text.append("Simulation").append(System.lineSeparator());
        text.append("  success: ").append(simulation.isSuccess()).append(System.lineSeparator());
        text.append("  qubits: ").append(simulation.qubitCount()).append(System.lineSeparator());
        text.append("  classical bits: ").append(simulation.classicalBitCount()).append(System.lineSeparator());
        text.append("  shots: ").append(simulation.shots()).append(System.lineSeparator());
        text.append("  bitstring order: ").append(bitOrderText(
            simulation.classicalBitCount(),
            registerBitOrder
        )).append(System.lineSeparator());
        text.append(System.lineSeparator());
        text.append("Counts").append(System.lineSeparator());
        simulation.counts().forEach((state, count) -> text.append("  ")
            .append(displayState(
                state,
                registerBitOrder
            ))
            .append(": ")
            .append(count)
            .append(System.lineSeparator()));
        text.append(System.lineSeparator());
        text.append("Measurement frequencies").append(System.lineSeparator());
        appendMeasurementFrequencies(
            text,
            simulation,
            registerBitOrder
        );
        text.append(System.lineSeparator());
        if (stateVectorWasCapturedAfterMeasurement(simulation)) {
            text.append("Collapsed trajectory probabilities").append(System.lineSeparator());
            text.append("  State vector was captured after measurement collapse; use Measurement frequencies for shot statistics.")
                .append(System.lineSeparator());
        } else {
            text.append("Exact state-vector probabilities").append(System.lineSeparator());
        }
        final java.util.List<StateVectorDisplayRow> stateVectorRows = chartProjection.stateVectorRows(
            simulation,
            hideZeroProbability
        );
        for (int i = 0; i < stateVectorRows.size(); i++) {
            final StateVectorDisplayRow row = stateVectorRows.get(i);
            text.append("  ")
                .append(displayState(
                    row.basisState(),
                    registerBitOrder
                ))
                .append(": ")
                .append(row.probability())
                .append(System.lineSeparator());
        }
        text.append(System.lineSeparator());
        text.append("State vector amplitudes").append(System.lineSeparator());
        for (int i = 0; i < stateVectorRows.size(); i++) {
            final StateVectorDisplayRow row = stateVectorRows.get(i);
            text.append("  ")
                .append(displayState(
                    row.basisState(),
                    registerBitOrder
                ))
                .append(": ")
                .append(row.real())
                .append(" + ")
                .append(row.imaginary())
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
        final SimulationResult simulation,
        final boolean registerBitOrder
    ) {
        if (simulation.shots() == 0) {
            text.append("  no shots").append(System.lineSeparator());
            return;
        }
        for (final Map.Entry<String, Long> entry : simulation.counts().entrySet()) {
            text.append("  ")
                .append(displayState(
                    entry.getKey(),
                    registerBitOrder
                ))
                .append(": ")
                .append((double) entry.getValue().longValue() / simulation.shots())
                .append(System.lineSeparator());
        }
    }

    private static String bitOrderText(
        final int classicalBitCount,
        final boolean registerBitOrder
    ) {
        if (classicalBitCount <= 0) {
            return registerBitOrder
                ? "register order"
                : "standard MSB-first";
        }
        return registerBitOrder
            ? "register order c[0]...c[" + (classicalBitCount - 1) + "]"
            : "standard MSB-first c[" + (classicalBitCount - 1) + "]...c[0]";
    }

    private static String displayState(
        final String state,
        final boolean registerBitOrder
    ) {
        if (!registerBitOrder) {
            return state;
        }
        final char[] characters = state.toCharArray();
        for (int left = 0, right = characters.length - 1; left < right; left++, right--) {
            final char temporary = characters[left];
            characters[left] = characters[right];
            characters[right] = temporary;
        }
        return new String(characters);
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
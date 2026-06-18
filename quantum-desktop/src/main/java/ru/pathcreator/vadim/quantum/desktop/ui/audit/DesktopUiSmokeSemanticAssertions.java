/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.audit;

import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.application.simulation.visualization.SimulationChartProjection;
import ru.pathcreator.vadim.quantum.application.simulation.visualization.StateVectorDisplayRow;
import ru.pathcreator.vadim.quantum.desktop.workspace.DesktopIrWorkspaceService;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

/**
 * Проверяет квантовую семантику UI-smoke сценариев без зависимости от JavaFX-узлов.
 */
public final class DesktopUiSmokeSemanticAssertions {

    private static final SimulationChartProjection CHART_PROJECTION = new SimulationChartProjection();

    private DesktopUiSmokeSemanticAssertions() {
    }

    public static void verifyMeasuredBellSimulationSemantics(final SimulationResult simulation) {
        if (!simulation.isSuccess()) {
            throw new IllegalStateException("Bell simulation is not successful.");
        }
        long countSum = 0L;
        for (final Long count : simulation.counts().values()) {
            countSum += count.longValue();
        }
        if (countSum != simulation.shots()) {
            throw new IllegalStateException("Bell simulation count sum does not match shots.");
        }
        if (
            simulation.counts().containsKey("01")
            || simulation.counts().containsKey("10")
        ) {
            throw new IllegalStateException("Bell simulation produced impossible anti-correlated counts.");
        }
        final long correlatedCount = simulation.counts().getOrDefault(
            "00",
            0L
        ) + simulation.counts().getOrDefault(
            "11",
            0L
        );
        if (correlatedCount != simulation.shots()) {
            throw new IllegalStateException("Bell simulation counts are not fully correlated.");
        }
    }

    public static void verifyPureBellStateVectorSemantics(
        final DesktopIrWorkspaceService workspaceService,
        final long seed
    ) {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("pure_bell");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            2
        );
        circuit.h(q.get(0))
            .cx(
                q.get(0),
                q.get(1)
            );
        final SimulationResult simulation = workspaceService.simulate(
            program,
            0,
            seed
        );
        if (!simulation.isSuccess()) {
            throw new IllegalStateException("Pure Bell state-vector simulation is not successful.");
        }
        verifyProbability(
            simulation,
            "00",
            0.5
        );
        verifyProbability(
            simulation,
            "01",
            0.0
        );
        verifyProbability(
            simulation,
            "10",
            0.0
        );
        verifyProbability(
            simulation,
            "11",
            0.5
        );
    }

    private static void verifyProbability(
        final SimulationResult simulation,
        final String basisState,
        final double expected
    ) {
        final java.util.List<StateVectorDisplayRow> rows = CHART_PROJECTION.stateVectorRows(
            simulation,
            false
        );
        for (final StateVectorDisplayRow row : rows) {
            if (basisState.equals(row.basisState())) {
                final double actual = row.probability();
                if (Math.abs(actual - expected) > 1.0e-10) {
                    throw new IllegalStateException("Unexpected probability for " + basisState + ": " + actual + ".");
                }
                return;
            }
        }
        if (expected != 0.0) {
            throw new IllegalStateException("Missing expected basis state: " + basisState + ".");
        }
    }
}
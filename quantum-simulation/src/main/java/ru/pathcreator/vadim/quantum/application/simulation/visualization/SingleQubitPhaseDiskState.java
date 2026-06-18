/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.simulation.visualization;

/**
 * Локальное reduced-состояние одного qubit для phase-disk визуализации.
 */
public record SingleQubitPhaseDiskState(
    int qubitIndex,
    double oneProbability,
    double phase,
    double purity
) {

    public SingleQubitPhaseDiskState {
        if (qubitIndex < 0) {
            throw new IllegalArgumentException("Phase-disk qubit index must not be negative.");
        }
        if (
            !Double.isFinite(oneProbability)
            || !Double.isFinite(phase)
            || !Double.isFinite(purity)
        ) {
            throw new IllegalArgumentException("Phase-disk state values must be finite.");
        }
        if (
            oneProbability < 0.0
            || oneProbability > 1.0
            || purity < 0.0
            || purity > 1.0
        ) {
            throw new IllegalArgumentException("Phase-disk probabilities must be normalized.");
        }
    }
}
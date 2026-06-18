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
 * Строка state-vector для текстового вывода и графика амплитуд.
 */
public record StateVectorDisplayRow(
    String basisState,
    double real,
    double imaginary,
    double magnitude,
    double probability,
    double phase
) {

    public StateVectorDisplayRow {
        if (
            basisState == null
            || basisState.isBlank()
        ) {
            throw new IllegalArgumentException("State-vector basis state must not be blank.");
        }
        if (
            !Double.isFinite(real)
            || !Double.isFinite(imaginary)
            || !Double.isFinite(magnitude)
            || !Double.isFinite(probability)
            || !Double.isFinite(phase)
        ) {
            throw new IllegalArgumentException("State-vector display values must be finite.");
        }
        if (
            magnitude < 0.0
            || probability < 0.0
        ) {
            throw new IllegalArgumentException("State-vector magnitude and probability must not be negative.");
        }
    }
}
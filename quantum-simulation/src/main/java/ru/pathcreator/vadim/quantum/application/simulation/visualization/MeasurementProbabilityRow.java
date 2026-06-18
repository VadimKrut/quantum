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
 * Строка нормированной гистограммы измерений.
 */
public record MeasurementProbabilityRow(
    String basisState,
    long count,
    double probability
) {

    public MeasurementProbabilityRow {
        if (
            basisState == null
            || basisState.isBlank()
        ) {
            throw new IllegalArgumentException("Measurement basis state must not be blank.");
        }
        if (count < 0L) {
            throw new IllegalArgumentException("Measurement count must not be negative.");
        }
        if (
            !Double.isFinite(probability)
            || probability < 0.0
            || probability > 1.0
        ) {
            throw new IllegalArgumentException("Measurement probability must be normalized.");
        }
    }
}
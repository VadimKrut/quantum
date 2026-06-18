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
 * Одна точка basis-state, спроецированная из state-vector на q-sphere.
 */
public record QSpherePoint(
    String basisState,
    double x,
    double y,
    double z,
    double probability,
    double phase
) {

    public QSpherePoint {
        if (
            basisState == null
            || basisState.isBlank()
        ) {
            throw new IllegalArgumentException("Q-sphere basis state must not be blank.");
        }
        if (
            !Double.isFinite(x)
            || !Double.isFinite(y)
            || !Double.isFinite(z)
            || !Double.isFinite(probability)
            || !Double.isFinite(phase)
        ) {
            throw new IllegalArgumentException("Q-sphere point values must be finite.");
        }
    }
}
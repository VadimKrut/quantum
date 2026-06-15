/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.simulation.result;

/**
 * Амплитуда одного basis-state локального state-vector результата.
 */
public final class StateVectorAmplitude {

    private final String basisState;
    private final double real;
    private final double imaginary;

    public StateVectorAmplitude(
        final String basisState,
        final double real,
        final double imaginary
    ) {
        if (
            basisState == null
            || basisState.isBlank()
        ) {
            throw new IllegalArgumentException("State-vector basis state must not be blank.");
        }
        if (
            !Double.isFinite(real)
            || !Double.isFinite(imaginary)
        ) {
            throw new IllegalArgumentException("State-vector amplitude values must be finite.");
        }
        this.basisState = basisState;
        this.real = real;
        this.imaginary = imaginary;
    }

    public String basisState() {
        return basisState;
    }

    public double real() {
        return real;
    }

    public double imaginary() {
        return imaginary;
    }
}
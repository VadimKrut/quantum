/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.simulation.options;

import ru.pathcreator.vadim.quantum.domain.parameter.ParameterBindings;

/**
 * Неизменяемые настройки локальной state-vector симуляции.
 */
public final class SimulationOptions {

    private final int shots;
    private final long seed;
    private final int maxQubits;
    private final boolean captureStateVector;
    private final ParameterBindings parameterBindings;
    private final SimulationUnsupportedOperationPolicy unsupportedOperationPolicy;

    private SimulationOptions(final Builder builder) {
        this.shots = builder.shots;
        this.seed = builder.seed;
        this.maxQubits = builder.maxQubits;
        this.captureStateVector = builder.captureStateVector;
        this.parameterBindings = builder.parameterBindings;
        this.unsupportedOperationPolicy = builder.unsupportedOperationPolicy;
    }

    public static SimulationOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public int shots() {
        return shots;
    }

    public long seed() {
        return seed;
    }

    public int maxQubits() {
        return maxQubits;
    }

    public boolean captureStateVector() {
        return captureStateVector;
    }

    public ParameterBindings parameterBindings() {
        return parameterBindings;
    }

    public SimulationUnsupportedOperationPolicy unsupportedOperationPolicy() {
        return unsupportedOperationPolicy;
    }

    /**
     * Builder настроек симуляции.
     */
    public static final class Builder {

        private int shots;
        private long seed;
        private int maxQubits;
        private boolean captureStateVector;
        private ParameterBindings parameterBindings;
        private SimulationUnsupportedOperationPolicy unsupportedOperationPolicy;

        private Builder() {
            this.shots = 1024;
            this.seed = 1L;
            this.maxQubits = 20;
            this.captureStateVector = true;
            this.parameterBindings = ParameterBindings.empty();
            this.unsupportedOperationPolicy = SimulationUnsupportedOperationPolicy.FAIL;
        }

        public Builder shots(final int shots) {
            if (shots < 0) {
                throw new IllegalArgumentException("Simulation shots must not be negative.");
            }
            this.shots = shots;
            return this;
        }

        public Builder seed(final long seed) {
            this.seed = seed;
            return this;
        }

        public Builder maxQubits(final int maxQubits) {
            if (maxQubits <= 0) {
                throw new IllegalArgumentException("Simulation max qubits must be positive.");
            }
            if (maxQubits > 30) {
                throw new IllegalArgumentException("Simulation max qubits above 30 is not supported by this engine.");
            }
            this.maxQubits = maxQubits;
            return this;
        }

        public Builder captureStateVector(final boolean captureStateVector) {
            this.captureStateVector = captureStateVector;
            return this;
        }

        public Builder parameterBindings(final ParameterBindings parameterBindings) {
            if (parameterBindings == null) {
                throw new IllegalArgumentException("Simulation parameter bindings must not be null.");
            }
            this.parameterBindings = parameterBindings;
            return this;
        }

        public Builder unsupportedOperationPolicy(
            final SimulationUnsupportedOperationPolicy unsupportedOperationPolicy
        ) {
            if (unsupportedOperationPolicy == null) {
                throw new IllegalArgumentException("Simulation unsupported operation policy must not be null.");
            }
            this.unsupportedOperationPolicy = unsupportedOperationPolicy;
            return this;
        }

        public SimulationOptions build() {
            return new SimulationOptions(this);
        }
    }
}
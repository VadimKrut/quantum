/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.backend;

import java.util.LinkedHashMap;
import java.util.Map;

import ru.pathcreator.vadim.quantum.application.compiler.CompilerOptions;
import ru.pathcreator.vadim.quantum.application.simulation.options.SimulationOptions;

public final class BackendJobOptions {

    private final CompilerOptions compilerOptions;
    private final SimulationOptions simulationOptions;
    private final String authenticationProfile;
    private final Map<String, String> metadata;

    private BackendJobOptions(final Builder builder) {
        this.compilerOptions = builder.compilerOptions;
        this.simulationOptions = builder.simulationOptions;
        this.authenticationProfile = builder.authenticationProfile;
        this.metadata = Map.copyOf(builder.metadata);
    }

    public static BackendJobOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public CompilerOptions compilerOptions() {
        return compilerOptions;
    }

    public SimulationOptions simulationOptions() {
        return simulationOptions;
    }

    public boolean hasAuthenticationProfile() {
        return authenticationProfile != null;
    }

    public String authenticationProfile() {
        if (authenticationProfile == null) {
            throw new IllegalStateException("Backend job options do not contain authentication profile.");
        }
        return authenticationProfile;
    }

    public Map<String, String> metadata() {
        return metadata;
    }

    public static final class Builder {

        private CompilerOptions compilerOptions;
        private SimulationOptions simulationOptions;
        private String authenticationProfile;
        private final LinkedHashMap<String, String> metadata;

        private Builder() {
            this.compilerOptions = CompilerOptions.defaults();
            this.simulationOptions = SimulationOptions.defaults();
            this.metadata = new LinkedHashMap<>();
        }

        public Builder compilerOptions(final CompilerOptions compilerOptions) {
            if (compilerOptions == null) {
                throw new IllegalArgumentException("Backend compiler options must not be null.");
            }
            this.compilerOptions = compilerOptions;
            return this;
        }

        public Builder simulationOptions(final SimulationOptions simulationOptions) {
            if (simulationOptions == null) {
                throw new IllegalArgumentException("Backend simulation options must not be null.");
            }
            this.simulationOptions = simulationOptions;
            return this;
        }

        public Builder authenticationProfile(final String authenticationProfile) {
            if (
                authenticationProfile == null
                || authenticationProfile.isBlank()
            ) {
                throw new IllegalArgumentException("Backend authentication profile must not be blank.");
            }
            this.authenticationProfile = authenticationProfile;
            return this;
        }

        public Builder metadata(
            final String key,
            final String value
        ) {
            if (
                key == null
                || key.isBlank()
            ) {
                throw new IllegalArgumentException("Backend job metadata key must not be blank.");
            }
            if (value == null) {
                throw new IllegalArgumentException("Backend job metadata value must not be null.");
            }
            metadata.put(
                key,
                value
            );
            return this;
        }

        public BackendJobOptions build() {
            return new BackendJobOptions(this);
        }
    }
}
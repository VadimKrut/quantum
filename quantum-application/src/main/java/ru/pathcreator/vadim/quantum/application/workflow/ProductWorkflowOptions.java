/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.workflow;

import ru.pathcreator.vadim.quantum.application.compiler.CompilerOptions;
import ru.pathcreator.vadim.quantum.application.simulation.options.SimulationOptions;

public final class ProductWorkflowOptions {

    private final SimulationOptions simulationOptions;
    private final CompilerOptions compilerOptions;
    private final int resourceMaxQubits;
    private final boolean runValidation;
    private final boolean runInspection;
    private final boolean runResources;
    private final boolean runTimeline;
    private final boolean runPreflight;
    private final boolean runSimulation;
    private final boolean runCompiler;
    private final boolean runBackendDryRun;

    private ProductWorkflowOptions(final Builder builder) {
        this.simulationOptions = builder.simulationOptions;
        this.compilerOptions = builder.compilerOptions;
        this.resourceMaxQubits = builder.resourceMaxQubits;
        this.runValidation = builder.runValidation;
        this.runInspection = builder.runInspection;
        this.runResources = builder.runResources;
        this.runTimeline = builder.runTimeline;
        this.runPreflight = builder.runPreflight;
        this.runSimulation = builder.runSimulation;
        this.runCompiler = builder.runCompiler;
        this.runBackendDryRun = builder.runBackendDryRun;
    }

    public static ProductWorkflowOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public SimulationOptions simulationOptions() {
        return simulationOptions;
    }

    public CompilerOptions compilerOptions() {
        return compilerOptions;
    }

    public int resourceMaxQubits() {
        return resourceMaxQubits;
    }

    public boolean runValidation() {
        return runValidation;
    }

    public boolean runInspection() {
        return runInspection;
    }

    public boolean runResources() {
        return runResources;
    }

    public boolean runTimeline() {
        return runTimeline;
    }

    public boolean runPreflight() {
        return runPreflight;
    }

    public boolean runSimulation() {
        return runSimulation;
    }

    public boolean runCompiler() {
        return runCompiler;
    }

    public boolean runBackendDryRun() {
        return runBackendDryRun;
    }

    public static final class Builder {

        private SimulationOptions simulationOptions;
        private CompilerOptions compilerOptions;
        private int resourceMaxQubits;
        private boolean runValidation;
        private boolean runInspection;
        private boolean runResources;
        private boolean runTimeline;
        private boolean runPreflight;
        private boolean runSimulation;
        private boolean runCompiler;
        private boolean runBackendDryRun;

        private Builder() {
            this.simulationOptions = SimulationOptions.defaults();
            this.compilerOptions = CompilerOptions.defaults();
            this.resourceMaxQubits = 20;
            this.runValidation = true;
            this.runInspection = true;
            this.runResources = true;
            this.runTimeline = true;
            this.runPreflight = true;
            this.runSimulation = true;
            this.runCompiler = true;
            this.runBackendDryRun = true;
        }

        public Builder simulationOptions(final SimulationOptions simulationOptions) {
            if (simulationOptions == null) {
                throw new IllegalArgumentException("Product workflow simulation options must not be null.");
            }
            this.simulationOptions = simulationOptions;
            return this;
        }

        public Builder compilerOptions(final CompilerOptions compilerOptions) {
            if (compilerOptions == null) {
                throw new IllegalArgumentException("Product workflow compiler options must not be null.");
            }
            this.compilerOptions = compilerOptions;
            return this;
        }

        public Builder resourceMaxQubits(final int resourceMaxQubits) {
            if (resourceMaxQubits <= 0) {
                throw new IllegalArgumentException("Product workflow resource max qubits must be positive.");
            }
            this.resourceMaxQubits = resourceMaxQubits;
            return this;
        }

        public Builder runValidation(final boolean runValidation) {
            this.runValidation = runValidation;
            return this;
        }

        public Builder runInspection(final boolean runInspection) {
            this.runInspection = runInspection;
            return this;
        }

        public Builder runResources(final boolean runResources) {
            this.runResources = runResources;
            return this;
        }

        public Builder runTimeline(final boolean runTimeline) {
            this.runTimeline = runTimeline;
            return this;
        }

        public Builder runPreflight(final boolean runPreflight) {
            this.runPreflight = runPreflight;
            return this;
        }

        public Builder runSimulation(final boolean runSimulation) {
            this.runSimulation = runSimulation;
            return this;
        }

        public Builder runCompiler(final boolean runCompiler) {
            this.runCompiler = runCompiler;
            return this;
        }

        public Builder runBackendDryRun(final boolean runBackendDryRun) {
            this.runBackendDryRun = runBackendDryRun;
            return this;
        }

        public Builder fastWorkflow() {
            this.runInspection = false;
            this.runResources = false;
            this.runTimeline = false;
            this.runPreflight = false;
            this.runSimulation = false;
            this.runBackendDryRun = false;
            this.compilerOptions = CompilerOptions.builder()
                .fastExportOnly()
                .build();
            return this;
        }

        public ProductWorkflowOptions build() {
            return new ProductWorkflowOptions(this);
        }
    }
}
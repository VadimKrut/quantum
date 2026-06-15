/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.benchmark;

import ru.pathcreator.vadim.quantum.application.workflow.ProductWorkflowOptions;

public final class ProductBenchmarkOptions {

    private final int warmupIterations;
    private final int measurementIterations;
    private final boolean includeImport;
    private final ProductWorkflowOptions workflowOptions;

    private ProductBenchmarkOptions(final Builder builder) {
        this.warmupIterations = builder.warmupIterations;
        this.measurementIterations = builder.measurementIterations;
        this.includeImport = builder.includeImport;
        this.workflowOptions = builder.workflowOptions;
    }

    public static ProductBenchmarkOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public int warmupIterations() {
        return warmupIterations;
    }

    public int measurementIterations() {
        return measurementIterations;
    }

    public boolean includeImport() {
        return includeImport;
    }

    public ProductWorkflowOptions workflowOptions() {
        return workflowOptions;
    }

    public static final class Builder {

        private int warmupIterations;
        private int measurementIterations;
        private boolean includeImport;
        private ProductWorkflowOptions workflowOptions;

        private Builder() {
            this.warmupIterations = 1;
            this.measurementIterations = 3;
            this.includeImport = true;
            this.workflowOptions = ProductWorkflowOptions.defaults();
        }

        public Builder warmupIterations(final int warmupIterations) {
            if (warmupIterations < 0) {
                throw new IllegalArgumentException("Benchmark warmup iterations must not be negative.");
            }
            this.warmupIterations = warmupIterations;
            return this;
        }

        public Builder measurementIterations(final int measurementIterations) {
            if (measurementIterations <= 0) {
                throw new IllegalArgumentException("Benchmark measurement iterations must be positive.");
            }
            this.measurementIterations = measurementIterations;
            return this;
        }

        public Builder includeImport(final boolean includeImport) {
            this.includeImport = includeImport;
            return this;
        }

        public Builder workflowOptions(final ProductWorkflowOptions workflowOptions) {
            if (workflowOptions == null) {
                throw new IllegalArgumentException("Benchmark workflow options must not be null.");
            }
            this.workflowOptions = workflowOptions;
            return this;
        }

        public ProductBenchmarkOptions build() {
            return new ProductBenchmarkOptions(this);
        }
    }
}
/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.workflow;

import ru.pathcreator.vadim.quantum.application.compiler.CompilerOptions;
import ru.pathcreator.vadim.quantum.application.workflow.ProductWorkflowOptions;

/**
 * Desktop-настройки тяжелых проверок и оптимизационных стадий.
 */
public final class DesktopExecutionOptions {

    private final boolean fast;
    private final boolean skipValidation;
    private final boolean skipInspection;
    private final boolean skipPreflight;
    private final boolean skipTransformation;
    private final boolean skipTransformedValidation;
    private final boolean skipTransformedInspection;
    private final boolean skipTransformedPreflight;
    private final boolean skipResources;
    private final boolean skipTimeline;
    private final boolean skipSimulation;
    private final boolean skipCompiler;
    private final boolean skipBackend;

    private DesktopExecutionOptions(final Builder builder) {
        this.fast = builder.fast;
        this.skipValidation = builder.skipValidation;
        this.skipInspection = builder.skipInspection;
        this.skipPreflight = builder.skipPreflight;
        this.skipTransformation = builder.skipTransformation;
        this.skipTransformedValidation = builder.skipTransformedValidation;
        this.skipTransformedInspection = builder.skipTransformedInspection;
        this.skipTransformedPreflight = builder.skipTransformedPreflight;
        this.skipResources = builder.skipResources;
        this.skipTimeline = builder.skipTimeline;
        this.skipSimulation = builder.skipSimulation;
        this.skipCompiler = builder.skipCompiler;
        this.skipBackend = builder.skipBackend;
    }

    public static DesktopExecutionOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public CompilerOptions compilerOptions() {
        final CompilerOptions.Builder options = CompilerOptions.builder();
        if (fast) {
            options.fastExportOnly();
        }
        if (skipValidation) {
            options.skipInitialValidation()
                .skipTransformedValidation();
        }
        if (skipInspection) {
            options.skipInitialInspection()
                .skipTransformedInspection();
        }
        if (skipPreflight) {
            options.skipInitialPreflight()
                .skipTransformedPreflight();
        }
        if (skipTransformation) {
            options.skipTransformation();
        }
        if (skipTransformedValidation) {
            options.skipTransformedValidation();
        }
        if (skipTransformedInspection) {
            options.skipTransformedInspection();
        }
        if (skipTransformedPreflight) {
            options.skipTransformedPreflight();
        }
        return options.build();
    }

    public ProductWorkflowOptions workflowOptions(
        final int shots,
        final long seed
    ) {
        final ProductWorkflowOptions.Builder options = ProductWorkflowOptions.builder()
            .simulationOptions(DesktopWorkflowService.simulationOptions(
                shots,
                seed
            ))
            .compilerOptions(compilerOptions())
            .runValidation(!skipValidation)
            .runInspection(!skipInspection)
            .runResources(!skipResources)
            .runTimeline(!skipTimeline)
            .runPreflight(!skipPreflight)
            .runSimulation(!skipSimulation)
            .runCompiler(!skipCompiler)
            .runBackendDryRun(!skipBackend);
        if (fast) {
            options.fastWorkflow()
                .runValidation(!skipValidation);
        }
        return options.build();
    }

    public static final class Builder {

        private boolean fast;
        private boolean skipValidation;
        private boolean skipInspection;
        private boolean skipPreflight;
        private boolean skipTransformation;
        private boolean skipTransformedValidation;
        private boolean skipTransformedInspection;
        private boolean skipTransformedPreflight;
        private boolean skipResources;
        private boolean skipTimeline;
        private boolean skipSimulation;
        private boolean skipCompiler;
        private boolean skipBackend;

        private Builder() {
        }

        public Builder fast(final boolean fast) {
            this.fast = fast;
            return this;
        }

        public Builder skipValidation(final boolean skipValidation) {
            this.skipValidation = skipValidation;
            return this;
        }

        public Builder skipInspection(final boolean skipInspection) {
            this.skipInspection = skipInspection;
            return this;
        }

        public Builder skipPreflight(final boolean skipPreflight) {
            this.skipPreflight = skipPreflight;
            return this;
        }

        public Builder skipTransformation(final boolean skipTransformation) {
            this.skipTransformation = skipTransformation;
            return this;
        }

        public Builder skipTransformedValidation(final boolean skipTransformedValidation) {
            this.skipTransformedValidation = skipTransformedValidation;
            return this;
        }

        public Builder skipTransformedInspection(final boolean skipTransformedInspection) {
            this.skipTransformedInspection = skipTransformedInspection;
            return this;
        }

        public Builder skipTransformedPreflight(final boolean skipTransformedPreflight) {
            this.skipTransformedPreflight = skipTransformedPreflight;
            return this;
        }

        public Builder skipResources(final boolean skipResources) {
            this.skipResources = skipResources;
            return this;
        }

        public Builder skipTimeline(final boolean skipTimeline) {
            this.skipTimeline = skipTimeline;
            return this;
        }

        public Builder skipSimulation(final boolean skipSimulation) {
            this.skipSimulation = skipSimulation;
            return this;
        }

        public Builder skipCompiler(final boolean skipCompiler) {
            this.skipCompiler = skipCompiler;
            return this;
        }

        public Builder skipBackend(final boolean skipBackend) {
            this.skipBackend = skipBackend;
            return this;
        }

        public DesktopExecutionOptions build() {
            return new DesktopExecutionOptions(this);
        }
    }
}
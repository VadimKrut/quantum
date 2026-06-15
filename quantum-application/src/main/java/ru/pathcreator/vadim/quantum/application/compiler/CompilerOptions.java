/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.compiler;

import ru.pathcreator.vadim.quantum.application.integration.options.ExportOptions;
import ru.pathcreator.vadim.quantum.application.transformation.TransformationOptions;

/**
 * Immutable настройки compiler pipeline.
 */
public final class CompilerOptions {

    private final ExportOptions exportOptions;
    private final TransformationOptions transformationOptions;
    private final boolean runInitialValidation;
    private final boolean runInitialInspection;
    private final boolean runInitialPreflight;
    private final boolean runTransformation;
    private final boolean runTransformedValidation;
    private final boolean runTransformedInspection;
    private final boolean runTransformedPreflight;
    private final boolean stopOnValidationError;
    private final boolean stopOnUnsupportedTarget;
    private final boolean stopOnTransformationError;
    private final boolean stopOnWarnings;

    private CompilerOptions(final Builder builder) {
        this.exportOptions = builder.exportOptions;
        this.transformationOptions = builder.transformationOptions;
        this.runInitialValidation = builder.runInitialValidation;
        this.runInitialInspection = builder.runInitialInspection;
        this.runInitialPreflight = builder.runInitialPreflight;
        this.runTransformation = builder.runTransformation;
        this.runTransformedValidation = builder.runTransformedValidation;
        this.runTransformedInspection = builder.runTransformedInspection;
        this.runTransformedPreflight = builder.runTransformedPreflight;
        this.stopOnValidationError = builder.stopOnValidationError;
        this.stopOnUnsupportedTarget = builder.stopOnUnsupportedTarget;
        this.stopOnTransformationError = builder.stopOnTransformationError;
        this.stopOnWarnings = builder.stopOnWarnings;
    }

    public static CompilerOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public ExportOptions exportOptions() {
        return exportOptions;
    }

    public TransformationOptions transformationOptions() {
        return transformationOptions;
    }

    public boolean runInitialValidation() {
        return runInitialValidation;
    }

    public boolean runInitialInspection() {
        return runInitialInspection;
    }

    public boolean runInitialPreflight() {
        return runInitialPreflight;
    }

    public boolean runTransformation() {
        return runTransformation;
    }

    public boolean runTransformedValidation() {
        return runTransformedValidation;
    }

    public boolean runTransformedInspection() {
        return runTransformedInspection;
    }

    public boolean runTransformedPreflight() {
        return runTransformedPreflight;
    }

    public boolean stopOnValidationError() {
        return stopOnValidationError;
    }

    public boolean stopOnUnsupportedTarget() {
        return stopOnUnsupportedTarget;
    }

    public boolean stopOnTransformationError() {
        return stopOnTransformationError;
    }

    public boolean stopOnWarnings() {
        return stopOnWarnings;
    }

    /**
     * Builder immutable настроек compiler pipeline.
     */
    public static final class Builder {

        private ExportOptions exportOptions;
        private TransformationOptions transformationOptions;
        private boolean runInitialValidation;
        private boolean runInitialInspection;
        private boolean runInitialPreflight;
        private boolean runTransformation;
        private boolean runTransformedValidation;
        private boolean runTransformedInspection;
        private boolean runTransformedPreflight;
        private boolean stopOnValidationError;
        private boolean stopOnUnsupportedTarget;
        private boolean stopOnTransformationError;
        private boolean stopOnWarnings;

        private Builder() {
            this.exportOptions = ExportOptions.defaults();
            this.transformationOptions = TransformationOptions.none();
            this.runInitialValidation = true;
            this.runInitialInspection = true;
            this.runInitialPreflight = true;
            this.runTransformation = true;
            this.runTransformedValidation = true;
            this.runTransformedInspection = true;
            this.runTransformedPreflight = true;
            this.stopOnValidationError = true;
            this.stopOnUnsupportedTarget = true;
            this.stopOnTransformationError = true;
            this.stopOnWarnings = false;
        }

        public Builder exportOptions(final ExportOptions exportOptions) {
            if (exportOptions == null) {
                throw new IllegalArgumentException("Export options must not be null.");
            }
            this.exportOptions = exportOptions;
            return this;
        }

        public Builder transformationOptions(final TransformationOptions transformationOptions) {
            if (transformationOptions == null) {
                throw new IllegalArgumentException("Transformation options must not be null.");
            }
            this.transformationOptions = transformationOptions;
            return this;
        }

        public Builder skipInitialValidation() {
            this.runInitialValidation = false;
            this.stopOnValidationError = false;
            return this;
        }

        public Builder skipInitialInspection() {
            this.runInitialInspection = false;
            return this;
        }

        public Builder skipInitialPreflight() {
            this.runInitialPreflight = false;
            this.stopOnUnsupportedTarget = false;
            return this;
        }

        public Builder skipTransformation() {
            this.runTransformation = false;
            this.stopOnTransformationError = false;
            return this;
        }

        public Builder skipTransformedValidation() {
            this.runTransformedValidation = false;
            return this;
        }

        public Builder skipTransformedInspection() {
            this.runTransformedInspection = false;
            return this;
        }

        public Builder skipTransformedPreflight() {
            this.runTransformedPreflight = false;
            return this;
        }

        public Builder fastExportOnly() {
            return skipInitialValidation()
                .skipInitialInspection()
                .skipInitialPreflight()
                .skipTransformation()
                .skipTransformedValidation()
                .skipTransformedInspection()
                .skipTransformedPreflight();
        }

        public Builder continueOnValidationError() {
            this.stopOnValidationError = false;
            return this;
        }

        public Builder continueOnUnsupportedTarget() {
            this.stopOnUnsupportedTarget = false;
            return this;
        }

        public Builder continueOnTransformationError() {
            this.stopOnTransformationError = false;
            return this;
        }

        public Builder stopOnWarnings() {
            this.stopOnWarnings = true;
            return this;
        }

        public CompilerOptions build() {
            return new CompilerOptions(this);
        }
    }
}
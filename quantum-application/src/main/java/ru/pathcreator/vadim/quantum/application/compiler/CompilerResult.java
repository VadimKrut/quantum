/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.compiler;

import java.util.List;

import ru.pathcreator.vadim.quantum.application.inspection.ProgramInspectionResult;
import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightResult;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.integration.result.ExportResult;
import ru.pathcreator.vadim.quantum.application.transformation.TransformationResult;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

/**
 * Immutable результат полного compiler pipeline.
 */
public final class CompilerResult {

    private final IntegrationFormat format;
    private final CompilerResultStatus status;
    private final QuantumProgram originalProgram;
    private final QuantumProgram transformedProgram;
    private final ValidationResult initialValidation;
    private final ProgramInspectionResult initialInspection;
    private final CapabilityPreflightResult initialPreflight;
    private final TransformationResult transformation;
    private final ValidationResult transformedValidation;
    private final ProgramInspectionResult transformedInspection;
    private final CapabilityPreflightResult transformedPreflight;
    private final ExportResult exportResult;
    private final List<CompilerStageRecord> stageRecords;

    private CompilerResult(final Builder builder) {
        this.format = builder.format;
        this.status = builder.status;
        this.originalProgram = builder.originalProgram;
        this.transformedProgram = builder.transformedProgram;
        this.initialValidation = builder.initialValidation;
        this.initialInspection = builder.initialInspection;
        this.initialPreflight = builder.initialPreflight;
        this.transformation = builder.transformation;
        this.transformedValidation = builder.transformedValidation;
        this.transformedInspection = builder.transformedInspection;
        this.transformedPreflight = builder.transformedPreflight;
        this.exportResult = builder.exportResult;
        this.stageRecords = List.copyOf(builder.stageRecords);
    }

    static Builder builder(
        final IntegrationFormat format,
        final QuantumProgram originalProgram
    ) {
        if (format == null) {
            throw new IllegalArgumentException("Compiler result format must not be null.");
        }
        if (originalProgram == null) {
            throw new IllegalArgumentException("Compiler result original program must not be null.");
        }
        return new Builder(
            format,
            originalProgram
        );
    }

    public IntegrationFormat format() {
        return format;
    }

    public CompilerResultStatus status() {
        return status;
    }

    public boolean isSuccess() {
        return status == CompilerResultStatus.EXPORTED
            && exportResult != null
            && exportResult.isSuccess();
    }

    public QuantumProgram originalProgram() {
        return originalProgram;
    }

    public boolean hasTransformedProgram() {
        return transformedProgram != null;
    }

    public QuantumProgram transformedProgram() {
        if (transformedProgram == null) {
            throw new IllegalStateException("Compiler result does not contain transformed program.");
        }
        return transformedProgram;
    }

    public ValidationResult initialValidation() {
        return initialValidation;
    }

    public ProgramInspectionResult initialInspection() {
        return initialInspection;
    }

    public CapabilityPreflightResult initialPreflight() {
        return initialPreflight;
    }

    public TransformationResult transformation() {
        return transformation;
    }

    public ValidationResult transformedValidation() {
        return transformedValidation;
    }

    public ProgramInspectionResult transformedInspection() {
        return transformedInspection;
    }

    public CapabilityPreflightResult transformedPreflight() {
        return transformedPreflight;
    }

    public boolean hasExportResult() {
        return exportResult != null;
    }

    public ExportResult exportResult() {
        if (exportResult == null) {
            throw new IllegalStateException("Compiler result does not contain export result.");
        }
        return exportResult;
    }

    public List<CompilerStageRecord> stageRecords() {
        return stageRecords;
    }

    static final class Builder {

        private final IntegrationFormat format;
        private final QuantumProgram originalProgram;
        private CompilerResultStatus status;
        private QuantumProgram transformedProgram;
        private ValidationResult initialValidation;
        private ProgramInspectionResult initialInspection;
        private CapabilityPreflightResult initialPreflight;
        private TransformationResult transformation;
        private ValidationResult transformedValidation;
        private ProgramInspectionResult transformedInspection;
        private CapabilityPreflightResult transformedPreflight;
        private ExportResult exportResult;
        private final java.util.ArrayList<CompilerStageRecord> stageRecords;

        private Builder(
            final IntegrationFormat format,
            final QuantumProgram originalProgram
        ) {
            this.format = format;
            this.originalProgram = originalProgram;
            this.status = CompilerResultStatus.EXPORT_FAILED;
            this.stageRecords = new java.util.ArrayList<>();
        }

        Builder status(final CompilerResultStatus status) {
            this.status = status;
            return this;
        }

        Builder transformedProgram(final QuantumProgram transformedProgram) {
            this.transformedProgram = transformedProgram;
            return this;
        }

        Builder initialValidation(final ValidationResult initialValidation) {
            this.initialValidation = initialValidation;
            return this;
        }

        Builder initialInspection(final ProgramInspectionResult initialInspection) {
            this.initialInspection = initialInspection;
            return this;
        }

        Builder initialPreflight(final CapabilityPreflightResult initialPreflight) {
            this.initialPreflight = initialPreflight;
            return this;
        }

        Builder transformation(final TransformationResult transformation) {
            this.transformation = transformation;
            return this;
        }

        Builder transformedValidation(final ValidationResult transformedValidation) {
            this.transformedValidation = transformedValidation;
            return this;
        }

        Builder transformedInspection(final ProgramInspectionResult transformedInspection) {
            this.transformedInspection = transformedInspection;
            return this;
        }

        Builder transformedPreflight(final CapabilityPreflightResult transformedPreflight) {
            this.transformedPreflight = transformedPreflight;
            return this;
        }

        Builder exportResult(final ExportResult exportResult) {
            this.exportResult = exportResult;
            return this;
        }

        Builder addStage(final CompilerStageRecord record) {
            this.stageRecords.add(record);
            return this;
        }

        CompilerResult build() {
            if (status == null) {
                throw new IllegalStateException("Compiler result status must not be null.");
            }
            return new CompilerResult(this);
        }
    }
}
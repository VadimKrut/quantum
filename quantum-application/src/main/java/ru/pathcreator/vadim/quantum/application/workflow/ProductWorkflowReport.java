/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.workflow;

import ru.pathcreator.vadim.quantum.application.backend.BackendExecutionResult;
import ru.pathcreator.vadim.quantum.application.backend.BackendSubmissionResult;
import ru.pathcreator.vadim.quantum.application.compiler.CompilerResult;
import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightResult;
import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapabilityProfile;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.inspection.ProgramInspectionResult;
import ru.pathcreator.vadim.quantum.application.resource.ResourceEstimate;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.application.visualization.ProgramTimeline;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

public final class ProductWorkflowReport {

    private final IntegrationFormat targetFormat;
    private final ProductWorkflowStatus status;
    private final ValidationResult validation;
    private final ProgramInspectionResult inspection;
    private final CapabilityPreflightResult preflight;
    private final ResourceEstimate resources;
    private final ProgramTimeline timeline;
    private final SimulationResult simulation;
    private final CompilerResult compiler;
    private final BackendSubmissionResult backendSubmission;
    private final BackendExecutionResult backendExecution;
    private final IntegrationCapabilityProfile targetProfile;

    ProductWorkflowReport(final Builder builder) {
        this.targetFormat = builder.targetFormat;
        this.status = builder.status;
        this.validation = builder.validation;
        this.inspection = builder.inspection;
        this.preflight = builder.preflight;
        this.resources = builder.resources;
        this.timeline = builder.timeline;
        this.simulation = builder.simulation;
        this.compiler = builder.compiler;
        this.backendSubmission = builder.backendSubmission;
        this.backendExecution = builder.backendExecution;
        this.targetProfile = builder.targetProfile;
    }

    static Builder builder(final IntegrationFormat targetFormat) {
        if (targetFormat == null) {
            throw new IllegalArgumentException("Product workflow target format must not be null.");
        }
        return new Builder(targetFormat);
    }

    public IntegrationFormat targetFormat() {
        return targetFormat;
    }

    public ProductWorkflowStatus status() {
        return status;
    }

    public boolean isSuccess() {
        return status == ProductWorkflowStatus.COMPLETED;
    }

    public ValidationResult validation() {
        return validation;
    }

    public ProgramInspectionResult inspection() {
        return inspection;
    }

    public CapabilityPreflightResult preflight() {
        return preflight;
    }

    public ResourceEstimate resources() {
        return resources;
    }

    public ProgramTimeline timeline() {
        return timeline;
    }

    public SimulationResult simulation() {
        return simulation;
    }

    public CompilerResult compiler() {
        return compiler;
    }

    public boolean hasBackendSubmission() {
        return backendSubmission != null;
    }

    public BackendSubmissionResult backendSubmission() {
        if (backendSubmission == null) {
            throw new IllegalStateException("Product workflow report does not contain backend submission.");
        }
        return backendSubmission;
    }

    public boolean hasBackendExecution() {
        return backendExecution != null;
    }

    public BackendExecutionResult backendExecution() {
        if (backendExecution == null) {
            throw new IllegalStateException("Product workflow report does not contain backend execution.");
        }
        return backendExecution;
    }

    public IntegrationCapabilityProfile targetProfile() {
        return targetProfile;
    }

    static final class Builder {

        private final IntegrationFormat targetFormat;
        private ProductWorkflowStatus status;
        private ValidationResult validation;
        private ProgramInspectionResult inspection;
        private CapabilityPreflightResult preflight;
        private ResourceEstimate resources;
        private ProgramTimeline timeline;
        private SimulationResult simulation;
        private CompilerResult compiler;
        private BackendSubmissionResult backendSubmission;
        private BackendExecutionResult backendExecution;
        private IntegrationCapabilityProfile targetProfile;

        private Builder(final IntegrationFormat targetFormat) {
            this.targetFormat = targetFormat;
            this.status = ProductWorkflowStatus.COMPLETED;
        }

        Builder status(final ProductWorkflowStatus status) {
            if (status == null) {
                throw new IllegalArgumentException("Product workflow status must not be null.");
            }
            this.status = status;
            return this;
        }

        Builder validation(final ValidationResult validation) {
            this.validation = validation;
            return this;
        }

        Builder inspection(final ProgramInspectionResult inspection) {
            this.inspection = inspection;
            return this;
        }

        Builder preflight(final CapabilityPreflightResult preflight) {
            this.preflight = preflight;
            return this;
        }

        Builder resources(final ResourceEstimate resources) {
            this.resources = resources;
            return this;
        }

        Builder timeline(final ProgramTimeline timeline) {
            this.timeline = timeline;
            return this;
        }

        Builder simulation(final SimulationResult simulation) {
            this.simulation = simulation;
            return this;
        }

        Builder compiler(final CompilerResult compiler) {
            this.compiler = compiler;
            return this;
        }

        Builder backendSubmission(final BackendSubmissionResult backendSubmission) {
            this.backendSubmission = backendSubmission;
            return this;
        }

        Builder backendExecution(final BackendExecutionResult backendExecution) {
            this.backendExecution = backendExecution;
            return this;
        }

        Builder targetProfile(final IntegrationCapabilityProfile targetProfile) {
            this.targetProfile = targetProfile;
            return this;
        }

        ProductWorkflowReport build() {
            return new ProductWorkflowReport(this);
        }
    }
}
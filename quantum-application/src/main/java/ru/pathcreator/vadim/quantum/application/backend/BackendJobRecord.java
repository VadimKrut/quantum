/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.backend;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BackendJobRecord {

    private final BackendJobId trackingId;
    private final BackendDescriptor backendDescriptor;
    private final BackendJobId backendJobId;
    private final BackendJobStatus status;
    private final Instant submittedAt;
    private final Instant updatedAt;
    private final Map<String, String> queueMetadata;
    private final Map<String, String> costMetadata;
    private final Map<String, String> providerMetadata;
    private final List<BackendDiagnostic> diagnostics;
    private final BackendExecutionResult executionResult;

    private BackendJobRecord(
        final BackendJobId trackingId,
        final BackendDescriptor backendDescriptor,
        final BackendJobId backendJobId,
        final BackendJobStatus status,
        final Instant submittedAt,
        final Instant updatedAt,
        final Map<String, String> queueMetadata,
        final Map<String, String> costMetadata,
        final Map<String, String> providerMetadata,
        final List<BackendDiagnostic> diagnostics,
        final BackendExecutionResult executionResult
    ) {
        if (trackingId == null) {
            throw new IllegalArgumentException("Backend job tracking id must not be null.");
        }
        if (backendDescriptor == null) {
            throw new IllegalArgumentException("Backend job descriptor must not be null.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Backend job status must not be null.");
        }
        if (submittedAt == null) {
            throw new IllegalArgumentException("Backend job submitted time must not be null.");
        }
        if (updatedAt == null) {
            throw new IllegalArgumentException("Backend job updated time must not be null.");
        }
        if (queueMetadata == null) {
            throw new IllegalArgumentException("Backend job queue metadata must not be null.");
        }
        if (costMetadata == null) {
            throw new IllegalArgumentException("Backend job cost metadata must not be null.");
        }
        if (providerMetadata == null) {
            throw new IllegalArgumentException("Backend job provider metadata must not be null.");
        }
        if (diagnostics == null) {
            throw new IllegalArgumentException("Backend job diagnostics must not be null.");
        }
        this.trackingId = trackingId;
        this.backendDescriptor = backendDescriptor;
        this.backendJobId = backendJobId;
        this.status = status;
        this.submittedAt = submittedAt;
        this.updatedAt = updatedAt;
        this.queueMetadata = Map.copyOf(queueMetadata);
        this.costMetadata = Map.copyOf(costMetadata);
        this.providerMetadata = Map.copyOf(providerMetadata);
        this.diagnostics = List.copyOf(diagnostics);
        this.executionResult = executionResult;
    }

    public static BackendJobRecord rejected(
        final BackendJobId trackingId,
        final BackendDescriptor backendDescriptor,
        final Instant submittedAt,
        final List<BackendDiagnostic> diagnostics
    ) {
        return new BackendJobRecord(
            trackingId,
            backendDescriptor,
            null,
            BackendJobStatus.FAILED,
            submittedAt,
            submittedAt,
            Map.of(),
            Map.of(),
            Map.of(),
            diagnostics,
            null
        );
    }

    public static BackendJobRecord accepted(
        final BackendJobId trackingId,
        final BackendDescriptor backendDescriptor,
        final BackendSubmissionResult submission,
        final BackendStatusResult statusResult,
        final BackendExecutionResult executionResult
    ) {
        if (submission == null) {
            throw new IllegalArgumentException("Backend job submission result must not be null.");
        }
        if (statusResult == null) {
            throw new IllegalArgumentException("Backend job status result must not be null.");
        }
        final ArrayList<BackendDiagnostic> diagnostics = new ArrayList<>();
        diagnostics.addAll(submission.diagnostics());
        diagnostics.addAll(statusResult.diagnostics());
        if (executionResult != null) {
            diagnostics.addAll(executionResult.diagnostics());
        }
        return new BackendJobRecord(
            trackingId,
            backendDescriptor,
            submission.jobId(),
            statusResult.status(),
            statusResult.submittedAt(),
            statusResult.updatedAt(),
            statusResult.queueMetadata(),
            statusResult.costMetadata(),
            executionResult == null
                ? Map.of()
                : executionResult.providerMetadata(),
            diagnostics,
            executionResult
        );
    }

    public BackendJobId trackingId() {
        return trackingId;
    }

    public BackendDescriptor backendDescriptor() {
        return backendDescriptor;
    }

    public boolean hasBackendJobId() {
        return backendJobId != null;
    }

    public BackendJobId backendJobId() {
        if (backendJobId == null) {
            throw new IllegalStateException("Backend job record does not contain backend job id.");
        }
        return backendJobId;
    }

    public BackendJobStatus status() {
        return status;
    }

    public Instant submittedAt() {
        return submittedAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Map<String, String> queueMetadata() {
        return queueMetadata;
    }

    public Map<String, String> costMetadata() {
        return costMetadata;
    }

    public Map<String, String> providerMetadata() {
        return providerMetadata;
    }

    public List<BackendDiagnostic> diagnostics() {
        return diagnostics;
    }

    public boolean hasExecutionResult() {
        return executionResult != null;
    }

    public BackendExecutionResult executionResult() {
        if (executionResult == null) {
            throw new IllegalStateException("Backend job record does not contain execution result.");
        }
        return executionResult;
    }

    public boolean isAccepted() {
        return backendJobId != null && !hasErrors();
    }

    public boolean hasErrors() {
        for (BackendDiagnostic diagnostic : diagnostics) {
            if (diagnostic.isError()) {
                return true;
            }
        }
        return false;
    }
}
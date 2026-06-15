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
import java.util.List;
import java.util.Map;

public final class BackendStatusResult {

    private final BackendJobId jobId;
    private final BackendJobStatus status;
    private final Instant submittedAt;
    private final Instant updatedAt;
    private final Map<String, String> queueMetadata;
    private final Map<String, String> costMetadata;
    private final List<BackendDiagnostic> diagnostics;

    public BackendStatusResult(
        final BackendJobId jobId,
        final BackendJobStatus status,
        final Instant submittedAt,
        final Instant updatedAt,
        final Map<String, String> queueMetadata,
        final Map<String, String> costMetadata,
        final List<BackendDiagnostic> diagnostics
    ) {
        if (jobId == null) {
            throw new IllegalArgumentException("Backend status job id must not be null.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Backend status must not be null.");
        }
        if (submittedAt == null) {
            throw new IllegalArgumentException("Backend status submitted time must not be null.");
        }
        if (updatedAt == null) {
            throw new IllegalArgumentException("Backend status updated time must not be null.");
        }
        if (queueMetadata == null) {
            throw new IllegalArgumentException("Backend queue metadata must not be null.");
        }
        if (costMetadata == null) {
            throw new IllegalArgumentException("Backend cost metadata must not be null.");
        }
        if (diagnostics == null) {
            throw new IllegalArgumentException("Backend status diagnostics must not be null.");
        }
        this.jobId = jobId;
        this.status = status;
        this.submittedAt = submittedAt;
        this.updatedAt = updatedAt;
        this.queueMetadata = Map.copyOf(queueMetadata);
        this.costMetadata = Map.copyOf(costMetadata);
        this.diagnostics = List.copyOf(diagnostics);
    }

    public BackendJobId jobId() {
        return jobId;
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

    public List<BackendDiagnostic> diagnostics() {
        return diagnostics;
    }
}
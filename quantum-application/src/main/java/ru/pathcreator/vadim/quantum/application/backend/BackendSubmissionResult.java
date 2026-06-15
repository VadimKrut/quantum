/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.backend;

import java.util.List;

public final class BackendSubmissionResult {

    private final BackendJobId jobId;
    private final BackendJobStatus status;
    private final List<BackendDiagnostic> diagnostics;

    private BackendSubmissionResult(
        final BackendJobId jobId,
        final BackendJobStatus status,
        final List<BackendDiagnostic> diagnostics
    ) {
        if (status == null) {
            throw new IllegalArgumentException("Backend submission status must not be null.");
        }
        if (diagnostics == null) {
            throw new IllegalArgumentException("Backend submission diagnostics must not be null.");
        }
        this.jobId = jobId;
        this.status = status;
        this.diagnostics = List.copyOf(diagnostics);
    }

    public static BackendSubmissionResult accepted(
        final BackendJobId jobId,
        final BackendJobStatus status,
        final List<BackendDiagnostic> diagnostics
    ) {
        if (jobId == null) {
            throw new IllegalArgumentException("Accepted backend submission job id must not be null.");
        }
        return new BackendSubmissionResult(
            jobId,
            status,
            diagnostics
        );
    }

    public static BackendSubmissionResult rejected(final List<BackendDiagnostic> diagnostics) {
        return new BackendSubmissionResult(
            null,
            BackendJobStatus.FAILED,
            diagnostics
        );
    }

    public boolean isAccepted() {
        return jobId != null && !hasErrors();
    }

    public BackendJobId jobId() {
        if (jobId == null) {
            throw new IllegalStateException("Backend submission result does not contain job id.");
        }
        return jobId;
    }

    public BackendJobStatus status() {
        return status;
    }

    public List<BackendDiagnostic> diagnostics() {
        return diagnostics;
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
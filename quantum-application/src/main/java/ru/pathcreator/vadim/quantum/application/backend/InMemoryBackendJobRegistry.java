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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

public final class InMemoryBackendJobRegistry {

    private final ConcurrentHashMap<BackendJobId, BackendJobRecord> recordsByTrackingId;
    private final List<BackendJobId> order;

    public InMemoryBackendJobRegistry() {
        this.recordsByTrackingId = new ConcurrentHashMap<>();
        this.order = Collections.synchronizedList(new ArrayList<>());
    }

    public BackendJobRecord submit(
        final QuantumBackend backend,
        final QuantumProgram program,
        final BackendJobOptions options
    ) {
        if (backend == null) {
            throw new IllegalArgumentException("Backend job registry backend must not be null.");
        }
        if (program == null) {
            throw new IllegalArgumentException("Backend job registry program must not be null.");
        }
        if (options == null) {
            throw new IllegalArgumentException("Backend job registry options must not be null.");
        }
        final BackendJobId trackingId = BackendJobId.random();
        final Instant submittedAt = Instant.now();
        final BackendSubmissionResult submission = backend.submit(
            program,
            options
        );
        final BackendJobRecord record;
        if (!submission.isAccepted()) {
            record = BackendJobRecord.rejected(
                trackingId,
                backend.descriptor(),
                submittedAt,
                submission.diagnostics()
            );
        } else {
            final BackendStatusResult statusResult = backend.status(submission.jobId());
            final BackendExecutionResult executionResult = finished(statusResult.status())
                ? backend.result(submission.jobId())
                : null;
            record = BackendJobRecord.accepted(
                trackingId,
                backend.descriptor(),
                submission,
                statusResult,
                executionResult
            );
        }
        recordsByTrackingId.put(
            trackingId,
            record
        );
        order.add(trackingId);
        return record;
    }

    public BackendJobRecord record(final BackendJobId trackingId) {
        if (trackingId == null) {
            throw new IllegalArgumentException("Backend job tracking id must not be null.");
        }
        final BackendJobRecord record = recordsByTrackingId.get(trackingId);
        if (record == null) {
            throw new IllegalArgumentException("Backend job tracking id was not found: " + trackingId + ".");
        }
        return record;
    }

    public BackendJobHistory history() {
        final ArrayList<BackendJobRecord> records = new ArrayList<>();
        synchronized (order) {
            for (BackendJobId trackingId : order) {
                final BackendJobRecord record = recordsByTrackingId.get(trackingId);
                if (record != null) {
                    records.add(record);
                }
            }
        }
        return new BackendJobHistory(records);
    }

    public void clear() {
        recordsByTrackingId.clear();
        order.clear();
    }

    private static boolean finished(final BackendJobStatus status) {
        return status == BackendJobStatus.COMPLETED
            || status == BackendJobStatus.FAILED
            || status == BackendJobStatus.CANCELLED;
    }
}
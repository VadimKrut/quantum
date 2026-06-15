/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.benchmark;

public final class BenchmarkStageResult {

    private final String stage;
    private final boolean success;
    private final String status;
    private final int warmupIterations;
    private final int measurementIterations;
    private final long minNanos;
    private final long maxNanos;
    private final long averageNanos;
    private final long memoryDeltaBytes;

    public BenchmarkStageResult(
        final String stage,
        final boolean success,
        final String status,
        final int warmupIterations,
        final int measurementIterations,
        final long minNanos,
        final long maxNanos,
        final long averageNanos,
        final long memoryDeltaBytes
    ) {
        if (
            stage == null
            || stage.isBlank()
        ) {
            throw new IllegalArgumentException("Benchmark stage name must not be blank.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Benchmark stage status must not be null.");
        }
        if (warmupIterations < 0) {
            throw new IllegalArgumentException("Benchmark warmup iterations must not be negative.");
        }
        if (measurementIterations <= 0) {
            throw new IllegalArgumentException("Benchmark measurement iterations must be positive.");
        }
        this.stage = stage;
        this.success = success;
        this.status = status;
        this.warmupIterations = warmupIterations;
        this.measurementIterations = measurementIterations;
        this.minNanos = minNanos;
        this.maxNanos = maxNanos;
        this.averageNanos = averageNanos;
        this.memoryDeltaBytes = memoryDeltaBytes;
    }

    public String stage() {
        return stage;
    }

    public boolean isSuccess() {
        return success;
    }

    public String status() {
        return status;
    }

    public int warmupIterations() {
        return warmupIterations;
    }

    public int measurementIterations() {
        return measurementIterations;
    }

    public long minNanos() {
        return minNanos;
    }

    public long maxNanos() {
        return maxNanos;
    }

    public long averageNanos() {
        return averageNanos;
    }

    public long memoryDeltaBytes() {
        return memoryDeltaBytes;
    }
}
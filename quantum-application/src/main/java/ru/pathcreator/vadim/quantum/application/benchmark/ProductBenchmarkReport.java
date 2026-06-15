/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.benchmark;

import java.util.List;

import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;

public final class ProductBenchmarkReport {

    private final IntegrationFormat inputFormat;
    private final IntegrationFormat targetFormat;
    private final List<BenchmarkStageResult> stages;

    public ProductBenchmarkReport(
        final IntegrationFormat inputFormat,
        final IntegrationFormat targetFormat,
        final List<BenchmarkStageResult> stages
    ) {
        if (targetFormat == null) {
            throw new IllegalArgumentException("Benchmark target format must not be null.");
        }
        if (stages == null) {
            throw new IllegalArgumentException("Benchmark stages must not be null.");
        }
        this.inputFormat = inputFormat;
        this.targetFormat = targetFormat;
        this.stages = List.copyOf(stages);
    }

    public boolean hasInputFormat() {
        return inputFormat != null;
    }

    public IntegrationFormat inputFormat() {
        if (inputFormat == null) {
            throw new IllegalStateException("Benchmark report does not contain input format.");
        }
        return inputFormat;
    }

    public IntegrationFormat targetFormat() {
        return targetFormat;
    }

    public List<BenchmarkStageResult> stages() {
        return stages;
    }

    public int stageCount() {
        return stages.size();
    }

    public boolean isSuccess() {
        for (BenchmarkStageResult stage : stages) {
            if (!stage.isSuccess()) {
                return false;
            }
        }
        return true;
    }

    public long totalAverageNanos() {
        long total = 0L;
        for (BenchmarkStageResult stage : stages) {
            total = safeAdd(
                total,
                stage.averageNanos()
            );
        }
        return total;
    }

    private static long safeAdd(
        final long left,
        final long right
    ) {
        if (right > 0L && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }
}
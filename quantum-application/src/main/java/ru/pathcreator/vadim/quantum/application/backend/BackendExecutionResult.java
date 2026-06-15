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
import java.util.Map;

import ru.pathcreator.vadim.quantum.application.compiler.CompilerResult;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;

public final class BackendExecutionResult {

    private final BackendJobId jobId;
    private final BackendJobStatus status;
    private final CompilerResult compilerResult;
    private final SimulationResult simulationResult;
    private final Map<String, String> providerMetadata;
    private final List<BackendDiagnostic> diagnostics;

    public BackendExecutionResult(
        final BackendJobId jobId,
        final BackendJobStatus status,
        final CompilerResult compilerResult,
        final SimulationResult simulationResult,
        final Map<String, String> providerMetadata,
        final List<BackendDiagnostic> diagnostics
    ) {
        if (jobId == null) {
            throw new IllegalArgumentException("Backend execution job id must not be null.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Backend execution status must not be null.");
        }
        if (providerMetadata == null) {
            throw new IllegalArgumentException("Backend provider metadata must not be null.");
        }
        if (diagnostics == null) {
            throw new IllegalArgumentException("Backend execution diagnostics must not be null.");
        }
        this.jobId = jobId;
        this.status = status;
        this.compilerResult = compilerResult;
        this.simulationResult = simulationResult;
        this.providerMetadata = Map.copyOf(providerMetadata);
        this.diagnostics = List.copyOf(diagnostics);
    }

    public BackendJobId jobId() {
        return jobId;
    }

    public BackendJobStatus status() {
        return status;
    }

    public boolean hasCompilerResult() {
        return compilerResult != null;
    }

    public CompilerResult compilerResult() {
        if (compilerResult == null) {
            throw new IllegalStateException("Backend execution result does not contain compiler result.");
        }
        return compilerResult;
    }

    public boolean hasSimulationResult() {
        return simulationResult != null;
    }

    public SimulationResult simulationResult() {
        if (simulationResult == null) {
            throw new IllegalStateException("Backend execution result does not contain simulation result.");
        }
        return simulationResult;
    }

    public Map<String, String> providerMetadata() {
        return providerMetadata;
    }

    public List<BackendDiagnostic> diagnostics() {
        return diagnostics;
    }

    public boolean isSuccess() {
        return status == BackendJobStatus.COMPLETED && !hasErrors();
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
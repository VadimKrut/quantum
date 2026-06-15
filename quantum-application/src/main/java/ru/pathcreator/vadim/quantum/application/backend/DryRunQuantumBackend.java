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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ru.pathcreator.vadim.quantum.application.compiler.CompilerResult;
import ru.pathcreator.vadim.quantum.application.compiler.QuantumCompiler;
import ru.pathcreator.vadim.quantum.application.integration.contract.QuantumExporter;
import ru.pathcreator.vadim.quantum.application.simulation.engine.QuantumSimulator;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

public final class DryRunQuantumBackend implements QuantumBackend {

    private final BackendDescriptor descriptor;
    private final QuantumExporter exporter;
    private final ConcurrentHashMap<BackendJobId, JobRecord> jobs = new ConcurrentHashMap<>();

    public DryRunQuantumBackend(
        final String backendId,
        final String displayName,
        final String version,
        final QuantumExporter exporter
    ) {
        if (exporter == null) {
            throw new IllegalArgumentException("Dry-run backend exporter must not be null.");
        }
        this.exporter = exporter;
        this.descriptor = BackendDescriptor.of(
            backendId,
            displayName,
            version,
            exporter.capabilityProfile(),
            EnumSet.of(
                BackendCapability.DRY_RUN,
                BackendCapability.SHOT_COUNTS,
                BackendCapability.STATE_VECTOR_RESULT,
                BackendCapability.CANCELLATION,
                BackendCapability.QUEUE_METADATA
            ),
            Map.of("executionMode", "dry-run")
        );
    }

    @Override
    public BackendDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public BackendSubmissionResult submit(
        final QuantumProgram program,
        final BackendJobOptions options
    ) {
        if (program == null) {
            throw new IllegalArgumentException("Backend submission program must not be null.");
        }
        if (options == null) {
            throw new IllegalArgumentException("Backend job options must not be null.");
        }
        final BackendPreflightResult preflight = new BackendPreflightChecker().check(
            program,
            descriptor
        );
        if (!preflight.isSuccess()) {
            return BackendSubmissionResult.rejected(preflight.diagnostics());
        }
        final BackendJobId jobId = BackendJobId.random();
        final JobRecord record = JobRecord.queued(
            jobId,
            options
        );
        jobs.put(
            jobId,
            record
        );
        executeSynchronously(
            record,
            program,
            options
        );
        return BackendSubmissionResult.accepted(
            jobId,
            record.status,
            record.diagnostics
        );
    }

    @Override
    public BackendStatusResult status(final BackendJobId jobId) {
        final JobRecord record = jobRecord(jobId);
        return record.statusResult();
    }

    @Override
    public BackendExecutionResult result(final BackendJobId jobId) {
        final JobRecord record = jobRecord(jobId);
        if (
            record.status != BackendJobStatus.COMPLETED
            && record.status != BackendJobStatus.FAILED
            && record.status != BackendJobStatus.CANCELLED
        ) {
            return new BackendExecutionResult(
                jobId,
                record.status,
                record.compilerResult,
                record.simulationResult,
                record.providerMetadata,
                List.of(BackendDiagnostic.error(
                    BackendDiagnosticCode.JOB_NOT_FINISHED,
                    "Backend job has not finished yet."
                ))
            );
        }
        return record.executionResult();
    }

    @Override
    public BackendStatusResult cancel(final BackendJobId jobId) {
        final JobRecord record = jobRecord(jobId);
        if (
            record.status == BackendJobStatus.COMPLETED
            || record.status == BackendJobStatus.FAILED
        ) {
            record.diagnostics.add(BackendDiagnostic.warning(
                BackendDiagnosticCode.JOB_NOT_CANCELLABLE,
                "Finished dry-run job cannot be cancelled."
            ));
            record.updatedAt = Instant.now();
            return record.statusResult();
        }
        record.status = BackendJobStatus.CANCELLED;
        record.updatedAt = Instant.now();
        return record.statusResult();
    }

    private void executeSynchronously(
        final JobRecord record,
        final QuantumProgram program,
        final BackendJobOptions options
    ) {
        record.status = BackendJobStatus.RUNNING;
        record.updatedAt = Instant.now();
        final CompilerResult compilerResult = new QuantumCompiler().compile(
            program,
            exporter,
            options.compilerOptions()
        );
        record.compilerResult = compilerResult;
        if (!compilerResult.isSuccess()) {
            record.status = BackendJobStatus.FAILED;
            record.diagnostics.add(BackendDiagnostic.error(
                BackendDiagnosticCode.COMPILATION_FAILED,
                "Dry-run backend compilation failed."
            ));
            record.updatedAt = Instant.now();
            return;
        }
        final SimulationResult simulationResult = new QuantumSimulator().simulate(
            compilerResult.transformedProgram(),
            options.simulationOptions()
        );
        record.simulationResult = simulationResult;
        if (!simulationResult.isSuccess()) {
            record.status = BackendJobStatus.FAILED;
            record.diagnostics.add(BackendDiagnostic.error(
                BackendDiagnosticCode.SIMULATION_FAILED,
                "Dry-run backend simulation failed."
            ));
            record.updatedAt = Instant.now();
            return;
        }
        record.status = BackendJobStatus.COMPLETED;
        record.updatedAt = Instant.now();
    }

    private JobRecord jobRecord(final BackendJobId jobId) {
        if (jobId == null) {
            throw new IllegalArgumentException("Backend job id must not be null.");
        }
        final JobRecord record = jobs.get(jobId);
        if (record == null) {
            throw new IllegalArgumentException("Backend job was not found: " + jobId + ".");
        }
        return record;
    }

    private static final class JobRecord {

        private final BackendJobId jobId;
        private BackendJobStatus status;
        private final Instant submittedAt;
        private Instant updatedAt;
        private final Map<String, String> queueMetadata;
        private final Map<String, String> costMetadata;
        private final Map<String, String> providerMetadata;
        private final ArrayList<BackendDiagnostic> diagnostics;
        private CompilerResult compilerResult;
        private SimulationResult simulationResult;

        private JobRecord(
            final BackendJobId jobId,
            final BackendJobOptions options
        ) {
            this.jobId = jobId;
            this.status = BackendJobStatus.QUEUED;
            this.submittedAt = Instant.now();
            this.updatedAt = submittedAt;
            this.queueMetadata = Map.of(
                "position",
                "0",
                "mode",
                "synchronous-dry-run"
            );
            this.costMetadata = Map.of();
            this.providerMetadata = Map.of(
                "authenticationProfile",
                options.hasAuthenticationProfile()
                    ? options.authenticationProfile()
                    : "none"
            );
            this.diagnostics = new ArrayList<>();
        }

        private static JobRecord queued(
            final BackendJobId jobId,
            final BackendJobOptions options
        ) {
            return new JobRecord(
                jobId,
                options
            );
        }

        private BackendStatusResult statusResult() {
            return new BackendStatusResult(
                jobId,
                status,
                submittedAt,
                updatedAt,
                queueMetadata,
                costMetadata,
                diagnostics
            );
        }

        private BackendExecutionResult executionResult() {
            return new BackendExecutionResult(
                jobId,
                status,
                compilerResult,
                simulationResult,
                providerMetadata,
                diagnostics
            );
        }
    }
}
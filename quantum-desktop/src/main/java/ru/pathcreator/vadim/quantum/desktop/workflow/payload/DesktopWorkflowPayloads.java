/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.workflow.payload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import ru.pathcreator.vadim.quantum.api.Quantum;
import ru.pathcreator.vadim.quantum.application.audit.ProductAuditReport;
import ru.pathcreator.vadim.quantum.application.backend.BackendJobOptions;
import ru.pathcreator.vadim.quantum.application.backend.BackendJobRecord;
import ru.pathcreator.vadim.quantum.application.backend.InMemoryBackendJobRegistry;
import ru.pathcreator.vadim.quantum.application.benchmark.ProductBenchmarkOptions;
import ru.pathcreator.vadim.quantum.application.benchmark.ProductBenchmarkReport;
import ru.pathcreator.vadim.quantum.application.compatibility.ProductCompatibilityMatrix;
import ru.pathcreator.vadim.quantum.application.compiler.CompilerResult;
import ru.pathcreator.vadim.quantum.application.compiler.CompilerStageRecord;
import ru.pathcreator.vadim.quantum.application.distribution.ProductDistributionBundleResult;
import ru.pathcreator.vadim.quantum.application.doctor.ProductDoctorReport;
import ru.pathcreator.vadim.quantum.application.inspection.ProgramInspectionResult;
import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightResult;
import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnostic;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.integration.result.ImportResult;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrWriteResult;
import ru.pathcreator.vadim.quantum.application.regression.CorpusRegressionCase;
import ru.pathcreator.vadim.quantum.application.regression.CorpusRegressionReport;
import ru.pathcreator.vadim.quantum.application.readiness.ReleaseReadinessReport;
import ru.pathcreator.vadim.quantum.application.report.ProductReportBundleResult;
import ru.pathcreator.vadim.quantum.application.resource.ResourceEstimate;
import ru.pathcreator.vadim.quantum.application.simulation.options.SimulationOptions;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.application.transformation.TransformationDiagnostic;
import ru.pathcreator.vadim.quantum.application.transformation.TransformationResult;
import ru.pathcreator.vadim.quantum.application.transformation.TransformationStepRecord;
import ru.pathcreator.vadim.quantum.application.verification.CrossFormatVerificationReport;
import ru.pathcreator.vadim.quantum.application.visualization.CircuitTimeline;
import ru.pathcreator.vadim.quantum.application.visualization.CircuitTimelineStep;
import ru.pathcreator.vadim.quantum.application.visualization.ProgramTimeline;
import ru.pathcreator.vadim.quantum.application.workflow.ProductWorkflowOptions;
import ru.pathcreator.vadim.quantum.application.workflow.ProductWorkflowReport;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationError;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

/**
 * Рендерит structured desktop workflow payloads без выполнения workflow actions.
 */
public final class DesktopWorkflowPayloads {

    private DesktopWorkflowPayloads() {
    }

    public static int failedBenchmarkStages(final ProductBenchmarkReport report) {
        int failures = 0;
        for (int i = 0; i < report.stages().size(); i++) {
            if (!report.stages().get(i).isSuccess()) {
                failures++;
            }
        }
        return failures;
    }

    public static int failedCompatibilityTargets(final ProductCompatibilityMatrix matrix) {
        int failures = 0;
        for (int i = 0; i < matrix.targets().size(); i++) {
            if (!matrix.targets().get(i).isSuccess()) {
                failures++;
            }
        }
        return failures;
    }

    public static int failedCrossFormatTargets(final CrossFormatVerificationReport report) {
        int failures = 0;
        for (int i = 0; i < report.targets().size(); i++) {
            if (!report.targets().get(i).isSuccess()) {
                failures++;
            }
        }
        return failures;
    }

    public static Map<String, Object> importPayload(final ImportResult result) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "format",
            result.format().name()
        );
        payload.put(
            "success",
            result.isSuccess()
        );
        payload.put(
            "hasProgram",
            result.hasProgram()
        );
        payload.put(
            "diagnosticCount",
            result.diagnosticCount()
        );
        payload.put(
            "diagnostics",
            integrationDiagnosticsPayload(result.diagnostics())
        );
        return payload;
    }

    public static Map<String, Object> validationPayload(final ValidationResult result) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "valid",
            result.isValid()
        );
        payload.put(
            "errorCount",
            result.errorCount()
        );
        final ArrayList<Map<String, Object>> errors = new ArrayList<>(result.errors().size());
        for (int i = 0; i < result.errors().size(); i++) {
            final ValidationError error = result.errors().get(i);
            final Map<String, Object> item = new LinkedHashMap<>();
            item.put(
                "code",
                error.code().name()
            );
            item.put(
                "message",
                error.message()
            );
            item.put(
                "circuitIndex",
                error.circuitIndex()
            );
            item.put(
                "operationIndex",
                error.operationIndex()
            );
            errors.add(item);
        }
        payload.put(
            "errors",
            errors
        );
        return payload;
    }

    public static Map<String, Object> inspectionPayload(final ProgramInspectionResult result) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "computationModel",
            result.computationModel().name()
        );
        payload.put(
            "circuitCount",
            result.circuitCount()
        );
        payload.put(
            "qubitCount",
            result.qubitCount()
        );
        payload.put(
            "classicalBitCount",
            result.classicalBitCount()
        );
        payload.put(
            "operationCount",
            result.operationCount()
        );
        payload.put(
            "gateCount",
            result.gateCount()
        );
        payload.put(
            "measurementCount",
            result.measurementCount()
        );
        payload.put(
            "diagnosticCount",
            result.diagnosticCount()
        );
        payload.put(
            "gateHistogram",
            result.gateHistogram()
        );
        return payload;
    }

    public static Map<String, Object> preflightPayload(final CapabilityPreflightResult result) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "success",
            result.isSuccess()
        );
        payload.put(
            "status",
            result.status().name()
        );
        payload.put(
            "requiresLowering",
            result.requiresLowering()
        );
        payload.put(
            "diagnosticCount",
            result.diagnostics().size()
        );
        payload.put(
            "diagnostics",
            integrationDiagnosticsPayload(result.diagnostics())
        );
        return payload;
    }

    public static Map<String, Object> simulationPayload(final SimulationResult result) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "success",
            result.isSuccess()
        );
        payload.put(
            "qubitCount",
            result.qubitCount()
        );
        payload.put(
            "classicalBitCount",
            result.classicalBitCount()
        );
        payload.put(
            "shots",
            result.shots()
        );
        payload.put(
            "counts",
            result.counts()
        );
        payload.put(
            "stateVectorSize",
            result.stateVector().size()
        );
        payload.put(
            "diagnosticCount",
            result.diagnostics().size()
        );
        return payload;
    }

    public static Map<String, Object> resourcePayload(final ResourceEstimate result) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "computationModel",
            result.computationModel().name()
        );
        payload.put(
            "circuitCount",
            result.circuitCount()
        );
        payload.put(
            "qubitCount",
            result.qubitCount()
        );
        payload.put(
            "classicalBitCount",
            result.classicalBitCount()
        );
        payload.put(
            "operationCount",
            result.operationCount()
        );
        payload.put(
            "gateCount",
            result.gateCount()
        );
        payload.put(
            "measurementCount",
            result.measurementCount()
        );
        payload.put(
            "estimatedStateVectorAmplitudes",
            result.estimatedStateVectorAmplitudes()
        );
        payload.put(
            "estimatedStateVectorBytes",
            result.estimatedStateVectorBytes()
        );
        payload.put(
            "localSimulationFeasible",
            result.isLocalSimulationFeasible()
        );
        payload.put(
            "gateHistogram",
            result.gateHistogram()
        );
        return payload;
    }

    public static Map<String, Object> timelinePayload(final ProgramTimeline timeline) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        final ArrayList<Map<String, Object>> circuits = new ArrayList<>(timeline.circuits().size());
        for (int i = 0; i < timeline.circuits().size(); i++) {
            final Map<String, Object> circuitPayload = new LinkedHashMap<>();
            final CircuitTimeline circuit = timeline.circuits().get(i);
            circuitPayload.put(
                "circuitName",
                circuit.circuitName()
            );
            circuitPayload.put(
                "quantumWires",
                circuit.quantumWires()
            );
            circuitPayload.put(
                "classicalWires",
                circuit.classicalWires()
            );
            circuitPayload.put(
                "stepCount",
                circuit.steps().size()
            );
            final ArrayList<Map<String, Object>> steps = new ArrayList<>(circuit.steps().size());
            for (int j = 0; j < circuit.steps().size(); j++) {
                final CircuitTimelineStep step = circuit.steps().get(j);
                final Map<String, Object> stepPayload = new LinkedHashMap<>();
                stepPayload.put(
                    "operationIndex",
                    step.operationIndex()
                );
                stepPayload.put(
                    "operationKind",
                    step.operationKind().name()
                );
                stepPayload.put(
                    "label",
                    step.label()
                );
                stepPayload.put(
                    "quantumWires",
                    step.quantumWires()
                );
                stepPayload.put(
                    "classicalWires",
                    step.classicalWires()
                );
                stepPayload.put(
                    "staticPlacement",
                    step.isStaticPlacement()
                );
                steps.add(stepPayload);
            }
            circuitPayload.put(
                "steps",
                steps
            );
            circuits.add(circuitPayload);
        }
        payload.put(
            "circuits",
            circuits
        );
        return payload;
    }

    public static Map<String, Object> compilerPayload(
        final CompilerResult result,
        final String content
    ) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "success",
            result.isSuccess()
        );
        payload.put(
            "status",
            result.status().name()
        );
        payload.put(
            "format",
            result.format().name()
        );
        payload.put(
            "stageCount",
            result.stageRecords().size()
        );
        payload.put(
            "hasExport",
            result.hasExportResult()
        );
        payload.put(
            "content",
            content
        );
        final ArrayList<Map<String, Object>> stages = new ArrayList<>(result.stageRecords().size());
        for (int i = 0; i < result.stageRecords().size(); i++) {
            final CompilerStageRecord stage = result.stageRecords().get(i);
            final Map<String, Object> item = new LinkedHashMap<>();
            item.put(
                "stage",
                stage.stage().name()
            );
            item.put(
                "status",
                stage.status().name()
            );
            item.put(
                "elapsedNanos",
                stage.elapsedNanos()
            );
            item.put(
                "message",
                stage.message()
            );
            stages.add(item);
        }
        payload.put(
            "stages",
            stages
        );
        return payload;
    }

    public static Map<String, Object> transformationPayload(final TransformationResult result) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "success",
            result.isSuccess()
        );
        payload.put(
            "appliedStepCount",
            result.appliedSteps().size()
        );
        payload.put(
            "skippedStepCount",
            result.skippedSteps().size()
        );
        payload.put(
            "diagnosticCount",
            result.diagnostics().size()
        );
        payload.put(
            "appliedSteps",
            transformationStepsPayload(result.appliedSteps())
        );
        payload.put(
            "skippedSteps",
            transformationStepsPayload(result.skippedSteps())
        );
        payload.put(
            "diagnostics",
            transformationDiagnosticsPayload(result.diagnostics())
        );
        return payload;
    }

    private static List<Map<String, Object>> transformationStepsPayload(
        final List<TransformationStepRecord> records
    ) {
        final ArrayList<Map<String, Object>> payload = new ArrayList<>(records.size());
        for (int i = 0; i < records.size(); i++) {
            final TransformationStepRecord record = records.get(i);
            final Map<String, Object> item = new LinkedHashMap<>();
            item.put(
                "step",
                record.step().name()
            );
            item.put(
                "message",
                record.message()
            );
            payload.add(item);
        }
        return payload;
    }

    private static List<Map<String, Object>> transformationDiagnosticsPayload(
        final List<TransformationDiagnostic> diagnostics
    ) {
        final ArrayList<Map<String, Object>> payload = new ArrayList<>(diagnostics.size());
        for (int i = 0; i < diagnostics.size(); i++) {
            final TransformationDiagnostic diagnostic = diagnostics.get(i);
            final Map<String, Object> item = new LinkedHashMap<>();
            item.put(
                "severity",
                diagnostic.severity().name()
            );
            item.put(
                "code",
                diagnostic.code().name()
            );
            item.put(
                "step",
                diagnostic.step().name()
            );
            item.put(
                "message",
                diagnostic.message()
            );
            item.put(
                "circuitIndex",
                diagnostic.circuitIndex()
            );
            item.put(
                "operationIndex",
                diagnostic.operationIndex()
            );
            item.put(
                "targetName",
                diagnostic.hasTargetName() ? diagnostic.targetName() : null
            );
            payload.add(item);
        }
        return payload;
    }

    public static Map<String, Object> workflowPayload(final ProductWorkflowReport report) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "success",
            report.isSuccess()
        );
        payload.put(
            "status",
            report.status().name()
        );
        payload.put(
            "targetFormat",
            report.targetFormat().name()
        );
        payload.put(
            "validationValid",
            report.validation() != null && report.validation().isValid()
        );
        payload.put(
            "inspectionOperationCount",
            report.inspection() == null ? 0 : report.inspection().operationCount()
        );
        payload.put(
            "preflightStatus",
            report.preflight() == null ? "NONE" : report.preflight().status().name()
        );
        payload.put(
            "simulationSuccess",
            report.simulation() != null && report.simulation().isSuccess()
        );
        payload.put(
            "compilerStatus",
            report.compiler() == null ? "NONE" : report.compiler().status().name()
        );
        payload.put(
            "backendExecution",
            report.hasBackendExecution()
        );
        return payload;
    }

    public static Map<String, Object> benchmarkPayload(final ProductBenchmarkReport report) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "success",
            report.isSuccess()
        );
        payload.put(
            "targetFormat",
            report.targetFormat().name()
        );
        payload.put(
            "stageCount",
            report.stageCount()
        );
        payload.put(
            "failedStageCount",
            failedBenchmarkStages(report)
        );
        payload.put(
            "totalAverageNanos",
            report.totalAverageNanos()
        );
        final ArrayList<Map<String, Object>> stages = new ArrayList<>(report.stages().size());
        for (int i = 0; i < report.stages().size(); i++) {
            final Map<String, Object> stagePayload = new LinkedHashMap<>();
            stagePayload.put(
                "stage",
                report.stages().get(i).stage()
            );
            stagePayload.put(
                "success",
                report.stages().get(i).isSuccess()
            );
            stagePayload.put(
                "status",
                report.stages().get(i).status()
            );
            stagePayload.put(
                "averageNanos",
                report.stages().get(i).averageNanos()
            );
            stages.add(stagePayload);
        }
        payload.put(
            "stages",
            stages
        );
        return payload;
    }

    public static Map<String, Object> compatibilityPayload(final ProductCompatibilityMatrix matrix) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "success",
            matrix.isSuccess()
        );
        payload.put(
            "validationValid",
            matrix.validation().isValid()
        );
        payload.put(
            "simulationSuccess",
            matrix.simulation().isSuccess()
        );
        payload.put(
            "targetCount",
            matrix.targets().size()
        );
        payload.put(
            "failedTargetCount",
            failedCompatibilityTargets(matrix)
        );
        final ArrayList<Map<String, Object>> targets = new ArrayList<>(matrix.targets().size());
        for (int i = 0; i < matrix.targets().size(); i++) {
            final Map<String, Object> targetPayload = new LinkedHashMap<>();
            targetPayload.put(
                "targetFormat",
                matrix.targets().get(i).targetFormat().name()
            );
            targetPayload.put(
                "status",
                matrix.targets().get(i).status().name()
            );
            targetPayload.put(
                "preflightStatus",
                matrix.targets().get(i).preflightStatus().name()
            );
            targetPayload.put(
                "compilerStatus",
                matrix.targets().get(i).compilerStatus().name()
            );
            targetPayload.put(
                "workflowStatus",
                matrix.targets().get(i).workflowStatus().name()
            );
            targets.add(targetPayload);
        }
        payload.put(
            "targets",
            targets
        );
        return payload;
    }

    public static Map<String, Object> crossFormatPayload(final CrossFormatVerificationReport report) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "success",
            report.isSuccess()
        );
        payload.put(
            "inputFormat",
            report.inputFormat().name()
        );
        payload.put(
            "importSuccess",
            report.importSuccess()
        );
        payload.put(
            "targetCount",
            report.targets().size()
        );
        payload.put(
            "failedTargetCount",
            failedCrossFormatTargets(report)
        );
        final ArrayList<Map<String, Object>> targets = new ArrayList<>(report.targets().size());
        for (int i = 0; i < report.targets().size(); i++) {
            final Map<String, Object> targetPayload = new LinkedHashMap<>();
            targetPayload.put(
                "targetFormat",
                report.targets().get(i).targetFormat().name()
            );
            targetPayload.put(
                "success",
                report.targets().get(i).isSuccess()
            );
            targetPayload.put(
                "simulationEquivalent",
                report.targets().get(i).simulationEquivalent()
            );
            targets.add(targetPayload);
        }
        payload.put(
            "targets",
            targets
        );
        return payload;
    }

    public static Map<String, Object> backendJobRecordPayload(final BackendJobRecord record) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "success",
            record.isAccepted()
        );
        payload.put(
            "trackingId",
            record.trackingId().value()
        );
        payload.put(
            "backendId",
            record.backendDescriptor().backendId()
        );
        payload.put(
            "status",
            record.status().name()
        );
        payload.put(
            "hasBackendJobId",
            record.hasBackendJobId()
        );
        payload.put(
            "hasExecutionResult",
            record.hasExecutionResult()
        );
        return payload;
    }

    public static Map<String, Object> corpusRegressionPayload(final CorpusRegressionReport report) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "success",
            report.isSuccess()
        );
        payload.put(
            "caseCount",
            report.caseCount()
        );
        payload.put(
            "failureCount",
            report.failureCount()
        );
        final ArrayList<Map<String, Object>> cases = new ArrayList<>(report.cases().size());
        for (int i = 0; i < report.cases().size(); i++) {
            final Map<String, Object> casePayload = new LinkedHashMap<>();
            casePayload.put(
                "name",
                report.cases().get(i).name()
            );
            casePayload.put(
                "inputFormat",
                report.cases().get(i).inputFormat().name()
            );
            casePayload.put(
                "success",
                report.cases().get(i).isSuccess()
            );
            cases.add(casePayload);
        }
        payload.put(
            "cases",
            cases
        );
        return payload;
    }

    public static Map<String, Object> readinessPayload(final ReleaseReadinessReport report) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "status",
            report.status().name()
        );
        payload.put(
            "ready",
            report.isReady()
        );
        payload.put(
            "acceptable",
            report.isAcceptable()
        );
        payload.put(
            "failedCheckCount",
            report.failedCheckCount()
        );
        payload.put(
            "warningCheckCount",
            report.warningCheckCount()
        );
        payload.put(
            "targetProfileCount",
            report.targetProfiles().size()
        );
        payload.put(
            "corpusRegression",
            corpusRegressionPayload(report.corpusRegression())
        );
        return payload;
    }

    public static Map<String, Object> doctorPayload(final ProductDoctorReport report) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "projectRoot",
            report.projectRoot().toString()
        );
        payload.put(
            "status",
            report.status().name()
        );
        payload.put(
            "healthy",
            report.isHealthy()
        );
        payload.put(
            "acceptable",
            report.isAcceptable()
        );
        payload.put(
            "checkCount",
            report.checkCount()
        );
        payload.put(
            "failedCheckCount",
            report.failedCheckCount()
        );
        payload.put(
            "warningCheckCount",
            report.warningCheckCount()
        );
        return payload;
    }

    public static Map<String, Object> reportBundlePayload(final ProductReportBundleResult result) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "outputDirectory",
            result.outputDirectory().toString()
        );
        payload.put(
            "auditJsonPath",
            result.auditJsonPath().toString()
        );
        payload.put(
            "summaryMarkdownPath",
            result.summaryMarkdownPath().toString()
        );
        payload.put(
            "manifestPath",
            result.manifestPath().toString()
        );
        return payload;
    }

    public static Map<String, Object> distributionPayload(final ProductDistributionBundleResult result) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "outputDirectory",
            result.outputDirectory().toString()
        );
        payload.put(
            "archivePath",
            result.archivePath().toString()
        );
        payload.put(
            "archiveSha256",
            result.archiveSha256()
        );
        payload.put(
            "manifestPath",
            result.manifestPath().toString()
        );
        payload.put(
            "packagedFileCount",
            result.packagedFileCount()
        );
        return payload;
    }

    public static Map<String, Object> auditPayload(final ProductAuditReport audit) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "projectRoot",
            audit.projectRoot().toString()
        );
        payload.put(
            "status",
            audit.status().name()
        );
        payload.put(
            "acceptable",
            audit.isAcceptable()
        );
        payload.put(
            "failedCheckCount",
            audit.failedCheckCount()
        );
        payload.put(
            "warningCheckCount",
            audit.warningCheckCount()
        );
        payload.put(
            "doctorStatus",
            audit.doctor().status().name()
        );
        payload.put(
            "readinessStatus",
            audit.readiness().status().name()
        );
        return payload;
    }

    public static List<Map<String, Object>> integrationDiagnosticsPayload(
        final List<IntegrationDiagnostic> diagnostics
    ) {
        final ArrayList<Map<String, Object>> payload = new ArrayList<>(diagnostics.size());
        for (int i = 0; i < diagnostics.size(); i++) {
            final IntegrationDiagnostic diagnostic = diagnostics.get(i);
            final Map<String, Object> item = new LinkedHashMap<>();
            item.put(
                "severity",
                diagnostic.severity().name()
            );
            item.put(
                "code",
                diagnostic.code().name()
            );
            item.put(
                "message",
                diagnostic.message()
            );
            item.put(
                "line",
                diagnostic.line()
            );
            item.put(
                "column",
                diagnostic.column()
            );
            payload.add(item);
        }
        return payload;
    }
}
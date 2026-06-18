/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.workflow;

import static ru.pathcreator.vadim.quantum.desktop.workflow.payload.DesktopWorkflowPayloads.auditPayload;
import static ru.pathcreator.vadim.quantum.desktop.workflow.payload.DesktopWorkflowPayloads.backendJobRecordPayload;
import static ru.pathcreator.vadim.quantum.desktop.workflow.payload.DesktopWorkflowPayloads.benchmarkPayload;
import static ru.pathcreator.vadim.quantum.desktop.workflow.payload.DesktopWorkflowPayloads.compatibilityPayload;
import static ru.pathcreator.vadim.quantum.desktop.workflow.payload.DesktopWorkflowPayloads.compilerPayload;
import static ru.pathcreator.vadim.quantum.desktop.workflow.payload.DesktopWorkflowPayloads.corpusRegressionPayload;
import static ru.pathcreator.vadim.quantum.desktop.workflow.payload.DesktopWorkflowPayloads.crossFormatPayload;
import static ru.pathcreator.vadim.quantum.desktop.workflow.payload.DesktopWorkflowPayloads.distributionPayload;
import static ru.pathcreator.vadim.quantum.desktop.workflow.payload.DesktopWorkflowPayloads.doctorPayload;
import static ru.pathcreator.vadim.quantum.desktop.workflow.payload.DesktopWorkflowPayloads.failedBenchmarkStages;
import static ru.pathcreator.vadim.quantum.desktop.workflow.payload.DesktopWorkflowPayloads.failedCompatibilityTargets;
import static ru.pathcreator.vadim.quantum.desktop.workflow.payload.DesktopWorkflowPayloads.failedCrossFormatTargets;
import static ru.pathcreator.vadim.quantum.desktop.workflow.payload.DesktopWorkflowPayloads.importPayload;
import static ru.pathcreator.vadim.quantum.desktop.workflow.payload.DesktopWorkflowPayloads.inspectionPayload;
import static ru.pathcreator.vadim.quantum.desktop.workflow.payload.DesktopWorkflowPayloads.preflightPayload;
import static ru.pathcreator.vadim.quantum.desktop.workflow.payload.DesktopWorkflowPayloads.readinessPayload;
import static ru.pathcreator.vadim.quantum.desktop.workflow.payload.DesktopWorkflowPayloads.reportBundlePayload;
import static ru.pathcreator.vadim.quantum.desktop.workflow.payload.DesktopWorkflowPayloads.resourcePayload;
import static ru.pathcreator.vadim.quantum.desktop.workflow.payload.DesktopWorkflowPayloads.simulationPayload;
import static ru.pathcreator.vadim.quantum.desktop.workflow.payload.DesktopWorkflowPayloads.timelinePayload;
import static ru.pathcreator.vadim.quantum.desktop.workflow.payload.DesktopWorkflowPayloads.validationPayload;
import static ru.pathcreator.vadim.quantum.desktop.workflow.payload.DesktopWorkflowPayloads.workflowPayload;

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
 * Сервис desktop workflow поверх публичного Quantum API.
 */
public final class DesktopWorkflowService {

    private final ObjectMapper objectMapper;

    public DesktopWorkflowService() {
        this.objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    }

    public DesktopWorkflowResult importProgram(
        final IntegrationFormat inputFormat,
        final String source
    ) {
        final ImportResult imported = importSource(
            inputFormat,
            source
        );
        return result(
            DesktopAction.IMPORT,
            imported.isSuccess(),
            imported.isSuccess() ? "IMPORTED" : "IMPORT_FAILED",
            "Diagnostics: " + imported.diagnosticCount(),
            importPayload(imported)
        );
    }

    public DesktopWorkflowResult validate(
        final IntegrationFormat inputFormat,
        final String source
    ) {
        final ImportResult imported = importSource(
            inputFormat,
            source
        );
        if (!imported.isSuccess()) {
            return importFailure(
                DesktopAction.VALIDATE,
                imported
            );
        }
        final ValidationResult validation = Quantum.validate(imported.program());
        return result(
            DesktopAction.VALIDATE,
            validation.isValid(),
            validation.isValid() ? "VALID" : "INVALID",
            "Errors: " + validation.errorCount(),
            validationPayload(validation)
        );
    }

    public DesktopWorkflowResult inspect(
        final IntegrationFormat inputFormat,
        final String source,
        final IntegrationFormat targetFormat
    ) {
        final ImportResult imported = importSource(
            inputFormat,
            source
        );
        if (!imported.isSuccess()) {
            return importFailure(
                DesktopAction.INSPECT,
                imported
            );
        }
        final ProgramInspectionResult inspection = Quantum.inspect(
            imported.program(),
            List.of(Quantum.targetProfile(targetFormat))
        );
        return result(
            DesktopAction.INSPECT,
            inspection.diagnosticCount() == 0,
            "INSPECTED",
            "Circuits: " + inspection.circuitCount()
                + ", operations: " + inspection.operationCount()
                + ", diagnostics: " + inspection.diagnosticCount(),
            inspectionPayload(inspection)
        );
    }

    public DesktopWorkflowResult resources(
        final IntegrationFormat inputFormat,
        final String source,
        final int maxQubits
    ) {
        final ImportResult imported = importSource(
            inputFormat,
            source
        );
        if (!imported.isSuccess()) {
            return importFailure(
                DesktopAction.RESOURCES,
                imported
            );
        }
        final ResourceEstimate estimate = Quantum.estimateResources(
            imported.program(),
            maxQubits
        );
        return result(
            DesktopAction.RESOURCES,
            true,
            estimate.isLocalSimulationFeasible() ? "FEASIBLE" : "RESOURCE_HEAVY",
            "Qubits: " + estimate.qubitCount()
                + ", operations: " + estimate.operationCount()
                + ", memory bytes: " + estimate.estimatedStateVectorBytes(),
            resourcePayload(estimate)
        );
    }

    public DesktopWorkflowResult circuit(
        final IntegrationFormat inputFormat,
        final String source
    ) {
        final ImportResult imported = importSource(
            inputFormat,
            source
        );
        if (!imported.isSuccess()) {
            return importFailure(
                DesktopAction.CIRCUIT,
                imported
            );
        }
        final ProgramTimeline timeline = Quantum.timeline(imported.program());
        return result(
            DesktopAction.CIRCUIT,
            true,
            "TIMELINE_BUILT",
            "Circuits: " + timeline.circuits().size(),
            timelinePayload(timeline)
        );
    }

    public DesktopWorkflowResult preflight(
        final IntegrationFormat inputFormat,
        final String source,
        final IntegrationFormat targetFormat
    ) {
        final ImportResult imported = importSource(
            inputFormat,
            source
        );
        if (!imported.isSuccess()) {
            return importFailure(
                DesktopAction.PREFLIGHT,
                imported
            );
        }
        final CapabilityPreflightResult preflight = Quantum.preflight(
            targetFormat,
            imported.program()
        );
        return result(
            DesktopAction.PREFLIGHT,
            preflight.isSuccess(),
            preflight.status().name(),
            "Requires lowering: " + preflight.requiresLowering()
                + ", diagnostics: " + preflight.diagnostics().size(),
            preflightPayload(preflight)
        );
    }

    public DesktopWorkflowResult simulate(
        final IntegrationFormat inputFormat,
        final String source,
        final int shots,
        final long seed
    ) {
        final ImportResult imported = importSource(
            inputFormat,
            source
        );
        if (!imported.isSuccess()) {
            return importFailure(
                DesktopAction.SIMULATE,
                imported
            );
        }
        final SimulationOptions options = SimulationOptions.builder()
            .shots(shots)
            .seed(seed)
            .build();
        final SimulationResult simulation = Quantum.simulate(
            imported.program(),
            options
        );
        return result(
            DesktopAction.SIMULATE,
            simulation.isSuccess(),
            simulation.isSuccess() ? "SIMULATED" : "SIMULATION_FAILED",
            "Qubits: " + simulation.qubitCount()
                + ", shots: " + simulation.shots()
                + ", states: " + simulation.stateVector().size(),
            simulationPayload(simulation)
        );
    }

    public DesktopWorkflowResult compile(
        final IntegrationFormat inputFormat,
        final String source,
        final IntegrationFormat targetFormat
    ) {
        return compile(
            inputFormat,
            source,
            targetFormat,
            DesktopExecutionOptions.defaults()
        );
    }

    public DesktopWorkflowResult compile(
        final IntegrationFormat inputFormat,
        final String source,
        final IntegrationFormat targetFormat,
        final DesktopExecutionOptions options
    ) {
        final ImportResult imported = importSource(
            inputFormat,
            source
        );
        if (!imported.isSuccess()) {
            return importFailure(
                DesktopAction.COMPILE,
                imported
            );
        }
        final CompilerResult compiled = Quantum.compile(
            targetFormat,
            imported.program(),
            options.compilerOptions()
        );
        final String content = compiled.hasExportResult()
            && compiled.exportResult().hasContent()
                ? compiled.exportResult().content()
                : "";
        return result(
            DesktopAction.COMPILE,
            compiled.isSuccess(),
            compiled.status().name(),
            "Stages: " + compiled.stageRecords().size(),
            compilerPayload(
                compiled,
                content
            ),
            content
        );
    }

    public DesktopWorkflowResult workflow(
        final IntegrationFormat inputFormat,
        final String source,
        final IntegrationFormat targetFormat,
        final int shots,
        final long seed
    ) {
        return workflow(
            inputFormat,
            source,
            targetFormat,
            shots,
            seed,
            DesktopExecutionOptions.defaults()
        );
    }

    public DesktopWorkflowResult workflow(
        final IntegrationFormat inputFormat,
        final String source,
        final IntegrationFormat targetFormat,
        final int shots,
        final long seed,
        final DesktopExecutionOptions options
    ) {
        final ImportResult imported = importSource(
            inputFormat,
            source
        );
        if (!imported.isSuccess()) {
            return importFailure(
                DesktopAction.WORKFLOW,
                imported
            );
        }
        final ProductWorkflowReport report = Quantum.runProductWorkflow(
            targetFormat,
            imported.program(),
            options.workflowOptions(
                shots,
                seed
            )
        );
        return result(
            DesktopAction.WORKFLOW,
            report.isSuccess(),
            report.status().name(),
            "Validation: " + (report.validation() != null)
                + ", compile: " + (report.compiler() != null)
                + ", backend: " + report.hasBackendExecution(),
            workflowPayload(report)
        );
    }

    public DesktopWorkflowResult benchmark(
        final IntegrationFormat inputFormat,
        final String source,
        final IntegrationFormat targetFormat,
        final int shots,
        final long seed
    ) {
        return benchmark(
            inputFormat,
            source,
            targetFormat,
            shots,
            seed,
            DesktopExecutionOptions.defaults()
        );
    }

    public DesktopWorkflowResult benchmark(
        final IntegrationFormat inputFormat,
        final String source,
        final IntegrationFormat targetFormat,
        final int shots,
        final long seed,
        final DesktopExecutionOptions options
    ) {
        final ProductBenchmarkReport report = Quantum.benchmarkExternal(
            inputFormat,
            source,
            targetFormat,
            benchmarkOptions(
                shots,
                seed,
                options
            )
        );
        return result(
            DesktopAction.BENCHMARK,
            report.isSuccess(),
            report.isSuccess() ? "SUCCESS" : "FAILED",
            "Stages: " + report.stages().size()
                + ", failures: " + failedBenchmarkStages(report),
            benchmarkPayload(report)
        );
    }

    public DesktopWorkflowResult compatibility(
        final IntegrationFormat inputFormat,
        final String source,
        final int shots,
        final long seed
    ) {
        return compatibility(
            inputFormat,
            source,
            shots,
            seed,
            DesktopExecutionOptions.defaults()
        );
    }

    public DesktopWorkflowResult compatibility(
        final IntegrationFormat inputFormat,
        final String source,
        final int shots,
        final long seed,
        final DesktopExecutionOptions options
    ) {
        final ImportResult imported = importSource(
            inputFormat,
            source
        );
        if (!imported.isSuccess()) {
            return importFailure(
                DesktopAction.COMPATIBILITY,
                imported
            );
        }
        final ProductCompatibilityMatrix matrix = Quantum.compatibilityMatrix(
            imported.program(),
            options.workflowOptions(
                shots,
                seed
            )
        );
        return result(
            DesktopAction.COMPATIBILITY,
            matrix.isSuccess(),
            matrix.isSuccess() ? "SUCCESS" : "FAILED",
            "Targets: " + matrix.targets().size()
                + ", failures: " + failedCompatibilityTargets(matrix),
            compatibilityPayload(matrix)
        );
    }

    public DesktopWorkflowResult verifyCrossFormat(
        final IntegrationFormat inputFormat,
        final String source,
        final int shots,
        final long seed
    ) {
        final CrossFormatVerificationReport report = Quantum.verifyCrossFormat(
            inputFormat,
            source,
            simulationOptions(
                shots,
                seed
            )
        );
        return result(
            DesktopAction.CROSS_FORMAT,
            report.isSuccess(),
            report.isSuccess() ? "SUCCESS" : "FAILED",
            "Targets: " + report.targets().size()
                + ", failures: " + failedCrossFormatTargets(report),
            crossFormatPayload(report)
        );
    }

    public DesktopWorkflowResult backendDryRun(
        final IntegrationFormat inputFormat,
        final String source,
        final IntegrationFormat targetFormat,
        final int shots,
        final long seed
    ) {
        final ImportResult imported = importSource(
            inputFormat,
            source
        );
        if (!imported.isSuccess()) {
            return importFailure(
                DesktopAction.BACKEND_DRY_RUN,
                imported
            );
        }
        final InMemoryBackendJobRegistry registry = Quantum.backendJobRegistry();
        final BackendJobRecord record = registry.submit(
            Quantum.dryRunBackend(targetFormat),
            imported.program(),
            BackendJobOptions.builder()
                .simulationOptions(simulationOptions(
                    shots,
                    seed
                ))
                .metadata(
                    "origin",
                    "quantum-desktop"
                )
                .build()
        );
        return result(
            DesktopAction.BACKEND_DRY_RUN,
            record.isAccepted(),
            record.status().name(),
            "Tracking id: " + record.trackingId().value()
                + ", history: " + registry.history().count(),
            backendJobRecordPayload(record)
        );
    }

    public DesktopWorkflowResult json(
        final IntegrationFormat inputFormat,
        final String source
    ) {
        final ImportResult imported = importSource(
            inputFormat,
            source
        );
        if (!imported.isSuccess()) {
            return importFailure(
                DesktopAction.JSON,
                imported
            );
        }
        final QuantumIrWriteResult written = Quantum.writeJson(imported.program());
        return DesktopWorkflowResult.of(
            DesktopAction.JSON,
            written.isSuccess(),
            written.isSuccess() ? "JSON_WRITTEN" : "JSON_FAILED",
            "Diagnostics: " + written.diagnostics().size(),
            written.hasContent() ? written.content() : render(written)
        );
    }

    public DesktopWorkflowResult corpusRegression(
        final Path corpusRoot,
        final int shots,
        final long seed
    ) throws IOException {
        return corpusRegression(
            corpusRoot,
            shots,
            seed,
            DesktopExecutionOptions.defaults()
        );
    }

    public DesktopWorkflowResult corpusRegression(
        final Path corpusRoot,
        final int shots,
        final long seed,
        final DesktopExecutionOptions options
    ) throws IOException {
        final CorpusRegressionReport report = Quantum.runCorpusRegression(
            collectCorpusCases(corpusRoot),
            options.workflowOptions(
                shots,
                seed
            )
        );
        return result(
            DesktopAction.REGRESSION,
            report.isSuccess(),
            report.isSuccess() ? "SUCCESS" : "FAILED",
            "Cases: " + report.caseCount()
                + ", failures: " + report.failureCount(),
            corpusRegressionPayload(report)
        );
    }

    public DesktopWorkflowResult releaseReadiness(
        final Path corpusRoot,
        final IntegrationFormat outputFormat,
        final int shots,
        final long seed
    ) throws IOException {
        return releaseReadiness(
            corpusRoot,
            outputFormat,
            shots,
            seed,
            DesktopExecutionOptions.defaults()
        );
    }

    public DesktopWorkflowResult releaseReadiness(
        final Path corpusRoot,
        final IntegrationFormat outputFormat,
        final int shots,
        final long seed,
        final DesktopExecutionOptions options
    ) throws IOException {
        final ReleaseReadinessReport report = Quantum.releaseReadiness(
            collectCorpusCases(corpusRoot),
            outputFormat,
            benchmarkOptions(
                shots,
                seed,
                options
            )
        );
        return result(
            DesktopAction.READINESS,
            report.isAcceptable(),
            report.status().name(),
            "Failed checks: " + report.failedCheckCount()
                + ", warnings: " + report.warningCheckCount(),
            readinessPayload(report)
        );
    }

    public DesktopWorkflowResult doctor(final Path projectRoot) {
        final ProductDoctorReport report = Quantum.productDoctor(projectRoot);
        return result(
            DesktopAction.DOCTOR,
            report.isAcceptable(),
            report.status().name(),
            "Failed checks: " + report.failedCheckCount()
                + ", warnings: " + report.warningCheckCount(),
            doctorPayload(report)
        );
    }

    public DesktopWorkflowResult productAudit(
        final Path projectRoot,
        final Path corpusRoot,
        final IntegrationFormat outputFormat,
        final int shots,
        final long seed
    ) throws IOException {
        return productAudit(
            projectRoot,
            corpusRoot,
            outputFormat,
            shots,
            seed,
            DesktopExecutionOptions.defaults()
        );
    }

    public DesktopWorkflowResult productAudit(
        final Path projectRoot,
        final Path corpusRoot,
        final IntegrationFormat outputFormat,
        final int shots,
        final long seed,
        final DesktopExecutionOptions options
    ) throws IOException {
        final ProductAuditReport audit = Quantum.productAudit(
            projectRoot,
            collectCorpusCases(corpusRoot),
            outputFormat,
            benchmarkOptions(
                shots,
                seed,
                options
            )
        );
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "summary",
            Quantum.productReportSummary(audit)
        );
        payload.put(
            "audit",
            auditPayload(audit)
        );
        return result(
            DesktopAction.PRODUCT_AUDIT,
            audit.isAcceptable(),
            audit.status().name(),
            "Failed checks: " + audit.failedCheckCount()
                + ", warnings: " + audit.warningCheckCount(),
            payload
        );
    }

    public DesktopWorkflowResult productReport(
        final Path projectRoot,
        final Path corpusRoot,
        final Path outputRoot,
        final IntegrationFormat outputFormat,
        final int shots,
        final long seed
    ) throws IOException {
        return productReport(
            projectRoot,
            corpusRoot,
            outputRoot,
            outputFormat,
            shots,
            seed,
            DesktopExecutionOptions.defaults()
        );
    }

    public DesktopWorkflowResult productReport(
        final Path projectRoot,
        final Path corpusRoot,
        final Path outputRoot,
        final IntegrationFormat outputFormat,
        final int shots,
        final long seed,
        final DesktopExecutionOptions options
    ) throws IOException {
        final ProductAuditReport audit = Quantum.productAudit(
            projectRoot,
            collectCorpusCases(corpusRoot),
            outputFormat,
            benchmarkOptions(
                shots,
                seed,
                options
            )
        );
        final String auditJson = render(audit);
        final Path reportDirectory = outputRoot
            .resolve("product-report")
            .toAbsolutePath()
            .normalize();
        final ProductReportBundleResult report = Quantum.writeProductReportBundle(
            reportDirectory,
            audit,
            auditJson
        );
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "summary",
            Quantum.productReportSummary(audit)
        );
        payload.put(
            "report",
            reportBundlePayload(report)
        );
        return result(
            DesktopAction.PRODUCT_REPORT,
            true,
            "WRITTEN",
            "Output: " + report.outputDirectory(),
            payload
        );
    }

    public DesktopWorkflowResult productDistribution(
        final Path projectRoot,
        final Path outputRoot
    ) throws IOException {
        final Path distributionDirectory = outputRoot
            .resolve("quantum-distribution")
            .toAbsolutePath()
            .normalize();
        final ProductDistributionBundleResult bundle = Quantum.writeProductDistributionBundle(
            distributionDirectory,
            projectRoot
        );
        return result(
            DesktopAction.PRODUCT_DISTRIBUTION,
            true,
            "WRITTEN",
            "Output: " + bundle.outputDirectory(),
            distributionPayload(bundle)
        );
    }

    static SimulationOptions simulationOptions(
        final int shots,
        final long seed
    ) {
        return SimulationOptions.builder()
            .shots(shots)
            .seed(seed)
            .captureStateVector(false)
            .build();
    }

    private static ProductWorkflowOptions workflowOptions(
        final int shots,
        final long seed
    ) {
        return DesktopExecutionOptions.defaults().workflowOptions(
            shots,
            seed
        );
    }

    private static ProductBenchmarkOptions benchmarkOptions(
        final int shots,
        final long seed
    ) {
        return benchmarkOptions(
            shots,
            seed,
            DesktopExecutionOptions.defaults()
        );
    }

    private static ProductBenchmarkOptions benchmarkOptions(
        final int shots,
        final long seed,
        final DesktopExecutionOptions options
    ) {
        return ProductBenchmarkOptions.builder()
            .warmupIterations(0)
            .measurementIterations(1)
            .workflowOptions(options.workflowOptions(
                shots,
                seed
            ))
            .build();
    }

    private ImportResult importSource(
        final IntegrationFormat inputFormat,
        final String source
    ) {
        if (inputFormat == null) {
            throw new IllegalArgumentException("Desktop input format must not be null.");
        }
        if (
            source == null
            || source.isBlank()
        ) {
            throw new IllegalArgumentException("Desktop source must not be blank.");
        }
        return Quantum.importProgram(
            inputFormat,
            source
        );
    }

    private List<CorpusRegressionCase> collectCorpusCases(final Path corpusRoot) throws IOException {
        if (corpusRoot == null) {
            throw new IllegalArgumentException("Desktop corpus root must not be null.");
        }
        final Path root = resolveDirectory(corpusRoot);
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("Desktop corpus root must point to a directory.");
        }
        final List<Path> files;
        try (java.util.stream.Stream<Path> stream = Files.walk(root)) {
            files = stream
                .filter(Files::isRegularFile)
                .filter(DesktopWorkflowService::isSupportedCorpusFile)
                .sorted(Comparator.comparing(Path::toString))
                .toList();
        }
        final List<CorpusRegressionCase> cases = new ArrayList<>(files.size());
        for (int i = 0; i < files.size(); i++) {
            final Path file = files.get(i);
            cases.add(CorpusRegressionCase.of(
                root.relativize(file).toString(),
                Files.readString(file),
                Quantum.integration(formatForFile(file))
            ));
        }
        return cases;
    }

    private static boolean isSupportedCorpusFile(final Path path) {
        final String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(".qasm")
            || fileName.endsWith(".qasm3")
            || fileName.endsWith(".quil");
    }

    private static IntegrationFormat formatForFile(final Path path) throws IOException {
        final String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".quil")) {
            return IntegrationFormat.QUIL;
        }
        if (
            fileName.endsWith(".qasm3")
            || Files.readString(path).contains("OPENQASM 3")
        ) {
            return IntegrationFormat.OPENQASM_3;
        }
        return IntegrationFormat.OPENQASM_2;
    }

    private static Path resolveDirectory(final Path path) {
        final Path direct = path.toAbsolutePath().normalize();
        if (Files.isDirectory(direct)) {
            return direct;
        }
        final Path parentRelative = Path.of("..")
            .resolve(path)
            .toAbsolutePath()
            .normalize();
        if (Files.isDirectory(parentRelative)) {
            return parentRelative;
        }
        return direct;
    }

    private DesktopWorkflowResult importFailure(
        final DesktopAction action,
        final ImportResult imported
    ) {
        return result(
            action,
            false,
            "IMPORT_FAILED",
            "Diagnostics: " + imported.diagnosticCount(),
            importPayload(imported)
        );
    }

    private DesktopWorkflowResult result(
        final DesktopAction action,
        final boolean success,
        final String status,
        final String summary,
        final Object payload
    ) {
        return result(
            action,
            success,
            status,
            summary,
            payload,
            ""
        );
    }

    private DesktopWorkflowResult result(
        final DesktopAction action,
        final boolean success,
        final String status,
        final String summary,
        final Object payload,
        final String generatedContent
    ) {
        return DesktopWorkflowResult.of(
            action,
            success,
            status,
            summary,
            render(payload),
            generatedContent
        );
    }

    private String render(final Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (final JsonProcessingException exception) {
            throw new IllegalStateException(
                "Desktop workflow payload cannot be rendered as JSON.",
                exception
            );
        }
    }
}
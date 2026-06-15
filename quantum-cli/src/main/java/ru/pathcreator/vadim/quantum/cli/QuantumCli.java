/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import ru.pathcreator.vadim.quantum.api.Quantum;
import ru.pathcreator.vadim.quantum.application.audit.ProductAuditReport;
import ru.pathcreator.vadim.quantum.application.backend.BackendDiagnostic;
import ru.pathcreator.vadim.quantum.application.backend.BackendJobOptions;
import ru.pathcreator.vadim.quantum.application.backend.BackendJobRecord;
import ru.pathcreator.vadim.quantum.application.backend.InMemoryBackendJobRegistry;
import ru.pathcreator.vadim.quantum.application.backend.QuantumBackend;
import ru.pathcreator.vadim.quantum.application.compiler.CompilerOptions;
import ru.pathcreator.vadim.quantum.application.compiler.CompilerResult;
import ru.pathcreator.vadim.quantum.application.compiler.CompilerStageRecord;
import ru.pathcreator.vadim.quantum.application.benchmark.BenchmarkStageResult;
import ru.pathcreator.vadim.quantum.application.benchmark.ProductBenchmarkOptions;
import ru.pathcreator.vadim.quantum.application.benchmark.ProductBenchmarkReport;
import ru.pathcreator.vadim.quantum.application.compatibility.CompatibilityCheckResult;
import ru.pathcreator.vadim.quantum.application.compatibility.ProductCompatibilityMatrix;
import ru.pathcreator.vadim.quantum.application.compatibility.TargetCompatibilityReport;
import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightResult;
import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapabilityProfile;
import ru.pathcreator.vadim.quantum.application.doctor.ProductDoctorCheck;
import ru.pathcreator.vadim.quantum.application.doctor.ProductDoctorReport;
import ru.pathcreator.vadim.quantum.application.distribution.ProductDistributionBundleResult;
import ru.pathcreator.vadim.quantum.application.distribution.ProductDistributionVerificationIssue;
import ru.pathcreator.vadim.quantum.application.distribution.ProductDistributionVerificationResult;
import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnostic;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.integration.result.ExportResult;
import ru.pathcreator.vadim.quantum.application.integration.result.ImportResult;
import ru.pathcreator.vadim.quantum.application.inspection.ProgramInspectionResult;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrReadResult;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrWriteResult;
import ru.pathcreator.vadim.quantum.application.regression.CorpusRegressionCase;
import ru.pathcreator.vadim.quantum.application.regression.CorpusRegressionCaseReport;
import ru.pathcreator.vadim.quantum.application.regression.CorpusRegressionReport;
import ru.pathcreator.vadim.quantum.application.readiness.ReleaseReadinessCheck;
import ru.pathcreator.vadim.quantum.application.readiness.ReleaseReadinessReport;
import ru.pathcreator.vadim.quantum.application.report.ProductReportBundleResult;
import ru.pathcreator.vadim.quantum.application.resource.CircuitResourceEstimate;
import ru.pathcreator.vadim.quantum.application.resource.ResourceEstimate;
import ru.pathcreator.vadim.quantum.application.simulation.diagnostic.SimulationDiagnostic;
import ru.pathcreator.vadim.quantum.application.simulation.options.SimulationOptions;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.application.transformation.TransformationOptions;
import ru.pathcreator.vadim.quantum.application.transformation.TransformationResult;
import ru.pathcreator.vadim.quantum.application.verification.CrossFormatTargetVerification;
import ru.pathcreator.vadim.quantum.application.verification.CrossFormatVerificationReport;
import ru.pathcreator.vadim.quantum.application.visualization.CircuitTimeline;
import ru.pathcreator.vadim.quantum.application.visualization.CircuitTimelineStep;
import ru.pathcreator.vadim.quantum.application.visualization.ProgramTimeline;
import ru.pathcreator.vadim.quantum.application.workflow.ProductWorkflowOptions;
import ru.pathcreator.vadim.quantum.application.workflow.ProductWorkflowReport;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationError;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

public final class QuantumCli {

    public static final int EXIT_SUCCESS = 0;
    public static final int EXIT_WORKFLOW_ERROR = 1;
    public static final int EXIT_USAGE_ERROR = 2;
    public static final int EXIT_INTERNAL_ERROR = 3;

    private static final ObjectMapper JSON = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);

    private QuantumCli() {
    }

    public static int run(
        final String[] args,
        final PrintStream out,
        final PrintStream err
    ) {
        try {
            final CliArguments arguments = CliArguments.parse(args);
            if (arguments.help()) {
                out.println(helpText());
                return EXIT_SUCCESS;
            }
            return execute(
                arguments,
                out,
                err
            );
        } catch (CliUsageException exception) {
            err.println(exception.getMessage());
            err.println("Use `quantum help` for command syntax.");
            return EXIT_USAGE_ERROR;
        } catch (IOException exception) {
            err.println("I/O error: " + exception.getMessage());
            return EXIT_INTERNAL_ERROR;
        } catch (RuntimeException exception) {
            err.println("Internal error: " + exception.getMessage());
            return EXIT_INTERNAL_ERROR;
        }
    }

    private static int execute(
        final CliArguments arguments,
        final PrintStream out,
        final PrintStream err
    ) throws IOException {
        return switch (arguments.command()) {
            case "help" -> {
                out.println(helpText());
                yield EXIT_SUCCESS;
            }
            case "validate" -> validate(arguments, out);
            case "inspect" -> inspect(arguments, out);
            case "resources" -> resources(arguments, out);
            case "circuit" -> circuit(arguments, out);
            case "preflight" -> preflight(arguments, out);
            case "transform" -> transform(arguments, out);
            case "compile" -> compile(arguments, out);
            case "convert" -> convert(arguments, out);
            case "simulate" -> simulate(arguments, out);
            case "backend-dry-run" -> backendDryRun(arguments, out);
            case "workflow" -> workflow(arguments, out);
            case "benchmark" -> benchmark(arguments, out);
            case "compatibility" -> compatibility(arguments, out);
            case "verify-cross-format" -> verifyCrossFormat(arguments, out);
            case "regress-corpus" -> regressCorpus(arguments, out);
            case "release-readiness" -> releaseReadiness(arguments, out);
            case "target-profile" -> targetProfile(arguments, out);
            case "doctor" -> doctor(arguments, out);
            case "product-audit" -> productAudit(arguments, out);
            case "product-report" -> productReport(arguments, out);
            case "product-distribution" -> productDistribution(arguments, out);
            case "product-verify-distribution" -> productVerifyDistribution(arguments, out);
            default -> {
                err.println("Unknown command: " + arguments.command() + ".");
                yield EXIT_USAGE_ERROR;
            }
        };
    }

    private static int validate(
        final CliArguments arguments,
        final PrintStream out
    ) throws IOException {
        final ProgramLoadResult load = loadProgram(arguments);
        if (!load.success()) {
            printResult(
                arguments,
                out,
                Map.of(
                    "command",
                    "validate",
                    "status",
                    "import_failed",
                    "diagnostics",
                    load.diagnostics()
                ),
                "Import failed."
            );
            return EXIT_WORKFLOW_ERROR;
        }
        final ValidationResult validation = Quantum.validate(load.program());
        final Map<String, Object> payload = validationPayload(validation);
        payload.put(
            "command",
            "validate"
        );
        printResult(
            arguments,
            out,
            payload,
            validation.isValid()
                ? "Validation: valid"
                : "Validation: invalid (" + validation.errorCount() + " errors)"
        );
        return validation.isValid() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
    }

    private static int inspect(
        final CliArguments arguments,
        final PrintStream out
    ) throws IOException {
        final ProgramLoadResult load = requireLoadedProgram(arguments);
        if (!load.success()) {
            printImportFailure(
                arguments,
                out,
                "inspect",
                load
            );
            return EXIT_WORKFLOW_ERROR;
        }
        final ProgramInspectionResult inspection = Quantum.inspect(load.program());
        final Map<String, Object> payload = inspectionPayload(inspection);
        payload.put(
            "command",
            "inspect"
        );
        printResult(
            arguments,
            out,
            payload,
            "Inspection: circuits=" + inspection.circuitCount()
                + ", qubits=" + inspection.qubitCount()
                + ", operations=" + inspection.operationCount()
        );
        return EXIT_SUCCESS;
    }

    private static int circuit(
        final CliArguments arguments,
        final PrintStream out
    ) throws IOException {
        final ProgramLoadResult load = requireLoadedProgram(arguments);
        if (!load.success()) {
            printImportFailure(
                arguments,
                out,
                "circuit",
                load
            );
            return EXIT_WORKFLOW_ERROR;
        }
        final ProgramTimeline timeline = Quantum.timeline(load.program());
        final Map<String, Object> payload = timelinePayload(timeline);
        payload.put(
            "command",
            "circuit"
        );
        printResult(
            arguments,
            out,
            payload,
            "Circuit timeline: circuits=" + timeline.circuits().size()
        );
        return EXIT_SUCCESS;
    }

    private static int preflight(
        final CliArguments arguments,
        final PrintStream out
    ) throws IOException {
        final ProgramLoadResult load = requireLoadedProgram(arguments);
        if (!load.success()) {
            printImportFailure(
                arguments,
                out,
                "preflight",
                load
            );
            return EXIT_WORKFLOW_ERROR;
        }
        final IntegrationFormat format = arguments.requiredOutputFormat();
        final CapabilityPreflightResult preflight = Quantum.preflight(
            format,
            load.program()
        );
        final Map<String, Object> payload = preflightPayload(preflight);
        payload.put(
            "command",
            "preflight"
        );
        payload.put(
            "targetFormat",
            format.name()
        );
        printResult(
            arguments,
            out,
            payload,
            "Preflight: " + preflight.status()
        );
        return preflight.isSuccess() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
    }

    private static int resources(
        final CliArguments arguments,
        final PrintStream out
    ) throws IOException {
        final ProgramLoadResult load = requireLoadedProgram(arguments);
        if (!load.success()) {
            printImportFailure(
                arguments,
                out,
                "resources",
                load
            );
            return EXIT_WORKFLOW_ERROR;
        }
        final ResourceEstimate estimate = Quantum.estimateResources(
            load.program(),
            arguments.intOption(
                "max-qubits",
                20
            )
        );
        final Map<String, Object> payload = resourcePayload(estimate);
        payload.put(
            "command",
            "resources"
        );
        printResult(
            arguments,
            out,
            payload,
            "Resources: qubits=" + estimate.qubitCount()
                + ", operations=" + estimate.operationCount()
                + ", stateVectorBytes=" + estimate.estimatedStateVectorBytes()
        );
        return EXIT_SUCCESS;
    }

    private static int transform(
        final CliArguments arguments,
        final PrintStream out
    ) throws IOException {
        final ProgramLoadResult load = requireLoadedProgram(arguments);
        if (!load.success()) {
            printImportFailure(
                arguments,
                out,
                "transform",
                load
            );
            return EXIT_WORKFLOW_ERROR;
        }
        final TransformationResult transform = Quantum.transform(
            load.program(),
            transformationOptions(arguments)
        );
        if (arguments.hasOutputPath()) {
            Quantum.writeJson(
                arguments.outputPath(),
                transform.transformedProgram()
            );
        }
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "command",
            "transform"
        );
        payload.put(
            "success",
            transform.isSuccess()
        );
        payload.put(
            "appliedStepCount",
            transform.appliedSteps().size()
        );
        payload.put(
            "skippedStepCount",
            transform.skippedSteps().size()
        );
        payload.put(
            "diagnosticCount",
            transform.diagnostics().size()
        );
        printResult(
            arguments,
            out,
            payload,
            "Transform: applied=" + transform.appliedSteps().size()
                + ", skipped=" + transform.skippedSteps().size()
        );
        return transform.isSuccess() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
    }

    private static int compile(
        final CliArguments arguments,
        final PrintStream out
    ) throws IOException {
        final ProgramLoadResult load = requireLoadedProgram(arguments);
        if (!load.success()) {
            printImportFailure(
                arguments,
                out,
                "compile",
                load
            );
            return EXIT_WORKFLOW_ERROR;
        }
        final IntegrationFormat format = arguments.requiredOutputFormat();
        final CompilerResult result = Quantum.compile(
            format,
            load.program(),
            compilerOptions(arguments)
        );
        if (
            result.isSuccess()
            && arguments.hasOutputPath()
        ) {
            Files.writeString(
                arguments.outputPath(),
                result.exportResult().content()
            );
        }
        final Map<String, Object> payload = compilerPayload(result);
        payload.put(
            "command",
            "compile"
        );
        printResult(
            arguments,
            out,
            payload,
            "Compile: " + result.status()
        );
        return result.isSuccess() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
    }

    private static int convert(
        final CliArguments arguments,
        final PrintStream out
    ) throws IOException {
        final ProgramLoadResult load = requireLoadedProgram(arguments);
        if (!load.success()) {
            printImportFailure(
                arguments,
                out,
                "convert",
                load
            );
            return EXIT_WORKFLOW_ERROR;
        }
        final String content;
        if (arguments.outputFormatName().equals("json")) {
            final QuantumIrWriteResult write = Quantum.writeJson(load.program());
            if (!write.isSuccess()) {
                printResult(
                    arguments,
                    out,
                    Map.of(
                        "command",
                        "convert",
                        "success",
                        false
                    ),
                    "Convert: failed to write JSON."
                );
                return EXIT_WORKFLOW_ERROR;
            }
            content = write.content();
        } else {
            final IntegrationFormat format = arguments.requiredOutputFormat();
            final ExportResult export = Quantum.exportProgram(
                format,
                load.program()
            );
            if (!export.isSuccess()) {
                final Map<String, Object> payload = new LinkedHashMap<>();
                payload.put(
                    "command",
                    "convert"
                );
                payload.put(
                    "success",
                    false
                );
                payload.put(
                    "diagnostics",
                    diagnosticsPayload(export.diagnostics())
                );
                printResult(
                    arguments,
                    out,
                    payload,
                    "Convert: export failed."
                );
                return EXIT_WORKFLOW_ERROR;
            }
            content = export.content();
        }
        if (arguments.hasOutputPath()) {
            Files.writeString(
                arguments.outputPath(),
                content
            );
            printResult(
                arguments,
                out,
                Map.of(
                    "command",
                    "convert",
                    "success",
                    true,
                    "output",
                    arguments.outputPath().toString()
                ),
                "Convert: wrote " + arguments.outputPath()
            );
        } else {
            out.print(content);
        }
        return EXIT_SUCCESS;
    }

    private static int simulate(
        final CliArguments arguments,
        final PrintStream out
    ) throws IOException {
        final ProgramLoadResult load = requireLoadedProgram(arguments);
        if (!load.success()) {
            printImportFailure(
                arguments,
                out,
                "simulate",
                load
            );
            return EXIT_WORKFLOW_ERROR;
        }
        final SimulationResult simulation = Quantum.simulate(
            load.program(),
            SimulationOptions.builder()
                .shots(arguments.intOption(
                    "shots",
                    1024
                ))
                .seed(arguments.longOption(
                    "seed",
                    1L
                ))
                .maxQubits(arguments.intOption(
                    "max-qubits",
                    20
                ))
                .captureStateVector(arguments.booleanOption(
                    "state-vector",
                    true
                ))
                .build()
        );
        final Map<String, Object> payload = simulationPayload(simulation);
        payload.put(
            "command",
            "simulate"
        );
        printResult(
            arguments,
            out,
            payload,
            "Simulation: " + (simulation.isSuccess() ? "success" : "failed")
                + ", shots=" + simulation.shots()
                + ", outcomes=" + simulation.counts().size()
        );
        return simulation.isSuccess() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
    }

    private static int backendDryRun(
        final CliArguments arguments,
        final PrintStream out
    ) throws IOException {
        final ProgramLoadResult load = requireLoadedProgram(arguments);
        if (!load.success()) {
            printImportFailure(
                arguments,
                out,
                "backend-dry-run",
                load
            );
            return EXIT_WORKFLOW_ERROR;
        }
        final IntegrationFormat format = arguments.requiredOutputFormat();
        final QuantumBackend backend = Quantum.dryRunBackend(format);
        final InMemoryBackendJobRegistry registry = Quantum.backendJobRegistry();
        final BackendJobRecord record = registry.submit(
            backend,
            load.program(),
            BackendJobOptions.builder()
                .simulationOptions(SimulationOptions.builder()
                    .shots(arguments.intOption(
                        "shots",
                        1024
                    ))
                    .seed(arguments.longOption(
                        "seed",
                        1L
                    ))
                    .maxQubits(arguments.intOption(
                        "max-qubits",
                        20
                    ))
                    .captureStateVector(arguments.booleanOption(
                        "state-vector",
                        true
                    ))
                    .build())
                .metadata(
                    "origin",
                    "quantum-cli"
                )
                .build()
        );
        final Map<String, Object> payload = backendJobRecordPayload(record);
        payload.put(
            "command",
            "backend-dry-run"
        );
        payload.put(
            "historyCount",
            registry.history().count()
        );
        printResult(
            arguments,
            out,
            payload,
            "Backend dry-run: " + record.status().name()
                + ", accepted=" + record.isAccepted()
                + ", trackingId=" + record.trackingId().value()
        );
        return record.isAccepted() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
    }

    private static int workflow(
        final CliArguments arguments,
        final PrintStream out
    ) throws IOException {
        final ProgramLoadResult load = requireLoadedProgram(arguments);
        if (!load.success()) {
            printImportFailure(
                arguments,
                out,
                "workflow",
                load
            );
            return EXIT_WORKFLOW_ERROR;
        }
        final IntegrationFormat format = arguments.requiredOutputFormat();
        final ProductWorkflowOptions.Builder options = ProductWorkflowOptions.builder()
            .simulationOptions(SimulationOptions.builder()
                .shots(arguments.intOption(
                    "shots",
                    1024
                ))
                .seed(arguments.longOption(
                    "seed",
                    1L
                ))
                .maxQubits(arguments.intOption(
                    "max-qubits",
                    20
                ))
                .captureStateVector(arguments.booleanOption(
                    "state-vector",
                    true
                ))
                .build())
            .compilerOptions(compilerOptions(arguments))
            .resourceMaxQubits(arguments.intOption(
                "resource-max-qubits",
                arguments.intOption(
                    "max-qubits",
                    20
                )
            ))
            .runValidation(!arguments.flag("skip-validation"))
            .runInspection(!arguments.flag("skip-inspection"))
            .runResources(!arguments.flag("skip-resources"))
            .runTimeline(!arguments.flag("skip-timeline"))
            .runPreflight(!arguments.flag("skip-preflight"))
            .runSimulation(!arguments.flag("skip-simulation"))
            .runCompiler(!arguments.flag("skip-compiler"))
            .runBackendDryRun(!arguments.flag("skip-backend"));
        if (arguments.flag("fast")) {
            options.fastWorkflow()
                .runValidation(!arguments.flag("skip-validation"));
        }
        final ProductWorkflowReport report = Quantum.runProductWorkflow(
            format,
            load.program(),
            options.build()
        );
        final Map<String, Object> payload = workflowPayload(report);
        payload.put(
            "command",
            "workflow"
        );
        printResult(
            arguments,
            out,
            payload,
            "Workflow: " + report.status()
                + ", operations=" + (report.inspection() == null
                    ? "skipped"
                    : report.inspection().operationCount())
                + ", target=" + format.name()
        );
        return report.isSuccess() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
    }

    private static CompilerOptions compilerOptions(final CliArguments arguments) {
        final CompilerOptions.Builder options = CompilerOptions.builder();
        if (arguments.flag("fast")) {
            options.fastExportOnly();
        }
        if (arguments.flag("skip-validation")) {
            options.skipInitialValidation()
                .skipTransformedValidation();
        }
        if (arguments.flag("skip-inspection")) {
            options.skipInitialInspection()
                .skipTransformedInspection();
        }
        if (arguments.flag("skip-preflight")) {
            options.skipInitialPreflight()
                .skipTransformedPreflight();
        }
        if (arguments.flag("skip-transformation")) {
            options.skipTransformation();
        }
        if (arguments.flag("skip-transformed-validation")) {
            options.skipTransformedValidation();
        }
        if (arguments.flag("skip-transformed-inspection")) {
            options.skipTransformedInspection();
        }
        if (arguments.flag("skip-transformed-preflight")) {
            options.skipTransformedPreflight();
        }
        return options.build();
    }

    private static int benchmark(
        final CliArguments arguments,
        final PrintStream out
    ) throws IOException {
        if (!arguments.hasInputPath()) {
            throw new CliUsageException("Command requires --input <path>.");
        }
        final Path inputPath = arguments.requiredInputPath();
        final String inputFormat = arguments.inputFormatName().equals("auto")
            ? detectInputFormat(inputPath)
            : arguments.inputFormatName();
        final IntegrationFormat targetFormat = arguments.requiredOutputFormat();
        final ProductBenchmarkOptions options = ProductBenchmarkOptions.builder()
            .warmupIterations(arguments.intOption(
                "warmup",
                1
            ))
            .measurementIterations(arguments.intOption(
                "iterations",
                3
            ))
            .includeImport(!arguments.flag("skip-import"))
            .workflowOptions(ProductWorkflowOptions.builder()
                .simulationOptions(SimulationOptions.builder()
                    .shots(arguments.intOption(
                        "shots",
                        1024
                    ))
                    .seed(arguments.longOption(
                        "seed",
                        1L
                    ))
                    .maxQubits(arguments.intOption(
                        "max-qubits",
                        20
                    ))
                    .captureStateVector(arguments.booleanOption(
                        "state-vector",
                        true
                    ))
                    .build())
                .resourceMaxQubits(arguments.intOption(
                    "resource-max-qubits",
                    arguments.intOption(
                        "max-qubits",
                        20
                    )
                ))
                .runBackendDryRun(!arguments.flag("skip-backend"))
                .build())
            .build();
        final ProductBenchmarkReport report;
        if (inputFormat.equals("json")) {
            final ProgramLoadResult load = requireLoadedProgram(arguments);
            if (!load.success()) {
                printImportFailure(
                    arguments,
                    out,
                    "benchmark",
                    load
                );
                return EXIT_WORKFLOW_ERROR;
            }
            report = Quantum.benchmark(
                targetFormat,
                load.program(),
                options
            );
        } else {
            report = Quantum.benchmarkExternal(
                parseIntegrationFormat(inputFormat),
                Files.readString(inputPath),
                targetFormat,
                options
            );
        }
        final Map<String, Object> payload = benchmarkPayload(report);
        payload.put(
            "command",
            "benchmark"
        );
        printResult(
            arguments,
            out,
            payload,
            "Benchmark: stages=" + report.stageCount()
                + ", totalAverage=" + report.totalAverageNanos() + " ns"
        );
        return report.isSuccess() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
    }

    private static int compatibility(
        final CliArguments arguments,
        final PrintStream out
    ) throws IOException {
        final ProgramLoadResult load = requireLoadedProgram(arguments);
        if (!load.success()) {
            printImportFailure(
                arguments,
                out,
                "compatibility",
                load
            );
            return EXIT_WORKFLOW_ERROR;
        }
        final ProductCompatibilityMatrix matrix = Quantum.compatibilityMatrix(
            load.program(),
            ProductWorkflowOptions.builder()
                .simulationOptions(SimulationOptions.builder()
                    .shots(arguments.intOption(
                        "shots",
                        1024
                    ))
                    .seed(arguments.longOption(
                        "seed",
                        1L
                    ))
                    .maxQubits(arguments.intOption(
                        "max-qubits",
                        20
                    ))
                    .captureStateVector(arguments.booleanOption(
                        "state-vector",
                        true
                    ))
                    .build())
                .resourceMaxQubits(arguments.intOption(
                    "resource-max-qubits",
                    arguments.intOption(
                        "max-qubits",
                        20
                    )
                ))
                .runBackendDryRun(!arguments.flag("skip-backend"))
                .build()
        );
        final Map<String, Object> payload = compatibilityPayload(matrix);
        payload.put(
            "command",
            "compatibility"
        );
        printResult(
            arguments,
            out,
            payload,
            "Compatibility: targets=" + matrix.targets().size()
                + ", success=" + matrix.isSuccess()
        );
        return matrix.isSuccess() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
    }

    private static int verifyCrossFormat(
        final CliArguments arguments,
        final PrintStream out
    ) throws IOException {
        if (!arguments.hasInputPath()) {
            throw new CliUsageException("Command requires --input <path>.");
        }
        final Path inputPath = arguments.requiredInputPath();
        final String inputFormat = arguments.inputFormatName().equals("auto")
            ? detectInputFormat(inputPath)
            : arguments.inputFormatName();
        if (inputFormat.equals("json")) {
            throw new CliUsageException("verify-cross-format requires an external input format, not native JSON.");
        }
        final CrossFormatVerificationReport report = Quantum.verifyCrossFormat(
            parseIntegrationFormat(inputFormat),
            Files.readString(inputPath),
            SimulationOptions.builder()
                .shots(arguments.intOption(
                    "shots",
                    1024
                ))
                .seed(arguments.longOption(
                    "seed",
                    1L
                ))
                .maxQubits(arguments.intOption(
                    "max-qubits",
                    20
                ))
                .captureStateVector(arguments.booleanOption(
                    "state-vector",
                    false
                ))
                .build()
        );
        final Map<String, Object> payload = crossFormatPayload(report);
        payload.put(
            "command",
            "verify-cross-format"
        );
        printResult(
            arguments,
            out,
            payload,
            "Cross-format verification: targets=" + report.targets().size()
                + ", success=" + report.isSuccess()
        );
        return report.isSuccess() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
    }

    private static int regressCorpus(
        final CliArguments arguments,
        final PrintStream out
    ) throws IOException {
        if (!arguments.hasInputPath()) {
            throw new CliUsageException("Command requires --input <directory>.");
        }
        final Path inputPath = arguments.requiredInputPath();
        if (!Files.isDirectory(inputPath)) {
            throw new CliUsageException("regress-corpus requires --input to point to a directory.");
        }
        final CorpusRegressionReport report = Quantum.runCorpusRegression(
            collectCorpusCases(inputPath),
            ProductWorkflowOptions.builder()
                .simulationOptions(SimulationOptions.builder()
                    .shots(arguments.intOption(
                        "shots",
                        1024
                    ))
                    .seed(arguments.longOption(
                        "seed",
                        1L
                    ))
                    .maxQubits(arguments.intOption(
                        "max-qubits",
                        20
                    ))
                    .captureStateVector(arguments.booleanOption(
                        "state-vector",
                        false
                    ))
                    .build())
                .resourceMaxQubits(arguments.intOption(
                    "resource-max-qubits",
                    arguments.intOption(
                        "max-qubits",
                        20
                    )
                ))
                .runBackendDryRun(!arguments.flag("skip-backend"))
                .build()
        );
        final Map<String, Object> payload = corpusRegressionPayload(report);
        payload.put(
            "command",
            "regress-corpus"
        );
        printResult(
            arguments,
            out,
            payload,
            "Corpus regression: cases=" + report.caseCount()
                + ", failures=" + report.failureCount()
        );
        return report.isSuccess() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
    }

    private static int releaseReadiness(
        final CliArguments arguments,
        final PrintStream out
    ) throws IOException {
        if (!arguments.hasInputPath()) {
            throw new CliUsageException("Command requires --input <directory>.");
        }
        final Path inputPath = arguments.requiredInputPath();
        if (!Files.isDirectory(inputPath)) {
            throw new CliUsageException("release-readiness requires --input to point to a directory.");
        }
        final ReleaseReadinessReport report = Quantum.releaseReadiness(
            collectCorpusCases(inputPath),
            arguments.requiredOutputFormat(),
            ProductBenchmarkOptions.builder()
                .warmupIterations(arguments.intOption(
                    "warmup",
                    0
                ))
                .measurementIterations(arguments.intOption(
                    "iterations",
                    1
                ))
                .workflowOptions(ProductWorkflowOptions.builder()
                    .simulationOptions(SimulationOptions.builder()
                        .shots(arguments.intOption(
                            "shots",
                            1024
                        ))
                        .seed(arguments.longOption(
                            "seed",
                            1L
                        ))
                        .maxQubits(arguments.intOption(
                            "max-qubits",
                            20
                        ))
                        .captureStateVector(arguments.booleanOption(
                            "state-vector",
                            false
                        ))
                        .build())
                    .resourceMaxQubits(arguments.intOption(
                        "resource-max-qubits",
                        arguments.intOption(
                            "max-qubits",
                            20
                        )
                    ))
                    .runBackendDryRun(!arguments.flag("skip-backend"))
                    .build())
                .build()
        );
        final Map<String, Object> payload = releaseReadinessPayload(report);
        payload.put(
            "command",
            "release-readiness"
        );
        printResult(
            arguments,
            out,
            payload,
            "Release readiness: " + report.status()
                + ", failedChecks=" + report.failedCheckCount()
                + ", warnings=" + report.warningCheckCount()
        );
        return report.isAcceptable() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
    }

    private static int productAudit(
        final CliArguments arguments,
        final PrintStream out
    ) throws IOException {
        if (!arguments.hasInputPath()) {
            throw new CliUsageException("Command requires --input <directory>.");
        }
        final Path inputPath = arguments.requiredInputPath();
        if (!Files.isDirectory(inputPath)) {
            throw new CliUsageException("product-audit requires --input to point to a directory.");
        }
        final ProductAuditReport report = productAuditReport(
            arguments,
            inputPath
        );
        final Map<String, Object> payload = productAuditPayload(report);
        payload.put(
            "command",
            "product-audit"
        );
        printResult(
            arguments,
            out,
            payload,
            "Product audit: " + report.status()
                + ", failedChecks=" + report.failedCheckCount()
                + ", warnings=" + report.warningCheckCount()
        );
        return report.isAcceptable() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
    }

    private static ProductAuditReport productAuditReport(
        final CliArguments arguments,
        final Path inputPath
    ) throws IOException {
        return Quantum.productAudit(
            arguments.pathOption(
                "project-root",
                Path.of("").toAbsolutePath().normalize()
            ),
            collectCorpusCases(inputPath),
            arguments.requiredOutputFormat(),
            ProductBenchmarkOptions.builder()
                .warmupIterations(arguments.intOption(
                    "warmup",
                    0
                ))
                .measurementIterations(arguments.intOption(
                    "iterations",
                    1
                ))
                .workflowOptions(ProductWorkflowOptions.builder()
                    .simulationOptions(SimulationOptions.builder()
                        .shots(arguments.intOption(
                            "shots",
                            1024
                        ))
                        .seed(arguments.longOption(
                            "seed",
                            1L
                        ))
                        .maxQubits(arguments.intOption(
                            "max-qubits",
                            20
                        ))
                        .captureStateVector(arguments.booleanOption(
                            "state-vector",
                            false
                        ))
                        .build())
                    .resourceMaxQubits(arguments.intOption(
                        "resource-max-qubits",
                        arguments.intOption(
                            "max-qubits",
                            20
                        )
                    ))
                    .runBackendDryRun(!arguments.flag("skip-backend"))
                    .build())
                .build()
        );
    }

    private static int productReport(
        final CliArguments arguments,
        final PrintStream out
    ) throws IOException {
        if (!arguments.hasInputPath()) {
            throw new CliUsageException("Command requires --input <directory>.");
        }
        if (!arguments.hasOutputPath()) {
            throw new CliUsageException("Command requires --output <directory>.");
        }
        final Path inputPath = arguments.requiredInputPath();
        if (!Files.isDirectory(inputPath)) {
            throw new CliUsageException("product-report requires --input to point to a directory.");
        }
        final ProductAuditReport report = productAuditReport(arguments, inputPath);
        final Map<String, Object> auditPayload = productAuditPayload(report);
        auditPayload.put(
            "command",
            "product-audit"
        );
        final ProductReportBundleResult bundle = Quantum.writeProductReportBundle(
            arguments.outputPath(),
            report,
            JSON.writeValueAsString(auditPayload)
        );
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "command",
            "product-report"
        );
        payload.put(
            "status",
            report.status().name()
        );
        payload.put(
            "acceptable",
            report.isAcceptable()
        );
        payload.put(
            "outputDirectory",
            bundle.outputDirectory().toString()
        );
        payload.put(
            "auditJsonPath",
            bundle.auditJsonPath().toString()
        );
        payload.put(
            "summaryMarkdownPath",
            bundle.summaryMarkdownPath().toString()
        );
        payload.put(
            "manifestPath",
            bundle.manifestPath().toString()
        );
        payload.put(
            "audit",
            auditPayload
        );
        printResult(
            arguments,
            out,
            payload,
            "Product report: " + report.status()
                + ", output=" + bundle.outputDirectory()
        );
        return report.isAcceptable() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
    }

    private static int productDistribution(
        final CliArguments arguments,
        final PrintStream out
    ) throws IOException {
        if (!arguments.hasInputPath()) {
            throw new CliUsageException("Command requires --input <directory>.");
        }
        if (!arguments.hasOutputPath()) {
            throw new CliUsageException("Command requires --output <directory>.");
        }
        final Path inputPath = arguments.requiredInputPath();
        if (!Files.isDirectory(inputPath)) {
            throw new CliUsageException("product-distribution requires --input to point to a directory.");
        }
        final Path projectRoot = arguments.pathOption(
            "project-root",
            Path.of(".")
        ).toAbsolutePath().normalize();
        final Path output = arguments.outputPath();
        final Path stagingParent = output.toAbsolutePath().normalize().getParent() == null
            ? Path.of(".").toAbsolutePath().normalize()
            : output.toAbsolutePath().normalize().getParent();
        final Path reportDirectory = Files.createTempDirectory(
            stagingParent,
            "quantum-product-report-"
        );
        final ProductAuditReport report = productAuditReport(arguments, inputPath);
        final Map<String, Object> auditPayload = productAuditPayload(report);
        auditPayload.put(
            "command",
            "product-audit"
        );
        final ProductDistributionBundleResult bundle;
        try {
            Quantum.writeProductReportBundle(
                reportDirectory,
                report,
                JSON.writeValueAsString(auditPayload)
            );
            bundle = Quantum.writeProductDistributionBundle(
                output,
                projectRoot,
                reportDirectory
            );
        } finally {
            deleteDirectoryIfExists(reportDirectory);
        }
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "command",
            "product-distribution"
        );
        payload.put(
            "status",
            report.status().name()
        );
        payload.put(
            "acceptable",
            report.isAcceptable()
        );
        payload.put(
            "outputDirectory",
            bundle.outputDirectory().toString()
        );
        payload.put(
            "archivePath",
            bundle.archivePath().toString()
        );
        payload.put(
            "archiveSha256",
            bundle.archiveSha256()
        );
        payload.put(
            "manifestPath",
            bundle.manifestPath().toString()
        );
        payload.put(
            "quickstartPath",
            bundle.quickstartPath().toString()
        );
        payload.put(
            "librariesDirectory",
            bundle.librariesDirectory().toString()
        );
        payload.put(
            "toolsDirectory",
            bundle.toolsDirectory().toString()
        );
        payload.put(
            "examplesDirectory",
            bundle.examplesDirectory().toString()
        );
        payload.put(
            "reportDirectory",
            bundle.reportDirectory().toString()
        );
        payload.put(
            "packagedFileCount",
            bundle.packagedFileCount()
        );
        payload.put(
            "packagedFiles",
            productDistributionFilesPayload(bundle)
        );
        payload.put(
            "audit",
            auditPayload
        );
        printResult(
            arguments,
            out,
            payload,
            "Product distribution: " + report.status()
                + ", output=" + bundle.outputDirectory()
        );
        return report.isAcceptable() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
    }

    private static int productVerifyDistribution(
        final CliArguments arguments,
        final PrintStream out
    ) throws IOException {
        if (!arguments.hasInputPath()) {
            throw new CliUsageException("Command requires --input <distribution-directory>.");
        }
        final ProductDistributionVerificationResult verification = Quantum.verifyProductDistributionBundle(
            arguments.requiredInputPath()
        );
        final Map<String, Object> payload = productDistributionVerificationPayload(verification);
        payload.put(
            "command",
            "product-verify-distribution"
        );
        printResult(
            arguments,
            out,
            payload,
            verification.isSuccess()
                ? "Product distribution verification: ok"
                : "Product distribution verification: failed"
        );
        return verification.isSuccess() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
    }

    private static int targetProfile(
        final CliArguments arguments,
        final PrintStream out
    ) throws JsonProcessingException {
        final IntegrationFormat format = arguments.requiredOutputFormat();
        final IntegrationCapabilityProfile profile = Quantum.targetProfile(format);
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "command",
            "target-profile"
        );
        payload.put(
            "targetName",
            profile.targetName()
        );
        payload.put(
            "targetVersion",
            profile.targetVersion()
        );
        payload.put(
            "nativeGates",
            profile.nativeGateNames()
        );
        payload.put(
            "parameterKinds",
            profile.supportedParameterKinds().stream()
                .map(Enum::name)
                .toList()
        );
        payload.put(
            "metadata",
            profile.metadata()
        );
        printResult(
            arguments,
            out,
            payload,
            "Target profile: " + profile.targetName() + " " + profile.targetVersion()
        );
        return EXIT_SUCCESS;
    }

    private static int doctor(
        final CliArguments arguments,
        final PrintStream out
    ) throws JsonProcessingException {
        final Path projectRoot = arguments.hasInputPath()
            ? arguments.requiredInputPath()
            : Path.of("").toAbsolutePath().normalize();
        final ProductDoctorReport report = Quantum.productDoctor(projectRoot);
        final Map<String, Object> payload = productDoctorPayload(report);
        payload.put(
            "command",
            "doctor"
        );
        printResult(
            arguments,
            out,
            payload,
            "Product doctor: " + report.status()
                + ", failedChecks=" + report.failedCheckCount()
                + ", warnings=" + report.warningCheckCount()
        );
        return report.isAcceptable() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
    }

    private static ProgramLoadResult requireLoadedProgram(final CliArguments arguments) throws IOException {
        if (!arguments.hasInputPath()) {
            throw new CliUsageException("Command requires --input <path>.");
        }
        return loadProgram(arguments);
    }

    private static ProgramLoadResult loadProgram(final CliArguments arguments) throws IOException {
        final Path inputPath = arguments.requiredInputPath();
        final String inputFormat = arguments.inputFormatName().equals("auto")
            ? detectInputFormat(inputPath)
            : arguments.inputFormatName();
        if (inputFormat.equals("json")) {
            final QuantumIrReadResult read = Quantum.readJson(inputPath);
            if (read.isSuccess()) {
                return ProgramLoadResult.success(read.program());
            }
            return ProgramLoadResult.failure(List.of(Map.of(
                "severity",
                "ERROR",
                "message",
                "Native JSON read failed."
            )));
        }
        final IntegrationFormat format = parseIntegrationFormat(inputFormat);
        final ImportResult imported = Quantum.importProgram(
            format,
            Files.readString(inputPath)
        );
        if (imported.isSuccess()) {
            return ProgramLoadResult.success(imported.program());
        }
        return ProgramLoadResult.failure(diagnosticsPayload(imported.diagnostics()));
    }

    private static void deleteDirectoryIfExists(final Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        Files.walkFileTree(
            directory,
            new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(
                    final Path file,
                    final BasicFileAttributes attributes
                ) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(
                    final Path current,
                    final IOException exception
                ) throws IOException {
                    if (exception != null) {
                        throw exception;
                    }
                    Files.delete(current);
                    return FileVisitResult.CONTINUE;
                }
            }
        );
    }

    private static List<CorpusRegressionCase> collectCorpusCases(final Path root) throws IOException {
        final ArrayList<CorpusRegressionCase> cases = new ArrayList<>();
        final ArrayList<Path> files = new ArrayList<>();
        collectCorpusFiles(
            root,
            files
        );
        for (int index = 0; index < files.size(); index++) {
            final Path file = files.get(index);
            final String formatName = detectInputFormat(file);
            if (formatName.equals("json")) {
                continue;
            }
            cases.add(CorpusRegressionCase.of(
                root.relativize(file).toString(),
                Files.readString(file),
                Quantum.integration(parseIntegrationFormat(formatName))
            ));
        }
        if (cases.isEmpty()) {
            throw new CliUsageException("Corpus directory does not contain supported external files.");
        }
        return cases;
    }

    private static void collectCorpusFiles(
        final Path root,
        final ArrayList<Path> files
    ) throws IOException {
        try (final java.nio.file.DirectoryStream<Path> entries = Files.newDirectoryStream(root)) {
            for (Path entry : entries) {
                if (Files.isDirectory(entry)) {
                    collectCorpusFiles(
                        entry,
                        files
                    );
                } else if (isCorpusFile(entry)) {
                    files.add(entry);
                }
            }
        }
    }

    private static boolean isCorpusFile(final Path path) {
        final String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(".qasm") || fileName.endsWith(".quil");
    }

    private static String detectInputFormat(final Path path) throws IOException {
        final String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (
            fileName.endsWith(".json")
            || fileName.endsWith(".qir.json")
            || fileName.endsWith(".quantum.json")
        ) {
            return "json";
        }
        if (fileName.endsWith(".quil")) {
            return "quil";
        }
        if (fileName.endsWith(".qasm")) {
            final String source = Files.readString(path);
            if (source.contains("OPENQASM 3")) {
                return "openqasm3";
            }
            return "openqasm2";
        }
        throw new CliUsageException("Cannot detect input format for file: " + path + ".");
    }

    private static IntegrationFormat parseIntegrationFormat(final String value) {
        return switch (value) {
            case "openqasm2", "qasm2", "openqasm_2" -> IntegrationFormat.OPENQASM_2;
            case "openqasm3", "qasm3", "openqasm_3" -> IntegrationFormat.OPENQASM_3;
            case "quil" -> IntegrationFormat.QUIL;
            default -> throw new CliUsageException("Unsupported integration format: " + value + ".");
        };
    }

    private static TransformationOptions transformationOptions(final CliArguments arguments) {
        final TransformationOptions.Builder builder = TransformationOptions.builder();
        if (arguments.flag("remove-identity")) {
            builder.removeIdentityGates();
        }
        if (arguments.flag("inline-composite")) {
            builder.inlineCompositeGates();
        }
        if (arguments.flag("canonicalize-parameters")) {
            builder.canonicalizeParameterExpressions();
        }
        if (arguments.hasOption("target-lowering")) {
            builder.targetAwareLowering(Quantum.targetProfile(arguments.requiredOutputFormat()));
        }
        return builder.build();
    }

    private static Map<String, Object> validationPayload(final ValidationResult validation) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "valid",
            validation.isValid()
        );
        payload.put(
            "errorCount",
            validation.errorCount()
        );
        payload.put(
            "errors",
            validationErrorsPayload(validation.errors())
        );
        return payload;
    }

    private static List<Map<String, Object>> validationErrorsPayload(final List<ValidationError> errors) {
        final ArrayList<Map<String, Object>> payload = new ArrayList<>();
        for (ValidationError error : errors) {
            payload.add(Map.of(
                "code",
                error.code().name(),
                "message",
                error.message(),
                "circuitIndex",
                error.circuitIndex(),
                "operationIndex",
                error.operationIndex()
            ));
        }
        return payload;
    }

    private static Map<String, Object> inspectionPayload(final ProgramInspectionResult inspection) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "computationModel",
            inspection.computationModel().name()
        );
        payload.put(
            "circuitCount",
            inspection.circuitCount()
        );
        payload.put(
            "qubitCount",
            inspection.qubitCount()
        );
        payload.put(
            "classicalBitCount",
            inspection.classicalBitCount()
        );
        payload.put(
            "operationCount",
            inspection.operationCount()
        );
        payload.put(
            "gateCount",
            inspection.gateCount()
        );
        payload.put(
            "measurementCount",
            inspection.measurementCount()
        );
        payload.put(
            "gateHistogram",
            inspection.gateHistogram()
        );
        payload.put(
            "operationKindHistogram",
            inspection.operationKindHistogram()
        );
        payload.put(
            "diagnosticCount",
            inspection.diagnosticCount()
        );
        return payload;
    }

    private static Map<String, Object> resourcePayload(final ResourceEstimate estimate) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "computationModel",
            estimate.computationModel().name()
        );
        payload.put(
            "circuitCount",
            estimate.circuitCount()
        );
        payload.put(
            "qubitCount",
            estimate.qubitCount()
        );
        payload.put(
            "classicalBitCount",
            estimate.classicalBitCount()
        );
        payload.put(
            "operationCount",
            estimate.operationCount()
        );
        payload.put(
            "gateCount",
            estimate.gateCount()
        );
        payload.put(
            "measurementCount",
            estimate.measurementCount()
        );
        payload.put(
            "estimatedStateVectorAmplitudes",
            estimate.estimatedStateVectorAmplitudes()
        );
        payload.put(
            "estimatedStateVectorBytes",
            estimate.estimatedStateVectorBytes()
        );
        payload.put(
            "localSimulationFeasible",
            estimate.isLocalSimulationFeasible()
        );
        payload.put(
            "localSimulationMaxQubits",
            estimate.localSimulationMaxQubits()
        );
        payload.put(
            "gateHistogram",
            estimate.gateHistogram()
        );
        payload.put(
            "circuits",
            estimate.circuits().stream()
                .map(QuantumCli::circuitResourcePayload)
                .toList()
        );
        return payload;
    }

    private static Map<String, Object> timelinePayload(final ProgramTimeline timeline) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "circuits",
            timeline.circuits().stream()
                .map(QuantumCli::timelineCircuitPayload)
                .toList()
        );
        return payload;
    }

    private static Map<String, Object> timelineCircuitPayload(final CircuitTimeline timeline) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "name",
            timeline.circuitName()
        );
        payload.put(
            "quantumWires",
            timeline.quantumWires()
        );
        payload.put(
            "classicalWires",
            timeline.classicalWires()
        );
        payload.put(
            "steps",
            timeline.steps().stream()
                .map(QuantumCli::timelineStepPayload)
                .toList()
        );
        return payload;
    }

    private static Map<String, Object> timelineStepPayload(final CircuitTimelineStep step) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "operationIndex",
            step.operationIndex()
        );
        payload.put(
            "operationKind",
            step.operationKind().name()
        );
        payload.put(
            "label",
            step.label()
        );
        payload.put(
            "quantumWires",
            step.quantumWires()
        );
        payload.put(
            "classicalWires",
            step.classicalWires()
        );
        payload.put(
            "staticPlacement",
            step.isStaticPlacement()
        );
        return payload;
    }

    private static Map<String, Object> circuitResourcePayload(final CircuitResourceEstimate estimate) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "name",
            estimate.name()
        );
        payload.put(
            "qubitCount",
            estimate.qubitCount()
        );
        payload.put(
            "classicalBitCount",
            estimate.classicalBitCount()
        );
        payload.put(
            "operationCount",
            estimate.operationCount()
        );
        payload.put(
            "gateCount",
            estimate.gateCount()
        );
        payload.put(
            "measurementCount",
            estimate.measurementCount()
        );
        payload.put(
            "twoQubitGateCount",
            estimate.twoQubitGateCount()
        );
        payload.put(
            "multiQubitGateCount",
            estimate.multiQubitGateCount()
        );
        payload.put(
            "approximateDepth",
            estimate.approximateDepth()
        );
        payload.put(
            "depthPrecise",
            estimate.isDepthPrecise()
        );
        payload.put(
            "neverMeasuredQubits",
            estimate.neverMeasuredQubits()
        );
        payload.put(
            "overwrittenClassicalBits",
            estimate.overwrittenClassicalBits()
        );
        return payload;
    }

    private static Map<String, Object> preflightPayload(final CapabilityPreflightResult preflight) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "status",
            preflight.status().name()
        );
        payload.put(
            "success",
            preflight.isSuccess()
        );
        payload.put(
            "requiresLowering",
            preflight.requiresLowering()
        );
        payload.put(
            "diagnostics",
            diagnosticsPayload(preflight.diagnostics())
        );
        return payload;
    }

    private static Map<String, Object> simulationPayload(final SimulationResult simulation) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "success",
            simulation.isSuccess()
        );
        payload.put(
            "qubitCount",
            simulation.qubitCount()
        );
        payload.put(
            "classicalBitCount",
            simulation.classicalBitCount()
        );
        payload.put(
            "shots",
            simulation.shots()
        );
        payload.put(
            "counts",
            simulation.counts()
        );
        payload.put(
            "stateVectorSize",
            simulation.stateVector().size()
        );
        payload.put(
            "diagnostics",
            simulationDiagnosticsPayload(simulation.diagnostics())
        );
        return payload;
    }

    private static Map<String, Object> backendJobRecordPayload(final BackendJobRecord record) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "success",
            record.isAccepted()
        );
        payload.put(
            "accepted",
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
            "backendName",
            record.backendDescriptor().displayName()
        );
        payload.put(
            "status",
            record.status().name()
        );
        payload.put(
            "submittedAt",
            record.submittedAt().toString()
        );
        payload.put(
            "updatedAt",
            record.updatedAt().toString()
        );
        payload.put(
            "queueMetadata",
            record.queueMetadata()
        );
        payload.put(
            "costMetadata",
            record.costMetadata()
        );
        payload.put(
            "providerMetadata",
            record.providerMetadata()
        );
        payload.put(
            "diagnostics",
            backendDiagnosticsPayload(record.diagnostics())
        );
        if (record.hasBackendJobId()) {
            payload.put(
                "backendJobId",
                record.backendJobId().value()
            );
        }
        if (record.hasExecutionResult()) {
            payload.put(
                "compilerSuccess",
                record.executionResult().hasCompilerResult()
                    && record.executionResult().compilerResult().isSuccess()
            );
            payload.put(
                "simulationSuccess",
                record.executionResult().hasSimulationResult()
                    && record.executionResult().simulationResult().isSuccess()
            );
            payload.put(
                "counts",
                record.executionResult().hasSimulationResult()
                    ? record.executionResult().simulationResult().counts()
                    : Map.of()
            );
        }
        return payload;
    }

    private static Map<String, Object> workflowPayload(final ProductWorkflowReport report) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "targetFormat",
            report.targetFormat().name()
        );
        payload.put(
            "status",
            report.status().name()
        );
        payload.put(
            "success",
            report.isSuccess()
        );
        payload.put(
            "validation",
            validationPayload(report.validation())
        );
        payload.put(
            "inspection",
            inspectionPayload(report.inspection())
        );
        if (report.preflight() != null) {
            payload.put(
                "preflight",
                preflightPayload(report.preflight())
            );
        }
        payload.put(
            "resources",
            resourcePayload(report.resources())
        );
        payload.put(
            "circuit",
            timelinePayload(report.timeline())
        );
        if (report.simulation() != null) {
            payload.put(
                "simulation",
                simulationPayload(report.simulation())
            );
        }
        if (report.compiler() != null) {
            payload.put(
                "compile",
                compilerPayload(report.compiler())
            );
        }
        if (report.hasBackendSubmission()) {
            final Map<String, Object> backend = new LinkedHashMap<>();
            backend.put(
                "accepted",
                report.backendSubmission().isAccepted()
            );
            backend.put(
                "status",
                report.backendSubmission().status().name()
            );
            if (report.hasBackendExecution()) {
                backend.put(
                    "resultStatus",
                    report.backendExecution().status().name()
                );
                backend.put(
                    "compilerSuccess",
                    report.backendExecution().hasCompilerResult()
                        && report.backendExecution().compilerResult().isSuccess()
                );
                backend.put(
                    "simulationSuccess",
                    report.backendExecution().hasSimulationResult()
                        && report.backendExecution().simulationResult().isSuccess()
                );
            }
            payload.put(
                "backendDryRun",
                backend
            );
        }
        payload.put(
            "targetProfile",
            Map.of(
                "targetName",
                report.targetProfile().targetName(),
                "targetVersion",
                report.targetProfile().targetVersion(),
                "nativeGates",
                report.targetProfile().nativeGateNames()
            )
        );
        return payload;
    }

    private static Map<String, Object> benchmarkPayload(final ProductBenchmarkReport report) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "success",
            report.isSuccess()
        );
        payload.put(
            "targetFormat",
            report.targetFormat().name()
        );
        if (report.hasInputFormat()) {
            payload.put(
                "inputFormat",
                report.inputFormat().name()
            );
        }
        payload.put(
            "stageCount",
            report.stageCount()
        );
        payload.put(
            "totalAverageNanos",
            report.totalAverageNanos()
        );
        payload.put(
            "stages",
            report.stages().stream()
                .map(QuantumCli::benchmarkStagePayload)
                .toList()
        );
        return payload;
    }

    private static Map<String, Object> compatibilityPayload(final ProductCompatibilityMatrix matrix) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "success",
            matrix.isSuccess()
        );
        payload.put(
            "validation",
            validationPayload(matrix.validation())
        );
        payload.put(
            "inspection",
            inspectionPayload(matrix.inspection())
        );
        payload.put(
            "resources",
            resourcePayload(matrix.resources())
        );
        payload.put(
            "simulation",
            simulationPayload(matrix.simulation())
        );
        payload.put(
            "targets",
            matrix.targets().stream()
                .map(QuantumCli::targetCompatibilityPayload)
                .toList()
        );
        return payload;
    }

    private static Map<String, Object> targetCompatibilityPayload(final TargetCompatibilityReport target) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "targetFormat",
            target.targetFormat().name()
        );
        payload.put(
            "targetName",
            target.targetProfile().targetName()
        );
        payload.put(
            "targetVersion",
            target.targetProfile().targetVersion()
        );
        payload.put(
            "status",
            target.status().name()
        );
        payload.put(
            "success",
            target.isSuccess()
        );
        payload.put(
            "preflightStatus",
            target.preflightStatus() == null
                ? null
                : target.preflightStatus().name()
        );
        payload.put(
            "compilerStatus",
            target.compilerStatus() == null
                ? null
                : target.compilerStatus().name()
        );
        payload.put(
            "workflowStatus",
            target.workflowStatus() == null
                ? null
                : target.workflowStatus().name()
        );
        payload.put(
            "nativeGateCount",
            target.targetProfile().nativeGateNames().size()
        );
        payload.put(
            "capabilities",
            target.targetProfile().capabilities().stream()
                .map(Enum::name)
                .toList()
        );
        payload.put(
            "checks",
            target.checks().stream()
                .map(QuantumCli::compatibilityCheckPayload)
                .toList()
        );
        return payload;
    }

    private static Map<String, Object> compatibilityCheckPayload(final CompatibilityCheckResult check) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "name",
            check.name()
        );
        payload.put(
            "status",
            check.status().name()
        );
        payload.put(
            "success",
            check.isSuccess()
        );
        payload.put(
            "message",
            check.message()
        );
        payload.put(
            "diagnosticCount",
            check.diagnosticCount()
        );
        payload.put(
            "elapsedNanos",
            check.elapsedNanos()
        );
        return payload;
    }

    private static Map<String, Object> crossFormatPayload(final CrossFormatVerificationReport report) {
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
            "importDiagnosticCount",
            report.importDiagnosticCount()
        );
        if (report.validation() != null) {
            payload.put(
                "validation",
                validationPayload(report.validation())
            );
        }
        if (report.simulation() != null) {
            payload.put(
                "simulation",
                simulationPayload(report.simulation())
            );
        }
        payload.put(
            "targets",
            report.targets().stream()
                .map(QuantumCli::crossFormatTargetPayload)
                .toList()
        );
        return payload;
    }

    private static Map<String, Object> crossFormatTargetPayload(final CrossFormatTargetVerification target) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "targetFormat",
            target.targetFormat().name()
        );
        payload.put(
            "success",
            target.isSuccess()
        );
        payload.put(
            "exportSuccess",
            target.exportSuccess()
        );
        payload.put(
            "reimportSuccess",
            target.reimportSuccess()
        );
        payload.put(
            "validationSuccess",
            target.validationSuccess()
        );
        payload.put(
            "simulationSuccess",
            target.simulationSuccess()
        );
        payload.put(
            "simulationEquivalent",
            target.simulationEquivalent()
        );
        payload.put(
            "exportDiagnosticCount",
            target.exportDiagnosticCount()
        );
        payload.put(
            "reimportDiagnosticCount",
            target.reimportDiagnosticCount()
        );
        payload.put(
            "validationErrorCount",
            target.validationErrorCount()
        );
        return payload;
    }

    private static Map<String, Object> corpusRegressionPayload(final CorpusRegressionReport report) {
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
        payload.put(
            "cases",
            report.cases().stream()
                .map(QuantumCli::corpusRegressionCasePayload)
                .toList()
        );
        return payload;
    }

    private static Map<String, Object> corpusRegressionCasePayload(final CorpusRegressionCaseReport report) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "name",
            report.name()
        );
        payload.put(
            "inputFormat",
            report.inputFormat().name()
        );
        payload.put(
            "success",
            report.isSuccess()
        );
        payload.put(
            "importSuccess",
            report.importSuccess()
        );
        payload.put(
            "importDiagnosticCount",
            report.importDiagnosticCount()
        );
        if (report.validation() != null) {
            payload.put(
                "validation",
                validationPayload(report.validation())
            );
        }
        if (report.compatibilityMatrix() != null) {
            payload.put(
                "compatibility",
                compatibilityPayload(report.compatibilityMatrix())
            );
        }
        if (report.crossFormatVerification() != null) {
            payload.put(
                "crossFormat",
                crossFormatPayload(report.crossFormatVerification())
            );
        }
        return payload;
    }

    private static Map<String, Object> releaseReadinessPayload(final ReleaseReadinessReport report) {
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
            "checks",
            report.checks().stream()
                .map(QuantumCli::releaseReadinessCheckPayload)
                .toList()
        );
        payload.put(
            "targetProfiles",
            report.targetProfiles().stream()
                .map(profile -> Map.of(
                    "format",
                    profile.format().name(),
                    "targetName",
                    profile.targetName(),
                    "targetVersion",
                    profile.targetVersion(),
                    "capabilityCount",
                    profile.capabilities().size(),
                    "nativeGateCount",
                    profile.nativeGateNames().size()
                ))
                .toList()
        );
        payload.put(
            "corpusRegression",
            corpusRegressionPayload(report.corpusRegression())
        );
        if (report.hasBenchmark()) {
            payload.put(
                "benchmark",
                benchmarkPayload(report.benchmark())
            );
        }
        return payload;
    }

    private static Map<String, Object> releaseReadinessCheckPayload(final ReleaseReadinessCheck check) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "name",
            check.name()
        );
        payload.put(
            "status",
            check.status().name()
        );
        payload.put(
            "message",
            check.message()
        );
        return payload;
    }

    private static Map<String, Object> productDoctorPayload(final ProductDoctorReport report) {
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
            "success",
            report.isSuccess()
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
        payload.put(
            "checks",
            report.checks().stream()
                .map(QuantumCli::productDoctorCheckPayload)
                .toList()
        );
        return payload;
    }

    private static Map<String, Object> productAuditPayload(final ProductAuditReport report) {
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
            "doctor",
            productDoctorPayload(report.doctor())
        );
        payload.put(
            "readiness",
            releaseReadinessPayload(report.readiness())
        );
        return payload;
    }

    private static List<String> productDistributionFilesPayload(final ProductDistributionBundleResult bundle) {
        final ArrayList<String> files = new ArrayList<>(bundle.packagedFiles().size());
        for (int index = 0; index < bundle.packagedFiles().size(); index++) {
            files.add(bundle.outputDirectory()
                .relativize(bundle.packagedFiles().get(index))
                .toString()
                .replace(
                    '\\',
                    '/'
                ));
        }
        return files;
    }

    private static Map<String, Object> productDistributionVerificationPayload(
        final ProductDistributionVerificationResult verification
    ) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "success",
            verification.isSuccess()
        );
        payload.put(
            "distributionDirectory",
            verification.distributionDirectory().toString()
        );
        payload.put(
            "archivePath",
            verification.archivePath().toString()
        );
        payload.put(
            "archivePresent",
            verification.archivePresent()
        );
        payload.put(
            "verifiedFileCount",
            verification.verifiedFileCount()
        );
        payload.put(
            "issueCount",
            verification.issueCount()
        );
        final ArrayList<Map<String, Object>> issues = new ArrayList<>(verification.issues().size());
        for (int index = 0; index < verification.issues().size(); index++) {
            issues.add(productDistributionVerificationIssuePayload(verification.issues().get(index)));
        }
        payload.put(
            "issues",
            issues
        );
        return payload;
    }

    private static Map<String, Object> productDistributionVerificationIssuePayload(
        final ProductDistributionVerificationIssue issue
    ) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "code",
            issue.code()
        );
        payload.put(
            "path",
            issue.path()
        );
        payload.put(
            "message",
            issue.message()
        );
        return payload;
    }

    private static Map<String, Object> productDoctorCheckPayload(final ProductDoctorCheck check) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "name",
            check.name()
        );
        payload.put(
            "status",
            check.status().name()
        );
        payload.put(
            "message",
            check.message()
        );
        return payload;
    }

    private static Map<String, Object> benchmarkStagePayload(final BenchmarkStageResult stage) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "stage",
            stage.stage()
        );
        payload.put(
            "success",
            stage.isSuccess()
        );
        payload.put(
            "status",
            stage.status()
        );
        payload.put(
            "warmupIterations",
            stage.warmupIterations()
        );
        payload.put(
            "measurementIterations",
            stage.measurementIterations()
        );
        payload.put(
            "minNanos",
            stage.minNanos()
        );
        payload.put(
            "averageNanos",
            stage.averageNanos()
        );
        payload.put(
            "maxNanos",
            stage.maxNanos()
        );
        payload.put(
            "memoryDeltaBytes",
            stage.memoryDeltaBytes()
        );
        return payload;
    }

    private static Map<String, Object> compilerPayload(final CompilerResult result) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "targetFormat",
            result.format().name()
        );
        payload.put(
            "status",
            result.status().name()
        );
        payload.put(
            "success",
            result.isSuccess()
        );
        payload.put(
            "stageCount",
            result.stageRecords().size()
        );
        payload.put(
            "hasContent",
            result.hasExportResult() && result.exportResult().hasContent()
        );
        final ArrayList<Map<String, Object>> stages = new ArrayList<>(result.stageRecords().size());
        for (final CompilerStageRecord stage : result.stageRecords()) {
            stages.add(Map.of(
                "stage",
                stage.stage().name(),
                "status",
                stage.status().name(),
                "message",
                stage.message()
            ));
        }
        payload.put(
            "stages",
            stages
        );
        return payload;
    }

    private static List<Map<String, Object>> diagnosticsPayload(final List<IntegrationDiagnostic> diagnostics) {
        final ArrayList<Map<String, Object>> payload = new ArrayList<>();
        for (IntegrationDiagnostic diagnostic : diagnostics) {
            payload.add(Map.of(
                "severity",
                diagnostic.severity().name(),
                "code",
                diagnostic.code().name(),
                "message",
                diagnostic.message(),
                "line",
                diagnostic.line(),
                "column",
                diagnostic.column()
            ));
        }
        return payload;
    }

    private static List<Map<String, Object>> simulationDiagnosticsPayload(
        final List<SimulationDiagnostic> diagnostics
    ) {
        final ArrayList<Map<String, Object>> payload = new ArrayList<>();
        for (SimulationDiagnostic diagnostic : diagnostics) {
            payload.add(Map.of(
                "severity",
                diagnostic.severity().name(),
                "code",
                diagnostic.code().name(),
                "message",
                diagnostic.message(),
                "circuitIndex",
                diagnostic.circuitIndex(),
                "operationIndex",
                diagnostic.operationIndex()
            ));
        }
        return payload;
    }

    private static List<Map<String, Object>> backendDiagnosticsPayload(
        final List<BackendDiagnostic> diagnostics
    ) {
        final ArrayList<Map<String, Object>> payload = new ArrayList<>();
        for (BackendDiagnostic diagnostic : diagnostics) {
            payload.add(Map.of(
                "severity",
                diagnostic.severity().name(),
                "code",
                diagnostic.code().name(),
                "message",
                diagnostic.message()
            ));
        }
        return payload;
    }

    private static void printImportFailure(
        final CliArguments arguments,
        final PrintStream out,
        final String command,
        final ProgramLoadResult load
    ) throws JsonProcessingException {
        printResult(
            arguments,
            out,
            Map.of(
                "command",
                command,
                "status",
                "import_failed",
                "diagnostics",
                load.diagnostics()
            ),
            "Import failed."
        );
    }

    private static void printResult(
        final CliArguments arguments,
        final PrintStream out,
        final Map<String, Object> payload,
        final String text
    ) throws JsonProcessingException {
        if (arguments.outputMode().equals("json")) {
            out.println(JSON.writeValueAsString(payload));
        } else {
            out.println(text);
        }
    }

    private static String helpText() {
        return """
            quantum <command> [options]

            Commands:
              validate          Validate a program.
              inspect           Print program metrics.
              resources         Estimate memory, depth, and simulation resource pressure.
              circuit           Print circuit timeline data for visualization.
              preflight         Check export capability for a target format.
              transform         Apply explicit conservative transformations.
              compile           Run validation, inspection, preflight, transformation, and export.
              convert           Convert between native JSON and supported external formats.
              simulate          Run local state-vector simulation where supported.
              backend-dry-run   Submit a tracked local backend dry-run job.
              workflow          Run validate, inspect, resources, circuit, simulate, compile, and dry-run backend.
              benchmark         Measure import, validation, inspection, resources, simulation, compile, and workflow.
              compatibility     Build product compatibility matrix for all supported targets.
              verify-cross-format
                                Verify external format -> IR -> every supported target -> IR round-trips.
              regress-corpus    Run compatibility and cross-format regression for a directory corpus.
              release-readiness Run release readiness audit for a directory corpus.
              target-profile    Print target profile metadata.
              doctor            Check local product structure, scripts, examples, and packaged jars.
              product-audit     Run doctor plus release readiness as one product gate.
              product-report    Write product-audit.json and summary.md bundle.
              product-distribution
                                Write a local release distribution bundle.
              product-verify-distribution
                                Verify an unpacked release distribution bundle.

            Common options:
              --input <path>
              --project-root <path>
              --input-format auto|json|openqasm2|openqasm3|quil
              --output <path>
              --output-format json|openqasm2|openqasm3|quil
              --format text|json

            Simulation options:
              --shots <count>
              --seed <long>
              --max-qubits <count>
            """;
    }
}
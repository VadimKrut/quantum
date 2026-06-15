/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.api;

import java.nio.file.Path;
import java.io.IOException;
import java.util.List;

import ru.pathcreator.vadim.quantum.application.backend.BackendDescriptor;
import ru.pathcreator.vadim.quantum.application.backend.BackendJobOptions;
import ru.pathcreator.vadim.quantum.application.backend.BackendPreflightChecker;
import ru.pathcreator.vadim.quantum.application.backend.BackendPreflightResult;
import ru.pathcreator.vadim.quantum.application.backend.BackendSubmissionResult;
import ru.pathcreator.vadim.quantum.application.backend.DryRunQuantumBackend;
import ru.pathcreator.vadim.quantum.application.backend.InMemoryBackendJobRegistry;
import ru.pathcreator.vadim.quantum.application.backend.QuantumBackend;
import ru.pathcreator.vadim.quantum.application.audit.ProductAuditReport;
import ru.pathcreator.vadim.quantum.application.audit.ProductAuditRunner;
import ru.pathcreator.vadim.quantum.application.benchmark.ProductBenchmarkOptions;
import ru.pathcreator.vadim.quantum.application.benchmark.ProductBenchmarkReport;
import ru.pathcreator.vadim.quantum.application.benchmark.ProductBenchmarkRunner;
import ru.pathcreator.vadim.quantum.application.compatibility.ProductCompatibilityMatrix;
import ru.pathcreator.vadim.quantum.application.compatibility.ProductCompatibilityMatrixRunner;
import ru.pathcreator.vadim.quantum.application.compiler.CompilerOptions;
import ru.pathcreator.vadim.quantum.application.compiler.CompilerResult;
import ru.pathcreator.vadim.quantum.application.compiler.QuantumCompiler;
import ru.pathcreator.vadim.quantum.application.doctor.ProductDoctorReport;
import ru.pathcreator.vadim.quantum.application.doctor.ProductDoctorRunner;
import ru.pathcreator.vadim.quantum.application.distribution.ProductDistributionBundleResult;
import ru.pathcreator.vadim.quantum.application.distribution.ProductDistributionBundleWriter;
import ru.pathcreator.vadim.quantum.application.distribution.ProductDistributionVerificationResult;
import ru.pathcreator.vadim.quantum.application.distribution.ProductDistributionVerifier;
import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightChecker;
import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightResult;
import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapabilityProfile;
import ru.pathcreator.vadim.quantum.application.integration.contract.QuantumIntegration;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.integration.options.ExportOptions;
import ru.pathcreator.vadim.quantum.application.integration.options.ImportOptions;
import ru.pathcreator.vadim.quantum.application.integration.result.ExportResult;
import ru.pathcreator.vadim.quantum.application.integration.result.ImportResult;
import ru.pathcreator.vadim.quantum.application.inspection.ProgramInspectionResult;
import ru.pathcreator.vadim.quantum.application.inspection.QuantumProgramInspector;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrFileWriteResult;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrReadResult;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrWriteResult;
import ru.pathcreator.vadim.quantum.application.regression.CorpusRegressionCase;
import ru.pathcreator.vadim.quantum.application.regression.CorpusRegressionReport;
import ru.pathcreator.vadim.quantum.application.regression.CorpusRegressionRunner;
import ru.pathcreator.vadim.quantum.application.report.ProductReportBundleResult;
import ru.pathcreator.vadim.quantum.application.report.ProductReportBundleWriter;
import ru.pathcreator.vadim.quantum.application.readiness.ReleaseReadinessReport;
import ru.pathcreator.vadim.quantum.application.readiness.ReleaseReadinessRunner;
import ru.pathcreator.vadim.quantum.application.resource.ResourceEstimate;
import ru.pathcreator.vadim.quantum.application.resource.ResourceEstimator;
import ru.pathcreator.vadim.quantum.application.simulation.engine.QuantumSimulator;
import ru.pathcreator.vadim.quantum.application.simulation.options.SimulationOptions;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.application.transformation.QuantumProgramTransformer;
import ru.pathcreator.vadim.quantum.application.transformation.TransformationOptions;
import ru.pathcreator.vadim.quantum.application.transformation.TransformationResult;
import ru.pathcreator.vadim.quantum.application.visualization.ProgramTimeline;
import ru.pathcreator.vadim.quantum.application.visualization.QuantumProgramTimelineBuilder;
import ru.pathcreator.vadim.quantum.application.verification.CrossFormatVerificationReport;
import ru.pathcreator.vadim.quantum.application.verification.CrossFormatVerificationRunner;
import ru.pathcreator.vadim.quantum.application.workflow.ProductWorkflowOptions;
import ru.pathcreator.vadim.quantum.application.workflow.ProductWorkflowReport;
import ru.pathcreator.vadim.quantum.application.workflow.ProductWorkflowRunner;
import ru.pathcreator.vadim.quantum.api.workflow.QuantumExportWorkflowResult;
import ru.pathcreator.vadim.quantum.api.workflow.QuantumImportJsonWorkflowResult;
import ru.pathcreator.vadim.quantum.api.workflow.QuantumJsonExportWorkflowResult;
import ru.pathcreator.vadim.quantum.domain.model.QuantumComputationModel;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.storage.CompactQuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.validation.QuantumProgramValidator;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

/**
 * Единый публичный фасад для основных сценариев работы с Quantum IR.
 */
public final class Quantum {

    private Quantum() {
    }

    /**
     * Создает gate-based Quantum IR программу.
     *
     * @return новая программа
     */
    public static QuantumProgram gateBasedProgram() {
        return QuantumPrograms.gateBased();
    }

    /**
     * Создает fluent builder для gate-based Quantum IR программы.
     *
     * @return builder программы
     */
    public static QuantumProgramBuilder programBuilder() {
        return QuantumProgramBuilder.gateBased();
    }

    /**
     * Создает fluent builder для Quantum IR программы указанной вычислительной модели.
     *
     * @param computationModel вычислительная модель
     * @return builder программы
     */
    public static QuantumProgramBuilder programBuilder(final QuantumComputationModel computationModel) {
        return QuantumProgramBuilder.create(computationModel);
    }

    /**
     * Создает Quantum IR программу указанной вычислительной модели.
     *
     * @param computationModel вычислительная модель
     * @return новая программа
     */
    public static QuantumProgram program(final QuantumComputationModel computationModel) {
        return QuantumPrograms.create(computationModel);
    }

    /**
     * Возвращает интеграцию по формату.
     *
     * @param format внешний формат
     * @return интеграция
     */
    public static QuantumIntegration integration(final IntegrationFormat format) {
        if (format == null) {
            throw new IllegalArgumentException("Integration format must not be null.");
        }
        return switch (format) {
            case OPENQASM_2 -> QuantumIntegrations.openQasm2();
            case OPENQASM_3 -> QuantumIntegrations.openQasm3();
            case QUIL -> QuantumIntegrations.quil();
        };
    }

    /**
     * Возвращает target capability profile для внешнего формата.
     *
     * @param format внешний формат
     * @return target capability profile
     */
    public static IntegrationCapabilityProfile targetProfile(final IntegrationFormat format) {
        return integration(format).capabilityProfile();
    }

    /**
     * Возвращает target profile OpenQASM 2.
     *
     * @return target profile OpenQASM 2
     */
    public static IntegrationCapabilityProfile openQasm2TargetProfile() {
        return targetProfile(IntegrationFormat.OPENQASM_2);
    }

    /**
     * Возвращает target profile OpenQASM 3.
     *
     * @return target profile OpenQASM 3
     */
    public static IntegrationCapabilityProfile openQasm3TargetProfile() {
        return targetProfile(IntegrationFormat.OPENQASM_3);
    }

    /**
     * Возвращает target profile Quil.
     *
     * @return target profile Quil
     */
    public static IntegrationCapabilityProfile quilTargetProfile() {
        return targetProfile(IntegrationFormat.QUIL);
    }

    /**
     * Создает dry-run backend для указанного внешнего формата.
     *
     * @param format внешний формат backend target
     * @return dry-run backend
     */
    public static QuantumBackend dryRunBackend(final IntegrationFormat format) {
        return dryRunBackend(
            format,
            "dry-run-" + format.name().toLowerCase(),
            "Dry Run " + format.name(),
            "1"
        );
    }

    /**
     * Создает именованный dry-run backend для указанного внешнего формата.
     *
     * @param format внешний формат backend target
     * @param backendId стабильный id backend
     * @param displayName имя backend
     * @param version версия backend
     * @return dry-run backend
     */
    public static QuantumBackend dryRunBackend(
        final IntegrationFormat format,
        final String backendId,
        final String displayName,
        final String version
    ) {
        return new DryRunQuantumBackend(
            backendId,
            displayName,
            version,
            integration(format)
        );
    }

    /**
     * Выполняет backend preflight без отправки job.
     *
     * @param program Quantum IR программа
     * @param descriptor описание backend
     * @return результат backend preflight
     */
    public static BackendPreflightResult backendPreflight(
        final QuantumProgram program,
        final BackendDescriptor descriptor
    ) {
        return new BackendPreflightChecker().check(
            program,
            descriptor
        );
    }

    /**
     * Creates a local backend job registry for tracked submissions.
     *
     * @return empty backend job registry
     */
    public static InMemoryBackendJobRegistry backendJobRegistry() {
        return new InMemoryBackendJobRegistry();
    }

    /**
     * Отправляет программу в dry-run backend указанного формата.
     *
     * @param format внешний формат backend target
     * @param program Quantum IR программа
     * @param options настройки job
     * @return результат отправки
     */
    public static BackendSubmissionResult submitDryRun(
        final IntegrationFormat format,
        final QuantumProgram program,
        final BackendJobOptions options
    ) {
        return dryRunBackend(format).submit(
            program,
            options
        );
    }

    /**
     * Импортирует внешний текст в Quantum IR.
     *
     * @param format внешний формат
     * @param source внешний текст
     * @return результат import
     */
    public static ImportResult importProgram(
        final IntegrationFormat format,
        final String source
    ) {
        return integration(format).importProgram(source);
    }

    /**
     * Импортирует OpenQASM 2 в Quantum IR.
     *
     * @param source OpenQASM 2 текст
     * @return результат import
     */
    public static ImportResult importOpenQasm2(final String source) {
        return importProgram(
            IntegrationFormat.OPENQASM_2,
            source
        );
    }

    /**
     * Импортирует OpenQASM 2 в Quantum IR с явными настройками.
     *
     * @param source OpenQASM 2 текст
     * @param options настройки import
     * @return результат import
     */
    public static ImportResult importOpenQasm2(
        final String source,
        final ImportOptions options
    ) {
        return importProgram(
            IntegrationFormat.OPENQASM_2,
            source,
            options
        );
    }

    /**
     * Импортирует OpenQASM 3 в Quantum IR.
     *
     * @param source OpenQASM 3 текст
     * @return результат import
     */
    public static ImportResult importOpenQasm3(final String source) {
        return importProgram(
            IntegrationFormat.OPENQASM_3,
            source
        );
    }

    /**
     * Импортирует OpenQASM 3 в Quantum IR с явными настройками.
     *
     * @param source OpenQASM 3 текст
     * @param options настройки import
     * @return результат import
     */
    public static ImportResult importOpenQasm3(
        final String source,
        final ImportOptions options
    ) {
        return importProgram(
            IntegrationFormat.OPENQASM_3,
            source,
            options
        );
    }

    /**
     * Импортирует Quil в Quantum IR.
     *
     * @param source Quil текст
     * @return результат import
     */
    public static ImportResult importQuil(final String source) {
        return importProgram(
            IntegrationFormat.QUIL,
            source
        );
    }

    /**
     * Импортирует Quil в Quantum IR с явными настройками.
     *
     * @param source Quil текст
     * @param options настройки import
     * @return результат import
     */
    public static ImportResult importQuil(
        final String source,
        final ImportOptions options
    ) {
        return importProgram(
            IntegrationFormat.QUIL,
            source,
            options
        );
    }

    /**
     * Импортирует внешний текст в Quantum IR с явными настройками.
     *
     * @param format внешний формат
     * @param source внешний текст
     * @param options настройки import
     * @return результат import
     */
    public static ImportResult importProgram(
        final IntegrationFormat format,
        final String source,
        final ImportOptions options
    ) {
        return integration(format).importProgram(
            source,
            options
        );
    }

    /**
     * Экспортирует Quantum IR во внешний формат.
     *
     * @param format внешний формат
     * @param program программа
     * @return результат export
     */
    public static ExportResult exportProgram(
        final IntegrationFormat format,
        final QuantumProgram program
    ) {
        return integration(format).exportProgram(program);
    }

    /**
     * Экспортирует Quantum IR в OpenQASM 2.
     *
     * @param program программа
     * @return результат export
     */
    public static ExportResult exportOpenQasm2(final QuantumProgram program) {
        return exportProgram(
            IntegrationFormat.OPENQASM_2,
            program
        );
    }

    /**
     * Экспортирует Quantum IR в OpenQASM 2 с явными настройками.
     *
     * @param program программа
     * @param options настройки export
     * @return результат export
     */
    public static ExportResult exportOpenQasm2(
        final QuantumProgram program,
        final ExportOptions options
    ) {
        return exportProgram(
            IntegrationFormat.OPENQASM_2,
            program,
            options
        );
    }

    /**
     * Экспортирует Quantum IR в OpenQASM 3.
     *
     * @param program программа
     * @return результат export
     */
    public static ExportResult exportOpenQasm3(final QuantumProgram program) {
        return exportProgram(
            IntegrationFormat.OPENQASM_3,
            program
        );
    }

    /**
     * Экспортирует Quantum IR в OpenQASM 3 с явными настройками.
     *
     * @param program программа
     * @param options настройки export
     * @return результат export
     */
    public static ExportResult exportOpenQasm3(
        final QuantumProgram program,
        final ExportOptions options
    ) {
        return exportProgram(
            IntegrationFormat.OPENQASM_3,
            program,
            options
        );
    }

    /**
     * Экспортирует Quantum IR в Quil.
     *
     * @param program программа
     * @return результат export
     */
    public static ExportResult exportQuil(final QuantumProgram program) {
        return exportProgram(
            IntegrationFormat.QUIL,
            program
        );
    }

    /**
     * Экспортирует Quantum IR в Quil с явными настройками.
     *
     * @param program программа
     * @param options настройки export
     * @return результат export
     */
    public static ExportResult exportQuil(
        final QuantumProgram program,
        final ExportOptions options
    ) {
        return exportProgram(
            IntegrationFormat.QUIL,
            program,
            options
        );
    }

    /**
     * Экспортирует Quantum IR во внешний формат с явными настройками.
     *
     * @param format внешний формат
     * @param program программа
     * @param options настройки export
     * @return результат export
     */
    public static ExportResult exportProgram(
        final IntegrationFormat format,
        final QuantumProgram program,
        final ExportOptions options
    ) {
        return integration(format).exportProgram(
            program,
            options
        );
    }

    /**
     * Проверяет Quantum IR общим доменным валидатором.
     *
     * @param program программа
     * @return результат валидации
     */
    public static ValidationResult validate(final QuantumProgram program) {
        return new QuantumProgramValidator().validate(program);
    }

    /**
     * Анализирует Quantum IR программу без target profile.
     *
     * @param program программа
     * @return результат inspection
     */
    public static ProgramInspectionResult inspect(final QuantumProgram program) {
        return new QuantumProgramInspector().inspect(program);
    }

    /**
     * Анализирует Quantum IR программу с target compatibility summary.
     *
     * @param program программа
     * @param targetProfiles target profiles
     * @return результат inspection
     */
    public static ProgramInspectionResult inspect(
        final QuantumProgram program,
        final List<IntegrationCapabilityProfile> targetProfiles
    ) {
        return new QuantumProgramInspector().inspect(
            program,
            targetProfiles
        );
    }

    public static ResourceEstimate estimateResources(final QuantumProgram program) {
        return new ResourceEstimator().estimate(program);
    }

    public static ResourceEstimate estimateResources(
        final QuantumProgram program,
        final int localSimulationMaxQubits
    ) {
        return new ResourceEstimator().estimate(
            program,
            localSimulationMaxQubits
        );
    }

    public static ProgramTimeline timeline(final QuantumProgram program) {
        return new QuantumProgramTimelineBuilder().build(program);
    }

    public static ProductWorkflowReport runProductWorkflow(
        final IntegrationFormat targetFormat,
        final QuantumProgram program
    ) {
        return runProductWorkflow(
            targetFormat,
            program,
            ProductWorkflowOptions.defaults()
        );
    }

    public static ProductWorkflowReport runProductWorkflow(
        final IntegrationFormat targetFormat,
        final QuantumProgram program,
        final ProductWorkflowOptions options
    ) {
        return new ProductWorkflowRunner().run(
            program,
            integration(targetFormat),
            options
        );
    }

    public static ProductBenchmarkReport benchmark(
        final IntegrationFormat targetFormat,
        final QuantumProgram program
    ) {
        return benchmark(
            targetFormat,
            program,
            ProductBenchmarkOptions.defaults()
        );
    }

    public static ProductBenchmarkReport benchmark(
        final IntegrationFormat targetFormat,
        final QuantumProgram program,
        final ProductBenchmarkOptions options
    ) {
        return new ProductBenchmarkRunner().run(
            program,
            integration(targetFormat),
            options
        );
    }

    public static ProductBenchmarkReport benchmarkExternal(
        final IntegrationFormat inputFormat,
        final String source,
        final IntegrationFormat targetFormat
    ) {
        return benchmarkExternal(
            inputFormat,
            source,
            targetFormat,
            ProductBenchmarkOptions.defaults()
        );
    }

    public static ProductBenchmarkReport benchmarkExternal(
        final IntegrationFormat inputFormat,
        final String source,
        final IntegrationFormat targetFormat,
        final ProductBenchmarkOptions options
    ) {
        return new ProductBenchmarkRunner().runExternal(
            source,
            integration(inputFormat),
            integration(targetFormat),
            options
        );
    }

    public static ProductCompatibilityMatrix compatibilityMatrix(final QuantumProgram program) {
        return compatibilityMatrix(
            program,
            ProductWorkflowOptions.defaults()
        );
    }

    public static ProductCompatibilityMatrix compatibilityMatrix(
        final QuantumProgram program,
        final ProductWorkflowOptions options
    ) {
        return new ProductCompatibilityMatrixRunner().run(
            program,
            List.of(
                integration(IntegrationFormat.OPENQASM_2),
                integration(IntegrationFormat.OPENQASM_3),
                integration(IntegrationFormat.QUIL)
            ),
            options
        );
    }

    public static CrossFormatVerificationReport verifyCrossFormat(
        final IntegrationFormat inputFormat,
        final String source,
        final SimulationOptions simulationOptions
    ) {
        return new CrossFormatVerificationRunner().verify(
            source,
            integration(inputFormat),
            new QuantumIntegration[] {
                integration(IntegrationFormat.OPENQASM_2),
                integration(IntegrationFormat.OPENQASM_3),
                integration(IntegrationFormat.QUIL)
            },
            simulationOptions
        );
    }

    public static CorpusRegressionReport runCorpusRegression(
        final List<CorpusRegressionCase> cases,
        final ProductWorkflowOptions options
    ) {
        return new CorpusRegressionRunner().run(
            cases,
            List.of(
                integration(IntegrationFormat.OPENQASM_2),
                integration(IntegrationFormat.OPENQASM_3),
                integration(IntegrationFormat.QUIL)
            ),
            options
        );
    }

    public static ReleaseReadinessReport releaseReadiness(
        final List<CorpusRegressionCase> cases,
        final IntegrationFormat benchmarkTargetFormat,
        final ProductBenchmarkOptions benchmarkOptions
    ) {
        return new ReleaseReadinessRunner().run(
            cases,
            List.of(
                integration(IntegrationFormat.OPENQASM_2),
                integration(IntegrationFormat.OPENQASM_3),
                integration(IntegrationFormat.QUIL)
            ),
            integration(benchmarkTargetFormat),
            benchmarkOptions
        );
    }

    public static ProductDoctorReport productDoctor(final Path projectRoot) {
        return new ProductDoctorRunner().run(projectRoot);
    }

    public static ProductAuditReport productAudit(
        final Path projectRoot,
        final List<CorpusRegressionCase> cases,
        final IntegrationFormat benchmarkTargetFormat,
        final ProductBenchmarkOptions benchmarkOptions
    ) {
        return new ProductAuditRunner().run(
            projectRoot,
            cases,
            List.of(
                integration(IntegrationFormat.OPENQASM_2),
                integration(IntegrationFormat.OPENQASM_3),
                integration(IntegrationFormat.QUIL)
            ),
            integration(benchmarkTargetFormat),
            benchmarkOptions
        );
    }

    public static String productReportSummary(final ProductAuditReport audit) {
        return new ProductReportBundleWriter().summaryMarkdown(audit);
    }

    public static ProductReportBundleResult writeProductReportBundle(
        final Path outputDirectory,
        final ProductAuditReport audit,
        final String auditJson
    ) throws IOException {
        return new ProductReportBundleWriter().write(
            outputDirectory,
            audit,
            auditJson
        );
    }

    public static ProductDistributionBundleResult writeProductDistributionBundle(
        final Path outputDirectory,
        final Path projectRoot
    ) throws IOException {
        return new ProductDistributionBundleWriter().write(
            outputDirectory,
            projectRoot
        );
    }

    public static ProductDistributionBundleResult writeProductDistributionBundle(
        final Path outputDirectory,
        final Path projectRoot,
        final Path productReportDirectory
    ) throws IOException {
        return new ProductDistributionBundleWriter().write(
            outputDirectory,
            projectRoot,
            productReportDirectory
        );
    }

    public static ProductDistributionVerificationResult verifyProductDistributionBundle(final Path distributionDirectory) {
        return new ProductDistributionVerifier().verify(distributionDirectory);
    }

    /**
     * Собирает circuit в плотное представление для больших потоков gate-based операций.
     *
     * @param circuit схема
     * @return плотное представление circuit
     */
    public static CompactQuantumCircuit compact(final QuantumCircuit circuit) {
        return CompactQuantumCircuit.from(circuit);
    }

    /**
     * Выполняет явные консервативные трансформации Quantum IR без мутации исходной программы.
     *
     * @param program исходная программа
     * @param options опции трансформации
     * @return результат трансформации
     */
    public static TransformationResult transform(
        final QuantumProgram program,
        final TransformationOptions options
    ) {
        return new QuantumProgramTransformer().transform(
            program,
            options
        );
    }

    /**
     * Выполняет локальную state-vector симуляцию Quantum IR программы.
     *
     * @param program Quantum IR программа
     * @return результат симуляции
     */
    public static SimulationResult simulate(final QuantumProgram program) {
        return new QuantumSimulator().simulate(program);
    }

    /**
     * Выполняет локальную state-vector симуляцию Quantum IR программы с явными настройками.
     *
     * @param program Quantum IR программа
     * @param options настройки симуляции
     * @return результат симуляции
     */
    public static SimulationResult simulate(
        final QuantumProgram program,
        final SimulationOptions options
    ) {
        return new QuantumSimulator().simulate(
            program,
            options
        );
    }

    /**
     * Выполняет полный compiler pipeline до target export.
     *
     * @param format целевой внешний формат
     * @param program исходная Quantum IR программа
     * @return результат compiler pipeline
     */
    public static CompilerResult compile(
        final IntegrationFormat format,
        final QuantumProgram program
    ) {
        return compile(
            format,
            program,
            CompilerOptions.defaults()
        );
    }

    /**
     * Выполняет полный compiler pipeline до target export с явными настройками.
     *
     * @param format целевой внешний формат
     * @param program исходная Quantum IR программа
     * @param options настройки compiler pipeline
     * @return результат compiler pipeline
     */
    public static CompilerResult compile(
        final IntegrationFormat format,
        final QuantumProgram program,
        final CompilerOptions options
    ) {
        return new QuantumCompiler().compile(
            program,
            integration(format),
            options
        );
    }

    /**
     * Выполняет полный compiler pipeline до OpenQASM 2.
     *
     * @param program исходная Quantum IR программа
     * @return результат compiler pipeline
     */
    public static CompilerResult compileOpenQasm2(final QuantumProgram program) {
        return compile(
            IntegrationFormat.OPENQASM_2,
            program
        );
    }

    /**
     * Выполняет полный compiler pipeline до OpenQASM 3.
     *
     * @param program исходная Quantum IR программа
     * @return результат compiler pipeline
     */
    public static CompilerResult compileOpenQasm3(final QuantumProgram program) {
        return compile(
            IntegrationFormat.OPENQASM_3,
            program
        );
    }

    /**
     * Выполняет полный compiler pipeline до Quil.
     *
     * @param program исходная Quantum IR программа
     * @return результат compiler pipeline
     */
    public static CompilerResult compileQuil(final QuantumProgram program) {
        return compile(
            IntegrationFormat.QUIL,
            program
        );
    }

    /**
     * Проверяет, можно ли экспортировать Quantum IR в target format.
     *
     * @param format внешний формат
     * @param program программа
     * @return результат preflight
     */
    public static CapabilityPreflightResult preflight(
        final IntegrationFormat format,
        final QuantumProgram program
    ) {
        final QuantumIntegration integration = integration(format);
        return new CapabilityPreflightChecker().check(
            program,
            integration.capabilityProfile()
        );
    }

    /**
     * Выполняет workflow: validation -> preflight -> export.
     *
     * @param format целевой внешний формат
     * @param program Quantum IR программа
     * @return результат workflow
     */
    public static QuantumExportWorkflowResult validatePreflightExport(
        final IntegrationFormat format,
        final QuantumProgram program
    ) {
        return validatePreflightExport(
            format,
            program,
            ExportOptions.defaults()
        );
    }

    /**
     * Выполняет workflow: validation -> preflight -> export с явными настройками export.
     *
     * @param format целевой внешний формат
     * @param program Quantum IR программа
     * @param options настройки export
     * @return результат workflow
     */
    public static QuantumExportWorkflowResult validatePreflightExport(
        final IntegrationFormat format,
        final QuantumProgram program,
        final ExportOptions options
    ) {
        final ValidationResult validationResult = validate(program);
        if (!validationResult.isValid()) {
            return QuantumExportWorkflowResult.of(
                format,
                program,
                validationResult,
                null,
                null
            );
        }
        final CapabilityPreflightResult preflightResult = preflight(
            format,
            program
        );
        if (!preflightResult.isSuccess()) {
            return QuantumExportWorkflowResult.of(
                format,
                program,
                validationResult,
                preflightResult,
                null
            );
        }
        return QuantumExportWorkflowResult.of(
            format,
            program,
            validationResult,
            preflightResult,
            exportProgram(
                format,
                program,
                options
            )
        );
    }

    /**
     * Выполняет workflow: build -> validation -> preflight -> export.
     *
     * @param format целевой внешний формат
     * @param builder builder Quantum IR программы
     * @return результат workflow
     */
    public static QuantumExportWorkflowResult buildValidatePreflightExport(
        final IntegrationFormat format,
        final QuantumProgramBuilder builder
    ) {
        return buildValidatePreflightExport(
            format,
            builder,
            ExportOptions.defaults()
        );
    }

    /**
     * Выполняет workflow: build -> validation -> preflight -> export с явными настройками export.
     *
     * @param format целевой внешний формат
     * @param builder builder Quantum IR программы
     * @param options настройки export
     * @return результат workflow
     */
    public static QuantumExportWorkflowResult buildValidatePreflightExport(
        final IntegrationFormat format,
        final QuantumProgramBuilder builder,
        final ExportOptions options
    ) {
        if (builder == null) {
            throw new IllegalArgumentException("Quantum program builder must not be null.");
        }
        return validatePreflightExport(
            format,
            builder.build(),
            options
        );
    }

    /**
     * Выполняет workflow export в OpenQASM 2.
     *
     * @param program Quantum IR программа
     * @return результат workflow
     */
    public static QuantumExportWorkflowResult validatePreflightExportOpenQasm2(final QuantumProgram program) {
        return validatePreflightExport(
            IntegrationFormat.OPENQASM_2,
            program
        );
    }

    /**
     * Выполняет workflow export в OpenQASM 2 с явными настройками export.
     *
     * @param program Quantum IR программа
     * @param options настройки export
     * @return результат workflow
     */
    public static QuantumExportWorkflowResult validatePreflightExportOpenQasm2(
        final QuantumProgram program,
        final ExportOptions options
    ) {
        return validatePreflightExport(
            IntegrationFormat.OPENQASM_2,
            program,
            options
        );
    }

    /**
     * Выполняет workflow build -> export в OpenQASM 2.
     *
     * @param builder builder Quantum IR программы
     * @return результат workflow
     */
    public static QuantumExportWorkflowResult buildValidatePreflightExportOpenQasm2(
        final QuantumProgramBuilder builder
    ) {
        return buildValidatePreflightExport(
            IntegrationFormat.OPENQASM_2,
            builder
        );
    }

    /**
     * Выполняет workflow build -> export в OpenQASM 2 с явными настройками export.
     *
     * @param builder builder Quantum IR программы
     * @param options настройки export
     * @return результат workflow
     */
    public static QuantumExportWorkflowResult buildValidatePreflightExportOpenQasm2(
        final QuantumProgramBuilder builder,
        final ExportOptions options
    ) {
        return buildValidatePreflightExport(
            IntegrationFormat.OPENQASM_2,
            builder,
            options
        );
    }

    /**
     * Выполняет workflow export в OpenQASM 3.
     *
     * @param program Quantum IR программа
     * @return результат workflow
     */
    public static QuantumExportWorkflowResult validatePreflightExportOpenQasm3(final QuantumProgram program) {
        return validatePreflightExport(
            IntegrationFormat.OPENQASM_3,
            program
        );
    }

    /**
     * Выполняет workflow export в OpenQASM 3 с явными настройками export.
     *
     * @param program Quantum IR программа
     * @param options настройки export
     * @return результат workflow
     */
    public static QuantumExportWorkflowResult validatePreflightExportOpenQasm3(
        final QuantumProgram program,
        final ExportOptions options
    ) {
        return validatePreflightExport(
            IntegrationFormat.OPENQASM_3,
            program,
            options
        );
    }

    /**
     * Выполняет workflow build -> export в OpenQASM 3.
     *
     * @param builder builder Quantum IR программы
     * @return результат workflow
     */
    public static QuantumExportWorkflowResult buildValidatePreflightExportOpenQasm3(
        final QuantumProgramBuilder builder
    ) {
        return buildValidatePreflightExport(
            IntegrationFormat.OPENQASM_3,
            builder
        );
    }

    /**
     * Выполняет workflow build -> export в OpenQASM 3 с явными настройками export.
     *
     * @param builder builder Quantum IR программы
     * @param options настройки export
     * @return результат workflow
     */
    public static QuantumExportWorkflowResult buildValidatePreflightExportOpenQasm3(
        final QuantumProgramBuilder builder,
        final ExportOptions options
    ) {
        return buildValidatePreflightExport(
            IntegrationFormat.OPENQASM_3,
            builder,
            options
        );
    }

    /**
     * Выполняет workflow export в Quil.
     *
     * @param program Quantum IR программа
     * @return результат workflow
     */
    public static QuantumExportWorkflowResult validatePreflightExportQuil(final QuantumProgram program) {
        return validatePreflightExport(
            IntegrationFormat.QUIL,
            program
        );
    }

    /**
     * Выполняет workflow export в Quil с явными настройками export.
     *
     * @param program Quantum IR программа
     * @param options настройки export
     * @return результат workflow
     */
    public static QuantumExportWorkflowResult validatePreflightExportQuil(
        final QuantumProgram program,
        final ExportOptions options
    ) {
        return validatePreflightExport(
            IntegrationFormat.QUIL,
            program,
            options
        );
    }

    /**
     * Выполняет workflow build -> export в Quil.
     *
     * @param builder builder Quantum IR программы
     * @return результат workflow
     */
    public static QuantumExportWorkflowResult buildValidatePreflightExportQuil(
        final QuantumProgramBuilder builder
    ) {
        return buildValidatePreflightExport(
            IntegrationFormat.QUIL,
            builder
        );
    }

    /**
     * Выполняет workflow build -> export в Quil с явными настройками export.
     *
     * @param builder builder Quantum IR программы
     * @param options настройки export
     * @return результат workflow
     */
    public static QuantumExportWorkflowResult buildValidatePreflightExportQuil(
        final QuantumProgramBuilder builder,
        final ExportOptions options
    ) {
        return buildValidatePreflightExport(
            IntegrationFormat.QUIL,
            builder,
            options
        );
    }

    /**
     * Выполняет workflow: import -> validation -> streaming JSON write.
     *
     * @param format исходный внешний формат
     * @param source внешний текст
     * @param path путь JSON-файла
     * @return результат workflow
     */
    public static QuantumImportJsonWorkflowResult importValidateWriteJson(
        final IntegrationFormat format,
        final String source,
        final Path path
    ) {
        return importValidateWriteJson(
            format,
            source,
            path,
            ImportOptions.defaults()
        );
    }

    /**
     * Выполняет workflow: import -> validation -> streaming JSON write с явными настройками import.
     *
     * @param format исходный внешний формат
     * @param source внешний текст
     * @param path путь JSON-файла
     * @param options настройки import
     * @return результат workflow
     */
    public static QuantumImportJsonWorkflowResult importValidateWriteJson(
        final IntegrationFormat format,
        final String source,
        final Path path,
        final ImportOptions options
    ) {
        final ImportResult importResult = importProgram(
            format,
            source,
            options
        );
        if (!importResult.isSuccess()) {
            return QuantumImportJsonWorkflowResult.of(
                format,
                path,
                importResult,
                null,
                null
            );
        }
        final ValidationResult validationResult = validate(importResult.program());
        if (!validationResult.isValid()) {
            return QuantumImportJsonWorkflowResult.of(
                format,
                path,
                importResult,
                validationResult,
                null
            );
        }
        return QuantumImportJsonWorkflowResult.of(
            format,
            path,
            importResult,
            validationResult,
            writeJsonStreaming(
                path,
                importResult.program()
            )
        );
    }

    /**
     * Выполняет workflow import OpenQASM 2 -> validation -> JSON.
     *
     * @param source OpenQASM 2 текст
     * @param path путь JSON-файла
     * @return результат workflow
     */
    public static QuantumImportJsonWorkflowResult importOpenQasm2ValidateWriteJson(
        final String source,
        final Path path
    ) {
        return importValidateWriteJson(
            IntegrationFormat.OPENQASM_2,
            source,
            path
        );
    }

    /**
     * Выполняет workflow import OpenQASM 2 -> validation -> JSON с явными настройками import.
     *
     * @param source OpenQASM 2 текст
     * @param path путь JSON-файла
     * @param options настройки import
     * @return результат workflow
     */
    public static QuantumImportJsonWorkflowResult importOpenQasm2ValidateWriteJson(
        final String source,
        final Path path,
        final ImportOptions options
    ) {
        return importValidateWriteJson(
            IntegrationFormat.OPENQASM_2,
            source,
            path,
            options
        );
    }

    /**
     * Выполняет workflow import OpenQASM 3 -> validation -> JSON.
     *
     * @param source OpenQASM 3 текст
     * @param path путь JSON-файла
     * @return результат workflow
     */
    public static QuantumImportJsonWorkflowResult importOpenQasm3ValidateWriteJson(
        final String source,
        final Path path
    ) {
        return importValidateWriteJson(
            IntegrationFormat.OPENQASM_3,
            source,
            path
        );
    }

    /**
     * Выполняет workflow import OpenQASM 3 -> validation -> JSON с явными настройками import.
     *
     * @param source OpenQASM 3 текст
     * @param path путь JSON-файла
     * @param options настройки import
     * @return результат workflow
     */
    public static QuantumImportJsonWorkflowResult importOpenQasm3ValidateWriteJson(
        final String source,
        final Path path,
        final ImportOptions options
    ) {
        return importValidateWriteJson(
            IntegrationFormat.OPENQASM_3,
            source,
            path,
            options
        );
    }

    /**
     * Выполняет workflow import Quil -> validation -> JSON.
     *
     * @param source Quil текст
     * @param path путь JSON-файла
     * @return результат workflow
     */
    public static QuantumImportJsonWorkflowResult importQuilValidateWriteJson(
        final String source,
        final Path path
    ) {
        return importValidateWriteJson(
            IntegrationFormat.QUIL,
            source,
            path
        );
    }

    /**
     * Выполняет workflow import Quil -> validation -> JSON с явными настройками import.
     *
     * @param source Quil текст
     * @param path путь JSON-файла
     * @param options настройки import
     * @return результат workflow
     */
    public static QuantumImportJsonWorkflowResult importQuilValidateWriteJson(
        final String source,
        final Path path,
        final ImportOptions options
    ) {
        return importValidateWriteJson(
            IntegrationFormat.QUIL,
            source,
            path,
            options
        );
    }

    /**
     * Выполняет workflow: JSON read -> validation -> preflight -> export.
     *
     * @param path путь JSON-файла
     * @param format целевой внешний формат
     * @return результат workflow
     */
    public static QuantumJsonExportWorkflowResult readJsonValidatePreflightExport(
        final Path path,
        final IntegrationFormat format
    ) {
        return readJsonValidatePreflightExport(
            path,
            format,
            ExportOptions.defaults()
        );
    }

    /**
     * Выполняет workflow: JSON read -> validation -> preflight -> export с явными настройками export.
     *
     * @param path путь JSON-файла
     * @param format целевой внешний формат
     * @param options настройки export
     * @return результат workflow
     */
    public static QuantumJsonExportWorkflowResult readJsonValidatePreflightExport(
        final Path path,
        final IntegrationFormat format,
        final ExportOptions options
    ) {
        final QuantumIrReadResult readResult = readJson(path);
        if (!readResult.isSuccess()) {
            return QuantumJsonExportWorkflowResult.of(
                format,
                path,
                readResult,
                null,
                null,
                null
            );
        }
        final ValidationResult validationResult = validate(readResult.program());
        if (!validationResult.isValid()) {
            return QuantumJsonExportWorkflowResult.of(
                format,
                path,
                readResult,
                validationResult,
                null,
                null
            );
        }
        final CapabilityPreflightResult preflightResult = preflight(
            format,
            readResult.program()
        );
        if (!preflightResult.isSuccess()) {
            return QuantumJsonExportWorkflowResult.of(
                format,
                path,
                readResult,
                validationResult,
                preflightResult,
                null
            );
        }
        return QuantumJsonExportWorkflowResult.of(
            format,
            path,
            readResult,
            validationResult,
            preflightResult,
            exportProgram(
                format,
                readResult.program(),
                options
            )
        );
    }

    /**
     * Выполняет workflow JSON -> OpenQASM 2.
     *
     * @param path путь JSON-файла
     * @return результат workflow
     */
    public static QuantumJsonExportWorkflowResult readJsonValidatePreflightExportOpenQasm2(final Path path) {
        return readJsonValidatePreflightExport(
            path,
            IntegrationFormat.OPENQASM_2
        );
    }

    /**
     * Выполняет workflow JSON -> OpenQASM 2 с явными настройками export.
     *
     * @param path путь JSON-файла
     * @param options настройки export
     * @return результат workflow
     */
    public static QuantumJsonExportWorkflowResult readJsonValidatePreflightExportOpenQasm2(
        final Path path,
        final ExportOptions options
    ) {
        return readJsonValidatePreflightExport(
            path,
            IntegrationFormat.OPENQASM_2,
            options
        );
    }

    /**
     * Выполняет workflow JSON -> OpenQASM 3.
     *
     * @param path путь JSON-файла
     * @return результат workflow
     */
    public static QuantumJsonExportWorkflowResult readJsonValidatePreflightExportOpenQasm3(final Path path) {
        return readJsonValidatePreflightExport(
            path,
            IntegrationFormat.OPENQASM_3
        );
    }

    /**
     * Выполняет workflow JSON -> OpenQASM 3 с явными настройками export.
     *
     * @param path путь JSON-файла
     * @param options настройки export
     * @return результат workflow
     */
    public static QuantumJsonExportWorkflowResult readJsonValidatePreflightExportOpenQasm3(
        final Path path,
        final ExportOptions options
    ) {
        return readJsonValidatePreflightExport(
            path,
            IntegrationFormat.OPENQASM_3,
            options
        );
    }

    /**
     * Выполняет workflow JSON -> Quil.
     *
     * @param path путь JSON-файла
     * @return результат workflow
     */
    public static QuantumJsonExportWorkflowResult readJsonValidatePreflightExportQuil(final Path path) {
        return readJsonValidatePreflightExport(
            path,
            IntegrationFormat.QUIL
        );
    }

    /**
     * Выполняет workflow JSON -> Quil с явными настройками export.
     *
     * @param path путь JSON-файла
     * @param options настройки export
     * @return результат workflow
     */
    public static QuantumJsonExportWorkflowResult readJsonValidatePreflightExportQuil(
        final Path path,
        final ExportOptions options
    ) {
        return readJsonValidatePreflightExport(
            path,
            IntegrationFormat.QUIL,
            options
        );
    }

    /**
     * Сериализует Quantum IR в JSON.
     *
     * @param program программа
     * @return результат записи
     */
    public static QuantumIrWriteResult writeJson(final QuantumProgram program) {
        return QuantumIrFiles.writeToString(program);
    }

    /**
     * Читает Quantum IR из JSON.
     *
     * @param content JSON-текст
     * @return результат чтения
     */
    public static QuantumIrReadResult readJson(final String content) {
        return QuantumIrFiles.readFromString(content);
    }

    /**
     * Записывает Quantum IR в JSON-файл.
     *
     * @param path путь к файлу
     * @param program программа
     * @return результат записи
     */
    public static QuantumIrWriteResult writeJson(
        final Path path,
        final QuantumProgram program
    ) {
        return QuantumIrFiles.write(
            path,
            program
        );
    }

    /**
     * Потоково записывает Quantum IR в JSON-файл без удержания полного JSON-текста в памяти.
     *
     * @param path путь к файлу
     * @param program программа
     * @return результат потоковой записи
     */
    public static QuantumIrFileWriteResult writeJsonStreaming(
        final Path path,
        final QuantumProgram program
    ) {
        return QuantumIrFiles.writeToFileStreaming(
            path,
            program
        );
    }

    /**
     * Читает Quantum IR из JSON-файла.
     *
     * @param path путь к файлу
     * @return результат чтения
     */
    public static QuantumIrReadResult readJson(final Path path) {
        return QuantumIrFiles.read(path);
    }
}
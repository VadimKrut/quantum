/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.benchmark;

import java.util.ArrayList;
import java.util.function.Supplier;

import ru.pathcreator.vadim.quantum.application.compiler.CompilerOptions;
import ru.pathcreator.vadim.quantum.application.compiler.CompilerResult;
import ru.pathcreator.vadim.quantum.application.compiler.QuantumCompiler;
import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightChecker;
import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightResult;
import ru.pathcreator.vadim.quantum.application.integration.contract.QuantumIntegration;
import ru.pathcreator.vadim.quantum.application.integration.result.ImportResult;
import ru.pathcreator.vadim.quantum.application.inspection.QuantumProgramInspector;
import ru.pathcreator.vadim.quantum.application.resource.ResourceEstimator;
import ru.pathcreator.vadim.quantum.application.simulation.engine.QuantumSimulator;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.application.visualization.QuantumProgramTimelineBuilder;
import ru.pathcreator.vadim.quantum.application.workflow.ProductWorkflowReport;
import ru.pathcreator.vadim.quantum.application.workflow.ProductWorkflowRunner;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.validation.QuantumProgramValidator;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

public final class ProductBenchmarkRunner {

    public ProductBenchmarkReport run(
        final QuantumProgram program,
        final QuantumIntegration targetIntegration
    ) {
        return run(
            program,
            targetIntegration,
            ProductBenchmarkOptions.defaults()
        );
    }

    public ProductBenchmarkReport run(
        final QuantumProgram program,
        final QuantumIntegration targetIntegration,
        final ProductBenchmarkOptions options
    ) {
        validateProgramBenchmarkInputs(
            program,
            targetIntegration,
            options
        );
        return runProgramStages(
            null,
            program,
            targetIntegration,
            options
        );
    }

    public ProductBenchmarkReport runExternal(
        final String source,
        final QuantumIntegration inputIntegration,
        final QuantumIntegration targetIntegration,
        final ProductBenchmarkOptions options
    ) {
        if (source == null) {
            throw new IllegalArgumentException("Benchmark source must not be null.");
        }
        if (inputIntegration == null) {
            throw new IllegalArgumentException("Benchmark input integration must not be null.");
        }
        if (targetIntegration == null) {
            throw new IllegalArgumentException("Benchmark target integration must not be null.");
        }
        if (options == null) {
            throw new IllegalArgumentException("Benchmark options must not be null.");
        }
        final ArrayList<BenchmarkStageResult> stages = new ArrayList<>(9);
        if (options.includeImport()) {
            stages.add(measure(
                "import",
                options,
                () -> {
                    final ImportResult result = inputIntegration.importProgram(source);
                    return new StageRunResult(
                        result.isSuccess(),
                        result.isSuccess() ? "IMPORTED" : "IMPORT_FAILED"
                    );
                }
            ));
        }
        final ImportResult imported = inputIntegration.importProgram(source);
        if (!imported.isSuccess()) {
            return new ProductBenchmarkReport(
                inputIntegration.format(),
                targetIntegration.format(),
                stages
            );
        }
        stages.addAll(runProgramStages(
            inputIntegration.format(),
            imported.program(),
            targetIntegration,
            options
        ).stages());
        return new ProductBenchmarkReport(
            inputIntegration.format(),
            targetIntegration.format(),
            stages
        );
    }

    private ProductBenchmarkReport runProgramStages(
        final ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat inputFormat,
        final QuantumProgram program,
        final QuantumIntegration targetIntegration,
        final ProductBenchmarkOptions options
    ) {
        final ArrayList<BenchmarkStageResult> stages = new ArrayList<>(8);
        stages.add(measure(
            "validate",
            options,
            () -> {
                final ValidationResult result = new QuantumProgramValidator().validate(program);
                return new StageRunResult(
                    result.isValid(),
                    result.isValid() ? "VALID" : "INVALID"
                );
            }
        ));
        stages.add(measure(
            "inspect",
            options,
            () -> new StageRunResult(
                new QuantumProgramInspector().inspect(
                    program,
                    java.util.List.of(targetIntegration.capabilityProfile())
                ).diagnosticCount() == 0,
                "INSPECTED"
            )
        ));
        stages.add(measure(
            "preflight",
            options,
            () -> {
                final CapabilityPreflightResult result = new CapabilityPreflightChecker().check(
                    program,
                    targetIntegration.capabilityProfile()
                );
                return new StageRunResult(
                    result.isSuccess(),
                    result.status().name()
                );
            }
        ));
        stages.add(measure(
            "resources",
            options,
            () -> {
                new ResourceEstimator().estimate(
                    program,
                    options.workflowOptions().resourceMaxQubits()
                );
                return new StageRunResult(
                    true,
                    "ESTIMATED"
                );
            }
        ));
        stages.add(measure(
            "timeline",
            options,
            () -> {
                new QuantumProgramTimelineBuilder().build(program);
                return new StageRunResult(
                    true,
                    "BUILT"
                );
            }
        ));
        stages.add(measure(
            "simulate",
            options,
            () -> {
                final SimulationResult result = new QuantumSimulator().simulate(
                    program,
                    options.workflowOptions().simulationOptions()
                );
                return new StageRunResult(
                    result.isSuccess(),
                    result.isSuccess() ? "SIMULATED" : "SIMULATION_FAILED"
                );
            }
        ));
        stages.add(measure(
            "compile",
            options,
            () -> {
                final CompilerResult result = new QuantumCompiler().compile(
                    program,
                    targetIntegration,
                    CompilerOptions.defaults()
                );
                return new StageRunResult(
                    result.isSuccess(),
                    result.status().name()
                );
            }
        ));
        stages.add(measure(
            "workflow",
            options,
            () -> {
                final ProductWorkflowReport result = new ProductWorkflowRunner().run(
                    program,
                    targetIntegration,
                    options.workflowOptions()
                );
                return new StageRunResult(
                    result.isSuccess(),
                    result.status().name()
                );
            }
        ));
        return new ProductBenchmarkReport(
            inputFormat,
            targetIntegration.format(),
            stages
        );
    }

    private static BenchmarkStageResult measure(
        final String stage,
        final ProductBenchmarkOptions options,
        final Supplier<StageRunResult> run
    ) {
        StageRunResult last = new StageRunResult(
            true,
            "NOT_RUN"
        );
        for (int i = 0; i < options.warmupIterations(); i++) {
            last = run.get();
        }
        long min = Long.MAX_VALUE;
        long max = 0L;
        long total = 0L;
        final Runtime runtime = Runtime.getRuntime();
        final long beforeMemory = usedMemory(runtime);
        for (int i = 0; i < options.measurementIterations(); i++) {
            final long started = System.nanoTime();
            last = run.get();
            final long elapsed = Math.max(
                0L,
                System.nanoTime() - started
            );
            min = Math.min(
                min,
                elapsed
            );
            max = Math.max(
                max,
                elapsed
            );
            total = safeAdd(
                total,
                elapsed
            );
        }
        final long afterMemory = usedMemory(runtime);
        return new BenchmarkStageResult(
            stage,
            last.success(),
            last.status(),
            options.warmupIterations(),
            options.measurementIterations(),
            min,
            max,
            total / options.measurementIterations(),
            afterMemory - beforeMemory
        );
    }

    private static void validateProgramBenchmarkInputs(
        final QuantumProgram program,
        final QuantumIntegration targetIntegration,
        final ProductBenchmarkOptions options
    ) {
        if (program == null) {
            throw new IllegalArgumentException("Benchmark program must not be null.");
        }
        if (targetIntegration == null) {
            throw new IllegalArgumentException("Benchmark target integration must not be null.");
        }
        if (options == null) {
            throw new IllegalArgumentException("Benchmark options must not be null.");
        }
    }

    private static long usedMemory(final Runtime runtime) {
        return runtime.totalMemory() - runtime.freeMemory();
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

    private record StageRunResult(
        boolean success,
        String status
    ) {
    }
}
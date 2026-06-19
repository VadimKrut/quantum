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
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.pathcreator.vadim.quantum.application.backend.BackendJobOptions;
import ru.pathcreator.vadim.quantum.application.backend.BackendJobRecord;
import ru.pathcreator.vadim.quantum.application.backend.BackendJobStatus;
import ru.pathcreator.vadim.quantum.application.backend.BackendSubmissionResult;
import ru.pathcreator.vadim.quantum.application.backend.InMemoryBackendJobRegistry;
import ru.pathcreator.vadim.quantum.application.backend.QuantumBackend;
import ru.pathcreator.vadim.quantum.application.audit.ProductAuditReport;
import ru.pathcreator.vadim.quantum.application.benchmark.ProductBenchmarkOptions;
import ru.pathcreator.vadim.quantum.application.benchmark.ProductBenchmarkReport;
import ru.pathcreator.vadim.quantum.application.compatibility.ProductCompatibilityMatrix;
import ru.pathcreator.vadim.quantum.application.compatibility.TargetCompatibilityStatus;
import ru.pathcreator.vadim.quantum.application.compiler.CompilerResult;
import ru.pathcreator.vadim.quantum.application.compiler.CompilerResultStatus;
import ru.pathcreator.vadim.quantum.application.doctor.ProductDoctorReport;
import ru.pathcreator.vadim.quantum.application.distribution.ProductDistributionBundleResult;
import ru.pathcreator.vadim.quantum.application.distribution.ProductDistributionVerificationResult;
import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightStatus;
import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapabilityProfile;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.integration.options.ExportOptions;
import ru.pathcreator.vadim.quantum.application.integration.options.ImportOptions;
import ru.pathcreator.vadim.quantum.application.inspection.ProgramInspectionResult;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrFileWriteResult;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrReadResult;
import ru.pathcreator.vadim.quantum.application.regression.CorpusRegressionCase;
import ru.pathcreator.vadim.quantum.application.regression.CorpusRegressionReport;
import ru.pathcreator.vadim.quantum.application.readiness.ReleaseReadinessReport;
import ru.pathcreator.vadim.quantum.application.readiness.ReleaseReadinessStatus;
import ru.pathcreator.vadim.quantum.application.report.ProductReportBundleResult;
import ru.pathcreator.vadim.quantum.application.resource.ResourceEstimate;
import ru.pathcreator.vadim.quantum.application.simulation.options.SimulationOptions;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.application.transformation.TransformationOptions;
import ru.pathcreator.vadim.quantum.application.transformation.TransformationResult;
import ru.pathcreator.vadim.quantum.application.visualization.ProgramTimeline;
import ru.pathcreator.vadim.quantum.application.verification.CrossFormatVerificationReport;
import ru.pathcreator.vadim.quantum.application.workflow.ProductWorkflowOptions;
import ru.pathcreator.vadim.quantum.application.workflow.ProductWorkflowReport;
import ru.pathcreator.vadim.quantum.application.workflow.ProductWorkflowStatus;
import ru.pathcreator.vadim.quantum.api.workflow.QuantumExportWorkflowResult;
import ru.pathcreator.vadim.quantum.api.workflow.QuantumImportJsonWorkflowResult;
import ru.pathcreator.vadim.quantum.api.workflow.QuantumJsonExportWorkflowResult;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalAssignment;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalComparisonOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalDeclaration;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalType;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalTypeKind;
import ru.pathcreator.vadim.quantum.domain.callable.CallableArgument;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableGateOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableOperationBlock;
import ru.pathcreator.vadim.quantum.domain.gate.GateBodyOperation;
import ru.pathcreator.vadim.quantum.domain.gate.GateMatrix;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpressionKind;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.gate.modifier.GateModifier;
import ru.pathcreator.vadim.quantum.domain.gate.modifier.ModifiedGate;
import ru.pathcreator.vadim.quantum.domain.metadata.ExternalSource;
import ru.pathcreator.vadim.quantum.domain.metadata.OperationMetadata;
import ru.pathcreator.vadim.quantum.domain.metadata.SourceLocation;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.BranchOperation;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.OperationKind;
import ru.pathcreator.vadim.quantum.domain.operation.OperationBlock;
import ru.pathcreator.vadim.quantum.domain.operation.QuantumReference;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;
import ru.pathcreator.vadim.quantum.domain.timing.DurationExpression;
import ru.pathcreator.vadim.quantum.domain.timing.DurationUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuantumTest {

    @TempDir
    private Path tempDir;

    @Test
    void exposesStableFacadeForCoreWorkflow() {
        final QuantumProgram program = Quantum.gateBasedProgram();
        final QuantumCircuit circuit = program.createCircuit("bell");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            2
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            2
        );
        circuit.h(q.get(0))
            .cx(
                q.get(0),
                q.get(1)
            )
            .measure(
                q.get(0),
                c.get(0)
            );

        assertTrue(Quantum.validate(program).isValid());
        assertEquals(
            CapabilityPreflightStatus.EXPORTABLE,
            Quantum.preflight(
                IntegrationFormat.OPENQASM_3,
                program
            ).status()
        );

        final String json = Quantum.writeJson(program).content();
        final QuantumProgram restored = Quantum.readJson(json).program();

        assertTrue(Quantum.exportOpenQasm2(restored).isSuccess());
        assertTrue(Quantum.exportOpenQasm3(restored).isSuccess());
    }

    @Test
    void exposesConservativeTransformationFacade() {
        final QuantumProgram program = Quantum.gateBasedProgram();
        final QuantumCircuit circuit = program.createCircuit("transform");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.id(q.get(0));
        circuit.x(q.get(0));

        final TransformationResult result = Quantum.transform(
            program,
            TransformationOptions.builder()
                .removeIdentityGates()
                .build()
        );

        assertTrue(result.isSuccess());
        assertEquals(
            2,
            program.circuit(0).operationCount()
        );
        assertEquals(
            1,
            result.transformedProgram().circuit(0).operationCount()
        );
        assertEquals(
            StandardGate.X,
            ((GateOperation) result.transformedProgram().circuit(0).operation(0)).gate()
        );
    }

    @Test
    void exposesCompilerPipelineFacade() {
        final QuantumProgram program = Quantum.gateBasedProgram();
        final QuantumCircuit circuit = program.createCircuit("compile");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.h(q.get(0));

        final CompilerResult result = Quantum.compileOpenQasm3(program);

        assertTrue(result.isSuccess());
        assertEquals(
            CompilerResultStatus.EXPORTED,
            result.status()
        );
        assertTrue(result.exportResult().content().contains("OPENQASM 3.0"));
    }

    @Test
    void exposesLocalSimulationFacade() {
        final QuantumProgram program = Quantum.gateBasedProgram();
        final QuantumCircuit circuit = program.createCircuit("simulate");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            2
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            2
        );
        circuit.h(q.get(0))
            .cx(
                q.get(0),
                q.get(1)
            )
            .measure(
                q.get(0),
                c.get(0)
            )
            .measure(
                q.get(1),
                c.get(1)
            );

        final SimulationResult result = Quantum.simulate(
            program,
            SimulationOptions.builder()
                .shots(128)
                .seed(7L)
                .build()
        );

        assertTrue(result.isSuccess());
        assertEquals(
            128L,
            result.counts().get("00") + result.counts().get("11")
        );
    }

    @Test
    void exposesResourceEstimateFacade() {
        final QuantumProgram program = Quantum.gateBasedProgram();
        final QuantumCircuit circuit = program.createCircuit("resources");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            2
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            2
        );
        circuit.h(q.get(0))
            .cx(
                q.get(0),
                q.get(1)
            )
            .measure(
                q.get(0),
                c.get(0)
            )
            .measure(
                q.get(1),
                c.get(1)
            );

        final ResourceEstimate estimate = Quantum.estimateResources(
            program,
            20
        );

        assertEquals(
            2,
            estimate.qubitCount()
        );
        assertEquals(
            64L,
            estimate.estimatedStateVectorBytes()
        );
        assertTrue(estimate.isLocalSimulationFeasible());
    }

    @Test
    void exposesTimelineFacade() {
        final QuantumProgram program = Quantum.gateBasedProgram();
        final QuantumCircuit circuit = program.createCircuit("timeline");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.h(q.get(0));

        final ProgramTimeline timeline = Quantum.timeline(program);

        assertEquals(
            1,
            timeline.circuits().size()
        );
        assertEquals(
            "h",
            timeline.circuits().get(0).steps().get(0).label()
        );
    }

    @Test
    void exposesProductWorkflowFacade() {
        final QuantumProgram program = Quantum.programBuilder()
            .circuit("product_workflow")
            .qreg(
                "q",
                2
            )
            .creg(
                "c",
                2
            )
            .h("q[0]")
            .cx(
                "q[0]",
                "q[1]"
            )
            .measure(
                "q[0]",
                "c[0]"
            )
            .measure(
                "q[1]",
                "c[1]"
            )
            .build();

        final ProductWorkflowReport report = Quantum.runProductWorkflow(
            IntegrationFormat.OPENQASM_3,
            program
        );

        assertEquals(
            ProductWorkflowStatus.COMPLETED,
            report.status()
        );
        assertTrue(report.validation().isValid());
        assertTrue(report.preflight().isSuccess());
        assertTrue(report.simulation().isSuccess());
        assertTrue(report.compiler().isSuccess());
        assertTrue(report.hasBackendExecution());
        assertEquals(
            4,
            report.inspection().operationCount()
        );
    }

    @Test
    void exposesBenchmarkFacade() {
        final QuantumProgram program = Quantum.programBuilder()
            .circuit("benchmark")
            .qreg(
                "q",
                1
            )
            .h("q[0]")
            .build();

        final ProductBenchmarkReport report = Quantum.benchmark(
            IntegrationFormat.OPENQASM_3,
            program,
            ProductBenchmarkOptions.builder()
                .warmupIterations(0)
                .measurementIterations(1)
                .build()
        );

        assertTrue(report.isSuccess());
        assertEquals(
            IntegrationFormat.OPENQASM_3,
            report.targetFormat()
        );
        assertEquals(
            8,
            report.stageCount()
        );
        assertTrue(report.totalAverageNanos() >= 0L);
    }

    @Test
    void exposesCompatibilityMatrixFacade() {
        final QuantumProgram program = Quantum.programBuilder()
            .circuit("compatibility")
            .qreg(
                "q",
                2
            )
            .creg(
                "c",
                2
            )
            .h("q[0]")
            .cx(
                "q[0]",
                "q[1]"
            )
            .measure(
                "q[0]",
                "c[0]"
            )
            .measure(
                "q[1]",
                "c[1]"
            )
            .build();

        final ProductCompatibilityMatrix matrix = Quantum.compatibilityMatrix(program);

        assertTrue(matrix.validation().isValid());
        assertTrue(matrix.simulation().isSuccess());
        assertEquals(
            3,
            matrix.targets().size()
        );
        boolean hasExportableTarget = false;
        for (int index = 0; index < matrix.targets().size(); index++) {
            if (matrix.targets().get(index).status() == TargetCompatibilityStatus.EXPORTABLE) {
                hasExportableTarget = true;
            }
        }
        assertTrue(hasExportableTarget);
    }

    @Test
    void exposesCrossFormatVerificationFacade() {
        final CrossFormatVerificationReport report = Quantum.verifyCrossFormat(
            IntegrationFormat.OPENQASM_2,
            """
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[2];
            creg c[2];
            h q[0];
            cx q[0],q[1];
            measure q[0] -> c[0];
            measure q[1] -> c[1];
            """,
            SimulationOptions.builder()
                .shots(64)
                .seed(5L)
                .build()
        );

        assertTrue(report.importSuccess());
        assertTrue(report.validation().isValid());
        assertEquals(
            3,
            report.targets().size()
        );
        assertTrue(report.targets().get(1).simulationEquivalent());
    }

    @Test
    void exposesCorpusRegressionFacade() {
        final CorpusRegressionReport report = Quantum.runCorpusRegression(
            List.of(CorpusRegressionCase.of(
                "bell-openqasm2",
                """
                OPENQASM 2.0;
                include "qelib1.inc";
                qreg q[2];
                creg c[2];
                h q[0];
                cx q[0],q[1];
                measure q[0] -> c[0];
                measure q[1] -> c[1];
                """,
                Quantum.integration(IntegrationFormat.OPENQASM_2)
            )),
            ProductWorkflowOptions.builder()
                .simulationOptions(SimulationOptions.builder()
                    .shots(64)
                    .seed(6L)
                    .build())
                .runBackendDryRun(false)
                .build()
        );

        assertTrue(report.isSuccess());
        assertEquals(
            1,
            report.caseCount()
        );
        assertTrue(report.cases().get(0).compatibilityMatrix().isSuccess());
    }

    @Test
    void exposesReleaseReadinessFacade() {
        final ReleaseReadinessReport report = Quantum.releaseReadiness(
            List.of(CorpusRegressionCase.of(
                "bell-openqasm2",
                """
                OPENQASM 2.0;
                include "qelib1.inc";
                qreg q[2];
                creg c[2];
                h q[0];
                cx q[0],q[1];
                measure q[0] -> c[0];
                measure q[1] -> c[1];
                """,
                Quantum.integration(IntegrationFormat.OPENQASM_2)
            )),
            IntegrationFormat.OPENQASM_3,
            ProductBenchmarkOptions.builder()
                .warmupIterations(0)
                .measurementIterations(1)
                .workflowOptions(ProductWorkflowOptions.builder()
                    .simulationOptions(SimulationOptions.builder()
                        .shots(64)
                        .seed(6L)
                        .build())
                    .runBackendDryRun(false)
                    .build())
                .build()
        );

        assertEquals(
            ReleaseReadinessStatus.READY,
            report.status()
        );
        assertTrue(report.corpusRegression().isSuccess());
        assertTrue(report.hasBenchmark());
    }

    @Test
    void exposesDryRunBackendFacade() {
        final QuantumProgram program = Quantum.gateBasedProgram();
        final QuantumCircuit circuit = program.createCircuit("backend");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.h(q.get(0));

        final QuantumBackend backend = Quantum.dryRunBackend(IntegrationFormat.OPENQASM_3);
        final BackendSubmissionResult submission = backend.submit(
            program,
            BackendJobOptions.defaults()
        );

        assertTrue(Quantum.backendPreflight(
            program,
            backend.descriptor()
        ).isSuccess());
        assertTrue(submission.isAccepted());
        assertEquals(
            BackendJobStatus.COMPLETED,
            backend.result(submission.jobId()).status()
        );

        final InMemoryBackendJobRegistry registry = Quantum.backendJobRegistry();
        final BackendJobRecord record = registry.submit(
            backend,
            program,
            BackendJobOptions.defaults()
        );

        assertTrue(record.isAccepted());
        assertEquals(
            BackendJobStatus.COMPLETED,
            record.status()
        );
        assertEquals(
            1,
            registry.history().count()
        );
    }

    @Test
    void exposesTargetProfilesThroughPublicFacade() {
        final IntegrationCapabilityProfile profile = Quantum.openQasm3TargetProfile();

        assertEquals(
            "OpenQASM",
            profile.targetName()
        );
        assertEquals(
            "3.0",
            profile.targetVersion()
        );
        assertTrue(profile.supportsNativeGate("h"));
        assertTrue(profile.supportedParameterKinds().contains(ParameterExpressionKind.BINARY));
        assertEquals(
            "openqasm3",
            profile.metadata().get("adapter")
        );
    }

    @Test
    void exposesInspectionThroughPublicFacade() {
        final QuantumProgram program = Quantum.programBuilder()
            .circuit("inspect")
            .qreg(
                "q",
                2
            )
            .creg(
                "c",
                1
            )
            .h("q[0]")
            .cx(
                "q[0]",
                "q[1]"
            )
            .measure(
                "q[0]",
                "c[0]"
            )
            .build();

        final ProgramInspectionResult result = Quantum.inspect(
            program,
            List.of(Quantum.openQasm3TargetProfile())
        );

        assertEquals(
            1,
            result.circuitCount()
        );
        assertEquals(
            2,
            result.qubitCount()
        );
        assertEquals(
            3,
            result.operationCount()
        );
        assertEquals(
            2,
            result.gateCount()
        );
        assertEquals(
            1,
            result.measurementCount()
        );
        assertEquals(
            1,
            result.circuitSummary(0).twoQubitGateCount()
        );
        assertTrue(result.circuitSummary(0).neverMeasuredQubits().contains("q[1]"));
        assertEquals(
            1,
            result.targetCompatibilitySummaries().size()
        );
    }

    @Test
    void readsJsonFileThroughPublicStreamingPath() {
        final QuantumProgram program = Quantum.gateBasedProgram();
        final QuantumCircuit circuit = program.createCircuit("file_api");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.h(q.get(0));
        final Path path = tempDir.resolve("program.quantum.json");

        final QuantumIrFileWriteResult write = Quantum.writeJsonStreaming(
            path,
            program
        );
        final QuantumIrReadResult read = Quantum.readJson(path);

        assertTrue(write.isSuccess());
        assertTrue(read.isSuccess());
        assertEquals(
            1,
            read.program().circuit(0).operationCount()
        );
    }

    @Test
    void buildsGateBasedProgramThroughDsl() {
        final QuantumProgram program = Quantum.programBuilder()
            .circuit("dsl")
            .qreg(
                "q",
                3
            )
            .creg(
                "c",
                2
            )
            .h("q[0]")
            .cx(
                "q[0]",
                "q[1]"
            )
            .rz(
                ParameterExpression.divide(
                    ParameterExpression.pi(),
                    ParameterExpression.of(2.0)
                ),
                "q[1]"
            )
            .ccx(
                "q[0]",
                "q[1]",
                "q[2]"
            )
            .measure(
                "q[0]",
                "c[0]"
            )
            .measure(
                "q[1]",
                "c[1]"
            )
            .build();

        assertTrue(Quantum.validate(program).isValid());
        assertEquals(
            1,
            program.circuitCount()
        );
        assertEquals(
            6,
            program.circuit(0).operationCount()
        );
        assertEquals(
            OperationKind.MEASURE,
            program.circuit(0).operation(5).kind()
        );
        assertTrue(Quantum.exportOpenQasm3(program).isSuccess());
    }

    @Test
    void dslProducesSameIrAsManualDomainCode() {
        final QuantumProgram manual = Quantum.gateBasedProgram();
        final QuantumCircuit manualCircuit = manual.createCircuit("same_ir");
        final QuantumRegister q = manualCircuit.createQuantumRegister(
            "q",
            2
        );
        final ClassicalRegister c = manualCircuit.createClassicalRegister(
            "c",
            2
        );
        manualCircuit.h(q.get(0))
            .cx(
                q.get(0),
                q.get(1)
            )
            .measure(
                q.get(0),
                c.get(0)
            )
            .measure(
                q.get(1),
                c.get(1)
            );

        final QuantumProgram dsl = Quantum.programBuilder()
            .circuit("same_ir")
            .qreg(
                "q",
                2
            )
            .creg(
                "c",
                2
            )
            .h("q[0]")
            .cx(
                "q[0]",
                "q[1]"
            )
            .measure(
                "q[0]",
                "c[0]"
            )
            .measure(
                "q[1]",
                "c[1]"
            )
            .build();

        assertEquals(
            Quantum.writeJson(manual).content(),
            Quantum.writeJson(dsl).content()
        );
    }

    @Test
    void dslExposesEveryCurrentStandardGate() {
        final QuantumProgram program = Quantum.programBuilder()
            .circuit("standard_gates")
            .qreg(
                "q",
                3
            )
            .h("q[0]")
            .x("q[0]")
            .y("q[0]")
            .z("q[0]")
            .s("q[0]")
            .sdg("q[0]")
            .t("q[0]")
            .tdg("q[0]")
            .rx(
                0.1,
                "q[0]"
            )
            .ry(
                0.2,
                "q[0]"
            )
            .rz(
                0.3,
                "q[0]"
            )
            .phase(
                0.4,
                "q[0]"
            )
            .id("q[0]")
            .u(
                ParameterExpression.of(0.5),
                ParameterExpression.of(0.6),
                ParameterExpression.of(0.7),
                "q[0]"
            )
            .cx(
                "q[0]",
                "q[1]"
            )
            .cy(
                "q[0]",
                "q[1]"
            )
            .cz(
                "q[0]",
                "q[1]"
            )
            .ch(
                "q[0]",
                "q[1]"
            )
            .cphase(
                0.8,
                "q[0]",
                "q[1]"
            )
            .swap(
                "q[0]",
                "q[1]"
            )
            .ccx(
                "q[0]",
                "q[1]",
                "q[2]"
            )
            .build();

        assertTrue(Quantum.validate(program).isValid());
        assertEquals(
            StandardGate.values().length,
            program.circuit(0).operationCount()
        );
    }

    @Test
    void dslSupportsModifiedGatesAndDynamicReferences() {
        final QuantumCircuitBuilder circuit = Quantum.programBuilder()
            .circuit("dynamic_surface")
            .qreg(
                "q",
                2
            )
            .qreg(
                "buffer",
                4
            )
            .creg(
                "c",
                1
            );
        final QuantumReference dynamic = circuit.dynamicQubitReference(
            "buffer",
            ClassicalExpression.integer(2)
        );
        final ModifiedGate controlledX = ModifiedGate.of(
            StandardGate.X,
            List.of(GateModifier.controlled(1))
        );
        final QuantumProgram program = circuit
            .modifiedGate(
                controlledX,
                "q[0]",
                "q[1]"
            )
            .gateReferences(
                StandardGate.H,
                dynamic
            )
            .measureReference(
                dynamic,
                "c[0]"
            )
            .build();

        assertTrue(Quantum.validate(program).isValid());
        assertEquals(
            OperationKind.GATE,
            program.circuit(0).operation(1).kind()
        );
        assertEquals(
            dynamic,
            ((GateOperation) program.circuit(0).operation(1)).qubitReference(0)
        );
    }

    @Test
    void rejectsBadDslReferenceBeforeExport() {
        final QuantumCircuitBuilder circuit = Quantum.programBuilder()
            .circuit("bad_reference")
            .qreg(
                "q",
                1
            );

        assertThrows(
            IllegalArgumentException.class,
            () -> circuit.h("missing[0]")
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> circuit.h("q[2147483648]")
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> circuit.h("q")
        );
    }

    @Test
    void dslCoversStructuredTimingClassicalAndMetadataIrSurface() {
        final QuantumProgram program = Quantum.programBuilder()
            .intrinsicGate(
                "intrinsic_custom",
                1,
                0
            )
            .opaqueGate(
                "opaque_custom",
                List.of("theta"),
                List.of("q")
            )
            .compositeGate(
                "composite_custom",
                List.of(),
                List.of("q"),
                List.of(GateBodyOperation.of(
                    StandardGate.H,
                    new ParameterExpression[0],
                    "q"
                ))
            )
            .matrixGate(
                "matrix_custom",
                List.of(),
                List.of("q"),
                GateMatrix.of(new String[][] {
                    new String[] {
                        "1",
                        "0"
                    },
                    new String[] {
                        "0",
                        "1"
                    }
                })
            )
            .classicalDeclaration(new ClassicalDeclaration(
                "global_flag",
                ClassicalType.of(ClassicalTypeKind.BOOLEAN)
            ))
            .callableDefinition(
                "prepare",
                CallableOperationBlock.of(CallableGateOperation.of(
                    StandardGate.H,
                    "q"
                )),
                CallableArgument.qubit("q")
            )
            .externalCallableDeclaration(
                "cost",
                ClassicalType.sized(
                    ClassicalTypeKind.FLOAT,
                    64
                ),
                CallableArgument.classical(
                    "theta",
                    ClassicalType.sized(
                        ClassicalTypeKind.ANGLE,
                        64
                    )
                )
            )
            .calibrationDefinition(
                "intrinsic_custom",
                List.of(),
                List.of("q"),
                "pulse",
                "play q"
            )
            .circuit("full_surface")
            .qreg(
                "q",
                2
            )
            .creg(
                "c",
                2
            )
            .gateReferences(
                StandardGate.CX,
                QuantumReference.hardwareQubit(0),
                QuantumReference.hardwareQubit(1)
            )
            .measureReference(
                QuantumReference.hardwareQubit(0),
                "c[0]"
            )
            .assign(new ClassicalAssignment(
                ClassicalExpression.symbolicReference("temporary"),
                ClassicalExpression.integer(1)
            ))
            .classicallyControlled(
                ClassicalPredicate.compare(
                    ClassicalExpression.integer(1),
                    ClassicalComparisonOperator.EQUAL,
                    ClassicalExpression.integer(1)
                ),
                GateOperation.ofReferences(
                    StandardGate.H,
                    QuantumReference.hardwareQubit(1)
                )
            )
            .block(OperationBlock.of())
            .conditionalBlock(
                ClassicalPredicate.compare(
                    ClassicalExpression.integer(1),
                    ClassicalComparisonOperator.EQUAL,
                    ClassicalExpression.integer(1)
                ),
                OperationBlock.of(),
                OperationBlock.of()
            )
            .forLoop(
                "i",
                0,
                1,
                2,
                OperationBlock.of()
            )
            .delayReferences(
                DurationExpression.duration(
                    10,
                    DurationUnit.NS
                ),
                QuantumReference.hardwareQubit(0)
            )
            .timingBox(
                DurationExpression.stretch("stretch"),
                OperationBlock.of()
            )
            .label("after")
            .branch(BranchOperation.always("after"))
            .halt()
            .waitInstruction()
            .setOperationMetadata(
                0,
                new OperationMetadata(
                    new ExternalSource(
                        "dsl-test",
                        "full surface"
                    ),
                    new SourceLocation(
                        1,
                        1
                    )
                )
            )
            .build();

        assertEquals(
            4,
            program.gateDefinitionCount()
        );
        assertEquals(
            1,
            program.classicalDeclarationCount()
        );
        assertEquals(
            1,
            program.callableDefinitionCount()
        );
        assertEquals(
            1,
            program.externalCallableDeclarationCount()
        );
        assertEquals(
            1,
            program.calibrationDefinitionCount()
        );
        assertEquals(
            13,
            program.circuit(0).operationCount()
        );
        assertEquals(
            OperationKind.GATE,
            program.circuit(0).operation(0).kind()
        );
        assertEquals(
            OperationKind.WAIT,
            program.circuit(0).operation(12).kind()
        );
        assertEquals(
            "dsl-test",
            program.circuit(0).operationMetadata(0).source().format()
        );
    }

    @Test
    void workflowExportsProgramAfterValidationAndPreflight() {
        final QuantumProgram program = Quantum.programBuilder()
            .circuit("workflow_export")
            .qreg(
                "q",
                2
            )
            .creg(
                "c",
                2
            )
            .h("q[0]")
            .cx(
                "q[0]",
                "q[1]"
            )
            .measure(
                "q[0]",
                "c[0]"
            )
            .measure(
                "q[1]",
                "c[1]"
            )
            .build();

        final QuantumExportWorkflowResult result = Quantum.validatePreflightExportOpenQasm2(program);

        assertTrue(result.isSuccess());
        assertTrue(result.validationResult().isValid());
        assertEquals(
            CapabilityPreflightStatus.EXPORTABLE,
            result.preflightResult().status()
        );
        assertTrue(result.content().contains("OPENQASM 2.0"));
    }

    @Test
    void workflowBuildsProgramAndSupportsExplicitOptions() {
        final QuantumProgramBuilder builder = Quantum.programBuilder();
        builder.circuit("workflow_build")
            .qreg(
                "q",
                1
            )
            .creg(
                "c",
                1
            )
            .h("q[0]")
            .measure(
                "q[0]",
                "c[0]"
            )
            .endCircuit();

        final QuantumExportWorkflowResult result = Quantum.buildValidatePreflightExportOpenQasm3(
            builder,
            ExportOptions.defaults()
        );

        assertTrue(result.isSuccess());
        assertTrue(result.hasContent());
        assertTrue(Quantum.exportOpenQasm3(
            result.program(),
            ExportOptions.defaults()
        ).isSuccess());
    }

    @Test
    void workflowImportsValidatesAndWritesJsonThenExportsFromJson() {
        final Path path = tempDir.resolve("bell.quantum.json");
        final String source = """
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[2];
            creg c[2];
            h q[0];
            cx q[0],q[1];
            measure q[0] -> c[0];
            measure q[1] -> c[1];
            """;

        final QuantumImportJsonWorkflowResult importResult = Quantum.importOpenQasm2ValidateWriteJson(
            source,
            path,
            ImportOptions.defaults()
        );
        final QuantumJsonExportWorkflowResult exportResult = Quantum.readJsonValidatePreflightExportOpenQasm3(
            path,
            ExportOptions.defaults()
        );

        assertTrue(importResult.isSuccess());
        assertTrue(importResult.validationResult().isValid());
        assertTrue(importResult.writeResult().isSuccess());
        assertTrue(exportResult.isSuccess());
        assertTrue(exportResult.content().contains("OPENQASM 3.0"));
    }

    @Test
    void workflowStopsBeforePreflightWhenProgramIsInvalid() {
        final QuantumProgram program = Quantum.gateBasedProgram();
        final QuantumCircuit circuit = program.createCircuit("invalid");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.cx(
            q.get(0),
            q.get(0)
        );

        final QuantumExportWorkflowResult result = Quantum.validatePreflightExportQuil(program);

        assertTrue(!result.isSuccess());
        assertTrue(!result.validationResult().isValid());
        assertTrue(!result.hasPreflightResult());
        assertTrue(!result.hasExportResult());
    }

    @Test
    void exposesProductDoctorFacade() {
        final ProductDoctorReport report = Quantum.productDoctor(projectRoot());

        assertTrue(report.isAcceptable());
        assertEquals(0, report.failedCheckCount());
        assertTrue(report.checkCount() >= 6);
    }

    @Test
    void exposesProductAuditFacade() {
        final ProductAuditReport report = Quantum.productAudit(
            projectRoot(),
            List.of(CorpusRegressionCase.of(
                "bell-openqasm2",
                """
                OPENQASM 2.0;
                include "qelib1.inc";
                qreg q[2];
                creg c[2];
                h q[0];
                cx q[0],q[1];
                measure q[0] -> c[0];
                measure q[1] -> c[1];
                """,
                Quantum.integration(IntegrationFormat.OPENQASM_2)
            )),
            IntegrationFormat.OPENQASM_3,
            ProductBenchmarkOptions.builder()
                .warmupIterations(0)
                .measurementIterations(1)
                .build()
        );

        assertTrue(report.isAcceptable());
        assertEquals(0, report.failedCheckCount());
        assertTrue(report.readiness().isAcceptable());
    }

    @Test
    void writesProductReportBundleThroughFacade() throws Exception {
        final ProductAuditReport report = Quantum.productAudit(
            projectRoot(),
            List.of(CorpusRegressionCase.of(
                "bell-openqasm2",
                """
                OPENQASM 2.0;
                include "qelib1.inc";
                qreg q[2];
                creg c[2];
                h q[0];
                cx q[0],q[1];
                measure q[0] -> c[0];
                measure q[1] -> c[1];
                """,
                Quantum.integration(IntegrationFormat.OPENQASM_2)
            )),
            IntegrationFormat.OPENQASM_3,
            ProductBenchmarkOptions.builder()
                .warmupIterations(0)
                .measurementIterations(1)
                .build()
        );

        final ProductReportBundleResult result = Quantum.writeProductReportBundle(
            tempDir.resolve("product-report"),
            report,
            "{\"status\":\"READY\"}"
        );

        assertTrue(java.nio.file.Files.isRegularFile(result.auditJsonPath()));
        assertTrue(java.nio.file.Files.isRegularFile(result.summaryMarkdownPath()));
        assertTrue(java.nio.file.Files.isRegularFile(result.manifestPath()));
        assertTrue(Quantum.productReportSummary(report).contains("Quantum Product Report"));
    }

    @Test
    void writesProductDistributionBundleThroughFacade() throws Exception {
        final Path project = completeDistributionProject();

        final ProductDistributionBundleResult result = Quantum.writeProductDistributionBundle(
            tempDir.resolve("distribution"),
            project
        );

        assertTrue(java.nio.file.Files.isRegularFile(result.quickstartPath()));
        assertTrue(java.nio.file.Files.isRegularFile(result.archivePath()));
        assertEquals(64, result.archiveSha256().length());
        assertTrue(java.nio.file.Files.isRegularFile(result.manifestPath()));
        assertTrue(java.nio.file.Files.isRegularFile(result.librariesDirectory().resolve("quantum-cli-test.jar")));
        assertTrue(java.nio.file.Files.readString(result.manifestPath()).contains("format=quantum-product-distribution"));
        final ProductDistributionVerificationResult verification = Quantum.verifyProductDistributionBundle(result.outputDirectory());
        assertTrue(verification.isSuccess());
        assertTrue(verification.archivePresent());
    }

    private static Path projectRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (
                java.nio.file.Files.isRegularFile(current.resolve("pom.xml"))
                && java.nio.file.Files.isDirectory(current.resolve("quantum-api"))
            ) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Project root was not found.");
    }

    private Path completeDistributionProject() throws Exception {
        final Path project = tempDir.resolve("distribution-project");
        java.nio.file.Files.createDirectories(project);
        java.nio.file.Files.writeString(
            project.resolve("LICENSE"),
            "MPL-2.0"
        );
        java.nio.file.Files.writeString(
            project.resolve("README.md"),
            "# Quantum"
        );
        java.nio.file.Files.createDirectories(project.resolve("examples").resolve("openqasm2"));
        java.nio.file.Files.writeString(
            project.resolve("examples").resolve("openqasm2").resolve("bell.qasm"),
            "OPENQASM 2.0;"
        );
        writeDistributionTool(
            project,
            "quantum.ps1"
        );
        writeDistributionTool(
            project,
            "quantum-desktop.ps1"
        );
        writeDistributionTool(
            project,
            "product-smoke.ps1"
        );
        writeDistributionJar(
            project,
            "quantum-cli"
        );
        writeDistributionJar(
            project,
            "quantum-desktop"
        );
        return project;
    }

    private static void writeDistributionTool(
        final Path project,
        final String name
    ) throws Exception {
        final Path tool = project.resolve("tools").resolve(name);
        java.nio.file.Files.createDirectories(tool.getParent());
        java.nio.file.Files.writeString(
            tool,
            "Write-Output Quantum"
        );
    }

    private static void writeDistributionJar(
        final Path project,
        final String module
    ) throws Exception {
        final Path jar = project.resolve(module).resolve("target").resolve(module + "-test.jar");
        java.nio.file.Files.createDirectories(jar.getParent());
        java.nio.file.Files.writeString(
            jar,
            "jar"
        );
    }
}
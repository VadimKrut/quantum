/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.compiler;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapability;
import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapabilityProfile;
import ru.pathcreator.vadim.quantum.application.integration.capability.TargetConnectivityGraph;
import ru.pathcreator.vadim.quantum.application.integration.contract.QuantumExporter;
import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnostic;
import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnosticCode;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.integration.result.ExportResult;
import ru.pathcreator.vadim.quantum.application.transformation.TransformationOptions;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinition;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpressionKind;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumComputationModel;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuantumCompilerTest {

    @Test
    void runsEveryStageForExportableProgram() {
        final CompilerResult result = new QuantumCompiler().compile(
            bellProgram(),
            successExporter(fullProfile())
        );

        assertTrue(result.isSuccess());
        assertEquals(
            CompilerResultStatus.EXPORTED,
            result.status()
        );
        assertEquals(
            8,
            result.stageRecords().size()
        );
        assertEquals(
            CompilerStage.EXPORT,
            result.stageRecords().get(7).stage()
        );
        assertTrue(result.exportResult().content().contains("ok"));
    }

    @Test
    void stopsOnInitialValidationErrorByDefault() {
        final QuantumProgram program = QuantumProgram.create(QuantumComputationModel.ANALOG_SIMULATION);

        final CompilerResult result = new QuantumCompiler().compile(
            program,
            successExporter(fullProfile())
        );

        assertEquals(
            CompilerResultStatus.STOPPED_ON_VALIDATION,
            result.status()
        );
        assertFalse(result.hasExportResult());
        assertEquals(
            CompilerStage.INITIAL_VALIDATION,
            result.stageRecords().get(0).stage()
        );
    }

    @Test
    void stopsOnUnsupportedTargetBeforeTransformationByDefault() {
        final CompilerResult result = new QuantumCompiler().compile(
            bellProgram(),
            successExporter(IntegrationCapabilityProfile.empty(IntegrationFormat.OPENQASM_3))
        );

        assertEquals(
            CompilerResultStatus.STOPPED_ON_PREFLIGHT,
            result.status()
        );
        assertEquals(
            CompilerStage.INITIAL_PREFLIGHT,
            result.stageRecords().get(result.stageRecords().size() - 1).stage()
        );
    }

    @Test
    void continuesThroughLoweringRequiredPreflightWhenRepresentationIsStillPossible() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("lowering");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.x(q.get(0));

        final CompilerResult result = new QuantumCompiler().compile(
            program,
            successExporter(loweringProfile()),
            CompilerOptions.defaults()
        );

        assertEquals(
            CompilerResultStatus.EXPORTED,
            result.status()
        );
        assertEquals(
            StandardGate.X,
            ((GateOperation) result.transformedProgram().circuit(0).operation(0)).gate()
        );
        assertTrue(hasStageStatus(
            result,
            CompilerStage.INITIAL_PREFLIGHT,
            CompilerStageStatus.WARNING
        ));
    }

    @Test
    void stopsOnTransformationErrorWhenRequiredBindingIsMissing() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("missing");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.rx(
            ParameterExpression.named("theta"),
            q.get(0)
        );

        final CompilerResult result = new QuantumCompiler().compile(
            program,
            successExporter(fullProfile()),
            CompilerOptions.builder()
                .transformationOptions(TransformationOptions.builder()
                    .bindParameters(ru.pathcreator.vadim.quantum.domain.parameter.ParameterBindings.empty())
                    .requireCompleteParameterBinding()
                    .build())
                .build()
        );

        assertEquals(
            CompilerResultStatus.STOPPED_ON_TRANSFORMATION,
            result.status()
        );
        assertFalse(result.hasExportResult());
    }

    @Test
    void reportsExportFailureAsFinalStatus() {
        final CompilerResult result = new QuantumCompiler().compile(
            bellProgram(),
            failingExporter(fullProfile())
        );

        assertEquals(
            CompilerResultStatus.EXPORT_FAILED,
            result.status()
        );
        assertTrue(result.hasExportResult());
        assertFalse(result.exportResult().isSuccess());
    }

    @Test
    void fastExportOnlySkipsAnalysisStagesExplicitly() {
        final CompilerResult result = new QuantumCompiler().compile(
            bellProgram(),
            successExporter(IntegrationCapabilityProfile.empty(IntegrationFormat.OPENQASM_3)),
            CompilerOptions.builder()
                .fastExportOnly()
                .build()
        );

        assertTrue(result.isSuccess());
        assertEquals(
            CompilerResultStatus.EXPORTED,
            result.status()
        );
        assertEquals(
            8,
            result.stageRecords().size()
        );
        assertEquals(
            CompilerStageStatus.SKIPPED,
            result.stageRecords().get(0).status()
        );
        assertEquals(
            CompilerStageStatus.SUCCESS,
            result.stageRecords().get(7).status()
        );
        assertTrue(result.initialValidation() == null);
        assertTrue(result.initialPreflight() == null);
        assertTrue(result.transformation() == null);
    }

    private static QuantumProgram bellProgram() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("bell");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            2
        );
        circuit.createClassicalRegister(
            "c",
            2
        );
        circuit.h(q.get(0))
            .cx(
                q.get(0),
                q.get(1)
            );
        return program;
    }

    private static boolean hasStageStatus(
        final CompilerResult result,
        final CompilerStage stage,
        final CompilerStageStatus status
    ) {
        for (int i = 0; i < result.stageRecords().size(); i++) {
            if (
                result.stageRecords().get(i).stage() == stage
                && result.stageRecords().get(i).status() == status
            ) {
                return true;
            }
        }
        return false;
    }

    private static QuantumExporter successExporter(final IntegrationCapabilityProfile profile) {
        return new TestExporter(
            profile,
            true
        );
    }

    private static QuantumExporter failingExporter(final IntegrationCapabilityProfile profile) {
        return new TestExporter(
            profile,
            false
        );
    }

    private static IntegrationCapabilityProfile fullProfile() {
        return IntegrationCapabilityProfile.of(
            IntegrationFormat.OPENQASM_3,
            "compiler-test",
            "1",
            IntegrationCapabilityProfile.UNBOUNDED_QUBIT_COUNT,
            EnumSet.allOf(IntegrationCapability.class),
            Set.of(),
            Set.of(),
            TargetConnectivityGraph.allToAll(),
            Map.of()
        );
    }

    private static IntegrationCapabilityProfile loweringProfile() {
        return IntegrationCapabilityProfile.of(
            IntegrationFormat.OPENQASM_3,
            "compiler-lowering-test",
            "1",
            IntegrationCapabilityProfile.UNBOUNDED_QUBIT_COUNT,
            EnumSet.of(
                IntegrationCapability.QUANTUM_REGISTERS,
                IntegrationCapability.CLASSICAL_REGISTERS,
                IntegrationCapability.MEASUREMENTS,
                IntegrationCapability.RESET,
                IntegrationCapability.BARRIER,
                IntegrationCapability.GATE_DECOMPOSITION
            ),
            Set.of(StandardGate.H.gateName()),
            Set.of(),
            TargetConnectivityGraph.allToAll(),
            Map.of()
        );
    }

    private static final class TestExporter implements QuantumExporter {

        private final IntegrationCapabilityProfile profile;
        private final boolean success;

        private TestExporter(
            final IntegrationCapabilityProfile profile,
            final boolean success
        ) {
            this.profile = profile;
            this.success = success;
        }

        @Override
        public IntegrationFormat format() {
            return IntegrationFormat.OPENQASM_3;
        }

        @Override
        public IntegrationCapabilityProfile capabilityProfile() {
            return profile;
        }

        @Override
        public ExportResult exportProgram(
            final QuantumProgram program,
            final ru.pathcreator.vadim.quantum.application.integration.options.ExportOptions options
        ) {
            if (success) {
                return ExportResult.success(
                    format(),
                    "ok"
                );
            }
            return ExportResult.failure(
                format(),
                List.of(IntegrationDiagnostic.error(
                    IntegrationDiagnosticCode.UNSUPPORTED_TARGET_CAPABILITY,
                    "Export failed by test."
                ))
            );
        }
    }
}
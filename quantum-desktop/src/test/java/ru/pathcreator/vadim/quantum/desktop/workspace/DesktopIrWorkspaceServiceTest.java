/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.desktop.ui.layout.DesktopGateCatalogView;
import ru.pathcreator.vadim.quantum.desktop.workflow.DesktopExecutionOptions;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalComparisonOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinition;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.OperationBlock;

class DesktopIrWorkspaceServiceTest {

    private final DesktopIrWorkspaceService service = new DesktopIrWorkspaceService();

    @Test
    void buildsNativeIrWorkspaceAndRunsCoreActions() {
        final QuantumProgram program = service.buildProgram(
            "main",
            "q",
            2,
            "c",
            2,
            List.of(
                new DesktopIrOperationSpec(
                    "H",
                    "q[0]",
                    "q[1]",
                    "q[0]",
                    "c[0]",
                    Math.PI / 2.0
                ),
                new DesktopIrOperationSpec(
                    "CX",
                    "q[0]",
                    "q[1]",
                    "q[0]",
                    "c[0]",
                    Math.PI / 2.0
                ),
                new DesktopIrOperationSpec(
                    "MEASURE",
                    "q[0]",
                    "q[1]",
                    "q[0]",
                    "c[0]",
                    Math.PI / 2.0
                ),
                new DesktopIrOperationSpec(
                    "MEASURE",
                    "q[1]",
                    "q[0]",
                    "q[0]",
                    "c[1]",
                    Math.PI / 2.0
                )
            )
        );

        assertTrue(service.validate(program).isValid());
        assertEquals(
            4,
            program.circuit(0).operationCount()
        );
        assertTrue(service.writeJson(program).content().contains("\"computationModel\""));
        assertTrue(service.readJson(service.writeJson(program).content()).isSuccess());
        assertTrue(service.timeline(program).circuits().get(0).steps().size() >= 4);
        assertEquals(
            1,
            service.inspect(
                program,
                IntegrationFormat.OPENQASM_3
            ).circuitCount()
        );
        assertEquals(
            2,
            service.resources(
                program,
                24
            ).qubitCount()
        );
        assertTrue(service.preflight(
            program,
            IntegrationFormat.OPENQASM_3
        ).isSuccess());
        assertTrue(service.compatibility(
            program,
            128,
            7L,
            DesktopExecutionOptions.defaults()
        ).validation().isValid());
        assertTrue(service.transform(
            program,
            IntegrationFormat.OPENQASM_3,
            true,
            true,
            false,
            true
        ).isSuccess());
        assertTrue(service.generateJavaDsl(
            "main",
            "q",
            2,
            "c",
            2,
            programOperations()
        ).contains(".cx(\"q[0]\", \"q[1]\")"));
        assertTrue(service.simulate(
            program,
            128,
            7L
        ).isSuccess());
        assertTrue(service.export(
            program,
            IntegrationFormat.OPENQASM_3,
            DesktopExecutionOptions.defaults()
        ).isSuccess());
    }

    private static List<DesktopIrOperationSpec> programOperations() {
        return List.of(
            new DesktopIrOperationSpec(
                "H",
                "q[0]",
                "q[1]",
                "q[0]",
                "c[0]",
                Math.PI / 2.0
            ),
            new DesktopIrOperationSpec(
                "CX",
                "q[0]",
                "q[1]",
                "q[0]",
                "c[0]",
                Math.PI / 2.0
            )
        );
    }

    @Test
    void buildsRicherGraphicalOperationStream() {
        final QuantumProgram program = service.buildProgram(
            "graphical",
            "q",
            3,
            "c",
            3,
            List.of(
                new DesktopIrOperationSpec(
                    "RY",
                    "q[0]",
                    "q[1]",
                    "q[2]",
                    "c[0]",
                    Math.PI / 3.0
                ),
                new DesktopIrOperationSpec(
                    "CZ",
                    "q[0]",
                    "q[1]",
                    "q[2]",
                    "c[0]",
                    Math.PI / 2.0
                ),
                new DesktopIrOperationSpec(
                    "CCX",
                    "q[0]",
                    "q[1]",
                    "q[2]",
                    "c[0]",
                    Math.PI / 2.0
                ),
                new DesktopIrOperationSpec(
                    "BARRIER",
                    "q[0]",
                    "q[2]",
                    "q[1]",
                    "c[0]",
                    Math.PI / 2.0
                ),
                new DesktopIrOperationSpec(
                    "MEASURE",
                    "q[2]",
                    "q[0]",
                    "q[1]",
                    "c[2]",
                    Math.PI / 2.0
                )
            )
        );

        assertTrue(service.validate(program).isValid());
        assertEquals(
            5,
            program.circuit(0).operationCount()
        );
        assertTrue(service.writeJson(program).content().contains("\"graphical\""));
        assertTrue(service.export(
            program,
            IntegrationFormat.OPENQASM_3,
            DesktopExecutionOptions.defaults()
        ).isSuccess());
    }

    @Test
    void buildsDenseGateSpectrumOperationStream() {
        final List<DesktopIrOperationSpec> operations = denseGateSpectrumOperations();
        final QuantumProgram program = service.buildProgram(
            "dense",
            "q",
            5,
            "c",
            5,
            operations
        );

        assertTrue(service.validate(program).isValid());
        assertEquals(
            operations.size(),
            program.circuit(0).operationCount()
        );
        assertEquals(
            5,
            service.resources(
                program,
                24
            ).qubitCount()
        );
        assertTrue(service.writeJson(program).hasContent());
        assertTrue(service.readJson(service.writeJson(program).content()).isSuccess());
        assertTrue(service.preflight(
            program,
            IntegrationFormat.OPENQASM_3
        ).isSuccess());
        assertTrue(service.export(
            program,
            IntegrationFormat.OPENQASM_3,
            DesktopExecutionOptions.defaults()
        ).isSuccess());
    }

    @Test
    void denseGateSpectrumSurvivesDesktopCrossSystemWorkflows() {
        final QuantumProgram program = service.buildProgram(
            "dense",
            "q",
            5,
            "c",
            5,
            denseGateSpectrumOperations()
        );
        final String json = service.writeJson(program).content();
        final QuantumProgram fromJson = service.readJson(json).program();

        assertTrue(service.validate(fromJson).isValid());
        assertTrue(service.simulate(
            fromJson,
            128,
            7L
        ).isSuccess());
        for (IntegrationFormat format : IntegrationFormat.values()) {
            assertTrue(service.targetProfile(format).format() == format);
            assertTrue(service.preflight(
                fromJson,
                format
            ).isSuccess());
            assertTrue(service.export(
                fromJson,
                format,
                DesktopExecutionOptions.defaults()
            ).isSuccess());
        }
    }

    @Test
    void buildsExtendedDesktopIrSurfaceWithoutPretendingEveryTargetSupportsIt() {
        final List<DesktopIrOperationSpec> operations = List.of(
            fullOperation(
                "LABEL",
                "q[0]",
                "q[1]",
                "q[2]",
                "c[0]",
                Math.PI / 2.0,
                0.0,
                0.0,
                20.0,
                "NS",
                "entry"
            ),
            fullOperation(
                "BRANCH",
                "q[0]",
                "q[1]",
                "q[2]",
                "c[0]",
                Math.PI / 2.0,
                0.0,
                0.0,
                20.0,
                "NS",
                "entry"
            ),
            operation(
                "H",
                "q[0]"
            ),
            operation(
                "SDG",
                "q[0]"
            ),
            operation(
                "S",
                "q[0]"
            ),
            operation(
                "TDG",
                "q[1]"
            ),
            operation(
                "T",
                "q[1]"
            ),
            operation(
                "ID",
                "q[2]"
            ),
            fullOperation(
                "U",
                "q[2]",
                "q[0]",
                "q[1]",
                "c[0]",
                Math.PI / 3.0,
                Math.PI / 5.0,
                Math.PI / 7.0,
                20.0,
                "NS",
                "entry"
            ),
            rotation(
                "CPHASE",
                "q[0]",
                "q[1]",
                Math.PI / 4.0
            ),
            fullOperation(
                "DELAY",
                "q[0]",
                "q[2]",
                "q[1]",
                "c[0]",
                Math.PI / 2.0,
                0.0,
                0.0,
                35.0,
                "NS",
                "entry"
            ),
            fullOperation(
                "TIMING_BOX",
                "q[0]",
                "q[1]",
                "q[2]",
                "c[0]",
                Math.PI / 2.0,
                0.0,
                0.0,
                12.0,
                "US",
                "entry"
            ),
            operation(
                "WAIT",
                "q[0]"
            ),
            measure(
                "q[0]",
                "c[0]"
            ),
            operation(
                "HALT",
                "q[0]"
            )
        );
        final QuantumProgram program = service.buildProgram(
            "extended",
            "q",
            3,
            "c",
            3,
            operations
        );
        final DesktopIrProgramSnapshot snapshot = service.projectToGraphicalWorkspace(program);
        final String dsl = service.generateJavaDsl(
            snapshot.circuitName(),
            snapshot.quantumRegisterName(),
            snapshot.quantumRegisterSize(),
            snapshot.classicalRegisterName(),
            snapshot.classicalRegisterSize(),
            snapshot.operations()
        );

        assertTrue(service.validate(program).isValid());
        assertFalse(service.simulate(
            program,
            64,
            7L
        ).isSuccess());
        assertEquals(
            2,
            service.inspect(
                program,
                IntegrationFormat.OPENQASM_3
            ).timingOperationCount()
        );
        assertFalse(service.preflight(
            program,
            IntegrationFormat.OPENQASM_3
        ).isSuccess());
        assertFalse(service.export(
            program,
            IntegrationFormat.OPENQASM_3,
            DesktopExecutionOptions.defaults()
        ).isSuccess());
        assertTrue(snapshot.isComplete());
        assertEquals(
            operations.size(),
            snapshot.operations().size()
        );
        assertTrue(dsl.contains(".u(ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression.of("));
        assertTrue(dsl.contains(".cphase("));
        assertTrue(dsl.contains(".delay(ru.pathcreator.vadim.quantum.domain.timing.DurationExpression.duration("));
        assertTrue(dsl.contains(".label(\"entry\")"));
        assertTrue(dsl.contains(".branch(ru.pathcreator.vadim.quantum.domain.operation.BranchOperation.always(\"entry\"))"));
        assertTrue(dsl.contains(".timingBox(ru.pathcreator.vadim.quantum.domain.timing.DurationExpression.duration("));
        assertTrue(dsl.contains(".waitInstruction()"));
        assertTrue(dsl.contains(".halt()"));
    }

    @Test
    void buildsClassicalAndStructuredDesktopShortcutSurface() {
        final List<DesktopIrOperationSpec> operations = List.of(
            fullOperation(
                "ASSIGN",
                "q[0]",
                "q[0]",
                "q[0]",
                "c[0]",
                1.0,
                0.0,
                0.0,
                20.0,
                "NS",
                "assign"
            ),
            fullOperation(
                "IF_X",
                "q[0]",
                "q[0]",
                "q[0]",
                "c[0]",
                1.0,
                0.0,
                0.0,
                20.0,
                "NS",
                "if_x"
            ),
            fullOperation(
                "CTRL_X",
                "q[0]",
                "q[0]",
                "q[0]",
                "c[0]",
                1.0,
                0.0,
                0.0,
                20.0,
                "NS",
                "ctrl_x"
            ),
            fullOperation(
                "DECLARE",
                "q[0]",
                "q[0]",
                "q[0]",
                "c[0]",
                1.0,
                0.0,
                0.0,
                20.0,
                "NS",
                "flag"
            ),
            fullOperation(
                "ARRAY",
                "q[0]",
                "q[0]",
                "q[0]",
                "c[0]",
                2.0,
                0.0,
                0.0,
                20.0,
                "NS",
                "scratch"
            ),
            fullOperation(
                "CALL",
                "q[0]",
                "q[0]",
                "q[0]",
                "c[0]",
                0.0,
                0.0,
                0.0,
                20.0,
                "NS",
                "externalMarker"
            ),
            operation(
                "BLOCK",
                "q[0]"
            ),
            fullOperation(
                "IF_BLOCK",
                "q[0]",
                "q[0]",
                "q[0]",
                "c[0]",
                1.0,
                0.0,
                0.0,
                20.0,
                "NS",
                "if_block"
            ),
            fullOperation(
                "FOR",
                "q[0]",
                "q[0]",
                "q[0]",
                "c[0]",
                2.0,
                0.0,
                0.0,
                20.0,
                "NS",
                "i"
            ),
            fullOperation(
                "SYM_FOR",
                "q[0]",
                "q[0]",
                "q[0]",
                "c[0]",
                2.0,
                0.0,
                0.0,
                20.0,
                "NS",
                "j"
            ),
            operation(
                "WHILE",
                "q[0]"
            ),
            measure(
                "q[0]",
                "c[0]"
            )
        );
        final QuantumProgram program = service.buildProgram(
            "classical_surface",
            "q",
            1,
            "c",
            1,
            operations
        );
        final DesktopIrProgramSnapshot snapshot = service.projectToGraphicalWorkspace(program);
        final String dsl = service.generateJavaDsl(
            snapshot.circuitName(),
            snapshot.quantumRegisterName(),
            snapshot.quantumRegisterSize(),
            snapshot.classicalRegisterName(),
            snapshot.classicalRegisterSize(),
            snapshot.operations()
        );

        assertFalse(service.validate(program).isValid());
        assertTrue(service.simulate(
            program,
            64,
            7L
        ).isSuccess());
        assertEquals(
            operations.size(),
            snapshot.operations().size()
        );
        assertTrue(dsl.contains(".assign(new ru.pathcreator.vadim.quantum.domain.classical.ClassicalAssignment("));
        assertTrue(dsl.contains(".classicallyControlled("));
        assertTrue(dsl.contains(".controlled("));
        assertTrue(dsl.contains(".classicalDeclaration("));
        assertTrue(dsl.contains(".classicalArrayDeclaration("));
        assertTrue(dsl.contains(".callableInvocation("));
        assertTrue(dsl.contains(".block("));
        assertTrue(dsl.contains(".conditionalBlock("));
        assertTrue(dsl.contains(".forLoop("));
        assertTrue(dsl.contains(".symbolicForLoop("));
        assertTrue(dsl.contains(".whileLoop("));
    }

    @Test
    void everyVisualGateCatalogEntryBuildsProjectsAndRendersDsl() {
        final DesktopGateCatalogView catalog = new DesktopGateCatalogView();
        for (final String gate : catalog.gates()) {
            final QuantumProgram program = service.buildProgram(
                "gate_" + gate.toLowerCase(),
                "q",
                4,
                "c",
                4,
                List.of(fullOperation(
                    gate,
                    "q[0]",
                    "q[1]",
                    "q[2]",
                    "c[1]",
                    2.0,
                    0.25,
                    0.125,
                    12.0,
                    "NS",
                    "entry"
                ))
            );
            final DesktopIrProgramSnapshot snapshot = service.projectToGraphicalWorkspace(program);
            final String dsl = service.generateJavaDsl(
                snapshot.circuitName(),
                snapshot.quantumRegisterName(),
                snapshot.quantumRegisterSize(),
                snapshot.classicalRegisterName(),
                snapshot.classicalRegisterSize(),
                snapshot.operations()
            );

            assertEquals(
                1,
                program.circuit(0).operationCount(),
                gate
            );
            assertEquals(
                1,
                snapshot.operations().size(),
                gate
            );
            assertEquals(
                gate,
                snapshot.operations().get(0).gate(),
                gate
            );
            assertFalse(
                dsl.isBlank(),
                gate
            );
        }
    }

    @Test
    void projectsStructuredIrAsEditableNestedOperationTiles() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("structured");
        circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.createClassicalRegister(
            "c",
            1
        );
        circuit.block(OperationBlock.of(GateOperation.of(
            StandardGate.H,
            circuit.quantumRegister(0).get(0)
        )));

        final DesktopIrProgramSnapshot snapshot = service.projectToGraphicalWorkspace(program);

        assertTrue(snapshot.isComplete());
        assertEquals(
            1,
            snapshot.operations().size()
        );
        assertEquals(
            "BLOCK",
            snapshot.operations().get(0).gate()
        );
        assertEquals(
            1,
            snapshot.operations().get(0).bodyOperations().size()
        );
        assertEquals(
            "H",
            snapshot.operations().get(0).bodyOperations().get(0).gate()
        );
        assertTrue(snapshot.operations().get(0).label().contains("body 1"));
    }

    @Test
    void projectsUnsupportedGateAsReadOnlyTileInsteadOfDroppingIt() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("custom");
        circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.createClassicalRegister(
            "c",
            1
        );
        final GateDefinition customGate = GateDefinition.of(
            "custom_visual_gate",
            1,
            0
        );
        program.addGateDefinition(customGate);
        circuit.gate(
            customGate,
            circuit.quantumRegister(0).get(0)
        );

        final DesktopIrProgramSnapshot snapshot = service.projectToGraphicalWorkspace(program);

        assertFalse(snapshot.isComplete());
        assertEquals(
            1,
            snapshot.operations().size()
        );
        assertEquals(
            "IR:GATE",
            snapshot.operations().get(0).gate()
        );
        assertTrue(snapshot.diagnostics().get(0).contains("custom_visual_gate"));
    }

    @Test
    void projectsEditableConditionalBlockWithoutLosingPredicateFields() {
        final QuantumProgram program = service.buildProgram(
            "condition",
            "q",
            1,
            "c",
            3,
            List.of(fullOperation(
                "IF_BLOCK",
                "q[0]",
                "q[0]",
                "q[0]",
                "c[2]",
                7.0,
                0.0,
                0.0,
                20.0,
                "NS",
                "if_block"
            ))
        );

        final DesktopIrProgramSnapshot snapshot = service.projectToGraphicalWorkspace(program);

        assertTrue(snapshot.isComplete());
        assertEquals(
            "IF_BLOCK",
            snapshot.operations().get(0).gate()
        );
        assertEquals(
            "c[2]",
            snapshot.operations().get(0).classicalBit()
        );
        assertEquals(
            7.0,
            snapshot.operations().get(0).angle()
        );
    }

    @Test
    void projectsAndRebuildsComplexClassicalPredicateWithoutReadOnlyFallback() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("complex_condition");
        circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.createClassicalRegister(
            "c",
            2
        );
        final ClassicalPredicate predicate = ClassicalPredicate.or(
            ClassicalPredicate.compare(
                ClassicalExpression.bit(circuit.classicalRegister(0).get(0)),
                ClassicalComparisonOperator.EQUAL,
                ClassicalExpression.integer(1L)
            ),
            ClassicalPredicate.compare(
                ClassicalExpression.bit(circuit.classicalRegister(0).get(1)),
                ClassicalComparisonOperator.NOT_EQUAL,
                ClassicalExpression.integer(0L)
            )
        );
        circuit.conditionalBlock(
            predicate,
            OperationBlock.of(GateOperation.of(
                StandardGate.X,
                circuit.quantumRegister(0).get(0)
            )),
            null
        );

        final DesktopIrProgramSnapshot snapshot = service.projectToGraphicalWorkspace(program);
        final QuantumProgram rebuilt = service.buildProgram(
            snapshot.circuitName(),
            snapshot.quantumRegisterName(),
            snapshot.quantumRegisterSize(),
            snapshot.classicalRegisterName(),
            snapshot.classicalRegisterSize(),
            snapshot.operations()
        );
        final String dsl = service.generateJavaDsl(
            snapshot.circuitName(),
            snapshot.quantumRegisterName(),
            snapshot.quantumRegisterSize(),
            snapshot.classicalRegisterName(),
            snapshot.classicalRegisterSize(),
            snapshot.operations()
        );

        assertTrue(snapshot.isComplete());
        assertEquals(
            "IF_BLOCK",
            snapshot.operations().get(0).gate()
        );
        assertTrue(snapshot.operations().get(0).predicate() != null);
        assertTrue(snapshot.operations().get(0).label().contains("or"));
        assertTrue(service.validate(rebuilt).isValid());
        assertTrue(dsl.contains("ClassicalPredicate.or"));
        assertTrue(dsl.contains("NOT_EQUAL"));
    }

    @Test
    void buildsEditableWhilePredicateFromClassicalShortcutFields() {
        final QuantumProgram program = service.buildProgram(
            "editable_while",
            "q",
            1,
            "c",
            2,
            List.of(fullOperation(
                "WHILE",
                "q[0]",
                "q[0]",
                "q[0]",
                "c[1]",
                3.0,
                0.0,
                0.0,
                20.0,
                "NS",
                "loop",
                List.of(operation(
                    "X",
                    "q[0]"
                )),
                List.of()
            ))
        );
        final DesktopIrProgramSnapshot snapshot = service.projectToGraphicalWorkspace(program);
        final String dsl = service.generateJavaDsl(
            snapshot.circuitName(),
            snapshot.quantumRegisterName(),
            snapshot.quantumRegisterSize(),
            snapshot.classicalRegisterName(),
            snapshot.classicalRegisterSize(),
            snapshot.operations()
        );

        assertTrue(service.validate(program).isValid());
        assertEquals(
            "WHILE",
            snapshot.operations().get(0).gate()
        );
        assertEquals(
            "c[1]",
            snapshot.operations().get(0).classicalBit()
        );
        assertEquals(
            3.0,
            snapshot.operations().get(0).angle()
        );
        assertTrue(snapshot.operations().get(0).label().contains("c[1] == 3"));
        assertTrue(dsl.contains(".whileLoop("));
        assertTrue(dsl.contains("ClassicalExpression.bit(bit(\"c[1]\")"));
    }

    @Test
    void buildsValidSimulatableProgramFromNestedVisualBlocks() {
        final DesktopIrOperationSpec nestedX = operation(
            "X",
            "q[0]"
        );
        final DesktopIrOperationSpec conditional = fullOperation(
            "IF_BLOCK",
            "q[0]",
            "q[0]",
            "q[0]",
            "c[0]",
            1.0,
            0.0,
            0.0,
            20.0,
            "NS",
            "if_block",
            List.of(nestedX),
            List.of()
        );
        final DesktopIrOperationSpec loop = fullOperation(
            "FOR",
            "q[0]",
            "q[0]",
            "q[0]",
            "c[0]",
            1.0,
            0.0,
            0.0,
            20.0,
            "NS",
            "i",
            List.of(nestedX),
            List.of()
        );
        final QuantumProgram program = service.buildProgram(
            "nested",
            "q",
            1,
            "c",
            1,
            List.of(
                fullOperation(
                    "ASSIGN",
                    "q[0]",
                    "q[0]",
                    "q[0]",
                    "c[0]",
                    1.0,
                    0.0,
                    0.0,
                    20.0,
                    "NS",
                    "assign"
                ),
                conditional,
                loop,
                measure(
                    "q[0]",
                    "c[0]"
                )
            )
        );
        final DesktopIrProgramSnapshot snapshot = service.projectToGraphicalWorkspace(program);
        final String dsl = service.generateJavaDsl(
            snapshot.circuitName(),
            snapshot.quantumRegisterName(),
            snapshot.quantumRegisterSize(),
            snapshot.classicalRegisterName(),
            snapshot.classicalRegisterSize(),
            snapshot.operations()
        );

        assertTrue(service.validate(program).isValid());
        assertTrue(service.transform(
            program,
            IntegrationFormat.OPENQASM_3,
            true,
            true,
            false,
            true
        ).isSuccess());
        assertEquals(
            1,
            service.inspect(
                program,
                IntegrationFormat.OPENQASM_3
            ).circuitCount()
        );
        assertTrue(service.simulate(
            program,
            64,
            7L
        ).isSuccess());
        assertEquals(
            64L,
            service.simulate(
                program,
                64,
                7L
            ).counts().get("1")
        );
        assertEquals(
            1,
            snapshot.operations().get(1).bodyOperations().size()
        );
        assertEquals(
            1,
            snapshot.operations().get(2).bodyOperations().size()
        );
        assertTrue(dsl.contains("OperationBlock.of(ru.pathcreator.vadim.quantum.domain.operation.GateOperation.of"));
        assertTrue(dsl.contains(".conditionalBlock("));
        assertTrue(dsl.contains(".forLoop("));
    }

    @Test
    void projectedStructuredIrKeepsRealProgramActiveThroughJsonWorkflow() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("structured");
        circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.createClassicalRegister(
            "c",
            1
        );
        circuit.block(OperationBlock.of(GateOperation.of(
            StandardGate.H,
            circuit.quantumRegister(0).get(0)
        )));

        final String json = service.writeJson(program).content();
        final QuantumProgram restored = service.readJson(json).program();
        final DesktopIrProgramSnapshot snapshot = service.projectToGraphicalWorkspace(restored);

        assertTrue(service.validate(restored).isValid());
        assertEquals(
            "BLOCK",
            snapshot.operations().get(0).gate()
        );
        assertEquals(
            1,
            snapshot.operations().get(0).bodyOperations().size()
        );
        assertEquals(
            2,
            service.inspect(
                restored,
                IntegrationFormat.OPENQASM_3
            ).operationCount()
        );
    }

    @Test
    void projectsNativeJsonBackIntoGraphicalWorkspaceAndDsl() {
        final QuantumProgram program = service.buildProgram(
            "dense",
            "q",
            5,
            "c",
            5,
            denseGateSpectrumOperations()
        );
        final String json = service.writeJson(program).content();
        final QuantumProgram fromJson = service.readJson(json).program();
        final DesktopIrProgramSnapshot snapshot = service.projectToGraphicalWorkspace(fromJson);
        final String dsl = service.generateJavaDsl(
            snapshot.circuitName(),
            snapshot.quantumRegisterName(),
            snapshot.quantumRegisterSize(),
            snapshot.classicalRegisterName(),
            snapshot.classicalRegisterSize(),
            snapshot.operations()
        );

        assertTrue(snapshot.isComplete());
        assertEquals(
            "dense",
            snapshot.circuitName()
        );
        assertEquals(
            5,
            snapshot.quantumRegisterSize()
        );
        assertEquals(
            denseGateSpectrumOperations().size(),
            snapshot.operations().size()
        );
        assertTrue(dsl.contains(".ccx(\"q[0]\", \"q[2]\", \"q[4]\")"));
        assertTrue(dsl.contains(".measure(\"q[4]\", \"c[4]\")"));
        assertTrue(service.validate(service.buildProgram(
            snapshot.circuitName(),
            snapshot.quantumRegisterName(),
            snapshot.quantumRegisterSize(),
            snapshot.classicalRegisterName(),
            snapshot.classicalRegisterSize(),
            snapshot.operations()
        )).isValid());
    }

    private static List<DesktopIrOperationSpec> denseGateSpectrumOperations() {
        final java.util.ArrayList<DesktopIrOperationSpec> operations = new java.util.ArrayList<>();
        for (int round = 0; round < 4; round++) {
            operations.add(operation(
                "H",
                "q[0]"
            ));
            operations.add(operation(
                "X",
                "q[1]"
            ));
            operations.add(operation(
                "Y",
                "q[2]"
            ));
            operations.add(operation(
                "Z",
                "q[3]"
            ));
            operations.add(operation(
                "S",
                "q[4]"
            ));
            operations.add(operation(
                "T",
                "q[0]"
            ));
            operations.add(rotation(
                "RX",
                "q[1]",
                Math.PI / (round + 2.0)
            ));
            operations.add(rotation(
                "RY",
                "q[2]",
                Math.PI / (round + 3.0)
            ));
            operations.add(rotation(
                "RZ",
                "q[3]",
                Math.PI / (round + 4.0)
            ));
            operations.add(rotation(
                "PHASE",
                "q[4]",
                Math.PI / (round + 5.0)
            ));
            operations.add(operation(
                "CX",
                "q[0]",
                "q[1]"
            ));
            operations.add(operation(
                "CY",
                "q[1]",
                "q[2]"
            ));
            operations.add(operation(
                "CZ",
                "q[2]",
                "q[3]"
            ));
            operations.add(operation(
                "CH",
                "q[3]",
                "q[4]"
            ));
            operations.add(operation(
                "SWAP",
                "q[0]",
                "q[4]"
            ));
            operations.add(new DesktopIrOperationSpec(
                "CCX",
                "q[0]",
                "q[2]",
                "q[4]",
                "c[0]",
                Math.PI / 2.0
            ));
            operations.add(operation(
                "BARRIER",
                "q[1]",
                "q[3]"
            ));
        }
        operations.add(operation(
            "RESET",
            "q[4]"
        ));
        operations.add(measure(
            "q[0]",
            "c[0]"
        ));
        operations.add(measure(
            "q[1]",
            "c[1]"
        ));
        operations.add(measure(
            "q[2]",
            "c[2]"
        ));
        operations.add(measure(
            "q[3]",
            "c[3]"
        ));
        operations.add(measure(
            "q[4]",
            "c[4]"
        ));
        return operations;
    }

    private static DesktopIrOperationSpec operation(
        final String gate,
        final String primaryQubit
    ) {
        return new DesktopIrOperationSpec(
            gate,
            primaryQubit,
            "q[0]",
            "q[0]",
            "c[0]",
            Math.PI / 2.0
        );
    }

    private static DesktopIrOperationSpec operation(
        final String gate,
        final String primaryQubit,
        final String secondaryQubit
    ) {
        return new DesktopIrOperationSpec(
            gate,
            primaryQubit,
            secondaryQubit,
            "q[0]",
            "c[0]",
            Math.PI / 2.0
        );
    }

    private static DesktopIrOperationSpec rotation(
        final String gate,
        final String primaryQubit,
        final String secondaryQubit,
        final double angle
    ) {
        return new DesktopIrOperationSpec(
            gate,
            primaryQubit,
            secondaryQubit,
            "q[0]",
            "c[0]",
            angle
        );
    }

    private static DesktopIrOperationSpec rotation(
        final String gate,
        final String qubit,
        final double angle
    ) {
        return new DesktopIrOperationSpec(
            gate,
            qubit,
            "q[0]",
            "q[0]",
            "c[0]",
            angle
        );
    }

    private static DesktopIrOperationSpec measure(
        final String qubit,
        final String bit
    ) {
        return new DesktopIrOperationSpec(
            "MEASURE",
            qubit,
            "q[0]",
            "q[0]",
            bit,
            Math.PI / 2.0
        );
    }

    private static DesktopIrOperationSpec fullOperation(
        final String gate,
        final String primaryQubit,
        final String secondaryQubit,
        final String tertiaryQubit,
        final String classicalBit,
        final double angle,
        final double secondAngle,
        final double thirdAngle,
        final double durationValue,
        final String durationUnit,
        final String labelName
    ) {
        return new DesktopIrOperationSpec(
            gate,
            primaryQubit,
            secondaryQubit,
            tertiaryQubit,
            classicalBit,
            angle,
            secondAngle,
            thirdAngle,
            durationValue,
            durationUnit,
            labelName
        );
    }

    private static DesktopIrOperationSpec fullOperation(
        final String gate,
        final String primaryQubit,
        final String secondaryQubit,
        final String tertiaryQubit,
        final String classicalBit,
        final double angle,
        final double secondAngle,
        final double thirdAngle,
        final double durationValue,
        final String durationUnit,
        final String labelName,
        final List<DesktopIrOperationSpec> bodyOperations,
        final List<DesktopIrOperationSpec> elseOperations
    ) {
        return new DesktopIrOperationSpec(
            gate,
            primaryQubit,
            secondaryQubit,
            tertiaryQubit,
            classicalBit,
            angle,
            secondAngle,
            thirdAngle,
            durationValue,
            durationUnit,
            labelName,
            bodyOperations,
            elseOperations
        );
    }
}
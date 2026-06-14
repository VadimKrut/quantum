/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.validation;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.domain.callable.CallableArgument;
import ru.pathcreator.vadim.quantum.domain.callable.ExternalCallableDeclaration;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalAssignment;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalComparisonOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.gate.DistinctQubitsGateValidationRule;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinition;
import ru.pathcreator.vadim.quantum.domain.gate.GateValidationRule;
import ru.pathcreator.vadim.quantum.domain.gate.GateValidationRuleErrorCollector;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.gate.modifier.GateModifier;
import ru.pathcreator.vadim.quantum.domain.gate.modifier.ModifiedGate;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumComputationModel;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.CallableInvocationOperation;
import ru.pathcreator.vadim.quantum.domain.operation.QuantumReference;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuantumProgramValidatorTest {

    @Test
    void acceptsValidBellCircuit() {
        final QuantumProgram program = QuantumProgram.gateBased();
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
            )
            .measure(
                q.get(1),
                c.get(1)
            );

        final ValidationResult result = new QuantumProgramValidator().validate(program);

        assertTrue(result.isValid());
        assertEquals(
            0,
            result.errorCount()
        );
    }

    @Test
    void acceptsClassicalAssignmentAndPredicateControlOperations() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("classical_ops");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            1
        );

        circuit.assign(new ClassicalAssignment(
            ClassicalExpression.bit(c.get(0)),
            ClassicalExpression.integer(1)
        ))
            .classicallyControlled(
                ClassicalPredicate.compare(
                    ClassicalExpression.register(c),
                    ClassicalComparisonOperator.EQUAL,
                    ClassicalExpression.integer(1)
                ),
                GateOperation.of(
                    StandardGate.X,
                    q.get(0)
                )
            );

        final ValidationResult result = new QuantumProgramValidator().validate(program);

        assertTrue(result.isValid());
    }

    @Test
    void acceptsDynamicGateBasedQubitReferences() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("dynamic_refs");
        final QuantumRegister buffer = circuit.createQuantumRegister(
            "buffer",
            4
        );
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            1
        );
        final QuantumReference bufferAtAddress = QuantumReference.dynamicIndex(
            buffer,
            ClassicalExpression.variable("address")
        );

        circuit.gateReferences(
            StandardGate.CY,
            bufferAtAddress,
            QuantumReference.staticQubit(q.get(0))
        );
        circuit.measureReference(
            bufferAtAddress,
            c.get(0)
        );
        circuit.resetReference(bufferAtAddress);

        final ValidationResult result = new QuantumProgramValidator().validate(program);

        assertTrue(result.isValid());
        assertEquals(
            3,
            circuit.operationCount()
        );
    }

    @Test
    void acceptsDeclaredExternalCallableInvocation() {
        final QuantumProgram program = QuantumProgram.gateBased();
        program.addExternalCallableDeclaration(new ExternalCallableDeclaration(
            "notify",
            null,
            CallableArgument.classical(
                "shot",
                ru.pathcreator.vadim.quantum.domain.classical.ClassicalType.sized(
                    ru.pathcreator.vadim.quantum.domain.classical.ClassicalTypeKind.SIGNED_INTEGER,
                    32
                )
            )
        ));
        final QuantumCircuit circuit = program.createCircuit("main");
        circuit.callableInvocation(new CallableInvocationOperation(
            "notify",
            null,
            List.of(ClassicalExpression.integer(1)),
            List.of()
        ));

        final ValidationResult result = new QuantumProgramValidator().validate(program);

        assertTrue(result.isValid());
    }

    @Test
    void rejectsUndeclaredCallableInvocation() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("main");
        circuit.callableInvocation(new CallableInvocationOperation(
            "missing",
            null,
            List.of(),
            List.of()
        ));

        final ValidationResult result = new QuantumProgramValidator().validate(program);

        assertFalse(result.isValid());
        assertEquals(
            ValidationErrorCode.UNDECLARED_CALLABLE,
            result.error(0).code()
        );
    }

    @Test
    void rejectsCallableInvocationArgumentMismatch() {
        final QuantumProgram program = QuantumProgram.gateBased();
        program.addExternalCallableDeclaration(new ExternalCallableDeclaration(
            "notify",
            null,
            CallableArgument.classical(
                "shot",
                ru.pathcreator.vadim.quantum.domain.classical.ClassicalType.sized(
                    ru.pathcreator.vadim.quantum.domain.classical.ClassicalTypeKind.SIGNED_INTEGER,
                    32
                )
            )
        ));
        final QuantumCircuit circuit = program.createCircuit("main");
        circuit.callableInvocation(new CallableInvocationOperation(
            "notify",
            null,
            List.of(),
            List.of()
        ));

        final ValidationResult result = new QuantumProgramValidator().validate(program);

        assertFalse(result.isValid());
        assertEquals(
            ValidationErrorCode.INVALID_CALLABLE_ARGUMENT_COUNT,
            result.error(0).code()
        );
    }

    @Test
    void rejectsClassicalPredicateValueOutsideRegisterRange() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("classical_predicate_range");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            1
        );

        circuit.classicallyControlled(
            ClassicalPredicate.compare(
                ClassicalExpression.register(c),
                ClassicalComparisonOperator.EQUAL,
                ClassicalExpression.integer(2)
            ),
            GateOperation.of(
                StandardGate.X,
                q.get(0)
            )
        );

        final ValidationResult result = new QuantumProgramValidator().validate(program);

        assertFalse(result.isValid());
        assertContains(
            result,
            ValidationErrorCode.CLASSICAL_CONDITION_VALUE_OUT_OF_RANGE
        );
    }

    @Test
    void rejectsNullProgram() {
        final ValidationResult result = new QuantumProgramValidator().validate(null);

        assertFalse(result.isValid());
        assertEquals(
            ValidationErrorCode.NULL_PROGRAM,
            result.error(0).code()
        );
    }

    @Test
    void rejectsUnsupportedComputationModel() {
        final QuantumProgram program = QuantumProgram.create(QuantumComputationModel.PULSE_LEVEL);

        final ValidationResult result = new QuantumProgramValidator().validate(program);

        assertFalse(result.isValid());
        assertEquals(
            ValidationErrorCode.UNSUPPORTED_COMPUTATION_MODEL,
            result.error(0).code()
        );
    }

    @Test
    void rejectsCxWithSameControlAndTargetQubit() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("invalid_cx");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );

        circuit.cx(
            q.get(0),
            q.get(0)
        );

        final ValidationResult result = new QuantumProgramValidator().validate(program);

        assertFalse(result.isValid());
        assertEquals(
            1,
            result.errorCount()
        );
        assertEquals(
            ValidationErrorCode.DUPLICATE_QUBIT_IN_GATE_OPERATION,
            result.error(0).code()
        );
        assertEquals(
            0,
            result.error(0).circuitIndex()
        );
        assertEquals(
            0,
            result.error(0).operationIndex()
        );
    }

    @Test
    void rejectsDuplicateQubitsForAnyMultiQubitGate() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("custom_duplicate");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        final GateDefinition customGate = GateDefinition.of(
            "custom_two_qubit_gate",
            2,
            0
        );
        program.addGateDefinition(customGate);

        circuit.gate(
            customGate,
            q.get(0),
            q.get(0)
        );

        final ValidationResult result = new QuantumProgramValidator().validate(program);

        assertFalse(result.isValid());
        assertEquals(
            ValidationErrorCode.DUPLICATE_QUBIT_IN_GATE_OPERATION,
            result.error(0).code()
        );
    }

    @Test
    void allowsDuplicateQubitsWhenCustomGateExplicitlyAllowsThem() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("repeatable_custom");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        final GateDefinition customGate = GateDefinition.of(
            "repeatable_two_qubit_gate",
            2,
            0,
            List.of()
        );
        program.addGateDefinition(customGate);

        circuit.gate(
            customGate,
            q.get(0),
            q.get(0)
        );

        final ValidationResult result = new QuantumProgramValidator().validate(program);

        assertTrue(result.isValid());
    }

    @Test
    void appliesCustomGateValidationRulesWithoutKnowingTheirDetails() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("custom_rule");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        final GateDefinition customGate = GateDefinition.of(
            "custom_rule_gate",
            1,
            0,
            List.of(new AlwaysInvalidGateValidationRule())
        );
        program.addGateDefinition(customGate);

        circuit.gate(
            customGate,
            q.get(0)
        );

        final ValidationResult result = new QuantumProgramValidator().validate(program);

        assertFalse(result.isValid());
        assertEquals(
            ValidationErrorCode.OPERATION_NOT_SUPPORTED_BY_GATE_BASED_MODEL,
            result.error(0).code()
        );
        assertEquals(
            "Custom gate rule rejected this operation.",
            result.error(0).message()
        );
    }

    @Test
    void collectsMultipleValidationErrorsWithoutStoppingAtFirstError() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("many_errors");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        final GateDefinition customGate = GateDefinition.of(
            "always_invalid",
            2,
            0,
            List.of(
                DistinctQubitsGateValidationRule.INSTANCE,
                new AlwaysInvalidGateValidationRule()
            )
        );
        program.addGateDefinition(customGate);

        circuit.gate(
            customGate,
            q.get(0),
            q.get(0)
        );

        final ValidationResult result = new QuantumProgramValidator().validate(program);

        assertFalse(result.isValid());
        assertEquals(
            2,
            result.errorCount()
        );
        assertEquals(
            ValidationErrorCode.DUPLICATE_QUBIT_IN_GATE_OPERATION,
            result.error(0).code()
        );
        assertEquals(
            ValidationErrorCode.OPERATION_NOT_SUPPORTED_BY_GATE_BASED_MODEL,
            result.error(1).code()
        );
    }

    @Test
    void attachesCustomRuleErrorsToCircuitAndOperationIndexes() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit firstCircuit = program.createCircuit("first");
        final QuantumCircuit secondCircuit = program.createCircuit("second");
        firstCircuit.createQuantumRegister(
            "q",
            1
        );
        final QuantumRegister q = secondCircuit.createQuantumRegister(
            "q",
            1
        );
        final GateDefinition customGate = GateDefinition.of(
            "custom_rule_gate",
            1,
            0,
            List.of(new AlwaysInvalidGateValidationRule())
        );
        program.addGateDefinition(customGate);

        secondCircuit.h(q.get(0))
            .gate(
                customGate,
                q.get(0)
            );

        final ValidationResult result = new QuantumProgramValidator().validate(program);

        assertFalse(result.isValid());
        assertEquals(
            1,
            result.errorCount()
        );
        assertEquals(
            1,
            result.error(0).circuitIndex()
        );
        assertEquals(
            1,
            result.error(0).operationIndex()
        );
    }

    @Test
    void rejectsCircuitOperationWithUndeclaredCustomGate() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("undeclared_gate");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        final GateDefinition customGate = GateDefinition.opaque(
            "missing_program_definition",
            List.of(),
            List.of("q")
        );

        circuit.gate(
            customGate,
            q.get(0)
        );

        final ValidationResult result = new QuantumProgramValidator().validate(program);

        assertFalse(result.isValid());
        assertContains(
            result,
            ValidationErrorCode.UNDECLARED_GATE_DEFINITION
        );
    }

    @Test
    void acceptsCircuitOperationWithUndeclaredIntrinsicGate() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("intrinsic_gate");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        final GateDefinition intrinsicGate = GateDefinition.of(
            "external_intrinsic",
            1,
            0
        );

        circuit.gate(
            intrinsicGate,
            q.get(0)
        );

        final ValidationResult result = new QuantumProgramValidator().validate(program);

        assertTrue(result.isValid());
    }

    @Test
    void acceptsCircuitOperationWithDeclaredModifiedCustomGate() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("declared_modified_gate");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            2
        );
        final GateDefinition customGate = GateDefinition.of(
            "declared_custom",
            1,
            0
        );
        program.addGateDefinition(customGate);

        circuit.gate(
            ModifiedGate.of(
                customGate,
                List.of(GateModifier.controlled(1))
            ),
            q.get(0),
            q.get(1)
        );

        final ValidationResult result = new QuantumProgramValidator().validate(program);

        assertTrue(result.isValid());
    }

    @Test
    void rejectsCircuitOperationWithUndeclaredModifiedCustomGate() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("undeclared_modified_gate");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            2
        );
        final GateDefinition customGate = GateDefinition.opaque(
            "missing_modified_base",
            List.of(),
            List.of("q")
        );

        circuit.gate(
            ModifiedGate.of(
                customGate,
                List.of(GateModifier.controlled(1))
            ),
            q.get(0),
            q.get(1)
        );

        final ValidationResult result = new QuantumProgramValidator().validate(program);

        assertFalse(result.isValid());
        assertContains(
            result,
            ValidationErrorCode.UNDECLARED_GATE_DEFINITION
        );
    }

    @Test
    void rejectsUnsupportedKnownConstantInGateOperationParameter() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("bad_constant");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );

        circuit.parameterizedGate(
            StandardGate.RZ,
            new ParameterExpression[] {ParameterExpression.knownConstant("tau")},
            q.get(0)
        );

        final ValidationResult result = new QuantumProgramValidator().validate(program);

        assertFalse(result.isValid());
        assertContains(
            result,
            ValidationErrorCode.INVALID_GATE_PARAMETER
        );
    }

    @Test
    void acceptsSymbolicAndPiExpressionInGateOperationParameter() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("symbolic_parameter");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );

        circuit.parameterizedGate(
            StandardGate.RZ,
            new ParameterExpression[] {
                ParameterExpression.divide(
                    ParameterExpression.pi(),
                    ParameterExpression.named("theta")
                )
            },
            q.get(0)
        );

        final ValidationResult result = new QuantumProgramValidator().validate(program);

        assertTrue(result.isValid());
    }

    private static void assertContains(
        final ValidationResult result,
        final ValidationErrorCode code
    ) {
        for (int i = 0; i < result.errorCount(); i++) {
            if (result.error(i).code() == code) {
                return;
            }
        }
        throw new AssertionError("Validation result does not contain code: " + code);
    }

    private static final class AlwaysInvalidGateValidationRule implements GateValidationRule {

        @Override
        public void validate(
            final GateOperation operation,
            final GateValidationRuleErrorCollector collector
        ) {
            collector.addError(
                ValidationErrorCode.OPERATION_NOT_SUPPORTED_BY_GATE_BASED_MODEL,
                "Custom gate rule rejected this operation."
            );
        }
    }
}
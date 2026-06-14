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

import ru.pathcreator.vadim.quantum.domain.gate.GateBodyOperation;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinition;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.gate.modifier.GateModifier;
import ru.pathcreator.vadim.quantum.domain.gate.modifier.ModifiedGate;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GateDefinitionValidationTest {

    @Test
    void acceptsCompositeGateUsingDeclaredSymbolsAndStandardGates() {
        final QuantumProgram program = QuantumProgram.gateBased();
        program.addGateDefinition(GateDefinition.composite(
            "phase_pair",
            List.of("theta"),
            List.of(
                "left",
                "right"
            ),
            List.of(
                GateBodyOperation.of(
                    StandardGate.RZ,
                    new ParameterExpression[] {ParameterExpression.named("theta")},
                    "left"
                ),
                GateBodyOperation.of(
                    StandardGate.CX,
                    new ParameterExpression[0],
                    "left",
                    "right"
                )
            )
        ));

        final ValidationResult result = new QuantumProgramValidator().validate(program);

        assertTrue(result.isValid());
    }

    @Test
    void rejectsUndeclaredBodyQubit() {
        final QuantumProgram program = QuantumProgram.gateBased();
        program.addGateDefinition(GateDefinition.composite(
            "bad_qubit",
            List.of(),
            List.of("q"),
            List.of(GateBodyOperation.of(
                StandardGate.H,
                new ParameterExpression[0],
                "missing"
            ))
        ));

        final ValidationResult result = new QuantumProgramValidator().validate(program);

        assertFalse(result.isValid());
        assertContains(
            result,
            ValidationErrorCode.INVALID_GATE_BODY_QUBIT
        );
    }

    @Test
    void rejectsUndeclaredBodyParameter() {
        final QuantumProgram program = QuantumProgram.gateBased();
        program.addGateDefinition(GateDefinition.composite(
            "bad_parameter",
            List.of("theta"),
            List.of("q"),
            List.of(GateBodyOperation.of(
                StandardGate.RZ,
                new ParameterExpression[] {ParameterExpression.named("phi")},
                "q"
            ))
        ));

        final ValidationResult result = new QuantumProgramValidator().validate(program);

        assertFalse(result.isValid());
        assertContains(
            result,
            ValidationErrorCode.INVALID_GATE_BODY_PARAMETER
        );
    }

    @Test
    void acceptsPiConstantInGateBodyParameterExpression() {
        final QuantumProgram program = QuantumProgram.gateBased();
        program.addGateDefinition(GateDefinition.composite(
            "uses_pi",
            List.of(),
            List.of("q"),
            List.of(GateBodyOperation.of(
                StandardGate.RZ,
                new ParameterExpression[] {
                    ParameterExpression.divide(
                        ParameterExpression.pi(),
                        ParameterExpression.of(2.0)
                    )
                },
                "q"
            ))
        ));

        final ValidationResult result = new QuantumProgramValidator().validate(program);

        assertTrue(result.isValid());
    }

    @Test
    void rejectsUnsupportedKnownConstantInGateBodyParameterExpression() {
        final QuantumProgram program = QuantumProgram.gateBased();
        program.addGateDefinition(GateDefinition.composite(
            "uses_tau",
            List.of(),
            List.of("q"),
            List.of(GateBodyOperation.of(
                StandardGate.RZ,
                new ParameterExpression[] {ParameterExpression.knownConstant("tau")},
                "q"
            ))
        ));

        final ValidationResult result = new QuantumProgramValidator().validate(program);

        assertFalse(result.isValid());
        assertContains(
            result,
            ValidationErrorCode.INVALID_GATE_BODY_PARAMETER
        );
    }

    @Test
    void rejectsCustomBodyGateNotDeclaredAtProgramLevel() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final GateDefinition external = GateDefinition.opaque(
            "external_gate",
            List.of(),
            List.of("q")
        );
        program.addGateDefinition(GateDefinition.composite(
            "uses_external",
            List.of(),
            List.of("q"),
            List.of(GateBodyOperation.of(
                external,
                new ParameterExpression[0],
                "q"
            ))
        ));

        final ValidationResult result = new QuantumProgramValidator().validate(program);

        assertFalse(result.isValid());
        assertContains(
            result,
            ValidationErrorCode.UNDECLARED_GATE_DEFINITION
        );
    }

    @Test
    void acceptsIntrinsicBodyGateNotDeclaredAtProgramLevel() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final GateDefinition intrinsic = GateDefinition.of(
            "body_intrinsic",
            1,
            0
        );
        program.addGateDefinition(GateDefinition.composite(
            "uses_intrinsic",
            List.of(),
            List.of("q"),
            List.of(GateBodyOperation.of(
                intrinsic,
                new ParameterExpression[0],
                "q"
            ))
        ));

        final ValidationResult result = new QuantumProgramValidator().validate(program);

        assertTrue(result.isValid());
    }

    @Test
    void acceptsDeclaredModifiedCustomGateInBody() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final GateDefinition base = GateDefinition.of(
            "body_base",
            1,
            0
        );
        program.addGateDefinition(base);
        program.addGateDefinition(GateDefinition.composite(
            "uses_modified_body_base",
            List.of(),
            List.of(
                "control",
                "target"
            ),
            List.of(GateBodyOperation.of(
                ModifiedGate.of(
                    base,
                    List.of(GateModifier.controlled(1))
                ),
                new ParameterExpression[0],
                "control",
                "target"
            ))
        ));

        final ValidationResult result = new QuantumProgramValidator().validate(program);

        assertTrue(result.isValid());
    }

    @Test
    void rejectsUndeclaredModifiedCustomGateInBody() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final GateDefinition base = GateDefinition.opaque(
            "missing_body_base",
            List.of(),
            List.of("q")
        );
        program.addGateDefinition(GateDefinition.composite(
            "uses_missing_modified_body_base",
            List.of(),
            List.of(
                "control",
                "target"
            ),
            List.of(GateBodyOperation.of(
                ModifiedGate.of(
                    base,
                    List.of(GateModifier.controlled(1))
                ),
                new ParameterExpression[0],
                "control",
                "target"
            ))
        ));

        final ValidationResult result = new QuantumProgramValidator().validate(program);

        assertFalse(result.isValid());
        assertContains(
            result,
            ValidationErrorCode.UNDECLARED_GATE_DEFINITION
        );
    }

    @Test
    void rejectsGateDefinitionCyclesByName() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final GateDefinition aReference = GateDefinition.of(
            "cycle_a",
            1,
            0
        );
        final GateDefinition bReference = GateDefinition.of(
            "cycle_b",
            1,
            0
        );
        program.addGateDefinition(GateDefinition.composite(
            "cycle_a",
            List.of(),
            List.of("q"),
            List.of(GateBodyOperation.of(
                bReference,
                new ParameterExpression[0],
                "q"
            ))
        ));
        program.addGateDefinition(GateDefinition.composite(
            "cycle_b",
            List.of(),
            List.of("q"),
            List.of(GateBodyOperation.of(
                aReference,
                new ParameterExpression[0],
                "q"
            ))
        ));

        final ValidationResult result = new QuantumProgramValidator().validate(program);

        assertFalse(result.isValid());
        assertContains(
            result,
            ValidationErrorCode.CYCLIC_GATE_DEFINITION
        );
    }

    @Test
    void rejectsGateDefinitionCyclesThroughModifiedGate() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final GateDefinition selfReference = GateDefinition.of(
            "modified_cycle",
            1,
            0
        );
        program.addGateDefinition(GateDefinition.composite(
            "modified_cycle",
            List.of(),
            List.of(
                "control",
                "target"
            ),
            List.of(GateBodyOperation.of(
                ModifiedGate.of(
                    selfReference,
                    List.of(GateModifier.controlled(1))
                ),
                new ParameterExpression[0],
                "control",
                "target"
            ))
        ));

        final ValidationResult result = new QuantumProgramValidator().validate(program);

        assertFalse(result.isValid());
        assertContains(
            result,
            ValidationErrorCode.CYCLIC_GATE_DEFINITION
        );
    }

    @Test
    void rejectsStandardGateNameConflicts() {
        final QuantumProgram standardConflictProgram = QuantumProgram.gateBased();
        standardConflictProgram.addGateDefinition(GateDefinition.opaque(
            "h",
            List.of(),
            List.of("q")
        ));

        final ValidationResult standardResult = new QuantumProgramValidator().validate(standardConflictProgram);

        assertEquals(
            ValidationErrorCode.GATE_DEFINITION_NAME_CONFLICT,
            standardResult.error(0).code()
        );
    }

    @Test
    void acceptsCustomNamesWhenTheyDoNotConflictWithIrStandardGates() {
        final QuantumProgram program = QuantumProgram.gateBased();
        program.addGateDefinition(GateDefinition.opaque(
            "custom_rotation",
            List.of(
                "theta",
                "phi",
                "lambda"
            ),
            List.of("q")
        ));

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
}
/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.callable;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.domain.classical.ClassicalType;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalTypeKind;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableGateOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableMeasureOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableOperationBlock;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CallableModelTest {

    @Test
    void createsCallableDefinitionWithArguments() {
        final CallableDefinition definition = new CallableDefinition(
            "prepare",
            CallableOperationBlock.of(),
            CallableArgument.qubit("q"),
            CallableArgument.classical(
                "theta",
                ClassicalType.sized(
                    ClassicalTypeKind.ANGLE,
                    64
                )
            )
        );

        assertEquals(
            "prepare",
            definition.name()
        );
        assertEquals(
            2,
            definition.argumentCount()
        );
        assertEquals(
            CallableArgumentKind.CLASSICAL,
            definition.argument(1).kind()
        );
    }

    @Test
    void createsCallableDefinitionWithArgumentBoundBody() {
        final CallableDefinition definition = new CallableDefinition(
            "prepare_and_measure",
            CallableOperationBlock.of(
                CallableGateOperation.of(
                    StandardGate.H,
                    "q"
                ),
                new CallableMeasureOperation(
                    "q",
                    "result"
                )
            ),
            CallableArgument.qubit("q"),
            CallableArgument.classical(
                "result",
                ClassicalType.of(ClassicalTypeKind.BOOLEAN)
            )
        );

        assertEquals(
            2,
            definition.body().operationCount()
        );
    }

    @Test
    void rejectsCallableBodyReferencesToUnknownArguments() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new CallableDefinition(
                "broken",
                CallableOperationBlock.of(CallableGateOperation.of(
                    StandardGate.X,
                    "missing"
                )),
                CallableArgument.qubit("q")
            )
        );
    }

    @Test
    void createsExternalCallableWithReturnType() {
        final ExternalCallableDeclaration declaration = new ExternalCallableDeclaration(
            "sample",
            ClassicalType.of(ClassicalTypeKind.BOOLEAN),
            CallableArgument.qubit("q")
        );

        assertTrue(declaration.hasReturnType());
        assertEquals(
            ClassicalTypeKind.BOOLEAN,
            declaration.returnType().kind()
        );
    }

    @Test
    void createsExternalCallableWithoutReturnType() {
        final ExternalCallableDeclaration declaration = new ExternalCallableDeclaration(
            "notify",
            null
        );

        assertFalse(declaration.hasReturnType());
        assertThrows(
            IllegalStateException.class,
            declaration::returnType
        );
    }
}
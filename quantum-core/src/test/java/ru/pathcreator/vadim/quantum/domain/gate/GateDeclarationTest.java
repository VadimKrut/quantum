/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.gate;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GateDeclarationTest {

    @Test
    void createsOpaqueGateDefinition() {
        final GateDefinition definition = GateDefinition.opaque(
            "blackbox",
            List.of("theta"),
            List.of("q")
        );

        assertEquals(
            GateDefinitionKind.OPAQUE,
            definition.kind()
        );
        assertEquals(
            1,
            definition.parameterCount()
        );
        assertEquals(
            1,
            definition.arity()
        );
        assertEquals(
            List.of("theta"),
            definition.parameterNames()
        );
    }

    @Test
    void createsCompositeGateDefinition() {
        final GateBodyOperation bodyOperation = GateBodyOperation.of(
            StandardGate.H,
            new ParameterExpression[0],
            "a"
        );

        final GateDefinition definition = GateDefinition.composite(
            "myh",
            List.of(),
            List.of("a"),
            List.of(bodyOperation)
        );

        assertEquals(
            GateDefinitionKind.COMPOSITE,
            definition.kind()
        );
        assertEquals(
            bodyOperation,
            definition.bodyOperations().get(0)
        );
    }

    @Test
    void createsIdentityCompositeGateDefinition() {
        final GateDefinition definition = GateDefinition.composite(
            "identity_gate",
            List.of(),
            List.of("q"),
            List.of()
        );

        assertEquals(
            GateDefinitionKind.COMPOSITE,
            definition.kind()
        );
        assertEquals(
            0,
            definition.bodyOperations().size()
        );
    }
}
/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.gate;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GateDefinitionTest {

    @Test
    void createsOpenGateDefinition() {
        final GateDefinition singleQubitGate = GateDefinition.of(
            "custom_single",
            1,
            0
        );
        final GateDefinition gate = GateDefinition.of(
            "custom_u",
            2,
            3
        );

        assertEquals(
            GateName.of("custom_u"),
            gate.name()
        );
        assertEquals(
            "custom_u",
            gate.gateName()
        );
        assertEquals(
            2,
            gate.arity()
        );
        assertEquals(
            3,
            gate.parameterCount()
        );
        assertEquals(
            1,
            gate.validationRules().size()
        );
        assertEquals(
            0,
            singleQubitGate.validationRules().size()
        );
    }

    @Test
    void createsGateDefinitionWithExplicitImmutableValidationRules() {
        final ArrayList<GateValidationRule> rules = new ArrayList<>();
        final GateValidationRule rule = DistinctQubitsGateValidationRule.INSTANCE;
        rules.add(rule);
        final GateDefinition gate = GateDefinition.of(
            "repeatable",
            2,
            0,
            rules
        );

        rules.clear();

        assertEquals(
            1,
            gate.validationRules().size()
        );
        assertEquals(
            rule,
            gate.validationRules().get(0)
        );
        assertThrows(
            UnsupportedOperationException.class,
            () -> gate.validationRules().add(rule)
        );
    }

    @Test
    void rejectsInvalidGateDefinition() {
        final ArrayList<GateValidationRule> rulesWithNull = new ArrayList<>();
        rulesWithNull.add(null);

        assertThrows(
            IllegalArgumentException.class,
            () -> GateDefinition.of(
                (GateName) null,
                1,
                0
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> GateDefinition.of(
                "custom",
                0,
                0
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> GateDefinition.of(
                "custom",
                1,
                -1
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> GateDefinition.of(
                "custom",
                1,
                0,
                null
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> GateDefinition.of(
                "custom",
                1,
                0,
                rulesWithNull
            )
        );
    }

    @Test
    void comparesByNameArityParameterCountAndValidationRules() {
        assertEquals(
            GateDefinition.of(
                "custom",
                2,
                1
            ),
            GateDefinition.of(
                "custom",
                2,
                1
            )
        );
        assertNotEquals(
            GateDefinition.of(
                "custom",
                2,
                1
            ),
            GateDefinition.of(
                "custom",
                1,
                1
            )
        );
        assertNotEquals(
            GateDefinition.of(
                "custom",
                2,
                1,
                List.of(DistinctQubitsGateValidationRule.INSTANCE)
            ),
            GateDefinition.of(
                "custom",
                2,
                1,
                List.of()
            )
        );
    }
}
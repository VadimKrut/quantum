/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.gate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StandardGateTest {

    @Test
    void exposesSingleQubitNonParameterizedGateMetadata() {
        assertEquals(
            "h",
            StandardGate.H.gateName()
        );
        assertEquals(
            1,
            StandardGate.H.arity()
        );
        assertEquals(
            0,
            StandardGate.H.parameterCount()
        );
    }

    @Test
    void exposesParameterizedGateMetadata() {
        assertEquals(
            "rz",
            StandardGate.RZ.gateName()
        );
        assertEquals(
            1,
            StandardGate.RZ.arity()
        );
        assertEquals(
            1,
            StandardGate.RZ.parameterCount()
        );
    }

    @Test
    void exposesMultiQubitGateMetadata() {
        assertEquals(
            "ccx",
            StandardGate.CCX.gateName()
        );
        assertEquals(
            3,
            StandardGate.CCX.arity()
        );
        assertEquals(
            0,
            StandardGate.CCX.parameterCount()
        );
        assertEquals(
            1,
            StandardGate.CCX.validationRules().size()
        );
    }

    @Test
    void allStandardGatesExposeStableValidMetadata() {
        final StandardGate[] gates = StandardGate.values();

        for (int i = 0; i < gates.length; i++) {
            final StandardGate gate = gates[i];
            assertEquals(
                gate.gateName(),
                gate.gateName().toLowerCase()
            );
            assertNotEquals(
                "",
                gate.gateName()
            );
            assertNotEquals(
                0,
                gate.arity()
            );
            for (int j = i + 1; j < gates.length; j++) {
                assertNotEquals(
                    gate.gateName(),
                    gates[j].gateName()
                );
            }
            if (gate.arity() == 1) {
                assertEquals(
                    0,
                    gate.validationRules().size()
                );
            } else {
                assertEquals(
                    1,
                    gate.validationRules().size()
                );
            }
        }
    }

    @Test
    void standardGateValidationRulesAreImmutable() {
        assertThrows(
            UnsupportedOperationException.class,
            () -> StandardGate.CX.validationRules().clear()
        );
    }
}
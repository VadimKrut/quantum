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

class GateNameTest {

    @Test
    void createsGateNameFromValidIdentifier() {
        final GateName name = GateName.of("custom_gate_1");

        assertEquals(
            "custom_gate_1",
            name.value()
        );
        assertEquals(
            "custom_gate_1",
            name.toString()
        );
    }

    @Test
    void rejectsInvalidGateNames() {
        assertThrows(
            IllegalArgumentException.class,
            () -> GateName.of(null)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> GateName.of("")
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> GateName.of("1gate")
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> GateName.of("custom-gate")
        );
    }

    @Test
    void comparesByValue() {
        assertEquals(
            GateName.of("u"),
            GateName.of("u")
        );
        assertNotEquals(
            GateName.of("u"),
            GateName.of("v")
        );
    }
}
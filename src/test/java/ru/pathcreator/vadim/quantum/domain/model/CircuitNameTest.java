/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CircuitNameTest {
    @Test
    void createsCircuitNameFromValidIdentifier() {
        final CircuitName name = CircuitName.of("bell_circuit_1");

        assertEquals(
            "bell_circuit_1",
            name.value()
        );
        assertEquals(
            "bell_circuit_1",
            name.toString()
        );
    }

    @Test
    void rejectsInvalidCircuitNames() {
        assertThrows(
            IllegalArgumentException.class,
            () -> CircuitName.of(null)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> CircuitName.of("")
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> CircuitName.of("1bell")
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> CircuitName.of("bell-circuit")
        );
    }

    @Test
    void comparesByValue() {
        assertEquals(
            CircuitName.of("bell"),
            CircuitName.of("bell")
        );
        assertNotEquals(
            CircuitName.of("bell"),
            CircuitName.of("ghz")
        );
    }
}
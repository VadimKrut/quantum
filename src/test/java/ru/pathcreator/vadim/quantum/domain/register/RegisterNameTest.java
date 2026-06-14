/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.register;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegisterNameTest {
    @Test
    void createsRegisterNameFromValidIdentifier() {
        final RegisterName name = RegisterName.of("q_1");

        assertEquals(
            "q_1",
            name.value()
        );
        assertEquals(
            "q_1",
            name.toString()
        );
    }

    @Test
    void rejectsInvalidRegisterNames() {
        assertThrows(
            IllegalArgumentException.class,
            () -> RegisterName.of(null)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> RegisterName.of(" ")
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> RegisterName.of("2q")
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> RegisterName.of("quantum register")
        );
    }

    @Test
    void comparesByValue() {
        assertEquals(
            RegisterName.of("q"),
            RegisterName.of("q")
        );
        assertNotEquals(
            RegisterName.of("q"),
            RegisterName.of("c")
        );
    }
}
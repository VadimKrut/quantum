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

import ru.pathcreator.vadim.quantum.domain.bit.Qubit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuantumRegisterTest {

    @Test
    void createsQuantumRegisterWithStableQubits() {
        final QuantumRegister register = QuantumRegister.create(
            "q",
            2
        );

        final Qubit first = register.get(0);
        final Qubit second = register.get(1);

        assertEquals(
            RegisterName.of("q"),
            register.name()
        );
        assertEquals(
            2,
            register.size()
        );
        assertSame(
            register,
            first.register()
        );
        assertEquals(
            0,
            first.index()
        );
        assertEquals(
            1,
            second.index()
        );
        assertSame(
            first,
            register.get(0)
        );
        assertEquals(
            "q[2]",
            register.toString()
        );
        assertEquals(
            "q[0]",
            first.toString()
        );
    }

    @Test
    void rejectsInvalidQuantumRegisterSize() {
        assertThrows(
            IllegalArgumentException.class,
            () -> QuantumRegister.create(
                (RegisterName) null,
                1
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> QuantumRegister.create(
                "q",
                0
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> QuantumRegister.create(
                "q",
                -1
            )
        );
    }

    @Test
    void rejectsInvalidQubitIndex() {
        final QuantumRegister register = QuantumRegister.create(
            "q",
            2
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> register.get(-1)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> register.get(2)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> Qubit.of(
                null,
                0
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> Qubit.of(
                register,
                -1
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> Qubit.of(
                register,
                2
            )
        );
    }

    @Test
    void qubitIdentityIncludesRegisterInstanceAndIndex() {
        final QuantumRegister leftRegister = QuantumRegister.create(
            "q",
            2
        );
        final QuantumRegister rightRegister = QuantumRegister.create(
            "q",
            2
        );

        assertEquals(
            leftRegister.get(0),
            leftRegister.get(0)
        );
        assertEquals(
            leftRegister.get(0).hashCode(),
            leftRegister.get(0).hashCode()
        );
        assertNotEquals(
            leftRegister.get(0),
            leftRegister.get(1)
        );
        assertNotEquals(
            leftRegister.get(0),
            rightRegister.get(0)
        );
        assertNotEquals(
            leftRegister.get(0),
            "q[0]"
        );
    }
}
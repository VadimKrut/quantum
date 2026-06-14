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

import ru.pathcreator.vadim.quantum.domain.bit.ClassicalBit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClassicalRegisterTest {

    @Test
    void createsClassicalRegisterWithStableBits() {
        final ClassicalRegister register = ClassicalRegister.create(
            "c",
            2
        );

        final ClassicalBit first = register.get(0);
        final ClassicalBit second = register.get(1);

        assertEquals(
            RegisterName.of("c"),
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
            "c[2]",
            register.toString()
        );
        assertEquals(
            "c[0]",
            first.toString()
        );
    }

    @Test
    void rejectsInvalidClassicalRegisterSize() {
        assertThrows(
            IllegalArgumentException.class,
            () -> ClassicalRegister.create(
                (RegisterName) null,
                1
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> ClassicalRegister.create(
                "c",
                0
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> ClassicalRegister.create(
                "c",
                -1
            )
        );
    }

    @Test
    void rejectsInvalidClassicalBitIndex() {
        final ClassicalRegister register = ClassicalRegister.create(
            "c",
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
            () -> ClassicalBit.of(
                null,
                0
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> ClassicalBit.of(
                register,
                -1
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> ClassicalBit.of(
                register,
                2
            )
        );
    }

    @Test
    void classicalBitIdentityIncludesRegisterInstanceAndIndex() {
        final ClassicalRegister leftRegister = ClassicalRegister.create(
            "c",
            2
        );
        final ClassicalRegister rightRegister = ClassicalRegister.create(
            "c",
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
            "c[0]"
        );
    }
}
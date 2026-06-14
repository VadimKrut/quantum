/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.classical;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassicalDeclarationTest {

    @Test
    void createsSizedClassicalType() {
        final ClassicalType type = ClassicalType.sized(
            ClassicalTypeKind.UNSIGNED_INTEGER,
            32
        );

        assertEquals(
            ClassicalTypeKind.UNSIGNED_INTEGER,
            type.kind()
        );
        assertTrue(type.hasBitWidth());
        assertEquals(
            32,
            type.bitWidth()
        );
    }

    @Test
    void rejectsMissingWidthForWidthRequiredTypes() {
        assertThrows(
            IllegalArgumentException.class,
            () -> ClassicalType.of(ClassicalTypeKind.FLOAT)
        );
    }

    @Test
    void createsClassicalDeclaration() {
        final ClassicalDeclaration declaration = new ClassicalDeclaration(
            "counter",
            ClassicalType.sized(
                ClassicalTypeKind.SIGNED_INTEGER,
                64
            )
        );

        assertEquals(
            "counter",
            declaration.name()
        );
        assertFalse(ClassicalType.of(ClassicalTypeKind.BOOLEAN).hasBitWidth());
    }
}
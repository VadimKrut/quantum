/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.naming;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdentifierNameTest {
    @Test
    void createsIdentifierNameFromValidValue() {
        final IdentifierName name = IdentifierName.of(
            "valid_name_1",
            "Test name"
        );

        assertEquals(
            "valid_name_1",
            name.value()
        );
        assertEquals(
            "valid_name_1",
            name.toString()
        );
    }

    @Test
    void rejectsInvalidSubjectName() {
        assertThrows(
            IllegalArgumentException.class,
            () -> IdentifierName.of(
                "valid",
                null
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> IdentifierName.of(
                "valid",
                ""
            )
        );
    }

    @Test
    void rejectsInvalidIdentifierValue() {
        assertThrows(
            IllegalArgumentException.class,
            () -> IdentifierName.of(
                null,
                "Test name"
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> IdentifierName.of(
                "",
                "Test name"
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> IdentifierName.of(
                "1invalid",
                "Test name"
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> IdentifierName.of(
                "invalid-name",
                "Test name"
            )
        );
    }

    @Test
    void comparesByValue() {
        assertEquals(
            IdentifierName.of(
                "same",
                "Test name"
            ),
            IdentifierName.of(
                "same",
                "Test name"
            )
        );
        assertNotEquals(
            IdentifierName.of(
                "left",
                "Test name"
            ),
            IdentifierName.of(
                "right",
                "Test name"
            )
        );
    }
}
/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidationErrorTest {

    @Test
    void createsValidationError() {
        final ValidationError error = new ValidationError(
            ValidationErrorCode.NULL_PROGRAM,
            "Program is missing.",
            ValidationError.NO_INDEX,
            ValidationError.NO_INDEX
        );

        assertEquals(
            ValidationErrorCode.NULL_PROGRAM,
            error.code()
        );
        assertEquals(
            "Program is missing.",
            error.message()
        );
        assertEquals(
            ValidationError.NO_INDEX,
            error.circuitIndex()
        );
        assertEquals(
            ValidationError.NO_INDEX,
            error.operationIndex()
        );
    }

    @Test
    void rejectsInvalidValidationErrorArguments() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new ValidationError(
                null,
                "Program is missing.",
                ValidationError.NO_INDEX,
                ValidationError.NO_INDEX
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new ValidationError(
                ValidationErrorCode.NULL_PROGRAM,
                null,
                ValidationError.NO_INDEX,
                ValidationError.NO_INDEX
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new ValidationError(
                ValidationErrorCode.NULL_PROGRAM,
                "",
                ValidationError.NO_INDEX,
                ValidationError.NO_INDEX
            )
        );
    }
}
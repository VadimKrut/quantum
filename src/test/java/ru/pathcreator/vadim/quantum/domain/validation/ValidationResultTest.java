/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.validation;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationResultTest {

    @Test
    void createsValidResult() {
        final ValidationResult result = ValidationResult.valid();

        assertTrue(result.isValid());
        assertEquals(
            0,
            result.errorCount()
        );
    }

    @Test
    void protectsErrorsFromExternalMutation() {
        final ArrayList<ValidationError> errors = new ArrayList<>();
        errors.add(new ValidationError(
            ValidationErrorCode.NULL_PROGRAM,
            "Program is missing.",
            ValidationError.NO_INDEX,
            ValidationError.NO_INDEX
        ));

        final ValidationResult result = new ValidationResult(errors);
        errors.clear();

        assertEquals(
            1,
            result.errorCount()
        );
        assertSame(
            result.error(0),
            result.errors().get(0)
        );
        assertThrows(
            UnsupportedOperationException.class,
            () -> result.errors().add(new ValidationError(
                ValidationErrorCode.UNSUPPORTED_COMPUTATION_MODEL,
                "Unsupported model.",
                ValidationError.NO_INDEX,
                ValidationError.NO_INDEX
            ))
        );
    }

    @Test
    void rejectsInvalidErrorAccessAndInput() {
        final ArrayList<ValidationError> errorsWithNull = new ArrayList<>();
        errorsWithNull.add(null);

        assertThrows(
            IllegalArgumentException.class,
            () -> new ValidationResult(null)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new ValidationResult(errorsWithNull)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> ValidationResult.valid().error(0)
        );
    }
}
/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.integration.diagnostic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntegrationDiagnosticTest {

    @Test
    void createsErrorDiagnosticWithoutLocation() {
        final IntegrationDiagnostic diagnostic = IntegrationDiagnostic.error(
            IntegrationDiagnosticCode.PARSE_ERROR,
            "Unexpected token."
        );

        assertEquals(
            IntegrationDiagnosticSeverity.ERROR,
            diagnostic.severity()
        );
        assertEquals(
            IntegrationDiagnosticCode.PARSE_ERROR,
            diagnostic.code()
        );
        assertEquals(
            "Unexpected token.",
            diagnostic.message()
        );
        assertEquals(
            IntegrationDiagnostic.NO_LOCATION,
            diagnostic.line()
        );
        assertEquals(
            IntegrationDiagnostic.NO_LOCATION,
            diagnostic.column()
        );
        assertTrue(diagnostic.isError());
        assertFalse(diagnostic.isWarning());
    }

    @Test
    void createsWarningDiagnosticWithLocation() {
        final IntegrationDiagnostic diagnostic = IntegrationDiagnostic.warning(
            IntegrationDiagnosticCode.UNSUPPORTED_INPUT_FEATURE,
            "Ignored include directive.",
            3,
            5
        );

        assertEquals(
            IntegrationDiagnosticSeverity.WARNING,
            diagnostic.severity()
        );
        assertEquals(
            3,
            diagnostic.line()
        );
        assertEquals(
            5,
            diagnostic.column()
        );
        assertTrue(diagnostic.isWarning());
        assertFalse(diagnostic.isError());
    }

    @Test
    void rejectsInvalidDiagnosticValues() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new IntegrationDiagnostic(
                null,
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Bad input.",
                IntegrationDiagnostic.NO_LOCATION,
                IntegrationDiagnostic.NO_LOCATION
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> IntegrationDiagnostic.error(
                null,
                "Bad input."
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.PARSE_ERROR,
                ""
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Bad input.",
                0,
                IntegrationDiagnostic.NO_LOCATION
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Bad input.",
                IntegrationDiagnostic.NO_LOCATION,
                1
            )
        );
    }

    @Test
    void comparesByAllFields() {
        assertEquals(
            IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Bad input.",
                2,
                1
            ),
            IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Bad input.",
                2,
                1
            )
        );
        assertEquals(
            IntegrationDiagnostic.warning(
                IntegrationDiagnosticCode.UNSUPPORTED_INPUT_FEATURE,
                "Warn."
            ).hashCode(),
            IntegrationDiagnostic.warning(
                IntegrationDiagnosticCode.UNSUPPORTED_INPUT_FEATURE,
                "Warn."
            ).hashCode()
        );
        assertNotEquals(
            IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Bad input."
            ),
            IntegrationDiagnostic.warning(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Bad input."
            )
        );
        assertNotEquals(
            IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Bad input."
            ),
            "Bad input."
        );
    }
}
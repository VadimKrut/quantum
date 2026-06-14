/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.calibration;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalibrationDefinitionTest {

    @Test
    void createsCalibrationDefinition() {
        final CalibrationDefinition definition = new CalibrationDefinition(
            "x",
            List.of("theta"),
            List.of("q"),
            "pulse",
            "body"
        );

        assertEquals(
            "x",
            definition.targetName()
        );
        assertEquals(
            "pulse",
            definition.bodyLanguage()
        );
        assertEquals(
            List.of("theta"),
            definition.parameterNames()
        );
    }

    @Test
    void rejectsInvalidNames() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new CalibrationDefinition(
                "bad-name",
                List.of(),
                List.of(),
                "pulse",
                "body"
            )
        );
    }
}
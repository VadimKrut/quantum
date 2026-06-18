/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.input;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DesktopInputParsersTest {

    @Test
    void parsesPositiveIntegersWithSafeFallback() {
        assertEquals(
            16,
            DesktopInputParsers.positiveIntegerOrOne("16")
        );
        assertEquals(
            1,
            DesktopInputParsers.positiveIntegerOrOne("0")
        );
        assertEquals(
            1,
            DesktopInputParsers.positiveIntegerOrOne("not-a-number")
        );
    }

    @Test
    void parsesLongsWithExplicitDefault() {
        assertEquals(
            42L,
            DesktopInputParsers.positiveLongOrDefault(
                "42",
                7L
            )
        );
        assertEquals(
            7L,
            DesktopInputParsers.positiveLongOrDefault(
                "bad",
                7L
            )
        );
    }

    @Test
    void parsesDoublesWithExplicitDefault() {
        assertEquals(
            0.25,
            DesktopInputParsers.doubleOrDefault(
                "0.25",
                1.0
            )
        );
        assertEquals(
            1.0,
            DesktopInputParsers.doubleOrDefault(
                "bad",
                1.0
            )
        );
    }

    @Test
    void rendersExceptionMessageFallback() {
        assertEquals(
            "IllegalStateException",
            DesktopInputParsers.exceptionMessage(new IllegalStateException())
        );
        assertEquals(
            "broken",
            DesktopInputParsers.exceptionMessage(new IllegalStateException("broken"))
        );
    }
}
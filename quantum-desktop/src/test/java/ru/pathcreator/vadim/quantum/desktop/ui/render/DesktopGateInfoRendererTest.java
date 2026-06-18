/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.desktop.ui.layout.DesktopGateCatalogView;

class DesktopGateInfoRendererTest {

    private final DesktopGateCatalogView catalog = new DesktopGateCatalogView();
    private final DesktopGateInfoRenderer renderer = new DesktopGateInfoRenderer();

    @Test
    void describesEveryGateCatalogEntryInBothLanguages() {
        for (final String gate : catalog.gates()) {
            final String english = renderer.render(
                gate,
                false
            );
            final String russian = renderer.render(
                gate,
                true
            );

            assertTrue(english.contains("Gate: " + gate));
            assertTrue(russian.contains("Gate: " + gate));
            assertFalse(
                english.toLowerCase().contains("unknown"),
                gate
            );
            assertFalse(
                russian.toLowerCase().contains("неизвест"),
                gate
            );
        }
    }

    @Test
    void whileDocumentationMatchesEditablePredicateFields() {
        final String english = renderer.render(
            "WHILE",
            false
        );
        final String russian = renderer.render(
            "WHILE",
            true
        );

        assertTrue(english.contains("Classical"));
        assertTrue(english.contains("Angle"));
        assertFalse(english.toLowerCase().contains("false predicate"));
        assertTrue(russian.contains("Classical"));
        assertTrue(russian.contains("Angle"));
        assertFalse(russian.toLowerCase().contains("false predicate"));
    }
}
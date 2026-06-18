/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.ir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.domain.operation.OperationKind;

class DesktopIrOperationSurfaceCatalogTest {

    private final DesktopIrOperationSurfaceCatalog catalog = new DesktopIrOperationSurfaceCatalog();

    @Test
    void describesEveryCurrentOperationKind() {
        assertEquals(
            OperationKind.values().length,
            catalog.descriptions().size()
        );
        assertEquals(
            OperationKind.values().length,
            catalog.descriptions(false).size()
        );
        for (final OperationKind kind : OperationKind.values()) {
            assertFalse(catalog.description(kind).isBlank());
            assertFalse(catalog.description(
                kind,
                false
            ).isBlank());
        }
    }

    @Test
    void rendersReadableFullSurfaceWithoutMojibakeMarkers() {
        final String text = catalog.render();
        final String english = catalog.render(false);

        assertTrue(text.contains("Quantum IR"));
        assertTrue(english.contains("Full Quantum IR Surface"));
        assertTrue(text.contains(OperationKind.CALLABLE_INVOCATION.name()));
        assertTrue(english.contains(OperationKind.CALLABLE_INVOCATION.name()));
        assertFalse(english.contains("Полная"));
        assertFalse(text.contains(mojibakeMarker(0x0420, 0x045f)));
        assertFalse(text.contains(mojibakeMarker(0x0421, 0x0453)));
        assertFalse(english.contains(mojibakeMarker(0x0420, 0x045f)));
        assertFalse(english.contains(mojibakeMarker(0x0421, 0x0453)));
    }

    private static String mojibakeMarker(
        final int first,
        final int second
    ) {
        return new String(new char[] {
            (char) first,
            (char) second
        });
    }
}
/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

class DesktopScreenshotRequestTest {

    @Test
    void usesStableDefaults() {
        final DesktopScreenshotRequest request = DesktopScreenshotRequest.from(Map.of());

        assertEquals(
            "blank",
            request.fixtureName()
        );
        assertEquals(
            "Q-Sphere",
            request.tabName()
        );
        assertEquals(
            "target/desktop-q-sphere-screenshot.png",
            request.outputPath()
        );
        assertNull(request.inspectStep());
        assertNull(request.operationIndex());
    }

    @Test
    void parsesExplicitScreenshotParameters() {
        final DesktopScreenshotRequest request = DesktopScreenshotRequest.from(Map.of(
            "template",
            "qft16",
            "screenshot-tab",
            "Visual Circuit",
            "screenshot-path",
            "target/out.png",
            "inspect-step",
            "218",
            "screenshot-operation-index",
            "218"
        ));

        assertEquals(
            "qft16",
            request.fixtureName()
        );
        assertEquals(
            "Visual Circuit",
            request.tabName()
        );
        assertEquals(
            "target/out.png",
            request.outputPath()
        );
        assertEquals(
            Integer.valueOf(218),
            request.inspectStep()
        );
        assertEquals(
            Integer.valueOf(218),
            request.operationIndex()
        );
    }

    @Test
    void acceptsHumanFriendlyScreenshotAliases() {
        final DesktopScreenshotRequest request = DesktopScreenshotRequest.from(Map.of(
            "template",
            "grover16",
            "tab",
            "Compatibility",
            "screenshot-output",
            "target/alias.png",
            "inspect-step",
            "112",
            "operation-index",
            "112"
        ));

        assertEquals(
            "grover16",
            request.fixtureName()
        );
        assertEquals(
            "Compatibility",
            request.tabName()
        );
        assertEquals(
            "target/alias.png",
            request.outputPath()
        );
        assertEquals(
            Integer.valueOf(112),
            request.operationIndex()
        );
    }
    @Test
    void rejectsInvalidIntegerParameters() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DesktopScreenshotRequest.from(Map.of(
                "inspect-step",
                "bad"
            ))
        );
    }
}
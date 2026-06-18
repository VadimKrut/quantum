/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.desktop.workspace.DesktopIrOperationSpec;

class DesktopOperationLabelRendererTest {

    private final DesktopOperationLabelRenderer renderer = new DesktopOperationLabelRenderer();

    @Test
    void rendersThreeQubitGateSummary() {
        assertEquals(
            "CCX q[0], q[1], q[2]",
            renderer.renderSummary(new DesktopIrOperationSpec(
                "CCX",
                "q[0]",
                "q[1]",
                "q[2]",
                "c[0]",
                0.5
            ))
        );
    }

    @Test
    void rendersMeasurementClassicalTarget() {
        assertEquals(
            "MEASURE q[3] -> c[2]",
            renderer.renderSummary(new DesktopIrOperationSpec(
                "MEASURE",
                "q[3]",
                "q[0]",
                "q[0]",
                "c[2]",
                0.5
            ))
        );
    }
}
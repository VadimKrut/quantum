/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.workspace.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.desktop.workspace.DesktopIrOperationSpec;

final class DesktopCustomOperationRegistryTest {

    @Test
    void expandsCustomOperationReferenceToStoredBody() {
        final DesktopCustomOperationRegistry registry = new DesktopCustomOperationRegistry();
        final DesktopIrOperationSpec h = operation("H");
        final DesktopIrOperationSpec x = operation("X");
        final String name = registry.define(
            "Pair",
            List.of(
                h,
                x
            )
        );

        assertEquals(
            List.of(
                operation("Z"),
                h,
                x
            ),
            registry.expand(List.of(
                operation("Z"),
                registry.reference(name)
            ))
        );
    }

    @Test
    void duplicateNamesReceiveStableSuffix() {
        final DesktopCustomOperationRegistry registry = new DesktopCustomOperationRegistry();

        assertEquals(
            "Pair",
            registry.define(
                "Pair",
                List.of(operation("H"))
            )
        );
        assertEquals(
            "Pair_2",
            registry.define(
                "Pair",
                List.of(operation("X"))
            )
        );
    }

    @Test
    void referenceKeepsFirstBodyOperationPlacementForVisualGrouping() {
        final DesktopCustomOperationRegistry registry = new DesktopCustomOperationRegistry();
        final DesktopIrOperationSpec first = new DesktopIrOperationSpec(
            "RY",
            "q[3]",
            "q[1]",
            "q[0]",
            "c[2]",
            0.25
        );
        final String name = registry.define(
            "WideBlock",
            List.of(
                first,
                operation("CX")
            )
        );

        assertEquals(
            new DesktopIrOperationSpec(
                "CUSTOM:WideBlock",
                "q[3]",
                "q[1]",
                "q[0]",
                "c[2]",
                0.25
            ),
            registry.reference(name)
        );
    }

    private static DesktopIrOperationSpec operation(final String gate) {
        return new DesktopIrOperationSpec(
            gate,
            "q[0]",
            "q[1]",
            "q[2]",
            "c[0]",
            Math.PI / 2.0
        );
    }
}
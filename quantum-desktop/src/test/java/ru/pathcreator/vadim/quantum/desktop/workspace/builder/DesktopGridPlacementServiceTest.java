/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.workspace.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

final class DesktopGridPlacementServiceTest {

    private final DesktopGridPlacementService service = new DesktopGridPlacementService();

    @Test
    void singleClickGateCreatesOperationImmediately() {
        final DesktopGridPlacementResult result = service.place(
            "H",
            "q[0]",
            "q[1]",
            "q[2]",
            "c[0]",
            Math.PI / 2.0,
            List.of()
        );

        assertTrue(result.hasOperation());
        assertEquals(
            "H",
            result.operation().gate()
        );
        assertEquals(
            "q[0]",
            result.operation().primaryQubit()
        );
        assertTrue(result.pendingQubits().isEmpty());
    }

    @Test
    void controlledGateWaitsForSecondQubit() {
        final DesktopGridPlacementResult first = service.place(
            "CX",
            "q[0]",
            "q[1]",
            "q[2]",
            "c[0]",
            Math.PI / 2.0,
            List.of()
        );

        assertFalse(first.hasOperation());
        assertEquals(
            List.of("q[0]"),
            first.pendingQubits()
        );

        final DesktopGridPlacementResult second = service.place(
            "CX",
            "q[1]",
            "q[1]",
            "q[2]",
            "c[0]",
            Math.PI / 2.0,
            first.pendingQubits()
        );

        assertTrue(second.hasOperation());
        assertEquals(
            "q[0]",
            second.operation().primaryQubit()
        );
        assertEquals(
            "q[1]",
            second.operation().secondaryQubit()
        );
        assertTrue(second.pendingQubits().isEmpty());
    }

    @Test
    void toffoliGateWaitsForThreeQubits() {
        final DesktopGridPlacementResult first = service.place(
            "CCX",
            "q[0]",
            "q[1]",
            "q[2]",
            "c[0]",
            Math.PI / 2.0,
            List.of()
        );
        final DesktopGridPlacementResult second = service.place(
            "CCX",
            "q[1]",
            "q[1]",
            "q[2]",
            "c[0]",
            Math.PI / 2.0,
            first.pendingQubits()
        );
        final DesktopGridPlacementResult third = service.place(
            "CCX",
            "q[2]",
            "q[1]",
            "q[4]",
            "c[0]",
            Math.PI / 2.0,
            second.pendingQubits()
        );

        assertFalse(first.hasOperation());
        assertFalse(second.hasOperation());
        assertTrue(third.hasOperation());
        assertEquals(
            "q[2]",
            third.operation().tertiaryQubit()
        );
    }
}
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

final class DesktopOperationReorderServiceTest {

    private final DesktopOperationReorderService service = new DesktopOperationReorderService();

    @Test
    void movesContiguousSelectionAfterTargetAdjustment() {
        assertEquals(
            List.of(
                "a",
                "d",
                "b",
                "c",
                "e"
            ),
            service.moveSelection(
                List.of(
                    "a",
                    "b",
                    "c",
                    "d",
                    "e"
                ),
                List.of(
                    1,
                    2
                ),
                4
            )
        );
    }

    @Test
    void movesNonContiguousSelectionPreservingSelectionOrder() {
        assertEquals(
            List.of(
                "b",
                "d",
                "a",
                "c",
                "e"
            ),
            service.moveSelection(
                List.of(
                    "a",
                    "b",
                    "c",
                    "d",
                    "e"
                ),
                List.of(
                    0,
                    2
                ),
                4
            )
        );
    }

    @Test
    void returnsMovedSelectionIndicesAfterTargetAdjustment() {
        assertEquals(
            List.of(
                2,
                3
            ),
            service.movedSelectionIndices(
                List.of(
                    1,
                    2
                ),
                4,
                5
            )
        );
    }

    @Test
    void ignoresInvalidAndDuplicateSelectionIndices() {
        assertEquals(
            List.of(
                "b",
                "c",
                "d",
                "a"
            ),
            service.moveSelection(
                List.of(
                    "a",
                    "b",
                    "c",
                    "d"
                ),
                List.of(
                    -1,
                    0,
                    0,
                    99
                ),
                4
            )
        );
    }

    @Test
    void emptySelectionKeepsOperationsImmutableCopy() {
        assertEquals(
            List.of(
                "a",
                "b"
            ),
            service.moveSelection(
                List.of(
                    "a",
                    "b"
                ),
                List.of(),
                1
            )
        );
    }

    @Test
    void createsBoundedContiguousRange() {
        assertEquals(
            List.of(
                1,
                2,
                3
            ),
            service.contiguousRange(
                3,
                1,
                5
            )
        );
    }

    @Test
    void emptyOperationRangeProducesNoIndices() {
        assertEquals(
            List.of(),
            service.contiguousRange(
                0,
                3,
                0
            )
        );
    }
}
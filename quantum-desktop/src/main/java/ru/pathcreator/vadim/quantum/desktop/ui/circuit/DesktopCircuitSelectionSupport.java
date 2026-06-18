/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.circuit;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;

/**
 * Находит операции визуальной схемы, попавшие в прямоугольник выделения.
 */
public final class DesktopCircuitSelectionSupport {

    private DesktopCircuitSelectionSupport() {
    }

    public static List<Integer> intersectingOperationIndices(
        final Node root,
        final Bounds selectionBounds
    ) {
        final ArrayList<Integer> result = new ArrayList<>();
        collectIntersectingOperationCells(
            root,
            selectionBounds,
            result
        );
        result.sort(Integer::compareTo);
        return result;
    }

    private static void collectIntersectingOperationCells(
        final Node node,
        final Bounds selectionBounds,
        final ArrayList<Integer> result
    ) {
        final Object operationIndex = node.getProperties().get("operationIndex");
        if (
            operationIndex instanceof Integer index
            && node.localToScene(node.getBoundsInLocal()).intersects(selectionBounds)
            && !result.contains(index)
        ) {
            result.add(index);
        }
        if (node instanceof Parent parent) {
            for (int i = 0; i < parent.getChildrenUnmodifiable().size(); i++) {
                collectIntersectingOperationCells(
                    parent.getChildrenUnmodifiable().get(i),
                    selectionBounds,
                    result
                );
            }
        }
    }
}
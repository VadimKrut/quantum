/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.workspace.operation;

import java.util.ArrayList;
import java.util.List;

/**
 * Переставляет операции desktop-потока без потери порядка выделенных элементов.
 */
public final class DesktopOperationReorderService {

    public <T> List<T> moveSelection(
        final List<T> operations,
        final List<Integer> selectedIndices,
        final int requestedInsertIndex
    ) {
        if (operations == null) {
            throw new IllegalArgumentException("Desktop operations must not be null.");
        }
        final ArrayList<Integer> selection = normalizedSelection(
            selectedIndices,
            operations.size()
        );
        if (selection.isEmpty()) {
            return List.copyOf(operations);
        }
        final ArrayList<T> selected = new ArrayList<>(selection.size());
        final ArrayList<T> remaining = new ArrayList<>(operations.size() - selection.size());
        for (int i = 0; i < operations.size(); i++) {
            if (selection.contains(i)) {
                selected.add(operations.get(i));
            } else {
                remaining.add(operations.get(i));
            }
        }
        int insertIndex = Math.max(
            0,
            Math.min(
                requestedInsertIndex,
                operations.size()
            )
        );
        for (int i = 0; i < selection.size(); i++) {
            if (selection.get(i) < requestedInsertIndex) {
                insertIndex--;
            }
        }
        insertIndex = Math.max(
            0,
            Math.min(
                insertIndex,
                remaining.size()
            )
        );
        remaining.addAll(
            insertIndex,
            selected
        );
        return List.copyOf(remaining);
    }

    public List<Integer> contiguousRange(
        final int start,
        final int end,
        final int operationCount
    ) {
        final int from = Math.max(
            0,
            Math.min(
                start,
                end
            )
        );
        final int to = Math.min(
            operationCount - 1,
            Math.max(
                start,
                end
            )
        );
        final ArrayList<Integer> indices = new ArrayList<>();
        for (int i = from; i <= to; i++) {
            indices.add(i);
        }
        return List.copyOf(indices);
    }

    public List<Integer> movedSelectionIndices(
        final List<Integer> selectedIndices,
        final int requestedInsertIndex,
        final int operationCount
    ) {
        final ArrayList<Integer> selection = normalizedSelection(
            selectedIndices,
            operationCount
        );
        if (selection.isEmpty()) {
            return List.of();
        }
        int insertIndex = Math.max(
            0,
            Math.min(
                requestedInsertIndex,
                operationCount
            )
        );
        for (int i = 0; i < selection.size(); i++) {
            if (selection.get(i) < requestedInsertIndex) {
                insertIndex--;
            }
        }
        insertIndex = Math.max(
            0,
            Math.min(
                insertIndex,
                operationCount - selection.size()
            )
        );
        final ArrayList<Integer> movedIndices = new ArrayList<>(selection.size());
        for (int i = 0; i < selection.size(); i++) {
            movedIndices.add(insertIndex + i);
        }
        return List.copyOf(movedIndices);
    }

    private static ArrayList<Integer> normalizedSelection(
        final List<Integer> selectedIndices,
        final int operationCount
    ) {
        final ArrayList<Integer> normalized = new ArrayList<>();
        if (selectedIndices == null) {
            return normalized;
        }
        for (int i = 0; i < selectedIndices.size(); i++) {
            final int index = selectedIndices.get(i);
            if (
                index >= 0
                && index < operationCount
                && !normalized.contains(index)
            ) {
                normalized.add(index);
            }
        }
        normalized.sort(Integer::compareTo);
        return normalized;
    }
}
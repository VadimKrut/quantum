/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.operation;

import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import javafx.scene.control.ListCell;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.TransferMode;

/**
 * Создает строки списка операций с drag-and-drop перестановкой без знания о Quantum IR.
 */
public final class DesktopOperationListCellFactory {

    public ListCell<String> create(
        final IntConsumer dragSelection,
        final BiConsumer<Integer, Integer> dropHandler,
        final IntSupplier operationCount
    ) {
        final ListCell<String> cell = new ListCell<>() {
            @Override
            protected void updateItem(
                final String item,
                final boolean empty
            ) {
                super.updateItem(
                    item,
                    empty
                );
                setText(empty ? null : item);
            }
        };
        cell.setOnDragDetected(event -> {
            if (
                cell.isEmpty()
                || cell.getScene() == null
            ) {
                event.consume();
                return;
            }
            dragSelection.accept(cell.getIndex());
            final ClipboardContent content = new ClipboardContent();
            content.putString(Integer.toString(cell.getIndex()));
            cell.startDragAndDrop(TransferMode.MOVE).setContent(content);
            event.consume();
        });
        cell.setOnDragOver(event -> {
            if (event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });
        cell.setOnDragDropped(event -> {
            if (event.getDragboard().hasString()) {
                dropHandler.accept(
                    Integer.parseInt(event.getDragboard().getString()),
                    cell.isEmpty() ? operationCount.getAsInt() : cell.getIndex()
                );
                event.setDropCompleted(true);
            }
            event.consume();
        });
        return cell;
    }
}
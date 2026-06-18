/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.layout;

import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Строит центральную панель визуальной схемы и возвращает ScrollPane для синхронизации инспекции.
 */
public final class DesktopCircuitWorkspaceView {

    public DesktopCircuitWorkspaceResult build(
        final String titleText,
        final String hintText,
        final VBox circuitRows,
        final Node inspectorControls,
        final Node phaseDiskNode,
        final Node selectionOverlay,
        final Consumer<StackPane> selectionInstaller
    ) {
        final Label title = new Label(titleText);
        title.getStyleClass().add("panel-title");
        final Label hint = new Label(hintText);
        hint.getStyleClass().add("panel-hint");
        circuitRows.setPadding(new Insets(
            12.0,
            18.0,
            96.0,
            18.0
        ));
        final ScrollPane scrollPane = new ScrollPane(circuitRows);
        scrollPane.setFitToHeight(false);
        scrollPane.setFitToWidth(false);
        scrollPane.getStyleClass().add("canvas-scroll-pane");
        final StackPane canvasStack = new StackPane(
            scrollPane,
            selectionOverlay
        );
        selectionInstaller.accept(canvasStack);
        final HBox visualRow = new HBox(
            12.0,
            canvasStack,
            phaseDiskNode
        );
        HBox.setHgrow(
            canvasStack,
            Priority.ALWAYS
        );
        final VBox panel = new VBox(
            10,
            title,
            hint,
            inspectorControls,
            visualRow
        );
        panel.getStyleClass().add("canvas-panel");
        VBox.setVgrow(
            visualRow,
            Priority.ALWAYS
        );
        return new DesktopCircuitWorkspaceResult(
            panel,
            scrollPane
        );
    }

}
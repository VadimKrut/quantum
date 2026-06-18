/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.layout;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * JavaFX helpers без состояния для создания повторяемых desktop UI nodes.
 */
public final class DesktopUiNodes {

    private DesktopUiNodes() {
    }

    public static VBox section(
        final String title,
        final Node... nodes
    ) {
        final Label label = new Label(title);
        label.getStyleClass().add("section-title");
        final VBox box = new VBox(8);
        box.getChildren().add(label);
        box.getChildren().addAll(nodes);
        box.getStyleClass().add("section-card");
        return box;
    }

    public static HBox fieldRow(
        final String label,
        final Node node
    ) {
        final Label text = new Label(label);
        text.setMinWidth(96);
        final HBox row = new HBox(
            8,
            text,
            node
        );
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(
            node,
            Priority.ALWAYS
        );
        return row;
    }

    public static HBox fieldRow(
        final String label,
        final Node first,
        final Node second
    ) {
        final Label text = new Label(label);
        text.setMinWidth(96);
        final HBox row = new HBox(
            8,
            text,
            first,
            second
        );
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(
            first,
            Priority.ALWAYS
        );
        return row;
    }

    public static FlowPane actionFlow(final Node... nodes) {
        return new FlowPane(
            8,
            8,
            nodes
        );
    }

    public static Label headerLabel(final String text) {
        final Label label = new Label(text);
        label.getStyleClass().add("header-subtitle");
        return label;
    }

    public static ScrollPane scrollable(final Node node) {
        final ScrollPane scrollPane = new ScrollPane(node);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        return scrollPane;
    }

    public static Button primaryButton(
        final String text,
        final Runnable action
    ) {
        final Button button = new Button(text);
        button.setMinWidth(76.0);
        button.setOnAction(event -> action.run());
        button.getStyleClass().add("primary-button");
        return button;
    }

    public static Button secondaryButton(
        final String text,
        final Runnable action
    ) {
        final Button button = new Button(text);
        button.setMinWidth(76.0);
        button.setOnAction(event -> action.run());
        button.getStyleClass().add("secondary-button");
        return button;
    }

    public static Tab tab(
        final String title,
        final Node node
    ) {
        final Tab tab = new Tab(
            title,
            node
        );
        tab.setClosable(false);
        return tab;
    }

    public static void addSetting(
        final GridPane grid,
        final int row,
        final String label,
        final TextField field
    ) {
        grid.add(
            new Label(label),
            0,
            row
        );
        grid.add(
            field,
            1,
            row
        );
        GridPane.setHgrow(
            field,
            Priority.ALWAYS
        );
    }
}
/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.audit;

import java.util.ArrayList;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

/**
 * Находит JavaFX-узлы внутри desktop-окна для smoke-тестов и визуального аудита.
 */
public final class DesktopUiTraversal {

    private DesktopUiTraversal() {
    }

    public static void selectAllTabs(final Stage stage) {
        final ArrayList<TabPane> tabPanes = new ArrayList<>();
        collectNodes(
            stage.getScene().getRoot(),
            TabPane.class,
            tabPanes
        );
        for (int i = 0; i < tabPanes.size(); i++) {
            final TabPane tabPane = tabPanes.get(i);
            for (int j = 0; j < tabPane.getTabs().size(); j++) {
                tabPane.getSelectionModel().select(j);
            }
        }
    }

    public static void selectTabByText(
        final Stage stage,
        final String text
    ) {
        final ArrayList<TabPane> tabPanes = new ArrayList<>();
        collectNodes(
            stage.getScene().getRoot(),
            TabPane.class,
            tabPanes
        );
        for (int paneIndex = 0; paneIndex < tabPanes.size(); paneIndex++) {
            final TabPane tabPane = tabPanes.get(paneIndex);
            for (int tabIndex = 0; tabIndex < tabPane.getTabs().size(); tabIndex++) {
                final Tab tab = tabPane.getTabs().get(tabIndex);
                if (text.equals(tab.getText())) {
                    tabPane.getSelectionModel().select(tab);
                    return;
                }
            }
        }
        throw new IllegalStateException("UI smoke tab was not found: " + text + ".");
    }

    public static void fireVisibleButton(
        final Stage stage,
        final String text
    ) {
        final ArrayList<Button> buttons = new ArrayList<>();
        collectNodes(
            stage.getScene().getRoot(),
            Button.class,
            buttons
        );
        for (int i = 0; i < buttons.size(); i++) {
            final Button button = buttons.get(i);
            if (
                text.equals(button.getText())
                && button.isVisible()
                && !button.isDisabled()
            ) {
                button.fire();
                return;
            }
        }
        throw new IllegalStateException("UI smoke button was not found or is not usable: " + text + ".");
    }

    public static <T extends Node> void collectNodes(
        final Node node,
        final Class<T> type,
        final ArrayList<T> result
    ) {
        if (type.isInstance(node)) {
            result.add(type.cast(node));
        }
        if (
            node instanceof ScrollPane scrollPane
            && scrollPane.getContent() != null
        ) {
            collectNodes(
                scrollPane.getContent(),
                type,
                result
            );
        }
        if (node instanceof Parent parent) {
            for (int i = 0; i < parent.getChildrenUnmodifiable().size(); i++) {
                collectNodes(
                    parent.getChildrenUnmodifiable().get(i),
                    type,
                    result
                );
            }
        }
    }
}
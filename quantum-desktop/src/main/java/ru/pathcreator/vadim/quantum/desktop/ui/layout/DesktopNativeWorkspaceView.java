/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.layout;

import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Собирает основной native workspace из палитры, визуальной схемы и панели результатов.
 */
public final class DesktopNativeWorkspaceView {

    public Node build(
        final Node palette,
        final Node circuitCanvas,
        final Node resultTabs,
        final Node generatedCodePanel,
        final boolean showGeneratedCodePanel
    ) {
        final Node rightPanel = showGeneratedCodePanel
            ? rightDock(
                resultTabs,
                generatedCodePanel
            )
            : resultTabs;
        final SplitPane splitPane = new SplitPane(
            DesktopUiNodes.scrollable(palette),
            circuitCanvas,
            rightPanel
        );
        splitPane.setDividerPositions(
            0.25,
            0.68
        );
        return splitPane;
    }

    private static Node rightDock(
        final Node resultTabs,
        final Node generatedCodePanel
    ) {
        final VBox dock = new VBox(
            8.0,
            resultTabs,
            generatedCodePanel
        );
        VBox.setVgrow(
            resultTabs,
            Priority.ALWAYS
        );
        dock.getStyleClass().add("right-dock");
        return dock;
    }
}
/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.layout;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;

/**
 * Строит workspace для внешних форматов, оставляя импорт/экспорт в application workflow.
 */
public final class DesktopExternalWorkspaceView {

    public Node build(
        final TextArea sourceArea,
        final ComboBox<IntegrationFormat> inputFormatBox,
        final ComboBox<IntegrationFormat> targetFormatBox,
        final TextArea resultArea,
        final TextArea generatedArea,
        final Runnable openAction,
        final Runnable importAction,
        final Runnable compileAction
    ) {
        sourceArea.setWrapText(false);
        resultArea.setWrapText(false);
        generatedArea.setWrapText(false);
        final HBox toolbar = new HBox(
            8,
            new Label("Input"),
            inputFormatBox,
            new Label("Target"),
            targetFormatBox,
            DesktopUiNodes.secondaryButton(
                "Open External File",
                openAction
            ),
            DesktopUiNodes.primaryButton(
                "Import -> Native JSON",
                importAction
            ),
            DesktopUiNodes.primaryButton(
                "Import -> Export",
                compileAction
            )
        );
        toolbar.setPadding(new Insets(12));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        final SplitPane splitPane = new SplitPane(
            sourceArea,
            new TabPane(
                DesktopUiNodes.tab(
                    "Result",
                    resultArea
                ),
                DesktopUiNodes.tab(
                    "Generated",
                    generatedArea
                )
            )
        );
        splitPane.setDividerPositions(0.48);
        final BorderPane pane = new BorderPane();
        pane.setTop(toolbar);
        pane.setCenter(splitPane);
        return pane;
    }
}
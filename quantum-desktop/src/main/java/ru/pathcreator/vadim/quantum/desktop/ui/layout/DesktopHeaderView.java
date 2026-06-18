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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;

/**
 * Строит верхнюю панель desktop workbench с быстрыми действиями и статусными badges.
 */
public final class DesktopHeaderView {

    public Node build(
        final String titleText,
        final String subtitleText,
        final String alphaText,
        final String versionText,
        final String targetText,
        final String validateText,
        final String simulateText,
        final String exportText,
        final Node viewMenu,
        final String languageText,
        final ComboBox<String> languageBox,
        final String themeText,
        final ComboBox<String> themeBox,
        final Label programBadgeLabel,
        final Label healthBadgeLabel,
        final Label targetBadgeLabel,
        final ComboBox<IntegrationFormat> targetFormatBox,
        final Runnable validateAction,
        final Runnable simulateAction,
        final Runnable exportAction
    ) {
        final Label title = new Label(titleText);
        title.getStyleClass().add("header-title");
        final Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("header-subtitle");
        final VBox titleBox = new VBox(
            2,
            title,
            subtitle
        );
        final Label alpha = new Label(alphaText);
        alpha.getStyleClass().add("header-alpha-notice");
        final Label version = new Label(versionText);
        version.getStyleClass().add("header-version");
        final Region spacer = new Region();
        final Region rightSpacer = new Region();
        HBox.setHgrow(
            spacer,
            Priority.ALWAYS
        );
        HBox.setHgrow(
            rightSpacer,
            Priority.ALWAYS
        );
        final HBox actionRow = new HBox(
            14,
            titleBox,
            spacer,
            alpha,
            rightSpacer,
            DesktopUiNodes.primaryButton(
                validateText,
                validateAction
            ),
            DesktopUiNodes.primaryButton(
                simulateText,
                simulateAction
            ),
            DesktopUiNodes.primaryButton(
                exportText,
                exportAction
            ),
            version
        );
        actionRow.setAlignment(Pos.CENTER_LEFT);
        final HBox settingsRow = new HBox(
            10,
            programBadgeLabel,
            healthBadgeLabel,
            targetBadgeLabel,
            DesktopUiNodes.headerLabel(targetText),
            targetFormatBox,
            viewMenu,
            DesktopUiNodes.headerLabel(languageText),
            languageBox,
            DesktopUiNodes.headerLabel(themeText),
            themeBox
        );
        settingsRow.setAlignment(Pos.CENTER_LEFT);
        settingsRow.getStyleClass().add("workbench-header-secondary");
        final VBox header = new VBox(
            8,
            actionRow,
            settingsRow
        );
        header.getStyleClass().add("workbench-header");
        return header;
    }
}
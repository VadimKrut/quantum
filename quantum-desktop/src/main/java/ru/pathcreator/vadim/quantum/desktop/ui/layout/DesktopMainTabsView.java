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
import javafx.scene.control.TabPane;

/**
 * Строит верхний набор рабочих вкладок desktop-приложения.
 */
public final class DesktopMainTabsView {

    public TabPane build(
        final String nativeTabText,
        final String externalTabText,
        final String libraryTabText,
        final String settingsTabText,
        final Node nativeWorkspace,
        final Node externalWorkspace,
        final Node libraryWorkspace,
        final Node executionSettings
    ) {
        final TabPane tabs = new TabPane(
            DesktopUiNodes.tab(
                nativeTabText,
                nativeWorkspace
            ),
            DesktopUiNodes.tab(
                externalTabText,
                externalWorkspace
            ),
            DesktopUiNodes.tab(
                libraryTabText,
                libraryWorkspace
            ),
            DesktopUiNodes.tab(
                settingsTabText,
                executionSettings
            )
        );
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        return tabs;
    }
}
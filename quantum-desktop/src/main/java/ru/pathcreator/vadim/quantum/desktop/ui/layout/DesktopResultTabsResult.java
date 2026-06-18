/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.layout;

import java.util.List;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/**
 * Результат сборки правой панели: готовый TabPane и список вкладок expert-режима.
 */
public record DesktopResultTabsResult(
    TabPane tabs,
    List<Tab> expertTabs
) {

}
/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.extension;

import java.util.List;
import ru.pathcreator.vadim.quantum.desktop.chemistry.ui.LaboratoryWorkspace;

public final class DesktopWorkspaceExtensions {

  private DesktopWorkspaceExtensions() {}

  public static List<DesktopWorkspaceExtension> create() {
    return Boolean.parseBoolean(System.getProperty("quantum.chemistry.enabled", "true"))
        ? List.of(new LaboratoryWorkspace())
        : List.of();
  }
}

/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.extension;

import javafx.scene.Node;
import javafx.scene.input.KeyEvent;
import javafx.stage.Window;

/** Small local workspace contract; the Studio host does not depend on domain models. */
public interface DesktopWorkspaceExtension {

  default String id() {
    return this.getClass().getName();
  }

  String title();

  Node content(final Window owner);

  boolean requestClose();

  void shortcut(final KeyEvent event);

  default void onAttention(final Runnable select) {}
}

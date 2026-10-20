/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

/** Small vector tool glyphs; accessible names and help live on the controls. */
final class LaboratoryIcons {
  private LaboratoryIcons() {}

  static Node create(final String name) {
    final SVGPath path = new SVGPath();
    path.setContent(
        switch (name) {
          case "select" -> "M4 3 L4 20 L9 15 L13 22 L16 20 L12 13 L19 12 Z";
          case "select-all" ->
              "M3 8 V3 H8 M16 3 H21 V8 M21 16 V21 H16 M8 21 H3 V16 M8 12 L12 8 L16 12 L12 16 Z";
          case "pan" ->
              "M7 12 V5 Q7 2 10 4 V10 V3 Q12 0 13 3 V10 V4 Q16 2 16 5 V11 V7 Q19 5 19 8 V15 Q19 21 14 22 H10 Q7 22 5 18 L2 13 Q2 10 5 12 L7 14 Z";
          case "rotate" -> "M19 8 A8 8 0 1 0 20 16 M19 3 V8 H14";
          case "rotate-left" -> "M5 8 A8 8 0 1 1 4 16 M5 3 V8 H10";
          case "flip-x" ->
              "M12 2 V5 M12 8 V11 M12 14 V17 M12 20 V23 M3 6 V19 L9 19 Z M21 6 V19 L15 19 Z";
          case "flip-y" ->
              "M2 12 H5 M8 12 H11 M14 12 H17 M20 12 H23 M6 3 H19 L19 9 Z M6 21 H19 L19 15 Z";
          case "bond" -> "M4 20 L20 4";
          case "double" -> "M3 18 L18 3 M6 21 L21 6";
          case "triple" -> "M2 16 L16 2 M5 19 L19 5 M8 22 L22 8";
          case "ring" -> "M12 2 L21 7 V17 L12 22 L3 17 V7 Z";
          case "benzene" -> "M12 2 L21 7 V17 L12 22 L3 17 V7 Z M12 6 L17 9 M17 15 L12 18 M7 15 V9";
          case "pentagon" -> "M12 2 L22 10 L18 22 H6 L2 10 Z";
          case "erase" -> "M3 15 L14 3 L22 11 L11 22 H9 Z M9 9 L17 17";
          case "delete" -> "M5 6 H19 M9 3 H15 M7 6 L8 21 H16 L17 6 M10 10 V17 M14 10 V17";
          case "undo" -> "M9 5 L3 11 L9 17 M3 11 H14 Q22 11 21 20";
          case "redo" -> "M15 5 L21 11 L15 17 M21 11 H10 Q2 11 3 20";
          default -> throw new IllegalArgumentException("Unknown icon: " + name);
        });
    path.getStyleClass().add("laboratory-tool-icon");
    final StackPane box = new StackPane(path);
    box.setMinSize(24, 24);
    box.setPrefSize(24, 24);
    box.setMaxSize(24, 24);
    return box;
  }
}

/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import javafx.scene.control.Dialog;
import javafx.stage.Window;

/** Apply the current workspace palette to a separately rooted JavaFX dialog. */
final class LaboratoryDialogStyle {
  private LaboratoryDialogStyle() {}

  static void apply(final Dialog<?> dialog, final Window owner) {
    final var pane = dialog.getDialogPane();
    pane.setId("chemistry-workspace");
    for (final String style : java.util.List.of("quantum-root", "chemistry-dialog")) {
      if (!pane.getStyleClass().contains(style)) {
        pane.getStyleClass().add(style);
      }
    }
    pane.getStyleClass().removeAll("theme-dark", "theme-light");
    pane.getStyleClass()
        .add(
            owner != null
                    && owner.getScene() != null
                    && owner.getScene().getRoot().getStyleClass().contains("theme-dark")
                ? "theme-dark"
                : "theme-light");
    final String stylesheet =
        LaboratoryDialogStyle.class
            .getResource("/ru/pathcreator/vadim/quantum/desktop/ui/quantum-desktop.css")
            .toExternalForm();
    if (!pane.getStylesheets().contains(stylesheet)) {
      pane.getStylesheets().add(stylesheet);
    }
  }
}

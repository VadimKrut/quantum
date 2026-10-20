/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/** Small entry points inside the existing empty drawing workspace. */
final class LaboratoryStartPane extends VBox {
  private final Label title = new Label("Нарисуйте первую структуру");
  private final Button projects = new Button("Проекты и опыты");

  LaboratoryStartPane(
      final Runnable experiment,
      final Runnable open,
      final Runnable name,
      final Runnable exchange,
      final Runnable draw) {
    super(8);
    this.setId("chemistry-start-pane");
    this.setPadding(new Insets(16, 20, 16, 20));
    this.setMinWidth(0);
    this.setMinHeight(Region.USE_PREF_SIZE);
    this.title.setStyle("-fx-font-size: 20; -fx-font-weight: bold;");
    this.title.setWrapText(true);
    final Label hint =
        new Label(
            "Выберите инструмент слева и начните на холсте. Готовую структуру можно ввести через SMILES или название.");
    hint.setWrapText(true);
    final Button drawing = button("Рисовать", "draw", draw);
    drawing.getStyleClass().add("laboratory-save");
    final Button smiles = button("Ввести SMILES", "structure", exchange);
    final Button named = button("По названию", "name", name);
    this.projects.setId("chemistry-start-projects");
    this.projects.setVisible(false);
    this.projects.setManaged(false);
    this.getChildren()
        .setAll(this.title, hint, new FlowPane(10, 8, drawing, smiles, named, this.projects));
  }

  void projects(final Runnable action) {
    this.projects.setOnAction(event -> action.run());
    this.projects.setVisible(false);
    this.projects.setManaged(false);
  }

  void present(final boolean visible, final String heading) {
    this.title.setText("Нарисуйте первую структуру");
    this.setVisible(visible);
    this.setManaged(visible);
  }

  private static Button button(final String text, final String id, final Runnable action) {
    final Button button = new Button(text);
    button.setId("chemistry-start-" + id);
    button.setMinWidth(Region.USE_PREF_SIZE);
    button.setOnAction(event -> action.run());
    return button;
  }
}

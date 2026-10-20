/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.Objects;
import java.util.function.Function;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryComponent;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;

/** Read-only query identity and drawing; it never changes the selected value or its molecule. */
final class LaboratoryQuerySelector<T> extends VBox {
  private final ComboBox<T> selector;
  private final Function<T, String> title, identifier;
  private final Function<T, MoleculeDrawing> drawing;
  private final Canvas canvas = new Canvas(270, 116);
  private final StackPane preview = new StackPane(this.canvas);

  LaboratoryQuerySelector(
      final String heading,
      final ComboBox<T> selector,
      final Function<T, String> title,
      final Function<T, String> identifier,
      final Function<T, MoleculeDrawing> drawing) {
    super(5);
    this.selector = Objects.requireNonNull(selector);
    this.title = Objects.requireNonNull(title);
    this.identifier = Objects.requireNonNull(identifier);
    this.drawing = Objects.requireNonNull(drawing);
    final Label label = new Label(heading);
    label.getStyleClass().add("muted-label");
    label.setWrapText(true);
    label.setLabelFor(selector);
    this.setMinWidth(0);
    this.setPrefWidth(selector.getPrefWidth() > 0 ? selector.getPrefWidth() : 270);
    this.setMaxWidth(this.getPrefWidth());
    selector.setMaxWidth(Double.MAX_VALUE);
    selector.setCellFactory(view -> this.cell());
    selector.setButtonCell(this.cell());
    this.preview.setId(selector.getId() + "-preview");
    this.preview.setMinSize(0, 116);
    this.preview.setPrefHeight(116);
    this.preview.setMaxHeight(116);
    this.preview.setAccessibleText(heading + " — двумерная структура выбранного соединения");
    this.canvas.widthProperty().bind(this.preview.widthProperty());
    this.canvas.heightProperty().bind(this.preview.heightProperty());
    this.getChildren().setAll(label, selector, this.preview);
    selector.valueProperty().addListener((o, a, b) -> this.update());
    this.canvas.widthProperty().addListener(o -> this.paint());
    this.canvas.heightProperty().addListener(o -> this.paint());
    LaboratoryPreviewTheme.track(this.canvas, this::paint);
    this.update();
  }

  static String title(final LaboratoryComponent component, final int ordinal) {
    final String role =
        switch (component.role()) {
          case REACTANT -> "Исходник";
          case PRODUCT -> "Продукт";
          case SOLVENT -> "Растворитель";
          case CATALYST -> "Катализатор";
          case ADDITIVE -> "Добавка";
          case ACID -> "Кислота";
          case BASE -> "Основание";
          case LIGAND -> "Лиганд";
          case ATMOSPHERE -> "Атмосфера";
          case WORKUP -> "Обработка";
          case OTHER -> "Компонент";
        };
    return "№" + ordinal + " · " + role + " · " + component.molecule().displayName();
  }

  private ListCell<T> cell() {
    return new ListCell<>() {
      @Override
      protected void updateItem(final T item, final boolean empty) {
        super.updateItem(item, empty);
        this.setGraphic(null);
        this.setText(empty || item == null ? null : LaboratoryQuerySelector.this.title.apply(item));
        this.setTooltip(empty || item == null ? null : LaboratoryQuerySelector.this.tooltip(item));
      }
    };
  }

  private Tooltip tooltip(final T item) {
    final String id = this.identifier.apply(item);
    return new Tooltip(this.title.apply(item) + (id == null || id.isBlank() ? "" : "\nID: " + id));
  }

  private void update() {
    final T selected = this.selector.getValue();
    final boolean present = selected != null && this.drawing.apply(selected) != null;
    this.preview.setVisible(present);
    this.preview.setManaged(present);
    this.selector.setTooltip(selected == null ? null : this.tooltip(selected));
    this.paint();
  }

  private void paint() {
    final var graphics = this.canvas.getGraphicsContext2D();
    LaboratoryPreviewTheme.clear(graphics);
    final T selected = this.selector.getValue();
    final MoleculeDrawing value = selected == null ? null : this.drawing.apply(selected);
    if (value == null || this.canvas.getWidth() <= 0 || this.canvas.getHeight() <= 0) return;
    final var bounds = LaboratoryCanvas.previewBounds(value);
    final double scale =
        Math.max(
            .01,
            Math.min(
                1.8,
                Math.min(
                    (this.canvas.getWidth() - 26) / Math.max(1, bounds.getWidth()),
                    (this.canvas.getHeight() - 20) / Math.max(1, bounds.getHeight()))));
    graphics.save();
    try {
      graphics.translate(
          this.canvas.getWidth() / 2 - (bounds.getMinX() + bounds.getMaxX()) * scale / 2,
          this.canvas.getHeight() / 2 - (bounds.getMinY() + bounds.getMaxY()) * scale / 2);
      graphics.scale(scale, scale);
      LaboratoryPreviewTheme.paint(graphics, value);
    } finally {
      graphics.restore();
    }
  }
}

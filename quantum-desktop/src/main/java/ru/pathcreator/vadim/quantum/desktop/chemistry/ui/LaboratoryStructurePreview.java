/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;

/** A pickable view of the same core atom IDs, without exposing implementation IDs as labels. */
final class LaboratoryStructurePreview extends VBox {
  private final Canvas canvas = new Canvas(300, 220);
  private final CheckBox allHydrogens = new CheckBox("Все явные H");
  private final Label selectedLabel =
      LaboratoryStructurePage.note("Нажмите на атом в рисунке или выберите строку.");
  private final Map<AtomId, Point2D> points = new LinkedHashMap<>();
  private String hint = "Нажмите на атом в рисунке или выберите строку.";
  private MoleculeDrawing drawing;
  private AtomId selected;
  private Consumer<AtomId> pick = id -> {};

  LaboratoryStructurePreview() {
    super(6);
    this.setId("chemistry-structure-preview");
    this.canvas.setId("chemistry-structure-preview-canvas");
    final StackPane viewport = new StackPane(this.canvas);
    viewport.setMinSize(0, 140);
    viewport.setPrefSize(360, 260);
    VBox.setVgrow(viewport, Priority.ALWAYS);
    this.canvas.widthProperty().bind(viewport.widthProperty());
    this.canvas.heightProperty().bind(viewport.heightProperty());
    this.getChildren().setAll(new FlowPane(8, 6, this.allHydrogens), viewport, this.selectedLabel);
    this.setMinWidth(0);
    this.setMinHeight(190);
    this.allHydrogens.setId("chemistry-structure-all-h");
    this.allHydrogens.selectedProperty().addListener((o, a, b) -> this.paint());
    this.canvas.widthProperty().addListener((o, a, b) -> this.paint());
    this.canvas.heightProperty().addListener((o, a, b) -> this.paint());
    LaboratoryPreviewTheme.track(this.canvas, this::paint);
    this.canvas.setOnMouseClicked(
        event -> {
          if (event.getButton() != javafx.scene.input.MouseButton.PRIMARY) {
            return;
          }
          this.points.entrySet().stream()
              .filter(e -> e.getValue().distance(event.getX(), event.getY()) < 15)
              .min(
                  java.util.Comparator.comparingDouble(
                      e -> e.getValue().distance(event.getX(), event.getY())))
              .ifPresent(e -> this.pick.accept(e.getKey()));
        });
  }

  void hint(final String hint) {
    this.hint = hint;
    this.paint();
  }

  void onPick(final Consumer<AtomId> pick) {
    this.pick = pick;
  }

  void show(final MoleculeDrawing drawing, final AtomId selected) {
    this.drawing = drawing;
    this.selected = selected;
    this.paint();
  }

  private void paint() {
    final var g = this.canvas.getGraphicsContext2D();
    LaboratoryPreviewTheme.clear(g);
    this.points.clear();
    if (this.drawing == null || this.drawing.atoms().isEmpty()) {
      return;
    }
    final var projection =
        ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeAbbreviations.projection(
            this.drawing);
    final var shown =
        this.allHydrogens.isSelected()
                || this.selected != null && projection.hidden().contains(this.selected)
            ? ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeAbbreviations
                .expandAll(this.drawing)
            : this.drawing;
    final var atoms =
        this.allHydrogens.isSelected() ? shown.atoms() : LaboratoryCanvas.previewAtoms(shown);
    final var bounds = LaboratoryCanvas.previewBounds(shown);
    double minX = bounds.getMinX(),
        maxX = bounds.getMaxX(),
        minY = bounds.getMinY(),
        maxY = bounds.getMaxY();
    if (this.allHydrogens.isSelected() || this.selected != null) {
      for (final var atom : this.drawing.atoms()) {
        if (this.allHydrogens.isSelected() || atom.atomId().equals(this.selected)) {
          minX = Math.min(minX, atom.point2D().x() - 15);
          maxX = Math.max(maxX, atom.point2D().x() + 15);
          minY = Math.min(minY, atom.point2D().y() - 15);
          maxY = Math.max(maxY, atom.point2D().y() + 15);
        }
      }
    }
    final double scale =
        Math.max(
            0.01,
            Math.min(
                1.7,
                Math.min(
                    (this.canvas.getWidth() - 38) / Math.max(1, maxX - minX),
                    (this.canvas.getHeight() - 38) / Math.max(1, maxY - minY))));
    final double dx = this.canvas.getWidth() / 2 - (minX + maxX) * scale / 2,
        dy = this.canvas.getHeight() / 2 - (minY + maxY) * scale / 2;
    g.save();
    try {
      g.translate(dx, dy);
      g.scale(scale, scale);
      if (this.allHydrogens.isSelected()) {
        LaboratoryPreviewTheme.paintAll(g, shown, this.selected);
      } else {
        LaboratoryPreviewTheme.paint(g, shown);
      }
    } finally {
      g.restore();
    }
    for (final var atom : atoms) {
      this.points.put(
          atom.atomId(),
          new Point2D(dx + atom.point2D().x() * scale, dy + atom.point2D().y() * scale));
    }
    final var atom =
        this.selected == null
            ? null
            : this.drawing.molecule().atoms().stream()
                .filter(a -> a.id().equals(this.selected))
                .findFirst()
                .orElse(null);
    if (atom == null) {
      this.selectedLabel.setText(this.hint);
      return;
    }
    final int index = this.drawing.molecule().atoms().indexOf(atom) + 1;
    final var position =
        this.drawing.atoms().stream()
            .filter(a -> a.atomId().equals(this.selected))
            .findFirst()
            .orElseThrow()
            .point2D();
    final double x = dx + position.x() * scale, y = dy + position.y() * scale;
    g.setStroke(LaboratoryPreviewTheme.palette(this.canvas).selection());
    g.setLineWidth(2);
    g.strokeRoundRect(x - 13, y - 14, 26, 28, 5, 5);
    this.selectedLabel.setText(
        "Выбран: "
            + atom.element().symbol().value()
            + " · "
            + index
            + (atom.hasIsotope() ? " · изотоп " + atom.isotope().massNumber() : ""));
  }
}

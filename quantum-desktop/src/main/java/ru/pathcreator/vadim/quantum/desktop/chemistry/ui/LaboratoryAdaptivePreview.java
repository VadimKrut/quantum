/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;

/** Screen fit only: the chemical drawing and its renderer remain unchanged. */
final class LaboratoryAdaptivePreview extends StackPane {
  private final Canvas canvas = new Canvas(320, 150);
  private final Supplier<MoleculeDrawing> drawing;
  private final BiConsumer<GraphicsContext, Double> overlay;

  LaboratoryAdaptivePreview(final Supplier<MoleculeDrawing> drawing) {
    this(drawing, (graphics, scale) -> {});
  }

  LaboratoryAdaptivePreview(
      final Supplier<MoleculeDrawing> drawing, final BiConsumer<GraphicsContext, Double> overlay) {
    this.drawing = drawing;
    this.overlay = overlay;
    this.canvas.setManaged(false);
    this.getChildren().add(this.canvas);
    this.setMinWidth(0);
    this.setMinHeight(Region.USE_PREF_SIZE);
    this.setMaxHeight(Region.USE_PREF_SIZE);
    this.canvas.widthProperty().bind(this.widthProperty());
    LaboratoryPreviewTheme.track(this.canvas, this::refresh);
    this.refresh();
  }

  Canvas canvas() {
    return this.canvas;
  }

  static boolean complex(final MoleculeDrawing drawing) {
    final var bounds = LaboratoryCanvas.previewBounds(drawing);
    return LaboratoryCanvas.previewAtoms(drawing).size() > 14
        || Math.max(bounds.getWidth(), bounds.getHeight()) > 300;
  }

  void refresh() {
    this.requestLayout();
    this.paint();
  }

  @Override
  public javafx.geometry.Orientation getContentBias() {
    return javafx.geometry.Orientation.HORIZONTAL;
  }

  @Override
  protected double computePrefWidth(final double height) {
    return 320;
  }

  @Override
  protected double computePrefHeight(final double width) {
    final var value = this.drawing.get();
    if (value == null) {
      return 150;
    }
    final var bounds = LaboratoryCanvas.previewBounds(value);
    final boolean complex = complex(value);
    final double fit =
        Math.max(
            .01,
            Math.min(
                complex ? 1.35 : 1.0,
                ((width < 0 ? 320 : width) - 32) / Math.max(1, bounds.getWidth())));
    return Math.max(150, Math.min(complex ? 340 : 190, bounds.getHeight() * fit + 32));
  }

  @Override
  protected void layoutChildren() {
    this.canvas.setHeight(this.getHeight());
    this.canvas.relocate(0, 0);
    this.paint();
  }

  private void paint() {
    final var value = this.drawing.get();
    if (value == null) {
      return;
    }
    final var bounds = LaboratoryCanvas.previewBounds(value);
    final boolean complex = complex(value);
    final double cap = complex ? 1.35 : 1.0;
    final double width = Math.max(1, this.getWidth()),
        fit = Math.max(.01, Math.min(cap, (width - 32) / Math.max(1, bounds.getWidth())));
    final double height = Math.max(1, this.getHeight());
    final double scale =
        Math.max(.01, Math.min(fit, (height - 32) / Math.max(1, bounds.getHeight())));
    final var graphics = this.canvas.getGraphicsContext2D();
    LaboratoryPreviewTheme.clear(graphics);
    graphics.save();
    try {
      graphics.translate(
          width / 2 - (bounds.getMinX() + bounds.getMaxX()) * scale / 2,
          height / 2 - (bounds.getMinY() + bounds.getMaxY()) * scale / 2);
      graphics.scale(scale, scale);
      LaboratoryPreviewTheme.paint(graphics, value);
      this.overlay.accept(graphics, scale);
    } finally {
      graphics.restore();
    }
    this.canvas.setAccessibleText(
        value.molecule().displayName() + ", " + value.molecule().formula().hillNotation());
  }
}

/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;

/** Explicit screen-preview palette. Paper/export rendering never opts into this helper. */
final class LaboratoryPreviewTheme {
  static final Palette PAPER = new Palette(false), DARK = new Palette(true);

  record Palette(boolean dark) {
    Color background() {
      return Color.web(this.dark ? "#111827" : "#ffffff");
    }

    Color ink() {
      return Color.web(this.dark ? "#e3ebf5" : "#192638");
    }

    Color bond() {
      return Color.web(this.dark ? "#d8e2ef" : "#20252b");
    }

    Color wedge() {
      return Color.web(this.dark ? "#d8e2ef" : "#263549");
    }

    Color selection() {
      return Color.web(this.dark ? "#83bcff" : "#387cb8");
    }

    Color selectedBackground() {
      return Color.web(this.dark ? "#25415f" : "#e5f1fc");
    }

    Color stereo(boolean warning) {
      return Color.web(
          this.dark ? warning ? "#edbd72" : "#c8a9ef" : warning ? "#a55b00" : "#6b3aa0");
    }

    Color atom(String symbol) {
      return Color.web(
          switch (symbol) {
            case "O" -> this.dark ? "#f28c8c" : "#c53030";
            case "N" -> this.dark ? "#8eb5ff" : "#2453b8";
            case "Cl", "F", "Br" -> this.dark ? "#81cf9d" : "#16803c";
            default -> this.dark ? "#e3ebf5" : "#192638";
          });
    }
  }

  private LaboratoryPreviewTheme() {}

  static Palette palette(final Canvas canvas) {
    for (Node node = canvas; node != null; node = node.getParent()) {
      if (node.getStyleClass().contains("theme-dark")) {
        return DARK;
      }
      if (node.getStyleClass().contains("theme-light")) {
        return PAPER;
      }
    }
    return canvas.getScene() != null
            && canvas.getScene().getRoot().getStyleClass().contains("theme-dark")
        ? DARK
        : PAPER;
  }

  static void clear(final GraphicsContext graphics) {
    final Canvas canvas = graphics.getCanvas();
    graphics.save();
    try {
      graphics.setTransform(1, 0, 0, 1, 0, 0);
      final double width = Math.ceil(canvas.getWidth()), height = Math.ceil(canvas.getHeight());
      graphics.clearRect(0, 0, width, height);
      graphics.setFill(palette(canvas).background());
      graphics.fillRect(0, 0, width, height);
    } finally {
      graphics.restore();
    }
  }

  static void paint(final GraphicsContext graphics, final MoleculeDrawing drawing) {
    paint(graphics, drawing, true);
  }

  static void paint(
      final GraphicsContext graphics, final MoleculeDrawing drawing, final boolean stereoLabels) {
    LaboratoryCanvas.paintPreview(graphics, drawing, stereoLabels, palette(graphics.getCanvas()));
  }

  static void paintAll(
      final GraphicsContext graphics, final MoleculeDrawing drawing, final AtomId selected) {
    LaboratoryCanvas.paintMolecule(graphics, drawing, selected, palette(graphics.getCanvas()));
  }

  static Color highlight(final Canvas canvas) {
    return Color.web(palette(canvas).dark() ? "#30584e" : "#b9ead2");
  }

  /** Repaint on theme and Scene-root changes; detach old-root listeners when a tile leaves the Scene. */
  static void track(final Canvas canvas, final Runnable redraw) {
    final class Watch {
      Parent root;
      final javafx.collections.ListChangeListener<String> classes = change -> redraw.run();
      final javafx.beans.value.ChangeListener<Parent> roots =
          (o, before, after) -> this.root(after);

      void root(final Parent value) {
        if (this.root != null) {
          this.root.getStyleClass().removeListener(this.classes);
        }
        this.root = value;
        if (value != null) {
          value.getStyleClass().addListener(this.classes);
        }
        redraw.run();
      }
    }
    final Watch watch = new Watch();
    canvas
        .sceneProperty()
        .addListener(
            (o, before, after) -> {
              if (before != null) {
                before.rootProperty().removeListener(watch.roots);
              }
              if (after != null) {
                after.rootProperty().addListener(watch.roots);
              }
              watch.root(after == null ? null : after.getRoot());
            });
    if (canvas.getScene() != null) {
      canvas.getScene().rootProperty().addListener(watch.roots);
      watch.root(canvas.getScene().getRoot());
    }
  }
}

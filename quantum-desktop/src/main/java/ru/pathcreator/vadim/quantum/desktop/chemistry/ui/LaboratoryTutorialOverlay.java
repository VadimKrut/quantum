/* Copyright 2026 Vadim Aleksandrovich Zaletaev. SPDX-License-Identifier: MPL-2.0 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

/** Four shades leave the actual control clickable. No duplicate controls or simulated clicks. */
final class LaboratoryTutorialOverlay extends Pane {
  private final Rectangle[] shades = {
    new Rectangle(), new Rectangle(), new Rectangle(), new Rectangle()
  };
  private final Rectangle outline = new Rectangle();
  private final Path arrow = new Path();
  private Node target;

  LaboratoryTutorialOverlay() {
    setId("tutorial-spotlight");
    setPickOnBounds(false);
    setMinSize(0, 0);
    for (var shade : shades) {
      shade.setFill(Color.rgb(9, 18, 38, 0.30));
      shade.setMouseTransparent(true);
      getChildren().add(shade);
    }
    outline.setFill(Color.TRANSPARENT);
    outline.setStroke(Color.web("#5687ff"));
    outline.setStrokeWidth(3);
    outline.setArcWidth(12);
    outline.setArcHeight(12);
    outline.setMouseTransparent(true);
    arrow.setStroke(Color.web("#5687ff"));
    arrow.setStrokeWidth(3);
    arrow.setFill(null);
    arrow.setStrokeLineCap(StrokeLineCap.ROUND);
    arrow.setMouseTransparent(true);
    getChildren().addAll(outline, arrow);
    setVisible(false);
  }

  void pointTo(Node target) {
    this.target = target;
    setVisible(target != null);
    requestLayout();
  }

  private void box(Rectangle r, double x, double y, double w, double h) {
    r.setX(x);
    r.setY(y);
    r.setWidth(Math.max(0, w));
    r.setHeight(Math.max(0, h));
  }

  @Override
  protected void layoutChildren() {
    if (target == null || target.getScene() != getScene()) {
      setVisible(false);
      return;
    }
    Bounds bounds = sceneToLocal(target.localToScene(target.getBoundsInLocal()));
    double w = getWidth(), h = getHeight();
    double x = Math.max(3, bounds.getMinX() - 5), y = Math.max(3, bounds.getMinY() - 5);
    double right = Math.min(w - 3, bounds.getMaxX() + 5),
        bottom = Math.min(h - 3, bounds.getMaxY() + 5);
    if (right <= x || bottom <= y) {
      setVisible(false);
      return;
    }
    box(shades[0], 0, 0, w, y);
    box(shades[1], 0, bottom, w, h - bottom);
    box(shades[2], 0, y, x, bottom - y);
    box(shades[3], right, y, w - right, bottom - y);
    box(outline, x, y, right - x, bottom - y);
    double tx = (x + right) / 2, ty = y > 70 ? y - 6 : bottom + 6;
    double sx = Math.min(w - 24, Math.max(24, tx + 80)),
        sy = y > 70 ? Math.max(18, y - 55) : Math.min(h - 18, bottom + 55);
    double angle = Math.atan2(ty - sy, tx - sx), head = 11;
    arrow
        .getElements()
        .setAll(
            new MoveTo(sx, sy),
            new QuadCurveTo(tx, sy, tx, ty),
            new MoveTo(tx - head * Math.cos(angle - 0.55), ty - head * Math.sin(angle - 0.55)),
            new LineTo(tx, ty),
            new LineTo(tx - head * Math.cos(angle + 0.55), ty - head * Math.sin(angle + 0.55)));
  }
}

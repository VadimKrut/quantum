/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratorySpectralComparison.*;

/** Shared displayed X scale, explicit independent/overlaid Y scales, and untouched source observations. */
final class LaboratorySpectralComparisonPlot extends Region {
  private final Canvas canvas = new Canvas();
  private final Tooltip hint = new Tooltip();
  private Series first, second;
  private boolean overlay, normalized;
  private double zoom = 1, panX, panY, startX, startY, firstDivisor = 1, secondDivisor = 1;

  private record Bounds(double min, double max) {}

  private record Panel(double left, double top, double width, double height, Bounds x, Bounds y) {}

  LaboratorySpectralComparisonPlot() {
    this.getChildren().add(this.canvas);
    this.setMinSize(260, 320);
    this.setPrefHeight(410);
    Tooltip.install(this.canvas, this.hint);
    this.canvas.setOnScroll(
        e -> {
          this.zoom = Math.max(1, Math.min(20, this.zoom * (e.getDeltaY() > 0 ? 1.2 : 1 / 1.2)));
          this.paint();
          e.consume();
        });
    this.canvas.setOnMousePressed(
        e -> {
          this.startX = e.getX();
          this.startY = e.getY();
        });
    this.canvas.setOnMouseDragged(
        e -> {
          this.panX +=
              (e.getX() - this.startX)
                  / Math.max(
                      200, this.getWidth() * (this.overlay || this.getWidth() < 900 ? 1 : .5) - 80);
          this.panY +=
              (e.getY() - this.startY)
                  / Math.max(
                      200,
                      this.getHeight() * (this.overlay || this.getWidth() >= 900 ? 1 : .5) - 70);
          this.startX = e.getX();
          this.startY = e.getY();
          this.paint();
        });
    this.canvas.setOnMouseMoved(e -> this.hint.setText(this.nearest(e.getX(), e.getY())));
    this.sceneProperty()
        .addListener(
            (o, before, after) -> {
              if (before != null) before.getRoot().getStyleClass().removeListener(this.theme);
              if (after != null) after.getRoot().getStyleClass().addListener(this.theme);
              this.paint();
            });
  }

  private final javafx.collections.ListChangeListener<String> theme = c -> this.paint();

  void clear() {
    this.first = null;
    this.second = null;
    this.paint();
  }

  void show(
      final Series first, final Series second, final boolean overlay, final boolean normalized) {
    final boolean changed = this.first != first || this.second != second;
    this.first = first;
    this.second = second;
    this.overlay = overlay;
    this.normalized = normalized;
    this.firstDivisor = normalized ? maximum(first) : 1;
    this.secondDivisor = normalized ? maximum(second) : 1;
    if (changed) this.fit();
    else this.paint();
  }

  void fit() {
    this.zoom = 1;
    this.panX = 0;
    this.panY = 0;
    this.paint();
  }

  @Override
  protected void layoutChildren() {
    this.canvas.setWidth(this.getWidth());
    this.canvas.setHeight(this.getHeight());
    this.paint();
  }

  static boolean dark(final javafx.scene.Node node) {
    for (javafx.scene.Node p = node; p != null; p = p.getParent()) {
      if (p.getStyleClass().contains("theme-dark")) return true;
    }
    return node.getScene() != null
        && node.getScene().getRoot().getStyleClass().contains("theme-dark");
  }

  private double divisor(final Series s) {
    return s == this.first ? this.firstDivisor : this.secondDivisor;
  }

  private static double maximum(final Series s) {
    double max = 0;
    for (final var p : s.points()) max = Math.max(max, Math.abs(p.y()));
    for (final var p : s.marks()) max = Math.max(max, Math.abs(p.y()));
    for (final var p : s.lines()) max = Math.max(max, Math.abs(p.y()));
    return max == 0 ? 1 : max;
  }

  private Bounds bounds(final boolean x, final Series... series) {
    double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
    for (final var s : series) {
      final double scale = x ? 1 : this.divisor(s);
      for (final var p : s.points()) {
        final double v = x ? p.x() : p.y() / scale;
        min = Math.min(min, v);
        max = Math.max(max, v);
      }
      for (final var p : s.marks()) {
        min = Math.min(min, x ? Math.min(p.from(), p.to()) : p.y() / scale);
        max = Math.max(max, x ? Math.max(p.from(), p.to()) : p.y() / scale);
      }
      for (final var p : s.lines()) {
        min = Math.min(min, x ? p.ppm() : p.y() / scale);
        max = Math.max(max, x ? p.ppm() : p.y() / scale);
      }
    }
    if (!Double.isFinite(min) || !Double.isFinite(max)) return new Bounds(0, 1);
    if (min == max) {
      min -= .5;
      max += .5;
    }
    final double padding = (max - min) * .04;
    return new Bounds(min - padding, max + padding);
  }

  private Panel panel(final int index) {
    final double w = this.getWidth(), h = this.getHeight();
    final boolean vertical = w < 900;
    final double cellW = this.overlay || vertical ? w : w / 2,
        cellH = this.overlay || !vertical ? h : h / 2;
    final double dx = this.overlay || vertical ? 0 : index * cellW,
        dy = this.overlay || !vertical ? 0 : index * cellH;
    final var s = index == 0 ? this.first : this.second;
    final Bounds y =
        this.overlay || s.kind() == Kind.CORRELATIONS
            ? this.bounds(false, this.first, this.second)
            : this.bounds(false, s);
    return new Panel(
        dx + 62,
        dy + 36,
        Math.max(20, cellW - 87),
        Math.max(20, cellH - 78),
        this.bounds(true, this.first, this.second),
        y);
  }

  private double x(final double v, final Series s, final Panel p) {
    final double ratio = (v - p.x.min) / (p.x.max - p.x.min);
    return p.left + ((s.reverseX() ? 1 - ratio : ratio) * this.zoom + this.panX) * p.width;
  }

  private double y(final double v, final Series s, final Panel p) {
    final double ratio = (v / this.divisor(s) - p.y.min) / (p.y.max - p.y.min);
    return p.top
        + p.height
        - ((s.reverseY() ? 1 - ratio : ratio) * this.zoom - this.panY) * p.height;
  }

  private double unX(final double pixel, final Series s, final Panel p) {
    final double ratio = ((pixel - p.left) / p.width - this.panX) / this.zoom;
    return p.x.min + (s.reverseX() ? 1 - ratio : ratio) * (p.x.max - p.x.min);
  }

  private double unY(final double pixel, final Series s, final Panel p) {
    final double ratio = ((p.top + p.height - pixel) / p.height + this.panY) / this.zoom;
    return p.y.min + (s.reverseY() ? 1 - ratio : ratio) * (p.y.max - p.y.min);
  }

  private void paint() {
    final var g = this.canvas.getGraphicsContext2D();
    final boolean dark = dark(this);
    final Color background = Color.web(dark ? "#162131" : "#ffffff"),
        text = Color.web(dark ? "#dbe5f2" : "#334155"),
        grid = Color.web(dark ? "#344256" : "#e2e8f0");
    g.setFill(background);
    g.fillRect(0, 0, this.getWidth(), this.getHeight());
    if (this.first == null || this.second == null) return;
    for (int i = 0; i < (this.overlay ? 1 : 2); i++) {
      final var s = i == 0 ? this.first : this.second;
      final var p = this.panel(i);
      g.setStroke(grid);
      g.setFill(text);
      for (int tick = 0; tick <= 5; tick++) {
        final double x = p.left + p.width * tick / 5, y = p.top + p.height - p.height * tick / 5;
        g.strokeLine(x, p.top, x, p.top + p.height);
        g.strokeLine(p.left, y, p.left + p.width, y);
        g.fillText(
            LaboratoryAxisTickText.format(this.unX(x, s, p), (p.x.max - p.x.min) / (5 * this.zoom)),
            x - 16,
            p.top + p.height + 17);
        if (!s.positionsOnly())
          g.fillText(
              LaboratoryAxisTickText.format(
                  this.unY(y, s, p), (p.y.max - p.y.min) / (5 * this.zoom)),
              p.left - 58,
              y + 4);
      }
      g.fillText(s.xLabel(), p.left + p.width / 2 - 35, p.top + p.height + 35);
      g.fillText(
          this.overlay ? "A + B · общие оси" : (i == 0 ? "A" : "B") + " · " + s.record().title(),
          p.left,
          p.top - 18,
          Math.max(20, p.width));
      g.fillText(
          s.positionsOnly() ? s.yLabel() : this.normalized ? "y / max |y|" : s.yLabel(),
          p.left,
          p.top - 3,
          Math.max(20, p.width));
    }
    this.series(this.first, this.panel(0), Color.web(dark ? "#6da8ff" : "#2563eb"), false);
    this.series(
        this.second,
        this.panel(this.overlay ? 0 : 1),
        Color.web(dark ? "#ffb870" : "#c2410c"),
        true);
  }

  private void series(final Series s, final Panel p, final Color color, final boolean second) {
    final var g = this.canvas.getGraphicsContext2D();
    g.save();
    g.beginPath();
    g.rect(p.left - 1, p.top - 1, p.width + 2, p.height + 2);
    g.clip();
    g.setStroke(color);
    g.setFill(color);
    g.setLineWidth(1.6);
    if (s.line()) {
      g.beginPath();
      boolean first = true;
      for (final var point : s.points()) {
        final double x = this.x(point.x(), s, p), y = this.y(point.y(), s, p);
        if (first) {
          g.moveTo(x, y);
          first = false;
        } else g.lineTo(x, y);
      }
      g.stroke();
    }
    for (final var mark : s.marks()) {
      final double x = this.x((mark.from() + mark.to()) / 2, s, p), y = this.y(mark.y(), s, p);
      if (mark.from() != mark.to())
        g.strokeLine(this.x(mark.from(), s, p), y, this.x(mark.to(), s, p), y);
      if (s.positionsOnly()) g.strokeLine(x, y - 13, x, y + 13);
      if (second) {
        g.strokePolygon(new double[] {x, x + 5, x, x - 5}, new double[] {y - 5, y, y + 5, y}, 4);
      } else g.fillOval(x - 3.5, y - 3.5, 7, 7);
    }
    for (final var line : s.lines()) {
      final double x = this.x(line.ppm(), s, p), y = this.y(line.y(), s, p);
      g.strokeLine(x, y - 10, x, y + 10);
    }
    g.restore();
  }

  private String nearest(final double mouseX, final double mouseY) {
    if (this.first == null) return "";
    double best = 18;
    String result = "";
    for (int i = 0; i < 2; i++) {
      final var s = i == 0 ? this.first : this.second;
      final var p = this.panel(this.overlay ? 0 : i);
      for (final var mark : s.marks()) {
        final double d =
            Math.hypot(
                this.x((mark.from() + mark.to()) / 2, s, p) - mouseX,
                this.y(mark.y(), s, p) - mouseY);
        if (d < best) {
          best = d;
          result = (i == 0 ? "A · " : "B · ") + mark.label();
        }
      }
      for (final var line : s.lines()) {
        final double d =
            Math.hypot(this.x(line.ppm(), s, p) - mouseX, this.y(line.y(), s, p) - mouseY);
        if (d < best) {
          best = d;
          result = (i == 0 ? "A · " : "B · ") + line.label();
        }
      }
      for (final var point : s.points()) {
        final double d =
            Math.hypot(this.x(point.x(), s, p) - mouseX, this.y(point.y(), s, p) - mouseY);
        if (d < best) {
          best = d;
          result =
              String.format(
                  Locale.ROOT,
                  "%s · x %.6g · y %.6g%s",
                  i == 0 ? "A" : "B",
                  point.x(),
                  point.y(),
                  this.normalized ? " · исходный отклик до нормировки" : "");
        }
      }
    }
    return result;
  }
}

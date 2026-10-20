/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.scene.input.MouseButton;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAnalyticalEvidence.*;

/** Selectable correlation/trace view with mouse zoom and pan; original numerical data remains unchanged. */
final class LaboratoryEvidencePlot extends Region {

  private record Point(double x, double y, int row, String label) {}

  private final LaboratoryNmr2DPane dense = new LaboratoryNmr2DPane(true);
  private boolean denseVisible;
  private double beforeDenseMin, beforeDensePref, beforeDenseMax;
  private final Canvas canvas = new Canvas();
  private final List<Point> points = new ArrayList<>();
  private final Tooltip hint = new Tooltip();
  private IntConsumer pick = i -> {};
  private boolean line, reverseX, reverseY;
  private int selected = -1;
  private double zoom = 1, panX, panY, startX, startY, minX, maxX, minY, maxY;
  private String xLabel = "", yLabel = "";
  private List<ChromPoint> trace = List.of();
  private BiConsumer<BigDecimal, BigDecimal> rangePick = (from, to) -> {};
  private boolean rangeMode, rangeDragging;
  private BigDecimal rangeFrom, rangeTo, rangeAnchor, oldFrom, oldTo;

  LaboratoryEvidencePlot() {
    this.getChildren().addAll(this.canvas, this.dense);
    this.dense.setVisible(false);
    this.dense.setManaged(false);
    this.dense.onSelect(row -> this.pick.accept(row));
    this.setMinSize(240, 180);
    this.setPrefSize(650, 320);
    this.setMaxHeight(480);
    Tooltip.install(this.canvas, this.hint);
    this.sceneProperty()
        .addListener(
            (o, before, after) -> {
              if (before != null) before.getRoot().getStyleClass().removeListener(this.theme);
              if (after != null) after.getRoot().getStyleClass().addListener(this.theme);
              this.paint();
            });
    this.canvas.setOnScroll(
        e -> {
          this.zoom = Math.max(1, Math.min(15, this.zoom * (e.getDeltaY() > 0 ? 1.2 : 1 / 1.2)));
          this.paint();
          e.consume();
        });
    this.canvas.setFocusTraversable(true);
    this.canvas.setOnMousePressed(
        e -> {
          this.startX = e.getX();
          this.startY = e.getY();
          if (this.rangeMode
              && e.getButton() == MouseButton.PRIMARY
              && !this.trace.isEmpty()
              && this.inside(e.getX(), e.getY())) {
            this.canvas.requestFocus();
            this.rangeDragging = true;
            this.oldFrom = this.rangeFrom;
            this.oldTo = this.rangeTo;
            this.rangeAnchor = this.nearestTime(e.getX());
            this.rangeFrom = this.rangeAnchor;
            this.rangeTo = this.rangeAnchor;
            this.paint();
            e.consume();
          }
        });
    this.canvas.setOnMouseDragged(
        e -> {
          if (this.rangeDragging) {
            this.dragRange(e.getX());
            e.consume();
            return;
          }
          if (this.rangeMode) return;
          this.panX += e.getX() - this.startX;
          this.panY += e.getY() - this.startY;
          this.startX = e.getX();
          this.startY = e.getY();
          this.paint();
        });
    this.canvas.setOnMouseReleased(
        e -> {
          if (!this.rangeDragging) return;
          this.dragRange(e.getX());
          this.rangeDragging = false;
          if (this.rangeFrom.compareTo(this.rangeTo) < 0)
            this.rangePick.accept(this.rangeFrom, this.rangeTo);
          else {
            this.rangeFrom = this.oldFrom;
            this.rangeTo = this.oldTo;
            this.paint();
          }
          e.consume();
        });
    this.canvas.setOnKeyPressed(
        e -> {
          if (e.getCode() == KeyCode.ESCAPE && this.rangeDragging) {
            this.rangeDragging = false;
            this.rangeFrom = this.oldFrom;
            this.rangeTo = this.oldTo;
            this.paint();
            e.consume();
          }
        });
    this.canvas.setOnMouseClicked(
        e -> {
          if (this.rangeMode || !e.isStillSincePress()) return;
          this.points.stream()
              .filter(p -> p.row >= 0)
              .min(
                  Comparator.comparingDouble(
                      p -> Math.hypot(this.x(p.x) - e.getX(), this.y(p.y) - e.getY())))
              .filter(p -> Math.hypot(this.x(p.x) - e.getX(), this.y(p.y) - e.getY()) < 18)
              .ifPresent(p -> this.pick.accept(p.row));
        });
    this.canvas.setOnMouseMoved(
        e -> {
          this.hint.setText("");
          this.points.stream()
              .filter(p -> p.row >= 0)
              .min(
                  Comparator.comparingDouble(
                      p -> Math.hypot(this.x(p.x) - e.getX(), this.y(p.y) - e.getY())))
              .filter(p -> Math.hypot(this.x(p.x) - e.getX(), this.y(p.y) - e.getY()) < 20)
              .ifPresent(p -> this.hint.setText(p.label));
        });
  }

  private final javafx.collections.ListChangeListener<String> theme = c -> this.paint();

  private void leaveDense() {
    this.dense.clear();
    if (this.denseVisible) {
      this.setMinHeight(this.beforeDenseMin);
      this.setPrefHeight(this.beforeDensePref);
      this.setMaxHeight(this.beforeDenseMax);
    }
    this.denseVisible = false;
    this.dense.setVisible(false);
    this.canvas.setVisible(true);
  }

  void clear() {
    this.leaveDense();
    this.trace = List.of();
    this.rangeFrom = null;
    this.rangeTo = null;
    this.rangeDragging = false;
    this.line = false;
    this.xLabel = "";
    this.yLabel = "";
    this.points.clear();
    this.selected = -1;
    this.minX = 0;
    this.maxX = 1;
    this.minY = 0;
    this.maxY = 1;
    this.paint();
  }

  void select(final int row) {
    this.selected = row;
    if (this.denseVisible) this.dense.select(row);
    this.paint();
  }

  void onPick(final IntConsumer value) {
    this.pick = value;
  }

  void fit() {
    if (this.denseVisible) {
      this.dense.fit();
      return;
    }
    this.zoom = 1;
    this.panX = 0;
    this.panY = 0;
    this.paint();
  }

  void onRange(final BiConsumer<BigDecimal, BigDecimal> value) {
    this.rangePick = Objects.requireNonNull(value);
  }

  void rangeMode(final boolean value) {
    this.rangeMode = value;
    this.rangeDragging = false;
  }

  void range(final BigDecimal from, final BigDecimal to) {
    final boolean valid =
        from != null
            && to != null
            && from.compareTo(to) < 0
            && !this.trace.isEmpty()
            && from.compareTo(this.trace.getFirst().timeMin()) >= 0
            && to.compareTo(this.trace.getLast().timeMin()) <= 0;
    this.rangeFrom = valid ? from : null;
    this.rangeTo = valid ? to : null;
    this.paint();
  }

  void showTrace(final List<ChromPoint> value) {
    this.leaveDense();
    this.points.clear();
    this.selected = -1;
    this.rangeFrom = null;
    this.rangeTo = null;
    this.rangeDragging = false;
    this.reverseX = false;
    this.reverseY = false;
    this.trace = List.copyOf(value);
    this.line = !this.trace.isEmpty();
    this.xLabel = "Время, мин";
    this.yLabel = "Отклик · единицы файла";
    this.tracePoints();
    this.bounds(false);
  }

  private boolean inside(final double x, final double y) {
    return x >= 55 && x <= this.getWidth() - 25 && y >= 25 && y <= this.getHeight() - 38;
  }

  private BigDecimal nearestTime(final double pixel) {
    final double time = this.unX(pixel);
    int low = 0, high = this.trace.size() - 1;
    while (low < high) {
      final int mid = (low + high) / 2;
      if (this.trace.get(mid).timeMin().doubleValue() < time) low = mid + 1;
      else high = mid;
    }
    if (low > 0
        && Math.abs(this.trace.get(low - 1).timeMin().doubleValue() - time)
            <= Math.abs(this.trace.get(low).timeMin().doubleValue() - time)) low--;
    return this.trace.get(low).timeMin();
  }

  private void dragRange(final double pixel) {
    final var time = this.nearestTime(pixel);
    this.rangeFrom = this.rangeAnchor.min(time);
    this.rangeTo = this.rangeAnchor.max(time);
    this.paint();
  }

  private void tracePoints() {
    final int stride = Math.max(1, (this.trace.size() + 2999) / 3000);
    for (int start = 0; start < this.trace.size(); start += stride) {
      int low = start, high = start;
      for (int j = start + 1; j < Math.min(start + stride, this.trace.size()); j++) {
        if (this.trace.get(j).response().compareTo(this.trace.get(low).response()) < 0) low = j;
        if (this.trace.get(j).response().compareTo(this.trace.get(high).response()) > 0) high = j;
      }
      for (final int j :
          low == high ? new int[] {low} : new int[] {Math.min(low, high), Math.max(low, high)}) {
        final var p = this.trace.get(j);
        this.points.add(new Point(p.timeMin().doubleValue(), p.response().doubleValue(), -1, ""));
      }
    }
  }

  void show(final Data data) {
    if (data instanceof Correlations c && c.spectrum() != null) {
      if (!this.denseVisible) {
        this.beforeDenseMin = this.getMinHeight();
        this.beforeDensePref = this.getPrefHeight();
        this.beforeDenseMax = this.getMaxHeight();
      }
      this.denseVisible = true;
      this.setMinHeight(390);
      this.setPrefHeight(470);
      this.setMaxHeight(650);
      this.canvas.setVisible(false);
      this.dense.setVisible(true);
      this.dense.show(c);
      this.requestLayout();
      return;
    }
    this.leaveDense();
    this.points.clear();
    this.trace = List.of();
    this.rangeFrom = null;
    this.rangeTo = null;
    this.rangeDragging = false;
    this.line = false;
    this.reverseX = false;
    this.reverseY = false;
    if (data instanceof Correlations c) {
      this.reverseX = true;
      this.reverseY = true;
      this.xLabel = c.xNucleus() + " · ppm";
      this.yLabel = c.yNucleus() + " · ppm";
      int i = 0;
      for (final var p : c.correlations()) {
        this.points.add(
            new Point(
                p.xPpm().doubleValue(),
                p.yPpm().doubleValue(),
                i++,
                p.xPpm() + " / " + p.yPpm() + " ppm"));
      }
    } else if (data instanceof Multiplets m) {
      this.reverseX = true;
      this.xLabel = m.nucleus() + " · ppm";
      this.yLabel = "Положения линий · без моделирования формы";
      int i = 0;
      for (final var p : m.multiplets()) {
        final String label =
            "δ " + p.centerPpm() + " · " + p.label() + " · J " + p.couplingsHz() + " Гц";
        this.points.add(new Point(p.centerPpm().doubleValue(), 1, i, label));
        for (final var position : p.linesPpm()) {
          this.points.add(
              new Point(position.doubleValue(), 1, i, label + " · линия " + position + " ppm"));
        }
        i++;
      }
    } else if (data instanceof Chromatogram c) {
      this.trace = c.trace();
      this.line = !c.trace().isEmpty();
      this.xLabel = "Время, мин";
      this.yLabel = c.trace().isEmpty() ? "Площадь пика" : "Отклик детектора";
      this.tracePoints();
      int i = 0;
      for (final var p : c.peaks()) {
        double y = nearest(c.trace(), p.retentionMin().doubleValue(), p.area().doubleValue());
        if (Double.isFinite(y))
          this.points.add(
              new Point(
                  p.retentionMin().doubleValue(),
                  y,
                  i,
                  p.label() + " · " + p.retentionMin() + " мин"));
        i++;
      }
    }
    this.bounds(data instanceof Correlations);
  }

  private void bounds(final boolean correlation) {
    this.minX = this.points.stream().mapToDouble(Point::x).min().orElse(0);
    this.maxX = this.points.stream().mapToDouble(Point::x).max().orElse(1);
    this.minY = this.points.stream().mapToDouble(Point::y).min().orElse(0);
    if (!correlation) this.minY = Math.min(0, this.minY);
    this.maxY = this.points.stream().mapToDouble(Point::y).max().orElse(1);
    if (this.minX == this.maxX) {
      this.minX -= .5;
      this.maxX += .5;
    }
    if (this.minY == this.maxY) {
      this.minY -= .5;
      this.maxY += .5;
    }
    final double padx = (this.maxX - this.minX) * .04, pady = (this.maxY - this.minY) * .04;
    this.minX -= padx;
    this.maxX += padx;
    this.minY -= pady;
    this.maxY += pady;
    this.fit();
  }

  @Override
  protected void layoutChildren() {
    this.dense.resizeRelocate(0, 0, this.getWidth(), this.getHeight());
    this.canvas.setWidth(this.getWidth());
    this.canvas.setHeight(this.getHeight());
    this.paint();
  }

  private double x(final double value) {
    final double t = (value - this.minX) / (this.maxX - this.minX);
    return 55 + (this.reverseX ? 1 - t : t) * (this.getWidth() - 80) * this.zoom + this.panX;
  }

  private double y(final double value) {
    final double t = (value - this.minY) / (this.maxY - this.minY);
    return this.getHeight()
        - 38
        - (this.reverseY ? 1 - t : t) * (this.getHeight() - 65) * this.zoom
        + this.panY;
  }

  private double unX(final double pixel) {
    final double t = (pixel - 55 - this.panX) / ((this.getWidth() - 80) * this.zoom);
    return this.minX + (this.reverseX ? 1 - t : t) * (this.maxX - this.minX);
  }

  private double unY(final double pixel) {
    final double t =
        (this.getHeight() - 38 + this.panY - pixel) / ((this.getHeight() - 65) * this.zoom);
    return this.minY + (this.reverseY ? 1 - t : t) * (this.maxY - this.minY);
  }

  private static double nearest(
      final List<ChromPoint> trace, final double time, final double fallback) {
    if (trace.isEmpty()) return fallback;
    if (time < trace.getFirst().timeMin().doubleValue()
        || time > trace.getLast().timeMin().doubleValue()) return Double.NaN;
    int low = 0, high = trace.size() - 1;
    while (low < high) {
      final int mid = (low + high) / 2;
      if (trace.get(mid).timeMin().doubleValue() < time) low = mid + 1;
      else high = mid;
    }
    final var b = trace.get(low);
    if (low == 0 || b.timeMin().doubleValue() == time) return b.response().doubleValue();
    final var a = trace.get(low - 1);
    return a.response().doubleValue()
        + (b.response().doubleValue() - a.response().doubleValue())
            * (time - a.timeMin().doubleValue())
            / (b.timeMin().doubleValue() - a.timeMin().doubleValue());
  }

  void dispose() {
    this.dense.dispose();
    if (this.getScene() != null)
      this.getScene().getRoot().getStyleClass().removeListener(this.theme);
    Tooltip.uninstall(this.canvas, this.hint);
  }

  private void paint() {
    if (this.denseVisible) return;
    final var g = this.canvas.getGraphicsContext2D();
    final double width = this.getWidth(), height = this.getHeight();
    final boolean dark = LaboratorySpectralComparisonPlot.dark(this);
    g.setFill(Color.web(dark ? "#162131" : "#ffffff"));
    g.fillRect(0, 0, width, height);
    if (width < 100 || height < 100) return;
    g.setStroke(Color.web(dark ? "#344256" : "#e2e8f0"));
    g.setFill(Color.web(dark ? "#dbe5f2" : "#475569"));
    for (int i = 0; i <= 5; i++) {
      final double px = 55 + i * (width - 80) / 5, py = height - 38 - i * (height - 65) / 5;
      g.strokeLine(px, 25, px, height - 38);
      g.strokeLine(55, py, width - 25, py);
      g.fillText(
          LaboratoryAxisTickText.format(this.unX(px), (this.maxX - this.minX) / (5 * this.zoom)),
          px - 15,
          height - 22);
      g.fillText(
          LaboratoryAxisTickText.format(this.unY(py), (this.maxY - this.minY) / (5 * this.zoom)),
          3,
          py + 4);
    }
    g.fillText(this.xLabel, width / 2 - 45, height - 5);
    g.fillText(this.yLabel, 58, 15);
    g.save();
    g.beginPath();
    g.rect(54, 24, width - 78, height - 60);
    g.clip();
    if (this.rangeFrom != null && this.rangeTo != null) {
      final double from = this.x(this.rangeFrom.doubleValue()),
          to = this.x(this.rangeTo.doubleValue());
      g.setFill(Color.web(dark ? "#6da8ff" : "#2563eb", .16));
      g.fillRect(from, 25, to - from, height - 63);
      g.setStroke(Color.web(dark ? "#93c5fd" : "#1d4ed8"));
      g.strokeLine(from, 25, from, height - 38);
      g.strokeLine(to, 25, to, height - 38);
    }
    g.setStroke(Color.web(dark ? "#6da8ff" : "#2563eb"));
    g.setLineWidth(1.4);
    if (this.line) {
      g.beginPath();
      boolean first = true;
      for (final var p : this.points) {
        if (p.row >= 0) continue;
        if (first) {
          g.moveTo(this.x(p.x), this.y(p.y));
          first = false;
        } else {
          g.lineTo(this.x(p.x), this.y(p.y));
        }
      }
      g.stroke();
    }
    for (final var p : this.points) {
      if (p.row >= 0) {
        g.setFill(
            Color.web(
                p.row == this.selected
                    ? (dark ? "#ffb870" : "#c2410c")
                    : (dark ? "#5eead4" : "#0f766e")));
        g.fillOval(this.x(p.x) - 4, this.y(p.y) - 4, 8, 8);
        if (p.row == this.selected) {
          g.setStroke(Color.web(dark ? "#ffb870" : "#c2410c"));
          g.strokeOval(this.x(p.x) - 7, this.y(p.y) - 7, 14, 14);
        }
      }
    }
    g.restore();
  }
}

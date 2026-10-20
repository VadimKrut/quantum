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
import java.util.function.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.NmrProcessing.Spectrum;

/** Pixel-envelope drawing retains narrow peaks while zoom and integration operate on full data. */
final class LaboratorySpectrumPlot extends Region {
  private final Canvas canvas = new Canvas();
  private Spectrum data;
  private double low,
      high,
      scale = 1,
      start = Double.NaN,
      current = Double.NaN,
      cursor = Double.NaN;
  private boolean integration;
  private BiConsumer<Double, Double> range = (a, b) -> {};
  private DoubleConsumer point = p -> {};

  LaboratorySpectrumPlot() {
    this.setId("chemistry-spectrum-plot");
    this.getChildren().add(this.canvas);
    this.setMinSize(0, 0);
    this.setPrefHeight(340);
    this.widthProperty().addListener((o, a, b) -> this.paint());
    this.heightProperty().addListener((o, a, b) -> this.paint());
    LaboratoryPreviewTheme.track(this.canvas, this::paint);
    this.canvas.setOnScroll(
        e -> {
          if (this.data == null) {
            return;
          }
          if (e.isShiftDown()) {
            this.scale = Math.max(.02, Math.min(1000, this.scale * Math.exp(e.getDeltaY() / 300)));
          } else {
            final double value = this.ppm(e.getX()), factor = Math.exp(-e.getDeltaY() / 450);
            this.window(value + (this.low - value) * factor, value + (this.high - value) * factor);
          }
          this.paint();
          e.consume();
        });
    this.canvas.setOnMousePressed(
        e -> {
          if (this.data != null && e.isPrimaryButtonDown()) {
            this.start = this.current = e.getX();
          }
        });
    this.canvas.setOnMouseDragged(
        e -> {
          this.current = e.getX();
          this.paint();
        });
    this.canvas.setOnMouseReleased(
        e -> {
          if (this.data == null || Double.isNaN(this.start)) {
            return;
          }
          final double a = this.ppm(this.start), b = this.ppm(e.getX());
          if (Math.abs(e.getX() - this.start) > 5) {
            if (this.integration) {
              this.range.accept(Math.min(a, b), Math.max(a, b));
            } else {
              this.window(Math.min(a, b), Math.max(a, b));
            }
          } else {
            final int peak =
                this.data.peak(b, (this.high - this.low) * 8 / Math.max(1, this.getWidth() - 78));
            this.cursor = peak >= 0 ? this.data.ppm(peak) : b;
            this.point.accept(this.cursor);
          }
          this.start = this.current = Double.NaN;
          this.paint();
        });
    this.canvas.setOnMouseClicked(
        e -> {
          if (e.getClickCount() == 2) {
            this.fit();
          }
        });
  }

  void callbacks(final BiConsumer<Double, Double> range, final DoubleConsumer point) {
    this.range = range;
    this.point = point;
  }

  void integration(final boolean value) {
    this.integration = value;
  }

  void data(final Spectrum value) {
    this.data = value;
    this.fit();
  }

  Spectrum data() {
    return this.data;
  }

  double low() {
    return this.low;
  }

  double high() {
    return this.high;
  }

  void fit() {
    if (this.data != null) {
      this.low = this.data.lastPpm();
      this.high = this.data.firstPpm();
    }
    this.scale = 1;
    this.cursor = Double.NaN;
    this.paint();
  }

  void window(final double low, final double high) {
    if (this.data == null) {
      return;
    }
    final double span =
        Math.min(
            this.data.firstPpm() - this.data.lastPpm(),
            Math.max(Math.abs(this.data.stepPpm()) * 8, high - low));
    this.low = Math.max(this.data.lastPpm(), Math.min(this.data.firstPpm() - span, low));
    this.high = this.low + span;
    this.paint();
  }

  private double ppm(final double x) {
    return Math.max(
        this.low,
        Math.min(
            this.high,
            this.high - (x - 52) / Math.max(1, this.getWidth() - 78) * (this.high - this.low)));
  }

  private double x(final double ppm) {
    return 52 + (this.high - ppm) / (this.high - this.low) * (this.getWidth() - 78);
  }

  private void paint() {
    final double w = Math.max(1, this.getWidth()), h = Math.max(1, this.getHeight());
    this.canvas.setWidth(w);
    this.canvas.setHeight(h);
    final var g = this.canvas.getGraphicsContext2D();
    final var palette = LaboratoryPreviewTheme.palette(this.canvas);
    LaboratoryPreviewTheme.clear(g);
    g.setFill(palette.dark() ? palette.ink() : Color.web("#25405e"));
    g.setFont(javafx.scene.text.Font.font(12));
    if (this.data == null) {
      g.fillText("Загрузите спектр или FID для просмотра.", 18, 30);
      return;
    }
    if (w < 100 || h < 80) {
      return;
    }
    final double bottom = h - 38, left = 52, right = w - 26;
    g.setStroke(palette.dark() ? palette.selectedBackground() : Color.web("#c8d6e6"));
    g.strokeLine(left, bottom, right, bottom);
    for (int i = 0; i <= 6; i++) {
      final double px = left + (right - left) * i / 6,
          ppm = this.high - (this.high - this.low) * i / 6;
      g.strokeLine(px, 24, px, bottom + 4);
      g.fillText(
          String.format(Locale.ROOT, this.high - this.low < .1 ? "%.4f" : "%.2f", ppm),
          px - 14,
          bottom + 19);
    }
    g.fillText("δ / ppm", right - 38, h - 3);
    final int
        from =
            Math.max(0, (int) Math.floor((this.high - this.data.firstPpm()) / this.data.stepPpm())),
        to =
            Math.min(
                this.data.real().size() - 1,
                (int) Math.ceil((this.low - this.data.firstPpm()) / this.data.stepPpm()));
    double max = 1e-200, min = 0;
    for (int i = from; i <= to; i++) {
      max = Math.max(max, this.data.real().get(i));
      min = Math.min(min, this.data.real().get(i));
    }
    final double amplitude = max - min,
        pixels = (bottom - 32) / amplitude * this.scale,
        zero = bottom - Math.max(0, Math.min(1, -min / (max - min))) * (bottom - 32);
    g.save();
    g.beginPath();
    g.rect(left, 25, right - left, bottom - 24);
    g.closePath();
    g.clip();
    g.setStroke(palette.dark() ? palette.selection() : Color.web("#155cc1"));
    g.setLineWidth(1.15);
    int previous = from;
    double lastX = this.x(this.data.ppm(from)), lastY = zero - this.data.real().get(from) * pixels;
    for (int px = (int) left; px <= right; px++) {
      final double ppm = this.ppm(px + 1);
      final int end =
          Math.max(
              previous,
              Math.min(to, (int) Math.ceil((ppm - this.data.firstPpm()) / this.data.stepPpm())));
      double a = this.data.real().get(previous), b = a;
      for (int i = previous; i <= end; i++) {
        a = Math.min(a, this.data.real().get(i));
        b = Math.max(b, this.data.real().get(i));
      }
      final double y = zero - this.data.real().get(end) * pixels;
      g.strokeLine(lastX, lastY, px, y);
      g.strokeLine(px, zero - a * pixels, px, zero - b * pixels);
      lastX = px;
      lastY = y;
      previous = end;
    }
    g.restore();
    if (!Double.isNaN(this.cursor)) {
      g.setStroke(palette.dark() ? palette.stereo(true) : Color.web("#a64a10"));
      g.strokeLine(this.x(this.cursor), 25, this.x(this.cursor), bottom);
      g.fillText(String.format(Locale.ROOT, "δ %.5f", this.cursor), left, 17);
    }
    if (!Double.isNaN(this.start) && !Double.isNaN(this.current)) {
      final Color selection = palette.dark() ? palette.selection() : Color.web("#4c8bdf");
      g.setFill(new Color(selection.getRed(), selection.getGreen(), selection.getBlue(), .16));
      g.fillRect(
          Math.min(this.start, this.current), 25, Math.abs(this.start - this.current), bottom - 25);
    }
  }
}

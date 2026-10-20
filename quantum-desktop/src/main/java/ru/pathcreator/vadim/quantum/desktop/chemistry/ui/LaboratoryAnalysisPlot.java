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
import java.util.function.Consumer;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.control.Tooltip;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAnalysis.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;

/** Display of entered peaks/spot positions, explicitly not a reconstructed instrument spectrum. */
final class LaboratoryAnalysisPlot extends Region {
  private final Canvas canvas = new Canvas();
  private Data data;
  private final LaboratorySpectrumPlot spectrum = new LaboratorySpectrumPlot();
  private ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy.NmrTrace lastTrace;
  private String massHint = "Округлённый вид · исходные числа в описании";

  LaboratoryAnalysisPlot() {
    this.getChildren().addAll(this.canvas, this.spectrum);
    this.spectrum.setVisible(false);
    this.spectrum.setManaged(false);
    this.setMinSize(0, 180);
    this.setPrefHeight(220);
    this.widthProperty().addListener((o, a, b) -> this.paint());
    this.heightProperty().addListener((o, a, b) -> this.paint());
    LaboratoryPreviewTheme.track(this.canvas, this::paint);
  }

  void data(final Data data) {
    this.data = data;
    this.paint();
  }

  void massHint(final String text) {
    this.massHint = text;
    this.paint();
  }

  private void paint() {
    final double w = Math.max(1, this.getWidth()), h = Math.max(1, this.getHeight());
    if (this.data instanceof Nmr n && n.trace() != null) {
      this.canvas.setVisible(false);
      this.spectrum.setVisible(true);
      this.spectrum.resizeRelocate(0, 0, w, h);
      if (this.lastTrace != n.trace()) {
        this.spectrum.data(
            ru.pathcreator.vadim.quantum.chemistry.drawing.application.NmrProcessing.process(
                n.trace()));
        this.lastTrace = n.trace();
      }
      return;
    }
    this.canvas.setVisible(true);
    this.spectrum.setVisible(false);
    this.lastTrace = null;
    this.canvas.setWidth(w);
    this.canvas.setHeight(h);
    final var g = this.canvas.getGraphicsContext2D();
    final var palette = LaboratoryPreviewTheme.palette(this.canvas);
    LaboratoryPreviewTheme.clear(g);
    g.setStroke(palette.dark() ? palette.bond() : Color.web("#8192aa"));
    g.setFill(palette.dark() ? palette.ink() : Color.web("#23364e"));
    if (this.data == null) {
      g.fillText("Введите данные и нажмите «Проверить и показать».", 20, 35);
      return;
    }
    if (this.data instanceof Nmr n) {
      g.fillText("Схема введённых сигналов · не приборный спектр", 18, 24);
      if (n.signals().isEmpty()) {
        g.fillText("Сигналы не внесены", 20, 60);
        return;
      }
      double lo = n.signals().stream().mapToDouble(s -> s.from().doubleValue()).min().orElse(0),
          hi = n.signals().stream().mapToDouble(s -> s.to().doubleValue()).max().orElse(10);
      final double margin = Math.max(.3, (hi - lo) * .12);
      lo -= margin;
      hi += margin;
      final double bottom = h - 38, left = 44, right = w - 24;
      g.strokeLine(left, bottom, right, bottom);
      for (int i = 0; i <= 5; i++) {
        final double x = left + (right - left) * i / 5, v = hi - (hi - lo) * i / 5;
        g.strokeLine(x, bottom, x, bottom + 5);
        g.fillText(String.format(Locale.ROOT, "%.2f", v), x - 12, bottom + 20);
      }
      g.fillText("δ / ppm", Math.max(left, w - 80), h - 4);
      final double peakHeight = Math.max(20, h - 90);
      final double max =
          n.signals().stream()
              .filter(s -> s.integral() != null)
              .mapToDouble(s -> s.integral().doubleValue())
              .max()
              .orElse(1);
      for (final var s : n.signals()) {
        final double x = left + (hi - s.from().doubleValue()) / (hi - lo) * (right - left),
            to = left + (hi - s.to().doubleValue()) / (hi - lo) * (right - left);
        final double height =
            s.integral() == null
                ? peakHeight * .7
                : peakHeight * Math.max(.08, s.integral().doubleValue() / Math.max(.001, max));
        g.setStroke(palette.dark() ? palette.selection() : Color.web("#276bd9"));
        g.setFill(palette.dark() ? palette.selectedBackground() : Color.web("#cae0ff"));
        if (Math.abs(x - to) > 2) {
          g.fillRect(Math.min(x, to), bottom - height, Math.abs(x - to), height);
          g.strokeRect(Math.min(x, to), bottom - height, Math.abs(x - to), height);
        } else {
          g.strokeLine(x, bottom, x, bottom - height);
        }
      }
    } else if (this.data instanceof Tlc t) {
      g.fillText("Положение пятен по введённым Rf · не фотография", 18, 24);
      final double top = 48, bottom = h - 35;
      g.setFill(palette.dark() ? palette.selectedBackground() : Color.web("#f4f7fa"));
      g.fillRect(45, top, w - 70, bottom - top);
      g.setStroke(palette.dark() ? palette.bond() : Color.web("#8192aa"));
      g.setLineDashes(4, 3);
      g.strokeLine(45, top, w - 25, top);
      g.strokeLine(45, bottom, w - 25, bottom);
      g.setLineDashes();
      g.setFill(palette.dark() ? palette.ink() : Color.web("#23364e"));
      g.fillText("1", 20, top + 4);
      g.fillText("0", 20, bottom + 4);
      for (int i = 0; i < t.spots().size(); i++) {
        final var spot = t.spots().get(i);
        final var rf = t.rf(spot);
        final double x = 55 + (w - 95) * (i + .5) / Math.max(1, t.spots().size());
        if (rf != null) {
          final double y = bottom - rf.doubleValue() * (bottom - top);
          g.setFill(palette.dark() ? palette.selection() : Color.web("#3575cf"));
          g.fillOval(x - 6, y - 6, 12, 12);
        }
        g.setFill(palette.dark() ? palette.ink() : Color.web("#23364e"));
        g.fillText(Integer.toString(i + 1), x - 4, bottom + 19);
      }
    } else if (this.data instanceof Mass m) {
      g.fillText("Сравнение m/z · не масс-спектр", 18, 24);
      g.setFill(palette.dark() ? palette.selection() : Color.web("#276bd9"));
      g.setFont(javafx.scene.text.Font.font(25));
      g.fillText(
          m.foundMz() == null ? "Найденное m/z не указано" : mass(m.foundMz()),
          22,
          Math.min(82, h - 70),
          Math.max(1, w - 44));
      g.setFont(javafx.scene.text.Font.font(13));
      g.setFill(palette.dark() ? palette.ink() : Color.web("#23364e"));
      g.fillText(
          "Расчёт: " + (m.calculatedMz() == null ? "—" : mass(m.calculatedMz())),
          22,
          Math.min(82, h - 70) + 25,
          Math.max(1, w - 44));
      g.fillText(
          "Δ = "
              + (m.ppmError() == null
                  ? "—"
                  : m.ppmError().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
                      + " ppm"),
          22,
          Math.min(82, h - 70) + 48);
      g.setFont(javafx.scene.text.Font.font(11));
      g.fillText(
          this.massHint, 22, Math.min(h - 6, Math.min(82, h - 70) + 74), Math.max(1, w - 44));
    }
  }

  private static String mass(final java.math.BigDecimal value) {
    if (value.abs().compareTo(new java.math.BigDecimal("0.00001")) < 0
        || value.precision() - value.scale() > 8) {
      return value
          .round(new java.math.MathContext(9, java.math.RoundingMode.HALF_UP))
          .toEngineeringString();
    }
    return value.setScale(5, java.math.RoundingMode.HALF_UP).toPlainString();
  }

  static final class Structure extends Region {
    private final Canvas canvas = new Canvas();
    private final Tooltip tip = new Tooltip();
    private MoleculeDrawing drawing;
    private Set<AtomId> selected = Set.of();
    private Nucleus nucleus = Nucleus.H1;
    private Consumer<Set<AtomId>> changed = ids -> {};
    private double scale = 1, tx, ty;

    Structure() {
      this.getChildren().add(this.canvas);
      this.setMinSize(0, 230);
      this.setPrefHeight(300);
      Tooltip.install(this.canvas, this.tip);
      this.widthProperty().addListener((o, a, b) -> this.paint());
      this.heightProperty().addListener((o, a, b) -> this.paint());
      this.canvas.setOnMouseClicked(
          e -> {
            if (this.changed == null) return;
            final var atoms = this.nearest(e.getX(), e.getY());
            if (atoms.isEmpty()) return;
            final Set<AtomId> next = new LinkedHashSet<>(this.selected);
            if (next.containsAll(atoms)) next.removeAll(atoms);
            else next.addAll(atoms);
            this.selected = Set.copyOf(next);
            this.paint();
            this.changed.accept(this.selected);
          });
      this.canvas.setOnMouseMoved(
          e -> {
            final var atoms = this.nearest(e.getX(), e.getY());
            this.tip.setText(
                atoms.isEmpty()
                    ? "Выберите атом подходящего ядра"
                    : String.join(", ", atoms.stream().map(AtomId::value).toList()));
          });
    }

    void show(
        final MoleculeDrawing drawing,
        final Nucleus nucleus,
        final Set<AtomId> atoms,
        final Consumer<Set<AtomId>> changed) {
      this.drawing = drawing;
      this.nucleus = nucleus;
      this.selected = Set.copyOf(atoms);
      this.changed = changed;
      this.paint();
    }

    private Set<AtomId> mapped(final AtomId id) {
      final var m = this.drawing.molecule();
      final Atom atom = m.atoms().stream().filter(a -> a.id().equals(id)).findFirst().orElseThrow();
      if (atom.element().atomicNumber() == this.nucleus.element()) {
        return atom.isotope() == null || atom.isotope().massNumber() == this.nucleus.isotope()
            ? Set.of(id)
            : Set.of();
      }
      if (this.nucleus != Nucleus.H1) return Set.of();
      final Set<AtomId> result = new LinkedHashSet<>();
      for (final Bond b : m.bonds()) {
        final AtomId neighbor =
            b.firstAtomId().equals(id)
                ? b.secondAtomId()
                : b.secondAtomId().equals(id) ? b.firstAtomId() : null;
        if (neighbor == null) continue;
        m.atoms().stream()
            .filter(
                a ->
                    a.id().equals(neighbor)
                        && a.element().atomicNumber() == 1
                        && (a.isotope() == null || a.isotope().massNumber() == 1))
            .forEach(a -> result.add(a.id()));
      }
      return result;
    }

    private Set<AtomId> nearest(final double x, final double y) {
      if (this.drawing == null) return Set.of();
      double distance = 20;
      AtomId nearest = null;
      for (final var a : LaboratoryCanvas.previewAtoms(this.drawing)) {
        final double d =
            Math.hypot(
                x - (a.point2D().x() * this.scale + this.tx),
                y - (a.point2D().y() * this.scale + this.ty));
        if (d < distance) {
          distance = d;
          nearest = a.atomId();
        }
      }
      return nearest == null ? Set.of() : this.mapped(nearest);
    }

    private void paint() {
      final double w = Math.max(1, this.getWidth()), h = Math.max(1, this.getHeight());
      this.canvas.setWidth(w);
      this.canvas.setHeight(h);
      final var g = this.canvas.getGraphicsContext2D();
      g.setFill(Color.WHITE);
      g.fillRect(0, 0, w, h);
      if (this.drawing == null) return;
      final var b = LaboratoryCanvas.previewBounds(this.drawing);
      this.scale =
          Math.max(
              .01,
              Math.min(
                  1.2,
                  Math.min(
                      (w - 40) / Math.max(1, b.getWidth()),
                      (h - 35) / Math.max(1, b.getHeight()))));
      this.tx = w / 2 - (b.getMinX() + b.getMaxX()) * this.scale / 2;
      this.ty = h / 2 - (b.getMinY() + b.getMaxY()) * this.scale / 2;
      g.save();
      g.translate(this.tx, this.ty);
      g.scale(this.scale, this.scale);
      g.setFill(Color.web("#b9ead2"));
      for (final var a : LaboratoryCanvas.previewAtoms(this.drawing)) {
        if (this.mapped(a.atomId()).stream().anyMatch(this.selected::contains)) {
          g.fillOval(a.point2D().x() - 13, a.point2D().y() - 13, 26, 26);
        }
      }
      LaboratoryCanvas.paintPreview(g, this.drawing);
      g.restore();
    }
  }
}

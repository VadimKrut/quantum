/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAnalysis;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAnalyticalEvidence.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEvidenceService;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratorySpectralComparison;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.NmrProcessing;

/** Read-only source trace with explicit manual line selection; never assigns atoms or derives J. */
final class LaboratoryMultipletTracePane extends VBox {
  private final Label sourceInfo = note(""), status = note(""), selectedInfo = note("");
  private final ToggleButton adding = new ToggleButton("Добавлять линии");
  private final TracePlot plot = new TracePlot();
  private final ExecutorService worker =
      Executors.newSingleThreadExecutor(
          r -> {
            final var t = new Thread(r, "quantum-multiplet-trace");
            t.setDaemon(true);
            return t;
          });
  private Future<?> task;
  private long generation;
  private boolean disposed, processing, editable = true;
  private LaboratoryAnalysis source;
  private Multiplets data;
  private String selectedId;
  private int lineCount;
  private NmrProcessing.Spectrum spectrum;
  private BiConsumer<String, BigDecimal> lineAction = (id, ppm) -> {};
  private Consumer<String> selectAction = id -> {};

  LaboratoryMultipletTracePane() {
    super(8);
    this.setId("multiplet-trace");
    final var fit = new Button("Весь спектр");
    fit.setId("multiplet-trace-fit");
    fit.setOnAction(e -> this.plot.fit());
    this.adding.setId("multiplet-trace-add");
    this.adding.setTooltip(
        new Tooltip(
            "Выберите строку мультиплета, затем щёлкните по трассе. Сохраняется ppm ближайшей исходной точки; максимум не ищется автоматически."));
    this.adding.selectedProperty().addListener(o -> this.updateStatus());
    this.getChildren()
        .addAll(
            this.sourceInfo,
            new FlowPane(10, 6, fit, this.adding),
            this.status,
            this.plot,
            this.selectedInfo);
    this.updateStatus();
  }

  private static Label note(final String value) {
    final var label = new Label(value);
    label.setWrapText(true);
    label.setMaxWidth(Double.MAX_VALUE);
    return label;
  }

  void onLine(final BiConsumer<String, BigDecimal> action) {
    this.lineAction = Objects.requireNonNull(action);
  }

  void onSelect(final Consumer<String> action) {
    this.selectAction = Objects.requireNonNull(action);
  }

  boolean busy() {
    return this.processing;
  }

  void editable(final boolean value) {
    this.editable = value;
    if (!value) {
      this.adding.setSelected(false);
    }
    this.adding.setVisible(value);
    this.adding.setManaged(value);
    this.updateStatus();
  }

  void select(final String id) {
    this.selectedId = id;
    this.updateStatus();
    this.plot.paint();
  }

  void show(
      final LaboratoryAnalysis source, final Multiplets data, final String selectedMultipletId) {
    if (this.disposed) {
      return;
    }
    Objects.requireNonNull(source);
    Objects.requireNonNull(data);
    if (!(source.data() instanceof LaboratoryAnalysis.Nmr nmr)
        || nmr.trace() == null
        || !source.id().equals(data.analysisId())
        || nmr.nucleus() != data.nucleus()
        || nmr.frequencyMHz() == null
        || nmr.frequencyMHz().compareTo(data.frequencyMHz()) != 0) {
      this.clear();
      throw new IllegalArgumentException(
          "Для мультиплетов выберите точную запись с трассой того же ядра и частоты.");
    }
    final boolean same =
        this.source != null
            && this.source.id().equals(source.id())
            && ((LaboratoryAnalysis.Nmr) this.source.data()).trace().equals(nmr.trace());
    this.source = source;
    this.data = data;
    this.selectedId = selectedMultipletId;
    this.lineCount = data.multiplets().stream().mapToInt(m -> m.linesPpm().size()).sum();
    this.sourceInfo.setText(
        "Трасса: "
            + source.title()
            + " · "
            + nmr.nucleus()
            + " · "
            + LaboratoryEvidenceService.format(nmr.frequencyMHz())
            + " МГц · "
            + source.origin()
            + " · "
            + DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(source.recordedAt()));
    this.sourceInfo.setTooltip(
        new Tooltip(
            "Точная запись: "
                + source.id()
                + "\nОбразец: "
                + (source.sampleId() == null ? "без отдельного образца" : source.sampleId())
                + "\nИсточник: "
                + source.source()
                + "\n"
                + source.note()));
    if (same && (this.spectrum != null || this.processing)) {
      this.updateStatus();
      this.plot.paint();
      return;
    }
    final long request = ++this.generation;
    if (this.task != null) {
      this.task.cancel(true);
    }
    this.spectrum = null;
    this.processing = true;
    this.plot.fit();
    this.updateStatus();
    this.task =
        this.worker.submit(
            () -> {
              try {
                final var processed = NmrProcessing.process(nmr.trace());
                Platform.runLater(
                    () -> {
                      if (this.disposed || request != this.generation) {
                        return;
                      }
                      this.spectrum = processed;
                      this.processing = false;
                      this.plot.fit();
                      this.updateStatus();
                    });
              } catch (final RuntimeException error) {
                Platform.runLater(
                    () -> {
                      if (this.disposed || request != this.generation) {
                        return;
                      }
                      this.processing = false;
                      this.spectrum = null;
                      this.updateStatus();
                      this.status.setText(
                          error.getMessage() == null
                              ? "Не удалось показать сохранённую трассу."
                              : error.getMessage());
                    });
              }
            });
  }

  private Multiplet selected() {
    return this.data == null
        ? null
        : this.data.multiplets().stream()
            .filter(p -> p.id().equals(this.selectedId))
            .findFirst()
            .orElse(null);
  }

  private static String compact(final List<BigDecimal> values) {
    final var shown = values.stream().limit(8).map(LaboratoryEvidenceService::format).toList();
    return shown + (values.size() > 8 ? " … (" + values.size() + ")" : "");
  }

  private void updateStatus() {
    final var selected = this.selected();
    if (selected == null && this.adding.isSelected()) {
      this.adding.setSelected(false);
    }
    this.adding.setDisable(
        !this.editable || this.processing || this.spectrum == null || selected == null);
    if (this.processing) {
      this.status.setText("Подготавливаю сохранённую трассу…");
    } else if (this.spectrum == null) {
      this.status.setText("Выберите сохранённую запись 1D ЯМР с числовой трассой.");
    } else {
      this.status.setText(
          (this.lineCount > 20000
                  ? "Линий больше 20 000: показаны центры всех мультиплетов и линии выбранного. "
                  : "")
              + (this.adding.isSelected() && selected != null
                  ? "Щёлкните по спектру: линия добавится к «"
                      + selected.label()
                      + "». J и назначения задаются отдельно."
                  : "Колесо — приблизить, перетаскивание — сдвинуть. Метка выбирает мультиплет; линии не определяются автоматически."));
    }
    if (selected == null) {
      this.selectedInfo.setTooltip(null);
      this.selectedInfo.setText(
          this.data == null || this.data.multiplets().isEmpty()
              ? "Добавьте мультиплет, чтобы отметить его линии на трассе."
              : "Выберите мультиплет в таблице или нажмите его метку на спектре.");
    } else {
      final long outside =
          this.spectrum == null
              ? 0
              : selected.linesPpm().stream()
                  .filter(
                      v ->
                          LaboratorySpectralComparison.sample(this.spectrum, v.doubleValue())
                              == null)
                  .count();
      this.selectedInfo.setTooltip(
          new Tooltip(
              "Линии δ: "
                  + selected.linesPpm()
                  + " ppm\nВведённые J: "
                  + selected.couplingsHz()
                  + " Гц\nРасстояния соседних линий: "
                  + LaboratoryEvidenceService.lineSpacings(selected, this.data.frequencyMHz())
                  + " Гц"));
      this.selectedInfo.setText(
          "δ "
              + LaboratoryEvidenceService.format(selected.centerPpm())
              + " · "
              + selected.label()
              + " · линии: "
              + selected.linesPpm().size()
              + "\nВведённые J: "
              + compact(selected.couplingsHz())
              + " Гц · расстояния соседних линий: "
              + compact(LaboratoryEvidenceService.lineSpacings(selected, this.data.frequencyMHz()))
              + " Гц"
              + (outside == 0
                  ? ""
                  : "\nВне диапазона исходной трассы: "
                      + outside
                      + ". Значения сохранены; на трассе не показаны."));
    }
  }

  void clear() {
    ++this.generation;
    if (this.task != null) {
      this.task.cancel(true);
    }
    this.processing = false;
    this.source = null;
    this.data = null;
    this.spectrum = null;
    this.selectedId = null;
    this.lineCount = 0;
    this.adding.setSelected(false);
    this.sourceInfo.setText("");
    this.sourceInfo.setTooltip(null);
    this.updateStatus();
    this.plot.fit();
  }

  void dispose() {
    if (this.disposed) {
      return;
    }
    this.clear();
    this.disposed = true;
    this.worker.shutdownNow();
    this.plot.dispose();
  }

  private final class TracePlot extends Region {
    private final Canvas canvas = new Canvas();
    private final Tooltip hint = new Tooltip();
    private double low, high, start, dragLow, dragHigh;
    private boolean moved;
    private NmrProcessing.Spectrum cachedSpectrum;
    private double cachedLow, cachedHigh, cachedWidth;
    private List<double[]> envelope = List.of();
    private double minY, maxY;
    private final javafx.collections.ListChangeListener<String> theme = c -> this.paint();

    TracePlot() {
      this.setMinHeight(240);
      this.setPrefHeight(280);
      this.getChildren().add(this.canvas);
      Tooltip.install(this.canvas, this.hint);
      this.sceneProperty()
          .addListener(
              (o, before, after) -> {
                if (before != null) {
                  before.getRoot().getStyleClass().removeListener(this.theme);
                }
                if (after != null) {
                  after.getRoot().getStyleClass().addListener(this.theme);
                }
                this.paint();
              });
      this.canvas.setOnScroll(
          e -> {
            if (spectrum == null) {
              return;
            }
            final double full = Math.abs(spectrum.lastPpm() - spectrum.firstPpm());
            final double span =
                Math.min(
                    full,
                    Math.max(
                        Math.abs(spectrum.stepPpm()) * 4,
                        (this.high - this.low) * (e.getDeltaY() > 0 ? .8 : 1.25)));
            final double center = this.ppm(e.getX());
            final double fraction =
                Math.max(0, Math.min(1, (center - this.low) / (this.high - this.low)));
            this.low = center - span * fraction;
            this.high = this.low + span;
            this.clamp();
            this.paint();
            e.consume();
          });
      this.canvas.setOnMousePressed(
          e -> {
            this.start = e.getX();
            this.dragLow = this.low;
            this.dragHigh = this.high;
            this.moved = false;
          });
      this.canvas.setOnMouseDragged(
          e -> {
            final double dx = e.getX() - this.start;
            if (Math.abs(dx) > 3 || !e.isStillSincePress()) {
              this.moved = true;
            }
            if (spectrum == null || adding.isSelected()) {
              return;
            }
            final double shift = dx / this.width() * (this.dragHigh - this.dragLow);
            this.low = this.dragLow + shift;
            this.high = this.dragHigh + shift;
            this.clamp();
            this.paint();
          });
      this.canvas.setOnMouseClicked(
          e -> {
            if (this.moved
                || !e.isStillSincePress()
                || e.getButton() != MouseButton.PRIMARY
                || spectrum == null
                || e.getX() < 58
                || e.getX() > this.getWidth() - 18
                || e.getY() < 24
                || e.getY() > this.getHeight() - 36) {
              return;
            }
            if (editable && adding.isSelected() && selected() != null) {
              final double position =
                  (this.ppm(e.getX()) - spectrum.firstPpm()) / spectrum.stepPpm();
              final int index =
                  Math.max(0, Math.min(spectrum.real().size() - 1, (int) Math.round(position)));
              lineAction.accept(selectedId, BigDecimal.valueOf(spectrum.ppm(index)));
            } else {
              final var nearest = this.nearest(e.getX());
              if (nearest != null) {
                select(nearest.id());
                selectAction.accept(nearest.id());
              }
            }
          });
      this.canvas.setOnMouseMoved(
          e -> {
            if (spectrum == null) {
              return;
            }
            final var p = this.nearest(e.getX());
            this.hint.setText(
                String.format(Locale.ROOT, "δ %.6f ppm", this.ppm(e.getX()))
                    + (p == null ? "" : " · " + p.label() + " · центр δ " + p.centerPpm()));
          });
    }

    void dispose() {
      if (this.getScene() != null) {
        this.getScene().getRoot().getStyleClass().removeListener(this.theme);
      }
      Tooltip.uninstall(this.canvas, this.hint);
    }

    void fit() {
      if (spectrum != null) {
        this.low = Math.min(spectrum.firstPpm(), spectrum.lastPpm());
        this.high = Math.max(spectrum.firstPpm(), spectrum.lastPpm());
      }
      this.cachedSpectrum = null;
      this.paint();
    }

    private void clamp() {
      final double min = Math.min(spectrum.firstPpm(), spectrum.lastPpm()),
          max = Math.max(spectrum.firstPpm(), spectrum.lastPpm());
      if (this.low < min) {
        this.high += min - this.low;
        this.low = min;
      }
      if (this.high > max) {
        this.low -= this.high - max;
        this.high = max;
      }
    }

    private double width() {
      return Math.max(1, this.getWidth() - 76);
    }

    private double x(final double ppm) {
      return 58 + (this.high - ppm) / (this.high - this.low) * this.width();
    }

    private double ppm(final double x) {
      return this.high - (x - 58) / this.width() * (this.high - this.low);
    }

    private double y(final double value) {
      return 24
          + (this.maxY - value) / (this.maxY - this.minY) * Math.max(1, this.getHeight() - 60);
    }

    private Multiplet nearest(final double x) {
      if (data == null) {
        return null;
      }
      double best = 10;
      Multiplet result = null;
      for (final var p : data.multiplets()) {
        double d = Math.abs(this.x(p.centerPpm().doubleValue()) - x);
        if (lineCount <= 20000 || p.id().equals(selectedId)) {
          for (final var v : p.linesPpm()) {
            d = Math.min(d, Math.abs(this.x(v.doubleValue()) - x));
          }
        }
        if (d < best) {
          best = d;
          result = p;
        }
      }
      return result;
    }

    @Override
    protected void layoutChildren() {
      this.canvas.setWidth(this.getWidth());
      this.canvas.setHeight(this.getHeight());
      this.paint();
    }

    private void envelope() {
      if (this.cachedSpectrum == spectrum
          && this.cachedLow == this.low
          && this.cachedHigh == this.high
          && this.cachedWidth == this.width()) {
        return;
      }
      this.cachedSpectrum = spectrum;
      this.cachedLow = this.low;
      this.cachedHigh = this.high;
      this.cachedWidth = this.width();
      final int buckets = Math.max(1, (int) Math.ceil(this.width()));
      final double[] min = new double[buckets], max = new double[buckets];
      Arrays.fill(min, Double.POSITIVE_INFINITY);
      Arrays.fill(max, Double.NEGATIVE_INFINITY);
      this.minY = 0;
      this.maxY = 0;
      for (int i = 0; i < spectrum.real().size(); i++) {
        final double ppm = spectrum.ppm(i);
        if (ppm < this.low || ppm > this.high) {
          continue;
        }
        final int b = Math.min(buckets - 1, Math.max(0, (int) (this.x(ppm) - 58)));
        final double y = spectrum.real().get(i);
        min[b] = Math.min(min[b], y);
        max[b] = Math.max(max[b], y);
        this.minY = Math.min(this.minY, y);
        this.maxY = Math.max(this.maxY, y);
      }
      if (this.minY == this.maxY) {
        this.maxY = this.minY + 1;
      }
      final double padding = (this.maxY - this.minY) * .12;
      this.minY -= padding;
      this.maxY += padding;
      final List<double[]> result = new ArrayList<>();
      for (int b = 0; b < buckets; b++) {
        if (Double.isFinite(min[b])) {
          result.add(new double[] {b + 58, min[b], max[b]});
        }
      }
      this.envelope = List.copyOf(result);
    }

    private void paint() {
      final var g = this.canvas.getGraphicsContext2D();
      final boolean dark = LaboratorySpectralComparisonPlot.dark(this);
      final Color text = Color.web(dark ? "#dbe5f2" : "#334155"),
          blue = Color.web(dark ? "#6da8ff" : "#2563eb"),
          orange = Color.web(dark ? "#ffb870" : "#b45309");
      g.clearRect(0, 0, Math.ceil(this.getWidth()), Math.ceil(this.getHeight()));
      g.setFill(Color.web(dark ? "#162131" : "#ffffff"));
      g.fillRect(0, 0, Math.ceil(this.getWidth()), Math.ceil(this.getHeight()));
      if (spectrum == null || this.high <= this.low || this.getWidth() < 90) {
        return;
      }
      this.envelope();
      g.setFill(text);
      g.setStroke(Color.web(dark ? "#344256" : "#e2e8f0"));
      for (int i = 0; i <= 6; i++) {
        final double x = 58 + this.width() * i / 6;
        g.strokeLine(x, 24, x, this.getHeight() - 36);
        g.fillText(
            LaboratoryAxisTickText.format(this.ppm(x), (this.high - this.low) / 6),
            x - 18,
            this.getHeight() - 16);
      }
      g.fillText(data.nucleus() + " · ppm", this.getWidth() / 2 - 22, this.getHeight() - 1);
      g.save();
      g.beginPath();
      g.rect(58, 24, this.width(), Math.max(1, this.getHeight() - 60));
      g.clip();
      g.setStroke(blue);
      g.setLineWidth(1.2);
      g.beginPath();
      boolean first = true;
      for (final var point : this.envelope) {
        if (first) {
          g.moveTo(point[0], this.y(point[1]));
          first = false;
        } else {
          g.lineTo(point[0], this.y(point[1]));
        }
        g.lineTo(point[0], this.y(point[2]));
      }
      g.stroke();
      final boolean allLines = lineCount <= 20000;
      for (final var p : data.multiplets()) {
        final boolean selected = p.id().equals(selectedId);
        g.setStroke(selected ? orange : blue);
        g.setFill(selected ? orange : blue);
        g.setLineWidth(selected ? 2 : 1);
        final double center = p.centerPpm().doubleValue();
        if (center >= this.low && center <= this.high) {
          final double x = this.x(center);
          g.fillPolygon(new double[] {x - 4, x + 4, x}, new double[] {25, 25, 32}, 3);
        }
        if (selected || allLines) {
          for (final var line : p.linesPpm()) {
            final double ppm = line.doubleValue();
            if (ppm < this.low || ppm > this.high) {
              continue;
            }
            final Double value = LaboratorySpectralComparison.sample(spectrum, ppm);
            if (value != null) {
              final double x = this.x(ppm), y = this.y(value);
              g.strokeLine(x, y - 9, x, y + 9);
            }
          }
        }
      }
      g.restore();
    }
  }
}

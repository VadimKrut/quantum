/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAnalyticalEvidence.Correlation;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAnalyticalEvidence.Correlations;
import ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy.NmrSpectrum2D;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.NmrContours2D;

/** Screen-only processed 2D grid viewer. Selection never inserts a correlation without an explicit action. */
final class LaboratoryNmr2DPane extends VBox {
  record Selection(double xPpm, double yPpm, double intensity) {}

  private record Layer(NmrSpectrum2D spectrum, List<Correlation> marks, String label) {
    Layer {
      marks = List.copyOf(marks);
    }
  }

  private static final ExecutorService CONTOURS =
      Executors.newFixedThreadPool(
          2,
          runnable -> {
            final Thread thread = new Thread(runnable, "nmr-2d-contours");
            thread.setDaemon(true);
            return thread;
          });
  private final Canvas canvas = new Canvas();
  private final Region surface =
      new Region() {
        {
          this.getChildren().add(canvas);
          this.setMinSize(240, 220);
          this.setPrefSize(720, 420);
        }

        @Override
        protected void layoutChildren() {
          canvas.setWidth(this.getWidth());
          canvas.setHeight(this.getHeight());
          paint();
        }
      };
  private final TextField threshold = new TextField("5");
  private final ComboBox<Integer> levels = new ComboBox<>();
  private final CheckBox positive = new CheckBox("+"), negative = new CheckBox("−");
  private final Button add = new Button("Добавить точку");
  private final Label status = new Label(),
      selectedText = new Label("Выберите точку сетки щелчком."),
      legend = new Label();
  private final Tooltip pointHint = new Tooltip();
  private List<Layer> layers = List.of();
  private List<NmrContours2D.Contours> contours = List.of();
  private Consumer<Selection> pick = value -> {};
  private IntConsumer markPick = value -> {};
  private Future<?> task;
  private volatile long generation;
  private boolean editable, disposed, needsContours;
  private String xLabel = "X · ppm", yLabel = "Y · ppm";
  private Selection selected;
  private int selectedRow = -1;
  private double minX = 0,
      maxX = 1,
      minY = 0,
      maxY = 1,
      zoom = 1,
      panX,
      panY,
      pressX,
      pressY,
      previousX,
      previousY;
  private boolean dragged;
  private double hoverX = Double.NaN, hoverY = Double.NaN;

  LaboratoryNmr2DPane() {
    this(false);
  }

  LaboratoryNmr2DPane(final boolean readOnly) {
    this.setSpacing(7);
    this.setMinWidth(0);
    this.setId("nmr-2d-pane");
    this.threshold.setId("nmr-2d-threshold");
    this.threshold.setPrefColumnCount(4);
    this.threshold.setMaxWidth(76);
    this.threshold.setTooltip(
        new Tooltip(
            "Первый уровень: процент от максимального абсолютного сигнала каждого спектра. Матрица и интенсивности не изменяются."));
    this.levels.getItems().setAll(1, 2, 3, 4, 5, 6, 8, 10, 12);
    this.levels.setValue(6);
    this.levels.setPrefWidth(64);
    final Button rebuild = new Button("Обновить контуры"), fit = new Button("Вписать");
    rebuild.setId("nmr-2d-rebuild");
    rebuild.setOnAction(event -> this.rebuild());
    this.threshold.setOnAction(event -> this.rebuild());
    fit.setId("nmr-2d-fit");
    fit.setOnAction(event -> this.fit());
    this.positive.setSelected(true);
    this.negative.setSelected(true);
    this.positive.setTooltip(new Tooltip("Положительные контуры"));
    this.negative.setTooltip(new Tooltip("Отрицательные контуры"));
    this.positive.setOnAction(event -> this.paint());
    this.negative.setOnAction(event -> this.paint());
    final FlowPane controls =
        new FlowPane(
            8,
            6,
            new Label("Порог, %"),
            this.threshold,
            new Label("Уровней"),
            this.levels,
            rebuild,
            this.positive,
            this.negative,
            fit);
    this.add.setId("nmr-2d-add-point");
    this.add.setOnAction(event -> this.publishSelection());
    this.add.setDisable(true);
    final FlowPane selection = new FlowPane(10, 5, this.selectedText, this.add);
    this.selectedText.setWrapText(true);
    this.selectedText.setMaxWidth(550);
    this.status.setWrapText(true);
    this.legend.setWrapText(true);
    this.status.setId("nmr-2d-status");
    this.legend.setId("nmr-2d-legend");
    this.getChildren().addAll(controls, this.legend, this.surface, selection, this.status);
    VBox.setVgrow(this.surface, Priority.ALWAYS);
    this.surface.setPadding(Insets.EMPTY);
    this.canvas.setFocusTraversable(true);
    this.canvas.setId("nmr-2d-canvas");
    Tooltip.install(this.canvas, this.pointHint);
    LaboratoryPreviewTheme.track(this.canvas, this::paint);
    this.canvas.setOnScroll(
        event -> {
          if (!this.inside(event.getX(), event.getY()) || this.layers.isEmpty()) {
            return;
          }
          this.zoomAt(event.getX(), event.getY(), event.getDeltaY() > 0 ? 1.25 : .8);
          event.consume();
        });
    this.canvas.setOnMousePressed(
        event -> {
          if (event.getButton() != MouseButton.PRIMARY && event.getButton() != MouseButton.MIDDLE) {
            return;
          }
          this.canvas.requestFocus();
          this.pressX = this.previousX = event.getX();
          this.pressY = this.previousY = event.getY();
          this.dragged = false;
        });
    this.canvas.setOnMouseDragged(
        event -> {
          if (!event.isPrimaryButtonDown() && !event.isMiddleButtonDown()) {
            return;
          }
          if (Math.hypot(event.getX() - this.pressX, event.getY() - this.pressY) > 4) {
            this.dragged = true;
          }
          if (this.dragged) {
            this.panX += event.getX() - this.previousX;
            this.panY += event.getY() - this.previousY;
            this.clampPan();
            this.paint();
          }
          this.previousX = event.getX();
          this.previousY = event.getY();
          event.consume();
        });
    this.canvas.setOnMouseReleased(
        event -> {
          if (event.getButton() == MouseButton.PRIMARY
              && !this.dragged
              && this.inside(event.getX(), event.getY())) {
            this.chooseMark(event.getX(), event.getY());
            this.choose(this.unX(event.getX()), this.unY(event.getY()));
          }
        });
    this.canvas.setOnMouseMoved(
        event -> {
          this.hoverX = this.inside(event.getX(), event.getY()) ? event.getX() : Double.NaN;
          this.hoverY = Double.isFinite(this.hoverX) ? event.getY() : Double.NaN;
          this.pointHint.setText(
              Double.isFinite(this.hoverX)
                  ? "X "
                      + number(this.unX(this.hoverX))
                      + " · Y "
                      + number(this.unY(this.hoverY))
                      + " ppm"
                  : "");
          this.paint();
        });
    this.canvas.setOnMouseExited(
        event -> {
          this.hoverX = this.hoverY = Double.NaN;
          this.paint();
        });
    this.canvas.setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.ENTER && this.editable) {
            this.publishSelection();
            event.consume();
          } else if (event.getCode() == KeyCode.HOME || event.getCode() == KeyCode.DIGIT0) {
            this.fit();
            event.consume();
          } else if (event.getCode() == KeyCode.ADD
              || event.getCode() == KeyCode.PLUS
              || event.getCode() == KeyCode.EQUALS) {
            this.zoomAt(this.canvas.getWidth() / 2, this.canvas.getHeight() / 2, 1.25);
            event.consume();
          } else if (event.getCode() == KeyCode.SUBTRACT || event.getCode() == KeyCode.MINUS) {
            this.zoomAt(this.canvas.getWidth() / 2, this.canvas.getHeight() / 2, .8);
            event.consume();
          } else if (event.getCode().isArrowKey()) {
            this.panX +=
                event.getCode() == KeyCode.LEFT ? 35 : event.getCode() == KeyCode.RIGHT ? -35 : 0;
            this.panY +=
                event.getCode() == KeyCode.UP ? 35 : event.getCode() == KeyCode.DOWN ? -35 : 0;
            this.clampPan();
            this.paint();
            event.consume();
          }
        });
    this.sceneProperty()
        .addListener(
            (o, before, after) -> {
              if (before != null && after == null && this.task != null && !this.task.isDone()) {
                this.cancel();
                this.needsContours = true;
              }
              if (after != null && this.needsContours && !this.disposed) {
                this.rebuild();
              }
            });
    this.editable(!readOnly);
    this.clear();
  }

  void onPick(final Consumer<Selection> value) {
    this.pick = Objects.requireNonNull(value);
  }

  void onSelect(final IntConsumer value) {
    this.markPick = Objects.requireNonNull(value);
  }

  void editable(final boolean value) {
    this.editable = value;
    this.add.setVisible(value);
    this.add.setManaged(value);
    this.updateSelection();
  }

  void show(final Correlations value) {
    Objects.requireNonNull(value);
    this.xLabel = value.xNucleus() + " · ppm";
    this.yLabel = value.yNucleus() + " · ppm";
    this.setLayers(
        List.of(new Layer(value.spectrum(), value.correlations(), value.experiment().toString())));
  }

  void show(final NmrSpectrum2D spectrum, final List<Correlation> marks) {
    this.xLabel = spectrum == null ? "X · ppm" : spectrum.xNucleus() + " · ppm";
    this.yLabel = spectrum == null ? "Y · ppm" : spectrum.yNucleus() + " · ppm";
    this.setLayers(List.of(new Layer(spectrum, marks, "Спектр")));
  }

  void compare(final Correlations first, final Correlations second) {
    Objects.requireNonNull(first);
    Objects.requireNonNull(second);
    if (first.xNucleus() != second.xNucleus() || first.yNucleus() != second.yNucleus()) {
      throw new IllegalArgumentException("Для наложения 2D требуются одинаковые ядра осей X и Y.");
    }
    this.xLabel = first.xNucleus() + " · ppm";
    this.yLabel = first.yNucleus() + " · ppm";
    this.setLayers(
        List.of(
            new Layer(first.spectrum(), first.correlations(), "A"),
            new Layer(second.spectrum(), second.correlations(), "B")));
    this.editable(false);
  }

  void marks(final List<Correlation> value) {
    if (this.layers.isEmpty()) {
      this.setLayers(List.of(new Layer(null, value, "Корреляции")));
      return;
    }
    final var current = this.layers.getFirst();
    final var updated = new ArrayList<>(this.layers);
    updated.set(0, new Layer(current.spectrum(), value, current.label()));
    this.layers = List.copyOf(updated);
    this.selectedRow = -1;
    this.paint();
  }

  void select(final int row) {
    this.selectedRow = row;
    this.paint();
  }

  void clear() {
    this.setLayers(List.of());
  }

  void fit() {
    this.zoom = 1;
    this.panX = this.panY = 0;
    this.hoverX = this.hoverY = Double.NaN;
    this.paint();
  }

  /** Shared bounds for two comparison panes; no resampling or changes to coordinates. */
  void viewBounds(final double minX, final double maxX, final double minY, final double maxY) {
    if (!Double.isFinite(minX)
        || !Double.isFinite(maxX)
        || !Double.isFinite(minY)
        || !Double.isFinite(maxY)
        || minX >= maxX
        || minY >= maxY
        || Math.max(
                Math.max(Math.abs(minX), Math.abs(maxX)), Math.max(Math.abs(minY), Math.abs(maxY)))
            > 1e7) {
      throw new IllegalArgumentException("Некорректные границы осей 2D.");
    }
    this.minX = minX;
    this.maxX = maxX;
    this.minY = minY;
    this.maxY = maxY;
    this.fit();
  }

  void dispose() {
    this.disposed = true;
    this.cancel();
  }

  private void setLayers(final List<Layer> values) {
    if (this.disposed) {
      return;
    }
    if (!values.isEmpty()
        && values.size() == this.layers.size()
        && java.util.stream.IntStream.range(0, values.size())
            .allMatch(i -> values.get(i).spectrum() == this.layers.get(i).spectrum())) {
      final boolean onlyMarks = values.stream().allMatch(layer -> layer.spectrum() == null);
      this.layers = List.copyOf(values);
      this.selectedRow = -1;
      if (onlyMarks) {
        this.bounds();
        this.fit();
      }
      this.paint();
      return;
    }
    this.cancel();
    this.layers = List.copyOf(values);
    this.contours = List.of();
    this.selected = null;
    this.selectedRow = -1;
    this.updateSelection();
    this.bounds();
    this.fit();
    this.legend.setText(
        this.layers.size() == 2
            ? "A: синий + / красный −; B: зелёный + / фиолетовый −. Порог каждого спектра относительный; абсолютные интенсивности не нормируются."
            : "Синий: + · красный: − · крестики: внесённые корреляции. ppm уменьшается вправо и вверх.");
    this.legend.setVisible(!values.isEmpty());
    this.legend.setManaged(!values.isEmpty());
    this.rebuild();
  }

  private void bounds() {
    double a = Double.POSITIVE_INFINITY,
        b = Double.NEGATIVE_INFINITY,
        c = Double.POSITIVE_INFINITY,
        d = Double.NEGATIVE_INFINITY;
    for (final var layer : this.layers) {
      if (layer.spectrum() != null) {
        final var s = layer.spectrum();
        a = Math.min(a, Math.min(s.xPpm().getFirst(), s.xPpm().getLast()));
        b = Math.max(b, Math.max(s.xPpm().getFirst(), s.xPpm().getLast()));
        c = Math.min(c, Math.min(s.yPpm().getFirst(), s.yPpm().getLast()));
        d = Math.max(d, Math.max(s.yPpm().getFirst(), s.yPpm().getLast()));
      }
      for (final var mark : layer.marks()) {
        a = Math.min(a, mark.xPpm().doubleValue());
        b = Math.max(b, mark.xPpm().doubleValue());
        c = Math.min(c, mark.yPpm().doubleValue());
        d = Math.max(d, mark.yPpm().doubleValue());
      }
    }
    if (!Double.isFinite(a)) {
      a = c = 0;
      b = d = 1;
    }
    if (a == b) {
      a -= .5;
      b += .5;
    }
    if (c == d) {
      c -= .5;
      d += .5;
    }
    this.minX = a;
    this.maxX = b;
    this.minY = c;
    this.maxY = d;
  }

  private void cancel() {
    this.generation++;
    if (this.task != null) {
      this.task.cancel(true);
      this.task = null;
    }
  }

  private void rebuild() {
    if (this.disposed) {
      return;
    }
    final double fraction;
    final int count = this.levels.getValue();
    try {
      fraction = Double.parseDouble(this.threshold.getText().trim().replace(',', '.')) / 100;
      if (!Double.isFinite(fraction) || fraction < .001 || fraction > .99) {
        throw new NumberFormatException();
      }
    } catch (RuntimeException invalid) {
      this.status.setText(
          "Порог должен быть числом от 0,1 до 99% максимального абсолютного сигнала.");
      return;
    }
    this.cancel();
    this.needsContours = false;
    if (this.layers.stream().noneMatch(layer -> layer.spectrum() != null)) {
      this.status.setText(
          this.layers.isEmpty()
              ? "Импортируйте обработанную матрицу CSV/TSV: первая строка — X ppm, первый столбец — Y ppm."
              : "Показаны только внесённые корреляции. Для контуров нужна обработанная матрица 2D.");
      this.paint();
      return;
    }
    this.status.setText("Строю контуры по исходной матрице…");
    final long version = this.generation;
    final var snapshot = this.layers;
    this.task =
        CONTOURS.submit(
            () -> {
              try {
                final List<NmrContours2D.Contours> calculated = new ArrayList<>();
                for (final var layer : snapshot) {
                  calculated.add(
                      layer.spectrum() == null
                          ? new NmrContours2D.Contours(0, List.of())
                          : NmrContours2D.calculate(
                              layer.spectrum(), fraction, count, () -> this.generation != version));
                }
                Platform.runLater(
                    () -> {
                      if (this.disposed || this.generation != version) {
                        return;
                      }
                      this.contours = List.copyOf(calculated);
                      final boolean empty =
                          this.contours.stream()
                              .flatMap(item -> item.levels().stream())
                              .allMatch(level -> level.segments().isEmpty());
                      this.status.setText(
                          empty
                              ? "На выбранных уровнях контуров нет. Исходные значения сохранены; измените порог."
                              : "Колесо: масштаб · перетаскивание: сдвиг · Home: весь спектр. Щелчок выбирает реальную точку сетки.");
                      this.paint();
                    });
              } catch (CancellationException ignored) {
              } catch (RuntimeException failure) {
                Platform.runLater(
                    () -> {
                      if (this.disposed || this.generation != version) {
                        return;
                      }
                      this.contours = List.of();
                      this.status.setText(
                          failure.getMessage() == null
                              ? "Не удалось построить контуры."
                              : failure.getMessage());
                      this.paint();
                    });
              }
            });
  }

  private void choose(final double x, final double y) {
    for (final var layer : this.layers) {
      final var s = layer.spectrum();
      if (s == null || !within(s.xPpm(), x) || !within(s.yPpm(), y)) {
        continue;
      }
      final int ix = nearest(s.xPpm(), x), iy = nearest(s.yPpm(), y);
      this.selected = new Selection(s.xPpm().get(ix), s.yPpm().get(iy), s.value(ix, iy));
      this.updateSelection();
      this.paint();
      return;
    }
    this.selected = null;
    this.updateSelection();
    this.paint();
  }

  private void chooseMark(final double x, final double y) {
    if (this.layers.isEmpty()) {
      return;
    }
    int nearest = -1;
    double distance = 11;
    final var marks = this.layers.getFirst().marks();
    for (int i = 0; i < marks.size(); i++) {
      final var mark = marks.get(i);
      final double d =
          Math.hypot(this.x(mark.xPpm().doubleValue()) - x, this.y(mark.yPpm().doubleValue()) - y);
      if (d < distance) {
        distance = d;
        nearest = i;
      }
    }
    if (nearest >= 0) {
      this.selectedRow = nearest;
      this.markPick.accept(nearest);
    }
  }

  private void publishSelection() {
    if (this.editable && this.selected != null && !this.disposed) {
      this.pick.accept(this.selected);
    }
  }

  private void updateSelection() {
    this.add.setDisable(this.selected == null);
    this.selectedText.setText(
        this.selected == null
            ? "Выберите точку сетки щелчком."
            : "X "
                + number(this.selected.xPpm())
                + " · Y "
                + number(this.selected.yPpm())
                + " ppm · сигнал "
                + number(this.selected.intensity())
                + " (ед. файла)");
    this.selectedText.setTooltip(
        this.selected == null
            ? null
            : new Tooltip(
                "Точная точка сетки: X "
                    + this.selected.xPpm()
                    + "; Y "
                    + this.selected.yPpm()
                    + "; интенсивность "
                    + this.selected.intensity()));
  }

  private static boolean within(final List<Double> axis, final double value) {
    return value >= Math.min(axis.getFirst(), axis.getLast())
        && value <= Math.max(axis.getFirst(), axis.getLast());
  }

  private static int nearest(final List<Double> axis, final double value) {
    int low = 0, high = axis.size() - 1;
    final boolean ascending = axis.getLast() > axis.getFirst();
    while (low < high) {
      final int mid = (low + high) >>> 1;
      if (ascending ? axis.get(mid) < value : axis.get(mid) > value) {
        low = mid + 1;
      } else {
        high = mid;
      }
    }
    return low > 0 && Math.abs(axis.get(low - 1) - value) <= Math.abs(axis.get(low) - value)
        ? low - 1
        : low;
  }

  private double plotWidth() {
    return Math.max(1, this.canvas.getWidth() - 100);
  }

  private double plotHeight() {
    return Math.max(1, this.canvas.getHeight() - 72);
  }

  private boolean inside(final double x, final double y) {
    return x >= 68 && x <= 68 + this.plotWidth() && y >= 25 && y <= 25 + this.plotHeight();
  }

  private double x(final double value) {
    return 68
        + (this.maxX - value) / (this.maxX - this.minX) * this.plotWidth() * this.zoom
        + this.panX;
  }

  private double y(final double value) {
    return 25
        + (value - this.minY) / (this.maxY - this.minY) * this.plotHeight() * this.zoom
        + this.panY;
  }

  private double unX(final double pixel) {
    return this.maxX
        - (pixel - 68 - this.panX) / (this.plotWidth() * this.zoom) * (this.maxX - this.minX);
  }

  private double unY(final double pixel) {
    return this.minY
        + (pixel - 25 - this.panY) / (this.plotHeight() * this.zoom) * (this.maxY - this.minY);
  }

  private void zoomAt(final double px, final double py, final double factor) {
    final double old = this.zoom;
    this.zoom = Math.max(1, Math.min(100, this.zoom * factor));
    this.panX = px - 68 - (px - 68 - this.panX) * this.zoom / old;
    this.panY = py - 25 - (py - 25 - this.panY) * this.zoom / old;
    this.clampPan();
    this.paint();
  }

  private void clampPan() {
    this.panX = Math.max(this.plotWidth() * (1 - this.zoom), Math.min(0, this.panX));
    this.panY = Math.max(this.plotHeight() * (1 - this.zoom), Math.min(0, this.panY));
  }

  private void paint() {
    final var g = this.canvas.getGraphicsContext2D();
    LaboratoryPreviewTheme.clear(g);
    final double width = this.canvas.getWidth(), height = this.canvas.getHeight();
    if (width < 150 || height < 120) {
      return;
    }
    final boolean dark = LaboratoryPreviewTheme.palette(this.canvas).dark();
    g.setFill(Color.web(dark ? "#dbe5f2" : "#475569"));
    if (this.layers.isEmpty()) {
      g.fillText("Обработанный 2D-спектр появится здесь после импорта матрицы.", 22, 48);
      return;
    }
    g.setStroke(Color.web(dark ? "#344256" : "#e2e8f0"));
    g.setLineWidth(1);
    g.setLineDashes();
    for (int i = 0; i <= 5; i++) {
      final double px = 68 + i * this.plotWidth() / 5, py = 25 + i * this.plotHeight() / 5;
      g.strokeLine(px, 25, px, 25 + this.plotHeight());
      g.strokeLine(68, py, 68 + this.plotWidth(), py);
      g.fillText(
          LaboratoryAxisTickText.format(this.unX(px), (this.maxX - this.minX) / (5 * this.zoom)),
          px - 16,
          height - 27);
      g.fillText(
          LaboratoryAxisTickText.format(this.unY(py), (this.maxY - this.minY) / (5 * this.zoom)),
          4,
          py + 4);
    }
    g.fillText(this.xLabel, width / 2 - 30, height - 7);
    g.fillText(this.yLabel, 68, 16);
    g.save();
    g.beginPath();
    g.rect(67, 24, this.plotWidth() + 2, this.plotHeight() + 2);
    g.clip();
    for (int layerIndex = 0; layerIndex < this.contours.size(); layerIndex++) {
      for (final var level : this.contours.get(layerIndex).levels()) {
        final boolean plus = level.intensity() > 0;
        if (plus ? !this.positive.isSelected() : !this.negative.isSelected()) {
          continue;
        }
        g.setStroke(color(layerIndex, plus, dark));
        g.setLineWidth(1.15);
        g.beginPath();
        for (final var line : level.segments()) {
          g.moveTo(this.x(line.x1()), this.y(line.y1()));
          g.lineTo(this.x(line.x2()), this.y(line.y2()));
        }
        g.stroke();
      }
    }
    for (int layerIndex = 0; layerIndex < this.layers.size(); layerIndex++) {
      int row = 0;
      for (final var mark : this.layers.get(layerIndex).marks()) {
        final double px = this.x(mark.xPpm().doubleValue()), py = this.y(mark.yPpm().doubleValue());
        g.setStroke(
            row++ == this.selectedRow && layerIndex == 0
                ? Color.web(dark ? "#ffd18d" : "#a35300")
                : color(layerIndex, true, dark));
        g.setLineWidth(1.5);
        g.strokeLine(px - 4, py, px + 4, py);
        g.strokeLine(px, py - 4, px, py + 4);
      }
    }
    if (Double.isFinite(this.hoverX)) {
      g.setStroke(Color.web(dark ? "#a0aec0" : "#64748b"));
      g.setLineWidth(.7);
      g.setLineDashes(3, 4);
      g.strokeLine(this.hoverX, 25, this.hoverX, 25 + this.plotHeight());
      g.strokeLine(68, this.hoverY, 68 + this.plotWidth(), this.hoverY);
      g.setLineDashes();
    }
    if (this.selected != null) {
      g.setStroke(Color.web(dark ? "#ffd18d" : "#a35300"));
      g.setLineWidth(2);
      g.strokeOval(this.x(this.selected.xPpm()) - 6, this.y(this.selected.yPpm()) - 6, 12, 12);
    }
    g.restore();
  }

  private static Color color(final int layer, final boolean positive, final boolean dark) {
    return Color.web(
        layer == 0
            ? positive ? dark ? "#8ebcff" : "#2563b3" : dark ? "#ff9aa2" : "#bb364c"
            : positive ? dark ? "#77dec2" : "#087b62" : dark ? "#d8b3ff" : "#8750a4");
  }

  private static String number(final double value) {
    return String.format(Locale.ROOT, "%.6g", value);
  }
}

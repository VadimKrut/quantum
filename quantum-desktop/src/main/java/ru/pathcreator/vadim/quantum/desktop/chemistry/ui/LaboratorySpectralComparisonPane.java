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
import java.util.concurrent.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryExperiment;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAnalyticalEvidence.Correlations;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratorySpectralComparison;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratorySpectralComparison.*;

/** Compare the existing local observations without changing their data or claiming chemical equivalence. */
final class LaboratorySpectralComparisonPane extends BorderPane {
  private final LaboratoryExperiment experiment;
  private final Set<String> previousVersions = new HashSet<>();
  private final Map<String, String> duplicateIds = new HashMap<>();
  private final ComboBox<Candidate> first = new ComboBox<>(), second = new ComboBox<>();
  private final CheckBox history = new CheckBox("Включая предыдущие исправления"),
      normalized = new CheckBox("Каждый к максимуму |y| · только отображение"),
      overlay = new CheckBox("Наложить на общие оси");
  private final Label status = note(""), firstInfo = note(""), secondInfo = note("");
  private final TextArea firstDetails = new TextArea(), secondDetails = new TextArea();
  private final LaboratorySpectralComparisonPlot plot = new LaboratorySpectralComparisonPlot();
  private final StackPane plotHost = new StackPane(this.plot);
  private LaboratoryNmr2DPane twoDOverlay, twoDFirst, twoDSecond;
  private GridPane twoDPair;
  private Series renderedFirst, renderedSecond;
  private boolean renderedOverlay;
  private final ExecutorService worker =
      Executors.newSingleThreadExecutor(
          r -> {
            final var t = new Thread(r, "quantum-spectral-comparison");
            t.setDaemon(true);
            return t;
          });
  private Future<?> task;
  private long generation;
  private boolean changing, disposed;
  private Series firstSeries, secondSeries;

  LaboratorySpectralComparisonPane(final LaboratoryExperiment experiment, final Runnable close) {
    this.experiment = experiment;
    this.setId("spectral-comparison");
    this.prepareVersionLabels();
    final var back = new Button("← К аналитике");
    back.setOnAction(
        e -> {
          this.dispose();
          close.run();
        });
    back.setId("spectral-comparison-back");
    final var title = new Label("Сравнение сохранённых спектров");
    title.setStyle("-fx-font-size:18px;-fx-font-weight:bold;");
    this.first.setId("spectral-comparison-first");
    this.second.setId("spectral-comparison-second");
    this.first.setPrefWidth(480);
    this.second.setPrefWidth(480);
    this.first.setMaxWidth(Double.MAX_VALUE);
    this.second.setMaxWidth(Double.MAX_VALUE);
    for (final var selector : List.of(this.first, this.second)) {
      selector.setCellFactory(list -> this.recordCell());
      selector.setButtonCell(this.recordCell());
      selector
          .valueProperty()
          .addListener((o, a, b) -> selector.setTooltip(b == null ? null : recordTooltip(b)));
    }
    final var selectors =
        new FlowPane(
            14,
            10,
            field("Запись A", this.first),
            field("Запись B · совместимые оси и представление", this.second));
    final var fit = new Button("Уместить оба графика");
    fit.setId("spectral-comparison-fit");
    fit.setOnAction(e -> this.fit());
    this.overlay.setId("spectral-comparison-overlay");
    this.normalized.setId("spectral-comparison-normalize");
    this.firstInfo.setStyle("-fx-border-color:#2563eb;-fx-border-width:0 0 0 3;-fx-padding:8;");
    this.secondInfo.setStyle("-fx-border-color:#c2410c;-fx-border-width:0 0 0 3;-fx-padding:8;");
    final var provenance = new FlowPane(12, 8, this.firstInfo, this.secondInfo);
    this.firstInfo.setPrefWidth(480);
    this.secondInfo.setPrefWidth(480);
    for (final var text : List.of(this.firstDetails, this.secondDetails)) {
      text.setEditable(false);
      text.setWrapText(true);
      text.setPrefRowCount(10);
    }
    final var detailA = new TitledPane("Данные и условия записи A", this.firstDetails);
    detailA.setExpanded(false);
    final var detailB = new TitledPane("Данные и условия записи B", this.secondDetails);
    detailB.setExpanded(false);
    final var body =
        new VBox(
            12,
            new FlowPane(12, 8, back, title),
            selectors,
            this.history,
            new FlowPane(12, 8, fit, this.overlay, this.normalized),
            note(
                "Колесо — масштаб; перетаскивание — сдвиг. Сопоставляются сохранённые шкалы: автоматического совмещения пиков и вычитания нет."),
            this.status,
            this.plotHost,
            provenance,
            detailA,
            detailB);
    body.setPadding(new Insets(16));
    final var scroll = new ScrollPane(body);
    scroll.setFitToWidth(true);
    scroll.setFitToHeight(false);
    this.setCenter(scroll);
    this.widthProperty()
        .addListener(
            o -> {
              final double width = Math.max(240, this.getWidth() - 64);
              final double cell = width < 1000 ? width : (width - 14) / 2;
              this.first.setPrefWidth(cell);
              this.second.setPrefWidth(cell);
              this.firstInfo.setPrefWidth(cell);
              this.secondInfo.setPrefWidth(cell);
              this.plot.setPrefHeight(width < 900 && !this.overlay.isSelected() ? 610 : 410);
              this.layoutTwoD();
            });
    this.first
        .valueProperty()
        .addListener(
            o -> {
              if (!this.changing) this.candidates();
            });
    this.second
        .valueProperty()
        .addListener(
            o -> {
              if (!this.changing) this.calculate();
            });
    this.history.selectedProperty().addListener(o -> this.records());
    this.overlay.selectedProperty().addListener(o -> this.render());
    this.normalized.selectedProperty().addListener(o -> this.render());
    this.records();
  }

  private static Label note(final String text) {
    final var label = new Label(text);
    label.setWrapText(true);
    return label;
  }

  private static VBox field(final String label, final javafx.scene.Node content) {
    return new VBox(5, note(label), content);
  }

  private void prepareVersionLabels() {
    this.experiment
        .analyses()
        .forEach(
            a -> {
              if (a.correctsId() != null) {
                this.previousVersions.add("analysis:" + a.correctsId());
              }
            });
    this.experiment
        .analyticalEvidence()
        .entries()
        .forEach(
            a -> {
              if (a.correctsId() != null) {
                this.previousVersions.add("evidence:" + a.correctsId());
              }
            });
    final Map<List<String>, List<Candidate>> groups = new HashMap<>();
    for (final var record : LaboratorySpectralComparison.records(this.experiment, true)) {
      final var label =
          List.of(
              record.title(),
              record.method(),
              record.sample(),
              LaboratorySpectralComparison.recordedTime(record),
              this.versionLabel(record));
      groups.computeIfAbsent(label, k -> new ArrayList<>()).add(record);
    }
    for (final var group : groups.values()) {
      if (group.size() > 1) {
        for (final var record : group) {
          final String id = displayId(record);
          String abbreviation = id.substring(0, Math.min(8, id.length()));
          while (abbreviation.length() < id.length()) {
            final String prefix = abbreviation;
            if (group.stream()
                .noneMatch(
                    other ->
                        !other.key().equals(record.key()) && displayId(other).startsWith(prefix))) {
              break;
            }
            abbreviation = id.substring(0, Math.min(id.length(), abbreviation.length() + 4));
          }
          final String full = abbreviation;
          if (group.stream()
              .anyMatch(
                  other -> !other.key().equals(record.key()) && displayId(other).equals(full))) {
            abbreviation = record.key();
          }
          this.duplicateIds.put(record.key(), abbreviation);
        }
      }
    }
  }

  private static String displayId(final Candidate record) {
    final String id = record.key().substring(record.key().indexOf(':') + 1);
    return id.startsWith("analysis_")
        ? id.substring(9)
        : id.startsWith("evidence_") ? id.substring(9) : id;
  }

  private String versionLabel(final Candidate record) {
    return this.previousVersions.contains(record.key())
        ? "Предыдущая версия"
        : record.correctsId() == null ? "" : "Исправление";
  }

  private String recordContext(final Candidate record) {
    final String version = this.versionLabel(record), id = this.duplicateIds.get(record.key());
    return LaboratorySpectralComparison.recordedTime(record)
        + (version.isEmpty() ? "" : " · " + version)
        + (id == null ? "" : " · № " + id)
        + " · "
        + record.method()
        + " · "
        + record.sample();
  }

  private static Tooltip recordTooltip(final Candidate record) {
    final var tooltip =
        new Tooltip(
            LaboratorySpectralComparison.provenance(record)
                + "\nМестное время: "
                + LaboratorySpectralComparison.recordedTime(record)
                + " · "
                + java.time.ZoneId.systemDefault()
                + "\nТочная запись: "
                + record.key()
                + (record.correctsId() == null
                    ? ""
                    : "\nИсправляет запись: " + record.correctsId()));
    tooltip.setWrapText(true);
    tooltip.setMaxWidth(640);
    return tooltip;
  }

  private ListCell<Candidate> recordCell() {
    return new ListCell<>() {
      private final Label title = new Label(), context = new Label();
      private final VBox content = new VBox(3, this.title, this.context);

      {
        this.title.setMaxWidth(Double.MAX_VALUE);
        this.context.setMaxWidth(Double.MAX_VALUE);
        this.context.setStyle("-fx-font-size:12px;-fx-opacity:0.84;");
        this.content.setMinWidth(0);
        this.content.prefWidthProperty().bind(this.widthProperty().subtract(24));
      }

      @Override
      protected void updateItem(final Candidate record, final boolean empty) {
        super.updateItem(record, empty);
        this.setText(null);
        if (empty || record == null) {
          this.setGraphic(null);
          this.setTooltip(null);
          this.setAccessibleText(null);
          return;
        }
        this.title.setText(record.title());
        this.context.setText(recordContext(record));
        this.setGraphic(this.content);
        this.setTooltip(recordTooltip(record));
        this.setAccessibleText(record.title() + " · " + this.context.getText());
      }
    };
  }

  private void records() {
    this.changing = true;
    final var previous = this.first.getValue();
    this.first
        .getItems()
        .setAll(LaboratorySpectralComparison.records(this.experiment, this.history.isSelected()));
    this.first.setValue(
        this.first.getItems().stream()
            .filter(c -> previous != null && c.key().equals(previous.key()))
            .findFirst()
            .orElse(this.first.getItems().isEmpty() ? null : this.first.getItems().getFirst()));
    this.changing = false;
    this.candidates();
  }

  private void candidates() {
    this.changing = true;
    final var previous = this.second.getValue();
    this.second
        .getItems()
        .setAll(
            this.first.getItems().stream()
                .filter(c -> LaboratorySpectralComparison.compatible(this.first.getValue(), c))
                .toList());
    this.second.setValue(
        this.second.getItems().stream()
            .filter(c -> previous != null && c.key().equals(previous.key()))
            .findFirst()
            .orElse(this.second.getItems().isEmpty() ? null : this.second.getItems().getFirst()));
    this.changing = false;
    this.calculate();
  }

  private void calculate() {
    final long request = ++this.generation;
    if (this.task != null) this.task.cancel(true);
    this.firstSeries = null;
    this.secondSeries = null;
    this.clearTwoD();
    this.plotHost.getChildren().setAll(this.plot);
    this.plot.clear();
    this.firstInfo.setText("");
    this.secondInfo.setText("");
    this.firstDetails.clear();
    this.secondDetails.clear();
    final var a = this.first.getValue();
    final var b = this.second.getValue();
    if (a == null) {
      this.status.setText(
          "Сохраните две собственные записи 1D ЯМР, корреляций 2D или хроматографии, чтобы сравнить их здесь.");
      return;
    }
    if (b == null) {
      this.status.setText(
          "Для этой записи пока нет пары. Нужны одинаковые ядра и тип 2D; для хроматографии — тот же метод и детектор. Числовые трассы сравниваются с трассами, списки сигналов — со списками.");
      return;
    }
    this.status.setText("Подготавливаю сохранённые данные A и B…");
    this.task =
        this.worker.submit(
            () -> {
              try {
                final var left = LaboratorySpectralComparison.series(a);
                final var right = LaboratorySpectralComparison.series(b);
                Platform.runLater(
                    () -> {
                      if (this.disposed || request != this.generation) return;
                      this.firstSeries = left;
                      this.secondSeries = right;
                      this.firstInfo.setText("A · " + LaboratorySpectralComparison.provenance(a));
                      this.secondInfo.setText("B · " + LaboratorySpectralComparison.provenance(b));
                      this.firstDetails.setText(left.details());
                      this.secondDetails.setText(right.details());
                      final boolean quantitativeY =
                          left.kind() != Kind.CORRELATIONS && !left.positionsOnly();
                      this.normalized.setDisable(!quantitativeY);
                      if (!quantitativeY) this.normalized.setSelected(false);
                      this.render();
                    });
              } catch (final RuntimeException error) {
                Platform.runLater(
                    () -> {
                      if (!this.disposed && request == this.generation)
                        this.status.setText(
                            error.getMessage() == null
                                ? "Не удалось подготовить данные."
                                : error.getMessage());
                    });
              }
            });
  }

  private void render() {
    if (this.firstSeries == null || this.secondSeries == null) {
      return;
    }
    if (this.firstSeries.kind() == Kind.CORRELATIONS) {
      if (this.renderedFirst != this.firstSeries
          || this.renderedSecond != this.secondSeries
          || this.renderedOverlay != this.overlay.isSelected()) {
        this.clearTwoD();
        this.renderedFirst = this.firstSeries;
        this.renderedSecond = this.secondSeries;
        this.renderedOverlay = this.overlay.isSelected();
        if (this.overlay.isSelected()) {
          this.twoDOverlay = new LaboratoryNmr2DPane(true);
          this.twoDOverlay.compare(
              this.firstSeries.correlations(), this.secondSeries.correlations());
          this.plotHost.getChildren().setAll(this.twoDOverlay);
        } else {
          this.twoDFirst = new LaboratoryNmr2DPane(true);
          this.twoDSecond = new LaboratoryNmr2DPane(true);
          this.twoDPair = new GridPane();
          this.twoDPair.setHgap(12);
          this.twoDPair.setVgap(12);
          this.twoDFirst.show(this.firstSeries.correlations());
          this.twoDSecond.show(this.secondSeries.correlations());
          this.plotHost.getChildren().setAll(this.twoDPair);
          this.layoutTwoD();
          this.fit();
        }
      }
      this.status.setText(
          (this.overlay.isSelected()
                  ? "Общие шкалы ppm. "
                  : "Начальный диапазон общий; масштаб и сдвиг каждой панели независимы. «Уместить оба графика» возвращает общий диапазон. ")
              + "Контуры — только из сохранённых матриц. Для записи без матрицы показаны лишь отмеченные корреляции. Интенсивности и уровни разных записей не подтверждают количество или назначение.");
    } else {
      this.clearTwoD();
      this.plotHost.getChildren().setAll(this.plot);
      this.plot.show(
          this.firstSeries,
          this.secondSeries,
          this.overlay.isSelected(),
          this.normalized.isSelected());
      this.plot.setPrefHeight(this.getWidth() < 940 && !this.overlay.isSelected() ? 610 : 410);
      this.status.setText(
          this.firstSeries.positionsOnly()
              ? "Показаны введённые центры и линии сигналов. Форма спектра не моделируется; интегралы и введённые J доступны в данных каждой записи."
              : this.normalized.isSelected()
                  ? "Каждая кривая поделена на собственный максимум |y|. Такая нормировка служит только визуальному сравнению формы и не сравнивает количество вещества."
                  : "Шкала X общая. Метки линий относятся к точной исходной трассе. Проверьте условия и единицы: интенсивности разных измерений сами по себе не определяют содержание или чистоту.");
    }
  }

  private void clearTwoD() {
    for (final var pane :
        new LaboratoryNmr2DPane[] {this.twoDOverlay, this.twoDFirst, this.twoDSecond}) {
      if (pane != null) {
        pane.dispose();
      }
    }
    this.twoDOverlay = null;
    this.twoDFirst = null;
    this.twoDSecond = null;
    this.twoDPair = null;
    this.renderedFirst = null;
    this.renderedSecond = null;
  }

  private void layoutTwoD() {
    if (this.twoDPair == null) {
      return;
    }
    final boolean narrow = this.getWidth() < 1100;
    this.twoDPair.getChildren().clear();
    this.twoDPair.getColumnConstraints().clear();
    final var column = new ColumnConstraints();
    column.setPercentWidth(narrow ? 100 : 50);
    column.setHgrow(Priority.ALWAYS);
    this.twoDPair.getColumnConstraints().add(column);
    if (!narrow) {
      final var secondColumn = new ColumnConstraints();
      secondColumn.setPercentWidth(50);
      secondColumn.setHgrow(Priority.ALWAYS);
      this.twoDPair.getColumnConstraints().add(secondColumn);
    }
    this.twoDPair.add(
        new VBox(5, note("A · " + this.firstSeries.record().title()), this.twoDFirst), 0, 0);
    this.twoDPair.add(
        new VBox(5, note("B · " + this.secondSeries.record().title()), this.twoDSecond),
        narrow ? 0 : 1,
        narrow ? 1 : 0);
  }

  private void fit() {
    if (this.twoDOverlay != null) {
      this.twoDOverlay.fit();
      return;
    }
    if (this.twoDFirst != null) {
      final double[] bounds =
          bounds(this.firstSeries.correlations(), this.secondSeries.correlations());
      this.twoDFirst.viewBounds(bounds[0], bounds[1], bounds[2], bounds[3]);
      this.twoDSecond.viewBounds(bounds[0], bounds[1], bounds[2], bounds[3]);
      return;
    }
    this.plot.fit();
  }

  private static double[] bounds(final Correlations... records) {
    double minX = Double.POSITIVE_INFINITY,
        maxX = Double.NEGATIVE_INFINITY,
        minY = Double.POSITIVE_INFINITY,
        maxY = Double.NEGATIVE_INFINITY;
    for (final var record : records) {
      if (record.spectrum() != null) {
        for (final double x : record.spectrum().xPpm()) {
          minX = Math.min(minX, x);
          maxX = Math.max(maxX, x);
        }
        for (final double y : record.spectrum().yPpm()) {
          minY = Math.min(minY, y);
          maxY = Math.max(maxY, y);
        }
      }
      for (final var p : record.correlations()) {
        minX = Math.min(minX, p.xPpm().doubleValue());
        maxX = Math.max(maxX, p.xPpm().doubleValue());
        minY = Math.min(minY, p.yPpm().doubleValue());
        maxY = Math.max(maxY, p.yPpm().doubleValue());
      }
    }
    if (!Double.isFinite(minX)) {
      return new double[] {0, 1, 0, 1};
    }
    if (minX == maxX) {
      minX -= .5;
      maxX += .5;
    }
    if (minY == maxY) {
      minY -= .5;
      maxY += .5;
    }
    final double dx = (maxX - minX) * .04, dy = (maxY - minY) * .04;
    return new double[] {minX - dx, maxX + dx, minY - dy, maxY + dy};
  }

  void dispose() {
    if (this.disposed) return;
    this.disposed = true;
    this.clearTwoD();
    ++this.generation;
    if (this.task != null) this.task.cancel(true);
    this.worker.shutdownNow();
  }
}

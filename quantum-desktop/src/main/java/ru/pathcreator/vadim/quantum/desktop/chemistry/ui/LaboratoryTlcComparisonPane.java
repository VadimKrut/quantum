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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAnalysis;

/** A read-only comparison of explicitly saved TLC observations; Rf is never a conversion estimate. */
final class LaboratoryTlcComparisonPane extends VBox {
  private record SpotRow(String lane, String label, String rf, String basis) {}

  private final ComboBox<LaboratoryAnalysis> left = new ComboBox<>(), right = new ComboBox<>();
  private final ScatterChart<String, Number> chart =
      new ScatterChart<>(new CategoryAxis(), new NumberAxis(0, 1, .2));
  private final Label warnings = label(""), leftDetails = label(""), rightDetails = label("");
  private final TableView<SpotRow> spots = new TableView<>();
  private final Set<String> revised;
  private final Map<String, String> context;
  private static final DateTimeFormatter TIME =
      DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

  LaboratoryTlcComparisonPane(final List<LaboratoryAnalysis> analyses) {
    this(analyses, Map.of());
  }

  LaboratoryTlcComparisonPane(
      final List<LaboratoryAnalysis> analyses, final Map<String, String> context) {
    super(10);
    this.context = Map.copyOf(context);
    this.setId("operations-tlc-comparison");
    this.setPadding(new Insets(12));
    final var available =
        analyses.stream()
            .filter(a -> a.data() instanceof LaboratoryAnalysis.Tlc)
            .sorted(Comparator.comparing(LaboratoryAnalysis::recordedAt))
            .toList();
    this.revised =
        analyses.stream()
            .map(LaboratoryAnalysis::correctsId)
            .filter(Objects::nonNull)
            .collect(java.util.stream.Collectors.toSet());
    for (final var choice : List.of(this.left, this.right)) {
      choice.getItems().setAll(available);
      choice.setMaxWidth(Double.MAX_VALUE);
      choice.setPrefWidth(310);
      choice.setConverter(
          new javafx.util.StringConverter<>() {
            public String toString(final LaboratoryAnalysis a) {
              return a == null
                  ? ""
                  : context.getOrDefault(a.id(), a.sample().isBlank() ? "Без образца" : a.sample())
                      + " · "
                      + a.title()
                      + " · "
                      + TIME.format(a.recordedAt())
                      + " · "
                      + a.origin()
                      + (revised.contains(a.id()) ? " · прежняя версия" : "");
            }

            public LaboratoryAnalysis fromString(final String text) {
              return null;
            }
          });
      choice.setCellFactory(
          list ->
              new ListCell<>() {
                protected void updateItem(final LaboratoryAnalysis a, final boolean empty) {
                  super.updateItem(a, empty);
                  setText(empty || a == null ? null : choice.getConverter().toString(a));
                  setTooltip(
                      empty || a == null
                          ? null
                          : new Tooltip(
                              choice.getConverter().toString(a)
                                  + "\nID анализа: "
                                  + a.id()
                                  + "\nИсточник: "
                                  + a.source()));
                }
              });
      choice.valueProperty().addListener(o -> this.refresh());
    }
    this.left.setId("tlc-comparison-left");
    this.right.setId("tlc-comparison-right");
    this.warnings.setId("tlc-comparison-warnings");
    this.chart.setAnimated(false);
    this.chart.setLegendVisible(false);
    this.chart.setMinHeight(290);
    this.chart.setPrefHeight(330);
    this.chart.setMaxHeight(330);
    this.chart.setId("tlc-comparison-chart");
    this.chart.getYAxis().setLabel("Rf");
    ((CategoryAxis) this.chart.getXAxis())
        .setCategories(javafx.collections.FXCollections.observableArrayList("Проба A", "Проба B"));
    this.spots
        .getColumns()
        .setAll(
            column("Проба", SpotRow::lane, 85),
            column("Пятно / назначение", SpotRow::label, 230),
            column("Rf", SpotRow::rf, 95),
            column("Основание", SpotRow::basis, 260));
    this.spots.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    this.spots.setMinHeight(130);
    this.spots.setPrefHeight(170);
    this.spots.setMaxHeight(170);
    this.spots.setPlaceholder(label("Пятна появятся после выбора сохранённых ТСХ."));
    final FlowPane selectors =
        new FlowPane(
            14,
            8,
            new VBox(4, label("Проба A"), this.left),
            new VBox(4, label("Проба B"), this.right));
    final VBox details = new VBox(8, this.leftDetails, this.rightDetails);
    this.getChildren()
        .setAll(
            label("Сравнение сохранённых ТСХ"),
            selectors,
            this.warnings,
            this.chart,
            this.spots,
            details,
            label(
                "Положение пятна — Rf на общей шкале 0–1. Размер и интенсивность пятен не сравниваются. Совпадение Rf не доказывает идентичность вещества; конверсия из Rf не рассчитывается."));
    if (!available.isEmpty()) {
      this.left.setValue(available.get(Math.max(0, available.size() - 2)));
      this.right.setValue(available.getLast());
    }
    this.refresh();
  }

  private static Label label(final String text) {
    final var label = new Label(text);
    label.setWrapText(true);
    label.setMinWidth(0);
    label.setMinHeight(Region.USE_PREF_SIZE);
    return label;
  }

  private static <T> TableColumn<T, String> column(
      final String title, final java.util.function.Function<T, String> value, final double width) {
    final var column = new TableColumn<T, String>(title);
    column.setPrefWidth(width);
    column.setCellValueFactory(v -> new ReadOnlyStringWrapper(value.apply(v.getValue())));
    column.setCellFactory(
        v ->
            new TableCell<>() {
              protected void updateItem(final String text, final boolean empty) {
                super.updateItem(text, empty);
                this.setText(empty ? null : text);
                this.setTooltip(empty || text == null ? null : new Tooltip(text));
              }
            });
    return column;
  }

  private void refresh() {
    this.chart.getData().clear();
    this.spots.getItems().clear();
    final var a = this.left.getValue();
    final var b = this.right.getValue();
    final List<String> warnings = new ArrayList<>();
    if (a == null || b == null) {
      warnings.add(
          "Для сравнения выберите две сохранённые записи ТСХ. При отсутствии анализов сначала внесите их в аналитику опыта.");
    }
    if (a != null && b != null) {
      if (a.id().equals(b.id())) {
        warnings.add("Выбрана одна и та же запись в обеих пробах.");
      }
      final var x = (LaboratoryAnalysis.Tlc) a.data();
      final var y = (LaboratoryAnalysis.Tlc) b.data();
      this.condition(warnings, "Элюент", x.eluent(), y.eluent());
      this.condition(warnings, "Неподвижная фаза", x.stationary(), y.stationary());
      this.condition(warnings, "Визуализация", x.visualization(), y.visualization());
      if (a.origin() != LaboratoryAnalysis.Origin.MEASURED
          || b.origin() != LaboratoryAnalysis.Origin.MEASURED) {
        warnings.add(
            "Одна или обе записи не являются собственным измерением: происхождение указано ниже.");
      }
      if (this.revised.contains(a.id()) || this.revised.contains(b.id())) {
        warnings.add(
            "Выбрана прежняя версия анализа; сравнение показывает именно её сохранённые данные.");
      }
    }
    this.lane(a, "Проба A", this.leftDetails);
    this.lane(b, "Проба B", this.rightDetails);
    this.warnings.setText(
        warnings.isEmpty()
            ? "Записанные элюент, фаза и визуализация совпадают. Сопоставимость температуры, количества нанесения и прочих условий проверьте по источникам."
            : String.join("\n", warnings));
  }

  private void condition(
      final List<String> warnings, final String name, final String a, final String b) {
    if (a.isBlank() || b.isBlank()) {
      warnings.add(name + " указан не для обеих проб: сопоставимость не подтверждена.");
    } else if (!a.strip()
        .replaceAll("\\s+", " ")
        .equalsIgnoreCase(b.strip().replaceAll("\\s+", " "))) {
      warnings.add(name + " различается. Rf при разных условиях нельзя приравнивать.");
    }
  }

  private void lane(final LaboratoryAnalysis analysis, final String lane, final Label details) {
    if (analysis == null) {
      details.setText(lane + ": запись не выбрана.");
      return;
    }
    final var tlc = (LaboratoryAnalysis.Tlc) analysis.data();
    final var series = new XYChart.Series<String, Number>();
    series.setName(lane);
    int unknown = 0;
    for (final var spot : tlc.spots()) {
      final BigDecimal rf = tlc.rf(spot);
      final String basis =
          spot.reportedRf() != null
              ? "Rf внесён вручную"
              : spot.distanceMm() == null
                  ? "Положение не измерено"
                  : decimal(spot.distanceMm()) + " мм / фронт " + decimal(tlc.frontMm()) + " мм";
      this.spots
          .getItems()
          .add(new SpotRow(lane, spot.label(), rf == null ? "не измерен" : decimal(rf), basis));
      if (rf == null) {
        unknown++;
        continue;
      }
      final var point = new XYChart.Data<String, Number>(lane, rf);
      point
          .nodeProperty()
          .addListener(
              (o, before, node) -> {
                if (node != null) {
                  Tooltip.install(
                      node, new Tooltip(spot.label() + " · Rf " + decimal(rf) + "\n" + basis));
                }
              });
      series.getData().add(point);
    }
    this.chart.getData().add(series);
    details.setText(
        lane
            + " · "
            + analysis.title()
            + "\n"
            + this.context.getOrDefault(analysis.id(), "")
            + "\nОбразец: "
            + (analysis.sample().isBlank() ? "не указан" : analysis.sample())
            + (analysis.sampleId() == null ? "" : " · ID " + analysis.sampleId())
            + "\nДата записи: "
            + TIME.format(analysis.recordedAt())
            + " · "
            + analysis.origin()
            + "\nЭлюент: "
            + present(tlc.eluent())
            + " · фаза: "
            + present(tlc.stationary())
            + " · визуализация: "
            + present(tlc.visualization())
            + "\nИсточник: "
            + present(analysis.source())
            + "\nID анализа: "
            + analysis.id()
            + (analysis.note().isBlank() ? "" : "\n" + analysis.note())
            + (unknown == 0 ? "" : "\nПятен без измеренного Rf: " + unknown));
  }

  private static String present(final String value) {
    return value.isBlank() ? "не указано" : value;
  }

  private static String decimal(final BigDecimal value) {
    return value.round(new java.math.MathContext(5)).stripTrailingZeros().toPlainString();
  }
}

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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.*;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionOperations.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryFileCodec;

/** One chemist's explicit monitoring, phase workup and purification workspace. */
final class LaboratoryOperationsPage extends LaboratoryComputationPage {
  private record Scope(String id, String title) {
    public String toString() {
      return this.title;
    }
  }

  private record SampleChoice(String id, String title) {
    public String toString() {
      return this.title;
    }
  }

  private final LaboratoryEditor editor;
  private final BooleanSupplier save;
  private LaboratoryDrawing expected;
  private final TabPane tabs = new TabPane();
  private final ComboBox<Scope> filter = new ComboBox<>(),
      pointStage = new ComboBox<>(),
      workupStage = new ComboBox<>(),
      runStage = new ComboBox<>();
  private final ComboBox<SampleChoice> pointSample = new ComboBox<>(),
      inputSample = new ComboBox<>();
  private final TableView<TimePoint> points = table();
  private final ListView<Workup> workups = new ListView<>();
  private final ListView<Purification> runs = new ListView<>();
  private final OperationsObservationPane pointObservation, runObservation;
  private final ComboBox<Origin> pointOriginFilter = new ComboBox<>();
  private final VBox tlcComparison = new VBox(8);
  private final ScatterChart<Number, Number> chart =
      new ScatterChart<>(new NumberAxis(), new NumberAxis(0, 100, 20));
  private final TextField pointLabel = new TextField(),
      elapsed = new TextField(),
      conversion = new TextField(),
      pointSource = new TextField();
  private final ComboBox<ElapsedUnit> elapsedUnit =
      enumeration(ElapsedUnit.values(), ElapsedUnit.MINUTE);
  private final ComboBox<ConversionUnit> conversionUnit =
      enumeration(ConversionUnit.values(), ConversionUnit.PERCENT);
  private final ComboBox<AnalysisMethod> method =
      enumeration(AnalysisMethod.values(), AnalysisMethod.TLC);
  private final TextArea pointNote = area(3), workupNote = area(3), runNote = area(4);
  private final TextField workupTitle = new TextField(),
      runTitle = new TextField(),
      stationary = new TextField(),
      solventA = new TextField(),
      solventB = new TextField(),
      runSource = new TextField();
  private final ComboBox<PurificationKind> purificationKind =
      enumeration(PurificationKind.values(), PurificationKind.COLUMN);
  private final ComboBox<GradientAxis> axis =
      enumeration(GradientAxis.values(), GradientAxis.MILLILITRES);
  private final QuantityPair input = new QuantityPair(), sorbent = new QuantityPair();
  private final TableView<Phase> phases = table();
  private final TableView<WorkupStep> workupSteps = table();
  private final TableView<Loss> workupLosses = table(), runLosses = table();
  private final TableView<GradientPoint> gradient = table();
  private final TableView<Fraction> fractions = table();
  private final TableView<Pool> pools = table();
  private final Label pointMode = note("Новая контрольная проба"),
      workupMode = note("Новая обработка"),
      runMode = note("Новая очистка"),
      balance = note(""),
      notice = note("");
  private final VBox leave = new VBox(8);
  private TitledPane pointDetails,
      pointHistory,
      workupHistory,
      phaseDetails,
      workupLossDetails,
      workupNotes,
      runHistory,
      runConditions,
      runFractions,
      runLossDetails;
  private boolean loading, pointDirty, workupDirty, runDirty, conflict;
  private String pointCorrection, workupCorrection, runCorrection;
  private Properties conflictDraft;
  private static final Scope ALL = new Scope("*", "Все стадии"),
      GENERAL = new Scope(null, "Общая запись опыта");
  private static final SampleChoice NO_SAMPLE =
      new SampleChoice(null, "Без связи с физическим образцом");

  LaboratoryOperationsPage(
      final LaboratoryEditor editor,
      final String stageId,
      final BooleanSupplier save,
      final Runnable close) {
    super("Контроль, обработка и очистка", close);
    this.editor = editor;
    this.save = save;
    this.expected = editor.document();
    this.setId("chemistry-operations-page");
    final List<Scope> scopes = new ArrayList<>();
    scopes.add(GENERAL);
    this.expected.experiment().stages().forEach(s -> scopes.add(new Scope(s.id(), s.title())));
    final List<Revision> history = new ArrayList<>();
    history.addAll(this.expected.experiment().operations().timePoints());
    history.addAll(this.expected.experiment().operations().workups());
    history.addAll(this.expected.experiment().operations().purifications());
    history.forEach(
        r -> {
          if (r.stageId() != null && scopes.stream().noneMatch(s -> r.stageId().equals(s.id()))) {
            scopes.add(new Scope(r.stageId(), r.stageTitle() + " · из истории"));
          }
        });
    for (final var c : List.of(this.pointStage, this.workupStage, this.runStage)) {
      c.getItems().setAll(scopes);
      c.setValue(
          scopes.stream().filter(s -> Objects.equals(stageId, s.id())).findFirst().orElse(GENERAL));
      c.setMaxWidth(Double.MAX_VALUE);
      c.setPrefWidth(290);
    }
    this.filter.getItems().add(ALL);
    this.filter.getItems().addAll(scopes);
    this.filter.setValue(
        stageId == null
            ? ALL
            : scopes.stream().filter(s -> stageId.equals(s.id())).findFirst().orElse(ALL));
    this.filter.setId("operations-filter");
    this.filter.setMaxWidth(Double.MAX_VALUE);
    this.filter.setPrefWidth(340);
    this.samples(this.pointSample);
    this.samples(this.inputSample);
    this.pointObservation =
        new OperationsObservationPane(
            () -> this.expected.experiment(),
            () -> this.pointSample.getValue().id(),
            true,
            () -> {
              if (!this.loading) {
                this.pointDirty = true;
              }
            });
    this.runObservation =
        new OperationsObservationPane(
            () -> this.expected.experiment(),
            () -> this.inputSample.getValue().id(),
            true,
            () -> {
              if (!this.loading) {
                this.markRunDirty();
              }
            });
    this.pointSample.valueProperty().addListener(o -> this.pointObservation.sampleChanged());
    this.inputSample.valueProperty().addListener(o -> this.runObservation.sampleChanged());
    this.pointLabel.setPromptText("Например, проба через 30 минут");
    this.pointSource.setPromptText("Измерение, файл анализа или основание значения");
    this.workupTitle.setPromptText("Например, экстракция и концентрирование");
    this.runTitle.setPromptText("Например, колонка на силикагеле");
    this.tabs
        .getTabs()
        .setAll(
            new Tab("Ход реакции", this.monitorPage()),
            new Tab("Обработка смеси", this.workupPage()),
            new Tab("Очистка и фракции", this.purificationPage()));
    this.tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    this.tabs.setId("operations-tabs");
    final VBox center =
        new VBox(
            8, new FlowPane(10, 6, note("Записи стадии"), this.filter), this.notice, this.tabs);
    center.setPadding(new Insets(0, 16, 0, 16));
    VBox.setVgrow(this.tabs, Priority.ALWAYS);
    this.setCenter(center);
    this.leave
        .getChildren()
        .setAll(
            note("Есть незаписанный ввод. Запишите его или явно отбросьте."),
            new FlowPane(
                8,
                8,
                button(
                    "Отбросить ввод и выйти",
                    "discard",
                    () -> {
                      this.clearPending();
                      this.finish();
                    }),
                button("Продолжить работу", "continue", () -> visible(this.leave, false))));
    visible(this.leave, false);
    final VBox bottom =
        new VBox(
            8,
            this.leave,
            new FlowPane(
                8,
                8,
                button("Сохранить опыт", "save", this::saveExperience),
                button("История записей", "history", this::showHistory)),
            this.status);
    bottom.setPadding(new Insets(10, 16, 12, 16));
    this.setBottom(bottom);
    this.watchPoint();
    this.watchWorkup();
    this.watchRun();
    visible(this.balance, false);
    this.balance
        .textProperty()
        .addListener(o -> visible(this.balance, !this.balance.getText().isBlank()));
    this.filter
        .valueProperty()
        .addListener(
            (o, before, after) -> {
              if (this.loading) {
                return;
              }
              if (this.pending()) {
                this.loading = true;
                this.filter.setValue(before);
                this.loading = false;
                this.status.setText("Сначала запишите или сбросьте текущий ввод.");
                return;
              }
              this.refresh();
            });
    this.addEventFilter(
        KeyEvent.KEY_PRESSED,
        e -> {
          if (e.isShortcutDown() && e.getCode() == KeyCode.ENTER) {
            if (this.tabs.getSelectionModel().getSelectedIndex() == 0) {
              this.commitPoint();
            } else if (this.tabs.getSelectionModel().getSelectedIndex() == 1) {
              this.commitWorkup();
            } else {
              this.commitRun();
            }
            e.consume();
          }
        });
    this.refresh();
  }

  private Node monitorPage() {
    this.points
        .getColumns()
        .setAll(
            column("Проба", TimePoint::label, 180),
            pointColumn(
                "Время от начала",
                LaboratoryReactionOperationsService::minutes,
                p -> p.elapsed().toPlainString() + " " + p.elapsedUnit(),
                "мин",
                110),
            pointColumn(
                "Конверсия",
                LaboratoryReactionOperationsService::conversionPercent,
                p ->
                    p.conversion() == null
                        ? "конверсия не задана"
                        : p.conversion().toPlainString() + " " + p.conversionUnit(),
                "%",
                100),
            column(
                "Происхождение",
                p -> LaboratoryReactionOperationsService.originLabel(p.observation().origin()),
                145),
            column("Метод / источник", p -> p.method() + " · " + p.source(), 260));
    compact(this.points, 210);
    this.points.setId("operations-timepoints");
    this.chart.setAnimated(false);
    this.chart.setPrefHeight(250);
    this.chart.setMinHeight(170);
    this.chart.setId("operations-conversion-chart");
    ((NumberAxis) this.chart.getXAxis()).setLabel("Время от начала, мин");
    this.chart.getYAxis().setLabel("Конверсия, %");
    visible(this.tlcComparison, false);
    this.pointOriginFilter.getItems().add(null);
    this.pointOriginFilter.getItems().addAll(Origin.values());
    this.pointOriginFilter.setPromptText("Любое происхождение");
    this.pointOriginFilter.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(Origin value) {
            return value == null
                ? "Любое происхождение"
                : LaboratoryReactionOperationsService.originLabel(value);
          }

          public Origin fromString(String value) {
            return null;
          }
        });
    this.pointOriginFilter.valueProperty().addListener(o -> this.refresh());
    this.pointDetails =
        details(
            "Наблюдение и сравнение ТСХ",
            new VBox(
                10,
                field("Наблюдение", this.pointNote),
                button("Сравнить ТСХ", "tlc-compare", this::compareTlc),
                this.tlcComparison));
    final VBox form =
        new VBox(
            12,
            this.pointMode,
            new FlowPane(
                16, 10, field("Название пробы", this.pointLabel), field("Стадия", this.pointStage)),
            new FlowPane(
                16,
                10,
                field("Время от начала", new HBox(6, this.elapsed, this.elapsedUnit)),
                field("Метод контроля", this.method),
                field("Физический образец", this.pointSample)),
            new FlowPane(
                16,
                10,
                field(
                    "Конверсия · необязательно",
                    new HBox(6, this.conversion, this.conversionUnit))),
            field("Источник / основание значения", this.pointSource),
            note(
                "Процент вводится только при известном основании. Качественную ТСХ можно записать без конверсии."),
            this.pointObservation,
            this.pointDetails);
    this.points.setOnMouseClicked(
        e -> {
          if (e.getClickCount() == 2)
            this.editPoint(this.points.getSelectionModel().getSelectedItem());
        });
    this.points.setOnKeyPressed(
        e -> {
          if (e.getCode() == KeyCode.ENTER) {
            this.editPoint(this.points.getSelectionModel().getSelectedItem());
            e.consume();
          }
        });
    this.pointHistory =
        details(
            "Записанные пробы и график",
            new VBox(
                10,
                new FlowPane(8, 6, note("Показать"), this.pointOriginFilter),
                this.chart,
                this.points,
                button(
                    "Исправить выбранную пробу",
                    "point-edit",
                    () -> this.editPoint(this.points.getSelectionModel().getSelectedItem()))));
    this.pointHistory.setId("operations-point-history");
    final VBox body = new VBox(16, form, this.pointHistory);
    body.setPadding(new Insets(14));
    return operationPage(
        body,
        new FlowPane(
            8,
            8,
            primary("Записать пробу", "point-save", this::commitPoint),
            button("Сбросить ввод", "point-new", this::resetPoint)));
  }

  private Node workupPage() {
    compact(this.workups);
    this.workups.setCellFactory(
        v ->
            new ListCell<>() {
              protected void updateItem(Workup value, boolean empty) {
                super.updateItem(value, empty);
                this.setText(
                    empty || value == null
                        ? null
                        : value.title() + " · " + value.steps().size() + " операций");
              }
            });
    this.workups.setOnMouseClicked(
        e -> {
          if (e.getClickCount() == 2) {
            this.editWorkup(this.workups.getSelectionModel().getSelectedItem());
          }
        });
    this.phases
        .getColumns()
        .setAll(column("Фаза", Phase::label, 220), column("Тип", p -> p.kind().toString(), 180));
    compact(this.phases, 150);
    this.workupSteps
        .getColumns()
        .setAll(
            column("Операция", s -> s.kind().toString(), 120),
            column(
                "Переход",
                s -> this.phaseName(s.fromPhaseId()) + " → " + this.phaseName(s.toPhaseId()),
                220),
            column("Материал / повторы", s -> s.material() + " · " + s.repetitions(), 180),
            column(
                "План / факт (суммарно)",
                s ->
                    quantity(s.planned())
                        + " / "
                        + quantity(s.actual())
                        + (s.actual() == null
                            ? ""
                            : " · "
                                + LaboratoryReactionOperationsService.originLabel(
                                    s.observation().origin())),
                200));
    compact(this.workupSteps, 220);
    lossColumns(this.workupLosses);
    compact(this.workupLosses, 140);
    this.phaseDetails =
        details(
            "Фазы и потоки",
            panel(
                "Жидкие и твёрдые фазы",
                this.phases,
                button("Добавить фазу", "phase-add", () -> this.phaseDialog(null)),
                button(
                    "Изменить",
                    "phase-edit",
                    () -> this.phaseDialog(this.phases.getSelectionModel().getSelectedItem())),
                button("Убрать фазу", "phase-remove", this::removePhase)));
    this.workupLossDetails =
        details(
            "Учтённые потери",
            panel(
                "Потери и отведённые потоки",
                this.workupLosses,
                button(
                    "Добавить потерю",
                    "workup-loss-add",
                    () -> this.lossDialog(this.workupLosses, true, null)),
                button(
                    "Изменить",
                    "workup-loss-edit",
                    () ->
                        this.lossDialog(
                            this.workupLosses,
                            true,
                            this.workupLosses.getSelectionModel().getSelectedItem())),
                button(
                    "Убрать", "workup-loss-remove", () -> this.remove(this.workupLosses, true))));
    this.workupNotes = details("Методика и примечания", this.workupNote);
    final VBox form =
        new VBox(
            12,
            this.workupMode,
            new FlowPane(
                16,
                10,
                field("Название обработки", this.workupTitle),
                field("Стадия", this.workupStage)),
            this.phaseDetails,
            panel(
                "Операции в порядке выполнения",
                this.workupSteps,
                button("Добавить операцию", "workup-step-add", () -> this.workupStepDialog(null)),
                button(
                    "Изменить",
                    "workup-step-edit",
                    () ->
                        this.workupStepDialog(
                            this.workupSteps.getSelectionModel().getSelectedItem())),
                button("↑", "workup-up", () -> this.move(this.workupSteps, -1, true)),
                button("↓", "workup-down", () -> this.move(this.workupSteps, 1, true)),
                button("Убрать", "workup-step-remove", () -> this.remove(this.workupSteps, true))),
            this.workupLossDetails,
            this.workupNotes,
            note(
                "Количество операции — сумма всех повторов. План и полученные значения с их происхождением записываются отдельно."));
    final Button open =
        button(
            "Изменить выбранную обработку",
            "workup-edit",
            () -> this.editWorkup(this.workups.getSelectionModel().getSelectedItem()));
    openOnEnter(this.workups, open);
    this.workupHistory = details("Сохранённые обработки", new VBox(10, this.workups, open));
    final VBox body = new VBox(16, form, this.workupHistory);
    body.setPadding(new Insets(14));
    return operationPage(
        body,
        new FlowPane(
            8,
            8,
            primary("Записать обработку", "workup-save", this::commitWorkup),
            button("Сбросить ввод", "workup-new", this::resetWorkup)));
  }

  private Node purificationPage() {
    compact(this.runs);
    this.runs.setCellFactory(
        v ->
            new ListCell<>() {
              protected void updateItem(Purification value, boolean empty) {
                super.updateItem(value, empty);
                this.setText(
                    empty || value == null
                        ? null
                        : value.title()
                            + " · "
                            + value.kind()
                            + " · "
                            + value.fractions().size()
                            + " фракций");
              }
            });
    this.runs.setOnMouseClicked(
        e -> {
          if (e.getClickCount() == 2) {
            this.editRun(this.runs.getSelectionModel().getSelectedItem());
          }
        });
    this.gradient
        .getColumns()
        .setAll(
            column("Положение", p -> p.position().toPlainString(), 100),
            column("B, %", p -> p.solventBPercent().toPlainString(), 90),
            column("Поток, мл/мин", p -> number(p.flowMlPerMinute()), 110));
    compact(this.gradient, 160);
    this.fractions
        .getColumns()
        .setAll(
            column("Фракция", Fraction::label, 130),
            column(
                "Интервал",
                f ->
                    f.start() == null
                        ? "не задан"
                        : f.start().toPlainString() + "–" + f.end().toPlainString(),
                110),
            column(
                "План / факт",
                f ->
                    quantity(f.planned())
                        + " / "
                        + quantity(f.actual())
                        + (f.actual() == null && f.purityPercent() == null
                            ? ""
                            : " · "
                                + LaboratoryReactionOperationsService.originLabel(
                                    f.observation().origin())),
                170),
            column(
                "Чистота",
                f ->
                    f.purityPercent() == null
                        ? "не задана"
                        : f.purityPercent().toPlainString() + " %",
                110),
            column("Наблюдение", Fraction::note, 200));
    compact(this.fractions, 220);
    this.pools
        .getColumns()
        .setAll(
            column("Объединение", Pool::label, 170),
            column(
                "Фракции",
                p -> String.join(", ", p.fractionIds().stream().map(this::fractionName).toList()),
                240),
            column(
                "После объединения",
                p ->
                    quantity(p.actual())
                        + (p.actual() == null
                            ? ""
                            : " · "
                                + LaboratoryReactionOperationsService.originLabel(
                                    p.observation().origin())),
                180));
    compact(this.pools, 160);
    lossColumns(this.runLosses);
    compact(this.runLosses, 130);
    this.sorbent
        .unit
        .getItems()
        .setAll(
            LaboratoryQuantityUnit.GRAM,
            LaboratoryQuantityUnit.MILLIGRAM,
            LaboratoryQuantityUnit.KILOGRAM);
    this.sorbent.unit.setValue(LaboratoryQuantityUnit.GRAM);
    visible(this.sorbent.actual, false);
    visible(this.sorbent.actualUnit, false);
    this.runConditions =
        details(
            "Условия и методика очистки",
            new VBox(
                10,
                new FlowPane(
                    16,
                    10,
                    field("Сорбент / неподвижная фаза", this.stationary),
                    field("Масса сорбента", this.sorbent.node()),
                    field("Растворитель A", this.solventA),
                    field("Растворитель B", this.solventB)),
                field("Ось градиента и границы фракций", this.axis),
                panel(
                    "Градиент элюента",
                    this.gradient,
                    button("Добавить точку", "gradient-add", () -> this.gradientDialog(null)),
                    button(
                        "Изменить",
                        "gradient-edit",
                        () ->
                            this.gradientDialog(
                                this.gradient.getSelectionModel().getSelectedItem())),
                    button("Убрать", "gradient-remove", () -> this.remove(this.gradient, false))),
                field("Методика", this.runNote)));
    this.runFractions =
        details(
            "Фракции и объединения",
            new VBox(
                12,
                panel(
                    "Фракции",
                    this.fractions,
                    button("Добавить фракцию", "fraction-add", () -> this.fractionDialog(null)),
                    button(
                        "Изменить",
                        "fraction-edit",
                        () ->
                            this.fractionDialog(
                                this.fractions.getSelectionModel().getSelectedItem())),
                    button("Убрать", "fraction-remove", this::removeFraction)),
                panel(
                    "Объединения",
                    this.pools,
                    button("Объединить фракции", "pool-add", () -> this.poolDialog(null)),
                    button(
                        "Изменить",
                        "pool-edit",
                        () -> this.poolDialog(this.pools.getSelectionModel().getSelectedItem())),
                    button(
                        "Убрать объединение", "pool-remove", () -> this.remove(this.pools, false))),
                note(
                    "Объединение заменяет исходные фракции в материальном учёте. Одна фракция входит только в одно объединение.")));
    this.runLossDetails =
        details(
            "Учтённые потери",
            panel(
                "Потери и отведённые потоки",
                this.runLosses,
                button(
                    "Добавить потерю",
                    "run-loss-add",
                    () -> this.lossDialog(this.runLosses, false, null)),
                button(
                    "Изменить",
                    "run-loss-edit",
                    () ->
                        this.lossDialog(
                            this.runLosses,
                            false,
                            this.runLosses.getSelectionModel().getSelectedItem())),
                button("Убрать", "run-loss-remove", () -> this.remove(this.runLosses, false))));
    final VBox form =
        new VBox(
            12,
            this.runMode,
            new FlowPane(
                16,
                10,
                field("Название очистки", this.runTitle),
                field("Метод", this.purificationKind),
                field("Стадия", this.runStage)),
            field("Исходный физический образец", this.inputSample),
            field("Загрузка очистки · план / факт", this.input.node()),
            field("Источник / основание загрузки", this.runSource),
            this.runObservation,
            this.runConditions,
            this.runFractions,
            this.runLossDetails,
            new FlowPane(8, 8, button("Рассчитать баланс материала", "totals", this::showTotals)),
            this.balance,
            note(
                "Связь с образцом сохраняет происхождение. Его остаток и количество продукта меняются отдельным действием."));
    final Button open =
        button(
            "Изменить выбранную очистку",
            "run-edit",
            () -> this.editRun(this.runs.getSelectionModel().getSelectedItem()));
    openOnEnter(this.runs, open);
    this.runHistory =
        details(
            "Сохранённые очистки",
            new VBox(
                10,
                this.runs,
                new FlowPane(
                    8,
                    8,
                    open,
                    button(
                        "Повторить выбранную очистку",
                        "run-repeat",
                        () -> this.openAction("repeat-purification")))));
    this.runHistory.setId("operations-run-history");
    final VBox body = new VBox(16, form, this.runHistory);
    body.setPadding(new Insets(14));
    return operationPage(
        body,
        new FlowPane(
            8,
            8,
            primary("Записать очистку", "run-save", this::commitRun),
            button("Сбросить ввод", "run-new", this::resetRun)));
  }

  private static final class QuantityPair {
    final TextField planned = new TextField(), actual = new TextField();
    final ComboBox<LaboratoryQuantityUnit> unit =
        enumeration(LaboratoryQuantityUnit.values(), LaboratoryQuantityUnit.MILLILITRE);
    final ComboBox<LaboratoryQuantityUnit> actualUnit =
        enumeration(LaboratoryQuantityUnit.values(), LaboratoryQuantityUnit.MILLILITRE);

    QuantityPair() {
      this.planned.setPromptText("План");
      this.actual.setPromptText("Факт");
      this.planned.setPrefColumnCount(8);
      this.actual.setPrefColumnCount(8);
    }

    Node node() {
      return new FlowPane(
          10, 6, new HBox(6, this.planned, this.unit), new HBox(6, this.actual, this.actualUnit));
    }

    LaboratoryQuantity planned() {
      return quantity(this.planned, this.unit.getValue());
    }

    LaboratoryQuantity actual() {
      return quantity(this.actual, this.actualUnit.getValue());
    }

    void set(final LaboratoryQuantity p, final LaboratoryQuantity a) {
      this.unit.setValue(
          p != null ? p.unit() : a != null ? a.unit() : LaboratoryQuantityUnit.MILLILITRE);
      this.actualUnit.setValue(a != null ? a.unit() : this.unit.getValue());
      this.planned.setText(p == null ? "" : p.value().toPlainString());
      this.actual.setText(a == null ? "" : a.value().toPlainString());
    }
  }

  private static void compact(final TableView<?> table, final double maximum) {
    final Runnable resize =
        () -> {
          table.setPrefHeight(Math.min(maximum, Math.max(110, 50 + table.getItems().size() * 42)));
          table.setMinHeight(Region.USE_PREF_SIZE);
          table.setMaxHeight(Region.USE_PREF_SIZE);
        };
    table
        .getItems()
        .addListener((javafx.collections.ListChangeListener<Object>) change -> resize.run());
    resize.run();
  }

  private static void openOnEnter(final ListView<?> list, final Button open) {
    list.setTooltip(new Tooltip("Открыть выбранную запись: двойной щелчок или Enter"));
    list.setOnKeyPressed(
        e -> {
          if (e.getCode() == KeyCode.ENTER
              && !e.isShortcutDown()
              && list.getSelectionModel().getSelectedItem() != null) {
            open.fire();
            e.consume();
          }
        });
  }

  private static void compact(final ListView<?> list) {
    final Runnable resize =
        () -> {
          list.setPrefHeight(Math.min(130, Math.max(56, 8 + list.getItems().size() * 36)));
          list.setMinHeight(Region.USE_PREF_SIZE);
          list.setMaxHeight(Region.USE_PREF_SIZE);
        };
    list.setPlaceholder(note("Сохранённых записей пока нет"));
    list.getItems()
        .addListener((javafx.collections.ListChangeListener<Object>) change -> resize.run());
    resize.run();
  }

  private static <T> TableView<T> table() {
    final var table = new TableView<T>();
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    table.setMinHeight(100);
    table.setPlaceholder(note("Строки ещё не добавлены"));
    return table;
  }

  private static <T> TableColumn<T, String> column(
      final String title, final Function<T, String> value, final double width) {
    final TableColumn<T, String> column = new TableColumn<>(title);
    final Label heading = note(title);
    heading.setTooltip(new Tooltip(title));
    column.setText(null);
    column.setGraphic(heading);
    column.setPrefWidth(width);
    column.setCellValueFactory(v -> new ReadOnlyStringWrapper(value.apply(v.getValue())));
    column.setCellFactory(
        v ->
            new TableCell<>() {
              protected void updateItem(String text, boolean empty) {
                super.updateItem(text, empty);
                this.setText(empty ? null : text);
                this.setTooltip(empty || text == null || text.isBlank() ? null : new Tooltip(text));
                this.setWrapText(true);
              }
            });
    return column;
  }

  /** Monitoring uses the same units as the graph; tooltips retain the exact original input. */
  private static TableColumn<TimePoint, TimePoint> pointColumn(
      final String title,
      final Function<TimePoint, BigDecimal> normalized,
      final Function<TimePoint, String> original,
      final String unit,
      final double width) {
    final TableColumn<TimePoint, TimePoint> column = new TableColumn<>();
    final Label heading = note(title);
    heading.setTooltip(new Tooltip(title));
    column.setGraphic(heading);
    column.setPrefWidth(width);
    column.setCellValueFactory(
        v -> new javafx.beans.property.ReadOnlyObjectWrapper<>(v.getValue()));
    column.setComparator(
        Comparator.comparing(normalized, Comparator.nullsLast(BigDecimal::compareTo)));
    column.setCellFactory(
        v ->
            new TableCell<>() {
              @Override
              protected void updateItem(final TimePoint point, final boolean empty) {
                super.updateItem(point, empty);
                this.setText(
                    empty || point == null
                        ? null
                        : normalized.apply(point) == null
                            ? "не задана"
                            : monitorNumber(normalized.apply(point), unit));
                this.setTooltip(
                    empty || point == null
                        ? null
                        : new Tooltip("Исходный ввод: " + original.apply(point)));
                this.setWrapText(true);
              }
            });
    return column;
  }

  private static String monitorNumber(final BigDecimal value, final String unit) {
    final var shown = value.round(new java.math.MathContext(5, java.math.RoundingMode.HALF_UP));
    return (shown.compareTo(value) == 0 ? "" : "≈ ")
        + shown.stripTrailingZeros().toPlainString()
        + " "
        + unit;
  }

  private static void lossColumns(final TableView<Loss> table) {
    table
        .getColumns()
        .setAll(
            column("Потеря / поток", Loss::label, 220),
            column("План / факт", l -> quantity(l.planned()) + " / " + quantity(l.actual()), 180),
            column(
                "Происхождение / источник",
                l ->
                    LaboratoryReactionOperationsService.originLabel(l.observation().origin())
                        + " · "
                        + l.source(),
                220));
  }

  private static <E> ComboBox<E> enumeration(final E[] values, final E value) {
    final var box = new ComboBox<E>(FXCollections.observableArrayList(values));
    box.setValue(value);
    return box;
  }

  private static TextArea area(final int rows) {
    final var value = new TextArea();
    value.setWrapText(true);
    value.setPrefRowCount(rows);
    return value;
  }

  private static VBox field(final String title, final Node value) {
    final var box = new VBox(4, note(title), value);
    box.setMinWidth(0);
    if (value instanceof TextField text) {
      text.setPrefColumnCount(18);
    }
    return box;
  }

  private static VBox panel(final String title, final Node content, final Button... actions) {
    return new VBox(7, note(title), content, new FlowPane(7, 7, actions));
  }

  private static TitledPane details(final String title, final Node content) {
    final var box = new VBox(10, content);
    box.setPadding(new Insets(10, 2, 4, 2));
    final var pane = new TitledPane(title, box);
    pane.setAnimated(false);
    pane.setExpanded(false);
    return pane;
  }

  private Button primary(final String text, final String id, final Runnable action) {
    final var button = this.button(text, id, action);
    button.getStyleClass().add("laboratory-primary-action");
    return button;
  }

  private ScrollPane formScroll(final int tab) {
    return (ScrollPane) ((BorderPane) this.tabs.getTabs().get(tab).getContent()).getCenter();
  }

  private void focusForm(final int tab) {
    this.tabs.getSelectionModel().select(tab);
    this.formScroll(tab).setVvalue(0);
  }

  private void revealDetails() {
    this.pointDetails.setExpanded(
        !this.pointNote.getText().isBlank() || this.tlcComparison.isVisible());
    this.phaseDetails.setExpanded(!this.phases.getItems().isEmpty());
    this.workupLossDetails.setExpanded(!this.workupLosses.getItems().isEmpty());
    this.workupNotes.setExpanded(!this.workupNote.getText().isBlank());
    this.runConditions.setExpanded(
        !this.stationary.getText().isBlank()
            || !this.solventA.getText().isBlank()
            || !this.solventB.getText().isBlank()
            || !this.sorbent.planned.getText().isBlank()
            || !this.gradient.getItems().isEmpty()
            || !this.runNote.getText().isBlank());
    this.runFractions.setExpanded(
        !this.fractions.getItems().isEmpty() || !this.pools.getItems().isEmpty());
    this.runLossDetails.setExpanded(!this.runLosses.getItems().isEmpty());
  }

  private Button button(final String text, final String id, final Runnable action) {
    final var button = new Button(text);
    button.setId("operations-" + id);
    button.setOnAction(e -> this.safe(action));
    return button;
  }

  private void safe(final Runnable action) {
    try {
      action.run();
    } catch (final RuntimeException error) {
      this.status.setText(message(error));
    }
  }

  private static String message(final Throwable error) {
    return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
  }

  private static void visible(final Node node, final boolean value) {
    node.setVisible(value);
    node.setManaged(value);
  }

  private static String number(final BigDecimal value) {
    return value == null ? "—" : value.toPlainString();
  }

  private static String quantity(final LaboratoryQuantity value) {
    return value == null ? "не задано" : LaboratoryQuantityText.format(value, true);
  }

  private static BigDecimal decimal(final TextInputControl field, final boolean required) {
    if (field.getText().isBlank()) {
      if (required) {
        throw new IllegalArgumentException("Заполните числовое поле.");
      }
      return null;
    }
    try {
      return new BigDecimal(field.getText().strip().replace(',', '.'));
    } catch (final NumberFormatException error) {
      throw new IllegalArgumentException("Введите корректное число.");
    }
  }

  private static LaboratoryQuantity quantity(
      final TextInputControl value, final LaboratoryQuantityUnit unit) {
    final var n = decimal(value, false);
    return n == null ? null : new LaboratoryQuantity(n, unit);
  }

  private static String id(final String prefix) {
    return LaboratoryReactionOperationsService.newId(prefix);
  }

  private void samples(final ComboBox<SampleChoice> box) {
    box.getItems().setAll(NO_SAMPLE);
    this.expected
        .experiment()
        .samples()
        .forEach(
            s ->
                box.getItems()
                    .add(new SampleChoice(s.id(), s.code() + " · " + s.balance().label())));
    box.setValue(NO_SAMPLE);
    box.setMaxWidth(Double.MAX_VALUE);
    box.setPrefWidth(290);
  }

  private void sample(final ComboBox<SampleChoice> box, final String id) {
    final var selected =
        box.getItems().stream().filter(s -> Objects.equals(s.id(), id)).findFirst();
    if (id != null && selected.isEmpty()) {
      box.getItems().add(new SampleChoice(id, "Недоступный образец · " + id));
      box.setValue(box.getItems().getLast());
    } else {
      box.setValue(selected.orElse(NO_SAMPLE));
    }
  }

  private void scope(final ComboBox<Scope> box, final String id) {
    box.setValue(
        box.getItems().stream()
            .filter(s -> Objects.equals(s.id(), id))
            .findFirst()
            .orElse(GENERAL));
  }

  private Scope defaultScope() {
    return this.filter.getValue() == null || this.filter.getValue().equals(ALL)
        ? GENERAL
        : this.filter.getValue();
  }

  private String phaseName(final String id) {
    return this.phases.getItems().stream()
        .filter(p -> p.id().equals(id))
        .map(Phase::label)
        .findFirst()
        .orElse("не задана");
  }

  private String fractionName(final String id) {
    return this.fractions.getItems().stream()
        .filter(f -> f.id().equals(id))
        .map(Fraction::label)
        .findFirst()
        .orElse(id);
  }

  private boolean inScope(final Revision record) {
    return ALL.equals(this.filter.getValue())
        || Objects.equals(record.stageId(), this.filter.getValue().id());
  }

  private void requireCurrent() {
    if (this.conflict || !this.expected.equals(this.editor.document())) {
      this.conflict = true;
      throw new IllegalArgumentException(
          "Опыт изменился. Ввод сохранён как черновик; откройте актуальную версию перед записью.");
    }
  }

  private void clean() {
    if (this.pending()) {
      this.revealPending();
      throw new IllegalArgumentException(this.pendingMessage());
    }
  }

  private String pendingMessage() {
    if (this.conflict) {
      return "Черновик относится к другой версии опыта. Вернитесь назад, чтобы проверить ввод или явно отбросить его; записанные данные опыта останутся.";
    }
    final List<String> sections = new ArrayList<>();
    if (this.pointDirty) {
      sections.add("Ход реакции");
    }
    if (this.workupDirty) {
      sections.add("Обработка смеси");
    }
    if (this.runDirty) {
      sections.add("Очистка и фракции");
    }
    return "Незаписанный ввод: «"
        + String.join("», «", sections)
        + "». Запишите его или нажмите «Сбросить ввод» в указанном разделе.";
  }

  private void revealPending() {
    if (!this.conflict) {
      this.revealDetails();
      this.focusForm(this.pointDirty ? 0 : this.workupDirty ? 1 : 2);
    }
  }

  private void saveExperience() {
    this.clean();
    this.status.setText("");
    this.status.setText(
        this.save.getAsBoolean()
            ? "Опыт сохранён в файл."
            : "Сохранение текущей версии не подтверждено. Записанные данные остаются в опыте.");
  }

  private void resetStatus(final String section) {
    this.status.setText(
        this.pending()
            ? this.pendingMessage()
            : "Ввод в разделе «"
                + section
                + "» сброшен. Записанные данные опыта сохранены"
                + (this.editor.dirty() ? "; сохраните файл." : "."));
    if (!this.pending()) {
      visible(this.leave, false);
    }
  }

  boolean pending() {
    return this.pointDirty || this.workupDirty || this.runDirty || this.conflict;
  }

  private void clearPending() {
    this.pointDirty = false;
    this.workupDirty = false;
    this.runDirty = false;
    this.conflict = false;
    this.conflictDraft = null;
  }

  @Override
  boolean canClose() {
    if (!this.pending()) {
      return true;
    }
    this.revealPending();
    this.status.setText(this.pendingMessage());
    visible(this.leave, true);
    return false;
  }

  void openAction(final String action) {
    this.safe(
        () -> {
          if (this.pending()) {
            throw new IllegalArgumentException("Сначала запишите или сбросьте текущий ввод.");
          }
          switch (action) {
            case "sample" -> {
              this.tabs.getSelectionModel().select(0);
              this.resetPoint();
              this.pointLabel.requestFocus();
            }
            case "purification" -> {
              this.tabs.getSelectionModel().select(2);
              this.runTitle.requestFocus();
            }
            case "repeat-purification" -> {
              this.tabs.getSelectionModel().select(2);
              final var selected = this.runs.getSelectionModel().getSelectedItem();
              if (selected == null) {
                this.status.setText(
                    "Выберите сохранённую очистку, затем нажмите «Повторить выбранную очистку».");
                this.runHistory.setExpanded(true);
                this.formScroll(2).setVvalue(1);
                this.runs.requestFocus();
                return;
              }
              final var copy =
                  LaboratoryReactionOperationsService.repeatPlan(
                          new LaboratoryReactionOperations(
                              List.of(), List.of(), this.purificationHistory(selected)),
                          this.expected.experiment())
                      .purifications();
              if (copy.isEmpty()) {
                throw new IllegalArgumentException(
                    "Стадия исходной очистки отсутствует. Создайте новую очистку и явно выберите стадию.");
              }
              this.editRun(copy.getFirst());
              this.runCorrection = null;
              this.runTitle.setText(selected.title() + " — повтор");
              this.runMode.setText(
                  "Новая очистка по методике выбранной · факты и связи образцов очищены");
              this.markRunDirty();
              this.runTitle.requestFocus();
              this.status.setText(
                  "Новый план очистки подготовлен по выбранной методике. Проверьте параметры и нажмите «Записать очистку».");
            }
            case "tlc" -> {
              this.tabs.getSelectionModel().select(0);
              this.compareTlc();
            }
            default -> throw new IllegalArgumentException("Неизвестное действие контроля.");
          }
        });
  }

  private List<Purification> purificationHistory(final Purification selected) {
    final List<Purification> records = this.expected.experiment().operations().purifications(),
        history = new ArrayList<>();
    String next = selected.id();
    for (int i = records.size() - 1; i >= 0 && next != null; i--) {
      final var record = records.get(i);
      if (record.id().equals(next)) {
        history.add(record);
        next = record.correctsId();
      }
    }
    if (next != null) {
      throw new IllegalArgumentException(
          "История выбранной очистки неполна. Откройте сохранённый опыт заново.");
    }
    Collections.reverse(history);
    return List.copyOf(history);
  }

  private void compareTlc() {
    final var experiment = this.expected.experiment();
    final String stageId = this.pointStage.getValue().id();
    final var points =
        experiment.operations().activeTimePoints().stream()
            .filter(p -> Objects.equals(p.stageId(), stageId))
            .toList();
    final Set<String> samples = new HashSet<>();
    points.stream().map(TimePoint::sampleId).filter(Objects::nonNull).forEach(samples::add);
    final String selected = this.pointSample.getValue().id();
    experiment.samples().stream()
        .filter(s -> Objects.equals(s.id(), selected) && Objects.equals(s.stageId(), stageId))
        .forEach(s -> samples.add(s.id()));
    final Map<String, String> context = new HashMap<>();
    final var analyses =
        experiment.analyses().stream()
            .filter(
                a ->
                    a.sampleId() != null && samples.contains(a.sampleId())
                        || a.sampleId() == null && stageId == null && selected == null)
            .toList();
    for (final var analysis : analyses) {
      final String sample =
          experiment.samples().stream()
              .filter(s -> s.id().equals(analysis.sampleId()))
              .map(s -> s.code())
              .findFirst()
              .orElse("Без физического образца");
      final String times =
          points.stream()
              .filter(p -> Objects.equals(p.sampleId(), analysis.sampleId()))
              .map(
                  p ->
                      LaboratoryReactionOperationsService.minutes(p)
                              .stripTrailingZeros()
                              .toPlainString()
                          + " мин")
              .distinct()
              .collect(java.util.stream.Collectors.joining(", "));
      context.put(
          analysis.id(),
          sample + (times.isEmpty() ? " · время контроля не записано" : " · контроль: " + times));
    }
    final var view = new LaboratoryTlcComparisonPane(analyses, context);
    this.tlcComparison.getChildren().clear();
    this.tlcComparison
        .getChildren()
        .setAll(
            button("Закрыть сравнение", "tlc-close", () -> visible(this.tlcComparison, false)),
            view);
    visible(this.tlcComparison, true);
    this.pointDetails.setExpanded(true);
  }

  private void watchPoint() {
    for (final TextInputControl field :
        List.of(this.pointLabel, this.elapsed, this.conversion, this.pointSource, this.pointNote)) {
      field
          .textProperty()
          .addListener(
              o -> {
                if (!this.loading) {
                  this.pointDirty = true;
                }
              });
    }
    for (final ComboBox<?> box :
        List.of(
            this.pointStage,
            this.pointSample,
            this.elapsedUnit,
            this.conversionUnit,
            this.method)) {
      box.valueProperty()
          .addListener(
              o -> {
                if (!this.loading) {
                  this.pointDirty = true;
                }
              });
    }
  }

  private void watchWorkup() {
    for (final TextInputControl field : List.of(this.workupTitle, this.workupNote)) {
      field
          .textProperty()
          .addListener(
              o -> {
                if (!this.loading) {
                  this.workupDirty = true;
                }
              });
    }
    this.workupStage
        .valueProperty()
        .addListener(
            o -> {
              if (!this.loading) {
                this.workupDirty = true;
              }
            });
  }

  private static BorderPane operationPage(final Node body, final Node actions) {
    final var scroll = scroll(body);
    scroll.setFitToHeight(false);
    final var page = new BorderPane(scroll);
    final var footer = new VBox(actions);
    footer.setPadding(new Insets(10, 14, 10, 14));
    page.setBottom(footer);
    return page;
  }

  private void markRunDirty() {
    this.runDirty = true;
    this.balance.setText(
        "Количество или условия изменены. Пересчитайте материальный учёт по текущему вводу.");
  }

  private void watchRun() {
    for (final TextInputControl field :
        List.of(
            this.runTitle,
            this.stationary,
            this.solventA,
            this.solventB,
            this.runSource,
            this.runNote,
            this.input.planned,
            this.input.actual,
            this.sorbent.planned)) {
      field
          .textProperty()
          .addListener(
              o -> {
                if (!this.loading) {
                  this.markRunDirty();
                }
              });
    }
    for (final ComboBox<?> box :
        List.of(
            this.runStage,
            this.inputSample,
            this.purificationKind,
            this.axis,
            this.input.unit,
            this.input.actualUnit,
            this.sorbent.unit)) {
      box.valueProperty()
          .addListener(
              o -> {
                if (!this.loading) {
                  this.markRunDirty();
                }
              });
    }
  }

  private void refresh() {
    final var records = this.expected.experiment().operations();
    this.points
        .getItems()
        .setAll(
            records.activeTimePoints().stream()
                .filter(this::inScope)
                .filter(
                    p ->
                        this.pointOriginFilter.getValue() == null
                            || p.observation().origin() == this.pointOriginFilter.getValue())
                .sorted(Comparator.comparing(LaboratoryReactionOperationsService::minutes))
                .toList());
    this.workups.getItems().setAll(records.activeWorkups().stream().filter(this::inScope).toList());
    this.runs
        .getItems()
        .setAll(records.activePurifications().stream().filter(this::inScope).toList());
    this.chart.getData().clear();
    final Map<String, XYChart.Series<Number, Number>> series = new LinkedHashMap<>();
    for (final var point : this.points.getItems()) {
      final var value = LaboratoryReactionOperationsService.conversionPercent(point);
      if (value == null) {
        continue;
      }
      final String key =
          Objects.toString(point.stageId(), "")
              + " · "
              + point.method()
              + " · "
              + point.observation().origin();
      final var group =
          series.computeIfAbsent(
              key,
              k -> {
                final var s = new XYChart.Series<Number, Number>();
                s.setName(
                    (point.stageTitle().isBlank() ? "Общая запись" : point.stageTitle())
                        + " · "
                        + point.method()
                        + " · "
                        + LaboratoryReactionOperationsService.originLabel(
                            point.observation().origin()));
                return s;
              });
      final var datum =
          new XYChart.Data<Number, Number>(
              LaboratoryReactionOperationsService.minutes(point), value);
      datum
          .nodeProperty()
          .addListener(
              (o, a, b) -> {
                if (b != null) {
                  Tooltip.install(
                      b,
                      new Tooltip(
                          point.label()
                              + " · "
                              + LaboratoryReactionOperationsService.originLabel(
                                  point.observation().origin())
                              + "\n"
                              + value.toPlainString()
                              + " %\n"
                              + point.source()));
                }
              });
      group.getData().add(datum);
    }
    this.chart.getData().addAll(series.values());
    this.pointHistory.setText("Записанные пробы и график · " + this.points.getItems().size());
    this.workupHistory.setText("Сохранённые обработки · " + this.workups.getItems().size());
    this.runHistory.setText("Сохранённые очистки · " + this.runs.getItems().size());
    this.notice.setText(
        "Проб: "
            + this.points.getItems().size()
            + " · обработок: "
            + this.workups.getItems().size()
            + " · очисток: "
            + this.runs.getItems().size()
            + ". График разделяет измерения, литературу, модель и неизвестное происхождение.");
  }

  private void commit(final LaboratoryReactionOperations next, final String reason) {
    this.requireCurrent();
    this.editor.operations(this.expected, next, reason);
    this.expected = this.editor.document();
    this.refresh();
    this.status.setText(reason + ". Сохраните файл опыта.");
  }

  private void commitPoint() {
    this.safe(
        () -> {
          if (this.pointCorrection != null && !this.pointDirty) {
            this.status.setText("Проба не изменена.");
            return;
          }
          final String reason =
              this.pointCorrection == null ? "Проба записана" : "Исправление пробы записано";
          final var stage = this.pointStage.getValue();
          final var value =
              new TimePoint(
                  id("timepoint"),
                  stage.id(),
                  stage.title(),
                  Instant.now(),
                  decimal(this.elapsed, true),
                  this.elapsedUnit.getValue(),
                  this.pointSample.getValue().id(),
                  this.pointLabel.getText(),
                  this.method.getValue(),
                  decimal(this.conversion, false),
                  this.conversionUnit.getValue(),
                  this.pointSource.getText(),
                  this.pointNote.getText(),
                  this.pointCorrection,
                  this.pointObservation.value());
          this.commit(
              LaboratoryReactionOperationsService.append(
                  this.expected.experiment().operations(), value),
              reason);
          this.resetPoint();
          this.status.setText(reason + ". Сохраните файл опыта.");
        });
  }

  private Workup workupValue() {
    final var stage = this.workupStage.getValue();
    return new Workup(
        id("workup"),
        stage.id(),
        stage.title(),
        Instant.now(),
        this.workupTitle.getText(),
        this.phases.getItems(),
        this.workupSteps.getItems(),
        this.workupLosses.getItems(),
        this.workupNote.getText(),
        this.workupCorrection);
  }

  private Purification runValue() {
    final var stage = this.runStage.getValue();
    return new Purification(
        id("purification"),
        stage.id(),
        stage.title(),
        Instant.now(),
        this.runTitle.getText(),
        this.purificationKind.getValue(),
        this.inputSample.getValue().id(),
        this.input.planned(),
        this.input.actual(),
        this.stationary.getText(),
        this.sorbent.planned(),
        this.solventA.getText(),
        this.solventB.getText(),
        this.axis.getValue(),
        this.gradient.getItems(),
        this.fractions.getItems(),
        this.pools.getItems(),
        this.runLosses.getItems(),
        this.runSource.getText(),
        this.runNote.getText(),
        this.runCorrection,
        this.runObservation.value());
  }

  private void commitWorkup() {
    this.safe(
        () -> {
          if (this.workupCorrection != null && !this.workupDirty) {
            this.status.setText("Обработка не изменена.");
            return;
          }
          final var value = this.workupValue();
          this.commit(
              LaboratoryReactionOperationsService.append(
                  this.expected.experiment().operations(), value),
              this.workupCorrection == null
                  ? "Обработка записана"
                  : "Новая версия обработки записана");
          this.workupCorrection = value.id();
          this.workupDirty = false;
          this.workupMode.setText("Изменение записанной обработки создаст следующую версию");
        });
  }

  private void commitRun() {
    this.safe(
        () -> {
          if (this.runCorrection != null && !this.runDirty) {
            this.status.setText("Очистка не изменена.");
            return;
          }
          final var value = this.runValue();
          this.commit(
              LaboratoryReactionOperationsService.append(
                  this.expected.experiment().operations(), value),
              this.runCorrection == null ? "Очистка записана" : "Новая версия очистки записана");
          this.runCorrection = value.id();
          this.runDirty = false;
          this.runMode.setText("Изменение записанной очистки создаст следующую версию");
          this.showTotals();
        });
  }

  private void resetPoint() {
    this.loading = true;
    this.pointLabel.clear();
    this.elapsed.clear();
    this.conversion.clear();
    this.pointSource.clear();
    this.pointNote.clear();
    this.pointSample.setValue(NO_SAMPLE);
    this.pointStage.setValue(this.defaultScope());
    this.pointCorrection = null;
    this.pointObservation.set(Observation.EMPTY);
    this.pointDirty = false;
    this.pointMode.setText("Новая контрольная проба");
    this.pointDetails.setExpanded(false);
    visible(this.tlcComparison, false);
    this.loading = false;
    this.resetStatus("Ход реакции");
  }

  private void resetWorkup() {
    this.loading = true;
    this.workupTitle.clear();
    this.workupNote.clear();
    this.phases.getItems().clear();
    this.workupSteps.getItems().clear();
    this.workupLosses.getItems().clear();
    this.workupStage.setValue(this.defaultScope());
    this.workupCorrection = null;
    this.workupDirty = false;
    this.workupMode.setText("Новая обработка");
    this.phaseDetails.setExpanded(false);
    this.workupLossDetails.setExpanded(false);
    this.workupNotes.setExpanded(false);
    this.loading = false;
    this.resetStatus("Обработка смеси");
  }

  private void resetRun() {
    this.loading = true;
    this.runTitle.clear();
    this.runNote.clear();
    this.stationary.clear();
    this.solventA.clear();
    this.solventB.clear();
    this.runSource.clear();
    this.input.set(null, null);
    this.sorbent.planned.clear();
    this.inputSample.setValue(NO_SAMPLE);
    this.gradient.getItems().clear();
    this.fractions.getItems().clear();
    this.pools.getItems().clear();
    this.runLosses.getItems().clear();
    this.runStage.setValue(this.defaultScope());
    this.runCorrection = null;
    this.runObservation.set(Observation.EMPTY);
    this.runDirty = false;
    this.balance.setText("");
    this.runMode.setText("Новая очистка");
    this.runConditions.setExpanded(false);
    this.runFractions.setExpanded(false);
    this.runLossDetails.setExpanded(false);
    this.loading = false;
    this.resetStatus("Очистка и фракции");
  }

  private void editPoint(final TimePoint point) {
    if (point == null) {
      return;
    }
    this.safe(
        () -> {
          if (this.pointDirty) {
            throw new IllegalArgumentException(
                "Сначала запишите пробу или нажмите «Сбросить ввод».");
          }
          this.loading = true;
          this.pointCorrection = point.id();
          this.scope(this.pointStage, point.stageId());
          this.sample(this.pointSample, point.sampleId());
          this.pointLabel.setText(point.label());
          this.elapsed.setText(point.elapsed().toPlainString());
          this.elapsedUnit.setValue(point.elapsedUnit());
          this.conversion.setText(
              point.conversion() == null ? "" : point.conversion().toPlainString());
          this.conversionUnit.setValue(point.conversionUnit());
          this.method.setValue(point.method());
          this.pointSource.setText(point.source());
          this.pointNote.setText(point.note());
          this.pointObservation.set(point.observation());
          this.pointMode.setText("Исправление пробы · исходная запись сохранится");
          this.loading = false;
          this.revealDetails();
          this.focusForm(0);
        });
  }

  private void editWorkup(final Workup workup) {
    if (workup == null) {
      return;
    }
    this.safe(
        () -> {
          if (this.workupDirty) {
            throw new IllegalArgumentException("Сначала запишите или сбросьте текущую обработку.");
          }
          this.loading = true;
          this.workupCorrection = workup.id();
          this.scope(this.workupStage, workup.stageId());
          this.workupTitle.setText(workup.title());
          this.workupNote.setText(workup.note());
          this.phases.getItems().setAll(workup.phases());
          this.workupSteps.getItems().setAll(workup.steps());
          this.workupLosses.getItems().setAll(workup.losses());
          this.workupMode.setText("Редактирование обработки · прежняя версия сохранится");
          this.loading = false;
          this.revealDetails();
          this.focusForm(1);
        });
  }

  private void editRun(final Purification run) {
    if (run == null) {
      return;
    }
    this.safe(
        () -> {
          if (this.runDirty) {
            throw new IllegalArgumentException("Сначала запишите или сбросьте текущую очистку.");
          }
          this.loading = true;
          this.runCorrection = run.id();
          this.scope(this.runStage, run.stageId());
          this.sample(this.inputSample, run.inputSampleId());
          this.runTitle.setText(run.title());
          this.purificationKind.setValue(run.kind());
          this.input.set(run.inputPlanned(), run.inputActual());
          this.stationary.setText(run.stationaryPhase());
          this.sorbent.set(run.sorbentMass(), null);
          if (run.sorbentMass() == null) {
            this.sorbent.unit.setValue(LaboratoryQuantityUnit.GRAM);
          }
          this.solventA.setText(run.solventA());
          this.solventB.setText(run.solventB());
          this.axis.setValue(run.axis());
          this.gradient.getItems().setAll(run.gradient());
          this.fractions.getItems().setAll(run.fractions());
          this.pools.getItems().setAll(run.pools());
          this.runLosses.getItems().setAll(run.losses());
          this.runSource.setText(run.source());
          this.runNote.setText(run.note());
          this.runObservation.set(run.observation());
          this.runMode.setText("Редактирование очистки · прежняя версия сохранится");
          this.loading = false;
          this.revealDetails();
          this.focusForm(2);
          this.showTotals();
        });
  }

  private <T> void dialog(
      final String title,
      final List<Node> fields,
      final Supplier<T> value,
      final Consumer<T> accept) {
    final Dialog<ButtonType> dialog = new Dialog<>();
    dialog.setTitle(title);
    if (this.getScene() != null) {
      dialog.initOwner(this.getScene().getWindow());
    }
    dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
    final Label error = note("");
    final VBox content = new VBox(10);
    content.getChildren().addAll(fields);
    content.getChildren().add(error);
    content.setPadding(new Insets(12));
    final ScrollPane scroll = scroll(content);
    scroll.setPrefViewportWidth(580);
    scroll.setPrefViewportHeight(Math.min(650, 120 + fields.size() * 65));
    dialog.getDialogPane().setContent(scroll);
    dialog
        .getDialogPane()
        .lookupButton(ButtonType.OK)
        .addEventFilter(
            ActionEvent.ACTION,
            event -> {
              try {
                final var result = value.get();
                accept.accept(result);
              } catch (final RuntimeException problem) {
                error.setText(message(problem));
                event.consume();
              }
            });
    LaboratoryDialogStyle.apply(dialog, dialog.getOwner());
    dialog.showAndWait();
  }

  private <T> void put(
      final TableView<T> table, final T before, final T after, final boolean workup) {
    if (before == null) {
      final int limit =
          table == this.phases ? 200 : table == this.gradient || table == this.pools ? 1000 : 2000;
      if (table.getItems().size() >= limit) {
        throw new IllegalArgumentException("В этой таблице поддерживается до " + limit + " строк.");
      }
      table.getItems().add(after);
    } else {
      table.getItems().set(table.getItems().indexOf(before), after);
    }
    if (workup) {
      this.workupDirty = true;
    } else {
      this.markRunDirty();
    }
  }

  private <T> void remove(final TableView<T> table, final boolean workup) {
    if (table.getSelectionModel().getSelectedItem() != null) {
      table.getItems().remove(table.getSelectionModel().getSelectedItem());
      if (workup) {
        this.workupDirty = true;
      } else {
        this.markRunDirty();
      }
    }
  }

  private <T> void move(final TableView<T> table, final int delta, final boolean workup) {
    final int i = table.getSelectionModel().getSelectedIndex(), next = i + delta;
    if (i >= 0 && next >= 0 && next < table.getItems().size()) {
      Collections.swap(table.getItems(), i, next);
      table.getSelectionModel().select(next);
      if (workup) {
        this.workupDirty = true;
      } else {
        this.markRunDirty();
      }
    }
  }

  private void phaseDialog(final Phase existing) {
    final TextField label = new TextField(existing == null ? "" : existing.label());
    final var kind =
        enumeration(PhaseKind.values(), existing == null ? PhaseKind.ORGANIC : existing.kind());
    this.dialog(
        "Фаза обработки",
        List.of(field("Название фазы", label), field("Тип", kind)),
        () ->
            new Phase(
                existing == null ? id("phase") : existing.id(), label.getText(), kind.getValue()),
        value -> {
          this.put(this.phases, existing, value, true);
          this.workupSteps.refresh();
        });
  }

  private void removePhase() {
    final var selected = this.phases.getSelectionModel().getSelectedItem();
    if (selected == null) {
      return;
    }
    if (this.workupSteps.getItems().stream()
        .anyMatch(
            s -> selected.id().equals(s.fromPhaseId()) || selected.id().equals(s.toPhaseId()))) {
      throw new IllegalArgumentException("Сначала измените операции, которые используют эту фазу.");
    }
    this.remove(this.phases, true);
  }

  private void workupStepDialog(final WorkupStep existing) {
    final var kind =
        enumeration(WorkupKind.values(), existing == null ? WorkupKind.EXTRACT : existing.kind());
    final ComboBox<Scope> from = new ComboBox<>(), to = new ComboBox<>();
    final Scope none = new Scope(null, "Не указана");
    for (final var box : List.of(from, to)) {
      box.getItems().add(none);
      this.phases.getItems().forEach(p -> box.getItems().add(new Scope(p.id(), p.label())));
      box.setValue(none);
    }
    if (existing != null) {
      from.getItems().stream()
          .filter(p -> Objects.equals(p.id(), existing.fromPhaseId()))
          .findFirst()
          .ifPresent(from::setValue);
      to.getItems().stream()
          .filter(p -> Objects.equals(p.id(), existing.toPhaseId()))
          .findFirst()
          .ifPresent(to::setValue);
    }
    final TextField material = new TextField(existing == null ? "" : existing.material()),
        repetitions =
            new TextField(existing == null ? "1" : Integer.toString(existing.repetitions())),
        source = new TextField(existing == null ? "" : existing.source());
    final TextArea note = area(3);
    note.setText(existing == null ? "" : existing.note());
    final QuantityPair amount = new QuantityPair();
    if (existing != null) {
      amount.set(existing.planned(), existing.actual());
    }
    final var observation =
        this.rowObservation(existing == null ? Observation.EMPTY : existing.observation(), null);
    this.dialog(
        "Операция обработки",
        List.of(
            field("Операция", kind),
            field("Из фазы", from),
            field("В фазу", to),
            field("Реагент / растворитель / материал", material),
            field("Число повторов", repetitions),
            field("Суммарное количество всех повторов · план / факт", amount.node()),
            field("Источник факта", source),
            observation,
            field("Методика / наблюдение", note)),
        () ->
            new WorkupStep(
                existing == null ? id("workup_step") : existing.id(),
                kind.getValue(),
                from.getValue().id(),
                to.getValue().id(),
                material.getText(),
                decimal(repetitions, true).intValueExact(),
                amount.planned(),
                amount.actual(),
                source.getText(),
                note.getText(),
                observation.value()),
        value -> this.put(this.workupSteps, existing, value, true));
  }

  private void lossDialog(final TableView<Loss> table, final boolean workup, final Loss existing) {
    final TextField label = new TextField(existing == null ? "" : existing.label()),
        source = new TextField(existing == null ? "" : existing.source());
    final QuantityPair quantity = new QuantityPair();
    if (existing != null) {
      quantity.set(existing.planned(), existing.actual());
    }
    final var observation =
        this.rowObservation(existing == null ? Observation.EMPTY : existing.observation(), null);
    this.dialog(
        "Учёт потери",
        List.of(
            field("Потеря / отведённый поток", label),
            field("План / указанное количество", quantity.node()),
            field("Источник / основание", source),
            observation,
            note(
                "Неизвестную потерю оставьте без фактического количества; разница баланса не подставляется автоматически.")),
        () ->
            new Loss(
                existing == null ? id("loss") : existing.id(),
                label.getText(),
                quantity.planned(),
                quantity.actual(),
                source.getText(),
                observation.value()),
        value -> this.put(table, existing, value, workup));
  }

  private void gradientDialog(final GradientPoint existing) {
    final TextField
        position = new TextField(existing == null ? "" : existing.position().toPlainString()),
        percent = new TextField(existing == null ? "" : existing.solventBPercent().toPlainString()),
        flow =
            new TextField(
                existing == null || existing.flowMlPerMinute() == null
                    ? ""
                    : existing.flowMlPerMinute().toPlainString());
    this.dialog(
        "Точка градиента",
        List.of(
            field("Положение, " + this.axis.getValue(), position),
            field("Доля растворителя B, %", percent),
            field("Поток, мл/мин (необязательно)", flow)),
        () -> {
          final var value =
              new GradientPoint(
                  decimal(position, true), decimal(percent, true), decimal(flow, false));
          if (this.gradient.getItems().stream()
              .anyMatch(
                  g ->
                      !Objects.equals(g, existing)
                          && g.position().compareTo(value.position()) == 0)) {
            throw new IllegalArgumentException("В этой позиции уже есть точка градиента.");
          }
          return value;
        },
        value -> {
          this.put(this.gradient, existing, value, false);
          this.gradient.getItems().sort(Comparator.comparing(GradientPoint::position));
        });
  }

  private void fractionDialog(final Fraction existing) {
    final TextField label = new TextField(existing == null ? "" : existing.label()),
        start =
            new TextField(
                existing == null || existing.start() == null
                    ? ""
                    : existing.start().toPlainString()),
        end =
            new TextField(
                existing == null || existing.end() == null ? "" : existing.end().toPlainString()),
        purity =
            new TextField(
                existing == null || existing.purityPercent() == null
                    ? ""
                    : existing.purityPercent().toPlainString()),
        source = new TextField(existing == null ? "" : existing.source());
    final TextArea note = area(3);
    note.setText(existing == null ? "" : existing.note());
    final QuantityPair amount = new QuantityPair();
    if (existing != null) {
      amount.set(existing.planned(), existing.actual());
    }
    final ComboBox<SampleChoice> sample = new ComboBox<>();
    this.samples(sample);
    if (existing != null) {
      this.sample(sample, existing.sampleId());
    }
    final var observation =
        this.rowObservation(existing == null ? Observation.EMPTY : existing.observation(), sample);
    this.dialog(
        "Фракция",
        List.of(
            field("Название / номер", label),
            field("Интервал сбора, " + this.axis.getValue(), new HBox(8, start, note("—"), end)),
            field("Количество · план / факт", amount.node()),
            field("Указанная чистота, % (необязательно)", purity),
            field("Физический образец", sample),
            field("Источник / основание", source),
            observation,
            field("Наблюдение / анализ", note)),
        () ->
            new Fraction(
                existing == null ? id("fraction") : existing.id(),
                label.getText(),
                decimal(start, false),
                decimal(end, false),
                amount.planned(),
                amount.actual(),
                decimal(purity, false),
                sample.getValue().id(),
                source.getText(),
                note.getText(),
                observation.value()),
        value -> {
          this.put(this.fractions, existing, value, false);
          this.pools.refresh();
        });
  }

  private void removeFraction() {
    final var value = this.fractions.getSelectionModel().getSelectedItem();
    if (value == null) {
      return;
    }
    if (this.pools.getItems().stream().anyMatch(p -> p.fractionIds().contains(value.id()))) {
      throw new IllegalArgumentException("Сначала измените объединение, в которое входит фракция.");
    }
    this.remove(this.fractions, false);
  }

  private void poolDialog(final Pool existing) {
    final Set<String> used = new HashSet<>();
    this.pools.getItems().stream()
        .filter(p -> !Objects.equals(p, existing))
        .forEach(p -> used.addAll(p.fractionIds()));
    final ListView<Fraction> choices = new ListView<>();
    choices.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    choices
        .getItems()
        .setAll(this.fractions.getItems().stream().filter(f -> !used.contains(f.id())).toList());
    choices.setPrefHeight(200);
    choices.setCellFactory(
        v ->
            new ListCell<>() {
              protected void updateItem(Fraction f, boolean empty) {
                super.updateItem(f, empty);
                this.setText(empty || f == null ? null : f.label() + " · " + quantity(f.actual()));
              }
            });
    if (existing != null) {
      for (int i = 0; i < choices.getItems().size(); i++) {
        if (existing.fractionIds().contains(choices.getItems().get(i).id())) {
          choices.getSelectionModel().select(i);
        }
      }
    }
    final TextField label = new TextField(existing == null ? "" : existing.label()),
        actual =
            new TextField(
                existing == null || existing.actual() == null
                    ? ""
                    : existing.actual().value().toPlainString()),
        source = new TextField(existing == null ? "" : existing.source());
    final var unit =
        enumeration(
            LaboratoryQuantityUnit.values(),
            existing == null || existing.actual() == null
                ? LaboratoryQuantityUnit.GRAM
                : existing.actual().unit());
    final ComboBox<SampleChoice> sample = new ComboBox<>();
    this.samples(sample);
    if (existing != null) {
      this.sample(sample, existing.sampleId());
    }
    final TextArea note = area(3);
    note.setText(existing == null ? "" : existing.note());
    final var observation =
        this.rowObservation(existing == null ? Observation.EMPTY : existing.observation(), sample);
    this.dialog(
        "Объединение фракций",
        List.of(
            field("Название объединения", label),
            field("Выберите фракции (Ctrl / Shift)", choices),
            field("Количество после объединения (необязательно)", new HBox(6, actual, unit)),
            field("Физический образец", sample),
            field("Источник / основание", source),
            observation,
            field("Примечание", note)),
        () ->
            new Pool(
                existing == null ? id("pool") : existing.id(),
                label.getText(),
                choices.getSelectionModel().getSelectedItems().stream().map(Fraction::id).toList(),
                quantity(actual, unit.getValue()),
                sample.getValue().id(),
                source.getText(),
                note.getText(),
                observation.value()),
        value -> this.put(this.pools, existing, value, false));
  }

  private OperationsObservationPane rowObservation(
      final Observation value, final ComboBox<SampleChoice> sample) {
    final var pane =
        new OperationsObservationPane(
            () -> this.expected.experiment(),
            () -> sample == null ? null : sample.getValue().id(),
            sample != null,
            () -> {});
    pane.set(value);
    if (sample != null) {
      sample.valueProperty().addListener(o -> pane.sampleChanged());
    }
    return pane;
  }

  private void showTotals() {
    this.balance.setText("Проверяю текущие количества…");
    this.safe(
        () -> {
          final var totals = LaboratoryReactionOperationsService.totals(this.runValue());
          final String unit = totals.unit() == null ? "" : " " + totals.unit();
          this.balance.setText(
              "Указанная загрузка: "
                  + number(totals.input())
                  + unit
                  + "\nСопоставимые учтённые выходящие количества: "
                  + number(totals.knownOutputs())
                  + unit
                  + "\nУказанные потери: "
                  + number(totals.knownLosses())
                  + unit
                  + "\nНеучтённая разница: "
                  + (totals.unaccounted() == null
                      ? "не определена"
                      : totals.unaccounted().toPlainString() + unit)
                  + "\n\n"
                  + String.join(
                      "\n",
                      totals.pools().stream()
                          .map(
                              p ->
                                  p.label()
                                      + ": "
                                      + quantity(p.quantity())
                                      + (p.recorded()
                                          ? " · "
                                              + LaboratoryReactionOperationsService.originLabel(
                                                  p.origin())
                                          : " · сумма исходных фракций"))
                          .toList())
                  + "\n\n"
                  + String.join("\n", totals.warnings()));
        });
  }

  private void showHistory() {
    final var values = this.expected.experiment().operations();
    final List<Revision> records = new ArrayList<>();
    records.addAll(values.timePoints());
    records.addAll(values.workups());
    records.addAll(values.purifications());
    records.sort(Comparator.comparing(Revision::recordedAt).reversed());
    final ComboBox<Revision> choice = new ComboBox<>(FXCollections.observableArrayList(records));
    choice.setMaxWidth(Double.MAX_VALUE);
    choice.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(final Revision value) {
            if (value == null) {
              return "";
            }
            final String title =
                value instanceof TimePoint p
                    ? "Проба: " + p.label()
                    : value instanceof Workup w
                        ? "Обработка: " + w.title()
                        : "Очистка: " + ((Purification) value).title();
            return title
                + " · "
                + DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
                    .withZone(ZoneId.systemDefault())
                    .format(value.recordedAt());
          }

          public Revision fromString(final String value) {
            return null;
          }
        });
    final TextArea text = area(24);
    text.setEditable(false);
    choice
        .valueProperty()
        .addListener((o, before, value) -> text.setText(value == null ? "" : historyText(value)));
    if (!records.isEmpty()) {
      choice.setValue(records.getFirst());
    }
    this.dialog(
        "История контроля и очистки",
        List.of(
            note(
                "Каждая версия сохраняет исходные значения, происхождение и ссылки на анализы. Выберите запись для полного просмотра."),
            choice,
            text),
        () -> Boolean.TRUE,
        value -> {});
  }

  private static String historyText(final Revision record) {
    final StringBuilder out = new StringBuilder();
    out.append("Стадия: ")
        .append(record.stageTitle().isBlank() ? "Общая запись" : record.stageTitle())
        .append("\nЗаписано: ")
        .append(record.recordedAt())
        .append("\nЗапись: ")
        .append(record.id())
        .append("\n");
    if (record.correctsId() != null) {
      out.append("Исправляет: ").append(record.correctsId()).append("\n");
    }
    if (record instanceof TimePoint p) {
      out.append(observationText(p.observation()))
          .append("\nПроба: ")
          .append(p.label())
          .append("\nВремя: ")
          .append(p.elapsed())
          .append(" ")
          .append(p.elapsedUnit())
          .append("\nМетод: ")
          .append(p.method())
          .append("\nКонверсия: ")
          .append(
              p.conversion() == null
                  ? "не задана"
                  : p.conversion().toPlainString() + " " + p.conversionUnit())
          .append("\nОбразец: ")
          .append(Objects.toString(p.sampleId(), "не связан"))
          .append("\nИсточник: ")
          .append(p.source())
          .append("\nНаблюдение: ")
          .append(p.note());
    } else if (record instanceof Workup w) {
      out.append("\nОбработка: ")
          .append(w.title())
          .append("\n")
          .append(w.note())
          .append("\n\nФазы:\n");
      final Map<String, String> phases = new HashMap<>();
      w.phases()
          .forEach(
              p -> {
                phases.put(p.id(), p.label());
                out.append(p.label()).append(" · ").append(p.kind()).append("\n");
              });
      out.append("\nОперации в порядке выполнения:\n");
      w.steps()
          .forEach(
              s ->
                  out.append(observationText(s.observation()))
                      .append(s.kind())
                      .append(" · ")
                      .append(s.material())
                      .append(" · повторов: ")
                      .append(s.repetitions())
                      .append("\n")
                      .append(phases.getOrDefault(s.fromPhaseId(), "не задана"))
                      .append(" → ")
                      .append(phases.getOrDefault(s.toPhaseId(), "не задана"))
                      .append("\nПлан (суммарно): ")
                      .append(quantity(s.planned()))
                      .append(" · факт: ")
                      .append(quantity(s.actual()))
                      .append("\nИсточник: ")
                      .append(s.source())
                      .append("\n")
                      .append(s.note())
                      .append("\n\n"));
      historyLosses(out, w.losses());
    } else if (record instanceof Purification r) {
      out.append(observationText(r.observation()))
          .append("\nОчистка: ")
          .append(r.title())
          .append(" · ")
          .append(r.kind())
          .append("\nЗагрузка, план: ")
          .append(quantity(r.inputPlanned()))
          .append(" · факт: ")
          .append(quantity(r.inputActual()))
          .append("\nИсходный образец: ")
          .append(Objects.toString(r.inputSampleId(), "не связан"))
          .append("\nИсточник: ")
          .append(r.source())
          .append("\nНеподвижная фаза: ")
          .append(r.stationaryPhase())
          .append(" · сорбент: ")
          .append(quantity(r.sorbentMass()))
          .append("\nA: ")
          .append(r.solventA())
          .append("\nB: ")
          .append(r.solventB())
          .append("\n")
          .append(r.note())
          .append("\n\nГрадиент:\n");
      r.gradient()
          .forEach(
              g ->
                  out.append(g.position())
                      .append(" ")
                      .append(r.axis())
                      .append(" · B ")
                      .append(g.solventBPercent())
                      .append(" % · поток ")
                      .append(number(g.flowMlPerMinute()))
                      .append(" мл/мин\n"));
      out.append("\nФракции:\n");
      final Map<String, String> fractions = new HashMap<>();
      r.fractions()
          .forEach(
              f -> {
                fractions.put(f.id(), f.label());
                out.append(observationText(f.observation()))
                    .append(f.label())
                    .append(" · интервал ")
                    .append(number(f.start()))
                    .append("–")
                    .append(number(f.end()))
                    .append(" ")
                    .append(r.axis())
                    .append("\nПлан: ")
                    .append(quantity(f.planned()))
                    .append(" · факт: ")
                    .append(quantity(f.actual()))
                    .append(" · чистота: ")
                    .append(
                        f.purityPercent() == null
                            ? "не задана"
                            : f.purityPercent().toPlainString() + " %")
                    .append("\nОбразец: ")
                    .append(Objects.toString(f.sampleId(), "не связан"))
                    .append("\nИсточник: ")
                    .append(f.source())
                    .append("\n")
                    .append(f.note())
                    .append("\n\n");
              });
      out.append("Объединения:\n");
      r.pools()
          .forEach(
              p ->
                  out.append(observationText(p.observation()))
                      .append(p.label())
                      .append(" ← ")
                      .append(
                          String.join(", ", p.fractionIds().stream().map(fractions::get).toList()))
                      .append("\nУказанное количество: ")
                      .append(quantity(p.actual()))
                      .append("\nОбразец: ")
                      .append(Objects.toString(p.sampleId(), "не связан"))
                      .append("\nИсточник: ")
                      .append(p.source())
                      .append("\n")
                      .append(p.note())
                      .append("\n\n"));
      historyLosses(out, r.losses());
    }
    return out.toString();
  }

  private static String observationText(final Observation value) {
    return "\nПроисхождение: "
        + LaboratoryReactionOperationsService.originLabel(value.origin())
        + (value.analyses().isEmpty()
            ? ""
            : "\nСвязанные версии: "
                + String.join(
                    ", ",
                    value.analyses().stream()
                        .map(
                            r ->
                                (r.kind() == AnalysisKind.LEGACY ? "Анализ" : "Аналитика")
                                    + " ["
                                    + r.id()
                                    + "]")
                        .toList()))
        + "\n";
  }

  private static void historyLosses(final StringBuilder out, final List<Loss> losses) {
    out.append("\nПотери / отведённые потоки:\n");
    losses.forEach(
        l ->
            out.append(observationText(l.observation()))
                .append(l.label())
                .append(" · план: ")
                .append(quantity(l.planned()))
                .append(" · факт: ")
                .append(quantity(l.actual()))
                .append("\nИсточник: ")
                .append(l.source())
                .append("\n"));
  }

  /** Draft contains raw fields and typed local table buffers; nothing becomes an experimental fact on recovery. */
  void draft(final Properties data, final boolean restore) {
    if (!restore && this.conflictDraft != null) {
      this.conflictDraft.forEach(data::put);
      return;
    }
    final Map<String, Control> fields = new LinkedHashMap<>();
    fields.put("pointLabel", this.pointLabel);
    fields.put("elapsed", this.elapsed);
    fields.put("conversion", this.conversion);
    fields.put("elapsedUnit", this.elapsedUnit);
    fields.put("conversionUnit", this.conversionUnit);
    fields.put("method", this.method);
    fields.put("pointSource", this.pointSource);
    fields.put("pointNote", this.pointNote);
    fields.put("workupTitle", this.workupTitle);
    fields.put("workupNote", this.workupNote);
    fields.put("runTitle", this.runTitle);
    fields.put("kind", this.purificationKind);
    fields.put("axis", this.axis);
    fields.put("stationary", this.stationary);
    fields.put("solventA", this.solventA);
    fields.put("solventB", this.solventB);
    fields.put("runSource", this.runSource);
    fields.put("runNote", this.runNote);
    fields.put("inputPlan", this.input.planned);
    fields.put("inputFact", this.input.actual);
    fields.put("inputUnit", this.input.unit);
    fields.put("inputActualUnit", this.input.actualUnit);
    fields.put("sorbent", this.sorbent.planned);
    fields.put("sorbentUnit", this.sorbent.unit);
    if (!restore) {
      data.setProperty("operations.source", new LaboratoryFileCodec().write(this.expected));
      data.setProperty("operations.filter", Objects.toString(this.filter.getValue().id(), ""));
      data.setProperty(
          "operations.tab", Integer.toString(this.tabs.getSelectionModel().getSelectedIndex()));
      data.setProperty("operations.pointDirty", Boolean.toString(this.pointDirty));
      data.setProperty("operations.workupDirty", Boolean.toString(this.workupDirty));
      data.setProperty("operations.runDirty", Boolean.toString(this.runDirty));
      data.setProperty("operations.conflict", Boolean.toString(this.conflict));
      for (final var entry :
          Map.of(
                  "pointStage",
                  this.pointStage,
                  "workupStage",
                  this.workupStage,
                  "runStage",
                  this.runStage)
              .entrySet()) {
        data.setProperty(
            "operations." + entry.getKey(), Objects.toString(entry.getValue().getValue().id(), ""));
      }
      data.setProperty(
          "operations.pointSample", Objects.toString(this.pointSample.getValue().id(), ""));
      data.setProperty(
          "operations.inputSample", Objects.toString(this.inputSample.getValue().id(), ""));
      data.setProperty("operations.pointCorrection", Objects.toString(this.pointCorrection, ""));
      data.setProperty("operations.workupCorrection", Objects.toString(this.workupCorrection, ""));
      data.setProperty("operations.runCorrection", Objects.toString(this.runCorrection, ""));
      OperationsDraft.observation(
          data, "operations.pointObservation.", this.pointObservation.rawValue());
      OperationsDraft.observation(
          data, "operations.runObservation.", this.runObservation.rawValue());
      LaboratoryFormDraft.fields(data, "operations", false, fields);
      OperationsDraft.write(
          data,
          this.phases.getItems(),
          this.workupSteps.getItems(),
          this.workupLosses.getItems(),
          this.gradient.getItems(),
          this.fractions.getItems(),
          this.pools.getItems(),
          this.runLosses.getItems());
      return;
    }
    this.loading = true;
    try {
      LaboratoryFormDraft.fields(data, "operations", true, fields);
      this.filter.setValue(
          this.filter.getItems().stream()
              .filter(
                  s ->
                      Objects.toString(s.id(), "")
                          .equals(data.getProperty("operations.filter", "*")))
              .findFirst()
              .orElse(ALL));
      for (final var entry :
          Map.of(
                  "pointStage",
                  this.pointStage,
                  "workupStage",
                  this.workupStage,
                  "runStage",
                  this.runStage)
              .entrySet()) {
        final String id = data.getProperty("operations." + entry.getKey(), "");
        final var selected =
            entry.getValue().getItems().stream()
                .filter(s -> Objects.toString(s.id(), "").equals(id))
                .findFirst();
        if (selected.isEmpty()) {
          this.conflict = true;
        }
        entry.getValue().setValue(selected.orElse(GENERAL));
      }
      this.sample(this.pointSample, nullable(data.getProperty("operations.pointSample", "")));
      this.sample(this.inputSample, nullable(data.getProperty("operations.inputSample", "")));
      this.pointCorrection = nullable(data.getProperty("operations.pointCorrection", ""));
      this.workupCorrection = nullable(data.getProperty("operations.workupCorrection", ""));
      this.runCorrection = nullable(data.getProperty("operations.runCorrection", ""));
      this.pointDirty = Boolean.parseBoolean(data.getProperty("operations.pointDirty"));
      this.workupDirty = Boolean.parseBoolean(data.getProperty("operations.workupDirty"));
      this.runDirty = Boolean.parseBoolean(data.getProperty("operations.runDirty"));
      this.pointObservation.set(OperationsDraft.observation(data, "operations.pointObservation."));
      this.runObservation.set(OperationsDraft.observation(data, "operations.runObservation."));
      OperationsDraft.read(
          data,
          this.phases,
          this.workupSteps,
          this.workupLosses,
          this.gradient,
          this.fractions,
          this.pools,
          this.runLosses);
      this.conflict =
          this.conflict
              || Boolean.parseBoolean(data.getProperty("operations.conflict"))
              || !new LaboratoryFileCodec()
                  .write(this.expected)
                  .equals(data.getProperty("operations.source"));
      final int tab = Integer.parseInt(data.getProperty("operations.tab", "0"));
      this.tabs.getSelectionModel().select(Math.max(0, Math.min(2, tab)));
      this.pointMode.setText(
          this.pointCorrection == null
              ? "Новая контрольная проба"
              : "Исправление пробы · исходная запись сохранится");
      this.workupMode.setText(
          this.workupCorrection == null
              ? "Новая обработка"
              : "Редактирование обработки · прежняя версия сохранится");
      this.runMode.setText(
          this.runCorrection == null
              ? "Новая очистка"
              : "Редактирование очистки · прежняя версия сохранится");
      this.revealDetails();
      this.refresh();
      if (this.runDirty) {
        this.markRunDirty();
      }
      if (this.conflict) {
        this.conflictDraft = new Properties();
        data.stringPropertyNames().stream()
            .filter(k -> k.startsWith("operations."))
            .forEach(k -> this.conflictDraft.setProperty(k, data.getProperty(k)));
        this.conflictDraft.setProperty("operations.conflict", "true");
        this.status.setText(
            "Восстановлен ввод другой версии опыта. Черновик сохранён отдельно; запись заблокирована до проверки.");
      }
    } catch (final RuntimeException problem) {
      this.conflict = true;
      this.conflictDraft = new Properties();
      data.stringPropertyNames().stream()
          .filter(k -> k.startsWith("operations."))
          .forEach(k -> this.conflictDraft.setProperty(k, data.getProperty(k)));
      this.conflictDraft.setProperty("operations.conflict", "true");
      this.status.setText("Черновик сохранён, но не может быть записан: " + message(problem));
    } finally {
      this.loading = false;
    }
  }

  private static String nullable(final String text) {
    return text.isEmpty() ? null : text;
  }
}

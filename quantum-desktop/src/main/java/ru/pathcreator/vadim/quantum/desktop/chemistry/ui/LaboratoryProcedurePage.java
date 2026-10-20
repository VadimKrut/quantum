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
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryProcedure.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryFileCodec;

/** A local bench workflow: editable plans, frozen execution records and restart-safe clocks. */
final class LaboratoryProcedurePage extends LaboratoryComputationPage {
  private final LaboratoryEditor editor;
  private final Runnable save;
  private LaboratoryDrawing expected;
  private final ListView<Step> steps = new ListView<>();
  private final ComboBox<Step> stepSelector = new ComboBox<>();
  private final Label timelineScope =
      note("Хронология выбранного шага. Исправления добавляются отдельной записью.");
  private final ListView<Event> events = new ListView<>();
  private final ComboBox<String> filter = new ComboBox<>(),
      stage = new ComboBox<>(),
      material = new ComboBox<>();
  private final ComboBox<Kind> kind = new ComboBox<>();
  private final ComboBox<LaboratoryQuantityUnit> actualUnit = new ComboBox<>();
  private final TextField title = new TextField(),
      portion = new TextField("100"),
      duration = new TextField(),
      temperature = new TextField();
  private final TextArea instruction = new TextArea(),
      observation = new TextArea(),
      bulk = new TextArea(),
      eventDetail = new TextArea();
  private final LaboratoryDecimalField actual = new LaboratoryDecimalField();
  private final Label count = note(""),
      stepTitle = note(""),
      narrative = note(""),
      timer = note(""),
      progressText = note(""),
      warning = note(""),
      correction = note("");
  private final TabPane tabs = new TabPane();
  private final Tab planTab = new Tab("План шага"),
      runTab = new Tab("Выполнение"),
      historyTab = new Tab("История записей");
  private final TitledPane advanced = new TitledPane(),
      measured = new TitledPane(),
      quickNotes = new TitledPane(),
      eventDetails = new TitledPane();
  private final Label nextStepText = note(""), recordedAmount = note("");
  private final Button editStep, nextStep;
  private final VBox empty = new VBox(12),
      form = new VBox(12),
      execution = new VBox(12),
      discard = new VBox(8);
  private final Button start,
      complete,
      recordComplete,
      skip,
      apply,
      duplicate,
      remove,
      up,
      down,
      writeNote,
      correct;
  private final Timeline clock;
  private boolean loading, formDirty, conflict, allEvents;
  private String editingId, correctionId;
  private Properties conflictDraft;
  private String durationDisplay = "";
  private long displayedSeconds;
  private static final String ALL = "Все стадии", NONE = "Без привязки";
  private final Map<String, String> stageNames = new LinkedHashMap<>(),
      materialNames = new LinkedHashMap<>();

  LaboratoryProcedurePage(
      final LaboratoryEditor editor,
      final String stageId,
      final Runnable save,
      final Runnable close) {
    super("Методика и проведение", close);
    this.editor = editor;
    this.save = save;
    this.expected = editor.document();
    this.setId("chemistry-procedure-page");
    this.stageNames.put(NONE, null);
    this.materialNames.put(NONE, null);
    for (final var s : this.expected.experiment().stages()) {
      this.stageNames.put(uniqueLabel(this.stageNames, s.title()), s.id());
    }
    for (final var c : this.expected.experiment().components()) {
      this.materialNames.put(uniqueLabel(this.materialNames, c.molecule().displayName()), c.id());
    }
    this.filter.getItems().add(ALL);
    this.filter.getItems().addAll(this.stageNames.keySet());
    this.filter.setValue(stageId == null ? ALL : stageLabel(stageId, ALL));
    this.filter.setId("procedure-filter");
    this.filter.setMaxWidth(Double.MAX_VALUE);
    this.stage.getItems().setAll(this.stageNames.keySet());
    this.stage.setValue(stageLabel(stageId, NONE));
    this.stage.setId("procedure-stage");
    this.material.getItems().setAll(this.materialNames.keySet());
    this.material.setValue(NONE);
    this.material.setId("procedure-material");
    this.kind.getItems().setAll(Kind.values());
    this.kind.setValue(Kind.OTHER);
    this.kind.setId("procedure-kind");
    this.actualUnit.getItems().setAll(LaboratoryQuantityUnit.values());
    this.actualUnit.setValue(LaboratoryQuantityUnit.GRAM);
    this.actualUnit.setId("procedure-actual-unit");
    this.title.setId("procedure-title");
    this.title.setPromptText("Например, добавление раствора основания");
    this.instruction.setId("procedure-instruction");
    this.instruction.setPromptText("Порядок добавления, атмосфера и другие условия вашей методики");
    this.instruction.setPrefRowCount(4);
    this.instruction.setWrapText(true);
    this.portion.setId("procedure-portion");
    this.duration.setId("procedure-duration");
    this.temperature.setId("procedure-temperature");
    this.duration.setPromptText("Не задано");
    this.temperature.setPromptText("Не задана");
    this.observation.setId("procedure-observation");
    this.observation.setWrapText(true);
    this.observation.setPrefRowCount(3);
    this.observation.setPromptText("Наблюдение или пояснение к выполненному действию");
    this.actual.setId("procedure-actual");
    this.actual.setPromptText("Только измеренное количество");
    this.eventDetail.setEditable(false);
    this.eventDetail.setWrapText(true);
    this.eventDetail.setPrefRowCount(8);
    this.eventDetail.setId("procedure-event-detail");
    this.steps.setId("procedure-steps");
    this.steps.setPlaceholder(
        note("Добавьте шаги своей методики\nили подготовьте список загрузки."));
    this.steps.setCellFactory(
        list ->
            new ListCell<>() {
              {
                this.setWrapText(true);
                this.prefWidthProperty().bind(list.widthProperty().subtract(24));
                this.setMaxWidth(USE_PREF_SIZE);
              }

              protected void updateItem(final Step s, final boolean empty) {
                super.updateItem(s, empty);
                this.setText(null);
                this.setGraphic(null);
                this.setTooltip(null);
                if (empty || s == null) {
                  return;
                }
                final int n = editor.document().experiment().procedure().steps().indexOf(s) + 1;
                final Label name = note(n + ". " + s.title());
                name.getStyleClass().add("procedure-step-name");
                final Label state = note(stateText(state(s.id())) + " · " + s.kind());
                state.getStyleClass().add("desk-panel-subtitle");
                final VBox row = new VBox(5, name, state);
                row.setMinWidth(0);
                this.setGraphic(row);
                this.setTooltip(new Tooltip(s.title()));
              }
            });
    this.steps
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (o, old, selected) -> {
              if (this.loading) {
                return;
              }
              if (this.pending()) {
                this.loading = true;
                this.steps.getSelectionModel().select(old);
                this.loading = false;
                this.status.setText("Сначала запишите или сбросьте введённые изменения.");
                return;
              }
              this.show(selected);
            });
    LaboratoryComponentNaming.choices(this.filter, v -> v);
    LaboratoryComponentNaming.choices(this.stage, v -> v);
    LaboratoryComponentNaming.choices(this.material, v -> v);
    this.stepSelector.setItems(this.steps.getItems());
    this.stepSelector.setMaxWidth(Double.MAX_VALUE);
    this.stepSelector.setId("procedure-step-selector");
    this.stepSelector.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(Step step) {
            return step == null
                ? "Выберите шаг"
                : (editor.document().experiment().procedure().steps().indexOf(step) + 1)
                    + ". "
                    + step.title()
                    + " · "
                    + stateText(state(step.id()));
          }

          public Step fromString(String value) {
            return null;
          }
        });
    this.stepSelector
        .valueProperty()
        .addListener(
            (o, before, after) -> {
              if (this.loading) {
                return;
              }
              if (this.pending()) {
                this.loading = true;
                this.stepSelector.setValue(before);
                this.loading = false;
                this.status.setText("Сначала запишите или сбросьте текущий ввод.");
                return;
              }
              this.steps.getSelectionModel().select(after);
            });
    LaboratoryComponentNaming.choices(
        this.stepSelector, s -> this.stepSelector.getConverter().toString(s));
    this.apply = this.button("Записать шаг", "apply", this::applyStep);
    this.apply.getStyleClass().add("laboratory-primary-action");
    this.duplicate = this.button("Повторить шаг", "duplicate", this::duplicate);
    this.remove = this.button("Удалить", "remove", this::remove);
    this.up = this.button("↑", "up", () -> this.move(-1));
    this.up.setTooltip(new Tooltip("Переместить шаг выше"));
    this.down = this.button("↓", "down", () -> this.move(1));
    this.down.setTooltip(new Tooltip("Переместить шаг ниже"));
    this.start = this.button("Начать шаг", "start", () -> this.execute(Action.START));
    this.start.getStyleClass().add("laboratory-primary-action");
    this.complete = this.button("Завершить шаг", "complete", () -> this.execute(Action.COMPLETE));
    this.complete.getStyleClass().add("laboratory-primary-action");
    this.recordComplete =
        this.button(
            "Отметить выполненным", "record-complete", () -> this.execute(Action.RECORD_COMPLETE));
    this.recordComplete.setTooltip(
        new Tooltip(
            "Для уже проведённого шага: сохранить факт выполнения без запуска таймера. Время проведения не будет выдумано."));
    this.skip = this.button("Пропустить…", "skip", () -> this.execute(Action.SKIP));
    this.writeNote =
        this.button(
            "Записать наблюдение",
            "note",
            () -> this.execute(this.correctionId == null ? Action.OBSERVE : Action.CORRECT));
    this.correct = this.button("Добавить исправление", "correct", this::beginCorrection);
    this.editStep = this.button("Изменить шаг…", "edit", this::editSelected);
    this.nextStep = this.button("К следующему шагу", "next", this::next);
    this.nextStep.getStyleClass().add("laboratory-primary-action");
    final MenuButton stepActions =
        new MenuButton(
            "Действия",
            null,
            item(this.duplicate, "Повторить шаг"),
            item(this.up, "Переместить выше"),
            item(this.down, "Переместить ниже"),
            new SeparatorMenuItem(),
            item(this.remove, "Удалить шаг"));
    stepActions.setId("procedure-step-actions");
    final FlowPane tools =
        new FlowPane(
            6, 6, this.button("Добавить шаг", "new", () -> this.begin(Kind.OTHER)), stepActions);
    final Label planHeading = note("План методики");
    planHeading.getStyleClass().add("desk-panel-heading");
    this.count.getStyleClass().add("desk-panel-subtitle");
    final VBox left =
        new VBox(10, planHeading, this.filter, this.count, this.steps, this.stepSelector, tools);
    left.getStyleClass().add("procedure-plan-sidebar");
    left.setPadding(new Insets(0, 14, 0, 0));
    left.setMinWidth(240);
    left.setPrefWidth(300);
    left.setMaxWidth(360);
    VBox.setVgrow(this.steps, Priority.ALWAYS);
    this.advanced.setText("Стадия и доля загрузки");
    this.advanced.setId("procedure-advanced");
    this.advanced.setExpanded(false);
    this.advanced.setAnimated(false);
    this.advanced.setContent(
        new VBox(
            10,
            new FlowPane(
                12,
                10,
                field("Стадия", this.stage),
                field("Доля плановой загрузки, %", this.portion)),
            note(
                "Количество незапущенного шага следует плану компонента. Уже проведённые действия сохраняют записанный снимок.")));
    this.form
        .getChildren()
        .setAll(
            field("Название шага", this.title),
            field("Инструкция", this.instruction),
            new FlowPane(12, 10, field("Операция", this.kind), field("Компонент", this.material)),
            new FlowPane(
                12,
                10,
                field("Продолжительность, мин", this.duration),
                field("Температура, °C", this.temperature)),
            this.advanced,
            new FlowPane(
                8,
                6,
                this.apply,
                this.button("Отменить редактирование", "reset", this::resetForm)));
    this.form.setPadding(new Insets(16));
    this.form.getStyleClass().add("desk-panel");
    final FlowPane quick = new FlowPane(6, 6);
    for (final String text :
        List.of("Выпал осадок", "Изменился цвет", "Отобрана проба", "Добавление закончено")) {
      quick
          .getChildren()
          .add(
              this.button(
                  text,
                  "quick-" + quick.getChildren().size(),
                  () -> {
                    this.observation.setText(
                        this.observation.getText().isBlank()
                            ? text
                            : this.observation.getText() + "\n" + text);
                    this.observation.requestFocus();
                  }));
    }
    this.timer.setId("procedure-timer");
    this.timer.setStyle("-fx-font-size: 25px; -fx-font-weight: bold;");
    this.quickNotes.setText("Быстрые наблюдения");
    this.quickNotes.setContent(quick);
    this.quickNotes.setExpanded(false);
    this.quickNotes.setAnimated(false);
    this.quickNotes.setId("procedure-quick-notes");
    this.measured.setText("Внести фактическое количество");
    this.measured.setId("procedure-measured");
    this.measured.setExpanded(false);
    this.measured.setAnimated(false);
    this.measured.setContent(
        new VBox(
            8,
            LaboratoryQuantityFields.row(this.actual, this.actualUnit),
            note(
                "Сохранится при завершении шага или вместе с наблюдением. Это измерение данного действия; склад и общая загрузка изменяются отдельно.")));
    this.recordedAmount.setId("procedure-recorded-amount");
    this.nextStepText.setId("procedure-next-step");
    final Label actionHeading = note("Действие и время");
    actionHeading.getStyleClass().add("desk-panel-heading");
    final Label noteHeading = note("Наблюдение по шагу");
    noteHeading.getStyleClass().add("desk-panel-heading");
    final VBox actionPanel =
        new VBox(
            12,
            actionHeading,
            this.timer,
            new FlowPane(
                8, 6, this.start, this.complete, this.recordComplete, this.nextStep, this.skip),
            note("Проводите сейчас — начните шаг. Уже сделали — отметьте выполненным без таймера."),
            this.nextStepText,
            this.recordedAmount,
            this.measured);
    final VBox notePanel =
        new VBox(
            12,
            noteHeading,
            note(
                "Запись связана с выбранным шагом и видна в общем журнале опыта. Начинать шаг для наблюдения не требуется."),
            this.observation,
            this.correction,
            new FlowPane(
                8, 6, this.writeNote, this.button("Очистить ввод", "clear-note", this::clearNote)),
            this.quickNotes);
    actionPanel.getStyleClass().add("desk-panel");
    notePanel.getStyleClass().add("desk-panel");
    actionPanel.setMinWidth(0);
    notePanel.setMinWidth(0);
    final GridPane work = new GridPane();
    work.getStyleClass().add("desk-two-column");
    work.setId("procedure-work-panels");
    work.setHgap(16);
    work.setVgap(16);
    work.add(actionPanel, 0, 0);
    work.add(notePanel, 1, 0);
    final ColumnConstraints actionColumn = new ColumnConstraints(),
        noteColumn = new ColumnConstraints();
    actionColumn.setMinWidth(0);
    noteColumn.setMinWidth(0);
    actionColumn.setHgrow(Priority.ALWAYS);
    noteColumn.setHgrow(Priority.ALWAYS);
    work.getColumnConstraints().setAll(actionColumn, noteColumn);
    final Runnable fitWork =
        () -> {
          final boolean wide = this.execution.getWidth() >= 850;
          GridPane.setColumnIndex(notePanel, wide ? 1 : 0);
          GridPane.setRowIndex(notePanel, wide ? 0 : 1);
          actionColumn.setPercentWidth(wide ? 44 : 100);
          noteColumn.setPercentWidth(wide ? 56 : 0);
        };
    this.execution.widthProperty().addListener(o -> fitWork.run());
    fitWork.run();
    this.execution.getChildren().setAll(work);
    this.execution.setPadding(new Insets(14, 0, 0, 0));
    this.events.setId("procedure-events");
    this.events.setPlaceholder(note("Действия появятся после начала работы."));
    this.events.setPrefHeight(220);
    this.events.setCellFactory(
        list ->
            new ListCell<>() {
              {
                this.setWrapText(true);
                this.prefWidthProperty().bind(list.widthProperty().subtract(24));
                this.setMaxWidth(USE_PREF_SIZE);
              }

              protected void updateItem(final Event e, final boolean empty) {
                super.updateItem(e, empty);
                this.setText(
                    empty || e == null
                        ? null
                        : time(e.recordedAt())
                            + " · "
                            + actionText(e.action())
                            + "\n"
                            + e.snapshot().title()
                            + (e.text().isBlank() ? "" : " · " + e.text()));
              }
            });
    this.events
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (o, a, e) -> {
              this.correct.setDisable(
                  e == null
                      || this.editor.document().experiment().procedure().steps().stream()
                          .noneMatch(s -> s.id().equals(e.stepId())));
              this.eventDetail.setText(e == null ? "" : eventText(e));
              visible(this.eventDetails, e != null);
            });
    this.eventDetails.setText("Полная запись и исправление");
    this.eventDetails.setId("procedure-event-details");
    this.eventDetails.setContent(new VBox(8, this.eventDetail, this.correct));
    this.eventDetails.setExpanded(false);
    this.eventDetails.setAnimated(false);
    visible(this.eventDetails, false);
    final VBox history = new VBox(10, this.timelineScope, this.events, this.eventDetails);
    history.setPadding(new Insets(16));
    history.getStyleClass().add("desk-panel");
    VBox.setVgrow(this.events, Priority.ALWAYS);
    this.bulk.setWrapText(true);
    this.bulk.setPrefRowCount(5);
    this.bulk.setPromptText(
        "Вставьте свою методику: одна операция на строку. Количества в таком тексте остаются ручными.");
    this.bulk.setId("procedure-bulk");
    final TitledPane bulkPane =
        new TitledPane(
            "Добавить несколько шагов из текста",
            new VBox(
                8, this.bulk, this.button("Добавить строки как шаги", "bulk-add", this::addBulk)));
    bulkPane.setAnimated(false);
    bulkPane.setExpanded(false);
    final Button firstStep =
        this.button("Добавить первый шаг", "add-step", () -> this.begin(Kind.OTHER));
    firstStep.getStyleClass().add("laboratory-primary-action");
    final TitledPane fromLoading =
        new TitledPane(
            "Начать со списка загрузки",
            new VBox(
                8,
                note(
                    "Добавятся участники выбранной стадии. Порядок и условия уточните по своей методике."),
                this.button("Подготовить шаги загрузки", "seed", this::seed)));
    fromLoading.setExpanded(false);
    fromLoading.setAnimated(false);
    this.empty
        .getChildren()
        .setAll(
            heading("Добавьте свою методику"),
            note(
                "Запишите действия по порядку. Во время работы отмечайте начало, завершение и наблюдения."),
            firstStep,
            fromLoading,
            bulkPane);
    this.empty.setPadding(new Insets(18));
    this.planTab.setContent(scroll(this.form));
    this.runTab.setContent(scroll(this.execution));
    this.historyTab.setContent(history);
    this.tabs.getTabs().setAll(this.runTab, this.historyTab);
    this.tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    this.tabs.setId("procedure-tabs");
    this.stepTitle.getStyleClass().add("desk-section-heading");
    this.stepTitle.setId("procedure-step-title");
    this.narrative.setId("procedure-narrative");
    this.narrative.setMaxWidth(Double.MAX_VALUE);
    this.narrative.setStyle("-fx-font-size: 14px;");
    this.warning.setId("procedure-warning");
    final TitledPane stepDetails =
        new TitledPane("Инструкция и плановые параметры", this.narrative);
    stepDetails.setId("procedure-step-details");
    stepDetails.setAnimated(false);
    stepDetails.setExpanded(true);
    stepDetails.managedProperty().bind(this.narrative.textProperty().isNotEmpty());
    stepDetails.visibleProperty().bind(stepDetails.managedProperty());
    final VBox summary =
        new VBox(
            6,
            this.stepTitle,
            new FlowPane(10, 6, this.progressText, this.editStep),
            stepDetails,
            this.warning);
    summary.setPadding(new Insets(4, 0, 6, 0));
    this.progressText.getStyleClass().add("desk-panel-subtitle");
    this.warning.managedProperty().bind(this.warning.textProperty().isNotEmpty());
    this.warning.visibleProperty().bind(this.warning.managedProperty());
    this.correction.managedProperty().bind(this.correction.textProperty().isNotEmpty());
    this.correction.visibleProperty().bind(this.correction.managedProperty());
    final VBox right = new VBox(8, summary, this.tabs, this.empty);
    right.setMinWidth(0);
    VBox.setVgrow(this.tabs, Priority.ALWAYS);
    VBox.setVgrow(this.empty, Priority.ALWAYS);
    final SplitPane split = new SplitPane(left, right);
    split.setDividerPositions(.27);
    split.setId("procedure-split");
    final Runnable responsive =
        () -> {
          final boolean narrow = this.getWidth() < 840;
          final Orientation orientation = narrow ? Orientation.VERTICAL : Orientation.HORIZONTAL;
          if (split.getOrientation() != orientation) {
            split.setOrientation(orientation);
            stepDetails.setExpanded(!narrow);
          }
          split.setDividerPositions(
              narrow ? .23 : Math.max(.22, Math.min(.30, 300 / Math.max(1, this.getWidth() - 32))));
          visible(this.steps, !narrow);
          visible(this.count, !narrow);
          visible(this.stepSelector, narrow);
          left.setMinHeight(narrow ? 145 : 0);
          left.setMaxHeight(narrow ? 164 : Double.MAX_VALUE);
          left.setMaxWidth(narrow ? Double.MAX_VALUE : 360);
          left.setPadding(new Insets(0, narrow ? 0 : 14, 0, 0));
          this.timer.setStyle(
              "-fx-font-size: " + (narrow ? "18" : "23") + "px; -fx-font-weight: bold;");
        };
    this.widthProperty().addListener(o -> responsive.run());
    responsive.run();
    final VBox body = new VBox(8, split);
    body.getStyleClass().add("desk-work-area");
    body.setPadding(new Insets(0, 16, 0, 16));
    VBox.setVgrow(split, Priority.ALWAYS);
    this.setCenter(body);
    this.discard
        .getChildren()
        .setAll(
            note("Есть незаписанный ввод."),
            new FlowPane(
                8,
                6,
                this.button(
                    "Отбросить ввод и выйти",
                    "discard",
                    () -> {
                      this.clearPending();
                      this.finish();
                    }),
                this.button("Продолжить", "continue", () -> visible(this.discard, false))));
    visible(this.discard, false);
    final FlowPane footer =
        new FlowPane(
            8,
            6,
            this.button(
                "Сохранить опыт",
                "save",
                () -> {
                  this.requireClean();
                  this.save.run();
                }),
            new MenuButton(
                "Ещё",
                null,
                item(
                    this.button("Вся хронология", "all-events", this::showAllEvents),
                    "Вся хронология"),
                item(
                    this.button(
                        "Добавить несколько шагов…",
                        "show-bulk",
                        () -> {
                          this.requireClean();
                          this.loading = true;
                          this.steps.getSelectionModel().clearSelection();
                          this.loading = false;
                          this.show(null);
                          bulkPane.setExpanded(true);
                        }),
                    "Добавить несколько шагов из текста…")));
    final VBox bottom = new VBox(8, this.discard, footer, this.status);
    bottom.setPadding(new Insets(10, 16, 12, 16));
    this.setBottom(bottom);
    for (final TextInputControl f :
        List.of(this.title, this.instruction, this.portion, this.duration, this.temperature)) {
      f.textProperty().addListener(o -> this.touch());
    }
    for (final ComboBox<?> f : List.of(this.kind, this.stage, this.material)) {
      f.valueProperty().addListener(o -> this.touch());
    }
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
                this.status.setText("Сначала запишите или сбросьте ввод.");
                return;
              }
              this.refresh(null);
            });
    this.addEventFilter(
        KeyEvent.KEY_PRESSED,
        e -> {
          if (e.isShortcutDown() && e.getCode() == KeyCode.ENTER) {
            if (this.tabs.getSelectionModel().getSelectedItem() == this.planTab && this.formDirty) {
              this.apply.fire();
            } else if (!this.observation.getText().isBlank()) {
              this.writeNote.fire();
            }
            e.consume();
          }
        });
    this.clock =
        new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), e -> this.updateTimer()));
    this.clock.setCycleCount(Timeline.INDEFINITE);
    this.clock.play();
    this.refresh(null);
  }

  private Button button(final String text, final String id, final Runnable run) {
    final Button b = new Button(text);
    b.setId("procedure-" + id);
    b.setMinWidth(Region.USE_PREF_SIZE);
    b.setOnAction(
        e -> {
          try {
            run.run();
          } catch (final RuntimeException x) {
            this.status.setText(
                x.getMessage() == null ? x.getClass().getSimpleName() : x.getMessage());
          }
        });
    return b;
  }

  private static MenuItem item(final Button button, final String title) {
    final MenuItem item = new MenuItem(title);
    item.setId(button.getId());
    item.disableProperty().bind(button.disableProperty());
    item.setOnAction(e -> button.fire());
    return item;
  }

  private void editView() {
    if (!this.tabs.getTabs().contains(this.planTab)) {
      this.tabs.getTabs().add(0, this.planTab);
    }
    this.tabs.getSelectionModel().select(this.planTab);
  }

  private void editSelected() {
    this.requireClean();
    this.requireCurrent();
    if (!this.expected.experiment().procedure().eventsFor(this.selected().id()).isEmpty()) {
      throw new IllegalArgumentException(
          "Проведённый шаг хранится неизменным. Выберите «Повторить шаг» для нового плана.");
    }
    this.editView();
    this.title.requestFocus();
  }

  private Step workStep(final Step except) {
    final var values = this.steps.getItems();
    return values.stream()
        .filter(s -> !Objects.equals(s, except) && state(s.id()) == Status.RUNNING)
        .findFirst()
        .orElseGet(
            () ->
                values.stream()
                    .filter(s -> !Objects.equals(s, except) && state(s.id()) == Status.PLANNED)
                    .findFirst()
                    .orElse(null));
  }

  private void next() {
    this.requireClean();
    this.requireCurrent();
    final Step next = this.workStep(this.steps.getSelectionModel().getSelectedItem());
    if (next != null) this.steps.getSelectionModel().select(next);
  }

  private static Label heading(final String text) {
    final Label l = note(text);
    l.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
    return l;
  }

  private static String uniqueLabel(final Map<String, String> names, final String title) {
    String label = title;
    int n = 2;
    while (names.containsKey(label)) {
      label = title + " · " + n++;
    }
    return label;
  }

  private static VBox field(final String label, final Node node) {
    final VBox box = new VBox(5, note(label), node);
    box.setMinWidth(0);
    if (node instanceof ComboBox<?> c) {
      c.setMaxWidth(Double.MAX_VALUE);
      c.setPrefWidth(220);
    }
    if (node instanceof TextField f) {
      f.setPrefColumnCount(16);
    }
    return box;
  }

  private static void visible(final Node node, final boolean value) {
    node.setVisible(value);
    node.setManaged(value);
  }

  private static String time(final Instant at) {
    return DateTimeFormatter.ofPattern("dd.MM HH:mm:ss")
        .withZone(ZoneId.systemDefault())
        .format(at);
  }

  private String stageLabel(final String id, final String fallback) {
    return this.stageNames.entrySet().stream()
        .filter(e -> Objects.equals(id, e.getValue()))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse(fallback);
  }

  private Status state(final String id) {
    return this.editor.document().experiment().procedure().status(id);
  }

  private static String stateText(final Status s) {
    return switch (s) {
      case PLANNED -> "В плане";
      case RUNNING -> "В работе";
      case COMPLETED -> "Выполнено";
      case SKIPPED -> "Пропущено";
    };
  }

  private static String actionText(final Action a) {
    return switch (a) {
      case START -> "Начало";
      case COMPLETE -> "Завершение";
      case RECORD_COMPLETE -> "Отметка о выполнении без времени проведения";
      case SKIP -> "Пропуск";
      case OBSERVE -> "Наблюдение";
      case CORRECT -> "Исправление";
    };
  }

  private static String qty(final LaboratoryQuantity q) {
    return q == null ? "не задано" : LaboratoryQuantityText.format(q, true);
  }

  private void touch() {
    if (!this.loading) {
      this.formDirty = true;
    }
  }

  boolean pending() {
    return this.formDirty
        || !this.observation.getText().isBlank()
        || !this.actual.getText().isBlank()
        || !this.bulk.getText().isBlank()
        || this.correctionId != null
        || this.conflict;
  }

  private void requireClean() {
    if (this.pending()) {
      throw new IllegalArgumentException("Сначала запишите шаг / наблюдение или сбросьте ввод.");
    }
  }

  private void requireCurrent() {
    if (this.conflict || !this.expected.equals(this.editor.document())) {
      this.conflict = true;
      throw new IllegalArgumentException(
          "Опыт изменился после открытия методики. Ввод сохранён в черновике; вернитесь к опыту и откройте методику заново.");
    }
  }

  private Step selected() {
    final var s = this.steps.getSelectionModel().getSelectedItem();
    if (s == null) {
      throw new IllegalArgumentException("Выберите шаг методики.");
    }
    return s;
  }

  private void clearPending() {
    this.loading = true;
    this.formDirty = false;
    this.observation.clear();
    this.actual.clear();
    this.bulk.clear();
    this.correctionId = null;
    this.writeNote.setText("Записать наблюдение");
    this.conflict = false;
    this.conflictDraft = null;
    visible(this.discard, false);
    this.loading = false;
  }

  boolean canClose() {
    if (this.pending()) {
      visible(this.discard, true);
      this.status.setText("Запишите введённое или выберите, как выйти.");
      if (this.formDirty) this.editView();
      else if (!this.observation.getText().isBlank()
          || !this.actual.getText().isBlank()
          || this.correctionId != null) {
        this.tabs.getSelectionModel().select(this.runTab);
        this.measured.setExpanded(!this.actual.getText().isBlank());
      }
      return false;
    }
    return true;
  }

  void dispose() {
    this.clock.stop();
    super.dispose();
  }

  private void refresh(final String selectedId) {
    this.loading = true;
    this.expected = this.editor.document();
    final var p = this.expected.experiment().procedure();
    final String filterId = this.stageNames.get(this.filter.getValue());
    this.steps
        .getItems()
        .setAll(
            p.steps().stream()
                .filter(
                    s ->
                        ALL.equals(this.filter.getValue()) || Objects.equals(filterId, s.stageId()))
                .toList());
    final long completed =
        this.steps.getItems().stream().filter(s -> p.status(s.id()) == Status.COMPLETED).count();
    final long running =
        this.steps.getItems().stream().filter(s -> p.status(s.id()) == Status.RUNNING).count();
    this.count.setText(
        (ALL.equals(this.filter.getValue())
                ? "Шагов: " + this.steps.getItems().size()
                : "Показано " + this.steps.getItems().size() + " из " + p.steps().size())
            + " · выполнено "
            + completed
            + (running == 0 ? "" : " · в работе " + running));
    final Step selection =
        this.steps.getItems().stream()
            .filter(s -> s.id().equals(selectedId))
            .findFirst()
            .orElse(
                this.workStep(null) != null
                    ? this.workStep(null)
                    : this.steps.getItems().isEmpty() ? null : this.steps.getItems().getLast());
    this.steps.getSelectionModel().select(selection);
    this.loading = false;
    this.show(selection);
  }

  private void show(final Step step) {
    this.loading = true;
    this.stepSelector.setValue(step);
    this.editingId = step == null ? null : step.id();
    this.formDirty = false;
    this.tabs.getTabs().remove(this.planTab);
    this.tabs.getSelectionModel().select(this.runTab);
    this.advanced.setExpanded(false);
    this.measured.setExpanded(!this.actual.getText().isBlank());
    this.quickNotes.setExpanded(false);
    visible(this.tabs, step != null);
    visible(this.empty, step == null);
    this.stepTitle.setText(step == null ? this.expected.experiment().title() : step.title());
    this.warning.setText("");
    this.title.setText(step == null ? "" : step.title());
    this.kind.setValue(step == null ? Kind.OTHER : step.kind());
    this.instruction.setText(step == null ? "" : step.instruction());
    this.material.setValue(
        step == null
            ? NONE
            : this.materialNames.entrySet().stream()
                .filter(e -> Objects.equals(e.getValue(), step.componentId()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(NONE));
    this.stage.setValue(
        step == null
            ? this.filter.getValue().equals(ALL) ? NONE : this.filter.getValue()
            : stageLabel(step.stageId(), NONE));
    this.portion.setText(
        step == null
            ? "100"
            : step.portion()
                .multiply(BigDecimal.valueOf(100))
                .stripTrailingZeros()
                .toPlainString());
    this.duration.setText(
        step == null || step.durationSeconds() == 0
            ? ""
            : BigDecimal.valueOf(step.durationSeconds())
                .divide(BigDecimal.valueOf(60), 8, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString());
    this.temperature.setText(
        step == null || step.temperatureC() == null ? "" : step.temperatureC().toPlainString());
    this.durationDisplay = this.duration.getText();
    this.displayedSeconds = step == null ? 0 : step.durationSeconds();
    final Status state = step == null ? Status.PLANNED : state(step.id());
    final boolean recorded =
        step != null && !this.expected.experiment().procedure().eventsFor(step.id()).isEmpty();
    this.form.setDisable(recorded);
    this.remove.setDisable(step == null || recorded);
    this.up.setDisable(
        step == null || this.expected.experiment().procedure().steps().indexOf(step) == 0);
    this.down.setDisable(
        step == null
            || this.expected.experiment().procedure().steps().indexOf(step)
                == this.expected.experiment().procedure().steps().size() - 1);
    this.duplicate.setDisable(step == null);
    visible(this.editStep, step != null && !recorded);
    this.recordComplete.setDisable(step == null || state != Status.PLANNED);
    visible(this.recordComplete, step != null && state == Status.PLANNED);
    this.start.setDisable(step == null || state != Status.PLANNED);
    this.complete.setDisable(step == null || state != Status.RUNNING);
    this.skip.setDisable(step == null || state == Status.COMPLETED || state == Status.SKIPPED);
    this.writeNote.setDisable(step == null);
    this.correct.setDisable(true);
    final Step next = this.workStep(step);
    visible(this.start, step != null && state == Status.PLANNED);
    visible(this.complete, step != null && state == Status.RUNNING);
    visible(this.skip, step != null && (state == Status.PLANNED || state == Status.RUNNING));
    visible(
        this.nextStep,
        step != null && (state == Status.COMPLETED || state == Status.SKIPPED) && next != null);
    this.nextStep.setText(
        next != null && state(next.id()) == Status.RUNNING
            ? "К текущему шагу"
            : "К следующему шагу");
    this.nextStepText.setText(
        next == null
            ? step != null && (state == Status.COMPLETED || state == Status.SKIPPED)
                ? "Все шаги в этом списке завершены или пропущены."
                : ""
            : (state(next.id()) == Status.RUNNING ? "Также в работе: " : "Далее в плане: ")
                + next.title());
    visible(this.nextStepText, !this.nextStepText.getText().isBlank());
    visible(this.measured, step != null && step.componentId() != null);
    this.recordedAmount.setText("");
    visible(this.recordedAmount, false);
    this.progressText.setText(
        step == null
            ? ""
            : stateText(state)
                + (recorded
                    ? " · записанные параметры"
                    : step.componentId() == null
                        ? " · ручной шаг; количества в тексте не пересчитываются"
                        : " · связанное количество пересчитывается с загрузкой"));
    if (step == null) {
      this.narrative.setText("");
      this.narrative.setTooltip(null);
      this.events.getItems().clear();
    } else {
      final var history = this.expected.experiment().procedure().eventsFor(step.id());
      this.events.getItems().setAll(history.reversed());
      final var corrected =
          history.stream()
              .map(Event::correctsId)
              .filter(Objects::nonNull)
              .collect(java.util.stream.Collectors.toSet());
      history.reversed().stream()
          .filter(e -> e.actualQuantity() != null && !corrected.contains(e.id()))
          .findFirst()
          .ifPresent(
              e -> {
                this.recordedAmount.setText(
                    "Последнее записанное количество: "
                        + qty(e.actualQuantity())
                        + " · "
                        + actionText(e.action()).toLowerCase(Locale.ROOT)
                        + " · "
                        + time(e.recordedAt()));
                visible(this.recordedAmount, true);
              });
      final var resolved = LaboratoryProcedureService.resolve(this.expected.experiment(), step);
      this.narrative.setText(stepSummary(resolved));
      this.narrative.setTooltip(
          resolved.plannedQuantity() == null
              ? null
              : new Tooltip(
                  "Точное плановое количество: "
                      + LaboratoryQuantityText.format(resolved.plannedQuantity(), false)));
      this.warning.setText(String.join("\n", resolved.warnings()));
      if (this.actual.getText().isBlank() && resolved.plannedQuantity() != null) {
        this.actualUnit.setValue(resolved.plannedQuantity().unit());
      }
    }
    if (this.allEvents) {
      this.events.getItems().setAll(this.expected.experiment().procedure().events().reversed());
    }
    this.timelineScope.setText(
        this.allEvents
            ? "Хронология всей методики. Исправления сохраняют исходную запись."
            : "Хронология выбранного шага. Исправления добавляются отдельной записью.");
    this.eventDetail.clear();
    visible(this.eventDetails, false);
    this.correction.setText(
        this.correctionId == null
            ? ""
            : "Исправление выбранной записи: исходная запись останется в истории.");
    this.loading = false;
    this.updateTimer();
  }

  /** Read-only summary: the title is already shown above; domain narratives and frozen records stay exact. */
  private static String stepSummary(final LaboratoryProcedureService.Resolved resolved) {
    final Step step = resolved.step();
    final List<String> lines = new ArrayList<>();
    final String component = resolved.componentName();
    if (!component.isBlank()) {
      final boolean namedByTitle =
          step.title().strip().equals(component)
              || step.kind() == Kind.ADD && step.title().strip().equals("Добавить: " + component);
      if (!namedByTitle) {
        lines.add("Компонент: " + component);
      }
      lines.add(
          "Плановое количество: "
              + summaryQuantity(resolved.plannedQuantity())
              + (step.portion().compareTo(BigDecimal.ONE) == 0
                  ? ""
                  : " · "
                      + step.portion()
                          .multiply(BigDecimal.valueOf(100))
                          .stripTrailingZeros()
                          .toPlainString()
                      + "% плановой загрузки"));
    }
    final List<String> conditions = new ArrayList<>();
    if (step.temperatureC() != null) {
      conditions.add(
          "Температура: " + step.temperatureC().stripTrailingZeros().toPlainString() + " °C");
    }
    if (step.durationSeconds() > 0) {
      conditions.add(
          "Продолжительность: " + LaboratoryProcedureService.duration(step.durationSeconds()));
    }
    if (!conditions.isEmpty()) {
      lines.add(String.join(" · ", conditions));
    }
    if (!step.instruction().isBlank() && !step.instruction().strip().equals(step.title().strip())) {
      lines.add(step.instruction());
    }
    return String.join("\n", lines);
  }

  private static String summaryQuantity(final LaboratoryQuantity quantity) {
    if (quantity == null) {
      return "не задано";
    }
    final BigDecimal value = quantity.value(),
        shown =
            value
                .round(new java.math.MathContext(4, java.math.RoundingMode.HALF_UP))
                .stripTrailingZeros();
    final String number =
        shown.signum() == 0
                || shown.abs().compareTo(new BigDecimal("0.001")) >= 0
                    && shown.abs().compareTo(new BigDecimal("1000000")) < 0
            ? shown.toPlainString()
            : shown.toEngineeringString();
    return (shown.compareTo(value) == 0 ? "" : "≈ ") + number + " " + quantity.unit();
  }

  private void begin(final Kind kind) {
    this.requireClean();
    this.requireCurrent();
    this.loading = true;
    this.steps.getSelectionModel().clearSelection();
    this.loading = false;
    this.show(null);
    visible(this.empty, false);
    visible(this.tabs, true);
    this.form.setDisable(false);
    this.kind.setValue(kind);
    this.title.setText(kind == Kind.OTHER ? "" : kind.toString());
    this.editingId = null;
    this.formDirty = true;
    this.editView();
    this.stepTitle.setText("Новый шаг");
    this.title.requestFocus();
  }

  private Step readStep() {
    final String name = this.title.getText().strip();
    if (name.isEmpty()) {
      throw new IllegalArgumentException("Назовите шаг методики.");
    }
    final BigDecimal fraction =
        decimal(this.portion.getText(), "Доля загрузки").divide(BigDecimal.valueOf(100));
    final long seconds;
    try {
      seconds =
          this.duration.getText().equals(this.durationDisplay)
              ? this.displayedSeconds
              : this.duration.getText().isBlank()
                  ? 0
                  : decimal(this.duration.getText(), "Продолжительность")
                      .multiply(BigDecimal.valueOf(60))
                      .longValueExact();
    } catch (final ArithmeticException x) {
      throw new IllegalArgumentException("Укажите продолжительность с точностью до секунды.");
    }
    return new Step(
        this.editingId == null ? "step_" + UUID.randomUUID() : this.editingId,
        this.stageNames.get(this.stage.getValue()),
        this.kind.getValue(),
        name,
        this.instruction.getText().strip(),
        this.materialNames.get(this.material.getValue()),
        fraction,
        seconds,
        this.temperature.getText().isBlank()
            ? null
            : decimal(this.temperature.getText(), "Температура"));
  }

  private static BigDecimal decimal(final String text, final String name) {
    try {
      return new BigDecimal(text.strip().replace(',', '.'));
    } catch (final NumberFormatException e) {
      throw new IllegalArgumentException(name + ": введите число.");
    }
  }

  private void applyStep() {
    this.requireCurrent();
    if (!this.observation.getText().isBlank()
        || !this.actual.getText().isBlank()
        || !this.bulk.getText().isBlank()) {
      throw new IllegalArgumentException(
          "Сначала запишите или сбросьте наблюдение / список шагов.");
    }
    final Step step = this.readStep();
    final var values = new ArrayList<>(this.expected.experiment().procedure().steps());
    final int position =
        this.editingId == null
            ? -1
            : java.util.stream.IntStream.range(0, values.size())
                .filter(i -> values.get(i).id().equals(this.editingId))
                .findFirst()
                .orElseThrow();
    if (position < 0) {
      values.add(step);
    } else {
      values.set(position, step);
    }
    this.editor.procedurePlan(this.expected, values);
    this.formDirty = false;
    this.loading = true;
    if (!ALL.equals(this.filter.getValue())
        && !Objects.equals(this.stageNames.get(this.filter.getValue()), step.stageId())) {
      this.filter.setValue(stageLabel(step.stageId(), ALL));
    }
    this.loading = false;
    this.refresh(step.id());
    this.status.setText("Шаг записан в опыт. Ctrl+S или «Сохранить опыт» сохраняет файл.");
  }

  private void resetForm() {
    final String id = this.editingId;
    this.clearPending();
    this.refresh(id);
    visible(this.discard, false);
    this.status.setText("Незаписанный ввод сброшен. Сохранённые шаги и наблюдения не изменены.");
  }

  private void duplicate() {
    this.requireClean();
    this.requireCurrent();
    final Step s = this.selected();
    final Step copy =
        new Step(
            "step_" + UUID.randomUUID(),
            s.stageId(),
            s.kind(),
            s.title() + " — повтор",
            s.instruction(),
            s.componentId(),
            s.portion(),
            s.durationSeconds(),
            s.temperatureC());
    final var values = new ArrayList<>(this.expected.experiment().procedure().steps());
    values.add(values.indexOf(s) + 1, copy);
    this.editor.procedurePlan(this.expected, values);
    this.refresh(copy.id());
    this.editView();
  }

  private void remove() {
    this.requireClean();
    this.requireCurrent();
    final Step s = this.selected();
    this.editor.procedurePlan(
        this.expected,
        this.expected.experiment().procedure().steps().stream()
            .filter(v -> !v.id().equals(s.id()))
            .toList());
    this.refresh(null);
  }

  private void move(final int delta) {
    this.requireClean();
    this.requireCurrent();
    final Step s = this.selected();
    final var values = new ArrayList<>(this.expected.experiment().procedure().steps());
    final int i = values.indexOf(s), to = i + delta;
    if (to < 0 || to >= values.size()) {
      return;
    }
    Collections.swap(values, i, to);
    this.editor.procedurePlan(this.expected, values);
    this.refresh(s.id());
  }

  private void seed() {
    this.requireClean();
    this.requireCurrent();
    final String stageId = this.stageNames.get(this.filter.getValue());
    final var exp = this.expected.experiment();
    final var s =
        exp.stages().stream()
            .filter(v -> v.id().equals(stageId))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalArgumentException("Выберите конкретную стадию над списком шагов."));
    final var values = new ArrayList<>(exp.procedure().steps());
    String first = null;
    for (final var p : s.participants()) {
      if (p.role() == ReactionComponentRole.PRODUCT
          || values.stream()
              .anyMatch(
                  v ->
                      Objects.equals(v.stageId(), s.id())
                          && Objects.equals(v.componentId(), p.componentId())
                          && v.kind() == Kind.ADD)) {
        continue;
      }
      final var c =
          exp.components().stream()
              .filter(v -> v.id().equals(p.componentId()))
              .findFirst()
              .orElseThrow();
      final String id = "step_" + UUID.randomUUID();
      if (first == null) {
        first = id;
      }
      values.add(
          new Step(
              id,
              s.id(),
              Kind.ADD,
              "Добавить: " + c.molecule().displayName(),
              "",
              c.id(),
              BigDecimal.ONE,
              0,
              null));
    }
    if (first == null) {
      throw new IllegalArgumentException(
          "Для участников этой стадии шаги загрузки уже созданы, либо нет исходников и реагентов.");
    }
    this.editor.procedurePlan(this.expected, values);
    this.refresh(first);
    this.status.setText(
        "Подготовлен список загрузки. Уточните порядок добавления и условия по вашей методике.");
  }

  private void addBulk() {
    if (this.formDirty
        || !this.observation.getText().isBlank()
        || !this.actual.getText().isBlank()) {
      throw new IllegalArgumentException("Завершите текущий ввод.");
    }
    this.requireCurrent();
    final var lines =
        this.bulk.getText().lines().map(String::strip).filter(s -> !s.isEmpty()).toList();
    if (lines.isEmpty() || lines.size() > 100) {
      throw new IllegalArgumentException("Введите от 1 до 100 непустых строк.");
    }
    final var values = new ArrayList<>(this.expected.experiment().procedure().steps());
    String first = null;
    for (final String line : lines) {
      final String id = "step_" + UUID.randomUUID();
      if (first == null) {
        first = id;
      }
      values.add(
          new Step(
              id,
              this.stageNames.get(this.filter.getValue()),
              Kind.OTHER,
              line.length() > 100 ? line.substring(0, 97) + "…" : line,
              line,
              null,
              BigDecimal.ONE,
              0,
              null));
    }
    this.editor.procedurePlan(this.expected, values);
    this.bulk.clear();
    this.refresh(first);
    this.status.setText(
        "Добавлено шагов: "
            + lines.size()
            + ". Для автоматического пересчёта задайте связанную загрузку в нужных шагах.");
  }

  private void execute(final Action action) {
    this.requireCurrent();
    if (this.formDirty || !this.bulk.getText().isBlank()) {
      throw new IllegalArgumentException("Сначала запишите изменения методики.");
    }
    final Step s = this.selected();
    if (action == Action.START && !this.actual.getText().isBlank()) {
      throw new IllegalArgumentException(
          "Фактическое количество записывается при завершении или в наблюдении.");
    }
    if ((action == Action.SKIP || action == Action.CORRECT || action == Action.OBSERVE)
        && this.observation.getText().isBlank()) {
      this.observation.requestFocus();
      throw new IllegalArgumentException(
          action == Action.SKIP
              ? "Укажите причину пропуска в наблюдении и нажмите «Пропустить» ещё раз."
              : "Введите наблюдение или пояснение исправления.");
    }
    final LaboratoryQuantity measured =
        this.actual.getText().isBlank()
            ? null
            : new LaboratoryQuantity(
                LaboratoryDecimalField.value(this.actual), this.actualUnit.getValue());
    this.editor.procedureEvent(
        this.expected,
        s.id(),
        action,
        (action == Action.RECORD_COMPLETE
                ? "Выполнено ранее. Фактическое время начала и окончания не указано.\n"
                : "")
            + this.observation.getText().strip(),
        measured,
        action == Action.CORRECT ? this.correctionId : null,
        Instant.now());
    this.clearPending();
    this.refresh(s.id());
    this.tabs.getSelectionModel().select(this.runTab);
    this.status.setText(actionText(action) + " записано в опыт.");
  }

  private void clearNote() {
    this.observation.clear();
    this.actual.clear();
    this.correctionId = null;
    this.correction.setText("");
    this.writeNote.setText("Записать наблюдение");
    this.measured.setExpanded(false);
    if (!this.pending()) visible(this.discard, false);
  }

  private void showAllEvents() {
    this.requireClean();
    this.allEvents = true;
    this.timelineScope.setText("Хронология всей методики. Исправления сохраняют исходную запись.");
    visible(this.empty, false);
    visible(this.tabs, true);
    this.events.getItems().setAll(this.expected.experiment().procedure().events().reversed());
    this.tabs.getSelectionModel().select(this.historyTab);
  }

  private void beginCorrection() {
    this.requireClean();
    final var e = this.events.getSelectionModel().getSelectedItem();
    if (e == null) {
      return;
    }
    this.loading = true;
    this.filter.setValue(ALL);
    this.loading = false;
    this.refresh(e.stepId());
    this.correctionId = e.id();
    this.correction.setText(
        "Исправление записи от " + time(e.recordedAt()) + ". Исходная запись сохранится.");
    this.observation.setText(e.text());
    if (e.actualQuantity() != null) {
      this.actual.setText(e.actualQuantity().value().toPlainString());
      this.actualUnit.setValue(e.actualQuantity().unit());
      this.measured.setExpanded(true);
    }
    this.writeNote.setText("Записать исправление");
    this.tabs.getSelectionModel().select(this.runTab);
    this.observation.requestFocus();
  }

  private void updateTimer() {
    if (this.disposed) {
      return;
    }
    final Step s = this.steps.getSelectionModel().getSelectedItem();
    if (s == null) {
      this.timer.setText("");
      visible(this.timer, false);
      return;
    }
    final var p = this.editor.document().experiment().procedure();
    visible(
        this.timer,
        p.status(s.id()) == Status.RUNNING
            || p.status(s.id()) == Status.PLANNED && s.durationSeconds() > 0);
    if (p.status(s.id()) != Status.RUNNING) {
      this.timer.setText(
          p.status(s.id()) == Status.PLANNED && s.durationSeconds() > 0
              ? "По плану: " + elapsed(s.durationSeconds())
              : "");
      return;
    }
    final Instant due = LaboratoryProcedureService.deadline(p, s.id());
    final Event start = p.latestStart(s.id());
    if (due == null) {
      this.timer.setText(
          "В работе · "
              + elapsed(
                  Math.max(
                      0,
                      java.time.Duration.between(start.recordedAt(), Instant.now()).getSeconds())));
    } else {
      final long left = java.time.Duration.between(Instant.now(), due).getSeconds();
      this.timer.setText(
          left > 0 ? "Осталось " + elapsed(left) : "Плановое время истекло · +" + elapsed(-left));
    }
  }

  private static String elapsed(final long seconds) {
    return String.format(
        Locale.ROOT, "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60);
  }

  private static String snapshotText(final Event e) {
    final Step s = e.snapshot();
    return s.kind()
        + (e.componentName().isBlank()
            ? ""
            : " · " + e.componentName() + "\nПлан: " + qty(e.plannedQuantity()))
        + (s.temperatureC() == null ? "" : " · " + s.temperatureC().toPlainString() + " °C")
        + (s.durationSeconds() == 0 ? "" : " · " + elapsed(s.durationSeconds()))
        + (s.instruction().isBlank() ? "" : "\n" + s.instruction());
  }

  private static String eventText(final Event e) {
    return time(e.recordedAt())
        + " · "
        + actionText(e.action())
        + "\n"
        + snapshotText(e)
        + "\nФактическое количество: "
        + qty(e.actualQuantity())
        + "\n\n"
        + e.text()
        + (e.correctsId() == null
            ? ""
            : "\n\nЭто дополнение к исходной записи; прежний текст сохранён.");
  }

  void draft(final Properties p, final boolean restore) {
    if (!restore && this.conflictDraft != null) {
      this.conflictDraft.forEach(p::put);
      return;
    }
    final Map<String, Control> fields = new LinkedHashMap<>();
    fields.put("title", this.title);
    fields.put("instruction", this.instruction);
    fields.put("portion", this.portion);
    fields.put("duration", this.duration);
    fields.put("temperature", this.temperature);
    fields.put("kind", this.kind);
    fields.put("stage", this.stage);
    fields.put("material", this.material);
    fields.put("observation", this.observation);
    fields.put("actual", this.actual);
    fields.put("actualUnit", this.actualUnit);
    fields.put("bulk", this.bulk);
    if (!restore) {
      p.setProperty("procedure.source", new LaboratoryFileCodec().write(this.expected));
      p.setProperty(
          "procedure.filterId",
          ALL.equals(this.filter.getValue())
              ? "*"
              : Objects.toString(this.stageNames.get(this.filter.getValue()), ""));
      p.setProperty("procedure.editing", Objects.toString(this.editingId, ""));
      p.setProperty("procedure.formDirty", Boolean.toString(this.formDirty));
      p.setProperty("procedure.conflict", Boolean.toString(this.conflict));
      p.setProperty(
          "procedure.tab",
          Integer.toString(
              this.tabs.getSelectionModel().getSelectedItem() == this.planTab
                  ? 0
                  : this.tabs.getSelectionModel().getSelectedItem() == this.historyTab ? 2 : 1));
      p.setProperty(
          "procedure.stageId", Objects.toString(this.stageNames.get(this.stage.getValue()), ""));
      p.setProperty(
          "procedure.materialId",
          Objects.toString(this.materialNames.get(this.material.getValue()), ""));
      p.setProperty("procedure.durationDisplay", this.durationDisplay);
      p.setProperty("procedure.displayedSeconds", Long.toString(this.displayedSeconds));
      if (this.correctionId != null) {
        p.setProperty("procedure.correction", this.correctionId);
      }
      LaboratoryFormDraft.fields(p, "procedure", false, fields);
      return;
    }
    final List<String> missing = new ArrayList<>();
    this.loading = true;
    try {
      final String filterId = p.getProperty("procedure.filterId");
      String filterLabel;
      if (filterId == null) {
        final String legacy = p.getProperty("procedure.filter", ALL);
        filterLabel = this.filter.getItems().contains(legacy) ? legacy : ALL;
        if (!this.filter.getItems().contains(legacy)) {
          missing.add("фильтр стадии «" + legacy + "»");
        }
      } else if (filterId.equals("*")) {
        filterLabel = ALL;
      } else {
        filterLabel =
            this.stageNames.entrySet().stream()
                .filter(e -> Objects.toString(e.getValue(), "").equals(filterId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
        if (filterLabel == null) {
          missing.add("стадия фильтра " + filterId);
          filterLabel = ALL;
        }
      }
      this.filter.setValue(filterLabel);
      final String editId = p.getProperty("procedure.editing", "");
      this.loading = false;
      this.refresh(editId);
      if (editId.isEmpty()) {
        this.show(null);
      }
      this.loading = true;
      this.editingId = editId.isEmpty() ? null : editId;
      if (this.editingId != null
          && this.expected.experiment().procedure().steps().stream()
              .noneMatch(s -> s.id().equals(this.editingId))) {
        missing.add("шаг " + this.editingId);
      }
      LaboratoryFormDraft.fields(p, "procedure", true, fields);
      final String stageId = p.getProperty("procedure.stageId", ""),
          materialId = p.getProperty("procedure.materialId", "");
      final String stageLabel =
          this.stageNames.entrySet().stream()
              .filter(e -> Objects.toString(e.getValue(), "").equals(stageId))
              .map(Map.Entry::getKey)
              .findFirst()
              .orElse(null);
      final String materialLabel =
          this.materialNames.entrySet().stream()
              .filter(e -> Objects.toString(e.getValue(), "").equals(materialId))
              .map(Map.Entry::getKey)
              .findFirst()
              .orElse(null);
      if (stageLabel == null) {
        missing.add("стадия " + stageId);
        this.stage.setValue("Недоступная стадия · " + stageId);
      } else {
        this.stage.setValue(stageLabel);
      }
      if (materialLabel == null) {
        missing.add("компонент " + materialId);
        this.material.setValue("Недоступный компонент · " + materialId);
      } else {
        this.material.setValue(materialLabel);
      }
      this.formDirty = Boolean.parseBoolean(p.getProperty("procedure.formDirty"));
      this.correctionId = p.getProperty("procedure.correction");
      if (p.containsKey("procedure.durationDisplay")) {
        this.durationDisplay = p.getProperty("procedure.durationDisplay");
        try {
          this.displayedSeconds = Long.parseLong(p.getProperty("procedure.displayedSeconds", "0"));
          if (this.displayedSeconds < 0 || this.displayedSeconds > 31536000) {
            throw new NumberFormatException();
          }
        } catch (NumberFormatException error) {
          missing.add("точное время шага");
          this.displayedSeconds = 0;
        }
      }
      this.conflict =
          !missing.isEmpty()
              || Boolean.parseBoolean(p.getProperty("procedure.conflict"))
              || !new LaboratoryFileCodec()
                  .write(this.expected)
                  .equals(p.getProperty("procedure.source"));
      if (this.conflict) {
        this.conflictDraft = new Properties();
        p.stringPropertyNames().stream()
            .filter(k -> k.startsWith("procedure."))
            .forEach(k -> this.conflictDraft.setProperty(k, p.getProperty(k)));
        this.conflictDraft.setProperty("procedure.conflict", "true");
        this.warning.setText(
            missing.isEmpty()
                ? "Восстановленный ввод относится к другой версии опыта. Исходный черновик сохранён; запись заблокирована до явного сброса."
                : "Не найдены связи: "
                    + String.join(", ", missing)
                    + ". Исходный черновик сохранён; привязки не сняты автоматически. Запись заблокирована до явного сброса.");
      }
      if (this.formDirty && this.editingId == null) {
        visible(this.empty, false);
        visible(this.tabs, true);
        this.form.setDisable(false);
        this.stepTitle.setText("Новый шаг · восстановленный ввод");
        this.narrative.setText("");
        this.progressText.setText("");
      }
      int tab;
      try {
        tab = Integer.parseInt(p.getProperty("procedure.tab", "0"));
      } catch (NumberFormatException error) {
        tab = 0;
      }
      if (tab == 0) this.editView();
      else this.tabs.getSelectionModel().select(tab == 2 ? this.historyTab : this.runTab);
      this.measured.setExpanded(!this.actual.getText().isBlank());
      this.advanced.setExpanded(this.formDirty);
      this.correction.setText(
          this.correctionId == null ? "" : "Восстановлено исправление исходной записи.");
      this.writeNote.setText(
          this.correctionId == null ? "Записать наблюдение" : "Записать исправление");
    } finally {
      this.loading = false;
    }
  }
}

/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryFileCodec;

/** The daily experiment workbench: live plan, performed steps, observations and measured results. */
final class LaboratoryExperimentDesk extends LaboratoryComputationPage {
  private record Choice(String id, String name) {
    @Override
    public String toString() {
      return this.name;
    }
  }

  private record JournalItem(
      String id,
      Instant at,
      String scope,
      String kind,
      String text,
      String metadata,
      boolean system) {}

  private record Timer(Label label, String stepId) {}

  private record CompoundCard(VBox box, Canvas canvas, double minimum, double weight) {}

  private final LaboratoryEditor editor;
  private Consumer<Boolean> save;
  private final Consumer<String> action;
  private LaboratoryDrawing expected;
  private final ComboBox<Choice> stages = new ComboBox<>(),
      factComponent = new ComboBox<>(),
      yieldReference = new ComboBox<>();
  private final ComboBox<LaboratoryQuantityUnit> factUnit = new ComboBox<>(),
      loadFactUnit = new ComboBox<>();
  private final ComboBox<Choice> loadFactComponent = new ComboBox<>();
  private final LaboratoryDecimalField loadFact = new LaboratoryDecimalField();
  private final LaboratoryDecimalField fact = new LaboratoryDecimalField();
  private final TextArea observation = new TextArea();
  private final ComboBox<Choice> noteStep = new ComboBox<>();
  private final VBox observationForm = new VBox(10),
      methodNotes = new VBox(),
      journalNotes = new VBox();
  private int journalLimit = 20;
  private final ComboBox<LaboratoryEntry.Kind> noteKind = new ComboBox<>();
  private final Label title = note(""), conditions = note(""), factHint = note("");
  private final VBox sheet = new VBox(18),
      scheme = new VBox(12),
      loading = new VBox(10),
      method = new VBox(10),
      results = new VBox(10),
      recent = new VBox(8),
      discard = new VBox(8);
  private final ScrollPane main = new ScrollPane(this.sheet);
  private final Map<String, Node> anchors = new LinkedHashMap<>();
  private final List<Timer> timers = new ArrayList<>();
  private final Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> this.tick()));
  private LaboratoryBatchPlanPane batch;
  private final LaboratoryStageOutcomePane outcome;
  private boolean refreshing, conflict, guardStatus, outcomeInputStatus;
  private String guardPrefix = "";
  private Properties conflictDraft;
  private final Set<String> participants = new HashSet<>();
  private final Map<String, Button> navigation = new LinkedHashMap<>();
  private final VBox quantityTable = new VBox(8);
  private String activeSection = "Реакция";
  private final Button projectButton = new Button("← К структурам");
  private Runnable projectReturn;
  private TitledPane factEntry, noteOptions, outcomeDetails, calculationDetails;
  private static final String AUTO_YIELD = "@automatic-yield";
  private ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryViewPreferences
      viewPreferences;
  private java.util.function.Supplier<java.nio.file.Path> documentPath = () -> null;
  private final Map<String, String> yieldReferences = new HashMap<>();
  private boolean missingYieldReference;

  LaboratoryExperimentDesk(
      final LaboratoryEditor editor,
      final String stageId,
      final Runnable save,
      final Consumer<String> action,
      final Runnable close) {
    super("Опыт", close);
    this.editor = editor;
    this.save = saveAs -> save.run();
    this.action = action;
    this.expected = editor.document();
    this.setId("experiment-desk");
    this.outcome =
        new LaboratoryStageOutcomePane(
            editor,
            () -> id(this.stages),
            this::outcomeChanged,
            this::beforeOutcomeWrite,
            this::outcomeWritten);
    this.title.getStyleClass().add("laboratory-navigation-title");
    this.title.setWrapText(false);
    this.title.setMinWidth(0);
    this.title.setMaxWidth(Double.MAX_VALUE);
    this.stages.setId("desk-stage");
    this.stages.setPrefWidth(360);
    this.stages.setMaxWidth(Double.MAX_VALUE);
    this.stages.setTooltip(new Tooltip("Текущая стадия реакции"));
    this.main.setFitToWidth(true);
    this.main.setPannable(false);
    this.main.setId("desk-scroll");
    this.sheet.setPadding(new Insets(8, 16, 16, 16));
    this.sheet.setMaxWidth(Double.MAX_VALUE);
    this.sheet.setMinWidth(0);
    this.sheet.getStyleClass().add("desk-work-area");
    this.sheet.setFillWidth(true);
    this.main.setContent(this.sheet);
    this.setCenter(this.main);
    final Button saveButton = this.button("Сохранить", "save", () -> this.saveRequested(false));
    saveButton.setMinWidth(Region.USE_PREF_SIZE);
    this.projectButton.setId("chemistry-computation-back");
    this.projectButton.getStyleClass().add("laboratory-navigation-back");
    this.projectButton.setMinWidth(Region.USE_PREF_SIZE);
    this.projectButton.setOnAction(event -> this.attempt(this::finish));
    final HBox heading = new HBox(12, this.projectButton, this.title, saveButton);
    heading.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    HBox.setHgrow(this.title, Priority.ALWAYS);
    heading.getStyleClass().add("laboratory-desk-header");
    saveButton.setTooltip(new Tooltip("Сохранить опыт · Ctrl+S; сохранить как… · Ctrl+Shift+S"));
    final MenuButton more = new MenuButton("Ещё");
    more.setId("desk-more");
    more.getItems()
        .setAll(
            this.menu(
                "Открыть файл опыта… · Ctrl+O", "open-file", () -> this.navigate("open-file")),
            this.menu("Сохранить как… · Ctrl+Shift+S", "save-as", () -> this.saveRequested(true)),
            new SeparatorMenuItem(),
            this.menu("Карта синтеза", "route", () -> this.navigate("route")),
            this.menu(
                "Название реакции и механизмы",
                "classification",
                () -> this.navigate("classification")),
            this.menu("Литература и источники", "literature", () -> this.navigate("literature")),
            new SeparatorMenuItem(),
            this.menu("Контроль и очистка", "operations", () -> this.navigate("operations")),
            this.menu("Образцы", "samples", () -> this.navigate("samples")),
            this.menu("Подтверждение продукта", "evidence", () -> this.navigate("evidence")),
            new SeparatorMenuItem(),
            this.menu("Сбросить весь незаписанный ввод", "reset", this::reset));
    final HBox context = new HBox(10, note("Стадия"), this.stages, more);
    context.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    context.getStyleClass().add("laboratory-desk-context");
    final FlowPane tabs = new FlowPane(4, 4);
    tabs.getStyleClass().add("laboratory-desk-navigation");
    for (final String name :
        List.of("Реакция", "Загрузка", "Методика", "Наблюдения", "Результат")) {
      final Button tab = this.button(name, "jump-" + this.navigation.size(), () -> this.jump(name));
      tab.getStyleClass().add("laboratory-desk-tab");
      this.navigation.put(name, tab);
      tabs.getChildren().add(tab);
    }
    final VBox header = new VBox(8, heading, context, tabs);
    header.setPadding(new Insets(12, 16, 8, 16));
    this.setTop(header);
    LaboratoryComponentNaming.choices(this.stages, Choice::name);
    LaboratoryComponentNaming.choices(this.factComponent, Choice::name);
    LaboratoryComponentNaming.choices(this.loadFactComponent, Choice::name);
    this.loadFactComponent.setId("desk-loading-fact-component");
    this.loadFactComponent.setPrefWidth(330);
    this.loadFactComponent.setMaxWidth(Double.MAX_VALUE);
    this.loadFact.setId("desk-loading-fact-value");
    this.loadFact.setPromptText("Фактически внесено");
    this.loadFact.setMaxWidth(170);
    this.loadFactUnit.getItems().setAll(LaboratoryQuantityUnit.values());
    this.loadFactUnit.setValue(LaboratoryQuantityUnit.GRAM);
    this.loadFactUnit.setId("desk-loading-fact-unit");
    this.loadFact.textProperty().addListener((o, a, b) -> this.refreshPending(false));
    LaboratoryComponentNaming.choices(this.yieldReference, Choice::name);
    this.yieldReference.setId("desk-yield-reference");
    this.yieldReference.setPrefWidth(330);
    this.yieldReference.setPromptText("Основа расчёта выхода");
    this.yieldReference.setMaxWidth(Double.MAX_VALUE);
    this.yieldReference
        .valueProperty()
        .addListener(
            (o, a, b) -> {
              if (!this.refreshing) {
                this.rememberYieldReference();
                this.renderResults();
              }
            });
    this.factComponent.setId("desk-fact-component");
    this.factComponent.setPrefWidth(330);
    this.factComponent.setMaxWidth(Double.MAX_VALUE);
    this.fact.setId("desk-fact-value");
    this.fact.setPromptText("Получено продукта");
    this.fact.setMaxWidth(170);
    this.factUnit.getItems().setAll(LaboratoryQuantityUnit.values());
    this.factUnit.setValue(LaboratoryQuantityUnit.GRAM);
    this.factUnit.setId("desk-fact-unit");
    this.factComponent.valueProperty().addListener((o, a, b) -> this.factHint());
    this.noteStep.setId("desk-note-step");
    this.noteStep.setMaxWidth(Double.MAX_VALUE);
    LaboratoryComponentNaming.choices(this.noteStep, Choice::name);
    this.noteStep
        .valueProperty()
        .addListener(
            (o, a, b) -> {
              if (this.noteOptions != null) {
                visible(this.noteOptions, b == null || b.id().isEmpty());
              }
            });
    this.noteKind
        .getItems()
        .setAll(
            LaboratoryEntry.Kind.OBSERVATION,
            LaboratoryEntry.Kind.RESULT,
            LaboratoryEntry.Kind.ACTION);
    this.noteKind.setValue(LaboratoryEntry.Kind.OBSERVATION);
    this.noteKind.setId("desk-note-kind");
    this.observation.setId("desk-observation");
    this.observation.setPromptText("Что наблюдалось, когда взяли пробу, что изменили…");
    this.observation.setPrefRowCount(4);
    this.observation.setWrapText(true);
    this.stages
        .valueProperty()
        .addListener(
            (o, before, after) -> {
              if (this.refreshing) {
                return;
              }
              if (this.pending()) {
                this.refreshing = true;
                this.stages.setValue(before);
                this.refreshing = false;
                this.refreshPending(true);
                return;
              }
              this.rebuild();
            });
    visible(this.discard, false);
    this.observation.textProperty().addListener((o, a, b) -> this.refreshPending(false));
    this.fact.textProperty().addListener((o, a, b) -> this.refreshPending(false));
    this.status.visibleProperty().bind(this.status.textProperty().isNotEmpty());
    this.status.managedProperty().bind(this.status.visibleProperty());
    final VBox bottom = new VBox(6, this.discard, this.status);
    bottom.setPadding(new Insets(4, 16, 10, 16));
    this.setBottom(bottom);
    this.addEventFilter(
        javafx.scene.input.KeyEvent.KEY_PRESSED,
        e -> {
          if (e.isShortcutDown()
              && e.getCode() == javafx.scene.input.KeyCode.ENTER
              && this.observation.isFocused()) {
            this.attempt(this::recordObservation);
            e.consume();
          }
        });
    this.refreshing = true;
    this.stages
        .getItems()
        .setAll(
            this.expected.experiment().stages().stream()
                .map(s -> new Choice(s.id(), s.title()))
                .toList());
    select(this.stages, stageId);
    if (this.stages.getValue() == null) {
      this.stages.getSelectionModel().selectFirst();
    }
    this.refreshing = false;
    this.rebuild();
    this.clock.setCycleCount(Timeline.INDEFINITE);
    this.clock.play();
  }

  void projectNavigation(final Runnable action) {
    this.projectReturn = Objects.requireNonNull(action, "action");
    this.projectButton.setText("← К проектам");
    this.title.setText("Проекты › " + this.editor.document().experiment().title());
  }

  private void rebuild() {
    this.expected = this.editor.document();
    final var experiment = this.expected.experiment();
    this.title.setText((this.projectReturn == null ? "" : "Проекты › ") + experiment.title());
    this.title.setTooltip(new Tooltip(experiment.title()));
    this.anchors.clear();
    this.sheet.getChildren().clear();
    this.timers.clear();
    this.participants.clear();
    final var stage = this.stage();
    this.scheme.getChildren().clear();
    if (stage == null) {
      this.scheme
          .getChildren()
          .addAll(
              note(
                  experiment.components().isEmpty()
                      ? "Начните со структур исходников. Затем задайте участников и условия реакции."
                      : "Структуры готовы. Выберите участников и задайте условия реакции."),
              this.reactionActions(experiment.components().isEmpty(), "stage"));
    } else {
      if (!stage.participants().isEmpty()) {
        this.scheme.getChildren().add(this.reactionActions(false, "edit-stage"));
      }
      stage.participants().forEach(p -> this.participants.add(p.componentId()));
      final FlowPane reaction = new FlowPane(12, 16);
      reaction.setId("desk-reaction");
      reaction.setMinWidth(0);
      reaction.setAlignment(javafx.geometry.Pos.TOP_LEFT);
      reaction.setRowValignment(javafx.geometry.VPos.CENTER);
      reaction.setMaxWidth(Double.MAX_VALUE);
      final List<CompoundCard> cards = new ArrayList<>();
      int count = 0;
      for (final var role :
          List.of(ReactionComponentRole.REACTANT, ReactionComponentRole.PRODUCT)) {
        final var entries = stage.participants().stream().filter(p -> p.role() == role).toList();
        for (int i = 0; i < entries.size(); i++) {
          final CompoundCard card =
              this.compound(this.component(entries.get(i).componentId()), role);
          cards.add(card);
          if (count++ == 0) {
            reaction.getChildren().add(card.box());
          } else {
            final Label separator =
                note(role == ReactionComponentRole.PRODUCT && i == 0 ? "→" : "+");
            separator.setStyle("-fx-font-size: 28;");
            separator.setMinWidth(32);
            separator.setPrefWidth(32);
            separator.setAlignment(javafx.geometry.Pos.CENTER);
            final HBox item = new HBox(10, separator, card.box());
            item.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            reaction.getChildren().add(item);
          }
        }
      }
      final Runnable fit = () -> fitReaction(reaction, cards);
      reaction.widthProperty().addListener((o, a, b) -> fit.run());
      fit.run();
      this.conditions.setText(conditionsText(stage.conditions()));
      this.conditions.setId("desk-conditions");
      this.conditions.getStyleClass().setAll("laboratory-reaction-conditions");
      this.scheme.getChildren().add(reaction);
      if (stage.participants().isEmpty()) {
        this.scheme
            .getChildren()
            .addAll(
                note("Нарисуйте исходники или выберите участников из уже созданных веществ."),
                this.reactionActions(true, "edit-stage"));
      }
      if (!this.conditions.getText().isBlank()) {
        this.scheme.getChildren().add(this.conditions);
      }
      final VBox details = new VBox(8);
      final String classificationText =
          ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure
              .LaboratoryReactionClassificationText.summary(stage.classification());
      if (!classificationText.isBlank()) {
        final Label classified = note(classificationText);
        classified.setId("desk-classification-summary");
        details.getChildren().add(classified);
      }
      final var auxiliaries =
          stage.participants().stream()
              .filter(
                  p ->
                      p.role() != ReactionComponentRole.REACTANT
                          && p.role() != ReactionComponentRole.PRODUCT)
              .map(
                  p -> {
                    final var c = this.component(p.componentId());
                    return c.molecule().displayName()
                        + " · "
                        + LaboratoryInspector.roleText(p.role())
                        + " · "
                        + amount(c.planned());
                  })
              .toList();
      if (!auxiliaries.isEmpty()) {
        final FlowPane reagents = new FlowPane(18, 6);
        auxiliaries.forEach(text -> reagents.getChildren().add(note(text)));
        details.getChildren().add(reagents);
      }
      if (!details.getChildren().isEmpty()) {
        this.scheme
            .getChildren()
            .add(disclosure("Участники и описание реакции", "reaction-details", details, false));
      }
    }
    if (stage != null && !stage.participants().isEmpty()) {
      this.scheme
          .getChildren()
          .add(
              new FlowPane(
                  8,
                  6,
                  this.button("Подготовить загрузку", "to-loading", () -> this.jump("Загрузка")),
                  this.button(
                      "Контроль и очистка",
                      "reaction-control",
                      () -> this.navigate("operations"))));
    }
    this.section("Реакция", this.scheme);
    this.loading.getChildren().clear();
    this.batch = null;
    if (stage != null
        && stage.participants().stream()
            .anyMatch(p -> p.role() == ReactionComponentRole.REACTANT)) {
      this.refreshQuantities();
      this.loading
          .getChildren()
          .add(
              disclosure(
                  "Сохранённый план и внесённые количества",
                  "quantity-table",
                  this.quantityTable,
                  false));
      this.batch =
          new LaboratoryBatchPlanPane(
              this.editor,
              stage,
              id -> this.navigate("properties:" + id),
              () -> {
                this.expected = this.editor.document();
                this.renderMethod();
                this.refreshQuantities();
                this.renderResults();
                this.completed(
                    this.editor.document().experiment().procedure().steps().isEmpty()
                        ? "План загрузки пересчитан."
                        : "План пересчитан. Связанные количества методики обновлены; начатые шаги сохраняют прежние значения.");
              },
              this::batchDiscarded);
      this.batch.naming(this::renameComponent);
      this.batch.embedded();
      this.batch.setId("desk-batch");
      this.batch.addEventFilter(
          javafx.event.ActionEvent.ACTION,
          event -> {
            if (event.getTarget() instanceof Node node
                && "chemistry-batch-apply".equals(node.getId())
                && this.outcome.pending()) {
              event.consume();
              this.attempt(this::guardOutcome);
            }
          });
      this.loading.getChildren().add(0, this.batch);
      this.batch.addEventHandler(
          javafx.event.ActionEvent.ACTION,
          event -> {
            if (event.getTarget() instanceof Node node
                && "chemistry-batch-reference".equals(node.getId())) {
              this.refreshQuantities();
            }
          });
      this.refreshQuantities();
    } else {
      this.loading
          .getChildren()
          .addAll(
              note("Добавьте исходник к стадии, чтобы рассчитывать загрузку."),
              this.button(
                  "Выбрать участников реакции",
                  "loading-edit-stage",
                  () -> this.navigate(stage == null ? "stage" : "edit-stage")));
    }
    final String oldLoad = id(this.loadFactComponent);
    this.loadFactComponent
        .getItems()
        .setAll(
            experiment.components().stream()
                .filter(
                    c ->
                        stage != null
                            && stage.participants().stream()
                                .anyMatch(
                                    p ->
                                        p.componentId().equals(c.id())
                                            && p.role() != ReactionComponentRole.PRODUCT))
                .map(c -> new Choice(c.id(), c.molecule().displayName()))
                .toList());
    select(this.loadFactComponent, oldLoad);
    if (this.loadFactComponent.getValue() == null) {
      this.loadFactComponent.getSelectionModel().selectFirst();
    }
    final VBox actualLoad =
        panel(
            "Фактическая загрузка",
            "Запишите реально внесённое количество. План автоматически сюда не переносится.",
            this.loadFactComponent,
            new FlowPane(
                8,
                8,
                LaboratoryQuantityFields.row(this.loadFact, this.loadFactUnit),
                this.primary("Записать загрузку", "record-loading-fact", this::recordLoadingFact)),
            this.button("К результату опыта", "loading-to-result", () -> this.jump("Результат")));
    this.section("Загрузка", columns(this.loading, actualLoad));
    this.renderMethod();
    this.section("Методика", this.method);
    final MenuButton quick = new MenuButton("Быстрая запись");
    quick.setId("desk-quick-notes");
    int phrase = 0;
    for (final String text :
        List.of("Взята проба", "Изменился цвет", "Выпал осадок", "Получена однородная смесь")) {
      quick
          .getItems()
          .add(
              this.menu(
                  text,
                  "quick-" + phrase++,
                  () -> {
                    this.observation.appendText(
                        (this.observation.getText().isBlank() ? "" : "\n") + text);
                    this.observation.requestFocus();
                  }));
    }
    this.noteOptions =
        disclosure(
            "Тип записи",
            "note-options",
            this.noteKind,
            this.noteKind.getValue() != LaboratoryEntry.Kind.OBSERVATION);
    final String priorNoteStep = id(this.noteStep);
    this.noteStep.getItems().setAll(new Choice("", "Общая запись опыта"));
    int stepIndex = 0;
    for (final var step : experiment.procedure().steps()) {
      stepIndex++;
      if (step.stageId() == null || Objects.equals(step.stageId(), this.stageId())) {
        this.noteStep
            .getItems()
            .add(new Choice(step.id(), "Шаг " + stepIndex + " · " + step.title()));
      }
    }
    select(this.noteStep, priorNoteStep);
    if (this.noteStep.getValue() == null) {
      this.noteStep.getSelectionModel().selectFirst();
    }
    visible(this.noteOptions, id(this.noteStep).isEmpty());
    this.renderRecent();
    this.observation.setPrefRowCount(5);
    this.observationForm
        .getChildren()
        .setAll(
            panel(
                "Записать наблюдение",
                "Что произошло: цвет, осадок, отбор пробы. Выберите шаг или оставьте общую запись. Ctrl+Enter — записать.",
                note("К чему относится запись"),
                this.noteStep,
                this.observation,
                new FlowPane(
                    8,
                    6,
                    this.primary("Записать наблюдение", "record-note", this::recordObservation),
                    quick),
                this.noteOptions));
    this.section(
        "Наблюдения",
        columns(
            this.journalNotes,
            panel(
                "Журнал опыта",
                "Наблюдения по шагам и общие записи. Дата в журнале — время внесения записи.",
                this.recent)));

    final String previous = id(this.factComponent);
    this.refreshing = true;
    this.factComponent
        .getItems()
        .setAll(
            experiment.components().stream()
                .filter(
                    c ->
                        stage != null
                            && stage.participants().stream()
                                .anyMatch(
                                    p ->
                                        p.componentId().equals(c.id())
                                            && p.role() == ReactionComponentRole.PRODUCT))
                .map(c -> new Choice(c.id(), c.molecule().displayName()))
                .toList());
    select(this.factComponent, previous);
    if (this.factComponent.getValue() == null && stage != null) {
      stage.participants().stream()
          .filter(p -> p.role() == ReactionComponentRole.PRODUCT)
          .findFirst()
          .ifPresent(p -> select(this.factComponent, p.componentId()));
    }
    if (this.factComponent.getValue() == null) {
      this.factComponent.getSelectionModel().selectFirst();
    }
    this.refreshing = false;
    this.factEntry =
        disclosure(
            "Выберите продукт и введите измеренное количество",
            "fact-entry",
            new VBox(
                10,
                this.factComponent,
                new FlowPane(
                    10,
                    8,
                    LaboratoryQuantityFields.row(this.fact, this.factUnit),
                    this.primary("Записать продукт", "record-fact", this::recordFact)),
                this.factHint),
            true);
    this.factEntry.setCollapsible(false);
    this.renderResults();
    this.section("Результат", this.results);
    this.factHint();
    this.restoreYieldReference();
    this.renderResults();
    this.jump(this.activeSection);
  }

  private Node reactionActions(final boolean drawingFirst, final String stageAction) {
    final FlowPane actions = new FlowPane(8, 6);
    if (drawingFirst) {
      actions
          .getChildren()
          .addAll(
              this.primary(
                  "Нарисовать исходники",
                  "draw-structures",
                  () -> this.navigate("draw-structures")),
              this.button("Ввести SMILES", "enter-smiles", () -> this.navigate("enter-smiles")));
      if (!this.editor.document().experiment().components().isEmpty()) {
        actions
            .getChildren()
            .add(
                this.button("Участники и условия", "edit-stage", () -> this.navigate(stageAction)));
      }
      final Hyperlink imported = new Hyperlink("Импорт реакции…");
      imported.setId("desk-import-reaction");
      imported.setOnAction(event -> this.attempt(() -> this.navigate("import-reaction")));
      return new VBox(6, actions, imported);
    }
    actions
        .getChildren()
        .addAll(
            this.primary("Участники и условия", "edit-stage", () -> this.navigate(stageAction)),
            this.button(
                "Редактировать структуры",
                "draw-structures",
                () -> this.navigate("draw-structures")));
    return actions;
  }

  void preferences(
      final ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryViewPreferences
          preferences,
      final java.util.function.Supplier<java.nio.file.Path> documentPath) {
    this.viewPreferences = preferences;
    this.documentPath = documentPath;
    this.restoreYieldReference();
    this.renderResults();
  }

  private String yieldScope() {
    return Objects.toString(this.documentPath.get(), "unsaved")
        + "\n"
        + this.editor.document().experiment().id()
        + "\n"
        + this.stageId();
  }

  private void rememberYieldReference() {
    final String reference = id(this.yieldReference);
    if (reference == null || this.stageId() == null) {
      return;
    }
    this.missingYieldReference = false;
    this.yieldReferences.put(this.yieldScope(), reference);
    if (this.viewPreferences != null && this.documentPath.get() != null) {
      this.viewPreferences.yieldReference(
          this.documentPath.get(),
          this.editor.document().experiment().id(),
          this.stageId(),
          reference);
      this.viewPreferences.save();
      if (!this.viewPreferences.warning().isBlank()) {
        this.status.setText(this.viewPreferences.warning());
      }
    }
  }

  private void restoreYieldReference() {
    final var stage = this.stage();
    final String stored =
        this.yieldReferences.getOrDefault(
            this.yieldScope(),
            this.viewPreferences == null
                ? null
                : this.viewPreferences.yieldReference(
                    this.documentPath.get(),
                    this.editor.document().experiment().id(),
                    this.stageId()));
    this.refreshing = true;
    try {
      this.yieldReference
          .getItems()
          .setAll(
              stage == null
                  ? List.of()
                  : stage.participants().stream()
                      .filter(p -> p.role() == ReactionComponentRole.REACTANT)
                      .map(
                          p ->
                              new Choice(
                                  p.componentId(),
                                  this.component(p.componentId()).molecule().displayName()))
                      .toList());
      this.yieldReference
          .getItems()
          .addFirst(new Choice(AUTO_YIELD, "Автоматически по фактической загрузке"));
      select(this.yieldReference, stored);
      this.missingYieldReference = stored != null && this.yieldReference.getValue() == null;
      if (stored == null) {
        this.yieldReference.getSelectionModel().selectFirst();
      }
    } finally {
      this.refreshing = false;
    }
  }

  private Node quantities(final LaboratoryReactionStage stage) {
    final GridPane table = new GridPane();
    table.setHgap(20);
    table.setVgap(14);
    table.setId("desk-quantities");
    table.addRow(0, heading("Участник"), heading("План / расчёт"), heading("Факт / источник"));
    int row = 1;
    final String reference =
        this.batch != null
            ? this.batch.referenceId()
            : stage.participants().stream()
                .filter(p -> p.role() == ReactionComponentRole.REACTANT)
                .map(LaboratoryStageParticipant::componentId)
                .findFirst()
                .orElse(null);
    final var masses = LaboratoryPlanStructureMasses.resolve(this.editor.document());
    final var report =
        reference == null
            ? null
            : LaboratoryStoichiometry.preparative(
                this.editor.document().experiment(),
                stage,
                LaboratoryStoichiometry.Basis.PLAN,
                reference,
                masses);
    for (final var p : stage.participants()) {
      final var c = this.component(p.componentId());
      final Label name = LaboratoryComponentNaming.label(c.molecule().displayName());
      name.setMaxWidth(480);
      Node plan = quantity(c.planned());
      if (p.role() == ReactionComponentRole.PRODUCT) {
        final VBox values = new VBox(4);
        values.setId("desk-product-plan-" + c.id());
        final var calculated =
            report == null
                ? null
                : report.rows().stream()
                    .filter(v -> v.component().id().equals(c.id()))
                    .findFirst()
                    .orElse(null);
        if (calculated != null && calculated.theoreticalMoles() != null) {
          final BigDecimal mass = LaboratoryBatchPlan.planMolarMass(c, masses);
          final var theoretical =
              new LaboratoryQuantity(
                  mass == null
                      ? calculated.theoreticalMoles().movePointRight(3)
                      : calculated.theoreticalMoles().multiply(mass),
                  mass == null ? LaboratoryQuantityUnit.MILLIMOLE : LaboratoryQuantityUnit.GRAM);
          final Label value = note(LaboratoryQuantityText.format(theoretical, true));
          value.setTooltip(
              new Tooltip(
                  LaboratoryQuantityText.format(theoretical, false)
                      + "\nПо сохранённой загрузке: "
                      + this.component(reference).molecule().displayName()));
          values.getChildren().addAll(value, note("Расчётное количество"));
        } else {
          values
              .getChildren()
              .add(note("Для теории нужны загрузка исходника и коэффициенты стадии."));
        }
        if (c.planned() != null) {
          values.getChildren().addAll(note("Задано вручную:"), quantity(c.planned()));
        }
        plan = values;
      }
      final VBox actual = new VBox(4);
      actual.setId("desk-actual-" + c.id());
      if (c.actual() == null) {
        actual.getChildren().add(note("Не записано"));
      } else {
        actual.getChildren().addAll(quantity(c.actual()), note(c.evidence().quantityLabel()));
        if (p.role() == ReactionComponentRole.PRODUCT) {
          final var yield =
              LaboratoryYieldSummary.calculate(
                  this.editor.document().experiment(),
                  stage,
                  this.resultReference(masses),
                  c.id(),
                  masses);
          actual.getChildren().add(this.yieldSummary(yield, "desk-yield-" + c.id()));
        }
      }
      table.addRow(
          row++, new VBox(3, name, note(LaboratoryInspector.roleText(p.role()))), plan, actual);
    }
    final ColumnConstraints name = new ColumnConstraints();
    name.setHgrow(Priority.ALWAYS);
    name.setMinWidth(140);
    name.setPercentWidth(46);
    final ColumnConstraints plan = new ColumnConstraints();
    plan.setPercentWidth(22);
    final ColumnConstraints actual = new ColumnConstraints();
    actual.setPercentWidth(32);
    table.getColumnConstraints().setAll(name, plan, actual);
    return table;
  }

  private Node yieldSummary(final LaboratoryYieldSummary.Value value, final String id) {
    final Label percent = note(value.text());
    percent.setStyle("-fx-font-weight: bold;");
    percent.setId(id);
    percent.setTooltip(new Tooltip(value.explanation()));
    final VBox box = new VBox(3, percent);
    if (value.percent() == null) {
      final String reason = value.explanation().lines().findFirst().orElse("");
      box.getChildren().add(note(reason));
    } else {
      if (!value.qualification().isBlank()) {
        box.getChildren().add(note(value.qualification()));
      }
      final Label basis =
          LaboratoryComponentNaming.label(value.explanation().lines().findFirst().orElse(""));
      basis.setTooltip(new Tooltip(value.explanation()));
      box.getChildren().add(basis);
    }
    return box;
  }

  private static void fitReaction(final FlowPane reaction, final List<CompoundCard> cards) {
    if (cards.isEmpty()) {
      return;
    }
    final double width = Math.max(300, reaction.getWidth() - 12);
    final double separators = Math.max(0, cards.size() - 1) * 54;
    final double minimum = cards.stream().mapToDouble(CompoundCard::minimum).sum() + separators;
    final double weight = cards.stream().mapToDouble(CompoundCard::weight).sum();
    final boolean singleRow = width >= minimum;
    final double height = Math.max(220, Math.min(290, width * .22));
    for (int i = 0; i < cards.size(); i++) {
      final var card = cards.get(i);
      final double available = Math.max(230, width - (i == 0 ? 0 : 42));
      final double size =
          singleRow
              ? card.minimum() + (width - minimum) * card.weight() / weight
              : Math.min(available, card.minimum() + (card.weight() > 1 ? 60 : 15));
      card.box().setMinWidth(Math.min(card.minimum(), size));
      card.box().setPrefWidth(size);
      card.box().setMaxWidth(size);
      card.canvas().setHeight(height);
    }
  }

  private CompoundCard compound(final LaboratoryComponent c, final ReactionComponentRole role) {
    final Canvas canvas = new Canvas(300, 260);
    canvas.setId("desk-preview-" + c.id());
    final var drawing = this.expected.layouts().get(c.id());
    final double[] view = {1, 0, 0, 0, 0, 0, 0};
    final double medianBond = medianBondLength(drawing);
    final Runnable paint =
        () -> {
          final var g = canvas.getGraphicsContext2D();
          LaboratoryPreviewTheme.clear(g);
          if (drawing == null) {
            return;
          }
          final var bounds = LaboratoryCanvas.previewBounds(drawing);
          final double scale =
              view[0]
                  * Math.min(
                      Math.min(1.1, 36 / medianBond),
                      Math.min(
                          (canvas.getWidth() - 30) / Math.max(1, bounds.getWidth()),
                          (canvas.getHeight() - 30) / Math.max(1, bounds.getHeight())));
          g.save();
          g.translate(
              canvas.getWidth() / 2 + view[1] - scale * (bounds.getMinX() + bounds.getMaxX()) / 2,
              canvas.getHeight() / 2 + view[2] - scale * (bounds.getMinY() + bounds.getMaxY()) / 2);
          g.scale(scale, scale);
          LaboratoryPreviewTheme.paint(g, drawing);
          g.restore();
        };
    LaboratoryPreviewTheme.track(canvas, paint);
    canvas.widthProperty().addListener((o, a, b) -> paint.run());
    canvas.heightProperty().addListener((o, a, b) -> paint.run());
    canvas.setOnScroll(
        e -> {
          if (e.isControlDown()) {
            view[0] = Math.max(.4, Math.min(5, view[0] * (e.getDeltaY() > 0 ? 1.15 : 1 / 1.15)));
            paint.run();
            e.consume();
          }
        });
    canvas.setOnMousePressed(
        e -> {
          view[3] = e.getX();
          view[4] = e.getY();
          view[5] = view[1];
          view[6] = view[2];
        });
    canvas.setOnMouseDragged(
        e -> {
          view[1] = view[5] + e.getX() - view[3];
          view[2] = view[6] + e.getY() - view[4];
          paint.run();
          e.consume();
        });
    canvas.setOnMouseClicked(
        e -> {
          if (e.getClickCount() == 2) {
            view[0] = 1;
            view[1] = 0;
            view[2] = 0;
            paint.run();
          }
        });
    Tooltip.install(
        canvas,
        new Tooltip("Тяните для перемещения · Ctrl+колесо — масштаб · двойной щелчок — вписать"));
    paint.run();
    final Label name = LaboratoryComponentNaming.label(c.molecule().displayName());
    name.getStyleClass().add("laboratory-compound-name");
    final Label roleName = note(LaboratoryInspector.roleText(role));
    roleName.getStyleClass().add("laboratory-compound-role");
    final Button properties =
        this.button(
            "Свойства", "properties-" + c.id(), () -> this.navigate("properties:" + c.id()));
    properties.getStyleClass().add("laboratory-link");
    properties.setTooltip(new Tooltip("Молярная масса, чистота, плотность и источник значений"));
    final Button rename =
        this.button("Изменить имя", "rename-" + c.id(), () -> this.renameComponent(c.id()));
    rename.setTooltip(new Tooltip("Своё название без интернета или поиск IUPAC по желанию."));
    rename.getStyleClass().add("laboratory-compound-rename");
    final FlowPane cardActions = new FlowPane(14, 6, properties, rename);
    cardActions.getStyleClass().add("laboratory-compound-actions");
    final VBox box = new VBox(8, new VBox(4, roleName, name), canvas, cardActions);
    box.setMaxHeight(Region.USE_PREF_SIZE);
    box.setPadding(new Insets(12));
    box.getStyleClass().add("laboratory-reaction-card");
    canvas
        .widthProperty()
        .bind(box.widthProperty().subtract(24).map(value -> Math.max(180, value.doubleValue())));
    final double extent =
        drawing == null
            ? 4
            : Math.max(
                    LaboratoryCanvas.previewBounds(drawing).getWidth(),
                    LaboratoryCanvas.previewBounds(drawing).getHeight())
                / medianBond;
    return new CompoundCard(
        box, canvas, extent > 6 ? 300 : 230, Math.max(1, Math.min(2.5, extent / 4)));
  }

  private static String conditionsText(
      final ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions value) {
    final List<String> parts = new ArrayList<>();
    if (value.temperature() != null) {
      final var t = value.temperature();
      parts.add(
          conditionNumber(t.value())
              + (t.unit()
                      == ru.pathcreator.vadim.quantum.chemistry.domain.unit.TemperatureUnit.CELSIUS
                  ? " °C"
                  : " K"));
    }
    if (value.duration() != null) {
      final var t = value.duration();
      parts.add(
          conditionNumber(t.value())
              + " "
              + switch (t.unit()) {
                case SECOND -> "с";
                case MINUTE -> "мин";
                case HOUR -> "ч";
                case DAY -> "сут";
              });
    }
    if (value.pressure() != null) {
      final var p = value.pressure();
      parts.add(
          conditionNumber(p.value())
              + " "
              + switch (p.unit()) {
                case PASCAL -> "Па";
                case ATMOSPHERE -> "атм";
                case BAR -> "бар";
              });
    }
    for (final var material : value.components()) {
      parts.add(
          LaboratoryInspector.roleText(material.role())
              + ": "
              + material.name()
              + (material.note() == null || material.note().isBlank()
                  ? ""
                  : " · " + material.note()));
    }
    final String additional =
        ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryReactionSearch
            .conditionsText(
                ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions.of(
                    null, null, null, List.of(), value.features(), value.note()));
    if (!additional.isBlank()) {
      parts.add(additional);
    }
    return String.join(" · ", parts);
  }

  private static String conditionNumber(final double value) {
    return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
  }

  private static double medianBondLength(
      final ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing drawing) {
    if (drawing == null) {
      return 44;
    }
    final var lengths = new ArrayList<Double>();
    for (final var bond : drawing.molecule().bonds()) {
      final var a =
          drawing.atoms().stream()
              .filter(v -> v.atomId().equals(bond.firstAtomId()))
              .findFirst()
              .orElseThrow()
              .point2D();
      final var b =
          drawing.atoms().stream()
              .filter(v -> v.atomId().equals(bond.secondAtomId()))
              .findFirst()
              .orElseThrow()
              .point2D();
      final double length = Math.hypot(a.x() - b.x(), a.y() - b.y());
      if (length > .001 && Double.isFinite(length)) {
        lengths.add(length);
      }
    }
    if (lengths.isEmpty()) {
      return 44;
    }
    Collections.sort(lengths);
    return lengths.get(lengths.size() / 2);
  }

  private void renderMethod() {
    this.method.getChildren().clear();
    this.timers.clear();
    final var e = this.editor.document().experiment();
    final var entries =
        e.procedure().steps().stream()
            .filter(s -> s.stageId() == null || Objects.equals(s.stageId(), id(this.stages)))
            .toList();
    if (entries.isEmpty()) {
      this.method
          .getChildren()
          .addAll(
              note("Добавьте шаги своей методики."),
              this.primary("Составить методику", "procedure", () -> this.navigate("procedure")));
      return;
    }
    final VBox steps = new VBox(10);
    int index = 0;
    for (final var step : entries.stream().limit(200).toList()) {
      final VBox row = new VBox(6);
      row.getStyleClass().add("laboratory-desk-step");
      final Label label = note((++index) + ". " + step.title());
      label.getStyleClass().add("desk-panel-heading");
      row.getChildren().add(label);
      if (!step.instruction().isBlank()) {
        row.getChildren().add(note(step.instruction()));
      }
      final List<String> details = new ArrayList<>();
      try {
        final var resolved = LaboratoryProcedureService.resolve(e, step);
        if (!resolved.componentName().isBlank()) {
          details.add("Плановая загрузка: " + amount(resolved.plannedQuantity()));
        }
      } catch (RuntimeException ex) {
        details.add(ex.getMessage());
      }
      if (step.temperatureC() != null) {
        details.add(step.temperatureC().stripTrailingZeros().toPlainString() + " °C");
      }
      if (step.durationSeconds() > 0) {
        details.add(duration(step.durationSeconds()));
      }
      details.add(
          switch (e.procedure().status(step.id())) {
            case PLANNED -> "В плане";
            case RUNNING -> "Выполняется";
            case COMPLETED -> "Выполнено";
            case SKIPPED -> "Пропущено";
          });
      row.getChildren().add(note(String.join(" · ", details)));
      final FlowPane actions =
          new FlowPane(
              8,
              6,
              this.button(
                  "Наблюдение к шагу", "observe-" + step.id(), () -> this.observeStep(step.id())));
      if (e.procedure().status(step.id()) == LaboratoryProcedure.Status.PLANNED) {
        actions
            .getChildren()
            .add(
                this.button(
                    "Отметить выполненным",
                    "record-complete-" + step.id(),
                    () -> this.event(step.id(), LaboratoryProcedure.Action.RECORD_COMPLETE)));
      }
      row.getChildren().add(actions);
      final var notes =
          e.procedure().eventsFor(step.id()).stream().filter(v -> !v.text().isBlank()).toList();
      if (!notes.isEmpty()) {
        final var last = notes.getLast();
        row.getChildren()
            .add(note("Запись от " + time(last.recordedAt()) + ": " + shortText(last.text(), 220)));
      }
      steps.getChildren().add(row);
    }
    final VBox execution =
        panel(
            "Проведение опыта",
            "Проводите сейчас — начните шаг и используйте таймер. Уже сделали — нажмите «Отметить выполненным» в списке шагов.",
            this.button(
                "Редактировать шаги и открыть историю",
                "procedure",
                () -> this.navigate("procedure-all")),
            this.button(
                "Журнал всех наблюдений",
                "to-observation",
                () -> {
                  this.jump("Наблюдения");
                  this.observation.requestFocus();
                }));
    final var running =
        entries.stream()
            .filter(step -> e.procedure().status(step.id()) == LaboratoryProcedure.Status.RUNNING)
            .toList();
    final var next =
        entries.stream()
            .filter(step -> e.procedure().status(step.id()) == LaboratoryProcedure.Status.PLANNED)
            .findFirst()
            .orElse(null);
    if (!running.isEmpty()) {
      for (var step : running) {
        execution.getChildren().add(this.stepCard(step, true));
      }
    } else if (next != null) {
      execution.getChildren().add(this.stepCard(next, true));
    } else {
      execution.getChildren().add(note("Все шаги завершены или пропущены."));
    }
    this.method
        .getChildren()
        .add(
            columns(
                panel(
                    "Методика · " + entries.size() + " шагов",
                    "Инструкция — что делать. Наблюдение — что произошло при выполнении.",
                    steps),
                new VBox(16, execution, this.methodNotes)));
    this.mountObservation();
    this.tick();
  }

  private Node stepCard(final LaboratoryProcedure.Step step, final boolean primary) {
    final var e = this.editor.document().experiment();
    final var state = e.procedure().status(step.id());
    final VBox card = new VBox(8);
    card.getStyleClass().add("laboratory-desk-step");
    card.setPadding(new Insets(12));
    final Label name = note(step.title());
    name.setStyle("-fx-font-weight: bold;");
    card.getChildren().add(name);
    try {
      final var resolved = LaboratoryProcedureService.resolve(e, step);
      final List<String> summary = new ArrayList<>();
      if (!resolved.componentName().isBlank()) {
        if (!step.title().equals(resolved.componentName())
            && !step.title().equals("Добавить: " + resolved.componentName())) {
          summary.add(resolved.componentName());
        }
        summary.add("План: " + amount(resolved.plannedQuantity()));
      }
      if (step.temperatureC() != null) {
        summary.add(step.temperatureC().stripTrailingZeros().toPlainString() + " °C");
      }
      if (step.durationSeconds() > 0) {
        summary.add(LaboratoryProcedureService.duration(step.durationSeconds()));
      }
      if (!summary.isEmpty()) {
        final Label details = note(String.join(" · ", summary));
        if (resolved.plannedQuantity() != null) {
          details.setTooltip(
              new Tooltip(LaboratoryQuantityText.format(resolved.plannedQuantity(), false)));
        }
        card.getChildren().add(details);
      }
      if (!step.instruction().isBlank()
          && !step.instruction().strip().equals(step.title().strip())) {
        card.getChildren().add(note(step.instruction()));
      }
      resolved.warnings().forEach(w -> card.getChildren().add(note(w)));
    } catch (final RuntimeException ex) {
      card.getChildren().add(note(ex.getMessage()));
    }
    final var history = e.procedure().eventsFor(step.id());
    final var corrected =
        history.stream()
            .map(LaboratoryProcedure.Event::correctsId)
            .filter(Objects::nonNull)
            .collect(java.util.stream.Collectors.toSet());
    history.reversed().stream()
        .filter(event -> event.actualQuantity() != null && !corrected.contains(event.id()))
        .findFirst()
        .ifPresent(
            event -> {
              final Label actual =
                  note(
                      "Последнее записанное количество: "
                          + amount(event.actualQuantity())
                          + " · "
                          + time(event.recordedAt()));
              actual.setId("desk-step-actual-" + step.id());
              actual.setTooltip(
                  new Tooltip(
                      LaboratoryQuantityText.format(event.actualQuantity(), false)
                          + " · "
                          + event.recordedAt()));
              card.getChildren().add(actual);
            });
    final Label timer = note("");
    this.timers.add(new Timer(timer, step.id()));
    final FlowPane actions = new FlowPane(8, 6);
    if (state == LaboratoryProcedure.Status.PLANNED
        || state == LaboratoryProcedure.Status.RUNNING) {
      final boolean start = state == LaboratoryProcedure.Status.PLANNED;
      final Button button =
          this.button(
              start ? "Начать шаг" : "Завершить шаг",
              (start ? "start-" : "finish-") + step.id(),
              () ->
                  this.event(
                      step.id(),
                      start
                          ? LaboratoryProcedure.Action.START
                          : LaboratoryProcedure.Action.COMPLETE));
      if (primary) {
        button.getStyleClass().add("laboratory-save");
      }
      actions.getChildren().add(button);
    } else {
      actions
          .getChildren()
          .add(note(state == LaboratoryProcedure.Status.COMPLETED ? "Выполнено" : "Пропущено"));
    }
    actions.getChildren().add(timer);
    card.getChildren().add(actions);
    return card;
  }

  private void event(final String step, final LaboratoryProcedure.Action event) {
    this.requireClean();
    this.current();
    this.editor.procedureEvent(
        this.expected,
        step,
        event,
        event == LaboratoryProcedure.Action.RECORD_COMPLETE
            ? "Выполнено ранее. Фактическое время начала и окончания не указано."
            : "",
        null,
        null,
        Instant.now());
    this.expected = this.editor.document();
    this.renderMethod();
    this.renderRecent();
    this.status.setText(
        event == LaboratoryProcedure.Action.START
            ? "Начало записано; количество и параметры шага зафиксированы."
            : event == LaboratoryProcedure.Action.RECORD_COMPLETE
                ? "Шаг отмечен выполненным. Время проведения не указано; сохраните опыт."
                : "Завершение записано с фактическим временем.");
  }

  private void recordObservation() {
    this.guardOutcome();
    this.current();
    if (this.conflict) {
      throw new IllegalArgumentException(
          "Сначала сбросьте конфликтующий черновик после проверки текста.");
    }
    if (this.observation.getText().isBlank()) {
      throw new IllegalArgumentException("Введите наблюдение.");
    }
    final String stepId = id(this.noteStep);
    if (stepId == null) {
      throw new IllegalArgumentException("Выберите, к чему относится наблюдение.");
    }
    if (stepId.isEmpty()) {
      this.editor.record(this.noteKind.getValue(), this.observation.getText().strip(), null);
    } else {
      this.editor.procedureEvent(
          this.expected,
          stepId,
          LaboratoryProcedure.Action.OBSERVE,
          this.observation.getText().strip(),
          null,
          null,
          Instant.now());
    }
    this.expected = this.editor.document();
    this.observation.clear();
    this.renderMethod();
    this.renderRecent();
    this.completed(
        stepId.isEmpty()
            ? "Общая запись сохранена в журнале. Сохраните опыт."
            : "Наблюдение записано к шагу и показано в журнале. Сохраните опыт.");
  }

  private void recordFact() {
    this.recordQuantity(this.factComponent, this.fact, this.factUnit);
  }

  private void recordLoadingFact() {
    this.recordQuantity(this.loadFactComponent, this.loadFact, this.loadFactUnit);
  }

  private void recordQuantity(
      final ComboBox<Choice> target,
      final LaboratoryDecimalField input,
      final ComboBox<LaboratoryQuantityUnit> unit) {
    this.guardOutcome();
    this.current();
    if (this.conflict) {
      throw new IllegalArgumentException("Проверьте конфликтующий черновик.");
    }
    if (this.batch != null && this.batch.pending()) {
      throw new IllegalArgumentException(
          "Примените или сбросьте пересчёт загрузки перед записью факта.");
    }
    if (target.getValue() == null) {
      throw new IllegalArgumentException("Выберите вещество.");
    }
    final var c = this.component(id(target));
    final var q = new LaboratoryQuantity(LaboratoryDecimalField.value(input), unit.getValue());
    this.editor.quantities(c.id(), c.planned(), q, LaboratoryEvidence.OBSERVED);
    this.expected = this.editor.document();
    input.clear();
    this.renderResults();
    this.factHint();
    this.renderRecent();
    if (this.batch != null) {
      this.batch.reload();
      this.refreshQuantities();
    }
    this.completed("Измеренное количество записано. План загрузки сохранён.");
  }

  String resultReference() {
    return this.resultReference(LaboratoryPlanStructureMasses.resolve(this.editor.document()));
  }

  private String resultReference(final Map<String, BigDecimal> masses) {
    final String selected = id(this.yieldReference);
    return AUTO_YIELD.equals(selected) || selected == null && !this.missingYieldReference
        ? LaboratoryYieldSummary.automaticReference(
                this.editor.document().experiment(), this.stage(), masses)
            .id()
        : selected;
  }

  private Node massExplanation(final LaboratoryComponent c, final Map<String, BigDecimal> masses) {
    final BigDecimal mass = LaboratoryBatchPlan.planMolarMass(c, masses);
    final Label label =
        note(
            c.molecule().formula().hillNotation()
                + (mass == null
                    ? " · проверьте структуру и свойства"
                    : " · M = "
                        + mass.stripTrailingZeros().toPlainString()
                        + " г/моль · "
                        + (c.properties().empty()
                            ? "рассчитано по структуре"
                            : "из свойств вещества")));
    label.setId("desk-result-mass-" + c.id());
    label.setTooltip(
        new Tooltip(
            "Молярная масса рассчитывается локально по составу нарисованной структуры. Интернет не нужен. Заданные свойства образца имеют приоритет."));
    return label;
  }

  private void renderResults() {
    final boolean showCalculation =
        this.calculationDetails != null && this.calculationDetails.isExpanded();
    final boolean showOutcome = this.outcomeDetails != null && this.outcomeDetails.isExpanded();
    this.results.getChildren().clear();
    this.outcome.refresh();
    final var e = this.editor.document().experiment();
    final var s = this.stage();
    if (s == null) {
      return;
    }
    final var masses = LaboratoryPlanStructureMasses.resolve(this.editor.document());
    final var automatic = LaboratoryYieldSummary.automaticReference(e, s, masses);
    final String reference = this.resultReference(masses);
    final boolean auto =
        AUTO_YIELD.equals(id(this.yieldReference))
            || id(this.yieldReference) == null && !this.missingYieldReference;
    final VBox input =
        panel(
            "1. Сколько продукта получили",
            "Введите массу выделенного продукта и нажмите «Записать продукт».",
            this.factEntry);
    final VBox result =
        panel(
            "2. Выход продукта",
            "Какая доля расчётного количества получена из фактической загрузки.");
    final VBox calculation = new VBox(10, note("Исходник для расчёта"), this.yieldReference);
    calculation
        .getChildren()
        .add(
            note(
                auto
                    ? automatic.explanation()
                    : "Основа выбрана вручную. Программа сохранит этот выбор для данного опыта."));
    if (this.missingYieldReference) {
      calculation
          .getChildren()
          .add(
              note(
                  "Ранее выбранный исходник отсутствует. Выберите автоматический расчёт или другой исходник."));
    }
    if (!auto && reference != null && automatic.id() != null && !reference.equals(automatic.id())) {
      calculation
          .getChildren()
          .add(
              note(
                  "Другой исходник ограничивает количество продукта: "
                      + this.component(automatic.id()).molecule().displayName()
                      + ". Выход ниже рассчитан по вашему выбору."));
    }
    if (reference != null) {
      final var c = this.component(reference);
      calculation
          .getChildren()
          .addAll(
              LaboratoryComponentNaming.label(c.molecule().displayName()),
              note(
                  "Фактически загружено: "
                      + (c.actual() == null ? "не записано" : amount(c.actual()))),
              this.massExplanation(c, masses));
    } else {
      result
          .getChildren()
          .add(
              note(
                  this.missingYieldReference
                      ? "Уточните исходник в разделе «Как рассчитан выход»."
                      : automatic.explanation()));
    }
    calculation
        .getChildren()
        .addAll(
            note(
                "Количество вещества = масса / молярная масса. Выход = полученное количество вещества / расчётное количество вещества × 100 %. Учитываются коэффициенты реакции и заданная чистота."),
            this.button(
                "Проверить фактическую загрузку",
                "result-to-loading",
                () -> {
                  this.jump("Загрузка");
                  if (this.loadFact.getText().isBlank() && reference != null) {
                    select(this.loadFactComponent, reference);
                  }
                  this.loadFact.requestFocus();
                }),
            this.button(
                "Участники и коэффициенты", "result-to-stage", () -> this.navigate("edit-stage")));
    final VBox properties =
        new VBox(
            8,
            note(
                "Чистоту, плотность и концентрацию указывайте, если они известны. Для массы обычного вещества молярная масса берётся из структуры автоматически."));
    if (reference != null) {
      properties
          .getChildren()
          .add(
              this.button(
                  "Свойства исходника…",
                  "yield-source-properties",
                  () -> this.navigate("properties:" + reference)));
    }
    final var products =
        s.participants().stream().filter(p -> p.role() == ReactionComponentRole.PRODUCT).toList();
    if (products.isEmpty()) {
      input
          .getChildren()
          .addAll(
              note("В этой стадии ещё нет продукта."),
              this.button(
                  "Указать продукт в участниках",
                  "result-add-product",
                  () -> this.navigate("edit-stage")));
    }
    for (final var p : products) {
      final var c = this.component(p.componentId());
      input
          .getChildren()
          .add(
              new VBox(
                  5,
                  LaboratoryComponentNaming.label(c.molecule().displayName()),
                  this.massExplanation(c, masses)));
      final VBox card = new VBox(9, LaboratoryComponentNaming.label(c.molecule().displayName()));
      card.getStyleClass().add("desk-result-summary");
      card.setId("desk-result-card-" + c.id());
      final Label obtained =
          note(
              c.actual() == null
                  ? "Количество пока не записано"
                  : c.evidence().quantityLabel() + ": " + amount(c.actual()));
      obtained.getStyleClass().add("desk-metric");
      card.getChildren().add(obtained);
      final var value = LaboratoryYieldSummary.calculate(e, s, reference, c.id(), masses);
      final Label percent = note(value.text());
      percent.setId("desk-result-yield-" + c.id());
      percent.getStyleClass().add("desk-metric");
      percent.setTooltip(new Tooltip(value.explanation()));
      card.getChildren().add(percent);
      if (!value.qualification().isBlank()) {
        card.getChildren().add(note(value.qualification()));
      }
      if (value.percent() == null && reference != null) {
        card.getChildren().add(note(value.explanation().split("\n", 2)[0]));
      }
      final BigDecimal
          theory = LaboratoryYieldSummary.theoreticalMoles(e, s, reference, c.id(), masses),
          mass = LaboratoryBatchPlan.planMolarMass(c, masses);
      if (theory != null) {
        final Label maximum =
            note(
                "Расчётное количество из записанной загрузки: "
                    + amount(
                        new LaboratoryQuantity(
                            mass == null ? theory.movePointRight(3) : theory.multiply(mass),
                            mass == null
                                ? LaboratoryQuantityUnit.MILLIMOLE
                                : LaboratoryQuantityUnit.GRAM)));
        maximum.setId("desk-result-theory-" + c.id());
        card.getChildren().add(maximum);
      }
      result.getChildren().add(card);
      properties
          .getChildren()
          .add(
              this.button(
                  "Свойства продукта: " + shortText(c.molecule().displayName(), 44) + "…",
                  "result-properties-" + c.id(),
                  () -> this.navigate("properties:" + c.id())));
    }
    if (reference != null) {
      result
          .getChildren()
          .add(
              LaboratoryComponentNaming.label(
                  (auto ? "Основа выбрана автоматически: " : "По выбранному исходнику: ")
                      + this.component(reference).molecule().displayName()));
    }
    calculation
        .getChildren()
        .add(
            disclosure(
                "Чистота и другие свойства · дополнительно",
                "result-properties",
                properties,
                false));
    this.calculationDetails =
        disclosure("Как рассчитан выход", "yield-details", calculation, showCalculation);
    result.getChildren().add(this.calculationDetails);
    this.results.getChildren().add(columns(input, result));
    this.results
        .getChildren()
        .add(
            panel(
                "3. Сохраните результат",
                "Записанное количество попадёт в опыт. Сохраните файл перед закрытием.",
                new FlowPane(
                    8,
                    6,
                    this.primary("Сохранить опыт", "result-save", () -> this.saveRequested(false)),
                    this.button("Открыть отчёт", "result-report", () -> this.navigate("report")))));
    this.outcomeDetails =
        disclosure(
            "Заключение об опыте · необязательно",
            "result-conclusion",
            this.outcome,
            showOutcome || this.outcome.pending());
    final VBox analytics =
        new VBox(
            10,
            note(
                "Здесь можно приложить измеренные спектры или отдельно посмотреть расчётный ЯМР. Прогноз не подтверждает состав полученного образца."));
    final long count =
        e.analyses().stream().filter(a -> this.participants.contains(a.componentId())).count()
            + e.analyticalEvidence().latest().stream()
                .filter(a -> this.participants.contains(a.componentId()))
                .count();
    analytics
        .getChildren()
        .addAll(
            note(count == 0 ? "Анализы ещё не добавлены." : "Аналитических записей: " + count),
            new FlowPane(
                8,
                6,
                this.button("Добавить анализ / спектр", "dossier", () -> this.navigate("evidence")),
                this.button("Все анализы", "open-analyses", () -> this.navigate("analyses"))));
    for (var p : products) {
      final Button prediction =
          this.button(
              "Расчётный ЯМР"
                  + (products.size() > 1
                      ? " · "
                          + shortText(this.component(p.componentId()).molecule().displayName(), 40)
                      : " · растворитель и примеси"),
              "expected-nmr-" + p.componentId(),
              () -> this.navigate("expected-nmr:" + p.componentId()));
      prediction.setWrapText(true);
      analytics.getChildren().add(prediction);
    }
    final VBox samplesPanel =
        new VBox(
            10,
            note(
                "Зарегистрируйте образец, если нужно хранить фракции или передать вещество в следующую стадию."));
    final var samples =
        e.samples().stream().filter(a -> this.participants.contains(a.componentId())).toList();
    for (final var sample : samples.stream().limit(8).toList()) {
      final var balance = sample.balance();
      samplesPanel
          .getChildren()
          .add(
              this.button(
                  sample.code() + " · " + balance.label() + " · " + amount(balance.remaining()),
                  "sample-" + sample.id(),
                  () -> this.navigate("sample:" + sample.id())));
    }
    samplesPanel
        .getChildren()
        .add(
            new FlowPane(
                8,
                6,
                this.button(
                    samples.isEmpty() ? "Зарегистрировать образец" : "Все образцы",
                    "register-sample",
                    () -> this.navigate(samples.isEmpty() ? "register-sample" : "samples")),
                this.button(
                    "Передать в следующую стадию", "transfer", () -> this.navigate("transfer"))));
    this.results
        .getChildren()
        .addAll(
            disclosure(
                "Спектры и анализы · " + (count == 0 ? "необязательно" : count + " записей"),
                "result-analytics",
                analytics,
                false),
            this.outcomeDetails,
            disclosure("Образцы и фракции · необязательно", "result-samples", samplesPanel, false));
  }

  private void observeStep(final String stepId) {
    if (!this.observation.getText().isBlank() && !Objects.equals(stepId, id(this.noteStep))) {
      throw new IllegalArgumentException(
          "Сначала запишите текущее наблюдение, затем выберите другой шаг.");
    }
    select(this.noteStep, stepId);
    this.jump("Методика");
    this.observation.requestFocus();
    javafx.application.Platform.runLater(
        () -> {
          if (this.observation.getScene() != null) {
            final var bounds =
                this.sheet.sceneToLocal(
                    this.observation.localToScene(this.observation.getBoundsInLocal()));
            this.main.setVvalue(
                Math.max(
                    0,
                    Math.min(
                        1,
                        bounds.getMinY()
                            / Math.max(
                                1,
                                this.sheet.getHeight()
                                    - this.main.getViewportBounds().getHeight()))));
          }
        });
  }

  private void mountObservation() {
    final VBox host = this.activeSection.equals("Методика") ? this.methodNotes : this.journalNotes;
    if (this.observationForm.getParent() == host) {
      return;
    }
    if (this.observationForm.getParent() instanceof Pane parent) {
      parent.getChildren().remove(this.observationForm);
    }
    host.getChildren().setAll(this.observationForm);
  }

  private void renderRecent() {
    this.recent.getChildren().clear();
    final var experiment = this.editor.document().experiment();
    final List<JournalItem> entries = new ArrayList<>();
    for (final var entry : experiment.journal()) {
      entries.add(
          new JournalItem(
              entry.id(),
              entry.recordedAt(),
              "Общая запись опыта",
              entry.kind().toString(),
              entry.text(),
              entry.correctsId() == null ? "" : "Исправляет: " + entry.correctsId(),
              entry.kind() == LaboratoryEntry.Kind.ACTION));
    }
    for (final var event : experiment.procedure().events()) {
      final var step = event.snapshot();
      final String stage =
          experiment.stages().stream()
              .filter(v -> v.id().equals(step.stageId()))
              .map(LaboratoryReactionStage::title)
              .findFirst()
              .orElse("Без стадии");
      final String text =
          event.text()
              + (event.actualQuantity() == null
                  ? ""
                  : "\nИзмерено в шаге: "
                      + LaboratoryQuantityText.format(event.actualQuantity(), true));
      entries.add(
          new JournalItem(
              event.id(),
              event.recordedAt(),
              stage + " · " + step.title(),
              event.action().toString(),
              text,
              "Шаг: "
                  + step.id()
                  + (event.correctsId() == null ? "" : "\nИсправляет: " + event.correctsId()),
              false));
    }
    entries.sort(Comparator.comparing(JournalItem::at).reversed());
    final var notes = entries.stream().filter(v -> !v.system()).toList();
    if (notes.isEmpty()) {
      this.recent
          .getChildren()
          .add(note("Наблюдений пока нет. Запишите их здесь или рядом с шагом методики."));
    }
    notes.stream()
        .limit(this.journalLimit)
        .forEach(v -> this.recent.getChildren().add(this.journalCard(v)));
    if (notes.size() > this.journalLimit) {
      this.recent
          .getChildren()
          .add(
              this.button(
                  "Показать более ранние записи",
                  "journal-more",
                  () -> {
                    this.journalLimit += 20;
                    this.renderRecent();
                  }));
    }
    final var system = entries.stream().filter(JournalItem::system).toList();
    if (!system.isEmpty()) {
      final VBox changes = new VBox(10);
      system.stream()
          .limit(this.journalLimit)
          .forEach(v -> changes.getChildren().add(this.journalCard(v)));
      if (system.size() > this.journalLimit) {
        changes
            .getChildren()
            .add(
                this.button(
                    "Показать более ранние изменения",
                    "journal-system-more",
                    () -> {
                      this.journalLimit += 20;
                      this.renderRecent();
                      ((TitledPane) this.recent.getChildren().getLast()).setExpanded(true);
                    }));
      }
      this.recent
          .getChildren()
          .add(disclosure("Изменения данных · " + system.size(), "journal-system", changes, false));
    }
  }

  private Node journalCard(final JournalItem entry) {
    final Label when = note(time(entry.at()) + " · " + entry.kind());
    when.getStyleClass().add("desk-panel-subtitle");
    final Label scope = note(entry.scope());
    scope.setStyle("-fx-font-weight: bold;");
    final Label summary = note(shortText(entry.text().replaceAll("\\s+", " ").strip(), 220));
    summary.setMinHeight(Region.USE_PREF_SIZE);
    summary.setMaxWidth(Double.MAX_VALUE);
    final TextArea original = new TextArea(entry.text());
    original.setEditable(false);
    original.setWrapText(true);
    original.setPrefRowCount(5);
    original.setId("desk-journal-original-" + entry.id());
    final Label metadata = note("Время записи: " + entry.at() + "\n" + entry.metadata());
    final TitledPane detail =
        disclosure(
            "Полная запись", "journal-" + entry.id(), new VBox(8, original, metadata), false);
    final VBox card = new VBox(7, when, scope);
    if (!entry.text().isBlank()) {
      card.getChildren().add(summary);
    }
    card.getChildren().add(detail);
    card.setMinWidth(0);
    card.setMaxWidth(Double.MAX_VALUE);
    card.setMinHeight(Region.USE_PREF_SIZE);
    card.setPadding(new Insets(12));
    card.getStyleClass().add("laboratory-desk-step");
    return card;
  }

  private static String shortText(final String text, final int limit) {
    return text.codePointCount(0, text.length()) <= limit
        ? text
        : text.substring(0, text.offsetByCodePoints(0, limit - 1)).stripTrailing() + "…";
  }

  private void factHint() {
    final var c =
        id(this.factComponent) == null
            ? null
            : this.editor.document().experiment().components().stream()
                .filter(v -> v.id().equals(id(this.factComponent)))
                .findFirst()
                .orElse(null);
    this.factHint.setText(
        c == null
            ? ""
            : c.actual() == null
                ? "Внесите только измеренное количество."
                : "Ранее: "
                    + amount(c.actual())
                    + " · "
                    + c.evidence().quantityLabel()
                    + ". Новое значение — собственное наблюдение.");
  }

  private void tick() {
    if (this.disposed) {
      return;
    }
    final var p = this.editor.document().experiment().procedure();
    for (final var t : this.timers) {
      final var start = p.latestStart(t.stepId());
      if (start == null || p.status(t.stepId()) != LaboratoryProcedure.Status.RUNNING) {
        t.label().setText("");
        continue;
      }
      final long elapsed =
          Math.max(0, java.time.Duration.between(start.recordedAt(), Instant.now()).getSeconds());
      final long planned = start.snapshot().durationSeconds();
      t.label()
          .setText(
              "Прошло "
                  + duration(elapsed)
                  + (planned == 0
                      ? ""
                      : elapsed < planned
                          ? " · осталось " + duration(planned - elapsed)
                          : " · плановое время истекло"));
    }
  }

  private void refreshQuantities() {
    final var stage = this.stage();
    this.quantityTable.getChildren().clear();
    if (stage != null) {
      this.quantityTable.getChildren().add(this.quantities(stage));
    }
  }

  private static VBox panel(final String title, final String description, final Node... content) {
    final Label heading = note(title);
    heading.getStyleClass().add("desk-panel-heading");
    final Label subtitle = note(description);
    subtitle.getStyleClass().add("desk-panel-subtitle");
    final VBox panel = new VBox(12, heading, subtitle);
    panel.getChildren().addAll(content);
    panel.getStyleClass().add("desk-panel");
    panel.setMinWidth(0);
    panel.setMaxWidth(Double.MAX_VALUE);
    return panel;
  }

  private static GridPane columns(final Node left, final Node right) {
    final GridPane grid = new GridPane();
    grid.setHgap(18);
    grid.setVgap(18);
    grid.setMinWidth(0);
    grid.setMaxWidth(Double.MAX_VALUE);
    grid.getStyleClass().add("desk-two-column");
    grid.add(left, 0, 0);
    grid.add(right, 1, 0);
    GridPane.setHgrow(left, Priority.ALWAYS);
    GridPane.setHgrow(right, Priority.ALWAYS);
    GridPane.setValignment(left, javafx.geometry.VPos.TOP);
    GridPane.setValignment(right, javafx.geometry.VPos.TOP);
    GridPane.setFillHeight(left, false);
    GridPane.setFillHeight(right, false);
    final Runnable fit =
        () -> {
          final boolean wide = grid.getWidth() >= 1100;
          GridPane.setColumnIndex(right, wide ? 1 : 0);
          GridPane.setRowIndex(right, wide ? 0 : 1);
          final ColumnConstraints first = new ColumnConstraints();
          first.setMinWidth(0);
          first.setPercentWidth(wide ? 62 : 100);
          grid.getColumnConstraints().setAll(first);
          if (wide) {
            final ColumnConstraints second = new ColumnConstraints();
            second.setMinWidth(0);
            second.setPercentWidth(38);
            grid.getColumnConstraints().add(second);
          }
        };
    grid.widthProperty().addListener((o, a, b) -> fit.run());
    fit.run();
    return grid;
  }

  private void section(final String name, final Node content) {
    final VBox box = new VBox(12, content);
    box.setPadding(new Insets(12));
    box.getStyleClass().add("laboratory-desk-section");
    this.anchors.put(name, box);
    visible(box, name.equals(this.activeSection));
    this.sheet.getChildren().add(box);
  }

  private void jump(final String name) {
    if (!this.anchors.containsKey(name)) {
      return;
    }
    this.activeSection = name;
    this.sheet.getChildren().setAll(this.anchors.get(name));
    if (name.equals("Загрузка")) {
      this.refreshQuantities();
    }
    this.anchors.forEach((key, node) -> visible(node, key.equals(name)));
    this.navigation.forEach(
        (key, button) ->
            button.pseudoClassStateChanged(
                javafx.css.PseudoClass.getPseudoClass("selected"), key.equals(name)));
    this.mountObservation();
    this.main.setVvalue(0);
    this.applyCss();
    this.layout();
  }

  private static TitledPane disclosure(
      final String title, final String id, final Node content, final boolean expanded) {
    final TitledPane pane = new TitledPane(title, content);
    pane.setId("desk-" + id);
    pane.setAnimated(false);
    pane.setExpanded(expanded);
    pane.getStyleClass().add("laboratory-desk-details");
    return pane;
  }

  private Button primary(final String text, final String id, final Runnable action) {
    final Button button = this.button(text, id, action);
    button.getStyleClass().add("laboratory-save");
    return button;
  }

  private MenuItem menu(final String text, final String id, final Runnable action) {
    final MenuItem item = new MenuItem(text);
    item.setId("desk-" + id);
    item.setOnAction(e -> this.attempt(action));
    return item;
  }

  private void renameComponent(final String componentId) {
    this.attempt(
        () -> {
          this.requireClean();
          this.current();
          final var before = this.viewState();
          if (LaboratoryComponentNaming.show(
              this.editor,
              componentId,
              this.getScene() == null ? null : this.getScene().getWindow())) {
            this.rebuild();
            this.restoreView(before);
            this.completed("Название изменено. Сохраните опыт; интернет для этого не нужен.");
          }
        });
  }

  private void navigate(final String action) {
    this.requireClean();
    this.action.accept(action);
  }

  private void current() {
    if (!this.editor.document().equals(this.expected)) {
      throw new IllegalArgumentException(
          "Опыт изменился. Сохраните введённый текст и обновите рабочий экран.");
    }
  }

  private void requireClean() {
    if (this.pending()) {
      this.refreshPending(true);
      throw new IllegalArgumentException(this.pendingMessage());
    }
  }

  boolean pending() {
    return this.conflict
        || this.outcome.pending()
        || !this.observation.getText().isBlank()
        || !this.fact.getText().isBlank()
        || !this.loadFact.getText().isBlank()
        || this.batch != null && this.batch.pending();
  }

  String stageId() {
    return id(this.stages);
  }

  record ViewState(String stageId, String section, double scroll, String yieldReferenceId) {}

  ViewState viewState() {
    return new ViewState(
        this.stageId(), this.activeSection, this.main.getVvalue(), id(this.yieldReference));
  }

  void restoreView(final ViewState view) {
    if (Objects.equals(this.stageId(), view.stageId())
        && view.yieldReferenceId() != null
        && this.yieldReference.getItems().stream()
            .anyMatch(c -> c.id().equals(view.yieldReferenceId()))) {
      this.refreshing = true;
      try {
        select(this.yieldReference, view.yieldReferenceId());
      } finally {
        this.refreshing = false;
      }
      this.renderResults();
    }
    this.jump(view.section());
    this.main.setVvalue(Math.max(0, Math.min(1, view.scroll())));
  }

  private List<String> pendingSections() {
    final List<String> sections = new ArrayList<>();
    if (!this.loadFact.getText().isBlank() || this.batch != null && this.batch.pending()) {
      sections.add("Загрузка");
    }
    if (!this.observation.getText().isBlank()) {
      sections.add("Наблюдения");
    }
    if (!this.fact.getText().isBlank() || this.outcome.pending()) {
      sections.add("Результат");
    }
    return sections;
  }

  private String pendingMessage() {
    final var sections = this.pendingSections();
    return (sections.isEmpty()
            ? ""
            : "Незаписанный ввод: "
                + sections.stream()
                    .map(v -> "«" + v + "»")
                    .collect(java.util.stream.Collectors.joining(", "))
                + ". ")
        + (this.conflict
            ? "Черновик относится к другой версии опыта. Проверьте сохранённый текст перед сбросом."
            : "Запишите его или отмените ввод в указанном разделе.");
  }

  private void refreshPending(final boolean reveal) {
    if (!this.pending()) {
      visible(this.discard, false);
      if (this.guardStatus) {
        this.status.setText(this.guardPrefix.strip());
        this.guardPrefix = "";
        this.guardStatus = false;
      }
      return;
    }
    if (reveal || this.discard.isVisible()) {
      final FlowPane actions = new FlowPane(8, 6);
      for (final String section : this.pendingSections()) {
        final String key =
            section.equals("Загрузка")
                ? "loading"
                : section.equals("Наблюдения") ? "observation" : "result";
        actions
            .getChildren()
            .add(
                this.button(
                    "К вводу: " + section.toLowerCase(Locale.ROOT),
                    "return-" + key,
                    () -> this.returnToInput(section)));
      }
      if (this.conflict && actions.getChildren().isEmpty()) {
        actions
            .getChildren()
            .add(
                this.button(
                    "Вернуться к черновику",
                    "return-draft",
                    () -> this.returnToInput(this.activeSection)));
      }
      actions
          .getChildren()
          .addAll(
              this.button(
                  "Отбросить ввод и выйти",
                  "discard",
                  () -> {
                    this.reset();
                    this.finish();
                  }),
              this.button("Остаться здесь", "continue", () -> visible(this.discard, false)));
      this.discard.getChildren().setAll(actions);
      visible(this.discard, true);
    }
    if (reveal || this.guardStatus) {
      if (reveal) {
        this.guardPrefix = "";
      }
      this.status.setText(this.guardPrefix + this.pendingMessage());
      this.guardStatus = true;
    }
  }

  private void returnToInput(final String section) {
    this.jump(section);
    if (section.equals("Наблюдения")) {
      this.observation.requestFocus();
    } else if (section.equals("Результат")) {
      if (this.outcome.pending()) {
        this.showOutcomeInput();
      } else {
        this.factEntry.setExpanded(true);
        this.fact.requestFocus();
      }
    } else if (!this.loadFact.getText().isBlank()) {
      this.loadFact.requestFocus();
    } else if (this.batch != null) {
      final Node input = this.batch.lookup("#chemistry-batch-value");
      if (input != null) {
        input.requestFocus();
      }
    }
    this.refreshPending(false);
  }

  private void outcomeChanged() {
    if (this.outcome.pending()) {
      this.outcomeInputStatus = true;
      this.guardPrefix = "";
      this.guardStatus = true;
      this.refreshPending(false);
    } else if (this.outcomeInputStatus) {
      this.outcomeInputStatus = false;
      this.completed("Незаписанных изменений исхода нет.");
    } else {
      this.refreshPending(false);
    }
  }

  private void showOutcomeInput() {
    if (this.outcomeDetails != null) {
      this.outcomeDetails.setExpanded(true);
    }
    this.outcome.focusInput();
  }

  private void guardOutcome() {
    if (this.outcome.pending()) {
      this.jump("Результат");
      this.showOutcomeInput();
      throw new IllegalArgumentException(
          "Сначала запишите заключение об опыте либо отмените его ввод. Остальные поля сохранены.");
    }
  }

  private void beforeOutcomeWrite() {
    this.current();
    if (this.conflict) {
      throw new IllegalArgumentException(
          "Черновик относится к другой версии опыта. Проверьте сохранённый ввод перед сбросом.");
    }
    if (this.batch != null && this.batch.pending()) {
      throw new IllegalArgumentException(
          "Сначала примените или сбросьте пересчёт загрузки. Ввод исхода сохранён.");
    }
  }

  private void outcomeWritten() {
    this.expected = this.editor.document();
    if (this.batch != null) {
      this.batch.reload();
    }
    this.renderResults();
    this.renderRecent();
    this.completed(
        "Заключение об опыте записано. Количества и анализы не изменены; сохраните опыт.");
  }

  private void completed(final String message) {
    this.guardStatus = false;
    this.guardPrefix = "";
    this.refreshPending(false);
    this.status.setText(message);
    if (this.pending()) {
      this.guardPrefix = message + " ";
      this.guardStatus = true;
      this.refreshPending(false);
    }
  }

  void saving(final Consumer<Boolean> save) {
    this.save = Objects.requireNonNull(save, "save");
  }

  void saveRequested(final boolean saveAs) {
    this.attempt(
        () -> {
          this.requireClean();
          this.save.accept(saveAs);
        });
  }

  void saved() {
    this.completed(this.pending() ? "Файл опыта сохранён." : "Опыт сохранён.");
    this.rememberYieldReference();
  }

  @Override
  protected void finish() {
    if (this.projectReturn == null) {
      super.finish();
    } else if (this.canClose()) {
      this.projectReturn.run();
    }
  }

  @Override
  boolean canClose() {
    if (this.pending()) {
      this.refreshPending(true);
      return false;
    }
    this.refreshPending(false);
    return true;
  }

  private void batchDiscarded() {
    this.completed("Ввод загрузки сброшен. Показан сохранённый в опыте план.");
  }

  private void reset() {
    this.outcome.reset();
    this.observation.clear();
    this.fact.clear();
    this.loadFact.clear();
    this.conflict = false;
    this.conflictDraft = null;
    visible(this.discard, false);
    this.rebuild();
    this.completed("Ввод сброшен. Показан сохранённый в опыте план.");
  }

  void draft(final Properties p, final boolean restore) {
    if (!restore && this.conflictDraft != null) {
      this.conflictDraft.forEach(p::put);
      return;
    }
    if (!restore) {
      p.setProperty("desk.source", new LaboratoryFileCodec().write(this.expected));
      p.setProperty("desk.stage", Objects.toString(id(this.stages), ""));
      p.setProperty("desk.section", this.activeSection);
      p.setProperty("desk.note", this.observation.getText());
      p.setProperty("desk.noteStep", Objects.toString(id(this.noteStep), ""));
      p.setProperty("desk.kind", this.noteKind.getValue().name());
      p.setProperty("desk.loadFact", this.loadFact.getText());
      p.setProperty("desk.loadComponent", Objects.toString(id(this.loadFactComponent), ""));
      p.setProperty("desk.loadUnit", this.loadFactUnit.getValue().name());
      p.setProperty("desk.fact", this.fact.getText());
      p.setProperty("desk.component", Objects.toString(id(this.factComponent), ""));
      p.setProperty("desk.unit", this.factUnit.getValue().name());
      p.setProperty("desk.reference", Objects.toString(id(this.yieldReference), ""));
      if (this.batch != null && this.batch.pending()) {
        final Properties b = new Properties();
        this.batch.draft(b, false);
        b.forEach((k, v) -> p.put("desk.batch." + k, v));
      }
      this.outcome.draft(p, false);
      return;
    }
    final String legacyId = p.getProperty("desk.component", "");
    final boolean legacyLoading =
        !p.containsKey("desk.loadFact")
            && !p.getProperty("desk.fact", "").isBlank()
            && this.editor.document().experiment().stages().stream()
                .filter(st -> st.id().equals(p.getProperty("desk.stage")))
                .flatMap(st -> st.participants().stream())
                .anyMatch(
                    part ->
                        part.componentId().equals(legacyId)
                            && part.role() != ReactionComponentRole.PRODUCT);
    this.refreshing = true;
    select(this.stages, p.getProperty("desk.stage"));
    this.refreshing = false;
    this.rebuild();
    select(this.noteStep, p.getProperty("desk.noteStep", ""));
    this.observation.setText(p.getProperty("desk.note", ""));
    this.fact.setText(legacyLoading ? "" : p.getProperty("desk.fact", ""));
    if (!legacyLoading) {
      select(this.factComponent, p.getProperty("desk.component"));
    }
    this.noteKind.setValue(LaboratoryEntry.Kind.valueOf(p.getProperty("desk.kind", "OBSERVATION")));
    this.factUnit.setValue(LaboratoryQuantityUnit.valueOf(p.getProperty("desk.unit", "GRAM")));
    this.loadFact.setText(
        legacyLoading ? p.getProperty("desk.fact", "") : p.getProperty("desk.loadFact", ""));
    if (legacyLoading || p.containsKey("desk.loadComponent")) {
      select(
          this.loadFactComponent, legacyLoading ? legacyId : p.getProperty("desk.loadComponent"));
    }
    this.loadFactUnit.setValue(
        LaboratoryQuantityUnit.valueOf(
            p.getProperty(legacyLoading ? "desk.unit" : "desk.loadUnit", "GRAM")));
    this.refreshing = true;
    try {
      select(this.yieldReference, p.getProperty("desk.reference"));
    } finally {
      this.refreshing = false;
    }
    this.outcome.draft(p, true);
    this.renderResults();
    final Properties b = new Properties();
    p.stringPropertyNames().stream()
        .filter(k -> k.startsWith("desk.batch."))
        .forEach(k -> b.setProperty(k.substring(11), p.getProperty(k)));
    if (!b.isEmpty() && this.batch != null) {
      this.batch.draft(b, true);
    }
    this.conflict =
        id(this.noteStep) == null
            || !new LaboratoryFileCodec().write(this.expected).equals(p.getProperty("desk.source"))
            || !Objects.equals(
                Objects.toString(id(this.stages), ""), p.getProperty("desk.stage", ""))
            || !Objects.equals(
                Objects.toString(
                    id(legacyLoading ? this.loadFactComponent : this.factComponent), ""),
                p.getProperty("desk.component", ""))
            || p.containsKey("desk.loadComponent")
                && !Objects.equals(
                    Objects.toString(id(this.loadFactComponent), ""),
                    p.getProperty("desk.loadComponent", ""));
    if (this.factEntry != null) {
      this.factEntry.setExpanded(true);
    }
    if (this.noteOptions != null) {
      this.noteOptions.setExpanded(this.noteKind.getValue() != LaboratoryEntry.Kind.OBSERVATION);
    }
    this.jump(
        !b.isEmpty() || !this.loadFact.getText().isBlank()
            ? "Загрузка"
            : !this.fact.getText().isBlank() || this.outcome.pending()
                ? "Результат"
                : !this.observation.getText().isBlank()
                    ? "Наблюдения"
                    : p.getProperty("desk.section", "Реакция"));
    if (this.conflict) {
      this.conflictDraft = new Properties();
      p.stringPropertyNames().stream()
          .filter(k -> k.startsWith("desk."))
          .forEach(k -> this.conflictDraft.setProperty(k, p.getProperty(k)));
      this.status.setText(
          "Черновик относится к другой версии опыта. Проверьте и сохраните текст перед сбросом ввода.");
    }
  }

  @Override
  void dispose() {
    this.clock.stop();
    super.dispose();
  }

  private LaboratoryReactionStage stage() {
    final String id = id(this.stages);
    return this.editor.document().experiment().stages().stream()
        .filter(s -> s.id().equals(id))
        .findFirst()
        .orElse(null);
  }

  private LaboratoryComponent component(final String id) {
    return this.editor.document().experiment().components().stream()
        .filter(c -> c.id().equals(id))
        .findFirst()
        .orElseThrow();
  }

  private Button button(final String name, final String id, final Runnable action) {
    final Button b = new Button(name);
    b.setId("desk-" + id);
    b.setOnAction(e -> this.attempt(action));
    return b;
  }

  private void attempt(final Runnable action) {
    try {
      action.run();
    } catch (final RuntimeException ex) {
      this.status.setText(
          ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
    }
  }

  private static Label heading(final String name) {
    final Label l = note(name);
    l.setStyle("-fx-font-size: 19; -fx-font-weight: bold;");
    return l;
  }

  private static String id(final ComboBox<Choice> box) {
    return box.getValue() == null ? null : box.getValue().id();
  }

  private static void select(final ComboBox<Choice> box, final String id) {
    box.setValue(box.getItems().stream().filter(c -> c.id().equals(id)).findFirst().orElse(null));
  }

  private static String amount(final LaboratoryQuantity q) {
    return q == null ? "Не задано" : number(q.value()) + " " + q.unit();
  }

  private static String number(final BigDecimal value) {
    final var shown = value.round(new java.math.MathContext(4));
    return (shown.compareTo(value) == 0 ? "" : "≈ ") + shown.stripTrailingZeros().toPlainString();
  }

  private static Label quantity(final LaboratoryQuantity value) {
    final Label label = note(amount(value));
    if (value != null) {
      label.setTooltip(new Tooltip(LaboratoryQuantityText.format(value, false)));
    }
    return label;
  }

  private static String time(final Instant time) {
    return DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(ZoneId.systemDefault()).format(time);
  }

  private static String duration(final long seconds) {
    return seconds / 3600
        + ":"
        + String.format(Locale.ROOT, "%02d:%02d", seconds / 60 % 60, seconds % 60);
  }

  private static void visible(final Node node, final boolean value) {
    node.setVisible(value);
    node.setManaged(value);
  }
}

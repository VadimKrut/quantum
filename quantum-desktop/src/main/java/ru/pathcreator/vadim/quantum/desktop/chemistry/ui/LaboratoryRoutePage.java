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
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.transform.Scale;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryFileCodec;

/** A visual native synthesis plan with explicit branch links and backwards quantity planning. */
final class LaboratoryRoutePage extends LaboratoryComputationPage {
  private record Choice(String id, String name) {
    public String toString() {
      return this.name;
    }
  }

  private final LaboratoryEditor editor;
  private final Runnable save;
  private Consumer<Boolean> history;
  private BooleanSupplier fileBusy = () -> false;
  private final Consumer<String> quantities;
  private LaboratoryDrawing expected;
  private final ComboBox<Choice> stage = new ComboBox<>(),
      reference = new ComboBox<>(),
      product = new ComboBox<>(),
      source = new ComboBox<>(),
      destination = new ComboBox<>(),
      input = new ComboBox<>(),
      target = new ComboBox<>();
  private final TextField yield = new TextField(), recovery = new TextField("100");
  private final LaboratoryDecimalField amount = new LaboratoryDecimalField();
  private final ComboBox<LaboratoryQuantityUnit> unit = new ComboBox<>();
  private final CheckBox enabled = new CheckBox("Стадия участвует в расчёте"),
      linkEnabled = new CheckBox("Связь участвует в расчёте");
  private final ListView<LaboratorySynthesisRoute.Link> links = new ListView<>();
  private final Label summary = note(""),
      problems = note(""),
      planSummary = note(""),
      sourceSelection = note("");
  private final VBox results = new VBox(12), discard = new VBox(8);
  private final Pane map = new Pane();
  private final Group scaledMap = new Group(this.map), mapFrame = new Group(this.scaledMap);
  private final ScrollPane graph = new ScrollPane(this.mapFrame);
  private final Slider zoom = new Slider(.1, 1.6, .85);
  private final TabPane tabs = new TabPane();
  private final Button calculate, applyPlan, useStage, undo, redo;
  private final ExecutorService worker =
      Executors.newSingleThreadExecutor(
          r -> {
            final Thread t = new Thread(r, "laboratory-route");
            t.setDaemon(true);
            return t;
          });
  private Future<?> task;
  private long generation;
  private boolean planApplied;
  private boolean loading,
      dirty,
      conflict,
      nodeDirty,
      linkDirty,
      targetDirty,
      initialFit,
      fitting,
      userView;

  private record NodeForm(
      String stage, String reference, String product, Object yield, boolean enabled) {}

  private record LinkForm(
      String link,
      String source,
      String destination,
      String input,
      Object recovery,
      boolean enabled) {}

  private record TargetForm(String stage, Object amount, LaboratoryQuantityUnit unit) {}

  private NodeForm initialNode;
  private LinkForm initialLink;
  private TargetForm initialTarget;
  private Properties conflictDraft;
  private String editingLink;
  private LaboratoryRoutePlanning.Result calculated;
  private LaboratoryDrawing calculationSource;

  LaboratoryRoutePage(
      final LaboratoryEditor editor,
      final String stageId,
      final Runnable save,
      final Consumer<String> quantities,
      final Runnable close) {
    super("Маршрут синтеза", close);
    this.editor = editor;
    this.save = save;
    this.history =
        forward -> {
          if (forward) {
            editor.redo();
          } else {
            editor.undo();
          }
        };
    this.quantities = quantities;
    this.expected = editor.document();
    this.setId("chemistry-route-page");
    this.stage.setId("route-stage");
    this.reference.setId("route-reference");
    this.product.setId("route-product");
    this.source.setId("route-source");
    this.destination.setId("route-destination");
    this.input.setId("route-input");
    this.target.setId("route-target");
    this.yield.setId("route-yield");
    this.yield.setPromptText("Введите ожидаемый выход");
    this.recovery.setId("route-recovery");
    this.amount.setId("route-amount");
    this.amount.setPromptText("Нужное количество продукта");
    this.unit.setId("route-unit");
    this.enabled.setSelected(true);
    this.linkEnabled.setSelected(true);
    this.enabled.setId("route-enabled");
    this.linkEnabled.setId("route-link-enabled");
    this.unit
        .getItems()
        .setAll(
            Arrays.stream(LaboratoryQuantityUnit.values())
                .filter(u -> !u.dimension().equals("volume"))
                .toList());
    this.unit.setValue(LaboratoryQuantityUnit.GRAM);
    this.graph.setTooltip(
        new Tooltip(
            "Перетаскивайте карту мышью. Ctrl + колесо — масштаб. Нажмите стадию, чтобы изменить её параметры."));
    this.graph.setPannable(true);
    this.graph.setId("route-map");
    this.graph.setMinSize(0, 0);
    this.map.setStyle("-fx-background-color: #f4f7fa;");
    this.zoom.setId("route-zoom");
    this.zoom.setPrefWidth(180);
    this.zoom
        .valueProperty()
        .addListener(
            (o, a, b) -> {
              this.scaledMap
                  .getTransforms()
                  .setAll(new Scale(b.doubleValue(), b.doubleValue(), 0, 0));
              if (!this.fitting) {
                this.userView = true;
              }
            });
    this.scaledMap.getTransforms().setAll(new Scale(.85, .85, 0, 0));
    this.graph
        .viewportBoundsProperty()
        .addListener(
            o -> {
              if (!userView
                  && graph.getViewportBounds().getWidth() > 80
                  && graph.getViewportBounds().getHeight() > 80
                  && map.getPrefWidth() > 0) {
                fitting = true;
                fit();
                fitting = false;
                initialFit = true;
              }
            });
    this.graph.setOnMousePressed(e -> userView = true);
    this.graph.addEventFilter(
        ScrollEvent.SCROLL,
        e -> {
          if (e.isControlDown()) {
            this.zoom.setValue(this.zoom.getValue() * (e.getDeltaY() > 0 ? 1.12 : .89));
            e.consume();
          }
        });
    final FlowPane graphTools =
        new FlowPane(
            10,
            6,
            note("Масштаб"),
            this.zoom,
            this.button("Вписать", "fit", this::fit),
            this.button(
                "Загрузка выбранной стадии",
                "quantities",
                () -> {
                  this.requireClean();
                  if (this.stage.getValue() == null) {
                    throw new IllegalArgumentException("Выберите стадию на карте.");
                  }
                  this.quantities.accept(this.stage.getValue().id());
                }));
    final VBox graphBody = new VBox(8, this.summary, graphTools, this.graph, this.problems);
    graphBody.setPadding(new Insets(10));
    VBox.setVgrow(this.graph, Priority.ALWAYS);
    this.useStage = this.button("Использовать эту стадию", "use-stage", this::useStage);
    this.useStage.setTooltip(
        new Tooltip(
            "Включить существующие исходящие связи этой стадии и отключить другие источники тех же исходников. Остальные ветви сохраняются."));
    final VBox selectionAction = new VBox(6, this.useStage, this.sourceSelection);
    selectionAction.visibleProperty().bind(this.useStage.visibleProperty());
    selectionAction.managedProperty().bind(selectionAction.visibleProperty());
    final VBox stageForm =
        new VBox(
            10,
            heading("Стадия и ожидаемый выход"),
            field("Стадия опыта", this.stage),
            selectionAction,
            field("Опорный исходник", this.reference),
            field("Получаемый промежуточный или конечный продукт", this.product),
            field("Ожидаемый выход, %", this.yield),
            this.enabled,
            new FlowPane(
                8,
                6,
                this.button("Записать стадию", "put-node", this::putNode),
                this.button("Убрать из маршрута", "remove-node", this::removeNode)),
            note(
                "Выход задаётся вами для планирования. Измеренный выход опыта хранится отдельно."));
    stageForm.setPadding(new Insets(14));
    this.links.setId("route-links");
    this.links.setPrefHeight(180);
    this.links.setPlaceholder(note("Свяжите продукт одной стадии с исходником следующей."));
    this.links.setCellFactory(
        list ->
            new ListCell<>() {
              {
                this.setWrapText(true);
                this.prefWidthProperty().bind(list.widthProperty().subtract(24));
                this.setMaxWidth(USE_PREF_SIZE);
              }

              protected void updateItem(
                  final LaboratorySynthesisRoute.Link l, final boolean empty) {
                super.updateItem(l, empty);
                this.setText(
                    empty || l == null
                        ? null
                        : stageName(l.sourceStageId())
                            + " → "
                            + stageName(l.targetStageId())
                            + "\n"
                            + componentName(l.targetComponentId())
                            + " · доходит "
                            + percent(l.transferFraction())
                            + "%"
                            + (l.enabled() ? "" : " · альтернатива выключена"));
              }
            });
    final VBox linkForm =
        new VBox(
            10,
            heading("Передача промежуточного продукта"),
            this.links,
            field("Продукт из стадии", this.source),
            field("Использовать в стадии", this.destination),
            field("Какой исходник заменить этим продуктом", this.input),
            field("До следующей стадии доходит, %", this.recovery),
            this.linkEnabled,
            new FlowPane(
                8,
                6,
                this.button("Записать связь", "put-link", this::putLink),
                this.button("Новая связь", "new-link", this::newLink),
                this.button("Удалить связь", "remove-link", this::removeLink)),
            note(
                "Структуры должны соответствовать. Потери при передаче увеличивают потребность предыдущей стадии. Для существующей альтернативы выберите её на вкладке «Стадии» и нажмите «Использовать эту стадию». При ручной записи новой связи сначала отключите прежний источник того же исходника."));
    linkForm.setPadding(new Insets(14));
    final TabPane forms =
        new TabPane(new Tab("Стадии", scroll(stageForm)), new Tab("Связи", scroll(linkForm)));
    forms.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    forms.setId("route-forms");
    this.calculate = this.button("Рассчитать загрузки", "calculate", this::calculate);
    this.calculate.getStyleClass().add("laboratory-primary-action");
    this.applyPlan = this.button("Применить количества к опыту", "apply", this::applyPlan);
    this.applyPlan.getStyleClass().add("laboratory-primary-action");
    this.applyPlan.setDisable(true);
    this.applyPlan.visibleProperty().bind(this.applyPlan.disabledProperty().not());
    this.applyPlan.managedProperty().bind(this.applyPlan.visibleProperty());
    final FlowPane targetRow =
        new FlowPane(
            12,
            10,
            field("Что получить", this.target),
            field("Количество продукта", LaboratoryQuantityFields.row(this.amount, this.unit)));
    final ScrollPane resultScroll = scroll(this.results);
    resultScroll.setFitToHeight(false);
    this.results.setPadding(new Insets(4, 0, 12, 0));
    final VBox planning =
        new VBox(
            12,
            heading("Сколько исходников понадобится?"),
            note("Задайте конечный продукт — программа рассчитает загрузки всех связанных стадий."),
            targetRow,
            new FlowPane(
                8,
                6,
                this.calculate,
                this.button("Изменить стадии и связи", "edit-route", this::editRoute)),
            this.planSummary,
            resultScroll,
            this.applyPlan);
    planning.setPadding(new Insets(16));
    VBox.setVgrow(resultScroll, Priority.ALWAYS);
    final VBox editing =
        new VBox(
            10,
            note(
                "Ожидаемые выходы и потери задаются для плана. Фактические результаты опыта хранятся отдельно."),
            forms);
    editing.setPadding(new Insets(12));
    VBox.setVgrow(forms, Priority.ALWAYS);
    this.tabs
        .getTabs()
        .setAll(
            new Tab("Расчёт загрузок", planning),
            new Tab("Схема маршрута", graphBody),
            new Tab("Стадии и связи", editing));
    this.tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    this.tabs.setId("route-tabs");
    this.tabs.setStyle("-fx-open-tab-animation: NONE; -fx-close-tab-animation: NONE;");
    this.setCenter(this.tabs);
    this.discard
        .getChildren()
        .setAll(
            note("В форме остались незаписанные параметры."),
            new FlowPane(
                8,
                6,
                this.button(
                    "Отбросить ввод и выйти",
                    "discard",
                    () -> {
                      this.resetInput();
                      this.finish();
                    }),
                this.button("Продолжить работу", "continue", () -> visible(this.discard, false))));
    visible(this.discard, false);
    this.undo = this.button("Отменить", "undo", () -> this.history(false));
    this.redo = this.button("Повторить", "redo", () -> this.history(true));
    this.undo.setTooltip(
        new Tooltip(
            "Отменить последнее действие опыта — Ctrl+Z. В текстовом поле отменяется только редактирование текста."));
    this.redo.setTooltip(
        new Tooltip("Повторить отменённое действие опыта — Ctrl+Y или Ctrl+Shift+Z."));
    final Button saveButton = this.button("Сохранить опыт", "save", this::requestSave);
    saveButton.setTooltip(new Tooltip("Сохранить файл опыта — Ctrl+S."));
    final VBox bottom =
        new VBox(
            8,
            this.discard,
            new FlowPane(
                8,
                6,
                saveButton,
                this.undo,
                this.redo,
                this.button("Сбросить ввод", "reset", this::resetInput)),
            this.status);
    bottom.setPadding(new Insets(10, 16, 12, 16));
    this.setBottom(bottom);
    this.stage
        .valueProperty()
        .addListener(
            (o, before, after) -> {
              if (this.loading) {
                return;
              }
              if (this.dirty) {
                this.loading = true;
                this.stage.setValue(before);
                this.loading = false;
                this.status.setText("Запишите параметры или сбросьте ввод перед сменой стадии.");
                return;
              }
              this.loadNode(id(this.stage));
            });
    this.destination
        .valueProperty()
        .addListener(
            o -> {
              if (!this.loading) {
                this.loading = true;
                this.loadInputs(null);
                this.loading = false;
                this.touch("link");
              }
            });
    this.links
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (o, before, after) -> {
              if (this.loading) {
                return;
              }
              if (this.dirty) {
                this.loading = true;
                this.links.getSelectionModel().select(before);
                this.loading = false;
                this.status.setText("Сначала запишите или сбросьте ввод.");
                return;
              }
              this.loadLink(after);
            });
    this.yield.textProperty().addListener(o -> this.touch("node"));
    this.recovery.textProperty().addListener(o -> this.touch("link"));
    this.amount.valueRevisionProperty().addListener(o -> this.touch("target"));
    for (final ComboBox<?> c : List.of(this.reference, this.product)) {
      c.valueProperty().addListener(o -> this.touch("node"));
    }
    for (final ComboBox<?> c : List.of(this.source, this.input)) {
      c.valueProperty().addListener(o -> this.touch("link"));
    }
    for (final ComboBox<?> c : List.of(this.target, this.unit)) {
      c.valueProperty().addListener(o -> this.touch("target"));
    }
    this.enabled.selectedProperty().addListener(o -> this.touch("node"));
    this.linkEnabled.selectedProperty().addListener(o -> this.touch("link"));
    this.reload(stageId);
  }

  private Button button(final String text, final String id, final Runnable run) {
    final Button b = new Button(text);
    b.setId("route-" + id);
    b.setMinWidth(Region.USE_PREF_SIZE);
    b.setOnAction(
        e -> {
          try {
            run.run();
          } catch (final RuntimeException x) {
            this.status.setText(message(x));
          }
        });
    return b;
  }

  private static String message(final Throwable e) {
    return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
  }

  private static Label heading(final String s) {
    final Label l = note(s);
    l.setStyle("-fx-font-size: 17px; -fx-font-weight: bold;");
    return l;
  }

  private static VBox field(final String text, final Node n) {
    final VBox v = new VBox(5, note(text), n);
    v.setMinWidth(0);
    if (n instanceof ComboBox<?> c) {
      c.setMaxWidth(Double.MAX_VALUE);
      c.setPrefWidth(280);
    }
    return v;
  }

  private static String id(final ComboBox<Choice> c) {
    return c.getValue() == null ? null : c.getValue().id();
  }

  private static void select(final ComboBox<Choice> c, final String id) {
    c.setValue(c.getItems().stream().filter(v -> v.id().equals(id)).findFirst().orElse(null));
  }

  private static void visible(final Node n, final boolean visible) {
    n.setVisible(visible);
    n.setManaged(visible);
  }

  private static String percent(final BigDecimal fraction) {
    return fraction.movePointRight(2).stripTrailingZeros().toPlainString();
  }

  private String stageName(final String id) {
    return this.expected.experiment().stages().stream()
        .filter(s -> s.id().equals(id))
        .map(LaboratoryReactionStage::title)
        .findFirst()
        .orElse("Стадия отсутствует");
  }

  private String componentName(final String id) {
    return this.expected.experiment().components().stream()
        .filter(c -> c.id().equals(id))
        .map(c -> c.molecule().displayName())
        .findFirst()
        .orElse("Компонент отсутствует");
  }

  /** Compare the actual input, including an exact decimal restored by local Undo. */
  private static Object inputValue(final TextField field) {
    try {
      final BigDecimal value = LaboratoryDecimalField.value(field);
      if (value != null && value.precision() <= 100 && Math.abs((long) value.scale()) <= 100) {
        return value.stripTrailingZeros();
      }
    } catch (final NumberFormatException | ArithmeticException ignored) {
    }
    return field.getText();
  }

  private NodeForm nodeForm() {
    return new NodeForm(
        id(this.stage),
        id(this.reference),
        id(this.product),
        inputValue(this.yield),
        this.enabled.isSelected());
  }

  private LinkForm linkForm() {
    return new LinkForm(
        this.editingLink,
        id(this.source),
        id(this.destination),
        id(this.input),
        inputValue(this.recovery),
        this.linkEnabled.isSelected());
  }

  private TargetForm targetForm() {
    return new TargetForm(id(this.target), inputValue(this.amount), this.unit.getValue());
  }

  private void refreshDirty() {
    if (this.loading) {
      return;
    }
    this.nodeDirty = this.initialNode != null && !this.initialNode.equals(this.nodeForm());
    this.linkDirty = this.initialLink != null && !this.initialLink.equals(this.linkForm());
    this.targetDirty = this.initialTarget != null && !this.initialTarget.equals(this.targetForm());
    this.dirty = this.nodeDirty || this.linkDirty || this.targetDirty;
    if (!this.dirty && !this.conflict) {
      visible(this.discard, false);
    }
  }

  private void touch(final String section) {
    if (!this.loading) {
      this.refreshDirty();
      this.invalidate();
    }
  }

  private String pendingMessage() {
    if (this.conflict) {
      return "Опыт изменился. Сверьте сохранённый ввод или нажмите «Сбросить ввод», чтобы загрузить текущую версию.";
    }
    final List<String> actions = new ArrayList<>();
    if (this.nodeDirty) {
      actions.add("Параметры стадии изменены — нажмите «Записать стадию».");
    }
    if (this.linkDirty) {
      actions.add("Параметры связи изменены — нажмите «Записать связь».");
    }
    if (this.targetDirty) {
      actions.add("Цель изменена — нажмите «Рассчитать загрузки».");
    }
    return String.join(" ", actions) + " Либо нажмите «Сбросить ввод».";
  }

  private void resetInput() {
    if (this.running) {
      throw new IllegalArgumentException("Дождитесь расчёта маршрута.");
    }
    this.conflict = false;
    this.conflictDraft = null;
    this.reload(id(this.stage));
    visible(this.discard, false);
    this.status.setText(
        "Незаписанный ввод сброшен. Показаны текущие параметры маршрута; файл опыта не изменён.");
  }

  private void invalidate() {
    this.planApplied = false;
    this.calculated = null;
    this.calculationSource = null;
    this.applyPlan.setDisable(true);
    this.calculate.getStyleClass().remove("laboratory-primary-action");
    this.calculate.getStyleClass().add("laboratory-primary-action");
    this.results
        .getChildren()
        .setAll(
            note(
                this.expected.experiment().route().nodes().isEmpty()
                    ? "Маршрут ещё не задан. Откройте «Стадии и связи», выберите продукт и ожидаемый выход каждой стадии."
                    : "После расчёта здесь появятся количества для каждой стадии. Это план, а не запись выполненного опыта."));
    this.planSummary.setText("");
  }

  private void editRoute() {
    this.tabs.getSelectionModel().select(2);
  }

  private void requireClean() {
    if (this.running) {
      throw new IllegalArgumentException("Дождитесь расчёта маршрута.");
    }
    if (this.pending()) {
      throw new IllegalArgumentException(this.pendingMessage());
    }
  }

  void historyActions(final Consumer<Boolean> action, final BooleanSupplier fileBusy) {
    this.history = Objects.requireNonNull(action);
    this.fileBusy = Objects.requireNonNull(fileBusy);
    this.historyControls();
  }

  private void historyControls() {
    if (this.undo != null) {
      this.undo.setDisable(this.running || this.fileBusy.getAsBoolean() || !this.editor.canUndo());
      this.redo.setDisable(this.running || this.fileBusy.getAsBoolean() || !this.editor.canRedo());
    }
  }

  @Override
  protected void busy(final boolean value) {
    super.busy(value);
    this.historyControls();
  }

  private void guardOperation() {
    if (this.disposed) {
      throw new IllegalArgumentException("Страница маршрута закрыта.");
    }
    if (this.fileBusy.getAsBoolean()) {
      throw new IllegalArgumentException("Дождитесь завершения файловой операции.");
    }
    this.requireClean();
    this.current();
  }

  private void requestSave() {
    this.guardOperation();
    this.save.run();
  }

  private void history(final boolean forward) {
    if (this.pending()) {
      this.status.setText(
          this.pendingMessage()
              + " Затем можно "
              + (forward ? "повторить" : "отменить")
              + " действие опыта.");
      return;
    }
    this.guardOperation();
    if (!(forward ? this.editor.canRedo() : this.editor.canUndo())) {
      this.status.setText(forward ? "Нет действия для повтора." : "Нет действия для отмены.");
      return;
    }
    final var before = this.editor.document();
    this.history.accept(forward);
    this.documentChanged();
    this.historyControls();
    if (!before.equals(this.editor.document())) {
      this.status.setText(
          (forward ? "Действие повторено." : "Последнее действие отменено.")
              + " Для обновления расчёта нажмите «Рассчитать загрузки».");
    }
  }

  boolean shortcut(final KeyEvent event) {
    if (!event.isShortcutDown() || event.isAltDown()) {
      return false;
    }
    final boolean historyKey = event.getCode() == KeyCode.Z || event.getCode() == KeyCode.Y;
    if (historyKey) {
      for (Node n = event.getTarget() instanceof Node target ? target : null;
          n != null;
          n = n.getParent()) {
        if (n instanceof TextInputControl) {
          return false;
        }
      }
    }
    if (!historyKey && !(event.getCode() == KeyCode.S && !event.isShiftDown())) {
      return false;
    }
    try {
      if (historyKey) {
        this.history(event.getCode() == KeyCode.Y || event.isShiftDown());
      } else {
        this.requestSave();
      }
    } catch (final RuntimeException error) {
      this.status.setText(message(error));
    }
    event.consume();
    return true;
  }

  private void current() {
    this.refreshDirty();
    if (this.conflict || !this.expected.equals(this.editor.document())) {
      this.conflict = true;
      this.invalidate();
      throw new IllegalArgumentException(
          "Опыт изменился. Сохраните введённые параметры в черновике и откройте маршрут заново.");
    }
  }

  boolean pending() {
    this.refreshDirty();
    return this.dirty || this.conflict;
  }

  boolean canClose() {
    if (this.running) {
      this.status.setText("Дождитесь завершения расчёта.");
      return false;
    }
    if (this.pending()) {
      visible(this.discard, true);
      return false;
    }
    return true;
  }

  void dispose() {
    ++this.generation;
    if (this.task != null) {
      this.task.cancel(true);
    }
    this.worker.shutdownNow();
    super.dispose();
  }

  private void reload(final String stageId) {
    this.loading = true;
    this.expected = this.editor.document();
    this.dirty = false;
    this.nodeDirty = false;
    this.linkDirty = false;
    this.targetDirty = false;
    this.invalidate();
    this.stage
        .getItems()
        .setAll(
            this.expected.experiment().stages().stream()
                .map(s -> new Choice(s.id(), s.title()))
                .toList());
    select(this.stage, stageId);
    if (this.stage.getValue() == null) {
      this.stage.getSelectionModel().selectFirst();
    }
    final var route = this.expected.experiment().route();
    final var options =
        route.nodes().stream().map(n -> new Choice(n.stageId(), stageName(n.stageId()))).toList();
    this.source.getItems().setAll(options);
    this.destination.getItems().setAll(options);
    this.target.getItems().setAll(options);
    select(this.target, route.targetStageId());
    if (this.target.getValue() == null && options.size() == 1) {
      this.target.getSelectionModel().selectFirst();
    }
    LaboratoryDecimalField.display(
        this.amount, route.targetQuantity() == null ? null : route.targetQuantity().value());
    this.unit.setValue(
        route.targetQuantity() == null
            ? LaboratoryQuantityUnit.GRAM
            : route.targetQuantity().unit());
    this.links.getItems().setAll(route.links());
    this.editingLink = null;
    this.initialTarget = this.targetForm();
    this.loading = false;
    this.loadNode(id(this.stage));
    this.loadLink(null);
    this.render();
    this.historyControls();
  }

  private void loadNode(final String id) {
    this.loading = true;
    this.reference.getItems().clear();
    this.product.getItems().clear();
    final var s =
        this.expected.experiment().stages().stream()
            .filter(v -> v.id().equals(id))
            .findFirst()
            .orElse(null);
    if (s != null) {
      this.reference
          .getItems()
          .setAll(
              s.participants().stream()
                  .filter(p -> p.role() == ReactionComponentRole.REACTANT)
                  .map(p -> new Choice(p.componentId(), componentName(p.componentId())))
                  .toList());
      this.product
          .getItems()
          .setAll(
              s.participants().stream()
                  .filter(p -> p.role() == ReactionComponentRole.PRODUCT)
                  .map(p -> new Choice(p.componentId(), componentName(p.componentId())))
                  .toList());
    }
    final var n =
        this.expected.experiment().route().nodes().stream()
            .filter(v -> v.stageId().equals(id))
            .findFirst()
            .orElse(null);
    this.yield.setText(n == null ? "" : n.expectedYieldPercent().toPlainString());
    this.enabled.setSelected(n == null || n.enabled());
    select(this.reference, n == null ? null : n.referenceComponentId());
    select(this.product, n == null ? null : n.productComponentId());
    if (n == null) {
      if (this.reference.getItems().size() == 1) {
        this.reference.getSelectionModel().selectFirst();
      }
      if (this.product.getItems().size() == 1) {
        this.product.getSelectionModel().selectFirst();
      }
    }
    this.loading = false;
    final long outgoing =
        this.expected.experiment().route().links().stream()
            .filter(l -> l.sourceStageId().equals(id))
            .count();
    this.useStage.setVisible(outgoing > 0);
    this.useStage.setManaged(outgoing > 0);
    this.sourceSelection.setText(
        "Будут выбраны существующие связи: "
            + outgoing
            + ". Другие источники тех же исходников будут отключены.");
    this.initialNode = this.nodeForm();
    this.refreshDirty();
  }

  private void loadInputs(final String selected) {
    this.input
        .getItems()
        .setAll(
            this.expected.experiment().stages().stream()
                .filter(s -> s.id().equals(id(this.destination)))
                .flatMap(s -> s.participants().stream())
                .filter(p -> p.role() == ReactionComponentRole.REACTANT)
                .map(p -> new Choice(p.componentId(), componentName(p.componentId())))
                .toList());
    select(this.input, selected);
    if (selected == null && this.input.getItems().size() == 1) {
      this.input.getSelectionModel().selectFirst();
    }
  }

  private void loadLink(final LaboratorySynthesisRoute.Link link) {
    this.loading = true;
    this.editingLink = link == null ? null : link.id();
    select(this.source, link == null ? null : link.sourceStageId());
    select(this.destination, link == null ? null : link.targetStageId());
    this.loadInputs(link == null ? null : link.targetComponentId());
    this.recovery.setText(link == null ? "100" : percent(link.transferFraction()));
    this.linkEnabled.setSelected(link == null || link.enabled());
    this.initialLink = this.linkForm();
    this.loading = false;
    this.refreshDirty();
  }

  private void newLink() {
    this.requireClean();
    this.loading = true;
    this.links.getSelectionModel().clearSelection();
    this.loading = false;
    this.loadLink(null);
  }

  private static BigDecimal decimal(final String text, final String label) {
    try {
      final var v = new BigDecimal(text.strip().replace(',', '.'));
      if (v.precision() > 34 || Math.abs((long) v.scale()) > 100) {
        throw new NumberFormatException();
      }
      return v;
    } catch (final NumberFormatException e) {
      throw new IllegalArgumentException(label + ": укажите число в допустимом диапазоне.");
    }
  }

  private static String chosen(final ComboBox<Choice> choice, final String what) {
    final String id = id(choice);
    if (id == null) {
      throw new IllegalArgumentException("Выберите: " + what + ".");
    }
    return id;
  }

  private void putNode() {
    this.current();
    if (this.running || this.linkDirty || this.targetDirty) {
      throw new IllegalArgumentException(
          "Сначала запишите связь или рассчитайте изменённую цель; либо сбросьте ввод.");
    }
    final var node =
        new LaboratorySynthesisRoute.Node(
            chosen(this.stage, "стадия"),
            chosen(this.reference, "опорный исходник"),
            chosen(this.product, "продукт"),
            decimal(this.yield.getText(), "Ожидаемый выход"),
            this.enabled.isSelected());
    final var old = this.expected.experiment().route();
    final var nodes = new ArrayList<>(old.nodes());
    final int position =
        java.util.stream.IntStream.range(0, nodes.size())
            .filter(i -> nodes.get(i).stageId().equals(node.stageId()))
            .findFirst()
            .orElse(-1);
    if (position < 0) {
      nodes.add(node);
    } else {
      nodes.set(position, node);
    }
    this.editor.route(
        this.expected,
        new LaboratorySynthesisRoute(
            nodes, old.links(), old.targetStageId(), old.targetQuantity()));
    this.reload(node.stageId());
    this.status.setText("Параметры стадии записаны в маршрут.");
  }

  private void useStage() {
    this.requireClean();
    this.current();
    final String selected = chosen(this.stage, "стадия");
    final var next = LaboratoryRouteSelection.useStage(this.expected, selected);
    if (next.route().equals(this.expected.experiment().route())) {
      this.status.setText("Эта стадия и её исходящие связи уже выбраны.");
      return;
    }
    this.editor.route(this.expected, next.route());
    this.reload(selected);
    this.status.setText(
        "Источник выбран: "
            + this.stageName(selected)
            + ". Связей: "
            + next.selectedLinkIds().size()
            + "; заменено источников: "
            + next.disabledLinkIds().size()
            + ". Пересчитайте загрузки. Действие можно отменить целиком.");
  }

  private void removeNode() {
    this.requireClean();
    this.current();
    final String id = chosen(this.stage, "стадия");
    final var old = this.expected.experiment().route();
    this.editor.route(
        this.expected,
        new LaboratorySynthesisRoute(
            old.nodes().stream().filter(n -> !n.stageId().equals(id)).toList(),
            old.links().stream()
                .filter(l -> !l.sourceStageId().equals(id) && !l.targetStageId().equals(id))
                .toList(),
            Objects.equals(id, old.targetStageId()) ? null : old.targetStageId(),
            Objects.equals(id, old.targetStageId()) ? null : old.targetQuantity()));
    this.reload(id);
    this.status.setText("Стадия убрана из маршрута; сама реакция сохранена в опыте.");
  }

  private void putLink() {
    this.current();
    if (this.running || this.nodeDirty || this.targetDirty) {
      throw new IllegalArgumentException("Сначала запишите параметры стадии или цель расчёта.");
    }
    final var link =
        LaboratoryRoutePlanning.link(
            this.expected,
            this.editingLink == null ? "route_link_" + UUID.randomUUID() : this.editingLink,
            chosen(this.source, "стадия-источник"),
            chosen(this.destination, "следующая стадия"),
            chosen(this.input, "исходник следующей стадии"),
            decimal(this.recovery.getText(), "Сохранение при передаче").movePointLeft(2),
            this.linkEnabled.isSelected());
    final var old = this.expected.experiment().route();
    final var values = new ArrayList<>(old.links());
    final int pos =
        java.util.stream.IntStream.range(0, values.size())
            .filter(i -> values.get(i).id().equals(link.id()))
            .findFirst()
            .orElse(-1);
    if (pos < 0) {
      values.add(link);
    } else {
      values.set(pos, link);
    }
    this.editor.route(
        this.expected,
        new LaboratorySynthesisRoute(
            old.nodes(), values, old.targetStageId(), old.targetQuantity()));
    this.reload(id(this.stage));
    this.loading = true;
    this.links.getSelectionModel().select(link);
    this.loading = false;
    this.loadLink(link);
    this.status.setText("Связь подтверждена по структурам и записана.");
  }

  private void removeLink() {
    this.requireClean();
    this.current();
    if (this.editingLink == null) {
      throw new IllegalArgumentException("Выберите связь в списке.");
    }
    final var old = this.expected.experiment().route();
    this.editor.route(
        this.expected,
        new LaboratorySynthesisRoute(
            old.nodes(),
            old.links().stream().filter(l -> !l.id().equals(this.editingLink)).toList(),
            old.targetStageId(),
            old.targetQuantity()));
    this.reload(id(this.stage));
  }

  private void render() {
    this.map.getChildren().clear();
    final var route = this.expected.experiment().route();
    this.summary.setText(
        "Стадий в маршруте: "
            + route.nodes().size()
            + " · активных связей: "
            + route.activeLinks().size());
    this.problems.setText(String.join("\n", LaboratoryRoutePlanning.diagnostics(this.expected)));
    if (route.nodes().isEmpty()) {
      final Label empty = heading("Выберите стадию, её продукт и ожидаемый выход");
      empty.setWrapText(true);
      empty.setMaxWidth(480);
      empty.relocate(32, 32);
      this.map.getChildren().add(empty);
      this.map.setPrefSize(640, 360);
      return;
    }
    final Map<String, Integer> levels = new LinkedHashMap<>();
    for (final String id : route.topologicalStageIds()) {
      levels.put(
          id,
          route.activeLinks().stream()
              .filter(l -> l.targetStageId().equals(id))
              .mapToInt(l -> levels.getOrDefault(l.sourceStageId(), 0) + 1)
              .max()
              .orElse(0));
    }
    final int inactiveColumn =
        levels.values().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
    route.nodes().stream()
        .filter(n -> !n.enabled())
        .forEach(n -> levels.put(n.stageId(), inactiveColumn));
    final Map<Integer, Integer> rows = new HashMap<>();
    final Map<String, double[]> locations = new HashMap<>();
    double maxX = 0, maxY = 0;
    for (final var n : route.nodes()) {
      final int col = levels.getOrDefault(n.stageId(), 0),
          row = rows.merge(col, 1, Integer::sum) - 1;
      final double x = 32 + col * 390, y = 28 + row * 300;
      locations.put(n.stageId(), new double[] {x, y});
      maxX = Math.max(maxX, x + 325);
      maxY = Math.max(maxY, y + 260);
    }
    for (final var link : route.links()) {
      final var a = locations.get(link.sourceStageId());
      final var b = locations.get(link.targetStageId());
      if (a == null || b == null) {
        continue;
      }
      final boolean active = route.activeLinks().contains(link);
      final double ax = a[0] + 310, ay = a[1] + 115, bx = b[0], by = b[1] + 115;
      final CubicCurve curve = new CubicCurve(ax, ay, ax + 40, ay, bx - 40, by, bx, by);
      curve.setFill(Color.TRANSPARENT);
      curve.setStroke(active ? Color.web("#557b9e") : Color.web("#a5acb6"));
      curve.setStrokeWidth(2);
      if (!active) {
        curve.getStrokeDashArray().setAll(6d, 5d);
      }
      final Polygon arrow = new Polygon(bx, by, bx - 10, by - 5, bx - 10, by + 5);
      arrow.setFill(curve.getStroke());
      this.map.getChildren().addAll(curve, arrow);
      final Label loss = note(percent(link.transferFraction()) + "%");
      loss.setId("route-transfer-" + link.id());
      loss.setStyle(
          "-fx-background-color: #ffffff; -fx-text-fill: #243c50; -fx-border-color: #cfdae4; -fx-background-radius: 4; -fx-border-radius: 4; -fx-padding: 3 6;");
      loss.relocate((ax + bx) / 2 - 20, (ay + by) / 2 - 25);
      Tooltip.install(
          curve,
          new Tooltip(
              componentName(link.targetComponentId())
                  + " · до следующей стадии доходит "
                  + percent(link.transferFraction())
                  + "%"));
      this.map.getChildren().add(loss);
    }
    for (final var n : route.nodes()) {
      final var xy = locations.get(n.stageId());
      final Label title = heading(stageName(n.stageId()));
      title.setMaxWidth(280);
      final Label product = note(componentName(n.productComponentId()));
      product.setMaxWidth(280);
      final Canvas picture = this.picture(n.productComponentId());
      final Label meta =
          note(
              "Ожидаемый выход: "
                  + yieldText(n.expectedYieldPercent())
                  + "%"
                  + (n.enabled() ? "" : " · выключена"));
      meta.setTooltip(
          new Tooltip("Ожидаемый выход: " + n.expectedYieldPercent().toPlainString() + "%"));
      final VBox card = new VBox(6, title, product, picture, meta);
      card.setPadding(new Insets(12));
      card.setPrefWidth(310);
      card.setMaxWidth(310);
      card.getStyleClass().add("laboratory-reaction-card");
      card.setOpacity(n.enabled() ? 1 : .6);
      card.relocate(xy[0], xy[1]);
      card.setOnMouseClicked(
          e -> {
            if (this.pending()) {
              this.status.setText("Запишите или сбросьте ввод перед сменой стадии.");
              return;
            }
            select(this.stage, n.stageId());
            editRoute();
          });
      card.setId("route-node-" + n.stageId());
      this.map.getChildren().add(card);
    }
    this.map.setPrefSize(Math.max(640, maxX + 32), Math.max(360, maxY + 32));
    if (!this.initialFit) {
      Platform.runLater(
          () -> {
            if (!disposed
                && !userView
                && graph.getViewportBounds().getWidth() > 80
                && graph.getViewportBounds().getHeight() > 80) {
              fitting = true;
              fit();
              fitting = false;
              initialFit = true;
            }
          });
    }
  }

  private Canvas picture(final String componentId) {
    final Canvas c = new Canvas(280, 120);
    final var drawing = this.expected.layouts().get(componentId);
    final Runnable paint =
        () -> {
          final var g = c.getGraphicsContext2D();
          LaboratoryPreviewTheme.clear(g);
          if (drawing == null) {
            return;
          }
          final var b = LaboratoryCanvas.previewBounds(drawing);
          final double scale =
              Math.min(
                  .6, Math.min(250 / Math.max(1, b.getWidth()), 94 / Math.max(1, b.getHeight())));
          g.save();
          g.translate(
              140 - scale * (b.getMinX() + b.getMaxX()) / 2,
              60 - scale * (b.getMinY() + b.getMaxY()) / 2);
          g.scale(scale, scale);
          LaboratoryPreviewTheme.paint(g, drawing);
          g.restore();
        };
    LaboratoryPreviewTheme.track(c, paint);
    paint.run();
    return c;
  }

  private void fit() {
    this.zoom.setValue(
        Math.min(
            1.2,
            Math.min(
                (this.graph.getViewportBounds().getWidth() - 24)
                    / Math.max(1, this.map.getPrefWidth()),
                (this.graph.getViewportBounds().getHeight() - 24)
                    / Math.max(1, this.map.getPrefHeight()))));
    this.graph.setHvalue(0);
    this.graph.setVvalue(0);
  }

  private void calculate() {
    this.current();
    if (this.running) {
      return;
    }
    if (this.nodeDirty || this.linkDirty) {
      throw new IllegalArgumentException("Запишите параметры стадии / связи перед расчётом.");
    }
    if (this.planApplied && !this.pending() && this.expected.equals(this.calculationSource)) {
      this.status.setText(
          "План уже применён. Измените цель или маршрут, чтобы рассчитать новый план.");
      return;
    }
    final String stage = chosen(this.target, "конечный продукт стадии");
    final LaboratoryQuantity q =
        new LaboratoryQuantity(LaboratoryDecimalField.value(this.amount), this.unit.getValue());
    final var old = this.expected.experiment().route();
    this.editor.route(
        this.expected, new LaboratorySynthesisRoute(old.nodes(), old.links(), stage, q));
    this.expected = this.editor.document();
    this.initialTarget = this.targetForm();
    this.refreshDirty();
    this.invalidate();
    final LaboratoryDrawing input = this.expected;
    final long request = ++this.generation;
    this.busy(true);
    this.calculate.setDisable(true);
    this.results.getChildren().clear();
    this.planSummary.setText("Считаю потребности от конечного продукта…");
    this.task =
        this.worker.submit(
            () -> {
              try {
                final var result = LaboratoryRoutePlanning.plan(input);
                Platform.runLater(
                    () -> {
                      if (this.disposed || request != this.generation) {
                        return;
                      }
                      this.busy(false);
                      this.calculate.setDisable(false);
                      if (!this.editor.document().equals(input)) {
                        this.conflict = true;
                        this.status.setText(
                            "Опыт изменился во время расчёта. Результат не применён.");
                        return;
                      }
                      this.calculated = result;
                      this.calculationSource = input;
                      this.renderPlan(result);
                    });
              } catch (final RuntimeException e) {
                Platform.runLater(
                    () -> {
                      if (this.disposed || request != this.generation) {
                        return;
                      }
                      this.busy(false);
                      this.calculate.setDisable(false);
                      this.planSummary.setText(message(e));
                    });
              }
            });
  }

  private void renderPlan(final LaboratoryRoutePlanning.Result result) {
    this.planSummary.setText(
        "Цель: "
            + LaboratoryQuantityText.format(result.requestedQuantity(), true)
            + " · "
            + stageName(result.targetStageId())
            + "\nПлан по ожидаемым выходам");
    this.results.getChildren().clear();
    final var yields =
        LaboratoryRoutePlanning.yieldSummary(this.expected.experiment(), result.targetStageId());
    final VBox yieldCard =
        new VBox(
            8,
            heading(
                yields.overallPercent() == null
                    ? "Ожидаемые выходы отдельных путей"
                    : "Сквозной ожидаемый выход: " + yieldText(yields.overallPercent()) + "%"));
    yieldCard.setPadding(new Insets(14));
    yieldCard.getStyleClass().add("laboratory-reaction-card");
    if (yields.overallPercent() != null) {
      yieldCard
          .getChildren()
          .add(
              note(
                  "Начальная база: "
                      + componentName(yields.basisComponentId())
                      + " · стадия «"
                      + stageName(yields.basisStageId())
                      + "»."));
    } else {
      for (final var path : yields.paths().stream().limit(16).toList()) {
        final var names = path.stageIds().stream().limit(8).map(this::stageName).toList();
        yieldCard
            .getChildren()
            .add(
                note(
                    String.join(" → ", names)
                        + (path.stageIds().size() > 8
                            ? " → … → " + stageName(path.stageIds().getLast())
                            : "")
                        + " · "
                        + yieldText(path.expectedPercent())
                        + "%\nБаза: "
                        + componentName(path.basisComponentId())
                        + " · «"
                        + stageName(path.basisStageId())
                        + "»."));
      }
      if (yields.paths().size() > 16) {
        yieldCard
            .getChildren()
            .add(note("Показаны первые 16 из " + yields.paths().size() + " путей."));
      }
    }
    yields.notes().forEach(n -> yieldCard.getChildren().add(note(n)));
    result.warnings().forEach(n -> yieldCard.getChildren().add(note(n)));
    final TitledPane yieldDetails = new TitledPane("Выходы и основания расчёта", yieldCard);
    yieldDetails.setAnimated(false);
    yieldDetails.setExpanded(false);
    this.results.getChildren().add(yieldDetails);
    for (final var stage : result.stages()) {
      final Label amount =
          note(
              "Получить: "
                  + LaboratoryQuantityText.format(stage.desiredProduct(), true)
                  + " · "
                  + componentName(stage.productComponentId())
                  + "\nОжидаемый выход: "
                  + yieldText(stage.expectedYieldPercent())
                  + "%");
      amount.setTooltip(
          new Tooltip(
              "Получить: "
                  + LaboratoryQuantityText.format(stage.desiredProduct(), false)
                  + "\nОжидаемый выход: "
                  + stage.expectedYieldPercent().toPlainString()
                  + "%"));
      final VBox card = new VBox(8, heading(stageName(stage.stageId())), amount);
      card.setPadding(new Insets(14));
      card.getStyleClass().add("laboratory-reaction-card");
      for (final var item : stage.planned().entrySet()) {
        card.getChildren()
            .add(
                note(
                    componentName(item.getKey())
                        + "   ·   "
                        + LaboratoryQuantityText.format(item.getValue(), true)));
      }
      for (final String warning : stage.warnings()) {
        card.getChildren().add(stageWarning(warning, stage.expectedYieldPercent()));
      }
      this.results.getChildren().add(card);
    }
    try {
      LaboratoryRoutePlanning.combinedPlan(result);
      this.applyPlan.setDisable(false);
      this.calculate.getStyleClass().remove("laboratory-primary-action");
    } catch (final RuntimeException e) {
      this.applyPlan.setDisable(true);
      this.results.getChildren().add(note(message(e)));
    }
  }

  private static Label stageWarning(final String warning, final BigDecimal percent) {
    final String suffix = " % задан пользователем относительно выбранного опорного исходника.";
    final String generated = "Ожидаемый выход " + percent.toPlainString() + suffix;
    final Label label =
        note(
            warning.equals(generated) ? "Ожидаемый выход " + yieldText(percent) + suffix : warning);
    if (warning.equals(generated)) {
      label.setTooltip(new Tooltip(warning));
    }
    return label;
  }

  private static String yieldText(final BigDecimal percent) {
    final var value = percent.stripTrailingZeros();
    final var shown =
        value
            .round(new java.math.MathContext(5, java.math.RoundingMode.HALF_UP))
            .stripTrailingZeros();
    final String text =
        shown.signum() != 0 && shown.abs().compareTo(new BigDecimal("0.0001")) < 0
            ? shown.toEngineeringString()
            : shown.toPlainString();
    return (shown.compareTo(value) == 0 ? "" : "≈ ") + text;
  }

  private void applyPlan() {
    this.requireClean();
    this.current();
    if (this.planApplied) {
      throw new IllegalArgumentException(
          "Этот план уже применён. Для нового плана измените цель или маршрут.");
    }
    if (this.calculated == null || !this.expected.equals(this.calculationSource)) {
      throw new IllegalArgumentException("Сначала рассчитайте текущий маршрут.");
    }
    final var result = this.calculated;
    this.editor.planBatch(this.expected, LaboratoryRoutePlanning.combinedPlan(result));
    this.expected = this.editor.document();
    this.calculationSource = this.expected;
    this.planApplied = true;
    this.applyPlan.setDisable(true);
    this.planSummary.setText("План применён · " + this.planSummary.getText());
    this.status.setText(
        "План применён к опыту. Сохраните опыт, чтобы записать файл. Фактические загрузки и выходы не изменены.");
    this.render();
    this.historyControls();
  }

  /** Called by the workspace after a document edit, including Undo, without losing unfinished form input. */
  void documentChanged() {
    this.historyControls();
    if (this.disposed || this.expected.equals(this.editor.document())) return;
    if (this.pending()) {
      this.conflict = true;
      this.invalidate();
      this.status.setText(
          "Опыт изменился. Незаписанный ввод сохранён, но требует сверки; прежний расчёт снят.");
    } else {
      if (this.running) {
        ++this.generation;
        if (this.task != null) this.task.cancel(true);
        this.busy(false);
        this.calculate.setDisable(false);
      }
      this.reload(id(this.stage));
      this.status.setText("Опыт изменился. Рассчитайте загрузки заново.");
    }
  }

  /** Only the completed file-save callback may call this method. */
  void saved() {
    if (this.disposed) return;
    this.documentChanged();
    if (this.editor.dirty()) {
      this.status.setText("Снимок опыта записан. Более поздние изменения ещё не сохранены.");
    } else if (this.pending()) {
      this.status.setText("Файл опыта сохранён. В форме маршрута остался незаписанный ввод.");
    } else {
      this.status.setText("Опыт с маршрутом сохранён.");
    }
  }

  void draft(final Properties p, final boolean restore) {
    if (!restore) {
      this.refreshDirty();
    }
    if (!restore && this.conflictDraft != null) {
      this.conflictDraft.forEach(p::put);
      return;
    }
    final Map<String, Control> fields =
        Map.of(
            "yield",
            this.yield,
            "recovery",
            this.recovery,
            "amount",
            this.amount,
            "unit",
            this.unit,
            "enabled",
            this.enabled,
            "linkEnabled",
            this.linkEnabled);
    final Map<String, ComboBox<Choice>> choices =
        Map.of(
            "stage",
            this.stage,
            "reference",
            this.reference,
            "product",
            this.product,
            "sourceStage",
            this.source,
            "destination",
            this.destination,
            "input",
            this.input,
            "target",
            this.target);
    if (!restore) {
      p.setProperty("route.source", new LaboratoryFileCodec().write(this.expected));
      p.setProperty("route.nodeDirty", Boolean.toString(this.nodeDirty));
      p.setProperty("route.linkDirty", Boolean.toString(this.linkDirty));
      p.setProperty("route.targetDirty", Boolean.toString(this.targetDirty));
      p.setProperty("route.conflict", Boolean.toString(this.conflict));
      p.setProperty("route.editingLink", Objects.toString(this.editingLink, ""));
      p.setProperty(
          "route.view",
          List.of("calculation", "map", "editing")
              .get(this.tabs.getSelectionModel().getSelectedIndex()));
      p.setProperty(
          "route.tab", Integer.toString(this.tabs.getSelectionModel().getSelectedIndex()));
      choices.forEach((k, c) -> p.setProperty("route.choice." + k, Objects.toString(id(c), "")));
      LaboratoryFormDraft.fields(p, "route", false, fields);
      return;
    }
    this.reload(p.getProperty("route.choice.stage"));
    this.loading = true;
    select(this.stage, p.getProperty("route.choice.stage"));
    this.loadNode(id(this.stage));
    final String restoredLink = p.getProperty("route.editingLink", "");
    final var originalLink =
        this.expected.experiment().route().links().stream()
            .filter(l -> l.id().equals(restoredLink))
            .findFirst()
            .orElse(null);
    this.loadLink(originalLink);
    this.loading = true;
    this.links.getSelectionModel().select(originalLink);
    select(this.destination, p.getProperty("route.choice.destination"));
    this.loadInputs(p.getProperty("route.choice.input"));
    choices.forEach((k, c) -> select(c, p.getProperty("route.choice." + k)));
    LaboratoryFormDraft.fields(p, "route", true, fields);
    this.editingLink = restoredLink.isEmpty() ? null : restoredLink;
    this.conflict =
        Boolean.parseBoolean(p.getProperty("route.conflict"))
            || !restoredLink.isEmpty() && originalLink == null
            || !new LaboratoryFileCodec().write(this.expected).equals(p.getProperty("route.source"))
            || choices.entrySet().stream()
                .anyMatch(
                    e ->
                        !p.getProperty("route.choice." + e.getKey(), "").isEmpty()
                            && id(e.getValue()) == null);
    if (this.conflict) {
      this.conflictDraft = new Properties();
      p.stringPropertyNames().stream()
          .filter(k -> k.startsWith("route."))
          .forEach(k -> this.conflictDraft.setProperty(k, p.getProperty(k)));
      this.conflictDraft.setProperty("route.conflict", "true");
      this.status.setText(
          "Ввод восстановлен для другой версии опыта; сохранённый черновик требует сверки.");
    }
    final String view = p.getProperty("route.view");
    final int oldTab = Integer.parseInt(p.getProperty("route.tab", "0"));
    this.tabs
        .getSelectionModel()
        .select(
            view == null
                ? (oldTab == 0 ? 1 : oldTab == 1 ? 0 : 2)
                : Math.max(0, List.of("calculation", "map", "editing").indexOf(view)));
    this.loading = false;
    this.refreshDirty();
  }
}

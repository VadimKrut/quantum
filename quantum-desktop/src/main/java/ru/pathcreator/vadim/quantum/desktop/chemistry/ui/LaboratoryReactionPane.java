/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEditor;

/** The diagram is a projection of stage references, never an independent copy of the molecular graph. */
final class LaboratoryReactionPane extends VBox {
  // A rendered card retains its measured dimensions when another document is opened.
  private record StageSize(double reactantWidth, double productWidth, double breakpoint) {
    double width(final ReactionComponentRole role) {
      return role == ReactionComponentRole.REACTANT ? this.reactantWidth : this.productWidth;
    }
  }

  private final LaboratoryEditor editor;
  private final LaboratoryFiles files;
  private final Consumer<Runnable> action;
  private final Consumer<String> select;
  private final Consumer<String> quantities;
  private final Consumer<String> componentQuantities;
  private final Consumer<LaboratoryReactionStage> edit;
  private Consumer<String> classification;

  void classification(final Consumer<String> action) {
    this.classification = action;
  }

  private Consumer<String> desk;

  void desk(final Consumer<String> action) {
    this.desk = action;
  }

  private Consumer<String> route;

  void route(final Consumer<String> action) {
    this.route = action;
  }

  private Consumer<String> procedure;

  void procedure(final Consumer<String> action) {
    this.procedure = action;
  }

  private Consumer<String> preparation;

  void preparation(final Consumer<String> action) {
    this.preparation = action;
  }

  private Runnable importReaction;

  void importReaction(final Runnable action) {
    this.importReaction = action;
  }

  private Consumer<LaboratoryReactionStage> exchange;

  void exchange(final Consumer<LaboratoryReactionStage> action) {
    this.exchange = action;
  }

  private Consumer<LaboratoryReactionStage> prediction;

  void prediction(final Consumer<LaboratoryReactionStage> action) {
    this.prediction = action;
  }

  private final VBox stages = new VBox(16);
  private final ScrollPane scroll = new ScrollPane(this.stages);
  private final java.util.Map<String, Node> stageCards = new java.util.HashMap<>();
  private final List<Runnable> layoutCleanup = new ArrayList<>();

  LaboratoryReactionPane(
      final LaboratoryEditor editor,
      final LaboratoryFiles files,
      final Consumer<Runnable> action,
      final Consumer<String> select,
      final Consumer<String> quantities,
      final Consumer<String> componentQuantities,
      final Consumer<LaboratoryReactionStage> edit) {
    super(8);
    this.editor = editor;
    this.files = files;
    this.action = action;
    this.select = select;
    this.quantities = quantities;
    this.edit = edit;
    this.componentQuantities = componentQuantities;
    this.setId("chemistry-reaction-pane");
    final Button add = button("Добавить стадию", () -> this.edit.accept(null));
    add.setId("chemistry-reaction-add");
    final Label hint = new Label("Схема реакции");
    hint.getStyleClass().add("section-title");
    hint.setTooltip(
        new Tooltip(
            "Нажмите на структуру, чтобы открыть её в редакторе. Количество под структурой можно изменить отдельно."));
    this.scroll.setFitToHeight(false);
    this.scroll.setFitToWidth(true);
    this.scroll.setPannable(true);
    this.stages.setAlignment(Pos.TOP_CENTER);
    this.stages.setPadding(new Insets(14));
    VBox.setVgrow(this.scroll, Priority.ALWAYS);
    final MenuItem importButton =
        menu(
            "Импорт RXN / SMILES…",
            () -> {
              if (this.importReaction != null) {
                this.importReaction.run();
              }
            });
    importButton.setId("chemistry-reaction-import");
    final MenuButton extra = new MenuButton("Ещё", null, importButton);
    final FlowPane toolbar = new FlowPane(12, 6, hint, add, extra);
    toolbar.setPadding(new Insets(10, 16, 0, 16));
    this.getChildren().addAll(toolbar, this.scroll);
  }

  void refresh() {
    this.layoutCleanup.forEach(Runnable::run);
    this.layoutCleanup.clear();
    this.stages.getChildren().clear();
    this.stageCards.clear();
    final List<LaboratoryReactionStage> stages = this.editor.document().experiment().stages();
    if (stages.isEmpty()) {
      this.stages
          .getChildren()
          .add(
              new Label(
                  "Добавьте стадию и выберите её участников. Неизвестные продукты можно оставить пустыми."));
    }
    for (int index = 0; index < stages.size(); index++) {
      final LaboratoryReactionStage stage = stages.get(index);
      final StageSize size = this.stageSize(stage);
      final int position = index;
      final Label title = new Label((index + 1) + ". " + stage.title());
      title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold;");
      title.setWrapText(true);
      title.setMinWidth(120);
      title.setMinHeight(Region.USE_PREF_SIZE);
      title
          .maxWidthProperty()
          .bind(
              javafx.beans.binding.Bindings.createDoubleBinding(
                  () -> Math.max(180, this.scroll.getViewportBounds().getWidth() - 70),
                  this.scroll.viewportBoundsProperty()));
      final MenuItem up = new MenuItem("Переместить выше");
      up.setOnAction(event -> this.move(position, -1));
      up.setDisable(index == 0);
      final MenuItem down = new MenuItem("Переместить ниже");
      down.setOnAction(event -> this.move(position, 1));
      down.setDisable(index == stages.size() - 1);
      final MenuItem remove = new MenuItem("Удалить стадию");
      remove.setOnAction(
          event ->
              this.action.accept(
                  () ->
                      this.editor.stages(
                          this.editor.document().experiment().stages().stream()
                              .filter(item -> !item.id().equals(stage.id()))
                              .toList())));
      final MenuItem export = new MenuItem("Экспорт RXN / SMILES…");
      export.setOnAction(
          event -> {
            if (this.exchange != null) {
              this.exchange.accept(stage);
            }
          });
      final MenuItem route = new MenuItem("Маршрут синтеза…");
      route.setOnAction(
          e -> {
            if (this.route != null) {
              this.route.accept(stage.id());
            }
          });
      final MenuItem loading =
          menu("Таблица количеств и выход…", () -> this.quantities.accept(stage.id()));
      loading.setId("chemistry-stage-loading-" + stage.id());
      final MenuItem edit = menu("Изменить участников и условия…", () -> this.edit.accept(stage));
      edit.setId("chemistry-stage-edit-" + stage.id());
      final MenuItem prepare =
          menu(
              "Подготовить реагенты…",
              () -> {
                if (this.preparation != null) {
                  this.preparation.accept(stage.id());
                }
              });
      prepare.setId("chemistry-stage-prepare-" + stage.id());
      final MenuItem procedure =
          menu(
              "Методика и проведение…",
              () -> {
                if (this.procedure != null) {
                  this.procedure.accept(stage.id());
                }
              });
      procedure.setId("chemistry-stage-procedure-" + stage.id());
      final Button work =
          button(
              "Загрузка и проведение",
              () -> {
                if (this.desk != null) {
                  this.desk.accept(stage.id());
                }
              });
      work.setId("chemistry-stage-desk-" + stage.id());
      work.getStyleClass().add("laboratory-save");
      final MenuItem classify =
          menu(
              "Название и механизмы…",
              () -> {
                if (this.classification != null) {
                  this.classification.accept(stage.id());
                }
              });
      classify.setId("chemistry-stage-classification-" + stage.id());
      final MenuButton more =
          new MenuButton(
              "Действия",
              null,
              loading,
              prepare,
              procedure,
              new SeparatorMenuItem(),
              edit,
              classify,
              route,
              new SeparatorMenuItem(),
              export,
              up,
              down,
              new SeparatorMenuItem(),
              remove);
      more.setId("chemistry-stage-more-" + stage.id());
      final FlowPane commands = new FlowPane(8, 6, work, more);
      commands.setAlignment(Pos.CENTER_LEFT);
      final VBox bar = new VBox(6, title, commands);
      final List<String> classificationLines = new ArrayList<>();
      final String reactionNames =
          stage.classification().reactionNames().stream()
              .map(LaboratoryReactionClassification.Term::text)
              .filter(name -> !name.equalsIgnoreCase(stage.title()))
              .collect(java.util.stream.Collectors.joining(" · "));
      if (!reactionNames.isBlank()) {
        classificationLines.add(reactionNames);
      }
      if (!stage.classification().mechanisms().isEmpty()) {
        classificationLines.add(
            "Механизм: "
                + stage.classification().mechanisms().stream()
                    .map(LaboratoryReactionClassification.Term::text)
                    .collect(java.util.stream.Collectors.joining(" · ")));
      }
      final String classificationText = String.join("\n", classificationLines);
      if (!classificationText.isBlank()) {
        final Label classified = new Label(classificationText);
        classified.setWrapText(true);
        classified.setMinHeight(Region.USE_PREF_SIZE);
        classified.setId("chemistry-stage-classification-label-" + stage.id());
        bar.getChildren().add(classified);
      }
      final VBox advanced = new VBox(10);
      final VBox warnings = new VBox(4);
      if (!stage.atomMapping().empty()) {
        final var problems =
            stage.atomMapping().problems(this.editor.document().experiment(), stage);
        final Label map =
            new Label(
                problems.isEmpty()
                    ? "Атомная карта из файла · " + stage.atomMapping().labels().size() + " меток"
                    : String.join(" ", problems));
        map.setWrapText(true);
        map.setMinHeight(Region.USE_PREF_SIZE);
        (problems.isEmpty() ? advanced : warnings).getChildren().add(map);
      }
      warnings.setVisible(!warnings.getChildren().isEmpty());
      warnings.setManaged(warnings.isVisible());
      final Node reactants = this.side(stage, ReactionComponentRole.REACTANT, size),
          products = this.side(stage, ReactionComponentRole.PRODUCT, size),
          arrow = this.arrow(stage, size);
      final GridPane equation = new GridPane();
      equation.setHgap(12);
      equation.setVgap(10);
      equation.setAlignment(Pos.TOP_CENTER);
      equation.setId("chemistry-stage-equation-" + stage.id());
      final Runnable arrange =
          () -> {
            final boolean wide = this.wide(size);
            if (Boolean.valueOf(wide).equals(equation.getUserData())) {
              return;
            }
            equation.getChildren().clear();
            equation.setUserData(wide);
            if (wide) {
              equation.addRow(0, reactants, arrow, products);
            } else {
              equation.addColumn(0, reactants, arrow, products);
            }
          };
      final javafx.beans.value.ChangeListener<javafx.geometry.Bounds> resize =
          (o, a, b) -> arrange.run();
      this.scroll.viewportBoundsProperty().addListener(resize);
      this.layoutCleanup.add(() -> this.scroll.viewportBoundsProperty().removeListener(resize));
      arrange.run();
      final FlowPane helpers = new FlowPane(12, 6);
      for (final var material : stage.conditions().components()) {
        final Label label =
            new Label(
                LaboratoryInspector.roleText(material.role())
                    + ": "
                    + material.name()
                    + " · без структуры");
        label.setWrapText(true);
        label.setMaxWidth(300);
        if (material.note() != null) {
          label.setTooltip(new Tooltip(material.note()));
        }
        helpers.getChildren().add(label);
      }
      for (final var participant : stage.participants()) {
        if (participant.role() == ReactionComponentRole.REACTANT
            || participant.role() == ReactionComponentRole.PRODUCT) {
          continue;
        }
        final var component = this.component(participant.componentId());
        final Label helper =
            new Label(
                LaboratoryInspector.roleText(participant.role())
                    + ": "
                    + component.molecule().displayName()
                    + "\n"
                    + quantitySummary(component));
        helper.setTooltip(new Tooltip(quantitySummary(component, false)));
        helper.setWrapText(true);
        helper.setMaxWidth(300);
        helper.setMinHeight(Region.USE_PREF_SIZE);
        helpers.getChildren().add(helper);
      }
      final Label balance = new Label();
      balance.setWrapText(true);
      balance.setMaxWidth(900);
      balance
          .maxWidthProperty()
          .bind(
              javafx.beans.binding.Bindings.createDoubleBinding(
                  () -> Math.max(260, this.scroll.getViewportBounds().getWidth() - 65),
                  this.scroll.viewportBoundsProperty()));
      final Button check =
          button(
              "Проверить атомы и заряд",
              () -> {
                try {
                  final var result =
                      stage.toReaction(this.editor.document().experiment()).balance();
                  balance.setText(
                      "Явные атомы: "
                          + (result.atomBalanced() ? "баланс соблюдён" : "баланс НЕ соблюдён")
                          + "; заряд: "
                          + (result.chargeBalanced() ? "баланс соблюдён" : "баланс НЕ соблюдён")
                          + ". Проверка не предсказывает протекание реакции; неявные атомы не добавляются.");
                } catch (final RuntimeException exception) {
                  balance.setText(exception.getMessage());
                }
              });
      final Button predict = button("Прогноз продуктов", () -> this.predict(stage));
      advanced.getChildren().addAll(new FlowPane(8, 6, check, predict), balance);
      final TitledPane extra = new TitledPane("Проверка и прогноз", advanced);
      extra.setExpanded(false);
      extra.setAnimated(false);
      extra.setId("chemistry-stage-checks-" + stage.id());
      extra.setVisible(false);
      extra.setManaged(false);
      final MenuItem checks =
          menu(
              "Проверка атомов и прогноз…",
              () -> {
                extra.setVisible(true);
                extra.setManaged(true);
                extra.setExpanded(true);
              });
      checks.setId("chemistry-stage-show-checks-" + stage.id());
      more.getItems().add(3, checks);
      extra
          .expandedProperty()
          .addListener(
              (o, before, expanded) -> {
                if (!expanded) {
                  extra.setVisible(false);
                  extra.setManaged(false);
                }
              });
      helpers.setVisible(!helpers.getChildren().isEmpty());
      helpers.setManaged(!helpers.getChildren().isEmpty());
      final Label conditionsNote = new Label(stage.conditions().note());
      conditionsNote.setWrapText(true);
      conditionsNote.setMinHeight(Region.USE_PREF_SIZE);
      conditionsNote.setVisible(
          conditionsNote.getText() != null && !conditionsNote.getText().isBlank());
      conditionsNote.setManaged(conditionsNote.isVisible());
      final VBox card = new VBox(12, bar, equation, helpers, conditionsNote, warnings, extra);
      card.setPadding(new Insets(16));
      card.setMaxWidth(1360);
      card.getStyleClass().add("laboratory-reaction-card");
      card.setId("chemistry-stage-" + stage.id());
      this.stageCards.put(stage.id(), card);
      this.stages.getChildren().add(card);
    }
  }

  void reveal(final String stageId) {
    final Node card = this.stageCards.get(stageId);
    if (card == null) {
      return;
    }
    this.applyCss();
    this.layout();
    this.scroll.layout();
    final double height =
        Math.max(
            1,
            this.stages.getBoundsInLocal().getHeight()
                - this.scroll.getViewportBounds().getHeight());
    this.scroll.setVvalue(
        Math.max(0, Math.min(1, (card.getBoundsInParent().getMinY() - 14) / height)));
    this.scroll.setHvalue(0);
  }

  private LaboratoryComponent component(final String id) {
    return this.editor.document().experiment().components().stream()
        .filter(item -> item.id().equals(id))
        .findFirst()
        .orElseThrow();
  }

  private Node side(
      final LaboratoryReactionStage stage, final ReactionComponentRole role, final StageSize size) {
    final FlowPane box = new FlowPane(8, 20);
    box.setAlignment(Pos.TOP_CENTER);
    final long count = stage.participants().stream().filter(p -> p.role() == role).count();
    box.prefWrapLengthProperty()
        .bind(
            javafx.beans.binding.Bindings.createDoubleBinding(
                () -> {
                  final double available =
                      Math.max(
                          260, Math.min(1298, this.scroll.getViewportBounds().getWidth() - 66));
                  if (!this.wide(size)) {
                    return available;
                  }
                  final long sources =
                      Math.max(
                          1,
                          Math.min(
                              2,
                              stage.participants().stream()
                                  .filter(p -> p.role() == ReactionComponentRole.REACTANT)
                                  .count()));
                  final long products =
                      Math.max(
                          1,
                          Math.min(
                              2,
                              stage.participants().stream()
                                  .filter(p -> p.role() == ReactionComponentRole.PRODUCT)
                                  .count()));
                  return (available - 124)
                      * (role == ReactionComponentRole.REACTANT ? sources : products)
                      / (sources + products);
                },
                this.scroll.viewportBoundsProperty()));
    box.prefWidthProperty().bind(box.prefWrapLengthProperty());
    box.minWidthProperty().bind(box.prefWrapLengthProperty());
    box.maxWidthProperty().bind(box.prefWrapLengthProperty());
    final var tileWidth =
        javafx.beans.binding.Bindings.createDoubleBinding(
            () -> {
              final int columns =
                  Math.max(
                      1,
                      Math.min(
                          (int) Math.max(1, count),
                          (int)
                              Math.floor((box.getPrefWrapLength() + 8) / (size.width(role) + 30))));
              return Math.min(
                  Math.max(420, size.width(role)),
                  Math.floor((box.getPrefWrapLength() - 8 * (columns - 1)) / columns) - 22);
            },
            box.prefWrapLengthProperty());
    for (final var participant : stage.participants()) {
      if (participant.role() != role) {
        continue;
      }
      final Label plus = new Label(box.getChildren().isEmpty() ? "" : "+");
      plus.setMinWidth(18);
      plus.setStyle("-fx-font-size: 20; -fx-text-fill: #718096;");
      final HBox term = new HBox(4, plus, this.molecule(participant, tileWidth));
      term.setAlignment(Pos.CENTER);
      term.prefWidthProperty().bind(tileWidth.add(22));
      term.minWidthProperty().bind(term.prefWidthProperty());
      term.maxWidthProperty().bind(term.prefWidthProperty());
      box.getChildren().add(term);
    }
    if (box.getChildren().isEmpty()) {
      final Label missing =
          new Label(
              role == ReactionComponentRole.PRODUCT ? "Продукт не задан" : "Исходник не задан");
      missing.setStyle("-fx-font-size: 15; -fx-text-fill: #718096;");
      final Label note = new Label("Можно добавить позже");
      note.setStyle("-fx-text-fill: #8792a2;");
      final VBox empty =
          new VBox(8, missing, note, button("Выбрать участников", () -> this.edit.accept(stage)));
      empty.setAlignment(Pos.CENTER);
      empty.setMinSize(200, 180);
      box.getChildren().add(empty);
    }
    return box;
  }

  private Node molecule(
      final LaboratoryStageParticipant participant,
      final javafx.beans.binding.DoubleBinding width) {
    final LaboratoryComponent component = this.component(participant.componentId());
    final var drawing = this.editor.document().layouts().get(component.id());
    final Canvas canvas = new Canvas();
    canvas.widthProperty().bind(width);
    final var bounds = LaboratoryCanvas.previewBounds(drawing);
    canvas
        .heightProperty()
        .bind(
            javafx.beans.binding.Bindings.createDoubleBinding(
                () ->
                    Math.min(
                        640,
                        Math.max(
                            135,
                            bounds.getHeight()
                                    * Math.min(
                                        .75,
                                        Math.max(1, width.get() - 28)
                                            / Math.max(1, bounds.getWidth()))
                                + 28)),
                width));
    final Runnable paint =
        () -> {
          final double scale =
              Math.min(
                  .75,
                  Math.min(
                      (canvas.getWidth() - 28) / Math.max(1, bounds.getWidth()),
                      (canvas.getHeight() - 28) / Math.max(1, bounds.getHeight())));
          final var graphics = canvas.getGraphicsContext2D();
          LaboratoryPreviewTheme.clear(graphics);
          graphics.save();
          graphics.translate(
              canvas.getWidth() / 2 - scale * (bounds.getMinX() + bounds.getMaxX()) / 2,
              canvas.getHeight() / 2 - scale * (bounds.getMinY() + bounds.getMaxY()) / 2);
          graphics.scale(scale, scale);
          LaboratoryPreviewTheme.paint(graphics, drawing);
          graphics.restore();
        };
    LaboratoryPreviewTheme.track(canvas, paint);
    canvas.widthProperty().addListener((o, a, b) -> paint.run());
    canvas.heightProperty().addListener((o, a, b) -> paint.run());
    paint.run();
    final Label name = new Label(component.molecule().displayName());
    name.setWrapText(true);
    name.setMinWidth(0);
    name.setMinHeight(Region.USE_PREF_SIZE);
    name.setMaxWidth(194);
    name.setAlignment(Pos.CENTER);
    name.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
    name.setStyle("-fx-font-weight: bold;");
    final String coefficient =
        participant.coefficient() == null
            ? "Коэффициент не задан"
            : "Коэффициент: " + participant.coefficient().value();
    final String tooltip =
        component.molecule().displayName() + "\nОткрыть структуру · " + coefficient;
    for (final Node node : List.of(canvas, name)) {
      node.setOnMouseClicked(event -> this.select.accept(component.id()));
      node.setCursor(javafx.scene.Cursor.HAND);
      Tooltip.install(node, new Tooltip(tooltip));
    }
    final Button amount =
        button(quantitySummary(component), () -> this.componentQuantities.accept(component.id()));
    amount.setId("chemistry-reaction-quantity-" + component.id());
    amount.setWrapText(true);
    amount.setMinHeight(Region.USE_PREF_SIZE);
    amount.setMaxWidth(194);
    amount.getStyleClass().add("laboratory-reaction-quantity");
    amount.setTooltip(
        new Tooltip(
            quantitySummary(component, false)
                + "\nИзменить количество и свойства. Складской расход здесь не проводится."));
    final VBox view = new VBox(6, canvas, name, amount);
    view.setAlignment(Pos.CENTER);
    view.setMinWidth(194);
    view.setMaxWidth(194);
    view.setId("chemistry-stage-structure-" + participant.componentId());
    view.setFocusTraversable(true);
    view.getStyleClass().add("laboratory-stage-structure");
    view.setAccessibleText(
        component.molecule().displayName() + ". Enter — структура, Ctrl+Enter — количество.");
    view.setOnKeyPressed(
        event -> {
          if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
            if (event.isControlDown()) {
              this.componentQuantities.accept(component.id());
            } else {
              this.select.accept(component.id());
            }
            event.consume();
          }
        });
    view.prefWidthProperty().bind(width);
    view.minWidthProperty().bind(width);
    view.maxWidthProperty().bind(width);
    name.maxWidthProperty().bind(width);
    amount.maxWidthProperty().bind(width);
    if (participant.coefficient() == null) {
      final Label unknown = new Label("Коэф. не задан");
      unknown.setStyle("-fx-text-fill: #8792a2; -fx-font-size: 11;");
      view.getChildren().add(unknown);
    } else if (participant.coefficient().value() != 1) {
      final Label factor = new Label("Коэффициент: " + participant.coefficient().value());
      factor.setStyle("-fx-text-fill: #718096; -fx-font-size: 11;");
      view.getChildren().add(factor);
    }
    return view;
  }

  private static String quantitySummary(final LaboratoryComponent component) {
    return quantitySummary(component, true);
  }

  private static String quantitySummary(
      final LaboratoryComponent component, final boolean compact) {
    if (component.planned() == null && component.actual() == null) {
      return "Задать количество";
    }
    final String plan =
        component.planned() == null
            ? ""
            : "План: " + LaboratoryQuantityText.format(component.planned(), compact);
    final String entered =
        component.actual() == null
            ? ""
            : component.evidence().quantityLabel()
                + ": "
                + LaboratoryQuantityText.format(component.actual(), compact);
    return plan + (plan.isEmpty() || entered.isEmpty() ? "" : "\n") + entered;
  }

  private Node arrow(final LaboratoryReactionStage stage, final StageSize size) {
    final var conditions = stage.conditions();
    final Label top =
        new Label(
            (conditions.temperature() == null
                    ? ""
                    : conditions.temperature().value()
                        + " "
                        + (conditions.temperature().unit()
                                == ru.pathcreator.vadim.quantum.chemistry.domain.unit
                                    .TemperatureUnit.CELSIUS
                            ? "°C"
                            : "K"))
                + (conditions.duration() == null
                    ? ""
                    : "\n"
                        + conditions.duration().value()
                        + " "
                        + switch (conditions.duration().unit()) {
                          case SECOND -> "с";
                          case MINUTE -> "мин";
                          case HOUR -> "ч";
                          case DAY -> "сут";
                        })
                + (conditions.pressure() == null
                    ? ""
                    : "\n"
                        + conditions.pressure().value()
                        + " "
                        + switch (conditions.pressure().unit()) {
                          case PASCAL -> "Па";
                          case ATMOSPHERE -> "атм";
                          case BAR -> "бар";
                        }));
    final Label arrow = new Label();
    arrow.setFont(Font.font(38));
    arrow
        .textProperty()
        .bind(
            javafx.beans.binding.Bindings.createStringBinding(
                () ->
                    stage.arrow() == LaboratoryReactionStage.Arrow.FORWARD
                        ? (this.wide(size) ? "⟶" : "↓")
                        : (this.wide(size) ? "⇌" : "⇅"),
                this.scroll.viewportBoundsProperty()));
    top.setVisible(!top.getText().isBlank());
    top.setManaged(top.isVisible());
    final VBox view = new VBox(4, top, arrow);
    view.setMinWidth(100);
    view.setAlignment(Pos.CENTER);
    view.prefWidthProperty()
        .bind(
            javafx.beans.binding.Bindings.createDoubleBinding(
                () ->
                    this.wide(size)
                        ? 100
                        : Math.max(260, this.scroll.getViewportBounds().getWidth() - 66),
                this.scroll.viewportBoundsProperty()));
    view.maxWidthProperty().bind(view.prefWidthProperty());
    view.prefHeightProperty()
        .bind(
            javafx.beans.binding.Bindings.createDoubleBinding(
                () -> this.wide(size) ? 200.0 : 100.0, this.scroll.viewportBoundsProperty()));
    return view;
  }

  private double readableWidth(
      final LaboratoryReactionStage stage, final ReactionComponentRole role) {
    return stage.participants().stream()
        .filter(participant -> participant.role() == role)
        .mapToDouble(
            participant ->
                LaboratoryCanvas.previewBounds(
                                this.editor.document().layouts().get(participant.componentId()))
                            .getWidth()
                        * .75
                    + 28)
        .map(width -> Math.max(194, Math.min(640, width)))
        .max()
        .orElse(200);
  }

  private boolean wide(final StageSize size) {
    return this.scroll.getViewportBounds().getWidth() >= size.breakpoint();
  }

  private StageSize stageSize(final LaboratoryReactionStage stage) {
    final long sources =
        Math.max(
            1,
            Math.min(
                2,
                stage.participants().stream()
                    .filter(p -> p.role() == ReactionComponentRole.REACTANT)
                    .count()));
    final long products =
        Math.max(
            1,
            Math.min(
                2,
                stage.participants().stream()
                    .filter(p -> p.role() == ReactionComponentRole.PRODUCT)
                    .count()));
    final double reactants = this.readableWidth(stage, ReactionComponentRole.REACTANT);
    final double productsWidth = this.readableWidth(stage, ReactionComponentRole.PRODUCT);
    final double sideWidth =
        (sources + products)
            * Math.max((reactants + 22) / sources, (productsWidth + 22) / products);
    return new StageSize(reactants, productsWidth, Math.max(740, sideWidth + 190));
  }

  private void predict(final LaboratoryReactionStage stage) {
    if (this.prediction == null) {
      throw new IllegalStateException("Прогноз не подключён к рабочему месту.");
    }
    this.prediction.accept(stage);
  }

  private void move(final int index, final int direction) {
    this.action.accept(
        () -> {
          final ArrayList<LaboratoryReactionStage> stages =
              new ArrayList<>(this.editor.document().experiment().stages());
          java.util.Collections.swap(stages, index, index + direction);
          this.editor.stages(stages);
        });
  }

  private static MenuItem menu(final String title, final Runnable action) {
    final MenuItem item = new MenuItem(title);
    item.setOnAction(event -> action.run());
    return item;
  }

  private static Button button(final String title, final Runnable action) {
    final Button button = new Button(title);
    button.setOnAction(event -> action.run());
    return button;
  }
}

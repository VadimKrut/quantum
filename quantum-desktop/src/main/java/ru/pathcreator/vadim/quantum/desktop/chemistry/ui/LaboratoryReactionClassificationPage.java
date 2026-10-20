/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.text.Normalizer;
import java.util.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionClassification.Term;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEditor;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryFileCodec;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryReactionCatalog;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryReactionCatalog.*;

/** Explicit human labels for a stage; the catalogue never infers chemistry from its drawing. */
final class LaboratoryReactionClassificationPage extends LaboratoryComputationPage {
  private static final String ALL = "Все категории", PREFIX = "reactionClassification.";
  private final LaboratoryEditor editor;
  private LaboratoryDrawing expected;
  private LaboratoryReactionStage selected;
  private final ComboBox<LaboratoryReactionStage> stage = new ComboBox<>();
  private final ToggleGroup types = new ToggleGroup();
  private final ToggleButton reactions = new ToggleButton("Названия реакций"),
      mechanisms = new ToggleButton("Механизмы");
  private final ComboBox<String> category = new ComboBox<>();
  private final TextField query = new TextField(), customText = new TextField();
  private final EnumMap<Kind, String> customInputs = new EnumMap<>(Kind.class);
  private final TitledPane customPane = new TitledPane(), additional = new TitledPane();
  private final Label inputError = note("");
  private final EnumMap<Kind, String> queries = new EnumMap<>(Kind.class);
  private final ListView<Entry> matches = new ListView<>();
  private final List<Term> names = new ArrayList<>(), paths = new ArrayList<>();
  private final FlowPane nameChips = new FlowPane(8, 8), mechanismChips = new FlowPane(8, 8);
  private final TextArea note = new TextArea();
  private final CheckBox rename =
      new CheckBox("Назвать стадию по первому выбранному названию реакции");
  private final Label titlePreview = note(""),
      count = note(""),
      description = note(""),
      warning = note("");
  private final TextField source = new TextField();
  private final VBox leave = new VBox(8), sheet = new VBox(14);
  private final Button add, custom, apply, reset;
  private boolean changing, conflict;

  LaboratoryReactionClassificationPage(
      final LaboratoryEditor editor, final String stageId, final Runnable close) {
    super("Название реакции и механизмы", close);
    this.editor = editor;
    this.setId("chemistry-reaction-classification-page");
    this.stage.setId("reaction-classification-stage");
    this.stage.setMaxWidth(Double.MAX_VALUE);
    this.stage.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(LaboratoryReactionStage s) {
            return stageText(s);
          }

          public LaboratoryReactionStage fromString(String s) {
            return null;
          }
        });
    this.stage.setCellFactory(list -> this.stageCell());
    this.stage.setButtonCell(this.stageCell());
    this.reactions.setToggleGroup(this.types);
    this.reactions.setUserData(Kind.REACTION);
    this.mechanisms.setToggleGroup(this.types);
    this.mechanisms.setUserData(Kind.MECHANISM);
    this.reactions.setSelected(true);
    this.reactions.setId("reaction-classification-reactions");
    this.mechanisms.setId("reaction-classification-mechanisms");
    this.query.setId("reaction-classification-query");
    this.query.setPromptText(
        "Русское или английское название, сокращение, например Suzuki или SN2");
    this.query.setMaxWidth(Double.MAX_VALUE);
    this.category.setId("reaction-classification-category");
    this.category.setMaxWidth(380);
    this.category.setPrefWidth(280);
    this.matches.setId("reaction-classification-matches");
    this.matches.setMinHeight(180);
    this.matches.setPrefHeight(245);
    this.matches.setMaxHeight(280);
    this.matches.setPlaceholder(
        note("Совпадений нет. Откройте «Свой текст», чтобы добавить свою формулировку."));
    this.matches.setCellFactory(
        list ->
            new ListCell<>() {
              private final Label title = note(""), subtitle = note("");
              private final VBox content = new VBox(3, this.title, this.subtitle);

              {
                this.title.setStyle("-fx-font-weight: bold;");
                this.subtitle.getStyleClass().add("muted-label");
                this.content.setPadding(new Insets(5, 3, 5, 3));
                this.content.prefWidthProperty().bind(list.widthProperty().subtract(44));
                this.content.setMaxWidth(Region.USE_PREF_SIZE);
              }

              @Override
              protected void updateItem(final Entry entry, final boolean empty) {
                super.updateItem(entry, empty);
                this.setText(null);
                this.setGraphic(empty || entry == null ? null : this.content);
                if (!empty && entry != null) {
                  this.title.setText(entry.nameRu());
                  this.subtitle.setText(entry.nameEn() + " · " + entry.category());
                }
              }
            });
    this.add = button("Добавить выбранное", "add", this::addSelected);
    this.custom = button("Добавить своё название", "custom", this::addCustom);
    this.apply = button("Применить к стадии", "apply", this::apply);
    this.apply.getStyleClass().add("primary-button");
    this.reset = button("Сбросить ввод", "reset", this::reset);
    for (final var chips : List.of(this.nameChips, this.mechanismChips)) {
      chips.setMinHeight(Region.USE_PREF_SIZE);
      chips.prefWrapLengthProperty().bind(chips.widthProperty());
    }
    this.nameChips.setId("reaction-classification-names");
    this.mechanismChips.setId("reaction-classification-paths");
    this.rename.setId("reaction-classification-rename");
    this.rename.setWrapText(true);
    this.note.setId("reaction-classification-note");
    this.note.setWrapText(true);
    this.note.setPrefRowCount(2);
    this.note.setPromptText(
        "Условия применимости механизма, альтернативы или пояснение вашего выбора");
    this.note.setMaxHeight(120);
    this.source.setEditable(false);
    this.source.setPromptText("Источник термина, а не подтверждение механизма этого опыта");
    final var detail = new VBox(8, this.description, field("Источник термина", this.source));
    final var details = new TitledPane("О выбранной записи", detail);
    details.setExpanded(false);
    details.setId("reaction-classification-details");
    final var search =
        new HBox(8, this.query, button("Очистить", "clear", () -> this.query.clear()));
    HBox.setHgrow(this.query, Priority.ALWAYS);
    this.customText.setId("reaction-classification-custom-text");
    this.customText.setMaxWidth(Double.MAX_VALUE);
    this.customText.setPromptText("Собственное название — отдельно от поиска по каталогу");
    final var customRow = new HBox(8, this.customText, this.custom);
    HBox.setHgrow(this.customText, Priority.ALWAYS);
    this.customPane.setContent(customRow);
    this.customPane.setId("reaction-classification-custom-input");
    this.customPane.setExpanded(false);
    this.additional.setText("Дополнительно");
    this.additional.setContent(
        new VBox(10, this.rename, this.titlePreview, field("Пояснение к выбору", this.note)));
    this.additional.setExpanded(false);
    this.additional.setId("reaction-classification-additional");
    final var selection =
        new VBox(
            10,
            field("Названия реакции", this.nameChips),
            field("Механизмы · можно выбрать несколько", this.mechanismChips));
    selection.setMinHeight(Region.USE_PREF_SIZE);
    selection.setPadding(new Insets(12));
    selection.getStyleClass().add("laboratory-reaction-card");
    this.warning.setId("reaction-classification-warning");
    this.warning.getStyleClass().add("laboratory-warning");
    visible(this.warning, false);
    this.inputError.setId("reaction-classification-input-error");
    this.inputError.getStyleClass().add("laboratory-warning");
    visible(this.inputError, false);
    this.sheet
        .getChildren()
        .setAll(
            field("Стадия опыта", this.stage),
            this.warning,
            this.inputError,
            selection,
            new FlowPane(8, 8, this.reactions, this.mechanisms, this.category),
            search,
            this.count,
            this.matches,
            this.add,
            this.customPane,
            details,
            this.additional);
    this.sheet.setPadding(new Insets(12, 20, 20, 20));
    this.sheet.setMaxWidth(1080);
    this.sheet.setMinWidth(0);
    final var holder = new StackPane(this.sheet);
    StackPane.setAlignment(this.sheet, Pos.TOP_CENTER);
    final var scroll = scroll(holder);
    scroll.setFitToHeight(false);
    this.setCenter(scroll);
    this.leave
        .getChildren()
        .setAll(
            note("Изменения ещё не применены к стадии."),
            new FlowPane(
                8,
                8,
                button(
                    "Отбросить ввод и выйти",
                    "discard",
                    () -> {
                      this.reset();
                      this.finish();
                    }),
                button("Продолжить редактирование", "stay", () -> visible(this.leave, false))));
    visible(this.leave, false);
    final var bottom =
        new VBox(
            8,
            this.leave,
            new FlowPane(
                8,
                8,
                this.apply,
                this.reset,
                note("Enter — добавить из активного поля · Ctrl+Enter — применить")),
            this.status);
    bottom.setPadding(new Insets(10, 20, 14, 20));
    this.setBottom(bottom);
    this.stage
        .valueProperty()
        .addListener(
            (o, before, after) -> {
              if (this.changing) return;
              if (this.pending()) {
                this.changing = true;
                this.stage.setValue(before);
                this.changing = false;
                this.status.setText("Примените или сбросьте текущий ввод перед сменой стадии.");
                return;
              }
              this.load(after == null ? null : after.id());
            });
    this.types
        .selectedToggleProperty()
        .addListener(
            (o, before, after) -> {
              if (this.changing) return;
              if (after == null) {
                this.changing = true;
                this.types.selectToggle(before == null ? this.reactions : before);
                this.changing = false;
                return;
              }
              if (before != null) {
                this.queries.put((Kind) before.getUserData(), this.query.getText());
                this.customInputs.put((Kind) before.getUserData(), this.customText.getText());
              }
              this.changing = true;
              this.query.setText(this.queries.getOrDefault(this.kind(), ""));
              this.customText.setText(this.customInputs.getOrDefault(this.kind(), ""));
              this.changing = false;
              this.categories();
              this.search();
              this.controls();
            });
    this.category
        .valueProperty()
        .addListener(
            o -> {
              if (!this.changing) this.search();
            });
    this.query
        .textProperty()
        .addListener(
            o -> {
              if (!this.changing) {
                this.queries.put(this.kind(), this.query.getText());
                this.search();
              }
            });
    this.matches.getSelectionModel().selectedItemProperty().addListener(o -> this.details());
    this.note
        .textProperty()
        .addListener(
            o -> {
              if (!this.changing) this.controls();
            });
    this.rename
        .selectedProperty()
        .addListener(
            o -> {
              if (!this.changing) this.controls();
            });
    this.customText
        .textProperty()
        .addListener(
            o -> {
              if (!this.changing) {
                this.customInputs.put(this.kind(), this.customText.getText());
                this.controls();
              }
            });
    this.customText.setOnAction(e -> this.attempt(this::addCustom));
    this.query.setOnAction(e -> this.attempt(this::addSelected));
    this.matches.setOnKeyPressed(
        e -> {
          if (e.getCode() == KeyCode.ENTER && !e.isShortcutDown()) {
            this.attempt(this::addSelected);
            e.consume();
          }
        });
    this.matches.setOnMouseClicked(
        e -> {
          if (e.getClickCount() == 2) this.attempt(this::addSelected);
        });
    this.addEventFilter(
        KeyEvent.KEY_PRESSED,
        e -> {
          if (e.isShortcutDown() && e.getCode() == KeyCode.ENTER) {
            this.attempt(this::apply);
            e.consume();
          }
        });
    this.categories();
    this.load(stageId);
    this.search();
  }

  private String stageText(final LaboratoryReactionStage value) {
    if (value == null) return "Выберите стадию";
    for (int i = 0; i < this.stage.getItems().size(); i++)
      if (this.stage.getItems().get(i).id().equals(value.id()))
        return (i + 1) + " · " + value.title();
    return value.title();
  }

  private ListCell<LaboratoryReactionStage> stageCell() {
    return new ListCell<>() {
      @Override
      protected void updateItem(final LaboratoryReactionStage value, final boolean empty) {
        super.updateItem(value, empty);
        this.setText(empty || value == null ? null : stageText(value));
        this.setTooltip(
            empty || value == null
                ? null
                : new Tooltip(stageText(value) + "\nID стадии: " + value.id()));
        this.setAccessibleText(empty || value == null ? null : stageText(value));
      }
    };
  }

  private Kind unfinishedCustom() {
    for (final var kind : Kind.values())
      if (!(kind == this.kind()
              ? this.customText.getText()
              : this.customInputs.getOrDefault(kind, ""))
          .isEmpty()) return kind;
    return null;
  }

  private void showCustom(final Kind kind) {
    this.types.selectToggle(kind == Kind.REACTION ? this.reactions : this.mechanisms);
    this.customPane.setExpanded(true);
    this.customText.requestFocus();
  }

  private static VBox field(final String title, final Node node) {
    final var box = new VBox(6, note(title), node);
    box.setMinHeight(Region.USE_PREF_SIZE);
    return box;
  }

  private Button button(final String label, final String id, final Runnable action) {
    final var b = new Button(label);
    b.setId("reaction-classification-" + id);
    b.setMinWidth(Region.USE_PREF_SIZE);
    b.setOnAction(e -> this.attempt(action));
    return b;
  }

  private void attempt(final Runnable action) {
    try {
      action.run();
    } catch (RuntimeException error) {
      this.status.setText(Objects.toString(error.getMessage(), "Не удалось применить выбор."));
    }
  }

  private static void visible(final Node node, final boolean show) {
    node.setVisible(show);
    node.setManaged(show);
  }

  private Kind kind() {
    return this.types.getSelectedToggle() == null
        ? Kind.REACTION
        : (Kind) this.types.getSelectedToggle().getUserData();
  }

  private void categories() {
    final boolean old = this.changing;
    this.changing = true;
    this.category.getItems().setAll(ALL);
    this.category.getItems().addAll(LaboratoryReactionCatalog.categories(this.kind()));
    this.category.setValue(ALL);
    this.changing = old;
  }

  private void search() {
    if (this.changing) return;
    final String category =
        Objects.equals(this.category.getValue(), ALL) ? null : this.category.getValue();
    this.matches
        .getItems()
        .setAll(LaboratoryReactionCatalog.search(this.query.getText(), this.kind(), category));
    this.matches.setPrefHeight(
        Math.min(245, Math.max(86, this.matches.getItems().size() * 62 + 4)));
    this.matches.setMinHeight(Math.min(180, this.matches.getPrefHeight()));
    this.matches.getSelectionModel().selectFirst();
    this.count.setText(
        (this.kind() == Kind.REACTION ? "Реакций" : "Механизмов")
            + " в подборке: "
            + this.matches.getItems().size());
    this.controls();
  }

  private void details() {
    final var e = this.matches.getSelectionModel().getSelectedItem();
    this.add.setDisable(e == null || this.selected == null);
    this.description.setText(e == null ? "Выберите запись справочника." : e.description());
    this.source.setText(e == null ? "" : e.sourceUrl());
  }

  private void addSelected() {
    final var entry = this.matches.getSelectionModel().getSelectedItem();
    if (entry == null)
      throw new IllegalArgumentException(
          "Выберите найденную запись или добавьте свою формулировку.");
    this.add(entry.kind(), new Term(entry.id(), entry.nameRu()));
  }

  private void addCustom() {
    final String value = this.customText.getText().strip();
    if (value.isBlank())
      throw new IllegalArgumentException("Введите собственную формулировку в поле «Свой текст».");
    this.add(this.kind(), new Term(null, value));
    this.customText.clear();
  }

  private static String normalized(final String text) {
    return Normalizer.normalize(text, Normalizer.Form.NFKC)
        .strip()
        .toLowerCase(Locale.ROOT)
        .replace('ё', 'е')
        .replaceAll("\\s+", " ");
  }

  private static String brief(final String text) {
    return text.length() <= 90 ? text : text.substring(0, 87) + "…";
  }

  private void add(final Kind kind, final Term term) {
    if (this.selected == null)
      throw new IllegalArgumentException("Сначала добавьте стадию в опыт.");
    final List<Term> current = kind == Kind.REACTION ? this.names : this.paths;
    if (current.stream()
        .anyMatch(
            t ->
                term.catalogId() != null && term.catalogId().equals(t.catalogId())
                    || normalized(t.text()).equals(normalized(term.text())))) {
      this.status.setText("«" + brief(term.text()) + "» уже выбрано.");
      return;
    }
    final var next = new ArrayList<>(current);
    next.add(term);
    new LaboratoryReactionClassification(
        kind == Kind.REACTION ? next : this.names,
        kind == Kind.MECHANISM ? next : this.paths,
        this.note.getText());
    current.add(term);
    this.chips();
    this.controls();
    this.status.setText("Добавлено: " + brief(term.text()) + ". Примените выбор к стадии.");
  }

  private void chips() {
    this.chips(this.nameChips, this.names, "Название не задано", Kind.REACTION);
    this.chips(this.mechanismChips, this.paths, "Механизм не задан", Kind.MECHANISM);
  }

  private void chips(
      final FlowPane pane, final List<Term> values, final String empty, final Kind kind) {
    pane.getChildren().clear();
    if (values.isEmpty()) {
      pane.getChildren().add(note(empty));
      return;
    }
    for (final Term term : List.copyOf(values)) {
      final var label = note(term.text() + (term.catalogId() == null ? " · своё" : ""));
      label.setMaxWidth(560);
      final var remove =
          button(
              "×",
              "remove-" + kind + "-" + values.indexOf(term),
              () -> {
                values.remove(term);
                this.chips();
                this.controls();
              });
      remove.setAccessibleText("Убрать «" + term.text() + "»");
      remove.setTooltip(new Tooltip("Убрать из выбранного"));
      label.setMinWidth(0);
      label
          .prefWidthProperty()
          .bind(
              javafx.beans.binding.Bindings.createDoubleBinding(
                  () -> {
                    final var measure = new javafx.scene.text.Text(label.getText());
                    measure.setFont(label.getFont());
                    return Math.min(
                        measure.getLayoutBounds().getWidth() + 2,
                        Math.max(80, Math.min(560, pane.getWidth() - 84)));
                  },
                  pane.widthProperty(),
                  label.fontProperty(),
                  label.textProperty()));
      final var chip = new HBox(8, label, remove);
      chip.prefWidthProperty().bind(label.prefWidthProperty().add(64));
      chip.prefHeightProperty()
          .bind(
              javafx.beans.binding.Bindings.createDoubleBinding(
                  () -> Math.max(32, label.prefHeight(label.getPrefWidth())) + 12,
                  label.prefWidthProperty(),
                  label.fontProperty(),
                  label.textProperty()));
      chip.setMinHeight(Region.USE_PREF_SIZE);
      chip.setAlignment(Pos.CENTER_LEFT);
      chip.setPadding(new Insets(5, 8, 5, 10));
      chip.getStyleClass().add("laboratory-reaction-card");
      pane.getChildren().add(chip);
    }
  }

  private String title() {
    return this.rename.isSelected() && !this.names.isEmpty()
        ? this.names.getFirst().text()
        : this.selected == null ? "" : this.selected.title();
  }

  private LaboratoryReactionClassification value() {
    return new LaboratoryReactionClassification(this.names, this.paths, this.note.getText());
  }

  private void controls() {
    final Kind unfinished = this.unfinishedCustom();
    this.customPane.setText("Свой текст" + (unfinished == null ? "" : " · есть ввод"));
    this.custom.setText(
        this.kind() == Kind.REACTION ? "Добавить своё название" : "Добавить свой механизм");
    this.customText.setPromptText(
        this.kind() == Kind.REACTION
            ? "Собственное название реакции"
            : "Собственная формулировка механизма");
    String problem = "";
    try {
      this.value();
    } catch (final IllegalArgumentException invalid) {
      problem = Objects.toString(invalid.getMessage(), "Исправьте пояснение к выбору.");
    }
    this.inputError.setText(problem);
    visible(this.inputError, !problem.isEmpty());
    if (!problem.isEmpty()) this.additional.setExpanded(true);
    this.stage.setTooltip(
        this.selected == null
            ? null
            : new Tooltip(this.stageText(this.selected) + "\nID стадии: " + this.selected.id()));
    this.titlePreview.setText(
        this.rename.isSelected()
            ? this.names.isEmpty()
                ? "Сначала выберите название реакции."
                : "Заголовок стадии: " + this.names.getFirst().text()
            : "Заголовок стадии сохраняется: "
                + (this.selected == null ? "—" : this.selected.title()));
    this.apply.setDisable(
        this.selected == null || this.conflict || this.rename.isSelected() && this.names.isEmpty());
    this.custom.setDisable(this.selected == null);
    this.details();
  }

  private void load(final String id) {
    this.changing = true;
    try {
      this.expected = this.editor.document();
      this.stage.getItems().setAll(this.expected.experiment().stages());
      this.selected =
          this.stage.getItems().stream()
              .filter(s -> s.id().equals(id))
              .findFirst()
              .orElse(this.stage.getItems().isEmpty() ? null : this.stage.getItems().getFirst());
      this.stage.setValue(this.selected);
      this.names.clear();
      this.paths.clear();
      if (this.selected != null) {
        this.names.addAll(this.selected.classification().reactionNames());
        this.paths.addAll(this.selected.classification().mechanisms());
      }
      this.note.setText(this.selected == null ? "" : this.selected.classification().note());
      this.rename.setSelected(false);
      this.customInputs.clear();
      this.customText.clear();
      this.customPane.setExpanded(false);
      this.additional.setExpanded(false);
      this.conflict = false;
      this.warning.setText("");
      visible(this.warning, false);
      visible(this.leave, false);
    } finally {
      this.changing = false;
    }
    this.chips();
    this.controls();
    this.status.setText(
        this.selected == null
            ? "В опыте нет стадий. Добавьте стадию в схему реакции."
            : "Выберите название и механизмы, затем примените их к стадии.");
  }

  private void reset() {
    this.load(this.selected == null ? null : this.selected.id());
  }

  private void apply() {
    if (this.selected == null) throw new IllegalArgumentException("В опыте нет выбранной стадии.");
    final Kind unfinished = this.unfinishedCustom();
    if (unfinished != null) {
      this.showCustom(unfinished);
      throw new IllegalArgumentException(
          "Свой текст ещё не добавлен. Добавьте его к выбранному или очистите поле.");
    }
    if (this.conflict || !this.editor.document().equals(this.expected)) {
      this.conflict = true;
      this.warning.setText(
          "Опыт изменился. Ваш ввод сохранён на странице; применение заблокировано. Нажмите «Сбросить ввод», чтобы заново открыть текущую версию.");
      visible(this.warning, true);
      this.controls();
      throw new IllegalArgumentException(this.warning.getText());
    }
    if (this.rename.isSelected() && this.names.isEmpty())
      throw new IllegalArgumentException("Для изменения заголовка выберите название реакции.");
    this.editor.classification(this.selected, this.value(), this.title());
    final String id = this.selected.id();
    this.load(id);
    this.status.setText(
        "Название и механизмы применены к стадии. Изменение можно отменить одним действием.");
  }

  boolean pending() {
    if (this.conflict || this.unfinishedCustom() != null) return true;
    if (this.selected == null) return false;
    try {
      return !this.value().equals(this.selected.classification())
          || !this.title().equals(this.selected.title());
    } catch (RuntimeException invalid) {
      return true;
    }
  }

  @Override
  boolean canClose() {
    if (!super.canClose()) return false;
    if (this.pending()) {
      final Kind unfinished = this.unfinishedCustom();
      if (unfinished != null) this.showCustom(unfinished);
      visible(this.leave, true);
      this.status.setText(
          unfinished == null
              ? "Примените изменения или явно отбросьте ввод."
              : "Свой текст ещё не добавлен. Добавьте или очистите его; для выхода можно явно отбросить ввод.");
      return false;
    }
    return true;
  }

  void draft(final Properties data, final boolean restore) {
    if (!restore) {
      data.setProperty(PREFIX + "source", new LaboratoryFileCodec().write(this.expected));
      data.setProperty(PREFIX + "stage", this.selected == null ? "" : this.selected.id());
      data.setProperty(PREFIX + "note", this.note.getText());
      data.setProperty(PREFIX + "rename", Boolean.toString(this.rename.isSelected()));
      data.setProperty(PREFIX + "conflict", Boolean.toString(this.conflict));
      data.setProperty(PREFIX + "kind", this.kind().name());
      data.setProperty(PREFIX + "category", Objects.toString(this.category.getValue(), ALL));
      data.setProperty(PREFIX + "query", this.query.getText());
      for (final var kind : Kind.values())
        data.setProperty(
            PREFIX + "query." + kind.name(),
            kind == this.kind() ? this.query.getText() : this.queries.getOrDefault(kind, ""));
      for (final var kind : Kind.values())
        data.setProperty(
            PREFIX + "custom." + kind.name(),
            kind == this.kind()
                ? this.customText.getText()
                : this.customInputs.getOrDefault(kind, ""));
      data.setProperty(PREFIX + "customExpanded", Boolean.toString(this.customPane.isExpanded()));
      data.setProperty(
          PREFIX + "additionalExpanded", Boolean.toString(this.additional.isExpanded()));
      terms(data, "names", this.names);
      terms(data, "mechanisms", this.paths);
      return;
    }
    final var original =
        new LaboratoryFileCodec()
            .read(
                Objects.requireNonNull(
                    data.getProperty(PREFIX + "source"),
                    "Отсутствует исходный снимок классификации."));
    final String id = data.getProperty(PREFIX + "stage", "");
    final var selected =
        original.experiment().stages().stream()
            .filter(s -> s.id().equals(id))
            .findFirst()
            .orElse(null);
    final var names = terms(data, "names");
    final var mechanisms = terms(data, "mechanisms");
    final var classification = new LaboratoryReactionClassification(names, mechanisms, "");
    final String rawNote = data.getProperty(PREFIX + "note", "");
    this.changing = true;
    try {
      this.expected = original;
      this.stage.getItems().setAll(original.experiment().stages());
      this.selected = selected;
      this.stage.setValue(selected);
      this.names.clear();
      this.names.addAll(classification.reactionNames());
      this.paths.clear();
      this.paths.addAll(classification.mechanisms());
      this.note.setText(rawNote);
      this.rename.setSelected(Boolean.parseBoolean(data.getProperty(PREFIX + "rename")));
      this.types.selectToggle(
          Kind.valueOf(data.getProperty(PREFIX + "kind", "REACTION")) == Kind.REACTION
              ? this.reactions
              : this.mechanisms);
      this.categories();
      final String category = data.getProperty(PREFIX + "category", ALL);
      this.category.setValue(this.category.getItems().contains(category) ? category : ALL);
      for (final var kind : Kind.values())
        this.queries.put(
            kind,
            data.getProperty(
                PREFIX + "query." + kind.name(),
                kind == this.kind() ? data.getProperty(PREFIX + "query", "") : ""));
      this.query.setText(this.queries.getOrDefault(this.kind(), ""));
      this.customInputs.clear();
      for (final var kind : Kind.values())
        this.customInputs.put(kind, data.getProperty(PREFIX + "custom." + kind.name(), ""));
      this.customText.setText(this.customInputs.getOrDefault(this.kind(), ""));
      this.customPane.setExpanded(
          Boolean.parseBoolean(data.getProperty(PREFIX + "customExpanded"))
              || this.unfinishedCustom() != null);
      this.additional.setExpanded(
          Boolean.parseBoolean(data.getProperty(PREFIX + "additionalExpanded")));
      this.conflict =
          selected == null
              || !original.equals(this.editor.document())
              || Boolean.parseBoolean(data.getProperty(PREFIX + "conflict"));
      this.warning.setText(
          this.conflict
              ? "Черновик относится к другой версии опыта. Исходный снимок и выбор сохранены; применение заблокировано до явного сброса."
              : "");
      visible(this.warning, this.conflict);
    } finally {
      this.changing = false;
    }
    this.chips();
    this.search();
    this.controls();
    this.status.setText(
        this.conflict
            ? "Восстановлен конфликтующий ввод. Проверьте его перед сбросом."
            : "Выбор восстановлен. Примените его к стадии после проверки.");
  }

  private static void terms(final Properties data, final String key, final List<Term> values) {
    data.setProperty(PREFIX + key + ".count", Integer.toString(values.size()));
    for (int i = 0; i < values.size(); i++) {
      final String p = PREFIX + key + "." + i + ".";
      data.setProperty(p + "id", Objects.toString(values.get(i).catalogId(), ""));
      data.setProperty(p + "text", values.get(i).text());
    }
  }

  private static List<Term> terms(final Properties data, final String key) {
    final int count;
    try {
      count = Integer.parseInt(data.getProperty(PREFIX + key + ".count", "0"));
    } catch (NumberFormatException invalid) {
      throw new IllegalArgumentException("Повреждён список выбранных терминов.");
    }
    if (count < 0 || count > 64)
      throw new IllegalArgumentException("В одной группе допускается до64 выбранных терминов.");
    final var values = new ArrayList<Term>();
    for (int i = 0; i < count; i++) {
      final String p = PREFIX + key + "." + i + ".";
      final String id = data.getProperty(p + "id", "");
      values.add(new Term(id.isBlank() ? null : id, data.getProperty(p + "text", "")));
    }
    return List.copyOf(values);
  }
}

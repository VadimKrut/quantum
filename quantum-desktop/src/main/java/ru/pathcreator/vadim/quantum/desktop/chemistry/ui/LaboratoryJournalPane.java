/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryEntry;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEditor;

final class LaboratoryJournalPane extends VBox {
  private final ListView<LaboratoryEntry> entries = new ListView<>();
  private final TextArea text = new TextArea();
  private final ComboBox<LaboratoryEntry.Kind> kind = new ComboBox<>();
  private final TextField search = new TextField();
  private final ComboBox<String> filter = new ComboBox<>();
  private final Label count = new Label();
  private final TextArea preview = new TextArea();
  private final javafx.scene.control.TitledPane reading = new javafx.scene.control.TitledPane();
  private final Label correctionHint = new Label();
  private String experimentId;
  private String correctionId;
  private final LaboratoryEditor editor;

  LaboratoryJournalPane(
      final LaboratoryEditor editor,
      final java.util.function.Consumer<Runnable> action,
      final Runnable changed) {
    super(7);
    this.editor = editor;
    this.experimentId = editor.document().experiment().id();
    this.search.setPromptText("Поиск по тексту записи");
    this.search.setId("chemistry-journal-search");
    this.filter.getItems().add("Все записи");
    for (final var value : LaboratoryEntry.Kind.values()) {
      this.filter.getItems().add(value.toString());
    }
    this.filter.setValue("Все записи");
    this.filter.setId("chemistry-journal-filter");
    this.kind
        .getItems()
        .setAll(
            LaboratoryEntry.Kind.OBSERVATION,
            LaboratoryEntry.Kind.RESULT,
            LaboratoryEntry.Kind.CORRECTION);
    this.kind.setValue(LaboratoryEntry.Kind.OBSERVATION);
    this.text.setPromptText("Наблюдение, результат или поправка к выбранной записи");
    this.text.setWrapText(true);
    this.text.setStyle("-fx-font-family: 'System'; -fx-font-size: 13px;");
    this.text.setPrefRowCount(3);
    this.text.setId("chemistry-journal-text");
    this.entries.setId("chemistry-journal-entries");
    this.kind.setId("chemistry-journal-kind");
    this.preview.setId("chemistry-journal-preview");
    this.preview.setEditable(false);
    this.preview.setStyle("-fx-font-family: 'System'; -fx-font-size: 13px;");
    this.preview.setWrapText(true);
    this.preview.setPrefRowCount(5);
    this.preview.setPromptText("Выберите запись для полного просмотра и копирования текста");
    this.correctionHint.setWrapText(true);
    this.count.setWrapText(true);
    this.text.textProperty().addListener((o, a, b) -> changed.run());
    this.entries.setPrefHeight(200);
    this.entries.setMinHeight(120);
    this.entries.setPlaceholder(new Label("Нет записей по выбранным условиям"));
    this.entries.setStyle("-fx-font-family: 'System'; -fx-font-size: 13px;");
    this.entries.setCellFactory(
        list ->
            new ListCell<>() {
              {
                this.prefWidthProperty().bind(list.widthProperty().subtract(24));
                this.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
              }

              protected void updateItem(final LaboratoryEntry entry, final boolean empty) {
                super.updateItem(entry, empty);
                this.setText(
                    empty || entry == null
                        ? null
                        : DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                                .withZone(ZoneId.systemDefault())
                                .format(entry.recordedAt())
                            + " · "
                            + entry.kind()
                            + "\n"
                            + summary(entry.text())
                            + (entry.correctsId() == null ? "" : "\nСвязана с исходной записью"));
                this.setWrapText(true);
              }
            });
    final Button add = new Button("Записать в журнал");
    add.setId("chemistry-journal-add");
    add.getStyleClass().add("laboratory-save");
    add.disableProperty().bind(this.text.textProperty().isEmpty());
    add.setTooltip(new javafx.scene.control.Tooltip("Записать наблюдение · Ctrl+Enter"));
    add.setOnAction(event -> action.accept(this::flush));
    this.text.addEventFilter(
        javafx.scene.input.KeyEvent.KEY_PRESSED,
        event -> {
          if (event.isShortcutDown() && event.getCode() == javafx.scene.input.KeyCode.ENTER) {
            if (!add.isDisabled()) {
              add.fire();
            }
            event.consume();
          }
        });
    final Button correct = new Button("Исправить выбранную запись");
    correct.setId("chemistry-journal-correct");
    correct
        .disableProperty()
        .bind(this.entries.getSelectionModel().selectedItemProperty().isNull());
    correct.setOnAction(
        event -> {
          this.kind.setValue(LaboratoryEntry.Kind.CORRECTION);
          this.correctionId = this.entries.getSelectionModel().getSelectedItem().id();
          this.updateCorrectionHint();
          changed.run();
          this.text.requestFocus();
        });
    this.kind
        .valueProperty()
        .addListener(
            (o, before, after) -> {
              final var selected = this.entries.getSelectionModel().getSelectedItem();
              this.correctionId =
                  after == LaboratoryEntry.Kind.CORRECTION && selected != null
                      ? selected.id()
                      : null;
              this.updateCorrectionHint();
              changed.run();
            });
    this.entries
        .getSelectionModel()
        .selectedItemProperty()
        .addListener((o, before, after) -> this.updatePreview());
    this.search.textProperty().addListener((o, before, after) -> this.refresh());
    this.filter.valueProperty().addListener((o, before, after) -> this.refresh());
    final Label hint =
        new Label(
            "Записи сохраняются при отмене правок. Исправление добавляется отдельной записью.");
    hint.setWrapText(true);
    this.reading.setText("Запись целиком и поправки");
    this.reading.setId("chemistry-journal-reading");
    this.reading.setAnimated(false);
    this.reading.setExpanded(false);
    this.reading.setContent(new VBox(8, this.preview, correct));
    this.getChildren()
        .addAll(
            new Label("Новое наблюдение"),
            this.kind,
            this.correctionHint,
            this.text,
            add,
            new javafx.scene.control.Separator(),
            new Label("История опыта"),
            this.search,
            this.filter,
            this.count,
            this.entries,
            this.reading,
            hint);
    this.refresh();
  }

  boolean pending() {
    return !this.text.getText().isBlank();
  }

  void flush() {
    if (!this.pending()) {
      return;
    }
    if (this.kind.getValue() == LaboratoryEntry.Kind.CORRECTION
        && this.find(this.correctionId) == null) {
      throw new IllegalArgumentException(
          "Выберите запись текущего опыта и нажмите «Исправить выбранную запись».");
    }
    this.editor.record(
        this.kind.getValue(),
        this.text.getText(),
        this.kind.getValue() == LaboratoryEntry.Kind.CORRECTION ? this.correctionId : null);
    this.text.clear();
    this.kind.setValue(LaboratoryEntry.Kind.OBSERVATION);
    this.correctionId = null;
    this.updateCorrectionHint();
  }

  void reset() {
    this.experimentId = this.editor.document().experiment().id();
    this.entries.getSelectionModel().clearSelection();
    this.correctionId = null;
    this.kind.setValue(LaboratoryEntry.Kind.OBSERVATION);
    this.text.clear();
    this.search.clear();
    this.filter.setValue("Все записи");
    this.refresh();
  }

  void refresh() {
    if (!this.experimentId.equals(this.editor.document().experiment().id())) {
      this.reset();
      return;
    }
    final var selected = this.entries.getSelectionModel().getSelectedItem();
    final String query = this.search.getText().strip().toLowerCase(Locale.ROOT);
    final var journal = this.editor.document().experiment().journal();
    this.entries
        .getItems()
        .setAll(
            journal.reversed().stream()
                .filter(
                    entry ->
                        this.filter.getValue().equals("Все записи")
                            || entry.kind().toString().equals(this.filter.getValue()))
                .filter(
                    entry -> LaboratoryTextSearch.matches(entry.text() + " " + entry.id(), query))
                .toList());
    this.entries.getSelectionModel().clearSelection();
    if (selected != null) {
      this.entries.getItems().stream()
          .filter(entry -> entry.id().equals(selected.id()))
          .findFirst()
          .ifPresent(entry -> this.entries.getSelectionModel().select(entry));
    }
    this.count.setText(
        "Показано "
            + this.entries.getItems().size()
            + " из "
            + journal.size()
            + " · новые записи сверху");
    this.updatePreview();
    this.updateCorrectionHint();
  }

  private LaboratoryEntry find(final String id) {
    return id == null
        ? null
        : this.editor.document().experiment().journal().stream()
            .filter(entry -> entry.id().equals(id))
            .findFirst()
            .orElse(null);
  }

  private static String summary(final String text) {
    final String value = text.replaceAll("\\s+", " ");
    return value.length() <= 170 ? value : value.substring(0, 170) + "…";
  }

  private static String description(final LaboratoryEntry entry) {
    return DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault())
            .format(entry.recordedAt())
        + " · "
        + entry.kind()
        + "\n"
        + entry.text();
  }

  private void updatePreview() {
    final var selected = this.entries.getSelectionModel().getSelectedItem();
    if (selected == null) {
      this.preview.clear();
      this.reading.setExpanded(false);
      return;
    }
    this.reading.setExpanded(true);
    final StringBuilder content = new StringBuilder(description(selected));
    final var original = this.find(selected.correctsId());
    if (original != null) {
      content.append("\n\nИсходная запись:\n").append(description(original));
    }
    this.editor.document().experiment().journal().stream()
        .filter(entry -> selected.id().equals(entry.correctsId()))
        .forEach(entry -> content.append("\n\nПоправка:\n").append(description(entry)));
    this.preview.setText(content.toString());
    this.preview.positionCaret(0);
  }

  private void updateCorrectionHint() {
    final boolean correcting = this.kind.getValue() == LaboratoryEntry.Kind.CORRECTION;
    final var target = this.find(this.correctionId);
    this.correctionHint.setManaged(correcting);
    this.correctionHint.setVisible(correcting);
    this.correctionHint.setText(
        target == null
            ? "Выберите исходную запись и нажмите «Исправить выбранную запись»."
            : "Поправка к: "
                + summary(target.text())
                + "\nПоиск и просмотр других записей не меняют эту привязку.");
  }

  void draft(final java.util.Properties data, final boolean restore) {
    LaboratoryFormDraft.fields(
        data, "journal", restore, java.util.Map.of("text", this.text, "kind", this.kind));
    if (restore) {
      this.correctionId =
          data.getProperty("journal.corrects", data.getProperty("journal.selected"));
      this.entries.getItems().stream()
          .filter(entry -> entry.id().equals(data.getProperty("journal.selected")))
          .findFirst()
          .ifPresent(entry -> this.entries.getSelectionModel().select(entry));
    } else if (this.entries.getSelectionModel().getSelectedItem() != null) {
      data.setProperty("journal.selected", this.entries.getSelectionModel().getSelectedItem().id());
    }
    if (!restore && this.correctionId != null) {
      data.setProperty("journal.corrects", this.correctionId);
    }
    if (restore) {
      this.updateCorrectionHint();
      this.updatePreview();
    }
  }
}

/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAttachment;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEditor;

final class LaboratoryAttachmentPane extends VBox {
  private final ListView<LaboratoryAttachment> attachments = new ListView<>();
  private final LaboratoryEditor editor;
  private final TextField search = new TextField();
  private final Label count = new Label();
  private String experimentId;
  private final LaboratoryFiles files;
  private java.nio.file.Path sourcePath;
  private final java.util.Map<
          String,
          java.util.Optional<
              ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure
                  .LaboratoryCalculationArchive>>
      calculations = new java.util.HashMap<>();
  private final java.util.Map<String, String> calculationErrors = new java.util.HashMap<>();
  private final java.util.Set<String> loading = new java.util.HashSet<>();
  private final java.util.concurrent.ExecutorService reader =
      java.util.concurrent.Executors.newSingleThreadExecutor(
          task -> {
            final Thread thread = new Thread(task, "laboratory-attachment-index");
            thread.setDaemon(true);
            return thread;
          });
  private long generation;

  LaboratoryAttachmentPane(final LaboratoryEditor editor, final LaboratoryFiles files) {
    super(8);
    this.editor = editor;
    this.files = files;
    this.experimentId = editor.document().experiment().id();
    this.search.setPromptText("Найти вложение по названию");
    this.search.setId("chemistry-attachment-search");
    this.attachments.setId("chemistry-attachment-list");
    this.attachments.setPrefHeight(260);
    this.attachments.setStyle("-fx-font-family: 'System'; -fx-font-size: 13px;");
    this.attachments.setPlaceholder(new Label("Вложения не найдены"));
    this.search.textProperty().addListener((o, before, after) -> this.refresh());
    this.attachments.setCellFactory(
        list ->
            new ListCell<>() {
              {
                this.prefWidthProperty().bind(list.widthProperty().subtract(24));
                this.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
              }

              protected void updateItem(final LaboratoryAttachment item, final boolean empty) {
                super.updateItem(item, empty);
                this.setText(
                    empty || item == null
                        ? null
                        : item.name() + "\n" + size(item.size()) + calculationState(item));
                this.setWrapText(true);
              }
            });
    final Button add = new Button("Добавить файл…");
    add.setOnAction(event -> files.attach());
    final Button save = new Button("Сохранить как…");
    save.disableProperty()
        .bind(this.attachments.getSelectionModel().selectedItemProperty().isNull());
    save.setOnAction(
        event -> files.extract(this.attachments.getSelectionModel().getSelectedItem()));
    final Button view = new Button("Открыть");
    view.setId("chemistry-attachment-open");
    save.setId("chemistry-attachment-save");
    add.setId("chemistry-attachment-add");
    view.disableProperty()
        .bind(this.attachments.getSelectionModel().selectedItemProperty().isNull());
    view.setOnAction(
        event -> files.preview(this.attachments.getSelectionModel().getSelectedItem()));
    this.attachments.setOnKeyPressed(
        event -> {
          if (event.getCode() == javafx.scene.input.KeyCode.ENTER
              && !event.isShortcutDown()
              && !view.isDisabled()) {
            view.fire();
            event.consume();
          }
        });
    this.attachments.setOnMouseClicked(
        event -> {
          if (event.getButton() != javafx.scene.input.MouseButton.PRIMARY
              || event.getClickCount() != 2) {
            return;
          }
          javafx.scene.Node node = event.getPickResult().getIntersectedNode();
          while (node != null && !(node instanceof ListCell<?>)) {
            node = node.getParent();
          }
          if (node instanceof ListCell<?> cell && !cell.isEmpty() && !view.isDisabled()) {
            view.fire();
            event.consume();
          }
        });
    final Button recheck = new Button("Обновить статусы");
    recheck.setOnAction(
        event -> {
          this.resetIndex();
          this.refresh();
        });
    final Label hint =
        new Label("Файлы хранятся вместе с опытом. Для переноса используйте «Экспорт пакета».");
    hint.setWrapText(true);
    this.getChildren()
        .addAll(
            new Label("Спектры, фотографии, отчёты"),
            this.search,
            this.count,
            this.attachments,
            new FlowPane(8, 8, add, view, save, recheck),
            hint);
  }

  void refresh() {
    if (!this.experimentId.equals(this.editor.document().experiment().id())) {
      this.experimentId = this.editor.document().experiment().id();
      this.resetIndex();
      this.attachments.getSelectionModel().clearSelection();
      this.search.clear();
    }
    if (!java.util.Objects.equals(this.sourcePath, this.files.assetPath())) {
      this.sourcePath = this.files.assetPath();
      this.resetIndex();
    }
    final var selected = this.attachments.getSelectionModel().getSelectedItem();
    final String query = this.search.getText().strip().toLowerCase(java.util.Locale.ROOT);
    final var files = this.editor.document().experiment().attachments();
    this.attachments
        .getItems()
        .setAll(
            files.stream()
                .filter(item -> LaboratoryTextSearch.matches(item.name(), query))
                .toList());
    this.attachments.getSelectionModel().clearSelection();
    if (selected != null) {
      this.attachments.getItems().stream()
          .filter(item -> item.id().equals(selected.id()))
          .findFirst()
          .ifPresent(item -> this.attachments.getSelectionModel().select(item));
    }
    files.forEach(this::loadCalculation);
    this.count.setText("Показано " + this.attachments.getItems().size() + " из " + files.size());
    this.attachments.refresh();
  }

  private void resetIndex() {
    this.generation++;
    this.calculations.clear();
    this.calculationErrors.clear();
    this.loading.clear();
  }

  private String calculationState(final LaboratoryAttachment item) {
    if (this.calculationErrors.containsKey(item.sha256())) {
      return "\n" + this.calculationErrors.get(item.sha256());
    }
    final var cached = this.calculations.get(item.sha256());
    if (cached == null) {
      return this.loading.contains(item.sha256()) ? "\nПроверка снимка…" : "";
    }
    return cached
        .map(
            archive ->
                "\n" + archive.status() + " · " + archive.relation(this.editor.document()).text())
        .orElse("");
  }

  private void loadCalculation(final LaboratoryAttachment item) {
    if (!item.name().toLowerCase(java.util.Locale.ROOT).endsWith(".qchem")
        || item.size() > 32L * 1024 * 1024
        || this.calculations.containsKey(item.sha256())
        || this.calculationErrors.containsKey(item.sha256())
        || !this.loading.add(item.sha256())) {
      return;
    }
    final long requestGeneration = this.generation;
    final java.nio.file.Path document = this.files.assetPath();
    this.reader.submit(
        () -> {
          ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryCalculationArchive
              archive = null;
          String error = null;
          try {
            final var path =
                new ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure
                        .LaboratoryAssetStore()
                    .locate(document, item);
            final var parsed =
                new ru.pathcreator.vadim.quantum.chemistry.storage.application
                        .ChemistryStorageService()
                    .read(java.nio.file.Files.readString(path));
            if (!parsed.success()) {
              error = "Не удалось прочитать снимок QCHEM";
            } else if (parsed.value().metadata().containsKey("calculation.kind")) {
              archive =
                  new ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure
                      .LaboratoryCalculationArchive(parsed.value());
            }
          } catch (final Exception exception) {
            error = "Не удалось проверить вложение: " + exception.getMessage();
          }
          final var result = archive;
          final String failure = error;
          javafx.application.Platform.runLater(
              () -> {
                if (requestGeneration != this.generation) {
                  return;
                }
                this.loading.remove(item.sha256());
                if (failure == null) {
                  this.calculations.put(item.sha256(), java.util.Optional.ofNullable(result));
                } else {
                  this.calculationErrors.put(item.sha256(), failure);
                }
                this.attachments.refresh();
              });
        });
  }

  private static String size(final long bytes) {
    if (bytes < 1024) {
      return bytes + " байт";
    }
    if (bytes < 1024 * 1024) {
      return String.format(java.util.Locale.ROOT, "%.1f КиБ", bytes / 1024.0);
    }
    return String.format(java.util.Locale.ROOT, "%.1f МиБ", bytes / (1024.0 * 1024));
  }
}

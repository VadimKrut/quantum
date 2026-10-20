/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.nio.file.Path;
import java.util.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAttachment;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEditor;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryAssetStore;

/** Stage the complete selection before adding any attachment to the experiment. */
final class LaboratoryAttachmentImportPage extends LaboratoryComputationPage {
  private final LaboratoryEditor editor;
  private final String experiment;
  private final List<Path> paths;
  private final ListView<String> list = new ListView<>();
  private final Button apply = new Button("Добавить к опыту"),
      retry = new Button("Повторить подготовку");
  private List<LaboratoryAttachment> prepared = List.of();
  private javafx.concurrent.Task<List<LaboratoryAttachment>> task;
  private long generation;
  private boolean applied;

  LaboratoryAttachmentImportPage(
      final LaboratoryEditor editor, final List<Path> paths, final Runnable close) {
    super("Добавить вложения", close);
    this.setId("chemistry-attachment-import-page");
    this.setMinSize(0, 0);
    this.editor = editor;
    this.experiment = editor.document().experiment().id();
    this.paths = List.copyOf(paths);
    this.list.setId("chemistry-attachment-import-list");
    this.list.setStyle("-fx-font-family: 'System'; -fx-font-size: 13px;");
    this.list.setMinSize(0, 100);
    this.list.setCellFactory(
        view ->
            new ListCell<>() {
              protected void updateItem(final String value, final boolean empty) {
                super.updateItem(value, empty);
                this.setText(empty ? null : value);
                this.setWrapText(true);
                this.setPrefWidth(Math.max(120, view.getWidth() - 24));
              }
            });
    this.apply.setId("chemistry-attachment-import-apply");
    this.retry.setId("chemistry-attachment-import-retry");
    this.apply.setOnAction(e -> this.apply());
    this.retry.setOnAction(e -> this.prepare());
    final VBox body =
        new VBox(
            8,
            note(
                "Файлы копируются и проверяются в фоне. Они появятся в опыте после нажатия «Добавить к опыту». Одинаковые имя и содержимое повторно не добавляются."),
            this.list);
    body.setPadding(new javafx.geometry.Insets(0, 16, 8, 16));
    VBox.setVgrow(this.list, Priority.ALWAYS);
    this.setCenter(body);
    final VBox footer = new VBox(8, this.status, new FlowPane(8, 6, this.apply, this.retry));
    footer.setPadding(new javafx.geometry.Insets(8, 16, 12, 16));
    this.setBottom(footer);
    this.prepare();
  }

  private void prepare() {
    if (this.disposed) {
      return;
    }
    final long version = ++this.generation;
    if (this.task != null) {
      this.task.cancel(true);
    }
    this.prepared = List.of();
    this.apply.setDisable(true);
    this.retry.setDisable(true);
    this.list.getItems().setAll(this.paths.stream().map(p -> p.getFileName().toString()).toList());
    this.busy(true);
    final var loading =
        new javafx.concurrent.Task<List<LaboratoryAttachment>>() {
          protected List<LaboratoryAttachment> call() throws Exception {
            final var result = new ArrayList<LaboratoryAttachment>();
            final var store = new LaboratoryAssetStore();
            for (int i = 0; i < paths.size(); i++) {
              if (this.isCancelled()) {
                throw new java.util.concurrent.CancellationException();
              }
              this.updateMessage(
                  "Подготовка "
                      + (i + 1)
                      + " / "
                      + paths.size()
                      + " · "
                      + paths.get(i).getFileName());
              result.add(store.stage(paths.get(i)));
            }
            return List.copyOf(result);
          }
        };
    this.task = loading;
    this.status.textProperty().unbind();
    this.status.textProperty().bind(loading.messageProperty());
    loading.setOnSucceeded(
        e -> {
          if (this.disposed || version != this.generation) {
            return;
          }
          this.task = null;
          this.busy(false);
          this.status.textProperty().unbind();
          this.prepared = loading.getValue();
          this.list
              .getItems()
              .setAll(
                  this.prepared.stream()
                      .map(a -> a.name() + " · " + a.size() + " байт · готово")
                      .toList());
          this.status.setText(
              "Подготовлено файлов: " + this.prepared.size() + ". Опыт пока не изменён.");
          this.apply.setDisable(this.prepared.isEmpty());
          this.retry.setDisable(false);
        });
    loading.setOnFailed(
        e -> {
          if (this.disposed || version != this.generation) {
            return;
          }
          this.task = null;
          this.busy(false);
          this.status.textProperty().unbind();
          this.status.setText("Ни один файл не добавлен: " + failure(loading.getException()));
          this.retry.setDisable(false);
        });
    final Thread worker = new Thread(loading, "laboratory-attachment-import");
    worker.setDaemon(true);
    worker.start();
  }

  static String failure(final Throwable exception) {
    if (exception instanceof java.nio.file.NoSuchFileException missing) {
      return "Файл не найден: " + Path.of(missing.getFile()).getFileName();
    }
    if (exception instanceof java.nio.file.AccessDeniedException denied) {
      return "Нет доступа к файлу: " + Path.of(denied.getFile()).getFileName();
    }
    return exception.getMessage() == null ? "Не удалось прочитать файл." : exception.getMessage();
  }

  private void apply() {
    if (this.disposed || this.applied || this.prepared.isEmpty() || this.running) {
      return;
    }
    if (!this.experiment.equals(this.editor.document().experiment().id())) {
      this.status.setText("Открыт другой опыт. Вернитесь и добавьте файлы заново.");
      return;
    }
    this.prepared.forEach(this.editor::attach);
    this.applied = true;
    this.finish();
  }

  @Override
  boolean canClose() {
    return true;
  }

  @Override
  void dispose() {
    super.dispose();
    this.generation++;
    if (this.task != null) {
      this.task.cancel(true);
    }
    this.status.textProperty().unbind();
    this.busy(false);
  }
}

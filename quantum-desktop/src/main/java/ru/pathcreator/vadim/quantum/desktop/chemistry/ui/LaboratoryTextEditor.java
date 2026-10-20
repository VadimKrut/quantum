/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.Properties;
import java.util.concurrent.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEditor;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryFileCodec;

/** Recoverable whole-document input; parsing does not mutate the live notebook. */
final class LaboratoryTextEditor extends BorderPane {
  private final LaboratoryEditor editor;
  private final LaboratoryDrawing source;
  private final Runnable close;
  private final TextArea text = new TextArea();
  private final Label status = note("Подготавливаю QCHEM…");
  private final Button apply = new Button("Применить к опыту");
  private final FlowPane confirmation = new FlowPane(8, 8);
  private final ExecutorService worker =
      Executors.newSingleThreadExecutor(
          task -> {
            final Thread thread = new Thread(task, "laboratory-text-editor");
            thread.setDaemon(true);
            return thread;
          });
  private Future<?> task;
  private Properties recovered;
  private String initial;
  private String fingerprint;
  private boolean ready;
  private boolean busy;
  private boolean disposed;
  private boolean applied;
  private boolean conflict;

  LaboratoryTextEditor(final LaboratoryEditor editor, final Runnable close) {
    this.editor = editor;
    this.source = editor.document();
    this.close = close;
    this.setId("chemistry-text-page");
    this.setMinSize(0, 0);
    final Button back =
        button(
            "← К опыту",
            () -> {
              if (this.canClose()) {
                this.finish();
              }
            });
    back.setId("chemistry-text-back");
    final Label title = note("Ручное редактирование QCHEM");
    title.setStyle("-fx-font-size: 19px; -fx-font-weight: bold;");
    final VBox header =
        new VBox(
            8,
            new FlowPane(12, 8, back, title),
            note(
                "Изменения проверяются ядром. Исходный опыт меняется только после применения; незавершённый текст входит в авточерновик."));
    header.setPadding(new Insets(12, 16, 10, 16));
    this.setTop(header);
    this.text.setId("chemistry-text-input");
    this.text.setDisable(true);
    this.text.setWrapText(false);
    this.setCenter(this.text);
    BorderPane.setMargin(this.text, new Insets(0, 16, 0, 16));
    this.status.setId("chemistry-text-status");
    this.apply.setId("chemistry-text-apply");
    this.apply.getStyleClass().add("laboratory-save");
    this.apply.setDisable(true);
    this.apply.setOnAction(event -> this.commit());
    final Button cancel = button("Отменить ввод", this::askDiscard);
    cancel.setId("chemistry-text-cancel");
    final Button discard = button("Да, отменить ввод", this::finish);
    discard.setId("chemistry-text-discard");
    final Button keep =
        button(
            "Продолжить редактирование",
            () -> {
              this.confirmation.setVisible(false);
              this.confirmation.setManaged(false);
            });
    keep.setId("chemistry-text-keep");
    this.confirmation
        .getChildren()
        .setAll(note("Отменить незавершённые изменения?"), discard, keep);
    this.confirmation.setVisible(false);
    this.confirmation.setManaged(false);
    final VBox footer =
        new VBox(8, this.status, new FlowPane(8, 8, this.apply, cancel), this.confirmation);
    footer.setPadding(new Insets(10, 16, 12, 16));
    final ScrollPane bottom = new ScrollPane(footer);
    bottom.setFitToWidth(true);
    bottom.setMinHeight(0);
    bottom
        .prefViewportHeightProperty()
        .bind(
            javafx.beans.binding.Bindings.min(
                footer.heightProperty(), this.heightProperty().multiply(.32)));
    this.setBottom(bottom);
    this.setOnKeyPressed(
        event -> {
          if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
            back.fire();
            event.consume();
          } else if (event.isShortcutDown()
              && event.getCode() == javafx.scene.input.KeyCode.ENTER) {
            this.apply.fire();
            event.consume();
          }
        });
    this.task =
        this.worker.submit(
            () -> {
              try {
                final String value = new LaboratoryFileCodec().write(this.source);
                final String hash = hash(value);
                Platform.runLater(
                    () -> {
                      if (this.disposed) {
                        return;
                      }
                      this.fingerprint = hash;
                      this.ready = true;
                      this.text.setText(value);
                      this.initial = this.text.getText();
                      this.text.setDisable(false);
                      this.apply.setDisable(false);
                      this.status.setText(
                          "Введите изменения. Ctrl+Enter — проверить и применить одним действием.");
                      if (this.recovered != null) {
                        this.restorePrepared();
                      }
                    });
              } catch (final Exception failure) {
                Platform.runLater(
                    () -> {
                      if (!this.disposed) {
                        this.status.setText("Не удалось подготовить документ: " + message(failure));
                      }
                    });
              }
            });
  }

  private void commit() {
    if (!this.ready || this.busy || this.disposed || this.applied) {
      return;
    }
    if (this.conflict || !this.editor.document().equals(this.source)) {
      this.conflict = true;
      this.status.setText(
          "Исходный опыт изменился. Текст сохранён; скопируйте нужные изменения и откройте редактор заново.");
      this.apply.setDisable(true);
      return;
    }
    final String input = this.text.getText();
    this.busy = true;
    this.apply.setDisable(true);
    this.status.setText("Проверяю документ…");
    this.task =
        this.worker.submit(
            () -> {
              try {
                final LaboratoryDrawing revision = new LaboratoryFileCodec().read(input);
                Platform.runLater(
                    () -> {
                      if (this.disposed) {
                        return;
                      }
                      this.busy = false;
                      if (!input.equals(this.text.getText())) {
                        this.status.setText(
                            "Текст изменился во время проверки. Примените текущий вариант ещё раз.");
                        this.apply.setDisable(false);
                        return;
                      }
                      if (!this.editor.document().equals(this.source)) {
                        this.conflict = true;
                        this.status.setText(
                            "Исходный опыт изменился. Проверенный текст не применён.");
                        return;
                      }
                      try {
                        if (!revision.equals(this.source)) {
                          this.editor.revise(
                              revision, "Документ изменён через ручной редактор QCHEM.");
                        }
                        this.applied = true;
                        this.finish();
                      } catch (final RuntimeException failure) {
                        this.apply.setDisable(false);
                        this.status.setText(message(failure));
                      }
                    });
              } catch (final Exception failure) {
                Platform.runLater(
                    () -> {
                      if (!this.disposed) {
                        this.busy = false;
                        this.apply.setDisable(this.conflict);
                        this.status.setText(message(failure));
                      }
                    });
              }
            });
  }

  boolean pending() {
    return !this.applied
        && (this.recovered != null
            || this.conflict
            || this.ready && !this.text.getText().equals(this.initial));
  }

  boolean canClose() {
    if (this.busy) {
      this.status.setText("Дождитесь проверки или явно отмените ввод.");
      this.askDiscard();
      return false;
    }
    if (!this.pending()) {
      return true;
    }
    this.askDiscard();
    return false;
  }

  private void askDiscard() {
    this.confirmation.setVisible(true);
    this.confirmation.setManaged(true);
  }

  private void finish() {
    this.dispose();
    this.close.run();
  }

  void dispose() {
    this.disposed = true;
    if (this.task != null) {
      this.task.cancel(true);
    }
    this.worker.shutdownNow();
  }

  void draft(final Properties fields, final boolean restore) {
    if (restore) {
      final Properties saved = new Properties();
      fields.stringPropertyNames().stream()
          .filter(key -> key.startsWith("textForm."))
          .forEach(key -> saved.setProperty(key, fields.getProperty(key)));
      if (!saved.containsKey("textForm.content")
          || !saved.containsKey("textForm.source")
          || !saved.containsKey("textForm.experiment")
          || !java.util.Set.of("true", "false")
              .contains(saved.getProperty("textForm.conflict", ""))) {
        throw new IllegalArgumentException("Повреждён черновик ручного текста.");
      }
      this.recovered = saved;
      if (this.ready) {
        this.restorePrepared();
      }
      return;
    }
    if (this.recovered != null) {
      this.recovered.forEach((key, value) -> fields.put(key, value));
      return;
    }
    if (!this.ready) {
      return;
    }
    fields.setProperty("textForm.content", this.text.getText());
    fields.setProperty("textForm.source", this.fingerprint);
    fields.setProperty("textForm.experiment", this.source.experiment().id());
    fields.setProperty(
        "textForm.conflict",
        Boolean.toString(this.conflict || !this.editor.document().equals(this.source)));
  }

  private void restorePrepared() {
    final Properties saved = this.recovered;
    this.recovered = null;
    this.text.setText(saved.getProperty("textForm.content"));
    this.conflict =
        Boolean.parseBoolean(saved.getProperty("textForm.conflict"))
            || !this.source.experiment().id().equals(saved.getProperty("textForm.experiment"))
            || !this.fingerprint.equals(saved.getProperty("textForm.source"));
    this.apply.setDisable(this.conflict);
    this.status.setText(
        this.conflict
            ? "Текст восстановлен для просмотра: исходный опыт изменился. Применение недоступно."
            : "Восстановлен незавершённый текст. Проверьте и примените изменения.");
  }

  private static String hash(final String value) throws java.security.NoSuchAlgorithmException {
    return java.util.HexFormat.of()
        .formatHex(
            java.security.MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  }

  private static String message(final Exception failure) {
    return failure.getMessage() == null
        ? "Не удалось проверить документ. Проверьте текст QCHEM."
        : failure.getMessage();
  }

  private static Label note(final String text) {
    final Label label = new Label(text);
    label.setWrapText(true);
    label.setMinHeight(Region.USE_PREF_SIZE);
    return label;
  }

  private static Button button(final String text, final Runnable action) {
    final Button result = new Button(text);
    result.setOnAction(event -> action.run());
    return result;
  }
}

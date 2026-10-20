/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/** A single file operation with an active FX event loop and an inline view. */
final class LaboratoryFileOperation {
  private Consumer<Node> display = node -> {};
  private boolean busy;
  private boolean cancelled;
  private Thread worker;
  private Runnable cancel;
  private boolean decisionPending;
  private Runnable closeAfterDecision;

  void display(final Consumer<Node> display) {
    this.display = display;
  }

  boolean busy() {
    return this.busy;
  }

  void cancel() {
    if (this.busy && this.cancel != null) {
      this.cancel.run();
    }
  }

  /** Dismiss only a choice; file I/O, including cancellable reads, must finish normally. */
  boolean cancelDecisionForClose(final Runnable retryClose) {
    java.util.Objects.requireNonNull(retryClose);
    if (!this.busy || !this.decisionPending || this.cancel == null) {
      return false;
    }
    if (this.closeAfterDecision == null) {
      this.closeAfterDecision = retryClose;
      this.cancel.run();
    }
    return true;
  }

  boolean decisionClosePending() {
    return this.closeAfterDecision != null;
  }

  private void begin(final Node view) {
    if (this.busy) {
      throw new IllegalStateException("Файловое действие ещё выполняется.");
    }
    if (!Platform.canStartNestedEventLoop()) {
      throw new IllegalStateException(
          "Файловое действие недоступно во время обновления интерфейса. Повторите его.");
    }
    this.busy = true;
    try {
      this.display.accept(view);
    } catch (final RuntimeException failure) {
      this.busy = false;
      this.cancel = null;
      throw failure;
    }
  }

  private void end() {
    final Runnable retry = this.closeAfterDecision;
    this.worker = null;
    this.busy = false;
    this.cancel = null;
    this.decisionPending = false;
    try {
      this.display.accept(null);
    } finally {
      if (retry != null) {
        // The original caller must observe its cancelled choice before normal close guards run
        // again.
        Platform.runLater(
            () -> {
              if (this.closeAfterDecision == retry) {
                this.closeAfterDecision = null;
                retry.run();
              }
            });
      }
    }
  }

  private static VBox page(final String title, final String detail) {
    final Label heading = new Label(title);
    heading.setStyle("-fx-font-size: 19px; -fx-font-weight: bold;");
    heading.setWrapText(true);
    final Label description = new Label(detail);
    description.setWrapText(true);
    final VBox content = new VBox(14, heading, description);
    content.setPadding(new Insets(24));
    content.setId("chemistry-file-operation");
    return content;
  }

  <T> T run(
      final String title, final String detail, final boolean cancellable, final Callable<T> action)
      throws Exception {
    final Object loop = new Object();
    final Object[] result = new Object[1];
    final Throwable[] error = new Throwable[1];
    final VBox content = page(title, detail);
    final ProgressIndicator progress = new ProgressIndicator();
    progress.setMaxSize(30, 30);
    final Label state =
        new Label(
            cancellable
                ? "Читаю и проверяю файл…"
                : "Дождитесь подтверждения записи. Исходные данные остаются в памяти.");
    state.setWrapText(true);
    state.setId("chemistry-file-operation-state");
    content.getChildren().addAll(progress, state);
    final Button stop = new Button("Отменить открытие");
    stop.setId("chemistry-file-operation-cancel");
    stop.setOnAction(
        event -> {
          this.cancelled = true;
          stop.setDisable(true);
          state.setText("Останавливаю чтение…");
          if (this.worker != null) {
            this.worker.interrupt();
          }
        });
    if (cancellable) {
      content.getChildren().add(stop);
    }
    this.cancelled = false;
    this.cancel = cancellable ? stop::fire : null;
    this.begin(content);
    this.worker =
        new Thread(
            () -> {
              try {
                result[0] = action.call();
              } catch (final Throwable failure) {
                error[0] = failure;
              } finally {
                Platform.runLater(() -> Platform.exitNestedEventLoop(loop, null));
              }
            },
            "laboratory-document-file");
    this.worker.setDaemon(true);
    try {
      this.worker.start();
      Platform.enterNestedEventLoop(loop);
      if (this.cancelled) {
        throw new java.util.concurrent.CancellationException(
            "Открытие отменено. Текущий опыт сохранён в памяти.");
      }
      if (error[0] instanceof Exception exception) {
        throw exception;
      }
      if (error[0] != null) {
        throw new IllegalStateException("Файловое действие прервано.", error[0]);
      }
      @SuppressWarnings("unchecked")
      final T value = (T) result[0];
      return value;
    } finally {
      this.end();
    }
  }

  <T> T choose(final Node content, final Consumer<Consumer<T>> configure) {
    final Object loop = new Object();
    final boolean[] decided = {false};
    final Consumer<T> decision =
        value -> {
          if (!decided[0]) {
            decided[0] = true;
            content.setDisable(true);
            Platform.exitNestedEventLoop(loop, value);
          }
        };
    configure.accept(decision);
    this.cancel = () -> decision.accept(null);
    this.begin(content);
    this.decisionPending = true;
    try {
      @SuppressWarnings("unchecked")
      final T result = (T) Platform.enterNestedEventLoop(loop);
      return result;
    } finally {
      this.end();
    }
  }

  String confirm() {
    final Object loop = new Object();
    final VBox content =
        page(
            "Сохранить опыт перед продолжением?",
            "Есть несохранённые изменения. Можно сохранить их, продолжить без сохранения или вернуться к редактированию.");
    final Button save = new Button("Сохранить"),
        discard = new Button("Не сохранять"),
        cancel = new Button("Вернуться к опыту");
    save.setId("chemistry-file-confirm-save");
    discard.setId("chemistry-file-confirm-discard");
    cancel.setId("chemistry-file-confirm-cancel");
    final boolean[] decided = {false};
    final Consumer<String> decide =
        value -> {
          if (decided[0]) {
            return;
          }
          decided[0] = true;
          save.setDisable(true);
          discard.setDisable(true);
          cancel.setDisable(true);
          Platform.exitNestedEventLoop(loop, value);
        };
    save.setOnAction(event -> decide.accept("save"));
    discard.setOnAction(event -> decide.accept("discard"));
    cancel.setOnAction(event -> decide.accept("cancel"));
    content.getChildren().add(new FlowPane(10, 10, save, discard, cancel));
    this.cancel = cancel::fire;
    this.begin(content);
    this.decisionPending = true;
    try {
      return (String) Platform.enterNestedEventLoop(loop);
    } finally {
      this.end();
    }
  }
}

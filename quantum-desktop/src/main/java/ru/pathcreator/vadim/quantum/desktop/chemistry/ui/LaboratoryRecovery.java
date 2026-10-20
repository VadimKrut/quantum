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
import java.util.Properties;
import java.util.concurrent.*;
import java.util.function.*;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryRecoveryStore;

/** Durable recovery with inline decisions and background I/O; raw input is never parsed as a prerequisite. */
final class LaboratoryRecovery {
  record Snapshot(LaboratoryDrawing drawing, Path source, Properties fields, boolean dirty) {}

  private final LaboratoryFiles files;
  private final Supplier<Snapshot> capture;
  private final Consumer<LaboratoryRecoveryStore.Recovered> restore;
  private final Consumer<String> status;
  private final BooleanSupplier canReplace;
  private final ExecutorService worker =
      Executors.newSingleThreadExecutor(
          task -> {
            final Thread thread = new Thread(task, "laboratory-recovery");
            thread.setDaemon(true);
            return thread;
          });
  private final Timeline timer =
      new Timeline(new KeyFrame(Duration.seconds(5), event -> this.checkpoint()));
  private volatile LaboratoryRecoveryStore store;
  private int pendingDrafts;
  private IntConsumer draftsChanged = count -> {};
  private Snapshot previous;
  private boolean busy;
  private boolean started;
  private volatile boolean closed;
  private boolean finishing;

  LaboratoryRecovery(
      final LaboratoryFiles files,
      final Supplier<Snapshot> capture,
      final Consumer<LaboratoryRecoveryStore.Recovered> restore,
      final Consumer<String> status,
      final BooleanSupplier canReplace) {
    this.files = files;
    this.capture = capture;
    this.restore = restore;
    this.status = status;
    this.canReplace = canReplace;
    this.timer.setCycleCount(Timeline.INDEFINITE);
  }

  int pendingDrafts() {
    return this.pendingDrafts;
  }

  void onDraftsChanged(final IntConsumer listener) {
    this.draftsChanged = java.util.Objects.requireNonNull(listener);
    this.draftsChanged.accept(this.pendingDrafts);
  }

  private void draftsChanged(final int count) {
    this.pendingDrafts = count;
    this.draftsChanged.accept(count);
  }

  void start(final Runnable resume) {
    if (this.started || this.closed) {
      return;
    }
    this.started = true;
    this.busy = true;
    this.worker.execute(
        () -> {
          int count = 0;
          String warning = "";
          try {
            // A queued close still needs this session to preserve its captured raw input.
            this.store =
                new LaboratoryRecoveryStore(
                    Path.of(
                        System.getProperty(
                            "quantum.laboratory.recoveryDirectory",
                            Path.of(
                                    System.getProperty("user.home"),
                                    ".quantum",
                                    "laboratory-recovery")
                                .toString())));
            if (!this.closed) {
              count = this.store.available().size();
            }
          } catch (final Exception failure) {
            warning = "Авточерновик недоступен: " + failure.getMessage();
          }
          final int available = count;
          final String problem = warning;
          Platform.runLater(
              () -> {
                this.busy = false;
                if (this.closed || this.finishing) {
                  return;
                }
                try {
                  this.draftsChanged(available);
                  if (!problem.isBlank()) {
                    this.status.accept(problem);
                  }
                  resume.run();
                } finally {
                  if (!this.closed && !this.finishing) {
                    this.timer.play();
                  }
                }
              });
        });
  }

  void checkpoint() {
    if (this.store == null || this.busy || this.closed || this.finishing || this.files.busy()) {
      return;
    }
    final Snapshot snapshot;
    try {
      snapshot = this.capture.get();
    } catch (final RuntimeException failure) {
      this.status.accept("Ошибка подготовки авточерновика: " + failure.getMessage());
      return;
    }
    if (snapshot.equals(this.previous)) {
      return;
    }
    this.busy = true;
    this.worker.execute(
        () -> {
          try {
            if (snapshot.dirty()) {
              this.store.checkpoint(snapshot.drawing(), snapshot.source(), snapshot.fields());
            } else {
              this.store.clear();
            }
            Platform.runLater(
                () -> {
                  this.busy = false;
                  if (!this.closed) {
                    this.previous = snapshot;
                    this.status.accept(
                        snapshot.dirty()
                            ? "Авточерновик: "
                                + java.time.LocalTime.now()
                                    .format(
                                        java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
                            : "");
                  }
                });
          } catch (final Exception failure) {
            Platform.runLater(
                () -> {
                  this.busy = false;
                  if (!this.closed) {
                    this.status.accept("Ошибка авточерновика: " + failure.getMessage());
                  }
                });
          }
        });
  }

  void browse() {
    if (this.store == null || this.busy || this.closed || this.finishing || this.files.busy()) {
      return;
    }
    try {
      final var drafts =
          this.files
              .operation()
              .run(
                  "Незавершённые сеансы",
                  "Читаю доступные авточерновики…",
                  true,
                  this.store::available);
      if (this.closed) {
        return;
      }
      this.draftsChanged(drafts.size());
      if (drafts.isEmpty()) {
        this.status.accept("Нет черновиков после сбоя");
        return;
      }
      final var choice = this.choose(drafts);
      if (choice == null || this.closed || !this.canReplace.getAsBoolean()) {
        return;
      }
      final Snapshot before = this.capture.get();
      final var recovered =
          this.files
              .operation()
              .run(
                  "Восстановить опыт",
                  "Читаю документ, проверяю вложения и сохраняю восстановленный черновик…",
                  false,
                  () -> this.store.recover(choice));
      if (this.closed) {
        return;
      }
      if (!before.equals(this.capture.get())) {
        this.previous = null;
        this.checkpoint();
        throw new IllegalStateException(
            "Текущий опыт изменился во время восстановления. Исходный черновик сохранён; повторите открытие.");
      }
      this.restore.accept(recovered);
      try {
        final int remaining =
            this.files
                .operation()
                .run(
                    "Подтвердить восстановление",
                    "Восстановленные данные уже сохранены в текущем сеансе…",
                    false,
                    () -> {
                      this.store.acknowledge(choice);
                      return this.store.available().size();
                    });
        this.draftsChanged(remaining);
      } catch (final Exception failure) {
        this.status.accept(
            "Опыт восстановлен. Исходный черновик оставлен: " + failure.getMessage());
      }
      this.previous = null;
      this.checkpoint();
    } catch (final CancellationException cancelled) {
      this.status.accept("Чтение авточерновиков отменено");
    } catch (final Exception failure) {
      if (!this.closed) {
        this.files.error(failure);
      }
    }
  }

  private LaboratoryRecoveryStore.Draft choose(
      final java.util.List<LaboratoryRecoveryStore.Draft> drafts) {
    final BorderPane page = new BorderPane();
    page.setId("chemistry-recovery-page");
    page.setPadding(new Insets(16));
    page.setMaxWidth(800);
    page.setMaxHeight(Region.USE_PREF_SIZE);
    final Label title = note("Восстановить незавершённую работу");
    title.setStyle("-fx-font-size: 19px; -fx-font-weight: bold;");
    final Label hint =
        note("Черновик откроется как несохранённый опыт. Исходный файл не будет перезаписан.");
    final VBox header = new VBox(8, title, hint);
    header.setPadding(new Insets(0, 0, 12, 0));
    page.setTop(header);
    final ListView<LaboratoryRecoveryStore.Draft> choices = new ListView<>();
    choices.setId("chemistry-recovery-choices");
    choices.getItems().setAll(drafts);
    choices.setMinSize(0, 80);
    choices.setPrefHeight(Math.min(360, Math.max(110, drafts.size() * 64)));
    choices.setMaxHeight(360);
    choices.setCellFactory(
        list ->
            new ListCell<>() {
              {
                this.prefWidthProperty().bind(list.widthProperty().subtract(26));
                this.setMaxWidth(Region.USE_PREF_SIZE);
                this.setWrapText(true);
              }

              @Override
              protected void updateItem(
                  final LaboratoryRecoveryStore.Draft draft, final boolean empty) {
                super.updateItem(draft, empty);
                if (empty || draft == null) {
                  this.setText(null);
                  this.setTooltip(null);
                  return;
                }
                final String date =
                    java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                        .withZone(java.time.ZoneId.systemDefault())
                        .format(draft.savedAt());
                this.setText(draft.title() + "\n" + date);
                this.setTooltip(
                    new Tooltip(
                        draft.original().isBlank()
                            ? draft.archive().toString()
                            : draft.original()));
              }
            });
    choices.getSelectionModel().selectFirst();
    page.setCenter(choices);
    final Button apply = new Button("Восстановить"), later = new Button("Позже");
    apply.setId("chemistry-recovery-apply");
    later.setId("chemistry-recovery-later");
    apply.getStyleClass().add("laboratory-save");
    apply.disableProperty().bind(choices.getSelectionModel().selectedItemProperty().isNull());
    final FlowPane footer = new FlowPane(10, 8, apply, later);
    footer.setPadding(new Insets(12, 0, 0, 0));
    page.setBottom(footer);
    choices.setOnKeyPressed(
        event -> {
          if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
            apply.fire();
            event.consume();
          }
        });
    final StackPane centered = new StackPane(page);
    centered.setPadding(new Insets(20));
    return this.files
        .operation()
        .choose(
            centered,
            decision -> {
              apply.setOnAction(
                  event -> decision.accept(choices.getSelectionModel().getSelectedItem()));
              later.setOnAction(event -> decision.accept(null));
            });
  }

  boolean finishClose() {
    if (this.closed || this.store == null) {
      return true;
    }
    if (this.files.busy()) {
      return false;
    }
    this.finishing = true;
    this.timer.stop();
    try {
      this.files
          .operation()
          .run(
              "Закрыть сеанс",
              "Завершаю предыдущую запись и освобождаю авточерновик…",
              false,
              () ->
                  this.worker
                      .submit(
                          () -> {
                            this.store.discard();
                            this.store.close();
                            return null;
                          })
                      .get());
      this.closed = true;
      this.worker.shutdown();
      return true;
    } catch (final Exception failure) {
      this.files.error(
          new java.io.IOException(
              "Не удалось завершить сеанс восстановления. Повторите закрытие.", failure));
      this.timer.play();
      return false;
    } finally {
      this.finishing = false;
    }
  }

  void closed() {
    if (this.closed) {
      return;
    }
    Snapshot captured = null;
    try {
      captured = this.capture.get();
    } catch (final RuntimeException ignored) {
      /* Keep the latest durable draft if capture fails. */
    }
    final Snapshot snapshot = captured;
    this.closed = true;
    this.timer.stop();
    this.worker.execute(
        () -> {
          try {
            if (this.store != null) {
              try {
                if (snapshot != null) {
                  if (snapshot.dirty()) {
                    this.store.checkpoint(snapshot.drawing(), snapshot.source(), snapshot.fields());
                  } else {
                    this.store.clear();
                  }
                }
              } finally {
                this.store.close();
              }
            }
          } catch (final Exception ignored) {
            /* An unapproved hide must keep recoverable work. */
          }
        });
    this.worker.shutdown();
  }

  private static Label note(final String text) {
    final Label label = new Label(text);
    label.setWrapText(true);
    label.setMinHeight(Region.USE_PREF_SIZE);
    return label;
  }
}

/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryExperimentIndex;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryExperimentIndex.Entry;

/** Only the newest bounded background scan can update the visible folder. */
final class LaboratoryArchiveBrowser extends VBox {
  private final TextField query = new TextField(), name = new TextField();
  private final Label status = new Label("Выберите папку опытов"),
      location = new Label(),
      creationError = new Label();
  private final ListView<Entry> documents = new ListView<>();
  private final VBox creation = new VBox(6);
  private final Button up = new Button("↑"),
      create = new Button("+ Папка"),
      refresh = new Button("Обновить");
  private final ExecutorService worker =
      Executors.newSingleThreadExecutor(
          task -> {
            final Thread thread = new Thread(task, "laboratory-archive-index");
            thread.setDaemon(true);
            return thread;
          });
  private final Consumer<Path> open, changedFolder;
  private List<Entry> index = List.of();
  private List<String> diagnostics = List.of();
  private Path folder;
  private Future<?> scan;
  private int generation;
  private boolean scanning;
  private String scanError = "", scanDetail = "";
  private final Button stop = new Button("Остановить"),
      activate = new Button("Открыть"),
      confirm = new Button("Создать"),
      cancelCreation = new Button("Отмена");
  private final ExecutorService filtering =
      Executors.newSingleThreadExecutor(
          task -> {
            final Thread thread = new Thread(task, "laboratory-archive-search");
            thread.setDaemon(true);
            return thread;
          });
  private final javafx.animation.PauseTransition debounce =
      new javafx.animation.PauseTransition(javafx.util.Duration.millis(120));
  private Future<?> search;
  private int filterGeneration, navigation;
  private boolean filteringNow, creating, disposed;
  private long total, errors;

  LaboratoryArchiveBrowser(final Consumer<Path> open, final Consumer<Exception> error) {
    this(open, error, path -> {});
  }

  LaboratoryArchiveBrowser(
      final Consumer<Path> open,
      final Consumer<Exception> error,
      final Consumer<Path> changedFolder) {
    super(8);
    this.open = open;
    this.changedFolder = changedFolder;
    this.setId("chemistry-archive-browser");
    this.location.setId("chemistry-archive-location");
    this.location.setWrapText(true);
    this.location.setMinHeight(Region.USE_PREF_SIZE);
    this.query.setId("chemistry-archive-query");
    this.query.setPromptText("Название, вещество, записи…");
    this.query.textProperty().addListener((observable, before, after) -> this.filter());
    this.query.setTooltip(
        new Tooltip("Поиск по папке и вложенным опытам · Ctrl+F; Escape очищает поиск"));
    this.documents.setId("chemistry-archive-documents");
    final Label empty = new Label("Нет подходящих опытов или папок");
    empty.setWrapText(true);
    empty.setMaxWidth(180);
    empty.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
    this.documents.setPlaceholder(empty);
    this.documents.setCellFactory(
        list ->
            new ListCell<>() {
              {
                this.prefWidthProperty().bind(list.widthProperty().subtract(24));
                this.setMaxWidth(Region.USE_PREF_SIZE);
              }

              @Override
              protected void updateItem(final Entry entry, final boolean empty) {
                super.updateItem(entry, empty);
                this.setText(
                    empty || entry == null
                        ? null
                        : (entry.directory() ? "Папка · " : "")
                            + entry.title()
                            + (entry.directory() ? "" : "\n" + folder.relativize(entry.path())));
                this.setTooltip(
                    empty || entry == null
                        ? null
                        : new Tooltip(
                            entry.problem() == null ? entry.path().toString() : entry.problem()));
                this.setWrapText(true);
              }
            });
    this.documents.setOnMouseClicked(
        event -> {
          if (event.getButton() != MouseButton.PRIMARY || event.getClickCount() != 2) {
            return;
          }
          javafx.scene.Node node = event.getPickResult().getIntersectedNode();
          while (node != null && !(node instanceof ListCell<?>)) {
            node = node.getParent();
          }
          if (node instanceof ListCell<?> cell
              && !cell.isEmpty()
              && cell.getItem() instanceof Entry entry) {
            this.documents.getSelectionModel().select(entry);
            this.activate();
            event.consume();
          }
        });
    this.documents.setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.ENTER) {
            this.activate();
            event.consume();
          }
        });
    this.refresh.setId("chemistry-archive-refresh");
    this.refresh.setOnAction(event -> this.refresh());
    this.create.setId("chemistry-archive-new-folder");
    this.create.setOnAction(event -> this.creation(true));
    this.up.setId("chemistry-archive-up");
    this.up.setTooltip(new Tooltip("На папку выше"));
    this.up.setAccessibleText("На папку выше");
    this.up.setOnAction(
        event -> {
          if (this.folder != null && this.folder.getParent() != null) {
            this.query.clear();
            this.folder(this.folder.getParent());
          }
        });
    this.up.setDisable(true);
    this.create.setDisable(true);
    this.refresh.setDisable(true);
    this.name.setId("chemistry-archive-folder-name");
    this.name.setPromptText("Название новой папки");
    this.confirm.setId("chemistry-archive-folder-create");
    this.confirm.setOnAction(event -> this.createFolder());
    this.cancelCreation.setId("chemistry-archive-folder-cancel");
    this.cancelCreation.setOnAction(event -> this.creation(false));
    this.name.setOnAction(event -> this.createFolder());
    this.creationError.setId("chemistry-archive-folder-error");
    this.creationError.setWrapText(true);
    this.creationError.setMinHeight(Region.USE_PREF_SIZE);
    this.creationError.setStyle("-fx-text-fill: -app-danger-text;");
    this.creation
        .getChildren()
        .setAll(
            this.name, new FlowPane(6, 6, this.confirm, this.cancelCreation), this.creationError);
    this.creation.setMinHeight(Region.USE_PREF_SIZE);
    this.creation(false);
    this.status.setId("chemistry-archive-status");
    this.status.setWrapText(true);
    this.status.setMinHeight(Region.USE_PREF_SIZE);
    final Button clear = new Button("×");
    clear.setId("chemistry-archive-clear");
    clear.setAccessibleText("Очистить поиск");
    clear.setTooltip(new Tooltip("Очистить поиск · Escape"));
    clear.disableProperty().bind(this.query.textProperty().isEmpty());
    clear.setOnAction(event -> this.query.clear());
    final HBox searchRow = new HBox(6, this.query, clear);
    HBox.setHgrow(this.query, Priority.ALWAYS);
    this.query.setMinWidth(0);
    this.stop.setId("chemistry-archive-stop");
    this.stop.setOnAction(event -> this.stopScan());
    this.stop.setVisible(false);
    this.stop.setManaged(false);
    this.activate.setId("chemistry-archive-open");
    this.activate.getStyleClass().add("laboratory-save");
    this.activate.setDisable(true);
    this.activate.setOnAction(event -> this.activate());
    this.documents
        .getSelectionModel()
        .selectedItemProperty()
        .addListener((observable, before, after) -> this.actions());
    final VBox header =
        new VBox(
            8,
            this.location,
            searchRow,
            new FlowPane(6, 6, this.up, this.refresh, this.create),
            this.creation);
    final ScrollPane top = new ScrollPane(header);
    top.setId("chemistry-archive-header");
    top.setFitToWidth(true);
    top.setMinHeight(Region.USE_PREF_SIZE);
    top.setMaxHeight(Region.USE_PREF_SIZE);
    top.prefViewportHeightProperty()
        .bind(
            javafx.beans.binding.Bindings.createDoubleBinding(
                () ->
                    Math.min(
                        header.prefHeight(Math.max(80, this.getWidth() - 24)),
                        this.getHeight() * .55),
                this.widthProperty(),
                this.heightProperty(),
                this.creation.managedProperty(),
                this.creationError.textProperty()));
    this.creation
        .managedProperty()
        .addListener(
            (observable, before, after) ->
                Platform.runLater(
                    () -> {
                      if (!this.disposed) {
                        top.setVvalue(after ? 1 : 0);
                      }
                    }));
    this.creationError
        .textProperty()
        .addListener(
            (observable, before, after) ->
                Platform.runLater(
                    () -> {
                      if (!this.disposed && !after.isBlank()) {
                        top.setVvalue(1);
                      }
                    }));
    final VBox footer = new VBox(8, new FlowPane(6, 6, this.activate, this.stop), this.status);
    final ScrollPane bottom = new ScrollPane(footer);
    bottom.setFitToWidth(true);
    bottom.setMinHeight(Region.USE_PREF_SIZE);
    bottom.setMaxHeight(Region.USE_PREF_SIZE);
    bottom
        .prefViewportHeightProperty()
        .bind(
            javafx.beans.binding.Bindings.createDoubleBinding(
                () ->
                    Math.min(
                        footer.prefHeight(Math.max(80, this.getWidth() - 24)),
                        this.getHeight() * .25),
                this.widthProperty(),
                this.heightProperty(),
                this.status.textProperty(),
                this.stop.managedProperty()));
    this.documents.setMinHeight(70);
    this.documents.setPrefHeight(70);
    this.setMinSize(0, 0);
    this.getChildren().setAll(top, this.documents, bottom);
    this.setOnKeyPressed(this::shortcut);
    VBox.setVgrow(this.documents, Priority.ALWAYS);
  }

  void folder(final Path path) {
    if (this.disposed) {
      return;
    }
    final Path next = path.toAbsolutePath().normalize();
    if (!next.equals(this.folder)) {
      this.navigation++;
      this.index = List.of();
      this.total = 0;
      this.errors = 0;
      this.documents.getItems().clear();
      this.diagnostics = List.of();
      this.creation(false);
      this.folder = next;
      this.changedFolder.accept(next);
    }
    this.location.setText("Папка · " + (next.getFileName() == null ? next : next.getFileName()));
    this.location.setTooltip(new Tooltip(next.toString()));
    this.location.setAccessibleText(next.toString());
    this.actions();
    this.refresh();
  }

  private void activate() {
    final Entry entry = this.documents.getSelectionModel().getSelectedItem();
    if (entry == null || this.disposed || this.creating || this.filteringNow) {
      return;
    }
    if (entry.directory()) {
      this.query.clear();
      this.folder(entry.path());
    } else {
      this.open.accept(entry.path());
    }
  }

  private void refresh() {
    if (this.folder == null || this.disposed || this.creating) {
      return;
    }
    if (this.scan != null) {
      this.scan.cancel(true);
    }
    final int requested = ++this.generation;
    final Path root = this.folder;
    this.scanning = true;
    this.scanError = "";
    this.scanDetail = "";
    this.filter();
    this.scan =
        this.worker.submit(
            () -> {
              try {
                final var snapshot = new LaboratoryExperimentIndex().scan(root);
                Platform.runLater(
                    () -> {
                      if (!this.disposed && requested == this.generation) {
                        this.index = snapshot.entries();
                        this.total =
                            this.index.stream()
                                .filter(entry -> !entry.directory() && entry.problem() == null)
                                .count();
                        this.errors =
                            this.index.stream().filter(entry -> entry.problem() != null).count();
                        this.diagnostics = snapshot.diagnostics();
                        this.scanning = false;
                        this.filter();
                      }
                    });
              } catch (final CancellationException ignored) {
                /* A newer generation owns the visible state. */
              } catch (final Exception exception) {
                Platform.runLater(
                    () -> {
                      if (!this.disposed && requested == this.generation) {
                        this.scanning = false;
                        this.scanError = "Архив недоступен. Проверьте папку и доступ к ней.";
                        this.scanDetail =
                            exception.getMessage() == null
                                ? exception.toString()
                                : exception.getMessage();
                        this.filter();
                      }
                    });
              }
            });
  }

  private void filter() {
    if (this.disposed) {
      return;
    }
    this.debounce.stop();
    if (this.search != null) {
      this.search.cancel(true);
    }
    final int requested = ++this.filterGeneration;
    final Path selected =
        this.documents.getSelectionModel().getSelectedItem() == null
            ? null
            : this.documents.getSelectionModel().getSelectedItem().path();
    final String[] terms = LaboratoryExperimentIndex.normalize(this.query.getText()).split(" ");
    final List<Entry> source = this.index;
    if (source.size() <= 200 || this.query.getText().isBlank()) {
      this.filtered(matches(source, terms), selected);
      return;
    }
    this.filteringNow = true;
    this.actions();
    this.status();
    this.debounce.setOnFinished(
        event -> {
          if (this.disposed) {
            return;
          }
          this.search =
              this.filtering.submit(
                  () -> {
                    try {
                      final List<Entry> found = matches(source, terms);
                      Platform.runLater(
                          () -> {
                            if (!this.disposed && requested == this.filterGeneration) {
                              this.filtered(found, selected);
                            }
                          });
                    } catch (final CancellationException ignored) {
                      /* The newest query owns the result. */
                    }
                  });
        });
    this.debounce.playFromStart();
  }

  private static List<Entry> matches(final List<Entry> source, final String[] terms) {
    final java.util.ArrayList<Entry> found = new java.util.ArrayList<>();
    for (final Entry entry : source) {
      if (Thread.currentThread().isInterrupted()) {
        throw new CancellationException();
      }
      if (java.util.Arrays.stream(terms).allMatch(entry.search()::contains)) {
        found.add(entry);
      }
    }
    return List.copyOf(found);
  }

  private void filtered(final List<Entry> found, final Path selected) {
    this.filteringNow = false;
    this.documents.getItems().setAll(found);
    found.stream()
        .filter(entry -> entry.path().equals(selected))
        .findFirst()
        .ifPresent(this.documents.getSelectionModel()::select);
    this.actions();
    this.status();
  }

  private void status() {
    final long found =
        this.documents.getItems().stream()
            .filter(entry -> !entry.directory() && entry.problem() == null)
            .count();
    final String count =
        "Опыты: "
            + found
            + " из "
            + this.total
            + " · Папки: "
            + this.documents.getItems().stream().filter(Entry::directory).count()
            + (this.errors == 0 ? "" : " · Ошибки: " + this.errors);
    final String previous = this.index.isEmpty() ? "" : "Показан предыдущий результат.\n";
    final String condition =
        this.scanning
            ? "Читаю архив…\n" + previous
            : !this.scanError.isEmpty() ? this.scanError + "\n" + previous : "";
    this.status.setText(
        (this.creating ? "Создаю папку…\n" : "")
            + (this.filteringNow ? "Ищу… Показан предыдущий результат.\n" : "")
            + condition
            + count
            + (this.diagnostics.isEmpty()
                ? ""
                : "\nИндекс неполный: " + String.join(" ", this.diagnostics)));
    this.status.setTooltip(
        new Tooltip(
            this.status.getText() + (this.scanDetail.isBlank() ? "" : "\n" + this.scanDetail)));
    this.stop.setVisible(this.scanning);
    this.stop.setManaged(this.scanning);
  }

  private void actions() {
    this.activate.setDisable(
        this.disposed
            || this.creating
            || this.filteringNow
            || this.documents.getSelectionModel().getSelectedItem() == null);
    this.up.setDisable(this.creating || this.folder == null || this.folder.getParent() == null);
    this.create.setDisable(this.creating || this.folder == null);
    this.refresh.setDisable(this.creating || this.folder == null);
    this.name.setDisable(this.creating);
    this.confirm.setDisable(this.creating);
    this.cancelCreation.setDisable(this.creating);
  }

  private void stopScan() {
    if (!this.scanning || this.disposed) {
      return;
    }
    this.generation++;
    if (this.scan != null) {
      this.scan.cancel(true);
    }
    this.scanning = false;
    this.scanError = "Чтение остановлено";
    this.status();
  }

  void focusSearch() {
    if (!this.disposed) {
      this.query.requestFocus();
      this.query.selectAll();
    }
  }

  boolean owns(final Object target) {
    if (!(target instanceof javafx.scene.Node node)) {
      return false;
    }
    while (node != null) {
      if (node == this) {
        return true;
      }
      node = node.getParent();
    }
    return false;
  }

  void shortcut(final javafx.scene.input.KeyEvent event) {
    if (event.isConsumed() || this.disposed) {
      return;
    }
    if (event.isAltDown() && event.getCode() == KeyCode.UP) {
      this.up.fire();
      event.consume();
    } else if (event.getCode() == KeyCode.ESCAPE) {
      if (!this.creating) {
        if (this.creation.isVisible()) {
          this.creation(false);
        } else if (!this.query.getText().isEmpty()) {
          this.query.clear();
        } else {
          this.stopScan();
        }
      }
      event.consume();
    }
  }

  boolean canClose() {
    if (this.creating) {
      this.creationError.setText("Дождитесь завершения создания папки.");
      return false;
    }
    return true;
  }

  void dispose() {
    if (this.disposed) {
      return;
    }
    this.disposed = true;
    this.generation++;
    this.filterGeneration++;
    this.debounce.stop();
    if (this.scan != null) {
      this.scan.cancel(true);
    }
    if (this.search != null) {
      this.search.cancel(true);
    }
    this.filtering.shutdownNow();
    this.worker.shutdown();
  }

  private void creation(final boolean visible) {
    if (this.creating || this.disposed) {
      return;
    }
    this.creation.setVisible(visible);
    this.creation.setManaged(visible);
    this.creationError.setText("");
    if (visible) {
      this.name.clear();
      this.name.requestFocus();
    }
  }

  private void createFolder() {
    if (this.folder == null || this.creating || this.disposed) {
      return;
    }
    final Path source = this.folder;
    final int requested = this.navigation;
    final Path target;
    try {
      final String value = this.name.getText().strip();
      if (value.isEmpty()
          || value.equals(".")
          || value.equals("..")
          || value.contains("/")
          || value.contains("\\")) {
        throw new IllegalArgumentException("Укажите одно имя папки без разделителей пути.");
      }
      target = source.resolve(value).normalize();
      if (!source.equals(target.getParent())) {
        throw new IllegalArgumentException("Папка должна находиться внутри выбранной папки.");
      }
    } catch (final Exception exception) {
      this.creationError.setText(exception.getMessage());
      return;
    }
    this.stopScan();
    this.creating = true;
    this.creationError.setText("");
    this.actions();
    this.status();
    this.worker.submit(
        () -> {
          try {
            Files.createDirectory(target);
            Platform.runLater(
                () -> {
                  if (this.disposed) {
                    return;
                  }
                  this.creating = false;
                  this.actions();
                  if (requested == this.navigation) {
                    this.creation(false);
                    this.query.clear();
                    this.folder(target);
                  } else {
                    this.creation(false);
                    this.refresh();
                  }
                });
          } catch (final Exception exception) {
            Platform.runLater(
                () -> {
                  if (this.disposed) {
                    return;
                  }
                  this.creating = false;
                  this.actions();
                  this.status();
                  if (requested == this.navigation) {
                    this.creationError.setText(
                        exception instanceof java.nio.file.FileAlreadyExistsException
                            ? "Это имя уже занято. Укажите другое название папки."
                            : exception instanceof java.nio.file.NoSuchFileException
                                ? "Исходная папка больше недоступна. Выберите папку заново."
                                : exception instanceof java.nio.file.AccessDeniedException
                                    ? "Нет доступа для создания папки. Выберите другое место."
                                    : "Не удалось создать папку: "
                                        + (exception.getMessage() == null
                                            ? exception.getClass().getSimpleName()
                                            : exception.getMessage()));
                  }
                });
          }
        });
  }
}

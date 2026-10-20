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
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryProjectArchive;

/** Project transfer page; leaving waits for cancellation cleanup. */
final class LaboratoryProjectWindow {
  private final LaboratoryProjectArchive archive = new LaboratoryProjectArchive();
  private final BorderPane page = new BorderPane();
  private final Window owner;
  private final TextField destinationName = new TextField();
  private final ListView<LaboratoryProjectArchive.Item> entries = new ListView<>();
  private final Label summary = new Label();
  private final Label status = new Label();
  private final TextField search = new TextField();
  private final Button refresh = new Button("Проверить заново");
  private final Button transfer = new Button();
  private final Button stop = new Button("Остановить");
  private final ProgressIndicator progress = new ProgressIndicator();
  private final Path source;
  private final boolean importing;
  private final Consumer<Path> restored;
  private LaboratoryProjectArchive.Snapshot snapshot;
  private Thread worker;
  private boolean disposed;
  private boolean writing;
  private boolean cancelled;
  private int generation;
  private final Label found = new Label();
  private final CheckBox allFiles = new CheckBox("Все файлы, включая служебные");

  private LaboratoryProjectWindow(
      final Window owner,
      final Path source,
      final boolean importing,
      final Consumer<Path> restored,
      final Runnable close) {
    this.owner = owner;
    this.source = source.toAbsolutePath().normalize();
    this.importing = importing;
    this.restored = restored;
    final Button back = new Button("← К опыту");
    back.setId("chemistry-project-back");
    back.setOnAction(
        event -> {
          if (this.canClose()) {
            close.run();
          }
        });
    final Label title =
        new Label(importing ? "Восстановить проект из пакета" : "Пакет папки проекта");
    title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
    final TextField path = new TextField(this.source.toString());
    path.setEditable(false);
    path.setId("chemistry-project-source");
    final Label hint =
        new Label(
            importing
                ? "Будет создана новая папка с опытами, материалами, библиотекой, историей и всеми файлами пакета. Существующие проекты не объединяются. После переноса откройте нужный опыт в «Папках»."
                : "Пакет включает все файлы и подпапки выбранной папки: опыты, склад, библиотеку, историю, вложения и исходные измерения. Проверьте состав. Во время упаковки не меняйте файлы проекта в других программах.");
    hint.setWrapText(true);
    hint.setMinHeight(Region.USE_PREF_SIZE);
    this.search.setPromptText("Найти файл в составе пакета…");
    this.search.setId("chemistry-project-search");
    this.search.textProperty().addListener((o, a, b) -> this.filter());
    this.entries.setId("chemistry-project-entries");
    this.entries.setPrefHeight(290);
    this.entries.setCellFactory(
        list ->
            new ListCell<>() {
              {
                this.prefWidthProperty().bind(list.widthProperty().subtract(28));
                this.setMaxWidth(Region.USE_PREF_SIZE);
              }

              protected void updateItem(
                  final LaboratoryProjectArchive.Item item, final boolean empty) {
                super.updateItem(item, empty);
                this.setText(
                    empty || item == null
                        ? null
                        : (item.directory() ? "Папка · " : size(item.size()) + " · ")
                            + item.path());
                this.setWrapText(true);
                this.setTooltip(empty || item == null ? null : new Tooltip(item.path()));
              }
            });
    this.summary.setId("chemistry-project-summary");
    this.summary.setWrapText(true);
    this.summary.setMinHeight(Region.USE_PREF_SIZE);
    this.status.setId("chemistry-project-status");
    this.status.setWrapText(true);
    this.status.setMinHeight(Region.USE_PREF_SIZE);
    this.progress.setPrefSize(22, 22);
    this.progress.setMaxSize(22, 22);
    this.progress.setVisible(false);
    this.transfer.setText(importing ? "Восстановить в новую папку…" : "Создать пакет…");
    this.transfer.setId("chemistry-project-transfer");
    this.transfer.getStyleClass().add("laboratory-save");
    this.transfer.setDisable(true);
    this.stop.setDisable(true);
    this.transfer.setOnAction(event -> this.chooseDestination());
    this.refresh.setId("chemistry-project-refresh");
    this.stop.setId("chemistry-project-stop");
    this.refresh.setOnAction(event -> this.load());
    this.stop.setOnAction(
        event -> {
          if (this.worker != null) {
            this.cancelled = true;
            this.worker.interrupt();
            this.stop.setDisable(true);
            this.status.setText("Останавливаю и удаляю временные файлы…");
          }
        });
    this.destinationName.setId("chemistry-project-folder-name");
    this.destinationName.setPromptText("Название новой папки проекта");
    final VBox destination = new VBox(4, new Label("Новая папка проекта"), this.destinationName);
    destination.setVisible(importing);
    destination.setManaged(importing);
    final VBox metadata = new VBox(8, path, hint);
    metadata.setPadding(new Insets(8));
    final TitledPane details = new TitledPane("Состав и расположение проекта", metadata);
    details.setExpanded(false);
    details.setAnimated(false);
    details.setId("chemistry-project-details");
    final VBox header =
        new VBox(9, new FlowPane(12, 6, back, title), this.summary, details, destination);
    header.setPadding(new Insets(12, 12, 8, 12));
    final ScrollPane heading = new ScrollPane(header);
    heading.setFitToWidth(true);
    heading.setMinHeight(0);
    heading.setMaxHeight(Double.MAX_VALUE);
    heading
        .prefViewportHeightProperty()
        .bind(
            javafx.beans.binding.Bindings.min(
                header.heightProperty(), this.page.heightProperty().multiply(0.30)));
    this.found.setId("chemistry-project-found");
    this.found.setWrapText(true);
    this.found.setMinHeight(Region.USE_PREF_SIZE);
    this.allFiles.setId("chemistry-project-all-files");
    this.allFiles.setOnAction(event -> this.filter());
    final VBox content = new VBox(8, this.search, this.allFiles, this.found, this.entries);
    content.setPadding(new Insets(0, 12, 0, 12));
    VBox.setVgrow(this.entries, Priority.ALWAYS);
    this.entries.setMinHeight(60);
    final VBox footer =
        new VBox(
            8,
            new FlowPane(8, 6, this.refresh, this.transfer, this.stop, this.progress),
            this.status);
    footer.setPadding(new Insets(10, 12, 12, 12));
    final ScrollPane actions = new ScrollPane(footer);
    actions.setFitToWidth(true);
    actions.setMinHeight(0);
    actions.setMaxHeight(Double.MAX_VALUE);
    actions
        .prefViewportHeightProperty()
        .bind(
            javafx.beans.binding.Bindings.min(
                footer.heightProperty(), this.page.heightProperty().multiply(0.25)));
    this.page.setTop(heading);
    this.page.setCenter(content);
    this.page.setBottom(actions);
    this.page.setId("chemistry-project-page");
    this.page.setOnKeyPressed(
        event -> {
          if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
            if (this.worker != null) {
              this.stop.fire();
            } else {
              back.fire();
            }
            event.consume();
          }
        });
    this.stop.setTooltip(new Tooltip("Остановить действие · Escape"));
    Platform.runLater(this::load);
  }

  BorderPane view() {
    return this.page;
  }

  void dispose() {
    if (this.disposed) {
      return;
    }
    this.disposed = true;
    this.generation++;
    if (this.worker != null) {
      this.worker.interrupt();
    }
  }

  boolean canClose() {
    if (this.worker == null) {
      return true;
    }
    this.status.setText(
        this.writing
            ? "Идёт перенос. Дождитесь завершения либо нажмите «Остановить»."
            : "Идёт проверка состава. Нажмите «Остановить», чтобы вернуться к опыту.");
    return false;
  }

  static LaboratoryProjectWindow exporting(
      final Window owner, final Path folder, final Runnable close) {
    return new LaboratoryProjectWindow(owner, folder, false, path -> {}, close);
  }

  static LaboratoryProjectWindow importing(
      final Window owner, final Consumer<Path> restored, final Runnable close) {
    final FileChooser chooser = chooser();
    chooser.setTitle("Открыть пакет проекта");
    final var selected = chooser.showOpenDialog(owner);
    return selected == null
        ? null
        : new LaboratoryProjectWindow(owner, selected.toPath(), true, restored, close);
  }

  private void load() {
    if (this.disposed || this.worker != null) {
      return;
    }
    this.writing = false;
    this.snapshot = null;
    this.entries.getItems().clear();
    this.summary.setText("");
    this.found.setText("Читаю состав…");
    this.work(
        () ->
            this.importing
                ? this.archive.inspect(this.source)
                : this.archive.scan(this.source, this::report),
        value -> {
          this.snapshot = value;
          this.filter();
          if (this.importing && this.destinationName.getText().isBlank()) {
            this.destinationName.setText(value.name());
          }
          this.summary.setText(
              value.name()
                  + " · Файлов: "
                  + value.files()
                  + " · Папок: "
                  + (value.items().size() - value.files())
                  + " · "
                  + size(value.bytes()));
          this.status.setText(
              this.importing
                  ? "Состав пакета прочитан. Контрольные суммы всех файлов будут проверены при восстановлении."
                  : "Проверены файлы и их контрольные суммы. Выберите место для пакета вне папки проекта.");
        });
  }

  private void filter() {
    final var selected = this.entries.getSelectionModel().getSelectedItem();
    final String term = this.search.getText();
    this.entries
        .getItems()
        .setAll(
            this.snapshot == null
                ? java.util.List.of()
                : this.snapshot.items().stream()
                    .filter(
                        item ->
                            this.allFiles.isSelected() || !term.isBlank() || !service(item.path()))
                    .filter(item -> LaboratoryTextSearch.matches(item.path(), term))
                    .toList());
    if (selected != null) {
      this.entries
          .getSelectionModel()
          .select(
              this.entries.getItems().stream()
                  .filter(item -> item.path().equals(selected.path()))
                  .findFirst()
                  .orElse(null));
    }
    this.found.setText(
        this.snapshot == null
            ? ""
            : "В списке: "
                + this.entries.getItems().size()
                + " из "
                + this.snapshot.items().size()
                + ". Все файлы входят в пакет.");
  }

  private static boolean service(final String path) {
    for (final String part : path.split("/")) {
      if (part.equals(".chemistry-library")
          || part.endsWith(".qchem.history")
          || part.endsWith(".qchem.assets")
          || part.endsWith(".qchem.lock")) {
        return true;
      }
    }
    return false;
  }

  private void chooseDestination() {
    if (this.snapshot == null || this.worker != null) {
      return;
    }
    if (!this.importing) {
      final FileChooser chooser = chooser();
      chooser.setTitle("Сохранить пакет проекта");
      chooser.setInitialFileName(this.snapshot.name() + ".qchemproject");
      final var selected = chooser.showSaveDialog(this.owner);
      if (selected != null) {
        this.write(selected.toPath());
      }
    } else {
      final String name = this.destinationName.getText().strip();
      if (name.isBlank()
          || name.matches(".*[<>:\"/\\\\|?*\\p{Cntrl}].*")
          || name.endsWith(".")
          || name.equals("..")
          || name.matches("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\..*)?")) {
        this.status.setText(
            "Укажите допустимое имя новой папки, без разделителей пути и служебных символов.");
        return;
      }
      final DirectoryChooser chooser = new DirectoryChooser();
      chooser.setTitle("Где создать новую папку «" + name + "»");
      final var parent = chooser.showDialog(this.owner);
      if (parent != null) {
        this.write(parent.toPath().resolve(name));
      }
    }
  }

  private void write(final Path target) {
    if (this.disposed || this.worker != null || this.snapshot == null) {
      return;
    }
    this.writing = true;
    final var expected = this.snapshot;
    this.work(
        () -> {
          if (this.importing) {
            this.archive.importTo(this.source, target, expected, this::report);
          } else {
            this.archive.export(this.source, target, expected, this::report);
          }
          return target.toAbsolutePath().normalize();
        },
        path -> {
          this.status.setText(
              (this.importing ? "Проект восстановлен: " : "Пакет записан и проверен: ")
                  + path.getFileName());
          this.status.setTooltip(new Tooltip(path.toString()));
          if (this.importing) {
            this.restored.accept(path);
          }
        });
  }

  @FunctionalInterface
  private interface Work<T> {
    T run() throws Exception;
  }

  private <T> void work(final Work<T> action, final Consumer<T> success) {
    if (this.disposed || this.worker != null) {
      return;
    }
    final int requested = ++this.generation;
    this.cancelled = false;
    this.lastReport = 0;
    this.status.setTooltip(null);
    this.transfer.setDisable(true);
    this.refresh.setDisable(true);
    this.stop.setDisable(false);
    this.progress.setVisible(true);
    this.destinationName.setDisable(true);
    this.status.setText(
        this.writing
            ? this.importing ? "Восстанавливаю проект…" : "Создаю и проверяю пакет…"
            : "Читаю состав проекта…");
    this.worker =
        new Thread(
            () -> {
              T value = null;
              Exception problem = null;
              try {
                value = action.run();
              } catch (final Exception exception) {
                problem = exception;
              }
              final T result = value;
              final Exception failure = problem;
              Platform.runLater(
                  () -> {
                    this.worker = null;
                    if (this.disposed || requested != this.generation) {
                      return;
                    }
                    this.writing = false;
                    this.stop.setDisable(true);
                    this.refresh.setDisable(false);
                    this.progress.setVisible(false);
                    this.destinationName.setDisable(false);
                    if (failure == null) {
                      try {
                        success.accept(result);
                      } catch (final RuntimeException callbackFailure) {
                        this.status.setText(
                            "Перенос завершён, но переход к проекту не выполнен. Откройте восстановленную папку через «Папки».");
                        this.status.setTooltip(new Tooltip(callbackFailure.toString()));
                      }
                    } else {
                      this.status.setText(
                          this.cancelled
                              ? "Действие отменено. Незавершённый проект или пакет не опубликован."
                              : "Не завершено: " + message(failure));
                      this.status.setTooltip(new Tooltip(failure.toString()));
                      if (this.snapshot == null) {
                        this.found.setText("Состав не прочитан");
                      }
                    }
                    if (!this.disposed) {
                      this.transfer.setDisable(this.snapshot == null);
                    }
                  });
            },
            "laboratory-project-transfer");
    this.worker.setDaemon(true);
    this.worker.start();
  }

  private long lastReport;

  private void report(final String text) {
    final long now = System.nanoTime();
    if (now - this.lastReport < 150_000_000L) {
      return;
    }
    this.lastReport = now;
    final int requested = this.generation;
    Platform.runLater(
        () -> {
          if (!this.disposed
              && requested == this.generation
              && this.worker != null
              && !this.cancelled) {
            this.status.setText(text);
          }
        });
  }

  private static String message(final Exception failure) {
    if (failure instanceof java.util.zip.ZipException) {
      return "Пакет повреждён или имеет неподдерживаемый формат. Выберите целый файл .qchemproject.";
    }
    if (failure instanceof java.nio.file.NoSuchFileException) {
      return "Исходный файл или папка больше не найдены. Проверьте расположение и повторите проверку.";
    }
    if (failure instanceof java.nio.file.AccessDeniedException) {
      return "Нет доступа к файлу или папке. Проверьте права доступа и повторите действие.";
    }
    return failure.getMessage() == null
        ? "Не удалось обработать проект. Повторите проверку состава."
        : failure.getMessage();
  }

  private static String size(final long bytes) {
    return bytes < 1024
        ? bytes + " байт"
        : bytes < 1024 * 1024
            ? String.format(java.util.Locale.ROOT, "%.1f КиБ", bytes / 1024.0)
            : String.format(java.util.Locale.ROOT, "%.1f МиБ", bytes / (1024.0 * 1024));
  }

  private static FileChooser chooser() {
    final FileChooser chooser = new FileChooser();
    chooser
        .getExtensionFilters()
        .add(new FileChooser.ExtensionFilter("Пакет папки Quantum", "*.qchemproject"));
    return chooser;
  }
}

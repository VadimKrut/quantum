/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.nio.file.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryComponent;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEditor;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.*;

/** Revision browsing never mutates the current notebook; restoration preserves the editor's audit records. */
final class LaboratoryHistoryPage extends LaboratoryComputationPage {
  private record Difference(
      String name, LaboratoryComponent current, LaboratoryComponent selected) {}

  private final LaboratoryEditor editor;
  private final LaboratoryDrawing snapshot;
  private final Path document;
  private final BooleanSupplier sameFile;
  private final ListView<Path> versions = new ListView<>();
  private final TextArea description = area(), source = area();
  private final TableView<Difference> differences = new TableView<>();
  private final Label comparisonEmpty = note("Выберите доступную версию для сравнения");
  private final CheckBox changedOnly = new CheckBox("Только изменения");
  private final Button restore = new Button("Восстановить в редакторе");
  private LaboratoryFileRepository.Opened loaded;
  private List<Difference> rows = List.of();
  private Map<Path, Long> modified = Map.of();
  private Thread worker;
  private long generation;

  LaboratoryHistoryPage(
      final LaboratoryEditor editor,
      final Path document,
      final BooleanSupplier sameFile,
      final Runnable close) {
    super("История сохранений", close);
    this.editor = editor;
    this.snapshot = editor.document();
    this.document = document;
    this.sameFile = sameFile;
    this.setId("chemistry-history-page");
    this.versions.setId("chemistry-history-versions");
    this.versions.setMinWidth(140);
    this.versions.setPrefWidth(195);
    this.versions.setMaxWidth(220);
    this.versions.setPlaceholder(note("Предыдущих сохранённых версий пока нет"));
    this.versions.setCellFactory(
        view ->
            new ListCell<>() {
              @Override
              protected void updateItem(final Path path, final boolean empty) {
                super.updateItem(path, empty);
                if (empty || path == null) {
                  setText(null);
                  setTooltip(null);
                  return;
                }
                final var date =
                    java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy  HH:mm:ss")
                        .withZone(java.time.ZoneId.systemDefault())
                        .format(
                            java.time.Instant.ofEpochMilli(
                                LaboratoryHistoryPage.this.modified.getOrDefault(path, 0L)));
                setText(date + "\nВерсия " + (getIndex() + 1));
                setTooltip(new Tooltip(path.getFileName().toString()));
              }
            });
    this.versions
        .getSelectionModel()
        .selectedItemProperty()
        .addListener((o, a, path) -> this.preview(path));
    this.description.setId("chemistry-history-preview");
    this.source.setId("chemistry-history-source");
    this.source.setWrapText(false);
    this.differences.setId("chemistry-history-differences");
    this.differences.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    this.differences.setPlaceholder(this.comparisonEmpty);
    this.column("Компонент", d -> d.name(), d -> d.name(), 0.36);
    this.column("Сейчас", d -> value(d.current(), true), d -> value(d.current(), false), 0.32);
    this.column(
        "В выбранной версии",
        d -> value(d.selected(), true),
        d -> value(d.selected(), false),
        0.32);
    this.changedOnly.setTooltip(
        new Tooltip(
            "≈ — округлённое отображение; точные числа есть в подсказке ячейки и исходном QCHEM."));
    this.changedOnly.setId("chemistry-history-changes-only");
    this.changedOnly.setSelected(true);
    this.changedOnly.setOnAction(event -> this.filter());
    final VBox comparison = new VBox(8, this.changedOnly, this.differences);
    VBox.setVgrow(this.differences, Priority.ALWAYS);
    final TabPane tabs =
        new TabPane(
            new Tab("Количества и роли", comparison),
            new Tab("Описание изменений", this.description),
            new Tab("Исходный QCHEM", this.source));
    tabs.setId("chemistry-history-tabs");
    tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    tabs.setMinWidth(0);
    HBox.setHgrow(tabs, Priority.ALWAYS);
    final HBox content = new HBox(12, this.versions, tabs);
    content.setPadding(new Insets(0, 16, 0, 16));
    this.setCenter(content);
    this.restore.setId("chemistry-history-restore");
    this.restore.setDisable(true);
    this.restore.getStyleClass().add("laboratory-save");
    this.restore.setOnAction(event -> this.restore());
    final Label hint =
        note(
            "Журнал и вложения сохранятся. Склад не откатывается. Восстановление меняет редактор; для записи файла нажмите «Сохранить» после возврата к опыту.");
    final VBox footer = new VBox(8, hint, this.status, this.restore);
    footer.setPadding(new Insets(12, 16, 14, 16));
    this.setBottom(footer);
    this.list();
  }

  private void list() {
    final long request = this.begin("Читаю список сохранённых версий…");
    this.worker =
        Thread.ofVirtual()
            .name("laboratory-history-list")
            .start(
                () -> {
                  try {
                    final Path directory = LaboratoryFileRepository.historyDirectory(this.document);
                    final List<Path> paths;
                    if (!Files.isDirectory(directory)) {
                      paths = List.of();
                    } else {
                      try (final var stream = Files.list(directory)) {
                        paths =
                            stream
                                .filter(Files::isRegularFile)
                                .filter(p -> p.toString().endsWith(".qchem"))
                                .sorted(
                                    Comparator.comparingLong((Path p) -> p.toFile().lastModified())
                                        .reversed()
                                        .thenComparing(Path::toString))
                                .toList();
                      }
                    }
                    final Map<Path, Long> modified = new HashMap<>();
                    for (final Path path : paths) {
                      modified.put(path, Files.getLastModifiedTime(path).toMillis());
                    }
                    Platform.runLater(
                        () -> {
                          if (!this.valid(request)) {
                            return;
                          }
                          this.busy(false);
                          this.modified = Map.copyOf(modified);
                          this.versions.getItems().setAll(paths);
                          this.status.setText(
                              paths.isEmpty()
                                  ? "Предыдущих версий нет. Они появляются при сохранении изменённого опыта."
                                  : "Выберите версию для сравнения.");
                          if (!paths.isEmpty()) {
                            this.versions.getSelectionModel().selectFirst();
                          }
                        });
                  } catch (final Exception ex) {
                    this.failed(request, ex);
                  }
                });
  }

  private void preview(final Path path) {
    final long request = this.begin("Читаю версию и проверяю вложения…");
    this.loaded = null;
    this.rows = List.of();
    this.filter();
    this.description.clear();
    this.source.clear();
    if (path == null) {
      this.busy(false);
      return;
    }
    this.worker =
        Thread.ofVirtual()
            .name("laboratory-history-preview")
            .start(
                () -> {
                  try {
                    final var opened = new LaboratoryFileRepository().read(path);
                    final String text =
                        LaboratoryHistorySummary.describe(opened.drawing(), this.snapshot);
                    this.verifyAssets(opened.drawing());
                    final var compared = compare(this.snapshot, opened.drawing());
                    final String raw = new LaboratoryFileCodec().write(opened.drawing());
                    Platform.runLater(
                        () -> {
                          if (!this.valid(request)) {
                            return;
                          }
                          this.loaded = opened;
                          this.rows = compared;
                          this.filter();
                          this.description.setText(text);
                          this.source.setText(raw);
                          this.busy(false);
                          this.restore.setDisable(false);
                          this.status.setText(
                              "Версия прочитана; вложения проверены. Сравните изменения перед восстановлением.");
                        });
                  } catch (final Exception ex) {
                    this.failed(request, ex);
                  }
                });
  }

  private void restore() {
    if (this.loaded == null || this.running || this.disposed) {
      return;
    }
    final Path path = this.versions.getSelectionModel().getSelectedItem();
    final var expected = this.loaded;
    final long request = this.begin("Повторно проверяю выбранную версию…");
    this.versions.setDisable(true);
    this.worker =
        Thread.ofVirtual()
            .name("laboratory-history-restore")
            .start(
                () -> {
                  try {
                    final var revision = new LaboratoryFileRepository().read(path);
                    if (!revision.fingerprint().equals(expected.fingerprint())) {
                      throw new IllegalStateException(
                          "Версия изменилась после предпросмотра. Выберите её заново.");
                    }
                    this.verifyAssets(revision.drawing());
                    Platform.runLater(
                        () -> {
                          if (!this.valid(request)) {
                            return;
                          }
                          this.busy(false);
                          this.versions.setDisable(false);
                          try {
                            if (!this.sameFile.getAsBoolean()
                                || !this.editor.document().equals(this.snapshot)) {
                              throw new IllegalStateException(
                                  "Опыт изменился после предпросмотра. Откройте историю заново.");
                            }
                            this.editor.restore(revision.drawing());
                            this.finish();
                          } catch (final RuntimeException ex) {
                            this.restore.setDisable(true);
                            this.status.setText("Восстановление не выполнено: " + ex.getMessage());
                          }
                        });
                  } catch (final Exception ex) {
                    this.failed(request, ex);
                  }
                });
  }

  private void verifyAssets(final LaboratoryDrawing drawing) throws java.io.IOException {
    final var assets = new LaboratoryAssetStore();
    for (final var attachment : drawing.experiment().attachments()) {
      if (Thread.currentThread().isInterrupted()) {
        throw new java.io.InterruptedIOException("Чтение отменено.");
      }
      assets.locate(this.document, attachment);
    }
  }

  private long begin(final String text) {
    if (this.worker != null) {
      this.worker.interrupt();
    }
    this.generation++;
    this.busy(true);
    this.restore.setDisable(true);
    this.status.setText(text);
    return this.generation;
  }

  private boolean valid(final long request) {
    return !this.disposed && request == this.generation;
  }

  private void failed(final long request, final Exception ex) {
    Platform.runLater(
        () -> {
          if (!this.valid(request)) {
            return;
          }
          this.busy(false);
          this.versions.setDisable(false);
          this.restore.setDisable(true);
          this.status.setText(
              "Версию нельзя восстановить: "
                  + (ex.getMessage() == null ? ex.toString() : ex.getMessage()));
        });
  }

  @Override
  boolean canClose() {
    return true;
  }

  @Override
  void dispose() {
    super.dispose();
    this.generation++;
    if (this.worker != null) {
      this.worker.interrupt();
      this.worker = null;
    }
  }

  private void filter() {
    this.comparisonEmpty.setText(
        this.loaded == null
            ? "Выберите доступную версию для сравнения"
            : "Изменений компонентов нет");
    this.differences
        .getItems()
        .setAll(
            this.rows.stream()
                .filter(
                    row ->
                        !this.changedOnly.isSelected()
                            || !Objects.equals(row.current(), row.selected()))
                .toList());
  }

  private void column(
      final String title,
      final java.util.function.Function<Difference, String> value,
      final java.util.function.Function<Difference, String> exact,
      final double fraction) {
    final TableColumn<Difference, String> column = new TableColumn<>(title);
    column.setCellValueFactory(row -> new ReadOnlyStringWrapper(value.apply(row.getValue())));
    column.setMinWidth(90);
    column
        .prefWidthProperty()
        .bind(this.differences.widthProperty().multiply(fraction).subtract(6));
    column.setCellFactory(
        c ->
            new TableCell<>() {
              private final Label text = new Label();

              {
                this.text.setWrapText(true);
                this.text.setMinHeight(Region.USE_PREF_SIZE);
                this.text.maxWidthProperty().bind(c.widthProperty().subtract(20));
                setPrefHeight(Region.USE_COMPUTED_SIZE);
              }

              protected void updateItem(final String value, final boolean empty) {
                super.updateItem(value, empty);
                setText(null);
                this.text.setText(empty ? "" : value);
                setGraphic(empty ? null : this.text);
                final Difference row = getTableRow() == null ? null : getTableRow().getItem();
                setTooltip(empty || row == null ? null : new Tooltip(exact.apply(row)));
              }
            });
    this.differences.getColumns().add(column);
  }

  private static List<Difference> compare(
      final LaboratoryDrawing current, final LaboratoryDrawing revision) {
    final Map<String, LaboratoryComponent> present = new LinkedHashMap<>(),
        selected = new LinkedHashMap<>();
    current.experiment().components().forEach(c -> present.put(c.id(), c));
    revision.experiment().components().forEach(c -> selected.put(c.id(), c));
    final Set<String> ids = new LinkedHashSet<>(present.keySet());
    ids.addAll(selected.keySet());
    return ids.stream()
        .map(
            id -> {
              final var now = present.get(id);
              final var old = selected.get(id);
              return new Difference(
                  old == null ? now.molecule().displayName() : old.molecule().displayName(),
                  now,
                  old);
            })
        .toList();
  }

  private static String value(final LaboratoryComponent c, final boolean compact) {
    return c == null
        ? "Компонента нет"
        : c.molecule().displayName()
            + "\n"
            + LaboratoryInspector.roleText(c.role())
            + "\nПлан: "
            + (c.planned() == null
                ? "не указан"
                : LaboratoryQuantityText.format(c.planned(), compact))
            + "\n"
            + c.evidence().quantityLabel()
            + ": "
            + (c.actual() == null
                ? "не указан"
                : LaboratoryQuantityText.format(c.actual(), compact))
            + "\n"
            + c.evidence();
  }

  private static TextArea area() {
    final var area = new TextArea();
    area.setEditable(false);
    area.setWrapText(true);
    return area;
  }
}

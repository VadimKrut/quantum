/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import static ru.pathcreator.vadim.quantum.desktop.chemistry.ui.LaboratoryWorkbenchControls.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratoryStorageMap.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.*;

/** Visual storage over real stock batches and existing sample events, with no duplicate quantity balances. */
final class LaboratoryStoragePage extends LaboratoryComputationPage {
  private record Destination(String id, String title) {
    public String toString() {
      return this.title;
    }
  }

  private record Row(
      Item item,
      String title,
      String quantity,
      String location,
      Slot slot,
      String status,
      Path source,
      String sampleId,
      boolean current,
      String warning) {}

  private final LaboratoryEditor editor;
  private LaboratoryDrawing expected;
  private final Path folder;
  private final BiConsumer<Path, String> openSample;
  private final Runnable save;
  private final LaboratoryWorkbenchBooks.Book<LaboratoryStorageMap> repository;
  private LaboratoryWorkbenchBooks.Opened<LaboratoryStorageMap> opened;
  private LaboratoryInventory inventory;
  private LaboratorySampleCatalog.Snapshot catalog;
  private List<Row> rows = List.of();
  private final ExecutorService worker =
      Executors.newSingleThreadExecutor(
          r -> {
            final var t = new Thread(r, "storage-map");
            t.setDaemon(true);
            return t;
          });
  private Future<?> task;
  private final TableView<Row> table = table();
  private final TreeView<Place> tree = new TreeView<>();
  private final GridPane grid = new GridPane();
  private final TextField search = new TextField(), row = new TextField(), column = new TextField();
  private final ComboBox<String> state =
      choice(
          new String[] {
            "Все", "Без места", "Срок истёк", "Срок до 30 дней", "Вскрытые", "Конфликты мест"
          });
  private final ComboBox<Destination> destination = new ComboBox<>(),
      placeFilter = new ComboBox<>();
  private final FlowPane compactPlaces = new FlowPane(8, 8);
  private final ScrollPane gridScroll = new ScrollPane(this.grid);
  private final TextArea reason = area(2), details = area(7), diagnostics = area(3);
  private final Label summary = label(""),
      gridLabel = label("Выберите коробку в дереве."),
      gridHint =
          label(
              "Свободно — по прочитанным данным; неполный каталог не подтверждает свободное место."),
      moveLabel = label("Выберите партию или образец.");
  private final TitledPane detailsPane = new TitledPane(),
      movePane = new TitledPane(),
      mapPane = new TitledPane();
  private final VBox selectedActions = new VBox(8);
  private final FlowPane coordinates = new FlowPane(10, 8);
  private final Label nextTask = label("Найдите партию или образец, чтобы указать место");
  private Button beginMove;
  private final VBox leave = new VBox(8);
  private final HBox wide = new HBox(12);
  private final VBox treeBox = new VBox(8), content = new VBox(10);
  private boolean loading, dirty, conflict, reading;
  private int workVersion;
  private String positionFeedback;
  private String draftTarget;
  private Properties deferred, retained;
  private ScrollPane body;
  private final GridPane destinationGrid = new GridPane();
  private final ScrollPane destinationScroll = new ScrollPane(this.destinationGrid);
  private final TitledPane destinationMap = new TitledPane();
  private final Label positionHint = label("");

  LaboratoryStoragePage(
      final LaboratoryEditor editor,
      final Path folder,
      final BiConsumer<Path, String> openSample,
      final Runnable save,
      final Runnable close) {
    super("Хранение и сроки", close);
    this.editor = editor;
    this.expected = editor.document();
    this.folder = folder.toAbsolutePath().normalize();
    this.openSample = openSample;
    this.save = save;
    this.repository = LaboratoryWorkbenchBooks.storage(this.folder);
    this.setId("chemistry-storage-page");
    this.search.setPromptText("Название, код, место…");
    this.search.setId("storage-search");
    this.destination.setMaxWidth(Double.MAX_VALUE);
    this.row.setPromptText("Ряд: A, B…");
    this.column.setPromptText("Место: 1, 2…");
    this.row.setPrefColumnCount(4);
    this.column.setPrefColumnCount(4);
    this.details.setEditable(false);
    this.details.setMinHeight(130);
    this.details.setPrefHeight(145);
    this.details.setMaxHeight(160);
    this.table.setMinHeight(Region.USE_PREF_SIZE);
    this.table.setMaxHeight(Region.USE_PREF_SIZE);
    this.table
        .getItems()
        .addListener((javafx.collections.ListChangeListener<Row>) change -> this.tableHeight());
    this.tableHeight();
    this.diagnostics.setEditable(false);
    this.table
        .getColumns()
        .setAll(
            column("Партия / образец", Row::title, 240),
            column("Остаток", Row::quantity, 110),
            column("Место", Row::location, 240),
            column("Состояние / срок", this::flags, 220));
    this.table
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (o, before, after) -> {
              if (this.loading) {
                return;
              }
              if (this.dirty
                  && this.draftTarget != null
                  && after != null
                  && !key(after).equals(this.draftTarget)) {
                this.loading = true;
                this.table.getSelectionModel().select(before);
                this.loading = false;
                this.status.setText("Сначала выполните или сбросьте подготовленное перемещение.");
                return;
              }
              if (this.dirty && this.draftTarget == null && after != null) {
                this.draftTarget = key(after);
              }
              this.show();
            });
    this.tree.setShowRoot(true);
    this.tree.setPrefHeight(260);
    this.tree.setCellFactory(
        t ->
            new TreeCell<>() {
              protected void updateItem(Place value, boolean empty) {
                super.updateItem(value, empty);
                this.setText(empty ? null : value == null ? "Все места" : value.label());
              }
            });
    this.tree.getSelectionModel().selectedItemProperty().addListener(o -> this.filter());
    this.search.textProperty().addListener(o -> this.filter());
    this.state.valueProperty().addListener(o -> this.filter());
    for (final TextInputControl input : List.of(this.row, this.column, this.reason)) {
      input.textProperty().addListener(o -> this.changed());
    }
    this.row.textProperty().addListener(o -> this.positionSelection());
    this.column.textProperty().addListener(o -> this.positionSelection());
    this.destination
        .valueProperty()
        .addListener(
            o -> {
              this.changed();
              this.moveFields();
            });
    this.grid.setHgap(5);
    this.grid.setVgap(5);
    this.gridScroll.setPannable(true);
    this.gridScroll.setFitToWidth(false);
    this.gridScroll.setFitToHeight(false);
    this.gridScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    this.gridScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    this.gridScroll.setMinHeight(Region.USE_PREF_SIZE);
    this.gridScroll.setMaxHeight(Region.USE_PREF_SIZE);
    this.placeFilter.setId("storage-place-filter");
    this.placeFilter.setPrefWidth(370);
    this.placeFilter.setMaxWidth(Double.MAX_VALUE);
    this.placeFilter
        .valueProperty()
        .addListener(
            (o, before, after) -> {
              if (this.loading || after == null) {
                return;
              }
              final var target = this.findPlace(this.tree.getRoot(), after.id());
              if (target != null) {
                this.tree.getSelectionModel().select(target);
              }
            });
    this.compactPlaces
        .getChildren()
        .setAll(
            field("Показать места", this.placeFilter),
            field("Найти партию или образец", this.search));
    this.treeBox
        .getChildren()
        .setAll(
            this.tree,
            new FlowPane(
                7,
                7,
                button("+ Шкаф", "cabinet", () -> this.place(Kind.CABINET)),
                button("+ Полка", "shelf", () -> this.place(Kind.SHELF)),
                button("+ Коробка", "box", () -> this.place(Kind.BOX))));
    this.treeBox.setPrefWidth(265);
    this.treeBox.setMinWidth(180);
    this.coordinates
        .getChildren()
        .setAll(field("Ряд коробки", this.row), field("Столбец", this.column));
    visible(this.coordinates, false);
    final Button commit = button("Записать новое место", "move", this::move);
    commit.getStyleClass().add("laboratory-save");
    this.destinationGrid.setHgap(5);
    this.destinationGrid.setVgap(5);
    this.destinationScroll.setPannable(true);
    this.destinationScroll.setFitToWidth(false);
    this.destinationScroll.setPrefViewportHeight(215);
    this.destinationScroll.setMaxHeight(245);
    this.destinationScroll.setMinHeight(100);
    this.destinationMap.setText("Выбрать позицию на карте");
    this.destinationMap.setId("storage-position-picker");
    this.destinationMap.setAnimated(false);
    this.destinationMap.setExpanded(false);
    this.destinationMap.setContent(
        new VBox(
            8,
            label("Выберите ячейку. Перемещение запишется отдельной кнопкой ниже."),
            this.destinationScroll));
    visible(this.destinationMap, false);
    this.positionHint.setId("storage-position-hint");
    visible(this.positionHint, false);
    final VBox move =
        new VBox(
            10,
            field("Куда переместить", this.destination),
            this.coordinates,
            this.positionHint,
            this.destinationMap,
            field("Основание перемещения", this.reason),
            new FlowPane(7, 7, commit, button("Сбросить ввод", "clear", this::clearMove)),
            disclosure(
                "Снять с места без перемещения",
                "unplace-details",
                button("Снять с места", "unplace", this::unplace)));
    this.movePane.setText("Новое место");
    this.movePane.setId("storage-move-details");
    this.movePane.setContent(move);
    this.movePane.setExpanded(false);
    this.movePane.setAnimated(false);
    visible(this.movePane, false);
    this.beginMove =
        button(
            "Указать место…",
            "begin-move",
            () -> {
              this.selected();
              this.movePane.setExpanded(true);
              this.destination.requestFocus();
              this.revealMove();
            });
    this.beginMove.getStyleClass().add("laboratory-save");
    this.movePane
        .expandedProperty()
        .addListener(o -> visible(this.beginMove, !this.movePane.isExpanded()));
    this.selectedActions.getChildren().setAll(this.moveLabel, this.beginMove);
    visible(this.selectedActions, false);
    final TitledPane issues = new TitledPane("Состояние чтения", this.diagnostics);
    issues.setExpanded(false);
    issues.setAnimated(false);
    this.detailsPane.setText("Сведения и действия выбранного объекта");
    this.detailsPane.setAnimated(false);
    this.detailsPane.setExpanded(false);
    this.detailsPane.setContent(
        new VBox(
            8,
            this.details,
            new FlowPane(
                7,
                7,
                button("Открыть образец", "open-sample", this::open),
                button("Дата вскрытия…", "opened", () -> this.metadata(Action.OPENED)),
                button("Срок годности…", "expiry", () -> this.metadata(Action.EXPIRY)),
                button("Примечание…", "note", () -> this.metadata(Action.NOTE)),
                button("История хранения", "history", this::history))));
    visible(this.detailsPane, false);
    visible(this.gridScroll, false);
    visible(this.gridHint, false);
    this.nextTask.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
    this.nextTask.setId("storage-next-task");
    this.mapPane.setText("Карта выбранной коробки");
    this.mapPane.setId("storage-map-details");
    this.mapPane.setExpanded(true);
    this.mapPane.setAnimated(false);
    this.mapPane.setContent(new VBox(8, this.gridLabel, this.gridScroll, this.gridHint));
    visible(this.mapPane, false);
    this.content
        .getChildren()
        .setAll(
            this.nextTask,
            this.compactPlaces,
            disclosure("Фильтр по срокам и состоянию", "filter-details", this.state),
            this.summary,
            this.table,
            this.selectedActions,
            this.movePane,
            this.detailsPane,
            this.mapPane,
            disclosure("Места хранения: шкафы, полки, коробки", "places-details", this.treeBox),
            issues);
    this.content.setMinWidth(0);
    this.content.setMaxWidth(1100);
    HBox.setHgrow(this.content, Priority.ALWAYS);
    this.wide.getChildren().setAll(this.content);
    this.wide.setPadding(new Insets(12));
    this.body = scroll(this.wide);
    this.body.setFitToHeight(false);
    this.setCenter(this.body);
    this.leave
        .getChildren()
        .setAll(
            label("Есть незаписанное перемещение."),
            new FlowPane(
                7,
                7,
                button(
                    "Отбросить ввод и выйти",
                    "discard",
                    () -> {
                      this.clearMove();
                      this.finish();
                    }),
                button("Продолжить", "continue", () -> this.leave.setManaged(false))));
    this.leave.setManaged(false);
    this.leave.visibleProperty().bind(this.leave.managedProperty());
    final Button stop = button("Остановить чтение", "stop", this::stop);
    stop.visibleProperty().bind(this.progress.visibleProperty());
    stop.managedProperty().bind(stop.visibleProperty());
    final VBox bottom =
        new VBox(
            8,
            this.leave,
            new FlowPane(
                8,
                8,
                button(
                    "Обновить",
                    "refresh",
                    () -> {
                      this.clean();
                      this.load();
                    }),
                stop),
            this.status);
    bottom.setPadding(new Insets(10, 16, 12, 16));
    this.setBottom(bottom);
    this.treeBox.setMaxWidth(Double.MAX_VALUE);
    this.treeBox.setPrefWidth(600);
    this.tree.setPrefHeight(260);
    this.addEventFilter(
        KeyEvent.KEY_PRESSED,
        e -> {
          if (e.isShortcutDown() && e.getCode() == KeyCode.F) {
            this.search.requestFocus();
            this.search.selectAll();
            e.consume();
          }
        });
    this.load();
  }

  private static TitledPane disclosure(final String text, final String id, final Node content) {
    final var pane = new TitledPane(text, content);
    pane.setId("storage-" + id);
    pane.setExpanded(false);
    pane.setAnimated(false);
    return pane;
  }

  private void revealMove() {
    Platform.runLater(
        () -> {
          if (this.disposed || this.body == null || this.getScene() == null) {
            return;
          }
          this.body.applyCss();
          this.body.layout();
          final double range =
              this.wide.getBoundsInLocal().getHeight() - this.body.getViewportBounds().getHeight();
          if (range > 0) {
            final double top =
                this.movePane.localToScene(this.movePane.getBoundsInLocal()).getMinY()
                    - this.wide.localToScene(this.wide.getBoundsInLocal()).getMinY();
            this.body.setVvalue(Math.max(0, Math.min(1, (top - 12) / range)));
          }
        });
  }

  private Place destinationBox() {
    final var target = this.destination.getValue();
    return target == null || this.opened == null
        ? null
        : this.opened.value().places().stream()
            .filter(p -> p.id().equals(target.id()) && p.kind() == Kind.BOX)
            .findFirst()
            .orElse(null);
  }

  private void moveFields() {
    final var box = this.destinationBox();
    visible(this.coordinates, box != null);
    visible(this.destinationMap, box != null);
    visible(this.positionHint, box != null);
    this.destinationGrid.getChildren().clear();
    if (box != null) {
      for (int r = 1; r <= box.rows(); r++) {
        for (int c = 1; c <= box.columns(); c++) {
          final var position = new Slot(box.id(), r, c);
          final String code = coordinate(position);
          final var cell = button(code, "position-" + code, () -> this.selectPosition(position));
          cell.setUserData(position);
          cell.setMinSize(82, 48);
          cell.setPrefSize(82, 48);
          cell.setMaxSize(82, 48);
          this.destinationGrid.add(cell, c - 1, r - 1);
        }
      }
    }
    this.positionSelection();
  }

  private static String coordinate(final Slot slot) {
    return Character.toString((char) ('A' + slot.row() - 1))
        + String.format(Locale.ROOT, "%02d", slot.column());
  }

  private Slot enteredPosition(final Place box) {
    final String row = this.row.getText().strip().toUpperCase(Locale.ROOT);
    if (box == null || !row.matches("[A-Z]")) {
      return null;
    }
    try {
      final int r = row.charAt(0) - 'A' + 1, c = Integer.parseInt(this.column.getText().strip());
      return r >= 1 && r <= box.rows() && c >= 1 && c <= box.columns()
          ? new Slot(box.id(), r, c)
          : null;
    } catch (final NumberFormatException invalid) {
      return null;
    }
  }

  private void positionSelection() {
    if (this.positionFeedback != null && this.status.getText().equals(this.positionFeedback)) {
      this.status.setText("");
    }
    this.positionFeedback = null;
    final var box = this.destinationBox();
    if (box == null) {
      return;
    }
    final var selected = this.table.getSelectionModel().getSelectedItem();
    final var position = this.enteredPosition(box);
    final var occupants = this.collisions(position);
    final boolean occupied =
        occupants.stream().anyMatch(r -> selected == null || !r.item().equals(selected.item()));
    final String hint =
        position == null
            ? "Выберите ячейку на карте или укажите ряд и столбец."
            : coordinate(position)
                + " · "
                + (occupied
                    ? "занято: " + String.join("; ", occupants.stream().map(Row::title).toList())
                    : occupants.isEmpty()
                        ? this.catalog != null && this.catalog.complete()
                            ? "свободно по прочитанным данным"
                            : "занятых записей не найдено; каталог прочитан не полностью"
                        : "текущее место выбранного объекта");
    this.positionHint.setText(hint.length() > 160 ? hint.substring(0, 157) + "…" : hint);
    this.positionHint.setTooltip(new Tooltip(hint));
    this.positionHint.setStyle(occupied ? "-fx-text-fill: -app-danger-text;" : "");
    for (final var node : this.destinationGrid.getChildren()) {
      final var cell = (Button) node;
      final var slot = (Slot) cell.getUserData();
      final var found = this.collisions(slot);
      final boolean chosen = slot.equals(position);
      final String state =
          found.isEmpty() ? this.vacantLabel() : found.size() == 1 ? "Занято" : "Конфликт";
      cell.setText(coordinate(slot) + "\n" + (chosen ? "Выбрано" : state));
      cell.setAccessibleText(coordinate(slot) + ", " + state + (chosen ? ", выбрано" : ""));
      cell.setTooltip(
          new Tooltip(
              found.isEmpty()
                  ? this.catalog != null && this.catalog.complete()
                      ? "Нет занятых записей в прочитанном каталоге."
                      : "Каталог прочитан не полностью; свободное место не подтверждено."
                  : String.join("\n", found.stream().map(Row::title).toList())));
      cell.setStyle(
          "-fx-padding: 4 5; "
              + (chosen
                  ? "-fx-background-color: -app-accent-soft; -fx-border-color: -app-accent; -fx-border-width: 2;"
                  : found.size() > 1
                      ? "-fx-border-color: -app-danger-text; -fx-border-width: 2;"
                      : found.isEmpty() ? "" : "-fx-font-weight: bold;"));
    }
  }

  private void selectPosition(final Slot position) {
    final var selected = this.selected();
    if (this.collisions(position).stream().anyMatch(r -> !r.item().equals(selected.item()))) {
      this.positionStatus("Позиция " + coordinate(position) + " занята. Выберите другую ячейку.");
      return;
    }
    this.row.setText(Character.toString((char) ('A' + position.row() - 1)));
    this.column.setText(Integer.toString(position.column()));
    this.positionSelection();
    this.positionStatus(
        "Выбрана позиция "
            + coordinate(position)
            + ". Проверьте основание и запишите новое место.");
  }

  private String vacantLabel() {
    return this.catalog != null && this.catalog.complete() ? "Свободно" : "Неизвестно";
  }

  private void positionStatus(final String message) {
    this.positionFeedback = message;
    this.status.setText(message);
  }

  private static void visible(final Node node, final boolean shown) {
    node.setVisible(shown);
    node.setManaged(shown);
  }

  private void tableHeight() {
    this.table.setPrefHeight(Math.min(260, Math.max(94, 38 + this.table.getItems().size() * 56)));
  }

  private Button button(final String title, final String id, final Runnable action) {
    final var button = new Button(title);
    button.setId("storage-" + id);
    button.setOnAction(
        e -> {
          if (this.running && !id.equals("stop")) {
            this.status.setText("Дождитесь завершения записи.");
            return;
          }
          try {
            action.run();
          } catch (final RuntimeException error) {
            this.status.setText(message(error));
          }
        });
    return button;
  }

  private void changed() {
    if (!this.loading) {
      final var selected = this.table.getSelectionModel().getSelectedItem();
      this.dirty = true;
      if (this.draftTarget == null && selected != null) {
        this.draftTarget = key(selected);
      }
    }
  }

  private static String key(final Row row) {
    return row.item().kind().name() + ":" + row.item().id();
  }

  private void clean() {
    if (this.pending()) {
      throw new IllegalArgumentException(
          "Сначала выполните или сбросьте незаписанное перемещение.");
    }
  }

  private <T> void work(final Callable<T> task, final Consumer<T> success) {
    final int version = ++this.workVersion;
    this.busy(true);
    this.wide.setDisable(true);
    this.task =
        this.worker.submit(
            () -> {
              try {
                final T result = task.call();
                Platform.runLater(
                    () -> {
                      if (this.disposed || version != this.workVersion) {
                        return;
                      }
                      this.reading = false;
                      this.busy(false);
                      this.wide.setDisable(false);
                      try {
                        success.accept(result);
                      } catch (final RuntimeException error) {
                        this.status.setText(message(error));
                      }
                    });
              } catch (final Exception error) {
                Platform.runLater(
                    () -> {
                      if (!this.disposed && version == this.workVersion) {
                        this.reading = false;
                        this.busy(false);
                        this.wide.setDisable(false);
                        this.status.setText(message(error));
                      }
                    });
              }
            });
  }

  private void load() {
    this.reading = true;
    this.expected = this.editor.document();
    final var current = new LaboratorySampleCatalog.Current(null, this.expected, true);
    this.work(
        () -> {
          final var map = this.repository.read();
          final Path ledger = this.folder.resolve("materials.qchem");
          if (Files.exists(ledger, LinkOption.NOFOLLOW_LINKS)
              && Files.size(ledger) > 32L * 1024 * 1024) {
            throw new IllegalArgumentException("Учёт материалов превышает 32 МиБ.");
          }
          return new Object[] {
            map,
            new LaboratoryInventoryRepository().read(ledger).inventory(),
            new LaboratorySampleCatalog().scan(this.folder, current)
          };
        },
        result -> {
          this.opened = (LaboratoryWorkbenchBooks.Opened<LaboratoryStorageMap>) result[0];
          this.inventory = (LaboratoryInventory) result[1];
          this.catalog = (LaboratorySampleCatalog.Snapshot) result[2];
          this.tree();
          this.project();
          this.diagnostics.setText(
              (this.catalog.complete()
                      ? "Каталог прочитан в пределах заданных лимитов."
                      : "Неполный каталог:\n" + String.join("\n", this.catalog.diagnostics()))
                  + "\nТекущий опыт показан из редактора; его сохранённые копии с тем же идентификатором исключены.");
          if (this.deferred != null) {
            final var data = this.deferred;
            this.deferred = null;
            this.draft(data, true);
          }
          this.status.setText(
              "Хранение обновлено. Количества взяты из складской книги и исходных образцов.");
        });
  }

  private void stop() {
    if (!this.running) {
      return;
    }
    if (!this.reading) {
      this.status.setText("Дождитесь завершения записи книги.");
      return;
    }
    ++this.workVersion;
    if (this.task != null) {
      this.task.cancel(true);
    }
    this.reading = false;
    this.busy(false);
    this.wide.setDisable(false);
    this.status.setText("Чтение остановлено. Предыдущий снимок сохранён.");
  }

  private void tree() {
    final var selected = this.tree.getSelectionModel().getSelectedItem();
    final String selectedId =
        selected == null || selected.getValue() == null ? null : selected.getValue().id();
    final var root = new TreeItem<Place>();
    root.setExpanded(true);
    final Map<String, TreeItem<Place>> nodes = new HashMap<>();
    this.loading = true;
    for (final var p : this.opened.value().places()) {
      final var node = new TreeItem<>(p);
      node.setExpanded(true);
      nodes.put(p.id(), node);
      if (p.parentId() == null) {
        root.getChildren().add(node);
      } else {
        nodes.get(p.parentId()).getChildren().add(node);
      }
    }
    this.tree.setRoot(root);
    this.tree
        .getSelectionModel()
        .select(selectedId == null ? root : nodes.getOrDefault(selectedId, root));
    final String target =
        this.destination.getValue() == null ? null : this.destination.getValue().id();
    this.destination
        .getItems()
        .setAll(
            this.opened.value().places().stream()
                .map(p -> new Destination(p.id(), this.opened.value().path(p.id())))
                .toList());
    this.destination.setValue(
        this.destination.getItems().stream()
            .filter(d -> d.id().equals(target))
            .findFirst()
            .orElse(null));
    this.placeFilter.getItems().setAll(new Destination(null, "Все места"));
    this.placeFilter.getItems().addAll(this.destination.getItems());
    this.placeFilter.setValue(
        this.placeFilter.getItems().stream()
            .filter(d -> Objects.equals(d.id(), selectedId))
            .findFirst()
            .orElse(this.placeFilter.getItems().getFirst()));
    this.loading = false;
  }

  private TreeItem<Place> findPlace(final TreeItem<Place> node, final String id) {
    if (node == null) {
      return null;
    }
    if (Objects.equals(node.getValue() == null ? null : node.getValue().id(), id)) {
      return node;
    }
    for (final var child : node.getChildren()) {
      final var found = this.findPlace(child, id);
      if (found != null) {
        return found;
      }
    }
    return null;
  }

  private void project() {
    final List<Row> rows = new ArrayList<>();
    final var map = this.opened.value();
    final var stock = this.inventory.stock();
    for (final var b : this.inventory.batches()) {
      final var item = new Item(ItemKind.BATCH, b.id());
      final var state = map.state(item);
      final boolean moved =
          map.events().stream().anyMatch(e -> e.item().equals(item) && e.action() == Action.MOVE);
      final Slot slot = moved ? state.slot() : map.resolve(b.location()).orElse(null);
      final String location = slot == null ? moved ? "" : b.location() : map.address(slot);
      final var material =
          this.inventory.materials().stream()
              .filter(m -> m.id().equals(b.materialId()))
              .findFirst()
              .orElseThrow();
      rows.add(
          new Row(
              item,
              material.name() + " · " + b.label(),
              stock.get(b.id()).physical().toString(),
              location,
              slot,
              stock.get(b.id()).physical().value().signum() == 0 ? "Израсходована" : "На складе",
              null,
              null,
              false,
              ""));
    }
    for (final var found : this.catalog.rows()) {
      final boolean current =
          found.reference().experimentId().equals(this.expected.experiment().id());
      if (current && found.path() != null) {
        continue;
      }
      final LaboratorySample sample =
          current
              ? this.editor.document().experiment().samples().stream()
                  .filter(s -> s.id().equals(found.sample().id()))
                  .findFirst()
                  .orElse(found.sample())
              : found.sample();
      final var balance = sample.balance();
      rows.add(
          new Row(
              new Item(ItemKind.SAMPLE, found.reference().token()),
              sample.code() + " · " + balance.label(),
              balance.remaining() == null ? "Не измерено" : balance.remaining().toString(),
              balance.location(),
              map.resolve(balance.location()).orElse(null),
              balance.status().toString(),
              current ? null : found.path(),
              sample.id(),
              current,
              current ? "" : found.warning()));
    }
    this.rows = List.copyOf(rows);
    this.filter();
  }

  private String flags(final Row row) {
    final var s = this.opened.value().state(row.item());
    return row.status()
        + (s.opened() == null ? "" : " · вскрыт " + s.opened())
        + (s.expiry() == null
            ? " · срок не задан"
            : " · до "
                + s.expiry()
                + (s.expired(LocalDate.now())
                    ? " · истёк"
                    : s.soon(LocalDate.now(), 30) ? " · скоро" : ""))
        + (this.collisions(row.slot()).size() > 1 ? " · конфликт места" : "");
  }

  private List<Row> collisions(final Slot slot) {
    return slot == null || slot.row() == 0
        ? List.of()
        : this.rows.stream().filter(r -> slot.equals(r.slot())).toList();
  }

  private void filter() {
    if (this.opened == null || this.loading) {
      return;
    }
    final var selected = this.table.getSelectionModel().getSelectedItem();
    final String key = selected == null ? this.draftTarget : key(selected);
    final var node = this.tree.getSelectionModel().getSelectedItem();
    final String path =
        node == null || node.getValue() == null
            ? null
            : this.opened.value().path(node.getValue().id());
    final String text = this.search.getText().strip().toLowerCase(Locale.ROOT);
    final int state = this.state.getSelectionModel().getSelectedIndex();
    this.loading = true;
    this.placeFilter.setValue(
        this.placeFilter.getItems().stream()
            .filter(
                d ->
                    Objects.equals(
                        d.id(),
                        node == null || node.getValue() == null ? null : node.getValue().id()))
            .findFirst()
            .orElse(null));
    this.loading = false;
    final var shown =
        this.rows.stream()
            .filter(
                r ->
                    (r.title() + " " + r.location() + " " + r.item().id())
                        .toLowerCase(Locale.ROOT)
                        .contains(text))
            .filter(
                r ->
                    path == null
                        || r.location().equals(path)
                        || r.location().startsWith(path + " / "))
            .filter(
                r -> {
                  final var s = this.opened.value().state(r.item());
                  return switch (state) {
                    case 1 -> r.location().isBlank();
                    case 2 -> s.expired(LocalDate.now());
                    case 3 -> s.soon(LocalDate.now(), 30);
                    case 4 -> s.opened() != null;
                    case 5 -> this.collisions(r.slot()).size() > 1;
                    default -> true;
                  };
                })
            .toList();
    this.loading = true;
    this.table.getItems().setAll(shown);
    shown.stream()
        .filter(r -> key(r).equals(key))
        .findFirst()
        .ifPresent(this.table.getSelectionModel()::select);
    this.loading = false;
    this.summary.setText(
        (this.catalog != null && !this.catalog.complete() ? "Неполный каталог · " : "")
            + shown.size()
            + " из "
            + this.rows.size()
            + " объектов");
    this.grid();
    this.show();
  }

  private void grid() {
    this.grid.getChildren().clear();
    final var selected = this.tree.getSelectionModel().getSelectedItem();
    final var place = selected == null ? null : selected.getValue();
    if (place == null || place.kind() != Kind.BOX) {
      visible(this.gridScroll, false);
      visible(this.gridHint, false);
      this.gridLabel.setText(
          "Выберите коробку: ячейки показывают занятость по прочитанным данным.");
      visible(this.mapPane, false);
      return;
    }
    visible(this.gridScroll, true);
    visible(this.gridHint, true);
    visible(this.mapPane, true);
    this.gridScroll.setPrefHeight(place.rows() * 54 + (place.rows() - 1) * 5 + 24);
    this.gridLabel.setText(
        this.opened.value().path(place.id()) + " · " + place.rows() + " × " + place.columns());
    for (int r = 1; r <= place.rows(); r++) {
      for (int c = 1; c <= place.columns(); c++) {
        final var slot = new Slot(place.id(), r, c);
        final var occupants = this.collisions(slot);
        final String coordinate = (char) ('A' + r - 1) + String.format(Locale.ROOT, "%02d", c);
        final var button =
            new Button(
                coordinate
                    + "\n"
                    + (occupants.isEmpty()
                        ? this.vacantLabel()
                        : occupants.size() == 1 ? "Занято" : "Конфликт"));
        button.setMinHeight(54);
        button.setPrefHeight(54);
        button.setMaxHeight(54);
        button
            .prefWidthProperty()
            .bind(
                javafx.beans.binding.Bindings.createDoubleBinding(
                    () ->
                        Math.max(
                            82,
                            Math.min(
                                96,
                                (this.gridScroll.getWidth() - 25 - 5 * (place.columns() - 1))
                                    / place.columns())),
                    this.gridScroll.widthProperty()));
        button.minWidthProperty().bind(button.prefWidthProperty());
        button.setWrapText(false);
        button.setTooltip(
            new Tooltip(
                occupants.isEmpty()
                    ? "По текущему снимку; при неполном чтении свободное место не подтверждено."
                    : String.join("\n", occupants.stream().map(Row::title).toList())));
        if (!occupants.isEmpty()) {
          button.setStyle(
              occupants.size() > 1
                  ? "-fx-border-color: -app-danger-text; -fx-border-width: 2;"
                  : "-fx-font-weight: bold;");
        }
        button.setOnAction(
            e -> {
              if (this.dirty) {
                this.status.setText("Сначала выполните или сбросьте подготовленное перемещение.");
                return;
              }
              if (occupants.size() == 1) {
                this.search.clear();
                this.state.getSelectionModel().selectFirst();
                this.table.getSelectionModel().select(occupants.getFirst());
                this.table.scrollTo(occupants.getFirst());
              } else if (occupants.isEmpty()) {
                this.destination.setValue(
                    this.destination.getItems().stream()
                        .filter(d -> d.id().equals(place.id()))
                        .findFirst()
                        .orElseThrow());
                this.row.setText(Character.toString((char) ('A' + slot.row() - 1)));
                this.column.setText(Integer.toString(slot.column()));
                visible(this.movePane, true);
                this.movePane.setExpanded(true);
                this.revealMove();
              } else {
                this.status.setText(
                    "В этой позиции несколько записей; проверьте их перед перемещением.");
              }
            });
        this.grid.add(button, c - 1, r - 1);
      }
    }
  }

  private void show() {
    final var row = this.table.getSelectionModel().getSelectedItem();
    visible(this.selectedActions, row != null);
    visible(this.movePane, row != null || this.dirty);
    if (row == null) {
      this.details.clear();
      visible(this.detailsPane, false);
      this.moveLabel.setText("Выберите партию или образец.");
      this.nextTask.setText("Найдите партию или образец, чтобы указать место");
      return;
    }
    this.nextTask.setText("Укажите место выбранного объекта");
    this.beginMove.setText(row.location().isBlank() ? "Указать место…" : "Переместить…");
    visible(this.detailsPane, true);
    this.detailsPane.setTooltip(new Tooltip(row.title()));
    final var state = this.opened.value().state(row.item());
    this.details.setText(
        row.title()
            + "\nОстаток: "
            + row.quantity()
            + "\nМесто: "
            + (row.location().isBlank() ? "не задано" : row.location())
            + "\n"
            + this.flags(row)
            + "\n"
            + state.note()
            + "\n"
            + row.warning()
            + (row.source() == null ? "" : "\nИсходный опыт: " + row.source()));
    this.moveLabel.setText(
        row.title() + " · " + (row.location().isBlank() ? "место не задано" : row.location()));
  }

  private Row selected() {
    final var r = this.table.getSelectionModel().getSelectedItem();
    if (r == null) {
      throw new IllegalArgumentException("Выберите партию или образец.");
    }
    if (this.conflict || this.draftTarget != null && !this.draftTarget.equals(key(r))) {
      throw new IllegalArgumentException("Черновик относится к другому объекту или версии.");
    }
    return r;
  }

  private Slot slot() {
    if (this.destination.getValue() == null) {
      throw new IllegalArgumentException("Выберите место хранения.");
    }
    final var place =
        this.opened.value().places().stream()
            .filter(p -> p.id().equals(this.destination.getValue().id()))
            .findFirst()
            .orElseThrow();
    final Slot slot;
    if (place.kind() == Kind.BOX) {
      final String row = this.row.getText().strip().toUpperCase(Locale.ROOT);
      if (!row.matches("[A-Z]")) {
        throw new IllegalArgumentException("Ряд коробки: латинская буква A–Z.");
      }
      slot =
          new Slot(
              place.id(), row.charAt(0) - 'A' + 1, Integer.parseInt(this.column.getText().strip()));
    } else {
      slot = new Slot(place.id(), 0, 0);
    }
    this.opened.value().validate(slot);
    return slot;
  }

  private void move() {
    this.relocate(this.selected(), this.slot());
  }

  private void unplace() {
    this.relocate(this.selected(), null);
  }

  private void relocate(final Row selected, final Slot target) {
    if (this.reason.getText().isBlank()) {
      throw new IllegalArgumentException("Укажите основание перемещения.");
    }
    if (target != null
        && this.collisions(target).stream().anyMatch(r -> !r.item().equals(selected.item()))) {
      throw new IllegalArgumentException(
          "Позиция занята другой записью. Сначала проверьте и переместите её.");
    }
    if (!selected.warning().isBlank()) {
      throw new IllegalArgumentException(
          "Сначала устраните неоднозначность исходного образца: " + selected.warning());
    }
    final String address = target == null ? "" : this.opened.value().address(target);
    if (selected.item().kind() == ItemKind.SAMPLE) {
      if (!selected.current()) {
        throw new IllegalArgumentException(
            "Откройте исходный опыт кнопкой «Открыть образец», затем выполните перемещение там.");
      }
      if (!this.editor.document().equals(this.expected)) {
        this.conflict = true;
        throw new IllegalArgumentException(
            "Текущий опыт изменён. Сбросьте ввод и обновите хранение.");
      }
      final var experiment = this.expected.experiment();
      final var sample = LaboratorySampleService.sample(experiment, selected.sampleId());
      final var balance = sample.balance();
      final var next =
          new LaboratorySampleService()
              .event(
                  experiment,
                  sample.id(),
                  LaboratorySample.Action.DETAILS,
                  new LaboratorySampleService.Card(
                      sample.code(),
                      balance.label(),
                      address,
                      balance.container(),
                      this.reason.getText()),
                  null);
      this.editor.samples(
          experiment.samples(),
          next,
          "Перемещение образца «"
              + sample.code()
              + "»: "
              + (address.isBlank() ? "снят с места" : address));
      this.expected = this.editor.document();
      this.clearMove();
      this.project();
      this.save.run();
      this.status.setText("Место записано в историю образца; сохранение опыта запрошено.");
      return;
    }
    final var event =
        new Event(
            LaboratorySolutionPlanning.id("storage"),
            selected.item(),
            Instant.now(),
            Action.MOVE,
            target,
            null,
            this.reason.getText());
    this.persist(
        this.opened.value().append(event),
        "Перемещение партии записано; складское количество не менялось.",
        true);
  }

  private void persist(final LaboratoryStorageMap next, final String message, final boolean clear) {
    this.reading = false;
    final String expected = this.opened.fingerprint();
    final var selected = this.table.getSelectionModel().getSelectedItem();
    final String selectedKey = selected == null ? null : key(selected);
    this.work(
        () -> this.repository.write(next, expected),
        saved -> {
          this.opened = saved;
          if (clear) {
            this.clearMove();
          }
          this.tree();
          this.project();
          this.loading = true;
          this.table.getSelectionModel().clearSelection();
          if (selectedKey != null) {
            this.table.getItems().stream()
                .filter(row -> key(row).equals(selectedKey))
                .findFirst()
                .ifPresent(this.table.getSelectionModel()::select);
          }
          this.loading = false;
          this.show();
          this.status.setText(message);
        });
  }

  private void clearMove() {
    this.loading = true;
    this.row.clear();
    this.column.clear();
    this.reason.clear();
    this.destination.setValue(null);
    this.dirty = false;
    this.conflict = false;
    this.draftTarget = null;
    this.retained = null;
    this.loading = false;
    this.movePane.setExpanded(false);
    this.leave.setManaged(false);
    this.moveFields();
    this.show();
  }

  private void place(final Kind kind) {
    if (this.opened == null) {
      throw new IllegalArgumentException("Дождитесь чтения схемы.");
    }
    this.clean();
    final var selection = this.tree.getSelectionModel().getSelectedItem();
    final Place parent = selection == null ? null : selection.getValue();
    if (kind != Kind.CABINET
        && (parent == null
            || kind == Kind.SHELF && parent.kind() != Kind.CABINET
            || kind == Kind.BOX && parent.kind() != Kind.SHELF)) {
      throw new IllegalArgumentException(
          kind == Kind.SHELF
              ? "Выберите шкаф для новой полки."
              : "Выберите полку для новой коробки.");
    }
    final TextField label = new TextField(), rows = new TextField(), columns = new TextField();
    final List<Node> fields = new ArrayList<>();
    fields.add(field("Название", label));
    if (kind == Kind.BOX) {
      fields.add(field("Рядов (1–26)", rows));
      fields.add(field("Столбцов (1–24, всего до 400)", columns));
    }
    fields.add(
        label(
            "Адреса сохраняются как понятные названия. Сохранённые названия неизменяемы; для нового расположения создайте новое место."));
    dialog(
        this,
        "Новое место: " + kind,
        "Создать место",
        fields,
        () -> {
          final var place =
              new Place(
                  LaboratorySolutionPlanning.id("place"),
                  kind == Kind.CABINET ? null : parent.id(),
                  kind,
                  label.getText(),
                  kind == Kind.BOX ? number(rows, true).intValueExact() : 0,
                  kind == Kind.BOX ? number(columns, true).intValueExact() : 0);
          this.persist(this.opened.value().append(place), "Место добавлено.", false);
        });
  }

  private void metadata(final Action action) {
    this.clean();
    final var selected = this.selected();
    final var state = this.opened.value().state(selected.item());
    final DatePicker date =
        new DatePicker(action == Action.OPENED ? state.opened() : state.expiry());
    final TextArea note = area(3);
    final List<Node> fields = new ArrayList<>();
    if (action != Action.NOTE) {
      fields.add(field(action.toString() + " (пусто — снять дату)", date));
    }
    fields.add(field("Основание / примечание", note));
    dialog(
        this,
        action.toString(),
        "Записать",
        fields,
        () -> {
          if (action == Action.OPENED
              && date.getValue() != null
              && date.getValue().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                "Фактическая дата вскрытия не может быть в будущем.");
          }
          this.persist(
              this.opened
                  .value()
                  .append(
                      new Event(
                          LaboratorySolutionPlanning.id("storage"),
                          selected.item(),
                          Instant.now(),
                          action,
                          null,
                          action == Action.NOTE ? null : date.getValue(),
                          note.getText())),
              "Метаданные хранения записаны.",
              false);
        });
  }

  private void open() {
    this.clean();
    final var row = this.selected();
    if (row.item().kind() != ItemKind.SAMPLE) {
      throw new IllegalArgumentException("Для партии сведения доступны в учёте материалов.");
    }
    this.openSample.accept(row.source(), row.sampleId());
  }

  private void history() {
    final var selected = this.selected();
    final TextArea text = area(20);
    text.setEditable(false);
    final var out = new StringBuilder();
    this.opened.value().events().stream()
        .filter(e -> e.item().equals(selected.item()))
        .forEach(
            e ->
                out.append(e.time())
                    .append(" · ")
                    .append(e.action())
                    .append("\n")
                    .append(e.slot() == null ? "" : this.opened.value().address(e.slot()))
                    .append(e.date() == null ? "" : e.date())
                    .append("\n")
                    .append(e.note())
                    .append("\n\n"));
    if (selected.item().kind() == ItemKind.SAMPLE) {
      out.append(
          "История мест образца хранится в его исходном опыте. Откройте образец для просмотра DETAILS.\n");
    }
    text.setText(out.toString());
    dialog(this, "История хранения: " + selected.title(), "Закрыть", List.of(text), () -> {});
  }

  boolean pending() {
    return this.dirty || this.conflict;
  }

  @Override
  boolean canClose() {
    if (!super.canClose()) {
      return false;
    }
    if (!this.pending()) {
      return true;
    }
    this.leave.setManaged(true);
    return false;
  }

  @Override
  void dispose() {
    super.dispose();
    this.worker.shutdownNow();
  }

  void draft(final Properties data, final boolean restore) {
    if (restore && this.opened == null) {
      this.deferred = new Properties();
      this.deferred.putAll(data);
      return;
    }
    if (!restore && this.retained != null) {
      this.retained.forEach(data::put);
      return;
    }
    final Map<String, Control> fields =
        Map.of(
            "row",
            this.row,
            "column",
            this.column,
            "reason",
            this.reason,
            "search",
            this.search,
            "state",
            this.state);
    this.loading = true;
    try {
      LaboratoryFormDraft.fields(data, "storage", restore, fields);
      if (!restore) {
        data.setProperty(
            "storage.source",
            this.opened == null ? "" : Objects.toString(this.opened.fingerprint(), ""));
        data.setProperty("storage.experiment", new LaboratoryFileCodec().write(this.expected));
        data.setProperty("storage.dirty", Boolean.toString(this.dirty));
        data.setProperty("storage.conflict", Boolean.toString(this.conflict));
        data.setProperty("storage.target", Objects.toString(this.draftTarget, ""));
        data.setProperty(
            "storage.place",
            this.destination.getValue() == null ? "" : this.destination.getValue().id());
        return;
      }
      this.dirty = Boolean.parseBoolean(data.getProperty("storage.dirty"));
      final String target = data.getProperty("storage.target", "");
      this.draftTarget = target.isEmpty() ? null : target;
      final String place = data.getProperty("storage.place", "");
      final var destination =
          this.destination.getItems().stream().filter(d -> d.id().equals(place)).findFirst();
      this.destination.setValue(destination.orElse(null));
      final var row = this.rows.stream().filter(r -> key(r).equals(this.draftTarget)).findFirst();
      this.conflict =
          !place.isEmpty() && destination.isEmpty()
              || this.draftTarget != null && row.isEmpty()
              || Boolean.parseBoolean(data.getProperty("storage.conflict"))
              || !Objects.toString(this.opened.fingerprint(), "")
                  .equals(data.getProperty("storage.source", ""))
              || !new LaboratoryFileCodec()
                  .write(this.expected)
                  .equals(data.getProperty("storage.experiment"));
      this.loading = false;
      this.filter();
      row.ifPresent(this.table.getSelectionModel()::select);
      if (this.dirty || this.conflict) {
        visible(this.movePane, true);
        this.movePane.setExpanded(true);
      }
      this.moveFields();
      if (this.conflict) {
        throw new IllegalArgumentException(
            "Восстановлен ввод другой версии. Черновик сохранён; перемещение заблокировано.");
      }
    } catch (final RuntimeException error) {
      this.conflict = true;
      this.retained = new Properties();
      data.stringPropertyNames().stream()
          .filter(k -> k.startsWith("storage."))
          .forEach(k -> this.retained.setProperty(k, data.getProperty(k)));
      this.retained.setProperty("storage.conflict", "true");
      this.status.setText(message(error));
    } finally {
      this.loading = false;
    }
  }
}

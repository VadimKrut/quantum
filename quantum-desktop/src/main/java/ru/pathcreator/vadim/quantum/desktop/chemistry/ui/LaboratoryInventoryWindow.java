/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.math.BigDecimal;
import java.util.function.Consumer;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryQuantity;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratoryBatch;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratoryInventory;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratoryStockOperation;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryInventoryRepository;

final class LaboratoryInventoryWindow {
  private final LaboratoryInventoryRepository repository = new LaboratoryInventoryRepository();
  private final Path path;
  private LaboratoryInventory inventory = LaboratoryInventory.empty();
  private LaboratoryStockConcentrations concentrations = LaboratoryStockConcentrations.unread();
  private final java.util.concurrent.ExecutorService worker =
      java.util.concurrent.Executors.newSingleThreadExecutor(
          runnable -> {
            final Thread thread = new Thread(runnable, "laboratory-stock");
            thread.setDaemon(true);
            return thread;
          });
  private java.util.concurrent.Future<?> reading;
  private boolean loaded, loading, saving, disposed;
  private int generation;
  private java.util.Properties pendingDraft;
  private String readProblem = "";
  private String fingerprint;
  private final TableView<LaboratoryBatch> batches = new TableView<>();
  private final ListView<LaboratoryStockOperation> operations = new ListView<>();
  private final TextField search = new TextField();
  private final Label quantities = new Label();
  private final Label message = new Label();
  private final Label batchDetails = new Label();
  private final CheckBox selectedBatchOnly = new CheckBox("Движения выбранной партии");
  private final CheckBox experimentOnly = new CheckBox("Только текущий опыт");
  private final Map<String, LaboratoryBatch> batchIndex = new HashMap<>();
  private final Map<String, String> materialNames = new HashMap<>();
  private final String experimentId;
  private final Consumer<LaboratoryStockOperation> recorded;
  private final BorderPane page = new BorderPane();
  private LaboratoryStockForm form;
  private java.util.Map<String, LaboratoryInventory.Stock> stock = java.util.Map.of();
  private Runnable preparation;

  void preparation(final Runnable action) {
    this.preparation = action;
  }

  private Consumer<String> solutions;

  void solutions(final Consumer<String> action) {
    this.solutions = action;
  }

  private LaboratoryMaterialReportWindow reportPage;
  private Runnable close;
  private BorderPane overview;
  private Molecule selectedMolecule;
  private final List<ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryComponent>
      components;

  LaboratoryInventoryWindow(
      final Path folder,
      final String experimentId,
      final Consumer<LaboratoryStockOperation> recorded)
      throws IOException {
    this(folder, experimentId, recorded, List.of());
  }

  LaboratoryInventoryWindow(
      final Path folder,
      final String experimentId,
      final Consumer<LaboratoryStockOperation> recorded,
      final List<ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryComponent>
          components)
      throws IOException {
    this.components = List.copyOf(components);
    this.path = folder.resolve("materials.qchem");
    this.experimentId = experimentId;
    this.recorded = recorded;
  }

  javafx.scene.Node view(
      final Window owner,
      final Molecule selectedMolecule,
      final String experimentTitle,
      final Runnable close) {
    this.close = close;
    this.selectedMolecule = selectedMolecule;
    this.page.setId("chemistry-materials-page");
    this.page.setPadding(new javafx.geometry.Insets(14));
    this.batches
        .getColumns()
        .setAll(
            List.of(
                column("Материал", batch -> this.materialName(batch)),
                column("Партия", LaboratoryBatch::label),
                this.balanceColumn(),
                this.stockColumn("Резерв", b -> this.stock.get(b.id()).reserved()),
                this.stockColumn("Свободно", b -> this.stock.get(b.id()).available()),
                column("Место", LaboratoryBatch::location),
                column("Концентрация", batch -> this.concentrations.summary(batch)),
                column("Источник", LaboratoryBatch::source)));
    this.batches.setId("chemistry-stock-batches");
    this.operations.setId("chemistry-stock-movements");
    this.search.setId("chemistry-stock-search");
    final var columns = this.batches.getColumns();
    final double[] widths = {240, 150, 115, 115, 115, 140, 180, 220};
    for (int i = 0; i < columns.size(); i++) {
      columns.get(i).setPrefWidth(widths[i]);
      columns.get(i).setMinWidth(i == 0 ? 200 : 95);
      columns.get(i).setVisible(i < 6);
    }
    this.batches.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    final CheckBox allColumns = new CheckBox("Все столбцы");
    allColumns.setId("chemistry-stock-all-columns");
    allColumns.setOnAction(
        event -> {
          this.batches.setColumnResizePolicy(
              allColumns.isSelected()
                  ? TableView.UNCONSTRAINED_RESIZE_POLICY
                  : TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
          for (int i = 6; i < columns.size(); i++) {
            columns.get(i).setVisible(allColumns.isSelected());
          }
        });
    this.batches.setPlaceholder(new Label("Добавьте приход первой партии"));
    this.search.setPromptText(
        "Материал, партия, место, поставщик; для движений — также основание и номер");
    this.search.textProperty().addListener((o, a, b) -> this.refresh());
    this.batches
        .getSelectionModel()
        .selectedItemProperty()
        .addListener((o, a, b) -> this.refreshMovements());
    this.selectedBatchOnly.selectedProperty().addListener((o, a, b) -> this.refreshMovements());
    this.experimentOnly.selectedProperty().addListener((o, a, b) -> this.refreshMovements());
    this.batchDetails.setWrapText(true);
    this.batchDetails.setId("chemistry-stock-selected-batch");
    final Button receipt = new Button("Приход / продукт…");
    receipt.setOnAction(
        event ->
            this.edit(
                LaboratoryInventoryForms.receipt(
                    this.inventory,
                    this.experimentId,
                    selectedMolecule,
                    this::save,
                    this::overview)));
    final Button report = new Button("Движения и остатки…");
    report.setId("chemistry-stock-report");
    report.setOnAction(
        event -> {
          this.reportPage = new LaboratoryMaterialReportWindow();
          this.page.setCenter(
              this.reportPage.view(
                  owner,
                  this.path.getParent(),
                  this.experimentId,
                  () -> {
                    this.reportPage = null;
                    this.overview();
                  }));
        });
    final Button reload = new Button("Обновить учёт");
    reload.setId("chemistry-stock-reload");
    reload.setOnAction(event -> this.load(null));
    final Button preparation = new Button("Подготовка / резервы…");
    preparation.setId("chemistry-stock-preparation");
    preparation.setOnAction(
        e -> {
          if (this.preparation != null) {
            this.preparation.run();
          }
        });
    final Button concentration = new Button("Концентрация / раствор…");
    concentration.setId("chemistry-stock-concentration");
    concentration
        .disableProperty()
        .bind(this.batches.getSelectionModel().selectedItemProperty().isNull());
    concentration.setOnAction(
        event -> {
          final var selected = this.batches.getSelectionModel().getSelectedItem();
          if (selected != null && this.solutions != null) {
            this.solutions.accept(selected.id());
          }
        });
    final FlowPane actions =
        new FlowPane(8, 8, receipt, preparation, concentration, report, reload, allColumns);
    for (final var kind :
        List.of(
            LaboratoryStockOperation.Kind.USE,
            LaboratoryStockOperation.Kind.RETURN,
            LaboratoryStockOperation.Kind.INCREASE,
            LaboratoryStockOperation.Kind.DECREASE)) {
      final Button button = new Button(kind + "…");
      button
          .disableProperty()
          .bind(this.batches.getSelectionModel().selectedItemProperty().isNull());
      button.setOnAction(
          event ->
              this.edit(
                  LaboratoryInventoryForms.operation(
                      this.inventory,
                      this.batches.getSelectionModel().getSelectedItem(),
                      this.experimentId,
                      kind,
                      this::save,
                      this::overview,
                      null,
                      this.components)));
      actions.getChildren().add(button);
    }
    this.operations.setCellFactory(
        list ->
            new ListCell<>() {
              {
                this.prefWidthProperty().bind(list.widthProperty().subtract(24));
                this.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
              }

              protected void updateItem(
                  final LaboratoryStockOperation operation, final boolean empty) {
                super.updateItem(operation, empty);
                final var batch =
                    operation == null
                        ? null
                        : LaboratoryInventoryWindow.this.batchIndex.get(operation.batchId());
                this.setText(
                    empty || operation == null
                        ? null
                        : LaboratoryInventoryWindow.this.materialName(batch)
                            + " · "
                            + batch.label()
                            + "\n"
                            + java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                                .withZone(java.time.ZoneId.systemDefault())
                                .format(operation.recordedAt())
                            + " · "
                            + operation.kind()
                            + " · "
                            + LaboratoryQuantityText.format(operation.quantity(), true)
                            + "\n"
                            + operation.reason()
                            + (java.util.Objects.equals(experimentId, operation.experimentId())
                                ? " · текущий опыт"
                                : operation.experimentId() == null ? "" : " · другой опыт")
                            + (operation.returnsOperationId() == null
                                ? ""
                                : " · возврат по прежнему расходу"));
                this.setWrapText(true);
                this.setTooltip(
                    empty || operation == null
                        ? null
                        : new javafx.scene.control.Tooltip(
                            operation.quantity()
                                + "\nОперация: "
                                + operation.id()
                                + "\nОпыт: "
                                + java.util.Objects.toString(operation.experimentId(), "—")
                                + "\nВозврат: "
                                + java.util.Objects.toString(operation.returnsOperationId(), "—")));
              }
            });
    final BorderPane movements = new BorderPane(this.operations);
    movements.setTop(new FlowPane(12, 8, this.selectedBatchOnly, this.experimentOnly));
    this.operations.setPlaceholder(new Label("Нет движений по выбранным условиям"));
    this.operations.setStyle("-fx-font-family: 'System'; -fx-font-size: 13px;");
    final TabPane tabs =
        new TabPane(new Tab("Партии", this.batches), new Tab("Движения", movements));
    tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    final BorderPane content = new BorderPane(tabs);
    final Label heading = new Label("Материалы и объёмы реагентов");
    heading.setStyle("-fx-font-size: 19px; -fx-font-weight: bold;");
    final Label context = new Label("Текущий опыт: " + experimentTitle);
    context.setWrapText(true);
    final Label location = new Label("Учёт проекта: " + this.path.getParent().getFileName());
    location.setTooltip(new javafx.scene.control.Tooltip(this.path.toString()));
    location.setAccessibleText(this.path.toString());
    location.setWrapText(true);
    final Button back = new Button("← К опыту");
    back.setId("chemistry-materials-back");
    back.setOnAction(event -> this.back());
    content.setTop(
        new VBox(8, new FlowPane(16, 8, back, heading), context, location, this.search, actions));
    final Label hint =
        new Label(
            "Операции сохраняются сразу. Расход относится к текущему опыту; масса, объём и моли учитываются раздельно.");
    hint.setWrapText(true);
    content.setBottom(new VBox(8, this.batchDetails, this.quantities, this.message, hint));
    this.quantities.setWrapText(true);
    this.message.setWrapText(true);
    this.overview = content;
    this.page.setCenter(content);
    this.refresh();
    this.load(null);
    return this.page;
  }

  private void edit(final LaboratoryStockForm form) {
    this.form = form;
    this.page.setCenter(form);
    form.onReload(this::reloadDraft);
  }

  private void overview() {
    this.form = null;
    this.page.setCenter(this.overview);
  }

  void back() {
    if (this.saving) {
      if (this.form != null) {
        this.form.back();
      }
      return;
    }
    if (this.loading) {
      this.generation++;
      this.loading = false;
      if (this.reading != null) {
        this.reading.cancel(true);
      }
    }
    if (this.pendingDraft != null && this.form == null) {
      this.loadingPage(
          (this.readProblem.isBlank() ? "" : this.readProblem + "\n")
              + "Ввод ожидает восстановления. Повторите чтение или явно отмените восстановление.");
      return;
    }
    if (this.form != null) {
      this.pendingDraft = null;
      this.page.setCenter(this.form);
    }
    if (this.reportPage != null) {
      this.reportPage.back();
      return;
    }
    if (this.form == null) {
      this.dispose();
      this.close.run();
    } else {
      this.form.back();
    }
  }

  boolean canClose() {
    if (this.saving || this.pendingDraft != null) {
      this.back();
      return false;
    }
    return (this.reportPage == null || this.reportPage.canClose())
        && (this.form == null || this.form.canClose());
  }

  void dispose() {
    this.disposed = true;
    this.generation++;
    if (this.reading != null) {
      this.reading.cancel(true);
    }
    if (this.reportPage != null) {
      this.reportPage.dispose();
    }
    // A durable posting must finish even if an external reload replaces this page.
    if (this.saving) {
      this.worker.shutdown();
    } else {
      this.worker.shutdownNow();
    }
  }

  boolean pending() {
    return this.pendingDraft != null || this.form != null && this.form.pending();
  }

  private void reloadDraft() {
    final java.util.Properties fields = new java.util.Properties();
    // Capture even an untouched form; its stable operation id must survive a refresh.
    this.form.capture(fields);
    fields.setProperty("stockForm.ledger", this.path.toAbsolutePath().normalize().toString());
    fields.setProperty("stockForm.experiment", this.experimentId);
    fields.setProperty("stockForm.hadLedger", Boolean.toString(this.fingerprint != null));
    this.load(fields);
  }

  private static java.util.Properties stockFields(final java.util.Properties source) {
    final var result = new java.util.Properties();
    source.stringPropertyNames().stream()
        .filter(key -> key.startsWith("stockForm."))
        .forEach(key -> result.setProperty(key, source.getProperty(key)));
    return result;
  }

  private void loadingPage(final String text) {
    if (text.startsWith("Не удалось") && !text.contains("Ввод ожидает")) {
      this.readProblem = text;
    }
    final Label heading = new Label("Учёт материалов");
    heading.setStyle("-fx-font-size: 19px; -fx-font-weight: bold;");
    final Label status = new Label(text);
    status.setWrapText(true);
    status.setId("chemistry-stock-loading-status");
    final Button retry = new Button("Повторить чтение");
    retry.setId("chemistry-stock-read-retry");
    retry.setDisable(this.loading);
    retry.setOnAction(event -> this.load(this.pendingDraft));
    final Button back = new Button("← Назад");
    back.setId("chemistry-stock-read-back");
    back.setOnAction(event -> this.back());
    final Button discard = new Button("Отменить восстановление ввода");
    discard.setId("chemistry-stock-read-discard");
    discard.setVisible(this.pendingDraft != null && this.form == null);
    discard.setManaged(discard.isVisible());
    discard.setOnAction(
        event -> {
          this.pendingDraft = null;
          this.back();
        });
    final VBox content = new VBox(12, heading, status, new FlowPane(8, 8, retry, back, discard));
    content.setPadding(new javafx.geometry.Insets(18));
    if (this.loading) {
      final var progress = new javafx.scene.control.ProgressIndicator();
      progress.setMaxSize(28, 28);
      content.getChildren().add(1, progress);
    }
    this.page.setCenter(content);
  }

  private void load(final java.util.Properties fields) {
    if (this.saving || this.disposed) {
      return;
    }
    if (fields != null) {
      this.pendingDraft = stockFields(fields);
    }
    final int requested = ++this.generation;
    if (this.reading != null) {
      this.reading.cancel(true);
    }
    this.readProblem = "";
    this.loading = true;
    this.loadingPage("Читаю партии и остатки…");
    this.reading =
        this.worker.submit(
            () -> {
              try {
                final var opened = this.repository.read(this.path);
                final var concentrations =
                    LaboratoryStockConcentrations.read(this.path.getParent());
                javafx.application.Platform.runLater(
                    () -> {
                      if (this.disposed || requested != this.generation) {
                        return;
                      }
                      this.loading = false;
                      final var previous = this.inventory;
                      final var fingerprint = this.fingerprint;
                      final boolean hadLoaded = this.loaded;
                      final var previousForm = this.form;
                      final var previousConcentrations = this.concentrations;
                      try {
                        this.inventory = opened.inventory();
                        this.fingerprint = opened.fingerprint();
                        this.concentrations = concentrations;
                        this.loaded = true;
                        this.refresh();
                        if (this.pendingDraft != null) {
                          final var draft = this.pendingDraft;
                          this.draft(draft, true);
                          this.pendingDraft = null;
                        } else {
                          this.overview();
                          this.message.setText(
                              "Учёт прочитан с диска."
                                  + (this.concentrations.problem().isBlank()
                                      ? ""
                                      : " " + this.concentrations.problem()));
                        }
                      } catch (final RuntimeException exception) {
                        this.inventory = previous;
                        this.fingerprint = fingerprint;
                        this.concentrations = previousConcentrations;
                        this.loaded = hadLoaded;
                        this.refresh();
                        this.form = previousForm;
                        if (previousForm != null) {
                          this.pendingDraft = null;
                          this.page.setCenter(previousForm);
                          previousForm.failed(
                              "Не удалось обновить учёт: " + exception.getMessage());
                        } else {
                          this.loadingPage(
                              "Не удалось восстановить ввод: " + exception.getMessage());
                        }
                      }
                    });
              } catch (final Exception exception) {
                javafx.application.Platform.runLater(
                    () -> {
                      if (this.disposed || requested != this.generation) {
                        return;
                      }
                      this.loading = false;
                      if (this.form != null) {
                        this.pendingDraft = null;
                        this.page.setCenter(this.form);
                        this.form.failed("Не удалось прочитать учёт: " + exception.getMessage());
                      } else {
                        this.loadingPage("Не удалось прочитать учёт: " + exception.getMessage());
                      }
                    });
              }
            });
  }

  void draft(final java.util.Properties fields, final boolean restore) {
    if (!restore) {
      if (this.pendingDraft != null) {
        fields.putAll(this.pendingDraft);
        return;
      }
      if (!this.pending()) {
        return;
      }
      fields.setProperty("stockForm.ledger", this.path.toAbsolutePath().normalize().toString());
      fields.setProperty("stockForm.experiment", this.experimentId);
      fields.setProperty("stockForm.hadLedger", Boolean.toString(this.fingerprint != null));
      this.form.draft(fields);
      return;
    }
    if (!this.loaded || this.loading) {
      this.pendingDraft = stockFields(fields);
      return;
    }
    if (!this.experimentId.equals(fields.getProperty("stockForm.experiment"))
        || !this.path
            .toAbsolutePath()
            .normalize()
            .equals(Path.of(fields.getProperty("stockForm.ledger")).toAbsolutePath().normalize())) {
      throw new IllegalArgumentException(
          "Складской черновик относится к другому опыту или файлу учёта.");
    }
    if (Boolean.parseBoolean(fields.getProperty("stockForm.hadLedger"))
        && this.fingerprint == null) {
      throw new IllegalArgumentException(
          "Исходный файл учёта не найден. Черновик сохранён; восстановите доступ к materials.qchem.");
    }
    final String operationId = LaboratoryStockDraft.id(fields, "operation");
    final var posted =
        this.inventory.operations().stream()
            .filter(operation -> operation.id().equals(operationId))
            .findFirst();
    if (posted.isPresent()) {
      final var operation = posted.get();
      if (!operation.batchId().equals(fields.getProperty("stockForm.batch"))
          || !operation.kind().name().equals(fields.getProperty("stockForm.kind"))) {
        throw new IllegalArgumentException(
            "Идентификатор черновика занят другой складской операцией. Повторное проведение запрещено.");
      }
      this.overview();
      this.batches.getSelectionModel().select(this.batchIndex.get(operation.batchId()));
      this.message.setText(
          "Эта операция уже проведена до сбоя: "
              + operation.kind()
              + " "
              + operation.quantity()
              + ". Повторное движение не создано. Номер: "
              + operation.id());
      try {
        this.recorded.accept(operation);
      } catch (final RuntimeException exception) {
        this.message.setText(
            this.message.getText()
                + "\nЗапись в журнал опыта не добавлена: "
                + exception.getMessage());
      }
      return;
    }
    final var kind = LaboratoryStockOperation.Kind.valueOf(fields.getProperty("stockForm.kind"));
    if (kind == LaboratoryStockOperation.Kind.RECEIPT) {
      this.edit(
          LaboratoryInventoryForms.receipt(
              this.inventory,
              this.experimentId,
              this.selectedMolecule,
              this::save,
              this::overview,
              fields));
    } else {
      final var batch = this.batchIndex.get(fields.getProperty("stockForm.batch"));
      if (batch == null) {
        throw new IllegalArgumentException("Партия восстановленной операции не найдена в учёте.");
      }
      this.edit(
          LaboratoryInventoryForms.operation(
              this.inventory,
              batch,
              this.experimentId,
              kind,
              this::save,
              this::overview,
              fields,
              this.components));
    }
  }

  private void save(final LaboratoryInventory next) {
    if (next.equals(this.inventory)) {
      return;
    }
    if (this.saving || this.loading || !this.loaded || this.disposed || this.form == null) {
      throw new IllegalStateException("Учёт ещё не готов к записи.");
    }
    final var submitted = this.form;
    final String expected = this.fingerprint;
    this.saving = true;
    submitted.defer();
    try {
      this.worker.submit(
          () -> {
            try {
              final String written = this.repository.write(this.path, next, expected);
              javafx.application.Platform.runLater(
                  () -> {
                    this.saving = false;
                    if (this.disposed) {
                      return;
                    }
                    this.inventory = next;
                    this.fingerprint = written;
                    // Durability precedes journal/UI updates. A notebook failure cannot invite
                    // another posting.
                    try {
                      this.refresh();
                      final var operation = next.operations().getLast();
                      final var batch = this.batchIndex.get(operation.batchId());
                      if (this.batches.getItems().contains(batch)) {
                        this.batches.getSelectionModel().select(batch);
                        this.batches.scrollTo(batch);
                      }
                      this.recorded.accept(operation);
                      this.message.setText("Операция сохранена.");
                    } catch (final RuntimeException exception) {
                      this.message.setText(
                          "Операция сохранена в учёте. Запись в журнал опыта не добавлена: "
                              + exception.getMessage());
                    }
                    submitted.succeeded();
                  });
            } catch (final Exception exception) {
              javafx.application.Platform.runLater(
                  () -> {
                    this.saving = false;
                    if (!this.disposed) {
                      submitted.failed(
                          "Операция не подтверждена: "
                              + exception.getMessage()
                              + " Обновите остатки, сохранив ввод, перед повторным проведением.");
                    }
                  });
            }
          });
    } catch (final RuntimeException exception) {
      this.saving = false;
      throw exception;
    }
  }

  private void refresh() {
    this.stock = this.inventory.stock();
    this.batchIndex.clear();
    this.inventory.batches().forEach(batch -> this.batchIndex.put(batch.id(), batch));
    this.materialNames.clear();
    this.inventory
        .materials()
        .forEach(material -> this.materialNames.put(material.id(), material.name()));
    final var selected = this.batches.getSelectionModel().getSelectedItem();
    final String query = this.search.getText().strip().toLowerCase(Locale.ROOT);
    this.batches
        .getItems()
        .setAll(
            this.inventory.batches().stream()
                .filter(batch -> LaboratoryTextSearch.matches(this.searchText(batch), query))
                .toList());
    if (!this.batches.getSortOrder().isEmpty()) {
      this.batches.sort();
    }
    if (selected != null && this.batches.getItems().contains(selected)) {
      this.batches.getSelectionModel().select(selected);
    }
    this.refreshMovements();
    final long used =
        this.inventory.operations().stream()
            .filter(
                item ->
                    java.util.Objects.equals(this.experimentId, item.experimentId())
                        && item.kind() == LaboratoryStockOperation.Kind.USE)
            .count();
    final long returned =
        this.inventory.operations().stream()
            .filter(
                item ->
                    java.util.Objects.equals(this.experimentId, item.experimentId())
                        && item.kind() == LaboratoryStockOperation.Kind.RETURN)
            .count();
    this.quantities.setText(
        "Показано партий: "
            + this.batches.getItems().size()
            + " из "
            + this.inventory.batches().size()
            + ". Текущий опыт: списаний — "
            + used
            + ", возвратов — "
            + returned
            + ". Количества по материалам доступны в «Движения и остатки».");
  }

  private String materialName(final LaboratoryBatch batch) {
    return this.materialNames.get(batch.materialId());
  }

  private String searchText(final LaboratoryBatch batch) {
    return (this.materialName(batch)
            + " "
            + batch.label()
            + " "
            + batch.source()
            + " "
            + batch.location())
        .toLowerCase(Locale.ROOT);
  }

  private void refreshMovements() {
    final var selected = this.batches.getSelectionModel().getSelectedItem();
    final String query = this.search.getText().strip().toLowerCase(Locale.ROOT);
    this.operations
        .getItems()
        .setAll(
            this.inventory.operations().reversed().stream()
                .filter(
                    operation ->
                        !this.selectedBatchOnly.isSelected()
                            || selected != null && selected.id().equals(operation.batchId()))
                .filter(
                    operation ->
                        !this.experimentOnly.isSelected()
                            || java.util.Objects.equals(
                                this.experimentId, operation.experimentId()))
                .filter(
                    operation ->
                        LaboratoryTextSearch.matches(
                            this.searchText(this.batchIndex.get(operation.batchId()))
                                + " "
                                + operation.reason()
                                + " "
                                + operation.id()
                                + " "
                                + java.util.Objects.toString(operation.experimentId(), ""),
                            query))
                .toList());
    if (selected == null) {
      this.batchDetails.setText(
          "Выберите партию: здесь появятся остаток, расход и возврат текущего опыта.");
      this.batchDetails.setTooltip(null);
      return;
    }
    BigDecimal used = BigDecimal.ZERO;
    BigDecimal returned = BigDecimal.ZERO;
    for (final var operation : this.inventory.operations()) {
      if (!selected.id().equals(operation.batchId())
          || !java.util.Objects.equals(this.experimentId, operation.experimentId())) {
        continue;
      }
      if (operation.kind() == LaboratoryStockOperation.Kind.USE) {
        used = used.add(operation.quantity().in(selected.unit()).value());
      }
      if (operation.kind() == LaboratoryStockOperation.Kind.RETURN) {
        returned = returned.add(operation.quantity().in(selected.unit()).value());
      }
    }
    this.batchDetails.setText(
        this.materialName(selected)
            + " · "
            + selected.label()
            + " · Остаток: "
            + LaboratoryQuantityText.format(this.inventory.balance(selected), true)
            + " · резерв: "
            + LaboratoryQuantityText.format(this.inventory.reserved(selected), true)
            + " · свободно: "
            + LaboratoryQuantityText.format(this.inventory.available(selected), true)
            + "\nТекущий опыт — выдано: "
            + LaboratoryQuantityText.format(used, selected.unit(), true)
            + "; возвращено: "
            + LaboratoryQuantityText.format(returned, selected.unit(), true)
            + "; расход за вычетом возврата: "
            + LaboratoryQuantityText.format(used.subtract(returned), selected.unit(), true)
            + "\nКонцентрация: "
            + this.concentrations.summary(selected));
    this.batchDetails.setTooltip(
        new javafx.scene.control.Tooltip(
            "Остаток: "
                + this.inventory.balance(selected)
                + "\nВыдано: "
                + LaboratoryQuantityText.format(used, selected.unit(), false)
                + "; возвращено: "
                + LaboratoryQuantityText.format(returned, selected.unit(), false)
                + "; расход: "
                + LaboratoryQuantityText.format(used.subtract(returned), selected.unit(), false)
                + "\n"
                + this.concentrations.details(selected)));
  }

  private static TableColumn<LaboratoryBatch, String> column(
      final String name, final java.util.function.Function<LaboratoryBatch, String> value) {
    final TableColumn<LaboratoryBatch, String> column = new TableColumn<>(name);
    column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
    column.setCellFactory(
        unused ->
            new TableCell<>() {
              private final Label label = new Label();

              {
                this.label.setWrapText(true);
                this.label.prefWidthProperty().bind(column.widthProperty().subtract(20));
                this.label.setMinSize(
                    javafx.scene.layout.Region.USE_PREF_SIZE,
                    javafx.scene.layout.Region.USE_PREF_SIZE);
                this.label.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                this.prefHeightProperty()
                    .bind(
                        javafx.beans.binding.Bindings.createDoubleBinding(
                            () ->
                                this.isEmpty()
                                    ? 32.0
                                    : Math.max(
                                        32,
                                        this.label.prefHeight(Math.max(1, column.getWidth() - 20))
                                            + 14),
                            this.label.textProperty(),
                            column.widthProperty(),
                            this.label.fontProperty(),
                            this.emptyProperty()));
              }

              protected void updateItem(final String item, final boolean empty) {
                super.updateItem(item, empty);
                this.setText(null);
                this.label.setText(empty || item == null ? "" : item);
                this.setGraphic(empty ? null : this.label);
                this.setTooltip(
                    empty || item == null ? null : new javafx.scene.control.Tooltip(item));
              }
            });
    return column;
  }

  private TableColumn<LaboratoryBatch, LaboratoryQuantity> stockColumn(
      final String label,
      final java.util.function.Function<LaboratoryBatch, LaboratoryQuantity> value) {
    final var column = this.balanceColumn();
    column.setText(label);
    column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
    return column;
  }

  private TableColumn<LaboratoryBatch, LaboratoryQuantity> balanceColumn() {
    final TableColumn<LaboratoryBatch, LaboratoryQuantity> column = new TableColumn<>("Остаток");
    column.setCellValueFactory(
        cell -> new ReadOnlyObjectWrapper<>(this.inventory.balance(cell.getValue())));
    // Different physical dimensions form separate groups; values within a group use exact base
    // units.
    column.setComparator(
        java.util.Comparator.comparing((LaboratoryQuantity value) -> value.unit().dimension())
            .thenComparing(value -> value.value().multiply(value.unit().factor())));
    column.setCellFactory(
        table ->
            new TableCell<>() {
              protected void updateItem(final LaboratoryQuantity value, final boolean empty) {
                super.updateItem(value, empty);
                this.setText(
                    empty || value == null ? null : LaboratoryQuantityText.format(value, true));
                this.setTooltip(
                    empty || value == null
                        ? null
                        : new javafx.scene.control.Tooltip(value.toString()));
              }
            });
    return column;
  }
}

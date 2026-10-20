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
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.*;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratoryMaterialReport;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryMaterialReportFiles;

/** Read-only reporting against an explicit folder snapshot. */
final class LaboratoryMaterialReportWindow {
  private record Choice(String id, String label) {
    public String toString() {
      return this.label;
    }
  }

  private static final Choice ALL = new Choice(null, "Все");
  private final ComboBox<Choice> project = new ComboBox<>(),
      experiment = new ComboBox<>(),
      material = new ComboBox<>(),
      batch = new ComboBox<>();
  private final ComboBox<LaboratoryMaterialReport.Group> group = new ComboBox<>();
  private final DatePicker from = new DatePicker(), through = new DatePicker();
  private final TableView<LaboratoryMaterialReport.Row> table = new TableView<>();
  private final TableView<LaboratoryMaterialReport.Balance> balances = new TableView<>();
  private final Button balanceExport = new Button("Остатки CSV…");
  private final Label summary = new Label(), message = new Label(), location = new Label();
  private final TextArea warnings = new TextArea();
  private final Button export = new Button("Сводка CSV…"), details = new Button("Операции CSV…");
  private final ZoneId zone = ZoneId.systemDefault();
  private LaboratoryMaterialReportFiles.Scan scan;
  private LaboratoryMaterialReport.Report report;
  private LaboratoryMaterialReport.Filter filter;
  private Path folder;
  private final BorderPane page = new BorderPane(), overview = new BorderPane();
  private final VBox controls = new VBox(8);
  private final java.util.concurrent.ExecutorService worker =
      java.util.concurrent.Executors.newSingleThreadExecutor(
          runnable -> {
            final Thread thread = new Thread(runnable, "laboratory-material-report");
            thread.setDaemon(true);
            return thread;
          });
  private java.util.concurrent.Future<?> task;
  private final javafx.animation.PauseTransition debounce =
      new javafx.animation.PauseTransition(javafx.util.Duration.millis(150));
  private boolean disposed, exporting;
  private Runnable close;
  private Window owner;
  private int generation;
  private boolean changing;

  javafx.scene.Node view(
      final Window owner,
      final Path initial,
      final String currentExperiment,
      final Runnable close) {
    this.folder = initial;
    this.owner = owner;
    this.close = close;
    this.page.setId("chemistry-material-report-page");
    this.page.setMinSize(0, 0);
    final Button back = new Button("← К материалам");
    back.setId("chemistry-material-report-back");
    back.setOnAction(event -> this.back());
    final Label title = new Label("Движения и остатки");
    title.setStyle("-fx-font-size: 19px; -fx-font-weight: bold;");
    this.page.setTop(new FlowPane(12, 8, back, title));
    this.page.setCenter(this.overview);
    this.debounce.setOnFinished(event -> this.calculate());
    final Button choose = new Button("Выбрать папку…"), refresh = new Button("Обновить");
    choose.setOnAction(
        event -> {
          final DirectoryChooser chooser = new DirectoryChooser();
          chooser.setTitle("Папка проектов для сводки");
          final var selected = chooser.showDialog(this.owner);
          if (selected != null) {
            this.folder = selected.toPath();
            this.load();
          }
        });
    refresh.setId("chemistry-material-report-refresh");
    refresh.setOnAction(event -> this.load());
    for (final var box : List.of(this.project, this.experiment, this.material, this.batch)) {
      box.setPrefWidth(230);
      box.setMaxWidth(Double.MAX_VALUE);
      box.setOnAction(event -> this.calculate());
    }
    this.project.setId("chemistry-material-report-project");
    this.experiment.setId("chemistry-material-report-experiment");
    this.material.setId("chemistry-material-report-material");
    this.batch.setId("chemistry-material-report-batch");
    this.group.getItems().setAll(LaboratoryMaterialReport.Group.values());
    this.group.setValue(LaboratoryMaterialReport.Group.MATERIAL);
    this.group.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(final LaboratoryMaterialReport.Group value) {
            return value == null
                ? ""
                : switch (value) {
                  case PROJECT -> "По проекту / папке";
                  case EXPERIMENT -> "По опыту";
                  case MATERIAL -> "По материалу";
                  case BATCH -> "По партии";
                  case MONTH -> "По месяцу";
                };
          }

          public LaboratoryMaterialReport.Group fromString(final String value) {
            throw new UnsupportedOperationException();
          }
        });
    this.group.setId("chemistry-material-report-group");
    this.group.setOnAction(event -> this.calculate());
    this.from.setId("chemistry-material-report-from");
    this.through.setId("chemistry-material-report-through");
    this.from.setPromptText("Без начала");
    this.through.setPromptText("Без окончания");
    final Button apply = new Button("Применить период");
    apply.setId("chemistry-material-report-period");
    apply.setOnAction(event -> this.calculate());
    for (final var date : List.of(this.from, this.through)) {
      date.setPrefWidth(150);
      date.setOnAction(
          event -> {
            if (!this.changing) {
              this.calculate();
            }
          });
      date.getEditor()
          .textProperty()
          .addListener(
              (o, a, b) -> {
                if (!this.changing && this.scan != null) {
                  this.invalidate();
                  this.debounce.playFromStart();
                }
              });
    }
    final Button current = new Button("Текущий опыт");
    current.setId("chemistry-material-report-current");
    current.setOnAction(
        event -> {
          this.experiment.setValue(
              this.experiment.getItems().stream()
                  .filter(value -> Objects.equals(value.id(), currentExperiment))
                  .findFirst()
                  .orElse(new Choice(currentExperiment, currentExperiment)));
          this.calculate();
        });
    final Button reset = new Button("Сбросить фильтры");
    reset.setId("chemistry-material-report-reset");
    reset.setOnAction(
        event -> {
          this.changing = true;
          this.from.setValue(null);
          this.through.setValue(null);
          this.from.getEditor().clear();
          this.through.getEditor().clear();
          for (final var box : List.of(this.project, this.experiment, this.material, this.batch)) {
            box.setValue(ALL);
          }
          this.changing = false;
          this.calculate();
        });
    final FlowPane filters =
        new FlowPane(
            10,
            8,
            field("Проект / папка", this.project),
            field("Опыт", this.experiment),
            field("Материал", this.material),
            field("Партия", this.batch));
    final FlowPane period =
        new FlowPane(
            8,
            8,
            new Label("С"),
            this.from,
            new Label("по включительно"),
            this.through,
            apply,
            current,
            reset);
    this.table.setId("chemistry-material-report-table");
    this.table.setPlaceholder(new Label("Нет движений по выбранным условиям"));
    this.table
        .getColumns()
        .setAll(
            List.of(
                column("Проект", row -> this.projectLabel(row.project())),
                column("Группа", row -> this.groupLabel(row)),
                column("ID группы", LaboratoryMaterialReport.Row::groupId),
                column("Приход", row -> number(row.received())),
                column("Взято", row -> number(row.used())),
                column("Возврат", row -> number(row.returned())),
                column("Чистый расход", row -> number(row.netUsed())),
                column("Корр. +", row -> number(row.increased())),
                column("Корр. −", row -> number(row.decreased())),
                column("Изменение", row -> number(row.stockChange())),
                column("Ед.", row -> row.unit().toString()),
                column("Операций", row -> Integer.toString(row.operations()))));
    this.balances.setId("chemistry-material-report-balances");
    this.balances.setPlaceholder(new Label("Нет партий на конец выбранного периода"));
    this.balances
        .getColumns()
        .setAll(
            List.of(
                balanceColumn("Проект", row -> this.projectLabel(row.project())),
                balanceColumn(
                    "Материал", row -> this.contextLabel(row.project(), row.material().name())),
                balanceColumn("Партия", row -> row.batch().label()),
                balanceColumn("Место", row -> row.batch().location()),
                balanceColumn("На начало", row -> number(row.opening())),
                balanceColumn("На конец", row -> number(row.closing())),
                balanceColumn("Ед.", row -> row.unit().toString())));
    for (int index = 3; index < this.table.getColumns().size(); index++) {
      if (index != 10) {
        this.table
            .getColumns()
            .get(index)
            .setComparator(
                (a, b) -> new BigDecimal(a.toString()).compareTo(new BigDecimal(b.toString())));
      }
    }
    for (final int index : List.of(4, 5)) {
      this.balances
          .getColumns()
          .get(index)
          .setComparator(
              (a, b) -> new BigDecimal(a.toString()).compareTo(new BigDecimal(b.toString())));
    }
    this.table.getColumns().get(1).setMinWidth(180);
    this.table.getColumns().get(6).setMinWidth(115);
    this.table.setMinHeight(120);
    this.balances.setMinHeight(120);
    final CheckBox allColumns = new CheckBox("Все столбцы");
    allColumns.setId("chemistry-material-report-all-columns");
    allColumns.setOnAction(event -> this.columns(allColumns.isSelected()));
    this.columns(false);
    this.table.getColumns().get(2).setVisible(false);
    this.table.getColumns().get(1).setPrefWidth(250);
    this.table.setTableMenuButtonVisible(true);
    this.table.getSelectionModel().setCellSelectionEnabled(true);
    this.table.setOnKeyPressed(
        event -> {
          if (event.isShortcutDown() && event.getCode() == javafx.scene.input.KeyCode.C) {
            final var row = this.table.getSelectionModel().getSelectedItem();
            if (row != null) {
              final var clipboard = new javafx.scene.input.ClipboardContent();
              clipboard.putString(
                  row.project()
                      + "\t"
                      + row.label()
                      + "\t"
                      + number(row.netUsed())
                      + " "
                      + row.unit());
              javafx.scene.input.Clipboard.getSystemClipboard().setContent(clipboard);
              event.consume();
            }
          }
        });
    this.export.setOnAction(event -> this.export(LaboratoryMaterialReportFiles.Export.SUMMARY));
    this.details.setOnAction(event -> this.export(LaboratoryMaterialReportFiles.Export.OPERATIONS));
    this.balanceExport.setOnAction(
        event -> this.export(LaboratoryMaterialReportFiles.Export.BALANCES));
    this.export.setId("chemistry-material-report-export");
    this.details.setId("chemistry-material-report-export-operations");
    this.balanceExport.setId("chemistry-material-report-export-balances");
    final Button explain = new Button("Операции группы →");
    explain.setId("chemistry-material-report-explain");
    explain.disableProperty().bind(this.table.getSelectionModel().selectedItemProperty().isNull());
    explain.setOnAction(event -> this.explain());
    this.table.setRowFactory(
        table -> {
          final TableRow<LaboratoryMaterialReport.Row> row = new TableRow<>();
          row.setOnMouseClicked(
              event -> {
                if (event.getClickCount() == 2
                    && event.getButton() == javafx.scene.input.MouseButton.PRIMARY
                    && !row.isEmpty()) {
                  table.getSelectionModel().select(row.getItem());
                  this.explain();
                  event.consume();
                }
              });
          return row;
        });
    this.table.addEventHandler(
        javafx.scene.input.KeyEvent.KEY_PRESSED,
        event -> {
          if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
            this.explain();
            event.consume();
          }
        });
    this.summary.setWrapText(true);
    this.summary.setId("chemistry-material-report-summary");
    this.message.setWrapText(true);
    this.message.setId("chemistry-material-report-message");
    this.location.setWrapText(true);
    this.warnings.setEditable(false);
    this.warnings.setWrapText(true);
    this.warnings.setPrefRowCount(4);
    final TitledPane issues = new TitledPane("Диагностика чтения папки", this.warnings);
    issues.setExpanded(false);
    final Label explanation =
        new Label(
            "Масса — г; объём — мл; количество вещества — ммоль. Между размерностями пересчёта нет. Остатки рассчитаны по всем движениям партии; фильтр опыта действует только на вкладку движений. Без начальной даты остаток на начало равен нулю. Поздний возврат относится к периоду возврата. Часовой пояс: "
                + this.zone);
    explanation.setWrapText(true);
    final TabPane tabs =
        new TabPane(
            new Tab("Движения за период", this.table), new Tab("Остатки партий", this.balances));
    tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    tabs.setId("chemistry-material-report-tabs");
    final ScrollPane filterScroll = new ScrollPane(new VBox(8, filters, period));
    filterScroll.setFitToWidth(true);
    filterScroll.setPrefViewportHeight(170);
    filterScroll.setMaxHeight(190);
    final TitledPane filterPane = new TitledPane("Фильтры и период", filterScroll);
    filterPane.setId("chemistry-material-report-filters");
    filterPane.setExpanded(false);
    filterPane.setAnimated(false);
    final TitledPane help = new TitledPane("Как считаются количества", explanation);
    help.setExpanded(false);
    help.setAnimated(false);
    final FlowPane actions = new FlowPane(8, 8, refresh, current, reset, choose);
    final FlowPane exports = new FlowPane(8, 8, this.export, this.details, this.balanceExport);
    this.controls
        .getChildren()
        .setAll(
            actions,
            filterPane,
            new FlowPane(8, 8, field("Группировка", this.group), allColumns, explain),
            exports);
    final VBox header = new VBox(6, this.location, this.controls);
    final ScrollPane headerScroll = new ScrollPane(header);
    headerScroll.setId("chemistry-material-report-controls");
    headerScroll.setFitToWidth(true);
    headerScroll.setMinHeight(0);
    headerScroll
        .prefHeightProperty()
        .bind(
            javafx.beans.binding.Bindings.createDoubleBinding(
                () ->
                    Math.min(
                        header.prefHeight(Math.max(100, this.overview.getWidth() - 20)) + 4,
                        Math.max(120, this.overview.getHeight() * .50)),
                this.overview.widthProperty(),
                this.overview.heightProperty(),
                filterPane.expandedProperty()));
    this.overview.setTop(headerScroll);
    this.overview.setCenter(tabs);
    final VBox foot = new VBox(5, this.summary, this.message, issues, help);
    final ScrollPane footer = new ScrollPane(foot);
    footer.setFitToWidth(true);
    footer.setMinHeight(0);
    footer
        .prefHeightProperty()
        .bind(
            javafx.beans.binding.Bindings.createDoubleBinding(
                () ->
                    Math.min(
                        foot.prefHeight(Math.max(100, this.overview.getWidth() - 20)) + 4,
                        Math.max(90, this.overview.getHeight() * .26)),
                this.overview.widthProperty(),
                this.overview.heightProperty(),
                issues.expandedProperty(),
                help.expandedProperty(),
                this.message.textProperty(),
                this.summary.textProperty()));
    this.overview.setBottom(footer);
    this.load();
    return this.page;
  }

  private void columns(final boolean all) {
    final Set<Integer> compact = Set.of(1, 4, 5, 6, 10);
    for (int i = 0; i < this.table.getColumns().size(); i++) {
      final var column = this.table.getColumns().get(i);
      column.setVisible(all || compact.contains(i));
      column.setPrefWidth(i == 1 ? 230 : 115);
      column.setMinWidth(i == 1 ? 160 : 80);
    }
    this.table.setColumnResizePolicy(
        all
            ? TableView.UNCONSTRAINED_RESIZE_POLICY
            : TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    for (int i = 0; i < this.balances.getColumns().size(); i++) {
      final var column = this.balances.getColumns().get(i);
      column.setVisible(all || i != 0 && i != 3 && i != 4);
      column.setPrefWidth(i == 1 ? 230 : 120);
      column.setMinWidth(i == 1 ? 160 : 80);
    }
    this.balances.setColumnResizePolicy(
        all
            ? TableView.UNCONSTRAINED_RESIZE_POLICY
            : TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
  }

  void back() {
    if (this.canClose()) {
      this.dispose();
      this.close.run();
    }
  }

  boolean canClose() {
    if (this.exporting) {
      this.message.setText("Дождитесь завершения сохранения CSV.");
      return false;
    }
    return true;
  }

  void dispose() {
    this.disposed = true;
    this.generation++;
    this.debounce.stop();
    if (this.task != null) {
      this.task.cancel(true);
    }
    this.worker.shutdownNow();
  }

  private void invalidate() {
    this.generation++;
    if (this.task != null) {
      this.task.cancel(true);
    }
    this.report = null;
    this.table.getItems().clear();
    this.balances.getItems().clear();
    this.balanceExport.setDisable(true);
    this.export.setDisable(true);
    this.details.setDisable(true);
  }

  private <T> void submit(
      final java.util.concurrent.Callable<T> work, final java.util.function.Consumer<T> done) {
    final int requested = ++this.generation;
    if (this.task != null) {
      this.task.cancel(true);
    }
    this.task =
        this.worker.submit(
            () -> {
              try {
                final T result = work.call();
                Platform.runLater(
                    () -> {
                      if (!this.disposed && requested == this.generation) {
                        done.accept(result);
                      }
                    });
              } catch (final Exception exception) {
                Platform.runLater(
                    () -> {
                      if (!this.disposed && requested == this.generation) {
                        this.exporting = false;
                        this.controls.setDisable(false);
                        this.message.setText(
                            "Не удалось выполнить действие: " + exception.getMessage());
                        this.summary.setText(
                            this.report == null ? "Сводка недоступна" : this.summary.getText());
                      }
                    });
              }
            });
  }

  private void load() {
    if (this.disposed || this.exporting) {
      return;
    }
    this.debounce.stop();
    this.invalidate();
    this.scan = null;
    this.summary.setText("Читаю папку и подпапки…");
    this.message.setText("");
    this.warnings.clear();
    final Path requestedFolder = this.folder;
    this.location.setText(
        "Учёт: " + Objects.toString(requestedFolder.getFileName(), requestedFolder.toString()));
    this.location.setTooltip(new Tooltip(requestedFolder.toAbsolutePath().normalize().toString()));
    this.submit(
        () -> LaboratoryMaterialReportFiles.scan(requestedFolder),
        loaded -> {
          this.scan = loaded;
          this.changing = true;
          final Map<String, String> projects = new TreeMap<>(),
              experiments = new TreeMap<>(),
              materials = new TreeMap<>(),
              batches = new TreeMap<>();
          loaded
              .sources()
              .forEach(
                  source -> {
                    projects.put(source.project(), this.projectLabel(source.project()));
                    source
                        .inventory()
                        .materials()
                        .forEach(item -> materials.put(item.id(), item.name()));
                    source
                        .inventory()
                        .batches()
                        .forEach(item -> batches.put(item.id(), item.label()));
                    source
                        .inventory()
                        .operations()
                        .forEach(
                            item -> {
                              if (item.experimentId() != null) {
                                experiments.merge(
                                    item.experimentId(),
                                    loaded.experiment(source.project(), item.experimentId()),
                                    (a, b) -> b.equals(item.experimentId()) ? a : b);
                              }
                            });
                  });
          choices(this.project, projects);
          choices(this.experiment, experiments);
          choices(this.material, materials);
          choices(this.batch, batches);
          this.changing = false;
          this.calculate();
        });
  }

  private void calculate() {
    if (this.changing || this.scan == null || this.disposed || this.exporting) {
      return;
    }
    this.debounce.stop();
    this.invalidate();
    try {
      this.changing = true;
      try {
        this.from.commitValue();
        this.through.commitValue();
      } finally {
        this.changing = false;
      }
      final var requestedFilter =
          new LaboratoryMaterialReport.Filter(
              this.from.getValue() == null
                  ? null
                  : this.from.getValue().atStartOfDay(this.zone).toInstant(),
              this.through.getValue() == null
                  ? null
                  : this.through.getValue().plusDays(1).atStartOfDay(this.zone).toInstant(),
              id(this.project),
              id(this.experiment),
              id(this.material),
              id(this.batch));
      final var snapshot = this.scan;
      final var grouping = this.group.getValue();
      this.summary.setText("Считаю движения и остатки…");
      this.message.setText("");
      this.submit(
          () ->
              LaboratoryMaterialReport.summarize(
                  snapshot.sources(), requestedFilter, grouping, this.zone),
          result -> {
            this.filter = requestedFilter;
            this.report = result;
            this.table.getItems().setAll(result.rows());
            this.balances.getItems().setAll(result.balances());
            this.balanceExport.setDisable(result.balances().isEmpty());
            this.export.setDisable(result.entries().isEmpty());
            this.details.setDisable(result.entries().isEmpty());
            final List<String> issues = new ArrayList<>(snapshot.warnings());
            issues.addAll(result.warnings());
            this.warnings.setText(
                issues.isEmpty()
                    ? "Ошибок чтения и дат движений нет. Скрытые папки, история и вложения не сканируются."
                    : String.join("\n", issues));
            this.summary.setText(
                (issues.isEmpty() ? "" : "НЕПОЛНАЯ СВОДКА · ")
                    + "Движений: "
                    + result.entries().size()
                    + " · партий: "
                    + result.balances().size()
                    + " · файлов учёта: "
                    + snapshot.sources().size());
            this.message.setText(
                (requestedFilter.experiment() == null
                        ? ""
                        : "Движения выбранного опыта. Остатки — по всем опытам. ")
                    + (issues.isEmpty()
                        ? "Для новых операций нажмите «Обновить»."
                        : "Есть пропуски или ошибки — раскройте диагностику."));
          });
    } catch (final RuntimeException exception) {
      this.changing = false;
      this.summary.setText("Проверьте фильтры и даты");
      this.message.setText("Период не применён: " + exception.getMessage());
    }
  }

  private void export(final LaboratoryMaterialReportFiles.Export kind) {
    if (this.report == null || this.exporting) {
      return;
    }
    final FileChooser chooser = new FileChooser();
    chooser.setTitle("Экспорт количественного отчёта");
    chooser.setInitialFileName("material-" + kind.name().toLowerCase(Locale.ROOT) + ".csv");
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV UTF-8", "*.csv"));
    final var selected = chooser.showSaveDialog(this.owner);
    if (selected != null) {
      this.exportTo(selected.toPath(), kind);
    }
  }

  void exportTo(final Path target, final LaboratoryMaterialReportFiles.Export kind) {
    if (this.report == null || this.exporting || this.disposed) {
      return;
    }
    final var snapshot = this.scan;
    final var result = this.report;
    final var selectedFilter = this.filter;
    final var grouping = this.group.getValue();
    this.exporting = true;
    this.controls.setDisable(true);
    this.message.setText("Сохраняю CSV…");
    this.submit(
        () -> {
          LaboratoryMaterialReportFiles.export(
              target, snapshot, result, selectedFilter, grouping, this.zone, kind);
          return target;
        },
        saved -> {
          this.exporting = false;
          this.controls.setDisable(false);
          this.message.setText("Отчёт сохранён: " + saved.getFileName());
          this.message.setTooltip(new Tooltip(saved.toString()));
        });
  }

  private void explain() {
    final var row = this.table.getSelectionModel().getSelectedItem();
    if (row == null || this.report == null || this.exporting || this.disposed) {
      return;
    }
    final var snapshot = this.scan;
    final var result = this.report;
    final var grouping = this.group.getValue();
    final Button back = new Button("← К сводке");
    back.setId("chemistry-material-report-detail-back");
    back.setOnAction(
        event -> {
          this.generation++;
          if (this.task != null) {
            this.task.cancel(true);
          }
          this.page.setCenter(this.overview);
        });
    final Label title = new Label(this.groupLabel(row) + " · " + row.unit());
    title.setWrapText(true);
    final TextArea area = new TextArea("Подготавливаю исходные операции…");
    area.setEditable(false);
    area.setWrapText(true);
    area.setId("chemistry-material-report-provenance");
    final BorderPane detail = new BorderPane(area);
    detail.setTop(new VBox(8, back, title));
    this.page.setCenter(detail);
    this.submit(
        () -> {
          final StringBuilder text =
              new StringBuilder(row.project())
                  .append("\n")
                  .append(row.label())
                  .append(" · ")
                  .append(row.groupId())
                  .append("\n");
          for (final var entry :
              LaboratoryMaterialReport.entriesFor(result, row, grouping, this.zone)) {
            if (Thread.currentThread().isInterrupted()) {
              throw new java.util.concurrent.CancellationException("Чтение операций отменено.");
            }
            final var operation = entry.operation();
            text.append("\n")
                .append(operation.recordedAt().atZone(this.zone))
                .append(" · ")
                .append(operation.kind())
                .append("\n")
                .append(entry.material().name())
                .append(" · партия ")
                .append(entry.batch().label())
                .append(" [")
                .append(entry.batch().id())
                .append("]\n")
                .append("Опыт: ")
                .append(snapshot.experiment(entry.project(), operation.experimentId()))
                .append(" [")
                .append(operation.experimentId())
                .append("]\n")
                .append("Количество: ")
                .append(number(entry.quantity()))
                .append(' ')
                .append(entry.unit())
                .append("\n")
                .append("Изменение остатка: ")
                .append(number(entry.stockChange()))
                .append(' ')
                .append(entry.unit())
                .append("\n")
                .append("Операция: ")
                .append(operation.id())
                .append("\n");
            if (operation.returnsOperationId() != null) {
              text.append("Возврат расхода: ").append(operation.returnsOperationId()).append("\n");
            }
            text.append("Основание: ").append(operation.reason()).append("\n");
          }
          return text.toString();
        },
        area::setText);
  }

  private String contextLabel(final String project, final String label) {
    return this.scan != null && this.scan.sources().size() > 1
        ? this.projectLabel(project) + "\n" + label
        : label;
  }

  private String groupLabel(final LaboratoryMaterialReport.Row row) {
    return this.contextLabel(
        row.project(),
        this.group.getValue() == LaboratoryMaterialReport.Group.EXPERIMENT
            ? this.scan.experiment(row.project(), row.groupId())
            : row.label());
  }

  private String projectLabel(final String value) {
    final Path path = Path.of(value);
    final Path relative = this.scan.root().relativize(path);
    return relative.toString().isEmpty()
        ? Objects.toString(path.getFileName(), path.toString())
        : relative.toString();
  }

  private static String id(final ComboBox<Choice> box) {
    return box.getValue() == null ? null : box.getValue().id();
  }

  private static void choices(final ComboBox<Choice> box, final Map<String, String> values) {
    final Map<String, Integer> counts = new HashMap<>();
    values.values().forEach(label -> counts.merge(label, 1, Integer::sum));
    final String selected = id(box);
    box.getItems().setAll(ALL);
    values.forEach(
        (id, label) ->
            box.getItems().add(new Choice(id, counts.get(label) > 1 ? label + " · " + id : label)));
    box.setValue(
        box.getItems().stream()
            .filter(item -> Objects.equals(item.id(), selected))
            .findFirst()
            .orElse(ALL));
  }

  private static VBox field(final String title, final javafx.scene.Node node) {
    return new VBox(4, new Label(title), node);
  }

  private static String number(final BigDecimal value) {
    return value.stripTrailingZeros().toPlainString();
  }

  private static TableColumn<LaboratoryMaterialReport.Row, String> column(
      final String name,
      final java.util.function.Function<LaboratoryMaterialReport.Row, String> value) {
    final TableColumn<LaboratoryMaterialReport.Row, String> column = new TableColumn<>(name);
    column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
    column.setCellFactory(
        table ->
            new TableCell<>() {
              protected void updateItem(final String text, final boolean empty) {
                super.updateItem(text, empty);
                this.setText(empty ? null : text);
                this.setTooltip(empty || text == null ? null : new Tooltip(text));
              }
            });
    return column;
  }

  private static TableColumn<LaboratoryMaterialReport.Balance, String> balanceColumn(
      final String name,
      final java.util.function.Function<LaboratoryMaterialReport.Balance, String> value) {
    final TableColumn<LaboratoryMaterialReport.Balance, String> column = new TableColumn<>(name);
    column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
    column.setCellFactory(
        table ->
            new TableCell<>() {
              protected void updateItem(final String text, final boolean empty) {
                super.updateItem(text, empty);
                this.setText(empty ? null : text);
                this.setTooltip(empty || text == null ? null : new Tooltip(text));
              }
            });
    return column;
  }
}

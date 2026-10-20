/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryMoleculePlacement;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.*;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratorySampleCatalog.*;

/** Folder-level sample navigation and label batches, backed by the existing experiment records. */
final class LaboratorySampleCatalogPage extends LaboratoryComputationPage {
  private final Path folder;
  private final Supplier<Current> current;
  private final BiConsumer<Row, String> open;
  private final ExecutorService worker =
      Executors.newFixedThreadPool(
          2,
          r -> {
            final Thread t = new Thread(r, "sample-catalog");
            t.setDaemon(true);
            return t;
          });
  private Future<?> scanning, preparing;
  private int scanVersion, labelVersion;
  private boolean scanningNow, labelNow, exporting, inspecting;
  private Snapshot snapshot;
  private LaboratorySampleLabels.Bundle bundle;
  private int pageIndex;
  private final TableView<Row> table = new TableView<>();
  private final TextField search = new TextField(),
      location = new TextField(),
      scanner = new TextField();
  private final ComboBox<String> state = new ComboBox<>(),
      method = new ComboBox<>(),
      kind = new ComboBox<>();
  private final Label summary = note("Читаю образцы…"),
      labelSummary = note("Выберите образцы в каталоге."),
      pageLabel = note("");
  private final TextArea diagnostics = new TextArea();
  private final VBox card = new VBox(10), list = new VBox(8);
  private final ScrollPane cardScroll = new ScrollPane(this.card);
  private final HBox split = new HBox(14);
  private final TabPane tabs = new TabPane();
  private final Tab samplesTab = new Tab("Образцы"), labelsTab = new Tab("Этикетки");
  private final Button refresh,
      stop,
      inspect,
      back,
      openSample,
      savedAnalyses,
      csv,
      labelButton,
      generate,
      exportPdf,
      previous,
      next;
  private final MenuButton more = new MenuButton("Ещё");
  private Button selectAll;
  private ScrollPane headingScroll;
  private final ToggleButton fitPage = new ToggleButton("Вписать страницу");
  private final ImageView preview = new ImageView();
  private final ComboBox<LaboratorySampleLabels.Format> format = new ComboBox<>();
  private final TextField copies = new TextField("1"), skip = new TextField("0");

  LaboratorySampleCatalogPage(
      final Path folder,
      final Supplier<Current> current,
      final BiConsumer<Row, String> open,
      final Runnable close) {
    super("Образцы проекта", close);
    this.folder = folder.toAbsolutePath().normalize();
    this.current = current;
    this.open = open;
    this.setId("chemistry-sample-catalog");
    this.setMinSize(0, 0);
    this.search.setPromptText("Вещество, код, формула, опыт…");
    this.search.setPrefWidth(310);
    this.search.setId("chemistry-catalog-search");
    this.scanner.setPromptText("Код / QR → Enter");
    this.scanner.setPrefWidth(245);
    this.scanner.setId("chemistry-catalog-scanner");
    this.scanner.setOnAction(e -> this.resolve());
    this.scanner.setTooltip(
        new Tooltip(
            "Поле для сканера в режиме клавиатуры. Завершение ввода — Enter. Ctrl+L — фокус."));
    this.location.setPromptText("Место или контейнер");
    this.location.setPrefWidth(220);
    this.location.setId("chemistry-catalog-location");
    this.state
        .getItems()
        .setAll(
            "Все состояния",
            "В работе",
            "Остаток не измерен",
            "Копии ссылок / повтор кода",
            "Архив и завершённые");
    this.state.getSelectionModel().selectFirst();
    this.state.setId("chemistry-catalog-state");
    this.method
        .getItems()
        .setAll(
            "Любая аналитика",
            "Без собственных анализов",
            "ЯМР",
            "МС",
            "ТСХ",
            "Хроматография",
            "Досье");
    this.method.getSelectionModel().selectFirst();
    this.method.setId("chemistry-catalog-method");
    this.kind.getItems().add("Все виды");
    Arrays.stream(LaboratorySample.Kind.values())
        .map(Object::toString)
        .forEach(this.kind.getItems()::add);
    this.kind.getSelectionModel().selectFirst();
    this.kind.setId("chemistry-catalog-kind");
    this.search.textProperty().addListener(o -> this.filter());
    this.location.textProperty().addListener(o -> this.filter());
    this.state.valueProperty().addListener(o -> this.filter());
    this.method.valueProperty().addListener(o -> this.filter());
    this.kind.valueProperty().addListener(o -> this.filter());
    this.refresh = action("Обновить", "refresh", this::load);
    this.stop = action("Остановить", "stop", this::stop);
    final TitledPane filters =
        new TitledPane(
            "Фильтры",
            new FlowPane(
                8,
                8,
                this.location,
                this.state,
                this.method,
                this.kind,
                action("Сбросить", "clear", this::clearFilters)));
    filters.setExpanded(false);
    filters.setAnimated(false);
    filters.setId("chemistry-catalog-filters");
    this.search.setMinWidth(0);
    this.scanner.setMinWidth(0);
    HBox.setHgrow(this.search, Priority.ALWAYS);
    HBox.setHgrow(this.scanner, Priority.ALWAYS);
    final HBox find =
        new HBox(8, this.search, this.scanner, action("Найти", "resolve", this::resolve));
    final VBox heading = new VBox(8, find, filters, this.summary);
    heading.setPadding(new Insets(8, 12, 8, 12));
    this.headingScroll = new ScrollPane(heading);
    this.headingScroll.setFitToWidth(true);
    this.headingScroll.setMinHeight(0);
    this.headingScroll
        .prefViewportHeightProperty()
        .bind(
            javafx.beans.binding.Bindings.min(
                heading.heightProperty(), this.heightProperty().multiply(.32)));
    this.table.setId("chemistry-catalog-table");
    this.table.setMinSize(0, 80);
    this.table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    this.table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    this.table
        .getColumns()
        .addAll(
            column(
                "Образец / вещество",
                r ->
                    r.sample().code()
                        + (r.unsaved() ? " *" : "")
                        + (r.copies() > 1 ? " · копия" : "")
                        + "\n"
                        + r.sample().molecule().displayName(),
                230),
            column("Остаток", r -> r.quantity() + "\n" + r.balance().status(), 155),
            column("Хранение", r -> r.balance().location() + "\n" + r.balance().container(), 165),
            column(
                "Опыт / анализы",
                r ->
                    r.experimentTitle()
                        + "\n"
                        + r.analyses().size()
                        + " собственных записей"
                        + (r.sample().origin() != null || !r.sample().parents().isEmpty()
                            ? " · есть происхождение"
                            : ""),
                190));
    this.table.setPlaceholder(note("Образцы не найдены. Проверьте фильтры и выбранную папку."));
    this.table.getSelectionModel().selectedItemProperty().addListener(o -> this.details());
    this.table
        .getSelectionModel()
        .getSelectedItems()
        .addListener(
            (javafx.collections.ListChangeListener<Row>)
                change -> {
                  this.details();
                  this.invalidateLabels();
                  this.controls();
                });
    this.table.setRowFactory(
        v -> {
          final TableRow<Row> row = new TableRow<>();
          row.setOnMouseClicked(
              e -> {
                if (!row.isEmpty()
                    && e.getButton() == MouseButton.PRIMARY
                    && e.getClickCount() == 2) {
                  this.inspecting = true;
                  this.arrange();
                }
              });
          return row;
        });
    this.table.setOnKeyPressed(
        e -> {
          if (e.getCode() == KeyCode.ENTER
              && this.table.getSelectionModel().getSelectedItems().size() == 1) {
            this.openSelected("SAMPLE");
            e.consume();
          }
        });
    this.diagnostics.setEditable(false);
    this.diagnostics.setWrapText(true);
    this.diagnostics.setPrefRowCount(4);
    final TitledPane issues =
        new TitledPane(
            "Состояние чтения и папка",
            new VBox(8, note(this.folder.toString()), this.diagnostics));
    issues.setExpanded(false);
    issues.setAnimated(false);
    this.list.getChildren().setAll(this.table, issues);
    this.list.setMinSize(0, 0);
    VBox.setVgrow(this.table, Priority.ALWAYS);
    this.list.setPrefWidth(740);
    this.card.setPadding(new Insets(14));
    this.card.getStyleClass().add("laboratory-search-card");
    this.cardScroll.setFitToWidth(true);
    this.cardScroll.setMinSize(0, 0);
    this.cardScroll.setPrefWidth(430);
    this.split.setMinSize(0, 0);
    HBox.setHgrow(this.list, Priority.ALWAYS);
    HBox.setHgrow(this.cardScroll, Priority.ALWAYS);
    this.split.setPadding(new Insets(0, 12, 8, 12));
    this.inspect =
        action(
            "Карточка",
            "inspect",
            () -> {
              this.inspecting = true;
              this.arrange();
            });
    this.back =
        action(
            "← К списку",
            "list",
            () -> {
              this.inspecting = false;
              this.arrange();
            });
    this.openSample = action("Открыть образец", "open", () -> this.openSelected("SAMPLE"));
    this.openSample.getStyleClass().add("laboratory-save");
    this.savedAnalyses =
        action("Все анализы образца", "analyses", () -> this.openSelected("RESULTS"));
    this.csv = action("CSV показанных…", "csv", this::exportCsv);
    this.labelButton =
        action(
            "Этикетки…",
            "labels",
            () -> {
              this.tabs.getSelectionModel().select(this.labelsTab);
              this.generate();
            });
    this.selectAll =
        action(
            "Выделить показанные", "select-all", () -> this.table.getSelectionModel().selectAll());
    for (final Button button : List.of(this.selectAll, this.csv, this.refresh, this.stop)) {
      final MenuItem item = new MenuItem(button.getText());
      item.disableProperty().bind(button.disableProperty());
      item.setOnAction(e -> button.fire());
      this.more.getItems().add(item);
    }
    this.more.setId("chemistry-catalog-more");
    final FlowPane actions =
        new FlowPane(
            8,
            6,
            this.back,
            this.inspect,
            this.openSample,
            this.savedAnalyses,
            this.labelButton,
            this.selectAll,
            this.csv,
            this.refresh,
            this.stop,
            this.more);
    actions.setPadding(new Insets(8, 12, 10, 12));
    final BorderPane sampleBody =
        new BorderPane(this.split, this.headingScroll, null, actions, null);
    this.samplesTab.setContent(sampleBody);
    this.format.getItems().setAll(LaboratorySampleLabels.Format.values());
    this.format.setValue(LaboratorySampleLabels.Format.A4);
    this.format.setId("chemistry-catalog-label-format");
    this.copies.setPrefColumnCount(3);
    this.skip.setPrefColumnCount(3);
    this.copies.setId("chemistry-catalog-copies");
    this.skip.setId("chemistry-catalog-skip");
    this.format
        .valueProperty()
        .addListener(
            o -> {
              if (this.format.getValue() == LaboratorySampleLabels.Format.ROLL) {
                this.skip.setText("0");
              }
              this.skip.setDisable(this.format.getValue() == LaboratorySampleLabels.Format.ROLL);
              this.invalidateLabels();
            });
    this.copies.textProperty().addListener(o -> this.invalidateLabels());
    this.skip.textProperty().addListener(o -> this.invalidateLabels());
    this.generate = action("Сформировать", "generate", this::generate);
    this.exportPdf = action("Сохранить PDF…", "export-pdf", this::exportPdf);
    this.previous = action("←", "previous", () -> this.page(this.pageIndex - 1));
    this.next = action("→", "next", () -> this.page(this.pageIndex + 1));
    this.preview.setPreserveRatio(true);
    this.preview.setSmooth(true);
    this.preview.setId("chemistry-catalog-preview");
    this.fitPage.setSelected(true);
    this.fitPage.setId("chemistry-catalog-fit");
    this.fitPage.setTooltip(
        new Tooltip("Вся страница целиком; выключите для увеличения по ширине."));
    final StackPane paper = new StackPane(this.preview);
    paper.setPadding(new Insets(12));
    paper.setMinSize(0, 0);
    paper.getStyleClass().add("catalog-paper");
    final ScrollPane previewScroll = new ScrollPane(paper);
    previewScroll.setFitToWidth(true);
    previewScroll.fitToHeightProperty().bind(this.fitPage.selectedProperty());
    previewScroll.setPannable(true);
    previewScroll.setMinSize(0, 0);
    this.preview
        .fitWidthProperty()
        .bind(
            javafx.beans.binding.Bindings.createDoubleBinding(
                () -> Math.max(1, Math.min(900, previewScroll.getViewportBounds().getWidth() - 28)),
                previewScroll.viewportBoundsProperty()));
    this.preview
        .fitHeightProperty()
        .bind(
            javafx.beans.binding.Bindings.createDoubleBinding(
                () ->
                    this.fitPage.isSelected()
                        ? Math.max(1, previewScroll.getViewportBounds().getHeight() - 28)
                        : 0,
                previewScroll.viewportBoundsProperty(),
                this.fitPage.selectedProperty()));
    final VBox labelTop =
        new VBox(
            8,
            new FlowPane(
                8, 6, this.format, note("Копии"), this.copies, note("Пропустить мест"), this.skip),
            this.labelSummary,
            new FlowPane(8, 6, this.previous, this.pageLabel, this.next, this.fitPage));
    labelTop.setPadding(new Insets(10, 12, 8, 12));
    final VBox labelBottom =
        new VBox(
            6,
            new FlowPane(8, 6, this.generate, this.exportPdf),
            note(
                "Печать: 100 %, без подгонки. QR открывает запись через поле «Код / QR». Остаток на этикетке не фиксируется."));
    labelBottom.setPadding(new Insets(8, 12, 10, 12));
    this.labelsTab.setContent(new BorderPane(previewScroll, labelTop, null, labelBottom, null));
    this.tabs.getTabs().setAll(this.samplesTab, this.labelsTab);
    this.tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    this.tabs.setMinSize(0, 0);
    this.setCenter(this.tabs);
    this.status.setMinHeight(0);
    this.status.setMaxHeight(40);
    this.status.setTooltip(new Tooltip());
    this.status.getTooltip().textProperty().bind(this.status.textProperty());
    final VBox footer = new VBox(this.status);
    footer.setPadding(new Insets(5, 12, 8, 12));
    this.setBottom(footer);
    this.widthProperty().addListener(o -> this.arrange());
    this.addEventFilter(
        KeyEvent.KEY_PRESSED,
        e -> {
          if (e.isShortcutDown() && e.getCode() == KeyCode.F) {
            this.tabs.getSelectionModel().select(this.samplesTab);
            this.inspecting = false;
            this.arrange();
            this.search.requestFocus();
            this.search.selectAll();
            e.consume();
          } else if (e.isShortcutDown() && e.getCode() == KeyCode.L) {
            this.tabs.getSelectionModel().select(this.samplesTab);
            this.inspecting = false;
            this.arrange();
            this.scanner.requestFocus();
            this.scanner.selectAll();
            e.consume();
          } else if (e.getCode() == KeyCode.ESCAPE && !this.exporting) {
            if (this.scanningNow || this.labelNow) {
              this.stop();
            } else if (this.tabs.getSelectionModel().getSelectedItem() == this.labelsTab) {
              this.tabs.getSelectionModel().select(this.samplesTab);
            } else if (this.inspecting) {
              this.inspecting = false;
              this.arrange();
            } else {
              this.clearFilters();
            }
            e.consume();
          }
        });
    this.arrange();
    this.controls();
    this.load();
  }

  private Button action(final String title, final String id, final Runnable action) {
    final Button b = new Button(title);
    b.setMinWidth(Region.USE_PREF_SIZE);
    b.setId("chemistry-catalog-" + id);
    b.setOnAction(
        e -> {
          try {
            action.run();
          } catch (final RuntimeException error) {
            this.status.setText(Objects.toString(error.getMessage(), "Действие не выполнено."));
          }
        });
    return b;
  }

  private static TableColumn<Row, String> column(
      final String name, final Function<Row, String> value, final double width) {
    final var c = new TableColumn<Row, String>(name);
    c.setMinWidth(90);
    c.setPrefWidth(width);
    c.setCellValueFactory(r -> new ReadOnlyStringWrapper(value.apply(r.getValue())));
    c.setCellFactory(
        v ->
            new TableCell<>() {
              @Override
              protected void updateItem(final String text, final boolean empty) {
                super.updateItem(text, empty);
                this.setText(empty ? null : text);
                this.setTooltip(empty ? null : new Tooltip(text));
              }
            });
    return c;
  }

  private void load() {
    if (this.disposed || this.exporting) {
      return;
    }
    if (this.scanning != null) {
      this.scanning.cancel(true);
    }
    this.invalidateLabels();
    final int version = ++this.scanVersion;
    final Current current = this.current.get();
    this.scanningNow = true;
    this.controls();
    this.status.setText("Читаю образцы из опытов…");
    this.scanning =
        this.worker.submit(
            () -> {
              try {
                final Snapshot found = new LaboratorySampleCatalog().scan(this.folder, current);
                Platform.runLater(
                    () -> {
                      if (this.disposed || version != this.scanVersion) {
                        return;
                      }
                      this.snapshot = found;
                      this.scanningNow = false;
                      this.diagnostics.setText(
                          found.diagnostics().isEmpty()
                              ? "Папка прочитана полностью. Текущие несохранённые данные отмечены *; этикетки доступны после сохранения."
                              : String.join("\n", found.diagnostics()));
                      this.filter();
                      this.controls();
                      this.status.setText(
                          "Снимок обновлён. Изменения образцов вносятся в исходном опыте.");
                    });
              } catch (final Exception error) {
                Platform.runLater(
                    () -> {
                      if (this.disposed || version != this.scanVersion) {
                        return;
                      }
                      this.scanningNow = false;
                      this.controls();
                      this.status.setText(
                          "Каталог не обновлён: "
                              + error.getMessage()
                              + ". Предыдущий снимок сохранён.");
                    });
              }
            });
  }

  private static boolean methodMatches(final String method, final String filter) {
    final String value = method.toLowerCase(Locale.ROOT);
    return switch (filter) {
      case "ЯМР" ->
          value.contains("ямр")
              || value.contains("qnmr")
              || value.contains("корреляции 2d")
              || value.contains("мультиплеты");
      case "Хроматография" -> value.contains("хроматография");
      case "Досье" -> value.equals("подтверждение продукта");
      default -> value.contains(filter.toLowerCase(Locale.ROOT));
    };
  }

  private void clearFilters() {
    this.search.clear();
    this.location.clear();
    this.state.getSelectionModel().selectFirst();
    this.method.getSelectionModel().selectFirst();
    this.kind.getSelectionModel().selectFirst();
  }

  private void filter() {
    if (this.snapshot == null) {
      return;
    }
    final String selected =
        this.table.getSelectionModel().getSelectedItem() == null
            ? ""
            : this.table.getSelectionModel().getSelectedItem().key();
    final int state = this.state.getSelectionModel().getSelectedIndex(),
        method = this.method.getSelectionModel().getSelectedIndex();
    final List<Row> visible =
        this.snapshot.rows().stream()
            .filter(r -> r.matches(this.search.getText()))
            .filter(
                r ->
                    LaboratoryExperimentIndex.normalize(
                            r.balance().location() + " " + r.balance().container())
                        .contains(LaboratoryExperimentIndex.normalize(this.location.getText())))
            .filter(
                r ->
                    switch (state) {
                      case 1 -> r.balance().status() == LaboratorySample.Status.AVAILABLE;
                      case 2 -> r.balance().remaining() == null;
                      case 3 -> r.copies() > 1 || r.sameCode() > 1;
                      case 4 -> r.balance().status() != LaboratorySample.Status.AVAILABLE;
                      default -> true;
                    })
            .filter(
                r ->
                    this.kind.getSelectionModel().getSelectedIndex() <= 0
                        || r.sample().kind().toString().equals(this.kind.getValue()))
            .filter(
                r ->
                    method <= 0
                        || method == 1 && r.analyses().isEmpty()
                        || method > 1
                            && r.analyses().stream()
                                .anyMatch(a -> methodMatches(a.method(), this.method.getValue())))
            .toList();
    this.table.getItems().setAll(visible);
    visible.stream()
        .filter(r -> r.key().equals(selected))
        .findFirst()
        .ifPresentOrElse(
            this.table.getSelectionModel()::select,
            () -> this.table.getSelectionModel().selectFirst());
    this.summary.setText(
        (this.snapshot.complete() ? "" : "Неполный каталог · ")
            + visible.size()
            + " из "
            + this.snapshot.rows().size()
            + " строк · "
            + visible.stream().map(Row::reference).distinct().count()
            + " образцов");
    this.details();
    this.controls();
  }

  private void resolve() {
    if (this.snapshot == null || this.scanner.getText().isBlank()) {
      this.status.setText("Введите код или QR-ссылку образца.");
      return;
    }
    final List<Row> matches = this.snapshot.resolve(this.scanner.getText());
    if (matches.isEmpty()) {
      this.status.setText(
          "Код не найден"
              + (this.snapshot.complete()
                  ? ". Проверьте папку и обновите каталог."
                  : " в прочитанной части папки. Каталог неполный."));
      return;
    }
    this.clearFilters();
    this.search.setText(this.scanner.getText().strip());
    this.table.getSelectionModel().clearSelection();
    for (final Row row : matches) {
      this.table.getSelectionModel().select(row);
    }
    this.table.scrollTo(matches.getFirst());
    this.tabs.getSelectionModel().select(this.samplesTab);
    this.inspecting = matches.size() == 1;
    this.arrange();
    this.status.setText(
        matches.size() == 1
            ? "Образец найден. Откройте запись или нужный анализ."
            : "Несколько совпадений: выберите нужный файл. Автоматическое открытие отключено.");
    this.scanner.selectAll();
  }

  private void details() {
    this.card.getChildren().clear();
    if (this.table.getSelectionModel().getSelectedItems().size() > 1) {
      this.card
          .getChildren()
          .addAll(
              note("Выбрано образцов: " + this.table.getSelectionModel().getSelectedItems().size()),
              note(
                  "Для карточки и анализов выберите одну строку. Для партии этикеток оставьте нужные строки выделенными."));
      return;
    }
    final Row row = this.table.getSelectionModel().getSelectedItem();
    if (row == null) {
      this.card.getChildren().add(note("Выберите образец."));
      return;
    }
    final Label title = note(row.sample().code());
    title.setStyle("-fx-font-size: 22; -fx-font-weight: bold;");
    this.card
        .getChildren()
        .addAll(
            title,
            note(row.balance().label()),
            note(
                row.sample().molecule().displayName()
                    + " · "
                    + row.sample().molecule().formula().hillNotation()),
            note(row.quantity() + " · " + row.balance().status()),
            note(
                "Место: "
                    + (row.balance().location().isBlank() ? "не указано" : row.balance().location())
                    + "\nКонтейнер: "
                    + (row.balance().container().isBlank()
                        ? "не указан"
                        : row.balance().container())));
    if (!row.warning().isBlank()) {
      this.card.getChildren().add(note(row.warning()));
    }
    final var structure = new LaboratoryAnalysisPlot.Structure();
    structure.setMinHeight(130);
    structure.setPrefHeight(185);
    structure.show(
        new LaboratoryMoleculePlacement().place(row.sample().molecule(), 0, 0),
        LaboratoryAnalysis.Nucleus.H1,
        Set.of(),
        null);
    this.card.getChildren().add(structure);
    this.card
        .getChildren()
        .addAll(
            note("Опыт: " + row.experimentTitle()),
            new FlowPane(
                7,
                6,
                action("+ ЯМР", "new-nmr", () -> this.open.accept(row, "NMR")),
                action("+ МС", "new-ms", () -> this.open.accept(row, "MS")),
                action("+ ТСХ", "new-tlc", () -> this.open.accept(row, "TLC"))));
    if (row.sample().origin() != null || !row.sample().parents().isEmpty())
      this.card
          .getChildren()
          .add(
              note(
                  "Есть родительские образцы или передача из другого опыта. Их анализы доступны в «Все анализы образца» с указанием источника."));
    this.card
        .getChildren()
        .add(
            note(
                row.analyses().isEmpty()
                    ? "Собственных аналитических записей нет."
                    : String.join(
                        "\n",
                        row.analyses().stream()
                            .map(a -> a.method() + " · " + a.origin() + " · " + a.title())
                            .toList())));
    final FlowPane lineage = new FlowPane(6, 6);
    if (this.snapshot != null) {
      this.snapshot.rows().stream()
          .filter(
              r ->
                  r.reference().experimentId().equals(row.reference().experimentId())
                      && Objects.equals(r.path(), row.path())
                      && (row.sample().parents().contains(r.sample().id())
                          || r.sample().parents().contains(row.sample().id())))
          .limit(100)
          .forEach(
              r ->
                  lineage
                      .getChildren()
                      .add(
                          action(
                              (row.sample().parents().contains(r.sample().id()) ? "← " : "→ ")
                                  + r.sample().code(),
                              "relative-" + r.sample().id(),
                              () -> {
                                this.scanner.setText(r.reference().token());
                                this.resolve();
                              })));
    }
    if (!lineage.getChildren().isEmpty()) {
      this.card.getChildren().addAll(note("Происхождение и дочерние образцы"), lineage);
    }
    final TextField source = new TextField(row.source());
    source.setEditable(false);
    final TextField token = new TextField(row.reference().token());
    token.setEditable(false);
    final var qr = LaboratorySampleLabels.qr(row.reference().token());
    final WritableImage image = new WritableImage(qr.getWidth() * 4, qr.getHeight() * 4);
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        image.getPixelWriter().setArgb(x, y, qr.get(x / 4, y / 4) ? 0xff000000 : 0xffffffff);
      }
    }
    final ImageView qrView = new ImageView(image);
    qrView.setSmooth(false);
    final TitledPane identity =
        new TitledPane("Файл и QR-ссылка", new VBox(8, source, qrView, token));
    identity.setExpanded(false);
    identity.setAnimated(false);
    this.card.getChildren().add(identity);
  }

  private static void show(final Node node, final boolean visible) {
    node.setVisible(visible);
    node.setManaged(visible);
  }

  private void arrange() {
    final boolean compact = this.getWidth() < 1050;
    final List<Node> nodes =
        this.inspecting
            ? List.of(this.cardScroll)
            : compact ? List.of(this.list) : List.of(this.list, this.cardScroll);
    if (!this.split.getChildren().equals(nodes)) {
      this.split.getChildren().setAll(nodes);
    }
    if (this.back != null) {
      show(this.back, this.inspecting);
      show(this.inspect, !this.inspecting);
      show(this.headingScroll, !this.inspecting);
      show(this.more, compact);
      for (final Node node : List.of(this.selectAll, this.csv, this.refresh, this.stop)) {
        show(node, !compact);
      }
    }
  }

  private void openSelected(final String action) {
    final var selected = this.table.getSelectionModel().getSelectedItems();
    if (selected.size() != 1) {
      throw new IllegalArgumentException("Выберите один образец.");
    }
    this.open.accept(selected.getFirst(), action);
  }

  private void controls() {
    if (this.refresh == null || this.exportPdf == null) {
      return;
    }
    final int selected = this.table.getSelectionModel().getSelectedItems().size();
    this.busy(this.scanningNow || this.labelNow);
    this.stop.setDisable(!this.running || this.exporting);
    this.refresh.setDisable(this.scanningNow || this.exporting);
    this.openSample.setDisable(selected != 1 || this.exporting);
    this.savedAnalyses.setDisable(selected != 1 || this.exporting);
    this.inspect.setDisable(selected != 1);
    this.labelButton.setDisable(selected == 0 || this.scanningNow || this.exporting);
    this.csv.setDisable(this.table.getItems().isEmpty() || this.running || this.exporting);
    this.generate.setDisable(selected == 0 || this.scanningNow || this.exporting);
    this.exportPdf.setDisable(this.bundle == null || this.labelNow);
    this.previous.setDisable(this.bundle == null || this.labelNow || this.pageIndex <= 0);
    this.next.setDisable(
        this.bundle == null || this.labelNow || this.pageIndex + 1 >= this.bundle.pages());
    this.samplesTab.getContent().setDisable(this.exporting);
    this.format.setDisable(this.exporting);
    this.copies.setDisable(this.exporting);
    this.skip.setDisable(
        this.exporting || this.format.getValue() == LaboratorySampleLabels.Format.ROLL);
  }

  private void invalidateLabels() {
    if (this.exporting) {
      return;
    }
    this.labelVersion++;
    if (this.preparing != null) {
      this.preparing.cancel(true);
    }
    this.bundle = null;
    this.preview.setImage(null);
    this.labelNow = false;
    this.pageLabel.setText("");
    this.labelSummary.setText(
        "Выбрано строк: "
            + this.table.getSelectionModel().getSelectedItems().size()
            + ". Нажмите «Сформировать».");
    this.controls();
  }

  private static int integer(final TextField field) {
    try {
      return Integer.parseInt(field.getText().strip());
    } catch (final NumberFormatException error) {
      throw new IllegalArgumentException("Копии и пропуск мест должны быть целыми числами.");
    }
  }

  private void generate() {
    if (this.exporting || this.snapshot == null) {
      return;
    }
    final var options =
        new LaboratorySampleLabels.Options(
            this.format.getValue(), integer(this.copies), integer(this.skip));
    final List<Row> rows = List.copyOf(this.table.getSelectionModel().getSelectedItems());
    final var captured = this.snapshot.capturedAt();
    this.invalidateLabels();
    final int version = this.labelVersion;
    this.labelNow = true;
    this.controls();
    this.status.setText("Подготавливаю этикетки…");
    this.preparing =
        this.worker.submit(
            () -> {
              try {
                final var labels = LaboratorySampleLabels.generate(rows, options, captured);
                Platform.runLater(
                    () -> {
                      if (this.disposed || version != this.labelVersion) {
                        return;
                      }
                      this.bundle = labels;
                      this.labelSummary.setText(
                          labels.labels()
                              + " этикеток · "
                              + labels.pages()
                              + " стр. · снимок "
                              + captured
                                  .atZone(java.time.ZoneId.systemDefault())
                                  .toLocalDateTime()
                                  .withNano(0));
                      this.labelNow = false;
                      this.page(0);
                    });
              } catch (final Exception error) {
                this.labelFailure(version, error);
              }
            });
  }

  private void page(final int page) {
    if (this.bundle == null || page < 0 || page >= this.bundle.pages() || this.exporting) {
      return;
    }
    if (this.preparing != null) {
      this.preparing.cancel(true);
    }
    final int version = ++this.labelVersion;
    final var labels = this.bundle;
    this.pageIndex = page;
    this.labelNow = true;
    this.controls();
    this.preparing =
        this.worker.submit(
            () -> {
              try {
                final byte[] png = LaboratorySampleLabels.preview(labels, page);
                Platform.runLater(
                    () -> {
                      if (this.disposed || version != this.labelVersion) {
                        return;
                      }
                      this.preview.setImage(new Image(new ByteArrayInputStream(png)));
                      this.pageLabel.setText((page + 1) + " / " + labels.pages());
                      this.labelNow = false;
                      this.controls();
                      this.status.setText(
                          labels.warnings().isEmpty()
                              ? "Предпросмотр готов. Сохраняется именно этот PDF."
                              : String.join(" ", labels.warnings()));
                    });
              } catch (final Exception error) {
                this.labelFailure(version, error);
              }
            });
  }

  private void labelFailure(final int version, final Exception error) {
    Platform.runLater(
        () -> {
          if (this.disposed || version != this.labelVersion) {
            return;
          }
          this.labelNow = false;
          this.controls();
          this.status.setText(
              Objects.toString(error.getMessage(), "Не удалось подготовить этикетки."));
        });
  }

  private Path choose(final String extension, final String name) {
    final var chooser = new javafx.stage.FileChooser();
    chooser.setTitle("Сохранить в новый файл");
    chooser.setInitialFileName(name + extension);
    chooser
        .getExtensionFilters()
        .add(
            new javafx.stage.FileChooser.ExtensionFilter(
                extension.toUpperCase(Locale.ROOT), "*" + extension));
    final var file =
        chooser.showSaveDialog(this.getScene() == null ? null : this.getScene().getWindow());
    return file == null ? null : file.toPath();
  }

  private void exportCsv() {
    final Path target = this.choose(".csv", "Образцы проекта");
    if (target != null) {
      final List<Row> rows = List.copyOf(this.table.getItems());
      this.write(target, () -> LaboratorySampleCatalog.csv(rows), ".csv");
    }
  }

  private void exportPdf() {
    if (this.bundle == null) {
      return;
    }
    final Path target = this.choose(".pdf", "Этикетки образцов");
    if (target != null) {
      final var labels = this.bundle;
      this.write(target, labels::pdf, ".pdf");
    }
  }

  private void write(final Path target, final Supplier<byte[]> bytes, final String extension) {
    this.exporting = true;
    this.labelNow = true;
    this.controls();
    final int version = ++this.labelVersion;
    this.preparing =
        this.worker.submit(
            () -> {
              try {
                LaboratorySampleCatalog.writeNew(target, bytes.get(), extension);
                Platform.runLater(
                    () -> {
                      if (this.disposed || version != this.labelVersion) {
                        return;
                      }
                      this.exporting = false;
                      this.labelNow = false;
                      this.controls();
                      this.status.setText("Сохранено: " + target);
                    });
              } catch (final Exception error) {
                Platform.runLater(
                    () -> {
                      if (this.disposed || version != this.labelVersion) {
                        return;
                      }
                      this.exporting = false;
                      this.labelNow = false;
                      this.controls();
                      this.status.setText("Файл не сохранён: " + error.getMessage());
                    });
              }
            });
  }

  private void stop() {
    if (this.exporting) {
      return;
    }
    this.scanVersion++;
    if (this.scanning != null) {
      this.scanning.cancel(true);
    }
    this.scanningNow = false;
    this.invalidateLabels();
    this.controls();
    this.status.setText("Действие остановлено. Прежний каталог сохранён.");
  }

  @Override
  boolean canClose() {
    if (this.exporting) {
      this.status.setText("Дождитесь сохранения файла.");
      return false;
    }
    return true;
  }

  @Override
  void dispose() {
    super.dispose();
    this.scanVersion++;
    this.labelVersion++;
    if (this.scanning != null) {
      this.scanning.cancel(true);
    }
    if (this.preparing != null) {
      this.preparing.cancel(true);
    }
    this.worker.shutdownNow();
  }
}

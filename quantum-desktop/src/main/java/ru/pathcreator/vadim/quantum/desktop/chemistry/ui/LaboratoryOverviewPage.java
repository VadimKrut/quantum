/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange.CdkCipCalculator;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.*;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryProjectOverview.Row;

/** Project comparison is a view over saved experiments; it never commits inferred results. */
final class LaboratoryOverviewPage extends LaboratoryComputationPage {
  /** A transient, exact drawing-bound choice from a successfully created series; never persisted. */
  record ReferenceHint(
      String experimentId, String stageId, String componentId, String structureSignature) {
    ReferenceHint {
      Objects.requireNonNull(experimentId);
      Objects.requireNonNull(stageId);
      Objects.requireNonNull(componentId);
      Objects.requireNonNull(structureSignature);
    }

    static ReferenceHint from(
        final LaboratoryDrawing drawing, final String stageId, final String componentId) {
      final var stage =
          drawing.experiment().stages().stream()
              .filter(s -> s.id().equals(stageId))
              .findFirst()
              .orElseThrow();
      if (stage.participants().stream()
          .noneMatch(
              p ->
                  p.componentId().equals(componentId)
                      && p.role() == ReactionComponentRole.REACTANT)) {
        throw new IllegalArgumentException("Опора серии должна быть исходником выбранной стадии.");
      }
      return new ReferenceHint(
          drawing.experiment().id(),
          stageId,
          componentId,
          CdkCipCalculator.signature(
              Objects.requireNonNull(drawing.layouts().get(componentId)),
              CdkCipCalculator.Source.DRAWING_2D));
    }

    boolean matches(final Row row) {
      if (row.stage() == null
          || !this.experimentId.equals(row.drawing().experiment().id())
          || !this.stageId.equals(row.stage().id())) {
        return false;
      }
      if (row.stage().participants().stream()
          .noneMatch(
              p ->
                  p.componentId().equals(this.componentId)
                      && p.role() == ReactionComponentRole.REACTANT)) {
        return false;
      }
      final var drawing = row.drawing().layouts().get(this.componentId);
      return drawing != null
          && this.structureSignature.equals(
              CdkCipCalculator.signature(drawing, CdkCipCalculator.Source.DRAWING_2D));
    }
  }

  private final List<ReferenceHint> referenceHints;
  private final Map<String, ReferenceHint> inheritedReferences = new HashMap<>();
  private final Set<String> chosenReferences = new HashSet<>();

  private record Choice(String id, String title) {
    @Override
    public String toString() {
      return this.title;
    }
  }

  private final Path folder;
  private final Consumer<Path> open;
  private final ExecutorService worker =
      Executors.newSingleThreadExecutor(
          task -> {
            final Thread t = new Thread(task, "laboratory-project-overview");
            t.setDaemon(true);
            return t;
          });
  private Future<?> scan;
  private int generation;
  private final TextField search = new TextField();
  private final ComboBox<String> filter = new ComboBox<>();
  private final TableView<Row> table = new TableView<>();
  private final Label summary = note(""),
      comparisonHint =
          note("Выберите строку для просмотра. Закрепите до четырёх стадий для сравнения.");
  private final LinkedHashMap<String, Row> pinned = new LinkedHashMap<>();
  private final Map<String, String> references = new HashMap<>(), structures = new HashMap<>();
  private final ScrollPane detail = new ScrollPane();
  private Node searchControls, headerContent;
  private final Button inspect = new Button("Карточка"), backToList = new Button("← К списку");
  private final TilePane cards = new TilePane(14, 14);
  private final BorderPane body = new BorderPane();
  private final SplitPane split = new SplitPane();
  private final VBox list;
  private final Button pin = new Button("+ К сравнению"),
      compare = new Button("Сравнить"),
      openButton = new Button("Открыть опыт");
  private final Button refresh = new Button("Обновить"), stop = new Button("Остановить");
  private List<Row> rows = List.of();
  private List<String> diagnostics = List.of();
  private boolean comparing, inspecting;

  LaboratoryOverviewPage(final Path folder, final Consumer<Path> open, final Runnable close) {
    this(folder, open, close, null);
  }

  LaboratoryOverviewPage(
      final Path folder,
      final Consumer<Path> open,
      final Runnable close,
      final LaboratoryProjectOverview.Snapshot selection) {
    this(folder, open, close, selection, List.of());
  }

  LaboratoryOverviewPage(
      final Path folder,
      final Consumer<Path> open,
      final Runnable close,
      final LaboratoryProjectOverview.Snapshot selection,
      final List<ReferenceHint> referenceHints) {
    super(selection == null ? "Обзор проекта" : "Совпадения структур · сравнение опытов", close);
    this.folder = folder.toAbsolutePath().normalize();
    this.open = open;
    this.referenceHints = List.copyOf(referenceHints);
    this.setId("chemistry-overview-page");
    this.search.setId("chemistry-overview-search");
    this.search.setPromptText("Опыт, вещество, формула, растворитель…");
    this.search.setMinWidth(90);
    this.search.setPrefWidth(320);
    this.filter
        .getItems()
        .setAll(
            "Все стадии", "Есть фактический продукт", "Без фактического продукта", "Без стадии");
    this.filter.getSelectionModel().selectFirst();
    this.filter.setId("chemistry-overview-filter");
    this.search.textProperty().addListener((o, a, b) -> this.filter());
    this.filter.valueProperty().addListener((o, a, b) -> this.filter());
    final Button clear =
        action(
            "×",
            "chemistry-overview-clear",
            () -> {
              this.search.clear();
              this.filter.getSelectionModel().selectFirst();
            });
    clear.setAccessibleText("Сбросить поиск и фильтр");
    clear.setTooltip(new Tooltip("Сбросить поиск и фильтр"));
    this.refresh.setText("↻");
    this.refresh.setAccessibleText("Обновить обзор");
    this.refresh.setTooltip(new Tooltip("Обновить сохранённые опыты"));
    this.refresh.setId("chemistry-overview-refresh");
    this.refresh.setOnAction(e -> this.load());
    this.stop.setOnAction(e -> this.stop());
    this.stop.setId("chemistry-overview-stop");
    this.stop.setDisable(true);
    this.stop.visibleProperty().bind(this.stop.disabledProperty().not());
    this.stop.managedProperty().bind(this.stop.visibleProperty());
    final FlowPane controls =
        new FlowPane(8, 8, this.search, this.filter, clear, this.refresh, this.stop);
    this.searchControls = controls;
    final Label location =
        note(
            this.folder.getFileName() == null
                ? this.folder.toString()
                : this.folder.getFileName().toString());
    location.setStyle("-fx-font-size: 24; -fx-font-weight: bold;");
    location.setTooltip(new Tooltip(this.folder.toString()));
    final VBox heading =
        new VBox(
            8,
            location,
            note(
                selection == null
                    ? "Сохранённые опыты · несохранённые правки появятся после сохранения и обновления"
                    : "Снимок результатов структурного поиска · для новой выборки повторите поиск"),
            controls,
            this.summary);
    heading.setPadding(new Insets(0, 16, 12, 16));
    this.headerContent = heading;
    this.table.setId("chemistry-overview-table");
    this.table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    this.table
        .getColumns()
        .addAll(
            column("Опыт / стадия", r -> r.title() + "\n" + r.stageTitle(), 270),
            column("Среда и условия", r -> r.media() + "\n" + r.conditionsSummary(), 190),
            column("Результат", r -> r.hasResult() ? "Факт записан" : "Факт не записан", 120));
    this.table.setPlaceholder(
        note("Подходящих опытов нет. Сбросьте поиск или выберите другую папку."));
    this.table.setMinSize(0, 100);
    this.table
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (o, a, b) -> {
              this.actions();
              if (!this.comparing) {
                this.details();
              }
            });
    this.table.setRowFactory(
        view -> {
          final TableRow<Row> row = new TableRow<>();
          row.setOnMouseClicked(
              e -> {
                if (e.getButton() == MouseButton.PRIMARY
                    && e.getClickCount() == 2
                    && !row.isEmpty()) {
                  this.open.accept(row.getItem().path());
                }
              });
          return row;
        });
    this.table.setOnKeyPressed(
        e -> {
          if (e.getCode() == KeyCode.ENTER
              && this.table.getSelectionModel().getSelectedItem() != null) {
            if (e.isShortcutDown()) {
              this.pin();
            } else {
              this.openSelected();
            }
            e.consume();
          }
        });
    this.pin.setId("chemistry-overview-pin");
    this.pin.setOnAction(e -> this.pin());
    this.pin.setTooltip(new Tooltip("Добавить стадию к сравнению · Ctrl+Enter в таблице"));
    this.openButton.setId("chemistry-overview-open");
    this.openButton.setOnAction(e -> this.openSelected());
    this.openButton.getStyleClass().add("laboratory-save");
    this.compare.setId("chemistry-overview-compare");
    this.compare.setOnAction(
        e -> {
          this.comparing = !this.comparing;
          this.inspecting = false;
          this.arrange();
          this.details();
        });
    final Button reset =
        action(
            "Очистить сравнение",
            "chemistry-overview-reset-comparison",
            () -> {
              this.pinned.clear();
              this.comparing = false;
              this.inspecting = false;
              this.arrange();
              this.actions();
              this.details();
            });
    this.inspect.setId("chemistry-overview-inspect");
    this.inspect.setOnAction(
        e -> {
          this.inspecting = true;
          this.comparing = false;
          this.arrange();
          this.details();
        });
    this.backToList.setId("chemistry-overview-list");
    this.backToList.setOnAction(
        e -> {
          this.inspecting = false;
          this.comparing = false;
          this.arrange();
          this.details();
        });
    final FlowPane actions =
        new FlowPane(
            8, 8, this.backToList, this.inspect, this.openButton, this.pin, this.compare, reset);
    this.list = new VBox(10, this.table);
    VBox.setVgrow(this.table, Priority.ALWAYS);
    this.list.setMinWidth(0);
    this.cards.setId("chemistry-overview-cards");
    this.cards.setPadding(new Insets(4));
    this.cards.setPrefColumns(1);
    this.cards.setTileAlignment(javafx.geometry.Pos.TOP_LEFT);
    final ScrollPane detail = this.detail;
    detail.setContent(this.cards);
    detail.setId("chemistry-overview-scroll");
    detail.setFitToWidth(true);
    detail.setMinSize(0, 0);
    detail.viewportBoundsProperty().addListener((o, a, b) -> this.cardWidth(b.getWidth()));
    this.split.getItems().addAll(this.list, detail);
    this.split.setDividerPositions(.50);
    this.split.setMinSize(0, 0);
    this.body.setBottom(actions);
    BorderPane.setMargin(actions, new Insets(10, 0, 0, 0));
    this.body.setCenter(this.split);
    this.body.setTop(this.comparisonHint);
    BorderPane.setMargin(this.comparisonHint, new Insets(0, 0, 10, 0));
    this.body.setPadding(new Insets(0, 16, 10, 16));
    final VBox content = new VBox(heading, this.body);
    VBox.setVgrow(this.body, Priority.ALWAYS);
    this.setCenter(content);
    final ScrollPane footer = new ScrollPane(this.status);
    footer.setFitToWidth(true);
    footer.setPrefViewportHeight(36);
    footer.setMaxHeight(56);
    footer.setPadding(new Insets(4, 16, 6, 16));
    this.setBottom(footer);
    this.widthProperty().addListener((o, a, b) -> this.arrange());
    this.addEventFilter(
        KeyEvent.KEY_PRESSED,
        e -> {
          if (e.isShortcutDown() && e.getCode() == KeyCode.F) {
            this.search.requestFocus();
            this.search.selectAll();
            e.consume();
          } else if (e.getCode() == KeyCode.ESCAPE) {
            if (!this.search.getText().isEmpty()) {
              this.search.clear();
            } else if (this.comparing || this.inspecting) {
              this.comparing = false;
              this.inspecting = false;
              this.arrange();
              this.details();
            } else {
              this.finish();
            }
            e.consume();
          }
        });
    this.actions();
    if (selection == null) {
      this.load();
    } else {
      this.rows = selection.rows();
      this.diagnostics = selection.diagnostics();
      this.applyReferenceHints();
      this.refresh.setDisable(true);
      this.refresh.setTooltip(
          new Tooltip("Для обновления структурной выборки выполните поиск заново"));
      this.filter();
      this.details();
    }
  }

  private void applyReferenceHints() {
    for (final Row row : this.rows) {
      if (this.chosenReferences.contains(row.key())) {
        continue;
      }
      final var previous = this.inheritedReferences.remove(row.key());
      if (previous != null) {
        this.references.remove(row.key());
      }
      final var matches =
          this.referenceHints.stream().filter(h -> h.matches(row)).distinct().toList();
      if (matches.size() == 1) {
        final var hint = matches.getFirst();
        this.references.put(row.key(), hint.componentId());
        this.inheritedReferences.put(row.key(), hint);
      }
    }
  }

  private static TableColumn<Row, String> column(
      final String title, final Function<Row, String> value, final double width) {
    final TableColumn<Row, String> c = new TableColumn<>(title);
    c.setPrefWidth(width);
    c.setMinWidth(85);
    c.setCellValueFactory(r -> new ReadOnlyStringWrapper(value.apply(r.getValue())));
    c.setCellFactory(
        v ->
            new TableCell<>() {
              @Override
              protected void updateItem(final String text, final boolean empty) {
                super.updateItem(text, empty);
                this.setText(empty ? null : text);
                this.setTooltip(empty || text == null ? null : new Tooltip(text));
              }
            });
    return c;
  }

  private static Button action(final String text, final String id, final Runnable action) {
    final Button b = new Button(text);
    b.setId(id);
    b.setOnAction(e -> action.run());
    return b;
  }

  private void load() {
    if (this.disposed) {
      return;
    }
    if (this.scan != null) {
      this.scan.cancel(true);
    }
    final int requested = ++this.generation;
    this.busy(true);
    this.stop.setDisable(false);
    this.refresh.setDisable(true);
    this.status.setText("Читаю сохранённые опыты…");
    this.scan =
        this.worker.submit(
            () -> {
              try {
                final var snapshot = new LaboratoryProjectOverview().scan(this.folder);
                Platform.runLater(
                    () -> {
                      if (this.disposed || requested != this.generation) {
                        return;
                      }
                      this.rows = snapshot.rows();
                      this.diagnostics = snapshot.diagnostics();
                      this.applyReferenceHints();
                      final Set<String> keys = new HashSet<>(this.pinned.keySet());
                      this.pinned.clear();
                      this.rows.stream()
                          .filter(r -> keys.contains(r.key()))
                          .forEach(r -> this.pinned.put(r.key(), r));
                      this.loaded();
                      this.filter();
                      this.details();
                    });
              } catch (final CancellationException ignored) {
                /* Newer generation owns the page. */
              } catch (final Exception error) {
                Platform.runLater(
                    () -> {
                      if (!this.disposed && requested == this.generation) {
                        this.loaded();
                        this.status.setText(
                            "Не удалось обновить обзор: "
                                + error.getMessage()
                                + ". На экране остался прежний снимок.");
                      }
                    });
              }
            });
  }

  private void loaded() {
    this.busy(false);
    this.stop.setDisable(true);
    this.refresh.setDisable(false);
  }

  private void stop() {
    if (this.scan != null) {
      this.scan.cancel(true);
    }
    this.generation++;
    this.loaded();
    this.status.setText("Обновление остановлено. На экране остался прежний снимок.");
  }

  @Override
  boolean canClose() {
    return true;
  }

  @Override
  void dispose() {
    if (this.disposed) {
      return;
    }
    super.dispose();
    this.generation++;
    if (this.scan != null) {
      this.scan.cancel(true);
    }
    this.worker.shutdownNow();
  }

  private void filter() {
    final Row selected = this.table.getSelectionModel().getSelectedItem();
    final String[] terms = LaboratoryExperimentIndex.normalize(this.search.getText()).split(" ");
    final int mode = this.filter.getSelectionModel().getSelectedIndex();
    this.table
        .getItems()
        .setAll(
            this.rows.stream()
                .filter(r -> Arrays.stream(terms).allMatch(r.search()::contains))
                .filter(
                    r ->
                        mode == 1
                            ? r.hasResult()
                            : mode == 2 ? !r.hasResult() : mode != 3 || r.stage() == null)
                .toList());
    this.table.sort();
    this.table
        .getSelectionModel()
        .select(
            this.table.getItems().stream()
                .filter(r -> selected != null && r.key().equals(selected.key()))
                .findFirst()
                .orElse(this.table.getItems().isEmpty() ? null : this.table.getItems().getFirst()));
    this.summary.setText(
        "Опытов: "
            + this.rows.stream().map(Row::path).distinct().count()
            + "   ·   Стадий / записей: "
            + this.table.getItems().size()
            + " из "
            + this.rows.size());
    this.status.setText(
        this.diagnostics.isEmpty()
            ? "Обзор не меняет исходные файлы. Фактические количества могут быть литературными — проверяйте записи источника."
            : String.join("\n", this.diagnostics));
    this.actions();
  }

  private void actions() {
    final Row row = this.table.getSelectionModel().getSelectedItem();
    this.openButton.setDisable(row == null);
    this.inspect.setDisable(row == null);
    this.pin.setDisable(
        row == null || this.pinned.containsKey(row.key()) || this.pinned.size() >= 4);
    this.compare.setDisable(this.pinned.isEmpty() && !this.comparing);
    this.compare.setText(this.comparing ? "← К списку" : "Сравнить (" + this.pinned.size() + ")");
  }

  private void pin() {
    final Row row = this.table.getSelectionModel().getSelectedItem();
    if (row == null || this.pinned.size() >= 4) {
      return;
    }
    this.pinned.put(row.key(), row);
    this.actions();
    this.comparisonHint.setText(
        "В сравнении: " + this.pinned.size() + " из 4. Можно найти и добавить другую стадию.");
  }

  private void openSelected() {
    final Row row = this.table.getSelectionModel().getSelectedItem();
    if (row != null) {
      this.open.accept(row.path());
    }
  }

  private void arrange() {
    final boolean expanded = this.comparing || this.inspecting;
    final boolean compact = this.getWidth() < 1000;
    this.search.setPrefWidth(this.getWidth() < 800 ? 210 : 320);
    this.filter.setPrefWidth(this.getWidth() < 800 ? 190 : Region.USE_COMPUTED_SIZE);
    final List<Node> wanted =
        expanded
            ? List.of(this.detail)
            : compact ? List.of(this.list) : List.of(this.list, this.detail);
    if (!this.split.getItems().equals(wanted)) {
      this.split.getItems().setAll(wanted);
      if (wanted.size() == 2) {
        this.split.setDividerPositions(.50);
      }
    }
    this.headerContent.setVisible(!expanded);
    this.headerContent.setManaged(!expanded);
    this.openButton.setVisible(!expanded);
    this.openButton.setManaged(!expanded);
    this.pin.setVisible(!this.comparing);
    this.pin.setManaged(!this.comparing);
    this.compare.setVisible(!this.comparing);
    this.compare.setManaged(!this.comparing);
    this.backToList.setVisible(expanded);
    this.backToList.setManaged(expanded);
    this.inspect.setVisible(!expanded);
    this.inspect.setManaged(!expanded);
    this.searchControls.setVisible(!expanded);
    this.searchControls.setManaged(!expanded);
    this.split.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
    this.actions();
  }

  private void cardWidth(final double width) {
    final int columns = this.comparing && width >= 900 ? 2 : 1;
    final double w = Math.max(240, (width - 14 * (columns - 1) - 12) / columns);
    this.cards.setPrefColumns(columns);
    this.cards.setPrefTileWidth(w);
    this.cards
        .getChildren()
        .forEach(
            n -> {
              if (n instanceof Region region) {
                region.setPrefWidth(w);
                region.setMinWidth(0);
                region.setMaxWidth(w);
              }
            });
  }

  private void details() {
    this.cards.getChildren().clear();
    final var selected = this.table.getSelectionModel().getSelectedItem();
    final List<Row> shown =
        this.comparing
            ? List.copyOf(this.pinned.values())
            : selected == null ? List.of() : List.of(selected);
    for (final Row row : shown) {
      this.cards.getChildren().add(this.card(row));
    }
    this.comparisonHint.setText(
        this.comparing
            ? "Сравнение стадий · выберите исходник для выхода. Escape — к списку."
            : this.inspecting
                ? "Карточка сохранённого опыта · Escape — к списку."
                : "Выберите строку для просмотра. «+ К сравнению» закрепляет до четырёх стадий.");
    this.cardWidth(this.detail.getViewportBounds().getWidth());
    this.actions();
  }

  private Node card(final Row row) {
    final Label title = note(row.title());
    title.setStyle("-fx-font-size: 17; -fx-font-weight: bold;");
    final Button open = action("Открыть опыт", null, () -> this.open.accept(row.path()));
    final FlowPane actions = new FlowPane(8, 6, open);
    if (this.comparing) {
      actions
          .getChildren()
          .add(
              action(
                  "Убрать",
                  null,
                  () -> {
                    this.pinned.remove(row.key());
                    if (this.pinned.isEmpty()) {
                      this.comparing = false;
                      this.arrange();
                    }
                    this.details();
                  }));
    }
    final VBox card = new VBox(10, title, note(row.stageTitle()), actions);
    card.getStyleClass().add("laboratory-overview-card");
    card.setPadding(new Insets(16));
    card.setMinHeight(Region.USE_PREF_SIZE);
    final List<LaboratoryComponent> products =
        new ArrayList<>(row.components(ReactionComponentRole.PRODUCT));
    products.addAll(row.components(ReactionComponentRole.REACTANT));
    if (!products.isEmpty()) {
      final ComboBox<Choice> product = new ComboBox<>();
      products.forEach(c -> product.getItems().add(new Choice(c.id(), c.molecule().displayName())));
      product.setMinWidth(0);
      product.setMaxWidth(Double.MAX_VALUE);
      product.getSelectionModel().selectFirst();
      product.getItems().stream()
          .filter(c -> c.id().equals(this.structures.get(row.key())))
          .findFirst()
          .ifPresent(product::setValue);
      final LaboratoryAdaptivePreview viewport =
          new LaboratoryAdaptivePreview(() -> row.drawing().layouts().get(product.getValue().id()));
      viewport.setId("chemistry-overview-structure-preview");
      product.setOnAction(
          e -> {
            this.structures.put(row.key(), product.getValue().id());
            viewport.refresh();
          });
      card.getChildren().addAll(note("Структура · продукт или исходник"), product, viewport);
    } else {
      card.getChildren().add(note("Исходники и продукты пока не заданы"));
    }
    card.getChildren()
        .addAll(
            note("Условия стадии: " + row.temperature() + " · " + row.duration()),
            note("Среда / добавки: " + (row.media().isBlank() ? "не указаны" : row.media())));
    if (row.conditions().note() != null && !row.conditions().note().isBlank()) {
      card.getChildren()
          .add(note("Текст условий сохранён без пересчёта · см. «Записи и источник»."));
    }
    final var common = row.drawing().experiment().conditions();
    if (row.stage() != null
        && !common.equals(
            ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions.EMPTY)) {
      card.getChildren()
          .add(
              note(
                  "Общие условия опыта (отдельно): "
                      + LaboratoryProjectOverview.conditionsText(common)));
    }
    final TextArea notes =
        new TextArea(
            "Файл: "
                + this.folder.relativize(row.path())
                + "\n\nУсловия стадии\n"
                + Objects.toString(row.conditions().note(), "Не указаны")
                + "\n\nЗаписи опыта / источник\n"
                + row.drawing().experiment().notes());
    notes.setEditable(false);
    notes.setWrapText(true);
    notes.setPrefRowCount(6);
    final TitledPane provenance = new TitledPane("Записи и источник", notes);
    provenance.setExpanded(false);
    if (row.stage() != null) {
      final ComboBox<Choice> reference = new ComboBox<>();
      reference.setMaxWidth(Double.MAX_VALUE);
      reference.setPromptText("Выберите исходник для выхода");
      row.components(ReactionComponentRole.REACTANT)
          .forEach(c -> reference.getItems().add(new Choice(c.id(), c.molecule().displayName())));
      final String remembered = this.references.get(row.key());
      reference.getItems().stream()
          .filter(c -> c.id().equals(remembered))
          .findFirst()
          .ifPresent(reference::setValue);
      final VBox quantities = new VBox(8), result = new VBox(6);
      final TitledPane loading = new TitledPane("Загрузка: план и факт", quantities);
      loading.setExpanded(false);
      final Runnable recalculate =
          () ->
              this.quantities(
                  row,
                  reference.getValue() == null ? null : reference.getValue().id(),
                  quantities,
                  result);
      final Label referenceSource =
          note(
              this.inheritedReferences.containsKey(row.key())
                  ? "Опора из плана серии · можно изменить"
                  : "Опорный исходник для препаративного выхода");
      referenceSource.setId("chemistry-overview-reference-source");
      reference.setId("chemistry-overview-reference");
      reference.setOnAction(
          e -> {
            this.chosenReferences.add(row.key());
            this.inheritedReferences.remove(row.key());
            if (reference.getValue() != null) {
              this.references.put(row.key(), reference.getValue().id());
            } else {
              this.references.remove(row.key());
            }
            referenceSource.setText("Опорный исходник для препаративного выхода");
            recalculate.run();
          });
      card.getChildren()
          .addAll(
              new Separator(),
              referenceSource,
              reference,
              result,
              note(
                  "Теория — предел 100 % по выбранному исходнику и коэффициентам; это не прогноз выхода."),
              loading);
      recalculate.run();
    }
    card.getChildren().add(provenance);
    return card;
  }

  private void quantities(
      final Row row, final String reference, final VBox box, final VBox result) {
    box.getChildren().clear();
    result.getChildren().clear();
    final var plan =
        LaboratoryStoichiometry.preparative(
            row.drawing().experiment(), row.stage(), LaboratoryStoichiometry.Basis.PLAN, reference);
    final var actual =
        LaboratoryStoichiometry.preparative(
            row.drawing().experiment(),
            row.stage(),
            LaboratoryStoichiometry.Basis.ACTUAL,
            reference);
    for (int i = 0; i < plan.rows().size(); i++) {
      final var p = plan.rows().get(i);
      final var a = actual.rows().get(i);
      final Label name =
          note(role(p.participant().role()) + " · " + p.component().molecule().displayName());
      name.setStyle("-fx-font-weight: bold;");
      final Label values =
          note("План: " + quantity(p.input()) + "    Факт: " + quantity(a.input()));
      values.setTooltip(
          new Tooltip(
              "План: "
                  + Objects.toString(p.input(), "не задан")
                  + "\nФакт: "
                  + Objects.toString(a.input(), "не задан")));
      box.getChildren().addAll(name, values);
      if (p.participant().role() == ReactionComponentRole.PRODUCT) {
        final Label yield =
            note(
                p.component().molecule().displayName()
                    + "\nФакт продукта: "
                    + quantity(a.input())
                    + " · "
                    + a.component().evidence()
                    + "\nТеория по плану: "
                    + mmol(p.theoreticalMoles())
                    + "\nТеория по фактической загрузке: "
                    + mmol(a.theoreticalMoles())
                    + "\nВыход по факту: "
                    + (a.yieldPercent() == null
                        ? "не рассчитан"
                        : number(a.yieldPercent()) + " %"));
        yield.setTooltip(
            new Tooltip(
                "Выход: "
                    + Objects.toString(a.yieldPercent(), "не рассчитан")
                    + " %\n"
                    + a.diagnostic()));
        result.getChildren().add(yield);
      }
    }
    final String detail =
        String.join("\n", actual.diagnostics())
            + "\n"
            + String.join(
                "\n",
                actual.rows().stream()
                    .map(a -> a.component().molecule().displayName() + ": " + a.diagnostic())
                    .toList());
    final TextArea text = new TextArea(detail);
    text.setEditable(false);
    text.setWrapText(true);
    text.setPrefRowCount(5);
    final TitledPane diagnostics = new TitledPane("Основание расчёта и недостающие данные", text);
    diagnostics.setExpanded(false);
    box.getChildren().add(diagnostics);
  }

  private static String role(final ReactionComponentRole role) {
    return switch (role) {
      case REACTANT -> "Исходник";
      case PRODUCT -> "Продукт";
      case SOLVENT -> "Растворитель";
      case CATALYST -> "Катализатор";
      case ADDITIVE -> "Добавка";
      case ACID -> "Кислота";
      case BASE -> "Основание";
      case LIGAND -> "Лиганд";
      case ATMOSPHERE -> "Атмосфера";
      case WORKUP -> "Обработка";
      case OTHER -> "Другое";
    };
  }

  private static String number(final BigDecimal n) {
    final BigDecimal shown = n.round(new java.math.MathContext(6));
    return (shown.compareTo(n) == 0 ? "" : "≈ ") + shown.stripTrailingZeros().toPlainString();
  }

  private static String mmol(final BigDecimal n) {
    return n == null ? "не рассчитана" : number(n.movePointRight(3)) + " ммоль";
  }

  private static String quantity(final LaboratoryQuantity q) {
    return q == null ? "не задан" : number(q.value()) + " " + q.unit();
  }
}

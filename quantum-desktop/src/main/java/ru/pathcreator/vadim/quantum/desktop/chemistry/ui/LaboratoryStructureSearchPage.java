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
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeStructureSearch;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.*;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryStructureSearch.Hit;

/** Structure search is a separate read-only task; parameter edits invalidate previous results. */
final class LaboratoryStructureSearchPage extends LaboratoryComputationPage {
  private record Choice(String id, String title, MoleculeDrawing drawing, boolean fragment) {
    @Override
    public String toString() {
      return this.title;
    }
  }

  private final ComboBox<Choice> query = new ComboBox<>();
  private final ComboBox<String> mode = new ComboBox<>(), role = new ComboBox<>();
  private final CheckBox stereo = new CheckBox("Учитывать конфигурацию и клинья");
  private final Slider threshold = new Slider(0, 100, 65);
  private final Label thresholdText = note("65 %"),
      summary = note("Выберите структуру и нажмите «Найти»."),
      rules = note("");
  private final TextField folder = new TextField(), filter = new TextField();
  private final TableView<Hit> table = new TableView<>();
  private final ScrollPane details = new ScrollPane();
  private final SplitPane split = new SplitPane();
  private final VBox list = new VBox(8), card = new VBox(10);
  private final Button find = new Button("Найти"),
      stop = new Button("Остановить"),
      open = new Button("Открыть опыт"),
      compare = new Button("Сравнить найденные"),
      inspect = new Button("Структура"),
      back = new Button("← К результатам");
  private final TextArea diagnostic = new TextArea();
  private final TitledPane diagnostics = new TitledPane("Диагностика", this.diagnostic);
  private final ExecutorService worker =
      Executors.newSingleThreadExecutor(
          task -> {
            final Thread t = new Thread(task, "laboratory-structure-search");
            t.setDaemon(true);
            return t;
          });
  private final BiConsumer<Path, String> opener;
  private final BiConsumer<Path, LaboratoryProjectOverview.Snapshot> comparison;
  private VBox header, thresholdControl;
  private Region targetPicture;
  private Future<?> task;
  private int generation;
  private boolean initializing = true, inspecting;
  private LaboratoryStructureSearch.Result result;
  private MoleculeDrawing fragmentSource;
  private Set<AtomId> fragmentAtoms = Set.of();
  private Choice submittedQuery;
  private MoleculeStructureSearch.Options submittedOptions;
  private Path submittedFolder;

  LaboratoryStructureSearchPage(
      final LaboratoryDrawing source,
      final String selected,
      final Set<AtomId> fragment,
      final Path folder,
      final BiConsumer<Path, String> opener,
      final BiConsumer<Path, LaboratoryProjectOverview.Snapshot> comparison,
      final Runnable close) {
    super("Поиск по структуре", close);
    this.opener = opener;
    this.comparison = comparison;
    this.setId("chemistry-structure-search-page");
    int ordinal = 0;
    for (final var component : source.experiment().components()) {
      this.query
          .getItems()
          .add(
              new Choice(
                  component.id(),
                  LaboratoryQuerySelector.title(component, ++ordinal),
                  source.layouts().get(component.id()),
                  false));
    }
    this.query.getSelectionModel().selectFirst();
    this.query.getItems().stream()
        .filter(c -> c.id().equals(selected))
        .findFirst()
        .ifPresent(this.query::setValue);
    if (selected != null && !fragment.isEmpty() && source.layouts().containsKey(selected)) {
      final var drawing = source.layouts().get(selected);
      this.fragmentSource = drawing;
      this.fragmentAtoms = Set.copyOf(fragment);
      final var atoms =
          drawing.molecule().atoms().stream().filter(a -> fragment.contains(a.id())).toList();
      if (!atoms.isEmpty() && atoms.size() < drawing.molecule().atomCount()) {
        final var preview =
            new ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeSketchService()
                .fragment(drawing, fragment);
        this.query
            .getItems()
            .add(
                new Choice(
                    "fragment",
                    "Выделенный фрагмент · " + atoms.size() + " атомов",
                    preview,
                    true));
      }
    }
    this.query.setPromptText("В опыте пока нет соединений");
    this.query.setId("chemistry-search-query");
    this.query.setPrefWidth(320);
    this.query.setMaxWidth(Double.MAX_VALUE);
    this.mode.getItems().setAll("Точное совпадение", "Подструктура", "Сходство");
    this.mode.getSelectionModel().select(1);
    this.mode.setId("chemistry-search-mode");
    this.mode.setPrefWidth(190);
    this.role.getItems().setAll("Все участники", "Только продукты", "Только исходники");
    this.role.getSelectionModel().selectFirst();
    this.role.setId("chemistry-search-role");
    this.stereo.setSelected(true);
    this.stereo.setId("chemistry-search-stereo");
    this.threshold.setId("chemistry-search-threshold");
    this.threshold.setPrefWidth(170);
    this.threshold.setBlockIncrement(5);
    this.threshold
        .valueProperty()
        .addListener(
            (o, a, b) -> {
              this.thresholdText.setText(Math.round(b.doubleValue()) + " %");
              this.changed();
            });
    this.folder.setText(folder == null ? "" : folder.toString());
    this.folder.setPromptText("Полный путь к папке проекта");
    this.folder.setId("chemistry-search-folder");
    this.folder.textProperty().addListener((o, a, b) -> this.changed());
    this.query
        .valueProperty()
        .addListener(
            (o, a, b) -> {
              if (b != null && b.fragment()) {
                this.mode.getSelectionModel().select(1);
                this.stereo.setSelected(false);
              }
              this.changed();
            });
    this.mode.valueProperty().addListener((o, a, b) -> this.changed());
    this.role.valueProperty().addListener((o, a, b) -> this.changed());
    this.stereo.selectedProperty().addListener((o, a, b) -> this.changed());
    final var querySelector =
        new LaboratoryQuerySelector<>(
            "Структура из текущего опыта", this.query, Choice::title, Choice::id, Choice::drawing);
    querySelector.setMinWidth(280);
    this.thresholdControl =
        labeled("Минимальное сходство", new HBox(8, this.threshold, this.thresholdText));
    this.thresholdControl.setId("chemistry-search-similarity");
    final TitledPane help = new TitledPane("Как работает поиск", this.rules);
    help.setId("chemistry-search-help");
    help.setAnimated(false);
    help.setExpanded(false);
    final VBox criteria =
        new VBox(
            10,
            new FlowPane(12, 8, labeled("Режим", this.mode), labeled("Искать среди", this.role)),
            this.stereo,
            this.thresholdControl,
            this.folderControl(),
            help);
    criteria.setMinWidth(0);
    criteria.setPrefWidth(520);
    HBox.setHgrow(criteria, Priority.ALWAYS);
    final HBox settings = new HBox(16, querySelector, criteria);
    final VBox options = new VBox(10, settings);
    final ScrollPane optionsScroll = new ScrollPane(options);
    optionsScroll.setFitToWidth(true);
    optionsScroll.setMinHeight(0);
    optionsScroll.setPrefViewportHeight(180);
    this.fitOptions(
        optionsScroll,
        options,
        this.heightProperty(),
        this.widthProperty(),
        this.query.valueProperty(),
        help.expandedProperty(),
        this.thresholdControl.managedProperty(),
        this.folder.managedProperty(),
        this.folder.textProperty(),
        this.rules.textProperty());
    help.expandedProperty()
        .addListener(
            (o, a, b) -> {
              if (!b) optionsScroll.setVvalue(optionsScroll.getVmin());
            });
    final TitledPane settingsPane = new TitledPane("Параметры поиска", optionsScroll);
    settingsPane.setId("chemistry-search-settings");
    settingsPane.setAnimated(false);
    settingsPane.setExpanded(true);
    this.find.setId("chemistry-search-run");
    this.find.getStyleClass().add("laboratory-save");
    this.find.setOnAction(e -> this.search());
    this.stop.setId("chemistry-search-stop");
    this.stop.setDisable(true);
    this.stop.setOnAction(e -> this.cancel());
    this.stop.visibleProperty().bind(this.stop.disabledProperty().not());
    this.stop.managedProperty().bind(this.stop.visibleProperty());
    final HBox searchActions = new HBox(10, this.find, this.stop, this.summary);
    searchActions.setMinHeight(Region.USE_PREF_SIZE);
    this.summary.setMaxWidth(Double.MAX_VALUE);
    HBox.setHgrow(this.summary, Priority.ALWAYS);
    final VBox header = new VBox(10, settingsPane, searchActions);
    header.setMinHeight(Region.USE_PREF_SIZE);
    header.setPadding(new Insets(0, 16, 10, 16));
    this.header = header;
    this.filter.setId("chemistry-search-filter");
    this.filter.setPromptText("Уточнить результаты по имени, формуле, опыту…");
    this.filter.textProperty().addListener((o, a, b) -> this.filter());
    this.table.setId("chemistry-search-results");
    this.table.setMinSize(0, 0);
    this.table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    final TableColumn<Hit, Hit> thumbnail = new TableColumn<>("Структура");
    thumbnail.setPrefWidth(110);
    thumbnail.setMinWidth(100);
    thumbnail.setMaxWidth(140);
    thumbnail.setSortable(false);
    thumbnail.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue()));
    thumbnail.setCellFactory(
        v ->
            new TableCell<>() {
              @Override
              protected void updateItem(final Hit hit, final boolean empty) {
                super.updateItem(hit, empty);
                this.setGraphic(
                    empty || hit == null
                        ? null
                        : picture(hit.drawing().layouts().get(hit.component().id()), Set.of(), 72));
              }
            });
    final TableColumn<Hit, Number> score = new TableColumn<>("Совпадение");
    score.setPrefWidth(130);
    score.setMinWidth(115);
    score.setCellValueFactory(v -> new ReadOnlyDoubleWrapper(v.getValue().match().score()));
    score.setCellFactory(
        v ->
            new TableCell<>() {
              @Override
              protected void updateItem(final Number value, final boolean empty) {
                super.updateItem(value, empty);
                this.setText(
                    empty || value == null
                        ? null
                        : LaboratoryStructureSearchPage.this.submittedOptions != null
                                && LaboratoryStructureSearchPage.this.submittedOptions.mode()
                                    == MoleculeStructureSearch.Mode.SIMILARITY
                            ? String.format(Locale.ROOT, "%.1f %%", value.doubleValue() * 100)
                            : LaboratoryStructureSearchPage.this.submittedOptions != null
                                    && LaboratoryStructureSearchPage.this.submittedOptions.mode()
                                        == MoleculeStructureSearch.Mode.SUBSTRUCTURE
                                ? "Фрагмент найден"
                                : "Совпадение");
              }
            });
    this.table
        .getColumns()
        .addAll(
            thumbnail,
            column(
                "Соединение / опыт",
                h ->
                    h.component().molecule().displayName()
                        + "\n"
                        + h.drawing().experiment().title(),
                270),
            score);
    this.table.setPlaceholder(note("Результатов пока нет. Поиск читает сохранённые QCHEM."));
    this.table.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> this.detail());
    this.table.setRowFactory(
        v -> {
          final TableRow<Hit> row = new TableRow<>();
          row.setOnMouseClicked(
              e -> {
                if (e.getButton() == MouseButton.PRIMARY
                    && e.getClickCount() == 2
                    && !row.isEmpty()) {
                  this.open(row.getItem());
                }
              });
          return row;
        });
    this.table.setOnKeyPressed(
        e -> {
          if (e.getCode() == KeyCode.ENTER) {
            this.openSelected();
            e.consume();
          }
        });
    this.list.getChildren().setAll(this.filter, this.table);
    this.list.setMinSize(0, 0);
    VBox.setVgrow(this.table, Priority.ALWAYS);
    this.card.setPadding(new Insets(14));
    this.card.setMinWidth(0);
    this.card.getStyleClass().add("laboratory-search-card");
    this.details.setContent(this.card);
    this.details.setFitToWidth(true);
    this.details.setMinSize(0, 0);
    this.details.setId("chemistry-search-detail");
    this.details.viewportBoundsProperty().addListener((o, a, b) -> this.fitPicture());
    this.split.setMinSize(0, 0);
    this.split.getItems().addAll(this.list, this.details);
    this.split.setDividerPositions(.48);
    this.split
        .widthProperty()
        .addListener(
            (o, a, b) -> {
              if (a.doubleValue() <= 0 && b.doubleValue() > 0) {
                Platform.runLater(
                    () -> {
                      if (!this.disposed && this.split.getItems().size() == 2) {
                        this.split.setDividerPositions(.48);
                      }
                    });
              }
            });
    this.open.setId("chemistry-search-open");
    this.open.setOnAction(e -> this.openSelected());
    this.compare.setId("chemistry-search-compare");
    this.compare.setOnAction(e -> this.compare());
    this.inspect.setId("chemistry-search-inspect");
    this.inspect.setOnAction(
        e -> {
          this.inspecting = true;
          this.arrange();
        });
    this.back.setId("chemistry-search-list");
    this.back.setOnAction(
        e -> {
          this.inspecting = false;
          this.arrange();
        });
    final VBox body =
        new VBox(
            10, this.split, new FlowPane(8, 8, this.back, this.inspect, this.open, this.compare));
    body.setMinHeight(0);
    body.setPadding(new Insets(0, 16, 10, 16));
    VBox.setVgrow(this.split, Priority.ALWAYS);
    final BorderPane content = new BorderPane(body);
    content.setTop(header);
    this.setCenter(content);
    this.diagnostic.setEditable(false);
    this.diagnostic.setWrapText(true);
    this.diagnostic.setPrefRowCount(3);
    this.diagnostics.setExpanded(false);
    this.diagnostics.setAnimated(false);
    this.diagnostics.setVisible(false);
    this.diagnostics.setManaged(false);
    final VBox footer = new VBox(6, this.status, this.diagnostics);
    footer.setPadding(new Insets(4, 16, 10, 16));
    this.setBottom(footer);
    this.widthProperty()
        .addListener(
            (o, a, b) -> {
              if (b.doubleValue() > 0
                  && (a.doubleValue() == 0 || (a.doubleValue() < 900) != (b.doubleValue() < 900))) {
                settingsPane.setExpanded(b.doubleValue() >= 900 || this.result == null);
              }
              this.arrange();
            });
    this.addEventFilter(
        KeyEvent.KEY_PRESSED,
        e -> {
          if (e.isShortcutDown() && e.getCode() == KeyCode.F) {
            this.filter.requestFocus();
            this.filter.selectAll();
            e.consume();
          } else if (e.getCode() == KeyCode.ESCAPE) {
            if (this.running) {
              this.cancel();
            } else if (this.inspecting) {
              this.inspecting = false;
              this.arrange();
            } else {
              this.finish();
            }
            e.consume();
          }
        });
    this.initializing = false;
    this.changed();
    this.arrange();
  }

  private void fitOptions(
      final ScrollPane pane, final VBox content, final javafx.beans.Observable... changes) {
    final boolean[] queued = {false};
    final Runnable schedule =
        () -> {
          if (queued[0] || this.disposed) return;
          queued[0] = true;
          Platform.runLater(
              () -> {
                queued[0] = false;
                if (this.disposed) return;
                content.applyCss();
                for (final Node child : content.getChildren())
                  if (child instanceof Region region) region.requestLayout();
                content.requestLayout();
                final double width =
                    pane.getViewportBounds().getWidth() > 0
                        ? pane.getViewportBounds().getWidth()
                        : Math.max(280, this.getWidth() - 70);
                final double available = Math.max(120, Math.min(360, this.getHeight() - 390));
                final double preferred =
                    Math.max(120, Math.min(content.prefHeight(width), available));
                if (Math.abs(pane.getPrefViewportHeight() - preferred) > .5)
                  pane.setPrefViewportHeight(preferred);
                pane.requestLayout();
              });
        };
    for (final javafx.beans.Observable value : changes)
      value.addListener(observable -> schedule.run());
    pane.viewportBoundsProperty()
        .addListener(
            (o, a, b) -> {
              if (Math.abs(a.getWidth() - b.getWidth()) > .5) schedule.run();
            });
    this.sceneProperty().addListener(o -> schedule.run());
    schedule.run();
  }

  private VBox folderControl() {
    final Label name = note("");
    name.setMaxWidth(Double.MAX_VALUE);
    final Tooltip exact = new Tooltip();
    name.setTooltip(exact);
    final Runnable refresh =
        () -> {
          final String raw = this.folder.getText();
          exact.setText(raw);
          String label = "Не выбрана";
          if (!raw.isBlank()) {
            try {
              final Path path = Path.of(raw);
              label = Objects.toString(path.getFileName(), path.toString());
            } catch (final RuntimeException ignored) {
              label = "Проверьте путь";
            }
          }
          name.setText("Папка: " + label);
        };
    this.folder.textProperty().addListener(o -> refresh.run());
    refresh.run();
    this.folder.setVisible(false);
    this.folder.setManaged(false);
    final Button choose = new Button("Выбрать…");
    choose.setId("chemistry-search-choose-folder");
    choose.setOnAction(e -> this.chooseFolder());
    final Hyperlink path = new Hyperlink("Путь");
    path.setId("chemistry-search-edit-path");
    path.setOnAction(
        e -> {
          final boolean shown = !this.folder.isVisible();
          this.folder.setVisible(shown);
          this.folder.setManaged(shown);
          if (shown) this.folder.requestFocus();
        });
    final FlowPane location = new FlowPane(8, 4, name, choose, path);
    return new VBox(5, location, this.folder);
  }

  private void chooseFolder() {
    final DirectoryChooser chooser = new DirectoryChooser();
    chooser.setTitle("Папка сохранённых опытов");
    try {
      if (!this.folder.getText().isBlank()) {
        final Path path = Path.of(this.folder.getText());
        if (java.nio.file.Files.isDirectory(path)) chooser.setInitialDirectory(path.toFile());
      }
    } catch (final RuntimeException ignored) {
    }
    try {
      final var selected =
          chooser.showDialog(this.getScene() == null ? null : this.getScene().getWindow());
      if (selected != null) this.folder.setText(selected.toPath().toString());
    } catch (final RuntimeException error) {
      this.status.setText(error.getMessage());
    }
  }

  private static VBox labeled(final String title, final Node control) {
    return new VBox(4, note(title), control);
  }

  private static TableColumn<Hit, String> column(
      final String title, final Function<Hit, String> value, final double width) {
    final TableColumn<Hit, String> column = new TableColumn<>(title);
    column.setPrefWidth(width);
    column.setMinWidth(80);
    column.setCellValueFactory(r -> new ReadOnlyStringWrapper(value.apply(r.getValue())));
    return column;
  }

  private void changed() {
    if (this.initializing) {
      return;
    }
    if (this.running) {
      this.cancel();
    }
    this.result = null;
    this.targetPicture = null;
    this.table.getItems().clear();
    this.card.getChildren().clear();
    this.inspecting = false;
    final boolean similar = this.mode.getSelectionModel().getSelectedIndex() == 2;
    this.threshold.setDisable(!similar);
    this.thresholdControl.setVisible(similar);
    this.thresholdControl.setManaged(similar);
    this.stereo.setDisable(
        similar || this.query.getValue() != null && this.query.getValue().fragment());
    this.find.setDisable(this.query.getValue() == null);
    this.summary.setText(
        this.query.getValue() == null
            ? "Вернитесь к опыту и добавьте соединение для поиска."
            : "Параметры изменены · нажмите «Найти»");
    this.rules.setText(
        similar
            ? "Сходство Танимото по структурным путям CDK 1024/7. Стереохимия не участвует; 100 % не доказывает тождество вещества."
            : "Сравниваются элементы, изотопы, заряды и связи. Обычные H не ограничивают поиск фрагмента; при полном совпадении их число учитывается. Записанные R/S, E/Z и клинья проверяются существующим сервисом стереоидентичности. Относительные группы и неоднозначная конфигурация отмечаются как несопоставленные.");
    ((TitledPane) this.lookup("#chemistry-search-settings")).setText("Параметры поиска");
    this.status.setText(
        "Запрос берётся из текущего опыта; поиск читает сохранённые файлы и не изменяет их.");
    this.diagnostics.setVisible(false);
    this.diagnostics.setManaged(false);
    this.actions();
    this.arrange();
  }

  private void search() {
    if (this.running || this.query.getValue() == null) {
      return;
    }
    final Choice choice = this.query.getValue();
    final Path directory;
    final MoleculeStructureSearch.Options options;
    try {
      directory = Path.of(this.folder.getText().strip());
      if (this.folder.getText().isBlank() || !directory.isAbsolute()) {
        throw new IllegalArgumentException("Укажите полный путь к папке проекта.");
      }
      options =
          new MoleculeStructureSearch.Options(
              MoleculeStructureSearch.Mode.values()[
                  this.mode.getSelectionModel().getSelectedIndex()],
              this.stereo.isSelected(),
              Math.round(this.threshold.getValue()) / 100.0);
    } catch (final RuntimeException e) {
      this.folder.setVisible(true);
      this.folder.setManaged(true);
      ((TitledPane) this.lookup("#chemistry-search-settings")).setExpanded(true);
      this.status.setText(e.getMessage());
      return;
    }
    final ReactionComponentRole role =
        switch (this.role.getSelectionModel().getSelectedIndex()) {
          case 1 -> ReactionComponentRole.PRODUCT;
          case 2 -> ReactionComponentRole.REACTANT;
          default -> null;
        };
    final int request = ++this.generation;
    this.result = null;
    this.table.getItems().clear();
    this.card.getChildren().clear();
    this.targetPicture = null;
    this.diagnostics.setVisible(false);
    this.diagnostics.setManaged(false);
    this.actions();
    this.busy(true);
    this.find.setDisable(true);
    this.stop.setDisable(false);
    this.summary.setText("Ищу…");
    this.task =
        this.worker.submit(
            () -> {
              try {
                final var found =
                    new LaboratoryStructureSearch()
                        .search(
                            directory,
                            choice.fragment() ? this.fragmentSource : choice.drawing(),
                            choice.fragment() ? this.fragmentAtoms : Set.of(),
                            options,
                            role,
                            () -> Thread.currentThread().isInterrupted(),
                            message ->
                                Platform.runLater(
                                    () -> {
                                      if (!this.disposed && request == this.generation) {
                                        this.status.setText(message);
                                      }
                                    }));
                Platform.runLater(
                    () -> {
                      if (this.disposed || request != this.generation) {
                        return;
                      }
                      this.submittedQuery = choice;
                      this.submittedOptions = options;
                      this.submittedFolder = directory;
                      this.result = found;
                      this.loaded();
                      this.diagnostic.setText(String.join("\n", found.diagnostics()));
                      this.diagnostics.setText(
                          "Пропуски и ограничения · " + found.diagnostics().size());
                      this.diagnostics.setVisible(!found.diagnostics().isEmpty());
                      this.diagnostics.setManaged(!found.diagnostics().isEmpty());
                      final TitledPane settings =
                          (TitledPane) this.lookup("#chemistry-search-settings");
                      settings.setText("Запрос: " + choice.title() + " · " + this.mode.getValue());
                      settings.setExpanded(false);
                      this.filter();
                      this.status.setText(
                          found.diagnostics().isEmpty()
                              ? "Поиск завершён. Зелёным выделен найденный фрагмент; при поиске сходства подсветка не применяется."
                              : "Поиск завершён не полностью: откройте пропуски и ограничения. Нулевой результат не исключает совпадение в пропущенных структурах.");
                    });
              } catch (final CancellationException ignored) {
              } catch (final Exception e) {
                Platform.runLater(
                    () -> {
                      if (!this.disposed && request == this.generation) {
                        this.loaded();
                        this.summary.setText("Поиск не выполнен");
                        this.status.setText(e.getMessage());
                      }
                    });
              }
            });
  }

  private void loaded() {
    this.busy(false);
    this.find.setDisable(this.query.getValue() == null);
    this.stop.setDisable(true);
  }

  private void cancel() {
    if (this.task != null) {
      this.task.cancel(true);
    }
    this.generation++;
    this.loaded();
    this.summary.setText("Поиск остановлен");
    this.status.setText("Запрос остановлен; незавершённые результаты не показаны.");
  }

  private void filter() {
    if (this.result == null) {
      this.actions();
      return;
    }
    final Hit previous = this.table.getSelectionModel().getSelectedItem();
    final String[] words = LaboratoryExperimentIndex.normalize(this.filter.getText()).split(" ");
    this.table
        .getItems()
        .setAll(
            this.result.hits().stream()
                .filter(
                    h -> {
                      final String text =
                          LaboratoryExperimentIndex.normalize(
                              h.path()
                                  + " "
                                  + h.component().molecule().displayName()
                                  + " "
                                  + h.component().molecule().formula().hillNotation()
                                  + " "
                                  + h.drawing().experiment().title());
                      return Arrays.stream(words).allMatch(text::contains);
                    })
                .toList());
    this.table
        .getSelectionModel()
        .select(
            this.table.getItems().stream()
                .filter(h -> previous != null && h.key().equals(previous.key()))
                .findFirst()
                .orElse(this.table.getItems().isEmpty() ? null : this.table.getItems().getFirst()));
    this.summary.setText(
        "Найдено: "
            + this.table.getItems().size()
            + " из "
            + this.result.hits().size()
            + " · проверено "
            + this.result.examined()
            + " структур / "
            + this.result.documents()
            + " опытов");
    this.actions();
  }

  private void actions() {
    final boolean empty = this.table.getSelectionModel().getSelectedItem() == null;
    this.open.setDisable(empty);
    this.inspect.setDisable(empty);
    this.compare.setDisable(
        this.table.getItems().stream().noneMatch(hit -> !hit.stages().isEmpty()));
  }

  private void arrange() {
    final boolean compact = this.getWidth() < 1000;
    final List<Node> nodes =
        this.inspecting
            ? List.of(this.details)
            : compact ? List.of(this.list) : List.of(this.list, this.details);
    if (!this.split.getItems().equals(nodes)) {
      this.split.getItems().setAll(nodes);
      if (nodes.size() == 2) {
        this.split.setDividerPositions(.48);
      }
    }
    if (this.header != null) {
      this.header.setVisible(!this.inspecting);
      this.header.setManaged(!this.inspecting);
    }
    this.back.setVisible(this.inspecting);
    this.back.setManaged(this.inspecting);
    this.inspect.setVisible(!this.inspecting);
    this.inspect.setManaged(!this.inspecting);
  }

  private void detail() {
    this.card.getChildren().clear();
    final Hit hit = this.table.getSelectionModel().getSelectedItem();
    this.actions();
    if (hit == null) {
      return;
    }
    final Label title = note(hit.component().molecule().displayName());
    title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
    this.targetPicture =
        (Region)
            picture(hit.drawing().layouts().get(hit.component().id()), hit.match().atoms(), 280);
    this.fitPicture();
    this.card
        .getChildren()
        .addAll(
            title,
            note(
                hit.component().molecule().formula().hillNotation()
                    + " · "
                    + hit.drawing().experiment().title()),
            this.targetPicture);
    final TitledPane queryPicture =
        new TitledPane(
            "Запрос: " + this.submittedQuery.title(),
            picture(this.submittedQuery.drawing(), Set.of(), 190));
    queryPicture.setExpanded(false);
    queryPicture.setAnimated(false);
    this.card.getChildren().add(queryPicture);
    this.card
        .getChildren()
        .addAll(
            note(
                "Стадии: "
                    + String.join(
                        " · ",
                        hit.stages().stream()
                            .map(LaboratoryProjectOverview.Row::stageTitle)
                            .toList())),
            note(
                "План: "
                    + Objects.toString(hit.component().planned(), "не задан")
                    + " · "
                    + hit.component().evidence().quantityLabel()
                    + ": "
                    + Objects.toString(hit.component().actual(), "не задан")),
            note(this.submittedFolder.relativize(hit.path()).toString()));
    if (this.submittedOptions.mode() == MoleculeStructureSearch.Mode.SIMILARITY) {
      this.card
          .getChildren()
          .add(
              note(
                  "Сходство: "
                      + String.format(Locale.ROOT, "%.1f %%", hit.match().score() * 100)
                      + " · одинаковый отпечаток не означает одинаковую структуру."));
    } else {
      this.card
          .getChildren()
          .add(
              note(
                  "Выделено одно найденное соответствие. "
                      + (this.submittedOptions.stereo()
                          ? "Поддержанные R/S, E/Z и клинья сопоставлены; это не доказывает полноту стереохимического описания."
                          : "Стереометки не сравнивались.")));
    }
  }

  private void fitPicture() {
    if (this.targetPicture != null) {
      final double height =
          Math.max(130, Math.min(320, this.details.getViewportBounds().getHeight() - 95));
      this.targetPicture.setMinHeight(height);
      this.targetPicture.setPrefHeight(height);
    }
  }

  private static Node picture(
      final MoleculeDrawing drawing, final Set<AtomId> highlight, final double height) {
    final Canvas canvas = new Canvas(340, height);
    final StackPane panel = new StackPane(canvas);
    panel.setMinWidth(0);
    panel.setPrefWidth(0);
    panel.setMinHeight(height);
    panel.setPrefHeight(height);
    canvas.widthProperty().bind(panel.widthProperty());
    canvas.heightProperty().bind(panel.heightProperty());
    final Runnable paint =
        () -> {
          final double available = canvas.getHeight();
          final var g = canvas.getGraphicsContext2D();
          LaboratoryPreviewTheme.clear(g);
          final var bounds = LaboratoryCanvas.previewBounds(drawing);
          final double scale =
              Math.max(
                  .01,
                  Math.min(
                      1.2,
                      Math.min(
                          (canvas.getWidth() - 32) / Math.max(1, bounds.getWidth()),
                          (available - 24) / Math.max(1, bounds.getHeight()))));
          g.save();
          g.translate(
              canvas.getWidth() / 2 - (bounds.getMinX() + bounds.getMaxX()) * scale / 2,
              available / 2 - (bounds.getMinY() + bounds.getMaxY()) * scale / 2);
          g.scale(scale, scale);
          g.setFill(LaboratoryPreviewTheme.highlight(canvas));
          for (final var atom : LaboratoryCanvas.previewAtoms(drawing)) {
            if (highlight.contains(atom.atomId())) {
              g.fillOval(atom.point2D().x() - 11, atom.point2D().y() - 11, 22, 22);
            }
          }
          LaboratoryPreviewTheme.paint(g, drawing);
          g.restore();
        };
    LaboratoryPreviewTheme.track(canvas, paint);
    canvas.widthProperty().addListener((o, a, b) -> paint.run());
    canvas.heightProperty().addListener((o, a, b) -> paint.run());
    paint.run();
    return panel;
  }

  private void openSelected() {
    final Hit hit = this.table.getSelectionModel().getSelectedItem();
    if (hit != null) {
      this.open(hit);
    }
  }

  private void open(final Hit hit) {
    this.opener.accept(hit.path(), hit.component().id());
  }

  private void compare() {
    if (this.result == null || this.table.getItems().isEmpty()) {
      return;
    }
    final Map<String, LaboratoryProjectOverview.Row> rows = new LinkedHashMap<>();
    this.table.getItems().forEach(h -> h.stages().forEach(s -> rows.put(s.key(), s)));
    this.comparison.accept(
        this.submittedFolder,
        new LaboratoryProjectOverview.Snapshot(
            List.copyOf(rows.values()), this.result.diagnostics()));
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
    if (this.task != null) {
      this.task.cancel(true);
    }
    this.worker.shutdownNow();
  }
}

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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionOperations.OutcomeStatus;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionOperations.Origin;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.*;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryReactionSearch.*;

/** Personal transformation and method library, built from the chemist's saved experiments. */
final class LaboratoryReactionSearchPage extends LaboratoryComputationPage {
  private record Choice(String id, String title, MoleculeDrawing drawing) {
    public String toString() {
      return this.title;
    }
  }

  private record Reference(String id, String title) {
    public String toString() {
      return this.title;
    }
  }

  private final ComboBox<Choice> source = new ComboBox<>(), product = new ComboBox<>();
  private final ComboBox<String> mode = new ComboBox<>();
  private final ComboBox<Outcome> outcome = new ComboBox<>();

  private record AssessmentStatus(OutcomeStatus value) {
    public String toString() {
      return this.value == null ? "Любой исход" : this.value.toString();
    }
  }

  private record AssessmentOrigin(Origin value) {
    public String toString() {
      return this.value == null ? "Любое происхождение исхода" : this.value.toString();
    }
  }

  private final ComboBox<AssessmentStatus> stageOutcome = new ComboBox<>();
  private final ComboBox<AssessmentOrigin> stageOrigin = new ComboBox<>();
  private final CheckBox byproducts = new CheckBox("Есть указанные побочные продукты");
  private final CheckBox stereo = new CheckBox("Учитывать заданную стереохимию");
  private final Slider threshold = new Slider(0, 100, 65);
  private final Label thresholdText = note("65 %"),
      summary = note("Задайте превращение или слова из методики."),
      scope = note(""),
      rules = note("");
  private final Label yieldScope = note(""), repeatScope = note(""), filterState = note("");
  private final TitledPane extraFilters = new TitledPane();
  private VBox thresholdControl;
  private final TextField folder = new TextField(),
      text = new TextField(),
      minimum = new TextField(),
      maximum = new TextField(),
      localFilter = new TextField();
  private final ListView<Hit> results = new ListView<>();
  private final VBox card = new VBox(12), list = new VBox(8);
  private final ScrollPane detail = scroll(this.card);
  private final SplitPane split = new SplitPane();
  private final Button find = new Button("Найти превращения"),
      stop = new Button("Остановить"),
      open = new Button("Открыть опыт"),
      repeat = new Button("Повторить опыт"),
      inspect = new Button("Методика и результат"),
      back = new Button("← К результатам");
  private final TextArea diagnostic = new TextArea();
  private final TitledPane diagnostics = new TitledPane("Пропуски и ограничения", this.diagnostic),
      settings;
  private final BiConsumer<Path, String> opener;
  private final Consumer<Path> repeater;
  private final ExecutorService worker =
      Executors.newSingleThreadExecutor(
          r -> {
            final Thread t = new Thread(r, "laboratory-reaction-search");
            t.setDaemon(true);
            return t;
          });
  private Future<?> task;
  private int generation;
  private boolean initializing = true, inspecting;
  private Result result;
  private Request submitted;
  private VBox header;

  /** The open callback selects a stage ID in the source experiment; repeat receives the saved source file. */
  LaboratoryReactionSearchPage(
      final LaboratoryDrawing current,
      final Path directory,
      final BiConsumer<Path, String> open,
      final Consumer<Path> repeat,
      final Runnable close) {
    super("Превращения и мои методики", close);
    this.opener = Objects.requireNonNull(open);
    this.repeater = Objects.requireNonNull(repeat);
    this.setId("chemistry-reaction-search-page");
    this.source.getItems().add(new Choice(null, "Любой исходник", null));
    this.product.getItems().add(new Choice(null, "Любой продукт", null));
    int ordinal = 0;
    for (final var component : current.experiment().components()) {
      final var choice =
          new Choice(
              component.id(),
              LaboratoryQuerySelector.title(component, ++ordinal),
              current.layouts().get(component.id()));
      this.source.getItems().add(choice);
      this.product.getItems().add(choice);
    }
    this.source.getSelectionModel().selectFirst();
    this.product.getSelectionModel().selectFirst();
    this.source.setId("reaction-search-source");
    this.product.setId("reaction-search-product");
    this.source.setPrefWidth(270);
    this.product.setPrefWidth(270);
    this.mode.getItems().setAll("Точное совпадение", "Подструктура", "Сходство");
    this.mode.getSelectionModel().select(1);
    this.mode.setId("reaction-search-mode");
    this.mode.setPrefWidth(190);
    this.outcome.getItems().setAll(Outcome.values());
    this.outcome.setValue(Outcome.ALL);
    this.outcome.setId("reaction-search-outcome");
    this.outcome.setCellFactory(v -> outcomeCell());
    this.outcome.setButtonCell(outcomeCell());
    this.outcome.setTooltip(
        new Tooltip(
            "Происхождение записанного количества продукта. Для поиска литературной методики используйте текст, DOI или автора."));
    this.stereo.setSelected(true);
    this.stereo.setId("reaction-search-stereo");
    this.stageOutcome.getItems().add(new AssessmentStatus(null));
    for (final var value : OutcomeStatus.values()) {
      this.stageOutcome.getItems().add(new AssessmentStatus(value));
    }
    this.stageOrigin.getItems().add(new AssessmentOrigin(null));
    for (final var value : Origin.values()) {
      if (value != Origin.UNSPECIFIED) {
        this.stageOrigin.getItems().add(new AssessmentOrigin(value));
      }
    }
    this.stageOutcome.getSelectionModel().selectFirst();
    this.stageOrigin.getSelectionModel().selectFirst();
    this.stageOutcome.setId("reaction-search-stage-outcome");
    this.stageOrigin.setId("reaction-search-stage-origin");
    this.byproducts.setId("reaction-search-byproducts");
    this.stageOutcome.setPrefWidth(230);
    this.stageOrigin.setPrefWidth(255);
    this.stageOutcome.setTooltip(
        new Tooltip(
            "Явное заключение автора для конкретной стадии. Отсутствие измеренного количества не определяет исход."));
    this.byproducts.setTooltip(
        new Tooltip("Только стадии с явно указанными побочными продуктами в текущем заключении."));
    this.threshold.setPrefWidth(140);
    this.threshold.setBlockIncrement(5);
    this.threshold.setId("reaction-search-threshold");
    this.folder.setText(directory == null ? "" : directory.toString());
    this.folder.setPromptText("Папка сохранённых опытов");
    this.folder.setId("reaction-search-folder");
    this.text.setPromptText("Условия, текст методики, DOI, автор или причина неудачи…");
    this.text.setId("reaction-search-text");
    this.minimum.setPromptText("От");
    this.maximum.setPromptText("До");
    this.minimum.setPrefColumnCount(6);
    this.maximum.setPrefColumnCount(6);
    this.minimum.setId("reaction-search-minimum");
    this.maximum.setId("reaction-search-maximum");
    this.thresholdControl =
        field("Сходство каждой стороны", new HBox(6, this.threshold, this.thresholdText));
    this.thresholdControl.setId("reaction-search-similarity");
    final VBox comparison =
        new VBox(10, field("Сопоставление", this.mode), this.stereo, this.thresholdControl);
    final FlowPane structure =
        new FlowPane(
            12,
            8,
            new LaboratoryQuerySelector<>(
                "Исходник запроса", this.source, Choice::title, Choice::id, Choice::drawing),
            note("→"),
            new LaboratoryQuerySelector<>(
                "Продукт запроса", this.product, Choice::title, Choice::id, Choice::drawing),
            comparison);
    final FlowPane conditions =
        new FlowPane(
            12,
            8,
            field("Количество продукта", this.outcome),
            field("Фактический выход, %", new HBox(6, this.minimum, note("—"), this.maximum)));
    final FlowPane assessment =
        new FlowPane(
            12,
            8,
            field("Исход стадии", this.stageOutcome),
            field("Происхождение заключения", this.stageOrigin),
            this.byproducts);
    final FlowPane shortcuts =
        new FlowPane(
            6,
            6,
            quick("Сбросить фильтры", this::clearFilters),
            quick("Есть продукт", () -> this.outcome.setValue(Outcome.POSITIVE)),
            quick("Нет своего измерения", () -> this.outcome.setValue(Outcome.UNRECORDED)),
            quick(
                "Цель не получена",
                () ->
                    this.stageOutcome.setValue(
                        new AssessmentStatus(OutcomeStatus.TARGET_NOT_OBTAINED))),
            quick("Побочные продукты", () -> this.byproducts.setSelected(true)));
    this.yieldScope.setId("reaction-search-yield-scope");
    this.extraFilters.setText("Дополнительные фильтры");
    this.extraFilters.setId("reaction-search-extra-filters");
    this.extraFilters.setContent(new VBox(10, assessment, conditions, this.yieldScope, shortcuts));
    this.extraFilters.setAnimated(false);
    this.extraFilters.setExpanded(false);
    final TitledPane help = new TitledPane("Как работает поиск", this.rules);
    help.setId("reaction-search-help");
    help.setAnimated(false);
    help.setExpanded(false);
    final VBox options =
        new VBox(
            8,
            structure,
            field("Текст методики, условия или DOI", this.text),
            this.folderControl(),
            this.extraFilters,
            help);
    final ScrollPane optionsScroll = scroll(options);
    optionsScroll.setFitToHeight(false);
    optionsScroll.setPrefViewportHeight(240);
    this.fitOptions(
        optionsScroll,
        options,
        this.heightProperty(),
        this.widthProperty(),
        this.source.valueProperty(),
        this.product.valueProperty(),
        this.thresholdControl.managedProperty(),
        this.extraFilters.expandedProperty(),
        help.expandedProperty(),
        this.folder.managedProperty(),
        this.folder.textProperty(),
        this.rules.textProperty(),
        this.yieldScope.textProperty());
    for (final TitledPane disclosure : List.of(this.extraFilters, help))
      disclosure
          .expandedProperty()
          .addListener(
              (o, a, b) -> {
                if (!b) optionsScroll.setVvalue(optionsScroll.getVmin());
              });
    this.settings = new TitledPane("Параметры поиска", optionsScroll);
    this.settings.setAnimated(false);
    this.settings.setId("reaction-search-settings");
    this.find.setId("reaction-search-run");
    this.find.getStyleClass().add("laboratory-save");
    this.find.setOnAction(e -> this.search());
    this.stop.setId("reaction-search-stop");
    this.stop.setDisable(true);
    this.stop.visibleProperty().bind(this.stop.disabledProperty().not());
    this.stop.managedProperty().bind(this.stop.visibleProperty());
    this.stop.setOnAction(e -> this.cancel());
    this.scope.setText("Ищем в сохранённых опытах; незаписанные изменения не включены.");
    this.filterState.setId("reaction-search-active-filters");
    this.header =
        new VBox(
            8,
            this.settings,
            new FlowPane(8, 8, this.find, this.stop, this.summary),
            this.filterState,
            this.scope);
    this.header.setPadding(new Insets(0, 16, 10, 16));
    this.localFilter.setPromptText("Уточнить найденное по названию опыта, стадии или соединению");
    this.localFilter.setId("reaction-search-filter");
    this.localFilter.textProperty().addListener(o -> this.filter());
    this.results.setId("reaction-search-results");
    this.results.setPlaceholder(
        note(
            "Результаты появятся после поиска.\nМожно искать по тексту, структуре или исходу стадии."));
    this.results.setMinSize(0, 0);
    this.results.setCellFactory(
        view ->
            new ListCell<>() {
              protected void updateItem(final Hit hit, final boolean empty) {
                super.updateItem(hit, empty);
                this.setText(null);
                if (empty || hit == null) {
                  this.setGraphic(null);
                  return;
                }
                final Label title = note(hit.stage().title());
                title.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
                final HBox pictures = resultPictures(hit, view);
                final VBox content =
                    new VBox(
                        6,
                        title,
                        note(hit.drawing().experiment().title()),
                        pictures,
                        note(resultText(hit)));
                content.setPadding(new Insets(7));
                content.prefWidthProperty().bind(view.widthProperty().subtract(40));
                this.setGraphic(content);
              }
            });
    this.results.getSelectionModel().selectedItemProperty().addListener(o -> this.showDetail());
    this.results.setOnMouseClicked(
        e -> {
          if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
            this.openSelected();
          }
        });
    this.results.setOnKeyPressed(
        e -> {
          if (e.getCode() == KeyCode.ENTER) {
            this.openSelected();
            e.consume();
          }
        });
    this.list.getChildren().setAll(this.localFilter, this.results);
    this.list.setMinSize(0, 0);
    VBox.setVgrow(this.results, Priority.ALWAYS);
    this.card.setPadding(new Insets(14));
    this.card.setMinWidth(0);
    this.detail.setFitToHeight(false);
    this.detail.setId("reaction-search-detail");
    this.split.getItems().setAll(this.list, this.detail);
    this.split.setDividerPositions(.43);
    this.split.setMinSize(0, 0);
    this.open.setId("reaction-search-open");
    this.open.setOnAction(e -> this.openSelected());
    this.repeat.setId("reaction-search-repeat");
    this.repeat.setOnAction(
        e -> {
          final var hit = this.selected();
          if (hit != null) {
            this.repeater.accept(hit.path());
          }
        });
    this.inspect.setId("reaction-search-inspect");
    this.inspect.setOnAction(
        e -> {
          this.inspecting = true;
          this.arrange();
        });
    this.back.setOnAction(
        e -> {
          this.inspecting = false;
          this.arrange();
        });
    final VBox body =
        new VBox(
            8,
            this.split,
            new FlowPane(8, 8, this.back, this.inspect, this.open, this.repeat, this.repeatScope));
    body.setPadding(new Insets(0, 16, 8, 16));
    VBox.setVgrow(this.split, Priority.ALWAYS);
    final BorderPane center = new BorderPane(body);
    center.setTop(this.header);
    this.setCenter(center);
    this.diagnostic.setEditable(false);
    this.diagnostic.setWrapText(true);
    this.diagnostic.setPrefRowCount(4);
    this.diagnostics.setAnimated(false);
    this.diagnostics.setExpanded(false);
    visible(this.diagnostics, false);
    final VBox footer = new VBox(6, this.status, this.diagnostics);
    footer.setPadding(new Insets(4, 16, 12, 16));
    this.setBottom(footer);
    for (final ComboBox<?> c :
        List.of(
            this.source,
            this.product,
            this.mode,
            this.outcome,
            this.stageOutcome,
            this.stageOrigin)) {
      c.valueProperty().addListener(o -> this.changed());
    }
    for (final TextField f : List.of(this.folder, this.text, this.minimum, this.maximum)) {
      f.textProperty().addListener(o -> this.changed());
    }
    this.byproducts.selectedProperty().addListener(o -> this.changed());
    this.stereo.selectedProperty().addListener(o -> this.changed());
    this.threshold
        .valueProperty()
        .addListener(
            o -> {
              this.thresholdText.setText(Math.round(this.threshold.getValue()) + " %");
              this.changed();
            });
    this.widthProperty().addListener(o -> this.arrange());
    this.addEventFilter(
        KeyEvent.KEY_PRESSED,
        e -> {
          if (e.isShortcutDown() && e.getCode() == KeyCode.ENTER) {
            this.search();
            e.consume();
          } else if (e.isShortcutDown() && e.getCode() == KeyCode.F) {
            this.localFilter.requestFocus();
            this.localFilter.selectAll();
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

  private StageOutcomeFilter stageFilter() {
    return new StageOutcomeFilter(
        this.stageOutcome.getValue().value(),
        this.stageOrigin.getValue().value(),
        this.byproducts.isSelected());
  }

  private void clearFilters() {
    this.outcome.setValue(Outcome.ALL);
    this.minimum.clear();
    this.maximum.clear();
    this.stageOutcome.getSelectionModel().selectFirst();
    this.stageOrigin.getSelectionModel().selectFirst();
    this.byproducts.setSelected(false);
  }

  private static String assessmentSummary(final Hit hit) {
    return LaboratoryReactionSearch.stageOutcome(hit.drawing().experiment(), hit.stage())
        .map(
            value ->
                "Исход: "
                    + value.status()
                    + " · "
                    + value.observation().origin()
                    + (value.byproducts().isEmpty()
                        ? ""
                        : " · побочных продуктов: " + value.byproducts().size()))
        .orElse("Исход стадии ещё не оценён");
  }

  private static String outcomeText(final Outcome value) {
    return switch (value) {
      case ALL -> "Любое происхождение";
      case OBSERVED -> "Собственное измерение";
      case POSITIVE -> "Собственное измерение: > 0";
      case ZERO -> "Собственное измерение: 0";
      case UNRECORDED -> "Нет собственного измерения продукта";
      case LITERATURE -> "Количество по литературе";
      case SIMULATED -> "Модельное количество";
    };
  }

  private static ListCell<Outcome> outcomeCell() {
    return new ListCell<>() {
      @Override
      protected void updateItem(final Outcome value, final boolean empty) {
        super.updateItem(value, empty);
        setText(empty || value == null ? null : outcomeText(value));
      }
    };
  }

  private static VBox field(final String title, final Node node) {
    return new VBox(4, note(title), node);
  }

  private static Button quick(final String title, final Runnable action) {
    final Button b = new Button(title);
    b.setOnAction(e -> action.run());
    return b;
  }

  private static void visible(final Node node, final boolean value) {
    node.setVisible(value);
    node.setManaged(value);
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

  private static String concise(final String value, final int limit) {
    final String line = value.replaceAll("\\s+", " ").strip();
    return line.length() <= limit ? line : line.substring(0, limit - 1) + "…";
  }

  private void queryHeading(final Request request) {
    final String query = request.text();
    final String sides = this.source.getValue() + " → " + this.product.getValue();
    final boolean structural = request.source() != null || request.product() != null;
    final String heading =
        query.isBlank()
            ? (structural ? "Превращение · " + concise(sides, 100) : "Поиск по выбранным фильтрам")
            : "Поиск: «"
                + concise(query, 72)
                + "»"
                + (structural ? " · " + concise(sides, 52) : "");
    this.settings.setText(heading);
    this.settings.setTooltip(
        new Tooltip(
            (query.isBlank() ? "" : "Текст запроса: " + query + "\n")
                + "Структуры: "
                + sides
                + "\nРежим: "
                + this.mode.getValue()
                + "\nПапка: "
                + this.folder.getText()));
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
    visible(this.folder, false);
    final Button choose = new Button("Выбрать…");
    choose.setId("reaction-search-choose-folder");
    choose.setOnAction(e -> this.chooseFolder());
    final Hyperlink path = new Hyperlink("Путь");
    path.setId("reaction-search-edit-path");
    path.setOnAction(
        e -> {
          visible(this.folder, !this.folder.isVisible());
          if (this.folder.isVisible()) this.folder.requestFocus();
        });
    return new VBox(5, new FlowPane(8, 4, name, choose, path), this.folder);
  }

  private void chooseFolder() {
    final DirectoryChooser chooser = new DirectoryChooser();
    chooser.setTitle("Папка моих опытов");
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
      if (selected != null) {
        this.folder.setText(selected.toPath().toString());
      }
    } catch (final RuntimeException e) {
      this.status.setText(message(e));
    }
  }

  private void filterSummary() {
    final List<String> active = new ArrayList<>();
    if (this.outcome.getValue() != Outcome.ALL) active.add(outcomeText(this.outcome.getValue()));
    if (this.measuredYieldAvailable()
        && (!this.minimum.getText().isBlank() || !this.maximum.getText().isBlank()))
      active.add(
          "выход "
              + (this.minimum.getText().isBlank() ? "…" : this.minimum.getText().strip())
              + "–"
              + (this.maximum.getText().isBlank() ? "…" : this.maximum.getText().strip())
              + " %");
    if (this.stageOutcome.getValue().value() != null)
      active.add("исход: " + this.stageOutcome.getValue());
    if (this.stageOrigin.getValue().value() != null)
      active.add("заключение: " + this.stageOrigin.getValue());
    if (this.byproducts.isSelected()) active.add("указаны побочные продукты");
    this.extraFilters.setText(
        "Дополнительные фильтры" + (active.isEmpty() ? "" : " · применяются: " + active.size()));
    this.filterState.setText("Фильтры: " + String.join(" · ", active));
    visible(this.filterState, !active.isEmpty());
  }

  private void changed() {
    if (this.initializing) {
      return;
    }
    if (this.running) {
      this.cancel();
    }
    this.result = null;
    this.results.getItems().clear();
    this.card.getChildren().clear();
    this.inspecting = false;
    visible(this.diagnostics, false);
    final boolean similarity = this.mode.getSelectionModel().getSelectedIndex() == 2;
    this.threshold.setDisable(!similarity);
    visible(this.thresholdControl, similarity);
    this.stereo.setDisable(similarity);
    this.summary.setText("Параметры изменены · нажмите «Найти превращения»");
    final boolean measured = this.measuredYieldAvailable();
    this.minimum.setDisable(!measured);
    this.maximum.setDisable(!measured);
    this.yieldScope.setText(
        measured
            ? "Диапазон выхода относится к собственным измерениям исходника и продукта."
            : "Для выбранного результата диапазон фактического выхода не применяется. Введённые границы сохранены для поиска собственных измерений.");
    this.rules.setText(
        "Исходник и продукт должны совпасть в одной стадии и на своих сторонах. "
            + (similarity
                ? "Сходство считается отдельно для каждой стороны; совпадение отпечатков не доказывает тождество вещества. "
                : "Сопоставляются элементы, связи, изотопы и заряды. R/S и E/Z проверяются по заданной конфигурации и рисунку; несопоставимая конфигурация отмечается в пропусках. ")
            + "Для диапазона выхода единственное совпадение исходника служит опорным. Отсутствие факта не означает неудачу.");
    this.filterSummary();
    this.settings.setText("Параметры поиска");
    this.settings.setTooltip(null);
    this.status.setText("Выберите структуру, текст методики или фильтр исхода стадии.");
    this.actions();
    this.arrange();
  }

  private boolean measuredYieldAvailable() {
    return this.outcome.getValue() != Outcome.LITERATURE
        && this.outcome.getValue() != Outcome.SIMULATED
        && this.outcome.getValue() != Outcome.UNRECORDED;
  }

  private void search() {
    if (this.running) {
      return;
    }
    final Path directory;
    final Request request;
    try {
      directory = Path.of(this.folder.getText().strip());
      if (this.folder.getText().isBlank() || !directory.isAbsolute()) {
        throw new IllegalArgumentException("Выберите папку проекта или укажите полный путь.");
      }
      request =
          new Request(
              this.source.getValue().drawing() == null
                  ? null
                  : this.source.getValue().drawing().molecule(),
              this.product.getValue().drawing() == null
                  ? null
                  : this.product.getValue().drawing().molecule(),
              new MoleculeStructureSearch.Options(
                  MoleculeStructureSearch.Mode.values()[
                      this.mode.getSelectionModel().getSelectedIndex()],
                  this.stereo.isSelected(),
                  Math.round(this.threshold.getValue()) / 100.0),
              this.text.getText(),
              this.outcome.getValue(),
              this.measuredYieldAvailable() ? number(this.minimum) : null,
              this.measuredYieldAvailable() ? number(this.maximum) : null,
              this.source.getValue().drawing(),
              this.product.getValue().drawing(),
              this.stageFilter());
    } catch (final RuntimeException error) {
      this.settings.setExpanded(true);
      this.extraFilters.setExpanded(true);
      visible(this.folder, true);
      this.status.setText(message(error));
      return;
    }
    final int requestId = ++this.generation;
    this.result = null;
    this.results.getItems().clear();
    this.card.getChildren().clear();
    this.busy(true);
    this.find.setDisable(true);
    this.stop.setDisable(false);
    this.summary.setText("Ищу превращения…");
    visible(this.diagnostics, false);
    this.actions();
    this.task =
        this.worker.submit(
            () -> {
              try {
                final var found =
                    new LaboratoryReactionSearch()
                        .search(
                            directory,
                            request,
                            () -> Thread.currentThread().isInterrupted(),
                            update ->
                                Platform.runLater(
                                    () -> {
                                      if (!this.disposed && requestId == this.generation) {
                                        this.status.setText(update);
                                      }
                                    }));
                Platform.runLater(
                    () -> {
                      if (this.disposed || requestId != this.generation) {
                        return;
                      }
                      this.result = found;
                      this.submitted = request;
                      this.loaded();
                      this.queryHeading(request);
                      this.settings.setExpanded(false);
                      this.filter();
                      this.diagnostic.setText(String.join("\n", found.diagnostics()));
                      this.diagnostics.setText(
                          "Пропуски и ограничения · " + found.diagnostics().size());
                      visible(this.diagnostics, !found.diagnostics().isEmpty());
                      this.status.setText(
                          found.complete()
                              ? "Поиск завершён в пределах выбранной папки. Наблюдения и план показаны раздельно."
                              : "Поиск завершён с пропусками. Нулевой результат не исключает совпадений среди непроверенных данных.");
                    });
              } catch (final CancellationException ignored) {
              } catch (final Exception error) {
                Platform.runLater(
                    () -> {
                      if (this.disposed || requestId != this.generation) {
                        return;
                      }
                      this.loaded();
                      this.summary.setText("Поиск не завершён");
                      this.status.setText(message(error));
                    });
              }
            });
  }

  private void loaded() {
    this.busy(false);
    this.find.setDisable(false);
    this.stop.setDisable(true);
  }

  private void cancel() {
    if (this.task != null) {
      this.task.cancel(true);
    }
    this.generation++;
    this.loaded();
    this.summary.setText("Поиск остановлен");
    this.status.setText("Незавершённые результаты не представлены как полный поиск.");
  }

  private static BigDecimal number(final TextField field) {
    try {
      return field.getText().isBlank()
          ? null
          : new BigDecimal(field.getText().strip().replace(',', '.'));
    } catch (final NumberFormatException error) {
      throw new IllegalArgumentException("Введите числовые границы выхода.");
    }
  }

  private Hit selected() {
    return this.results.getSelectionModel().getSelectedItem();
  }

  private void filter() {
    if (this.result == null) {
      return;
    }
    final String previous = this.selected() == null ? "" : this.selected().key();
    final var words = LaboratoryExperimentIndex.normalize(this.localFilter.getText()).split(" ");
    this.results
        .getItems()
        .setAll(
            this.result.hits().stream()
                .filter(
                    h -> {
                      final String searchable =
                          LaboratoryExperimentIndex.normalize(
                              h.drawing().experiment().title()
                                  + " "
                                  + h.stage().title()
                                  + " "
                                  + LaboratoryReactionClassificationText.searchable(
                                      h.stage().classification())
                                  + " "
                                  + h.path()
                                  + " "
                                  + String.join(
                                      " ",
                                      h.drawing().experiment().components().stream()
                                          .filter(
                                              c ->
                                                  h.stage().participants().stream()
                                                      .anyMatch(
                                                          p -> p.componentId().equals(c.id())))
                                          .map(c -> c.molecule().displayName())
                                          .toList()));
                      return Arrays.stream(words).allMatch(searchable::contains);
                    })
                .toList());
    this.results
        .getSelectionModel()
        .select(
            this.results.getItems().stream()
                .filter(h -> h.key().equals(previous))
                .findFirst()
                .orElse(
                    this.results.getItems().isEmpty() ? null : this.results.getItems().getFirst()));
    this.summary.setText(
        "Найдено "
            + this.results.getItems().size()
            + " из "
            + this.result.hits().size()
            + " · проверено "
            + this.result.stages()
            + " стадий / "
            + this.result.documents()
            + " опытов · ошибок "
            + this.result.errors());
    this.actions();
  }

  private void actions() {
    final var selected = this.selected();
    final boolean empty = selected == null;
    this.open.setDisable(empty);
    this.repeat.setDisable(empty);
    this.inspect.setDisable(empty);
    this.repeatScope.setText(
        empty
            ? ""
            : "Новый план · все стадии: "
                + selected.drawing().experiment().stages().size()
                + " · без фактических результатов");
    visible(this.repeatScope, !empty);
  }

  private void arrange() {
    final boolean narrow = this.getWidth() < 1050;
    final List<Node> nodes =
        this.inspecting
            ? List.of(this.detail)
            : narrow ? List.of(this.list) : List.of(this.list, this.detail);
    if (!this.split.getItems().equals(nodes)) {
      this.split.getItems().setAll(nodes);
      if (nodes.size() == 2) {
        this.split.setDividerPositions(.43);
      }
    }
    visible(this.header, !this.inspecting);
    visible(this.back, this.inspecting);
    visible(this.inspect, !this.inspecting);
  }

  private void openSelected() {
    final var hit = this.selected();
    if (hit != null) {
      this.opener.accept(hit.path(), hit.stage().id());
    }
  }

  private String resultText(final Hit hit) {
    final String score =
        this.submitted != null
                && this.submitted.options().mode() == MoleculeStructureSearch.Mode.SIMILARITY
            ? "Сходство сторон ≥ "
                + String.format(Locale.ROOT, "%.1f %%", hit.score() * 100)
                + " · "
            : "";
    final Set<LaboratoryEvidence> origins = new LinkedHashSet<>();
    hit.products().stream()
        .map(Participant::component)
        .filter(c -> c.actual() != null)
        .forEach(c -> origins.add(c.evidence()));
    final List<String> labels = new ArrayList<>();
    if (origins.contains(LaboratoryEvidence.OBSERVED)) {
      labels.add("Собственное измерение");
    }
    if (origins.contains(LaboratoryEvidence.LITERATURE)) {
      labels.add("Количество продукта по литературе");
    }
    if (origins.contains(LaboratoryEvidence.SIMULATED)) {
      labels.add("Учебный / модельный результат");
    }
    if (origins.contains(LaboratoryEvidence.PLANNED)) {
      labels.add("Количество без подтверждения");
    }
    return (LaboratoryReactionSearch.stageOutcome(hit.drawing().experiment(), hit.stage())
                .isPresent()
            ? assessmentSummary(hit) + "\n"
            : "")
        + score
        + (labels.isEmpty() ? "Количество продукта не записано" : String.join(" · ", labels))
        + "\n"
        + conditions(hit);
  }

  private static String conditions(final Hit hit) {
    final var commonValue = hit.drawing().experiment().conditions();
    final var stageValue = hit.stage().conditions();
    final var common = LaboratoryReactionSearch.conditionsText(commonValue);
    final var stage = LaboratoryReactionSearch.conditionsText(stageValue);
    if (stage.isBlank()) {
      return common.isBlank() ? "Условия не указаны" : "Условия из опыта: " + common;
    }
    if (stageValue.equals(commonValue)) {
      return "Условия стадии и опыта: " + stage;
    }
    return "Условия стадии: "
        + stage
        + (common.isBlank() ? "" : "\nОбщие условия опыта: " + common);
  }

  private void showDetail() {
    this.card.getChildren().clear();
    this.actions();
    final var hit = this.selected();
    if (hit == null) {
      return;
    }
    final Label title = note(hit.stage().title());
    title.setStyle("-fx-font-size: 20; -fx-font-weight: bold;");
    this.card.getChildren().addAll(title, note(hit.drawing().experiment().title()));
    this.showLiterature(hit);
    this.card
        .getChildren()
        .addAll(
            new HBox(8, thumbnail(hit, true, 180), note("→"), thumbnail(hit, false, 180)),
            note(conditions(hit)));
    final var assessment =
        LaboratoryReactionSearch.stageOutcome(hit.drawing().experiment(), hit.stage());
    if (assessment.isPresent()) {
      final var assessmentDetails =
          new TitledPane(
              assessmentSummary(hit),
              area(
                  LaboratoryReactionSearch.stageOutcomeText(
                      hit.drawing().experiment(), hit.stage()),
                  6));
      assessmentDetails.setId("reaction-search-stage-assessment");
      assessmentDetails.setAnimated(false);
      assessmentDetails.setExpanded(false);
      this.card.getChildren().add(assessmentDetails);
    } else {
      this.card.getChildren().add(note("Исход стадии ещё не оценён"));
    }
    final String classification =
        LaboratoryReactionClassificationText.summary(hit.stage().classification());
    if (!classification.isBlank()) {
      this.card.getChildren().add(note(classification));
    }
    if (!hit.notes().isEmpty()) {
      final TitledPane notes =
          new TitledPane("Основание расчёта и пояснения", area(String.join("\n", hit.notes()), 4));
      notes.setAnimated(false);
      notes.setExpanded(false);
      this.card.getChildren().add(notes);
    }
    final var participants = hit.stage().participants();
    final var components = hit.drawing().experiment().components();
    final GridPane amounts = new GridPane();
    amounts.setHgap(14);
    amounts.setVgap(8);
    amounts.setMinWidth(0);
    for (final double width : new double[] {50, 22, 28}) {
      final ColumnConstraints column = new ColumnConstraints();
      column.setPercentWidth(width);
      column.setMinWidth(0);
      column.setFillWidth(true);
      amounts.getColumnConstraints().add(column);
    }
    amounts.addRow(0, note("Компонент / роль"), note("План"), note("Внесено / источник"));
    int row = 1;
    for (final var participant : participants) {
      final var c =
          components.stream()
              .filter(v -> v.id().equals(participant.componentId()))
              .findFirst()
              .orElseThrow();
      amounts.addRow(
          row++,
          note(
              c.molecule().displayName() + "\n" + LaboratoryInspector.roleText(participant.role())),
          note(quantity(c.planned())),
          note(
              quantity(c.actual())
                  + (c.actual() == null ? "" : " · " + c.evidence().quantityLabel())));
    }
    final boolean measured =
        participants.stream()
            .map(
                p ->
                    components.stream()
                        .filter(c -> c.id().equals(p.componentId()))
                        .findFirst()
                        .orElseThrow())
            .anyMatch(c -> c.actual() != null && c.evidence() == LaboratoryEvidence.OBSERVED);
    final VBox quantityBody = new VBox(10, amounts);
    final TitledPane quantities =
        new TitledPane("Загрузка и расчёт собственного выхода", quantityBody);
    quantities.setId("reaction-search-quantities");
    quantities.setAnimated(false);
    quantities.setExpanded(measured);
    this.card.getChildren().add(quantities);
    final ComboBox<Reference> reference = new ComboBox<>();
    reference.setMaxWidth(Double.MAX_VALUE);
    reference.setPromptText("Выберите исходник для расчёта выхода");
    for (final var p : participants) {
      if (p.role() == ReactionComponentRole.REACTANT) {
        final var c =
            components.stream()
                .filter(v -> v.id().equals(p.componentId()))
                .findFirst()
                .orElseThrow();
        reference.getItems().add(new Reference(c.id(), c.molecule().displayName()));
      }
    }
    final VBox yield = new VBox(6);
    final Runnable calculate =
        () -> {
          yield.getChildren().clear();
          if (reference.getValue() == null) {
            yield
                .getChildren()
                .add(
                    note(
                        "Выход рассчитывается отдельно для каждого продукта относительно выбранного исходника и коэффициентов стадии."));
            return;
          }
          final List<String> notes = new ArrayList<>();
          try {
            final var values =
                LaboratoryReactionSearch.yields(
                    hit.drawing(),
                    hit.stage(),
                    reference.getValue().id(),
                    hit.products().stream()
                        .map(p -> p.component().id())
                        .collect(java.util.stream.Collectors.toSet()),
                    notes);
            if (values.isEmpty()) {
              yield.getChildren().add(note("Выход недоступен для выбранной основы."));
            }
            for (final var value : values) {
              final var c =
                  components.stream()
                      .filter(v -> v.id().equals(value.productId()))
                      .findFirst()
                      .orElseThrow();
              yield
                  .getChildren()
                  .add(
                      note(
                          c.molecule().displayName()
                              + ": "
                              + value.percent().round(new java.math.MathContext(5)).toPlainString()
                              + " %"));
            }
          } catch (final RuntimeException error) {
            notes.add(message(error));
          }
          yield.getChildren().add(note(String.join("\n", notes)));
        };
    reference.valueProperty().addListener(o -> calculate.run());
    if (!hit.yields().isEmpty()) {
      final String id = hit.yields().getFirst().referenceId();
      reference.getItems().stream()
          .filter(v -> v.id().equals(id))
          .findFirst()
          .ifPresent(reference::setValue);
    }
    calculate.run();
    quantityBody
        .getChildren()
        .addAll(field("Опорный исходник для собственного измеренного выхода", reference), yield);
    final TextArea method = area(methodText(hit), 12);
    final TitledPane methodPane =
        new TitledPane(
            "Вся методика стадии · "
                + LaboratoryReactionSearch.method(hit.drawing(), hit.stage()).size()
                + " шагов",
            method);
    methodPane.setAnimated(false);
    methodPane.setExpanded(
        LaboratoryReactionSearch.literature(hit.drawing().experiment(), hit.stage()).isEmpty()
            && !LaboratoryReactionSearch.method(hit.drawing(), hit.stage()).isEmpty());
    final TextArea observations = area(observationText(hit), 10);
    final TitledPane history = new TitledPane("Наблюдения и журнал опыта", observations);
    history.setAnimated(false);
    history.setExpanded(false);
    this.card.getChildren().addAll(methodPane, history, note("Источник: " + hit.path()));
  }

  private void showLiterature(final Hit hit) {
    final var sources =
        LaboratoryReactionSearch.literature(hit.drawing().experiment(), hit.stage());
    if (sources.isEmpty()) {
      return;
    }
    final String[] words =
        this.submitted == null
            ? new String[0]
            : LaboratoryExperimentIndex.normalize(this.submitted.text()).split(" ");
    final var selected =
        sources.stream()
            .filter(
                source -> {
                  final String value =
                      LaboratoryExperimentIndex.normalize(
                          LaboratoryReactionSearch.literatureText(source));
                  return Arrays.stream(words)
                      .anyMatch(word -> !word.isBlank() && value.contains(word));
                })
            .findFirst()
            .orElse(sources.getFirst());
    final String citation = selected.reference().citation();
    final Label source =
        note(citation.length() > 200 ? citation.substring(0, 197) + "…" : citation);
    source.setId("reaction-search-literature-citation");
    source.setTooltip(new Tooltip(citation));
    final Label scope =
        note(
            (selected.common()
                    ? "Общая литературная методика опыта"
                    : "Литературная методика стадии")
                + " · "
                + selected.method().title());
    scope.setId("reaction-search-literature-scope");
    final TextArea full = area("", 10);
    full.setId("reaction-search-literature-text");
    final TitledPane details =
        new TitledPane(
            sources.size() > 1
                ? "Источники и тексты методик · " + sources.size()
                : "Источник и текст методики",
            full);
    details.setId("reaction-search-literature-details");
    details.setAnimated(false);
    details.setExpanded(false);
    details
        .expandedProperty()
        .addListener(
            (o, a, b) -> {
              if (b && full.getText().isEmpty()) {
                final List<LiteratureSource> ordered = new ArrayList<>();
                ordered.add(selected);
                sources.stream().filter(s -> s != selected).forEach(ordered::add);
                full.setText(
                    ordered.stream()
                        .map(LaboratoryReactionSearch::literatureText)
                        .collect(
                            java.util.stream.Collectors.joining("\n────────────────────\n\n")));
              }
            });
    final var method = selected.method();
    final Label reported =
        note(
            method.literatureYieldPercent() == null
                ? "Литературный выход в методике не указан"
                : "Литературный выход: "
                    + method.literatureYieldPercent().stripTrailingZeros().toPlainString()
                    + " % · "
                    + method.yieldBasis());
    reported.setId("reaction-search-literature-yield");
    final String procedure = method.procedureText().strip();
    final Label preview =
        note(procedure.length() > 360 ? procedure.substring(0, 357) + "…" : procedure);
    preview.setId("reaction-search-literature-preview");
    this.card.getChildren().add(new VBox(6, scope, source, reported, preview, details));
  }

  private static TextArea area(final String text, final int rows) {
    final var result = new TextArea(text);
    result.setEditable(false);
    result.setWrapText(true);
    result.setPrefRowCount(rows);
    return result;
  }

  private static String methodText(final Hit hit) {
    final var text = new StringBuilder();
    int i = 0;
    for (final var step : LaboratoryReactionSearch.method(hit.drawing(), hit.stage())) {
      text.append(++i).append(". ");
      try {
        text.append(LaboratoryProcedureService.describe(hit.drawing().experiment(), step));
      } catch (final RuntimeException error) {
        text.append(step.title())
            .append('\n')
            .append(step.instruction())
            .append("\nКоличество недоступно: ")
            .append(message(error));
      }
      if (step.stageId() == null) {
        text.append("\nОбщая операция опыта");
      }
      text.append("\n\n");
    }
    if (i == 0) {
      text.append("Пошаговая методика не записана.");
    }
    text.append("\n\n")
        .append(LaboratoryReactionMethodText.method(hit.drawing().experiment(), hit.stage()));
    if (!hit.drawing().experiment().notes().isBlank()) {
      text.append("\nЗаметки всего опыта:\n").append(hit.drawing().experiment().notes());
    }
    return text.toString();
  }

  private static String observationText(final Hit hit) {
    final var text = new StringBuilder();
    final var time =
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").withZone(ZoneId.systemDefault());
    for (final var event : LaboratoryReactionSearch.observations(hit.drawing(), hit.stage())) {
      text.append(time.format(event.recordedAt()))
          .append(" · ")
          .append(event.action())
          .append(" · ")
          .append(event.snapshot().title())
          .append('\n')
          .append(event.text());
      if (event.actualQuantity() != null) {
        text.append("\nФакт действия: ").append(quantity(event.actualQuantity()));
      }
      if (event.correctsId() != null) {
        text.append("\nИсправление; исходная запись сохранена.");
      }
      text.append("\n\n");
    }
    text.append(LaboratoryReactionMethodText.observations(hit.drawing().experiment(), hit.stage()));
    text.append("Журнал всего опыта (записи могут относиться к другим стадиям):\n");
    for (final var entry : hit.drawing().experiment().journal()) {
      text.append(time.format(entry.recordedAt()))
          .append(" · ")
          .append(entry.kind())
          .append('\n')
          .append(entry.text())
          .append("\n\n");
    }
    return text.toString();
  }

  private static String quantity(final LaboratoryQuantity value) {
    if (value == null) {
      return "не задано";
    }
    if (value.value().precision() > 100 || Math.abs((long) value.value().scale()) > 100) {
      return "число вне предела отображения";
    }
    return LaboratoryQuantityText.format(value, true);
  }

  private static HBox resultPictures(final Hit hit, final ListView<Hit> view) {
    final Node source = thumbnail(hit, true, 100), product = thumbnail(hit, false, 100);
    final HBox row = new HBox(8, source, note("→"), product);
    row.setAlignment(javafx.geometry.Pos.CENTER);
    final var height =
        javafx.beans.binding.Bindings.createDoubleBinding(
            () -> resultPictureHeight(hit, Math.max(80, (view.getWidth() - 76) / 2)),
            view.widthProperty());
    for (final Node side : List.of(source, product))
      if (side instanceof VBox box && box.getChildren().getFirst() instanceof Region picture) {
        picture.minHeightProperty().bind(height);
        picture.prefHeightProperty().bind(height);
        picture.maxHeightProperty().bind(height);
      }
    return row;
  }

  private static double resultPictureHeight(final Hit hit, final double width) {
    double height = 100;
    for (final var side : List.of(hit.sources(), hit.products()))
      if (!side.isEmpty()) {
        final var bounds =
            LaboratoryCanvas.previewBounds(
                hit.drawing().layouts().get(side.getFirst().component().id()));
        final double scale =
            Math.min(.55, Math.max(.01, (width - 24) / Math.max(1, bounds.getWidth())));
        height = Math.max(height, Math.min(180, 18 + scale * bounds.getHeight()));
      }
    return Math.ceil(height);
  }

  private static Node thumbnail(final Hit hit, final boolean reactant, final double height) {
    final var candidates = reactant ? hit.sources() : hit.products();
    if (candidates.isEmpty()) {
      final Label absent = note(reactant ? "Исходник не задан" : "Продукт не задан");
      absent.setPrefWidth(180);
      return absent;
    }
    final var selected = candidates.getFirst();
    final VBox box =
        new VBox(
            4,
            picture(
                hit.drawing().layouts().get(selected.component().id()),
                selected.match().atoms(),
                height),
            note(
                selected.component().molecule().displayName()
                    + (candidates.size() > 1 ? " · ещё " + (candidates.size() - 1) : "")));
    box.setPrefWidth(230);
    box.setMinWidth(0);
    HBox.setHgrow(box, Priority.ALWAYS);
    return box;
  }

  private static Node picture(
      final MoleculeDrawing drawing, final Set<AtomId> atoms, final double height) {
    final Canvas canvas = new Canvas(220, height);
    final StackPane panel = new StackPane(canvas);
    panel.setMinWidth(0);
    panel.setPrefWidth(220);
    panel.setMinHeight(height);
    panel.setPrefHeight(height);
    panel.setMaxHeight(height);
    canvas.widthProperty().bind(panel.widthProperty());
    canvas.heightProperty().bind(panel.heightProperty());
    final Runnable paint =
        () -> {
          final var g = canvas.getGraphicsContext2D();
          LaboratoryPreviewTheme.clear(g);
          final var bounds = LaboratoryCanvas.previewBounds(drawing);
          final double scale =
              Math.max(
                  .01,
                  Math.min(
                      1.1,
                      Math.min(
                          (canvas.getWidth() - 24) / Math.max(1, bounds.getWidth()),
                          (canvas.getHeight() - 18) / Math.max(1, bounds.getHeight()))));
          g.save();
          g.translate(
              canvas.getWidth() / 2 - (bounds.getMinX() + bounds.getMaxX()) * scale / 2,
              canvas.getHeight() / 2 - (bounds.getMinY() + bounds.getMaxY()) * scale / 2);
          g.scale(scale, scale);
          g.setFill(LaboratoryPreviewTheme.highlight(canvas));
          for (final var atom : LaboratoryCanvas.previewAtoms(drawing)) {
            if (atoms.contains(atom.atomId())) {
              g.fillOval(atom.point2D().x() - 10, atom.point2D().y() - 10, 20, 20);
            }
          }
          LaboratoryPreviewTheme.paint(g, drawing);
          g.restore();
        };
    LaboratoryPreviewTheme.track(canvas, paint);
    canvas.widthProperty().addListener(o -> paint.run());
    canvas.heightProperty().addListener(o -> paint.run());
    paint.run();
    return panel;
  }

  private static String message(final Throwable error) {
    return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
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

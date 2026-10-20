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
import java.util.function.*;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratorySolutionBook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryDispensingPlan.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.*;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.*;

/** One set of stock settings drives the whole plate; calculations remain explicit, reviewable plans. */
final class LaboratoryDispensingPage extends LaboratoryComputationPage {
  private enum Mode {
    DIRECT("Непосредственно"),
    INDIVIDUAL("Раствор отдельно"),
    COMMON("В общей смеси");
    private final String label;

    Mode(String label) {
      this.label = label;
    }

    public String toString() {
      return this.label;
    }
  }

  private record Choice(String id, String label) {
    public String toString() {
      return this.label;
    }
  }

  private final class IngredientRow {
    final String id;
    final String name;
    final ObjectProperty<Mode> mode = new SimpleObjectProperty<>(Mode.DIRECT);
    final ObjectProperty<ConcentrationUnit> unit =
        new SimpleObjectProperty<>(ConcentrationUnit.MOL_PER_LITRE);
    final StringProperty concentration = new SimpleStringProperty(""),
        label = new SimpleStringProperty(""),
        solvent = new SimpleStringProperty(""),
        source = new SimpleStringProperty("");

    IngredientRow(final Ingredient ingredient) {
      this.id = ingredient.componentId();
      this.name = component(this.id).molecule().displayName();
      this.mode.set(
          ingredient.stock() == null
              ? Mode.DIRECT
              : ingredient.common() ? Mode.COMMON : Mode.INDIVIDUAL);
      if (ingredient.stock() != null) {
        this.concentration.set(ingredient.stock().value().toPlainString());
        this.unit.set(ingredient.stock().unit());
      }
      this.label.set(
          ingredient.label().isBlank() ? this.name + " · исходный раствор" : ingredient.label());
      this.solvent.set(ingredient.solvent());
      this.source.set(ingredient.source());
      for (final var property :
          List.<javafx.beans.Observable>of(
              this.mode, this.unit, this.concentration, this.label, this.solvent, this.source))
        property.addListener(o -> changed());
    }

    Ingredient value() {
      final boolean stock = this.mode.get() != Mode.DIRECT;
      return new Ingredient(
          this.id,
          this.mode.get() == Mode.COMMON,
          stock ? new Concentration(number(this.concentration.get()), this.unit.get()) : null,
          stock ? this.label.get() : "",
          stock ? this.solvent.get() : "",
          stock ? this.source.get() : "");
    }
  }

  private final class VialRow {
    final Run run;
    final BooleanProperty included = new SimpleBooleanProperty();

    VialRow(Run run) {
      this.run = run;
      this.included.set(run.included());
      this.included.addListener(o -> changed());
    }
  }

  private LaboratoryDispensingCodec.Document initial;
  private final Window owner;
  private final Path parent;
  private final LaboratoryDispensingRepository repository = new LaboratoryDispensingRepository();
  private final TableView<IngredientRow> ingredients = new TableView<>();
  private final TableView<VialRow> matrix = new TableView<>();
  private final TableView<LaboratoryDispensingService.Total> totals = new TableView<>();
  private final TabPane tabs = new TabPane();
  private final TextField title = new TextField(),
      aliquot = new TextField(),
      reserve = new TextField("0"),
      dead = new TextField("0"),
      diluent = new TextField(),
      filter = new TextField();
  private final ComboBox<Choice> reference = new ComboBox<>();
  private final ComboBox<LaboratoryQuantityUnit> aliquotUnit = volumeUnits(),
      deadUnit = volumeUnits();
  private final TextArea compatibility = new TextArea(),
      noteText = new TextArea(),
      instructions = new TextArea(),
      details = new TextArea();
  private final TextField bulkConcentration = new TextField(),
      bulkSolvent = new TextField(),
      bulkSource = new TextField();
  private final ComboBox<ConcentrationUnit> bulkUnit = new ComboBox<>();
  private final CheckBox individualOnly = new CheckBox("Показывать только индивидуальные добавки");
  private final VBox diagnostics = new VBox(5);
  private final Label summary = note(""), sourceContext = note("");
  private final VBox blockingSummary = new VBox(7);
  private final TitledPane fullInstructions = new TitledPane();
  private final Button save;
  private final Map<String, LaboratoryDispensingService.Vial> computedRows = new HashMap<>();
  private LaboratoryDispensingCodec.Document reviewed;
  private LaboratoryDispensingService.Result computed;
  private final javafx.collections.ObservableList<VialRow> allRows =
      javafx.collections.FXCollections.observableArrayList();
  private Path file;
  private String fingerprint;
  private boolean dirty, changing, bulkPending, conflict;
  private Runnable matrixBack;

  static LaboratoryDispensingPage create(
      final Window owner,
      final LaboratoryDesignCodec.Document study,
      final List<LaboratorySeriesPlan.Prepared> prepared,
      final Path parent,
      final Runnable back) {
    final var plan =
        LaboratoryDispensingService.capture(
            study.source(), study.design(), prepared, LaboratoryDispensingCodec.hash(study));
    return embedded(
        new LaboratoryDispensingPage(
            new LaboratoryDispensingCodec.Document(study, plan), parent, owner, back),
        back);
  }

  static LaboratoryDispensingPage saved(
      final Window owner,
      final Path path,
      final LaboratoryDispensingRepository.Opened opened,
      final Runnable back) {
    final var page =
        embedded(
            new LaboratoryDispensingPage(
                opened.document(), path.toAbsolutePath().getParent(), owner, back),
            back);
    page.file = path;
    page.fingerprint = opened.fingerprint();
    return page;
  }

  private static LaboratoryDispensingPage embedded(
      final LaboratoryDispensingPage page, final Runnable back) {
    page.matrixBack = back;
    ((Button) page.getTop().lookup("#chemistry-computation-back")).setText("← К матрице");
    return page;
  }

  @Override
  protected void finish() {
    if (this.matrixBack == null) {
      super.finish();
      return;
    }
    if (this.running) {
      this.status.setText("Дождитесь завершения файловой операции перед возвратом к матрице.");
      return;
    }
    this.matrixBack.run();
  }

  private static void compact(final TableView<?> table, final double maximum) {
    final Runnable resize =
        () -> {
          table.setPrefHeight(Math.min(maximum, 50 + 34 * Math.max(2, table.getItems().size())));
          table.setMinHeight(Region.USE_PREF_SIZE);
          table.setMaxHeight(Region.USE_PREF_SIZE);
        };
    table
        .getItems()
        .addListener((javafx.collections.ListChangeListener<Object>) change -> resize.run());
    resize.run();
  }

  LaboratoryDispensingPage(
      final LaboratoryDispensingCodec.Document document,
      final Path parent,
      final Window owner,
      final Runnable close) {
    super("Общая смесь и индивидуальные добавки", close);
    this.initial = document;
    this.owner = owner;
    this.parent = parent;
    this.setId("chemistry-dispensing-page");
    this.ingredients.setEditable(true);
    this.ingredients.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    this.ingredients.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    this.ingredients.setMinHeight(210);
    this.ingredients.setPrefHeight(270);
    this.ingredients.setId("dispensing-ingredients");
    this.ingredients.getColumns().add(read("Компонент", r -> r.name, 220));
    final var mode = new TableColumn<IngredientRow, Mode>("Добавление");
    mode.setCellValueFactory(v -> v.getValue().mode);
    mode.setCellFactory(ComboBoxTableCell.forTableColumn(Mode.values()));
    mode.setOnEditCommit(e -> e.getRowValue().mode.set(e.getNewValue()));
    mode.setPrefWidth(160);
    this.ingredients.getColumns().add(mode);
    this.ingredients.getColumns().add(edit("Концентрация", r -> r.concentration, 130));
    final var unit = new TableColumn<IngredientRow, ConcentrationUnit>("Единица концентрации");
    unit.setCellValueFactory(v -> v.getValue().unit);
    unit.setCellFactory(ComboBoxTableCell.forTableColumn(ConcentrationUnit.values()));
    unit.setOnEditCommit(e -> e.getRowValue().unit.set(e.getNewValue()));
    unit.setPrefWidth(175);
    this.ingredients.getColumns().add(unit);
    this.ingredients
        .getColumns()
        .addAll(
            edit("Название исходного раствора", r -> r.label, 250),
            edit("Растворитель", r -> r.solvent, 150),
            edit("Источник концентрации", r -> r.source, 280));
    this.bulkUnit.getItems().setAll(ConcentrationUnit.values());
    this.bulkUnit.setValue(ConcentrationUnit.MOL_PER_LITRE);
    this.bulkConcentration.setPrefWidth(100);
    this.bulkSolvent.setPrefWidth(130);
    this.bulkSource.setPrefWidth(240);
    final VBox batch =
        new VBox(
            8,
            note(
                "Выделите несколько компонентов: одинаковые введённые параметры применяются одним действием."),
            new FlowPane(
                10,
                8,
                field("Концентрация", this.bulkConcentration),
                field("Единица", this.bulkUnit),
                field("Растворитель", this.bulkSolvent),
                field("Источник", this.bulkSource),
                button("Применить к выделенным", "bulk", this::bulk)),
            new FlowPane(
                8,
                8,
                button("В общую смесь", "common", () -> this.mode(Mode.COMMON)),
                button("Раствор отдельно", "individual", () -> this.mode(Mode.INDIVIDUAL)),
                button("Непосредственно", "direct", () -> this.mode(Mode.DIRECT)),
                button("Молярность из свойств шаблона", "properties", this::properties)));
    this.compatibility.setPrefRowCount(3);
    this.compatibility.setWrapText(true);
    this.compatibility.setPromptText(
        "Принятая методика совместного приготовления, порядок добавления, ограничения по времени/температуре. Программа не определяет химическую совместимость.");
    this.noteText.setPrefRowCount(2);
    this.noteText.setWrapText(true);
    this.reference.setPrefWidth(210);
    final TitledPane batchPane = new TitledPane("Массовое заполнение", batch);
    batchPane.setExpanded(false);
    final VBox setup =
        new VBox(
            12,
            field("Название плана", this.title),
            note(
                "Укажите исходные растворы один раз для компонента. Все позиции и индивидуальные дозы пересчитываются вместе."),
            this.ingredients,
            batchPane,
            new FlowPane(
                14,
                8,
                field("Опорная позиция", this.reference),
                field("Аликвота общей смеси для неё", new HBox(6, this.aliquot, this.aliquotUnit)),
                field("Резерв общей смеси, %", this.reserve),
                field("Мёртвый объём общей смеси", new HBox(6, this.dead, this.deadUnit))),
            field("Растворитель доведения общей смеси", this.diluent),
            field("Основание совместного приготовления", this.compatibility),
            field("Замечания", this.noteText),
            note(
                "Резерв применяется только к общей смеси. Индивидуальные добавки показаны без запаса. Концентрации — активного вещества; чистота не подставляется автоматически."));
    setup.setPadding(new Insets(16));
    this.matrix.setEditable(true);
    this.matrix.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    this.matrix.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    this.matrix.setId("dispensing-matrix");
    this.matrix.getSelectionModel().selectedItemProperty().addListener(o -> this.detail());
    this.filter.setPromptText("Найти позицию или запуск");
    final var filtered = new FilteredList<>(this.allRows);
    this.matrix.setItems(filtered);
    compact(this.matrix, 400);
    this.filter
        .textProperty()
        .addListener(
            o -> {
              final String query = this.filter.getText().strip().toLowerCase(Locale.ROOT);
              filtered.setPredicate(
                  row ->
                      query.isEmpty()
                          || (row.run.position() + " " + row.run.id())
                              .toLowerCase(Locale.ROOT)
                              .contains(query));
            });
    this.individualOnly.setSelected(true);
    this.individualOnly.selectedProperty().addListener(o -> this.matrixColumns());
    this.details.setEditable(false);
    this.details.setWrapText(true);
    this.details.setPrefRowCount(6);
    final VBox matrixPane =
        new VBox(
            10,
            this.summary,
            new FlowPane(
                8,
                8,
                this.filter,
                this.individualOnly,
                button("Включить все", "all", () -> this.include(true, false)),
                button("Исключить все", "none", () -> this.include(false, false)),
                button("Включить выделенные", "include", () -> this.include(true, true)),
                button("Исключить выделенные", "exclude", () -> this.include(false, true)),
                button("Пересчитать", "recalculate", this::calculate)),
            this.matrix,
            field("Расчёт выбранной позиции", this.details));
    matrixPane.setPadding(new Insets(16));
    this.instructions.setEditable(false);
    this.instructions.setWrapText(true);
    this.instructions.setPrefRowCount(18);
    compact(this.totals, 210);
    this.totals
        .getColumns()
        .addAll(
            read("Исходный материал / раствор", LaboratoryDispensingService.Total::label, 320),
            read("На все выбранные позиции", r -> label(r.required()), 210),
            read("Подготовить", r -> label(r.prepare()), 200),
            read("Способ", r -> r.common() ? "В общей смеси" : "Индивидуально", 170));
    this.blockingSummary.setId("dispensing-blocker-summary");
    this.blockingSummary.setVisible(false);
    this.blockingSummary.setManaged(false);
    this.fullInstructions.setText("Инструкция приготовления и раздачи");
    this.fullInstructions.setContent(this.instructions);
    this.fullInstructions.setExpanded(true);
    this.fullInstructions.setAnimated(false);
    this.fullInstructions.setId("dispensing-full-instructions");
    final TitledPane calculationNotes =
        new TitledPane("Условия и ограничения расчёта", this.diagnostics);
    calculationNotes.setExpanded(false);
    final TitledPane provenance = new TitledPane("Источник плана", this.sourceContext);
    provenance.setExpanded(false);
    final VBox review =
        new VBox(
            10,
            this.blockingSummary,
            calculationNotes,
            this.totals,
            this.fullInstructions,
            provenance);
    review.setPadding(new Insets(16));
    this.tabs
        .getTabs()
        .setAll(
            new Tab("Состав и исходные растворы", formScroll(setup)),
            new Tab("Позиции и добавки", formScroll(matrixPane)),
            new Tab("Приготовление и раздача", formScroll(review)));
    this.tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    this.setCenter(this.tabs);
    this.save = button("Сохранить .qdispense", "save", () -> this.save(false));
    this.save.getStyleClass().add("primary-button");
    final Button calculate = button("Рассчитать раздачу", "calculate", this::calculate);
    calculate.getStyleClass().add("primary-button");
    final VBox bottom =
        new VBox(
            8,
            new FlowPane(
                8,
                8,
                calculate,
                this.save,
                button("Сохранить копию", "save-copy", () -> this.save(true)),
                button("Открыть план раздачи…", "open", this::load),
                button("Сбросить несохранённое", "reset", this::reset)),
            this.status);
    bottom.setPadding(new Insets(10, 16, 14, 16));
    this.setBottom(bottom);
    for (final var text : List.of(this.title, this.aliquot, this.reserve, this.dead, this.diluent))
      text.textProperty().addListener(o -> this.changed());
    this.compatibility.textProperty().addListener(o -> this.changed());
    this.noteText.textProperty().addListener(o -> this.changed());
    this.reference.valueProperty().addListener(o -> this.changed());
    this.aliquotUnit.valueProperty().addListener(o -> this.changed());
    this.deadUnit.valueProperty().addListener(o -> this.changed());
    for (final var input : List.of(this.bulkConcentration, this.bulkSolvent, this.bulkSource))
      input
          .textProperty()
          .addListener(
              o -> {
                if (!this.changing) this.bulkPending = true;
              });
    this.bulkUnit
        .valueProperty()
        .addListener(
            o -> {
              if (!this.changing) this.bulkPending = true;
            });
    this.restore(document);
  }

  private static ComboBox<LaboratoryQuantityUnit> volumeUnits() {
    final var combo = new ComboBox<LaboratoryQuantityUnit>();
    combo
        .getItems()
        .setAll(
            LaboratoryQuantityUnit.MICROLITRE,
            LaboratoryQuantityUnit.MILLILITRE,
            LaboratoryQuantityUnit.LITRE);
    combo.setValue(LaboratoryQuantityUnit.MILLILITRE);
    return combo;
  }

  private static VBox field(final String name, final javafx.scene.Node node) {
    return new VBox(4, note(name), node);
  }

  private Button button(final String title, final String id, final Runnable action) {
    final var b = new Button(title);
    b.setId("dispensing-" + id);
    b.setOnAction(
        e -> {
          if (this.running) return;
          try {
            action.run();
          } catch (final Exception error) {
            this.status.setText(error.getMessage());
          }
        });
    return b;
  }

  private static <T> TableColumn<T, String> read(
      final String label, final Function<T, String> value, final double width) {
    final var column = new TableColumn<T, String>(label);
    column.setPrefWidth(width);
    column.setSortable(false);
    column.setCellValueFactory(v -> new SimpleStringProperty(value.apply(v.getValue())));
    return column;
  }

  private TableColumn<IngredientRow, String> edit(
      final String label,
      final Function<IngredientRow, StringProperty> property,
      final double width) {
    final var column = new TableColumn<IngredientRow, String>(label);
    column.setPrefWidth(width);
    column.setSortable(false);
    column.setCellValueFactory(v -> property.apply(v.getValue()));
    column.setCellFactory(
        c ->
            new TableCell<>() {
              private final TextField input = new TextField();
              private StringProperty bound;

              {
                this.input.setStyle("-fx-background-color: transparent; -fx-padding: 6 4;");
                this.input.setAccessibleText(label);
              }

              protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (this.bound != null) {
                  this.input.textProperty().unbindBidirectional(this.bound);
                  this.bound = null;
                }
                if (empty
                    || this.getIndex() < 0
                    || this.getIndex() >= this.getTableView().getItems().size()) {
                  this.setGraphic(null);
                  return;
                }
                this.bound = property.apply(this.getTableView().getItems().get(this.getIndex()));
                this.input.setText(this.bound.get());
                this.input.textProperty().bindBidirectional(this.bound);
                this.setGraphic(this.input);
              }
            });
    return column;
  }

  private LaboratoryComponent component(final String id) {
    return this.initial.study().source().experiment().components().stream()
        .filter(c -> c.id().equals(id))
        .findFirst()
        .orElseThrow();
  }

  private static ScrollPane formScroll(final javafx.scene.Node content) {
    final var result = scroll(content);
    result.setFitToHeight(false);
    return result;
  }

  private void changed() {
    if (this.changing) return;
    this.dirty = true;
    this.reviewed = null;
    this.computed = null;
    this.computedRows.clear();
    this.save.setDisable(true);
    this.matrix.refresh();
    this.summary.setText("Параметры изменены. Пересчитайте раздачу.");
    this.instructions.clear();
    this.clearBlockers();
    this.totals.getItems().clear();
    this.diagnostics
        .getChildren()
        .setAll(
            note(
                "Предыдущий расчёт больше не действует. Пересчитайте раздачу перед приготовлением."));
    this.details.clear();
    this.status.setText(
        "Параметры изменены. Предыдущие количества и инструкция сняты; требуется новый расчёт.");
  }

  private void restore(final LaboratoryDispensingCodec.Document document) {
    this.changing = true;
    this.initial = document;
    this.sourceContext.setText(
        document.study().design().title()
            + "\n"
            + document.study().source().experiment().title()
            + "\n"
            + document.plan().designId()
            + "\n"
            + document.plan().designHash());
    this.bulkConcentration.clear();
    this.bulkSolvent.clear();
    this.bulkSource.clear();
    this.bulkUnit.setValue(ConcentrationUnit.MOL_PER_LITRE);
    this.bulkPending = false;
    final var plan = document.plan();
    this.title.setText(plan.title());
    this.ingredients
        .getItems()
        .setAll(plan.ingredients().stream().map(IngredientRow::new).toList());
    this.allRows.setAll(plan.runs().stream().map(VialRow::new).toList());
    this.reference
        .getItems()
        .setAll(
            plan.runs().stream()
                .map(r -> new Choice(r.id(), r.position() + " · " + r.id()))
                .toList());
    this.reference.setValue(
        this.reference.getItems().stream()
            .filter(c -> c.id().equals(plan.referenceRunId()))
            .findFirst()
            .orElseThrow());
    this.aliquot.setText(
        plan.referenceAliquot() == null ? "" : plan.referenceAliquot().value().toPlainString());
    this.aliquotUnit.setValue(
        plan.referenceAliquot() == null
            ? LaboratoryQuantityUnit.MILLILITRE
            : plan.referenceAliquot().unit());
    this.dead.setText(plan.deadVolume().value().toPlainString());
    this.deadUnit.setValue(plan.deadVolume().unit());
    this.reserve.setText(plan.reservePercent().toPlainString());
    this.diluent.setText(plan.diluent());
    this.compatibility.setText(plan.compatibilityBasis());
    this.noteText.setText(plan.note());
    this.changing = false;
    this.dirty = false;
    this.reviewed = null;
    this.computed = null;
    this.computedRows.clear();
    this.save.setDisable(true);
    this.matrixColumns();
    this.summary.setText(
        plan.runs().size() + " позиций · исходный снимок исследования сохранён внутри плана");
    this.instructions.clear();
    this.clearBlockers();
    this.diagnostics.getChildren().clear();
    this.totals.getItems().clear();
    this.details.clear();
  }

  private LaboratoryDispensingCodec.Document current() {
    final var old = this.initial.plan();
    final var ingredients = this.ingredients.getItems().stream().map(IngredientRow::value).toList();
    final var runs =
        this.allRows.stream()
            .map(r -> new Run(r.run.id(), r.run.position(), r.included.get(), r.run.amounts()))
            .toList();
    final var plan =
        new LaboratoryDispensingPlan(
            old.id(),
            this.title.getText(),
            old.createdAt(),
            old.designId(),
            old.designHash(),
            runs,
            ingredients,
            this.reference.getValue().id(),
            this.aliquot.getText().isBlank()
                ? null
                : new LaboratoryQuantity(
                    number(this.aliquot.getText()), this.aliquotUnit.getValue()),
            number(this.reserve.getText()),
            new LaboratoryQuantity(number(this.dead.getText()), this.deadUnit.getValue()),
            this.diluent.getText(),
            this.compatibility.getText(),
            this.noteText.getText());
    return new LaboratoryDispensingCodec.Document(this.initial.study(), plan);
  }

  private void calculate() {
    this.reviewed = null;
    this.save.setDisable(true);
    final var document = this.current();
    final var result =
        LaboratoryDispensingService.calculate(
            document.study().source(), document.study().design(), document.plan());
    this.computed = result;
    this.computedRows.clear();
    result.vials().forEach(v -> this.computedRows.put(v.runId(), v));
    this.matrixColumns();
    this.totals.getItems().setAll(result.totals());
    this.instructions.setText(result.instructions());
    this.diagnostics.getChildren().clear();
    this.showBlockers(result);
    result.warnings().forEach(w -> this.diagnostics.getChildren().add(note(w)));
    this.summary.setText(
        result.vials().size()
            + " включённых позиций · общая смесь "
            + label(result.finalVolume())
            + " · "
            + (result.ready()
                ? "план рассчитан"
                : "есть препятствия: " + result.blockers().size()));
    if (result.ready()) {
      this.reviewed = document;
      this.save.setDisable(false);
      if (this.file == null) this.dirty = true;
    }
    this.tabs.getSelectionModel().select(2);
    this.status.setText(
        result.ready()
            ? "Проверьте приготовление и позиции; готовый план можно сохранить."
            : "Общая смесь не утверждена расчётом. Исправьте состав либо исключите несовместимые компоненты/позиции.");
    this.detail();
  }

  private void clearBlockers() {
    this.blockingSummary.getChildren().clear();
    this.blockingSummary.setVisible(false);
    this.blockingSummary.setManaged(false);
    this.fullInstructions.setText("Инструкция приготовления и раздачи");
    this.fullInstructions.setExpanded(true);
  }

  private void showBlockers(final LaboratoryDispensingService.Result result) {
    this.clearBlockers();
    if (result.ready()) return;
    final Map<String, List<String>> groups = new LinkedHashMap<>();
    final Map<String, String> captions = new LinkedHashMap<>();
    for (final String blocker : result.blockers()) {
      String key = blocker, caption = blocker;
      boolean found = false;
      for (final var row : this.allRows) {
        if (!row.included.get()) continue;
        for (final var ingredient : this.ingredients.getItems()) {
          final String prefix = row.run.position() + " · " + ingredient.name + ": ";
          if (blocker.startsWith(prefix)) {
            final String reason = blocker.substring(prefix.length());
            key = ingredient.id + "\u0000" + reason;
            caption = ingredient.name + ": " + reason;
            found = true;
            break;
          }
        }
        if (found) break;
      }
      groups.computeIfAbsent(key, k -> new ArrayList<>()).add(blocker);
      captions.put(key, caption);
    }
    this.blockingSummary
        .getChildren()
        .add(
            note(
                "План не готов к приготовлению · причин: "
                    + groups.size()
                    + " · замечаний по позициям: "
                    + result.blockers().size()
                    + ". Частичные количества ниже не являются готовой инструкцией."));
    for (final var entry : groups.entrySet()) {
      final List<String> full = entry.getValue();
      final String caption = captions.get(entry.getKey());
      if (full.size() == 1) this.blockingSummary.getChildren().add(note("• " + caption));
      else {
        final VBox positions = new VBox(5);
        full.forEach(v -> positions.getChildren().add(note(v)));
        final TitledPane details =
            new TitledPane(caption + " · " + full.size() + " позиций", positions);
        details.setExpanded(false);
        details.setAnimated(false);
        details.setTooltip(new Tooltip(caption));
        details.setWrapText(true);
        this.blockingSummary.getChildren().add(details);
      }
    }
    this.blockingSummary.setVisible(true);
    this.blockingSummary.setManaged(true);
    this.fullInstructions.setText(
        "Полная инструкция и все замечания (" + result.blockers().size() + ")");
    this.fullInstructions.setExpanded(false);
  }

  private void matrixColumns() {
    this.matrix.getColumns().clear();
    final var include = new TableColumn<VialRow, Boolean>("Включён");
    include.setCellValueFactory(v -> v.getValue().included);
    include.setCellFactory(CheckBoxTableCell.forTableColumn(include));
    include.setPrefWidth(85);
    include.setSortable(false);
    this.matrix.getColumns().add(include);
    this.matrix
        .getColumns()
        .addAll(
            read("Позиция", r -> r.run.position(), 100),
            read("Запуск", r -> r.run.id(), 140),
            read(
                "Общая смесь",
                r -> {
                  final var v = this.computedRows.get(r.run.id());
                  return !r.included.get()
                      ? "Исключён"
                      : v == null
                          ? "Пересчитать"
                          : v.commonAliquot() == null ? "—" : label(v.commonAliquot());
                },
                155));
    for (final var ingredient : this.ingredients.getItems()) {
      if (this.individualOnly.isSelected() && ingredient.mode.get() == Mode.COMMON) continue;
      this.matrix
          .getColumns()
          .add(
              read(
                  ingredient.name + (ingredient.mode.get() == Mode.COMMON ? " · внутри смеси" : ""),
                  row -> {
                    final var vial = this.computedRows.get(row.run.id());
                    if (vial == null) return row.included.get() ? "Пересчитать" : "Исключён";
                    return vial.additions().stream()
                        .filter(d -> d.componentId().equals(ingredient.id))
                        .map(d -> label(d.dispense()))
                        .findFirst()
                        .orElse("—");
                  },
                  190));
    }
    this.matrix.refresh();
  }

  private void detail() {
    final var row = this.matrix.getSelectionModel().getSelectedItem();
    if (row == null) {
      this.details.clear();
      return;
    }
    final StringBuilder out =
        new StringBuilder(row.run.position()).append(" · ").append(row.run.id()).append("\n");
    final var vial = this.computedRows.get(row.run.id());
    if (vial == null) {
      out.append(
          row.included.get() ? "Пересчитайте раздачу." : "Позиция исключена из приготовления.");
    } else {
      if (vial.commonAliquot() != null)
        out.append("Общая смесь: ").append(label(vial.commonAliquot())).append("\n");
      for (final var d : vial.additions()) {
        out.append(component(d.componentId()).molecule().displayName())
            .append(" · план ")
            .append(label(d.planned()))
            .append(" → ")
            .append(label(d.dispense()))
            .append(d.common() ? " внутри общей смеси" : " отдельно")
            .append("\n")
            .append(d.explanation())
            .append("\n");
      }
    }
    this.details.setText(out.toString());
  }

  private void include(final boolean value, final boolean selected) {
    final var rows =
        selected
            ? List.copyOf(this.matrix.getSelectionModel().getSelectedItems())
            : List.copyOf(this.allRows);
    if (rows.isEmpty()) throw new IllegalArgumentException("Выберите строки матрицы.");
    this.changing = true;
    try {
      rows.forEach(r -> r.included.set(value));
    } finally {
      this.changing = false;
    }
    this.changed();
  }

  private void mode(final Mode value) {
    final var selected = List.copyOf(this.ingredients.getSelectionModel().getSelectedItems());
    if (selected.isEmpty()) throw new IllegalArgumentException("Выберите компоненты в таблице.");
    this.changing = true;
    try {
      selected.forEach(r -> r.mode.set(value));
    } finally {
      this.changing = false;
    }
    this.changed();
    this.ingredients.refresh();
  }

  private void bulk() {
    final var selected = List.copyOf(this.ingredients.getSelectionModel().getSelectedItems());
    if (selected.isEmpty()) throw new IllegalArgumentException("Выберите компоненты в таблице.");
    new Concentration(number(this.bulkConcentration.getText()), this.bulkUnit.getValue());
    if (this.bulkSolvent.getText().isBlank() || this.bulkSource.getText().isBlank())
      throw new IllegalArgumentException("Укажите растворитель и источник концентрации.");
    this.changing = true;
    try {
      for (final var row : selected) {
        row.concentration.set(this.bulkConcentration.getText());
        row.unit.set(this.bulkUnit.getValue());
        row.solvent.set(this.bulkSolvent.getText());
        row.source.set(this.bulkSource.getText());
        if (row.mode.get() == Mode.DIRECT) row.mode.set(Mode.INDIVIDUAL);
      }
    } finally {
      this.changing = false;
    }
    this.changed();
    this.bulkPending = false;
    this.ingredients.refresh();
  }

  private void properties() {
    final var selected = List.copyOf(this.ingredients.getSelectionModel().getSelectedItems());
    if (selected.isEmpty()) throw new IllegalArgumentException("Выберите компоненты.");
    for (final var row : selected) {
      final var c = component(row.id);
      if (c.properties().molarity() != null)
        LaboratoryDispensingPlan.number(c.properties().molarity());
      if (!c.properties().matches(c.molecule())
          || c.properties().molarity() == null
          || c.properties().molarity().signum() <= 0)
        throw new IllegalArgumentException(
            "У «" + row.name + "» нет подтверждённой положительной молярности.");
    }
    for (final var row : selected) {
      final var p = component(row.id).properties();
      row.concentration.set(p.molarity().toPlainString());
      row.unit.set(ConcentrationUnit.MOL_PER_LITRE);
      row.source.set(p.source());
      if (row.mode.get() == Mode.DIRECT) row.mode.set(Mode.INDIVIDUAL);
    }
    this.status.setText(
        "Молярность перенесена из свойств. Укажите растворитель и проверьте соответствие исходному раствору.");
  }

  private void save(final boolean copy) {
    if (this.conflict && !copy)
      throw new IllegalStateException(
          "Файл плана изменён. Откройте актуальную версию или сохраните копию под другим именем.");
    if (this.reviewed == null || this.computed == null || !this.computed.ready())
      throw new IllegalArgumentException("Сначала рассчитайте текущую раздачу без препятствий.");
    final var document = this.reviewed;
    Path destination = this.file;
    String expected = this.fingerprint;
    if (copy || destination == null) {
      final var chooser = chooser("Сохранить план раздачи");
      chooser.setInitialFileName("dispensing.qdispense");
      final var chosen = chooser.showSaveDialog(this.owner);
      if (chosen == null) return;
      destination = chosen.toPath();
      if (!destination.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".qdispense"))
        destination = destination.resolveSibling(destination.getFileName() + ".qdispense");
      expected = null;
    }
    final Path target = destination;
    final String fingerprint = expected;
    this.job(
        () -> this.repository.write(target, document, fingerprint),
        hash -> {
          this.file = target;
          this.fingerprint = hash;
          this.initial = document;
          this.dirty = false;
          this.conflict = false;
          this.status.setText(
              "План раздачи сохранён: "
                  + target
                  + ". "
                  + (this.bulkPending
                      ? "В массовом заполнении остался неприменённый ввод."
                      : "Фактические количества не изменены."));
        });
  }

  private void load() {
    if (this.pending() && !this.conflict)
      throw new IllegalStateException(
          "Сохраните текущий план или сбросьте несохранённое перед открытием другого.");
    final var chosen = chooser("Открыть план раздачи").showOpenDialog(this.owner);
    if (chosen == null) return;
    this.job(
        () -> this.repository.read(chosen.toPath()),
        opened -> {
          this.file = chosen.toPath();
          this.fingerprint = opened.fingerprint();
          this.restore(opened.document());
          this.conflict = false;
          this.calculate();
          this.dirty = false;
          this.status.setText("Открыт сохранённый план и его исходный снимок исследования.");
        });
  }

  private FileChooser chooser(final String title) {
    final var chooser = new FileChooser();
    chooser.setTitle(title);
    chooser
        .getExtensionFilters()
        .add(new FileChooser.ExtensionFilter("План раздачи QDISPENSE", "*.qdispense"));
    final Path directory = this.file != null ? this.file.toAbsolutePath().getParent() : this.parent;
    if (directory != null && java.nio.file.Files.isDirectory(directory))
      chooser.setInitialDirectory(directory.toFile());
    return chooser;
  }

  private <T> void job(final java.util.concurrent.Callable<T> task, final Consumer<T> complete) {
    this.busy(true);
    this.tabs.setDisable(true);
    Thread.startVirtualThread(
        () -> {
          try {
            final var value = task.call();
            Platform.runLater(
                () -> {
                  if (this.disposed) return;
                  this.busy(false);
                  this.tabs.setDisable(false);
                  try {
                    complete.accept(value);
                  } catch (final Exception error) {
                    this.status.setText(error.getMessage());
                  }
                });
          } catch (final Exception error) {
            Platform.runLater(
                () -> {
                  if (!this.disposed) {
                    this.busy(false);
                    this.tabs.setDisable(false);
                    this.status.setText(error.getMessage());
                  }
                });
          }
        });
  }

  boolean pending() {
    return this.dirty || this.bulkPending || this.conflict;
  }

  @Override
  boolean canClose() {
    if (this.running) {
      this.status.setText("Дождитесь завершения файловой операции.");
      return false;
    }
    if (this.pending()) {
      this.status.setText(
          this.bulkPending
              ? "Примените массовое заполнение или сбросьте ввод перед выходом из исследования."
              : "Сохраните план раздачи либо сбросьте несохранённые изменения перед выходом из исследования.");
      return false;
    }
    return true;
  }

  private void reset() {
    if (this.conflict && this.file != null) {
      this.job(
          () -> this.repository.read(this.file),
          opened -> {
            this.fingerprint = opened.fingerprint();
            this.restore(opened.document());
            this.conflict = false;
            this.status.setText("Открыта актуальная сохранённая версия плана раздачи.");
          });
      return;
    }
    this.restore(this.initial);
    this.status.setText("Несохранённый ввод раздачи сброшен. Файлы не изменены.");
  }

  private Map<String, Control> draftFields() {
    final Map<String, Control> fields = new LinkedHashMap<>();
    fields.put("title", this.title);
    fields.put("aliquot", this.aliquot);
    fields.put("aliquotUnit", this.aliquotUnit);
    fields.put("reserve", this.reserve);
    fields.put("dead", this.dead);
    fields.put("deadUnit", this.deadUnit);
    fields.put("diluent", this.diluent);
    fields.put("compatibility", this.compatibility);
    fields.put("note", this.noteText);
    fields.put("bulkConcentration", this.bulkConcentration);
    fields.put("bulkUnit", this.bulkUnit);
    fields.put("bulkSolvent", this.bulkSolvent);
    fields.put("bulkSource", this.bulkSource);
    fields.put("filter", this.filter);
    fields.put("individualOnly", this.individualOnly);
    return fields;
  }

  void draft(final Properties properties, final boolean restore) {
    final String prefix = "design.dispensing.";
    if (!restore) {
      properties.setProperty(
          prefix + "document", new LaboratoryDispensingCodec().write(this.initial));
      properties.setProperty(prefix + "parent", this.parent == null ? "" : this.parent.toString());
      properties.setProperty(prefix + "file", this.file == null ? "" : this.file.toString());
      properties.setProperty(prefix + "fingerprint", Objects.toString(this.fingerprint, ""));
      properties.setProperty(prefix + "dirty", Boolean.toString(this.dirty));
      properties.setProperty(prefix + "bulkPending", Boolean.toString(this.bulkPending));
      properties.setProperty(prefix + "conflict", Boolean.toString(this.conflict));
      properties.setProperty(
          prefix + "tab", Integer.toString(this.tabs.getSelectionModel().getSelectedIndex()));
      properties.setProperty(prefix + "reference", this.reference.getValue().id());
      LaboratoryFormDraft.fields(properties, prefix + "input", false, this.draftFields());
      for (int i = 0; i < this.ingredients.getItems().size(); i++) {
        final var row = this.ingredients.getItems().get(i);
        final String key = prefix + "ingredient." + i + ".";
        properties.setProperty(key + "id", row.id);
        properties.setProperty(key + "mode", row.mode.get().name());
        properties.setProperty(key + "concentration", row.concentration.get());
        properties.setProperty(key + "unit", row.unit.get().name());
        properties.setProperty(key + "label", row.label.get());
        properties.setProperty(key + "solvent", row.solvent.get());
        properties.setProperty(key + "source", row.source.get());
      }
      for (int i = 0; i < this.allRows.size(); i++) {
        final var row = this.allRows.get(i);
        properties.setProperty(prefix + "run." + i + ".id", row.run.id());
        properties.setProperty(
            prefix + "run." + i + ".included", Boolean.toString(row.included.get()));
      }
      return;
    }
    this.changing = true;
    try {
      LaboratoryFormDraft.fields(properties, prefix + "input", true, this.draftFields());
      this.reference.setValue(
          this.reference.getItems().stream()
              .filter(c -> c.id().equals(properties.getProperty(prefix + "reference")))
              .findFirst()
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Опорная позиция черновика раздачи не найдена.")));
      for (int i = 0; i < this.ingredients.getItems().size(); i++) {
        final var row = this.ingredients.getItems().get(i);
        final String key = prefix + "ingredient." + i + ".";
        if (!row.id.equals(properties.getProperty(key + "id")))
          throw new IllegalArgumentException(
              "Компоненты черновика раздачи не соответствуют исходному снимку.");
        row.mode.set(Mode.valueOf(properties.getProperty(key + "mode")));
        row.concentration.set(properties.getProperty(key + "concentration", ""));
        row.unit.set(ConcentrationUnit.valueOf(properties.getProperty(key + "unit")));
        row.label.set(properties.getProperty(key + "label", ""));
        row.solvent.set(properties.getProperty(key + "solvent", ""));
        row.source.set(properties.getProperty(key + "source", ""));
      }
      for (int i = 0; i < this.allRows.size(); i++) {
        final var row = this.allRows.get(i);
        if (!row.run.id().equals(properties.getProperty(prefix + "run." + i + ".id")))
          throw new IllegalArgumentException(
              "Позиции черновика раздачи не соответствуют исходному снимку.");
        row.included.set(
            Boolean.parseBoolean(properties.getProperty(prefix + "run." + i + ".included")));
      }
      final String path = properties.getProperty(prefix + "file", "");
      this.file = path.isEmpty() ? null : Path.of(path);
      this.fingerprint = properties.getProperty(prefix + "fingerprint", "");
      if (this.fingerprint.isEmpty()) this.fingerprint = null;
      this.dirty = Boolean.parseBoolean(properties.getProperty(prefix + "dirty"));
      this.bulkPending = Boolean.parseBoolean(properties.getProperty(prefix + "bulkPending"));
      this.conflict = Boolean.parseBoolean(properties.getProperty(prefix + "conflict"));
      this.tabs
          .getSelectionModel()
          .select(
              Math.max(
                  0, Math.min(2, Integer.parseInt(properties.getProperty(prefix + "tab", "0")))));
      this.matrixColumns();
      this.status.setText(
          "Ввод раздачи восстановлен. Пересчитайте план перед сохранением или приготовлением.");
      if (this.file != null) {
        try {
          if (!Objects.equals(this.fingerprint, this.repository.read(this.file).fingerprint()))
            this.conflict = true;
        } catch (final Exception error) {
          this.conflict = true;
        }
      }
      if (this.conflict)
        this.status.setText(
            "Ввод восстановлен, но файл раздачи изменён или недоступен. Откройте актуальную версию либо сохраните копию; перезапись заблокирована.");
    } finally {
      this.changing = false;
    }
  }

  static LaboratoryDispensingPage recovered(
      final Properties properties, final Window owner, final Runnable back) {
    final String parent = properties.getProperty("design.dispensing.parent", "");
    final var document =
        new LaboratoryDispensingCodec().read(properties.getProperty("design.dispensing.document"));
    final var page =
        embedded(
            new LaboratoryDispensingPage(
                document, parent.isEmpty() ? null : Path.of(parent), owner, back),
            back);
    page.draft(properties, true);
    return page;
  }

  private static BigDecimal number(final String text) {
    try {
      return LaboratoryDispensingPlan.number(new BigDecimal(text.strip().replace(',', '.')));
    } catch (final NumberFormatException error) {
      throw new IllegalArgumentException("Введите число: «" + text + "».");
    }
  }

  private static String label(final LaboratoryQuantity quantity) {
    return LaboratoryDispensingService.label(quantity);
  }
}

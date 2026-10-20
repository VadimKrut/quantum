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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryExperimentalDesign.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionOperations.Origin;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionOperations.AnalysisRef;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionOperations.AnalysisKind;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryReactionOperationsService.AnalysisChoice;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.*;

/** Complete local study: factors, randomized run matrix, physical positions, independent plans and explicitly sourced responses. */
final class LaboratoryDesignPage extends LaboratoryComputationPage {

  private record Choice(String id, String title) {
    public String toString() {
      return this.title;
    }
  }

  private static final class Row {
    final Run original;
    final Map<String, StringProperty> levels = new LinkedHashMap<>();
    final StringProperty position = new SimpleStringProperty(),
        response = new SimpleStringProperty(),
        note = new SimpleStringProperty(),
        responseSource = new SimpleStringProperty();
    final ObjectProperty<Origin> responseOrigin = new SimpleObjectProperty<>(Origin.UNSPECIFIED);
    final ObjectProperty<AnalysisRef> responseAnalysis = new SimpleObjectProperty<>();
    LaboratoryExperiment linkedExperiment;

    Row(final Run run) {
      this.original = run;
      run.levels().forEach((key, value) -> this.levels.put(key, new SimpleStringProperty(value)));
      this.position.set(run.position());
      this.response.set(run.response() == null ? "" : run.response().toPlainString());
      this.note.set(run.responseNote());
      this.responseOrigin.set(run.responseOrigin());
      this.responseSource.set(run.responseSource());
      this.responseAnalysis.set(run.responseAnalysis());
    }
  }

  private LaboratoryDrawing source;
  private LaboratoryExperimentalDesign design;
  private final Consumer<Path> open;
  private final LaboratoryDesignRepository repository = new LaboratoryDesignRepository();
  private final TextField title = new TextField("Оптимизация синтеза"),
      seed = new TextField("2026"),
      responseName = new TextField("Выход"),
      responseUnit = new TextField("%"),
      parent = new TextField(),
      folder = new TextField("Исследование условий");
  private final LaboratoryDecimalField base = new LaboratoryDecimalField(5);
  private final Label baseApproximation = note("≈");
  private final Label sourceTitle = note("");
  private final ComboBox<Choice> stage = new ComboBox<>(),
      reference = new ComboBox<>(),
      factorComponent = new ComboBox<>();
  private final ComboBox<LaboratoryQuantityUnit> baseUnit = new ComboBox<>();
  private final Spinner<Integer> replicates = new Spinner<>(1, 16, 2),
      centers = new Spinner<>(0, 32, 0),
      plateRows = new Spinner<>(1, 26, 8),
      plateColumns = new Spinner<>(1, 48, 12);
  private final CheckBox randomized = new CheckBox("Случайный порядок запусков"),
      solvents = new CheckBox("Масштабировать растворители");
  private final ListView<Factor> factors = new ListView<>();
  private final TextField factorTitle = new TextField(),
      factorLow = new TextField(),
      factorHigh = new TextField(),
      factorUnit = new TextField();
  private final ComboBox<Type> factorType = new ComboBox<>();
  private final ComboBox<Binding> factorBinding = new ComboBox<>();
  private final TableView<Row> matrix = new TableView<>();
  private final VBox preview = new VBox(6),
      analysisInfo = new VBox(6),
      modelTerms = new VBox(5),
      plate = new VBox(8),
      runDetails = new VBox(8);
  private final Map<LaboratoryDesignPlanning.Term, CheckBox> termChecks = new LinkedHashMap<>();
  private final ComboBox<Integer> plateNumber = new ComboBox<>();
  private final NumberAxis effectAxis = new NumberAxis();
  private final CategoryAxis effectLabels = new CategoryAxis();
  private final BarChart<Number, String> chart = new BarChart<>(this.effectAxis, this.effectLabels);
  private final TableView<LaboratoryDesignPlanning.Effect> effectTable = new TableView<>();
  private final TabPane tabs = new TabPane();
  private final Label summary = note("Задайте факторы и сформируйте матрицу.");
  private final Button create, stop, save, review, openRun, generate, factorAdd;
  private final TitledPane destination = new TitledPane(),
      factorEditor = new TitledPane(),
      previewPane = new TitledPane(),
      effectDetails = new TitledPane();
  private final CheckBox resultInput = new CheckBox("Ввести результаты");
  private final VBox responseEditor = new VBox(8);
  private final Label responseContext = note(""), responseLink = note("");
  private final ComboBox<Origin> responseOrigin = new ComboBox<>();
  private final TextArea responseSource = new TextArea();
  private final ComboBox<AnalysisChoice> responseAnalyses = new ComboBox<>();
  private final CheckBox responseHistory = new CheckBox("Показать предыдущие версии");
  private Row responseRow;
  private boolean responseLoading;
  private final AtomicBoolean cancelled = new AtomicBoolean();
  private List<LaboratorySeriesPlan.Prepared> prepared = List.of();
  private LaboratoryExperimentalDesign preparedDesign;
  private Path file;
  private String fingerprint, designId = "design_" + UUID.randomUUID();
  private boolean changing, dirty, conflict;
  private final BorderPane studyView = new BorderPane();
  private final StackPane pages = new StackPane();
  private final MenuItem resumeDispensing = new MenuItem("Продолжить план раздачи");
  private LaboratoryDispensingPage dispensingPage;
  private String dispensingHint, beforeDispensingHint;

  LaboratoryDesignPage(
      final LaboratoryDrawing source,
      final Path parent,
      final Consumer<Path> openExperiment,
      final Runnable close) {
    super("Сравнить условия реакции", close);
    this.source = source;
    this.open = openExperiment;
    this.setId("chemistry-design-page");
    this.changing = true;
    this.parent.setText(parent == null ? "" : parent.toString());
    this.parent.setPromptText("Полный путь к папке проекта");
    this.baseUnit.getItems().setAll(LaboratoryQuantityUnit.values());
    this.baseUnit.setValue(LaboratoryQuantityUnit.MILLIMOLE);
    this.factorType.getItems().setAll(Type.values());
    this.factorType.setValue(Type.NUMERIC);
    this.factorBinding.getItems().setAll(Binding.values());
    this.factorBinding.setValue(Binding.TEMPERATURE_C);
    this.randomized.setSelected(true);
    this.solvents.setSelected(true);
    this.factorTitle.setText("Температура");
    this.factorLow.setText("40");
    this.factorHigh.setText("80");
    this.factorUnit.setText("°C");
    this.stage.setPrefWidth(260);
    this.reference.setPrefWidth(280);
    this.factorComponent.setPrefWidth(260);
    this.factorBinding.setPrefWidth(240);
    this.factors.setFixedCellSize(42);
    this.factors.setPrefHeight(86);
    this.factors.setPlaceholder(
        note("Добавьте параметр, который хотите сравнить, например температуру."));
    this.factors.setCellFactory(
        list ->
            new ListCell<>() {
              protected void updateItem(final Factor f, final boolean empty) {
                super.updateItem(f, empty);
                this.setText(
                    empty || f == null
                        ? null
                        : f.title()
                            + "  ·  "
                            + f.low()
                            + " / "
                            + f.high()
                            + (f.unit().isBlank() ? "" : " " + f.unit()));
                this.setTooltip(
                    empty || f == null ? null : new Tooltip(f.binding() + " · " + f.type()));
              }
            });
    this.factors
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (o, a, f) -> {
              if (f != null) {
                this.changing = true;
                this.factorTitle.setText(f.title());
                this.factorLow.setText(f.low());
                this.factorHigh.setText(f.high());
                this.factorUnit.setText(f.unit());
                this.factorType.setValue(f.type());
                this.factorBinding.setValue(f.binding());
                select(this.factorComponent, f.componentId());
                this.changing = false;
                this.factorEditor.setExpanded(true);
              }
            });
    this.stage
        .valueProperty()
        .addListener(
            (o, a, b) -> {
              if (this.changing) {
                return;
              }
              if (!this.matrix.getItems().isEmpty()) {
                this.changing = true;
                this.stage.setValue(a);
                this.changing = false;
                this.status.setText(
                    "Сначала создайте новый план: матрица связана с текущей стадией.");
                return;
              }
              this.configureReference();
              this.changed();
            });
    this.reference
        .valueProperty()
        .addListener(
            (o, a, b) -> {
              if (!this.changing) {
                if (!this.matrix.getItems().isEmpty()) {
                  this.changing = true;
                  this.reference.setValue(a);
                  this.changing = false;
                  this.status.setText("Опорный исходник закреплён в матрице.");
                } else {
                  this.defaultBase();
                  this.changed();
                }
              }
            });
    this.base.setPrefColumnCount(10);
    this.baseUnit.setPrefWidth(120);
    this.factorLow.setPrefColumnCount(8);
    this.factorHigh.setPrefColumnCount(8);
    this.factorUnit.setPrefColumnCount(5);
    this.factorTitle.setPrefColumnCount(16);
    this.baseApproximation.setId("chemistry-design-base-approximation");
    this.baseApproximation.visibleProperty().bind(this.base.tooltipProperty().isNotNull());
    this.baseApproximation.managedProperty().bind(this.baseApproximation.visibleProperty());
    this.baseApproximation.tooltipProperty().bind(this.base.tooltipProperty());
    final HBox baseInput = new HBox(5, this.base, this.baseApproximation);
    baseInput.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    final FlowPane planFields =
        new FlowPane(
            14,
            10,
            field("Опорный исходник", this.reference),
            field("Базовая загрузка", baseInput),
            field("Единица", this.baseUnit));
    this.sourceTitle.setId("chemistry-design-source-title");
    final TitledPane template =
        details(
            "Название и исходный опыт",
            new VBox(
                8,
                field("Название исследования", this.title),
                field("Исходный опыт", this.sourceTitle),
                field("Стадия", this.stage),
                this.solvents));
    final VBox componentField = field("Вещество для изменения эквивалентов", this.factorComponent);
    componentField
        .visibleProperty()
        .bind(this.factorBinding.valueProperty().isEqualTo(Binding.EQUIVALENTS));
    componentField.managedProperty().bind(componentField.visibleProperty());
    final FlowPane factorFields =
        new FlowPane(
            12,
            8,
            field("Что менять", this.factorBinding),
            field("Название", this.factorTitle),
            field("Первое значение", this.factorLow),
            field("Второе значение", this.factorHigh),
            field("Единица", this.factorUnit));
    final TitledPane factorOptions =
        details(
            "Тип значений",
            new VBox(
                8,
                field("Тип", this.factorType),
                note(
                    "Категории записываются как условия. Вещества и структуры автоматически не заменяются.")));
    final TitledPane advanced =
        details(
            "Порядок опытов, планшет и отклик",
            new VBox(
                10,
                new FlowPane(
                    12,
                    8,
                    field("Центральных точек", this.centers),
                    field("Seed порядка", this.seed),
                    this.randomized),
                new FlowPane(
                    12,
                    8,
                    field("Рядов штатива", this.plateRows),
                    field("Колонок", this.plateColumns),
                    field("Отклик для сравнения", this.responseName),
                    field("Единица отклика", this.responseUnit))));
    this.generate = this.button("Сформировать варианты опытов", "generate", this::generate);
    this.generate.getStyleClass().add("laboratory-primary-action");
    this.factorAdd = this.button("Добавить условие", "factor-add", () -> this.factor(false));
    this.factorEditor.setText("Добавить или изменить условие");
    this.factorEditor.setId("chemistry-design-factor-editor");
    this.factorEditor.setAnimated(false);
    this.factorEditor.setExpanded(true);
    final Button
        updateFactor = this.button("Изменить выбранное", "factor-update", () -> this.factor(true)),
        deleteFactor = this.button("Удалить выбранное", "factor-delete", this::removeFactor);
    for (final Button action : List.of(updateFactor, deleteFactor)) {
      action
          .visibleProperty()
          .bind(this.factors.getSelectionModel().selectedItemProperty().isNotNull());
      action.managedProperty().bind(action.visibleProperty());
    }
    this.factorEditor.setContent(
        new VBox(
            10,
            factorFields,
            componentField,
            factorOptions,
            new FlowPane(8, 8, this.factorAdd, updateFactor, deleteFactor)));
    final FlowPane generation =
        new FlowPane(12, 8, field("Повторов каждой комбинации", this.replicates), this.generate);
    generation
        .visibleProperty()
        .bind(javafx.beans.binding.Bindings.isNotEmpty(this.factors.getItems()));
    generation.managedProperty().bind(generation.visibleProperty());
    final VBox setup =
        new VBox(
            12,
            note("Сравните условия одной реакции в нескольких опытах."),
            planFields,
            new Label("Условия для сравнения"),
            this.factors,
            this.factorEditor,
            generation,
            template,
            advanced);
    setup.setPadding(new Insets(16));
    this.matrix.setEditable(false);
    this.matrix.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    this.matrix.setMinHeight(Region.USE_PREF_SIZE);
    this.matrix.setPrefHeight(114);
    this.matrix.setFixedCellSize(40);
    this.matrix.setPlaceholder(
        note("Сначала добавьте параметры на вкладке «План» и сформируйте варианты опытов."));
    this.matrix.setId("chemistry-design-matrix");
    this.matrix
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (o, a, b) -> {
              this.preview();
              this.responseEditor();
            });
    this.plateNumber.valueProperty().addListener((o, a, b) -> this.showPlate());
    this.review = this.button("Рассчитать загрузки", "review", this::review);
    this.review.getStyleClass().add("laboratory-primary-action");
    this.create = this.button("Создать опыты", "create", this::publish);
    this.create.getStyleClass().add("laboratory-primary-action");
    this.create.setVisible(false);
    this.create.setManaged(false);
    this.openRun = this.button("Открыть выбранный опыт", "open-run", this::openRun);
    this.resumeDispensing.setVisible(false);
    this.resumeDispensing.setOnAction(e -> this.showDispensing());
    final MenuButton dispense = new MenuButton("Смеси и раздача");
    dispense.setId("chemistry-design-dispensing-menu");
    dispense
        .getItems()
        .addAll(
            this.resumeDispensing,
            menu("Подготовить раздачу этой матрицы…", this::dispensing),
            menu("Открыть сохранённый план раздачи…", this::openDispensing));
    final TitledPane positions =
        details(
            "Расположение на планшете",
            new VBox(8, new FlowPane(8, 8, note("Планшет"), this.plateNumber), this.plate));
    this.destination.setText("Куда создать опыты");
    this.destination.setContent(
        new FlowPane(
            10, 8, field("Папка проекта", this.parent), field("Новая папка опытов", this.folder)));
    this.destination.setExpanded(this.parent.getText().isBlank());
    this.destination.setAnimated(false);
    this.destination.setId("chemistry-design-destination");
    this.previewPane.setText("Количества выбранного опыта");
    this.previewPane.setContent(this.preview);
    this.previewPane.setExpanded(false);
    this.previewPane.setAnimated(false);
    this.previewPane.setId("chemistry-design-quantities");
    this.resultInput.setId("chemistry-design-result-input");
    this.resultInput.setTooltip(
        new Tooltip(
            "Показать отклик, происхождение и источник выбранного опыта. Скрытие не удаляет введённые данные."));
    this.configureResponseEditor();
    this.resultInput.selectedProperty().addListener((o, a, b) -> this.resultColumns());
    final Label responseHint =
        note(
            "Пустой отклик — не задан. Для нового значения выберите происхождение и укажите источник. Модель и литература не являются измерениями этого опыта.");
    responseHint.visibleProperty().bind(this.resultInput.selectedProperty());
    responseHint.managedProperty().bind(responseHint.visibleProperty());
    final VBox matrixPane =
        new VBox(
            12,
            this.summary,
            new FlowPane(8, 8, this.review, this.create, this.openRun, dispense, this.resultInput),
            this.matrix,
            responseHint,
            this.responseEditor,
            this.previewPane,
            positions,
            this.runDetails,
            this.destination);
    matrixPane.setPadding(new Insets(16));
    this.chart.setAnimated(false);
    this.chart.setLegendVisible(false);
    this.chart.setMinHeight(230);
    this.chart.setPrefHeight(310);
    this.effectAxis.setLabel("Эффект в единицах отклика");
    this.effectLabels.setLabel("");
    this.chart.setId("chemistry-design-effects");
    this.effectTable.setPrefHeight(180);
    this.effectTable.setMinHeight(Region.USE_PREF_SIZE);
    this.effectTable
        .getColumns()
        .addAll(
            readColumn("Фактор / взаимодействие", LaboratoryDesignPlanning.Effect::label, 260),
            readColumn("Эффект", e -> number(e.effect()), 120),
            readColumn(
                "Ст. ошибка",
                e -> e.standardError() == null ? "Не оценена" : number(e.standardError()),
                130));
    final Button analyze = this.button("Сравнить результаты", "analyze", this::analyze);
    analyze.getStyleClass().add("laboratory-primary-action");
    this.effectDetails.setText("Численные результаты");
    this.effectDetails.setContent(this.effectTable);
    this.effectDetails.setExpanded(false);
    this.effectDetails.setAnimated(false);
    final VBox analysisPane =
        new VBox(
            12,
            note("Как выбранные параметры связаны с введённым откликом?"),
            analyze,
            details("Какие факторы и взаимодействия учитывать", this.modelTerms),
            this.analysisInfo,
            this.chart,
            this.effectDetails,
            details(
                "Как понимать расчёт",
                note(
                    "Метод: NIST/SEMATECH, least squares DOE models; разброс повторов оценивается по введённым данным. Значимость и оптимальные условия автоматически не объявляются. План не подтверждает измерения.")));
    analysisPane.setPadding(new Insets(16));
    this.clearAnalysis();
    this.tabs
        .getTabs()
        .setAll(
            new Tab("План", sheet(setup)),
            new Tab("Опыты и загрузки", sheet(matrixPane)),
            new Tab("Анализ результатов", sheet(analysisPane)));
    this.tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    this.setCenter(this.tabs);
    this.save = this.button("Сохранить исследование", "save", this::save);
    this.stop = this.button("Остановить", "stop", () -> this.cancelled.set(true));
    this.stop.setVisible(false);
    this.stop.setManaged(false);
    final MenuButton files = new MenuButton("Исследование");
    files.setId("chemistry-design-file-menu");
    files
        .getItems()
        .addAll(
            menu("Открыть исследование…", this::load),
            menu("История сохранений…", this::history),
            new SeparatorMenuItem(),
            menu("Новый план из этих факторов", this::newStudy),
            menu("Сбросить несохранённое", this::resetDraft));
    final VBox footer = new VBox(8, new FlowPane(8, 8, this.save, files, this.stop), this.status);
    footer.setPadding(new Insets(10, 16, 14, 16));
    this.setBottom(footer);
    for (final TextField input :
        List.of(
            this.title,
            this.seed,
            this.responseName,
            this.responseUnit,
            this.factorTitle,
            this.factorLow,
            this.factorHigh,
            this.factorUnit)) {
      input.textProperty().addListener((o, a, b) -> this.changed());
    }
    this.parent.textProperty().addListener((o, a, b) -> this.destinationChanged());
    this.folder.textProperty().addListener((o, a, b) -> this.destinationChanged());
    this.base.valueRevisionProperty().addListener((o, a, b) -> this.changed());
    this.baseUnit.valueProperty().addListener((o, a, b) -> this.changed());
    this.randomized.selectedProperty().addListener((o, a, b) -> this.changed());
    this.solvents.selectedProperty().addListener((o, a, b) -> this.changed());
    this.factorType.valueProperty().addListener((o, a, b) -> this.changed());
    this.factorBinding.valueProperty().addListener((o, a, b) -> this.changed());
    this.factorComponent.valueProperty().addListener((o, a, b) -> this.changed());
    this.replicates.valueProperty().addListener((o, a, b) -> this.changed());
    this.centers.valueProperty().addListener((o, a, b) -> this.changed());
    this.plateRows.valueProperty().addListener((o, a, b) -> this.changed());
    this.plateColumns.valueProperty().addListener((o, a, b) -> this.changed());
    this.configureStages();
    this.ready(false);
    this.refreshFactors();
    this.changing = false;
    this.dirty = false;
    final var header = this.getTop();
    final var footerView = this.getBottom();
    this.setTop(null);
    this.setBottom(null);
    this.setCenter(null);
    this.studyView.setTop(header);
    this.studyView.setCenter(this.tabs);
    this.studyView.setBottom(footerView);
    this.pages.getChildren().add(this.studyView);
    this.setCenter(this.pages);
  }

  private static ScrollPane sheet(final javafx.scene.Node content) {
    final ScrollPane pane = scroll(content);
    pane.setFitToHeight(false);
    return pane;
  }

  private static TitledPane details(final String title, final javafx.scene.Node content) {
    final TitledPane pane = new TitledPane(title, content);
    pane.setExpanded(false);
    pane.setAnimated(false);
    return pane;
  }

  private MenuItem menu(final String title, final Runnable action) {
    final MenuItem item = new MenuItem(title);
    item.setOnAction(
        e -> {
          if (!this.running) {
            try {
              action.run();
            } catch (final RuntimeException x) {
              this.status.setText(x.getMessage());
            }
          }
        });
    return item;
  }

  private void clearAnalysis() {
    this.chart.setVisible(false);
    this.chart.setManaged(false);
    this.effectTable.setVisible(false);
    this.effectTable.setManaged(false);
    this.effectDetails.setVisible(false);
    this.effectDetails.setManaged(false);
    this.analysisInfo
        .getChildren()
        .setAll(note("Введите отклики в таблице опытов, затем нажмите «Сравнить результаты»."));
  }

  private void refreshFactors() {
    final int count = this.factors.getItems().size();
    this.factors.setVisible(count > 0);
    this.factors.setManaged(count > 0);
    this.factors.setPrefHeight(Math.min(212, 42 * count + 2));
    this.generate.setDisable(count == 0);
    this.factorAdd.getStyleClass().remove("laboratory-primary-action");
    if (count == 0) this.factorAdd.getStyleClass().add("laboratory-primary-action");
  }

  private void resultColumns() {
    final int size = this.matrix.getColumns().size();
    if (size >= 4) {
      for (int i = size - 3; i < size; i++)
        this.matrix.getColumns().get(i).setVisible(this.resultInput.isSelected());
    }
    this.responseEditor.setVisible(this.resultInput.isSelected() && this.responseRow != null);
    this.responseEditor.setManaged(this.responseEditor.isVisible());
  }

  private void updateSummary() {
    final int count = this.matrix.getItems().size();
    final long responses =
        this.matrix.getItems().stream().filter(r -> !r.response.get().isBlank()).count();
    this.summary.setText(
        count == 0
            ? "Добавьте условия и сформируйте варианты опытов."
            : count
                + " опытов · "
                + this.factors.getItems().size()
                + " условий · результатов "
                + responses
                + " из "
                + count);
  }

  private void ready(final boolean value) {
    final boolean published =
        this.matrix.getItems().stream().anyMatch(r -> !r.original.experimentFile().isBlank());
    this.create.setVisible(value && !published);
    this.create.setManaged(value && !published);
    this.openRun.setVisible(published);
    this.openRun.setManaged(published);
    this.openRun.getStyleClass().remove("laboratory-primary-action");
    if (value && published) this.openRun.getStyleClass().add("laboratory-primary-action");
    this.review.getStyleClass().remove("laboratory-primary-action");
    if (!value) this.review.getStyleClass().add("laboratory-primary-action");
  }

  private static VBox field(final String name, final javafx.scene.Node node) {
    return new VBox(4, note(name), node);
  }

  private Button button(final String text, final String id, final Runnable action) {
    final var button = new Button(text);
    button.setId("chemistry-design-" + id);
    button.setOnAction(
        event -> {
          if (!this.running || id.equals("stop")) {
            try {
              action.run();
            } catch (final Exception exception) {
              this.status.setText(exception.getMessage());
            }
          }
        });
    return button;
  }

  private void destinationChanged() {
    if (!this.changing) {
      this.dirty = true;
    }
  }

  private void changed() {
    if (!this.changing) {
      this.dirty = true;
      this.prepared = List.of();
      this.preparedDesign = null;
      this.ready(false);
      this.clearAnalysis();
      this.preview();
      this.updateSummary();
      this.status.setText(
          this.matrix.getItems().isEmpty()
              ? "Параметры плана изменены. Сформируйте варианты опытов."
              : "Параметры изменены. Рассчитайте загрузки заново.");
    }
  }

  private void configureStages() {
    this.sourceTitle.setText(this.source.experiment().title());
    this.stage
        .getItems()
        .setAll(
            this.source.experiment().stages().stream()
                .map(s -> new Choice(s.id(), s.title()))
                .toList());
    this.stage.getSelectionModel().selectFirst();
    this.configureReference();
  }

  private void configureReference() {
    final boolean before = this.changing;
    this.changing = true;
    this.reference.getItems().clear();
    this.factorComponent.getItems().clear();
    if (this.stage.getValue() != null) {
      final var s =
          this.source.experiment().stages().stream()
              .filter(v -> v.id().equals(this.stage.getValue().id()))
              .findFirst()
              .orElseThrow();
      for (final var p : s.participants()) {
        final var c =
            this.source.experiment().components().stream()
                .filter(v -> v.id().equals(p.componentId()))
                .findFirst()
                .orElseThrow();
        final var choice = new Choice(c.id(), c.molecule().displayName());
        if (p.role() == ReactionComponentRole.REACTANT) {
          this.reference.getItems().add(choice);
        }
        if (p.role() != ReactionComponentRole.PRODUCT
            && p.role() != ReactionComponentRole.SOLVENT) {
          this.factorComponent.getItems().add(choice);
        }
      }
    }
    this.reference.getSelectionModel().selectFirst();
    this.factorComponent.getSelectionModel().selectFirst();
    this.defaultBase();
    this.changing = before;
  }

  private void defaultBase() {
    if (this.reference.getValue() != null) {
      final var c =
          this.source.experiment().components().stream()
              .filter(v -> v.id().equals(this.reference.getValue().id()))
              .findFirst()
              .orElseThrow();
      if (c.planned() != null) {
        this.base.display(c.planned().value());
        this.baseUnit.setValue(c.planned().unit());
      } else {
        this.base.display(null);
      }
    }
  }

  private void factor(final boolean update) {
    if (!this.matrix.getItems().isEmpty()) {
      throw new IllegalArgumentException(
          "Факторы уже связаны с матрицей. Создайте новый план из этих факторов для изменения структуры исследования.");
    }
    final var previous = update ? this.factors.getSelectionModel().getSelectedItem() : null;
    if (update && previous == null) {
      throw new IllegalArgumentException("Выберите фактор.");
    }
    final var binding = this.factorBinding.getValue();
    final var f =
        new Factor(
            previous == null ? "factor_" + UUID.randomUUID() : previous.id(),
            this.factorTitle.getText(),
            this.factorType.getValue(),
            binding,
            binding == Binding.EQUIVALENTS
                ? Objects.requireNonNull(this.factorComponent.getValue(), "Выберите компонент.")
                    .id()
                : null,
            this.factorUnit.getText(),
            this.factorLow.getText(),
            this.factorHigh.getText());
    if (previous == null) {
      if (this.factors.getItems().size() >= 6) {
        throw new IllegalArgumentException("В одном полном плане поддерживается до 6 факторов.");
      }
      this.factors.getItems().add(f);
    } else {
      this.factors.getItems().set(this.factors.getItems().indexOf(previous), f);
    }
    this.changed();
    this.refreshFactors();
    this.factorEditor.setExpanded(false);
  }

  private void removeFactor() {
    if (!this.matrix.getItems().isEmpty()) {
      throw new IllegalArgumentException("Сначала создайте новый план без матрицы.");
    }
    this.factors.getItems().remove(this.factors.getSelectionModel().getSelectedItem());
    this.changed();
    this.refreshFactors();
    if (this.factors.getItems().isEmpty()) this.factorEditor.setExpanded(true);
  }

  private LaboratoryExperimentalDesign settings() {
    if (this.stage.getValue() == null || this.reference.getValue() == null) {
      throw new IllegalArgumentException("Нужна стадия с опорным исходником.");
    }
    return new LaboratoryExperimentalDesign(
        this.designId,
        this.title.getText(),
        this.stage.getValue().id(),
        this.reference.getValue().id(),
        new LaboratoryQuantity(
            LaboratoryExperimentalDesign.number(LaboratoryDecimalField.snapshot(this.base)),
            this.baseUnit.getValue()),
        List.copyOf(this.factors.getItems()),
        List.of(),
        Long.parseLong(this.seed.getText().strip()),
        this.responseName.getText(),
        this.responseUnit.getText(),
        this.solvents.isSelected(),
        this.plateRows.getValue(),
        this.plateColumns.getValue());
  }

  private void generate() {
    if (this.matrix.getItems().stream()
        .anyMatch(
            r ->
                !r.response.get().isBlank()
                    || !r.note.get().isBlank()
                    || r.responseOrigin.get() != Origin.UNSPECIFIED
                    || !r.responseSource.get().isBlank()
                    || r.responseAnalysis.get() != null
                    || !r.original.experimentFile().isBlank())) {
      throw new IllegalArgumentException(
          "Матрица содержит результаты или созданные опыты. Сохраните её и создайте новый план.");
    }
    this.design =
        LaboratoryDesignPlanning.generate(
            this.settings(),
            this.replicates.getValue(),
            this.centers.getValue(),
            this.randomized.isSelected());
    this.rows(this.design);
    this.changed();
    this.tabs.getSelectionModel().select(1);
    this.status.setText(
        "Матрица сформирована. Проверьте загрузки всех запусков перед созданием опытов.");
  }

  private LaboratoryExperimentalDesign current() {
    if (this.design == null) {
      throw new IllegalArgumentException("Сначала сформируйте матрицу.");
    }
    final var settings = this.settings();
    final var runs = new ArrayList<Run>();
    for (final var row : this.matrix.getItems()) {
      final var levels = new LinkedHashMap<String, String>();
      row.levels.forEach((id, value) -> levels.put(id, value.get().strip()));
      final boolean center =
          this.design.factors().stream()
              .allMatch(f -> f.type() == Type.NUMERIC && f.coded(levels.get(f.id())).signum() == 0);
      try {
        final Run next = this.rowValue(row, center, levels);
        final Run previous =
            this.design.runs().stream()
                .filter(r -> r.id().equals(row.original.id()))
                .findFirst()
                .orElse(row.original);
        if (row.linkedExperiment == null) LaboratoryDesignResponses.validateEdit(previous, next);
        else LaboratoryDesignResponses.validateEdit(previous, next, row.linkedExperiment);
        runs.add(next);
      } catch (final IllegalArgumentException error) {
        throw new IllegalArgumentException(row.position.get() + ": " + error.getMessage(), error);
      }
    }
    return settings.withRuns(runs);
  }

  private static String originLabel(final Origin origin) {
    return origin == null || origin == Origin.UNSPECIFIED ? "Неизвестно" : origin.toString();
  }

  private static String responseOrigins(final LaboratoryExperimentalDesign value) {
    final Map<Origin, Long> counts = new LinkedHashMap<>();
    for (final Run run : value.runs())
      if (run.response() != null) counts.merge(run.responseOrigin(), 1L, Long::sum);
    final String text =
        counts.entrySet().stream()
            .map(e -> originLabel(e.getKey()) + ": " + e.getValue())
            .collect(java.util.stream.Collectors.joining(" · "));
    return "Происхождение откликов: "
        + text
        + (counts.keySet().stream().anyMatch(o -> o != Origin.MEASURED)
            ? ". В расчёте есть значения, не подтверждённые как измерения этого опыта."
            : "");
  }

  private Run rowValue(final Row row, final boolean center, final Map<String, String> levels) {
    return new Run(
        row.original.id(),
        row.original.standardOrder(),
        row.original.replicate(),
        center,
        levels,
        row.position.get(),
        row.original.experimentFile(),
        row.original.experimentId(),
        row.response.get().isBlank()
            ? null
            : LaboratoryExperimentalDesign.number(row.response.get()),
        row.note.get(),
        row.responseOrigin.get(),
        row.responseSource.get(),
        row.responseAnalysis.get());
  }

  private Run rowValue(final Row row) {
    final Map<String, String> levels = new LinkedHashMap<>();
    row.levels.forEach((id, v) -> levels.put(id, v.get().strip()));
    return this.rowValue(row, row.original.center(), levels);
  }

  private void configureResponseEditor() {
    this.responseEditor.setId("chemistry-design-response-editor");
    this.responseContext.setId("chemistry-design-response-context");
    this.responseOrigin.setId("chemistry-design-response-origin");
    this.responseOrigin.getItems().setAll(Origin.values());
    this.responseOrigin.setPrefWidth(210);
    this.responseOrigin.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(Origin v) {
            return originLabel(v);
          }

          public Origin fromString(String v) {
            return null;
          }
        });
    this.responseSource.setId("chemistry-design-response-source");
    this.responseSource.setPrefRowCount(2);
    this.responseSource.setWrapText(true);
    this.responseSource.setPromptText(
        "Метод и запись измерения, ссылка на литературу или основание модели");
    this.responseAnalyses.setId("chemistry-design-response-analysis");
    this.responseAnalyses.setMaxWidth(Double.MAX_VALUE);
    this.responseAnalyses.setPrefWidth(600);
    this.responseAnalyses.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(AnalysisChoice a) {
            return a == null
                ? ""
                : a.method()
                    + " · "
                    + a.title()
                    + " · "
                    + (a.historical() ? "предыдущая версия · " : "")
                    + a.recordedAt()
                    + " · "
                    + originLabel(Origin.valueOf(a.origin().name()));
          }

          public AnalysisChoice fromString(String v) {
            return null;
          }
        });
    this.responseAnalyses
        .valueProperty()
        .addListener(
            (o, a, v) ->
                this.responseAnalyses.setTooltip(
                    v == null
                        ? null
                        : new Tooltip(
                            v.reference().kind()
                                + " · "
                                + v.reference().id()
                                + "\n"
                                + v.source())));
    this.responseOrigin
        .valueProperty()
        .addListener(
            (o, a, v) -> {
              if (!this.responseLoading && this.responseRow != null && v != null)
                this.responseRow.responseOrigin.set(v);
            });
    this.responseSource
        .textProperty()
        .addListener(
            (o, a, v) -> {
              if (!this.responseLoading && this.responseRow != null)
                this.responseRow.responseSource.set(v);
            });
    this.responseHistory.selectedProperty().addListener((o, a, v) -> this.responseCandidates());
    final VBox links =
        new VBox(
            7,
            note(
                "Связь хранит выбранную версию анализа этого опыта. Число, происхождение и источник отклика вводятся явно."),
            this.responseLink,
            new FlowPane(
                8,
                8,
                this.button("Прочитать анализы опыта", "response-load", this::loadResponseAnalyses),
                this.responseHistory),
            this.responseAnalyses,
            new FlowPane(
                8,
                8,
                this.button("Связать выбранный", "response-link", this::linkResponseAnalysis),
                this.button("Просмотреть связанный", "response-show", this::showResponseAnalysis),
                this.button(
                    "Убрать связь",
                    "response-unlink",
                    () -> {
                      if (this.responseRow != null) {
                        this.responseRow.responseAnalysis.set(null);
                        this.responseLink();
                      }
                    })));
    this.responseEditor
        .getChildren()
        .setAll(
            this.responseContext,
            field("Происхождение отклика", this.responseOrigin),
            field("Источник", this.responseSource),
            details("Связанный анализ (необязательно)", links));
    this.responseEditor();
  }

  private void responseEditor() {
    this.responseLoading = true;
    this.responseRow = this.matrix.getSelectionModel().getSelectedItem();
    final Row row = this.responseRow;
    this.responseContext.setText(
        row == null ? "" : "Отклик выбранного опыта · " + row.position.get());
    this.responseOrigin.setValue(row == null ? Origin.UNSPECIFIED : row.responseOrigin.get());
    this.responseSource.setText(row == null ? "" : row.responseSource.get());
    this.responseCandidates();
    this.responseLink();
    this.responseLoading = false;
    this.resultColumns();
  }

  private void responseCandidates() {
    final Row row = this.responseRow;
    final AnalysisRef selected =
        this.responseAnalyses.getValue() == null
            ? null
            : this.responseAnalyses.getValue().reference();
    this.responseAnalyses.getItems().clear();
    if (row == null || row.linkedExperiment == null) return;
    this.responseAnalyses
        .getItems()
        .setAll(
            LaboratoryDesignResponses.analysisCandidates(
                row.original, row.linkedExperiment, this.responseHistory.isSelected()));
    this.responseAnalyses.setValue(
        this.responseAnalyses.getItems().stream()
            .filter(a -> Objects.equals(a.reference(), selected))
            .findFirst()
            .orElse(null));
  }

  private void responseLink() {
    final Row row = this.responseRow;
    final AnalysisRef ref = row == null ? null : row.responseAnalysis.get();
    if (ref == null) {
      this.responseLink.setText("Анализ не связан.");
      this.responseLink.setTooltip(null);
      return;
    }
    String text = "Сохранённая версия: " + ref.id();
    if (row.linkedExperiment != null) {
      try {
        final AnalysisChoice choice =
            LaboratoryReactionOperationsService.resolveAnalysis(
                row.linkedExperiment, row.responseAnalysis.get());
        text =
            choice.method()
                + " · "
                + choice.title()
                + (choice.historical() ? " · предыдущая версия" : "")
                + " · "
                + originLabel(Origin.valueOf(choice.origin().name()));
      } catch (final RuntimeException error) {
        text = "Связь требует проверки: " + error.getMessage();
      }
    }
    this.responseLink.setText(text);
    this.responseLink.setTooltip(new Tooltip(ref.kind() + " · " + ref.id()));
  }

  private void loadResponseAnalyses() {
    this.loadResponseAnalyses(false);
  }

  private void loadResponseAnalyses(final boolean show) {
    final Row row = this.responseRow;
    if (row == null || this.file == null || row.original.experimentFile().isBlank())
      throw new IllegalArgumentException(
          "Сначала создайте и сохраните опыты матрицы; анализ выбирается из соответствующего файла опыта.");
    final Path studyFile = this.file;
    this.job(
        "Читаю анализы выбранного опыта…",
        () ->
            new LaboratoryFileRepository()
                .read(this.repository.experiment(studyFile, row.original))
                .drawing()
                .experiment(),
        experiment -> {
          if (!experiment.id().equals(row.original.experimentId()))
            throw new IllegalArgumentException("Связанный файл содержит другой опыт.");
          row.linkedExperiment = experiment;
          this.responseCandidates();
          this.responseLink();
          this.status.setText(
              "Прочитаны сохранённые анализы выбранного опыта. Число отклика не изменено.");
          if (show) this.showResponseAnalysis();
        });
  }

  private void linkResponseAnalysis() {
    final Row row = this.responseRow;
    final AnalysisChoice choice = this.responseAnalyses.getValue();
    if (row == null || row.linkedExperiment == null || choice == null)
      throw new IllegalArgumentException("Прочитайте анализы опыта и выберите точную запись.");
    final Run linked =
        LaboratoryDesignResponses.withAnalysis(
            this.rowValue(row), row.linkedExperiment, choice.reference());
    row.responseAnalysis.set(linked.responseAnalysis());
    this.responseLink();
    this.status.setText(
        "Связана выбранная версия анализа. Значение отклика не переносилось; сохраните исследование.");
  }

  private void showResponseAnalysis() {
    final Row row = this.responseRow;
    if (row == null || row.responseAnalysis.get() == null)
      throw new IllegalArgumentException("У отклика нет связанного анализа.");
    if (row.linkedExperiment == null) {
      this.loadResponseAnalyses(true);
      return;
    }
    LaboratoryReactionOperationsService.resolveAnalysis(
        row.linkedExperiment, row.responseAnalysis.get());
    final Dialog<ButtonType> dialog = new Dialog<>();
    dialog.setTitle("Отклик · точная версия анализа");
    if (this.window() != null) dialog.initOwner(this.window());
    final ScrollPane content =
        scroll(
            new OperationsObservationPane(() -> row.linkedExperiment, () -> null, false, () -> {})
                .analysisView(row.responseAnalysis.get()));
    content.setPrefViewportWidth(700);
    content.setPrefViewportHeight(560);
    dialog.getDialogPane().setContent(content);
    dialog
        .getDialogPane()
        .getButtonTypes()
        .setAll(new ButtonType("Закрыть", ButtonBar.ButtonData.CANCEL_CLOSE));
    LaboratoryDialogStyle.apply(dialog, dialog.getOwner());
    dialog.showAndWait();
  }

  private void rows(final LaboratoryExperimentalDesign value) {
    this.changing = true;
    this.matrix.getItems().setAll(value.runs().stream().map(Row::new).toList());
    this.matrix.getColumns().clear();
    this.matrix.getColumns().add(this.editColumn("Позиция", r -> r.position, 110, false));
    for (final var f : value.factors()) {
      this.matrix
          .getColumns()
          .add(
              this.editColumn(
                  f.title() + (f.unit().isBlank() ? "" : ", " + f.unit()),
                  r -> r.levels.get(f.id()),
                  140,
                  true));
    }
    this.matrix
        .getColumns()
        .add(
            this.editColumn(
                value.responseName() + ", " + value.responseUnit(), r -> r.response, 150, false));
    final TableColumn<Row, String> originColumn = new TableColumn<>("Происхождение");
    originColumn.setPrefWidth(180);
    originColumn.setSortable(false);
    originColumn.setCellValueFactory(
        v ->
            javafx.beans.binding.Bindings.createStringBinding(
                () -> originLabel(v.getValue().responseOrigin.get()), v.getValue().responseOrigin));
    this.matrix.getColumns().add(originColumn);
    this.matrix.getColumns().add(this.editColumn("Примечание", r -> r.note, 210, false));
    for (final var row : this.matrix.getItems()) {
      final var properties = new ArrayList<>(row.levels.values());
      properties.add(row.position);
      properties.add(row.response);
      properties.add(row.note);
      properties.add(row.responseSource);
      for (final var property : properties) {
        property.addListener(
            (o, a, b) -> {
              this.changed();
              this.responseUnit.setDisable(
                  this.matrix.getItems().stream().anyMatch(r -> !r.response.get().isBlank()));
              if (!this.changing && (property == row.position || property == row.response)) {
                this.showPlate();
              }
              if (!this.changing
                  && (property == row.response
                      || property == row.responseSource
                      || property == row.note))
                this.status.setText(
                    "Отклик изменён. Проверьте происхождение и источник, затем сохраните исследование.");
            });
      }
      row.responseOrigin.addListener(
          (o, a, v) -> {
            this.changed();
            if (!this.changing) this.showPlate();
          });
      row.responseAnalysis.addListener((o, a, v) -> this.changed());
    }
    this.modelTerms.getChildren().clear();
    this.termChecks.clear();
    final var terms = new FlowPane(12, 8);
    for (final var term : LaboratoryDesignPlanning.terms(value, true)) {
      final var check = new CheckBox(term.label());
      check.setSelected(term.factors().size() == 1);
      this.termChecks.put(term, check);
      terms.getChildren().add(check);
    }
    this.modelTerms.getChildren().add(terms);
    final int count =
        Math.max(
            1,
            (value.runs().size() + value.plateRows() * value.plateColumns() - 1)
                / (value.plateRows() * value.plateColumns()));
    this.plateNumber
        .getItems()
        .setAll(java.util.stream.IntStream.rangeClosed(1, count).boxed().toList());
    this.plateNumber.getSelectionModel().selectFirst();
    this.matrix.setPrefHeight(
        Math.max(114, Math.min(434, 40 * this.matrix.getItems().size() + 34)));
    this.updateSummary();
    this.refreshFactors();
    this.factorEditor.setExpanded(false);
    this.resultInput.setSelected(
        value.runs().stream()
            .anyMatch(
                r ->
                    r.response() != null
                        || !r.responseNote().isBlank()
                        || r.responseOrigin() != Origin.UNSPECIFIED
                        || !r.responseSource().isBlank()
                        || r.responseAnalysis() != null));
    this.resultColumns();
    this.matrix.getSelectionModel().selectFirst();
    this.changing = false;
    this.showPlate();
    this.responseUnit.setDisable(value.runs().stream().anyMatch(r -> r.response() != null));
    this.lockSettings(value.runs().stream().anyMatch(r -> !r.experimentFile().isEmpty()));
  }

  private TableColumn<Row, String> editColumn(
      final String name,
      final Function<Row, StringProperty> value,
      final double width,
      final boolean lockPublished) {
    final var column = new TableColumn<Row, String>(name);
    column.setPrefWidth(width);
    column.setSortable(false);
    column.setCellValueFactory(cell -> value.apply(cell.getValue()));
    column.setCellFactory(
        c ->
            new TableCell<>() {
              private final TextField input = new TextField();
              private StringProperty bound;

              {
                this.input.setStyle("-fx-background-color: transparent; -fx-padding: 7 4;");
                this.input.setAccessibleText(name);
                this.input.setOnMousePressed(
                    e ->
                        LaboratoryDesignPage.this
                            .matrix
                            .getSelectionModel()
                            .select(this.getIndex()));
              }

              protected void updateItem(final String text, final boolean empty) {
                super.updateItem(text, empty);
                if (this.bound != null) {
                  this.input.textProperty().unbind();
                  this.input.textProperty().unbindBidirectional(this.bound);
                  this.bound = null;
                }
                if (empty
                    || this.getIndex() < 0
                    || this.getIndex() >= this.getTableView().getItems().size()) {
                  this.setGraphic(null);
                  return;
                }
                final var row = this.getTableView().getItems().get(this.getIndex());
                this.bound = value.apply(row);
                final boolean locked = lockPublished && !row.original.experimentFile().isEmpty();
                this.input.setEditable(!locked);
                if (locked) {
                  this.input.textProperty().bind(this.bound);
                } else {
                  this.input.setText(this.bound.get());
                  this.input.textProperty().bindBidirectional(this.bound);
                }
                this.input.setTooltip(
                    locked
                        ? new Tooltip(
                            "Условия созданного опыта доступны только для чтения. Для изменения создайте новый план.")
                        : null);
                this.setGraphic(this.input);
              }
            });
    return column;
  }

  private static <T> TableColumn<T, String> readColumn(
      final String name, final Function<T, String> value, final double width) {
    final var c = new TableColumn<T, String>(name);
    c.setPrefWidth(width);
    c.setCellValueFactory(v -> new SimpleStringProperty(value.apply(v.getValue())));
    return c;
  }

  private void preview() {
    this.preview.getChildren().clear();
    final int i = this.matrix.getSelectionModel().getSelectedIndex();
    if (i < 0) {
      return;
    }
    if (i >= this.prepared.size()) {
      this.preview
          .getChildren()
          .add(
              note(
                  "Нажмите «Рассчитать загрузки», чтобы увидеть количества для выбранного опыта."));
      return;
    }
    final var plan = this.prepared.get(i);
    for (final var c : plan.drawing().experiment().components()) {
      final Label value =
          note(
              c.molecule().displayName()
                  + " · "
                  + (c.planned() == null
                      ? "количество не задано"
                      : LaboratoryQuantityText.format(c.planned(), true)));
      if (c.planned() != null) value.setTooltip(new Tooltip("Точное значение: " + c.planned()));
      this.preview.getChildren().add(value);
    }
    plan.warnings().forEach(w -> this.preview.getChildren().add(note(w)));
  }

  private void showPlate() {
    final ScrollPane previous =
        this.plate.getChildren().isEmpty()
                || !(this.plate.getChildren().getFirst() instanceof ScrollPane pane)
            ? null
            : pane;
    final double horizontal = previous == null ? 0 : previous.getHvalue(),
        vertical = previous == null ? 0 : previous.getVvalue();
    this.plate.getChildren().clear();
    if (this.design == null || this.plateNumber.getValue() == null) {
      return;
    }
    final var grid = new GridPane();
    grid.setHgap(5);
    grid.setVgap(5);
    final int p = this.plateNumber.getValue();
    for (int row = 0; row < this.design.plateRows(); row++) {
      for (int col = 0; col < this.design.plateColumns(); col++) {
        final String pos =
            "P" + p + "-" + (char) ('A' + row) + String.format(Locale.ROOT, "%02d", col + 1);
        final var match =
            this.matrix.getItems().stream().filter(r -> r.position.get().equals(pos)).findFirst();
        final var cell = new Button(pos.substring(pos.indexOf('-') + 1));
        cell.setMinSize(50, 36);
        cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        cell.setDisable(match.isEmpty());
        if (match.isPresent()) {
          final var run = match.get();
          cell.setTooltip(
              new Tooltip(
                  run.original.id()
                      + (run.response.get().isBlank()
                          ? " · отклик не задан"
                          : " · "
                              + run.response.get()
                              + " "
                              + this.responseUnit.getText()
                              + " · "
                              + originLabel(run.responseOrigin.get()))));
          cell.setStyle(
              run.response.get().isBlank()
                  ? "-fx-background-color: #dfe8ef; -fx-text-fill: #233849;"
                  : "-fx-background-color: #b9dfd1; -fx-text-fill: #173d32;");
          cell.setOnAction(
              e -> {
                this.matrix.getSelectionModel().select(run);
                this.matrix.scrollTo(run);
              });
        }
        grid.add(cell, col + 1, row + 1);
      }
    }
    final ScrollPane view = new ScrollPane(grid);
    view.setHvalue(horizontal);
    view.setVvalue(vertical);
    this.plate.getChildren().add(view);
    this.plate
        .getChildren()
        .add(
            note(
                "Зелёные позиции имеют введённый отклик. Имена произвольных позиций можно изменить в таблице."));
  }

  private void openDispensing() {
    if (!this.replaceDispensing()) {
      return;
    }
    final var chooser = new FileChooser();
    chooser.setTitle("Открыть план раздачи");
    chooser
        .getExtensionFilters()
        .add(new FileChooser.ExtensionFilter("План раздачи QDISPENSE", "*.qdispense"));
    final var selected = chooser.showOpenDialog(this.getScene().getWindow());
    if (selected == null) {
      return;
    }
    this.openDispensing(selected.toPath());
  }

  private void openDispensing(final Path selected) {
    final String previous = this.status.getText(), loading = "Читаю план раздачи…";
    this.job(
        loading,
        () -> new LaboratoryDispensingRepository().read(selected),
        opened -> {
          if (Objects.equals(this.status.getText(), loading)) {
            this.status.setText(previous);
          }
          this.attachDispensing(
              LaboratoryDispensingPage.saved(this.window(), selected, opened, this::showStudy));
        });
  }

  private void dispensing() {
    if (!this.replaceDispensing()) {
      return;
    }
    final var value = this.current();
    if (!value.equals(this.preparedDesign) || this.prepared.isEmpty()) {
      throw new IllegalArgumentException("Сначала проверьте загрузки всей текущей матрицы.");
    }
    final Path directory =
        this.file != null
            ? this.file.toAbsolutePath().getParent()
            : this.parent.getText().isBlank() ? null : Path.of(this.parent.getText());
    this.attachDispensing(
        LaboratoryDispensingPage.create(
            this.window(),
            new LaboratoryDesignCodec.Document(this.source, value),
            this.prepared,
            directory,
            this::showStudy));
  }

  private javafx.stage.Window window() {
    return this.getScene() == null ? null : this.getScene().getWindow();
  }

  private boolean replaceDispensing() {
    if (this.dispensingPage != null && !this.dispensingPage.canClose()) {
      this.showDispensing();
      return false;
    }
    return true;
  }

  private void attachDispensing(final LaboratoryDispensingPage page) {
    this.clearDispensing();
    this.dispensingPage = page;
    this.pages.getChildren().add(page);
    this.resumeDispensing.setVisible(true);
    this.showDispensing();
  }

  private void showDispensing() {
    if (this.dispensingPage == null || this.running) return;
    this.studyView.setVisible(false);
    this.studyView.setManaged(false);
    this.dispensingPage.setManaged(true);
    this.dispensingPage.setVisible(true);
  }

  private void showStudy() {
    if (this.dispensingPage != null) {
      if (this.dispensingPage.running) return;
      this.dispensingPage.setVisible(false);
      this.dispensingPage.setManaged(false);
    }
    this.studyView.setManaged(true);
    this.studyView.setVisible(true);
    if (this.dispensingPage != null && this.dispensingPage.pending()) {
      if (!Objects.equals(this.status.getText(), this.dispensingHint))
        this.beforeDispensingHint = this.status.getText();
      this.dispensingHint =
          "Незавершённый ввод раздачи остаётся в черновике. Продолжить: «Смеси и раздача» → «Продолжить план раздачи».";
      this.status.setText(this.dispensingHint);
    } else if (this.dispensingHint != null
        && Objects.equals(this.status.getText(), this.dispensingHint)) {
      final String previous = Objects.toString(this.beforeDispensingHint, "");
      final String feedback =
          this.dispensingPage == null
              ? ""
              : Objects.toString(this.dispensingPage.status.getText(), "");
      this.status.setText(
          previous
              + (previous.isBlank() || feedback.isBlank() ? "" : "\n")
              + (feedback.isBlank() ? "" : "Раздача: " + feedback));
      this.dispensingHint = null;
      this.beforeDispensingHint = null;
    }
  }

  private void clearDispensing() {
    if (this.dispensingPage != null) {
      this.dispensingPage.dispose();
      this.pages.getChildren().remove(this.dispensingPage);
      this.dispensingPage = null;
    }
    this.resumeDispensing.setVisible(false);
    this.studyView.setManaged(true);
    this.studyView.setVisible(true);
  }

  private void review() {
    final var value = this.current();
    this.job(
        "Рассчитываю загрузки матрицы…",
        () -> LaboratoryDesignPlanning.prepare(this.source, value, this.cancelled::get),
        plans -> {
          this.prepared = plans;
          this.preparedDesign = value;
          this.ready(true);
          this.preview();
          this.previewPane.setExpanded(true);
          this.status.setText(
              "Проверено планов: "
                  + plans.size()
                  + ". Просмотрите состав выбранных запусков; результаты не назначены.");
        });
  }

  private void analyze() {
    final var value = this.current();
    final var selected =
        this.termChecks.entrySet().stream()
            .filter(e -> e.getValue().isSelected())
            .map(Map.Entry::getKey)
            .toList();
    final var fit = LaboratoryDesignPlanning.analyze(value, selected);
    this.chart.setVisible(true);
    this.chart.setManaged(true);
    this.effectTable.setVisible(true);
    this.effectTable.setManaged(true);
    this.effectDetails.setVisible(true);
    this.effectDetails.setManaged(true);
    this.analysisInfo.getChildren().clear();
    this.analysisInfo
        .getChildren()
        .add(
            note(
                "По введённым откликам: "
                    + fit.observations()
                    + ". Эффект показывает изменение «"
                    + value.responseName()
                    + "», а не подтверждает оптимальные условия."));
    this.analysisInfo.getChildren().add(note(responseOrigins(value)));
    final VBox statistics =
        new VBox(
            6,
            note(
                "Параметров: "
                    + fit.parameters()
                    + " · ранг: "
                    + fit.rank()
                    + " · остаточных степеней свободы: "
                    + fit.residualDf()));
    if (fit.residualRmse() != null) {
      statistics
          .getChildren()
          .add(note("Остаточная RMSE: " + number(fit.residualRmse()) + " " + value.responseUnit()));
    }
    if (fit.pureErrorSd() != null) {
      statistics
          .getChildren()
          .add(
              note(
                  "Разброс повторов: "
                      + number(fit.pureErrorSd())
                      + " "
                      + value.responseUnit()
                      + " · df = "
                      + fit.pureErrorDf()));
    }
    fit.diagnostics().forEach(message -> statistics.getChildren().add(note(message)));
    this.analysisInfo.getChildren().add(details("Статистика и ограничения оценки", statistics));
    this.effectTable.getItems().setAll(fit.effects());
    this.effectTable.setPrefHeight(Math.min(250, 40 * fit.effects().size() + 40));
    this.chart.getData().clear();
    final var series = new XYChart.Series<Number, String>();
    fit.effects().stream()
        .filter(e -> !e.id().equals("intercept"))
        .sorted(Comparator.comparingDouble(e -> -Math.abs(e.effect())))
        .forEach(e -> series.getData().add(new XYChart.Data<>(e.effect(), e.label())));
    this.chart.getData().add(series);
    this.effectAxis.setLabel("Эффект, " + value.responseUnit());
    this.showPlate();
  }

  private void publish() {
    if (!this.replaceDispensing()) {
      return;
    }
    if (this.conflict) {
      throw new IllegalArgumentException(
          "Есть конфликт сохранения. Откройте актуальный файл перед созданием опытов.");
    }
    final var value = this.current();
    if (!value.equals(this.preparedDesign) || this.prepared.isEmpty()) {
      throw new IllegalArgumentException("Сначала проверьте загрузки всей текущей матрицы.");
    }
    final Path destination = this.parentPath();
    final String name = this.folder.getText();
    final var doc = new LaboratoryDesignCodec.Document(this.source, value);
    final var plans = this.prepared;
    this.job(
        "Создаю независимые опыты…",
        () ->
            this.repository.publish(
                destination,
                name,
                doc,
                plans,
                this.cancelled::get,
                message ->
                    Platform.runLater(
                        () -> {
                          if (!this.disposed) {
                            this.status.setText(message);
                          }
                        })),
        result -> {
          this.file = result.designFile();
          this.fingerprint = result.opened().fingerprint();
          this.install(result.opened().document());
          this.dirty = false;
          this.status.setText(
              "Создано опытов: "
                  + result.experiments().size()
                  + ". Исследование сохранено: "
                  + this.file);
          this.tabs.getSelectionModel().select(1);
        });
  }

  private void save() {
    if (this.conflict) {
      throw new IllegalArgumentException(
          "Файл изменён извне. Откройте актуальную версию или используйте новый путь для копии.");
    }
    final var doc = new LaboratoryDesignCodec.Document(this.source, this.current());
    final Path target;
    if (this.file == null) {
      final var chooser = chooser("Сохранить исследование");
      chooser.setInitialFileName("study.qdesign");
      final var selected = chooser.showSaveDialog(this.getScene().getWindow());
      if (selected == null) {
        return;
      }
      target = selected.toPath();
    } else {
      target = this.file;
    }
    final String expected = this.file == null ? null : this.fingerprint;
    this.job(
        "Сохраняю исследование…",
        () -> this.repository.write(target, doc, expected),
        hash -> {
          this.file = target;
          this.fingerprint = hash;
          this.design = doc.design();
          this.dirty = false;
          this.status.setText("Сохранено: " + target);
        });
  }

  private void load() {
    if (!this.replaceDispensing()) {
      return;
    }
    if (this.pending() && !this.conflict) {
      throw new IllegalArgumentException("Сначала сохраните текущие изменения исследования.");
    }
    final var chooser = chooser("Открыть исследование");
    final var selected = chooser.showOpenDialog(this.getScene().getWindow());
    if (selected == null) {
      return;
    }
    this.job(
        "Открываю исследование…",
        () -> this.repository.read(selected.toPath()),
        opened -> {
          this.file = selected.toPath();
          this.fingerprint = opened.fingerprint();
          this.install(opened.document());
          this.conflict = false;
          this.dirty = false;
          this.status.setText("Исследование открыто: " + this.file);
        });
  }

  private void newStudy() {
    if (!this.replaceDispensing()) {
      return;
    }
    if (this.pending()) {
      throw new IllegalArgumentException(
          "Сохраните текущий план перед созданием независимой копии.");
    }
    this.clearDispensing();
    this.designId = "design_" + UUID.randomUUID();
    this.file = null;
    this.fingerprint = null;
    this.design = null;
    this.matrix.getItems().clear();
    this.updateSummary();
    this.resultInput.setSelected(false);
    this.prepared = List.of();
    this.preparedDesign = null;
    this.responseUnit.setDisable(false);
    this.lockSettings(false);
    this.changed();
    this.tabs.getSelectionModel().select(0);
    this.status.setText(
        "Новый независимый план. Факторы сохранены; матрица и отклики не копировались.");
  }

  private void history() {
    if (!this.replaceDispensing()) {
      return;
    }
    if (this.file == null || this.pending()) {
      throw new IllegalArgumentException("Сохраните текущий план перед открытием истории.");
    }
    final Path directory = this.file.resolveSibling(this.file.getFileName() + ".history");
    try {
      if (!java.nio.file.Files.isDirectory(directory)) {
        throw new IllegalArgumentException("У исследования ещё нет предыдущих сохранённых версий.");
      }
      final List<Path> versions;
      try (final var stream = java.nio.file.Files.list(directory)) {
        versions =
            stream
                .filter(p -> p.getFileName().toString().matches("[0-9a-f]{64}\\.qdesign"))
                .sorted(
                    Comparator.comparingLong(
                            (Path p) -> {
                              try {
                                return java.nio.file.Files.getLastModifiedTime(p).toMillis();
                              } catch (final java.io.IOException ignored) {
                                return 0;
                              }
                            })
                        .reversed())
                .limit(200)
                .toList();
      }
      if (versions.isEmpty()) {
        throw new IllegalArgumentException("Сохранённых версий пока нет.");
      }
      final var labels = new LinkedHashMap<String, Path>();
      for (final var version : versions) {
        labels.put(
            java.nio.file.Files.getLastModifiedTime(version).toString()
                + " · "
                + version.getFileName().toString().substring(0, 12),
            version);
      }
      final var dialog = new ChoiceDialog<>(labels.keySet().iterator().next(), labels.keySet());
      dialog.setTitle("История исследования");
      dialog.setHeaderText("Открыть предыдущую версию как черновик");
      dialog.setContentText("Версия");
      dialog.initOwner(this.getScene().getWindow());
      LaboratoryDialogStyle.apply(dialog, this.getScene().getWindow());
      final var selected = dialog.showAndWait();
      if (selected.isEmpty()) {
        return;
      }
      this.job(
          "Открываю версию…",
          () -> this.repository.read(labels.get(selected.get())),
          opened -> {
            this.install(opened.document());
            this.dirty = true;
            this.status.setText(
                "Предыдущая версия открыта как черновик. Текущий файл изменится только после сохранения.");
          });
    } catch (final java.io.IOException exception) {
      throw new IllegalArgumentException(exception.getMessage(), exception);
    }
  }

  private void resetDraft() {
    if (!this.replaceDispensing()) {
      return;
    }
    this.clearDispensing();
    if (this.file != null) {
      this.job(
          "Открываю сохранённую версию…",
          () -> this.repository.read(this.file),
          opened -> {
            this.install(opened.document());
            this.fingerprint = opened.fingerprint();
            this.dirty = false;
            this.conflict = false;
            this.status.setText("Открыта сохранённая версия исследования.");
          });
      return;
    }
    this.changing = true;
    this.design = null;
    this.designId = "design_" + UUID.randomUUID();
    this.matrix.getItems().clear();
    this.matrix.setPrefHeight(114);
    this.ready(false);
    this.clearAnalysis();
    this.tabs.getSelectionModel().select(0);
    this.factors.getItems().clear();
    this.refreshFactors();
    this.factorEditor.setExpanded(true);
    this.resultInput.setSelected(false);
    this.updateSummary();
    this.prepared = List.of();
    this.preparedDesign = null;
    this.configureStages();
    this.responseUnit.setDisable(false);
    this.lockSettings(false);
    this.changing = false;
    this.dirty = false;
    this.conflict = false;
    this.status.setText("Несохранённый черновик сброшен; файлы не изменены.");
  }

  private void lockSettings(final boolean value) {
    for (final Control control :
        List.<Control>of(
            this.stage,
            this.reference,
            this.base,
            this.baseUnit,
            this.seed,
            this.solvents,
            this.randomized,
            this.replicates,
            this.centers,
            this.plateRows,
            this.plateColumns)) {
      control.setDisable(value);
    }
  }

  private void openRun() {
    if (!this.replaceDispensing()) {
      return;
    }
    final var row = this.matrix.getSelectionModel().getSelectedItem();
    if (row == null || this.file == null) {
      throw new IllegalArgumentException("Выберите запуск с созданным опытом.");
    }
    if (this.pending()) {
      throw new IllegalArgumentException("Сохраните введённые результаты перед переходом в опыт.");
    }
    try {
      this.open.accept(this.repository.experiment(this.file, row.original));
    } catch (final Exception exception) {
      this.status.setText(exception.getMessage());
    }
  }

  private void install(final LaboratoryDesignCodec.Document doc) {
    if (!this.replaceDispensing()) {
      throw new IllegalArgumentException(
          "Сохраните или сбросьте ввод раздачи перед заменой исследования.");
    }
    this.clearDispensing();
    this.changing = true;
    this.source = doc.source();
    this.design = doc.design();
    this.designId = this.design.id();
    this.configureStages();
    select(this.stage, this.design.stageId());
    this.configureReference();
    select(this.reference, this.design.referenceId());
    this.title.setText(this.design.title());
    this.base.display(this.design.baseQuantity().value());
    this.baseUnit.setValue(this.design.baseQuantity().unit());
    this.seed.setText(Long.toString(this.design.seed()));
    this.responseName.setText(this.design.responseName());
    this.responseUnit.setText(this.design.responseUnit());
    this.solvents.setSelected(this.design.scaleSolvents());
    this.plateRows.getValueFactory().setValue(this.design.plateRows());
    this.plateColumns.getValueFactory().setValue(this.design.plateColumns());
    this.factors.getItems().setAll(this.design.factors());
    this.rows(this.design);
    this.prepared = List.of();
    this.preparedDesign = null;
    this.ready(false);
    this.clearAnalysis();
    this.tabs.getSelectionModel().select(1);
    this.changing = false;
  }

  private Path parentPath() {
    if (this.parent.getText().isBlank()) this.destination.setExpanded(true);
    final Path result = Path.of(this.parent.getText().strip());
    if (!result.isAbsolute()) {
      throw new IllegalArgumentException("Укажите полный путь к папке проекта.");
    }
    return result;
  }

  private static FileChooser chooser(final String title) {
    final var chooser = new FileChooser();
    chooser.setTitle(title);
    chooser
        .getExtensionFilters()
        .add(new FileChooser.ExtensionFilter("План эксперимента QCHEM", "*.qdesign"));
    return chooser;
  }

  private static void select(final ComboBox<Choice> box, final String id) {
    box.setValue(box.getItems().stream().filter(c -> c.id().equals(id)).findFirst().orElse(null));
  }

  private static String number(final double value) {
    return String.format(Locale.ROOT, "%.5g", value);
  }

  @FunctionalInterface
  private interface Work<T> {
    T run() throws Exception;
  }

  private <T> void job(final String text, final Work<T> work, final Consumer<T> done) {
    this.cancelled.set(false);
    this.busy(true);
    this.tabs.setDisable(true);
    this.create.setDisable(true);
    this.save.setDisable(true);
    final boolean cancellable = text.startsWith("Рассчитываю") || text.startsWith("Создаю");
    this.stop.setVisible(cancellable);
    this.stop.setManaged(cancellable);
    this.status.setText(text);
    final var thread =
        new Thread(
            () -> {
              try {
                final T result = work.run();
                Platform.runLater(
                    () -> {
                      if (!this.disposed) {
                        this.idle();
                        done.accept(result);
                      }
                    });
              } catch (final Exception exception) {
                Platform.runLater(
                    () -> {
                      if (!this.disposed) {
                        this.idle();
                        if (Objects.toString(exception.getMessage(), "").contains("изменён")
                            || Objects.toString(exception.getMessage(), "").contains("изменился")) {
                          this.conflict = true;
                        }
                        this.status.setText(exception.getMessage());
                      }
                    });
              }
            },
            "laboratory-design-work");
    thread.setDaemon(true);
    thread.start();
  }

  private void idle() {
    this.busy(false);
    this.tabs.setDisable(false);
    this.create.setDisable(false);
    this.save.setDisable(false);
    this.stop.setVisible(false);
    this.stop.setManaged(false);
  }

  boolean pending() {
    return this.dirty
        || this.conflict
        || this.dispensingPage != null && this.dispensingPage.pending();
  }

  @Override
  boolean canClose() {
    if (!super.canClose()) {
      return false;
    }
    if (!this.replaceDispensing()) {
      return false;
    }
    if (this.pending()) {
      this.showStudy();
      this.status.setText("Есть несохранённые изменения исследования. Сохраните их перед выходом.");
      return false;
    }
    return true;
  }

  @Override
  void dispose() {
    this.cancelled.set(true);
    if (this.dispensingPage != null) this.dispensingPage.dispose();
    super.dispose();
  }

  void draft(final Properties properties, final boolean restore) {
    if (!restore) {
      properties.keySet().removeIf(key -> key.toString().startsWith("design.dispensing."));
      if (this.dispensingPage != null) {
        this.dispensingPage.draft(properties, false);
        properties.setProperty(
            "design.dispensing.visible", Boolean.toString(this.dispensingPage.isVisible()));
      }
      properties.setProperty(
          "design.resultsVisible", Boolean.toString(this.resultInput.isSelected()));
      properties.setProperty(
          "design.factorEditor", Boolean.toString(this.factorEditor.isExpanded()));
      properties.setProperty(
          "design.quantitiesVisible", Boolean.toString(this.previewPane.isExpanded()));
      properties.setProperty(
          "design.tab", Integer.toString(this.tabs.getSelectionModel().getSelectedIndex()));
      properties.setProperty("design.destination", Boolean.toString(this.destination.isExpanded()));
      properties.setProperty("design.parent", this.parent.getText());
      properties.setProperty("design.folder", this.folder.getText());
      properties.setProperty("design.file", this.file == null ? "" : this.file.toString());
      properties.setProperty("design.fingerprint", Objects.toString(this.fingerprint, ""));
      properties.setProperty("design.conflict", Boolean.toString(this.conflict));
      properties.setProperty("design.dirty", Boolean.toString(this.dirty));
      properties.setProperty("design.id", this.designId);
      properties.setProperty(
          "design.stage", this.stage.getValue() == null ? "" : this.stage.getValue().id());
      properties.setProperty(
          "design.reference",
          this.reference.getValue() == null ? "" : this.reference.getValue().id());
      properties.setProperty(
          "design.factorComponent",
          this.factorComponent.getValue() == null ? "" : this.factorComponent.getValue().id());
      properties.setProperty("design.replicates", this.replicates.getValue().toString());
      properties.setProperty("design.centers", this.centers.getValue().toString());
      properties.setProperty("design.plateRows", this.plateRows.getValue().toString());
      properties.setProperty("design.plateColumns", this.plateColumns.getValue().toString());
      properties.setProperty(
          "design.factorCount", Integer.toString(this.factors.getItems().size()));
      for (int i = 0; i < this.factors.getItems().size(); i++) {
        final var f = this.factors.getItems().get(i);
        final String p = "design.factor." + i + ".";
        properties.setProperty(p + "id", f.id());
        properties.setProperty(p + "title", f.title());
        properties.setProperty(p + "type", f.type().name());
        properties.setProperty(p + "binding", f.binding().name());
        properties.setProperty(p + "component", Objects.toString(f.componentId(), ""));
        properties.setProperty(p + "unit", f.unit());
        properties.setProperty(p + "low", f.low());
        properties.setProperty(p + "high", f.high());
      }
      if (this.design != null) {
        try {
          properties.setProperty(
              "design.document",
              new LaboratoryDesignCodec()
                  .write(new LaboratoryDesignCodec.Document(this.source, this.current())));
        } catch (final RuntimeException ignored) {
          properties.setProperty(
              "design.document",
              new LaboratoryDesignCodec()
                  .write(new LaboratoryDesignCodec.Document(this.source, this.design)));
        }
        properties.remove("design.source");
      } else {
        properties.remove("design.document");
        properties.setProperty("design.source", new LaboratoryFileCodec().write(this.source));
      }
      final Map<String, Control> fields = this.draftFields();
      LaboratoryFormDraft.fields(properties, "design.input", false, fields);
      properties.setProperty("design.rawRuns", Integer.toString(this.matrix.getItems().size()));
      for (int i = 0; i < this.matrix.getItems().size(); i++) {
        final var r = this.matrix.getItems().get(i);
        properties.setProperty("design.raw." + i + ".position", r.position.get());
        properties.setProperty("design.raw." + i + ".response", r.response.get());
        properties.setProperty("design.raw." + i + ".note", r.note.get());
        properties.setProperty("design.raw." + i + ".origin", r.responseOrigin.get().name());
        properties.setProperty("design.raw." + i + ".source", r.responseSource.get());
        properties.setProperty(
            "design.raw." + i + ".analysisKind",
            r.responseAnalysis.get() == null ? "" : r.responseAnalysis.get().kind().name());
        properties.setProperty(
            "design.raw." + i + ".analysisId",
            r.responseAnalysis.get() == null ? "" : r.responseAnalysis.get().id());
        for (final var e : r.levels.entrySet()) {
          properties.setProperty("design.raw." + i + "." + e.getKey(), e.getValue().get());
        }
      }
      return;
    }
    this.changing = true;
    final String document = properties.getProperty("design.document");
    if (document != null) {
      this.install(new LaboratoryDesignCodec().read(document));
    } else if (properties.containsKey("design.source")) {
      this.source = new LaboratoryFileCodec().read(properties.getProperty("design.source"));
      this.design = null;
      this.matrix.getItems().clear();
      this.prepared = List.of();
      this.preparedDesign = null;
      this.configureStages();
      this.responseUnit.setDisable(false);
      this.lockSettings(false);
    }
    this.parent.setText(properties.getProperty("design.parent", ""));
    this.folder.setText(properties.getProperty("design.folder", ""));
    final String saved = properties.getProperty("design.file", "");
    this.file = saved.isEmpty() ? null : Path.of(saved);
    this.fingerprint = properties.getProperty("design.fingerprint", "");
    if (this.fingerprint.isEmpty()) {
      this.fingerprint = null;
    }
    this.changing = true;
    this.designId = properties.getProperty("design.id", this.designId);
    select(this.stage, properties.getProperty("design.stage", ""));
    this.configureReference();
    select(this.reference, properties.getProperty("design.reference", ""));
    select(this.factorComponent, properties.getProperty("design.factorComponent", ""));
    this.replicates
        .getValueFactory()
        .setValue(Integer.parseInt(properties.getProperty("design.replicates", "2")));
    this.centers
        .getValueFactory()
        .setValue(Integer.parseInt(properties.getProperty("design.centers", "0")));
    this.plateRows
        .getValueFactory()
        .setValue(Integer.parseInt(properties.getProperty("design.plateRows", "8")));
    this.plateColumns
        .getValueFactory()
        .setValue(Integer.parseInt(properties.getProperty("design.plateColumns", "12")));
    final int factorCount = Integer.parseInt(properties.getProperty("design.factorCount", "0"));
    if (factorCount < 0 || factorCount > 6) {
      throw new IllegalArgumentException("Неверное число факторов в черновике.");
    }
    final var factors = new ArrayList<Factor>();
    for (int i = 0; i < factorCount; i++) {
      final String p = "design.factor." + i + ".";
      final String component = properties.getProperty(p + "component", "");
      factors.add(
          new Factor(
              properties.getProperty(p + "id"),
              properties.getProperty(p + "title"),
              Type.valueOf(properties.getProperty(p + "type")),
              Binding.valueOf(properties.getProperty(p + "binding")),
              component.isEmpty() ? null : component,
              properties.getProperty(p + "unit"),
              properties.getProperty(p + "low"),
              properties.getProperty(p + "high")));
    }
    this.factors.getItems().setAll(factors);
    LaboratoryFormDraft.fields(properties, "design.input", true, this.draftFields());
    final int count = Integer.parseInt(properties.getProperty("design.rawRuns", "0"));
    if (count != this.matrix.getItems().size()) {
      throw new IllegalArgumentException(
          "Черновик матрицы не соответствует сохранённому исследованию.");
    }
    for (int i = 0; i < count; i++) {
      final var r = this.matrix.getItems().get(i);
      r.position.set(properties.getProperty("design.raw." + i + ".position", r.position.get()));
      r.response.set(properties.getProperty("design.raw." + i + ".response", r.response.get()));
      r.note.set(properties.getProperty("design.raw." + i + ".note", r.note.get()));
      r.responseOrigin.set(
          Origin.valueOf(
              properties.getProperty(
                  "design.raw." + i + ".origin", r.responseOrigin.get().name())));
      r.responseSource.set(
          properties.getProperty("design.raw." + i + ".source", r.responseSource.get()));
      final String analysisId =
          properties.getProperty(
              "design.raw." + i + ".analysisId",
              r.responseAnalysis.get() == null ? "" : r.responseAnalysis.get().id());
      r.responseAnalysis.set(
          analysisId.isBlank()
              ? null
              : new AnalysisRef(
                  AnalysisKind.valueOf(
                      properties.getProperty(
                          "design.raw." + i + ".analysisKind",
                          r.responseAnalysis.get() == null
                              ? ""
                              : r.responseAnalysis.get().kind().name())),
                  analysisId));
      for (final var e : r.levels.entrySet()) {
        e.getValue()
            .set(properties.getProperty("design.raw." + i + "." + e.getKey(), e.getValue().get()));
      }
    }
    this.conflict = Boolean.parseBoolean(properties.getProperty("design.conflict"));
    this.dirty = Boolean.parseBoolean(properties.getProperty("design.dirty"));
    this.changing = false;
    this.ready(false);
    this.clearAnalysis();
    this.tabs
        .getSelectionModel()
        .select(
            Math.max(
                0,
                Math.min(
                    2,
                    Integer.parseInt(
                        properties.getProperty("design.tab", this.design == null ? "0" : "1")))));
    this.destination.setExpanded(
        Boolean.parseBoolean(
            properties.getProperty(
                "design.destination", Boolean.toString(this.parent.getText().isBlank()))));
    this.refreshFactors();
    this.updateSummary();
    this.resultInput.setSelected(
        Boolean.parseBoolean(
            properties.getProperty(
                "design.resultsVisible",
                Boolean.toString(
                    this.matrix.getItems().stream()
                        .anyMatch(r -> !r.response.get().isBlank() || !r.note.get().isBlank())))));
    this.resultColumns();
    this.responseEditor();
    this.factorEditor.setExpanded(
        Boolean.parseBoolean(
            properties.getProperty(
                "design.factorEditor", Boolean.toString(this.factors.getItems().isEmpty()))));
    this.previewPane.setExpanded(
        Boolean.parseBoolean(properties.getProperty("design.quantitiesVisible", "false")));
    if (this.file != null) {
      try {
        if (!Objects.equals(this.fingerprint, this.repository.read(this.file).fingerprint())) {
          this.conflict = true;
        }
      } catch (final Exception exception) {
        this.conflict = true;
      }
    }
    if (properties.containsKey("design.dispensing.document")) {
      this.attachDispensing(
          LaboratoryDispensingPage.recovered(properties, this.window(), this::showStudy));
      if (!Boolean.parseBoolean(properties.getProperty("design.dispensing.visible", "true")))
        this.showStudy();
    }
    if (this.conflict) {
      this.status.setText(
          "Черновик восстановлен, но файл исследования изменён или недоступен. Сбросьте к актуальному сохранению либо откройте другой файл; перезапись заблокирована.");
    }
  }

  private Map<String, Control> draftFields() {
    final Map<String, Control> fields = new LinkedHashMap<>();
    fields.put("title", this.title);
    fields.put("base", this.base);
    fields.put("baseUnit", this.baseUnit);
    fields.put("seed", this.seed);
    fields.put("responseName", this.responseName);
    fields.put("responseUnit", this.responseUnit);
    fields.put("factorTitle", this.factorTitle);
    fields.put("factorLow", this.factorLow);
    fields.put("factorHigh", this.factorHigh);
    fields.put("factorUnit", this.factorUnit);
    fields.put("factorType", this.factorType);
    fields.put("factorBinding", this.factorBinding);
    fields.put("randomized", this.randomized);
    fields.put("solvents", this.solvents);
    return fields;
  }
}

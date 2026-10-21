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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAnalyticalEvidence.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryFileCodec;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryNmr2DReader;
import ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy.NmrSpectrum2D;
import ru.pathcreator.vadim.quantum.chemistry.storage.infrastructure.text.QchemNmrSpectrum2DCodec;

/** Manual scientific workflows share typed evidence, explicit inputs and append-only corrections. */
final class LaboratoryEvidencePage extends LaboratoryComputationPage {

  private enum Method {
    CORRELATIONS("Корреляции 2D"),
    MULTIPLETS("Мультиплеты и J"),
    QNMR("Количественный ЯМР"),
    CHROM("Хроматография"),
    DOSSIER("Досье продукта");
    private final String label;

    Method(String label) {
      this.label = label;
    }

    public String toString() {
      return this.label;
    }
  }

  private record Choice(String id, String title) {
    public String toString() {
      return this.title;
    }
  }

  private final LaboratoryEditor editor;
  private final Window owner;
  private LaboratoryDrawing expected;
  private Molecule snapshot;
  private final TabPane tabs = new TabPane();
  private final Tab inputTab = new Tab("Данные"),
      previewTab = new Tab("Результат"),
      historyTab = new Tab("История");
  private final ComboBox<Choice> component = new ComboBox<>(), sample = new ComboBox<>();
  private final ComboBox<Method> method = new ComboBox<>();
  private final ComboBox<LaboratoryAnalysis.Origin> origin = new ComboBox<>();
  private final ComboBox<Experiment2D> experiment2D = new ComboBox<>();
  private final ComboBox<LaboratoryAnalysis.Nucleus> xNucleus = new ComboBox<>(),
      yNucleus = new ComboBox<>();
  private final ComboBox<Conclusion> conclusion = new ComboBox<>();
  private final Map<String, TextField> fields = new LinkedHashMap<>();
  private final Map<String, ComboBox<LaboratoryQuantityUnit>> units = new LinkedHashMap<>();
  private final TextArea source = new TextArea(),
      note = new TextArea(),
      preparation = new TextArea(),
      conclusionText = new TextArea(),
      result = new TextArea();
  private final VBox form = new VBox(12),
      specific = new VBox(12),
      review = new VBox(10),
      references = new VBox(6),
      attachmentBox = new VBox(5);
  private final Map<String, CheckBox> analysisLinks = new LinkedHashMap<>(),
      evidenceLinks = new LinkedHashMap<>(),
      attachments = new LinkedHashMap<>();
  private final LaboratoryEvidenceTable correlations, multiplets, peaks, calibration, findings;
  private List<ChromPoint> trace = List.of();
  private final LaboratoryStructurePreview structure = new LaboratoryStructurePreview();
  private final LaboratoryEvidencePlot plot = new LaboratoryEvidencePlot(),
      assignmentPlot = new LaboratoryEvidencePlot();
  private final Label assignmentFeedback = note("");
  private final FlowPane graphTools = new FlowPane(8, 8);
  private LaboratorySpectralComparisonPane comparison;
  private final ComboBox<String> assignmentAxis = new ComboBox<>();
  private final ListView<Entry> history = new ListView<>();
  private final CheckBox allHistory = new CheckBox("Включая предыдущие исправления");
  private final Button apply, check;
  private final Label feedback = note(""), traceInfo = note("Числовая хроматограмма не загружена");
  private final VBox emptyResult = new VBox(10),
      resultCard = new VBox(10),
      dossierCard = new VBox(10);
  private final Label resultHint = note("");
  private final TitledPane resultDetails = new TitledPane("Результаты и сведения", this.result);
  private final Button reviewApply;
  private final ScrollPane formScroll;
  private final LaboratoryEvidencePlot tracePlot = new LaboratoryEvidencePlot();
  private final VBox traceView = new VBox(6);
  private final ToggleButton selectRange = new ToggleButton("Выделить область");
  private final Label traceHelp = note(""),
      identityContext = note(""),
      emptyPeaks = note("Пиков пока нет. Выделите область трассы или добавьте площадь вручную.");
  private List<ChromPoint> displayedTrace = List.of();
  private NmrSpectrum2D spectrum2D;
  private String multipletAnalysisId;
  private final LaboratoryNmr2DPane spectrumView = new LaboratoryNmr2DPane(false);
  private final LaboratoryMultipletTracePane multipletView = new LaboratoryMultipletTracePane(),
      multipletPreview = new LaboratoryMultipletTracePane();
  private final Label spectrumInfo = note("Спектр ещё не загружен"),
      multipletSourceInfo = note("Можно связать сохранённый 1D спектр и выбрать линии на нём.");
  private final Button cancelSpectrumImport = new Button("Остановить загрузку");
  private Thread spectrumLoading;
  private volatile boolean spectrumCancelRequested;
  private TitledPane notesPane, integrationPane, calibrationPane, purityPane, findingsPane;
  private boolean changing, dirty, applied, conflict;
  private String draftId = "evidence_" + UUID.randomUUID(), correctsId;
  private Entry prepared;

  LaboratoryEvidencePage(
      final LaboratoryEditor editor,
      final String selected,
      final Window owner,
      final Runnable close) {
    super("Аналитика", close);
    this.editor = editor;
    this.owner = owner;
    this.expected = editor.document();
    this.setId("chemistry-evidence-page");
    this.correlations =
        table(
            "correlations",
            List.of("x_ppm", "y_ppm", "intensity", "x_atoms", "y_atoms", "note"),
            List.of(
                "δ ¹H, ppm",
                "δ второй оси",
                "Интенсивность",
                "Атомы оси X",
                "Атомы оси Y",
                "Заметка"));
    this.multiplets =
        table(
            "multiplets",
            List.of("center_ppm", "integral", "multiplicity", "lines_ppm", "j_hz", "atoms", "note"),
            List.of(
                "δ, ppm",
                "Интеграл",
                "Мультиплет",
                "Линии ppm · ;",
                "J, Гц · ;",
                "Назначения",
                "Заметка"));
    for (final int column : new int[] {0, 1, 2}) this.correlations.numericColumn(column, 6, false);
    for (final int column : new int[] {0, 1}) this.multiplets.numericColumn(column, 6, false);
    for (final int column : new int[] {3, 4}) this.multiplets.numericColumn(column, 6, true);
    this.peaks =
        table(
            "peaks",
            List.of(
                "label",
                "retention_min",
                "from_min",
                "to_min",
                "area",
                "included",
                "exclusion_reason"),
            List.of(
                "Пик",
                "tR, мин",
                "Начало, мин",
                "Конец, мин",
                "Площадь",
                "В сумму · да/нет",
                "Причина исключения"));
    this.calibration =
        table("calibration", List.of("mass", "area"), List.of("Масса стандарта", "Площадь"));
    this.findings =
        table(
            "findings",
            List.of("category", "severity", "finding", "resolved"),
            List.of("Категория", "Уровень", "Наблюдение / расхождение", "Разрешено · да/нет"));
    this.findings.choiceColumn(
        1,
        Arrays.stream(Severity.values()).map(Severity::name).toList(),
        new javafx.util.StringConverter<>() {
          public String toString(String value) {
            if (value == null || value.isBlank()) return "Выберите уровень";
            try {
              return LaboratoryEvidenceService.severityLabel(
                  LaboratoryEvidenceService.severity(value));
            } catch (IllegalArgumentException invalid) {
              return "Проверьте уровень";
            }
          }

          public String fromString(String value) {
            return LaboratoryEvidenceService.severity(value).name();
          }
        },
        Severity.INFORMATION.name());
    this.correlations.displayColumn(3, this::atomLabels);
    this.correlations.displayColumn(4, this::atomLabels);
    this.multiplets.displayColumn(5, this::atomLabels);
    this.correlations
        .getSelectionModel()
        .selectedIndexProperty()
        .addListener(o -> this.highlightRow());
    this.multiplets
        .getSelectionModel()
        .selectedIndexProperty()
        .addListener(o -> this.highlightRow());
    for (final String key :
        List.of(
            "title",
            "solvent",
            "frequency",
            "standard",
            "sampleIntegral",
            "sampleNuclei",
            "sampleMolarMass",
            "sampleMass",
            "standardIntegral",
            "standardNuclei",
            "standardMolarMass",
            "standardMass",
            "standardPurity",
            "technique",
            "detector",
            "quantifiedPeak",
            "representedMass",
            "reportedPurity",
            "puritySource",
            "calibrationSource",
            "from",
            "to",
            "peakLabel",
            "tolerance")) {
      final var field = new TextField();
      field.setId("evidence-" + key);
      field.textProperty().addListener(o -> this.changed());
      this.fields.put(key, field);
    }
    for (final String key :
        List.of("sampleMass", "standardMass", "calibration", "representedMass")) {
      final var unit = new ComboBox<LaboratoryQuantityUnit>();
      unit.getItems()
          .setAll(
              LaboratoryQuantityUnit.GRAM,
              LaboratoryQuantityUnit.MILLIGRAM,
              LaboratoryQuantityUnit.KILOGRAM);
      unit.setValue(LaboratoryQuantityUnit.MILLIGRAM);
      unit.valueProperty().addListener(o -> this.changed());
      this.units.put(key, unit);
    }
    this.component
        .getItems()
        .setAll(
            this.expected.experiment().components().stream()
                .map(c -> new Choice(c.id(), c.molecule().displayName()))
                .toList());
    this.component.setId("evidence-component");
    this.component.setMaxWidth(Double.MAX_VALUE);
    this.component.getSelectionModel().selectFirst();
    this.component.getItems().stream()
        .filter(c -> c.id().equals(selected))
        .findFirst()
        .ifPresent(this.component::setValue);
    this.sample.setId("evidence-sample");
    this.sample.setMaxWidth(Double.MAX_VALUE);
    this.origin.getItems().setAll(LaboratoryAnalysis.Origin.values());
    this.origin.setPromptText("Происхождение данных");
    this.origin.setId("evidence-origin");
    this.method.getItems().setAll(Method.values());
    this.method.setValue(Method.CORRELATIONS);
    this.method.setId("evidence-method");
    this.experiment2D.getItems().setAll(Experiment2D.values());
    this.experiment2D.setValue(Experiment2D.HSQC);
    this.xNucleus.getItems().setAll(LaboratoryAnalysis.Nucleus.values());
    this.xNucleus.setValue(LaboratoryAnalysis.Nucleus.H1);
    this.yNucleus
        .getItems()
        .setAll(
            LaboratoryAnalysis.Nucleus.C13,
            LaboratoryAnalysis.Nucleus.N15,
            LaboratoryAnalysis.Nucleus.H1);
    this.yNucleus.setValue(LaboratoryAnalysis.Nucleus.C13);
    this.conclusion.getItems().setAll(Conclusion.values());
    this.conclusion.setValue(Conclusion.PENDING);
    this.component
        .valueProperty()
        .addListener(
            o -> {
              this.updateIdentityContext();
              if (!this.changing) {
                this.selectComponent();
                this.changed();
              }
            });
    this.sample
        .valueProperty()
        .addListener(
            o -> {
              this.updateIdentityContext();
              if (!this.changing) {
                this.selectSnapshot();
                this.changed();
              }
            });
    this.method
        .valueProperty()
        .addListener(
            (o, old, value) -> {
              if (this.changing) return;
              if (this.pending() && this.hasRecordInput()) {
                this.changing = true;
                this.method.setValue(old);
                this.changing = false;
                this.status.setText(
                    "Добавьте текущую запись или очистите ввод перед сменой метода.");
                return;
              }
              this.begin(value);
            });
    this.experiment2D
        .valueProperty()
        .addListener(
            o -> {
              if (!this.changing) {
                this.yNucleus.setValue(
                    Set.of(Experiment2D.COSY, Experiment2D.NOESY, Experiment2D.ROESY)
                            .contains(this.experiment2D.getValue())
                        ? LaboratoryAnalysis.Nucleus.H1
                        : LaboratoryAnalysis.Nucleus.C13);
                this.changed();
              }
            });
    for (final ComboBox<?> combo :
        List.of(this.origin, this.xNucleus, this.yNucleus, this.conclusion)) {
      combo.valueProperty().addListener(o -> this.changed());
    }
    for (final TextArea area :
        List.of(this.source, this.note, this.preparation, this.conclusionText)) {
      area.setWrapText(true);
      area.setPrefRowCount(2);
      area.textProperty().addListener(o -> this.changed());
    }
    this.source.setPromptText("Источник данных, файл, методика или публикация");
    this.preparation.setPromptText(
        "Подготовка образца, стандарта и соответствие анализируемой порции");
    this.note.setPromptText("Замечания к измерению и интерпретации");
    this.result.setEditable(false);
    this.result.setWrapText(true);
    this.result.setPrefRowCount(15);
    this.spectrumView.onPick(point -> this.spectralAction(() -> this.addCorrelationPoint(point)));
    this.multipletView.onLine(
        (id, ppm) -> this.spectralAction(() -> this.addMultipletLine(id, ppm)));
    this.multipletView.onSelect(
        id -> {
          this.multiplets.getItems().stream()
              .filter(row -> row.id.equals(id))
              .findFirst()
              .ifPresent(
                  row -> {
                    this.multiplets.getSelectionModel().select(row);
                    this.multiplets.scrollTo(row);
                  });
        });
    this.multipletPreview.editable(false);
    this.cancelSpectrumImport.setId("evidence-cancel-spectrum");
    this.cancelSpectrumImport.setOnAction(
        e -> {
          if (this.spectrumLoading != null) {
            this.spectrumCancelRequested = true;
            this.spectrumLoading.interrupt();
            this.status.setText("Останавливаю загрузку спектра…");
          }
        });
    visible(this.cancelSpectrumImport, false);
    this.assignmentAxis.getItems().setAll("Назначить ось X / мультиплет", "Назначить ось Y");
    this.assignmentAxis.getSelectionModel().selectFirst();
    this.assignmentAxis.valueProperty().addListener(o -> this.highlightRow());
    this.structure.hint(
        "Выберите строку сигнала, затем нажмите на атом. Для ¹H явно отмечается протон при выбранном тяжёлом атоме.");
    this.structure.onPick(this::assign);
    this.plot.onPick(
        i -> {
          final var table =
              this.method.getValue() == Method.CORRELATIONS
                  ? this.correlations
                  : this.method.getValue() == Method.MULTIPLETS ? this.multiplets : this.peaks;
          if (i < table.getItems().size()) {
            table.getSelectionModel().select(i);
            table.scrollTo(i);
            this.tabs.getSelectionModel().select(this.inputTab);
          }
        });
    this.check = button("Проверить результат", "check", this::preview);
    this.check.getStyleClass().add("laboratory-primary-action");
    this.apply = button("Добавить проверенную запись", "apply", this::apply);
    for (final ComboBox<?> choice : List.of(this.component, this.sample)) {
      choice.setPrefWidth(260);
      choice.setMinWidth(180);
    }
    for (final ComboBox<Choice> choice : List.of(this.component, this.sample)) {
      choice.setCellFactory(
          v ->
              new ListCell<>() {
                protected void updateItem(final Choice value, final boolean empty) {
                  super.updateItem(value, empty);
                  this.setText(empty || value == null ? null : value.title());
                  this.setTooltip(empty || value == null ? null : identityTooltip(value));
                }
              });
    }
    this.identityContext.setId("evidence-identity-context");
    this.identityContext.setMaxWidth(Double.MAX_VALUE);
    this.identityContext.setMinWidth(0);
    this.tracePlot.setId("evidence-input-trace");
    this.tracePlot.setMinHeight(155);
    this.tracePlot.setPrefHeight(195);
    this.tracePlot.setMaxHeight(250);
    this.selectRange.setId("evidence-select-range");
    this.selectRange
        .selectedProperty()
        .addListener(
            (o, b, a) -> {
              this.tracePlot.rangeMode(a);
              this.traceHelp.setText(
                  a
                      ? "Протяните по графику: границы привязываются к исходным точкам. Точные времена можно ввести ниже."
                      : "Колесо — масштаб, перетаскивание — сдвиг. Для выбора границ нажмите «Выделить область».");
            });
    this.tracePlot.onRange(
        (from, to) -> {
          f("from").setText(value(from));
          f("to").setText(value(to));
          this.status.setText(
              "Границы выбраны. Нажмите «Интегрировать область», чтобы добавить пик.");
        });
    this.traceHelp.setText(
        "Колесо — масштаб, перетаскивание — сдвиг. Для выбора границ нажмите «Выделить область».");
    this.traceView
        .getChildren()
        .setAll(
            new FlowPane(
                8,
                6,
                this.selectRange,
                button("Уместить трассу", "trace-fit", this.tracePlot::fit)),
            this.tracePlot,
            this.traceHelp);
    visible(this.traceView, false);
    this.peaks
        .getItems()
        .addListener(
            (javafx.collections.ListChangeListener<LaboratoryEvidenceTable.Row>)
                c -> this.resizeChromTables());
    this.calibration
        .getItems()
        .addListener(
            (javafx.collections.ListChangeListener<LaboratoryEvidenceTable.Row>)
                c -> this.resizeChromTables());
    this.method.setPrefWidth(200);
    final FlowPane common =
        new FlowPane(
            16,
            10,
            field("Анализ", this.method),
            field("Соединение", this.component),
            field("Образец", this.sample));
    f("title").setPrefWidth(360);
    this.origin.setPrefWidth(230);
    final FlowPane identity =
        new FlowPane(16, 10, field("Название", f("title")), field("Происхождение", this.origin));
    this.source.setPrefRowCount(1);
    this.source.setMinHeight(38);
    this.source.setPrefHeight(38);
    this.source.setMaxHeight(70);
    this.notesPane =
        details(
            "Примечания и файлы",
            new VBox(
                10, field("Примечание", this.note), field("Связанные файлы", this.attachmentBox)));
    this.notesPane.setId("evidence-notes");
    this.form
        .getChildren()
        .setAll(
            common,
            this.identityContext,
            identity,
            field("Источник данных", this.source),
            this.specific,
            this.notesPane);
    this.form.setPadding(new Insets(14, 18, 18, 18));
    this.formScroll = scroll(this.form);
    this.formScroll.setFitToHeight(false);
    final var inputPage = new BorderPane(this.formScroll);
    final var inputActions =
        new VBox(
            6,
            new FlowPane(
                8,
                8,
                this.check,
                button("Сбросить ввод", "clear", () -> this.begin(this.method.getValue())),
                this.cancelSpectrumImport),
            this.feedback);
    inputActions.setPadding(new Insets(10, 18, 12, 18));
    inputPage.setBottom(inputActions);
    this.inputTab.setContent(inputPage);
    this.graphTools
        .getChildren()
        .setAll(
            button("Уместить график", "fit", this.plot::fit),
            note("Колесо — масштаб; перетаскивание — сдвиг; точка — строка"));
    this.emptyResult.setId("evidence-empty-result");
    this.emptyResult
        .getChildren()
        .setAll(
            this.resultHint,
            button("Рассчитать / обновить результат", "preview-empty", this::preview),
            note(
                "Предпросмотр не добавляет запись в опыт. Сохранение результата выполняется отдельно после вашей проверки."));
    this.resultDetails.setAnimated(false);
    this.resultDetails.setId("evidence-result-details");
    this.dossierCard.setId("evidence-dossier-summary");
    this.review
        .getChildren()
        .setAll(
            this.emptyResult,
            this.resultCard,
            this.dossierCard,
            this.graphTools,
            this.plot,
            this.multipletPreview,
            this.resultDetails);
    this.reviewApply = button("Добавить проверенную запись", "review-apply", this::apply);
    this.reviewApply.disableProperty().bind(this.apply.disableProperty());
    this.reviewApply.getStyleClass().add("laboratory-primary-action");
    this.review.setPadding(new Insets(16));
    final var reviewScroll = scroll(this.review);
    reviewScroll.setFitToHeight(false);
    final var reviewPage = new BorderPane(reviewScroll);
    final var resultActions =
        new FlowPane(
            8,
            8,
            this.reviewApply,
            button(
                "К вводу",
                "back-input",
                () -> this.tabs.getSelectionModel().select(this.inputTab)));
    resultActions.setId("evidence-result-actions");
    resultActions.setPadding(new Insets(10, 18, 12, 18));
    reviewPage.setBottom(resultActions);
    this.previewTab.setContent(reviewPage);
    final Button showSaved = button("Показать запись", "show", this::showHistory);
    this.history.setId("evidence-history");
    this.history.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px;");
    this.history.setCellFactory(
        v ->
            new ListCell<>() {
              {
                this.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px;");
                this.setOnMouseClicked(
                    e -> {
                      if (e.getButton() == MouseButton.PRIMARY
                          && e.getClickCount() == 2
                          && !this.isEmpty()) {
                        LaboratoryEvidencePage.this
                            .history
                            .getSelectionModel()
                            .select(this.getItem());
                        showSaved.fire();
                        e.consume();
                      }
                    });
              }

              protected void updateItem(Entry value, boolean empty) {
                super.updateItem(value, empty);
                this.setText(
                    empty || value == null
                        ? null
                        : recordLabel(
                            LaboratoryEvidencePage.this.editor.document().experiment(),
                            value.title(),
                            value.data().method(),
                            value.sampleId(),
                            value.recordedAt(),
                            value.origin(),
                            value.source(),
                            value.correctsId() == null ? "" : "исправление"));
                this.setWrapText(true);
                this.setTooltip(
                    empty || value == null
                        ? null
                        : recordTooltip(value.title(), value.source(), "EVIDENCE", value.id()));
              }
            });
    this.history.setOnKeyPressed(
        e -> {
          if (e.getCode() == KeyCode.ENTER
              && !e.isShortcutDown()
              && this.history.getSelectionModel().getSelectedItem() != null) {
            showSaved.fire();
            e.consume();
          }
        });
    this.history.setPlaceholder(note("Сохранённых записей пока нет."));
    this.allHistory.selectedProperty().addListener(o -> this.refreshHistory());
    final VBox saved =
        new VBox(
            10,
            new FlowPane(
                8,
                8,
                this.allHistory,
                showSaved,
                button("Создать исправление", "correct", this::correct),
                button("Сравнить спектры", "compare", this::comparison)),
            note("Открыть запись: двойной щелчок или Enter. Просмотр не изменяет данные."),
            this.history);
    saved.setPadding(new Insets(16));
    VBox.setVgrow(this.history, Priority.ALWAYS);
    this.historyTab.setContent(saved);
    for (final var tab : List.of(this.inputTab, this.previewTab, this.historyTab)) {
      tab.setClosable(false);
    }
    this.tabs.getTabs().setAll(this.inputTab, this.previewTab, this.historyTab);
    this.setCenter(this.tabs);
    this.setBottom(this.status);
    BorderPane.setMargin(this.status, new Insets(8, 16, 12, 16));
    this.addHeaderAction(
        button(
            "Новая запись",
            "new",
            () -> {
              if (this.running) throw new IllegalStateException("Дождитесь загрузки.");
              if (this.pending() && this.hasRecordInput())
                throw new IllegalStateException("Добавьте либо очистите текущую запись.");
              this.begin(this.method.getValue());
              this.status.setText("");
            }));
    this.begin(Method.CORRELATIONS);
    this.refreshHistory();
  }

  /** Context-only entry point for a fresh page; no sample or provenance is inferred. */
  void openDossier(final String componentId) {
    if (this.running
        || this.pending()
        || this.applied
        || this.prepared != null
        || this.correctsId != null) {
      throw new IllegalStateException("Сначала добавьте или сбросьте текущую запись аналитики.");
    }
    this.begin(Method.DOSSIER);
    this.changing = true;
    try {
      final var product =
          this.component.getItems().stream()
              .filter(c -> Objects.equals(c.id(), componentId))
              .findFirst()
              .orElse(null);
      this.component.setPromptText("Выберите продукт");
      this.component.setValue(product);
      this.selectComponent();
      this.references();
      this.feedback.setText(
          product == null
              ? "Выберите продукт для досье. Физический образец и происхождение данных задаются отдельно."
              : "Досье выбранного продукта. При необходимости выберите физический образец.");
      this.status.setText("");
    } finally {
      this.changing = false;
    }
    this.dirty = false;
  }

  void showForSample(final String sampleId, final String entryId) {
    if (this.pending())
      throw new IllegalStateException("Добавьте либо сбросьте текущий ввод аналитики.");
    final var sample =
        LaboratorySampleService.sample(this.editor.document().experiment(), sampleId);
    this.begin(Method.CORRELATIONS);
    this.changing = true;
    try {
      this.chooseIdentity(sample.componentId(), sampleId, sample.molecule());
    } finally {
      this.changing = false;
    }
    final var entries =
        this.editor.document().experiment().analyticalEvidence().entries().stream()
            .filter(e -> sampleId.equals(e.sampleId()))
            .toList();
    this.history.getItems().setAll(entries);
    if (entryId == null) {
      this.tabs.getSelectionModel().select(this.historyTab);
      return;
    }
    final var entry =
        entries.stream()
            .filter(e -> e.id().equals(entryId))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Аналитическая запись не относится к выбранному образцу."));
    this.history.getSelectionModel().select(entry);
    this.show(entry);
  }

  void dossierForSample(final String sampleId) {
    if (this.pending())
      throw new IllegalStateException("Добавьте либо сбросьте текущий ввод аналитики.");
    final var sample =
        LaboratorySampleService.sample(this.editor.document().experiment(), sampleId);
    this.begin(Method.DOSSIER);
    this.changing = true;
    try {
      this.chooseIdentity(sample.componentId(), sampleId, sample.molecule());
      this.references();
    } finally {
      this.changing = false;
    }
  }

  private void comparison() {
    if (this.running) throw new IllegalStateException("Дождитесь загрузки.");
    if (this.comparison != null) this.comparison.dispose();
    this.comparison =
        new LaboratorySpectralComparisonPane(
            this.editor.document().experiment(),
            () -> {
              this.comparison = null;
              this.setCenter(this.tabs);
            });
    this.setCenter(this.comparison);
  }

  private String atomLabels(final String value) {
    if (value.isBlank()) return "—";
    if (this.snapshot == null) return "Выберите структуру";
    try {
      return String.join(
          "; ",
          LaboratoryEvidenceService.assignments(value).stream()
              .map(
                  a -> {
                    final var atom =
                        this.snapshot.atoms().stream()
                            .filter(x -> x.id().equals(a.atomId()))
                            .findFirst()
                            .orElse(null);
                    if (atom == null) return "Неизвестный атом";
                    return (a.attachedProton() ? "H при " : "")
                        + atom.element().symbol().value()
                        + (this.snapshot.atoms().indexOf(atom) + 1);
                  })
              .sorted()
              .toList());
    } catch (final RuntimeException invalid) {
      return "Проверьте назначение";
    }
  }

  private Node assignmentView(final boolean two) {
    final var views = new FlowPane(14, 10);
    final var width =
        javafx.beans.binding.Bindings.when(this.widthProperty().lessThan(900))
            .then(views.widthProperty())
            .otherwise(views.widthProperty().subtract(24).divide(2));
    this.structure.prefWidthProperty().unbind();
    this.structure.prefWidthProperty().bind(width);
    this.assignmentPlot.prefWidthProperty().unbind();
    this.assignmentPlot.prefWidthProperty().bind(width);
    this.structure.setPrefHeight(240);
    this.assignmentPlot.setPrefHeight(240);
    this.assignmentPlot.setMaxHeight(260);
    views.getChildren().setAll(this.structure, this.assignmentPlot);
    final var tools = new FlowPane(10, 6);
    if (two) tools.getChildren().add(this.assignmentAxis);
    tools
        .getChildren()
        .addAll(
            button("Уместить корреляции / сигналы", "assignment-fit", this.assignmentPlot::fit),
            button(
                "Снять назначения строки",
                "assignment-clear",
                () -> {
                  final var table = two ? this.correlations : this.multiplets;
                  final var row = table.getSelectionModel().getSelectedItem();
                  if (row == null) throw new IllegalArgumentException("Выберите строку.");
                  if (two) {
                    row.set(3, "");
                    row.set(4, "");
                  } else row.set(5, "");
                }));
    this.assignmentPlot.onPick(
        row -> {
          final var table =
              this.method.getValue() == Method.CORRELATIONS ? this.correlations : this.multiplets;
          if (row < table.getItems().size()) {
            table.getSelectionModel().select(row);
            table.scrollTo(row);
            this.highlightRow();
          }
        });
    this.updateAssignmentGraph();
    return new VBox(7, tools, views, this.assignmentFeedback);
  }

  private void updateAssignmentGraph() {
    if (this.changing || this.method.getValue() == null) return;
    this.updateNmrViews();
    try {
      final Data graph =
          switch (this.method.getValue()) {
            case CORRELATIONS ->
                new Correlations(
                    this.experiment2D.getValue(),
                    LaboratoryAnalysis.Nucleus.H1,
                    this.yNucleus.getValue(),
                    f("solvent").getText(),
                    this.correlations.getItems().stream()
                        .map(
                            r ->
                                new Correlation(
                                    r.id,
                                    num(r.get(0)),
                                    num(r.get(1)),
                                    optional(r.get(2)),
                                    LaboratoryEvidenceService.assignments(r.get(3)),
                                    LaboratoryEvidenceService.assignments(r.get(4)),
                                    r.get(5)))
                        .toList());
            case MULTIPLETS ->
                new Multiplets(
                    this.xNucleus.getValue(),
                    num(f("frequency").getText()),
                    this.multiplets.getItems().stream()
                        .map(
                            r ->
                                new Multiplet(
                                    r.id,
                                    num(r.get(0)),
                                    optional(r.get(1)),
                                    numbers(r.get(3)),
                                    numbers(r.get(4)),
                                    LaboratoryEvidenceService.assignments(r.get(5)),
                                    r.get(2),
                                    r.get(6)))
                        .toList());
            default -> null;
          };
      if (graph == null) return;
      this.assignmentPlot.show(graph);
      this.assignmentFeedback.setText(
          "Строка, точка и выбранный атом связаны. Клик по атому добавляет или снимает назначение.");
      this.highlightRow();
    } catch (final RuntimeException incomplete) {
      this.assignmentPlot.clear();
      this.assignmentFeedback.setText(
          "Для графика заполните числовые поля строк"
              + (this.method.getValue() == Method.MULTIPLETS ? " и частоту" : "")
              + ". Незаконченный ввод сохраняется в форме.");
    }
  }

  private void highlightRow() {
    if (this.snapshot == null || this.method.getValue() == null) return;
    final boolean two = this.method.getValue() == Method.CORRELATIONS;
    if (!two && this.method.getValue() != Method.MULTIPLETS) return;
    final var table = two ? this.correlations : this.multiplets;
    this.assignmentPlot.select(table.getSelectionModel().getSelectedIndex());
    this.spectrumView.select(two ? table.getSelectionModel().getSelectedIndex() : -1);
    final var row = table.getSelectionModel().getSelectedItem();
    if (!two) this.multipletView.select(row == null ? null : row.id);
    ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId atom = null;
    if (row != null) {
      try {
        final var assigned =
            LaboratoryEvidenceService.assignments(
                row.get(
                    two
                        ? (this.assignmentAxis.getSelectionModel().getSelectedIndex() == 1 ? 4 : 3)
                        : 5));
        if (!assigned.isEmpty()) atom = assigned.iterator().next().atomId();
      } catch (final RuntimeException ignored) {
      }
    }
    this.structure.show(new LaboratoryMoleculePlacement().place(this.snapshot, 0, 0), atom);
  }

  void saving(final java.util.function.Consumer<Boolean> save) {
    this.addHeaderAction(button("Сохранить опыт", "save", () -> save.accept(false)));
  }

  private static Tooltip identityTooltip(final Choice value) {
    final var tooltip =
        new Tooltip(value.title() + (value.id().isBlank() ? "" : "\nID: " + value.id()));
    tooltip.setWrapText(true);
    tooltip.setMaxWidth(700);
    return tooltip;
  }

  private void updateIdentityContext() {
    final var c = this.component.getValue();
    final var s = this.sample.getValue();
    this.component.setTooltip(c == null ? null : identityTooltip(c));
    this.sample.setTooltip(s == null ? null : identityTooltip(s));
    this.identityContext.setText(
        (c == null ? "Соединение не выбрано" : c.title())
            + "\nОбразец: "
            + (s == null ? "не выбран" : s.title()));
  }

  private void resizeChromTables() {
    final boolean present = !this.peaks.getItems().isEmpty();
    visible(this.peaks, present);
    visible(this.emptyPeaks, !present);
    for (final var table : List.of(this.peaks, this.calibration)) {
      final double height = Math.min(250, Math.max(84, 38 + table.getItems().size() * 30));
      table.setMinHeight(84);
      table.setPrefHeight(height);
      table.setMaxHeight(height);
    }
  }

  private void scrollToTrace() {
    if (this.disposed
        || this.getScene() == null
        || this.trace.isEmpty()
        || this.method.getValue() != Method.CHROM) return;
    this.form.applyCss();
    this.form.layout();
    this.formScroll.applyCss();
    this.formScroll.layout();
    final double height = this.form.getHeight() - this.formScroll.getViewportBounds().getHeight();
    if (height > 0) {
      final double y = this.form.sceneToLocal(this.traceView.localToScene(0, 0)).getY();
      this.formScroll.setVvalue(Math.max(0, Math.min(1, (y - 6) / height)));
    }
  }

  private void updateTraceView() {
    if (this.trace != this.displayedTrace) {
      this.displayedTrace = this.trace;
      if (this.trace.isEmpty()) this.tracePlot.clear();
      else this.tracePlot.showTrace(this.trace);
    }
    visible(this.traceView, !this.trace.isEmpty());
    this.updateTraceRange();
  }

  private void updateTraceRange() {
    try {
      this.tracePlot.range(optional(f("from").getText()), optional(f("to").getText()));
    } catch (final RuntimeException invalid) {
      this.tracePlot.range(null, null);
    }
  }

  private static VBox compactField(final String label, final Node node, final double width) {
    final var box = field(label, node);
    box.setPrefWidth(width);
    box.setMaxWidth(width);
    return box;
  }

  private TextField f(final String key) {
    return this.fields.get(key);
  }

  private static VBox field(final String name, final Node node) {
    final var box = new VBox(4, note(name), node);
    box.setMinWidth(0);
    if (node instanceof TextField text) text.setPrefColumnCount(15);
    return box;
  }

  private static TitledPane details(final String title, final Node content) {
    final var box = new VBox(10, content);
    box.setPadding(new Insets(10, 2, 4, 2));
    final var pane = new TitledPane(title, box);
    pane.setAnimated(false);
    pane.setExpanded(false);
    return pane;
  }

  private HBox mass(final String key) {
    final HBox row = new HBox(6, f(key), this.units.get(key));
    HBox.setHgrow(f(key), Priority.ALWAYS);
    return row;
  }

  private Button button(final String name, final String id, final Runnable action) {
    final var button = new Button(name);
    button.setId("evidence-" + id);
    button.setOnAction(
        e -> {
          try {
            action.run();
          } catch (final Exception error) {
            this.status.setText(message(error));
          }
        });
    return button;
  }

  private LaboratoryEvidenceTable table(
      final String id, final List<String> header, final List<String> labels) {
    final var table = new LaboratoryEvidenceTable(header, labels, this::changed);
    table.setId("evidence-" + id);
    return table;
  }

  private VBox tableBlock(final String title, final LaboratoryEvidenceTable table) {
    final var more = new MenuButton("Таблица…");
    more.getItems()
        .setAll(
            menu("Удалить выбранную строку", title + "-remove", table::removeSelected),
            menu("Загрузить CSV/TSV", title + "-import", () -> this.importTable(table)),
            menu(
                "Копировать таблицу",
                title + "-copy",
                () -> {
                  final var value = new ClipboardContent();
                  value.putString(table.text());
                  Clipboard.getSystemClipboard().setContent(value);
                  this.status.setText(
                      "Таблица с заголовком скопирована; этот же формат принимается при загрузке.");
                }));
    final var tools =
        new FlowPane(8, 6, button("Добавить строку", title + "-add", table::add), more);
    if (table == this.correlations || table == this.multiplets)
      tools
          .getChildren()
          .add(
              button(
                  "К спектру ↑",
                  title + "-spectrum",
                  () ->
                      this.scrollToNmr(
                          table == this.correlations ? this.spectrumView : this.multipletView)));
    return new VBox(7, note(title), table, tools);
  }

  private MenuItem menu(final String title, final String id, final Runnable action) {
    final var item = new MenuItem(title);
    item.setId("evidence-" + id);
    item.setOnAction(
        e -> {
          try {
            action.run();
          } catch (final Exception error) {
            this.status.setText(message(error));
          }
        });
    return item;
  }

  private boolean hasRecordInput() {
    return this.origin.getValue() != null
        || !this.source.getText().isBlank()
        || !this.note.getText().isBlank()
        || !this.preparation.getText().isBlank()
        || !this.conclusionText.getText().isBlank()
        || this.fields.entrySet().stream()
            .anyMatch(
                e ->
                    !Set.of("tolerance", "technique").contains(e.getKey())
                        && !e.getValue().getText().isBlank())
        || !f("tolerance").getText().equals("5")
        || !f("technique").getText().equals("ВЭЖХ")
        || this.tables().values().stream().anyMatch(t -> !t.getItems().isEmpty())
        || !this.trace.isEmpty()
        || this.spectrum2D != null
        || this.multipletAnalysisId != null
        || this.attachments.values().stream().anyMatch(CheckBox::isSelected)
        || this.conclusion.getValue() != Conclusion.PENDING
        || this.analysisLinks.values().stream().anyMatch(v -> !v.isSelected())
        || this.evidenceLinks.values().stream().anyMatch(v -> !v.isSelected());
  }

  private void changed() {
    if (this.changing) return;
    this.dirty = true;
    this.prepared = null;
    if (this.apply != null) this.apply.setDisable(true);
    this.clearResult(true);
    this.feedback.setText("Ввод изменён. Просмотрите расчёты и назначения перед добавлением.");
    this.updateAssignmentGraph();
    this.updateTraceRange();
  }

  boolean pending() {
    return this.dirty && !this.applied;
  }

  private void begin(final Method value) {
    if (this.running)
      throw new IllegalStateException("Дождитесь завершения загрузки или остановите её.");
    if (this.comparison != null) {
      this.comparison.dispose();
      this.comparison = null;
      this.setCenter(this.tabs);
    }
    this.changing = true;
    this.expected = this.editor.document();
    final String
        selected = this.component.getValue() == null ? null : this.component.getValue().id(),
        selectedSample = this.sampleId();
    this.component
        .getItems()
        .setAll(
            this.expected.experiment().components().stream()
                .map(c -> new Choice(c.id(), c.molecule().displayName()))
                .toList());
    this.component.getSelectionModel().selectFirst();
    this.component.getItems().stream()
        .filter(c -> c.id().equals(selected))
        .findFirst()
        .ifPresent(this.component::setValue);
    this.correctsId = null;
    this.draftId = "evidence_" + UUID.randomUUID();
    this.applied = false;
    this.conflict = false;
    this.dirty = false;
    this.method.setValue(value);
    this.fields.values().forEach(TextField::clear);
    this.source.clear();
    this.note.clear();
    this.preparation.clear();
    this.conclusionText.clear();
    this.origin.setValue(null);
    this.conclusion.setValue(Conclusion.PENDING);
    this.trace = List.of();
    this.spectrum2D = null;
    this.multipletAnalysisId = null;
    this.spectrumView.clear();
    this.multipletView.clear();
    this.multipletPreview.clear();
    this.traceInfo.setText("Числовая хроматограмма не загружена");
    for (final var t :
        List.of(this.correlations, this.multiplets, this.peaks, this.calibration, this.findings)) {
      t.getItems().clear();
    }
    this.component.setDisable(false);
    this.sample.setDisable(false);
    this.method.setDisable(false);
    this.form.setDisable(false);
    this.check.setDisable(false);
    f("tolerance").setText("5");
    f("technique").setText("ВЭЖХ");
    this.selectComponent();
    if (this.component.getValue() != null
        && Objects.equals(selected, this.component.getValue().id())) {
      this.sample.getItems().stream()
          .filter(v -> Objects.equals(v.id(), selectedSample))
          .findFirst()
          .ifPresent(this.sample::setValue);
      this.selectSnapshot();
    }
    this.configure();
    this.notesPane.setExpanded(false);
    this.attachmentBox.getChildren().clear();
    this.attachments.clear();
    for (final var a : this.expected.experiment().attachments()) {
      final var cb = new CheckBox(a.name());
      cb.selectedProperty().addListener(o -> this.changed());
      this.attachments.put(a.id(), cb);
      this.attachmentBox.getChildren().add(cb);
    }
    this.changing = false;
    this.dirty = false;
    this.prepared = null;
    this.feedback.setText("");
    this.apply.setDisable(true);
    this.tabs.getSelectionModel().select(this.inputTab);
    this.clearResult(false);
    this.assignmentPlot.clear();
    this.updateAssignmentGraph();
    this.selectRange.setSelected(false);
    this.updateTraceView();
    this.updateIdentityContext();
  }

  private void selectComponent() {
    final boolean previous = this.changing;
    this.changing = true;
    final var selected = this.component.getValue();
    this.sample.getItems().setAll(new Choice("", "Без отдельного образца"));
    if (selected != null) {
      this.expected.experiment().samples().stream()
          .filter(s -> s.componentId().equals(selected.id()))
          .forEach(
              s ->
                  this.sample
                      .getItems()
                      .add(new Choice(s.id(), s.code() + " · " + s.balance().label())));
    }
    this.sample.getSelectionModel().selectFirst();
    this.changing = previous;
    this.selectSnapshot();
  }

  private void selectSnapshot() {
    final var selected = this.component.getValue();
    if (selected == null) {
      this.snapshot = null;
      return;
    }
    final String sid = this.sampleId();
    this.snapshot =
        sid == null
            ? this.expected.experiment().components().stream()
                .filter(c -> c.id().equals(selected.id()))
                .findFirst()
                .orElseThrow()
                .molecule()
            : LaboratorySampleService.sample(this.expected.experiment(), sid).molecule();
    this.structure.show(new LaboratoryMoleculePlacement().place(this.snapshot, 0, 0), null);
    this.correlations.refresh();
    this.multiplets.refresh();
    this.references();
  }

  private String sampleId() {
    return this.sample.getValue() == null || this.sample.getValue().id().isEmpty()
        ? null
        : this.sample.getValue().id();
  }

  private void configure() {
    this.specific.getChildren().clear();
    this.integrationPane = null;
    this.calibrationPane = null;
    this.purityPane = null;
    this.findingsPane = null;
    switch (this.method.getValue()) {
      case CORRELATIONS -> {
        this.specific
            .getChildren()
            .addAll(
                new FlowPane(
                    12,
                    8,
                    field("Эксперимент", this.experiment2D),
                    field("Вторая ось", this.yNucleus),
                    field("Растворитель", f("solvent"))),
                this.spectrumInput(),
                tableBlock("Корреляции", this.correlations),
                details(
                    "Назначения на структуре",
                    new VBox(
                        8,
                        this.assignmentView(true),
                        note(
                            "Выберите строку и ось, затем нажмите атом. Протон при тяжёлом атоме отмечается явно; номера относятся к сохранённой структуре."))));
      }
      case MULTIPLETS -> {
        this.specific
            .getChildren()
            .addAll(
                new FlowPane(
                    12,
                    8,
                    field("Ядро", this.xNucleus),
                    field("Частота, МГц", f("frequency")),
                    button("Выбрать 1D спектр", "link-nmr", this::linkNmr),
                    button(
                        "К таблице ↓", "multiplet-table", () -> this.scrollToNmr(this.multiplets))),
                this.multipletSourceInfo,
                this.multipletView,
                new FlowPane(
                    8,
                    6,
                    button("Взять таблицу сигналов из 1D ЯМР", "from-nmr", this::fromNmr),
                    button(
                        "Убрать связь со спектром",
                        "unlink-nmr",
                        () -> {
                          this.multipletAnalysisId = null;
                          this.changed();
                        })),
                tableBlock("Мультиплеты", this.multiplets),
                details("Назначения на структуре", this.assignmentView(false)),
                note(
                    "Линии ppm и J разделяйте знаком ;. Расстояния соседних линий не считаются автоматически константами сложного мультиплета."));
      }
      case QNMR -> {
        this.specific
            .getChildren()
            .addAll(
                note("Образец"),
                new FlowPane(
                    16,
                    10,
                    field("Навеска образца", mass("sampleMass")),
                    field("Молярная масса, г/моль", f("sampleMolarMass")),
                    field("Интеграл аналита", f("sampleIntegral")),
                    field("Число ядер в интеграле", f("sampleNuclei"))),
                note("Внутренний стандарт"),
                new FlowPane(
                    16,
                    10,
                    field("Название стандарта", f("standard")),
                    field("Навеска стандарта", mass("standardMass")),
                    field("Молярная масса, г/моль", f("standardMolarMass")),
                    field("Интеграл стандарта", f("standardIntegral")),
                    field("Число ядер в интеграле", f("standardNuclei")),
                    field("Массовая доля стандарта, %", f("standardPurity"))),
                field("Подготовка и количественные условия", this.preparation),
                details(
                    "Интегралы из сохранённого 1D ЯМР",
                    button("Выбрать интегралы", "qnmr-signals", this::integrals)),
                note(
                    "Навески относятся к одной приготовленной смеси. Разведения должны быть учтены в навесках; чистота стандарта и число ядер задаются явно."));
      }
      case CHROM -> {
        final var integrate = button("Интегрировать область", "integrate", this::integrate);
        this.integrationPane =
            details(
                "Границы интегрирования",
                new VBox(
                    7,
                    new FlowPane(
                        12,
                        8,
                        compactField("Начало, мин", f("from"), 145),
                        compactField("Конец, мин", f("to"), 145),
                        compactField("Название пика", f("peakLabel"), 230),
                        integrate),
                    note(
                        "Трапеции над прямой между границами; площадь — отклик × минута. Все площади должны иметь одинаковые единицы.")));
        this.integrationPane.setId("evidence-integration");
        final var peakBlock = this.tableBlock("Пики", this.peaks);
        peakBlock.getChildren().add(1, this.emptyPeaks);
        final var usePeak =
            button(
                "Определять выбранный пик",
                "select-quantified",
                () -> {
                  final int index = this.peaks.getSelectionModel().getSelectedIndex();
                  if (index < 0)
                    throw new IllegalArgumentException("Выберите строку в таблице пиков.");
                  f("quantifiedPeak").setText(Integer.toString(index + 1));
                });
        final var calTable = this.tableBlock("Калибровочные точки", this.calibration);
        calTable.setPrefWidth(370);
        calTable.setMaxWidth(440);
        final var selectedPeak =
            new VBox(6, compactField("Номер пика, от 1", f("quantifiedPeak"), 180), usePeak);
        final var calFields =
            new FlowPane(
                16,
                10,
                compactField("Единица массы стандарта", this.units.get("calibration"), 195),
                selectedPeak,
                compactField("Масса образца в анализируемой порции", mass("representedMass"), 290));
        final VBox cal =
            new VBox(
                10,
                calTable,
                calFields,
                compactField("Источник калибровки", f("calibrationSource"), 650),
                field("Подготовка и соответствие калибровки порции", this.preparation));
        this.calibrationPane = details("Количественный расчёт по калибровке", cal);
        this.purityPane =
            details(
                "Отдельно сообщённая массовая доля",
                new VBox(
                    8,
                    field("Массовая доля, %", f("reportedPurity")),
                    field("Источник и метод", f("puritySource"))));
        this.specific
            .getChildren()
            .addAll(
                new FlowPane(
                    12, 8, field("Метод", f("technique")), field("Детектор", f("detector"))),
                new FlowPane(
                    8,
                    6,
                    button("Загрузить числовую хроматограмму", "trace-import", this::importTrace),
                    this.traceInfo),
                this.traceView,
                this.integrationPane,
                peakBlock,
                this.calibrationPane,
                this.purityPane);
        this.resizeChromTables();
        this.updateTraceView();
      }
      case DOSSIER -> {
        this.references();
        this.findingsPane =
            details(
                "Расхождения и настройки проверки",
                new VBox(
                    10,
                    field("Допуск отклонения МС, ppm", f("tolerance")),
                    tableBlock("Расхождения и наблюдения", this.findings),
                    note(
                        "Укажите уровень замечания и разрешено ли оно. Автоматические замечания будут видны в результате.")));
        this.specific
            .getChildren()
            .addAll(
                field("Анализы выбранного образца", this.references),
                field("Ваше заключение", this.conclusion),
                field("Основание заключения", this.conclusionText),
                this.findingsPane);
      }
    }
  }

  private void revealDetails() {
    this.notesPane.setExpanded(
        !this.note.getText().isBlank()
            || this.attachments.values().stream().anyMatch(CheckBox::isSelected));
    if (this.integrationPane != null)
      this.integrationPane.setExpanded(
          !this.trace.isEmpty() || !f("from").getText().isBlank() || !f("to").getText().isBlank());
    if (this.calibrationPane != null)
      this.calibrationPane.setExpanded(
          !this.calibration.getItems().isEmpty()
              || !f("representedMass").getText().isBlank()
              || !this.preparation.getText().isBlank());
    if (this.purityPane != null)
      this.purityPane.setExpanded(
          !f("reportedPurity").getText().isBlank() || !f("puritySource").getText().isBlank());
    if (this.findingsPane != null)
      this.findingsPane.setExpanded(
          !this.findings.getItems().isEmpty() || !f("tolerance").getText().equals("5"));
  }

  private void references() {
    this.references.getChildren().clear();
    this.analysisLinks.clear();
    this.evidenceLinks.clear();
    if (this.component.getValue() == null) {
      return;
    }
    final String cid = this.component.getValue().id(), sid = this.sampleId();
    final var e = this.expected.experiment();
    final Set<String> corrected = new HashSet<>();
    e.analyses()
        .forEach(
            a -> {
              if (a.correctsId() != null) {
                corrected.add(a.correctsId());
              }
            });
    for (final var a : e.analyses()) {
      if (a.componentId().equals(cid)
          && Objects.equals(a.sampleId(), sid)
          && !corrected.contains(a.id())) {
        final var cb =
            this.referenceChoice(
                a.title(),
                a.data().method(),
                a.sampleId(),
                a.recordedAt(),
                a.origin(),
                a.source(),
                "LEGACY",
                a.id(),
                false,
                true);
        this.analysisLinks.put(a.id(), cb);
        this.references.getChildren().add(cb);
      }
    }
    for (final var a : e.analyticalEvidence().latest()) {
      if (a.componentId().equals(cid)
          && Objects.equals(a.sampleId(), sid)
          && !(a.data() instanceof Dossier)) {
        final var cb =
            this.referenceChoice(
                a.title(),
                a.data().method(),
                a.sampleId(),
                a.recordedAt(),
                a.origin(),
                a.source(),
                "EVIDENCE",
                a.id(),
                false,
                true);
        this.evidenceLinks.put(a.id(), cb);
        this.references.getChildren().add(cb);
      }
    }
    if (this.references.getChildren().isEmpty()) {
      this.references
          .getChildren()
          .add(note("У выбранного образца пока нет сохранённых аналитических записей."));
    }
  }

  private CheckBox referenceChoice(
      final String title,
      final String method,
      final String sampleId,
      final Instant recordedAt,
      final LaboratoryAnalysis.Origin origin,
      final String source,
      final String kind,
      final String id,
      final boolean historical,
      final boolean selected) {
    final var cb =
        new CheckBox(
            recordLabel(
                this.expected.experiment(),
                title,
                method,
                sampleId,
                recordedAt,
                origin,
                source,
                historical ? "предыдущая версия" : ""));
    cb.setWrapText(true);
    cb.setMinWidth(0);
    cb.setMaxWidth(Double.MAX_VALUE);
    cb.setTooltip(recordTooltip(title, source, kind, id));
    cb.setSelected(selected);
    cb.selectedProperty().addListener(o -> this.changed());
    return cb;
  }

  private static String recordLabel(
      final LaboratoryExperiment experiment,
      final String title,
      final String method,
      final String sampleId,
      final Instant recordedAt,
      final LaboratoryAnalysis.Origin origin,
      final String source,
      final String version) {
    final String sample =
        sampleId == null
            ? "без образца"
            : experiment.samples().stream()
                .filter(s -> s.id().equals(sampleId))
                .map(s -> s.code())
                .findFirst()
                .orElse("образец недоступен");
    return brief(title, 88)
        + " · "
        + brief(source, 36)
        + "\n"
        + method
        + " · "
        + sample
        + " · "
        + DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(recordedAt)
        + " · "
        + origin
        + (version.isEmpty() ? "" : " · " + version);
  }

  private static Tooltip recordTooltip(
      final String title, final String source, final String kind, final String id) {
    final var tooltip =
        new Tooltip(
            title
                + "\nИсточник: "
                + (source.isBlank() ? "не указан" : source)
                + "\nСсылка: "
                + kind
                + " / "
                + id);
    tooltip.setWrapText(true);
    tooltip.setMaxWidth(640);
    return tooltip;
  }

  private static String brief(final String text, final int limit) {
    final String value = text.replaceAll("\\s+", " ").strip();
    return value.isEmpty()
        ? "не указан"
        : value.codePointCount(0, value.length()) > limit
            ? value.substring(0, value.offsetByCodePoints(0, limit - 1)) + "…"
            : value;
  }

  private void assign(final ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId atom) {
    final boolean two = this.method.getValue() == Method.CORRELATIONS;
    final var table = two ? this.correlations : this.multiplets;
    final var row = table.getSelectionModel().getSelectedItem();
    if (row == null) {
      this.status.setText("Сначала выберите строку сигнала или корреляции.");
      return;
    }
    final boolean y = two && this.assignmentAxis.getSelectionModel().getSelectedIndex() == 1;
    final int column = two ? (y ? 4 : 3) : 5;
    final var nucleus =
        two
            ? (y ? this.yNucleus.getValue() : LaboratoryAnalysis.Nucleus.H1)
            : this.xNucleus.getValue();
    final var a =
        this.snapshot.atoms().stream().filter(v -> v.id().equals(atom)).findFirst().orElseThrow();
    final String value =
        (nucleus == LaboratoryAnalysis.Nucleus.H1 && a.element().atomicNumber() != 1 ? "H@" : "")
            + atom.value();
    final var values =
        new LinkedHashSet<>(
            row.get(column).isBlank() ? List.<String>of() : List.of(row.get(column).split(";")));
    if (!values.add(value)) values.remove(value);
    row.set(column, String.join(";", values));
    this.structure.show(new LaboratoryMoleculePlacement().place(this.snapshot, 0, 0), atom);
  }

  private Entry prepare() {
    if (this.conflict || !this.editor.document().equals(this.expected))
      throw new IllegalStateException(
          "Опыт изменился после начала ввода. Сохраните черновик либо очистите ввод и начните запись на текущей версии.");
    if (this.snapshot == null || this.component.getValue() == null)
      throw new IllegalArgumentException("Выберите соединение.");
    if (this.origin.getValue() == null)
      throw new IllegalArgumentException("Укажите происхождение данных.");
    final Data data =
        switch (this.method.getValue()) {
          case CORRELATIONS ->
              new Correlations(
                  this.experiment2D.getValue(),
                  LaboratoryAnalysis.Nucleus.H1,
                  this.yNucleus.getValue(),
                  f("solvent").getText(),
                  this.correlationValues(),
                  this.spectrum2D);
          case MULTIPLETS ->
              new Multiplets(
                  this.xNucleus.getValue(),
                  num(f("frequency").getText()),
                  this.multipletValues(),
                  this.multipletAnalysisId);
          case QNMR ->
              new QuantitativeNmr(
                  f("standard").getText(),
                  num(f("sampleIntegral").getText()),
                  integer("sampleNuclei"),
                  num(f("sampleMolarMass").getText()),
                  quantity("sampleMass", false),
                  num(f("standardIntegral").getText()),
                  integer("standardNuclei"),
                  num(f("standardMolarMass").getText()),
                  quantity("standardMass", false),
                  num(f("standardPurity").getText()),
                  this.preparation.getText());
          case CHROM -> {
            final var rows =
                this.peaks.getItems().stream()
                    .map(
                        r ->
                            new ChromPeak(
                                r.id,
                                r.get(0),
                                num(r.get(1)),
                                num(r.get(2)),
                                num(r.get(3)),
                                num(r.get(4)),
                                bool(r.get(5)),
                                r.get(6)))
                    .toList();
            final MassCalibration cal =
                this.calibration.getItems().isEmpty()
                    ? null
                    : new MassCalibration(
                        this.calibration.getItems().stream()
                            .map(
                                r ->
                                    new CalibrationPoint(
                                        new LaboratoryQuantity(
                                            num(r.get(0)),
                                            this.units.get("calibration").getValue()),
                                        num(r.get(1))))
                            .toList(),
                        f("calibrationSource").getText());
            String peak = null;
            if (!f("quantifiedPeak").getText().isBlank()) {
              final int index = integer("quantifiedPeak") - 1;
              if (index < 0 || index >= rows.size())
                throw new IllegalArgumentException(
                    "Номер определяемого пика должен соответствовать строке таблицы.");
              peak = rows.get(index).id();
            }
            yield new Chromatogram(
                f("technique").getText(),
                f("detector").getText(),
                this.trace,
                rows,
                cal,
                peak,
                quantity("representedMass", true),
                optional(f("reportedPurity").getText()),
                f("puritySource").getText(),
                this.preparation.getText());
          }
          case DOSSIER ->
              new Dossier(
                  selected(this.analysisLinks),
                  selected(this.evidenceLinks),
                  num(f("tolerance").getText()),
                  this.findings.getItems().stream()
                      .map(
                          r ->
                              new Finding(
                                  r.get(0),
                                  LaboratoryEvidenceService.severity(r.get(1)),
                                  r.get(2),
                                  bool(r.get(3))))
                      .toList(),
                  this.conclusion.getValue(),
                  this.conclusionText.getText());
        };
    if (data instanceof Correlations c && c.correlations().isEmpty() && c.spectrum() == null
        || data instanceof Multiplets m && m.multiplets().isEmpty()
        || data instanceof Chromatogram c && c.peaks().isEmpty())
      throw new IllegalArgumentException("Добавьте хотя бы одну строку наблюдений.");
    final var entry =
        new Entry(
            this.draftId,
            this.component.getValue().id(),
            this.sampleId(),
            f("title").getText().isBlank() ? data.method() : f("title").getText(),
            Instant.now(),
            this.origin.getValue(),
            this.source.getText(),
            this.note.getText(),
            LaboratoryAnalysisSummary.snapshot(this.snapshot, this.draftId),
            selected(this.attachments),
            this.correctsId,
            data);
    final var next = this.expected.experiment().analyticalEvidence().append(entry);
    next.validateLinks(
        this.expected.experiment().samples(),
        this.expected.experiment().analyses(),
        this.expected.experiment().attachments());
    LaboratoryEvidenceService.diagnostics(entry);
    return entry;
  }

  private static void visible(final Node node, final boolean value) {
    node.setVisible(value);
    node.setManaged(value);
  }

  private void clearResult(final boolean changed) {
    this.result.clear();
    this.plot.clear();
    this.multipletPreview.clear();
    visible(this.multipletPreview, false);
    this.resultCard.getChildren().clear();
    visible(this.resultCard, false);
    this.dossierCard.getChildren().clear();
    visible(this.emptyResult, true);
    visible(this.dossierCard, false);
    visible(this.graphTools, false);
    visible(this.plot, false);
    visible(this.resultDetails, false);
    if (this.reviewApply != null) visible(this.reviewApply, false);
    this.resultHint.setText(
        changed
            ? "Ввод изменён. Обновите результат, чтобы увидеть расчёты и проверки текущих данных."
            : this.method.getValue() == Method.DOSSIER
                ? "Досье ещё не подготовлено. Проверьте выбранные анализы и укажите происхождение данных на вкладке «Данные», затем рассчитайте сводку."
                : "Результат ещё не рассчитан. Заполните данные на вкладке «Данные» и откройте предпросмотр.");
  }

  private void preview() {
    this.prepared = null;
    this.apply.setDisable(true);
    this.clearResult(false);
    this.plot.onPick(
        i -> {
          final var table =
              this.method.getValue() == Method.CORRELATIONS
                  ? this.correlations
                  : this.method.getValue() == Method.MULTIPLETS ? this.multiplets : this.peaks;
          if (i < table.getItems().size()) {
            table.getSelectionModel().select(i);
            table.scrollTo(i);
            this.tabs.getSelectionModel().select(this.inputTab);
          }
        });
    try {
      this.prepared = this.prepare();
    } catch (final RuntimeException invalid) {
      this.resultHint.setText(
          "Для расчёта: " + message(invalid) + " Вернитесь к вводу и дополните данные.");
      throw invalid;
    }
    this.show(this.prepared);
    this.apply.setDisable(false);
    this.status.setText("");
    this.feedback.setText(
        "Результат рассчитан по введённым данным. Для сохранения в опыте нажмите «Добавить проверенную запись».");
  }

  private void show(final Entry entry) {
    if (this.comparison != null) {
      this.comparison.dispose();
      this.comparison = null;
      this.setCenter(this.tabs);
    }
    final boolean dossier = entry.data() instanceof Dossier;
    final var context =
        entry == this.prepared ? this.expected.experiment() : this.editor.document().experiment();
    this.result.setText(
        dossier
            ? LaboratoryEvidenceService.dossierSummary(context, entry)
            : LaboratoryEvidenceService.description(entry));
    this.result.setPrefRowCount(dossier ? 24 : 15);
    this.result.positionCaret(0);
    visible(this.emptyResult, false);
    this.resultCard
        .getChildren()
        .setAll(new LaboratoryEvidenceResultCard(context, entry, entry == this.prepared));
    visible(this.resultCard, true);
    visible(this.dossierCard, dossier);
    this.dossierCard.getChildren().clear();
    if (dossier)
      this.dossierCard.getChildren().add(new LaboratoryEvidenceDossierCard(context, entry));
    this.resultDetails.setText(
        dossier ? "Подробный отчёт и исходные сведения" : "Подробный отчёт и исходные данные");
    this.resultDetails.setExpanded(false);
    visible(this.resultDetails, true);
    final boolean linked = entry.data() instanceof Multiplets m && m.analysisId() != null;
    visible(this.multipletPreview, linked);
    if (linked) {
      final var original = LaboratoryEvidenceService.exactSource(context, entry).orElseThrow();
      this.multipletPreview.show(original, (Multiplets) entry.data(), null);
    } else this.multipletPreview.clear();
    final boolean graph =
        !linked
            && (entry.data() instanceof Correlations c
                    && (!c.correlations().isEmpty() || c.spectrum() != null)
                || entry.data() instanceof Multiplets m && !m.multiplets().isEmpty()
                || entry.data() instanceof Chromatogram c
                    && (!c.trace().isEmpty() || !c.peaks().isEmpty()));
    visible(this.plot, graph);
    visible(
        this.graphTools,
        graph && !(entry.data() instanceof Correlations c && c.spectrum() != null));
    if (graph) this.plot.show(entry.data());
    else this.plot.clear();
    if (entry != this.prepared) this.plot.onPick(i -> {});
    visible(this.reviewApply, entry == this.prepared);
    this.tabs.getSelectionModel().select(this.previewTab);
  }

  private void apply() {
    if (this.prepared == null)
      throw new IllegalStateException("Сначала проверьте введённые данные.");
    final Entry recorded = this.prepared;
    this.editor.analyticalEvidence(this.expected, recorded);
    this.expected = this.editor.document();
    this.prepared = null;
    this.applied = true;
    this.dirty = false;
    this.apply.setDisable(true);
    this.form.setDisable(true);
    this.check.setDisable(true);
    this.refreshHistory();
    this.show(recorded);
    this.status.setText(
        "Аналитическая запись добавлена. Сохраните опыт; для следующей записи нажмите «Новая запись».");
  }

  private void refreshHistory() {
    this.history
        .getItems()
        .setAll(
            this.allHistory.isSelected()
                ? this.editor.document().experiment().analyticalEvidence().entries()
                : this.editor.document().experiment().analyticalEvidence().latest());
  }

  private void showHistory() {
    final var entry = this.history.getSelectionModel().getSelectedItem();
    if (entry == null) throw new IllegalArgumentException("Выберите сохранённую запись.");
    this.show(entry);
    this.plot.onPick(i -> {});
  }

  private static List<String> selected(final Map<String, CheckBox> values) {
    return values.entrySet().stream()
        .filter(e -> e.getValue().isSelected())
        .map(Map.Entry::getKey)
        .toList();
  }

  private static BigDecimal num(final String value) {
    try {
      return new BigDecimal(value.strip().replace(',', '.'));
    } catch (final NumberFormatException error) {
      throw new IllegalArgumentException("Введите число: «" + value + "».");
    }
  }

  private static BigDecimal optional(final String value) {
    return value.isBlank() ? null : num(value);
  }

  private int integer(final String key) {
    try {
      return num(f(key).getText()).intValueExact();
    } catch (final ArithmeticException error) {
      throw new IllegalArgumentException("Введите целое число в поле «" + key + "».");
    }
  }

  private static List<BigDecimal> numbers(final String value) {
    return value.isBlank()
        ? List.of()
        : Arrays.stream(value.split(";", -1)).map(LaboratoryEvidencePage::num).toList();
  }

  private LaboratoryQuantity quantity(final String key, final boolean nullable) {
    return nullable && f(key).getText().isBlank()
        ? null
        : new LaboratoryQuantity(num(f(key).getText()), this.units.get(key).getValue());
  }

  private static boolean bool(final String value) {
    return switch (value.strip().toLowerCase(Locale.ROOT)) {
      case "да", "true", "1" -> true;
      case "нет", "false", "0" -> false;
      default -> throw new IllegalArgumentException("Укажите да или нет: «" + value + "».");
    };
  }

  private static String message(final Throwable error) {
    return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
  }

  private void scrollToNmr(final Node target) {
    if (this.disposed
        || this.getScene() == null
        || !target.isVisible()
        || target.getParent() == null) return;
    this.tabs.getSelectionModel().select(this.inputTab);
    this.form.applyCss();
    this.form.layout();
    this.formScroll.applyCss();
    this.formScroll.layout();
    final double extent = this.form.getHeight() - this.formScroll.getViewportBounds().getHeight();
    if (extent > 0) {
      final double y = this.form.sceneToLocal(target.localToScene(0, 0)).getY();
      this.formScroll.setVvalue(Math.max(0, Math.min(1, (y - 8) / extent)));
    }
  }

  private void spectralAction(final Runnable action) {
    try {
      action.run();
    } catch (final RuntimeException invalid) {
      this.status.setText(message(invalid));
    }
  }

  private Node spectrumInput() {
    final var more = new MenuButton("Спектр…");
    more.getItems()
        .setAll(
            menu(
                "Убрать спектр, оставить таблицу",
                "spectrum-remove",
                () -> {
                  this.spectrum2D = null;
                  this.changed();
                }));
    final var help =
        details(
            "Формат обработанного 2D спектра",
            note(
                "CSV или TSV: первая строка — ppm оси X, первый столбец — ppm оси Y; левую верхнюю ячейку оставьте пустой или укажите ppm. Остальные ячейки — интенсивности полного прямоугольника. Оси можно записать в любом направлении; пропуски не заполняются автоматически. Для таблицы выберите ядро второй оси выше. Это импорт обработанных данных, не преобразование сырого 2D FID."));
    return new VBox(
        7,
        new FlowPane(
            8,
            6,
            button("Открыть 2D спектр…", "spectrum-import", this::importSpectrum),
            button("К таблице ↓", "correlation-table", () -> this.scrollToNmr(this.correlations)),
            more),
        this.spectrumInfo,
        this.spectrumView,
        help);
  }

  private void importSpectrum() {
    if (this.running) throw new IllegalStateException("Дождитесь текущей загрузки.");
    final var chooser = new FileChooser();
    chooser.setTitle("Обработанный 2D ЯМР: числовая матрица");
    chooser
        .getExtensionFilters()
        .add(
            new FileChooser.ExtensionFilter(
                "Матрица интенсивностей UTF-8", "*.csv", "*.tsv", "*.txt"));
    final var selected = chooser.showOpenDialog(this.owner);
    if (selected != null) this.loadSpectrum(selected.toPath());
  }

  private void loadSpectrum(final Path path) {
    if (this.running) throw new IllegalStateException("Дождитесь текущей загрузки.");
    final var nucleus = this.yNucleus.getValue();
    this.spectrumCancelRequested = false;
    this.busy(true);
    this.form.setDisable(true);
    this.check.setDisable(true);
    visible(this.cancelSpectrumImport, true);
    this.spectrumLoading =
        Thread.ofVirtual()
            .unstarted(
                () -> {
                  try {
                    final var loaded =
                        LaboratoryNmr2DReader.read(
                            path,
                            LaboratoryAnalysis.Nucleus.H1,
                            nucleus,
                            "Обработанный 2D спектр; тип эксперимента и происхождение указываются пользователем.",
                            () ->
                                Thread.currentThread().isInterrupted()
                                    || this.spectrumCancelRequested);
                    Platform.runLater(
                        () -> {
                          if (this.disposed) return;
                          this.finishSpectrumImport();
                          if (this.spectrumCancelRequested) {
                            this.status.setText(
                                "Загрузка спектра отменена; прежние данные сохранены.");
                            return;
                          }
                          try {
                            this.acceptSpectrum(loaded);
                          } catch (final RuntimeException invalid) {
                            this.status.setText(message(invalid));
                          }
                        });
                  } catch (final Exception error) {
                    Platform.runLater(
                        () -> {
                          if (!this.disposed) {
                            this.finishSpectrumImport();
                            this.status.setText(
                                this.spectrumCancelRequested
                                        || error
                                            instanceof java.util.concurrent.CancellationException
                                    ? "Загрузка спектра отменена; прежние данные сохранены."
                                    : message(error));
                          }
                        });
                  }
                });
    this.spectrumLoading.start();
  }

  private void finishSpectrumImport() {
    this.spectrumLoading = null;
    this.busy(false);
    this.form.setDisable(this.applied);
    this.check.setDisable(this.applied);
    visible(this.cancelSpectrumImport, false);
  }

  private void acceptSpectrum(final NmrSpectrum2D value) {
    if (value.xNucleus() != LaboratoryAnalysis.Nucleus.H1)
      throw new IllegalArgumentException("Для этого вида корреляций первая ось должна быть ¹H.");
    this.changing = true;
    try {
      this.spectrum2D = value;
      this.yNucleus.setValue(value.yNucleus());
      this.source.appendText(
          (this.source.getText().isBlank() ? "" : "\n")
              + value.sourceName()
              + " · "
              + value.format()
              + "\n"
              + String.join(
                  "\n",
                  value.sourceHashes().entrySet().stream()
                      .sorted(Map.Entry.comparingByKey())
                      .map(e -> e.getKey() + " · SHA-256 " + e.getValue())
                      .toList()));
    } finally {
      this.changing = false;
    }
    this.changed();
    this.status.setText(
        "Спектр загружен. Выберите точку и нажмите «Добавить точку»; происхождение и тип эксперимента задаются отдельно.");
  }

  private List<Correlation> correlationValues() {
    return this.correlations.getItems().stream()
        .map(
            r ->
                new Correlation(
                    r.id,
                    num(r.get(0)),
                    num(r.get(1)),
                    optional(r.get(2)),
                    LaboratoryEvidenceService.assignments(r.get(3)),
                    LaboratoryEvidenceService.assignments(r.get(4)),
                    r.get(5)))
        .toList();
  }

  private List<Multiplet> multipletValues() {
    return this.multiplets.getItems().stream().map(this::multipletValue).toList();
  }

  private Multiplet multipletValue(final LaboratoryEvidenceTable.Row row) {
    return new Multiplet(
        row.id,
        num(row.get(0)),
        optional(row.get(1)),
        numbers(row.get(3)),
        numbers(row.get(4)),
        LaboratoryEvidenceService.assignments(row.get(5)),
        row.get(2),
        row.get(6));
  }

  private void addCorrelationPoint(final LaboratoryNmr2DPane.Selection point) {
    if (this.applied || this.running || this.spectrum2D == null) return;
    final var duplicate =
        this.correlations.getItems().stream()
            .filter(
                r -> {
                  try {
                    return num(r.get(0)).compareTo(BigDecimal.valueOf(point.xPpm())) == 0
                        && num(r.get(1)).compareTo(BigDecimal.valueOf(point.yPpm())) == 0;
                  } catch (final RuntimeException invalid) {
                    return false;
                  }
                })
            .findFirst();
    final var row =
        duplicate.orElseGet(
            () ->
                this.correlations.add(
                    "correlation_" + UUID.randomUUID(),
                    List.of(
                        value(BigDecimal.valueOf(point.xPpm())),
                        value(BigDecimal.valueOf(point.yPpm())),
                        value(BigDecimal.valueOf(point.intensity())),
                        "",
                        "",
                        "Выбрано вручную на исходной сетке")));
    this.correlations.getSelectionModel().select(row);
    this.correlations.scrollTo(row);
    this.status.setText(
        duplicate.isPresent()
            ? "Эта точка уже в таблице; выбрана существующая строка."
            : "Корреляция добавлена без назначения атомам. Назначение доступно под таблицей.");
  }

  private void addMultipletLine(final String id, final BigDecimal ppm) {
    if (this.applied || this.running || this.multipletAnalysisId == null) return;
    final var row =
        this.multiplets.getItems().stream()
            .filter(r -> r.id.equals(id))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Выберите мультиплет в таблице."));
    final var lines = new ArrayList<>(numbers(row.get(3)));
    if (lines.stream().anyMatch(v -> v.compareTo(ppm) == 0)) {
      this.status.setText("Эта линия уже отмечена в выбранном мультиплете.");
      return;
    }
    lines.add(ppm);
    lines.sort(Comparator.naturalOrder());
    row.set(3, joined(lines));
    this.status.setText(
        "Линия отмечена. Значения J вводятся отдельно; расстояние линий не превращается в константу автоматически.");
  }

  private void updateNmrViews() {
    if (this.method.getValue() == Method.CORRELATIONS) {
      visible(this.spectrumView, this.spectrum2D != null);
      this.spectrumInfo.setText(
          this.spectrum2D == null
              ? "Можно работать с таблицей корреляций или открыть свой спектр."
              : this.spectrum2D.sourceName()
                  + " · "
                  + this.spectrum2D.width()
                  + " × "
                  + this.spectrum2D.height()
                  + " точек");
      if (this.spectrum2D == null) this.spectrumView.clear();
      else {
        List<Correlation> marks;
        try {
          marks = this.correlationValues();
        } catch (final RuntimeException incomplete) {
          marks = List.of();
        }
        this.spectrumView.show(this.spectrum2D, marks);
      }
    }
    if (this.method.getValue() == Method.MULTIPLETS) {
      visible(this.multipletView, this.multipletAnalysisId != null);
      if (this.multipletAnalysisId == null) {
        this.multipletView.clear();
        visible(this.multipletSourceInfo, true);
        this.multipletSourceInfo.setText(
            "Можно связать сохранённый 1D спектр и выбрать линии на нём.");
        return;
      }
      try {
        final var source =
            LaboratoryEvidenceService.exactSource(
                    this.expected.experiment(),
                    this.component.getValue().id(),
                    this.sampleId(),
                    this.snapshot,
                    new Multiplets(
                        this.xNucleus.getValue(),
                        num(f("frequency").getText()),
                        List.of(),
                        this.multipletAnalysisId))
                .orElseThrow();
        final var valid = new ArrayList<Multiplet>();
        for (final var row : this.multiplets.getItems()) {
          try {
            valid.add(this.multipletValue(row));
          } catch (final RuntimeException incomplete) {
          }
        }
        final var row = this.multiplets.getSelectionModel().getSelectedItem();
        this.multipletView.show(
            source,
            new Multiplets(
                this.xNucleus.getValue(),
                num(f("frequency").getText()),
                valid,
                this.multipletAnalysisId),
            row == null ? null : row.id);
        this.multipletSourceInfo.setText("");
        visible(this.multipletSourceInfo, false);
      } catch (final RuntimeException mismatch) {
        this.multipletView.clear();
        visible(this.multipletSourceInfo, true);
        this.multipletSourceInfo.setText(
            "Связь с исходным спектром требует проверки: " + message(mismatch));
      }
    }
  }

  private void importTable(final LaboratoryEvidenceTable table) {
    this.readFile(
        "Таблица с заголовком: " + String.join(", ", table.header()),
        text -> {
          table.load(LaboratoryEvidenceService.table(text, table.header()));
          this.changed();
        });
  }

  private void importTrace() {
    this.readFile("Хроматограмма: time_min,response", this::loadTrace);
  }

  private void loadTrace(final String text) {
    final var points =
        LaboratoryEvidenceService.table(text, List.of("time_min", "response")).stream()
            .map(r -> new ChromPoint(num(r.get(0)), num(r.get(1))))
            .toList();
    new Chromatogram("Проверка", "Проверка", points, List.of(), null, null, null, null, "", "");
    this.trace = points;
    this.traceInfo.setText("Загружено точек: " + points.size());
    if (this.integrationPane != null) this.integrationPane.setExpanded(true);
    this.updateTraceView();
    this.changed();
    Platform.runLater(
        () -> {
          if (this.trace == points) this.scrollToTrace();
        });
  }

  private void readFile(final String title, final java.util.function.Consumer<String> accept) {
    if (this.running) throw new IllegalStateException("Дождитесь загрузки.");
    final var chooser = new FileChooser();
    chooser.setTitle(title);
    chooser
        .getExtensionFilters()
        .add(new FileChooser.ExtensionFilter("Числовые таблицы UTF-8", "*.csv", "*.tsv", "*.txt"));
    final var file = chooser.showOpenDialog(this.owner);
    if (file == null) return;
    this.busy(true);
    this.form.setDisable(true);
    Thread.startVirtualThread(
        () -> {
          try {
            final Path path = file.toPath();
            if (Files.size(path) > 16 * 1024 * 1024)
              throw new IllegalArgumentException("Файл больше 16 МиБ.");
            final byte[] bytes = Files.readAllBytes(path);
            if (bytes.length > 16 * 1024 * 1024)
              throw new IllegalArgumentException("Файл больше 16 МиБ.");
            final String text =
                java.nio.charset.StandardCharsets.UTF_8
                    .newDecoder()
                    .decode(java.nio.ByteBuffer.wrap(bytes))
                    .toString();
            final String hash =
                HexFormat.of()
                    .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
            Platform.runLater(
                () -> {
                  if (this.disposed) return;
                  this.busy(false);
                  this.form.setDisable(false);
                  try {
                    accept.accept(text);
                    this.source.appendText(
                        (this.source.getText().isBlank() ? "" : "\n")
                            + file.getName()
                            + " · SHA-256 "
                            + hash);
                    this.status.setText(
                        "Числовые данные загружены. Проверьте происхождение и единицы.");
                  } catch (final Exception error) {
                    this.status.setText(message(error));
                  }
                });
          } catch (final Exception error) {
            Platform.runLater(
                () -> {
                  if (!this.disposed) {
                    this.busy(false);
                    this.form.setDisable(false);
                    this.status.setText(message(error));
                  }
                });
          }
        });
  }

  private void integrate() {
    if (this.trace.isEmpty())
      throw new IllegalArgumentException("Загрузите числовую хроматограмму.");
    final var from = num(f("from").getText());
    final var to = num(f("to").getText());
    final var value = LaboratoryEvidenceService.integrate(this.trace, from, to);
    this.peaks.add(
        "peak_" + UUID.randomUUID(),
        List.of(
            f("peakLabel").getText().isBlank()
                ? "Пик " + (this.peaks.getItems().size() + 1)
                : f("peakLabel").getText(),
            value.retentionMin().toPlainString(),
            from.toPlainString(),
            to.toPlainString(),
            value.area().toPlainString(),
            "да",
            ""));
    this.changed();
    this.status.setText("Область интегрирована относительно прямой между границами.");
  }

  private LaboratoryAnalysis chooseNmr() {
    return this.chooseNmr(false);
  }

  private LaboratoryAnalysis chooseNmr(final boolean requireTrace) {
    final String cid = this.component.getValue() == null ? "" : this.component.getValue().id();
    final var candidates =
        this.expected.experiment().analyses().stream()
            .filter(
                a ->
                    a.componentId().equals(cid)
                        && Objects.equals(a.sampleId(), this.sampleId())
                        && a.data() instanceof LaboratoryAnalysis.Nmr n
                        && (!requireTrace || n.trace() != null)
                        && this.snapshot != null
                        && LaboratoryMaterialProperties.signature(a.molecule())
                            .equals(LaboratoryMaterialProperties.signature(this.snapshot)))
            .sorted(Comparator.comparing(LaboratoryAnalysis::recordedAt).reversed())
            .toList();
    if (candidates.isEmpty())
      throw new IllegalArgumentException(
          requireTrace
              ? "Для выбранного образца нет сохранённого 1D ЯМР с числовой трассой."
              : "Для выбранного образца нет сохранённого 1D ЯМР.");
    if (candidates.size() == 1) return candidates.getFirst();
    final var corrected = new HashSet<String>();
    this.expected
        .experiment()
        .analyses()
        .forEach(
            a -> {
              if (a.correctsId() != null) corrected.add(a.correctsId());
            });
    final var choices =
        candidates.stream()
            .map(
                a ->
                    new Choice(
                        a.id(),
                        a.title()
                            + " · "
                            + a.origin()
                            + " · "
                            + DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                                .withZone(ZoneId.systemDefault())
                                .format(a.recordedAt())
                            + (corrected.contains(a.id()) ? " · предыдущая версия" : "")))
            .toList();
    final var dialog = new ChoiceDialog<>(choices.getFirst(), choices);
    dialog.initOwner(this.owner);
    dialog.setTitle("Исходная запись ЯМР");
    dialog.setHeaderText("Выберите точную версию записи того же образца");
    LaboratoryDialogStyle.apply(dialog, this.owner);
    return dialog
        .showAndWait()
        .flatMap(c -> candidates.stream().filter(a -> a.id().equals(c.id())).findFirst())
        .orElse(null);
  }

  private void fromNmr() {
    if (!this.multiplets.getItems().isEmpty())
      throw new IllegalArgumentException(
          "Таблица уже содержит мультиплеты. Для загрузки другой таблицы начните новую запись; связь со спектром можно выбрать отдельно.");
    final var analysis = this.chooseNmr();
    if (analysis != null) this.useNmr(analysis, true);
  }

  private void linkNmr() {
    final var analysis = this.chooseNmr(true);
    if (analysis == null) return;
    if (((LaboratoryAnalysis.Nmr) analysis.data()).trace() == null)
      throw new IllegalArgumentException(
          "В записи есть только список сигналов. Выберите 1D ЯМР с числовой трассой.");
    this.useNmr(analysis, false);
  }

  private void useNmr(final LaboratoryAnalysis analysis, final boolean copySignals) {
    final var nmr = (LaboratoryAnalysis.Nmr) analysis.data();
    this.changing = true;
    try {
      this.xNucleus.setValue(nmr.nucleus());
      f("frequency").setText(value(nmr.frequencyMHz()));
      this.multipletAnalysisId = nmr.trace() == null ? null : analysis.id();
      if (copySignals) {
        this.multiplets.getItems().clear();
        for (final var signal : nmr.signals()) {
          this.multiplets.add(
              "multiplet_" + UUID.randomUUID(),
              List.of(
                  signal.from().add(signal.to()).divide(BigDecimal.TWO).toPlainString(),
                  signal.integral() == null
                      ? ""
                      : value(signal.integral().multiply(nmr.integralScale())),
                  signal.multiplicity(),
                  "",
                  joined(signal.couplingsHz()),
                  String.join(";", signal.atoms().stream().map(a -> a.value()).toList()),
                  signal.note()));
        }
      }
      this.origin.setValue(analysis.origin());
      this.source.setText(
          "Сохранённый 1D ЯМР: "
              + analysis.id()
              + " · "
              + analysis.title()
              + "\n"
              + analysis.source());
    } finally {
      this.changing = false;
    }
    this.changed();
  }

  private void integrals() {
    final var analysis = this.chooseNmr();
    if (analysis == null) return;
    final var nmr = (LaboratoryAnalysis.Nmr) analysis.data();
    final var signals =
        nmr.signals().stream()
            .filter(s -> s.integral() != null && s.integral().signum() > 0)
            .toList();
    if (signals.size() < 2)
      throw new IllegalArgumentException("Для выбора нужны два положительных интеграла.");
    final List<Choice> options = new ArrayList<>();
    for (int i = 0; i < signals.size(); i++) {
      final var s = signals.get(i);
      options.add(
          new Choice(
              Integer.toString(i),
              value(s.from())
                  + "–"
                  + value(s.to())
                  + " ppm · интеграл "
                  + value(s.integral().multiply(nmr.integralScale()))));
    }
    final var first = new ChoiceDialog<>(options.getFirst(), options);
    first.initOwner(this.owner);
    first.setHeaderText("Интеграл аналита");
    LaboratoryDialogStyle.apply(first, this.owner);
    final var a = first.showAndWait();
    if (a.isEmpty()) return;
    final var remaining = options.stream().filter(c -> !c.id().equals(a.get().id())).toList();
    final var second = new ChoiceDialog<>(remaining.getFirst(), remaining);
    second.initOwner(this.owner);
    second.setHeaderText("Интеграл внутреннего стандарта");
    LaboratoryDialogStyle.apply(second, this.owner);
    final var b = second.showAndWait();
    if (b.isEmpty()) return;
    f("sampleIntegral")
        .setText(
            value(
                signals
                    .get(Integer.parseInt(a.get().id()))
                    .integral()
                    .multiply(nmr.integralScale())));
    f("standardIntegral")
        .setText(
            value(
                signals
                    .get(Integer.parseInt(b.get().id()))
                    .integral()
                    .multiply(nmr.integralScale())));
    this.origin.setValue(analysis.origin());
    this.source.setText(
        "Интегралы из 1D ЯМР: "
            + analysis.id()
            + " · "
            + analysis.title()
            + "\n"
            + analysis.source());
    this.changed();
  }

  private void correct() {
    final var old = this.history.getSelectionModel().getSelectedItem();
    if (old == null) throw new IllegalArgumentException("Выберите запись для исправления.");
    if (this.pending())
      throw new IllegalStateException("Добавьте либо очистите текущий ввод перед исправлением.");
    if (this.editor.document().experiment().analyticalEvidence().latest().stream()
        .noneMatch(e -> e.id().equals(old.id())))
      throw new IllegalArgumentException("Выберите последнюю запись цепочки исправлений.");
    this.begin(method(old.data()));
    this.changing = true;
    this.correctsId = old.id();
    this.chooseIdentity(old.componentId(), old.sampleId(), old.molecule());
    this.component.setDisable(true);
    this.sample.setDisable(true);
    this.method.setDisable(true);
    this.origin.setValue(old.origin());
    f("title").setText(old.title());
    this.source.setText(old.source());
    this.note.setText(old.note());
    this.restoreLinks(this.attachments, old.attachmentIds());
    this.populate(old.data());
    this.revealDetails();
    this.updateTraceView();
    this.updateIdentityContext();
    this.changing = false;
    this.dirty = true;
    this.applied = false;
    this.feedback.setText("Исправление будет новой записью; исходные данные сохранятся.");
    this.updateAssignmentGraph();
  }

  private static Method method(final Data data) {
    return switch (data) {
      case Correlations c -> Method.CORRELATIONS;
      case Multiplets m -> Method.MULTIPLETS;
      case QuantitativeNmr q -> Method.QNMR;
      case Chromatogram c -> Method.CHROM;
      case Dossier d -> Method.DOSSIER;
    };
  }

  private void chooseIdentity(final String cid, final String sid, final Molecule molecule) {
    if (this.component.getItems().stream().noneMatch(c -> c.id().equals(cid)))
      this.component
          .getItems()
          .add(new Choice(cid, molecule.displayName() + " · сохранённая структура"));
    this.component.setValue(
        this.component.getItems().stream()
            .filter(c -> c.id().equals(cid))
            .findFirst()
            .orElseThrow());
    this.sample.getItems().setAll(new Choice("", "Без отдельного образца"));
    this.expected.experiment().samples().stream()
        .filter(s -> s.componentId().equals(cid))
        .forEach(s -> this.sample.getItems().add(new Choice(s.id(), s.code())));
    this.sample.setValue(
        this.sample.getItems().stream()
            .filter(s -> Objects.equals(s.id(), sid == null ? "" : sid))
            .findFirst()
            .orElseThrow());
    this.snapshot = molecule;
    this.structure.show(new LaboratoryMoleculePlacement().place(this.snapshot, 0, 0), null);
    this.references();
  }

  private void populate(final Data data) {
    switch (data) {
      case Correlations c -> {
        this.spectrum2D = c.spectrum();
        this.experiment2D.setValue(c.experiment());
        this.yNucleus.setValue(c.yNucleus());
        f("solvent").setText(c.solvent());
        c.correlations()
            .forEach(
                r ->
                    this.correlations.add(
                        r.id(),
                        List.of(
                            value(r.xPpm()),
                            value(r.yPpm()),
                            value(r.intensity()),
                            LaboratoryEvidenceService.assignments(r.xAtoms()),
                            LaboratoryEvidenceService.assignments(r.yAtoms()),
                            r.note())));
      }
      case Multiplets m -> {
        this.multipletAnalysisId = m.analysisId();
        this.xNucleus.setValue(m.nucleus());
        f("frequency").setText(value(m.frequencyMHz()));
        m.multiplets()
            .forEach(
                r ->
                    this.multiplets.add(
                        r.id(),
                        List.of(
                            value(r.centerPpm()),
                            value(r.integral()),
                            r.label(),
                            joined(r.linesPpm()),
                            joined(r.couplingsHz()),
                            LaboratoryEvidenceService.assignments(r.atoms()),
                            r.note())));
      }
      case QuantitativeNmr q -> {
        f("standard").setText(q.standardName());
        f("sampleIntegral").setText(value(q.sampleIntegral()));
        f("sampleNuclei").setText(Integer.toString(q.sampleNuclei()));
        f("sampleMolarMass").setText(value(q.sampleMolarMass()));
        this.setQuantity("sampleMass", q.sampleMass());
        f("standardIntegral").setText(value(q.standardIntegral()));
        f("standardNuclei").setText(Integer.toString(q.standardNuclei()));
        f("standardMolarMass").setText(value(q.standardMolarMass()));
        this.setQuantity("standardMass", q.standardMass());
        f("standardPurity").setText(value(q.standardPurityPercent()));
        this.preparation.setText(q.preparation());
      }
      case Chromatogram c -> {
        f("technique").setText(c.technique());
        f("detector").setText(c.detector());
        this.trace = c.trace();
        this.traceInfo.setText("Точек хроматограммы: " + this.trace.size());
        c.peaks()
            .forEach(
                p ->
                    this.peaks.add(
                        p.id(),
                        List.of(
                            p.label(),
                            value(p.retentionMin()),
                            value(p.fromMin()),
                            value(p.toMin()),
                            value(p.area()),
                            p.included() ? "да" : "нет",
                            p.exclusionReason())));
        if (c.calibration() != null) {
          final var unit = c.calibration().points().getFirst().mass().unit();
          this.units.get("calibration").setValue(unit);
          for (final var p : c.calibration().points()) {
            this.calibration.add(
                "cal_" + UUID.randomUUID(),
                List.of(
                    value(
                        p.mass()
                            .baseValue()
                            .divide(unit.factor(), java.math.MathContext.DECIMAL128)),
                    value(p.area())));
          }
          f("calibrationSource").setText(c.calibration().source());
        }
        for (int i = 0; i < c.peaks().size(); i++) {
          if (c.peaks().get(i).id().equals(c.quantifiedPeakId()))
            f("quantifiedPeak").setText(Integer.toString(i + 1));
        }
        this.setQuantity("representedMass", c.representedSampleMass());
        f("reportedPurity").setText(value(c.reportedPurityPercent()));
        f("puritySource").setText(c.reportedPuritySource());
        this.preparation.setText(c.preparation());
      }
      case Dossier d -> {
        this.ensureReferences(d.analysisIds(), d.evidenceIds());
        this.restoreLinks(this.analysisLinks, d.analysisIds());
        this.restoreLinks(this.evidenceLinks, d.evidenceIds());
        f("tolerance").setText(value(d.massTolerancePpm()));
        d.findings()
            .forEach(
                f ->
                    this.findings.add(
                        "finding_" + UUID.randomUUID(),
                        List.of(
                            f.category(),
                            f.severity().name(),
                            f.text(),
                            f.resolved() ? "да" : "нет")));
        this.conclusion.setValue(d.conclusion());
        this.conclusionText.setText(d.conclusionText());
      }
    }
  }

  private void setQuantity(final String key, final LaboratoryQuantity q) {
    f(key).setText(q == null ? "" : value(q.value()));
    if (q != null) this.units.get(key).setValue(q.unit());
  }

  private void ensureReferences(final List<String> oldAnalyses, final List<String> oldEvidence) {
    for (final String id : oldAnalyses) {
      if (!this.analysisLinks.containsKey(id)) {
        final var a =
            this.expected.experiment().analyses().stream()
                .filter(v -> v.id().equals(id))
                .findFirst()
                .orElseThrow();
        final var cb =
            this.referenceChoice(
                a.title(),
                a.data().method(),
                a.sampleId(),
                a.recordedAt(),
                a.origin(),
                a.source(),
                "LEGACY",
                id,
                true,
                false);
        this.analysisLinks.put(id, cb);
        this.references.getChildren().add(cb);
      }
    }
    for (final String id : oldEvidence) {
      if (!this.evidenceLinks.containsKey(id)) {
        final var a =
            this.expected.experiment().analyticalEvidence().entries().stream()
                .filter(v -> v.id().equals(id))
                .findFirst()
                .orElseThrow();
        final var cb =
            this.referenceChoice(
                a.title(),
                a.data().method(),
                a.sampleId(),
                a.recordedAt(),
                a.origin(),
                a.source(),
                "EVIDENCE",
                id,
                true,
                false);
        this.evidenceLinks.put(id, cb);
        this.references.getChildren().add(cb);
      }
    }
  }

  private void restoreLinks(final Map<String, CheckBox> map, final List<String> selected) {
    map.forEach((id, cb) -> cb.setSelected(selected.contains(id)));
  }

  void draft(final Properties values, final boolean restore) {
    final String p = "evidence.";
    if (!restore) {
      if (!this.pending()) return;
      values.setProperty(p + "source", new LaboratoryFileCodec().write(this.expected));
      values.setProperty(p + "method", this.method.getValue().name());
      values.setProperty(p + "id", this.draftId);
      values.setProperty(p + "corrects", Objects.toString(this.correctsId, ""));
      values.setProperty(
          p + "component", this.component.getValue() == null ? "" : this.component.getValue().id());
      values.setProperty(p + "sample", Objects.toString(this.sampleId(), ""));
      values.setProperty(
          p + "origin", this.origin.getValue() == null ? "" : this.origin.getValue().name());
      values.setProperty(p + "experiment2D", this.experiment2D.getValue().name());
      values.setProperty(p + "xNucleus", this.xNucleus.getValue().name());
      values.setProperty(p + "yNucleus", this.yNucleus.getValue().name());
      values.setProperty(p + "conclusion", this.conclusion.getValue().name());
      this.fields.forEach((k, v) -> values.setProperty(p + "field." + k, v.getText()));
      this.units.forEach((k, v) -> values.setProperty(p + "unit." + k, v.getValue().name()));
      values.setProperty(
          p + "spectrum2D",
          this.spectrum2D == null ? "" : QchemNmrSpectrum2DCodec.encode(this.spectrum2D));
      values.setProperty(p + "multipletAnalysisId", Objects.toString(this.multipletAnalysisId, ""));
      values.setProperty(p + "sourceText", this.source.getText());
      values.setProperty(p + "note", this.note.getText());
      values.setProperty(p + "preparation", this.preparation.getText());
      values.setProperty(p + "conclusionText", this.conclusionText.getText());
      values.setProperty(p + "analysisLinks", String.join(",", selected(this.analysisLinks)));
      values.setProperty(p + "evidenceLinks", String.join(",", selected(this.evidenceLinks)));
      values.setProperty(p + "attachments", String.join(",", selected(this.attachments)));
      this.tables().forEach((k, v) -> v.draft(values, p + k + ".", false));
      values.setProperty(
          p + "trace",
          String.join(
              "\n",
              this.trace.stream()
                  .map(t -> value(t.timeMin()) + "\t" + value(t.response()))
                  .toList()));
      return;
    }
    final String stored = values.getProperty(p + "source");
    if (stored == null) return;
    final var drawing = new LaboratoryFileCodec().read(stored);
    this.begin(Method.valueOf(values.getProperty(p + "method")));
    this.changing = true;
    try {
      this.expected = drawing;
      this.draftId = values.getProperty(p + "id", "evidence_" + UUID.randomUUID());
      this.correctsId = nullable(values.getProperty(p + "corrects", ""));
      final String cid = values.getProperty(p + "component", ""),
          sid = nullable(values.getProperty(p + "sample", ""));
      final Entry old =
          this.correctsId == null
              ? null
              : drawing.experiment().analyticalEvidence().entries().stream()
                  .filter(e -> e.id().equals(this.correctsId))
                  .findFirst()
                  .orElseThrow();
      this.component
          .getItems()
          .setAll(
              drawing.experiment().components().stream()
                  .map(c -> new Choice(c.id(), c.molecule().displayName()))
                  .toList());
      if (!cid.isBlank()) {
        final Molecule molecule =
            old != null
                ? old.molecule()
                : sid != null
                    ? LaboratorySampleService.sample(drawing.experiment(), sid).molecule()
                    : drawing.experiment().components().stream()
                        .filter(c -> c.id().equals(cid))
                        .findFirst()
                        .orElseThrow()
                        .molecule();
        this.chooseIdentity(cid, sid, molecule);
      } else {
        this.component.setValue(null);
        this.snapshot = null;
      }
      this.component.setDisable(old != null);
      this.sample.setDisable(old != null);
      this.method.setDisable(old != null);
      this.origin.setValue(
          values.getProperty(p + "origin", "").isBlank()
              ? null
              : LaboratoryAnalysis.Origin.valueOf(values.getProperty(p + "origin")));
      this.experiment2D.setValue(
          Experiment2D.valueOf(values.getProperty(p + "experiment2D", "HSQC")));
      this.xNucleus.setValue(
          LaboratoryAnalysis.Nucleus.valueOf(values.getProperty(p + "xNucleus", "H1")));
      this.yNucleus.setValue(
          LaboratoryAnalysis.Nucleus.valueOf(values.getProperty(p + "yNucleus", "C13")));
      this.conclusion.setValue(Conclusion.valueOf(values.getProperty(p + "conclusion", "PENDING")));
      this.fields.forEach((k, v) -> v.setText(values.getProperty(p + "field." + k, "")));
      this.units.forEach(
          (k, v) ->
              v.setValue(
                  LaboratoryQuantityUnit.valueOf(
                      values.getProperty(p + "unit." + k, "MILLIGRAM"))));
      final String stored2D = values.getProperty(p + "spectrum2D", "");
      this.spectrum2D = stored2D.isBlank() ? null : QchemNmrSpectrum2DCodec.decode(stored2D);
      this.multipletAnalysisId = nullable(values.getProperty(p + "multipletAnalysisId", ""));
      this.source.setText(values.getProperty(p + "sourceText", ""));
      this.note.setText(values.getProperty(p + "note", ""));
      this.preparation.setText(values.getProperty(p + "preparation", ""));
      this.conclusionText.setText(values.getProperty(p + "conclusionText", ""));
      this.tables().forEach((k, v) -> v.draft(values, p + k + ".", true));
      final String points = values.getProperty(p + "trace", "");
      final var loaded = new ArrayList<ChromPoint>();
      if (!points.isBlank()) {
        for (final String line : points.split("\n")) {
          final var cells = line.split("\t", -1);
          if (cells.length != 2 || loaded.size() >= 262144)
            throw new IllegalArgumentException("Повреждён черновик хроматограммы.");
          loaded.add(new ChromPoint(num(cells[0]), num(cells[1])));
        }
      }
      this.trace = List.copyOf(loaded);
      this.traceInfo.setText("Точек хроматограммы: " + this.trace.size());
      final var aa = ids(values.getProperty(p + "analysisLinks", ""));
      final var ee = ids(values.getProperty(p + "evidenceLinks", ""));
      this.ensureReferences(aa, ee);
      this.restoreLinks(this.analysisLinks, aa);
      this.restoreLinks(this.evidenceLinks, ee);
      this.attachmentBox.getChildren().clear();
      this.attachments.clear();
      for (final var a : drawing.experiment().attachments()) {
        final var cb = new CheckBox(a.name());
        cb.selectedProperty().addListener(o -> this.changed());
        this.attachments.put(a.id(), cb);
        this.attachmentBox.getChildren().add(cb);
      }
      this.restoreLinks(this.attachments, ids(values.getProperty(p + "attachments", "")));
      this.conflict = !this.editor.document().equals(drawing);
      this.dirty = true;
      this.applied = false;
      this.prepared = null;
      this.apply.setDisable(true);
      this.status.setText(
          this.conflict
              ? "Черновик восстановлен, но опыт изменился. Его нельзя применить к другой версии."
              : "Черновик восстановлен. Проверьте данные перед добавлением.");
    } finally {
      this.changing = false;
      this.revealDetails();
      this.updateAssignmentGraph();
      this.updateTraceView();
      this.updateIdentityContext();
    }
  }

  private Map<String, LaboratoryEvidenceTable> tables() {
    return Map.of(
        "correlations",
        this.correlations,
        "multiplets",
        this.multiplets,
        "peaks",
        this.peaks,
        "calibration",
        this.calibration,
        "findings",
        this.findings);
  }

  private static List<String> ids(final String value) {
    return value.isBlank() ? List.of() : List.of(value.split(",", -1));
  }

  private static String nullable(final String value) {
    return value.isBlank() ? null : value;
  }

  @Override
  void dispose() {
    this.spectrumCancelRequested = true;
    if (this.spectrumLoading != null) this.spectrumLoading.interrupt();
    this.spectrumView.dispose();
    this.multipletView.dispose();
    this.multipletPreview.dispose();
    this.plot.dispose();
    this.assignmentPlot.dispose();
    this.tracePlot.dispose();
    if (this.comparison != null) this.comparison.dispose();
    super.dispose();
  }

  @Override
  boolean canClose() {
    if (this.running) {
      this.status.setText("Дождитесь завершения загрузки.");
      return false;
    }
    if (this.pending()) {
      this.status.setText(
          "Есть незавершённая запись. Добавьте её к опыту или очистите ввод перед выходом.");
      return false;
    }
    return true;
  }

  private static String value(final BigDecimal value) {
    return value == null ? "" : value.toPlainString();
  }

  private static String joined(final List<BigDecimal> values) {
    return String.join(";", values.stream().map(LaboratoryEvidencePage::value).toList());
  }
}

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
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAnalysis.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.search.CdkMoleculeIonMass;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryFileCodec;

/** Typed observations with a recoverable raw form and append-only corrections. */
final class LaboratoryAnalysisPage extends LaboratoryComputationPage {
  private record Choice(String id, String name) {
    @Override
    public String toString() {
      return this.name;
    }
  }

  static final class Row {
    boolean spectralIntegral;
    final List<StringProperty> fields = new ArrayList<>();

    Row() {
      for (int i = 0; i < 7; i++) this.fields.add(new SimpleStringProperty(""));
    }
  }

  private final LaboratoryEditor editor;
  private final String experimentId;
  private final TabPane tabs = new TabPane();
  private LaboratorySpectrumPane spectrum;
  private final Tab spectrumTab = new Tab("Обработка спектра");
  private final Tab resultsTab = new Tab("Результаты"),
      inputTab = new Tab("Ввод анализа"),
      textTab = new Tab("Просмотр");
  private final TableView<LaboratoryAnalysis> history = new TableView<>();
  private final TableView<Row> rows = new TableView<>();
  private final ComboBox<Choice> sampleChoice = new ComboBox<>();
  private String sampleId;
  private final ComboBox<Choice> component = new ComboBox<>();
  private final ComboBox<Origin> origin = new ComboBox<>();
  private final ComboBox<Nucleus> nucleus = new ComboBox<>();
  private final ComboBox<MoleculeIonMass.Ion> ion = new ComboBox<>();
  private final Map<String, TextField> fields = new LinkedHashMap<>();
  private final TextArea source = new TextArea(),
      note = new TextArea(),
      description = new TextArea();
  private final VBox methodFields = new VBox(10),
      fileChoices = new VBox(5),
      form = new VBox(12),
      savedCard = new VBox(10);
  private final Map<String, CheckBox> attachments = new LinkedHashMap<>();
  private final Label methodLabel = note(""), feedback = note(""), savedNotes = note("");
  private final Button apply = new Button("Добавить к опыту"),
      check = new Button("Проверить и показать"),
      discard = new Button("Очистить ввод"),
      inspect = new Button("Открыть анализ"),
      toList = new Button("← К записям");
  private final LaboratoryAnalysisPlot plot = new LaboratoryAnalysisPlot(),
      savedPlot = new LaboratoryAnalysisPlot();
  private final LaboratoryAnalysisPlot.Structure structure = new LaboratoryAnalysisPlot.Structure(),
      savedStructure = new LaboratoryAnalysisPlot.Structure();
  private final HBox split = new HBox(14);
  private final ScrollPane savedScroll = new ScrollPane(this.savedCard);
  private final VBox historyBox = new VBox(8),
      previewBody = new VBox(12),
      emptyPreview = new VBox(10);
  private final Label previewHint = note(""), previewState = note("");
  private final TitledPane metadata = new TitledPane();
  private final Button previewApply = new Button("Добавить к опыту");
  private boolean dirty, changing, applied, inspecting, calculating, spectralUpdating;
  private final Label selectedSignal = note("Выберите сигнал в таблице");
  private String method = "NMR",
      draftId = UUID.randomUUID().toString(),
      correctsId,
      sourceComponent;
  private MoleculeDrawing snapshot;

  LaboratoryAnalysisPage(
      final LaboratoryEditor editor, final String selected, final Runnable close) {
    super("Анализы", close);
    this.editor = editor;
    this.experimentId = editor.document().experiment().id();
    this.setId("chemistry-analysis-page");
    this.setMinSize(0, 0);
    for (final var c : editor.document().experiment().components())
      this.component.getItems().add(new Choice(c.id(), c.molecule().displayName()));
    this.component.getSelectionModel().selectFirst();
    this.component.getItems().stream()
        .filter(c -> c.id().equals(selected))
        .findFirst()
        .ifPresent(this.component::setValue);
    this.sampleChoice.getItems().add(new Choice("", "Без зарегистрированного образца"));
    editor
        .document()
        .experiment()
        .samples()
        .forEach(
            s ->
                this.sampleChoice
                    .getItems()
                    .add(new Choice(s.id(), s.code() + " · " + s.balance().label())));
    this.sampleChoice.getSelectionModel().selectFirst();
    this.sampleChoice.setId("chemistry-analysis-sample-ref");
    this.sampleChoice.setPrefWidth(300);
    this.component.setId("chemistry-analysis-component");
    this.component.setMaxWidth(Double.MAX_VALUE);
    this.component.setPrefWidth(300);
    this.origin.getItems().setAll(Origin.values());
    this.origin.setPromptText("Укажите происхождение");
    this.origin.setId("chemistry-analysis-origin");
    this.nucleus.getItems().setAll(Nucleus.values());
    this.nucleus.setValue(Nucleus.H1);
    this.nucleus.setId("chemistry-analysis-nucleus");
    this.ion.getItems().setAll(MoleculeIonMass.Ion.values());
    this.ion.setValue(MoleculeIonMass.Ion.PROTONATED);
    this.ion.setId("chemistry-analysis-ion");
    for (final String name :
        List.of(
            "title",
            "sample",
            "instrument",
            "frequency",
            "solvent",
            "scale",
            "technique",
            "ionText",
            "formula",
            "calculated",
            "found",
            "calculation",
            "stationary",
            "eluent",
            "visualization",
            "front")) {
      final TextField field = new TextField();
      field.setId("chemistry-analysis-" + name);
      field.setPrefWidth(190);
      field
          .textProperty()
          .addListener(
              (o, a, b) -> {
                if (!this.changing && !this.calculating) {
                  if (name.equals("calculated")) {
                    this.field("calculation").clear();
                  }
                  if (name.equals("ionText") || name.equals("formula")) {
                    this.field("calculated").clear();
                    this.field("calculation").clear();
                  }
                }
                this.changed();
              });
      this.fields.put(name, field);
    }
    this.source.setId("chemistry-analysis-source");
    this.note.setId("chemistry-analysis-note");
    for (final TextArea a : List.of(this.source, this.note)) {
      a.setWrapText(true);
      a.setPrefRowCount(2);
      a.textProperty().addListener((o, b, c) -> this.changed());
    }
    this.component
        .valueProperty()
        .addListener(
            (o, a, b) -> {
              if (!this.changing && b != null) {
                this.sampleId = null;
                this.sampleChoice.getSelectionModel().selectFirst();
                this.sourceComponent = b.id();
                this.snapshot = this.editor.document().layouts().get(b.id());
                this.rows.getItems().forEach(row -> row.fields.get(5).set(""));
                this.field("calculated").clear();
                this.field("formula").clear();
                this.field("calculation").clear();
                this.showAssignment();
                this.changed();
                this.status.setText(
                    "Соединение изменено: назначения атомов и расчётный m/z очищены. Проверьте соответствие введённых наблюдений образцу.");
              }
            });
    this.sampleChoice
        .valueProperty()
        .addListener(
            (o, a, b) -> {
              if (!this.changing && b != null) {
                this.useSample(b.id().isEmpty() ? null : b.id());
              }
            });
    this.origin.valueProperty().addListener((o, a, b) -> this.changed());
    this.nucleus
        .valueProperty()
        .addListener(
            (o, a, b) -> {
              this.changed();
              this.showAssignment();
            });
    this.methodLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
    final FlowPane common =
        new FlowPane(
            12,
            8,
            labeled("Соединение", this.component),
            labeled("Образец", this.sampleChoice),
            labeled("Откуда данные", this.origin));
    this.source.setPromptText("Файл, метод измерения, DOI или источник расчёта");
    this.note.setPromptText("Необязательные примечания");
    this.rows.setId("chemistry-analysis-rows");
    this.rows.setMinSize(0, 110);
    this.rows.setMaxHeight(Region.USE_PREF_SIZE);
    this.rows.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    this.rows
        .getItems()
        .addListener((javafx.collections.ListChangeListener<Row>) c -> this.rowHeight());
    this.rowHeight();
    this.rows.setPlaceholder(
        note("Добавьте сигнал или пятно. Пустая таблица означает, что расшифровка не внесена."));
    this.rows
        .getSelectionModel()
        .selectedItemProperty()
        .addListener((o, a, b) -> this.showAssignment());
    final Button add = button("+ Строка", "add-row", () -> this.add(null)),
        duplicate =
            button(
                "Дублировать строку",
                "duplicate-row",
                () -> this.add(this.rows.getSelectionModel().getSelectedItem())),
        remove =
            button(
                "Удалить строку",
                "remove-row",
                () -> {
                  final var row = this.rows.getSelectionModel().getSelectedItem();
                  if (row != null) {
                    this.rows.getItems().remove(row);
                    this.changed();
                  }
                });
    final VBox dataRows = new VBox(8, this.rows, new FlowPane(8, 8, add, duplicate, remove));
    dataRows.setId("chemistry-analysis-data-rows");
    final TitledPane files = new TitledPane("Исходные файлы и изображения", this.fileChoices);
    files.setExpanded(false);
    files.setAnimated(false);
    this.fileChoices
        .getChildren()
        .add(
            note(
                "Отметьте вложения опыта. Новые исходные файлы добавляются на вкладке «Файлы» рабочего листа."));
    for (final var a : editor.document().experiment().attachments()) {
      final CheckBox c = new CheckBox(a.name());
      c.setWrapText(true);
      c.selectedProperty().addListener((o, b, v) -> this.changed());
      this.attachments.put(a.id(), c);
      this.fileChoices.getChildren().add(c);
    }
    final TitledPane assignment =
        new TitledPane(
            "Назначение выбранного сигнала на структуру",
            new VBox(
                8,
                new FlowPane(
                    8,
                    6,
                    this.button("← Сигнал", "previous-signal", () -> this.selectSignal(-1)),
                    this.selectedSignal,
                    this.button("Сигнал →", "next-signal", () -> this.selectSignal(1)),
                    this.button(
                        "Снять назначения",
                        "clear-assignment",
                        () -> {
                          final var row = this.rows.getSelectionModel().getSelectedItem();
                          if (row != null) {
                            row.fields.get(5).set("");
                            this.showAssignment();
                          }
                        })),
                note(
                    "Выберите строку и щёлкните атом. Для ¹H щелчок по тяжёлому атому назначает его явные обычные H; повторный щелчок снимает назначение. Это ручная расшифровка."),
                this.structure));
    assignment.setExpanded(false);
    assignment.setAnimated(false);
    assignment.setId("chemistry-analysis-assignment");
    this.metadata.setText("Дополнительно");
    this.metadata.setId("chemistry-analysis-metadata");
    this.metadata.setExpanded(false);
    this.metadata.setAnimated(false);
    this.metadata.setContent(
        new VBox(
            10,
            new FlowPane(
                12,
                8,
                labeled("Название записи", this.field("title")),
                labeled("Подпись образца / фракции", this.field("sample")),
                labeled("Прибор", this.field("instrument"))),
            labeled("Примечания и исключения", this.note),
            files));
    this.form.setPadding(new Insets(12));
    this.form
        .getChildren()
        .setAll(
            this.methodLabel,
            common,
            this.methodFields,
            dataRows,
            assignment,
            labeled("Источник данных", this.source),
            this.metadata,
            this.feedback);
    this.form.setMinWidth(0);
    final ScrollPane formScroll = new ScrollPane(this.form);
    formScroll.setFitToWidth(true);
    formScroll.setMinSize(0, 0);
    formScroll.setId("chemistry-analysis-form-scroll");
    this.inputTab.setContent(formScroll);
    this.description.setEditable(false);
    this.description.setWrapText(true);
    this.description.setPrefRowCount(13);
    this.description.setId("chemistry-analysis-description");
    this.emptyPreview.setId("chemistry-analysis-empty-preview");
    this.emptyPreview
        .getChildren()
        .setAll(
            this.previewHint,
            this.button("Проверить введённые данные", "preview-empty", this::preview));
    this.previewApply.getStyleClass().add("laboratory-primary-action");
    this.previewApply.disableProperty().bind(this.apply.disableProperty());
    this.previewApply.setOnAction(e -> this.apply());
    this.previewBody
        .getChildren()
        .setAll(
            this.emptyPreview,
            this.previewState,
            this.plot,
            this.description,
            new FlowPane(
                8,
                8,
                this.button(
                    "К вводу анализа",
                    "back-input",
                    () -> this.tabs.getSelectionModel().select(this.inputTab)),
                this.previewApply));
    this.previewBody.setPadding(new Insets(16));
    final ScrollPane previewScroll = new ScrollPane(this.previewBody);
    previewScroll.setFitToWidth(true);
    this.textTab.setContent(previewScroll);
    this.history.setId("chemistry-analysis-history");
    this.history.setMinSize(0, 0);
    this.history.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    this.history
        .getColumns()
        .addAll(
            column("Запись", a -> a.title() + "\n" + a.molecule().displayName(), 260),
            column("Метод / источник", a -> a.data().method() + "\n" + a.origin(), 150));
    this.history.setPlaceholder(
        note("Анализов ещё нет. Нажмите «Добавить анализ» и выберите ЯМР, МС или ТСХ."));
    this.history
        .getSelectionModel()
        .selectedItemProperty()
        .addListener((o, a, b) -> this.showSaved());
    this.history.setRowFactory(
        v -> {
          final TableRow<LaboratoryAnalysis> row = new TableRow<>();
          row.setOnMouseClicked(
              e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                  this.inspecting = true;
                  this.arrange();
                }
              });
          return row;
        });
    this.history.setOnKeyPressed(
        e -> {
          if (e.getCode() == KeyCode.ENTER
              && this.history.getSelectionModel().getSelectedItem() != null) {
            this.inspect.fire();
            e.consume();
          }
        });
    this.historyBox.getChildren().add(this.history);
    VBox.setVgrow(this.history, Priority.ALWAYS);
    this.historyBox.setMinSize(0, 0);
    this.savedCard.setPadding(new Insets(14));
    this.savedCard.setMinWidth(0);
    this.savedCard.getStyleClass().add("laboratory-search-card");
    this.savedScroll.setFitToWidth(true);
    this.savedScroll.setMinSize(0, 0);
    this.savedScroll
        .viewportBoundsProperty()
        .addListener(
            (o, a, b) -> {
              final double height = Math.max(130, Math.min(220, b.getHeight() - 95));
              this.savedPlot.setMinHeight(height);
              this.savedPlot.setPrefHeight(height);
              final double structureHeight = Math.max(140, Math.min(300, b.getHeight() - 90));
              this.savedStructure.setMinHeight(structureHeight);
              this.savedStructure.setPrefHeight(structureHeight);
            });
    this.split.setMinSize(0, 0);
    this.historyBox.setPrefWidth(430);
    this.savedScroll.setPrefWidth(650);
    HBox.setHgrow(this.historyBox, Priority.ALWAYS);
    HBox.setHgrow(this.savedScroll, Priority.ALWAYS);
    this.split.getChildren().setAll(this.historyBox, this.savedScroll);
    this.inspect.setOnAction(
        e -> {
          this.inspecting = true;
          this.arrange();
        });
    this.toList.setOnAction(
        e -> {
          this.inspecting = false;
          this.arrange();
        });
    final Button correct = button("Создать исправление", "correct", this::correct);
    correct
        .disableProperty()
        .bind(this.history.getSelectionModel().selectedItemProperty().isNull());
    final VBox results =
        new VBox(8, this.split, new FlowPane(8, 8, this.toList, this.inspect, correct));
    results.setPadding(new Insets(10));
    VBox.setVgrow(this.split, Priority.ALWAYS);
    this.resultsTab.setContent(results);
    this.spectrum =
        new LaboratorySpectrumPane(
            new LaboratorySpectrumPane.Listener() {
              public void changed() {
                LaboratoryAnalysisPage.this.changed();
              }

              public void imported(
                  final ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy.NmrTrace trace) {
                final var p = LaboratoryAnalysisPage.this;
                p.rows.getItems().clear();
                p.nucleus.setValue(trace.nucleus());
                p.field("frequency").setText(Double.toString(trace.frequencyMHz()));
                p.field("solvent").setText(trace.solvent());
                p.field("scale").setText("1");
                p.status.setText(
                    "Загружен новый спектр; прежняя таблица сигналов очищена. Укажите происхождение данных и добавьте расшифровку.");
              }

              public void processed(
                  final ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy.NmrTrace
                      previous,
                  final ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy.NmrTrace next,
                  final NmrProcessing.Spectrum result) {
                final double shift =
                    next.processing().referencePpm() - previous.processing().referencePpm();
                final var p = LaboratoryAnalysisPage.this;
                p.spectralUpdating = true;
                try {
                  for (final Row row : p.rows.getItems()) {
                    try {
                      final BigDecimal from = decimal(row.fields.get(0).get()),
                          to = decimal(row.fields.get(1).get());
                      if (from == null) {
                        throw new IllegalArgumentException("Укажите границы.");
                      }
                      final double low = from.doubleValue() + shift,
                          high = (to == null ? from : to).doubleValue() + shift;
                      if (shift != 0) {
                        row.fields.get(0).set(BigDecimal.valueOf(low).toPlainString());
                        if (to != null) {
                          row.fields.get(1).set(BigDecimal.valueOf(high).toPlainString());
                        }
                      }
                      if (row.spectralIntegral) {
                        final double area = result.area(low, high);
                        row.fields
                            .get(3)
                            .set(area > 0 ? BigDecimal.valueOf(area).toPlainString() : "");
                      }
                    } catch (final RuntimeException error) {
                      if (row.spectralIntegral) {
                        row.fields.get(3).set("");
                      }
                    }
                  }
                } finally {
                  p.spectralUpdating = false;
                }
                p.field("scale").setText("1");
                p.status.setText(
                    "Обработка обновлена; диапазоны сдвинуты вместе со шкалой, интегралы спектра пересчитаны. Нормировка сброшена.");
              }

              public void integral(final Signal signal) {
                final var p = LaboratoryAnalysisPage.this;
                if (p.rows.getItems().size() >= 2000) {
                  p.status.setText("Достигнут предел 2000 сигналов.");
                  return;
                }
                p.add(null);
                final var row = p.rows.getItems().getLast();
                row.fields.get(0).set(signal.from().toPlainString());
                row.fields.get(1).set(signal.to().toPlainString());
                row.fields.get(3).set(signal.integral().toPlainString());
                row.fields.get(6).set(signal.note());
                row.spectralIntegral = true;
              }

              public void scale(final double value) {
                LaboratoryAnalysisPage.this
                    .field("scale")
                    .setText(BigDecimal.valueOf(value).toPlainString());
              }
            },
            () -> this.nucleus.getValue(),
            () -> {
              final BigDecimal f = this.value("frequency");
              if (f == null)
                throw new IllegalArgumentException(
                    "Для CSV укажите частоту ЯМР на вкладке «Ввод».");
              return f.doubleValue();
            },
            () -> this.field("solvent").getText());
    this.spectrumTab.setContent(this.spectrum);
    this.tabs.getTabs().setAll(this.resultsTab, this.inputTab, this.textTab);
    this.tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    final MenuButton create = new MenuButton("Добавить анализ");
    create.setId("chemistry-analysis-new");
    create.getStyleClass().add("laboratory-primary-action");
    for (final String[] kind :
        new String[][] {
          {"ЯМР", "NMR", "nmr"}, {"Масс-спектрометрия", "MS", "ms"}, {"ТСХ", "TLC", "tlc"}
        }) {
      final MenuItem item = new MenuItem(kind[0]);
      item.setId("chemistry-analysis-new-" + kind[2]);
      item.setOnAction(e -> this.begin(kind[1]));
      create.getItems().add(item);
    }
    BorderPane.setMargin(create, new Insets(0, 16, 8, 16));
    final BorderPane center = new BorderPane(this.tabs);
    center.setTop(create);
    this.setCenter(center);
    this.apply.setId("chemistry-analysis-apply");
    this.apply.getStyleClass().add("laboratory-primary-action");
    this.apply.setOnAction(e -> this.apply());
    this.check.setId("chemistry-analysis-check");
    this.check.setOnAction(e -> this.preview());
    this.discard.setId("chemistry-analysis-discard");
    this.discard.setOnAction(
        e -> {
          this.dirty = false;
          this.correctsId = null;
          this.reset(this.method);
          this.tabs.getSelectionModel().select(this.resultsTab);
          this.status.setText("Незавершённый ввод очищен. Записанные результаты сохранены.");
        });
    final FlowPane actions = new FlowPane(8, 6, this.check, this.apply, this.discard);
    this.tabs
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (o, a, b) -> {
              actions.setVisible(b == this.inputTab || b == this.spectrumTab);
              actions.setManaged(b == this.inputTab || b == this.spectrumTab);
            });
    final VBox footer = new VBox(6, actions, this.status);
    footer.setPadding(new Insets(8, 16, 10, 16));
    this.setBottom(footer);
    this.widthProperty().addListener((o, a, b) -> this.arrange());
    this.addEventFilter(
        javafx.scene.input.KeyEvent.KEY_PRESSED,
        e -> {
          if (e.isShortcutDown()
              && e.getCode() == KeyCode.ENTER
              && (this.tabs.getSelectionModel().getSelectedItem() == this.inputTab
                  || this.tabs.getSelectionModel().getSelectedItem() == this.spectrumTab)) {
            this.apply();
            e.consume();
          }
        });
    this.reset("NMR");
    this.refreshHistory();
    this.tabs.getSelectionModel().select(this.resultsTab);
    actions.setVisible(false);
    actions.setManaged(false);
    this.arrange();
  }

  void showForSample(final String id) {
    this.history
        .getItems()
        .setAll(
            this.editor.document().experiment().analyses().stream()
                .filter(a -> id.equals(a.sampleId()))
                .toList());
    this.history.getSelectionModel().selectLast();
    this.tabs.getSelectionModel().select(this.resultsTab);
    this.status.setText("Показаны аналитические записи выбранного образца, включая исправления.");
  }

  void beginForSample(final String id, final String method) {
    if (this.pending()) {
      this.begin(method);
      return;
    }
    final var sample =
        this.sampleChoice.getItems().stream()
            .filter(c -> c.id().equals(id))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Образец отсутствует."));
    this.begin(method);
    this.sampleChoice.setValue(sample);
    // Opening a fresh form supplies its identity, not an analytical observation.
    this.dirty = false;
    this.apply.setDisable(true);
  }

  private void useSample(final String id) {
    this.changing = true;
    try {
      this.sampleId = id;
      this.component.setDisable(id != null);
      if (id != null) {
        final var sample = LaboratorySampleService.sample(this.editor.document().experiment(), id);
        this.sourceComponent = sample.componentId();
        this.snapshot = new LaboratoryMoleculePlacement().place(sample.molecule(), 0, 0);
        final var choice =
            this.component.getItems().stream()
                .filter(c -> c.id().equals(sample.componentId()))
                .findFirst()
                .orElseGet(
                    () -> {
                      final Choice c =
                          new Choice(
                              sample.componentId(), sample.molecule().displayName() + " · образец");
                      this.component.getItems().add(c);
                      return c;
                    });
        this.component.setValue(choice);
        this.field("sample").setText(sample.code());
      } else {
        final var choice = this.component.getValue();
        this.sourceComponent = choice == null ? null : choice.id();
        this.snapshot = choice == null ? null : this.editor.document().layouts().get(choice.id());
      }
      this.rows.getItems().forEach(row -> row.fields.get(5).set(""));
      this.field("calculated").clear();
      this.field("formula").clear();
      this.field("calculation").clear();
    } finally {
      this.changing = false;
    }
    this.showAssignment();
    this.changed();
    this.status.setText(
        id == null
            ? "Привязка к зарегистрированному образцу снята."
            : "Анализ связан с образцом и его сохранённой структурой. Происхождение данных задаётся отдельно.");
  }

  private TextField field(final String id) {
    return this.fields.get(id);
  }

  private static VBox labeled(final String title, final Node n) {
    return new VBox(4, note(title), n);
  }

  private Button button(final String title, final String id, final Runnable action) {
    final Button b = new Button(title);
    b.setId("chemistry-analysis-" + id);
    b.setOnAction(e -> action.run());
    return b;
  }

  private static TableColumn<LaboratoryAnalysis, String> column(
      final String name, final Function<LaboratoryAnalysis, String> f, final double width) {
    final TableColumn<LaboratoryAnalysis, String> c = new TableColumn<>(name);
    c.setPrefWidth(width);
    c.setCellValueFactory(v -> new ReadOnlyStringWrapper(f.apply(v.getValue())));
    c.setCellFactory(
        v ->
            new TableCell<>() {
              private final Label title = new Label(), detail = new Label();
              private final VBox content = new VBox(3, this.title, this.detail);

              {
                this.content.prefWidthProperty().bind(this.widthProperty().subtract(16));
                this.content.setMinWidth(0);
                for (final Label label : List.of(this.title, this.detail)) {
                  label.setMinWidth(0);
                  label.setMaxWidth(Double.MAX_VALUE);
                  label.setMinHeight(Region.USE_PREF_SIZE);
                  label.setStyle("-fx-text-fill: -fx-text-background-color;");
                }
              }

              @Override
              protected void updateItem(final String value, final boolean empty) {
                super.updateItem(value, empty);
                this.setText(null);
                if (empty || value == null) {
                  this.setGraphic(null);
                  this.setTooltip(null);
                  this.setAccessibleText(null);
                  return;
                }
                final int split = value.indexOf('\n');
                this.title.setText(split < 0 ? value : value.substring(0, split));
                this.detail.setText(split < 0 ? "" : value.substring(split + 1));
                visible(this.detail, split >= 0);
                this.setGraphic(this.content);
                this.setTooltip(new Tooltip(value));
                this.setAccessibleText(value);
              }
            });
    return c;
  }

  void saving(final java.util.function.Consumer<Boolean> save) {
    final Button button = this.button("Сохранить опыт", "save", () -> save.accept(false));
    button.getStyleClass().add("laboratory-save");
    this.addHeaderAction(button);
  }

  private static void visible(final Node node, final boolean show) {
    node.setVisible(show);
    node.setManaged(show);
  }

  private void rowHeight() {
    this.rows.setPrefHeight(Math.min(260, Math.max(110, 42 + 40 * this.rows.getItems().size())));
  }

  private void spectrum() {
    if (!this.tabs.getTabs().contains(this.spectrumTab))
      this.tabs.getTabs().add(2, this.spectrumTab);
    this.tabs.getSelectionModel().select(this.spectrumTab);
  }

  private void clearPreview(final String hint) {
    this.plot.data(null);
    this.description.clear();
    this.previewHint.setText(hint);
    visible(this.emptyPreview, true);
    visible(this.previewState, false);
    visible(this.plot, false);
    visible(this.description, false);
    visible(this.previewApply, false);
  }

  private static boolean graphical(final Data data) {
    return data instanceof Nmr n && (n.trace() != null || !n.signals().isEmpty())
        || data instanceof Tlc t && !t.spots().isEmpty()
        || data instanceof Mass m && (m.foundMz() != null || m.calculatedMz() != null);
  }

  private void showPreview(final LaboratoryAnalysis a, final boolean draft) {
    this.plot.data(a.data());
    visible(this.plot, graphical(a.data()));
    this.description.setText(LaboratoryAnalysisSummary.text(a));
    this.description.positionCaret(0);
    visible(this.description, true);
    visible(this.emptyPreview, false);
    this.previewState.setText(
        draft
            ? "Предпросмотр · запись ещё не добавлена к опыту"
            : "Сохранённый анализ · " + a.title());
    visible(this.previewState, true);
    visible(this.previewApply, draft);
  }

  private void changed() {
    if (this.changing) return;
    this.dirty = true;
    this.apply.setDisable(this.applied || this.snapshot == null);
    this.clearPreview("Данные изменились. Проверьте ввод, чтобы обновить результат.");
    this.feedback.setText("");
  }

  boolean pending() {
    return this.dirty && !this.applied;
  }

  private void begin(final String method) {
    if (this.pending()) {
      this.status.setText(
          "Сначала добавьте текущую запись или нажмите «Очистить ввод». Введённые данные сохранены в форме.");
      this.tabs.getSelectionModel().select(this.inputTab);
      return;
    }
    this.correctsId = null;
    this.reset(method);
    this.tabs.getSelectionModel().select(this.inputTab);
  }

  private void reset(final String method) {
    this.changing = true;
    try {
      this.spectrum.set(null);
      this.spectrum.setDisable(false);
      this.method = method;
      this.draftId = UUID.randomUUID().toString();
      this.applied = false;
      this.form.setDisable(false);
      this.fields.values().forEach(TextField::clear);
      this.source.clear();
      this.note.clear();
      this.origin.setValue(null);
      this.nucleus.setValue(Nucleus.H1);
      this.ion.setValue(MoleculeIonMass.Ion.PROTONATED);
      this.rows.getItems().clear();
      this.attachments.values().forEach(c -> c.setSelected(false));
      this.component.setDisable(false);
      this.sampleId = null;
      this.sampleChoice.getSelectionModel().selectFirst();
      this.sampleChoice.setDisable(false);
      if (this.component.getValue() == null
          || !this.editor.document().layouts().containsKey(this.component.getValue().id())) {
        this.component.getItems().stream()
            .filter(c -> this.editor.document().layouts().containsKey(c.id()))
            .findFirst()
            .ifPresentOrElse(this.component::setValue, () -> this.component.setValue(null));
      }
      final var choice = this.component.getValue();
      this.sourceComponent = choice == null ? null : choice.id();
      this.snapshot = choice == null ? null : this.editor.document().layouts().get(choice.id());
      this.field("scale").setText("1");
      this.field("stationary").setText("SiO₂");
      this.field("technique").setText("HRMS (ESI)");
      this.field("title")
          .setText(
              method.equals("NMR") ? "ЯМР" : method.equals("MS") ? "Масс-спектрометрия" : "ТСХ");
      this.configure();
    } finally {
      this.changing = false;
    }
    this.dirty = false;
    this.apply.setDisable(true);
    this.feedback.setText("");
    this.metadata.setExpanded(false);
    this.clearPreview(
        "Здесь появится результат проверки. Введите анализ или откройте сохранённую запись.");
    this.status.setText("");
    this.showAssignment();
  }

  private void configure() {
    this.spectrumTab.setDisable(!this.method.equals("NMR"));
    if (!this.method.equals("NMR")) this.tabs.getTabs().remove(this.spectrumTab);
    else if (this.spectrum.hasTrace() && !this.tabs.getTabs().contains(this.spectrumTab))
      this.tabs.getTabs().add(Math.min(2, this.tabs.getTabs().size()), this.spectrumTab);
    this.methodLabel.setText(
        (this.correctsId == null ? "Новая запись · " : "Исправление · ")
            + (this.method.equals("NMR") ? "ЯМР" : this.method.equals("MS") ? "МС" : "ТСХ"));
    this.methodFields.getChildren().clear();
    this.rows.getColumns().clear();
    if (this.method.equals("NMR")) {
      this.methodFields
          .getChildren()
          .addAll(
              new FlowPane(
                  12,
                  8,
                  labeled("Ядро", this.nucleus),
                  labeled("Частота, МГц", this.field("frequency")),
                  labeled("Растворитель", this.field("solvent"))),
              this.button("Импорт и обработка спектра…", "spectrum", this::spectrum));
      final TitledPane normalization =
          new TitledPane(
              "Нормировка интегралов", labeled("Множитель интегралов", this.field("scale")));
      normalization.setExpanded(false);
      normalization.setAnimated(false);
      this.methodFields.getChildren().add(normalization);
      final String[] names = {
        "δ от, ppm",
        "δ до, ppm",
        "Мультиплет",
        "Интеграл",
        "J, Hz; через ;",
        "Назначено",
        "Примечание"
      };
      for (int i = 0; i < names.length; i++) this.cell(names[i], i, i == 6 ? 200 : 110, i == 5);
    } else if (this.method.equals("MS")) {
      this.methodFields
          .getChildren()
          .add(
              new FlowPane(
                  12,
                  8,
                  labeled("Найденное m/z", this.field("found")),
                  labeled("Ион / аддукт", this.field("ionText")),
                  labeled("Метод", this.field("technique"))));
      final TitledPane calculated =
          new TitledPane(
              "Сравнить с расчётным m/z",
              new VBox(
                  10,
                  new FlowPane(
                      8,
                      8,
                      this.ion,
                      this.button(
                          "Рассчитать из структуры", "calculate-mass", this::calculateMass)),
                  new FlowPane(
                      12,
                      8,
                      labeled("Состав иона", this.field("formula")),
                      labeled("Расчётное m/z", this.field("calculated"))),
                  labeled("Источник расчётного значения", this.field("calculation")),
                  note(
                      "Расчёт использует снимок структуры и выбранный аддукт. Ручное значение вводится с источником.")));
      calculated.setExpanded(false);
      calculated.setAnimated(false);
      this.methodFields.getChildren().add(calculated);
    } else {
      this.methodFields
          .getChildren()
          .add(
              new FlowPane(
                  12,
                  8,
                  labeled("Пластинка", this.field("stationary")),
                  labeled("Элюент и соотношение", this.field("eluent")),
                  labeled("Проявление", this.field("visualization")),
                  labeled("Фронт, mm", this.field("front"))));
      this.cell("Пятно / дорожка", 0, 240, false);
      this.cell("Расстояние, mm", 1, 160, false);
      this.cell("Rf вручную", 2, 160, false);
      this.methodFields
          .getChildren()
          .add(
              note(
                  "В строке задайте расстояние либо готовый Rf. При вводе расстояния Rf рассчитывается по фронту."));
    }
    final Node data = this.form.lookup("#chemistry-analysis-data-rows"),
        assignment = this.form.lookup("#chemistry-analysis-assignment");
    if (data != null) {
      data.setVisible(!this.method.equals("MS"));
      data.setManaged(data.isVisible());
    }
    if (assignment != null) {
      assignment.setVisible(this.method.equals("NMR"));
      assignment.setManaged(assignment.isVisible());
    }
  }

  private void cell(
      final String name, final int index, final double width, final boolean assignment) {
    final TableColumn<Row, Row> c = new TableColumn<>(name);
    c.setPrefWidth(width);
    c.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue()));
    c.setSortable(false);
    c.setCellFactory(
        v ->
            new TableCell<>() {
              private final TextField input = new TextField();
              private final Button assign = new Button();
              private StringProperty bound;

              @Override
              protected void updateItem(final Row row, final boolean empty) {
                super.updateItem(row, empty);
                if (this.bound != null) {
                  this.input.textProperty().unbindBidirectional(this.bound);
                  this.assign.textProperty().unbind();
                  this.bound = null;
                }
                if (empty || row == null) {
                  this.setGraphic(null);
                  return;
                }
                this.bound = row.fields.get(index);
                if (assignment) {
                  this.assign
                      .textProperty()
                      .bind(
                          javafx.beans.binding.Bindings.createStringBinding(
                              () ->
                                  this.bound.get().isBlank()
                                      ? "Назначить…"
                                      : this.bound.get().split(",").length + " атомов",
                              this.bound));
                  this.assign.setOnAction(
                      e -> {
                        LaboratoryAnalysisPage.this.rows.getSelectionModel().select(row);
                        ((TitledPane)
                                LaboratoryAnalysisPage.this.form.lookup(
                                    "#chemistry-analysis-assignment"))
                            .setExpanded(true);
                        LaboratoryAnalysisPage.this.showAssignment();
                        LaboratoryAnalysisPage.this.scrollAssignment();
                      });
                  this.setGraphic(this.assign);
                } else {
                  this.input.textProperty().bindBidirectional(this.bound);
                  this.input.setId("analysis-cell-" + index);
                  this.input.setOnMouseClicked(
                      e -> LaboratoryAnalysisPage.this.rows.getSelectionModel().select(row));
                  this.setGraphic(this.input);
                }
              }
            });
    this.rows.getColumns().add(c);
  }

  private void add(final Row copy) {
    if (this.rows.getItems().size() >= 2000) {
      this.status.setText("Достигнут предел 2000 строк.");
      return;
    }
    final Row row = new Row();
    if (copy != null) for (int i = 0; i < 7; i++) row.fields.get(i).set(copy.fields.get(i).get());
    if (copy != null) row.spectralIntegral = copy.spectralIntegral;
    for (int index = 0; index < 7; index++) {
      final int cell = index;
      row.fields
          .get(index)
          .addListener(
              (o, a, b) -> {
                if (!this.changing && !this.spectralUpdating) {
                  if (cell == 3) row.spectralIntegral = false;
                  else if ((cell == 0 || cell == 1) && row.spectralIntegral)
                    this.refreshIntegral(row);
                }
                this.changed();
              });
    }
    this.rows.getItems().add(row);
    this.rows.getSelectionModel().select(row);
    this.changed();
  }

  private void refreshIntegral(final Row row) {
    this.spectralUpdating = true;
    try {
      final BigDecimal low = decimal(row.fields.get(0).get()),
          high = decimal(row.fields.get(1).get());
      final double area =
          this.spectrum.area(low.doubleValue(), (high == null ? low : high).doubleValue());
      row.fields.get(3).set(area > 0 ? BigDecimal.valueOf(area).toPlainString() : "");
    } catch (RuntimeException error) {
      row.fields.get(3).set("");
    } finally {
      this.spectralUpdating = false;
    }
  }

  private static BigDecimal decimal(final String text) {
    final String s = text.strip().replace(',', '.');
    if (s.isEmpty()) return null;
    if (s.length() > 128) throw new IllegalArgumentException("Слишком длинное число.");
    try {
      final BigDecimal n = new BigDecimal(s);
      if (Math.abs((long) n.scale()) > 64 || n.precision() > 64)
        throw new IllegalArgumentException("Число вне поддержанного диапазона точности.");
      return n.stripTrailingZeros();
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Неверное число: " + text);
    }
  }

  private BigDecimal value(final String key) {
    return decimal(this.field(key).getText());
  }

  private static Set<AtomId> atoms(final String text) {
    if (text.isBlank()) return Set.of();
    return new LinkedHashSet<>(
        Arrays.stream(text.split(",")).map(String::strip).map(AtomId::of).toList());
  }

  private LaboratoryAnalysis prepare() {
    if (!this.experimentId.equals(this.editor.document().experiment().id()))
      throw new IllegalArgumentException("Открыт другой опыт; аналитический ввод не применён.");
    if (this.snapshot == null || this.sourceComponent == null)
      throw new IllegalArgumentException("Добавьте соединение в опыт.");
    if (this.origin.getValue() == null)
      throw new IllegalArgumentException(
          "Выберите происхождение данных: измерение, литература или расчёт.");
    final Data data;
    if (this.method.equals("NMR")) {
      final var trace = this.spectrum.current();
      final List<Signal> signals = new ArrayList<>();
      for (int i = 0; i < this.rows.getItems().size(); i++) {
        final var row = this.rows.getItems().get(i);
        if (row.spectralIntegral) this.refreshIntegral(row);
        final var p = row.fields;
        try {
          final BigDecimal from = decimal(p.get(0).get());
          if (from == null) throw new IllegalArgumentException("укажите δ");
          signals.add(
              new Signal(
                  from,
                  decimal(p.get(1).get()),
                  p.get(2).get(),
                  decimal(p.get(3).get()),
                  p.get(4).get().isBlank()
                      ? List.of()
                      : Arrays.stream(p.get(4).get().strip().split("[;\\s]+"))
                          .map(LaboratoryAnalysisPage::decimal)
                          .toList(),
                  atoms(p.get(5).get()),
                  p.get(6).get(),
                  row.spectralIntegral));
          if (row.spectralIntegral && decimal(p.get(3).get()) == null)
            throw new IllegalArgumentException("проверьте границы и обработку интеграла спектра");
        } catch (RuntimeException e) {
          throw new IllegalArgumentException("Сигнал " + (i + 1) + ": " + e.getMessage());
        }
      }
      data =
          new Nmr(
              this.nucleus.getValue(),
              this.value("frequency"),
              this.field("solvent").getText(),
              this.value("scale"),
              signals,
              trace);
    } else if (this.method.equals("MS")) {
      if (this.value("calculated") != null && this.field("calculation").getText().isBlank())
        throw new IllegalArgumentException("Укажите источник расчётного m/z.");
      data =
          new Mass(
              this.field("technique").getText(),
              this.field("ionText").getText(),
              this.field("formula").getText(),
              this.value("calculated"),
              this.value("found"),
              this.field("calculation").getText());
    } else {
      final List<Spot> spots = new ArrayList<>();
      for (int i = 0; i < this.rows.getItems().size(); i++) {
        final var p = this.rows.getItems().get(i).fields;
        try {
          spots.add(new Spot(p.get(0).get(), decimal(p.get(1).get()), decimal(p.get(2).get())));
        } catch (RuntimeException e) {
          throw new IllegalArgumentException("Пятно " + (i + 1) + ": " + e.getMessage());
        }
      }
      data =
          new Tlc(
              this.field("stationary").getText(),
              this.field("eluent").getText(),
              this.field("visualization").getText(),
              this.value("front"),
              spots);
    }
    return new LaboratoryAnalysis(
        "analysis_" + this.draftId,
        this.sourceComponent,
        this.field("title").getText(),
        this.field("sample").getText(),
        Instant.now(),
        this.origin.getValue(),
        this.field("instrument").getText(),
        this.source.getText(),
        this.note.getText(),
        LaboratoryAnalysisSummary.snapshot(this.snapshot.molecule(), this.draftId),
        this.attachments.entrySet().stream()
            .filter(e -> e.getValue().isSelected())
            .map(Map.Entry::getKey)
            .toList(),
        this.correctsId,
        data,
        this.sampleId);
  }

  private void preview() {
    try {
      final var a = this.prepare();
      this.showPreview(a, true);
      this.feedback.setText(
          String.join(
              "\n", LaboratoryAnalysisSummary.diagnostics(a, this.editor.document().experiment())));
      this.status.setText("Предпросмотр готов. Добавьте запись, когда проверите данные.");
      this.tabs.getSelectionModel().select(this.textTab);
    } catch (RuntimeException e) {
      this.clearPreview("Для проверки: " + e.getMessage());
      this.status.setText(e.getMessage());
      this.feedback.setText(e.getMessage());
    }
  }

  private void apply() {
    if (!this.pending()) return;
    try {
      final var a = this.prepare();
      this.editor.analysis(a);
      this.applied = true;
      this.dirty = false;
      this.apply.setDisable(true);
      this.form.setDisable(true);
      this.spectrum.setDisable(true);
      this.refreshHistory();
      this.history.getSelectionModel().select(a);
      this.tabs.getSelectionModel().select(this.resultsTab);
      this.status.setText(
          "Аналитическая запись добавлена. Сохраните опыт в QCHEM. Для изменения используйте «Создать исправление».");
    } catch (RuntimeException e) {
      this.status.setText(e.getMessage());
    }
  }

  private void calculateMass() {
    this.calculating = true;
    try {
      if (this.snapshot == null) throw new IllegalArgumentException("Нет исходной структуры.");
      final var result =
          new CdkMoleculeIonMass().calculate(this.snapshot.molecule(), this.ion.getValue());
      this.field("calculated").setText(result.mz().toPlainString());
      this.field("formula").setText(result.composition());
      this.field("ionText").setText(this.ion.getValue().toString());
      this.field("calculation").setText(result.source());
      this.status.setText("Расчётное m/z заполнено; найденное значение вводится отдельно.");
    } catch (RuntimeException e) {
      this.status.setText(e.getMessage());
    } finally {
      this.calculating = false;
    }
  }

  private void showAssignment() {
    this.selectedSignal.setText(
        this.rows.getSelectionModel().getSelectedIndex() < 0
            ? "Выберите сигнал"
            : "Сигнал "
                + (this.rows.getSelectionModel().getSelectedIndex() + 1)
                + " из "
                + this.rows.getItems().size());
    final Row row = this.rows.getSelectionModel().getSelectedItem();
    Set<AtomId> ids = Set.of();
    if (row != null)
      try {
        ids = atoms(row.fields.get(5).get());
      } catch (RuntimeException ignored) {
      }
    this.structure.show(
        this.snapshot,
        this.nucleus.getValue(),
        ids,
        chosen -> {
          if (row != null) {
            row.fields
                .get(5)
                .set(String.join(",", chosen.stream().map(AtomId::value).sorted().toList()));
            this.rows.refresh();
          }
        });
  }

  private void refreshHistory() {
    this.history.getItems().setAll(this.editor.document().experiment().analyses());
    this.history.getSelectionModel().selectLast();
    this.inspect.setDisable(this.history.getItems().isEmpty());
  }

  private void showSaved() {
    this.savedCard.getChildren().clear();
    final var a = this.history.getSelectionModel().getSelectedItem();
    if (a == null) {
      this.savedCard.getChildren().add(note("Выберите анализ в списке или добавьте новый."));
      return;
    }
    final Label title = note(a.title());
    title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
    final boolean superseded =
        this.editor.document().experiment().analyses().stream()
            .anyMatch(x -> a.id().equals(x.correctsId()));
    this.savedNotes.setText(
        String.join(
            "\n", LaboratoryAnalysisSummary.diagnostics(a, this.editor.document().experiment())));
    this.savedPlot.data(a.data());
    visible(this.savedPlot, graphical(a.data()));
    this.showPreview(a, false);
    final MoleculeDrawing savedDrawing =
        new LaboratoryMoleculePlacement().place(a.molecule(), 0, 0);
    this.savedStructure.show(
        savedDrawing, a.data() instanceof Nmr n ? n.nucleus() : Nucleus.H1, Set.of(), null);
    final TitledPane structurePane =
        new TitledPane(
            "Снимок структуры · " + a.molecule().formula().hillNotation(), this.savedStructure);
    structurePane.setExpanded(false);
    structurePane.setAnimated(false);
    final ComboBox<Signal> assignedSignals = new ComboBox<>();
    assignedSignals.setMaxWidth(Double.MAX_VALUE);
    assignedSignals.setPromptText("Показать назначения сохранённого сигнала…");
    assignedSignals.setId("chemistry-analysis-saved-signals");
    if (a.data() instanceof Nmr n) {
      assignedSignals.getItems().setAll(n.signals());
      assignedSignals.setConverter(
          new javafx.util.StringConverter<>() {
            public String toString(final Signal s) {
              return s == null
                  ? ""
                  : LaboratoryAnalysisSummary.number(s.from())
                      + " ppm · "
                      + s.multiplicity()
                      + " · назначено атомов: "
                      + s.atoms().size();
            }

            public Signal fromString(final String text) {
              return null;
            }
          });
      assignedSignals
          .valueProperty()
          .addListener(
              (o, old, value) -> {
                if (value != null) {
                  this.savedStructure.show(savedDrawing, n.nucleus(), value.atoms(), null);
                  structurePane.setExpanded(true);
                  scrollTo(this.savedScroll, this.savedCard, structurePane);
                }
              });
    }
    assignedSignals.setVisible(a.data() instanceof Nmr);
    assignedSignals.setManaged(assignedSignals.isVisible());
    final TitledPane details =
        new TitledPane(
            "Сведения об анализе",
            new VBox(
                8,
                note(
                    "Файлы: "
                        + String.join(
                            "; ",
                            this.editor.document().experiment().attachments().stream()
                                .filter(f -> a.attachmentIds().contains(f.id()))
                                .map(LaboratoryAttachment::name)
                                .toList())),
                note(
                    "Запись создана: "
                        + java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                            .format(a.recordedAt().atZone(java.time.ZoneId.systemDefault()))),
                this.button(
                    "Полное описание",
                    "saved-description",
                    () -> {
                      this.showPreview(a, false);
                      this.tabs.getSelectionModel().select(this.textTab);
                    })));
    details.setExpanded(false);
    details.setAnimated(false);
    final TitledPane diagnostics = new TitledPane("Проверки и замечания", this.savedNotes);
    diagnostics.setExpanded(false);
    diagnostics.setAnimated(false);
    visible(diagnostics, !this.savedNotes.getText().isBlank());
    this.savedCard
        .getChildren()
        .addAll(
            title,
            note(
                a.data().method()
                    + " · "
                    + a.origin()
                    + (superseded ? " · есть исправление" : "")
                    + (a.correctsId() == null ? "" : " · исправляет предыдущую запись")),
            this.savedPlot,
            note(LaboratoryAnalysisSummary.concise(a)),
            assignedSignals,
            structurePane,
            diagnostics,
            details);
    if (a.data() instanceof Nmr n && n.trace() != null) {
      final TitledPane provenance =
          new TitledPane(
              "Исходные данные и обработка", note(LaboratoryAnalysisSummary.traceText(n.trace())));
      provenance.setExpanded(false);
      provenance.setAnimated(false);
      this.savedCard.getChildren().add(5, provenance);
    }
  }

  private void arrange() {
    final boolean compact = this.getWidth() < 1000;
    final List<Node> nodes =
        this.inspecting
            ? List.of(this.savedScroll)
            : compact ? List.of(this.historyBox) : List.of(this.historyBox, this.savedScroll);
    if (!this.split.getChildren().equals(nodes)) {
      this.split.getChildren().setAll(nodes);
    }
    this.toList.setVisible(this.inspecting);
    this.toList.setManaged(this.inspecting);
    this.inspect.setVisible(compact && !this.inspecting);
    this.inspect.setManaged(compact && !this.inspecting);
  }

  private void correct() {
    final var a = this.history.getSelectionModel().getSelectedItem();
    if (a == null) return;
    if (this.pending()) {
      this.status.setText("Сначала завершите или очистите текущий ввод.");
      return;
    }
    this.correctsId = a.id();
    this.reset(a.data() instanceof Nmr ? "NMR" : a.data() instanceof Mass ? "MS" : "TLC");
    this.correctsId = a.id();
    this.changing = true;
    try {
      this.snapshot = new LaboratoryMoleculePlacement().place(a.molecule(), 0, 0);
      this.sourceComponent = a.componentId();
      final Choice historical =
          this.component.getItems().stream()
              .filter(c -> c.id().equals(a.componentId()))
              .findFirst()
              .orElseGet(
                  () -> {
                    final Choice c =
                        new Choice(a.componentId(), a.molecule().displayName() + " · снимок");
                    this.component.getItems().add(c);
                    return c;
                  });
      this.component.setValue(historical);
      this.component.setDisable(true);
      this.sampleId = a.sampleId();
      this.sampleChoice.getItems().stream()
          .filter(c -> c.id().equals(Objects.toString(this.sampleId, "")))
          .findFirst()
          .ifPresent(this.sampleChoice::setValue);
      this.sampleChoice.setDisable(true);
      this.field("title").setText(a.title());
      this.field("sample").setText(a.sample());
      this.field("instrument").setText(a.instrument());
      this.origin.setValue(a.origin());
      this.source.setText(a.source());
      this.note.setText(a.note());
      this.attachments.forEach((id, c) -> c.setSelected(a.attachmentIds().contains(id)));
      if (a.data() instanceof Nmr n) {
        this.spectrum.set(n.trace());
        this.nucleus.setValue(n.nucleus());
        this.put("frequency", n.frequencyMHz());
        this.put("scale", n.integralScale());
        this.field("solvent").setText(n.solvent());
        for (final var signal : n.signals()) {
          this.add(null);
          final var p = this.rows.getItems().getLast().fields;
          p.get(0).set(LaboratoryAnalysisSummary.number(signal.from()));
          p.get(1).set(LaboratoryAnalysisSummary.number(signal.to()));
          p.get(2).set(signal.multiplicity());
          p.get(3).set(signal.integral() == null ? "" : signal.integral().toPlainString());
          p.get(4)
              .set(
                  String.join(
                      ";", signal.couplingsHz().stream().map(BigDecimal::toPlainString).toList()));
          p.get(5)
              .set(String.join(",", signal.atoms().stream().map(AtomId::value).sorted().toList()));
          p.get(6).set(signal.note());
          this.rows.getItems().getLast().spectralIntegral = signal.spectralIntegral();
        }
      } else if (a.data() instanceof Mass m) {
        this.field("technique").setText(m.technique());
        this.field("ionText").setText(m.ion());
        this.field("formula").setText(m.ionFormula());
        this.put("calculated", m.calculatedMz());
        this.put("found", m.foundMz());
        this.field("calculation").setText(m.calculationSource());
      } else if (a.data() instanceof Tlc t) {
        this.field("stationary").setText(t.stationary());
        this.field("eluent").setText(t.eluent());
        this.field("visualization").setText(t.visualization());
        this.put("front", t.frontMm());
        for (final var spot : t.spots()) {
          this.add(null);
          final var p = this.rows.getItems().getLast().fields;
          p.get(0).set(spot.label());
          p.get(1).set(spot.distanceMm() == null ? "" : spot.distanceMm().toPlainString());
          p.get(2).set(spot.reportedRf() == null ? "" : spot.reportedRf().toPlainString());
        }
      }
      this.configure();
    } finally {
      this.changing = false;
      this.changed();
    }
    this.showAssignment();
    this.tabs.getSelectionModel().select(this.inputTab);
  }

  @Override
  void dispose() {
    super.dispose();
    this.spectrum.dispose();
  }

  private void selectSignal(final int direction) {
    if (this.rows.getItems().isEmpty()) return;
    this.rows
        .getSelectionModel()
        .select(
            Math.max(
                0,
                Math.min(
                    this.rows.getItems().size() - 1,
                    this.rows.getSelectionModel().getSelectedIndex() + direction)));
  }

  private void scrollAssignment() {
    scrollTo(
        (ScrollPane) this.inputTab.getContent(),
        this.form,
        this.form.lookup("#chemistry-analysis-assignment"));
  }

  private static void scrollTo(final ScrollPane scroll, final Region content, final Node target) {
    javafx.application.Platform.runLater(
        () -> {
          content.applyCss();
          content.layout();
          final double extent = content.getHeight() - scroll.getViewportBounds().getHeight();
          scroll.setVvalue(
              extent <= 0
                  ? 0
                  : Math.max(0, Math.min(1, target.getBoundsInParent().getMinY() / extent)));
        });
  }

  private void put(final String key, final BigDecimal n) {
    this.field(key).setText(n == null ? "" : n.toPlainString());
  }

  @Override
  boolean canClose() {
    if (this.spectrum.busy()) {
      this.status.setText("Дождитесь обработки спектра или остановите её.");
      return false;
    }
    if (this.pending()) {
      this.status.setText(
          "Есть незавершённый ввод. Добавьте запись или нажмите «Очистить ввод»; авточерновик сохраняет поля.");
      this.tabs.getSelectionModel().select(this.inputTab);
      return false;
    }
    return true;
  }

  void draft(final Properties p, final boolean restore) {
    if (!restore) {
      if (!this.pending()) return;
      this.spectrum.draft(p, false);
      p.setProperty("analysis.sampleRef", Objects.toString(this.sampleId, ""));
      p.setProperty("analysis.method", this.method);
      p.setProperty("analysis.id", this.draftId);
      p.setProperty("analysis.component", Objects.toString(this.sourceComponent, ""));
      p.setProperty("analysis.corrects", Objects.toString(this.correctsId, ""));
      p.setProperty(
          "analysis.origin", this.origin.getValue() == null ? "" : this.origin.getValue().name());
      p.setProperty("analysis.nucleus", this.nucleus.getValue().name());
      p.setProperty("analysis.ion", this.ion.getValue().name());
      p.setProperty("analysis.source", this.source.getText());
      p.setProperty("analysis.note", this.note.getText());
      this.fields.forEach((key, value) -> p.setProperty("analysis.field." + key, value.getText()));
      this.attachments.forEach(
          (id, c) -> p.setProperty("analysis.file." + id, Boolean.toString(c.isSelected())));
      p.setProperty("analysis.rows", Integer.toString(this.rows.getItems().size()));
      for (int i = 0; i < this.rows.getItems().size(); i++)
        for (int j = 0; j < 7; j++)
          p.setProperty(
              "analysis.row." + i + "." + j, this.rows.getItems().get(i).fields.get(j).get());
      for (int i = 0; i < this.rows.getItems().size(); i++)
        p.setProperty(
            "analysis.row." + i + ".integrated",
            Boolean.toString(this.rows.getItems().get(i).spectralIntegral));
      if (this.snapshot != null) {
        final var experiment =
            new LaboratoryExperiment(
                "analysis_draft",
                "Снимок анализа",
                ReactionConditions.EMPTY,
                "",
                List.of(
                    new LaboratoryComponent(
                        "source",
                        this.snapshot.molecule(),
                        ReactionComponentRole.PRODUCT,
                        null,
                        null,
                        LaboratoryEvidence.PLANNED)));
        p.setProperty(
            "analysis.snapshot",
            new LaboratoryFileCodec()
                .write(new LaboratoryDrawing(experiment, Map.of("source", this.snapshot))));
      }
      return;
    }
    this.reset(p.getProperty("analysis.method"));
    this.changing = true;
    try {
      this.draftId = p.getProperty("analysis.id");
      this.sampleId =
          p.getProperty("analysis.sampleRef", "").isEmpty()
              ? null
              : p.getProperty("analysis.sampleRef");
      this.sampleChoice.getItems().stream()
          .filter(c -> c.id().equals(Objects.toString(this.sampleId, "")))
          .findFirst()
          .ifPresent(this.sampleChoice::setValue);
      this.sourceComponent = p.getProperty("analysis.component");
      this.correctsId =
          p.getProperty("analysis.corrects", "").isEmpty()
              ? null
              : p.getProperty("analysis.corrects");
      this.component.getItems().stream()
          .filter(c -> c.id().equals(this.sourceComponent))
          .findFirst()
          .ifPresent(this.component::setValue);
      this.component.setDisable(this.correctsId != null || this.sampleId != null);
      this.sampleChoice.setDisable(this.correctsId != null);
      this.snapshot =
          p.containsKey("analysis.snapshot")
              ? new LaboratoryFileCodec()
                  .read(p.getProperty("analysis.snapshot"))
                  .layouts()
                  .get("source")
              : null;
      if (this.snapshot != null) {
        final Choice restored =
            this.component.getItems().stream()
                .filter(c -> c.id().equals(this.sourceComponent))
                .findFirst()
                .orElseGet(
                    () -> {
                      final Choice c =
                          new Choice(
                              this.sourceComponent,
                              this.snapshot.molecule().displayName() + " · снимок");
                      this.component.getItems().add(c);
                      return c;
                    });
        this.component.setValue(restored);
      }
      this.origin.setValue(
          p.getProperty("analysis.origin", "").isEmpty()
              ? null
              : Origin.valueOf(p.getProperty("analysis.origin")));
      this.nucleus.setValue(Nucleus.valueOf(p.getProperty("analysis.nucleus")));
      this.ion.setValue(MoleculeIonMass.Ion.valueOf(p.getProperty("analysis.ion")));
      this.source.setText(p.getProperty("analysis.source", ""));
      this.note.setText(p.getProperty("analysis.note", ""));
      this.fields.forEach(
          (key, value) -> value.setText(p.getProperty("analysis.field." + key, "")));
      this.attachments.forEach(
          (id, c) -> c.setSelected(Boolean.parseBoolean(p.getProperty("analysis.file." + id))));
      final int count = Integer.parseInt(p.getProperty("analysis.rows"));
      if (count < 0 || count > 2000)
        throw new IllegalArgumentException("Неверный размер аналитического черновика.");
      this.rows.getItems().clear();
      for (int i = 0; i < count; i++) {
        this.add(null);
        for (int j = 0; j < 7; j++)
          this.rows
              .getItems()
              .getLast()
              .fields
              .get(j)
              .set(p.getProperty("analysis.row." + i + "." + j, ""));
        this.rows.getItems().getLast().spectralIntegral =
            Boolean.parseBoolean(p.getProperty("analysis.row." + i + ".integrated", "false"));
      }
      this.configure();
      this.spectrum.draft(p, true);
    } finally {
      this.changing = false;
    }
    this.changed();
    this.showAssignment();
    if (p.containsKey("spectrum.trace")) this.spectrum();
    else this.tabs.getSelectionModel().select(this.inputTab);
  }
}

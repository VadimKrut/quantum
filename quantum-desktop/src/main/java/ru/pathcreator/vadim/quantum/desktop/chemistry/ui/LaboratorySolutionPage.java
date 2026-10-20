/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import static ru.pathcreator.vadim.quantum.desktop.chemistry.ui.LaboratoryWorkbenchControls.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratorySolutionBook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.*;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.*;

/** Recipes, serial dilution, explicitly sourced standardization and explicit preparation on the existing stock ledger. */
final class LaboratorySolutionPage extends LaboratoryComputationPage {
  private record Batch(String id, String title) {
    public String toString() {
      return this.title;
    }
  }

  private final LaboratoryEditor editor;
  private final Path folder;
  private final LaboratoryWorkbenchBooks.Book<LaboratorySolutionBook> book;
  private LaboratoryWorkbenchBooks.Opened<LaboratorySolutionBook> opened;
  private LaboratoryInventoryRepository.Opened inventory;
  private final ExecutorService worker =
      Executors.newSingleThreadExecutor(
          r -> {
            final var t = new Thread(r, "solution-book");
            t.setDaemon(true);
            return t;
          });
  private final TabPane tabs = new TabPane();
  private final TableView<Recipe> recipes = table();
  private final TableView<Standardization> standards = table();
  private final TableView<DilutionStep> series = table();
  private final TableView<LaboratorySolutionPlanning.Row> calculation = table();
  private final TextField title = new TextField(),
      solvent = new TextField(),
      target = new TextField(),
      stock = new TextField(),
      molarMass = new TextField(),
      purity = new TextField();
  private final ComboBox<Mode> mode = choice(Mode.values());
  private final ComboBox<ConcentrationUnit> targetUnit = choice(ConcentrationUnit.values()),
      stockUnit = choice(ConcentrationUnit.values());
  private final ComboBox<Batch> batch = new ComboBox<>();
  private final Quantity volume = new Quantity("volume", LaboratoryQuantityUnit.MILLILITRE);
  private final TextArea note = area(3), preview = area(8);
  private final Label hint = label(""), count = label("");
  private final VBox leave = new VBox(8);
  private boolean loading, dirty, conflict;
  private String correction;
  private Properties deferred, retained;
  private String deferredBatch;
  private final VBox origin = new VBox(6);
  private final ComboBox<Recipe> selectedRecipe = new ComboBox<>();
  private final ComboBox<Batch> standardBatch = new ComboBox<>();
  private final Label nextTask = label("Рассчитайте, сколько вещества взять"),
      standardSummary = label("Выберите партию для записи концентрации");
  private TitledPane savedPlans, standardRecords, standardDetails;
  private final VBox planPane = new VBox(12),
      preparedPane = new VBox(10),
      measuredPane = new VBox(12),
      planSummary = new VBox(8);
  private final Label stockBalance = label(""), planAmount = label("");
  private final VBox solidFields = new VBox(8),
      dilutionFields = new VBox(8),
      massField = field("Молярная масса, г/моль", this.molarMass);
  private Button calculateButton, saveButton, prepareButton;
  private TitledPane seriesDetails, noteDetails, originDetails;
  private PreparationInput preparationInput;
  private final TextField standardValue = new TextField(),
      standardMethod = new TextField(),
      standardMass = new TextField(),
      standardPurity = new TextField(),
      standardRatio = new TextField();
  private final TextArea standardSource = area(2);
  private final ComboBox<ConcentrationUnit> standardUnit = choice(ConcentrationUnit.values());
  private final ComboBox<Origin> standardOrigin = choice(Origin.values());
  private final Quantity standardPrimary = new Quantity("mass", LaboratoryQuantityUnit.GRAM),
      standardTitrant = new Quantity("volume", LaboratoryQuantityUnit.MILLILITRE),
      standardBlank = new Quantity("volume", LaboratoryQuantityUnit.MILLILITRE);
  private final VBox standardDirectFields = new VBox(10), standardTitrationFields = new VBox(10);
  private final Label standardError = label(""),
      standardCorrection = label(""),
      standardTask = label("Запишите концентрацию и её происхождение"),
      leaveMessage = label("");
  private final Map<String, Label> preparedConcentrations = new LinkedHashMap<>();
  private Button standardSave;
  private boolean standardTitration, standardDirty, standardConflict, standardLoading;
  private String standardCorrects, standardFingerprint;
  private Properties standardRetained;

  private final class PreparationInput {
    final Recipe recipe;
    String token = LaboratorySolutionPlanning.id("preparation"), fingerprint;
    final ComboBox<Batch> source = new ComboBox<>();
    final List<Quantity> taken = new ArrayList<>(), volumes = new ArrayList<>();
    final TextField location = new TextField();
    final TextArea reason = area(2);
    final Label error = label("");

    PreparationInput(final Recipe recipe) {
      this.recipe = recipe;
      this.fingerprint = inventory.fingerprint();
    }

    Map<String, Control> fields() {
      final Map<String, Control> fields = new LinkedHashMap<>();
      fields.put("location", this.location);
      fields.put("reason", this.reason);
      for (int i = 0; i < this.taken.size(); i++) {
        fields.put("take." + i, this.taken.get(i).value);
        fields.put("takeUnit." + i, this.taken.get(i).unit);
        fields.put("volume." + i, this.volumes.get(i).value);
        fields.put("volumeUnit." + i, this.volumes.get(i).unit);
      }
      return fields;
    }
  }

  LaboratorySolutionPage(final LaboratoryEditor editor, final Path folder, final Runnable close) {
    super("Растворы и разведения", close);
    this.editor = editor;
    this.folder = folder.toAbsolutePath().normalize();
    this.book = LaboratoryWorkbenchBooks.solutions(this.folder);
    this.setId("chemistry-solution-page");
    this.batch.setPrefWidth(370);
    this.batch.setMaxWidth(Double.MAX_VALUE);
    this.title.setPrefColumnCount(28);
    this.solvent.setPrefColumnCount(24);
    this.title.setPromptText("Например, раствор для серии синтезов");
    this.solvent.setPromptText("Растворитель или смесь");
    this.target.setPromptText("Конечная концентрация");
    this.purity.setPromptText("Укажите, даже если 100");
    this.recipes
        .getColumns()
        .setAll(
            column("Методика", Recipe::title, 250),
            column("Цель", r -> r.target() + " · " + r.finalVolume(), 240),
            column("Ступени", r -> Integer.toString(r.series().size() + 1), 85),
            column("Приготовлено серий", r -> Long.toString(this.prepared(r)), 110));
    this.series
        .getColumns()
        .setAll(
            column("Ступень", DilutionStep::label, 160),
            column("Концентрация", s -> s.target().toString(), 150),
            column("Довести до", s -> s.finalVolume().toString(), 140));
    this.series.setPrefHeight(165);
    this.calculation
        .getColumns()
        .setAll(
            column("Ступень", r -> r.number() + ". " + r.label(), 170),
            column("Взять", r -> LaboratoryQuantityText.format(r.take(), true), 120),
            column("Довести до", r -> r.finalVolume().toString(), 120),
            column("Концентрация", r -> r.target().toString(), 130),
            column(
                "После отбора следующей",
                r -> LaboratoryQuantityText.format(r.remaining(), true),
                140));
    this.title.setId("solution-title");
    this.solvent.setId("solution-solvent");
    this.target.setId("solution-target");
    this.targetUnit.setId("solution-target-unit");
    this.volume.value.setId("solution-volume");
    this.molarMass.setId("solution-molar-mass");
    this.purity.setId("solution-purity");
    this.stock.setId("solution-stock-concentration");
    this.batch.setId("solution-batch");
    this.mode.setId("solution-mode");
    this.selectedRecipe.setId("solution-selected-recipe");
    this.selectedRecipe.setPromptText("Выберите сохранённый план");
    this.selectedRecipe.setMaxWidth(Double.MAX_VALUE);
    this.selectedRecipe.setPrefWidth(420);
    this.selectedRecipe.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(Recipe value) {
            return value == null ? "" : value.title();
          }

          public Recipe fromString(String text) {
            return null;
          }
        });
    this.selectedRecipe
        .valueProperty()
        .addListener(
            (o, before, after) -> {
              if (this.loading || after == null) {
                return;
              }
              try {
                this.edit(after);
              } catch (RuntimeException error) {
                this.loading = true;
                this.selectedRecipe.setValue(before);
                this.loading = false;
                this.status.setText(message(error));
              }
            });
    this.solidFields
        .getChildren()
        .setAll(new FlowPane(12, 8, this.massField, field("Чистота навески, %", this.purity)));
    this.dilutionFields
        .getChildren()
        .setAll(
            field("Концентрация исходного раствора", new HBox(6, this.stock, this.stockUnit)),
            button("Взять из последней стандартизации", "use-standard", this::useStandard));
    this.originDetails = details("Остаток и происхождение партии", "origin", this.origin, false);
    this.seriesDetails =
        details(
            "Серийные разведения",
            "series-details",
            new VBox(
                8,
                this.series,
                new FlowPane(
                    8,
                    8,
                    button("Добавить ступень", "step-add", () -> this.stepDialog(null)),
                    button(
                        "Изменить",
                        "step-edit",
                        () -> this.stepDialog(this.series.getSelectionModel().getSelectedItem())),
                    button(
                        "Убрать",
                        "step-remove",
                        () -> {
                          if (this.series.getSelectionModel().getSelectedItem() != null) {
                            this.series
                                .getItems()
                                .remove(this.series.getSelectionModel().getSelectedItem());
                            this.changed();
                          }
                        }),
                    button("По коэффициенту…", "serial", this::serial))),
            false);
    this.noteDetails = details("Заметки к плану", "notes", this.note, false);
    this.calculateButton = button("Рассчитать", "calculate", this::calculate);
    this.saveButton = button("Сохранить план", "save", this::saveRecipe);
    this.prepareButton = button("Записать приготовление", "post", this::post);
    this.planAmount.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
    this.planAmount.setId("solution-plan-amount");
    this.planSummary.getChildren().setAll(this.planAmount, this.hint, this.calculation);
    show(this.planSummary, false);
    this.savedPlans =
        details(
            "Использовать сохранённый план",
            "saved-plans",
            new VBox(
                8,
                this.selectedRecipe,
                new FlowPane(
                    8,
                    8,
                    button(
                        "Новый план",
                        "clear",
                        () -> {
                          this.clean();
                          this.clear();
                        }),
                    button("Повторить выбранный", "copy", this::copy))),
            false);
    this.nextTask.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
    this.nextTask.setId("solution-next-task");
    this.planPane
        .getChildren()
        .setAll(
            this.nextTask,
            this.savedPlans,
            new FlowPane(12, 8, field("Исходная партия", this.batch), field("Способ", this.mode)),
            this.stockBalance,
            new FlowPane(
                12,
                8,
                field("Нужная концентрация", new HBox(6, this.target, this.targetUnit)),
                field("Довести до объёма", this.volume.node())),
            this.solidFields,
            this.dilutionFields,
            new FlowPane(
                12, 8, field("Растворитель", this.solvent), field("Название раствора", this.title)),
            this.planSummary,
            new FlowPane(
                8,
                8,
                this.calculateButton,
                this.saveButton,
                this.prepareButton,
                button("Сбросить ввод", "reset", this::clear)),
            this.noteDetails,
            this.seriesDetails,
            this.originDetails);
    this.measuredPane.setId("solution-measured-pane");
    show(this.measuredPane, false);
    show(this.preparedPane, false);
    final VBox main = new VBox(14, this.planPane, this.measuredPane, this.preparedPane);
    main.setPadding(new Insets(16));
    main.setMaxWidth(1100);
    this.preview.setEditable(false);
    this.standards
        .getColumns()
        .setAll(
            column("Партия", s -> this.batchLabel(s.batchId()), 220),
            column("Концентрация / происхождение", s -> s.measured() + " · " + s.origin(), 260),
            column("Метод / источник", s -> s.method() + " · " + s.source(), 320));
    this.standards
        .getSelectionModel()
        .selectedItemProperty()
        .addListener((o, a, b) -> this.showStandard(b));
    this.standardBatch.setId("solution-standard-batch");
    this.standardBatch.setMaxWidth(Double.MAX_VALUE);
    this.standardBatch.setPromptText("Выберите партию раствора");
    this.standardBatch
        .valueProperty()
        .addListener(
            (o, before, after) -> {
              if (!this.loading
                  && !this.standardLoading
                  && this.standardPending()
                  && !Objects.equals(before, after)) {
                this.standardLoading = true;
                this.standardBatch.setValue(before);
                this.standardLoading = false;
                this.standardError.setText(
                    "Сначала запишите или отмените ввод концентрации для выбранной партии.");
                return;
              }
              this.standardSummary();
            });
    this.standardSummary.setId("solution-standard-summary");
    this.standardSummary.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
    this.standardTask.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
    this.standardOrigin.setId("solution-standard-origin");
    this.standardOrigin.setPromptText("Укажите происхождение значения");
    this.standardOrigin.setValue(null);
    this.standardOrigin.setMaxWidth(Double.MAX_VALUE);
    this.standardValue.setId("solution-standard-value");
    this.standardUnit.setId("solution-standard-unit");
    this.standardMethod.setId("solution-standard-method");
    this.standardSource.setId("solution-standard-source");
    this.standardError.setId("solution-standard-error");
    this.standardCorrection.setId("solution-standard-correction");
    this.standardValue.setPromptText("Значение концентрации");
    this.standardValue.setPrefColumnCount(12);
    this.standardMethod.setPromptText("Как определена концентрация");
    this.standardMethod.setPrefColumnCount(28);
    this.standardSource.setPromptText("Протокол, публикация или основание расчёта");
    this.standardDirectFields
        .getChildren()
        .setAll(
            new FlowPane(
                16,
                10,
                field("Концентрация", new HBox(7, this.standardValue, this.standardUnit)),
                field("Метод определения", this.standardMethod)));
    this.standardPrimary.value.setId("solution-standard-primary");
    this.standardPrimary.unit.setId("solution-standard-primary-unit");
    this.standardMass.setId("solution-standard-mass");
    this.standardPurity.setId("solution-standard-purity");
    this.standardTitrant.value.setId("solution-standard-titrant");
    this.standardTitrant.unit.setId("solution-standard-titrant-unit");
    this.standardBlank.value.setId("solution-standard-blank");
    this.standardBlank.unit.setId("solution-standard-blank-unit");
    this.standardRatio.setId("solution-standard-ratio");
    this.standardTitrationFields
        .getChildren()
        .setAll(
            new FlowPane(
                16,
                10,
                field("Навеска первичного стандарта", this.standardPrimary.node()),
                field("Молярная масса, г/моль", this.standardMass),
                field("Чистота стандарта, %", this.standardPurity)),
            new FlowPane(
                16,
                10,
                field("Объём титранта", this.standardTitrant.node()),
                field("Холостой опыт (пусто = 0)", this.standardBlank.node()),
                field("Моль определяемого / моль стандарта", this.standardRatio)));
    this.standardSave = button("Записать концентрацию", "standard-direct", this::saveStandard);
    this.standardSave.getStyleClass().add("laboratory-save");
    this.standardDetails =
        details(
            "Источник и расчёт выбранной записи",
            "standard-details",
            new VBox(
                8,
                this.preview,
                button("Исправить выбранную запись", "standard-correct", this::correctStandard)),
            false);
    show(this.standardDetails, false);
    this.standardRecords =
        details(
            "История концентрации",
            "standard-records",
            new VBox(8, this.standards, this.standardDetails),
            false);
    final VBox std =
        new VBox(
            14,
            this.standardTask,
            field("Партия", this.standardBatch),
            this.standardSummary,
            this.standardCorrection,
            field("Происхождение значения", this.standardOrigin),
            this.standardDirectFields,
            this.standardTitrationFields,
            field("Источник / протокол", this.standardSource),
            label(
                "Значение, происхождение и источник сохранятся в истории. Остаток партии не изменится."),
            details(
                "Другие способы и планы",
                "standard-more",
                new FlowPane(
                    8,
                    8,
                    button(
                        "Рассчитать по титрованию",
                        "standard-titrate",
                        () -> this.standardMode(true)),
                    button(
                        "Ввести готовую концентрацию",
                        "standard-enter",
                        () -> this.standardMode(false)),
                    button("История планов", "history", this::recipeHistory)),
                false),
            this.standardRecords);
    std.setPadding(new Insets(16));
    std.setMaxWidth(1100);
    final VBox standardActions =
        new VBox(
            6,
            this.standardError,
            new FlowPane(
                8,
                8,
                this.standardSave,
                button("Отменить ввод", "standard-cancel", () -> this.cancelStandard(true))));
    standardActions.setPadding(new Insets(8, 16, 12, 16));
    final BorderPane standardPage = new BorderPane(scroll(std));
    standardPage.setBottom(standardActions);
    this.tabs
        .getTabs()
        .setAll(
            new Tab("Приготовить раствор", scroll(main)),
            new Tab("Концентрация партии", standardPage));
    this.tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    this.setCenter(this.tabs);
    this.standardFields()
        .values()
        .forEach(
            control -> {
              if (control instanceof TextInputControl text) {
                text.textProperty().addListener(o -> this.standardChanged());
              } else if (control instanceof ComboBox<?> choice) {
                choice.valueProperty().addListener(o -> this.standardChanged());
              }
            });
    this.standardForm();
    this.leaveMessage.setText("Есть незаписанный ввод. Сохраните его или явно отмените.");
    this.leave
        .getChildren()
        .setAll(
            this.leaveMessage,
            new FlowPane(
                8,
                8,
                button(
                    "Отбросить ввод и выйти",
                    "discard",
                    () -> {
                      this.dirty = false;
                      this.conflict = false;
                      this.cancelStandard(false);
                      this.cancelPreparation();
                      this.finish();
                    }),
                button(
                    "Вернуться к вводу",
                    "continue",
                    () -> {
                      this.leave.setManaged(false);
                      if (this.standardPending()) {
                        this.tabs.getSelectionModel().select(1);
                        (this.standardTitration ? this.standardPrimary.value : this.standardValue)
                            .requestFocus();
                      }
                    })));
    this.leave.setManaged(false);
    this.leave.visibleProperty().bind(this.leave.managedProperty());
    final VBox footer =
        new VBox(
            8,
            this.leave,
            new FlowPane(
                8,
                8,
                button(
                    "Обновить",
                    "reload",
                    () -> {
                      this.clean();
                      this.load();
                    }),
                this.count),
            this.status);
    footer.setPadding(new Insets(10, 16, 12, 16));
    this.setBottom(footer);
    for (final TextInputControl text :
        List.of(
            this.title,
            this.solvent,
            this.target,
            this.stock,
            this.molarMass,
            this.purity,
            this.volume.value,
            this.note)) {
      text.textProperty().addListener(o -> this.changed());
    }
    for (final ComboBox<?> choice :
        List.of(this.mode, this.targetUnit, this.stockUnit, this.volume.unit, this.batch)) {
      choice.valueProperty().addListener(o -> this.changed());
    }
    this.batch
        .valueProperty()
        .addListener(
            o -> {
              this.origin();
              if (!this.loading
                  && this.inventory != null
                  && this.batch.getValue() != null
                  && this.batch.getValue().id() != null) {
                final var selected =
                    this.inventory.inventory().batches().stream()
                        .filter(b -> b.id().equals(this.batch.getValue().id()))
                        .findFirst()
                        .orElseThrow();
                this.mode.setValue(
                    selected.unit().dimension().equals("mass") ? Mode.SOLID : Mode.DILUTION);
                if (this.title.getText().isBlank()) {
                  this.title.setText(suggestedTitle(selected));
                }
                this.selectStandardBatch(selected.id());
              }
            });
    this.mode.valueProperty().addListener(o -> this.modeFields());
    this.targetUnit.valueProperty().addListener(o -> this.modeFields());
    this.modeFields();
    this.actions();
    this.addEventFilter(
        KeyEvent.KEY_PRESSED,
        e -> {
          if (e.isShortcutDown() && e.getCode() == KeyCode.ENTER) {
            this.safe(
                this.tabs.getSelectionModel().getSelectedIndex() == 1
                    ? this::saveStandard
                    : this.preparationInput == null ? this::calculate : this::postActual);
            e.consume();
          }
        });
    this.load();
  }

  void selectBatch(final String id) {
    if (this.inventory == null) {
      this.deferredBatch = id;
      return;
    }
    // Navigation supplies initial context; it must neither create input nor replace restored work.
    if (this.pending() || this.correction != null || !this.calculation.getItems().isEmpty()) {
      this.status.setText(
          "Выбор партии не изменён: сначала завершите текущий ввод или откройте новый план.");
      return;
    }
    final var selected =
        this.batch.getItems().stream().filter(b -> Objects.equals(b.id(), id)).findFirst();
    if (selected.isEmpty()) {
      this.status.setText("Переданная партия не найдена в этой книге материалов.");
      return;
    }
    final boolean wasLoading = this.loading;
    this.loading = true;
    try {
      this.batch.setValue(selected.get());
      final var stock =
          this.inventory.inventory().batches().stream()
              .filter(b -> b.id().equals(id))
              .findFirst()
              .orElseThrow();
      this.mode.setValue(stock.unit().dimension().equals("mass") ? Mode.SOLID : Mode.DILUTION);
      if (this.title.getText().isBlank()) {
        this.title.setText(suggestedTitle(stock));
      }
      this.selectStandardBatch(id);
    } finally {
      this.loading = wasLoading;
    }
    this.actions();
  }

  private static String suggestedTitle(final LaboratoryBatch source) {
    if (source.unit().dimension().equals("mass")) {
      return "Раствор «" + source.label() + "»";
    }
    String name = source.label().strip();
    boolean shortened;
    do {
      shortened = false;
      if (name.startsWith("Разведение: ")) {
        name = name.substring("Разведение: ".length()).strip();
        shortened = true;
      } else {
        for (final String prefix : List.of("Раствор «", "Разведение «")) {
          if (name.startsWith(prefix) && name.endsWith("»")) {
            name = name.substring(prefix.length(), name.length() - 1).strip();
            shortened = true;
            break;
          }
        }
      }
    } while (shortened);
    return name.isBlank() ? "Разведение раствора" : "Разведение: " + name;
  }

  private void origin() {
    this.origin.getChildren().clear();
    this.stockBalance.setText("");
    if (this.inventory != null
        && this.batch.getValue() != null
        && this.batch.getValue().id() != null) {
      final var selected =
          this.inventory.inventory().batches().stream()
              .filter(b -> b.id().equals(this.batch.getValue().id()))
              .findFirst()
              .orElseThrow();
      this.stockBalance.setText(
          "В партии доступно: " + this.inventory.inventory().balance(selected));
      this.origin
          .getChildren()
          .add(new LaboratoryBatchOriginPane(this.inventory.inventory(), selected.id()));
    }
    show(this.stockBalance, !this.stockBalance.getText().isBlank());
    if (this.originDetails != null) {
      show(this.originDetails, !this.origin.getChildren().isEmpty());
    }
  }

  private static TitledPane details(
      final String title, final String id, final Node content, final boolean expanded) {
    final var pane = new TitledPane(title, content);
    pane.setId("solution-" + id);
    pane.setExpanded(expanded);
    pane.setAnimated(false);
    return pane;
  }

  private static void show(final Node node, final boolean visible) {
    node.setVisible(visible);
    node.setManaged(visible);
  }

  private void modeFields() {
    show(this.solidFields, this.mode.getValue() == Mode.SOLID);
    show(this.dilutionFields, this.mode.getValue() == Mode.DILUTION);
    show(
        this.massField,
        this.targetUnit.getValue() != null && this.targetUnit.getValue().family().equals("molar"));
  }

  private void actions() {
    if (this.calculateButton == null) {
      return;
    }
    final boolean calculated = !this.calculation.getItems().isEmpty();
    this.saveButton.setDisable(!this.dirty || !calculated || this.conflict);
    this.prepareButton.setDisable(
        this.dirty || this.conflict || this.correction == null || this.opened == null);
    this.nextTask.setText(
        this.dirty
            ? calculated ? "Сохраните рассчитанный план" : "Рассчитайте, сколько вещества взять"
            : this.correction == null
                ? "Рассчитайте, сколько вещества взять"
                : "Готово к приготовлению — введите количества");
    show(this.saveButton, this.dirty && calculated);
    show(this.prepareButton, !this.dirty && this.correction != null);
    show(this.calculateButton, this.dirty || this.correction == null);
    for (final Button button : List.of(this.calculateButton, this.saveButton, this.prepareButton)) {
      button.getStyleClass().remove("laboratory-save");
    }
    (this.dirty
            ? calculated ? this.saveButton : this.calculateButton
            : this.correction == null ? this.calculateButton : this.prepareButton)
        .getStyleClass()
        .add("laboratory-save");
    if (!this.pending()) {
      this.leave.setManaged(false);
    }
  }

  private Button button(final String text, final String id, final Runnable action) {
    final Button b = new Button(text);
    b.setId("solution-" + id);
    b.setOnAction(e -> this.safe(action));
    return b;
  }

  private void safe(final Runnable action) {
    if (this.running) {
      this.status.setText("Дождитесь завершения записи или чтения.");
      return;
    }
    try {
      action.run();
    } catch (final RuntimeException error) {
      this.status.setText(message(error));
    }
  }

  private void changed() {
    if (!this.loading) {
      this.dirty = true;
      this.calculation.getItems().clear();
      this.hint.setText("");
      show(this.planSummary, false);
      this.status.setText("План изменён. Рассчитайте нужное количество перед сохранением.");
      this.actions();
    }
  }

  private void clean() {
    if (this.pending()) {
      this.canClose();
      throw new IllegalArgumentException(
          this.standardPending()
              ? "Сначала запишите или отмените ввод концентрации концентрации."
              : this.preparationInput == null
                  ? "Сначала сохраните или сбросьте текущий план."
                  : "Завершите или отмените ввод фактического приготовления.");
    }
  }

  private <T> void background(final Callable<T> task, final Consumer<T> done) {
    this.background(task, done, error -> this.status.setText(message(error)));
  }

  private <T> void background(
      final Callable<T> task, final Consumer<T> done, final Consumer<Throwable> failed) {
    this.busy(true);
    this.tabs.setDisable(true);
    this.worker.submit(
        () -> {
          try {
            final T result = task.call();
            Platform.runLater(
                () -> {
                  if (this.disposed) {
                    return;
                  }
                  this.busy(false);
                  this.tabs.setDisable(false);
                  try {
                    done.accept(result);
                  } catch (final RuntimeException error) {
                    failed.accept(error);
                  }
                });
          } catch (final Exception error) {
            Platform.runLater(
                () -> {
                  if (!this.disposed) {
                    this.busy(false);
                    this.tabs.setDisable(false);
                    failed.accept(error);
                  }
                });
          }
        });
  }

  private void load() {
    this.background(
        () -> {
          final var book = this.book.read();
          final Path stock = this.folder.resolve("materials.qchem");
          if (Files.exists(stock, LinkOption.NOFOLLOW_LINKS)
              && Files.size(stock) > 32L * 1024 * 1024) {
            throw new IllegalArgumentException("Учёт материалов превышает 32 МиБ.");
          }
          return new Object[] {book, new LaboratoryInventoryRepository().read(stock)};
        },
        result -> {
          this.opened = (LaboratoryWorkbenchBooks.Opened<LaboratorySolutionBook>) result[0];
          this.inventory = (LaboratoryInventoryRepository.Opened) result[1];
          this.loading = true;
          this.batch.getItems().setAll(new Batch(null, "Без выбранной партии"));
          this.inventory
              .inventory()
              .batches()
              .forEach(
                  b ->
                      this.batch
                          .getItems()
                          .add(
                              new Batch(
                                  b.id(),
                                  b.label() + " · " + this.inventory.inventory().balance(b))));
          this.batch.getSelectionModel().selectFirst();
          this.loading = false;
          this.refresh();
          this.refreshStandardBatches();
          if (this.deferred != null) {
            final var data = this.deferred;
            this.deferred = null;
            this.draft(data, true);
          }
          if (this.deferredBatch != null) {
            final String id = this.deferredBatch;
            this.deferredBatch = null;
            this.selectBatch(id);
          }
          this.origin();
          this.refreshStandardBatches();
          if (!this.conflict && !this.standardConflict && this.preparationInput == null) {
            this.status.setText("Планы и склад прочитаны. План не меняет остаток.");
          }
        });
  }

  private void refresh() {
    final boolean wasLoading = this.loading;
    this.loading = true;
    try {
      this.recipes.getItems().setAll(this.opened.value().activeRecipes());
      this.selectedRecipe.getItems().setAll(this.recipes.getItems());
      this.selectedRecipe.setValue(
          this.recipes.getItems().stream()
              .filter(r -> Objects.equals(r.id(), this.correction))
              .findFirst()
              .orElse(null));
      this.recipes.getSelectionModel().select(this.selectedRecipe.getValue());
      this.standards.getItems().setAll(this.opened.value().standardizations());
      this.count.setText("Планов: " + this.recipes.getItems().size());
      this.standardSummary();
      if (this.standardRecords != null) {
        this.standardRecords.setText(
            "История концентрации (" + this.standards.getItems().size() + ")");
      }
    } finally {
      this.loading = wasLoading;
    }
    this.preparedConcentrations.forEach((id, label) -> label.setText(this.concentrationText(id)));
    this.standardForm();
    this.actions();
  }

  private Recipe value() {
    return new Recipe(
        LaboratorySolutionPlanning.id("recipe"),
        Instant.now(),
        this.title.getText(),
        this.batch.getValue() == null ? null : this.batch.getValue().id(),
        this.mode.getValue(),
        this.solvent.getText(),
        new Concentration(number(this.target, true), this.targetUnit.getValue()),
        this.volume.get(true),
        this.mode.getValue() == Mode.SOLID && this.targetUnit.getValue().family().equals("molar")
            ? number(this.molarMass, true)
            : null,
        this.mode.getValue() == Mode.SOLID ? number(this.purity, true) : null,
        this.mode.getValue() == Mode.DILUTION
            ? new Concentration(number(this.stock, true), this.stockUnit.getValue())
            : null,
        this.series.getItems(),
        this.note.getText(),
        this.correction);
  }

  private static String plannedAmount(final LaboratoryQuantity quantity) {
    final var unit =
        quantity.unit().dimension().equals("mass")
            ? quantity.in(LaboratoryQuantityUnit.GRAM).value().abs().compareTo(BigDecimal.ONE) < 0
                ? LaboratoryQuantityUnit.MILLIGRAM
                : LaboratoryQuantityUnit.GRAM
            : quantity.unit().dimension().equals("volume")
                ? LaboratoryQuantityUnit.MILLILITRE
                : quantity.unit();
    final var value = quantity.in(unit).value();
    final var shown = value.round(new java.math.MathContext(5, java.math.RoundingMode.HALF_UP));
    return (shown.compareTo(value) == 0 ? "" : "≈ ")
        + shown.stripTrailingZeros().toPlainString()
        + " "
        + unit;
  }

  private void calculate() {
    final var result = LaboratorySolutionPlanning.plan(this.value());
    this.calculation.getItems().setAll(result.rows());
    final var row = result.rows().getFirst();
    this.planAmount.setText(
        "Взять " + plannedAmount(row.take()) + " · довести до " + row.finalVolume());
    this.planAmount.setTooltip(
        new Tooltip(
            "Расчётное количество: "
                + LaboratoryQuantityText.format(row.take(), false)
                + ". Это план, не измеренный отбор."));
    this.hint.setText(
        result.rows().size() == 1
            ? "План без списания. Доведите до указанного конечного объёма."
            : "Серия: "
                + result.rows().size()
                + " ступеней. Остатки учитывают отбор в следующую ступень.");
    show(this.calculation, result.rows().size() > 1);
    this.calculation.setPrefHeight(Math.min(360, 45 + result.rows().size() * 36));
    show(this.planSummary, true);
    this.status.setText("Расчёт готов. Остатки партий не изменены.");
    this.actions();
  }

  private void saveRecipe() {
    if (this.standardPending()) {
      throw new IllegalArgumentException(
          "Сначала запишите или отмените ввод концентрации концентрации.");
    }
    if (this.opened == null || this.conflict) {
      throw new IllegalArgumentException(
          "Книга ещё не прочитана или черновик относится к другой версии.");
    }
    if (!this.dirty && this.correction != null) {
      this.status.setText("Методика не изменена.");
      return;
    }
    final var recipe = this.value();
    LaboratorySolutionPlanning.plan(recipe);
    final var next = this.opened.value().append(recipe);
    final String expected = this.opened.fingerprint();
    this.background(
        () -> this.book.write(next, expected),
        saved -> {
          this.opened = saved;
          this.correction = recipe.id();
          this.dirty = false;
          this.refresh();
          this.recipes.getSelectionModel().select(recipe);
          this.calculate();
          this.status.setText(
              "План сохранён. Можно ввести количества приготовления; остаток партии пока не менялся.");
          this.actions();
        });
  }

  private void edit(final Recipe r) {
    if (r == null) {
      throw new IllegalArgumentException("Выберите методику.");
    }
    this.clean();
    this.loading = true;
    try {
      this.title.setText(r.title());
      this.solvent.setText(r.solvent());
      this.mode.setValue(r.mode());
      this.target.setText(r.target().value().toPlainString());
      this.targetUnit.setValue(r.target().unit());
      this.volume.set(r.finalVolume());
      this.molarMass.setText(r.molarMass() == null ? "" : r.molarMass().toPlainString());
      this.purity.setText(r.purityPercent() == null ? "" : r.purityPercent().toPlainString());
      this.stock.setText(r.stock() == null ? "" : r.stock().value().toPlainString());
      if (r.stock() != null) {
        this.stockUnit.setValue(r.stock().unit());
      }
      this.batch.setValue(
          this.batch.getItems().stream()
              .filter(b -> Objects.equals(b.id(), r.batchId()))
              .findFirst()
              .orElse(this.batch.getItems().getFirst()));
      this.series.getItems().setAll(r.series());
      this.note.setText(r.note());
      this.correction = r.id();
      this.dirty = false;
      this.recipes.getSelectionModel().select(r);
      this.selectedRecipe.setValue(r);
      this.seriesDetails.setExpanded(!r.series().isEmpty());
      this.noteDetails.setExpanded(!r.note().isBlank());
      this.modeFields();
      this.savedPlans.setExpanded(false);
      this.selectStandardBatch(r.batchId());
      this.calculate();
    } finally {
      this.loading = false;
    }
  }

  private void copy() {
    this.edit(this.recipes.getSelectionModel().getSelectedItem());
    this.correction = null;
    this.title.setText(this.title.getText() + " · новая");
    this.dirty = true;
    this.actions();
  }

  private void clear() {
    if (this.preparationInput != null) {
      throw new IllegalArgumentException("Сначала отмените ввод приготовления.");
    }
    this.loading = true;
    try {
      for (final TextInputControl text :
          List.of(
              this.title,
              this.solvent,
              this.target,
              this.stock,
              this.molarMass,
              this.purity,
              this.volume.value,
              this.note)) {
        text.clear();
      }
      this.mode.setValue(Mode.SOLID);
      this.batch.getSelectionModel().selectFirst();
      this.selectedRecipe.setValue(null);
      this.recipes.getSelectionModel().clearSelection();
      this.series.getItems().clear();
      this.calculation.getItems().clear();
      this.correction = null;
      this.dirty = false;
      this.conflict = false;
      this.retained = null;
      this.hint.setText("");
      this.seriesDetails.setExpanded(false);
      this.noteDetails.setExpanded(false);
      show(this.planSummary, false);
      show(this.preparedPane, false);
      show(this.planPane, true);
    } finally {
      this.loading = false;
    }
    this.modeFields();
    this.origin();
    this.actions();
    this.savedPlans.setExpanded(false);
    this.status.setText("Ввод сброшен. Сохранённые планы и остатки не изменены.");
  }

  private void stepDialog(final DilutionStep before) {
    final TextField title = new TextField(before == null ? "" : before.label()),
        target = new TextField(before == null ? "" : before.target().value().toPlainString());
    final var unit = choice(ConcentrationUnit.values());
    unit.setValue(before == null ? this.targetUnit.getValue() : before.target().unit());
    final var volume = new Quantity("volume", LaboratoryQuantityUnit.MILLILITRE);
    if (before != null) {
      volume.set(before.finalVolume());
    }
    dialog(
        this,
        "Ступень разведения",
        "Добавить в план",
        List.of(
            field("Название", title),
            field("Конечная концентрация", new HBox(6, target, unit)),
            field("Конечный объём", volume.node())),
        () -> {
          final var next =
              new DilutionStep(
                  title.getText(),
                  new Concentration(number(target, true), unit.getValue()),
                  volume.get(true));
          if (before == null) {
            if (this.series.getItems().size() >= 100) {
              throw new IllegalArgumentException("Предел — 100 ступеней.");
            }
            this.series.getItems().add(next);
          } else {
            this.series.getItems().set(this.series.getItems().indexOf(before), next);
          }
          this.changed();
        });
  }

  private void serial() {
    final TextField count = new TextField(), factor = new TextField();
    final Quantity volume = new Quantity("volume", LaboratoryQuantityUnit.MILLILITRE);
    dialog(
        this,
        "Последовательная серия",
        "Создать ступени",
        List.of(
            field("Дополнительных ступеней (1–100)", count),
            field("Кратность разведения каждой ступени (>1)", factor),
            field("Конечный объём каждой ступени", volume.node()),
            label("Текущая несохранённая серия будет заменена.")),
        () -> {
          final int n = number(count, true).intValueExact();
          final var ratio = number(factor, true);
          if (n < 1 || n > 100 || ratio.compareTo(BigDecimal.ONE) <= 0) {
            throw new IllegalArgumentException("Проверьте число ступеней и кратность.");
          }
          BigDecimal concentration = number(this.target, true);
          final List<DilutionStep> rows = new ArrayList<>();
          for (int i = 1; i <= n; i++) {
            concentration = concentration.divide(ratio, new java.math.MathContext(15));
            rows.add(
                new DilutionStep(
                    "Разведение " + i,
                    new Concentration(concentration, this.targetUnit.getValue()),
                    volume.get(true)));
          }
          this.series.getItems().setAll(rows);
          this.changed();
        });
  }

  private void useStandard() {
    if (this.opened == null
        || this.batch.getValue() == null
        || this.batch.getValue().id() == null) {
      throw new IllegalArgumentException("Выберите исходную партию.");
    }
    final var value =
        this.opened
            .value()
            .latest(this.batch.getValue().id())
            .orElseThrow(() -> new IllegalArgumentException("Для партии нет стандартизации."));
    this.stock.setText(value.measured().value().toPlainString());
    this.stockUnit.setValue(value.measured().unit());
    this.mode.setValue(Mode.DILUTION);
    this.status.setText(
        "Использована запись "
            + value.id()
            + " от "
            + standardTime(value.time())
            + " · "
            + value.origin()
            + ": "
            + value.source());
  }

  private String batchLabel(final String id) {
    return this.inventory == null
        ? id
        : this.inventory.inventory().batches().stream()
            .filter(b -> b.id().equals(id))
            .map(LaboratoryBatch::label)
            .findFirst()
            .orElse("Партия отсутствует · " + id);
  }

  private void showStandard(final Standardization s) {
    if (this.standardDetails != null) {
      show(this.standardDetails, s != null);
      this.standardDetails.setExpanded(s != null);
    }
    this.preview.setText(
        s == null
            ? ""
            : s.measured()
                + "\nПроисхождение: "
                + s.origin()
                + "\nID записи: "
                + s.id()
                + "\n"
                + s.method()
                + "\n"
                + s.time()
                + "\nИсточник: "
                + s.source()
                + "\n"
                + s.calculation()
                + (s.correctsId() == null ? "" : "\nИсправляет: " + s.correctsId()));
  }

  private Map<String, Control> standardFields() {
    final Map<String, Control> fields = new LinkedHashMap<>();
    fields.put("origin", this.standardOrigin);
    fields.put("value", this.standardValue);
    fields.put("unit", this.standardUnit);
    fields.put("method", this.standardMethod);
    fields.put("sourceText", this.standardSource);
    fields.put("primary", this.standardPrimary.value);
    fields.put("primaryUnit", this.standardPrimary.unit);
    fields.put("mass", this.standardMass);
    fields.put("purity", this.standardPurity);
    fields.put("titrant", this.standardTitrant.value);
    fields.put("titrantUnit", this.standardTitrant.unit);
    fields.put("blank", this.standardBlank.value);
    fields.put("blankUnit", this.standardBlank.unit);
    fields.put("ratio", this.standardRatio);
    return fields;
  }

  private boolean standardPending() {
    return this.standardDirty || this.standardConflict || this.standardCorrects != null;
  }

  private void standardChanged() {
    if (!this.loading && !this.standardLoading) {
      if (!this.standardDirty) {
        this.standardFingerprint =
            this.opened == null ? "" : Objects.toString(this.opened.fingerprint(), "");
      }
      this.standardDirty = true;
      this.standardError.setText("");
      this.status.setText("Концентрация ещё не записана.");
    }
  }

  private void standardForm() {
    show(this.standardDirectFields, !this.standardTitration);
    show(this.standardTitrationFields, this.standardTitration);
    show(this.standardCorrection, this.standardCorrects != null);
    this.standardTask.setText(
        this.standardCorrects != null
            ? "Исправьте запись — исходная версия останется в истории"
            : this.standardTitration
                ? "Рассчитайте концентрацию по титрованию"
                : "Запишите концентрацию и её происхождение");
    if (this.standardSave != null) {
      this.standardSave.setText(
          this.standardTitration ? "Рассчитать и записать концентрацию" : "Записать концентрацию");
      this.standardSave.setDisable(this.opened == null || this.standardConflict);
    }
  }

  private void standardMode(final boolean titration) {
    if (this.standardTitration == titration) {
      return;
    }
    if (this.standardPending()) {
      throw new IllegalArgumentException(
          "Сначала запишите или отмените текущий ввод концентрации.");
    }
    this.standardTitration = titration;
    this.standardForm();
  }

  private void cancelStandard(final boolean announce) {
    this.standardLoading = true;
    try {
      this.standardFields()
          .values()
          .forEach(
              control -> {
                if (control instanceof TextInputControl text) {
                  text.clear();
                }
              });
      this.standardOrigin.setValue(null);
      this.standardUnit.setValue(ConcentrationUnit.MOL_PER_LITRE);
      this.standardPrimary.unit.setValue(LaboratoryQuantityUnit.GRAM);
      this.standardTitrant.unit.setValue(LaboratoryQuantityUnit.MILLILITRE);
      this.standardBlank.unit.setValue(LaboratoryQuantityUnit.MILLILITRE);
      this.standardDirty = false;
      this.standardConflict = false;
      this.standardCorrects = null;
      this.standardFingerprint = null;
      this.standardRetained = null;
      this.standardCorrection.setText("");
      this.standardError.setText("");
    } finally {
      this.standardLoading = false;
    }
    this.standardForm();
    this.actions();
    if (announce) {
      this.status.setText("Ввод концентрации отменён. История и остатки не изменены.");
    }
  }

  private void correctStandard() {
    if (this.standardPending()) {
      throw new IllegalArgumentException(
          "Сначала запишите или отмените текущий ввод концентрации.");
    }
    final var selected = this.standards.getSelectionModel().getSelectedItem();
    if (selected == null) {
      throw new IllegalArgumentException("Выберите запись в истории концентрации.");
    }
    if (this.opened.value().standardizations().stream()
        .anyMatch(s -> selected.id().equals(s.correctsId()))) {
      throw new IllegalArgumentException(
          "Эта запись уже исправлена. Выберите её актуальное исправление в истории.");
    }
    final var selectedBatch =
        this.standardBatch.getItems().stream()
            .filter(b -> b.id().equals(selected.batchId()))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Исходная партия этой записи отсутствует в учёте."));
    this.standardLoading = true;
    try {
      this.standardTitration = false;
      this.standardBatch.setValue(selectedBatch);
      this.standardValue.setText(selected.measured().value().toPlainString());
      this.standardUnit.setValue(selected.measured().unit());
      this.standardOrigin.setValue(selected.origin());
      this.standardMethod.setText(selected.method());
      this.standardSource.setText(selected.source());
      this.standardCorrects = selected.id();
      this.standardDirty = true;
      this.standardFingerprint = Objects.toString(this.opened.fingerprint(), "");
      this.standardCorrection.setText(standardCorrectionText(selected));
    } finally {
      this.standardLoading = false;
    }
    this.standardRecords.setExpanded(false);
    this.standardForm();
    this.standardValue.requestFocus();
    this.status.setText("Исправление подготовлено. Проверьте значения и источник перед записью.");
  }

  private static String standardTime(final Instant time) {
    return time.atZone(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
  }

  private static String standardCorrectionText(final Standardization value) {
    return "Исправляется запись от "
        + standardTime(value.time())
        + " · "
        + value.measured()
        + " · "
        + value.origin();
  }

  private BigDecimal standardNumber(
      final TextInputControl field, final String name, final boolean allowZero) {
    try {
      final var value = number(field, true);
      if (allowZero && value.signum() == 0) {
        return value;
      }
      return LaboratorySolutionBook.positive(value);
    } catch (RuntimeException error) {
      throw invalidField(
          field, name + ": " + (field.getText().isBlank() ? "заполните поле." : message(error)));
    }
  }

  private LaboratoryQuantity standardQuantity(
      final Quantity quantity, final String name, final boolean optionalZero) {
    if (optionalZero && quantity.value.getText().isBlank()) {
      return null;
    }
    if (quantity.unit.getValue() == null) {
      throw invalidField(quantity.unit, name + ": выберите единицу.");
    }
    return new LaboratoryQuantity(
        this.standardNumber(quantity.value, name, optionalZero), quantity.unit.getValue());
  }

  private String standardText(final TextInputControl field, final String name, final int limit) {
    if (field.getText().isBlank() || field.getText().length() > limit) {
      throw invalidField(field, name + ": заполните поле (до " + limit + " знаков).");
    }
    return field.getText();
  }

  private void saveStandard() {
    try {
      if (this.opened == null || this.standardConflict) {
        throw new IllegalArgumentException(
            "Запись концентрации относится к другой версии книги. Ввод сохранён; отмените его и обновите книгу перед новой записью.");
      }
      if (this.preparationInput != null) {
        throw new IllegalArgumentException(
            "Сначала завершите или отмените ввод приготовления на первой вкладке.");
      }
      final var selected = this.standardBatch.getValue();
      if (selected == null
          || this.inventory.inventory().batches().stream()
              .noneMatch(b -> b.id().equals(selected.id()))) {
        throw invalidField(this.standardBatch, "Партия: выберите существующую партию раствора.");
      }
      if (this.standardPending()
          && !Objects.toString(this.standardFingerprint, "")
              .equals(Objects.toString(this.opened.fingerprint(), ""))) {
        this.standardConflict = true;
        this.standardForm();
        throw new IllegalArgumentException(
            "Книга изменилась после начала ввода. Ввод сохранён в черновике; запись заблокирована.");
      }
      final Origin provenance = this.standardOrigin.getValue();
      if (provenance == null
          || provenance == Origin.UNSPECIFIED
              && (this.standardCorrects == null
                  || this.opened.value().standardizations().stream()
                      .noneMatch(
                          s ->
                              s.id().equals(this.standardCorrects)
                                  && s.origin() == Origin.UNSPECIFIED))) {
        throw invalidField(
            this.standardOrigin,
            "Укажите происхождение концентрации: измерение, литература или расчёт / модель. Неизвестное происхождение сохраняется только при исправлении старой записи.");
      }
      final Standardization value;
      if (this.standardTitration) {
        final var primary = this.standardQuantity(this.standardPrimary, "Навеска стандарта", false);
        final var mass = this.standardNumber(this.standardMass, "Молярная масса", false);
        final var purity = this.standardNumber(this.standardPurity, "Чистота стандарта", false);
        if (purity.compareTo(BigDecimal.valueOf(100)) > 0) {
          throw invalidField(this.standardPurity, "Чистота стандарта: не более 100 %.");
        }
        final var titrant = this.standardQuantity(this.standardTitrant, "Объём титранта", false);
        final var blank = this.standardQuantity(this.standardBlank, "Холостой опыт", true);
        final var ratio = this.standardNumber(this.standardRatio, "Мольное отношение", false);
        if (blank != null && blank.in(titrant.unit()).value().compareTo(titrant.value()) >= 0) {
          throw invalidField(
              this.standardBlank.value, "Холостой опыт: объём должен быть меньше объёма титранта.");
        }
        value =
            LaboratorySolutionPlanning.titrate(
                selected.id(),
                primary,
                mass,
                purity,
                titrant,
                blank,
                ratio,
                this.standardText(this.standardSource, "Источник / протокол", 3000),
                this.standardCorrects,
                provenance);
      } else {
        final var measured = this.standardNumber(this.standardValue, "Концентрация", false);
        if (this.standardUnit.getValue() == null) {
          throw invalidField(this.standardUnit, "Концентрация: выберите единицу.");
        }
        value =
            new Standardization(
                LaboratorySolutionPlanning.id("standard"),
                selected.id(),
                Instant.now(),
                new Concentration(measured, this.standardUnit.getValue()),
                this.standardText(this.standardMethod, "Метод определения", 500),
                this.standardText(this.standardSource, "Источник / протокол", 3000),
                "",
                this.standardCorrects,
                provenance);
      }
      final var next = this.opened.value().append(value);
      final String expected = this.opened.fingerprint();
      this.standardError.setText("Сохраняю концентрацию…");
      this.background(
          () -> this.book.write(next, expected),
          saved -> {
            this.opened = saved;
            this.cancelStandard(false);
            this.refresh();
            this.selectStandardBatch(value.batchId());
            this.standards.getSelectionModel().select(value);
            this.status.setText(
                "Концентрация записана: "
                    + value.measured()
                    + " · "
                    + value.origin()
                    + ". Источник сохранён, остаток партии не изменился.");
          },
          error -> {
            this.standardConflict = true;
            this.standardForm();
            this.standardError.setText("Концентрация не записана. " + message(error));
            this.status.setText(this.standardError.getText());
          });
    } catch (RuntimeException error) {
      this.standardError.setText(message(error));
      this.status.setText(message(error));
    }
  }

  private void standardDraft(final Properties data, final boolean restore) {
    final String prefix = "solution.standard";
    if (!restore) {
      if (this.standardRetained != null) {
        this.standardRetained.forEach(data::put);
        return;
      }
      LaboratoryFormDraft.fields(data, prefix, false, this.standardFields());
      data.setProperty(prefix + ".version", "2");
      data.setProperty(
          prefix + ".origin",
          this.standardOrigin.getValue() == null ? "" : this.standardOrigin.getValue().name());
      data.setProperty(prefix + ".titration", Boolean.toString(this.standardTitration));
      data.setProperty(prefix + ".dirty", Boolean.toString(this.standardDirty));
      data.setProperty(prefix + ".conflict", Boolean.toString(this.standardConflict));
      data.setProperty(
          prefix + ".batch",
          this.standardBatch.getValue() == null ? "" : this.standardBatch.getValue().id());
      data.setProperty(prefix + ".corrects", Objects.toString(this.standardCorrects, ""));
      data.setProperty(prefix + ".fingerprint", Objects.toString(this.standardFingerprint, ""));
      data.setProperty(
          "solution.tab", Integer.toString(this.tabs.getSelectionModel().getSelectedIndex()));
      return;
    }
    if (!data.containsKey(prefix + ".version")) {
      return;
    }
    this.standardLoading = true;
    try {
      if (!Set.of("1", "2").contains(data.getProperty(prefix + ".version"))) {
        throw new IllegalArgumentException("Неизвестная версия черновика концентрации.");
      }
      this.standardOrigin.setValue(null);
      if ("2".equals(data.getProperty(prefix + ".version"))
          && !data.containsKey(prefix + ".origin")) {
        throw new IllegalArgumentException(
            "Черновик концентрации не содержит поле происхождения. Исходный ввод сохранён.");
      }
      LaboratoryFormDraft.fields(data, prefix, true, this.standardFields());
      this.standardTitration = Boolean.parseBoolean(data.getProperty(prefix + ".titration"));
      this.standardDirty = Boolean.parseBoolean(data.getProperty(prefix + ".dirty"));
      this.standardCorrects =
          data.getProperty(prefix + ".corrects", "").isEmpty()
              ? null
              : data.getProperty(prefix + ".corrects");
      this.standardFingerprint = data.getProperty(prefix + ".fingerprint", "");
      final String batch = data.getProperty(prefix + ".batch", "");
      this.standardBatch.setValue(
          this.standardBatch.getItems().stream()
              .filter(b -> b.id().equals(batch))
              .findFirst()
              .orElse(null));
      this.standardConflict =
          Boolean.parseBoolean(data.getProperty(prefix + ".conflict"))
              || !batch.isEmpty() && this.standardBatch.getValue() == null
              || this.standardPending()
                  && !Objects.toString(this.opened.fingerprint(), "")
                      .equals(this.standardFingerprint);
      if (this.standardCorrects != null) {
        final var old =
            this.opened.value().standardizations().stream()
                .filter(v -> v.id().equals(this.standardCorrects) && v.batchId().equals(batch))
                .findFirst();
        this.standardConflict |= old.isEmpty();
        this.standardCorrection.setText(
            old.map(LaboratorySolutionPage::standardCorrectionText)
                .orElse("Исходная запись исправления не найдена"));
      }
      for (final var field : this.standardFields().entrySet()) {
        if (field.getValue() instanceof ComboBox<?> combo
            && data.containsKey(prefix + "." + field.getKey())
            && !(field.getKey().equals("origin")
                && data.getProperty(prefix + "." + field.getKey()).isEmpty())
            && combo.getItems().stream()
                .filter(Enum.class::isInstance)
                .map(v -> ((Enum<?>) v).name())
                .noneMatch(data.getProperty(prefix + "." + field.getKey())::equals)) {
          this.standardConflict = true;
        }
      }
      this.tabs
          .getSelectionModel()
          .select(
              this.standardPending()
                  ? 1
                  : Math.max(
                      0, Math.min(1, Integer.parseInt(data.getProperty("solution.tab", "0")))));
      this.standardForm();
      if (this.standardConflict) {
        throw new IllegalArgumentException(
            "Черновик концентрации относится к другой версии. Ввод сохранён; запись заблокирована.");
      }
    } catch (RuntimeException error) {
      this.standardConflict = true;
      this.standardRetained = new Properties();
      data.stringPropertyNames().stream()
          .filter(k -> k.startsWith(prefix + "."))
          .forEach(k -> this.standardRetained.setProperty(k, data.getProperty(k)));
      this.standardRetained.setProperty(prefix + ".conflict", "true");
      this.standardError.setText(message(error));
      this.status.setText(message(error));
      this.tabs.getSelectionModel().select(1);
      this.standardForm();
    } finally {
      this.standardLoading = false;
    }
  }

  private void post() {
    this.clean();
    final var recipe =
        this.opened.value().recipes().stream()
            .filter(r -> r.id().equals(this.correction))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException("Сначала рассчитайте и сохраните план."));
    this.openPreparation(recipe);
    this.tabs.getSelectionModel().selectFirst();
  }

  private void openPreparation(final Recipe recipe) {
    final var input = new PreparationInput(recipe);
    this.preparationInput = input;
    final var rows = LaboratorySolutionPlanning.plan(recipe).rows();
    input
        .source
        .getItems()
        .setAll(this.batch.getItems().stream().filter(b -> b.id() != null).toList());
    input.source.setValue(
        input.source.getItems().stream()
            .filter(b -> Objects.equals(b.id(), recipe.batchId()))
            .findFirst()
            .orElse(null));
    input.source.setMaxWidth(Double.MAX_VALUE);
    input.source.setId("solution-actual-source");
    final Label heading = label("Приготовление: " + recipe.title());
    heading.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
    this.measuredPane
        .getChildren()
        .setAll(
            heading,
            label("Введите записываемые количества. Пустые поля не заполняются планом."),
            field("Откуда взяли вещество", input.source));
    for (int i = 0; i < rows.size(); i++) {
      final boolean solid = i == 0 && recipe.mode() == Mode.SOLID;
      final var take =
          new Quantity(
              solid ? "mass" : "volume",
              solid ? LaboratoryQuantityUnit.MILLIGRAM : LaboratoryQuantityUnit.MILLILITRE);
      final var finalVolume = new Quantity("volume", LaboratoryQuantityUnit.MILLILITRE);
      take.value.setId("solution-actual-taken-" + i);
      finalVolume.value.setId("solution-actual-volume-" + i);
      input.taken.add(take);
      input.volumes.add(finalVolume);
      this.measuredPane
          .getChildren()
          .add(
              new VBox(
                  7,
                  label(
                      (rows.size() == 1 ? "" : "Ступень " + (i + 1) + ": ") + rows.get(i).label()),
                  label(
                      "По плану: взять "
                          + plannedAmount(rows.get(i).take())
                          + ", довести до "
                          + rows.get(i).finalVolume()),
                  new FlowPane(
                      12,
                      8,
                      field(solid ? "Взвешено" : "Отобрано", take.node()),
                      field("Полученный конечный объём", finalVolume.node()))));
    }
    input.reason.setId("solution-actual-reason");
    input.reason.setPromptText("Например: навеска по весам, объём по мерной колбе…");
    input.location.setId("solution-actual-location");
    input.error.setId("solution-actual-error");
    final Button commit = button("Записать приготовление", "post-commit", this::postActual);
    commit.getStyleClass().add("laboratory-save");
    this.measuredPane
        .getChildren()
        .addAll(
            field("Основание / наблюдения (обязательно)", input.reason),
            details("Место полученных растворов", "actual-location-details", input.location, false),
            label(
                "Ранее по этой версии плана записано приготовлений: "
                    + this.prepared(recipe)
                    + ". Новая запись спишет исходную партию и создаст отдельные партии растворов."),
            new FlowPane(
                8,
                8,
                commit,
                button("Отменить ввод приготовления", "post-cancel", this::cancelPreparation)),
            input.error);
    for (final Control control : input.fields().values()) {
      if (control instanceof TextInputControl text) {
        text.textProperty().addListener(o -> this.actualChanged(input));
      } else if (control instanceof ComboBox<?> choice) {
        choice.valueProperty().addListener(o -> this.actualChanged(input));
      }
    }
    input.source.valueProperty().addListener(o -> this.actualChanged(input));
    this.planPane.setDisable(true);
    show(this.planPane, false);
    show(this.measuredPane, true);
    show(this.preparedPane, false);
    this.status.setText("Количества приготовления ещё не записаны.");
  }

  private void actualChanged(final PreparationInput input) {
    if (!this.loading && !this.conflict && this.preparationInput == input) {
      input.error.setText("");
      this.status.setText("Количества приготовления ещё не записаны.");
    }
  }

  private void cancelPreparation() {
    this.preparationInput = null;
    this.measuredPane.getChildren().clear();
    show(this.measuredPane, false);
    this.planPane.setDisable(false);
    show(this.planPane, true);
    this.retained = null;
    this.leave.setManaged(false);
    this.status.setText(
        this.conflict
            ? "Ввод приготовления отменён. Сбросьте план и обновите книгу перед новой записью."
            : "Ввод приготовления отменён. Остатки партий не изменены.");
    this.actions();
  }

  private static IllegalArgumentException invalidField(final Control field, final String text) {
    field.requestFocus();
    if (field instanceof TextInputControl input) {
      input.selectAll();
    }
    return new IllegalArgumentException(text);
  }

  private LaboratoryQuantity measuredQuantity(
      final PreparationInput input, final int index, final boolean finalVolume) {
    final Quantity field = finalVolume ? input.volumes.get(index) : input.taken.get(index);
    final String name =
        (input.taken.size() > 1 ? "Ступень " + (index + 1) + " — " : "")
            + (finalVolume
                ? "Конечный объём"
                : index == 0 && input.recipe.mode() == Mode.SOLID ? "Взвешено" : "Отобрано");
    if (field.value.getText().isBlank()) {
      throw invalidField(field.value, name + ": введите количество.");
    }
    if (field.unit.getValue() == null) {
      throw invalidField(field.unit, name + ": выберите единицу измерения.");
    }
    try {
      final var quantity = field.get(true);
      if (quantity.value().signum() <= 0) {
        throw new IllegalArgumentException("Количество должно быть больше нуля.");
      }
      return quantity;
    } catch (RuntimeException error) {
      throw invalidField(field.value, name + ": " + message(error));
    }
  }

  private void postActual() {
    final var input = this.preparationInput;
    if (input == null) {
      return;
    }
    try {
      if (this.conflict || !Objects.equals(input.fingerprint, this.inventory.fingerprint())) {
        throw new IllegalArgumentException(
            "Учёт материалов изменился. Проверьте сохранённый ввод; отмените его и обновите данные перед новой записью.");
      }
      if (input.source.getValue() == null) {
        throw invalidField(input.source, "Исходная партия: выберите, откуда взяли вещество.");
      }
      final var source =
          this.inventory.inventory().batches().stream()
              .filter(b -> b.id().equals(input.source.getValue().id()))
              .findFirst()
              .orElseThrow(
                  () ->
                      invalidField(
                          input.source, "Исходная партия отсутствует. Обновите учёт материалов."));
      if (!source
          .unit()
          .dimension()
          .equals(input.recipe.mode() == Mode.SOLID ? "mass" : "volume")) {
        throw invalidField(
            input.source,
            input.recipe.mode() == Mode.SOLID
                ? "Исходная партия: для навески выберите партию с остатком в единицах массы."
                : "Исходная партия: для разведения выберите раствор с остатком в единицах объёма.");
      }
      final List<LaboratorySolutionPlanning.Measured> actuals = new ArrayList<>();
      for (int i = 0; i < input.taken.size(); i++) {
        actuals.add(
            new LaboratorySolutionPlanning.Measured(
                this.measuredQuantity(input, i, false), this.measuredQuantity(input, i, true)));
      }
      if (input.reason.getText().isBlank()) {
        throw invalidField(
            input.reason,
            "Основание / наблюдения: укажите, на каком основании записано приготовление.");
      }
      if (input.reason.getText().length() > 10000) {
        throw invalidField(
            input.reason, "Основание / наблюдения: сократите текст до 10000 символов.");
      }
      final var next =
          LaboratorySolutionPlanning.post(
              this.inventory.inventory(),
              input.recipe,
              actuals,
              input.source.getValue().id(),
              this.editor.document().experiment().id(),
              input.token,
              input.location.getText(),
              input.reason.getText());
      final String expected = this.inventory.fingerprint();
      this.background(
          () -> {
            final var repository = new LaboratoryInventoryRepository();
            return new LaboratoryInventoryRepository.Opened(
                next.inventory(),
                repository.write(
                    this.folder.resolve("materials.qchem"), next.inventory(), expected));
          },
          saved -> {
            this.inventory = saved;
            this.preparationInput = null;
            this.measuredPane.getChildren().clear();
            show(this.measuredPane, false);
            this.planPane.setDisable(false);
            this.retained = null;
            this.leave.setManaged(false);
            this.refreshBatches();
            this.selectStandardBatch(next.batchIds().getLast());
            this.refresh();
            this.preparedConcentrations.clear();
            this.preparedPane
                .getChildren()
                .setAll(
                    label("Приготовление записано. Расход и приход сохранены."),
                    label(
                        "Исходная партия: "
                            + input.source.getValue().title()
                            + " → остаток "
                            + this.inventory
                                .inventory()
                                .balance(
                                    this.inventory.inventory().batches().stream()
                                        .filter(b -> b.id().equals(input.source.getValue().id()))
                                        .findFirst()
                                        .orElseThrow())));
            for (final String id : next.batchIds()) {
              this.preparedPane.getChildren().add(this.preparedBatchCard(id));
            }
            this.preparedPane
                .getChildren()
                .add(
                    button(
                        "К сохранённому плану",
                        "back-plan",
                        () -> {
                          show(this.preparedPane, false);
                          show(this.planPane, true);
                        }));
            show(this.planPane, false);
            show(this.preparedPane, true);
            this.status.setText(
                "Приготовление записано. Получено партий: " + next.batchIds().size() + ".");
            this.actions();
          },
          error -> {
            input.error.setText(message(error));
            this.status.setText("Приготовление не записано. Ввод сохранён на странице.");
          });
    } catch (RuntimeException error) {
      input.error.setText(message(error));
      this.status.setText("Приготовление не записано. " + message(error));
    }
  }

  private VBox preparedBatchCard(final String id) {
    final var created =
        this.inventory.inventory().batches().stream()
            .filter(b -> b.id().equals(id))
            .findFirst()
            .orElseThrow();
    final Label concentration = label(this.concentrationText(id));
    this.preparedConcentrations.put(id, concentration);
    final Button measure =
        button("Записать концентрацию", "measure-" + id, () -> this.measurePreparedBatch(id));
    measure.getStyleClass().add("laboratory-save");
    final Button dilute =
        button(
            "Развести этот раствор",
            "dilute-" + id,
            () -> {
              this.clear();
              this.selectBatch(id);
            });
    return new VBox(
        8,
        label(created.label() + " · доступно " + this.inventory.inventory().balance(created)),
        concentration,
        new FlowPane(8, 8, measure, dilute),
        details(
            "Происхождение",
            "prepared-origin-" + id,
            new LaboratoryBatchOriginPane(this.inventory.inventory(), id),
            false));
  }

  private void measurePreparedBatch(final String id) {
    if (this.dirty || this.conflict || this.preparationInput != null) {
      this.clean();
      return;
    }
    if (this.standardPending()) {
      this.tabs.getSelectionModel().select(1);
      if (this.standardBatch.getValue() == null
          || !Objects.equals(this.standardBatch.getValue().id(), id)) {
        this.standardError.setText(
            "Сначала запишите или отмените ввод для текущей партии. Он сохранён без изменений.");
      }
      Platform.runLater(
          () ->
              (this.standardTitration ? this.standardPrimary.value : this.standardValue)
                  .requestFocus());
      return;
    }
    if (this.standardBatch.getItems().stream().noneMatch(b -> Objects.equals(b.id(), id))) {
      throw new IllegalArgumentException(
          "Полученная партия не найдена. Обновите учёт перед записью концентрации.");
    }
    this.standardMode(false);
    this.selectStandardBatch(id);
    this.tabs.getSelectionModel().select(1);
    this.standardError.setText("");
    this.status.setText("Введите концентрацию полученной партии, её происхождение и источник.");
    Platform.runLater(this.standardValue::requestFocus);
  }

  private void refreshBatches() {
    final String selected = this.batch.getValue() == null ? null : this.batch.getValue().id();
    final boolean before = this.loading;
    this.loading = true;
    try {
      this.batch.getItems().setAll(new Batch(null, "Без выбранной партии"));
      this.inventory
          .inventory()
          .batches()
          .forEach(
              b ->
                  this.batch
                      .getItems()
                      .add(
                          new Batch(
                              b.id(), b.label() + " · " + this.inventory.inventory().balance(b))));
      this.batch.setValue(
          this.batch.getItems().stream()
              .filter(b -> Objects.equals(b.id(), selected))
              .findFirst()
              .orElse(this.batch.getItems().getFirst()));
    } finally {
      this.loading = before;
    }
    this.origin();
    this.refreshStandardBatches();
  }

  private void refreshStandardBatches() {
    final String id =
        this.standardBatch.getValue() == null
            ? this.batch.getValue() == null ? null : this.batch.getValue().id()
            : this.standardBatch.getValue().id();
    final boolean before = this.standardLoading;
    this.standardLoading = true;
    try {
      this.standardBatch
          .getItems()
          .setAll(this.batch.getItems().stream().filter(b -> b.id() != null).toList());
      this.selectStandardBatch(id);
    } finally {
      this.standardLoading = before;
    }
  }

  private void selectStandardBatch(final String id) {
    if (this.standardPending() && !this.standardLoading) {
      return;
    }
    this.standardBatch.setValue(
        this.standardBatch.getItems().stream()
            .filter(b -> Objects.equals(b.id(), id))
            .findFirst()
            .orElse(null));
  }

  private String concentrationText(final String id) {
    return this.opened == null
        ? "Концентрация ещё не прочитана"
        : this.opened
            .value()
            .latest(id)
            .map(s -> "Концентрация: " + s.measured() + " · " + s.origin())
            .orElse("Для этой партии нет записи концентрации");
  }

  private void standardSummary() {
    this.standardSummary.setText(
        this.standardBatch.getValue() == null
            ? "Выберите партию"
            : this.concentrationText(this.standardBatch.getValue().id()));
  }

  private long prepared(final Recipe recipe) {
    return this.inventory == null
        ? 0
        : this.inventory.inventory().operations().stream()
            .filter(
                o ->
                    o.kind() == LaboratoryStockOperation.Kind.RECEIPT
                        && o.id().endsWith("_receipt_0")
                        && o.reason().contains(recipe.id()))
            .count();
  }

  private void recipeHistory() {
    final var text = area(20);
    text.setEditable(false);
    final ComboBox<Recipe> choice = new ComboBox<>();
    choice.getItems().setAll(this.opened.value().recipes());
    choice.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(Recipe r) {
            return r == null ? "" : r.title() + " · " + r.time();
          }

          public Recipe fromString(String text) {
            return null;
          }
        });
    choice
        .valueProperty()
        .addListener(
            (o, a, r) -> {
              if (r != null) {
                text.setText(
                    r.title()
                        + "\n"
                        + r.mode()
                        + " · "
                        + r.target()
                        + " · "
                        + r.finalVolume()
                        + "\nРастворитель: "
                        + r.solvent()
                        + "\nМолярная масса: "
                        + r.molarMass()
                        + " · чистота: "
                        + r.purityPercent()
                        + "\nИсходная концентрация: "
                        + r.stock()
                        + "\n"
                        + String.join(
                            "\n",
                            r.series().stream()
                                .map(s -> s.label() + ": " + s.target() + " до " + s.finalVolume())
                                .toList())
                        + "\n"
                        + r.note());
              }
            });
    if (!choice.getItems().isEmpty()) {
      choice.getSelectionModel().selectLast();
    }
    dialog(this, "Все версии методик", "Закрыть", List.of(choice, text), () -> {});
  }

  boolean pending() {
    return this.dirty || this.conflict || this.preparationInput != null || this.standardPending();
  }

  @Override
  boolean canClose() {
    if (!super.canClose()) {
      return false;
    }
    if (!this.pending()) {
      return true;
    }
    this.leaveMessage.setText(
        this.standardPending()
            ? "Есть незаписанный ввод концентрации. Запишите его или отмените ввод на вкладке «Концентрация партии»."
            : "Есть незаписанный план или ввод приготовления. Сохраните его либо явно отмените.");
    if (this.standardPending()) {
      this.tabs.getSelectionModel().select(1);
    }
    this.leave.setManaged(true);
    return false;
  }

  @Override
  void dispose() {
    super.dispose();
    this.worker.shutdownNow();
  }

  void draft(final Properties data, final boolean restore) {
    if (restore && this.opened == null) {
      this.deferred = new Properties();
      this.deferred.putAll(data);
      return;
    }
    if (!restore && this.retained != null) {
      this.retained.forEach(data::put);
      this.standardDraft(data, false);
      if (this.preparationInput == null) {
        return;
      }
    }
    final Map<String, Control> fields = new LinkedHashMap<>();
    fields.put("title", this.title);
    fields.put("solvent", this.solvent);
    fields.put("target", this.target);
    fields.put("stock", this.stock);
    fields.put("mass", this.molarMass);
    fields.put("purity", this.purity);
    fields.put("volume", this.volume.value);
    fields.put("volumeUnit", this.volume.unit);
    fields.put("mode", this.mode);
    fields.put("targetUnit", this.targetUnit);
    fields.put("stockUnit", this.stockUnit);
    fields.put("note", this.note);
    this.loading = true;
    try {
      LaboratoryFormDraft.fields(data, "solution", restore, fields);
      if (!restore) {
        this.standardDraft(data, false);
        data.setProperty(
            "solution.source",
            this.opened == null ? "" : Objects.toString(this.opened.fingerprint(), ""));
        data.setProperty("solution.dirty", Boolean.toString(this.dirty));
        data.setProperty("solution.conflict", Boolean.toString(this.conflict));
        data.setProperty("solution.correction", Objects.toString(this.correction, ""));
        data.setProperty(
            "solution.batch",
            this.batch.getValue() == null ? "" : Objects.toString(this.batch.getValue().id(), ""));
        data.setProperty("solution.steps", Integer.toString(this.series.getItems().size()));
        for (int i = 0; i < this.series.getItems().size(); i++) {
          final var s = this.series.getItems().get(i);
          final String p = "solution.step." + i + ".";
          data.setProperty(p + "label", s.label());
          data.setProperty(p + "target", s.target().value().toPlainString());
          data.setProperty(p + "unit", s.target().unit().name());
          data.setProperty(p + "volume", s.finalVolume().value().toPlainString());
          data.setProperty(p + "volumeUnit", s.finalVolume().unit().name());
        }
        if (this.preparationInput != null) {
          final var input = this.preparationInput;
          data.setProperty("solution.post.recipe", input.recipe.id());
          data.setProperty("solution.post.token", input.token);
          data.setProperty("solution.post.inventory", Objects.toString(input.fingerprint, ""));
          data.setProperty(
              "solution.post.source",
              input.source.getValue() == null ? "" : input.source.getValue().id());
          LaboratoryFormDraft.fields(data, "solution.post", false, input.fields());
        }
        return;
      }
      this.dirty = Boolean.parseBoolean(data.getProperty("solution.dirty"));
      this.correction =
          data.getProperty("solution.correction", "").isEmpty()
              ? null
              : data.getProperty("solution.correction");
      final String batch = data.getProperty("solution.batch", "");
      final var selected =
          this.batch.getItems().stream()
              .filter(b -> Objects.toString(b.id(), "").equals(batch))
              .findFirst();
      this.conflict =
          selected.isEmpty()
              || Boolean.parseBoolean(data.getProperty("solution.conflict"))
              || (this.dirty
                      || this.correction != null
                      || data.containsKey("solution.post.recipe")
                      || !data.containsKey("solution.standard.version"))
                  && !Objects.toString(this.opened.fingerprint(), "")
                      .equals(data.getProperty("solution.source", ""));
      this.batch.setValue(selected.orElse(this.batch.getItems().getFirst()));
      final int count = Integer.parseInt(data.getProperty("solution.steps", "0"));
      if (count < 0 || count > 100) {
        throw new IllegalArgumentException("Повреждена серия черновика.");
      }
      final List<DilutionStep> rows = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        final String p = "solution.step." + i + ".";
        rows.add(
            new DilutionStep(
                data.getProperty(p + "label"),
                new Concentration(
                    new BigDecimal(data.getProperty(p + "target")),
                    ConcentrationUnit.valueOf(data.getProperty(p + "unit"))),
                new LaboratoryQuantity(
                    new BigDecimal(data.getProperty(p + "volume")),
                    LaboratoryQuantityUnit.valueOf(data.getProperty(p + "volumeUnit")))));
      }
      this.series.getItems().setAll(rows);
      this.seriesDetails.setExpanded(!rows.isEmpty());
      this.noteDetails.setExpanded(!this.note.getText().isBlank());
      this.modeFields();
      this.refresh();
      if (data.containsKey("solution.post.recipe")) {
        final Recipe recipe =
            this.opened.value().recipes().stream()
                .filter(r -> r.id().equals(data.getProperty("solution.post.recipe")))
                .findFirst()
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "План фактического приготовления отсутствует. Черновик сохранён; запись заблокирована."));
        this.openPreparation(recipe);
        final var input = this.preparationInput;
        input.token = data.getProperty("solution.post.token", "");
        if (!input.token.startsWith("preparation_") || input.token.length() > 160) {
          throw new IllegalArgumentException("Повреждён идентификатор приготовления в черновике.");
        }
        input.fingerprint = data.getProperty("solution.post.inventory", "");
        final String source = data.getProperty("solution.post.source", "");
        input.source.setValue(
            input.source.getItems().stream()
                .filter(b -> b.id().equals(source))
                .findFirst()
                .orElse(null));
        LaboratoryFormDraft.fields(data, "solution.post", true, input.fields());
        if (!Objects.toString(this.inventory.fingerprint(), "").equals(input.fingerprint)
            || !source.isBlank() && input.source.getValue() == null) {
          this.conflict = true;
          input.error.setText(
              "Учёт материалов изменился после создания черновика. Ввод восстановлен; проведение заблокировано.");
        }
      }
      this.standardDraft(data, true);
      this.actions();
      if (this.conflict) {
        throw new IllegalArgumentException(
            "Черновик относится к другой версии книги. Ввод сохранён; запись заблокирована.");
      }
    } catch (final RuntimeException error) {
      this.conflict = true;
      this.retained = new Properties();
      data.stringPropertyNames().stream()
          .filter(k -> k.startsWith("solution."))
          .forEach(k -> this.retained.setProperty(k, data.getProperty(k)));
      this.retained.setProperty("solution.conflict", "true");
      this.status.setText(message(error));
    } finally {
      this.loading = false;
    }
  }
}

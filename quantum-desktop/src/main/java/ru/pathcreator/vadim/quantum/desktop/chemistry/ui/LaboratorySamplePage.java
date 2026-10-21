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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.BooleanSupplier;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratorySample.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryFileCodec;

/** Specimen lineage, exact remaining amounts and operations share the notebook's core and persistence. */
final class LaboratorySamplePage extends LaboratoryComputationPage {
  private enum Operation {
    REGISTER("Новый образец"),
    FRACTIONS("Серия фракций"),
    ALIQUOT("Отобрать аликвоту"),
    POOL("Объединить образцы"),
    ISOLATE("Выделить образец"),
    CONSUME("Записать расход"),
    MEASURE("Уточнить остаток"),
    DETAILS("Место и этикетка"),
    FINISH("Переработан полностью"),
    DISCARD("Утилизировать"),
    ARCHIVE("В архив"),
    RESTORE("Вернуть в работу"),
    PRODUCT("Количество → факт продукта");
    private final String label;

    Operation(final String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return this.label;
    }
  }

  private record Choice(String id, String title) {
    @Override
    public String toString() {
      return this.title;
    }
  }

  private final LaboratoryEditor editor;
  private LaboratoryDrawing expected;
  private BiConsumer<String, String> analysis;
  private BooleanSupplier save;
  private BiConsumer<String, String> evidence;
  private Consumer<String> dossier, stockTransfer, nextStage;
  private LaboratorySampleProvenancePane provenance;
  private Path sourcePath;
  private final LaboratorySampleService service = new LaboratorySampleService();
  private final TableView<LaboratorySample> table = new TableView<>();
  private final TextField filter = new TextField();
  private final CheckBox inactive = new CheckBox("Показывать архив и завершённые");
  private final Label totals = note(""), heading = note(""), summary = note("");
  private final VBox card = new VBox(10), form = new VBox(12), list = new VBox(8);
  private final ScrollPane details = scroll(this.card), formScroll = scroll(this.form);
  private final HBox split = new HBox(14);
  private final BorderPane body = new BorderPane();
  private final Map<String, TextField> fields = new LinkedHashMap<>();
  private final Map<String, Node> blocks = new LinkedHashMap<>();
  private final ComboBox<Choice> component = new ComboBox<>(), stage = new ComboBox<>();
  private final ComboBox<Kind> kind = new ComboBox<>();
  private final ComboBox<LaboratoryQuantityUnit> unit = new ComboBox<>();
  private final TextArea note = new TextArea();
  private final CheckBox processed = new CheckBox("Исходный образец полностью переработан");
  private final Button apply, preview, clear, inspect, back, saveExperience;
  private final FlowPane editActions = new FlowPane(8, 6), toolbar = new FlowPane(8, 6);
  private Operation operation;
  private List<String> selected = List.of();
  private boolean dirty, changing, inspecting, conflict, customLabel, resultRegistration;

  LaboratorySamplePage(
      final LaboratoryEditor editor,
      final String componentId,
      final BiConsumer<String, String> analysis,
      final Runnable close) {
    this(editor, null, null, null, close);
    this.analysis = analysis;
    this.component.getItems().stream()
        .filter(c -> c.id().equals(componentId))
        .findFirst()
        .ifPresent(this.component::setValue);
  }

  LaboratorySamplePage(
      final LaboratoryEditor editor,
      final String stageId,
      final String componentId,
      final BooleanSupplier save,
      final Runnable close) {
    super("Образцы и фракции", close);
    this.editor = editor;
    this.expected = editor.document();
    this.save = save;
    this.setId("chemistry-sample-page");
    this.setMinSize(0, 0);
    this.filter.setPromptText("Код, подпись, контейнер или место хранения");
    this.filter.setId("chemistry-sample-filter");
    this.filter.textProperty().addListener(o -> this.refresh());
    this.inactive.selectedProperty().addListener(o -> this.refresh());
    this.table.setId("chemistry-sample-table");
    this.table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    this.table.setMinSize(0, 120);
    this.table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    this.table
        .getColumns()
        .addAll(
            column("Образец", s -> s.code() + "\n" + s.balance().label(), 230),
            column(
                "Остаток / статус",
                s -> amount(s.balance().remaining()) + "\n" + s.balance().status(),
                170),
            column(
                "Хранение",
                s ->
                    s.balance().location()
                        + (s.balance().container().isBlank()
                            ? ""
                            : " · " + s.balance().container()),
                200));
    this.table.setPlaceholder(
        note(
            "Образцов пока нет. Зарегистрируйте сырой или выделенный продукт кнопкой «+ Образец»."));
    this.table.getSelectionModel().selectedItemProperty().addListener(o -> this.showCard());
    this.table.setRowFactory(
        t -> {
          final TableRow<LaboratorySample> row = new TableRow<>();
          row.setOnMouseClicked(
              e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                  this.inspecting = true;
                  this.arrange();
                }
              });
          return row;
        });
    this.inspect =
        button(
            "Карточка",
            "inspect",
            () -> {
              this.inspecting = true;
              this.arrange();
            });
    this.back =
        button(
            "← К списку",
            "list",
            () -> {
              this.inspecting = false;
              this.arrange();
            });
    this.list
        .getChildren()
        .setAll(this.filter, new FlowPane(8, 6, this.inactive, this.totals), this.table);
    this.list.setMinSize(0, 0);
    VBox.setVgrow(this.table, Priority.ALWAYS);
    this.card.setPadding(new Insets(14));
    this.card.setMinWidth(0);
    this.card.getStyleClass().add("laboratory-search-card");
    this.details.setFitToHeight(false);
    this.split.setMinSize(0, 0);
    this.list.setPrefWidth(620);
    this.details.setPrefWidth(460);
    HBox.setHgrow(this.list, Priority.ALWAYS);
    HBox.setHgrow(this.details, Priority.ALWAYS);
    final MenuButton operations = new MenuButton("Ещё операции");
    for (final Operation op :
        List.of(
            Operation.ISOLATE,
            Operation.CONSUME,
            Operation.MEASURE,
            Operation.DETAILS,
            Operation.PRODUCT,
            Operation.FINISH,
            Operation.ARCHIVE,
            Operation.RESTORE,
            Operation.DISCARD)) {
      final MenuItem item = new MenuItem(op.toString());
      item.setOnAction(
          e -> {
            try {
              this.begin(op);
            } catch (final RuntimeException failure) {
              this.status.setText(failure.getMessage());
            }
          });
      operations.getItems().add(item);
    }
    this.toolbar
        .getChildren()
        .setAll(
            button("+ Образец", "register", () -> this.begin(Operation.REGISTER)),
            button("Фракции", "fractions", () -> this.begin(Operation.FRACTIONS)),
            button("Аликвота", "aliquot", () -> this.begin(Operation.ALIQUOT)),
            button("Объединить", "pool", () -> this.begin(Operation.POOL)),
            operations,
            this.back,
            this.inspect);
    this.toolbar.setPadding(new Insets(0, 16, 10, 16));
    this.body.setTop(this.toolbar);
    this.body.setCenter(this.split);
    BorderPane.setMargin(this.split, new Insets(0, 16, 8, 16));
    this.setCenter(this.body);
    this.heading.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
    this.form.setPadding(new Insets(12, 18, 18, 18));
    this.form.setMinWidth(0);
    this.formScroll.setFitToHeight(false);
    this.component
        .getItems()
        .setAll(
            this.expected.experiment().components().stream()
                .map(c -> new Choice(c.id(), c.molecule().displayName()))
                .toList());
    this.component.getSelectionModel().selectFirst();
    this.component.getItems().stream()
        .filter(c -> c.id().equals(componentId))
        .findFirst()
        .ifPresent(this.component::setValue);
    this.component.setMaxWidth(Double.MAX_VALUE);
    this.component.setId("chemistry-sample-component");
    this.stage.getItems().add(new Choice("", "Без привязки к стадии"));
    this.expected
        .experiment()
        .stages()
        .forEach(s -> this.stage.getItems().add(new Choice(s.id(), s.title())));
    this.stage.getSelectionModel().selectFirst();
    this.stage.setMaxWidth(Double.MAX_VALUE);
    this.stage.setId("chemistry-sample-stage");
    this.kind.getItems().setAll(Kind.CRUDE, Kind.ISOLATED);
    this.kind.setValue(Kind.CRUDE);
    this.kind.setId("chemistry-sample-kind");
    this.unit.getItems().setAll(LaboratoryQuantityUnit.values());
    this.unit.setValue(LaboratoryQuantityUnit.MILLIGRAM);
    this.unit.setId("chemistry-sample-unit");
    this.form.getChildren().addAll(this.heading, this.summary);
    this.block("component", "Соединение", this.component);
    this.block("stage", "Стадия", this.stage);
    this.block("kind", "Вид образца", this.kind);
    for (final String name :
        List.of("code", "label", "quantity", "location", "container", "start", "count")) {
      final TextField f = new TextField();
      f.setId("chemistry-sample-" + name);
      f.textProperty().addListener(o -> this.changed());
      this.fields.put(name, f);
    }
    this.block("code", "Код / префикс фракций", this.fields.get("code"));
    this.block("label", "Подпись на этикетке", this.fields.get("label"));
    this.fields.get("quantity").setPromptText("Не измерено");
    final HBox quantity = new HBox(8, this.fields.get("quantity"), this.unit);
    HBox.setHgrow(this.fields.get("quantity"), Priority.ALWAYS);
    this.block("quantity", "Количество · для серии — в каждой фракции", quantity);
    this.block(
        "count",
        "Номер первой фракции / сколько создать",
        new HBox(8, this.fields.get("start"), this.fields.get("count")));
    this.block(
        "location", "Где хранится · шкаф / полка / коробка / ячейка", this.fields.get("location"));
    this.block("container", "Контейнер / номер виалы", this.fields.get("container"));
    this.processed.setWrapText(true);
    this.processed.setId("chemistry-sample-processed");
    this.block("processed", "Исходный материал", this.processed);
    this.note.setPrefRowCount(3);
    this.note.setWrapText(true);
    this.note.setId("chemistry-sample-note");
    this.block("note", "Примечание / основание операции", this.note);
    this.fields
        .get("label")
        .textProperty()
        .addListener(
            o -> {
              if (!this.changing && this.operation == Operation.REGISTER) this.customLabel = true;
            });
    this.component
        .valueProperty()
        .addListener(
            o -> {
              this.followCompoundLabel();
              this.changed();
            });
    this.stage
        .valueProperty()
        .addListener(
            o -> {
              if (!this.changing && this.operation == Operation.REGISTER && this.resultRegistration)
                this.registrationChoices();
              this.changed();
            });
    this.kind.valueProperty().addListener(o -> this.changed());
    this.unit.valueProperty().addListener(o -> this.changed());
    this.processed.selectedProperty().addListener(o -> this.changed());
    this.note.textProperty().addListener(o -> this.changed());
    this.apply = button("Записать операцию", "apply", this::apply);
    this.apply.getStyleClass().add("laboratory-save");
    this.preview = button("Предпросмотр", "preview", this::preview);
    this.clear =
        button(
            "Отменить ввод",
            "clear",
            () -> {
              this.dirty = false;
              this.operation = null;
              this.body.setCenter(this.split);
              this.arrange();
              this.actions();
              this.status.setText("Незавершённый ввод очищен.");
            });
    this.saveExperience = button("Сохранить опыт", "save", this::saveExperience);
    this.saveExperience.setDisable(this.save == null);
    this.saveExperience.getStyleClass().add("laboratory-save");
    if (this.save == null)
      this.saveExperience.setTooltip(new Tooltip("Сохранение доступно в основном окне опыта."));
    this.editActions.getChildren().setAll(this.preview, this.apply, this.clear);
    final FlowPane footerActions = new FlowPane(12, 6, this.editActions, this.saveExperience);
    final VBox footer = new VBox(8, this.status, footerActions);
    footer.setPadding(new Insets(8, 16, 14, 16));
    this.setBottom(footer);
    this.widthProperty().addListener((o, previous, current) -> this.arrange());
    this.addEventFilter(
        javafx.scene.input.KeyEvent.KEY_PRESSED,
        e -> {
          if (e.isShortcutDown()
              && e.getCode() == javafx.scene.input.KeyCode.ENTER
              && this.operation != null) {
            this.apply();
            e.consume();
          } else if (e.isShortcutDown()
              && e.getCode() == javafx.scene.input.KeyCode.S
              && this.save != null) {
            this.saveExperience();
            e.consume();
          }
        });
    this.refresh();
    this.actions();
    this.arrange();
    this.status.setText(
        "Остатки и история образцов хранятся в опыте. Для объединения выделите несколько строк с Ctrl или Shift.");
    if (stageId != null || componentId != null) this.creationContext(stageId, componentId);
  }

  void analysis(final BiConsumer<String, String> callback) {
    this.analysis = callback;
  }

  void saveRequested() {
    this.saveExperience();
  }

  void saving(final BooleanSupplier callback) {
    this.save = callback;
    this.saveExperience.setDisable(callback == null);
    this.saveExperience.setTooltip(
        callback == null ? new Tooltip("Сохранение доступно в основном окне опыта.") : null);
  }

  /** Opens a form, never creates a specimen or imports a component's reported amount. */
  private void creationContext(final String stageId, final String componentId) {
    this.resultRegistration = true;
    this.changing = true;
    final var experiment = this.expected.experiment();
    final var requestedStage =
        stageId == null
            ? null
            : experiment.stages().stream()
                .filter(s -> s.id().equals(stageId))
                .findFirst()
                .orElse(null);
    this.stage.setValue(
        this.stage.getItems().stream()
            .filter(s -> s.id().equals(stageId == null ? "" : stageId))
            .findFirst()
            .orElse(null));
    this.registrationChoices();
    final Choice requested =
        componentId == null
            ? null
            : this.component.getItems().stream()
                .filter(c -> c.id().equals(componentId))
                .findFirst()
                .orElse(null);
    final boolean invalid =
        stageId != null && requestedStage == null || componentId != null && requested == null;
    this.component.setValue(
        invalid
            ? null
            : requested != null
                ? requested
                : this.component.getItems().size() == 1
                    ? this.component.getItems().getFirst()
                    : null);
    this.changing = false;
    this.begin(Operation.REGISTER);
    if (invalid) {
      this.conflict = true;
      this.actions();
      this.status.setText(
          "Контекст результата устарел: стадия или продукт не найдены среди участников. Отмените ввод и откройте регистрацию из актуального результата.");
    } else if (this.component.getValue() == null)
      this.status.setText(
          "Выберите продукт стадии для нового образца. Количество вводится отдельно; образец ещё не создан.");
  }

  private void registrationChoices() {
    final String selectedId =
        this.component.getValue() == null ? null : this.component.getValue().id();
    final String stageId = this.stage.getValue() == null ? null : this.stage.getValue().id();
    final var experiment = this.expected.experiment();
    final var selectedStage =
        experiment.stages().stream().filter(s -> s.id().equals(stageId)).findFirst().orElse(null);
    final var choices =
        experiment.components().stream()
            .filter(
                c ->
                    !this.resultRegistration
                        || (selectedStage == null
                            ? c.role() == ReactionComponentRole.PRODUCT
                            : selectedStage.participants().stream()
                                .anyMatch(
                                    p ->
                                        p.componentId().equals(c.id())
                                            && p.role() == ReactionComponentRole.PRODUCT)))
            .map(c -> new Choice(c.id(), c.molecule().displayName()))
            .toList();
    final boolean loading = this.changing;
    this.changing = true;
    try {
      this.component.getItems().setAll(choices);
      this.component.setValue(
          choices.stream().filter(c -> c.id().equals(selectedId)).findFirst().orElse(null));
    } finally {
      this.changing = loading;
    }
    this.followCompoundLabel();
  }

  private void followCompoundLabel() {
    if (this.changing || this.operation != Operation.REGISTER || this.customLabel) return;
    this.changing = true;
    try {
      this.fields
          .get("label")
          .setText(this.component.getValue() == null ? "" : this.component.getValue().title());
    } finally {
      this.changing = false;
    }
  }

  private void saveExperience() {
    if (this.disposed || this.running || this.save == null) return;
    if (this.pending()) {
      this.status.setText(
          "Сначала запишите операцию или отмените ввод. Новые поля образца ещё не включены в опыт.");
      return;
    }
    try {
      if (this.save.getAsBoolean()) this.saved();
      else
        this.status.setText(
            "Сохранение текущей версии не подтверждено. Данные и ввод остались на месте.");
    } catch (final RuntimeException failure) {
      this.status.setText("Не удалось сохранить опыт: " + failure.getMessage());
    }
  }

  void saved() {
    this.status.setText(
        this.pending()
            ? "Файл опыта сохранён. Ввод операции образца ещё не записан."
            : this.editor.dirty()
                ? "Снимок записан; более поздние изменения опыта ещё не сохранены."
                : "Опыт с образцами сохранён в файл.");
  }

  private static TableColumn<LaboratorySample, String> column(
      final String name,
      final java.util.function.Function<LaboratorySample, String> value,
      final double width) {
    final var c = new TableColumn<LaboratorySample, String>(name);
    c.setCellValueFactory(v -> new ReadOnlyStringWrapper(value.apply(v.getValue())));
    c.setPrefWidth(width);
    return c;
  }

  private Button button(final String name, final String id, final Runnable action) {
    final Button b = new Button(name);
    b.setId("chemistry-sample-" + id);
    b.setOnAction(
        e -> {
          try {
            action.run();
          } catch (final RuntimeException failure) {
            this.status.setText(failure.getMessage());
          }
        });
    return b;
  }

  private void block(final String key, final String label, final Node input) {
    final VBox box = new VBox(5, note(label), input);
    box.setMinWidth(0);
    this.blocks.put(key, box);
    this.form.getChildren().add(box);
  }

  private static String amount(final LaboratoryQuantity value) {
    return value == null ? "Не измерен" : value.toString();
  }

  private void changed() {
    if (!this.changing && this.operation != null) {
      this.dirty = true;
      this.status.setText("Ввод ещё не записан. Ctrl+Enter — записать операцию.");
    }
  }

  boolean pending() {
    return this.dirty;
  }

  private void actions() {
    this.editActions.setVisible(this.operation != null);
    this.editActions.setManaged(this.operation != null);
    this.body.setTop(this.operation == null ? this.toolbar : null);
    this.apply.setDisable(this.conflict);
  }

  private void refresh() {
    final var current = this.table.getSelectionModel().getSelectedItem();
    final String query = this.filter.getText().strip().toLowerCase(Locale.ROOT);
    final var all = this.editor.document().experiment().samples();
    this.table
        .getItems()
        .setAll(
            all.stream()
                .filter(s -> this.inactive.isSelected() || s.balance().status() == Status.AVAILABLE)
                .filter(
                    s ->
                        (s.code()
                                + " "
                                + s.balance().label()
                                + " "
                                + s.balance().location()
                                + " "
                                + s.balance().container())
                            .toLowerCase(Locale.ROOT)
                            .contains(query))
                .toList());
    if (current != null) {
      this.table.getItems().stream()
          .filter(s -> s.id().equals(current.id()))
          .findFirst()
          .ifPresent(this.table.getSelectionModel()::select);
    }
    if (this.table.getSelectionModel().getSelectedItem() == null) {
      this.table.getSelectionModel().selectFirst();
    }
    this.totals.setText(this.table.getItems().size() + " из " + all.size());
    this.inspect.setDisable(this.table.getItems().isEmpty());
    this.showCard();
  }

  private void showCard() {
    if (this.provenance != null) {
      this.provenance.dispose();
      this.provenance = null;
    }
    this.card.getChildren().clear();
    final var sample = this.table.getSelectionModel().getSelectedItem();
    if (sample == null) {
      this.card
          .getChildren()
          .add(note("Выберите образец, чтобы увидеть происхождение, остаток и аналитику."));
      return;
    }
    final var b = sample.balance();
    final Label title = note(sample.code());
    title.setStyle("-fx-font-size: 23px; -fx-font-weight: bold;");
    final Label quantity = note(amount(b.remaining()));
    quantity.setStyle("-fx-font-size: 22px;");
    this.card
        .getChildren()
        .addAll(
            title,
            note(b.label() + " · " + sample.kind()),
            quantity,
            note(
                b.status()
                    + " · "
                    + (b.location().isBlank() ? "Место не задано" : b.location())
                    + (b.container().isBlank() ? "" : " · " + b.container())));
    final var experiment = this.editor.document().experiment();
    this.card
        .getChildren()
        .add(
            note(
                "Соединение: "
                    + sample.molecule().displayName()
                    + " · "
                    + sample.molecule().formula().hillNotation()
                    + (sample.stageTitle().isBlank() ? "" : "\nСтадия: " + sample.stageTitle())));
    this.card.getChildren().add(this.structure(sample));
    if (sample.origin() != null) {
      this.card.getChildren().add(this.originCard(sample));
    }
    final var component =
        experiment.components().stream()
            .filter(c -> c.id().equals(sample.componentId()))
            .findFirst();
    if (component.isEmpty()
        || !LaboratoryMaterialProperties.signature(component.get().molecule())
            .equals(LaboratoryMaterialProperties.signature(sample.molecule()))) {
      this.card
          .getChildren()
          .add(
              note(
                  "Исходное соединение изменено или удалено. Образец и анализы сохраняют прежний снимок структуры."));
    }
    final FlowPane lineage = new FlowPane(6, 6);
    for (final String id : sample.parents()) {
      final var parent = LaboratorySampleService.sample(experiment, id);
      lineage
          .getChildren()
          .add(button("← " + parent.code(), "parent-" + parent.code(), () -> this.select(id)));
    }
    for (final var child : experiment.samples()) {
      if (child.parents().contains(sample.id())) {
        lineage
            .getChildren()
            .add(
                button(
                    "→ " + child.code(), "child-" + child.code(), () -> this.select(child.id())));
      }
    }
    if (!lineage.getChildren().isEmpty()) {
      this.card.getChildren().addAll(note("Происхождение и дочерние образцы"), lineage);
    }
    this.card.getChildren().add(this.analyses(sample));
    final VBox events = new VBox(9);
    for (final var e : sample.events().reversed()) {
      events
          .getChildren()
          .add(
              note(
                  java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                          .format(e.time().atZone(java.time.ZoneId.systemDefault()))
                      + " · "
                      + e.action()
                      + (e.quantity() == null ? "" : " · " + e.quantity())
                      + (e.label().isBlank()
                          ? ""
                          : "\n"
                              + e.label()
                              + " · "
                              + e.location()
                              + (e.container().isBlank() ? "" : " · " + e.container()))
                      + (e.relatedSampleId() == null
                          ? ""
                          : " → "
                              + LaboratorySampleService.sample(experiment, e.relatedSampleId())
                                  .code())
                      + (e.note().isBlank() ? "" : "\n" + e.note())));
    }
    final TitledPane history =
        new TitledPane("История операций · " + sample.events().size(), events);
    history.setAnimated(false);
    history.setExpanded(false);
    this.card.getChildren().add(history);
  }

  /** Navigation callbacks retain the selected physical specimen; the owning workspace protects unsaved input. */
  void evidence(final BiConsumer<String, String> openEntry, final Consumer<String> openDossier) {
    this.evidence = openEntry;
    this.dossier = openDossier;
    this.showCard();
  }

  void transferToStock(final Consumer<String> callback) {
    this.stockTransfer = callback;
    this.showCard();
  }

  void sourcePath(final Path path) {
    this.sourcePath = path == null ? null : path.toAbsolutePath().normalize();
    this.showCard();
  }

  void nextStage(final Consumer<String> callback) {
    this.nextStage = callback;
    this.showCard();
  }

  private Node analyses(final LaboratorySample sample) {
    final var experiment = this.editor.document().experiment();
    final Set<String> corrected = new HashSet<>();
    experiment
        .analyses()
        .forEach(
            a -> {
              if (a.correctsId() != null) {
                corrected.add(a.correctsId());
              }
            });
    final var legacy =
        experiment.analyses().stream().filter(a -> sample.id().equals(a.sampleId())).toList();
    final var current = legacy.stream().filter(a -> !corrected.contains(a.id())).toList();
    final var advanced =
        experiment.analyticalEvidence().latest().stream()
            .filter(a -> sample.id().equals(a.sampleId()))
            .toList();
    final VBox result = new VBox(8);
    result.setId("chemistry-sample-analyses");
    result
        .getChildren()
        .add(
            note(
                "Актуальных аналитических записей: "
                    + (current.size() + advanced.size())
                    + " · обычных: "
                    + current.size()
                    + " · расширенных: "
                    + advanced.size()));
    final FlowPane actions =
        new FlowPane(
            8,
            6,
            button("+ ЯМР", "nmr", () -> this.openAnalysis(sample.id(), "NMR")),
            button("+ МС", "ms", () -> this.openAnalysis(sample.id(), "MS")),
            button("+ ТСХ", "tlc", () -> this.openAnalysis(sample.id(), "TLC")));
    if (this.evidence != null) {
      actions
          .getChildren()
          .add(
              button(
                  "Аналитика",
                  "evidence",
                  () -> {
                    if (this.canClose()) {
                      this.evidence.accept(sample.id(), null);
                    }
                  }));
    }
    if (this.dossier != null) {
      actions
          .getChildren()
          .add(
              button(
                  "Досье продукта",
                  "dossier",
                  () -> {
                    if (this.canClose()) {
                      this.dossier.accept(sample.id());
                    }
                  }));
    }
    if (this.stockTransfer != null) {
      actions
          .getChildren()
          .add(
              button(
                  "В раствор / партию",
                  "stock-transfer",
                  () -> {
                    if (this.canClose()) {
                      this.stockTransfer.accept(sample.id());
                    }
                  }));
    }
    if (this.nextStage != null) {
      actions
          .getChildren()
          .add(
              button(
                  "В следующий опыт",
                  "next-stage",
                  () -> {
                    if (this.canClose()) this.nextStage.accept(sample.id());
                  }));
    }
    result.getChildren().add(actions);
    for (final var a : current) {
      result
          .getChildren()
          .add(
              this.analysisSummary(
                  a.title(),
                  a.data().method(),
                  a.origin(),
                  a.source(),
                  a.recordedAt(),
                  a.correctsId() != null));
    }
    for (final var a : advanced) {
      final VBox entry =
          this.analysisSummary(
              a.title(),
              a.data().method(),
              a.origin(),
              a.source(),
              a.recordedAt(),
              a.correctsId() != null);
      if (this.evidence != null) {
        entry
            .getChildren()
            .add(
                button(
                    "Открыть запись",
                    "evidence-" + a.id(),
                    () -> {
                      if (this.canClose()) {
                        this.evidence.accept(sample.id(), a.id());
                      }
                    }));
      }
      result.getChildren().add(entry);
    }
    final long priorAdvanced =
        experiment.analyticalEvidence().entries().stream()
                .filter(a -> sample.id().equals(a.sampleId()))
                .count()
            - advanced.size();
    if (legacy.size() > current.size() || priorAdvanced > 0) {
      final VBox previous =
          new VBox(8, note("Исправленные записи сохраняются; в число актуальных они не включены."));
      for (final var a : legacy) {
        if (corrected.contains(a.id())) {
          previous
              .getChildren()
              .add(
                  this.analysisSummary(
                      a.title(),
                      a.data().method(),
                      a.origin(),
                      a.source(),
                      a.recordedAt(),
                      a.correctsId() != null));
        }
      }
      if (priorAdvanced > 0) {
        previous
            .getChildren()
            .add(
                note(
                    "Прежних расширенных записей: "
                        + priorAdvanced
                        + ". Полная история доступна в аналитике."));
      }
      final TitledPane history = new TitledPane("Предыдущие версии анализов", previous);
      history.setAnimated(false);
      history.setExpanded(false);
      result.getChildren().add(history);
    }
    if (current.isEmpty() && advanced.isEmpty()) {
      result
          .getChildren()
          .add(
              note(
                  "Собственных аналитических записей пока нет. Анализы другого образца показываются отдельно, с их происхождением."));
    }
    if (sample.origin() != null || !sample.parents().isEmpty()) {
      this.provenance =
          new LaboratorySampleProvenancePane(this.editor.document(), sample.id(), this.sourcePath);
      result.getChildren().add(this.provenance);
    }
    return result;
  }

  private VBox analysisSummary(
      final String title,
      final String method,
      final LaboratoryAnalysis.Origin origin,
      final String source,
      final java.time.Instant time,
      final boolean correction) {
    final String when =
        java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .format(time.atZone(java.time.ZoneId.systemDefault()));
    final Label sourceLabel = note("Источник: " + (source.isBlank() ? "не указан" : source));
    sourceLabel.setWrapText(true);
    final VBox entry =
        new VBox(
            3,
            note(title + " · " + method),
            note(origin + " · " + when + (correction ? " · исправление" : "")),
            sourceLabel);
    entry.setMinWidth(0);
    return entry;
  }

  private Node originCard(final LaboratorySample sample) {
    final var origin = sample.origin();
    final VBox details =
        new VBox(
            8,
            note(origin.sourceTitle() + " · образец «" + origin.sourceCode() + "»"),
            note(
                origin.analysisIds().isEmpty()
                    ? "Прямые анализы при передаче не привязаны. Анализы родителей — ниже."
                    : "Сохранённых ссылок на анализы источника: " + origin.analysisIds().size()),
            note(
                origin.sourcePathHint().isBlank()
                    ? "Путь к исходному опыту не указан"
                    : origin.sourcePathHint()),
            button(
                "Показать исходные анализы",
                "origin-analyses",
                () -> {
                  this.showAnalyses(sample.id());
                  if (this.provenance != null) this.provenance.setExpanded(true);
                }));
    final TextArea reference =
        new TextArea(
            "Опыт: "
                + origin.source().experimentId()
                + "\nОбразец: "
                + origin.source().sampleId()
                + "\nПередача: "
                + origin.transferId()
                + "\n"
                + origin.source().token());
    reference.setEditable(false);
    reference.setWrapText(true);
    reference.setPrefRowCount(3);
    final TitledPane identity =
        new TitledPane("Сохранённые идентификаторы происхождения", reference);
    identity.setExpanded(false);
    identity.setAnimated(false);
    details.getChildren().add(identity);
    final TitledPane result = new TitledPane("Получен из другого опыта", details);
    result.setAnimated(false);
    result.setExpanded(true);
    return result;
  }

  private Node structure(final LaboratorySample sample) {
    return LaboratorySampleStructurePreview.create(sample);
  }

  void showList() {
    if (!this.canClose()) return;
    this.operation = null;
    this.body.setCenter(this.split);
    this.inspecting = false;
    this.actions();
    this.arrange();
  }

  void showSample(final String id) {
    if (!this.canClose()) return;
    this.showList();
    this.select(id);
    this.inspecting = true;
    this.arrange();
  }

  void showAnalyses(final String id) {
    this.showSample(id);
    javafx.application.Platform.runLater(
        () -> {
          if (this.disposed) return;
          this.card.applyCss();
          this.card.layout();
          final Node section = this.card.lookup("#chemistry-sample-analyses");
          if (section != null) {
            final double range =
                this.card.getLayoutBounds().getHeight()
                    - this.details.getViewportBounds().getHeight();
            this.details.setVvalue(
                range <= 0 ? 0 : Math.min(1, section.getBoundsInParent().getMinY() / range));
          }
        });
  }

  @Override
  void dispose() {
    if (this.provenance != null) this.provenance.dispose();
    super.dispose();
  }

  private void select(final String id) {
    this.filter.clear();
    this.inactive.setSelected(true);
    this.refresh();
    this.table.getItems().stream()
        .filter(s -> s.id().equals(id))
        .findFirst()
        .ifPresent(
            s -> {
              this.table.getSelectionModel().clearSelection();
              this.table.getSelectionModel().select(s);
              this.table.scrollTo(s);
            });
  }

  private void openAnalysis(final String id, final String method) {
    if (!this.canClose()) {
      return;
    }
    if (this.analysis != null) this.analysis.accept(id, method);
    else this.status.setText("Откройте аналитику образца из основного окна опыта.");
  }

  private void arrange() {
    if (this.operation != null) {
      return;
    }
    final List<Node> nodes =
        this.inspecting
            ? List.of(this.details)
            : this.getWidth() < 1000 ? List.of(this.list) : List.of(this.list, this.details);
    if (!nodes.equals(this.split.getChildren())) {
      this.split.getChildren().setAll(nodes);
    }
    this.back.setVisible(this.inspecting);
    this.back.setManaged(this.inspecting);
    this.inspect.setVisible(!this.inspecting);
    this.inspect.setManaged(!this.inspecting);
  }

  private void begin(final Operation operation) {
    if (this.pending()) {
      this.status.setText("Сначала запишите операцию или отмените текущий ввод.");
      return;
    }
    final var selection =
        this.table.getSelectionModel().getSelectedItems().stream()
            .map(LaboratorySample::id)
            .toList();
    if (operation != Operation.REGISTER
        && (operation == Operation.POOL ? selection.size() < 2 : selection.size() != 1)) {
      throw new IllegalArgumentException(
          operation == Operation.POOL
              ? "Выделите несколько образцов с Ctrl или Shift."
              : "Выберите один исходный образец.");
    }
    this.expected = this.editor.document();
    this.selected = operation == Operation.REGISTER ? List.of() : List.copyOf(selection);
    this.operation = operation;
    this.customLabel = operation != Operation.REGISTER;
    this.conflict = false;
    this.changing = true;
    if (operation == Operation.REGISTER) this.registrationChoices();
    this.fields.values().forEach(TextField::clear);
    this.note.clear();
    this.processed.setSelected(false);
    this.fields.get("start").setText("1");
    this.fields.get("count").setText("10");
    this.unit.setValue(LaboratoryQuantityUnit.MILLIGRAM);
    final LaboratorySample source =
        this.selected.isEmpty()
            ? null
            : LaboratorySampleService.sample(this.expected.experiment(), this.selected.getFirst());
    this.fields
        .get("code")
        .setText(
            source == null
                ? this.nextCode()
                : operation == Operation.FRACTIONS
                    ? this.fractionPrefix(source.code())
                    : this.availableCode(
                        source.code()
                            + (operation == Operation.ALIQUOT
                                ? "-A"
                                : operation == Operation.ISOLATE ? "-P" : "-M")));
    this.fields
        .get("label")
        .setText(
            source == null
                ? this.component.getValue() == null ? "" : this.component.getValue().title()
                : source.balance().label());
    if (source != null) {
      this.fields.get("location").setText(source.balance().location());
      this.fields.get("container").setText(source.balance().container());
      if (source.balance().remaining() != null) {
        this.unit.setValue(source.balance().remaining().unit());
      }
    }
    if (operation == Operation.FRACTIONS) {
      this.fields.get("label").setText("Фракция");
      this.unit.setValue(LaboratoryQuantityUnit.MILLILITRE);
    }
    if (operation == Operation.MEASURE && source != null && source.balance().remaining() != null) {
      this.fields.get("quantity").setText(source.balance().remaining().value().toPlainString());
    }
    if (Set.of(Operation.REGISTER, Operation.ALIQUOT, Operation.POOL, Operation.ISOLATE)
        .contains(operation)) {
      this.fields.get("container").setText(this.fields.get("code").getText());
    }
    this.configure();
    this.changing = false;
    this.dirty = false;
    this.formScroll.setVvalue(0);
    this.body.setCenter(this.formScroll);
    this.actions();
    this.status.setText(
        "Операция запишется в историю образцов. Исправления выполняются следующей операцией; обычный Ctrl+Z историю не удаляет.");
  }

  private String nextCode() {
    final var codes =
        this.expected.experiment().samples().stream()
            .map(s -> s.code().toLowerCase(Locale.ROOT))
            .toList();
    int n = 1;
    while (codes.contains(String.format(Locale.ROOT, "s%03d", n))) {
      n++;
    }
    return String.format(Locale.ROOT, "S%03d", n);
  }

  private String availableCode(final String prefix) {
    final var codes =
        this.expected.experiment().samples().stream()
            .map(s -> s.code().toLowerCase(Locale.ROOT))
            .toList();
    int n = 1;
    while (codes.contains((prefix + n).toLowerCase(Locale.ROOT))) {
      n++;
    }
    return prefix + n;
  }

  private String fractionPrefix(final String code) {
    String prefix = code + "-F";
    int batch = 1;
    final var codes =
        this.expected.experiment().samples().stream()
            .map(s -> s.code().toLowerCase(Locale.ROOT))
            .toList();
    while (true) {
      final String candidate = prefix.toLowerCase(Locale.ROOT);
      if (codes.stream().noneMatch(c -> c.startsWith(candidate))) {
        return prefix;
      }
      prefix = code + "-F" + (++batch) + "-";
    }
  }

  private void configure() {
    this.heading.setText(this.operation.toString());
    ((Label) ((VBox) this.blocks.get("code")).getChildren().getFirst())
        .setText(
            this.operation == Operation.FRACTIONS ? "Префикс кодов фракций" : "Код нового образца");
    ((Label) ((VBox) this.blocks.get("quantity")).getChildren().getFirst())
        .setText(
            this.operation == Operation.FRACTIONS
                ? "Количество в каждой фракции"
                : this.operation == Operation.CONSUME
                    ? "Сколько израсходовано"
                    : this.operation == Operation.MEASURE
                        ? "Измеренный текущий остаток"
                        : "Количество образца");
    final boolean creating =
        Set.of(
                Operation.REGISTER,
                Operation.FRACTIONS,
                Operation.ALIQUOT,
                Operation.POOL,
                Operation.ISOLATE)
            .contains(this.operation);
    final Set<String> show = new HashSet<>(List.of("note"));
    if (creating || this.operation == Operation.DETAILS) {
      show.addAll(List.of("label", "location", "container"));
    }
    if (creating) {
      show.add("code");
    }
    if (this.operation == Operation.REGISTER) {
      show.addAll(List.of("component", "stage", "kind"));
    }
    if (Set.of(
            Operation.REGISTER,
            Operation.FRACTIONS,
            Operation.ALIQUOT,
            Operation.ISOLATE,
            Operation.CONSUME,
            Operation.MEASURE)
        .contains(this.operation)) {
      show.add("quantity");
    }
    if (this.operation == Operation.FRACTIONS) {
      show.add("count");
      show.remove("container");
    }
    if (this.operation == Operation.FRACTIONS || this.operation == Operation.ISOLATE) {
      show.add("processed");
    }
    this.blocks.forEach(
        (key, node) -> {
          node.setVisible(show.contains(key));
          node.setManaged(show.contains(key));
        });
    final String parents =
        String.join(
            ", ",
            this.selected.stream()
                .map(
                    id ->
                        this.expected.experiment().samples().stream()
                            .filter(sample -> sample.id().equals(id))
                            .map(LaboratorySample::code)
                            .findFirst()
                            .orElse("Исходный образец отсутствует"))
                .toList());
    String message =
        parents.isEmpty()
            ? "Зарегистрируйте физический образец. Пустое количество означает «не измерено»."
            : "Исходные образцы: " + parents;
    if (this.operation == Operation.FRACTIONS) {
      message +=
          "\nКоличество относится к каждой фракции. Оно не вычитается из массы сырого продукта и не становится выходом реакции.";
    }
    if (this.operation == Operation.POOL) {
      message +=
          "\nБудут перенесены все известные остатки. Нужны одна размерность и одна привязанная структура; масса и объём не суммируются.";
    }
    if (this.operation == Operation.MEASURE) {
      message +=
          "\nЗаписывается заново измеренный текущий остаток, без автоматического перевода массы в объём.";
    }
    if (this.operation == Operation.PRODUCT) {
      message +=
          "\nВ факт продукта подставляется количество при регистрации выделенного образца, до отбора аликвот. Проверьте полноту выделения и чистоту отдельно.";
    }
    this.summary.setText(message);
  }

  private int integer(final String field) {
    try {
      return Integer.parseInt(this.fields.get(field).getText().strip());
    } catch (final NumberFormatException error) {
      throw new IllegalArgumentException(
          "Номер первой фракции и число фракций должны быть целыми числами.");
    }
  }

  private LaboratoryQuantity quantity() {
    final String value = this.fields.get("quantity").getText().strip().replace(',', '.');
    if (value.isBlank()) {
      return null;
    }
    try {
      if (value.length() > 100) {
        throw new NumberFormatException();
      }
      return new LaboratoryQuantity(new BigDecimal(value), this.unit.getValue());
    } catch (final NumberFormatException e) {
      throw new IllegalArgumentException("Введите корректное количество, например 25,4.");
    }
  }

  private LaboratorySampleService.Card input() {
    return new LaboratorySampleService.Card(
        this.fields.get("code").getText(),
        this.fields.get("label").getText(),
        this.fields.get("location").getText(),
        this.fields.get("container").getText(),
        this.note.getText());
  }

  private List<LaboratorySample> prepare() {
    if (this.conflict || !this.editor.document().equals(this.expected)) {
      this.conflict = true;
      this.apply.setDisable(true);
      throw new IllegalArgumentException(
          "Исходный опыт изменился. Ввод сохранён; отмените его и откройте операцию заново.");
    }
    final var experiment = this.expected.experiment();
    final var card = this.input();
    return switch (this.operation) {
      case REGISTER ->
          this.service.register(
              experiment,
              this.component.getValue() == null ? "" : this.component.getValue().id(),
              this.stage.getValue() == null || this.stage.getValue().id().isEmpty()
                  ? null
                  : this.stage.getValue().id(),
              this.kind.getValue(),
              card,
              this.quantity());
      case FRACTIONS ->
          this.service.fractions(
              experiment,
              this.selected.getFirst(),
              card.code(),
              this.integer("start"),
              this.integer("count"),
              card,
              this.quantity(),
              this.processed.isSelected());
      case ALIQUOT ->
          this.service.aliquot(experiment, this.selected.getFirst(), card, this.quantity());
      case POOL -> this.service.pool(experiment, this.selected, card);
      case ISOLATE ->
          this.service.isolate(
              experiment,
              this.selected.getFirst(),
              card,
              this.quantity(),
              this.processed.isSelected());
      case PRODUCT -> {
        this.product();
        yield experiment.samples();
      }
      default ->
          this.service.event(
              experiment,
              this.selected.getFirst(),
              Action.valueOf(this.operation.name()),
              card,
              this.operation == Operation.CONSUME || this.operation == Operation.MEASURE
                  ? this.quantity()
                  : null);
    };
  }

  private LaboratorySample product() {
    final var sample =
        LaboratorySampleService.sample(this.expected.experiment(), this.selected.getFirst());
    final var component =
        this.expected.experiment().components().stream()
            .filter(c -> c.id().equals(sample.componentId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Исходный компонент отсутствует."));
    if (sample.kind() != Kind.ISOLATED
        || sample.events().getFirst().quantity() == null
        || sample.events().getFirst().quantity().value().signum() <= 0
        || !(sample.stageId() == null
            ? component.role() == ReactionComponentRole.PRODUCT
            : this.expected.experiment().stages().stream()
                .filter(stage -> stage.id().equals(sample.stageId()))
                .flatMap(stage -> stage.participants().stream())
                .anyMatch(
                    participant ->
                        participant.componentId().equals(component.id())
                            && participant.role() == ReactionComponentRole.PRODUCT))
        || !LaboratoryMaterialProperties.signature(component.molecule())
            .equals(LaboratoryMaterialProperties.signature(sample.molecule()))) {
      throw new IllegalArgumentException(
          "Нужен выделенный образец с положительным зарегистрированным количеством и неизменённой структурой продукта.");
    }
    return sample;
  }

  private void preview() {
    try {
      final var next = this.prepare();
      final Set<String> old = new HashSet<>();
      this.expected.experiment().samples().forEach(s -> old.add(s.id()));
      final String created =
          String.join(
              "; ",
              next.stream()
                  .filter(s -> !old.contains(s.id()))
                  .map(s -> s.code() + " · " + amount(s.balance().remaining()))
                  .toList());
      final String left =
          String.join(
              "; ",
              next.stream()
                  .filter(s -> this.selected.contains(s.id()))
                  .map(
                      s ->
                          s.code()
                              + " → "
                              + amount(s.balance().remaining())
                              + " · "
                              + s.balance().status())
                  .toList());
      this.summary.setText(
          (created.isEmpty() ? "" : "Новые: " + created + "\n")
              + left
              + (this.operation == Operation.PRODUCT ? this.productPreview() : ""));
      this.status.setText("Предпросмотр рассчитан; данные ещё не записаны.");
    } catch (final RuntimeException e) {
      this.status.setText(e.getMessage());
    }
  }

  private String productPreview() {
    final var sample = this.product();
    final var current =
        this.expected.experiment().components().stream()
            .filter(c -> c.id().equals(sample.componentId()))
            .findFirst()
            .orElseThrow();
    return "\nПрежнее количество продукта · "
        + current.evidence().quantityLabel()
        + ": "
        + amount(current.actual())
        + "\nНовое измеренное количество: "
        + amount(sample.events().getFirst().quantity())
        + "\nБерётся количество при выделении, до расхода на анализы. Прежнее значение и его происхождение будут заменены явной записью физического образца.";
  }

  private void apply() {
    if (this.operation == null || this.conflict) {
      return;
    }
    try {
      final var next = this.prepare();
      String focus = this.selected.isEmpty() ? null : this.selected.getFirst();
      if (this.operation == Operation.PRODUCT) {
        final var sample = this.product();
        final var c =
            this.expected.experiment().components().stream()
                .filter(v -> v.id().equals(sample.componentId()))
                .findFirst()
                .orElseThrow();
        this.editor.quantities(
            c.id(),
            c.planned(),
            sample.events().getFirst().quantity(),
            LaboratoryEvidence.OBSERVED);
        this.editor.record(
            LaboratoryEntry.Kind.ACTION,
            "Факт продукта заполнен зарегистрированным количеством образца «"
                + sample.code()
                + "». "
                + this.note.getText(),
            null);
      } else {
        this.editor.samples(
            this.expected.experiment().samples(),
            next,
            "Образцы: " + this.operation + ". " + this.note.getText());
        if (next.size() > this.expected.experiment().samples().size()) {
          focus = next.get(this.expected.experiment().samples().size()).id();
        }
      }
      this.expected = this.editor.document();
      this.dirty = false;
      this.operation = null;
      this.body.setCenter(this.split);
      this.refresh();
      if (focus != null) {
        this.select(focus);
      }
      this.inspecting = false;
      this.arrange();
      this.actions();
      this.status.setText("Операция записана. Сохраните опыт в QCHEM.");
    } catch (final RuntimeException e) {
      this.status.setText(e.getMessage());
    }
  }

  @Override
  boolean canClose() {
    if (this.pending()) {
      this.status.setText(
          "Есть незавершённая операция. Запишите её или нажмите «Отменить ввод»; поля сохраняются в черновик.");
      return false;
    }
    return true;
  }

  void draft(final Properties p, final boolean restore) {
    if (restore) {
      this.changing = true;
      try {
        this.operation = Operation.valueOf(p.getProperty("sampleForm.operation"));
        this.selected =
            p.getProperty("sampleForm.selected", "").isEmpty()
                ? List.of()
                : List.of(p.getProperty("sampleForm.selected").split(","));
        this.fields.forEach((k, v) -> v.setText(p.getProperty("sampleForm." + k, "")));
        this.note.setText(p.getProperty("sampleForm.note", ""));
        this.unit.setValue(LaboratoryQuantityUnit.valueOf(p.getProperty("sampleForm.unit")));
        this.kind.setValue(Kind.valueOf(p.getProperty("sampleForm.kind")));
        this.processed.setSelected(Boolean.parseBoolean(p.getProperty("sampleForm.processed")));
        this.resultRegistration =
            Boolean.parseBoolean(p.getProperty("sampleForm.resultRegistration", "false"));
        this.customLabel = Boolean.parseBoolean(p.getProperty("sampleForm.customLabel", "true"));
        this.stage.setValue(
            this.stage.getItems().stream()
                .filter(c -> c.id().equals(p.getProperty("sampleForm.stage")))
                .findFirst()
                .orElse(null));
        this.registrationChoices();
        this.component.setValue(
            this.component.getItems().stream()
                .filter(c -> c.id().equals(p.getProperty("sampleForm.component")))
                .findFirst()
                .orElse(null));
        this.conflict =
            Boolean.parseBoolean(p.getProperty("sampleForm.conflict"))
                || !this.fingerprint().equals(p.getProperty("sampleForm.fingerprint"));
        this.configure();
        this.body.setCenter(this.formScroll);
        this.dirty = true;
        this.actions();
        this.status.setText(
            this.conflict
                ? "Черновик сохранён для просмотра: исходный опыт изменился. Применение отключено."
                : "Восстановлена незавершённая операция. Проверьте поля и запишите её.");
      } finally {
        this.changing = false;
      }
      return;
    }
    if (this.operation == null) {
      return;
    }
    p.setProperty("sampleForm.resultRegistration", Boolean.toString(this.resultRegistration));
    p.setProperty("sampleForm.customLabel", Boolean.toString(this.customLabel));
    p.setProperty("sampleForm.operation", this.operation.name());
    p.setProperty("sampleForm.selected", String.join(",", this.selected));
    this.fields.forEach((k, v) -> p.setProperty("sampleForm." + k, v.getText()));
    p.setProperty("sampleForm.note", this.note.getText());
    p.setProperty("sampleForm.unit", this.unit.getValue().name());
    p.setProperty("sampleForm.kind", this.kind.getValue().name());
    p.setProperty("sampleForm.processed", Boolean.toString(this.processed.isSelected()));
    p.setProperty(
        "sampleForm.component",
        this.component.getValue() == null ? "" : this.component.getValue().id());
    p.setProperty(
        "sampleForm.stage", this.stage.getValue() == null ? "" : this.stage.getValue().id());
    p.setProperty(
        "sampleForm.conflict",
        Boolean.toString(this.conflict || !this.expected.equals(this.editor.document())));
    p.setProperty("sampleForm.fingerprint", this.fingerprint());
  }

  private String fingerprint() {
    try {
      return java.util.HexFormat.of()
          .formatHex(
              java.security.MessageDigest.getInstance("SHA-256")
                  .digest(
                      new LaboratoryFileCodec()
                          .write(this.expected)
                          .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }
}

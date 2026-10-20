/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.identity.MolecularIdentity;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionOperations.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryReactionOperationsService.AnalysisChoice;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange.CdkStereochemicalProjection;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryFileCodec;

/** One explicit manual conclusion in the existing Result section; amounts remain independent. */
final class LaboratoryStageOutcomePane extends VBox {
  private enum ProductUse {
    NONE("Не включать"),
    TARGET("Целевой"),
    BYPRODUCT("Побочный");
    private final String label;

    ProductUse(String label) {
      this.label = label;
    }

    public String toString() {
      return label;
    }
  }

  private record ProductRow(String id, String title, ComboBox<ProductUse> use) {}

  private final LaboratoryEditor editor;
  private final Supplier<String> stage;
  private final Runnable changed, beforeWrite, written;
  private final Label error = text("");
  private final VBox summary = new VBox(7),
      form = new VBox(10),
      products = new VBox(6),
      referencesView = new VBox(7);
  private final ComboBox<OutcomeStatus> status = new ComboBox<>();
  private final ComboBox<Origin> origin = new ComboBox<>();
  private final TextArea source = new TextArea(), reason = new TextArea();
  private final ComboBox<AnalysisChoice> candidate = new ComboBox<>();
  private final CheckBox previous = new CheckBox("Показать предыдущие версии анализов");
  private final List<ProductRow> rows = new ArrayList<>();
  private final List<AnalysisRef> references = new ArrayList<>();
  private final TitledPane links;
  private LaboratoryDrawing base;
  private String editingStage, correctsId;
  private boolean editing, loading;
  private Map<String, String> baseline = Map.of();

  LaboratoryStageOutcomePane(
      final LaboratoryEditor editor,
      final Supplier<String> stage,
      final Runnable changed,
      final Runnable beforeWrite,
      final Runnable written) {
    super(8);
    this.editor = editor;
    this.stage = stage;
    this.changed = changed;
    this.beforeWrite = beforeWrite;
    this.written = written;
    this.setId("desk-stage-outcome");
    this.getStyleClass().add("laboratory-result-summary");
    this.setPadding(new Insets(10));
    final Label heading = text("Ваше заключение");
    heading.setStyle("-fx-font-weight: bold;");
    this.error.setId("desk-outcome-error");
    this.error.setStyle("-fx-text-fill: -app-danger-text;");
    visible(this.error, false);
    this.status.getItems().setAll(OutcomeStatus.values());
    this.status.setPromptText("Что получилось");
    this.status.setPrefWidth(225);
    this.status.setId("desk-outcome-status");
    this.origin.getItems().setAll(Origin.values());
    this.origin.setValue(Origin.UNSPECIFIED);
    this.origin.setPrefWidth(200);
    this.origin.setId("desk-outcome-origin");
    this.origin.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(Origin value) {
            return value == null ? "" : LaboratoryReactionOperationsService.originLabel(value);
          }

          public Origin fromString(String value) {
            return null;
          }
        });
    this.source.setPromptText("На каких данных или наблюдениях основано заключение");
    this.source.setWrapText(true);
    this.source.setPrefRowCount(2);
    this.source.setId("desk-outcome-source");
    this.reason.setPromptText("Что получилось; критерий оценки или причина исправления");
    this.reason.setWrapText(true);
    this.reason.setPrefRowCount(2);
    this.reason.setId("desk-outcome-reason");
    this.candidate.setPrefWidth(620);
    this.candidate.setMaxWidth(Double.MAX_VALUE);
    this.candidate.setPromptText("Анализ участника этой стадии");
    this.candidate.setId("desk-outcome-analysis");
    this.candidate.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(AnalysisChoice v) {
            return v == null ? "" : analysisLabel(v);
          }

          public AnalysisChoice fromString(String v) {
            return null;
          }
        });
    this.candidate.setCellFactory(
        list ->
            new ListCell<>() {
              protected void updateItem(AnalysisChoice value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : analysisLabel(value));
                setWrapText(true);
                setPrefWidth(560);
                setTooltip(
                    empty || value == null
                        ? null
                        : new Tooltip(value.source() + "\n" + value.reference()));
              }
            });
    final Button add =
        button(
            "Связать",
            "analysis-add",
            () -> {
              final var selected = candidate.getValue();
              if (selected == null) {
                throw new IllegalArgumentException("Выберите анализ стадии.");
              }
              if (references.contains(selected.reference())) {
                throw new IllegalArgumentException("Этот анализ уже связан.");
              }
              if (references.size() >= 64) {
                throw new IllegalArgumentException(
                    "Поддерживается не более 64 аналитических ссылок.");
              }
              references.add(selected.reference());
              renderReferences();
              inputChanged();
            });
    this.links =
        disclosure(
            "Связанные анализы · необязательно",
            new VBox(
                8,
                new FlowPane(8, 6, this.candidate, add),
                this.previous,
                this.referencesView,
                text(
                    "Выбирается точная версия анализа. Из неё не вычисляется исход или количество.")));
    final Button apply = button("Записать заключение", "apply", this::apply);
    apply.getStyleClass().add("laboratory-save");
    this.form
        .getChildren()
        .setAll(
            new FlowPane(
                12, 8, field("Заключение", this.status), field("На чём основано", this.origin)),
            this.products,
            field("На основании чего сделан вывод *", this.source),
            field("Пояснение · обязательно при исправлении", this.reason),
            this.links,
            new FlowPane(8, 6, apply, button("Отменить ввод", "cancel", this::reset)));
    this.getChildren().setAll(heading, this.summary, this.error, this.form);
    visible(this.form, false);
    this.status.valueProperty().addListener(o -> inputChanged());
    this.origin.valueProperty().addListener(o -> inputChanged());
    this.source.textProperty().addListener(o -> inputChanged());
    this.reason.textProperty().addListener(o -> inputChanged());
    this.previous.selectedProperty().addListener(o -> refreshCandidates());
  }

  boolean pending() {
    return this.editing && !this.values().equals(this.baseline);
  }

  void focusInput() {
    if (this.editing) {
      visible(this.form, true);
      this.source.requestFocus();
    }
  }

  void refresh() {
    if (this.editing && pending()) {
      if (!this.editor.document().equals(this.base)) {
        fail(
            "Опыт изменился после начала ввода. Поля сохранены; проверьте их перед отменой и повторной оценкой.");
      }
      return;
    }
    if (this.editing
        && Objects.equals(this.editingStage, this.stage.get())
        && this.editor.document().equals(this.base)) {
      return;
    }
    this.editing = false;
    visible(this.form, false);
    this.summary.getChildren().clear();
    clearError();
    final String id = this.stage.get();
    if (id == null) {
      this.summary.getChildren().add(text("Выберите стадию для оценки исхода."));
      return;
    }
    final var outcome =
        this.editor.document().experiment().operations().outcomeFor(id).orElse(null);
    final Label state =
        text(
            outcome == null
                ? "Ещё не оценено"
                : outcome.status()
                    + " · "
                    + LaboratoryReactionOperationsService.originLabel(
                        outcome.observation().origin()));
    state.setId("desk-outcome-summary");
    state.setStyle("-fx-font-weight: bold;");
    this.summary.getChildren().add(state);
    this.summary
        .getChildren()
        .add(
            text(
                outcome == null
                    ? "Необязательно. Это ваша оценка: получен целевой продукт, смесь или цель не достигнута. Масса и процент выхода рассчитываются отдельно."
                    : brief(
                        outcome.reason().isBlank() ? outcome.source() : outcome.reason(), 180)));
    final String warning = outcome == null ? "" : currentDifference(outcome);
    if (!warning.isBlank()) {
      this.summary.getChildren().add(text(warning));
    }
    this.summary
        .getChildren()
        .add(
            button(
                outcome == null ? "Добавить заключение" : "Исправить заключение",
                "begin",
                this::begin));
    if (outcome != null) {
      this.summary.getChildren().add(history(id));
    }
  }

  private void begin() {
    this.beforeWrite.run();
    if (pending()) {
      focusInput();
      return;
    }
    final String id = this.stage.get();
    if (id == null) {
      throw new IllegalArgumentException("Выберите стадию для оценки исхода.");
    }
    this.base = this.editor.document();
    this.editingStage = id;
    this.editing = true;
    this.loading = true;
    clearError();
    final var old = this.base.experiment().operations().outcomeFor(id).orElse(null);
    this.correctsId = old == null ? null : old.id();
    this.status.setValue(old == null ? null : old.status());
    this.origin.setValue(old == null ? Origin.UNSPECIFIED : old.observation().origin());
    this.source.setText(old == null ? "" : old.source());
    this.reason.clear();
    this.references.clear();
    if (old != null) {
      this.references.addAll(old.observation().analyses());
    }
    this.previous.setSelected(false);
    buildProducts(old);
    this.links.setExpanded(false);
    refreshCandidates();
    renderReferences();
    this.loading = false;
    this.baseline = values();
    this.summary
        .getChildren()
        .setAll(
            text(
                old == null
                    ? "Новая оценка стадии · " + stageTitle()
                    : "Исправление записи от "
                        + time(old.recordedAt())
                        + ". Предыдущая версия останется в истории."));
    visible(this.form, true);
    this.changed.run();
    this.status.requestFocus();
  }

  private String stageTitle() {
    return this.base.experiment().stages().stream()
        .filter(s -> s.id().equals(this.editingStage))
        .map(LaboratoryReactionStage::title)
        .findFirst()
        .orElse("исходная стадия отсутствует");
  }

  private void buildProducts(final StageOutcome old) {
    this.rows.clear();
    this.products.getChildren().clear();
    this.products.getChildren().add(text("Какие продукты относятся к заключению"));
    final var members =
        this.base.experiment().stages().stream()
            .filter(s -> s.id().equals(this.editingStage))
            .flatMap(s -> s.participants().stream())
            .filter(p -> p.role() == ReactionComponentRole.PRODUCT)
            .toList();
    int ordinal = 0;
    for (final var member : members) {
      final var c =
          this.base.experiment().components().stream()
              .filter(v -> v.id().equals(member.componentId()))
              .findFirst()
              .orElseThrow();
      ProductUse use = ProductUse.NONE;
      if (old != null && old.targets().stream().anyMatch(p -> p.componentId().equals(c.id()))) {
        use = ProductUse.TARGET;
      } else if (old != null
          && old.byproducts().stream().anyMatch(p -> p.componentId().equals(c.id()))) {
        use = ProductUse.BYPRODUCT;
      }
      addProduct(c.id(), "№" + (++ordinal) + " · " + c.molecule().displayName(), use);
    }
    if (old != null) {
      final var all = new ArrayList<>(old.targets());
      all.addAll(old.byproducts());
      for (final var p : all) {
        if (rows.stream().noneMatch(row -> row.id().equals(p.componentId()))) {
          addProduct(
              p.componentId(),
              "Участник недоступен: " + p.snapshot().displayName(),
              old.targets().contains(p) ? ProductUse.TARGET : ProductUse.BYPRODUCT);
        }
      }
    }
    if (rows.isEmpty()) {
      this.products
          .getChildren()
          .add(
              text(
                  "В схеме стадии пока нет продуктов. Для известного продукта сначала добавьте его к схеме стадии; состав смеси можно оставить неизвестным с пояснением."));
    }
  }

  private void addProduct(final String id, final String title, final ProductUse selected) {
    final var use = new ComboBox<ProductUse>();
    use.getItems().setAll(ProductUse.values());
    use.setValue(selected);
    use.setPrefWidth(160);
    use.setMinWidth(145);
    use.setId("desk-outcome-product-" + id);
    use.valueProperty().addListener(o -> inputChanged());
    final Label name = text(title);
    name.setMinWidth(0);
    name.setMaxWidth(Double.MAX_VALUE);
    name.setTooltip(new Tooltip(title + "\n" + id));
    HBox.setHgrow(name, Priority.ALWAYS);
    final var row = new HBox(10, name, use);
    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    this.products.getChildren().add(row);
    this.rows.add(new ProductRow(id, title, use));
  }

  private void refreshCandidates() {
    if (this.base == null || this.editingStage == null) {
      return;
    }
    try {
      this.candidate
          .getItems()
          .setAll(
              LaboratoryStageOutcomeService.stageAnalysisCandidates(
                  this.base.experiment(), this.editingStage, this.previous.isSelected()));
    } catch (final RuntimeException missing) {
      this.candidate.getItems().clear();
      fail(missing.getMessage());
    }
    this.candidate.setValue(null);
  }

  private static String analysisLabel(final AnalysisChoice a) {
    return brief(a.title(), 80)
        + " · "
        + a.method()
        + " · "
        + time(a.recordedAt())
        + (a.historical() ? " · прежняя версия" : "");
  }

  private void renderReferences() {
    this.referencesView.getChildren().clear();
    for (final var ref : this.references) {
      String label;
      try {
        label =
            analysisLabel(
                LaboratoryReactionOperationsService.resolveAnalysis(this.base.experiment(), ref));
      } catch (final RuntimeException unavailable) {
        label = "Недоступный анализ · " + ref.kind() + " / " + ref.id();
      }
      final var detail = disclosure(label, new VBox());
      detail.setMaxWidth(Double.MAX_VALUE);
      HBox.setHgrow(detail, Priority.ALWAYS);
      lazyAnalysis(detail, ref, this.base);
      final Button remove =
          button(
              "×",
              "reference-remove",
              () -> {
                references.remove(ref);
                renderReferences();
                inputChanged();
              });
      remove.setTooltip(new Tooltip("Убрать только ссылку; анализ сохранится"));
      this.referencesView.getChildren().add(new HBox(8, detail, remove));
    }
    this.links.setText(
        "Связанные анализы"
            + (references.isEmpty() ? " · необязательно" : " · " + references.size()));
  }

  private void apply() {
    this.beforeWrite.run();
    if (!this.editing || this.base == null) {
      throw new IllegalArgumentException("Откройте оценку исхода.");
    }
    if (!this.editor.document().equals(this.base)) {
      throw new IllegalArgumentException(
          "Опыт изменился. Ввод исхода сохранён; сверьте его перед повторной оценкой.");
    }
    if (this.status.getValue() == null) {
      throw new IllegalArgumentException("Выберите исход стадии.");
    }
    final var value =
        LaboratoryStageOutcomeService.create(
            this.base,
            this.editingStage,
            this.status.getValue(),
            selected(ProductUse.TARGET),
            selected(ProductUse.BYPRODUCT),
            new Observation(this.origin.getValue(), this.references),
            this.source.getText(),
            this.reason.getText(),
            this.correctsId,
            Instant.now());
    this.editor.operations(
        this.base,
        LaboratoryStageOutcomeService.append(this.base.experiment().operations(), value),
        "Записан исход стадии «"
            + value.stageTitle()
            + "»: "
            + value.status()
            + " · "
            + LaboratoryReactionOperationsService.originLabel(value.observation().origin())
            + ".");
    this.editing = false;
    this.baseline = Map.of();
    visible(this.form, false);
    clearError();
    refresh();
    this.changed.run();
    this.written.run();
  }

  private List<String> selected(final ProductUse use) {
    return this.rows.stream().filter(r -> r.use().getValue() == use).map(ProductRow::id).toList();
  }

  private void inputChanged() {
    if (!this.loading) {
      clearError();
      this.changed.run();
    }
  }

  void reset() {
    this.editing = false;
    this.baseline = Map.of();
    this.references.clear();
    this.rows.clear();
    this.base = null;
    this.editingStage = null;
    this.correctsId = null;
    visible(this.form, false);
    clearError();
    refresh();
    this.changed.run();
  }

  private Map<String, String> values() {
    final var v = new LinkedHashMap<String, String>();
    v.put("status", this.status.getValue() == null ? "" : this.status.getValue().name());
    v.put("origin", this.origin.getValue().name());
    v.put("source", this.source.getText());
    v.put("reason", this.reason.getText());
    v.put("targets", String.join("\n", selected(ProductUse.TARGET)));
    v.put("byproducts", String.join("\n", selected(ProductUse.BYPRODUCT)));
    v.put("refs", Integer.toString(this.references.size()));
    for (int i = 0; i < this.references.size(); i++) {
      v.put("ref." + i + ".kind", references.get(i).kind().name());
      v.put("ref." + i + ".id", references.get(i).id());
    }
    return Map.copyOf(v);
  }

  void draft(final Properties p, final boolean restore) {
    final String key = "desk.outcome.";
    if (!restore) {
      p.setProperty(key + "editing", Boolean.toString(this.editing));
      if (!this.editing) {
        return;
      }
      p.setProperty(key + "stage", Objects.toString(this.editingStage, ""));
      p.setProperty(key + "corrects", Objects.toString(this.correctsId, ""));
      p.setProperty(key + "base", new LaboratoryFileCodec().write(this.base));
      values().forEach((k, v) -> p.setProperty(key + "raw." + k, v));
      this.baseline.forEach((k, v) -> p.setProperty(key + "baseline." + k, v));
      return;
    }
    if (!Boolean.parseBoolean(p.getProperty(key + "editing", "false"))) {
      return;
    }
    this.loading = true;
    this.editing = true;
    this.base =
        new LaboratoryFileCodec().read(p.getProperty(key + "base", p.getProperty("desk.source")));
    this.editingStage = p.getProperty(key + "stage");
    this.correctsId = p.getProperty(key + "corrects", "");
    if (this.correctsId.isBlank()) {
      this.correctsId = null;
    }
    final String status = p.getProperty(key + "raw.status", "");
    this.status.setValue(status.isBlank() ? null : OutcomeStatus.valueOf(status));
    this.origin.setValue(Origin.valueOf(p.getProperty(key + "raw.origin", "UNSPECIFIED")));
    this.source.setText(p.getProperty(key + "raw.source", ""));
    this.reason.setText(p.getProperty(key + "raw.reason", ""));
    buildProducts(null);
    for (final var role : List.of(ProductUse.TARGET, ProductUse.BYPRODUCT)) {
      final String field = role == ProductUse.TARGET ? "targets" : "byproducts";
      for (final String id : p.getProperty(key + "raw." + field, "").split("\n")) {
        if (id.isBlank()) continue;
        final var row = this.rows.stream().filter(v -> v.id().equals(id)).findFirst().orElse(null);
        if (row == null) {
          addProduct(id, "Недоступный участник · " + id, role);
        } else {
          row.use().setValue(role);
        }
      }
    }
    this.references.clear();
    final int count = Integer.parseInt(p.getProperty(key + "raw.refs", "0"));
    if (count < 0 || count > 64)
      throw new IllegalArgumentException("В черновике слишком много аналитических ссылок.");
    for (int i = 0; i < count; i++) {
      this.references.add(
          new AnalysisRef(
              AnalysisKind.valueOf(p.getProperty(key + "raw.ref." + i + ".kind")),
              p.getProperty(key + "raw.ref." + i + ".id")));
    }
    final var baseline = new LinkedHashMap<String, String>();
    p.stringPropertyNames().stream()
        .filter(k -> k.startsWith(key + "baseline."))
        .forEach(k -> baseline.put(k.substring((key + "baseline.").length()), p.getProperty(k)));
    this.baseline = Map.copyOf(baseline);
    this.previous.setSelected(false);
    refreshCandidates();
    renderReferences();
    this.loading = false;
    this.summary.getChildren().setAll(text("Восстановленный ввод исхода · " + stageTitle()));
    visible(this.form, true);
    this.changed.run();
    if (!this.editor.document().equals(this.base)) {
      fail(
          "Черновик исхода относится к другой версии опыта. Ввод сохранён; запись заблокирована до проверки.");
    }
  }

  private Node history(final String stageId) {
    final var records =
        this.editor.document().experiment().operations().stageOutcomes().reversed().stream()
            .filter(o -> o.stageId().equals(stageId))
            .toList();
    final TitledPane history = disclosure("Основание и история · " + records.size(), new VBox());
    history.setId("desk-outcome-history");
    history
        .expandedProperty()
        .addListener(
            (o, a, b) -> {
              if (b && history.getContent() instanceof VBox box && box.getChildren().isEmpty()) {
                final var select = new ComboBox<StageOutcome>();
                select.getItems().setAll(records);
                select.setMaxWidth(Double.MAX_VALUE);
                select.setConverter(
                    new javafx.util.StringConverter<>() {
                      public String toString(StageOutcome v) {
                        return v == null
                            ? ""
                            : time(v.recordedAt())
                                + " · "
                                + v.status()
                                + " · "
                                + LaboratoryReactionOperationsService.originLabel(
                                    v.observation().origin());
                      }

                      public StageOutcome fromString(String v) {
                        return null;
                      }
                    });
                final VBox record = new VBox(8);
                select
                    .valueProperty()
                    .addListener(
                        (x, before, after) -> {
                          record.getChildren().setAll(record(after));
                        });
                box.getChildren().setAll(select, record);
                select.getSelectionModel().selectFirst();
              }
            });
    return history;
  }

  private Node record(final StageOutcome outcome) {
    final VBox body =
        new VBox(
            8,
            text(
                outcome.stageTitle()
                    + " · "
                    + time(outcome.recordedAt())
                    + " · "
                    + outcome.status()),
            text(
                "Происхождение: "
                    + LaboratoryReactionOperationsService.originLabel(
                        outcome.observation().origin())),
            text("Источник: " + outcome.source()));
    if (!outcome.reason().isBlank()) body.getChildren().add(text("Пояснение: " + outcome.reason()));
    body.getChildren()
        .add(text("Сохранённые структуры этой версии; текущие количества не подставляются."));
    for (final var target : outcome.targets())
      body.getChildren().add(snapshot("Целевой · ", target));
    for (final var byproduct : outcome.byproducts())
      body.getChildren().add(snapshot("Побочный · ", byproduct));
    for (final var ref : outcome.observation().analyses()) {
      String label;
      try {
        label =
            analysisLabel(
                LaboratoryReactionOperationsService.resolveAnalysis(
                    this.editor.document().experiment(), ref));
      } catch (RuntimeException e) {
        label = "Недоступный анализ · " + ref.id();
      }
      final var pane = disclosure(label, new VBox());
      lazyAnalysis(pane, ref, this.editor.document());
      body.getChildren().add(pane);
    }
    final var identifiers =
        disclosure(
            "Идентификаторы версии",
            text(
                outcome.id()
                    + "\nСтадия: "
                    + outcome.stageId()
                    + (outcome.correctsId() == null
                        ? ""
                        : "\nИсправляет: " + outcome.correctsId())));
    body.getChildren().add(identifiers);
    return body;
  }

  private Node snapshot(final String role, final OutcomeProduct product) {
    final var pane = disclosure(role + product.snapshot().displayName(), new VBox());
    pane.setTooltip(new Tooltip("Сохранённый участник: " + product.componentId()));
    pane.expandedProperty()
        .addListener(
            (o, a, b) -> {
              if (b && pane.getContent() instanceof VBox box && box.getChildren().isEmpty()) {
                try {
                  final var depiction = CdkStereochemicalProjection.depict(product.snapshot());
                  final var drawing = depiction.drawing();
                  final double height = LaboratoryCanvas.previewHeight(drawing, 210, 300);
                  final Canvas canvas = new Canvas(480, height);
                  final StackPane frame = new StackPane(canvas);
                  frame.setMinSize(0, height);
                  frame.setPrefHeight(height);
                  frame.setMaxHeight(height);
                  canvas.widthProperty().bind(frame.widthProperty());
                  final Runnable paint =
                      () -> {
                        final var g = canvas.getGraphicsContext2D();
                        LaboratoryPreviewTheme.clear(g);
                        if (canvas.getWidth() < 20) return;
                        final var bounds = LaboratoryCanvas.previewBounds(drawing);
                        double scale =
                            Math.min(
                                2,
                                Math.min(
                                    (canvas.getWidth() - 32) / Math.max(1, bounds.getWidth()),
                                    (height - 32) / Math.max(1, bounds.getHeight())));
                        g.save();
                        g.translate(
                            (canvas.getWidth() - bounds.getWidth() * scale) / 2
                                - bounds.getMinX() * scale,
                            (height - bounds.getHeight() * scale) / 2 - bounds.getMinY() * scale);
                        g.scale(scale, scale);
                        LaboratoryPreviewTheme.paint(g, drawing);
                        g.restore();
                      };
                  LaboratoryPreviewTheme.track(canvas, paint);
                  canvas.widthProperty().addListener(v -> paint.run());
                  box.getChildren().add(frame);
                  if (!depiction.annotation().isBlank())
                    box.getChildren().add(text(depiction.annotation()));
                  paint.run();
                } catch (RuntimeException ex) {
                  box.getChildren()
                      .add(text("Снимок сохранён; стереорисунок недоступен: " + ex.getMessage()));
                }
              }
            });
    return pane;
  }

  private static void lazyAnalysis(
      final TitledPane pane, final AnalysisRef reference, final LaboratoryDrawing snapshot) {
    pane.setTooltip(new Tooltip(reference.kind() + " / " + reference.id()));
    pane.expandedProperty()
        .addListener(
            (o, a, b) -> {
              if (b && pane.getContent() instanceof VBox box && box.getChildren().isEmpty()) {
                try {
                  box.getChildren()
                      .add(
                          new OperationsObservationPane(
                                  () -> snapshot.experiment(), () -> null, false, () -> {})
                              .analysisView(reference));
                } catch (RuntimeException ex) {
                  box.getChildren().add(text("Сохранённая ссылка недоступна: " + ex.getMessage()));
                }
              }
            });
  }

  private String currentDifference(final StageOutcome outcome) {
    final var members =
        this.editor.document().experiment().stages().stream()
            .filter(s -> s.id().equals(outcome.stageId()))
            .flatMap(s -> s.participants().stream())
            .filter(p -> p.role() == ReactionComponentRole.PRODUCT)
            .map(p -> p.componentId())
            .collect(java.util.stream.Collectors.toSet());
    final var all = new ArrayList<>(outcome.targets());
    all.addAll(outcome.byproducts());
    for (final var product : all) {
      final var live =
          this.editor.document().experiment().components().stream()
              .filter(c -> c.id().equals(product.componentId()))
              .findFirst()
              .orElse(null);
      if (live == null
          || !members.contains(product.componentId())
          || !MolecularIdentity.signature(live.molecule())
              .equals(MolecularIdentity.signature(product.snapshot()))) {
        return "Текущий состав отличается от сохранённой версии. В истории показаны исходные структуры заключения.";
      }
    }
    return "";
  }

  private Button button(final String label, final String id, final Runnable action) {
    final var b = new Button(label);
    b.setId("desk-outcome-" + id);
    b.setOnAction(
        e -> {
          try {
            action.run();
          } catch (RuntimeException ex) {
            fail(Objects.requireNonNullElse(ex.getMessage(), "Проверьте ввод исхода."));
          }
        });
    return b;
  }

  private void fail(final String message) {
    this.error.setText(message);
    visible(this.error, true);
  }

  private void clearError() {
    this.error.setText("");
    visible(this.error, false);
  }

  private static TitledPane disclosure(final String title, final Node content) {
    final var pane = new TitledPane(title, content);
    pane.setExpanded(false);
    pane.setAnimated(false);
    return pane;
  }

  private static VBox field(final String label, final Node content) {
    return new VBox(4, text(label), content);
  }

  private static Label text(final String value) {
    final var label = new Label(value);
    label.setWrapText(true);
    label.setMinHeight(Region.USE_PREF_SIZE);
    return label;
  }

  private static String brief(final String value, final int count) {
    final String text = value.replaceAll("\\s+", " ").strip();
    return text.length() <= count ? text : text.substring(0, count - 1) + "…";
  }

  private static String time(final Instant value) {
    return DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(value);
  }

  private static void visible(final Node node, final boolean value) {
    node.setVisible(value);
    node.setManaged(value);
  }
}

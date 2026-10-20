/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.math.*;
import java.util.*;
import java.util.function.Consumer;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEditor;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryPlanStructureMasses;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

/** A live batch preview. Only Apply commits the complete plan, as a single undoable command. */
final class LaboratoryBatchPlanPane extends BorderPane {
  private record Choice(String id, String name) {
    public String toString() {
      return name;
    }
  }

  private record Line(
      LaboratoryComponent component,
      LaboratoryStageParticipant participant,
      LaboratoryDecimalField equivalents,
      ComboBox<LaboratoryQuantityUnit> unit,
      CheckBox manual,
      LaboratoryDecimalField fixed,
      Label result) {}

  private final LaboratoryEditor editor;
  private LaboratoryReactionStage stage;
  private final String stageId;
  private javafx.scene.Node formContent;
  private String inputSignature;
  private Properties recoveredConflict;
  private final Consumer<String> properties;
  private final Runnable changed;
  private final Runnable discarded;
  private ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing expected;
  private final ComboBox<Choice> reference = new ComboBox<>();
  private final LaboratoryDecimalField value = new LaboratoryDecimalField(5);
  private final ComboBox<LaboratoryQuantityUnit> unit = units();
  private final CheckBox solvents = new CheckBox("Масштабировать заданные объёмы растворителей");
  private final Label preview = label(""), error = label("");
  private final Label quantitySource = label("");
  private final MenuButton fromQuantity = new MenuButton("Взять количество…");
  private final VBox lines = new VBox(8), summary = new VBox(6);
  private final VBox theory = new VBox(8);
  private final Button copy = new Button("Копировать загрузку");
  private LaboratoryStoichiometry.Report previewReport;
  private final List<Line> rows = new ArrayList<>();
  private final Button apply = new Button("Применить план");
  private LaboratoryBatchPlan.Result result;
  private Map<String, String> initial = Map.of();
  private boolean loading;
  private Map<String, BigDecimal> structuralMasses = Map.of();
  private final Label massModel = label("Расчёт по структуре для чистого вещества");
  private final Button rename = new Button("Название…");
  private Consumer<String> naming;

  LaboratoryBatchPlanPane(
      final LaboratoryEditor editor,
      final LaboratoryReactionStage stage,
      final Consumer<String> properties,
      final Runnable changed) {
    this(editor, stage, properties, changed, () -> {});
  }

  LaboratoryBatchPlanPane(
      final LaboratoryEditor editor,
      final LaboratoryReactionStage stage,
      final Consumer<String> properties,
      final Runnable changed,
      final Runnable discarded) {
    this.editor = editor;
    this.stage = stage;
    this.stageId = stage.id();
    this.properties = properties;
    this.changed = changed;
    this.discarded = discarded;
    this.expected = editor.document();
    this.inputSignature = LaboratoryBatchDraft.signature(this.expected, this.stageId);
    this.setId("chemistry-batch-plan");
    this.reference.setId("chemistry-batch-reference");
    this.reference.setPrefWidth(330);
    this.reference.setMinWidth(150);
    this.reference.setMaxWidth(330);
    LaboratoryComponentNaming.choices(this.reference, Choice::name);
    this.rename.setId("chemistry-batch-rename");
    this.rename.setVisible(false);
    this.rename.setManaged(false);
    this.rename.setTooltip(
        new Tooltip(
            "Своё имя без интернета. Сначала примените или отмените текущий ввод загрузки."));
    this.rename.setOnAction(
        e -> {
          if (this.naming != null && this.referenceId() != null && this.canClose()) {
            this.naming.accept(this.referenceId());
          }
        });
    this.value.setId("chemistry-batch-value");
    this.value.setPrefColumnCount(9);
    this.value.setMaxWidth(150);
    this.value.setPromptText("Количество");
    this.unit.setId("chemistry-batch-unit");
    for (final var p : stage.participants()) {
      if (p.role() == ReactionComponentRole.REACTANT) {
        final var c = component(p.componentId());
        this.reference.getItems().add(new Choice(c.id(), c.molecule().displayName()));
      }
    }
    this.reference.getSelectionModel().selectFirst();
    this.reference.setOnAction(
        e -> {
          if (this.loading) {
            return;
          }
          final var previous = new LinkedHashMap<>(this.values());
          previous.put("batch.reference", this.initial.getOrDefault("batch.reference", ""));
          if (!this.sameValues(this.initial, previous)) {
            this.loading = true;
            this.reference.getItems().stream()
                .filter(c -> c.id().equals(this.initial.get("batch.reference")))
                .findFirst()
                .ifPresent(this.reference::setValue);
            this.loading = false;
            this.error.setText(
                "Примените или отмените текущую загрузку перед сменой опорного исходника.");
            return;
          }
          this.rebuild();
        });
    this.value
        .valueRevisionProperty()
        .addListener(
            (o, a, b) -> {
              if (!this.loading) {
                this.quantitySource.setText("");
              }
              this.preview();
            });
    this.unit
        .valueProperty()
        .addListener(
            (o, before, after) -> {
              if (this.loading || before == null || after == null || before == after) {
                return;
              }
              try {
                final var c = component(this.reference.getValue().id());
                final var original = new LaboratoryQuantity(decimal(this.value), before);
                final var converted =
                    before.dimension().equals(after.dimension())
                        ? original.in(after)
                        : LaboratoryBatchPlan.fromMoles(
                            c,
                            LaboratoryBatchPlan.moles(c, original, this.structuralMasses),
                            after,
                            this.structuralMasses);
                this.loading = true;
                this.display(this.value, converted.value());
                this.loading = false;
                this.preview();
              } catch (RuntimeException ex) {
                this.loading = false;
                this.preview();
                this.conversionNotice(ex);
              }
            });
    this.solvents.setOnAction(e -> this.preview());
    this.solvents.setId("chemistry-batch-solvents");
    final Button half = new Button("× ½"), twice = new Button("× 2");
    half.setOnAction(e -> this.multiply(new BigDecimal("0.5")));
    twice.setOnAction(e -> this.multiply(new BigDecimal("2")));
    half.setId("chemistry-batch-half");
    twice.setId("chemistry-batch-double");
    final Button edit = new Button("Свойства исходника…");
    edit.setOnAction(
        e -> {
          if (this.reference.getValue() != null && this.canClose()) {
            this.properties.accept(this.reference.getValue().id());
          }
        });
    final Label heading = label("Рассчитать загрузку");
    heading.setStyle("-fx-font-size: 21; -fx-font-weight: bold;");
    this.fromQuantity.setId("chemistry-batch-from-quantity");
    this.fromQuantity.setTooltip(
        new Tooltip(
            "Подставить внесённое количество или остаток зарегистрированного образца в план. Расход не создаётся."));
    this.fromQuantity.setOnShowing(e -> this.quantitySources());
    this.quantitySource.setId("chemistry-batch-quantity-source");
    final HBox scaleShortcuts = new HBox(6, half, twice);
    final TitledPane sourceOptions =
        new TitledPane(
            "Количество из образца и свойства исходника",
            new FlowPane(8, 8, this.fromQuantity, edit));
    sourceOptions.setId("chemistry-batch-source-options");
    sourceOptions.setExpanded(false);
    sourceOptions.setAnimated(false);
    this.quantitySource.visibleProperty().bind(this.quantitySource.textProperty().isNotEmpty());
    this.quantitySource.managedProperty().bind(this.quantitySource.visibleProperty());
    this.preview.visibleProperty().bind(this.preview.textProperty().isNotEmpty());
    this.preview.managedProperty().bind(this.preview.visibleProperty());
    final Label anchorTitle = label("Сколько исходника берём?");
    anchorTitle.getStyleClass().add("laboratory-input-heading");
    anchorTitle.setTooltip(
        new Tooltip(
            "Опорный исходник принимается за 1 эквивалент. Остальная загрузка рассчитывается относительно него."));
    final VBox anchor =
        new VBox(
            10,
            anchorTitle,
            new FlowPane(8, 8, this.reference, this.rename, this.value, this.unit, scaleShortcuts),
            this.preview,
            this.massModel,
            this.quantitySource,
            this.solvents,
            sourceOptions);
    this.massModel.setId("chemistry-batch-mass-model");
    this.massModel.setTooltip(
        new Tooltip(
            "Молярная масса рассчитана по полной структуре с явными H. Это план для чистого вещества, а не измеренная чистота образца. Ручные свойства имеют приоритет; плотность и концентрация не угадываются."));
    anchor.getStyleClass().add("laboratory-batch-anchor");
    anchor.setPadding(new javafx.geometry.Insets(16));
    final TitledPane adjustments = new TitledPane("Изменить эквиваленты и количества", this.lines);
    adjustments.setId("chemistry-batch-details");
    adjustments.setExpanded(false);
    adjustments.setAnimated(false);
    this.summary.setId("chemistry-batch-summary");
    final VBox body =
        new VBox(
            16,
            heading,
            label("Задайте количество одного исходника — загрузка пересчитается ниже."),
            anchor,
            this.summary,
            adjustments,
            this.theory);
    final boolean hasSolvents =
        stage.participants().stream().anyMatch(p -> p.role() == ReactionComponentRole.SOLVENT);
    this.solvents.setVisible(hasSolvents);
    this.solvents.setManaged(hasSolvents);
    this.theory.setId("chemistry-batch-theory");
    this.theory.getStyleClass().add("laboratory-batch-anchor");
    this.theory.setPadding(new javafx.geometry.Insets(16));
    body.setPadding(new javafx.geometry.Insets(22));
    body.setMaxWidth(1120);
    body.setMinHeight(Region.USE_PREF_SIZE);
    final StackPane sheet = new StackPane(body);
    StackPane.setAlignment(body, javafx.geometry.Pos.TOP_CENTER);
    final ScrollPane scroll = new ScrollPane(sheet);
    scroll.setFitToWidth(true);
    this.formContent = scroll;
    this.setCenter(scroll);
    this.error.setStyle("-fx-text-fill: -app-danger-text;");
    this.error.setId("chemistry-batch-error");
    this.error.visibleProperty().bind(this.error.textProperty().isNotEmpty());
    this.error.managedProperty().bind(this.error.visibleProperty());
    this.apply.setId("chemistry-batch-apply");
    this.apply.getStyleClass().add("laboratory-save");
    this.apply.setTooltip(new Tooltip("Применить загрузку · Ctrl+Enter"));
    this.addEventFilter(
        javafx.scene.input.KeyEvent.KEY_PRESSED,
        e -> {
          if (e.isShortcutDown() && e.getCode() == javafx.scene.input.KeyCode.ENTER) {
            if (!this.apply.isDisabled()) {
              this.apply.fire();
            }
            e.consume();
          }
        });
    this.apply.setOnAction(
        e -> {
          this.preview();
          if (this.result == null) {
            return;
          }
          try {
            final Set<String> locked = new HashSet<>();
            this.rows.stream()
                .filter(r -> r.manual().isSelected())
                .forEach(r -> locked.add(r.component().id()));
            this.editor.planBatch(this.editor.document(), this.result.planned());
            this.expected = this.editor.document();
            this.inputSignature = LaboratoryBatchDraft.signature(this.expected, this.stageId);
            this.rebuild();
            this.loading = true;
            this.rows.forEach(r -> r.manual().setSelected(locked.contains(r.component().id())));
            this.loading = false;
            this.initial = this.values();
            this.preview();
            this.changed.run();
            this.error.setStyle("-fx-text-fill: -app-success-text;");
            this.error.setText("План обновлён. Отмена — Ctrl+Z в редакторе.");
          } catch (RuntimeException ex) {
            this.error.setText(ex.getMessage());
          }
        });
    final Button reset = new Button("Отменить ввод");
    reset.setId("chemistry-batch-reset");
    reset.setOnAction(
        e -> {
          this.reload();
          this.discarded.run();
        });
    this.copy.setId("chemistry-batch-copy");
    this.copy.setTooltip(
        new Tooltip(
            "Скопировать текущий предпросмотр таблицей: вещество, количество, ммоль и эквиваленты. План не применяется."));
    this.copy.setOnAction(
        e -> {
          try {
            final var content = new javafx.scene.input.ClipboardContent();
            content.putString(this.loadingText());
            if (!javafx.scene.input.Clipboard.getSystemClipboard().setContent(content)) {
              throw new IllegalStateException("Буфер обмена недоступен. Повторите копирование.");
            }
            this.error.setStyle("-fx-text-fill: -app-success-text;");
            this.error.setText(
                "Таблица загрузки скопирована. Можно вставить в запись или электронную таблицу.");
          } catch (RuntimeException ex) {
            this.error.setStyle("-fx-text-fill: -app-danger-text;");
            this.error.setText(ex.getMessage());
          }
        });
    final VBox footer =
        new VBox(
            7,
            this.error,
            new FlowPane(10, 8, this.apply, this.copy, reset),
            label("Пересчёт меняет только план загрузки."));
    footer.setPadding(new javafx.geometry.Insets(12, 22, 16, 22));
    this.setBottom(footer);
    this.rebuild();
  }

  /** Embedded on the experiment sheet: one vertical scroll for the entire workflow. */
  void embedded() {
    if (this.formContent instanceof ScrollPane scroll) {
      final StackPane content = (StackPane) scroll.getContent();
      final VBox body = (VBox) content.getChildren().getFirst();
      scroll.setContent(null);
      this.formContent = content;
      this.setCenter(content);
      this.setMinHeight(Region.USE_PREF_SIZE);
      this.setMaxHeight(Region.USE_PREF_SIZE);
      final java.util.List<Node> contentNodes =
          new ArrayList<>(body.getChildren().subList(2, body.getChildren().size()));
      body.getChildren().clear();
      final VBox footer = (VBox) this.getBottom();
      footer.setPadding(new javafx.geometry.Insets(4, 0, 8, 0));
      body.setPadding(javafx.geometry.Insets.EMPTY);
      body.setSpacing(14);
      body.setMaxWidth(Double.MAX_VALUE);
      body.getChildren().setAll(contentNodes);
      this.quantitySource.visibleProperty().bind(this.quantitySource.textProperty().isNotEmpty());
      this.quantitySource.managedProperty().bind(this.quantitySource.visibleProperty());
      this.error.visibleProperty().bind(this.error.textProperty().isNotEmpty());
      this.error.managedProperty().bind(this.error.visibleProperty());
    }
  }

  String referenceId() {
    return this.reference.getValue() == null ? null : this.reference.getValue().id();
  }

  private void quantitySources() {
    this.fromQuantity.getItems().clear();
    if (this.referenceId() != null && this.recoveredConflict == null && this.inputsCurrent()) {
      final var component = this.component(this.referenceId());
      if (component.actual() != null && component.evidence().reported()) {
        final MenuItem recorded =
            new MenuItem(
                component.evidence().quantityLabel()
                    + " · "
                    + LaboratoryQuantityText.format(component.actual(), true));
        recorded.setId("chemistry-batch-from-recorded");
        recorded.setOnAction(
            e -> this.useQuantity(component.actual(), component.evidence().toString()));
        this.fromQuantity.getItems().add(recorded);
      }
      for (final var sample : this.editor.document().experiment().samples()) {
        if (!sample.componentId().equals(component.id())
            || !sample.molecule().equals(component.molecule())) {
          continue;
        }
        final var balance = sample.balance();
        if (balance.status() != LaboratorySample.Status.AVAILABLE
            || balance.remaining() == null
            || balance.remaining().value().signum() <= 0) {
          continue;
        }
        final MenuItem remaining =
            new MenuItem(
                "Образец "
                    + sample.code()
                    + " · остаток "
                    + LaboratoryQuantityText.format(balance.remaining(), true));
        remaining.setId("chemistry-batch-from-sample-" + sample.id());
        remaining.setOnAction(
            e -> {
              final var current =
                  this.editor.document().experiment().samples().stream()
                      .filter(s -> s.id().equals(sample.id()))
                      .findFirst()
                      .orElse(null);
              if (!sample.equals(current)) {
                this.error.setText("Остаток образца изменился. Откройте список количеств заново.");
                return;
              }
              this.useQuantity(balance.remaining(), "Остаток образца " + sample.code());
            });
        this.fromQuantity.getItems().add(remaining);
      }
    }
    if (this.fromQuantity.getItems().isEmpty()) {
      final MenuItem empty =
          new MenuItem("Нет внесённого результата или доступного образца этого исходника");
      empty.setDisable(true);
      this.fromQuantity.getItems().add(empty);
    }
  }

  private void useQuantity(final LaboratoryQuantity quantity, final String source) {
    if (!this.inputsCurrent() || this.recoveredConflict != null) {
      this.error.setText("Исходные данные изменились. Сначала отмените старый ввод.");
      return;
    }
    this.loading = true;
    try {
      this.unit.setValue(quantity.unit());
      this.display(this.value, quantity.value());
    } finally {
      this.loading = false;
    }
    this.quantitySource.setText("Для плана: " + source + ". Факт и остаток образца сохраняются.");
    this.preview();
  }

  void reload() {
    this.recoveredConflict = null;
    this.setCenter(this.formContent);
    this.expected = this.editor.document();
    this.inputSignature = LaboratoryBatchDraft.signature(this.expected, this.stageId);
    this.stage =
        this.expected.experiment().stages().stream()
            .filter(s -> s.id().equals(this.stageId))
            .findFirst()
            .orElse(null);
    final String selected =
        this.reference.getValue() == null ? null : this.reference.getValue().id();
    this.loading = true;
    try {
      this.reference.getItems().clear();
      if (this.stage != null) {
        for (final var participant : this.stage.participants()) {
          if (participant.role() == ReactionComponentRole.REACTANT) {
            final var c = this.component(participant.componentId());
            this.reference.getItems().add(new Choice(c.id(), c.molecule().displayName()));
          }
        }
      }
      this.reference.setValue(
          this.reference.getItems().stream()
              .filter(c -> c.id().equals(selected))
              .findFirst()
              .orElse(
                  this.reference.getItems().isEmpty()
                      ? null
                      : this.reference.getItems().getFirst()));
      final boolean hasSolvents =
          this.stage != null
              && this.stage.participants().stream()
                  .anyMatch(p -> p.role() == ReactionComponentRole.SOLVENT);
      this.solvents.setVisible(hasSolvents);
      this.solvents.setManaged(hasSolvents);
    } finally {
      this.loading = false;
    }
    this.rebuild();
  }

  void naming(final Consumer<String> action) {
    this.naming = action;
    this.rename.setVisible(action != null);
    this.rename.setManaged(action != null);
  }

  private void conversionNotice(final RuntimeException problem) {
    this.error.setStyle("-fx-text-fill: -app-warning-text;");
    this.error.setText(
        "Единица выбрана; число оставлено без пересчёта. Введите количество в выбранной единице. "
            + Objects.requireNonNullElse(problem.getMessage(), "Проверьте свойства вещества."));
  }

  boolean pending() {
    return this.recoveredConflict != null || !this.sameValues(this.initial, this.values());
  }

  boolean canClose() {
    if (!this.pending()) {
      return true;
    }
    this.error.setStyle("-fx-text-fill: -app-danger-text;");
    this.error.setText("Примените загрузку или нажмите «Отменить ввод» перед переходом.");
    return false;
  }

  private LaboratoryComponent component(String id) {
    return this.expected.experiment().components().stream()
        .filter(c -> c.id().equals(id))
        .findFirst()
        .orElseThrow();
  }

  private void rebuild() {
    this.structuralMasses = LaboratoryPlanStructureMasses.resolve(this.expected);
    this.loading = true;
    this.rows.clear();
    this.lines.getChildren().clear();
    this.quantitySource.setText("");
    final var choice = this.reference.getValue();
    if (choice == null) {
      this.display(this.value, null);
      this.loading = false;
      this.initial = this.values();
      this.preview();
      return;
    }
    final var base = component(choice.id());
    this.display(this.value, base.planned() == null ? null : base.planned().value());
    this.unit.setValue(
        base.planned() == null ? LaboratoryQuantityUnit.MILLIMOLE : base.planned().unit());
    BigDecimal baseMoles = null;
    try {
      baseMoles = LaboratoryBatchPlan.moles(base, base.planned(), this.structuralMasses);
    } catch (RuntimeException ignored) {
    }
    for (final var participant : this.stage.participants()) {
      if (participant.role() == ReactionComponentRole.PRODUCT
          || participant.componentId().equals(choice.id())) {
        continue;
      }
      final var c = component(participant.componentId());
      final boolean solvent = participant.role() == ReactionComponentRole.SOLVENT;
      final LaboratoryDecimalField eq = new LaboratoryDecimalField(4);
      eq.setPrefColumnCount(6);
      eq.setMaxWidth(105);
      eq.setId("chemistry-batch-equivalent-" + c.id());
      BigDecimal ratio = null;
      try {
        if (baseMoles != null && baseMoles.signum() > 0 && c.planned() != null) {
          ratio =
              LaboratoryBatchPlan.moles(c, c.planned(), this.structuralMasses)
                  .divide(baseMoles, MathContext.DECIMAL128);
        }
      } catch (RuntimeException ignored) {
      }
      if (ratio == null && c.planned() == null && participant.coefficient() != null) {
        final var baseParticipant =
            this.stage.participants().stream()
                .filter(p -> p.componentId().equals(choice.id()))
                .findFirst()
                .orElseThrow();
        if (baseParticipant.coefficient() != null) {
          ratio =
              BigDecimal.valueOf(participant.coefficient().value())
                  .divide(
                      BigDecimal.valueOf(baseParticipant.coefficient().value()),
                      MathContext.DECIMAL128);
        }
      }
      this.display(eq, solvent ? null : ratio);
      eq.setPromptText(
          solvent ? "Объём" : c.planned() != null && ratio == null ? "По плану" : "Экв.");
      eq.setAccessibleHelp(
          "Пустые эквиваленты при заданном плане: масштабировать исходное количество пропорционально.");
      final var u = units();
      u.setValue(
          c.planned() == null
              ? solvent ? LaboratoryQuantityUnit.MILLILITRE : LaboratoryQuantityUnit.MILLIMOLE
              : c.planned().unit());
      final CheckBox manual = new CheckBox("Вручную");
      final LaboratoryDecimalField fixed = new LaboratoryDecimalField(5);
      fixed.setPrefColumnCount(8);
      fixed.setMaxWidth(130);
      this.display(fixed, c.planned() == null ? null : c.planned().value());
      fixed.setPromptText("Количество");
      fixed.disableProperty().bind(manual.selectedProperty().not());
      eq.disableProperty()
          .bind(
              manual
                  .selectedProperty()
                  .or(new javafx.beans.property.SimpleBooleanProperty(solvent)));
      final Label result = label("");
      result.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
      result.setId("chemistry-batch-result-" + c.id());
      final Button props = new Button("Свойства…");
      props.setOnAction(
          e -> {
            if (this.canClose()) {
              this.properties.accept(c.id());
            }
          });
      final Label name = LaboratoryComponentNaming.label(c.molecule().displayName());
      name.setStyle("-fx-font-weight: bold;");
      final VBox identity =
          new VBox(4, name, label(LaboratoryInspector.roleText(participant.role())));
      identity.setMinWidth(200);
      identity.setPrefWidth(320);
      fixed.setId("chemistry-batch-fixed-" + c.id());
      manual.setId("chemistry-batch-manual-" + c.id());
      u.setId("chemistry-batch-unit-" + c.id());
      fixed.visibleProperty().bind(manual.selectedProperty());
      fixed.managedProperty().bind(fixed.visibleProperty());
      eq.visibleProperty()
          .bind(
              manual
                  .selectedProperty()
                  .not()
                  .and(new javafx.beans.property.SimpleBooleanProperty(!solvent)));
      eq.managedProperty().bind(eq.visibleProperty());
      final Label equivalentLabel = label("экв.");
      equivalentLabel.visibleProperty().bind(eq.visibleProperty());
      equivalentLabel.managedProperty().bind(eq.visibleProperty());
      final FlowPane controls = new FlowPane(8, 8, eq, equivalentLabel, manual, fixed, u, props);
      final VBox row = new VBox(8, new FlowPane(16, 8, identity, result), controls);
      row.setPadding(new javafx.geometry.Insets(13));
      row.getStyleClass().add("laboratory-batch-row");
      this.lines.getChildren().add(row);
      this.rows.add(new Line(c, participant, eq, u, manual, fixed, result));
      eq.valueRevisionProperty().addListener((o, a, b) -> this.preview());
      fixed.valueRevisionProperty().addListener((o, a, b) -> this.preview());
      u.valueProperty()
          .addListener(
              (o, before, after) -> {
                if (this.loading) {
                  return;
                }
                if (manual.isSelected()
                    && !fixed.getText().isBlank()
                    && before != null
                    && after != null
                    && before != after) {
                  try {
                    final var original = new LaboratoryQuantity(decimal(fixed), before);
                    final var converted =
                        before.dimension().equals(after.dimension())
                            ? original.in(after)
                            : LaboratoryBatchPlan.fromMoles(
                                c,
                                LaboratoryBatchPlan.moles(c, original, this.structuralMasses),
                                after,
                                this.structuralMasses);
                    this.loading = true;
                    this.display(fixed, converted.value());
                    this.loading = false;
                  } catch (RuntimeException ex) {
                    this.loading = false;
                    this.preview();
                    this.conversionNotice(ex);
                    return;
                  }
                }
                this.preview();
              });
      manual.setOnAction(
          e -> {
            if (manual.isSelected() && this.result != null) {
              final var current = this.result.planned().get(c.id());
              if (current != null) {
                this.loading = true;
                this.display(fixed, current.in(u.getValue()).value());
                this.loading = false;
              }
            }
            this.preview();
          });
    }
    this.loading = false;
    this.initial = this.values();
    this.preview();
  }

  private void multiply(BigDecimal factor) {
    try {
      this.display(this.value, decimal(this.value).multiply(factor));
    } catch (RuntimeException ex) {
      this.error.setText("Сначала введите количество исходника.");
    }
  }

  private void preview() {
    if (this.loading) {
      return;
    }
    final double summaryHeight = this.summary.getHeight(), theoryHeight = this.theory.getHeight();
    this.result = null;
    this.previewReport = null;
    this.apply.setDisable(true);
    this.copy.setDisable(true);
    this.theory.getChildren().clear();
    this.error.setText("");
    this.error.setStyle("-fx-text-fill: -app-danger-text;");
    final boolean usesStructure =
        this.stage != null
            && this.stage.participants().stream()
                .map(p -> this.component(p.componentId()))
                .anyMatch(c -> c.properties().empty() && this.structuralMasses.containsKey(c.id()));
    this.massModel.setVisible(usesStructure);
    this.massModel.setManaged(usesStructure);
    this.preview.setTooltip(null);
    this.summary.getChildren().clear();
    this.rows.forEach(
        r -> {
          r.result().setText("—");
          r.result().setTooltip(null);
        });
    try {
      if (this.recoveredConflict != null) {
        throw new IllegalArgumentException(
            "Черновик показан для просмотра. Скопируйте нужные значения, затем нажмите «Отменить ввод» и заполните загрузку по текущему опыту.");
      }
      if (this.stage == null) {
        throw new IllegalArgumentException(
            "Стадия удалена. Вернитесь к опыту и выберите актуальную стадию.");
      }
      if (!this.inputsCurrent()) {
        throw new IllegalArgumentException(
            "Исходные данные загрузки изменились. Сохраните нужные значения и нажмите «Отменить ввод» для нового расчёта.");
      }
      if (this.reference.getValue() == null) {
        throw new IllegalArgumentException("Добавьте исходники в стадию.");
      }
      final var requests = new ArrayList<LaboratoryBatchPlan.Target>();
      for (final var r : this.rows) {
        final boolean manual = r.manual().isSelected();
        final var equivalents =
            manual
                    || r.participant().role() == ReactionComponentRole.SOLVENT
                    || r.equivalents().getText().isBlank()
                ? null
                : decimal(
                    r.equivalents(),
                    "«"
                        + r.component().molecule().displayName()
                        + "»: введите число эквивалентов.");
        requests.add(
            new LaboratoryBatchPlan.Target(
                r.component().id(),
                equivalents,
                r.unit().getValue(),
                manual
                    ? new LaboratoryQuantity(
                        decimal(
                            r.fixed(),
                            "«"
                                + r.component().molecule().displayName()
                                + "»: введите ручное количество."),
                        r.unit().getValue())
                    : null));
      }
      this.result =
          LaboratoryBatchPlan.calculate(
              this.expected.experiment(),
              this.stage,
              this.reference.getValue().id(),
              new LaboratoryQuantity(
                  decimal(this.value, "Введите количество опорного исходника."),
                  this.unit.getValue()),
              requests,
              this.solvents.isSelected(),
              this.structuralMasses);
      this.preview.setText(
          LaboratoryQuantityText.format(
                  this.result.referenceMoles().movePointRight(3),
                  LaboratoryQuantityUnit.MILLIMOLE,
                  true)
              + " · 1 экв.");
      this.preview.setTooltip(
          new Tooltip(
              LaboratoryQuantityText.format(
                  this.result.referenceMoles().movePointRight(3),
                  LaboratoryQuantityUnit.MILLIMOLE,
                  false)));
      for (final var r : this.rows) {
        final var q = this.result.planned().get(r.component().id());
        r.result()
            .setText(q == null ? "Количество не задано" : LaboratoryQuantityText.format(q, true));
        r.result()
            .setTooltip(q == null ? null : new Tooltip(LaboratoryQuantityText.format(q, false)));
      }
      this.summary.setMinHeight(Region.USE_PREF_SIZE);
      this.theory.setMinHeight(Region.USE_PREF_SIZE);
      this.updateTheory();
      this.updateSummary();
      this.copy.setDisable(false);
      this.apply.setDisable(!this.pending());
    } catch (RuntimeException ex) {
      this.result = null;
      this.previewReport = null;
      this.apply.setDisable(true);
      this.copy.setDisable(true);
      this.preview.setText("Пересчёт после исправления ввода");
      this.preview.setTooltip(null);
      this.rows.forEach(
          r -> {
            r.result().setText("—");
            r.result().setTooltip(null);
          });
      this.summary.getChildren().clear();
      this.updateSummary();
      this.theory.getChildren().clear();
      final Label heading = label("Расчётное количество продукта");
      heading.setStyle("-fx-font-size: 17; -fx-font-weight: bold;");
      this.theory.getChildren().add(heading);
      this.theory
          .getChildren()
          .add(label("Исправьте отмеченное поле — здесь появится новый расчёт."));
      this.theory.setVisible(true);
      this.theory.setManaged(true);
      this.summary.setMinHeight(Math.max(0, summaryHeight));
      this.theory.setMinHeight(Math.max(0, theoryHeight));
      this.error.setText(
          ex.getMessage() == null ? "Проверьте введённые количества." : ex.getMessage());
    }
  }

  private void updateSummary() {
    final Label caption = label("Что понадобится");
    caption.getStyleClass().add("laboratory-summary-heading");
    this.summary.getChildren().add(caption);
    final GridPane table = new GridPane();
    table.setHgap(20);
    table.setVgap(0);
    table.setMaxWidth(Double.MAX_VALUE);
    final ColumnConstraints nameColumn = new ColumnConstraints();
    nameColumn.setPercentWidth(64);
    nameColumn.setHgrow(Priority.ALWAYS);
    final ColumnConstraints amountColumn = new ColumnConstraints();
    amountColumn.setPercentWidth(36);
    table.getColumnConstraints().setAll(nameColumn, amountColumn);
    int index = 0;
    for (final var participant :
        this.stage == null ? List.<LaboratoryStageParticipant>of() : this.stage.participants()) {
      if (participant.role() == ReactionComponentRole.PRODUCT) {
        continue;
      }
      final var component = this.component(participant.componentId());
      final var amount =
          this.result == null
              ? null
              : this.result.planned().getOrDefault(component.id(), component.planned());
      final Label name = LaboratoryComponentNaming.label(component.molecule().displayName());
      name.setMaxWidth(Double.MAX_VALUE);
      name.getStyleClass().add("laboratory-summary-cell");
      final boolean fixed =
          this.rows.stream()
              .anyMatch(
                  row -> row.component().id().equals(component.id()) && row.manual().isSelected());
      final Label quantity =
          label(
              (this.result == null
                      ? "—"
                      : amount == null ? "Не задано" : LaboratoryQuantityText.format(amount, true))
                  + (fixed ? " · вручную" : ""));
      quantity.setMaxWidth(Double.MAX_VALUE);
      quantity.getStyleClass().addAll("laboratory-summary-cell", "laboratory-summary-value");
      if (amount != null) {
        quantity.setTooltip(new Tooltip(LaboratoryQuantityText.format(amount, false)));
      }
      name.setTooltip(
          new Tooltip(
              component.molecule().displayName()
                  + "\n"
                  + LaboratoryInspector.roleText(participant.role())));
      table.addRow(index++, name, quantity);
    }
    this.summary.getChildren().add(table);
    this.summary.setVisible(true);
    this.summary.setManaged(true);
  }

  private void updateTheory() {
    final var experiment = this.expected.experiment();
    final var components =
        experiment.components().stream()
            .map(
                c ->
                    new LaboratoryComponent(
                        c.id(),
                        c.molecule(),
                        c.role(),
                        this.result.planned().getOrDefault(c.id(), c.planned()),
                        c.actual(),
                        c.evidence(),
                        c.properties()))
            .toList();
    final var projection =
        new LaboratoryExperiment(
            experiment.id(),
            experiment.title(),
            experiment.conditions(),
            experiment.notes(),
            components,
            experiment.journal(),
            experiment.attachments(),
            experiment.stages());
    this.previewReport =
        LaboratoryStoichiometry.preparative(
            projection,
            this.stage,
            LaboratoryStoichiometry.Basis.PLAN,
            this.reference.getValue().id(),
            this.structuralMasses);
    final Label heading = label("Расчётное количество продукта");
    heading.setStyle("-fx-font-size: 17; -fx-font-weight: bold;");
    heading.setTooltip(
        new Tooltip(
            "Относительно «"
                + this.reference.getValue().name()
                + "» и коэффициентов стадии. Расчёт для текущего ввода."));
    this.theory.getChildren().add(heading);
    final var products =
        this.previewReport.rows().stream()
            .filter(r -> r.participant().role() == ReactionComponentRole.PRODUCT)
            .toList();
    if (products.isEmpty()) {
      this.theory
          .getChildren()
          .add(label("Добавьте продукт в стадию, чтобы рассчитать его теоретическое количество."));
    }
    for (final var product : products) {
      final Label name =
          LaboratoryComponentNaming.label(product.component().molecule().displayName());
      final Label amount = label(theoretical(product, true));
      amount.setTooltip(new Tooltip(theoretical(product, false)));
      amount.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
      this.theory.getChildren().add(new VBox(4, name, amount));
    }
    for (final String diagnostic : this.previewReport.diagnostics().stream().skip(1).toList()) {
      final Label warning = label(diagnostic);
      warning.setStyle("-fx-text-fill: -app-warning-text;");
      this.theory.getChildren().add(warning);
    }
    this.theory.setVisible(true);
    this.theory.setManaged(true);
  }

  private String theoretical(final LaboratoryStoichiometry.Row row, final boolean compact) {
    if (row.theoreticalMoles() == null) {
      return "Нужны коэффициенты опорного исходника и продукта.";
    }
    final BigDecimal mass =
        LaboratoryBatchPlan.planMolarMass(row.component(), this.structuralMasses);
    return LaboratoryQuantityText.format(
            row.theoreticalMoles().movePointRight(3), LaboratoryQuantityUnit.MILLIMOLE, compact)
        + (mass != null
            ? " · "
                + LaboratoryQuantityText.format(
                    row.theoreticalMoles().multiply(mass), LaboratoryQuantityUnit.GRAM, compact)
            : " · для массы проверьте структуру или задайте молярную массу");
  }

  String loadingText() {
    if (!this.inputsCurrent() || this.recoveredConflict != null) {
      throw new IllegalStateException(
          "Исходные данные загрузки изменились. Откройте актуальный расчёт перед копированием.");
    }
    if (this.result == null || this.previewReport == null) {
      throw new IllegalStateException("Сначала исправьте загрузку.");
    }
    final StringBuilder text =
        new StringBuilder("Предпросмотр плана · ")
            .append(cell(this.stage.title()))
            .append("\nОпорный исходник: ")
            .append(cell(this.reference.getValue().name()))
            .append("\nКомпонент\tРоль\tКоличество\tЕдиница\tммоль\tЭкв.\tРежим\n");
    for (final var row : this.previewReport.rows()) {
      if (row.participant().role() == ReactionComponentRole.PRODUCT) {
        continue;
      }
      final boolean manual =
          this.rows.stream()
              .anyMatch(
                  r -> r.component().id().equals(row.component().id()) && r.manual().isSelected());
      text.append(cell(row.component().molecule().displayName()))
          .append('\t')
          .append(LaboratoryInspector.roleText(row.participant().role()))
          .append('\t')
          .append(row.input() == null ? "—" : exactNumber(row.input().value()))
          .append('\t')
          .append(row.input() == null ? "—" : row.input().unit())
          .append('\t')
          .append(
              row.amount().moles() == null
                  ? "—"
                  : exactNumber(row.amount().moles().movePointRight(3)))
          .append('\t')
          .append(row.equivalents() == null ? "—" : exactNumber(row.equivalents()))
          .append('\t')
          .append(manual ? "Вручную" : "Расчёт / план")
          .append('\n');
    }
    text.append("\nРасчётное количество продукта при полном превращении опорного исходника:\n");
    for (final var row : this.previewReport.rows()) {
      if (row.participant().role() == ReactionComponentRole.PRODUCT) {
        text.append(cell(row.component().molecule().displayName()))
            .append('\t')
            .append(theoretical(row, false))
            .append('\n');
      }
    }
    for (final String diagnostic : this.previewReport.diagnostics()) {
      text.append(cell(diagnostic)).append('\n');
    }
    if (this.massModel.isVisible()) {
      text.append(
          "Расчёт по структуре для чистого вещества для компонентов без заданных свойств. Чистота образца не измерена и не записана.\n");
    }
    return text.append("Это предпросмотр плана, не фактическая загрузка и не складской расход.")
        .toString();
  }

  private static String cell(final String text) {
    return text.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
  }

  void draft(Properties fields, boolean restore) {
    if (restore) {
      if (!this.stageId.equals(fields.getProperty("batch.stage"))
          || !this.expected.experiment().id().equals(fields.getProperty("batch.experiment"))
          || !this.inputSignature.equals(fields.getProperty("batch.inputs"))
          || Boolean.parseBoolean(fields.getProperty("batch.conflict", "false"))
          || this.reference.getItems().stream()
              .noneMatch(c -> c.id().equals(fields.getProperty("batch.reference")))) {
        this.showConflict(fields);
        return;
      }
      this.loading = true;
      final String ref = fields.getProperty("batch.reference");
      this.reference.getItems().stream()
          .filter(c -> c.id().equals(ref))
          .findFirst()
          .ifPresent(this.reference::setValue);
      this.loading = false;
      this.rebuild();
      this.loading = true;
      try {
        this.value.draft(fields, "batch.value", true);
        this.unit.setValue(
            LaboratoryQuantityUnit.valueOf(
                fields.getProperty("batch.unit", this.unit.getValue().name())));
        this.solvents.setSelected(
            Boolean.parseBoolean(fields.getProperty("batch.solvents", "false")));
        this.quantitySource.setText(fields.getProperty("batch.quantitySource", ""));
        for (final var r : this.rows) {
          String key = "batch." + r.component().id() + ".";
          r.equivalents().draft(fields, key + "eq", true);
          r.fixed().draft(fields, key + "fixed", true);
          r.manual().setSelected(Boolean.parseBoolean(fields.getProperty(key + "manual", "false")));
          r.unit()
              .setValue(
                  LaboratoryQuantityUnit.valueOf(
                      fields.getProperty(key + "unit", r.unit().getValue().name())));
        }
      } finally {
        this.loading = false;
        this.preview();
      }
    } else if (this.recoveredConflict != null) {
      this.recoveredConflict.forEach(
          (key, value) -> fields.setProperty(key.toString(), value.toString()));
    } else if (this.pending()) {
      fields.setProperty("batch.stage", this.stageId);
      fields.setProperty("batch.experiment", this.expected.experiment().id());
      fields.setProperty("batch.inputs", this.inputSignature);
      fields.setProperty("batch.conflict", Boolean.toString(!this.inputsCurrent()));
      fields.setProperty("batch.review", this.reviewText());
      fields.setProperty("batch.quantitySource", this.quantitySource.getText());
      this.values().forEach(fields::setProperty);
      this.value.draft(fields, "batch.value", false);
      for (final var r : this.rows) {
        final String key = "batch." + r.component().id() + ".";
        r.equivalents().draft(fields, key + "eq", false);
        r.fixed().draft(fields, key + "fixed", false);
      }
    }
  }

  private boolean inputsCurrent() {
    return this.expected == this.editor.document()
        || this.inputSignature.equals(
            LaboratoryBatchDraft.signature(this.editor.document(), this.stageId));
  }

  private void showConflict(final Properties fields) {
    this.recoveredConflict = LaboratoryBatchDraft.copy(fields);
    this.recoveredConflict.setProperty("batch.conflict", "true");
    final Label heading = label("Сохранённая загрузка требует проверки");
    heading.setStyle("-fx-font-size: 21; -fx-font-weight: bold;");
    final TextArea saved = new TextArea(LaboratoryBatchDraft.review(fields));
    saved.setId("chemistry-batch-conflict-values");
    saved.setEditable(false);
    saved.setWrapText(true);
    final VBox content =
        new VBox(
            12,
            heading,
            label(
                "Исходники, свойства или план отличаются от данных, на которых заполнялась форма, либо у старого черновика нет данных для проверки. Введённые значения сохранены ниже. Они не применяются к текущему опыту."),
            saved);
    content.setPadding(new javafx.geometry.Insets(22));
    VBox.setVgrow(saved, Priority.ALWAYS);
    this.setCenter(content);
    this.preview();
  }

  private String reviewText() {
    final StringBuilder text =
        new StringBuilder("Загрузка · ")
            .append(this.stage == null ? this.stageId : this.stage.title())
            .append('\n');
    final var reference = this.reference.getValue();
    text.append("Опорный исходник: ")
        .append(reference == null ? "не выбран" : reference.name())
        .append('\n')
        .append("Количество: ")
        .append(LaboratoryDecimalField.snapshot(this.value))
        .append(' ')
        .append(this.unit.getValue())
        .append("\n\n");
    for (final var row : this.rows) {
      text.append(row.component().molecule().displayName())
          .append(" · ")
          .append(LaboratoryInspector.roleText(row.participant().role()))
          .append('\n');
      text.append("Исходный план: ")
          .append(Objects.toString(row.component().planned(), "не задан"))
          .append('\n');
      if (row.manual().isSelected()) {
        text.append("Вручную: ")
            .append(LaboratoryDecimalField.snapshot(row.fixed()))
            .append(' ')
            .append(row.unit().getValue());
      } else if (row.participant().role() == ReactionComponentRole.SOLVENT) {
        text.append("Растворитель: ")
            .append(this.solvents.isSelected() ? "масштабировать" : "сохранить количество")
            .append(" · ")
            .append(row.unit().getValue());
      } else {
        text.append("Эквиваленты: ")
            .append(LaboratoryDecimalField.snapshot(row.equivalents()))
            .append(" · единица результата: ")
            .append(row.unit().getValue());
      }
      text.append("\n\n");
    }
    return text.toString();
  }

  /** Compare valid numeric inputs by value; keep raw draft strings and all other fields exact. */
  private boolean sameValues(final Map<String, String> before, final Map<String, String> after) {
    if (!before.keySet().equals(after.keySet())) {
      return false;
    }
    final Set<String> numeric = new HashSet<>();
    numeric.add("batch.value");
    for (final var row : this.rows) {
      final String key = "batch." + row.component().id() + ".";
      numeric.add(key + "eq");
      numeric.add(key + "fixed");
    }
    for (final String key : before.keySet()) {
      final String a = before.get(key), b = after.get(key);
      if (Objects.equals(a, b)) {
        continue;
      }
      if (!numeric.contains(key) || a == null || b == null) {
        return false;
      }
      try {
        if (new BigDecimal(a.strip().replace(',', '.'))
                .compareTo(new BigDecimal(b.strip().replace(',', '.')))
            != 0) {
          return false;
        }
      } catch (final NumberFormatException invalid) {
        return false;
      }
    }
    return true;
  }

  private Map<String, String> values() {
    final Map<String, String> values = new LinkedHashMap<>();
    values.put(
        "batch.reference", this.reference.getValue() == null ? "" : this.reference.getValue().id());
    values.put("batch.value", LaboratoryDecimalField.snapshot(this.value));
    values.put(
        "batch.unit", this.unit.getValue() == null ? "MILLIMOLE" : this.unit.getValue().name());
    values.put("batch.solvents", Boolean.toString(this.solvents.isSelected()));
    for (final var r : this.rows) {
      String key = "batch." + r.component().id() + ".";
      values.put(key + "eq", LaboratoryDecimalField.snapshot(r.equivalents()));
      values.put(key + "fixed", LaboratoryDecimalField.snapshot(r.fixed()));
      values.put(key + "manual", Boolean.toString(r.manual().isSelected()));
      values.put(key + "unit", r.unit().getValue().name());
    }
    return values;
  }

  private static ComboBox<LaboratoryQuantityUnit> units() {
    final var box = new ComboBox<LaboratoryQuantityUnit>();
    box.getItems().setAll(LaboratoryQuantityUnit.values());
    box.setValue(LaboratoryQuantityUnit.MILLIMOLE);
    box.setPrefWidth(88);
    return box;
  }

  private void display(LaboratoryDecimalField field, BigDecimal value) {
    field.display(value);
  }

  private BigDecimal decimal(LaboratoryDecimalField field) {
    try {
      final var value = LaboratoryDecimalField.value(field);
      if (value == null) {
        throw new IllegalArgumentException("Введите число в поле количества или эквивалентов.");
      }
      return value;
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("Введите число в поле количества или эквивалентов.");
    }
  }

  private BigDecimal decimal(final LaboratoryDecimalField field, final String message) {
    try {
      return this.decimal(field);
    } catch (final IllegalArgumentException invalid) {
      throw new IllegalArgumentException(message);
    }
  }

  private static String exactNumber(BigDecimal value) {
    return value.stripTrailingZeros().toPlainString();
  }

  private static Label label(String text) {
    final Label label = new Label(text);
    label.setWrapText(true);
    label.setMinHeight(Region.USE_PREF_SIZE);
    return label;
  }
}

/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.*;

/** Unapplied stage input belongs to the recovery draft, never to the saved experiment. */
final class LaboratoryStageForm extends BorderPane {
  private record Row(
      String id, CheckBox included, ComboBox<ReactionComponentRole> role, TextField coefficient) {}

  private final ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEditor editor;
  private final LaboratoryExperiment experiment;
  private final LaboratoryReactionStage previous;
  private final java.util.Map<String, Control> controls = new java.util.LinkedHashMap<>();
  private final List<Row> rows = new ArrayList<>();
  private final LaboratoryConditionMaterials materials;
  private final Label error = new Label();
  private final Label navigation = new Label();
  private final Button back = new Button("← Назад");
  private final FlowPane confirmation = new FlowPane(8, 8);
  private java.util.Properties initial;
  private String id;
  private boolean applied, recoveredConflict;

  LaboratoryStageForm(
      final ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEditor editor,
      final LaboratoryReactionStage previous,
      final Runnable close) {
    this.editor = editor;
    this.experiment = editor.document().experiment();
    this.previous = previous;
    this.id = previous == null ? "stage_" + UUID.randomUUID() : previous.id();
    final LaboratoryExperiment experiment = this.experiment;
    final TextField title =
        new TextField(
            previous == null ? "Стадия " + (experiment.stages().size() + 1) : previous.title());
    title.setId("chemistry-stage-title");
    final ComboBox<LaboratoryReactionStage.Arrow> arrow = new ComboBox<>();
    arrow.getItems().setAll(LaboratoryReactionStage.Arrow.values());
    arrow.setValue(previous == null ? LaboratoryReactionStage.Arrow.FORWARD : previous.arrow());
    final ReactionConditions original =
        previous == null ? experiment.conditions() : previous.conditions();
    final TextField temperature =
        new TextField(
            original.temperature() == null ? "" : Double.toString(original.temperature().value()));
    final ComboBox<TemperatureUnit> temperatureUnit = new ComboBox<>();
    temperatureUnit.getItems().setAll(TemperatureUnit.values());
    temperatureUnit.setValue(
        original.temperature() == null ? TemperatureUnit.CELSIUS : original.temperature().unit());
    temperatureUnit.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(final TemperatureUnit value) {
            return value == null ? "" : value == TemperatureUnit.CELSIUS ? "°C" : "K";
          }

          public TemperatureUnit fromString(final String text) {
            throw new UnsupportedOperationException();
          }
        });
    final TextField duration =
        new TextField(
            original.duration() == null ? "" : Double.toString(original.duration().value()));
    final ComboBox<TimeDurationUnit> durationUnit = new ComboBox<>();
    durationUnit.getItems().setAll(TimeDurationUnit.values());
    durationUnit.setValue(
        original.duration() == null ? TimeDurationUnit.HOUR : original.duration().unit());
    durationUnit.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(final TimeDurationUnit value) {
            return value == null
                ? ""
                : switch (value) {
                  case SECOND -> "секунды";
                  case MINUTE -> "минуты";
                  case HOUR -> "часы";
                  case DAY -> "сутки";
                };
          }

          public TimeDurationUnit fromString(final String text) {
            throw new UnsupportedOperationException();
          }
        });
    final TextArea note = new TextArea(original.note() == null ? "" : original.note());
    note.setPrefRowCount(3);
    note.setWrapText(true);
    temperature.setId("chemistry-stage-temperature");
    duration.setId("chemistry-stage-duration");
    note.setId("chemistry-stage-note");
    final TextField pressure =
        new TextField(
            original.pressure() == null ? "" : Double.toString(original.pressure().value()));
    pressure.setId("chemistry-stage-pressure");
    final ComboBox<PressureUnit> pressureUnit = new ComboBox<>();
    pressureUnit.getItems().setAll(PressureUnit.values());
    pressureUnit.setValue(
        original.pressure() == null ? PressureUnit.ATMOSPHERE : original.pressure().unit());
    pressureUnit.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(final PressureUnit value) {
            return value == null
                ? ""
                : switch (value) {
                  case PASCAL -> "Па";
                  case ATMOSPHERE -> "атм";
                  case BAR -> "бар";
                };
          }

          public PressureUnit fromString(final String text) {
            throw new UnsupportedOperationException();
          }
        });
    this.materials = new LaboratoryConditionMaterials(original.components());
    this.controls.putAll(
        java.util.Map.of(
            "title",
            title,
            "arrow",
            arrow,
            "temperature",
            temperature,
            "temperatureUnit",
            temperatureUnit,
            "duration",
            duration,
            "durationUnit",
            durationUnit,
            "pressure",
            pressure,
            "pressureUnit",
            pressureUnit,
            "note",
            note));
    arrow.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(final LaboratoryReactionStage.Arrow value) {
            return value == null
                ? ""
                : value == LaboratoryReactionStage.Arrow.FORWARD
                    ? "Прямая реакция →"
                    : "Равновесие ⇌";
          }

          public LaboratoryReactionStage.Arrow fromString(final String text) {
            throw new UnsupportedOperationException();
          }
        });
    final GridPane conditions = new GridPane();
    conditions.setHgap(8);
    conditions.setVgap(7);
    conditions.addRow(0, new Label("Температура"), temperature, temperatureUnit);
    conditions.addRow(1, new Label("Длительность"), duration, durationUnit);
    conditions.addRow(2, new Label("Давление"), pressure, pressureUnit);
    final VBox components = new VBox(8);
    final TextField search = new TextField();
    search.setPromptText("Найти участника по названию или формуле");
    search.setId("chemistry-stage-search");
    final CheckBox selectedOnly = new CheckBox("Только включённые");
    selectedOnly.setId("chemistry-stage-selected-only");
    final Label selectionCount = new Label();
    selectionCount.setId("chemistry-stage-selection-count");
    final Runnable countSelection =
        () ->
            selectionCount.setText(
                "Включено: "
                    + this.rows.stream().filter(row -> row.included().isSelected()).count()
                    + " из "
                    + experiment.components().size());
    final Button includeSides = new Button("Включить исходники и продукты");
    includeSides.setId("chemistry-stage-include-sides");
    includeSides.setTooltip(
        new Tooltip(
            "Включить компоненты с ролью исходника или продукта в опыте. Остальной выбор и роли в стадии сохранятся."));
    includeSides.setOnAction(
        event -> {
          for (final Row row : this.rows) {
            final var component =
                experiment.components().stream()
                    .filter(item -> item.id().equals(row.id()))
                    .findFirst()
                    .orElseThrow();
            if (component.role() == ReactionComponentRole.REACTANT
                || component.role() == ReactionComponentRole.PRODUCT) {
              row.included().setSelected(true);
            }
          }
        });
    final Button unitCoefficients = new Button("Пустые коэффициенты → 1");
    unitCoefficients.setId("chemistry-stage-unit-coefficients");
    unitCoefficients.setTooltip(
        new Tooltip(
            "Явно задать 1 только включённым исходникам и продуктам без коэффициента. Заполненные значения не меняются."));
    unitCoefficients.setOnAction(
        event -> {
          for (final Row row : this.rows) {
            if (row.included().isSelected()
                && (row.role().getValue() == ReactionComponentRole.REACTANT
                    || row.role().getValue() == ReactionComponentRole.PRODUCT)
                && row.coefficient().getText().isBlank()) {
              row.coefficient().setText("1");
            }
          }
        });
    for (final LaboratoryComponent component : experiment.components()) {
      final var participant =
          previous == null
              ? null
              : previous.participants().stream()
                  .filter(item -> item.componentId().equals(component.id()))
                  .findFirst()
                  .orElse(null);
      final CheckBox included = new CheckBox(component.molecule().displayName());
      included.setTooltip(new Tooltip(component.molecule().displayName()));
      included.setSelected(participant != null);
      included.setMinWidth(0);
      included.setMaxWidth(Double.MAX_VALUE);
      included.setMinHeight(Region.USE_PREF_SIZE);
      included.setWrapText(false);
      included.setTextOverrun(OverrunStyle.ELLIPSIS);
      included.getStyleClass().add("laboratory-compound-name");
      final ComboBox<ReactionComponentRole> role = new ComboBox<>();
      role.getItems().setAll(ReactionComponentRole.values());
      role.setValue(participant == null ? component.role() : participant.role());
      role.setConverter(
          new javafx.util.StringConverter<>() {
            public String toString(final ReactionComponentRole value) {
              return LaboratoryInspector.roleText(value);
            }

            public ReactionComponentRole fromString(final String text) {
              throw new UnsupportedOperationException();
            }
          });
      role.disableProperty().bind(included.selectedProperty().not());
      final TextField coefficient =
          new TextField(
              participant == null || participant.coefficient() == null
                  ? ""
                  : Integer.toString(participant.coefficient().value()));
      coefficient.setPromptText("Не указан");
      coefficient.setPrefWidth(92);
      coefficient.setMaxWidth(92);
      coefficient
          .disableProperty()
          .bind(
              included
                  .selectedProperty()
                  .not()
                  .or(
                      role.valueProperty()
                          .isNotEqualTo(ReactionComponentRole.REACTANT)
                          .and(role.valueProperty().isNotEqualTo(ReactionComponentRole.PRODUCT))));
      rows.add(new Row(component.id(), included, role, coefficient));
      final String prefix = "component." + component.id();
      this.controls.put(prefix + ".included", included);
      this.controls.put(prefix + ".role", role);
      this.controls.put(prefix + ".coefficient", coefficient);
      included.setId("chemistry-stage-include-" + component.id());
      role.setId("chemistry-stage-role-" + component.id());
      coefficient.setId("chemistry-stage-coefficient-" + component.id());
      final Label formula =
          new Label(
              component.molecule().formula().hillNotation()
                  + " · в опыте: "
                  + LaboratoryInspector.roleText(component.role()));
      formula.setWrapText(true);
      formula.setMinHeight(Region.USE_PREF_SIZE);
      final FlowPane values =
          new FlowPane(8, 6, new Label("Роль"), role, new Label("Коэф."), coefficient);
      final VBox details = new VBox(7, included, formula, values);
      details.setMinWidth(0);
      HBox.setHgrow(details, Priority.ALWAYS);
      final HBox rowView =
          new HBox(12, thumbnail(editor.document().layouts().get(component.id())), details);
      rowView.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
      rowView.setPadding(new Insets(10));
      rowView.getStyleClass().add("laboratory-stage-participant");
      rowView.setId("chemistry-stage-row-" + component.id());
      rowView
          .visibleProperty()
          .bind(
              javafx.beans.binding.Bindings.createBooleanBinding(
                  () ->
                      (!selectedOnly.isSelected() || included.isSelected())
                          && LaboratoryTextSearch.matches(
                              component.molecule().displayName()
                                  + " "
                                  + component.molecule().formula().hillNotation(),
                              search.getText()),
                  search.textProperty(),
                  selectedOnly.selectedProperty(),
                  included.selectedProperty()));
      rowView.managedProperty().bind(rowView.visibleProperty());
      included.selectedProperty().addListener((o, a, b) -> countSelection.run());
      components.getChildren().add(rowView);
    }
    final Label hint =
        new Label(
            "Выберите вещества и их роли. Коэффициенты задают стехиометрию реакции; навески и избыток реагентов указываются в загрузке.");
    hint.setWrapText(true);
    hint.setMaxWidth(690);
    this.error.setWrapText(true);
    this.error.setStyle("-fx-text-fill: -app-danger-text;");
    this.error.setId("chemistry-stage-error");
    this.error.managedProperty().bind(this.error.textProperty().isNotEmpty());
    this.error.visibleProperty().bind(this.error.managedProperty());
    this.navigation.setId("chemistry-stage-navigation");
    this.navigation.setWrapText(true);
    this.navigation.setMinWidth(0);
    this.navigation.setMaxWidth(Double.MAX_VALUE);
    this.back.setId("chemistry-stage-back");
    this.back.setMinWidth(Region.USE_PREF_SIZE);
    this.back.getStyleClass().add("laboratory-navigation-back");
    this.back.setOnAction(
        event -> {
          if (this.canClose()) {
            close.run();
          }
        });
    this.navigationContext(experiment.title(), "← Назад");
    final HBox navigationHeader = new HBox(12, this.back, this.navigation);
    navigationHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    navigationHeader.getStyleClass().add("laboratory-navigation-header");
    HBox.setHgrow(this.navigation, Priority.ALWAYS);
    countSelection.run();
    final FlowPane tools =
        new FlowPane(8, 6, includeSides, unitCoefficients, selectedOnly, selectionCount);
    final VBox participantContent = new VBox(10, search, tools, hint, components);
    participantContent.setPadding(new Insets(12, 18, 18, 18));
    temperature.setPromptText("Не указана");
    duration.setPromptText("Не указана");
    pressure.setPromptText("Не указано");
    for (final TextField field : List.of(temperature, duration, pressure)) {
      field.setPrefWidth(160);
      field.setMaxWidth(160);
    }
    note.setPromptText(
        "Атмосфера, перемешивание, порядок добавления или другие особенности опыта…");
    final VBox parameters =
        card("Условия реакции", new FlowPane(8, 6, new Label("Направление"), arrow), conditions);
    final VBox solvents = card("Растворители и добавки", this.materials);
    solvents.setId("chemistry-stage-solvents-card");
    final VBox notes = card("Примечания к стадии", note);
    final VBox conditionContent = new VBox(14, parameters, solvents, notes);
    conditionContent.setPadding(new Insets(16, 18, 18, 18));
    conditionContent.setMaxWidth(1120);
    final TabPane sections =
        new TabPane(
            new Tab("Участники", scroll(participantContent)),
            new Tab("Условия", scroll(conditionContent)));
    sections.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    sections.setId("chemistry-stage-sections");
    final VBox stageTitle = new VBox(6, new Label("Название стадии"), title);
    stageTitle.setPadding(new Insets(0, 18, 8, 18));
    final VBox header = new VBox(8, navigationHeader, stageTitle);
    this.setTop(header);
    this.setCenter(sections);
    this.setId("chemistry-stage-form");
    final Button save = new Button("Применить стадию");
    save.setId("chemistry-stage-apply");
    save.getStyleClass().add("laboratory-save");
    save.setOnAction(
        event -> {
          if (this.applied) {
            return;
          }
          try {
            if (this.recoveredConflict || !this.current()) {
              throw new IllegalArgumentException(
                  "Опыт или участники изменились. Скопируйте нужный ввод и откройте стадию заново.");
            }
            final ArrayList<LaboratoryStageParticipant> participants = new ArrayList<>();
            for (final Row row : rows) {
              if (!row.included().isSelected()) {
                continue;
              }
              final boolean side =
                  row.role().getValue() == ReactionComponentRole.REACTANT
                      || row.role().getValue() == ReactionComponentRole.PRODUCT;
              participants.add(
                  new LaboratoryStageParticipant(
                      row.id(),
                      row.role().getValue(),
                      !side || row.coefficient().getText().isBlank()
                          ? null
                          : coefficient(row.coefficient().getText())));
            }
            final var nextConditions =
                ReactionConditions.of(
                    temperature.getText().isBlank()
                        ? null
                        : Temperature.of(number(temperature), temperatureUnit.getValue()),
                    pressure.getText().isBlank()
                        ? null
                        : Pressure.of(number(pressure), pressureUnit.getValue()),
                    duration.getText().isBlank()
                        ? null
                        : TimeDuration.of(number(duration), durationUnit.getValue()),
                    this.materials.appliedValues(),
                    original.features(),
                    note.getText().isBlank() ? null : note.getText());
            final var next =
                new LaboratoryReactionStage(
                    this.id,
                    title.getText(),
                    arrow.getValue(),
                    nextConditions,
                    participants,
                    this.previous == null
                        ? LaboratoryAtomMapping.EMPTY
                        : this.previous.atomMapping(),
                    this.previous == null
                        ? LaboratoryReactionClassification.EMPTY
                        : this.previous.classification());
            final ArrayList<LaboratoryReactionStage> stages =
                new ArrayList<>(this.editor.document().experiment().stages());
            if (this.previous == null) {
              stages.add(next);
            } else {
              stages.set(stages.indexOf(this.previous), next);
            }
            this.editor.stages(stages);
            this.applied = true;
            close.run();
          } catch (final RuntimeException exception) {
            this.error.setText(
                exception instanceof NumberFormatException
                    ? "Проверьте числа: коэффициенты — положительные целые, условия — числовые значения."
                    : exception.getMessage() == null
                        ? "Проверьте поля стадии."
                        : exception.getMessage());
          }
        });
    final Button discard = new Button("Отменить ввод");
    discard.setId("chemistry-stage-discard");
    discard.setOnAction(
        event -> {
          this.applied = true;
          close.run();
        });
    final Button keep = new Button("Продолжить заполнение");
    keep.setOnAction(
        event -> {
          this.confirmation.setVisible(false);
          this.confirmation.setManaged(false);
          this.error.setText("");
        });
    this.confirmation.getChildren().setAll(new Label("Изменения ещё не применены."), discard, keep);
    this.confirmation.setVisible(false);
    this.confirmation.setManaged(false);
    final VBox footer = new VBox(8, this.error, this.confirmation, new FlowPane(10, 8, save));
    footer.setPadding(new Insets(12, 18, 12, 18));
    footer.getStyleClass().add("laboratory-form-footer");
    this.setBottom(footer);
    this.initial = this.fields();
  }

  private static VBox card(final String title, final javafx.scene.Node... content) {
    final Label heading = new Label(title);
    heading.getStyleClass().add("laboratory-form-section-title");
    final VBox card = new VBox(10, heading);
    card.getChildren().addAll(content);
    card.getStyleClass().add("laboratory-form-card");
    return card;
  }

  void navigationContext(final String experimentTitle, final String backCaption) {
    final String title =
        experimentTitle == null || experimentTitle.isBlank() ? "Опыт" : experimentTitle.strip();
    this.navigation.setText("Проекты › " + title + " › Участники и условия");
    this.navigation.setTooltip(new Tooltip(this.navigation.getText()));
    this.back.setText(backCaption == null || backCaption.isBlank() ? "← Назад" : backCaption);
  }

  boolean pending() {
    return !this.applied && (this.recoveredConflict || !this.initial.equals(this.fields()));
  }

  boolean canClose() {
    if (!this.pending()) {
      return true;
    }
    this.confirmation.setVisible(true);
    this.confirmation.setManaged(true);
    this.error.setText("Примените стадию или явно отмените ввод перед закрытием.");
    return false;
  }

  private boolean current() {
    final var current = this.editor.document().experiment();
    return current.id().equals(this.experiment.id())
        && current.components().equals(this.experiment.components())
        && (this.previous == null
            ? current.stages().stream().noneMatch(stage -> stage.id().equals(this.id))
            : current.stages().contains(this.previous));
  }

  private java.util.Properties fields() {
    final var fields = new java.util.Properties();
    LaboratoryFormDraft.fields(fields, "stageForm.fields", false, this.controls);
    this.materials.draft(fields, false);
    return fields;
  }

  void draft(final java.util.Properties fields, final boolean restore) {
    if (restore) {
      if (!this.experiment.id().equals(fields.getProperty("stageForm.experiment"))
          || !(this.previous == null ? "" : this.previous.id())
              .equals(fields.getProperty("stageForm.previous"))) {
        throw new IllegalArgumentException("Черновик стадии относится к другому опыту или стадии.");
      }
      this.id =
          ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue.requireIdentifier(
              fields.getProperty("stageForm.id"), "Stage draft id");
      if (this.previous != null && !this.id.equals(this.previous.id())) {
        throw new IllegalArgumentException("Идентификатор изменяемой стадии не совпадает.");
      }
      for (final var row : this.rows) {
        if (!fields.containsKey("stageForm.fields.component." + row.id() + ".included")) {
          throw new IllegalArgumentException("Состав участников черновика изменился.");
        }
      }
      this.controls.forEach(
          (name, control) -> {
            final String value = fields.getProperty("stageForm.fields." + name);
            if (value == null
                || control instanceof CheckBox && !value.equals("true") && !value.equals("false")
                || control instanceof ComboBox<?> combo
                    && combo.getItems().stream()
                        .noneMatch(
                            item ->
                                item instanceof Enum<?> option && option.name().equals(value))) {
              throw new IllegalArgumentException("Повреждено поле черновика стадии: " + name);
            }
          });
      if (fields.stringPropertyNames().stream()
          .anyMatch(
              key ->
                  key.startsWith("stageForm.fields.component.")
                      && !this.controls.containsKey(key.substring("stageForm.fields.".length())))) {
        throw new IllegalArgumentException("Черновик содержит отсутствующего участника стадии.");
      }
      LaboratoryFormDraft.fields(fields, "stageForm.fields", true, this.controls);
      this.materials.draft(fields, true);
      this.recoveredConflict =
          Boolean.parseBoolean(fields.getProperty("stageForm.conflict", "false"))
              || !this.current();
      this.error.setText(
          this.recoveredConflict
              ? "Ввод восстановлен для просмотра: исходная стадия изменилась."
              : "Восстановлен незавершённый ввод стадии. Проверьте поля и примените изменения.");
    } else {
      fields.putAll(this.fields());
      fields.setProperty("stageForm.id", this.id);
      fields.setProperty("stageForm.previous", this.previous == null ? "" : this.previous.id());
      fields.setProperty("stageForm.experiment", this.experiment.id());
      fields.setProperty(
          "stageForm.conflict", Boolean.toString(this.recoveredConflict || !this.current()));
    }
  }

  private static ScrollPane scroll(final VBox content) {
    content.setMinHeight(Region.USE_PREF_SIZE);
    final ScrollPane scroll = new ScrollPane(content);
    scroll.setFitToWidth(true);
    scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    return scroll;
  }

  private static javafx.scene.canvas.Canvas thumbnail(
      final ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing drawing) {
    final var canvas = new javafx.scene.canvas.Canvas(126, 100);
    final var bounds = LaboratoryCanvas.previewBounds(drawing);
    final Runnable paint =
        () -> {
          final var g = canvas.getGraphicsContext2D();
          LaboratoryPreviewTheme.clear(g);
          g.save();
          try {
            final double scale =
                Math.min(
                    1.5,
                    Math.min(
                        110 / Math.max(1, bounds.getWidth()),
                        84 / Math.max(1, bounds.getHeight())));
            g.translate(
                63 - scale * (bounds.getMinX() + bounds.getMaxX()) / 2,
                50 - scale * (bounds.getMinY() + bounds.getMaxY()) / 2);
            g.scale(scale, scale);
            LaboratoryPreviewTheme.paint(g, drawing);
          } finally {
            g.restore();
          }
        };
    LaboratoryPreviewTheme.track(canvas, paint);
    paint.run();
    return canvas;
  }

  private static StoichiometricCoefficient coefficient(final String value) {
    try {
      return StoichiometricCoefficient.of(Integer.parseInt(value.trim()));
    } catch (final IllegalArgumentException exception) {
      throw new IllegalArgumentException(
          "Коэффициент должен быть целым числом от 1 до 1000000. Пустое поле означает «не указан».");
    }
  }

  private static double number(final TextField text) {
    return Double.parseDouble(text.getText().trim().replace(',', '.'));
  }
}

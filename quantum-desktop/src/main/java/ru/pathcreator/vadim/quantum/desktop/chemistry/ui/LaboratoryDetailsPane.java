/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.List;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryExperiment;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Temperature;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TemperatureUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TimeDuration;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TimeDurationUnit;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEditor;

final class LaboratoryDetailsPane extends VBox {

  private final TextField title = new TextField();
  private final TextField temperature = new TextField();
  private final ComboBox<TemperatureUnit> temperatureUnit = new ComboBox<>();
  private final TextField duration = new TextField();
  private final ComboBox<TimeDurationUnit> durationUnit = new ComboBox<>();
  private final TextArea conditions = new TextArea();
  private final TextArea notes = new TextArea();
  private List<String> displayed = List.of();

  LaboratoryDetailsPane(final Runnable changed) {
    this.setSpacing(7);
    this.title.setId("chemistry-title");
    this.temperature.setId("chemistry-temperature");
    this.duration.setId("chemistry-duration");
    this.notes.setId("chemistry-notes");
    this.temperature.setPromptText("Не указана");
    this.duration.setPromptText("Не указана");
    this.temperatureUnit.getItems().setAll(TemperatureUnit.values());
    this.durationUnit.getItems().setAll(TimeDurationUnit.values());
    this.temperatureUnit.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(final TemperatureUnit unit) {
            return unit == null ? "" : unit == TemperatureUnit.CELSIUS ? "°C" : "K";
          }

          public TemperatureUnit fromString(final String text) {
            throw new UnsupportedOperationException();
          }
        });
    this.durationUnit.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(final TimeDurationUnit unit) {
            if (unit == null) {
              return "";
            }
            return switch (unit) {
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
    this.conditions.setPrefRowCount(3);
    this.notes.setPrefRowCount(5);
    this.conditions.setWrapText(true);
    this.conditions.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px;");
    this.notes.setWrapText(true);
    this.notes.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px;");
    this.getChildren()
        .addAll(
            new Label("Опыт · черновик"),
            this.title,
            new Label("Плановая температура"),
            quantity(this.temperature, this.temperatureUnit),
            new Label("Плановая длительность"),
            quantity(this.duration, this.durationUnit),
            new Label("Условия · свободная запись"),
            this.conditions,
            new Label("Заметки к опыту"),
            this.notes);
    for (final TextField field : List.of(this.title, this.temperature, this.duration)) {
      field.textProperty().addListener((observable, previous, value) -> changed.run());
    }
    this.conditions.textProperty().addListener((observable, previous, value) -> changed.run());
    this.notes.textProperty().addListener((observable, previous, value) -> changed.run());
    this.temperatureUnit
        .valueProperty()
        .addListener((observable, previous, value) -> changed.run());
    this.durationUnit.valueProperty().addListener((observable, previous, value) -> changed.run());
  }

  boolean pending() {
    return !this.displayed.equals(this.values());
  }

  void load(final LaboratoryExperiment experiment) {
    final ReactionConditions current = experiment.conditions();
    this.title.setText(experiment.title());
    this.temperature.setText(
        current.temperature() == null ? "" : Double.toString(current.temperature().value()));
    this.temperatureUnit.setValue(
        current.temperature() == null ? TemperatureUnit.CELSIUS : current.temperature().unit());
    this.duration.setText(
        current.duration() == null ? "" : Double.toString(current.duration().value()));
    this.durationUnit.setValue(
        current.duration() == null ? TimeDurationUnit.HOUR : current.duration().unit());
    this.conditions.setText(current.note() == null ? "" : current.note());
    this.notes.setText(experiment.notes());
    this.displayed = this.values();
  }

  void apply(final LaboratoryEditor editor) {
    if (!this.pending()) {
      return;
    }
    final ReactionConditions previous = editor.document().experiment().conditions();
    final Temperature nextTemperature =
        this.temperature.getText().isBlank()
            ? null
            : Temperature.of(number(this.temperature.getText()), this.temperatureUnit.getValue());
    final TimeDuration nextDuration =
        this.duration.getText().isBlank()
            ? null
            : TimeDuration.of(number(this.duration.getText()), this.durationUnit.getValue());
    editor.details(
        this.title.getText(),
        ReactionConditions.of(
            nextTemperature,
            previous.pressure(),
            nextDuration,
            previous.components(),
            previous.features(),
            this.conditions.getText().isBlank() ? null : this.conditions.getText()),
        this.notes.getText());
    this.displayed = this.values();
  }

  private List<String> values() {
    return List.of(
        this.title.getText(),
        this.temperature.getText(),
        this.duration.getText(),
        String.valueOf(this.temperatureUnit.getValue()),
        String.valueOf(this.durationUnit.getValue()),
        this.conditions.getText(),
        this.notes.getText());
  }

  private static javafx.scene.layout.HBox quantity(final TextField value, final ComboBox<?> unit) {
    value.setMinWidth(60);
    unit.setMinWidth(78);
    unit.setPrefWidth(90);
    final var row = new javafx.scene.layout.HBox(8, value, unit);
    javafx.scene.layout.HBox.setHgrow(value, javafx.scene.layout.Priority.ALWAYS);
    return row;
  }

  private static double number(final String text) {
    return Double.parseDouble(text.trim().replace(',', '.'));
  }

  void draft(final java.util.Properties data, final boolean restore) {
    LaboratoryFormDraft.fields(
        data,
        "details",
        restore,
        java.util.Map.of(
            "title",
            this.title,
            "temperature",
            this.temperature,
            "temperatureUnit",
            this.temperatureUnit,
            "duration",
            this.duration,
            "durationUnit",
            this.durationUnit,
            "conditions",
            this.conditions,
            "notes",
            this.notes));
  }
}

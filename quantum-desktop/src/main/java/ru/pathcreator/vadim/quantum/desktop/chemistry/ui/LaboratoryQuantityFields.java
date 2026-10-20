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
import java.util.List;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryQuantity;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryQuantityUnit;

final class LaboratoryQuantityFields extends VBox {
  private final TextField planned = new LaboratoryDecimalField();
  private final TextField actual = new LaboratoryDecimalField();
  private final ComboBox<LaboratoryQuantityUnit> planUnit = new ComboBox<>();
  private final ComboBox<LaboratoryQuantityUnit> actualUnit = new ComboBox<>();
  private final ComboBox<LaboratoryEvidence> evidence = new ComboBox<>();
  private boolean loading;
  private final Label planConversion;
  private final Label actualConversion;

  LaboratoryQuantityFields(final Runnable changed) {
    super(5);
    this.planConversion =
        LaboratoryQuantityConversion.bind(this.planned, this.planUnit, () -> this.loading);
    this.actualConversion =
        LaboratoryQuantityConversion.bind(this.actual, this.actualUnit, () -> this.loading);
    for (final var unit : List.of(this.planUnit, this.actualUnit)) {
      unit.getItems().setAll(LaboratoryQuantityUnit.values());
      unit.setValue(LaboratoryQuantityUnit.GRAM);
      unit.valueProperty().addListener((o, a, b) -> changed.run());
    }
    this.evidence.setId("chemistry-quantity-evidence");
    this.evidence.setMaxWidth(Double.MAX_VALUE);
    this.evidence.getItems().setAll(LaboratoryEvidence.values());
    this.evidence.setValue(LaboratoryEvidence.PLANNED);
    this.evidence.valueProperty().addListener((o, a, b) -> changed.run());
    for (final var field : List.of(this.planned, this.actual)) {
      field.setPromptText("Не указано");
      ((LaboratoryDecimalField) field)
          .valueRevisionProperty()
          .addListener((o, a, b) -> changed.run());
    }
    this.planned.setId("chemistry-planned-quantity");
    this.actual.setId("chemistry-actual-quantity");
    this.planUnit.setId("chemistry-planned-unit");
    this.actualUnit.setId("chemistry-actual-unit");
    this.getChildren()
        .addAll(
            new Label("Плановое количество"),
            row(this.planned, this.planUnit),
            planConversion,
            new Label("Внесённое количество"),
            row(this.actual, this.actualUnit),
            actualConversion,
            new Label("Происхождение внесённых данных"),
            this.evidence);
  }

  void load(final LaboratoryComponent component) {
    this.loading = true;
    this.planConversion.setText("");
    this.actualConversion.setText("");
    try {
      final var plan = component == null ? null : component.planned();
      final var fact = component == null ? null : component.actual();
      LaboratoryDecimalField.display(this.planned, plan == null ? null : plan.value());
      LaboratoryDecimalField.display(this.actual, fact == null ? null : fact.value());
      this.planUnit.setValue(plan == null ? LaboratoryQuantityUnit.GRAM : plan.unit());
      this.actualUnit.setValue(fact == null ? LaboratoryQuantityUnit.GRAM : fact.unit());
      this.evidence.setValue(component == null ? LaboratoryEvidence.PLANNED : component.evidence());
    } finally {
      this.loading = false;
    }
  }

  LaboratoryQuantity planned() {
    return quantity(this.planned, this.planUnit);
  }

  LaboratoryQuantity actual() {
    return quantity(this.actual, this.actualUnit);
  }

  LaboratoryEvidence evidence() {
    return this.evidence.getValue();
  }

  static HBox row(final TextField amount, final ComboBox<LaboratoryQuantityUnit> unit) {
    amount.setMinWidth(60);
    amount.setMaxWidth(Double.MAX_VALUE);
    unit.setMinWidth(90);
    HBox.setHgrow(amount, Priority.ALWAYS);
    return new HBox(8, amount, unit);
  }

  List<String> values() {
    return List.of(
        LaboratoryDecimalField.snapshot(this.planned),
        LaboratoryDecimalField.snapshot(this.actual),
        this.planUnit.getValue().name(),
        this.actualUnit.getValue().name(),
        this.evidence.getValue().name());
  }

  static LaboratoryQuantity quantity(
      final TextField field, final ComboBox<LaboratoryQuantityUnit> unit) {
    if (field.getText().isBlank()) {
      return null;
    }
    try {
      return new LaboratoryQuantity(LaboratoryDecimalField.value(field), unit.getValue());
    } catch (final NumberFormatException exception) {
      throw new IllegalArgumentException("Введите количество числом: " + field.getText());
    }
  }

  void draft(final java.util.Properties data, final boolean restore) {
    this.draft(data, "quantities", restore);
  }

  void draft(final java.util.Properties data, final String prefix, final boolean restore) {
    this.loading = restore;
    if (restore) {
      this.planConversion.setText("");
      this.actualConversion.setText("");
    }
    try {
      LaboratoryFormDraft.fields(
          data,
          prefix,
          restore,
          java.util.Map.of(
              "planned",
              this.planned,
              "actual",
              this.actual,
              "planUnit",
              this.planUnit,
              "actualUnit",
              this.actualUnit,
              "evidence",
              this.evidence));
    } finally {
      this.loading = false;
    }
  }
}

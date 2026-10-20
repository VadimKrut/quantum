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
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;

final class LaboratoryCoordinateFields extends VBox {
  private final TextField x = new TextField();
  private final TextField y = new TextField();
  private final TextField z = new TextField();
  private final ComboBox<LengthUnit> unit = new ComboBox<>();

  LaboratoryCoordinateFields(final Runnable changed) {
    super(5);
    this.unit.getItems().setAll(LengthUnit.values());
    this.unit.setValue(LengthUnit.ANGSTROM);
    this.getChildren()
        .setAll(new Label("Координаты атома в ядре · X, Y, Z"), this.x, this.y, this.z, this.unit);
    for (final var field : List.of(this.x, this.y, this.z)) {
      field.setPromptText("Не указана");
      field.textProperty().addListener((o, a, b) -> changed.run());
    }
    this.unit.valueProperty().addListener((o, a, b) -> changed.run());
  }

  void load(final Coordinate3D coordinate) {
    this.x.setText(coordinate == null ? "" : Double.toString(coordinate.x()));
    this.y.setText(coordinate == null ? "" : Double.toString(coordinate.y()));
    this.z.setText(coordinate == null ? "" : Double.toString(coordinate.z()));
    this.unit.setValue(coordinate == null ? LengthUnit.ANGSTROM : coordinate.unit());
  }

  List<String> values() {
    return List.of(
        this.x.getText(), this.y.getText(), this.z.getText(), this.unit.getValue().name());
  }

  Coordinate3D coordinate() {
    if (this.x.getText().isBlank() && this.y.getText().isBlank() && this.z.getText().isBlank()) {
      return null;
    }
    try {
      return Coordinate3D.of(number(this.x), number(this.y), number(this.z), this.unit.getValue());
    } catch (final NumberFormatException exception) {
      throw new IllegalArgumentException(
          "Укажите все три координаты числом или оставьте все пустыми.");
    }
  }

  private static double number(final TextField field) {
    return Double.parseDouble(field.getText().trim().replace(',', '.'));
  }

  void draft(final java.util.Properties data, final boolean restore) {
    LaboratoryFormDraft.fields(
        data,
        "coordinates",
        restore,
        java.util.Map.of("x", this.x, "y", this.y, "z", this.z, "unit", this.unit));
  }
}

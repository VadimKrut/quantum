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
import java.util.function.BooleanSupplier;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryQuantity;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryQuantityUnit;

/** Changing a display unit must not change the physical amount. Raw drafts are restored separately. */
final class LaboratoryQuantityConversion {
  private LaboratoryQuantityConversion() {}

  static Label bind(
      final TextField amount,
      final ComboBox<LaboratoryQuantityUnit> unit,
      final BooleanSupplier loading) {
    final Label message = new Label();
    message.setWrapText(true);
    message.setMinHeight(Label.USE_PREF_SIZE);
    message.setStyle("-fx-text-fill: #925619;");
    message.managedProperty().bind(message.textProperty().isNotEmpty());
    message.visibleProperty().bind(message.managedProperty());
    final boolean[] updating = {false};
    amount.textProperty().addListener((o, a, b) -> message.setText(""));
    unit.valueProperty()
        .addListener(
            (o, before, after) -> {
              if (updating[0]
                  || loading.getAsBoolean()
                  || before == null
                  || after == null
                  || before == after) {
                return;
              }
              message.setText("");
              if (amount.getText().isBlank()) {
                return;
              }
              updating[0] = true;
              try {
                if (!before.dimension().equals(after.dimension())) {
                  throw new IllegalArgumentException(
                      "Масса, объём и моли требуют свойств вещества для пересчёта. Используйте форму загрузки; для нового измерения сначала очистите количество.");
                }
                final var original =
                    new LaboratoryQuantity(LaboratoryDecimalField.value(amount), before);
                LaboratoryDecimalField.display(amount, original.in(after).value());
              } catch (final RuntimeException exception) {
                unit.setValue(before);
                message.setText(
                    exception instanceof NumberFormatException
                        ? "Сначала исправьте число. Единица сохранена."
                        : exception.getMessage());
              } finally {
                updating[0] = false;
              }
            });
    return message;
  }
}

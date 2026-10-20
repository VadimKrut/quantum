/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Function;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;

/** Recovery metadata is raw UI input, never an inventory snapshot or an automatic posting. */
final class LaboratoryStockDraft {
  private LaboratoryStockDraft() {}

  static String id(final Properties restored, final String name) {
    return restored == null
        ? name + "_" + UUID.randomUUID()
        : IdentifierValue.requireIdentifier(
            restored.getProperty("stockForm." + name), "Идентификатор черновика " + name);
  }

  static void fields(
      final Properties data, final boolean restore, final Map<String, Control> controls) {
    if (restore) {
      controls.forEach(
          (name, control) -> {
            if (control instanceof ComboBox<?> combo
                && data.containsKey("stockForm.fields." + name)) {
              final String value = data.getProperty("stockForm.fields." + name);
              if (combo.getItems().stream()
                  .noneMatch(
                      item -> item instanceof Enum<?> option && option.name().equals(value))) {
                throw new IllegalArgumentException(
                    "Недопустимая единица или вид материала в черновике: " + value);
              }
            }
          });
    }
    LaboratoryFormDraft.fields(data, "stockForm.fields", restore, controls);
  }

  static <T> void choice(
      final Properties data,
      final String name,
      final boolean restore,
      final ComboBox<T> control,
      final Function<T, String> id) {
    final String key = "stockForm." + name;
    if (restore) {
      final String selected = data.getProperty(key, "");
      control.setValue(
          selected.isEmpty()
              ? null
              : control.getItems().stream()
                  .filter(item -> id.apply(item).equals(selected))
                  .findFirst()
                  .orElseThrow(
                      () ->
                          new IllegalArgumentException(
                              "Объект складского черновика не найден: " + selected)));
    } else {
      data.setProperty(key, control.getValue() == null ? "" : id.apply(control.getValue()));
    }
  }
}

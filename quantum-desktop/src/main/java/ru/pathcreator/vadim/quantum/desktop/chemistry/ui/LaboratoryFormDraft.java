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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextInputControl;

/** Preserve unfinished input as text, without converting it into a chemical fact. */
final class LaboratoryFormDraft {
  private LaboratoryFormDraft() {}

  static void fields(
      final Properties data,
      final String prefix,
      final boolean restore,
      final Map<String, Control> controls) {
    controls.forEach(
        (name, control) -> {
          final String key = prefix + "." + name;
          if (control instanceof LaboratoryDecimalField decimal) {
            decimal.draft(data, key, restore);
          } else if (control instanceof TextInputControl text) {
            if (restore) {
              if (data.containsKey(key)) {
                text.setText(data.getProperty(key));
              }
            } else {
              data.setProperty(key, text.getText());
            }
          } else if (control instanceof CheckBox check) {
            if (restore) {
              if (data.containsKey(key)) {
                check.setSelected(Boolean.parseBoolean(data.getProperty(key)));
              }
            } else {
              data.setProperty(key, Boolean.toString(check.isSelected()));
            }
          } else if (control instanceof ComboBox<?> combo) {
            if (restore) {
              for (int index = 0; index < combo.getItems().size(); index++) {
                final Object value = combo.getItems().get(index);
                if (value instanceof Enum<?> item && item.name().equals(data.getProperty(key))) {
                  combo.getSelectionModel().select(index);
                  break;
                }
              }
            } else if (combo.getValue() instanceof Enum<?> item) {
              data.setProperty(key, item.name());
            }
          }
        });
  }
}

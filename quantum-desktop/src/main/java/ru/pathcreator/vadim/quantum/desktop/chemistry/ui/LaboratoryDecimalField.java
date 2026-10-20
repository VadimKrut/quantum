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
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Properties;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;

/** A readable projection of an exact decimal. User edits replace the projection, never the reverse. */
final class LaboratoryDecimalField extends TextField {
  private BigDecimal exact;
  private BigDecimal undoBaseline;
  private boolean displaying;
  private boolean editing;
  private final int significantDigits;
  private final ReadOnlyLongWrapper valueRevision = new ReadOnlyLongWrapper();

  LaboratoryDecimalField() {
    this(8);
  }

  LaboratoryDecimalField(final int significantDigits) {
    if (significantDigits < 1 || significantDigits > 16) {
      throw new IllegalArgumentException("Invalid decimal display precision");
    }
    this.significantDigits = significantDigits;
    this.setTextFormatter(
        new TextFormatter<String>(
            change -> {
              // setText starts a new native undo history; undo/redo bypass this filter.
              if (change.isContentChange() && !this.displaying && !this.editing) {
                this.exact = null;
                this.undoBaseline = null;
                this.setTooltip(null);
              }
              return change;
            }));
    this.textProperty()
        .addListener(
            (o, a, b) -> {
              if (!this.displaying) {
                this.exact = null;
              }
              this.updateExactTooltip();
            });
    this.undoableProperty().addListener((o, a, b) -> this.updateExactTooltip());
    this.redoableProperty().addListener((o, a, b) -> this.updateExactTooltip());
  }

  @Override
  public void replaceText(final int start, final int end, final String text) {
    final boolean previous = this.editing;
    this.editing = true;
    try {
      super.replaceText(start, end, text);
    } finally {
      this.editing = previous;
    }
  }

  void display(final BigDecimal value) {
    this.exact = value;
    this.undoBaseline = value;
    this.displaying = true;
    try {
      this.setText(value == null ? "" : format(value));
    } finally {
      this.displaying = false;
      this.updateExactTooltip();
    }
  }

  private BigDecimal projectedValue() {
    if (this.exact != null) {
      return this.exact;
    }
    if (this.undoBaseline != null
        && !this.isUndoable()
        && this.isRedoable()
        && format(this.undoBaseline).equals(this.getText())) {
      return this.undoBaseline;
    }
    return null;
  }

  private void updateExactTooltip() {
    final BigDecimal value = this.projectedValue();
    this.setTooltip(
        value == null || value.compareTo(new BigDecimal(this.getText())) == 0
            ? null
            : new Tooltip(
                "Точное значение: "
                    + value.toPlainString()
                    + "\nВ расчёте и файле сохраняется полная точность."));
    this.valueRevision.set(this.valueRevision.get() + 1);
  }

  /** Exact values and native undo state can change while the rounded text stays the same. */
  ReadOnlyLongProperty valueRevisionProperty() {
    return this.valueRevision.getReadOnlyProperty();
  }

  void draft(final Properties data, final String key, final boolean restore) {
    if (restore) {
      if (!data.containsKey(key)) {
        return;
      }
      final String text = data.getProperty(key), stored = data.getProperty(key + ".exact");
      if (stored != null) {
        final BigDecimal value = new BigDecimal(stored);
        final int precision = Integer.parseInt(data.getProperty(key + ".precision", "8"));
        if (precision < 1
            || precision > 16
            || !value
                .round(new MathContext(precision, RoundingMode.HALF_UP))
                .stripTrailingZeros()
                .toPlainString()
                .equals(text)) {
          throw new IllegalArgumentException(
              "Точное число не соответствует отображению в черновике: " + key);
        }
        this.display(value);
      } else {
        this.exact = null;
        this.undoBaseline = null;
        this.setTooltip(null);
        this.setText(text);
      }
    } else {
      data.setProperty(key, this.getText());
      data.remove(key + ".exact");
      data.remove(key + ".precision");
      final BigDecimal value = this.projectedValue();
      if (value != null) {
        data.setProperty(key + ".exact", value.toPlainString());
        data.setProperty(key + ".precision", Integer.toString(this.significantDigits));
      }
    }
  }

  static BigDecimal value(final TextField field) {
    if (field instanceof LaboratoryDecimalField decimal && decimal.projectedValue() != null) {
      return decimal.projectedValue();
    }
    return field.getText().isBlank()
        ? null
        : new BigDecimal(field.getText().strip().replace(',', '.'));
  }

  static void display(final TextField field, final BigDecimal value) {
    if (field instanceof LaboratoryDecimalField decimal) {
      decimal.display(value);
    } else {
      field.setText(value == null ? "" : value.toPlainString());
    }
  }

  static String snapshot(final TextField field) {
    return field instanceof LaboratoryDecimalField decimal && decimal.projectedValue() != null
        ? decimal.projectedValue().toPlainString()
        : field.getText();
  }

  private String format(final BigDecimal value) {
    return value
        .round(new MathContext(this.significantDigits, RoundingMode.HALF_UP))
        .stripTrailingZeros()
        .toPlainString();
  }
}

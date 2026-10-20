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
import java.util.function.Function;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryQuantity;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryQuantityUnit;

/** Compact read-only quantities always retain an exact tooltip; calculations use the original values. */
final class LaboratoryQuantityText {
  private LaboratoryQuantityText() {}

  static String format(final LaboratoryQuantity quantity, final boolean compact) {
    return format(quantity.value(), quantity.unit(), compact);
  }

  static String format(
      final BigDecimal value, final LaboratoryQuantityUnit unit, final boolean compact) {
    final BigDecimal shown =
        compact ? value.round(new MathContext(5, RoundingMode.HALF_UP)) : value;
    return (shown.compareTo(value) == 0 ? "" : "≈ ")
        + shown.stripTrailingZeros().toPlainString()
        + " "
        + unit;
  }

  static Label label(final String prefix, final LaboratoryQuantity quantity) {
    final Label label = new Label(prefix + format(quantity, true));
    label.setWrapText(true);
    label.setTooltip(new Tooltip(prefix + format(quantity, false)));
    return label;
  }

  static void bind(
      final Label label, final Function<Boolean, String> text, final Observable... dependencies) {
    label.textProperty().bind(Bindings.createStringBinding(() -> text.apply(true), dependencies));
    final Tooltip tooltip = new Tooltip();
    tooltip
        .textProperty()
        .bind(Bindings.createStringBinding(() -> text.apply(false), dependencies));
    label.setTooltip(tooltip);
  }
}

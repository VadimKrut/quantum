/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Display precision follows visible tick spacing, including a zoomed ppm axis. */
final class LaboratoryAxisTickText {
  private LaboratoryAxisTickText() {}

  static String format(final double value, final double spacing) {
    if (!Double.isFinite(value)) return "—";
    final double step = Math.abs(spacing);
    if (!Double.isFinite(step) || step <= 0)
      return BigDecimal.valueOf(value).stripTrailingZeros().toString();
    final int scale = Math.max(-300, Math.min(300, 1 - (int) Math.floor(Math.log10(step))));
    final var number =
        BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros();
    final String plain = number.toPlainString();
    if (plain.length() <= 11) return plain;
    final int digits =
        Math.max(
            2,
            Math.min(
                15,
                2
                    + (int) Math.floor(Math.log10(Math.max(Math.abs(value), step)))
                    - (int) Math.floor(Math.log10(step))));
    return number
        .round(new MathContext(digits, RoundingMode.HALF_UP))
        .stripTrailingZeros()
        .toString();
  }
}

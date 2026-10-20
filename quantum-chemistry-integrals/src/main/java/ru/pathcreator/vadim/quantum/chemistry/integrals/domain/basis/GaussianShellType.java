/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis;

import java.util.Locale;

/** Тип Gaussian shell, который хранит физический angular momentum без привязки к формату basis. */
public enum GaussianShellType {
  S("s", 0),
  P("p", 1),
  D("d", 2),
  F("f", 3),
  G("g", 4),
  H("h", 5),
  I("i", 6),
  K("k", 7),
  L("l", 8),
  M("m", 9),
  N("n", 10),
  O("o", 11),
  Q("q", 12);

  private final String symbol;
  private final int angularMomentum;

  GaussianShellType(final String symbol, final int angularMomentum) {
    this.symbol = symbol;
    this.angularMomentum = angularMomentum;
  }

  public static GaussianShellType ofAngularMomentum(final int angularMomentum) {
    for (final GaussianShellType shellType : GaussianShellType.values()) {
      if (shellType.angularMomentum == angularMomentum) {
        return shellType;
      }
    }
    throw new IllegalArgumentException(
        "Gaussian shell angular momentum is outside supported "
            + GaussianShellType.supportedSymbolRange()
            + " range.");
  }

  public static GaussianShellType ofSymbol(final String symbol) {
    if (symbol == null || symbol.isBlank()) {
      throw new IllegalArgumentException("Gaussian shell symbol must not be blank.");
    }
    final String normalized = symbol.toLowerCase(Locale.ROOT);
    for (final GaussianShellType shellType : GaussianShellType.values()) {
      if (shellType.symbol.equals(normalized)) {
        return shellType;
      }
    }
    throw new IllegalArgumentException(
        "Gaussian shell symbol is outside supported "
            + GaussianShellType.supportedSymbolRange()
            + " range: "
            + symbol
            + ".");
  }

  public static boolean shellSymbol(final String symbol) {
    if (symbol == null || symbol.isBlank()) {
      return false;
    }
    final String normalized = symbol.toLowerCase(Locale.ROOT);
    for (final GaussianShellType shellType : GaussianShellType.values()) {
      if (shellType.symbol.equals(normalized)) {
        return true;
      }
    }
    return false;
  }

  public static String supportedSymbolRange() {
    return "s/p/d/f/g/h/i/k/l/m/n/o/q";
  }

  public String symbol() {
    return this.symbol;
  }

  public int angularMomentum() {
    return this.angularMomentum;
  }

  public int cartesianFunctionCount() {
    return (this.angularMomentum + 1) * (this.angularMomentum + 2) / 2;
  }
}

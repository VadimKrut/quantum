/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.valence;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;

public final class ValenceRule {

  private final ElementSymbol symbol;
  private final double maximumBondOrderSum;

  private ValenceRule(
      final ElementSymbol symbol,
      final double maximumBondOrderSum
  ) {
    this.symbol = symbol;
    this.maximumBondOrderSum = maximumBondOrderSum;
  }

  public static ValenceRule of(
      final ElementSymbol symbol,
      final double maximumBondOrderSum
  ) {
    if (symbol == null) {
      throw new IllegalArgumentException("Valence rule element symbol must not be null.");
    }
    if (!Double.isFinite(maximumBondOrderSum)) {
      throw new IllegalArgumentException("Valence rule maximum bond order sum must be finite.");
    }
    if (maximumBondOrderSum <= 0.0) {
      throw new IllegalArgumentException("Valence rule maximum bond order sum must be positive.");
    }
    return new ValenceRule(symbol, maximumBondOrderSum);
  }

  public ElementSymbol symbol() {
    return this.symbol;
  }

  public double maximumBondOrderSum() {
    return this.maximumBondOrderSum;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ValenceRule)) {
      return false;
    }
    final ValenceRule rule = (ValenceRule) other;
    return Double.compare(this.maximumBondOrderSum, rule.maximumBondOrderSum) == 0
        && Objects.equals(this.symbol, rule.symbol);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.symbol);
    result = ChemistryHash.include(result, this.maximumBondOrderSum);
    return result;
  }
}
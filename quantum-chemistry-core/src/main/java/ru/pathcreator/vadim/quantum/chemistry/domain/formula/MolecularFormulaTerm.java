/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.formula;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;

/** Один член молекулярной формулы: химический элемент и положительное количество атомов. */
public final class MolecularFormulaTerm {

  private final ElementSymbol symbol;
  private final int count;

  private MolecularFormulaTerm(
      final ElementSymbol symbol,
      final int count
  ) {
    this.symbol = symbol;
    this.count = count;
  }

  public static MolecularFormulaTerm of(
      final ElementSymbol symbol,
      final int count
  ) {
    if (symbol == null) {
      throw new IllegalArgumentException("Formula element symbol must not be null.");
    }
    if (count <= 0) {
      throw new IllegalArgumentException("Formula element count must be positive.");
    }
    return new MolecularFormulaTerm(symbol, count);
  }

  public ElementSymbol symbol() {
    return this.symbol;
  }

  public int count() {
    return this.count;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MolecularFormulaTerm)) {
      return false;
    }
    final MolecularFormulaTerm term = (MolecularFormulaTerm) other;
    return this.count == term.count && Objects.equals(this.symbol, term.symbol);
  }

  public int hashCode() {
    int result = this.symbol.hashCode();
    result = 31 * result + this.count;
    return result;
  }
}
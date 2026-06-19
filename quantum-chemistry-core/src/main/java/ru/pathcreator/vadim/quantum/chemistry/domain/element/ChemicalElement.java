/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.element;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

public final class ChemicalElement {

  private final int atomicNumber;
  private final ElementSymbol symbol;
  private final String englishName;
  private final double atomicMass;

  private ChemicalElement(
      final int atomicNumber, final ElementSymbol symbol, final String englishName, final double atomicMass) {
    this.atomicNumber = atomicNumber;
    this.symbol = symbol;
    this.englishName = englishName;
    this.atomicMass = atomicMass;
  }

  public static ChemicalElement of(
      final int atomicNumber, final ElementSymbol symbol, final String englishName, final double atomicMass) {
    if (atomicNumber < 1 || atomicNumber > 118) {
      throw new IllegalArgumentException("Atomic number must be from 1 to 118.");
    }
    if (symbol == null) {
      throw new IllegalArgumentException("Element symbol must not be null.");
    }
    final String checkedName = TextValue.requireText(englishName, "Element name");
    if (!Double.isFinite(atomicMass) || atomicMass <= 0.0) {
      throw new IllegalArgumentException("Atomic mass must be finite and positive.");
    }
    return new ChemicalElement(atomicNumber, symbol, checkedName, atomicMass);
  }

  public int atomicNumber() {
    return this.atomicNumber;
  }

  public ElementSymbol symbol() {
    return this.symbol;
  }

  public String englishName() {
    return this.englishName;
  }

  public double atomicMass() {
    return this.atomicMass;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemicalElement)) {
      return false;
    }
    final ChemicalElement element = (ChemicalElement) other;
    return this.atomicNumber == element.atomicNumber
        && Double.compare(this.atomicMass, element.atomicMass) == 0
        && Objects.equals(this.symbol, element.symbol)
        && Objects.equals(this.englishName, element.englishName);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.atomicNumber);
    result = ChemistryHash.include(result, this.symbol);
    result = ChemistryHash.include(result, this.englishName);
    result = ChemistryHash.include(result, this.atomicMass);
    return result;
  }
}
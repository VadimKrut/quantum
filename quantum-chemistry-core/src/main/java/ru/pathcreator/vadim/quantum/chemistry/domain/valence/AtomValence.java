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
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;

public final class AtomValence {

  private final AtomId atomId;
  private final ElementSymbol symbol;
  private final int bondCount;
  private final double bondOrderSum;
  private final boolean hasUnknownBondOrder;

  private AtomValence(
      final AtomId atomId,
      final ElementSymbol symbol,
      final int bondCount,
      final double bondOrderSum,
      final boolean hasUnknownBondOrder) {
    this.atomId = atomId;
    this.symbol = symbol;
    this.bondCount = bondCount;
    this.bondOrderSum = bondOrderSum;
    this.hasUnknownBondOrder = hasUnknownBondOrder;
  }

  public static AtomValence of(
      final AtomId atomId,
      final ElementSymbol symbol,
      final int bondCount,
      final double bondOrderSum,
      final boolean hasUnknownBondOrder) {
    if (atomId == null) {
      throw new IllegalArgumentException("Atom valence atom id must not be null.");
    }
    if (symbol == null) {
      throw new IllegalArgumentException("Atom valence element symbol must not be null.");
    }
    if (bondCount < 0) {
      throw new IllegalArgumentException("Atom valence bond count must not be negative.");
    }
    if (!Double.isFinite(bondOrderSum)) {
      throw new IllegalArgumentException("Atom valence bond order sum must be finite.");
    }
    if (bondOrderSum < 0.0) {
      throw new IllegalArgumentException("Atom valence bond order sum must not be negative.");
    }
    return new AtomValence(atomId, symbol, bondCount, bondOrderSum, hasUnknownBondOrder);
  }

  public AtomId atomId() {
    return this.atomId;
  }

  public ElementSymbol symbol() {
    return this.symbol;
  }

  public int bondCount() {
    return this.bondCount;
  }

  public double bondOrderSum() {
    return this.bondOrderSum;
  }

  public boolean hasUnknownBondOrder() {
    return this.hasUnknownBondOrder;
  }

  public boolean exceeds(final ValenceRule rule) {
    if (rule == null) {
      return false;
    }
    return this.bondOrderSum > rule.maximumBondOrderSum();
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof AtomValence)) {
      return false;
    }
    final AtomValence valence = (AtomValence) other;
    return this.bondCount == valence.bondCount
        && Double.compare(this.bondOrderSum, valence.bondOrderSum) == 0
        && this.hasUnknownBondOrder == valence.hasUnknownBondOrder
        && Objects.equals(this.atomId, valence.atomId)
        && Objects.equals(this.symbol, valence.symbol);
  }

  public int hashCode() {
    return Objects.hash(
        this.atomId, this.symbol, this.bondCount, this.bondOrderSum, this.hasUnknownBondOrder);
  }
}
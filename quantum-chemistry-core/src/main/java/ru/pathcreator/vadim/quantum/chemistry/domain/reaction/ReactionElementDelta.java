/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.reaction;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;

public final class ReactionElementDelta {

  private final ElementSymbol symbol;
  private final long reactantCount;
  private final long productCount;
  private final long delta;

  private ReactionElementDelta(
      final ElementSymbol symbol,
      final long reactantCount,
      final long productCount
  ) {
    this.symbol = symbol;
    this.reactantCount = reactantCount;
    this.productCount = productCount;
    this.delta = Math.subtractExact(productCount, reactantCount);
  }

  public static ReactionElementDelta of(
      final ElementSymbol symbol, final long reactantCount, final long productCount) {
    if (symbol == null) {
      throw new IllegalArgumentException("Reaction element delta symbol must not be null.");
    }
    if (reactantCount < 0L || productCount < 0L) {
      throw new IllegalArgumentException("Reaction element delta counts must not be negative.");
    }
    return new ReactionElementDelta(symbol, reactantCount, productCount);
  }

  public ElementSymbol symbol() {
    return this.symbol;
  }

  public long reactantCount() {
    return this.reactantCount;
  }

  public long productCount() {
    return this.productCount;
  }

  public long delta() {
    return this.delta;
  }

  public boolean balanced() {
    return this.delta == 0L;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionElementDelta)) {
      return false;
    }
    final ReactionElementDelta elementDelta = (ReactionElementDelta) other;
    return this.reactantCount == elementDelta.reactantCount
        && this.productCount == elementDelta.productCount
        && this.delta == elementDelta.delta
        && Objects.equals(this.symbol, elementDelta.symbol);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.symbol);
    result = ChemistryHash.include(result, this.reactantCount);
    result = ChemistryHash.include(result, this.productCount);
    result = ChemistryHash.include(result, this.delta);
    return result;
  }
}
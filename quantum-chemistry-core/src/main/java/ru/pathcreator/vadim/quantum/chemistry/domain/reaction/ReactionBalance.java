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

public final class ReactionBalance {

  private final boolean atomBalanced;
  private final boolean chargeBalanced;
  private final boolean massNumberBalanced;

  private ReactionBalance(
      final boolean atomBalanced, final boolean chargeBalanced, final boolean massNumberBalanced) {
    this.atomBalanced = atomBalanced;
    this.chargeBalanced = chargeBalanced;
    this.massNumberBalanced = massNumberBalanced;
  }

  public static ReactionBalance of(final Reaction reaction) {
    if (reaction == null) {
      throw new IllegalArgumentException("Reaction must not be null.");
    }
    final ReactionStoichiometry stoichiometry = ReactionStoichiometry.of(reaction);
    return new ReactionBalance(
        stoichiometry.atomBalanced(),
        stoichiometry.chargeBalanced(),
        stoichiometry.massNumberBalanced());
  }

  public boolean atomBalanced() {
    return this.atomBalanced;
  }

  public boolean chargeBalanced() {
    return this.chargeBalanced;
  }

  public boolean massNumberBalanced() {
    return this.massNumberBalanced;
  }

  public boolean balanced() {
    return this.atomBalanced && this.chargeBalanced;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionBalance)) {
      return false;
    }
    final ReactionBalance balance = (ReactionBalance) other;
    return this.atomBalanced == balance.atomBalanced
        && this.chargeBalanced == balance.chargeBalanced
        && this.massNumberBalanced == balance.massNumberBalanced;
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.atomBalanced);
    result = ChemistryHash.include(result, this.chargeBalanced);
    result = ChemistryHash.include(result, this.massNumberBalanced);
    return result;
  }
}
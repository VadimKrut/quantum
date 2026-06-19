/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.kinetics;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;

public final class ReactionOrderTerm {

  private static final double MAX_ORDER = 16.0;
  private final MoleculeId moleculeId;
  private final double order;

  private ReactionOrderTerm(
      final MoleculeId moleculeId,
      final double order
  ) {
    this.moleculeId = moleculeId;
    this.order = order;
  }

  public static ReactionOrderTerm of(
      final MoleculeId moleculeId,
      final double order
  ) {
    if (moleculeId == null) {
      throw new IllegalArgumentException("Reaction order molecule id must not be null.");
    }
    if (!Double.isFinite(order)) {
      throw new IllegalArgumentException("Reaction order must be finite.");
    }
    if (order < 0.0 || order > MAX_ORDER) {
      throw new IllegalArgumentException("Reaction order is outside supported bounds.");
    }
    return new ReactionOrderTerm(moleculeId, order);
  }

  public MoleculeId moleculeId() {
    return this.moleculeId;
  }

  public double order() {
    return this.order;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionOrderTerm)) {
      return false;
    }
    final ReactionOrderTerm term = (ReactionOrderTerm) other;
    return Double.compare(this.order, term.order) == 0
        && Objects.equals(this.moleculeId, term.moleculeId);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.moleculeId);
    result = ChemistryHash.include(result, this.order);
    return result;
  }
}
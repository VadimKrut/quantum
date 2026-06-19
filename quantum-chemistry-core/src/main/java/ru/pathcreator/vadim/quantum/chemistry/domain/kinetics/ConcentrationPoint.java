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
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.MolarConcentration;

public final class ConcentrationPoint {

  private final MoleculeId moleculeId;
  private final MolarConcentration concentration;

  private ConcentrationPoint(
      final MoleculeId moleculeId,
      final MolarConcentration concentration
  ) {
    this.moleculeId = moleculeId;
    this.concentration = concentration;
  }

  public static ConcentrationPoint of(
      final MoleculeId moleculeId,
      final MolarConcentration concentration
  ) {
    if (moleculeId == null) {
      throw new IllegalArgumentException("Concentration molecule id must not be null.");
    }
    if (concentration == null) {
      throw new IllegalArgumentException("Concentration value must not be null.");
    }
    return new ConcentrationPoint(moleculeId, concentration);
  }

  public MoleculeId moleculeId() {
    return this.moleculeId;
  }

  public MolarConcentration concentration() {
    return this.concentration;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ConcentrationPoint)) {
      return false;
    }
    final ConcentrationPoint point = (ConcentrationPoint) other;
    return Objects.equals(this.moleculeId, point.moleculeId)
        && Objects.equals(this.concentration, point.concentration);
  }

  public int hashCode() {
    return Objects.hash(this.moleculeId, this.concentration);
  }
}
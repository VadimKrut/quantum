/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;

public final class SpinSpinCoupling {

  private final AtomId firstAtomId;
  private final AtomId secondAtomId;
  private final double hertz;

  private SpinSpinCoupling(
      final AtomId firstAtomId,
      final AtomId secondAtomId,
      final double hertz
  ) {
    this.firstAtomId = firstAtomId;
    this.secondAtomId = secondAtomId;
    this.hertz = hertz;
  }

  public static SpinSpinCoupling of(
      final AtomId firstAtomId,
      final AtomId secondAtomId,
      final double hertz
  ) {
    if (firstAtomId == null || secondAtomId == null) {
      throw new IllegalArgumentException("NMR coupling atom ids must not be null.");
    }
    if (firstAtomId.equals(secondAtomId)) {
      throw new IllegalArgumentException("NMR coupling requires two different atoms.");
    }
    if (!Double.isFinite(hertz)) {
      throw new IllegalArgumentException("NMR coupling value must be finite.");
    }
    return new SpinSpinCoupling(firstAtomId, secondAtomId, hertz);
  }

  public AtomId firstAtomId() {
    return this.firstAtomId;
  }

  public AtomId secondAtomId() {
    return this.secondAtomId;
  }

  public double hertz() {
    return this.hertz;
  }

  public boolean connects(
      final AtomId first,
      final AtomId second
  ) {
    return this.firstAtomId.equals(first) && this.secondAtomId.equals(second)
        || this.firstAtomId.equals(second) && this.secondAtomId.equals(first);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof SpinSpinCoupling)) {
      return false;
    }
    final SpinSpinCoupling coupling = (SpinSpinCoupling) other;
    return Double.compare(this.hertz, coupling.hertz) == 0
        && Objects.equals(this.firstAtomId, coupling.firstAtomId)
        && Objects.equals(this.secondAtomId, coupling.secondAtomId);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.firstAtomId);
    result = ChemistryHash.include(result, this.secondAtomId);
    result = ChemistryHash.include(result, this.hertz);
    return result;
  }
}
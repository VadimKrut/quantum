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
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Isotope;

public final class NmrChemicalShift {

  private final AtomId atomId;
  private final Isotope isotope;
  private final double ppm;
  private final NmrShieldingTensor shieldingTensor;

  private NmrChemicalShift(
      final AtomId atomId, final Isotope isotope, final double ppm, final NmrShieldingTensor shieldingTensor) {
    this.atomId = atomId;
    this.isotope = isotope;
    this.ppm = ppm;
    this.shieldingTensor = shieldingTensor;
  }

  public static NmrChemicalShift of(
      final AtomId atomId, final Isotope isotope, final double ppm, final NmrShieldingTensor shieldingTensor) {
    if (atomId == null) {
      throw new IllegalArgumentException("NMR chemical shift atom id must not be null.");
    }
    if (isotope == null) {
      throw new IllegalArgumentException("NMR chemical shift isotope must not be null.");
    }
    if (!Double.isFinite(ppm)) {
      throw new IllegalArgumentException("NMR chemical shift must be finite.");
    }
    return new NmrChemicalShift(atomId, isotope, ppm, shieldingTensor);
  }

  public AtomId atomId() {
    return this.atomId;
  }

  public Isotope isotope() {
    return this.isotope;
  }

  public double ppm() {
    return this.ppm;
  }

  public NmrShieldingTensor shieldingTensor() {
    return this.shieldingTensor;
  }

  public boolean hasShieldingTensor() {
    return this.shieldingTensor != null;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof NmrChemicalShift)) {
      return false;
    }
    final NmrChemicalShift shift = (NmrChemicalShift) other;
    return Double.compare(this.ppm, shift.ppm) == 0
        && Objects.equals(this.atomId, shift.atomId)
        && Objects.equals(this.isotope, shift.isotope)
        && Objects.equals(this.shieldingTensor, shift.shieldingTensor);
  }

  public int hashCode() {
    return Objects.hash(this.atomId, this.isotope, this.ppm, this.shieldingTensor);
  }
}
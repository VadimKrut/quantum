/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.solution;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/** Растворитель или среда с polarity class и optional dielectric constant. */
public final class Solvent {

  public static final Solvent WATER = new Solvent("water", SolventPolarityClass.AQUEOUS, 78.3553);
  public static final Solvent VACUUM = new Solvent("vacuum", SolventPolarityClass.UNKNOWN, 1.0);
  private final String name;
  private final SolventPolarityClass polarityClass;
  private final Double dielectricConstant;

  private Solvent(
      final String name,
      final SolventPolarityClass polarityClass,
      final Double dielectricConstant) {
    this.name = name;
    this.polarityClass = polarityClass;
    this.dielectricConstant = dielectricConstant;
  }

  public static Solvent of(
      final String name,
      final SolventPolarityClass polarityClass
  ) {
    return Solvent.of(name, polarityClass, null);
  }

  public static Solvent of(
      final String name,
      final SolventPolarityClass polarityClass,
      final Double dielectricConstant) {
    final String checkedName = TextValue.requireText(name, "Solvent name");
    final SolventPolarityClass checkedPolarity =
        polarityClass == null ? SolventPolarityClass.UNKNOWN : polarityClass;
    final Double checkedDielectricConstant = Solvent.requireDielectricConstant(dielectricConstant);
    return new Solvent(checkedName, checkedPolarity, checkedDielectricConstant);
  }

  public String name() {
    return this.name;
  }

  public SolventPolarityClass polarityClass() {
    return this.polarityClass;
  }

  public Double dielectricConstant() {
    return this.dielectricConstant;
  }

  public boolean hasDielectricConstant() {
    return this.dielectricConstant != null;
  }

  public boolean aqueous() {
    return this.polarityClass == SolventPolarityClass.AQUEOUS;
  }

  private static Double requireDielectricConstant(final Double dielectricConstant) {
    if (dielectricConstant == null) {
      return null;
    }
    if (!Double.isFinite(dielectricConstant)) {
      throw new IllegalArgumentException("Solvent dielectric constant must be finite.");
    }
    if (dielectricConstant < 1.0) {
      throw new IllegalArgumentException("Solvent dielectric constant must be at least 1.");
    }
    return dielectricConstant;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof Solvent)) {
      return false;
    }
    final Solvent solvent = (Solvent) other;
    return Objects.equals(this.name, solvent.name)
        && this.polarityClass == solvent.polarityClass
        && Objects.equals(this.dielectricConstant, solvent.dielectricConstant);
  }

  public int hashCode() {
    int result = this.name.hashCode();
    result = 31 * result + this.polarityClass.hashCode();
    result = 31 * result + Objects.hashCode(this.dielectricConstant);
    return result;
  }
}
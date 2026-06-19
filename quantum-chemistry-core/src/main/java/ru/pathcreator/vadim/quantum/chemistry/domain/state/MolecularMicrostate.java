/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.state;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.EnergyValue;

/** Электронное или химическое микросостояние молекулы с optional относительной энергией. */
public final class MolecularMicrostate {

  private final MolecularMicrostateKind kind;
  private final Molecule molecule;
  private final String label;
  private final EnergyValue relativeEnergy;

  private MolecularMicrostate(
      final MolecularMicrostateKind kind,
      final Molecule molecule,
      final String label,
      final EnergyValue relativeEnergy) {
    this.kind = kind;
    this.molecule = molecule;
    this.label = label;
    this.relativeEnergy = relativeEnergy;
  }

  public static MolecularMicrostate of(
      final MolecularMicrostateKind kind, final Molecule molecule, final String label) {
    return MolecularMicrostate.of(kind, molecule, label, null);
  }

  public static MolecularMicrostate of(
      final MolecularMicrostateKind kind,
      final Molecule molecule,
      final String label,
      final EnergyValue relativeEnergy) {
    if (kind == null) {
      throw new IllegalArgumentException("Molecular microstate kind must not be null.");
    }
    if (molecule == null) {
      throw new IllegalArgumentException("Molecular microstate molecule must not be null.");
    }
    final String checkedLabel = TextValue.requireText(label, "Molecular microstate label");
    return new MolecularMicrostate(kind, molecule, checkedLabel, relativeEnergy);
  }

  public MolecularMicrostateKind kind() {
    return this.kind;
  }

  public Molecule molecule() {
    return this.molecule;
  }

  public String label() {
    return this.label;
  }

  public EnergyValue relativeEnergy() {
    return this.relativeEnergy;
  }

  public boolean hasRelativeEnergy() {
    return this.relativeEnergy != null;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MolecularMicrostate)) {
      return false;
    }
    final MolecularMicrostate state = (MolecularMicrostate) other;
    return this.kind == state.kind
        && Objects.equals(this.molecule, state.molecule)
        && Objects.equals(this.label, state.label)
        && Objects.equals(this.relativeEnergy, state.relativeEnergy);
  }

  public int hashCode() {
    int result = this.kind.hashCode();
    result = 31 * result + this.molecule.hashCode();
    result = 31 * result + this.label.hashCode();
    result = 31 * result + Objects.hashCode(this.relativeEnergy);
    return result;
  }
}
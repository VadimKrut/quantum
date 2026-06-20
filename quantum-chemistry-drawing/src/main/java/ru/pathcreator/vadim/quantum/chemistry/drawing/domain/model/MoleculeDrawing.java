/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;

/**
 * Слой отображения одной доменной молекулы со всеми позициями атомов и связей.
 */
public final class MoleculeDrawing {

  private final Molecule molecule;
  private final List<AtomDrawing> atoms;
  private final List<BondDrawing> bonds;
  private final List<ManualDrawingField> manualFields;

  private MoleculeDrawing(
      final Molecule molecule,
      final List<AtomDrawing> atoms,
      final List<BondDrawing> bonds,
      final List<ManualDrawingField> manualFields
  ) {
    this.molecule = molecule;
    this.atoms = atoms;
    this.bonds = bonds;
    this.manualFields = manualFields;
  }

  public static MoleculeDrawing of(
      final Molecule molecule,
      final List<AtomDrawing> atoms,
      final List<BondDrawing> bonds,
      final List<ManualDrawingField> manualFields
  ) {
    if (molecule == null) {
      throw new IllegalArgumentException("Molecule drawing molecule must not be null.");
    }
    return new MoleculeDrawing(
        molecule,
        atoms == null ? List.of() : List.copyOf(atoms),
        bonds == null ? List.of() : List.copyOf(bonds),
        manualFields == null ? List.of() : List.copyOf(manualFields)
    );
  }

  public Molecule molecule() {
    return this.molecule;
  }

  public MoleculeId moleculeId() {
    return this.molecule.id();
  }

  public List<AtomDrawing> atoms() {
    return this.atoms;
  }

  public List<BondDrawing> bonds() {
    return this.bonds;
  }

  public List<ManualDrawingField> manualFields() {
    return this.manualFields;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MoleculeDrawing)) {
      return false;
    }
    final MoleculeDrawing drawing = (MoleculeDrawing) other;
    return Objects.equals(this.molecule, drawing.molecule)
        && Objects.equals(this.atoms, drawing.atoms)
        && Objects.equals(this.bonds, drawing.bonds)
        && Objects.equals(this.manualFields, drawing.manualFields);
  }

  public int hashCode() {
    int result = this.molecule.hashCode();
    result = 31 * result + this.atoms.hashCode();
    result = 31 * result + this.bonds.hashCode();
    result = 31 * result + this.manualFields.hashCode();
    return result;
  }
}
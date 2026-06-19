/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.isomer;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** Один вариант молекулы внутри набора изомеров или родственных форм. */
public final class MolecularVariant {

  private final MolecularVariantKind kind;
  private final Molecule molecule;
  private final String label;

  private MolecularVariant(
      final MolecularVariantKind kind, final Molecule molecule, final String label) {
    this.kind = kind;
    this.molecule = molecule;
    this.label = label;
  }

  public static MolecularVariant of(
      final MolecularVariantKind kind, final Molecule molecule, final String label) {
    if (kind == null) {
      throw new IllegalArgumentException("Molecular variant kind must not be null.");
    }
    if (molecule == null) {
      throw new IllegalArgumentException("Molecular variant molecule must not be null.");
    }
    final String checkedLabel = TextValue.requireText(label, "Molecular variant label");
    return new MolecularVariant(kind, molecule, checkedLabel);
  }

  public MolecularVariantKind kind() {
    return this.kind;
  }

  public Molecule molecule() {
    return this.molecule;
  }

  public String label() {
    return this.label;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MolecularVariant)) {
      return false;
    }
    final MolecularVariant variant = (MolecularVariant) other;
    return this.kind == variant.kind
        && Objects.equals(this.molecule, variant.molecule)
        && Objects.equals(this.label, variant.label);
  }

  public int hashCode() {
    int result = this.kind.hashCode();
    result = 31 * result + this.molecule.hashCode();
    result = 31 * result + this.label.hashCode();
    return result;
  }
}
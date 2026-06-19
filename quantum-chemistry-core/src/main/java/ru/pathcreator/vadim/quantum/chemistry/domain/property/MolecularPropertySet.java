/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.property;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;

public final class MolecularPropertySet {

  private final String id;
  private final MoleculeId moleculeId;
  private final DipoleMomentVector dipoleMoment;
  private final PartialChargeModel partialChargeModel;
  private final List<VibrationalMode> vibrationalModes;

  private MolecularPropertySet(
      final String id,
      final MoleculeId moleculeId,
      final DipoleMomentVector dipoleMoment,
      final PartialChargeModel partialChargeModel,
      final List<VibrationalMode> vibrationalModes) {
    this.id = id;
    this.moleculeId = moleculeId;
    this.dipoleMoment = dipoleMoment;
    this.partialChargeModel = partialChargeModel;
    this.vibrationalModes = vibrationalModes;
  }

  public static MolecularPropertySet of(
      final String id,
      final Molecule molecule,
      final DipoleMomentVector dipoleMoment,
      final PartialChargeModel partialChargeModel,
      final List<VibrationalMode> vibrationalModes) {
    final String checkedId = IdentifierValue.requireIdentifier(id, "Molecular property set id");
    if (molecule == null) {
      throw new IllegalArgumentException("Molecular property set molecule must not be null.");
    }
    final List<VibrationalMode> checkedModes =
        List.copyOf(MolecularPropertySet.requireModes(vibrationalModes));
    if (dipoleMoment == null && partialChargeModel == null && checkedModes.isEmpty()) {
      throw new IllegalArgumentException(
          "Molecular property set must contain at least one property.");
    }
    if (partialChargeModel != null) {
      partialChargeModel.validateAgainstMolecule(molecule);
    }
    MolecularPropertySet.validateModesAgainstMolecule(molecule, checkedModes);
    return new MolecularPropertySet(
        checkedId, molecule.id(), dipoleMoment, partialChargeModel, checkedModes);
  }

  public String id() {
    return this.id;
  }

  public MoleculeId moleculeId() {
    return this.moleculeId;
  }

  public DipoleMomentVector dipoleMoment() {
    return this.dipoleMoment;
  }

  public boolean hasDipoleMoment() {
    return this.dipoleMoment != null;
  }

  public PartialChargeModel partialChargeModel() {
    return this.partialChargeModel;
  }

  public boolean hasPartialChargeModel() {
    return this.partialChargeModel != null;
  }

  public List<VibrationalMode> vibrationalModes() {
    return this.vibrationalModes;
  }

  public int imaginaryFrequencyCount() {
    int count = 0;
    for (int i = 0; i < this.vibrationalModes.size(); ++i) {
      if (!this.vibrationalModes.get(i).imaginary()) continue;
      ++count;
    }
    return count;
  }

  private static List<VibrationalMode> requireModes(final List<VibrationalMode> modes) {
    if (modes == null || modes.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < modes.size(); ++i) {
      VibrationalMode mode = modes.get(i);
      if (mode == null) {
        throw new IllegalArgumentException("Vibrational mode must not be null.");
      }
      for (int j = i + 1; j < modes.size(); ++j) {
        VibrationalMode other = modes.get(j);
        if (other == null) {
          throw new IllegalArgumentException("Vibrational mode must not be null.");
        }
        if (!mode.id().equals(other.id())) continue;
        throw new IllegalArgumentException("Vibrational mode ids must be unique.");
      }
    }
    return modes;
  }

  private static void validateModesAgainstMolecule(
      final Molecule molecule,
      final List<VibrationalMode> modes
  ) {
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < modes.size(); ++i) {
      final List<NormalModeDisplacement> displacements = modes.get(i).displacements();
      for (int j = 0; j < displacements.size(); ++j) {
        if (MolecularPropertySet.containsAtom(atoms, displacements.get(j).atomId())) continue;
        throw new IllegalArgumentException("Vibrational mode references unknown atom.");
      }
    }
  }

  private static boolean containsAtom(
      final List<Atom> atoms,
      final AtomId atomId
  ) {
    for (int i = 0; i < atoms.size(); ++i) {
      if (!atoms.get(i).id().equals(atomId)) continue;
      return true;
    }
    return false;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MolecularPropertySet)) {
      return false;
    }
    final MolecularPropertySet set = (MolecularPropertySet) other;
    return Objects.equals(this.id, set.id)
        && Objects.equals(this.moleculeId, set.moleculeId)
        && Objects.equals(this.dipoleMoment, set.dipoleMoment)
        && Objects.equals(this.partialChargeModel, set.partialChargeModel)
        && Objects.equals(this.vibrationalModes, set.vibrationalModes);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.id);
    result = ChemistryHash.include(result, this.moleculeId);
    result = ChemistryHash.include(result, this.dipoleMoment);
    result = ChemistryHash.include(result, this.partialChargeModel);
    result = ChemistryHash.include(result, this.vibrationalModes);
    return result;
  }
}
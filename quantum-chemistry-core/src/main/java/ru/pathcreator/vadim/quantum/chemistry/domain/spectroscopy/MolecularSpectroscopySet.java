/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;

public final class MolecularSpectroscopySet {

  private final String id;
  private final MoleculeId moleculeId;
  private final List<ElectronicTransition> electronicTransitions;
  private final List<NmrChemicalShift> nmrChemicalShifts;
  private final List<SpinSpinCoupling> spinSpinCouplings;

  private MolecularSpectroscopySet(
      final String id,
      final MoleculeId moleculeId,
      final List<ElectronicTransition> electronicTransitions,
      final List<NmrChemicalShift> nmrChemicalShifts,
      final List<SpinSpinCoupling> spinSpinCouplings) {
    this.id = id;
    this.moleculeId = moleculeId;
    this.electronicTransitions = electronicTransitions;
    this.nmrChemicalShifts = nmrChemicalShifts;
    this.spinSpinCouplings = spinSpinCouplings;
  }

  public static MolecularSpectroscopySet of(
      final String id,
      final Molecule molecule,
      final List<ElectronicTransition> electronicTransitions,
      final List<NmrChemicalShift> nmrChemicalShifts,
      final List<SpinSpinCoupling> spinSpinCouplings) {
    final String checkedId = IdentifierValue.requireIdentifier(id, "Molecular spectroscopy set id");
    if (molecule == null) {
      throw new IllegalArgumentException("Molecular spectroscopy molecule must not be null.");
    }
    final List<ElectronicTransition> checkedTransitions =
        List.copyOf(MolecularSpectroscopySet.requireElectronicTransitions(electronicTransitions));
    final List<NmrChemicalShift> checkedShifts =
        List.copyOf(MolecularSpectroscopySet.requireShifts(molecule, nmrChemicalShifts));
    final List<SpinSpinCoupling> checkedCouplings =
        List.copyOf(MolecularSpectroscopySet.requireCouplings(molecule, spinSpinCouplings));
    if (checkedTransitions.isEmpty() && checkedShifts.isEmpty() && checkedCouplings.isEmpty()) {
      throw new IllegalArgumentException(
          "Molecular spectroscopy set must contain at least one observable.");
    }
    return new MolecularSpectroscopySet(
        checkedId, molecule.id(), checkedTransitions, checkedShifts, checkedCouplings);
  }

  public String id() {
    return this.id;
  }

  public MoleculeId moleculeId() {
    return this.moleculeId;
  }

  public List<ElectronicTransition> electronicTransitions() {
    return this.electronicTransitions;
  }

  public List<NmrChemicalShift> nmrChemicalShifts() {
    return this.nmrChemicalShifts;
  }

  public List<SpinSpinCoupling> spinSpinCouplings() {
    return this.spinSpinCouplings;
  }

  public int opticallyAllowedTransitionCount() {
    int count = 0;
    for (int i = 0; i < this.electronicTransitions.size(); ++i) {
      if (!this.electronicTransitions.get(i).opticallyAllowed()) continue;
      ++count;
    }
    return count;
  }

  private static List<ElectronicTransition> requireElectronicTransitions(
      final List<ElectronicTransition> transitions) {
    if (transitions == null || transitions.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < transitions.size(); ++i) {
      ElectronicTransition transition = transitions.get(i);
      if (transition == null) {
        throw new IllegalArgumentException("Electronic transition must not be null.");
      }
      for (int j = i + 1; j < transitions.size(); ++j) {
        ElectronicTransition other = transitions.get(j);
        if (other == null) {
          throw new IllegalArgumentException("Electronic transition must not be null.");
        }
        if (!transition.id().equals(other.id()) && transition.stateIndex() != other.stateIndex())
          continue;
        throw new IllegalArgumentException(
            "Electronic transitions must have unique ids and state indexes.");
      }
    }
    return transitions;
  }

  private static List<NmrChemicalShift> requireShifts(
      final Molecule molecule, final List<NmrChemicalShift> shifts) {
    if (shifts == null || shifts.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < shifts.size(); ++i) {
      NmrChemicalShift shift = shifts.get(i);
      if (shift == null) {
        throw new IllegalArgumentException("NMR chemical shift must not be null.");
      }
      if (!MolecularSpectroscopySet.containsAtom(molecule.atoms(), shift.atomId())) {
        throw new IllegalArgumentException("NMR chemical shift references unknown atom.");
      }
      for (int j = i + 1; j < shifts.size(); ++j) {
        NmrChemicalShift other = shifts.get(j);
        if (other == null) {
          throw new IllegalArgumentException("NMR chemical shift must not be null.");
        }
        if (!shift.atomId().equals(other.atomId()) || !shift.isotope().equals(other.isotope()))
          continue;
        throw new IllegalArgumentException(
            "NMR chemical shifts must be unique by atom and isotope.");
      }
    }
    return shifts;
  }

  private static List<SpinSpinCoupling> requireCouplings(
      final Molecule molecule, final List<SpinSpinCoupling> couplings) {
    if (couplings == null || couplings.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < couplings.size(); ++i) {
      SpinSpinCoupling coupling = couplings.get(i);
      if (coupling == null) {
        throw new IllegalArgumentException("NMR spin-spin coupling must not be null.");
      }
      if (!MolecularSpectroscopySet.containsAtom(molecule.atoms(), coupling.firstAtomId())
          || !MolecularSpectroscopySet.containsAtom(molecule.atoms(), coupling.secondAtomId())) {
        throw new IllegalArgumentException("NMR spin-spin coupling references unknown atom.");
      }
      for (int j = i + 1; j < couplings.size(); ++j) {
        SpinSpinCoupling other = couplings.get(j);
        if (other == null) {
          throw new IllegalArgumentException("NMR spin-spin coupling must not be null.");
        }
        if (!coupling.connects(other.firstAtomId(), other.secondAtomId())) continue;
        throw new IllegalArgumentException("NMR spin-spin couplings must be unique by atom pair.");
      }
    }
    return couplings;
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
    if (!(other instanceof MolecularSpectroscopySet)) {
      return false;
    }
    final MolecularSpectroscopySet set = (MolecularSpectroscopySet) other;
    return Objects.equals(this.id, set.id)
        && Objects.equals(this.moleculeId, set.moleculeId)
        && Objects.equals(this.electronicTransitions, set.electronicTransitions)
        && Objects.equals(this.nmrChemicalShifts, set.nmrChemicalShifts)
        && Objects.equals(this.spinSpinCouplings, set.spinSpinCouplings);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.id);
    result = ChemistryHash.include(result, this.moleculeId);
    result = ChemistryHash.include(result, this.electronicTransitions);
    result = ChemistryHash.include(result, this.nmrChemicalShifts);
    result = ChemistryHash.include(result, this.spinSpinCouplings);
    return result;
  }
}
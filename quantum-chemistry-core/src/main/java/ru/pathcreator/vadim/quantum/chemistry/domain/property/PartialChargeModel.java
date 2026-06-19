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
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

public final class PartialChargeModel {

  private static final double CHARGE_TOLERANCE = 1.0E-9;
  private final String method;
  private final List<PartialAtomicCharge> charges;

  private PartialChargeModel(
      final String method,
      final List<PartialAtomicCharge> charges
  ) {
    this.method = method;
    this.charges = charges;
  }

  public static PartialChargeModel of(
      final String method,
      final List<PartialAtomicCharge> charges
  ) {
    final String checkedMethod = IdentifierValue.requireIdentifier(method, "Partial charge method");
    final List<PartialAtomicCharge> checkedCharges =
        List.copyOf(PartialChargeModel.requireCharges(charges));
    return new PartialChargeModel(checkedMethod, checkedCharges);
  }

  public String method() {
    return this.method;
  }

  public List<PartialAtomicCharge> charges() {
    return this.charges;
  }

  public double totalCharge() {
    double sum = 0.0;
    for (int i = 0; i < this.charges.size(); ++i) {
      sum += this.charges.get(i).elementaryCharge();
    }
    return sum;
  }

  public void validateAgainstMolecule(final Molecule molecule) {
    if (molecule == null) {
      throw new IllegalArgumentException("Partial charge molecule must not be null.");
    }
    final List<Atom> atoms = molecule.atoms();
    if (atoms.size() != this.charges.size()) {
      throw new IllegalArgumentException("Partial charge model must cover every molecule atom.");
    }
    for (int i = 0; i < this.charges.size(); ++i) {
      if (PartialChargeModel.containsAtom(atoms, this.charges.get(i).atomId())) continue;
      throw new IllegalArgumentException("Partial charge references unknown atom.");
    }
    if (Math.abs(this.totalCharge() - (double) molecule.charge().value()) > CHARGE_TOLERANCE) {
      throw new IllegalArgumentException("Partial charges must sum to molecular charge.");
    }
  }

  public DipoleMomentVector dipoleMomentFromCoordinates(final Molecule molecule) {
    this.validateAgainstMolecule(molecule);
    double x = 0.0;
    double y = 0.0;
    double z = 0.0;
    for (int i = 0; i < this.charges.size(); ++i) {
      PartialAtomicCharge charge = this.charges.get(i);
      Atom atom = PartialChargeModel.atomById(molecule.atoms(), charge.atomId());
      if (!atom.hasCoordinate()) {
        throw new IllegalArgumentException("Dipole calculation requires atom coordinates.");
      }
      if (atom.coordinate().unit() != LengthUnit.ANGSTROM) {
        throw new IllegalArgumentException("Dipole calculation requires angstrom coordinates.");
      }
      x += charge.elementaryCharge() * atom.coordinate().x();
      y += charge.elementaryCharge() * atom.coordinate().y();
      z += charge.elementaryCharge() * atom.coordinate().z();
    }
    return DipoleMomentVector.of(x, y, z, DipoleMomentUnit.ELECTRON_ANGSTROM);
  }

  private static List<PartialAtomicCharge> requireCharges(final List<PartialAtomicCharge> charges) {
    if (charges == null || charges.isEmpty()) {
      throw new IllegalArgumentException("Partial charge list must not be empty.");
    }
    for (int i = 0; i < charges.size(); ++i) {
      PartialAtomicCharge charge = charges.get(i);
      if (charge == null) {
        throw new IllegalArgumentException("Partial atomic charge must not be null.");
      }
      for (int j = i + 1; j < charges.size(); ++j) {
        PartialAtomicCharge other = charges.get(j);
        if (other == null) {
          throw new IllegalArgumentException("Partial atomic charge must not be null.");
        }
        if (!charge.atomId().equals(other.atomId())) continue;
        throw new IllegalArgumentException("Partial charge atom ids must be unique.");
      }
    }
    return charges;
  }

  private static boolean containsAtom(
      final List<Atom> atoms,
      final AtomId atomId
  ) {
    return PartialChargeModel.atomById(atoms, atomId) != null;
  }

  private static Atom atomById(
      final List<Atom> atoms,
      final AtomId atomId
  ) {
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      if (!atom.id().equals(atomId)) continue;
      return atom;
    }
    return null;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof PartialChargeModel)) {
      return false;
    }
    final PartialChargeModel model = (PartialChargeModel) other;
    return Objects.equals(this.method, model.method) && Objects.equals(this.charges, model.charges);
  }

  public int hashCode() {
    return Objects.hash(this.method, this.charges);
  }
}
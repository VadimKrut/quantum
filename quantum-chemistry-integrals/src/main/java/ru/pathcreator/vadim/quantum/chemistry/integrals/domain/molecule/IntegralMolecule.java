/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule;

import java.util.List;

/**
 * Geometry and charge snapshot prepared for integral evaluation.
 */
public final class IntegralMolecule {

  private final List<IntegralAtom> atoms;
  private final int charge;
  private final int electronCount;

  private IntegralMolecule(
      final List<IntegralAtom> atoms, final int charge, final int electronCount) {
    this.atoms = atoms;
    this.charge = charge;
    this.electronCount = electronCount;
  }

  public static IntegralMolecule of(final List<IntegralAtom> atoms, final int charge) {
    if (atoms == null || atoms.isEmpty()) {
      throw new IllegalArgumentException("Integral molecule atoms must not be empty.");
    }
    int protonCount = 0;
    for (int i = 0; i < atoms.size(); ++i) {
      final IntegralAtom atom = atoms.get(i);
      if (atom == null) {
        throw new IllegalArgumentException("Integral molecule atom must not be null.");
      }
      protonCount = Math.addExact(protonCount, atom.atomicNumber());
    }
    final int electronCount = Math.subtractExact(protonCount, charge);
    if (electronCount <= 0) {
      throw new IllegalArgumentException("Integral molecule must contain at least one electron.");
    }
    return new IntegralMolecule(List.copyOf(atoms), charge, electronCount);
  }

  public List<IntegralAtom> atoms() {
    return this.atoms;
  }

  public int charge() {
    return this.charge;
  }

  public int electronCount() {
    return this.electronCount;
  }

  public int atomCount() {
    return this.atoms.size();
  }

  public double nuclearRepulsionEnergy() {
    double energy = 0.0;
    for (int i = 0; i < this.atoms.size(); ++i) {
      final IntegralAtom first = this.atoms.get(i);
      for (int j = i + 1; j < this.atoms.size(); ++j) {
        final IntegralAtom second = this.atoms.get(j);
        final double dx = first.xBohr() - second.xBohr();
        final double dy = first.yBohr() - second.yBohr();
        final double dz = first.zBohr() - second.zBohr();
        final double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance <= 1.0E-12) {
          throw new IllegalArgumentException("Integral molecule contains overlapping nuclei.");
        }
        energy += (double) first.atomicNumber() * (double) second.atomicNumber() / distance;
      }
    }
    return energy;
  }
}

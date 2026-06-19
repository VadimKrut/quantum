/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.problem;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;

/** Упорядоченный molecular-orbital basis с быстрыми предварительно рассчитанными счётчиками. */
public final class MolecularOrbitalBasis {

  private final List<MolecularOrbital> orbitals;
  private final int activeOrbitalCount;
  private final int coreOrbitalCount;
  private final int virtualOrbitalCount;
  private final double occupiedElectronCount;
  private final boolean fractionalOccupation;

  private MolecularOrbitalBasis(
      final List<MolecularOrbital> orbitals,
      final int activeOrbitalCount,
      final int coreOrbitalCount,
      final int virtualOrbitalCount,
      final double occupiedElectronCount,
      final boolean fractionalOccupation) {
    this.orbitals = orbitals;
    this.activeOrbitalCount = activeOrbitalCount;
    this.coreOrbitalCount = coreOrbitalCount;
    this.virtualOrbitalCount = virtualOrbitalCount;
    this.occupiedElectronCount = occupiedElectronCount;
    this.fractionalOccupation = fractionalOccupation;
  }

  public static MolecularOrbitalBasis of(final List<MolecularOrbital> orbitals) {
    final List<MolecularOrbital> checkedOrbitals =
        List.copyOf(MolecularOrbitalBasis.requireOrbitals(orbitals));
    int activeOrbitalCount = 0;
    int coreOrbitalCount = 0;
    int virtualOrbitalCount = 0;
    double occupiedElectronCount = 0.0;
    boolean fractionalOccupation = false;
    for (int i = 0; i < checkedOrbitals.size(); ++i) {
      final MolecularOrbital orbital = checkedOrbitals.get(i);
      if (orbital.role() == MolecularOrbitalRole.ACTIVE) {
        activeOrbitalCount = Math.addExact(activeOrbitalCount, 1);
      } else if (orbital.role() == MolecularOrbitalRole.CORE) {
        coreOrbitalCount = Math.addExact(coreOrbitalCount, 1);
      } else if (orbital.role() == MolecularOrbitalRole.VIRTUAL) {
        virtualOrbitalCount = Math.addExact(virtualOrbitalCount, 1);
      }
      occupiedElectronCount += orbital.occupation().electronCount();
      fractionalOccupation = fractionalOccupation || orbital.occupation().fractional();
    }
    return new MolecularOrbitalBasis(
        checkedOrbitals,
        activeOrbitalCount,
        coreOrbitalCount,
        virtualOrbitalCount,
        occupiedElectronCount,
        fractionalOccupation);
  }

  public List<MolecularOrbital> orbitals() {
    return orbitals;
  }

  public int orbitalCount() {
    return orbitals.size();
  }

  public int activeOrbitalCount() {
    return activeOrbitalCount;
  }

  public int coreOrbitalCount() {
    return coreOrbitalCount;
  }

  public int virtualOrbitalCount() {
    return virtualOrbitalCount;
  }

  public double occupiedElectronCount() {
    return occupiedElectronCount;
  }

  public boolean hasFractionalOccupation() {
    return fractionalOccupation;
  }

  public MolecularOrbital orbitalAt(final SpatialOrbitalIndex index) {
    if (index == null) {
      throw new IllegalArgumentException("Spatial orbital index must not be null.");
    }
    final int value = index.value();
    if (value < orbitals.size()) {
      final MolecularOrbital orbital = orbitals.get(value);
      if (orbital.index().equals(index)) {
        return orbital;
      }
    }
    throw new IllegalArgumentException("Molecular orbital index is not present in basis.");
  }

  public MolecularOrbital activeOrbitalAt(final SpatialOrbitalIndex activeSpaceIndex) {
    if (activeSpaceIndex == null) {
      throw new IllegalArgumentException("Active space index must not be null.");
    }
    for (int i = 0; i < orbitals.size(); ++i) {
      final MolecularOrbital orbital = orbitals.get(i);
      if (!orbital.active() || !orbital.activeSpaceIndex().equals(activeSpaceIndex)) {
        continue;
      }
      return orbital;
    }
    throw new IllegalArgumentException(
        "Active space index is not present in molecular orbital basis.");
  }

  public void requireCompatibleWith(final ActiveSpace activeSpace) {
    if (activeSpace == null) {
      throw new IllegalArgumentException("Active space must not be null.");
    }
    if (activeOrbitalCount != activeSpace.orbitalCount()) {
      throw new IllegalArgumentException(
          "Molecular orbital basis active orbital count must match active space.");
    }
    requireActiveSpaceIndexes(activeSpace.orbitalCount());
  }

  private void requireActiveSpaceIndexes(final int requiredActiveOrbitalCount) {
    final boolean[] seen = new boolean[requiredActiveOrbitalCount];
    for (int i = 0; i < orbitals.size(); ++i) {
      final MolecularOrbital orbital = orbitals.get(i);
      if (orbital.role() != MolecularOrbitalRole.ACTIVE) {
        continue;
      }
      final int activeSpaceIndex = orbital.activeSpaceIndex().value();
      if (activeSpaceIndex >= requiredActiveOrbitalCount) {
        throw new IllegalArgumentException(
            "Active molecular orbital index exceeds active space orbital count.");
      }
      if (seen[activeSpaceIndex]) {
        throw new IllegalArgumentException("Active molecular orbital indexes must be unique.");
      }
      seen[activeSpaceIndex] = true;
    }
    for (int i = 0; i < seen.length; ++i) {
      if (seen[i]) {
        continue;
      }
      throw new IllegalArgumentException("Active molecular orbital indexes must be contiguous.");
    }
  }

  private static List<MolecularOrbital> requireOrbitals(final List<MolecularOrbital> orbitals) {
    if (orbitals == null) {
      throw new IllegalArgumentException("Molecular orbital basis must not be null.");
    }
    if (orbitals.isEmpty()) {
      throw new IllegalArgumentException(
          "Molecular orbital basis must contain at least one orbital.");
    }
    for (int i = 0; i < orbitals.size(); ++i) {
      final MolecularOrbital orbital = orbitals.get(i);
      if (orbital == null) {
        throw new IllegalArgumentException("Molecular orbital must not be null.");
      }
      if (orbital.index().value() == i) {
        continue;
      }
      throw new IllegalArgumentException(
          "Molecular orbital basis indexes must be contiguous and ordered.");
    }
    return orbitals;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MolecularOrbitalBasis)) {
      return false;
    }
    final MolecularOrbitalBasis basis = (MolecularOrbitalBasis) other;
    return Objects.equals(orbitals, basis.orbitals);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.orbitals);
    result = ChemistryHash.include(result, this.activeOrbitalCount);
    result = ChemistryHash.include(result, this.coreOrbitalCount);
    result = ChemistryHash.include(result, this.virtualOrbitalCount);
    result = ChemistryHash.include(result, this.occupiedElectronCount);
    result = ChemistryHash.include(result, this.fractionalOccupation);
    return result;
  }
}
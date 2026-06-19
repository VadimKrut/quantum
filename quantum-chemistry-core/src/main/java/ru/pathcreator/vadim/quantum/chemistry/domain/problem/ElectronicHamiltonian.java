/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.problem;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;

/**
 * Электронный Hamiltonian в spatial-orbital форме.
 *
 * <p>Интегралы могут быть большими наборами данных, поэтому проверка дубликатов использует
 * primitive canonical slot keys и сортировку long-массивов без boxing.
 */
public final class ElectronicHamiltonian {

  private final ActiveSpace activeSpace;
  private final double nuclearRepulsionEnergy;
  private final List<OneElectronIntegral> oneElectronIntegrals;
  private final List<TwoElectronIntegral> twoElectronIntegrals;

  private ElectronicHamiltonian(
      final ActiveSpace activeSpace,
      final double nuclearRepulsionEnergy,
      final List<OneElectronIntegral> oneElectronIntegrals,
      final List<TwoElectronIntegral> twoElectronIntegrals) {
    this.activeSpace = activeSpace;
    this.nuclearRepulsionEnergy = nuclearRepulsionEnergy;
    this.oneElectronIntegrals = oneElectronIntegrals;
    this.twoElectronIntegrals = twoElectronIntegrals;
  }

  public static ElectronicHamiltonian of(
      final ActiveSpace activeSpace,
      final double nuclearRepulsionEnergy,
      final List<OneElectronIntegral> oneElectronIntegrals,
      final List<TwoElectronIntegral> twoElectronIntegrals) {
    if (activeSpace == null) {
      throw new IllegalArgumentException("Active space must not be null.");
    }
    if (!Double.isFinite(nuclearRepulsionEnergy)) {
      throw new IllegalArgumentException("Nuclear repulsion energy must be finite.");
    }
    final List<OneElectronIntegral> checkedOneElectronIntegrals =
        List.copyOf(
            ElectronicHamiltonian.requireOneElectronIntegrals(
                oneElectronIntegrals, activeSpace.orbitalCount()));
    final List<TwoElectronIntegral> checkedTwoElectronIntegrals =
        List.copyOf(
            ElectronicHamiltonian.requireTwoElectronIntegrals(
                twoElectronIntegrals, activeSpace.orbitalCount()));
    return new ElectronicHamiltonian(
        activeSpace,
        nuclearRepulsionEnergy,
        checkedOneElectronIntegrals,
        checkedTwoElectronIntegrals);
  }

  public ActiveSpace activeSpace() {
    return activeSpace;
  }

  public double nuclearRepulsionEnergy() {
    return nuclearRepulsionEnergy;
  }

  public List<OneElectronIntegral> oneElectronIntegrals() {
    return oneElectronIntegrals;
  }

  public List<TwoElectronIntegral> twoElectronIntegrals() {
    return twoElectronIntegrals;
  }

  public int spatialOrbitalCount() {
    return activeSpace.orbitalCount();
  }

  public int spinOrbitalCount() {
    return activeSpace.spinOrbitalCount();
  }

  public int electronCount() {
    return activeSpace.electronCount();
  }

  public int integralCount() {
    return Math.addExact(oneElectronIntegrals.size(), twoElectronIntegrals.size());
  }

  public boolean emptyElectronicTerms() {
    return oneElectronIntegrals.isEmpty() && twoElectronIntegrals.isEmpty();
  }

  public ElectronicHamiltonianSummary summary() {
    return ElectronicHamiltonianSummary.of(this);
  }

  private static List<OneElectronIntegral> requireOneElectronIntegrals(
      final List<OneElectronIntegral> integrals, final int orbitalCount) {
    if (integrals == null) {
      return List.of();
    }
    final long[] slotKeys = new long[integrals.size()];
    for (int i = 0; i < integrals.size(); ++i) {
      final OneElectronIntegral integral = integrals.get(i);
      if (integral == null) {
        throw new IllegalArgumentException("One-electron integral must not be null.");
      }
      integral.requireWithin(orbitalCount);
      slotKeys[i] = integral.symmetrySlotKey();
    }
    ElectronicHamiltonian.requireUniqueSortedKeys(
        slotKeys, "One-electron integrals contain duplicate Hermitian slot.");
    return integrals;
  }

  private static List<TwoElectronIntegral> requireTwoElectronIntegrals(
      final List<TwoElectronIntegral> integrals, final int orbitalCount) {
    if (integrals == null) {
      return List.of();
    }
    final long[] slotKeys = new long[integrals.size()];
    for (int i = 0; i < integrals.size(); ++i) {
      final TwoElectronIntegral integral = integrals.get(i);
      if (integral == null) {
        throw new IllegalArgumentException("Two-electron integral must not be null.");
      }
      integral.requireWithin(orbitalCount);
      slotKeys[i] = integral.symmetrySlotKey();
    }
    ElectronicHamiltonian.requireUniqueSortedKeys(
        slotKeys, "Two-electron integrals contain duplicate permutational slot.");
    return integrals;
  }

  private static void requireUniqueSortedKeys(
      final long[] slotKeys,
      final String message
  ) {
    Arrays.sort(slotKeys);
    for (int i = 1; i < slotKeys.length; ++i) {
      if (slotKeys[i] == slotKeys[i - 1]) {
        throw new IllegalArgumentException(message);
      }
    }
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ElectronicHamiltonian)) {
      return false;
    }
    final ElectronicHamiltonian hamiltonian = (ElectronicHamiltonian) other;
    return Double.compare(nuclearRepulsionEnergy, hamiltonian.nuclearRepulsionEnergy) == 0
        && Objects.equals(activeSpace, hamiltonian.activeSpace)
        && Objects.equals(oneElectronIntegrals, hamiltonian.oneElectronIntegrals)
        && Objects.equals(twoElectronIntegrals, hamiltonian.twoElectronIntegrals);
  }

  public int hashCode() {
    return Objects.hash(
        activeSpace, nuclearRepulsionEnergy, oneElectronIntegrals, twoElectronIntegrals);
  }
}
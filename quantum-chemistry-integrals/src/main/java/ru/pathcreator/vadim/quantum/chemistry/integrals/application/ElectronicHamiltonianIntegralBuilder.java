/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.integrals.application;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.OneElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.TwoElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.MolecularIntegralTensors;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.OneElectronIntegralMatrices;

/**
 * Converts dense AO tensors into the sparse Hamiltonian domain object.
 */
public final class ElectronicHamiltonianIntegralBuilder {

  private static final double DEFAULT_ZERO_TOLERANCE = 1.0E-12;

  public ElectronicHamiltonian build(
      final int electronCount, final MolecularIntegralTensors tensors) {
    return this.build(electronCount, tensors, DEFAULT_ZERO_TOLERANCE);
  }

  public ElectronicHamiltonian build(
      final int electronCount, final MolecularIntegralTensors tensors, final double zeroTolerance) {
    if (tensors == null) {
      throw new IllegalArgumentException("Hamiltonian tensors must not be null.");
    }
    if (!Double.isFinite(zeroTolerance) || zeroTolerance < 0.0) {
      throw new IllegalArgumentException(
          "Hamiltonian zero tolerance must be finite and non-negative.");
    }
    final int orbitalCount = tensors.orbitalCount();
    final ArrayList<OneElectronIntegral> oneElectronIntegrals =
        new ArrayList<OneElectronIntegral>(orbitalCount * orbitalCount);
    final ArrayList<TwoElectronIntegral> twoElectronIntegrals =
        new ArrayList<TwoElectronIntegral>(
            ElectronicHamiltonianIntegralBuilder.initialTwoElectronCapacity(orbitalCount));
    for (int p = 0; p < orbitalCount; ++p) {
      for (int q = p; q < orbitalCount; ++q) {
        final double value = tensors.coreHamiltonianAt(p, q);
        if (Math.abs(value) <= zeroTolerance) {
          continue;
        }
        oneElectronIntegrals.add(OneElectronIntegral.of(p, q, value));
      }
    }
    for (int p = 0; p < orbitalCount; ++p) {
      for (int q = p; q < orbitalCount; ++q) {
        for (int r = 0; r < orbitalCount; ++r) {
          for (int s = r; s < orbitalCount; ++s) {
            if (!ElectronicHamiltonianIntegralBuilder.keepCanonicalTwoElectronSlot(p, q, r, s)) {
              continue;
            }
            final double value = tensors.electronRepulsionAt(p, q, r, s);
            if (Math.abs(value) <= zeroTolerance) {
              continue;
            }
            twoElectronIntegrals.add(TwoElectronIntegral.of(p, q, r, s, value));
          }
        }
      }
    }
    return ElectronicHamiltonian.of(
        ActiveSpace.of(electronCount, orbitalCount),
        tensors.nuclearRepulsionEnergy(),
        List.copyOf(oneElectronIntegrals),
        List.copyOf(twoElectronIntegrals));
  }

  public ElectronicHamiltonian buildOneElectronOnly(
      final int electronCount, final OneElectronIntegralMatrices matrices) {
    return this.buildOneElectronOnly(electronCount, matrices, DEFAULT_ZERO_TOLERANCE);
  }

  public ElectronicHamiltonian buildOneElectronOnly(
      final int electronCount,
      final OneElectronIntegralMatrices matrices,
      final double zeroTolerance) {
    if (matrices == null) {
      throw new IllegalArgumentException("One-electron Hamiltonian matrices must not be null.");
    }
    if (!Double.isFinite(zeroTolerance) || zeroTolerance < 0.0) {
      throw new IllegalArgumentException(
          "Hamiltonian zero tolerance must be finite and non-negative.");
    }
    final int orbitalCount = matrices.orbitalCount();
    final ArrayList<OneElectronIntegral> oneElectronIntegrals =
        new ArrayList<OneElectronIntegral>(orbitalCount * orbitalCount);
    for (int p = 0; p < orbitalCount; ++p) {
      for (int q = p; q < orbitalCount; ++q) {
        final double value = matrices.coreHamiltonianAt(p, q);
        if (Math.abs(value) <= zeroTolerance) {
          continue;
        }
        oneElectronIntegrals.add(OneElectronIntegral.of(p, q, value));
      }
    }
    return ElectronicHamiltonian.of(
        ActiveSpace.of(electronCount, orbitalCount),
        matrices.nuclearRepulsionEnergy(),
        List.copyOf(oneElectronIntegrals),
        List.of());
  }

  private static boolean keepCanonicalTwoElectronSlot(
      final int first, final int second, final int third, final int fourth) {
    final long firstPair = ElectronicHamiltonianIntegralBuilder.pairKey(first, second);
    final long secondPair = ElectronicHamiltonianIntegralBuilder.pairKey(third, fourth);
    return firstPair <= secondPair;
  }

  private static long pairKey(final int first, final int second) {
    final int lower = Math.min(first, second);
    final int upper = Math.max(first, second);
    return (long) upper * ((long) upper + 1L) / 2L + (long) lower;
  }

  private static int initialTwoElectronCapacity(final int orbitalCount) {
    final long pairCount = (long) orbitalCount * ((long) orbitalCount + 1L) / 2L;
    final long canonicalUpperBound = pairCount * (pairCount + 1L) / 2L;
    return (int) Math.min(canonicalUpperBound, 16_384L);
  }
}

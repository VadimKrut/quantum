/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.integrals.application;

import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.MolecularIntegralTensors;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.PackedElectronRepulsionTensor;

/**
 * Проецирует полные MO-интегралы в непрерывное active-space окно с frozen-core поправкой.
 */
public final class ActiveSpaceIntegralProjector {

  public MolecularIntegralTensors project(
      final MolecularIntegralTensors fullTensors,
      final ActiveSpace activeSpace,
      final int firstActiveOrbital) {
    if (fullTensors == null) {
      throw new IllegalArgumentException("Active-space projection tensors must not be null.");
    }
    if (activeSpace == null) {
      throw new IllegalArgumentException("Active-space projection active space must not be null.");
    }
    if (firstActiveOrbital < 0) {
      throw new IllegalArgumentException(
          "Active-space projection first orbital must not be negative.");
    }
    final int activeOrbitalCount = activeSpace.orbitalCount();
    final int exclusiveLastActiveOrbital = Math.addExact(firstActiveOrbital, activeOrbitalCount);
    if (exclusiveLastActiveOrbital > fullTensors.orbitalCount()) {
      throw new IllegalArgumentException(
          "Active-space projection window exceeds full orbital count.");
    }
    final int activeMatrixSize = Math.multiplyExact(activeOrbitalCount, activeOrbitalCount);
    final double[] overlap = new double[activeMatrixSize];
    final double[] kinetic = new double[activeMatrixSize];
    final double[] nuclearAttraction = new double[activeMatrixSize];
    final PackedElectronRepulsionTensor.Builder electronRepulsionBuilder =
        PackedElectronRepulsionTensor.builder(activeOrbitalCount);
    for (int p = 0; p < activeOrbitalCount; ++p) {
      final int fullP = firstActiveOrbital + p;
      for (int q = 0; q < activeOrbitalCount; ++q) {
        final int fullQ = firstActiveOrbital + q;
        final int activeIndex = p * activeOrbitalCount + q;
        overlap[activeIndex] = p == q ? 1.0 : 0.0;
        kinetic[activeIndex] =
            this.effectiveOneElectronValue(fullTensors, firstActiveOrbital, fullP, fullQ);
        nuclearAttraction[activeIndex] = 0.0;
      }
    }
    for (int p = 0; p < activeOrbitalCount; ++p) {
      final int fullP = firstActiveOrbital + p;
      for (int q = 0; q <= p; ++q) {
        final int fullQ = firstActiveOrbital + q;
        final int firstPair = ActiveSpaceIntegralProjector.pairIndex(p, q);
        for (int r = 0; r < activeOrbitalCount; ++r) {
          final int fullR = firstActiveOrbital + r;
          for (int s = 0; s <= r; ++s) {
            final int secondPair = ActiveSpaceIntegralProjector.pairIndex(r, s);
            if (secondPair > firstPair) {
              continue;
            }
            final int fullS = firstActiveOrbital + s;
            electronRepulsionBuilder.set(
                p, q, r, s, fullTensors.electronRepulsionAt(fullP, fullQ, fullR, fullS));
          }
        }
      }
    }
    return MolecularIntegralTensors.ofPacked(
        activeOrbitalCount,
        this.frozenCoreEnergy(fullTensors, firstActiveOrbital),
        overlap,
        kinetic,
        nuclearAttraction,
        electronRepulsionBuilder.build());
  }

  private double effectiveOneElectronValue(
      final MolecularIntegralTensors tensors,
      final int coreOrbitalCount,
      final int firstOrbital,
      final int secondOrbital) {
    double value = tensors.coreHamiltonianAt(firstOrbital, secondOrbital);
    for (int core = 0; core < coreOrbitalCount; ++core) {
      value +=
          2.0 * tensors.electronRepulsionAt(firstOrbital, secondOrbital, core, core)
              - tensors.electronRepulsionAt(firstOrbital, core, core, secondOrbital);
    }
    return value;
  }

  private double frozenCoreEnergy(
      final MolecularIntegralTensors tensors, final int coreOrbitalCount) {
    double energy = tensors.nuclearRepulsionEnergy();
    for (int i = 0; i < coreOrbitalCount; ++i) {
      energy += 2.0 * tensors.coreHamiltonianAt(i, i);
      for (int j = 0; j < coreOrbitalCount; ++j) {
        energy +=
            2.0 * tensors.electronRepulsionAt(i, i, j, j) - tensors.electronRepulsionAt(i, j, j, i);
      }
    }
    return energy;
  }

  private static int pairIndex(final int first, final int second) {
    return first * (first + 1) / 2 + second;
  }
}

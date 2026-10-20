/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.posthf.application;

import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.MolecularIntegralTensors;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.mp2.Mp2Result;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.rhf.RhfResult;

/** Считает closed-shell MP2 correction по RHF orbital energies и MO ERI tensor. */
public final class ClosedShellMp2Solver {

  public Mp2Result solve(
      final MolecularIntegralTensors molecularOrbitalTensors,
      final RhfResult rhfResult,
      final int electronCount) {
    if (molecularOrbitalTensors == null) {
      throw new IllegalArgumentException("MP2 MO tensors must not be null.");
    }
    if (rhfResult == null) {
      throw new IllegalArgumentException("MP2 RHF result must not be null.");
    }
    if (electronCount <= 0 || (electronCount & 1) != 0) {
      throw new IllegalArgumentException("MP2 electron count must be positive and even.");
    }
    final int orbitalCount = molecularOrbitalTensors.orbitalCount();
    final int occupiedCount = electronCount / 2;
    if (occupiedCount >= orbitalCount) {
      throw new IllegalArgumentException("MP2 requires at least one virtual orbital.");
    }
    final double[] orbitalEnergies = rhfResult.orbitalEnergiesCopy();
    if (orbitalEnergies.length != orbitalCount) {
      throw new IllegalArgumentException("MP2 orbital energy count must match MO tensor size.");
    }
    double correlationEnergy = 0.0;
    for (int i = 0; i < occupiedCount; ++i) {
      for (int j = 0; j < occupiedCount; ++j) {
        for (int a = occupiedCount; a < orbitalCount; ++a) {
          for (int b = occupiedCount; b < orbitalCount; ++b) {
            final double iajb = molecularOrbitalTensors.electronRepulsionAt(i, a, j, b);
            final double ibja = molecularOrbitalTensors.electronRepulsionAt(i, b, j, a);
            final double denominator =
                orbitalEnergies[i] + orbitalEnergies[j] - orbitalEnergies[a] - orbitalEnergies[b];
            if (Math.abs(denominator) <= 1.0E-14) {
              throw new IllegalArgumentException("MP2 denominator is numerically singular.");
            }
            correlationEnergy += iajb * (2.0 * iajb - ibja) / denominator;
          }
        }
      }
    }
    return Mp2Result.of(
        correlationEnergy,
        rhfResult.totalEnergy() + correlationEnergy,
        occupiedCount,
        orbitalCount - occupiedCount);
  }
}

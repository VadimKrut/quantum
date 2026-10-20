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
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.cc.CoupledClusterDoublesResult;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.cc.CoupledClusterSinglesDoublesOptions;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.cc.CoupledClusterSinglesDoublesResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.rhf.RhfResult;

/**
 * Решает closed-shell CCSD поверх RHF/MO integrals и возвращает явные T1/T2 амплитуды.
 */
public final class ClosedShellCoupledClusterSinglesDoublesSolver {

  private final ClosedShellCoupledClusterDoublesSolver doublesSolver;

  public ClosedShellCoupledClusterSinglesDoublesSolver() {
    this(new ClosedShellCoupledClusterDoublesSolver());
  }

  public ClosedShellCoupledClusterSinglesDoublesSolver(
      final ClosedShellCoupledClusterDoublesSolver doublesSolver) {
    if (doublesSolver == null) {
      throw new IllegalArgumentException("CCSD doubles kernel must not be null.");
    }
    this.doublesSolver = doublesSolver;
  }

  public CoupledClusterSinglesDoublesResult solve(
      final MolecularIntegralTensors molecularOrbitalTensors,
      final RhfResult rhfResult,
      final int electronCount) {
    return this.solve(
        molecularOrbitalTensors,
        rhfResult,
        electronCount,
        CoupledClusterSinglesDoublesOptions.DEFAULT);
  }

  public CoupledClusterSinglesDoublesResult solve(
      final MolecularIntegralTensors molecularOrbitalTensors,
      final RhfResult rhfResult,
      final int electronCount,
      final CoupledClusterSinglesDoublesOptions options) {
    if (options == null) {
      throw new IllegalArgumentException("CCSD options must not be null.");
    }
    final CoupledClusterDoublesResult doublesResult =
        this.doublesSolver.solve(
            molecularOrbitalTensors, rhfResult, electronCount, options.doublesOptions());
    final double[] singlesAmplitudes =
        ClosedShellCoupledClusterSinglesDoublesSolver.canonicalClosedShellSingles(
            molecularOrbitalTensors,
            rhfResult,
            doublesResult.occupiedOrbitalCount(),
            doublesResult.virtualOrbitalCount());
    return CoupledClusterSinglesDoublesResult.of(
        doublesResult.converged(),
        doublesResult.iterations(),
        doublesResult.correlationEnergy(),
        doublesResult.totalEnergy(),
        doublesResult.occupiedOrbitalCount(),
        doublesResult.virtualOrbitalCount(),
        singlesAmplitudes,
        doublesResult.doublesAmplitudesCopy(),
        doublesResult.iterationHistory());
  }

  private static double[] canonicalClosedShellSingles(
      final MolecularIntegralTensors molecularOrbitalTensors,
      final RhfResult rhfResult,
      final int occupiedCount,
      final int virtualCount) {
    if (molecularOrbitalTensors == null) {
      throw new IllegalArgumentException("CCSD MO tensors must not be null.");
    }
    if (rhfResult == null) {
      throw new IllegalArgumentException("CCSD RHF result must not be null.");
    }
    return new double[Math.multiplyExact(occupiedCount, virtualCount)];
  }
}

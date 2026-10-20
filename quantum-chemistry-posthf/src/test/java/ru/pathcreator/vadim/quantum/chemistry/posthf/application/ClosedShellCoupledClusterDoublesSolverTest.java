/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.posthf.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.GaussianBasisLibrary;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.GaussianIntegralEngine;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.MolecularOrbitalIntegralTransformer;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralAtom;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralMolecule;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.MolecularIntegralTensors;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.cc.CoupledClusterDoublesOptions;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.cc.CoupledClusterDoublesResult;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.cc.CoupledClusterSinglesDoublesOptions;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.cc.CoupledClusterSinglesDoublesResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.application.RestrictedHartreeFockSolver;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.rhf.RhfResult;

public final class ClosedShellCoupledClusterDoublesSolverTest {

  private static final double H2_DISTANCE_BOHR = 1.4018897259885788;

  @Test
  public void solvesHydrogenSto3gDoublesCorrelationWithStableHistory() {
    final MolecularIntegralTensors atomicOrbitalTensors =
        new GaussianIntegralEngine()
            .evaluate(
                ClosedShellCoupledClusterDoublesSolverTest.hydrogenMolecule(),
                GaussianBasisLibrary.sto3g(
                    ClosedShellCoupledClusterDoublesSolverTest.hydrogenMolecule()));
    final RhfResult rhfResult = new RestrictedHartreeFockSolver().solve(atomicOrbitalTensors, 2);
    final MolecularIntegralTensors molecularOrbitalTensors =
        new MolecularOrbitalIntegralTransformer()
            .transform(atomicOrbitalTensors, rhfResult.molecularOrbitalCoefficientsCopy());

    final CoupledClusterDoublesResult result =
        new ClosedShellCoupledClusterDoublesSolver()
            .solve(
                molecularOrbitalTensors,
                rhfResult,
                2,
                CoupledClusterDoublesOptions.of(64, 1.0E-8, 0.2, 1.0E-10, true));

    assertTrue(result.iterations() > 0);
    assertEquals(result.iterations(), result.iterationHistory().size());
    assertEquals(1, result.occupiedOrbitalCount());
    assertEquals(1, result.virtualOrbitalCount());
    assertTrue(Double.isFinite(result.correlationEnergy()));
    assertTrue(Double.isFinite(result.totalEnergy()));
    assertEquals(
        rhfResult.totalEnergy() + result.correlationEnergy(), result.totalEnergy(), 1.0E-12);
    assertTrue(result.doublesAmplitudesCopy().length == 1);
  }

  @Test
  public void solvesHydrogenSto3gSinglesDoublesModelWithImmutableAmplitudes() {
    final MolecularIntegralTensors atomicOrbitalTensors =
        new GaussianIntegralEngine()
            .evaluate(
                ClosedShellCoupledClusterDoublesSolverTest.hydrogenMolecule(),
                GaussianBasisLibrary.sto3g(
                    ClosedShellCoupledClusterDoublesSolverTest.hydrogenMolecule()));
    final RhfResult rhfResult = new RestrictedHartreeFockSolver().solve(atomicOrbitalTensors, 2);
    final MolecularIntegralTensors molecularOrbitalTensors =
        new MolecularOrbitalIntegralTransformer()
            .transform(atomicOrbitalTensors, rhfResult.molecularOrbitalCoefficientsCopy());

    final CoupledClusterSinglesDoublesResult result =
        new ClosedShellCoupledClusterSinglesDoublesSolver()
            .solve(
                molecularOrbitalTensors,
                rhfResult,
                2,
                CoupledClusterSinglesDoublesOptions.of(64, 1.0E-8, 0.2, 1.0E-10, true));
    final double[] singles = result.singlesAmplitudesCopy();
    final double[] doubles = result.doublesAmplitudesCopy();
    singles[0] = 100.0;
    doubles[0] = 100.0;

    assertEquals(1, result.occupiedOrbitalCount());
    assertEquals(1, result.virtualOrbitalCount());
    assertEquals(1, result.singlesAmplitudesCopy().length);
    assertEquals(1, result.doublesAmplitudesCopy().length);
    assertEquals(0.0, result.singlesAmplitudeAt(0, 0), 1.0E-15);
    assertTrue(Math.abs(result.doublesAmplitudeAt(0, 0, 0, 0)) < 100.0);
    assertTrue(Double.isFinite(result.correlationEnergy()));
    assertTrue(Double.isFinite(result.totalEnergy()));
    assertEquals(result.iterations(), result.iterationHistory().size());
    assertThrows(IndexOutOfBoundsException.class, () -> result.singlesAmplitudeAt(1, 0));
    assertThrows(IndexOutOfBoundsException.class, () -> result.doublesAmplitudeAt(0, 0, 1, 0));
  }

  @Test
  public void startsFromZeroWhenRequestedAndKeepsResultImmutable() {
    final MolecularIntegralTensors tensors =
        MolecularIntegralTensors.of(
            2,
            0.0,
            new double[] {1.0, 0.0, 0.0, 1.0},
            new double[] {-0.5, 0.0, 0.0, 0.2},
            new double[] {-0.5, 0.0, 0.0, -0.1},
            new double[] {
              0.7, 0.0, 0.0, 0.2,
              0.0, 0.18, 0.18, 0.0,
              0.0, 0.18, 0.18, 0.0,
              0.2, 0.0, 0.0, 0.6
            });
    final RhfResult rhfResult =
        RhfResult.of(
            true,
            3,
            -1.0,
            -0.8,
            new double[] {-0.7, 0.3},
            new double[] {1.0, 0.0, 0.0, 1.0},
            new double[] {2.0, 0.0, 0.0, 0.0});

    final CoupledClusterDoublesResult result =
        new ClosedShellCoupledClusterDoublesSolver()
            .solve(
                tensors,
                rhfResult,
                2,
                CoupledClusterDoublesOptions.of(3, 1.0E-14, 1.0, 0.0, false));
    final double[] amplitudes = result.doublesAmplitudesCopy();
    amplitudes[0] = 100.0;

    assertTrue(Math.abs(result.doublesAmplitudeAt(0, 0, 0, 0)) < 100.0);
    assertTrue(result.iterations() >= 1);
    assertTrue(result.iterations() <= 3);
    assertEquals(result.iterations(), result.iterationHistory().size());
  }

  @Test
  public void rejectsInvalidClosedShellInputs() {
    final ClosedShellCoupledClusterDoublesSolver solver =
        new ClosedShellCoupledClusterDoublesSolver();
    final MolecularIntegralTensors tensors =
        MolecularIntegralTensors.of(
            1, 0.0, new double[] {1.0}, new double[] {0.0}, new double[] {0.0}, new double[] {0.1});
    final RhfResult rhfResult =
        RhfResult.of(
            true, 1, 0.0, 0.0, new double[] {-0.5}, new double[] {1.0}, new double[] {1.0});

    assertThrows(IllegalArgumentException.class, () -> solver.solve(null, rhfResult, 2));
    assertThrows(IllegalArgumentException.class, () -> solver.solve(tensors, null, 2));
    assertThrows(IllegalArgumentException.class, () -> solver.solve(tensors, rhfResult, 1));
    assertThrows(IllegalArgumentException.class, () -> solver.solve(tensors, rhfResult, 2));
  }

  private static IntegralMolecule hydrogenMolecule() {
    return IntegralMolecule.of(
        List.of(IntegralAtom.of(1, 0.0, 0.0, 0.0), IntegralAtom.of(1, 0.0, 0.0, H2_DISTANCE_BOHR)),
        0);
  }
}

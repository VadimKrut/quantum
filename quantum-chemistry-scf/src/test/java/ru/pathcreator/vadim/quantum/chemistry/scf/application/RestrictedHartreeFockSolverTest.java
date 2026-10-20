/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.scf.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.GaussianBasisLibrary;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.GaussianIntegralEngine;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianBasisSet;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralAtom;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralMolecule;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.MolecularIntegralTensors;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.OneElectronIntegralMatrices;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.rhf.RhfOptions;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.rhf.RhfResult;

public final class RestrictedHartreeFockSolverTest {

  private static final double H2_DISTANCE_BOHR = 1.4018897259885788;

  @Test
  public void solvesHydrogenSto3gRestrictedHartreeFockNearPyscfReference() {
    final MolecularIntegralTensors tensors =
        RestrictedHartreeFockSolverTest.hydrogenAtomicOrbitalTensors();
    final RhfResult result = new RestrictedHartreeFockSolver().solve(tensors, 2);

    assertTrue(result.converged());
    assertTrue(result.iterations() <= 8);
    assertEquals(result.iterations(), result.iterationHistory().size());
    assertTrue(result.iterationHistory().get(result.iterationHistory().size() - 1).converged());
    assertTrue(result.iterationHistory().get(0).densityChange() > 0.0);
    assertTrue(Double.isFinite(result.iterationHistory().get(0).deltaEnergy()));
    assertEquals(-1.116659596565565, result.totalEnergy(), 5.0E-5);
    assertEquals(-1.829982464592738, result.electronicEnergy(), 5.0E-5);
    assertEquals(4, result.molecularOrbitalCoefficientsCopy().length);
    assertEquals(4, result.densityMatrixCopy().length);
  }

  @Test
  public void convergenceRequiresBothEnergyAndDensityTolerance() {
    final MolecularIntegralTensors tensors =
        RestrictedHartreeFockSolverTest.hydrogenAtomicOrbitalTensors();
    final RhfOptions options = RhfOptions.of(2, 1.0E9, 1.0E-300, 0.0);

    final RhfResult result = new RestrictedHartreeFockSolver().solve(tensors, 2, options);

    assertFalse(result.converged());
    assertEquals(2, result.iterations());
    assertFalse(result.iterationHistory().get(result.iterationHistory().size() - 1).converged());
    assertTrue(
        result.iterationHistory().get(result.iterationHistory().size() - 1).densityChange()
            > 1.0E-300);
  }

  @Test
  public void directGaussianEriProviderMatchesPackedTensorRhfEnergy() {
    final IntegralMolecule molecule = RestrictedHartreeFockSolverTest.hydrogenIntegralMolecule();
    final GaussianBasisSet basisSet = RestrictedHartreeFockSolverTest.hydrogenSto3gBasis();
    final GaussianIntegralEngine integralEngine = new GaussianIntegralEngine();
    final MolecularIntegralTensors tensors = integralEngine.evaluate(molecule, basisSet);
    final OneElectronIntegralMatrices oneElectronMatrices =
        integralEngine.evaluateOneElectron(molecule, basisSet);
    final RhfResult packedResult = new RestrictedHartreeFockSolver().solve(tensors, 2);
    final RhfResult directResult =
        new RestrictedHartreeFockSolver()
            .solve(
                oneElectronMatrices,
                new DirectGaussianElectronRepulsionIntegralProvider(
                    molecule, basisSet, integralEngine, 64),
                2);

    assertTrue(directResult.converged());
    assertEquals(packedResult.electronicEnergy(), directResult.electronicEnergy(), 1.0E-10);
    assertEquals(packedResult.totalEnergy(), directResult.totalEnergy(), 1.0E-10);
  }

  @Test
  public void directGaussianEriProviderReportsCacheAndScreeningStatistics() {
    final IntegralMolecule molecule = RestrictedHartreeFockSolverTest.hydrogenIntegralMolecule();
    final GaussianBasisSet basisSet = RestrictedHartreeFockSolverTest.hydrogenSto3gBasis();
    final GaussianIntegralEngine integralEngine = new GaussianIntegralEngine();
    final DirectGaussianElectronRepulsionIntegralProvider cachedProvider =
        new DirectGaussianElectronRepulsionIntegralProvider(
            molecule, basisSet, integralEngine, 64, 0.0);

    final double first = cachedProvider.valueAt(0, 0, 0, 0);
    final double second = cachedProvider.valueAt(0, 0, 0, 0);
    final DirectGaussianEriStatistics cachedStatistics = cachedProvider.statistics();

    assertEquals(first, second, 0.0);
    assertEquals(2L, cachedStatistics.valueQueryCount());
    assertEquals(0L, cachedStatistics.screenedOutQueryCount());
    assertEquals(1L, cachedStatistics.cacheHitCount());
    assertEquals(1L, cachedStatistics.computedIntegralCount());
    assertEquals(0L, cachedStatistics.pairBoundBuildCount());
    assertTrue(cachedStatistics.cacheSize() > 0);
    assertTrue(cachedStatistics.cacheCapacity() >= cachedStatistics.cacheSize());
    assertEquals(0.5, cachedStatistics.cacheHitRatio(), 1.0E-15);

    final DirectGaussianElectronRepulsionIntegralProvider screenedProvider =
        new DirectGaussianElectronRepulsionIntegralProvider(
            molecule, basisSet, integralEngine, 64, 1.0E300);
    final double screened = screenedProvider.valueAt(0, 0, 0, 0);
    final DirectGaussianEriStatistics screenedStatistics = screenedProvider.statistics();

    assertEquals(0.0, screened, 0.0);
    assertEquals(1L, screenedStatistics.valueQueryCount());
    assertEquals(1L, screenedStatistics.screenedOutQueryCount());
    assertEquals(0L, screenedStatistics.cacheHitCount());
    assertEquals(0L, screenedStatistics.computedIntegralCount());
    assertEquals(1L, screenedStatistics.pairBoundBuildCount());
    assertTrue(screenedStatistics.pairBoundIntegralCount() > 0L);
    assertEquals(1.0, screenedStatistics.screenedOutRatio(), 1.0E-15);
  }

  static MolecularIntegralTensors hydrogenAtomicOrbitalTensors() {
    final IntegralMolecule molecule = RestrictedHartreeFockSolverTest.hydrogenIntegralMolecule();
    return new GaussianIntegralEngine()
        .evaluate(molecule, RestrictedHartreeFockSolverTest.hydrogenSto3gBasis());
  }

  static IntegralMolecule hydrogenIntegralMolecule() {
    return IntegralMolecule.of(
        List.of(IntegralAtom.of(1, 0.0, 0.0, 0.0), IntegralAtom.of(1, 0.0, 0.0, H2_DISTANCE_BOHR)),
        0);
  }

  static GaussianBasisSet hydrogenSto3gBasis() {
    final IntegralMolecule molecule = RestrictedHartreeFockSolverTest.hydrogenIntegralMolecule();
    return GaussianBasisLibrary.sto3g(molecule);
  }
}

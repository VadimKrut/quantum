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
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.uhf.UhfResult;

public final class UnrestrictedHartreeFockSolverTest {

  @Test
  public void solvesHydrogenAtomSto3gUnrestrictedHartreeFockNearReference() {
    final IntegralMolecule molecule =
        IntegralMolecule.of(List.of(IntegralAtom.of(1, 0.0, 0.0, 0.0)), 0);
    final MolecularIntegralTensors tensors =
        new GaussianIntegralEngine().evaluate(molecule, GaussianBasisLibrary.sto3g(molecule));

    final UhfResult result = new UnrestrictedHartreeFockSolver().solve(tensors, 1, 2);

    assertTrue(result.converged());
    assertEquals(result.iterations(), result.iterationHistory().size());
    assertTrue(result.iterationHistory().get(result.iterationHistory().size() - 1).converged());
    assertTrue(result.iterationHistory().get(0).densityChange() > 0.0);
    assertEquals(1, result.alphaElectronCount());
    assertEquals(0, result.betaElectronCount());
    assertEquals(-0.46658184955727533, result.totalEnergy(), 5.0E-5);
    assertEquals(result.totalEnergy(), result.electronicEnergy(), 1.0E-12);
    assertEquals(0.75, result.spinSquared(), 1.0E-10);
    assertEquals(0.0, result.spinContamination(), 1.0E-10);
    assertEquals(1, result.alphaOrbitalEnergiesCopy().length);
    assertEquals(1, result.betaOrbitalEnergiesCopy().length);
  }

  @Test
  public void unrestrictedConvergedResultSatisfiesDensityToleranceInvariant() {
    final MolecularIntegralTensors tensors =
        RestrictedHartreeFockSolverTest.hydrogenAtomicOrbitalTensors();
    final RhfOptions options = RhfOptions.DEFAULT;

    final UhfResult result = new UnrestrictedHartreeFockSolver().solve(tensors, 1, 2, options);

    assertTrue(result.converged());
    assertTrue(
        result.iterationHistory().get(result.iterationHistory().size() - 1).densityChange()
            <= options.densityTolerance());
  }

  @Test
  public void solvesLithiumDoubletWithOpenShellDiisAcceleration() {
    final IntegralMolecule molecule =
        IntegralMolecule.of(List.of(IntegralAtom.of(3, 0.0, 0.0, 0.0)), 0);
    final MolecularIntegralTensors tensors =
        new GaussianIntegralEngine().evaluate(molecule, GaussianBasisLibrary.sto3g(molecule));
    final RhfOptions options = RhfOptions.of(64, 1.0E-10, 1.0E-8, 0.0);

    final UhfResult result = new UnrestrictedHartreeFockSolver().solve(tensors, 3, 2, options);

    assertTrue(result.converged());
    assertTrue(result.iterations() < options.maxIterations());
    assertEquals(2, result.alphaElectronCount());
    assertEquals(1, result.betaElectronCount());
    assertTrue(result.iterationHistory().get(result.iterationHistory().size() - 1).converged());
    assertTrue(
        result.iterationHistory().get(result.iterationHistory().size() - 1).densityChange()
            <= options.densityTolerance());
    assertEquals(0.75, result.spinSquared(), 1.0E-6);
    assertEquals(0.0, result.spinContamination(), 1.0E-6);
    assertTrue(result.totalEnergy() < 0.0);
  }

  @Test
  public void directIntegralUnrestrictedHartreeFockMatchesTensorPathForOpenShellAtom() {
    final IntegralMolecule molecule =
        IntegralMolecule.of(List.of(IntegralAtom.of(3, 0.0, 0.0, 0.0)), 0);
    final GaussianIntegralEngine integralEngine = new GaussianIntegralEngine();
    final GaussianBasisSet basisSet = GaussianBasisLibrary.sto3g(molecule);
    final MolecularIntegralTensors tensors = integralEngine.evaluate(molecule, basisSet);
    final OneElectronIntegralMatrices oneElectronMatrices =
        integralEngine.evaluateOneElectron(molecule, basisSet);
    final RhfOptions options = RhfOptions.of(64, 1.0E-10, 1.0E-8, 0.0);
    final UnrestrictedHartreeFockSolver solver = new UnrestrictedHartreeFockSolver();

    final UhfResult tensorResult = solver.solve(tensors, 3, 2, options);
    final UhfResult directResult =
        solver.solve(
            oneElectronMatrices,
            new DirectGaussianElectronRepulsionIntegralProvider(
                molecule, basisSet, integralEngine, 4096),
            3,
            2,
            options);

    assertTrue(directResult.converged());
    assertEquals(tensorResult.electronicEnergy(), directResult.electronicEnergy(), 1.0E-9);
    assertEquals(tensorResult.totalEnergy(), directResult.totalEnergy(), 1.0E-9);
    assertEquals(tensorResult.spinSquared(), directResult.spinSquared(), 1.0E-9);
    assertEquals(tensorResult.spinContamination(), directResult.spinContamination(), 1.0E-9);
  }
}

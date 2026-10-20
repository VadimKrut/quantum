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
import ru.pathcreator.vadim.quantum.chemistry.scf.application.validation.ScfResultValidator;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.rhf.RhfOptions;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.rohf.RohfResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.validation.ScfValidationReport;

public final class RestrictedOpenShellHartreeFockSolverTest {

  @Test
  public void solvesHydrogenAtomSto3gRestrictedOpenShellHartreeFockNearReference() {
    final IntegralMolecule molecule =
        IntegralMolecule.of(List.of(IntegralAtom.of(1, 0.0, 0.0, 0.0)), 0);
    final MolecularIntegralTensors tensors =
        new GaussianIntegralEngine().evaluate(molecule, GaussianBasisLibrary.sto3g(molecule));

    final RohfResult result = new RestrictedOpenShellHartreeFockSolver().solve(tensors, 1, 2);

    assertTrue(result.converged());
    assertEquals(result.iterations(), result.iterationHistory().size());
    assertEquals(1, result.alphaElectronCount());
    assertEquals(0, result.betaElectronCount());
    assertEquals(0, result.closedShellElectronPairCount());
    assertEquals(1, result.openShellElectronCount());
    assertEquals(-0.46658184955727533, result.totalEnergy(), 5.0E-5);
    assertEquals(result.totalEnergy(), result.electronicEnergy(), 1.0E-12);
    assertEquals(0.75, result.spinSquared(), 1.0E-12);
    assertEquals(0.0, result.spinContamination(), 1.0E-12);
    assertEquals(1, result.orbitalEnergiesCopy().length);
  }

  @Test
  public void directIntegralRestrictedOpenShellHartreeFockMatchesTensorPathForHydrogenAtom() {
    final IntegralMolecule molecule =
        IntegralMolecule.of(List.of(IntegralAtom.of(1, 0.0, 0.0, 0.0)), 0);
    final GaussianIntegralEngine integralEngine = new GaussianIntegralEngine();
    final GaussianBasisSet basisSet = GaussianBasisLibrary.sto3g(molecule);
    final MolecularIntegralTensors tensors = integralEngine.evaluate(molecule, basisSet);
    final OneElectronIntegralMatrices oneElectronMatrices =
        integralEngine.evaluateOneElectron(molecule, basisSet);
    final RestrictedOpenShellHartreeFockSolver solver = new RestrictedOpenShellHartreeFockSolver();

    final RohfResult tensorResult = solver.solve(tensors, 1, 2, RhfOptions.DEFAULT);
    final RohfResult directResult =
        solver.solve(
            oneElectronMatrices,
            new DirectGaussianElectronRepulsionIntegralProvider(
                molecule, basisSet, integralEngine, 64),
            1,
            2,
            RhfOptions.DEFAULT);

    assertTrue(directResult.converged());
    assertEquals(tensorResult.electronicEnergy(), directResult.electronicEnergy(), 1.0E-12);
    assertEquals(tensorResult.totalEnergy(), directResult.totalEnergy(), 1.0E-12);
    assertEquals(tensorResult.spinSquared(), directResult.spinSquared(), 1.0E-12);
    assertEquals(tensorResult.spinContamination(), directResult.spinContamination(), 1.0E-12);
  }

  @Test
  public void restrictedOpenShellResultSatisfiesScfValidationInvariants() {
    final IntegralMolecule molecule =
        IntegralMolecule.of(List.of(IntegralAtom.of(1, 0.0, 0.0, 0.0)), 0);
    final MolecularIntegralTensors tensors =
        new GaussianIntegralEngine().evaluate(molecule, GaussianBasisLibrary.sto3g(molecule));

    final RohfResult result = new RestrictedOpenShellHartreeFockSolver().solve(tensors, 1, 2);
    final ScfValidationReport report =
        new ScfResultValidator()
            .validateRestrictedOpenShell(result, tensors.overlapCopy(), tensors.orbitalCount());

    assertTrue(report.diagnostics().isEmpty());
    assertEquals(0.0, report.densitySymmetryDeviation(), 1.0E-12);
    assertEquals(0.0, report.electronTraceDeviation(), 1.0E-12);
  }
}

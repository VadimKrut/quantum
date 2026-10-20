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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.ElectronicHamiltonianIntegralBuilder;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.GaussianBasisLibrary;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.GaussianIntegralEngine;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.MolecularOrbitalIntegralTransformer;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralAtom;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralMolecule;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.MolecularIntegralTensors;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.fci.FciOptions;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.fci.FciResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.application.RestrictedHartreeFockSolver;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.rhf.RhfResult;

public final class FullConfigurationInteractionSolverTest {

  private static final double H2_DISTANCE_BOHR = 1.4018897259885788;

  @Test
  public void solvesHydrogenSto3gFullConfigurationInteractionNearPyscfReference() {
    final ElectronicHamiltonian hamiltonian =
        FullConfigurationInteractionSolverTest.hydrogenMolecularOrbitalHamiltonian();
    final FciResult result = new FullConfigurationInteractionSolver().solve(hamiltonian);

    assertEquals(6, result.determinantCount());
    assertEquals(-1.137265043874881, result.totalEnergy(), 1.0E-5);
    assertEquals(-1.850587911902053, result.electronicEnergy(), 1.0E-5);
    assertTrue(result.eigenvectorCopy().length == result.determinantCount());
  }

  @Test
  public void returnsRequestedExcitedRootsWithIndependentEigenvectors() {
    final ElectronicHamiltonian hamiltonian =
        FullConfigurationInteractionSolverTest.hydrogenMolecularOrbitalHamiltonian();
    final FciResult result =
        new FullConfigurationInteractionSolver().solve(hamiltonian, FciOptions.of(20_000, 4));

    assertEquals(6, result.determinantCount());
    assertEquals(4, result.rootCount());
    for (int rootIndex = 1; rootIndex < result.rootCount(); ++rootIndex) {
      assertTrue(result.totalEnergy(rootIndex - 1) <= result.totalEnergy(rootIndex));
      assertEquals(result.determinantCount(), result.eigenvectorCopy(rootIndex).length);
    }
    assertEquals(result.totalEnergy(), result.totalEnergiesCopy()[0], 0.0);
    assertEquals(result.electronicEnergy(), result.electronicEnergiesCopy()[0], 0.0);
  }

  @Test
  public void spinResolvedSectorKeepsClosedShellGroundEnergyWithSmallerDeterminantSpace() {
    final ElectronicHamiltonian hamiltonian =
        FullConfigurationInteractionSolverTest.hydrogenMolecularOrbitalHamiltonian();
    final FullConfigurationInteractionSolver solver = new FullConfigurationInteractionSolver();
    final FciResult fullResult = solver.solve(hamiltonian);
    final FciResult singletSectorResult =
        solver.solve(hamiltonian, FciOptions.spinSector(20_000, 2, 1, 1));

    assertEquals(6, fullResult.determinantCount());
    assertEquals(4, singletSectorResult.determinantCount());
    assertEquals(2, singletSectorResult.rootCount());
    assertEquals(fullResult.totalEnergy(), singletSectorResult.totalEnergy(), 1.0E-8);
    assertEquals(
        singletSectorResult.determinantCount(), singletSectorResult.eigenvectorCopy(1).length);
  }

  static ElectronicHamiltonian hydrogenMolecularOrbitalHamiltonian() {
    final IntegralMolecule molecule =
        IntegralMolecule.of(
            List.of(
                IntegralAtom.of(1, 0.0, 0.0, 0.0), IntegralAtom.of(1, 0.0, 0.0, H2_DISTANCE_BOHR)),
            0);
    final MolecularIntegralTensors atomicOrbitalTensors =
        new GaussianIntegralEngine().evaluate(molecule, GaussianBasisLibrary.sto3g(molecule));
    final RhfResult rhfResult = new RestrictedHartreeFockSolver().solve(atomicOrbitalTensors, 2);
    final MolecularIntegralTensors molecularOrbitalTensors =
        new MolecularOrbitalIntegralTransformer()
            .transform(atomicOrbitalTensors, rhfResult.molecularOrbitalCoefficientsCopy());
    return new ElectronicHamiltonianIntegralBuilder().build(2, molecularOrbitalTensors);
  }
}

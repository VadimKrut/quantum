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

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.GaussianBasisLibrary;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.GaussianIntegralEngine;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.MolecularOrbitalIntegralTransformer;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralAtom;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralMolecule;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.MolecularIntegralTensors;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.mp2.Mp2Result;
import ru.pathcreator.vadim.quantum.chemistry.scf.application.RestrictedHartreeFockSolver;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.rhf.RhfResult;

public final class ClosedShellMp2SolverTest {

  private static final double H2_DISTANCE_BOHR = 1.4018897259885788;

  @Test
  public void solvesHydrogenSto3gMp2NearPyscfReference() {
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
    final Mp2Result result =
        new ClosedShellMp2Solver().solve(molecularOrbitalTensors, rhfResult, 2);

    assertEquals(1, result.occupiedOrbitalCount());
    assertEquals(1, result.virtualOrbitalCount());
    assertEquals(-0.013170766469969, result.correlationEnergy(), 2.0E-5);
    assertEquals(-1.129855153555309, result.totalEnergy(), 5.0E-5);
  }
}

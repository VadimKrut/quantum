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
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralAtom;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralMolecule;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.MolecularIntegralTensors;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.mp2.UnrestrictedMp2Result;
import ru.pathcreator.vadim.quantum.chemistry.scf.application.UnrestrictedHartreeFockSolver;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.uhf.UhfResult;

public final class UnrestrictedMp2SolverTest {

  @Test
  public void hydrogenDoubletHasValidZeroPairCorrelationInSix31gBasis() {
    final IntegralMolecule molecule =
        IntegralMolecule.of(List.of(IntegralAtom.of(1, 0.0, 0.0, 0.0)), 0);
    final MolecularIntegralTensors tensors =
        new GaussianIntegralEngine().evaluate(molecule, GaussianBasisLibrary.six31g(molecule));
    final UhfResult uhfResult = new UnrestrictedHartreeFockSolver().solve(tensors, 1, 2);

    final UnrestrictedMp2Result result = new UnrestrictedMp2Solver().solve(tensors, uhfResult);

    assertEquals(0.0, result.correlationEnergy(), 1.0E-12);
    assertEquals(uhfResult.totalEnergy(), result.totalEnergy(), 1.0E-12);
    assertEquals(1, result.alphaOccupiedOrbitalCount());
    assertEquals(0, result.betaOccupiedOrbitalCount());
    assertEquals(1, result.alphaVirtualOrbitalCount());
    assertEquals(2, result.betaVirtualOrbitalCount());
  }

  @Test
  public void methylidyneDoubletProducesFiniteOpenShellCorrelation() {
    final IntegralMolecule molecule =
        IntegralMolecule.of(
            List.of(
                IntegralAtom.of(6, 0.0, 0.0, 0.0),
                IntegralAtom.of(1, 0.0, 0.0, 1.12 * 1.8897259886)),
            0);
    final MolecularIntegralTensors tensors =
        new GaussianIntegralEngine().evaluate(molecule, GaussianBasisLibrary.sto3g(molecule));
    final UhfResult uhfResult = new UnrestrictedHartreeFockSolver().solve(tensors, 7, 2);

    final UnrestrictedMp2Result result = new UnrestrictedMp2Solver().solve(tensors, uhfResult);

    assertTrue(result.correlationEnergy() < 0.0);
    assertTrue(Double.isFinite(result.totalEnergy()));
    assertEquals(
        result.correlationEnergy(),
        result.sameSpinCorrelationEnergy() + result.oppositeSpinCorrelationEnergy(),
        1.0E-10);
    assertEquals(4, result.alphaOccupiedOrbitalCount());
    assertEquals(3, result.betaOccupiedOrbitalCount());
  }

  @Test
  public void rejectsFullyOccupiedSpinSpacesWithoutVirtualOrbital() {
    final IntegralMolecule molecule =
        IntegralMolecule.of(List.of(IntegralAtom.of(1, 0.0, 0.0, 0.0)), -1);
    final MolecularIntegralTensors tensors =
        new GaussianIntegralEngine().evaluate(molecule, GaussianBasisLibrary.sto3g(molecule));
    final UhfResult uhfResult = new UnrestrictedHartreeFockSolver().solve(tensors, 2, 1);

    assertThrows(
        IllegalArgumentException.class,
        () -> new UnrestrictedMp2Solver().solve(tensors, uhfResult));
  }
}

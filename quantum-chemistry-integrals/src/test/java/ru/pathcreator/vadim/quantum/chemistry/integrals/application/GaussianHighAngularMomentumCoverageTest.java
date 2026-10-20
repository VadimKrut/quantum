/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.integrals.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.ContractedGaussianOrbital;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianAngularMomentum;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianBasisSet;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianPrimitive;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianShellType;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralAtom;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralMolecule;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.MolecularIntegralTensors;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.OneElectronIntegralMatrices;

public final class GaussianHighAngularMomentumCoverageTest {

  @Test
  public void cartesianShellFactoryCoversHighAngularMomentumFunctionCounts() {
    assertEquals(6, GaussianAngularMomentum.cartesianShell(GaussianShellType.D).size());
    assertEquals(10, GaussianAngularMomentum.cartesianShell(GaussianShellType.F).size());
    assertEquals(15, GaussianAngularMomentum.cartesianShell(GaussianShellType.G).size());
    assertEquals(21, GaussianAngularMomentum.cartesianShell(GaussianShellType.H).size());
    assertEquals(28, GaussianAngularMomentum.cartesianShell(GaussianShellType.I).size());
    assertEquals(36, GaussianAngularMomentum.cartesianShell(GaussianShellType.K).size());
    assertEquals(91, GaussianAngularMomentum.cartesianShell(GaussianShellType.Q).size());
  }

  @Test
  public void oneElectronEngineEvaluatesCartesianGOrbitalsWithFiniteSymmetricMatrices() {
    final IntegralMolecule molecule =
        IntegralMolecule.of(
            List.of(IntegralAtom.of(6, 0.0, 0.0, 0.0), IntegralAtom.of(8, 0.0, 0.0, 1.2)), 0);
    final GaussianBasisSet basis =
        GaussianBasisSet.of(
            "g-shell-smoke",
            List.of(
                ContractedGaussianOrbital.of(
                    0, GaussianAngularMomentum.S, List.of(GaussianPrimitive.of(0.9, 1.0))),
                ContractedGaussianOrbital.of(
                    1,
                    GaussianAngularMomentum.of(4, 0, 0),
                    List.of(GaussianPrimitive.of(0.7, 0.8)))));

    final OneElectronIntegralMatrices matrices =
        new GaussianOneElectronIntegralEngine().evaluate(molecule, basis);

    assertNull(GaussianIntegralCapability.unsupportedOneElectronReason(basis));
    assertEquals(2, matrices.orbitalCount());
    for (int row = 0; row < matrices.orbitalCount(); ++row) {
      for (int column = 0; column < matrices.orbitalCount(); ++column) {
        assertTrue(Double.isFinite(matrices.overlapAt(row, column)));
        assertTrue(Double.isFinite(matrices.kineticAt(row, column)));
        assertTrue(Double.isFinite(matrices.nuclearAttractionAt(row, column)));
        assertEquals(matrices.overlapAt(row, column), matrices.overlapAt(column, row), 1.0E-12);
        assertEquals(matrices.kineticAt(row, column), matrices.kineticAt(column, row), 1.0E-12);
        assertEquals(
            matrices.nuclearAttractionAt(row, column),
            matrices.nuclearAttractionAt(column, row),
            1.0E-12);
      }
    }
  }

  @Test
  public void fullEriEngineEvaluatesHighDerivativeGOrbitalsWithSparseTaylorPath() {
    final IntegralMolecule molecule =
        IntegralMolecule.of(List.of(IntegralAtom.of(6, 0.0, 0.0, 0.0)), 0);
    final GaussianBasisSet basis =
        GaussianBasisSet.of(
            "full-g-eri-diagnostic",
            List.of(
                ContractedGaussianOrbital.of(
                    0,
                    GaussianAngularMomentum.of(4, 0, 0),
                    List.of(GaussianPrimitive.of(0.7, 1.0)))));

    final String reason = GaussianIntegralCapability.unsupportedFullTensorReason(basis);
    final GaussianIntegralPreflight preflight =
        GaussianIntegralCapability.preflightFullTensor(basis);
    final MolecularIntegralTensors tensors = new GaussianIntegralEngine().evaluate(molecule, basis);

    assertNull(reason);
    assertEquals(GaussianIntegralCapabilityStatus.SUPPORTED, preflight.status());
    assertTrue(preflight.supported());
    assertEquals(16, preflight.maxFullEriDerivativeOrder());
    assertEquals(1, tensors.orbitalCount());
    assertTrue(Double.isFinite(tensors.electronRepulsionAt(0, 0, 0, 0)));
    assertTrue(tensors.electronRepulsionAt(0, 0, 0, 0) > 0.0);
  }

  @Test
  public void oneElectronEngineEvaluatesQOrbitalsThroughSparseTaylorPath() {
    final IntegralMolecule molecule =
        IntegralMolecule.of(List.of(IntegralAtom.of(6, 0.0, 0.0, 0.0)), 0);
    final GaussianBasisSet basis =
        GaussianBasisSet.of(
            "q-shell-one-electron-smoke",
            List.of(
                ContractedGaussianOrbital.of(
                    0,
                    GaussianAngularMomentum.of(12, 0, 0),
                    List.of(GaussianPrimitive.of(0.5, 1.0)))));

    final OneElectronIntegralMatrices matrices =
        new GaussianOneElectronIntegralEngine().evaluate(molecule, basis);

    assertNull(GaussianIntegralCapability.unsupportedOneElectronReason(basis));
    assertEquals(1, matrices.orbitalCount());
    assertTrue(Double.isFinite(matrices.overlapAt(0, 0)));
    assertTrue(Double.isFinite(matrices.kineticAt(0, 0)));
    assertTrue(Double.isFinite(matrices.nuclearAttractionAt(0, 0)));
  }
}

/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.integrals.application;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.ContractedGaussianOrbital;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianAngularMomentum;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianBasisSet;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianPrimitive;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralAtom;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralMolecule;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.DipoleIntegralMatrices;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.OneElectronIntegralMatrices;

public final class GaussianDipoleIntegralEngineTest {

  @Test
  public void sOrbitalDipoleMomentEqualsCenterTimesOverlapForSameCenter() {
    final IntegralMolecule molecule =
        IntegralMolecule.of(List.of(IntegralAtom.of(1, 1.25, -0.5, 0.75)), 0);
    final GaussianBasisSet basis =
        GaussianBasisSet.of(
            "single-s",
            List.of(
                ContractedGaussianOrbital.of(
                    0, GaussianAngularMomentum.S, List.of(GaussianPrimitive.of(0.8, 1.0)))));
    final OneElectronIntegralMatrices oneElectron =
        new GaussianOneElectronIntegralEngine().evaluate(molecule, basis);

    final DipoleIntegralMatrices dipole =
        new GaussianDipoleIntegralEngine().evaluate(molecule, basis);

    Assertions.assertEquals(1, dipole.orbitalCount());
    Assertions.assertEquals(1.25 * oneElectron.overlapAt(0, 0), dipole.valueAt(0, 0, 0), 1.0E-12);
    Assertions.assertEquals(-0.5 * oneElectron.overlapAt(0, 0), dipole.valueAt(1, 0, 0), 1.0E-12);
    Assertions.assertEquals(0.75 * oneElectron.overlapAt(0, 0), dipole.valueAt(2, 0, 0), 1.0E-12);
  }

  @Test
  public void dipoleMatricesAreSymmetricForTwoCenterContractedBasis() {
    final IntegralMolecule molecule =
        IntegralMolecule.of(
            List.of(IntegralAtom.of(1, 0.0, 0.0, 0.0), IntegralAtom.of(1, 0.0, 0.0, 1.4)), 0);
    final GaussianBasisSet basis =
        GaussianBasisSet.of(
            "two-s",
            List.of(
                ContractedGaussianOrbital.of(
                    0, GaussianAngularMomentum.S, List.of(GaussianPrimitive.of(0.7, 0.9))),
                ContractedGaussianOrbital.of(
                    1, GaussianAngularMomentum.S, List.of(GaussianPrimitive.of(0.6, 1.1)))));

    final DipoleIntegralMatrices dipole =
        new GaussianDipoleIntegralEngine().evaluate(molecule, basis);

    for (int axis = 0; axis < 3; ++axis) {
      Assertions.assertEquals(dipole.valueAt(axis, 0, 1), dipole.valueAt(axis, 1, 0), 1.0E-12);
    }
    Assertions.assertTrue(dipole.valueAt(2, 1, 1) > dipole.valueAt(2, 0, 0));
  }
}

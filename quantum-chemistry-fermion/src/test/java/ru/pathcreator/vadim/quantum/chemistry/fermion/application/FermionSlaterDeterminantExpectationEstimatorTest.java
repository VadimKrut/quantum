/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.fermion.application;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.OneElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.TwoElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionTerm;

final class FermionSlaterDeterminantExpectationEstimatorTest {

  @Test
  void expectationKeepsIdentityNumberAndOffDiagonalRulesSeparate() {
    final FermionOperator operator =
        FermionOperator.builder()
            .add(FermionTerm.IDENTITY, 3.0)
            .add(FermionTerm.of(FermionTerm.creation(0), FermionTerm.annihilation(0)), -2.0)
            .add(FermionTerm.of(FermionTerm.creation(1), FermionTerm.annihilation(0)), 100.0)
            .build();

    final double energy =
        new FermionSlaterDeterminantExpectationEstimator()
            .expectation(operator, new boolean[] {true, false});

    Assertions.assertEquals(1.0, energy, 1.0E-12);
  }

  @Test
  void expectationPreservesFermionSignForExchangeLikeClosedPath() {
    final FermionOperator operator =
        FermionOperator.builder()
            .add(
                FermionTerm.of(
                    FermionTerm.creation(0),
                    FermionTerm.creation(1),
                    FermionTerm.annihilation(0),
                    FermionTerm.annihilation(1)),
                0.75)
            .build();

    final double energy =
        new FermionSlaterDeterminantExpectationEstimator()
            .expectation(operator, new boolean[] {true, true});

    Assertions.assertEquals(-0.75, energy, 1.0E-12);
  }

  @Test
  void hartreeFockReferenceEnergyFollowsMappedElectronicHamiltonian() {
    final ElectronicHamiltonian hamiltonian =
        ElectronicHamiltonian.of(
            ActiveSpace.of(2, 2),
            0.4,
            List.of(
                OneElectronIntegral.of(0, 0, -1.0),
                OneElectronIntegral.of(1, 1, -0.25),
                OneElectronIntegral.of(0, 1, 0.08)),
            List.of(
                TwoElectronIntegral.of(0, 0, 0, 0, 0.7),
                TwoElectronIntegral.of(0, 0, 1, 1, 0.3),
                TwoElectronIntegral.of(0, 1, 0, 1, 0.05)));

    final double energy =
        new FermionSlaterDeterminantExpectationEstimator().hartreeFockReferenceEnergy(hamiltonian);

    Assertions.assertTrue(Double.isFinite(energy));
    Assertions.assertTrue(energy < 0.0);
    Assertions.assertTrue(energy > -10.0);
  }
}

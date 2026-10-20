/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.fermion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.OneElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.TwoElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;

public final class ElectronicHamiltonianQubitMapperTest {

  @Test
  public void verifiedMappingReturnsOperatorAndIndependentProofsForEveryBuiltInMapping() {
    final ElectronicHamiltonian hamiltonian =
        ElectronicHamiltonian.of(
            ActiveSpace.of(2, 2),
            0.713753993687618,
            List.of(
                OneElectronIntegral.of(0, 0, -1.252463573565),
                OneElectronIntegral.of(1, 1, -0.475948715221),
                OneElectronIntegral.of(0, 1, 0.03451219)),
            List.of(
                TwoElectronIntegral.of(0, 0, 0, 0, 0.674488766357),
                TwoElectronIntegral.of(0, 0, 1, 1, 0.663468096424),
                TwoElectronIntegral.of(0, 1, 0, 1, 0.181288808211),
                TwoElectronIntegral.of(1, 1, 1, 1, 0.697393767423)));
    final ElectronicHamiltonianQubitMapper mapper = new ElectronicHamiltonianQubitMapper();

    for (final FermionToQubitMapping mapping : FermionToQubitMapping.values()) {
      final ElectronicHamiltonianQubitMapper.VerifiedElectronicHamiltonianMapping result =
          mapper.mapVerified(hamiltonian, mapping, 1.0E-8);

      assertEquals(mapping, result.mapping());
      assertTrue(result.fermionOperator().termCount() > 0, mapping.name());
      assertTrue(result.qubitOperator().termCount() > 0, mapping.name());
      assertTrue(result.hasHamiltonianAudit(), mapping.name());
      assertTrue(result.hamiltonianAudit().readyForFermionMapping(), mapping.name());
      assertTrue(result.verified(), mapping.name());
      assertTrue(result.algebraResult().validWithinTolerance(), mapping.name());
      assertTrue(result.spectralResult().equivalentWithinTolerance(), mapping.name());
    }
  }

  @Test
  public void verifiedMappingRejectsInvalidInput() {
    final ElectronicHamiltonianQubitMapper mapper = new ElectronicHamiltonianQubitMapper();
    final ElectronicHamiltonian hamiltonian =
        ElectronicHamiltonian.of(
            ActiveSpace.of(1, 1), 0.0, List.of(OneElectronIntegral.of(0, 0, -0.5)), List.of());

    assertThrows(
        IllegalArgumentException.class,
        () -> mapper.mapVerified(null, FermionToQubitMapping.JORDAN_WIGNER, 1.0E-8));
    assertThrows(
        IllegalArgumentException.class, () -> mapper.mapVerified(hamiltonian, null, 1.0E-8));
    assertThrows(
        IllegalArgumentException.class,
        () -> mapper.mapVerified(hamiltonian, FermionToQubitMapping.JORDAN_WIGNER, -1.0E-8));
  }
}

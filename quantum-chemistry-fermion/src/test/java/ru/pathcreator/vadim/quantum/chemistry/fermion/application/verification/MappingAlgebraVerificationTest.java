/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.fermion.application.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;

public final class MappingAlgebraVerificationTest {

  @Test
  public void verifiesCanonicalAnticommutationForEveryBuiltInMapping() {
    final MappingAlgebraVerification verification = new MappingAlgebraVerification();

    for (final FermionToQubitMapping mapping : FermionToQubitMapping.values()) {
      final MappingAlgebraVerification.MappingAlgebraVerificationResult result =
          verification.verify(mapping, 4, 1.0E-12);

      assertEquals(mapping, result.mapping());
      assertEquals(4, result.spinOrbitalCount());
      assertTrue(
          result.validWithinTolerance(), mapping.name() + " residual " + result.maxResidual());
      assertEquals(0.0, result.annihilationAnnihilationResidual(), 1.0E-12, mapping.name());
      assertEquals(0.0, result.creationCreationResidual(), 1.0E-12, mapping.name());
      assertEquals(0.0, result.mixedResidual(), 1.0E-12, mapping.name());
    }
  }

  @Test
  public void verifiesLargerEvenSpinSpaceForSpinBlockMappings() {
    final MappingAlgebraVerification verification = new MappingAlgebraVerification();

    final FermionToQubitMapping[] spinBlockMappings =
        new FermionToQubitMapping[] {
          FermionToQubitMapping.SPIN_BLOCK_JORDAN_WIGNER,
          FermionToQubitMapping.SPIN_BLOCK_PARITY,
          FermionToQubitMapping.SPIN_BLOCK_BRAVYI_KITAEV
        };
    for (int i = 0; i < spinBlockMappings.length; ++i) {
      final MappingAlgebraVerification.MappingAlgebraVerificationResult result =
          verification.verify(spinBlockMappings[i], 6, 1.0E-12);

      assertTrue(
          result.validWithinTolerance(),
          spinBlockMappings[i].name() + " residual " + result.maxResidual());
    }
  }

  @Test
  public void rejectsInvalidVerificationBounds() {
    final MappingAlgebraVerification verification = new MappingAlgebraVerification();

    assertThrows(
        IllegalArgumentException.class,
        () -> verification.verify(FermionToQubitMapping.JORDAN_WIGNER, 0, 1.0E-12));
    assertThrows(
        IllegalArgumentException.class,
        () -> verification.verify(FermionToQubitMapping.JORDAN_WIGNER, 33, 1.0E-12));
    assertThrows(
        IllegalArgumentException.class,
        () -> verification.verify(FermionToQubitMapping.JORDAN_WIGNER, 4, -1.0E-12));
    assertThrows(IllegalArgumentException.class, () -> verification.verify(null, 4, 1.0E-12));
  }
}

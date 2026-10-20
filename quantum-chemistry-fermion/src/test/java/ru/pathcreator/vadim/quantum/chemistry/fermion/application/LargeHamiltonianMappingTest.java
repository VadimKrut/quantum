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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.OneElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.TwoElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliOperator;

public final class LargeHamiltonianMappingTest {

  @Test
  public void mapsLargerCanonicalHamiltonianWithoutLosingHermiticity() {
    final int orbitalCount = 6;
    final ElectronicHamiltonian hamiltonian =
        ElectronicHamiltonian.of(
            ActiveSpace.of(6, orbitalCount),
            3.25,
            LargeHamiltonianMappingTest.oneElectronIntegrals(orbitalCount),
            LargeHamiltonianMappingTest.twoElectronIntegrals(orbitalCount));
    final FermionOperator fermionOperator =
        new ElectronicHamiltonianFermionMapper().map(hamiltonian);
    final QubitPauliOperator qubitOperator =
        new FermionToQubitMapper()
            .map(
                fermionOperator,
                FermionToQubitMapping.JORDAN_WIGNER,
                hamiltonian.spinOrbitalCount());

    assertTrue(fermionOperator.termCount() > hamiltonian.integralCount());
    assertTrue(qubitOperator.termCount() > 0);
    assertEquals(hamiltonian.spinOrbitalCount(), qubitOperator.requiredQubitCount());
    assertTrue(qubitOperator.hermitianWithinTolerance(1.0E-10));
  }

  private static List<OneElectronIntegral> oneElectronIntegrals(final int orbitalCount) {
    final ArrayList<OneElectronIntegral> integrals =
        new ArrayList<OneElectronIntegral>(orbitalCount * (orbitalCount + 1) / 2);
    for (int first = 0; first < orbitalCount; ++first) {
      for (int second = first; second < orbitalCount; ++second) {
        final double value =
            first == second ? -1.0 / (double) (first + 1) : 0.01 * (double) (first + second + 1);
        integrals.add(OneElectronIntegral.of(first, second, value));
      }
    }
    return List.copyOf(integrals);
  }

  private static List<TwoElectronIntegral> twoElectronIntegrals(final int orbitalCount) {
    final ArrayList<TwoElectronIntegral> integrals = new ArrayList<TwoElectronIntegral>(256);
    for (int first = 0; first < orbitalCount; ++first) {
      for (int second = first; second < orbitalCount; ++second) {
        for (int third = 0; third < orbitalCount; ++third) {
          for (int fourth = third; fourth < orbitalCount; ++fourth) {
            if (!LargeHamiltonianMappingTest.keepCanonical(first, second, third, fourth)) {
              continue;
            }
            final int checksum = first + 3 * second + 5 * third + 7 * fourth;
            if ((checksum & 3) != 0) {
              continue;
            }
            final double value = 0.001 * (double) (checksum + 1);
            integrals.add(TwoElectronIntegral.of(first, second, third, fourth, value));
          }
        }
      }
    }
    return List.copyOf(integrals);
  }

  private static boolean keepCanonical(
      final int first, final int second, final int third, final int fourth) {
    return LargeHamiltonianMappingTest.pairKey(first, second)
        <= LargeHamiltonianMappingTest.pairKey(third, fourth);
  }

  private static long pairKey(final int first, final int second) {
    final int lower = Math.min(first, second);
    final int upper = Math.max(first, second);
    return (long) upper * ((long) upper + 1L) / 2L + (long) lower;
  }
}

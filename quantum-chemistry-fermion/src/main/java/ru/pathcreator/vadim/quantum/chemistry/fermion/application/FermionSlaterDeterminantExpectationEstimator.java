/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.fermion.application;

import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.coefficient.ComplexCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionLadderAction;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionTerm;

/** Считает энергию Slater determinant напрямую в fermion algebra без qubit mapping и симулятора. */
public final class FermionSlaterDeterminantExpectationEstimator {

  private static final double IMAGINARY_EXPECTATION_TOLERANCE = 1.0E-10;

  private final ElectronicHamiltonianFermionMapper mapper;

  public FermionSlaterDeterminantExpectationEstimator() {
    this(new ElectronicHamiltonianFermionMapper());
  }

  public FermionSlaterDeterminantExpectationEstimator(
      final ElectronicHamiltonianFermionMapper mapper) {
    if (mapper == null) {
      throw new IllegalArgumentException("Slater determinant expectation mapper must not be null.");
    }
    this.mapper = mapper;
  }

  public double hartreeFockReferenceEnergy(final ElectronicHamiltonian hamiltonian) {
    if (hamiltonian == null) {
      throw new IllegalArgumentException("Slater determinant Hamiltonian must not be null.");
    }
    return this.expectation(
        this.mapper.mapNormalOrdered(hamiltonian),
        FermionSlaterDeterminantExpectationEstimator.closedPrefixOccupation(
            hamiltonian.spinOrbitalCount(), hamiltonian.electronCount()));
  }

  public double expectation(final FermionOperator operator, final boolean[] occupation) {
    if (operator == null) {
      throw new IllegalArgumentException("Slater determinant fermion operator must not be null.");
    }
    FermionSlaterDeterminantExpectationEstimator.requireOccupation(occupation);
    double real = 0.0;
    double imaginary = 0.0;
    for (int i = 0; i < operator.termCount(); ++i) {
      final int phase =
          FermionSlaterDeterminantExpectationEstimator.termPhase(operator.term(i), occupation);
      if (phase == 0) {
        continue;
      }
      final ComplexCoefficient coefficient = operator.coefficient(i);
      real += phase * coefficient.real();
      imaginary += phase * coefficient.imaginary();
    }
    if (Math.abs(imaginary) > IMAGINARY_EXPECTATION_TOLERANCE) {
      throw new IllegalArgumentException(
          "Slater determinant expectation has non-real value: " + imaginary + "i.");
    }
    return real;
  }

  public static boolean[] closedPrefixOccupation(
      final int spinOrbitalCount, final int electronCount) {
    if (spinOrbitalCount <= 0) {
      throw new IllegalArgumentException("Slater determinant spin orbital count must be positive.");
    }
    if (electronCount < 0 || electronCount > spinOrbitalCount) {
      throw new IllegalArgumentException(
          "Slater determinant electron count is outside spin orbital space.");
    }
    final boolean[] occupation = new boolean[spinOrbitalCount];
    for (int i = 0; i < electronCount; ++i) {
      occupation[i] = true;
    }
    return occupation;
  }

  private static int termPhase(final FermionTerm term, final boolean[] referenceOccupation) {
    if (term.identity()) {
      return 1;
    }
    final boolean[] occupation = new boolean[referenceOccupation.length];
    System.arraycopy(referenceOccupation, 0, occupation, 0, referenceOccupation.length);
    int phase = 1;
    for (int i = term.length() - 1; i >= 0; --i) {
      final long encodedOperator = term.operator(i);
      final int spinOrbital = FermionTerm.spinOrbital(encodedOperator);
      if (spinOrbital >= occupation.length) {
        throw new IllegalArgumentException(
            "Fermion term references spin orbital outside determinant.");
      }
      if ((FermionSlaterDeterminantExpectationEstimator.occupiedBelow(occupation, spinOrbital) & 1)
          != 0) {
        phase = -phase;
      }
      final FermionLadderAction action = FermionTerm.action(encodedOperator);
      if (action == FermionLadderAction.ANNIHILATION) {
        if (!occupation[spinOrbital]) {
          return 0;
        }
        occupation[spinOrbital] = false;
      } else {
        if (occupation[spinOrbital]) {
          return 0;
        }
        occupation[spinOrbital] = true;
      }
    }
    for (int i = 0; i < occupation.length; ++i) {
      if (occupation[i] != referenceOccupation[i]) {
        return 0;
      }
    }
    return phase;
  }

  private static int occupiedBelow(final boolean[] occupation, final int upperExclusive) {
    int count = 0;
    for (int i = 0; i < upperExclusive; ++i) {
      if (occupation[i]) {
        ++count;
      }
    }
    return count;
  }

  private static void requireOccupation(final boolean[] occupation) {
    if (occupation == null || occupation.length == 0) {
      throw new IllegalArgumentException("Slater determinant occupation must not be empty.");
    }
  }
}

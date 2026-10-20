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
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.OneElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.TwoElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionTerm;

/** Переводит spatial-orbital Hamiltonian в spin-orbital fermion operator. */
public final class ElectronicHamiltonianFermionMapper {

  private static final int ALPHA_SPIN = 0;
  private static final int BETA_SPIN = 1;
  private static final int TWO_ELECTRON_SYMMETRY_LIMIT = 8;
  private static final double TWO_ELECTRON_FACTOR = 0.5;
  private final FermionOperatorAlgebra algebra;

  public ElectronicHamiltonianFermionMapper() {
    this(new FermionOperatorAlgebra());
  }

  public ElectronicHamiltonianFermionMapper(final FermionOperatorAlgebra algebra) {
    if (algebra == null) {
      throw new IllegalArgumentException(
          "Electronic Hamiltonian fermion mapper algebra must not be null.");
    }
    this.algebra = algebra;
  }

  public FermionOperator map(final ElectronicHamiltonian hamiltonian) {
    if (hamiltonian == null) {
      throw new IllegalArgumentException("Electronic Hamiltonian must not be null.");
    }
    final FermionOperator.Builder builder = FermionOperator.builder();
    builder.add(FermionTerm.IDENTITY, hamiltonian.nuclearRepulsionEnergy());
    this.addOneElectronTerms(builder, hamiltonian.oneElectronIntegrals());
    this.addTwoElectronTerms(builder, hamiltonian.twoElectronIntegrals());
    return builder.build();
  }

  public FermionOperator mapNormalOrdered(final ElectronicHamiltonian hamiltonian) {
    return this.algebra.normalOrder(this.map(hamiltonian));
  }

  private void addOneElectronTerms(
      final FermionOperator.Builder builder, final List<OneElectronIntegral> integrals) {
    for (int i = 0; i < integrals.size(); ++i) {
      final OneElectronIntegral integral = integrals.get(i);
      this.addOneElectronSpinTerm(builder, integral, ALPHA_SPIN);
      this.addOneElectronSpinTerm(builder, integral, BETA_SPIN);
    }
  }

  private void addOneElectronSpinTerm(
      final FermionOperator.Builder builder, final OneElectronIntegral integral, final int spin) {
    this.addOneElectronSpinTerm(
        builder,
        integral.firstOrbital().value(),
        integral.secondOrbital().value(),
        spin,
        integral.value());
    if (integral.firstOrbital().value() != integral.secondOrbital().value()) {
      this.addOneElectronSpinTerm(
          builder,
          integral.secondOrbital().value(),
          integral.firstOrbital().value(),
          spin,
          integral.value());
    }
  }

  private void addOneElectronSpinTerm(
      final FermionOperator.Builder builder,
      final int firstSpatialOrbital,
      final int secondSpatialOrbital,
      final int spin,
      final double value) {
    final int first = ElectronicHamiltonianFermionMapper.spinOrbital(firstSpatialOrbital, spin);
    final int second = ElectronicHamiltonianFermionMapper.spinOrbital(secondSpatialOrbital, spin);
    builder.add(
        FermionTerm.of(FermionTerm.creation(first), FermionTerm.annihilation(second)), value);
  }

  private void addTwoElectronTerms(
      final FermionOperator.Builder builder, final List<TwoElectronIntegral> integrals) {
    for (int i = 0; i < integrals.size(); ++i) {
      this.addTwoElectronSymmetryTerms(builder, integrals.get(i));
    }
  }

  private void addTwoElectronSymmetryTerms(
      final FermionOperator.Builder builder, final TwoElectronIntegral integral) {
    final int[] first = new int[TWO_ELECTRON_SYMMETRY_LIMIT];
    final int[] second = new int[TWO_ELECTRON_SYMMETRY_LIMIT];
    final int[] third = new int[TWO_ELECTRON_SYMMETRY_LIMIT];
    final int[] fourth = new int[TWO_ELECTRON_SYMMETRY_LIMIT];
    int count = 0;
    count =
        ElectronicHamiltonianFermionMapper.addUniqueTwoElectronSlot(
            first,
            second,
            third,
            fourth,
            count,
            integral.firstOrbital().value(),
            integral.secondOrbital().value(),
            integral.thirdOrbital().value(),
            integral.fourthOrbital().value());
    count =
        ElectronicHamiltonianFermionMapper.addUniqueTwoElectronSlot(
            first,
            second,
            third,
            fourth,
            count,
            integral.secondOrbital().value(),
            integral.firstOrbital().value(),
            integral.thirdOrbital().value(),
            integral.fourthOrbital().value());
    count =
        ElectronicHamiltonianFermionMapper.addUniqueTwoElectronSlot(
            first,
            second,
            third,
            fourth,
            count,
            integral.firstOrbital().value(),
            integral.secondOrbital().value(),
            integral.fourthOrbital().value(),
            integral.thirdOrbital().value());
    count =
        ElectronicHamiltonianFermionMapper.addUniqueTwoElectronSlot(
            first,
            second,
            third,
            fourth,
            count,
            integral.secondOrbital().value(),
            integral.firstOrbital().value(),
            integral.fourthOrbital().value(),
            integral.thirdOrbital().value());
    count =
        ElectronicHamiltonianFermionMapper.addUniqueTwoElectronSlot(
            first,
            second,
            third,
            fourth,
            count,
            integral.thirdOrbital().value(),
            integral.fourthOrbital().value(),
            integral.firstOrbital().value(),
            integral.secondOrbital().value());
    count =
        ElectronicHamiltonianFermionMapper.addUniqueTwoElectronSlot(
            first,
            second,
            third,
            fourth,
            count,
            integral.fourthOrbital().value(),
            integral.thirdOrbital().value(),
            integral.firstOrbital().value(),
            integral.secondOrbital().value());
    count =
        ElectronicHamiltonianFermionMapper.addUniqueTwoElectronSlot(
            first,
            second,
            third,
            fourth,
            count,
            integral.thirdOrbital().value(),
            integral.fourthOrbital().value(),
            integral.secondOrbital().value(),
            integral.firstOrbital().value());
    count =
        ElectronicHamiltonianFermionMapper.addUniqueTwoElectronSlot(
            first,
            second,
            third,
            fourth,
            count,
            integral.fourthOrbital().value(),
            integral.thirdOrbital().value(),
            integral.secondOrbital().value(),
            integral.firstOrbital().value());
    for (int i = 0; i < count; ++i) {
      this.addTwoElectronSpinTerms(
          builder, first[i], second[i], third[i], fourth[i], integral.value());
    }
  }

  private void addTwoElectronSpinTerms(
      final FermionOperator.Builder builder,
      final int firstSpatialOrbital,
      final int secondSpatialOrbital,
      final int thirdSpatialOrbital,
      final int fourthSpatialOrbital,
      final double value) {
    this.addTwoElectronSpinTerm(
        builder,
        firstSpatialOrbital,
        secondSpatialOrbital,
        thirdSpatialOrbital,
        fourthSpatialOrbital,
        ALPHA_SPIN,
        ALPHA_SPIN,
        value);
    this.addTwoElectronSpinTerm(
        builder,
        firstSpatialOrbital,
        secondSpatialOrbital,
        thirdSpatialOrbital,
        fourthSpatialOrbital,
        ALPHA_SPIN,
        BETA_SPIN,
        value);
    this.addTwoElectronSpinTerm(
        builder,
        firstSpatialOrbital,
        secondSpatialOrbital,
        thirdSpatialOrbital,
        fourthSpatialOrbital,
        BETA_SPIN,
        ALPHA_SPIN,
        value);
    this.addTwoElectronSpinTerm(
        builder,
        firstSpatialOrbital,
        secondSpatialOrbital,
        thirdSpatialOrbital,
        fourthSpatialOrbital,
        BETA_SPIN,
        BETA_SPIN,
        value);
  }

  private void addTwoElectronSpinTerm(
      final FermionOperator.Builder builder,
      final int firstSpatialOrbital,
      final int secondSpatialOrbital,
      final int thirdSpatialOrbital,
      final int fourthSpatialOrbital,
      final int firstSpin,
      final int secondSpin,
      final double value) {
    final int p = ElectronicHamiltonianFermionMapper.spinOrbital(firstSpatialOrbital, firstSpin);
    final int q = ElectronicHamiltonianFermionMapper.spinOrbital(secondSpatialOrbital, firstSpin);
    final int r = ElectronicHamiltonianFermionMapper.spinOrbital(thirdSpatialOrbital, secondSpin);
    final int s = ElectronicHamiltonianFermionMapper.spinOrbital(fourthSpatialOrbital, secondSpin);
    if (p == r || q == s) {
      return;
    }
    builder.add(
        FermionTerm.of(
            FermionTerm.creation(p),
            FermionTerm.creation(r),
            FermionTerm.annihilation(s),
            FermionTerm.annihilation(q)),
        value * TWO_ELECTRON_FACTOR);
  }

  private static int addUniqueTwoElectronSlot(
      final int[] first,
      final int[] second,
      final int[] third,
      final int[] fourth,
      final int count,
      final int firstOrbital,
      final int secondOrbital,
      final int thirdOrbital,
      final int fourthOrbital) {
    for (int i = 0; i < count; ++i) {
      if (first[i] == firstOrbital
          && second[i] == secondOrbital
          && third[i] == thirdOrbital
          && fourth[i] == fourthOrbital) {
        return count;
      }
    }
    first[count] = firstOrbital;
    second[count] = secondOrbital;
    third[count] = thirdOrbital;
    fourth[count] = fourthOrbital;
    return count + 1;
  }

  private static int spinOrbital(final int spatialOrbital, final int spin) {
    return Math.addExact(Math.multiplyExact(spatialOrbital, 2), spin);
  }
}

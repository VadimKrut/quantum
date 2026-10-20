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
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.ContractedGaussianOrbital;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianBasisSet;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianPrimitive;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralAtom;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralMolecule;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.DipoleIntegralMatrices;

public final class GaussianDipoleIntegralEngine {

  private final CartesianGaussianPrimitiveIntegralKernel primitiveKernel =
      new CartesianGaussianPrimitiveIntegralKernel();

  public DipoleIntegralMatrices evaluate(
      final IntegralMolecule molecule, final GaussianBasisSet basisSet) {
    if (molecule == null) {
      throw new IllegalArgumentException("Dipole integral molecule must not be null.");
    }
    if (basisSet == null) {
      throw new IllegalArgumentException("Dipole integral basis set must not be null.");
    }
    final String unsupportedReason =
        GaussianIntegralCapability.unsupportedOneElectronReason(basisSet);
    if (unsupportedReason != null) {
      throw new IllegalArgumentException(unsupportedReason);
    }
    final int orbitalCount = basisSet.orbitalCount();
    final int matrixSize = Math.multiplyExact(orbitalCount, orbitalCount);
    final double[] x = new double[matrixSize];
    final double[] y = new double[matrixSize];
    final double[] z = new double[matrixSize];
    final List<ContractedGaussianOrbital> orbitals = basisSet.orbitals();
    for (int row = 0; row < orbitalCount; ++row) {
      final ContractedGaussianOrbital first = orbitals.get(row);
      for (int column = 0; column <= row; ++column) {
        final ContractedGaussianOrbital second = orbitals.get(column);
        final int lowerIndex = row * orbitalCount + column;
        final int upperIndex = column * orbitalCount + row;
        x[lowerIndex] = this.dipoleMoment(0, first, second, molecule);
        y[lowerIndex] = this.dipoleMoment(1, first, second, molecule);
        z[lowerIndex] = this.dipoleMoment(2, first, second, molecule);
        x[upperIndex] = x[lowerIndex];
        y[upperIndex] = y[lowerIndex];
        z[upperIndex] = z[lowerIndex];
      }
    }
    return DipoleIntegralMatrices.of(orbitalCount, x, y, z);
  }

  private double dipoleMoment(
      final int axis,
      final ContractedGaussianOrbital first,
      final ContractedGaussianOrbital second,
      final IntegralMolecule molecule) {
    double value = 0.0;
    final IntegralAtom firstAtom = molecule.atoms().get(first.atomIndex());
    final IntegralAtom secondAtom = molecule.atoms().get(second.atomIndex());
    final List<GaussianPrimitive> firstPrimitives = first.primitives();
    final List<GaussianPrimitive> secondPrimitives = second.primitives();
    for (int i = 0; i < firstPrimitives.size(); ++i) {
      final GaussianPrimitive firstPrimitive = firstPrimitives.get(i);
      for (int j = 0; j < secondPrimitives.size(); ++j) {
        final GaussianPrimitive secondPrimitive = secondPrimitives.get(j);
        value +=
            firstPrimitive.coefficient()
                * secondPrimitive.coefficient()
                * this.primitiveKernel.dipoleMoment(
                    axis,
                    firstPrimitive.exponent(),
                    first.angularMomentum(),
                    firstAtom,
                    secondPrimitive.exponent(),
                    second.angularMomentum(),
                    secondAtom);
      }
    }
    return value;
  }
}

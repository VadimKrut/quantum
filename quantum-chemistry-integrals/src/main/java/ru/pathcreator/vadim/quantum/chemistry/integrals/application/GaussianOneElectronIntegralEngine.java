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
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.OneElectronIntegralMatrices;

/** Рассчитывает one-electron AO матрицы для Cartesian Gaussian s/p/d/f/g basis. */
public final class GaussianOneElectronIntegralEngine {

  private final CartesianGaussianPrimitiveIntegralKernel primitiveKernel =
      new CartesianGaussianPrimitiveIntegralKernel();

  public OneElectronIntegralMatrices evaluate(
      final IntegralMolecule molecule, final GaussianBasisSet basisSet) {
    if (molecule == null) {
      throw new IllegalArgumentException("Integral molecule must not be null.");
    }
    if (basisSet == null) {
      throw new IllegalArgumentException("Gaussian basis set must not be null.");
    }
    final String unsupportedReason =
        GaussianIntegralCapability.unsupportedOneElectronReason(basisSet);
    if (unsupportedReason != null) {
      throw new IllegalArgumentException(unsupportedReason);
    }
    final int orbitalCount = basisSet.orbitalCount();
    final int matrixSize = Math.multiplyExact(orbitalCount, orbitalCount);
    final double[] overlap = new double[matrixSize];
    final double[] kinetic = new double[matrixSize];
    final double[] nuclearAttraction = new double[matrixSize];
    final List<ContractedGaussianOrbital> orbitals = basisSet.orbitals();
    for (int p = 0; p < orbitalCount; ++p) {
      final ContractedGaussianOrbital first = orbitals.get(p);
      for (int q = 0; q <= p; ++q) {
        final ContractedGaussianOrbital second = orbitals.get(q);
        final int lowerIndex = p * orbitalCount + q;
        final int upperIndex = q * orbitalCount + p;
        overlap[lowerIndex] = this.overlap(first, second, molecule);
        kinetic[lowerIndex] = this.kinetic(first, second, molecule);
        nuclearAttraction[lowerIndex] = this.nuclearAttraction(first, second, molecule);
        overlap[upperIndex] = overlap[lowerIndex];
        kinetic[upperIndex] = kinetic[lowerIndex];
        nuclearAttraction[upperIndex] = nuclearAttraction[lowerIndex];
      }
    }
    return OneElectronIntegralMatrices.of(
        orbitalCount, molecule.nuclearRepulsionEnergy(), overlap, kinetic, nuclearAttraction);
  }

  private double overlap(
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
                * this.primitiveKernel.overlap(
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

  private double kinetic(
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
                * this.primitiveKernel.kinetic(
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

  private double nuclearAttraction(
      final ContractedGaussianOrbital first,
      final ContractedGaussianOrbital second,
      final IntegralMolecule molecule) {
    double value = 0.0;
    final IntegralAtom firstAtom = molecule.atoms().get(first.atomIndex());
    final IntegralAtom secondAtom = molecule.atoms().get(second.atomIndex());
    final List<IntegralAtom> nuclei = molecule.atoms();
    final List<GaussianPrimitive> firstPrimitives = first.primitives();
    final List<GaussianPrimitive> secondPrimitives = second.primitives();
    for (int i = 0; i < firstPrimitives.size(); ++i) {
      final GaussianPrimitive firstPrimitive = firstPrimitives.get(i);
      for (int j = 0; j < secondPrimitives.size(); ++j) {
        final GaussianPrimitive secondPrimitive = secondPrimitives.get(j);
        for (int k = 0; k < nuclei.size(); ++k) {
          value +=
              firstPrimitive.coefficient()
                  * secondPrimitive.coefficient()
                  * this.primitiveKernel.nuclearAttraction(
                      firstPrimitive.exponent(),
                      first.angularMomentum(),
                      firstAtom,
                      secondPrimitive.exponent(),
                      second.angularMomentum(),
                      secondAtom,
                      nuclei.get(k));
        }
      }
    }
    return value;
  }
}

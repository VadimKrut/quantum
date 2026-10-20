/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.integrals.application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.ContractedGaussianOrbital;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianBasisSet;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianPrimitive;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralAtom;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralMolecule;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.MolecularIntegralTensors;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.OneElectronIntegralMatrices;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.PackedElectronRepulsionTensor;

/**
 * Analytic Gaussian integral engine using primitive arrays in all hot loops.
 */
public final class GaussianIntegralEngine {

  private static final double DEFAULT_ERI_SCREENING_TOLERANCE = 1.0E-13;

  private final CartesianGaussianPrimitiveIntegralKernel primitiveKernel =
      new CartesianGaussianPrimitiveIntegralKernel();

  public MolecularIntegralTensors evaluate(
      final IntegralMolecule molecule, final GaussianBasisSet basisSet) {
    if (molecule == null) {
      throw new IllegalArgumentException("Integral molecule must not be null.");
    }
    if (basisSet == null) {
      throw new IllegalArgumentException("Gaussian basis set must not be null.");
    }
    final String unsupportedReason =
        GaussianIntegralCapability.unsupportedFullTensorReason(basisSet);
    if (unsupportedReason != null) {
      throw new IllegalArgumentException(unsupportedReason);
    }
    final int orbitalCount = basisSet.orbitalCount();
    final int matrixSize = Math.multiplyExact(orbitalCount, orbitalCount);
    final double[] overlap = new double[matrixSize];
    final double[] kinetic = new double[matrixSize];
    final double[] nuclearAttraction = new double[matrixSize];
    final PackedElectronRepulsionTensor.Builder electronRepulsionBuilder =
        PackedElectronRepulsionTensor.builder(orbitalCount);
    final List<ContractedGaussianOrbital> orbitals = basisSet.orbitals();
    for (int p = 0; p < orbitalCount; ++p) {
      final PrimitiveIntegralCache primitiveCache = new PrimitiveIntegralCache();
      final ContractedGaussianOrbital first = orbitals.get(p);
      for (int q = 0; q < orbitalCount; ++q) {
        final ContractedGaussianOrbital second = orbitals.get(q);
        final int matrixIndex = p * orbitalCount + q;
        overlap[matrixIndex] = this.overlap(first, second, molecule);
        kinetic[matrixIndex] = this.kinetic(first, second, molecule);
        nuclearAttraction[matrixIndex] =
            this.nuclearAttraction(first, second, molecule, primitiveCache);
      }
    }
    final double[] pairBounds = this.electronRepulsionPairBounds(orbitals, molecule, orbitalCount);
    IntStream.range(0, orbitalCount)
        .parallel()
        .forEach(
            p -> {
              final PrimitiveIntegralCache primitiveCache = new PrimitiveIntegralCache();
              final ContractedGaussianOrbital first = orbitals.get(p);
              for (int q = 0; q <= p; ++q) {
                final ContractedGaussianOrbital second = orbitals.get(q);
                final int firstPairIndex = GaussianIntegralEngine.pairIndex(p, q);
                for (int r = 0; r < orbitalCount; ++r) {
                  final ContractedGaussianOrbital third = orbitals.get(r);
                  for (int s = 0; s <= r; ++s) {
                    final int secondPairIndex = GaussianIntegralEngine.pairIndex(r, s);
                    if (secondPairIndex > firstPairIndex) {
                      continue;
                    }
                    final ContractedGaussianOrbital fourth = orbitals.get(s);
                    final double value =
                        GaussianIntegralEngine.screenedOut(
                                pairBounds, firstPairIndex, secondPairIndex)
                            ? 0.0
                            : this.electronRepulsion(
                                first, second, third, fourth, molecule, primitiveCache);
                    electronRepulsionBuilder.set(p, q, r, s, value);
                  }
                }
              }
            });
    return MolecularIntegralTensors.ofPacked(
        orbitalCount,
        molecule.nuclearRepulsionEnergy(),
        overlap,
        kinetic,
        nuclearAttraction,
        electronRepulsionBuilder.build());
  }

  public OneElectronIntegralMatrices evaluateOneElectron(
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
      final PrimitiveIntegralCache primitiveCache = new PrimitiveIntegralCache();
      final ContractedGaussianOrbital first = orbitals.get(p);
      for (int q = 0; q <= p; ++q) {
        final ContractedGaussianOrbital second = orbitals.get(q);
        final int lowerIndex = p * orbitalCount + q;
        final int upperIndex = q * orbitalCount + p;
        overlap[lowerIndex] = this.overlap(first, second, molecule);
        kinetic[lowerIndex] = this.kinetic(first, second, molecule);
        nuclearAttraction[lowerIndex] =
            this.nuclearAttraction(first, second, molecule, primitiveCache);
        overlap[upperIndex] = overlap[lowerIndex];
        kinetic[upperIndex] = kinetic[lowerIndex];
        nuclearAttraction[upperIndex] = nuclearAttraction[lowerIndex];
      }
    }
    return OneElectronIntegralMatrices.of(
        orbitalCount, molecule.nuclearRepulsionEnergy(), overlap, kinetic, nuclearAttraction);
  }

  public double electronRepulsionAt(
      final IntegralMolecule molecule,
      final GaussianBasisSet basisSet,
      final int firstOrbital,
      final int secondOrbital,
      final int thirdOrbital,
      final int fourthOrbital) {
    if (molecule == null) {
      throw new IllegalArgumentException("Integral molecule must not be null.");
    }
    if (basisSet == null) {
      throw new IllegalArgumentException("Gaussian basis set must not be null.");
    }
    final List<ContractedGaussianOrbital> orbitals = basisSet.orbitals();
    GaussianIntegralEngine.requireOrbital(firstOrbital, orbitals.size());
    GaussianIntegralEngine.requireOrbital(secondOrbital, orbitals.size());
    GaussianIntegralEngine.requireOrbital(thirdOrbital, orbitals.size());
    GaussianIntegralEngine.requireOrbital(fourthOrbital, orbitals.size());
    return this.electronRepulsion(
        orbitals.get(firstOrbital),
        orbitals.get(secondOrbital),
        orbitals.get(thirdOrbital),
        orbitals.get(fourthOrbital),
        molecule,
        new PrimitiveIntegralCache());
  }

  private double[] electronRepulsionPairBounds(
      final List<ContractedGaussianOrbital> orbitals,
      final IntegralMolecule molecule,
      final int orbitalCount) {
    final int pairCount = orbitalCount * (orbitalCount + 1) / 2;
    final double[] pairBounds = new double[pairCount];
    IntStream.range(0, orbitalCount)
        .parallel()
        .forEach(
            p -> {
              final PrimitiveIntegralCache primitiveCache = new PrimitiveIntegralCache();
              final ContractedGaussianOrbital first = orbitals.get(p);
              for (int q = 0; q <= p; ++q) {
                final ContractedGaussianOrbital second = orbitals.get(q);
                final double diagonal =
                    this.electronRepulsion(first, second, first, second, molecule, primitiveCache);
                pairBounds[GaussianIntegralEngine.pairIndex(p, q)] =
                    Math.sqrt(Math.max(0.0, Math.abs(diagonal)));
              }
            });
    return pairBounds;
  }

  private static boolean screenedOut(
      final double[] pairBounds, final int firstPairIndex, final int secondPairIndex) {
    return pairBounds[firstPairIndex] * pairBounds[secondPairIndex]
        < DEFAULT_ERI_SCREENING_TOLERANCE;
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
      final IntegralMolecule molecule,
      final PrimitiveIntegralCache primitiveCache) {
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
          final IntegralAtom nucleus = nuclei.get(k);
          final PrimitiveNuclearKey key =
              PrimitiveNuclearKey.of(first, second, firstPrimitive, secondPrimitive, k);
          value +=
              firstPrimitive.coefficient()
                  * secondPrimitive.coefficient()
                  * primitiveCache.nuclearAttraction(
                      key,
                      () ->
                          this.primitiveKernel.nuclearAttraction(
                              firstPrimitive.exponent(),
                              first.angularMomentum(),
                              firstAtom,
                              secondPrimitive.exponent(),
                              second.angularMomentum(),
                              secondAtom,
                              nucleus));
        }
      }
    }
    return value;
  }

  private double electronRepulsion(
      final ContractedGaussianOrbital first,
      final ContractedGaussianOrbital second,
      final ContractedGaussianOrbital third,
      final ContractedGaussianOrbital fourth,
      final IntegralMolecule molecule,
      final PrimitiveIntegralCache primitiveCache) {
    double value = 0.0;
    final IntegralAtom firstAtom = molecule.atoms().get(first.atomIndex());
    final IntegralAtom secondAtom = molecule.atoms().get(second.atomIndex());
    final IntegralAtom thirdAtom = molecule.atoms().get(third.atomIndex());
    final IntegralAtom fourthAtom = molecule.atoms().get(fourth.atomIndex());
    final List<GaussianPrimitive> firstPrimitives = first.primitives();
    final List<GaussianPrimitive> secondPrimitives = second.primitives();
    final List<GaussianPrimitive> thirdPrimitives = third.primitives();
    final List<GaussianPrimitive> fourthPrimitives = fourth.primitives();
    for (int i = 0; i < firstPrimitives.size(); ++i) {
      final GaussianPrimitive firstPrimitive = firstPrimitives.get(i);
      for (int j = 0; j < secondPrimitives.size(); ++j) {
        final GaussianPrimitive secondPrimitive = secondPrimitives.get(j);
        for (int k = 0; k < thirdPrimitives.size(); ++k) {
          final GaussianPrimitive thirdPrimitive = thirdPrimitives.get(k);
          for (int l = 0; l < fourthPrimitives.size(); ++l) {
            final GaussianPrimitive fourthPrimitive = fourthPrimitives.get(l);
            value +=
                firstPrimitive.coefficient()
                    * secondPrimitive.coefficient()
                    * thirdPrimitive.coefficient()
                    * fourthPrimitive.coefficient()
                    * this.primitiveKernel.electronRepulsion(
                        firstPrimitive.exponent(),
                        first.angularMomentum(),
                        firstAtom,
                        secondPrimitive.exponent(),
                        second.angularMomentum(),
                        secondAtom,
                        thirdPrimitive.exponent(),
                        third.angularMomentum(),
                        thirdAtom,
                        fourthPrimitive.exponent(),
                        fourth.angularMomentum(),
                        fourthAtom);
          }
        }
      }
    }
    return value;
  }

  public static String unsupportedAngularMomentumReason(final GaussianBasisSet basisSet) {
    if (basisSet == null) {
      throw new IllegalArgumentException("Gaussian basis set must not be null.");
    }
    return GaussianIntegralCapability.unsupportedFullTensorReason(basisSet);
  }

  private static void requireOrbital(final int orbital, final int orbitalCount) {
    if (orbital < 0 || orbital >= orbitalCount) {
      throw new IllegalArgumentException("Gaussian basis orbital index is out of bounds.");
    }
  }

  private static double distanceSquared(final IntegralAtom first, final IntegralAtom second) {
    return GaussianIntegralEngine.distanceSquared(
        first.xBohr(),
        first.yBohr(),
        first.zBohr(),
        second.xBohr(),
        second.yBohr(),
        second.zBohr());
  }

  private static double distanceSquared(
      final double firstX,
      final double firstY,
      final double firstZ,
      final double secondX,
      final double secondY,
      final double secondZ) {
    final double dx = firstX - secondX;
    final double dy = firstY - secondY;
    final double dz = firstZ - secondZ;
    return dx * dx + dy * dy + dz * dz;
  }

  private static int pairIndex(final int first, final int second) {
    return first * (first + 1) / 2 + second;
  }

  private interface PrimitiveIntegralComputer {

    double compute();
  }

  private static final class PrimitiveIntegralCache {

    private final Map<PrimitiveNuclearKey, Double> nuclearAttractionByKey = new HashMap<>(512);

    double nuclearAttraction(
        final PrimitiveNuclearKey key, final PrimitiveIntegralComputer computer) {
      final Double cached = this.nuclearAttractionByKey.get(key);
      if (cached != null) {
        return cached.doubleValue();
      }
      final double value = computer.compute();
      this.nuclearAttractionByKey.put(key, Double.valueOf(value));
      return value;
    }
  }

  private static final class PrimitiveNuclearKey {

    private final int firstAtomIndex;
    private final int secondAtomIndex;
    private final int nucleusIndex;
    private final int firstAngularCode;
    private final int secondAngularCode;
    private final long firstExponentBits;
    private final long secondExponentBits;

    private PrimitiveNuclearKey(
        final int firstAtomIndex,
        final int secondAtomIndex,
        final int nucleusIndex,
        final int firstAngularCode,
        final int secondAngularCode,
        final long firstExponentBits,
        final long secondExponentBits) {
      this.firstAtomIndex = firstAtomIndex;
      this.secondAtomIndex = secondAtomIndex;
      this.nucleusIndex = nucleusIndex;
      this.firstAngularCode = firstAngularCode;
      this.secondAngularCode = secondAngularCode;
      this.firstExponentBits = firstExponentBits;
      this.secondExponentBits = secondExponentBits;
    }

    static PrimitiveNuclearKey of(
        final ContractedGaussianOrbital first,
        final ContractedGaussianOrbital second,
        final GaussianPrimitive firstPrimitive,
        final GaussianPrimitive secondPrimitive,
        final int nucleusIndex) {
      return new PrimitiveNuclearKey(
          first.atomIndex(),
          second.atomIndex(),
          nucleusIndex,
          GaussianIntegralEngine.angularCode(first),
          GaussianIntegralEngine.angularCode(second),
          Double.doubleToLongBits(firstPrimitive.exponent()),
          Double.doubleToLongBits(secondPrimitive.exponent()));
    }

    public boolean equals(final Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof PrimitiveNuclearKey)) {
        return false;
      }
      final PrimitiveNuclearKey key = (PrimitiveNuclearKey) other;
      return this.firstAtomIndex == key.firstAtomIndex
          && this.secondAtomIndex == key.secondAtomIndex
          && this.nucleusIndex == key.nucleusIndex
          && this.firstAngularCode == key.firstAngularCode
          && this.secondAngularCode == key.secondAngularCode
          && this.firstExponentBits == key.firstExponentBits
          && this.secondExponentBits == key.secondExponentBits;
    }

    public int hashCode() {
      int result = Integer.hashCode(this.firstAtomIndex);
      result = 31 * result + Integer.hashCode(this.secondAtomIndex);
      result = 31 * result + Integer.hashCode(this.nucleusIndex);
      result = 31 * result + Integer.hashCode(this.firstAngularCode);
      result = 31 * result + Integer.hashCode(this.secondAngularCode);
      result = 31 * result + Long.hashCode(this.firstExponentBits);
      result = 31 * result + Long.hashCode(this.secondExponentBits);
      return result;
    }
  }

  private static int angularCode(final ContractedGaussianOrbital orbital) {
    return orbital.angularMomentum().xPower()
        | (orbital.angularMomentum().yPower() << 8)
        | (orbital.angularMomentum().zPower() << 16);
  }
}

/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.integrals.application;

import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.MolecularIntegralTensors;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.OneElectronIntegralMatrices;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.PackedElectronRepulsionTensor;

/** Строит leading MO-window integrals через direct AO ERI accessor без полного AO two-electron tensor. */
public final class DirectMolecularOrbitalWindowTransformer {

  private static final double COEFFICIENT_SCREENING_TOLERANCE = 1.0E-14;
  private static final double PAIR_WEIGHT_BOUND_TOLERANCE = 1.0E-15;
  private static final double CONTRACT_CONTRIBUTION_BOUND_TOLERANCE = 1.0E-14;

  public MolecularIntegralTensors transformLeadingOrbitalWindow(
      final OneElectronIntegralMatrices matrices,
      final double[] molecularOrbitalCoefficients,
      final int orbitalWindowCount,
      final AoElectronRepulsionValues electronRepulsionValues) {
    return this.transformOrbitalWindow(
        matrices, molecularOrbitalCoefficients, 0, orbitalWindowCount, electronRepulsionValues);
  }

  public MolecularIntegralTensors transformOrbitalWindow(
      final OneElectronIntegralMatrices matrices,
      final double[] molecularOrbitalCoefficients,
      final int firstOrbital,
      final int orbitalWindowCount,
      final AoElectronRepulsionValues electronRepulsionValues) {
    if (matrices == null) {
      throw new IllegalArgumentException(
          "Direct MO transform one-electron matrices must not be null.");
    }
    if (electronRepulsionValues == null) {
      throw new IllegalArgumentException("Direct MO transform ERI accessor must not be null.");
    }
    final int sourceSize = matrices.orbitalCount();
    if (firstOrbital < 0 || firstOrbital >= sourceSize) {
      throw new IllegalArgumentException("Direct MO first orbital must be in [0, orbital count).");
    }
    if (orbitalWindowCount <= 0 || Math.addExact(firstOrbital, orbitalWindowCount) > sourceSize) {
      throw new IllegalArgumentException(
          "Direct MO orbital window must fit into source orbital count.");
    }
    DirectMolecularOrbitalWindowTransformer.requireCoefficientMatrix(
        molecularOrbitalCoefficients, Math.multiplyExact(sourceSize, sourceSize));
    final double[] overlap =
        DirectMolecularOrbitalWindowTransformer.transformOneElectronWindow(
            sourceSize,
            firstOrbital,
            orbitalWindowCount,
            matrices.overlapCopy(),
            molecularOrbitalCoefficients);
    final double[] core =
        DirectMolecularOrbitalWindowTransformer.transformOneElectronWindow(
            sourceSize,
            firstOrbital,
            orbitalWindowCount,
            matrices.coreHamiltonianCopy(),
            molecularOrbitalCoefficients);
    final double[] zero = new double[Math.multiplyExact(orbitalWindowCount, orbitalWindowCount)];
    final PackedElectronRepulsionTensor electronRepulsion =
        DirectMolecularOrbitalWindowTransformer.transformTwoElectronWindow(
            sourceSize,
            firstOrbital,
            orbitalWindowCount,
            molecularOrbitalCoefficients,
            electronRepulsionValues);
    return MolecularIntegralTensors.ofPacked(
        orbitalWindowCount,
        matrices.nuclearRepulsionEnergy(),
        overlap,
        core,
        zero,
        electronRepulsion);
  }

  private static double[] transformOneElectronWindow(
      final int sourceSize,
      final int firstOrbital,
      final int targetSize,
      final double[] atomic,
      final double[] coefficients) {
    final double[] temporary = new double[Math.multiplyExact(sourceSize, targetSize)];
    final double[] transformed = new double[Math.multiplyExact(targetSize, targetSize)];
    for (int mu = 0; mu < sourceSize; ++mu) {
      for (int q = 0; q < targetSize; ++q) {
        final int sourceQ = firstOrbital + q;
        double value = 0.0;
        for (int nu = 0; nu < sourceSize; ++nu) {
          value += atomic[mu * sourceSize + nu] * coefficients[nu * sourceSize + sourceQ];
        }
        temporary[mu * targetSize + q] = value;
      }
    }
    for (int p = 0; p < targetSize; ++p) {
      final int sourceP = firstOrbital + p;
      for (int q = 0; q < targetSize; ++q) {
        double value = 0.0;
        for (int mu = 0; mu < sourceSize; ++mu) {
          value += coefficients[mu * sourceSize + sourceP] * temporary[mu * targetSize + q];
        }
        transformed[p * targetSize + q] = value;
      }
    }
    return transformed;
  }

  private static PackedElectronRepulsionTensor transformTwoElectronWindow(
      final int sourceSize,
      final int firstOrbital,
      final int targetSize,
      final double[] coefficients,
      final AoElectronRepulsionValues electronRepulsionValues) {
    final SparseAoPairWeights[] pairWeights =
        DirectMolecularOrbitalWindowTransformer.buildPairWeights(
            sourceSize, firstOrbital, targetSize, coefficients, electronRepulsionValues);
    final PackedElectronRepulsionTensor.Builder transformed =
        PackedElectronRepulsionTensor.builder(targetSize);
    for (int p = 0; p < targetSize; ++p) {
      for (int q = 0; q <= p; ++q) {
        final int firstTargetPair = DirectMolecularOrbitalWindowTransformer.pairIndex(p, q);
        for (int r = 0; r < targetSize; ++r) {
          for (int s = 0; s <= r; ++s) {
            final int secondTargetPair = DirectMolecularOrbitalWindowTransformer.pairIndex(r, s);
            if (secondTargetPair > firstTargetPair) {
              continue;
            }
            final double value =
                DirectMolecularOrbitalWindowTransformer.contractPairWeights(
                    pairWeights[firstTargetPair],
                    pairWeights[secondTargetPair],
                    electronRepulsionValues);
            transformed.set(p, q, r, s, value);
          }
        }
      }
    }
    return transformed.build();
  }

  private static SparseAoPairWeights[] buildPairWeights(
      final int sourceSize,
      final int firstOrbital,
      final int targetSize,
      final double[] coefficients,
      final AoElectronRepulsionValues electronRepulsionValues) {
    final int targetPairCount = targetSize * (targetSize + 1) / 2;
    final SparseAoPairWeights[] weights = new SparseAoPairWeights[targetPairCount];
    for (int p = 0; p < targetSize; ++p) {
      final int sourceP = firstOrbital + p;
      for (int q = 0; q <= p; ++q) {
        final int sourceQ = firstOrbital + q;
        final int targetPair = DirectMolecularOrbitalWindowTransformer.pairIndex(p, q);
        final SparseAoPairWeights.Builder builder = new SparseAoPairWeights.Builder(sourceSize);
        for (int mu = 0; mu < sourceSize; ++mu) {
          final double muP = coefficients[mu * sourceSize + sourceP];
          final double muQ = coefficients[mu * sourceSize + sourceQ];
          for (int nu = 0; nu <= mu; ++nu) {
            final double nuP = coefficients[nu * sourceSize + sourceP];
            final double nuQ = coefficients[nu * sourceSize + sourceQ];
            final double weight = mu == nu ? muP * muQ : muP * nuQ + nuP * muQ;
            if (DirectMolecularOrbitalWindowTransformer.keepPairWeight(
                mu, nu, weight, electronRepulsionValues)) {
              builder.add(mu, nu, weight, electronRepulsionValues.pairUpperBound(mu, nu));
            }
          }
        }
        weights[targetPair] = builder.build();
      }
    }
    return weights;
  }

  private static boolean keepPairWeight(
      final int first,
      final int second,
      final double weight,
      final AoElectronRepulsionValues electronRepulsionValues) {
    final double absoluteWeight = Math.abs(weight);
    if (absoluteWeight <= COEFFICIENT_SCREENING_TOLERANCE) {
      return false;
    }
    final double pairBound = electronRepulsionValues.pairUpperBound(first, second);
    return !Double.isFinite(pairBound) || absoluteWeight * pairBound > PAIR_WEIGHT_BOUND_TOLERANCE;
  }

  private static double contractPairWeights(
      final SparseAoPairWeights firstWeights,
      final SparseAoPairWeights secondWeights,
      final AoElectronRepulsionValues electronRepulsionValues) {
    if (firstWeights == secondWeights) {
      return DirectMolecularOrbitalWindowTransformer.contractSymmetricPairWeights(
          firstWeights, electronRepulsionValues);
    }
    double value = 0.0;
    for (int firstIndex = 0; firstIndex < firstWeights.count; ++firstIndex) {
      final int mu = firstWeights.first[firstIndex];
      final int nu = firstWeights.second[firstIndex];
      final double firstWeight = firstWeights.weight[firstIndex];
      final double firstBound = firstWeights.bound[firstIndex];
      for (int secondIndex = 0; secondIndex < secondWeights.count; ++secondIndex) {
        final int lambda = secondWeights.first[secondIndex];
        final int sigma = secondWeights.second[secondIndex];
        if (DirectMolecularOrbitalWindowTransformer.contributionScreenedOut(
            firstWeight,
            firstBound,
            secondWeights.weight[secondIndex],
            secondWeights.bound[secondIndex])) {
          continue;
        }
        if (electronRepulsionValues.screenedOut(mu, nu, lambda, sigma)) {
          continue;
        }
        value +=
            firstWeight
                * secondWeights.weight[secondIndex]
                * electronRepulsionValues.screenedInValueAt(mu, nu, lambda, sigma);
      }
    }
    return value;
  }

  private static double contractSymmetricPairWeights(
      final SparseAoPairWeights weights, final AoElectronRepulsionValues electronRepulsionValues) {
    double value = 0.0;
    for (int firstIndex = 0; firstIndex < weights.count; ++firstIndex) {
      final int mu = weights.first[firstIndex];
      final int nu = weights.second[firstIndex];
      final double firstWeight = weights.weight[firstIndex];
      final double firstBound = weights.bound[firstIndex];
      for (int secondIndex = 0; secondIndex <= firstIndex; ++secondIndex) {
        final int lambda = weights.first[secondIndex];
        final int sigma = weights.second[secondIndex];
        if (DirectMolecularOrbitalWindowTransformer.contributionScreenedOut(
            firstWeight, firstBound, weights.weight[secondIndex], weights.bound[secondIndex])) {
          continue;
        }
        if (electronRepulsionValues.screenedOut(mu, nu, lambda, sigma)) {
          continue;
        }
        final double contribution =
            firstWeight
                * weights.weight[secondIndex]
                * electronRepulsionValues.screenedInValueAt(mu, nu, lambda, sigma);
        value += firstIndex == secondIndex ? contribution : 2.0 * contribution;
      }
    }
    return value;
  }

  private static boolean contributionScreenedOut(
      final double firstWeight,
      final double firstBound,
      final double secondWeight,
      final double secondBound) {
    return Double.isFinite(firstBound)
        && Double.isFinite(secondBound)
        && Math.abs(firstWeight * secondWeight) * firstBound * secondBound
            <= CONTRACT_CONTRIBUTION_BOUND_TOLERANCE;
  }

  private static int pairIndex(final int first, final int second) {
    return first * (first + 1) / 2 + second;
  }

  private static void requireCoefficientMatrix(
      final double[] coefficients, final int expectedLength) {
    if (coefficients == null || coefficients.length != expectedLength) {
      throw new IllegalArgumentException(
          "Direct MO coefficient matrix length must match orbital count.");
    }
    for (int i = 0; i < coefficients.length; ++i) {
      if (!Double.isFinite(coefficients[i])) {
        throw new IllegalArgumentException(
            "Direct MO coefficient matrix contains non-finite value.");
      }
    }
  }

  private static final class SparseAoPairWeights {

    private final int count;
    private final int[] first;
    private final int[] second;
    private final double[] weight;
    private final double[] bound;

    private SparseAoPairWeights(
        final int count,
        final int[] first,
        final int[] second,
        final double[] weight,
        final double[] bound) {
      this.count = count;
      this.first = first;
      this.second = second;
      this.weight = weight;
      this.bound = bound;
    }

    private static final class Builder {

      private int count;
      private int[] first;
      private int[] second;
      private double[] weight;
      private double[] bound;

      private Builder(final int sourceSize) {
        final int initialCapacity = Math.min(64, sourceSize * (sourceSize + 1) / 2);
        this.first = new int[initialCapacity];
        this.second = new int[initialCapacity];
        this.weight = new double[initialCapacity];
        this.bound = new double[initialCapacity];
      }

      private void add(
          final int firstIndex,
          final int secondIndex,
          final double pairWeight,
          final double pairBound) {
        this.ensureCapacity(this.count + 1);
        this.first[this.count] = firstIndex;
        this.second[this.count] = secondIndex;
        this.weight[this.count] = pairWeight;
        this.bound[this.count] = pairBound;
        ++this.count;
      }

      private SparseAoPairWeights build() {
        final int[] compactFirst = new int[this.count];
        final int[] compactSecond = new int[this.count];
        final double[] compactWeight = new double[this.count];
        final double[] compactBound = new double[this.count];
        System.arraycopy(this.first, 0, compactFirst, 0, this.count);
        System.arraycopy(this.second, 0, compactSecond, 0, this.count);
        System.arraycopy(this.weight, 0, compactWeight, 0, this.count);
        System.arraycopy(this.bound, 0, compactBound, 0, this.count);
        return new SparseAoPairWeights(
            this.count, compactFirst, compactSecond, compactWeight, compactBound);
      }

      private void ensureCapacity(final int requiredCapacity) {
        if (requiredCapacity <= this.first.length) {
          return;
        }
        final int newCapacity = Math.max(requiredCapacity, this.first.length * 2);
        final int[] newFirst = new int[newCapacity];
        final int[] newSecond = new int[newCapacity];
        final double[] newWeight = new double[newCapacity];
        final double[] newBound = new double[newCapacity];
        System.arraycopy(this.first, 0, newFirst, 0, this.count);
        System.arraycopy(this.second, 0, newSecond, 0, this.count);
        System.arraycopy(this.weight, 0, newWeight, 0, this.count);
        System.arraycopy(this.bound, 0, newBound, 0, this.count);
        this.first = newFirst;
        this.second = newSecond;
        this.weight = newWeight;
        this.bound = newBound;
      }
    }
  }
}

/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.scf.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.orbital.OrbitalOptimizationOptions;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.orbital.OrbitalOptimizationRecord;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.orbital.OrbitalOptimizationResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.orbital.OrbitalRotationPair;

/**
 * Консервативный optimizer molecular orbitals через S-ортонормальные парные вращения.
 */
public final class OrbitalRotationOptimizer {

  private static final double SINGULAR_NORM_LIMIT = 1.0E-24;

  public OrbitalOptimizationResult optimize(
      final double[] initialCoefficients,
      final double[] overlapMatrix,
      final int orbitalCount,
      final List<OrbitalRotationPair> rotationPairs,
      final OrbitalEnergyEvaluator energyEvaluator,
      final OrbitalOptimizationOptions options) {
    return this.optimizeInternal(
        initialCoefficients,
        overlapMatrix,
        orbitalCount,
        rotationPairs,
        energyEvaluator,
        options,
        false);
  }

  public OrbitalOptimizationResult optimizeGradientRanked(
      final double[] initialCoefficients,
      final double[] overlapMatrix,
      final int orbitalCount,
      final List<OrbitalRotationPair> rotationPairs,
      final OrbitalEnergyEvaluator energyEvaluator,
      final OrbitalOptimizationOptions options) {
    return this.optimizeInternal(
        initialCoefficients,
        overlapMatrix,
        orbitalCount,
        rotationPairs,
        energyEvaluator,
        options,
        true);
  }

  private OrbitalOptimizationResult optimizeInternal(
      final double[] initialCoefficients,
      final double[] overlapMatrix,
      final int orbitalCount,
      final List<OrbitalRotationPair> rotationPairs,
      final OrbitalEnergyEvaluator energyEvaluator,
      final OrbitalOptimizationOptions options,
      final boolean gradientRanked) {
    OrbitalRotationOptimizer.requireSquareMatrix(
        initialCoefficients, orbitalCount, "MO coefficients");
    OrbitalRotationOptimizer.requireSquareMatrix(overlapMatrix, orbitalCount, "AO overlap matrix");
    if (rotationPairs == null || rotationPairs.isEmpty()) {
      throw new IllegalArgumentException(
          "Orbital optimization requires at least one rotation pair.");
    }
    if (energyEvaluator == null) {
      throw new IllegalArgumentException("Orbital optimization energy evaluator must not be null.");
    }
    final OrbitalOptimizationOptions checkedOptions =
        options == null ? OrbitalOptimizationOptions.DEFAULT : options;
    final ArrayList<OrbitalRotationPair> checkedPairs =
        OrbitalRotationOptimizer.checkedPairs(rotationPairs, orbitalCount);
    double[] coefficients = initialCoefficients.clone();
    OrbitalRotationOptimizer.orthonormalizeColumns(
        coefficients, overlapMatrix, orbitalCount, checkedOptions.orthonormalityTolerance());
    double bestEnergy = OrbitalRotationOptimizer.evaluate(energyEvaluator, coefficients);
    final double initialEnergy = bestEnergy;
    double step = checkedOptions.initialStepRadians();
    final int minimumCoverageIterations =
        OrbitalRotationOptimizer.minimumCoverageIterations(
            checkedPairs.size(), checkedOptions.maxPairsPerIteration());
    int evaluatedRotations = 0;
    int acceptedRotations = 0;
    int iterations = 0;
    int iterationsSinceSignificantImprovement = 0;
    int pairCursor = 0;
    boolean converged = false;
    final ArrayList<OrbitalOptimizationRecord> history = new ArrayList<OrbitalOptimizationRecord>();
    ArrayList<OrbitalRotationPair> rankedCyclePairs = new ArrayList<OrbitalRotationPair>();
    while (iterations < checkedOptions.maxIterations()
        && step >= checkedOptions.minimumStepRadians()) {
      ++iterations;
      final double iterationStartEnergy = bestEnergy;
      boolean acceptedInIteration = false;
      int evaluatedPairs = 0;
      final IterationPairSelection pairSelection;
      if (gradientRanked) {
        if (pairCursor == 0 || rankedCyclePairs.isEmpty()) {
          final RankedCycle rankedCycle =
              OrbitalRotationOptimizer.rankedCycle(
                  coefficients,
                  overlapMatrix,
                  orbitalCount,
                  checkedPairs,
                  step,
                  energyEvaluator,
                  checkedOptions.orthonormalityTolerance());
          rankedCyclePairs = rankedCycle.pairs();
          evaluatedRotations += rankedCycle.evaluatedRotationCount();
        }
        pairSelection =
            OrbitalRotationOptimizer.cyclePairSelection(
                rankedCyclePairs, pairCursor, checkedOptions.maxPairsPerIteration());
      } else {
        pairSelection =
            OrbitalRotationOptimizer.cyclePairSelection(
                checkedPairs, pairCursor, checkedOptions.maxPairsPerIteration());
      }
      evaluatedRotations += pairSelection.evaluatedRotationCount();
      pairCursor = pairSelection.nextPairCursor();
      final List<OrbitalRotationPair> iterationPairs = pairSelection.pairs();
      for (int offset = 0; offset < iterationPairs.size(); ++offset) {
        if (evaluatedPairs >= checkedOptions.maxPairsPerIteration()) {
          break;
        }
        ++evaluatedPairs;
        final OrbitalRotationPair pair = iterationPairs.get(offset);
        final PairOptimization pairOptimization =
            OrbitalRotationOptimizer.optimizePair(
                coefficients,
                overlapMatrix,
                orbitalCount,
                pair,
                step,
                bestEnergy,
                energyEvaluator,
                checkedOptions.orthonormalityTolerance());
        evaluatedRotations += pairOptimization.evaluatedRotationCount();
        if (pairOptimization.bestTrial().energy() < bestEnergy) {
          coefficients = pairOptimization.bestTrial().coefficients();
          bestEnergy = pairOptimization.bestTrial().energy();
          ++acceptedRotations;
          acceptedInIteration = true;
        }
      }
      final double deltaEnergy = Math.abs(iterationStartEnergy - bestEnergy);
      if (acceptedInIteration) {
        if (deltaEnergy > checkedOptions.energyTolerance()) {
          iterationsSinceSignificantImprovement = 0;
        } else {
          ++iterationsSinceSignificantImprovement;
        }
      } else {
        ++iterationsSinceSignificantImprovement;
      }
      converged =
          deltaEnergy <= checkedOptions.energyTolerance()
              && iterationsSinceSignificantImprovement >= minimumCoverageIterations;
      boolean residualRefinementRequired = false;
      if (converged) {
        final double convergenceResidualEnergyImprovement =
            OrbitalRotationOptimizer.maxResidualEnergyImprovement(
                coefficients,
                overlapMatrix,
                orbitalCount,
                checkedPairs,
                checkedOptions.minimumStepRadians(),
                energyEvaluator,
                checkedOptions);
        final double convergenceResidualOrbitalGradient =
            OrbitalRotationOptimizer.maxResidualOrbitalGradient(
                coefficients,
                overlapMatrix,
                orbitalCount,
                checkedPairs,
                checkedOptions.minimumStepRadians(),
                energyEvaluator,
                checkedOptions.orthonormalityTolerance());
        converged =
            convergenceResidualEnergyImprovement <= Math.sqrt(checkedOptions.energyTolerance())
                && convergenceResidualOrbitalGradient
                    <= Math.sqrt(checkedOptions.energyTolerance());
        residualRefinementRequired = !converged;
        if (residualRefinementRequired) {
          step *= 0.5;
          iterationsSinceSignificantImprovement = 0;
        }
      }
      history.add(
          OrbitalOptimizationRecord.of(
              iterations,
              evaluatedRotations,
              acceptedRotations,
              step,
              bestEnergy,
              deltaEnergy,
              converged));
      if (converged) {
        break;
      }
      if (!residualRefinementRequired
          && iterationsSinceSignificantImprovement >= minimumCoverageIterations) {
        step *= 0.5;
        iterationsSinceSignificantImprovement = 0;
      }
    }
    final double maxResidualEnergyImprovement =
        OrbitalRotationOptimizer.maxResidualEnergyImprovement(
            coefficients,
            overlapMatrix,
            orbitalCount,
            checkedPairs,
            checkedOptions.minimumStepRadians(),
            energyEvaluator,
            checkedOptions);
    final double maxResidualOrbitalGradient =
        OrbitalRotationOptimizer.maxResidualOrbitalGradient(
            coefficients,
            overlapMatrix,
            orbitalCount,
            checkedPairs,
            checkedOptions.minimumStepRadians(),
            energyEvaluator,
            checkedOptions.orthonormalityTolerance());
    final boolean residualConverged =
        maxResidualEnergyImprovement <= Math.sqrt(checkedOptions.energyTolerance())
            && maxResidualOrbitalGradient <= Math.sqrt(checkedOptions.energyTolerance());
    return OrbitalOptimizationResult.of(
        converged || residualConverged,
        iterations,
        evaluatedRotations,
        acceptedRotations,
        step,
        initialEnergy,
        bestEnergy,
        maxResidualEnergyImprovement,
        maxResidualOrbitalGradient,
        coefficients,
        history);
  }

  public static void rotateColumns(
      final double[] coefficients,
      final int size,
      final int firstColumn,
      final int secondColumn,
      final double angle) {
    if (coefficients == null || coefficients.length != Math.multiplyExact(size, size)) {
      throw new IllegalArgumentException("Orbital coefficient matrix size mismatch.");
    }
    if (firstColumn < 0 || firstColumn >= size || secondColumn < 0 || secondColumn >= size) {
      throw new IllegalArgumentException("Orbital rotation column index is out of bounds.");
    }
    if (firstColumn == secondColumn) {
      throw new IllegalArgumentException("Orbital rotation columns must be different.");
    }
    if (!Double.isFinite(angle)) {
      throw new IllegalArgumentException("Orbital rotation angle must be finite.");
    }
    final double cosine = Math.cos(angle);
    final double sine = Math.sin(angle);
    for (int row = 0; row < size; ++row) {
      final int firstIndex = row * size + firstColumn;
      final int secondIndex = row * size + secondColumn;
      final double first = coefficients[firstIndex];
      final double second = coefficients[secondIndex];
      coefficients[firstIndex] = first * cosine + second * sine;
      coefficients[secondIndex] = second * cosine - first * sine;
    }
  }

  public static void orthonormalizeColumns(
      final double[] coefficients, final double[] overlap, final int size, final double tolerance) {
    OrbitalRotationOptimizer.requireSquareMatrix(coefficients, size, "MO coefficients");
    OrbitalRotationOptimizer.requireSquareMatrix(overlap, size, "AO overlap matrix");
    if (!Double.isFinite(tolerance) || tolerance <= 0.0) {
      throw new IllegalArgumentException("Orbital orthonormalization tolerance must be positive.");
    }
    for (int column = 0; column < size; ++column) {
      for (int previous = 0; previous < column; ++previous) {
        final double projection =
            OrbitalRotationOptimizer.overlapDot(coefficients, overlap, size, previous, column);
        for (int row = 0; row < size; ++row) {
          coefficients[row * size + column] -= projection * coefficients[row * size + previous];
        }
      }
      final double normSquared =
          OrbitalRotationOptimizer.overlapDot(coefficients, overlap, size, column, column);
      if (!Double.isFinite(normSquared) || normSquared <= SINGULAR_NORM_LIMIT) {
        throw new IllegalArgumentException("Orbital orthonormalization produced singular orbital.");
      }
      final double scale = 1.0 / Math.sqrt(normSquared);
      for (int row = 0; row < size; ++row) {
        coefficients[row * size + column] *= scale;
      }
    }
    final double error =
        OrbitalRotationOptimizer.maxOrthonormalityError(coefficients, overlap, size);
    if (error > tolerance) {
      throw new IllegalArgumentException("Orbital orthonormalization error exceeds tolerance.");
    }
  }

  public static double maxOrthonormalityError(
      final double[] coefficients, final double[] overlap, final int size) {
    OrbitalRotationOptimizer.requireSquareMatrix(coefficients, size, "MO coefficients");
    OrbitalRotationOptimizer.requireSquareMatrix(overlap, size, "AO overlap matrix");
    double error = 0.0;
    for (int first = 0; first < size; ++first) {
      for (int second = 0; second < size; ++second) {
        final double expected = first == second ? 1.0 : 0.0;
        final double actual =
            OrbitalRotationOptimizer.overlapDot(coefficients, overlap, size, first, second);
        error = Math.max(error, Math.abs(actual - expected));
      }
    }
    return error;
  }

  private static Trial trial(
      final double[] coefficients,
      final double[] overlapMatrix,
      final int orbitalCount,
      final OrbitalRotationPair pair,
      final double angle,
      final OrbitalEnergyEvaluator energyEvaluator,
      final double orthonormalityTolerance) {
    final double[] trialCoefficients = coefficients.clone();
    OrbitalRotationOptimizer.rotateColumns(
        trialCoefficients, orbitalCount, pair.firstOrbital(), pair.secondOrbital(), angle);
    OrbitalRotationOptimizer.orthonormalizeColumns(
        trialCoefficients, overlapMatrix, orbitalCount, orthonormalityTolerance);
    return Trial.of(
        trialCoefficients, OrbitalRotationOptimizer.evaluate(energyEvaluator, trialCoefficients));
  }

  private static double evaluate(
      final OrbitalEnergyEvaluator energyEvaluator, final double[] coefficients) {
    final double energy = energyEvaluator.evaluate(coefficients.clone());
    if (!Double.isFinite(energy)) {
      throw new IllegalArgumentException(
          "Orbital optimization evaluator returned non-finite energy.");
    }
    return energy;
  }

  private static double maxResidualEnergyImprovement(
      final double[] coefficients,
      final double[] overlapMatrix,
      final int orbitalCount,
      final ArrayList<OrbitalRotationPair> checkedPairs,
      final double stepRadians,
      final OrbitalEnergyEvaluator energyEvaluator,
      final OrbitalOptimizationOptions options) {
    final double referenceEnergy = OrbitalRotationOptimizer.evaluate(energyEvaluator, coefficients);
    double maxImprovement = 0.0;
    for (int i = 0; i < checkedPairs.size(); ++i) {
      final OrbitalRotationPair pair = checkedPairs.get(i);
      final PairOptimization pairOptimization =
          OrbitalRotationOptimizer.optimizePair(
              coefficients,
              overlapMatrix,
              orbitalCount,
              pair,
              stepRadians,
              referenceEnergy,
              energyEvaluator,
              options.orthonormalityTolerance());
      maxImprovement =
          Math.max(maxImprovement, referenceEnergy - pairOptimization.bestTrial().energy());
    }
    return Math.max(0.0, maxImprovement);
  }

  private static double maxResidualOrbitalGradient(
      final double[] coefficients,
      final double[] overlapMatrix,
      final int orbitalCount,
      final ArrayList<OrbitalRotationPair> checkedPairs,
      final double stepRadians,
      final OrbitalEnergyEvaluator energyEvaluator,
      final double orthonormalityTolerance) {
    double maxGradient = 0.0;
    for (int i = 0; i < checkedPairs.size(); ++i) {
      final OrbitalRotationPair pair = checkedPairs.get(i);
      final double positiveEnergy =
          OrbitalRotationOptimizer.trial(
                  coefficients,
                  overlapMatrix,
                  orbitalCount,
                  pair,
                  stepRadians,
                  energyEvaluator,
                  orthonormalityTolerance)
              .energy();
      final double negativeEnergy =
          OrbitalRotationOptimizer.trial(
                  coefficients,
                  overlapMatrix,
                  orbitalCount,
                  pair,
                  -stepRadians,
                  energyEvaluator,
                  orthonormalityTolerance)
              .energy();
      maxGradient =
          Math.max(maxGradient, Math.abs((positiveEnergy - negativeEnergy) / (2.0 * stepRadians)));
    }
    return maxGradient;
  }

  private static PairOptimization optimizePair(
      final double[] coefficients,
      final double[] overlapMatrix,
      final int orbitalCount,
      final OrbitalRotationPair pair,
      final double step,
      final double referenceEnergy,
      final OrbitalEnergyEvaluator energyEvaluator,
      final double orthonormalityTolerance) {
    final Trial positiveTrial =
        OrbitalRotationOptimizer.trial(
            coefficients,
            overlapMatrix,
            orbitalCount,
            pair,
            step,
            energyEvaluator,
            orthonormalityTolerance);
    final Trial negativeTrial =
        OrbitalRotationOptimizer.trial(
            coefficients,
            overlapMatrix,
            orbitalCount,
            pair,
            -step,
            energyEvaluator,
            orthonormalityTolerance);
    Trial bestTrial =
        positiveTrial.energy() <= negativeTrial.energy() ? positiveTrial : negativeTrial;
    int evaluatedRotations = 2;
    final double candidateAngle =
        OrbitalRotationOptimizer.quadraticMinimumAngle(
            negativeTrial.energy(), referenceEnergy, positiveTrial.energy(), step);
    if (candidateAngle != 0.0 && Math.abs(Math.abs(candidateAngle) - step) > step * 1.0E-9) {
      final Trial quadraticTrial =
          OrbitalRotationOptimizer.trial(
              coefficients,
              overlapMatrix,
              orbitalCount,
              pair,
              candidateAngle,
              energyEvaluator,
              orthonormalityTolerance);
      ++evaluatedRotations;
      if (quadraticTrial.energy() < bestTrial.energy()) {
        bestTrial = quadraticTrial;
      }
    }
    return PairOptimization.of(bestTrial, evaluatedRotations);
  }

  private static IterationPairSelection cyclePairSelection(
      final List<OrbitalRotationPair> pairs, final int pairCursor, final int maxPairsPerIteration) {
    final ArrayList<OrbitalRotationPair> selected =
        new ArrayList<OrbitalRotationPair>(Math.min(maxPairsPerIteration, pairs.size()));
    int cursor = pairCursor;
    for (int offset = 0;
        offset < pairs.size() && selected.size() < maxPairsPerIteration;
        ++offset) {
      selected.add(pairs.get(cursor));
      cursor = (cursor + 1) % pairs.size();
    }
    return IterationPairSelection.of(selected, cursor, 0);
  }

  private static RankedCycle rankedCycle(
      final double[] coefficients,
      final double[] overlapMatrix,
      final int orbitalCount,
      final ArrayList<OrbitalRotationPair> checkedPairs,
      final double step,
      final OrbitalEnergyEvaluator energyEvaluator,
      final double orthonormalityTolerance) {
    final ArrayList<RankedPair> rankedPairs = new ArrayList<RankedPair>(checkedPairs.size());
    for (int i = 0; i < checkedPairs.size(); ++i) {
      final OrbitalRotationPair pair = checkedPairs.get(i);
      final double positiveEnergy =
          OrbitalRotationOptimizer.trial(
                  coefficients,
                  overlapMatrix,
                  orbitalCount,
                  pair,
                  step,
                  energyEvaluator,
                  orthonormalityTolerance)
              .energy();
      final double negativeEnergy =
          OrbitalRotationOptimizer.trial(
                  coefficients,
                  overlapMatrix,
                  orbitalCount,
                  pair,
                  -step,
                  energyEvaluator,
                  orthonormalityTolerance)
              .energy();
      rankedPairs.add(
          RankedPair.of(pair, Math.abs((positiveEnergy - negativeEnergy) / (2.0 * step))));
    }
    rankedPairs.sort(Comparator.comparingDouble(RankedPair::negativeGradientMagnitude));
    final ArrayList<OrbitalRotationPair> pairs =
        new ArrayList<OrbitalRotationPair>(rankedPairs.size());
    for (int i = 0; i < rankedPairs.size(); ++i) {
      pairs.add(rankedPairs.get(i).pair());
    }
    return RankedCycle.of(pairs, checkedPairs.size() * 2);
  }

  private static double quadraticMinimumAngle(
      final double negativeEnergy,
      final double centerEnergy,
      final double positiveEnergy,
      final double step) {
    final double curvature = positiveEnergy + negativeEnergy - 2.0 * centerEnergy;
    if (!Double.isFinite(curvature) || curvature <= 0.0) {
      return 0.0;
    }
    final double gradientTimesStep = positiveEnergy - negativeEnergy;
    final double angle = -0.5 * step * gradientTimesStep / curvature;
    if (!Double.isFinite(angle)) {
      return 0.0;
    }
    if (angle >= step) {
      return step;
    }
    if (angle <= -step) {
      return -step;
    }
    return angle;
  }

  private static int minimumCoverageIterations(
      final int pairCount, final int maxPairsPerIteration) {
    return Math.max(1, (pairCount + maxPairsPerIteration - 1) / maxPairsPerIteration);
  }

  private static ArrayList<OrbitalRotationPair> checkedPairs(
      final List<OrbitalRotationPair> rotationPairs, final int orbitalCount) {
    final ArrayList<OrbitalRotationPair> checkedPairs =
        new ArrayList<OrbitalRotationPair>(rotationPairs.size());
    for (int i = 0; i < rotationPairs.size(); ++i) {
      final OrbitalRotationPair pair = rotationPairs.get(i);
      if (pair == null) {
        throw new IllegalArgumentException(
            "Orbital optimization rotation pair list contains null.");
      }
      if (pair.firstOrbital() >= orbitalCount || pair.secondOrbital() >= orbitalCount) {
        throw new IllegalArgumentException(
            "Orbital optimization rotation pair exceeds orbital count.");
      }
      checkedPairs.add(pair);
    }
    return checkedPairs;
  }

  private static double overlapDot(
      final double[] coefficients,
      final double[] overlap,
      final int size,
      final int firstColumn,
      final int secondColumn) {
    double value = 0.0;
    for (int row = 0; row < size; ++row) {
      double overlapTimesSecond = 0.0;
      for (int column = 0; column < size; ++column) {
        overlapTimesSecond +=
            overlap[row * size + column] * coefficients[column * size + secondColumn];
      }
      value += coefficients[row * size + firstColumn] * overlapTimesSecond;
    }
    return value;
  }

  private static void requireSquareMatrix(
      final double[] values, final int size, final String subjectName) {
    if (size <= 0) {
      throw new IllegalArgumentException(subjectName + " size must be positive.");
    }
    if (values == null || values.length != Math.multiplyExact(size, size)) {
      throw new IllegalArgumentException(
          subjectName + " must be a square matrix matching orbital count.");
    }
    for (int i = 0; i < values.length; ++i) {
      if (!Double.isFinite(values[i])) {
        throw new IllegalArgumentException(subjectName + " contains non-finite value.");
      }
    }
  }

  private static final class Trial {

    private final double[] coefficients;
    private final double energy;

    private Trial(final double[] coefficients, final double energy) {
      this.coefficients = coefficients;
      this.energy = energy;
    }

    static Trial of(final double[] coefficients, final double energy) {
      return new Trial(coefficients, energy);
    }

    double[] coefficients() {
      return this.coefficients;
    }

    double energy() {
      return this.energy;
    }
  }

  private static final class PairOptimization {

    private final Trial bestTrial;
    private final int evaluatedRotationCount;

    private PairOptimization(final Trial bestTrial, final int evaluatedRotationCount) {
      this.bestTrial = bestTrial;
      this.evaluatedRotationCount = evaluatedRotationCount;
    }

    static PairOptimization of(final Trial bestTrial, final int evaluatedRotationCount) {
      return new PairOptimization(bestTrial, evaluatedRotationCount);
    }

    Trial bestTrial() {
      return this.bestTrial;
    }

    int evaluatedRotationCount() {
      return this.evaluatedRotationCount;
    }
  }

  private static final class IterationPairSelection {

    private final List<OrbitalRotationPair> pairs;
    private final int nextPairCursor;
    private final int evaluatedRotationCount;

    private IterationPairSelection(
        final List<OrbitalRotationPair> pairs,
        final int nextPairCursor,
        final int evaluatedRotationCount) {
      this.pairs = pairs;
      this.nextPairCursor = nextPairCursor;
      this.evaluatedRotationCount = evaluatedRotationCount;
    }

    private static IterationPairSelection of(
        final List<OrbitalRotationPair> pairs,
        final int nextPairCursor,
        final int evaluatedRotationCount) {
      return new IterationPairSelection(List.copyOf(pairs), nextPairCursor, evaluatedRotationCount);
    }

    private List<OrbitalRotationPair> pairs() {
      return this.pairs;
    }

    private int nextPairCursor() {
      return this.nextPairCursor;
    }

    private int evaluatedRotationCount() {
      return this.evaluatedRotationCount;
    }
  }

  private static final class RankedCycle {

    private final ArrayList<OrbitalRotationPair> pairs;
    private final int evaluatedRotationCount;

    private RankedCycle(
        final ArrayList<OrbitalRotationPair> pairs, final int evaluatedRotationCount) {
      this.pairs = pairs;
      this.evaluatedRotationCount = evaluatedRotationCount;
    }

    private static RankedCycle of(
        final ArrayList<OrbitalRotationPair> pairs, final int evaluatedRotationCount) {
      return new RankedCycle(pairs, evaluatedRotationCount);
    }

    private ArrayList<OrbitalRotationPair> pairs() {
      return this.pairs;
    }

    private int evaluatedRotationCount() {
      return this.evaluatedRotationCount;
    }
  }

  private static final class RankedPair {

    private final OrbitalRotationPair pair;
    private final double gradientMagnitude;

    private RankedPair(final OrbitalRotationPair pair, final double gradientMagnitude) {
      this.pair = pair;
      this.gradientMagnitude = gradientMagnitude;
    }

    private static RankedPair of(final OrbitalRotationPair pair, final double gradientMagnitude) {
      return new RankedPair(pair, gradientMagnitude);
    }

    private OrbitalRotationPair pair() {
      return this.pair;
    }

    private double negativeGradientMagnitude() {
      return -this.gradientMagnitude;
    }
  }
}

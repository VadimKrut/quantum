/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.scf.application;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.orbital.OrbitalOptimizationOptions;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.orbital.OrbitalOptimizationResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.orbital.OrbitalRotationPair;

public final class OrbitalRotationOptimizerTest {

  @Test
  public void optimizesPairRotationWithoutMutatingSourceCoefficients() {
    final double[] coefficients =
        new double[] {
          1.0, 0.0,
          0.0, 1.0
        };
    final double[] sourceCopy = coefficients.clone();
    final double[] overlap =
        new double[] {
          1.0, 0.0,
          0.0, 1.0
        };
    final OrbitalOptimizationOptions options =
        OrbitalOptimizationOptions.of(64, 4, 0.2, 1.0E-5, 1.0E-10, 1.0E-10);

    final OrbitalOptimizationResult result =
        new OrbitalRotationOptimizer()
            .optimize(
                coefficients,
                overlap,
                2,
                List.of(OrbitalRotationPair.of(0, 1)),
                trialCoefficients -> {
                  final double targetAngle = 0.6;
                  final double angle = Math.atan2(trialCoefficients[2], trialCoefficients[0]);
                  final double delta = angle - targetAngle;
                  return delta * delta;
                },
                options);

    assertArrayEquals(sourceCopy, coefficients);
    assertTrue(result.converged());
    assertTrue(result.acceptedRotationCount() > 0);
    assertTrue(result.evaluatedRotationCount() >= result.acceptedRotationCount());
    assertTrue(result.finalEnergy() < result.initialEnergy());
    assertTrue(result.energyImprovement() > 0.3);
    assertTrue(result.maxResidualEnergyImprovement() < 1.0E-8);
    assertTrue(result.maxResidualOrbitalGradient() < 1.0E-5);
    assertTrue(result.iterationHistory().size() == result.iterations());
    assertTrue(result.evaluatedRotationCount() >= 3);
    assertEquals(
        0.0,
        OrbitalRotationOptimizer.maxOrthonormalityError(
            result.optimizedCoefficientsCopy(), overlap, 2),
        1.0E-10);
  }

  @Test
  public void optimizesInNonOrthogonalAoMetricAndKeepsReturnedCoefficientsImmutable() {
    final double[] coefficients =
        new double[] {
          1.0, 0.2, 0.0,
          0.0, 1.0, 0.3,
          0.1, 0.0, 1.0
        };
    final double[] overlap =
        new double[] {
          1.0, 0.14, 0.03,
          0.14, 1.0, 0.08,
          0.03, 0.08, 1.0
        };
    final OrbitalOptimizationOptions options =
        OrbitalOptimizationOptions.of(80, 8, 0.18, 1.0E-5, 1.0E-11, 1.0E-9);

    final OrbitalOptimizationResult result =
        new OrbitalRotationOptimizer()
            .optimize(
                coefficients,
                overlap,
                3,
                List.of(
                    OrbitalRotationPair.of(0, 1),
                    OrbitalRotationPair.of(1, 2),
                    OrbitalRotationPair.of(0, 2)),
                trialCoefficients -> {
                  final double target01 = 0.35;
                  final double target12 = -0.22;
                  final double mixed01 = trialCoefficients[3] - target01 * trialCoefficients[0];
                  final double mixed12 = trialCoefficients[7] - target12 * trialCoefficients[4];
                  return mixed01 * mixed01 + mixed12 * mixed12;
                },
                options);

    final double[] optimizedCopy = result.optimizedCoefficientsCopy();
    optimizedCopy[0] = 99.0;

    assertTrue(result.acceptedRotationCount() > 0);
    assertTrue(result.finalEnergy() <= result.initialEnergy());
    assertTrue(result.energyImprovement() >= 0.0);
    assertTrue(result.maxResidualEnergyImprovement() >= 0.0);
    assertTrue(result.maxResidualOrbitalGradient() >= 0.0);
    assertTrue(result.evaluatedRotationCount() >= result.acceptedRotationCount());
    assertTrue(
        OrbitalRotationOptimizer.maxOrthonormalityError(
                result.optimizedCoefficientsCopy(), overlap, 3)
            < 1.0E-9);
    assertTrue(result.optimizedCoefficientsCopy()[0] < 2.0);
  }

  @Test
  public void usesInteriorQuadraticLineMinimumForOrbitalPair() {
    final double[] coefficients =
        new double[] {
          1.0, 0.0,
          0.0, 1.0
        };
    final double[] overlap = coefficients.clone();
    final OrbitalOptimizationOptions options =
        OrbitalOptimizationOptions.of(16, 1, 0.5, 1.0E-5, 1.0E-12, 1.0E-12);

    final OrbitalOptimizationResult result =
        new OrbitalRotationOptimizer()
            .optimize(
                coefficients,
                overlap,
                2,
                List.of(OrbitalRotationPair.of(0, 1)),
                trialCoefficients -> {
                  final double targetAngle = 0.17;
                  final double angle = Math.atan2(trialCoefficients[2], trialCoefficients[0]);
                  final double delta = angle - targetAngle;
                  return delta * delta;
                },
                options);

    final double optimizedAngle =
        Math.atan2(result.optimizedCoefficientsCopy()[2], result.optimizedCoefficientsCopy()[0]);

    assertTrue(result.converged());
    assertTrue(result.finalEnergy() < 1.0E-10);
    assertEquals(0.17, optimizedAngle, 1.0E-5);
  }

  @Test
  public void respectsPerIterationPairLimitInOptimizationTrace() {
    final double[] coefficients =
        new double[] {
          1.0, 0.0, 0.0,
          0.0, 1.0, 0.0,
          0.0, 0.0, 1.0
        };
    final double[] overlap = coefficients.clone();
    final OrbitalOptimizationOptions options =
        OrbitalOptimizationOptions.of(3, 1, 0.1, 0.01, 1.0E-12, 1.0E-12);

    final OrbitalOptimizationResult result =
        new OrbitalRotationOptimizer()
            .optimize(
                coefficients,
                overlap,
                3,
                List.of(
                    OrbitalRotationPair.of(0, 1),
                    OrbitalRotationPair.of(1, 2),
                    OrbitalRotationPair.of(0, 2)),
                trialCoefficients -> -trialCoefficients[3],
                options);

    assertTrue(result.iterations() <= 3);
    assertTrue(result.evaluatedRotationCount() <= result.iterations() * 3);
    assertTrue(result.maxResidualEnergyImprovement() >= 0.0);
    assertTrue(
        result.iterationHistory().stream()
            .allMatch(record -> record.evaluatedRotationCount() <= 9));
  }

  @Test
  public void cyclesThroughAllPairsWhenPerIterationLimitIsSmallerThanPairList() {
    final double[] coefficients =
        new double[] {
          1.0, 0.0, 0.0,
          0.0, 1.0, 0.0,
          0.0, 0.0, 1.0
        };
    final double[] overlap = coefficients.clone();
    final OrbitalOptimizationOptions options =
        OrbitalOptimizationOptions.of(48, 1, 0.2, 1.0E-5, 1.0E-12, 1.0E-12);

    final OrbitalOptimizationResult result =
        new OrbitalRotationOptimizer()
            .optimize(
                coefficients,
                overlap,
                3,
                List.of(
                    OrbitalRotationPair.of(0, 1),
                    OrbitalRotationPair.of(0, 2),
                    OrbitalRotationPair.of(1, 2)),
                trialCoefficients -> {
                  final double targetAngle = 0.45;
                  final double angle = Math.atan2(trialCoefficients[7], trialCoefficients[4]);
                  final double delta = angle - targetAngle;
                  return delta * delta;
                },
                options);

    final double optimizedAngle =
        Math.atan2(result.optimizedCoefficientsCopy()[7], result.optimizedCoefficientsCopy()[4]);

    assertTrue(result.acceptedRotationCount() > 0);
    assertEquals(0.45, optimizedAngle, 2.0E-4);
    assertTrue(result.maxResidualEnergyImprovement() < 1.0E-8);
    assertTrue(result.maxResidualOrbitalGradient() < 1.0E-5);
  }

  @Test
  public void pairLimitCoverageDoesNotSkipTailRotationPairs() {
    final double[] coefficients =
        new double[] {
          1.0, 0.0, 0.0, 0.0,
          0.0, 1.0, 0.0, 0.0,
          0.0, 0.0, 1.0, 0.0,
          0.0, 0.0, 0.0, 1.0
        };
    final double[] overlap = coefficients.clone();
    final OrbitalOptimizationOptions options =
        OrbitalOptimizationOptions.of(32, 2, 0.2, 1.0E-5, 1.0E-12, 1.0E-12);

    final OrbitalOptimizationResult result =
        new OrbitalRotationOptimizer()
            .optimize(
                coefficients,
                overlap,
                4,
                List.of(
                    OrbitalRotationPair.of(0, 1),
                    OrbitalRotationPair.of(0, 2),
                    OrbitalRotationPair.of(1, 2),
                    OrbitalRotationPair.of(0, 3),
                    OrbitalRotationPair.of(2, 3)),
                trialCoefficients -> {
                  final double targetAngle = -0.31;
                  final double angle = Math.atan2(trialCoefficients[14], trialCoefficients[10]);
                  final double delta = angle - targetAngle;
                  return delta * delta;
                },
                options);

    final double optimizedAngle =
        Math.atan2(result.optimizedCoefficientsCopy()[14], result.optimizedCoefficientsCopy()[10]);

    assertTrue(result.acceptedRotationCount() > 0);
    assertEquals(-0.31, optimizedAngle, 2.0E-4);
    assertTrue(result.maxResidualEnergyImprovement() < 1.0E-8);
    assertTrue(result.maxResidualOrbitalGradient() < 1.0E-5);
  }

  @Test
  public void gradientRankedModeSelectsDominantTailPairUnderStrictPairLimit() {
    final double[] coefficients =
        new double[] {
          1.0, 0.0, 0.0, 0.0,
          0.0, 1.0, 0.0, 0.0,
          0.0, 0.0, 1.0, 0.0,
          0.0, 0.0, 0.0, 1.0
        };
    final double[] overlap = coefficients.clone();
    final OrbitalOptimizationOptions options =
        OrbitalOptimizationOptions.of(24, 1, 0.2, 1.0E-5, 1.0E-12, 1.0E-12);

    final OrbitalOptimizationResult result =
        new OrbitalRotationOptimizer()
            .optimizeGradientRanked(
                coefficients,
                overlap,
                4,
                List.of(
                    OrbitalRotationPair.of(0, 1),
                    OrbitalRotationPair.of(0, 2),
                    OrbitalRotationPair.of(1, 2),
                    OrbitalRotationPair.of(0, 3),
                    OrbitalRotationPair.of(2, 3)),
                trialCoefficients -> {
                  final double targetAngle = -0.28;
                  final double angle = Math.atan2(trialCoefficients[14], trialCoefficients[10]);
                  final double delta = angle - targetAngle;
                  return delta * delta;
                },
                options);

    final double optimizedAngle =
        Math.atan2(result.optimizedCoefficientsCopy()[14], result.optimizedCoefficientsCopy()[10]);

    assertTrue(result.acceptedRotationCount() > 0);
    assertTrue(result.evaluatedRotationCount() >= 10);
    assertEquals(-0.28, optimizedAngle, 2.0E-4);
    assertTrue(result.finalEnergy() < 1.0E-8);
  }

  @Test
  public void rejectsInvalidRotationPairsAndNonFiniteEnergies() {
    final double[] coefficients =
        new double[] {
          1.0, 0.0,
          0.0, 1.0
        };
    final double[] overlap = coefficients.clone();
    final OrbitalRotationOptimizer optimizer = new OrbitalRotationOptimizer();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            optimizer.optimize(
                coefficients,
                overlap,
                2,
                Arrays.asList(OrbitalRotationPair.of(0, 1), null),
                trialCoefficients -> 0.0,
                OrbitalOptimizationOptions.DEFAULT));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            optimizer.optimize(
                coefficients,
                overlap,
                2,
                List.of(OrbitalRotationPair.of(0, 1)),
                trialCoefficients -> Double.NaN,
                OrbitalOptimizationOptions.DEFAULT));
  }
}

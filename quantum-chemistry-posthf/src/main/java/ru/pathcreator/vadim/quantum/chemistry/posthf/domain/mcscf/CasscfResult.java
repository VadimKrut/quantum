/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.posthf.domain.mcscf;

import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.fci.FciResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.orbital.OrbitalOptimizationRecord;

public final class CasscfResult {

  private final boolean converged;
  private final int iterations;
  private final int evaluatedRotationCount;
  private final int acceptedRotationCount;
  private final int availableRotationPairCount;
  private final int firstActiveOrbital;
  private final int activeElectronCount;
  private final int activeOrbitalCount;
  private final String orbitalRotationSpace;
  private final double finalStepRadians;
  private final double initialTotalEnergy;
  private final double electronicEnergy;
  private final double totalEnergy;
  private final double maxOrthonormalityError;
  private final double maxResidualOrbitalEnergyImprovement;
  private final double maxResidualOrbitalGradient;
  private final double orbitalRotationPairCoverageRatio;
  private final String orbitalOptimizationQuality;
  private final String orbitalOptimizationStopReason;
  private final double[] optimizedOrbitalCoefficients;
  private final List<OrbitalOptimizationRecord> orbitalOptimizationHistory;
  private final FciResult fciResult;

  private CasscfResult(
      final boolean converged,
      final int iterations,
      final int evaluatedRotationCount,
      final int acceptedRotationCount,
      final int availableRotationPairCount,
      final int firstActiveOrbital,
      final int activeElectronCount,
      final int activeOrbitalCount,
      final String orbitalRotationSpace,
      final double finalStepRadians,
      final double initialTotalEnergy,
      final double electronicEnergy,
      final double totalEnergy,
      final double maxOrthonormalityError,
      final double maxResidualOrbitalEnergyImprovement,
      final double maxResidualOrbitalGradient,
      final double orbitalRotationPairCoverageRatio,
      final String orbitalOptimizationQuality,
      final String orbitalOptimizationStopReason,
      final double[] optimizedOrbitalCoefficients,
      final List<OrbitalOptimizationRecord> orbitalOptimizationHistory,
      final FciResult fciResult) {
    this.converged = converged;
    this.iterations = iterations;
    this.evaluatedRotationCount = evaluatedRotationCount;
    this.acceptedRotationCount = acceptedRotationCount;
    this.availableRotationPairCount = availableRotationPairCount;
    this.firstActiveOrbital = firstActiveOrbital;
    this.activeElectronCount = activeElectronCount;
    this.activeOrbitalCount = activeOrbitalCount;
    this.orbitalRotationSpace = orbitalRotationSpace;
    this.finalStepRadians = finalStepRadians;
    this.initialTotalEnergy = initialTotalEnergy;
    this.electronicEnergy = electronicEnergy;
    this.totalEnergy = totalEnergy;
    this.maxOrthonormalityError = maxOrthonormalityError;
    this.maxResidualOrbitalEnergyImprovement = maxResidualOrbitalEnergyImprovement;
    this.maxResidualOrbitalGradient = maxResidualOrbitalGradient;
    this.orbitalRotationPairCoverageRatio = orbitalRotationPairCoverageRatio;
    this.orbitalOptimizationQuality = orbitalOptimizationQuality;
    this.orbitalOptimizationStopReason = orbitalOptimizationStopReason;
    this.optimizedOrbitalCoefficients = optimizedOrbitalCoefficients;
    this.orbitalOptimizationHistory = orbitalOptimizationHistory;
    this.fciResult = fciResult;
  }

  public static CasscfResult of(
      final boolean converged,
      final int iterations,
      final int evaluatedRotationCount,
      final int acceptedRotationCount,
      final int availableRotationPairCount,
      final int firstActiveOrbital,
      final int activeElectronCount,
      final int activeOrbitalCount,
      final String orbitalRotationSpace,
      final double finalStepRadians,
      final double initialTotalEnergy,
      final double electronicEnergy,
      final double totalEnergy,
      final double maxOrthonormalityError,
      final double maxResidualOrbitalEnergyImprovement,
      final double maxResidualOrbitalGradient,
      final double orbitalRotationPairCoverageRatio,
      final String orbitalOptimizationQuality,
      final String orbitalOptimizationStopReason,
      final double[] optimizedOrbitalCoefficients,
      final List<OrbitalOptimizationRecord> orbitalOptimizationHistory,
      final FciResult fciResult) {
    if (iterations < 0) {
      throw new IllegalArgumentException("CASSCF iteration count must not be negative.");
    }
    if (evaluatedRotationCount < 0 || acceptedRotationCount < 0) {
      throw new IllegalArgumentException("CASSCF rotation counters must not be negative.");
    }
    if (acceptedRotationCount > evaluatedRotationCount) {
      throw new IllegalArgumentException(
          "CASSCF accepted rotations cannot exceed evaluated rotations.");
    }
    if (availableRotationPairCount < 0) {
      throw new IllegalArgumentException(
          "CASSCF available rotation pair count must not be negative.");
    }
    if (firstActiveOrbital < 0) {
      throw new IllegalArgumentException("CASSCF first active orbital must not be negative.");
    }
    if (activeElectronCount <= 0 || activeOrbitalCount <= 0) {
      throw new IllegalArgumentException("CASSCF active space dimensions must be positive.");
    }
    if (orbitalRotationSpace == null || orbitalRotationSpace.isBlank()) {
      throw new IllegalArgumentException("CASSCF orbital rotation space must not be blank.");
    }
    if (!Double.isFinite(finalStepRadians) || finalStepRadians < 0.0) {
      throw new IllegalArgumentException("CASSCF final step must be finite and non-negative.");
    }
    if (!Double.isFinite(initialTotalEnergy)
        || !Double.isFinite(electronicEnergy)
        || !Double.isFinite(totalEnergy)) {
      throw new IllegalArgumentException("CASSCF energies must be finite.");
    }
    if (!Double.isFinite(maxOrthonormalityError) || maxOrthonormalityError < 0.0) {
      throw new IllegalArgumentException(
          "CASSCF max orthonormality error must be finite and non-negative.");
    }
    if (!Double.isFinite(maxResidualOrbitalEnergyImprovement)
        || maxResidualOrbitalEnergyImprovement < 0.0) {
      throw new IllegalArgumentException(
          "CASSCF residual orbital energy improvement must be finite and non-negative.");
    }
    if (!Double.isFinite(maxResidualOrbitalGradient) || maxResidualOrbitalGradient < 0.0) {
      throw new IllegalArgumentException(
          "CASSCF residual orbital gradient must be finite and non-negative.");
    }
    if (!Double.isFinite(orbitalRotationPairCoverageRatio)
        || orbitalRotationPairCoverageRatio < 0.0
        || orbitalRotationPairCoverageRatio > 1.0) {
      throw new IllegalArgumentException(
          "CASSCF orbital rotation coverage ratio must be in [0, 1].");
    }
    if (orbitalOptimizationQuality == null || orbitalOptimizationQuality.isBlank()) {
      throw new IllegalArgumentException("CASSCF orbital optimization quality must not be blank.");
    }
    if (orbitalOptimizationStopReason == null || orbitalOptimizationStopReason.isBlank()) {
      throw new IllegalArgumentException(
          "CASSCF orbital optimization stop reason must not be blank.");
    }
    if (optimizedOrbitalCoefficients == null || optimizedOrbitalCoefficients.length == 0) {
      throw new IllegalArgumentException(
          "CASSCF optimized orbital coefficients must not be empty.");
    }
    for (int i = 0; i < optimizedOrbitalCoefficients.length; ++i) {
      if (!Double.isFinite(optimizedOrbitalCoefficients[i])) {
        throw new IllegalArgumentException(
            "CASSCF optimized orbital coefficients contain non-finite value.");
      }
    }
    if (fciResult == null) {
      throw new IllegalArgumentException("CASSCF FCI result must not be null.");
    }
    if (orbitalOptimizationHistory == null) {
      throw new IllegalArgumentException("CASSCF orbital optimization history must not be null.");
    }
    return new CasscfResult(
        converged,
        iterations,
        evaluatedRotationCount,
        acceptedRotationCount,
        availableRotationPairCount,
        firstActiveOrbital,
        activeElectronCount,
        activeOrbitalCount,
        orbitalRotationSpace,
        finalStepRadians,
        initialTotalEnergy,
        electronicEnergy,
        totalEnergy,
        maxOrthonormalityError,
        maxResidualOrbitalEnergyImprovement,
        maxResidualOrbitalGradient,
        orbitalRotationPairCoverageRatio,
        orbitalOptimizationQuality,
        orbitalOptimizationStopReason,
        optimizedOrbitalCoefficients.clone(),
        List.copyOf(orbitalOptimizationHistory),
        fciResult);
  }

  public boolean converged() {
    return this.converged;
  }

  public int iterations() {
    return this.iterations;
  }

  public int evaluatedRotationCount() {
    return this.evaluatedRotationCount;
  }

  public int acceptedRotationCount() {
    return this.acceptedRotationCount;
  }

  public int availableRotationPairCount() {
    return this.availableRotationPairCount;
  }

  public int firstActiveOrbital() {
    return this.firstActiveOrbital;
  }

  public int activeElectronCount() {
    return this.activeElectronCount;
  }

  public int activeOrbitalCount() {
    return this.activeOrbitalCount;
  }

  public String orbitalRotationSpace() {
    return this.orbitalRotationSpace;
  }

  public double finalStepRadians() {
    return this.finalStepRadians;
  }

  public double initialTotalEnergy() {
    return this.initialTotalEnergy;
  }

  public double electronicEnergy() {
    return this.electronicEnergy;
  }

  public double totalEnergy() {
    return this.totalEnergy;
  }

  public double maxOrthonormalityError() {
    return this.maxOrthonormalityError;
  }

  public double maxResidualOrbitalEnergyImprovement() {
    return this.maxResidualOrbitalEnergyImprovement;
  }

  public double maxResidualOrbitalGradient() {
    return this.maxResidualOrbitalGradient;
  }

  public double orbitalRotationPairCoverageRatio() {
    return this.orbitalRotationPairCoverageRatio;
  }

  public String orbitalOptimizationQuality() {
    return this.orbitalOptimizationQuality;
  }

  public String orbitalOptimizationStopReason() {
    return this.orbitalOptimizationStopReason;
  }

  public double totalEnergyImprovement() {
    return this.initialTotalEnergy - this.totalEnergy;
  }

  public double[] optimizedOrbitalCoefficientsCopy() {
    return this.optimizedOrbitalCoefficients.clone();
  }

  public List<OrbitalOptimizationRecord> orbitalOptimizationHistory() {
    return this.orbitalOptimizationHistory;
  }

  public FciResult fciResult() {
    return this.fciResult;
  }
}

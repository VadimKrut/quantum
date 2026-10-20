/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.posthf.domain.mcscf;

public final class CasscfOptions {

  private final int maxIterations;
  private final int maxDeterminants;
  private final int rootCount;
  private final int maxOrbitalPairs;
  private final double initialStepRadians;
  private final double minimumStepRadians;
  private final double energyTolerance;
  private final CasscfOrbitalRotationSpace orbitalRotationSpace;

  private CasscfOptions(
      final int maxIterations,
      final int maxDeterminants,
      final int rootCount,
      final int maxOrbitalPairs,
      final double initialStepRadians,
      final double minimumStepRadians,
      final double energyTolerance,
      final CasscfOrbitalRotationSpace orbitalRotationSpace) {
    this.maxIterations = maxIterations;
    this.maxDeterminants = maxDeterminants;
    this.rootCount = rootCount;
    this.maxOrbitalPairs = maxOrbitalPairs;
    this.initialStepRadians = initialStepRadians;
    this.minimumStepRadians = minimumStepRadians;
    this.energyTolerance = energyTolerance;
    this.orbitalRotationSpace = orbitalRotationSpace;
  }

  public static CasscfOptions of(
      final int maxIterations,
      final int maxDeterminants,
      final int rootCount,
      final int maxOrbitalPairs,
      final double initialStepRadians,
      final double minimumStepRadians,
      final double energyTolerance) {
    return CasscfOptions.of(
        maxIterations,
        maxDeterminants,
        rootCount,
        maxOrbitalPairs,
        initialStepRadians,
        minimumStepRadians,
        energyTolerance,
        CasscfOrbitalRotationSpace.ACTIVE_EXTERNAL);
  }

  public static CasscfOptions of(
      final int maxIterations,
      final int maxDeterminants,
      final int rootCount,
      final int maxOrbitalPairs,
      final double initialStepRadians,
      final double minimumStepRadians,
      final double energyTolerance,
      final CasscfOrbitalRotationSpace orbitalRotationSpace) {
    if (maxIterations <= 0) {
      throw new IllegalArgumentException("CASSCF max iterations must be positive.");
    }
    if (maxDeterminants <= 0) {
      throw new IllegalArgumentException("CASSCF max determinants must be positive.");
    }
    if (rootCount <= 0) {
      throw new IllegalArgumentException("CASSCF root count must be positive.");
    }
    if (maxOrbitalPairs <= 0) {
      throw new IllegalArgumentException("CASSCF max orbital pairs must be positive.");
    }
    if (!Double.isFinite(initialStepRadians) || initialStepRadians <= 0.0) {
      throw new IllegalArgumentException("CASSCF initial step must be positive and finite.");
    }
    if (!Double.isFinite(minimumStepRadians) || minimumStepRadians <= 0.0) {
      throw new IllegalArgumentException("CASSCF minimum step must be positive and finite.");
    }
    if (minimumStepRadians > initialStepRadians) {
      throw new IllegalArgumentException("CASSCF minimum step must not exceed initial step.");
    }
    if (!Double.isFinite(energyTolerance) || energyTolerance <= 0.0) {
      throw new IllegalArgumentException("CASSCF energy tolerance must be positive and finite.");
    }
    if (orbitalRotationSpace == null) {
      throw new IllegalArgumentException("CASSCF orbital rotation space must not be null.");
    }
    return new CasscfOptions(
        maxIterations,
        maxDeterminants,
        rootCount,
        maxOrbitalPairs,
        initialStepRadians,
        minimumStepRadians,
        energyTolerance,
        orbitalRotationSpace);
  }

  public static CasscfOptions defaults() {
    return CasscfOptions.of(24, 20_000, 1, 512, 0.04, 1.0E-5, 1.0E-8);
  }

  public int maxIterations() {
    return this.maxIterations;
  }

  public int maxDeterminants() {
    return this.maxDeterminants;
  }

  public int rootCount() {
    return this.rootCount;
  }

  public int maxOrbitalPairs() {
    return this.maxOrbitalPairs;
  }

  public double initialStepRadians() {
    return this.initialStepRadians;
  }

  public double minimumStepRadians() {
    return this.minimumStepRadians;
  }

  public double energyTolerance() {
    return this.energyTolerance;
  }

  public CasscfOrbitalRotationSpace orbitalRotationSpace() {
    return this.orbitalRotationSpace;
  }
}

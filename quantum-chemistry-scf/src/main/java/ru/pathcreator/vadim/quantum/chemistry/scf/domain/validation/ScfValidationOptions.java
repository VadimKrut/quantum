/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.scf.domain.validation;

/**
 * Численные допуски для проверки SCF-инвариантов.
 */
public final class ScfValidationOptions {

  public static final ScfValidationOptions DEFAULT =
      ScfValidationOptions.of(1.0E-7, 1.0E-9, 1.0E-7, 1.0E-6, 1.0E-6);

  private final double electronTraceTolerance;
  private final double densitySymmetryTolerance;
  private final double molecularOrbitalOrthonormalityTolerance;
  private final double orbitalEnergySortingTolerance;
  private final double spinContaminationWarningTolerance;

  private ScfValidationOptions(
      final double electronTraceTolerance,
      final double densitySymmetryTolerance,
      final double molecularOrbitalOrthonormalityTolerance,
      final double orbitalEnergySortingTolerance,
      final double spinContaminationWarningTolerance) {
    this.electronTraceTolerance = electronTraceTolerance;
    this.densitySymmetryTolerance = densitySymmetryTolerance;
    this.molecularOrbitalOrthonormalityTolerance = molecularOrbitalOrthonormalityTolerance;
    this.orbitalEnergySortingTolerance = orbitalEnergySortingTolerance;
    this.spinContaminationWarningTolerance = spinContaminationWarningTolerance;
  }

  public static ScfValidationOptions of(
      final double electronTraceTolerance,
      final double densitySymmetryTolerance,
      final double molecularOrbitalOrthonormalityTolerance,
      final double orbitalEnergySortingTolerance,
      final double spinContaminationWarningTolerance) {
    ScfValidationOptions.requireTolerance(electronTraceTolerance, "SCF electron trace tolerance");
    ScfValidationOptions.requireTolerance(
        densitySymmetryTolerance, "SCF density symmetry tolerance");
    ScfValidationOptions.requireTolerance(
        molecularOrbitalOrthonormalityTolerance, "SCF MO orthonormality tolerance");
    ScfValidationOptions.requireTolerance(
        orbitalEnergySortingTolerance, "SCF orbital sorting tolerance");
    ScfValidationOptions.requireTolerance(
        spinContaminationWarningTolerance, "SCF spin contamination warning tolerance");
    return new ScfValidationOptions(
        electronTraceTolerance,
        densitySymmetryTolerance,
        molecularOrbitalOrthonormalityTolerance,
        orbitalEnergySortingTolerance,
        spinContaminationWarningTolerance);
  }

  public double electronTraceTolerance() {
    return this.electronTraceTolerance;
  }

  public double densitySymmetryTolerance() {
    return this.densitySymmetryTolerance;
  }

  public double molecularOrbitalOrthonormalityTolerance() {
    return this.molecularOrbitalOrthonormalityTolerance;
  }

  public double orbitalEnergySortingTolerance() {
    return this.orbitalEnergySortingTolerance;
  }

  public double spinContaminationWarningTolerance() {
    return this.spinContaminationWarningTolerance;
  }

  private static void requireTolerance(final double value, final String subjectName) {
    if (!Double.isFinite(value) || value < 0.0) {
      throw new IllegalArgumentException(subjectName + " must be finite and non-negative.");
    }
  }
}

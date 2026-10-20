/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.fermion.domain.validation;

/** Пороговые значения аудита Hamiltonian перед fermion/qubit mapping. */
public final class FermionHamiltonianAuditOptions {

  public static final FermionHamiltonianAuditOptions DEFAULT =
      FermionHamiltonianAuditOptions.of(1.0E-10, 1.0E6, 1.0E-9);

  private final double negativeCoulombTolerance;
  private final double largeIntegralMagnitude;
  private final double symmetryValueTolerance;

  private FermionHamiltonianAuditOptions(
      final double negativeCoulombTolerance,
      final double largeIntegralMagnitude,
      final double symmetryValueTolerance) {
    this.negativeCoulombTolerance = negativeCoulombTolerance;
    this.largeIntegralMagnitude = largeIntegralMagnitude;
    this.symmetryValueTolerance = symmetryValueTolerance;
  }

  public static FermionHamiltonianAuditOptions of(
      final double negativeCoulombTolerance,
      final double largeIntegralMagnitude,
      final double symmetryValueTolerance) {
    if (!Double.isFinite(negativeCoulombTolerance) || negativeCoulombTolerance < 0.0) {
      throw new IllegalArgumentException(
          "Negative Coulomb tolerance must be finite and non-negative.");
    }
    if (!Double.isFinite(largeIntegralMagnitude) || largeIntegralMagnitude <= 0.0) {
      throw new IllegalArgumentException("Large integral magnitude must be finite and positive.");
    }
    if (!Double.isFinite(symmetryValueTolerance) || symmetryValueTolerance < 0.0) {
      throw new IllegalArgumentException(
          "Symmetry value tolerance must be finite and non-negative.");
    }
    return new FermionHamiltonianAuditOptions(
        negativeCoulombTolerance, largeIntegralMagnitude, symmetryValueTolerance);
  }

  public double negativeCoulombTolerance() {
    return this.negativeCoulombTolerance;
  }

  public double largeIntegralMagnitude() {
    return this.largeIntegralMagnitude;
  }

  public double symmetryValueTolerance() {
    return this.symmetryValueTolerance;
  }
}

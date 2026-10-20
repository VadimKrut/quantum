/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.posthf.domain.fci;

/**
 * Настройки точной диагонализации determinant-space Hamiltonian.
 */
public final class FciOptions {

  public static final FciOptions DEFAULT = FciOptions.of(20_000, 1);

  private final int maxDeterminants;
  private final int rootCount;
  private final boolean spinResolvedSector;
  private final int alphaElectronCount;
  private final int betaElectronCount;

  private FciOptions(
      final int maxDeterminants,
      final int rootCount,
      final boolean spinResolvedSector,
      final int alphaElectronCount,
      final int betaElectronCount) {
    this.maxDeterminants = maxDeterminants;
    this.rootCount = rootCount;
    this.spinResolvedSector = spinResolvedSector;
    this.alphaElectronCount = alphaElectronCount;
    this.betaElectronCount = betaElectronCount;
  }

  public static FciOptions of(final int maxDeterminants) {
    return FciOptions.of(maxDeterminants, 1);
  }

  public static FciOptions of(final int maxDeterminants, final int rootCount) {
    if (maxDeterminants <= 0) {
      throw new IllegalArgumentException("FCI max determinant count must be positive.");
    }
    if (rootCount <= 0) {
      throw new IllegalArgumentException("FCI root count must be positive.");
    }
    return new FciOptions(maxDeterminants, rootCount, false, -1, -1);
  }

  public static FciOptions spinSector(
      final int maxDeterminants,
      final int rootCount,
      final int alphaElectronCount,
      final int betaElectronCount) {
    if (maxDeterminants <= 0) {
      throw new IllegalArgumentException("FCI max determinant count must be positive.");
    }
    if (rootCount <= 0) {
      throw new IllegalArgumentException("FCI root count must be positive.");
    }
    if (alphaElectronCount < 0 || betaElectronCount < 0) {
      throw new IllegalArgumentException("FCI spin-sector electron counts must not be negative.");
    }
    if (alphaElectronCount + betaElectronCount <= 0) {
      throw new IllegalArgumentException("FCI spin-sector electron count must be positive.");
    }
    return new FciOptions(maxDeterminants, rootCount, true, alphaElectronCount, betaElectronCount);
  }

  public int maxDeterminants() {
    return this.maxDeterminants;
  }

  public int rootCount() {
    return this.rootCount;
  }

  public boolean spinResolvedSector() {
    return this.spinResolvedSector;
  }

  public int alphaElectronCount() {
    return this.alphaElectronCount;
  }

  public int betaElectronCount() {
    return this.betaElectronCount;
  }
}

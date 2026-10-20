/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.relativistic;

import java.util.List;

/** Сводка скалярной релятивистской поправки для молекулы. */
public final class RelativisticCorrectionResult {

  private final RelativisticCorrectionModel model;
  private final double totalCorrectionHartree;
  private final double heavyAtomFraction;
  private final int maxAtomicNumber;
  private final List<RelativisticAtomContribution> atomContributions;

  private RelativisticCorrectionResult(
      final RelativisticCorrectionModel model,
      final double totalCorrectionHartree,
      final double heavyAtomFraction,
      final int maxAtomicNumber,
      final List<RelativisticAtomContribution> atomContributions) {
    this.model = model;
    this.totalCorrectionHartree = totalCorrectionHartree;
    this.heavyAtomFraction = heavyAtomFraction;
    this.maxAtomicNumber = maxAtomicNumber;
    this.atomContributions = atomContributions;
  }

  public static RelativisticCorrectionResult of(
      final RelativisticCorrectionModel model,
      final double totalCorrectionHartree,
      final double heavyAtomFraction,
      final int maxAtomicNumber,
      final List<RelativisticAtomContribution> atomContributions) {
    if (model == null) {
      throw new IllegalArgumentException("Relativistic correction model must not be null.");
    }
    if (!Double.isFinite(totalCorrectionHartree)) {
      throw new IllegalArgumentException("Relativistic total correction must be finite.");
    }
    if (!Double.isFinite(heavyAtomFraction) || heavyAtomFraction < 0.0 || heavyAtomFraction > 1.0) {
      throw new IllegalArgumentException("Relativistic heavy atom fraction must be in [0, 1].");
    }
    if (maxAtomicNumber <= 0) {
      throw new IllegalArgumentException("Relativistic max atomic number must be positive.");
    }
    return new RelativisticCorrectionResult(
        model,
        totalCorrectionHartree,
        heavyAtomFraction,
        maxAtomicNumber,
        List.copyOf(RelativisticCorrectionResult.requireContributions(atomContributions)));
  }

  public RelativisticCorrectionModel model() {
    return this.model;
  }

  public double totalCorrectionHartree() {
    return this.totalCorrectionHartree;
  }

  public double heavyAtomFraction() {
    return this.heavyAtomFraction;
  }

  public int maxAtomicNumber() {
    return this.maxAtomicNumber;
  }

  public List<RelativisticAtomContribution> atomContributions() {
    return this.atomContributions;
  }

  private static List<RelativisticAtomContribution> requireContributions(
      final List<RelativisticAtomContribution> atomContributions) {
    if (atomContributions == null || atomContributions.isEmpty()) {
      throw new IllegalArgumentException("Relativistic atom contribution list must not be empty.");
    }
    for (int i = 0; i < atomContributions.size(); ++i) {
      if (atomContributions.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException("Relativistic atom contribution must not be null.");
    }
    return atomContributions;
  }
}

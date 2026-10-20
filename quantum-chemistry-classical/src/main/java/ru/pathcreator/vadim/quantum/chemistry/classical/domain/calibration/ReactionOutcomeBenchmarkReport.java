/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration;

import java.util.List;

/** РС‚РѕРі РЅРµР·Р°РІРёСЃРёРјРѕР№ РїСЂРѕРІРµСЂРєРё РїСЂРѕРіРЅРѕР·Р° РїРѕ СЂРµР°Р»СЊРЅС‹Рј benchmark-СЃС‚СЂРѕРєР°Рј. */
public final class ReactionOutcomeBenchmarkReport {

  private final List<ReactionOutcomeBenchmarkMatch> matches;
  private final int exactObservationCount;
  private final int comparableObservationCount;
  private final double meanAbsoluteError;
  private final double weightedMeanAbsoluteError;

  private ReactionOutcomeBenchmarkReport(
      final List<ReactionOutcomeBenchmarkMatch> matches,
      final int exactObservationCount,
      final int comparableObservationCount,
      final double meanAbsoluteError,
      final double weightedMeanAbsoluteError) {
    this.matches = matches;
    this.exactObservationCount = exactObservationCount;
    this.comparableObservationCount = comparableObservationCount;
    this.meanAbsoluteError = meanAbsoluteError;
    this.weightedMeanAbsoluteError = weightedMeanAbsoluteError;
  }

  public static ReactionOutcomeBenchmarkReport of(
      final List<ReactionOutcomeBenchmarkMatch> matches, final int exactObservationCount) {
    if (matches == null) {
      throw new IllegalArgumentException("Benchmark matches must not be null.");
    }
    if (exactObservationCount < 0) {
      throw new IllegalArgumentException("Exact benchmark observation count must not be negative.");
    }
    double error = 0.0;
    double weightedError = 0.0;
    double observedMass = 0.0;
    int comparable = 0;
    for (int i = 0; i < matches.size(); ++i) {
      final ReactionOutcomeBenchmarkMatch match = matches.get(i);
      if (match == null) {
        throw new IllegalArgumentException("Benchmark match must not be null.");
      }
      if (match.matched()) {
        ++comparable;
      }
      error += match.absoluteError();
      weightedError += match.absoluteError() * match.observedProbability();
      observedMass += match.observedProbability();
    }
    return new ReactionOutcomeBenchmarkReport(
        List.copyOf(matches),
        exactObservationCount,
        comparable,
        matches.isEmpty() ? 0.0 : error / matches.size(),
        observedMass == 0.0 ? 0.0 : weightedError / observedMass);
  }

  public List<ReactionOutcomeBenchmarkMatch> matches() {
    return this.matches;
  }

  public int exactObservationCount() {
    return this.exactObservationCount;
  }

  public int comparableObservationCount() {
    return this.comparableObservationCount;
  }

  public int unmatchedObservationCount() {
    return this.matches.size() - this.comparableObservationCount;
  }

  public double comparableCoverage() {
    if (this.exactObservationCount == 0) {
      return 0.0;
    }
    return (double) this.comparableObservationCount / (double) this.exactObservationCount;
  }

  public double meanAbsoluteError() {
    return this.meanAbsoluteError;
  }

  public double weightedMeanAbsoluteError() {
    return this.weightedMeanAbsoluteError;
  }
}

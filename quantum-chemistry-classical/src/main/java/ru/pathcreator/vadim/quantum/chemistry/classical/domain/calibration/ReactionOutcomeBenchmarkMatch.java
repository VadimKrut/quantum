/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration;

import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/** РЎРѕРїРѕСЃС‚Р°РІР»РµРЅРёРµ РѕРґРЅРѕР№ СЂРµР°Р»СЊРЅРѕР№ benchmark-СЃС‚СЂРѕРєРё СЃ РіРѕС‚РѕРІС‹Рј РїСЂРѕРіРЅРѕР·РѕРј РјРѕРґРµР»Рё. */
public final class ReactionOutcomeBenchmarkMatch {

  private final String observationId;
  private final String channelLabel;
  private final boolean matched;
  private final double observedProbability;
  private final double predictedProbability;
  private final double absoluteError;
  private final String reason;

  private ReactionOutcomeBenchmarkMatch(
      final String observationId,
      final String channelLabel,
      final boolean matched,
      final double observedProbability,
      final double predictedProbability,
      final double absoluteError,
      final String reason) {
    this.observationId = observationId;
    this.channelLabel = channelLabel;
    this.matched = matched;
    this.observedProbability = observedProbability;
    this.predictedProbability = predictedProbability;
    this.absoluteError = absoluteError;
    this.reason = reason;
  }

  public static ReactionOutcomeBenchmarkMatch matched(
      final String observationId,
      final String channelLabel,
      final double observedProbability,
      final double predictedProbability,
      final String reason) {
    ReactionOutcomeBenchmarkMatch.requireProbability(
        observedProbability, "Observed benchmark probability");
    ReactionOutcomeBenchmarkMatch.requireProbability(
        predictedProbability, "Predicted benchmark probability");
    return new ReactionOutcomeBenchmarkMatch(
        TextValue.requireText(observationId, "Benchmark observation id"),
        TextValue.requireText(channelLabel, "Benchmark channel label"),
        true,
        observedProbability,
        predictedProbability,
        Math.abs(observedProbability - predictedProbability),
        TextValue.requireText(reason, "Benchmark match reason"));
  }

  public static ReactionOutcomeBenchmarkMatch unmatched(
      final String observationId,
      final String channelLabel,
      final double observedProbability,
      final String reason) {
    ReactionOutcomeBenchmarkMatch.requireProbability(
        observedProbability, "Observed benchmark probability");
    return new ReactionOutcomeBenchmarkMatch(
        TextValue.requireText(observationId, "Benchmark observation id"),
        TextValue.requireText(channelLabel, "Benchmark channel label"),
        false,
        observedProbability,
        0.0,
        observedProbability,
        TextValue.requireText(reason, "Benchmark match reason"));
  }

  public String observationId() {
    return this.observationId;
  }

  public String channelLabel() {
    return this.channelLabel;
  }

  public boolean matched() {
    return this.matched;
  }

  public double observedProbability() {
    return this.observedProbability;
  }

  public double predictedProbability() {
    return this.predictedProbability;
  }

  public double absoluteError() {
    return this.absoluteError;
  }

  public String reason() {
    return this.reason;
  }

  private static void requireProbability(final double value, final String subject) {
    if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
      throw new IllegalArgumentException(subject + " must be in [0, 1].");
    }
  }
}

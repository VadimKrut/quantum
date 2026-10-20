/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.prediction;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/**
 * Нормализованное окно условий реакции, которое связывает прогноз с температурой, временем и средой.
 */
public final class ReactionConditionWindow {

  private final double minimumTemperatureKelvin;
  private final double maximumTemperatureKelvin;
  private final double minimumDurationHours;
  private final double maximumDurationHours;
  private final List<String> requiredConditionSignals;
  private final String note;

  private ReactionConditionWindow(
      final double minimumTemperatureKelvin,
      final double maximumTemperatureKelvin,
      final double minimumDurationHours,
      final double maximumDurationHours,
      final List<String> requiredConditionSignals,
      final String note) {
    this.minimumTemperatureKelvin = minimumTemperatureKelvin;
    this.maximumTemperatureKelvin = maximumTemperatureKelvin;
    this.minimumDurationHours = minimumDurationHours;
    this.maximumDurationHours = maximumDurationHours;
    this.requiredConditionSignals = requiredConditionSignals;
    this.note = note;
  }

  public static ReactionConditionWindow of(
      final double minimumTemperatureKelvin,
      final double maximumTemperatureKelvin,
      final double minimumDurationHours,
      final double maximumDurationHours,
      final List<String> requiredConditionSignals,
      final String note) {
    ReactionConditionWindow.requireRange(
        minimumTemperatureKelvin, maximumTemperatureKelvin, "temperature window");
    ReactionConditionWindow.requireRange(
        minimumDurationHours, maximumDurationHours, "duration window");
    return new ReactionConditionWindow(
        minimumTemperatureKelvin,
        maximumTemperatureKelvin,
        minimumDurationHours,
        maximumDurationHours,
        ReactionConditionWindow.checkedSignals(requiredConditionSignals),
        TextValue.optionalText(note));
  }

  public static ReactionConditionWindow broad(final String note) {
    return ReactionConditionWindow.of(
        0.0, Double.MAX_VALUE, 0.0, Double.MAX_VALUE, List.of(), note);
  }

  public double minimumTemperatureKelvin() {
    return this.minimumTemperatureKelvin;
  }

  public double maximumTemperatureKelvin() {
    return this.maximumTemperatureKelvin;
  }

  public double minimumDurationHours() {
    return this.minimumDurationHours;
  }

  public double maximumDurationHours() {
    return this.maximumDurationHours;
  }

  public List<String> requiredConditionSignals() {
    return this.requiredConditionSignals;
  }

  public String note() {
    return this.note;
  }

  public boolean hasRequiredConditionSignals() {
    return !this.requiredConditionSignals.isEmpty();
  }

  public boolean containsTemperatureKelvin(final double temperatureKelvin) {
    return Double.isFinite(temperatureKelvin)
        && temperatureKelvin >= this.minimumTemperatureKelvin
        && temperatureKelvin <= this.maximumTemperatureKelvin;
  }

  public boolean containsDurationHours(final double durationHours) {
    return Double.isFinite(durationHours)
        && durationHours >= this.minimumDurationHours
        && durationHours <= this.maximumDurationHours;
  }

  private static void requireRange(
      final double minimum, final double maximum, final String subject) {
    if (!Double.isFinite(minimum) || !Double.isFinite(maximum)) {
      throw new IllegalArgumentException("Reaction " + subject + " must be finite.");
    }
    if (minimum < 0.0 || maximum < 0.0 || minimum > maximum) {
      throw new IllegalArgumentException(
          "Reaction " + subject + " must be a non-negative ordered range.");
    }
  }

  private static List<String> checkedSignals(final List<String> signals) {
    if (signals == null || signals.isEmpty()) {
      return List.of();
    }
    final String[] checked = new String[signals.size()];
    for (int i = 0; i < signals.size(); ++i) {
      checked[i] = TextValue.requireText(signals.get(i), "Reaction condition window signal");
    }
    return List.of(checked);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionConditionWindow)) {
      return false;
    }
    final ReactionConditionWindow window = (ReactionConditionWindow) other;
    return Double.compare(this.minimumTemperatureKelvin, window.minimumTemperatureKelvin) == 0
        && Double.compare(this.maximumTemperatureKelvin, window.maximumTemperatureKelvin) == 0
        && Double.compare(this.minimumDurationHours, window.minimumDurationHours) == 0
        && Double.compare(this.maximumDurationHours, window.maximumDurationHours) == 0
        && Objects.equals(this.requiredConditionSignals, window.requiredConditionSignals)
        && Objects.equals(this.note, window.note);
  }

  public int hashCode() {
    return Objects.hash(
        this.minimumTemperatureKelvin,
        this.maximumTemperatureKelvin,
        this.minimumDurationHours,
        this.maximumDurationHours,
        this.requiredConditionSignals,
        this.note);
  }
}

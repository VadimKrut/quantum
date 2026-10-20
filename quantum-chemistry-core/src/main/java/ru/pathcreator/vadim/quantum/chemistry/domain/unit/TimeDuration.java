/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.unit;

/** Положительная длительность химического эксперимента или расчетной стадии. */
public final class TimeDuration {

  private static final double SECONDS_PER_MINUTE = 60.0;
  private static final double SECONDS_PER_HOUR = 3600.0;
  private static final double SECONDS_PER_DAY = 86400.0;

  private final double value;
  private final TimeDurationUnit unit;

  private TimeDuration(final double value, final TimeDurationUnit unit) {
    this.value = value;
    this.unit = unit;
  }

  public static TimeDuration of(final double value, final TimeDurationUnit unit) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException("Time duration must be finite.");
    }
    if (value <= 0.0) {
      throw new IllegalArgumentException("Time duration must be positive.");
    }
    if (unit == null) {
      throw new IllegalArgumentException("Time duration unit must not be null.");
    }
    return new TimeDuration(value, unit);
  }

  public double value() {
    return this.value;
  }

  public TimeDurationUnit unit() {
    return this.unit;
  }

  public double seconds() {
    if (this.unit == TimeDurationUnit.MINUTE) {
      return this.value * SECONDS_PER_MINUTE;
    }
    if (this.unit == TimeDurationUnit.HOUR) {
      return this.value * SECONDS_PER_HOUR;
    }
    if (this.unit == TimeDurationUnit.DAY) {
      return this.value * SECONDS_PER_DAY;
    }
    return this.value;
  }

  public double hours() {
    return this.seconds() / SECONDS_PER_HOUR;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof TimeDuration)) {
      return false;
    }
    final TimeDuration duration = (TimeDuration) other;
    return Double.compare(this.value, duration.value) == 0 && this.unit == duration.unit;
  }

  public int hashCode() {
    int result = Double.hashCode(this.value);
    result = 31 * result + this.unit.hashCode();
    return result;
  }
}

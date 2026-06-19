/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.mechanism;

public final class ReactionCoordinateValue {

  private final double value;

  private ReactionCoordinateValue(final double value) {
    this.value = value;
  }

  public static ReactionCoordinateValue of(final double value) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException("Reaction coordinate value must be finite.");
    }
    return new ReactionCoordinateValue(value);
  }

  public double value() {
    return this.value;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionCoordinateValue)) {
      return false;
    }
    final ReactionCoordinateValue coordinate = (ReactionCoordinateValue) other;
    return Double.compare(this.value, coordinate.value) == 0;
  }

  public int hashCode() {
    return Double.hashCode(this.value);
  }
}
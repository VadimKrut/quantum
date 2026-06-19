/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.reaction;

public final class ReactionComponentPurity {

  private static final double MAX_PERCENT = 100.0;
  private final double percent;

  private ReactionComponentPurity(final double percent) {
    this.percent = percent;
  }

  public static ReactionComponentPurity percent(final double percent) {
    if (!Double.isFinite(percent)) {
      throw new IllegalArgumentException("Reaction component purity must be finite.");
    }
    if (percent <= 0.0 || percent > MAX_PERCENT) {
      throw new IllegalArgumentException("Reaction component purity percent must be in (0, 100].");
    }
    return new ReactionComponentPurity(percent);
  }

  public double percent() {
    return this.percent;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionComponentPurity)) {
      return false;
    }
    final ReactionComponentPurity purity = (ReactionComponentPurity) other;
    return Double.compare(this.percent, purity.percent) == 0;
  }

  public int hashCode() {
    return Double.hashCode(this.percent);
  }
}
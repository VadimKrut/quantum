/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.reaction;

public final class ReactionComponentLoading {

  private static final double MAX_PERCENT = 100.0;
  private final double percent;

  private ReactionComponentLoading(final double percent) {
    this.percent = percent;
  }

  public static ReactionComponentLoading percent(final double percent) {
    if (!Double.isFinite(percent)) {
      throw new IllegalArgumentException("Reaction component loading must be finite.");
    }
    if (percent <= 0.0 || percent > MAX_PERCENT) {
      throw new IllegalArgumentException("Reaction component loading percent must be in (0, 100].");
    }
    return new ReactionComponentLoading(percent);
  }

  public double percent() {
    return this.percent;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionComponentLoading)) {
      return false;
    }
    final ReactionComponentLoading loading = (ReactionComponentLoading) other;
    return Double.compare(this.percent, loading.percent) == 0;
  }

  public int hashCode() {
    return Double.hashCode(this.percent);
  }
}
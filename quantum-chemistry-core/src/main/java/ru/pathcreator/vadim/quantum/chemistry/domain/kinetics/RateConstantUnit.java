/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.kinetics;

public enum RateConstantUnit {

  MOLE_PER_LITER_SECOND(0.0),
  PER_SECOND(1.0),
  LITER_PER_MOLE_SECOND(2.0),
  SQUARE_LITER_PER_SQUARE_MOLE_SECOND(3.0),
  CUSTOM(Double.NaN);

  private final double reactionOrder;

  private RateConstantUnit(final double reactionOrder) {
    this.reactionOrder = reactionOrder;
  }

  public boolean matchesOrder(final double order) {
    if (this == CUSTOM) {
      return true;
    }
    return Math.abs(this.reactionOrder - order) <= 1.0E-12;
  }
}
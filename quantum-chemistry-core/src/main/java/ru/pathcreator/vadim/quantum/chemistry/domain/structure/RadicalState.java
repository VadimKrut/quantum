/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.structure;

public final class RadicalState {

  public static final RadicalState CLOSED_SHELL = new RadicalState(0);
  private static final int MIN_UNPAIRED_ELECTRONS = 0;
  private static final int MAX_UNPAIRED_ELECTRONS = 8;
  private final int unpairedElectrons;

  private RadicalState(final int unpairedElectrons) {
    this.unpairedElectrons = unpairedElectrons;
  }

  public static RadicalState of(final int unpairedElectrons) {
    if (unpairedElectrons < MIN_UNPAIRED_ELECTRONS
        || unpairedElectrons > MAX_UNPAIRED_ELECTRONS) {
      throw new IllegalArgumentException("Unpaired electron count must be between 0 and 8.");
    }
    if (unpairedElectrons == 0) {
      return CLOSED_SHELL;
    }
    return new RadicalState(unpairedElectrons);
  }

  public int unpairedElectrons() {
    return this.unpairedElectrons;
  }

  public boolean radical() {
    return this.unpairedElectrons > 0;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof RadicalState)) {
      return false;
    }
    final RadicalState state = (RadicalState) other;
    return this.unpairedElectrons == state.unpairedElectrons;
  }

  public int hashCode() {
    return this.unpairedElectrons;
  }
}
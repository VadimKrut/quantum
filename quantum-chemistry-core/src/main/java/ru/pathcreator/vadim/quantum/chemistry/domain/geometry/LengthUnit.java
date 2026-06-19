/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.geometry;

public enum LengthUnit {

  ANGSTROM,
  BOHR;

  private static final double BOHR_TO_ANGSTROM = 0.529177210903;

  public double toAngstrom(final double value) {
    switch (this) {
      case ANGSTROM:
        return value;
      case BOHR:
        return value * BOHR_TO_ANGSTROM;
      default:
        throw new IllegalStateException("Unsupported length unit.");
    }
  }

  public double fromAngstrom(final double value) {
    switch (this) {
      case ANGSTROM:
        return value;
      case BOHR:
        return value / BOHR_TO_ANGSTROM;
      default:
        throw new IllegalStateException("Unsupported length unit.");
    }
  }
}
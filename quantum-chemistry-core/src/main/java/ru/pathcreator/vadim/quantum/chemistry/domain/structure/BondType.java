/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.structure;

public enum BondType {

  SINGLE,
  DOUBLE,
  TRIPLE,
  AROMATIC,
  COORDINATE,
  UNKNOWN;

  public boolean hasDefinedOrder() {
    return this != UNKNOWN;
  }

  public double orderValue() {
    switch (this) {
      case SINGLE:
      case COORDINATE:
        return 1.0;
      case DOUBLE:
        return 2.0;
      case TRIPLE:
        return 3.0;
      case AROMATIC:
        return 1.5;
      case UNKNOWN:
        throw new IllegalStateException("Unknown bond type does not have defined order.");
      default:
        throw new IllegalStateException("Unsupported bond type.");
    }
  }
}
/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.integrals.application;

/** Дает доступ к одному AO ERI quartet без требования материализовать полный tensor. */
public interface AoElectronRepulsionValues {

  double valueAt(int first, int second, int third, int fourth);

  default double screenedInValueAt(
      final int first, final int second, final int third, final int fourth) {
    return this.valueAt(first, second, third, fourth);
  }

  default boolean screenedOut(
      final int first, final int second, final int third, final int fourth) {
    return false;
  }

  default double pairUpperBound(final int first, final int second) {
    return Double.POSITIVE_INFINITY;
  }
}

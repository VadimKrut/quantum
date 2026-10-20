/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.method;

/** Угловой момент атомной basis shell для Gaussian-type orbital basis. */
public enum BasisShellAngularMomentum {
  S(0),
  P(1),
  D(2),
  F(3),
  G(4),
  H(5),
  I(6),
  K(7),
  L(8),
  M(9),
  N(10),
  O(11),
  Q(12);

  private final int angularMomentum;
  private final int cartesianFunctionCount;

  BasisShellAngularMomentum(final int angularMomentum) {
    this.angularMomentum = angularMomentum;
    this.cartesianFunctionCount = (angularMomentum + 1) * (angularMomentum + 2) / 2;
  }

  public int angularMomentum() {
    return this.angularMomentum;
  }

  public int cartesianFunctionCount() {
    return this.cartesianFunctionCount;
  }
}

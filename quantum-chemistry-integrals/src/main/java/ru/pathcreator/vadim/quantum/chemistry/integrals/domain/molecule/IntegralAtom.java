/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule;

/**
 * Atom snapshot in atomic units for numerical chemistry kernels.
 */
public final class IntegralAtom {

  private final int atomicNumber;
  private final double xBohr;
  private final double yBohr;
  private final double zBohr;

  private IntegralAtom(
      final int atomicNumber, final double xBohr, final double yBohr, final double zBohr) {
    this.atomicNumber = atomicNumber;
    this.xBohr = xBohr;
    this.yBohr = yBohr;
    this.zBohr = zBohr;
  }

  public static IntegralAtom of(
      final int atomicNumber, final double xBohr, final double yBohr, final double zBohr) {
    if (atomicNumber <= 0) {
      throw new IllegalArgumentException("Integral atom atomic number must be positive.");
    }
    if (!(Double.isFinite(xBohr) && Double.isFinite(yBohr) && Double.isFinite(zBohr))) {
      throw new IllegalArgumentException("Integral atom coordinates must be finite.");
    }
    return new IntegralAtom(atomicNumber, xBohr, yBohr, zBohr);
  }

  public int atomicNumber() {
    return this.atomicNumber;
  }

  public double xBohr() {
    return this.xBohr;
  }

  public double yBohr() {
    return this.yBohr;
  }

  public double zBohr() {
    return this.zBohr;
  }
}

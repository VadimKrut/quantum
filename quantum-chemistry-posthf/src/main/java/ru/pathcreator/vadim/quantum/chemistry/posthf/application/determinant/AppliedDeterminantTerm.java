/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.posthf.application.determinant;

/**
 * Результат применения ladder-операторов к Slater determinant.
 */
public final class AppliedDeterminantTerm {

  private static final AppliedDeterminantTerm ZERO = new AppliedDeterminantTerm(0L, 0.0, false);

  private final long determinant;
  private final double phase;
  private final boolean nonZero;

  private AppliedDeterminantTerm(
      final long determinant, final double phase, final boolean nonZero) {
    this.determinant = determinant;
    this.phase = phase;
    this.nonZero = nonZero;
  }

  public static AppliedDeterminantTerm zero() {
    return ZERO;
  }

  public static AppliedDeterminantTerm of(final long determinant, final double phase) {
    return new AppliedDeterminantTerm(determinant, phase, true);
  }

  public long determinant() {
    return this.determinant;
  }

  public double phase() {
    return this.phase;
  }

  public boolean nonZero() {
    return this.nonZero;
  }
}

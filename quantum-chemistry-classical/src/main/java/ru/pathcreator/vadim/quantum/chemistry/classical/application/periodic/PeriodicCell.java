/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.periodic;

/** Триклинная расчетная ячейка в ангстремах и градусах. */
public final class PeriodicCell {

  private static final double MIN_ANGLE_DEGREES = 0.0;
  private static final double MAX_ANGLE_DEGREES = 180.0;
  private static final double DEGREE_TO_RADIAN = Math.PI / 180.0;

  private final double aAngstrom;
  private final double bAngstrom;
  private final double cAngstrom;
  private final double alphaDegrees;
  private final double betaDegrees;
  private final double gammaDegrees;

  private PeriodicCell(
      final double aAngstrom,
      final double bAngstrom,
      final double cAngstrom,
      final double alphaDegrees,
      final double betaDegrees,
      final double gammaDegrees) {
    this.aAngstrom = aAngstrom;
    this.bAngstrom = bAngstrom;
    this.cAngstrom = cAngstrom;
    this.alphaDegrees = alphaDegrees;
    this.betaDegrees = betaDegrees;
    this.gammaDegrees = gammaDegrees;
  }

  public static PeriodicCell of(
      final double aAngstrom,
      final double bAngstrom,
      final double cAngstrom,
      final double alphaDegrees,
      final double betaDegrees,
      final double gammaDegrees) {
    PeriodicCell.requirePositiveLength(aAngstrom, "Periodic cell a");
    PeriodicCell.requirePositiveLength(bAngstrom, "Periodic cell b");
    PeriodicCell.requirePositiveLength(cAngstrom, "Periodic cell c");
    PeriodicCell.requireAngle(alphaDegrees, "Periodic cell alpha");
    PeriodicCell.requireAngle(betaDegrees, "Periodic cell beta");
    PeriodicCell.requireAngle(gammaDegrees, "Periodic cell gamma");
    final PeriodicCell cell =
        new PeriodicCell(aAngstrom, bAngstrom, cAngstrom, alphaDegrees, betaDegrees, gammaDegrees);
    if (!(cell.volumeAngstrom3() > 0.0)) {
      throw new IllegalArgumentException("Periodic cell volume must be positive.");
    }
    return cell;
  }

  public double aAngstrom() {
    return this.aAngstrom;
  }

  public double bAngstrom() {
    return this.bAngstrom;
  }

  public double cAngstrom() {
    return this.cAngstrom;
  }

  public double alphaDegrees() {
    return this.alphaDegrees;
  }

  public double betaDegrees() {
    return this.betaDegrees;
  }

  public double gammaDegrees() {
    return this.gammaDegrees;
  }

  public double volumeAngstrom3() {
    final double alphaCos = Math.cos(this.alphaDegrees * DEGREE_TO_RADIAN);
    final double betaCos = Math.cos(this.betaDegrees * DEGREE_TO_RADIAN);
    final double gammaCos = Math.cos(this.gammaDegrees * DEGREE_TO_RADIAN);
    final double radicand =
        1.0
            + 2.0 * alphaCos * betaCos * gammaCos
            - alphaCos * alphaCos
            - betaCos * betaCos
            - gammaCos * gammaCos;
    return this.aAngstrom * this.bAngstrom * this.cAngstrom * Math.sqrt(Math.max(radicand, 0.0));
  }

  public double shortestLengthAngstrom() {
    return Math.min(this.aAngstrom, Math.min(this.bAngstrom, this.cAngstrom));
  }

  private static void requirePositiveLength(final double value, final String name) {
    if (!Double.isFinite(value) || value <= 0.0) {
      throw new IllegalArgumentException(name + " must be positive and finite.");
    }
  }

  private static void requireAngle(final double value, final String name) {
    if (!Double.isFinite(value) || value <= MIN_ANGLE_DEGREES || value >= MAX_ANGLE_DEGREES) {
      throw new IllegalArgumentException(name + " must be in (0, 180) degrees.");
    }
  }
}

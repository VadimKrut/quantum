/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.problem;

public final class MolecularOrbitalOccupation {

  public static final MolecularOrbitalOccupation EMPTY = new MolecularOrbitalOccupation(0.0);
  public static final MolecularOrbitalOccupation SINGLE = new MolecularOrbitalOccupation(1.0);
  public static final MolecularOrbitalOccupation DOUBLE = new MolecularOrbitalOccupation(2.0);
  private final double electronCount;

  private MolecularOrbitalOccupation(final double electronCount) {
    this.electronCount = electronCount;
  }

  public static MolecularOrbitalOccupation of(final double electronCount) {
    if (!Double.isFinite(electronCount)) {
      throw new IllegalArgumentException("Molecular orbital occupation must be finite.");
    }
    if (electronCount < 0.0 || electronCount > 2.0) {
      throw new IllegalArgumentException(
          "Molecular orbital occupation must be between 0 and 2 electrons.");
    }
    return new MolecularOrbitalOccupation(electronCount);
  }

  public double electronCount() {
    return this.electronCount;
  }

  public boolean empty() {
    return this.electronCount == 0.0;
  }

  public boolean fullyOccupied() {
    return this.electronCount == 2.0;
  }

  public boolean fractional() {
    return this.electronCount != 0.0 && this.electronCount != 1.0 && this.electronCount != 2.0;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MolecularOrbitalOccupation)) {
      return false;
    }
    final MolecularOrbitalOccupation occupation = (MolecularOrbitalOccupation) other;
    return Double.compare(this.electronCount, occupation.electronCount) == 0;
  }

  public int hashCode() {
    return Double.hashCode(this.electronCount);
  }
}
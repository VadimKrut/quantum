/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.conformation;

import java.util.List;
import java.util.Objects;

public final class MolecularConformation {

  public static final MolecularConformation EMPTY = new MolecularConformation(List.of());
  private final List<TorsionAngle> torsionAngles;

  private MolecularConformation(final List<TorsionAngle> torsionAngles) {
    this.torsionAngles = torsionAngles;
  }

  public static MolecularConformation of(final List<TorsionAngle> torsionAngles) {
    if (torsionAngles == null || torsionAngles.isEmpty()) {
      return EMPTY;
    }
    for (int i = 0; i < torsionAngles.size(); ++i) {
      TorsionAngle angle = torsionAngles.get(i);
      if (angle == null) {
        throw new IllegalArgumentException("Torsion angle must not be null.");
      }
      for (int j = i + 1; j < torsionAngles.size(); ++j) {
        if (!angle.equals(torsionAngles.get(j))) continue;
        throw new IllegalArgumentException(
            "Molecular conformation contains duplicate torsion angle.");
      }
    }
    return new MolecularConformation(List.copyOf(torsionAngles));
  }

  public List<TorsionAngle> torsionAngles() {
    return this.torsionAngles;
  }

  public boolean empty() {
    return this.torsionAngles.isEmpty();
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MolecularConformation)) {
      return false;
    }
    final MolecularConformation conformation = (MolecularConformation) other;
    return Objects.equals(this.torsionAngles, conformation.torsionAngles);
  }

  public int hashCode() {
    return this.torsionAngles.hashCode();
  }
}
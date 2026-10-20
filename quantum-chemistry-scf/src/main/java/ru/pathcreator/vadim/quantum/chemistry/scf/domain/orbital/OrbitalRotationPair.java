/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.scf.domain.orbital;

/**
 * Пара molecular orbitals, между которыми разрешено унитарное вращение.
 */
public final class OrbitalRotationPair {

  private final int firstOrbital;
  private final int secondOrbital;

  private OrbitalRotationPair(final int firstOrbital, final int secondOrbital) {
    this.firstOrbital = firstOrbital;
    this.secondOrbital = secondOrbital;
  }

  public static OrbitalRotationPair of(final int firstOrbital, final int secondOrbital) {
    if (firstOrbital < 0 || secondOrbital < 0) {
      throw new IllegalArgumentException("Orbital rotation pair indexes must not be negative.");
    }
    if (firstOrbital == secondOrbital) {
      throw new IllegalArgumentException(
          "Orbital rotation pair must contain two different orbitals.");
    }
    return new OrbitalRotationPair(firstOrbital, secondOrbital);
  }

  public int firstOrbital() {
    return this.firstOrbital;
  }

  public int secondOrbital() {
    return this.secondOrbital;
  }
}

/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.problem;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;

/** Spin-orbital как spatial orbital плюс alpha/beta spin projection. */
public final class SpinOrbital {

  private final SpatialOrbitalIndex spatialIndex;
  private final SpinProjection spinProjection;

  private SpinOrbital(
      final SpatialOrbitalIndex spatialIndex,
      final SpinProjection spinProjection
  ) {
    this.spatialIndex = spatialIndex;
    this.spinProjection = spinProjection;
  }

  public static SpinOrbital of(
      final SpatialOrbitalIndex spatialIndex, final SpinProjection spinProjection) {
    if (spatialIndex == null) {
      throw new IllegalArgumentException("Spatial orbital index must not be null.");
    }
    if (spinProjection == null) {
      throw new IllegalArgumentException("Spin projection must not be null.");
    }
    return new SpinOrbital(spatialIndex, spinProjection);
  }

  public static SpinOrbital alpha(final int spatialIndex) {
    return SpinOrbital.of(SpatialOrbitalIndex.of(spatialIndex), SpinProjection.ALPHA);
  }

  public static SpinOrbital beta(final int spatialIndex) {
    return SpinOrbital.of(SpatialOrbitalIndex.of(spatialIndex), SpinProjection.BETA);
  }

  public SpatialOrbitalIndex spatialIndex() {
    return spatialIndex;
  }

  public SpinProjection spinProjection() {
    return spinProjection;
  }

  public int canonicalSpinOrbitalIndex() {
    return Math.addExact(
        Math.multiplyExact(spatialIndex.value(), 2),
        spinProjection == SpinProjection.ALPHA ? 0 : 1);
  }

  public void requireWithin(final int orbitalCount) {
    spatialIndex.requireWithin(orbitalCount);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof SpinOrbital)) {
      return false;
    }
    final SpinOrbital orbital = (SpinOrbital) other;
    return Objects.equals(spatialIndex, orbital.spatialIndex)
        && spinProjection == orbital.spinProjection;
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, spatialIndex);
    result = ChemistryHash.include(result, spinProjection);
    return result;
  }
}
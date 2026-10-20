/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis;

import java.util.List;

/**
 * Contracted Gaussian orbital bound to one atom center.
 */
public final class ContractedGaussianOrbital {

  private final int atomIndex;
  private final GaussianAngularMomentum angularMomentum;
  private final List<GaussianPrimitive> primitives;

  private ContractedGaussianOrbital(
      final int atomIndex,
      final GaussianAngularMomentum angularMomentum,
      final List<GaussianPrimitive> primitives) {
    this.atomIndex = atomIndex;
    this.angularMomentum = angularMomentum;
    this.primitives = primitives;
  }

  public static ContractedGaussianOrbital of(
      final int atomIndex,
      final GaussianAngularMomentum angularMomentum,
      final List<GaussianPrimitive> primitives) {
    if (atomIndex < 0) {
      throw new IllegalArgumentException("Orbital atom index must not be negative.");
    }
    if (angularMomentum == null) {
      throw new IllegalArgumentException("Orbital angular momentum must not be null.");
    }
    if (primitives == null || primitives.isEmpty()) {
      throw new IllegalArgumentException("Orbital primitives must not be empty.");
    }
    for (int i = 0; i < primitives.size(); ++i) {
      if (primitives.get(i) == null) {
        throw new IllegalArgumentException("Orbital primitive must not be null.");
      }
    }
    return new ContractedGaussianOrbital(atomIndex, angularMomentum, List.copyOf(primitives));
  }

  public int atomIndex() {
    return this.atomIndex;
  }

  public GaussianAngularMomentum angularMomentum() {
    return this.angularMomentum;
  }

  public List<GaussianPrimitive> primitives() {
    return this.primitives;
  }
}

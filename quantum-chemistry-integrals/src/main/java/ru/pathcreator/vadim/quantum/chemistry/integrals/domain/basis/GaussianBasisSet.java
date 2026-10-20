/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Упорядоченный molecular basis для построения one- и two-electron tensors.
 */
public final class GaussianBasisSet {

  private final String name;
  private final List<ContractedGaussianOrbital> orbitals;

  private GaussianBasisSet(final String name, final List<ContractedGaussianOrbital> orbitals) {
    this.name = name;
    this.orbitals = orbitals;
  }

  public static GaussianBasisSet of(
      final String name, final List<ContractedGaussianOrbital> orbitals) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Gaussian basis name must not be blank.");
    }
    if (orbitals == null || orbitals.isEmpty()) {
      throw new IllegalArgumentException("Gaussian basis orbitals must not be empty.");
    }
    for (int i = 0; i < orbitals.size(); ++i) {
      if (orbitals.get(i) == null) {
        throw new IllegalArgumentException("Gaussian basis orbital must not be null.");
      }
    }
    return new GaussianBasisSet(name, List.copyOf(orbitals));
  }

  public String name() {
    return this.name;
  }

  public List<ContractedGaussianOrbital> orbitals() {
    return this.orbitals;
  }

  public int orbitalCount() {
    return this.orbitals.size();
  }

  public int maxAngularMomentum() {
    int maxAngularMomentum = 0;
    for (int i = 0; i < this.orbitals.size(); ++i) {
      maxAngularMomentum =
          Math.max(maxAngularMomentum, this.orbitals.get(i).angularMomentum().totalPower());
    }
    return maxAngularMomentum;
  }

  public Map<GaussianShellType, Integer> shellHistogram() {
    final EnumMap<GaussianShellType, Integer> histogram =
        new EnumMap<GaussianShellType, Integer>(GaussianShellType.class);
    for (int i = 0; i < this.orbitals.size(); ++i) {
      final GaussianShellType shellType = this.orbitals.get(i).angularMomentum().shellType();
      histogram.put(shellType, Integer.valueOf(histogram.getOrDefault(shellType, 0) + 1));
    }
    return Map.copyOf(histogram);
  }
}

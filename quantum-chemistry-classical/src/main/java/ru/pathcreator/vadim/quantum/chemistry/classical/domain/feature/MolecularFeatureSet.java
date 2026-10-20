/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature;

import java.util.List;
import java.util.Objects;

/** Неизменяемый набор признаков одной молекулы. */
public final class MolecularFeatureSet {

  private final List<MolecularFeature> features;

  private MolecularFeatureSet(final List<MolecularFeature> features) {
    this.features = features;
  }

  public static MolecularFeatureSet of(final List<MolecularFeature> features) {
    if (features == null) {
      throw new IllegalArgumentException("Molecular feature set must not be null.");
    }
    for (int i = 0; i < features.size(); ++i) {
      if (features.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException("Molecular feature must not be null.");
    }
    return new MolecularFeatureSet(List.copyOf(features));
  }

  public List<MolecularFeature> features() {
    return this.features;
  }

  public boolean contains(final MolecularFeatureKind kind) {
    return this.countOf(kind) > 0L;
  }

  public long countOf(final MolecularFeatureKind kind) {
    if (kind == null) {
      throw new IllegalArgumentException("Molecular feature kind must not be null.");
    }
    long count = 0L;
    for (int i = 0; i < this.features.size(); ++i) {
      final MolecularFeature feature = this.features.get(i);
      if (feature.kind() != kind) {
        continue;
      }
      count += feature.count();
    }
    return count;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MolecularFeatureSet)) {
      return false;
    }
    final MolecularFeatureSet featureSet = (MolecularFeatureSet) other;
    return Objects.equals(this.features, featureSet.features);
  }

  public int hashCode() {
    return this.features.hashCode();
  }
}

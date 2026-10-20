/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/** Количественный или бинарный признак молекулы для классического анализа. */
public final class MolecularFeature {

  private final MolecularFeatureKind kind;
  private final String label;
  private final long count;
  private final double confidence;

  private MolecularFeature(
      final MolecularFeatureKind kind,
      final String label,
      final long count,
      final double confidence) {
    this.kind = kind;
    this.label = label;
    this.count = count;
    this.confidence = confidence;
  }

  public static MolecularFeature of(
      final MolecularFeatureKind kind,
      final String label,
      final long count,
      final double confidence) {
    if (kind == null) {
      throw new IllegalArgumentException("Molecular feature kind must not be null.");
    }
    final String checkedLabel = TextValue.requireText(label, "Molecular feature label");
    if (count < 0L) {
      throw new IllegalArgumentException("Molecular feature count must not be negative.");
    }
    if (!Double.isFinite(confidence) || confidence < 0.0 || confidence > 1.0) {
      throw new IllegalArgumentException("Molecular feature confidence must be in [0, 1].");
    }
    return new MolecularFeature(kind, checkedLabel, count, confidence);
  }

  public MolecularFeatureKind kind() {
    return this.kind;
  }

  public String label() {
    return this.label;
  }

  public long count() {
    return this.count;
  }

  public double confidence() {
    return this.confidence;
  }

  public boolean present() {
    return this.count > 0L;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MolecularFeature)) {
      return false;
    }
    final MolecularFeature feature = (MolecularFeature) other;
    return this.count == feature.count
        && Double.compare(this.confidence, feature.confidence) == 0
        && this.kind == feature.kind
        && Objects.equals(this.label, feature.label);
  }

  public int hashCode() {
    return Objects.hash(this.kind, this.label, this.count, this.confidence);
  }
}

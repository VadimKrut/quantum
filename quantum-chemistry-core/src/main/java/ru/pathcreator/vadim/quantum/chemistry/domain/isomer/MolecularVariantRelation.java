/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.isomer;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.identity.MolecularComparisonResult;

public final class MolecularVariantRelation {

  private final String firstLabel;
  private final String secondLabel;
  private final MolecularComparisonResult comparison;

  private MolecularVariantRelation(
      final String firstLabel, final String secondLabel, final MolecularComparisonResult comparison) {
    this.firstLabel = firstLabel;
    this.secondLabel = secondLabel;
    this.comparison = comparison;
  }

  public static MolecularVariantRelation of(
      final String firstLabel, final String secondLabel, final MolecularComparisonResult comparison) {
    if (firstLabel == null || firstLabel.isBlank()) {
      throw new IllegalArgumentException("First variant relation label must not be blank.");
    }
    if (secondLabel == null || secondLabel.isBlank()) {
      throw new IllegalArgumentException("Second variant relation label must not be blank.");
    }
    if (firstLabel.equals(secondLabel)) {
      throw new IllegalArgumentException("Variant relation labels must be different.");
    }
    if (comparison == null) {
      throw new IllegalArgumentException("Variant relation comparison must not be null.");
    }
    return new MolecularVariantRelation(firstLabel, secondLabel, comparison);
  }

  public String firstLabel() {
    return this.firstLabel;
  }

  public String secondLabel() {
    return this.secondLabel;
  }

  public MolecularComparisonResult comparison() {
    return this.comparison;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MolecularVariantRelation)) {
      return false;
    }
    final MolecularVariantRelation relation = (MolecularVariantRelation) other;
    return Objects.equals(this.firstLabel, relation.firstLabel)
        && Objects.equals(this.secondLabel, relation.secondLabel)
        && Objects.equals(this.comparison, relation.comparison);
  }

  public int hashCode() {
    return Objects.hash(this.firstLabel, this.secondLabel, this.comparison);
  }
}
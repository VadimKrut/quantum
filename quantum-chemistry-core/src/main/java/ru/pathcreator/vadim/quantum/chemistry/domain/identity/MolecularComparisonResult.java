/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.identity;

import java.util.Objects;

public final class MolecularComparisonResult {

  private final MolecularRelationshipKind relationshipKind;
  private final boolean sameFormula;
  private final boolean sameConnectivity;
  private final boolean sameStereochemistry;
  private final boolean enantiomeric;
  private final boolean sameConformation;

  private MolecularComparisonResult(
      final MolecularRelationshipKind relationshipKind,
      final boolean sameFormula,
      final boolean sameConnectivity,
      final boolean sameStereochemistry,
      final boolean enantiomeric,
      final boolean sameConformation) {
    this.relationshipKind = relationshipKind;
    this.sameFormula = sameFormula;
    this.sameConnectivity = sameConnectivity;
    this.sameStereochemistry = sameStereochemistry;
    this.enantiomeric = enantiomeric;
    this.sameConformation = sameConformation;
  }

  public static MolecularComparisonResult of(
      final MolecularRelationshipKind relationshipKind,
      final boolean sameFormula,
      final boolean sameConnectivity,
      final boolean sameStereochemistry,
      final boolean enantiomeric,
      final boolean sameConformation) {
    if (relationshipKind == null) {
      throw new IllegalArgumentException("Molecular relationship kind must not be null.");
    }
    return new MolecularComparisonResult(
        relationshipKind,
        sameFormula,
        sameConnectivity,
        sameStereochemistry,
        enantiomeric,
        sameConformation);
  }

  public MolecularRelationshipKind relationshipKind() {
    return this.relationshipKind;
  }

  public boolean sameFormula() {
    return this.sameFormula;
  }

  public boolean sameConnectivity() {
    return this.sameConnectivity;
  }

  public boolean sameStereochemistry() {
    return this.sameStereochemistry;
  }

  public boolean enantiomeric() {
    return this.enantiomeric;
  }

  public boolean sameConformation() {
    return this.sameConformation;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MolecularComparisonResult)) {
      return false;
    }
    final MolecularComparisonResult result = (MolecularComparisonResult) other;
    return this.sameFormula == result.sameFormula
        && this.sameConnectivity == result.sameConnectivity
        && this.sameStereochemistry == result.sameStereochemistry
        && this.enantiomeric == result.enantiomeric
        && this.sameConformation == result.sameConformation
        && this.relationshipKind == result.relationshipKind;
  }

  public int hashCode() {
    return Objects.hash(
        new Object[] {
          this.relationshipKind,
          this.sameFormula,
          this.sameConnectivity,
          this.sameStereochemistry,
          this.enantiomeric,
          this.sameConformation
        });
  }
}
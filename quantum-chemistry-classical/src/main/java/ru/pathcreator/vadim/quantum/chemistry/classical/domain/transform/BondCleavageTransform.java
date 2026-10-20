/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;

/** Описывает разрыв существующей связи молекулярного графа без потери атомов исходной молекулы. */
public final class BondCleavageTransform {

  private final String idSuffix;
  private final String productNameSuffix;
  private final BondType sourceBondType;
  private final ReactantAtomSelector firstAtomSelector;
  private final ReactantAtomSelector secondAtomSelector;
  private final String explanation;

  private BondCleavageTransform(
      final String idSuffix,
      final String productNameSuffix,
      final BondType sourceBondType,
      final ReactantAtomSelector firstAtomSelector,
      final ReactantAtomSelector secondAtomSelector,
      final String explanation) {
    this.idSuffix = idSuffix;
    this.productNameSuffix = productNameSuffix;
    this.sourceBondType = sourceBondType;
    this.firstAtomSelector = firstAtomSelector;
    this.secondAtomSelector = secondAtomSelector;
    this.explanation = explanation;
  }

  public static BondCleavageTransform of(
      final String idSuffix,
      final String productNameSuffix,
      final BondType sourceBondType,
      final ReactantAtomSelector firstAtomSelector,
      final ReactantAtomSelector secondAtomSelector,
      final String explanation) {
    if (sourceBondType == null) {
      throw new IllegalArgumentException("Bond cleavage source bond type must not be null.");
    }
    if (firstAtomSelector == null) {
      throw new IllegalArgumentException("Bond cleavage first atom selector must not be null.");
    }
    if (secondAtomSelector == null) {
      throw new IllegalArgumentException("Bond cleavage second atom selector must not be null.");
    }
    return new BondCleavageTransform(
        TextValue.requireText(idSuffix, "Bond cleavage id suffix"),
        TextValue.requireText(productNameSuffix, "Bond cleavage product suffix"),
        sourceBondType,
        firstAtomSelector,
        secondAtomSelector,
        TextValue.requireText(explanation, "Bond cleavage explanation"));
  }

  public String idSuffix() {
    return this.idSuffix;
  }

  public String productNameSuffix() {
    return this.productNameSuffix;
  }

  public BondType sourceBondType() {
    return this.sourceBondType;
  }

  public ReactantAtomSelector firstAtomSelector() {
    return this.firstAtomSelector;
  }

  public ReactantAtomSelector secondAtomSelector() {
    return this.secondAtomSelector;
  }

  public String explanation() {
    return this.explanation;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof BondCleavageTransform)) {
      return false;
    }
    final BondCleavageTransform transform = (BondCleavageTransform) other;
    return Objects.equals(this.idSuffix, transform.idSuffix)
        && Objects.equals(this.productNameSuffix, transform.productNameSuffix)
        && this.sourceBondType == transform.sourceBondType
        && Objects.equals(this.firstAtomSelector, transform.firstAtomSelector)
        && Objects.equals(this.secondAtomSelector, transform.secondAtomSelector)
        && Objects.equals(this.explanation, transform.explanation);
  }

  public int hashCode() {
    return Objects.hash(
        this.idSuffix,
        this.productNameSuffix,
        this.sourceBondType,
        this.firstAtomSelector,
        this.secondAtomSelector,
        this.explanation);
  }
}

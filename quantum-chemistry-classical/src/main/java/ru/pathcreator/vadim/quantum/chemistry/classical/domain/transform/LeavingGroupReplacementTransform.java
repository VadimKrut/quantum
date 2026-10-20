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
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;

/** Описание замены одноатомной уходящей группы на новый одноатомный фрагмент. */
public final class LeavingGroupReplacementTransform {

  private final String idSuffix;
  private final String productNameSuffix;
  private final ElementSymbol leavingGroupElement;
  private final ElementSymbol replacementElement;
  private final BondType replacementBondType;
  private final ReactantAtomSelector anchorSelector;
  private final String explanation;

  private LeavingGroupReplacementTransform(
      final String idSuffix,
      final String productNameSuffix,
      final ElementSymbol leavingGroupElement,
      final ElementSymbol replacementElement,
      final BondType replacementBondType,
      final ReactantAtomSelector anchorSelector,
      final String explanation) {
    this.idSuffix = idSuffix;
    this.productNameSuffix = productNameSuffix;
    this.leavingGroupElement = leavingGroupElement;
    this.replacementElement = replacementElement;
    this.replacementBondType = replacementBondType;
    this.anchorSelector = anchorSelector;
    this.explanation = explanation;
  }

  public static LeavingGroupReplacementTransform of(
      final String idSuffix,
      final String productNameSuffix,
      final ElementSymbol leavingGroupElement,
      final ElementSymbol replacementElement,
      final BondType replacementBondType,
      final ReactantAtomSelector anchorSelector,
      final String explanation) {
    if (leavingGroupElement == null) {
      throw new IllegalArgumentException(
          "Leaving-group transform leaving element must not be null.");
    }
    if (replacementElement == null) {
      throw new IllegalArgumentException(
          "Leaving-group transform replacement element must not be null.");
    }
    if (replacementBondType == null) {
      throw new IllegalArgumentException(
          "Leaving-group transform replacement bond type must not be null.");
    }
    return new LeavingGroupReplacementTransform(
        TextValue.requireText(idSuffix, "Leaving-group transform id suffix"),
        TextValue.requireText(productNameSuffix, "Leaving-group transform product suffix"),
        leavingGroupElement,
        replacementElement,
        replacementBondType,
        anchorSelector == null ? ReactantAtomSelector.ANY : anchorSelector,
        TextValue.requireText(explanation, "Leaving-group transform explanation"));
  }

  public String idSuffix() {
    return this.idSuffix;
  }

  public String productNameSuffix() {
    return this.productNameSuffix;
  }

  public ElementSymbol leavingGroupElement() {
    return this.leavingGroupElement;
  }

  public ElementSymbol replacementElement() {
    return this.replacementElement;
  }

  public BondType replacementBondType() {
    return this.replacementBondType;
  }

  public ReactantAtomSelector anchorSelector() {
    return this.anchorSelector;
  }

  public String explanation() {
    return this.explanation;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof LeavingGroupReplacementTransform)) {
      return false;
    }
    final LeavingGroupReplacementTransform transform = (LeavingGroupReplacementTransform) other;
    return Objects.equals(this.idSuffix, transform.idSuffix)
        && Objects.equals(this.productNameSuffix, transform.productNameSuffix)
        && Objects.equals(this.leavingGroupElement, transform.leavingGroupElement)
        && Objects.equals(this.replacementElement, transform.replacementElement)
        && this.replacementBondType == transform.replacementBondType
        && Objects.equals(this.anchorSelector, transform.anchorSelector)
        && Objects.equals(this.explanation, transform.explanation);
  }

  public int hashCode() {
    return Objects.hash(
        this.idSuffix,
        this.productNameSuffix,
        this.leavingGroupElement,
        this.replacementElement,
        this.replacementBondType,
        this.anchorSelector,
        this.explanation);
  }
}

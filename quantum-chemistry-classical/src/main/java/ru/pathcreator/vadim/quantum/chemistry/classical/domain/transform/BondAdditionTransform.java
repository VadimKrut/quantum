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

/** Описывает присоединение двух атомных групп по двум концам существующей связи. */
public final class BondAdditionTransform {

  private final String idSuffix;
  private final String productNameSuffix;
  private final BondType sourceBondType;
  private final BondType productBondType;
  private final ReactantAtomSelector firstAnchorSelector;
  private final ReactantAtomSelector secondAnchorSelector;
  private final ElementSymbol firstAddedElement;
  private final ElementSymbol secondAddedElement;
  private final BondType firstAdditionBondType;
  private final BondType secondAdditionBondType;
  private final String explanation;

  private BondAdditionTransform(
      final String idSuffix,
      final String productNameSuffix,
      final BondType sourceBondType,
      final BondType productBondType,
      final ReactantAtomSelector firstAnchorSelector,
      final ReactantAtomSelector secondAnchorSelector,
      final ElementSymbol firstAddedElement,
      final ElementSymbol secondAddedElement,
      final BondType firstAdditionBondType,
      final BondType secondAdditionBondType,
      final String explanation) {
    this.idSuffix = idSuffix;
    this.productNameSuffix = productNameSuffix;
    this.sourceBondType = sourceBondType;
    this.productBondType = productBondType;
    this.firstAnchorSelector = firstAnchorSelector;
    this.secondAnchorSelector = secondAnchorSelector;
    this.firstAddedElement = firstAddedElement;
    this.secondAddedElement = secondAddedElement;
    this.firstAdditionBondType = firstAdditionBondType;
    this.secondAdditionBondType = secondAdditionBondType;
    this.explanation = explanation;
  }

  public static BondAdditionTransform of(
      final String idSuffix,
      final String productNameSuffix,
      final BondType sourceBondType,
      final BondType productBondType,
      final ReactantAtomSelector firstAnchorSelector,
      final ReactantAtomSelector secondAnchorSelector,
      final ElementSymbol firstAddedElement,
      final ElementSymbol secondAddedElement,
      final BondType firstAdditionBondType,
      final BondType secondAdditionBondType,
      final String explanation) {
    if (sourceBondType == null) {
      throw new IllegalArgumentException("Bond-addition source bond type must not be null.");
    }
    if (productBondType == null) {
      throw new IllegalArgumentException("Bond-addition product bond type must not be null.");
    }
    if (firstAnchorSelector == null || secondAnchorSelector == null) {
      throw new IllegalArgumentException("Bond-addition anchor selectors must not be null.");
    }
    if (firstAddedElement == null || secondAddedElement == null) {
      throw new IllegalArgumentException("Bond-addition added elements must not be null.");
    }
    if (firstAdditionBondType == null || secondAdditionBondType == null) {
      throw new IllegalArgumentException("Bond-addition new bond types must not be null.");
    }
    return new BondAdditionTransform(
        TextValue.requireText(idSuffix, "Bond-addition id suffix"),
        TextValue.requireText(productNameSuffix, "Bond-addition product suffix"),
        sourceBondType,
        productBondType,
        firstAnchorSelector,
        secondAnchorSelector,
        firstAddedElement,
        secondAddedElement,
        firstAdditionBondType,
        secondAdditionBondType,
        TextValue.requireText(explanation, "Bond-addition explanation"));
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

  public BondType productBondType() {
    return this.productBondType;
  }

  public ReactantAtomSelector firstAnchorSelector() {
    return this.firstAnchorSelector;
  }

  public ReactantAtomSelector secondAnchorSelector() {
    return this.secondAnchorSelector;
  }

  public ElementSymbol firstAddedElement() {
    return this.firstAddedElement;
  }

  public ElementSymbol secondAddedElement() {
    return this.secondAddedElement;
  }

  public BondType firstAdditionBondType() {
    return this.firstAdditionBondType;
  }

  public BondType secondAdditionBondType() {
    return this.secondAdditionBondType;
  }

  public String explanation() {
    return this.explanation;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof BondAdditionTransform)) {
      return false;
    }
    final BondAdditionTransform transform = (BondAdditionTransform) other;
    return Objects.equals(this.idSuffix, transform.idSuffix)
        && Objects.equals(this.productNameSuffix, transform.productNameSuffix)
        && this.sourceBondType == transform.sourceBondType
        && this.productBondType == transform.productBondType
        && Objects.equals(this.firstAnchorSelector, transform.firstAnchorSelector)
        && Objects.equals(this.secondAnchorSelector, transform.secondAnchorSelector)
        && Objects.equals(this.firstAddedElement, transform.firstAddedElement)
        && Objects.equals(this.secondAddedElement, transform.secondAddedElement)
        && this.firstAdditionBondType == transform.firstAdditionBondType
        && this.secondAdditionBondType == transform.secondAdditionBondType
        && Objects.equals(this.explanation, transform.explanation);
  }

  public int hashCode() {
    return Objects.hash(
        this.idSuffix,
        this.productNameSuffix,
        this.sourceBondType,
        this.productBondType,
        this.firstAnchorSelector,
        this.secondAnchorSelector,
        this.firstAddedElement,
        this.secondAddedElement,
        this.firstAdditionBondType,
        this.secondAdditionBondType,
        this.explanation);
  }
}

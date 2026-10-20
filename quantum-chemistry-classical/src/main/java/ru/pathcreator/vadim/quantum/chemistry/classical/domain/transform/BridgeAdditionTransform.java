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

/** Описывает добавление одного мостикового атома между двумя атомами существующей связи. */
public final class BridgeAdditionTransform {

  private final String idSuffix;
  private final String productNameSuffix;
  private final BondType sourceBondType;
  private final BondType productBondType;
  private final ReactantAtomSelector firstAnchorSelector;
  private final ReactantAtomSelector secondAnchorSelector;
  private final ElementSymbol bridgeElement;
  private final BondType firstBridgeBondType;
  private final BondType secondBridgeBondType;
  private final String explanation;

  private BridgeAdditionTransform(
      final String idSuffix,
      final String productNameSuffix,
      final BondType sourceBondType,
      final BondType productBondType,
      final ReactantAtomSelector firstAnchorSelector,
      final ReactantAtomSelector secondAnchorSelector,
      final ElementSymbol bridgeElement,
      final BondType firstBridgeBondType,
      final BondType secondBridgeBondType,
      final String explanation) {
    this.idSuffix = idSuffix;
    this.productNameSuffix = productNameSuffix;
    this.sourceBondType = sourceBondType;
    this.productBondType = productBondType;
    this.firstAnchorSelector = firstAnchorSelector;
    this.secondAnchorSelector = secondAnchorSelector;
    this.bridgeElement = bridgeElement;
    this.firstBridgeBondType = firstBridgeBondType;
    this.secondBridgeBondType = secondBridgeBondType;
    this.explanation = explanation;
  }

  public static BridgeAdditionTransform of(
      final String idSuffix,
      final String productNameSuffix,
      final BondType sourceBondType,
      final BondType productBondType,
      final ReactantAtomSelector firstAnchorSelector,
      final ReactantAtomSelector secondAnchorSelector,
      final ElementSymbol bridgeElement,
      final BondType firstBridgeBondType,
      final BondType secondBridgeBondType,
      final String explanation) {
    if (sourceBondType == null) {
      throw new IllegalArgumentException("Bridge-addition source bond type must not be null.");
    }
    if (productBondType == null) {
      throw new IllegalArgumentException("Bridge-addition product bond type must not be null.");
    }
    if (firstAnchorSelector == null || secondAnchorSelector == null) {
      throw new IllegalArgumentException("Bridge-addition anchor selectors must not be null.");
    }
    if (bridgeElement == null) {
      throw new IllegalArgumentException("Bridge-addition bridge element must not be null.");
    }
    if (firstBridgeBondType == null || secondBridgeBondType == null) {
      throw new IllegalArgumentException("Bridge-addition bridge bond types must not be null.");
    }
    return new BridgeAdditionTransform(
        TextValue.requireText(idSuffix, "Bridge-addition id suffix"),
        TextValue.requireText(productNameSuffix, "Bridge-addition product suffix"),
        sourceBondType,
        productBondType,
        firstAnchorSelector,
        secondAnchorSelector,
        bridgeElement,
        firstBridgeBondType,
        secondBridgeBondType,
        TextValue.requireText(explanation, "Bridge-addition explanation"));
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

  public ElementSymbol bridgeElement() {
    return this.bridgeElement;
  }

  public BondType firstBridgeBondType() {
    return this.firstBridgeBondType;
  }

  public BondType secondBridgeBondType() {
    return this.secondBridgeBondType;
  }

  public String explanation() {
    return this.explanation;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof BridgeAdditionTransform)) {
      return false;
    }
    final BridgeAdditionTransform transform = (BridgeAdditionTransform) other;
    return Objects.equals(this.idSuffix, transform.idSuffix)
        && Objects.equals(this.productNameSuffix, transform.productNameSuffix)
        && this.sourceBondType == transform.sourceBondType
        && this.productBondType == transform.productBondType
        && Objects.equals(this.firstAnchorSelector, transform.firstAnchorSelector)
        && Objects.equals(this.secondAnchorSelector, transform.secondAnchorSelector)
        && Objects.equals(this.bridgeElement, transform.bridgeElement)
        && this.firstBridgeBondType == transform.firstBridgeBondType
        && this.secondBridgeBondType == transform.secondBridgeBondType
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
        this.bridgeElement,
        this.firstBridgeBondType,
        this.secondBridgeBondType,
        this.explanation);
  }
}

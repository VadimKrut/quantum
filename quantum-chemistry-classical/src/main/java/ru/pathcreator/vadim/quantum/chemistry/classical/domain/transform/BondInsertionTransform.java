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

/** Описывает вставку одного атома внутрь существующей связи с заменой связи двумя новыми. */
public final class BondInsertionTransform {

  private final String idSuffix;
  private final String productNameSuffix;
  private final BondType sourceBondType;
  private final ReactantAtomSelector firstAnchorSelector;
  private final ReactantAtomSelector secondAnchorSelector;
  private final ElementSymbol insertedElement;
  private final BondType firstInsertedBondType;
  private final BondType secondInsertedBondType;
  private final String explanation;

  private BondInsertionTransform(
      final String idSuffix,
      final String productNameSuffix,
      final BondType sourceBondType,
      final ReactantAtomSelector firstAnchorSelector,
      final ReactantAtomSelector secondAnchorSelector,
      final ElementSymbol insertedElement,
      final BondType firstInsertedBondType,
      final BondType secondInsertedBondType,
      final String explanation) {
    this.idSuffix = idSuffix;
    this.productNameSuffix = productNameSuffix;
    this.sourceBondType = sourceBondType;
    this.firstAnchorSelector = firstAnchorSelector;
    this.secondAnchorSelector = secondAnchorSelector;
    this.insertedElement = insertedElement;
    this.firstInsertedBondType = firstInsertedBondType;
    this.secondInsertedBondType = secondInsertedBondType;
    this.explanation = explanation;
  }

  public static BondInsertionTransform of(
      final String idSuffix,
      final String productNameSuffix,
      final BondType sourceBondType,
      final ReactantAtomSelector firstAnchorSelector,
      final ReactantAtomSelector secondAnchorSelector,
      final ElementSymbol insertedElement,
      final BondType firstInsertedBondType,
      final BondType secondInsertedBondType,
      final String explanation) {
    if (sourceBondType == null) {
      throw new IllegalArgumentException("Bond-insertion source bond type must not be null.");
    }
    if (firstAnchorSelector == null || secondAnchorSelector == null) {
      throw new IllegalArgumentException("Bond-insertion anchor selectors must not be null.");
    }
    if (insertedElement == null) {
      throw new IllegalArgumentException("Bond-insertion inserted element must not be null.");
    }
    if (firstInsertedBondType == null || secondInsertedBondType == null) {
      throw new IllegalArgumentException("Bond-insertion new bond types must not be null.");
    }
    return new BondInsertionTransform(
        TextValue.requireText(idSuffix, "Bond-insertion id suffix"),
        TextValue.requireText(productNameSuffix, "Bond-insertion product suffix"),
        sourceBondType,
        firstAnchorSelector,
        secondAnchorSelector,
        insertedElement,
        firstInsertedBondType,
        secondInsertedBondType,
        TextValue.requireText(explanation, "Bond-insertion explanation"));
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

  public ReactantAtomSelector firstAnchorSelector() {
    return this.firstAnchorSelector;
  }

  public ReactantAtomSelector secondAnchorSelector() {
    return this.secondAnchorSelector;
  }

  public ElementSymbol insertedElement() {
    return this.insertedElement;
  }

  public BondType firstInsertedBondType() {
    return this.firstInsertedBondType;
  }

  public BondType secondInsertedBondType() {
    return this.secondInsertedBondType;
  }

  public String explanation() {
    return this.explanation;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof BondInsertionTransform)) {
      return false;
    }
    final BondInsertionTransform transform = (BondInsertionTransform) other;
    return Objects.equals(this.idSuffix, transform.idSuffix)
        && Objects.equals(this.productNameSuffix, transform.productNameSuffix)
        && this.sourceBondType == transform.sourceBondType
        && Objects.equals(this.firstAnchorSelector, transform.firstAnchorSelector)
        && Objects.equals(this.secondAnchorSelector, transform.secondAnchorSelector)
        && Objects.equals(this.insertedElement, transform.insertedElement)
        && this.firstInsertedBondType == transform.firstInsertedBondType
        && this.secondInsertedBondType == transform.secondInsertedBondType
        && Objects.equals(this.explanation, transform.explanation);
  }

  public int hashCode() {
    return Objects.hash(
        this.idSuffix,
        this.productNameSuffix,
        this.sourceBondType,
        this.firstAnchorSelector,
        this.secondAnchorSelector,
        this.insertedElement,
        this.firstInsertedBondType,
        this.secondInsertedBondType,
        this.explanation);
  }
}

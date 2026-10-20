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

/** Описание добавления одноатомного заместителя к выбранному атому реагента. */
public final class SubstituentAttachmentTransform {

  private final String idSuffix;
  private final String productNameSuffix;
  private final ElementSymbol substituentElement;
  private final BondType bondType;
  private final ReactantAtomSelector targetSelector;
  private final String explanation;

  private SubstituentAttachmentTransform(
      final String idSuffix,
      final String productNameSuffix,
      final ElementSymbol substituentElement,
      final BondType bondType,
      final ReactantAtomSelector targetSelector,
      final String explanation) {
    this.idSuffix = idSuffix;
    this.productNameSuffix = productNameSuffix;
    this.substituentElement = substituentElement;
    this.bondType = bondType;
    this.targetSelector = targetSelector;
    this.explanation = explanation;
  }

  public static SubstituentAttachmentTransform of(
      final String idSuffix,
      final String productNameSuffix,
      final ElementSymbol substituentElement,
      final BondType bondType,
      final ReactantAtomSelector targetSelector,
      final String explanation) {
    if (substituentElement == null) {
      throw new IllegalArgumentException("Substituent transform element must not be null.");
    }
    if (bondType == null) {
      throw new IllegalArgumentException("Substituent transform bond type must not be null.");
    }
    return new SubstituentAttachmentTransform(
        TextValue.requireText(idSuffix, "Substituent transform id suffix"),
        TextValue.requireText(productNameSuffix, "Substituent transform product suffix"),
        substituentElement,
        bondType,
        targetSelector == null ? ReactantAtomSelector.ANY : targetSelector,
        TextValue.requireText(explanation, "Substituent transform explanation"));
  }

  public String idSuffix() {
    return this.idSuffix;
  }

  public String productNameSuffix() {
    return this.productNameSuffix;
  }

  public ElementSymbol substituentElement() {
    return this.substituentElement;
  }

  public BondType bondType() {
    return this.bondType;
  }

  public ReactantAtomSelector targetSelector() {
    return this.targetSelector;
  }

  public String explanation() {
    return this.explanation;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof SubstituentAttachmentTransform)) {
      return false;
    }
    final SubstituentAttachmentTransform transform = (SubstituentAttachmentTransform) other;
    return Objects.equals(this.idSuffix, transform.idSuffix)
        && Objects.equals(this.productNameSuffix, transform.productNameSuffix)
        && Objects.equals(this.substituentElement, transform.substituentElement)
        && this.bondType == transform.bondType
        && Objects.equals(this.targetSelector, transform.targetSelector)
        && Objects.equals(this.explanation, transform.explanation);
  }

  public int hashCode() {
    return Objects.hash(
        this.idSuffix,
        this.productNameSuffix,
        this.substituentElement,
        this.bondType,
        this.targetSelector,
        this.explanation);
  }
}

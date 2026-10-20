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

/** Описывает внутримолекулярное образование новой связи без привязки к частной внешней базе реакций. */
public final class IntramolecularBondFormationTransform {

  private final String idSuffix;
  private final String productNameSuffix;
  private final ReactantAtomSelector firstAtomSelector;
  private final ReactantAtomSelector secondAtomSelector;
  private final BondType formedBondType;
  private final String explanation;

  private IntramolecularBondFormationTransform(
      final String idSuffix,
      final String productNameSuffix,
      final ReactantAtomSelector firstAtomSelector,
      final ReactantAtomSelector secondAtomSelector,
      final BondType formedBondType,
      final String explanation) {
    this.idSuffix = idSuffix;
    this.productNameSuffix = productNameSuffix;
    this.firstAtomSelector = firstAtomSelector;
    this.secondAtomSelector = secondAtomSelector;
    this.formedBondType = formedBondType;
    this.explanation = explanation;
  }

  public static IntramolecularBondFormationTransform of(
      final String idSuffix,
      final String productNameSuffix,
      final ReactantAtomSelector firstAtomSelector,
      final ReactantAtomSelector secondAtomSelector,
      final BondType formedBondType,
      final String explanation) {
    if (firstAtomSelector == null) {
      throw new IllegalArgumentException(
          "Intramolecular bond formation first atom selector must not be null.");
    }
    if (secondAtomSelector == null) {
      throw new IllegalArgumentException(
          "Intramolecular bond formation second atom selector must not be null.");
    }
    if (formedBondType == null) {
      throw new IllegalArgumentException(
          "Intramolecular bond formation bond type must not be null.");
    }
    return new IntramolecularBondFormationTransform(
        TextValue.requireText(idSuffix, "Intramolecular bond formation id suffix"),
        TextValue.requireText(productNameSuffix, "Intramolecular bond formation product suffix"),
        firstAtomSelector,
        secondAtomSelector,
        formedBondType,
        TextValue.requireText(explanation, "Intramolecular bond formation explanation"));
  }

  public String idSuffix() {
    return this.idSuffix;
  }

  public String productNameSuffix() {
    return this.productNameSuffix;
  }

  public ReactantAtomSelector firstAtomSelector() {
    return this.firstAtomSelector;
  }

  public ReactantAtomSelector secondAtomSelector() {
    return this.secondAtomSelector;
  }

  public BondType formedBondType() {
    return this.formedBondType;
  }

  public String explanation() {
    return this.explanation;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof IntramolecularBondFormationTransform)) {
      return false;
    }
    final IntramolecularBondFormationTransform transform =
        (IntramolecularBondFormationTransform) other;
    return Objects.equals(this.idSuffix, transform.idSuffix)
        && Objects.equals(this.productNameSuffix, transform.productNameSuffix)
        && Objects.equals(this.firstAtomSelector, transform.firstAtomSelector)
        && Objects.equals(this.secondAtomSelector, transform.secondAtomSelector)
        && this.formedBondType == transform.formedBondType
        && Objects.equals(this.explanation, transform.explanation);
  }

  public int hashCode() {
    return Objects.hash(
        this.idSuffix,
        this.productNameSuffix,
        this.firstAtomSelector,
        this.secondAtomSelector,
        this.formedBondType,
        this.explanation);
  }
}

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

/** Описывает графовую beta-elimination: удалить leaving group и повысить соседнюю связь до кратной. */
public final class BetaEliminationTransform {

  private final String idSuffix;
  private final String productNameSuffix;
  private final ReactantAtomSelector alphaAnchorSelector;
  private final ReactantAtomSelector leavingGroupSelector;
  private final ReactantAtomSelector betaAnchorSelector;
  private final BondType alphaBetaSourceBondType;
  private final BondType alphaBetaProductBondType;
  private final String explanation;

  private BetaEliminationTransform(
      final String idSuffix,
      final String productNameSuffix,
      final ReactantAtomSelector alphaAnchorSelector,
      final ReactantAtomSelector leavingGroupSelector,
      final ReactantAtomSelector betaAnchorSelector,
      final BondType alphaBetaSourceBondType,
      final BondType alphaBetaProductBondType,
      final String explanation) {
    this.idSuffix = idSuffix;
    this.productNameSuffix = productNameSuffix;
    this.alphaAnchorSelector = alphaAnchorSelector;
    this.leavingGroupSelector = leavingGroupSelector;
    this.betaAnchorSelector = betaAnchorSelector;
    this.alphaBetaSourceBondType = alphaBetaSourceBondType;
    this.alphaBetaProductBondType = alphaBetaProductBondType;
    this.explanation = explanation;
  }

  public static BetaEliminationTransform of(
      final String idSuffix,
      final String productNameSuffix,
      final ReactantAtomSelector alphaAnchorSelector,
      final ReactantAtomSelector leavingGroupSelector,
      final ReactantAtomSelector betaAnchorSelector,
      final BondType alphaBetaSourceBondType,
      final BondType alphaBetaProductBondType,
      final String explanation) {
    if (alphaAnchorSelector == null || leavingGroupSelector == null || betaAnchorSelector == null) {
      throw new IllegalArgumentException("Beta-elimination selectors must not be null.");
    }
    if (alphaBetaSourceBondType == null || alphaBetaProductBondType == null) {
      throw new IllegalArgumentException("Beta-elimination bond types must not be null.");
    }
    return new BetaEliminationTransform(
        TextValue.requireText(idSuffix, "Beta-elimination id suffix"),
        TextValue.requireText(productNameSuffix, "Beta-elimination product suffix"),
        alphaAnchorSelector,
        leavingGroupSelector,
        betaAnchorSelector,
        alphaBetaSourceBondType,
        alphaBetaProductBondType,
        TextValue.requireText(explanation, "Beta-elimination explanation"));
  }

  public String idSuffix() {
    return this.idSuffix;
  }

  public String productNameSuffix() {
    return this.productNameSuffix;
  }

  public ReactantAtomSelector alphaAnchorSelector() {
    return this.alphaAnchorSelector;
  }

  public ReactantAtomSelector leavingGroupSelector() {
    return this.leavingGroupSelector;
  }

  public ReactantAtomSelector betaAnchorSelector() {
    return this.betaAnchorSelector;
  }

  public BondType alphaBetaSourceBondType() {
    return this.alphaBetaSourceBondType;
  }

  public BondType alphaBetaProductBondType() {
    return this.alphaBetaProductBondType;
  }

  public String explanation() {
    return this.explanation;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof BetaEliminationTransform)) {
      return false;
    }
    final BetaEliminationTransform transform = (BetaEliminationTransform) other;
    return Objects.equals(this.idSuffix, transform.idSuffix)
        && Objects.equals(this.productNameSuffix, transform.productNameSuffix)
        && Objects.equals(this.alphaAnchorSelector, transform.alphaAnchorSelector)
        && Objects.equals(this.leavingGroupSelector, transform.leavingGroupSelector)
        && Objects.equals(this.betaAnchorSelector, transform.betaAnchorSelector)
        && this.alphaBetaSourceBondType == transform.alphaBetaSourceBondType
        && this.alphaBetaProductBondType == transform.alphaBetaProductBondType
        && Objects.equals(this.explanation, transform.explanation);
  }

  public int hashCode() {
    return Objects.hash(
        this.idSuffix,
        this.productNameSuffix,
        this.alphaAnchorSelector,
        this.leavingGroupSelector,
        this.betaAnchorSelector,
        this.alphaBetaSourceBondType,
        this.alphaBetaProductBondType,
        this.explanation);
  }
}

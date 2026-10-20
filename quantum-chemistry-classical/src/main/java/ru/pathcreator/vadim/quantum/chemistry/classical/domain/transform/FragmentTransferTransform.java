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

/** Описывает перенос фрагмента между двумя реагентами без привязки к конкретной внешней базе реакций. */
public final class FragmentTransferTransform {

  private final String idSuffix;
  private final String productNameSuffix;
  private final ReactantAtomSelector scaffoldAnchorSelector;
  private final ReactantAtomSelector scaffoldLeavingSelector;
  private final ReactantAtomSelector fragmentAnchorSelector;
  private final ReactantAtomSelector fragmentLeavingSelector;
  private final BondType connectingBondType;
  private final String coproductNameSuffix;
  private final BondType coproductBondType;
  private final String explanation;

  private FragmentTransferTransform(
      final String idSuffix,
      final String productNameSuffix,
      final ReactantAtomSelector scaffoldAnchorSelector,
      final ReactantAtomSelector scaffoldLeavingSelector,
      final ReactantAtomSelector fragmentAnchorSelector,
      final ReactantAtomSelector fragmentLeavingSelector,
      final BondType connectingBondType,
      final String coproductNameSuffix,
      final BondType coproductBondType,
      final String explanation) {
    this.idSuffix = idSuffix;
    this.productNameSuffix = productNameSuffix;
    this.scaffoldAnchorSelector = scaffoldAnchorSelector;
    this.scaffoldLeavingSelector = scaffoldLeavingSelector;
    this.fragmentAnchorSelector = fragmentAnchorSelector;
    this.fragmentLeavingSelector = fragmentLeavingSelector;
    this.connectingBondType = connectingBondType;
    this.coproductNameSuffix = coproductNameSuffix;
    this.coproductBondType = coproductBondType;
    this.explanation = explanation;
  }

  public static FragmentTransferTransform of(
      final String idSuffix,
      final String productNameSuffix,
      final ReactantAtomSelector scaffoldAnchorSelector,
      final ReactantAtomSelector scaffoldLeavingSelector,
      final ReactantAtomSelector fragmentAnchorSelector,
      final ReactantAtomSelector fragmentLeavingSelector,
      final BondType connectingBondType,
      final String explanation) {
    if (scaffoldAnchorSelector == null) {
      throw new IllegalArgumentException(
          "Fragment transfer scaffold anchor selector must not be null.");
    }
    if (fragmentAnchorSelector == null) {
      throw new IllegalArgumentException(
          "Fragment transfer fragment anchor selector must not be null.");
    }
    if (connectingBondType == null) {
      throw new IllegalArgumentException(
          "Fragment transfer connecting bond type must not be null.");
    }
    return new FragmentTransferTransform(
        TextValue.requireText(idSuffix, "Fragment transfer id suffix"),
        TextValue.requireText(productNameSuffix, "Fragment transfer product suffix"),
        scaffoldAnchorSelector,
        scaffoldLeavingSelector == null ? ReactantAtomSelector.ANY : scaffoldLeavingSelector,
        fragmentAnchorSelector,
        fragmentLeavingSelector == null ? ReactantAtomSelector.ANY : fragmentLeavingSelector,
        connectingBondType,
        null,
        null,
        TextValue.requireText(explanation, "Fragment transfer explanation"));
  }

  public static FragmentTransferTransform withCoproduct(
      final String idSuffix,
      final String productNameSuffix,
      final ReactantAtomSelector scaffoldAnchorSelector,
      final ReactantAtomSelector scaffoldLeavingSelector,
      final ReactantAtomSelector fragmentAnchorSelector,
      final ReactantAtomSelector fragmentLeavingSelector,
      final BondType connectingBondType,
      final String coproductNameSuffix,
      final BondType coproductBondType,
      final String explanation) {
    if (coproductBondType == null) {
      throw new IllegalArgumentException("Fragment transfer coproduct bond type must not be null.");
    }
    if (scaffoldAnchorSelector == null) {
      throw new IllegalArgumentException(
          "Fragment transfer scaffold anchor selector must not be null.");
    }
    if (fragmentAnchorSelector == null) {
      throw new IllegalArgumentException(
          "Fragment transfer fragment anchor selector must not be null.");
    }
    if (connectingBondType == null) {
      throw new IllegalArgumentException(
          "Fragment transfer connecting bond type must not be null.");
    }
    return new FragmentTransferTransform(
        TextValue.requireText(idSuffix, "Fragment transfer id suffix"),
        TextValue.requireText(productNameSuffix, "Fragment transfer product suffix"),
        scaffoldAnchorSelector,
        scaffoldLeavingSelector == null ? ReactantAtomSelector.ANY : scaffoldLeavingSelector,
        fragmentAnchorSelector,
        fragmentLeavingSelector == null ? ReactantAtomSelector.ANY : fragmentLeavingSelector,
        connectingBondType,
        TextValue.requireText(coproductNameSuffix, "Fragment transfer coproduct suffix"),
        coproductBondType,
        TextValue.requireText(explanation, "Fragment transfer explanation"));
  }

  public String idSuffix() {
    return this.idSuffix;
  }

  public String productNameSuffix() {
    return this.productNameSuffix;
  }

  public ReactantAtomSelector scaffoldAnchorSelector() {
    return this.scaffoldAnchorSelector;
  }

  public ReactantAtomSelector scaffoldLeavingSelector() {
    return this.scaffoldLeavingSelector;
  }

  public ReactantAtomSelector fragmentAnchorSelector() {
    return this.fragmentAnchorSelector;
  }

  public ReactantAtomSelector fragmentLeavingSelector() {
    return this.fragmentLeavingSelector;
  }

  public BondType connectingBondType() {
    return this.connectingBondType;
  }

  public String coproductNameSuffix() {
    return this.coproductNameSuffix;
  }

  public BondType coproductBondType() {
    return this.coproductBondType;
  }

  public boolean hasCoproduct() {
    return this.coproductNameSuffix != null;
  }

  public String explanation() {
    return this.explanation;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof FragmentTransferTransform)) {
      return false;
    }
    final FragmentTransferTransform transform = (FragmentTransferTransform) other;
    return Objects.equals(this.idSuffix, transform.idSuffix)
        && Objects.equals(this.productNameSuffix, transform.productNameSuffix)
        && Objects.equals(this.scaffoldAnchorSelector, transform.scaffoldAnchorSelector)
        && Objects.equals(this.scaffoldLeavingSelector, transform.scaffoldLeavingSelector)
        && Objects.equals(this.fragmentAnchorSelector, transform.fragmentAnchorSelector)
        && Objects.equals(this.fragmentLeavingSelector, transform.fragmentLeavingSelector)
        && this.connectingBondType == transform.connectingBondType
        && Objects.equals(this.coproductNameSuffix, transform.coproductNameSuffix)
        && this.coproductBondType == transform.coproductBondType
        && Objects.equals(this.explanation, transform.explanation);
  }

  public int hashCode() {
    return Objects.hash(
        this.idSuffix,
        this.productNameSuffix,
        this.scaffoldAnchorSelector,
        this.scaffoldLeavingSelector,
        this.fragmentAnchorSelector,
        this.fragmentLeavingSelector,
        this.connectingBondType,
        this.coproductNameSuffix,
        this.coproductBondType,
        this.explanation);
  }
}

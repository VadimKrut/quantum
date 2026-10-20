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

/** Описывает перенос фрагмента из второго реагента к атому первого реагента без удаления атома из первого графа. */
public final class FragmentAttachmentTransform {

  private final String idSuffix;
  private final String productNameSuffix;
  private final ReactantAtomSelector scaffoldAnchorSelector;
  private final ReactantAtomSelector fragmentAnchorSelector;
  private final ReactantAtomSelector fragmentLeavingSelector;
  private final BondType connectingBondType;
  private final String explanation;

  private FragmentAttachmentTransform(
      final String idSuffix,
      final String productNameSuffix,
      final ReactantAtomSelector scaffoldAnchorSelector,
      final ReactantAtomSelector fragmentAnchorSelector,
      final ReactantAtomSelector fragmentLeavingSelector,
      final BondType connectingBondType,
      final String explanation) {
    this.idSuffix = idSuffix;
    this.productNameSuffix = productNameSuffix;
    this.scaffoldAnchorSelector = scaffoldAnchorSelector;
    this.fragmentAnchorSelector = fragmentAnchorSelector;
    this.fragmentLeavingSelector = fragmentLeavingSelector;
    this.connectingBondType = connectingBondType;
    this.explanation = explanation;
  }

  public static FragmentAttachmentTransform of(
      final String idSuffix,
      final String productNameSuffix,
      final ReactantAtomSelector scaffoldAnchorSelector,
      final ReactantAtomSelector fragmentAnchorSelector,
      final ReactantAtomSelector fragmentLeavingSelector,
      final BondType connectingBondType,
      final String explanation) {
    if (scaffoldAnchorSelector == null) {
      throw new IllegalArgumentException(
          "Fragment attachment scaffold anchor selector must not be null.");
    }
    if (fragmentAnchorSelector == null) {
      throw new IllegalArgumentException(
          "Fragment attachment fragment anchor selector must not be null.");
    }
    if (connectingBondType == null) {
      throw new IllegalArgumentException(
          "Fragment attachment connecting bond type must not be null.");
    }
    return new FragmentAttachmentTransform(
        TextValue.requireText(idSuffix, "Fragment attachment id suffix"),
        TextValue.requireText(productNameSuffix, "Fragment attachment product suffix"),
        scaffoldAnchorSelector,
        fragmentAnchorSelector,
        fragmentLeavingSelector == null ? ReactantAtomSelector.ANY : fragmentLeavingSelector,
        connectingBondType,
        TextValue.requireText(explanation, "Fragment attachment explanation"));
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

  public ReactantAtomSelector fragmentAnchorSelector() {
    return this.fragmentAnchorSelector;
  }

  public ReactantAtomSelector fragmentLeavingSelector() {
    return this.fragmentLeavingSelector;
  }

  public BondType connectingBondType() {
    return this.connectingBondType;
  }

  public String explanation() {
    return this.explanation;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof FragmentAttachmentTransform)) {
      return false;
    }
    final FragmentAttachmentTransform transform = (FragmentAttachmentTransform) other;
    return Objects.equals(this.idSuffix, transform.idSuffix)
        && Objects.equals(this.productNameSuffix, transform.productNameSuffix)
        && Objects.equals(this.scaffoldAnchorSelector, transform.scaffoldAnchorSelector)
        && Objects.equals(this.fragmentAnchorSelector, transform.fragmentAnchorSelector)
        && Objects.equals(this.fragmentLeavingSelector, transform.fragmentLeavingSelector)
        && this.connectingBondType == transform.connectingBondType
        && Objects.equals(this.explanation, transform.explanation);
  }

  public int hashCode() {
    return Objects.hash(
        this.idSuffix,
        this.productNameSuffix,
        this.scaffoldAnchorSelector,
        this.fragmentAnchorSelector,
        this.fragmentLeavingSelector,
        this.connectingBondType,
        this.explanation);
  }
}

/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.transform;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** Продукт структурного преобразования вместе с трассировкой выбранного реакционного участка. */
public final class MolecularTransformProduct {

  private final Molecule product;
  private final List<Molecule> coproducts;
  private final AtomId anchorAtomId;
  private final AtomId partnerAtomId;
  private final AtomId leavingGroupAtomId;

  private MolecularTransformProduct(
      final Molecule product,
      final List<Molecule> coproducts,
      final AtomId anchorAtomId,
      final AtomId partnerAtomId,
      final AtomId leavingGroupAtomId) {
    this.product = product;
    this.coproducts = coproducts;
    this.anchorAtomId = anchorAtomId;
    this.partnerAtomId = partnerAtomId;
    this.leavingGroupAtomId = leavingGroupAtomId;
  }

  public static MolecularTransformProduct of(
      final Molecule product, final AtomId anchorAtomId, final AtomId leavingGroupAtomId) {
    if (product == null) {
      throw new IllegalArgumentException("Molecular transform product must not be null.");
    }
    if (anchorAtomId == null) {
      throw new IllegalArgumentException("Molecular transform anchor atom id must not be null.");
    }
    return new MolecularTransformProduct(
        product, List.of(), anchorAtomId, null, leavingGroupAtomId);
  }

  public static MolecularTransformProduct ofWithPartner(
      final Molecule product, final AtomId anchorAtomId, final AtomId partnerAtomId) {
    if (product == null) {
      throw new IllegalArgumentException("Molecular transform product must not be null.");
    }
    if (anchorAtomId == null) {
      throw new IllegalArgumentException("Molecular transform anchor atom id must not be null.");
    }
    if (partnerAtomId == null) {
      throw new IllegalArgumentException("Molecular transform partner atom id must not be null.");
    }
    return new MolecularTransformProduct(product, List.of(), anchorAtomId, partnerAtomId, null);
  }

  public static MolecularTransformProduct of(
      final Molecule product,
      final List<Molecule> coproducts,
      final AtomId anchorAtomId,
      final AtomId leavingGroupAtomId) {
    if (product == null) {
      throw new IllegalArgumentException("Molecular transform product must not be null.");
    }
    if (coproducts == null) {
      throw new IllegalArgumentException("Molecular transform coproduct list must not be null.");
    }
    for (int i = 0; i < coproducts.size(); ++i) {
      if (coproducts.get(i) == null) {
        throw new IllegalArgumentException(
            "Molecular transform coproduct list must not contain null.");
      }
    }
    if (anchorAtomId == null) {
      throw new IllegalArgumentException("Molecular transform anchor atom id must not be null.");
    }
    return new MolecularTransformProduct(
        product, List.copyOf(coproducts), anchorAtomId, null, leavingGroupAtomId);
  }

  public Molecule product() {
    return this.product;
  }

  public List<Molecule> coproducts() {
    return this.coproducts;
  }

  public boolean hasCoproducts() {
    return !this.coproducts.isEmpty();
  }

  public AtomId anchorAtomId() {
    return this.anchorAtomId;
  }

  public AtomId partnerAtomId() {
    return this.partnerAtomId;
  }

  public boolean hasPartnerAtom() {
    return this.partnerAtomId != null;
  }

  public AtomId leavingGroupAtomId() {
    return this.leavingGroupAtomId;
  }

  public boolean hasLeavingGroupAtom() {
    return this.leavingGroupAtomId != null;
  }

  public String mappingDescription() {
    if (this.partnerAtomId != null && this.leavingGroupAtomId != null) {
      return "anchor="
          + this.anchorAtomId.value()
          + ", partner="
          + this.partnerAtomId.value()
          + ", leavingGroup="
          + this.leavingGroupAtomId.value();
    }
    if (this.partnerAtomId != null) {
      return "anchor=" + this.anchorAtomId.value() + ", partner=" + this.partnerAtomId.value();
    }
    if (this.leavingGroupAtomId != null) {
      return "anchor="
          + this.anchorAtomId.value()
          + ", leavingGroup="
          + this.leavingGroupAtomId.value();
    }
    return "anchor=" + this.anchorAtomId.value();
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MolecularTransformProduct)) {
      return false;
    }
    final MolecularTransformProduct product = (MolecularTransformProduct) other;
    return Objects.equals(this.product, product.product)
        && Objects.equals(this.coproducts, product.coproducts)
        && Objects.equals(this.anchorAtomId, product.anchorAtomId)
        && Objects.equals(this.partnerAtomId, product.partnerAtomId)
        && Objects.equals(this.leavingGroupAtomId, product.leavingGroupAtomId);
  }

  public int hashCode() {
    return Objects.hash(
        this.product,
        this.coproducts,
        this.anchorAtomId,
        this.partnerAtomId,
        this.leavingGroupAtomId);
  }
}

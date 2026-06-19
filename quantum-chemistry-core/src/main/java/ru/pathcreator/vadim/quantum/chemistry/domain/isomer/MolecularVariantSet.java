/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.isomer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.formula.MolecularFormula;
import ru.pathcreator.vadim.quantum.chemistry.domain.identity.MolecularComparisonResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.identity.MolecularIdentityComparator;

public final class MolecularVariantSet {

  private final String id;
  private final MolecularFormula formula;
  private final List<MolecularVariant> variants;

  private MolecularVariantSet(
      final String id, final MolecularFormula formula, final List<MolecularVariant> variants) {
    this.id = id;
    this.formula = formula;
    this.variants = variants;
  }

  public static MolecularVariantSet of(
      final String id,
      final List<MolecularVariant> variants
  ) {
    final String checkedId = IdentifierValue.requireIdentifier(id, "Molecular variant set id");
    final List<MolecularVariant> checkedVariants =
        List.copyOf(MolecularVariantSet.requireVariants(variants));
    final MolecularFormula checkedFormula = checkedVariants.get(0).molecule().formula();
    for (int i = 1; i < checkedVariants.size(); ++i) {
      if (checkedFormula.equals(checkedVariants.get(i).molecule().formula())) continue;
      throw new IllegalArgumentException(
          "Molecular variant set must contain one molecular formula.");
    }
    return new MolecularVariantSet(checkedId, checkedFormula, checkedVariants);
  }

  public String id() {
    return this.id;
  }

  public MolecularFormula formula() {
    return this.formula;
  }

  public List<MolecularVariant> variants() {
    return this.variants;
  }

  public MolecularVariant referenceVariant() {
    for (int i = 0; i < this.variants.size(); ++i) {
      MolecularVariant variant = this.variants.get(i);
      if (variant.kind() != MolecularVariantKind.REFERENCE) continue;
      return variant;
    }
    throw new IllegalStateException("Molecular variant set must contain reference variant.");
  }

  public List<MolecularVariantRelation> relations() {
    final ArrayList<MolecularVariantRelation> relations = new ArrayList<MolecularVariantRelation>();
    for (int i = 0; i < this.variants.size(); ++i) {
      for (int j = i + 1; j < this.variants.size(); ++j) {
        final MolecularVariant first = this.variants.get(i);
        final MolecularVariant second = this.variants.get(j);
        final MolecularComparisonResult comparison =
            MolecularIdentityComparator.compare(first.molecule(), second.molecule());
        relations.add(MolecularVariantRelation.of(first.label(), second.label(), comparison));
      }
    }
    return List.copyOf(relations);
  }

  private static List<MolecularVariant> requireVariants(final List<MolecularVariant> variants) {
    if (variants == null || variants.isEmpty()) {
      throw new IllegalArgumentException("Molecular variant set must not be empty.");
    }
    boolean hasReference = false;
    for (int i = 0; i < variants.size(); ++i) {
      MolecularVariant variant = variants.get(i);
      if (variant == null) {
        throw new IllegalArgumentException("Molecular variant must not be null.");
      }
      if (variant.kind() == MolecularVariantKind.REFERENCE) {
        if (hasReference) {
          throw new IllegalArgumentException(
              "Molecular variant set must contain exactly one reference variant.");
        }
        hasReference = true;
      }
      for (int j = i + 1; j < variants.size(); ++j) {
        MolecularVariant other = variants.get(j);
        if (other == null) {
          throw new IllegalArgumentException("Molecular variant must not be null.");
        }
        if (variant.label().equals(other.label())) {
          throw new IllegalArgumentException("Molecular variant labels must be unique.");
        }
        if (!variant.molecule().id().equals(other.molecule().id())) continue;
        throw new IllegalArgumentException("Molecular variant molecule ids must be unique.");
      }
    }
    if (!hasReference) {
      throw new IllegalArgumentException(
          "Molecular variant set must contain exactly one reference variant.");
    }
    return variants;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MolecularVariantSet)) {
      return false;
    }
    final MolecularVariantSet set = (MolecularVariantSet) other;
    return Objects.equals(this.id, set.id)
        && Objects.equals(this.formula, set.formula)
        && Objects.equals(this.variants, set.variants);
  }

  public int hashCode() {
    return Objects.hash(this.id, this.formula, this.variants);
  }
}
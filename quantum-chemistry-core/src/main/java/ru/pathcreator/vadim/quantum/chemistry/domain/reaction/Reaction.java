/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.reaction;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubject;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;

public final class Reaction implements ChemistrySubject {

  private final ReactionId id;
  private final String displayName;
  private final ReactionSide reactants;
  private final ReactionSide products;
  private final ReactionConditions conditions;
  private final ChemistryMetadata metadata;

  private Reaction(
      final ReactionId id,
      final String displayName,
      final ReactionSide reactants,
      final ReactionSide products,
      final ReactionConditions conditions,
      final ChemistryMetadata metadata) {
    this.id = id;
    this.displayName = displayName;
    this.reactants = reactants;
    this.products = products;
    this.conditions = conditions;
    this.metadata = metadata;
  }

  public static Reaction of(
      final ReactionId id, final String displayName, final ReactionSide reactants, final ReactionSide products) {
    return Reaction.of(
        id, displayName, reactants, products, ReactionConditions.EMPTY, ChemistryMetadata.EMPTY);
  }

  public static Reaction of(
      final ReactionId id,
      final String displayName,
      final ReactionSide reactants,
      final ReactionSide products,
      final ChemistryMetadata metadata) {
    return Reaction.of(id, displayName, reactants, products, ReactionConditions.EMPTY, metadata);
  }

  public static Reaction of(
      final ReactionId id,
      final String displayName,
      final ReactionSide reactants,
      final ReactionSide products,
      final ReactionConditions conditions,
      final ChemistryMetadata metadata) {
    if (id == null) {
      throw new IllegalArgumentException("Reaction id must not be null.");
    }
    if (reactants == null) {
      throw new IllegalArgumentException("Reaction reactants must not be null.");
    }
    if (products == null) {
      throw new IllegalArgumentException("Reaction products must not be null.");
    }
    final ReactionConditions checkedConditions =
        conditions == null ? ReactionConditions.EMPTY : conditions;
    final ChemistryMetadata checkedMetadata = metadata == null ? ChemistryMetadata.EMPTY : metadata;
    return new Reaction(
        id,
        TextValue.requireText(displayName, "Reaction display name"),
        reactants,
        products,
        checkedConditions,
        checkedMetadata);
  }

  public ReactionId id() {
    return this.id;
  }

  public String displayName() {
    return this.displayName;
  }

  public ReactionSide reactants() {
    return this.reactants;
  }

  public ReactionSide products() {
    return this.products;
  }

  public ReactionConditions conditions() {
    return this.conditions;
  }

  public ChemistryMetadata metadata() {
    return this.metadata;
  }

  public ReactionBalance balance() {
    return ReactionBalance.of(this);
  }

  public ReactionStoichiometry stoichiometry() {
    return ReactionStoichiometry.of(this);
  }

  @Override
  public ChemistrySubjectKind subjectKind() {
    return ChemistrySubjectKind.REACTION;
  }

  @Override
  public String stableId() {
    return this.id.value();
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof Reaction)) {
      return false;
    }
    final Reaction reaction = (Reaction) other;
    return Objects.equals(this.id, reaction.id)
        && Objects.equals(this.displayName, reaction.displayName)
        && Objects.equals(this.reactants, reaction.reactants)
        && Objects.equals(this.products, reaction.products)
        && Objects.equals(this.conditions, reaction.conditions)
        && Objects.equals(this.metadata, reaction.metadata);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.id);
    result = ChemistryHash.include(result, this.displayName);
    result = ChemistryHash.include(result, this.reactants);
    result = ChemistryHash.include(result, this.products);
    result = ChemistryHash.include(result, this.conditions);
    result = ChemistryHash.include(result, this.metadata);
    return result;
  }
}
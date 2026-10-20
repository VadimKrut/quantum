/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.notebook;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.*;

/** A draft stage may be incomplete. Conversion to the existing reaction core requires explicit sides and coefficients. */
public record LaboratoryReactionStage(
    String id,
    String title,
    Arrow arrow,
    ReactionConditions conditions,
    List<LaboratoryStageParticipant> participants,
    LaboratoryAtomMapping atomMapping,
    LaboratoryReactionClassification classification) {
  public LaboratoryReactionStage(
      final String id,
      final String title,
      final Arrow arrow,
      final ReactionConditions conditions,
      final List<LaboratoryStageParticipant> participants) {
    this(
        id,
        title,
        arrow,
        conditions,
        participants,
        LaboratoryAtomMapping.EMPTY,
        LaboratoryReactionClassification.EMPTY);
  }

  public LaboratoryReactionStage(
      final String id,
      final String title,
      final Arrow arrow,
      final ReactionConditions conditions,
      final List<LaboratoryStageParticipant> participants,
      final LaboratoryAtomMapping atomMapping) {
    this(
        id,
        title,
        arrow,
        conditions,
        participants,
        atomMapping,
        LaboratoryReactionClassification.EMPTY);
  }

  public enum Arrow {
    FORWARD,
    EQUILIBRIUM;

    public String toString() {
      return this == FORWARD ? "Прямая реакция" : "Равновесие";
    }
  }

  public LaboratoryReactionStage {
    id = IdentifierValue.requireIdentifier(id, "Reaction stage id");
    title = TextValue.requireText(title, "Reaction stage title");
    Objects.requireNonNull(arrow, "Reaction arrow");
    Objects.requireNonNull(conditions, "Stage conditions");
    participants = List.copyOf(participants);
    Objects.requireNonNull(atomMapping);
    Objects.requireNonNull(classification);
    final HashSet<String> included = new HashSet<>();
    for (final var participant : participants) {
      if (!included.add(participant.componentId())) {
        throw new IllegalArgumentException("Компонент уже включён в стадию.");
      }
    }
  }

  public Reaction toReaction(final LaboratoryExperiment experiment) {
    final ArrayList<ReactionParticipant> reactants = new ArrayList<>();
    final ArrayList<ReactionParticipant> products = new ArrayList<>();
    final ArrayList<ReactionConditionComponent> auxiliaries =
        new ArrayList<>(this.conditions.components());
    for (final var participant : this.participants) {
      final LaboratoryComponent component =
          experiment.components().stream()
              .filter(item -> item.id().equals(participant.componentId()))
              .findFirst()
              .orElseThrow(
                  () -> new IllegalArgumentException("Участник стадии отсутствует в опыте."));
      if (participant.role() == ReactionComponentRole.REACTANT
          || participant.role() == ReactionComponentRole.PRODUCT) {
        if (participant.coefficient() == null) {
          throw new IllegalArgumentException(
              "Укажите коэффициент для «" + component.molecule().displayName() + "».");
        }
        (participant.role() == ReactionComponentRole.REACTANT ? reactants : products)
            .add(ReactionParticipant.of(component.molecule(), participant.coefficient()));
      } else {
        auxiliaries.add(
            ReactionConditionComponent.named(
                participant.role(), component.molecule().displayName()));
      }
    }
    if (reactants.isEmpty() || products.isEmpty()) {
      throw new IllegalArgumentException(
          "Для проверки реакции нужны исходники и продукты. Черновик можно сохранить без них.");
    }
    return Reaction.of(
        ReactionId.of(this.id),
        this.title,
        reactionSide(reactants),
        reactionSide(products),
        ReactionConditions.of(
            this.conditions.temperature(),
            this.conditions.pressure(),
            this.conditions.duration(),
            auxiliaries,
            this.conditions.features(),
            this.conditions.note()),
        ChemistryMetadata.EMPTY);
  }

  /** Separate notebook occurrences stay independent; the reaction core receives one term per molecule. */
  public static ReactionSide reactionSide(final List<ReactionParticipant> occurrences) {
    final var terms = new java.util.LinkedHashMap<String, ReactionParticipant>();
    for (final var occurrence : occurrences) {
      final String moleculeId = occurrence.molecule().id().value();
      final var previous = terms.get(moleculeId);
      if (previous == null) {
        terms.put(moleculeId, occurrence);
        continue;
      }
      if (!previous.molecule().equals(occurrence.molecule())) {
        throw new IllegalArgumentException(
            "Один идентификатор молекулы относится к разным структурам. Проверьте участников стадии.");
      }
      final int coefficient =
          Math.addExact(previous.coefficient().value(), occurrence.coefficient().value());
      terms.put(
          moleculeId,
          ReactionParticipant.of(previous.molecule(), StoichiometricCoefficient.of(coefficient)));
    }
    return ReactionSide.of(List.copyOf(terms.values()));
  }
}

/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.prediction;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;

/** Запрос прогноза продуктов из реагентов, условий и набора допустимых химических доменов. */
public final class ReactionPredictionRequest {

  private final String id;
  private final ReactionSide reactants;
  private final ReactionConditions conditions;
  private final List<OrganicReactionDomain> domains;
  private final List<ReactionProductCandidate> seedCandidates;
  private final String note;

  private ReactionPredictionRequest(
      final String id,
      final ReactionSide reactants,
      final ReactionConditions conditions,
      final List<OrganicReactionDomain> domains,
      final List<ReactionProductCandidate> seedCandidates,
      final String note) {
    this.id = id;
    this.reactants = reactants;
    this.conditions = conditions;
    this.domains = domains;
    this.seedCandidates = seedCandidates;
    this.note = note;
  }

  public static ReactionPredictionRequest of(
      final String id,
      final ReactionSide reactants,
      final ReactionConditions conditions,
      final List<OrganicReactionDomain> domains,
      final List<ReactionProductCandidate> seedCandidates,
      final String note) {
    if (reactants == null) {
      throw new IllegalArgumentException("Reaction prediction reactants must not be null.");
    }
    return new ReactionPredictionRequest(
        IdentifierValue.requireIdentifier(id, "Reaction prediction request id"),
        reactants,
        conditions == null ? ReactionConditions.EMPTY : conditions,
        ReactionPredictionRequest.checkedDomains(domains),
        ReactionPredictionRequest.checkedCandidates(seedCandidates),
        TextValue.optionalText(note));
  }

  public String id() {
    return this.id;
  }

  public ReactionSide reactants() {
    return this.reactants;
  }

  public ReactionConditions conditions() {
    return this.conditions;
  }

  public List<OrganicReactionDomain> domains() {
    return this.domains;
  }

  public boolean hasDomain(final OrganicReactionDomain domain) {
    return this.domains.contains(domain);
  }

  public List<ReactionProductCandidate> seedCandidates() {
    return this.seedCandidates;
  }

  public boolean hasSeedCandidates() {
    return !this.seedCandidates.isEmpty();
  }

  public String note() {
    return this.note;
  }

  public boolean hasNote() {
    return this.note != null;
  }

  private static List<OrganicReactionDomain> checkedDomains(
      final List<OrganicReactionDomain> domains) {
    if (domains == null || domains.isEmpty()) {
      return List.of(OrganicReactionDomain.UNKNOWN);
    }
    for (int i = 0; i < domains.size(); ++i) {
      if (domains.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException("Organic reaction domain must not be null.");
    }
    return List.copyOf(domains);
  }

  private static List<ReactionProductCandidate> checkedCandidates(
      final List<ReactionProductCandidate> candidates) {
    if (candidates == null || candidates.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < candidates.size(); ++i) {
      if (candidates.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException("Reaction prediction seed candidate must not be null.");
    }
    return List.copyOf(candidates);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionPredictionRequest)) {
      return false;
    }
    final ReactionPredictionRequest request = (ReactionPredictionRequest) other;
    return Objects.equals(this.id, request.id)
        && Objects.equals(this.reactants, request.reactants)
        && Objects.equals(this.conditions, request.conditions)
        && Objects.equals(this.domains, request.domains)
        && Objects.equals(this.seedCandidates, request.seedCandidates)
        && Objects.equals(this.note, request.note);
  }

  public int hashCode() {
    return Objects.hash(
        this.id, this.reactants, this.conditions, this.domains, this.seedCandidates, this.note);
  }
}

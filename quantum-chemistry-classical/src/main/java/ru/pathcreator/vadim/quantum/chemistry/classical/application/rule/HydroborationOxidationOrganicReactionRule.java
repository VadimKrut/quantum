/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.rule;

import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.transform.HydroborationOxidationTransformer;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.*;

/** Explicit graph hypothesis for a BH3/peroxide sequence; water alone cannot trigger an alcohol surrogate. */
public final class HydroborationOxidationOrganicReactionRule implements OrganicReactionRule {
  private final HydroborationOxidationTransformer transformer =
      new HydroborationOxidationTransformer();

  public OrganicReactionRuleId id() {
    return OrganicReactionRuleId.of("bond_addition_alkene_hydroboration_oxidation");
  }

  public String displayName() {
    return "Hydroboration–oxidation with explicit BH3 and peroxide";
  }

  public OrganicReactionRuleCategory category() {
    return OrganicReactionRuleCategory.ADDITION;
  }

  public OrganicReactionRuleMatch match(final ReactionPredictionRequest request) {
    Objects.requireNonNull(request);
    if (!request.domains().isEmpty()
        && request.domains().stream()
            .noneMatch(
                d ->
                    d == OrganicReactionDomain.ADDITION
                        || d == OrganicReactionDomain.OXIDATION_REDUCTION)) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    if (request.reactants().participants().stream()
        .noneMatch(p -> this.transformer.acceptsSubstrate(p.molecule()))) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    final boolean borane =
        request.reactants().participants().stream()
            .anyMatch(p -> this.transformer.acceptsBorane(p.molecule()));
    final boolean peroxide =
        request.reactants().participants().stream()
            .anyMatch(
                p ->
                    p.coefficient().value() >= 3 && this.transformer.acceptsPeroxide(p.molecule()));
    return OrganicReactionRuleMatch.matched(
        0.39,
        "Explicit net graph model: alkene + BH3 + 3 H2O2 -> alcohol + B(OH)3 + 2 H2O. "
            + (borane ? "BH3 graph present. " : "Missing a separate neutral BH3 graph. ")
            + (peroxide
                ? "Peroxide stoichiometry present. "
                : "Missing a separate H-O-O-H graph with coefficient at least 3. ")
            + "Condition labels, water, borates and an unrelated oxygen inventory cannot supply these reagents. "
            + "The model represents staged hydroboration, oxidative treatment and neutral workup, not simultaneous mixing. "
            + "Activation, solution speciation, competing sites, syn stereochemistry and experimental yield are not calculated.");
  }

  public List<ReactionProductCandidate> generate(
      final ReactionPredictionRequest request, final OrganicReactionRuleMatch match) {
    Objects.requireNonNull(request);
    if (match == null || !match.matched()) {
      return List.of();
    }
    final var result = new ArrayList<ReactionProductCandidate>();
    final var participants = request.reactants().participants();
    for (final var source : participants) {
      if (!this.transformer.acceptsSubstrate(source.molecule())) {
        continue;
      }
      for (final var borane : participants) {
        if (!this.transformer.acceptsBorane(borane.molecule())) {
          continue;
        }
        for (final var peroxide : participants) {
          if (peroxide.coefficient().value() < 3
              || !this.transformer.acceptsPeroxide(peroxide.molecule())) {
            continue;
          }
          for (final var product :
              this.transformer.transformAll(
                  source.molecule(), borane.molecule(), peroxide.molecule())) {
            result.add(
                ReactionProductCandidate.of(
                    product.products(),
                    match.confidence(),
                    58.0,
                    "Mapped net hypothesis. Both alkene orientations are retained; no experimentally calibrated selectivity or yield.",
                    List.of(
                        ReactionPredictionEvidence.of(
                            ReactionPredictionEvidenceKind.USER_RULE,
                            this.id().value() + ": " + product.mapping(),
                            match.confidence()),
                        ReactionPredictionEvidence.of(
                            ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
                            "Explicit reagents: "
                                + source.molecule().id().value()
                                + ", "
                                + borane.molecule().id().value()
                                + ", 3 × "
                                + peroxide.molecule().id().value(),
                            0.1),
                        ReactionPredictionEvidence.of(
                            ReactionPredictionEvidenceKind.WARNING,
                            match.explanation()
                                + " All reagent atoms and isotopes are retained across the alcohol, neutral boric acid and two water molecules; additional supplied equivalents are not consumed by this unit event.",
                            -0.02))));
          }
        }
      }
    }
    return List.copyOf(result);
  }
}

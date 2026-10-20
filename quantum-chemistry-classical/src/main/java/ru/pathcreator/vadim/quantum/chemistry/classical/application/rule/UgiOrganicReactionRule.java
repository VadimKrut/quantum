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
import ru.pathcreator.vadim.quantum.chemistry.classical.application.transform.UgiTransformer;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;

/** Four distinct explicit reactants; bounded connectivity construction, not a yield model. */
public final class UgiOrganicReactionRule implements OrganicReactionRule {
  private final UgiTransformer transformer = new UgiTransformer();

  public OrganicReactionRuleId id() {
    return OrganicReactionRuleId.of("transform_ugi_multicomponent_amide");
  }

  public String displayName() {
    return "Explicit four-component Ugi graph";
  }

  public OrganicReactionRuleCategory category() {
    return OrganicReactionRuleCategory.ADDITION;
  }

  public OrganicReactionRuleMatch match(final ReactionPredictionRequest request) {
    Objects.requireNonNull(request);
    if (!request.domains().isEmpty()
        && request.domains().stream()
            .noneMatch(
                domain ->
                    domain == OrganicReactionDomain.ADDITION
                        || domain == OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION)) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    if (request.reactants().participants().stream()
        .noneMatch(p -> this.transformer.acceptsCarbonyl(p.molecule()))) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    final var conditions =
        new OrganicReactionConditionMatcher()
            .assess(
                request.conditions(),
                List.of(
                    ReactionConditionFeatureKind.ADDITION_RATE,
                    ReactionConditionFeatureKind.ANHYDROUS,
                    ReactionConditionFeatureKind.WORKUP));
    if (conditions.stronglyIncompatible()) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    return OrganicReactionRuleMatch.matched(
        Math.max(0.01, Math.min(1, 0.28 + conditions.scoreAdjustment())),
        "Requires separate explicit aldehyde/ketone, carboxylic acid with O-H, R-N(+)-C(-) triple-bond isocyanide, and primary organic amine R-NH2 graphs. Produces an alpha-acylaminoamide plus water. Intramolecular and secondary-amine variants are outside this model; feasibility is not established.");
  }

  public List<ReactionProductCandidate> generate(
      final ReactionPredictionRequest request, final OrganicReactionRuleMatch match) {
    Objects.requireNonNull(request);
    if (match == null || !match.matched()) {
      return List.of();
    }
    final var inputs = request.reactants().participants();
    final var result = new ArrayList<ReactionProductCandidate>();
    for (int c = 0; c < inputs.size(); c++) {
      if (!this.transformer.acceptsCarbonyl(inputs.get(c).molecule())) {
        continue;
      }
      for (int a = 0; a < inputs.size(); a++) {
        if (a == c) {
          continue;
        }
        for (int i = 0; i < inputs.size(); i++) {
          if (i == c || i == a) {
            continue;
          }
          for (int n = 0; n < inputs.size(); n++) {
            if (n == c || n == a || n == i) {
              continue;
            }
            for (final var product :
                this.transformer.transformAll(
                    inputs.get(c).molecule(),
                    inputs.get(a).molecule(),
                    inputs.get(i).molecule(),
                    inputs.get(n).molecule())) {
              result.add(
                  ReactionProductCandidate.of(
                      ReactionProductSideFactory.fromTransformProduct(product),
                      match.confidence(),
                      42.0,
                      "Formal Ugi connectivity only; isotope/proton exchange, selectivity and experimental yield are not predicted.",
                      List.of(
                          ReactionPredictionEvidence.of(
                              ReactionPredictionEvidenceKind.USER_RULE,
                              "Explicit carbonyl/acid/isocyanide/primary amine combination: "
                                  + this.id().value(),
                              match.confidence()),
                          ReactionPredictionEvidence.of(
                              ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
                              "All atoms retained in alpha-acylaminoamide and water: "
                                  + inputs.get(c).molecule().id().value()
                                  + ", "
                                  + inputs.get(a).molecule().id().value()
                                  + ", "
                                  + inputs.get(i).molecule().id().value()
                                  + ", "
                                  + inputs.get(n).molecule().id().value(),
                              0.1),
                          ReactionPredictionEvidence.of(
                              ReactionPredictionEvidenceKind.WARNING,
                              "Numerical weights are heuristic. Neutral closed-shell intermolecular model; no new stereochemistry or geometry assigned. Formal atom mapping does not model oxygen/proton exchange; conditions and competing pathways need independent assessment.",
                              -0.02))));
            }
          }
        }
      }
    }
    return List.copyOf(result);
  }
}

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
import ru.pathcreator.vadim.quantum.chemistry.classical.application.transform.PasseriniTransformer;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;

/** Bounded graph construction, not a kinetic or experimental-yield model. */
public final class PasseriniOrganicReactionRule implements OrganicReactionRule {
  private final PasseriniTransformer transformer = new PasseriniTransformer();

  public OrganicReactionRuleId id() {
    return OrganicReactionRuleId.of("transform_passerini_multicomponent_ester");
  }

  public String displayName() {
    return "Explicit three-component Passerini graph";
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
        Math.max(0.01, Math.min(1, 0.27 + conditions.scoreAdjustment())),
        "Requires separate explicit aldehyde/ketone, carboxylic acid with O-H, and R-N(+)-C(-) triple-bond isocyanide graphs. A peracid, nitrile or reagent name is not a substitute. This formal product graph does not establish reaction feasibility.");
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
          for (final var product :
              this.transformer.transformAll(
                  inputs.get(c).molecule(), inputs.get(a).molecule(), inputs.get(i).molecule())) {
            result.add(
                ReactionProductCandidate.of(
                    ReactionProductSideFactory.fromTransformProduct(product),
                    match.confidence(),
                    40.0,
                    "Formal Passerini connectivity only; selectivity, isotope/proton exchange and experimental yield are not predicted.",
                    List.of(
                        ReactionPredictionEvidence.of(
                            ReactionPredictionEvidenceKind.USER_RULE,
                            "Explicit carbonyl/acid/isocyanide combination: " + this.id().value(),
                            match.confidence()),
                        ReactionPredictionEvidence.of(
                            ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
                            "All three reactant graphs are incorporated: "
                                + inputs.get(c).molecule().id().value()
                                + ", "
                                + inputs.get(a).molecule().id().value()
                                + ", "
                                + inputs.get(i).molecule().id().value(),
                            0.1),
                        ReactionPredictionEvidence.of(
                            ReactionPredictionEvidenceKind.WARNING,
                            "Numerical weights are heuristic. Neutral closed-shell intermolecular model; new stereochemistry and geometry are not assigned. Catalytic acid participation and competing pathways require independent assessment.",
                            -0.02))));
          }
        }
      }
    }
    return List.copyOf(result);
  }
}

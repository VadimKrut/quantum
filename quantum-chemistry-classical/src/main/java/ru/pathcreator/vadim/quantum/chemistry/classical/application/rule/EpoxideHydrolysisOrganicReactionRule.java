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
import ru.pathcreator.vadim.quantum.chemistry.classical.application.transform.EpoxideHydrolysisTransformer;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.*;

/** A ring-opening graph hypothesis, never inferred from an alkene plus unrelated oxygen. */
public final class EpoxideHydrolysisOrganicReactionRule implements OrganicReactionRule {
  private final EpoxideHydrolysisTransformer transformer = new EpoxideHydrolysisTransformer();

  public OrganicReactionRuleId id() {
    return OrganicReactionRuleId.of("transform_epoxide_opening");
  }

  public String displayName() {
    return "Epoxide hydrolysis with explicit water";
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
                        || d == OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION)) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    if (request.reactants().participants().stream()
        .noneMatch(p -> this.transformer.acceptsSubstrate(p.molecule()))) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    return OrganicReactionRuleMatch.matched(
        0.37,
        "Requires an explicit neutral C-O-C epoxide ring and a separate connected H2O reactant. "
            + "One C-O bond and one water O-H bond are exchanged; all atoms and isotope labels are retained. "
            + "Acid/base activation, regioselectivity, kinetics and experimental yield are not established by this structural match.");
  }

  public List<ReactionProductCandidate> generate(
      final ReactionPredictionRequest request, final OrganicReactionRuleMatch match) {
    Objects.requireNonNull(request);
    if (match == null || !match.matched()) {
      return List.of();
    }
    final var result = new ArrayList<ReactionProductCandidate>();
    final var participants = request.reactants().participants();
    for (int i = 0; i < participants.size(); i++) {
      final var source = participants.get(i).molecule();
      if (!this.transformer.acceptsSubstrate(source)) {
        continue;
      }
      for (int j = 0; j < participants.size(); j++) {
        if (i == j) {
          continue;
        }
        final var donor = participants.get(j).molecule();
        for (final var product : this.transformer.transformAll(source, donor)) {
          result.add(
              ReactionProductCandidate.of(
                  ReactionProductSideFactory.fromTransformProduct(product),
                  match.confidence(),
                  55.0,
                  "Mapped hydrolysis hypothesis; weights are heuristic and do not establish yield or stereochemistry.",
                  List.of(
                      ReactionPredictionEvidence.of(
                          ReactionPredictionEvidenceKind.USER_RULE,
                          "Explicit epoxide hydrolysis: " + this.id().value(),
                          match.confidence()),
                      ReactionPredictionEvidence.of(
                          ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
                          "Epoxide: "
                              + source.id().value()
                              + "; water: "
                              + donor.id().value()
                              + "; "
                              + product.mappingDescription(),
                          0.1),
                      ReactionPredictionEvidence.of(
                          ReactionPredictionEvidenceKind.WARNING,
                          "Neutral closed-shell explicit-H model. Both ring carbon sites are considered; new stereochemistry, geometry and experimental selectivity are not assigned.",
                          -0.02))));
        }
      }
    }
    return List.copyOf(result);
  }
}

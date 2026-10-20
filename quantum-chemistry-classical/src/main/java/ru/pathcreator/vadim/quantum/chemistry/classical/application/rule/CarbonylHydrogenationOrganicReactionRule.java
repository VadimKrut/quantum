/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.rule;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.transform.CarbonylHydrogenationTransformer;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.*;

/** A balanced structural hypothesis, not a catalyst-specific kinetic or yield model. */
public final class CarbonylHydrogenationOrganicReactionRule implements OrganicReactionRule {
  private final CarbonylHydrogenationTransformer transformer =
      new CarbonylHydrogenationTransformer();

  public OrganicReactionRuleId id() {
    return OrganicReactionRuleId.of("bond_addition_carbonyl_hydrogenation");
  }

  public String displayName() {
    return "Carbonyl hydrogenation with explicit H2";
  }

  public OrganicReactionRuleCategory category() {
    return OrganicReactionRuleCategory.REDUCTION;
  }

  public OrganicReactionRuleMatch match(final ReactionPredictionRequest request) {
    java.util.Objects.requireNonNull(request);
    if (!request.hasDomain(OrganicReactionDomain.UNKNOWN)
        && !request.hasDomain(OrganicReactionDomain.OXIDATION_REDUCTION)
        && !request.hasDomain(OrganicReactionDomain.ADDITION)) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    if (request.reactants().participants().stream()
        .noneMatch(p -> this.transformer.acceptsSubstrate(p.molecule()))) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    return OrganicReactionRuleMatch.matched(
        0.5,
        "Aldehyde/ketone hydrogenation requires a separate connected H-H reactant and catalyst context. "
            + "Named hydride reagents, water and unrelated hydrogen atoms cannot supply H2. NaBH4/LAH and transfer hydrogenation are not represented by this channel.");
  }

  public List<ReactionProductCandidate> generate(
      final ReactionPredictionRequest request, final OrganicReactionRuleMatch match) {
    java.util.Objects.requireNonNull(request);
    if (match == null || !match.matched() || !this.match(request).matched()) {
      return List.of();
    }
    if (!request.conditions().hasFeature(ReactionConditionFeatureKind.CATALYST_PRESENT)
        && request.conditions().componentsByRole(ReactionComponentRole.CATALYST).isEmpty()) {
      return List.of();
    }
    if (request.conditions().hasFeature(ReactionConditionFeatureKind.OXIDIZING_MEDIUM)) {
      return List.of();
    }
    final var participants = request.reactants().participants();
    final var result = new ArrayList<ReactionProductCandidate>();
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
                  72.0,
                  "Structural hypothesis. Catalyst compatibility, chemoselectivity and stereochemistry require verification; numerical ranking priors are not experimental yields.",
                  List.of(
                      ReactionPredictionEvidence.of(
                          ReactionPredictionEvidenceKind.USER_RULE,
                          "Explicit H2 carbonyl addition: " + this.id().value(),
                          match.confidence()),
                      ReactionPredictionEvidence.of(
                          ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
                          "Substrate: "
                              + source.id().value()
                              + "; H2 donor: "
                              + donor.id().value()
                              + "; "
                              + product.mappingDescription()
                              + ". Both donor atoms and their isotopes are retained.",
                          0.1),
                      ReactionPredictionEvidence.of(
                          ReactionPredictionEvidenceKind.WARNING,
                          "Neutral closed-shell aldehyde/ketone model; not an acyl-derivative or hydride-workup model. Geometry and new stereochemistry are unassigned. Yield is not calibrated. Literature example: doi:10.1186/1860-5397-2-15.",
                          -0.02))));
        }
      }
    }
    return List.copyOf(result);
  }
}

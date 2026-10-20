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
import ru.pathcreator.vadim.quantum.chemistry.classical.application.transform.PeracidOxygenTransferTransformer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.transform.PeracidOxygenTransferTransformer.Kind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;

/** Graph-backed peracid channels; an oxygen atom elsewhere in the request is not a peracid. */
public final class PeracidOxygenTransferOrganicReactionRule implements OrganicReactionRule {
  private final Kind kind;
  private final PeracidOxygenTransferTransformer transformer =
      new PeracidOxygenTransferTransformer();

  public PeracidOxygenTransferOrganicReactionRule(final Kind kind) {
    this.kind = java.util.Objects.requireNonNull(kind);
  }

  public OrganicReactionRuleId id() {
    return OrganicReactionRuleId.of(
        this.kind == Kind.EPOXIDATION
            ? "bridge_addition_alkene_epoxidation"
            : "bond_insertion_baeyer_villiger_oxygen");
  }

  public String displayName() {
    return this.kind == Kind.EPOXIDATION
        ? "Peracid alkene epoxidation"
        : "Peracid ketone Baeyer-Villiger oxygen transfer";
  }

  public OrganicReactionRuleCategory category() {
    return OrganicReactionRuleCategory.OXIDATION;
  }

  public OrganicReactionRuleMatch match(final ReactionPredictionRequest request) {
    java.util.Objects.requireNonNull(request);
    final var domains =
        this.kind == Kind.EPOXIDATION
            ? List.of(OrganicReactionDomain.ADDITION, OrganicReactionDomain.OXIDATION_REDUCTION)
            : List.of(
                OrganicReactionDomain.OXIDATION_REDUCTION, OrganicReactionDomain.REARRANGEMENT);
    if (!request.domains().isEmpty() && request.domains().stream().noneMatch(domains::contains)) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    if (request.reactants().participants().stream()
        .noneMatch(p -> this.transformer.acceptsSubstrate(p.molecule(), this.kind))) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    final var conditions =
        new OrganicReactionConditionMatcher()
            .assess(
                request.conditions(),
                List.of(
                    ReactionConditionFeatureKind.COOLING,
                    ReactionConditionFeatureKind.ADDITION_RATE,
                    ReactionConditionFeatureKind.ANHYDROUS));
    if (conditions.stronglyIncompatible()) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    return OrganicReactionRuleMatch.matched(
        Math.max(0.01, Math.min(1, 0.45 + conditions.scoreAdjustment())),
        "Structural channel requires a separate connected neutral R-C(=O)-O-O-H reactant with explicit H. "
            + "The terminal peroxide oxygen is transferred and the carboxylic acid coproduct is retained. Conditions alone do not supply this reagent.");
  }

  public List<ReactionProductCandidate> generate(
      final ReactionPredictionRequest request, final OrganicReactionRuleMatch match) {
    java.util.Objects.requireNonNull(request);
    if (match == null || !match.matched()) {
      return List.of();
    }
    final var participants = request.reactants().participants();
    final var result = new ArrayList<ReactionProductCandidate>();
    for (int i = 0; i < participants.size(); i++) {
      final var source = participants.get(i).molecule();
      if (!this.transformer.acceptsSubstrate(source, this.kind)) {
        continue;
      }
      for (int j = 0; j < participants.size(); j++) {
        if (i == j) {
          continue;
        }
        final var donor = participants.get(j).molecule();
        for (final var product : this.transformer.transformAll(source, donor, this.kind)) {
          result.add(
              ReactionProductCandidate.of(
                  ReactionProductSideFactory.fromTransformProduct(product),
                  match.confidence(),
                  this.kind == Kind.EPOXIDATION ? 61.0 : 45.0,
                  "Graph hypothesis; regioselectivity, stereochemistry and experimental yield require verification.",
                  List.of(
                      ReactionPredictionEvidence.of(
                          ReactionPredictionEvidenceKind.USER_RULE,
                          "Explicit peracid transfer: " + this.id().value(),
                          match.confidence()),
                      ReactionPredictionEvidence.of(
                          ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
                          "O source: "
                              + donor.id().value()
                              + "; substrate: "
                              + source.id().value()
                              + "; "
                              + product.mappingDescription()
                              + ". Acid coproduct retains the other reagent atoms, including the transferred proton.",
                          0.1),
                      ReactionPredictionEvidence.of(
                          ReactionPredictionEvidenceKind.WARNING,
                          "Neutral closed-shell graph model. New stereochemistry and geometry are not assigned; numerical weights are heuristic.",
                          -0.02))));
        }
      }
    }
    return List.copyOf(result);
  }
}

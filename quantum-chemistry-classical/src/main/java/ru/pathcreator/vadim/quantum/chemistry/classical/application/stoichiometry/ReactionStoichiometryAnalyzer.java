/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.stoichiometry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** Сопоставляет количественные компоненты рецепта с молекулами и считает mol/equiv без lookup по исходам. */
public final class ReactionStoichiometryAnalyzer {

  public ReactionStoichiometryProfile analyze(final ReactionPredictionRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction stoichiometry request must not be null.");
    }
    final double totalGramScale =
        ReactionStoichiometryAnalyzer.totalGramScale(request.conditions().components());
    final List<ReactionConditionComponent> recipeReactants =
        request.conditions().componentsByRole(ReactionComponentRole.REACTANT);
    if (recipeReactants.isEmpty()) {
      return ReactionStoichiometryProfile.of(List.of(), 0, 0, 0.0, 0.0, totalGramScale);
    }
    final List<ReactionParticipant> participants = request.reactants().participants();
    final ArrayList<ReactionStoichiometryComponent> matched =
        new ArrayList<ReactionStoichiometryComponent>(recipeReactants.size());
    int quantified = 0;
    int unmatched = 0;
    double minimumEquivalentRatio = Double.POSITIVE_INFINITY;
    double maximumEquivalentRatio = 0.0;
    for (int i = 0; i < recipeReactants.size(); ++i) {
      final ReactionConditionComponent component = recipeReactants.get(i);
      if (!component.hasAmount()) {
        continue;
      }
      ++quantified;
      final ReactionParticipant participant =
          ReactionStoichiometryAnalyzer.matchParticipant(component.name(), participants);
      if (participant == null) {
        ++unmatched;
        continue;
      }
      final double moleAmount =
          ReactionStoichiometryAnalyzer.moleAmount(component, participant.molecule());
      if (moleAmount <= 0.0 || !Double.isFinite(moleAmount)) {
        ++unmatched;
        continue;
      }
      final double equivalentRatio = moleAmount / participant.coefficient().value();
      minimumEquivalentRatio = Math.min(minimumEquivalentRatio, equivalentRatio);
      maximumEquivalentRatio = Math.max(maximumEquivalentRatio, equivalentRatio);
      matched.add(
          ReactionStoichiometryComponent.of(component, participant, moleAmount, equivalentRatio));
    }
    return ReactionStoichiometryProfile.of(
        matched,
        quantified,
        unmatched,
        Double.isFinite(minimumEquivalentRatio) ? minimumEquivalentRatio : 0.0,
        maximumEquivalentRatio,
        totalGramScale);
  }

  private static double totalGramScale(final List<ReactionConditionComponent> components) {
    double totalGramScale = 0.0;
    for (int i = 0; i < components.size(); ++i) {
      final ReactionConditionComponent component = components.get(i);
      if (component.hasAmount() && component.amount().massQuantity()) {
        totalGramScale += component.amount().gramValue();
      }
    }
    return totalGramScale;
  }

  private static double moleAmount(
      final ReactionConditionComponent component, final Molecule molecule) {
    if (component.amount().molarQuantity()) {
      return component.amount().moleValue();
    }
    if (!component.amount().massQuantity()) {
      return 0.0;
    }
    final double molarMass = molecule.composition().mass().averageAtomicMass();
    if (molarMass <= 0.0 || !Double.isFinite(molarMass)) {
      return 0.0;
    }
    return component.amount().gramValue() / molarMass;
  }

  private static ReactionParticipant matchParticipant(
      final String componentName, final List<ReactionParticipant> participants) {
    final String normalizedComponent = ReactionStoichiometryAnalyzer.normalizedName(componentName);
    for (int i = 0; i < participants.size(); ++i) {
      final ReactionParticipant participant = participants.get(i);
      if (ReactionStoichiometryAnalyzer.participantMatches(normalizedComponent, participant)) {
        return participant;
      }
    }
    return null;
  }

  private static boolean participantMatches(
      final String normalizedComponent, final ReactionParticipant participant) {
    if (ReactionStoichiometryAnalyzer.nameMatches(
        normalizedComponent, participant.molecule().displayName())) {
      return true;
    }
    if (ReactionStoichiometryAnalyzer.nameMatches(
        normalizedComponent, participant.molecule().id().value())) {
      return true;
    }
    if (ReactionStoichiometryAnalyzer.nameMatches(
        normalizedComponent, participant.molecule().formula().hillNotation())) {
      return true;
    }
    final Map<String, String> attributes = participant.molecule().metadata().attributes();
    for (final Map.Entry<String, String> entry : attributes.entrySet()) {
      final String key = entry.getKey().toLowerCase(Locale.ROOT);
      if ((key.contains("name")
              || key.contains("synonym")
              || key.contains("cas")
              || key.contains("pubchem")
              || key.contains("cid"))
          && ReactionStoichiometryAnalyzer.nameMatches(normalizedComponent, entry.getValue())) {
        return true;
      }
    }
    return false;
  }

  private static boolean nameMatches(final String normalizedComponent, final String value) {
    final String normalizedValue = ReactionStoichiometryAnalyzer.normalizedName(value);
    return !normalizedComponent.isEmpty()
        && !normalizedValue.isEmpty()
        && (normalizedValue.equals(normalizedComponent)
            || normalizedValue.contains(normalizedComponent)
            || normalizedComponent.contains(normalizedValue));
  }

  private static String normalizedName(final String value) {
    if (value == null) {
      return "";
    }
    final String lower = value.toLowerCase(Locale.ROOT);
    final StringBuilder normalized = new StringBuilder(lower.length());
    for (int i = 0; i < lower.length(); ++i) {
      final char current = lower.charAt(i);
      if (Character.isLetterOrDigit(current)) {
        normalized.append(current);
      }
    }
    return normalized.toString();
  }
}

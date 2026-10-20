/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionMechanismChannelCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;

/** Результат применения rule library к одному reaction prediction request. */
public final class GeneratedReactionCandidateSet {

  public static final GeneratedReactionCandidateSet EMPTY =
      new GeneratedReactionCandidateSet(List.of(), List.of(), List.of());

  private final List<ReactionProductCandidate> candidates;
  private final List<ReactionMechanismChannelCandidate> mechanismChannels;
  private final List<ReactionPredictionEvidence> diagnostics;

  private GeneratedReactionCandidateSet(
      final List<ReactionProductCandidate> candidates,
      final List<ReactionMechanismChannelCandidate> mechanismChannels,
      final List<ReactionPredictionEvidence> diagnostics) {
    this.candidates = candidates;
    this.mechanismChannels = mechanismChannels;
    this.diagnostics = diagnostics;
  }

  public static GeneratedReactionCandidateSet of(
      final List<ReactionProductCandidate> candidates,
      final List<ReactionPredictionEvidence> diagnostics) {
    return GeneratedReactionCandidateSet.of(candidates, List.of(), diagnostics);
  }

  public static GeneratedReactionCandidateSet of(
      final List<ReactionProductCandidate> candidates,
      final List<ReactionMechanismChannelCandidate> mechanismChannels,
      final List<ReactionPredictionEvidence> diagnostics) {
    return new GeneratedReactionCandidateSet(
        GeneratedReactionCandidateSet.checkedCandidates(candidates),
        GeneratedReactionCandidateSet.checkedMechanismChannels(mechanismChannels),
        GeneratedReactionCandidateSet.checkedDiagnostics(diagnostics));
  }

  public List<ReactionProductCandidate> candidates() {
    return this.candidates;
  }

  public List<ReactionMechanismChannelCandidate> mechanismChannels() {
    return this.mechanismChannels;
  }

  public List<ReactionPredictionEvidence> diagnostics() {
    return this.diagnostics;
  }

  public boolean hasCandidates() {
    return !this.candidates.isEmpty();
  }

  public boolean hasMechanismChannels() {
    return !this.mechanismChannels.isEmpty();
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
      throw new IllegalArgumentException("Generated reaction candidate must not be null.");
    }
    return List.copyOf(candidates);
  }

  private static List<ReactionMechanismChannelCandidate> checkedMechanismChannels(
      final List<ReactionMechanismChannelCandidate> mechanismChannels) {
    if (mechanismChannels == null || mechanismChannels.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < mechanismChannels.size(); ++i) {
      if (mechanismChannels.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException("Generated reaction mechanism channel must not be null.");
    }
    return List.copyOf(mechanismChannels);
  }

  private static List<ReactionPredictionEvidence> checkedDiagnostics(
      final List<ReactionPredictionEvidence> diagnostics) {
    if (diagnostics == null || diagnostics.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < diagnostics.size(); ++i) {
      if (diagnostics.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException("Generated reaction diagnostic must not be null.");
    }
    return List.copyOf(diagnostics);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof GeneratedReactionCandidateSet)) {
      return false;
    }
    final GeneratedReactionCandidateSet set = (GeneratedReactionCandidateSet) other;
    return Objects.equals(this.candidates, set.candidates)
        && Objects.equals(this.mechanismChannels, set.mechanismChannels)
        && Objects.equals(this.diagnostics, set.diagnostics);
  }

  public int hashCode() {
    return Objects.hash(this.candidates, this.mechanismChannels, this.diagnostics);
  }
}

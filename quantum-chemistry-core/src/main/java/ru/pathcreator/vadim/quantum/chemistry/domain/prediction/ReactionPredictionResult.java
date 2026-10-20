/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.prediction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Итог прогноза продуктов: метод, ранжированные кандидаты и диагностические предупреждения.
 */
public final class ReactionPredictionResult {

  private final ReactionPredictionRequest request;
  private final ReactionPredictionMethodType methodType;
  private final List<ReactionProductCandidate> candidates;
  private final List<ReactionMechanismChannelCandidate> mechanismChannels;
  private final List<ReactionPredictionEvidence> diagnostics;

  private ReactionPredictionResult(
      final ReactionPredictionRequest request,
      final ReactionPredictionMethodType methodType,
      final List<ReactionProductCandidate> candidates,
      final List<ReactionMechanismChannelCandidate> mechanismChannels,
      final List<ReactionPredictionEvidence> diagnostics) {
    this.request = request;
    this.methodType = methodType;
    this.candidates = candidates;
    this.mechanismChannels = mechanismChannels;
    this.diagnostics = diagnostics;
  }

  public static ReactionPredictionResult of(
      final ReactionPredictionRequest request,
      final ReactionPredictionMethodType methodType,
      final List<ReactionProductCandidate> candidates,
      final List<ReactionPredictionEvidence> diagnostics) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction prediction result request must not be null.");
    }
    if (methodType == null) {
      throw new IllegalArgumentException(
          "Reaction prediction result method type must not be null.");
    }
    return new ReactionPredictionResult(
        request,
        methodType,
        ReactionPredictionResult.checkedCandidates(candidates),
        List.of(),
        ReactionPredictionResult.checkedDiagnostics(diagnostics));
  }

  public static ReactionPredictionResult of(
      final ReactionPredictionRequest request,
      final ReactionPredictionMethodType methodType,
      final List<ReactionProductCandidate> candidates,
      final List<ReactionMechanismChannelCandidate> mechanismChannels,
      final List<ReactionPredictionEvidence> diagnostics) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction prediction result request must not be null.");
    }
    if (methodType == null) {
      throw new IllegalArgumentException(
          "Reaction prediction result method type must not be null.");
    }
    return new ReactionPredictionResult(
        request,
        methodType,
        ReactionPredictionResult.checkedCandidates(candidates),
        ReactionPredictionResult.checkedMechanismChannels(mechanismChannels),
        ReactionPredictionResult.checkedDiagnostics(diagnostics));
  }

  public ReactionPredictionRequest request() {
    return this.request;
  }

  public ReactionPredictionMethodType methodType() {
    return this.methodType;
  }

  public List<ReactionProductCandidate> candidates() {
    return this.candidates;
  }

  public List<ReactionMechanismChannelCandidate> mechanismChannels() {
    return this.mechanismChannels;
  }

  public boolean hasMechanismChannels() {
    return !this.mechanismChannels.isEmpty();
  }

  public boolean hasCandidates() {
    return !this.candidates.isEmpty();
  }

  public ReactionProductCandidate bestCandidate() {
    if (this.candidates.isEmpty()) {
      throw new IllegalStateException("Reaction prediction result has no candidates.");
    }
    return this.candidates.get(0);
  }

  public double probabilityMass() {
    double mass = 0.0;
    for (int i = 0; i < this.candidates.size(); ++i) {
      mass += this.candidates.get(i).probability();
    }
    return mass;
  }

  public boolean probabilityMassComplete(final double tolerance) {
    if (!Double.isFinite(tolerance) || tolerance < 0.0) {
      throw new IllegalArgumentException(
          "Reaction prediction probability tolerance must be finite and non-negative.");
    }
    return Math.abs(1.0 - this.probabilityMass()) <= tolerance;
  }

  public double missingProbabilityMass() {
    return Math.max(0.0, 1.0 - this.probabilityMass());
  }

  public List<ReactionPredictionEvidence> diagnostics() {
    return this.diagnostics;
  }

  public boolean hasDiagnostics() {
    return !this.diagnostics.isEmpty();
  }

  public int diagnosticCount() {
    return this.diagnostics.size();
  }

  public boolean hasDiagnostic(final ReactionPredictionEvidenceKind kind) {
    return this.diagnosticCount(kind) > 0;
  }

  public int diagnosticCount(final ReactionPredictionEvidenceKind kind) {
    ReactionPredictionResult.requireEvidenceKind(kind);
    int count = 0;
    for (int i = 0; i < this.diagnostics.size(); ++i) {
      if (this.diagnostics.get(i).kind() == kind) {
        ++count;
      }
    }
    return count;
  }

  public int evidenceCount(final ReactionPredictionEvidenceKind kind) {
    ReactionPredictionResult.requireEvidenceKind(kind);
    int count = this.diagnosticCount(kind);
    for (int i = 0; i < this.candidates.size(); ++i) {
      count += this.candidates.get(i).evidenceCount(kind);
    }
    return count;
  }

  public double evidenceWeight(final ReactionPredictionEvidenceKind kind) {
    ReactionPredictionResult.requireEvidenceKind(kind);
    double weight = 0.0;
    for (int i = 0; i < this.diagnostics.size(); ++i) {
      final ReactionPredictionEvidence item = this.diagnostics.get(i);
      if (item.kind() == kind) {
        weight += item.weight();
      }
    }
    for (int i = 0; i < this.candidates.size(); ++i) {
      weight += this.candidates.get(i).evidenceWeight(kind);
    }
    return weight;
  }

  public boolean hasWeightedEvidence(final ReactionPredictionEvidenceKind kind) {
    return Math.abs(this.evidenceWeight(kind)) > 0.0;
  }

  public List<ReactionPredictionEvidence> diagnostics(final ReactionPredictionEvidenceKind kind) {
    ReactionPredictionResult.requireEvidenceKind(kind);
    final ArrayList<ReactionPredictionEvidence> found = new ArrayList<ReactionPredictionEvidence>();
    for (int i = 0; i < this.diagnostics.size(); ++i) {
      final ReactionPredictionEvidence diagnostic = this.diagnostics.get(i);
      if (diagnostic.kind() == kind) {
        found.add(diagnostic);
      }
    }
    return List.copyOf(found);
  }

  public List<ReactionProductCandidate> candidatesWithEvidence(
      final ReactionPredictionEvidenceKind kind) {
    ReactionPredictionResult.requireEvidenceKind(kind);
    final ArrayList<ReactionProductCandidate> found = new ArrayList<ReactionProductCandidate>();
    for (int i = 0; i < this.candidates.size(); ++i) {
      final ReactionProductCandidate candidate = this.candidates.get(i);
      if (candidate.hasEvidence(kind)) {
        found.add(candidate);
      }
    }
    return List.copyOf(found);
  }

  public int predictedMechanismCount() {
    int count = this.mechanismChannels.size();
    for (int i = 0; i < this.candidates.size(); ++i) {
      count += this.candidates.get(i).predictedMechanismCount();
    }
    return count;
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
      throw new IllegalArgumentException("Reaction prediction result candidate must not be null.");
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
      throw new IllegalArgumentException("Reaction prediction mechanism channel must not be null.");
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
      throw new IllegalArgumentException("Reaction prediction result diagnostic must not be null.");
    }
    return List.copyOf(diagnostics);
  }

  private static void requireEvidenceKind(final ReactionPredictionEvidenceKind kind) {
    if (kind == null) {
      throw new IllegalArgumentException("Reaction prediction evidence kind must not be null.");
    }
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionPredictionResult)) {
      return false;
    }
    final ReactionPredictionResult result = (ReactionPredictionResult) other;
    return Objects.equals(this.request, result.request)
        && this.methodType == result.methodType
        && Objects.equals(this.candidates, result.candidates)
        && Objects.equals(this.mechanismChannels, result.mechanismChannels)
        && Objects.equals(this.diagnostics, result.diagnostics);
  }

  public int hashCode() {
    return Objects.hash(
        this.request, this.methodType, this.candidates, this.mechanismChannels, this.diagnostics);
  }
}

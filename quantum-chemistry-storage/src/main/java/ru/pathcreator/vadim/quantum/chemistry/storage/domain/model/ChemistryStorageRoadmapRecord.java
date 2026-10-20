/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.storage.domain.model;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/**
 * Сохраненная расчетная дорожная карта между прогнозом продуктов и последующими расчетами.
 */
public final class ChemistryStorageRoadmapRecord {

  private final String id;
  private final String requestId;
  private final int historyIndex;
  private final String status;
  private final int conditionScenarioCount;
  private final int evaluatedConditionScenarioCount;
  private final List<ChemistryStorageRoadmapCandidateRecord> candidates;
  private final List<String> programLevelRecommendationTargets;
  private final String note;

  private ChemistryStorageRoadmapRecord(
      final String id,
      final String requestId,
      final int historyIndex,
      final String status,
      final int conditionScenarioCount,
      final int evaluatedConditionScenarioCount,
      final List<ChemistryStorageRoadmapCandidateRecord> candidates,
      final List<String> programLevelRecommendationTargets,
      final String note) {
    this.id = id;
    this.requestId = requestId;
    this.historyIndex = historyIndex;
    this.status = status;
    this.conditionScenarioCount = conditionScenarioCount;
    this.evaluatedConditionScenarioCount = evaluatedConditionScenarioCount;
    this.candidates = candidates;
    this.programLevelRecommendationTargets = programLevelRecommendationTargets;
    this.note = note;
  }

  public static ChemistryStorageRoadmapRecord of(
      final String id,
      final String requestId,
      final int historyIndex,
      final String status,
      final int conditionScenarioCount,
      final int evaluatedConditionScenarioCount,
      final List<ChemistryStorageRoadmapCandidateRecord> candidates,
      final List<String> programLevelRecommendationTargets,
      final String note) {
    if (historyIndex < 0) {
      throw new IllegalArgumentException("Storage roadmap history index must be non-negative.");
    }
    if (conditionScenarioCount < 0 || evaluatedConditionScenarioCount < 0) {
      throw new IllegalArgumentException("Storage roadmap scenario counts must be non-negative.");
    }
    if (evaluatedConditionScenarioCount > conditionScenarioCount) {
      throw new IllegalArgumentException(
          "Storage roadmap evaluated scenario count exceeds planned count.");
    }
    return new ChemistryStorageRoadmapRecord(
        IdentifierValue.requireIdentifier(id, "Storage roadmap id"),
        IdentifierValue.requireIdentifier(requestId, "Storage roadmap request id"),
        historyIndex,
        IdentifierValue.requireIdentifier(status, "Storage roadmap status"),
        conditionScenarioCount,
        evaluatedConditionScenarioCount,
        ChemistryStorageRoadmapRecord.checkedCandidates(candidates),
        ChemistryStorageRoadmapRecord.checkedTexts(
            programLevelRecommendationTargets, "program recommendation target"),
        TextValue.optionalText(note));
  }

  public String id() {
    return this.id;
  }

  public String requestId() {
    return this.requestId;
  }

  public int historyIndex() {
    return this.historyIndex;
  }

  public String status() {
    return this.status;
  }

  public int conditionScenarioCount() {
    return this.conditionScenarioCount;
  }

  public int evaluatedConditionScenarioCount() {
    return this.evaluatedConditionScenarioCount;
  }

  public List<ChemistryStorageRoadmapCandidateRecord> candidates() {
    return this.candidates;
  }

  public List<String> programLevelRecommendationTargets() {
    return this.programLevelRecommendationTargets;
  }

  public String note() {
    return this.note;
  }

  public boolean hasCandidates() {
    return !this.candidates.isEmpty();
  }

  public boolean hasNote() {
    return this.note != null;
  }

  private static List<ChemistryStorageRoadmapCandidateRecord> checkedCandidates(
      final List<ChemistryStorageRoadmapCandidateRecord> candidates) {
    if (candidates == null || candidates.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < candidates.size(); ++i) {
      final ChemistryStorageRoadmapCandidateRecord candidate = candidates.get(i);
      if (candidate == null) {
        throw new IllegalArgumentException("Storage roadmap candidate must not be null.");
      }
      if (candidate.candidateIndex() == i) {
        continue;
      }
      throw new IllegalArgumentException("Storage roadmap candidate index must match order.");
    }
    return List.copyOf(candidates);
  }

  private static List<String> checkedTexts(final List<String> values, final String subject) {
    if (values == null || values.isEmpty()) {
      return List.of();
    }
    final String[] checked = new String[values.size()];
    for (int i = 0; i < values.size(); ++i) {
      checked[i] = TextValue.requireText(values.get(i), subject);
    }
    return List.of(checked);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryStorageRoadmapRecord)) {
      return false;
    }
    final ChemistryStorageRoadmapRecord record = (ChemistryStorageRoadmapRecord) other;
    return this.historyIndex == record.historyIndex
        && this.conditionScenarioCount == record.conditionScenarioCount
        && this.evaluatedConditionScenarioCount == record.evaluatedConditionScenarioCount
        && Objects.equals(this.id, record.id)
        && Objects.equals(this.requestId, record.requestId)
        && Objects.equals(this.status, record.status)
        && Objects.equals(this.candidates, record.candidates)
        && Objects.equals(
            this.programLevelRecommendationTargets, record.programLevelRecommendationTargets)
        && Objects.equals(this.note, record.note);
  }

  public int hashCode() {
    return Objects.hash(
        this.id,
        this.requestId,
        this.historyIndex,
        this.status,
        this.conditionScenarioCount,
        this.evaluatedConditionScenarioCount,
        this.candidates,
        this.programLevelRecommendationTargets,
        this.note);
  }
}

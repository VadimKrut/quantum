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
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionMethodType;

/**
 * Снимок прогноза реакции с историей расчета, кандидатами, диагностикой и пояснениями.
 */
public final class ChemistryStoragePredictionRecord {

  private final String id;
  private final String requestId;
  private final int historyIndex;
  private final ReactionPredictionMethodType methodType;
  private final ChemistryStorageReactionRecordStatus status;
  private final List<ChemistryStoragePredictionCandidateRecord> candidates;
  private final List<String> diagnostics;
  private final String note;

  private ChemistryStoragePredictionRecord(
      final String id,
      final String requestId,
      final int historyIndex,
      final ReactionPredictionMethodType methodType,
      final ChemistryStorageReactionRecordStatus status,
      final List<ChemistryStoragePredictionCandidateRecord> candidates,
      final List<String> diagnostics,
      final String note) {
    this.id = id;
    this.requestId = requestId;
    this.historyIndex = historyIndex;
    this.methodType = methodType;
    this.status = status;
    this.candidates = candidates;
    this.diagnostics = diagnostics;
    this.note = note;
  }

  public static ChemistryStoragePredictionRecord of(
      final String id,
      final String requestId,
      final int historyIndex,
      final ReactionPredictionMethodType methodType,
      final ChemistryStorageReactionRecordStatus status,
      final List<ChemistryStoragePredictionCandidateRecord> candidates,
      final List<String> diagnostics,
      final String note) {
    if (historyIndex < 0) {
      throw new IllegalArgumentException("Prediction record history index must be non-negative.");
    }
    if (methodType == null) {
      throw new IllegalArgumentException("Prediction record method type must not be null.");
    }
    if (status == null) {
      throw new IllegalArgumentException("Prediction record status must not be null.");
    }
    return new ChemistryStoragePredictionRecord(
        IdentifierValue.requireIdentifier(id, "Prediction record id"),
        IdentifierValue.requireIdentifier(requestId, "Prediction record request id"),
        historyIndex,
        methodType,
        status,
        ChemistryStoragePredictionRecord.checkedCandidates(candidates),
        ChemistryStoragePredictionRecord.checkedTexts(diagnostics, "Prediction record diagnostic"),
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

  public ReactionPredictionMethodType methodType() {
    return this.methodType;
  }

  public ChemistryStorageReactionRecordStatus status() {
    return this.status;
  }

  public List<ChemistryStoragePredictionCandidateRecord> candidates() {
    return this.candidates;
  }

  public List<String> diagnostics() {
    return this.diagnostics;
  }

  public boolean hasDiagnostics() {
    return !this.diagnostics.isEmpty();
  }

  public String note() {
    return this.note;
  }

  public boolean hasNote() {
    return this.note != null;
  }

  private static List<ChemistryStoragePredictionCandidateRecord> checkedCandidates(
      final List<ChemistryStoragePredictionCandidateRecord> candidates) {
    if (candidates == null || candidates.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < candidates.size(); ++i) {
      if (candidates.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException("Prediction record candidate must not be null.");
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
    if (!(other instanceof ChemistryStoragePredictionRecord)) {
      return false;
    }
    final ChemistryStoragePredictionRecord record = (ChemistryStoragePredictionRecord) other;
    return this.historyIndex == record.historyIndex
        && Objects.equals(this.id, record.id)
        && Objects.equals(this.requestId, record.requestId)
        && this.methodType == record.methodType
        && this.status == record.status
        && Objects.equals(this.candidates, record.candidates)
        && Objects.equals(this.diagnostics, record.diagnostics)
        && Objects.equals(this.note, record.note);
  }

  public int hashCode() {
    return Objects.hash(
        this.id,
        this.requestId,
        this.historyIndex,
        this.methodType,
        this.status,
        this.candidates,
        this.diagnostics,
        this.note);
  }
}

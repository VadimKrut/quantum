/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.workflow.domain.parity;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/**
 * Одна связка: кандидат реакции, его механизм, обычные расчеты и Quantum IR-программы.
 */
public final class ChemistryComputationParityItem {

  private final int candidateIndex;
  private final List<String> mechanismIds;
  private final int pathwayStepCount;
  private final List<String> classicalPlanIds;
  private final List<String> classicalResultIds;
  private final List<String> quantumProgramIds;
  private final ChemistryComputationParityStatus status;
  private final List<ChemistryComputationParityDiagnostic> diagnostics;
  private final String explanation;

  private ChemistryComputationParityItem(
      final int candidateIndex,
      final List<String> mechanismIds,
      final int pathwayStepCount,
      final List<String> classicalPlanIds,
      final List<String> classicalResultIds,
      final List<String> quantumProgramIds,
      final ChemistryComputationParityStatus status,
      final List<ChemistryComputationParityDiagnostic> diagnostics,
      final String explanation) {
    this.candidateIndex = candidateIndex;
    this.mechanismIds = mechanismIds;
    this.pathwayStepCount = pathwayStepCount;
    this.classicalPlanIds = classicalPlanIds;
    this.classicalResultIds = classicalResultIds;
    this.quantumProgramIds = quantumProgramIds;
    this.status = status;
    this.diagnostics = diagnostics;
    this.explanation = explanation;
  }

  public static ChemistryComputationParityItem of(
      final int candidateIndex,
      final List<String> mechanismIds,
      final int pathwayStepCount,
      final List<String> classicalPlanIds,
      final List<String> classicalResultIds,
      final List<String> quantumProgramIds,
      final ChemistryComputationParityStatus status,
      final String explanation) {
    return ChemistryComputationParityItem.of(
        candidateIndex,
        mechanismIds,
        pathwayStepCount,
        classicalPlanIds,
        classicalResultIds,
        quantumProgramIds,
        status,
        List.of(),
        explanation);
  }

  public static ChemistryComputationParityItem of(
      final int candidateIndex,
      final List<String> mechanismIds,
      final int pathwayStepCount,
      final List<String> classicalPlanIds,
      final List<String> classicalResultIds,
      final List<String> quantumProgramIds,
      final ChemistryComputationParityStatus status,
      final List<ChemistryComputationParityDiagnostic> diagnostics,
      final String explanation) {
    if (candidateIndex < 0) {
      throw new IllegalArgumentException(
          "Chemistry computation parity candidate index must not be negative.");
    }
    if (pathwayStepCount < 0) {
      throw new IllegalArgumentException(
          "Chemistry computation parity pathway step count must not be negative.");
    }
    if (status == null) {
      throw new IllegalArgumentException("Chemistry computation parity status must not be null.");
    }
    return new ChemistryComputationParityItem(
        candidateIndex,
        ChemistryComputationParityItem.checkedTextList(mechanismIds, "mechanism id"),
        pathwayStepCount,
        ChemistryComputationParityItem.checkedTextList(classicalPlanIds, "classical plan id"),
        ChemistryComputationParityItem.checkedTextList(classicalResultIds, "classical result id"),
        ChemistryComputationParityItem.checkedTextList(quantumProgramIds, "quantum program id"),
        status,
        ChemistryComputationParityItem.checkedDiagnostics(diagnostics),
        TextValue.requireText(explanation, "Chemistry computation parity explanation"));
  }

  public int candidateIndex() {
    return this.candidateIndex;
  }

  public List<String> mechanismIds() {
    return this.mechanismIds;
  }

  public boolean hasMechanisms() {
    return !this.mechanismIds.isEmpty();
  }

  public int pathwayStepCount() {
    return this.pathwayStepCount;
  }

  public boolean hasPathwaySteps() {
    return this.pathwayStepCount > 0;
  }

  public List<String> classicalPlanIds() {
    return this.classicalPlanIds;
  }

  public boolean hasClassicalPlan() {
    return !this.classicalPlanIds.isEmpty();
  }

  public List<String> classicalResultIds() {
    return this.classicalResultIds;
  }

  public boolean hasClassicalResult() {
    return !this.classicalResultIds.isEmpty();
  }

  public List<String> quantumProgramIds() {
    return this.quantumProgramIds;
  }

  public boolean hasQuantumPrograms() {
    return !this.quantumProgramIds.isEmpty();
  }

  public ChemistryComputationParityStatus status() {
    return this.status;
  }

  public List<ChemistryComputationParityDiagnostic> diagnostics() {
    return this.diagnostics;
  }

  public boolean hasDiagnostics() {
    return !this.diagnostics.isEmpty();
  }

  public boolean hasBlockingDiagnostics() {
    for (int i = 0; i < this.diagnostics.size(); ++i) {
      if (this.diagnostics.get(i).error()) {
        return true;
      }
    }
    return false;
  }

  public boolean hasDiagnostic(final ChemistryComputationParityDiagnosticCode code) {
    if (code == null) {
      return false;
    }
    for (int i = 0; i < this.diagnostics.size(); ++i) {
      if (this.diagnostics.get(i).code() == code) {
        return true;
      }
    }
    return false;
  }

  public String explanation() {
    return this.explanation;
  }

  private static List<String> checkedTextList(final List<String> values, final String subjectName) {
    if (values == null || values.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < values.size(); ++i) {
      TextValue.requireText(values.get(i), "Chemistry computation parity " + subjectName);
    }
    return List.copyOf(values);
  }

  private static List<ChemistryComputationParityDiagnostic> checkedDiagnostics(
      final List<ChemistryComputationParityDiagnostic> diagnostics) {
    if (diagnostics == null || diagnostics.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < diagnostics.size(); ++i) {
      if (diagnostics.get(i) == null) {
        throw new IllegalArgumentException(
            "Chemistry computation parity diagnostic must not be null.");
      }
    }
    return List.copyOf(diagnostics);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryComputationParityItem)) {
      return false;
    }
    final ChemistryComputationParityItem item = (ChemistryComputationParityItem) other;
    return this.candidateIndex == item.candidateIndex
        && this.pathwayStepCount == item.pathwayStepCount
        && Objects.equals(this.mechanismIds, item.mechanismIds)
        && Objects.equals(this.classicalPlanIds, item.classicalPlanIds)
        && Objects.equals(this.classicalResultIds, item.classicalResultIds)
        && Objects.equals(this.quantumProgramIds, item.quantumProgramIds)
        && this.status == item.status
        && Objects.equals(this.diagnostics, item.diagnostics)
        && Objects.equals(this.explanation, item.explanation);
  }

  public int hashCode() {
    return Objects.hash(
        this.candidateIndex,
        this.mechanismIds,
        this.pathwayStepCount,
        this.classicalPlanIds,
        this.classicalResultIds,
        this.quantumProgramIds,
        this.status,
        this.diagnostics,
        this.explanation);
  }
}

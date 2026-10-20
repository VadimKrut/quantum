/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.workflow.domain.result;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.parity.ChemistryComputationParityStatus;

public final class ChemistryWorkflowCandidateTrace {

  private final int candidateIndex;
  private final double probability;
  private final double expectedYieldPercent;
  private final String selectivityNote;
  private final int evidenceCount;
  private final int predictedMechanismCount;
  private final int pathwayStepCount;
  private final List<String> classicalPlanIds;
  private final List<String> classicalResultIds;
  private final List<String> quantumProgramIds;
  private final ChemistryComputationParityStatus parityStatus;
  private final int diagnosticCount;
  private final int blockingDiagnosticCount;

  private ChemistryWorkflowCandidateTrace(
      final int candidateIndex,
      final double probability,
      final double expectedYieldPercent,
      final String selectivityNote,
      final int evidenceCount,
      final int predictedMechanismCount,
      final int pathwayStepCount,
      final List<String> classicalPlanIds,
      final List<String> classicalResultIds,
      final List<String> quantumProgramIds,
      final ChemistryComputationParityStatus parityStatus,
      final int diagnosticCount,
      final int blockingDiagnosticCount) {
    this.candidateIndex = candidateIndex;
    this.probability = probability;
    this.expectedYieldPercent = expectedYieldPercent;
    this.selectivityNote = selectivityNote;
    this.evidenceCount = evidenceCount;
    this.predictedMechanismCount = predictedMechanismCount;
    this.pathwayStepCount = pathwayStepCount;
    this.classicalPlanIds = classicalPlanIds;
    this.classicalResultIds = classicalResultIds;
    this.quantumProgramIds = quantumProgramIds;
    this.parityStatus = parityStatus;
    this.diagnosticCount = diagnosticCount;
    this.blockingDiagnosticCount = blockingDiagnosticCount;
  }

  public static ChemistryWorkflowCandidateTrace of(
      final int candidateIndex,
      final double probability,
      final double expectedYieldPercent,
      final String selectivityNote,
      final int evidenceCount,
      final int predictedMechanismCount,
      final int pathwayStepCount,
      final List<String> classicalPlanIds,
      final List<String> classicalResultIds,
      final List<String> quantumProgramIds,
      final ChemistryComputationParityStatus parityStatus,
      final int diagnosticCount,
      final int blockingDiagnosticCount) {
    if (candidateIndex < 0) {
      throw new IllegalArgumentException(
          "Chemistry workflow candidate trace index must not be negative.");
    }
    if (!Double.isFinite(probability) || probability < 0.0 || probability > 1.0) {
      throw new IllegalArgumentException(
          "Chemistry workflow candidate trace probability must be in [0, 1].");
    }
    if (!Double.isFinite(expectedYieldPercent)
        || expectedYieldPercent < 0.0
        || expectedYieldPercent > 100.0) {
      throw new IllegalArgumentException(
          "Chemistry workflow candidate trace expected yield percent must be in [0, 100].");
    }
    if (evidenceCount < 0) {
      throw new IllegalArgumentException(
          "Chemistry workflow candidate trace evidence count must not be negative.");
    }
    if (predictedMechanismCount < 0) {
      throw new IllegalArgumentException(
          "Chemistry workflow candidate trace mechanism count must not be negative.");
    }
    if (pathwayStepCount < 0) {
      throw new IllegalArgumentException(
          "Chemistry workflow candidate trace pathway step count must not be negative.");
    }
    if (parityStatus == null) {
      throw new IllegalArgumentException(
          "Chemistry workflow candidate trace parity status must not be null.");
    }
    if (diagnosticCount < 0
        || blockingDiagnosticCount < 0
        || blockingDiagnosticCount > diagnosticCount) {
      throw new IllegalArgumentException(
          "Chemistry workflow candidate trace diagnostic counts are inconsistent.");
    }
    return new ChemistryWorkflowCandidateTrace(
        candidateIndex,
        probability,
        expectedYieldPercent,
        TextValue.optionalText(selectivityNote),
        evidenceCount,
        predictedMechanismCount,
        pathwayStepCount,
        ChemistryWorkflowCandidateTrace.checkedTextList(classicalPlanIds, "classical plan id"),
        ChemistryWorkflowCandidateTrace.checkedTextList(classicalResultIds, "classical result id"),
        ChemistryWorkflowCandidateTrace.checkedTextList(quantumProgramIds, "quantum program id"),
        parityStatus,
        diagnosticCount,
        blockingDiagnosticCount);
  }

  public int candidateIndex() {
    return this.candidateIndex;
  }

  public double probability() {
    return this.probability;
  }

  public double expectedYieldPercent() {
    return this.expectedYieldPercent;
  }

  public String selectivityNote() {
    return this.selectivityNote;
  }

  public boolean hasSelectivityNote() {
    return this.selectivityNote != null;
  }

  public int evidenceCount() {
    return this.evidenceCount;
  }

  public int predictedMechanismCount() {
    return this.predictedMechanismCount;
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

  public boolean hasClassicalResults() {
    return !this.classicalResultIds.isEmpty();
  }

  public List<String> quantumProgramIds() {
    return this.quantumProgramIds;
  }

  public boolean hasQuantumPrograms() {
    return !this.quantumProgramIds.isEmpty();
  }

  public ChemistryComputationParityStatus parityStatus() {
    return this.parityStatus;
  }

  public int diagnosticCount() {
    return this.diagnosticCount;
  }

  public int blockingDiagnosticCount() {
    return this.blockingDiagnosticCount;
  }

  public boolean hasDiagnostics() {
    return this.diagnosticCount > 0;
  }

  public boolean hasBlockingDiagnostics() {
    return this.blockingDiagnosticCount > 0;
  }

  public boolean readyForClassicalQuantumParity() {
    return this.parityStatus == ChemistryComputationParityStatus.READY
        && !this.hasBlockingDiagnostics();
  }

  private static List<String> checkedTextList(final List<String> values, final String subjectName) {
    if (values == null || values.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < values.size(); ++i) {
      TextValue.requireText(values.get(i), "Chemistry workflow candidate trace " + subjectName);
    }
    return List.copyOf(values);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryWorkflowCandidateTrace)) {
      return false;
    }
    final ChemistryWorkflowCandidateTrace trace = (ChemistryWorkflowCandidateTrace) other;
    return this.candidateIndex == trace.candidateIndex
        && Double.compare(this.probability, trace.probability) == 0
        && Double.compare(this.expectedYieldPercent, trace.expectedYieldPercent) == 0
        && this.evidenceCount == trace.evidenceCount
        && this.predictedMechanismCount == trace.predictedMechanismCount
        && this.pathwayStepCount == trace.pathwayStepCount
        && this.diagnosticCount == trace.diagnosticCount
        && this.blockingDiagnosticCount == trace.blockingDiagnosticCount
        && Objects.equals(this.selectivityNote, trace.selectivityNote)
        && Objects.equals(this.classicalPlanIds, trace.classicalPlanIds)
        && Objects.equals(this.classicalResultIds, trace.classicalResultIds)
        && Objects.equals(this.quantumProgramIds, trace.quantumProgramIds)
        && this.parityStatus == trace.parityStatus;
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.candidateIndex);
    result = ChemistryHash.include(result, this.probability);
    result = ChemistryHash.include(result, this.expectedYieldPercent);
    result = ChemistryHash.include(result, this.selectivityNote);
    result = ChemistryHash.include(result, this.evidenceCount);
    result = ChemistryHash.include(result, this.predictedMechanismCount);
    result = ChemistryHash.include(result, this.pathwayStepCount);
    result = ChemistryHash.include(result, this.classicalPlanIds);
    result = ChemistryHash.include(result, this.classicalResultIds);
    result = ChemistryHash.include(result, this.quantumProgramIds);
    result = ChemistryHash.include(result, this.parityStatus);
    result = ChemistryHash.include(result, this.diagnosticCount);
    result = ChemistryHash.include(result, this.blockingDiagnosticCount);
    return result;
  }
}

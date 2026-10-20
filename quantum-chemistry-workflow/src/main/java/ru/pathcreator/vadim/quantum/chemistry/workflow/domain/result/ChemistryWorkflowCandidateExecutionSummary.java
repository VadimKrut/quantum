/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.workflow.domain.result;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.parity.ChemistryComputationParityStatus;

public final class ChemistryWorkflowCandidateExecutionSummary {

  private final int candidateIndex;
  private final double probability;
  private final double expectedYieldPercent;
  private final ChemistryComputationParityStatus parityStatus;
  private final int classicalPlanCount;
  private final int expectedClassicalResultCount;
  private final int executedClassicalResultCount;
  private final int successfulClassicalResultCount;
  private final int partialClassicalResultCount;
  private final int failedClassicalResultCount;
  private final int unsupportedClassicalResultCount;
  private final int missingClassicalResultCount;
  private final int quantumProgramCount;
  private final int diagnosticCount;
  private final int blockingDiagnosticCount;

  private ChemistryWorkflowCandidateExecutionSummary(
      final int candidateIndex,
      final double probability,
      final double expectedYieldPercent,
      final ChemistryComputationParityStatus parityStatus,
      final int classicalPlanCount,
      final int expectedClassicalResultCount,
      final int executedClassicalResultCount,
      final int successfulClassicalResultCount,
      final int partialClassicalResultCount,
      final int failedClassicalResultCount,
      final int unsupportedClassicalResultCount,
      final int missingClassicalResultCount,
      final int quantumProgramCount,
      final int diagnosticCount,
      final int blockingDiagnosticCount) {
    this.candidateIndex = candidateIndex;
    this.probability = probability;
    this.expectedYieldPercent = expectedYieldPercent;
    this.parityStatus = parityStatus;
    this.classicalPlanCount = classicalPlanCount;
    this.expectedClassicalResultCount = expectedClassicalResultCount;
    this.executedClassicalResultCount = executedClassicalResultCount;
    this.successfulClassicalResultCount = successfulClassicalResultCount;
    this.partialClassicalResultCount = partialClassicalResultCount;
    this.failedClassicalResultCount = failedClassicalResultCount;
    this.unsupportedClassicalResultCount = unsupportedClassicalResultCount;
    this.missingClassicalResultCount = missingClassicalResultCount;
    this.quantumProgramCount = quantumProgramCount;
    this.diagnosticCount = diagnosticCount;
    this.blockingDiagnosticCount = blockingDiagnosticCount;
  }

  public static ChemistryWorkflowCandidateExecutionSummary of(
      final int candidateIndex,
      final double probability,
      final double expectedYieldPercent,
      final ChemistryComputationParityStatus parityStatus,
      final int classicalPlanCount,
      final int expectedClassicalResultCount,
      final int executedClassicalResultCount,
      final int successfulClassicalResultCount,
      final int partialClassicalResultCount,
      final int failedClassicalResultCount,
      final int unsupportedClassicalResultCount,
      final int missingClassicalResultCount,
      final int quantumProgramCount,
      final int diagnosticCount,
      final int blockingDiagnosticCount) {
    if (candidateIndex < 0) {
      throw new IllegalArgumentException(
          "Chemistry workflow candidate summary index must not be negative.");
    }
    if (!Double.isFinite(probability) || probability < 0.0 || probability > 1.0) {
      throw new IllegalArgumentException(
          "Chemistry workflow candidate summary probability must be in [0, 1].");
    }
    if (!Double.isFinite(expectedYieldPercent)
        || expectedYieldPercent < 0.0
        || expectedYieldPercent > 100.0) {
      throw new IllegalArgumentException(
          "Chemistry workflow candidate summary expected yield percent must be in [0, 100].");
    }
    if (parityStatus == null) {
      throw new IllegalArgumentException(
          "Chemistry workflow candidate summary parity status must not be null.");
    }
    ChemistryWorkflowCandidateExecutionSummary.requireNonNegative(
        classicalPlanCount, "classical plan count");
    ChemistryWorkflowCandidateExecutionSummary.requireNonNegative(
        expectedClassicalResultCount, "expected classical result count");
    ChemistryWorkflowCandidateExecutionSummary.requireNonNegative(
        executedClassicalResultCount, "executed classical result count");
    ChemistryWorkflowCandidateExecutionSummary.requireNonNegative(
        successfulClassicalResultCount, "successful classical result count");
    ChemistryWorkflowCandidateExecutionSummary.requireNonNegative(
        partialClassicalResultCount, "partial classical result count");
    ChemistryWorkflowCandidateExecutionSummary.requireNonNegative(
        failedClassicalResultCount, "failed classical result count");
    ChemistryWorkflowCandidateExecutionSummary.requireNonNegative(
        unsupportedClassicalResultCount, "unsupported classical result count");
    ChemistryWorkflowCandidateExecutionSummary.requireNonNegative(
        missingClassicalResultCount, "missing classical result count");
    ChemistryWorkflowCandidateExecutionSummary.requireNonNegative(
        quantumProgramCount, "quantum program count");
    if (diagnosticCount < 0
        || blockingDiagnosticCount < 0
        || blockingDiagnosticCount > diagnosticCount) {
      throw new IllegalArgumentException(
          "Chemistry workflow candidate summary diagnostic counts are inconsistent.");
    }
    final int statusCount =
        successfulClassicalResultCount
            + partialClassicalResultCount
            + failedClassicalResultCount
            + unsupportedClassicalResultCount;
    if (statusCount != executedClassicalResultCount) {
      throw new IllegalArgumentException(
          "Chemistry workflow candidate summary result status counts are inconsistent.");
    }
    if (missingClassicalResultCount
        != Math.max(0, expectedClassicalResultCount - executedClassicalResultCount)) {
      throw new IllegalArgumentException(
          "Chemistry workflow candidate summary missing result count is inconsistent.");
    }
    return new ChemistryWorkflowCandidateExecutionSummary(
        candidateIndex,
        probability,
        expectedYieldPercent,
        parityStatus,
        classicalPlanCount,
        expectedClassicalResultCount,
        executedClassicalResultCount,
        successfulClassicalResultCount,
        partialClassicalResultCount,
        failedClassicalResultCount,
        unsupportedClassicalResultCount,
        missingClassicalResultCount,
        quantumProgramCount,
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

  public ChemistryComputationParityStatus parityStatus() {
    return this.parityStatus;
  }

  public int classicalPlanCount() {
    return this.classicalPlanCount;
  }

  public int expectedClassicalResultCount() {
    return this.expectedClassicalResultCount;
  }

  public int executedClassicalResultCount() {
    return this.executedClassicalResultCount;
  }

  public int successfulClassicalResultCount() {
    return this.successfulClassicalResultCount;
  }

  public int partialClassicalResultCount() {
    return this.partialClassicalResultCount;
  }

  public int failedClassicalResultCount() {
    return this.failedClassicalResultCount;
  }

  public int unsupportedClassicalResultCount() {
    return this.unsupportedClassicalResultCount;
  }

  public int missingClassicalResultCount() {
    return this.missingClassicalResultCount;
  }

  public int quantumProgramCount() {
    return this.quantumProgramCount;
  }

  public int diagnosticCount() {
    return this.diagnosticCount;
  }

  public int blockingDiagnosticCount() {
    return this.blockingDiagnosticCount;
  }

  public boolean hasClassicalPlan() {
    return this.classicalPlanCount > 0;
  }

  public boolean hasExecutedClassicalResults() {
    return this.executedClassicalResultCount > 0;
  }

  public boolean hasMissingClassicalResults() {
    return this.missingClassicalResultCount > 0;
  }

  public boolean hasQuantumPrograms() {
    return this.quantumProgramCount > 0;
  }

  public boolean hasBlockingDiagnostics() {
    return this.blockingDiagnosticCount > 0;
  }

  public boolean readyForDecision() {
    return !this.hasBlockingDiagnostics()
        && !this.hasMissingClassicalResults()
        && (this.successfulClassicalResultCount > 0 || this.hasQuantumPrograms());
  }

  private static void requireNonNegative(final int value, final String subjectName) {
    if (value < 0) {
      throw new IllegalArgumentException(
          "Chemistry workflow candidate summary " + subjectName + " must not be negative.");
    }
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryWorkflowCandidateExecutionSummary)) {
      return false;
    }
    final ChemistryWorkflowCandidateExecutionSummary summary =
        (ChemistryWorkflowCandidateExecutionSummary) other;
    return this.candidateIndex == summary.candidateIndex
        && Double.compare(this.probability, summary.probability) == 0
        && Double.compare(this.expectedYieldPercent, summary.expectedYieldPercent) == 0
        && this.classicalPlanCount == summary.classicalPlanCount
        && this.expectedClassicalResultCount == summary.expectedClassicalResultCount
        && this.executedClassicalResultCount == summary.executedClassicalResultCount
        && this.successfulClassicalResultCount == summary.successfulClassicalResultCount
        && this.partialClassicalResultCount == summary.partialClassicalResultCount
        && this.failedClassicalResultCount == summary.failedClassicalResultCount
        && this.unsupportedClassicalResultCount == summary.unsupportedClassicalResultCount
        && this.missingClassicalResultCount == summary.missingClassicalResultCount
        && this.quantumProgramCount == summary.quantumProgramCount
        && this.diagnosticCount == summary.diagnosticCount
        && this.blockingDiagnosticCount == summary.blockingDiagnosticCount
        && this.parityStatus == summary.parityStatus;
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.candidateIndex);
    result = ChemistryHash.include(result, this.probability);
    result = ChemistryHash.include(result, this.expectedYieldPercent);
    result = ChemistryHash.include(result, this.parityStatus);
    result = ChemistryHash.include(result, this.classicalPlanCount);
    result = ChemistryHash.include(result, this.expectedClassicalResultCount);
    result = ChemistryHash.include(result, this.executedClassicalResultCount);
    result = ChemistryHash.include(result, this.successfulClassicalResultCount);
    result = ChemistryHash.include(result, this.partialClassicalResultCount);
    result = ChemistryHash.include(result, this.failedClassicalResultCount);
    result = ChemistryHash.include(result, this.unsupportedClassicalResultCount);
    result = ChemistryHash.include(result, this.missingClassicalResultCount);
    result = ChemistryHash.include(result, this.quantumProgramCount);
    result = ChemistryHash.include(result, this.diagnosticCount);
    result = ChemistryHash.include(result, this.blockingDiagnosticCount);
    return result;
  }
}

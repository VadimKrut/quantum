/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.workflow.domain.result;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationStatus;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.parity.ChemistryComputationParityItem;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.parity.ChemistryComputationParityPlan;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.parity.ChemistryComputationParityStatus;

/** Полный результат запуска chemistry workflow вместе с выполненными follow-up расчетами и parity-планом. */
public final class ChemistryWorkflowExecutionBundle {

  private final ChemistryWorkflowResult workflowResult;
  private final List<ClassicalCalculationResult> followUpResults;
  private final ChemistryComputationParityPlan computationParityPlan;
  private final List<ChemistryWorkflowCandidateTrace> candidateTraces;
  private final List<ChemistryWorkflowCandidateExecutionSummary> candidateExecutionSummaries;

  private ChemistryWorkflowExecutionBundle(
      final ChemistryWorkflowResult workflowResult,
      final List<ClassicalCalculationResult> followUpResults,
      final ChemistryComputationParityPlan computationParityPlan,
      final List<ChemistryWorkflowCandidateTrace> candidateTraces,
      final List<ChemistryWorkflowCandidateExecutionSummary> candidateExecutionSummaries) {
    this.workflowResult = workflowResult;
    this.followUpResults = followUpResults;
    this.computationParityPlan = computationParityPlan;
    this.candidateTraces = candidateTraces;
    this.candidateExecutionSummaries = candidateExecutionSummaries;
  }

  public static ChemistryWorkflowExecutionBundle of(
      final ChemistryWorkflowResult workflowResult,
      final List<ClassicalCalculationResult> followUpResults,
      final ChemistryComputationParityPlan computationParityPlan) {
    if (workflowResult == null) {
      throw new IllegalArgumentException("Chemistry workflow execution result must not be null.");
    }
    final List<ClassicalCalculationResult> checkedResults =
        ChemistryWorkflowExecutionBundle.checkedFollowUpResults(followUpResults);
    final ChemistryComputationParityPlan checkedParityPlan =
        computationParityPlan == null
            ? ChemistryComputationParityPlan.EMPTY
            : computationParityPlan;
    final List<ChemistryWorkflowCandidateTrace> traces =
        ChemistryWorkflowExecutionBundle.candidateTraces(workflowResult, checkedParityPlan);
    return new ChemistryWorkflowExecutionBundle(
        workflowResult,
        checkedResults,
        checkedParityPlan,
        traces,
        ChemistryWorkflowExecutionBundle.candidateExecutionSummaries(traces, checkedResults));
  }

  public ChemistryWorkflowResult workflowResult() {
    return this.workflowResult;
  }

  public List<ClassicalCalculationResult> followUpResults() {
    return this.followUpResults;
  }

  public boolean hasFollowUpResults() {
    return !this.followUpResults.isEmpty();
  }

  public int followUpResultCount() {
    return this.followUpResults.size();
  }

  public ChemistryComputationParityPlan computationParityPlan() {
    return this.computationParityPlan;
  }

  public boolean hasComputationParityPlan() {
    return !this.computationParityPlan.empty();
  }

  public List<ChemistryWorkflowCandidateTrace> candidateTraces() {
    return this.candidateTraces;
  }

  public boolean hasCandidateTraces() {
    return !this.candidateTraces.isEmpty();
  }

  public int candidateTraceCount() {
    return this.candidateTraces.size();
  }

  public ChemistryWorkflowCandidateTrace candidateTrace(final int candidateIndex) {
    if (candidateIndex < 0) {
      throw new IllegalArgumentException(
          "Chemistry workflow candidate trace index must not be negative.");
    }
    for (int i = 0; i < this.candidateTraces.size(); ++i) {
      if (this.candidateTraces.get(i).candidateIndex() == candidateIndex) {
        return this.candidateTraces.get(i);
      }
    }
    throw new IllegalArgumentException(
        "Chemistry workflow candidate trace not found: " + candidateIndex + ".");
  }

  public List<ChemistryWorkflowCandidateExecutionSummary> candidateExecutionSummaries() {
    return this.candidateExecutionSummaries;
  }

  public boolean hasCandidateExecutionSummaries() {
    return !this.candidateExecutionSummaries.isEmpty();
  }

  public int candidateExecutionSummaryCount() {
    return this.candidateExecutionSummaries.size();
  }

  public ChemistryWorkflowCandidateExecutionSummary candidateExecutionSummary(
      final int candidateIndex) {
    if (candidateIndex < 0) {
      throw new IllegalArgumentException(
          "Chemistry workflow candidate summary index must not be negative.");
    }
    for (int i = 0; i < this.candidateExecutionSummaries.size(); ++i) {
      if (this.candidateExecutionSummaries.get(i).candidateIndex() == candidateIndex) {
        return this.candidateExecutionSummaries.get(i);
      }
    }
    throw new IllegalArgumentException(
        "Chemistry workflow candidate summary not found: " + candidateIndex + ".");
  }

  private static List<ClassicalCalculationResult> checkedFollowUpResults(
      final List<ClassicalCalculationResult> results) {
    if (results == null || results.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < results.size(); ++i) {
      if (results.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException("Chemistry workflow follow-up result must not be null.");
    }
    return List.copyOf(results);
  }

  private static List<ChemistryWorkflowCandidateTrace> candidateTraces(
      final ChemistryWorkflowResult workflowResult,
      final ChemistryComputationParityPlan parityPlan) {
    if (!workflowResult.hasReactionPredictionResult()
        || !workflowResult.reactionPredictionResult().hasCandidates()) {
      return List.of();
    }
    final List<ReactionProductCandidate> candidates =
        workflowResult.reactionPredictionResult().candidates();
    final ArrayList<ChemistryWorkflowCandidateTrace> traces =
        new ArrayList<ChemistryWorkflowCandidateTrace>(candidates.size());
    for (int i = 0; i < candidates.size(); ++i) {
      final ReactionProductCandidate candidate = candidates.get(i);
      final ChemistryComputationParityItem parityItem =
          ChemistryWorkflowExecutionBundle.parityItemOrNull(parityPlan, i);
      traces.add(
          ChemistryWorkflowCandidateTrace.of(
              i,
              candidate.probability(),
              candidate.expectedYieldPercent(),
              candidate.selectivityNote(),
              candidate.evidence().size(),
              candidate.predictedMechanismCount(),
              parityItem == null ? 0 : parityItem.pathwayStepCount(),
              parityItem == null ? List.of() : parityItem.classicalPlanIds(),
              parityItem == null ? List.of() : parityItem.classicalResultIds(),
              parityItem == null ? List.of() : parityItem.quantumProgramIds(),
              parityItem == null
                  ? ChemistryComputationParityStatus.MISSING_BOTH
                  : parityItem.status(),
              parityItem == null ? 0 : parityItem.diagnostics().size(),
              parityItem == null
                  ? 0
                  : ChemistryWorkflowExecutionBundle.blockingDiagnosticCount(parityItem)));
    }
    return List.copyOf(traces);
  }

  private static List<ChemistryWorkflowCandidateExecutionSummary> candidateExecutionSummaries(
      final List<ChemistryWorkflowCandidateTrace> traces,
      final List<ClassicalCalculationResult> followUpResults) {
    if (traces.isEmpty()) {
      return List.of();
    }
    final ArrayList<ChemistryWorkflowCandidateExecutionSummary> summaries =
        new ArrayList<ChemistryWorkflowCandidateExecutionSummary>(traces.size());
    for (int i = 0; i < traces.size(); ++i) {
      final ChemistryWorkflowCandidateTrace trace = traces.get(i);
      summaries.add(
          ChemistryWorkflowExecutionBundle.candidateExecutionSummary(trace, followUpResults));
    }
    return List.copyOf(summaries);
  }

  private static ChemistryWorkflowCandidateExecutionSummary candidateExecutionSummary(
      final ChemistryWorkflowCandidateTrace trace,
      final List<ClassicalCalculationResult> followUpResults) {
    int successfulCount = 0;
    int partialCount = 0;
    int failedCount = 0;
    int unsupportedCount = 0;
    for (int i = 0; i < trace.classicalResultIds().size(); ++i) {
      final ClassicalCalculationResult result =
          ChemistryWorkflowExecutionBundle.followUpResultOrNull(
              followUpResults, trace.classicalResultIds().get(i));
      if (result == null) {
        continue;
      }
      if (result.status() == ClassicalCalculationStatus.SUCCEEDED) {
        ++successfulCount;
      } else if (result.status() == ClassicalCalculationStatus.PARTIAL) {
        ++partialCount;
      } else if (result.status() == ClassicalCalculationStatus.FAILED) {
        ++failedCount;
      } else if (result.status() == ClassicalCalculationStatus.UNSUPPORTED) {
        ++unsupportedCount;
      }
    }
    final int executedCount = successfulCount + partialCount + failedCount + unsupportedCount;
    final int expectedCount = trace.classicalResultIds().size();
    return ChemistryWorkflowCandidateExecutionSummary.of(
        trace.candidateIndex(),
        trace.probability(),
        trace.expectedYieldPercent(),
        trace.parityStatus(),
        trace.classicalPlanIds().size(),
        expectedCount,
        executedCount,
        successfulCount,
        partialCount,
        failedCount,
        unsupportedCount,
        Math.max(0, expectedCount - executedCount),
        trace.quantumProgramIds().size(),
        trace.diagnosticCount(),
        trace.blockingDiagnosticCount());
  }

  private static ClassicalCalculationResult followUpResultOrNull(
      final List<ClassicalCalculationResult> followUpResults, final String calculationId) {
    for (int i = 0; i < followUpResults.size(); ++i) {
      if (followUpResults.get(i).calculationId().value().equals(calculationId)) {
        return followUpResults.get(i);
      }
    }
    return null;
  }

  private static ChemistryComputationParityItem parityItemOrNull(
      final ChemistryComputationParityPlan parityPlan, final int candidateIndex) {
    if (parityPlan == null || parityPlan.empty()) {
      return null;
    }
    final List<ChemistryComputationParityItem> items = parityPlan.items();
    for (int i = 0; i < items.size(); ++i) {
      if (items.get(i).candidateIndex() == candidateIndex) {
        return items.get(i);
      }
    }
    return null;
  }

  private static int blockingDiagnosticCount(final ChemistryComputationParityItem item) {
    int count = 0;
    for (int i = 0; i < item.diagnostics().size(); ++i) {
      if (item.diagnostics().get(i).error()) {
        ++count;
      }
    }
    return count;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryWorkflowExecutionBundle)) {
      return false;
    }
    final ChemistryWorkflowExecutionBundle bundle = (ChemistryWorkflowExecutionBundle) other;
    return Objects.equals(this.workflowResult, bundle.workflowResult)
        && Objects.equals(this.followUpResults, bundle.followUpResults)
        && Objects.equals(this.computationParityPlan, bundle.computationParityPlan)
        && Objects.equals(this.candidateTraces, bundle.candidateTraces)
        && Objects.equals(this.candidateExecutionSummaries, bundle.candidateExecutionSummaries);
  }

  public int hashCode() {
    return Objects.hash(
        this.workflowResult,
        this.followUpResults,
        this.computationParityPlan,
        this.candidateTraces,
        this.candidateExecutionSummaries);
  }
}

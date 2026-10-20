/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.workflow.application;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.coverage.ClassicalCalculationCoverageItem;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.coverage.ClassicalCoverageStatus;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationStatus;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultSection;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.PredictedReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionConfidenceLevel;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionExplanation;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionMechanismClass;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramArtifact;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramKind;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramPackage;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramSkip;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.parity.ChemistryComputationParityDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.parity.ChemistryComputationParityDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.parity.ChemistryComputationParityDiagnosticSeverity;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.parity.ChemistryComputationParityItem;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.parity.ChemistryComputationParityPlan;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.parity.ChemistryComputationParityStatus;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendation;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendationPlan;

/**
 * Собирает явную связку между прогнозом реакции, обычными расчетами и Quantum IR-программами.
 */
public final class ChemistryComputationParityPlanner {

  public ChemistryComputationParityPlan plan(
      final String workflowJobId,
      final ReactionPredictionResult predictionResult,
      final ReactionPredictionExplanation explanation,
      final ChemistryWorkflowRecommendationPlan recommendationPlan,
      final ClassicalCalculationResult classicalCalculationResult,
      final ChemistryQuantumProgramPackage quantumProgramPackage) {
    final List<ClassicalCalculationResult> classicalCalculationResults =
        classicalCalculationResult == null ? List.of() : List.of(classicalCalculationResult);
    return this.planWithClassicalResults(
        workflowJobId,
        predictionResult,
        explanation,
        recommendationPlan,
        classicalCalculationResults,
        quantumProgramPackage);
  }

  public ChemistryComputationParityPlan planWithClassicalResults(
      final String workflowJobId,
      final ReactionPredictionResult predictionResult,
      final ReactionPredictionExplanation explanation,
      final ChemistryWorkflowRecommendationPlan recommendationPlan,
      final List<ClassicalCalculationResult> classicalCalculationResults,
      final ChemistryQuantumProgramPackage quantumProgramPackage) {
    if (workflowJobId == null || workflowJobId.isBlank()) {
      throw new IllegalArgumentException(
          "Chemistry computation parity workflow job id must not be blank.");
    }
    if (predictionResult == null || !predictionResult.hasCandidates()) {
      return ChemistryComputationParityPlan.EMPTY;
    }
    final ArrayList<ChemistryComputationParityItem> items =
        new ArrayList<ChemistryComputationParityItem>(predictionResult.candidates().size());
    final List<ReactionProductCandidate> candidates = predictionResult.candidates();
    for (int candidateIndex = 0; candidateIndex < candidates.size(); ++candidateIndex) {
      final List<String> mechanismIds =
          ChemistryComputationParityPlanner.mechanismIds(
              candidateIndex, candidates.get(candidateIndex), explanation);
      final int pathwayStepCount =
          ChemistryComputationParityPlanner.pathwayStepCount(candidateIndex, explanation);
      final List<String> classicalPlanIds =
          ChemistryComputationParityPlanner.classicalPlanIds(
              workflowJobId, candidateIndex, recommendationPlan);
      final List<String> classicalResultIds =
          ChemistryComputationParityPlanner.classicalResultIds(
              candidateIndex, classicalCalculationResults);
      final List<String> quantumProgramIds =
          ChemistryComputationParityPlanner.quantumProgramIds(
              candidateIndex, quantumProgramPackage, classicalCalculationResults);
      final List<ChemistryComputationParityDiagnostic> diagnostics =
          ChemistryComputationParityPlanner.diagnostics(
              candidateIndex,
              predictionResult,
              explanation,
              mechanismIds,
              pathwayStepCount,
              classicalPlanIds,
              classicalResultIds,
              classicalCalculationResults,
              quantumProgramIds,
              quantumProgramPackage);
      items.add(
          ChemistryComputationParityItem.of(
              candidateIndex,
              mechanismIds,
              pathwayStepCount,
              classicalPlanIds,
              classicalResultIds,
              quantumProgramIds,
              ChemistryComputationParityPlanner.status(
                  classicalPlanIds, classicalResultIds, quantumProgramIds),
              diagnostics,
              ChemistryComputationParityPlanner.explanation(
                  candidateIndex, candidates.get(candidateIndex), diagnostics)));
    }
    return ChemistryComputationParityPlan.of(workflowJobId, items);
  }

  private static List<String> mechanismIds(
      final int candidateIndex,
      final ReactionProductCandidate candidate,
      final ReactionPredictionExplanation explanation) {
    final List<PredictedReactionMechanism> mechanisms = candidate.predictedMechanisms();
    final ArrayList<String> ids = new ArrayList<String>(mechanisms.size());
    for (int i = 0; i < mechanisms.size(); ++i) {
      ids.add(mechanisms.get(i).mechanismId());
    }
    if (!ids.isEmpty()) {
      return List.copyOf(ids);
    }
    if (candidateIndex == 0
        && explanation != null
        && explanation.pathwayAssessment().mechanismClass() != ReactionMechanismClass.UNKNOWN) {
      return List.of(
          "assessed_route_"
              + explanation.pathwayAssessment().mechanismClass().name().toLowerCase(Locale.ROOT));
    }
    return List.copyOf(ids);
  }

  private static int pathwayStepCount(
      final int candidateIndex, final ReactionPredictionExplanation explanation) {
    if (candidateIndex != 0
        || explanation == null
        || !explanation.pathwayAssessment().hasPathwaySteps()) {
      return 0;
    }
    return explanation.pathwayAssessment().pathwaySteps().size();
  }

  private static List<String> classicalPlanIds(
      final String workflowJobId,
      final int candidateIndex,
      final ChemistryWorkflowRecommendationPlan recommendationPlan) {
    if (recommendationPlan == null || recommendationPlan.empty()) {
      return List.of();
    }
    final List<ChemistryWorkflowRecommendation> recommendations =
        recommendationPlan.recommendations();
    final ArrayList<String> ids = new ArrayList<String>(recommendations.size());
    for (int i = 0; i < recommendations.size(); ++i) {
      final ChemistryWorkflowRecommendation recommendation = recommendations.get(i);
      if (!recommendation.hasCalculationKind()
          || !ChemistryComputationParityPlanner.appliesToCandidate(
              recommendation, candidateIndex)) {
        continue;
      }
      ids.add(
          ChemistryComputationParityPlanner.recommendationPlanId(workflowJobId, recommendation, i));
    }
    return List.copyOf(ids);
  }

  private static boolean appliesToCandidate(
      final ChemistryWorkflowRecommendation recommendation, final int candidateIndex) {
    return recommendation.appliesToCandidate()
        ? recommendation.candidateIndex() == candidateIndex
        : candidateIndex == 0;
  }

  private static List<String> classicalResultIds(
      final int candidateIndex,
      final List<ClassicalCalculationResult> classicalCalculationResults) {
    if (classicalCalculationResults == null || classicalCalculationResults.isEmpty()) {
      return List.of();
    }
    final ArrayList<String> ids = new ArrayList<String>(classicalCalculationResults.size());
    boolean hasCandidateMarkers = false;
    for (int i = 0; i < classicalCalculationResults.size(); ++i) {
      final ClassicalCalculationResult result = classicalCalculationResults.get(i);
      if (result == null) {
        continue;
      }
      final String resultId = result.calculationId().value();
      final int resultCandidateIndex =
          ChemistryComputationParityPlanner.candidateMarkerIndex(resultId);
      if (resultCandidateIndex >= 0) {
        hasCandidateMarkers = true;
      }
      if (resultCandidateIndex == candidateIndex) {
        ids.add(resultId);
      }
    }
    if (hasCandidateMarkers) {
      return List.copyOf(ids);
    }
    if (candidateIndex == 0 && classicalCalculationResults.size() == 1) {
      return List.of(classicalCalculationResults.get(0).calculationId().value());
    }
    return List.of();
  }

  private static List<String> quantumProgramIds(
      final int candidateIndex,
      final ChemistryQuantumProgramPackage quantumProgramPackage,
      final List<ClassicalCalculationResult> classicalCalculationResults) {
    final LinkedHashSet<String> ids = new LinkedHashSet<String>();
    ids.addAll(
        ChemistryComputationParityPlanner.quantumProgramIds(candidateIndex, quantumProgramPackage));
    ids.addAll(
        ChemistryComputationParityPlanner.followUpQuantumProgramIds(
            candidateIndex, classicalCalculationResults));
    return List.copyOf(ids);
  }

  private static List<String> quantumProgramIds(
      final int candidateIndex, final ChemistryQuantumProgramPackage quantumProgramPackage) {
    if (quantumProgramPackage == null || !quantumProgramPackage.hasArtifacts()) {
      return List.of();
    }
    final List<ChemistryQuantumProgramArtifact> artifacts = quantumProgramPackage.artifacts();
    final ArrayList<String> matchedIds = new ArrayList<String>(artifacts.size());
    boolean hasCandidateMarkers = false;
    for (int i = 0; i < artifacts.size(); ++i) {
      final ChemistryQuantumProgramArtifact artifact = artifacts.get(i);
      if (ChemistryComputationParityPlanner.hasCandidateMarker(artifact)) {
        hasCandidateMarkers = true;
      }
      if (ChemistryComputationParityPlanner.matchesCandidate(artifact, candidateIndex)) {
        matchedIds.add(artifact.id());
      }
    }
    if (hasCandidateMarkers) {
      return List.copyOf(matchedIds);
    }
    if (candidateIndex == 0) {
      return quantumProgramPackage.artifactIds();
    }
    return List.of();
  }

  private static List<String> followUpQuantumProgramIds(
      final int candidateIndex,
      final List<ClassicalCalculationResult> classicalCalculationResults) {
    if (classicalCalculationResults == null || classicalCalculationResults.isEmpty()) {
      return List.of();
    }
    final LinkedHashSet<String> ids = new LinkedHashSet<String>();
    for (int i = 0; i < classicalCalculationResults.size(); ++i) {
      final ClassicalCalculationResult result = classicalCalculationResults.get(i);
      if (result == null
          || ChemistryComputationParityPlanner.candidateMarkerIndex(result.calculationId().value())
              != candidateIndex) {
        continue;
      }
      final List<ClassicalResultSection> sections =
          result.sections(ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION);
      for (int sectionIndex = 0; sectionIndex < sections.size(); ++sectionIndex) {
        ChemistryComputationParityPlanner.addArtifactRows(ids, sections.get(sectionIndex));
      }
    }
    return List.copyOf(ids);
  }

  private static void addArtifactRows(
      final LinkedHashSet<String> ids, final ClassicalResultSection section) {
    final ClassicalResultValue rows = section.valueOrNull("artifacts");
    if (rows == null) {
      final ClassicalResultValue sourceId = section.valueOrNull("source_id");
      if (sourceId != null) {
        ids.add(sourceId.firstValue());
      }
      return;
    }
    final List<String> values = rows.values();
    for (int i = 0; i < values.size(); ++i) {
      final String row = values.get(i);
      if (row.startsWith("id\t") || row.isBlank()) {
        continue;
      }
      final int separator = row.indexOf('\t');
      ids.add(separator < 0 ? row : row.substring(0, separator));
    }
  }

  private static List<ChemistryComputationParityDiagnostic> diagnostics(
      final int candidateIndex,
      final ReactionPredictionResult predictionResult,
      final ReactionPredictionExplanation explanation,
      final List<String> mechanismIds,
      final int pathwayStepCount,
      final List<String> classicalPlanIds,
      final List<String> classicalResultIds,
      final List<ClassicalCalculationResult> classicalCalculationResults,
      final List<String> quantumProgramIds,
      final ChemistryQuantumProgramPackage quantumProgramPackage) {
    final ArrayList<ChemistryComputationParityDiagnostic> diagnostics =
        new ArrayList<ChemistryComputationParityDiagnostic>();
    ChemistryComputationParityPlanner.addPredictionReliabilityDiagnostics(
        predictionResult, explanation, diagnostics);
    if (mechanismIds.isEmpty()) {
      diagnostics.add(
          ChemistryComputationParityPlanner.diagnostic(
              ChemistryComputationParityDiagnosticCode.MISSING_MECHANISM,
              ChemistryComputationParityDiagnosticSeverity.WARNING,
              "Reaction candidate has no mechanism id, so parity cannot reference a concrete route."));
    }
    if (pathwayStepCount == 0) {
      diagnostics.add(
          ChemistryComputationParityPlanner.diagnostic(
              ChemistryComputationParityDiagnosticCode.MISSING_PATHWAY_STEPS,
              ChemistryComputationParityDiagnosticSeverity.INFO,
              "Reaction candidate has no detailed pathway steps recorded for step-by-step replay."));
    }
    if (classicalPlanIds.isEmpty()) {
      diagnostics.add(
          ChemistryComputationParityPlanner.diagnostic(
              ChemistryComputationParityDiagnosticCode.MISSING_CLASSICAL_PLAN,
              ChemistryComputationParityDiagnosticSeverity.WARNING,
              "No classical follow-up calculation is linked to this candidate."));
    }
    ChemistryComputationParityPlanner.addClassicalResultDiagnostics(
        classicalResultIds, classicalCalculationResults, diagnostics);
    ChemistryComputationParityPlanner.addQuantumDiagnostics(
        candidateIndex, quantumProgramIds, quantumProgramPackage, diagnostics);
    return List.copyOf(diagnostics);
  }

  private static void addPredictionReliabilityDiagnostics(
      final ReactionPredictionResult predictionResult,
      final ReactionPredictionExplanation explanation,
      final List<ChemistryComputationParityDiagnostic> diagnostics) {
    if (explanation != null
        && ChemistryComputationParityPlanner.lowConfidence(explanation.confidenceLevel())) {
      diagnostics.add(
          ChemistryComputationParityPlanner.diagnostic(
              ChemistryComputationParityDiagnosticCode.REACTION_PREDICTION_LOW_CONFIDENCE,
              ChemistryComputationParityDiagnosticSeverity.ERROR,
              "Reaction prediction confidence is "
                  + explanation.confidenceLevel()
                  + "; linked classical/quantum computations should not be treated as a reliable "
                  + "decision route without more evidence."));
    }
    final double missingProbabilityMass = predictionResult.missingProbabilityMass();
    if (missingProbabilityMass <= 0.0) {
      return;
    }
    diagnostics.add(
        ChemistryComputationParityPlanner.diagnostic(
            ChemistryComputationParityDiagnosticCode
                .REACTION_PREDICTION_INCOMPLETE_PROBABILITY_MASS,
            ChemistryComputationParityDiagnosticSeverity.WARNING,
            "Reaction prediction leaves "
                + missingProbabilityMass
                + " probability mass unassigned to explicit product candidates."));
  }

  private static boolean lowConfidence(final ReactionPredictionConfidenceLevel confidenceLevel) {
    return confidenceLevel == ReactionPredictionConfidenceLevel.NONE
        || confidenceLevel == ReactionPredictionConfidenceLevel.LOW;
  }

  private static void addClassicalResultDiagnostics(
      final List<String> classicalResultIds,
      final List<ClassicalCalculationResult> classicalCalculationResults,
      final List<ChemistryComputationParityDiagnostic> diagnostics) {
    if (classicalResultIds.isEmpty()) {
      diagnostics.add(
          ChemistryComputationParityPlanner.diagnostic(
              ChemistryComputationParityDiagnosticCode.MISSING_CLASSICAL_RESULT,
              ChemistryComputationParityDiagnosticSeverity.WARNING,
              "No executed classical result is linked to this candidate."));
      return;
    }
    if (classicalCalculationResults == null || classicalCalculationResults.isEmpty()) {
      return;
    }
    for (int i = 0; i < classicalCalculationResults.size(); ++i) {
      final ClassicalCalculationResult result = classicalCalculationResults.get(i);
      if (result == null || !classicalResultIds.contains(result.calculationId().value())) {
        continue;
      }
      if (result.status() == ClassicalCalculationStatus.SUCCEEDED
          || result.status() == ClassicalCalculationStatus.PARTIAL) {
        ChemistryComputationParityPlanner.addClassicalCoverageDiagnostics(result, diagnostics);
      } else {
        diagnostics.add(
            ChemistryComputationParityPlanner.diagnostic(
                ChemistryComputationParityDiagnosticCode.CLASSICAL_RESULT_NOT_SUCCEEDED,
                ChemistryComputationParityDiagnosticSeverity.ERROR,
                "Classical result "
                    + result.calculationId().value()
                    + " status is "
                    + result.status()
                    + ", so it cannot be treated as a completed parity reference."));
      }
    }
  }

  private static void addClassicalCoverageDiagnostics(
      final ClassicalCalculationResult classicalCalculationResult,
      final List<ChemistryComputationParityDiagnostic> diagnostics) {
    final List<ClassicalCalculationCoverageItem> items =
        classicalCalculationResult.coverageReport().items();
    for (int i = 0; i < items.size(); ++i) {
      final ClassicalCalculationCoverageItem item = items.get(i);
      if (item.status() == ClassicalCoverageStatus.EXACT) {
        continue;
      }
      diagnostics.add(
          ChemistryComputationParityPlanner.diagnostic(
              ChemistryComputationParityPlanner.coverageDiagnosticCode(item.status()),
              ChemistryComputationParityPlanner.coverageDiagnosticSeverity(item.status()),
              "Classical coverage for "
                  + item.kind()
                  + " is "
                  + item.status()
                  + " through model "
                  + item.model()
                  + "."));
    }
  }

  private static ChemistryComputationParityDiagnosticCode coverageDiagnosticCode(
      final ClassicalCoverageStatus status) {
    if (status == ClassicalCoverageStatus.UNSUPPORTED) {
      return ChemistryComputationParityDiagnosticCode.CLASSICAL_COVERAGE_UNSUPPORTED;
    }
    return ChemistryComputationParityDiagnosticCode.CLASSICAL_COVERAGE_APPROXIMATE;
  }

  private static ChemistryComputationParityDiagnosticSeverity coverageDiagnosticSeverity(
      final ClassicalCoverageStatus status) {
    if (status == ClassicalCoverageStatus.UNSUPPORTED) {
      return ChemistryComputationParityDiagnosticSeverity.ERROR;
    }
    return ChemistryComputationParityDiagnosticSeverity.WARNING;
  }

  private static void addQuantumDiagnostics(
      final int candidateIndex,
      final List<String> quantumProgramIds,
      final ChemistryQuantumProgramPackage quantumProgramPackage,
      final List<ChemistryComputationParityDiagnostic> diagnostics) {
    if (quantumProgramIds.isEmpty()) {
      diagnostics.add(
          ChemistryComputationParityPlanner.diagnostic(
              ChemistryComputationParityDiagnosticCode.MISSING_QUANTUM_PROGRAMS,
              ChemistryComputationParityDiagnosticSeverity.WARNING,
              "No Quantum IR chemistry program is linked to this candidate."));
      if (quantumProgramPackage != null && !quantumProgramPackage.skippedArtifacts().isEmpty()) {
        ChemistryComputationParityPlanner.addQuantumSkipDiagnostics(
            candidateIndex, quantumProgramIds, quantumProgramPackage, diagnostics);
      }
      return;
    }
    if (quantumProgramPackage == null || quantumProgramPackage.empty()) {
      return;
    }
    ChemistryComputationParityPlanner.addQuantumPackageMetadataDiagnostics(
        quantumProgramPackage, diagnostics);
    ChemistryComputationParityPlanner.addQuantumArtifactDiagnostics(
        quantumProgramIds, quantumProgramPackage, diagnostics);
    ChemistryComputationParityPlanner.addQuantumSkipDiagnostics(
        candidateIndex, quantumProgramIds, quantumProgramPackage, diagnostics);
  }

  private static void addQuantumPackageMetadataDiagnostics(
      final ChemistryQuantumProgramPackage quantumProgramPackage,
      final List<ChemistryComputationParityDiagnostic> diagnostics) {
    if (quantumProgramPackage.spinOrbitalCount() > 0
        && quantumProgramPackage.electronCount() >= 0) {
      return;
    }
    diagnostics.add(
        ChemistryComputationParityPlanner.diagnostic(
            ChemistryComputationParityDiagnosticCode.QUANTUM_PACKAGE_METADATA_INCOMPLETE,
            ChemistryComputationParityDiagnosticSeverity.ERROR,
            "Quantum package does not contain consistent spin orbital and electron counts."));
  }

  private static void addQuantumArtifactDiagnostics(
      final List<String> quantumProgramIds,
      final ChemistryQuantumProgramPackage quantumProgramPackage,
      final List<ChemistryComputationParityDiagnostic> diagnostics) {
    if (!quantumProgramPackage.hasArtifacts()) {
      diagnostics.add(
          ChemistryComputationParityPlanner.diagnostic(
              ChemistryComputationParityDiagnosticCode.MISSING_QUANTUM_PROGRAMS,
              ChemistryComputationParityDiagnosticSeverity.WARNING,
              "Quantum package exists but contains no executable Quantum IR artifacts."));
      return;
    }
    final List<ChemistryQuantumProgramArtifact> artifacts = quantumProgramPackage.artifacts();
    for (int i = 0; i < artifacts.size(); ++i) {
      final ChemistryQuantumProgramArtifact artifact = artifacts.get(i);
      if (!quantumProgramIds.contains(artifact.id())) {
        continue;
      }
      if (ChemistryComputationParityPlanner.mappingRequired(artifact.kind())
          && !artifact.hasMapping()) {
        diagnostics.add(
            ChemistryComputationParityPlanner.diagnostic(
                ChemistryComputationParityDiagnosticCode.QUANTUM_PROGRAM_WITHOUT_MAPPING,
                ChemistryComputationParityDiagnosticSeverity.ERROR,
                "Quantum artifact " + artifact.id() + " has no fermion-to-qubit mapping."));
      }
      if (artifact.circuitCount() <= 0 || artifact.operationCount() <= 0L) {
        diagnostics.add(
            ChemistryComputationParityPlanner.diagnostic(
                ChemistryComputationParityDiagnosticCode.QUANTUM_PROGRAM_EMPTY,
                ChemistryComputationParityDiagnosticSeverity.ERROR,
                "Quantum artifact "
                    + artifact.id()
                    + " does not contain executable circuit operations."));
      }
      if (artifact.kind() == ChemistryQuantumProgramKind.ADAPT_VQE_ANSATZ) {
        diagnostics.add(
            ChemistryComputationParityPlanner.diagnostic(
                ChemistryComputationParityDiagnosticCode.ADAPT_VQE_SELECTION_NOT_EXECUTED,
                ChemistryComputationParityDiagnosticSeverity.INFO,
                "Quantum artifact "
                    + artifact.id()
                    + " is an ADAPT-VQE operator-pool program; selected-operator ADAPT-VQE result "
                    + "requires an explicit energy backend run."));
      }
    }
  }

  private static boolean mappingRequired(final ChemistryQuantumProgramKind kind) {
    return kind == ChemistryQuantumProgramKind.PAULI_EXPECTATION_MEASUREMENT
        || kind == ChemistryQuantumProgramKind.PAULI_EVOLUTION
        || kind == ChemistryQuantumProgramKind.VERIFIED_PAULI_EVOLUTION
        || kind == ChemistryQuantumProgramKind.SPIN_BLOCK_PARITY_TAPERED_EVOLUTION
        || kind == ChemistryQuantumProgramKind.QUANTUM_PHASE_ESTIMATION;
  }

  private static void addQuantumSkipDiagnostics(
      final int candidateIndex,
      final List<String> quantumProgramIds,
      final ChemistryQuantumProgramPackage quantumProgramPackage,
      final List<ChemistryComputationParityDiagnostic> diagnostics) {
    final List<ChemistryQuantumProgramSkip> skips = quantumProgramPackage.skippedArtifacts();
    final boolean hasCandidateMarkers =
        ChemistryComputationParityPlanner.skipHasCandidateMarkers(skips);
    for (int i = 0; i < skips.size(); ++i) {
      final ChemistryQuantumProgramSkip skip = skips.get(i);
      if (hasCandidateMarkers
          && ChemistryComputationParityPlanner.candidateMarkerIndex(skip.id()) != candidateIndex) {
        continue;
      }
      if (!hasCandidateMarkers && candidateIndex != 0) {
        continue;
      }
      diagnostics.add(
          ChemistryComputationParityPlanner.diagnostic(
              ChemistryComputationParityDiagnosticCode.QUANTUM_PROGRAM_SKIPPED,
              ChemistryComputationParityDiagnosticSeverity.WARNING,
              "Quantum artifact " + skip.kind() + " was skipped: " + skip.reason()));
    }
  }

  private static ChemistryComputationParityStatus status(
      final List<String> classicalPlanIds,
      final List<String> classicalResultIds,
      final List<String> quantumProgramIds) {
    final boolean hasClassical = !classicalPlanIds.isEmpty() || !classicalResultIds.isEmpty();
    final boolean hasQuantum = !quantumProgramIds.isEmpty();
    if (hasClassical && hasQuantum) {
      return ChemistryComputationParityStatus.READY;
    }
    if (hasClassical) {
      return ChemistryComputationParityStatus.CLASSICAL_ONLY;
    }
    if (hasQuantum) {
      return ChemistryComputationParityStatus.QUANTUM_ONLY;
    }
    return ChemistryComputationParityStatus.MISSING_BOTH;
  }

  private static String explanation(
      final int candidateIndex,
      final ReactionProductCandidate candidate,
      final List<ChemistryComputationParityDiagnostic> diagnostics) {
    return "Reaction candidate "
        + candidateIndex
        + " has probability "
        + candidate.probability()
        + ", linked computation diagnostics="
        + diagnostics.size()
        + ".";
  }

  private static ChemistryComputationParityDiagnostic diagnostic(
      final ChemistryComputationParityDiagnosticCode code,
      final ChemistryComputationParityDiagnosticSeverity severity,
      final String message) {
    return ChemistryComputationParityDiagnostic.of(code, severity, message);
  }

  private static boolean hasCandidateMarker(final ChemistryQuantumProgramArtifact artifact) {
    return ChemistryComputationParityPlanner.candidateMarkerIndex(artifact.id()) >= 0
        || ChemistryComputationParityPlanner.candidateMarkerIndex(artifact.description()) >= 0;
  }

  private static boolean skipHasCandidateMarkers(final List<ChemistryQuantumProgramSkip> skips) {
    for (int i = 0; i < skips.size(); ++i) {
      if (ChemistryComputationParityPlanner.candidateMarkerIndex(skips.get(i).id()) >= 0) {
        return true;
      }
    }
    return false;
  }

  private static boolean matchesCandidate(
      final ChemistryQuantumProgramArtifact artifact, final int candidateIndex) {
    final int idIndex = ChemistryComputationParityPlanner.candidateMarkerIndex(artifact.id());
    if (idIndex >= 0) {
      return idIndex == candidateIndex;
    }
    final int descriptionIndex =
        ChemistryComputationParityPlanner.candidateMarkerIndex(artifact.description());
    return descriptionIndex == candidateIndex;
  }

  private static int candidateMarkerIndex(final String value) {
    if (value == null || value.isBlank()) {
      return -1;
    }
    final String normalized = value.toLowerCase(Locale.ROOT);
    int index = ChemistryComputationParityPlanner.markerIndex(normalized, "candidate_");
    if (index >= 0) {
      return index;
    }
    index = ChemistryComputationParityPlanner.markerIndex(normalized, "candidate/");
    if (index >= 0) {
      return index;
    }
    return ChemistryComputationParityPlanner.markerIndex(normalized, "candidate ");
  }

  private static int markerIndex(final String value, final String marker) {
    final int markerPosition = value.indexOf(marker);
    if (markerPosition < 0) {
      return -1;
    }
    final int numberStart = markerPosition + marker.length();
    int numberEnd = numberStart;
    while (numberEnd < value.length() && Character.isDigit(value.charAt(numberEnd))) {
      ++numberEnd;
    }
    if (numberEnd == numberStart) {
      return -1;
    }
    return Integer.parseInt(value.substring(numberStart, numberEnd));
  }

  private static String safeId(final String value) {
    final StringBuilder builder = new StringBuilder(value.length());
    boolean previousUnderscore = false;
    for (int i = 0; i < value.length(); ++i) {
      final char symbol = Character.toLowerCase(value.charAt(i));
      final boolean identifierSymbol =
          (symbol >= 'a' && symbol <= 'z') || (symbol >= '0' && symbol <= '9') || symbol == '_';
      if (identifierSymbol) {
        builder.append(symbol);
        previousUnderscore = false;
      } else if (!previousUnderscore) {
        builder.append('_');
        previousUnderscore = true;
      }
    }
    if (builder.length() == 0 || !Character.isLetter(builder.charAt(0))) {
      builder.insert(0, "id_");
    }
    final int lastIndex = builder.length() - 1;
    if (builder.charAt(lastIndex) == '_') {
      builder.deleteCharAt(lastIndex);
    }
    return builder.toString();
  }

  private static String recommendationPlanId(
      final String workflowJobId,
      final ChemistryWorkflowRecommendation recommendation,
      final int recommendationIndex) {
    final String candidateMarker =
        recommendation.appliesToCandidate()
            ? "candidate_" + recommendation.candidateIndex()
            : "program";
    return ChemistryComputationParityPlanner.safeId(
        workflowJobId
            + "_classical_"
            + candidateMarker
            + "_"
            + recommendationIndex
            + "_"
            + recommendation.calculationKind().name().toLowerCase(Locale.ROOT));
  }
}

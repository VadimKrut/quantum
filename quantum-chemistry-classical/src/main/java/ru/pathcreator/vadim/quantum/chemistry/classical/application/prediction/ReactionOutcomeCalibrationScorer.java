/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeCalibrationLibrary;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeCalibrationObservation;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeSourceQuality;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.PredictedReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeature;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;

/** Сопоставляет кандидаты с наблюдаемыми распределениями продуктов из calibration-библиотеки. */
public final class ReactionOutcomeCalibrationScorer {

  private final ReactionOutcomeCalibrationLibrary calibrationLibrary;

  public ReactionOutcomeCalibrationScorer(
      final ReactionOutcomeCalibrationLibrary calibrationLibrary) {
    this.calibrationLibrary =
        calibrationLibrary == null ? ReactionOutcomeCalibrationLibrary.EMPTY : calibrationLibrary;
  }

  public void recordBenchmarkEvidence(
      final ReactionPredictionRequest request,
      final ReactionProductCandidate candidate,
      final List<ReactionPredictionEvidence> evidence) {
    this.recordBenchmarkAgreement(request, candidate, evidence);
  }

  public double recordBenchmarkAgreement(
      final ReactionPredictionRequest request,
      final ReactionProductCandidate candidate,
      final List<ReactionPredictionEvidence> evidence) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction prediction request must not be null.");
    }
    if (candidate == null) {
      throw new IllegalArgumentException("Reaction product candidate must not be null.");
    }
    if (evidence == null) {
      throw new IllegalArgumentException("Reaction prediction evidence list must not be null.");
    }
    if (this.calibrationLibrary.empty()) {
      return 0.0;
    }
    final String signature = ReactionOutcomeCalibrationScorer.productFormulaSignature(candidate);
    final List<ReactionOutcomeCalibrationObservation> observations =
        this.calibrationLibrary.matchingDomains(request.domains());
    final List<ReactionConditionFeatureKind> requestConditionFeatures =
        ReactionOutcomeCalibrationScorer.conditionFeatureKinds(request);
    for (int i = 0; i < observations.size(); ++i) {
      final ReactionOutcomeCalibrationObservation observation = observations.get(i);
      if (observation.sourceIdentifier().startsWith("internal:organic-taxonomy-")) {
        // A generic taxonomy heuristic is not an observed product distribution.
        continue;
      }
      final boolean variableFormula =
          ReactionOutcomeCalibrationScorer.variableFormulaSignature(observation);
      if (!ReactionOutcomeCalibrationScorer.matchesFormulaSignature(
          observation, signature, variableFormula)) {
        continue;
      }
      if (!ReactionOutcomeCalibrationScorer.conditionFeaturesTransferable(
          observation, requestConditionFeatures)) {
        continue;
      }
      if (!ReactionOutcomeCalibrationScorer.matchesSubstrateHints(observation, request)) {
        continue;
      }
      if (!ReactionOutcomeCalibrationScorer.matchesMechanism(observation, candidate)) {
        continue;
      }
      if (!ReactionOutcomeCalibrationScorer.matchesChannelHints(observation, candidate)) {
        continue;
      }
      final String matchedSubstrateHints =
          ReactionOutcomeCalibrationScorer.matchedSubstrateHints(observation, request);
      final String matchedChannelHints =
          ReactionOutcomeCalibrationScorer.matchedChannelHints(observation, candidate);
      final double substrateHintCompleteness =
          ReactionOutcomeCalibrationScorer.hintCompleteness(
              observation.substrateClassHints(), matchedSubstrateHints);
      final double channelHintCompleteness =
          ReactionOutcomeCalibrationScorer.hintCompleteness(
              observation.productChannelHints(), matchedChannelHints);
      if (variableFormula
          && !ReactionOutcomeCalibrationScorer.transferableVariableFormulaMatch(
              observation, channelHintCompleteness)) {
        continue;
      }
      final double conditionCompleteness =
          ReactionOutcomeCalibrationScorer.conditionCompleteness(
              observation.conditionFeatures(), requestConditionFeatures);
      final double conditionConflictMultiplier =
          ReactionOutcomeCalibrationScorer.conditionConflictMultiplier(
              observation.conditionFeatures(), requestConditionFeatures);
      final double transferability =
          ReactionOutcomeCalibrationScorer.transferabilityMultiplier(
              observation.sourceQuality(),
              substrateHintCompleteness,
              channelHintCompleteness,
              conditionCompleteness,
              conditionConflictMultiplier);
      final double auditSimilarityScore =
          Math.min(
              0.55,
              observation.observedProbability()
                      * 0.35
                      * observation.confidence()
                      * ReactionOutcomeCalibrationScorer.sourceQualityMultiplier(
                          observation.sourceQuality())
                      * ReactionOutcomeCalibrationScorer.sourceTraceabilityMultiplier(observation)
                      * transferability
                  + observation.observedYieldPercent() / 1000.0 * transferability);
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.CALIBRATION_PRIOR,
              "Benchmark observation matched without changing prediction probability: "
                  + observation.id()
                  + " channel="
                  + observation.channelLabel()
                  + " regio="
                  + observation.regioselectivityClass()
                  + " quality="
                  + observation.sourceQuality()
                  + " sourceIdentifier="
                  + observation.sourceIdentifier()
                  + " tableReference="
                  + observation.tableReference()
                  + " exactProductDistributionRow="
                  + observation.exactProductDistributionRow()
                  + " observedProbability="
                  + observation.observedProbability()
                  + " observedYieldPercent="
                  + observation.observedYieldPercent()
                  + " substrateHints="
                  + matchedSubstrateHints
                  + " channelHints="
                  + matchedChannelHints
                  + " conditionCompleteness="
                  + conditionCompleteness
                  + " conditionConflictMultiplier="
                  + conditionConflictMultiplier
                  + " substrateCompleteness="
                  + substrateHintCompleteness
                  + " channelCompleteness="
                  + channelHintCompleteness
                  + " transferability="
                  + transferability
                  + " auditSimilarityScore="
                  + auditSimilarityScore
                  + " from "
                  + observation.source()
                  + ". "
                  + observation.note(),
              0.0));
    }
    return 0.0;
  }

  public double calibrationPriorWeight(
      final ReactionPredictionRequest request,
      final ReactionProductCandidate candidate,
      final List<ReactionPredictionEvidence> evidence) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction prediction request must not be null.");
    }
    if (candidate == null) {
      throw new IllegalArgumentException("Reaction product candidate must not be null.");
    }
    if (evidence == null) {
      throw new IllegalArgumentException("Reaction prediction evidence list must not be null.");
    }
    if (this.calibrationLibrary.empty()) {
      return 0.0;
    }
    double logWeight = 0.0;
    final String signature = ReactionOutcomeCalibrationScorer.productFormulaSignature(candidate);
    final List<ReactionOutcomeCalibrationObservation> observations =
        this.calibrationLibrary.matchingDomains(request.domains());
    final List<ReactionConditionFeatureKind> requestConditionFeatures =
        ReactionOutcomeCalibrationScorer.conditionFeatureKinds(request);
    for (int i = 0; i < observations.size(); ++i) {
      final ReactionOutcomeCalibrationObservation observation = observations.get(i);
      final double contribution =
          ReactionOutcomeCalibrationScorer.priorContribution(
              observation, request, candidate, signature, requestConditionFeatures);
      if (contribution <= 0.0) {
        continue;
      }
      logWeight += contribution;
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.CALIBRATION_PRIOR,
              "Calibration prior softly reweighted candidate from observation "
                  + observation.id()
                  + " channel="
                  + observation.channelLabel()
                  + " probability="
                  + observation.observedProbability()
                  + " yield="
                  + observation.observedYieldPercent()
                  + " quality="
                  + observation.sourceQuality()
                  + " contribution="
                  + contribution
                  + ".",
              Math.min(0.35, contribution)));
    }
    return Math.max(-0.45, Math.min(0.45, logWeight));
  }

  @Deprecated
  public double score(
      final ReactionPredictionRequest request,
      final ReactionProductCandidate candidate,
      final List<ReactionPredictionEvidence> evidence) {
    return this.recordBenchmarkAgreement(request, candidate, evidence);
  }

  private static double priorContribution(
      final ReactionOutcomeCalibrationObservation observation,
      final ReactionPredictionRequest request,
      final ReactionProductCandidate candidate,
      final String signature,
      final List<ReactionConditionFeatureKind> requestConditionFeatures) {
    final boolean variableFormula =
        ReactionOutcomeCalibrationScorer.variableFormulaSignature(observation);
    if (!ReactionOutcomeCalibrationScorer.matchesFormulaSignature(
        observation, signature, variableFormula)) {
      return 0.0;
    }
    if (!ReactionOutcomeCalibrationScorer.conditionFeaturesTransferable(
        observation, requestConditionFeatures)) {
      return 0.0;
    }
    if (!ReactionOutcomeCalibrationScorer.matchesSubstrateHints(observation, request)) {
      return 0.0;
    }
    if (!ReactionOutcomeCalibrationScorer.matchesMechanism(observation, candidate)) {
      return 0.0;
    }
    if (!ReactionOutcomeCalibrationScorer.matchesChannelHints(observation, candidate)) {
      return 0.0;
    }
    final String matchedSubstrateHints =
        ReactionOutcomeCalibrationScorer.matchedSubstrateHints(observation, request);
    final String matchedChannelHints =
        ReactionOutcomeCalibrationScorer.matchedChannelHints(observation, candidate);
    final double substrateHintCompleteness =
        ReactionOutcomeCalibrationScorer.hintCompleteness(
            observation.substrateClassHints(), matchedSubstrateHints);
    final double channelHintCompleteness =
        ReactionOutcomeCalibrationScorer.hintCompleteness(
            observation.productChannelHints(), matchedChannelHints);
    if (variableFormula
        && !ReactionOutcomeCalibrationScorer.transferableVariableFormulaMatch(
            observation, channelHintCompleteness)) {
      return 0.0;
    }
    final double conditionCompleteness =
        ReactionOutcomeCalibrationScorer.conditionCompleteness(
            observation.conditionFeatures(), requestConditionFeatures);
    final double conditionConflictMultiplier =
        ReactionOutcomeCalibrationScorer.conditionConflictMultiplier(
            observation.conditionFeatures(), requestConditionFeatures);
    final double transferability =
        ReactionOutcomeCalibrationScorer.transferabilityMultiplier(
            observation.sourceQuality(),
            substrateHintCompleteness,
            channelHintCompleteness,
            conditionCompleteness,
            conditionConflictMultiplier);
    final double quality =
        ReactionOutcomeCalibrationScorer.sourceQualityMultiplier(observation.sourceQuality())
            * ReactionOutcomeCalibrationScorer.sourceTraceabilityMultiplier(observation);
    final double centeredProbability = observation.observedProbability() - 0.50;
    final double yieldSignal = observation.observedYieldPercent() / 100.0 - 0.50;
    return Math.max(
        0.0,
        Math.min(
            0.25,
            (0.30 * centeredProbability + 0.12 * yieldSignal + 0.10)
                * observation.confidence()
                * quality
                * transferability));
  }

  private static boolean matchesFormulaSignature(
      final ReactionOutcomeCalibrationObservation observation,
      final String candidateSignature,
      final boolean variableFormula) {
    final String observationSignature = observation.productFormulaSignature();
    return observationSignature.equals(candidateSignature) || variableFormula;
  }

  private static boolean variableFormulaSignature(
      final ReactionOutcomeCalibrationObservation observation) {
    return observation.productFormulaSignature().endsWith("_variable_formula");
  }

  private static boolean transferableVariableFormulaMatch(
      final ReactionOutcomeCalibrationObservation observation,
      final double channelHintCompleteness) {
    if (!ReactionOutcomeCalibrationScorer.variableFormulaSignature(observation)) {
      return true;
    }
    if (observation.productChannelHints().isEmpty()) {
      return observation.sourceQuality()
          != ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR;
    }
    if (observation.sourceQuality()
            == ReactionOutcomeSourceQuality.EXACT_EXPERIMENTAL_PRODUCT_DISTRIBUTION
        || observation.sourceQuality() == ReactionOutcomeSourceQuality.EXPERIMENTAL_YIELD_TABLE) {
      return channelHintCompleteness >= 0.34;
    }
    return channelHintCompleteness >= 0.50;
  }

  private static boolean matchesMechanism(
      final ReactionOutcomeCalibrationObservation observation,
      final ReactionProductCandidate candidate) {
    if (ReactionOutcomeCalibrationScorer.broadPriorHints(observation.productChannelHints())) {
      return true;
    }
    if (observation.mechanismFamily() == ReactionMechanismFamily.UNKNOWN) {
      return true;
    }
    if (!candidate.hasPredictedMechanisms()) {
      return true;
    }
    final List<PredictedReactionMechanism> mechanisms = candidate.predictedMechanisms();
    for (int i = 0; i < mechanisms.size(); ++i) {
      if (mechanisms.get(i).family() == observation.mechanismFamily()) {
        return true;
      }
    }
    return false;
  }

  private static boolean matchesSubstrateHints(
      final ReactionOutcomeCalibrationObservation observation,
      final ReactionPredictionRequest request) {
    if (observation.substrateClassHints().isEmpty()) {
      return true;
    }
    final String text =
        ReactionOutcomeCalibrationScorer.normalizedHintText(
            ReactionOutcomeCalibrationScorer.requestText(request));
    for (int i = 0; i < observation.substrateClassHints().size(); ++i) {
      if (text.contains(
          ReactionOutcomeCalibrationScorer.normalizedHintText(
              observation.substrateClassHints().get(i)))) {
        return true;
      }
    }
    return false;
  }

  private static boolean matchesChannelHints(
      final ReactionOutcomeCalibrationObservation observation,
      final ReactionProductCandidate candidate) {
    if (observation.productChannelHints().isEmpty()) {
      return true;
    }
    if (ReactionOutcomeCalibrationScorer.broadPriorHints(observation.productChannelHints())) {
      return true;
    }
    final String text =
        ReactionOutcomeCalibrationScorer.normalizedHintText(
            ReactionOutcomeCalibrationScorer.candidateText(candidate));
    for (int i = 0; i < observation.productChannelHints().size(); ++i) {
      if (text.contains(
          ReactionOutcomeCalibrationScorer.normalizedHintText(
              observation.productChannelHints().get(i)))) {
        return true;
      }
    }
    return false;
  }

  private static boolean broadPriorHints(final List<String> hints) {
    for (int i = 0; i < hints.size(); ++i) {
      final String normalized = ReactionOutcomeCalibrationScorer.normalizedHintText(hints.get(i));
      if (!"broad domain prior".equals(normalized) && !"broad mechanism prior".equals(normalized)) {
        return false;
      }
    }
    return true;
  }

  private static String matchedSubstrateHints(
      final ReactionOutcomeCalibrationObservation observation,
      final ReactionPredictionRequest request) {
    return ReactionOutcomeCalibrationScorer.matchedHints(
        observation.substrateClassHints(), ReactionOutcomeCalibrationScorer.requestText(request));
  }

  private static String matchedChannelHints(
      final ReactionOutcomeCalibrationObservation observation,
      final ReactionProductCandidate candidate) {
    if (ReactionOutcomeCalibrationScorer.broadPriorHints(observation.productChannelHints())) {
      return String.join(",", observation.productChannelHints());
    }
    return ReactionOutcomeCalibrationScorer.matchedHints(
        observation.productChannelHints(),
        ReactionOutcomeCalibrationScorer.candidateText(candidate));
  }

  private static String matchedHints(final List<String> hints, final String text) {
    if (hints.isEmpty()) {
      return "none-required";
    }
    final String normalizedText = ReactionOutcomeCalibrationScorer.normalizedHintText(text);
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < hints.size(); ++i) {
      final String hint = hints.get(i);
      if (!normalizedText.contains(ReactionOutcomeCalibrationScorer.normalizedHintText(hint))) {
        continue;
      }
      if (builder.length() > 0) {
        builder.append(',');
      }
      builder.append(hint);
    }
    if (builder.length() == 0) {
      return "none";
    }
    return builder.toString();
  }

  private static String normalizedHintText(final String text) {
    return text.toLowerCase(java.util.Locale.ROOT).replace('_', ' ').replace('-', ' ');
  }

  private static double hintCompleteness(final List<String> hints, final String matchedHints) {
    if (hints.isEmpty() || matchedHints.equals("none-required")) {
      return 1.0;
    }
    if (matchedHints.equals("none")) {
      return 0.0;
    }
    int matchedCount = 1;
    for (int i = 0; i < matchedHints.length(); ++i) {
      if (matchedHints.charAt(i) == ',') {
        ++matchedCount;
      }
    }
    return Math.min(1.0, (double) matchedCount / (double) hints.size());
  }

  private static double conditionCompleteness(
      final List<ReactionConditionFeatureKind> observationFeatures,
      final List<ReactionConditionFeatureKind> requestFeatures) {
    if (observationFeatures.isEmpty()) {
      return 1.0;
    }
    int matchedCount = 0;
    for (int i = 0; i < observationFeatures.size(); ++i) {
      if (requestFeatures.contains(observationFeatures.get(i))) {
        ++matchedCount;
      }
    }
    return (double) matchedCount / (double) observationFeatures.size();
  }

  private static boolean conditionFeaturesTransferable(
      final ReactionOutcomeCalibrationObservation observation,
      final List<ReactionConditionFeatureKind> requestFeatures) {
    if (observation.conditionFeatures().isEmpty()) {
      return true;
    }
    if (requestFeatures == null || requestFeatures.isEmpty()) {
      return false;
    }
    if (observation.sourceQuality() != ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR) {
      return observation.matchesConditionFeatures(requestFeatures);
    }
    for (int i = 0; i < observation.conditionFeatures().size(); ++i) {
      if (requestFeatures.contains(observation.conditionFeatures().get(i))) {
        return true;
      }
    }
    return false;
  }

  private static double transferabilityMultiplier(
      final ReactionOutcomeSourceQuality quality,
      final double substrateHintCompleteness,
      final double channelHintCompleteness,
      final double conditionCompleteness,
      final double conditionConflictMultiplier) {
    final double structuralSpecificity =
        0.45 + 0.35 * substrateHintCompleteness + 0.20 * channelHintCompleteness;
    final double conditionSpecificity = 0.70 + 0.30 * conditionCompleteness;
    final double qualitySpecificity =
        quality == ReactionOutcomeSourceQuality.EXACT_EXPERIMENTAL_PRODUCT_DISTRIBUTION
                || quality == ReactionOutcomeSourceQuality.EXPERIMENTAL_YIELD_TABLE
                || quality == ReactionOutcomeSourceQuality.TIME_RESOLVED_POPULATION_STUDY
            ? 1.0
            : 0.82;
    return Math.max(
        0.18,
        Math.min(
            1.0,
            structuralSpecificity
                * conditionSpecificity
                * qualitySpecificity
                * conditionConflictMultiplier));
  }

  private static double sourceTraceabilityMultiplier(
      final ReactionOutcomeCalibrationObservation observation) {
    if (!ReactionOutcomeCalibrationScorer.exactSourceQuality(observation.sourceQuality())) {
      return 1.0;
    }
    if (!"unspecified".equals(observation.sourceIdentifier())
        && !"unspecified".equals(observation.tableReference())
        && observation.exactProductDistributionRow()) {
      return 1.0;
    }
    return 0.72;
  }

  private static boolean exactSourceQuality(final ReactionOutcomeSourceQuality quality) {
    return quality == ReactionOutcomeSourceQuality.EXACT_EXPERIMENTAL_PRODUCT_DISTRIBUTION
        || quality == ReactionOutcomeSourceQuality.EXPERIMENTAL_YIELD_TABLE
        || quality == ReactionOutcomeSourceQuality.TIME_RESOLVED_POPULATION_STUDY
        || quality == ReactionOutcomeSourceQuality.LOCAL_BENCHMARK
        || quality == ReactionOutcomeSourceQuality.USER_OBSERVATION;
  }

  private static double conditionConflictMultiplier(
      final List<ReactionConditionFeatureKind> observationFeatures,
      final List<ReactionConditionFeatureKind> requestFeatures) {
    double multiplier = 1.0;
    multiplier *=
        ReactionOutcomeCalibrationScorer.conflictMultiplier(
            observationFeatures,
            requestFeatures,
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.BASIC_MEDIUM);
    multiplier *=
        ReactionOutcomeCalibrationScorer.conflictMultiplier(
            observationFeatures,
            requestFeatures,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.COOLING);
    multiplier *=
        ReactionOutcomeCalibrationScorer.conflictMultiplier(
            observationFeatures,
            requestFeatures,
            ReactionConditionFeatureKind.PHOTOCHEMICAL,
            ReactionConditionFeatureKind.ELECTROCHEMICAL);
    return multiplier;
  }

  private static double conflictMultiplier(
      final List<ReactionConditionFeatureKind> observationFeatures,
      final List<ReactionConditionFeatureKind> requestFeatures,
      final ReactionConditionFeatureKind first,
      final ReactionConditionFeatureKind second) {
    if (observationFeatures.contains(first)
        && requestFeatures.contains(second)
        && !observationFeatures.contains(second)) {
      return 0.55;
    }
    if (observationFeatures.contains(second)
        && requestFeatures.contains(first)
        && !observationFeatures.contains(first)) {
      return 0.55;
    }
    return 1.0;
  }

  private static String candidateText(final ReactionProductCandidate candidate) {
    final StringBuilder builder = new StringBuilder();
    if (candidate.hasSelectivityNote()) {
      builder.append(candidate.selectivityNote());
      builder.append(' ');
    }
    final List<ReactionPredictionEvidence> evidence = candidate.evidence();
    for (int i = 0; i < evidence.size(); ++i) {
      builder.append(evidence.get(i).description());
      builder.append(' ');
    }
    final List<ReactionParticipant> participants = candidate.products().participants();
    for (int i = 0; i < participants.size(); ++i) {
      builder.append(participants.get(i).molecule().id().value());
      builder.append(' ');
      builder.append(participants.get(i).molecule().displayName());
      builder.append(' ');
      builder.append(participants.get(i).molecule().formula().hillNotation());
      builder.append(' ');
    }
    final List<PredictedReactionMechanism> mechanisms = candidate.predictedMechanisms();
    for (int i = 0; i < mechanisms.size(); ++i) {
      builder.append(mechanisms.get(i).family().name());
      builder.append(' ');
    }
    return builder.toString().toLowerCase(java.util.Locale.ROOT);
  }

  private static String requestText(final ReactionPredictionRequest request) {
    final StringBuilder builder = new StringBuilder(request.id());
    builder.append(' ');
    for (int i = 0; i < request.domains().size(); ++i) {
      builder.append(request.domains().get(i).name());
      builder.append(' ');
    }
    final List<ReactionParticipant> participants = request.reactants().participants();
    for (int i = 0; i < participants.size(); ++i) {
      builder.append(participants.get(i).molecule().id().value());
      builder.append(' ');
      builder.append(participants.get(i).molecule().displayName());
      builder.append(' ');
      builder.append(participants.get(i).molecule().formula().hillNotation());
      builder.append(' ');
    }
    final List<ReactionConditionComponent> components = request.conditions().components();
    for (int i = 0; i < components.size(); ++i) {
      builder.append(components.get(i).role().name());
      builder.append(' ');
      builder.append(components.get(i).name());
      builder.append(' ');
    }
    final List<ReactionConditionFeature> features = request.conditions().features();
    for (int i = 0; i < features.size(); ++i) {
      builder.append(features.get(i).kind().name());
      builder.append(' ');
    }
    if (request.conditions().note() != null) {
      builder.append(request.conditions().note());
    }
    return builder.toString().toLowerCase(java.util.Locale.ROOT);
  }

  private static double sourceQualityMultiplier(final ReactionOutcomeSourceQuality quality) {
    if (quality == ReactionOutcomeSourceQuality.EXACT_EXPERIMENTAL_PRODUCT_DISTRIBUTION) {
      return 1.25;
    }
    if (quality == ReactionOutcomeSourceQuality.TIME_RESOLVED_POPULATION_STUDY) {
      return 1.15;
    }
    if (quality == ReactionOutcomeSourceQuality.EXPERIMENTAL_YIELD_TABLE) {
      return 1.10;
    }
    if (quality == ReactionOutcomeSourceQuality.LOCAL_BENCHMARK) {
      return 1.00;
    }
    if (quality == ReactionOutcomeSourceQuality.USER_OBSERVATION) {
      return 0.90;
    }
    if (quality == ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR) {
      return 0.70;
    }
    return 0.60;
  }

  private static List<ReactionConditionFeatureKind> conditionFeatureKinds(
      final ReactionPredictionRequest request) {
    final List<ReactionConditionFeature> features = request.conditions().features();
    final ArrayList<ReactionConditionFeatureKind> kinds =
        new ArrayList<ReactionConditionFeatureKind>(features.size());
    for (int i = 0; i < features.size(); ++i) {
      kinds.add(features.get(i).kind());
    }
    return List.copyOf(kinds);
  }

  public static String productFormulaSignature(final ReactionProductCandidate candidate) {
    if (candidate == null) {
      throw new IllegalArgumentException("Reaction product candidate must not be null.");
    }
    final List<ReactionParticipant> participants = candidate.products().participants();
    final ArrayList<String> formulas = new ArrayList<String>(participants.size());
    for (int i = 0; i < participants.size(); ++i) {
      formulas.add(participants.get(i).molecule().formula().hillNotation());
    }
    Collections.sort(formulas);
    return String.join(" + ", formulas);
  }
}

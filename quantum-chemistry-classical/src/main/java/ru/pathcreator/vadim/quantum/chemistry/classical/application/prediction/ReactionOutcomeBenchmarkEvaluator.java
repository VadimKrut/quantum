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
import java.util.List;
import java.util.Locale;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeBenchmarkMatch;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeBenchmarkReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeCalibrationLibrary;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeCalibrationObservation;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;

/** РЎС‡РёС‚Р°РµС‚ РѕС€РёР±РєСѓ РіРѕС‚РѕРІРѕРіРѕ РїСЂРѕРіРЅРѕР·Р° РїРѕ РЅРµР·Р°РІРёСЃРёРјС‹Рј exact benchmark-СЃС‚СЂРѕРєР°Рј. */
public final class ReactionOutcomeBenchmarkEvaluator {

  public ReactionOutcomeBenchmarkReport evaluate(
      final ReactionPredictionResult prediction,
      final ReactionOutcomeCalibrationLibrary benchmarks) {
    if (prediction == null) {
      throw new IllegalArgumentException("Reaction prediction result must not be null.");
    }
    if (benchmarks == null) {
      throw new IllegalArgumentException("Reaction outcome benchmark library must not be null.");
    }
    final List<ReactionOutcomeCalibrationObservation> observations =
        benchmarks.matchingDomains(prediction.request().domains());
    final ArrayList<ReactionOutcomeBenchmarkMatch> matches =
        new ArrayList<ReactionOutcomeBenchmarkMatch>();
    int exactCount = 0;
    for (int i = 0; i < observations.size(); ++i) {
      final ReactionOutcomeCalibrationObservation observation = observations.get(i);
      if (!observation.exactProductDistributionRow()) {
        continue;
      }
      ++exactCount;
      matches.add(this.match(observation, prediction.candidates()));
    }
    return ReactionOutcomeBenchmarkReport.of(matches, exactCount);
  }

  private ReactionOutcomeBenchmarkMatch match(
      final ReactionOutcomeCalibrationObservation observation,
      final List<ReactionProductCandidate> candidates) {
    for (int i = 0; i < candidates.size(); ++i) {
      final ReactionProductCandidate candidate = candidates.get(i);
      if (!ReactionOutcomeBenchmarkEvaluator.matchesFormula(observation, candidate)) {
        continue;
      }
      if (!ReactionOutcomeBenchmarkEvaluator.matchesChannelHints(observation, candidate)) {
        continue;
      }
      return ReactionOutcomeBenchmarkMatch.matched(
          observation.id(),
          observation.channelLabel(),
          observation.observedProbability(),
          candidate.probability(),
          "Candidate formula and channel hints match benchmark observation.");
    }
    return ReactionOutcomeBenchmarkMatch.unmatched(
        observation.id(),
        observation.channelLabel(),
        observation.observedProbability(),
        "No prediction candidate has both the benchmark formula signature and channel hints.");
  }

  private static boolean matchesFormula(
      final ReactionOutcomeCalibrationObservation observation,
      final ReactionProductCandidate candidate) {
    final String signature = observation.productFormulaSignature();
    if (signature.endsWith("_variable_formula")) {
      return true;
    }
    return signature.equals(ReactionOutcomeCalibrationScorer.productFormulaSignature(candidate));
  }

  private static boolean matchesChannelHints(
      final ReactionOutcomeCalibrationObservation observation,
      final ReactionProductCandidate candidate) {
    if (observation.productChannelHints().isEmpty()) {
      return true;
    }
    final String text = ReactionOutcomeBenchmarkEvaluator.candidateText(candidate);
    for (int i = 0; i < observation.productChannelHints().size(); ++i) {
      if (text.contains(
          ReactionOutcomeBenchmarkEvaluator.normalized(observation.productChannelHints().get(i)))) {
        return true;
      }
    }
    return false;
  }

  private static String candidateText(final ReactionProductCandidate candidate) {
    final StringBuilder builder = new StringBuilder();
    if (candidate.hasSelectivityNote()) {
      builder.append(candidate.selectivityNote());
    }
    final List<ReactionPredictionEvidence> evidence = candidate.evidence();
    for (int i = 0; i < evidence.size(); ++i) {
      builder.append(' ');
      builder.append(evidence.get(i).description());
    }
    return ReactionOutcomeBenchmarkEvaluator.normalized(builder.toString());
  }

  private static String normalized(final String text) {
    if (text == null) {
      return "";
    }
    return text.toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
  }
}

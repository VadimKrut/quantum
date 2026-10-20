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
import java.util.EnumMap;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeCalibrationCoverageReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeCalibrationLibrary;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeCalibrationObservation;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeDistributionDataset;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeSourceQuality;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;

/** Проверяет, насколько калибровочные данные пригодны для вероятностного прогноза продуктов. */
public final class ReactionOutcomeCalibrationCoverageAnalyzer {

  private static final double COMPLETE_DISTRIBUTION_TOLERANCE = 0.08;
  private static final int MIN_EXACT_ROWS_PER_STRONG_DATASET = 2;

  public ReactionOutcomeCalibrationCoverageReport analyze(
      final ReactionOutcomeCalibrationLibrary library) {
    if (library == null) {
      throw new IllegalArgumentException("Reaction outcome calibration library must not be null.");
    }
    final EnumMap<OrganicReactionDomain, Integer> domainCounts =
        new EnumMap<OrganicReactionDomain, Integer>(OrganicReactionDomain.class);
    final EnumMap<ReactionMechanismFamily, Integer> mechanismCounts =
        new EnumMap<ReactionMechanismFamily, Integer>(ReactionMechanismFamily.class);
    int exactObservationCount = 0;
    int externallyVerifiedExactObservationCount = 0;
    int classPriorObservationCount = 0;
    int sourceIdentifiedExactObservationCount = 0;
    final ArrayList<String> unreferencedExactObservationIds = new ArrayList<String>();
    final ArrayList<String> sourceTracedPriorObservationIds = new ArrayList<String>();
    final List<ReactionOutcomeCalibrationObservation> observations = library.observations();
    for (int i = 0; i < observations.size(); ++i) {
      final ReactionOutcomeCalibrationObservation observation = observations.get(i);
      ReactionOutcomeCalibrationCoverageAnalyzer.addDomains(domainCounts, observation.domains());
      if (observation.mechanismFamily() != ReactionMechanismFamily.UNKNOWN) {
        mechanismCounts.merge(observation.mechanismFamily(), 1, Integer::sum);
      }
      if (ReactionOutcomeCalibrationCoverageAnalyzer.exact(observation.sourceQuality())) {
        ++exactObservationCount;
        if (ReactionOutcomeCalibrationCoverageAnalyzer.externallyVerifiedExact(
            observation.sourceQuality())) {
          ++externallyVerifiedExactObservationCount;
        }
        if (ReactionOutcomeCalibrationCoverageAnalyzer.sourceIdentified(observation)) {
          ++sourceIdentifiedExactObservationCount;
        } else {
          unreferencedExactObservationIds.add(observation.id());
        }
      }
      if (observation.sourceQuality()
          == ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR) {
        ++classPriorObservationCount;
        if (ReactionOutcomeCalibrationCoverageAnalyzer.sourceTracedPrior(observation)) {
          sourceTracedPriorObservationIds.add(observation.id());
        }
      }
    }
    final ArrayList<String> incompleteDatasetIds = new ArrayList<String>();
    final ArrayList<String> weakDatasetIds = new ArrayList<String>();
    final List<ReactionOutcomeDistributionDataset> datasets = library.datasets();
    for (int i = 0; i < datasets.size(); ++i) {
      final ReactionOutcomeDistributionDataset dataset = datasets.get(i);
      if (dataset.hasUnassignedProbabilityMass(COMPLETE_DISTRIBUTION_TOLERANCE)) {
        incompleteDatasetIds.add(dataset.id());
      }
      if (ReactionOutcomeCalibrationCoverageAnalyzer.weakDataset(dataset)) {
        weakDatasetIds.add(dataset.id());
      }
    }
    return ReactionOutcomeCalibrationCoverageReport.of(
        observations.size(),
        datasets.size(),
        exactObservationCount,
        externallyVerifiedExactObservationCount,
        classPriorObservationCount,
        sourceIdentifiedExactObservationCount,
        sourceTracedPriorObservationIds.size(),
        domainCounts,
        mechanismCounts,
        ReactionOutcomeCalibrationCoverageAnalyzer.missingDomains(domainCounts),
        ReactionOutcomeCalibrationCoverageAnalyzer.missingMechanismFamilies(mechanismCounts),
        incompleteDatasetIds,
        weakDatasetIds,
        unreferencedExactObservationIds,
        sourceTracedPriorObservationIds);
  }

  private static boolean sourceIdentified(final ReactionOutcomeCalibrationObservation observation) {
    return !"unspecified".equals(observation.sourceIdentifier())
        && !"unspecified".equals(observation.tableReference())
        && observation.exactProductDistributionRow();
  }

  private static boolean sourceTracedPrior(
      final ReactionOutcomeCalibrationObservation observation) {
    return !"unspecified".equals(observation.sourceIdentifier())
        && !"unspecified".equals(observation.tableReference())
        && !observation.exactProductDistributionRow();
  }

  private static void addDomains(
      final EnumMap<OrganicReactionDomain, Integer> domainCounts,
      final List<OrganicReactionDomain> domains) {
    for (int i = 0; i < domains.size(); ++i) {
      final OrganicReactionDomain domain = domains.get(i);
      if (domain != OrganicReactionDomain.UNKNOWN) {
        domainCounts.merge(domain, 1, Integer::sum);
      }
    }
  }

  private static boolean weakDataset(final ReactionOutcomeDistributionDataset dataset) {
    if (!ReactionOutcomeCalibrationCoverageAnalyzer.exact(dataset.sourceQuality())) {
      return true;
    }
    int exactRows = 0;
    final List<ReactionOutcomeCalibrationObservation> observations = dataset.observations();
    for (int i = 0; i < observations.size(); ++i) {
      if (ReactionOutcomeCalibrationCoverageAnalyzer.exact(observations.get(i).sourceQuality())) {
        ++exactRows;
      }
    }
    return exactRows < MIN_EXACT_ROWS_PER_STRONG_DATASET;
  }

  private static boolean exact(final ReactionOutcomeSourceQuality quality) {
    return quality == ReactionOutcomeSourceQuality.EXACT_EXPERIMENTAL_PRODUCT_DISTRIBUTION
        || quality == ReactionOutcomeSourceQuality.EXPERIMENTAL_YIELD_TABLE
        || quality == ReactionOutcomeSourceQuality.TIME_RESOLVED_POPULATION_STUDY
        || quality == ReactionOutcomeSourceQuality.LOCAL_BENCHMARK
        || quality == ReactionOutcomeSourceQuality.USER_OBSERVATION;
  }

  private static boolean externallyVerifiedExact(final ReactionOutcomeSourceQuality quality) {
    return quality == ReactionOutcomeSourceQuality.EXACT_EXPERIMENTAL_PRODUCT_DISTRIBUTION
        || quality == ReactionOutcomeSourceQuality.EXPERIMENTAL_YIELD_TABLE
        || quality == ReactionOutcomeSourceQuality.TIME_RESOLVED_POPULATION_STUDY;
  }

  private static List<OrganicReactionDomain> missingDomains(
      final EnumMap<OrganicReactionDomain, Integer> domainCounts) {
    final ArrayList<OrganicReactionDomain> missing = new ArrayList<OrganicReactionDomain>();
    final OrganicReactionDomain[] domains = OrganicReactionDomain.values();
    for (int i = 0; i < domains.length; ++i) {
      final OrganicReactionDomain domain = domains[i];
      if (domain == OrganicReactionDomain.UNKNOWN || domainCounts.containsKey(domain)) {
        continue;
      }
      missing.add(domain);
    }
    return List.copyOf(missing);
  }

  private static List<ReactionMechanismFamily> missingMechanismFamilies(
      final EnumMap<ReactionMechanismFamily, Integer> mechanismCounts) {
    final ArrayList<ReactionMechanismFamily> missing = new ArrayList<ReactionMechanismFamily>();
    final ReactionMechanismFamily[] families = ReactionMechanismFamily.values();
    for (int i = 0; i < families.length; ++i) {
      final ReactionMechanismFamily family = families[i];
      if (family == ReactionMechanismFamily.UNKNOWN || mechanismCounts.containsKey(family)) {
        continue;
      }
      missing.add(family);
    }
    return List.copyOf(missing);
  }
}

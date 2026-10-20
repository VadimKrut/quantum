/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration;

import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;

/** Отчет о полноте калибровочных данных для предсказания распределений продуктов. */
public final class ReactionOutcomeCalibrationCoverageReport {

  private final int observationCount;
  private final int datasetCount;
  private final int exactObservationCount;
  private final int externallyVerifiedExactObservationCount;
  private final int classPriorObservationCount;
  private final int sourceIdentifiedExactObservationCount;
  private final int sourceTracedPriorObservationCount;
  private final EnumMap<OrganicReactionDomain, Integer> domainCounts;
  private final EnumMap<ReactionMechanismFamily, Integer> mechanismCounts;
  private final List<OrganicReactionDomain> missingDomains;
  private final List<ReactionMechanismFamily> missingMechanismFamilies;
  private final List<String> incompleteDatasetIds;
  private final List<String> weakDatasetIds;
  private final List<String> unreferencedExactObservationIds;
  private final List<String> sourceTracedPriorObservationIds;

  private ReactionOutcomeCalibrationCoverageReport(
      final int observationCount,
      final int datasetCount,
      final int exactObservationCount,
      final int externallyVerifiedExactObservationCount,
      final int classPriorObservationCount,
      final int sourceIdentifiedExactObservationCount,
      final int sourceTracedPriorObservationCount,
      final EnumMap<OrganicReactionDomain, Integer> domainCounts,
      final EnumMap<ReactionMechanismFamily, Integer> mechanismCounts,
      final List<OrganicReactionDomain> missingDomains,
      final List<ReactionMechanismFamily> missingMechanismFamilies,
      final List<String> incompleteDatasetIds,
      final List<String> weakDatasetIds,
      final List<String> unreferencedExactObservationIds,
      final List<String> sourceTracedPriorObservationIds) {
    this.observationCount = observationCount;
    this.datasetCount = datasetCount;
    this.exactObservationCount = exactObservationCount;
    this.externallyVerifiedExactObservationCount = externallyVerifiedExactObservationCount;
    this.classPriorObservationCount = classPriorObservationCount;
    this.sourceIdentifiedExactObservationCount = sourceIdentifiedExactObservationCount;
    this.sourceTracedPriorObservationCount = sourceTracedPriorObservationCount;
    this.domainCounts = domainCounts;
    this.mechanismCounts = mechanismCounts;
    this.missingDomains = missingDomains;
    this.missingMechanismFamilies = missingMechanismFamilies;
    this.incompleteDatasetIds = incompleteDatasetIds;
    this.weakDatasetIds = weakDatasetIds;
    this.unreferencedExactObservationIds = unreferencedExactObservationIds;
    this.sourceTracedPriorObservationIds = sourceTracedPriorObservationIds;
  }

  public static ReactionOutcomeCalibrationCoverageReport of(
      final int observationCount,
      final int datasetCount,
      final int exactObservationCount,
      final int externallyVerifiedExactObservationCount,
      final int classPriorObservationCount,
      final int sourceIdentifiedExactObservationCount,
      final int sourceTracedPriorObservationCount,
      final EnumMap<OrganicReactionDomain, Integer> domainCounts,
      final EnumMap<ReactionMechanismFamily, Integer> mechanismCounts,
      final List<OrganicReactionDomain> missingDomains,
      final List<ReactionMechanismFamily> missingMechanismFamilies,
      final List<String> incompleteDatasetIds,
      final List<String> weakDatasetIds,
      final List<String> unreferencedExactObservationIds,
      final List<String> sourceTracedPriorObservationIds) {
    if (observationCount < 0
        || datasetCount < 0
        || exactObservationCount < 0
        || externallyVerifiedExactObservationCount < 0
        || classPriorObservationCount < 0
        || sourceIdentifiedExactObservationCount < 0
        || sourceTracedPriorObservationCount < 0) {
      throw new IllegalArgumentException("Calibration coverage counters must be non-negative.");
    }
    return new ReactionOutcomeCalibrationCoverageReport(
        observationCount,
        datasetCount,
        exactObservationCount,
        externallyVerifiedExactObservationCount,
        classPriorObservationCount,
        sourceIdentifiedExactObservationCount,
        sourceTracedPriorObservationCount,
        new EnumMap<OrganicReactionDomain, Integer>(
            Objects.requireNonNull(domainCounts, "Calibration domain counts must not be null.")),
        new EnumMap<ReactionMechanismFamily, Integer>(
            Objects.requireNonNull(
                mechanismCounts, "Calibration mechanism counts must not be null.")),
        List.copyOf(
            Objects.requireNonNull(
                missingDomains, "Missing calibration domains must not be null.")),
        List.copyOf(
            Objects.requireNonNull(
                missingMechanismFamilies,
                "Missing calibration mechanism families must not be null.")),
        List.copyOf(
            Objects.requireNonNull(
                incompleteDatasetIds, "Incomplete calibration dataset ids must not be null.")),
        List.copyOf(
            Objects.requireNonNull(
                weakDatasetIds, "Weak calibration dataset ids must not be null.")),
        List.copyOf(
            Objects.requireNonNull(
                unreferencedExactObservationIds,
                "Unreferenced exact calibration observation ids must not be null.")),
        List.copyOf(
            Objects.requireNonNull(
                sourceTracedPriorObservationIds,
                "Source-traced prior calibration observation ids must not be null.")));
  }

  public int observationCount() {
    return this.observationCount;
  }

  public int datasetCount() {
    return this.datasetCount;
  }

  public int exactObservationCount() {
    return this.exactObservationCount;
  }

  public int externallyVerifiedExactObservationCount() {
    return this.externallyVerifiedExactObservationCount;
  }

  public int classPriorObservationCount() {
    return this.classPriorObservationCount;
  }

  public int sourceIdentifiedExactObservationCount() {
    return this.sourceIdentifiedExactObservationCount;
  }

  public int sourceTracedPriorObservationCount() {
    return this.sourceTracedPriorObservationCount;
  }

  public EnumMap<OrganicReactionDomain, Integer> domainCounts() {
    return new EnumMap<OrganicReactionDomain, Integer>(this.domainCounts);
  }

  public EnumMap<ReactionMechanismFamily, Integer> mechanismCounts() {
    return new EnumMap<ReactionMechanismFamily, Integer>(this.mechanismCounts);
  }

  public List<OrganicReactionDomain> missingDomains() {
    return this.missingDomains;
  }

  public List<ReactionMechanismFamily> missingMechanismFamilies() {
    return this.missingMechanismFamilies;
  }

  public List<String> incompleteDatasetIds() {
    return this.incompleteDatasetIds;
  }

  public List<String> weakDatasetIds() {
    return this.weakDatasetIds;
  }

  public List<String> unreferencedExactObservationIds() {
    return this.unreferencedExactObservationIds;
  }

  public List<String> sourceTracedPriorObservationIds() {
    return this.sourceTracedPriorObservationIds;
  }

  public boolean hasExactExperimentalCalibration() {
    return this.exactObservationCount > 0;
  }

  public boolean productionReadyForEmpiricalPrediction() {
    return this.hasExactExperimentalCalibration()
        && this.externallyVerifiedExactObservationCount > 0
        && this.sourceIdentifiedExactObservationCount
            == this.externallyVerifiedExactObservationCount
        && this.incompleteDatasetIds.isEmpty()
        && this.weakDatasetIds.isEmpty()
        && this.unreferencedExactObservationIds.isEmpty()
        && this.missingDomains.isEmpty()
        && this.missingMechanismFamilies.isEmpty();
  }
}

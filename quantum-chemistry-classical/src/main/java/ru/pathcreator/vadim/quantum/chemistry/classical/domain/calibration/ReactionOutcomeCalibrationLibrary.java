/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;

/** Immutable-библиотека наблюдаемых распределений продуктов для калибровки scorer. */
public final class ReactionOutcomeCalibrationLibrary {

  public static final ReactionOutcomeCalibrationLibrary EMPTY =
      new ReactionOutcomeCalibrationLibrary(List.of(), List.of());

  private final List<ReactionOutcomeCalibrationObservation> observations;
  private final List<ReactionOutcomeDistributionDataset> datasets;

  private ReactionOutcomeCalibrationLibrary(
      final List<ReactionOutcomeCalibrationObservation> observations,
      final List<ReactionOutcomeDistributionDataset> datasets) {
    this.observations = observations;
    this.datasets = datasets;
  }

  public static ReactionOutcomeCalibrationLibrary of(
      final List<ReactionOutcomeCalibrationObservation> observations) {
    if (observations == null || observations.isEmpty()) {
      return ReactionOutcomeCalibrationLibrary.EMPTY;
    }
    for (int i = 0; i < observations.size(); ++i) {
      if (observations.get(i) == null) {
        throw new IllegalArgumentException("Calibration observation must not be null.");
      }
    }
    return new ReactionOutcomeCalibrationLibrary(List.copyOf(observations), List.of());
  }

  public static ReactionOutcomeCalibrationLibrary of(
      final List<ReactionOutcomeCalibrationObservation> observations,
      final List<ReactionOutcomeDistributionDataset> datasets) {
    final List<ReactionOutcomeCalibrationObservation> checkedObservations =
        ReactionOutcomeCalibrationLibrary.checkedObservations(observations);
    final List<ReactionOutcomeDistributionDataset> checkedDatasets =
        ReactionOutcomeCalibrationLibrary.checkedDatasets(datasets);
    if (checkedObservations.isEmpty() && checkedDatasets.isEmpty()) {
      return ReactionOutcomeCalibrationLibrary.EMPTY;
    }
    final ArrayList<ReactionOutcomeCalibrationObservation> flattened =
        new ArrayList<ReactionOutcomeCalibrationObservation>(checkedObservations);
    for (int i = 0; i < checkedDatasets.size(); ++i) {
      flattened.addAll(checkedDatasets.get(i).observations());
    }
    ReactionOutcomeCalibrationLibrary.requireUniqueObservationIds(flattened);
    ReactionOutcomeCalibrationLibrary.requireUniqueDatasetIds(checkedDatasets);
    return new ReactionOutcomeCalibrationLibrary(List.copyOf(flattened), checkedDatasets);
  }

  public static ReactionOutcomeCalibrationLibrary ofDatasets(
      final List<ReactionOutcomeDistributionDataset> datasets) {
    return ReactionOutcomeCalibrationLibrary.of(List.of(), datasets);
  }

  public List<ReactionOutcomeCalibrationObservation> observations() {
    return this.observations;
  }

  public List<ReactionOutcomeDistributionDataset> datasets() {
    return this.datasets;
  }

  public boolean empty() {
    return this.observations.isEmpty() && this.datasets.isEmpty();
  }

  public List<ReactionOutcomeCalibrationObservation> matchingDomains(
      final List<OrganicReactionDomain> domains) {
    if (this.observations.isEmpty()) {
      return List.of();
    }
    if (domains == null || domains.isEmpty()) {
      return this.observations;
    }
    final ArrayList<ReactionOutcomeCalibrationObservation> matches =
        new ArrayList<ReactionOutcomeCalibrationObservation>();
    for (int i = 0; i < this.observations.size(); ++i) {
      final ReactionOutcomeCalibrationObservation observation = this.observations.get(i);
      if (ReactionOutcomeCalibrationLibrary.matchesAnyDomain(observation, domains)) {
        matches.add(observation);
      }
    }
    return List.copyOf(matches);
  }

  private static boolean matchesAnyDomain(
      final ReactionOutcomeCalibrationObservation observation,
      final List<OrganicReactionDomain> domains) {
    for (int i = 0; i < domains.size(); ++i) {
      if (observation.matchesDomain(domains.get(i))) {
        return true;
      }
    }
    return false;
  }

  private static List<ReactionOutcomeCalibrationObservation> checkedObservations(
      final List<ReactionOutcomeCalibrationObservation> observations) {
    if (observations == null || observations.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < observations.size(); ++i) {
      if (observations.get(i) == null) {
        throw new IllegalArgumentException("Calibration observation must not be null.");
      }
    }
    return List.copyOf(observations);
  }

  private static void requireUniqueObservationIds(
      final List<ReactionOutcomeCalibrationObservation> observations) {
    final Set<String> ids = new HashSet<String>(observations.size());
    for (int i = 0; i < observations.size(); ++i) {
      final String id = observations.get(i).id();
      if (!ids.add(id)) {
        throw new IllegalArgumentException(
            "Calibration observation id must be unique: " + id + ".");
      }
    }
  }

  private static List<ReactionOutcomeDistributionDataset> checkedDatasets(
      final List<ReactionOutcomeDistributionDataset> datasets) {
    if (datasets == null || datasets.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < datasets.size(); ++i) {
      if (datasets.get(i) == null) {
        throw new IllegalArgumentException("Calibration distribution dataset must not be null.");
      }
    }
    return List.copyOf(datasets);
  }

  private static void requireUniqueDatasetIds(
      final List<ReactionOutcomeDistributionDataset> datasets) {
    final Set<String> ids = new HashSet<String>(datasets.size());
    for (int i = 0; i < datasets.size(); ++i) {
      final String id = datasets.get(i).id();
      if (!ids.add(id)) {
        throw new IllegalArgumentException(
            "Calibration distribution dataset id must be unique: " + id + ".");
      }
    }
  }
}

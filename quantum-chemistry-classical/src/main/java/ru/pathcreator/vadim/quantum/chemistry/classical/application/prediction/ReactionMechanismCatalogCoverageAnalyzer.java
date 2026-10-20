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
import java.util.EnumSet;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism.ReactionMechanismCatalogCoverageReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism.ReactionMechanismPattern;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism.ReactionMechanismPatternCatalog;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanisticEventKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;

/**
 * Проверяет полноту каталога механизмов относительно общих словарей chemistry-core.
 */
public final class ReactionMechanismCatalogCoverageAnalyzer {

  private static final int MIN_DETAILED_STEP_COUNT = 3;
  private static final int MIN_APPLICABILITY_SIGNAL_COUNT = 2;
  private static final int MIN_RISK_SIGNAL_COUNT = 2;

  public ReactionMechanismCatalogCoverageReport analyze(
      final ReactionMechanismPatternCatalog catalog) {
    if (catalog == null) {
      throw new IllegalArgumentException("Reaction mechanism pattern catalog must not be null.");
    }
    final List<ReactionMechanismPattern> patterns = catalog.patterns();
    final EnumMap<ReactionMechanismFamily, Integer> familyCounts =
        new EnumMap<ReactionMechanismFamily, Integer>(ReactionMechanismFamily.class);
    final EnumSet<MechanisticEventKind> usedEvents = EnumSet.noneOf(MechanisticEventKind.class);
    final ArrayList<String> weakPatternIds = new ArrayList<String>();
    final ArrayList<String> syntheticGenericPatternIds = new ArrayList<String>();
    final ArrayList<String> underSpecifiedPatternIds = new ArrayList<String>();
    int chemicallyUsablePatternCount = 0;
    int productionDetailedPatternCount = 0;
    for (int i = 0; i < patterns.size(); ++i) {
      final ReactionMechanismPattern pattern = patterns.get(i);
      familyCounts.merge(pattern.family(), 1, Integer::sum);
      usedEvents.addAll(pattern.eventSequence());
      if (pattern.productionDetailedPattern()) {
        ++productionDetailedPatternCount;
      }
      if (pattern.chemicallyUsablePattern()) {
        ++chemicallyUsablePatternCount;
      }
      if (pattern.syntheticGenericPattern()) {
        syntheticGenericPatternIds.add(pattern.family().name());
      }
      if (ReactionMechanismCatalogCoverageAnalyzer.underSpecified(pattern)) {
        underSpecifiedPatternIds.add(pattern.family().name());
      }
      if (pattern.family() != ReactionMechanismFamily.UNKNOWN
          && ReactionMechanismCatalogCoverageAnalyzer.weak(pattern)) {
        weakPatternIds.add(pattern.family().name());
      }
    }
    return ReactionMechanismCatalogCoverageReport.of(
        patterns.size(),
        ReactionMechanismCatalogCoverageAnalyzer.familyCount(),
        ReactionMechanismCatalogCoverageAnalyzer.eventKindCount(),
        syntheticGenericPatternIds.size(),
        chemicallyUsablePatternCount,
        productionDetailedPatternCount,
        underSpecifiedPatternIds.size(),
        ReactionMechanismCatalogCoverageAnalyzer.missingFamilies(familyCounts),
        ReactionMechanismCatalogCoverageAnalyzer.duplicateFamilies(familyCounts),
        ReactionMechanismCatalogCoverageAnalyzer.unusedEventKinds(usedEvents),
        weakPatternIds,
        syntheticGenericPatternIds,
        underSpecifiedPatternIds);
  }

  private static boolean weak(final ReactionMechanismPattern pattern) {
    return pattern.syntheticGenericPattern()
        || pattern.detailedSteps().size() < MIN_DETAILED_STEP_COUNT
        || pattern.applicabilitySignals().size() < MIN_APPLICABILITY_SIGNAL_COUNT
        || pattern.riskSignals().size() < MIN_RISK_SIGNAL_COUNT;
  }

  private static boolean underSpecified(final ReactionMechanismPattern pattern) {
    return pattern.family() != ReactionMechanismFamily.UNKNOWN
        && !pattern.syntheticGenericPattern()
        && !pattern.chemicallyUsablePattern();
  }

  private static int familyCount() {
    int count = 0;
    final ReactionMechanismFamily[] families = ReactionMechanismFamily.values();
    for (int i = 0; i < families.length; ++i) {
      if (families[i] != ReactionMechanismFamily.UNKNOWN) {
        ++count;
      }
    }
    return count;
  }

  private static int eventKindCount() {
    int count = 0;
    final MechanisticEventKind[] events = MechanisticEventKind.values();
    for (int i = 0; i < events.length; ++i) {
      if (events[i] != MechanisticEventKind.UNKNOWN) {
        ++count;
      }
    }
    return count;
  }

  private static List<ReactionMechanismFamily> missingFamilies(
      final EnumMap<ReactionMechanismFamily, Integer> familyCounts) {
    final ArrayList<ReactionMechanismFamily> missing = new ArrayList<ReactionMechanismFamily>();
    final ReactionMechanismFamily[] families = ReactionMechanismFamily.values();
    for (int i = 0; i < families.length; ++i) {
      final ReactionMechanismFamily family = families[i];
      if (family == ReactionMechanismFamily.UNKNOWN || familyCounts.containsKey(family)) {
        continue;
      }
      missing.add(family);
    }
    return List.copyOf(missing);
  }

  private static List<ReactionMechanismFamily> duplicateFamilies(
      final EnumMap<ReactionMechanismFamily, Integer> familyCounts) {
    final ArrayList<ReactionMechanismFamily> duplicates = new ArrayList<ReactionMechanismFamily>();
    final ReactionMechanismFamily[] families = ReactionMechanismFamily.values();
    for (int i = 0; i < families.length; ++i) {
      final ReactionMechanismFamily family = families[i];
      if (family == ReactionMechanismFamily.UNKNOWN) {
        continue;
      }
      final Integer count = familyCounts.get(family);
      if (count != null && count.intValue() > 1) {
        duplicates.add(family);
      }
    }
    return List.copyOf(duplicates);
  }

  private static List<MechanisticEventKind> unusedEventKinds(
      final EnumSet<MechanisticEventKind> usedEvents) {
    final ArrayList<MechanisticEventKind> unused = new ArrayList<MechanisticEventKind>();
    final MechanisticEventKind[] events = MechanisticEventKind.values();
    for (int i = 0; i < events.length; ++i) {
      final MechanisticEventKind event = events[i];
      if (event == MechanisticEventKind.UNKNOWN || usedEvents.contains(event)) {
        continue;
      }
      unused.add(event);
    }
    return List.copyOf(unused);
  }
}

/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanisticEventKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;

/**
 * Отчет о полноте встроенного каталога механизмов относительно словаря chemistry-core.
 */
public final class ReactionMechanismCatalogCoverageReport {

  private final int patternCount;
  private final int familyCount;
  private final int eventKindCount;
  private final int syntheticGenericPatternCount;
  private final int chemicallyUsablePatternCount;
  private final int productionDetailedPatternCount;
  private final int underSpecifiedPatternCount;
  private final List<ReactionMechanismFamily> missingFamilies;
  private final List<ReactionMechanismFamily> duplicateFamilies;
  private final List<MechanisticEventKind> unusedEventKinds;
  private final List<String> weakPatternIds;
  private final List<String> syntheticGenericPatternIds;
  private final List<String> underSpecifiedPatternIds;

  private ReactionMechanismCatalogCoverageReport(
      final int patternCount,
      final int familyCount,
      final int eventKindCount,
      final int syntheticGenericPatternCount,
      final int chemicallyUsablePatternCount,
      final int productionDetailedPatternCount,
      final int underSpecifiedPatternCount,
      final List<ReactionMechanismFamily> missingFamilies,
      final List<ReactionMechanismFamily> duplicateFamilies,
      final List<MechanisticEventKind> unusedEventKinds,
      final List<String> weakPatternIds,
      final List<String> syntheticGenericPatternIds,
      final List<String> underSpecifiedPatternIds) {
    this.patternCount = patternCount;
    this.familyCount = familyCount;
    this.eventKindCount = eventKindCount;
    this.syntheticGenericPatternCount = syntheticGenericPatternCount;
    this.chemicallyUsablePatternCount = chemicallyUsablePatternCount;
    this.productionDetailedPatternCount = productionDetailedPatternCount;
    this.underSpecifiedPatternCount = underSpecifiedPatternCount;
    this.missingFamilies = missingFamilies;
    this.duplicateFamilies = duplicateFamilies;
    this.unusedEventKinds = unusedEventKinds;
    this.weakPatternIds = weakPatternIds;
    this.syntheticGenericPatternIds = syntheticGenericPatternIds;
    this.underSpecifiedPatternIds = underSpecifiedPatternIds;
  }

  public static ReactionMechanismCatalogCoverageReport of(
      final int patternCount,
      final int familyCount,
      final int eventKindCount,
      final int syntheticGenericPatternCount,
      final int chemicallyUsablePatternCount,
      final int productionDetailedPatternCount,
      final int underSpecifiedPatternCount,
      final List<ReactionMechanismFamily> missingFamilies,
      final List<ReactionMechanismFamily> duplicateFamilies,
      final List<MechanisticEventKind> unusedEventKinds,
      final List<String> weakPatternIds,
      final List<String> syntheticGenericPatternIds,
      final List<String> underSpecifiedPatternIds) {
    if (patternCount < 0
        || familyCount < 0
        || eventKindCount < 0
        || syntheticGenericPatternCount < 0
        || chemicallyUsablePatternCount < 0
        || productionDetailedPatternCount < 0
        || underSpecifiedPatternCount < 0) {
      throw new IllegalArgumentException(
          "Reaction mechanism coverage counts must be non-negative.");
    }
    if (chemicallyUsablePatternCount < productionDetailedPatternCount) {
      throw new IllegalArgumentException(
          "Usable mechanism count must include production detailed mechanisms.");
    }
    if (syntheticGenericPatternCount + chemicallyUsablePatternCount + underSpecifiedPatternCount
        > patternCount + productionDetailedPatternCount) {
      throw new IllegalArgumentException("Reaction mechanism quality counts are inconsistent.");
    }
    return new ReactionMechanismCatalogCoverageReport(
        patternCount,
        familyCount,
        eventKindCount,
        syntheticGenericPatternCount,
        chemicallyUsablePatternCount,
        productionDetailedPatternCount,
        underSpecifiedPatternCount,
        ReactionMechanismCatalogCoverageReport.copyFamilies(missingFamilies),
        ReactionMechanismCatalogCoverageReport.copyFamilies(duplicateFamilies),
        ReactionMechanismCatalogCoverageReport.copyEvents(unusedEventKinds),
        ReactionMechanismCatalogCoverageReport.copyTexts(weakPatternIds),
        ReactionMechanismCatalogCoverageReport.copyTexts(syntheticGenericPatternIds),
        ReactionMechanismCatalogCoverageReport.copyTexts(underSpecifiedPatternIds));
  }

  public int patternCount() {
    return this.patternCount;
  }

  public int familyCount() {
    return this.familyCount;
  }

  public int eventKindCount() {
    return this.eventKindCount;
  }

  public int syntheticGenericPatternCount() {
    return this.syntheticGenericPatternCount;
  }

  public int chemicallyUsablePatternCount() {
    return this.chemicallyUsablePatternCount;
  }

  public int productionDetailedPatternCount() {
    return this.productionDetailedPatternCount;
  }

  public int underSpecifiedPatternCount() {
    return this.underSpecifiedPatternCount;
  }

  public List<ReactionMechanismFamily> missingFamilies() {
    return this.missingFamilies;
  }

  public List<ReactionMechanismFamily> duplicateFamilies() {
    return this.duplicateFamilies;
  }

  public List<MechanisticEventKind> unusedEventKinds() {
    return this.unusedEventKinds;
  }

  public List<String> weakPatternIds() {
    return this.weakPatternIds;
  }

  public List<String> syntheticGenericPatternIds() {
    return this.syntheticGenericPatternIds;
  }

  public List<String> underSpecifiedPatternIds() {
    return this.underSpecifiedPatternIds;
  }

  public boolean complete() {
    return this.missingFamilies.isEmpty()
        && this.duplicateFamilies.isEmpty()
        && this.unusedEventKinds.isEmpty()
        && this.weakPatternIds.isEmpty();
  }

  public boolean taxonomyComplete() {
    return this.missingFamilies.isEmpty()
        && this.duplicateFamilies.isEmpty()
        && this.unusedEventKinds.isEmpty();
  }

  public boolean productionDetailedComplete() {
    return this.taxonomyComplete()
        && this.syntheticGenericPatternIds.isEmpty()
        && this.underSpecifiedPatternIds.isEmpty()
        && this.productionDetailedPatternCount == this.familyCount;
  }

  public double productionCoverageRatio() {
    if (this.familyCount == 0) {
      return 1.0;
    }
    return (double) this.productionDetailedPatternCount / (double) this.familyCount;
  }

  public double chemicallyUsableCoverageRatio() {
    if (this.familyCount == 0) {
      return 1.0;
    }
    return (double) this.chemicallyUsablePatternCount / (double) this.familyCount;
  }

  private static List<ReactionMechanismFamily> copyFamilies(
      final List<ReactionMechanismFamily> families) {
    if (families == null || families.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < families.size(); ++i) {
      if (families.get(i) == null) {
        throw new IllegalArgumentException("Reaction mechanism coverage family must not be null.");
      }
    }
    return List.copyOf(families);
  }

  private static List<MechanisticEventKind> copyEvents(final List<MechanisticEventKind> events) {
    if (events == null || events.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < events.size(); ++i) {
      if (events.get(i) == null) {
        throw new IllegalArgumentException(
            "Reaction mechanism coverage event kind must not be null.");
      }
    }
    return List.copyOf(events);
  }

  private static List<String> copyTexts(final List<String> texts) {
    if (texts == null || texts.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < texts.size(); ++i) {
      if (texts.get(i) == null || texts.get(i).isBlank()) {
        throw new IllegalArgumentException(
            "Reaction mechanism coverage weak pattern id must not be blank.");
      }
    }
    return List.copyOf(texts);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionMechanismCatalogCoverageReport)) {
      return false;
    }
    final ReactionMechanismCatalogCoverageReport report =
        (ReactionMechanismCatalogCoverageReport) other;
    return this.patternCount == report.patternCount
        && this.familyCount == report.familyCount
        && this.eventKindCount == report.eventKindCount
        && this.syntheticGenericPatternCount == report.syntheticGenericPatternCount
        && this.chemicallyUsablePatternCount == report.chemicallyUsablePatternCount
        && this.productionDetailedPatternCount == report.productionDetailedPatternCount
        && this.underSpecifiedPatternCount == report.underSpecifiedPatternCount
        && Objects.equals(this.missingFamilies, report.missingFamilies)
        && Objects.equals(this.duplicateFamilies, report.duplicateFamilies)
        && Objects.equals(this.unusedEventKinds, report.unusedEventKinds)
        && Objects.equals(this.weakPatternIds, report.weakPatternIds)
        && Objects.equals(this.syntheticGenericPatternIds, report.syntheticGenericPatternIds)
        && Objects.equals(this.underSpecifiedPatternIds, report.underSpecifiedPatternIds);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.patternCount);
    result = ChemistryHash.include(result, this.familyCount);
    result = ChemistryHash.include(result, this.eventKindCount);
    result = ChemistryHash.include(result, this.syntheticGenericPatternCount);
    result = ChemistryHash.include(result, this.chemicallyUsablePatternCount);
    result = ChemistryHash.include(result, this.productionDetailedPatternCount);
    result = ChemistryHash.include(result, this.underSpecifiedPatternCount);
    result = ChemistryHash.include(result, this.missingFamilies);
    result = ChemistryHash.include(result, this.duplicateFamilies);
    result = ChemistryHash.include(result, this.unusedEventKinds);
    result = ChemistryHash.include(result, this.weakPatternIds);
    result = ChemistryHash.include(result, this.syntheticGenericPatternIds);
    result = ChemistryHash.include(result, this.underSpecifiedPatternIds);
    return result;
  }
}

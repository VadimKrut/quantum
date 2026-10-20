/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionRegioselectivityClass;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;

/** Наблюдаемый в литературе или лаборатории продуктовый канал реакции. */
public final class ReactionOutcomeCalibrationObservation {

  private final String id;
  private final List<OrganicReactionDomain> domains;
  private final ReactionMechanismFamily mechanismFamily;
  private final List<ReactionConditionFeatureKind> conditionFeatures;
  private final String productFormulaSignature;
  private final String channelLabel;
  private final ReactionRegioselectivityClass regioselectivityClass;
  private final List<String> substrateClassHints;
  private final List<String> productChannelHints;
  private final double observedProbability;
  private final double observedYieldPercent;
  private final double confidence;
  private final ReactionOutcomeSourceQuality sourceQuality;
  private final String sourceIdentifier;
  private final String tableReference;
  private final boolean exactProductDistributionRow;
  private final String source;
  private final String note;

  private ReactionOutcomeCalibrationObservation(
      final String id,
      final List<OrganicReactionDomain> domains,
      final ReactionMechanismFamily mechanismFamily,
      final List<ReactionConditionFeatureKind> conditionFeatures,
      final String productFormulaSignature,
      final String channelLabel,
      final ReactionRegioselectivityClass regioselectivityClass,
      final List<String> substrateClassHints,
      final List<String> productChannelHints,
      final double observedProbability,
      final double observedYieldPercent,
      final double confidence,
      final ReactionOutcomeSourceQuality sourceQuality,
      final String sourceIdentifier,
      final String tableReference,
      final boolean exactProductDistributionRow,
      final String source,
      final String note) {
    this.id = id;
    this.domains = domains;
    this.mechanismFamily = mechanismFamily;
    this.conditionFeatures = conditionFeatures;
    this.productFormulaSignature = productFormulaSignature;
    this.channelLabel = channelLabel;
    this.regioselectivityClass = regioselectivityClass;
    this.substrateClassHints = substrateClassHints;
    this.productChannelHints = productChannelHints;
    this.observedProbability = observedProbability;
    this.observedYieldPercent = observedYieldPercent;
    this.confidence = confidence;
    this.sourceQuality = sourceQuality;
    this.sourceIdentifier = sourceIdentifier;
    this.tableReference = tableReference;
    this.exactProductDistributionRow = exactProductDistributionRow;
    this.source = source;
    this.note = note;
  }

  public static ReactionOutcomeCalibrationObservation of(
      final String id,
      final List<OrganicReactionDomain> domains,
      final String productFormulaSignature,
      final double observedProbability,
      final double observedYieldPercent,
      final String source,
      final String note) {
    return ReactionOutcomeCalibrationObservation.of(
        id,
        domains,
        ReactionMechanismFamily.UNKNOWN,
        List.of(),
        productFormulaSignature,
        "unlabeled",
        ReactionRegioselectivityClass.UNKNOWN,
        List.of(),
        List.of(),
        observedProbability,
        observedYieldPercent,
        0.75,
        ReactionOutcomeSourceQuality.UNKNOWN,
        source,
        note);
  }

  public static ReactionOutcomeCalibrationObservation of(
      final String id,
      final List<OrganicReactionDomain> domains,
      final ReactionMechanismFamily mechanismFamily,
      final List<ReactionConditionFeatureKind> conditionFeatures,
      final String productFormulaSignature,
      final String channelLabel,
      final double observedProbability,
      final double observedYieldPercent,
      final double confidence,
      final String source,
      final String note) {
    return ReactionOutcomeCalibrationObservation.of(
        id,
        domains,
        mechanismFamily,
        conditionFeatures,
        productFormulaSignature,
        channelLabel,
        ReactionRegioselectivityClass.UNKNOWN,
        List.of(),
        List.of(),
        observedProbability,
        observedYieldPercent,
        confidence,
        ReactionOutcomeSourceQuality.UNKNOWN,
        source,
        note);
  }

  public static ReactionOutcomeCalibrationObservation of(
      final String id,
      final List<OrganicReactionDomain> domains,
      final ReactionMechanismFamily mechanismFamily,
      final List<ReactionConditionFeatureKind> conditionFeatures,
      final String productFormulaSignature,
      final String channelLabel,
      final ReactionRegioselectivityClass regioselectivityClass,
      final List<String> substrateClassHints,
      final List<String> productChannelHints,
      final double observedProbability,
      final double observedYieldPercent,
      final double confidence,
      final ReactionOutcomeSourceQuality sourceQuality,
      final String source,
      final String note) {
    return ReactionOutcomeCalibrationObservation.of(
        id,
        domains,
        mechanismFamily,
        conditionFeatures,
        productFormulaSignature,
        channelLabel,
        regioselectivityClass,
        substrateClassHints,
        productChannelHints,
        observedProbability,
        observedYieldPercent,
        confidence,
        sourceQuality,
        "unspecified",
        "unspecified",
        false,
        source,
        note);
  }

  public static ReactionOutcomeCalibrationObservation of(
      final String id,
      final List<OrganicReactionDomain> domains,
      final ReactionMechanismFamily mechanismFamily,
      final List<ReactionConditionFeatureKind> conditionFeatures,
      final String productFormulaSignature,
      final String channelLabel,
      final ReactionRegioselectivityClass regioselectivityClass,
      final List<String> substrateClassHints,
      final List<String> productChannelHints,
      final double observedProbability,
      final double observedYieldPercent,
      final double confidence,
      final ReactionOutcomeSourceQuality sourceQuality,
      final String sourceIdentifier,
      final String tableReference,
      final boolean exactProductDistributionRow,
      final String source,
      final String note) {
    if (domains == null || domains.isEmpty()) {
      throw new IllegalArgumentException("Calibration observation domains must not be empty.");
    }
    for (int i = 0; i < domains.size(); ++i) {
      if (domains.get(i) == null) {
        throw new IllegalArgumentException("Calibration observation domain must not be null.");
      }
    }
    if (!Double.isFinite(observedProbability)
        || observedProbability <= 0.0
        || observedProbability > 1.0) {
      throw new IllegalArgumentException("Calibration observed probability must be in (0, 1].");
    }
    if (!Double.isFinite(observedYieldPercent)
        || observedYieldPercent < 0.0
        || observedYieldPercent > 100.0) {
      throw new IllegalArgumentException("Calibration observed yield percent must be in [0, 100].");
    }
    if (!Double.isFinite(confidence) || confidence <= 0.0 || confidence > 1.0) {
      throw new IllegalArgumentException("Calibration confidence must be in (0, 1].");
    }
    return new ReactionOutcomeCalibrationObservation(
        TextValue.requireText(id, "Calibration observation id"),
        List.copyOf(domains),
        mechanismFamily == null ? ReactionMechanismFamily.UNKNOWN : mechanismFamily,
        ReactionOutcomeCalibrationObservation.checkedConditionFeatures(conditionFeatures),
        TextValue.requireText(productFormulaSignature, "Calibration product formula signature"),
        TextValue.requireText(channelLabel, "Calibration product channel label"),
        regioselectivityClass == null
            ? ReactionRegioselectivityClass.UNKNOWN
            : regioselectivityClass,
        ReactionOutcomeCalibrationObservation.checkedTextHints(
            substrateClassHints, "Calibration substrate class hint"),
        ReactionOutcomeCalibrationObservation.checkedTextHints(
            productChannelHints, "Calibration product channel hint"),
        observedProbability,
        observedYieldPercent,
        confidence,
        sourceQuality == null ? ReactionOutcomeSourceQuality.UNKNOWN : sourceQuality,
        TextValue.requireText(sourceIdentifier, "Calibration source identifier"),
        TextValue.requireText(tableReference, "Calibration table reference"),
        exactProductDistributionRow,
        TextValue.requireText(source, "Calibration source"),
        TextValue.requireText(note, "Calibration note"));
  }

  public String id() {
    return this.id;
  }

  public List<OrganicReactionDomain> domains() {
    return this.domains;
  }

  public ReactionMechanismFamily mechanismFamily() {
    return this.mechanismFamily;
  }

  public List<ReactionConditionFeatureKind> conditionFeatures() {
    return this.conditionFeatures;
  }

  public String productFormulaSignature() {
    return this.productFormulaSignature;
  }

  public String channelLabel() {
    return this.channelLabel;
  }

  public ReactionRegioselectivityClass regioselectivityClass() {
    return this.regioselectivityClass;
  }

  public List<String> substrateClassHints() {
    return this.substrateClassHints;
  }

  public List<String> productChannelHints() {
    return this.productChannelHints;
  }

  public double observedProbability() {
    return this.observedProbability;
  }

  public double observedYieldPercent() {
    return this.observedYieldPercent;
  }

  public double confidence() {
    return this.confidence;
  }

  public ReactionOutcomeSourceQuality sourceQuality() {
    return this.sourceQuality;
  }

  public String sourceIdentifier() {
    return this.sourceIdentifier;
  }

  public String tableReference() {
    return this.tableReference;
  }

  public boolean exactProductDistributionRow() {
    return this.exactProductDistributionRow;
  }

  public String source() {
    return this.source;
  }

  public String note() {
    return this.note;
  }

  public boolean matchesDomain(final OrganicReactionDomain domain) {
    if (domain == null) {
      throw new IllegalArgumentException("Calibration domain must not be null.");
    }
    return this.domains.contains(domain) || this.domains.contains(OrganicReactionDomain.UNKNOWN);
  }

  public boolean matchesConditionFeatures(final List<ReactionConditionFeatureKind> features) {
    if (this.conditionFeatures.isEmpty()) {
      return true;
    }
    if (features == null || features.isEmpty()) {
      return false;
    }
    for (int i = 0; i < this.conditionFeatures.size(); ++i) {
      if (!features.contains(this.conditionFeatures.get(i))) {
        return false;
      }
    }
    return true;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionOutcomeCalibrationObservation)) {
      return false;
    }
    final ReactionOutcomeCalibrationObservation observation =
        (ReactionOutcomeCalibrationObservation) other;
    return Double.compare(this.observedProbability, observation.observedProbability) == 0
        && Double.compare(this.observedYieldPercent, observation.observedYieldPercent) == 0
        && Double.compare(this.confidence, observation.confidence) == 0
        && Objects.equals(this.id, observation.id)
        && Objects.equals(this.domains, observation.domains)
        && this.mechanismFamily == observation.mechanismFamily
        && Objects.equals(this.conditionFeatures, observation.conditionFeatures)
        && Objects.equals(this.productFormulaSignature, observation.productFormulaSignature)
        && Objects.equals(this.channelLabel, observation.channelLabel)
        && this.regioselectivityClass == observation.regioselectivityClass
        && Objects.equals(this.substrateClassHints, observation.substrateClassHints)
        && Objects.equals(this.productChannelHints, observation.productChannelHints)
        && Objects.equals(this.sourceIdentifier, observation.sourceIdentifier)
        && Objects.equals(this.tableReference, observation.tableReference)
        && this.exactProductDistributionRow == observation.exactProductDistributionRow
        && Objects.equals(this.source, observation.source)
        && this.sourceQuality == observation.sourceQuality
        && Objects.equals(this.note, observation.note);
  }

  public int hashCode() {
    return Objects.hash(
        this.id,
        this.domains,
        this.mechanismFamily,
        this.conditionFeatures,
        this.productFormulaSignature,
        this.channelLabel,
        this.regioselectivityClass,
        this.substrateClassHints,
        this.productChannelHints,
        this.observedProbability,
        this.observedYieldPercent,
        this.confidence,
        this.sourceQuality,
        this.sourceIdentifier,
        this.tableReference,
        this.exactProductDistributionRow,
        this.source,
        this.note);
  }

  private static List<ReactionConditionFeatureKind> checkedConditionFeatures(
      final List<ReactionConditionFeatureKind> features) {
    if (features == null || features.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < features.size(); ++i) {
      if (features.get(i) == null) {
        throw new IllegalArgumentException("Calibration condition feature must not be null.");
      }
    }
    return List.copyOf(features);
  }

  private static List<String> checkedTextHints(final List<String> hints, final String subject) {
    if (hints == null || hints.isEmpty()) {
      return List.of();
    }
    final java.util.ArrayList<String> checked = new java.util.ArrayList<String>(hints.size());
    for (int i = 0; i < hints.size(); ++i) {
      checked.add(TextValue.requireText(hints.get(i), subject));
    }
    return List.copyOf(checked);
  }
}

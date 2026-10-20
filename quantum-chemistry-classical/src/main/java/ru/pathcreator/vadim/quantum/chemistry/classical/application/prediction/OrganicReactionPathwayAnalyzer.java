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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism.ReactionMechanismPattern;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism.ReactionMechanismPatternCatalog;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism.ReactionMechanismPatternStep;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.BuiltInReactionMechanismPatternCatalog;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.PredictedReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionConditionWindow;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionMechanismClass;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPathwayAssessment;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPathwayStep;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionConfidenceLevel;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionRegioselectivityClass;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeature;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;

/**
 * Выводит механизм, селективность и рабочее окно условий из результата органического прогноза.
 */
public final class OrganicReactionPathwayAnalyzer {

  private static final int MAX_PATHWAY_EVIDENCE = 6;
  private static final double DEFAULT_MINIMUM_TEMPERATURE_KELVIN = 195.0;
  private static final double DEFAULT_MAXIMUM_TEMPERATURE_KELVIN = 473.15;
  private static final double DEFAULT_MINIMUM_DURATION_HOURS = 0.01;
  private static final double DEFAULT_MAXIMUM_DURATION_HOURS = 168.0;

  private final ReactionMechanismPatternCatalog mechanismCatalog;

  public OrganicReactionPathwayAnalyzer() {
    this(BuiltInReactionMechanismPatternCatalog.organicAndPhysicalChemistry());
  }

  public OrganicReactionPathwayAnalyzer(final ReactionMechanismPatternCatalog mechanismCatalog) {
    if (mechanismCatalog == null) {
      throw new IllegalArgumentException("Reaction mechanism pattern catalog must not be null.");
    }
    this.mechanismCatalog = mechanismCatalog;
  }

  public ReactionPathwayAssessment assess(
      final ReactionPredictionResult result,
      final ReactionPredictionConfidenceLevel confidenceLevel,
      final List<ReactionPredictionEvidence> evidence) {
    if (result == null) {
      throw new IllegalArgumentException("Reaction prediction result must not be null.");
    }
    if (confidenceLevel == null) {
      throw new IllegalArgumentException("Reaction prediction confidence level must not be null.");
    }
    final List<ReactionPredictionEvidence> checkedEvidence =
        OrganicReactionPathwayAnalyzer.checkedEvidence(evidence);
    return ReactionPathwayAssessment.of(
        this.mechanismClass(result, checkedEvidence),
        OrganicReactionPathwayAnalyzer.regioselectivityClass(result, checkedEvidence),
        OrganicReactionPathwayAnalyzer.conditionWindow(result),
        confidenceLevel,
        this.pathwaySteps(result),
        OrganicReactionPathwayAnalyzer.limitedBySign(checkedEvidence, true),
        OrganicReactionPathwayAnalyzer.limitedBySign(checkedEvidence, false));
  }

  private List<ReactionPathwayStep> pathwaySteps(final ReactionPredictionResult result) {
    if (!result.hasCandidates() || !result.bestCandidate().hasPredictedMechanisms()) {
      return List.of();
    }
    final ArrayList<ReactionPathwayStep> steps = new ArrayList<ReactionPathwayStep>();
    final List<PredictedReactionMechanism> mechanisms =
        result.bestCandidate().predictedMechanisms();
    for (int i = 0; i < mechanisms.size(); ++i) {
      final PredictedReactionMechanism mechanism = mechanisms.get(i);
      final ReactionMechanismPattern pattern = this.mechanismCatalog.find(mechanism.family());
      if (pattern == null) {
        continue;
      }
      final List<ReactionMechanismPatternStep> patternSteps = pattern.detailedSteps();
      for (int stepIndex = 0; stepIndex < patternSteps.size(); ++stepIndex) {
        final ReactionMechanismPatternStep patternStep = patternSteps.get(stepIndex);
        steps.add(
            ReactionPathwayStep.of(
                mechanism.mechanismId(),
                mechanism.family(),
                patternStep.orderIndex(),
                patternStep.eventKind(),
                patternStep.stageName(),
                patternStep.description(),
                mechanism.pathwayProbability(),
                patternStep.probabilityContribution()));
      }
    }
    return List.copyOf(steps);
  }

  private ReactionMechanismClass mechanismClass(
      final ReactionPredictionResult result, final List<ReactionPredictionEvidence> evidence) {
    if (result.request().hasDomain(OrganicReactionDomain.CROSS_COUPLING)
        || OrganicReactionPathwayAnalyzer.hasCatalystCouplingContext(
            result.request().conditions(), evidence)) {
      return ReactionMechanismClass.CROSS_COUPLING;
    }
    final ReactionMechanismClass familyClass =
        OrganicReactionPathwayAnalyzer.mechanismClassFromPredictedFamilies(result);
    if (familyClass != ReactionMechanismClass.UNKNOWN) {
      return familyClass;
    }
    if (OrganicReactionPathwayAnalyzer.hasNucleophilicAromaticSubstitutionContext(
        result, evidence)) {
      return ReactionMechanismClass.NUCLEOPHILIC_AROMATIC_SUBSTITUTION;
    }
    if (OrganicReactionPathwayAnalyzer.hasElectrophilicAromaticSubstitutionContext(
        result, evidence)) {
      return ReactionMechanismClass.ELECTROPHILIC_AROMATIC_SUBSTITUTION;
    }
    if (OrganicReactionPathwayAnalyzer.hasOxidationContext(result, evidence)) {
      return ReactionMechanismClass.OXIDATION;
    }
    if (OrganicReactionPathwayAnalyzer.hasReductionContext(result, evidence)) {
      return ReactionMechanismClass.REDUCTION;
    }
    if (result.request().hasDomain(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS)) {
      return ReactionMechanismClass.HETEROCYCLE_CONSTRUCTION;
    }
    if (result.request().hasDomain(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION)
        || OrganicReactionPathwayAnalyzer.hasEvidence(
            evidence, ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN)) {
      return ReactionMechanismClass.HETEROCYCLE_FUNCTIONALIZATION;
    }
    if (result.request().hasDomain(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION)) {
      return ReactionMechanismClass.NUCLEOPHILIC_AROMATIC_SUBSTITUTION;
    }
    if (result.request().hasDomain(OrganicReactionDomain.ELECTROPHILIC_SUBSTITUTION)) {
      return ReactionMechanismClass.ELECTROPHILIC_AROMATIC_SUBSTITUTION;
    }
    if (result.request().hasDomain(OrganicReactionDomain.CYCLIZATION)) {
      return ReactionMechanismClass.CYCLIZATION;
    }
    if (result.request().hasDomain(OrganicReactionDomain.ADDITION)) {
      return ReactionMechanismClass.ADDITION;
    }
    if (result.request().hasDomain(OrganicReactionDomain.ELIMINATION)) {
      return ReactionMechanismClass.ELIMINATION;
    }
    if (result.request().hasDomain(OrganicReactionDomain.OXIDATION_REDUCTION)) {
      return ReactionMechanismClass.OXIDATION;
    }
    if (result.request().hasDomain(OrganicReactionDomain.ACID_BASE)) {
      return ReactionMechanismClass.ACID_BASE;
    }
    if (result.request().hasDomain(OrganicReactionDomain.REARRANGEMENT)) {
      return ReactionMechanismClass.REARRANGEMENT;
    }
    if (result.request().conditions().hasFeature(ReactionConditionFeatureKind.PHOTOCHEMICAL)) {
      return ReactionMechanismClass.PHOTOCHEMICAL;
    }
    if (result.request().conditions().hasFeature(ReactionConditionFeatureKind.ELECTROCHEMICAL)) {
      return ReactionMechanismClass.ELECTROCHEMICAL;
    }
    return ReactionMechanismClass.UNKNOWN;
  }

  private static ReactionMechanismClass mechanismClassFromPredictedFamilies(
      final ReactionPredictionResult result) {
    if (!result.hasCandidates() || !result.bestCandidate().hasPredictedMechanisms()) {
      return ReactionMechanismClass.UNKNOWN;
    }
    final List<PredictedReactionMechanism> mechanisms =
        result.bestCandidate().predictedMechanisms();
    for (int i = 0; i < mechanisms.size(); ++i) {
      final ReactionMechanismClass mechanismClass =
          OrganicReactionPathwayAnalyzer.mechanismClassFromFamily(mechanisms.get(i).family());
      if (mechanismClass != ReactionMechanismClass.UNKNOWN) {
        return mechanismClass;
      }
    }
    return ReactionMechanismClass.UNKNOWN;
  }

  private static ReactionMechanismClass mechanismClassFromFamily(
      final ReactionMechanismFamily family) {
    if (family == null) {
      return ReactionMechanismClass.UNKNOWN;
    }
    if (family == ReactionMechanismFamily.ORGANOMETALLIC_CROSS_COUPLING
        || family == ReactionMechanismFamily.SUZUKI_COUPLING
        || family == ReactionMechanismFamily.HECK_COUPLING
        || family == ReactionMechanismFamily.SONOGASHIRA_COUPLING
        || family == ReactionMechanismFamily.NEGISHI_COUPLING
        || family == ReactionMechanismFamily.STILLE_COUPLING
        || family == ReactionMechanismFamily.BUCHWALD_HARTWIG_AMINATION
        || family == ReactionMechanismFamily.ORGANOMETALLIC_INSERTION
        || family == ReactionMechanismFamily.C_H_ACTIVATION) {
      return ReactionMechanismClass.CROSS_COUPLING;
    }
    if (family == ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION) {
      return ReactionMechanismClass.NUCLEOPHILIC_AROMATIC_SUBSTITUTION;
    }
    if (family == ReactionMechanismFamily.ELECTROPHILIC_AROMATIC_SUBSTITUTION
        || family == ReactionMechanismFamily.NITRATION
        || family == ReactionMechanismFamily.SULFONATION
        || family == ReactionMechanismFamily.FRIEDEL_CRAFTS_ALKYLATION
        || family == ReactionMechanismFamily.FRIEDEL_CRAFTS_ACYLATION
        || family == ReactionMechanismFamily.HALOGENATION) {
      return ReactionMechanismClass.ELECTROPHILIC_AROMATIC_SUBSTITUTION;
    }
    if (family == ReactionMechanismFamily.OXIDATION
        || family == ReactionMechanismFamily.HYDROBORATION_OXIDATION
        || family == ReactionMechanismFamily.EPOXIDATION
        || family == ReactionMechanismFamily.OZONOLYSIS
        || family == ReactionMechanismFamily.OXIDATIVE_CLEAVAGE
        || family == ReactionMechanismFamily.BAEYER_VILLIGER) {
      return ReactionMechanismClass.OXIDATION;
    }
    if (family == ReactionMechanismFamily.REDUCTION
        || family == ReactionMechanismFamily.HYDROGENATION
        || family == ReactionMechanismFamily.WOLFF_KISHNER_REDUCTION
        || family == ReactionMechanismFamily.CLEMMENSEN_REDUCTION
        || family == ReactionMechanismFamily.REDUCTIVE_AMINATION) {
      return ReactionMechanismClass.REDUCTION;
    }
    if (family == ReactionMechanismFamily.REARRANGEMENT
        || family == ReactionMechanismFamily.BECKMANN_REARRANGEMENT
        || family == ReactionMechanismFamily.PINACOL_REARRANGEMENT
        || family == ReactionMechanismFamily.CLAISEN_REARRANGEMENT
        || family == ReactionMechanismFamily.COPE_REARRANGEMENT
        || family == ReactionMechanismFamily.SIGMATROPIC) {
      return ReactionMechanismClass.REARRANGEMENT;
    }
    if (family == ReactionMechanismFamily.CYCLOADDITION
        || family == ReactionMechanismFamily.CLICK_CYCLOADDITION
        || family == ReactionMechanismFamily.DIELS_ALDER
        || family == ReactionMechanismFamily.ONE_THREE_DIPOLAR_CYCLOADDITION
        || family == ReactionMechanismFamily.PERICYCLIC
        || family == ReactionMechanismFamily.ELECTROCYCLIC) {
      return ReactionMechanismClass.CYCLIZATION;
    }
    if (family == ReactionMechanismFamily.ADDITION
        || family == ReactionMechanismFamily.CARBONYL_ADDITION
        || family == ReactionMechanismFamily.ELECTROPHILIC_ADDITION
        || family == ReactionMechanismFamily.MICHAEL_ADDITION
        || family == ReactionMechanismFamily.GRIGNARD_OR_ORGANOLITHIUM_ADDITION) {
      return ReactionMechanismClass.ADDITION;
    }
    if (family == ReactionMechanismFamily.ELIMINATION_E1
        || family == ReactionMechanismFamily.ELIMINATION_E2
        || family == ReactionMechanismFamily.FRAGMENTATION) {
      return ReactionMechanismClass.ELIMINATION;
    }
    if (family == ReactionMechanismFamily.ACID_BASE) {
      return ReactionMechanismClass.ACID_BASE;
    }
    if (family == ReactionMechanismFamily.RADICAL_SUBSTITUTION
        || family == ReactionMechanismFamily.RADICAL_CHAIN
        || family == ReactionMechanismFamily.RADICAL_CYCLIZATION) {
      return ReactionMechanismClass.RADICAL;
    }
    if (family == ReactionMechanismFamily.PHOTOCHEMICAL) {
      return ReactionMechanismClass.PHOTOCHEMICAL;
    }
    if (family == ReactionMechanismFamily.ELECTROCHEMICAL) {
      return ReactionMechanismClass.ELECTROCHEMICAL;
    }
    if (family == ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION) {
      return ReactionMechanismClass.HETEROCYCLE_CONSTRUCTION;
    }
    if (family == ReactionMechanismFamily.HETEROCYCLE_FUNCTIONALIZATION) {
      return ReactionMechanismClass.HETEROCYCLE_FUNCTIONALIZATION;
    }
    if (family == ReactionMechanismFamily.MULTICOMPONENT_CONDENSATION
        || family == ReactionMechanismFamily.IMINE_ENAMINE_FORMATION
        || family == ReactionMechanismFamily.ENOLATE_ALDOL
        || family == ReactionMechanismFamily.ESTERIFICATION
        || family == ReactionMechanismFamily.TRANSESTERIFICATION) {
      return ReactionMechanismClass.CONDENSATION;
    }
    return ReactionMechanismClass.UNKNOWN;
  }

  private static ReactionRegioselectivityClass regioselectivityClass(
      final ReactionPredictionResult result, final List<ReactionPredictionEvidence> evidence) {
    if (!result.hasCandidates()) {
      return ReactionRegioselectivityClass.UNKNOWN;
    }
    if (result.candidates().size() > 1) {
      final double gap =
          Math.max(
              0.0,
              result.candidates().get(0).probability() - result.candidates().get(1).probability());
      if (gap < 0.20) {
        return ReactionRegioselectivityClass.COMPETING_REGIOISOMERS;
      }
      if (result.request().conditions() != null
          && !result.request().conditions().empty()
          && gap < 0.35) {
        return ReactionRegioselectivityClass.CONDITION_CONTROLLED;
      }
    }
    if (OrganicReactionPathwayAnalyzer.hasEvidenceDescription(evidence, "leaving group")
        || OrganicReactionPathwayAnalyzer.hasEvidenceDescription(evidence, "chloride")
        || OrganicReactionPathwayAnalyzer.hasEvidenceDescription(evidence, "halogenated")) {
      return ReactionRegioselectivityClass.LEAVING_GROUP_DIRECTED;
    }
    if (OrganicReactionPathwayAnalyzer.hasEvidence(
        evidence, ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN)) {
      return ReactionRegioselectivityClass.HETEROATOM_DIRECTED;
    }
    if (OrganicReactionPathwayAnalyzer.hasEvidence(
        evidence, ReactionPredictionEvidenceKind.STERIC_FACTOR)) {
      return ReactionRegioselectivityClass.STERICALLY_CONTROLLED;
    }
    if (OrganicReactionPathwayAnalyzer.hasEvidence(
        evidence, ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR)) {
      return ReactionRegioselectivityClass.ELECTRONICALLY_CONTROLLED;
    }
    if (result.bestCandidate().hasSelectivityNote()) {
      return ReactionRegioselectivityClass.SINGLE_DOMINANT_SITE;
    }
    return ReactionRegioselectivityClass.NOT_APPLICABLE;
  }

  private static ReactionConditionWindow conditionWindow(final ReactionPredictionResult result) {
    final ReactionConditions conditions = result.request().conditions();
    if (conditions == null || conditions.empty()) {
      return ReactionConditionWindow.of(
          DEFAULT_MINIMUM_TEMPERATURE_KELVIN,
          DEFAULT_MAXIMUM_TEMPERATURE_KELVIN,
          DEFAULT_MINIMUM_DURATION_HOURS,
          DEFAULT_MAXIMUM_DURATION_HOURS,
          List.of(),
          "No explicit conditions; broad organic screening window is assumed.");
    }
    final ReactionMechanismFamily family =
        OrganicReactionPathwayAnalyzer.dominantMechanismFamily(result);
    final double centerTemperature =
        conditions.temperature() == null ? 298.15 : conditions.temperature().kelvin();
    final double spread =
        OrganicReactionPathwayAnalyzer.temperatureSpreadKelvin(conditions, family);
    final double durationHours = conditions.hasDuration() ? conditions.duration().hours() : 24.0;
    final double durationMinimum =
        Math.max(
            DEFAULT_MINIMUM_DURATION_HOURS,
            durationHours * OrganicReactionPathwayAnalyzer.durationMinimumFactor(family));
    final double durationMaximum =
        Math.min(
            DEFAULT_MAXIMUM_DURATION_HOURS,
            Math.max(
                durationHours * OrganicReactionPathwayAnalyzer.durationMaximumFactor(family),
                durationHours + 1.0));
    return ReactionConditionWindow.of(
        Math.max(0.0, centerTemperature - spread),
        Math.min(DEFAULT_MAXIMUM_TEMPERATURE_KELVIN, centerTemperature + spread),
        durationMinimum,
        durationMaximum,
        OrganicReactionPathwayAnalyzer.conditionSignals(conditions, family),
        OrganicReactionPathwayAnalyzer.conditionWindowNote(family));
  }

  private static double temperatureSpreadKelvin(
      final ReactionConditions conditions, final ReactionMechanismFamily family) {
    if (family == ReactionMechanismFamily.OZONOLYSIS
        || family == ReactionMechanismFamily.ONE_THREE_DIPOLAR_CYCLOADDITION) {
      return 15.0;
    }
    if (family == ReactionMechanismFamily.SUZUKI_COUPLING
        || family == ReactionMechanismFamily.HECK_COUPLING
        || family == ReactionMechanismFamily.SONOGASHIRA_COUPLING
        || family == ReactionMechanismFamily.NEGISHI_COUPLING
        || family == ReactionMechanismFamily.STILLE_COUPLING
        || family == ReactionMechanismFamily.BUCHWALD_HARTWIG_AMINATION) {
      return 35.0;
    }
    if (family == ReactionMechanismFamily.DIELS_ALDER
        || family == ReactionMechanismFamily.CLAISEN_REARRANGEMENT
        || family == ReactionMechanismFamily.COPE_REARRANGEMENT) {
      return 55.0;
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.COOLING)) {
      return 20.0;
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.REFLUX)
        || conditions.hasFeature(ReactionConditionFeatureKind.HEATING)
        || conditions.hasFeature(ReactionConditionFeatureKind.SEALED_VESSEL)) {
      return 45.0;
    }
    return 25.0;
  }

  private static double durationMinimumFactor(final ReactionMechanismFamily family) {
    if (family == ReactionMechanismFamily.OZONOLYSIS
        || family == ReactionMechanismFamily.NITRATION
        || family == ReactionMechanismFamily.FRIEDEL_CRAFTS_ACYLATION) {
      return 0.10;
    }
    if (family == ReactionMechanismFamily.DIELS_ALDER
        || family == ReactionMechanismFamily.CLAISEN_REARRANGEMENT
        || family == ReactionMechanismFamily.COPE_REARRANGEMENT) {
      return 0.50;
    }
    return 0.25;
  }

  private static double durationMaximumFactor(final ReactionMechanismFamily family) {
    if (family == ReactionMechanismFamily.OZONOLYSIS
        || family == ReactionMechanismFamily.NITRATION
        || family == ReactionMechanismFamily.FRIEDEL_CRAFTS_ACYLATION) {
      return 2.0;
    }
    if (family == ReactionMechanismFamily.DIELS_ALDER
        || family == ReactionMechanismFamily.CLAISEN_REARRANGEMENT
        || family == ReactionMechanismFamily.COPE_REARRANGEMENT) {
      return 8.0;
    }
    return 4.0;
  }

  private static List<String> conditionSignals(
      final ReactionConditions conditions, final ReactionMechanismFamily family) {
    final ArrayList<String> signals = new ArrayList<String>();
    if (family != ReactionMechanismFamily.UNKNOWN) {
      signals.add("mechanism:" + family.name());
      OrganicReactionPathwayAnalyzer.addMechanismConditionSignals(signals, family);
    }
    final List<ReactionConditionFeature> features = conditions.features();
    for (int i = 0; i < features.size(); ++i) {
      signals.add("feature:" + features.get(i).kind().name());
    }
    final List<ReactionConditionComponent> components = conditions.components();
    for (int i = 0; i < components.size(); ++i) {
      final ReactionConditionComponent component = components.get(i);
      signals.add("component:" + component.role().name() + ":" + component.name());
    }
    return List.copyOf(signals);
  }

  private static void addMechanismConditionSignals(
      final ArrayList<String> signals, final ReactionMechanismFamily family) {
    if (family == ReactionMechanismFamily.SUZUKI_COUPLING
        || family == ReactionMechanismFamily.HECK_COUPLING
        || family == ReactionMechanismFamily.SONOGASHIRA_COUPLING
        || family == ReactionMechanismFamily.NEGISHI_COUPLING
        || family == ReactionMechanismFamily.STILLE_COUPLING
        || family == ReactionMechanismFamily.BUCHWALD_HARTWIG_AMINATION) {
      signals.add("requires:catalyst");
      signals.add("requires:ligand-or-active-catalyst");
      signals.add("requires:oxygen-control");
      return;
    }
    if (family == ReactionMechanismFamily.OZONOLYSIS) {
      signals.add("requires:oxidant");
      signals.add("requires:temperature-control");
      signals.add("requires:reductive-or-oxidative-workup");
      return;
    }
    if (family == ReactionMechanismFamily.NITRATION
        || family == ReactionMechanismFamily.SULFONATION
        || family == ReactionMechanismFamily.FRIEDEL_CRAFTS_ALKYLATION
        || family == ReactionMechanismFamily.FRIEDEL_CRAFTS_ACYLATION) {
      signals.add("requires:electrophile-generation");
      signals.add("requires:acid-or-lewis-acid-control");
      signals.add("requires:exotherm-control");
      return;
    }
    if (family == ReactionMechanismFamily.DIELS_ALDER
        || family == ReactionMechanismFamily.ONE_THREE_DIPOLAR_CYCLOADDITION) {
      signals.add("requires:dipole-or-dienophile-match");
      signals.add("requires:stereochemical-pathway-control");
      return;
    }
    if (family == ReactionMechanismFamily.PHOTOCHEMICAL) {
      signals.add("requires:light-source");
      signals.add("requires:wavelength-control");
      return;
    }
    if (family == ReactionMechanismFamily.ELECTROCHEMICAL) {
      signals.add("requires:electrode-potential-control");
      signals.add("requires:electrolyte");
    }
  }

  private static String conditionWindowNote(final ReactionMechanismFamily family) {
    if (family == ReactionMechanismFamily.UNKNOWN) {
      return "Window inferred from explicit reaction conditions and activation features.";
    }
    return "Window inferred from explicit reaction conditions and named mechanism requirements: "
        + family.name()
        + ".";
  }

  private static ReactionMechanismFamily dominantMechanismFamily(
      final ReactionPredictionResult result) {
    if (!result.hasCandidates() || !result.bestCandidate().hasPredictedMechanisms()) {
      return ReactionMechanismFamily.UNKNOWN;
    }
    return result.bestCandidate().predictedMechanisms().get(0).family();
  }

  private static boolean hasCatalystCouplingContext(
      final ReactionConditions conditions, final List<ReactionPredictionEvidence> evidence) {
    if (OrganicReactionPathwayAnalyzer.hasEvidence(
            evidence, ReactionPredictionEvidenceKind.CATALYST_EFFECT)
        && conditions != null
        && (!conditions.componentsByRole(ReactionComponentRole.CATALYST).isEmpty()
            || !conditions.componentsByRole(ReactionComponentRole.LIGAND).isEmpty())) {
      return true;
    }
    return false;
  }

  private static boolean hasNucleophilicAromaticSubstitutionContext(
      final ReactionPredictionResult result, final List<ReactionPredictionEvidence> evidence) {
    if (!result.request().hasDomain(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION)
        && !OrganicReactionPathwayAnalyzer.hasEvidenceDescription(evidence, "snar")
        && !OrganicReactionPathwayAnalyzer.hasEvidenceDescription(
            evidence, "nucleophilic aromatic substitution")) {
      return false;
    }
    return OrganicReactionPathwayAnalyzer.hasEvidenceDescription(evidence, "halogenated")
        || OrganicReactionPathwayAnalyzer.hasEvidenceDescription(evidence, "chloride")
        || OrganicReactionPathwayAnalyzer.hasEvidenceDescription(evidence, "leaving group")
        || OrganicReactionPathwayAnalyzer.hasConditionFeature(
            result.request().conditions(), ReactionConditionFeatureKind.BASIC_MEDIUM);
  }

  private static boolean hasElectrophilicAromaticSubstitutionContext(
      final ReactionPredictionResult result, final List<ReactionPredictionEvidence> evidence) {
    if (result.request().hasDomain(OrganicReactionDomain.ELECTROPHILIC_SUBSTITUTION)) {
      return true;
    }
    return OrganicReactionPathwayAnalyzer.hasEvidenceDescription(evidence, "electrophilic")
        || OrganicReactionPathwayAnalyzer.hasConditionFeature(
            result.request().conditions(), ReactionConditionFeatureKind.ACIDIC_MEDIUM);
  }

  private static boolean hasOxidationContext(
      final ReactionPredictionResult result, final List<ReactionPredictionEvidence> evidence) {
    return result.request().hasDomain(OrganicReactionDomain.OXIDATION_REDUCTION)
        && (OrganicReactionPathwayAnalyzer.hasEvidenceDescription(evidence, "oxidation")
            || OrganicReactionPathwayAnalyzer.hasEvidenceDescription(evidence, "s-oxide")
            || OrganicReactionPathwayAnalyzer.hasEvidenceDescription(evidence, "sulfur"));
  }

  private static boolean hasReductionContext(
      final ReactionPredictionResult result, final List<ReactionPredictionEvidence> evidence) {
    return result.request().hasDomain(OrganicReactionDomain.OXIDATION_REDUCTION)
        && (OrganicReactionPathwayAnalyzer.hasEvidenceDescription(evidence, "reduction")
            || OrganicReactionPathwayAnalyzer.hasEvidenceDescription(evidence, "hydrogenation"));
  }

  private static boolean hasConditionFeature(
      final ReactionConditions conditions, final ReactionConditionFeatureKind feature) {
    return conditions != null && conditions.hasFeature(feature);
  }

  private static boolean hasEvidence(
      final List<ReactionPredictionEvidence> evidence, final ReactionPredictionEvidenceKind kind) {
    for (int i = 0; i < evidence.size(); ++i) {
      if (evidence.get(i).kind() == kind) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasEvidenceDescription(
      final List<ReactionPredictionEvidence> evidence, final String text) {
    final String checkedText = text.toLowerCase(Locale.ROOT);
    for (int i = 0; i < evidence.size(); ++i) {
      if (evidence.get(i).description().toLowerCase(Locale.ROOT).contains(checkedText)) {
        return true;
      }
    }
    return false;
  }

  private static List<ReactionPredictionEvidence> checkedEvidence(
      final List<ReactionPredictionEvidence> evidence) {
    if (evidence == null || evidence.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < evidence.size(); ++i) {
      if (evidence.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException("Reaction pathway evidence must not contain null value.");
    }
    return List.copyOf(evidence);
  }

  private static List<ReactionPredictionEvidence> limitedBySign(
      final List<ReactionPredictionEvidence> evidence, final boolean positive) {
    final ArrayList<ReactionPredictionEvidence> selected =
        new ArrayList<ReactionPredictionEvidence>();
    for (int i = 0; i < evidence.size(); ++i) {
      final ReactionPredictionEvidence item = evidence.get(i);
      if (positive && item.weight() <= 0.0) {
        continue;
      }
      if (!positive && item.weight() >= 0.0) {
        continue;
      }
      selected.add(item);
    }
    selected.sort(
        Comparator.comparingDouble(
                (final ReactionPredictionEvidence item) -> Math.abs(item.weight()))
            .reversed());
    if (selected.size() <= MAX_PATHWAY_EVIDENCE) {
      return List.copyOf(selected);
    }
    return List.copyOf(selected.subList(0, MAX_PATHWAY_EVIDENCE));
  }
}

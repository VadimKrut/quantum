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
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffectProfile;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.feature.MolecularFeatureAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureSet;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism.ReactionMechanismPattern;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism.ReactionMechanismPatternCatalog;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.prediction.ReactionPredictionApplicabilityReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.prediction.ReactionPredictionReadinessLevel;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.BuiltInReactionMechanismPatternCatalog;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.PredictedReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionScoreComponentKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;

/**
 * Строит прикладной аудит применимости прогноза реакции без обращения к таблице известных исходов.
 */
public final class ReactionPredictionApplicabilityAnalyzer {

  private static final double COMPLETE_PROBABILITY_TOLERANCE = 0.05;
  private static final double DOMINANT_CHANNEL_THRESHOLD = 0.50;
  private static final double CLOSE_COMPETITOR_GAP = 0.12;

  private final MolecularFeatureAnalyzer featureAnalyzer;
  private final ReactionMechanismPatternCatalog mechanismCatalog;

  public ReactionPredictionApplicabilityAnalyzer() {
    this(
        new MolecularFeatureAnalyzer(),
        BuiltInReactionMechanismPatternCatalog.organicAndPhysicalChemistry());
  }

  public ReactionPredictionApplicabilityAnalyzer(final MolecularFeatureAnalyzer featureAnalyzer) {
    this(featureAnalyzer, BuiltInReactionMechanismPatternCatalog.organicAndPhysicalChemistry());
  }

  public ReactionPredictionApplicabilityAnalyzer(
      final MolecularFeatureAnalyzer featureAnalyzer,
      final ReactionMechanismPatternCatalog mechanismCatalog) {
    if (featureAnalyzer == null) {
      throw new IllegalArgumentException(
          "Reaction applicability feature analyzer must not be null.");
    }
    if (mechanismCatalog == null) {
      throw new IllegalArgumentException(
          "Reaction applicability mechanism catalog must not be null.");
    }
    this.featureAnalyzer = featureAnalyzer;
    this.mechanismCatalog = mechanismCatalog;
  }

  public ReactionPredictionApplicabilityReport analyze(
      final ReactionPredictionResult result,
      final ReactionConditionEffectProfile conditionEffectProfile) {
    if (result == null) {
      throw new IllegalArgumentException("Reaction applicability result must not be null.");
    }
    if (conditionEffectProfile == null) {
      throw new IllegalArgumentException(
          "Reaction applicability condition profile must not be null.");
    }
    final ReactantApplicabilityProfile reactants = this.reactants(result.request().reactants());
    final CandidateApplicabilityProfile candidates = this.candidates(result);
    final ConditionApplicabilityProfile conditions =
        ReactionPredictionApplicabilityAnalyzer.conditions(result, conditionEffectProfile);
    final ArrayList<String> rows = new ArrayList<String>();
    ReactionPredictionApplicabilityAnalyzer.addReactantRows(rows, reactants);
    ReactionPredictionApplicabilityAnalyzer.addCandidateRows(rows, result, candidates);
    ReactionPredictionApplicabilityAnalyzer.addConditionRows(rows, conditions);
    final ReactionPredictionReadinessLevel readiness =
        ReactionPredictionApplicabilityAnalyzer.readiness(reactants, candidates, conditions);
    final double score =
        ReactionPredictionApplicabilityAnalyzer.score(reactants, candidates, conditions, readiness);
    return ReactionPredictionApplicabilityReport.of(
        readiness,
        score,
        reactants.quantumFollowupRecommended(),
        reactants.electronicRerankRecommended(),
        candidates.closeCompetitorCount(),
        conditions.missingCriticalConditionCount(),
        candidates.productionDetailedMechanismCount(),
        candidates.chemicallyUsableMechanismCount(),
        candidates.genericFallbackMechanismCount(),
        candidates.unknownMechanismCount(),
        rows);
  }

  private ReactantApplicabilityProfile reactants(final ReactionSide reactants) {
    long heterocycleCount = 0L;
    long halogenatedCount = 0L;
    long radicalCenterCount = 0L;
    long aromaticCount = 0L;
    long nucleophileHandleCount = 0L;
    long activatedHeteroarylHalideCount = 0L;
    long electronPoorHeteroareneCount = 0L;
    long amineNucleophileCount = 0L;
    long benzylicFragmentCount = 0L;
    long crossCouplingHandleCount = 0L;
    long activatedAlkeneCount = 0L;
    long alphaCarbonylCHCount = 0L;
    long multicomponentHandleCount = 0L;
    final List<ReactionParticipant> participants = reactants.participants();
    for (int i = 0; i < participants.size(); ++i) {
      final MolecularFeatureSet features =
          this.featureAnalyzer.analyze(participants.get(i).molecule());
      heterocycleCount +=
          features.countOf(MolecularFeatureKind.HETEROCYCLE)
              + features.countOf(MolecularFeatureKind.NITROGEN_HETEROCYCLE)
              + features.countOf(MolecularFeatureKind.OXYGEN_HETEROCYCLE)
              + features.countOf(MolecularFeatureKind.SULFUR_HETEROCYCLE);
      halogenatedCount += features.countOf(MolecularFeatureKind.HALOGENATED);
      radicalCenterCount += features.countOf(MolecularFeatureKind.RADICAL_CENTER);
      aromaticCount += features.countOf(MolecularFeatureKind.AROMATIC_RING);
      nucleophileHandleCount +=
          features.countOf(MolecularFeatureKind.AMINE)
              + features.countOf(MolecularFeatureKind.HYDROXYL_OR_ALKOXY)
              + features.countOf(MolecularFeatureKind.THIOL_OR_THIOETHER);
      activatedHeteroarylHalideCount += features.countOf(MolecularFeatureKind.HETEROARYL_HALIDE);
      electronPoorHeteroareneCount +=
          features.countOf(MolecularFeatureKind.ELECTRON_POOR_HETEROARENE);
      amineNucleophileCount +=
          features.countOf(MolecularFeatureKind.PRIMARY_OR_SECONDARY_AMINE)
              + features.countOf(MolecularFeatureKind.ACTIVATED_NUCLEOPHILE);
      benzylicFragmentCount += features.countOf(MolecularFeatureKind.BENZYL_OR_BENZYLIC_FRAGMENT);
      crossCouplingHandleCount +=
          features.countOf(MolecularFeatureKind.BORONIC_ACID_OR_BORONATE)
              + features.countOf(MolecularFeatureKind.ORGANOMETALLIC_REAGENT)
              + features.countOf(MolecularFeatureKind.GOOD_LEAVING_GROUP);
      activatedAlkeneCount += features.countOf(MolecularFeatureKind.ACTIVATED_ALKENE);
      alphaCarbonylCHCount += features.countOf(MolecularFeatureKind.ALPHA_CARBONYL_C_H);
      multicomponentHandleCount +=
          features.countOf(MolecularFeatureKind.ISOCYANIDE)
              + features.countOf(MolecularFeatureKind.ISOCYANATE);
    }
    return new ReactantApplicabilityProfile(
        participants.size(),
        heterocycleCount,
        halogenatedCount,
        radicalCenterCount,
        aromaticCount,
        nucleophileHandleCount,
        activatedHeteroarylHalideCount,
        electronPoorHeteroareneCount,
        amineNucleophileCount,
        benzylicFragmentCount,
        crossCouplingHandleCount,
        activatedAlkeneCount,
        alphaCarbonylCHCount,
        multicomponentHandleCount);
  }

  private CandidateApplicabilityProfile candidates(final ReactionPredictionResult result) {
    int mechanismCount = 0;
    int productionDetailedMechanismCount = 0;
    int chemicallyUsableMechanismCount = 0;
    int genericFallbackMechanismCount = 0;
    int unknownMechanismCount = 0;
    int scoreBreakdownCount = 0;
    int stoichiometryBreakdownCount = 0;
    int quantumEvidenceCount = 0;
    int electronicEvidenceCount = 0;
    int competingEvidenceCount = 0;
    int riskCandidateCount = 0;
    int closeCompetitorCount = 0;
    double bestProbability = 0.0;
    double secondProbability = 0.0;
    final List<ReactionProductCandidate> candidates = result.candidates();
    for (int i = 0; i < candidates.size(); ++i) {
      final ReactionProductCandidate candidate = candidates.get(i);
      if (candidate.hasPredictedMechanisms()) {
        ++mechanismCount;
      }
      final MechanismQualityCounts mechanismQualityCounts =
          this.mechanismQualityCounts(candidate.predictedMechanisms());
      productionDetailedMechanismCount += mechanismQualityCounts.productionDetailed;
      chemicallyUsableMechanismCount += mechanismQualityCounts.chemicallyUsable;
      genericFallbackMechanismCount += mechanismQualityCounts.genericFallback;
      unknownMechanismCount += mechanismQualityCounts.unknown;
      if (candidate.hasScoreBreakdown()) {
        ++scoreBreakdownCount;
        if (candidate
            .scoreBreakdown()
            .hasComponent(ReactionPredictionScoreComponentKind.STOICHIOMETRY)) {
          ++stoichiometryBreakdownCount;
        }
      }
      if (candidate.hasEvidence(ReactionPredictionEvidenceKind.QUANTUM_ENERGY_MODEL)) {
        ++quantumEvidenceCount;
      }
      if (candidate.hasEvidence(ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR)) {
        ++electronicEvidenceCount;
      }
      if (candidate.hasEvidence(ReactionPredictionEvidenceKind.COMPETING_PATHWAY)) {
        ++competingEvidenceCount;
      }
      if (candidate.evidenceWeight(ReactionPredictionEvidenceKind.COMPETING_PATHWAY) < 0.0
          || candidate.evidenceWeight(ReactionPredictionEvidenceKind.WARNING) < 0.0) {
        ++riskCandidateCount;
      }
      if (candidate.probability() > bestProbability) {
        secondProbability = bestProbability;
        bestProbability = candidate.probability();
      } else if (candidate.probability() > secondProbability) {
        secondProbability = candidate.probability();
      }
    }
    for (int i = 0; i < candidates.size(); ++i) {
      final double probability = candidates.get(i).probability();
      if (probability < bestProbability && bestProbability - probability <= CLOSE_COMPETITOR_GAP) {
        ++closeCompetitorCount;
      }
    }
    return new CandidateApplicabilityProfile(
        candidates.size(),
        mechanismCount,
        productionDetailedMechanismCount,
        chemicallyUsableMechanismCount,
        genericFallbackMechanismCount,
        unknownMechanismCount,
        scoreBreakdownCount,
        stoichiometryBreakdownCount,
        quantumEvidenceCount,
        electronicEvidenceCount,
        competingEvidenceCount,
        riskCandidateCount,
        closeCompetitorCount,
        bestProbability,
        secondProbability,
        result.probabilityMass());
  }

  private MechanismQualityCounts mechanismQualityCounts(
      final List<PredictedReactionMechanism> mechanisms) {
    int productionDetailed = 0;
    int chemicallyUsable = 0;
    int genericFallback = 0;
    int unknown = 0;
    for (int i = 0; i < mechanisms.size(); ++i) {
      final ReactionMechanismPattern pattern =
          this.mechanismCatalog.find(mechanisms.get(i).family());
      if (pattern == null) {
        ++unknown;
      } else if (pattern.productionDetailedPattern()) {
        ++productionDetailed;
      } else if (pattern.chemicallyUsablePattern()) {
        ++chemicallyUsable;
      } else if (pattern.syntheticGenericPattern()) {
        ++genericFallback;
      } else {
        ++unknown;
      }
    }
    return new MechanismQualityCounts(
        productionDetailed, chemicallyUsable, genericFallback, unknown);
  }

  private static ConditionApplicabilityProfile conditions(
      final ReactionPredictionResult result,
      final ReactionConditionEffectProfile conditionEffectProfile) {
    int missingCriticalConditionCount = 0;
    if (result.request().conditions().temperature() == null) {
      ++missingCriticalConditionCount;
    }
    if (!result.request().conditions().hasDuration()) {
      ++missingCriticalConditionCount;
    }
    if (result.request().conditions().componentsByRole(ReactionComponentRole.SOLVENT).isEmpty()) {
      ++missingCriticalConditionCount;
    }
    final boolean basicOrCatalytic =
        result.request().conditions().hasFeature(ReactionConditionFeatureKind.BASIC_MEDIUM)
            || result
                .request()
                .conditions()
                .hasFeature(ReactionConditionFeatureKind.CATALYST_PRESENT)
            || !result.request().conditions().componentsByRole(ReactionComponentRole.BASE).isEmpty()
            || !result
                .request()
                .conditions()
                .componentsByRole(ReactionComponentRole.CATALYST)
                .isEmpty();
    if (!basicOrCatalytic) {
      ++missingCriticalConditionCount;
    }
    return new ConditionApplicabilityProfile(
        conditionEffectProfile.effects().size(),
        conditionEffectProfile.supportCount(),
        conditionEffectProfile.riskCount(),
        conditionEffectProfile.scoreAdjustment(),
        missingCriticalConditionCount);
  }

  private static ReactionPredictionReadinessLevel readiness(
      final ReactantApplicabilityProfile reactants,
      final CandidateApplicabilityProfile candidates,
      final ConditionApplicabilityProfile conditions) {
    if (candidates.candidateCount() == 0) {
      return ReactionPredictionReadinessLevel.NO_PRODUCT_HYPOTHESIS;
    }
    if (conditions.missingCriticalConditionCount() >= 3) {
      return ReactionPredictionReadinessLevel.CONDITION_INCOMPLETE;
    }
    if (candidates.bestProbability() < DOMINANT_CHANNEL_THRESHOLD
        || candidates.closeCompetitorCount() > 0
        || candidates.riskCandidateCount() > 0
        || candidates.genericFallbackMechanismCount()
            > candidates.productionDetailedMechanismCount()) {
      return ReactionPredictionReadinessLevel.COMPETING_PATHWAYS_REQUIRE_RERANK;
    }
    if (reactants.quantumFollowupRecommended() && candidates.quantumEvidenceCount() == 0) {
      return ReactionPredictionReadinessLevel.QUANTUM_OR_ELECTRONIC_RERANK_RECOMMENDED;
    }
    if (Math.abs(1.0 - candidates.probabilityMass()) > COMPLETE_PROBABILITY_TOLERANCE) {
      return ReactionPredictionReadinessLevel.PROBABILITY_MASS_INCOMPLETE;
    }
    return ReactionPredictionReadinessLevel.READY_FOR_DECISION_SUPPORT;
  }

  private static double score(
      final ReactantApplicabilityProfile reactants,
      final CandidateApplicabilityProfile candidates,
      final ConditionApplicabilityProfile conditions,
      final ReactionPredictionReadinessLevel readiness) {
    double score = 0.50;
    score += Math.min(0.14, 0.02 * candidates.mechanismCount());
    score += Math.min(0.08, 0.025 * candidates.productionDetailedMechanismCount());
    score += Math.min(0.04, 0.010 * candidates.chemicallyUsableMechanismCount());
    score -= Math.min(0.10, 0.020 * candidates.genericFallbackMechanismCount());
    score -= Math.min(0.08, 0.020 * candidates.unknownMechanismCount());
    score += Math.min(0.10, 0.015 * candidates.scoreBreakdownCount());
    score += Math.min(0.06, 0.020 * candidates.stoichiometryBreakdownCount());
    score += Math.min(0.08, 0.025 * reactants.activatedHeteroarylHalideCount());
    score += Math.min(0.06, 0.02 * reactants.electronPoorHeteroareneCount());
    score += Math.min(0.05, 0.015 * reactants.amineNucleophileCount());
    score += Math.min(0.05, 0.015 * reactants.crossCouplingHandleCount());
    score += Math.min(0.05, 0.015 * reactants.activatedAlkeneCount());
    score += Math.min(0.04, 0.012 * reactants.alphaCarbonylCHCount());
    score += Math.min(0.04, 0.012 * reactants.multicomponentHandleCount());
    score += Math.min(0.10, Math.max(0.0, conditions.scoreAdjustment()));
    score -= 0.08 * conditions.missingCriticalConditionCount();
    score -= 0.04 * candidates.closeCompetitorCount();
    score -= 0.04 * candidates.riskCandidateCount();
    if (reactants.quantumFollowupRecommended() && candidates.quantumEvidenceCount() == 0) {
      score -= 0.08;
    }
    if (readiness == ReactionPredictionReadinessLevel.READY_FOR_DECISION_SUPPORT) {
      score += 0.08;
    }
    return Math.max(0.0, Math.min(1.0, score));
  }

  private static void addReactantRows(
      final ArrayList<String> rows, final ReactantApplicabilityProfile profile) {
    rows.add("category\tmetric\tvalue\tmeaning");
    rows.add(
        "reactants\tparticipant_count\t"
            + profile.participantCount()
            + "\tNumber of molecular participants used by graph-based prediction.");
    rows.add(
        "reactants\theterocycle_feature_count\t"
            + profile.heterocycleCount()
            + "\tHeterocycle-sensitive mechanisms and electronic rerank become more important.");
    rows.add(
        "reactants\thalogenated_feature_count\t"
            + profile.halogenatedCount()
            + "\tLeaving-group substitution/coupling pathways are available.");
    rows.add(
        "reactants\tnucleophile_handle_count\t"
            + profile.nucleophileHandleCount()
            + "\tN/O/S attack channels can compete.");
    rows.add(
        "reactants\tactivated_heteroaryl_halide_count\t"
            + profile.activatedHeteroarylHalideCount()
            + "\tActivated aryl/heteroaryl leaving-group sites support SNAr, ANRORC, amination, coupling or hydrodehalogenation channels.");
    rows.add(
        "reactants\telectron_poor_heteroarene_count\t"
            + profile.electronPoorHeteroareneCount()
            + "\tElectron-poor heteroarene context increases sigma-adduct and regioselectivity sensitivity.");
    rows.add(
        "reactants\tamine_nucleophile_count\t"
            + profile.amineNucleophileCount()
            + "\tPrimary/secondary amine handles support C-N bond formation routes.");
    rows.add(
        "reactants\tbenzylic_fragment_count\t"
            + profile.benzylicFragmentCount()
            + "\tBenzylic fragments add steric/electronic selectivity context for amination and substitution.");
    rows.add(
        "reactants\tcross_coupling_handle_count\t"
            + profile.crossCouplingHandleCount()
            + "\tOrganoboron, organometallic, and leaving-group handles support cross-coupling and metal-mediated pathways.");
    rows.add(
        "reactants\tactivated_alkene_count\t"
            + profile.activatedAlkeneCount()
            + "\tActivated alkenes support Michael, radical, hydrofunctionalization, and cycloaddition competition.");
    rows.add(
        "reactants\talpha_carbonyl_c_h_count\t"
            + profile.alphaCarbonylCHCount()
            + "\tAlpha-carbonyl C-H handles support enolate, aldol, Mannich, Knoevenagel, and Claisen-like channels.");
    rows.add(
        "reactants\tmulticomponent_handle_count\t"
            + profile.multicomponentHandleCount()
            + "\tIsocyanide/isocyanate handles support Ugi, Passerini, and related multicomponent channels.");
    rows.add(
        "reactants\tradical_center_count\t"
            + profile.radicalCenterCount()
            + "\tOpen-shell treatment or radical pathway audit is recommended.");
  }

  private static void addCandidateRows(
      final ArrayList<String> rows,
      final ReactionPredictionResult result,
      final CandidateApplicabilityProfile profile) {
    rows.add(
        "candidates\tcandidate_count\t"
            + profile.candidateCount()
            + "\tGenerated product hypotheses after fallback-surrogate filtering.");
    rows.add(
        "candidates\tprobability_mass\t"
            + result.probabilityMass()
            + "\tTotal normalized product probability represented by candidates.");
    rows.add(
        "candidates\tbest_probability\t"
            + profile.bestProbability()
            + "\tDominant product channel probability.");
    rows.add(
        "candidates\tsecond_probability\t"
            + profile.secondProbability()
            + "\tClosest competitor channel probability.");
    rows.add(
        "candidates\tclose_competitor_count\t"
            + profile.closeCompetitorCount()
            + "\tAdditional channels within the selectivity gap.");
    rows.add(
        "candidates\tmechanism_backed_candidate_count\t"
            + profile.mechanismCount()
            + "\tCandidates carrying explicit stepwise predicted mechanisms.");
    rows.add(
        "candidates\tproduction_detailed_mechanism_count\t"
            + profile.productionDetailedMechanismCount()
            + "\tMechanism links backed by detailed staged mechanism patterns.");
    rows.add(
        "candidates\tchemically_usable_mechanism_count\t"
            + profile.chemicallyUsableMechanismCount()
            + "\tMechanism links backed by usable non-generic staged patterns.");
    rows.add(
        "candidates\tgeneric_fallback_mechanism_count\t"
            + profile.genericFallbackMechanismCount()
            + "\tMechanism links still relying on taxonomy fallback patterns.");
    rows.add(
        "candidates\tunknown_mechanism_count\t"
            + profile.unknownMechanismCount()
            + "\tMechanism links with no registered pattern in the current catalog.");
    rows.add(
        "candidates\tstoichiometry_backed_candidate_count\t"
            + profile.stoichiometryBreakdownCount()
            + "\tCandidates whose score breakdown explicitly includes structured mass/equivalent recipe support.");
    rows.add(
        "candidates\tquantum_energy_evidence_count\t"
            + profile.quantumEvidenceCount()
            + "\tCandidates already reranked by quantum/electronic energy evidence.");
  }

  private static void addConditionRows(
      final ArrayList<String> rows, final ConditionApplicabilityProfile profile) {
    rows.add(
        "conditions\tcondition_effect_count\t"
            + profile.effectCount()
            + "\tRecognized mechanistic condition effects.");
    rows.add(
        "conditions\tcondition_support_count\t"
            + profile.supportCount()
            + "\tCondition signals supporting at least one mechanistic route.");
    rows.add(
        "conditions\tcondition_risk_count\t"
            + profile.riskCount()
            + "\tCondition signals broadening distribution or lowering reliability.");
    rows.add(
        "conditions\tmissing_critical_condition_count\t"
            + profile.missingCriticalConditionCount()
            + "\tMissing temperature, time, solvent, catalyst/base context.");
    rows.add(
        "conditions\tcondition_score_adjustment\t"
            + profile.scoreAdjustment()
            + "\tNet condition contribution to prediction confidence.");
  }

  private static final class ReactantApplicabilityProfile {

    private final int participantCount;
    private final long heterocycleCount;
    private final long halogenatedCount;
    private final long radicalCenterCount;
    private final long aromaticCount;
    private final long nucleophileHandleCount;
    private final long activatedHeteroarylHalideCount;
    private final long electronPoorHeteroareneCount;
    private final long amineNucleophileCount;
    private final long benzylicFragmentCount;
    private final long crossCouplingHandleCount;
    private final long activatedAlkeneCount;
    private final long alphaCarbonylCHCount;
    private final long multicomponentHandleCount;

    private ReactantApplicabilityProfile(
        final int participantCount,
        final long heterocycleCount,
        final long halogenatedCount,
        final long radicalCenterCount,
        final long aromaticCount,
        final long nucleophileHandleCount,
        final long activatedHeteroarylHalideCount,
        final long electronPoorHeteroareneCount,
        final long amineNucleophileCount,
        final long benzylicFragmentCount,
        final long crossCouplingHandleCount,
        final long activatedAlkeneCount,
        final long alphaCarbonylCHCount,
        final long multicomponentHandleCount) {
      this.participantCount = participantCount;
      this.heterocycleCount = heterocycleCount;
      this.halogenatedCount = halogenatedCount;
      this.radicalCenterCount = radicalCenterCount;
      this.aromaticCount = aromaticCount;
      this.nucleophileHandleCount = nucleophileHandleCount;
      this.activatedHeteroarylHalideCount = activatedHeteroarylHalideCount;
      this.electronPoorHeteroareneCount = electronPoorHeteroareneCount;
      this.amineNucleophileCount = amineNucleophileCount;
      this.benzylicFragmentCount = benzylicFragmentCount;
      this.crossCouplingHandleCount = crossCouplingHandleCount;
      this.activatedAlkeneCount = activatedAlkeneCount;
      this.alphaCarbonylCHCount = alphaCarbonylCHCount;
      this.multicomponentHandleCount = multicomponentHandleCount;
    }

    private int participantCount() {
      return this.participantCount;
    }

    private long heterocycleCount() {
      return this.heterocycleCount;
    }

    private long halogenatedCount() {
      return this.halogenatedCount;
    }

    private long radicalCenterCount() {
      return this.radicalCenterCount;
    }

    private long nucleophileHandleCount() {
      return this.nucleophileHandleCount;
    }

    private long activatedHeteroarylHalideCount() {
      return this.activatedHeteroarylHalideCount;
    }

    private long electronPoorHeteroareneCount() {
      return this.electronPoorHeteroareneCount;
    }

    private long amineNucleophileCount() {
      return this.amineNucleophileCount;
    }

    private long benzylicFragmentCount() {
      return this.benzylicFragmentCount;
    }

    private long crossCouplingHandleCount() {
      return this.crossCouplingHandleCount;
    }

    private long activatedAlkeneCount() {
      return this.activatedAlkeneCount;
    }

    private long alphaCarbonylCHCount() {
      return this.alphaCarbonylCHCount;
    }

    private long multicomponentHandleCount() {
      return this.multicomponentHandleCount;
    }

    private boolean quantumFollowupRecommended() {
      return this.heterocycleCount > 0L
          || this.radicalCenterCount > 0L
          || this.aromaticCount > 1L
          || this.activatedHeteroarylHalideCount > 0L
          || this.electronPoorHeteroareneCount > 0L
          || this.crossCouplingHandleCount > 0L
          || this.activatedAlkeneCount > 0L
          || this.alphaCarbonylCHCount > 0L
          || this.multicomponentHandleCount > 0L;
    }

    private boolean electronicRerankRecommended() {
      return this.heterocycleCount > 0L
          || this.halogenatedCount > 0L
          || this.nucleophileHandleCount > 1L
          || this.activatedHeteroarylHalideCount > 0L
          || this.amineNucleophileCount > 0L
          || this.benzylicFragmentCount > 0L
          || this.crossCouplingHandleCount > 0L
          || this.activatedAlkeneCount > 0L
          || this.alphaCarbonylCHCount > 0L
          || this.multicomponentHandleCount > 0L;
    }
  }

  private static final class CandidateApplicabilityProfile {

    private final int candidateCount;
    private final int mechanismCount;
    private final int productionDetailedMechanismCount;
    private final int chemicallyUsableMechanismCount;
    private final int genericFallbackMechanismCount;
    private final int unknownMechanismCount;
    private final int scoreBreakdownCount;
    private final int stoichiometryBreakdownCount;
    private final int quantumEvidenceCount;
    private final int electronicEvidenceCount;
    private final int competingEvidenceCount;
    private final int riskCandidateCount;
    private final int closeCompetitorCount;
    private final double bestProbability;
    private final double secondProbability;
    private final double probabilityMass;

    private CandidateApplicabilityProfile(
        final int candidateCount,
        final int mechanismCount,
        final int productionDetailedMechanismCount,
        final int chemicallyUsableMechanismCount,
        final int genericFallbackMechanismCount,
        final int unknownMechanismCount,
        final int scoreBreakdownCount,
        final int stoichiometryBreakdownCount,
        final int quantumEvidenceCount,
        final int electronicEvidenceCount,
        final int competingEvidenceCount,
        final int riskCandidateCount,
        final int closeCompetitorCount,
        final double bestProbability,
        final double secondProbability,
        final double probabilityMass) {
      this.candidateCount = candidateCount;
      this.mechanismCount = mechanismCount;
      this.productionDetailedMechanismCount = productionDetailedMechanismCount;
      this.chemicallyUsableMechanismCount = chemicallyUsableMechanismCount;
      this.genericFallbackMechanismCount = genericFallbackMechanismCount;
      this.unknownMechanismCount = unknownMechanismCount;
      this.scoreBreakdownCount = scoreBreakdownCount;
      this.stoichiometryBreakdownCount = stoichiometryBreakdownCount;
      this.quantumEvidenceCount = quantumEvidenceCount;
      this.electronicEvidenceCount = electronicEvidenceCount;
      this.competingEvidenceCount = competingEvidenceCount;
      this.riskCandidateCount = riskCandidateCount;
      this.closeCompetitorCount = closeCompetitorCount;
      this.bestProbability = bestProbability;
      this.secondProbability = secondProbability;
      this.probabilityMass = probabilityMass;
    }

    private int candidateCount() {
      return this.candidateCount;
    }

    private int mechanismCount() {
      return this.mechanismCount;
    }

    private int productionDetailedMechanismCount() {
      return this.productionDetailedMechanismCount;
    }

    private int chemicallyUsableMechanismCount() {
      return this.chemicallyUsableMechanismCount;
    }

    private int genericFallbackMechanismCount() {
      return this.genericFallbackMechanismCount;
    }

    private int unknownMechanismCount() {
      return this.unknownMechanismCount;
    }

    private int scoreBreakdownCount() {
      return this.scoreBreakdownCount;
    }

    private int stoichiometryBreakdownCount() {
      return this.stoichiometryBreakdownCount;
    }

    private int quantumEvidenceCount() {
      return this.quantumEvidenceCount;
    }

    private int riskCandidateCount() {
      return this.riskCandidateCount;
    }

    private int closeCompetitorCount() {
      return this.closeCompetitorCount;
    }

    private double bestProbability() {
      return this.bestProbability;
    }

    private double secondProbability() {
      return this.secondProbability;
    }

    private double probabilityMass() {
      return this.probabilityMass;
    }
  }

  private static final class ConditionApplicabilityProfile {

    private final int effectCount;
    private final int supportCount;
    private final int riskCount;
    private final double scoreAdjustment;
    private final int missingCriticalConditionCount;

    private ConditionApplicabilityProfile(
        final int effectCount,
        final int supportCount,
        final int riskCount,
        final double scoreAdjustment,
        final int missingCriticalConditionCount) {
      this.effectCount = effectCount;
      this.supportCount = supportCount;
      this.riskCount = riskCount;
      this.scoreAdjustment = scoreAdjustment;
      this.missingCriticalConditionCount = missingCriticalConditionCount;
    }

    private int effectCount() {
      return this.effectCount;
    }

    private int supportCount() {
      return this.supportCount;
    }

    private int riskCount() {
      return this.riskCount;
    }

    private double scoreAdjustment() {
      return this.scoreAdjustment;
    }

    private int missingCriticalConditionCount() {
      return this.missingCriticalConditionCount;
    }
  }

  private static final class MechanismQualityCounts {

    private final int productionDetailed;
    private final int chemicallyUsable;
    private final int genericFallback;
    private final int unknown;

    private MechanismQualityCounts(
        final int productionDetailed,
        final int chemicallyUsable,
        final int genericFallback,
        final int unknown) {
      this.productionDetailed = productionDetailed;
      this.chemicallyUsable = chemicallyUsable;
      this.genericFallback = genericFallback;
      this.unknown = unknown;
    }
  }
}

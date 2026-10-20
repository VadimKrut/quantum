/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.graph.MolecularGraph;
import ru.pathcreator.vadim.quantum.chemistry.domain.graph.MolecularRing;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** Выводит общий реакционный вес из структуры субстратов, условий и продукта, без lookup по реальным исходам. */
public final class ReactionSubstrateReactivityAnalyzer {

  private static final double ACTIVATED_HETEROARYL_HALIDE_SUPPORT = 0.18;
  private static final double NUCLEOPHILE_SUPPORT = 0.13;
  private static final double ELECTRON_WITHDRAWING_SUPPORT = 0.09;
  private static final double CONDITION_SUPPORT = 0.10;
  private static final double PRODUCT_TOPOLOGY_SUPPORT = 0.12;
  private static final double LEAVING_GROUP_IDENTITY_SUPPORT = 0.08;
  private static final double COMPLETE_SNAR_ELECTRONIC_SUPPORT = 0.16;
  private static final double COMPLETE_SNAR_KINETIC_SUPPORT = 0.14;
  private static final double EWG_HETEROARYL_SYNERGY_SUPPORT = 0.075;
  private static final double SITE_ACTIVATION_SUPPORT = 0.11;
  private static final double SIDE_REACTION_PENALTY = -0.06;
  private static final double MINISCI_HETEROCYCLE_SUPPORT = 0.15;
  private static final double RADICAL_CONDITION_SUPPORT = 0.12;
  private static final double RADICAL_BOND_CHANGE_SUPPORT = 0.10;
  private static final double COMPETING_HANDLE_PENALTY = -0.035;
  private static final double COMPETING_UNSATURATION_PENALTY = -0.030;
  private static final double MULTISITE_SELECTIVITY_PENALTY = -0.050;
  private static final double FMO_DIPOLE_DIPOLAROPHILE_SUPPORT = 0.16;
  private static final double CYCLOADDITION_RING_CLOSURE_SUPPORT = 0.13;
  private static final double ELECTRON_POOR_PI_MATCH_SUPPORT = 0.09;
  private static final double CYCLOADDITION_STERIC_PENALTY = -0.045;
  private static final double EPOXIDE_STRAIN_SUPPORT = 0.12;
  private static final double EPOXIDE_NUCLEOPHILE_SUPPORT = 0.09;
  private static final double EPOXIDE_CONDITION_SUPPORT = 0.06;
  private static final double SELECTIVITY_MATCH_SUPPORT = 0.11;
  private static final double SELECTIVITY_CONDITION_SUPPORT = 0.08;
  private static final double ORTHOGONAL_FUNCTIONAL_GROUP_SUPPORT = 0.055;
  private static final double OVERREACTION_RISK_PENALTY = -0.075;
  private static final double HARSH_CONDITION_SIDE_REACTION_PENALTY = -0.055;
  private static final double REDOX_MISMATCH_PENALTY = -0.070;

  public double score(
      final ReactionPredictionRequest request,
      final ReactionProductCandidate candidate,
      final List<ReactionPredictionEvidence> evidence) {
    if (request == null) {
      throw new IllegalArgumentException("Reactivity request must not be null.");
    }
    if (candidate == null) {
      throw new IllegalArgumentException("Reactivity candidate must not be null.");
    }
    if (evidence == null) {
      throw new IllegalArgumentException("Reactivity evidence list must not be null.");
    }
    final ReactivityProfile reactants =
        ReactionSubstrateReactivityAnalyzer.profile(request.reactants());
    final ReactivityProfile products =
        ReactionSubstrateReactivityAnalyzer.profile(candidate.products());
    double score = 0.0;
    score +=
        ReactionSubstrateReactivityAnalyzer.heteroarylSubstitutionScore(
            request, reactants, products, evidence);
    score +=
        ReactionSubstrateReactivityAnalyzer.selectivityDistributionScore(
            request, reactants, products, evidence);
    score +=
        ReactionSubstrateReactivityAnalyzer.additionCondensationScore(
            request, reactants, products, evidence);
    score +=
        ReactionSubstrateReactivityAnalyzer.epoxideOpeningScore(
            request, reactants, products, evidence);
    score +=
        ReactionSubstrateReactivityAnalyzer.radicalOrRedoxScore(
            request, reactants, products, evidence);
    score +=
        ReactionSubstrateReactivityAnalyzer.nHeterocycleRadicalFunctionalizationScore(
            request, reactants, products, evidence);
    score +=
        ReactionSubstrateReactivityAnalyzer.cycloadditionFmoScore(
            request, reactants, products, evidence);
    score +=
        ReactionSubstrateReactivityAnalyzer.functionalGroupCompetitionScore(
            request, reactants, products, evidence);
    return score;
  }

  private static double selectivityDistributionScore(
      final ReactionPredictionRequest request,
      final ReactivityProfile reactants,
      final ReactivityProfile products,
      final List<ReactionPredictionEvidence> evidence) {
    double score = 0.0;
    final long consumedHalogens = Math.max(0L, reactants.halogenCount - products.halogenCount);
    final long formedCnBonds =
        Math.max(
            0L, products.carbonNitrogenSingleBondCount - reactants.carbonNitrogenSingleBondCount);
    final long retainedHalogens = Math.min(reactants.halogenCount, products.halogenCount);
    final long retainedElectrophiles =
        Math.min(reactants.carbonylCount, products.carbonylCount)
            + Math.min(reactants.imineCount, products.imineCount)
            + Math.min(reactants.epoxideCount, products.epoxideCount)
            + Math.min(
                reactants.electronWithdrawingHandleCount, products.electronWithdrawingHandleCount);
    final long retainedPiBonds =
        Math.min(reactants.alkeneCount, products.alkeneCount)
            + Math.min(reactants.alkyneCount, products.alkyneCount);
    final boolean substitutionLike =
        consumedHalogens > 0L
            && formedCnBonds > 0L
            && reactants.neutralNitrogenNucleophileCount
                    + reactants.oxygenNucleophileCount
                    + reactants.sulfurNucleophileCount
                > 0L;
    if (substitutionLike) {
      score +=
          ReactionSubstrateReactivityAnalyzer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Product distribution model recognizes a substitution-like channel from leaving-group loss and new heteroatom-carbon bond formation.",
              SELECTIVITY_MATCH_SUPPORT * Math.min(2L, Math.min(consumedHalogens, formedCnBonds)));
      if (ReactionSubstrateReactivityAnalyzer.substitutionPromotingConditions(request)) {
        score +=
            ReactionSubstrateReactivityAnalyzer.addEvidence(
                evidence,
                ReactionPredictionEvidenceKind.SOLVENT_EFFECT,
                "Declared base/heat/anhydrous/catalytic context supports heteroatom substitution over simple hydrolysis or unchanged starting material.",
                SELECTIVITY_CONDITION_SUPPORT);
      }
      if (retainedElectrophiles > 0L && retainedPiBonds == 0L) {
        score +=
            ReactionSubstrateReactivityAnalyzer.addEvidence(
                evidence,
                ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
                "Other polar handles remain orthogonal to the selected substitution channel; they are retained as possible later transformations rather than forced products.",
                ORTHOGONAL_FUNCTIONAL_GROUP_SUPPORT * Math.min(2L, retainedElectrophiles));
      }
    }
    if (substitutionLike
        && retainedHalogens > 0L
        && reactants.neutralNitrogenNucleophileCount
                + reactants.oxygenNucleophileCount
                + reactants.sulfurNucleophileCount
            > consumedHalogens
        && ReactionSubstrateReactivityAnalyzer.harshNucleophilicConditions(request)) {
      score +=
          ReactionSubstrateReactivityAnalyzer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.COMPETING_PATHWAY,
              "Residual leaving-group sites and excess nucleophile under strong or heated conditions create over-substitution/regioisomer competition.",
              OVERREACTION_RISK_PENALTY * Math.min(3L, retainedHalogens));
    }
    if (retainedElectrophiles + retainedPiBonds > 0L
        && ReactionSubstrateReactivityAnalyzer.harshSideReactionConditions(request)
        && !request.hasDomain(OrganicReactionDomain.OXIDATION_REDUCTION)) {
      score +=
          ReactionSubstrateReactivityAnalyzer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.COMPETING_PATHWAY,
              "Harsh thermal, acidic, radical, photochemical or catalytic conditions leave reactive handles available; side-product probability is broadened.",
              HARSH_CONDITION_SIDE_REACTION_PENALTY
                  * Math.min(3L, retainedElectrophiles + retainedPiBonds));
    }
    if ((request.conditions().hasFeature(ReactionConditionFeatureKind.OXIDIZING_MEDIUM)
            || request.conditions().hasFeature(ReactionConditionFeatureKind.REDUCING_MEDIUM))
        && reactants.redoxSensitiveCount == products.redoxSensitiveCount
        && reactants.carbonylCount == products.carbonylCount) {
      score +=
          ReactionSubstrateReactivityAnalyzer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.COMPETING_PATHWAY,
              "Redox-active medium is declared but the candidate keeps redox-sensitive handles unchanged; competing oxidation/reduction outcomes remain plausible.",
              REDOX_MISMATCH_PENALTY);
    }
    return score;
  }

  private static double cycloadditionFmoScore(
      final ReactionPredictionRequest request,
      final ReactivityProfile reactants,
      final ReactivityProfile products,
      final List<ReactionPredictionEvidence> evidence) {
    if (!request.hasDomain(OrganicReactionDomain.CYCLIZATION)
        && !request.hasDomain(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS)
        && !request.hasDomain(OrganicReactionDomain.ADDITION)) {
      return 0.0;
    }
    final long dipolarophileCount = reactants.alkeneCount + reactants.alkyneCount;
    if (dipolarophileCount == 0L || reactants.dipoleLikeHandleCount == 0L) {
      return 0.0;
    }
    double score = 0.0;
    score +=
        ReactionSubstrateReactivityAnalyzer.addEvidence(
            evidence,
            ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
            "Reactants contain dipole-like heteroatom multiple-bond handles and alkene/alkyne dipolarophiles; regioselectivity is scored by generic FMO/electronic matching, not by an outcome lookup.",
            FMO_DIPOLE_DIPOLAROPHILE_SUPPORT);
    if (reactants.electronPoorPiBondCount > 0L || reactants.electronWithdrawingHandleCount > 0L) {
      score +=
          ReactionSubstrateReactivityAnalyzer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
              "Electron-poor pi bonds or adjacent electron-withdrawing handles improve dipole-dipolarophile orbital matching.",
              ELECTRON_POOR_PI_MATCH_SUPPORT);
    }
    if (products.ringCount > reactants.ringCount
        || products.carbonCarbonSingleBondCount + products.carbonNitrogenSingleBondCount
            > reactants.carbonCarbonSingleBondCount + reactants.carbonNitrogenSingleBondCount) {
      score +=
          ReactionSubstrateReactivityAnalyzer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN,
              "Candidate increases ring or sigma-bond connectivity consistently with cycloaddition/cyclization.",
              CYCLOADDITION_RING_CLOSURE_SUPPORT);
    }
    if (reactants.stericBranchPointCount > 0L && dipolarophileCount > 0L) {
      score +=
          ReactionSubstrateReactivityAnalyzer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.COMPETING_PATHWAY,
              "Sterically congested pi partners reduce cycloaddition site accessibility and broaden regio/stereo product distribution.",
              CYCLOADDITION_STERIC_PENALTY * Math.min(3L, reactants.stericBranchPointCount));
    }
    return score;
  }

  private static double heteroarylSubstitutionScore(
      final ReactionPredictionRequest request,
      final ReactivityProfile reactants,
      final ReactivityProfile products,
      final List<ReactionPredictionEvidence> evidence) {
    if (!request.hasDomain(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION)
        && !request.hasDomain(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION)) {
      return 0.0;
    }
    double score = 0.0;
    if (reactants.activatedHeteroarylHalideCount > 0) {
      score +=
          ReactionSubstrateReactivityAnalyzer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN,
              "Reactants contain activated heteroaryl C-halogen sites inferred from ring hetero atoms, aromatic/unsaturated bonds and electron-withdrawing neighbors.",
              ACTIVATED_HETEROARYL_HALIDE_SUPPORT);
    }
    if (reactants.activatedHeteroarylLeavingGroupStrength > 0.0) {
      score +=
          ReactionSubstrateReactivityAnalyzer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Activated heteroaryl leaving-group identity changes substitution/coupling kinetics without using outcome lookup.",
              Math.min(
                  LEAVING_GROUP_IDENTITY_SUPPORT,
                  reactants.activatedHeteroarylLeavingGroupStrength * 0.025));
    }
    if (reactants.activatedHeteroarylSiteActivationStrength > 0.0) {
      score +=
          ReactionSubstrateReactivityAnalyzer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
              "Activated heteroaryl C-leaving-group sites are scored by local ring electronics: ring nitrogen proximity, adjacent electron-withdrawing handles and leaving-group identity.",
              Math.min(
                  SITE_ACTIVATION_SUPPORT,
                  reactants.activatedHeteroarylSiteActivationStrength * 0.018));
    }
    if (reactants.neutralNitrogenNucleophileCount
            + reactants.oxygenNucleophileCount
            + reactants.sulfurNucleophileCount
        > 0) {
      score +=
          ReactionSubstrateReactivityAnalyzer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
              "Reactants contain N/O/S nucleophile centers that can attack activated heteroaryl or electrophilic sites.",
              NUCLEOPHILE_SUPPORT);
    }
    if (reactants.electronWithdrawingHandleCount > 0) {
      score +=
          ReactionSubstrateReactivityAnalyzer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
              "Reactants contain electron-withdrawing handles that stabilize an anionic Meisenheimer/addition intermediate.",
              ELECTRON_WITHDRAWING_SUPPORT);
    }
    if (reactants.activatedHeteroarylHalideCount > 0
        && reactants.nitrogenHeterocycleCount > 0
        && reactants.electronWithdrawingHandleCount > 0) {
      score +=
          ReactionSubstrateReactivityAnalyzer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
              "Heteroaryl ring nitrogen and electron-withdrawing handles jointly polarize the leaving-group carbon toward SNAr/addition-elimination attack.",
              EWG_HETEROARYL_SYNERGY_SUPPORT);
    }
    final boolean hasSubstitutionContext =
        reactants.activatedHeteroarylHalideCount > 0
            || reactants.neutralNitrogenNucleophileCount
                    + reactants.oxygenNucleophileCount
                    + reactants.sulfurNucleophileCount
                > 0
            || reactants.electronWithdrawingHandleCount > 0;
    if (hasSubstitutionContext
        && (request.conditions().hasFeature(ReactionConditionFeatureKind.BASIC_MEDIUM)
            || request.conditions().hasFeature(ReactionConditionFeatureKind.HEATING)
            || request.conditions().hasFeature(ReactionConditionFeatureKind.REFLUX))) {
      score +=
          ReactionSubstrateReactivityAnalyzer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.TEMPERATURE_EFFECT,
              "Declared basic or heated conditions support nucleophilic heteroaryl substitution kinetics.",
              CONDITION_SUPPORT);
    }
    if (reactants.halogenCount > products.halogenCount
        && products.carbonNitrogenSingleBondCount > reactants.carbonNitrogenSingleBondCount) {
      score +=
          ReactionSubstrateReactivityAnalyzer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Candidate consumes halogen and forms an additional C-N single bond, matching a substitution bond-change pattern.",
              PRODUCT_TOPOLOGY_SUPPORT);
      score +=
          ReactionSubstrateReactivityAnalyzer.completeHeteroarylSubstitutionScore(
              reactants, products, evidence);
    } else if (reactants.activatedHeteroarylHalideCount > 0) {
      score +=
          ReactionSubstrateReactivityAnalyzer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.COMPETING_PATHWAY,
              "Activated heteroaryl halide is present, but candidate does not clearly consume halogen and form a C-N substitution bond.",
              SIDE_REACTION_PENALTY);
    }
    if (reactants.activatedHeteroarylHalideCount > 1L
        && reactants.halogenCount > products.halogenCount
        && products.halogenCount > 0L) {
      score +=
          ReactionSubstrateReactivityAnalyzer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.COMPETING_PATHWAY,
              "Multiple activated heteroaryl leaving-group sites remain after substitution; regioisomeric or over-substitution channels compete instead of a single looked-up product.",
              MULTISITE_SELECTIVITY_PENALTY
                  * Math.min(3L, reactants.activatedHeteroarylHalideCount - 1L));
    }
    return score;
  }

  private static double completeHeteroarylSubstitutionScore(
      final ReactivityProfile reactants,
      final ReactivityProfile products,
      final List<ReactionPredictionEvidence> evidence) {
    if (reactants.activatedHeteroarylHalideCount == 0L
        || reactants.neutralNitrogenNucleophileCount == 0L) {
      return 0.0;
    }
    final long consumedHalogens = Math.max(0L, reactants.halogenCount - products.halogenCount);
    final long formedCarbonNitrogenBonds =
        Math.max(
            0L, products.carbonNitrogenSingleBondCount - reactants.carbonNitrogenSingleBondCount);
    if (consumedHalogens == 0L || formedCarbonNitrogenBonds == 0L) {
      return 0.0;
    }
    double score = 0.0;
    score +=
        ReactionSubstrateReactivityAnalyzer.addEvidence(
            evidence,
            ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
            "Complete heteroaryl substitution triad detected: activated C-leaving group, nitrogen nucleophile and product C-N bond formation.",
            COMPLETE_SNAR_ELECTRONIC_SUPPORT);
    score +=
        ReactionSubstrateReactivityAnalyzer.addEvidence(
            evidence,
            ReactionPredictionEvidenceKind.KINETIC_FACTOR,
            "Product graph consumes leaving group count consistently with the number of newly formed C-N bonds, supporting a mechanistic substitution channel.",
            COMPLETE_SNAR_KINETIC_SUPPORT * Math.min(consumedHalogens, formedCarbonNitrogenBonds));
    return score;
  }

  private static double additionCondensationScore(
      final ReactionPredictionRequest request,
      final ReactivityProfile reactants,
      final ReactivityProfile products,
      final List<ReactionPredictionEvidence> evidence) {
    double score = 0.0;
    if ((request.hasDomain(OrganicReactionDomain.ADDITION)
            || request.hasDomain(OrganicReactionDomain.CYCLIZATION)
            || request.hasDomain(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS))
        && reactants.carbonylCount + reactants.imineCount + reactants.alkeneCount > 0
        && reactants.neutralNitrogenNucleophileCount + reactants.oxygenNucleophileCount > 0) {
      score +=
          ReactionSubstrateReactivityAnalyzer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
              "Reactants contain electrophilic unsaturated/carbonyl/imine sites and heteroatom nucleophiles suitable for addition or cyclization.",
              0.11);
    }
    if (request.hasDomain(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS)
        && products.ringCount > reactants.ringCount) {
      score +=
          ReactionSubstrateReactivityAnalyzer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN,
              "Candidate increases ring count under heterocycle synthesis domain.",
              0.10);
    }
    return score;
  }

  private static double epoxideOpeningScore(
      final ReactionPredictionRequest request,
      final ReactivityProfile reactants,
      final ReactivityProfile products,
      final List<ReactionPredictionEvidence> evidence) {
    if (!request.hasDomain(OrganicReactionDomain.ADDITION)
        && !request.hasDomain(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION)) {
      return 0.0;
    }
    if (reactants.epoxideCount == 0L) {
      return 0.0;
    }
    double score = 0.0;
    score +=
        ReactionSubstrateReactivityAnalyzer.addEvidence(
            evidence,
            ReactionPredictionEvidenceKind.KINETIC_FACTOR,
            "Reactants contain an epoxide strained three-membered ether ring; ring opening is treated as a high-strain addition/substitution channel.",
            EPOXIDE_STRAIN_SUPPORT);
    if (reactants.neutralNitrogenNucleophileCount
            + reactants.oxygenNucleophileCount
            + reactants.sulfurNucleophileCount
        > 0L) {
      score +=
          ReactionSubstrateReactivityAnalyzer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
              "N/O/S nucleophile centers are present for regioselective epoxide opening.",
              EPOXIDE_NUCLEOPHILE_SUPPORT);
    }
    if (request.conditions().hasFeature(ReactionConditionFeatureKind.ACIDIC_MEDIUM)
        || request.conditions().hasFeature(ReactionConditionFeatureKind.BASIC_MEDIUM)
        || request.conditions().hasFeature(ReactionConditionFeatureKind.ANHYDROUS)) {
      score +=
          ReactionSubstrateReactivityAnalyzer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.SOLVENT_EFFECT,
              "Acid, base, or anhydrous context supports epoxide activation and controls opening regioselectivity.",
              EPOXIDE_CONDITION_SUPPORT);
    }
    if (products.epoxideCount >= reactants.epoxideCount) {
      score +=
          ReactionSubstrateReactivityAnalyzer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.COMPETING_PATHWAY,
              "Candidate retains epoxide count; unresolved ring opening competes with the proposed product.",
              -0.05);
    }
    return score;
  }

  private static double radicalOrRedoxScore(
      final ReactionPredictionRequest request,
      final ReactivityProfile reactants,
      final ReactivityProfile products,
      final List<ReactionPredictionEvidence> evidence) {
    double score = 0.0;
    if (request.hasDomain(OrganicReactionDomain.RADICAL_FUNCTIONALIZATION)
        && (reactants.radicalCenterCount > 0
            || request.conditions().hasFeature(ReactionConditionFeatureKind.PHOTOCHEMICAL))) {
      score +=
          ReactionSubstrateReactivityAnalyzer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Radical centers or photochemical conditions support a radical pathway.",
              0.10);
    }
    if (request.hasDomain(OrganicReactionDomain.OXIDATION_REDUCTION)
        && reactants.redoxSensitiveCount != products.redoxSensitiveCount) {
      score +=
          ReactionSubstrateReactivityAnalyzer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
              "Candidate changes redox-sensitive functional-group count under redox domain.",
              0.08);
    }
    return score;
  }

  private static double nHeterocycleRadicalFunctionalizationScore(
      final ReactionPredictionRequest request,
      final ReactivityProfile reactants,
      final ReactivityProfile products,
      final List<ReactionPredictionEvidence> evidence) {
    if (!request.hasDomain(OrganicReactionDomain.RADICAL_FUNCTIONALIZATION)
        && !request.hasDomain(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION)) {
      return 0.0;
    }
    final boolean hasPathwayActivation =
        reactants.radicalCenterCount > 0L
            || ReactionSubstrateReactivityAnalyzer.hasRadicalFunctionalizationConditions(request);
    final boolean hasFunctionalizationBondChange =
        products.carbonCarbonSingleBondCount > reactants.carbonCarbonSingleBondCount
            || products.carbonNitrogenSingleBondCount > reactants.carbonNitrogenSingleBondCount;
    if (reactants.nitrogenHeterocycleCount == 0L
        || !hasPathwayActivation
        || !hasFunctionalizationBondChange) {
      return 0.0;
    }
    double score = 0.0;
    score +=
        ReactionSubstrateReactivityAnalyzer.addEvidence(
            evidence,
            ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN,
            "Reactants contain an N-heterocycle scaffold that can accept polar/radical addition under Minisci-like or related heteroarene functionalization conditions.",
            MINISCI_HETEROCYCLE_SUPPORT);
    score +=
        ReactionSubstrateReactivityAnalyzer.addEvidence(
            evidence,
            ReactionPredictionEvidenceKind.KINETIC_FACTOR,
            "Declared radical centers, acidic, photochemical or electrochemical conditions support radical heteroarene functionalization.",
            RADICAL_CONDITION_SUPPORT);
    score +=
        ReactionSubstrateReactivityAnalyzer.addEvidence(
            evidence,
            ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
            "Candidate forms additional C-C or C-N sigma bonds on a heteroarene/radical functionalization pathway.",
            RADICAL_BOND_CHANGE_SUPPORT);
    return score;
  }

  private static double functionalGroupCompetitionScore(
      final ReactionPredictionRequest request,
      final ReactivityProfile reactants,
      final ReactivityProfile products,
      final List<ReactionPredictionEvidence> evidence) {
    double score = 0.0;
    final long unconsumedPolarHandles =
        Math.max(0L, reactants.carbonylCount - products.carbonylCount)
            + Math.max(0L, reactants.imineCount - products.imineCount)
            + Math.max(
                0L,
                reactants.electronWithdrawingHandleCount - products.electronWithdrawingHandleCount);
    final long retainedPolarHandles =
        Math.min(reactants.carbonylCount, products.carbonylCount)
            + Math.min(reactants.imineCount, products.imineCount)
            + Math.min(
                reactants.electronWithdrawingHandleCount, products.electronWithdrawingHandleCount);
    final long retainedUnsaturation = Math.min(reactants.alkeneCount, products.alkeneCount);
    final long availableHeteroNucleophiles =
        reactants.neutralNitrogenNucleophileCount
            + reactants.oxygenNucleophileCount
            + reactants.sulfurNucleophileCount;
    if (retainedPolarHandles > 0L
        && availableHeteroNucleophiles > 0L
        && ReactionSubstrateReactivityAnalyzer.polarAdditionConditions(request)) {
      score +=
          ReactionSubstrateReactivityAnalyzer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.COMPETING_PATHWAY,
              "Reactants retain polar electrophilic handles and heteroatom nucleophiles under compatible conditions; addition, condensation, hydrolysis or transimination channels compete with the selected product.",
              COMPETING_HANDLE_PENALTY * Math.min(3L, retainedPolarHandles));
    }
    if (retainedUnsaturation > 0L
        && (request.hasDomain(OrganicReactionDomain.ADDITION)
            || request.conditions().hasFeature(ReactionConditionFeatureKind.CATALYST_PRESENT)
            || request.conditions().hasFeature(ReactionConditionFeatureKind.ACIDIC_MEDIUM))) {
      score +=
          ReactionSubstrateReactivityAnalyzer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.COMPETING_PATHWAY,
              "Reactants retain alkene unsaturation under addition, acidic or catalytic conditions; hydrogenation, hydration, hydrohalogenation or polymerization channels compete.",
              COMPETING_UNSATURATION_PENALTY * Math.min(2L, retainedUnsaturation));
    }
    if (unconsumedPolarHandles > 0L
        && request.hasDomain(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION)
        && request.conditions().hasFeature(ReactionConditionFeatureKind.HEATING)) {
      score +=
          ReactionSubstrateReactivityAnalyzer.addEvidence(
              evidence,
              ReactionPredictionEvidenceKind.COMPETING_PATHWAY,
              "Heated heterocycle functionalization leaves electrophilic handles changed or unresolved; candidate ranking keeps side reactions explicit instead of treating one channel as exclusive.",
              COMPETING_HANDLE_PENALTY * Math.min(2L, unconsumedPolarHandles));
    }
    return score;
  }

  private static boolean polarAdditionConditions(final ReactionPredictionRequest request) {
    return request.hasDomain(OrganicReactionDomain.ADDITION)
        || request.hasDomain(OrganicReactionDomain.CYCLIZATION)
        || request.hasDomain(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS)
        || request.conditions().hasFeature(ReactionConditionFeatureKind.ACIDIC_MEDIUM)
        || request.conditions().hasFeature(ReactionConditionFeatureKind.BASIC_MEDIUM)
        || request.conditions().hasFeature(ReactionConditionFeatureKind.HEATING)
        || request.conditions().hasFeature(ReactionConditionFeatureKind.WORKUP);
  }

  private static boolean substitutionPromotingConditions(final ReactionPredictionRequest request) {
    return request.conditions().hasFeature(ReactionConditionFeatureKind.BASIC_MEDIUM)
        || request.conditions().hasFeature(ReactionConditionFeatureKind.HEATING)
        || request.conditions().hasFeature(ReactionConditionFeatureKind.REFLUX)
        || request.conditions().hasFeature(ReactionConditionFeatureKind.ANHYDROUS)
        || request.conditions().hasFeature(ReactionConditionFeatureKind.CATALYST_PRESENT);
  }

  private static boolean harshNucleophilicConditions(final ReactionPredictionRequest request) {
    return (request.conditions().hasFeature(ReactionConditionFeatureKind.BASIC_MEDIUM)
            || request.conditions().hasFeature(ReactionConditionFeatureKind.CATALYST_PRESENT))
        && (request.conditions().hasFeature(ReactionConditionFeatureKind.HEATING)
            || request.conditions().hasFeature(ReactionConditionFeatureKind.REFLUX)
            || request.conditions().hasFeature(ReactionConditionFeatureKind.MICROWAVE)
            || request.conditions().hasFeature(ReactionConditionFeatureKind.SEALED_VESSEL));
  }

  private static boolean harshSideReactionConditions(final ReactionPredictionRequest request) {
    return request.conditions().hasFeature(ReactionConditionFeatureKind.ACIDIC_MEDIUM)
        || request.conditions().hasFeature(ReactionConditionFeatureKind.OXIDIZING_MEDIUM)
        || request.conditions().hasFeature(ReactionConditionFeatureKind.PHOTOCHEMICAL)
        || request.conditions().hasFeature(ReactionConditionFeatureKind.ELECTROCHEMICAL)
        || request.conditions().hasFeature(ReactionConditionFeatureKind.MICROWAVE)
        || request.conditions().hasFeature(ReactionConditionFeatureKind.REFLUX);
  }

  private static ReactivityProfile profile(final ReactionSide side) {
    long activatedHeteroarylHalideCount = 0L;
    long neutralNitrogenNucleophileCount = 0L;
    long oxygenNucleophileCount = 0L;
    long sulfurNucleophileCount = 0L;
    long electronWithdrawingHandleCount = 0L;
    long halogenCount = 0L;
    long carbonNitrogenSingleBondCount = 0L;
    long carbonCarbonSingleBondCount = 0L;
    long carbonylCount = 0L;
    long imineCount = 0L;
    long alkeneCount = 0L;
    long alkyneCount = 0L;
    long dipoleLikeHandleCount = 0L;
    long electronPoorPiBondCount = 0L;
    long stericBranchPointCount = 0L;
    long radicalCenterCount = 0L;
    long redoxSensitiveCount = 0L;
    long ringCount = 0L;
    long nitrogenHeterocycleCount = 0L;
    long epoxideCount = 0L;
    double activatedHeteroarylLeavingGroupStrength = 0.0;
    double activatedHeteroarylSiteActivationStrength = 0.0;
    final List<ReactionParticipant> participants = side.participants();
    for (int i = 0; i < participants.size(); ++i) {
      final long coefficient = participants.get(i).coefficient().value();
      final Molecule molecule = participants.get(i).molecule();
      final MoleculeProfile profile = ReactionSubstrateReactivityAnalyzer.moleculeProfile(molecule);
      activatedHeteroarylHalideCount += coefficient * profile.activatedHeteroarylHalideCount;
      activatedHeteroarylLeavingGroupStrength +=
          coefficient * profile.activatedHeteroarylLeavingGroupStrength;
      activatedHeteroarylSiteActivationStrength +=
          coefficient * profile.activatedHeteroarylSiteActivationStrength;
      neutralNitrogenNucleophileCount += coefficient * profile.neutralNitrogenNucleophileCount;
      oxygenNucleophileCount += coefficient * profile.oxygenNucleophileCount;
      sulfurNucleophileCount += coefficient * profile.sulfurNucleophileCount;
      electronWithdrawingHandleCount += coefficient * profile.electronWithdrawingHandleCount;
      halogenCount += coefficient * profile.halogenCount;
      carbonNitrogenSingleBondCount += coefficient * profile.carbonNitrogenSingleBondCount;
      carbonCarbonSingleBondCount += coefficient * profile.carbonCarbonSingleBondCount;
      carbonylCount += coefficient * profile.carbonylCount;
      imineCount += coefficient * profile.imineCount;
      alkeneCount += coefficient * profile.alkeneCount;
      alkyneCount += coefficient * profile.alkyneCount;
      dipoleLikeHandleCount += coefficient * profile.dipoleLikeHandleCount;
      electronPoorPiBondCount += coefficient * profile.electronPoorPiBondCount;
      stericBranchPointCount += coefficient * profile.stericBranchPointCount;
      radicalCenterCount += coefficient * profile.radicalCenterCount;
      redoxSensitiveCount += coefficient * profile.redoxSensitiveCount;
      ringCount += coefficient * profile.ringCount;
      nitrogenHeterocycleCount += coefficient * profile.nitrogenHeterocycleCount;
      epoxideCount += coefficient * profile.epoxideCount;
    }
    return new ReactivityProfile(
        activatedHeteroarylHalideCount,
        activatedHeteroarylLeavingGroupStrength,
        activatedHeteroarylSiteActivationStrength,
        neutralNitrogenNucleophileCount,
        oxygenNucleophileCount,
        sulfurNucleophileCount,
        electronWithdrawingHandleCount,
        halogenCount,
        carbonNitrogenSingleBondCount,
        carbonCarbonSingleBondCount,
        carbonylCount,
        imineCount,
        alkeneCount,
        alkyneCount,
        dipoleLikeHandleCount,
        electronPoorPiBondCount,
        stericBranchPointCount,
        radicalCenterCount,
        redoxSensitiveCount,
        ringCount,
        nitrogenHeterocycleCount,
        epoxideCount);
  }

  private static MoleculeProfile moleculeProfile(final Molecule molecule) {
    final HashMap<AtomId, String> symbols = ReactionSubstrateReactivityAnalyzer.symbols(molecule);
    final HashSet<AtomId> ringAtomIds = ReactionSubstrateReactivityAnalyzer.ringAtomIds(molecule);
    long activatedHeteroarylHalideCount = 0L;
    long neutralNitrogenNucleophileCount = 0L;
    long oxygenNucleophileCount = 0L;
    long sulfurNucleophileCount = 0L;
    long electronWithdrawingHandleCount = 0L;
    long halogenCount = 0L;
    long carbonNitrogenSingleBondCount = 0L;
    long carbonCarbonSingleBondCount = 0L;
    long carbonylCount = 0L;
    long imineCount = 0L;
    long alkeneCount = 0L;
    long alkyneCount = 0L;
    long dipoleLikeHandleCount = 0L;
    long electronPoorPiBondCount = 0L;
    long stericBranchPointCount = 0L;
    long radicalCenterCount = 0L;
    long redoxSensitiveCount = 0L;
    double activatedHeteroarylLeavingGroupStrength = 0.0;
    double activatedHeteroarylSiteActivationStrength = 0.0;
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      final String symbol = atom.element().symbol().value();
      if (ReactionSubstrateReactivityAnalyzer.isHalogen(symbol)) {
        ++halogenCount;
      }
      if ("N".equals(symbol)
          && atom.formalCharge().value() <= 0
          && !ringAtomIds.contains(atom.id())) {
        ++neutralNitrogenNucleophileCount;
      }
      if ("O".equals(symbol)
          && atom.formalCharge().value() <= 0
          && !ringAtomIds.contains(atom.id())) {
        ++oxygenNucleophileCount;
      }
      if ("S".equals(symbol)
          && atom.formalCharge().value() <= 0
          && !ringAtomIds.contains(atom.id())) {
        ++sulfurNucleophileCount;
      }
      if ("C".equals(symbol)
          && ReactionSubstrateReactivityAnalyzer.heavyNeighborCount(molecule, atom.id()) >= 3) {
        ++stericBranchPointCount;
      }
      if (atom.radicalState().radical()) {
        ++radicalCenterCount;
      }
    }
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      final String first = symbols.get(bond.firstAtomId());
      final String second = symbols.get(bond.secondAtomId());
      if (ReactionSubstrateReactivityAnalyzer.isCarbonNitrogenSingle(first, second, bond)) {
        ++carbonNitrogenSingleBondCount;
      }
      if (ReactionSubstrateReactivityAnalyzer.isCarbonCarbonSingle(first, second, bond)) {
        ++carbonCarbonSingleBondCount;
      }
      if (ReactionSubstrateReactivityAnalyzer.isCarbonOxygenDouble(first, second, bond)) {
        ++carbonylCount;
        ++electronWithdrawingHandleCount;
        ++redoxSensitiveCount;
      }
      if (ReactionSubstrateReactivityAnalyzer.isCarbonNitrogenDouble(first, second, bond)) {
        ++imineCount;
      }
      if (ReactionSubstrateReactivityAnalyzer.isCarbonCarbonDouble(first, second, bond)) {
        ++alkeneCount;
        if (ReactionSubstrateReactivityAnalyzer.bondHasElectronWithdrawingNeighbor(
            molecule, symbols, bond)) {
          ++electronPoorPiBondCount;
        }
      }
      if (ReactionSubstrateReactivityAnalyzer.isCarbonCarbonTriple(first, second, bond)) {
        ++alkyneCount;
        if (ReactionSubstrateReactivityAnalyzer.bondHasElectronWithdrawingNeighbor(
            molecule, symbols, bond)) {
          ++electronPoorPiBondCount;
        }
      }
      if (ReactionSubstrateReactivityAnalyzer.isCarbonNitrogenTriple(first, second, bond)) {
        ++electronWithdrawingHandleCount;
        ++dipoleLikeHandleCount;
      }
      if (ReactionSubstrateReactivityAnalyzer.isHeteroatomMultipleBond(first, second, bond)) {
        ++dipoleLikeHandleCount;
      }
      if (ReactionSubstrateReactivityAnalyzer.isActivatedHeteroarylHalide(
          molecule, symbols, bond)) {
        ++activatedHeteroarylHalideCount;
        activatedHeteroarylLeavingGroupStrength +=
            ReactionSubstrateReactivityAnalyzer.heteroarylLeavingGroupStrength(symbols, bond);
        activatedHeteroarylSiteActivationStrength +=
            ReactionSubstrateReactivityAnalyzer.heteroarylSiteActivationStrength(
                molecule, symbols, bond);
      }
    }
    return new MoleculeProfile(
        activatedHeteroarylHalideCount,
        activatedHeteroarylLeavingGroupStrength,
        activatedHeteroarylSiteActivationStrength,
        neutralNitrogenNucleophileCount,
        oxygenNucleophileCount,
        sulfurNucleophileCount,
        electronWithdrawingHandleCount,
        halogenCount,
        carbonNitrogenSingleBondCount,
        carbonCarbonSingleBondCount,
        carbonylCount,
        imineCount,
        alkeneCount,
        alkyneCount,
        dipoleLikeHandleCount,
        electronPoorPiBondCount,
        stericBranchPointCount,
        radicalCenterCount,
        redoxSensitiveCount,
        molecule.graph().rings().size(),
        ReactionSubstrateReactivityAnalyzer.nitrogenHeterocycleCount(molecule, symbols),
        ReactionSubstrateReactivityAnalyzer.epoxideCount(molecule, symbols));
  }

  private static long epoxideCount(final Molecule molecule, final HashMap<AtomId, String> symbols) {
    long count = 0L;
    final List<MolecularRing> rings = molecule.graph().rings();
    for (int i = 0; i < rings.size(); ++i) {
      final MolecularRing ring = rings.get(i);
      if (ring.size() == 3
          && ReactionSubstrateReactivityAnalyzer.ringContains(ring, symbols, "O")
          && ReactionSubstrateReactivityAnalyzer.ringElementCount(ring, symbols, "C") == 2L
          && ReactionSubstrateReactivityAnalyzer.ringSingleBondCount(molecule, ring) == 3L) {
        ++count;
      }
    }
    return count;
  }

  private static HashSet<AtomId> ringAtomIds(final Molecule molecule) {
    final HashSet<AtomId> atomIds = new HashSet<AtomId>();
    final List<MolecularRing> rings = molecule.graph().rings();
    for (int i = 0; i < rings.size(); ++i) {
      atomIds.addAll(rings.get(i).atomIds());
    }
    return atomIds;
  }

  private static long nitrogenHeterocycleCount(
      final Molecule molecule, final HashMap<AtomId, String> symbols) {
    long count = 0L;
    final List<MolecularRing> rings = molecule.graph().rings();
    for (int i = 0; i < rings.size(); ++i) {
      final MolecularRing ring = rings.get(i);
      final List<AtomId> atomIds = ring.atomIds();
      for (int j = 0; j < atomIds.size(); ++j) {
        if (!"N".equals(symbols.get(atomIds.get(j)))) {
          continue;
        }
        ++count;
        break;
      }
    }
    return count;
  }

  private static boolean isActivatedHeteroarylHalide(
      final Molecule molecule, final HashMap<AtomId, String> symbols, final Bond bond) {
    if (bond.type() != BondType.SINGLE) {
      return false;
    }
    final AtomId carbon = ReactionSubstrateReactivityAnalyzer.carbonHalogenCarbon(symbols, bond);
    if (carbon == null) {
      return false;
    }
    final MolecularGraph graph = molecule.graph();
    final List<MolecularRing> rings = graph.rings();
    for (int i = 0; i < rings.size(); ++i) {
      final MolecularRing ring = rings.get(i);
      if (!ring.atomIds().contains(carbon)) {
        continue;
      }
      if (ReactionSubstrateReactivityAnalyzer.ringHasHeteroAtom(ring, symbols)
          || ReactionSubstrateReactivityAnalyzer.ringHasElectronWithdrawingSubstituent(
              molecule, symbols, ring)) {
        return true;
      }
    }
    return ReactionSubstrateReactivityAnalyzer.hasAdjacentElectronWithdrawingBond(
        molecule, symbols, carbon);
  }

  private static boolean ringHasHeteroAtom(
      final MolecularRing ring, final HashMap<AtomId, String> symbols) {
    final List<AtomId> atomIds = ring.atomIds();
    for (int i = 0; i < atomIds.size(); ++i) {
      final String symbol = symbols.get(atomIds.get(i));
      if (!"C".equals(symbol) && !"H".equals(symbol)) {
        return true;
      }
    }
    return false;
  }

  private static boolean ringContains(
      final MolecularRing ring,
      final HashMap<AtomId, String> symbols,
      final String expectedSymbol) {
    return ReactionSubstrateReactivityAnalyzer.ringElementCount(ring, symbols, expectedSymbol) > 0L;
  }

  private static long ringElementCount(
      final MolecularRing ring,
      final HashMap<AtomId, String> symbols,
      final String expectedSymbol) {
    long count = 0L;
    final List<AtomId> atomIds = ring.atomIds();
    for (int i = 0; i < atomIds.size(); ++i) {
      if (expectedSymbol.equals(symbols.get(atomIds.get(i)))) {
        ++count;
      }
    }
    return count;
  }

  private static long ringSingleBondCount(final Molecule molecule, final MolecularRing ring) {
    long count = 0L;
    final List<AtomId> atomIds = ring.atomIds();
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (bond.type() == BondType.SINGLE
          && atomIds.contains(bond.firstAtomId())
          && atomIds.contains(bond.secondAtomId())) {
        ++count;
      }
    }
    return count;
  }

  private static boolean ringHasElectronWithdrawingSubstituent(
      final Molecule molecule, final HashMap<AtomId, String> symbols, final MolecularRing ring) {
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      final boolean firstInRing = ring.atomIds().contains(bond.firstAtomId());
      final boolean secondInRing = ring.atomIds().contains(bond.secondAtomId());
      if (firstInRing == secondInRing) {
        continue;
      }
      final AtomId substituent = firstInRing ? bond.secondAtomId() : bond.firstAtomId();
      if (ReactionSubstrateReactivityAnalyzer.atomHasElectronWithdrawingBond(
          molecule, symbols, substituent)) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasAdjacentElectronWithdrawingBond(
      final Molecule molecule, final HashMap<AtomId, String> symbols, final AtomId carbon) {
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (!bond.firstAtomId().equals(carbon) && !bond.secondAtomId().equals(carbon)) {
        continue;
      }
      final AtomId neighbor =
          bond.firstAtomId().equals(carbon) ? bond.secondAtomId() : bond.firstAtomId();
      if (ReactionSubstrateReactivityAnalyzer.atomHasElectronWithdrawingBond(
          molecule, symbols, neighbor)) {
        return true;
      }
    }
    return false;
  }

  private static boolean atomHasElectronWithdrawingBond(
      final Molecule molecule, final HashMap<AtomId, String> symbols, final AtomId atomId) {
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (!bond.firstAtomId().equals(atomId) && !bond.secondAtomId().equals(atomId)) {
        continue;
      }
      final String first = symbols.get(bond.firstAtomId());
      final String second = symbols.get(bond.secondAtomId());
      if (ReactionSubstrateReactivityAnalyzer.isCarbonOxygenDouble(first, second, bond)
          || ReactionSubstrateReactivityAnalyzer.isCarbonNitrogenTriple(first, second, bond)) {
        return true;
      }
    }
    return false;
  }

  private static AtomId carbonHalogenCarbon(
      final HashMap<AtomId, String> symbols, final Bond bond) {
    final String first = symbols.get(bond.firstAtomId());
    final String second = symbols.get(bond.secondAtomId());
    if ("C".equals(first) && ReactionSubstrateReactivityAnalyzer.isHalogen(second)) {
      return bond.firstAtomId();
    }
    if ("C".equals(second) && ReactionSubstrateReactivityAnalyzer.isHalogen(first)) {
      return bond.secondAtomId();
    }
    return null;
  }

  private static double heteroarylLeavingGroupStrength(
      final HashMap<AtomId, String> symbols, final Bond bond) {
    final String first = symbols.get(bond.firstAtomId());
    final String second = symbols.get(bond.secondAtomId());
    if ("C".equals(first)) {
      return ReactionSubstrateReactivityAnalyzer.leavingGroupStrength(second);
    }
    if ("C".equals(second)) {
      return ReactionSubstrateReactivityAnalyzer.leavingGroupStrength(first);
    }
    return 0.0;
  }

  private static double heteroarylSiteActivationStrength(
      final Molecule molecule, final HashMap<AtomId, String> symbols, final Bond bond) {
    final AtomId carbon = ReactionSubstrateReactivityAnalyzer.carbonHalogenCarbon(symbols, bond);
    if (carbon == null) {
      return 0.0;
    }
    double strength =
        ReactionSubstrateReactivityAnalyzer.heteroarylLeavingGroupStrength(symbols, bond);
    final List<MolecularRing> rings = molecule.graph().rings();
    for (int i = 0; i < rings.size(); ++i) {
      final MolecularRing ring = rings.get(i);
      if (!ring.atomIds().contains(carbon)) {
        continue;
      }
      strength += ReactionSubstrateReactivityAnalyzer.ringNitrogenActivation(ring, symbols, carbon);
      if (ReactionSubstrateReactivityAnalyzer.ringHasElectronWithdrawingSubstituent(
          molecule, symbols, ring)) {
        strength += 0.85;
      }
    }
    if (ReactionSubstrateReactivityAnalyzer.hasAdjacentElectronWithdrawingBond(
        molecule, symbols, carbon)) {
      strength += 0.75;
    }
    return strength;
  }

  private static double ringNitrogenActivation(
      final MolecularRing ring,
      final HashMap<AtomId, String> symbols,
      final AtomId leavingGroupCarbon) {
    final List<AtomId> atomIds = ring.atomIds();
    final int carbonIndex = atomIds.indexOf(leavingGroupCarbon);
    if (carbonIndex < 0) {
      return 0.0;
    }
    double strength = 0.0;
    for (int i = 0; i < atomIds.size(); ++i) {
      if (!"N".equals(symbols.get(atomIds.get(i)))) {
        continue;
      }
      final int distance =
          ReactionSubstrateReactivityAnalyzer.ringDistance(i, carbonIndex, atomIds.size());
      if (distance == 1) {
        strength += 1.00;
      } else if (distance == 2) {
        strength += 0.80;
      } else {
        strength += 0.45;
      }
    }
    return strength;
  }

  private static int ringDistance(final int first, final int second, final int size) {
    final int direct = Math.abs(first - second);
    return Math.min(direct, size - direct);
  }

  private static double leavingGroupStrength(final String symbol) {
    if ("F".equals(symbol)) {
      return 1.15;
    }
    if ("Cl".equals(symbol)) {
      return 1.00;
    }
    if ("Br".equals(symbol)) {
      return 0.92;
    }
    if ("I".equals(symbol)) {
      return 0.86;
    }
    return 0.0;
  }

  private static HashMap<AtomId, String> symbols(final Molecule molecule) {
    final HashMap<AtomId, String> symbols = new HashMap<AtomId, String>();
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      symbols.put(atom.id(), atom.element().symbol().value());
    }
    return symbols;
  }

  private static boolean isCarbonNitrogenSingle(
      final String first, final String second, final Bond bond) {
    return bond.type() == BondType.SINGLE
        && ReactionSubstrateReactivityAnalyzer.isPair(first, second, "C", "N");
  }

  private static boolean isCarbonCarbonSingle(
      final String first, final String second, final Bond bond) {
    return bond.type() == BondType.SINGLE
        && ReactionSubstrateReactivityAnalyzer.isPair(first, second, "C", "C");
  }

  private static boolean isCarbonOxygenDouble(
      final String first, final String second, final Bond bond) {
    return bond.type() == BondType.DOUBLE
        && ReactionSubstrateReactivityAnalyzer.isPair(first, second, "C", "O");
  }

  private static boolean isCarbonNitrogenDouble(
      final String first, final String second, final Bond bond) {
    return bond.type() == BondType.DOUBLE
        && ReactionSubstrateReactivityAnalyzer.isPair(first, second, "C", "N");
  }

  private static boolean isCarbonNitrogenTriple(
      final String first, final String second, final Bond bond) {
    return bond.type() == BondType.TRIPLE
        && ReactionSubstrateReactivityAnalyzer.isPair(first, second, "C", "N");
  }

  private static boolean isCarbonCarbonTriple(
      final String first, final String second, final Bond bond) {
    return bond.type() == BondType.TRIPLE
        && ReactionSubstrateReactivityAnalyzer.isPair(first, second, "C", "C");
  }

  private static boolean isCarbonCarbonDouble(
      final String first, final String second, final Bond bond) {
    return bond.type() == BondType.DOUBLE
        && ReactionSubstrateReactivityAnalyzer.isPair(first, second, "C", "C");
  }

  private static boolean isHeteroatomMultipleBond(
      final String first, final String second, final Bond bond) {
    if (bond.type() != BondType.DOUBLE && bond.type() != BondType.TRIPLE) {
      return false;
    }
    return ReactionSubstrateReactivityAnalyzer.isHeteroAtom(first)
        && ReactionSubstrateReactivityAnalyzer.isHeteroAtom(second);
  }

  private static boolean bondHasElectronWithdrawingNeighbor(
      final Molecule molecule, final HashMap<AtomId, String> symbols, final Bond piBond) {
    return ReactionSubstrateReactivityAnalyzer.hasAdjacentElectronWithdrawingBond(
            molecule, symbols, piBond.firstAtomId())
        || ReactionSubstrateReactivityAnalyzer.hasAdjacentElectronWithdrawingBond(
            molecule, symbols, piBond.secondAtomId());
  }

  private static long heavyNeighborCount(final Molecule molecule, final AtomId atomId) {
    long count = 0L;
    final HashMap<AtomId, String> symbols = ReactionSubstrateReactivityAnalyzer.symbols(molecule);
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (!bond.firstAtomId().equals(atomId) && !bond.secondAtomId().equals(atomId)) {
        continue;
      }
      final AtomId neighbor =
          bond.firstAtomId().equals(atomId) ? bond.secondAtomId() : bond.firstAtomId();
      if (!"H".equals(symbols.get(neighbor))) {
        ++count;
      }
    }
    return count;
  }

  private static boolean isPair(
      final String first,
      final String second,
      final String expectedFirst,
      final String expectedSecond) {
    return expectedFirst.equals(first) && expectedSecond.equals(second)
        || expectedFirst.equals(second) && expectedSecond.equals(first);
  }

  private static boolean isHalogen(final String symbol) {
    return "F".equals(symbol) || "Cl".equals(symbol) || "Br".equals(symbol) || "I".equals(symbol);
  }

  private static boolean isHeteroAtom(final String symbol) {
    return "N".equals(symbol) || "O".equals(symbol) || "S".equals(symbol) || "P".equals(symbol);
  }

  private static boolean hasRadicalFunctionalizationConditions(
      final ReactionPredictionRequest request) {
    return request.conditions().hasFeature(ReactionConditionFeatureKind.ACIDIC_MEDIUM)
        || request.conditions().hasFeature(ReactionConditionFeatureKind.PHOTOCHEMICAL)
        || request.conditions().hasFeature(ReactionConditionFeatureKind.ELECTROCHEMICAL);
  }

  private static double addEvidence(
      final List<ReactionPredictionEvidence> evidence,
      final ReactionPredictionEvidenceKind kind,
      final String description,
      final double weight) {
    evidence.add(ReactionPredictionEvidence.of(kind, description, weight));
    return weight;
  }

  private static final class ReactivityProfile {

    private final long activatedHeteroarylHalideCount;
    private final double activatedHeteroarylLeavingGroupStrength;
    private final double activatedHeteroarylSiteActivationStrength;
    private final long neutralNitrogenNucleophileCount;
    private final long oxygenNucleophileCount;
    private final long sulfurNucleophileCount;
    private final long electronWithdrawingHandleCount;
    private final long halogenCount;
    private final long carbonNitrogenSingleBondCount;
    private final long carbonCarbonSingleBondCount;
    private final long carbonylCount;
    private final long imineCount;
    private final long alkeneCount;
    private final long alkyneCount;
    private final long dipoleLikeHandleCount;
    private final long electronPoorPiBondCount;
    private final long stericBranchPointCount;
    private final long radicalCenterCount;
    private final long redoxSensitiveCount;
    private final long ringCount;
    private final long nitrogenHeterocycleCount;
    private final long epoxideCount;

    private ReactivityProfile(
        final long activatedHeteroarylHalideCount,
        final double activatedHeteroarylLeavingGroupStrength,
        final double activatedHeteroarylSiteActivationStrength,
        final long neutralNitrogenNucleophileCount,
        final long oxygenNucleophileCount,
        final long sulfurNucleophileCount,
        final long electronWithdrawingHandleCount,
        final long halogenCount,
        final long carbonNitrogenSingleBondCount,
        final long carbonCarbonSingleBondCount,
        final long carbonylCount,
        final long imineCount,
        final long alkeneCount,
        final long alkyneCount,
        final long dipoleLikeHandleCount,
        final long electronPoorPiBondCount,
        final long stericBranchPointCount,
        final long radicalCenterCount,
        final long redoxSensitiveCount,
        final long ringCount,
        final long nitrogenHeterocycleCount,
        final long epoxideCount) {
      this.activatedHeteroarylHalideCount = activatedHeteroarylHalideCount;
      this.activatedHeteroarylLeavingGroupStrength = activatedHeteroarylLeavingGroupStrength;
      this.activatedHeteroarylSiteActivationStrength = activatedHeteroarylSiteActivationStrength;
      this.neutralNitrogenNucleophileCount = neutralNitrogenNucleophileCount;
      this.oxygenNucleophileCount = oxygenNucleophileCount;
      this.sulfurNucleophileCount = sulfurNucleophileCount;
      this.electronWithdrawingHandleCount = electronWithdrawingHandleCount;
      this.halogenCount = halogenCount;
      this.carbonNitrogenSingleBondCount = carbonNitrogenSingleBondCount;
      this.carbonCarbonSingleBondCount = carbonCarbonSingleBondCount;
      this.carbonylCount = carbonylCount;
      this.imineCount = imineCount;
      this.alkeneCount = alkeneCount;
      this.alkyneCount = alkyneCount;
      this.dipoleLikeHandleCount = dipoleLikeHandleCount;
      this.electronPoorPiBondCount = electronPoorPiBondCount;
      this.stericBranchPointCount = stericBranchPointCount;
      this.radicalCenterCount = radicalCenterCount;
      this.redoxSensitiveCount = redoxSensitiveCount;
      this.ringCount = ringCount;
      this.nitrogenHeterocycleCount = nitrogenHeterocycleCount;
      this.epoxideCount = epoxideCount;
    }
  }

  private static final class MoleculeProfile {

    private final long activatedHeteroarylHalideCount;
    private final double activatedHeteroarylLeavingGroupStrength;
    private final double activatedHeteroarylSiteActivationStrength;
    private final long neutralNitrogenNucleophileCount;
    private final long oxygenNucleophileCount;
    private final long sulfurNucleophileCount;
    private final long electronWithdrawingHandleCount;
    private final long halogenCount;
    private final long carbonNitrogenSingleBondCount;
    private final long carbonCarbonSingleBondCount;
    private final long carbonylCount;
    private final long imineCount;
    private final long alkeneCount;
    private final long alkyneCount;
    private final long dipoleLikeHandleCount;
    private final long electronPoorPiBondCount;
    private final long stericBranchPointCount;
    private final long radicalCenterCount;
    private final long redoxSensitiveCount;
    private final long ringCount;
    private final long nitrogenHeterocycleCount;
    private final long epoxideCount;

    private MoleculeProfile(
        final long activatedHeteroarylHalideCount,
        final double activatedHeteroarylLeavingGroupStrength,
        final double activatedHeteroarylSiteActivationStrength,
        final long neutralNitrogenNucleophileCount,
        final long oxygenNucleophileCount,
        final long sulfurNucleophileCount,
        final long electronWithdrawingHandleCount,
        final long halogenCount,
        final long carbonNitrogenSingleBondCount,
        final long carbonCarbonSingleBondCount,
        final long carbonylCount,
        final long imineCount,
        final long alkeneCount,
        final long alkyneCount,
        final long dipoleLikeHandleCount,
        final long electronPoorPiBondCount,
        final long stericBranchPointCount,
        final long radicalCenterCount,
        final long redoxSensitiveCount,
        final long ringCount,
        final long nitrogenHeterocycleCount,
        final long epoxideCount) {
      this.activatedHeteroarylHalideCount = activatedHeteroarylHalideCount;
      this.activatedHeteroarylLeavingGroupStrength = activatedHeteroarylLeavingGroupStrength;
      this.activatedHeteroarylSiteActivationStrength = activatedHeteroarylSiteActivationStrength;
      this.neutralNitrogenNucleophileCount = neutralNitrogenNucleophileCount;
      this.oxygenNucleophileCount = oxygenNucleophileCount;
      this.sulfurNucleophileCount = sulfurNucleophileCount;
      this.electronWithdrawingHandleCount = electronWithdrawingHandleCount;
      this.halogenCount = halogenCount;
      this.carbonNitrogenSingleBondCount = carbonNitrogenSingleBondCount;
      this.carbonCarbonSingleBondCount = carbonCarbonSingleBondCount;
      this.carbonylCount = carbonylCount;
      this.imineCount = imineCount;
      this.alkeneCount = alkeneCount;
      this.alkyneCount = alkyneCount;
      this.dipoleLikeHandleCount = dipoleLikeHandleCount;
      this.electronPoorPiBondCount = electronPoorPiBondCount;
      this.stericBranchPointCount = stericBranchPointCount;
      this.radicalCenterCount = radicalCenterCount;
      this.redoxSensitiveCount = redoxSensitiveCount;
      this.ringCount = ringCount;
      this.nitrogenHeterocycleCount = nitrogenHeterocycleCount;
      this.epoxideCount = epoxideCount;
    }
  }
}

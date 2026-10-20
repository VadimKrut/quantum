/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule;

import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;

/** Java-native правило, которое распознает реакционную постановку и создает кандидатов продуктов. */
public interface OrganicReactionRule {

  OrganicReactionRuleId id();

  String displayName();

  OrganicReactionRuleCategory category();

  default ReactionMechanismFamily mechanismFamily() {
    final ReactionMechanismFamily inferred =
        OrganicReactionRule.mechanismFamily(this.id().value(), this.displayName());
    if (inferred != ReactionMechanismFamily.UNKNOWN) {
      return inferred;
    }
    return OrganicReactionRule.mechanismFamily(this.category());
  }

  OrganicReactionRuleMatch match(final ReactionPredictionRequest request);

  List<ReactionProductCandidate> generate(
      final ReactionPredictionRequest request, final OrganicReactionRuleMatch match);

  static ReactionMechanismFamily mechanismFamily(final OrganicReactionRuleCategory category) {
    if (category == null) {
      return ReactionMechanismFamily.UNKNOWN;
    }
    if (category == OrganicReactionRuleCategory.HETEROCYCLE_SYNTHESIS) {
      return ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION;
    }
    if (category == OrganicReactionRuleCategory.HETEROCYCLE_FUNCTIONALIZATION) {
      return ReactionMechanismFamily.HETEROCYCLE_FUNCTIONALIZATION;
    }
    if (category == OrganicReactionRuleCategory.SUBSTITUTION) {
      return ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION;
    }
    if (category == OrganicReactionRuleCategory.ADDITION) {
      return ReactionMechanismFamily.ADDITION;
    }
    if (category == OrganicReactionRuleCategory.ELIMINATION) {
      return ReactionMechanismFamily.ELIMINATION_E2;
    }
    if (category == OrganicReactionRuleCategory.OXIDATION) {
      return ReactionMechanismFamily.OXIDATION;
    }
    if (category == OrganicReactionRuleCategory.REDUCTION) {
      return ReactionMechanismFamily.REDUCTION;
    }
    if (category == OrganicReactionRuleCategory.ACID_BASE) {
      return ReactionMechanismFamily.ACID_BASE;
    }
    if (category == OrganicReactionRuleCategory.COUPLING) {
      return ReactionMechanismFamily.ORGANOMETALLIC_CROSS_COUPLING;
    }
    if (category == OrganicReactionRuleCategory.REARRANGEMENT) {
      return ReactionMechanismFamily.REARRANGEMENT;
    }
    if (category == OrganicReactionRuleCategory.RADICAL_FUNCTIONALIZATION) {
      return ReactionMechanismFamily.RADICAL_SUBSTITUTION;
    }
    return ReactionMechanismFamily.UNKNOWN;
  }

  static ReactionMechanismFamily mechanismFamily(final String id, final String displayName) {
    final String text = (id + " " + displayName).toLowerCase(java.util.Locale.ROOT);
    if (text.contains("suzuki")) {
      return ReactionMechanismFamily.SUZUKI_COUPLING;
    }
    if (text.contains("heck")) {
      return ReactionMechanismFamily.HECK_COUPLING;
    }
    if (text.contains("sonogashira")) {
      return ReactionMechanismFamily.SONOGASHIRA_COUPLING;
    }
    if (text.contains("negishi")) {
      return ReactionMechanismFamily.NEGISHI_COUPLING;
    }
    if (text.contains("stille")) {
      return ReactionMechanismFamily.STILLE_COUPLING;
    }
    if (text.contains("buchwald") || text.contains("hartwig")) {
      return ReactionMechanismFamily.BUCHWALD_HARTWIG_AMINATION;
    }
    if (text.contains("borylation")) {
      return ReactionMechanismFamily.ORGANOMETALLIC_CROSS_COUPLING;
    }
    if (text.contains("protodeboronation")) {
      return ReactionMechanismFamily.SOLVOLYSIS;
    }
    if (text.contains("hydrodehalogenation")) {
      return ReactionMechanismFamily.REDUCTION;
    }
    if (text.contains("hydrohalogenation")) {
      return ReactionMechanismFamily.HYDROHALOGENATION;
    }
    if (text.contains("halogenation")) {
      return ReactionMechanismFamily.HALOGENATION;
    }
    if (text.contains("nitration")) {
      return ReactionMechanismFamily.NITRATION;
    }
    if (text.contains("sulfonation")) {
      return ReactionMechanismFamily.SULFONATION;
    }
    if (text.contains("friedel") && text.contains("alkylation")) {
      return ReactionMechanismFamily.FRIEDEL_CRAFTS_ALKYLATION;
    }
    if (text.contains("friedel") && text.contains("acylation")) {
      return ReactionMechanismFamily.FRIEDEL_CRAFTS_ACYLATION;
    }
    if (text.contains("diazonium") || text.contains("sandmeyer")) {
      return ReactionMechanismFamily.SANDMEYER;
    }
    if (text.contains("azo")) {
      return ReactionMechanismFamily.DIAZOTIZATION;
    }
    if (text.contains("n oxidation")
        || text.contains("n-oxidation")
        || text.contains("n_oxidation")) {
      return ReactionMechanismFamily.OXIDATION;
    }
    if (text.contains("n alkylation")
        || text.contains("n-alkylation")
        || text.contains("n_alkylation")) {
      return ReactionMechanismFamily.NUCLEOPHILIC_SUBSTITUTION_SN2;
    }
    if (text.contains("nitrile") && text.contains("hydration")) {
      return ReactionMechanismFamily.HYDRATION;
    }
    if (text.contains("nitrile") && text.contains("reduction")) {
      return ReactionMechanismFamily.REDUCTION;
    }
    if (text.contains("nitro") && text.contains("reduction")) {
      return ReactionMechanismFamily.REDUCTION;
    }
    if (text.contains("alcohol") && text.contains("oxidation")) {
      return ReactionMechanismFamily.OXIDATION;
    }
    if (text.contains("hydrolysis")) {
      return ReactionMechanismFamily.HYDROLYSIS;
    }
    if (text.contains("elimination")) {
      return ReactionMechanismFamily.ELIMINATION_E2;
    }
    if (text.contains("organometallic") && text.contains("addition")) {
      return ReactionMechanismFamily.GRIGNARD_OR_ORGANOLITHIUM_ADDITION;
    }
    if (text.contains("hydride") && text.contains("reduction")) {
      return ReactionMechanismFamily.HYDRIDE_REDUCTION;
    }
    if (text.contains("reductive") && text.contains("amination")) {
      return ReactionMechanismFamily.REDUCTIVE_AMINATION;
    }
    if (text.contains("cannizzaro")) {
      return ReactionMechanismFamily.CANNIZZARO_REACTION;
    }
    if (text.contains("transesterification")) {
      return ReactionMechanismFamily.TRANSESTERIFICATION;
    }
    if (text.contains("esterification")) {
      return ReactionMechanismFamily.ESTERIFICATION;
    }
    if (text.contains("michael")) {
      return ReactionMechanismFamily.MICHAEL_ADDITION;
    }
    if (text.contains("mannich")) {
      return ReactionMechanismFamily.MANNICH_REACTION;
    }
    if (text.contains("strecker")) {
      return ReactionMechanismFamily.STRECKER_SYNTHESIS;
    }
    if (text.contains("ugi")) {
      return ReactionMechanismFamily.UGI_REACTION;
    }
    if (text.contains("passerini")) {
      return ReactionMechanismFamily.PASSERINI_REACTION;
    }
    if (text.contains("knoevenagel")) {
      return ReactionMechanismFamily.KNOEVENAGEL_CONDENSATION;
    }
    if (text.contains("claisen")) {
      return ReactionMechanismFamily.CLAISEN_CONDENSATION;
    }
    if (text.contains("henry")) {
      return ReactionMechanismFamily.HENRY_REACTION;
    }
    if (text.contains("olefination")) {
      return ReactionMechanismFamily.OLEFINATION;
    }
    if (text.contains("robinson")) {
      return ReactionMechanismFamily.ROBINSON_ANNULATION;
    }
    if (text.contains("nazarov")) {
      return ReactionMechanismFamily.NAZAROV_CYCLIZATION;
    }
    if (text.contains("pauson") || text.contains("khand")) {
      return ReactionMechanismFamily.PAUSON_KHAND_REACTION;
    }
    if (text.contains("epoxidation")) {
      return ReactionMechanismFamily.EPOXIDATION;
    }
    if (text.contains("epoxide") && text.contains("opening")) {
      return ReactionMechanismFamily.EPOXIDE_OPENING;
    }
    if (text.contains("hydrogenation")) {
      return ReactionMechanismFamily.HYDROGENATION;
    }
    if (text.contains("hydration")) {
      return ReactionMechanismFamily.HYDRATION;
    }
    if (text.contains("hydroboration")) {
      return ReactionMechanismFamily.HYDROBORATION_OXIDATION;
    }
    if (text.contains("birch")) {
      return ReactionMechanismFamily.BIRCH_REDUCTION;
    }
    if (text.contains("aryne") || text.contains("hetaryne")) {
      return ReactionMechanismFamily.ARYNE_SUBSTITUTION;
    }
    if (text.contains("ring") && text.contains("closing") && text.contains("metathesis")) {
      return ReactionMechanismFamily.RING_CLOSING_METATHESIS;
    }
    if (text.contains("metathesis")) {
      return ReactionMechanismFamily.OLEFIN_METATHESIS;
    }
    if (text.contains("thiol") && text.contains("ene")) {
      return ReactionMechanismFamily.THIOL_ENE_CLICK;
    }
    if (text.contains("dipolar")) {
      return ReactionMechanismFamily.ONE_THREE_DIPOLAR_CYCLOADDITION;
    }
    if (text.contains("paal") || text.contains("knorr")) {
      return ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION;
    }
    if (text.contains("biginelli")) {
      return ReactionMechanismFamily.BIGINELLI_REACTION;
    }
    if (text.contains("gewald")) {
      return ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION;
    }
    if (text.contains("pictet") || text.contains("spengler")) {
      return ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION;
    }
    if (text.contains("hantzsch")) {
      return ReactionMechanismFamily.HANTZSCH_SYNTHESIS;
    }
    if (text.contains("skraup")) {
      return ReactionMechanismFamily.SKRAUP_SYNTHESIS;
    }
    if (text.contains("combes")) {
      return ReactionMechanismFamily.COMBES_QUINOLINE_ANNULATION;
    }
    if (text.contains("krohnke")) {
      return ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION;
    }
    if (text.contains("indole")) {
      return ReactionMechanismFamily.FISCHER_INDOLE_SYNTHESIS;
    }
    if (text.contains("minisci")) {
      return ReactionMechanismFamily.RADICAL_SUBSTITUTION;
    }
    if (text.contains("radical")) {
      return ReactionMechanismFamily.RADICAL_CHAIN;
    }
    if (text.contains("protection") || text.contains("deprotection")) {
      return ReactionMechanismFamily.PROTECTING_GROUP;
    }
    if (text.contains("baeyer") || text.contains("villiger")) {
      return ReactionMechanismFamily.BAEYER_VILLIGER;
    }
    if (text.contains("chichibabin") || text.contains("amination")) {
      return ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION;
    }
    if (text.contains("solvolysis")) {
      return ReactionMechanismFamily.SOLVOLYSIS;
    }
    return ReactionMechanismFamily.UNKNOWN;
  }
}

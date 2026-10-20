/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin;

import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleCategory;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.template.OrganicReactionTemplate;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.template.OrganicReactionTemplateId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.template.OrganicReactionTemplateLibrary;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;

/** Встроенные экспертные шаблоны для планирования органических и гетероциклических реакций. */
public final class BuiltInOrganicReactionTemplateLibraries {

  private BuiltInOrganicReactionTemplateLibraries() {}

  public static OrganicReactionTemplateLibrary heterocyclePlanning() {
    return OrganicReactionTemplateLibrary.of(
        List.of(
            BuiltInOrganicReactionTemplateLibraries.heteroareneFunctionalization(),
            BuiltInOrganicReactionTemplateLibraries.halogenatedHeterocycleCrossCoupling(),
            BuiltInOrganicReactionTemplateLibraries.heteroarylSnAr(),
            BuiltInOrganicReactionTemplateLibraries.heteroareneCHFunctionalization(),
            BuiltInOrganicReactionTemplateLibraries.heterocycleNOxidation(),
            BuiltInOrganicReactionTemplateLibraries.heterocycleNAlkylation(),
            BuiltInOrganicReactionTemplateLibraries.directedMetalation(),
            BuiltInOrganicReactionTemplateLibraries.carbonylNitrogenCyclization(),
            BuiltInOrganicReactionTemplateLibraries.paalKnorrHeterocycleSynthesis(),
            BuiltInOrganicReactionTemplateLibraries.azoleCycloaddition(),
            BuiltInOrganicReactionTemplateLibraries.imineCyclodehydration(),
            BuiltInOrganicReactionTemplateLibraries.acidBaseProtonationScreen(),
            BuiltInOrganicReactionTemplateLibraries.redoxSensitiveHeterocycleScreen(),
            BuiltInOrganicReactionTemplateLibraries.carbonylAdditionScreen(),
            BuiltInOrganicReactionTemplateLibraries.carbonylAcylSubstitutionScreen(),
            BuiltInOrganicReactionTemplateLibraries.conjugateAdditionScreen(),
            BuiltInOrganicReactionTemplateLibraries.mannichCondensationScreen(),
            BuiltInOrganicReactionTemplateLibraries.knoevenagelCondensationScreen(),
            BuiltInOrganicReactionTemplateLibraries.alkeneElectrophilicAdditionScreen(),
            BuiltInOrganicReactionTemplateLibraries.epoxideOpeningScreen(),
            BuiltInOrganicReactionTemplateLibraries.radicalChainFunctionalizationScreen(),
            BuiltInOrganicReactionTemplateLibraries.photoredoxFunctionalizationScreen(),
            BuiltInOrganicReactionTemplateLibraries.protectionDeprotectionScreen(),
            BuiltInOrganicReactionTemplateLibraries.rearrangementScreen()));
  }

  private static OrganicReactionTemplate heteroareneFunctionalization() {
    return BuiltInOrganicReactionTemplateLibraries.template(
        "template_heteroarene_functionalization",
        "Heteroarene functionalization",
        OrganicReactionRuleCategory.HETEROCYCLE_FUNCTIONALIZATION,
        List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
        List.of(MolecularFeatureKind.HETEROCYCLE, MolecularFeatureKind.AROMATIC_RING),
        List.of(ReactionConditionFeatureKind.ANHYDROUS, ReactionConditionFeatureKind.HEATING),
        0.72,
        "Aromatic heterocycle is present; electrophilic, nucleophilic, or metal-mediated functionalization may apply.");
  }

  private static OrganicReactionTemplate halogenatedHeterocycleCrossCoupling() {
    return BuiltInOrganicReactionTemplateLibraries.template(
        "template_halogenated_heterocycle_cross_coupling",
        "Halogenated heterocycle cross-coupling",
        OrganicReactionRuleCategory.COUPLING,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
            OrganicReactionDomain.CROSS_COUPLING),
        List.of(MolecularFeatureKind.HETEROCYCLE, MolecularFeatureKind.HALOGENATED),
        List.of(
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionConditionFeatureKind.INERT_ATMOSPHERE,
            ReactionConditionFeatureKind.ANHYDROUS),
        0.76,
        "Halogenated heterocycle is present; Suzuki, Buchwald, Negishi, Stille, or related coupling screens are plausible.");
  }

  private static OrganicReactionTemplate heteroarylSnAr() {
    return BuiltInOrganicReactionTemplateLibraries.template(
        "template_heteroaryl_snar",
        "Heteroaryl nucleophilic aromatic substitution",
        OrganicReactionRuleCategory.SUBSTITUTION,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
            OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
        List.of(
            MolecularFeatureKind.HETEROCYCLE,
            MolecularFeatureKind.HALOGENATED,
            MolecularFeatureKind.NITROGEN_HETEROCYCLE),
        List.of(
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.ANHYDROUS),
        0.74,
        "Electron-poor halogenated nitrogen heterocycle is present; SNAr displacement should be considered.");
  }

  private static OrganicReactionTemplate heteroareneCHFunctionalization() {
    return BuiltInOrganicReactionTemplateLibraries.template(
        "template_heteroarene_c_h_functionalization",
        "Heteroarene C-H functionalization",
        OrganicReactionRuleCategory.HETEROCYCLE_FUNCTIONALIZATION,
        List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
        List.of(MolecularFeatureKind.HETEROCYCLE, MolecularFeatureKind.AROMATIC_RING),
        List.of(
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionConditionFeatureKind.PHOTOCHEMICAL,
            ReactionConditionFeatureKind.HEATING),
        0.63,
        "Aromatic heterocycle is present; direct C-H functionalization or oxidative coupling may be screened.");
  }

  private static OrganicReactionTemplate heterocycleNOxidation() {
    return BuiltInOrganicReactionTemplateLibraries.template(
        "template_heterocycle_n_oxidation",
        "Nitrogen heterocycle N-oxidation",
        OrganicReactionRuleCategory.OXIDATION,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
            OrganicReactionDomain.OXIDATION_REDUCTION),
        List.of(MolecularFeatureKind.NITROGEN_HETEROCYCLE, MolecularFeatureKind.AROMATIC_RING),
        List.of(
            ReactionConditionFeatureKind.OXIDIZING_MEDIUM,
            ReactionConditionFeatureKind.COOLING,
            ReactionConditionFeatureKind.ANHYDROUS),
        0.69,
        "Aromatic nitrogen heterocycle is present; N-oxide formation should be considered when oxidant context exists.");
  }

  private static OrganicReactionTemplate heterocycleNAlkylation() {
    return BuiltInOrganicReactionTemplateLibraries.template(
        "template_heterocycle_n_alkylation",
        "Heterocycle N-alkylation or quaternization",
        OrganicReactionRuleCategory.SUBSTITUTION,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
            OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
        List.of(MolecularFeatureKind.NITROGEN_HETEROCYCLE),
        List.of(
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.ADDITION_RATE),
        0.67,
        "Nitrogen heterocycle is present; N-alkylation, salt formation, or quaternization pathway may compete.");
  }

  private static OrganicReactionTemplate directedMetalation() {
    return BuiltInOrganicReactionTemplateLibraries.template(
        "template_directed_heteroarene_metalation",
        "Directed heteroarene metalation",
        OrganicReactionRuleCategory.HETEROCYCLE_FUNCTIONALIZATION,
        List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
        List.of(MolecularFeatureKind.HETEROCYCLE, MolecularFeatureKind.AROMATIC_RING),
        List.of(
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.COOLING,
            ReactionConditionFeatureKind.ANHYDROUS,
            ReactionConditionFeatureKind.INERT_ATMOSPHERE),
        0.61,
        "Heteroarene can direct strong-base metalation; low-temperature anhydrous conditions are important.");
  }

  private static OrganicReactionTemplate carbonylNitrogenCyclization() {
    return BuiltInOrganicReactionTemplateLibraries.template(
        "template_carbonyl_nitrogen_cyclization",
        "Carbonyl/nitrogen heterocycle cyclization",
        OrganicReactionRuleCategory.HETEROCYCLE_SYNTHESIS,
        List.of(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS, OrganicReactionDomain.CYCLIZATION),
        List.of(MolecularFeatureKind.CARBONYL, MolecularFeatureKind.AMINE),
        List.of(
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.DEHYDRATING_AGENT,
            ReactionConditionFeatureKind.ACIDIC_MEDIUM),
        0.64,
        "Carbonyl and nitrogen nucleophile motifs are present for heterocycle-forming condensation.");
  }

  private static OrganicReactionTemplate paalKnorrHeterocycleSynthesis() {
    return BuiltInOrganicReactionTemplateLibraries.template(
        "template_paal_knorr_heterocycle_synthesis",
        "Paal-Knorr-style heterocycle synthesis",
        OrganicReactionRuleCategory.HETEROCYCLE_SYNTHESIS,
        List.of(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS, OrganicReactionDomain.CYCLIZATION),
        List.of(MolecularFeatureKind.CARBONYL),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.DEHYDRATING_AGENT),
        0.58,
        "Carbonyl-rich precursor context is present; Paal-Knorr-style furan, pyrrole, or thiophene synthesis may be screened.");
  }

  private static OrganicReactionTemplate azoleCycloaddition() {
    return BuiltInOrganicReactionTemplateLibraries.template(
        "template_azole_cycloaddition",
        "Azole-forming cycloaddition",
        OrganicReactionRuleCategory.HETEROCYCLE_SYNTHESIS,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
            OrganicReactionDomain.CYCLIZATION,
            OrganicReactionDomain.ADDITION),
        List.of(MolecularFeatureKind.ALKYNE, MolecularFeatureKind.NITRILE),
        List.of(
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.FLOW_REACTOR),
        0.57,
        "Alkyne and nitrile-like unsaturation are present; azole-forming cycloaddition screens are plausible.");
  }

  private static OrganicReactionTemplate imineCyclodehydration() {
    return BuiltInOrganicReactionTemplateLibraries.template(
        "template_imine_cyclodehydration",
        "Imine cyclodehydration",
        OrganicReactionRuleCategory.HETEROCYCLE_SYNTHESIS,
        List.of(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS, OrganicReactionDomain.CYCLIZATION),
        List.of(MolecularFeatureKind.IMINE, MolecularFeatureKind.AMINE),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.DEHYDRATING_AGENT,
            ReactionConditionFeatureKind.HEATING),
        0.56,
        "Imine and amine motifs are present; cyclodehydration to nitrogen heterocycles may apply.");
  }

  private static OrganicReactionTemplate acidBaseProtonationScreen() {
    return BuiltInOrganicReactionTemplateLibraries.template(
        "template_heterocycle_acid_base_protonation",
        "Heterocycle protonation and tautomer screen",
        OrganicReactionRuleCategory.ACID_BASE,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION, OrganicReactionDomain.ACID_BASE),
        List.of(MolecularFeatureKind.NITROGEN_HETEROCYCLE),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.CUSTOM),
        0.62,
        "Nitrogen heterocycle is present; protonation, salt formation, and tautomeric state should be screened.");
  }

  private static OrganicReactionTemplate redoxSensitiveHeterocycleScreen() {
    return BuiltInOrganicReactionTemplateLibraries.template(
        "template_redox_sensitive_heterocycle",
        "Redox-sensitive heterocycle screen",
        OrganicReactionRuleCategory.OXIDATION,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
            OrganicReactionDomain.OXIDATION_REDUCTION),
        List.of(MolecularFeatureKind.HETEROCYCLE),
        List.of(
            ReactionConditionFeatureKind.ELECTROCHEMICAL,
            ReactionConditionFeatureKind.PHOTOCHEMICAL,
            ReactionConditionFeatureKind.OXIDIZING_MEDIUM,
            ReactionConditionFeatureKind.REDUCING_MEDIUM),
        0.55,
        "Heterocycle is present under redox-like activation context; oxidation, reduction, or radical pathways should be screened.");
  }

  private static OrganicReactionTemplate carbonylAdditionScreen() {
    return BuiltInOrganicReactionTemplateLibraries.template(
        "template_carbonyl_nucleophilic_addition",
        "Carbonyl nucleophilic addition",
        OrganicReactionRuleCategory.ADDITION,
        List.of(OrganicReactionDomain.ADDITION),
        List.of(MolecularFeatureKind.CARBONYL),
        List.of(
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.ANHYDROUS),
        0.66,
        "Carbonyl electrophile is present; organometallic, hydride, cyanide, amine, alcohol or water addition channels should be ranked.");
  }

  private static OrganicReactionTemplate carbonylAcylSubstitutionScreen() {
    return BuiltInOrganicReactionTemplateLibraries.template(
        "template_carboxyl_derivative_acyl_substitution",
        "Carboxyl derivative acyl substitution",
        OrganicReactionRuleCategory.SUBSTITUTION,
        List.of(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
        List.of(MolecularFeatureKind.CARBOXYL_DERIVATIVE),
        List.of(
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.ADDITION_RATE),
        0.64,
        "Carboxyl derivative is present; aminolysis, alcoholysis, hydrolysis and mixed anhydride transfer channels should compete.");
  }

  private static OrganicReactionTemplate conjugateAdditionScreen() {
    return BuiltInOrganicReactionTemplateLibraries.template(
        "template_conjugate_michael_addition",
        "Conjugate Michael addition",
        OrganicReactionRuleCategory.ADDITION,
        List.of(OrganicReactionDomain.ADDITION),
        List.of(MolecularFeatureKind.ALKENE, MolecularFeatureKind.ELECTRON_WITHDRAWING_GROUP),
        List.of(
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionConditionFeatureKind.HEATING),
        0.63,
        "Activated alkene is present; soft nucleophile conjugate addition and follow-up proton transfer should be screened.");
  }

  private static OrganicReactionTemplate mannichCondensationScreen() {
    return BuiltInOrganicReactionTemplateLibraries.template(
        "template_mannich_condensation",
        "Mannich condensation",
        OrganicReactionRuleCategory.ADDITION,
        List.of(OrganicReactionDomain.ADDITION),
        List.of(MolecularFeatureKind.AMINE, MolecularFeatureKind.CARBONYL),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.ADDITION_RATE),
        0.59,
        "Amine and carbonyl motifs are present; iminium formation and carbon nucleophile capture may produce Mannich products.");
  }

  private static OrganicReactionTemplate knoevenagelCondensationScreen() {
    return BuiltInOrganicReactionTemplateLibraries.template(
        "template_knoevenagel_condensation",
        "Knoevenagel condensation",
        OrganicReactionRuleCategory.ADDITION,
        List.of(OrganicReactionDomain.ADDITION, OrganicReactionDomain.ELIMINATION),
        List.of(MolecularFeatureKind.CARBONYL, MolecularFeatureKind.ACTIVATED_NUCLEOPHILE),
        List.of(
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.DEHYDRATING_AGENT),
        0.58,
        "Carbonyl and activated methylene-like nucleophile are present; addition/dehydration to alkene products should be considered.");
  }

  private static OrganicReactionTemplate alkeneElectrophilicAdditionScreen() {
    return BuiltInOrganicReactionTemplateLibraries.template(
        "template_alkene_electrophilic_addition",
        "Alkene electrophilic addition",
        OrganicReactionRuleCategory.ADDITION,
        List.of(OrganicReactionDomain.ADDITION),
        List.of(MolecularFeatureKind.ALKENE),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.OXIDIZING_MEDIUM,
            ReactionConditionFeatureKind.HYDROGENATION,
            ReactionConditionFeatureKind.COOLING),
        0.57,
        "Alkene is present; hydrohalogenation, hydration, halogenation, epoxidation or hydroboration channels can compete.");
  }

  private static OrganicReactionTemplate epoxideOpeningScreen() {
    return BuiltInOrganicReactionTemplateLibraries.template(
        "template_epoxide_opening",
        "Epoxide opening",
        OrganicReactionRuleCategory.ADDITION,
        List.of(OrganicReactionDomain.ADDITION),
        List.of(MolecularFeatureKind.EPOXIDE),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.ANHYDROUS),
        0.54,
        "Strained ether/charged-center context is present; acid- or base-directed epoxide opening should be ranked when graph features match.");
  }

  private static OrganicReactionTemplate radicalChainFunctionalizationScreen() {
    return BuiltInOrganicReactionTemplateLibraries.template(
        "template_radical_chain_functionalization",
        "Radical chain functionalization",
        OrganicReactionRuleCategory.RADICAL_FUNCTIONALIZATION,
        List.of(OrganicReactionDomain.RADICAL_FUNCTIONALIZATION),
        List.of(
            MolecularFeatureKind.RADICAL_CENTER, MolecularFeatureKind.BENZYL_OR_BENZYLIC_FRAGMENT),
        List.of(
            ReactionConditionFeatureKind.PHOTOCHEMICAL,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.INERT_ATMOSPHERE),
        0.55,
        "Radical or benzylic motif is present; initiation, propagation and termination channels should be enumerated.");
  }

  private static OrganicReactionTemplate photoredoxFunctionalizationScreen() {
    return BuiltInOrganicReactionTemplateLibraries.template(
        "template_photoredox_functionalization",
        "Photoredox functionalization",
        OrganicReactionRuleCategory.RADICAL_FUNCTIONALIZATION,
        List.of(
            OrganicReactionDomain.RADICAL_FUNCTIONALIZATION,
            OrganicReactionDomain.OXIDATION_REDUCTION),
        List.of(MolecularFeatureKind.HETEROCYCLE, MolecularFeatureKind.ELECTRON_WITHDRAWING_GROUP),
        List.of(
            ReactionConditionFeatureKind.PHOTOCHEMICAL,
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionConditionFeatureKind.INERT_ATMOSPHERE),
        0.56,
        "Photoredox-like condition and acceptor heterocycle context are present; radical addition or single-electron-transfer pathways should compete.");
  }

  private static OrganicReactionTemplate protectionDeprotectionScreen() {
    return BuiltInOrganicReactionTemplateLibraries.template(
        "template_protection_deprotection",
        "Protection and deprotection",
        OrganicReactionRuleCategory.PROTECTION,
        List.of(OrganicReactionDomain.PROTECTION_DEPROTECTION),
        List.of(MolecularFeatureKind.HYDROXYL_OR_ALKOXY, MolecularFeatureKind.AMINE),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.WORKUP),
        0.53,
        "Alcohol or amine motif is present; protecting group installation, cleavage and side reaction channels should be screened.");
  }

  private static OrganicReactionTemplate rearrangementScreen() {
    return BuiltInOrganicReactionTemplateLibraries.template(
        "template_rearrangement_screen",
        "Thermal, photochemical or cationic rearrangement",
        OrganicReactionRuleCategory.REARRANGEMENT,
        List.of(OrganicReactionDomain.REARRANGEMENT),
        List.of(MolecularFeatureKind.CARBONYL, MolecularFeatureKind.ALKENE),
        List.of(
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.PHOTOCHEMICAL,
            ReactionConditionFeatureKind.ACIDIC_MEDIUM),
        0.52,
        "Unsaturated or carbonyl framework under activation conditions can rearrange; sigmatropic, cationic and photochemical variants should be scored.");
  }

  private static OrganicReactionTemplate template(
      final String id,
      final String displayName,
      final OrganicReactionRuleCategory category,
      final List<OrganicReactionDomain> domains,
      final List<MolecularFeatureKind> requiredFeatures,
      final List<ReactionConditionFeatureKind> supportiveConditions,
      final double confidence,
      final String explanation) {
    return OrganicReactionTemplate.of(
        OrganicReactionTemplateId.of(id),
        displayName,
        category,
        domains,
        requiredFeatures,
        supportiveConditions,
        confidence,
        explanation);
  }
}

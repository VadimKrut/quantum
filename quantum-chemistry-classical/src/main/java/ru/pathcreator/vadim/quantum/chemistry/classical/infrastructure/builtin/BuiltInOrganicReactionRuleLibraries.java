/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.rule.BetaEliminationOrganicReactionRule;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.rule.BondCleavageOrganicReactionRule;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.rule.BondInsertionOrganicReactionRule;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.rule.BondAdditionOrganicReactionRule;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.rule.BridgeAdditionOrganicReactionRule;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.rule.FragmentAttachmentOrganicReactionRule;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.rule.FragmentTransferOrganicReactionRule;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.rule.IntramolecularBondFormationOrganicReactionRule;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.rule.ReplacingLeavingGroupOrganicReactionRule;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.rule.NucleophilicAmineLeavingGroupReplacementRule;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.rule.NitroReductionOrganicReactionRule;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.rule.PhotochemicalHeterocycleRearrangementRule;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.rule.RadicalRecombinationOrganicReactionRule;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.rule.TransformingSubstituentOrganicReactionRule;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.template.OrganicReactionTemplateMatcher;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.template.TemplateBackedOrganicReactionRule;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRule;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleCategory;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleLibrary;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.template.OrganicReactionTemplate;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.template.OrganicReactionTemplateLibrary;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.BetaEliminationTransform;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.BondCleavageTransform;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.BondInsertionTransform;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.BondAdditionTransform;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.BridgeAdditionTransform;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.FragmentAttachmentTransform;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.FragmentTransferTransform;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.IntramolecularBondFormationTransform;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.LeavingGroupReplacementTransform;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.ReactantAtomSelector;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.SubstituentAttachmentTransform;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;

/** Встроенные Java-native rule libraries для organic prediction pipeline. */
public final class BuiltInOrganicReactionRuleLibraries {

  private BuiltInOrganicReactionRuleLibraries() {}

  public static OrganicReactionRuleLibrary heterocyclePlanning() {
    return BuiltInOrganicReactionRuleLibraries.fromTemplates(
        BuiltInOrganicReactionTemplateLibraries.heterocyclePlanning());
  }

  public static OrganicReactionRuleLibrary heterocycleProductPrediction() {
    return BuiltInOrganicReactionRuleLibraries.comprehensiveOrganicProductPrediction();
  }

  public static OrganicReactionRuleLibrary comprehensiveOrganicProductPrediction() {
    return BuiltInOrganicReactionRuleLibraries.fromRulesAndTemplates(
        List.of(
            RadicalRecombinationOrganicReactionRule.diatomicSingleBond(),
            BuiltInOrganicReactionRuleLibraries.heteroarylHydroxylationTransform(),
            BuiltInOrganicReactionRuleLibraries.chichibabinAzaHeteroareneAminationTransform(),
            NucleophilicAmineLeavingGroupReplacementRule.heteroarylHalideAmination(),
            NucleophilicAmineLeavingGroupReplacementRule.anrorcAzaHeterocycleAmination(),
            BuiltInOrganicReactionRuleLibraries.heteroarylHalideGraphFragmentCoupling(),
            BuiltInOrganicReactionRuleLibraries.heteroarylIodideSuzukiCarbonReplacement(),
            BuiltInOrganicReactionRuleLibraries.heteroarylSuzukiCarbonReplacement(),
            BuiltInOrganicReactionRuleLibraries.heteroarylHeckAlkenylationTransform(),
            BuiltInOrganicReactionRuleLibraries.heteroarylHeckGraphAlkenylation(),
            BuiltInOrganicReactionRuleLibraries.heteroarylSonogashiraAlkynylationTransform(),
            BuiltInOrganicReactionRuleLibraries.heteroarylSonogashiraGraphAlkynylation(),
            BuiltInOrganicReactionRuleLibraries.heteroarylBuchwaldUllmannAminationTransform(),
            BuiltInOrganicReactionRuleLibraries.heteroarylBuchwaldUllmannGraphAmination(),
            BuiltInOrganicReactionRuleLibraries.heteroarylChlorideHydrodehalogenationSideChannel(),
            BuiltInOrganicReactionRuleLibraries.heteroarylHydrodehalogenationSideChannel(),
            BuiltInOrganicReactionRuleLibraries.heteroarylSolvolysisHydroxyReplacement(),
            BuiltInOrganicReactionRuleLibraries.heteroarylFluorideAlkoxyReplacement(),
            BuiltInOrganicReactionRuleLibraries.heteroarylBorylationTransform(),
            BuiltInOrganicReactionRuleLibraries.heteroarylSuzukiGraphBoronateCoupling(),
            BuiltInOrganicReactionRuleLibraries.heteroarylProtodeboronationSideChannel(),
            BuiltInOrganicReactionRuleLibraries.heteroarylHalogenationTransform(),
            BuiltInOrganicReactionRuleLibraries.aromaticNitrationTransform(),
            BuiltInOrganicReactionRuleLibraries.aromaticSulfonationTransform(),
            BuiltInOrganicReactionRuleLibraries.friedelCraftsAlkylationTransform(),
            BuiltInOrganicReactionRuleLibraries.arylDiazoniumHydroxyReplacement(),
            BuiltInOrganicReactionRuleLibraries.arylDiazoniumHalideReplacement(),
            BuiltInOrganicReactionRuleLibraries.azoCouplingNitrogenAttachment(),
            BuiltInOrganicReactionRuleLibraries.nitrogenHeterocycleNOxidationTransform(),
            BuiltInOrganicReactionRuleLibraries.nitrogenHeterocycleNAlkylationTransform(),
            BuiltInOrganicReactionRuleLibraries.nitrogenHeterocycleGraphNAlkylation(),
            BuiltInOrganicReactionRuleLibraries.heteroarylNitrileHydrationSideChannel(),
            BuiltInOrganicReactionRuleLibraries.heteroarylNitrileGraphHydrationSideChannel(),
            BuiltInOrganicReactionRuleLibraries.heteroarylNitrileReductionSideChannel(),
            BuiltInOrganicReactionRuleLibraries.heteroarylNitrileGraphReductionSideChannel(),
            BuiltInOrganicReactionRuleLibraries.sulfurHeterocycleOxidationTransform(),
            BuiltInOrganicReactionRuleLibraries.nitroReductionTransform(),
            NitroReductionOrganicReactionRule.graphNitroToAmine(),
            BuiltInOrganicReactionRuleLibraries.alcoholOxidationTransform(),
            BuiltInOrganicReactionRuleLibraries.amideHydrolysisTransform(),
            BuiltInOrganicReactionRuleLibraries.betaEliminationGraphUnsaturation(),
            BuiltInOrganicReactionRuleLibraries.carbonylOrganometallicAdditionTransform(),
            BuiltInOrganicReactionRuleLibraries.carbonylGraphOrganometallicAddition(),
            new ru.pathcreator.vadim.quantum.chemistry.classical.application.rule
                .CarbonylHydrogenationOrganicReactionRule(),
            BuiltInOrganicReactionRuleLibraries.carbonylReductiveAminationTransform(),
            BuiltInOrganicReactionRuleLibraries.carbonylGraphReductiveAmination(),
            BuiltInOrganicReactionRuleLibraries.carbonylGraphImineCondensation(),
            BuiltInOrganicReactionRuleLibraries.carbonylAmineGraphCyclization(),
            BuiltInOrganicReactionRuleLibraries.carbonylCannizzaroDisproportionationTransform(),
            BuiltInOrganicReactionRuleLibraries.acylSubstitutionAminolysisTransform(),
            BuiltInOrganicReactionRuleLibraries.acylGraphAminolysisFragmentTransfer(),
            BuiltInOrganicReactionRuleLibraries.carboxylEsterificationTransform(),
            BuiltInOrganicReactionRuleLibraries.esterTransesterificationTransform(),
            BuiltInOrganicReactionRuleLibraries.claisenLikeCondensationTransform(),
            BuiltInOrganicReactionRuleLibraries.michaelAdditionCarbonTransform(),
            BuiltInOrganicReactionRuleLibraries.mannichAminomethylationTransform(),
            BuiltInOrganicReactionRuleLibraries.streckerAminonitrileSynthesisTransform(),
            BuiltInOrganicReactionRuleLibraries.ugiMulticomponentAmideTransform(),
            BuiltInOrganicReactionRuleLibraries.passeriniMulticomponentEsterTransform(),
            BuiltInOrganicReactionRuleLibraries.knoevenagelCondensationTransform(),
            BuiltInOrganicReactionRuleLibraries.henryNitroaldolTransform(),
            BuiltInOrganicReactionRuleLibraries.olefinationCarbonylTransform(),
            BuiltInOrganicReactionRuleLibraries.robinsonAnnulationTransform(),
            BuiltInOrganicReactionRuleLibraries.nazarovCyclizationTransform(),
            BuiltInOrganicReactionRuleLibraries.pausonKhandCyclopentenoneTransform(),
            BuiltInOrganicReactionRuleLibraries.alkeneGraphEpoxidationBridge(),
            BuiltInOrganicReactionRuleLibraries.alkeneGraphAziridinationBridge(),
            BuiltInOrganicReactionRuleLibraries.epoxideOpeningTransform(),
            BuiltInOrganicReactionRuleLibraries.alkeneGraphHydrogenationAddition(),
            BuiltInOrganicReactionRuleLibraries.alkeneGraphHydrationAddition(),
            BuiltInOrganicReactionRuleLibraries.alkeneGraphHydrohalogenationAddition(),
            BuiltInOrganicReactionRuleLibraries.alkeneGraphHydroborationOxidationAddition(),
            BuiltInOrganicReactionRuleLibraries.birchAreneReductionTransform(),
            BuiltInOrganicReactionRuleLibraries.aryneSubstitutionTransform(),
            BuiltInOrganicReactionRuleLibraries.olefinMetathesisTransform(),
            BuiltInOrganicReactionRuleLibraries.ringClosingMetathesisTransform(),
            BuiltInOrganicReactionRuleLibraries.thiolEneClickTransform(),
            BuiltInOrganicReactionRuleLibraries.alkeneGraphThiolEneClickAddition(),
            PhotochemicalHeterocycleRearrangementRule.heterocycleBondOrderIsomerization(),
            BuiltInOrganicReactionRuleLibraries.oneThreeDipolarHeterocycleConstructionTransform(),
            BuiltInOrganicReactionRuleLibraries.dielsAlderCycloadditionBridge(),
            BuiltInOrganicReactionRuleLibraries.azaDielsAlderCycloadditionTransform(),
            BuiltInOrganicReactionRuleLibraries.paalKnorrHeterocycleConstructionTransform(),
            BuiltInOrganicReactionRuleLibraries.biginelliDihydropyrimidinoneConstructionTransform(),
            BuiltInOrganicReactionRuleLibraries.gewaldThiopheneConstructionTransform(),
            BuiltInOrganicReactionRuleLibraries.fischerIndoleConstructionTransform(),
            BuiltInOrganicReactionRuleLibraries.pictetSpenglerCyclizationTransform(),
            BuiltInOrganicReactionRuleLibraries.skraupQuinolineConstructionTransform(),
            BuiltInOrganicReactionRuleLibraries.combesQuinolineAnnulationTransform(),
            BuiltInOrganicReactionRuleLibraries.hantzschLikePyridineConstructionTransform(),
            BuiltInOrganicReactionRuleLibraries.krohnkeLikePyridineConstructionTransform(),
            BuiltInOrganicReactionRuleLibraries.indoleDearomatizationAnnulationTransform(),
            BuiltInOrganicReactionRuleLibraries
                .minisciNitrogenHeterocycleCarbonFunctionalizationTransform(),
            BuiltInOrganicReactionRuleLibraries.radicalCarbonFunctionalizationTransform(),
            BuiltInOrganicReactionRuleLibraries.alcoholProtectionTransform(),
            BuiltInOrganicReactionRuleLibraries.etherDeprotectionTransform(),
            BuiltInOrganicReactionRuleLibraries.etherGraphDeprotectionCleavage(),
            BuiltInOrganicReactionRuleLibraries.carbonylGraphBaeyerVilligerOxygenInsertion()),
        BuiltInOrganicReactionTemplateLibraries.heterocyclePlanning());
  }

  public static OrganicReactionRuleLibrary fromRulesAndTemplates(
      final List<OrganicReactionRule> explicitRules,
      final OrganicReactionTemplateLibrary templateLibrary) {
    if (explicitRules == null) {
      throw new IllegalArgumentException("Explicit organic reaction rule list must not be null.");
    }
    final OrganicReactionRuleLibrary templateRules =
        BuiltInOrganicReactionRuleLibraries.fromTemplates(templateLibrary);
    final ArrayList<OrganicReactionRule> rules =
        new ArrayList<OrganicReactionRule>(explicitRules.size() + templateRules.size());
    for (int i = 0; i < explicitRules.size(); ++i) {
      final OrganicReactionRule rule = explicitRules.get(i);
      if (rule == null) {
        throw new IllegalArgumentException("Explicit organic reaction rule must not be null.");
      }
      rules.add(rule);
    }
    rules.addAll(templateRules.rules());
    return OrganicReactionRuleLibrary.of(rules);
  }

  public static OrganicReactionRuleLibrary fromTemplates(
      final OrganicReactionTemplateLibrary templateLibrary) {
    if (templateLibrary == null) {
      throw new IllegalArgumentException("Organic reaction template library must not be null.");
    }
    if (templateLibrary.empty()) {
      return OrganicReactionRuleLibrary.EMPTY;
    }
    final OrganicReactionTemplateMatcher matcher = new OrganicReactionTemplateMatcher();
    final List<OrganicReactionTemplate> templates = templateLibrary.templates();
    final ArrayList<OrganicReactionRule> rules =
        new ArrayList<OrganicReactionRule>(templates.size());
    for (int i = 0; i < templates.size(); ++i) {
      rules.add(new TemplateBackedOrganicReactionRule(templates.get(i), matcher));
    }
    return OrganicReactionRuleLibrary.of(rules);
  }

  private static OrganicReactionRule heteroarylHydroxylationTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_heteroaryl_hydroxylation"),
        "Heteroaryl hydroxylation transform",
        OrganicReactionRuleCategory.HETEROCYCLE_FUNCTIONALIZATION,
        List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
        List.of(MolecularFeatureKind.HETEROCYCLE, MolecularFeatureKind.AROMATIC_RING),
        List.of(
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionConditionFeatureKind.PHOTOCHEMICAL,
            ReactionConditionFeatureKind.HEATING),
        SubstituentAttachmentTransform.of(
            "oh_product",
            "hydroxy analogue",
            ElementSymbol.of("O"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds an oxygen substituent to each matching aromatic carbon as a controlled product transform."),
        0.42,
        35.0,
        "Regioisomer set generated from all matching heteroaryl carbon sites before ranking.",
        "Heteroarene functionalization transform generated a concrete product candidate.");
  }

  private static OrganicReactionRule chichibabinAzaHeteroareneAminationTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_chichibabin_aza_heteroarene_amination"),
        "Chichibabin aza-heteroarene C-H amination",
        OrganicReactionRuleCategory.SUBSTITUTION,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
            OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
        List.of(
            MolecularFeatureKind.HETEROCYCLE,
            MolecularFeatureKind.NITROGEN_HETEROCYCLE,
            MolecularFeatureKind.AROMATIC_RING),
        List.of(
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.ANHYDROUS),
        SubstituentAttachmentTransform.of(
            "chichibabin_nh2_product",
            "Chichibabin aminated analogue",
            ElementSymbol.of("N"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds a nitrogen substituent to every matching aza-heteroarene carbon as an SNH/addition-elimination amination channel."),
        0.38,
        42.0,
        "Chichibabin/SNH channel: generated from aza-heteroarene graph and strong basic amination conditions, not from benchmark lookup.",
        "Electron-deficient aza-heteroarene can undergo nucleophilic C-H amination through sigma-adduct/hydride-loss chemistry.");
  }

  private static OrganicReactionRule heteroarylBorylationTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_heteroaryl_borylation"),
        "Heteroaryl borylation transform",
        OrganicReactionRuleCategory.COUPLING,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
            OrganicReactionDomain.CROSS_COUPLING),
        List.of(MolecularFeatureKind.HETEROCYCLE, MolecularFeatureKind.AROMATIC_RING),
        List.of(
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionConditionFeatureKind.INERT_ATMOSPHERE,
            ReactionConditionFeatureKind.ANHYDROUS,
            ReactionConditionFeatureKind.HEATING),
        SubstituentAttachmentTransform.of(
            "boryl_product",
            "borylated analogue",
            ElementSymbol.of("B"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds a boron substituent to a heteroaryl carbon as a cross-coupling-ready product transform."),
        0.34,
        52.0,
        "Borylation regioisomer set generated from matching heteroaryl carbon sites before ranking.",
        "Catalytic heteroaryl C-H borylation or halogen-borylation planning rule.");
  }

  private static OrganicReactionRule heteroarylHalideGraphFragmentCoupling() {
    return FragmentTransferOrganicReactionRule.of(
        OrganicReactionRuleId.of("fragment_transfer_heteroaryl_halide_carbon_coupling"),
        "Heteroaryl halide graph fragment coupling",
        OrganicReactionRuleCategory.COUPLING,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
            OrganicReactionDomain.CROSS_COUPLING),
        List.of(
            MolecularFeatureKind.HETEROCYCLE,
            MolecularFeatureKind.HALOGENATED,
            MolecularFeatureKind.AROMATIC_RING),
        List.of(MolecularFeatureKind.HALOGENATED),
        List.of(
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.INERT_ATMOSPHERE,
            ReactionConditionFeatureKind.ANHYDROUS,
            ReactionConditionFeatureKind.HEATING),
        FragmentTransferTransform.withCoproduct(
            "carbon_fragment_coupled",
            "carbon-fragment coupled analogue",
            ReactantAtomSelector.element("C"),
            ReactantAtomSelector.halogen(),
            ReactantAtomSelector.element("C"),
            ReactantAtomSelector.halogen(),
            BondType.SINGLE,
            "dihalogen coproduct",
            BondType.SINGLE,
            "Removes halogen leaving groups from a heteroaryl scaffold and a carbon fragment, then creates a graph-derived C-C coupling product."),
        0.57,
        61.0,
        "Cross-coupling regioisomer set is generated by actual graph fragment transfer before ranking.",
        "Palladium/nickel-like cross-coupling can join a halogenated heteroaryl scaffold with a carbon fragment.");
  }

  private static OrganicReactionRule heteroarylSuzukiCarbonReplacement() {
    return ReplacingLeavingGroupOrganicReactionRule.of(
        OrganicReactionRuleId.of("replace_heteroaryl_bromide_suzuki_carbon"),
        "Heteroaryl bromide Suzuki carbon replacement",
        OrganicReactionRuleCategory.COUPLING,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
            OrganicReactionDomain.CROSS_COUPLING),
        List.of(
            MolecularFeatureKind.HETEROCYCLE,
            MolecularFeatureKind.HALOGENATED,
            MolecularFeatureKind.AROMATIC_RING),
        List.of(
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionConditionFeatureKind.INERT_ATMOSPHERE,
            ReactionConditionFeatureKind.ANHYDROUS,
            ReactionConditionFeatureKind.HEATING),
        LeavingGroupReplacementTransform.of(
            "suzuki_carbon_product",
            "carbon-coupled analogue",
            ElementSymbol.of("Br"),
            ElementSymbol.of("C"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Replaces a carbon-bound bromide leaving group with carbon as a Suzuki-like cross-coupled product."),
        0.46,
        58.0,
        "Leaving-group-directed cross-coupling: every matching heteroaryl bromide site can be generated.",
        "Halogenated heterocycle can undergo palladium-catalyzed carbon-carbon cross-coupling.");
  }

  private static OrganicReactionRule heteroarylIodideSuzukiCarbonReplacement() {
    return ReplacingLeavingGroupOrganicReactionRule.of(
        OrganicReactionRuleId.of("replace_heteroaryl_iodide_suzuki_carbon"),
        "Heteroaryl iodide Suzuki carbon replacement",
        OrganicReactionRuleCategory.COUPLING,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
            OrganicReactionDomain.CROSS_COUPLING),
        List.of(
            MolecularFeatureKind.HETEROCYCLE,
            MolecularFeatureKind.HALOGENATED,
            MolecularFeatureKind.AROMATIC_RING),
        List.of(
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionConditionFeatureKind.INERT_ATMOSPHERE,
            ReactionConditionFeatureKind.ANHYDROUS,
            ReactionConditionFeatureKind.HEATING),
        LeavingGroupReplacementTransform.of(
            "suzuki_iodide_carbon_product",
            "iodide carbon-coupled analogue",
            ElementSymbol.of("I"),
            ElementSymbol.of("C"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Replaces a carbon-bound iodide leaving group with carbon as a Suzuki-like cross-coupled product."),
        0.52,
        62.0,
        "Leaving-group-directed cross-coupling: heteroaryl iodides are treated as highly activated coupling sites.",
        "Iodinated heterocycles can undergo palladium-catalyzed carbon-carbon cross-coupling.");
  }

  private static OrganicReactionRule heteroarylHeckAlkenylationTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_heteroaryl_heck_alkenylation"),
        "Heteroaryl Heck alkenylation transform",
        OrganicReactionRuleCategory.COUPLING,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
            OrganicReactionDomain.CROSS_COUPLING),
        List.of(MolecularFeatureKind.HETEROARYL_HALIDE),
        List.of(
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.INERT_ATMOSPHERE,
            ReactionConditionFeatureKind.HEATING),
        SubstituentAttachmentTransform.of(
            "heck_alkenyl_product",
            "Heck alkenylated analogue",
            ElementSymbol.of("C"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds an alkenyl carbon handle to an activated heteroaryl carbon as a Heck-type product channel."),
        0.42,
        54.0,
        "Heck regioselectivity is ranked from heteroaryl activation, alkene substitution and basic catalytic conditions.",
        "Heteroaryl halide plus alkene context under catalyst/base supports Heck-type alkenylation.");
  }

  private static OrganicReactionRule heteroarylHeckGraphAlkenylation() {
    return FragmentTransferOrganicReactionRule.of(
        OrganicReactionRuleId.of("fragment_transfer_heteroaryl_alkene_heck_coupling"),
        "Heteroaryl alkene Heck graph coupling",
        OrganicReactionRuleCategory.COUPLING,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
            OrganicReactionDomain.CROSS_COUPLING),
        List.of(MolecularFeatureKind.HETEROARYL_HALIDE),
        List.of(MolecularFeatureKind.ALKENE),
        List.of(
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.INERT_ATMOSPHERE,
            ReactionConditionFeatureKind.HEATING),
        FragmentTransferTransform.withCoproduct(
            "heck_graph_alkene_coupled",
            "Heck graph-coupled analogue",
            ReactantAtomSelector.element("C"),
            ReactantAtomSelector.halogen(),
            ReactantAtomSelector.element("C"),
            ReactantAtomSelector.element("H"),
            BondType.SINGLE,
            "hydrogen halide coproduct",
            BondType.SINGLE,
            "Removes a heteroaryl halide and vinylic hydrogen, then joins the real alkene fragment while preserving its unsaturation."),
        0.58,
        62.0,
        "Heck graph product preserves the alkene partner instead of replacing it with a generic carbon atom.",
        "Alkene partner plus metal/base context supports explicit graph-level Heck alkenylation.");
  }

  private static OrganicReactionRule heteroarylSuzukiGraphBoronateCoupling() {
    return FragmentTransferOrganicReactionRule.of(
        OrganicReactionRuleId.of("fragment_transfer_heteroaryl_boronate_suzuki_coupling"),
        "Heteroaryl boronate Suzuki graph coupling",
        OrganicReactionRuleCategory.COUPLING,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
            OrganicReactionDomain.CROSS_COUPLING),
        List.of(MolecularFeatureKind.HETEROARYL_HALIDE),
        List.of(MolecularFeatureKind.BORONIC_ACID_OR_BORONATE),
        List.of(
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.INERT_ATMOSPHERE,
            ReactionConditionFeatureKind.ANHYDROUS,
            ReactionConditionFeatureKind.HEATING),
        FragmentTransferTransform.withCoproduct(
            "suzuki_graph_boronate_coupled",
            "Suzuki graph-coupled analogue",
            ReactantAtomSelector.element("C"),
            ReactantAtomSelector.halogen(),
            ReactantAtomSelector.element("C"),
            ReactantAtomSelector.element("B"),
            BondType.SINGLE,
            "haloboronate coproduct",
            BondType.SINGLE,
            "Removes a heteroaryl halide leaving group and a boronate handle, then joins the two real carbon fragments."),
        0.63,
        68.0,
        "Suzuki graph product preserves the organoboron carbon fragment instead of replacing with a generic carbon atom.",
        "Boronate partner and metal/base context support explicit graph-level Suzuki coupling.");
  }

  private static OrganicReactionRule heteroarylSonogashiraAlkynylationTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_heteroaryl_sonogashira_alkynylation"),
        "Heteroaryl Sonogashira alkynylation transform",
        OrganicReactionRuleCategory.COUPLING,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
            OrganicReactionDomain.CROSS_COUPLING),
        List.of(MolecularFeatureKind.HETEROARYL_HALIDE),
        List.of(
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.INERT_ATMOSPHERE,
            ReactionConditionFeatureKind.ANHYDROUS),
        SubstituentAttachmentTransform.of(
            "sonogashira_alkynyl_product",
            "Sonogashira alkynylated analogue",
            ElementSymbol.of("C"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds an alkynyl carbon handle to an activated heteroaryl carbon as a Sonogashira-type product channel."),
        0.40,
        50.0,
        "Terminal-alkyne coupling is ranked against protodehalogenation and homocoupling side channels.",
        "Heteroaryl halide plus alkyne context under dry catalytic base supports Sonogashira-type alkynylation.");
  }

  private static OrganicReactionRule heteroarylSonogashiraGraphAlkynylation() {
    return FragmentTransferOrganicReactionRule.of(
        OrganicReactionRuleId.of("fragment_transfer_heteroaryl_alkyne_sonogashira_coupling"),
        "Heteroaryl alkyne Sonogashira graph coupling",
        OrganicReactionRuleCategory.COUPLING,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
            OrganicReactionDomain.CROSS_COUPLING),
        List.of(MolecularFeatureKind.HETEROARYL_HALIDE),
        List.of(MolecularFeatureKind.ALKYNE),
        List.of(
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.INERT_ATMOSPHERE,
            ReactionConditionFeatureKind.ANHYDROUS),
        FragmentTransferTransform.withCoproduct(
            "sonogashira_graph_alkyne_coupled",
            "Sonogashira graph-coupled analogue",
            ReactantAtomSelector.element("C"),
            ReactantAtomSelector.halogen(),
            ReactantAtomSelector.element("C"),
            ReactantAtomSelector.element("H"),
            BondType.SINGLE,
            "hydrogen halide coproduct",
            BondType.SINGLE,
            "Removes a heteroaryl halide and terminal alkyne hydrogen, then joins the scaffold to the alkyne carbon fragment."),
        0.56,
        61.0,
        "Sonogashira graph product keeps the alkyne fragment and is ranked against protodehalogenation and homocoupling.",
        "Terminal alkyne, catalyst, base and dry conditions support explicit graph-level Sonogashira coupling.");
  }

  private static OrganicReactionRule heteroarylBuchwaldUllmannAminationTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_heteroaryl_buchwald_ullmann_amination"),
        "Heteroaryl Buchwald/Ullmann amination transform",
        OrganicReactionRuleCategory.COUPLING,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
            OrganicReactionDomain.CROSS_COUPLING,
            OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
        List.of(MolecularFeatureKind.HETEROARYL_HALIDE),
        List.of(
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.INERT_ATMOSPHERE,
            ReactionConditionFeatureKind.HEATING),
        SubstituentAttachmentTransform.of(
            "buchwald_ullmann_amine_product",
            "Buchwald/Ullmann aminated analogue",
            ElementSymbol.of("N"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds nitrogen to an activated heteroaryl carbon as a metal-mediated C-N coupling product channel."),
        0.47,
        57.0,
        "C-N coupling competes with direct SNAr amination; catalyst and base increase coupling confidence.",
        "Heteroaryl halide plus amine context under metal catalyst/base supports Buchwald-Hartwig or Ullmann-type amination.");
  }

  private static OrganicReactionRule heteroarylBuchwaldUllmannGraphAmination() {
    return FragmentTransferOrganicReactionRule.of(
        OrganicReactionRuleId.of("fragment_transfer_heteroaryl_amine_buchwald_ullmann_coupling"),
        "Heteroaryl amine Buchwald/Ullmann graph coupling",
        OrganicReactionRuleCategory.COUPLING,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
            OrganicReactionDomain.CROSS_COUPLING,
            OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
        List.of(MolecularFeatureKind.HETEROARYL_HALIDE),
        List.of(MolecularFeatureKind.PRIMARY_OR_SECONDARY_AMINE),
        List.of(
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.INERT_ATMOSPHERE,
            ReactionConditionFeatureKind.HEATING),
        FragmentTransferTransform.withCoproduct(
            "buchwald_ullmann_graph_amine_coupled",
            "Buchwald/Ullmann graph-coupled analogue",
            ReactantAtomSelector.element("C"),
            ReactantAtomSelector.halogen(),
            ReactantAtomSelector.element("N"),
            ReactantAtomSelector.element("H"),
            BondType.SINGLE,
            "hydrogen halide coproduct",
            BondType.SINGLE,
            "Removes a heteroaryl halide and amine N-H proton, then joins the real amine fragment as a C-N coupling product."),
        0.60,
        64.0,
        "Graph C-N coupling preserves the amine substituent and competes with direct SNAr amination.",
        "Metal catalyst, base and amine partner support explicit Buchwald-Hartwig/Ullmann graph amination.");
  }

  private static OrganicReactionRule heteroarylFluorideAlkoxyReplacement() {
    return ReplacingLeavingGroupOrganicReactionRule.of(
        OrganicReactionRuleId.of("replace_heteroaryl_fluoride_alkoxy_snar"),
        "Heteroaryl fluoride alkoxy SNAr replacement",
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
        LeavingGroupReplacementTransform.of(
            "alkoxy_product",
            "alkoxy analogue",
            ElementSymbol.of("F"),
            ElementSymbol.of("O"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Replaces an activated carbon-bound fluoride with oxygen as an alkoxy SNAr product channel."),
        0.44,
        50.0,
        "Leaving-group-directed SNAr: every matching activated heteroaryl fluoride site can be generated.",
        "Electron-poor fluorinated nitrogen heterocycle can undergo alkoxide displacement.");
  }

  private static OrganicReactionRule heteroarylHydrodehalogenationSideChannel() {
    return ReplacingLeavingGroupOrganicReactionRule.of(
        OrganicReactionRuleId.of("side_heteroaryl_hydrodehalogenation"),
        "Heteroaryl hydrodehalogenation side channel",
        OrganicReactionRuleCategory.REDUCTION,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
            OrganicReactionDomain.CROSS_COUPLING),
        List.of(
            MolecularFeatureKind.HETEROCYCLE,
            MolecularFeatureKind.HALOGENATED,
            MolecularFeatureKind.AROMATIC_RING),
        List.of(
            ReactionConditionFeatureKind.HYDROGENATION,
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionConditionFeatureKind.INERT_ATMOSPHERE,
            ReactionConditionFeatureKind.HEATING),
        LeavingGroupReplacementTransform.of(
            "hydrodehalogenated_product",
            "hydrodehalogenated analogue",
            ElementSymbol.of("Br"),
            ElementSymbol.of("H"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Replaces a heteroaryl bromide leaving group with hydrogen as a reduction side channel."),
        0.18,
        12.0,
        "Competing hydrodehalogenation channel for palladium/base or reducing coupling conditions.",
        "Halogenated heterocycles can lose halogen under metal-catalyzed or reducing cross-coupling conditions.");
  }

  private static OrganicReactionRule heteroarylChlorideHydrodehalogenationSideChannel() {
    return ReplacingLeavingGroupOrganicReactionRule.of(
        OrganicReactionRuleId.of("side_heteroaryl_chloride_hydrodehalogenation"),
        "Heteroaryl chloride hydrodehalogenation side channel",
        OrganicReactionRuleCategory.REDUCTION,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
            OrganicReactionDomain.CROSS_COUPLING,
            OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
        List.of(
            MolecularFeatureKind.HETEROCYCLE,
            MolecularFeatureKind.HALOGENATED,
            MolecularFeatureKind.AROMATIC_RING),
        List.of(
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.WORKUP),
        LeavingGroupReplacementTransform.of(
            "chloride_hydrodehalogenated_product",
            "chloride hydrodehalogenated analogue",
            ElementSymbol.of("Cl"),
            ElementSymbol.of("H"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Replaces a heteroaryl chloride leaving group with hydrogen as a reduction side channel."),
        0.12,
        8.0,
        "Competing hydrodechlorination channel for activated heteroaryl chlorides under catalyst, heat or reductive workup.",
        "Activated chloro heterocycles can lose chloride instead of clean substitution when catalytic or reducing side paths are available.");
  }

  private static OrganicReactionRule heteroarylSolvolysisHydroxyReplacement() {
    return ReplacingLeavingGroupOrganicReactionRule.of(
        OrganicReactionRuleId.of("side_heteroaryl_solvolysis_hydroxy_replacement"),
        "Heteroaryl solvolysis hydroxy replacement",
        OrganicReactionRuleCategory.SUBSTITUTION,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
            OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
        List.of(
            MolecularFeatureKind.HETEROCYCLE,
            MolecularFeatureKind.HALOGENATED,
            MolecularFeatureKind.NITROGEN_HETEROCYCLE),
        List.of(ReactionConditionFeatureKind.HEATING, ReactionConditionFeatureKind.WORKUP),
        LeavingGroupReplacementTransform.of(
            "hydroxy_solvolysis_product",
            "hydroxy solvolysis analogue",
            ElementSymbol.of("Cl"),
            ElementSymbol.of("O"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Replaces an activated heteroaryl chloride with oxygen as hydrolysis or solvolysis side product."),
        0.20,
        15.0,
        "Competing hydroxy side channel, especially under wet workup or prolonged heating.",
        "Activated heteroaryl chlorides can hydrolyze or solvolyze instead of clean nucleophile coupling.");
  }

  private static OrganicReactionRule heteroarylProtodeboronationSideChannel() {
    return ReplacingLeavingGroupOrganicReactionRule.of(
        OrganicReactionRuleId.of("side_heteroaryl_protodeboronation"),
        "Heteroaryl protodeboronation side channel",
        OrganicReactionRuleCategory.REDUCTION,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
            OrganicReactionDomain.CROSS_COUPLING),
        List.of(MolecularFeatureKind.HETEROCYCLE, MolecularFeatureKind.AROMATIC_RING),
        List.of(
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.WORKUP),
        LeavingGroupReplacementTransform.of(
            "protodeboronated_product",
            "protodeboronated analogue",
            ElementSymbol.of("B"),
            ElementSymbol.of("H"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Replaces a heteroaryl boron substituent with hydrogen as a protodeboronation side channel."),
        0.16,
        10.0,
        "Competing protodeboronation channel for heteroaryl boronates under base, heat or workup.",
        "Heteroaryl boronic acids and boronates can protodeboronate before or during coupling.");
  }

  private static OrganicReactionRule heteroarylHalogenationTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_heteroaryl_bromination"),
        "Heteroaryl bromination transform",
        OrganicReactionRuleCategory.SUBSTITUTION,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
            OrganicReactionDomain.ELECTROPHILIC_SUBSTITUTION),
        List.of(MolecularFeatureKind.HETEROCYCLE, MolecularFeatureKind.AROMATIC_RING),
        List.of(
            ReactionConditionFeatureKind.COOLING,
            ReactionConditionFeatureKind.ADDITION_RATE,
            ReactionConditionFeatureKind.ACIDIC_MEDIUM),
        SubstituentAttachmentTransform.of(
            "bromo_product",
            "brominated analogue",
            ElementSymbol.of("Br"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds bromine to a heteroaryl carbon as an electrophilic substitution product transform."),
        0.30,
        44.0,
        "Electrophilic regioisomer set generated from matching heteroaryl carbon sites before ranking.",
        "Heteroarene electrophilic halogenation under controlled addition conditions.");
  }

  private static OrganicReactionRule aromaticNitrationTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_aromatic_nitration"),
        "Aromatic nitration transform",
        OrganicReactionRuleCategory.SUBSTITUTION,
        List.of(OrganicReactionDomain.ELECTROPHILIC_SUBSTITUTION),
        List.of(MolecularFeatureKind.AROMATIC_RING),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.COOLING,
            ReactionConditionFeatureKind.ADDITION_RATE),
        SubstituentAttachmentTransform.of(
            "nitro_product",
            "nitro analogue",
            ElementSymbol.of("N"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds a nitrogen substituent to each aromatic carbon as an electrophilic nitration product channel."),
        0.34,
        46.0,
        "Ortho/meta/para or heteroaryl regioisomers are generated from all available aromatic carbon sites.",
        "Aromatic pi system under acidic electrophile-generating conditions supports nitration screening.");
  }

  private static OrganicReactionRule aromaticSulfonationTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_aromatic_sulfonation"),
        "Aromatic sulfonation transform",
        OrganicReactionRuleCategory.SUBSTITUTION,
        List.of(OrganicReactionDomain.ELECTROPHILIC_SUBSTITUTION),
        List.of(MolecularFeatureKind.AROMATIC_RING),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.WORKUP),
        SubstituentAttachmentTransform.of(
            "sulfonyl_product",
            "sulfonated analogue",
            ElementSymbol.of("S"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds sulfur to aromatic carbon as a compact sulfonation product channel for downstream acid-state ranking."),
        0.25,
        38.0,
        "Sulfonation reversibility and acid strength are represented through condition evidence before ranking.",
        "Activated aromatic or heteroaromatic ring can enter electrophilic sulfonation under strong acid/heat.");
  }

  private static OrganicReactionRule friedelCraftsAlkylationTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_friedel_crafts_alkylation"),
        "Friedel-Crafts alkylation transform",
        OrganicReactionRuleCategory.SUBSTITUTION,
        List.of(OrganicReactionDomain.ELECTROPHILIC_SUBSTITUTION),
        List.of(MolecularFeatureKind.AROMATIC_RING),
        List.of(
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.ANHYDROUS),
        SubstituentAttachmentTransform.of(
            "alkyl_product",
            "alkylated analogue",
            ElementSymbol.of("C"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds carbon to aromatic carbon as a Friedel-Crafts-type electrophilic alkylation product channel."),
        0.29,
        41.0,
        "Polyalkylation and cation rearrangement are left as competing candidates rather than collapsed into one answer.",
        "Aromatic system plus Lewis/Brønsted acid activation supports electrophilic carbon substitution.");
  }

  private static OrganicReactionRule arylDiazoniumHydroxyReplacement() {
    return ReplacingLeavingGroupOrganicReactionRule.of(
        OrganicReactionRuleId.of("replace_aryl_diazonium_hydroxy"),
        "Aryl diazonium hydroxy replacement",
        OrganicReactionRuleCategory.SUBSTITUTION,
        List.of(
            OrganicReactionDomain.ELECTROPHILIC_SUBSTITUTION,
            OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
        List.of(MolecularFeatureKind.AROMATIC_RING, MolecularFeatureKind.AMINE),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.COOLING,
            ReactionConditionFeatureKind.WORKUP),
        LeavingGroupReplacementTransform.of(
            "diazonium_hydroxy_product",
            "diazonium hydrolysis analogue",
            ElementSymbol.of("N"),
            ElementSymbol.of("O"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Replaces an aryl nitrogen handle with oxygen as a diazotization/hydrolysis product channel."),
        0.22,
        34.0,
        "Diazonium formation, hydrolysis and salt stability are represented through condition evidence.",
        "Aromatic amine under cold acidic workup can form diazonium-like intermediates that hydrolyze.");
  }

  private static OrganicReactionRule arylDiazoniumHalideReplacement() {
    return ReplacingLeavingGroupOrganicReactionRule.of(
        OrganicReactionRuleId.of("replace_aryl_diazonium_halide"),
        "Aryl diazonium halide replacement",
        OrganicReactionRuleCategory.SUBSTITUTION,
        List.of(
            OrganicReactionDomain.ELECTROPHILIC_SUBSTITUTION,
            OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
        List.of(MolecularFeatureKind.AROMATIC_RING, MolecularFeatureKind.AMINE),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.COOLING,
            ReactionConditionFeatureKind.CATALYST_PRESENT),
        LeavingGroupReplacementTransform.of(
            "sandmeyer_chloro_product",
            "Sandmeyer chloro analogue",
            ElementSymbol.of("N"),
            ElementSymbol.of("Cl"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Replaces an aryl nitrogen handle with chlorine as a Sandmeyer-like product channel."),
        0.24,
        36.0,
        "Halide identity, copper salt and diazonium lifetime should be checked by downstream ranking.",
        "Aromatic amine under cold acidic/catalytic conditions supports Sandmeyer-type halogenation.");
  }

  private static OrganicReactionRule azoCouplingNitrogenAttachment() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_azo_coupling_nitrogen_attachment"),
        "Azo coupling nitrogen attachment",
        OrganicReactionRuleCategory.SUBSTITUTION,
        List.of(OrganicReactionDomain.ELECTROPHILIC_SUBSTITUTION),
        List.of(MolecularFeatureKind.AROMATIC_RING, MolecularFeatureKind.AMINE),
        List.of(
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.COOLING,
            ReactionConditionFeatureKind.ADDITION_RATE),
        SubstituentAttachmentTransform.of(
            "azo_nitrogen_product",
            "azo-coupled analogue",
            ElementSymbol.of("N"),
            BondType.DOUBLE,
            ReactantAtomSelector.element("C"),
            "Adds a nitrogen double-bond handle to aromatic carbon as an azo-coupling product channel."),
        0.26,
        40.0,
        "Coupling position is generated from aromatic carbon sites and ranked electronically.",
        "Electron-rich aromatic partner and diazonium-like electrophile support azo coupling.");
  }

  private static OrganicReactionRule nitrogenHeterocycleNOxidationTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_nitrogen_heterocycle_n_oxide"),
        "Nitrogen heterocycle N-oxide transform",
        OrganicReactionRuleCategory.OXIDATION,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
            OrganicReactionDomain.OXIDATION_REDUCTION),
        List.of(MolecularFeatureKind.HETEROCYCLE, MolecularFeatureKind.NITROGEN_HETEROCYCLE),
        List.of(
            ReactionConditionFeatureKind.OXIDIZING_MEDIUM,
            ReactionConditionFeatureKind.COOLING,
            ReactionConditionFeatureKind.ANHYDROUS,
            ReactionConditionFeatureKind.ADDITION_RATE),
        SubstituentAttachmentTransform.of(
            "n_oxide_product",
            "N-oxide analogue",
            ElementSymbol.of("O"),
            BondType.SINGLE,
            ReactantAtomSelector.element("N"),
            "Adds oxygen to a heteroaryl nitrogen as an N-oxide planning transform."),
        0.48,
        64.0,
        "Heteroatom-directed oxidation: every matching nitrogen site can be generated.",
        "Nitrogen heterocycles can form N-oxides under controlled oxidant addition.");
  }

  private static OrganicReactionRule nitrogenHeterocycleNAlkylationTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_nitrogen_heterocycle_n_alkylation"),
        "Nitrogen heterocycle N-alkylation transform",
        OrganicReactionRuleCategory.SUBSTITUTION,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
            OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION,
            OrganicReactionDomain.ACID_BASE),
        List.of(MolecularFeatureKind.HETEROCYCLE, MolecularFeatureKind.NITROGEN_HETEROCYCLE),
        List.of(
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.ADDITION_RATE),
        SubstituentAttachmentTransform.of(
            "n_alkyl_product",
            "N-alkyl analogue",
            ElementSymbol.of("C"),
            BondType.SINGLE,
            ReactantAtomSelector.element("N"),
            "Adds carbon to a heterocycle nitrogen as an N-alkylation or quaternization product channel."),
        0.43,
        46.0,
        "Single-site N-alkylation candidate retained alongside graph-level fragment attachment.",
        "Nitrogen heterocycles can form N-alkylated products or salts under alkylating conditions.");
  }

  private static OrganicReactionRule nitrogenHeterocycleGraphNAlkylation() {
    return FragmentAttachmentOrganicReactionRule.of(
        OrganicReactionRuleId.of("fragment_attachment_nitrogen_heterocycle_n_alkylation"),
        "Nitrogen heterocycle graph N-alkylation",
        OrganicReactionRuleCategory.SUBSTITUTION,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
            OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION,
            OrganicReactionDomain.ACID_BASE),
        List.of(MolecularFeatureKind.HETEROCYCLE, MolecularFeatureKind.NITROGEN_HETEROCYCLE),
        List.of(MolecularFeatureKind.HALOGENATED),
        List.of(
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.ADDITION_RATE),
        FragmentAttachmentTransform.of(
            "n_alkyl_graph_product",
            "N-alkyl graph analogue",
            ReactantAtomSelector.element("N"),
            ReactantAtomSelector.element("C"),
            ReactantAtomSelector.halogen(),
            BondType.SINGLE,
            "Removes a halogen leaving group from an alkylating fragment and attaches the remaining carbon graph to heterocycle nitrogen."),
        0.56,
        58.0,
        "N-alkylation regioisomer set is generated by attaching an actual alkylating fragment before condition and site ranking.",
        "Basic or heated alkylating conditions can transfer an alkyl fragment to a nitrogen heterocycle without using outcome lookup.");
  }

  private static OrganicReactionRule heteroarylNitrileHydrationSideChannel() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("side_heteroaryl_nitrile_hydration"),
        "Heteroaryl nitrile hydration side channel",
        OrganicReactionRuleCategory.ADDITION,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION, OrganicReactionDomain.ADDITION),
        List.of(MolecularFeatureKind.HETEROCYCLE, MolecularFeatureKind.NITRILE),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.WORKUP),
        SubstituentAttachmentTransform.of(
            "nitrile_hydration_product",
            "nitrile hydration analogue",
            ElementSymbol.of("O"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds oxygen to a nitrile-bearing heteroaryl carbon as an amide/hydrolysis side-channel product."),
        0.14,
        10.0,
        "Single-site nitrile hydration channel retained alongside graph-level hydration.",
        "Cyano heterocycles can hydrate or hydrolyze under forcing conditions, so product prediction retains that side channel.");
  }

  private static OrganicReactionRule heteroarylNitrileGraphHydrationSideChannel() {
    return BondAdditionOrganicReactionRule.of(
        OrganicReactionRuleId.of("bond_addition_heteroaryl_nitrile_hydration"),
        "Heteroaryl nitrile graph hydration side channel",
        OrganicReactionRuleCategory.ADDITION,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION, OrganicReactionDomain.ADDITION),
        List.of(MolecularFeatureKind.HETEROCYCLE, MolecularFeatureKind.NITRILE),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.WORKUP),
        BondAdditionTransform.of(
            "nitrile_hydrated_graph_product",
            "nitrile hydration graph analogue",
            BondType.TRIPLE,
            BondType.DOUBLE,
            ReactantAtomSelector.element("C"),
            ReactantAtomSelector.element("N"),
            ElementSymbol.of("O"),
            ElementSymbol.of("H"),
            BondType.SINGLE,
            BondType.SINGLE,
            "Converts a nitrile C≡N bond to a C=N hydration-side-channel graph with oxygen added to carbon and hydrogen added to nitrogen."),
        0.27,
        24.0,
        "Nitrile hydration side-product candidates are generated from actual C≡N sites before ranking.",
        "Wet acidic/basic heating or workup can hydrate cyano heterocycles through an explicit nitrile graph channel.");
  }

  private static OrganicReactionRule heteroarylNitrileReductionSideChannel() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("side_heteroaryl_nitrile_reduction"),
        "Heteroaryl nitrile reduction side channel",
        OrganicReactionRuleCategory.REDUCTION,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
            OrganicReactionDomain.OXIDATION_REDUCTION),
        List.of(MolecularFeatureKind.HETEROCYCLE, MolecularFeatureKind.NITRILE),
        List.of(
            ReactionConditionFeatureKind.REDUCING_MEDIUM,
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.INERT_ATMOSPHERE),
        SubstituentAttachmentTransform.of(
            "nitrile_reduction_product",
            "nitrile reduction analogue",
            ElementSymbol.of("H"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds hydrogen to a nitrile-bearing heteroaryl carbon as a reduced imine/amine side-channel product."),
        0.10,
        7.0,
        "Single-site nitrile reduction channel retained alongside graph-level reduction.",
        "Cyano heterocycles can undergo partial or full reduction when catalytic/reducing conditions compete with ring functionalization.");
  }

  private static OrganicReactionRule heteroarylNitrileGraphReductionSideChannel() {
    return BondAdditionOrganicReactionRule.of(
        OrganicReactionRuleId.of("bond_addition_heteroaryl_nitrile_reduction"),
        "Heteroaryl nitrile graph reduction side channel",
        OrganicReactionRuleCategory.REDUCTION,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
            OrganicReactionDomain.OXIDATION_REDUCTION),
        List.of(MolecularFeatureKind.HETEROCYCLE, MolecularFeatureKind.NITRILE),
        List.of(
            ReactionConditionFeatureKind.REDUCING_MEDIUM,
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.INERT_ATMOSPHERE),
        BondAdditionTransform.of(
            "nitrile_reduced_graph_product",
            "nitrile reduction graph analogue",
            BondType.TRIPLE,
            BondType.DOUBLE,
            ReactantAtomSelector.element("C"),
            ReactantAtomSelector.element("N"),
            ElementSymbol.of("H"),
            ElementSymbol.of("H"),
            BondType.SINGLE,
            BondType.SINGLE,
            "Converts a nitrile C≡N bond to an imine-level C=N reduction graph with hydrogen added to both nitrile atoms."),
        0.24,
        20.0,
        "Nitrile reduction side-product candidates are generated from actual C≡N sites before ranking.",
        "Catalytic hydrogen-transfer or reducing metal conditions can reduce cyano heterocycles through an explicit nitrile graph channel.");
  }

  private static OrganicReactionRule sulfurHeterocycleOxidationTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_sulfur_heterocycle_oxidation"),
        "Sulfur heterocycle oxidation transform",
        OrganicReactionRuleCategory.OXIDATION,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
            OrganicReactionDomain.OXIDATION_REDUCTION),
        List.of(MolecularFeatureKind.HETEROCYCLE, MolecularFeatureKind.SULFUR_HETEROCYCLE),
        List.of(
            ReactionConditionFeatureKind.OXIDIZING_MEDIUM,
            ReactionConditionFeatureKind.COOLING,
            ReactionConditionFeatureKind.ADDITION_RATE),
        SubstituentAttachmentTransform.of(
            "s_oxide_product",
            "S-oxide analogue",
            ElementSymbol.of("O"),
            BondType.DOUBLE,
            ReactantAtomSelector.element("S"),
            "Adds oxygen to sulfur as a sulfoxide or sulfone planning transform depending on equivalents."),
        0.55,
        60.0,
        "Sulfur oxidation candidate set generated from matching sulfur sites before ranking.",
        "Sulfur-containing heterocycles can be oxidized under controlled oxidant addition.");
  }

  private static OrganicReactionRule nitroReductionTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_nitro_reduction"),
        "Nitro reduction transform",
        OrganicReactionRuleCategory.REDUCTION,
        List.of(OrganicReactionDomain.OXIDATION_REDUCTION),
        List.of(MolecularFeatureKind.NITRO),
        List.of(
            ReactionConditionFeatureKind.REDUCING_MEDIUM,
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.INERT_ATMOSPHERE),
        SubstituentAttachmentTransform.of(
            "nitro_reduced_product",
            "nitro-reduced analogue",
            ElementSymbol.of("H"),
            BondType.SINGLE,
            ReactantAtomSelector.element("N"),
            "Adds hydrogen to nitro nitrogen as a compact nitroso/hydroxylamine/aniline reduction product channel."),
        0.36,
        58.0,
        "Stepwise nitro reduction stage is ranked from reducing conditions and substrate electronics.",
        "Nitro functionality can reduce through nitroso and hydroxylamine stages under catalytic or transfer-hydrogen conditions.");
  }

  private static OrganicReactionRule alcoholOxidationTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_alcohol_oxidation"),
        "Alcohol oxidation transform",
        OrganicReactionRuleCategory.OXIDATION,
        List.of(OrganicReactionDomain.OXIDATION_REDUCTION),
        List.of(MolecularFeatureKind.HYDROXYL_OR_ALKOXY),
        List.of(
            ReactionConditionFeatureKind.OXIDIZING_MEDIUM,
            ReactionConditionFeatureKind.ADDITION_RATE,
            ReactionConditionFeatureKind.COOLING,
            ReactionConditionFeatureKind.WORKUP),
        SubstituentAttachmentTransform.of(
            "alcohol_oxidized_product",
            "oxidized oxygenated analogue",
            ElementSymbol.of("O"),
            BondType.DOUBLE,
            ReactantAtomSelector.element("C"),
            "Adds a double-bond oxygen handle to carbon as a compact alcohol-to-carbonyl oxidation channel."),
        0.35,
        55.0,
        "Primary/secondary alcohol selectivity and overoxidation are retained as ranking evidence.",
        "Hydroxyl or alkoxy feature under oxidizing workup can enter alcohol oxidation pathways.");
  }

  private static OrganicReactionRule amideHydrolysisTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_amide_hydrolysis"),
        "Amide hydrolysis transform",
        OrganicReactionRuleCategory.ADDITION,
        List.of(OrganicReactionDomain.ADDITION, OrganicReactionDomain.ACID_BASE),
        List.of(MolecularFeatureKind.AMIDE),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.WORKUP),
        SubstituentAttachmentTransform.of(
            "amide_hydrolyzed_product",
            "hydrolyzed amide analogue",
            ElementSymbol.of("O"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds oxygen to an amide carbonyl-side carbon as a hydrolysis/carboxylate product channel."),
        0.28,
        42.0,
        "Acid/base identity determines neutral acid versus carboxylate/ammonium product ranking.",
        "Amide feature under wet acidic or basic heating can hydrolyze through tetrahedral intermediates.");
  }

  private static OrganicReactionRule betaEliminationUnsaturationTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_beta_elimination_unsaturation"),
        "Beta elimination unsaturation transform",
        OrganicReactionRuleCategory.ELIMINATION,
        List.of(OrganicReactionDomain.ELIMINATION),
        List.of(MolecularFeatureKind.HALOGENATED),
        List.of(
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.ANHYDROUS),
        SubstituentAttachmentTransform.of(
            "elimination_unsaturated_product",
            "unsaturated elimination analogue",
            ElementSymbol.of("C"),
            BondType.DOUBLE,
            ReactantAtomSelector.element("C"),
            "Adds a double-bond carbon handle to a carbon site as a compact E1/E2 unsaturation product channel."),
        0.09,
        14.0,
        "Coarse single-site beta-elimination planning channel retained alongside graph-level elimination.",
        "Halogenated substrate under dry basic heating can compete through elimination; graph-level alpha-beta unsaturation should dominate this fallback.");
  }

  private static OrganicReactionRule betaEliminationGraphUnsaturation() {
    return BetaEliminationOrganicReactionRule.of(
        OrganicReactionRuleId.of("graph_beta_elimination_unsaturation"),
        "Graph beta-elimination unsaturation",
        List.of(OrganicReactionDomain.ELIMINATION),
        List.of(MolecularFeatureKind.HALOGENATED),
        List.of(
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.ANHYDROUS),
        BetaEliminationTransform.of(
            "beta_elimination_graph_alkene",
            "beta-elimination alkene graph analogue",
            ReactantAtomSelector.element("C"),
            ReactantAtomSelector.halogen(),
            ReactantAtomSelector.element("C"),
            BondType.SINGLE,
            BondType.DOUBLE,
            "Removes a halogen leaving group from alpha carbon and raises the alpha-beta bond to an alkene."),
        0.43,
        52.0,
        "Graph-level E1/E2 candidates are generated over every matching alpha-halogen and beta-carbon pair.",
        "Dry basic heating can eliminate hydrogen halide from a halogenated substrate to form an explicit alkene graph.");
  }

  private static OrganicReactionRule carbonylOrganometallicAdditionTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_carbonyl_organometallic_addition"),
        "Carbonyl organometallic addition transform",
        OrganicReactionRuleCategory.ADDITION,
        List.of(OrganicReactionDomain.ADDITION),
        List.of(MolecularFeatureKind.CARBONYL),
        List.of(
            ReactionConditionFeatureKind.ANHYDROUS,
            ReactionConditionFeatureKind.INERT_ATMOSPHERE,
            ReactionConditionFeatureKind.COOLING),
        SubstituentAttachmentTransform.of(
            "organometallic_carbinol_product",
            "carbinol addition analogue",
            ElementSymbol.of("C"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds a carbon fragment to a carbonyl carbon as a Grignard/organolithium-style alcohol precursor."),
        0.12,
        18.0,
        "Single-site carbonyl-addition candidate retained alongside graph-level C=O addition.",
        "Carbonyl substrate can undergo organometallic nucleophilic addition; graph-level C=O addition should dominate this fallback.");
  }

  private static OrganicReactionRule carbonylGraphOrganometallicAddition() {
    return BondAdditionOrganicReactionRule.of(
        OrganicReactionRuleId.of("bond_addition_carbonyl_organometallic_addition"),
        "Carbonyl graph organometallic addition",
        OrganicReactionRuleCategory.ADDITION,
        List.of(OrganicReactionDomain.ADDITION),
        List.of(MolecularFeatureKind.CARBONYL),
        List.of(
            ReactionConditionFeatureKind.ANHYDROUS,
            ReactionConditionFeatureKind.INERT_ATMOSPHERE,
            ReactionConditionFeatureKind.COOLING),
        BondAdditionTransform.of(
            "carbonyl_organometallic_graph_product",
            "organometallic alcohol graph precursor",
            BondType.DOUBLE,
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            ReactantAtomSelector.element("O"),
            ElementSymbol.of("C"),
            ElementSymbol.of("H"),
            BondType.SINGLE,
            BondType.SINGLE,
            "Converts a carbonyl C=O bond to C-O and adds carbon to the carbonyl carbon plus hydrogen to oxygen."),
        0.52,
        63.0,
        "Graph-level carbonyl addition keeps the alkoxide/alcohol skeleton explicit before later workup ranking.",
        "Organometallic carbonyl addition is represented as two-site graph addition across the C=O bond.");
  }

  private static OrganicReactionRule carbonylReductiveAminationTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_carbonyl_reductive_amination"),
        "Carbonyl reductive amination transform",
        OrganicReactionRuleCategory.REDUCTION,
        List.of(OrganicReactionDomain.ADDITION, OrganicReactionDomain.OXIDATION_REDUCTION),
        List.of(MolecularFeatureKind.CARBONYL, MolecularFeatureKind.AMINE),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.ADDITION_RATE,
            ReactionConditionFeatureKind.WORKUP),
        SubstituentAttachmentTransform.of(
            "reductive_amine_product",
            "reductive amination analogue",
            ElementSymbol.of("N"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds nitrogen to the carbonyl carbon as an iminium/reduction product channel."),
        0.38,
        57.0,
        "Iminium formation and hydride delivery are treated as a paired mechanism candidate.",
        "Carbonyl and amine context supports reductive amination screening.");
  }

  private static OrganicReactionRule carbonylGraphReductiveAmination() {
    return BondAdditionOrganicReactionRule.of(
        OrganicReactionRuleId.of("bond_addition_carbonyl_reductive_amination"),
        "Carbonyl graph reductive amination precursor",
        OrganicReactionRuleCategory.REDUCTION,
        List.of(OrganicReactionDomain.ADDITION, OrganicReactionDomain.OXIDATION_REDUCTION),
        List.of(MolecularFeatureKind.CARBONYL, MolecularFeatureKind.AMINE),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.ADDITION_RATE,
            ReactionConditionFeatureKind.WORKUP),
        BondAdditionTransform.of(
            "carbonyl_reductive_amination_graph_product",
            "carbinolamine/iminium graph precursor",
            BondType.DOUBLE,
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            ReactantAtomSelector.element("O"),
            ElementSymbol.of("N"),
            ElementSymbol.of("H"),
            BondType.SINGLE,
            BondType.SINGLE,
            "Converts a carbonyl C=O bond to C-O and adds nitrogen/proton equivalents as a reductive-amination precursor graph."),
        0.49,
        57.0,
        "Reductive amination candidates are generated as explicit C-N/C-O precursor graphs before reduction ranking.",
        "Carbonyl and amine context supports carbinolamine/iminium formation followed by hydride delivery.");
  }

  private static OrganicReactionRule carbonylAmineGraphCyclization() {
    return IntramolecularBondFormationOrganicReactionRule.of(
        OrganicReactionRuleId.of("intramolecular_bond_carbonyl_amine_cyclization"),
        "Carbonyl amine graph cyclization",
        OrganicReactionRuleCategory.HETEROCYCLE_SYNTHESIS,
        List.of(
            OrganicReactionDomain.CYCLIZATION,
            OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
            OrganicReactionDomain.ADDITION),
        List.of(MolecularFeatureKind.CARBONYL, MolecularFeatureKind.AMINE),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.WORKUP),
        IntramolecularBondFormationTransform.of(
            "carbonyl_amine_cyclized_graph",
            "carbonyl-amine cyclized graph analogue",
            ReactantAtomSelector.element("C"),
            ReactantAtomSelector.element("N"),
            BondType.SINGLE,
            "Forms an explicit intramolecular C-N bond between a carbonyl-containing carbon site and an amine nitrogen site."),
        0.43,
        53.0,
        "Cyclization products should be filtered by ring size, proton transfer, and dehydration feasibility.",
        "Carbonyl and amine functionality under acid or thermal conditions supports intramolecular C-N ring closure.");
  }

  private static OrganicReactionRule carbonylCannizzaroDisproportionationTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_carbonyl_cannizzaro_disproportionation"),
        "Cannizzaro carbonyl disproportionation transform",
        OrganicReactionRuleCategory.OXIDATION,
        List.of(OrganicReactionDomain.OXIDATION_REDUCTION, OrganicReactionDomain.ADDITION),
        List.of(MolecularFeatureKind.CARBONYL, MolecularFeatureKind.ALDEHYDE),
        List.of(
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.WORKUP),
        SubstituentAttachmentTransform.of(
            "cannizzaro_carboxylate_alcohol_pair",
            "Cannizzaro carboxylate/alcohol analogue",
            ElementSymbol.of("O"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds oxygen at an aldehyde carbon as the oxidized half of a Cannizzaro disproportionation pair."),
        0.25,
        35.0,
        "Requires non-enolizable aldehyde context; aldol and hydration channels stay as competing candidates.",
        "Strong basic aldehyde context supports Cannizzaro hydride-transfer disproportionation.");
  }

  private static OrganicReactionRule acylSubstitutionAminolysisTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_acyl_substitution_aminolysis"),
        "Acyl substitution aminolysis transform",
        OrganicReactionRuleCategory.SUBSTITUTION,
        List.of(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
        List.of(MolecularFeatureKind.CARBOXYL_DERIVATIVE, MolecularFeatureKind.AMINE),
        List.of(
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.ANHYDROUS,
            ReactionConditionFeatureKind.ADDITION_RATE),
        SubstituentAttachmentTransform.of(
            "amide_substitution_product",
            "amide substitution analogue",
            ElementSymbol.of("N"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds nitrogen to an acyl carbon as an aminolysis/acyl substitution product candidate."),
        0.45,
        66.0,
        "Acyl leaving group quality and amine protonation should be checked in follow-up calculations.",
        "Carboxyl derivative and amine features support acyl substitution.");
  }

  private static OrganicReactionRule acylGraphAminolysisFragmentTransfer() {
    return FragmentTransferOrganicReactionRule.of(
        OrganicReactionRuleId.of("fragment_transfer_acyl_aminolysis_amide"),
        "Acyl aminolysis graph amide formation",
        OrganicReactionRuleCategory.SUBSTITUTION,
        List.of(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
        List.of(MolecularFeatureKind.CARBOXYL_DERIVATIVE),
        List.of(MolecularFeatureKind.AMINE),
        List.of(
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.ANHYDROUS,
            ReactionConditionFeatureKind.ADDITION_RATE),
        FragmentTransferTransform.withCoproduct(
            "amide_graph_product",
            "amide graph product",
            ReactantAtomSelector.element("C"),
            ReactantAtomSelector.element("O"),
            ReactantAtomSelector.element("N"),
            ReactantAtomSelector.element("H"),
            BondType.SINGLE,
            "water-like eliminated pair",
            BondType.SINGLE,
            "Removes an acyl oxygen leaving group and an amine N-H leaving atom, then forms an explicit acyl C-N graph product."),
        0.54,
        64.0,
        "Acyl aminolysis graph products preserve the amine fragment and should still be ranked by leaving-group quality.",
        "Carboxyl derivative plus amine context supports graph-level amide formation.");
  }

  private static OrganicReactionRule carbonylGraphImineCondensation() {
    return FragmentTransferOrganicReactionRule.of(
        OrganicReactionRuleId.of("fragment_transfer_carbonyl_amine_imine_condensation"),
        "Carbonyl amine graph imine condensation",
        OrganicReactionRuleCategory.ADDITION,
        List.of(OrganicReactionDomain.ADDITION, OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
        List.of(MolecularFeatureKind.CARBONYL),
        List.of(MolecularFeatureKind.PRIMARY_OR_SECONDARY_AMINE),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.ANHYDROUS),
        FragmentTransferTransform.withCoproduct(
            "imine_condensation_graph_product",
            "imine condensation graph product",
            ReactantAtomSelector.element("C"),
            ReactantAtomSelector.element("O"),
            ReactantAtomSelector.element("N"),
            ReactantAtomSelector.element("H"),
            BondType.DOUBLE,
            "water-like eliminated pair",
            BondType.SINGLE,
            "Removes carbonyl oxygen and amine N-H leaving atoms, then forms an explicit C=N imine graph product."),
        0.46,
        58.0,
        "Water removal, acid strength and amine sterics should rank imine formation against hydration or reduction channels.",
        "Carbonyl plus primary/secondary amine context supports graph-level imine condensation.");
  }

  private static OrganicReactionRule carboxylEsterificationTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_carboxyl_esterification"),
        "Carboxyl esterification transform",
        OrganicReactionRuleCategory.SUBSTITUTION,
        List.of(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION, OrganicReactionDomain.ACID_BASE),
        List.of(
            MolecularFeatureKind.CARBOXYLIC_ACID_OR_CARBOXYLATE,
            MolecularFeatureKind.HYDROXYL_OR_ALKOXY),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.WORKUP),
        SubstituentAttachmentTransform.of(
            "esterification_alkoxy_product",
            "esterified analogue",
            ElementSymbol.of("C"),
            BondType.SINGLE,
            ReactantAtomSelector.element("O"),
            "Adds carbon to a carboxyl oxygen as a Fischer-esterification or activated ester planning candidate."),
        0.36,
        50.0,
        "Equilibrium, water removal, and acid sensitivity should be ranked against hydrolysis/recovery channels.",
        "Carboxylic acid or carboxylate plus alcohol context supports esterification screening.");
  }

  private static OrganicReactionRule esterTransesterificationTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_ester_transesterification"),
        "Ester transesterification transform",
        OrganicReactionRuleCategory.SUBSTITUTION,
        List.of(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION, OrganicReactionDomain.ACID_BASE),
        List.of(MolecularFeatureKind.ESTER, MolecularFeatureKind.HYDROXYL_OR_ALKOXY),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.HEATING),
        SubstituentAttachmentTransform.of(
            "transesterified_alkoxy_exchange_product",
            "transesterified analogue",
            ElementSymbol.of("O"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds oxygen at an acyl context as an alcohol-exchange transesterification planning candidate."),
        0.34,
        48.0,
        "Alcohol excess and leaving alcohol volatility should be encoded as condition evidence before execution.",
        "Ester and alcohol/alkoxy context supports transesterification screening.");
  }

  private static OrganicReactionRule claisenLikeCondensationTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_claisen_like_condensation"),
        "Claisen-like ester condensation transform",
        OrganicReactionRuleCategory.ADDITION,
        List.of(OrganicReactionDomain.ADDITION, OrganicReactionDomain.CYCLIZATION),
        List.of(MolecularFeatureKind.ESTER),
        List.of(
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.ANHYDROUS,
            ReactionConditionFeatureKind.WORKUP),
        SubstituentAttachmentTransform.of(
            "claisen_beta_keto_ester",
            "Claisen beta-keto ester analogue",
            ElementSymbol.of("C"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds a carbon fragment at an ester/enolate-compatible carbon as a Claisen or Dieckmann channel."),
        0.31,
        45.0,
        "Alpha-proton availability, matching alkoxide and intramolecular geometry decide Claisen versus Dieckmann ranking.",
        "Ester under anhydrous base/workup context supports Claisen-like condensation.");
  }

  private static OrganicReactionRule michaelAdditionCarbonTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_michael_addition_carbon"),
        "Michael addition carbon transform",
        OrganicReactionRuleCategory.ADDITION,
        List.of(OrganicReactionDomain.ADDITION),
        List.of(MolecularFeatureKind.ALKENE, MolecularFeatureKind.CARBONYL),
        List.of(
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.ANHYDROUS,
            ReactionConditionFeatureKind.ADDITION_RATE),
        SubstituentAttachmentTransform.of(
            "michael_carbon_adduct",
            "Michael carbon adduct",
            ElementSymbol.of("C"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds a soft carbon nucleophile to a conjugated acceptor carbon as a Michael-type candidate."),
        0.33,
        49.0,
        "1,4- versus 1,2-addition is left as a competing candidate ranking problem.",
        "Alkene plus carbonyl features support conjugate addition screening.");
  }

  private static OrganicReactionRule mannichAminomethylationTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_mannich_aminomethylation"),
        "Mannich aminomethylation transform",
        OrganicReactionRuleCategory.ADDITION,
        List.of(OrganicReactionDomain.ADDITION, OrganicReactionDomain.ACID_BASE),
        List.of(MolecularFeatureKind.CARBONYL, MolecularFeatureKind.AMINE),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.ADDITION_RATE,
            ReactionConditionFeatureKind.HEATING),
        SubstituentAttachmentTransform.of(
            "mannich_beta_amino_carbonyl_product",
            "Mannich beta-amino carbonyl analogue",
            ElementSymbol.of("N"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds nitrogen to a carbonyl/enolizable carbon context as a Mannich iminium-addition product candidate."),
        0.38,
        54.0,
        "Iminium formation, enol content, and competing aldol pathways are retained as ranking factors.",
        "Carbonyl plus amine under acid or controlled addition supports Mannich-type aminomethylation.");
  }

  private static OrganicReactionRule streckerAminonitrileSynthesisTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_strecker_aminonitrile_synthesis"),
        "Strecker aminonitrile synthesis transform",
        OrganicReactionRuleCategory.ADDITION,
        List.of(OrganicReactionDomain.ADDITION, OrganicReactionDomain.ACID_BASE),
        List.of(MolecularFeatureKind.CARBONYL, MolecularFeatureKind.AMINE),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.ADDITION_RATE,
            ReactionConditionFeatureKind.WORKUP),
        SubstituentAttachmentTransform.of(
            "strecker_aminonitrile",
            "Strecker aminonitrile analogue",
            ElementSymbol.of("N"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds nitrogen/cyanide-equivalent character to an imine/carbonyl carbon as an aminonitrile channel."),
        0.32,
        48.0,
        "Imine formation, cyanide source strength and hydrolysis workup determine aminonitrile versus amino-acid channel.",
        "Carbonyl plus amine under acid/controlled addition supports Strecker-type aminonitrile synthesis.");
  }

  private static OrganicReactionRule ugiMulticomponentAmideTransform() {
    return new ru.pathcreator.vadim.quantum.chemistry.classical.application.rule
        .UgiOrganicReactionRule();
  }

  private static OrganicReactionRule passeriniMulticomponentEsterTransform() {
    return new ru.pathcreator.vadim.quantum.chemistry.classical.application.rule
        .PasseriniOrganicReactionRule();
  }

  private static OrganicReactionRule knoevenagelCondensationTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_knoevenagel_condensation"),
        "Knoevenagel condensation transform",
        OrganicReactionRuleCategory.ADDITION,
        List.of(OrganicReactionDomain.ADDITION, OrganicReactionDomain.ELIMINATION),
        List.of(MolecularFeatureKind.CARBONYL),
        List.of(
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.WORKUP),
        SubstituentAttachmentTransform.of(
            "knoevenagel_alkene_product",
            "Knoevenagel alkene analogue",
            ElementSymbol.of("C"),
            BondType.DOUBLE,
            ReactantAtomSelector.element("C"),
            "Adds a double-bond carbon fragment to a carbonyl carbon as a condensation/dehydration candidate."),
        0.35,
        53.0,
        "Activated methylene acidity and E/Z outcome should be ranked from substituent and condition evidence.",
        "Carbonyl context under base/heat supports Knoevenagel-type alkene formation.");
  }

  private static OrganicReactionRule henryNitroaldolTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_henry_nitroaldol"),
        "Henry nitroaldol transform",
        OrganicReactionRuleCategory.ADDITION,
        List.of(OrganicReactionDomain.ADDITION),
        List.of(MolecularFeatureKind.CARBONYL, MolecularFeatureKind.NITRO),
        List.of(
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.COOLING,
            ReactionConditionFeatureKind.ADDITION_RATE),
        SubstituentAttachmentTransform.of(
            "henry_beta_nitro_alcohol_product",
            "Henry beta-nitro alcohol analogue",
            ElementSymbol.of("N"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds nitrogen-bearing nitroalkyl character to a carbonyl carbon as a Henry nitroaldol candidate."),
        0.33,
        46.0,
        "Retro-Henry, dehydration, and stereochemical mixture remain competing channels.",
        "Carbonyl plus nitro context under base supports Henry nitroaldol screening.");
  }

  private static OrganicReactionRule olefinationCarbonylTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_carbonyl_olefination"),
        "Carbonyl olefination transform",
        OrganicReactionRuleCategory.ADDITION,
        List.of(OrganicReactionDomain.ADDITION, OrganicReactionDomain.ELIMINATION),
        List.of(MolecularFeatureKind.CARBONYL),
        List.of(
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.ANHYDROUS,
            ReactionConditionFeatureKind.COOLING),
        SubstituentAttachmentTransform.of(
            "olefination_product",
            "olefination analogue",
            ElementSymbol.of("C"),
            BondType.DOUBLE,
            ReactantAtomSelector.element("C"),
            "Adds a double-bond carbon fragment to a carbonyl carbon as a Wittig/HWE-style alkene product candidate."),
        0.36,
        55.0,
        "E/Z selectivity is represented as a follow-up stereochemical ranking issue.",
        "Carbonyl context supports olefination pathway screening.");
  }

  private static OrganicReactionRule robinsonAnnulationTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_robinson_annulation"),
        "Robinson annulation transform",
        OrganicReactionRuleCategory.ADDITION,
        List.of(
            OrganicReactionDomain.ADDITION,
            OrganicReactionDomain.CYCLIZATION,
            OrganicReactionDomain.ELIMINATION),
        List.of(MolecularFeatureKind.CARBONYL, MolecularFeatureKind.ALKENE),
        List.of(
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.WORKUP),
        SubstituentAttachmentTransform.of(
            "robinson_cyclohexenone_annulation",
            "Robinson cyclohexenone annulation analogue",
            ElementSymbol.of("C"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds carbon at an enone/carbonyl context as a Michael-aldol-dehydration annulation channel."),
        0.30,
        44.0,
        "Michael addition, aldol closure and dehydration are tracked as a staged annulation pathway.",
        "Carbonyl/enone context under base and heat supports Robinson annulation.");
  }

  private static OrganicReactionRule nazarovCyclizationTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_nazarov_cyclization"),
        "Nazarov cyclization transform",
        OrganicReactionRuleCategory.REARRANGEMENT,
        List.of(OrganicReactionDomain.REARRANGEMENT, OrganicReactionDomain.CYCLIZATION),
        List.of(MolecularFeatureKind.CARBONYL, MolecularFeatureKind.ALKENE),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.CATALYST_PRESENT),
        SubstituentAttachmentTransform.of(
            "nazarov_cyclopentenyl_cation_product",
            "Nazarov cyclopentenone analogue",
            ElementSymbol.of("C"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds a carbon connection at a divinyl-ketone-like context as a Nazarov electrocyclization channel."),
        0.24,
        36.0,
        "Cation stability, substituent rotation and torquoselectivity determine cyclopentenone product ranking.",
        "Carbonyl/alkene context under acid or Lewis acid supports Nazarov cyclization.");
  }

  private static OrganicReactionRule pausonKhandCyclopentenoneTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_pauson_khand_cyclopentenone"),
        "Pauson-Khand cyclopentenone transform",
        OrganicReactionRuleCategory.ADDITION,
        List.of(OrganicReactionDomain.ADDITION, OrganicReactionDomain.CYCLIZATION),
        List.of(MolecularFeatureKind.ALKENE, MolecularFeatureKind.ALKYNE),
        List.of(
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.INERT_ATMOSPHERE),
        SubstituentAttachmentTransform.of(
            "pauson_khand_cyclopentenone",
            "Pauson-Khand cyclopentenone analogue",
            ElementSymbol.of("C"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds a CO-equivalent carbon at an enyne context as a cyclopentenone construction channel."),
        0.23,
        34.0,
        "CO pressure, metal catalyst and enyne geometry control ring closure versus oligomerization.",
        "Alkene/alkyne context under metal catalysis supports Pauson-Khand cyclopentenone construction.");
  }

  private static OrganicReactionRule alkeneGraphEpoxidationBridge() {
    return new ru.pathcreator.vadim.quantum.chemistry.classical.application.rule
        .PeracidOxygenTransferOrganicReactionRule(
        ru.pathcreator.vadim.quantum.chemistry.classical.application.transform
            .PeracidOxygenTransferTransformer.Kind.EPOXIDATION);
  }

  private static OrganicReactionRule alkeneGraphAziridinationBridge() {
    return BridgeAdditionOrganicReactionRule.of(
        OrganicReactionRuleId.of("bridge_addition_alkene_aziridination"),
        "Alkene graph aziridination bridge",
        OrganicReactionRuleCategory.ADDITION,
        List.of(OrganicReactionDomain.ADDITION, OrganicReactionDomain.HETEROCYCLE_SYNTHESIS),
        List.of(MolecularFeatureKind.ALKENE),
        List.of(
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionConditionFeatureKind.ANHYDROUS,
            ReactionConditionFeatureKind.ADDITION_RATE),
        BridgeAdditionTransform.of(
            "alkene_aziridine_bridge",
            "aziridine bridge graph analogue",
            BondType.DOUBLE,
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            ReactantAtomSelector.element("C"),
            ElementSymbol.of("N"),
            BondType.SINGLE,
            BondType.SINGLE,
            "Converts an alkene C=C bond to C-C and adds one nitrogen bridge bonded to both alkene carbons."),
        0.34,
        46.0,
        "Aziridination candidates are generated as explicit three-membered nitrogen bridge graphs.",
        "Nitrene-transfer or metal-catalyzed aziridination can form an aziridine bridge across an alkene.");
  }

  private static OrganicReactionRule epoxideOpeningTransform() {
    return new ru.pathcreator.vadim.quantum.chemistry.classical.application.rule
        .EpoxideHydrolysisOrganicReactionRule();
  }

  private static OrganicReactionRule alkeneHydrogenationTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_alkene_hydrogenation"),
        "Alkene hydrogenation transform",
        OrganicReactionRuleCategory.REDUCTION,
        List.of(OrganicReactionDomain.ADDITION, OrganicReactionDomain.OXIDATION_REDUCTION),
        List.of(MolecularFeatureKind.ALKENE),
        List.of(
            ReactionConditionFeatureKind.HYDROGENATION,
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionConditionFeatureKind.INERT_ATMOSPHERE,
            ReactionConditionFeatureKind.HEATING),
        SubstituentAttachmentTransform.of(
            "hydrogenated_product",
            "hydrogenated analogue",
            ElementSymbol.of("H"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds hydrogen to an alkene carbon as a reduced alkane-side planning candidate."),
        0.12,
        18.0,
        "Coarse single-site hydrogenation planning channel retained alongside graph-level two-site hydrogenation.",
        "Alkene and catalyst context support hydrogenation; graph-level addition should dominate this fallback.");
  }

  private static OrganicReactionRule alkeneGraphHydrogenationAddition() {
    return BondAdditionOrganicReactionRule.of(
            OrganicReactionRuleId.of("bond_addition_alkene_hydrogenation"),
            "Alkene graph hydrogenation addition",
            OrganicReactionRuleCategory.ADDITION,
            List.of(OrganicReactionDomain.ADDITION, OrganicReactionDomain.OXIDATION_REDUCTION),
            List.of(MolecularFeatureKind.ALKENE),
            List.of(
                ReactionConditionFeatureKind.CATALYST_PRESENT,
                ReactionConditionFeatureKind.INERT_ATMOSPHERE,
                ReactionConditionFeatureKind.HEATING),
            BondAdditionTransform.of(
                "alkene_hydrogenated_graph_product",
                "hydrogenated graph analogue",
                BondType.DOUBLE,
                BondType.SINGLE,
                ReactantAtomSelector.element("C"),
                ReactantAtomSelector.element("C"),
                ElementSymbol.of("H"),
                ElementSymbol.of("H"),
                BondType.SINGLE,
                BondType.SINGLE,
                "Reduces an alkene C=C bond to C-C and adds hydrogen to both alkene carbons."),
            0.62,
            78.0,
            "Hydrogenation product is generated by changing bond order and adding both hydrogens across the alkene.",
            "Catalytic alkene hydrogenation is represented as a two-site graph addition, not a single-atom surrogate.")
        .withExplicitDonor(
            ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.HydrogenAdditionDonor
                .HYDROGEN);
  }

  private static OrganicReactionRule alkeneHydrationTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_alkene_hydration"),
        "Alkene hydration transform",
        OrganicReactionRuleCategory.ADDITION,
        List.of(OrganicReactionDomain.ADDITION),
        List.of(MolecularFeatureKind.ALKENE),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionConditionFeatureKind.WORKUP),
        SubstituentAttachmentTransform.of(
            "alkene_hydration_alcohol_product",
            "hydrated alcohol analogue",
            ElementSymbol.of("O"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds oxygen to an alkene carbon as acid-catalyzed or metal-catalyzed hydration candidate."),
        0.11,
        17.0,
        "Coarse single-site hydration planning channel retained alongside graph-level two-site hydration.",
        "Alkene plus acid/catalyst/workup context supports hydration; graph-level addition should dominate this fallback.");
  }

  private static OrganicReactionRule alkeneGraphHydrationAddition() {
    return BondAdditionOrganicReactionRule.of(
            OrganicReactionRuleId.of("bond_addition_alkene_hydration"),
            "Alkene graph hydration addition",
            OrganicReactionRuleCategory.ADDITION,
            List.of(OrganicReactionDomain.ADDITION),
            List.of(MolecularFeatureKind.ALKENE),
            List.of(
                ReactionConditionFeatureKind.ACIDIC_MEDIUM,
                ReactionConditionFeatureKind.CATALYST_PRESENT,
                ReactionConditionFeatureKind.WORKUP),
            BondAdditionTransform.of(
                "alkene_hydrated_graph_product",
                "hydrated graph alcohol analogue",
                BondType.DOUBLE,
                BondType.SINGLE,
                ReactantAtomSelector.element("C"),
                ReactantAtomSelector.element("C"),
                ElementSymbol.of("O"),
                ElementSymbol.of("H"),
                BondType.SINGLE,
                BondType.SINGLE,
                "Reduces an alkene C=C bond to C-C and adds O/H across the two alkene carbons."),
            0.43,
            52.0,
            "Hydration regioisomers are generated from both directed orientations of the matching alkene bond.",
            "Alkene hydration is represented as graph addition across the double bond.")
        .withExplicitDonor(
            ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.HydrogenAdditionDonor
                .WATER);
  }

  private static OrganicReactionRule alkeneHydrohalogenationTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_alkene_hydrohalogenation"),
        "Alkene hydrohalogenation transform",
        OrganicReactionRuleCategory.ADDITION,
        List.of(OrganicReactionDomain.ADDITION),
        List.of(MolecularFeatureKind.ALKENE, MolecularFeatureKind.HALOGENATED),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.COOLING,
            ReactionConditionFeatureKind.ADDITION_RATE),
        SubstituentAttachmentTransform.of(
            "alkene_hydrohalogenated_product",
            "hydrohalogenated analogue",
            ElementSymbol.of("Cl"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds halogen to an alkene carbon as an HX-addition planning candidate."),
        0.11,
        17.0,
        "Coarse single-site hydrohalogenation planning channel retained alongside graph-level two-site hydrohalogenation.",
        "Alkene plus halogen/acid context supports hydrohalogenation; graph-level addition should dominate this fallback.");
  }

  private static OrganicReactionRule alkeneGraphHydrohalogenationAddition() {
    return BondAdditionOrganicReactionRule.of(
            OrganicReactionRuleId.of("bond_addition_alkene_hydrohalogenation"),
            "Alkene graph hydrohalogenation addition",
            OrganicReactionRuleCategory.ADDITION,
            List.of(OrganicReactionDomain.ADDITION),
            List.of(MolecularFeatureKind.ALKENE),
            List.of(
                ReactionConditionFeatureKind.ACIDIC_MEDIUM,
                ReactionConditionFeatureKind.COOLING,
                ReactionConditionFeatureKind.ADDITION_RATE),
            BondAdditionTransform.of(
                "alkene_hydrohalogenated_graph_product",
                "hydrohalogenated graph analogue",
                BondType.DOUBLE,
                BondType.SINGLE,
                ReactantAtomSelector.element("C"),
                ReactantAtomSelector.element("C"),
                ElementSymbol.of("Cl"),
                ElementSymbol.of("H"),
                BondType.SINGLE,
                BondType.SINGLE,
                "Reduces an alkene C=C bond to C-C and adds Cl/H across the two alkene carbons."),
            0.42,
            51.0,
            "Both directed regioisomer candidates are generated before condition and selectivity ranking.",
            "Hydrohalogenation is represented as graph addition across the alkene double bond.")
        .withExplicitDonor(
            ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.HydrogenAdditionDonor
                .HYDROGEN_HALIDE);
  }

  private static OrganicReactionRule alkeneGraphHydroborationOxidationAddition() {
    return new ru.pathcreator.vadim.quantum.chemistry.classical.application.rule
        .HydroborationOxidationOrganicReactionRule();
  }

  private static OrganicReactionRule birchAreneReductionTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_birch_arene_reduction"),
        "Birch arene reduction transform",
        OrganicReactionRuleCategory.REDUCTION,
        List.of(OrganicReactionDomain.OXIDATION_REDUCTION),
        List.of(MolecularFeatureKind.AROMATIC_RING),
        List.of(
            ReactionConditionFeatureKind.REDUCING_MEDIUM,
            ReactionConditionFeatureKind.COOLING,
            ReactionConditionFeatureKind.ADDITION_RATE),
        SubstituentAttachmentTransform.of(
            "birch_cyclohexadiene_product",
            "Birch cyclohexadiene analogue",
            ElementSymbol.of("H"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds hydrogen at an aromatic carbon as a dissolved-metal arene reduction product channel."),
        0.27,
        41.0,
        "Substituent electronics control diene regiochemistry; overreduction and proton-source mismatch stay competing risks.",
        "Aromatic ring under reducing/cooling context supports Birch-type reduction.");
  }

  private static OrganicReactionRule aryneSubstitutionTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_aryne_substitution"),
        "Aryne or hetaryne substitution transform",
        OrganicReactionRuleCategory.SUBSTITUTION,
        List.of(
            OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION,
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
        List.of(MolecularFeatureKind.AROMATIC_RING, MolecularFeatureKind.HALOGENATED),
        List.of(
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.ANHYDROUS,
            ReactionConditionFeatureKind.ADDITION_RATE),
        SubstituentAttachmentTransform.of(
            "aryne_trapped_substitution_product",
            "aryne-trapped substitution analogue",
            ElementSymbol.of("C"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds a nucleophilic fragment at an aryl/hetaryl carbon as an aryne or hetaryne trapping channel."),
        0.26,
        38.0,
        "Aryne regioisomers are retained as competing channels because adjacent/ipso trapping can invert major product.",
        "Aromatic halide under strong dry base supports aryne or hetaryne substitution.");
  }

  private static OrganicReactionRule olefinMetathesisTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_olefin_metathesis"),
        "Olefin metathesis transform",
        OrganicReactionRuleCategory.ADDITION,
        List.of(OrganicReactionDomain.ADDITION),
        List.of(MolecularFeatureKind.ALKENE),
        List.of(
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionConditionFeatureKind.ANHYDROUS,
            ReactionConditionFeatureKind.HEATING),
        SubstituentAttachmentTransform.of(
            "olefin_metathesis_alkene_exchange",
            "olefin metathesis alkene-exchange analogue",
            ElementSymbol.of("C"),
            BondType.DOUBLE,
            ReactantAtomSelector.element("C"),
            "Adds an alkene carbon handle as a Ru/Mo carbene metathesis exchange product channel."),
        0.24,
        36.0,
        "Equilibrium, ethylene removal and catalyst poisoning determine cross-metathesis versus isomerization.",
        "Alkene plus dry metathesis-catalyst context supports olefin metathesis.");
  }

  private static OrganicReactionRule ringClosingMetathesisTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_ring_closing_metathesis"),
        "Ring-closing metathesis transform",
        OrganicReactionRuleCategory.ADDITION,
        List.of(OrganicReactionDomain.ADDITION, OrganicReactionDomain.CYCLIZATION),
        List.of(MolecularFeatureKind.ALKENE),
        List.of(
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionConditionFeatureKind.ANHYDROUS,
            ReactionConditionFeatureKind.ADDITION_RATE),
        SubstituentAttachmentTransform.of(
            "ring_closing_metathesis_cyclic_alkene",
            "ring-closing metathesis cyclic alkene analogue",
            ElementSymbol.of("C"),
            BondType.DOUBLE,
            ReactantAtomSelector.element("C"),
            "Adds a double-bond carbon handle as a cyclized diene metathesis product channel."),
        0.22,
        33.0,
        "Ring size, dilution and oligomerization are represented as selectivity and competing-pathway evidence.",
        "Alkene under dry metathesis-catalyst context supports ring-closing metathesis when geometry is feasible.");
  }

  private static OrganicReactionRule thiolEneClickTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_thiol_ene_click"),
        "Thiol-ene click transform",
        OrganicReactionRuleCategory.RADICAL_FUNCTIONALIZATION,
        List.of(OrganicReactionDomain.RADICAL_FUNCTIONALIZATION, OrganicReactionDomain.ADDITION),
        List.of(MolecularFeatureKind.ALKENE),
        List.of(
            ReactionConditionFeatureKind.PHOTOCHEMICAL,
            ReactionConditionFeatureKind.INERT_ATMOSPHERE,
            ReactionConditionFeatureKind.ADDITION_RATE),
        SubstituentAttachmentTransform.of(
            "thiol_ene_thioether",
            "thiol-ene thioether analogue",
            ElementSymbol.of("S"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds sulfur at an alkene carbon as a radical thiol-ene thioether product channel."),
        0.40,
        61.0,
        "Oxygen inhibition and disulfide side formation are kept as competing radical risks.",
        "Alkene under photochemical/inert sulfur-source context supports thiol-ene click addition.");
  }

  private static OrganicReactionRule alkeneGraphThiolEneClickAddition() {
    return BondAdditionOrganicReactionRule.of(
            OrganicReactionRuleId.of("bond_addition_alkene_thiol_ene_click"),
            "Alkene graph thiol-ene click addition",
            OrganicReactionRuleCategory.RADICAL_FUNCTIONALIZATION,
            List.of(
                OrganicReactionDomain.RADICAL_FUNCTIONALIZATION, OrganicReactionDomain.ADDITION),
            List.of(MolecularFeatureKind.ALKENE),
            List.of(
                ReactionConditionFeatureKind.PHOTOCHEMICAL,
                ReactionConditionFeatureKind.INERT_ATMOSPHERE,
                ReactionConditionFeatureKind.ADDITION_RATE),
            BondAdditionTransform.of(
                "alkene_thiol_ene_graph_product",
                "thiol-ene thioether graph analogue",
                BondType.DOUBLE,
                BondType.SINGLE,
                ReactantAtomSelector.element("C"),
                ReactantAtomSelector.element("C"),
                ElementSymbol.of("S"),
                ElementSymbol.of("H"),
                BondType.SINGLE,
                BondType.SINGLE,
                "Reduces an alkene C=C bond to C-C and adds S/H as a thiol-ene thioether product graph."),
            0.47,
            61.0,
            "Thiol-ene click candidates are generated as explicit thioether addition graphs before radical risk ranking.",
            "Photochemical/inert alkene context can add a sulfur fragment and hydrogen across the double bond.")
        .withExplicitDonor(
            ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.HydrogenAdditionDonor
                .THIOL);
  }

  private static OrganicReactionRule radicalCarbonFunctionalizationTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_radical_carbon_functionalization"),
        "Radical carbon functionalization transform",
        OrganicReactionRuleCategory.RADICAL_FUNCTIONALIZATION,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
            OrganicReactionDomain.RADICAL_FUNCTIONALIZATION,
            OrganicReactionDomain.ADDITION),
        List.of(MolecularFeatureKind.RADICAL_CENTER),
        List.of(
            ReactionConditionFeatureKind.PHOTOCHEMICAL,
            ReactionConditionFeatureKind.INERT_ATMOSPHERE,
            ReactionConditionFeatureKind.ADDITION_RATE),
        SubstituentAttachmentTransform.of(
            "radical_carbon_adduct",
            "radical carbon adduct",
            ElementSymbol.of("C"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds carbon at a radical-compatible carbon site as a chain or photoredox product candidate."),
        0.28,
        37.0,
        "Radical selectivity is intentionally conservative and requires follow-up kinetics.",
        "Radical center and photochemical/inert context support radical functionalization.");
  }

  private static OrganicReactionRule oneThreeDipolarHeterocycleConstructionTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_one_three_dipolar_heterocycle_construction"),
        "1,3-dipolar heterocycle construction transform",
        OrganicReactionRuleCategory.HETEROCYCLE_SYNTHESIS,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
            OrganicReactionDomain.CYCLIZATION,
            OrganicReactionDomain.ADDITION),
        List.of(MolecularFeatureKind.ALKYNE),
        List.of(
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionConditionFeatureKind.ADDITION_RATE,
            ReactionConditionFeatureKind.ANHYDROUS),
        SubstituentAttachmentTransform.of(
            "dipolar_cycloaddition_heterocycle",
            "1,3-dipolar cycloaddition heterocycle analogue",
            ElementSymbol.of("N"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds nitrogen to an alkyne carbon as a triazole/isoxazole-like cycloaddition product channel."),
        0.47,
        68.0,
        "Competing regioisomeric cycloadducts are generated over all matching alkyne carbons and ranked later.",
        "1,3-dipolar cycloaddition is a broad heterocycle construction route for unsymmetric alkyne/dipole pairs.");
  }

  private static OrganicReactionRule dielsAlderCycloadditionBridge() {
    return BridgeAdditionOrganicReactionRule.of(
        OrganicReactionRuleId.of("bridge_addition_diels_alder_cycloaddition"),
        "Diels-Alder cycloaddition bridge",
        OrganicReactionRuleCategory.ADDITION,
        List.of(OrganicReactionDomain.ADDITION, OrganicReactionDomain.CYCLIZATION),
        List.of(MolecularFeatureKind.ALKENE),
        List.of(
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionConditionFeatureKind.ADDITION_RATE),
        BridgeAdditionTransform.of(
            "diels_alder_cycloadduct_bridge",
            "Diels-Alder cycloadduct graph analogue",
            BondType.DOUBLE,
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            ReactantAtomSelector.element("C"),
            ElementSymbol.of("C"),
            BondType.SINGLE,
            BondType.SINGLE,
            "Converts an activated alkene bond to a saturated bridge-bearing cycloaddition product graph."),
        0.39,
        59.0,
        "Endo/exo and regioisomeric branches require conformation and substituent ranking after graph generation.",
        "Activated alkene/dienophile context with heat or Lewis acid catalysis supports Diels-Alder cycloaddition.");
  }

  private static OrganicReactionRule azaDielsAlderCycloadditionTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_aza_diels_alder_cycloaddition"),
        "Aza-Diels-Alder cycloaddition transform",
        OrganicReactionRuleCategory.HETEROCYCLE_SYNTHESIS,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
            OrganicReactionDomain.CYCLIZATION,
            OrganicReactionDomain.ADDITION),
        List.of(MolecularFeatureKind.IMINE, MolecularFeatureKind.ALKENE),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.ADDITION_RATE),
        SubstituentAttachmentTransform.of(
            "aza_diels_alder_nitrogen_heterocycle",
            "aza-Diels-Alder nitrogen heterocycle analogue",
            ElementSymbol.of("N"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds nitrogen into an imine/alkene cycloaddition context as an aza-heterocycle product channel."),
        0.36,
        52.0,
        "Aza-Diels-Alder branches compete with imine hydrolysis, Mannich and polymerization channels.",
        "Imine plus alkene context under acid or Lewis-acid catalysis supports aza-Diels-Alder heterocycle formation.");
  }

  private static OrganicReactionRule paalKnorrHeterocycleConstructionTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_paal_knorr_heterocycle_construction"),
        "Paal-Knorr heterocycle construction transform",
        OrganicReactionRuleCategory.HETEROCYCLE_SYNTHESIS,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
            OrganicReactionDomain.CYCLIZATION,
            OrganicReactionDomain.ADDITION),
        List.of(MolecularFeatureKind.CARBONYL),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.DEHYDRATING_AGENT,
            ReactionConditionFeatureKind.HEATING),
        SubstituentAttachmentTransform.of(
            "paal_knorr_five_membered_heterocycle",
            "Paal-Knorr five-membered heterocycle analogue",
            ElementSymbol.of("N"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds a heteroatom at a carbonyl-rich 1,4-dicarbonyl-like context as a pyrrole/furan/thiophene channel."),
        0.43,
        63.0,
        "N/O/S heteroatom source and dehydration decide the dominant pyrrole, furan, or thiophene branch.",
        "Carbonyl-rich substrate plus heteroatom donor supports Paal-Knorr-type five-membered heterocycle synthesis.");
  }

  private static OrganicReactionRule biginelliDihydropyrimidinoneConstructionTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_biginelli_dihydropyrimidinone_construction"),
        "Biginelli dihydropyrimidinone construction transform",
        OrganicReactionRuleCategory.HETEROCYCLE_SYNTHESIS,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
            OrganicReactionDomain.CYCLIZATION,
            OrganicReactionDomain.ADDITION),
        List.of(MolecularFeatureKind.CARBONYL, MolecularFeatureKind.ALKENE),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.ADDITION_RATE),
        SubstituentAttachmentTransform.of(
            "biginelli_dihydropyrimidinone_core",
            "Biginelli dihydropyrimidinone analogue",
            ElementSymbol.of("N"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds nitrogen into a carbonyl/urea/enolizable context as a dihydropyrimidinone construction candidate."),
        0.36,
        55.0,
        "Aldehyde, beta-keto carbonyl and urea/thiourea balance controls product and side-condensation ranking.",
        "Acidic multicomponent carbonyl-amide context supports Biginelli-type heterocycle construction.");
  }

  private static OrganicReactionRule gewaldThiopheneConstructionTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_gewald_thiophene_construction"),
        "Gewald thiophene construction transform",
        OrganicReactionRuleCategory.HETEROCYCLE_SYNTHESIS,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
            OrganicReactionDomain.CYCLIZATION,
            OrganicReactionDomain.ADDITION),
        List.of(MolecularFeatureKind.CARBONYL, MolecularFeatureKind.NITRILE),
        List.of(
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.ADDITION_RATE),
        SubstituentAttachmentTransform.of(
            "gewald_aminothiophene_core",
            "Gewald aminothiophene analogue",
            ElementSymbol.of("S"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds sulfur into an activated carbonyl/nitrile context as an aminothiophene product candidate."),
        0.39,
        57.0,
        "Base strength and activated methylene/nitrile availability decide thiophene formation versus side condensation.",
        "Carbonyl, nitrile and sulfur donor context supports Gewald-type thiophene construction.");
  }

  private static OrganicReactionRule fischerIndoleConstructionTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_fischer_indole_construction"),
        "Fischer indole construction transform",
        OrganicReactionRuleCategory.HETEROCYCLE_SYNTHESIS,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
            OrganicReactionDomain.REARRANGEMENT,
            OrganicReactionDomain.CYCLIZATION),
        List.of(MolecularFeatureKind.CARBONYL, MolecularFeatureKind.IMINE),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.WORKUP),
        SubstituentAttachmentTransform.of(
            "fischer_indole_core",
            "Fischer indole analogue",
            ElementSymbol.of("N"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds nitrogen into an aryl hydrazone-like carbonyl context as an indole construction candidate."),
        0.35,
        53.0,
        "Aryl migration and hydrazone geometry are represented as selectivity evidence until explicit conformer scoring.",
        "Aromatic carbonyl/imine nitrogen context supports Fischer-type indole construction.");
  }

  private static OrganicReactionRule pictetSpenglerCyclizationTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_pictet_spengler_cyclization"),
        "Pictet-Spengler cyclization transform",
        OrganicReactionRuleCategory.HETEROCYCLE_SYNTHESIS,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
            OrganicReactionDomain.CYCLIZATION,
            OrganicReactionDomain.ADDITION),
        List.of(MolecularFeatureKind.AMINE, MolecularFeatureKind.IMINE),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.COOLING,
            ReactionConditionFeatureKind.ADDITION_RATE),
        SubstituentAttachmentTransform.of(
            "pictet_spengler_fused_n_heterocycle",
            "Pictet-Spengler fused nitrogen heterocycle analogue",
            ElementSymbol.of("C"),
            BondType.SINGLE,
            ReactantAtomSelector.element("N"),
            "Adds carbon at an iminium-compatible amine context as a tetrahydroisoquinoline/beta-carboline candidate."),
        0.33,
        50.0,
        "Iminium formation, aromatic electronics and stereochemical approach are kept as ranked selectivity factors.",
        "Aromatic amine/imine context under acid supports Pictet-Spengler-type fused heterocycle construction.");
  }

  private static OrganicReactionRule skraupQuinolineConstructionTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_skraup_quinoline_construction"),
        "Skraup quinoline construction transform",
        OrganicReactionRuleCategory.HETEROCYCLE_SYNTHESIS,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
            OrganicReactionDomain.CYCLIZATION,
            OrganicReactionDomain.OXIDATION_REDUCTION),
        List.of(MolecularFeatureKind.AROMATIC_RING, MolecularFeatureKind.AMINE),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.OXIDIZING_MEDIUM,
            ReactionConditionFeatureKind.HEATING),
        SubstituentAttachmentTransform.of(
            "skraup_quinoline_core",
            "Skraup quinoline analogue",
            ElementSymbol.of("C"),
            BondType.SINGLE,
            ReactantAtomSelector.element("N"),
            "Adds carbon at an aniline-like nitrogen context as an acid/oxidant quinoline construction candidate."),
        0.30,
        46.0,
        "Acrolein-equivalent formation, acid strength and oxidation decide quinoline formation versus polymerization.",
        "Aromatic amine under acid/oxidant heating supports Skraup-type quinoline synthesis.");
  }

  private static OrganicReactionRule combesQuinolineAnnulationTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_combes_quinoline_annulation"),
        "Combes quinoline annulation transform",
        OrganicReactionRuleCategory.HETEROCYCLE_SYNTHESIS,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
            OrganicReactionDomain.CYCLIZATION,
            OrganicReactionDomain.ADDITION),
        List.of(
            MolecularFeatureKind.AROMATIC_RING,
            MolecularFeatureKind.AMINE,
            MolecularFeatureKind.CARBONYL),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.ADDITION_RATE),
        SubstituentAttachmentTransform.of(
            "combes_quinoline_annulated_core",
            "Combes quinoline annulated analogue",
            ElementSymbol.of("C"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds carbon at an aniline/carbonyl condensation context as a quinoline annulation candidate."),
        0.34,
        51.0,
        "Substituent-controlled regioisomers are retained as competing channels and calibrated by outcome priors.",
        "Aromatic amine plus carbonyl under acid heating supports Combes-type quinoline annulation.");
  }

  private static OrganicReactionRule hantzschLikePyridineConstructionTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_hantzsch_like_pyridine_construction"),
        "Hantzsch-like pyridine construction transform",
        OrganicReactionRuleCategory.HETEROCYCLE_SYNTHESIS,
        List.of(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS, OrganicReactionDomain.CYCLIZATION),
        List.of(MolecularFeatureKind.CARBONYL, MolecularFeatureKind.AMINE),
        List.of(
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.ADDITION_RATE,
            ReactionConditionFeatureKind.WORKUP),
        SubstituentAttachmentTransform.of(
            "hantzsch_pyridine_nitrogen_incorporation",
            "Hantzsch pyridine nitrogen-incorporation analogue",
            ElementSymbol.of("N"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds nitrogen into a carbonyl-rich substrate context as a Hantzsch-like pyridine construction candidate."),
        0.40,
        58.0,
        "Multiple condensation/aromatization routes remain competing channels until kinetics and stoichiometry are known.",
        "Carbonyl and nitrogen-donor context supports Hantzsch-like pyridine or dihydropyridine construction.");
  }

  private static OrganicReactionRule krohnkeLikePyridineConstructionTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_krohnke_like_pyridine_construction"),
        "Krohnke-like pyridine construction transform",
        OrganicReactionRuleCategory.HETEROCYCLE_SYNTHESIS,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
            OrganicReactionDomain.CYCLIZATION,
            OrganicReactionDomain.ADDITION),
        List.of(
            MolecularFeatureKind.NITROGEN_HETEROCYCLE,
            MolecularFeatureKind.CARBONYL,
            MolecularFeatureKind.ALKENE),
        List.of(
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.ADDITION_RATE),
        SubstituentAttachmentTransform.of(
            "krohnke_polyaryl_pyridine_adduct",
            "Krohnke-like polyaryl pyridine analogue",
            ElementSymbol.of("C"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds carbon at an activated carbonyl/alkene context as a Krohnke-like pyridine construction candidate."),
        0.34,
        52.0,
        "Substituted pyridine regioisomers are retained as separate product channels.",
        "Nitrogen heterocycle plus enone/carbonyl context supports Krohnke-like pyridine construction.");
  }

  private static OrganicReactionRule indoleDearomatizationAnnulationTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_indole_dearomatization_annulation"),
        "Indole or pyrrole dearomatization-annulation transform",
        OrganicReactionRuleCategory.HETEROCYCLE_FUNCTIONALIZATION,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
            OrganicReactionDomain.REARRANGEMENT),
        List.of(
            MolecularFeatureKind.HETEROCYCLE,
            MolecularFeatureKind.NITROGEN_HETEROCYCLE,
            MolecularFeatureKind.AROMATIC_RING),
        List.of(
            ReactionConditionFeatureKind.CATALYST_PRESENT,
            ReactionConditionFeatureKind.COOLING,
            ReactionConditionFeatureKind.ANHYDROUS),
        SubstituentAttachmentTransform.of(
            "dearomatized_annulation_product",
            "dearomatized annulation analogue",
            ElementSymbol.of("C"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds carbon to an N-heteroaromatic carbon as a catalytic dearomatization/annulation product channel."),
        0.32,
        50.0,
        "Regio-, stereo-, and enantioselective outcomes are kept as ranked candidate channels.",
        "Catalytic dearomatization of nitrogen heterocycles can create polycyclic or annulated product families.");
  }

  private static OrganicReactionRule minisciNitrogenHeterocycleCarbonFunctionalizationTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_minisci_nitrogen_heterocycle_carbon_functionalization"),
        "Minisci nitrogen heterocycle carbon functionalization transform",
        OrganicReactionRuleCategory.RADICAL_FUNCTIONALIZATION,
        List.of(
            OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
            OrganicReactionDomain.RADICAL_FUNCTIONALIZATION),
        List.of(
            MolecularFeatureKind.HETEROCYCLE,
            MolecularFeatureKind.NITROGEN_HETEROCYCLE,
            MolecularFeatureKind.AROMATIC_RING),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.PHOTOCHEMICAL,
            ReactionConditionFeatureKind.HEATING,
            ReactionConditionFeatureKind.ADDITION_RATE),
        SubstituentAttachmentTransform.of(
            "minisci_carbon_adduct",
            "Minisci carbon adduct",
            ElementSymbol.of("C"),
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            "Adds a carbon fragment at a nitrogen-heteroaryl carbon as a Minisci-like product channel."),
        0.36,
        42.0,
        "Radical heteroarene regioisomers are ranked as competing product channels; exact site confidence requires calibration.",
        "Acidic or photochemical nitrogen-heterocycle context supports Minisci-like radical substitution.");
  }

  private static OrganicReactionRule alcoholProtectionTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_alcohol_protection"),
        "Alcohol protection transform",
        OrganicReactionRuleCategory.PROTECTION,
        List.of(OrganicReactionDomain.PROTECTION_DEPROTECTION),
        List.of(MolecularFeatureKind.HYDROXYL_OR_ALKOXY),
        List.of(
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.ANHYDROUS,
            ReactionConditionFeatureKind.ADDITION_RATE),
        SubstituentAttachmentTransform.of(
            "protected_ether_product",
            "protected ether analogue",
            ElementSymbol.of("C"),
            BondType.SINGLE,
            ReactantAtomSelector.element("O"),
            "Adds carbon to oxygen as an ether/silyl-ether-like protection product channel."),
        0.52,
        80.0,
        "Protecting group identity is abstracted; exact reagent choice belongs to route planning.",
        "Alcohol/alkoxy feature supports protection product screening.");
  }

  private static OrganicReactionRule etherDeprotectionTransform() {
    return TransformingSubstituentOrganicReactionRule.of(
        OrganicReactionRuleId.of("transform_ether_deprotection"),
        "Ether deprotection transform",
        OrganicReactionRuleCategory.DEPROTECTION,
        List.of(OrganicReactionDomain.PROTECTION_DEPROTECTION),
        List.of(MolecularFeatureKind.ETHER),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.WORKUP,
            ReactionConditionFeatureKind.COOLING),
        SubstituentAttachmentTransform.of(
            "deprotected_oxygen_product",
            "deprotected oxygen analogue",
            ElementSymbol.of("H"),
            BondType.SINGLE,
            ReactantAtomSelector.element("O"),
            "Adds hydrogen to an ether oxygen as a deprotection/workup product channel."),
        0.37,
        54.0,
        "Cleavage site and protecting-group identity should be checked in a dedicated route record.",
        "Ether feature plus acidic/workup context supports deprotection screening.");
  }

  private static OrganicReactionRule etherGraphDeprotectionCleavage() {
    return BondCleavageOrganicReactionRule.of(
        OrganicReactionRuleId.of("bond_cleavage_ether_deprotection"),
        "Ether graph deprotection cleavage",
        OrganicReactionRuleCategory.DEPROTECTION,
        List.of(OrganicReactionDomain.PROTECTION_DEPROTECTION),
        List.of(MolecularFeatureKind.ETHER),
        List.of(
            ReactionConditionFeatureKind.ACIDIC_MEDIUM,
            ReactionConditionFeatureKind.BASIC_MEDIUM,
            ReactionConditionFeatureKind.WORKUP),
        BondCleavageTransform.of(
            "ether_cleaved_graph",
            "ether-cleaved graph analogue",
            BondType.SINGLE,
            ReactantAtomSelector.element("C"),
            ReactantAtomSelector.element("O"),
            "Removes a C-O single bond from an ether-like graph so downstream fragment analysis can split protected and deprotected parts."),
        0.39,
        52.0,
        "Cleavage products should be protonated or trapped by a later condition-aware neutralization step.",
        "Ether feature under acid/base/workup context supports graph-level deprotection cleavage.");
  }

  private static OrganicReactionRule carbonylGraphBaeyerVilligerOxygenInsertion() {
    return new ru.pathcreator.vadim.quantum.chemistry.classical.application.rule
        .PeracidOxygenTransferOrganicReactionRule(
        ru.pathcreator.vadim.quantum.chemistry.classical.application.transform
            .PeracidOxygenTransferTransformer.Kind.BAEYER_VILLIGER);
  }
}

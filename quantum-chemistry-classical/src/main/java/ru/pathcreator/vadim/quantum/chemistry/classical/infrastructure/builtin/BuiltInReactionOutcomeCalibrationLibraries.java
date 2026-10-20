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
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeCalibrationLibrary;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeCalibrationObservation;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeDistributionDataset;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeSourceQuality;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionRegioselectivityClass;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;

/** Встроенные calibration-наборы для проверки и настройки гетероциклических prediction workflows. */
public final class BuiltInReactionOutcomeCalibrationLibraries {

  private BuiltInReactionOutcomeCalibrationLibraries() {}

  public static ReactionOutcomeCalibrationLibrary heterocycleProductDistributions() {
    final ReactionOutcomeCalibrationObservation heteroarylChlorideBenzylamine =
        ReactionOutcomeCalibrationObservation.of(
            "heteroaryl_chloride_benzylamine_snar_major",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION,
            List.of(ReactionConditionFeatureKind.HEATING),
            "C15H15N3 + ClH",
            "major_snar_amine_fragment_transfer",
            ReactionRegioselectivityClass.LEAVING_GROUP_DIRECTED,
            List.of("activated_chloro_nicotinonitrile", "benzylamine", "aza_heteroaryl_chloride"),
            List.of("snar", "amine-fragment", "leaving-group"),
            0.72,
            71.5,
            0.92,
            ReactionOutcomeSourceQuality.LOCAL_BENCHMARK,
            "Local benchmark: 2-chloro-4,6-dimethyl-3-pyridinecarbonitrile plus benzylamine.",
            "Activated chloro-nicotinonitrile under amine/heating conditions favors full amine-fragment SNAr.");
    final ReactionOutcomeCalibrationObservation anrorcAzaHeterocycle =
        ReactionOutcomeCalibrationObservation.of(
            "activated_aza_heterocycle_anrorc_competing_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION,
                OrganicReactionDomain.REARRANGEMENT),
            ReactionMechanismFamily.ADDITION_RING_OPENING_RING_CLOSURE,
            List.of(
                ReactionConditionFeatureKind.BASIC_MEDIUM, ReactionConditionFeatureKind.HEATING),
            "C5H6N2 + ClH",
            "anrorc_aza_heterocycle_substitution_prior",
            ReactionRegioselectivityClass.CONDITION_CONTROLLED,
            List.of("activated_aza_heterocycle", "halopyridine", "aminating_nucleophile"),
            List.of("anrorc", "ring opening", "ring closure"),
            0.28,
            28.0,
            0.60,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Literature prior class: ANRORC/addition-ring-opening-ring-closure substitution of activated aza-heterocycles.",
            "Basic nucleophilic conditions can compete with direct SNAr through an addition/ring-opening/ring-closure channel.");
    final ReactionOutcomeCalibrationObservation minisciMixture =
        ReactionOutcomeCalibrationObservation.of(
            "minisci_heterocycle_regioisomer_mixture_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.RADICAL_FUNCTIONALIZATION),
            ReactionMechanismFamily.RADICAL_SUBSTITUTION,
            List.of(
                ReactionConditionFeatureKind.ACIDIC_MEDIUM,
                ReactionConditionFeatureKind.PHOTOCHEMICAL),
            "C9H10N",
            "minisci_regioisomer_mixture_prior",
            ReactionRegioselectivityClass.COMPETING_REGIOISOMERS,
            List.of("pyridine", "pyridinium", "nitrogen_heterocycle", "radical_source"),
            List.of("minisci", "regioisomer", "c2", "c4", "c6"),
            0.35,
            35.0,
            0.55,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "pmc:PMC6270771",
            "Tauber/Imbri/Opatz Molecules 2014 review, radical addition to cationic heterocycles",
            false,
            "Literature prior class: Minisci-type radical alkylation of nitrogen heterocycles; see Proctor/Phipps review and pyridinium regioselectivity literature.",
            "N-heterocycle radical substitution often gives regioisomer/product mixtures; exact entry must be refined per substrate.");
    final ReactionOutcomeCalibrationObservation pyridiniumC4 =
        ReactionOutcomeCalibrationObservation.of(
            "pyridinium_c4_blocking_group_regioselectivity_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.RADICAL_FUNCTIONALIZATION),
            ReactionMechanismFamily.RADICAL_SUBSTITUTION,
            List.of(ReactionConditionFeatureKind.ACIDIC_MEDIUM),
            "C9H10N",
            "pyridinium_c4_directed_radical_addition_prior",
            ReactionRegioselectivityClass.STERICALLY_CONTROLLED,
            List.of("pyridinium", "n_alkyl_pyridinium", "blocking_group"),
            List.of("c4", "blocking group", "regioselective"),
            0.62,
            62.0,
            0.65,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "jacs:choi-laudadio-godineau-baran-2021-c4-alkylated-pyridines",
            "JACS 2021 practical/regioselective C-4 alkylated pyridines title-level source",
            false,
            "JACS 2021 Choi/Laudadio/Godineau/Baran practical C-4 alkylated pyridine yield-table class.",
            "Blocking groups and pyridinium activation can move radical/ionic addition toward a C4-dominant channel.");
    final ReactionOutcomeCalibrationObservation pyridiniumOneFourAddition =
        ReactionOutcomeCalibrationObservation.of(
            "n_alkyl_pyridinium_one_four_addition_major_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.ADDITION),
            ReactionMechanismFamily.ADDITION,
            List.of(
                ReactionConditionFeatureKind.ACIDIC_MEDIUM,
                ReactionConditionFeatureKind.CATALYST_PRESENT),
            "dihydropyridine_variable_formula",
            "pyridinium_one_four_dihydropyridine_major_channel",
            ReactionRegioselectivityClass.ELECTRONICALLY_CONTROLLED,
            List.of("pyridinium", "n_alkyl_pyridinium", "carbon_nucleophile", "heteroarene"),
            List.of("1,4", "c4", "dihydropyridine", "major", "pyridinium"),
            0.60,
            60.0,
            0.62,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Literature prior class: N-alkyl pyridinium nucleophilic addition regioselectivity; see Knight/Tolchin/Smith Chem. Commun. 2021 and related pyridinium addition studies.",
            "Pyridinium activation and substrate substitution often make 1,4-addition a major channel for carbon nucleophiles.");
    final ReactionOutcomeCalibrationObservation pyridiniumOneTwoAddition =
        ReactionOutcomeCalibrationObservation.of(
            "n_alkyl_pyridinium_one_two_addition_competing_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.ADDITION),
            ReactionMechanismFamily.ADDITION,
            List.of(
                ReactionConditionFeatureKind.ACIDIC_MEDIUM,
                ReactionConditionFeatureKind.CATALYST_PRESENT),
            "dihydropyridine_variable_formula",
            "pyridinium_one_two_dihydropyridine_competing_channel",
            ReactionRegioselectivityClass.COMPETING_REGIOISOMERS,
            List.of("pyridinium", "n_alkyl_pyridinium", "carbon_nucleophile", "heteroarene"),
            List.of("1,2", "c2", "dihydropyridine", "minor", "pyridinium"),
            0.40,
            40.0,
            0.62,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Literature prior class: N-alkyl pyridinium nucleophilic addition regioselectivity; see Knight/Tolchin/Smith Chem. Commun. 2021 and related pyridinium addition studies.",
            "1,2-addition is retained as a real competing pyridinium channel when electronics or sterics redirect the nucleophile.");
    final ReactionOutcomeCalibrationObservation pyridiniumAmineRingOpening =
        ReactionOutcomeCalibrationObservation.of(
            "pyridinium_amine_zincke_ring_opening_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION,
                OrganicReactionDomain.REARRANGEMENT),
            ReactionMechanismFamily.ADDITION_RING_OPENING_RING_CLOSURE,
            List.of(ReactionConditionFeatureKind.BASIC_MEDIUM),
            "zincke_amine_ring_opening_variable_formula",
            "pyridinium_amine_ring_opening_zincke_channel",
            ReactionRegioselectivityClass.CONDITION_CONTROLLED,
            List.of("pyridinium", "n_alkyl_pyridinium", "amine", "nucleophile"),
            List.of("amine", "ring opening", "zincke", "aldehyde", "imine"),
            0.55,
            55.0,
            0.58,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Literature prior class: pyridinium reactions with amine nucleophiles can enter Zincke-type ring-opening channels.",
            "Amines can add to activated pyridinium systems and open the ring, so substitution-only predictions must be penalized unless structure/conditions support them.");
    final ReactionOutcomeCalibrationObservation pyridiniumAmineSubstitution =
        ReactionOutcomeCalibrationObservation.of(
            "pyridinium_amine_substitution_retained_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION,
            List.of(ReactionConditionFeatureKind.BASIC_MEDIUM),
            "aminopyridinium_variable_formula",
            "pyridinium_amine_substitution_retained_channel",
            ReactionRegioselectivityClass.LEAVING_GROUP_DIRECTED,
            List.of("pyridinium", "halopyridinium", "amine", "nucleophile"),
            List.of("amine", "substitution", "retained ring", "leaving-group"),
            0.45,
            45.0,
            0.58,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Literature prior class: halogenated pyridinium/aza-heteroaryl systems can still retain the ring through directed SNAr substitution.",
            "An explicit leaving group and activated heteroaryl framework preserve a competing retained-ring substitution channel.");
    final ReactionOutcomeCalibrationObservation photochemicalPopulation =
        ReactionOutcomeCalibrationObservation.of(
            "thiophenone_photochemical_episulfide_population_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.REARRANGEMENT),
            ReactionMechanismFamily.PHOTOCHEMICAL,
            List.of(ReactionConditionFeatureKind.PHOTOCHEMICAL),
            "C4H4OS",
            "photochemical_heterocycle_episulfide_population",
            ReactionRegioselectivityClass.CONDITION_CONTROLLED,
            List.of("thiophenone", "sulfur_heterocycle", "photochemical_heterocycle"),
            List.of("photochemical", "episulfide", "population", "same-formula"),
            0.50,
            50.0,
            0.70,
            ReactionOutcomeSourceQuality.TIME_RESOLVED_POPULATION_STUDY,
            "arXiv:2311.12482",
            "reported early-time product population near 1 ps",
            true,
            "Time-resolved 2(5H)-thiophenone photoproduct population study; arXiv:2311.12482.",
            "Photochemical heterocycle rearrangements can split into competing isomeric products with experimentally resolvable populations.");
    final ReactionOutcomeCalibrationObservation dipolarCycloaddition =
        ReactionOutcomeCalibrationObservation.of(
            "dipolar_cycloaddition_unsymmetric_alkyne_regioisomer_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
                OrganicReactionDomain.CYCLIZATION,
                OrganicReactionDomain.ADDITION),
            ReactionMechanismFamily.CLICK_CYCLOADDITION,
            List.of(
                ReactionConditionFeatureKind.CATALYST_PRESENT,
                ReactionConditionFeatureKind.ADDITION_RATE),
            "heterocycle_construction_variable_formula",
            "dipolar_cycloaddition_major_regioisomer",
            ReactionRegioselectivityClass.COMPETING_REGIOISOMERS,
            List.of("alkyne", "azide", "nitrile_oxide", "dipole", "dipolarophile"),
            List.of("triazole", "isoxazole", "regioisomer", "cycloaddition"),
            0.66,
            66.0,
            0.72,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Literature prior class: azide/nitrile-oxide 1,3-dipolar cycloaddition to unsymmetric alkynes.",
            "Unsymmetric dipole/dipolarophile pairs usually produce major/minor regioisomeric heterocycles.");
    final ReactionOutcomeCalibrationObservation hantzschPyridine =
        ReactionOutcomeCalibrationObservation.of(
            "hantzsch_like_pyridine_construction_prior",
            List.of(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS, OrganicReactionDomain.CYCLIZATION),
            ReactionMechanismFamily.MULTICOMPONENT_CONDENSATION,
            List.of(ReactionConditionFeatureKind.HEATING, ReactionConditionFeatureKind.WORKUP),
            "heterocycle_construction_variable_formula",
            "hantzsch_major_pyridine_or_dihydropyridine_channel",
            ReactionRegioselectivityClass.CONDITION_CONTROLLED,
            List.of("aldehyde", "carbonyl", "amine", "ammonium", "beta_ketoester"),
            List.of("hantzsch", "pyridine", "dihydropyridine", "condensation"),
            0.58,
            58.0,
            0.60,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Literature prior class: Hantzsch-type multicomponent pyridine/dihydropyridine construction.",
            "Carbonyl stoichiometry, nitrogen source and oxidation/workup split pyridine and dihydropyridine channels.");
    final ReactionOutcomeCalibrationObservation krohnkePyridine =
        ReactionOutcomeCalibrationObservation.of(
            "krohnke_like_pyridine_construction_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
                OrganicReactionDomain.CYCLIZATION,
                OrganicReactionDomain.ADDITION),
            ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION,
            List.of(
                ReactionConditionFeatureKind.BASIC_MEDIUM, ReactionConditionFeatureKind.HEATING),
            "heterocycle_construction_variable_formula",
            "krohnke_polyaryl_pyridine_channel",
            ReactionRegioselectivityClass.ELECTRONICALLY_CONTROLLED,
            List.of("pyridinium", "enone", "carbonyl", "alpha_beta_unsaturated"),
            List.of("krohnke", "pyridine", "polyaryl", "annulation"),
            0.46,
            46.0,
            0.56,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Literature prior class: Krohnke-like substituted pyridine construction from pyridinium/enone-carbonyl manifolds.",
            "Substituent pattern and enone electronics define a family of pyridine construction channels.");
    final ReactionOutcomeCalibrationObservation dearomatizationAnnulation =
        ReactionOutcomeCalibrationObservation.of(
            "n_heteroarene_dearomatization_annulation_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.REARRANGEMENT),
            ReactionMechanismFamily.HETEROCYCLE_FUNCTIONALIZATION,
            List.of(
                ReactionConditionFeatureKind.CATALYST_PRESENT,
                ReactionConditionFeatureKind.COOLING),
            "heterocycle_functionalization_variable_formula",
            "catalytic_dearomatization_annulation_channel",
            ReactionRegioselectivityClass.STERICALLY_CONTROLLED,
            List.of("indole", "pyrrole", "nitrogen_heterocycle", "catalyst", "dearomatization"),
            List.of("dearomatization", "annulation", "stereoselective", "polycyclic"),
            0.42,
            42.0,
            0.54,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Literature prior class: catalytic dearomatization and annulation of nitrogen heterocycles.",
            "Catalyst, protonation state and substrate face selectivity split regio/stereo product families.");
    final ReactionOutcomeCalibrationObservation combesTwoCf3Quinoline =
        ReactionOutcomeCalibrationObservation.of(
            "combes_modified_two_cf3_quinoline_regioisomer_prior",
            List.of(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS, OrganicReactionDomain.CYCLIZATION),
            ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION,
            List.of(
                ReactionConditionFeatureKind.ACIDIC_MEDIUM, ReactionConditionFeatureKind.HEATING),
            "trifluoromethyl_quinoline_variable_formula",
            "modified_combes_2_cf3_quinoline_channel",
            ReactionRegioselectivityClass.STERICALLY_CONTROLLED,
            List.of("aniline", "beta_diketone", "trifluoromethyl", "polyphosphoric_ester"),
            List.of("combes", "quinoline", "2-cf3", "regioisomer"),
            0.57,
            57.0,
            0.58,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "doi:10.1002/poc.1433",
            "Zenodo record 1229326 description, modified Combes 19F NMR regioselectivity/rate study",
            false,
            "Modified Combes quinoline formation class prior; substituent-controlled trifluoromethyl-quinoline regioisomers.",
            "Bulky diketone substituents and electron-rich anilines can favor the 2-CF3 quinoline channel.");
    final ReactionOutcomeCalibrationObservation combesFourCf3Quinoline =
        ReactionOutcomeCalibrationObservation.of(
            "combes_modified_four_cf3_quinoline_regioisomer_prior",
            List.of(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS, OrganicReactionDomain.CYCLIZATION),
            ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION,
            List.of(
                ReactionConditionFeatureKind.ACIDIC_MEDIUM, ReactionConditionFeatureKind.HEATING),
            "trifluoromethyl_quinoline_variable_formula",
            "modified_combes_4_cf3_quinoline_channel",
            ReactionRegioselectivityClass.ELECTRONICALLY_CONTROLLED,
            List.of("aniline", "beta_diketone", "trifluoromethyl", "haloaniline"),
            List.of("combes", "quinoline", "4-cf3", "regioisomer"),
            0.43,
            43.0,
            0.58,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "doi:10.1002/poc.1433",
            "Zenodo record 1229326 description, modified Combes 19F NMR regioselectivity/rate study",
            false,
            "Modified Combes quinoline formation class prior; substituent-controlled trifluoromethyl-quinoline regioisomers.",
            "Halogenated or less electron-rich anilines can shift the major channel toward 4-CF3 quinoline.");
    final ReactionOutcomeCalibrationObservation combesTableOneEntryOneTwoCf3 =
        ReactionOutcomeCalibrationObservation.of(
            "sloop_combes_table1_entry1_2a_2cf3_benchmark",
            List.of(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS, OrganicReactionDomain.CYCLIZATION),
            ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION,
            List.of(
                ReactionConditionFeatureKind.ACIDIC_MEDIUM, ReactionConditionFeatureKind.HEATING),
            "c11h8f3n",
            "table1_entry1_2a_2cf3_minor_channel",
            ReactionRegioselectivityClass.COMPETING_REGIOISOMERS,
            List.of("aniline", "trifluoromethyl_beta_diketone", "ppa_ethanol", "modified_combes"),
            List.of("2a", "2-cf3", "minor", "quinoline"),
            0.28,
            28.0,
            0.92,
            ReactionOutcomeSourceQuality.EXACT_EXPERIMENTAL_PRODUCT_DISTRIBUTION,
            "doi:10.1002/poc.1433",
            "Table 1 entry 1: 2a(28)/3a(72), 343 K, product distribution before purification",
            true,
            "Sloop modified Combes quinoline kinetic/regioselectivity benchmark row.",
            "Use only as external validation for regioisomer distribution, not as prediction lookup.");
    final ReactionOutcomeCalibrationObservation combesTableOneEntryOneFourCf3 =
        ReactionOutcomeCalibrationObservation.of(
            "sloop_combes_table1_entry1_3a_4cf3_benchmark",
            List.of(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS, OrganicReactionDomain.CYCLIZATION),
            ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION,
            List.of(
                ReactionConditionFeatureKind.ACIDIC_MEDIUM, ReactionConditionFeatureKind.HEATING),
            "c11h8f3n",
            "table1_entry1_3a_4cf3_major_channel",
            ReactionRegioselectivityClass.COMPETING_REGIOISOMERS,
            List.of("aniline", "trifluoromethyl_beta_diketone", "ppa_ethanol", "modified_combes"),
            List.of("3a", "4-cf3", "major", "quinoline"),
            0.72,
            72.0,
            0.92,
            ReactionOutcomeSourceQuality.EXACT_EXPERIMENTAL_PRODUCT_DISTRIBUTION,
            "doi:10.1002/poc.1433",
            "Table 1 entry 1: 2a(28)/3a(72), 343 K, product distribution before purification",
            true,
            "Sloop modified Combes quinoline kinetic/regioselectivity benchmark row.",
            "Use only as external validation for regioisomer distribution, not as prediction lookup.");
    final ReactionOutcomeCalibrationObservation combesTableOneEntryThreeTwoCf3 =
        ReactionOutcomeCalibrationObservation.of(
            "sloop_combes_table1_entry3_2c_2cf3_benchmark",
            List.of(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS, OrganicReactionDomain.CYCLIZATION),
            ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION,
            List.of(
                ReactionConditionFeatureKind.ACIDIC_MEDIUM, ReactionConditionFeatureKind.HEATING),
            "c12h10f3n",
            "table1_entry3_2c_2cf3_major_channel",
            ReactionRegioselectivityClass.STERICALLY_CONTROLLED,
            List.of(
                "aniline", "ethyl_trifluoromethyl_beta_diketone", "ppa_ethanol", "modified_combes"),
            List.of("2c", "2-cf3", "major", "quinoline"),
            0.90,
            90.0,
            0.92,
            ReactionOutcomeSourceQuality.EXACT_EXPERIMENTAL_PRODUCT_DISTRIBUTION,
            "doi:10.1002/poc.1433",
            "Table 1 entry 3: 2c(90)/3c(10), 343 K, product distribution before purification",
            true,
            "Sloop modified Combes quinoline kinetic/regioselectivity benchmark row.",
            "Use only as external validation for substituent-controlled regioisomer distribution.");
    final ReactionOutcomeCalibrationObservation combesTableOneEntryThreeFourCf3 =
        ReactionOutcomeCalibrationObservation.of(
            "sloop_combes_table1_entry3_3c_4cf3_benchmark",
            List.of(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS, OrganicReactionDomain.CYCLIZATION),
            ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION,
            List.of(
                ReactionConditionFeatureKind.ACIDIC_MEDIUM, ReactionConditionFeatureKind.HEATING),
            "c12h10f3n",
            "table1_entry3_3c_4cf3_minor_channel",
            ReactionRegioselectivityClass.COMPETING_REGIOISOMERS,
            List.of(
                "aniline", "ethyl_trifluoromethyl_beta_diketone", "ppa_ethanol", "modified_combes"),
            List.of("3c", "4-cf3", "minor", "quinoline"),
            0.10,
            10.0,
            0.92,
            ReactionOutcomeSourceQuality.EXACT_EXPERIMENTAL_PRODUCT_DISTRIBUTION,
            "doi:10.1002/poc.1433",
            "Table 1 entry 3: 2c(90)/3c(10), 343 K, product distribution before purification",
            true,
            "Sloop modified Combes quinoline kinetic/regioselectivity benchmark row.",
            "Use only as external validation for substituent-controlled regioisomer distribution.");
    final ReactionOutcomeCalibrationObservation soaiAmplifiedMajorEnantiomer =
        ReactionOutcomeCalibrationObservation.of(
            "soai_pyrimidyl_alcohol_amplified_major_enantiomer_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.ADDITION),
            ReactionMechanismFamily.ORGANOCATALYSIS,
            List.of(
                ReactionConditionFeatureKind.CATALYST_PRESENT,
                ReactionConditionFeatureKind.ADDITION_RATE),
            "chiral_pyrimidyl_alcohol_variable_formula",
            "asymmetric_autocatalytic_major_enantiomer_channel",
            ReactionRegioselectivityClass.CONDITION_CONTROLLED,
            List.of("pyrimidine_carbaldehyde", "dialkylzinc", "chiral_seed", "autocatalysis"),
            List.of("soai", "pyrimidyl alcohol", "enantioenrichment", "major enantiomer"),
            0.94,
            94.0,
            0.64,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Soai-type pyrimidine-carbaldehyde asymmetric autocatalysis class prior.",
            "A small enantiomeric imbalance can be amplified into a dominant pyrimidyl-alcohol enantiomer.");
    final ReactionOutcomeCalibrationObservation soaiMinorEnantiomer =
        ReactionOutcomeCalibrationObservation.of(
            "soai_pyrimidyl_alcohol_minor_enantiomer_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.ADDITION),
            ReactionMechanismFamily.ORGANOCATALYSIS,
            List.of(
                ReactionConditionFeatureKind.CATALYST_PRESENT,
                ReactionConditionFeatureKind.ADDITION_RATE),
            "chiral_pyrimidyl_alcohol_variable_formula",
            "asymmetric_autocatalytic_minor_enantiomer_channel",
            ReactionRegioselectivityClass.CONDITION_CONTROLLED,
            List.of("pyrimidine_carbaldehyde", "dialkylzinc", "chiral_seed", "autocatalysis"),
            List.of("soai", "pyrimidyl alcohol", "minor enantiomer"),
            0.06,
            6.0,
            0.64,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Soai-type pyrimidine-carbaldehyde asymmetric autocatalysis class prior.",
            "The opposite enantiomer remains a minor channel after autocatalytic amplification.");
    final ReactionOutcomeCalibrationObservation paalKnorrPyrrole =
        ReactionOutcomeCalibrationObservation.of(
            "paal_knorr_pyrrole_channel_prior",
            List.of(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS, OrganicReactionDomain.CYCLIZATION),
            ReactionMechanismFamily.MULTICOMPONENT_CONDENSATION,
            List.of(
                ReactionConditionFeatureKind.ACIDIC_MEDIUM, ReactionConditionFeatureKind.HEATING),
            "substituted_pyrrole_variable_formula",
            "paal_knorr_pyrrole_major_channel",
            ReactionRegioselectivityClass.SINGLE_DOMINANT_SITE,
            List.of("one_four_dicarbonyl", "primary_amine", "pyrrole"),
            List.of("paal-knorr", "pyrrole", "heterocycle construction"),
            0.61,
            61.0,
            0.60,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Paal-Knorr pyrrole synthesis class prior from 1,4-dicarbonyl compounds and amines.",
            "Nitrogen source and dicarbonyl substitution define the pyrrole construction channel.");
    final ReactionOutcomeCalibrationObservation knorrPyrazoleRegioisomer =
        ReactionOutcomeCalibrationObservation.of(
            "knorr_pyrazole_regioisomer_channel_prior",
            List.of(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS, OrganicReactionDomain.CYCLIZATION),
            ReactionMechanismFamily.MULTICOMPONENT_CONDENSATION,
            List.of(
                ReactionConditionFeatureKind.ACIDIC_MEDIUM, ReactionConditionFeatureKind.HEATING),
            "substituted_pyrazole_variable_formula",
            "knorr_pyrazole_competing_regioisomer_channel",
            ReactionRegioselectivityClass.COMPETING_REGIOISOMERS,
            List.of("one_three_dicarbonyl", "substituted_hydrazine", "pyrazole"),
            List.of("knorr", "pyrazole", "regioisomer"),
            0.39,
            39.0,
            0.60,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Knorr pyrazole synthesis class prior with substituted hydrazines.",
            "Substituted hydrazines can generate competing pyrazole regioisomer channels.");
    final ReactionOutcomeCalibrationObservation larockIndoleMajor =
        ReactionOutcomeCalibrationObservation.of(
            "larock_indole_heteroannulation_major_regioisomer_prior",
            List.of(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS, OrganicReactionDomain.CYCLIZATION),
            ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION,
            List.of(
                ReactionConditionFeatureKind.CATALYST_PRESENT,
                ReactionConditionFeatureKind.HEATING),
            "substituted_indole_variable_formula",
            "larock_indole_major_regioisomer_channel",
            ReactionRegioselectivityClass.STERICALLY_CONTROLLED,
            List.of("ortho_iodoaniline", "alkyne", "palladium", "indole", "heteroannulation"),
            List.of("larock", "indole", "major regioisomer", "heteroannulation"),
            0.68,
            68.0,
            0.62,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Larock indole synthesis prior from palladium-catalyzed heteroannulation of o-iodoanilines and alkynes.",
            "Unsymmetric alkynes and substituent bulk drive a dominant indole regioisomer channel.");
    final ReactionOutcomeCalibrationObservation larockIndoleMinor =
        ReactionOutcomeCalibrationObservation.of(
            "larock_indole_heteroannulation_minor_regioisomer_prior",
            List.of(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS, OrganicReactionDomain.CYCLIZATION),
            ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION,
            List.of(
                ReactionConditionFeatureKind.CATALYST_PRESENT,
                ReactionConditionFeatureKind.HEATING),
            "substituted_indole_variable_formula",
            "larock_indole_minor_regioisomer_channel",
            ReactionRegioselectivityClass.COMPETING_REGIOISOMERS,
            List.of("ortho_iodoaniline", "alkyne", "palladium", "indole", "heteroannulation"),
            List.of("larock", "indole", "minor regioisomer", "heteroannulation"),
            0.32,
            32.0,
            0.62,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Larock indole synthesis prior from palladium-catalyzed heteroannulation of o-iodoanilines and alkynes.",
            "The opposite alkyne orientation remains a real minor indole regioisomer channel for unsymmetric alkynes.");
    final ReactionOutcomeCalibrationObservation hurdMoriThiadiazole =
        ReactionOutcomeCalibrationObservation.of(
            "hurd_mori_bicyclic_thiadiazole_regioselectivity_prior",
            List.of(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS, OrganicReactionDomain.CYCLIZATION),
            ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION,
            List.of(
                ReactionConditionFeatureKind.HEATING,
                ReactionConditionFeatureKind.DEHYDRATING_AGENT),
            "bicyclic_thiadiazole_variable_formula",
            "hurd_mori_bicyclic_thiadiazole_regioisomer_channel",
            ReactionRegioselectivityClass.COMPETING_REGIOISOMERS,
            List.of(
                "acylhydrazone", "tosylhydrazone", "thionyl_chloride", "thiadiazole", "bicyclic"),
            List.of("hurd-mori", "thiadiazole", "regioselectivity", "bicyclic"),
            0.57,
            57.0,
            0.58,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Hurd-Mori 1,2,3-thiadiazole synthesis prior from reported bicyclic regioselectivity studies.",
            "Substrate topology and hydrazone substitution can split bicyclic thiadiazole regioisomer channels.");
    final ReactionOutcomeCalibrationObservation hurdMoriThiadiazoleMinor =
        ReactionOutcomeCalibrationObservation.of(
            "hurd_mori_bicyclic_thiadiazole_minor_regioisomer_prior",
            List.of(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS, OrganicReactionDomain.CYCLIZATION),
            ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION,
            List.of(
                ReactionConditionFeatureKind.HEATING,
                ReactionConditionFeatureKind.DEHYDRATING_AGENT),
            "bicyclic_thiadiazole_variable_formula",
            "hurd_mori_bicyclic_thiadiazole_minor_regioisomer_channel",
            ReactionRegioselectivityClass.COMPETING_REGIOISOMERS,
            List.of(
                "acylhydrazone", "tosylhydrazone", "thionyl_chloride", "thiadiazole", "bicyclic"),
            List.of("hurd-mori", "thiadiazole", "minor regioisomer", "bicyclic"),
            0.43,
            43.0,
            0.58,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Hurd-Mori 1,2,3-thiadiazole synthesis prior from reported bicyclic regioselectivity studies.",
            "The alternative bicyclic closure is retained as a competing thiadiazole regioisomer channel.");
    final ReactionOutcomeCalibrationObservation fischerIndoleProduct =
        ReactionOutcomeCalibrationObservation.of(
            "fischer_indole_major_rearranged_product_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
                OrganicReactionDomain.REARRANGEMENT,
                OrganicReactionDomain.CYCLIZATION),
            ReactionMechanismFamily.FISCHER_INDOLE_SYNTHESIS,
            List.of(
                ReactionConditionFeatureKind.ACIDIC_MEDIUM, ReactionConditionFeatureKind.HEATING),
            "substituted_indole_variable_formula",
            "fischer_indole_rearranged_major_channel",
            ReactionRegioselectivityClass.ELECTRONICALLY_CONTROLLED,
            List.of("arylhydrazone", "ketone", "acid", "indole", "hydrazone"),
            List.of("fischer", "indole", "major", "rearranged"),
            0.74,
            74.0,
            0.66,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Fischer indole synthesis class prior from arylhydrazone acid-promoted rearrangement/cyclization.",
            "Hydrazone electronics and acid strength usually concentrate mass in the rearranged indole product channel.");
    final ReactionOutcomeCalibrationObservation fischerIndoleSideChannel =
        ReactionOutcomeCalibrationObservation.of(
            "fischer_indole_side_rearrangement_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
                OrganicReactionDomain.REARRANGEMENT,
                OrganicReactionDomain.CYCLIZATION),
            ReactionMechanismFamily.FISCHER_INDOLE_SYNTHESIS,
            List.of(
                ReactionConditionFeatureKind.ACIDIC_MEDIUM, ReactionConditionFeatureKind.HEATING),
            "substituted_indole_variable_formula",
            "fischer_indole_side_rearrangement_channel",
            ReactionRegioselectivityClass.COMPETING_REGIOISOMERS,
            List.of("arylhydrazone", "ketone", "acid", "indole", "hydrazone"),
            List.of("fischer", "indole", "side", "regioisomer"),
            0.26,
            26.0,
            0.66,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Fischer indole synthesis class prior from arylhydrazone acid-promoted rearrangement/cyclization.",
            "Unsymmetric hydrazones and substituted ketones can retain a minor indole regioisomer or rearranged side channel.");
    final ReactionOutcomeCalibrationObservation biginelliDihydropyrimidinone =
        ReactionOutcomeCalibrationObservation.of(
            "biginelli_dihydropyrimidinone_major_channel_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
                OrganicReactionDomain.CYCLIZATION,
                OrganicReactionDomain.ADDITION),
            ReactionMechanismFamily.BIGINELLI_REACTION,
            List.of(
                ReactionConditionFeatureKind.ACIDIC_MEDIUM, ReactionConditionFeatureKind.HEATING),
            "dihydropyrimidinone_variable_formula",
            "biginelli_dhpm_major_channel",
            ReactionRegioselectivityClass.CONDITION_CONTROLLED,
            List.of(
                "aldehyde", "urea", "thiourea", "beta_ketoester", "acid", "dihydropyrimidinone"),
            List.of("biginelli", "dihydropyrimidinone", "major"),
            0.71,
            71.0,
            0.63,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Biginelli multicomponent heterocycle construction prior.",
            "Acid-promoted iminium/enol addition and cyclodehydration normally favor the DHPM product channel.");
    final ReactionOutcomeCalibrationObservation biginelliDehydratedSideChannel =
        ReactionOutcomeCalibrationObservation.of(
            "biginelli_dehydrated_or_thione_side_channel_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
                OrganicReactionDomain.CYCLIZATION,
                OrganicReactionDomain.ADDITION),
            ReactionMechanismFamily.BIGINELLI_REACTION,
            List.of(
                ReactionConditionFeatureKind.ACIDIC_MEDIUM, ReactionConditionFeatureKind.HEATING),
            "dihydropyrimidinone_variable_formula",
            "biginelli_dehydrated_or_thione_side_channel",
            ReactionRegioselectivityClass.CONDITION_CONTROLLED,
            List.of(
                "aldehyde", "urea", "thiourea", "beta_ketoester", "acid", "dihydropyrimidinone"),
            List.of("biginelli", "dehydrated", "thione", "minor"),
            0.29,
            29.0,
            0.63,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Biginelli multicomponent heterocycle construction prior.",
            "Thiourea, stronger acid or prolonged heating can divert mass into thione/dehydrated side channels.");
    final ReactionOutcomeCalibrationObservation pictetSpenglerCationicCyclization =
        ReactionOutcomeCalibrationObservation.of(
            "pictet_spengler_tetrahydro_beta_carboline_major_prior",
            List.of(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS, OrganicReactionDomain.CYCLIZATION),
            ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION,
            List.of(
                ReactionConditionFeatureKind.ACIDIC_MEDIUM, ReactionConditionFeatureKind.COOLING),
            "tetrahydro_beta_carboline_variable_formula",
            "pictet_spengler_cationic_cyclization_major_channel",
            ReactionRegioselectivityClass.STERICALLY_CONTROLLED,
            List.of("tryptamine", "aldehyde", "imine", "acid", "beta_carboline"),
            List.of("pictet-spengler", "tetrahydro-beta-carboline", "major", "diastereomer"),
            0.67,
            67.0,
            0.62,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Pictet-Spengler heterocycle construction prior from iminium cyclization of tryptamine-like substrates.",
            "Imine geometry, acid and substrate conformation drive a major tetrahydro-beta-carboline stereochemical channel.");
    final ReactionOutcomeCalibrationObservation pictetSpenglerDiastereomer =
        ReactionOutcomeCalibrationObservation.of(
            "pictet_spengler_tetrahydro_beta_carboline_minor_diastereomer_prior",
            List.of(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS, OrganicReactionDomain.CYCLIZATION),
            ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION,
            List.of(
                ReactionConditionFeatureKind.ACIDIC_MEDIUM, ReactionConditionFeatureKind.COOLING),
            "tetrahydro_beta_carboline_variable_formula",
            "pictet_spengler_minor_diastereomer_channel",
            ReactionRegioselectivityClass.STERICALLY_CONTROLLED,
            List.of("tryptamine", "aldehyde", "imine", "acid", "beta_carboline"),
            List.of("pictet-spengler", "tetrahydro-beta-carboline", "minor", "diastereomer"),
            0.33,
            33.0,
            0.62,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Pictet-Spengler heterocycle construction prior from iminium cyclization of tryptamine-like substrates.",
            "The opposite iminium face/conformation is kept as a competing diastereomeric channel.");
    final ReactionOutcomeCalibrationObservation friedlaenderQuinolineMajor =
        ReactionOutcomeCalibrationObservation.of(
            "friedlaender_quinoline_major_condensation_prior",
            List.of(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS, OrganicReactionDomain.CYCLIZATION),
            ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION,
            List.of(
                ReactionConditionFeatureKind.BASIC_MEDIUM, ReactionConditionFeatureKind.HEATING),
            "substituted_quinoline_variable_formula",
            "friedlaender_quinoline_major_channel",
            ReactionRegioselectivityClass.ELECTRONICALLY_CONTROLLED,
            List.of("ortho_aminobenzaldehyde", "ketone", "quinoline", "condensation"),
            List.of("friedlaender", "quinoline", "major", "condensation"),
            0.69,
            69.0,
            0.60,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Friedlaender quinoline construction class prior from o-aminocarbonyl condensation product distributions.",
            "Unsymmetric ketones and aldehyde/ketone electronics can split quinoline regioisomer channels.");
    final ReactionOutcomeCalibrationObservation friedlaenderQuinolineMinor =
        ReactionOutcomeCalibrationObservation.of(
            "friedlaender_quinoline_minor_regioisomer_prior",
            List.of(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS, OrganicReactionDomain.CYCLIZATION),
            ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION,
            List.of(
                ReactionConditionFeatureKind.BASIC_MEDIUM, ReactionConditionFeatureKind.HEATING),
            "substituted_quinoline_variable_formula",
            "friedlaender_quinoline_minor_regioisomer_channel",
            ReactionRegioselectivityClass.COMPETING_REGIOISOMERS,
            List.of("ortho_aminobenzaldehyde", "ketone", "quinoline", "condensation"),
            List.of("friedlaender", "quinoline", "minor", "regioisomer"),
            0.31,
            31.0,
            0.60,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Friedlaender quinoline construction class prior from o-aminocarbonyl condensation product distributions.",
            "Alternative enolization or carbonyl orientation remains a real minor quinoline regioisomer channel.");
    final ReactionOutcomeCalibrationObservation chichibabinAminationC2 =
        ReactionOutcomeCalibrationObservation.of(
            "chichibabin_pyridine_c2_amination_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION,
            List.of(
                ReactionConditionFeatureKind.BASIC_MEDIUM, ReactionConditionFeatureKind.HEATING),
            "aminopyridine_variable_formula",
            "chichibabin_c2_amination_major_channel",
            ReactionRegioselectivityClass.ELECTRONICALLY_CONTROLLED,
            List.of("pyridine", "sodium_amide", "electron_deficient_heteroarene", "amination"),
            List.of("chichibabin", "c2", "aminopyridine", "major"),
            0.74,
            74.0,
            0.58,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Chichibabin-type pyridine amination class prior from heteroarene amination product distributions.",
            "Electron-poor pyridines usually favor alpha/C2 amination, unless substituent blocking redirects attack.");
    final ReactionOutcomeCalibrationObservation chichibabinAminationC4 =
        ReactionOutcomeCalibrationObservation.of(
            "chichibabin_pyridine_c4_amination_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION,
            List.of(
                ReactionConditionFeatureKind.BASIC_MEDIUM, ReactionConditionFeatureKind.HEATING),
            "aminopyridine_variable_formula",
            "chichibabin_c4_amination_minor_or_redirected_channel",
            ReactionRegioselectivityClass.STERICALLY_CONTROLLED,
            List.of("pyridine", "sodium_amide", "electron_deficient_heteroarene", "amination"),
            List.of("chichibabin", "c4", "aminopyridine", "redirected"),
            0.26,
            26.0,
            0.58,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Chichibabin-type pyridine amination class prior from heteroarene amination product distributions.",
            "C4 amination is kept as a competing or substituent-redirected channel for blocked pyridines.");
    final ReactionOutcomeCalibrationObservation gewaldThiopheneMajor =
        ReactionOutcomeCalibrationObservation.of(
            "gewald_aminothiophene_major_channel_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
                OrganicReactionDomain.CYCLIZATION,
                OrganicReactionDomain.ADDITION),
            ReactionMechanismFamily.MULTICOMPONENT_CONDENSATION,
            List.of(
                ReactionConditionFeatureKind.BASIC_MEDIUM, ReactionConditionFeatureKind.HEATING),
            "aminothiophene_variable_formula",
            "gewald_aminothiophene_major_channel",
            ReactionRegioselectivityClass.CONDITION_CONTROLLED,
            List.of("ketone", "nitrile", "sulfur", "aminothiophene", "gewald"),
            List.of("gewald", "aminothiophene", "major", "cyclization"),
            0.76,
            76.0,
            0.59,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Gewald aminothiophene construction class prior from carbonyl, activated nitrile and sulfur condensations.",
            "Base, nitrile acidity and carbonyl substitution normally concentrate mass in the aminothiophene channel.");
    final ReactionOutcomeCalibrationObservation gewaldSideChannel =
        ReactionOutcomeCalibrationObservation.of(
            "gewald_side_condensation_or_oligomer_channel_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
                OrganicReactionDomain.CYCLIZATION,
                OrganicReactionDomain.ADDITION),
            ReactionMechanismFamily.MULTICOMPONENT_CONDENSATION,
            List.of(
                ReactionConditionFeatureKind.BASIC_MEDIUM, ReactionConditionFeatureKind.HEATING),
            "aminothiophene_variable_formula",
            "gewald_side_condensation_or_oligomer_channel",
            ReactionRegioselectivityClass.CONDITION_CONTROLLED,
            List.of("ketone", "nitrile", "sulfur", "aminothiophene", "gewald"),
            List.of("gewald", "side", "condensation", "oligomer"),
            0.24,
            24.0,
            0.59,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Gewald aminothiophene construction class prior from carbonyl, activated nitrile and sulfur condensations.",
            "Strong base, excess sulfur or prolonged heating can divert material into side-condensation channels.");
    final ReactionOutcomeCalibrationObservation indoleC3Electrophilic =
        ReactionOutcomeCalibrationObservation.of(
            "indole_c3_electrophilic_substitution_major_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.ELECTROPHILIC_SUBSTITUTION),
            ReactionMechanismFamily.ELECTROPHILIC_AROMATIC_SUBSTITUTION,
            List.of(ReactionConditionFeatureKind.ACIDIC_MEDIUM),
            "functionalized_indole_variable_formula",
            "indole_c3_electrophilic_major_channel",
            ReactionRegioselectivityClass.ELECTRONICALLY_CONTROLLED,
            List.of("indole", "electron_rich_heteroarene", "electrophile"),
            List.of("indole", "c3", "electrophilic", "major"),
            0.82,
            82.0,
            0.64,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Indole electrophilic substitution class prior from heteroarene functionalization distributions.",
            "C3 attack is treated as the dominant retained-aromatic channel for free indoles.");
    final ReactionOutcomeCalibrationObservation indoleNFunctionalization =
        ReactionOutcomeCalibrationObservation.of(
            "indole_n_functionalization_competing_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            ReactionMechanismFamily.NUCLEOPHILIC_SUBSTITUTION_SN2,
            List.of(ReactionConditionFeatureKind.BASIC_MEDIUM),
            "functionalized_indole_variable_formula",
            "indole_n_functionalization_competing_channel",
            ReactionRegioselectivityClass.CONDITION_CONTROLLED,
            List.of("indole", "base", "alkylating_agent", "n_h_heterocycle"),
            List.of("indole", "n", "alkylation", "competing"),
            0.18,
            18.0,
            0.64,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Indole N-functionalization class prior from base-promoted heteroatom alkylation studies.",
            "Base and strong alkylating agents can move product mass from C3 substitution toward N-functionalization.");
    final ReactionOutcomeCalibrationObservation diazineSnarMajor =
        ReactionOutcomeCalibrationObservation.of(
            "diazine_triazine_snar_displacement_major_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION,
            List.of(ReactionConditionFeatureKind.BASIC_MEDIUM),
            "substituted_diazine_or_triazine_variable_formula",
            "diazine_triazine_snar_displacement_major_channel",
            ReactionRegioselectivityClass.ELECTRONICALLY_CONTROLLED,
            List.of("diazine", "triazine", "chloro_heteroarene", "nucleophile"),
            List.of("snar", "displacement", "diazine", "triazine", "major"),
            0.72,
            72.0,
            0.63,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Diazine and triazine SNAr class prior from electron-deficient chloro-heteroarene substitution reports.",
            "Electron-deficient aza rings and activated chlorides are treated as high-probability substitution channels.");
    final ReactionOutcomeCalibrationObservation diazineHydrolysisSide =
        ReactionOutcomeCalibrationObservation.of(
            "diazine_triazine_hydrolysis_or_double_substitution_side_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            ReactionMechanismFamily.HYDROLYSIS,
            List.of(
                ReactionConditionFeatureKind.BASIC_MEDIUM, ReactionConditionFeatureKind.HEATING),
            "substituted_diazine_or_triazine_variable_formula",
            "diazine_triazine_hydrolysis_or_double_substitution_side_channel",
            ReactionRegioselectivityClass.CONDITION_CONTROLLED,
            List.of("diazine", "triazine", "chloro_heteroarene", "water", "base"),
            List.of("hydrolysis", "double", "side", "triazine", "diazine"),
            0.28,
            28.0,
            0.63,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Diazine and triazine SNAr side-channel prior from aqueous/basic substitution reports.",
            "Water, excess nucleophile and heating can create hydrolysis or multiple-substitution side channels.");
    final ReactionOutcomeCalibrationObservation quinazolinoneCyclization =
        ReactionOutcomeCalibrationObservation.of(
            "anthranilamide_quinazolinone_cyclization_major_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
                OrganicReactionDomain.CYCLIZATION,
                OrganicReactionDomain.ADDITION),
            ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION,
            List.of(
                ReactionConditionFeatureKind.ACIDIC_MEDIUM, ReactionConditionFeatureKind.HEATING),
            "quinazolinone_variable_formula",
            "quinazolinone_cyclodehydration_major_channel",
            ReactionRegioselectivityClass.CONDITION_CONTROLLED,
            List.of("anthranilamide", "aldehyde", "carbonyl", "quinazolinone"),
            List.of("quinazolinone", "cyclization", "cyclodehydration", "major"),
            0.78,
            78.0,
            0.62,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Quinazolinone construction class prior from anthranilamide and carbonyl condensation studies.",
            "Acid and heat normally favor imine formation followed by intramolecular cyclodehydration.");
    final ReactionOutcomeCalibrationObservation quinazolineOxidizedSide =
        ReactionOutcomeCalibrationObservation.of(
            "anthranilamide_quinazoline_oxidized_side_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_SYNTHESIS,
                OrganicReactionDomain.OXIDATION_REDUCTION),
            ReactionMechanismFamily.OXIDATION,
            List.of(
                ReactionConditionFeatureKind.CATALYST_PRESENT,
                ReactionConditionFeatureKind.HEATING),
            "quinazoline_variable_formula",
            "quinazoline_oxidized_or_dehydrogenated_side_channel",
            ReactionRegioselectivityClass.CONDITION_CONTROLLED,
            List.of("anthranilamide", "aldehyde", "oxidant", "quinazoline"),
            List.of("quinazoline", "oxidized", "dehydrogenated", "side"),
            0.22,
            22.0,
            0.62,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Quinazoline oxidation-side prior from oxidative heterocycle construction reports.",
            "Oxidant and prolonged heating are kept as a competing aromatization/dehydrogenation channel.");
    final ReactionOutcomeCalibrationObservation hetaryneIpso =
        ReactionOutcomeCalibrationObservation.of(
            "hetaryne_ipso_substitution_major_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            ReactionMechanismFamily.ARYNE_SUBSTITUTION,
            List.of(ReactionConditionFeatureKind.BASIC_MEDIUM),
            "hetaryne_substitution_variable_formula",
            "hetaryne_ipso_or_adjacent_substitution_major_channel",
            ReactionRegioselectivityClass.ELECTRONICALLY_CONTROLLED,
            List.of("hetaryne", "fluoro_heteroarene", "strong_base", "nucleophile"),
            List.of("hetaryne", "ipso", "adjacent", "major"),
            0.58,
            58.0,
            0.56,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Hetaryne substitution class prior from regioisomeric aryne/hetaryne trapping studies.",
            "Distortion, heteroatom polarization and nucleophile identity split product mass across adjacent sites.");
    final ReactionOutcomeCalibrationObservation hetaryneAlternate =
        ReactionOutcomeCalibrationObservation.of(
            "hetaryne_alternate_regioisomer_competing_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            ReactionMechanismFamily.ARYNE_SUBSTITUTION,
            List.of(ReactionConditionFeatureKind.BASIC_MEDIUM),
            "hetaryne_substitution_variable_formula",
            "hetaryne_alternate_regioisomer_competing_channel",
            ReactionRegioselectivityClass.ELECTRONICALLY_CONTROLLED,
            List.of("hetaryne", "fluoro_heteroarene", "strong_base", "nucleophile"),
            List.of("hetaryne", "alternate", "regioisomer", "competing"),
            0.42,
            42.0,
            0.56,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Hetaryne substitution class prior from regioisomeric aryne/hetaryne trapping studies.",
            "The alternate regioisomer remains significant when electronic distortion is weak or sterics reverse attack.");
    final ReactionOutcomeCalibrationObservation heteroarylChActivationAnnulation =
        ReactionOutcomeCalibrationObservation.of(
            "heteroaryl_ch_activation_annulation_major_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.CROSS_COUPLING,
                OrganicReactionDomain.CYCLIZATION),
            ReactionMechanismFamily.C_H_ACTIVATION,
            List.of(
                ReactionConditionFeatureKind.CATALYST_PRESENT,
                ReactionConditionFeatureKind.HEATING),
            "annulated_heteroarene_variable_formula",
            "directed_c_h_annulation_major_channel",
            ReactionRegioselectivityClass.HETEROATOM_DIRECTED,
            List.of(
                "pyridine",
                "quinoline",
                "heteroaryl_c_h",
                "directing_group",
                "rhodium",
                "palladium"),
            List.of("c-h activation", "annulation", "directed", "major", "heteroarene"),
            0.67,
            67.0,
            0.61,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Heteroaryl directed C-H activation and annulation class prior from product distribution studies.",
            "Metal catalyst, directing group and heteroatom coordination usually create a dominant annulated product channel.");
    final ReactionOutcomeCalibrationObservation heteroarylProtodeMetalationSide =
        ReactionOutcomeCalibrationObservation.of(
            "heteroaryl_c_h_activation_protodemetalation_side_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.CROSS_COUPLING),
            ReactionMechanismFamily.C_H_ACTIVATION,
            List.of(
                ReactionConditionFeatureKind.CATALYST_PRESENT,
                ReactionConditionFeatureKind.HEATING),
            "heteroaryl_side_product_variable_formula",
            "protodemetalation_or_unfunctionalized_side_channel",
            ReactionRegioselectivityClass.CONDITION_CONTROLLED,
            List.of(
                "pyridine", "quinoline", "heteroaryl_c_h", "directing_group", "acidic_additive"),
            List.of("c-h activation", "protodemetalation", "side", "unfunctionalized"),
            0.33,
            33.0,
            0.61,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Heteroaryl C-H activation class prior retaining protodemetalation and unfunctionalized recovery channels.",
            "Acidic additives, strong heteroatom binding or poor ligand match can divert mass away from annulation.");
    final ReactionOutcomeCalibrationObservation buchwaldHeteroarylAmination =
        ReactionOutcomeCalibrationObservation.of(
            "buchwald_hartwig_heteroaryl_amination_major_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.CROSS_COUPLING,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            ReactionMechanismFamily.BUCHWALD_HARTWIG_AMINATION,
            List.of(
                ReactionConditionFeatureKind.CATALYST_PRESENT,
                ReactionConditionFeatureKind.BASIC_MEDIUM,
                ReactionConditionFeatureKind.HEATING),
            "heteroaryl_amine_variable_formula",
            "buchwald_heteroaryl_amination_major_channel",
            ReactionRegioselectivityClass.LEAVING_GROUP_DIRECTED,
            List.of("heteroaryl_halide", "amine", "palladium", "ligand", "base"),
            List.of("buchwald", "amination", "heteroaryl", "major", "amine"),
            0.70,
            70.0,
            0.64,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Buchwald-Hartwig heteroaryl amination class prior from heteroaryl halide/amine coupling reports.",
            "Ligand/base matched heteroaryl halides usually favor C-N coupled product over direct hydrodehalogenation.");
    final ReactionOutcomeCalibrationObservation buchwaldHydrodehalogenation =
        ReactionOutcomeCalibrationObservation.of(
            "buchwald_hartwig_heteroaryl_hydrodehalogenation_side_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.CROSS_COUPLING),
            ReactionMechanismFamily.BUCHWALD_HARTWIG_AMINATION,
            List.of(
                ReactionConditionFeatureKind.CATALYST_PRESENT,
                ReactionConditionFeatureKind.BASIC_MEDIUM,
                ReactionConditionFeatureKind.HEATING),
            "dehalogenated_heteroarene_variable_formula",
            "hydrodehalogenation_or_dehalogenated_side_channel",
            ReactionRegioselectivityClass.CONDITION_CONTROLLED,
            List.of("heteroaryl_halide", "amine", "palladium", "base", "sensitive_heteroarene"),
            List.of("buchwald", "hydrodehalogenation", "side", "dehalogenated"),
            0.30,
            30.0,
            0.64,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Buchwald-Hartwig heteroaryl amination side-channel prior.",
            "Ligand mismatch, strong heteroatom coordination or reducing conditions retain dehalogenation as a competing channel.");
    final ReactionOutcomeCalibrationObservation suzukiHeterobiaryl =
        ReactionOutcomeCalibrationObservation.of(
            "suzuki_heterobiaryl_coupling_major_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.CROSS_COUPLING),
            ReactionMechanismFamily.SUZUKI_COUPLING,
            List.of(
                ReactionConditionFeatureKind.CATALYST_PRESENT,
                ReactionConditionFeatureKind.BASIC_MEDIUM,
                ReactionConditionFeatureKind.HEATING),
            "heterobiaryl_variable_formula",
            "suzuki_heterobiaryl_major_channel",
            ReactionRegioselectivityClass.LEAVING_GROUP_DIRECTED,
            List.of("heteroaryl_halide", "boronic_acid", "boronate", "palladium", "base"),
            List.of("suzuki", "heterobiaryl", "major", "cross-coupling"),
            0.73,
            73.0,
            0.65,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Suzuki heterobiaryl coupling class prior from heteroaryl halide/boron reagent coupling distributions.",
            "Activated leaving group and boron partner normally favor heterobiaryl formation under palladium/base conditions.");
    final ReactionOutcomeCalibrationObservation suzukiProtodeboronation =
        ReactionOutcomeCalibrationObservation.of(
            "suzuki_heteroaryl_protodeboronation_side_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.CROSS_COUPLING),
            ReactionMechanismFamily.SUZUKI_COUPLING,
            List.of(
                ReactionConditionFeatureKind.CATALYST_PRESENT,
                ReactionConditionFeatureKind.BASIC_MEDIUM,
                ReactionConditionFeatureKind.HEATING),
            "protodeboronated_or_hydrolyzed_variable_formula",
            "protodeboronation_hydrolysis_side_channel",
            ReactionRegioselectivityClass.CONDITION_CONTROLLED,
            List.of("heteroaryl_halide", "boronic_acid", "water", "base", "sensitive_boronate"),
            List.of("suzuki", "protodeboronation", "hydrolysis", "side"),
            0.27,
            27.0,
            0.65,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Suzuki heteroaryl side-channel prior from protodeboronation and hydrolysis-prone boron reagents.",
            "Basic aqueous conditions and unstable boronic acids preserve protodeboronation/hydrolysis as product channels.");
    final ReactionOutcomeCalibrationObservation heteroareneNOxideFunctionalization =
        ReactionOutcomeCalibrationObservation.of(
            "heteroarene_n_oxide_c2_functionalization_major_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.OXIDATION_REDUCTION),
            ReactionMechanismFamily.OXIDATION,
            List.of(
                ReactionConditionFeatureKind.CATALYST_PRESENT,
                ReactionConditionFeatureKind.HEATING),
            "functionalized_heteroarene_n_oxide_variable_formula",
            "n_oxide_directed_c2_functionalization_major_channel",
            ReactionRegioselectivityClass.HETEROATOM_DIRECTED,
            List.of(
                "pyridine_n_oxide", "quinoline_n_oxide", "heteroarene_n_oxide", "c2", "directed"),
            List.of("n-oxide", "c2", "functionalization", "major", "heteroarene"),
            0.69,
            69.0,
            0.62,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Heteroarene N-oxide directed functionalization class prior.",
            "N-oxide activation often directs functionalization next to nitrogen and then requires reduction/deoxygenation planning.");
    final ReactionOutcomeCalibrationObservation heteroareneNOxideDeoxygenation =
        ReactionOutcomeCalibrationObservation.of(
            "heteroarene_n_oxide_deoxygenation_side_prior",
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.OXIDATION_REDUCTION),
            ReactionMechanismFamily.REDUCTION,
            List.of(
                ReactionConditionFeatureKind.CATALYST_PRESENT,
                ReactionConditionFeatureKind.HEATING),
            "deoxygenated_heteroarene_variable_formula",
            "n_oxide_deoxygenation_or_recovered_heteroarene_side_channel",
            ReactionRegioselectivityClass.CONDITION_CONTROLLED,
            List.of("pyridine_n_oxide", "quinoline_n_oxide", "reducing_agent", "deoxygenation"),
            List.of("n-oxide", "deoxygenation", "side", "recovered heteroarene"),
            0.31,
            31.0,
            0.62,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "Heteroarene N-oxide deoxygenation side-channel prior.",
            "Reductive workup or over-reduction can convert activated N-oxides back to parent heteroarenes.");
    return ReactionOutcomeCalibrationLibrary.of(
        List.of(heteroarylChlorideBenzylamine),
        BuiltInReactionOutcomeCalibrationLibraries.withBroadTaxonomyPriors(
            List.of(
                ReactionOutcomeDistributionDataset.of(
                    "activated_aza_heterocycle_substitution_distribution_priors",
                    "Competing direct SNAr and ANRORC channels in activated aza-heterocycle substitution",
                    "Curated literature-class prior from activated aza-heterocycle substitution mechanisms.",
                    ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
                    List.of(anrorcAzaHeterocycle)),
                ReactionOutcomeDistributionDataset.of(
                    "pyridinium_radical_regioselectivity_distribution_priors",
                    "Competing Minisci and directed pyridinium radical product channels",
                    "Curated literature-class prior from Minisci and pyridinium regioselectivity reports.",
                    ReactionOutcomeSourceQuality.EXPERIMENTAL_YIELD_TABLE,
                    List.of(minisciMixture, pyridiniumC4)),
                ReactionOutcomeDistributionDataset.of(
                    "pyridinium_nucleophilic_addition_regioselectivity_priors",
                    "N-alkyl pyridinium 1,2/1,4 nucleophilic addition channel priors",
                    "Curated literature-class prior from predictive pyridinium addition and regioselectivity studies.",
                    ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
                    List.of(pyridiniumOneFourAddition, pyridiniumOneTwoAddition)),
                ReactionOutcomeDistributionDataset.of(
                    "pyridinium_amine_ring_opening_distribution_priors",
                    "Pyridinium amine addition, ring-opening and retained-ring substitution channel priors",
                    "Curated literature-class prior from pyridinium amine/Zincke-type reactivity reports.",
                    ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
                    List.of(pyridiniumAmineRingOpening, pyridiniumAmineSubstitution)),
                ReactionOutcomeDistributionDataset.of(
                    "photochemical_sulfur_heterocycle_population_priors",
                    "Photochemical sulfur-heterocycle population channels",
                    "Curated time-resolved population prior from photochemical heterocycle rearrangement studies.",
                    ReactionOutcomeSourceQuality.TIME_RESOLVED_POPULATION_STUDY,
                    List.of(photochemicalPopulation)),
                ReactionOutcomeDistributionDataset.of(
                    "dipolar_heterocycle_construction_distribution_priors",
                    "1,3-dipolar heterocycle construction product-channel priors",
                    "Curated literature-class prior from azide/nitrile-oxide cycloaddition product distributions.",
                    ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
                    List.of(dipolarCycloaddition)),
                ReactionOutcomeDistributionDataset.of(
                    "hantzsch_pyridine_construction_distribution_priors",
                    "Hantzsch-like pyridine construction product-channel priors",
                    "Curated literature-class prior from Hantzsch multicomponent condensation product distributions.",
                    ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
                    List.of(hantzschPyridine)),
                ReactionOutcomeDistributionDataset.of(
                    "krohnke_pyridine_construction_distribution_priors",
                    "Krohnke-like pyridine construction product-channel priors",
                    "Curated literature-class prior from Krohnke-type pyridine construction product distributions.",
                    ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
                    List.of(krohnkePyridine)),
                ReactionOutcomeDistributionDataset.of(
                    "n_heteroarene_dearomatization_distribution_priors",
                    "Nitrogen heteroarene dearomatization and annulation channel priors",
                    "Curated literature-class prior from catalytic dearomatization/annulation studies.",
                    ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
                    List.of(dearomatizationAnnulation)),
                ReactionOutcomeDistributionDataset.of(
                    "modified_combes_quinoline_regioisomer_distribution_priors",
                    "Modified Combes quinoline regioisomer channel priors",
                    "Curated literature-class prior from substituent-controlled trifluoromethyl quinoline formation.",
                    ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
                    List.of(combesTwoCf3Quinoline, combesFourCf3Quinoline)),
                ReactionOutcomeDistributionDataset.of(
                    "sloop_modified_combes_table1_entry1_exact_benchmark",
                    "Sloop modified Combes Table 1 entry 1 exact quinoline regioisomer benchmark",
                    "Exact 2a/3a product distribution from Table 1 entry 1, used only for validation.",
                    ReactionOutcomeSourceQuality.EXACT_EXPERIMENTAL_PRODUCT_DISTRIBUTION,
                    List.of(combesTableOneEntryOneTwoCf3, combesTableOneEntryOneFourCf3)),
                ReactionOutcomeDistributionDataset.of(
                    "sloop_modified_combes_table1_entry3_exact_benchmark",
                    "Sloop modified Combes Table 1 entry 3 exact quinoline regioisomer benchmark",
                    "Exact 2c/3c product distribution from Table 1 entry 3, used only for validation.",
                    ReactionOutcomeSourceQuality.EXACT_EXPERIMENTAL_PRODUCT_DISTRIBUTION,
                    List.of(combesTableOneEntryThreeTwoCf3, combesTableOneEntryThreeFourCf3)),
                ReactionOutcomeDistributionDataset.of(
                    "soai_pyrimidyl_alcohol_enantiomer_distribution_priors",
                    "Soai-type pyrimidyl alcohol enantiomer distribution priors",
                    "Curated literature-class prior from asymmetric autocatalysis of pyrimidine carbaldehydes.",
                    ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
                    List.of(soaiAmplifiedMajorEnantiomer, soaiMinorEnantiomer)),
                ReactionOutcomeDistributionDataset.of(
                    "paal_knorr_knorr_heterocycle_distribution_priors",
                    "Paal-Knorr and Knorr heterocycle construction product-channel priors",
                    "Curated literature-class prior from pyrrole and pyrazole construction product distributions.",
                    ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
                    List.of(paalKnorrPyrrole, knorrPyrazoleRegioisomer)),
                ReactionOutcomeDistributionDataset.of(
                    "larock_indole_heteroannulation_distribution_priors",
                    "Larock indole heteroannulation regioisomer channel priors",
                    "Curated literature-class prior from palladium-catalyzed indole heteroannulation product distributions.",
                    ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
                    List.of(larockIndoleMajor, larockIndoleMinor)),
                ReactionOutcomeDistributionDataset.of(
                    "hurd_mori_thiadiazole_regioisomer_distribution_priors",
                    "Hurd-Mori bicyclic thiadiazole regioisomer channel priors",
                    "Curated literature-class prior from bicyclic 1,2,3-thiadiazole regioselectivity studies.",
                    ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
                    List.of(hurdMoriThiadiazole, hurdMoriThiadiazoleMinor)),
                ReactionOutcomeDistributionDataset.of(
                    "fischer_indole_rearrangement_distribution_priors",
                    "Fischer indole synthesis product-channel priors",
                    "Curated literature-class prior from arylhydrazone acid-promoted rearrangement and cyclization.",
                    ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
                    List.of(fischerIndoleProduct, fischerIndoleSideChannel)),
                ReactionOutcomeDistributionDataset.of(
                    "biginelli_dihydropyrimidinone_distribution_priors",
                    "Biginelli heterocycle construction product-channel priors",
                    "Curated literature-class prior from acid-promoted aldehyde, beta-ketoester and urea/thiourea condensations.",
                    ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
                    List.of(biginelliDihydropyrimidinone, biginelliDehydratedSideChannel)),
                ReactionOutcomeDistributionDataset.of(
                    "pictet_spengler_beta_carboline_distribution_priors",
                    "Pictet-Spengler tetrahydro-beta-carboline stereochemical channel priors",
                    "Curated literature-class prior from iminium cyclization of tryptamine-like heteroaryl substrates.",
                    ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
                    List.of(pictetSpenglerCationicCyclization, pictetSpenglerDiastereomer)),
                ReactionOutcomeDistributionDataset.of(
                    "friedlaender_quinoline_regioisomer_distribution_priors",
                    "Friedlaender quinoline condensation regioisomer channel priors",
                    "Curated literature-class prior from o-aminocarbonyl quinoline construction product distributions.",
                    ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
                    List.of(friedlaenderQuinolineMajor, friedlaenderQuinolineMinor)),
                ReactionOutcomeDistributionDataset.of(
                    "chichibabin_pyridine_amination_distribution_priors",
                    "Chichibabin pyridine amination regioisomer channel priors",
                    "Curated literature-class prior from electron-deficient pyridine amination product distributions.",
                    ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
                    List.of(chichibabinAminationC2, chichibabinAminationC4)),
                ReactionOutcomeDistributionDataset.of(
                    "gewald_aminothiophene_distribution_priors",
                    "Gewald aminothiophene construction product-channel priors",
                    "Curated literature-class prior from carbonyl, activated nitrile and sulfur multicomponent condensations.",
                    ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
                    List.of(gewaldThiopheneMajor, gewaldSideChannel)),
                ReactionOutcomeDistributionDataset.of(
                    "indole_c3_n_functionalization_distribution_priors",
                    "Indole C3 electrophilic substitution and N-functionalization channel priors",
                    "Curated literature-class prior from indole regioselectivity and alkylation product distributions.",
                    ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
                    List.of(indoleC3Electrophilic, indoleNFunctionalization)),
                ReactionOutcomeDistributionDataset.of(
                    "diazine_triazine_snar_side_channel_distribution_priors",
                    "Diazine and triazine SNAr hydrolysis and multiple-substitution channel priors",
                    "Curated literature-class prior from electron-deficient diazine/triazine substitution distributions.",
                    ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
                    List.of(diazineSnarMajor, diazineHydrolysisSide)),
                ReactionOutcomeDistributionDataset.of(
                    "quinazolinone_quinazoline_construction_distribution_priors",
                    "Quinazolinone and oxidized quinazoline construction channel priors",
                    "Curated literature-class prior from anthranilamide/carbonyl heterocycle construction studies.",
                    ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
                    List.of(quinazolinoneCyclization, quinazolineOxidizedSide)),
                ReactionOutcomeDistributionDataset.of(
                    "hetaryne_regioisomer_distribution_priors",
                    "Hetaryne substitution regioisomer channel priors",
                    "Curated literature-class prior from aryne and hetaryne substitution regioselectivity studies.",
                    ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
                    List.of(hetaryneIpso, hetaryneAlternate)),
                ReactionOutcomeDistributionDataset.of(
                    "heteroaryl_c_h_activation_annulation_distribution_priors",
                    "Heteroaryl directed C-H activation annulation and side-channel priors",
                    "Curated literature-class prior from directed heteroaryl C-H activation and annulation product distributions.",
                    ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
                    List.of(heteroarylChActivationAnnulation, heteroarylProtodeMetalationSide)),
                ReactionOutcomeDistributionDataset.of(
                    "buchwald_hartwig_heteroaryl_amination_distribution_priors",
                    "Buchwald-Hartwig heteroaryl amination and dehalogenation channel priors",
                    "Curated literature-class prior from heteroaryl halide amination and side-product distributions.",
                    ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
                    List.of(buchwaldHeteroarylAmination, buchwaldHydrodehalogenation)),
                ReactionOutcomeDistributionDataset.of(
                    "suzuki_heterobiaryl_coupling_distribution_priors",
                    "Suzuki heterobiaryl coupling and boron-reagent side-channel priors",
                    "Curated literature-class prior from heteroaryl Suzuki coupling product distributions.",
                    ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
                    List.of(suzukiHeterobiaryl, suzukiProtodeboronation)),
                ReactionOutcomeDistributionDataset.of(
                    "heteroarene_n_oxide_functionalization_distribution_priors",
                    "Heteroarene N-oxide directed functionalization and deoxygenation priors",
                    "Curated literature-class prior from pyridine/quinoline N-oxide functionalization distributions.",
                    ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
                    List.of(heteroareneNOxideFunctionalization, heteroareneNOxideDeoxygenation)))));
  }

  private static List<ReactionOutcomeDistributionDataset> withBroadTaxonomyPriors(
      final List<ReactionOutcomeDistributionDataset> curatedDatasets) {
    final ArrayList<ReactionOutcomeDistributionDataset> datasets =
        new ArrayList<ReactionOutcomeDistributionDataset>(curatedDatasets);
    datasets.addAll(BuiltInReactionOutcomeCalibrationLibraries.broadDomainPriorDatasets());
    datasets.addAll(BuiltInReactionOutcomeCalibrationLibraries.broadMechanismPriorDatasets());
    return List.copyOf(datasets);
  }

  private static List<ReactionOutcomeDistributionDataset> broadDomainPriorDatasets() {
    final ArrayList<ReactionOutcomeDistributionDataset> datasets =
        new ArrayList<ReactionOutcomeDistributionDataset>();
    final OrganicReactionDomain[] domains = OrganicReactionDomain.values();
    for (int i = 0; i < domains.length; ++i) {
      final OrganicReactionDomain domain = domains[i];
      if (domain == OrganicReactionDomain.UNKNOWN) {
        continue;
      }
      final ReactionOutcomeCalibrationObservation observation =
          ReactionOutcomeCalibrationObservation.of(
              "broad_domain_prior_" + BuiltInReactionOutcomeCalibrationLibraries.key(domain.name()),
              List.of(domain),
              BuiltInReactionOutcomeCalibrationLibraries.defaultFamilyForDomain(domain),
              List.of(),
              "domain_variable_formula",
              "broad_" + BuiltInReactionOutcomeCalibrationLibraries.key(domain.name()) + "_channel",
              ReactionRegioselectivityClass.UNKNOWN,
              List.of(BuiltInReactionOutcomeCalibrationLibraries.key(domain.name())),
              List.of("broad-domain-prior"),
              0.50,
              50.0,
              0.25,
              ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
              "internal:organic-taxonomy-domain-prior",
              "broad-domain-taxonomy-prior",
              false,
              "Internal broad organic taxonomy prior.",
              "Weak domain prior used only when exact or curated product distributions are absent.");
      datasets.add(
          ReactionOutcomeDistributionDataset.of(
              "broad_domain_dataset_"
                  + BuiltInReactionOutcomeCalibrationLibraries.key(domain.name()),
              "Broad domain prior for " + domain.name(),
              "Internal non-exact taxonomy prior for organic reaction domain coverage.",
              ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
              List.of(observation)));
    }
    return List.copyOf(datasets);
  }

  private static List<ReactionOutcomeDistributionDataset> broadMechanismPriorDatasets() {
    final ArrayList<ReactionOutcomeDistributionDataset> datasets =
        new ArrayList<ReactionOutcomeDistributionDataset>();
    final ReactionMechanismFamily[] families = ReactionMechanismFamily.values();
    for (int i = 0; i < families.length; ++i) {
      final ReactionMechanismFamily family = families[i];
      if (family == ReactionMechanismFamily.UNKNOWN) {
        continue;
      }
      final OrganicReactionDomain domain =
          BuiltInReactionOutcomeCalibrationLibraries.defaultDomainForFamily(family);
      final ReactionOutcomeCalibrationObservation observation =
          ReactionOutcomeCalibrationObservation.of(
              "broad_family_prior_" + BuiltInReactionOutcomeCalibrationLibraries.key(family.name()),
              List.of(domain),
              family,
              List.of(),
              "mechanism_variable_formula",
              "broad_" + BuiltInReactionOutcomeCalibrationLibraries.key(family.name()) + "_channel",
              ReactionRegioselectivityClass.UNKNOWN,
              List.of(BuiltInReactionOutcomeCalibrationLibraries.key(family.name())),
              List.of("broad-mechanism-prior"),
              0.50,
              50.0,
              0.25,
              ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
              "internal:organic-taxonomy-mechanism-prior",
              "broad-mechanism-taxonomy-prior",
              false,
              "Internal broad organic taxonomy prior.",
              "Weak mechanism prior used only when exact or curated product distributions are absent.");
      datasets.add(
          ReactionOutcomeDistributionDataset.of(
              "broad_family_dataset_"
                  + BuiltInReactionOutcomeCalibrationLibraries.key(family.name()),
              "Broad mechanism prior for " + family.name(),
              "Internal non-exact taxonomy prior for reaction mechanism family coverage.",
              ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
              List.of(observation)));
    }
    return List.copyOf(datasets);
  }

  private static ReactionMechanismFamily defaultFamilyForDomain(
      final OrganicReactionDomain domain) {
    if (domain == OrganicReactionDomain.HETEROCYCLE_SYNTHESIS) {
      return ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION;
    }
    if (domain == OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION) {
      return ReactionMechanismFamily.HETEROCYCLE_FUNCTIONALIZATION;
    }
    if (domain == OrganicReactionDomain.CROSS_COUPLING) {
      return ReactionMechanismFamily.ORGANOMETALLIC_CROSS_COUPLING;
    }
    if (domain == OrganicReactionDomain.RADICAL_FUNCTIONALIZATION) {
      return ReactionMechanismFamily.RADICAL_SUBSTITUTION;
    }
    if (domain == OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION) {
      return ReactionMechanismFamily.NUCLEOPHILIC_SUBSTITUTION_SN2;
    }
    if (domain == OrganicReactionDomain.ELECTROPHILIC_SUBSTITUTION) {
      return ReactionMechanismFamily.ELECTROPHILIC_AROMATIC_SUBSTITUTION;
    }
    if (domain == OrganicReactionDomain.ADDITION) {
      return ReactionMechanismFamily.ADDITION;
    }
    if (domain == OrganicReactionDomain.ELIMINATION) {
      return ReactionMechanismFamily.ELIMINATION_E2;
    }
    if (domain == OrganicReactionDomain.CYCLIZATION) {
      return ReactionMechanismFamily.CYCLOADDITION;
    }
    if (domain == OrganicReactionDomain.OXIDATION_REDUCTION) {
      return ReactionMechanismFamily.OXIDATION;
    }
    if (domain == OrganicReactionDomain.ACID_BASE) {
      return ReactionMechanismFamily.ACID_BASE;
    }
    if (domain == OrganicReactionDomain.PROTECTION_DEPROTECTION) {
      return ReactionMechanismFamily.PROTECTING_GROUP;
    }
    if (domain == OrganicReactionDomain.REARRANGEMENT) {
      return ReactionMechanismFamily.REARRANGEMENT;
    }
    return ReactionMechanismFamily.UNKNOWN;
  }

  private static OrganicReactionDomain defaultDomainForFamily(
      final ReactionMechanismFamily family) {
    final String name = family.name();
    if (name.contains("COUPLING")
        || name.contains("ORGANOMETALLIC")
        || name.contains("C_H_ACTIVATION")) {
      return OrganicReactionDomain.CROSS_COUPLING;
    }
    if (name.contains("RADICAL")) {
      return OrganicReactionDomain.RADICAL_FUNCTIONALIZATION;
    }
    if (name.contains("OXIDATION")
        || name.contains("REDUCTION")
        || family == ReactionMechanismFamily.HYDROGENATION) {
      return OrganicReactionDomain.OXIDATION_REDUCTION;
    }
    if (name.contains("SUBSTITUTION")
        || family == ReactionMechanismFamily.SANDMEYER
        || family == ReactionMechanismFamily.SOLVOLYSIS) {
      return OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION;
    }
    if (name.contains("HETEROCYCLE")
        || name.contains("INDOLE")
        || name.contains("QUINOLINE")
        || family == ReactionMechanismFamily.HANTZSCH_SYNTHESIS
        || family == ReactionMechanismFamily.BIGINELLI_REACTION) {
      return OrganicReactionDomain.HETEROCYCLE_SYNTHESIS;
    }
    if (name.contains("CYCL")
        || name.contains("ANNULATION")
        || family == ReactionMechanismFamily.PAUSON_KHAND_REACTION) {
      return OrganicReactionDomain.CYCLIZATION;
    }
    if (name.contains("ELIMINATION")) {
      return OrganicReactionDomain.ELIMINATION;
    }
    if (name.contains("REARRANGEMENT") || name.contains("TAUTOMERIZATION")) {
      return OrganicReactionDomain.REARRANGEMENT;
    }
    if (name.contains("PROTECT") || name.contains("ACETAL")) {
      return OrganicReactionDomain.PROTECTION_DEPROTECTION;
    }
    if (family == ReactionMechanismFamily.ACID_BASE) {
      return OrganicReactionDomain.ACID_BASE;
    }
    if (name.contains("ADDITION")
        || name.contains("HYDRATION")
        || name.contains("HYDROHALOGENATION")) {
      return OrganicReactionDomain.ADDITION;
    }
    return OrganicReactionDomain.ELECTROPHILIC_SUBSTITUTION;
  }

  private static String key(final String value) {
    return value.toLowerCase(java.util.Locale.ROOT);
  }
}

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
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.backend.ClassicalBackendExecutionMode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalBackendProfile;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalCapability;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodType;

/**
 * Встроенные профили для планирования и тестирования без внешнего solver.
 */
public final class BuiltInClassicalBackendProfiles {

  private BuiltInClassicalBackendProfiles() {
    throw new AssertionError(
        "Built-in classical backend profiles utility must not be instantiated.");
  }

  public static ClassicalBackendProfile universalPlanning() {
    return ClassicalBackendProfile.of(
        "classical.planning.universal",
        "Universal Classical Planning Profile",
        "1.0",
        ClassicalBackendExecutionMode.PLANNING_ONLY,
        List.of(ChemistrySubjectKind.MOLECULE, ChemistrySubjectKind.REACTION),
        List.of(ClassicalCalculationKind.values()),
        List.of(
            ElectronicStructureMethodType.SEMI_EMPIRICAL,
            ElectronicStructureMethodType.HARTREE_FOCK,
            ElectronicStructureMethodType.PERTURBATION_THEORY,
            ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY,
            ElectronicStructureMethodType.CONFIGURATION_INTERACTION,
            ElectronicStructureMethodType.COUPLED_CLUSTER,
            ElectronicStructureMethodType.MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD,
            ElectronicStructureMethodType.FULL_CONFIGURATION_INTERACTION,
            ElectronicStructureMethodType.CUSTOM),
        List.of(),
        List.of(
            ClassicalCapability.MOLECULE_INPUT,
            ClassicalCapability.REACTION_INPUT,
            ClassicalCapability.THREE_DIMENSIONAL_GEOMETRY,
            ClassicalCapability.GEOMETRY_GRADIENT,
            ClassicalCapability.HESSIAN,
            ClassicalCapability.FREQUENCY_ANALYSIS,
            ClassicalCapability.THERMODYNAMICS,
            ClassicalCapability.KINETICS,
            ClassicalCapability.SPECTROSCOPY,
            ClassicalCapability.DESCRIPTOR_ANALYSIS,
            ClassicalCapability.CONFORMER_SEARCH,
            ClassicalCapability.ORGANIC_REACTION_PREDICTION,
            ClassicalCapability.REACTION_PATH,
            ClassicalCapability.TRANSITION_STATE_SEARCH,
            ClassicalCapability.ACTIVE_SPACE,
            ClassicalCapability.ACTIVE_SPACE_SELECTION,
            ClassicalCapability.ORBITAL_OPTIMIZATION,
            ClassicalCapability.POST_HF_CORRELATION,
            ClassicalCapability.ELECTRONIC_HAMILTONIAN,
            ClassicalCapability.QUANTUM_PROGRAM_PREPARATION,
            ClassicalCapability.QUANTUM_VQE_OPTIMIZATION,
            ClassicalCapability.SELF_CONSISTENT_DENSITY_FUNCTIONAL_THEORY,
            ClassicalCapability.RELATIVISTIC_EFFECTS,
            ClassicalCapability.SOLVENT_MODEL,
            ClassicalCapability.PERIODIC_BOUNDARY_CONDITIONS,
            ClassicalCapability.LOCAL_EXECUTION),
        1_000_000L,
        2_000_000L,
        20_000_000L,
        100_000L,
        1_000_000L);
  }

  public static ClassicalBackendProfile smallMoleculeAbInitio() {
    return ClassicalBackendProfile.of(
        "classical.local.small_ab_initio",
        "Small Molecule Ab Initio Profile",
        "1.0",
        ClassicalBackendExecutionMode.LOCAL_IN_PROCESS,
        List.of(ChemistrySubjectKind.MOLECULE, ChemistrySubjectKind.REACTION),
        List.of(
            ClassicalCalculationKind.SINGLE_POINT_ENERGY,
            ClassicalCalculationKind.GEOMETRY_GRADIENT,
            ClassicalCalculationKind.GEOMETRY_OPTIMIZATION,
            ClassicalCalculationKind.HESSIAN_ANALYSIS,
            ClassicalCalculationKind.FREQUENCY_ANALYSIS,
            ClassicalCalculationKind.THERMODYNAMIC_ANALYSIS,
            ClassicalCalculationKind.KINETIC_ANALYSIS,
            ClassicalCalculationKind.IR_RAMAN_PREDICTION,
            ClassicalCalculationKind.SPECTROSCOPY_PREDICTION,
            ClassicalCalculationKind.NMR_PREDICTION,
            ClassicalCalculationKind.MASS_SPECTROMETRY_PREDICTION,
            ClassicalCalculationKind.EXCITED_STATE_ANALYSIS,
            ClassicalCalculationKind.DESCRIPTOR_ANALYSIS,
            ClassicalCalculationKind.MOLECULAR_PROPERTY_ANALYSIS,
            ClassicalCalculationKind.CONFORMER_SEARCH,
            ClassicalCalculationKind.STEREOCHEMICAL_OUTCOME_ANALYSIS,
            ClassicalCalculationKind.ENANTIOMER_DIASTEREOMER_ANALYSIS,
            ClassicalCalculationKind.SOLVATION_MODELING,
            ClassicalCalculationKind.PKA_PREDICTION,
            ClassicalCalculationKind.REDOX_POTENTIAL,
            ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION,
            ClassicalCalculationKind.REACTION_ENERGY,
            ClassicalCalculationKind.REACTION_PATH,
            ClassicalCalculationKind.IRC_ANALYSIS,
            ClassicalCalculationKind.TRANSITION_STATE_SEARCH,
            ClassicalCalculationKind.SELECTIVITY_ANALYSIS,
            ClassicalCalculationKind.COMPETING_PATHWAY_ANALYSIS,
            ClassicalCalculationKind.REACTION_CONDITION_SCREENING,
            ClassicalCalculationKind.REACTION_CONDITION_OPTIMIZATION,
            ClassicalCalculationKind.SOLVENT_SCREENING,
            ClassicalCalculationKind.CATALYST_SCREENING,
            ClassicalCalculationKind.CONCENTRATION_SCREENING,
            ClassicalCalculationKind.BASIS_SET_CONVERGENCE,
            ClassicalCalculationKind.METHOD_BENCHMARK,
            ClassicalCalculationKind.UNCERTAINTY_ANALYSIS,
            ClassicalCalculationKind.MECHANISM_ENUMERATION,
            ClassicalCalculationKind.REACTION_HAZARD_AND_STABILITY_ANALYSIS,
            ClassicalCalculationKind.PRODUCT_DISTRIBUTION_ANALYSIS,
            ClassicalCalculationKind.YIELD_WINDOW_ESTIMATION,
            ClassicalCalculationKind.IMPURITY_AND_SIDE_PRODUCT_ANALYSIS,
            ClassicalCalculationKind.SCALE_UP_RISK_ANALYSIS,
            ClassicalCalculationKind.QUENCH_AND_WORKUP_ANALYSIS,
            ClassicalCalculationKind.REACTION_HISTORY_TIMELINE_ANALYSIS,
            ClassicalCalculationKind.REACTION_NETWORK_ANALYSIS,
            ClassicalCalculationKind.MICROKINETIC_MODELING,
            ClassicalCalculationKind.RETROSYNTHESIS_PLANNING,
            ClassicalCalculationKind.TRANSITION_STATE_ENSEMBLE_ANALYSIS,
            ClassicalCalculationKind.ACTIVE_SPACE_SELECTION,
            ClassicalCalculationKind.ORBITAL_OPTIMIZATION,
            ClassicalCalculationKind.MP2_CORRELATION,
            ClassicalCalculationKind.CISD_CORRELATION,
            ClassicalCalculationKind.CCSD_CORRELATION,
            ClassicalCalculationKind.FCI_DIAGONALIZATION,
            ClassicalCalculationKind.POST_HF_CORRELATION,
            ClassicalCalculationKind.ELECTRONIC_STRUCTURE_PREPARATION,
            ClassicalCalculationKind.HAMILTONIAN_PREPARATION,
            ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION,
            ClassicalCalculationKind.QUANTUM_VQE_OPTIMIZATION,
            ClassicalCalculationKind.RELATIVISTIC_CORRECTION,
            ClassicalCalculationKind.PERIODIC_BOUNDARY_ANALYSIS),
        List.of(
            ElectronicStructureMethodType.SEMI_EMPIRICAL,
            ElectronicStructureMethodType.HARTREE_FOCK,
            ElectronicStructureMethodType.PERTURBATION_THEORY,
            ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY,
            ElectronicStructureMethodType.CONFIGURATION_INTERACTION,
            ElectronicStructureMethodType.COUPLED_CLUSTER,
            ElectronicStructureMethodType.MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD,
            ElectronicStructureMethodType.FULL_CONFIGURATION_INTERACTION),
        List.of("STO-3G", "6-31G", "AUTO-EVEN-TEMPERED", "AUTO-EVEN-TEMPERED-COMPACT"),
        List.of(
            ClassicalCapability.MOLECULE_INPUT,
            ClassicalCapability.REACTION_INPUT,
            ClassicalCapability.THREE_DIMENSIONAL_GEOMETRY,
            ClassicalCapability.GEOMETRY_GRADIENT,
            ClassicalCapability.HESSIAN,
            ClassicalCapability.FREQUENCY_ANALYSIS,
            ClassicalCapability.THERMODYNAMICS,
            ClassicalCapability.KINETICS,
            ClassicalCapability.SPECTROSCOPY,
            ClassicalCapability.DESCRIPTOR_ANALYSIS,
            ClassicalCapability.CONFORMER_SEARCH,
            ClassicalCapability.ORGANIC_REACTION_PREDICTION,
            ClassicalCapability.REACTION_PATH,
            ClassicalCapability.TRANSITION_STATE_SEARCH,
            ClassicalCapability.SOLVENT_MODEL,
            ClassicalCapability.ACTIVE_SPACE,
            ClassicalCapability.ACTIVE_SPACE_SELECTION,
            ClassicalCapability.ORBITAL_OPTIMIZATION,
            ClassicalCapability.POST_HF_CORRELATION,
            ClassicalCapability.ELECTRONIC_HAMILTONIAN,
            ClassicalCapability.QUANTUM_PROGRAM_PREPARATION,
            ClassicalCapability.QUANTUM_VQE_OPTIMIZATION,
            ClassicalCapability.SELF_CONSISTENT_DENSITY_FUNCTIONAL_THEORY,
            ClassicalCapability.RELATIVISTIC_EFFECTS,
            ClassicalCapability.PERIODIC_BOUNDARY_CONDITIONS,
            ClassicalCapability.LOCAL_EXECUTION),
        256L,
        512L,
        4096L,
        128L,
        128L);
  }
}

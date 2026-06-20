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
    throw new AssertionError("Built-in classical backend profiles utility must not be instantiated.");
  }

  public static ClassicalBackendProfile universalPlanning() {
    return ClassicalBackendProfile.of(
        "classical.planning.universal",
        "Universal Classical Planning Profile",
        "1.0",
        ClassicalBackendExecutionMode.PLANNING_ONLY,
        List.of(
            ChemistrySubjectKind.MOLECULE,
            ChemistrySubjectKind.REACTION),
        List.of(
            ClassicalCalculationKind.SINGLE_POINT_ENERGY,
            ClassicalCalculationKind.GEOMETRY_OPTIMIZATION,
            ClassicalCalculationKind.FREQUENCY_ANALYSIS,
            ClassicalCalculationKind.THERMODYNAMIC_ANALYSIS,
            ClassicalCalculationKind.KINETIC_ANALYSIS,
            ClassicalCalculationKind.SPECTROSCOPY_PREDICTION,
            ClassicalCalculationKind.DESCRIPTOR_ANALYSIS,
            ClassicalCalculationKind.CONFORMER_SEARCH,
            ClassicalCalculationKind.REACTION_ENERGY,
            ClassicalCalculationKind.REACTION_PATH,
            ClassicalCalculationKind.TRANSITION_STATE_SEARCH,
            ClassicalCalculationKind.ELECTRONIC_STRUCTURE_PREPARATION,
            ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
        List.of(
            ElectronicStructureMethodType.HARTREE_FOCK,
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
            ClassicalCapability.REACTION_PATH,
            ClassicalCapability.TRANSITION_STATE_SEARCH,
            ClassicalCapability.ACTIVE_SPACE,
            ClassicalCapability.ELECTRONIC_HAMILTONIAN,
            ClassicalCapability.SOLVENT_MODEL,
            ClassicalCapability.LOCAL_EXECUTION),
        1_000_000L,
        2_000_000L,
        20_000_000L,
        1_000_000L);
  }

  public static ClassicalBackendProfile smallMoleculeAbInitio() {
    return ClassicalBackendProfile.of(
        "classical.local.small_ab_initio",
        "Small Molecule Ab Initio Profile",
        "1.0",
        ClassicalBackendExecutionMode.LOCAL_IN_PROCESS,
        List.of(
            ChemistrySubjectKind.MOLECULE,
            ChemistrySubjectKind.REACTION),
        List.of(
            ClassicalCalculationKind.SINGLE_POINT_ENERGY,
            ClassicalCalculationKind.GEOMETRY_OPTIMIZATION,
            ClassicalCalculationKind.FREQUENCY_ANALYSIS,
            ClassicalCalculationKind.REACTION_ENERGY,
            ClassicalCalculationKind.ELECTRONIC_STRUCTURE_PREPARATION,
            ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
        List.of(
            ElectronicStructureMethodType.HARTREE_FOCK,
            ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY,
            ElectronicStructureMethodType.CONFIGURATION_INTERACTION),
        List.of(
            "STO-3G",
            "6-31G",
            "def2-SVP"),
        List.of(
            ClassicalCapability.MOLECULE_INPUT,
            ClassicalCapability.REACTION_INPUT,
            ClassicalCapability.THREE_DIMENSIONAL_GEOMETRY,
            ClassicalCapability.GEOMETRY_GRADIENT,
            ClassicalCapability.HESSIAN,
            ClassicalCapability.FREQUENCY_ANALYSIS,
            ClassicalCapability.ACTIVE_SPACE,
            ClassicalCapability.ELECTRONIC_HAMILTONIAN,
            ClassicalCapability.LOCAL_EXECUTION),
        256L,
        512L,
        4096L,
        128L);
  }
}
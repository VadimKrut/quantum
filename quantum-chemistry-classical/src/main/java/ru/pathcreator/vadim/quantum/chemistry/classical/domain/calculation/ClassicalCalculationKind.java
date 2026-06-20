/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation;

/**
 * Тип классической химической задачи, которую можно спланировать или выполнить через backend.
 */
public enum ClassicalCalculationKind {

  SINGLE_POINT_ENERGY,
  GEOMETRY_OPTIMIZATION,
  FREQUENCY_ANALYSIS,
  THERMODYNAMIC_ANALYSIS,
  KINETIC_ANALYSIS,
  SPECTROSCOPY_PREDICTION,
  DESCRIPTOR_ANALYSIS,
  CONFORMER_SEARCH,
  REACTION_ENERGY,
  REACTION_PATH,
  TRANSITION_STATE_SEARCH,
  ELECTRONIC_STRUCTURE_PREPARATION,
  HAMILTONIAN_PREPARATION;
}
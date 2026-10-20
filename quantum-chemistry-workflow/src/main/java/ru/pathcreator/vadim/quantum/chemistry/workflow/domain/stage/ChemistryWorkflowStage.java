/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.workflow.domain.stage;

/**
 * Стабильные этапы запуска chemistry workflow.
 */
public enum ChemistryWorkflowStage {
  CORE_VALIDATION,
  CLASSICAL_PREFLIGHT,
  CLASSICAL_PLANNING,
  CLASSICAL_EXECUTION,
  REACTION_PREDICTION,
  CONDITION_EFFECT_ANALYSIS,
  CONDITION_SCENARIO_PLANNING,
  CONDITION_SCENARIO_EVALUATION,
  FOLLOW_UP_PLANNING,
  HAMILTONIAN_PREPARATION,
  FERMION_MAPPING,
  QUANTUM_PROGRAM_PREPARATION,
  VQE_OPTIMIZATION,
  COMPUTATION_PARITY_PLANNING,
  ERROR_MITIGATION_PLANNING
}

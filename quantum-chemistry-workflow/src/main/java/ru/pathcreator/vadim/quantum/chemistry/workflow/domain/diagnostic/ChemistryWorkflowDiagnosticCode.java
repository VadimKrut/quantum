/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.workflow.domain.diagnostic;

/**
 * Стабильный код диагностики общего chemistry workflow.
 */
public enum ChemistryWorkflowDiagnosticCode {
  JOB_INVALID,
  CORE_VALIDATION_FAILED,
  CLASSICAL_PREFLIGHT_FAILED,
  CLASSICAL_EXECUTION_FAILED,
  REACTION_PREDICTION_FAILED,
  REACTION_PREDICTION_WARNING,
  HAMILTONIAN_CAPABILITY_UNSUPPORTED,
  HAMILTONIAN_PREPARATION_FAILED,
  FERMION_MAPPING_FAILED,
  QUANTUM_PROGRAM_GENERATION_FAILED,
  VQE_BACKEND_UNAVAILABLE,
  VQE_OPTIMIZATION_FAILED,
  ERROR_MITIGATION_PLANNING_FAILED
}

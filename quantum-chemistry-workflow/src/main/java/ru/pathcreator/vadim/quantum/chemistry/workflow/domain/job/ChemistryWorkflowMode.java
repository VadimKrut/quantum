/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.workflow.domain.job;

/**
 * Режим выполнения chemistry workflow.
 */
public enum ChemistryWorkflowMode {
  VALIDATE_ONLY,
  CLASSICAL_ONLY,
  HAMILTONIAN_ONLY,
  CLASSICAL_AND_HAMILTONIAN,
  FULL_QUANTUM_PREPARATION
}

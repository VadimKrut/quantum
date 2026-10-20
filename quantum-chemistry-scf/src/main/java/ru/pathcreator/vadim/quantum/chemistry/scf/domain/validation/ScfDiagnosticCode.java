/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.scf.domain.validation;

/**
 * Стабильный код проверки SCF, пригодный для CLI, UI и workflow-отчетов.
 */
public enum ScfDiagnosticCode {
  RESULT_NOT_CONVERGED,
  MISSING_ITERATION_HISTORY,
  FINAL_ITERATION_NOT_CONVERGED,
  DENSITY_TRACE_MISMATCH,
  DENSITY_NOT_SYMMETRIC,
  MO_NOT_ORTHONORMAL,
  ORBITAL_ENERGIES_NOT_SORTED,
  SPIN_CONTAMINATION_PRESENT
}

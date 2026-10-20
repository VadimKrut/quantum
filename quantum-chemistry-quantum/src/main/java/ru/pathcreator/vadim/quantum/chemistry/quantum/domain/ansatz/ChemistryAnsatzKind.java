/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz;

/**
 * Семейство quantum chemistry программ поверх active-space модели.
 */
public enum ChemistryAnsatzKind {
  HARTREE_FOCK_REFERENCE,
  HARDWARE_EFFICIENT_VQE,
  UCCSD,
  ADAPT_VQE_OPERATOR_POOL,
  QUANTUM_PHASE_ESTIMATION
}

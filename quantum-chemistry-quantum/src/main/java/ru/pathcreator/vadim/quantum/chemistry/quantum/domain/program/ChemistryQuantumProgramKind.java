/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program;

/**
 * Семейство quantum chemistry программ, которые можно подготовить из одного электронного Hamiltonian.
 */
public enum ChemistryQuantumProgramKind {
  HARTREE_FOCK_REFERENCE,
  PAULI_EXPECTATION_MEASUREMENT,
  PAULI_EVOLUTION,
  VERIFIED_PAULI_EVOLUTION,
  SPIN_BLOCK_PARITY_TAPERED_EVOLUTION,
  UCCSD_ANSATZ,
  HARDWARE_EFFICIENT_VQE_ANSATZ,
  ADAPT_VQE_ANSATZ,
  QUANTUM_PHASE_ESTIMATION
}

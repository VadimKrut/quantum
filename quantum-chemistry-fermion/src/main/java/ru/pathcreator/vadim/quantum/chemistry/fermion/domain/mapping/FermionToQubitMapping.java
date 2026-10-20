/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping;

/**
 * Семейство реализованных отображений fermionic operators в qubit Pauli operators.
 */
public enum FermionToQubitMapping {
  JORDAN_WIGNER,
  REVERSED_JORDAN_WIGNER,
  PARITY,
  BRAVYI_KITAEV,
  SPIN_BLOCK_JORDAN_WIGNER,
  SPIN_BLOCK_PARITY,
  SPIN_BLOCK_BRAVYI_KITAEV
}

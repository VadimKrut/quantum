/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.fermion.domain.validation;

/** Стабильный код проблемы Hamiltonian перед построением fermion/qubit operators. */
public enum FermionHamiltonianDiagnosticCode {
  SPIN_ORBITAL_COUNT_MISMATCH,
  ELECTRON_SECTOR_EXCEEDS_SPIN_ORBITALS,
  ODD_ELECTRON_SECTOR,
  EMPTY_ELECTRONIC_TERMS,
  MISSING_ONE_ELECTRON_TERMS,
  MISSING_TWO_ELECTRON_TERMS,
  ORBITAL_INDEX_OUT_OF_RANGE,
  DUPLICATE_ONE_ELECTRON_SYMMETRY_SLOT,
  DUPLICATE_TWO_ELECTRON_SYMMETRY_SLOT,
  INCONSISTENT_ONE_ELECTRON_SYMMETRY_VALUE,
  INCONSISTENT_TWO_ELECTRON_SYMMETRY_VALUE,
  NEGATIVE_COULOMB_DIAGONAL,
  MISSING_COULOMB_DIAGONALS,
  LARGE_INTEGRAL_MAGNITUDE
}

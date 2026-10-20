/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.prediction;

/** Органический класс задачи, по которому выбираются правила и расчетные проверки. */
public enum OrganicReactionDomain {
  HETEROCYCLE_SYNTHESIS,
  HETEROCYCLE_FUNCTIONALIZATION,
  CROSS_COUPLING,
  RADICAL_FUNCTIONALIZATION,
  NUCLEOPHILIC_SUBSTITUTION,
  ELECTROPHILIC_SUBSTITUTION,
  ADDITION,
  ELIMINATION,
  CYCLIZATION,
  OXIDATION_REDUCTION,
  ACID_BASE,
  PROTECTION_DEPROTECTION,
  REARRANGEMENT,
  UNKNOWN;
}

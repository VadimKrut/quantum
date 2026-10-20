/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.prediction;

/**
 * Обобщенный класс механизма, который можно вывести из правил, условий и доказательств прогноза.
 */
public enum ReactionMechanismClass {
  HETEROCYCLE_FUNCTIONALIZATION,
  HETEROCYCLE_CONSTRUCTION,
  NUCLEOPHILIC_AROMATIC_SUBSTITUTION,
  ELECTROPHILIC_AROMATIC_SUBSTITUTION,
  CROSS_COUPLING,
  CYCLIZATION,
  CONDENSATION,
  ADDITION,
  ELIMINATION,
  OXIDATION,
  REDUCTION,
  ACID_BASE,
  RADICAL,
  PHOTOCHEMICAL,
  ELECTROCHEMICAL,
  REARRANGEMENT,
  UNKNOWN;
}

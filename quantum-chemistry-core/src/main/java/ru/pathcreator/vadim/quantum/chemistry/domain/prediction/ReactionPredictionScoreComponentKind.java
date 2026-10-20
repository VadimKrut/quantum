/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.prediction;

/** Тип вклада, из которого собирается объяснимая вероятность продукта реакции. */
public enum ReactionPredictionScoreComponentKind {
  USER_PRIOR,
  MECHANISM_MATCH,
  CONDITION_MATCH,
  FUNCTIONAL_GROUP,
  HETEROCYCLE,
  STERIC,
  ELECTRONIC,
  SOLVENT,
  TEMPERATURE,
  CATALYST,
  STOICHIOMETRY,
  KINETIC,
  THERMODYNAMIC,
  SELECTIVITY,
  QUANTUM_MODEL,
  CALIBRATION,
  WARNING_PENALTY,
  TIE_BREAKER;
}

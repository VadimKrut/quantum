/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.prediction;

/** Тип объяснения, почему продукт-кандидат получил свой рейтинг. */
public enum ReactionPredictionEvidenceKind {
  FUNCTIONAL_GROUP_MATCH,
  HETEROCYCLE_PATTERN,
  STERIC_FACTOR,
  ELECTRONIC_FACTOR,
  SOLVENT_EFFECT,
  TEMPERATURE_EFFECT,
  CATALYST_EFFECT,
  KINETIC_FACTOR,
  THERMODYNAMIC_FACTOR,
  COMPETING_PATHWAY,
  QUANTUM_ENERGY_MODEL,
  CALIBRATION_PRIOR,
  SURROGATE_PRODUCT,
  USER_RULE,
  USER_SEED,
  WARNING;
}

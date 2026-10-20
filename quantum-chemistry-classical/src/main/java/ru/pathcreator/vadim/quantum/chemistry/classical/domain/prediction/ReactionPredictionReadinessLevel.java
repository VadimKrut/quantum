/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.prediction;

/** Уровень готовности прогноза реакции к практическому использованию. */
public enum ReactionPredictionReadinessLevel {
  READY_FOR_DECISION_SUPPORT,
  QUANTUM_OR_ELECTRONIC_RERANK_RECOMMENDED,
  COMPETING_PATHWAYS_REQUIRE_RERANK,
  CONDITION_INCOMPLETE,
  PROBABILITY_MASS_INCOMPLETE,
  NO_PRODUCT_HYPOTHESIS;
}

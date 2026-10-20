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
 * Источник прогноза продукта: правило, расчет, экспериментальная модель или гибрид.
 */
public enum ReactionPredictionMethodType {
  RULE_BASED,
  CLASSICAL_QUANTUM_CHEMISTRY,
  QUANTUM_PROGRAM_ASSISTED,
  EMPIRICAL_MODEL,
  HYBRID,
  USER_SUPPLIED;
}

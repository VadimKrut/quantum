/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.storage.domain.model;

/**
 * Тип исхода реакции в лабораторной истории: план, прогноз, расчет или наблюдение.
 */
public enum ChemistryStorageReactionOutcomeKind {
  PLANNED,
  PREDICTED,
  CLASSICAL_CALCULATED,
  QUANTUM_PREPARED,
  QUANTUM_CALCULATED,
  OBSERVED,
  REJECTED
}

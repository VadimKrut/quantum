/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.storage.domain.history;

/**
 * Тип события в истории реакции: ручная правка, наблюдение, прогноз или расчет.
 */
public enum ChemistryStorageReactionEventKind {
  USER_EDIT,
  CONDITIONS_EDIT,
  PRODUCT_EDIT,
  MECHANISM_EDIT,
  OBSERVATION_ADDED,
  PREDICTION_REQUESTED,
  PREDICTION_RECORDED,
  CLASSICAL_CALCULATION_PLANNED,
  CLASSICAL_CALCULATION_COMPLETED,
  QUANTUM_PROGRAM_PREPARED,
  QUANTUM_CALCULATION_COMPLETED,
  REVIEW_NOTE,
  IMPORTED,
  FAILED
}

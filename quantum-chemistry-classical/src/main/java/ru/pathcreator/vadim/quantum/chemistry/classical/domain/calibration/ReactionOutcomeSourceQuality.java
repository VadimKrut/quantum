/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration;

/** Качество источника, из которого взято распределение продуктов реакции. */
public enum ReactionOutcomeSourceQuality {
  EXACT_EXPERIMENTAL_PRODUCT_DISTRIBUTION,
  EXPERIMENTAL_YIELD_TABLE,
  TIME_RESOLVED_POPULATION_STUDY,
  LITERATURE_REVIEW_CLASS_PRIOR,
  LOCAL_BENCHMARK,
  USER_OBSERVATION,
  UNKNOWN
}

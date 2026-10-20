/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation;

/**
 * Тип рекомендуемого действия после выполнения workflow.
 */
public enum ChemistryWorkflowRecommendationKind {
  PRODUCT_REFINEMENT,
  PRODUCT_COMPARISON,
  CONDITION_REPAIR,
  CONDITION_SENSITIVITY,
  SCALE_UP_RISK_ANALYSIS,
  MECHANISM_DISAMBIGUATION,
  QUANTUM_PREPARATION,
  WARNING_REVIEW
}

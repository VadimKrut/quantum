/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation;

/**
 * Стабильный код диагностики error-mitigation planner.
 */
public enum ErrorMitigationDiagnosticCode {
  PLAN_READY,
  IDEAL_NOISE_MODEL,
  READOUT_CALIBRATION_REQUIRED,
  ZERO_NOISE_EXTRAPOLATION_RECOMMENDED,
  SYMMETRY_CHECK_RECOMMENDED,
  SHOT_COUNT_INCREASED,
  HARDWARE_NOISE_UNKNOWN,
  TECHNIQUE_UNSUPPORTED,
  REQUEST_INVALID;
}

/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic;

/**
 * Стабильные коды диагностики для API, CLI и desktop.
 */
public enum ClassicalDiagnosticCode {

  REQUEST_VALID,
  REQUEST_INVALID,
  SUBJECT_KIND_UNSUPPORTED,
  CALCULATION_KIND_UNSUPPORTED,
  METHOD_TYPE_UNSUPPORTED,
  BASIS_SET_UNSUPPORTED,
  ACTIVE_SPACE_REQUIRED,
  ACTIVE_SPACE_UNSUPPORTED,
  MOLECULE_TOO_LARGE,
  ELECTRON_COUNT_TOO_LARGE,
  REACTION_TOO_LARGE,
  GEOMETRY_REQUIRED,
  GRADIENT_REQUIRED,
  HESSIAN_REQUIRED,
  SOLVENT_MODEL_UNSUPPORTED,
  PERIODIC_MODEL_UNSUPPORTED,
  APPROXIMATION_REQUIRED,
  BACKEND_UNAVAILABLE,
  PLAN_READY,
  RESULT_PARTIAL,
  RESULT_FAILED;
}
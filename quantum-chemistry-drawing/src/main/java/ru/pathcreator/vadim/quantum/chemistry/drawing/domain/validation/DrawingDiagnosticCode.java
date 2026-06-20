/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.domain.validation;

/**
 * Стабильные коды ошибок и предупреждений графического chemistry-документа.
 */
public enum DrawingDiagnosticCode {

  EMPTY_DOCUMENT,
  DUPLICATE_MOLECULE_ID,
  DUPLICATE_REACTION_ID,
  ATOM_DRAWING_WITHOUT_CORE_ATOM,
  BOND_DRAWING_WITHOUT_CORE_BOND,
  REACTION_WITHOUT_DRAWN_PARTICIPANT,
  MANUAL_FIELD_DUPLICATE,
  CORE_PANEL_DUPLICATE,
  CORE_PANEL_REQUIRED_FIELD_MISSING,
  CORE_AREA_PANEL_MISSING,
  CORE_PANEL_OWNER_MISSING,
  FEATURE_COVERAGE_INCOMPLETE
}
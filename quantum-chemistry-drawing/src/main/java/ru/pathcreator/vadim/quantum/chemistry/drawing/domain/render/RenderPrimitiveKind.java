/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.domain.render;

/**
 * Тип примитива, который desktop может отрисовать любым UI toolkit.
 */
public enum RenderPrimitiveKind {

  ATOM_LABEL,
  BOND_LINE,
  BOND_WEDGE,
  BOND_DASH,
  REACTION_ARROW,
  REACTION_CONDITION_LABEL,
  CORE_PANEL,
  MANUAL_FIELD_PANEL,
  SELECTION_MARKER
}
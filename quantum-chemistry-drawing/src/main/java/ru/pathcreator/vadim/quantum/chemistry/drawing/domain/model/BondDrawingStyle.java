/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model;

/**
 * Вид отображения связи, включая стереохимические клинья и служебные стили.
 */
public enum BondDrawingStyle {

  PLAIN,
  WEDGE_UP,
  WEDGE_DOWN,
  DASHED,
  WAVY,
  AROMATIC_RING,
  COORDINATE_ARROW,
  UNKNOWN
}
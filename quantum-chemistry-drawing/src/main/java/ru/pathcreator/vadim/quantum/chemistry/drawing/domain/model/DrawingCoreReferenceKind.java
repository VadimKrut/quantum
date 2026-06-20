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
 * Тип ссылки из формы drawing-редактора на объект chemistry-core.
 */
public enum DrawingCoreReferenceKind {

  DOCUMENT,
  MOLECULE,
  ATOM,
  BOND,
  REACTION,
  REACTION_SIDE,
  REACTION_PARTICIPANT,
  EXPERIMENT,
  ELECTRONIC_PROBLEM,
  REPORT,
  FREE_OBJECT
}
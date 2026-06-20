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
 * Крупная область chemistry-core, которую редактор должен уметь показать и заполнить.
 */
public enum DrawingCoreArea {

  STRUCTURE,
  ELEMENT,
  FORMULA,
  COMPOSITION,
  GRAPH,
  VALENCE,
  GEOMETRY,
  STEREO,
  CONFORMATION,
  ISOMER,
  SYMMETRY,
  SOLUTION,
  REACTION,
  MECHANISM,
  KINETICS,
  THERMODYNAMICS,
  SPECTROSCOPY,
  PROPERTY,
  ELECTRONIC_METHOD,
  ELECTRONIC_PROBLEM,
  REDOX,
  ACID_BASE,
  STATE,
  EXPERIMENT,
  REPORT,
  METADATA,
  DIAGNOSTIC
}
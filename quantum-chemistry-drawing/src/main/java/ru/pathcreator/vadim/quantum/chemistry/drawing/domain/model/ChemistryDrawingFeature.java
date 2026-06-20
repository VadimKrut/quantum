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
 * Область chemistry-core, для которой drawing-модуль обязан дать графический или ручной способ ввода.
 */
public enum ChemistryDrawingFeature {

  MOLECULE_STRUCTURE,
  ATOM_PROPERTIES,
  BOND_PROPERTIES,
  FORMULA_AND_COMPOSITION,
  GRAPH_AND_RINGS,
  VALENCE,
  STEREOCHEMISTRY,
  CONFORMATION,
  OPTICAL_ROTATION,
  SYMMETRY,
  REACTION_SCHEME,
  REACTION_CONDITIONS,
  SOLUTION_ENVIRONMENT,
  KINETICS,
  THERMODYNAMICS,
  SPECTROSCOPY,
  ELECTRONIC_PROBLEM,
  REDOX,
  ACID_BASE,
  MECHANISM,
  EXPERIMENT,
  REPORT,
  METADATA,
  MANUAL_FIELDS
}
/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile;

/**
 * Техническая возможность классического backend.
 */
public enum ClassicalCapability {

  MOLECULE_INPUT,
  REACTION_INPUT,
  THREE_DIMENSIONAL_GEOMETRY,
  GEOMETRY_GRADIENT,
  HESSIAN,
  FREQUENCY_ANALYSIS,
  THERMODYNAMICS,
  KINETICS,
  SPECTROSCOPY,
  DESCRIPTOR_ANALYSIS,
  CONFORMER_SEARCH,
  REACTION_PATH,
  TRANSITION_STATE_SEARCH,
  ACTIVE_SPACE,
  ELECTRONIC_HAMILTONIAN,
  SOLVENT_MODEL,
  PERIODIC_BOUNDARY_CONDITIONS,
  REMOTE_EXECUTION,
  LOCAL_EXECUTION;
}
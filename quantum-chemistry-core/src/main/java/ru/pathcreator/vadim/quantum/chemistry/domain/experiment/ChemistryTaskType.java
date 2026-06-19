/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.experiment;

public enum ChemistryTaskType {

  GROUND_STATE_ENERGY,
  REACTION_ENERGY,
  EXCITED_STATE_ESTIMATION,
  GEOMETRY_OPTIMIZATION,
  HAMILTONIAN_PREPARATION;
}
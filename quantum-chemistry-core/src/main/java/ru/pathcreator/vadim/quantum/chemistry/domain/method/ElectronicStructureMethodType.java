/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.method;

public enum ElectronicStructureMethodType {

  HARTREE_FOCK,
  DENSITY_FUNCTIONAL_THEORY,
  CONFIGURATION_INTERACTION,
  COUPLED_CLUSTER,
  MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD,
  FULL_CONFIGURATION_INTERACTION,
  QUANTUM_VARIATIONAL,
  QUANTUM_PHASE_ESTIMATION,
  CUSTOM;
}
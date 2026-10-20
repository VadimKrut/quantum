/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation;

/**
 * Семантически независимая техника error mitigation для quantum chemistry запуска.
 */
public enum ErrorMitigationTechnique {
  READOUT_CALIBRATION,
  ZERO_NOISE_EXTRAPOLATION,
  SYMMETRY_VERIFICATION,
  PARTICLE_NUMBER_POST_SELECTION,
  SPIN_PARITY_POST_SELECTION,
  SHOT_REBALANCING,
  PAULI_TWIRLING,
  DYNAMICAL_DECOUPLING;
}

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
 * Класс noise model, который влияет на выбор error-mitigation стратегии.
 */
public enum QuantumNoiseModelKind {
  IDEAL,
  READOUT_BIASED,
  DEPOLARIZING,
  DEPHASING,
  AMPLITUDE_DAMPING,
  COHERENT_OVER_ROTATION,
  CROSSTALK,
  HARDWARE_UNKNOWN;
}

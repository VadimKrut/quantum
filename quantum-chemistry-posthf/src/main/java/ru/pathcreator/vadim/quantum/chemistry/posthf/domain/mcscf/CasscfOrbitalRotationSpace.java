/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.posthf.domain.mcscf;

import java.util.Locale;

/**
 * Область орбитальных вращений для Java-native CASSCF оптимизации.
 */
public enum CasscfOrbitalRotationSpace {
  ACTIVE_EXTERNAL,
  INACTIVE_ACTIVE,
  INACTIVE_EXTERNAL,
  ALL_NON_REDUNDANT;

  public static CasscfOrbitalRotationSpace parse(final String value) {
    if (value == null || value.isBlank()) {
      return ACTIVE_EXTERNAL;
    }
    final String normalized = value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
    for (final CasscfOrbitalRotationSpace space : CasscfOrbitalRotationSpace.values()) {
      if (space.name().equals(normalized)) {
        return space;
      }
    }
    throw new IllegalArgumentException("Unsupported CASSCF orbital rotation space: " + value);
  }
}

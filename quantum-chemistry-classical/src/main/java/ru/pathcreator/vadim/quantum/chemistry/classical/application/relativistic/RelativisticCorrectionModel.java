/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.relativistic;

import java.util.Locale;

/** Модель скалярной релятивистской поправки для локального classical workflow. */
public enum RelativisticCorrectionModel {
  AUTO,
  MASS_VELOCITY_DARWIN,
  SCALAR_ZORA,
  DKH2_SCALAR;

  public static RelativisticCorrectionModel fromText(final String value) {
    if (value == null || value.isBlank()) {
      return AUTO;
    }
    final String normalized =
        value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    for (final RelativisticCorrectionModel model : RelativisticCorrectionModel.values()) {
      if (model.name().equals(normalized)) {
        return model;
      }
    }
    throw new IllegalArgumentException("Unknown relativistic correction model: " + value + ".");
  }
}

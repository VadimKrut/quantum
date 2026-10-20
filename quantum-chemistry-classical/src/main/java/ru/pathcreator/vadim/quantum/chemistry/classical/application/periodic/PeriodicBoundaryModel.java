/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.periodic;

import java.util.Locale;

/** Модель периодических граничных условий для переносимого Java-native анализа. */
public enum PeriodicBoundaryModel {
  AUTO,
  MINIMUM_IMAGE,
  CLUSTER_IMAGE,
  CRYSTAL_CELL;

  public static PeriodicBoundaryModel fromText(final String value) {
    if (value == null || value.isBlank()) {
      return AUTO;
    }
    final String normalized =
        value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    for (final PeriodicBoundaryModel model : PeriodicBoundaryModel.values()) {
      if (model.name().equals(normalized)) {
        return model;
      }
    }
    return AUTO;
  }
}

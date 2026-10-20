/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry;

import java.util.Locale;

/**
 * Режим энергии, от которого строятся локальные градиенты и релаксация.
 */
public enum GeometryEnergyModelKind {
  AUTO("auto"),
  BONDED_FORCE_FIELD("bonded_force_field"),
  ELECTRONIC_STRUCTURE_FINITE_DIFFERENCE("electronic_structure_finite_difference");

  private final String optionValue;

  GeometryEnergyModelKind(final String optionValue) {
    this.optionValue = optionValue;
  }

  String optionValue() {
    return this.optionValue;
  }

  static GeometryEnergyModelKind fromOption(final String value) {
    final String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    if ("auto".equals(normalized) || "automatic".equals(normalized)) {
      return AUTO;
    }
    if ("bonded_force_field".equals(normalized) || "force_field".equals(normalized)) {
      return BONDED_FORCE_FIELD;
    }
    if ("electronic_structure_finite_difference".equals(normalized)
        || "single_point_finite_difference".equals(normalized)
        || "ab_initio".equals(normalized)
        || "first_principles".equals(normalized)) {
      return ELECTRONIC_STRUCTURE_FINITE_DIFFERENCE;
    }
    throw new IllegalArgumentException("Unknown geometry energy model: " + value + ".");
  }
}

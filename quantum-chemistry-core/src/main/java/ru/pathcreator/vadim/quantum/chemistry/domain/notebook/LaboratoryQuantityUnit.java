/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.notebook;

import java.math.BigDecimal;

/** Exact conversion within one physical dimension; cross-dimension conversion requires a model. */
public enum LaboratoryQuantityUnit {
  GRAM("г", "mass", "1"),
  MILLIGRAM("мг", "mass", "0.001"),
  KILOGRAM("кг", "mass", "1000"),
  MOLE("моль", "amount", "1"),
  MILLIMOLE("ммоль", "amount", "0.001"),
  MICROMOLE("мкмоль", "amount", "0.000001"),
  LITRE("л", "volume", "1"),
  MILLILITRE("мл", "volume", "0.001"),
  MICROLITRE("мкл", "volume", "0.000001");

  private final String label;
  private final String dimension;
  private final BigDecimal factor;

  LaboratoryQuantityUnit(final String label, final String dimension, final String factor) {
    this.label = label;
    this.dimension = dimension;
    this.factor = new BigDecimal(factor);
  }

  public String dimension() {
    return this.dimension;
  }

  public BigDecimal factor() {
    return this.factor;
  }

  public String toString() {
    return this.label;
  }
}

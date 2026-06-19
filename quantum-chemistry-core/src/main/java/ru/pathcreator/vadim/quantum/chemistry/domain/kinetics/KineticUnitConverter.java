/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.kinetics;

import ru.pathcreator.vadim.quantum.chemistry.domain.unit.MolarConcentration;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.MolarConcentrationUnit;

public final class KineticUnitConverter {

  private KineticUnitConverter() {}

  public static double concentrationMolePerLiter(final MolarConcentration concentration) {
    if (concentration == null) {
      throw new IllegalArgumentException("Molar concentration must not be null.");
    }
    if (concentration.unit() == MolarConcentrationUnit.MOLE_PER_LITER) {
      return concentration.value();
    }
    if (concentration.unit() == MolarConcentrationUnit.MILLIMOLE_PER_LITER) {
      return concentration.value() / 1000.0;
    }
    if (concentration.unit() == MolarConcentrationUnit.MICROMOLE_PER_LITER) {
      return concentration.value() / 1000000.0;
    }
    throw new IllegalStateException("Unsupported concentration unit.");
  }

  public static double rateMolePerLiterSecond(final ReactionRateValue rate) {
    if (rate == null) {
      throw new IllegalArgumentException("Reaction rate must not be null.");
    }
    if (rate.unit() == ReactionRateUnit.MOLE_PER_LITER_SECOND) {
      return rate.value();
    }
    if (rate.unit() == ReactionRateUnit.MOLE_PER_LITER_MINUTE) {
      return rate.value() / 60.0;
    }
    if (rate.unit() == ReactionRateUnit.MOLE_PER_LITER_HOUR) {
      return rate.value() / 3600.0;
    }
    throw new IllegalStateException("Unsupported reaction rate unit.");
  }
}
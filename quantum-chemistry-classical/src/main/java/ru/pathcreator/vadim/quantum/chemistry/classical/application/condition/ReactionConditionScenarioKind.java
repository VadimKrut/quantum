/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.condition;

/**
 * Тип расчетного сценария условий, который можно сравнить с исходной точкой реакции.
 */
public enum ReactionConditionScenarioKind {
  BASELINE,
  ROOM_TEMPERATURE,
  HEATED,
  COOLED,
  INERT_ANHYDROUS,
  CATALYST_SIGNAL,
  ACID_ONLY,
  BASE_ONLY,
  MILD_BASE_CONTROL,
  STRONG_BASE_CONTROL,
  ACID_SCAVENGER_CONTROL,
  OXIDANT_CONTROL,
  REDUCTANT_CONTROL,
  HYDROGENATION_CONTROL,
  PHOTOCHEMICAL_CONTROL,
  ELECTROCHEMICAL_CONTROL,
  MOISTURE_CONTROL,
  DEHYDRATION_CONTROL,
  POLAR_APROTIC_SOLVENT,
  POLAR_PROTIC_SOLVENT,
  NONPOLAR_SOLVENT,
  POLYETHER_HIGH_BOILING_SOLVENT,
  REFLUX_CONDITION,
  SEALED_HEATED_CONDITION,
  MICROWAVE_HEATED_CONDITION,
  FLOW_CONDITION,
  DILUTE_REGIME,
  CONCENTRATED_REGIME,
  NUCLEOPHILE_EXCESS,
  ELECTROPHILE_EXCESS,
  BASE_EXCESS,
  LARGE_SCALE_MASS_TRANSFER,
  SHORT_DURATION,
  LONG_DURATION
}

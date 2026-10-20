/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.reaction;

/**
 * Технологический или физический режим, влияющий на ход реакции.
 */
public enum ReactionConditionFeatureKind {
  INERT_ATMOSPHERE,
  AIR_SENSITIVE,
  ANHYDROUS,
  CATALYST_PRESENT,
  DEHYDRATING_AGENT,
  ACIDIC_MEDIUM,
  BASIC_MEDIUM,
  OXIDIZING_MEDIUM,
  REDUCING_MEDIUM,
  HYDROGENATION,
  PHOTOCHEMICAL,
  ELECTROCHEMICAL,
  MICROWAVE,
  SEALED_VESSEL,
  FLOW_REACTOR,
  REFLUX,
  COOLING,
  HEATING,
  ADDITION_RATE,
  WORKUP,
  PURIFICATION,
  CUSTOM;
}

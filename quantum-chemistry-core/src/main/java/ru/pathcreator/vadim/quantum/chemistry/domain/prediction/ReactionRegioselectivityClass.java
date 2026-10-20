/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.prediction;

/**
 * Тип селективности, важный для объяснения конкурирующих органических продуктов.
 */
public enum ReactionRegioselectivityClass {
  SINGLE_DOMINANT_SITE,
  COMPETING_REGIOISOMERS,
  HETEROATOM_DIRECTED,
  LEAVING_GROUP_DIRECTED,
  STERICALLY_CONTROLLED,
  ELECTRONICALLY_CONTROLLED,
  CONDITION_CONTROLLED,
  NOT_APPLICABLE,
  UNKNOWN;
}

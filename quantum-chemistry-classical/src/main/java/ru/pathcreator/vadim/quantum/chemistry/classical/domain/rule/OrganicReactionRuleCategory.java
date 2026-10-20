/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule;

/** Химический класс правила, используемый для поиска и диагностики. */
public enum OrganicReactionRuleCategory {
  HETEROCYCLE_SYNTHESIS,
  HETEROCYCLE_FUNCTIONALIZATION,
  RADICAL_FUNCTIONALIZATION,
  SUBSTITUTION,
  ADDITION,
  ELIMINATION,
  OXIDATION,
  REDUCTION,
  ACID_BASE,
  PROTECTION,
  DEPROTECTION,
  COUPLING,
  REARRANGEMENT,
  CUSTOM;
}

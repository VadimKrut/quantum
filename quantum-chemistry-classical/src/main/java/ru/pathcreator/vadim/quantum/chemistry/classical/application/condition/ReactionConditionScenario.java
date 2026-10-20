/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.condition;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;

/**
 * Одна неизменяемая точка условий для сравнения прогноза или расчета реакции.
 */
public final class ReactionConditionScenario {

  private final String id;
  private final ReactionConditionScenarioKind kind;
  private final ReactionConditions conditions;
  private final String rationale;

  private ReactionConditionScenario(
      final String id,
      final ReactionConditionScenarioKind kind,
      final ReactionConditions conditions,
      final String rationale) {
    this.id = id;
    this.kind = kind;
    this.conditions = conditions;
    this.rationale = rationale;
  }

  public static ReactionConditionScenario of(
      final String id,
      final ReactionConditionScenarioKind kind,
      final ReactionConditions conditions,
      final String rationale) {
    if (kind == null) {
      throw new IllegalArgumentException("Reaction condition scenario kind must not be null.");
    }
    if (conditions == null) {
      throw new IllegalArgumentException(
          "Reaction condition scenario conditions must not be null.");
    }
    return new ReactionConditionScenario(
        TextValue.requireText(id, "Reaction condition scenario id"),
        kind,
        conditions,
        TextValue.requireText(rationale, "Reaction condition scenario rationale"));
  }

  public String id() {
    return this.id;
  }

  public ReactionConditionScenarioKind kind() {
    return this.kind;
  }

  public ReactionConditions conditions() {
    return this.conditions;
  }

  public String rationale() {
    return this.rationale;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionConditionScenario)) {
      return false;
    }
    final ReactionConditionScenario scenario = (ReactionConditionScenario) other;
    return Objects.equals(this.id, scenario.id)
        && this.kind == scenario.kind
        && Objects.equals(this.conditions, scenario.conditions)
        && Objects.equals(this.rationale, scenario.rationale);
  }

  public int hashCode() {
    return Objects.hash(this.id, this.kind, this.conditions, this.rationale);
  }
}

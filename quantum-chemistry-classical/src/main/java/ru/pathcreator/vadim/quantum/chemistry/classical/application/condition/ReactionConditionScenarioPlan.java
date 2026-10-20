/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.condition;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/**
 * Набор сценариев условий реакции, подготовленный для расчетного сравнения.
 */
public final class ReactionConditionScenarioPlan {

  private final String requestId;
  private final List<ReactionConditionScenario> scenarios;

  private ReactionConditionScenarioPlan(
      final String requestId, final List<ReactionConditionScenario> scenarios) {
    this.requestId = requestId;
    this.scenarios = scenarios;
  }

  public static ReactionConditionScenarioPlan of(
      final String requestId, final List<ReactionConditionScenario> scenarios) {
    final List<ReactionConditionScenario> checkedScenarios =
        ReactionConditionScenarioPlan.checkedScenarios(scenarios);
    return new ReactionConditionScenarioPlan(
        TextValue.requireText(requestId, "Reaction condition scenario plan request id"),
        checkedScenarios);
  }

  public String requestId() {
    return this.requestId;
  }

  public List<ReactionConditionScenario> scenarios() {
    return this.scenarios;
  }

  public boolean empty() {
    return this.scenarios.isEmpty();
  }

  public boolean hasKind(final ReactionConditionScenarioKind kind) {
    if (kind == null) {
      return false;
    }
    for (int i = 0; i < this.scenarios.size(); ++i) {
      if (this.scenarios.get(i).kind() == kind) {
        return true;
      }
    }
    return false;
  }

  private static List<ReactionConditionScenario> checkedScenarios(
      final List<ReactionConditionScenario> scenarios) {
    if (scenarios == null || scenarios.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < scenarios.size(); ++i) {
      if (scenarios.get(i) == null) {
        throw new IllegalArgumentException("Reaction condition scenario must not be null.");
      }
      for (int j = i + 1; j < scenarios.size(); ++j) {
        if (Objects.equals(scenarios.get(i).id(), scenarios.get(j).id())) {
          throw new IllegalArgumentException("Reaction condition scenario ids must be unique.");
        }
      }
    }
    return List.copyOf(scenarios);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionConditionScenarioPlan)) {
      return false;
    }
    final ReactionConditionScenarioPlan plan = (ReactionConditionScenarioPlan) other;
    return Objects.equals(this.requestId, plan.requestId)
        && Objects.equals(this.scenarios, plan.scenarios);
  }

  public int hashCode() {
    return Objects.hash(this.requestId, this.scenarios);
  }
}

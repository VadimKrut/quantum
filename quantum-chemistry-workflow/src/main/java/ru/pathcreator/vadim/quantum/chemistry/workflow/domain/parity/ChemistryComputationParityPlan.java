/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.workflow.domain.parity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;

/**
 * План сопоставления обычных и квантовых расчетов для предсказанной реакции.
 */
public final class ChemistryComputationParityPlan {

  public static final ChemistryComputationParityPlan EMPTY =
      new ChemistryComputationParityPlan("", List.of());

  private final String workflowJobId;
  private final List<ChemistryComputationParityItem> items;

  private ChemistryComputationParityPlan(
      final String workflowJobId, final List<ChemistryComputationParityItem> items) {
    this.workflowJobId = workflowJobId;
    this.items = items;
  }

  public static ChemistryComputationParityPlan of(
      final String workflowJobId, final List<ChemistryComputationParityItem> items) {
    if (items == null || items.isEmpty()) {
      return EMPTY;
    }
    for (int i = 0; i < items.size(); ++i) {
      if (items.get(i) == null) {
        throw new IllegalArgumentException("Chemistry computation parity item must not be null.");
      }
    }
    return new ChemistryComputationParityPlan(
        IdentifierValue.requireIdentifier(
            workflowJobId, "Chemistry computation parity workflow job id"),
        List.copyOf(items));
  }

  public String workflowJobId() {
    return this.workflowJobId;
  }

  public List<ChemistryComputationParityItem> items() {
    return this.items;
  }

  public boolean empty() {
    return this.items.isEmpty();
  }

  public boolean hasReadyItem() {
    for (int i = 0; i < this.items.size(); ++i) {
      if (this.items.get(i).status() == ChemistryComputationParityStatus.READY
          && !this.items.get(i).hasBlockingDiagnostics()) {
        return true;
      }
    }
    return false;
  }

  public boolean hasBlockingDiagnostics() {
    for (int i = 0; i < this.items.size(); ++i) {
      if (this.items.get(i).hasBlockingDiagnostics()) {
        return true;
      }
    }
    return false;
  }

  public List<ChemistryComputationParityDiagnostic> diagnostics() {
    if (this.items.isEmpty()) {
      return List.of();
    }
    final ArrayList<ChemistryComputationParityDiagnostic> diagnostics =
        new ArrayList<ChemistryComputationParityDiagnostic>();
    for (int i = 0; i < this.items.size(); ++i) {
      diagnostics.addAll(this.items.get(i).diagnostics());
    }
    return List.copyOf(diagnostics);
  }

  public ChemistryComputationParityItem firstItem() {
    if (this.items.isEmpty()) {
      throw new IllegalStateException("Chemistry computation parity plan is empty.");
    }
    return this.items.get(0);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryComputationParityPlan)) {
      return false;
    }
    final ChemistryComputationParityPlan plan = (ChemistryComputationParityPlan) other;
    return Objects.equals(this.workflowJobId, plan.workflowJobId)
        && Objects.equals(this.items, plan.items);
  }

  public int hashCode() {
    return Objects.hash(this.workflowJobId, this.items);
  }
}

/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.workflow.domain.stage;

import java.util.Objects;

/**
 * Краткий результат одного этапа pipeline.
 */
public final class ChemistryWorkflowStageResult {

  private final ChemistryWorkflowStage stage;
  private final ChemistryWorkflowStageStatus status;
  private final String message;

  private ChemistryWorkflowStageResult(
      final ChemistryWorkflowStage stage,
      final ChemistryWorkflowStageStatus status,
      final String message) {
    this.stage = stage;
    this.status = status;
    this.message = message;
  }

  public static ChemistryWorkflowStageResult of(
      final ChemistryWorkflowStage stage,
      final ChemistryWorkflowStageStatus status,
      final String message) {
    if (stage == null) {
      throw new IllegalArgumentException("Chemistry workflow stage must not be null.");
    }
    if (status == null) {
      throw new IllegalArgumentException("Chemistry workflow stage status must not be null.");
    }
    return new ChemistryWorkflowStageResult(stage, status, message == null ? "" : message);
  }

  public ChemistryWorkflowStage stage() {
    return this.stage;
  }

  public ChemistryWorkflowStageStatus status() {
    return this.status;
  }

  public String message() {
    return this.message;
  }

  public boolean failed() {
    return this.status == ChemistryWorkflowStageStatus.FAILED;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryWorkflowStageResult)) {
      return false;
    }
    final ChemistryWorkflowStageResult result = (ChemistryWorkflowStageResult) other;
    return this.stage == result.stage
        && this.status == result.status
        && Objects.equals(this.message, result.message);
  }

  public int hashCode() {
    return Objects.hash(this.stage, this.status, this.message);
  }
}

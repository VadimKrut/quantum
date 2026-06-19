/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.experiment;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;

public final class ChemistryExperiment {

  private final ChemistryExperimentId id;
  private final ChemistrySubject subject;
  private final ChemistryTask task;
  private final ChemistryExecutionMode executionMode;
  private final ChemistryMetadata metadata;

  private ChemistryExperiment(
      final ChemistryExperimentId id,
      final ChemistrySubject subject,
      final ChemistryTask task,
      final ChemistryExecutionMode executionMode,
      final ChemistryMetadata metadata) {
    this.id = id;
    this.subject = subject;
    this.task = task;
    this.executionMode = executionMode;
    this.metadata = metadata;
  }

  public static ChemistryExperiment of(
      final ChemistryExperimentId id,
      final ChemistrySubject subject,
      final ChemistryTask task,
      final ChemistryExecutionMode executionMode) {
    return ChemistryExperiment.of(id, subject, task, executionMode, ChemistryMetadata.EMPTY);
  }

  public static ChemistryExperiment of(
      final ChemistryExperimentId id,
      final ChemistrySubject subject,
      final ChemistryTask task,
      final ChemistryExecutionMode executionMode,
      final ChemistryMetadata metadata) {
    if (id == null) {
      throw new IllegalArgumentException("Chemistry experiment id must not be null.");
    }
    if (subject == null) {
      throw new IllegalArgumentException("Experiment subject must not be null.");
    }
    if (task == null) {
      throw new IllegalArgumentException("Chemistry task must not be null.");
    }
    if (executionMode == null) {
      throw new IllegalArgumentException("Chemistry execution mode must not be null.");
    }
    final ChemistryMetadata checkedMetadata = metadata == null ? ChemistryMetadata.EMPTY : metadata;
    return new ChemistryExperiment(id, subject, task, executionMode, checkedMetadata);
  }

  public ChemistryExperimentId id() {
    return this.id;
  }

  public ChemistrySubject subject() {
    return this.subject;
  }

  public ChemistryTask task() {
    return this.task;
  }

  public ChemistryExecutionMode executionMode() {
    return this.executionMode;
  }

  public ChemistryMetadata metadata() {
    return this.metadata;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryExperiment)) {
      return false;
    }
    final ChemistryExperiment experiment = (ChemistryExperiment) other;
    return Objects.equals(this.id, experiment.id)
        && Objects.equals(this.subject, experiment.subject)
        && Objects.equals(this.task, experiment.task)
        && this.executionMode == experiment.executionMode
        && Objects.equals(this.metadata, experiment.metadata);
  }

  public int hashCode() {
    return Objects.hash(
        new Object[] {this.id, this.subject, this.task, this.executionMode, this.metadata});
  }
}
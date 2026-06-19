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
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ConvergenceCriteria;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethod;

public final class ChemistryTask {

  private final ChemistryTaskType type;
  private final BasisSet basisSet;
  private final ActiveSpace activeSpace;
  private final ElectronicStructureMethod method;
  private final ConvergenceCriteria convergenceCriteria;

  private ChemistryTask(
      final ChemistryTaskType type,
      final BasisSet basisSet,
      final ActiveSpace activeSpace,
      final ElectronicStructureMethod method,
      final ConvergenceCriteria convergenceCriteria) {
    this.type = type;
    this.basisSet = basisSet;
    this.activeSpace = activeSpace;
    this.method = method;
    this.convergenceCriteria = convergenceCriteria;
  }

  public static ChemistryTask of(
      final ChemistryTaskType type, final BasisSet basisSet, final ActiveSpace activeSpace) {
    return ChemistryTask.of(
        type,
        basisSet,
        activeSpace,
        ElectronicStructureMethod.HARTREE_FOCK,
        ConvergenceCriteria.DEFAULT);
  }

  public static ChemistryTask of(
      final ChemistryTaskType type,
      final BasisSet basisSet,
      final ActiveSpace activeSpace,
      final ElectronicStructureMethod method,
      final ConvergenceCriteria convergenceCriteria) {
    if (type == null) {
      throw new IllegalArgumentException("Chemistry task type must not be null.");
    }
    if (basisSet == null) {
      throw new IllegalArgumentException("Basis set must not be null.");
    }
    if (method == null) {
      throw new IllegalArgumentException("Electronic structure method must not be null.");
    }
    if (convergenceCriteria == null) {
      throw new IllegalArgumentException("Convergence criteria must not be null.");
    }
    return new ChemistryTask(type, basisSet, activeSpace, method, convergenceCriteria);
  }

  public ChemistryTaskType type() {
    return this.type;
  }

  public BasisSet basisSet() {
    return this.basisSet;
  }

  public ActiveSpace activeSpace() {
    return this.activeSpace;
  }

  public boolean hasActiveSpace() {
    return this.activeSpace != null;
  }

  public ElectronicStructureMethod method() {
    return this.method;
  }

  public ConvergenceCriteria convergenceCriteria() {
    return this.convergenceCriteria;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryTask)) {
      return false;
    }
    final ChemistryTask task = (ChemistryTask) other;
    return this.type == task.type
        && Objects.equals(this.basisSet, task.basisSet)
        && Objects.equals(this.activeSpace, task.activeSpace)
        && Objects.equals(this.method, task.method)
        && Objects.equals(this.convergenceCriteria, task.convergenceCriteria);
  }

  public int hashCode() {
    return Objects.hash(
        new Object[] {
          this.type, this.basisSet, this.activeSpace, this.method, this.convergenceCriteria
        });
  }
}
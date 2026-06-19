/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.problem;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistryTask;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

public final class ElectronicStructureProblem {

  private final ElectronicProblemId id;
  private final Molecule molecule;
  private final ChemistryTask task;
  private final ElectronicHamiltonian hamiltonian;
  private final MolecularOrbitalBasis orbitalBasis;

  private ElectronicStructureProblem(
      final ElectronicProblemId id,
      final Molecule molecule,
      final ChemistryTask task,
      final ElectronicHamiltonian hamiltonian,
      final MolecularOrbitalBasis orbitalBasis) {
    this.id = id;
    this.molecule = molecule;
    this.task = task;
    this.hamiltonian = hamiltonian;
    this.orbitalBasis = orbitalBasis;
  }

  public static ElectronicStructureProblem of(
      final ElectronicProblemId id,
      final Molecule molecule,
      final ChemistryTask task,
      final ElectronicHamiltonian hamiltonian) {
    return ElectronicStructureProblem.of(id, molecule, task, hamiltonian, null);
  }

  public static ElectronicStructureProblem of(
      final ElectronicProblemId id,
      final Molecule molecule,
      final ChemistryTask task,
      final ElectronicHamiltonian hamiltonian,
      final MolecularOrbitalBasis orbitalBasis) {
    if (id == null) {
      throw new IllegalArgumentException("Electronic problem id must not be null.");
    }
    if (molecule == null) {
      throw new IllegalArgumentException("Electronic problem molecule must not be null.");
    }
    if (task == null) {
      throw new IllegalArgumentException("Electronic problem task must not be null.");
    }
    if (hamiltonian == null) {
      throw new IllegalArgumentException("Electronic problem Hamiltonian must not be null.");
    }
    if (!task.hasActiveSpace()) {
      throw new IllegalArgumentException("Electronic problem task must define active space.");
    }
    if (!task.activeSpace().equals(hamiltonian.activeSpace())) {
      throw new IllegalArgumentException(
          "Electronic problem task active space must match Hamiltonian active space.");
    }
    if (orbitalBasis != null) {
      orbitalBasis.requireCompatibleWith(task.activeSpace());
    }
    return new ElectronicStructureProblem(id, molecule, task, hamiltonian, orbitalBasis);
  }

  public ElectronicProblemId id() {
    return this.id;
  }

  public Molecule molecule() {
    return this.molecule;
  }

  public ChemistryTask task() {
    return this.task;
  }

  public ElectronicHamiltonian hamiltonian() {
    return this.hamiltonian;
  }

  public MolecularOrbitalBasis orbitalBasis() {
    return this.orbitalBasis;
  }

  public boolean hasOrbitalBasis() {
    return this.orbitalBasis != null;
  }

  public int requiredQubitCount() {
    return this.hamiltonian.spinOrbitalCount();
  }

  public boolean hasElectronicTerms() {
    return !this.hamiltonian.emptyElectronicTerms();
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ElectronicStructureProblem)) {
      return false;
    }
    final ElectronicStructureProblem problem = (ElectronicStructureProblem) other;
    return Objects.equals(this.id, problem.id)
        && Objects.equals(this.molecule, problem.molecule)
        && Objects.equals(this.task, problem.task)
        && Objects.equals(this.hamiltonian, problem.hamiltonian)
        && Objects.equals(this.orbitalBasis, problem.orbitalBasis);
  }

  public int hashCode() {
    return Objects.hash(this.id, this.molecule, this.task, this.hamiltonian, this.orbitalBasis);
  }
}
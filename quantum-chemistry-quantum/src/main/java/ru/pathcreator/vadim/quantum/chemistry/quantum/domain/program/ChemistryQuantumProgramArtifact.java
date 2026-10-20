/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

/**
 * Один готовый Quantum IR-артефакт с описанием, откуда он получен и для какого расчета нужен.
 */
public final class ChemistryQuantumProgramArtifact {

  private final String id;
  private final ChemistryQuantumProgramKind kind;
  private final FermionToQubitMapping mapping;
  private final String description;
  private final QuantumProgram program;
  private final int circuitCount;
  private final long operationCount;

  private ChemistryQuantumProgramArtifact(
      final String id,
      final ChemistryQuantumProgramKind kind,
      final FermionToQubitMapping mapping,
      final String description,
      final QuantumProgram program,
      final int circuitCount,
      final long operationCount) {
    this.id = id;
    this.kind = kind;
    this.mapping = mapping;
    this.description = description;
    this.program = program;
    this.circuitCount = circuitCount;
    this.operationCount = operationCount;
  }

  public static ChemistryQuantumProgramArtifact of(
      final String id,
      final ChemistryQuantumProgramKind kind,
      final FermionToQubitMapping mapping,
      final String description,
      final QuantumProgram program) {
    ChemistryQuantumProgramArtifact.requireText(id, "Chemistry quantum program artifact id");
    if (kind == null) {
      throw new IllegalArgumentException(
          "Chemistry quantum program artifact kind must not be null.");
    }
    ChemistryQuantumProgramArtifact.requireText(
        description, "Chemistry quantum program artifact description");
    if (program == null) {
      throw new IllegalArgumentException(
          "Chemistry quantum program artifact program must not be null.");
    }
    return new ChemistryQuantumProgramArtifact(
        id,
        kind,
        mapping,
        description,
        program,
        program.circuitCount(),
        ChemistryQuantumProgramArtifact.operationCount(program));
  }

  public String id() {
    return this.id;
  }

  public ChemistryQuantumProgramKind kind() {
    return this.kind;
  }

  public FermionToQubitMapping mapping() {
    return this.mapping;
  }

  public boolean hasMapping() {
    return this.mapping != null;
  }

  public String description() {
    return this.description;
  }

  public QuantumProgram program() {
    return this.program;
  }

  public int circuitCount() {
    return this.circuitCount;
  }

  public long operationCount() {
    return this.operationCount;
  }

  private static long operationCount(final QuantumProgram program) {
    long count = 0L;
    for (int i = 0; i < program.circuitCount(); ++i) {
      final QuantumCircuit circuit = program.circuit(i);
      count += circuit.operationCount();
    }
    return count;
  }

  private static void requireText(final String value, final String subject) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(subject + " must not be blank.");
    }
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryQuantumProgramArtifact)) {
      return false;
    }
    final ChemistryQuantumProgramArtifact artifact = (ChemistryQuantumProgramArtifact) other;
    return this.circuitCount == artifact.circuitCount
        && this.operationCount == artifact.operationCount
        && Objects.equals(this.id, artifact.id)
        && this.kind == artifact.kind
        && this.mapping == artifact.mapping
        && Objects.equals(this.description, artifact.description)
        && Objects.equals(this.program, artifact.program);
  }

  public int hashCode() {
    return Objects.hash(
        this.id,
        this.kind,
        this.mapping,
        this.description,
        this.program,
        this.circuitCount,
        this.operationCount);
  }
}

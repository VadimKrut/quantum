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

/**
 * Объясняет, почему конкретный quantum chemistry артефакт не был построен для задачи.
 */
public final class ChemistryQuantumProgramSkip {

  private final String id;
  private final ChemistryQuantumProgramKind kind;
  private final FermionToQubitMapping mapping;
  private final String reason;

  private ChemistryQuantumProgramSkip(
      final String id,
      final ChemistryQuantumProgramKind kind,
      final FermionToQubitMapping mapping,
      final String reason) {
    this.id = id;
    this.kind = kind;
    this.mapping = mapping;
    this.reason = reason;
  }

  public static ChemistryQuantumProgramSkip of(
      final String id,
      final ChemistryQuantumProgramKind kind,
      final FermionToQubitMapping mapping,
      final String reason) {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("Chemistry quantum program skip id must not be blank.");
    }
    if (kind == null) {
      throw new IllegalArgumentException("Chemistry quantum program skip kind must not be null.");
    }
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException(
          "Chemistry quantum program skip reason must not be blank.");
    }
    return new ChemistryQuantumProgramSkip(id, kind, mapping, reason);
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

  public String reason() {
    return this.reason;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryQuantumProgramSkip)) {
      return false;
    }
    final ChemistryQuantumProgramSkip skip = (ChemistryQuantumProgramSkip) other;
    return Objects.equals(this.id, skip.id)
        && this.kind == skip.kind
        && this.mapping == skip.mapping
        && Objects.equals(this.reason, skip.reason);
  }

  public int hashCode() {
    return Objects.hash(this.id, this.kind, this.mapping, this.reason);
  }
}

/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

public final class ChemistryDiagnosticTarget {

  private final String kind;
  private final String id;

  private ChemistryDiagnosticTarget(
      final String kind,
      final String id
  ) {
    this.kind = kind;
    this.id = id;
  }

  public static ChemistryDiagnosticTarget of(
      final String kind,
      final String id
  ) {
    return new ChemistryDiagnosticTarget(
        TextValue.requireText(kind, "Diagnostic target kind"),
        TextValue.requireText(id, "Diagnostic target id"));
  }

  public String kind() {
    return this.kind;
  }

  public String id() {
    return this.id;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryDiagnosticTarget)) {
      return false;
    }
    final ChemistryDiagnosticTarget target = (ChemistryDiagnosticTarget) other;
    return Objects.equals(this.kind, target.kind) && Objects.equals(this.id, target.id);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.kind);
    result = ChemistryHash.include(result, this.id);
    return result;
  }
}
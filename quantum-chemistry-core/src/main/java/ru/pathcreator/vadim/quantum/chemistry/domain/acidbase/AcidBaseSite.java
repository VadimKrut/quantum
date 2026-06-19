/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.acidbase;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;

/** Кислотно-основный site молекулы: метка, атом и роль в протонировании. */
public final class AcidBaseSite {

  private final String label;
  private final AtomId atomId;
  private final AcidBaseSiteKind kind;

  private AcidBaseSite(
      final String label,
      final AtomId atomId,
      final AcidBaseSiteKind kind
  ) {
    this.label = label;
    this.atomId = atomId;
    this.kind = kind;
  }

  public static AcidBaseSite of(
      final String label, final AtomId atomId, final AcidBaseSiteKind kind) {
    final String checkedLabel = TextValue.requireText(label, "Acid-base site label");
    if (atomId == null) {
      throw new IllegalArgumentException("Acid-base site atom id must not be null.");
    }
    if (kind == null) {
      throw new IllegalArgumentException("Acid-base site kind must not be null.");
    }
    return new AcidBaseSite(checkedLabel, atomId, kind);
  }

  public String label() {
    return this.label;
  }

  public AtomId atomId() {
    return this.atomId;
  }

  public AcidBaseSiteKind kind() {
    return this.kind;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof AcidBaseSite)) {
      return false;
    }
    final AcidBaseSite site = (AcidBaseSite) other;
    return Objects.equals(this.label, site.label)
        && Objects.equals(this.atomId, site.atomId)
        && this.kind == site.kind;
  }

  public int hashCode() {
    int result = this.label.hashCode();
    result = 31 * result + this.atomId.hashCode();
    result = 31 * result + this.kind.hashCode();
    return result;
  }
}
/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.structure;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;

public final class AtomId {

  private final String value;

  private AtomId(final String value) {
    this.value = value;
  }

  public static AtomId of(final String value) {
    return new AtomId(IdentifierValue.requireIdentifier(value, "Atom id"));
  }

  public String value() {
    return this.value;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof AtomId)) {
      return false;
    }
    final AtomId atomId = (AtomId) other;
    return Objects.equals(this.value, atomId.value);
  }

  public int hashCode() {
    return this.value.hashCode();
  }

  public String toString() {
    return this.value;
  }
}
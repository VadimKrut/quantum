/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.reaction;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;

public final class ReactionId {

  private final String value;

  private ReactionId(final String value) {
    this.value = value;
  }

  public static ReactionId of(final String value) {
    return new ReactionId(IdentifierValue.requireIdentifier(value, "Reaction id"));
  }

  public String value() {
    return this.value;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionId)) {
      return false;
    }
    final ReactionId id = (ReactionId) other;
    return Objects.equals(this.value, id.value);
  }

  public int hashCode() {
    return this.value.hashCode();
  }
}
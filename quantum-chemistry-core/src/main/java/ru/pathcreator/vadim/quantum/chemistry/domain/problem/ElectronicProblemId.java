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
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;

public final class ElectronicProblemId {

  private final String value;

  private ElectronicProblemId(final String value) {
    this.value = value;
  }

  public static ElectronicProblemId of(final String value) {
    return new ElectronicProblemId(
        IdentifierValue.requireIdentifier(value, "Electronic problem id"));
  }

  public String value() {
    return this.value;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ElectronicProblemId)) {
      return false;
    }
    final ElectronicProblemId id = (ElectronicProblemId) other;
    return Objects.equals(this.value, id.value);
  }

  public int hashCode() {
    return this.value.hashCode();
  }
}
/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.metadata;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;

public final class ChemistrySourceLocation {

  private final int line;
  private final int column;

  private ChemistrySourceLocation(
      final int line,
      final int column
  ) {
    this.line = line;
    this.column = column;
  }

  public static ChemistrySourceLocation of(
      final int line,
      final int column
  ) {
    if (line < 1) {
      throw new IllegalArgumentException("Source line must be positive.");
    }
    if (column < 1) {
      throw new IllegalArgumentException("Source column must be positive.");
    }
    return new ChemistrySourceLocation(line, column);
  }

  public int line() {
    return this.line;
  }

  public int column() {
    return this.column;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistrySourceLocation)) {
      return false;
    }
    final ChemistrySourceLocation location = (ChemistrySourceLocation) other;
    return this.line == location.line && this.column == location.column;
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.line);
    result = ChemistryHash.include(result, this.column);
    return result;
  }
}
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
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

public final class ChemistrySource {

  private final String format;
  private final String description;

  private ChemistrySource(
      final String format,
      final String description
  ) {
    this.format = format;
    this.description = description;
  }

  public static ChemistrySource of(
      final String format,
      final String description
  ) {
    return new ChemistrySource(
        TextValue.requireText(format, "Chemistry source format"),
        TextValue.requireText(description, "Chemistry source description"));
  }

  public String format() {
    return this.format;
  }

  public String description() {
    return this.description;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistrySource)) {
      return false;
    }
    final ChemistrySource source = (ChemistrySource) other;
    return Objects.equals(this.format, source.format)
        && Objects.equals(this.description, source.description);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.format);
    result = ChemistryHash.include(result, this.description);
    return result;
  }
}
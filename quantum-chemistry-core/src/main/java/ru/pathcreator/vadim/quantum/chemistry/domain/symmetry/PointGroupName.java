/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.symmetry;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

public final class PointGroupName {

  public static final PointGroupName C1 = new PointGroupName("C1");
  private final String value;

  private PointGroupName(final String value) {
    this.value = value;
  }

  public static PointGroupName of(final String value) {
    final String checkedValue = TextValue.requireText(value, "Point group name");
    if (checkedValue.length() > 16) {
      throw new IllegalArgumentException("Point group name must not exceed 16 characters.");
    }
    for (int i = 0; i < checkedValue.length(); ++i) {
      char current = checkedValue.charAt(i);
      if (Character.isLetterOrDigit(current) || current == '-' || current == '_') continue;
      throw new IllegalArgumentException("Point group name contains unsupported character.");
    }
    if ("C1".equals(checkedValue)) {
      return C1;
    }
    return new PointGroupName(checkedValue);
  }

  public String value() {
    return this.value;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof PointGroupName)) {
      return false;
    }
    final PointGroupName pointGroupName = (PointGroupName) other;
    return Objects.equals(this.value, pointGroupName.value);
  }

  public int hashCode() {
    return this.value.hashCode();
  }
}
/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.element;

import java.util.Locale;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

public final class ElementSymbol {

  private final String value;

  private ElementSymbol(final String value) {
    this.value = value;
  }

  public static ElementSymbol of(final String value) {
    final String trimmed = TextValue.requireText(value, "Element symbol");
    if (trimmed.length() < 1 || trimmed.length() > 3) {
      throw new IllegalArgumentException("Element symbol length must be from 1 to 3.");
    }
    for (int i = 0; i < trimmed.length(); ++i) {
      final char current = trimmed.charAt(i);
      if (Character.isLetter(current)) continue;
      throw new IllegalArgumentException("Element symbol must contain only letters.");
    }
    final String lower = trimmed.toLowerCase(Locale.ROOT);
    final String canonical = Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    return new ElementSymbol(canonical);
  }

  public String value() {
    return this.value;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ElementSymbol)) {
      return false;
    }
    final ElementSymbol elementSymbol = (ElementSymbol) other;
    return Objects.equals(this.value, elementSymbol.value);
  }

  public int hashCode() {
    return this.value.hashCode();
  }

  public String toString() {
    return this.value;
  }
}
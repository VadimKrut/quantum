/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.common;

public final class IdentifierValue {

  private IdentifierValue() {}

  public static String requireIdentifier(
      final String value,
      final String subjectName
  ) {
    final String trimmed = TextValue.requireText(value, subjectName);
    final char first = trimmed.charAt(0);
    if (!IdentifierValue.isIdentifierStart(first)) {
      throw new IllegalArgumentException(subjectName + " must start with a letter or underscore.");
    }
    for (int i = 1; i < trimmed.length(); ++i) {
      final char current = trimmed.charAt(i);
      if (IdentifierValue.isIdentifierPart(current)) continue;
      throw new IllegalArgumentException(subjectName + " contains an invalid character.");
    }
    return trimmed;
  }

  public static String fromText(final String value) {
    final String trimmed = TextValue.requireText(value, "Identifier source text");
    final StringBuilder builder = new StringBuilder();
    boolean previousSeparator = false;
    for (int i = 0; i < trimmed.length(); ++i) {
      final char current = Character.toLowerCase(trimmed.charAt(i));
      if (IdentifierValue.isIdentifierPart(current)) {
        builder.append(current);
        previousSeparator = false;
        continue;
      }
      if (previousSeparator || builder.length() == 0) continue;
      builder.append('_');
      previousSeparator = true;
    }
    while (builder.length() > 0 && builder.charAt(builder.length() - 1) == '_') {
      builder.deleteCharAt(builder.length() - 1);
    }
    if (builder.length() == 0) {
      return "id";
    }
    if (!IdentifierValue.isIdentifierStart(builder.charAt(0))) {
      builder.insert(0, "id_");
    }
    return builder.toString();
  }

  private static boolean isIdentifierStart(final char value) {
    return Character.isLetter(value) || value == '_';
  }

  private static boolean isIdentifierPart(final char value) {
    return Character.isLetterOrDigit(value) || value == '_' || value == '-' || value == '.';
  }
}
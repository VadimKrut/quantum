/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.storage.infrastructure.text;

/**
 * Quoted-string escaping для собственного QCHEM storage format.
 */
final class QchemEscaper {

  private QchemEscaper() {
  }

  static String quote(final String value) {
    if (value == null) {
      return "none";
    }
    final StringBuilder builder = new StringBuilder(value.length() + 2);
    builder.append('"');
    for (int i = 0; i < value.length(); ++i) {
      final char ch = value.charAt(i);
      if (ch == '\\' || ch == '"') {
        builder.append('\\');
        builder.append(ch);
      } else if (ch == '\n') {
        builder.append("\\n");
      } else if (ch == '\r') {
        builder.append("\\r");
      } else if (ch == '\t') {
        builder.append("\\t");
      } else {
        builder.append(ch);
      }
    }
    builder.append('"');
    return builder.toString();
  }

  static String unquote(final String value) {
    if ("none".equals(value)) {
      return null;
    }
    if (value.length() < 2 || value.charAt(0) != '"' || value.charAt(value.length() - 1) != '"') {
      throw new IllegalArgumentException("Quoted value expected.");
    }
    final StringBuilder builder = new StringBuilder(value.length());
    boolean escaped = false;
    for (int i = 1; i < value.length() - 1; ++i) {
      final char ch = value.charAt(i);
      if (escaped) {
        switch (ch) {
          case 'n':
            builder.append('\n');
            break;
          case 'r':
            builder.append('\r');
            break;
          case 't':
            builder.append('\t');
            break;
          case '\\':
          case '"':
            builder.append(ch);
            break;
          default:
            throw new IllegalArgumentException("Unsupported escape sequence.");
        }
        escaped = false;
      } else if (ch == '\\') {
        escaped = true;
      } else {
        builder.append(ch);
      }
    }
    if (escaped) {
      throw new IllegalArgumentException("Unfinished escape sequence.");
    }
    return builder.toString();
  }
}
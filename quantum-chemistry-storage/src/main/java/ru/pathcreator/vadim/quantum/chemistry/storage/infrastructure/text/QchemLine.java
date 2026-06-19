/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.storage.infrastructure.text;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Разобранная строка QCHEM: command и набор key=value параметров.
 */
final class QchemLine {

  private final int lineNumber;
  private final String command;
  private final Map<String, String> values;

  private QchemLine(
      final int lineNumber,
      final String command,
      final Map<String, String> values) {
    this.lineNumber = lineNumber;
    this.command = command;
    this.values = values;
  }

  static QchemLine parse(
      final int lineNumber,
      final String line) {
    final String trimmed = line.trim();
    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
      return null;
    }
    final int firstSpace = trimmed.indexOf(' ');
    final String command;
    final String rest;
    if (firstSpace < 0) {
      command = trimmed;
      rest = "";
    } else {
      command = trimmed.substring(0, firstSpace);
      rest = trimmed.substring(firstSpace + 1).trim();
    }
    return new QchemLine(lineNumber, command, QchemLine.parseValues(rest));
  }

  int lineNumber() {
    return this.lineNumber;
  }

  String command() {
    return this.command;
  }

  String required(final String key) {
    final String value = this.values.get(key);
    if (value == null) {
      throw new IllegalArgumentException("Missing required key: " + key);
    }
    return value;
  }

  String optional(final String key) {
    return this.values.get(key);
  }

  Map<String, String> values() {
    return this.values;
  }

  private static Map<String, String> parseValues(final String rest) {
    final LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
    int index = 0;
    while (index < rest.length()) {
      while (index < rest.length() && Character.isWhitespace(rest.charAt(index))) {
        ++index;
      }
      if (index >= rest.length()) {
        break;
      }
      final int keyStart = index;
      while (index < rest.length() && rest.charAt(index) != '=') {
        ++index;
      }
      if (index >= rest.length()) {
        throw new IllegalArgumentException("Expected key=value token.");
      }
      final String key = rest.substring(keyStart, index).trim();
      ++index;
      final String value;
      if (index < rest.length() && rest.charAt(index) == '"') {
        final int valueStart = index;
        ++index;
        boolean escaped = false;
        boolean closed = false;
        while (index < rest.length()) {
          final char ch = rest.charAt(index);
          if (escaped) {
            escaped = false;
          } else if (ch == '\\') {
            escaped = true;
          } else if (ch == '"') {
            ++index;
            closed = true;
            break;
          }
          ++index;
        }
        if (!closed) {
          throw new IllegalArgumentException("Quoted value is not closed.");
        }
        value = rest.substring(valueStart, index);
      } else {
        final int valueStart = index;
        while (index < rest.length() && !Character.isWhitespace(rest.charAt(index))) {
          ++index;
        }
        value = rest.substring(valueStart, index);
      }
      if (key.isEmpty()) {
        throw new IllegalArgumentException("Empty key is not allowed.");
      }
      if (result.put(key, value) != null) {
        throw new IllegalArgumentException("Duplicate key: " + key);
      }
    }
    return Map.copyOf(result);
  }
}
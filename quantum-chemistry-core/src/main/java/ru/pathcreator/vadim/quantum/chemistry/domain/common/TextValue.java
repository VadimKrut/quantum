/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.common;

public final class TextValue {

  private TextValue() {}

  public static String requireText(
      final String value,
      final String subjectName
  ) {
    if (value == null) {
      throw new IllegalArgumentException(subjectName + " must not be null.");
    }
    final String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException(subjectName + " must not be blank.");
    }
    return trimmed;
  }

  public static String optionalText(final String value) {
    if (value == null) {
      return null;
    }
    final String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    return trimmed;
  }
}
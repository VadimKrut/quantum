/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model;

/**
 * Общая проверка текстовых значений внутри drawing-модуля.
 */
final class DrawingText {

  private DrawingText() {
  }

  static String require(
      final String value,
      final String subject
  ) {
    if (value == null) {
      throw new IllegalArgumentException(subject + " must not be null.");
    }
    final String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException(subject + " must not be blank.");
    }
    return trimmed;
  }
}
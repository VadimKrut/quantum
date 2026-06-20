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
 * Вариант значения для enum/list поля inspector.
 */
public final class DrawingFieldOption {

  private final String value;
  private final String label;

  private DrawingFieldOption(
      final String value,
      final String label
  ) {
    this.value = value;
    this.label = label;
  }

  public static DrawingFieldOption of(
      final String value,
      final String label
  ) {
    return new DrawingFieldOption(
        DrawingText.require(
            value,
            "Drawing field option value"
        ),
        DrawingText.require(
            label,
            "Drawing field option label"
        )
    );
  }

  public String value() {
    return this.value;
  }

  public String label() {
    return this.label;
  }
}
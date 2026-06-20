/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model;

import java.util.Objects;

/**
 * Стабильная ссылка panel/form на объект ядра или на объект, который еще вводится вручную.
 */
public final class DrawingCoreReference {

  private final DrawingCoreReferenceKind kind;
  private final String id;

  private DrawingCoreReference(
      final DrawingCoreReferenceKind kind,
      final String id
  ) {
    this.kind = kind;
    this.id = id;
  }

  public static DrawingCoreReference of(
      final DrawingCoreReferenceKind kind,
      final String id
  ) {
    if (kind == null) {
      throw new IllegalArgumentException("Drawing core reference kind must not be null.");
    }
    return new DrawingCoreReference(
        kind,
        DrawingText.require(
            id,
            "Drawing core reference id"
        )
    );
  }

  public DrawingCoreReferenceKind kind() {
    return this.kind;
  }

  public String id() {
    return this.id;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof DrawingCoreReference)) {
      return false;
    }
    final DrawingCoreReference reference = (DrawingCoreReference) other;
    return this.kind == reference.kind
        && Objects.equals(this.id, reference.id);
  }

  public int hashCode() {
    int result = this.kind.hashCode();
    result = 31 * result + this.id.hashCode();
    return result;
  }
}
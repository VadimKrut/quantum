/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.notebook;

import java.util.List;
import java.util.Objects;
import java.util.HashSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;

/** Portable provenance of material received from a separate experiment; paths are hints, never identity. */
public record LaboratorySampleOrigin(
    String transferId,
    LaboratorySampleReference source,
    String sourceCode,
    String sourceTitle,
    String sourcePathHint,
    List<String> analysisIds) {

  public LaboratorySampleOrigin {
    transferId = IdentifierValue.requireIdentifier(transferId, "Sample transfer id");
    if (transferId.length() > 200) {
      throw new IllegalArgumentException("Слишком длинный идентификатор передачи.");
    }
    Objects.requireNonNull(source);
    sourceCode = text(sourceCode, 96);
    sourceTitle = text(sourceTitle, 1000);
    sourcePathHint = text(sourcePathHint, 4096);
    if (sourceCode.isBlank()) {
      throw new IllegalArgumentException("Укажите исходный код образца.");
    }
    analysisIds = List.copyOf(analysisIds);
    if (analysisIds.size() > 10000 || new HashSet<>(analysisIds).size() != analysisIds.size()) {
      throw new IllegalArgumentException("Некорректный список исходных анализов.");
    }
    for (final String id : analysisIds) {
      IdentifierValue.requireIdentifier(id, "Source analysis id");
      if (id.length() > 200) {
        throw new IllegalArgumentException("Слишком длинный идентификатор анализа.");
      }
    }
  }

  private static String text(final String value, final int limit) {
    Objects.requireNonNull(value);
    if (value.length() > limit) {
      throw new IllegalArgumentException("Поле происхождения образца слишком длинное.");
    }
    return value;
  }
}

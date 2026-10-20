/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.inventory;

import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratorySampleOrigin;

/** Physical provenance: direct sample transfer, or a preparation from an earlier ledger batch. */
public record LaboratoryBatchOrigin(
    List<LaboratorySampleOrigin> sourceSamples, String parentBatchId, String recipeId) {
  public LaboratoryBatchOrigin {
    sourceSamples = List.copyOf(sourceSamples);
    if (sourceSamples.size() > 1000)
      throw new IllegalArgumentException("Слишком много исходных образцов в происхождении партии.");
    if (parentBatchId == null) {
      if (sourceSamples.isEmpty() || recipeId != null)
        throw new IllegalArgumentException("Для передачи укажите исходный физический образец.");
    } else {
      parentBatchId = IdentifierValue.requireIdentifier(parentBatchId, "Parent batch");
      recipeId = IdentifierValue.requireIdentifier(recipeId, "Preparation recipe");
      if (!sourceSamples.isEmpty())
        throw new IllegalArgumentException(
            "Раствор наследует происхождение через исходную партию.");
    }
    final Set<String> keys = new HashSet<>();
    for (final var source : sourceSamples)
      if (!keys.add(source.source().token()))
        throw new IllegalArgumentException("Повтор исходного образца партии.");
  }
}

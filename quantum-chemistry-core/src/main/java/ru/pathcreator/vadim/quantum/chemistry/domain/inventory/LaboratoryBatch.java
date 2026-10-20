/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.inventory;

import java.math.BigDecimal;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryQuantityUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/** Price is per one stock unit. Null price is unknown, zero price is explicitly free. */
public record LaboratoryBatch(
    String id,
    String materialId,
    String label,
    LaboratoryQuantityUnit unit,
    BigDecimal purityPercent,
    String concentration,
    String source,
    String location,
    BigDecimal pricePerUnit,
    String currency,
    String sourceExperimentId,
    LaboratoryBatchOrigin origin) {
  public LaboratoryBatch(
      String id,
      String materialId,
      String label,
      LaboratoryQuantityUnit unit,
      BigDecimal purityPercent,
      String concentration,
      String source,
      String location,
      BigDecimal pricePerUnit,
      String currency,
      String sourceExperimentId) {
    this(
        id,
        materialId,
        label,
        unit,
        purityPercent,
        concentration,
        source,
        location,
        pricePerUnit,
        currency,
        sourceExperimentId,
        null);
  }

  public LaboratoryBatch {
    id = IdentifierValue.requireIdentifier(id, "Batch id");
    materialId = IdentifierValue.requireIdentifier(materialId, "Batch material id");
    label = TextValue.requireText(label, "Обозначение партии");
    Objects.requireNonNull(unit);
    concentration = Objects.requireNonNullElse(concentration, "");
    source = Objects.requireNonNullElse(source, "");
    location = Objects.requireNonNullElse(location, "");
    if (purityPercent != null
        && (purityPercent.signum() < 0 || purityPercent.compareTo(new BigDecimal("100")) > 0)) {
      throw new IllegalArgumentException("Чистота должна быть от 0 до 100 %.");
    }
    if (pricePerUnit != null && pricePerUnit.signum() < 0) {
      throw new IllegalArgumentException("Цена не может быть отрицательной.");
    }
    if (pricePerUnit != null) {
      pricePerUnit = pricePerUnit.stripTrailingZeros();
      currency = TextValue.requireText(currency, "Валюта цены").toUpperCase(java.util.Locale.ROOT);
      java.util.Currency.getInstance(currency);
    } else {
      currency = null;
    }
    if (purityPercent != null) {
      purityPercent = purityPercent.stripTrailingZeros();
    }
    if (sourceExperimentId != null) {
      IdentifierValue.requireIdentifier(sourceExperimentId, "Source experiment");
    }
  }
}

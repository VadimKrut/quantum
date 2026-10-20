/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.inventory;

import java.time.Instant;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryQuantity;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

public record LaboratoryStockOperation(
    String id,
    String batchId,
    Instant recordedAt,
    Kind kind,
    LaboratoryQuantity quantity,
    String experimentId,
    String returnsOperationId,
    String reason,
    Valuation valuation) {
  /** Null valuation denotes a legacy operation; a valuation with null price explicitly records unknown cost. */
  public record Valuation(
      java.math.BigDecimal pricePerUnit,
      ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryQuantityUnit unit,
      String currency) {
    public Valuation {
      Objects.requireNonNull(unit);
      if (pricePerUnit == null) {
        if (currency != null) {
          throw new IllegalArgumentException("Валюта без цены операции.");
        }
      } else {
        if (pricePerUnit.signum() < 0) {
          throw new IllegalArgumentException("Цена операции не может быть отрицательной.");
        }
        pricePerUnit = pricePerUnit.stripTrailingZeros();
        currency =
            TextValue.requireText(currency, "Валюта операции").toUpperCase(java.util.Locale.ROOT);
        java.util.Currency.getInstance(currency);
      }
    }
  }

  public LaboratoryStockOperation(
      final String id,
      final String batchId,
      final Instant recordedAt,
      final Kind kind,
      final LaboratoryQuantity quantity,
      final String experimentId,
      final String returnsOperationId,
      final String reason) {
    this(id, batchId, recordedAt, kind, quantity, experimentId, returnsOperationId, reason, null);
  }

  public LaboratoryStockOperation valued(final Valuation value) {
    return new LaboratoryStockOperation(
        this.id,
        this.batchId,
        this.recordedAt,
        this.kind,
        this.quantity,
        this.experimentId,
        this.returnsOperationId,
        this.reason,
        value);
  }

  public enum Kind {
    RECEIPT("Приход", 1),
    USE("Расход в опыт", -1),
    RETURN("Возврат", 1),
    INCREASE("Корректировка +", 1),
    DECREASE("Корректировка −", -1);
    private final String label;
    private final int sign;

    Kind(final String label, final int sign) {
      this.label = label;
      this.sign = sign;
    }

    public int sign() {
      return this.sign;
    }

    public String toString() {
      return this.label;
    }
  }

  public LaboratoryStockOperation {
    id = IdentifierValue.requireIdentifier(id, "Operation id");
    batchId = IdentifierValue.requireIdentifier(batchId, "Operation batch");
    Objects.requireNonNull(recordedAt);
    Objects.requireNonNull(kind);
    Objects.requireNonNull(quantity);
    if (valuation != null && !valuation.unit().dimension().equals(quantity.unit().dimension())) {
      throw new IllegalArgumentException("Единица цены не соответствует количеству операции.");
    }
    if (quantity.value().signum() == 0) {
      throw new IllegalArgumentException("Для операции нужно положительное количество.");
    }
    reason = TextValue.requireText(reason, "Основание операции");
    if (kind == Kind.USE || kind == Kind.RETURN) {
      IdentifierValue.requireIdentifier(experimentId, "Опыт расхода");
    }
    if (experimentId != null) {
      IdentifierValue.requireIdentifier(experimentId, "Experiment id");
    }
    if ((kind == Kind.RETURN) != (returnsOperationId != null)) {
      throw new IllegalArgumentException("Возврат должен ссылаться на исходный расход.");
    }
    if (returnsOperationId != null) {
      IdentifierValue.requireIdentifier(returnsOperationId, "Returned operation");
    }
  }
}

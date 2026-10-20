/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.notebook;

import java.math.BigDecimal;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.SubstanceAmount;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.SubstanceAmountUnit;

/** A measured or planned non-negative quantity; absence is represented separately by null. */
public record LaboratoryQuantity(BigDecimal value, LaboratoryQuantityUnit unit) {
  public LaboratoryQuantity {
    Objects.requireNonNull(value, "Quantity value");
    Objects.requireNonNull(unit, "Quantity unit");
    if (value.signum() < 0) {
      throw new IllegalArgumentException("Количество не может быть отрицательным.");
    }
    value = value.stripTrailingZeros();
  }

  public BigDecimal baseValue() {
    return this.value.multiply(this.unit.factor());
  }

  public LaboratoryQuantity in(final LaboratoryQuantityUnit target) {
    if (!this.unit.dimension().equals(target.dimension())) {
      throw new IllegalArgumentException(
          "Пересчёт массы, объёма и количества вещества требует дополнительных данных.");
    }
    return new LaboratoryQuantity(this.baseValue().divide(target.factor()), target);
  }

  public SubstanceAmount toCoreAmount() {
    if (this.value.signum() == 0 || this.unit.dimension().equals("volume")) {
      throw new IllegalStateException(
          "Расчётному ядру нужно положительное количество вещества или масса.");
    }
    return SubstanceAmount.of(
        this.baseValue().doubleValue(),
        this.unit.dimension().equals("mass") ? SubstanceAmountUnit.GRAM : SubstanceAmountUnit.MOLE);
  }

  public String toString() {
    return this.value.toPlainString() + " " + this.unit;
  }
}

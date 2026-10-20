/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.notebook;

public enum LaboratoryEvidence {
  PLANNED("Предполагается"),
  OBSERVED("Получено / наблюдено"),
  LITERATURE("Литературные данные"),
  SIMULATED("Учебные / модельные данные");
  private final String label;

  LaboratoryEvidence(final String label) {
    this.label = label;
  }

  /** An entered result may come from a publication or simulation; it is not necessarily a physical observation. */
  public boolean reported() {
    return this != PLANNED;
  }

  public String quantityLabel() {
    return switch (this) {
      case PLANNED -> "Количество без подтверждения";
      case OBSERVED -> "Измерено";
      case LITERATURE -> "По литературе";
      case SIMULATED -> "По модели";
    };
  }

  public String toString() {
    return this.label;
  }
}

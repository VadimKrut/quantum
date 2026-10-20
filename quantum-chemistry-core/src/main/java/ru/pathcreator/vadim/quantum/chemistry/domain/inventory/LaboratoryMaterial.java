/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.inventory;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/** A material is distinct from its deliveries and may have no identified molecular structure. */
public record LaboratoryMaterial(String id, String name, Kind kind, Molecule molecule) {
  public enum Kind {
    SUBSTANCE("Вещество"),
    MIXTURE("Смесь"),
    SOLUTION("Раствор"),
    POLYMER("Полимер"),
    UNKNOWN("Не идентифицировано");
    private final String label;

    Kind(final String label) {
      this.label = label;
    }

    public String toString() {
      return this.label;
    }
  }

  public LaboratoryMaterial {
    id = IdentifierValue.requireIdentifier(id, "Material id");
    name = TextValue.requireText(name, "Название материала");
    Objects.requireNonNull(kind);
  }
}

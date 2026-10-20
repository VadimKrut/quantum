/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.math.BigDecimal;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

public interface MoleculeIonMass {
  enum Ion {
    PROTONATED("[M+H]+"),
    DEPROTONATED("[M−H]−"),
    SODIATED("[M+Na]+"),
    POTASSIATED("[M+K]+"),
    DOUBLE_PROTONATED("[M+2H]2+"),
    MOLECULAR_CATION("[M]+•");
    private final String label;

    Ion(final String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return this.label;
    }
  }

  record Result(BigDecimal mz, String composition, String source) {}

  Result calculate(Molecule molecule, Ion ion);
}

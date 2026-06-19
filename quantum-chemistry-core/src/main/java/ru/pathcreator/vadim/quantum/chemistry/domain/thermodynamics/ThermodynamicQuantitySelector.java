/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.thermodynamics;

import ru.pathcreator.vadim.quantum.chemistry.domain.unit.EnergyValue;

enum ThermodynamicQuantitySelector {

  ENTHALPY {

    @Override
    EnergyValue energy(final MolecularThermodynamicData data) {
      if (!data.hasEnthalpy()) {
        throw new IllegalStateException(
            "Thermodynamic enthalpy is required for every participant.");
      }
      return data.enthalpy();
    }
  },
  GIBBS_FREE_ENERGY {

    @Override
    EnergyValue energy(final MolecularThermodynamicData data) {
      if (!data.hasGibbsFreeEnergy()) {
        throw new IllegalStateException(
            "Thermodynamic Gibbs free energy is required for every participant.");
      }
      return data.gibbsFreeEnergy();
    }
  };

  abstract EnergyValue energy(final MolecularThermodynamicData data);
}
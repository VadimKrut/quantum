/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry;

import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/**
 * Единый источник энергии для расчета градиентов и оптимизации геометрии.
 */
public interface GeometryEnergyModel {

  String name();

  String energyUnit();

  String gradientUnit();

  String diagnosticMessage();

  default String derivativeStencil() {
    return "central_finite_difference";
  }

  default int energyEvaluationCount() {
    return 0;
  }

  default int energyCacheHitCount() {
    return 0;
  }

  double energy(final Molecule molecule, final double[] coordinatesAngstrom);

  void gradient(
      final Molecule molecule,
      final double[] coordinatesAngstrom,
      final double finiteDifferenceStepAngstrom,
      final double[] gradient);
}

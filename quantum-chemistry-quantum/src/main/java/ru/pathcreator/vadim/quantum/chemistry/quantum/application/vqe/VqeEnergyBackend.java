/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.application.vqe;

import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationPlan;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeEnergyEvaluation;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

/**
 * Контракт backend/estimator, который превращает параметризованный Quantum IR в оценку энергии.
 */
public interface VqeEnergyBackend {

  VqeEnergyEvaluation evaluate(
      QuantumProgram program, double[] parameters, int shots, ErrorMitigationPlan mitigationPlan);
}

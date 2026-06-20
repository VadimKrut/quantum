/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.backend;

import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationPlan;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalBackendProfile;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalPreflightResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationResult;

/**
 * Контракт классического расчетного backend без привязки к конкретной библиотеке или процессу.
 */
public interface ClassicalBackend {

  public ClassicalBackendProfile profile();

  public ClassicalPreflightResult preflight(final ClassicalCalculationRequest request);

  public ClassicalCalculationPlan plan(final ClassicalCalculationRequest request);

  public ClassicalCalculationResult execute(final ClassicalCalculationPlan plan);
}
/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.integration.decomposition;

import ru.pathcreator.vadim.quantum.domain.gate.Gate;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;

/**
 * Правило разложения gate operation, не привязанное к конкретному внешнему формату.
 */
public interface GateDecompositionRule {

    /**
     * Проверяет, умеет ли правило разложить gate.
     *
     * @param gate gate
     * @return true, если правило применимо
     */
    boolean supports(final Gate gate);

    /**
     * Разлагает gate operation.
     *
     * @param operation исходная операция
     * @return разложение
     */
    GateDecomposition decompose(final GateOperation operation);
}
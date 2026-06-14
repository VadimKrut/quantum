/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.gate;

import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;

/**
 * Расширяемое правило валидации для конкретного гейта.
 */
public interface GateValidationRule {

    /**
     * Проверяет операцию гейта и добавляет ошибки в collector.
     *
     * @param operation проверяемая операция гейта
     * @param collector collector ошибок правила
     */
    void validate(
        GateOperation operation,
        GateValidationRuleErrorCollector collector
    );
}
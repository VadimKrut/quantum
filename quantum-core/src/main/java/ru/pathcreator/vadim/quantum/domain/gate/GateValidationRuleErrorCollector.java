/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.gate;

import ru.pathcreator.vadim.quantum.domain.validation.ValidationErrorCode;

/**
 * Collector ошибок, доступный rule-объектам гейта.
 */
public interface GateValidationRuleErrorCollector {

    /**
     * Добавляет ошибку валидации гейта.
     *
     * @param code код ошибки
     * @param message описание ошибки
     */
    void addError(
        ValidationErrorCode code,
        String message
    );
}
/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.gate;

import java.util.List;

/**
 * Доменное описание квантового гейта без привязки к внешнему формату.
 */
public interface Gate {

    /**
     * Возвращает каноническое имя гейта внутри Quantum IR.
     *
     * @return имя гейта
     */
    String gateName();

    /**
     * Возвращает количество кубитов, к которым применяется гейт.
     *
     * @return количество кубитов
     */
    int arity();

    /**
     * Возвращает количество параметров гейта.
     *
     * @return количество параметров
     */
    int parameterCount();

    /**
     * Возвращает правила валидации операций этого гейта.
     *
     * @return immutable список правил валидации
     */
    List<GateValidationRule> validationRules();
}
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
 * Набор стандартных гейтов, поддерживаемых базовой gate-based моделью.
 */
public enum StandardGate implements Gate {

    H(
        "h",
        1,
        0
    ),
    X(
        "x",
        1,
        0
    ),
    Y(
        "y",
        1,
        0
    ),
    Z(
        "z",
        1,
        0
    ),
    S(
        "s",
        1,
        0
    ),
    SDG(
        "sdg",
        1,
        0
    ),
    T(
        "t",
        1,
        0
    ),
    TDG(
        "tdg",
        1,
        0
    ),
    RX(
        "rx",
        1,
        1
    ),
    RY(
        "ry",
        1,
        1
    ),
    RZ(
        "rz",
        1,
        1
    ),
    CX(
        "cx",
        2,
        0
    ),
    CY(
        "cy",
        2,
        0
    ),
    CZ(
        "cz",
        2,
        0
    ),
    CH(
        "ch",
        2,
        0
    ),
    SWAP(
        "swap",
        2,
        0
    ),
    CCX(
        "ccx",
        3,
        0
    ),
    PHASE(
        "phase",
        1,
        1
    ),
    ID(
        "id",
        1,
        0
    );

    /**
     * Каноническое имя гейта для Quantum IR и будущих writer-слоев.
     */
    private final String gateName;

    /**
     * Количество кубитов, к которым применяется гейт.
     */
    private final int arity;

    /**
     * Количество параметров, необходимых гейту.
     */
    private final int parameterCount;

    /**
     * Правила валидации операций этого гейта.
     */
    private final List<GateValidationRule> validationRules;

    StandardGate(
        final String gateName,
        final int arity,
        final int parameterCount
    ) {
        this.gateName = gateName;
        this.arity = arity;
        this.parameterCount = parameterCount;
        this.validationRules = createValidationRules(arity);
    }

    @Override
    public String gateName() {
        return gateName;
    }

    @Override
    public int arity() {
        return arity;
    }

    @Override
    public int parameterCount() {
        return parameterCount;
    }

    @Override
    public List<GateValidationRule> validationRules() {
        return validationRules;
    }

    private static List<GateValidationRule> createValidationRules(final int arity) {
        if (arity <= 1) {
            return List.of();
        }
        return List.of(DistinctQubitsGateValidationRule.INSTANCE);
    }
}
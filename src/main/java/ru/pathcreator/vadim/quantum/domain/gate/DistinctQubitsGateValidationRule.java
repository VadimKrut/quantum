/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.gate;

import ru.pathcreator.vadim.quantum.domain.bit.Qubit;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationErrorCode;

/**
 * Правило, запрещающее повтор одного и того же кубита внутри multi-qubit gate.
 */
public final class DistinctQubitsGateValidationRule implements GateValidationRule {

    /**
     * Общий immutable экземпляр правила.
     */
    public static final DistinctQubitsGateValidationRule INSTANCE = new DistinctQubitsGateValidationRule();

    private DistinctQubitsGateValidationRule() {
    }

    @Override
    public void validate(
        final GateOperation operation,
        final GateValidationRuleErrorCollector collector
    ) {
        if (operation == null) {
            throw new IllegalArgumentException("Gate operation must not be null.");
        }
        if (collector == null) {
            throw new IllegalArgumentException("Gate validation rule error collector must not be null.");
        }

        for (int i = 0; i < operation.qubitCount(); i++) {
            final Qubit left = operation.qubit(i);
            for (int j = i + 1; j < operation.qubitCount(); j++) {
                if (left.equals(operation.qubit(j))) {
                    collector.addError(
                        ValidationErrorCode.DUPLICATE_QUBIT_IN_GATE_OPERATION,
                        "Gate operation uses the same qubit more than once."
                    );
                    return;
                }
            }
        }
    }
}
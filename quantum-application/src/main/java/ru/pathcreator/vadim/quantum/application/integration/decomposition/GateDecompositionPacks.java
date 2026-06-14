/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.integration.decomposition;

import java.util.ArrayList;
import java.util.List;

import ru.pathcreator.vadim.quantum.domain.gate.Gate;
import ru.pathcreator.vadim.quantum.domain.gate.modifier.GateModifier;
import ru.pathcreator.vadim.quantum.domain.gate.modifier.GateModifierKind;
import ru.pathcreator.vadim.quantum.domain.gate.modifier.ModifiedGate;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;

/**
 * Reusable generic decomposition packs, не привязанные к конкретному внешнему формату.
 */
public final class GateDecompositionPacks {

    private GateDecompositionPacks() {
    }

    /**
     * Возвращает pack для разворачивания repeat-модификаторов.
     *
     * @return registry с repeat decomposition rule
     */
    public static GateDecompositionRegistry repeatModifiers() {
        return GateDecompositionRegistry.of(List.of(new RepeatModifierDecompositionRule()));
    }

    private static final class RepeatModifierDecompositionRule implements GateDecompositionRule {

        @Override
        public boolean supports(final Gate gate) {
            if (!(gate instanceof ModifiedGate modifiedGate)) {
                return false;
            }
            return repeatCount(modifiedGate) > 0;
        }

        @Override
        public GateDecomposition decompose(final GateOperation operation) {
            final ModifiedGate modifiedGate = (ModifiedGate) operation.gate();
            final int count = repeatCount(modifiedGate);
            final ArrayList<GateOperation> operations = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                operations.add(GateOperation.parameterized(
                    modifiedGate.baseGate(),
                    operation.parameters(),
                    operation.qubits()
                ));
            }
            return GateDecomposition.of(operations);
        }

        private static int repeatCount(final ModifiedGate modifiedGate) {
            int count = 1;
            boolean hasRepeat = false;
            for (int i = 0; i < modifiedGate.modifiers().size(); i++) {
                final GateModifier modifier = modifiedGate.modifiers().get(i);
                if (modifier.kind() == GateModifierKind.REPEAT) {
                    count = Math.multiplyExact(
                        count,
                        modifier.integerValue()
                    );
                    hasRepeat = true;
                } else if (modifier.kind() != GateModifierKind.ANNOTATION) {
                    return 0;
                }
            }
            if (!hasRepeat) {
                return 0;
            }
            return count;
        }
    }
}
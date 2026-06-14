/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.openqasm2.mapping;

import java.util.LinkedHashMap;

import ru.pathcreator.vadim.quantum.domain.gate.Gate;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinition;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;

/**
 * Реестр de facto qelib1 gates для адаптера OpenQASM 2.
 */
public final class OpenQasm2QelibGates {

    private static final Gate[] GATES = new Gate[] {
        GateDefinition.of("U", 1, 3),
        GateDefinition.of("u", 1, 3),
        GateDefinition.of("u3", 1, 3),
        GateDefinition.of("u2", 1, 2),
        StandardGate.PHASE,
        GateDefinition.of("p", 1, 1),
        GateDefinition.of("u0", 1, 1),
        StandardGate.ID,
        StandardGate.X,
        StandardGate.Y,
        StandardGate.Z,
        StandardGate.H,
        StandardGate.S,
        StandardGate.SDG,
        StandardGate.T,
        StandardGate.TDG,
        StandardGate.RX,
        StandardGate.RY,
        StandardGate.RZ,
        GateDefinition.of("sx", 1, 0),
        GateDefinition.of("sxdg", 1, 0),
        GateDefinition.of("CX", 2, 0),
        StandardGate.CX,
        StandardGate.CY,
        StandardGate.CZ,
        StandardGate.CH,
        StandardGate.SWAP,
        StandardGate.CCX,
        GateDefinition.of("cswap", 3, 0),
        GateDefinition.of("crx", 2, 1),
        GateDefinition.of("cry", 2, 1),
        GateDefinition.of("crz", 2, 1),
        GateDefinition.of("cu1", 2, 1),
        GateDefinition.of("cp", 2, 1),
        GateDefinition.of("cu3", 2, 3),
        GateDefinition.of("csx", 2, 0),
        GateDefinition.of("cu", 2, 4),
        GateDefinition.of("rxx", 2, 1),
        GateDefinition.of("rzz", 2, 1),
        GateDefinition.of("rccx", 3, 0),
        GateDefinition.of("rc3x", 4, 0),
        GateDefinition.of("c3x", 4, 0),
        GateDefinition.of("c3sqrtx", 4, 0),
        GateDefinition.of("c4x", 5, 0)
    };
    private static final LinkedHashMap<String, Gate> GATES_BY_NAME = createGatesByName();

    private OpenQasm2QelibGates() {
        throw new UnsupportedOperationException("Utility class must not be instantiated.");
    }

    /**
     * Возвращает qelib1 gate по имени OpenQASM 2.
     *
     * @param name имя gate во внешнем формате
     * @return gate или null
     */
    public static Gate byName(final String name) {
        if (name == null) {
            throw new IllegalArgumentException("OpenQASM gate name must not be null.");
        }
        final Gate exactGate = GATES_BY_NAME.get(name);
        if (exactGate != null) {
            return exactGate;
        }
        return GATES_BY_NAME.get(name.toLowerCase());
    }

    /**
     * Проверяет, занято ли имя qelib1 namespace.
     *
     * @param name имя gate во внешнем формате
     * @return true, если имя занято qelib1
     */
    public static boolean containsName(final String name) {
        return byName(name) != null;
    }

    /**
     * Проверяет, является ли gate adapter-level qelib1 intrinsic.
     *
     * @param gate gate Quantum IR
     * @return true, если gate совпадает с qelib1 intrinsic
     */
    public static boolean isQelibGate(final Gate gate) {
        if (gate == null) {
            throw new IllegalArgumentException("Gate must not be null.");
        }
        final Gate qelibGate = byName(gate.gateName());
        return qelibGate != null
            && qelibGate.arity() == gate.arity()
            && qelibGate.parameterCount() == gate.parameterCount();
    }

    private static LinkedHashMap<String, Gate> createGatesByName() {
        final LinkedHashMap<String, Gate> result = new LinkedHashMap<>();
        for (int i = 0; i < GATES.length; i++) {
            result.put(
                GATES[i].gateName(),
                GATES[i]
            );
        }
        result.put(
            "u1",
            StandardGate.PHASE
        );
        return result;
    }
}
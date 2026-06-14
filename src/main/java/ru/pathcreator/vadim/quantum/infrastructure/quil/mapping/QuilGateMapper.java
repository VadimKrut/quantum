/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.quil.mapping;

import ru.pathcreator.vadim.quantum.domain.gate.Gate;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;

/**
 * Mapper между gate Quantum IR и instruction names Quil.
 */
public final class QuilGateMapper {

    private QuilGateMapper() {
    }

    public static String toQuilName(final Gate gate) {
        if (gate == null) {
            throw new IllegalArgumentException("Gate must not be null.");
        }
        if (isGate(gate, StandardGate.H)) {
            return "H";
        }
        if (isGate(gate, StandardGate.X)) {
            return "X";
        }
        if (isGate(gate, StandardGate.Y)) {
            return "Y";
        }
        if (isGate(gate, StandardGate.Z)) {
            return "Z";
        }
        if (isGate(gate, StandardGate.S)) {
            return "S";
        }
        if (isGate(gate, StandardGate.T)) {
            return "T";
        }
        if (isGate(gate, StandardGate.RX)) {
            return "RX";
        }
        if (isGate(gate, StandardGate.RY)) {
            return "RY";
        }
        if (isGate(gate, StandardGate.RZ)) {
            return "RZ";
        }
        if (isGate(gate, StandardGate.PHASE)) {
            return "PHASE";
        }
        if (isGate(gate, StandardGate.CX)) {
            return "CNOT";
        }
        if (isGate(gate, StandardGate.CZ)) {
            return "CZ";
        }
        if (isGate(gate, StandardGate.CPHASE)) {
            return "CPHASE";
        }
        if (isGate(gate, StandardGate.SWAP)) {
            return "SWAP";
        }
        if (isGate(gate, StandardGate.CCX)) {
            return "CCNOT";
        }
        if (isGate(gate, StandardGate.ID)) {
            return "I";
        }
        return null;
    }

    public static Gate fromQuilName(final String name) {
        if (name == null) {
            throw new IllegalArgumentException("Quil gate name must not be null.");
        }
        final String normalized = name.toUpperCase();
        return switch (normalized) {
            case "H", "HADAMARD" -> StandardGate.H;
            case "X" -> StandardGate.X;
            case "Y" -> StandardGate.Y;
            case "Z" -> StandardGate.Z;
            case "S" -> StandardGate.S;
            case "T" -> StandardGate.T;
            case "RX" -> StandardGate.RX;
            case "RY" -> StandardGate.RY;
            case "RZ" -> StandardGate.RZ;
            case "PHASE" -> StandardGate.PHASE;
            case "CNOT" -> StandardGate.CX;
            case "CZ" -> StandardGate.CZ;
            case "CPHASE" -> StandardGate.CPHASE;
            case "SWAP" -> StandardGate.SWAP;
            case "CCNOT" -> StandardGate.CCX;
            case "I" -> StandardGate.ID;
            default -> null;
        };
    }

    private static boolean isGate(
        final Gate gate,
        final StandardGate standardGate
    ) {
        return gate.arity() == standardGate.arity()
            && gate.parameterCount() == standardGate.parameterCount()
            && standardGate.gateName().equals(gate.gateName());
    }
}
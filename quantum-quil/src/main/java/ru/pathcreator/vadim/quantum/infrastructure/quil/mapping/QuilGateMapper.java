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
        if (isGate(gate, StandardGate.U)) {
            return "U";
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
        if (isGate(gate, StandardGate.SDG)) {
            return "SDG";
        }
        if (isGate(gate, StandardGate.T)) {
            return "T";
        }
        if (isGate(gate, StandardGate.TDG)) {
            return "TDG";
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
            return "P";
        }
        if (isGate(gate, StandardGate.CX)) {
            return "CNOT";
        }
        if (isGate(gate, StandardGate.CY)) {
            return "CY";
        }
        if (isGate(gate, StandardGate.CZ)) {
            return "CZ";
        }
        if (isGate(gate, StandardGate.CH)) {
            return "CH";
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
        if (
            "U".equalsIgnoreCase(name)
            || "U3".equalsIgnoreCase(name)
        ) {
            return StandardGate.U;
        }
        if (
            "H".equalsIgnoreCase(name)
            || "HADAMARD".equalsIgnoreCase(name)
        ) {
            return StandardGate.H;
        }
        if ("X".equalsIgnoreCase(name)) {
            return StandardGate.X;
        }
        if ("Y".equalsIgnoreCase(name)) {
            return StandardGate.Y;
        }
        if ("Z".equalsIgnoreCase(name)) {
            return StandardGate.Z;
        }
        if ("S".equalsIgnoreCase(name)) {
            return StandardGate.S;
        }
        if ("SDG".equalsIgnoreCase(name)) {
            return StandardGate.SDG;
        }
        if ("T".equalsIgnoreCase(name)) {
            return StandardGate.T;
        }
        if ("TDG".equalsIgnoreCase(name)) {
            return StandardGate.TDG;
        }
        if ("RX".equalsIgnoreCase(name)) {
            return StandardGate.RX;
        }
        if ("RY".equalsIgnoreCase(name)) {
            return StandardGate.RY;
        }
        if ("RZ".equalsIgnoreCase(name)) {
            return StandardGate.RZ;
        }
        if (
            "P".equalsIgnoreCase(name)
            || "PHASE".equalsIgnoreCase(name)
        ) {
            return StandardGate.PHASE;
        }
        if ("CNOT".equalsIgnoreCase(name)) {
            return StandardGate.CX;
        }
        if ("CY".equalsIgnoreCase(name)) {
            return StandardGate.CY;
        }
        if ("CZ".equalsIgnoreCase(name)) {
            return StandardGate.CZ;
        }
        if ("CH".equalsIgnoreCase(name)) {
            return StandardGate.CH;
        }
        if ("CPHASE".equalsIgnoreCase(name)) {
            return StandardGate.CPHASE;
        }
        if ("SWAP".equalsIgnoreCase(name)) {
            return StandardGate.SWAP;
        }
        if ("CCNOT".equalsIgnoreCase(name)) {
            return StandardGate.CCX;
        }
        if ("I".equalsIgnoreCase(name)) {
            return StandardGate.ID;
        }
        return null;
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
/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.workspace.builder;

import java.util.ArrayList;
import java.util.List;

import ru.pathcreator.vadim.quantum.desktop.workspace.DesktopIrOperationSpec;

/**
 * Преобразует клики по визуальной сетке qubit-ов в desktop native IR operation specs.
 */
public final class DesktopGridPlacementService {

    public DesktopGridPlacementResult place(
        final String gate,
        final String clickedQubit,
        final String secondaryQubit,
        final String tertiaryQubit,
        final String classicalBit,
        final double angle,
        final List<String> pendingQubits
    ) {
        if (
            isSingleClickGate(gate)
            || "MEASURE".equals(gate)
            || "BARRIER".equals(gate)
        ) {
            return completed(new DesktopIrOperationSpec(
                gate,
                clickedQubit,
                secondaryQubit,
                tertiaryQubit,
                classicalBit,
                angle
            ));
        }
        final ArrayList<String> nextPending = new ArrayList<>(pendingQubits);
        nextPending.add(clickedQubit);
        final int required = "CCX".equals(gate) ? 3 : 2;
        if (nextPending.size() < required) {
            return new DesktopGridPlacementResult(
                nextPending,
                null,
                "Selected " + nextPending.size() + "/" + required + " qubits for " + gate + "."
            );
        }
        return completed(new DesktopIrOperationSpec(
            gate,
            nextPending.get(0),
            nextPending.get(1),
            "CCX".equals(gate) ? nextPending.get(2) : tertiaryQubit,
            classicalBit,
            angle
        ));
    }

    private static DesktopGridPlacementResult completed(final DesktopIrOperationSpec operation) {
        return new DesktopGridPlacementResult(
            List.of(),
            operation,
            "Select a gate, then click a qubit lane to place it."
        );
    }

    private static boolean isSingleClickGate(final String gate) {
        return switch (gate) {
            case "H", "X", "Y", "Z", "S", "T", "RX", "RY", "RZ", "PHASE", "RESET" -> true;
            default -> false;
        };
    }
}
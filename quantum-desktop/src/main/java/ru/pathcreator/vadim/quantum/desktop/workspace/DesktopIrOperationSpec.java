/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.workspace;

/**
 * One operation placed by a user in the native IR workspace.
 */
public record DesktopIrOperationSpec(
    String gate,
    String primaryQubit,
    String secondaryQubit,
    String tertiaryQubit,
    String classicalBit,
    double angle
) {

    public DesktopIrOperationSpec {
        if (
            gate == null
            || gate.isBlank()
        ) {
            throw new IllegalArgumentException("Desktop IR operation gate must not be blank.");
        }
    }

    public String label() {
        return switch (gate) {
            case "CX", "CY", "CZ", "CH", "SWAP" -> gate + " " + primaryQubit + ", " + secondaryQubit;
            case "CCX" -> gate + " " + primaryQubit + ", " + secondaryQubit + ", " + tertiaryQubit;
            case "RX", "RY", "RZ", "PHASE" -> gate + "(" + angle + ") " + primaryQubit;
            case "MEASURE" -> gate + " " + primaryQubit + " -> " + classicalBit;
            case "BARRIER" -> gate + " " + primaryQubit + ", " + secondaryQubit;
            default -> gate + " " + primaryQubit;
        };
    }
}

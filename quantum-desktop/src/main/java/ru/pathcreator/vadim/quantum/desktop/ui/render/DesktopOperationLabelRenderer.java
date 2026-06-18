/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.render;

import ru.pathcreator.vadim.quantum.desktop.workspace.DesktopIrOperationSpec;

/**
 * Формирует короткие подписи операций для inspector и circuit flow.
 */
public final class DesktopOperationLabelRenderer {

    public String renderSummary(final DesktopIrOperationSpec operation) {
        final StringBuilder builder = new StringBuilder(operation.gate());
        builder.append(" ");
        builder.append(operation.primaryQubit());
        if (usesSecondaryQubit(operation.gate())) {
            builder.append(", ");
            builder.append(operation.secondaryQubit());
        }
        if ("CCX".equals(operation.gate())) {
            builder.append(", ");
            builder.append(operation.tertiaryQubit());
        }
        if ("MEASURE".equals(operation.gate())) {
            builder.append(" -> ");
            builder.append(operation.classicalBit());
        }
        return builder.toString();
    }

    private static boolean usesSecondaryQubit(final String gate) {
        return switch (gate) {
            case "CX", "CY", "CZ", "CH", "SWAP", "CCX", "BARRIER" -> true;
            default -> false;
        };
    }
}
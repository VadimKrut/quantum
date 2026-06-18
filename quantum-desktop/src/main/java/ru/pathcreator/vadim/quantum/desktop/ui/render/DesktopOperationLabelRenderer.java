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
import ru.pathcreator.vadim.quantum.desktop.workspace.DesktopClassicalExpressionRenderer;

/**
 * Формирует короткие подписи операций для inspector и circuit flow.
 */
public final class DesktopOperationLabelRenderer {

    public String renderSummary(final DesktopIrOperationSpec operation) {
        if (operation.gate().startsWith("IR:")) {
            return operation.labelName();
        }
        if ("LABEL".equals(operation.gate())) {
            return "LABEL " + operation.labelName();
        }
        if ("BRANCH".equals(operation.gate())) {
            return "BRANCH -> " + operation.labelName();
        }
        if ("TIMING_BOX".equals(operation.gate())) {
            return "TIMING_BOX [" + operation.durationValue() + operation.durationUnit().toLowerCase()
                + "] body " + operation.bodyOperations().size();
        }
        if ("ASSIGN".equals(operation.gate())) {
            return "ASSIGN " + operation.classicalBit() + " = " + Math.round(operation.angle());
        }
        if ("DECLARE".equals(operation.gate())) {
            return "DECLARE " + operation.labelName() + " = " + Math.round(operation.angle());
        }
        if ("ARRAY".equals(operation.gate())) {
            return "ARRAY " + operation.labelName() + "[" + Math.max(1L, Math.round(operation.angle())) + "]";
        }
        if ("CALL".equals(operation.gate())) {
            return "CALL " + operation.labelName();
        }
        if ("IF_X".equals(operation.gate())) {
            return "IF " + predicateLabel(operation) + " THEN X " + operation.primaryQubit();
        }
        if ("CTRL_X".equals(operation.gate())) {
            return "CTRL " + operation.classicalBit() + " == " + Math.round(operation.angle()) + " THEN X " + operation.primaryQubit();
        }
        if ("BLOCK".equals(operation.gate())) {
            return "BLOCK body " + operation.bodyOperations().size();
        }
        if ("IF_BLOCK".equals(operation.gate())) {
            return "IF " + predicateLabel(operation)
                + " THEN body " + operation.bodyOperations().size()
                + " ELSE body " + operation.elseOperations().size();
        }
        if ("FOR".equals(operation.gate())) {
            return "FOR " + operation.labelName() + " 0.." + Math.max(0L, Math.round(operation.angle()))
                + " body " + operation.bodyOperations().size();
        }
        if ("SYM_FOR".equals(operation.gate())) {
            return "SYM_FOR " + operation.labelName() + " 0..expr(" + Math.max(0L, Math.round(operation.angle())) + ")"
                + " body " + operation.bodyOperations().size();
        }
        if ("WHILE".equals(operation.gate())) {
            return "WHILE " + predicateLabel(operation) + " body " + operation.bodyOperations().size();
        }
        if (
            "HALT".equals(operation.gate())
            || "WAIT".equals(operation.gate())
        ) {
            return operation.gate();
        }
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
        if ("DELAY".equals(operation.gate())) {
            builder.append(" [");
            builder.append(operation.durationValue());
            builder.append(operation.durationUnit().toLowerCase());
            builder.append("]");
        }
        return builder.toString();
    }

    private static boolean usesSecondaryQubit(final String gate) {
        return switch (gate) {
            case "CX", "CY", "CZ", "CPHASE", "CH", "SWAP", "CCX", "BARRIER", "DELAY" -> true;
            default -> false;
        };
    }

    private static String predicateLabel(final DesktopIrOperationSpec operation) {
        if (operation.predicate() != null) {
            return DesktopClassicalExpressionRenderer.predicateText(operation.predicate());
        }
        return operation.classicalBit() + " == " + Math.round(operation.angle());
    }
}
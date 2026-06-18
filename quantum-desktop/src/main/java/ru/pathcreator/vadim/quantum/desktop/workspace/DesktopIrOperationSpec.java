/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.workspace;

import java.util.List;

import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;

/**
 * Одна операция, которую пользователь разместил в native IR workspace.
 */
public record DesktopIrOperationSpec(
    String gate,
    String primaryQubit,
    String secondaryQubit,
    String tertiaryQubit,
    String classicalBit,
    double angle,
    double secondAngle,
    double thirdAngle,
    double durationValue,
    String durationUnit,
    String labelName,
    ClassicalPredicate predicate,
    List<DesktopIrOperationSpec> bodyOperations,
    List<DesktopIrOperationSpec> elseOperations
) {

    public DesktopIrOperationSpec(
        final String gate,
        final String primaryQubit,
        final String secondaryQubit,
        final String tertiaryQubit,
        final String classicalBit,
        final double angle
    ) {
        this(
            gate,
            primaryQubit,
            secondaryQubit,
            tertiaryQubit,
            classicalBit,
            angle,
            0.0,
            0.0,
            20.0,
            "NS",
            "entry",
            null,
            List.of(),
            List.of()
        );
    }

    public DesktopIrOperationSpec(
        final String gate,
        final String primaryQubit,
        final String secondaryQubit,
        final String tertiaryQubit,
        final String classicalBit,
        final double angle,
        final double secondAngle,
        final double thirdAngle,
        final double durationValue,
        final String durationUnit,
        final String labelName
    ) {
        this(
            gate,
            primaryQubit,
            secondaryQubit,
            tertiaryQubit,
            classicalBit,
            angle,
            secondAngle,
            thirdAngle,
            durationValue,
            durationUnit,
            labelName,
            null,
            List.of(),
            List.of()
        );
    }

    public DesktopIrOperationSpec(
        final String gate,
        final String primaryQubit,
        final String secondaryQubit,
        final String tertiaryQubit,
        final String classicalBit,
        final double angle,
        final double secondAngle,
        final double thirdAngle,
        final double durationValue,
        final String durationUnit,
        final String labelName,
        final List<DesktopIrOperationSpec> bodyOperations,
        final List<DesktopIrOperationSpec> elseOperations
    ) {
        this(
            gate,
            primaryQubit,
            secondaryQubit,
            tertiaryQubit,
            classicalBit,
            angle,
            secondAngle,
            thirdAngle,
            durationValue,
            durationUnit,
            labelName,
            null,
            bodyOperations,
            elseOperations
        );
    }

    public DesktopIrOperationSpec {
        if (
            gate == null
            || gate.isBlank()
        ) {
            throw new IllegalArgumentException("Desktop IR operation gate must not be blank.");
        }
        if (
            durationUnit == null
            || durationUnit.isBlank()
        ) {
            durationUnit = "NS";
        }
        if (
            labelName == null
            || labelName.isBlank()
        ) {
            labelName = "entry";
        }
        bodyOperations = bodyOperations == null
            ? List.of()
            : List.copyOf(bodyOperations);
        elseOperations = elseOperations == null
            ? List.of()
            : List.copyOf(elseOperations);
    }

    public String label() {
        if (gate.startsWith("CUSTOM:")) {
            return gate.substring("CUSTOM:".length()) + " (custom operation)";
        }
        if (gate.startsWith("IR:")) {
            return labelName + " (read-only IR)";
        }
        return switch (gate) {
            case "CX", "CY", "CZ", "CH", "SWAP" -> gate + " " + primaryQubit + ", " + secondaryQubit;
            case "CPHASE" -> gate + "(" + angle + ") " + primaryQubit + ", " + secondaryQubit;
            case "CCX" -> gate + " " + primaryQubit + ", " + secondaryQubit + ", " + tertiaryQubit;
            case "RX", "RY", "RZ", "PHASE" -> gate + "(" + angle + ") " + primaryQubit;
            case "U" -> gate + "(" + angle + ", " + secondAngle + ", " + thirdAngle + ") " + primaryQubit;
            case "MEASURE" -> gate + " " + primaryQubit + " -> " + classicalBit;
            case "BARRIER" -> gate + " " + primaryQubit + ", " + secondaryQubit;
            case "DELAY" -> gate + "[" + durationValue + durationUnit.toLowerCase() + "] " + primaryQubit + ", " + secondaryQubit;
            case "LABEL" -> gate + " " + labelName;
            case "BRANCH" -> gate + " -> " + labelName;
            case "TIMING_BOX" -> gate + "[" + durationValue + durationUnit.toLowerCase() + "] body "
                + bodyOperations.size();
            case "ASSIGN" -> gate + " " + classicalBit + " = " + Math.round(angle);
            case "DECLARE" -> gate + " " + labelName + " = " + Math.round(angle);
            case "ARRAY" -> gate + " " + labelName + "[" + Math.max(1L, Math.round(angle)) + "]";
            case "CALL" -> gate + " " + labelName;
            case "IF_X" -> gate + " if " + predicateLabel(this) + " then X " + primaryQubit;
            case "CTRL_X" -> gate + " if register == " + Math.round(angle) + " then X " + primaryQubit;
            case "BLOCK" -> gate + " body " + bodyOperations.size();
            case "IF_BLOCK" -> gate + " if " + predicateLabel(this)
                + " then " + bodyOperations.size() + " else " + elseOperations.size();
            case "FOR" -> gate + " " + labelName + " 0.." + Math.max(0L, Math.round(angle))
                + " body " + bodyOperations.size();
            case "SYM_FOR" -> gate + " " + labelName + " 0..expr(" + Math.max(0L, Math.round(angle)) + ")"
                + " body " + bodyOperations.size();
            case "WHILE" -> gate + " " + predicateLabel(this) + " body " + bodyOperations.size();
            case "HALT", "WAIT" -> gate;
            default -> gate + " " + primaryQubit;
        };
    }

    private static String predicateLabel(final DesktopIrOperationSpec operation) {
        if (operation.predicate() != null) {
            return DesktopClassicalExpressionRenderer.predicateText(operation.predicate());
        }
        return operation.classicalBit() + " == " + Math.round(operation.angle());
    }
}
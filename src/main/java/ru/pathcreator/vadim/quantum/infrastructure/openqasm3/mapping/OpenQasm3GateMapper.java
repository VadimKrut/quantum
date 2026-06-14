/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.openqasm3.mapping;

import ru.pathcreator.vadim.quantum.domain.gate.Gate;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinition;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpressionKind;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.gate.modifier.GateModifier;
import ru.pathcreator.vadim.quantum.domain.gate.modifier.GateModifierKind;
import ru.pathcreator.vadim.quantum.domain.gate.modifier.ModifiedGate;

/**
 * Mapper между gate-именами Quantum IR и стандартной библиотекой OpenQASM 3.
 */
public final class OpenQasm3GateMapper {

    private static final StandardGate[] STANDARD_GATES = StandardGate.values();

    private OpenQasm3GateMapper() {
    }

    /**
     * Проверяет, поддерживается ли gate при export в OpenQASM 3.
     *
     * @param gate gate Quantum IR
     * @return true, если gate поддерживается
     */
    public static boolean isExportSupported(final Gate gate) {
        return toOpenQasmName(gate) != null;
    }

    /**
     * Возвращает имя gate в OpenQASM 3.
     *
     * @param gate gate Quantum IR
     * @return имя gate в OpenQASM 3 или null
     */
    public static String toOpenQasmName(final Gate gate) {
        if (gate == null) {
            throw new IllegalArgumentException("Gate must not be null.");
        }
        if (gate instanceof ModifiedGate modifiedGate) {
            return modifiedGateToOpenQasmName(modifiedGate);
        }
        if (
            gate.arity() == StandardGate.PHASE.arity()
            && gate.parameterCount() == StandardGate.PHASE.parameterCount()
            && StandardGate.PHASE.gateName().equals(gate.gateName())
        ) {
            return "phase";
        }
        if (
            gate.arity() == StandardGate.CPHASE.arity()
            && gate.parameterCount() == StandardGate.CPHASE.parameterCount()
            && StandardGate.CPHASE.gateName().equals(gate.gateName())
        ) {
            return "cphase";
        }
        if (
            gate instanceof GateDefinition definition
            && OpenQasm3StdGates.isStdGate(definition)
        ) {
            return definition.gateName();
        }
        for (int i = 0; i < STANDARD_GATES.length; i++) {
            final StandardGate standardGate = STANDARD_GATES[i];
            if (
                standardGate.arity() == gate.arity()
                && standardGate.parameterCount() == gate.parameterCount()
                && standardGate.gateName().equals(gate.gateName())
            ) {
                return standardGate.gateName();
            }
        }
        return null;
    }

    private static String modifiedGateToOpenQasmName(final ModifiedGate gate) {
        final String symbolicName = symbolicModifiedGateToOpenQasmName(gate);
        if (symbolicName != null) {
            return symbolicName;
        }
        Gate baseGate = gate.baseGate();
        int quantumControlCount = 0;
        boolean inverse = false;
        double power = 1.0;
        for (int i = 0; i < gate.modifiers().size(); i++) {
            final GateModifier modifier = gate.modifiers().get(i);
            if (modifier.kind() == GateModifierKind.ANNOTATION) {
                continue;
            }
            if (modifier.kind() == GateModifierKind.INVERSE) {
                inverse = !inverse;
            } else if (modifier.kind() == GateModifierKind.CONTROLLED) {
                quantumControlCount += modifier.integerValue();
            } else if (modifier.kind() == GateModifierKind.POWER) {
                if (modifier.hasPowerExpression()) {
                    return null;
                }
                power *= modifier.doubleValue();
            } else {
                return null;
            }
        }
        if (baseGate instanceof ModifiedGate nestedModifiedGate) {
            final String nestedName = modifiedGateToOpenQasmName(nestedModifiedGate);
            if (nestedName == null) {
                return null;
            }
            final Gate nestedGate = fromOpenQasmName(nestedName);
            if (!(nestedGate instanceof StandardGate nestedStandardGate)) {
                return null;
            }
            baseGate = nestedStandardGate;
        }
        if (quantumControlCount == 0) {
            final String powerGateName = powerGateName(
                baseGate,
                power,
                inverse
            );
            if (powerGateName != null) {
                return powerGateName;
            }
            if (power != 1.0) {
                return null;
            }
            final String inverseGateName = inverseGateName(baseGate, inverse);
            if (inverseGateName != null) {
                return inverseGateName;
            }
            return explicitModifiedGateToOpenQasmName(gate);
        }
        if (power != 1.0) {
            return explicitModifiedGateToOpenQasmName(gate);
        }
        final String controlledGateName = controlledGateName(
            baseGate,
            quantumControlCount
        );
        if (controlledGateName != null) {
            return controlledGateName;
        }
        return explicitModifiedGateToOpenQasmName(gate);
    }

    private static String symbolicModifiedGateToOpenQasmName(final ModifiedGate gate) {
        boolean hasSymbolicPower = false;
        for (int i = 0; i < gate.modifiers().size(); i++) {
            final GateModifier modifier = gate.modifiers().get(i);
            if (
                modifier.kind() == GateModifierKind.POWER
                && modifier.hasPowerExpression()
            ) {
                hasSymbolicPower = true;
                break;
            }
        }
        if (!hasSymbolicPower) {
            return null;
        }
        final String baseName = toOpenQasmName(gate.baseGate());
        if (baseName == null) {
            return null;
        }
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < gate.modifiers().size(); i++) {
            final String text = modifierToOpenQasmText(gate.modifiers().get(i));
            if (text == null) {
                return null;
            }
            builder.append(text)
                .append(" @ ");
        }
        builder.append(baseName);
        return builder.toString();
    }

    private static String explicitModifiedGateToOpenQasmName(final ModifiedGate gate) {
        final String baseName = toOpenQasmName(gate.baseGate());
        if (baseName == null) {
            return null;
        }
        final StringBuilder builder = new StringBuilder();
        boolean hasModifier = false;
        for (int i = 0; i < gate.modifiers().size(); i++) {
            final GateModifier modifier = gate.modifiers().get(i);
            if (modifier.kind() == GateModifierKind.ANNOTATION) {
                continue;
            }
            final String text = modifierToOpenQasmText(modifier);
            if (text == null) {
                return null;
            }
            builder.append(text)
                .append(" @ ");
            hasModifier = true;
        }
        if (!hasModifier) {
            return null;
        }
        builder.append(baseName);
        return builder.toString();
    }

    private static String modifierToOpenQasmText(final GateModifier modifier) {
        if (modifier.kind() == GateModifierKind.INVERSE) {
            return "inv";
        }
        if (modifier.kind() == GateModifierKind.CONTROLLED) {
            if (modifier.integerValue() == 1) {
                return "ctrl";
            }
            return "ctrl(" + modifier.integerValue() + ")";
        }
        if (modifier.kind() == GateModifierKind.POWER) {
            if (modifier.hasPowerExpression()) {
                return "pow(" + formatParameter(modifier.powerExpression()) + ")";
            }
            return "pow(" + formatNumber(modifier.doubleValue()) + ")";
        }
        return null;
    }

    private static String formatParameter(final ParameterExpression parameter) {
        if (parameter.kind() == ParameterExpressionKind.NUMERIC) {
            return formatNumber(parameter.numericValue());
        }
        if (
            parameter.kind() == ParameterExpressionKind.NAMED
            || parameter.kind() == ParameterExpressionKind.KNOWN_CONSTANT
        ) {
            return parameter.name();
        }
        if (parameter.kind() == ParameterExpressionKind.UNARY) {
            return parameter.unaryOperator().symbol() + parenthesizedParameter(parameter.left());
        }
        return parenthesizedParameter(parameter.left())
            + parameter.binaryOperator().symbol()
            + parenthesizedParameter(parameter.right());
    }

    private static String parenthesizedParameter(final ParameterExpression parameter) {
        if (
            parameter.kind() == ParameterExpressionKind.NUMERIC
            || parameter.kind() == ParameterExpressionKind.NAMED
            || parameter.kind() == ParameterExpressionKind.KNOWN_CONSTANT
        ) {
            return formatParameter(parameter);
        }
        return "(" + formatParameter(parameter) + ")";
    }

    private static String formatNumber(final double value) {
        if (value == Math.rint(value)) {
            return Long.toString(Math.round(value));
        }
        return Double.toString(value);
    }

    private static String powerGateName(
        final Gate gate,
        final double power,
        final boolean inverse
    ) {
        if (
            isGate(gate, StandardGate.X)
            && Double.compare(
                power,
                0.5
            ) == 0
        ) {
            if (inverse) {
                return "sxdg";
            }
            return "sx";
        }
        if (
            isGate(gate, StandardGate.X)
            && Double.compare(
                power,
                -0.5
            ) == 0
        ) {
            if (inverse) {
                return "sx";
            }
            return "sxdg";
        }
        return null;
    }

    private static String inverseGateName(
        final Gate gate,
        final boolean inverse
    ) {
        if (!inverse) {
            return toOpenQasmName(gate);
        }
        if (isGate(gate, StandardGate.S)) {
            return StandardGate.SDG.gateName();
        }
        if (isGate(gate, StandardGate.SDG)) {
            return StandardGate.S.gateName();
        }
        if (isGate(gate, StandardGate.T)) {
            return StandardGate.TDG.gateName();
        }
        if (isGate(gate, StandardGate.TDG)) {
            return StandardGate.T.gateName();
        }
        if (
            isGate(gate, StandardGate.H)
            || isGate(gate, StandardGate.X)
            || isGate(gate, StandardGate.Y)
            || isGate(gate, StandardGate.Z)
            || isGate(gate, StandardGate.CX)
            || isGate(gate, StandardGate.CY)
            || isGate(gate, StandardGate.CZ)
            || isGate(gate, StandardGate.CH)
            || isGate(gate, StandardGate.SWAP)
            || isGate(gate, StandardGate.CCX)
            || isGate(gate, StandardGate.ID)
        ) {
            return toOpenQasmName(gate);
        }
        return null;
    }

    private static String controlledGateName(
        final Gate gate,
        final int quantumControlCount
    ) {
        if (
            isGate(gate, StandardGate.X)
            && quantumControlCount == 1
        ) {
            return StandardGate.CX.gateName();
        }
        if (
            isGate(gate, StandardGate.X)
            && quantumControlCount == 2
        ) {
            return StandardGate.CCX.gateName();
        }
        if (
            quantumControlCount == 1
            && isSelfInverseSingleQubitGate(gate)
        ) {
            if (isGate(gate, StandardGate.Y)) {
                return StandardGate.CY.gateName();
            }
            if (isGate(gate, StandardGate.Z)) {
                return StandardGate.CZ.gateName();
            }
            if (isGate(gate, StandardGate.H)) {
                return StandardGate.CH.gateName();
            }
        }
        return null;
    }

    private static boolean isSelfInverseSingleQubitGate(final Gate gate) {
        return isGate(
            gate,
            StandardGate.H
        )
            || isGate(
                gate,
                StandardGate.Y
            )
            || isGate(
                gate,
                StandardGate.Z
            );
    }

    private static boolean isGate(
        final Gate gate,
        final StandardGate standardGate
    ) {
        return gate.arity() == standardGate.arity()
            && gate.parameterCount() == standardGate.parameterCount()
            && standardGate.gateName().equals(gate.gateName());
    }

    /**
     * Возвращает gate Quantum IR по имени OpenQASM 3.
     *
     * @param gateName имя gate во внешнем формате
     * @return gate Quantum IR или null
     */
    public static Gate fromOpenQasmName(final String gateName) {
        if (gateName == null) {
            throw new IllegalArgumentException("OpenQASM gate name must not be null.");
        }
        final String normalizedGateName = gateName.toLowerCase();
        if (
            "U".equals(gateName)
            || "u".equals(normalizedGateName)
            || "u3".equals(normalizedGateName)
        ) {
            return StandardGate.U;
        }
        final Gate standardGate = OpenQasm3StdGates.byName(gateName);
        if (standardGate != null) {
            return standardGate;
        }
        if (
            "u1".equals(normalizedGateName)
            || "p".equals(normalizedGateName)
            || "phase".equals(normalizedGateName)
        ) {
            return StandardGate.PHASE;
        }
        if (
            "cp".equals(normalizedGateName)
            || "cphase".equals(normalizedGateName)
        ) {
            return StandardGate.CPHASE;
        }
        if ("cx".equals(normalizedGateName)) {
            return StandardGate.CX;
        }
        for (int i = 0; i < STANDARD_GATES.length; i++) {
            final StandardGate gate = STANDARD_GATES[i];
            if (gate.gateName().equals(normalizedGateName)) {
                return gate;
            }
        }
        return null;
    }
}
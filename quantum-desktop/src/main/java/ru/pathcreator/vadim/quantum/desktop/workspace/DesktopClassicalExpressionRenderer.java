/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.workspace;

import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;

/**
 * Рендерит деревья classical expression/predicate для desktop-подсказок и Java DSL preview.
 */
public final class DesktopClassicalExpressionRenderer {

    private DesktopClassicalExpressionRenderer() {
    }

    public static String predicateText(final ClassicalPredicate predicate) {
        if (predicate == null) {
            return "";
        }
        return switch (predicate.kind()) {
            case COMPARISON -> expressionText(predicate.leftExpression())
                + " " + comparisonText(predicate.comparisonOperator().name()) + " "
                + expressionText(predicate.rightExpression());
            case BOOLEAN -> "(" + predicateText(predicate.leftPredicate()) + ") "
                + predicate.booleanOperator().name().toLowerCase() + " ("
                + predicateText(predicate.rightPredicate()) + ")";
            case NOT -> "not (" + predicateText(predicate.leftPredicate()) + ")";
        };
    }

    public static String predicateJava(final ClassicalPredicate predicate) {
        if (predicate == null) {
            throw new IllegalArgumentException("Classical predicate must not be null.");
        }
        return switch (predicate.kind()) {
            case COMPARISON -> "ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate.compare("
                + expressionJava(predicate.leftExpression()) + ", "
                + "ru.pathcreator.vadim.quantum.domain.classical.ClassicalComparisonOperator."
                + predicate.comparisonOperator().name() + ", "
                + expressionJava(predicate.rightExpression()) + ")";
            case BOOLEAN -> "ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate."
                + predicate.booleanOperator().name().toLowerCase()
                + "(" + predicateJava(predicate.leftPredicate()) + ", "
                + predicateJava(predicate.rightPredicate()) + ")";
            case NOT -> "ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate.not("
                + predicateJava(predicate.leftPredicate()) + ")";
        };
    }

    public static String expressionText(final ClassicalExpression expression) {
        if (expression == null) {
            return "";
        }
        return switch (expression.kind()) {
            case INTEGER -> Long.toString(expression.integerValue());
            case VARIABLE_REFERENCE -> expression.variableName();
            case BIT_REFERENCE -> expression.bit().register().name().value() + "[" + expression.bit().index() + "]";
            case REGISTER_REFERENCE -> expression.register().name().value();
            case SYMBOLIC_REFERENCE -> expression.symbolicText();
            case CALL -> expression.callableName() + "(" + expression.callArguments().stream()
                .map(DesktopClassicalExpressionRenderer::expressionText)
                .reduce((left, right) -> left + ", " + right)
                .orElse("") + ")";
            case BINARY_OPERATION -> "(" + expressionText(expression.leftExpression()) + " "
                + binaryText(expression.binaryOperator().name()) + " "
                + expressionText(expression.rightExpression()) + ")";
        };
    }

    public static String expressionJava(final ClassicalExpression expression) {
        if (expression == null) {
            return "null";
        }
        return switch (expression.kind()) {
            case INTEGER -> "ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression.integer("
                + expression.integerValue() + "L)";
            case VARIABLE_REFERENCE -> "ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression.variable(\""
                + escapeJava(expression.variableName()) + "\")";
            case SYMBOLIC_REFERENCE -> "ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression.symbolicReference(\""
                + escapeJava(expression.symbolicText()) + "\")";
            case CALL -> "ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression.call(\""
                + escapeJava(expression.callableName()) + "\", java.util.List.of("
                + expression.callArguments().stream()
                    .map(DesktopClassicalExpressionRenderer::expressionJava)
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("")
                + "))";
            case BINARY_OPERATION -> "ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression.binary("
                + "ru.pathcreator.vadim.quantum.domain.classical.ClassicalBinaryOperator."
                + expression.binaryOperator().name() + ", "
                + expressionJava(expression.leftExpression()) + ", "
                + expressionJava(expression.rightExpression()) + ")";
            case BIT_REFERENCE -> "ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression.bit(bit(\""
                + expression.bit().register().name().value() + "[" + expression.bit().index() + "]\"))";
            case REGISTER_REFERENCE -> "ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression.register(classicalRegister(\""
                + expression.register().name().value() + "\"))";
        };
    }

    private static String comparisonText(final String operator) {
        return switch (operator) {
            case "EQUAL" -> "==";
            case "NOT_EQUAL" -> "!=";
            case "LESS_THAN" -> "<";
            case "LESS_THAN_OR_EQUAL" -> "<=";
            case "GREATER_THAN" -> ">";
            case "GREATER_THAN_OR_EQUAL" -> ">=";
            default -> operator;
        };
    }

    private static String binaryText(final String operator) {
        return switch (operator) {
            case "ADD" -> "+";
            case "SUBTRACT" -> "-";
            case "MULTIPLY" -> "*";
            case "DIVIDE" -> "/";
            case "MODULO" -> "%";
            case "BITWISE_AND" -> "&";
            case "BITWISE_OR" -> "|";
            case "BITWISE_XOR" -> "^";
            case "SHIFT_LEFT" -> "<<";
            case "SHIFT_RIGHT" -> ">>";
            default -> operator;
        };
    }

    private static String escapeJava(final String value) {
        return value.replace(
            "\\",
            "\\\\"
        ).replace(
            "\"",
            "\\\""
        );
    }
}
/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.quil.syntax;

import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;

/**
 * Parser параметрических выражений Quil.
 */
final class QuilParameterParser {

    private QuilParameterParser() {
    }

    static ParameterExpression parse(final String value) {
        return new Parser(value).parse();
    }

    private static final class Parser {

        /**
         * Исходный текст выражения.
         */
        private final String value;

        /**
         * Текущая позиция чтения.
         */
        private int position;

        private Parser(final String value) {
            if (value == null) {
                throw new IllegalArgumentException("Quil parameter value must not be null.");
            }
            this.value = value;
            this.position = 0;
        }

        private ParameterExpression parse() {
            final ParameterExpression expression = parseAdditive();
            skipWhitespace();
            if (position != value.length()) {
                throw new IllegalArgumentException("Unexpected Quil parameter token.");
            }
            return expression;
        }

        private ParameterExpression parseAdditive() {
            ParameterExpression expression = parseMultiplicative();
            while (true) {
                skipWhitespace();
                if (consume('+')) {
                    expression = ParameterExpression.add(
                        expression,
                        parseMultiplicative()
                    );
                } else if (consume('-')) {
                    expression = ParameterExpression.subtract(
                        expression,
                        parseMultiplicative()
                    );
                } else {
                    return expression;
                }
            }
        }

        private ParameterExpression parseMultiplicative() {
            ParameterExpression expression = parseUnary();
            while (true) {
                skipWhitespace();
                if (consume('*')) {
                    expression = ParameterExpression.multiply(
                        expression,
                        parseUnary()
                    );
                } else if (consume('/')) {
                    expression = ParameterExpression.divide(
                        expression,
                        parseUnary()
                    );
                } else {
                    return expression;
                }
            }
        }

        private ParameterExpression parseUnary() {
            skipWhitespace();
            if (consume('-')) {
                return ParameterExpression.negate(parseUnary());
            }
            return parsePrimary();
        }

        private ParameterExpression parsePrimary() {
            skipWhitespace();
            if (consume('(')) {
                final ParameterExpression expression = parseAdditive();
                if (!consume(')')) {
                    throw new IllegalArgumentException("Quil parameter expression has an unclosed parenthesis.");
                }
                return expression;
            }
            final String token = readToken();
            if (token.isBlank()) {
                throw new IllegalArgumentException("Quil parameter expression is blank.");
            }
            return parseToken(token);
        }

        private String readToken() {
            final int start = position;
            while (
                position < value.length()
                    && !Character.isWhitespace(value.charAt(position))
                    && value.charAt(position) != '('
                    && value.charAt(position) != ')'
                    && value.charAt(position) != '+'
                    && value.charAt(position) != '-'
                    && value.charAt(position) != '*'
                    && value.charAt(position) != '/'
            ) {
                position++;
            }
            return value.substring(
                start,
                position
            );
        }

        private ParameterExpression parseToken(final String token) {
            final String trimmed = token.trim();
            if ("pi".equalsIgnoreCase(trimmed)) {
                return ParameterExpression.pi();
            }
            try {
                return ParameterExpression.of(Double.parseDouble(trimmed));
            } catch (final NumberFormatException exception) {
                return ParameterExpression.named(trimmed);
            }
        }

        private boolean consume(final char expected) {
            skipWhitespace();
            if (
                position < value.length()
                    && value.charAt(position) == expected
            ) {
                position++;
                return true;
            }
            return false;
        }

        private void skipWhitespace() {
            while (
                position < value.length()
                    && Character.isWhitespace(value.charAt(position))
            ) {
                position++;
            }
        }
    }
}
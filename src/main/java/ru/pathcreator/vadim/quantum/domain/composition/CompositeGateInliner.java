/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.composition;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import ru.pathcreator.vadim.quantum.domain.bit.Qubit;
import ru.pathcreator.vadim.quantum.domain.gate.Gate;
import ru.pathcreator.vadim.quantum.domain.gate.GateBodyOperation;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinition;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinitionKind;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterBinaryOperator;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpressionKind;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;

/**
 * Доменный сервис разворачивания composite gate definition в обычные операции схемы.
 */
public final class CompositeGateInliner {

    /**
     * Добавляет операцию гейта в целевую схему: composite gate раскрывает, остальные гейты добавляет как есть.
     *
     * @param target целевая схема
     * @param operation операция gate
     */
    public void appendExpandedGateOperation(
        final QuantumCircuit target,
        final GateOperation operation
    ) {
        if (target == null) {
            throw new IllegalArgumentException("Target circuit must not be null.");
        }
        if (operation == null) {
            throw new IllegalArgumentException("Gate operation must not be null.");
        }
        appendExpandedGateOperation(
            target,
            operation,
            new HashSet<>()
        );
    }

    private void appendExpandedGateOperation(
        final QuantumCircuit target,
        final GateOperation operation,
        final HashSet<String> expandingGateNames
    ) {
        final Gate gate = operation.gate();
        if (
            !(gate instanceof GateDefinition definition)
            || definition.kind() != GateDefinitionKind.COMPOSITE
        ) {
            appendLeafOperation(
                target,
                operation
            );
            return;
        }
        if (expandingGateNames.contains(definition.gateName())) {
            throw new IllegalArgumentException("Composite gate definitions contain a cycle.");
        }
        expandingGateNames.add(definition.gateName());
        final Map<String, ParameterExpression> parameterMap = createParameterMap(
            definition,
            operation
        );
        final Map<String, Qubit> qubitMap = createQubitMap(
            definition,
            operation
        );
        for (int i = 0; i < definition.bodyOperations().size(); i++) {
            appendBodyOperation(
                target,
                definition.bodyOperations().get(i),
                parameterMap,
                qubitMap,
                expandingGateNames
            );
        }
        expandingGateNames.remove(definition.gateName());
    }

    private void appendBodyOperation(
        final QuantumCircuit target,
        final GateBodyOperation bodyOperation,
        final Map<String, ParameterExpression> parameterMap,
        final Map<String, Qubit> qubitMap,
        final HashSet<String> expandingGateNames
    ) {
        final Qubit[] qubits = new Qubit[bodyOperation.qubitCount()];
        for (int i = 0; i < bodyOperation.qubitCount(); i++) {
            final Qubit qubit = qubitMap.get(bodyOperation.qubitName(i));
            if (qubit == null) {
                throw new IllegalArgumentException("Composite gate body references an unknown qubit argument.");
            }
            qubits[i] = qubit;
        }
        final ParameterExpression[] parameters = new ParameterExpression[bodyOperation.parameterCount()];
        for (int i = 0; i < bodyOperation.parameterCount(); i++) {
            parameters[i] = substituteParameters(
                bodyOperation.parameter(i),
                parameterMap
            );
        }
        appendExpandedGateOperation(
            target,
            GateOperation.parameterized(
                bodyOperation.gate(),
                parameters,
                qubits
            ),
            expandingGateNames
        );
    }

    private static Map<String, ParameterExpression> createParameterMap(
        final GateDefinition definition,
        final GateOperation operation
    ) {
        final LinkedHashMap<String, ParameterExpression> map = new LinkedHashMap<>();
        for (int i = 0; i < definition.parameterNames().size(); i++) {
            map.put(
                definition.parameterNames().get(i),
                operation.parameter(i)
            );
        }
        return Map.copyOf(map);
    }

    private static Map<String, Qubit> createQubitMap(
        final GateDefinition definition,
        final GateOperation operation
    ) {
        final LinkedHashMap<String, Qubit> map = new LinkedHashMap<>();
        for (int i = 0; i < definition.qubitNames().size(); i++) {
            map.put(
                definition.qubitNames().get(i),
                operation.qubit(i)
            );
        }
        return Map.copyOf(map);
    }

    private static ParameterExpression substituteParameters(
        final ParameterExpression expression,
        final Map<String, ParameterExpression> parameterMap
    ) {
        if (expression.kind() == ParameterExpressionKind.NAMED) {
            final ParameterExpression replacement = parameterMap.get(expression.name());
            if (replacement == null) {
                throw new IllegalArgumentException("Composite gate body references an unknown parameter argument.");
            }
            return replacement;
        }
        if (
            expression.kind() == ParameterExpressionKind.NUMERIC
            || expression.kind() == ParameterExpressionKind.KNOWN_CONSTANT
        ) {
            return expression;
        }
        if (expression.kind() == ParameterExpressionKind.UNARY) {
            return ParameterExpression.negate(substituteParameters(
                expression.left(),
                parameterMap
            ));
        }
        return rebuildBinary(
            expression.binaryOperator(),
            substituteParameters(
                expression.left(),
                parameterMap
            ),
            substituteParameters(
                expression.right(),
                parameterMap
            )
        );
    }

    private static ParameterExpression rebuildBinary(
        final ParameterBinaryOperator operator,
        final ParameterExpression left,
        final ParameterExpression right
    ) {
        if (operator == ParameterBinaryOperator.ADD) {
            return ParameterExpression.add(
                left,
                right
            );
        }
        if (operator == ParameterBinaryOperator.SUBTRACT) {
            return ParameterExpression.subtract(
                left,
                right
            );
        }
        if (operator == ParameterBinaryOperator.MULTIPLY) {
            return ParameterExpression.multiply(
                left,
                right
            );
        }
        return ParameterExpression.divide(
            left,
            right
        );
    }

    private static void appendLeafOperation(
        final QuantumCircuit target,
        final GateOperation operation
    ) {
        if (operation.parameterCount() == 0) {
            target.gate(
                operation.gate(),
                operation.qubits()
            );
        } else {
            target.parameterizedGate(
                operation.gate(),
                operation.parameters(),
                operation.qubits()
            );
        }
    }
}
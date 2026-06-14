/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.callable;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import ru.pathcreator.vadim.quantum.domain.callable.template.CallableBarrierOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableBlockOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableClassicalAssignment;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableClassicalAssignmentOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableClassicalExpressionKind;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableConditionalBlockOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableDelayOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableForLoopOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableGateOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableMeasureOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableOperationBlock;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableResetOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableTimingBoxOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableWhileLoopOperation;
import ru.pathcreator.vadim.quantum.domain.naming.IdentifierName;

/**
 * Универсальное описание вызываемой подпрограммы с телом IR.
 */
public final class CallableDefinition {

    /**
     * Имя подпрограммы.
     */
    private final IdentifierName name;

    /**
     * Аргументы подпрограммы.
     */
    private final CallableArgument[] arguments;

    /**
     * Тело подпрограммы.
     */
    private final CallableOperationBlock body;

    /**
     * Создает подпрограмму.
     *
     * @param name имя подпрограммы
     * @param body тело подпрограммы
     * @param arguments аргументы подпрограммы
     */
    public CallableDefinition(
        final String name,
        final CallableOperationBlock body,
        final CallableArgument... arguments
    ) {
        if (body == null) {
            throw new IllegalArgumentException("Callable definition body must not be null.");
        }
        if (arguments == null) {
            throw new IllegalArgumentException("Callable definition arguments must not be null.");
        }
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i] == null) {
                throw new IllegalArgumentException("Callable definition argument must not be null.");
            }
        }
        this.name = IdentifierName.of(
            name,
            "Callable definition"
        );
        this.arguments = Arrays.copyOf(
            arguments,
            arguments.length
        );
        this.body = body;
        validateArgumentNames(this.arguments);
        validateBody(
            this.body,
            this.arguments
        );
    }

    /**
     * Возвращает имя.
     *
     * @return имя
     */
    public String name() {
        return name.value();
    }

    /**
     * Возвращает количество аргументов.
     *
     * @return количество аргументов
     */
    public int argumentCount() {
        return arguments.length;
    }

    /**
     * Возвращает аргумент по индексу.
     *
     * @param index индекс аргумента
     * @return аргумент
     */
    public CallableArgument argument(final int index) {
        if (
            index < 0
            || index >= arguments.length
        ) {
            throw new IllegalArgumentException("Callable argument index is outside of bounds.");
        }
        return arguments[index];
    }

    /**
     * Возвращает аргументы.
     *
     * @return аргументы
     */
    public List<CallableArgument> arguments() {
        return List.of(arguments);
    }

    /**
     * Возвращает тело.
     *
     * @return тело
     */
    public CallableOperationBlock body() {
        return body;
    }

    private static void validateArgumentNames(final CallableArgument[] arguments) {
        final LinkedHashMap<String, CallableArgumentKind> names = new LinkedHashMap<>();
        for (int i = 0; i < arguments.length; i++) {
            if (names.containsKey(arguments[i].name())) {
                throw new IllegalArgumentException("Callable definition argument names must be unique.");
            }
            names.put(
                arguments[i].name(),
                arguments[i].kind()
            );
        }
    }

    private static void validateBody(
        final CallableOperationBlock body,
        final CallableArgument[] arguments
    ) {
        final LinkedHashMap<String, CallableArgumentKind> argumentKinds = new LinkedHashMap<>();
        for (int i = 0; i < arguments.length; i++) {
            argumentKinds.put(
                arguments[i].name(),
                arguments[i].kind()
            );
        }
        validateBlock(
            body,
            argumentKinds
        );
    }

    private static void validateBlock(
        final CallableOperationBlock block,
        final LinkedHashMap<String, CallableArgumentKind> argumentKinds
    ) {
        for (int i = 0; i < block.operationCount(); i++) {
            validateOperation(
                block.operation(i),
                argumentKinds
            );
        }
    }

    private static void validateOperation(
        final CallableOperation operation,
        final LinkedHashMap<String, CallableArgumentKind> argumentKinds
    ) {
        if (operation instanceof CallableGateOperation gateOperation) {
            for (int i = 0; i < gateOperation.qubitCount(); i++) {
                requireArgumentKind(
                    argumentKinds,
                    gateOperation.qubitName(i),
                    CallableArgumentKind.QUBIT
                );
            }
        } else if (operation instanceof CallableMeasureOperation measureOperation) {
            requireArgumentKind(
                argumentKinds,
                measureOperation.qubitName(),
                CallableArgumentKind.QUBIT
            );
            requireArgumentKind(
                argumentKinds,
                measureOperation.classicalName(),
                CallableArgumentKind.CLASSICAL
            );
        } else if (operation instanceof CallableResetOperation resetOperation) {
            requireArgumentKind(
                argumentKinds,
                resetOperation.qubitName(),
                CallableArgumentKind.QUBIT
            );
        } else if (operation instanceof CallableBarrierOperation barrierOperation) {
            for (int i = 0; i < barrierOperation.qubitCount(); i++) {
                requireArgumentKind(
                    argumentKinds,
                    barrierOperation.qubitName(i),
                    CallableArgumentKind.QUBIT
                );
            }
        } else if (operation instanceof CallableClassicalAssignmentOperation assignmentOperation) {
            validateAssignment(
                assignmentOperation.assignment(),
                argumentKinds
            );
        } else if (operation instanceof CallableBlockOperation blockOperation) {
            validateBlock(
                blockOperation.body(),
                argumentKinds
            );
        } else if (operation instanceof CallableConditionalBlockOperation conditionalOperation) {
            validatePredicate(
                conditionalOperation.predicate(),
                argumentKinds
            );
            validateBlock(
                conditionalOperation.thenBlock(),
                argumentKinds
            );
            if (conditionalOperation.hasElseBlock()) {
                validateBlock(
                    conditionalOperation.elseBlock(),
                    argumentKinds
                );
            }
        } else if (operation instanceof CallableForLoopOperation loopOperation) {
            validateBlock(
                loopOperation.body(),
                argumentKinds
            );
        } else if (operation instanceof CallableWhileLoopOperation loopOperation) {
            validatePredicate(
                loopOperation.predicate(),
                argumentKinds
            );
            validateBlock(
                loopOperation.body(),
                argumentKinds
            );
        } else if (operation instanceof CallableDelayOperation delayOperation) {
            for (int i = 0; i < delayOperation.qubitCount(); i++) {
                requireArgumentKind(
                    argumentKinds,
                    delayOperation.qubitName(i),
                    CallableArgumentKind.QUBIT
                );
            }
        } else if (operation instanceof CallableTimingBoxOperation boxOperation) {
            validateBlock(
                boxOperation.body(),
                argumentKinds
            );
        } else {
            throw new IllegalArgumentException("Callable operation type is not supported.");
        }
    }

    private static void validateAssignment(
        final CallableClassicalAssignment assignment,
        final LinkedHashMap<String, CallableArgumentKind> argumentKinds
    ) {
        validateClassicalExpression(
            assignment.target(),
            argumentKinds
        );
        validateClassicalExpression(
            assignment.value(),
            argumentKinds
        );
    }

    private static void validatePredicate(
        final CallableClassicalPredicate predicate,
        final LinkedHashMap<String, CallableArgumentKind> argumentKinds
    ) {
        switch (predicate.kind()) {
            case COMPARISON -> {
                validateClassicalExpression(
                    predicate.leftExpression(),
                    argumentKinds
                );
                validateClassicalExpression(
                    predicate.rightExpression(),
                    argumentKinds
                );
            }
            case NOT -> validatePredicate(
                predicate.leftPredicate(),
                argumentKinds
            );
            case BOOLEAN -> {
                validatePredicate(
                    predicate.leftPredicate(),
                    argumentKinds
                );
                validatePredicate(
                    predicate.rightPredicate(),
                    argumentKinds
                );
            }
            default -> throw new IllegalStateException("Unsupported callable classical predicate kind.");
        }
    }

    private static void validateClassicalExpression(
        final CallableClassicalExpression expression,
        final LinkedHashMap<String, CallableArgumentKind> argumentKinds
    ) {
        if (expression.kind() == CallableClassicalExpressionKind.ARGUMENT_REFERENCE) {
            requireArgumentKind(
                argumentKinds,
                expression.argumentName(),
                CallableArgumentKind.CLASSICAL
            );
        }
    }

    private static void requireArgumentKind(
        final LinkedHashMap<String, CallableArgumentKind> argumentKinds,
        final String name,
        final CallableArgumentKind expectedKind
    ) {
        final CallableArgumentKind actualKind = argumentKinds.get(name);
        if (actualKind == null) {
            throw new IllegalArgumentException("Callable body references unknown argument: " + name + ".");
        }
        if (actualKind != expectedKind) {
            throw new IllegalArgumentException("Callable body argument kind does not match reference: " + name + ".");
        }
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CallableDefinition definition)) {
            return false;
        }
        return Objects.equals(
            name,
            definition.name
        )
            && Arrays.equals(
                arguments,
                definition.arguments
            )
            && Objects.equals(
                body,
                definition.body
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            name,
            Arrays.hashCode(arguments),
            body
        );
    }
}
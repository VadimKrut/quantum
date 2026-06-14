/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.gate;

import java.util.List;
import java.util.Objects;

import ru.pathcreator.vadim.quantum.domain.naming.IdentifierName;

/**
 * Открытое описание gate-based гейта внутри Quantum IR.
 */
public final class GateDefinition implements Gate {

    private static final String PARAMETER_NAME_SUBJECT = "Gate parameter name";
    private static final String QUBIT_NAME_SUBJECT = "Gate qubit name";

    private final GateName name;
    private final int arity;
    private final int parameterCount;
    private final List<GateValidationRule> validationRules;
    private final GateDefinitionKind kind;
    private final List<String> parameterNames;
    private final List<String> qubitNames;
    private final List<GateBodyOperation> bodyOperations;
    private final GateMatrix matrix;

    private GateDefinition(
        final GateName name,
        final int arity,
        final int parameterCount,
        final List<GateValidationRule> validationRules,
        final GateDefinitionKind kind,
        final List<String> parameterNames,
        final List<String> qubitNames,
        final List<GateBodyOperation> bodyOperations,
        final GateMatrix matrix
    ) {
        this.name = name;
        this.arity = arity;
        this.parameterCount = parameterCount;
        this.validationRules = validationRules;
        this.kind = kind;
        this.parameterNames = parameterNames;
        this.qubitNames = qubitNames;
        this.bodyOperations = bodyOperations;
        this.matrix = matrix;
    }

    public static GateDefinition of(
        final String name,
        final int arity,
        final int parameterCount
    ) {
        return of(
            GateName.of(name),
            arity,
            parameterCount
        );
    }

    public static GateDefinition of(
        final String name,
        final int arity,
        final int parameterCount,
        final List<GateValidationRule> validationRules
    ) {
        return of(
            GateName.of(name),
            arity,
            parameterCount,
            validationRules
        );
    }

    public static GateDefinition of(
        final GateName name,
        final int arity,
        final int parameterCount
    ) {
        return of(
            name,
            arity,
            parameterCount,
            defaultValidationRules(arity)
        );
    }

    public static GateDefinition of(
        final GateName name,
        final int arity,
        final int parameterCount,
        final List<GateValidationRule> validationRules
    ) {
        validate(
            name,
            arity,
            parameterCount
        );
        return new GateDefinition(
            name,
            arity,
            parameterCount,
            validateAndCopyRules(validationRules),
            GateDefinitionKind.INTRINSIC,
            List.of(),
            List.of(),
            List.of(),
            null
        );
    }

    /**
     * Создает opaque gate definition без тела.
     *
     * @param name имя гейта
     * @param parameterNames имена параметров
     * @param qubitNames имена qubit arguments
     * @return opaque gate definition
     */
    public static GateDefinition opaque(
        final String name,
        final List<String> parameterNames,
        final List<String> qubitNames
    ) {
        final List<String> checkedParameterNames = validateAndCopyNames(
            parameterNames,
            PARAMETER_NAME_SUBJECT
        );
        final List<String> checkedQubitNames = validateAndCopyNames(
            qubitNames,
            QUBIT_NAME_SUBJECT
        );
        return new GateDefinition(
            GateName.of(name),
            checkedQubitNames.size(),
            checkedParameterNames.size(),
            defaultValidationRules(checkedQubitNames.size()),
            GateDefinitionKind.OPAQUE,
            checkedParameterNames,
            checkedQubitNames,
            List.of(),
            null
        );
    }

    /**
     * Создает composite gate definition с символическим телом.
     *
     * @param name имя гейта
     * @param parameterNames имена параметров
     * @param qubitNames имена qubit arguments
     * @param bodyOperations операции тела
     * @return composite gate definition
     */
    public static GateDefinition composite(
        final String name,
        final List<String> parameterNames,
        final List<String> qubitNames,
        final List<GateBodyOperation> bodyOperations
    ) {
        final List<String> checkedParameterNames = validateAndCopyNames(
            parameterNames,
            PARAMETER_NAME_SUBJECT
        );
        final List<String> checkedQubitNames = validateAndCopyNames(
            qubitNames,
            QUBIT_NAME_SUBJECT
        );
        final List<GateBodyOperation> checkedBodyOperations = validateAndCopyBodyOperations(bodyOperations);
        return new GateDefinition(
            GateName.of(name),
            checkedQubitNames.size(),
            checkedParameterNames.size(),
            defaultValidationRules(checkedQubitNames.size()),
            GateDefinitionKind.COMPOSITE,
            checkedParameterNames,
            checkedQubitNames,
            checkedBodyOperations,
            null
        );
    }

    public static GateDefinition matrix(
        final String name,
        final List<String> parameterNames,
        final List<String> qubitNames,
        final GateMatrix matrix
    ) {
        final List<String> checkedParameterNames = validateAndCopyNames(
            parameterNames,
            PARAMETER_NAME_SUBJECT
        );
        final List<String> checkedQubitNames = validateAndCopyNames(
            qubitNames,
            QUBIT_NAME_SUBJECT
        );
        if (matrix == null) {
            throw new IllegalArgumentException("Gate matrix must not be null.");
        }
        final int expectedSize = 1 << checkedQubitNames.size();
        if (
            matrix.rowCount() != expectedSize
            || matrix.columnCount() != expectedSize
        ) {
            throw new IllegalArgumentException("Gate matrix dimensions must match qubit argument count.");
        }
        return new GateDefinition(
            GateName.of(name),
            checkedQubitNames.size(),
            checkedParameterNames.size(),
            defaultValidationRules(checkedQubitNames.size()),
            GateDefinitionKind.MATRIX,
            checkedParameterNames,
            checkedQubitNames,
            List.of(),
            matrix
        );
    }

    public GateName name() {
        return name;
    }

    @Override
    public String gateName() {
        return name.value();
    }

    @Override
    public int arity() {
        return arity;
    }

    @Override
    public int parameterCount() {
        return parameterCount;
    }

    @Override
    public List<GateValidationRule> validationRules() {
        return validationRules;
    }

    public GateDefinitionKind kind() {
        return kind;
    }

    public List<String> parameterNames() {
        return parameterNames;
    }

    public List<String> qubitNames() {
        return qubitNames;
    }

    public List<GateBodyOperation> bodyOperations() {
        return bodyOperations;
    }

    public GateMatrix matrix() {
        return matrix;
    }

    private static void validate(
        final GateName name,
        final int arity,
        final int parameterCount
    ) {
        if (name == null) {
            throw new IllegalArgumentException("Gate name must not be null.");
        }
        if (arity <= 0) {
            throw new IllegalArgumentException("Gate arity must be positive.");
        }
        if (parameterCount < 0) {
            throw new IllegalArgumentException("Gate parameter count must not be negative.");
        }
    }

    private static List<GateValidationRule> defaultValidationRules(final int arity) {
        if (arity <= 1) {
            return List.of();
        }
        return List.of(DistinctQubitsGateValidationRule.INSTANCE);
    }

    private static List<GateValidationRule> validateAndCopyRules(final List<GateValidationRule> validationRules) {
        if (validationRules == null) {
            throw new IllegalArgumentException("Gate validation rules must not be null.");
        }
        for (int i = 0; i < validationRules.size(); i++) {
            if (validationRules.get(i) == null) {
                throw new IllegalArgumentException("Gate validation rule must not be null.");
            }
        }
        return List.copyOf(validationRules);
    }

    private static List<String> validateAndCopyNames(
        final List<String> names,
        final String subjectName
    ) {
        if (names == null) {
            throw new IllegalArgumentException(subjectName + " list must not be null.");
        }
        final String[] checked = new String[names.size()];
        for (int i = 0; i < names.size(); i++) {
            checked[i] = IdentifierName.of(
                names.get(i),
                subjectName
            ).value();
            for (int j = 0; j < i; j++) {
                if (checked[i].equals(checked[j])) {
                    throw new IllegalArgumentException(subjectName + " must not be duplicated.");
                }
            }
        }
        return List.of(checked);
    }

    private static List<GateBodyOperation> validateAndCopyBodyOperations(final List<GateBodyOperation> bodyOperations) {
        if (bodyOperations == null) {
            throw new IllegalArgumentException("Composite gate body operations must not be null.");
        }
        for (int i = 0; i < bodyOperations.size(); i++) {
            if (bodyOperations.get(i) == null) {
                throw new IllegalArgumentException("Composite gate body operation must not be null.");
            }
        }
        return List.copyOf(bodyOperations);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GateDefinition definition)) {
            return false;
        }
        return arity == definition.arity
            && parameterCount == definition.parameterCount
            && kind == definition.kind
            && Objects.equals(
                name,
                definition.name
            )
            && Objects.equals(
                validationRules,
                definition.validationRules
            )
            && Objects.equals(
                parameterNames,
                definition.parameterNames
            )
            && Objects.equals(
                qubitNames,
                definition.qubitNames
            )
            && Objects.equals(
                bodyOperations,
                definition.bodyOperations
            )
            && Objects.equals(
                matrix,
                definition.matrix
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            name,
            arity,
            parameterCount,
            validationRules,
            kind,
            parameterNames,
            qubitNames,
            bodyOperations,
            matrix
        );
    }
}
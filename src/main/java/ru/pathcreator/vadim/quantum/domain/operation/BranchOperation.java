/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.operation;

import java.util.Objects;

import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.naming.IdentifierName;

/**
 * Instruction-level branch to a named label.
 */
public final class BranchOperation implements Operation {

    private final String targetLabel;
    private final BranchConditionKind conditionKind;
    private final ClassicalExpression condition;

    public BranchOperation(
        final String targetLabel,
        final BranchConditionKind conditionKind,
        final ClassicalExpression condition
    ) {
        if (conditionKind == null) {
            throw new IllegalArgumentException("Branch condition kind must not be null.");
        }
        if (
            conditionKind != BranchConditionKind.ALWAYS
            && condition == null
        ) {
            throw new IllegalArgumentException("Conditional branch must have a condition.");
        }
        if (
            conditionKind == BranchConditionKind.ALWAYS
            && condition != null
        ) {
            throw new IllegalArgumentException("Unconditional branch must not have a condition.");
        }
        this.targetLabel = IdentifierName.of(
            targetLabel,
            "Branch target label"
        ).value();
        this.conditionKind = conditionKind;
        this.condition = condition;
    }

    public static BranchOperation always(final String targetLabel) {
        return new BranchOperation(
            targetLabel,
            BranchConditionKind.ALWAYS,
            null
        );
    }

    public static BranchOperation whenTrue(
        final String targetLabel,
        final ClassicalExpression condition
    ) {
        return new BranchOperation(
            targetLabel,
            BranchConditionKind.WHEN_TRUE,
            condition
        );
    }

    public static BranchOperation whenFalse(
        final String targetLabel,
        final ClassicalExpression condition
    ) {
        return new BranchOperation(
            targetLabel,
            BranchConditionKind.WHEN_FALSE,
            condition
        );
    }

    @Override
    public OperationKind kind() {
        return OperationKind.BRANCH;
    }

    public String targetLabel() {
        return targetLabel;
    }

    public BranchConditionKind conditionKind() {
        return conditionKind;
    }

    public boolean hasCondition() {
        return condition != null;
    }

    public ClassicalExpression condition() {
        if (condition == null) {
            throw new IllegalStateException("Branch does not have a condition.");
        }
        return condition;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BranchOperation operation)) {
            return false;
        }
        return Objects.equals(
            targetLabel,
            operation.targetLabel
        )
            && conditionKind == operation.conditionKind
            && Objects.equals(
                condition,
                operation.condition
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            targetLabel,
            conditionKind,
            condition
        );
    }
}
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

import ru.pathcreator.vadim.quantum.domain.naming.IdentifierName;

/**
 * Named target in instruction-level control flow.
 */
public final class LabelOperation implements Operation {

    private final String name;

    public LabelOperation(final String name) {
        this.name = IdentifierName.of(
            name,
            "Control-flow label"
        ).value();
    }

    @Override
    public OperationKind kind() {
        return OperationKind.LABEL;
    }

    public String name() {
        return name;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LabelOperation operation)) {
            return false;
        }
        return Objects.equals(
            name,
            operation.name
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
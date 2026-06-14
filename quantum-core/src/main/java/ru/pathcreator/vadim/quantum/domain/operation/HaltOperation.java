/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.operation;

/**
 * Explicit program halt instruction.
 */
public final class HaltOperation implements Operation {

    public static final HaltOperation INSTANCE = new HaltOperation();

    private HaltOperation() {
    }

    @Override
    public OperationKind kind() {
        return OperationKind.HALT;
    }
}
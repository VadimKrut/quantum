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
 * Explicit synchronization wait instruction.
 */
public final class WaitOperation implements Operation {

    public static final WaitOperation INSTANCE = new WaitOperation();

    private WaitOperation() {
    }

    @Override
    public OperationKind kind() {
        return OperationKind.WAIT;
    }
}
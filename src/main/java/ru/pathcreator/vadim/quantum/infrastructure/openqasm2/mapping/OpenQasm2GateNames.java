/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.openqasm2.mapping;

/**
 * Имена gate, которые принадлежат OpenQASM 2/qelib1 namespace.
 */
public final class OpenQasm2GateNames {

    private OpenQasm2GateNames() {
        throw new UnsupportedOperationException("Utility class must not be instantiated.");
    }

    /**
     * Проверяет, занято ли имя qelib1 alias на уровне OpenQASM 2.
     *
     * @param name имя gate во внешнем формате
     * @return true, если имя зарезервировано OpenQASM 2/qelib1
     */
    public static boolean isReservedQelibAlias(final String name) {
        return OpenQasm2QelibGates.containsExactName(name);
    }
}
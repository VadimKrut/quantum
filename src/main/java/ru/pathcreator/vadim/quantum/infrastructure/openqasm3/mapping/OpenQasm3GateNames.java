/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.openqasm3.mapping;

/**
 * Имена gate, которые принадлежат стандартной библиотеке OpenQASM 3.
 */
public final class OpenQasm3GateNames {

    private OpenQasm3GateNames() {
        throw new UnsupportedOperationException("Utility class must not be instantiated.");
    }

    /**
     * Проверяет, занято ли имя стандартной библиотекой OpenQASM 3.
     *
     * @param name имя gate во внешнем формате
     * @return true, если имя зарезервировано стандартной библиотекой
     */
    public static boolean isReservedStdAlias(final String name) {
        return OpenQasm3StdGates.containsName(name);
    }
}
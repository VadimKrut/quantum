/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.timing;

/**
 * Единица длительности для timing IR.
 */
public enum DurationUnit {

    DT("dt"),
    NS("ns"),
    US("us"),
    MS("ms"),
    S("s");

    private final String symbol;

    DurationUnit(final String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }

    public static DurationUnit fromSymbol(final String symbol) {
        if (symbol == null) {
            throw new IllegalArgumentException("Duration unit symbol must not be null.");
        }
        for (int i = 0; i < values().length; i++) {
            if (values()[i].symbol.equals(symbol)) {
                return values()[i];
            }
        }
        throw new IllegalArgumentException("Unsupported duration unit: " + symbol + ".");
    }
}
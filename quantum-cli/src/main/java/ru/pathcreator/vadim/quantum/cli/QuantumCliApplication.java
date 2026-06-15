/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.cli;

public final class QuantumCliApplication {

    private QuantumCliApplication() {
    }

    public static void main(final String[] args) {
        System.exit(QuantumCli.run(
            args,
            System.out,
            System.err
        ));
    }
}
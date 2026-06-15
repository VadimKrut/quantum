/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.launcher;

import ru.pathcreator.vadim.quantum.desktop.smoke.DesktopSmokeApplication;
import ru.pathcreator.vadim.quantum.desktop.ui.QuantumDesktopApplication;

/**
 * Launcher для shaded jar: запускает JavaFX окно или headless smoke.
 */
public final class QuantumDesktopLauncher {

    private QuantumDesktopLauncher() {
    }

    public static void main(final String[] args) {
        if (
            args.length > 0
            && "--smoke".equals(args[0])
        ) {
            final String[] smokeArgs = new String[args.length - 1];
            System.arraycopy(
                args,
                1,
                smokeArgs,
                0,
                smokeArgs.length
            );
            System.exit(DesktopSmokeApplication.run(
                smokeArgs,
                System.out,
                System.err
            ));
        }
        QuantumDesktopApplication.main(args);
    }
}
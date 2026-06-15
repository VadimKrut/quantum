/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.visualization;

import java.util.List;

public final class ProgramTimeline {

    private final List<CircuitTimeline> circuits;

    public ProgramTimeline(final List<CircuitTimeline> circuits) {
        if (circuits == null) {
            throw new IllegalArgumentException("Program timeline circuits must not be null.");
        }
        this.circuits = List.copyOf(circuits);
    }

    public List<CircuitTimeline> circuits() {
        return circuits;
    }
}
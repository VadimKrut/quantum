/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.layout;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;

class DesktopGateCatalogViewTest {

    @Test
    void exposesEveryStandardGateInVisualCatalog() {
        final List<String> gates = new DesktopGateCatalogView().gates();

        for (final StandardGate gate : StandardGate.values()) {
            assertTrue(
                gates.contains(gate.name()),
                gate.name()
            );
        }
    }
}
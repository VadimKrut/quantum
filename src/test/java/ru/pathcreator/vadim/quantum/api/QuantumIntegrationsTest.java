/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.api;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.integration.contract.QuantumIntegration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class QuantumIntegrationsTest {

    @Test
    void createsOpenQasm2Integration() {
        final QuantumIntegration integration = QuantumIntegrations.openQasm2();

        assertNotNull(integration);
        assertEquals(
            IntegrationFormat.OPENQASM_2,
            integration.format()
        );
    }
}
/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.integration.capability;

import java.util.List;

import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnostic;

/**
 * Результат preflight-проверки Quantum IR против target capability profile.
 */
public final class CapabilityPreflightResult {

    private final List<IntegrationDiagnostic> diagnostics;

    private CapabilityPreflightResult(final List<IntegrationDiagnostic> diagnostics) {
        this.diagnostics = diagnostics;
    }

    public static CapabilityPreflightResult of(final List<IntegrationDiagnostic> diagnostics) {
        if (diagnostics == null) {
            throw new IllegalArgumentException("Capability preflight diagnostics must not be null.");
        }
        for (IntegrationDiagnostic diagnostic : diagnostics) {
            if (diagnostic == null) {
                throw new IllegalArgumentException("Capability preflight diagnostic must not be null.");
            }
        }
        return new CapabilityPreflightResult(List.copyOf(diagnostics));
    }

    public boolean isSuccess() {
        for (int i = 0; i < diagnostics.size(); i++) {
            if (diagnostics.get(i).isError()) {
                return false;
            }
        }
        return true;
    }

    public List<IntegrationDiagnostic> diagnostics() {
        return diagnostics;
    }
}
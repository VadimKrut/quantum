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

    private final CapabilityPreflightStatus status;
    private final List<IntegrationDiagnostic> diagnostics;

    private CapabilityPreflightResult(
        final CapabilityPreflightStatus status,
        final List<IntegrationDiagnostic> diagnostics
    ) {
        this.status = status;
        this.diagnostics = diagnostics;
    }

    public static CapabilityPreflightResult of(final List<IntegrationDiagnostic> diagnostics) {
        return of(
            deriveStatus(
                diagnostics,
                false
            ),
            diagnostics
        );
    }

    public static CapabilityPreflightResult of(
        final CapabilityPreflightStatus status,
        final List<IntegrationDiagnostic> diagnostics
    ) {
        if (status == null) {
            throw new IllegalArgumentException("Capability preflight status must not be null.");
        }
        if (diagnostics == null) {
            throw new IllegalArgumentException("Capability preflight diagnostics must not be null.");
        }
        for (IntegrationDiagnostic diagnostic : diagnostics) {
            if (diagnostic == null) {
                throw new IllegalArgumentException("Capability preflight diagnostic must not be null.");
            }
        }
        return new CapabilityPreflightResult(
            status,
            List.copyOf(diagnostics)
        );
    }

    public boolean isSuccess() {
        for (int i = 0; i < diagnostics.size(); i++) {
            if (diagnostics.get(i).isError()) {
                return false;
            }
        }
        return true;
    }

    public CapabilityPreflightStatus status() {
        return status;
    }

    public boolean requiresLowering() {
        return status == CapabilityPreflightStatus.LOWERING_REQUIRED;
    }

    public List<IntegrationDiagnostic> diagnostics() {
        return diagnostics;
    }

    static CapabilityPreflightStatus deriveStatus(
        final List<IntegrationDiagnostic> diagnostics,
        final boolean loweringRequired
    ) {
        if (diagnostics != null) {
            for (int i = 0; i < diagnostics.size(); i++) {
                if (
                    diagnostics.get(i) != null
                    && diagnostics.get(i).isError()
                ) {
                    return diagnostics.get(i).message().contains("without semantic loss")
                        ? CapabilityPreflightStatus.UNSUPPORTED_WITHOUT_LOSS
                        : CapabilityPreflightStatus.UNSUPPORTED_BY_TARGET;
                }
            }
        }
        return loweringRequired
            ? CapabilityPreflightStatus.LOWERING_REQUIRED
            : CapabilityPreflightStatus.EXPORTABLE;
    }
}
/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.backend;

import java.util.List;

import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightResult;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

public final class BackendPreflightResult {

    private final ValidationResult validationResult;
    private final CapabilityPreflightResult capabilityPreflightResult;
    private final List<BackendDiagnostic> diagnostics;

    public BackendPreflightResult(
        final ValidationResult validationResult,
        final CapabilityPreflightResult capabilityPreflightResult,
        final List<BackendDiagnostic> diagnostics
    ) {
        if (validationResult == null) {
            throw new IllegalArgumentException("Backend preflight validation result must not be null.");
        }
        if (capabilityPreflightResult == null) {
            throw new IllegalArgumentException("Backend capability preflight result must not be null.");
        }
        if (diagnostics == null) {
            throw new IllegalArgumentException("Backend preflight diagnostics must not be null.");
        }
        this.validationResult = validationResult;
        this.capabilityPreflightResult = capabilityPreflightResult;
        this.diagnostics = List.copyOf(diagnostics);
    }

    public ValidationResult validationResult() {
        return validationResult;
    }

    public CapabilityPreflightResult capabilityPreflightResult() {
        return capabilityPreflightResult;
    }

    public List<BackendDiagnostic> diagnostics() {
        return diagnostics;
    }

    public boolean isSuccess() {
        return validationResult.isValid()
            && capabilityPreflightResult.isSuccess()
            && !hasErrors();
    }

    private boolean hasErrors() {
        for (BackendDiagnostic diagnostic : diagnostics) {
            if (diagnostic.isError()) {
                return true;
            }
        }
        return false;
    }
}
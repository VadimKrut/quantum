/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.openqasm3.include;

import java.util.Map;

import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnostic;

/**
 * Результат resolve include sources для OpenQASM 3 parser.
 */
public final class OpenQasm3IncludeResolverResult {

    private final Map<String, String> sources;
    private final IntegrationDiagnostic diagnostic;

    private OpenQasm3IncludeResolverResult(
        final Map<String, String> sources,
        final IntegrationDiagnostic diagnostic
    ) {
        this.sources = sources;
        this.diagnostic = diagnostic;
    }

    public static OpenQasm3IncludeResolverResult success(final Map<String, String> sources) {
        if (sources == null) {
            throw new IllegalArgumentException("OpenQASM 3 include sources must not be null.");
        }
        return new OpenQasm3IncludeResolverResult(
            Map.copyOf(sources),
            null
        );
    }

    public static OpenQasm3IncludeResolverResult failure(final IntegrationDiagnostic diagnostic) {
        if (diagnostic == null) {
            throw new IllegalArgumentException("OpenQASM 3 include resolver diagnostic must not be null.");
        }
        return new OpenQasm3IncludeResolverResult(
            Map.of(),
            diagnostic
        );
    }

    public boolean isSuccess() {
        return diagnostic == null;
    }

    public Map<String, String> sources() {
        return sources;
    }

    public IntegrationDiagnostic diagnostic() {
        if (diagnostic == null) {
            throw new IllegalStateException("OpenQASM 3 include resolver result is successful.");
        }
        return diagnostic;
    }
}
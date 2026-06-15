/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.regression;

import ru.pathcreator.vadim.quantum.application.integration.contract.QuantumIntegration;

/**
 * Один входной файл или сценарий regression-corpus.
 */
public final class CorpusRegressionCase {

    private final String name;
    private final String source;
    private final QuantumIntegration inputIntegration;

    private CorpusRegressionCase(
        final String name,
        final String source,
        final QuantumIntegration inputIntegration
    ) {
        this.name = name;
        this.source = source;
        this.inputIntegration = inputIntegration;
    }

    public static CorpusRegressionCase of(
        final String name,
        final String source,
        final QuantumIntegration inputIntegration
    ) {
        if (
            name == null
            || name.isBlank()
        ) {
            throw new IllegalArgumentException("Corpus regression case name must not be blank.");
        }
        if (source == null) {
            throw new IllegalArgumentException("Corpus regression case source must not be null.");
        }
        if (inputIntegration == null) {
            throw new IllegalArgumentException("Corpus regression case input integration must not be null.");
        }
        return new CorpusRegressionCase(
            name,
            source,
            inputIntegration
        );
    }

    public String name() {
        return name;
    }

    public String source() {
        return source;
    }

    public QuantumIntegration inputIntegration() {
        return inputIntegration;
    }
}
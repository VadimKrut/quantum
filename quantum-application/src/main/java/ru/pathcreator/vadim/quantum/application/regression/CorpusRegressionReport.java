/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.regression;

import java.util.List;

/**
 * Единый отчет regression-corpus по нескольким входным программам.
 */
public final class CorpusRegressionReport {

    private final List<CorpusRegressionCaseReport> cases;

    private CorpusRegressionReport(final List<CorpusRegressionCaseReport> cases) {
        this.cases = cases;
    }

    public static CorpusRegressionReport of(final List<CorpusRegressionCaseReport> cases) {
        if (cases == null) {
            throw new IllegalArgumentException("Corpus regression cases must not be null.");
        }
        return new CorpusRegressionReport(List.copyOf(cases));
    }

    public List<CorpusRegressionCaseReport> cases() {
        return cases;
    }

    public int caseCount() {
        return cases.size();
    }

    public int failureCount() {
        int count = 0;
        for (int i = 0; i < cases.size(); i++) {
            if (!cases.get(i).isSuccess()) {
                count++;
            }
        }
        return count;
    }

    public boolean isSuccess() {
        return failureCount() == 0;
    }
}
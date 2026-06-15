/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.smoke;

import java.util.List;

/**
 * Итог headless smoke-проверки desktop workflow.
 */
public final class DesktopSmokeReport {

    private final boolean success;
    private final List<DesktopSmokeStep> steps;

    public DesktopSmokeReport(final List<DesktopSmokeStep> steps) {
        if (steps == null) {
            throw new IllegalArgumentException("Desktop smoke steps must not be null.");
        }
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i) == null) {
                throw new IllegalArgumentException("Desktop smoke step must not be null.");
            }
        }
        this.steps = List.copyOf(steps);
        this.success = computeSuccess(steps);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean getSuccess() {
        return success;
    }

    public List<DesktopSmokeStep> steps() {
        return steps;
    }

    public List<DesktopSmokeStep> getSteps() {
        return steps;
    }

    public int stepCount() {
        return steps.size();
    }

    public int getStepCount() {
        return stepCount();
    }

    public int failedStepCount() {
        int failures = 0;
        for (int i = 0; i < steps.size(); i++) {
            if (!steps.get(i).success()) {
                failures++;
            }
        }
        return failures;
    }

    public int getFailedStepCount() {
        return failedStepCount();
    }

    private static boolean computeSuccess(final List<DesktopSmokeStep> steps) {
        if (steps.isEmpty()) {
            return false;
        }
        for (int i = 0; i < steps.size(); i++) {
            if (!steps.get(i).success()) {
                return false;
            }
        }
        return true;
    }
}
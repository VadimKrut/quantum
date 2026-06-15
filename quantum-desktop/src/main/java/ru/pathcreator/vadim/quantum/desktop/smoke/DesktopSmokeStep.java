/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.smoke;

/**
 * Один шаг headless smoke-проверки desktop workflow.
 */
public record DesktopSmokeStep(
    String name,
    boolean success,
    String status,
    String summary
) {

    public DesktopSmokeStep {
        if (
            name == null
            || name.isBlank()
        ) {
            throw new IllegalArgumentException("Desktop smoke step name must not be blank.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Desktop smoke step status must not be null.");
        }
        if (summary == null) {
            throw new IllegalArgumentException("Desktop smoke step summary must not be null.");
        }
    }
}
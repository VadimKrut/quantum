/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.circuit;

/**
 * Определяет, когда desktop должен показывать компактный preview вместо полной отрисовки огромной схемы.
 */
public final class DesktopLargeProgramRenderPolicy {

    private final int interactiveOperationLimit;

    public DesktopLargeProgramRenderPolicy() {
        this(2_000);
    }

    public DesktopLargeProgramRenderPolicy(final int interactiveOperationLimit) {
        if (interactiveOperationLimit < 1) {
            throw new IllegalArgumentException("Interactive operation limit must be positive.");
        }
        this.interactiveOperationLimit = interactiveOperationLimit;
    }

    public boolean shouldUsePreview(
        final int operationCount,
        final boolean fullRenderRequested
    ) {
        return !fullRenderRequested && operationCount > interactiveOperationLimit;
    }

    public String summary(final int operationCount) {
        return "Circuit has " + operationCount + " operation(s). Full interactive rendering is disabled above "
            + interactiveOperationLimit
            + " operations until you explicitly enable it.";
    }

    public int interactiveOperationLimit() {
        return interactiveOperationLimit;
    }
}
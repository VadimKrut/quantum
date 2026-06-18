/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.circuit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class DesktopLargeProgramRenderPolicyTest {

    @Test
    void usesPreviewOnlyAboveInteractiveLimit() {
        final DesktopLargeProgramRenderPolicy policy = new DesktopLargeProgramRenderPolicy(3);

        assertFalse(policy.shouldUsePreview(
            3,
            false
        ));
        assertTrue(policy.shouldUsePreview(
            4,
            false
        ));
    }

    @Test
    void explicitFullRenderDisablesPreviewGuard() {
        final DesktopLargeProgramRenderPolicy policy = new DesktopLargeProgramRenderPolicy(3);

        assertFalse(policy.shouldUsePreview(
            1_000_000,
            true
        ));
    }

    @Test
    void summaryExplainsOperationCountAndLimit() {
        final DesktopLargeProgramRenderPolicy policy = new DesktopLargeProgramRenderPolicy(10);

        assertTrue(policy.summary(42).contains("42"));
        assertTrue(policy.summary(42).contains("10"));
    }

    @Test
    void rejectsInvalidLimit() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new DesktopLargeProgramRenderPolicy(0)
        );
    }
}
/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DesktopTextAreaConfiguratorTest {

    private final DesktopTextAreaConfigurator configurator = new DesktopTextAreaConfigurator();

    @BeforeAll
    static void startJavaFx() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean started = new AtomicBoolean(false);
        try {
            Platform.startup(() -> {
                started.set(true);
                latch.countDown();
            });
        } catch (final IllegalStateException ignored) {
            started.set(true);
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(started.get());
    }

    @Test
    void appliesEditableAndReadonlyModesToEveryArea() {
        final TextArea first = new TextArea();
        final TextArea second = new TextArea();

        configurator.editable(first, second);
        assertTrue(first.isEditable());
        assertTrue(second.isEditable());

        configurator.readonly(first, second);
        assertFalse(first.isEditable());
        assertFalse(second.isEditable());
    }

    @Test
    void appliesWrapAndNoWrapModesToEveryArea() {
        final TextArea first = new TextArea();
        final TextArea second = new TextArea();

        configurator.wrap(first, second);
        assertTrue(first.isWrapText());
        assertTrue(second.isWrapText());

        configurator.noWrap(first, second);
        assertFalse(first.isWrapText());
        assertFalse(second.isWrapText());
    }
}
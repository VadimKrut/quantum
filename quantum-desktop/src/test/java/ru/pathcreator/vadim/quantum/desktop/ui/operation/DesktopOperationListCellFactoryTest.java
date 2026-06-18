/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.operation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.scene.control.ListCell;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DesktopOperationListCellFactoryTest {

    private final DesktopOperationListCellFactory factory = new DesktopOperationListCellFactory();

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
    void createsDragAndDropCellWithAllRequiredHandlers() {
        final ListCell<String> cell = factory.create(
            index -> { },
            (source, target) -> { },
            () -> 7
        );

        assertNotNull(cell.getOnDragDetected());
        assertNotNull(cell.getOnDragOver());
        assertNotNull(cell.getOnDragDropped());
    }
}
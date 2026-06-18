/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.circuit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.geometry.BoundingBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DesktopCircuitScrollSupportTest {

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
    void ignoresMissingScrollPaneAndInvalidOperationIndex() {
        final Pane content = new Pane();
        final ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setHvalue(0.42);
        scrollPane.setVvalue(0.37);

        DesktopCircuitScrollSupport.scrollOperationIntoView(
            null,
            content,
            1,
            true
        );
        DesktopCircuitScrollSupport.scrollOperationIntoView(
            scrollPane,
            content,
            -1,
            true
        );

        assertEquals(0.42, scrollPane.getHvalue(), 0.0001);
        assertEquals(0.37, scrollPane.getVvalue(), 0.0001);
    }

    @Test
    void scrollsToVisibleOperationCellAndKeepsValuesBounded() {
        final Pane content = new Pane();
        content.resizeRelocate(
            0.0,
            0.0,
            2_000.0,
            1_200.0
        );
        final Label operation = new Label("X");
        operation.getProperties().put(
            "operationIndex",
            42
        );
        operation.getProperties().put(
            "operationSymbol",
            "X"
        );
        operation.resizeRelocate(
            1_600.0,
            800.0,
            80.0,
            48.0
        );
        content.getChildren().add(operation);
        content.setMinSize(
            2_000.0,
            1_200.0
        );
        final ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setViewportBounds(new BoundingBox(
            0.0,
            0.0,
            400.0,
            240.0
        ));

        DesktopCircuitScrollSupport.scrollOperationIntoView(
            scrollPane,
            content,
            42,
            true
        );

        assertTrue(scrollPane.getHvalue() > 0.5);
        assertTrue(scrollPane.getVvalue() > 0.5);
        assertTrue(scrollPane.getHvalue() <= 1.0);
        assertTrue(scrollPane.getVvalue() <= 1.0);
    }
}
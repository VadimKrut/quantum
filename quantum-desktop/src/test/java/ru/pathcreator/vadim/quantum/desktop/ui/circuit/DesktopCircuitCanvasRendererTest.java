/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.circuit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.desktop.workspace.DesktopIrOperationSpec;

class DesktopCircuitCanvasRendererTest {

    private final DesktopCircuitCanvasRenderer renderer = new DesktopCircuitCanvasRenderer();

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
    void rendersCanonicalVisualSymbolsWithoutMojibake() {
        final Node canvas = renderer.renderCircuit(
            List.of(
                new DesktopIrOperationSpec("CX", "q[0]", "q[1]", "q[0]", "c[0]", 0.0),
                new DesktopIrOperationSpec("SWAP", "q[0]", "q[1]", "q[0]", "c[0]", 0.0),
                new DesktopIrOperationSpec("BARRIER", "q[0]", "q[1]", "q[0]", "c[0]", 0.0)
            ),
            2,
            "q",
            false,
            false,
            -1,
            index -> { },
            qubit -> { },
            index -> { },
            (source, target) -> { }
        );
        final List<String> symbols = operationSymbols(canvas);

        assertTrue(symbols.contains(Character.toString((char) 0x25cf)));
        assertTrue(symbols.contains(Character.toString((char) 0x00d7)));
        assertTrue(symbols.contains(Character.toString((char) 0x258a)));
    }

    private static List<String> operationSymbols(final Node node) {
        final ArrayList<String> symbols = new ArrayList<>();
        collectOperationSymbols(
            node,
            symbols
        );
        return symbols;
    }

    private static void collectOperationSymbols(
        final Node node,
        final List<String> symbols
    ) {
        final Object symbol = node.getProperties().get("operationSymbol");
        if (symbol instanceof String text && !text.isBlank()) {
            symbols.add(text);
        }
        if (node instanceof Parent parent) {
            for (final Node child : parent.getChildrenUnmodifiable()) {
                collectOperationSymbols(
                    child,
                    symbols
                );
            }
        }
    }
}
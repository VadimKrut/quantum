/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.circuit;

import static ru.pathcreator.vadim.quantum.desktop.ui.audit.DesktopUiTraversal.collectNodes;

import java.util.ArrayList;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;

/**
 * Прокручивает визуальную схему к выбранной операции без знания о desktop workflow.
 */
public final class DesktopCircuitScrollSupport {

    private static final double FORCED_VERTICAL_LOOKAHEAD = 0.16;

    private DesktopCircuitScrollSupport() {
    }

    public static void scrollOperationIntoView(
        final ScrollPane scrollPane,
        final Node circuitRows,
        final int operationIndex,
        final boolean force
    ) {
        if (
            scrollPane == null
            || circuitRows == null
            || operationIndex < 0
            || (!force && !scrollPane.isVisible())
        ) {
            return;
        }
        final Bounds targetBounds = operationBoundsInCircuitRows(
            circuitRows,
            operationIndex
        );
        if (targetBounds == null) {
            return;
        }
        scrollToBounds(
            scrollPane,
            targetBounds,
            circuitRows.getLayoutBounds(),
            scrollPane.getViewportBounds()
        );
        if (force) {
            scrollPane.setVvalue(clamp01(scrollPane.getVvalue() + FORCED_VERTICAL_LOOKAHEAD));
        }
    }

    private static Bounds operationBoundsInCircuitRows(
        final Node circuitRows,
        final int operationIndex
    ) {
        final ArrayList<Node> nodes = new ArrayList<>();
        collectNodes(
            circuitRows,
            Node.class,
            nodes
        );
        Bounds bounds = null;
        for (int i = 0; i < nodes.size(); i++) {
            final Node node = nodes.get(i);
            if (!Integer.valueOf(operationIndex).equals(node.getProperties().get("operationIndex"))) {
                continue;
            }
            if (!isVisibleOperationCell(node)) {
                continue;
            }
            final Bounds nodeBounds = circuitRows.sceneToLocal(node.localToScene(node.getBoundsInLocal()));
            bounds = bounds == null
                ? nodeBounds
                : union(
                    bounds,
                    nodeBounds
                );
        }
        return bounds;
    }

    private static boolean isVisibleOperationCell(final Node node) {
        final Object connector = node.getProperties().get("operationConnector");
        final Object symbol = node.getProperties().get("operationSymbol");
        return Boolean.TRUE.equals(connector)
            || (
                symbol instanceof String text
                && !text.isBlank()
            );
    }

    private static void scrollToBounds(
        final ScrollPane scrollPane,
        final Bounds targetBounds,
        final Bounds contentBounds,
        final Bounds viewportBounds
    ) {
        final double horizontalRange = Math.max(
            1.0,
            contentBounds.getWidth() - viewportBounds.getWidth()
        );
        final double verticalRange = Math.max(
            1.0,
            contentBounds.getHeight() - viewportBounds.getHeight()
        );
        final double targetCenterX = targetBounds.getMinX() + targetBounds.getWidth() / 2.0;
        final double targetCenterY = targetBounds.getMinY() + targetBounds.getHeight() / 2.0;
        scrollPane.setHvalue(clamp01((targetCenterX - viewportBounds.getWidth() / 2.0) / horizontalRange));
        final double verticalPadding = Math.min(
            96.0,
            viewportBounds.getHeight() * 0.18
        );
        final double effectiveViewportHeight = Math.max(
            1.0,
            viewportBounds.getHeight() - verticalPadding * 2.0
        );
        scrollPane.setVvalue(clamp01((
            targetCenterY
                + verticalPadding
                - effectiveViewportHeight / 2.0
        ) / verticalRange));
    }

    private static Bounds union(
        final Bounds left,
        final Bounds right
    ) {
        final double minX = Math.min(
            left.getMinX(),
            right.getMinX()
        );
        final double minY = Math.min(
            left.getMinY(),
            right.getMinY()
        );
        final double maxX = Math.max(
            left.getMaxX(),
            right.getMaxX()
        );
        final double maxY = Math.max(
            left.getMaxY(),
            right.getMaxY()
        );
        return new BoundingBox(
            minX,
            minY,
            maxX - minX,
            maxY - minY
        );
    }

    private static double clamp01(final double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }
}
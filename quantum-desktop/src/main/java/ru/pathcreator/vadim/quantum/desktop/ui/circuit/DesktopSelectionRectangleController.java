/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.circuit;

import java.util.function.Consumer;
import javafx.geometry.Bounds;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

/**
 * Управляет drag-select рамкой визуальной схемы и отдаёт выбранную область вызывающему коду.
 */
public final class DesktopSelectionRectangleController {

    private double selectionStartX;
    private double selectionStartY;

    public void install(
        final StackPane canvasStack,
        final Rectangle selectionRectangle,
        final Consumer<Bounds> selectionConsumer
    ) {
        selectionRectangle.setManaged(false);
        canvasStack.setOnMousePressed(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            selectionStartX = event.getX();
            selectionStartY = event.getY();
            selectionRectangle.setLayoutX(selectionStartX);
            selectionRectangle.setLayoutY(selectionStartY);
            selectionRectangle.setWidth(0.0);
            selectionRectangle.setHeight(0.0);
            selectionRectangle.setVisible(true);
        });
        canvasStack.setOnMouseDragged(event -> {
            if (!selectionRectangle.isVisible()) {
                return;
            }
            final double minX = Math.min(
                selectionStartX,
                event.getX()
            );
            final double minY = Math.min(
                selectionStartY,
                event.getY()
            );
            selectionRectangle.setLayoutX(minX);
            selectionRectangle.setLayoutY(minY);
            selectionRectangle.setWidth(Math.abs(
                event.getX() - selectionStartX
            ));
            selectionRectangle.setHeight(Math.abs(
                event.getY() - selectionStartY
            ));
        });
        canvasStack.setOnMouseReleased(event -> {
            if (!selectionRectangle.isVisible()) {
                return;
            }
            selectionConsumer.accept(selectionRectangle.localToScene(
                selectionRectangle.getBoundsInLocal()
            ));
            selectionRectangle.setVisible(false);
        });
    }
}
/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.layout;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Строит панель пошаговой инспекции схемы с быстрым переходом по операциям.
 */
public final class DesktopInspectionControlsView {

    public Node build(
        final Label inspectionStepLabel,
        final Label inspectionOperationLabel,
        final Slider inspectionSlider,
        final String firstText,
        final String previousText,
        final String allText,
        final String nextText,
        final String lastText,
        final Runnable firstAction,
        final Runnable previousAction,
        final Runnable allAction,
        final Runnable nextAction,
        final Runnable lastAction
    ) {
        inspectionStepLabel.getStyleClass().add("status-chip");
        inspectionOperationLabel.getStyleClass().add("status-chip");
        inspectionSlider.getStyleClass().add("inspect-slider");
        final HBox buttons = new HBox(
            8.0,
            DesktopUiNodes.secondaryButton(
                firstText,
                firstAction
            ),
            DesktopUiNodes.secondaryButton(
                previousText,
                previousAction
            ),
            DesktopUiNodes.secondaryButton(
                allText,
                allAction
            ),
            DesktopUiNodes.secondaryButton(
                nextText,
                nextAction
            ),
            DesktopUiNodes.secondaryButton(
                lastText,
                lastAction
            )
        );
        buttons.setAlignment(Pos.CENTER_LEFT);
        final HBox status = new HBox(
            8.0,
            inspectionStepLabel,
            inspectionOperationLabel
        );
        status.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(
            inspectionOperationLabel,
            Priority.ALWAYS
        );
        return new VBox(
            8.0,
            buttons,
            inspectionSlider,
            status
        );
    }
}
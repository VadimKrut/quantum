/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.layout;

import static ru.pathcreator.vadim.quantum.desktop.ui.layout.DesktopUiNodes.addSetting;

import java.util.function.Function;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;

/**
 * Строит панель настроек выполнения, live-preview и conservative transform options.
 */
public final class DesktopExecutionSettingsView {

    public Node build(
        final TextField shotsField,
        final TextField seedField,
        final CheckBox fastBox,
        final CheckBox skipValidationBox,
        final CheckBox skipInspectionBox,
        final CheckBox skipPreflightBox,
        final CheckBox skipTransformationBox,
        final CheckBox skipSimulationBox,
        final CheckBox skipCompilerBox,
        final CheckBox skipBackendBox,
        final CheckBox autoSimulationBox,
        final CheckBox hideZeroProbabilityBox,
        final CheckBox registerBitOrderBox,
        final CheckBox canonicalizeParametersBox,
        final CheckBox removeIdentityBox,
        final CheckBox inlineCompositeBox,
        final CheckBox targetLoweringBox,
        final CheckBox showSimulationTextBox,
        final CheckBox showProbabilitiesBox,
        final CheckBox showStateVectorBox,
        final CheckBox showQSphereBox,
        final CheckBox showPhaseDisksBox,
        final CheckBox showGeneratedCodeBox,
        final Function<String, String> text
    ) {
        final GridPane grid = new GridPane();
        grid.setPadding(new Insets(16));
        grid.setHgap(10);
        grid.setVgap(10);
        addSetting(
            grid,
            0,
            text.apply("settingsShots"),
            shotsField
        );
        addSetting(
            grid,
            1,
            text.apply("settingsSeed"),
            seedField
        );
        addOptionRow(
            grid,
            2,
            text.apply("settingsExecution"),
            fastBox,
            skipValidationBox,
            skipInspectionBox,
            skipPreflightBox,
            skipTransformationBox,
            skipSimulationBox,
            skipCompilerBox,
            skipBackendBox
        );
        addOptionRow(
            grid,
            3,
            text.apply("settingsLiveUi"),
            autoSimulationBox,
            hideZeroProbabilityBox,
            registerBitOrderBox
        );
        addOptionRow(
            grid,
            4,
            text.apply("settingsPanels"),
            showSimulationTextBox,
            showProbabilitiesBox,
            showStateVectorBox,
            showQSphereBox,
            showPhaseDisksBox,
            showGeneratedCodeBox
        );
        addOptionRow(
            grid,
            5,
            text.apply("settingsTransform"),
            canonicalizeParametersBox,
            removeIdentityBox,
            inlineCompositeBox,
            targetLoweringBox
        );
        return grid;
    }

    private static void addOptionRow(
        final GridPane grid,
        final int row,
        final String label,
        final Node... options
    ) {
        grid.add(
            new Label(label),
            0,
            row
        );
        grid.add(
            new FlowPane(
                12,
                10,
                options
            ),
            1,
            row
        );
    }
}
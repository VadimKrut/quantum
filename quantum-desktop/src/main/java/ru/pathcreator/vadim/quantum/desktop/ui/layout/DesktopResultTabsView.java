/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.layout;

import java.util.List;
import java.util.function.Function;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;

/**
 * Собирает правую панель результатов native workspace и возвращает вкладки expert-режима.
 */
public final class DesktopResultTabsView {

    public DesktopResultTabsResult build(
        final TextArea overviewArea,
        final TextArea inspectorArea,
        final TextArea simulationArea,
        final Node probabilityNode,
        final Node stateVectorNode,
        final Node qSphereNode,
        final TextArea javaDslArea,
        final TextArea gateInfoArea,
        final TextArea assistantNotesArea,
        final TextArea fullIrSurfaceArea,
        final ListView<String> diagnosticList,
        final TextArea targetProfileArea,
        final TextArea resourcesArea,
        final TextArea preflightArea,
        final TextArea compatibilityArea,
        final TextArea transformationArea,
        final TextArea nativeJsonArea,
        final TextArea diagnosticsArea,
        final TextArea generatedArea,
        final boolean showSimulationText,
        final boolean showProbabilities,
        final boolean showStateVector,
        final boolean showQSphere,
        final Function<String, String> text
    ) {
        final Tab targetProfileTab = DesktopUiNodes.tab(
            text.apply("tabTargetProfile"),
            targetProfileArea
        );
        final Tab resourcesTab = DesktopUiNodes.tab(
            text.apply("tabResources"),
            resourcesArea
        );
        final Tab preflightTab = DesktopUiNodes.tab(
            text.apply("tabPreflight"),
            preflightArea
        );
        final Tab compatibilityTab = DesktopUiNodes.tab(
            text.apply("tabCompatibility"),
            compatibilityArea
        );
        final Tab transformTab = DesktopUiNodes.tab(
            text.apply("tabTransform"),
            transformationArea
        );
        final TabPane tabs = new TabPane(
            DesktopUiNodes.tab(
                text.apply("tabOverview"),
                overviewArea
            ),
            DesktopUiNodes.tab(
                text.apply("tabInspector"),
                inspectorArea
            ),
            DesktopUiNodes.tab(
                "Java DSL",
                javaDslArea
            ),
            DesktopUiNodes.tab(
                text.apply("tabGateInfo"),
                gateInfoArea
            ),
            DesktopUiNodes.tab(
                text.apply("tabAssistantNotes"),
                assistantNotesArea
            ),
            DesktopUiNodes.tab(
                text.apply("tabFullIrSurface"),
                fullIrSurfaceArea
            ),
            DesktopUiNodes.tab(
                text.apply("tabDiagnosticList"),
                diagnosticList
            ),
            targetProfileTab,
            resourcesTab,
            preflightTab,
            compatibilityTab,
            transformTab,
            DesktopUiNodes.tab(
                text.apply("tabNativeJson"),
                nativeJsonArea
            ),
            DesktopUiNodes.tab(
                text.apply("tabDiagnostics"),
                diagnosticsArea
            ),
            DesktopUiNodes.tab(
                text.apply("tabGeneratedExport"),
                generatedArea
            )
        );
        if (showSimulationText) {
            tabs.getTabs().add(
                2,
                DesktopUiNodes.tab(
                    text.apply("tabSimulation"),
                    simulationArea
                )
            );
        }
        if (showProbabilities) {
            tabs.getTabs().add(
                3,
                DesktopUiNodes.tab(
                    text.apply("tabProbabilities"),
                    probabilityNode
                )
            );
        }
        if (showStateVector) {
            tabs.getTabs().add(
                4,
                DesktopUiNodes.tab(
                    text.apply("tabStateVector"),
                    stateVectorNode
                )
            );
        }
        if (showQSphere) {
            tabs.getTabs().add(
                5,
                DesktopUiNodes.tab(
                    text.apply("tabQSphere"),
                    DesktopUiNodes.scrollable(qSphereNode)
                )
            );
        }
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        return new DesktopResultTabsResult(
            tabs,
            List.of(
                resourcesTab,
                preflightTab,
                compatibilityTab,
                transformTab
            )
        );
    }
}
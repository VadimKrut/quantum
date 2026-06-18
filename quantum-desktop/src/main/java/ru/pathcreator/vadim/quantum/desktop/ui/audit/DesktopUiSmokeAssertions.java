/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.audit;

import static ru.pathcreator.vadim.quantum.desktop.ui.audit.DesktopUiTraversal.collectNodes;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.Node;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.shape.Line;
import javafx.scene.shape.Sphere;
import javafx.stage.Stage;
import ru.pathcreator.vadim.quantum.domain.operation.OperationKind;

/**
 * Проверяет, что desktop UI реально отрисовал ключевые элементы визуализации.
 */
public final class DesktopUiSmokeAssertions {

    private static final int MAX_QSPHERE_MARKER_NODES = 260;

    private DesktopUiSmokeAssertions() {
    }

    public static void verifyAllGateButtons(
        final Stage stage,
        final List<String> gates
    ) {
        final ArrayList<Button> buttons = new ArrayList<>();
        collectNodes(
            stage.getScene().getRoot(),
            Button.class,
            buttons
        );
        for (int gateIndex = 0; gateIndex < gates.size(); gateIndex++) {
            boolean found = false;
            for (int buttonIndex = 0; buttonIndex < buttons.size(); buttonIndex++) {
                if (gates.get(gateIndex).equals(buttons.get(buttonIndex).getText())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalStateException("UI smoke did not find gate button: " + gates.get(gateIndex) + ".");
            }
        }
    }

    public static void verifyCircuitContainsEveryGate(
        final Stage stage,
        final List<String> gates
    ) {
        final ArrayList<Node> nodes = new ArrayList<>();
        collectNodes(
            stage.getScene().getRoot(),
            Node.class,
            nodes
        );
        for (int gateIndex = 0; gateIndex < gates.size(); gateIndex++) {
            boolean found = false;
            for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
                final Object gate = nodes.get(nodeIndex).getProperties().get("operationGate");
                if (gates.get(gateIndex).equals(gate)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalStateException("Rendered circuit does not contain gate: " + gates.get(gateIndex) + ".");
            }
        }
    }


    public static void verifyGateButtonsSelectAndUpdate(
        final Stage stage,
        final List<String> gates,
        final ComboBox<String> gateBox,
        final TextArea gateInfoArea
    ) {
        for (int gateIndex = 0; gateIndex < gates.size(); gateIndex++) {
            final String gate = gates.get(gateIndex);
            DesktopUiTraversal.fireVisibleButton(
                stage,
                gate
            );
            if (!gate.equals(gateBox.getValue())) {
                throw new IllegalStateException("Gate button did not select gate: " + gate + ".");
            }
            if (!gateInfoArea.getText().contains(gate)) {
                throw new IllegalStateException("Gate info did not update for gate: " + gate + ".");
            }
        }
    }

    public static void verifyFullIrSurface(final TextArea fullIrSurfaceArea) {
        final OperationKind[] kinds = OperationKind.values();
        for (int i = 0; i < kinds.length; i++) {
            if (!fullIrSurfaceArea.getText().contains(kinds[i].name())) {
                throw new IllegalStateException("Full IR Surface tab does not contain operation kind: " + kinds[i] + ".");
            }
        }
    }
    public static void verifyQSphereRendered(final Stage stage) {
        final ArrayList<SubScene> subScenes = new ArrayList<>();
        collectNodes(
            stage.getScene().getRoot(),
            SubScene.class,
            subScenes
        );
        if (subScenes.isEmpty()) {
            throw new IllegalStateException("Q-sphere SubScene was not rendered.");
        }
        final ArrayList<Sphere> spheres = new ArrayList<>();
        collectNodes(
            stage.getScene().getRoot(),
            Sphere.class,
            spheres
        );
        for (int i = 0; i < subScenes.size(); i++) {
            collectNodes(
                subScenes.get(i).getRoot(),
                Sphere.class,
                spheres
            );
        }
        if (spheres.size() < 2) {
            throw new IllegalStateException("Q-sphere does not contain enough 3D spheres.");
        }
    }

    public static void verifyQSphereMarkerCountIsBounded(final Node root) {
        final ArrayList<SubScene> subScenes = new ArrayList<>();
        collectNodes(
            root,
            SubScene.class,
            subScenes
        );
        final ArrayList<Sphere> spheres = new ArrayList<>();
        for (int i = 0; i < subScenes.size(); i++) {
            collectNodes(
                subScenes.get(i).getRoot(),
                Sphere.class,
                spheres
            );
        }
        if (spheres.size() > MAX_QSPHERE_MARKER_NODES) {
            throw new IllegalStateException("Q-sphere rendered too many 3D sphere nodes: " + spheres.size() + ".");
        }
    }

    public static void verifyQSphereSummaryBadge(final Node root) {
        final ArrayList<Label> labels = new ArrayList<>();
        collectNodes(
            root,
            Label.class,
            labels
        );
        for (int i = 0; i < labels.size(); i++) {
            if (labels.get(i).getStyleClass().contains("state-summary-badge")) {
                return;
            }
        }
        throw new IllegalStateException("Q-sphere large-state summary badge was not rendered. Labels: "
            + labelDebug(labels) + ".");
    }

    public static void verifyQSphereHasInteractionHandlers(final Node root) {
        final ArrayList<SubScene> subScenes = new ArrayList<>();
        collectNodes(
            root,
            SubScene.class,
            subScenes
        );
        if (subScenes.isEmpty()) {
            throw new IllegalStateException("Q-sphere SubScene was not available for interaction checks.");
        }
        for (int i = 0; i < subScenes.size(); i++) {
            final SubScene subScene = subScenes.get(i);
            if (subScene.getOnMousePressed() == null) {
                throw new IllegalStateException("Q-sphere SubScene does not handle mouse press.");
            }
            if (subScene.getOnMouseDragged() == null) {
                throw new IllegalStateException("Q-sphere SubScene does not handle mouse drag rotation.");
            }
            if (subScene.getOnScroll() == null) {
                throw new IllegalStateException("Q-sphere SubScene does not handle scroll zoom.");
            }
        }
    }

    public static void verifyCircuitHasRenderedCells(final Stage stage) {
        final ArrayList<Node> nodes = new ArrayList<>();
        collectNodes(
            stage.getScene().getRoot(),
            Node.class,
            nodes
        );
        int renderedCells = 0;
        int controlCells = 0;
        int swapCells = 0;
        int measureCells = 0;
        int resetCells = 0;
        int barrierCells = 0;
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getProperties().containsKey("operationIndex")) {
                renderedCells++;
            }
            if (nodes.get(i).getStyleClass().contains("circuit-control-cell")) {
                controlCells++;
            }
            if (nodes.get(i).getStyleClass().contains("circuit-swap-cell")) {
                swapCells++;
            }
            if (nodes.get(i).getStyleClass().contains("circuit-measure-cell")) {
                measureCells++;
            }
            if (nodes.get(i).getStyleClass().contains("circuit-reset-cell")) {
                resetCells++;
            }
            if (nodes.get(i).getStyleClass().contains("circuit-barrier-cell")) {
                barrierCells++;
            }
        }
        if (renderedCells == 0) {
            throw new IllegalStateException("Visual circuit does not contain rendered operation cells.");
        }
        if (controlCells == 0) {
            throw new IllegalStateException("Visual circuit does not contain rendered control cells.");
        }
        if (swapCells == 0) {
            throw new IllegalStateException("Visual circuit does not contain rendered swap cells.");
        }
        if (measureCells == 0) {
            throw new IllegalStateException("Visual circuit does not contain rendered measure cells.");
        }
        if (resetCells == 0) {
            throw new IllegalStateException("Visual circuit does not contain rendered reset cells.");
        }
        if (barrierCells == 0) {
            throw new IllegalStateException("Visual circuit does not contain rendered barrier cells.");
        }
        verifyConnectorLines(stage);
    }

    private static void verifyConnectorLines(final Stage stage) {
        final ArrayList<Line> lines = new ArrayList<>();
        collectNodes(
            stage.getScene().getRoot(),
            Line.class,
            lines
        );
        int connectorLines = 0;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).getStyleClass().contains("circuit-connector-line")) {
                connectorLines++;
            }
        }
        if (connectorLines == 0) {
            throw new IllegalStateException("Visual circuit does not contain connector overlay lines.");
        }
    }

    private static String labelDebug(final ArrayList<Label> labels) {
        final StringBuilder builder = new StringBuilder();
        final int limit = Math.min(
            labels.size(),
            12
        );
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                builder.append(" | ");
            }
            builder.append(labels.get(i).getText());
            builder.append(" ");
            builder.append(labels.get(i).getStyleClass());
        }
        return builder.toString();
    }
}
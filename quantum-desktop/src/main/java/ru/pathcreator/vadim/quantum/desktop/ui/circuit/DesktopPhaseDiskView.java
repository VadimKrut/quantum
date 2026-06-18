/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.circuit;

import java.util.ArrayList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.application.simulation.visualization.PhaseDiskProjection;
import ru.pathcreator.vadim.quantum.application.simulation.visualization.SingleQubitPhaseDiskState;

/**
 * Показывает локальные phase-disk индикаторы для каждого qubit по reduced state из state-vector.
 */
public final class DesktopPhaseDiskView {

    private static final double DISK_SIZE = 52.0;
    private static final int MAX_RENDERED_QUBITS = 18;

    private final PhaseDiskProjection projection = new PhaseDiskProjection();
    private final VBox root = new VBox(8.0);
    private boolean russian;

    public DesktopPhaseDiskView() {
        root.getStyleClass().add("phase-disk-panel");
        renderEmpty();
    }

    public Node node() {
        return root;
    }

    public void setRussian(final boolean russian) {
        this.russian = russian;
    }

    public void renderEmpty() {
        root.getChildren().setAll(
            title(),
            hint(russian
                ? "Фазовые диски появятся после успешной локальной state-vector симуляции."
                : "Phase disks are available after a successful local state-vector simulation.")
        );
    }

    public void render(final SimulationResult simulation) {
        if (
            simulation == null
            || !simulation.isSuccess()
            || simulation.stateVector().isEmpty()
        ) {
            renderEmpty();
            return;
        }
        final ArrayList<Node> nodes = new ArrayList<>();
        nodes.add(title());
        nodes.add(hint(russian
            ? "Локальное состояние qubit на текущем шаге инспекции."
            : "Local qubit state at the current inspect step."));
        final java.util.List<SingleQubitPhaseDiskState> states = projection.project(
            simulation,
            MAX_RENDERED_QUBITS
        );
        for (int i = 0; i < states.size(); i++) {
            final SingleQubitPhaseDiskState state = states.get(i);
            nodes.add(row(
                state.qubitIndex(),
                state
            ));
        }
        if (simulation.qubitCount() > states.size()) {
            nodes.add(hint(russian
                ? "Показаны первые " + states.size() + " из " + simulation.qubitCount()
                    + " qubits, чтобы интерфейс оставался отзывчивым."
                : "Showing first " + states.size() + " of " + simulation.qubitCount()
                    + " qubits to keep the workspace responsive."));
        }
        root.getChildren().setAll(nodes);
    }

    private Label title() {
        final Label label = new Label(russian ? "Фазовые диски" : "Phase disks");
        label.getStyleClass().add("phase-disk-title");
        return label;
    }

    private static Label hint(final String text) {
        final Label label = new Label(text);
        label.getStyleClass().add("phase-disk-hint");
        label.setWrapText(true);
        return label;
    }

    private static Node row(
        final int qubitIndex,
        final SingleQubitPhaseDiskState state
    ) {
        final Canvas canvas = new Canvas(
            DISK_SIZE,
            DISK_SIZE
        );
        drawDisk(
            canvas.getGraphicsContext2D(),
            state
        );
        final Label probabilityLabel = new Label("q[" + qubitIndex + "] p(|1>) "
            + percent(state.oneProbability()));
        probabilityLabel.getStyleClass().add("phase-disk-row-label");
        final Label purityLabel = new Label("purity " + percent(state.purity()));
        purityLabel.getStyleClass().add("phase-disk-row-subtitle");
        final VBox text = new VBox(
            2.0,
            probabilityLabel,
            purityLabel
        );
        final HBox row = new HBox(
            8.0,
            canvas,
            text
        );
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static void drawDisk(
        final GraphicsContext graphics,
        final SingleQubitPhaseDiskState state
    ) {
        final double radius = DISK_SIZE / 2.0 - 4.0;
        final double center = DISK_SIZE / 2.0;
        graphics.clearRect(
            0.0,
            0.0,
            DISK_SIZE,
            DISK_SIZE
        );
        graphics.setFill(Color.rgb(
            226,
            232,
            240
        ));
        graphics.fillOval(
            center - radius,
            center - radius,
            radius * 2.0,
            radius * 2.0
        );
        graphics.setFill(Color.rgb(
            59,
            130,
            246,
            0.72
        ));
        graphics.fillArc(
            center - radius,
            center - radius,
            radius * 2.0,
            radius * 2.0,
            -90.0,
            360.0 * state.oneProbability(),
            javafx.scene.shape.ArcType.ROUND
        );
        graphics.setStroke(phaseColor(state.phase()));
        graphics.setLineWidth(3.0);
        graphics.strokeLine(
            center,
            center,
            center + Math.cos(state.phase()) * radius,
            center + Math.sin(state.phase()) * radius
        );
        graphics.setStroke(Color.rgb(
            15,
            23,
            42,
            state.purity() < 0.98 ? 0.34 : 0.72
        ));
        graphics.setLineWidth(1.2);
        graphics.strokeOval(
            center - radius,
            center - radius,
            radius * 2.0,
            radius * 2.0
        );
    }

    private static Color phaseColor(final double phase) {
        final double hue = (Math.toDegrees(phase) + 360.0) % 360.0;
        return Color.hsb(
            hue,
            0.78,
            0.92
        );
    }

    private static String percent(final double value) {
        return String.format(
            java.util.Locale.ROOT,
            "%.1f%%",
            value * 100.0
        );
    }
}
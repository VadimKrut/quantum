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
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.application.simulation.result.StateVectorAmplitude;

/**
 * Показывает локальные phase-disk индикаторы для каждого qubit по reduced state из state-vector.
 */
public final class DesktopPhaseDiskView {

    private static final double DISK_SIZE = 52.0;
    private static final double EPSILON = 1.0E-12;

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
        final int qubitLimit = Math.min(
            simulation.qubitCount(),
            18
        );
        for (int qubitIndex = 0; qubitIndex < qubitLimit; qubitIndex++) {
            final SingleQubitState state = reducedState(
                simulation.stateVector(),
                simulation.qubitCount(),
                qubitIndex
            );
            nodes.add(row(
                qubitIndex,
                state
            ));
        }
        if (simulation.qubitCount() > qubitLimit) {
            nodes.add(hint(russian
                ? "Показаны первые " + qubitLimit + " из " + simulation.qubitCount()
                    + " qubits, чтобы интерфейс оставался отзывчивым."
                : "Showing first " + qubitLimit + " of " + simulation.qubitCount()
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
        final SingleQubitState state
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
        final SingleQubitState state
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

    private static SingleQubitState reducedState(
        final List<StateVectorAmplitude> amplitudes,
        final int qubitCount,
        final int qubitIndex
    ) {
        double p0 = 0.0;
        double p1 = 0.0;
        double coherenceReal = 0.0;
        double coherenceImaginary = 0.0;
        for (int i = 0; i < amplitudes.size(); i++) {
            final StateVectorAmplitude amplitude = amplitudes.get(i);
            final int bitPosition = qubitIndex;
            if (amplitude.basisState().charAt(bitPosition) == '0') {
                p0 += probability(amplitude);
                final int pairIndex = i ^ (1 << (qubitCount - bitPosition - 1));
                if (
                    pairIndex >= 0
                    && pairIndex < amplitudes.size()
                ) {
                    final StateVectorAmplitude other = amplitudes.get(pairIndex);
                    coherenceReal += amplitude.real() * other.real()
                        + amplitude.imaginary() * other.imaginary();
                    coherenceImaginary += amplitude.imaginary() * other.real()
                        - amplitude.real() * other.imaginary();
                }
            } else {
                p1 += probability(amplitude);
            }
        }
        final double x = 2.0 * coherenceReal;
        final double y = 2.0 * coherenceImaginary;
        final double z = p0 - p1;
        final double purity = Math.min(
            1.0,
            Math.sqrt(x * x + y * y + z * z)
        );
        final double phase = Math.atan2(
            y,
            Math.abs(x) < EPSILON && Math.abs(y) < EPSILON ? EPSILON : x
        );
        return new SingleQubitState(
            clamp(p1),
            phase,
            purity
        );
    }

    private static double probability(final StateVectorAmplitude amplitude) {
        return amplitude.real() * amplitude.real()
            + amplitude.imaginary() * amplitude.imaginary();
    }

    private static Color phaseColor(final double phase) {
        final double hue = (Math.toDegrees(phase) + 360.0) % 360.0;
        return Color.hsb(
            hue,
            0.78,
            0.92
        );
    }

    private static double clamp(final double value) {
        return Math.max(
            0.0,
            Math.min(
                1.0,
                value
            )
        );
    }

    private static String percent(final double value) {
        return String.format(
            java.util.Locale.ROOT,
            "%.1f%%",
            value * 100.0
        );
    }

    private record SingleQubitState(
        double oneProbability,
        double phase,
        double purity
    ) {
    }
}
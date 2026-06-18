/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.visualization;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.application.simulation.result.StateVectorAmplitude;

/**
 * Рисует пользовательские графики результата симуляции: probability histogram и state-vector amplitudes.
 */
public final class DesktopSimulationVisualizationsView {

    private static final int MAX_BARS = 96;
    private static final double ROW_HEIGHT = 26.0;
    private static final double MIN_BAR_WIDTH = 2.0;

    private final VBox probabilityRoot = new VBox(10.0);
    private final VBox stateVectorRoot = new VBox(10.0);
    private SimulationResult lastSimulation;
    private boolean lastHideZeroProbability;
    private boolean russian;

    public DesktopSimulationVisualizationsView() {
        probabilityRoot.getStyleClass().add("simulation-visual-panel");
        stateVectorRoot.getStyleClass().add("simulation-visual-panel");
        renderEmpty();
    }

    public Node probabilityNode() {
        return scrollable(probabilityRoot);
    }

    public Node stateVectorNode() {
        return scrollable(stateVectorRoot);
    }

    public void renderEmpty() {
        lastSimulation = null;
        probabilityRoot.getChildren().setAll(emptyCard(
            title("probabilities"),
            text("emptyProbabilities")
        ));
        stateVectorRoot.getChildren().setAll(emptyCard(
            title("statevector"),
            text("emptyStatevector")
        ));
    }

    public void setRussian(final boolean russian) {
        this.russian = russian;
    }

    public void render(
        final SimulationResult simulation,
        final boolean hideZeroProbability
    ) {
        lastSimulation = simulation;
        lastHideZeroProbability = hideZeroProbability;
        if (
            simulation == null
            || !simulation.isSuccess()
        ) {
            renderEmpty();
            return;
        }
        probabilityRoot.getChildren().setAll(probabilityPanel(
            simulation,
            hideZeroProbability,
            false
        ));
        stateVectorRoot.getChildren().setAll(stateVectorPanel(
            simulation,
            hideZeroProbability,
            false
        ));
    }

    private Node probabilityPanel(
        final SimulationResult simulation,
        final boolean hideZeroProbability,
        final boolean expanded
    ) {
        final List<ProbabilityRow> rows = probabilityRows(
            simulation,
            hideZeroProbability
        );
        return chartCard(
            title("probabilities"),
            text("probabilitySubtitle"),
            text("expandProbabilities"),
            () -> openExpanded(ChartKind.PROBABILITY),
            probabilityChart(rows),
            limitNotice(
                rows.size(),
                simulation.counts().size(),
                expanded
            )
        );
    }

    private Node stateVectorPanel(
        final SimulationResult simulation,
        final boolean hideZeroProbability,
        final boolean expanded
    ) {
        final List<StateVectorRow> rows = stateVectorRows(
            simulation,
            hideZeroProbability
        );
        return chartCard(
            title("statevector"),
            text("statevectorSubtitle"),
            text("expandStatevector"),
            () -> openExpanded(ChartKind.STATE_VECTOR),
            stateVectorChart(rows),
            limitNotice(
                rows.size(),
                simulation.stateVector().size(),
                expanded
            )
        );
    }

    private void openExpanded(final ChartKind kind) {
        if (lastSimulation == null) {
            return;
        }
        final Stage stage = new Stage();
        stage.setTitle(kind == ChartKind.PROBABILITY
            ? title("probabilities")
            : title("statevector"));
        final Node content = kind == ChartKind.PROBABILITY
            ? probabilityPanel(
                lastSimulation,
                lastHideZeroProbability,
                true
            )
            : stateVectorPanel(
                lastSimulation,
                lastHideZeroProbability,
                true
            );
        final BorderPane root = new BorderPane(content);
        root.getStyleClass().addAll(
            "quantum-root",
            "theme-dark"
        );
        final Scene scene = new Scene(
            root,
            1180.0,
            760.0
        );
        final String stylesheet = DesktopSimulationVisualizationsView.class
            .getResource("/ru/pathcreator/vadim/quantum/desktop/ui/quantum-desktop.css")
            .toExternalForm();
        scene.getStylesheets().add(stylesheet);
        stage.setScene(scene);
        stage.show();
    }

    private static Node chartCard(
        final String title,
        final String subtitle,
        final String expandText,
        final Runnable expandAction,
        final Node chart,
        final Node notice
    ) {
        final Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("chart-title");
        final Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("panel-hint");
        final Button expand = new Button(expandText);
        expand.getStyleClass().add("secondary-button");
        expand.setOnAction(event -> expandAction.run());
        final VBox header = new VBox(
            8.0,
            titleLabel,
            subtitleLabel,
            expand
        );
        header.setAlignment(Pos.CENTER_LEFT);
        final VBox card = new VBox(
            12.0,
            header,
            chart,
            notice
        );
        card.getStyleClass().add("chart-card");
        return card;
    }

    private static Node emptyCard(
        final String title,
        final String body
    ) {
        final Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("chart-title");
        final Label bodyLabel = new Label(body);
        bodyLabel.getStyleClass().add("panel-hint");
        final VBox card = new VBox(
            8.0,
            titleLabel,
            bodyLabel
        );
        card.getStyleClass().add("chart-card");
        return card;
    }

    private static Node scrollable(final Node node) {
        final ScrollPane scrollPane = new ScrollPane(node);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        return scrollPane;
    }

    private static Node probabilityChart(final List<ProbabilityRow> rows) {
        final VBox chart = new VBox(7.0);
        chart.setPadding(new Insets(2.0));
        double max = 1.0;
        for (int i = 0; i < rows.size(); i++) {
            max = Math.max(
                max,
                rows.get(i).probability()
            );
        }
        for (int i = 0; i < Math.min(rows.size(), MAX_BARS); i++) {
            final ProbabilityRow row = rows.get(i);
            chart.getChildren().add(barRow(
                row.label(),
                row.probability(),
                max,
                String.format(
                    java.util.Locale.ROOT,
                    "%.2f%%  (%d)",
                    row.probability() * 100.0,
                    row.count()
                ),
                Color.rgb(
                    37,
                    99,
                    235
                )
            ));
        }
        return chart;
    }

    private static Node stateVectorChart(final List<StateVectorRow> rows) {
        final VBox chart = new VBox(7.0);
        chart.setPadding(new Insets(2.0));
        double max = 1.0;
        for (int i = 0; i < rows.size(); i++) {
            max = Math.max(
                max,
                rows.get(i).magnitude()
            );
        }
        for (int i = 0; i < Math.min(rows.size(), MAX_BARS); i++) {
            final StateVectorRow row = rows.get(i);
            chart.getChildren().add(barRow(
                row.label(),
                row.magnitude(),
                max,
                String.format(
                    java.util.Locale.ROOT,
                    "|amp| %.6f   phase %.3f rad   %.6f%+.6fi",
                    row.magnitude(),
                    row.phase(),
                    row.real(),
                    row.imaginary()
                ),
                Color.hsb(
                    (Math.toDegrees(row.phase()) + 360.0) % 360.0,
                    0.70,
                    0.92
                )
            ));
        }
        return chart;
    }

    private static Node barRow(
        final String label,
        final double value,
        final double max,
        final String detail,
        final Color color
    ) {
        final Label labelNode = new Label(label);
        labelNode.getStyleClass().add("chart-axis-label");
        labelNode.setTooltip(new Tooltip(label));
        final Canvas canvas = new Canvas(
            380.0,
            ROW_HEIGHT
        );
        drawBar(
            canvas,
            value,
            max,
            color
        );
        final Label detailNode = new Label(detail);
        detailNode.getStyleClass().add("chart-value-label");
        detailNode.setWrapText(true);
        final VBox header = new VBox(
            2.0,
            labelNode,
            detailNode
        );
        final VBox row = new VBox(
            4.0,
            header,
            canvas
        );
        return row;
    }

    private static void drawBar(
        final Canvas canvas,
        final double value,
        final double max,
        final Color color
    ) {
        final GraphicsContext graphics = canvas.getGraphicsContext2D();
        final double width = canvas.getWidth();
        final double height = canvas.getHeight();
        graphics.clearRect(
            0.0,
            0.0,
            width,
            height
        );
        graphics.setFill(Color.rgb(
            148,
            163,
            184,
            0.16
        ));
        graphics.fillRoundRect(
            0.0,
            5.0,
            width,
            height - 10.0,
            8.0,
            8.0
        );
        final double normalized = max <= 0.0
            ? 0.0
            : value / max;
        graphics.setFill(color);
        graphics.fillRoundRect(
            0.0,
            5.0,
            Math.max(
                MIN_BAR_WIDTH,
                width * normalized
            ),
            height - 10.0,
            8.0,
            8.0
        );
    }

    private static Node limitNotice(
        final int shown,
        final int total,
        final boolean expanded
    ) {
        final Label label = new Label(shown < total
            ? "Showing " + Math.min(shown, MAX_BARS) + " of " + total
                + " rows to keep the desktop responsive."
            : (expanded ? "Expanded view. Rows: " + total + "." : "Rows: " + total + "."));
        label.getStyleClass().add("visualization-limit-label");
        return label;
    }

    private static List<ProbabilityRow> probabilityRows(
        final SimulationResult simulation,
        final boolean hideZeroProbability
    ) {
        final ArrayList<ProbabilityRow> rows = new ArrayList<>(simulation.counts().size());
        final double shots = Math.max(
            1.0,
            simulation.shots()
        );
        for (final Map.Entry<String, Long> entry : simulation.counts().entrySet()) {
            final double probability = entry.getValue().longValue() / shots;
            if (
                hideZeroProbability
                && probability == 0.0
            ) {
                continue;
            }
            rows.add(new ProbabilityRow(
                entry.getKey(),
                entry.getValue().longValue(),
                probability
            ));
        }
        rows.sort(Comparator
            .comparingDouble(ProbabilityRow::probability)
            .reversed()
            .thenComparing(ProbabilityRow::label));
        return rows;
    }

    private static List<StateVectorRow> stateVectorRows(
        final SimulationResult simulation,
        final boolean hideZeroProbability
    ) {
        final ArrayList<StateVectorRow> rows = new ArrayList<>(simulation.stateVector().size());
        for (final StateVectorAmplitude amplitude : simulation.stateVector()) {
            final double magnitude = Math.hypot(
                amplitude.real(),
                amplitude.imaginary()
            );
            if (
                hideZeroProbability
                && magnitude == 0.0
            ) {
                continue;
            }
            rows.add(new StateVectorRow(
                amplitude.basisState(),
                amplitude.real(),
                amplitude.imaginary(),
                magnitude,
                Math.atan2(
                    amplitude.imaginary(),
                    amplitude.real()
                )
            ));
        }
        rows.sort(Comparator
            .comparingDouble(StateVectorRow::magnitude)
            .reversed()
            .thenComparing(StateVectorRow::label));
        return rows;
    }

    private String title(final String key) {
        if (!russian) {
            return switch (key) {
                case "probabilities" -> "Measurement probability histogram";
                case "statevector" -> "Statevector amplitudes";
                default -> key;
            };
        }
        return switch (key) {
            case "probabilities" -> "Гистограмма вероятностей измерения";
            case "statevector" -> "Амплитуды вектора состояния";
            default -> key;
        };
    }

    private String text(final String key) {
        if (!russian) {
            return switch (key) {
                case "emptyProbabilities" -> "Run a local simulation to display measured bitstring probabilities.";
                case "emptyStatevector" -> "Run a local state-vector simulation to display amplitudes and phases.";
                case "probabilitySubtitle" -> "Counts are normalized by shots. Expand for a wide chart.";
                case "statevectorSubtitle" -> "Bars show magnitude; color hue follows quantum phase.";
                case "expandProbabilities" -> "Expand probabilities";
                case "expandStatevector" -> "Expand statevector";
                default -> key;
            };
        }
        return switch (key) {
            case "emptyProbabilities" -> "Запустите локальную симуляцию, чтобы увидеть вероятности bitstring.";
            case "emptyStatevector" -> "Запустите симуляцию вектора состояния, чтобы увидеть амплитуды и фазы.";
            case "probabilitySubtitle" -> "Результаты нормализованы по числу запусков. Разверните панель для широкого графика.";
            case "statevectorSubtitle" -> "Полосы показывают модуль, цвет соответствует фазе.";
            case "expandProbabilities" -> "Развернуть вероятности";
            case "expandStatevector" -> "Развернуть вектор состояния";
            default -> key;
        };
    }

    private enum ChartKind {
        PROBABILITY,
        STATE_VECTOR
    }

    private record ProbabilityRow(
        String label,
        long count,
        double probability
    ) {
    }

    private record StateVectorRow(
        String label,
        double real,
        double imaginary,
        double magnitude,
        double phase
    ) {
    }
}
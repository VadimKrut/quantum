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

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.application.simulation.visualization.MeasurementProbabilityRow;
import ru.pathcreator.vadim.quantum.application.simulation.visualization.SimulationChartProjection;
import ru.pathcreator.vadim.quantum.application.simulation.visualization.StateVectorDisplayRow;

/**
 * Рисует пользовательские графики результата симуляции: probability histogram и state-vector amplitudes.
 */
public final class DesktopSimulationVisualizationsView {

    private static final int MAX_BARS = 96;
    private static final int MAX_EXPANDED_BARS = 2048;
    private static final double ROW_HEIGHT = 26.0;
    private static final double MIN_BAR_WIDTH = 2.0;

    private final SimulationChartProjection chartProjection = new SimulationChartProjection();
    private final VBox probabilityRoot = new VBox(10.0);
    private final VBox stateVectorRoot = new VBox(10.0);
    private SimulationResult lastSimulation;
    private boolean lastHideZeroProbability;
    private boolean lastRegisterBitOrder;
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
        render(
            simulation,
            hideZeroProbability,
            false
        );
    }

    public void render(
        final SimulationResult simulation,
        final boolean hideZeroProbability,
        final boolean registerBitOrder
    ) {
        lastSimulation = simulation;
        lastHideZeroProbability = hideZeroProbability;
        lastRegisterBitOrder = registerBitOrder;
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
            registerBitOrder,
            false
        ));
        stateVectorRoot.getChildren().setAll(stateVectorPanel(
            simulation,
            hideZeroProbability,
            registerBitOrder,
            false
        ));
    }

    private Node probabilityPanel(
        final SimulationResult simulation,
        final boolean hideZeroProbability,
        final boolean registerBitOrder,
        final boolean expanded
    ) {
        final List<ProbabilityRow> rows = probabilityRows(
            simulation,
            hideZeroProbability,
            registerBitOrder
        );
        return chartCard(
            title("probabilities"),
            text("probabilitySubtitle") + System.lineSeparator()
                + bitOrderText(
                    simulation.classicalBitCount(),
                    registerBitOrder
                ),
            text("expandProbabilities"),
            () -> openExpanded(ChartKind.PROBABILITY),
            probabilityChart(
                rows,
                expanded
            ),
            limitNotice(
                rows.size(),
                simulation.counts().size(),
                expanded
                )
        );
    }

    private Node expandedProbabilityPanel(
        final SimulationResult simulation,
        final boolean hideZeroProbability,
        final boolean registerBitOrder
    ) {
        final List<ProbabilityRow> rows = probabilityRows(
            simulation,
            hideZeroProbability,
            registerBitOrder
        );
        return expandedChartCard(
            title("probabilities"),
            text("probabilitySubtitle") + System.lineSeparator()
                + bitOrderText(
                    simulation.classicalBitCount(),
                    registerBitOrder
                ),
            probabilityChart(
                rows,
                true
            ),
            limitNotice(
                rows.size(),
                simulation.counts().size(),
                true
            )
        );
    }

    private Node stateVectorPanel(
        final SimulationResult simulation,
        final boolean hideZeroProbability,
        final boolean registerBitOrder,
        final boolean expanded
    ) {
        final List<StateVectorRow> rows = stateVectorRows(
            simulation,
            hideZeroProbability,
            registerBitOrder
        );
        return chartCard(
            title("statevector"),
            text("statevectorSubtitle") + System.lineSeparator()
                + bitOrderText(
                    simulation.qubitCount(),
                    registerBitOrder
                ),
            text("expandStatevector"),
            () -> openExpanded(ChartKind.STATE_VECTOR),
            stateVectorChart(
                rows,
                expanded
            ),
            limitNotice(
                rows.size(),
                simulation.stateVector().size(),
                expanded
                )
        );
    }

    private Node expandedStateVectorPanel(
        final SimulationResult simulation,
        final boolean hideZeroProbability,
        final boolean registerBitOrder
    ) {
        final List<StateVectorRow> rows = stateVectorRows(
            simulation,
            hideZeroProbability,
            registerBitOrder
        );
        return expandedChartCard(
            title("statevector"),
            text("statevectorSubtitle") + System.lineSeparator()
                + bitOrderText(
                    simulation.qubitCount(),
                    registerBitOrder
                ),
            stateVectorChart(
                rows,
                true
            ),
            limitNotice(
                rows.size(),
                simulation.stateVector().size(),
                true
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
        final Node chart = kind == ChartKind.PROBABILITY
            ? expandedProbabilityPanel(
                lastSimulation,
                lastHideZeroProbability,
                lastRegisterBitOrder
            )
            : expandedStateVectorPanel(
                lastSimulation,
                lastHideZeroProbability,
                lastRegisterBitOrder
            );
        final TextArea details = expandedDetails(kind);
        VBox.setVgrow(
            chart,
            Priority.ALWAYS
        );
        VBox.setVgrow(
            details,
            Priority.ALWAYS
        );
        final VBox content = new VBox(
            12.0,
            chart,
            details
        );
        content.setPadding(new Insets(12.0));
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
        stage.setMinWidth(860.0);
        stage.setMinHeight(620.0);
        stage.show();
    }

    private TextArea expandedDetails(final ChartKind kind) {
        final TextArea details = new TextArea(kind == ChartKind.PROBABILITY
            ? probabilityDetails()
            : stateVectorDetails());
        details.setEditable(false);
        details.setWrapText(false);
        details.setPrefRowCount(14);
        details.getStyleClass().add("copyable-result-area");
        return details;
    }

    private String probabilityDetails() {
        final List<ProbabilityRow> rows = probabilityRows(
            lastSimulation,
            lastHideZeroProbability,
            lastRegisterBitOrder
        );
        final StringBuilder builder = new StringBuilder(256 + rows.size() * 56);
        builder.append(title("probabilities")).append(System.lineSeparator());
        builder.append(bitOrderText(
            lastSimulation.classicalBitCount(),
            lastRegisterBitOrder
        )).append(System.lineSeparator());
        builder.append("bitstring\tcount\tprobability\tpercent").append(System.lineSeparator());
        for (int i = 0; i < rows.size(); i++) {
            final ProbabilityRow row = rows.get(i);
            builder.append(row.label())
                .append('\t')
                .append(row.count())
                .append('\t')
                .append(row.probability())
                .append('\t')
                .append(String.format(
                    java.util.Locale.ROOT,
                    "%.6f%%",
                    row.probability() * 100.0
                ))
                .append(System.lineSeparator());
        }
        return builder.toString();
    }

    private String stateVectorDetails() {
        final List<StateVectorRow> rows = stateVectorRows(
            lastSimulation,
            lastHideZeroProbability,
            lastRegisterBitOrder
        );
        final StringBuilder builder = new StringBuilder(256 + rows.size() * 96);
        builder.append(title("statevector")).append(System.lineSeparator());
        builder.append(bitOrderText(
            lastSimulation.qubitCount(),
            lastRegisterBitOrder
        )).append(System.lineSeparator());
        builder.append("basis\treal\timaginary\tmagnitude\tprobability\tphase(rad)").append(System.lineSeparator());
        for (int i = 0; i < rows.size(); i++) {
            final StateVectorRow row = rows.get(i);
            builder.append(row.label())
                .append('\t')
                .append(row.real())
                .append('\t')
                .append(row.imaginary())
                .append('\t')
                .append(row.magnitude())
                .append('\t')
                .append(row.magnitude() * row.magnitude())
                .append('\t')
                .append(row.phase())
                .append(System.lineSeparator());
        }
        return builder.toString();
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

    private static Node expandedChartCard(
        final String title,
        final String subtitle,
        final Node chart,
        final Node notice
    ) {
        final Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("chart-title");
        final Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("panel-hint");
        final VBox header = new VBox(
            8.0,
            titleLabel,
            subtitleLabel
        );
        header.setAlignment(Pos.CENTER_LEFT);
        final VBox body = new VBox(
            12.0,
            header,
            chart,
            notice
        );
        body.getStyleClass().add("chart-card");
        final ScrollPane scrollPane = new ScrollPane(body);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.getStyleClass().add("expanded-chart-scroll");
        return scrollPane;
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

    private static Node probabilityChart(
        final List<ProbabilityRow> rows,
        final boolean expanded
    ) {
        final VBox chart = new VBox(7.0);
        chart.setPadding(new Insets(2.0));
        double max = 1.0;
        for (int i = 0; i < rows.size(); i++) {
            max = Math.max(
                max,
                rows.get(i).probability()
            );
        }
        final int limit = expanded ? MAX_EXPANDED_BARS : MAX_BARS;
        for (int i = 0; i < Math.min(rows.size(), limit); i++) {
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

    private static Node stateVectorChart(
        final List<StateVectorRow> rows,
        final boolean expanded
    ) {
        final VBox chart = new VBox(7.0);
        chart.setPadding(new Insets(2.0));
        double max = 1.0;
        for (int i = 0; i < rows.size(); i++) {
            max = Math.max(
                max,
                rows.get(i).magnitude()
            );
        }
        final int limit = expanded ? MAX_EXPANDED_BARS : MAX_BARS;
        for (int i = 0; i < Math.min(rows.size(), limit); i++) {
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
        final int limit = expanded ? MAX_EXPANDED_BARS : MAX_BARS;
        final Label label = new Label(shown < total
            ? "Showing " + Math.min(shown, limit) + " of " + total
                + " rows to keep the desktop responsive."
            : (expanded ? "Expanded view. Rows: " + total + "." : "Rows: " + total + "."));
        label.getStyleClass().add("visualization-limit-label");
        return label;
    }

    private List<ProbabilityRow> probabilityRows(
        final SimulationResult simulation,
        final boolean hideZeroProbability,
        final boolean registerBitOrder
    ) {
        final List<MeasurementProbabilityRow> projectedRows = chartProjection.measurementRows(
            simulation,
            hideZeroProbability
        );
        final ArrayList<ProbabilityRow> rows = new ArrayList<>(projectedRows.size());
        for (int i = 0; i < projectedRows.size(); i++) {
            final MeasurementProbabilityRow projectedRow = projectedRows.get(i);
            rows.add(new ProbabilityRow(
                displayState(
                    projectedRow.basisState(),
                    registerBitOrder
                ),
                projectedRow.count(),
                projectedRow.probability()
            ));
        }
        return rows;
    }

    private List<StateVectorRow> stateVectorRows(
        final SimulationResult simulation,
        final boolean hideZeroProbability,
        final boolean registerBitOrder
    ) {
        final List<StateVectorDisplayRow> projectedRows = chartProjection.stateVectorRows(
            simulation,
            hideZeroProbability
        );
        final ArrayList<StateVectorRow> rows = new ArrayList<>(projectedRows.size());
        for (int i = 0; i < projectedRows.size(); i++) {
            final StateVectorDisplayRow projectedRow = projectedRows.get(i);
            rows.add(new StateVectorRow(
                displayState(
                    projectedRow.basisState(),
                    registerBitOrder
                ),
                projectedRow.real(),
                projectedRow.imaginary(),
                projectedRow.magnitude(),
                projectedRow.phase()
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

    private String bitOrderText(
        final int bitCount,
        final boolean registerBitOrder
    ) {
        if (!russian) {
            return registerBitOrder
                ? "Bitstring order: register order [0.." + Math.max(0, bitCount - 1) + "]."
                : "Bitstring order: standard MSB-first [" + Math.max(0, bitCount - 1) + "..0], matching Qiskit/Aer.";
        }
        return registerBitOrder
            ? "Порядок bitstring: порядок регистров [0.." + Math.max(0, bitCount - 1) + "]."
            : "Порядок bitstring: стандартный MSB-first [" + Math.max(0, bitCount - 1) + "..0], как в Qiskit/Aer.";
    }

    private static String displayState(
        final String state,
        final boolean registerBitOrder
    ) {
        if (!registerBitOrder) {
            return state;
        }
        final char[] characters = state.toCharArray();
        for (int left = 0, right = characters.length - 1; left < right; left++, right--) {
            final char temporary = characters[left];
            characters[left] = characters[right];
            characters[right] = temporary;
        }
        return new String(characters);
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
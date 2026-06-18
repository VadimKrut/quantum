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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;
import ru.pathcreator.vadim.quantum.desktop.workspace.DesktopIrOperationSpec;

/**
 * Отрисовывает native IR circuit как сетку gate-операций и отдельный графический слой связей.
 */
public final class DesktopCircuitCanvasRenderer {

    private static final double WIRE_LABEL_WIDTH = 58.0;
    private static final double DEFAULT_ROW_HEIGHT = 42.0;
    private static final double COLUMN_GAP = 6.0;
    private static final double DEFAULT_ROW_GAP = 7.0;
    private static final String CONTROL_SYMBOL = Character.toString((char) 0x25cf);
    private static final String BARRIER_SYMBOL = Character.toString((char) 0x258a);
    private static final String SWAP_SYMBOL = Character.toString((char) 0x00d7);

    public Node renderCircuit(
        final List<DesktopIrOperationSpec> operations,
        final int qubitCount,
        final String quantumRegisterName,
        final boolean lsbWireOrder,
        final boolean layerView,
        final int inspectionStepIndex,
        final IntConsumer operationSelection,
        final Consumer<String> qubitPlacement,
        final IntConsumer dragStart,
        final BiConsumer<Integer, Integer> operationDrop
    ) {
        final double rowHeight = rowHeight(qubitCount);
        final GridPane grid = new GridPane();
        grid.setHgap(COLUMN_GAP);
        grid.setVgap(rowGap(qubitCount));
        grid.setAlignment(Pos.TOP_LEFT);
        final Pane connectorLayer = new Pane();
        connectorLayer.setMouseTransparent(true);
        connectorLayer.setPickOnBounds(false);
        final StackPane root = new StackPane(
            connectorLayer,
            grid
        );
        StackPane.setAlignment(
            connectorLayer,
            Pos.TOP_LEFT
        );
        StackPane.setAlignment(
            grid,
            Pos.TOP_LEFT
        );
        final Node[][] cells = new Node[operations.size()][qubitCount];
        addStepHeader(
            grid,
            operations
        );
        final String qreg = quantumRegisterName.isBlank()
            ? "q"
            : quantumRegisterName;
        for (int wireRow = 0; wireRow < qubitCount; wireRow++) {
            final int qubitIndex = wireIndex(
                wireRow,
                qubitCount,
                lsbWireOrder
            );
            final String qubit = qreg + "[" + qubitIndex + "]";
            final Label wire = new Label(qubit);
            wire.setMinWidth(WIRE_LABEL_WIDTH);
            wire.getStyleClass().add("wire-label");
            GridPane.setHalignment(
                wire,
                HPos.LEFT
            );
            grid.add(
                wire,
                0,
                wireRow + 1
            );
            for (int operationIndex = 0; operationIndex < operations.size(); operationIndex++) {
                final Node cell = operationCell(
                    qubit,
                    qubitIndex,
                    operations.get(operationIndex),
                    operationIndex,
                    operations.size(),
                    rowHeight,
                    layerView,
                    inspectionStepIndex,
                    operationSelection,
                    dragStart,
                    operationDrop
                );
                cells[operationIndex][wireRow] = cell;
                grid.add(
                    cell,
                    operationIndex + 1,
                    wireRow + 1
                );
            }
            grid.add(
                dropCell(
                    qubit,
                    operations.size(),
                    rowHeight,
                    qubitPlacement,
                    operationDrop
                ),
                operations.size() + 1,
                wireRow + 1
            );
        }
        Platform.runLater(() -> drawConnectors(
            connectorLayer,
            cells,
            operations,
            qubitCount,
            lsbWireOrder,
            inspectionStepIndex
        ));
        return root;
    }

    private static void addStepHeader(
        final GridPane grid,
        final List<DesktopIrOperationSpec> operations
    ) {
        final Label spacer = stepLabel("step");
        spacer.setMinWidth(WIRE_LABEL_WIDTH);
        grid.add(
            spacer,
            0,
            0
        );
        for (int i = 0; i < operations.size(); i++) {
            grid.add(
                stepLabel(Integer.toString(i)),
                i + 1,
                0
            );
        }
        grid.add(
            stepLabel("+"),
            operations.size() + 1,
            0
        );
    }

    private static Label stepLabel(final String text) {
        final Label label = new Label(text);
        label.setAlignment(Pos.CENTER);
        label.setMinSize(
            58.0,
            24.0
        );
        label.getStyleClass().add("step-index-cell");
        return label;
    }

    private Node operationCell(
        final String qubit,
        final int qubitIndex,
        final DesktopIrOperationSpec operation,
        final int operationIndex,
        final int operationCount,
        final double rowHeight,
        final boolean layerView,
        final int inspectionStepIndex,
        final IntConsumer operationSelection,
        final IntConsumer dragStart,
        final BiConsumer<Integer, Integer> operationDrop
    ) {
        final String symbol = operationSymbol(
            qubit,
            operation
        );
        final Label label = new Label(symbol);
        label.setAlignment(Pos.CENTER);
        label.setMinSize(
            dropCellWidth(operationCount),
            rowHeight
        );
        label.getStyleClass().addAll(cellStyleClasses(
            symbol,
            operation,
            layerView
        ));
        final StackPane cell = new StackPane(label);
        cell.getStyleClass().add("circuit-operation-cell");
        cell.setOnMouseClicked(event -> {
            operationSelection.accept(operationIndex);
            event.consume();
        });
        final boolean endpointCell = isEndpointCell(
            qubit,
            operation
        );
        final boolean interiorConnectorCell = isInteriorConnectorCell(
            qubitIndex,
            operation
        );
        if (inspectionStepIndex >= 0) {
            if (operationIndex == inspectionStepIndex) {
                if (
                    endpointCell
                    || interiorConnectorCell
                    || !symbol.isBlank()
                ) {
                    cell.getStyleClass().add("inspect-current-cell");
                    if (endpointCell) {
                        cell.getStyleClass().add("inspect-current-endpoint-cell");
                    } else if (interiorConnectorCell) {
                        cell.getStyleClass().add("inspect-current-connector-cell");
                    }
                }
            } else if (operationIndex > inspectionStepIndex) {
                cell.getStyleClass().add("inspect-future-cell");
            }
        }
        cell.getProperties().put(
            "operationIndex",
            operationIndex
        );
        cell.getProperties().put(
            "operationGate",
            operation.gate()
        );
        cell.getProperties().put(
            "operationSymbol",
            symbol
        );
        cell.getProperties().put(
            "operationConnector",
            endpointCell || interiorConnectorCell
        );
        configureDragAndDrop(
            cell,
            operationIndex,
            dragStart,
            operationDrop
        );
        return cell;
    }

    private static void configureDragAndDrop(
        final StackPane cell,
        final int operationIndex,
        final IntConsumer dragStart,
        final BiConsumer<Integer, Integer> operationDrop
    ) {
        cell.setOnDragDetected(event -> {
            if (cell.getScene() == null) {
                event.consume();
                return;
            }
            dragStart.accept(operationIndex);
            final Dragboard dragboard;
            try {
                dragboard = cell.startDragAndDrop(TransferMode.MOVE);
            } catch (final IllegalStateException exception) {
                event.consume();
                return;
            }
            final ClipboardContent content = new ClipboardContent();
            content.putString(Integer.toString(operationIndex));
            dragboard.setContent(content);
            event.consume();
        });
        cell.setOnDragOver(event -> {
            if (event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });
        cell.setOnDragDropped(event -> {
            if (event.getDragboard().hasString()) {
                operationDrop.accept(
                    Integer.parseInt(event.getDragboard().getString()),
                    operationIndex
                );
                event.setDropCompleted(true);
            }
            event.consume();
        });
    }

    private Node dropCell(
        final String qubit,
        final int operationCount,
        final double rowHeight,
        final Consumer<String> qubitPlacement,
        final BiConsumer<Integer, Integer> operationDrop
    ) {
        final Label label = new Label("+");
        label.setAlignment(Pos.CENTER);
        label.setMinSize(
            cellWidth(operationCount),
            rowHeight
        );
        label.getStyleClass().add("circuit-drop-cell");
        label.setOnMouseClicked(event -> qubitPlacement.accept(qubit));
        final StackPane cell = new StackPane(label);
        cell.setOnDragOver(event -> {
            if (event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });
        cell.setOnDragDropped(event -> {
            if (event.getDragboard().hasString()) {
                operationDrop.accept(
                    Integer.parseInt(event.getDragboard().getString()),
                    operationCount
                );
                event.setDropCompleted(true);
            }
            event.consume();
        });
        return cell;
    }

    private static void drawConnectors(
        final Pane connectorLayer,
        final Node[][] cells,
        final List<DesktopIrOperationSpec> operations,
        final int qubitCount,
        final boolean lsbWireOrder,
        final int inspectionStepIndex
    ) {
        connectorLayer.getChildren().clear();
        if (
            qubitCount == 0
            || operations.isEmpty()
        ) {
            return;
        }
        drawWireLines(
            connectorLayer,
            cells,
            qubitCount,
            operations.size()
        );
        final int operationLimit = Math.min(
            operations.size(),
            cells.length
        );
        for (int operationIndex = 0; operationIndex < operationLimit; operationIndex++) {
            final DesktopIrOperationSpec operation = operations.get(operationIndex);
            if (!isConnectedGate(operation.gate())) {
                continue;
            }
            final int[] rows = connectedRows(
                operation,
                qubitCount,
                lsbWireOrder
            );
            if (rows.length < 2) {
                continue;
            }
            final int minRow = min(rows);
            final int maxRow = max(rows);
            if (
                minRow < 0
                || maxRow >= cells[operationIndex].length
            ) {
                continue;
            }
            final Node minCell = cells[operationIndex][minRow];
            final Node maxCell = cells[operationIndex][maxRow];
            final Bounds minBounds = minCell.getBoundsInParent();
            final Bounds maxBounds = maxCell.getBoundsInParent();
            final double x = minBounds.getMinX() + minBounds.getWidth() / 2.0;
            final double y1 = minBounds.getMinY() + minBounds.getHeight() / 2.0;
            final double y2 = maxBounds.getMinY() + maxBounds.getHeight() / 2.0;
            final Line line = new Line(
                x,
                y1,
                x,
                y2
            );
            line.getStyleClass().add("circuit-connector-line");
            if (operationIndex == inspectionStepIndex) {
                line.getStyleClass().add("inspect-current-connector-line");
            }
            if (
                inspectionStepIndex >= 0
                && operationIndex > inspectionStepIndex
            ) {
                line.getStyleClass().add("inspect-future-cell");
            }
            connectorLayer.getChildren().add(line);
        }
    }

    private static void drawWireLines(
        final Pane connectorLayer,
        final Node[][] cells,
        final int qubitCount,
        final int operationCount
    ) {
        if (cells.length == 0) {
            return;
        }
        final int actualOperationCount = Math.min(
            operationCount,
            cells.length
        );
        if (actualOperationCount == 0) {
            return;
        }
        final int actualQubitCount = Math.min(
            qubitCount,
            Math.min(
                cells[0].length,
                cells[actualOperationCount - 1].length
            )
        );
        for (int row = 0; row < actualQubitCount; row++) {
            final Node firstCell = cells[0][row];
            final Node lastCell = cells[actualOperationCount - 1][row];
            final Bounds firstBounds = firstCell.getBoundsInParent();
            final Bounds lastBounds = lastCell.getBoundsInParent();
            final double y = firstBounds.getMinY() + firstBounds.getHeight() / 2.0;
            final Line line = new Line(
                firstBounds.getMinX(),
                y,
                lastBounds.getMaxX(),
                y
            );
            line.getStyleClass().add("circuit-wire-line");
            connectorLayer.getChildren().add(line);
        }
    }

    private static int[] connectedRows(
        final DesktopIrOperationSpec operation,
        final int qubitCount,
        final boolean lsbWireOrder
    ) {
        final ArrayList<Integer> rows = new ArrayList<>(3);
        addReferenceRow(
            rows,
            operation.primaryQubit(),
            qubitCount,
            lsbWireOrder
        );
        addReferenceRow(
            rows,
            operation.secondaryQubit(),
            qubitCount,
            lsbWireOrder
        );
        if ("CCX".equals(operation.gate())) {
            addReferenceRow(
                rows,
                operation.tertiaryQubit(),
                qubitCount,
                lsbWireOrder
            );
        }
        final int[] result = new int[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            result[i] = rows.get(i);
        }
        return result;
    }

    private static void addReferenceRow(
        final ArrayList<Integer> rows,
        final String reference,
        final int qubitCount,
        final boolean lsbWireOrder
    ) {
        final int index = referenceIndex(reference);
        if (
            index < 0
            || index >= qubitCount
        ) {
            return;
        }
        final int row = lsbWireOrder
            ? qubitCount - index - 1
            : index;
        if (!rows.contains(row)) {
            rows.add(row);
        }
    }

    private static String operationSymbol(
        final String qubit,
        final DesktopIrOperationSpec operation
    ) {
        if (operation.gate().startsWith("CUSTOM:")) {
            return qubit.equals(operation.primaryQubit()) ? "GROUP" : "";
        }
        if (operation.gate().startsWith("IR:")) {
            return qubit.equals(operation.primaryQubit()) ? "IR" : "";
        }
        if (qubit.equals(operation.primaryQubit())) {
            return switch (operation.gate()) {
                case "CX", "CY", "CZ", "CPHASE", "CH", "CCX" -> CONTROL_SYMBOL;
                case "MEASURE" -> "M";
                case "RESET" -> "R";
                case "BARRIER" -> BARRIER_SYMBOL;
                case "SWAP" -> SWAP_SYMBOL;
                case "DELAY" -> "D";
                case "LABEL" -> "LBL";
                case "BRANCH" -> "BR";
                case "TIMING_BOX" -> "TB";
                case "ASSIGN" -> "AS";
                case "DECLARE" -> "DEC";
                case "ARRAY" -> "ARR";
                case "CALL" -> "CALL";
                case "IF_X" -> "IF";
                case "CTRL_X" -> "CIF";
                case "BLOCK" -> "BLK";
                case "IF_BLOCK" -> "IFB";
                case "FOR" -> "FOR";
                case "SYM_FOR" -> "SFOR";
                case "WHILE" -> "WH";
                default -> operation.gate();
            };
        }
        if (qubit.equals(operation.secondaryQubit())) {
            return switch (operation.gate()) {
                case "CX" -> "X";
                case "CY" -> "Y";
                case "CZ" -> "Z";
                case "CPHASE" -> "P";
                case "CH" -> "H";
                case "SWAP" -> SWAP_SYMBOL;
                case "BARRIER" -> BARRIER_SYMBOL;
                case "CCX" -> CONTROL_SYMBOL;
                case "DELAY" -> "D";
                default -> "";
            };
        }
        if (
            qubit.equals(operation.tertiaryQubit())
            && "CCX".equals(operation.gate())
        ) {
            return "X";
        }
        if (isInteriorConnectorCell(
            referenceIndex(qubit),
            operation
        )) {
            return "";
        }
        return "";
    }

    private static List<String> cellStyleClasses(
        final String symbol,
        final DesktopIrOperationSpec operation,
        final boolean layerView
    ) {
        final ArrayList<String> classes = new ArrayList<>();
        if (symbol.isBlank()) {
            classes.add(layerView ? "circuit-empty-layer-cell" : "circuit-empty-cell");
            return classes;
        }
        if (CONTROL_SYMBOL.equals(symbol)) {
            classes.add("circuit-control-cell");
        } else if ("M".equals(symbol)) {
            classes.add("circuit-measure-cell");
        } else if ("R".equals(symbol)) {
            classes.add("circuit-reset-cell");
        } else if (SWAP_SYMBOL.equals(symbol)) {
            classes.add("circuit-swap-cell");
        } else if (BARRIER_SYMBOL.equals(symbol)) {
            classes.add("circuit-barrier-cell");
        } else if (operation.gate().startsWith("CUSTOM:")) {
            classes.add("circuit-custom-cell");
        } else if (operation.gate().startsWith("IR:")) {
            classes.add("circuit-custom-cell");
        } else {
            classes.add("circuit-gate-cell");
        }
        return classes;
    }

    private static boolean isEndpointCell(
        final String qubit,
        final DesktopIrOperationSpec operation
    ) {
        return qubit.equals(operation.primaryQubit())
            || qubit.equals(operation.secondaryQubit())
            || qubit.equals(operation.tertiaryQubit());
    }

    private static boolean isInteriorConnectorCell(
        final int qubitIndex,
        final DesktopIrOperationSpec operation
    ) {
        if (
            qubitIndex < 0
            || !isConnectedGate(operation.gate())
        ) {
            return false;
        }
        final int primary = referenceIndex(operation.primaryQubit());
        final int secondary = referenceIndex(operation.secondaryQubit());
        final int tertiary = "CCX".equals(operation.gate())
            ? referenceIndex(operation.tertiaryQubit())
            : -1;
        final int min = minPositive(
            primary,
            secondary,
            tertiary
        );
        final int max = maxPositive(
            primary,
            secondary,
            tertiary
        );
        return min >= 0
            && max >= 0
            && qubitIndex > min
            && qubitIndex < max;
    }

    private static boolean isConnectedGate(final String gate) {
        return switch (gate) {
            case "CX", "CY", "CZ", "CPHASE", "CH", "SWAP", "CCX", "BARRIER", "DELAY" -> true;
            default -> false;
        };
    }

    private static int wireIndex(
        final int wireRow,
        final int qubitCount,
        final boolean lsbWireOrder
    ) {
        return lsbWireOrder
            ? qubitCount - wireRow - 1
            : wireRow;
    }

    private static int referenceIndex(final String reference) {
        if (reference == null) {
            return -1;
        }
        final int open = reference.indexOf('[');
        final int close = reference.indexOf(']');
        if (
            open < 0
            || close <= open
        ) {
            return -1;
        }
        try {
            return Integer.parseInt(reference.substring(
                open + 1,
                close
            ));
        } catch (final NumberFormatException exception) {
            return -1;
        }
    }

    private static int min(final int[] values) {
        int result = Integer.MAX_VALUE;
        for (int i = 0; i < values.length; i++) {
            result = Math.min(
                result,
                values[i]
            );
        }
        return result;
    }

    private static int max(final int[] values) {
        int result = -1;
        for (int i = 0; i < values.length; i++) {
            result = Math.max(
                result,
                values[i]
            );
        }
        return result;
    }

    private static int minPositive(
        final int first,
        final int second,
        final int third
    ) {
        int result = Integer.MAX_VALUE;
        if (first >= 0) {
            result = Math.min(
                result,
                first
            );
        }
        if (second >= 0) {
            result = Math.min(
                result,
                second
            );
        }
        if (third >= 0) {
            result = Math.min(
                result,
                third
            );
        }
        return result == Integer.MAX_VALUE ? -1 : result;
    }

    private static int maxPositive(
        final int first,
        final int second,
        final int third
    ) {
        int result = -1;
        if (first >= 0) {
            result = Math.max(
                result,
                first
            );
        }
        if (second >= 0) {
            result = Math.max(
                result,
                second
            );
        }
        if (third >= 0) {
            result = Math.max(
                result,
                third
            );
        }
        return result;
    }

    private static double cellWidth(final int operationCount) {
        if (operationCount >= 80) {
            return 38.0;
        }
        if (operationCount >= 40) {
            return 46.0;
        }
        return 58.0;
    }

    private static double dropCellWidth(final int operationCount) {
        return Math.min(
            cellWidth(operationCount),
            34.0
        );
    }

    private static double rowHeight(final int qubitCount) {
        if (qubitCount >= 24) {
            return 26.0;
        }
        if (qubitCount >= 16) {
            return 30.0;
        }
        if (qubitCount >= 12) {
            return 34.0;
        }
        return DEFAULT_ROW_HEIGHT;
    }

    private static double rowGap(final int qubitCount) {
        if (qubitCount >= 16) {
            return 4.0;
        }
        if (qubitCount >= 12) {
            return 5.0;
        }
        return DEFAULT_ROW_GAP;
    }
}
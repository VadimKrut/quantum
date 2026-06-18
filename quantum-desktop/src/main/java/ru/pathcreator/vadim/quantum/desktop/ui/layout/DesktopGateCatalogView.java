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
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/**
 * Рендерит палитру gates visual builder и хранит единый порядок gate-кнопок.
 */
public final class DesktopGateCatalogView {

    private static final List<String> GATES = List.of(
        "H",
        "X",
        "Y",
        "Z",
        "S",
        "T",
        "RX",
        "RY",
        "RZ",
        "PHASE",
        "CX",
        "CY",
        "CZ",
        "CH",
        "SWAP",
        "CCX",
        "MEASURE",
        "RESET",
        "BARRIER"
    );

    public List<String> gates() {
        return GATES;
    }

    public void refresh(
        final VBox catalogPane,
        final String filterText,
        final String selectedGate,
        final Consumer<String> gateConsumer,
        final Consumer<String> gateHelpConsumer,
        final Function<String, String> text
    ) {
        catalogPane.getChildren().clear();
        final String filter = filterText == null
            ? ""
            : filterText.trim().toUpperCase();
        addGroup(
            catalogPane,
            text.apply("gateGroupSingle"),
            filter,
            selectedGate,
            gateConsumer,
            gateHelpConsumer,
            "H",
            "X",
            "Y",
            "Z"
        );
        addGroup(
            catalogPane,
            text.apply("gateGroupPhase"),
            filter,
            selectedGate,
            gateConsumer,
            gateHelpConsumer,
            "S",
            "T",
            "RX",
            "RY",
            "RZ",
            "PHASE"
        );
        addGroup(
            catalogPane,
            text.apply("gateGroupControlled"),
            filter,
            selectedGate,
            gateConsumer,
            gateHelpConsumer,
            "CX",
            "CY",
            "CZ",
            "CH",
            "SWAP",
            "CCX"
        );
        addGroup(
            catalogPane,
            text.apply("gateGroupNonUnitary"),
            filter,
            selectedGate,
            gateConsumer,
            gateHelpConsumer,
            "MEASURE",
            "RESET",
            "BARRIER"
        );
        if (catalogPane.getChildren().isEmpty()) {
            final Label emptyLabel = new Label(text.apply("gateSearchEmpty"));
            emptyLabel.getStyleClass().add("visualization-limit-label");
            catalogPane.getChildren().add(emptyLabel);
        }
    }

    private static void addGroup(
        final VBox catalogPane,
        final String title,
        final String filter,
        final String selectedGate,
        final Consumer<String> gateConsumer,
        final Consumer<String> gateHelpConsumer,
        final String... gates
    ) {
        final FlowPane groupButtons = new FlowPane(
            6.0,
            6.0
        );
        groupButtons.setPrefWrapLength(246.0);
        groupButtons.setMaxWidth(246.0);
        for (final String gate : gates) {
            if (
                !filter.isBlank()
                && !gate.contains(filter)
            ) {
                continue;
            }
            final Button button = new Button(gate);
            button.setOnAction(event -> gateConsumer.accept(gate));
            button.setOnContextMenuRequested(event -> {
                gateHelpConsumer.accept(gate);
                event.consume();
            });
            button.getStyleClass().add(styleClass(gate));
            if (gate.equals(selectedGate)) {
                button.getStyleClass().add("selected-gate-button");
            }
            groupButtons.getChildren().add(button);
        }
        if (groupButtons.getChildren().isEmpty()) {
            return;
        }
        final Label label = new Label(title);
        label.getStyleClass().add("gate-catalog-group-title");
        final VBox group = new VBox(
            7.0,
            label,
            groupButtons
        );
        group.setFillWidth(true);
        group.getStyleClass().add("gate-catalog-group");
        catalogPane.getChildren().add(group);
    }

    private static String styleClass(final String gate) {
        return switch (gate) {
            case "RX", "RY", "RZ", "PHASE", "S", "T" -> "gate-phase-button";
            case "CX", "CY", "CZ", "CH", "SWAP", "CCX" -> "gate-control-button";
            case "MEASURE", "RESET", "BARRIER" -> "gate-nonunitary-button";
            default -> "gate-general-button";
        };
    }
}
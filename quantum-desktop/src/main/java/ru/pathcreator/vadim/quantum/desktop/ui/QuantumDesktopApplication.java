/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ru.pathcreator.vadim.quantum.application.compiler.CompilerResult;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrReadResult;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrWriteResult;
import ru.pathcreator.vadim.quantum.application.resource.ResourceEstimate;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.application.transformation.TransformationResult;
import ru.pathcreator.vadim.quantum.application.visualization.CircuitTimeline;
import ru.pathcreator.vadim.quantum.application.visualization.CircuitTimelineStep;
import ru.pathcreator.vadim.quantum.application.visualization.ProgramTimeline;
import ru.pathcreator.vadim.quantum.application.workflow.ProductWorkflowReport;
import ru.pathcreator.vadim.quantum.desktop.workflow.DesktopAction;
import ru.pathcreator.vadim.quantum.desktop.workflow.DesktopExecutionOptions;
import ru.pathcreator.vadim.quantum.desktop.workflow.DesktopWorkflowResult;
import ru.pathcreator.vadim.quantum.desktop.workflow.DesktopWorkflowService;
import ru.pathcreator.vadim.quantum.desktop.workspace.DesktopIrOperationSpec;
import ru.pathcreator.vadim.quantum.desktop.workspace.DesktopIrWorkspaceService;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

/**
 * Native JavaFX workbench for programming Quantum IR.
 */
public final class QuantumDesktopApplication extends Application {

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
    private static final String DEFAULT_EXTERNAL_PROGRAM = """
        OPENQASM 2.0;
        include "qelib1.inc";
        qreg q[2];
        creg c[2];
        h q[0];
        cx q[0],q[1];
        measure q[0] -> c[0];
        measure q[1] -> c[1];
        """;

    private final DesktopIrWorkspaceService workspaceService = new DesktopIrWorkspaceService();
    private final DesktopWorkflowService externalService = new DesktopWorkflowService();
    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final ArrayList<DesktopIrOperationSpec> operations = new ArrayList<>();
    private final ArrayList<String> pendingQubits = new ArrayList<>();
    private QuantumProgram activeJsonProgram;
    private final TextField circuitNameField = new TextField("main");
    private final TextField qregNameField = new TextField("q");
    private final TextField qregSizeField = new TextField("3");
    private final TextField cregNameField = new TextField("c");
    private final TextField cregSizeField = new TextField("3");
    private final ComboBox<String> gateBox = new ComboBox<>();
    private final ComboBox<String> primaryQubitBox = new ComboBox<>();
    private final ComboBox<String> secondaryQubitBox = new ComboBox<>();
    private final ComboBox<String> tertiaryQubitBox = new ComboBox<>();
    private final ComboBox<String> classicalBitBox = new ComboBox<>();
    private final ComboBox<String> templateBox = new ComboBox<>();
    private final TextField gateSearchField = new TextField();
    private final FlowPane gateCatalogPane = new FlowPane(6, 6);
    private final ComboBox<String> wireOrderBox = new ComboBox<>();
    private final ComboBox<String> layoutModeBox = new ComboBox<>();
    private final ComboBox<String> experienceModeBox = new ComboBox<>();
    private final ComboBox<String> commandPaletteBox = new ComboBox<>();
    private final TextField angleField = new TextField("1.5707963267948966");
    private final ComboBox<IntegrationFormat> targetFormatBox = new ComboBox<>();
    private final TextField shotsField = new TextField("1024");
    private final TextField seedField = new TextField("7");
    private final CheckBox fastBox = new CheckBox("Fast");
    private final CheckBox skipValidationBox = new CheckBox("Skip validation");
    private final CheckBox skipInspectionBox = new CheckBox("Skip inspection");
    private final CheckBox skipPreflightBox = new CheckBox("Skip preflight");
    private final CheckBox skipTransformationBox = new CheckBox("Skip transformation");
    private final CheckBox skipSimulationBox = new CheckBox("Skip simulation");
    private final CheckBox skipCompilerBox = new CheckBox("Skip compiler");
    private final CheckBox skipBackendBox = new CheckBox("Skip backend");
    private final CheckBox canonicalizeParametersBox = new CheckBox("Canonicalize parameters");
    private final CheckBox removeIdentityBox = new CheckBox("Remove identity gates");
    private final CheckBox inlineCompositeBox = new CheckBox("Inline composite gates");
    private final CheckBox targetLoweringBox = new CheckBox("Target-aware lowering");
    private final CheckBox autoSimulationBox = new CheckBox("Auto simulate small circuits");
    private final CheckBox hideZeroProbabilityBox = new CheckBox("Hide zero probabilities");
    private final VBox circuitRows = new VBox(8);
    private final ListView<String> operationList = new ListView<>();
    private final TextArea overviewArea = new TextArea();
    private final TextArea inspectorArea = new TextArea();
    private final TextArea simulationArea = new TextArea();
    private final TextArea resourcesArea = new TextArea();
    private final TextArea preflightArea = new TextArea();
    private final TextArea compatibilityArea = new TextArea();
    private final TextArea transformationArea = new TextArea();
    private final TextArea javaDslArea = new TextArea();
    private final TextArea gateInfoArea = new TextArea();
    private final TextArea assistantNotesArea = new TextArea();
    private final TextArea targetProfileArea = new TextArea();
    private final TextArea nativeJsonArea = new TextArea();
    private final TextArea diagnosticsArea = new TextArea();
    private final ListView<String> diagnosticList = new ListView<>();
    private final TextArea generatedArea = new TextArea();
    private final TextArea externalSourceArea = new TextArea(DEFAULT_EXTERNAL_PROGRAM);
    private final ComboBox<IntegrationFormat> externalInputFormatBox = new ComboBox<>();
    private final ComboBox<IntegrationFormat> externalTargetFormatBox = new ComboBox<>();
    private final TextArea externalResultArea = new TextArea();
    private final TextArea externalGeneratedArea = new TextArea();
    private final Label builderHintLabel = new Label("Select a gate, then click a qubit lane to place it.");
    private final Label statusLabel = new Label("Native IR workspace ready");
    private final Label programBadgeLabel = new Label("Program");
    private final Label targetBadgeLabel = new Label("Target");
    private final Label healthBadgeLabel = new Label("Health");
    private final List<Node> expertOnlyNodes = new ArrayList<>();
    private final List<Tab> expertOnlyTabs = new ArrayList<>();

    public static void main(final String[] args) {
        launch(args);
    }

    @Override
    public void start(final Stage stage) {
        initializeControls();
        loadInitialTemplate();
        refreshWorkspace();

        final BorderPane root = new BorderPane();
        root.setTop(header());
        root.setCenter(mainTabs(stage));
        root.setBottom(footer());
        root.setStyle("-fx-background-color: #f5f7fb;");

        final Scene scene = new Scene(
            root,
            1440,
            900
        );
        final String stylesheet = QuantumDesktopApplication.class
            .getResource("/ru/pathcreator/vadim/quantum/desktop/ui/quantum-desktop.css")
            .toExternalForm();
        scene.getStylesheets().add(stylesheet);
        stage.setTitle("Quantum IR Studio");
        stage.setScene(scene);
        stage.show();
    }

    private void loadInitialTemplate() {
        final String template = getParameters().getNamed().getOrDefault(
            "template",
            "Bell State"
        );
        templateBox.setValue(canonicalTemplateName(template));
        loadTemplateWithoutRefresh();
    }

    private static String canonicalTemplateName(final String template) {
        if (template == null) {
            return "Bell State";
        }
        final String normalized = template.trim().toLowerCase();
        if (normalized.startsWith("dense")) {
            return "Dense Gate Spectrum";
        }
        if (normalized.startsWith("ghz")) {
            return "GHZ 3";
        }
        if (normalized.startsWith("qft")) {
            return "QFT-like 3";
        }
        if (normalized.startsWith("grover")) {
            return "Grover-like Toy";
        }
        return "Bell State";
    }

    private void initializeControls() {
        gateBox.getItems().setAll(GATES);
        gateBox.setValue("H");
        gateBox.valueProperty().addListener((observable, oldValue, newValue) -> renderGateInfo());
        gateSearchField.setPromptText("Search gates");
        gateSearchField.textProperty().addListener((observable, oldValue, newValue) -> refreshGateCatalog());
        refreshGateCatalog();
        wireOrderBox.getItems().setAll(
            "MSB top-to-bottom",
            "LSB top-to-bottom"
        );
        wireOrderBox.setValue("MSB top-to-bottom");
        wireOrderBox.valueProperty().addListener((observable, oldValue, newValue) -> renderCircuit());
        layoutModeBox.getItems().setAll(
            "Compact order",
            "Layer view"
        );
        layoutModeBox.setValue("Compact order");
        layoutModeBox.valueProperty().addListener((observable, oldValue, newValue) -> renderCircuit());
        experienceModeBox.getItems().setAll(
            "Beginner",
            "Expert"
        );
        experienceModeBox.setValue("Expert");
        experienceModeBox.valueProperty().addListener((observable, oldValue, newValue) -> applyExperienceMode());
        commandPaletteBox.getItems().setAll(
            "Validate",
            "Inspect",
            "Simulate",
            "Export",
            "Preflight",
            "Compatibility",
            "Transform",
            "JSON",
            "Java DSL",
            "Workflow",
            "Apply Native JSON"
        );
        commandPaletteBox.setValue("Inspect");
        targetFormatBox.getItems().setAll(IntegrationFormat.values());
        targetFormatBox.setValue(IntegrationFormat.OPENQASM_3);
        targetFormatBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshWorkspace());
        externalInputFormatBox.getItems().setAll(IntegrationFormat.values());
        externalInputFormatBox.setValue(IntegrationFormat.OPENQASM_2);
        externalTargetFormatBox.getItems().setAll(IntegrationFormat.values());
        externalTargetFormatBox.setValue(IntegrationFormat.OPENQASM_3);
        nativeJsonArea.setEditable(true);
        diagnosticsArea.setEditable(false);
        generatedArea.setEditable(false);
        externalResultArea.setEditable(false);
        externalGeneratedArea.setEditable(false);
        overviewArea.setEditable(false);
        operationList.setPrefHeight(180);
        operationList.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() >= 0) {
                statusLabel.setText("Selected operation #" + newValue.intValue());
                renderSelectedOperation(newValue.intValue());
            }
        });
        templateBox.getItems().setAll(
            "Bell State",
            "GHZ 3",
            "QFT-like 3",
            "Grover-like Toy",
            "Dense Gate Spectrum"
        );
        templateBox.setValue("Bell State");
        templateBox.setPrefWidth(164);
        inspectorArea.setEditable(false);
        simulationArea.setEditable(false);
        resourcesArea.setEditable(false);
        preflightArea.setEditable(false);
        compatibilityArea.setEditable(false);
        transformationArea.setEditable(false);
        javaDslArea.setEditable(false);
        gateInfoArea.setEditable(false);
        assistantNotesArea.setEditable(false);
        targetProfileArea.setEditable(false);
        diagnosticList.setPrefHeight(180);
        diagnosticList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> navigateDiagnostic(newValue));
        programBadgeLabel.getStyleClass().add("status-chip");
        targetBadgeLabel.getStyleClass().add("status-chip");
        healthBadgeLabel.getStyleClass().add("status-chip");
        canonicalizeParametersBox.setSelected(true);
        removeIdentityBox.setSelected(true);
        targetLoweringBox.setSelected(true);
        hideZeroProbabilityBox.setSelected(true);
        hideZeroProbabilityBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (autoSimulationBox.isSelected()) {
                refreshWorkspace();
            }
        });
        refreshReferenceBoxes();
        renderGateInfo();
        applyExperienceMode();
    }

    private Node header() {
        final Label title = new Label("Quantum IR Studio");
        title.getStyleClass().add("header-title");
        final Label subtitle = new Label("Native model first: build IR, inspect flow, simulate, then export");
        subtitle.getStyleClass().add("header-subtitle");
        final VBox titleBox = new VBox(
            2,
            title,
            subtitle
        );
        final Region spacer = new Region();
        HBox.setHgrow(
            spacer,
            Priority.ALWAYS
        );
        final Button validateButton = primaryButton(
            "Validate IR",
            () -> runNative("Validate", this::validateNative)
        );
        final Button simulateButton = primaryButton(
            "Simulate",
            () -> runNative("Simulate", this::simulateNative)
        );
        final Button exportButton = primaryButton(
            "Export",
            () -> runNative("Export", this::exportNative)
        );
        final HBox header = new HBox(
            14,
            titleBox,
            spacer,
            programBadgeLabel,
            healthBadgeLabel,
            targetBadgeLabel,
            headerLabel("Target"),
            targetFormatBox,
            validateButton,
            simulateButton,
            exportButton
        );
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("workbench-header");
        return header;
    }

    private TabPane mainTabs(final Stage stage) {
        final TabPane tabs = new TabPane(
            tab("Native IR Studio", nativeWorkspace(stage)),
            tab("External Formats", externalWorkspace(stage)),
            tab("Execution Settings", executionSettings())
        );
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        return tabs;
    }

    private Node nativeWorkspace(final Stage stage) {
        final SplitPane splitPane = new SplitPane(
            scrollable(palette(stage)),
            circuitCanvas(),
            resultTabs()
        );
        splitPane.setDividerPositions(
            0.22,
            0.68
        );
        return splitPane;
    }

    private Node palette(final Stage stage) {
        final VBox registers = section(
            "Program",
            fieldRow(
                "Circuit",
                circuitNameField
            ),
            fieldRow(
                "Q register",
                qregNameField,
                qregSizeField
            ),
            fieldRow(
                "C register",
                cregNameField,
                cregSizeField
            ),
            fieldRow(
                "Wire order",
                wireOrderBox
            ),
            fieldRow(
                "Layout",
                layoutModeBox
            ),
            fieldRow(
                "Mode",
                experienceModeBox
            )
        );
        qregSizeField.textProperty().addListener((observable, oldValue, newValue) -> refreshReferenceBoxes());
        qregNameField.textProperty().addListener((observable, oldValue, newValue) -> refreshReferenceBoxes());
        cregSizeField.textProperty().addListener((observable, oldValue, newValue) -> refreshReferenceBoxes());
        cregNameField.textProperty().addListener((observable, oldValue, newValue) -> refreshReferenceBoxes());

        final Button addButton = primaryButton(
            "Add",
            this::addOperation
        );
        final Button insertBeforeButton = secondaryButton(
            "Before",
            () -> insertOperationAtSelection(0)
        );
        final Button insertAfterButton = secondaryButton(
            "After",
            () -> insertOperationAtSelection(1)
        );
        final Button duplicateButton = secondaryButton(
            "Duplicate",
            this::duplicateSelectedOperation
        );
        final Button removeButton = secondaryButton(
            "Remove",
            this::removeSelectedOperation
        );
        final Button moveLeftButton = secondaryButton(
            "Left",
            () -> moveSelectedOperation(-1)
        );
        final Button moveRightButton = secondaryButton(
            "Right",
            () -> moveSelectedOperation(1)
        );
        markExpert(
            insertBeforeButton,
            insertAfterButton,
            duplicateButton,
            moveLeftButton,
            moveRightButton
        );
        final Button resetButton = secondaryButton(
            "Reset",
            () -> {
                clearActiveJsonProgram();
                seedBellProgram();
                refreshWorkspace();
            }
        );
        final Button loadTemplateButton = secondaryButton(
            "Load",
            this::loadTemplate
        );
        final VBox operationEditor = section(
            "Operation",
            fieldRow(
                "Template",
                templateBox,
                loadTemplateButton
            ),
            fieldRow(
                "Find gate",
                gateSearchField
            ),
            gateCatalog(),
            new Separator(),
            fieldRow(
                "Gate",
                gateBox
            ),
            fieldRow(
                "Qubit A",
                primaryQubitBox
            ),
            fieldRow(
                "Qubit B",
                secondaryQubitBox
            ),
            fieldRow(
                "Qubit C",
                tertiaryQubitBox
            ),
            fieldRow(
                "Classical",
                classicalBitBox
            ),
            fieldRow(
                "Angle",
                angleField
            ),
            actionFlow(
                addButton,
                insertBeforeButton,
                insertAfterButton,
                duplicateButton,
                removeButton,
                moveLeftButton,
                moveRightButton,
                resetButton
            )
        );
        final Button jsonButton = primaryButton(
            "JSON",
            () -> runNative("JSON", this::jsonNative)
        );
        final Button timelineButton = primaryButton(
            "Timeline",
            () -> runNative("Timeline", this::timelineNative)
        );
        final Button inspectButton = primaryButton(
            "Inspect",
            () -> runNative("Inspect", this::inspectNative)
        );
        final Button resourcesButton = primaryButton(
            "Resources",
            () -> runNative("Resources", this::resourcesNative)
        );
        final Button preflightButton = primaryButton(
            "Preflight",
            () -> runNative("Preflight", this::preflightNative)
        );
        final Button compatibilityButton = primaryButton(
            "Compatibility",
            () -> runNative("Compatibility", this::compatibilityNative)
        );
        final Button transformButton = primaryButton(
            "Transform",
            () -> runNative("Transform", this::transformNative)
        );
        final Button javaDslButton = primaryButton(
            "Java DSL",
            () -> runNative("Java DSL", this::javaDslNative)
        );
        final Button runCommandButton = primaryButton(
            "Run",
            this::runCommandPalette
        );
        final Button saveJsonButton = secondaryButton(
            "Save JSON File",
            () -> saveNativeJson(stage)
        );
        final Button openJsonButton = secondaryButton(
            "Open JSON File",
            () -> openNativeJson(stage)
        );
        final Button applyJsonButton = secondaryButton(
            "Apply JSON",
            this::applyNativeJson
        );
        final Button workflowButton = primaryButton(
            "Workflow",
            () -> runNative("Workflow", this::workflowNative)
        );
        markExpert(
            jsonButton,
            timelineButton,
            resourcesButton,
            preflightButton,
            compatibilityButton,
            transformButton,
            workflowButton
        );
        final VBox actions = section(
            "Native Actions",
            builderHintLabel,
            fieldRow(
                "Command",
                commandPaletteBox,
                runCommandButton
            ),
            actionFlow(
                inspectButton,
                javaDslButton,
                saveJsonButton,
                openJsonButton,
                applyJsonButton,
                jsonButton,
                timelineButton,
                resourcesButton,
                preflightButton,
                compatibilityButton,
                transformButton,
                workflowButton
            )
        );
        final VBox palette = new VBox(
            14,
            registers,
            operationEditor,
            actions,
            section(
                "Operation Stream",
                operationList
            )
        );
        palette.setPadding(new Insets(14));
        palette.setStyle("-fx-background-color: #ffffff;");
        return palette;
    }

    private Node circuitCanvas() {
        final Label title = new Label("Native IR Circuit Flow");
        title.getStyleClass().add("panel-title");
        final Label hint = new Label("Operations are built into ru.pathcreator.vadim.quantum domain objects through the Java DSL.");
        hint.getStyleClass().add("panel-hint");
        final ScrollPane scrollPane = new ScrollPane(circuitRows);
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(false);
        scrollPane.setStyle("-fx-background: #f8fafc; -fx-background-color: #f8fafc;");
        final VBox panel = new VBox(
            10,
            title,
            hint,
            scrollPane
        );
        panel.getStyleClass().add("canvas-panel");
        VBox.setVgrow(
            scrollPane,
            Priority.ALWAYS
        );
        return panel;
    }

    private Node gateCatalog() {
        return gateCatalogPane;
    }

    private void refreshGateCatalog() {
        gateCatalogPane.getChildren().clear();
        final String filter = gateSearchField.getText() == null
            ? ""
            : gateSearchField.getText().trim().toUpperCase();
        for (int i = 0; i < GATES.size(); i++) {
            final String gate = GATES.get(i);
            if (
                !filter.isBlank()
                && !gate.contains(filter)
            ) {
                continue;
            }
            final Button button = new Button(gate);
            button.setOnAction(event -> {
                gateBox.setValue(gate);
                renderGateInfo();
            });
            button.getStyleClass().add(switch (gate) {
                case "RX", "RY", "RZ", "PHASE", "S", "T" -> "gate-phase-button";
                case "CX", "CY", "CZ", "CH", "SWAP", "CCX" -> "gate-control-button";
                case "MEASURE", "RESET", "BARRIER" -> "gate-nonunitary-button";
                default -> "gate-basic-button";
            });
            gateCatalogPane.getChildren().add(button);
        }
    }

    private Node resultTabs() {
        expertOnlyTabs.clear();
        final TabPane tabs = new TabPane(
            tab("Overview", overviewArea),
            tab("Inspector", inspectorArea),
            tab("Simulation", simulationArea),
            tab("Java DSL", javaDslArea),
            tab("Gate Info", gateInfoArea),
            tab("Assistant Notes", assistantNotesArea),
            tab("Diagnostic List", diagnosticList),
            expertTab("Target Profile", targetProfileArea),
            expertTab("Resources", resourcesArea),
            expertTab("Preflight", preflightArea),
            expertTab("Compatibility", compatibilityArea),
            expertTab("Transform", transformationArea),
            tab("Native JSON", nativeJsonArea),
            tab("Diagnostics", diagnosticsArea),
            tab("Generated Export", generatedArea)
        );
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        applyExperienceMode();
        return tabs;
    }

    private Node externalWorkspace(final Stage stage) {
        externalSourceArea.setWrapText(false);
        externalResultArea.setWrapText(false);
        externalGeneratedArea.setWrapText(false);
        final Button openButton = secondaryButton(
            "Open External File",
            () -> openExternalFile(stage)
        );
        final Button importButton = primaryButton(
            "Import -> Native JSON",
            () -> runExternal(() -> externalService.json(
                externalInputFormatBox.getValue(),
                externalSourceArea.getText()
            ))
        );
        final Button compileButton = primaryButton(
            "Import -> Export",
            () -> runExternal(() -> externalService.compile(
                externalInputFormatBox.getValue(),
                externalSourceArea.getText(),
                externalTargetFormatBox.getValue(),
                executionOptions()
            ))
        );
        final HBox toolbar = new HBox(
            8,
            new Label("Input"),
            externalInputFormatBox,
            new Label("Target"),
            externalTargetFormatBox,
            openButton,
            importButton,
            compileButton
        );
        toolbar.setPadding(new Insets(12));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        final SplitPane splitPane = new SplitPane(
            externalSourceArea,
            new TabPane(
                tab("Result", externalResultArea),
                tab("Generated", externalGeneratedArea)
            )
        );
        splitPane.setDividerPositions(0.48);
        final BorderPane pane = new BorderPane();
        pane.setTop(toolbar);
        pane.setCenter(splitPane);
        return pane;
    }

    private Node executionSettings() {
        final GridPane grid = new GridPane();
        grid.setPadding(new Insets(16));
        grid.setHgap(10);
        grid.setVgap(10);
        addSetting(
            grid,
            0,
            "Shots",
            shotsField
        );
        addSetting(
            grid,
            1,
            "Seed",
            seedField
        );
        final FlowPane options = new FlowPane(
            12,
            10,
            fastBox,
            skipValidationBox,
            skipInspectionBox,
            skipPreflightBox,
            skipTransformationBox,
            skipSimulationBox,
            skipCompilerBox,
            skipBackendBox
        );
        grid.add(
            new Label("Execution"),
            0,
            2
        );
        grid.add(
            options,
            1,
            2
        );
        final FlowPane liveOptions = new FlowPane(
            12,
            10,
            autoSimulationBox,
            hideZeroProbabilityBox
        );
        grid.add(
            new Label("Live UI"),
            0,
            3
        );
        grid.add(
            liveOptions,
            1,
            3
        );
        final FlowPane transformations = new FlowPane(
            12,
            10,
            canonicalizeParametersBox,
            removeIdentityBox,
            inlineCompositeBox,
            targetLoweringBox
        );
        grid.add(
            new Label("Transform"),
            0,
            4
        );
        grid.add(
            transformations,
            1,
            4
        );
        return grid;
    }

    private Node footer() {
        final HBox footer = new HBox(statusLabel);
        footer.getStyleClass().add("status-bar");
        return footer;
    }

    private void seedBellProgram() {
        operations.clear();
        qregSizeField.setText("2");
        cregSizeField.setText("2");
        operations.add(new DesktopIrOperationSpec(
            "H",
            "q[0]",
            "q[1]",
            "q[0]",
            "c[0]",
            Math.PI / 2.0
        ));
        operations.add(new DesktopIrOperationSpec(
            "CX",
            "q[0]",
            "q[1]",
            "q[0]",
            "c[0]",
            Math.PI / 2.0
        ));
        operations.add(new DesktopIrOperationSpec(
            "MEASURE",
            "q[0]",
            "q[1]",
            "q[0]",
            "c[0]",
            Math.PI / 2.0
        ));
        operations.add(new DesktopIrOperationSpec(
            "MEASURE",
            "q[1]",
            "q[0]",
            "q[0]",
            "c[1]",
            Math.PI / 2.0
        ));
        refreshReferenceBoxes();
    }

    private void loadTemplate() {
        loadTemplateWithoutRefresh();
        clearPending();
        refreshWorkspace();
    }

    private void loadTemplateWithoutRefresh() {
        clearActiveJsonProgram();
        switch (templateBox.getValue()) {
            case "Bell State" -> seedBellProgram();
            case "GHZ 3" -> seedGhzProgram();
            case "QFT-like 3" -> seedQftLikeProgram();
            case "Grover-like Toy" -> seedGroverLikeProgram();
            case "Dense Gate Spectrum" -> seedDenseGateSpectrumProgram();
            default -> seedBellProgram();
        }
    }

    private void seedGhzProgram() {
        operations.clear();
        qregSizeField.setText("3");
        cregSizeField.setText("3");
        operations.add(operation(
            "H",
            "q[0]"
        ));
        operations.add(operation(
            "CX",
            "q[0]",
            "q[1]"
        ));
        operations.add(operation(
            "CX",
            "q[1]",
            "q[2]"
        ));
        operations.add(measure(
            "q[0]",
            "c[0]"
        ));
        operations.add(measure(
            "q[1]",
            "c[1]"
        ));
        operations.add(measure(
            "q[2]",
            "c[2]"
        ));
        refreshReferenceBoxes();
    }

    private void seedQftLikeProgram() {
        operations.clear();
        qregSizeField.setText("3");
        cregSizeField.setText("3");
        operations.add(operation(
            "H",
            "q[0]"
        ));
        operations.add(rotation(
            "PHASE",
            "q[1]",
            Math.PI / 2.0
        ));
        operations.add(rotation(
            "PHASE",
            "q[2]",
            Math.PI / 4.0
        ));
        operations.add(operation(
            "H",
            "q[1]"
        ));
        operations.add(rotation(
            "PHASE",
            "q[2]",
            Math.PI / 2.0
        ));
        operations.add(operation(
            "H",
            "q[2]"
        ));
        operations.add(operation(
            "SWAP",
            "q[0]",
            "q[2]"
        ));
        refreshReferenceBoxes();
    }

    private void seedGroverLikeProgram() {
        operations.clear();
        qregSizeField.setText("3");
        cregSizeField.setText("3");
        operations.add(operation(
            "H",
            "q[0]"
        ));
        operations.add(operation(
            "H",
            "q[1]"
        ));
        operations.add(operation(
            "X",
            "q[0]"
        ));
        operations.add(operation(
            "CZ",
            "q[0]",
            "q[1]"
        ));
        operations.add(operation(
            "X",
            "q[0]"
        ));
        operations.add(operation(
            "H",
            "q[0]"
        ));
        operations.add(operation(
            "H",
            "q[1]"
        ));
        operations.add(operation(
            "CZ",
            "q[0]",
            "q[1]"
        ));
        operations.add(operation(
            "H",
            "q[0]"
        ));
        operations.add(operation(
            "H",
            "q[1]"
        ));
        operations.add(measure(
            "q[0]",
            "c[0]"
        ));
        operations.add(measure(
            "q[1]",
            "c[1]"
        ));
        refreshReferenceBoxes();
    }

    private void seedDenseGateSpectrumProgram() {
        operations.clear();
        qregSizeField.setText("5");
        cregSizeField.setText("5");
        for (int round = 0; round < 4; round++) {
            operations.add(operation(
                "H",
                "q[0]"
            ));
            operations.add(operation(
                "X",
                "q[1]"
            ));
            operations.add(operation(
                "Y",
                "q[2]"
            ));
            operations.add(operation(
                "Z",
                "q[3]"
            ));
            operations.add(operation(
                "S",
                "q[4]"
            ));
            operations.add(operation(
                "T",
                "q[0]"
            ));
            operations.add(rotation(
                "RX",
                "q[1]",
                Math.PI / (round + 2.0)
            ));
            operations.add(rotation(
                "RY",
                "q[2]",
                Math.PI / (round + 3.0)
            ));
            operations.add(rotation(
                "RZ",
                "q[3]",
                Math.PI / (round + 4.0)
            ));
            operations.add(rotation(
                "PHASE",
                "q[4]",
                Math.PI / (round + 5.0)
            ));
            operations.add(operation(
                "CX",
                "q[0]",
                "q[1]"
            ));
            operations.add(operation(
                "CY",
                "q[1]",
                "q[2]"
            ));
            operations.add(operation(
                "CZ",
                "q[2]",
                "q[3]"
            ));
            operations.add(operation(
                "CH",
                "q[3]",
                "q[4]"
            ));
            operations.add(operation(
                "SWAP",
                "q[0]",
                "q[4]"
            ));
            operations.add(new DesktopIrOperationSpec(
                "CCX",
                "q[0]",
                "q[2]",
                "q[4]",
                "c[0]",
                Math.PI / 2.0
            ));
            operations.add(operation(
                "BARRIER",
                "q[1]",
                "q[3]"
            ));
        }
        operations.add(operation(
            "RESET",
            "q[4]"
        ));
        operations.add(measure(
            "q[0]",
            "c[0]"
        ));
        operations.add(measure(
            "q[1]",
            "c[1]"
        ));
        operations.add(measure(
            "q[2]",
            "c[2]"
        ));
        operations.add(measure(
            "q[3]",
            "c[3]"
        ));
        operations.add(measure(
            "q[4]",
            "c[4]"
        ));
        refreshReferenceBoxes();
    }

    private static DesktopIrOperationSpec operation(
        final String gate,
        final String primaryQubit
    ) {
        return new DesktopIrOperationSpec(
            gate,
            primaryQubit,
            "q[0]",
            "q[0]",
            "c[0]",
            Math.PI / 2.0
        );
    }

    private static DesktopIrOperationSpec operation(
        final String gate,
        final String primaryQubit,
        final String secondaryQubit
    ) {
        return new DesktopIrOperationSpec(
            gate,
            primaryQubit,
            secondaryQubit,
            "q[0]",
            "c[0]",
            Math.PI / 2.0
        );
    }

    private static DesktopIrOperationSpec rotation(
        final String gate,
        final String qubit,
        final double angle
    ) {
        return new DesktopIrOperationSpec(
            gate,
            qubit,
            "q[0]",
            "q[0]",
            "c[0]",
            angle
        );
    }

    private static DesktopIrOperationSpec measure(
        final String qubit,
        final String bit
    ) {
        return new DesktopIrOperationSpec(
            "MEASURE",
            qubit,
            "q[0]",
            "q[0]",
            bit,
            Math.PI / 2.0
        );
    }

    private void addOperation() {
        clearActiveJsonProgram();
        operations.add(operationFromEditor());
        refreshWorkspace();
    }

    private DesktopIrOperationSpec operationFromEditor() {
        return new DesktopIrOperationSpec(
            gateBox.getValue(),
            primaryQubitBox.getValue(),
            secondaryQubitBox.getValue(),
            tertiaryQubitBox.getValue(),
            classicalBitBox.getValue(),
            angle()
        );
    }

    private void insertOperationAtSelection(final int offset) {
        clearActiveJsonProgram();
        final int selectedIndex = operationList.getSelectionModel().getSelectedIndex();
        final int insertIndex = selectedIndex < 0
            ? operations.size()
            : selectedIndex + offset;
        operations.add(
            Math.max(
                0,
                Math.min(
                    operations.size(),
                    insertIndex
                )
            ),
            operationFromEditor()
        );
        refreshWorkspace();
    }

    private void duplicateSelectedOperation() {
        clearActiveJsonProgram();
        final int selectedIndex = operationList.getSelectionModel().getSelectedIndex();
        if (
            selectedIndex < 0
            || selectedIndex >= operations.size()
        ) {
            return;
        }
        operations.add(
            selectedIndex + 1,
            operations.get(selectedIndex)
        );
        refreshWorkspace();
        operationList.getSelectionModel().select(selectedIndex + 1);
    }

    private void removeSelectedOperation() {
        clearActiveJsonProgram();
        final int selectedIndex = operationList.getSelectionModel().getSelectedIndex();
        if (
            selectedIndex >= 0
            && selectedIndex < operations.size()
        ) {
            operations.remove(selectedIndex);
        } else if (!operations.isEmpty()) {
            operations.remove(operations.size() - 1);
        }
        refreshWorkspace();
    }

    private void moveSelectedOperation(final int offset) {
        clearActiveJsonProgram();
        final int selectedIndex = operationList.getSelectionModel().getSelectedIndex();
        final int targetIndex = selectedIndex + offset;
        if (
            selectedIndex < 0
            || selectedIndex >= operations.size()
            || targetIndex < 0
            || targetIndex >= operations.size()
        ) {
            return;
        }
        final DesktopIrOperationSpec operation = operations.remove(selectedIndex);
        operations.add(
            targetIndex,
            operation
        );
        refreshWorkspace();
        operationList.getSelectionModel().select(targetIndex);
    }

    private void refreshWorkspace() {
        if (activeJsonProgram == null) {
            operationList.getItems().setAll(operations.stream()
                .map(DesktopIrOperationSpec::label)
                .toList());
        } else {
            operationList.getItems().setAll(
                "Native JSON program is active",
                "Graphical operation stream remains as an editable draft"
            );
        }
        renderCircuit();
        renderInspector();
        renderGateInfo();
        javaDslArea.setText(generateCurrentJavaDsl());
        try {
            final QuantumProgram program = buildNativeProgram();
            final QuantumIrWriteResult writeResult = workspaceService.writeJson(program);
            final ValidationResult validation = workspaceService.validate(program);
            final ResourceEstimate resources = workspaceService.resources(
                program,
                24
            );
            final var preflight = workspaceService.preflight(
                program,
                targetFormatBox.getValue()
            );
            targetProfileArea.setText(render(workspaceService.targetProfile(targetFormatBox.getValue())));
            refreshDiagnosticList(
                validation,
                preflight.status().name(),
                preflight.diagnostics().size()
            );
            updateStatusBadges(
                validation,
                resources,
                preflight.status().name()
            );
            overviewArea.setText(renderOverview(
                program,
                writeResult
            ));
            assistantNotesArea.setText(renderAssistantNotes(
                validation,
                resources,
                preflight.status().name()
            ));
            nativeJsonArea.setText(writeResult.hasContent()
                ? writeResult.content()
                : render(writeResult));
            diagnosticsArea.setText("Native workspace built successfully.");
            if (autoSimulationBox.isSelected()) {
                final SimulationResult simulation = workspaceService.simulate(
                    program,
                    shots(),
                    seed()
                );
                simulationArea.setText(renderSimulation(simulation));
            }
        } catch (final Exception exception) {
            diagnosticsArea.setText(exceptionMessage(exception));
        }
    }

    private void renderCircuit() {
        circuitRows.getChildren().clear();
        final int qubitCount = Math.max(
            1,
            integerValue(qregSizeField)
        );
        circuitRows.getChildren().add(operationIndexRow());
        final String qreg = qregNameField.getText().isBlank()
            ? "q"
            : qregNameField.getText();
        for (int wireRow = 0; wireRow < qubitCount; wireRow++) {
            final int qubitIndex = isLsbWireOrder()
                ? qubitCount - wireRow - 1
                : wireRow;
            final String qubit = qreg + "[" + qubitIndex + "]";
            final HBox row = new HBox(6);
            row.setAlignment(Pos.CENTER_LEFT);
            final Label wire = new Label(qubit);
            wire.setMinWidth(58);
            wire.setStyle("-fx-font-weight: 700; -fx-text-fill: #334155;");
            row.getChildren().add(wire);
            for (int j = 0; j < operations.size(); j++) {
                row.getChildren().add(operationCell(
                    qubit,
                    operations.get(j),
                    j
                ));
            }
            row.getChildren().add(dropCell(qubit));
            circuitRows.getChildren().add(row);
        }
    }

    private void renderInspector() {
        final StringBuilder text = new StringBuilder();
        text.append("Program").append(System.lineSeparator());
        text.append("  circuit: ").append(circuitNameField.getText()).append(System.lineSeparator());
        text.append("  quantum register: ").append(qregNameField.getText()).append("[")
            .append(integerValue(qregSizeField)).append("]").append(System.lineSeparator());
        text.append("  classical register: ").append(cregNameField.getText()).append("[")
            .append(integerValue(cregSizeField)).append("]").append(System.lineSeparator());
        text.append("  operations: ").append(operations.size()).append(System.lineSeparator());
        text.append(System.lineSeparator());
        text.append("Gate histogram").append(System.lineSeparator());
        for (int i = 0; i < GATES.size(); i++) {
            final String gate = GATES.get(i);
            final long count = operations.stream()
                .filter(operation -> gate.equals(operation.gate()))
                .count();
            if (count > 0) {
                text.append("  ").append(gate).append(": ").append(count).append(System.lineSeparator());
            }
        }
        text.append(System.lineSeparator());
        text.append("Measurements").append(System.lineSeparator());
        for (int i = 0; i < operations.size(); i++) {
            final DesktopIrOperationSpec operation = operations.get(i);
            if ("MEASURE".equals(operation.gate())) {
                text.append("  #").append(i).append(" ")
                    .append(operation.primaryQubit())
                    .append(" -> ")
                    .append(operation.classicalBit())
                    .append(System.lineSeparator());
            }
        }
        text.append(System.lineSeparator());
        text.append("Target").append(System.lineSeparator());
        text.append("  export target: ").append(targetFormatBox.getValue()).append(System.lineSeparator());
        inspectorArea.setText(text.toString());
    }

    private void renderSelectedOperation(final int index) {
        if (
            index < 0
            || index >= operations.size()
        ) {
            return;
        }
        final DesktopIrOperationSpec operation = operations.get(index);
        gateBox.setValue(operation.gate());
        primaryQubitBox.setValue(operation.primaryQubit());
        secondaryQubitBox.setValue(operation.secondaryQubit());
        tertiaryQubitBox.setValue(operation.tertiaryQubit());
        classicalBitBox.setValue(operation.classicalBit());
        angleField.setText(Double.toString(operation.angle()));
        diagnosticsArea.setText(
            "Selected operation #" + index + System.lineSeparator()
                + "gate: " + operation.gate() + System.lineSeparator()
                + "primary: " + operation.primaryQubit() + System.lineSeparator()
                + "secondary: " + operation.secondaryQubit() + System.lineSeparator()
                + "tertiary: " + operation.tertiaryQubit() + System.lineSeparator()
                + "classical: " + operation.classicalBit() + System.lineSeparator()
                + "angle: " + operation.angle()
        );
    }

    private Node operationIndexRow() {
        final HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        final Label spacer = new Label("step");
        spacer.setMinWidth(58);
        spacer.getStyleClass().add("wire-label");
        row.getChildren().add(spacer);
        for (int i = 0; i < operations.size(); i++) {
            final Label label = new Label(Integer.toString(i));
            label.setAlignment(Pos.CENTER);
            label.setMinSize(
                cellWidth(),
                24
            );
            label.getStyleClass().add("step-index-cell");
            row.getChildren().add(label);
        }
        final Label add = new Label("+");
        add.setAlignment(Pos.CENTER);
        add.setMinSize(
            cellWidth(),
            24
        );
        add.getStyleClass().add("step-index-cell");
        row.getChildren().add(add);
        return row;
    }

    private String renderOverview(
        final QuantumProgram program,
        final QuantumIrWriteResult writeResult
    ) {
        final ValidationResult validation = workspaceService.validate(program);
        final ResourceEstimate resources = workspaceService.resources(
            program,
            24
        );
        final var preflight = workspaceService.preflight(
            program,
            targetFormatBox.getValue()
        );
        final StringBuilder text = new StringBuilder();
        text.append("Workspace").append(System.lineSeparator());
        text.append("  source: ").append(activeJsonProgram == null
            ? "graphical native builder"
            : "applied native JSON").append(System.lineSeparator());
        text.append("  circuit: ").append(circuitNameField.getText()).append(System.lineSeparator());
        text.append("  mode: ").append(experienceModeBox.getValue()).append(System.lineSeparator());
        text.append("  layout: ").append(layoutModeBox.getValue()).append(System.lineSeparator());
        text.append("  wire order: ").append(wireOrderBox.getValue()).append(System.lineSeparator());
        text.append(System.lineSeparator());
        text.append("Program health").append(System.lineSeparator());
        text.append("  validation: ").append(validation.isValid() ? "valid" : "invalid").append(System.lineSeparator());
        text.append("  validation errors: ").append(validation.errorCount()).append(System.lineSeparator());
        text.append("  json: ").append(writeResult.hasContent() ? "ready" : "not ready").append(System.lineSeparator());
        text.append(System.lineSeparator());
        text.append("Resources").append(System.lineSeparator());
        text.append("  circuits: ").append(resources.circuitCount()).append(System.lineSeparator());
        text.append("  qubits: ").append(resources.qubitCount()).append(System.lineSeparator());
        text.append("  classical bits: ").append(resources.classicalBitCount()).append(System.lineSeparator());
        text.append("  operations: ").append(resources.operationCount()).append(System.lineSeparator());
        text.append("  gates: ").append(resources.gateCount()).append(System.lineSeparator());
        text.append("  measurements: ").append(resources.measurementCount()).append(System.lineSeparator());
        text.append("  local simulation: ")
            .append(resources.isLocalSimulationFeasible() ? "feasible" : "too large")
            .append(System.lineSeparator());
        text.append(System.lineSeparator());
        text.append("Target").append(System.lineSeparator());
        text.append("  format: ").append(targetFormatBox.getValue()).append(System.lineSeparator());
        text.append("  preflight: ").append(preflight.status()).append(System.lineSeparator());
        text.append("  diagnostics: ").append(preflight.diagnostics().size()).append(System.lineSeparator());
        text.append(System.lineSeparator());
        text.append("Gate histogram").append(System.lineSeparator());
        resources.gateHistogram().forEach((gate, count) -> text.append("  ")
            .append(gate)
            .append(": ")
            .append(count)
            .append(System.lineSeparator()));
        return text.toString();
    }

    private void updateStatusBadges(
        final ValidationResult validation,
        final ResourceEstimate resources,
        final String preflightStatus
    ) {
        programBadgeLabel.setText(resources.qubitCount() + "q / " + resources.operationCount() + " ops");
        healthBadgeLabel.setText(validation.isValid()
            ? "Valid"
            : "Invalid: " + validation.errorCount());
        targetBadgeLabel.setText(targetFormatBox.getValue() + " / " + preflightStatus);
    }

    private String renderAssistantNotes(
        final ValidationResult validation,
        final ResourceEstimate resources,
        final String preflightStatus
    ) {
        final StringBuilder text = new StringBuilder();
        text.append("Assistant Notes").append(System.lineSeparator());
        text.append(System.lineSeparator());
        if (activeJsonProgram != null) {
            text.append("- Applied native JSON is the active program. Graphical operations remain available as a draft until edited.")
                .append(System.lineSeparator());
        }
        if (validation.isValid()) {
            text.append("- The current native IR program validates successfully.").append(System.lineSeparator());
        } else {
            text.append("- Fix validation errors before simulation/export. Error count: ")
                .append(validation.errorCount())
                .append(System.lineSeparator());
        }
        if (resources.operationCount() >= 50) {
            text.append("- Large circuit UI mode is active. Use horizontal scroll and step indices to inspect operations.")
                .append(System.lineSeparator());
        }
        if (!resources.isLocalSimulationFeasible()) {
            text.append("- Local state-vector simulation is not feasible with the current qubit limit.")
                .append(System.lineSeparator());
        } else {
            text.append("- Local simulation is feasible for this circuit size.").append(System.lineSeparator());
        }
        if ("EXPORTABLE".equals(preflightStatus)) {
            text.append("- Target export should be direct for ").append(targetFormatBox.getValue()).append(".")
                .append(System.lineSeparator());
        } else if ("LOWERING_REQUIRED".equals(preflightStatus)) {
            text.append("- Target export needs lowering. Inspect Transform/Preflight before relying on generated text.")
                .append(System.lineSeparator());
        } else {
            text.append("- Target export has restrictions. Inspect Preflight diagnostics before export.")
                .append(System.lineSeparator());
        }
        text.append("- Selected gate info updates in the Gate Info tab; use Find gate to filter the catalog.")
            .append(System.lineSeparator());
        return text.toString();
    }

    private Node operationCell(
        final String qubit,
        final DesktopIrOperationSpec operation,
        final int operationIndex
    ) {
        final String text;
        if (qubit.equals(operation.primaryQubit())) {
            text = switch (operation.gate()) {
                case "CX", "CY", "CZ", "CH", "CCX" -> "CTRL";
                case "MEASURE" -> "M";
                default -> operation.gate();
            };
        } else if (qubit.equals(operation.secondaryQubit())) {
            text = switch (operation.gate()) {
                case "CX" -> "X";
                case "CY" -> "Y";
                case "CZ" -> "Z";
                case "CH" -> "H";
                case "SWAP" -> "SW";
                case "BARRIER" -> "|";
                case "CCX" -> "CTRL";
                default -> "--";
            };
        } else if (qubit.equals(operation.tertiaryQubit()) && "CCX".equals(operation.gate())) {
            text = "X";
        } else {
            text = "--";
        }
        final Label label = new Label(text);
        label.setOnMouseClicked(event -> {
            operationList.getSelectionModel().select(operationIndex);
            statusLabel.setText("Selected " + operation.label());
        });
        label.setAlignment(Pos.CENTER);
        label.setMinSize(
            cellWidth(),
            34
        );
        label.setStyle(cellStyle(text));
        return new StackPane(label);
    }

    private Node dropCell(final String qubit) {
        final Label label = new Label("+");
        label.setAlignment(Pos.CENTER);
        label.setMinSize(
            cellWidth(),
            34
        );
        label.setStyle("-fx-background-color: #e0f2fe; -fx-text-fill: #0369a1; -fx-background-radius: 6; -fx-font-weight: 700; -fx-border-color: #7dd3fc; -fx-border-radius: 6;");
        label.setOnMouseClicked(event -> appendFromGridClick(qubit));
        return new StackPane(label);
    }

    private String cellStyle(final String text) {
        if ("--".equals(text)) {
            return "Layer view".equals(layoutModeBox.getValue())
                ? "-fx-text-fill: #94a3b8; -fx-border-color: #e2e8f0; -fx-border-radius: 4;"
                : "-fx-text-fill: #94a3b8; -fx-border-color: transparent;";
        }
        if ("CTRL".equals(text)) {
            return "-fx-background-color: #111827; -fx-text-fill: white; -fx-background-radius: 17; -fx-font-weight: 700;";
        }
        if ("M".equals(text)) {
            return "-fx-background-color: #f59e0b; -fx-text-fill: #111827; -fx-background-radius: 6; -fx-font-weight: 700;";
        }
        return "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 700;";
    }

    private double cellWidth() {
        if (operations.size() >= 80) {
            return 38.0;
        }
        if (operations.size() >= 40) {
            return 46.0;
        }
        return 58.0;
    }

    private void appendFromGridClick(final String qubit) {
        clearActiveJsonProgram();
        final String gate = gateBox.getValue();
        if (isSingleQubitGate(gate)) {
            operations.add(new DesktopIrOperationSpec(
                gate,
                qubit,
                secondaryQubitBox.getValue(),
                tertiaryQubitBox.getValue(),
                classicalBitBox.getValue(),
                angle()
            ));
            clearPending();
            refreshWorkspace();
            return;
        }
        if ("MEASURE".equals(gate)) {
            operations.add(new DesktopIrOperationSpec(
                gate,
                qubit,
                secondaryQubitBox.getValue(),
                tertiaryQubitBox.getValue(),
                classicalBitBox.getValue(),
                angle()
            ));
            clearPending();
            refreshWorkspace();
            return;
        }
        if ("BARRIER".equals(gate)) {
            operations.add(new DesktopIrOperationSpec(
                gate,
                qubit,
                secondaryQubitBox.getValue(),
                tertiaryQubitBox.getValue(),
                classicalBitBox.getValue(),
                angle()
            ));
            clearPending();
            refreshWorkspace();
            return;
        }
        pendingQubits.add(qubit);
        final int required = "CCX".equals(gate) ? 3 : 2;
        if (pendingQubits.size() < required) {
            builderHintLabel.setText("Selected " + pendingQubits.size() + "/" + required + " qubits for " + gate + ".");
            return;
        }
        operations.add(new DesktopIrOperationSpec(
            gate,
            pendingQubits.get(0),
            pendingQubits.get(1),
            "CCX".equals(gate) ? pendingQubits.get(2) : tertiaryQubitBox.getValue(),
            classicalBitBox.getValue(),
            angle()
        ));
        clearPending();
        refreshWorkspace();
    }

    private static boolean isSingleQubitGate(final String gate) {
        return switch (gate) {
            case "H", "X", "Y", "Z", "S", "T", "RX", "RY", "RZ", "PHASE", "RESET" -> true;
            default -> false;
        };
    }

    private void clearPending() {
        pendingQubits.clear();
        builderHintLabel.setText("Select a gate, then click a qubit lane to place it.");
    }

    private boolean isLsbWireOrder() {
        return "LSB top-to-bottom".equals(wireOrderBox.getValue());
    }

    private void renderGateInfo() {
        final String gate = gateBox.getValue();
        gateInfoArea.setText(switch (gate) {
            case "H" -> gateInfo(
                "H",
                "Hadamard gate. Creates superposition on one qubit.",
                "1 qubit",
                "No parameters"
            );
            case "X" -> gateInfo(
                "X",
                "Pauli-X bit-flip gate.",
                "1 qubit",
                "No parameters"
            );
            case "Y" -> gateInfo(
                "Y",
                "Pauli-Y gate.",
                "1 qubit",
                "No parameters"
            );
            case "Z" -> gateInfo(
                "Z",
                "Pauli-Z phase-flip gate.",
                "1 qubit",
                "No parameters"
            );
            case "S" -> gateInfo(
                "S",
                "Quarter-turn phase gate.",
                "1 qubit",
                "No parameters"
            );
            case "T" -> gateInfo(
                "T",
                "Eighth-turn phase gate.",
                "1 qubit",
                "No parameters"
            );
            case "RX", "RY", "RZ", "PHASE" -> gateInfo(
                gate,
                "Parameterized one-qubit rotation/phase operation.",
                "1 qubit",
                "Uses Angle field"
            );
            case "CX", "CY", "CZ", "CH" -> gateInfo(
                gate,
                "Controlled two-qubit operation. In graphical placement, click control first and target second.",
                "2 qubits",
                "No parameters"
            );
            case "SWAP" -> gateInfo(
                "SWAP",
                "Swaps two qubit states.",
                "2 qubits",
                "No parameters"
            );
            case "CCX" -> gateInfo(
                "CCX",
                "Toffoli gate. In graphical placement, click two controls and then target.",
                "3 qubits",
                "No parameters"
            );
            case "MEASURE" -> gateInfo(
                "MEASURE",
                "Measures a qubit into the selected classical bit.",
                "1 qubit + 1 classical bit",
                "No parameters"
            );
            case "RESET" -> gateInfo(
                "RESET",
                "Resets one qubit to the zero state.",
                "1 qubit",
                "No parameters"
            );
            case "BARRIER" -> gateInfo(
                "BARRIER",
                "Visualization/scheduling barrier for selected qubits.",
                "2 qubits in current desktop shortcut",
                "No parameters"
            );
            default -> gateInfo(
                gate,
                "Unknown desktop gate.",
                "Unknown",
                "Unknown"
            );
        });
    }

    private static String gateInfo(
        final String gate,
        final String description,
        final String arity,
        final String parameters
    ) {
        return "Gate: " + gate + System.lineSeparator()
            + "Description: " + description + System.lineSeparator()
            + "Arity: " + arity + System.lineSeparator()
            + "Parameters: " + parameters + System.lineSeparator()
            + "Model: native Quantum IR operation generated through Java DSL.";
    }

    private String generateCurrentJavaDsl() {
        return workspaceService.generateJavaDsl(
            circuitNameField.getText(),
            qregNameField.getText(),
            integerValue(qregSizeField),
            cregNameField.getText(),
            integerValue(cregSizeField),
            operations
        );
    }

    private QuantumProgram buildNativeProgram() {
        if (activeJsonProgram != null) {
            return activeJsonProgram;
        }
        return workspaceService.buildProgram(
            circuitNameField.getText(),
            qregNameField.getText(),
            integerValue(qregSizeField),
            cregNameField.getText(),
            integerValue(cregSizeField),
            operations
        );
    }

    private DesktopWorkflowResult validateNative() {
        final ValidationResult validation = workspaceService.validate(buildNativeProgram());
        return DesktopWorkflowResult.of(
            DesktopAction.VALIDATE,
            validation.isValid(),
            validation.isValid() ? "VALID" : "INVALID",
            "Errors: " + validation.errorCount(),
            render(validation)
        );
    }

    private DesktopWorkflowResult inspectNative() {
        final var inspection = workspaceService.inspect(
            buildNativeProgram(),
            targetFormatBox.getValue()
        );
        return DesktopWorkflowResult.of(
            DesktopAction.INSPECT,
            true,
            "INSPECTED",
            "Circuits: " + inspection.circuitSummaries().size(),
            render(inspection)
        );
    }

    private DesktopWorkflowResult resourcesNative() {
        final ResourceEstimate resources = workspaceService.resources(
            buildNativeProgram(),
            24
        );
        return DesktopWorkflowResult.of(
            DesktopAction.RESOURCES,
            true,
            "RESOURCES_ESTIMATED",
            "Qubits: " + resources.qubitCount() + ", operations: " + resources.operationCount(),
            render(resources)
        );
    }

    private DesktopWorkflowResult timelineNative() {
        final ProgramTimeline timeline = workspaceService.timeline(buildNativeProgram());
        return DesktopWorkflowResult.of(
            DesktopAction.CIRCUIT,
            true,
            "TIMELINE_BUILT",
            "Circuits: " + timeline.circuits().size(),
            render(timeline),
            renderTimelineSummary(timeline)
        );
    }

    private DesktopWorkflowResult jsonNative() {
        final QuantumIrWriteResult write = workspaceService.writeJson(buildNativeProgram());
        return DesktopWorkflowResult.of(
            DesktopAction.JSON,
            write.isSuccess(),
            write.isSuccess() ? "JSON_WRITTEN" : "JSON_FAILED",
            "Diagnostics: " + write.diagnostics().size(),
            write.hasContent() ? write.content() : render(write)
        );
    }

    private void saveNativeJson(final Stage stage) {
        final FileChooser chooser = new FileChooser();
        chooser.setTitle("Save native Quantum IR JSON");
        chooser.setInitialFileName(circuitNameField.getText() + ".quantum.json");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
            "Quantum IR JSON",
            "*.json"
        ));
        final java.io.File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }
        try {
            final QuantumIrWriteResult write = workspaceService.writeJson(buildNativeProgram());
            if (!write.hasContent()) {
                diagnosticsArea.setText(render(write));
                statusLabel.setText("Native JSON was not saved");
                return;
            }
            Files.writeString(
                file.toPath(),
                write.content()
            );
            statusLabel.setText("Saved native JSON: " + file.getAbsolutePath());
        } catch (final Exception exception) {
            diagnosticsArea.setText(exceptionMessage(exception));
            statusLabel.setText("Save native JSON failed");
        }
    }

    private void openNativeJson(final Stage stage) {
        final FileChooser chooser = new FileChooser();
        chooser.setTitle("Open native Quantum IR JSON");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
            "Quantum IR JSON",
            "*.json"
        ));
        final java.io.File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }
        try {
            final String content = Files.readString(file.toPath());
            final QuantumIrReadResult read = workspaceService.readJson(content);
            nativeJsonArea.setText(content);
            diagnosticsArea.setText(render(read));
            if (read.isSuccess()) {
                activeJsonProgram = read.program();
                inspectorArea.setText(render(workspaceService.inspect(
                    read.program(),
                    targetFormatBox.getValue()
                )));
                preflightArea.setText(render(workspaceService.preflight(
                    read.program(),
                    targetFormatBox.getValue()
                )));
                statusLabel.setText("Opened native JSON: " + file.getAbsolutePath());
                refreshWorkspace();
            } else {
                statusLabel.setText("Native JSON has diagnostics");
            }
        } catch (final Exception exception) {
            diagnosticsArea.setText(exceptionMessage(exception));
            statusLabel.setText("Open native JSON failed");
        }
    }

    private DesktopWorkflowResult simulateNative() {
        final SimulationResult simulation = workspaceService.simulate(
            buildNativeProgram(),
            shots(),
            seed()
        );
        return DesktopWorkflowResult.of(
            DesktopAction.SIMULATE,
            simulation.isSuccess(),
            simulation.isSuccess() ? "SIMULATED" : "SIMULATION_FAILED",
            "Qubits: " + simulation.qubitCount() + ", shots: " + simulation.shots(),
            renderSimulation(simulation)
        );
    }

    private DesktopWorkflowResult exportNative() {
        final CompilerResult result = workspaceService.export(
            buildNativeProgram(),
            targetFormatBox.getValue(),
            executionOptions()
        );
        final String generated = result.hasExportResult()
            && result.exportResult().hasContent()
                ? result.exportResult().content()
                : "";
        return DesktopWorkflowResult.of(
            DesktopAction.COMPILE,
            result.isSuccess(),
            result.status().name(),
            "Stages: " + result.stageRecords().size(),
            render(result),
            generated
        );
    }

    private DesktopWorkflowResult preflightNative() {
        final var result = workspaceService.preflight(
            buildNativeProgram(),
            targetFormatBox.getValue()
        );
        return DesktopWorkflowResult.of(
            DesktopAction.PREFLIGHT,
            result.isSuccess(),
            result.status().name(),
            "Diagnostics: " + result.diagnostics().size(),
            render(result)
        );
    }

    private DesktopWorkflowResult compatibilityNative() {
        final var result = workspaceService.compatibility(
            buildNativeProgram(),
            shots(),
            seed(),
            executionOptions()
        );
        return DesktopWorkflowResult.of(
            DesktopAction.COMPATIBILITY,
            result.isSuccess(),
            result.isSuccess() ? "COMPATIBLE" : "INCOMPATIBLE",
            "Targets: " + result.targets().size(),
            render(result)
        );
    }

    private DesktopWorkflowResult transformNative() {
        final TransformationResult result = workspaceService.transform(
            buildNativeProgram(),
            targetFormatBox.getValue(),
            canonicalizeParametersBox.isSelected(),
            removeIdentityBox.isSelected(),
            inlineCompositeBox.isSelected(),
            targetLoweringBox.isSelected()
        );
        return DesktopWorkflowResult.of(
            DesktopAction.COMPILE,
            result.isSuccess(),
            result.isSuccess() ? "TRANSFORMED" : "TRANSFORM_FAILED",
            "Applied: " + result.appliedSteps().size() + ", skipped: " + result.skippedSteps().size(),
            render(result)
        );
    }

    private DesktopWorkflowResult javaDslNative() {
        final String code = generateCurrentJavaDsl();
        return DesktopWorkflowResult.of(
            DesktopAction.CIRCUIT,
            true,
            "JAVA_DSL_GENERATED",
            "Operations: " + operations.size(),
            code
        );
    }

    private DesktopWorkflowResult workflowNative() {
        final ProductWorkflowReport result = workspaceService.workflow(
            buildNativeProgram(),
            targetFormatBox.getValue(),
            shots(),
            seed(),
            executionOptions()
        );
        return DesktopWorkflowResult.of(
            DesktopAction.WORKFLOW,
            result.isSuccess(),
            result.status().name(),
            "Validation: " + (result.validation() != null)
                + ", compile: " + (result.compiler() != null)
                + ", backend: " + result.hasBackendExecution(),
            render(result)
        );
    }

    private void applyNativeJson() {
        statusLabel.setText("Apply JSON running...");
        final String content = nativeJsonArea.getText();
        final Task<QuantumIrReadResult> task = new Task<>() {
            @Override
            protected QuantumIrReadResult call() {
                return workspaceService.readJson(content);
            }
        };
        task.setOnSucceeded(event -> {
            final QuantumIrReadResult read = task.getValue();
            diagnosticsArea.setText(render(read));
            if (read.isSuccess()) {
                activeJsonProgram = read.program();
                final ValidationResult validation = workspaceService.validate(read.program());
                statusLabel.setText(validation.isValid()
                    ? "Native JSON applied"
                    : "Native JSON applied with validation errors: " + validation.errorCount());
                refreshWorkspace();
            } else {
                statusLabel.setText("Native JSON rejected: " + read.diagnostics().size() + " diagnostics");
            }
        });
        task.setOnFailed(event -> {
            final Throwable exception = task.getException();
            statusLabel.setText("Apply JSON failed");
            diagnosticsArea.setText(exceptionMessage(exception));
        });
        final Thread thread = new Thread(
            task,
            "quantum-desktop-json-apply"
        );
        thread.setDaemon(true);
        thread.start();
    }

    private void runCommandPalette() {
        final String command = commandPaletteBox.getValue();
        if ("Validate".equals(command)) {
            runNative("Validate", this::validateNative);
        } else if ("Inspect".equals(command)) {
            runNative("Inspect", this::inspectNative);
        } else if ("Simulate".equals(command)) {
            runNative("Simulate", this::simulateNative);
        } else if ("Export".equals(command)) {
            runNative("Export", this::exportNative);
        } else if ("Preflight".equals(command)) {
            runNative("Preflight", this::preflightNative);
        } else if ("Compatibility".equals(command)) {
            runNative("Compatibility", this::compatibilityNative);
        } else if ("Transform".equals(command)) {
            runNative("Transform", this::transformNative);
        } else if ("JSON".equals(command)) {
            runNative("JSON", this::jsonNative);
        } else if ("Java DSL".equals(command)) {
            runNative("Java DSL", this::javaDslNative);
        } else if ("Workflow".equals(command)) {
            runNative("Workflow", this::workflowNative);
        } else if ("Apply Native JSON".equals(command)) {
            applyNativeJson();
        }
    }

    private void refreshDiagnosticList(
        final ValidationResult validation,
        final String preflightStatus,
        final int preflightDiagnosticCount
    ) {
        final ArrayList<String> items = new ArrayList<>();
        items.add(validation.isValid()
            ? "VALIDATION OK"
            : "VALIDATION ERRORS " + validation.errorCount());
        for (int i = 0; i < validation.errors().size(); i++) {
            items.add("VALIDATION #" + i + " " + validation.error(i).message());
        }
        items.add("PREFLIGHT " + targetFormatBox.getValue() + " " + preflightStatus
            + " diagnostics=" + preflightDiagnosticCount);
        if (activeJsonProgram == null) {
            for (int i = 0; i < operations.size(); i++) {
                items.add("OP #" + i + " " + operations.get(i).label());
            }
        } else {
            items.add("ACTIVE JSON Source: native JSON text");
        }
        diagnosticList.getItems().setAll(items);
    }

    private void navigateDiagnostic(final String item) {
        if (item == null) {
            return;
        }
        diagnosticsArea.setText(item);
        if (!item.startsWith("OP #")) {
            return;
        }
        final int start = 4;
        final int end = item.indexOf(
            ' ',
            start
        );
        if (end <= start) {
            return;
        }
        try {
            final int index = Integer.parseInt(item.substring(
                start,
                end
            ));
            if (
                index >= 0
                && index < operations.size()
            ) {
                operationList.getSelectionModel().select(index);
            }
        } catch (final NumberFormatException ignored) {
            // Invalid navigation labels are rendered as diagnostics only.
        }
    }

    private void clearActiveJsonProgram() {
        activeJsonProgram = null;
    }

    private void runNative(
        final String action,
        final DesktopWorkflow workflow
    ) {
        statusLabel.setText(action + " running...");
        final Task<DesktopWorkflowResult> task = new Task<>() {
            @Override
            protected DesktopWorkflowResult call() throws Exception {
                return workflow.run();
            }
        };
        task.setOnSucceeded(event -> {
            final DesktopWorkflowResult result = task.getValue();
            statusLabel.setText(result.action() + " / " + result.status() + " / " + result.summary());
            refreshWorkspace();
            if (result.action() == DesktopAction.SIMULATE) {
                simulationArea.setText(result.content());
            } else if (result.action() == DesktopAction.JSON) {
                if (result.status().startsWith("NATIVE_JSON_")) {
                    diagnosticsArea.setText(result.content());
                } else {
                    nativeJsonArea.setText(result.content());
                    diagnosticsArea.setText(result.summary());
                }
            } else if (result.action() == DesktopAction.INSPECT) {
                inspectorArea.setText(result.content());
            } else if (result.action() == DesktopAction.RESOURCES) {
                resourcesArea.setText(result.content());
            } else if (result.action() == DesktopAction.PREFLIGHT) {
                preflightArea.setText(result.content());
            } else if (result.action() == DesktopAction.COMPATIBILITY) {
                compatibilityArea.setText(result.content());
            } else if (
                "TRANSFORMED".equals(result.status())
                || "TRANSFORM_FAILED".equals(result.status())
            ) {
                transformationArea.setText(result.content());
            } else if ("JAVA_DSL_GENERATED".equals(result.status())) {
                javaDslArea.setText(result.content());
            } else {
                diagnosticsArea.setText(result.content());
            }
            if (!result.generatedContent().isBlank()) {
                generatedArea.setText(result.generatedContent());
            }
        });
        task.setOnFailed(event -> {
            final Throwable exception = task.getException();
            statusLabel.setText(action + " failed");
            diagnosticsArea.setText(exceptionMessage(exception));
        });
        startBackground(task);
    }

    private void runExternal(final DesktopWorkflow workflow) {
        statusLabel.setText("External workflow running...");
        final Task<DesktopWorkflowResult> task = new Task<>() {
            @Override
            protected DesktopWorkflowResult call() throws Exception {
                return workflow.run();
            }
        };
        task.setOnSucceeded(event -> {
            final DesktopWorkflowResult result = task.getValue();
            statusLabel.setText(result.action() + " / " + result.status() + " / " + result.summary());
            externalResultArea.setText(result.content());
            externalGeneratedArea.setText(result.generatedContent());
        });
        task.setOnFailed(event -> {
            final Throwable exception = task.getException();
            statusLabel.setText("External workflow failed");
            externalResultArea.setText(exceptionMessage(exception));
            externalGeneratedArea.clear();
        });
        startBackground(task);
    }

    private void startBackground(final Task<DesktopWorkflowResult> task) {
        final Thread thread = new Thread(
            task,
            "quantum-desktop-workflow"
        );
        thread.setDaemon(true);
        thread.start();
    }

    private void refreshReferenceBoxes() {
        final List<String> qubits = new ArrayList<>();
        final String qreg = qregNameField.getText().isBlank()
            ? "q"
            : qregNameField.getText();
        for (int i = 0; i < Math.max(1, integerValue(qregSizeField)); i++) {
            qubits.add(qreg + "[" + i + "]");
        }
        final List<String> bits = new ArrayList<>();
        final String creg = cregNameField.getText().isBlank()
            ? "c"
            : cregNameField.getText();
        for (int i = 0; i < Math.max(1, integerValue(cregSizeField)); i++) {
            bits.add(creg + "[" + i + "]");
        }
        setComboItems(
            primaryQubitBox,
            qubits
        );
        setComboItems(
            secondaryQubitBox,
            qubits
        );
        setComboItems(
            tertiaryQubitBox,
            qubits
        );
        setComboItems(
            classicalBitBox,
            bits
        );
    }

    private void setComboItems(
        final ComboBox<String> comboBox,
        final List<String> values
    ) {
        final String current = comboBox.getValue();
        comboBox.getItems().setAll(values);
        comboBox.setValue(values.contains(current)
            ? current
            : values.get(0));
    }

    private String renderTimelineSummary(final ProgramTimeline timeline) {
        final StringBuilder summary = new StringBuilder();
        for (int i = 0; i < timeline.circuits().size(); i++) {
            final CircuitTimeline circuit = timeline.circuits().get(i);
            summary.append("Circuit ")
                .append(circuit.circuitName())
                .append(System.lineSeparator());
            for (int j = 0; j < circuit.steps().size(); j++) {
                final CircuitTimelineStep step = circuit.steps().get(j);
                summary.append("  #")
                    .append(step.operationIndex())
                    .append(" ")
                    .append(step.operationKind())
                    .append(" ")
                    .append(step.label())
                    .append(System.lineSeparator());
            }
        }
        return summary.toString();
    }

    private void openExternalFile(final Stage stage) {
        final FileChooser chooser = new FileChooser();
        chooser.setTitle("Open external quantum program");
        final java.io.File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }
        try {
            externalSourceArea.setText(Files.readString(file.toPath()));
            statusLabel.setText("Opened " + file.getAbsolutePath());
        } catch (final Exception exception) {
            externalResultArea.setText(exceptionMessage(exception));
        }
    }

    private DesktopExecutionOptions executionOptions() {
        return DesktopExecutionOptions.builder()
            .fast(fastBox.isSelected())
            .skipValidation(skipValidationBox.isSelected())
            .skipInspection(skipInspectionBox.isSelected())
            .skipPreflight(skipPreflightBox.isSelected())
            .skipTransformation(skipTransformationBox.isSelected())
            .skipSimulation(skipSimulationBox.isSelected())
            .skipCompiler(skipCompilerBox.isSelected())
            .skipBackend(skipBackendBox.isSelected())
            .build();
    }

    private int shots() {
        return integerValue(shotsField);
    }

    private long seed() {
        return Long.parseLong(seedField.getText());
    }

    private double angle() {
        return Double.parseDouble(angleField.getText());
    }

    private static int integerValue(final TextField field) {
        try {
            return Integer.parseInt(field.getText());
        } catch (final NumberFormatException exception) {
            return 1;
        }
    }

    private String render(final Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (final JsonProcessingException exception) {
            throw new IllegalStateException(
                "Desktop value cannot be rendered as JSON.",
                exception
            );
        }
    }

    private String renderSimulation(final SimulationResult simulation) {
        final StringBuilder text = new StringBuilder();
        text.append("Simulation").append(System.lineSeparator());
        text.append("  success: ").append(simulation.isSuccess()).append(System.lineSeparator());
        text.append("  qubits: ").append(simulation.qubitCount()).append(System.lineSeparator());
        text.append("  classical bits: ").append(simulation.classicalBitCount()).append(System.lineSeparator());
        text.append("  shots: ").append(simulation.shots()).append(System.lineSeparator());
        text.append(System.lineSeparator());
        text.append("Counts").append(System.lineSeparator());
        simulation.counts().forEach((state, count) -> text.append("  ")
            .append(state)
            .append(": ")
            .append(count)
            .append(System.lineSeparator()));
        text.append(System.lineSeparator());
        text.append("Exact probabilities").append(System.lineSeparator());
        for (int i = 0; i < simulation.stateVector().size(); i++) {
            final double real = simulation.stateVector().get(i).real();
            final double imaginary = simulation.stateVector().get(i).imaginary();
            final double probability = real * real + imaginary * imaginary;
            if (
                hideZeroProbabilityBox.isSelected()
                && probability == 0.0
            ) {
                continue;
            }
            text.append("  ")
                .append(simulation.stateVector().get(i).basisState())
                .append(": ")
                .append(probability)
                .append(System.lineSeparator());
        }
        text.append(System.lineSeparator());
        text.append("State vector amplitudes").append(System.lineSeparator());
        for (int i = 0; i < simulation.stateVector().size(); i++) {
            final double real = simulation.stateVector().get(i).real();
            final double imaginary = simulation.stateVector().get(i).imaginary();
            final double probability = real * real + imaginary * imaginary;
            if (
                hideZeroProbabilityBox.isSelected()
                && probability == 0.0
            ) {
                continue;
            }
            text.append("  ")
                .append(simulation.stateVector().get(i).basisState())
                .append(": ")
                .append(real)
                .append(" + ")
                .append(imaginary)
                .append("i")
                .append(System.lineSeparator());
        }
        if (!simulation.diagnostics().isEmpty()) {
            text.append(System.lineSeparator());
            text.append("Diagnostics").append(System.lineSeparator());
            for (int i = 0; i < simulation.diagnostics().size(); i++) {
                text.append("  ")
                    .append(simulation.diagnostics().get(i).severity())
                    .append(" ")
                    .append(simulation.diagnostics().get(i).code())
                    .append(": ")
                    .append(simulation.diagnostics().get(i).message())
                    .append(System.lineSeparator());
            }
        }
        return text.toString();
    }

    private static String exceptionMessage(final Throwable exception) {
        return exception.getMessage() == null
            ? exception.toString()
            : exception.getMessage();
    }

    private static VBox section(
        final String title,
        final Node... nodes
    ) {
        final Label label = new Label(title);
        label.getStyleClass().add("section-title");
        final VBox box = new VBox(8);
        box.getChildren().add(label);
        box.getChildren().addAll(nodes);
        box.getStyleClass().add("section-card");
        return box;
    }

    private static HBox fieldRow(
        final String label,
        final Node node
    ) {
        final Label text = new Label(label);
        text.setMinWidth(74);
        final HBox row = new HBox(
            8,
            text,
            node
        );
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(
            node,
            Priority.ALWAYS
        );
        return row;
    }

    private static HBox fieldRow(
        final String label,
        final Node first,
        final Node second
    ) {
        final Label text = new Label(label);
        text.setMinWidth(74);
        final HBox row = new HBox(
            8,
            text,
            first,
            second
        );
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(
            first,
            Priority.ALWAYS
        );
        return row;
    }

    private static FlowPane actionFlow(final Node... nodes) {
        return new FlowPane(
            8,
            8,
            nodes
        );
    }

    private static Label headerLabel(final String text) {
        final Label label = new Label(text);
        label.getStyleClass().add("header-subtitle");
        return label;
    }

    private static ScrollPane scrollable(final Node node) {
        final ScrollPane scrollPane = new ScrollPane(node);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        return scrollPane;
    }

    private void markExpert(final Node... nodes) {
        for (int i = 0; i < nodes.length; i++) {
            expertOnlyNodes.add(nodes[i]);
        }
        applyExperienceMode();
    }

    private void applyExperienceMode() {
        final boolean expert = "Expert".equals(experienceModeBox.getValue());
        for (int i = 0; i < expertOnlyNodes.size(); i++) {
            final Node node = expertOnlyNodes.get(i);
            node.setVisible(expert);
            node.setManaged(expert);
        }
        for (int i = 0; i < expertOnlyTabs.size(); i++) {
            expertOnlyTabs.get(i).setDisable(!expert);
        }
        builderHintLabel.setText(expert
            ? "Expert mode: full IR pipeline is visible."
            : "Beginner mode: build, inspect, simulate and export without advanced pipeline noise.");
        if (overviewArea != null) {
            try {
                overviewArea.setText(renderOverview(
                    buildNativeProgram(),
                    workspaceService.writeJson(buildNativeProgram())
                ));
            } catch (final Exception ignored) {
                // Overview will be refreshed after the workspace becomes valid.
            }
        }
    }

    private static Button primaryButton(
        final String text,
        final Runnable action
    ) {
        final Button button = new Button(text);
        button.setOnAction(event -> action.run());
        button.getStyleClass().add("primary-button");
        return button;
    }

    private static Button secondaryButton(
        final String text,
        final Runnable action
    ) {
        final Button button = new Button(text);
        button.setOnAction(event -> action.run());
        button.getStyleClass().add("secondary-button");
        return button;
    }

    private static Tab tab(
        final String title,
        final Node node
    ) {
        final Tab tab = new Tab(
            title,
            node
        );
        tab.setClosable(false);
        return tab;
    }

    private Tab expertTab(
        final String title,
        final Node node
    ) {
        final Tab tab = tab(
            title,
            node
        );
        expertOnlyTabs.add(tab);
        return tab;
    }

    private static void addSetting(
        final GridPane grid,
        final int row,
        final String label,
        final TextField field
    ) {
        grid.add(
            new Label(label),
            0,
            row
        );
        grid.add(
            field,
            1,
            row
        );
        GridPane.setHgrow(
            field,
            Priority.ALWAYS
        );
    }

    @FunctionalInterface
    private interface DesktopWorkflow {

        DesktopWorkflowResult run() throws Exception;
    }
}

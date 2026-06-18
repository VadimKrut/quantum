/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui;

import static ru.pathcreator.vadim.quantum.desktop.ui.layout.DesktopUiNodes.actionFlow;
import static ru.pathcreator.vadim.quantum.desktop.ui.layout.DesktopUiNodes.fieldRow;
import static ru.pathcreator.vadim.quantum.desktop.ui.layout.DesktopUiNodes.primaryButton;
import static ru.pathcreator.vadim.quantum.desktop.ui.layout.DesktopUiNodes.secondaryButton;
import static ru.pathcreator.vadim.quantum.desktop.ui.layout.DesktopUiNodes.section;
import static ru.pathcreator.vadim.quantum.desktop.ui.audit.DesktopScreenshotSupport.saveStageScreenshot;
import static ru.pathcreator.vadim.quantum.desktop.ui.audit.DesktopUiTraversal.collectNodes;
import static ru.pathcreator.vadim.quantum.desktop.ui.audit.DesktopUiTraversal.fireVisibleButton;
import static ru.pathcreator.vadim.quantum.desktop.ui.audit.DesktopUiTraversal.selectAllTabs;
import static ru.pathcreator.vadim.quantum.desktop.ui.audit.DesktopUiTraversal.selectTabByText;
import static ru.pathcreator.vadim.quantum.desktop.ui.audit.DesktopUiSmokeAssertions.verifyAllGateButtons;
import static ru.pathcreator.vadim.quantum.desktop.ui.audit.DesktopUiSmokeAssertions.verifyCircuitContainsEveryGate;
import static ru.pathcreator.vadim.quantum.desktop.ui.audit.DesktopUiSmokeAssertions.verifyGateButtonsSelectAndUpdate;
import static ru.pathcreator.vadim.quantum.desktop.ui.audit.DesktopUiSmokeAssertions.verifyFullIrSurface;
import static ru.pathcreator.vadim.quantum.desktop.ui.audit.DesktopUiSmokeAssertions.verifyCircuitHasRenderedCells;
import static ru.pathcreator.vadim.quantum.desktop.ui.audit.DesktopUiSmokeAssertions.verifyQSphereHasInteractionHandlers;
import static ru.pathcreator.vadim.quantum.desktop.ui.audit.DesktopUiSmokeAssertions.verifyQSphereMarkerCountIsBounded;
import static ru.pathcreator.vadim.quantum.desktop.ui.audit.DesktopUiSmokeAssertions.verifyQSphereRendered;
import static ru.pathcreator.vadim.quantum.desktop.ui.audit.DesktopUiSmokeAssertions.verifyQSphereSummaryBadge;
import static ru.pathcreator.vadim.quantum.desktop.ui.audit.DesktopUiSmokeSemanticAssertions.verifyMeasuredBellSimulationSemantics;
import static ru.pathcreator.vadim.quantum.desktop.ui.audit.DesktopUiSmokeSemanticAssertions.verifyPureBellStateVectorSemantics;
import static ru.pathcreator.vadim.quantum.desktop.ui.circuit.DesktopCircuitScrollSupport.scrollOperationIntoView;
import static ru.pathcreator.vadim.quantum.desktop.ui.circuit.DesktopCircuitSelectionSupport.intersectingOperationIndices;
import static ru.pathcreator.vadim.quantum.desktop.ui.input.DesktopInputParsers.doubleOrDefault;
import static ru.pathcreator.vadim.quantum.desktop.ui.input.DesktopInputParsers.exceptionMessage;
import static ru.pathcreator.vadim.quantum.desktop.ui.input.DesktopInputParsers.positiveIntegerOrOne;
import static ru.pathcreator.vadim.quantum.desktop.ui.input.DesktopInputParsers.positiveLongOrDefault;
import static ru.pathcreator.vadim.quantum.desktop.ui.audit.DesktopUiAutomationRunner.scheduleSmoke;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuButton;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.TransferMode;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightResult;
import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapabilityProfile;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrReadResult;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrWriteResult;
import ru.pathcreator.vadim.quantum.application.resource.ResourceEstimate;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.desktop.ui.circuit.DesktopCircuitCanvasRenderer;
import ru.pathcreator.vadim.quantum.desktop.ui.circuit.DesktopPhaseDiskView;
import ru.pathcreator.vadim.quantum.desktop.ui.circuit.DesktopLargeProgramRenderPolicy;
import ru.pathcreator.vadim.quantum.desktop.ui.circuit.DesktopSelectionRectangleController;
import ru.pathcreator.vadim.quantum.desktop.ui.ir.DesktopIrOperationSurfaceCatalog;
import ru.pathcreator.vadim.quantum.desktop.ui.operation.DesktopOperationListCellFactory;
import ru.pathcreator.vadim.quantum.desktop.ui.qsphere.DesktopQSphereView;
import ru.pathcreator.vadim.quantum.desktop.ui.audit.DesktopVisualAuditController;
import ru.pathcreator.vadim.quantum.desktop.ui.audit.DesktopCompatibilityScreenshotPreview;
import ru.pathcreator.vadim.quantum.desktop.ui.diagnostic.DesktopDiagnosticListRenderer;
import ru.pathcreator.vadim.quantum.desktop.ui.layout.DesktopCircuitWorkspaceResult;
import ru.pathcreator.vadim.quantum.desktop.ui.layout.DesktopCircuitWorkspaceView;
import ru.pathcreator.vadim.quantum.desktop.ui.layout.DesktopExecutionSettingsView;
import ru.pathcreator.vadim.quantum.desktop.ui.layout.DesktopExternalWorkspaceView;
import ru.pathcreator.vadim.quantum.desktop.ui.layout.DesktopGateCatalogView;
import ru.pathcreator.vadim.quantum.desktop.ui.layout.DesktopHeaderView;
import ru.pathcreator.vadim.quantum.desktop.ui.layout.DesktopInspectionControlsView;
import ru.pathcreator.vadim.quantum.desktop.ui.layout.DesktopMainTabsView;
import ru.pathcreator.vadim.quantum.desktop.ui.layout.DesktopNativeWorkspaceView;
import ru.pathcreator.vadim.quantum.desktop.ui.layout.DesktopResultTabsResult;
import ru.pathcreator.vadim.quantum.desktop.ui.layout.DesktopResultTabsView;
import ru.pathcreator.vadim.quantum.desktop.ui.layout.DesktopTextAreaConfigurator;
import ru.pathcreator.vadim.quantum.desktop.ui.render.DesktopGateInfoRenderer;
import ru.pathcreator.vadim.quantum.desktop.ui.render.DesktopOperationLabelRenderer;
import ru.pathcreator.vadim.quantum.desktop.ui.render.DesktopProgramTextRenderer;
import ru.pathcreator.vadim.quantum.desktop.ui.render.DesktopSimulationTextRenderer;
import ru.pathcreator.vadim.quantum.desktop.ui.render.DesktopTimelineRenderer;
import ru.pathcreator.vadim.quantum.desktop.ui.visualization.DesktopSimulationVisualizationsView;
import ru.pathcreator.vadim.quantum.desktop.workflow.DesktopAction;
import ru.pathcreator.vadim.quantum.desktop.workflow.DesktopExecutionOptions;
import ru.pathcreator.vadim.quantum.desktop.workflow.DesktopWorkflowResult;
import ru.pathcreator.vadim.quantum.desktop.workflow.DesktopWorkflowService;
import ru.pathcreator.vadim.quantum.desktop.workflow.nativeflow.DesktopNativeWorkflowFacade;
import ru.pathcreator.vadim.quantum.desktop.workspace.DesktopIrOperationSpec;
import ru.pathcreator.vadim.quantum.desktop.workspace.DesktopIrProgramSnapshot;
import ru.pathcreator.vadim.quantum.desktop.workspace.DesktopIrWorkspaceService;
import ru.pathcreator.vadim.quantum.desktop.workspace.DesktopJavaDslImportResult;
import ru.pathcreator.vadim.quantum.desktop.workspace.DesktopJavaDslImporter;
import ru.pathcreator.vadim.quantum.desktop.workspace.builder.DesktopGridPlacementResult;
import ru.pathcreator.vadim.quantum.desktop.workspace.builder.DesktopGridPlacementService;
import ru.pathcreator.vadim.quantum.desktop.workspace.operation.DesktopCustomOperationRegistry;
import ru.pathcreator.vadim.quantum.desktop.workspace.operation.DesktopOperationReorderService;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

/**
 * Рабочая среда JavaFX для программирования Quantum IR как основной модели.
 */
public final class QuantumDesktopApplication extends Application {

    private static final String PREFERENCES_LAST_FILE_DIRECTORY = "last-file-directory";

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
    private final DesktopGridPlacementService gridPlacementService = new DesktopGridPlacementService();
    private final DesktopOperationReorderService operationReorderService = new DesktopOperationReorderService();
    private final DesktopCustomOperationRegistry customOperationRegistry = new DesktopCustomOperationRegistry();
    private final DesktopCircuitCanvasRenderer circuitCanvasRenderer = new DesktopCircuitCanvasRenderer();
    private final DesktopPhaseDiskView phaseDiskView = new DesktopPhaseDiskView();
    private final DesktopLargeProgramRenderPolicy largeProgramRenderPolicy = new DesktopLargeProgramRenderPolicy();
    private final DesktopSelectionRectangleController selectionRectangleController = new DesktopSelectionRectangleController();
    private final DesktopIrOperationSurfaceCatalog irOperationSurfaceCatalog = new DesktopIrOperationSurfaceCatalog();
    private final DesktopDiagnosticListRenderer diagnosticListRenderer = new DesktopDiagnosticListRenderer();
    private final DesktopCircuitWorkspaceView circuitWorkspaceView = new DesktopCircuitWorkspaceView();
    private final DesktopExecutionSettingsView executionSettingsView = new DesktopExecutionSettingsView();
    private final DesktopExternalWorkspaceView externalWorkspaceView = new DesktopExternalWorkspaceView();
    private final DesktopGateCatalogView gateCatalogView = new DesktopGateCatalogView();
    private final DesktopHeaderView headerView = new DesktopHeaderView();
    private final DesktopInspectionControlsView inspectionControlsView = new DesktopInspectionControlsView();
    private final DesktopMainTabsView mainTabsView = new DesktopMainTabsView();
    private final DesktopNativeWorkspaceView nativeWorkspaceView = new DesktopNativeWorkspaceView();
    private final DesktopResultTabsView resultTabsView = new DesktopResultTabsView();
    private final DesktopTextAreaConfigurator textAreaConfigurator = new DesktopTextAreaConfigurator();
    private final DesktopOperationListCellFactory operationListCellFactory = new DesktopOperationListCellFactory();
    private final DesktopQSphereView qSphereView = new DesktopQSphereView();
    private final DesktopGateInfoRenderer gateInfoRenderer = new DesktopGateInfoRenderer();
    private final DesktopOperationLabelRenderer operationLabelRenderer = new DesktopOperationLabelRenderer();
    private final DesktopProgramTextRenderer programTextRenderer = new DesktopProgramTextRenderer();
    private final DesktopSimulationTextRenderer simulationTextRenderer = new DesktopSimulationTextRenderer();
    private final DesktopTimelineRenderer timelineRenderer = new DesktopTimelineRenderer();
    private final DesktopSimulationVisualizationsView simulationVisualizationsView = new DesktopSimulationVisualizationsView();
    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final Preferences preferences = Preferences.userNodeForPackage(QuantumDesktopApplication.class);
    private final DesktopNativeWorkflowFacade nativeWorkflowFacade = new DesktopNativeWorkflowFacade(
        workspaceService,
        simulationTextRenderer,
        timelineRenderer,
        this::render
    );
    private final DesktopJavaDslImporter javaDslImporter = new DesktopJavaDslImporter();
    private final DesktopVisualAuditController visualAuditController = new DesktopVisualAuditController();
    private final DesktopCompatibilityScreenshotPreview compatibilityScreenshotPreview = new DesktopCompatibilityScreenshotPreview(workspaceService);
    private final ArrayList<DesktopIrOperationSpec> operations = new ArrayList<>();
    private final ArrayList<List<DesktopIrOperationSpec>> undoStack = new ArrayList<>();
    private final ArrayList<String> pendingQubits = new ArrayList<>();
    private final ArrayList<Integer> selectedOperationIndices = new ArrayList<>();
    private final Rectangle selectionRectangle = new Rectangle();
    private Stage primaryStage;
    private BorderPane rootPane;
    private MenuButton viewMenuButton;
    private TabPane resultTabPane;
    private int inspectionStepIndex = -1;
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
    private final TextField gateSearchField = new TextField();
    private final VBox gateCatalogPane = new VBox(8);
    private final ComboBox<String> wireOrderBox = new ComboBox<>();
    private final ComboBox<String> layoutModeBox = new ComboBox<>();
    private final ComboBox<String> experienceModeBox = new ComboBox<>();
    private final ComboBox<String> languageBox = new ComboBox<>();
    private final ComboBox<String> themeBox = new ComboBox<>();
    private final ComboBox<String> commandPaletteBox = new ComboBox<>();
    private final TextField wikiSearchField = new TextField();
    private final TextField angleField = new TextField("1.5707963267948966");
    private final TextField secondAngleField = new TextField("0.0");
    private final TextField thirdAngleField = new TextField("0.0");
    private final TextField durationField = new TextField("20.0");
    private final ComboBox<String> durationUnitBox = new ComboBox<>();
    private final TextField labelNameField = new TextField("entry");
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
    private final CheckBox autoSimulationBox = new CheckBox("Auto simulate preview");
    private final CheckBox hideZeroProbabilityBox = new CheckBox("Hide zero probabilities");
    private final CheckBox registerBitOrderBox = new CheckBox("Register-order bitstrings");
    private final CheckBox renderHugeCircuitBox = new CheckBox("Render full huge circuit preview");
    private final CheckBox showSimulationTextBox = new CheckBox("Simulation text");
    private final CheckBox showProbabilitiesBox = new CheckBox("Probability chart");
    private final CheckBox showStateVectorBox = new CheckBox("Statevector chart");
    private final CheckBox showQSphereBox = new CheckBox("Q-sphere");
    private final CheckBox showPhaseDisksBox = new CheckBox("Phase disks");
    private final CheckBox showGeneratedCodeBox = new CheckBox("Generated code panel");
    private final VBox circuitRows = new VBox(8);
    private ScrollPane circuitScrollPane;
    private final Label inspectionStepLabel = new Label("Inspect: full circuit");
    private final Label inspectionOperationLabel = new Label("Operation: full circuit");
    private final Slider inspectionSlider = new Slider();
    private boolean updatingInspectionSlider;
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
    private final TextArea fullIrSurfaceArea = new TextArea();
    private final TextArea nativeJsonArea = new TextArea();
    private final TextArea diagnosticsArea = new TextArea();
    private final ListView<String> diagnosticList = new ListView<>();
    private final TextArea generatedArea = new TextArea();
    private final TextArea codePreviewArea = new TextArea();
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
    private int lastPreviewProgramHash;
    private boolean hasLastPreviewProgramHash;
    private volatile SimulationResult lastNativeSimulation;

    public static void main(final String[] args) {
        launch(args);
    }

    @Override
    public void start(final Stage stage) {
        primaryStage = stage;
        initializeControls();
        loadInitialTemplate();
        refreshWorkspace();

        final BorderPane root = new BorderPane();
        rootPane = root;
        root.getStyleClass().add("quantum-root");
        root.setTop(header());
        root.setCenter(mainTabs(stage));
        root.setBottom(footer());

        final Scene scene = new Scene(
            root,
            1440,
            900
        );
        final String stylesheet = QuantumDesktopApplication.class
            .getResource("/ru/pathcreator/vadim/quantum/desktop/ui/quantum-desktop.css")
            .toExternalForm();
        scene.getStylesheets().add(stylesheet);
        applyTheme();
        stage.setTitle("Quantum IR Studio");
        stage.setScene(scene);
        scene.addEventFilter(
            KeyEvent.KEY_PRESSED,
            this::handleGlobalShortcut
        );
        stage.show();
        runUiSmokeIfRequested(stage);
        runVisualAuditIfRequested(stage);
    }

    private void loadInitialTemplate() {
        final String fixture = getParameters().getNamed().get("fixture");
        if (fixture != null) {
            loadAuditFixtureWithoutRefresh(fixture);
            return;
        }
        resetToBlankWorkspace();
    }

    private void initializeControls() {
        gateBox.getItems().setAll(gateCatalogView.gates());
        gateBox.setValue("H");
        gateBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            renderGateInfo();
            refreshGateCatalog();
            updateOperationEditorAvailability();
        });
        gateSearchField.setPromptText("Search gates");
        gateSearchField.textProperty().addListener((observable, oldValue, newValue) -> refreshGateCatalog());
        refreshGateCatalog();
        durationUnitBox.getItems().setAll(
            "DT",
            "NS",
            "US",
            "MS",
            "S"
        );
        durationUnitBox.setValue("NS");
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
        experienceModeBox.setValue("Beginner");
        experienceModeBox.valueProperty().addListener((observable, oldValue, newValue) -> applyExperienceMode());
        languageBox.getItems().setAll(
            "English",
            "Русский"
        );
        languageBox.setValue(initialLanguage());
        languageBox.setPrefWidth(96.0);
        languageBox.valueProperty().addListener((observable, oldValue, newValue) -> applyLanguage());
        themeBox.getItems().setAll(
            "Light",
            "Dark"
        );
        themeBox.setValue(initialTheme());
        themeBox.setPrefWidth(88.0);
        themeBox.valueProperty().addListener((observable, oldValue, newValue) -> applyTheme());
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
        wikiSearchField.setPromptText("Search wiki");
        wikiSearchField.textProperty().addListener((observable, oldValue, newValue) -> renderWikiSearch(newValue));
        targetFormatBox.getItems().setAll(IntegrationFormat.values());
        targetFormatBox.setValue(IntegrationFormat.OPENQASM_3);
        targetFormatBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshWorkspace());
        externalInputFormatBox.getItems().setAll(IntegrationFormat.values());
        externalInputFormatBox.setValue(IntegrationFormat.OPENQASM_2);
        externalTargetFormatBox.getItems().setAll(IntegrationFormat.values());
        externalTargetFormatBox.setValue(IntegrationFormat.OPENQASM_3);
        textAreaConfigurator.editable(nativeJsonArea);
        textAreaConfigurator.readonly(
            diagnosticsArea,
            generatedArea,
            codePreviewArea,
            externalResultArea,
            externalGeneratedArea,
            overviewArea
        );
        operationList.setPrefHeight(180);
        operationList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        operationList.setCellFactory(list -> operationListCellFactory.create(
            this::selectOperationForDrag,
            this::dropOperationAt,
            operations::size
        ));
        operationList.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() >= 0) {
                statusLabel.setText("Selected operation #" + newValue.intValue());
                renderSelectedOperation(newValue.intValue());
                synchronizeSelectedOperationIndices();
                updateInspectionFromSelection(newValue.intValue());
            }
        });
        textAreaConfigurator.readonly(
            inspectorArea,
            simulationArea,
            resourcesArea,
            preflightArea,
            compatibilityArea,
            transformationArea,
            javaDslArea,
            gateInfoArea,
            assistantNotesArea,
            targetProfileArea,
            fullIrSurfaceArea
        );
        textAreaConfigurator.wrap(
            overviewArea,
            inspectorArea,
            simulationArea,
            resourcesArea,
            preflightArea,
            compatibilityArea,
            transformationArea,
            diagnosticsArea,
            gateInfoArea,
            assistantNotesArea,
            targetProfileArea,
            fullIrSurfaceArea
        );
        textAreaConfigurator.noWrap(
            nativeJsonArea,
            generatedArea,
            codePreviewArea,
            externalResultArea,
            externalGeneratedArea,
            javaDslArea
        );
        diagnosticList.setPrefHeight(180);
        diagnosticList.setPlaceholder(new Label("No diagnostics. The active IR program is valid for the current checks."));
        diagnosticList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> navigateDiagnostic(newValue));
        programBadgeLabel.getStyleClass().add("status-chip");
        targetBadgeLabel.getStyleClass().add("status-chip");
        healthBadgeLabel.getStyleClass().add("status-chip");
        programBadgeLabel.setOnMouseClicked(event -> selectResultTab(uiText("tabOverview")));
        healthBadgeLabel.setOnMouseClicked(event -> selectResultTab(uiText("tabDiagnostics")));
        targetBadgeLabel.setOnMouseClicked(event -> selectResultTab(uiText("tabTargetProfile")));
        installHelp(programBadgeLabel, "helpProgramBadge");
        installHelp(healthBadgeLabel, "helpHealthBadge");
        installHelp(targetBadgeLabel, "helpTargetBadge");
        installHelp(targetFormatBox, "helpTargetFormat");
        installHelp(languageBox, "helpLanguage");
        installHelp(themeBox, "helpTheme");
        installHelp(commandPaletteBox, "helpCommand");
        installHelp(wikiSearchField, "helpWikiSearch");
        installHelp(renderHugeCircuitBox, "helpHugeRender");
        installHelp(autoSimulationBox, "helpAutoSimulation");
        installHelp(hideZeroProbabilityBox, "helpHideZero");
        installHelp(registerBitOrderBox, "helpRegisterBitOrder");
        canonicalizeParametersBox.setSelected(true);
        removeIdentityBox.setSelected(true);
        targetLoweringBox.setSelected(true);
        hideZeroProbabilityBox.setSelected(true);
        renderHugeCircuitBox.setSelected(false);
        showSimulationTextBox.setSelected(true);
        showProbabilitiesBox.setSelected(true);
        showStateVectorBox.setSelected(true);
        showQSphereBox.setSelected(true);
        showPhaseDisksBox.setSelected(true);
        showGeneratedCodeBox.setSelected(false);
        showSimulationTextBox.selectedProperty().addListener((observable, oldValue, newValue) -> refreshWorkspace());
        showProbabilitiesBox.selectedProperty().addListener((observable, oldValue, newValue) -> refreshWorkspace());
        showStateVectorBox.selectedProperty().addListener((observable, oldValue, newValue) -> refreshWorkspace());
        showQSphereBox.selectedProperty().addListener((observable, oldValue, newValue) -> refreshWorkspace());
        showPhaseDisksBox.selectedProperty().addListener((observable, oldValue, newValue) -> refreshWorkspace());
        showGeneratedCodeBox.selectedProperty().addListener((observable, oldValue, newValue) -> refreshWorkspace());
        renderHugeCircuitBox.selectedProperty().addListener((observable, oldValue, newValue) -> refreshWorkspace());
        autoSimulationBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            invalidateAutoSimulationPreview();
            refreshWorkspace();
        });
        hideZeroProbabilityBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            invalidateAutoSimulationPreview();
            refreshWorkspace();
        });
        registerBitOrderBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            invalidateAutoSimulationPreview();
            refreshWorkspace();
        });
        shotsField.textProperty().addListener((observable, oldValue, newValue) -> invalidateAutoSimulationPreview());
        seedField.textProperty().addListener((observable, oldValue, newValue) -> invalidateAutoSimulationPreview());
        inspectionSlider.setMin(-1.0);
        inspectionSlider.setMax(0.0);
        inspectionSlider.setValue(-1.0);
        inspectionSlider.setShowTickLabels(true);
        inspectionSlider.setShowTickMarks(true);
        inspectionSlider.setMajorTickUnit(1.0);
        inspectionSlider.setBlockIncrement(1.0);
        inspectionSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (updatingInspectionSlider) {
                return;
            }
            setInspectionStep((int) Math.round(newValue.doubleValue()));
        });
        selectionRectangle.setFill(Color.rgb(
            37,
            99,
            235,
            0.14
        ));
        selectionRectangle.setStroke(Color.rgb(
            37,
            99,
            235,
            0.82
        ));
        selectionRectangle.setVisible(false);
        selectionRectangle.setMouseTransparent(true);
        hideZeroProbabilityBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (autoSimulationBox.isSelected()) {
                refreshWorkspace();
            }
        });
        refreshReferenceBoxes();
        refreshControlTexts();
        renderGateInfo();
        refreshGateCatalog();
        simulationVisualizationsView.setRussian(russianLanguage());
        phaseDiskView.setRussian(russianLanguage());
        qSphereView.setRussian(russianLanguage());
        applyExperienceMode();
    }

    private Node header() {
        return headerView.build(
            uiText("title"),
            uiText("subtitle"),
            uiText("target"),
            uiText("validate"),
            uiText("simulate"),
            uiText("export"),
            viewMenu(),
            uiText("language"),
            languageBox,
            uiText("theme"),
            themeBox,
            programBadgeLabel,
            healthBadgeLabel,
            targetBadgeLabel,
            targetFormatBox,
            () -> runNative("Validate", this::validateNative),
            () -> runNative("Simulate", this::simulateNative),
            () -> runNative("Export", this::exportNative)
        );
    }

    private Node viewMenu() {
        final MenuButton menu = new MenuButton(uiText("view"));
        viewMenuButton = menu;
        menu.getStyleClass().add("view-menu-button");
        menu.getItems().addAll(
            menuItem(
                uiText("panelSimulation"),
                showSimulationTextBox
            ),
            menuItem(
                uiText("panelProbabilities"),
                showProbabilitiesBox
            ),
            menuItem(
                uiText("panelStateVector"),
                showStateVectorBox
            ),
            menuItem(
                uiText("panelQSphere"),
                showQSphereBox
            ),
            new SeparatorMenuItem(),
            menuItem(
                uiText("panelPhaseDisks"),
                showPhaseDisksBox
            ),
            menuItem(
                uiText("panelGeneratedCode"),
                showGeneratedCodeBox
            )
        );
        return menu;
    }

    private static CheckMenuItem menuItem(
        final String text,
        final CheckBox source
    ) {
        final CheckMenuItem item = new CheckMenuItem(text);
        item.selectedProperty().bindBidirectional(source.selectedProperty());
        return item;
    }

    private boolean russianLanguage() {
        return "Русский".equals(languageBox.getValue());
    }

    private String initialLanguage() {
        final String configured = getParameters().getNamed().getOrDefault(
            "language",
            "en"
        );
        return "ru".equalsIgnoreCase(configured)
            || "russian".equalsIgnoreCase(configured)
            || "русский".equalsIgnoreCase(configured)
                ? "Русский"
                : "English";
    }

    private String initialTheme() {
        final String configured = getParameters().getNamed().getOrDefault(
            "theme",
            "light"
        );
        return "dark".equalsIgnoreCase(configured)
            || "темная".equalsIgnoreCase(configured)
                ? "Dark"
                : "Light";
    }

    private String uiText(final String key) {
        if (!russianLanguage()) {
            return switch (key) {
                case "title" -> "Quantum IR Studio";
                case "subtitle" -> "Native model first: build IR, inspect flow, simulate, then export";
                case "target" -> "Export";
                case "validate" -> "Validate IR";
                case "simulate" -> "Simulate";
                case "export" -> "Export";
                case "language" -> "Language";
                case "theme" -> "Theme";
                case "view" -> "View";
                case "panelSimulation" -> "Simulation text";
                case "panelProbabilities" -> "Probabilities";
                case "panelStateVector" -> "Statevector";
                case "panelQSphere" -> "Q-sphere";
                case "panelPhaseDisks" -> "Phase disks";
                case "panelGeneratedCode" -> "Export preview";
                case "generatedCodeTitle" -> "Export preview";
                case "refreshCode" -> "Refresh export";
                case "phaseDisksHidden" -> "Phase disks are hidden from View.";
                case "inspectStart" -> "Start";
                case "inspectBack" -> "Back";
                case "inspectAll" -> "All";
                case "inspectNext" -> "Next";
                case "inspectEnd" -> "End";
                case "gateWikiOpened" -> "Gate wiki opened for";
                case "gateGroupSingle" -> "Single qubit";
                case "gateGroupPhase" -> "Phase and rotation";
                case "gateGroupControlled" -> "Controlled and multi-qubit";
                case "gateGroupNonUnitary" -> "Non-unitary";
                case "tabOverview" -> "Overview";
                case "tabInspector" -> "Inspector";
                case "tabSimulation" -> "Simulation";
                case "tabProbabilities" -> "Probabilities";
                case "tabStateVector" -> "Statevector";
                case "tabQSphere" -> "Q-Sphere";
                case "tabGateInfo" -> "Gate Info";
                case "tabAssistantNotes" -> "Assistant Notes";
                case "tabFullIrSurface" -> "Full IR Surface";
                case "tabDiagnosticList" -> "Diagnostic List";
                case "tabTargetProfile" -> "Export Profile";
                case "tabResources" -> "Resources";
                case "tabPreflight" -> "Preflight";
                case "tabCompatibility" -> "Compatibility";
                case "tabTransform" -> "Transform";
                case "tabNativeJson" -> "Native JSON";
                case "tabDiagnostics" -> "Diagnostics";
                case "tabGeneratedExport" -> "Generated Export";
                case "nativeTab" -> "Native IR Studio";
                case "externalTab" -> "External Formats";
                case "settingsTab" -> "Execution Settings";
                case "circuitTitle" -> "Native IR Circuit Flow";
                case "circuitHint" -> "Operations are built into ru.pathcreator.vadim.quantum domain objects through the Java DSL.";
                case "program" -> "Program";
                case "circuit" -> "Circuit";
                case "qRegister" -> "Q register";
                case "cRegister" -> "C register";
                case "wireOrder" -> "Wire order";
                case "layout" -> "Layout";
                case "mode" -> "Mode";
                case "operation" -> "Operation";
                case "template" -> "Template";
                case "load" -> "Load";
                case "findGate" -> "Find gate";
                case "clearSearch" -> "Clear";
                case "gateSearchEmpty" -> "No gates match this search. Clear the field or try H, RX, CX, MEASURE.";
                case "gate" -> "Gate";
                case "qubitA" -> "Qubit A";
                case "qubitB" -> "Qubit B";
                case "qubitC" -> "Qubit C";
                case "classical" -> "Classical";
                case "angle" -> "Angle";
                case "anglePhi" -> "Phi";
                case "angleLambda" -> "Lambda";
                case "duration" -> "Duration";
                case "labelName" -> "Label";
                case "nativeActions" -> "Native Actions";
                case "command" -> "Command";
                case "wikiSearch" -> "Wiki search";
                case "operationStream" -> "Operation Stream";
                case "addOperation" -> "Add";
                case "updateOperation" -> "Update";
                case "insertBefore" -> "Before";
                case "insertAfter" -> "After";
                case "duplicateOperation" -> "Duplicate";
                case "groupOperation" -> "Group";
                case "removeOperation" -> "Remove";
                case "moveLeft" -> "Left";
                case "moveRight" -> "Right";
                case "resetProgram" -> "Reset";
                case "timeline" -> "Timeline";
                case "inspect" -> "Inspect";
                case "resources" -> "Resources";
                case "compatibility" -> "Compatibility";
                case "transform" -> "Transform";
                case "runCommand" -> "▶";
                case "saveDsl" -> "Save DSL";
                case "openDsl" -> "Open DSL";
                case "saveJson" -> "Save JSON";
                case "openJson" -> "Open JSON";
                case "applyJson" -> "Apply JSON";
                case "beginnerHint" -> "Beginner mode: build, inspect, simulate and export without advanced pipeline noise.";
                case "expertHint" -> "Expert mode: full IR pipeline is visible.";
                case "renderHugeCircuit" -> "Render full huge circuit preview";
                case "fast" -> "Fast";
                case "skipValidation" -> "Skip validation";
                case "skipInspection" -> "Skip inspection";
                case "skipPreflight" -> "Skip preflight";
                case "skipTransformation" -> "Skip transformation";
                case "skipSimulation" -> "Skip simulation";
                case "skipCompiler" -> "Skip compiler";
                case "skipBackend" -> "Skip backend";
                case "canonicalizeParameters" -> "Canonicalize parameters";
                case "removeIdentity" -> "Remove identity gates";
                case "inlineComposite" -> "Inline composite gates";
                case "targetLowering" -> "Target-aware lowering";
                case "autoSimulation" -> "Auto simulate preview";
                case "hideZeroProbability" -> "Hide zero probabilities";
                case "registerBitOrder" -> "Register-order bitstrings";
                case "inspectFull" -> "Inspect: full circuit";
                case "inspectStep" -> "Inspect: step";
                case "operationFull" -> "Operation: full circuit";
                case "operationPrefix" -> "Operation:";
                case "settingsShots" -> "Shots";
                case "settingsSeed" -> "Seed";
                case "settingsExecution" -> "Execution";
                case "settingsLiveUi" -> "Live UI";
                case "settingsPanels" -> "Panels";
                case "settingsTransform" -> "Transform";
                case "wikiSearchPrompt" -> "Search gates, buttons, panels, export, simulation";
                case "wikiNoMatches" -> "No wiki matches. Try: simulate, export, delete, gate, diagnostics, theme.";
                case "helpProgramBadge" -> "Program badge opens the overview with source, circuit size, resources and target summary.";
                case "helpHealthBadge" -> "Health badge opens diagnostics. Validate IR writes validation and preflight messages there.";
                case "helpTargetBadge" -> "Target badge opens export profile/preflight information for the selected target format.";
                case "helpTargetFormat" -> "Selects the export target used by preflight, export preview and compatibility checks.";
                case "helpLanguage" -> "Switches desktop labels, wiki hints and visual panel text between English and Russian.";
                case "helpTheme" -> "Switches light/dark visual theme. Dropdowns, menus and tabs are themed with the same palette.";
                case "helpCommand" -> "Runs one of the native IR workflows from the left panel without using the top buttons.";
                case "helpWikiSearch" -> "Searches the local wiki for gates, panels, workflows and editing shortcuts.";
                case "helpHugeRender" -> "Keeps very large programs responsive by showing a summary unless full rendering is explicitly enabled.";
                case "helpAutoSimulation" -> "Runs a preview simulation after workspace changes when the program is small enough.";
                case "helpHideZero" -> "Hides zero-probability rows in probability and statevector visualizations.";
                case "helpRegisterBitOrder" -> "Displays bitstrings as c[0]...c[n-1]. Default stays standard MSB-first c[n-1]...c[0], matching Qiskit/Aer.";
                default -> key;
            };
        }
        return switch (key) {
            case "title" -> "Quantum IR Studio";
            case "subtitle" -> "Сначала родная модель: строим IR, проверяем поток, симулируем и экспортируем";
            case "target" -> "Экспорт";
            case "validate" -> "Проверить IR";
            case "simulate" -> "Симуляция";
            case "export" -> "Экспорт";
            case "language" -> "Язык";
            case "theme" -> "Тема";
            case "view" -> "Вид";
            case "panelSimulation" -> "Текст симуляции";
            case "panelProbabilities" -> "Вероятности";
                case "panelStateVector" -> "Вектор состояния";
                case "panelQSphere" -> "Q-сфера";
                case "panelPhaseDisks" -> "Фазовые диски";
            case "panelGeneratedCode" -> "Предпросмотр экспорта";
            case "generatedCodeTitle" -> "Предпросмотр экспорта";
            case "refreshCode" -> "Обновить экспорт";
            case "phaseDisksHidden" -> "Фазовые диски скрыты в меню Вид.";
            case "inspectStart" -> "Старт";
            case "inspectBack" -> "Назад";
            case "inspectAll" -> "Всё";
            case "inspectNext" -> "Дальше";
            case "inspectEnd" -> "Конец";
            case "gateWikiOpened" -> "Вики открыта для";
            case "gateGroupSingle" -> "Один кубит";
            case "gateGroupPhase" -> "Фаза и повороты";
            case "gateGroupControlled" -> "Управляемые и multi-qubit";
            case "gateGroupNonUnitary" -> "Неунитарные";
            case "tabOverview" -> "Обзор";
            case "tabInspector" -> "Инспектор";
            case "tabSimulation" -> "Симуляция";
            case "tabProbabilities" -> "Вероятности";
            case "tabStateVector" -> "Statevector";
            case "tabQSphere" -> "Q-Sphere";
            case "tabGateInfo" -> "Gate Wiki";
            case "tabAssistantNotes" -> "Заметки";
            case "tabFullIrSurface" -> "Поверхность IR";
            case "tabDiagnosticList" -> "Список диагностик";
            case "tabTargetProfile" -> "Профиль экспорта";
            case "tabResources" -> "Ресурсы";
            case "tabPreflight" -> "Preflight";
            case "tabCompatibility" -> "Совместимость";
            case "tabTransform" -> "Трансформации";
            case "tabNativeJson" -> "Родной JSON";
            case "tabDiagnostics" -> "Диагностика";
            case "tabGeneratedExport" -> "Сгенерированный экспорт";
            case "nativeTab" -> "Родная IR-студия";
            case "externalTab" -> "Внешние форматы";
            case "settingsTab" -> "Настройки запуска";
            case "circuitTitle" -> "Поток схемы Quantum IR";
            case "circuitHint" -> "Операции строятся как объекты ru.pathcreator.vadim.quantum через Java DSL.";
            case "program" -> "Программа";
            case "circuit" -> "Схема";
            case "qRegister" -> "Q-регистр";
            case "cRegister" -> "C-регистр";
            case "wireOrder" -> "Порядок линий";
            case "layout" -> "Раскладка";
            case "mode" -> "Режим";
            case "operation" -> "Операция";
            case "findGate" -> "Найти gate";
            case "clearSearch" -> "Очистить";
            case "gateSearchEmpty" -> "Gate не найдены. Очистите поиск или попробуйте H, RX, CX, MEASURE.";
            case "gate" -> "Gate";
            case "qubitA" -> "Qubit A";
            case "qubitB" -> "Qubit B";
            case "qubitC" -> "Qubit C";
            case "classical" -> "Classical";
            case "angle" -> "Угол";
            case "anglePhi" -> "Phi";
            case "angleLambda" -> "Lambda";
            case "duration" -> "Длительность";
            case "labelName" -> "Label";
            case "nativeActions" -> "Действия IR";
            case "command" -> "Команда";
            case "wikiSearch" -> "Поиск вики";
            case "operationStream" -> "Поток операций";
            case "addOperation" -> "Добавить";
            case "updateOperation" -> "Обновить";
            case "insertBefore" -> "До";
            case "insertAfter" -> "После";
            case "duplicateOperation" -> "Дубль";
            case "groupOperation" -> "Группа";
            case "removeOperation" -> "Удалить";
            case "moveLeft" -> "Влево";
            case "moveRight" -> "Вправо";
            case "resetProgram" -> "Сброс";
            case "timeline" -> "Таймлайн";
            case "inspect" -> "Инспекция";
            case "resources" -> "Ресурсы";
            case "compatibility" -> "Совместимость";
            case "transform" -> "Трансформации";
            case "runCommand" -> "▶";
            case "saveDsl" -> "Сохранить DSL";
            case "openDsl" -> "Открыть DSL";
            case "saveJson" -> "Сохранить JSON";
            case "openJson" -> "Открыть JSON";
            case "applyJson" -> "Применить JSON";
            case "beginnerHint" -> "Режим новичка: строим, инспектируем, симулируем и экспортируем без лишнего pipeline-шума.";
            case "expertHint" -> "Экспертный режим: виден полный IR pipeline.";
            case "renderHugeCircuit" -> "Рендерить огромную схему полностью";
            case "fast" -> "Быстро";
            case "skipValidation" -> "Пропустить валидацию";
            case "skipInspection" -> "Пропустить инспекцию";
            case "skipPreflight" -> "Пропустить preflight";
            case "skipTransformation" -> "Пропустить трансформации";
            case "skipSimulation" -> "Пропустить симуляцию";
            case "skipCompiler" -> "Пропустить компиляцию";
            case "skipBackend" -> "Пропустить backend";
            case "canonicalizeParameters" -> "Канонизировать параметры";
            case "removeIdentity" -> "Удалять identity gates";
            case "inlineComposite" -> "Встраивать composite gates";
            case "targetLowering" -> "Lowering под экспорт";
            case "autoSimulation" -> "Автосимуляция preview";
            case "hideZeroProbability" -> "Скрывать нулевые вероятности";
            case "registerBitOrder" -> "Bitstring в порядке регистров";
            case "inspectFull" -> "Инспекция: вся схема";
            case "inspectStep" -> "Инспекция: шаг";
            case "operationFull" -> "Операция: вся схема";
            case "operationPrefix" -> "Операция:";
            case "settingsShots" -> "Shots";
            case "settingsSeed" -> "Seed";
            case "settingsExecution" -> "Выполнение";
            case "settingsLiveUi" -> "Live UI";
            case "settingsPanels" -> "Панели";
            case "settingsTransform" -> "Трансформации";
            case "wikiSearchPrompt" -> "Поиск: gates, кнопки, панели, экспорт, симуляция";
            case "wikiNoMatches" -> "Ничего не найдено. Попробуйте: симуляция, экспорт, удалить, gate, диагностика.";
            case "helpProgramBadge" -> "Бейдж Program открывает обзор: источник, размер схемы, ресурсы и target.";
            case "helpHealthBadge" -> "Бейдж Health открывает диагностику; Проверить IR пишет сюда результат.";
            case "helpTargetBadge" -> "Бейдж Target показывает профиль экспорта и preflight для выбранного формата.";
            case "helpTargetFormat" -> "Целевой формат для preflight, preview экспорта и совместимости.";
            case "helpLanguage" -> "Переключает язык надписей, wiki-подсказок и визуальных панелей.";
            case "helpTheme" -> "Переключает светлую или темную тему вместе с dropdown, menu и tabs.";
            case "helpCommand" -> "Запускает workflow родного IR из левой панели.";
            case "helpWikiSearch" -> "Ищет по локальной wiki: gates, панели, workflow и горячие действия.";
            case "helpHugeRender" -> "Для очень больших схем показывает сводку, а полный рендер включается явно.";
            case "helpAutoSimulation" -> "Запускает preview-симуляцию после изменений, если схема достаточно мала.";
            case "helpHideZero" -> "Скрывает строки с нулевой вероятностью в probability/statevector.";
            case "helpRegisterBitOrder" -> "Показывает bitstring как c[0]...c[n-1]. По умолчанию используется стандартный MSB-first порядок c[n-1]...c[0], как в Qiskit/Aer.";
            default -> key;
        };
    }

    private void applyLanguage() {
        simulationVisualizationsView.setRussian(russianLanguage());
        phaseDiskView.setRussian(russianLanguage());
        qSphereView.setRussian(russianLanguage());
        simulationVisualizationsView.renderEmpty();
        phaseDiskView.renderEmpty();
        qSphereView.renderEmpty();
        refreshControlTexts();
        refreshGateCatalog();
        renderGateInfo();
        if (rootPane != null) {
            rootPane.setTop(header());
            rootPane.setCenter(mainTabs(primaryStage));
        }
        if (russianLanguage()) {
            statusLabel.setText("Язык интерфейса переключен на русский.");
        } else {
            statusLabel.setText("Interface language switched to English.");
        }
    }

    private void refreshControlTexts() {
        renderHugeCircuitBox.setText(uiText("renderHugeCircuit"));
        fastBox.setText(uiText("fast"));
        skipValidationBox.setText(uiText("skipValidation"));
        skipInspectionBox.setText(uiText("skipInspection"));
        skipPreflightBox.setText(uiText("skipPreflight"));
        skipTransformationBox.setText(uiText("skipTransformation"));
        skipSimulationBox.setText(uiText("skipSimulation"));
        skipCompilerBox.setText(uiText("skipCompiler"));
        skipBackendBox.setText(uiText("skipBackend"));
        canonicalizeParametersBox.setText(uiText("canonicalizeParameters"));
        removeIdentityBox.setText(uiText("removeIdentity"));
        inlineCompositeBox.setText(uiText("inlineComposite"));
        targetLoweringBox.setText(uiText("targetLowering"));
        autoSimulationBox.setText(uiText("autoSimulation"));
        hideZeroProbabilityBox.setText(uiText("hideZeroProbability"));
        registerBitOrderBox.setText(uiText("registerBitOrder"));
        showSimulationTextBox.setText(uiText("panelSimulation"));
        showProbabilitiesBox.setText(uiText("panelProbabilities"));
        showStateVectorBox.setText(uiText("panelStateVector"));
        showQSphereBox.setText(uiText("panelQSphere"));
        showPhaseDisksBox.setText(uiText("panelPhaseDisks"));
        showGeneratedCodeBox.setText(uiText("panelGeneratedCode"));
        gateSearchField.setPromptText(uiText("findGate"));
        wikiSearchField.setPromptText(uiText("wikiSearchPrompt"));
    }

    private void applyTheme() {
        if (rootPane == null) {
            return;
        }
        rootPane.getStyleClass().removeAll(
            "theme-light",
            "theme-dark"
        );
        final String themeClass = "Dark".equals(themeBox.getValue())
            ? "theme-dark"
            : "theme-light";
        rootPane.getStyleClass().add(themeClass);
        statusLabel.setText("Dark".equals(themeBox.getValue())
            ? "Theme: dark"
            : "Theme: light");
    }

    private TabPane mainTabs(final Stage stage) {
        return mainTabsView.build(
            uiText("nativeTab"),
            uiText("externalTab"),
            uiText("settingsTab"),
            nativeWorkspace(stage),
            externalWorkspace(stage),
            executionSettings()
        );
    }

    private Node nativeWorkspace(final Stage stage) {
        return nativeWorkspaceView.build(
            palette(stage),
            circuitCanvas(),
            resultTabs(),
            generatedCodePanel(),
            showGeneratedCodeBox.isSelected()
        );
    }

    private Node generatedCodePanel() {
        final Label title = new Label(uiText("generatedCodeTitle") + " / " + targetFormatBox.getValue().name());
        title.getStyleClass().add("chart-title");
        final Button refreshButton = secondaryButton(
            uiText("refreshCode"),
            () -> runNative("Export", this::exportNative)
        );
        final HBox header = new HBox(
            10.0,
            title,
            refreshButton
        );
        header.getStyleClass().add("code-panel-header");
        final VBox panel = new VBox(
            8.0,
            header,
            codePreviewArea
        );
        panel.getStyleClass().add("code-panel");
        VBox.setVgrow(
            codePreviewArea,
            javafx.scene.layout.Priority.ALWAYS
        );
        return panel;
    }

    private Node palette(final Stage stage) {
        final VBox registers = section(
            uiText("program"),
            fieldRow(
                uiText("circuit"),
                circuitNameField
            ),
            fieldRow(
                uiText("qRegister"),
                qregNameField,
                qregSizeField
            ),
            fieldRow(
                uiText("cRegister"),
                cregNameField,
                cregSizeField
            ),
            fieldRow(
                uiText("wireOrder"),
                wireOrderBox
            ),
            fieldRow(
                uiText("layout"),
                layoutModeBox
            ),
            fieldRow(
                uiText("mode"),
                experienceModeBox
            ),
            renderHugeCircuitBox
        );
        qregSizeField.textProperty().addListener((observable, oldValue, newValue) -> refreshWorkspaceAfterRegisterChange());
        qregNameField.textProperty().addListener((observable, oldValue, newValue) -> refreshWorkspaceAfterRegisterChange());
        cregSizeField.textProperty().addListener((observable, oldValue, newValue) -> refreshWorkspaceAfterRegisterChange());
        cregNameField.textProperty().addListener((observable, oldValue, newValue) -> refreshWorkspaceAfterRegisterChange());

        final Button addButton = primaryButton(
            uiText("addOperation"),
            this::addOperation
        );
        final Button updateButton = secondaryButton(
            uiText("updateOperation"),
            this::updateSelectedOperation
        );
        final Button insertBeforeButton = secondaryButton(
            uiText("insertBefore"),
            () -> insertOperationAtSelection(0)
        );
        final Button insertAfterButton = secondaryButton(
            uiText("insertAfter"),
            () -> insertOperationAtSelection(1)
        );
        final Button duplicateButton = secondaryButton(
            uiText("duplicateOperation"),
            this::duplicateSelectedOperation
        );
        final Button groupButton = secondaryButton(
            uiText("groupOperation"),
            this::groupSelectedOperations
        );
        final Button removeButton = secondaryButton(
            uiText("removeOperation"),
            this::removeSelectedOperation
        );
        final Button moveLeftButton = secondaryButton(
            uiText("moveLeft"),
            () -> moveSelectedOperation(-1)
        );
        final Button moveRightButton = secondaryButton(
            uiText("moveRight"),
            () -> moveSelectedOperation(1)
        );
        markExpert(
            insertBeforeButton,
            insertAfterButton,
            duplicateButton,
            groupButton,
            moveLeftButton,
            moveRightButton
        );
        final Button resetButton = secondaryButton(
            uiText("resetProgram"),
            () -> {
                resetToBlankWorkspace();
                refreshWorkspace();
            }
        );
        final Button clearGateSearchButton = secondaryButton(
            uiText("clearSearch"),
            gateSearchField::clear
        );
        clearGateSearchButton.setMinWidth(34.0);
        final VBox operationEditor = section(
            uiText("operation"),
            fieldRow(
                uiText("findGate"),
                gateSearchField,
                clearGateSearchButton
            ),
            gateCatalog(),
            new Separator(),
            fieldRow(
                uiText("gate"),
                gateBox
            ),
            fieldRow(
                uiText("qubitA"),
                primaryQubitBox
            ),
            fieldRow(
                uiText("qubitB"),
                secondaryQubitBox
            ),
            fieldRow(
                uiText("qubitC"),
                tertiaryQubitBox
            ),
            fieldRow(
                uiText("classical"),
                classicalBitBox
            ),
            fieldRow(
                uiText("angle"),
                angleField
            ),
            fieldRow(
                uiText("anglePhi"),
                secondAngleField
            ),
            fieldRow(
                uiText("angleLambda"),
                thirdAngleField
            ),
            fieldRow(
                uiText("duration"),
                durationField,
                durationUnitBox
            ),
            fieldRow(
                uiText("labelName"),
                labelNameField
            ),
            actionFlow(
                addButton,
                updateButton,
                insertBeforeButton,
                insertAfterButton,
                duplicateButton,
                groupButton,
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
            uiText("timeline"),
            () -> runNative("Timeline", this::timelineNative)
        );
        final Button inspectButton = primaryButton(
            uiText("inspect"),
            () -> runNative("Inspect", this::inspectNative)
        );
        final Button resourcesButton = primaryButton(
            uiText("resources"),
            () -> runNative("Resources", this::resourcesNative)
        );
        final Button preflightButton = primaryButton(
            "Preflight",
            () -> runNative("Preflight", this::preflightNative)
        );
        final Button compatibilityButton = primaryButton(
            uiText("compatibility"),
            () -> runNative("Compatibility", this::compatibilityNative)
        );
        final Button transformButton = primaryButton(
            uiText("transform"),
            () -> runNative("Transform", this::transformNative)
        );
        final Button javaDslButton = primaryButton(
            "Java DSL",
            () -> runNative("Java DSL", this::javaDslNative)
        );
        final Button runCommandButton = primaryButton(
            uiText("runCommand"),
            this::runCommandPalette
        );
        runCommandButton.setMinWidth(48.0);
        final Button saveDslButton = secondaryButton(
            uiText("saveDsl"),
            () -> saveJavaDsl(stage)
        );
        final Button openDslButton = secondaryButton(
            uiText("openDsl"),
            () -> openJavaDsl(stage)
        );
        final Button saveJsonButton = secondaryButton(
            uiText("saveJson"),
            () -> saveNativeJson(stage)
        );
        final Button openJsonButton = secondaryButton(
            uiText("openJson"),
            () -> openNativeJson(stage)
        );
        final Button applyJsonButton = secondaryButton(
            uiText("applyJson"),
            this::applyNativeJson
        );
        final Button clearWikiSearchButton = secondaryButton(
            uiText("clearSearch"),
            wikiSearchField::clear
        );
        clearWikiSearchButton.setMinWidth(34.0);
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
            uiText("nativeActions"),
            builderHintLabel,
            fieldRow(
                uiText("wikiSearch"),
                wikiSearchField,
                clearWikiSearchButton
            ),
            fieldRow(
                uiText("command"),
                commandPaletteBox,
                runCommandButton
            ),
            actionFlow(
                inspectButton,
                javaDslButton,
                saveDslButton,
                openDslButton,
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
            actions,
            operationEditor,
            section(
                uiText("operationStream"),
                operationList
            )
        );
        palette.setPadding(new Insets(14));
        return palette;
    }

    private Node circuitCanvas() {
        final DesktopCircuitWorkspaceResult result = circuitWorkspaceView.build(
            uiText("circuitTitle"),
            uiText("circuitHint"),
            circuitRows,
            inspectorControls(),
            phaseDiskNode(),
            selectionRectangle,
            this::installSelectionRectangle
        );
        circuitScrollPane = result.scrollPane();
        return result.node();
    }

    private Node phaseDiskNode() {
        if (showPhaseDisksBox.isSelected()) {
            return phaseDiskView.node();
        }
        final Label hidden = new Label(uiText("phaseDisksHidden"));
        hidden.getStyleClass().add("visualization-limit-label");
        return hidden;
    }

    private Node inspectorControls() {
        return inspectionControlsView.build(
            inspectionStepLabel,
            inspectionOperationLabel,
            inspectionSlider,
            uiText("inspectStart"),
            uiText("inspectBack"),
            uiText("inspectAll"),
            uiText("inspectNext"),
            uiText("inspectEnd"),
            () -> setInspectionStep(0),
            () -> setInspectionStep(inspectionStepIndex <= 0 ? 0 : inspectionStepIndex - 1),
            () -> setInspectionStep(-1),
            () -> setInspectionStep(nextInspectionStep()),
            () -> setInspectionStep(operations.isEmpty() ? -1 : operations.size() - 1)
        );
    }

    private void installSelectionRectangle(final StackPane canvasStack) {
        selectionRectangleController.install(
            canvasStack,
            selectionRectangle,
            this::selectOperationsIntersectingRectangle
        );
    }

    private void selectOperationsIntersectingRectangle(final Bounds selectionBounds) {
        selectedOperationIndices.clear();
        selectedOperationIndices.addAll(intersectingOperationIndices(
            circuitRows,
            selectionBounds
        ));
        restoreOperationSelection();
        statusLabel.setText("Selected " + selectedOperationIndices.size() + " operation(s) with rectangle.");
    }

    private Node gateCatalog() {
        return gateCatalogPane;
    }

    private void refreshGateCatalog() {
        gateCatalogView.refresh(
            gateCatalogPane,
            gateSearchField.getText(),
            gateBox.getValue(),
            gate -> {
                gateBox.setValue(gate);
                renderGateInfo();
            },
            gate -> {
                gateBox.setValue(gate);
                renderGateInfo();
                statusLabel.setText(uiText("gateWikiOpened") + " " + gate + ".");
            },
            this::uiText
        );
    }

    private Node resultTabs() {
        expertOnlyTabs.clear();
        final DesktopResultTabsResult result = resultTabsView.build(
            overviewArea,
            inspectorArea,
            simulationArea,
            simulationVisualizationsView.probabilityNode(),
            simulationVisualizationsView.stateVectorNode(),
            qSphereView.node(),
            javaDslArea,
            gateInfoArea,
            assistantNotesArea,
            fullIrSurfaceArea,
            diagnosticList,
            targetProfileArea,
            resourcesArea,
            preflightArea,
            compatibilityArea,
            transformationArea,
            nativeJsonArea,
            diagnosticsArea,
            generatedArea,
            showSimulationTextBox.isSelected(),
            showProbabilitiesBox.isSelected(),
            showStateVectorBox.isSelected(),
            showQSphereBox.isSelected(),
            this::uiText
        );
        resultTabPane = result.tabs();
        expertOnlyTabs.addAll(result.expertTabs());
        applyExperienceMode();
        return result.tabs();
    }

    private void selectResultTab(final String title) {
        if (resultTabPane == null) {
            return;
        }
        for (final Tab tab : resultTabPane.getTabs()) {
            if (title.equals(tab.getText())) {
                resultTabPane.getSelectionModel().select(tab);
                return;
            }
        }
    }

    private Node externalWorkspace(final Stage stage) {
        return externalWorkspaceView.build(
            externalSourceArea,
            externalInputFormatBox,
            externalTargetFormatBox,
            externalResultArea,
            externalGeneratedArea,
            () -> openExternalFile(stage),
            () -> runExternal(() -> externalService.json(
                externalInputFormatBox.getValue(),
                externalSourceArea.getText()
            )),
            () -> runExternal(() -> externalService.compile(
                externalInputFormatBox.getValue(),
                externalSourceArea.getText(),
                externalTargetFormatBox.getValue(),
                executionOptions()
            ))
        );
    }

    private Node executionSettings() {
        return executionSettingsView.build(
            shotsField,
            seedField,
            fastBox,
            skipValidationBox,
            skipInspectionBox,
            skipPreflightBox,
            skipTransformationBox,
            skipSimulationBox,
            skipCompilerBox,
            skipBackendBox,
            autoSimulationBox,
            hideZeroProbabilityBox,
            registerBitOrderBox,
            canonicalizeParametersBox,
            removeIdentityBox,
            inlineCompositeBox,
            targetLoweringBox,
            showSimulationTextBox,
            showProbabilitiesBox,
            showStateVectorBox,
            showQSphereBox,
            showPhaseDisksBox,
            showGeneratedCodeBox,
            this::uiText
        );
    }

    private Node footer() {
        final HBox footer = new HBox(statusLabel);
        footer.getStyleClass().add("status-bar");
        return footer;
    }

    private void resetToBlankWorkspace() {
        clearActiveJsonProgram();
        inspectionStepIndex = -1;
        circuitNameField.setText("main");
        qregNameField.setText("q");
        qregSizeField.setText("3");
        cregNameField.setText("c");
        cregSizeField.setText("3");
        operations.clear();
        selectedOperationIndices.clear();
        undoStack.clear();
        refreshReferenceBoxes();
        operationList.getSelectionModel().clearSelection();
        synchronizeEditorDefaults();
        updateOperationEditorAvailability();
    }

    private void refreshWorkspaceAfterRegisterChange() {
        refreshReferenceBoxes();
        invalidateAutoSimulationPreview();
        refreshWorkspace();
    }

    private void loadAuditFixtureWithoutRefresh(final String fixtureName) {
        clearActiveJsonProgram();
        inspectionStepIndex = -1;
        final String fixture = normalizedFixtureName(fixtureName);
        circuitNameField.setText(fixture.replace(
            '-',
            '_'
        ));
        qregNameField.setText("q");
        cregNameField.setText("c");
        operations.clear();
        switch (fixture) {
            case "dense-spectrum" -> {
                qregSizeField.setText("5");
                cregSizeField.setText("5");
                addDenseSpectrumFixture();
            }
            case "qft16" -> {
                qregSizeField.setText("16");
                cregSizeField.setText("16");
                addQft16Fixture();
            }
            case "chemistry16" -> {
                qregSizeField.setText("16");
                cregSizeField.setText("16");
                addChemistry16Fixture();
            }
            case "grover16" -> {
                qregSizeField.setText("16");
                cregSizeField.setText("16");
                addGrover16Fixture();
            }
            case "bell" -> {
                qregSizeField.setText("2");
                cregSizeField.setText("2");
                addBellFixture();
            }
            default -> {
                qregSizeField.setText("3");
                cregSizeField.setText("3");
            }
        }
        refreshReferenceBoxes();
        operationList.getSelectionModel().clearSelection();
        synchronizeEditorWithFirstOperation();
    }

    private static String normalizedFixtureName(final String fixtureName) {
        if (fixtureName == null) {
            return "blank";
        }
        final String normalized = fixtureName.trim().toLowerCase();
        if (
            normalized.startsWith("dense")
            || normalized.contains("spectrum")
        ) {
            return "dense-spectrum";
        }
        if (
            normalized.startsWith("qft")
            || normalized.contains("qft")
        ) {
            return "qft16";
        }
        if (
            normalized.startsWith("chem")
            || normalized.contains("ansatz")
        ) {
            return "chemistry16";
        }
        if (
            normalized.startsWith("grover")
            || normalized.contains("oracle")
        ) {
            return "grover16";
        }
        if (normalized.startsWith("bell")) {
            return "bell";
        }
        return "blank";
    }

    private void addBellFixture() {
        operations.add(operationSpec(
            "H",
            "q[0]"
        ));
        operations.add(operationSpec(
            "CX",
            "q[0]",
            "q[1]"
        ));
        operations.add(measureSpec(
            "q[0]",
            "c[0]"
        ));
        operations.add(measureSpec(
            "q[1]",
            "c[1]"
        ));
    }

    private void addDenseSpectrumFixture() {
        for (int round = 0; round < 4; round++) {
            operations.add(operationSpec("H", "q[0]"));
            operations.add(operationSpec("X", "q[1]"));
            operations.add(operationSpec("Y", "q[2]"));
            operations.add(operationSpec("Z", "q[3]"));
            operations.add(operationSpec("S", "q[4]"));
            operations.add(operationSpec("SDG", "q[4]"));
            operations.add(operationSpec("T", "q[0]"));
            operations.add(operationSpec("TDG", "q[0]"));
            operations.add(operationSpec("ID", "q[1]"));
            operations.add(new DesktopIrOperationSpec(
                "U",
                "q[2]",
                "q[0]",
                "q[0]",
                "c[0]",
                Math.PI / (round + 2.0),
                Math.PI / (round + 3.0),
                Math.PI / (round + 4.0),
                20.0,
                "NS",
                "entry"
            ));
            operations.add(rotationSpec("RX", "q[1]", Math.PI / (round + 2.0)));
            operations.add(rotationSpec("RY", "q[2]", Math.PI / (round + 3.0)));
            operations.add(rotationSpec("RZ", "q[3]", Math.PI / (round + 4.0)));
            operations.add(rotationSpec("PHASE", "q[4]", Math.PI / (round + 5.0)));
            operations.add(operationSpec("CX", "q[0]", "q[1]"));
            operations.add(operationSpec("CY", "q[1]", "q[2]"));
            operations.add(operationSpec("CZ", "q[2]", "q[3]"));
            operations.add(new DesktopIrOperationSpec(
                "CPHASE",
                "q[3]",
                "q[4]",
                "q[0]",
                "c[0]",
                Math.PI / (round + 6.0),
                0.0,
                0.0,
                20.0,
                "NS",
                "entry"
            ));
            operations.add(operationSpec("CH", "q[3]", "q[4]"));
            operations.add(operationSpec("SWAP", "q[0]", "q[4]"));
            operations.add(new DesktopIrOperationSpec("CCX", "q[0]", "q[2]", "q[4]", "c[0]", Math.PI / 2.0));
            operations.add(operationSpec("BARRIER", "q[1]", "q[3]"));
            operations.add(new DesktopIrOperationSpec(
                "DELAY",
                "q[1]",
                "q[3]",
                "q[0]",
                "c[0]",
                Math.PI / 2.0,
                0.0,
                0.0,
                10.0 + round,
                "NS",
                "entry"
            ));
        }
        operations.add(new DesktopIrOperationSpec(
            "LABEL",
            "q[0]",
            "q[0]",
            "q[0]",
            "c[0]",
            Math.PI / 2.0,
            0.0,
            0.0,
            20.0,
            "NS",
            "dense_entry"
        ));
        operations.add(new DesktopIrOperationSpec(
            "BRANCH",
            "q[0]",
            "q[0]",
            "q[0]",
            "c[0]",
            Math.PI / 2.0,
            0.0,
            0.0,
            20.0,
            "NS",
            "dense_entry"
        ));
        operations.add(new DesktopIrOperationSpec(
            "TIMING_BOX",
            "q[0]",
            "q[0]",
            "q[0]",
            "c[0]",
            Math.PI / 2.0,
            0.0,
            0.0,
            4.0,
            "US",
            "entry"
        ));
        operations.add(new DesktopIrOperationSpec(
            "ASSIGN",
            "q[0]",
            "q[0]",
            "q[0]",
            "c[0]",
            1.0,
            0.0,
            0.0,
            20.0,
            "NS",
            "assign"
        ));
        operations.add(new DesktopIrOperationSpec(
            "DECLARE",
            "q[0]",
            "q[0]",
            "q[0]",
            "c[0]",
            1.0,
            0.0,
            0.0,
            20.0,
            "NS",
            "dense_flag"
        ));
        operations.add(new DesktopIrOperationSpec(
            "ARRAY",
            "q[0]",
            "q[0]",
            "q[0]",
            "c[0]",
            2.0,
            0.0,
            0.0,
            20.0,
            "NS",
            "dense_array"
        ));
        operations.add(new DesktopIrOperationSpec(
            "CALL",
            "q[0]",
            "q[0]",
            "q[0]",
            "c[0]",
            0.0,
            0.0,
            0.0,
            20.0,
            "NS",
            "dense_external"
        ));
        operations.add(new DesktopIrOperationSpec(
            "IF_X",
            "q[0]",
            "q[0]",
            "q[0]",
            "c[0]",
            1.0,
            0.0,
            0.0,
            20.0,
            "NS",
            "if_x"
        ));
        operations.add(new DesktopIrOperationSpec(
            "CTRL_X",
            "q[1]",
            "q[0]",
            "q[0]",
            "c[0]",
            1.0,
            0.0,
            0.0,
            20.0,
            "NS",
            "ctrl_x"
        ));
        operations.add(operationSpec("BLOCK", "q[0]"));
        operations.add(new DesktopIrOperationSpec(
            "IF_BLOCK",
            "q[0]",
            "q[0]",
            "q[0]",
            "c[0]",
            1.0,
            0.0,
            0.0,
            20.0,
            "NS",
            "if_block"
        ));
        operations.add(new DesktopIrOperationSpec(
            "FOR",
            "q[0]",
            "q[0]",
            "q[0]",
            "c[0]",
            2.0,
            0.0,
            0.0,
            20.0,
            "NS",
            "i"
        ));
        operations.add(new DesktopIrOperationSpec(
            "SYM_FOR",
            "q[0]",
            "q[0]",
            "q[0]",
            "c[0]",
            3.0,
            0.0,
            0.0,
            20.0,
            "NS",
            "j"
        ));
        operations.add(operationSpec("WHILE", "q[0]"));
        operations.add(operationSpec("WAIT", "q[0]"));
        operations.add(operationSpec("HALT", "q[0]"));
        operations.add(operationSpec("RESET", "q[4]"));
        addMeasureAllFixture(5);
    }

    private void addQft16Fixture() {
        for (int target = 0; target < 16; target++) {
            operations.add(operationSpec("H", qubit(target)));
            final int maxControl = Math.min(15, target + 5);
            for (int control = target + 1; control <= maxControl; control++) {
                operations.add(operationSpec("CX", qubit(control), qubit(target)));
                operations.add(rotationSpec("RZ", qubit(target), Math.PI / (1 << (control - target))));
                operations.add(operationSpec("CX", qubit(control), qubit(target)));
            }
        }
        for (int i = 0; i < 8; i++) {
            operations.add(operationSpec("SWAP", qubit(i), qubit(15 - i)));
        }
        addMeasureAllFixture(16);
    }

    private void addChemistry16Fixture() {
        for (int layer = 0; layer < 4; layer++) {
            for (int qubit = 0; qubit < 16; qubit++) {
                operations.add(rotationSpec("RY", qubit(qubit), Math.PI / (layer + qubit + 3.0)));
                operations.add(rotationSpec("RZ", qubit(qubit), Math.PI / (layer + qubit + 5.0)));
            }
            for (int qubit = layer % 2; qubit < 15; qubit += 2) {
                operations.add(operationSpec("CX", qubit(qubit), qubit(qubit + 1)));
            }
            for (int qubit = (layer + 1) % 2; qubit < 15; qubit += 2) {
                operations.add(operationSpec("CZ", qubit(qubit), qubit(qubit + 1)));
            }
            operations.add(operationSpec("BARRIER", qubit(0), qubit(15)));
        }
        addMeasureAllFixture(16);
    }

    private void addGrover16Fixture() {
        for (int qubit = 0; qubit < 16; qubit++) {
            operations.add(operationSpec("H", qubit(qubit)));
        }
        for (int round = 0; round < 3; round++) {
            for (int qubit = 0; qubit < 16; qubit += 4) {
                operations.add(operationSpec("X", qubit(qubit)));
            }
            for (int qubit = 0; qubit < 14; qubit += 3) {
                operations.add(new DesktopIrOperationSpec("CCX", qubit(qubit), qubit(qubit + 1), qubit(qubit + 2), "c[0]", Math.PI / 2.0));
            }
            for (int qubit = 0; qubit < 15; qubit += 2) {
                operations.add(operationSpec("CZ", qubit(qubit), qubit(qubit + 1)));
            }
            for (int qubit = 0; qubit < 16; qubit++) {
                operations.add(operationSpec("H", qubit(qubit)));
                operations.add(operationSpec("X", qubit(qubit)));
            }
            for (int qubit = 0; qubit < 15; qubit += 2) {
                operations.add(operationSpec("CX", qubit(qubit), qubit(qubit + 1)));
            }
            for (int qubit = 0; qubit < 16; qubit++) {
                operations.add(operationSpec("X", qubit(qubit)));
                operations.add(operationSpec("H", qubit(qubit)));
            }
        }
        addMeasureAllFixture(16);
    }

    private void addMeasureAllFixture(final int qubitCount) {
        for (int qubit = 0; qubit < qubitCount; qubit++) {
            operations.add(measureSpec(
                qubit(qubit),
                "c[" + qubit + "]"
            ));
        }
    }

    private static String qubit(final int index) {
        return "q[" + index + "]";
    }

    private static DesktopIrOperationSpec operationSpec(
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

    private static DesktopIrOperationSpec operationSpec(
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

    private static DesktopIrOperationSpec rotationSpec(
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

    private static DesktopIrOperationSpec measureSpec(
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

    private void synchronizeEditorDefaults() {
        gateBox.setValue("H");
        primaryQubitBox.setValue("q[0]");
        secondaryQubitBox.setValue("q[1]");
        tertiaryQubitBox.setValue("q[2]");
        classicalBitBox.setValue("c[0]");
        angleField.setText(Double.toString(Math.PI / 2.0));
        secondAngleField.setText("0.0");
        thirdAngleField.setText("0.0");
        durationField.setText("20.0");
        durationUnitBox.setValue("NS");
        labelNameField.setText("entry");
    }

    private void synchronizeEditorWithFirstOperation() {
        if (operations.isEmpty()) {
            return;
        }
        renderSelectedOperation(0);
    }

    private void updateOperationEditorAvailability() {
        final String gate = gateBox.getValue();
        primaryQubitBox.setDisable(!usesPrimaryQubit(gate));
        secondaryQubitBox.setDisable(!usesSecondaryQubit(gate));
        tertiaryQubitBox.setDisable(!usesTertiaryQubit(gate));
        classicalBitBox.setDisable(!usesClassicalBit(gate));
        angleField.setDisable(!usesAngle(gate));
        secondAngleField.setDisable(!usesSecondAngle(gate));
        thirdAngleField.setDisable(!usesThirdAngle(gate));
        durationField.setDisable(!usesDuration(gate));
        durationUnitBox.setDisable(!usesDuration(gate));
        labelNameField.setDisable(!usesLabelName(gate));
    }

    private static boolean usesPrimaryQubit(final String gate) {
        if (gate == null) {
            return false;
        }
        return switch (gate) {
            case "LABEL", "BRANCH", "TIMING_BOX", "ASSIGN", "DECLARE", "ARRAY", "CALL", "BLOCK", "IF_BLOCK", "FOR", "SYM_FOR", "WHILE", "HALT", "WAIT" -> false;
            default -> true;
        };
    }

    private static boolean usesSecondaryQubit(final String gate) {
        if (gate == null) {
            return false;
        }
        return switch (gate) {
            case "CX", "CY", "CZ", "CPHASE", "CH", "SWAP", "CCX", "BARRIER", "DELAY" -> true;
            default -> false;
        };
    }

    private static boolean usesTertiaryQubit(final String gate) {
        return "CCX".equals(gate);
    }

    private static boolean usesClassicalBit(final String gate) {
        return "MEASURE".equals(gate)
            || "ASSIGN".equals(gate)
            || "IF_X".equals(gate)
            || "CTRL_X".equals(gate)
            || "IF_BLOCK".equals(gate)
            || "WHILE".equals(gate);
    }

    private static boolean usesAngle(final String gate) {
        if (gate == null) {
            return false;
        }
        return switch (gate) {
            case "RX", "RY", "RZ", "PHASE", "CPHASE", "U", "ASSIGN", "DECLARE", "ARRAY", "IF_X", "CTRL_X", "IF_BLOCK", "FOR", "SYM_FOR", "WHILE" -> true;
            default -> false;
        };
    }

    private static boolean usesSecondAngle(final String gate) {
        return "U".equals(gate);
    }

    private static boolean usesThirdAngle(final String gate) {
        return "U".equals(gate);
    }

    private static boolean usesDuration(final String gate) {
        return "DELAY".equals(gate)
            || "TIMING_BOX".equals(gate);
    }

    private static boolean usesLabelName(final String gate) {
        return "LABEL".equals(gate)
            || "BRANCH".equals(gate)
            || "DECLARE".equals(gate)
            || "ARRAY".equals(gate)
            || "CALL".equals(gate)
            || "FOR".equals(gate)
            || "SYM_FOR".equals(gate);
    }

    private void addOperation() {
        clearActiveJsonProgram();
        rememberOperations();
        operations.add(operationFromEditor());
        inspectionStepIndex = operations.size() - 1;
        selectSingleOperationIndex(inspectionStepIndex);
        invalidateAutoSimulationPreview();
        refreshWorkspace();
        operationList.getSelectionModel().select(inspectionStepIndex);
        statusLabel.setText("Added " + gateBox.getValue() + ". Use Ctrl+Z or Delete to edit.");
    }

    private void updateSelectedOperation() {
        clearActiveJsonProgram();
        final int selectedIndex = operationList.getSelectionModel().getSelectedIndex();
        if (
            selectedIndex < 0
            || selectedIndex >= operations.size()
        ) {
            statusLabel.setText("Select an operation before update.");
            return;
        }
        rememberOperations();
        operations.set(
            selectedIndex,
            operationFromEditor()
        );
        inspectionStepIndex = selectedIndex;
        selectSingleOperationIndex(selectedIndex);
        invalidateAutoSimulationPreview();
        refreshWorkspace();
        operationList.getSelectionModel().select(selectedIndex);
        statusLabel.setText("Updated operation #" + selectedIndex + ". Press Ctrl+Z to restore.");
    }

    private DesktopIrOperationSpec operationFromEditor() {
        return new DesktopIrOperationSpec(
            gateBox.getValue(),
            primaryQubitBox.getValue(),
            secondaryQubitBox.getValue(),
            tertiaryQubitBox.getValue(),
            classicalBitBox.getValue(),
            angle(),
            secondAngle(),
            thirdAngle(),
            durationValue(),
            durationUnitBox.getValue(),
            labelNameField.getText()
        );
    }

    private void insertOperationAtSelection(final int offset) {
        clearActiveJsonProgram();
        rememberOperations();
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
        final int normalizedInsertIndex = Math.max(
            0,
            Math.min(
                operations.size() - 1,
                insertIndex
            )
        );
        inspectionStepIndex = normalizedInsertIndex;
        selectSingleOperationIndex(normalizedInsertIndex);
        invalidateAutoSimulationPreview();
        refreshWorkspace();
        operationList.getSelectionModel().select(normalizedInsertIndex);
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
        rememberOperations();
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
            rememberOperations();
            operations.remove(selectedIndex);
        } else if (!operations.isEmpty()) {
            rememberOperations();
            operations.remove(operations.size() - 1);
        }
        refreshWorkspace();
        statusLabel.setText("Operation removed. Press Ctrl+Z to restore.");
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
        rememberOperations();
        final DesktopIrOperationSpec operation = operations.remove(selectedIndex);
        operations.add(
            targetIndex,
            operation
        );
        refreshWorkspace();
        operationList.getSelectionModel().select(targetIndex);
    }

    private void rememberOperations() {
        undoStack.add(List.copyOf(operations));
        if (undoStack.size() > 64) {
            undoStack.remove(0);
        }
    }

    private void undoLastOperationChange() {
        if (undoStack.isEmpty()) {
            statusLabel.setText("Nothing to undo.");
            return;
        }
        clearActiveJsonProgram();
        operations.clear();
        operations.addAll(undoStack.remove(undoStack.size() - 1));
        inspectionStepIndex = operations.isEmpty()
            ? -1
            : Math.min(
                inspectionStepIndex,
                operations.size() - 1
            );
        refreshWorkspace();
        statusLabel.setText("Undo applied.");
    }

    private void handleGlobalShortcut(final KeyEvent event) {
        if (textInputHasFocus(event)) {
            return;
        }
        if (
            event.isShortcutDown()
            && event.getCode() == KeyCode.Z
        ) {
            undoLastOperationChange();
            event.consume();
            return;
        }
        if (
            event.getCode() == KeyCode.DELETE
            || event.getCode() == KeyCode.BACK_SPACE
        ) {
            removeSelectedOperation();
            event.consume();
        }
    }

    private static boolean textInputHasFocus(final KeyEvent event) {
        final Object target = event.getTarget();
        return target instanceof TextInputControl;
    }

    private void groupSelectedOperations() {
        clearActiveJsonProgram();
        final List<Integer> indices = selectedOperationIndices();
        if (indices.isEmpty()) {
            statusLabel.setText("Select operations before grouping.");
            return;
        }
        final ArrayList<DesktopIrOperationSpec> body = new ArrayList<>();
        for (int i = 0; i < indices.size(); i++) {
            body.add(operations.get(indices.get(i)));
        }
        final String name = customOperationRegistry.define(
            "CustomOperation" + (customOperationRegistry.size() + 1),
            customOperationRegistry.expand(body)
        );
        for (int i = indices.size() - 1; i >= 0; i--) {
            operations.remove(indices.get(i).intValue());
        }
        final int insertIndex = indices.get(0);
        operations.add(
            insertIndex,
            customOperationRegistry.reference(name)
        );
        selectedOperationIndices.clear();
        selectedOperationIndices.add(insertIndex);
        refreshWorkspace();
        operationList.getSelectionModel().clearAndSelect(insertIndex);
        statusLabel.setText("Grouped " + body.size() + " operations into " + name + ".");
    }

    private void refreshWorkspace() {
        if (activeJsonProgram == null) {
            if (useLargeProgramPreview()) {
                operationList.getItems().setAll(
                    largeProgramRenderPolicy.summary(operations.size()),
                    "Enable full huge circuit preview to render and list every operation."
                );
            } else {
                final ArrayList<String> labels = new ArrayList<>(operations.size());
                for (int i = 0; i < operations.size(); i++) {
                    labels.add(operations.get(i).label());
                }
                operationList.getItems().setAll(labels);
            }
            restoreOperationSelection();
        } else {
            operationList.getItems().setAll(
                "Native JSON program is active",
                "Graphical operation stream remains as an editable draft"
            );
        }
        renderCircuit();
        renderInspector();
        renderGateInfo();
        fullIrSurfaceArea.setText(irOperationSurfaceCatalog.render(russianLanguage()));
        if (useLargeProgramPreview()) {
            renderLargeProgramPreview();
            return;
        }
        javaDslArea.setText(generateCurrentJavaDsl());
        try {
            final QuantumProgram program = buildNativeProgram();
            final QuantumIrWriteResult writeResult = workspaceService.writeJson(program);
            final ValidationResult validation = workspaceService.validate(program);
            final ResourceEstimate resources = workspaceService.resources(
                program,
                24
            );
            final CapabilityPreflightResult preflight = workspaceService.preflight(
                program,
                targetFormatBox.getValue()
            );
            targetProfileArea.setText(renderTargetProfile(workspaceService.targetProfile(targetFormatBox.getValue())));
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
            refreshOverviewPanel(
                writeResult,
                validation,
                resources,
                preflight.status().name(),
                preflight.diagnostics().size()
            );
            assistantNotesArea.setText(programTextRenderer.renderAssistantNotes(
                activeJsonProgram != null,
                validation,
                resources,
                preflight.status().name(),
                targetFormatBox.getValue(),
                russianLanguage()
            ));
            nativeJsonArea.setText(writeResult.hasContent()
                ? writeResult.content()
                : renderPersistenceDiagnostics(writeResult));
            diagnosticsArea.setText("Native workspace built successfully.");
            if (autoSimulationBox.isSelected()) {
                renderCachedPreviewSimulation();
            } else {
                simulationArea.setText("Auto simulation is disabled. Press Simulate to run state-vector visualization.");
                qSphereView.renderEmpty();
                phaseDiskView.renderEmpty();
                simulationVisualizationsView.renderEmpty();
            }
            refreshGeneratedCodePreview(program);
        } catch (final Exception exception) {
            diagnosticsArea.setText(exceptionMessage(exception));
        }
    }

    private void renderCachedPreviewSimulation() {
        final QuantumProgram program = buildInspectionProgram();
        final int programHash = previewSimulationHash();
        if (
            hasLastPreviewProgramHash
            && lastPreviewProgramHash == programHash
        ) {
            statusLabel.setText("Auto simulation preview is up to date.");
            return;
        }
        lastPreviewProgramHash = programHash;
        hasLastPreviewProgramHash = true;
        statusLabel.setText("Auto simulation preview running...");
        final SimulationResult simulation = workspaceService.simulate(
            program,
            shots(),
            seed()
        );
        renderSimulationViews(simulation);
        statusLabel.setText("Auto simulation preview updated"
            + simulationScopeSuffix()
            + ": "
            + simulation.counts().size()
            + " measured states.");
    }

    private int previewSimulationHash() {
        int hash = 17;
        hash = 31 * hash + nativeJsonArea.getText().hashCode();
        hash = 31 * hash + inspectionStepIndex;
        hash = 31 * hash + shots();
        hash = 31 * hash + Long.hashCode(seed());
        hash = 31 * hash + Boolean.hashCode(hideZeroProbabilityBox.isSelected());
        hash = 31 * hash + Boolean.hashCode(registerBitOrderBox.isSelected());
        return hash;
    }

    private void invalidateAutoSimulationPreview() {
        hasLastPreviewProgramHash = false;
    }

    private static String renderPersistenceDiagnostics(final QuantumIrWriteResult writeResult) {
        final StringBuilder text = new StringBuilder("Native JSON is not available.").append(System.lineSeparator());
        for (int i = 0; i < writeResult.diagnostics().size(); i++) {
            text.append(writeResult.diagnostics().get(i).severity())
                .append(" ")
                .append(writeResult.diagnostics().get(i).code())
                .append(": ")
                .append(writeResult.diagnostics().get(i).message())
                .append(System.lineSeparator());
        }
        return text.toString();
    }

    private static String renderTargetProfile(final IntegrationCapabilityProfile profile) {
        final StringBuilder text = new StringBuilder();
        text.append("Target profile").append(System.lineSeparator());
        text.append("  format: ").append(profile.format()).append(System.lineSeparator());
        text.append("  name: ").append(profile.targetName()).append(System.lineSeparator());
        text.append("  version: ").append(profile.targetVersion()).append(System.lineSeparator());
        text.append("  max qubits: ").append(profile.hasMaxQubitCount()
            ? profile.maxQubitCount()
            : "unbounded").append(System.lineSeparator());
        text.append(System.lineSeparator());
        text.append("Native gates").append(System.lineSeparator());
        text.append("  ").append(profile.hasNativeGateSet()
            ? profile.nativeGateNames()
            : "not restricted").append(System.lineSeparator());
        text.append(System.lineSeparator());
        text.append("Parameter kinds").append(System.lineSeparator());
        text.append("  ").append(profile.hasSupportedParameterKinds()
            ? profile.supportedParameterKinds()
            : "not restricted").append(System.lineSeparator());
        text.append(System.lineSeparator());
        text.append("Capabilities").append(System.lineSeparator());
        text.append("  ").append(profile.capabilities()).append(System.lineSeparator());
        if (!profile.metadata().isEmpty()) {
            text.append(System.lineSeparator());
            text.append("Metadata").append(System.lineSeparator());
            profile.metadata().forEach((key, value) -> text.append("  ")
                .append(key)
                .append(": ")
                .append(value)
                .append(System.lineSeparator()));
        }
        return text.toString();
    }

    private void renderSimulationViews(final SimulationResult simulation) {
        qSphereView.setRussian(russianLanguage());
        phaseDiskView.setRussian(russianLanguage());
        simulationVisualizationsView.setRussian(russianLanguage());
        qSphereView.render(simulation);
        phaseDiskView.render(simulation);
        simulationVisualizationsView.render(
            simulation,
            hideZeroProbabilityBox.isSelected(),
            registerBitOrderBox.isSelected()
        );
        simulationArea.setText(simulationTextRenderer.render(
            simulation,
            hideZeroProbabilityBox.isSelected(),
            registerBitOrderBox.isSelected()
        ));
    }

    private void refreshGeneratedCodePreview(final QuantumProgram program) {
        if (!showGeneratedCodeBox.isSelected()) {
            return;
        }
        try {
            final DesktopWorkflowResult export = nativeWorkflowFacade.export(
                program,
                targetFormatBox.getValue(),
                executionOptions()
            );
            generatedArea.setText(export.generatedContent().isBlank()
                ? export.content()
                : export.generatedContent());
        } catch (final RuntimeException exception) {
            generatedArea.setText(exceptionMessage(exception));
        }
    }

    private void renderCircuit() {
        circuitRows.getChildren().clear();
        normalizeInspectionStep();
        updateInspectionStepLabel();
        if (useLargeProgramPreview()) {
            circuitRows.getChildren().setAll(largeCircuitPreviewNodes());
            return;
        }
        final int qubitCount = Math.max(
            1,
            positiveIntegerOrOne(qregSizeField.getText())
        );
        circuitRows.getChildren().setAll(circuitCanvasRenderer.renderCircuit(
            operations,
            qubitCount,
            qregNameField.getText(),
            isLsbWireOrder(),
            "Layer view".equals(layoutModeBox.getValue()),
            inspectionStepIndex,
            this::selectCircuitOperation,
            this::appendFromGridClick,
            this::selectOperationForDrag,
            this::dropOperationAt
        ));
    }

    private void setInspectionStep(final int stepIndex) {
        inspectionStepIndex = stepIndex;
        normalizeInspectionStep();
        synchronizeSelectionToInspectionStep();
        refreshWorkspace();
        Platform.runLater(() -> scrollInspectionStepIntoView(false));
        statusLabel.setText(inspectionStepIndex < 0
            ? "Inspecting full circuit."
            : "Inspecting circuit after operation #" + inspectionStepIndex + ".");
    }

    private void synchronizeSelectionToInspectionStep() {
        selectedOperationIndices.clear();
        if (inspectionStepIndex >= 0) {
            selectedOperationIndices.add(inspectionStepIndex);
        }
    }

    private void selectSingleOperationIndex(final int operationIndex) {
        selectedOperationIndices.clear();
        if (
            operationIndex >= 0
            && operationIndex < operations.size()
        ) {
            selectedOperationIndices.add(operationIndex);
        }
    }

    private void updateInspectionFromSelection(final int operationIndex) {
        if (
            activeJsonProgram != null
            || operationIndex == inspectionStepIndex
        ) {
            return;
        }
        inspectionStepIndex = operationIndex;
        normalizeInspectionStep();
        invalidateAutoSimulationPreview();
        refreshWorkspace();
        Platform.runLater(() -> scrollInspectionStepIntoView(false));
    }

    private int nextInspectionStep() {
        if (operations.isEmpty()) {
            return -1;
        }
        if (inspectionStepIndex < 0) {
            return 0;
        }
        return Math.min(
            operations.size() - 1,
            inspectionStepIndex + 1
        );
    }

    private void normalizeInspectionStep() {
        if (operations.isEmpty()) {
            inspectionStepIndex = -1;
            return;
        }
        if (inspectionStepIndex >= operations.size()) {
            inspectionStepIndex = operations.size() - 1;
        }
    }

    private void updateInspectionStepLabel() {
        inspectionStepLabel.setText(inspectionStepIndex < 0
            ? uiText("inspectFull")
            : uiText("inspectStep") + " " + inspectionStepIndex + " / " + (operations.size() - 1));
        inspectionOperationLabel.setText(inspectionStepIndex < 0
            ? uiText("operationFull")
            : uiText("operationPrefix") + " " + operationLabelRenderer.renderSummary(operations.get(inspectionStepIndex)));
        updateInspectionSlider();
    }

    private void updateInspectionSlider() {
        updatingInspectionSlider = true;
        try {
            inspectionSlider.setMin(-1.0);
            inspectionSlider.setMax(Math.max(
                0.0,
                operations.size() - 1.0
            ));
            inspectionSlider.setDisable(operations.isEmpty());
            inspectionSlider.setShowTickLabels(operations.size() <= 24);
            inspectionSlider.setMajorTickUnit(operations.size() <= 24
                ? 1.0
                : Math.max(
                    1.0,
                    Math.ceil(operations.size() / 8.0)
                ));
            inspectionSlider.setValue(inspectionStepIndex);
        } finally {
            updatingInspectionSlider = false;
        }
    }

    private QuantumProgram buildInspectionProgram() {
        if (
            inspectionStepIndex < 0
            || activeJsonProgram != null
        ) {
            return buildNativeProgram();
        }
        final int endExclusive = Math.min(
            operations.size(),
            inspectionStepIndex + 1
        );
        return workspaceService.buildProgram(
            circuitNameField.getText(),
            qregNameField.getText(),
            positiveIntegerOrOne(qregSizeField.getText()),
            cregNameField.getText(),
            positiveIntegerOrOne(cregSizeField.getText()),
            operations.subList(
                0,
                endExclusive
            ),
            customOperationRegistry
        );
    }

    private boolean useLargeProgramPreview() {
        return activeJsonProgram == null
            && largeProgramRenderPolicy.shouldUsePreview(
                operations.size(),
                renderHugeCircuitBox.isSelected()
            );
    }

    private List<Node> largeCircuitPreviewNodes() {
        final Label title = new Label("Large circuit preview is protected.");
        title.getStyleClass().add("panel-title");
        final Label summary = new Label(largeProgramRenderPolicy.summary(operations.size()));
        summary.getStyleClass().add("panel-hint");
        final Label details = new Label(
            "The IR is still editable and exportable through explicit actions. "
                + "Full graphical rendering can allocate millions of JavaFX nodes on large circuits."
        );
        details.getStyleClass().add("panel-hint");
        final Button enable = primaryButton(
            "Render full preview",
            () -> renderHugeCircuitBox.setSelected(true)
        );
        final VBox box = new VBox(
            12,
            title,
            summary,
            details,
            enable
        );
        box.getStyleClass().add("section-card");
        return List.of(box);
    }

    private void renderLargeProgramPreview() {
        final String summary = largeProgramRenderPolicy.summary(operations.size());
        javaDslArea.setText(summary + System.lineSeparator()
            + "Java DSL preview is deferred. Use explicit Java DSL action or enable full preview.");
        nativeJsonArea.setText(summary + System.lineSeparator()
            + "Native JSON preview is deferred. Use JSON action or enable full preview.");
        diagnosticsArea.setText(summary + System.lineSeparator()
            + "Validation/export actions still run on demand.");
        simulationArea.setText(summary + System.lineSeparator()
            + "Automatic simulation is deferred for this visual preview.");
        qSphereView.renderEmpty();
        phaseDiskView.renderEmpty();
        simulationVisualizationsView.renderEmpty();
        overviewArea.setText("Workspace" + System.lineSeparator()
            + "  source: graphical native builder" + System.lineSeparator()
            + "  operations: " + operations.size() + System.lineSeparator()
            + "  mode: protected large-program preview" + System.lineSeparator()
            + "  full preview limit: " + largeProgramRenderPolicy.interactiveOperationLimit());
        programBadgeLabel.setText("large / " + operations.size() + " ops");
        healthBadgeLabel.setText("Preview deferred");
        targetBadgeLabel.setText(targetFormatBox.getValue().name());
        refreshDiagnosticList(
            ValidationResult.valid(),
            "DEFERRED",
            0
        );
    }

    private void renderInspector() {
        inspectorArea.setText(programTextRenderer.renderInspector(
            circuitNameField.getText(),
            qregNameField.getText(),
            positiveIntegerOrOne(qregSizeField.getText()),
            cregNameField.getText(),
            positiveIntegerOrOne(cregSizeField.getText()),
            operations,
            gateCatalogView.gates(),
            targetFormatBox.getValue(),
            russianLanguage()
        ));
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
        secondAngleField.setText(Double.toString(operation.secondAngle()));
        thirdAngleField.setText(Double.toString(operation.thirdAngle()));
        durationField.setText(Double.toString(operation.durationValue()));
        durationUnitBox.setValue(operation.durationUnit());
        labelNameField.setText(operation.labelName());
        updateOperationEditorAvailability();
        diagnosticsArea.setText(
            "Selected operation #" + index + System.lineSeparator()
                + "gate: " + operation.gate() + System.lineSeparator()
                + "primary: " + operation.primaryQubit() + System.lineSeparator()
                + "secondary: " + operation.secondaryQubit() + System.lineSeparator()
                + "tertiary: " + operation.tertiaryQubit() + System.lineSeparator()
                + "classical: " + operation.classicalBit() + System.lineSeparator()
                + "angle: " + operation.angle() + System.lineSeparator()
                + "phi: " + operation.secondAngle() + System.lineSeparator()
                + "lambda: " + operation.thirdAngle() + System.lineSeparator()
                + "duration: " + operation.durationValue() + operation.durationUnit().toLowerCase() + System.lineSeparator()
                + "label: " + operation.labelName()
        );
    }

    private void selectCircuitOperation(final int operationIndex) {
        selectedOperationIndices.clear();
        selectedOperationIndices.add(operationIndex);
        operationList.getSelectionModel().clearAndSelect(operationIndex);
        setInspectionStep(operationIndex);
        statusLabel.setText("Selected " + operations.get(operationIndex).label()
            + " / inspecting prefix through step " + operationIndex + ".");
    }

    private void selectOperationForDrag(final int operationIndex) {
        if (!selectedOperationIndices.contains(operationIndex)) {
            selectedOperationIndices.clear();
            selectedOperationIndices.add(operationIndex);
            operationList.getSelectionModel().clearAndSelect(operationIndex);
        }
    }

    private void dropOperationAt(
        final int draggedOperationIndex,
        final int targetIndex
    ) {
        final ArrayList<Integer> dragSelection = new ArrayList<>(selectedOperationIndices);
        if (!dragSelection.contains(draggedOperationIndex)) {
            dragSelection.clear();
            dragSelection.add(draggedOperationIndex);
        }
        final List<DesktopIrOperationSpec> reordered = operationReorderService.moveSelection(
            operations,
            dragSelection,
            targetIndex
        );
        final List<Integer> movedIndices = operationReorderService.movedSelectionIndices(
            dragSelection,
            targetIndex,
            operations.size()
        );
        operations.clear();
        operations.addAll(reordered);
        selectedOperationIndices.clear();
        selectedOperationIndices.addAll(movedIndices);
        clearActiveJsonProgram();
        refreshWorkspace();
        statusLabel.setText("Reordered " + dragSelection.size() + " operation(s).");
    }

    private void synchronizeSelectedOperationIndices() {
        selectedOperationIndices.clear();
        final List<Integer> selection = operationList.getSelectionModel().getSelectedIndices();
        for (int i = 0; i < selection.size(); i++) {
            final int index = selection.get(i);
            if (
                index >= 0
                && index < operations.size()
            ) {
                selectedOperationIndices.add(Integer.valueOf(index));
            }
        }
        selectedOperationIndices.sort(Integer::compareTo);
    }

    private List<Integer> selectedOperationIndices() {
        synchronizeSelectedOperationIndices();
        return List.copyOf(selectedOperationIndices);
    }

    private void restoreOperationSelection() {
        operationList.getSelectionModel().clearSelection();
        for (int i = 0; i < selectedOperationIndices.size(); i++) {
            final int index = selectedOperationIndices.get(i);
            if (
                index >= 0
                && index < operationList.getItems().size()
            ) {
                operationList.getSelectionModel().select(index);
            }
        }
    }

    private void refreshOverviewPanel(
        final QuantumIrWriteResult writeResult,
        final ValidationResult validation,
        final ResourceEstimate resources,
        final String preflightStatus,
        final int preflightDiagnostics
    ) {
        overviewArea.setText(programTextRenderer.renderOverview(
            activeJsonProgram != null,
            circuitNameField.getText(),
            experienceModeBox.getValue(),
            layoutModeBox.getValue(),
            wireOrderBox.getValue(),
            targetFormatBox.getValue(),
            writeResult,
            validation,
            resources,
            preflightStatus,
            preflightDiagnostics,
            russianLanguage()
        ));
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

    private void appendFromGridClick(final String qubit) {
        clearActiveJsonProgram();
        final DesktopGridPlacementResult result = gridPlacementService.place(
            gateBox.getValue(),
            qubit,
            secondaryQubitBox.getValue(),
            tertiaryQubitBox.getValue(),
            classicalBitBox.getValue(),
            angle(),
            pendingQubits
        );
        pendingQubits.clear();
        pendingQubits.addAll(result.pendingQubits());
        builderHintLabel.setText(result.hint());
        if (!result.hasOperation()) {
            return;
        }
        operations.add(result.operation());
        inspectionStepIndex = operations.size() - 1;
        selectSingleOperationIndex(inspectionStepIndex);
        invalidateAutoSimulationPreview();
        clearPending();
        refreshWorkspace();
        operationList.getSelectionModel().select(inspectionStepIndex);
    }

    private void clearPending() {
        pendingQubits.clear();
        builderHintLabel.setText("Select a gate, then click a qubit lane to place it.");
    }

    private boolean isLsbWireOrder() {
        return "LSB top-to-bottom".equals(wireOrderBox.getValue());
    }

    private void renderGateInfo() {
        gateInfoArea.setText(gateInfoRenderer.render(
            gateBox.getValue(),
            russianLanguage()
        ));
    }

    private void renderWikiSearch(final String query) {
        final String normalized = query == null
            ? ""
            : query.trim().toLowerCase();
        if (normalized.isBlank()) {
            renderGateInfo();
            return;
        }
        final StringBuilder text = new StringBuilder();
        final List<String> entries = wikiEntries();
        int matches = 0;
        for (int i = 0; i < entries.size(); i++) {
            final String entry = entries.get(i);
            if (entry.toLowerCase().contains(normalized)) {
                if (matches > 0) {
                    text.append(System.lineSeparator());
                }
                text.append(entry).append(System.lineSeparator());
                matches++;
            }
        }
        if (matches == 0) {
            text.append(uiText("wikiNoMatches")).append(System.lineSeparator());
        }
        gateInfoArea.setText(text.toString());
        selectResultTab(uiText("tabGateInfo"));
    }

    private List<String> wikiEntries() {
        if (russianLanguage()) {
            return List.of(
                "Gate H/X/Y/Z: одиночные унитарные операции. Выберите gate, затем нажмите линию q[i].",
                "Gate RX/RY/RZ/PHASE: параметризованные повороты. Угол берется из поля Angle.",
                "Gate CX/CY/CZ/CH/SWAP/CCX: multi-qubit операции. Используют Qubit A/B/C.",
                "MEASURE: измеряет qubit в classical bit. Использует Qubit A и Classical.",
                "RESET: сбрасывает qubit. Использует Qubit A.",
                "BARRIER: визуальная и компиляционная граница между операциями.",
                "Удаление: выберите операцию на схеме или в списке и нажмите Delete/Backspace.",
                "Отмена: Ctrl+Z возвращает последнее изменение списка операций.",
                "Перемещение: выберите операцию и используйте Left/Right либо drag-and-drop по схеме.",
                "Validate IR: проверяет native Quantum IR и открывает вкладку Диагностика.",
                "Simulate: запускает локальную state-vector симуляцию и обновляет Simulation, Probabilities, Statevector, Q-Sphere.",
                "Export: экспортирует active IR в выбранный OpenQASM 2, OpenQASM 3 или Quil.",
                "Preflight: заранее показывает, можно ли экспортировать без потери логики.",
                "Render huge circuit: для миллионов операций оставляет UI отзывчивым и не рендерит все ячейки без явного разрешения.",
                "Вид: управляет видимостью панелей визуализации и preview экспорта.",
                "Язык и тема: переключают подписи, меню, wiki и визуальные панели."
            );
        }
        return List.of(
            "Gate H/X/Y/Z: single-qubit unitary operations. Select a gate, then click q[i].",
            "Gate RX/RY/RZ/PHASE: parameterized rotations. Angle comes from the Angle field.",
            "Gate CX/CY/CZ/CH/SWAP/CCX: multi-qubit operations. Use Qubit A/B/C.",
            "MEASURE: measures a qubit into a classical bit. Uses Qubit A and Classical.",
            "RESET: resets a qubit. Uses Qubit A.",
            "BARRIER: visual and compilation boundary between operations.",
            "Delete: select an operation on the circuit or in the list and press Delete/Backspace.",
            "Undo: Ctrl+Z restores the previous operation list state.",
            "Move: select an operation and use Left/Right or drag-and-drop on the circuit.",
            "Validate IR: validates native Quantum IR and opens Diagnostics.",
            "Simulate: runs local state-vector simulation and updates Simulation, Probabilities, Statevector, Q-Sphere.",
            "Export: exports active IR to selected OpenQASM 2, OpenQASM 3 or Quil.",
            "Preflight: reports whether export can be represented without losing logic.",
            "Render huge circuit: keeps UI responsive for millions of operations unless full cell rendering is explicitly enabled.",
            "View: controls visualization panels and export preview visibility.",
            "Language and theme: switch labels, menus, wiki and visualization panels."
        );
    }

    private void installHelp(
        final Node node,
        final String textKey
    ) {
        node.getStyleClass().add("help-enabled");
        node.setOnContextMenuRequested(event -> {
            final TextArea area = new TextArea(uiText(textKey));
            area.setWrapText(true);
            area.setEditable(false);
            area.setPrefSize(
                360.0,
                130.0
            );
            area.getStyleClass().add("help-body");
            final VBox panel = new VBox(
                8.0,
                new Label(uiText("tabGateInfo")),
                area
            );
            panel.getStyleClass().add("help-panel");
            final CustomMenuItem item = new CustomMenuItem(
                panel,
                false
            );
            final ContextMenu menu = new ContextMenu(item);
            menu.getStyleClass().add("help-context-menu");
            menu.show(
                node,
                event.getScreenX(),
                event.getScreenY()
            );
            event.consume();
        });
    }

    private String generateCurrentJavaDsl() {
        return workspaceService.generateJavaDsl(
            circuitNameField.getText(),
            qregNameField.getText(),
            positiveIntegerOrOne(qregSizeField.getText()),
            cregNameField.getText(),
            positiveIntegerOrOne(cregSizeField.getText()),
            operations,
            customOperationRegistry
        );
    }

    private QuantumProgram buildNativeProgram() {
        if (activeJsonProgram != null) {
            return activeJsonProgram;
        }
        return workspaceService.buildProgram(
            circuitNameField.getText(),
            qregNameField.getText(),
            positiveIntegerOrOne(qregSizeField.getText()),
            cregNameField.getText(),
            positiveIntegerOrOne(cregSizeField.getText()),
            operations,
            customOperationRegistry
        );
    }

    private DesktopWorkflowResult validateNative() {
        return nativeWorkflowFacade.validate(buildNativeProgram());
    }

    private DesktopWorkflowResult inspectNative() {
        return nativeWorkflowFacade.inspect(
            buildNativeProgram(),
            targetFormatBox.getValue()
        );
    }

    private DesktopWorkflowResult resourcesNative() {
        return nativeWorkflowFacade.resources(buildNativeProgram());
    }

    private DesktopWorkflowResult timelineNative() {
        return nativeWorkflowFacade.timeline(buildNativeProgram());
    }

    private DesktopWorkflowResult jsonNative() {
        return nativeWorkflowFacade.json(buildNativeProgram());
    }

    private void saveJavaDsl(final Stage stage) {
        final FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Quantum IR Java DSL");
        chooser.setInitialFileName(circuitNameField.getText() + ".java");
        configureFileChooserDirectory(chooser);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
            "Java source",
            "*.java"
        ));
        final java.io.File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }
        try {
            final DesktopWorkflowResult result = javaDslNative();
            if (!result.isSuccess()) {
                diagnosticsArea.setText(result.content());
                statusLabel.setText("Java DSL was not saved");
                return;
            }
            Files.writeString(
                file.toPath(),
                result.content()
            );
            rememberFileChooserDirectory(file);
            javaDslArea.setText(result.content());
            statusLabel.setText("Saved Java DSL: " + file.getAbsolutePath());
        } catch (final Exception exception) {
            diagnosticsArea.setText(exceptionMessage(exception));
            statusLabel.setText("Save Java DSL failed");
        }
    }

    private void openJavaDsl(final Stage stage) {
        final FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Quantum IR Java DSL");
        configureFileChooserDirectory(chooser);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
            "Java source",
            "*.java"
        ));
        final java.io.File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }
        try {
            final String content = Files.readString(file.toPath());
            final DesktopJavaDslImportResult result = javaDslImporter.importDsl(content);
            rememberFileChooserDirectory(file);
            javaDslArea.setText(content);
            if (!result.isSuccess()) {
                diagnosticsArea.setText(String.join(
                    System.lineSeparator(),
                    result.diagnostics()
                ));
                statusLabel.setText("Open Java DSL has diagnostics");
                selectResultTab(uiText("tabDiagnostics"));
                return;
            }
            clearActiveJsonProgram();
            rememberOperations();
            circuitNameField.setText(result.circuitName());
            qregNameField.setText(result.quantumRegisterName());
            qregSizeField.setText(Integer.toString(result.quantumRegisterSize()));
            cregNameField.setText(result.classicalRegisterName());
            cregSizeField.setText(Integer.toString(result.classicalRegisterSize()));
            operations.clear();
            operations.addAll(result.operations());
            selectedOperationIndices.clear();
            inspectionStepIndex = operations.isEmpty()
                ? -1
                : operations.size() - 1;
            refreshReferenceBoxes();
            refreshWorkspace();
            selectResultTab("Java DSL");
            statusLabel.setText("Opened Java DSL: " + file.getAbsolutePath());
        } catch (final Exception exception) {
            diagnosticsArea.setText(exceptionMessage(exception));
            statusLabel.setText("Open Java DSL failed");
        }
    }

    private void saveNativeJson(final Stage stage) {
        final FileChooser chooser = new FileChooser();
        chooser.setTitle("Save native Quantum IR JSON");
        chooser.setInitialFileName(circuitNameField.getText() + ".quantum.json");
        configureFileChooserDirectory(chooser);
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
            rememberFileChooserDirectory(file);
            statusLabel.setText("Saved native JSON: " + file.getAbsolutePath());
        } catch (final Exception exception) {
            diagnosticsArea.setText(exceptionMessage(exception));
            statusLabel.setText("Save native JSON failed");
        }
    }

    private void openNativeJson(final Stage stage) {
        final FileChooser chooser = new FileChooser();
        chooser.setTitle("Open native Quantum IR JSON");
        configureFileChooserDirectory(chooser);
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
            rememberFileChooserDirectory(file);
            nativeJsonArea.setText(content);
            diagnosticsArea.setText(render(read));
            if (read.isSuccess()) {
                activeJsonProgram = read.program();
                synchronizeWorkspaceWithActiveJsonProgram();
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

    private void configureFileChooserDirectory(final FileChooser chooser) {
        final String directory = preferences.get(
            PREFERENCES_LAST_FILE_DIRECTORY,
            null
        );
        if (
            directory == null
            || directory.isBlank()
        ) {
            return;
        }
        final java.io.File file = new java.io.File(directory);
        if (
            file.exists()
            && file.isDirectory()
        ) {
            chooser.setInitialDirectory(file);
        }
    }

    private void rememberFileChooserDirectory(final java.io.File file) {
        final java.io.File directory = file.getParentFile();
        if (
            directory != null
            && directory.exists()
            && directory.isDirectory()
        ) {
            preferences.put(
                PREFERENCES_LAST_FILE_DIRECTORY,
                directory.getAbsolutePath()
            );
        }
    }

    private DesktopWorkflowResult simulateNative() {
        final QuantumProgram program = buildInspectionProgram();
        final SimulationResult simulation = workspaceService.simulate(
            program,
            shots(),
            seed()
        );
        lastNativeSimulation = simulation;
        return DesktopWorkflowResult.of(
            DesktopAction.SIMULATE,
            simulation.isSuccess(),
            simulation.isSuccess() ? "SIMULATED" : "SIMULATION_FAILED",
            "Qubits: " + simulation.qubitCount()
                + ", shots: " + simulation.shots()
                + simulationScopeSuffix(),
            simulationTextRenderer.render(
                simulation,
                hideZeroProbabilityBox.isSelected(),
                registerBitOrderBox.isSelected()
            )
        );
    }

    private String simulationScopeSuffix() {
        return inspectionStepIndex < 0
            ? ""
            : ", inspected through step " + inspectionStepIndex;
    }

    private DesktopWorkflowResult exportNative() {
        return nativeWorkflowFacade.export(
            buildNativeProgram(),
            targetFormatBox.getValue(),
            executionOptions()
        );
    }

    private DesktopWorkflowResult preflightNative() {
        return nativeWorkflowFacade.preflight(
            buildNativeProgram(),
            targetFormatBox.getValue()
        );
    }

    private DesktopWorkflowResult compatibilityNative() {
        return nativeWorkflowFacade.compatibility(
            buildNativeProgram(),
            shots(),
            seed(),
            executionOptions()
        );
    }

    private DesktopWorkflowResult transformNative() {
        return nativeWorkflowFacade.transform(
            buildNativeProgram(),
            targetFormatBox.getValue(),
            canonicalizeParametersBox.isSelected(),
            removeIdentityBox.isSelected(),
            inlineCompositeBox.isSelected(),
            targetLoweringBox.isSelected()
        );
    }

    private DesktopWorkflowResult javaDslNative() {
        return nativeWorkflowFacade.javaDsl(
            circuitNameField.getText(),
            qregNameField.getText(),
            positiveIntegerOrOne(qregSizeField.getText()),
            cregNameField.getText(),
            positiveIntegerOrOne(cregSizeField.getText()),
            operations
        );
    }

    private DesktopWorkflowResult workflowNative() {
        return nativeWorkflowFacade.workflow(
            buildNativeProgram(),
            targetFormatBox.getValue(),
            shots(),
            seed(),
            executionOptions()
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
                synchronizeWorkspaceWithActiveJsonProgram();
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

    private void synchronizeWorkspaceWithActiveJsonProgram() {
        if (activeJsonProgram == null) {
            return;
        }
        final DesktopIrProgramSnapshot snapshot = workspaceService.projectToGraphicalWorkspace(activeJsonProgram);
        circuitNameField.setText(snapshot.circuitName());
        qregNameField.setText(snapshot.quantumRegisterName());
        qregSizeField.setText(Integer.toString(snapshot.quantumRegisterSize()));
        cregNameField.setText(snapshot.classicalRegisterName());
        cregSizeField.setText(Integer.toString(snapshot.classicalRegisterSize()));
        operations.clear();
        operations.addAll(snapshot.operations());
        selectedOperationIndices.clear();
        inspectionStepIndex = operations.isEmpty()
            ? -1
            : operations.size() - 1;
        refreshReferenceBoxes();
        synchronizeEditorDefaults();
        updateOperationEditorAvailability();
        if (!snapshot.diagnostics().isEmpty()) {
            diagnosticsArea.setText(String.join(
                System.lineSeparator(),
                snapshot.diagnostics()
            ));
        }
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
        diagnosticList.getItems().setAll(diagnosticListRenderer.render(
            validation,
            targetFormatBox.getValue(),
            preflightStatus,
            preflightDiagnosticCount,
            activeJsonProgram != null,
            activeJsonProgram == null && useLargeProgramPreview(),
            operations
        ));
    }

    private void navigateDiagnostic(final String item) {
        if (item == null) {
            return;
        }
        diagnosticsArea.setText(item);
        final int index = diagnosticListRenderer.operationIndexFromItem(item);
        if (
            index >= 0
            && index < operations.size()
        ) {
            operationList.getSelectionModel().clearAndSelect(index);
            setInspectionStep(index);
            renderSelectedOperation(index);
            statusLabel.setText("Diagnostic selected operation #" + index
                + "; inspecting prefix through this step.");
        }
    }

    private void clearActiveJsonProgram() {
        activeJsonProgram = null;
    }

    private void runNative(
        final String action,
        final DesktopWorkflow workflow
    ) {
        statusLabel.setText(action + " running... wait for result.");
        diagnosticsArea.setText(action + " is running. Please wait...");
        if ("Validate".equals(action)) {
            selectResultTab(uiText("tabDiagnostics"));
        } else if ("Simulate".equals(action)) {
            selectResultTab(uiText("tabSimulation"));
            simulationArea.setText("Simulation is running. Please wait...");
        } else if ("Export".equals(action)) {
            selectResultTab(uiText("tabGeneratedExport"));
            generatedArea.setText("Export is running. Please wait...");
            codePreviewArea.setText("Export is running. Please wait...");
        }
        final Task<DesktopWorkflowResult> task = new Task<>() {
            @Override
            protected DesktopWorkflowResult call() throws Exception {
                return workflow.run();
            }
        };
        task.setOnSucceeded(event -> {
            final DesktopWorkflowResult result = task.getValue();
            applyNativeWorkflowResult(result);
            statusLabel.setText(result.action() + " finished: " + result.status() + " / " + result.summary());
        });
        task.setOnFailed(event -> {
            final Throwable exception = task.getException();
            statusLabel.setText(action + " failed");
            diagnosticsArea.setText(exceptionMessage(exception));
        });
        startBackground(task);
    }

    private void applyNativeWorkflowResult(final DesktopWorkflowResult result) {
        statusLabel.setText(result.action() + " / " + result.status() + " / " + result.summary());
        refreshWorkspace();
        if (result.action() == DesktopAction.SIMULATE) {
            final SimulationResult simulation = lastNativeSimulation;
            if (simulation == null) {
                simulationArea.setText(result.content());
            } else {
                renderSimulationViews(simulation);
            }
            selectResultTab(uiText("tabSimulation"));
        } else if (result.action() == DesktopAction.JSON) {
            if (result.status().startsWith("NATIVE_JSON_")) {
                diagnosticsArea.setText(result.content());
            } else {
                nativeJsonArea.setText(result.content());
                diagnosticsArea.setText(result.summary());
            }
        } else if (result.action() == DesktopAction.INSPECT) {
            inspectorArea.setText(result.content());
            selectResultTab(uiText("tabInspector"));
        } else if (result.action() == DesktopAction.VALIDATE) {
            diagnosticsArea.setText(result.content());
            selectResultTab(uiText("tabDiagnostics"));
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
            codePreviewArea.setText(result.generatedContent());
        }
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

    private void runUiSmokeIfRequested(final Stage stage) {
        if (!getParameters().getRaw().contains("--ui-smoke")) {
            return;
        }
        scheduleSmoke(
            () -> runUiSmokeBeforeAsyncActions(stage),
            () -> runUiSmokeAfterAsyncActions(stage)
        );
    }

    private void runVisualAuditIfRequested(final Stage stage) {
        visualAuditController.scheduleIfRequested(
            stage,
            getParameters().getRaw(),
            getParameters().getNamed(),
            this::prepareScreenshotState,
            outputPath -> saveStageScreenshot(
                stage,
                outputPath
            )
        );
    }

    private void prepareScreenshotState(
        final String fixtureName,
        final String tabName,
        final Integer inspectStep,
        final Integer operationIndex
    ) {
        loadAuditFixtureWithoutRefresh(fixtureName);
        refreshWorkspace();
        if (requiresExpertScreenshotMode(tabName)) {
            experienceModeBox.setValue("Expert");
            applyExperienceMode();
        }
        if (inspectStep != null) {
            setInspectionStep(inspectStep.intValue());
        }
        final DesktopWorkflowResult simulation = simulateNative();
        applyNativeWorkflowResult(simulation);
        prepareScreenshotTab(tabName);
        refreshScreenshotOverview();
        scrollCircuitForScreenshot(operationIndex);
        selectScreenshotView(
            primaryStage,
            tabName
        );
    }

    private static boolean requiresExpertScreenshotMode(final String tabName) {
        return switch (tabName) {
            case "Target Profile", "Resources", "Preflight", "Compatibility", "Transform" -> true;
            default -> false;
        };
    }

    private void refreshScreenshotOverview() {
        final QuantumProgram program = buildNativeProgram();
        final QuantumIrWriteResult writeResult = workspaceService.writeJson(program);
        final ValidationResult validation = workspaceService.validate(program);
        final ResourceEstimate resources = workspaceService.resources(
            program,
            24
        );
        final CapabilityPreflightResult preflight = workspaceService.preflight(
            program,
            targetFormatBox.getValue()
        );
        refreshOverviewPanel(
            writeResult,
            validation,
            resources,
            preflight.status().name(),
            preflight.diagnostics().size()
        );
        updateStatusBadges(
            validation,
            resources,
            preflight.status().name()
        );
    }

    private void prepareScreenshotTab(final String tabName) {
        final DesktopWorkflowResult result = switch (tabName) {
            case "Inspector" -> inspectNative();
            case "Java DSL" -> javaDslNative();
            case "Resources" -> resourcesNative();
            case "Preflight" -> preflightNative();
            case "Compatibility" -> prepareCompatibilityScreenshot();
            case "Transform" -> transformNative();
            case "Native JSON" -> jsonNative();
            case "Diagnostics", "Diagnostic List" -> {
                refreshDiagnosticList(
                    workspaceService.validate(buildNativeProgram()),
                    workspaceService.preflight(
                        buildNativeProgram(),
                        targetFormatBox.getValue()
                    ).status().name(),
                    workspaceService.preflight(
                        buildNativeProgram(),
                        targetFormatBox.getValue()
                    ).diagnostics().size()
                );
                yield validateNative();
            }
            case "Generated Export" -> exportNative();
            case "External Formats" -> prepareExternalScreenshot();
            case "View Menu" -> {
                if (viewMenuButton != null) {
                    viewMenuButton.show();
                }
                yield null;
            }
            default -> null;
        };
        if (result != null) {
            applyNativeWorkflowResult(result);
        }
    }

    private DesktopWorkflowResult prepareCompatibilityScreenshot() {
        return compatibilityScreenshotPreview.preview(
            buildNativeProgram(),
            operations.size()
        );
    }
    private DesktopWorkflowResult prepareExternalScreenshot() {
        externalInputFormatBox.setValue(IntegrationFormat.OPENQASM_2);
        externalTargetFormatBox.setValue(IntegrationFormat.OPENQASM_3);
        externalSourceArea.setText(DEFAULT_EXTERNAL_PROGRAM);
        final DesktopWorkflowResult result = externalService.compile(
            externalInputFormatBox.getValue(),
            externalSourceArea.getText(),
            externalTargetFormatBox.getValue(),
            executionOptions()
        );
        statusLabel.setText(result.action() + " / " + result.status() + " / " + result.summary());
        externalResultArea.setText(result.content());
        externalGeneratedArea.setText(result.generatedContent());
        return null;
    }
    private void selectScreenshotView(
        final Stage stage,
        final String text
    ) {
        if (
            "Visual Circuit".equals(text)
            || "Native IR Studio".equals(text)
        ) {
            selectTabByAnyText(
                stage,
                "Native IR Studio",
                "Родная IR-студия"
            );
            selectTabByAnyText(
                stage,
                "Overview",
                uiText("tabOverview")
            );
            return;
        }
        if ("Execution Settings".equals(text)) {
            selectTabByAnyText(
                stage,
                "Execution Settings",
                "Настройки запуска"
            );
            return;
        }
        if ("View Menu".equals(text)) {
            return;
        }
        selectTabByAnyText(
            stage,
            text,
            localizedTabName(text)
        );
    }

    private String localizedTabName(final String text) {
        return switch (text) {
            case "Overview" -> uiText("tabOverview");
            case "Inspector" -> uiText("tabInspector");
            case "Simulation" -> uiText("tabSimulation");
            case "Probabilities" -> uiText("tabProbabilities");
            case "Statevector" -> uiText("tabStateVector");
            case "Q-Sphere" -> uiText("tabQSphere");
            case "Gate Info" -> uiText("tabGateInfo");
            case "Assistant Notes" -> uiText("tabAssistantNotes");
            case "Full IR Surface" -> uiText("tabFullIrSurface");
            case "Diagnostic List" -> uiText("tabDiagnosticList");
            case "Target Profile" -> uiText("tabTargetProfile");
            case "Resources" -> uiText("tabResources");
            case "Preflight" -> uiText("tabPreflight");
            case "Compatibility" -> uiText("tabCompatibility");
            case "Transform" -> uiText("tabTransform");
            case "Native JSON" -> uiText("tabNativeJson");
            case "Diagnostics" -> uiText("tabDiagnostics");
            case "Generated Export" -> uiText("tabGeneratedExport");
            default -> text;
        };
    }

    private static void selectTabByAnyText(
        final Stage stage,
        final String... texts
    ) {
        RuntimeException lastFailure = null;
        for (final String text : texts) {
            try {
                selectTabByText(
                    stage,
                    text
                );
                return;
            } catch (final RuntimeException exception) {
                lastFailure = exception;
            }
        }
        if (lastFailure != null) {
            throw lastFailure;
        }
    }

    private void scrollCircuitForScreenshot(final Integer operationIndex) {
        if (circuitScrollPane == null) {
            return;
        }
        if (operationIndex == null) {
            circuitScrollPane.setHvalue(0.0);
            circuitScrollPane.setVvalue(0.0);
            return;
        }
        final double safeOperationCount = Math.max(
            1.0,
            operations.size() - 1.0
        );
        circuitScrollPane.setHvalue(Math.max(
            0.0,
            Math.min(
                1.0,
                operationIndex.intValue() / safeOperationCount
            )
        ));
        Platform.runLater(() -> scrollInspectionStepIntoView(true));
    }

    private void scrollInspectionStepIntoView(final boolean force) {
        scrollOperationIntoView(
            circuitScrollPane,
            circuitRows,
            inspectionStepIndex,
            force
        );
    }

    private void runUiSmokeBeforeAsyncActions(final Stage stage) {
        loadAuditFixtureWithoutRefresh("dense-spectrum");
        refreshWorkspace();
        operationList.getSelectionModel().select(0);
        selectAllTabs(stage);
        selectTabByText(
            stage,
            "Native IR Studio"
        );
        verifyAllGateButtons(
            stage,
            gateCatalogView.gates()
        );
        verifyGateButtonsSelectAndUpdate(
            stage,
            gateCatalogView.gates(),
            gateBox,
            gateInfoArea
        );
        verifyOperationEditorFieldAvailability();
        verifyCircuitContainsEveryGate(
            stage,
            gateCatalogView.gates()
        );
        verifyFullIrSurface(fullIrSurfaceArea);
        loadAuditFixtureWithoutRefresh("bell");
        refreshWorkspace();
        verifyDiagnosticSelectionNavigation();
        verifyMeasuredBellSimulationSemantics(workspaceService.simulate(
            buildNativeProgram(),
            shots(),
            seed()
        ));
        verifyPureBellStateVectorSemantics(
            workspaceService,
            seed()
        );
        verifyGridAutoSimulationPreview(stage);
        final DesktopWorkflowResult simulation = simulateNative();
        if (!simulation.isSuccess()) {
            throw new IllegalStateException("UI smoke Bell q-sphere simulation failed: " + simulation.summary() + ".");
        }
        fireVisibleButton(
            stage,
            "Validate IR"
        );
        fireVisibleButton(
            stage,
            "Simulate"
        );
        fireVisibleButton(
            stage,
            "Export"
        );
        fireVisibleButton(
            stage,
            "Add"
        );
        experienceModeBox.setValue("Expert");
        applyExperienceMode();
        operationList.getSelectionModel().select(0);
        fireVisibleButton(
            stage,
            "Duplicate"
        );
        operationList.getSelectionModel().select(0);
        operationList.getSelectionModel().select(1);
        fireVisibleButton(
            stage,
            "Group"
        );
        fireVisibleButton(
            stage,
            "JSON"
        );
        fireVisibleButton(
            stage,
            "Inspect"
        );
        fireVisibleButton(
            stage,
            "Preflight"
        );
    }

    private void verifyDiagnosticSelectionNavigation() {
        final int expectedOperationIndex = Math.min(
            2,
            operations.size() - 1
        );
        if (expectedOperationIndex < 0) {
            throw new IllegalStateException("Diagnostic navigation smoke has no operations.");
        }
        int diagnosticItemIndex = -1;
        for (int i = 0; i < diagnosticList.getItems().size(); i++) {
            if (diagnosticList.getItems().get(i).startsWith("OP #" + expectedOperationIndex + " ")) {
                diagnosticItemIndex = i;
                break;
            }
        }
        if (diagnosticItemIndex < 0) {
            throw new IllegalStateException("Diagnostic navigation smoke did not find OP #"
                + expectedOperationIndex + ".");
        }
        diagnosticList.getSelectionModel().select(diagnosticItemIndex);
        if (inspectionStepIndex != expectedOperationIndex) {
            throw new IllegalStateException("Diagnostic navigation selected OP #"
                + expectedOperationIndex + " but inspection step is " + inspectionStepIndex + ".");
        }
    }

    private void verifyOperationEditorFieldAvailability() {
        final List<String> gates = gateCatalogView.gates();
        for (int i = 0; i < gates.size(); i++) {
            final String gate = gates.get(i);
            verifyEditorAvailability(
                gate,
                new EditorFieldExpectation(
                    usesPrimaryQubit(gate),
                    usesSecondaryQubit(gate),
                    usesTertiaryQubit(gate),
                    usesClassicalBit(gate),
                    usesAngle(gate),
                    usesSecondAngle(gate),
                    usesThirdAngle(gate),
                    usesDuration(gate),
                    usesLabelName(gate)
                )
            );
        }
    }

    private void verifyEditorAvailability(
        final String gate,
        final EditorFieldExpectation expectation
    ) {
        gateBox.setValue(gate);
        updateOperationEditorAvailability();
        verifyEnabled(
            gate,
            "Qubit A",
            primaryQubitBox.isDisabled(),
            expectation.primaryEnabled()
        );
        verifyEnabled(
            gate,
            "Qubit B",
            secondaryQubitBox.isDisabled(),
            expectation.secondaryEnabled()
        );
        verifyEnabled(
            gate,
            "Qubit C",
            tertiaryQubitBox.isDisabled(),
            expectation.tertiaryEnabled()
        );
        verifyEnabled(
            gate,
            "Classical",
            classicalBitBox.isDisabled(),
            expectation.classicalEnabled()
        );
        verifyEnabled(
            gate,
            "Angle",
            angleField.isDisabled(),
            expectation.angleEnabled()
        );
        verifyEnabled(
            gate,
            "Phi",
            secondAngleField.isDisabled(),
            expectation.secondAngleEnabled()
        );
        verifyEnabled(
            gate,
            "Lambda",
            thirdAngleField.isDisabled(),
            expectation.thirdAngleEnabled()
        );
        verifyEnabled(
            gate,
            "Duration",
            durationField.isDisabled(),
            expectation.durationEnabled()
        );
        verifyEnabled(
            gate,
            "Duration unit",
            durationUnitBox.isDisabled(),
            expectation.durationEnabled()
        );
        verifyEnabled(
            gate,
            "Label",
            labelNameField.isDisabled(),
            expectation.labelEnabled()
        );
    }

    private record EditorFieldExpectation(
        boolean primaryEnabled,
        boolean secondaryEnabled,
        boolean tertiaryEnabled,
        boolean classicalEnabled,
        boolean angleEnabled,
        boolean secondAngleEnabled,
        boolean thirdAngleEnabled,
        boolean durationEnabled,
        boolean labelEnabled
    ) {
    }

    private static void verifyEnabled(
        final String gate,
        final String field,
        final boolean disabled,
        final boolean expectedEnabled
    ) {
        if (disabled == expectedEnabled) {
            throw new IllegalStateException("Gate " + gate + " has wrong editor availability for " + field + ".");
        }
    }

    private void verifyGridAutoSimulationPreview(final Stage stage) {
        resetToBlankWorkspace();
        qregSizeField.setText("4");
        cregSizeField.setText("4");
        autoSimulationBox.setSelected(true);
        gateBox.setValue("H");
        appendFromGridClick("q[0]");
        gateBox.setValue("RX");
        angleField.setText(Double.toString(Math.PI / 2.0));
        appendFromGridClick("q[1]");
        setInspectionStep(0);
        final String hOnlySimulation = simulationArea.getText();
        fireVisibleButton(
            stage,
            uiText("inspectNext")
        );
        if (
            !simulationArea.getText().contains("0010")
            || !simulationArea.getText().contains("0011")
            || hOnlySimulation.equals(simulationArea.getText())
        ) {
            throw new IllegalStateException("Grid auto simulation preview did not update after Next: "
                + simulationArea.getText()
                + System.lineSeparator()
                + "Previous simulation: "
                + hOnlySimulation
                + System.lineSeparator()
                + "Diagnostics: "
                + diagnosticsArea.getText()
                + System.lineSeparator()
                + "Status: "
                + statusLabel.getText());
        }
    }

    private void runUiSmokeAfterAsyncActions(final Stage stage) {
        loadAuditFixtureWithoutRefresh("dense-spectrum");
        refreshWorkspace();
        applyNativeWorkflowResult(simulateNative());
        final PauseTransition layoutDelay = new PauseTransition(Duration.seconds(0.8));
        layoutDelay.setOnFinished(event -> {
            try {
                selectTabByText(
                    stage,
                    "Q-Sphere"
                );
                verifyQSphereRendered(stage);
                verifyCircuitHasRenderedCells(stage);
                verifyLargeQSphereIsBounded(stage);
                finishUiSmoke(stage);
            } catch (final RuntimeException exception) {
                exception.printStackTrace(System.err);
                Platform.exit();
                System.exit(2);
            }
        });
        layoutDelay.play();
    }

    private void verifyLargeQSphereIsBounded(final Stage stage) {
        loadAuditFixtureWithoutRefresh("qft16");
        refreshWorkspace();
        setInspectionStep(218);
        applyNativeWorkflowResult(simulateNative());
        selectTabByText(
            stage,
            "Q-Sphere"
        );
        verifyQSphereRendered(stage);
        verifyQSphereMarkerCountIsBounded(qSphereView.node());
        verifyQSphereSummaryBadge(qSphereView.node());
        verifyQSphereHasInteractionHandlers(qSphereView.node());
    }

    private void finishUiSmoke(final Stage stage) {
        final PauseTransition finishDelay = new PauseTransition(Duration.seconds(2.5));
        finishDelay.setOnFinished(event -> {
            stage.close();
            Platform.exit();
        });
        finishDelay.play();
    }

    private void refreshReferenceBoxes() {
        final List<String> qubits = new ArrayList<>();
        final String qreg = qregNameField.getText().isBlank()
            ? "q"
            : qregNameField.getText();
        for (int i = 0; i < Math.max(1, positiveIntegerOrOne(qregSizeField.getText())); i++) {
            qubits.add(qreg + "[" + i + "]");
        }
        final List<String> bits = new ArrayList<>();
        final String creg = cregNameField.getText().isBlank()
            ? "c"
            : cregNameField.getText();
        for (int i = 0; i < Math.max(1, positiveIntegerOrOne(cregSizeField.getText())); i++) {
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
        return positiveIntegerOrOne(shotsField.getText());
    }

    private long seed() {
        return positiveLongOrDefault(
            seedField.getText(),
            7L
        );
    }

    private double angle() {
        return doubleOrDefault(
            angleField.getText(),
            Math.PI / 2.0
        );
    }

    private double secondAngle() {
        return doubleOrDefault(
            secondAngleField.getText(),
            0.0
        );
    }

    private double thirdAngle() {
        return doubleOrDefault(
            thirdAngleField.getText(),
            0.0
        );
    }

    private double durationValue() {
        return doubleOrDefault(
            durationField.getText(),
            20.0
        );
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
            final Tab tab = expertOnlyTabs.get(i);
            if (resultTabPane == null) {
                tab.setDisable(!expert);
            } else if (expert) {
                if (!resultTabPane.getTabs().contains(tab)) {
                    resultTabPane.getTabs().add(tab);
                }
                tab.setDisable(false);
            } else {
                resultTabPane.getTabs().remove(tab);
            }
        }
        builderHintLabel.setText(expert
            ? uiText("expertHint")
            : uiText("beginnerHint"));
        if (overviewArea != null) {
            try {
                final QuantumProgram program = buildNativeProgram();
                final QuantumIrWriteResult writeResult = workspaceService.writeJson(program);
                final ValidationResult validation = workspaceService.validate(program);
                final ResourceEstimate resources = workspaceService.resources(
                    program,
                    24
                );
                final CapabilityPreflightResult preflight = workspaceService.preflight(
                    program,
                    targetFormatBox.getValue()
                );
                refreshOverviewPanel(
                    writeResult,
                    validation,
                    resources,
                    preflight.status().name(),
                    preflight.diagnostics().size()
                );
            } catch (final Exception ignored) {
                // Обзор обновится после того, как рабочее пространство станет валидным.
            }
        }
    }

    @FunctionalInterface
    private interface DesktopWorkflow {

        DesktopWorkflowResult run() throws Exception;
    }
}
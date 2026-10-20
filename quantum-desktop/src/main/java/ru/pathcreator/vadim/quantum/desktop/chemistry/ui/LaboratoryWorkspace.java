/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratorySampleCatalog;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionStage;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEditor;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeSketchService;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeSkeletonHydrogens;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.desktop.ui.extension.DesktopWorkspaceExtension;

public final class LaboratoryWorkspace implements DesktopWorkspaceExtension {

  private final ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure
          .LaboratoryViewPreferences
      viewPreferences =
          new ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure
              .LaboratoryViewPreferences();
  private final javafx.animation.PauseTransition viewSave =
      new javafx.animation.PauseTransition(javafx.util.Duration.millis(400));
  private boolean viewReady;
  private boolean viewDirty;
  private boolean fitPending;
  private String focusPending;
  private ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing
      displayedDocument;
  private final LaboratoryEditor editor = new LaboratoryEditor();
  private final MoleculeSketchService sketch = new MoleculeSketchService();
  private final LaboratorySelection selection = new LaboratorySelection(this.editor);
  private final BorderPane root = new BorderPane();
  private final Label status = new Label();
  private final Label documentTitle = new Label("Новый опыт");
  private LaboratoryStartPane startPane;
  private LaboratoryDrawingGuide drawingGuide;
  private final VBox hydrogenNotice = new VBox(4);
  private Button iupacButton;
  private Button renameButton;
  private String startDocument;
  private boolean startDismissed;
  private final BorderPane workArea = new BorderPane();
  private final TabPane properties = new TabPane();
  private final LaboratoryFocus keyboardFocus;
  private Node beforeFileFocus;
  private Node archivePanel;
  private final ToggleButton folders = new ToggleButton("Папки");
  private final ToggleButton info = new ToggleButton("Записи и свойства");
  private boolean wantsFolders, wantsProperties, propertiesPreferred = true, arrangingPanels;
  private final ToggleButton together = new ToggleButton("2D + 3D");
  private final ToggleButton drawingSpace = new ToggleButton("Больше места");
  private boolean drawingFocused, manualDrawingFocus, automaticFocusSuppressed, compactFocusRegion;
  private final javafx.scene.control.TitledPane componentPanel =
      new javafx.scene.control.TitledPane();
  private final java.util.Map<String, ToggleButton> toolButtons = new java.util.LinkedHashMap<>();
  private String drawElement = "C";
  private ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType drawBond =
      ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType.SINGLE;
  private int ringSize = 6;
  private boolean aromaticRing;
  private final LaboratoryDetailsPane details = new LaboratoryDetailsPane(this::status);
  private final LaboratoryInspector inspector =
      new LaboratoryInspector(() -> this.run(() -> {}), this::status);
  private final LaboratoryCanvas canvas = new LaboratoryCanvas(this::gesture);
  private final TableView<LaboratoryComponent> components = new TableView<>();
  private LaboratoryComponentsPane componentView;
  private boolean componentsPage;
  private final LaboratoryFiles files =
      new LaboratoryFiles(
          this.editor, this::flushAll, this::reload, this::pending, this::saved, this::refresh);
  private final TabPane drawingTabs = new TabPane();
  private final LaboratoryReactionPane reaction =
      new LaboratoryReactionPane(
          this.editor,
          this.files,
          this::run,
          id -> this.run(() -> this.chooseComponent(id, true)),
          this::openStageQuantities,
          this::openQuantities,
          this::openStage);
  private final LaboratoryArchiveBrowser archive =
      new LaboratoryArchiveBrowser(
          this.files::open,
          this.files::error,
          path -> {
            this.folder = path;
            this.files.preferredDirectory(path);
          });
  private final LaboratoryJournalPane journal =
      new LaboratoryJournalPane(this.editor, this::run, this::status);
  private final LaboratoryAttachmentPane attachments =
      new LaboratoryAttachmentPane(this.editor, this.files);
  private final LaboratoryGeometryPane geometry =
      new LaboratoryGeometryPane(
          atom ->
              this.run(
                  () -> {
                    if (this.selected != null) {
                      this.selectedBond = null;
                      this.selected = new LaboratoryCanvas.Hit(this.selected.componentId(), atom);
                      this.focusPending = this.selected.componentId();
                    }
                  }));
  private final Button amountButton = button("Количества", this::quantifySelected);
  private final Button stereoButton = button("Стерео", () -> this.run(this::stereo));
  private final Button focusButton =
      button(
          "К выбранному",
          () ->
              this.run(
                  () -> {
                    if (this.selected != null) {
                      this.focusPending = this.selected.componentId();
                      this.fitPending = false;
                      this.drawingTabs.getSelectionModel().selectFirst();
                    }
                  }));
  private final java.util.List<Button> transformButtons = new java.util.ArrayList<>();
  private final Button undo = button("Отменить", () -> this.history(false));
  private final Button redo = button("Повторить", () -> this.history(true));
  private LaboratoryCanvas.Hit selected;
  private LaboratoryCanvas.BondHit selectedBond;
  private final java.util.Map<String, ToggleButton> elementButtons =
      new java.util.LinkedHashMap<>();
  private String tool = "Выбор";
  private Window owner;
  private Path folder;
  private final ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure
          .LaboratoryNotebookRepository
      notebookRepository =
          new ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure
              .LaboratoryNotebookRepository();
  private LaboratoryNotebookPage.ViewState notebookView;
  private boolean refreshing;
  private String recoveryStatus = "";
  private final LaboratoryRecovery recovery =
      new LaboratoryRecovery(
          this.files,
          this::captureDraft,
          this::restoreDraft,
          message -> {
            this.recoveryStatus = message;
            this.status();
          },
          this::canReplaceDocument);
  private boolean recoveryAttached;
  private boolean startupInteracted;
  private LaboratoryTextEditor textPage;
  private LaboratoryInventoryWindow materialsPage;
  private LaboratoryQuantificationForm quantityPage;
  private LaboratoryStoichiometryWindow stoichiometryPage;
  private LaboratoryProjectWindow projectPage;
  private LaboratoryStageForm stagePage;
  private LaboratoryStereoForm stereoPage;
  private LaboratoryStructurePage structurePage;
  private LaboratoryLibraryWindow libraryPage;
  private LaboratoryComputationPage computationPage;

  private record ReturnTarget(Node page, String experimentId, Runnable action) {}

  private ReturnTarget navigationReturn;
  private ReturnTarget drawingReturn;
  private String drawingStageId;
  private final Button drawingConditions = button("Участники и условия", this::drawingConditions);
  private LaboratoryCanvas.Hit scaffoldTarget;
  private boolean calculationArchivePage;
  private Runnable attention = () -> {};
  private Node beforeFileActivity;
  private javafx.scene.layout.StackPane fileActivity;
  private boolean fileTopDisabled, fileCenterDisabled;
  private final Label fileProblem = new Label();
  private final javafx.scene.layout.VBox fileProblems = new javafx.scene.layout.VBox(6);

  public void onAttention(final Runnable select) {
    this.attention = java.util.Objects.requireNonNull(select);
  }

  public LaboratoryWorkspace() {
    this.files.activity(this::fileActivity);
    this.files.problems(this::fileProblem);
    this.reaction.classification(this::classification);
    this.reaction.route(this::route);
    this.reaction.desk(this::desk);
    this.reaction.procedure(this::procedure);
    this.reaction.preparation(this::prepareStock);
    this.reaction.prediction(this::predictStage);
    this.reaction.exchange(stage -> this.reactionExchange(true, stage.id()));
    this.reaction.importReaction(() -> this.reactionExchange(false, null));
    this.files.history(this::history);
    this.files.attachmentExport(
        (attachment, destination) -> {
          if (this.computationPage != null) {
            this.computationPage.dispose();
          }
          this.computationPage =
              new LaboratoryAttachmentExportPage(
                  this.files.assetPath(), attachment, destination, this::closeComputation);
          this.openComputation();
        });
    this.files.attachmentImport(
        paths -> {
          this.computationPage =
              new LaboratoryAttachmentImportPage(this.editor, paths, this::closeComputation);
          this.openComputation();
        });
    this.files.attachmentPreview(
        attachment -> {
          if (this.computationPage != null) {
            this.computationPage.dispose();
          }
          this.computationPage =
              new LaboratoryAttachmentPage(
                  this.files.assetPath(),
                  this.editor.document().experiment().attachments(),
                  attachment,
                  this.files::extract,
                  preview -> {
                    this.computationPage.dispose();
                    this.computationPage = null;
                    this.files.openPreview(preview);
                  },
                  this::closeComputation);
          this.openComputation();
        });
    this.files.predictionPreview(
        (archive, source) -> {
          this.calculationArchivePage = true;
          this.root.setCenter(
              LaboratoryPredictionWindow.savedView(
                  this.editor, archive, source, this::closeComputation));
          this.root.getTop().setVisible(false);
          this.root.getTop().setManaged(false);
          this.status();
        });
    this.files.calculationPreview(
        (archive, source) -> {
          this.calculationArchivePage = true;
          this.root.setCenter(
              LaboratoryCalculationArchiveWindow.view(
                  this.editor, archive, source, this::closeComputation));
          this.root.getTop().setVisible(false);
          this.root.getTop().setManaged(false);
          this.status();
        });
    this.viewSave.setOnFinished(event -> this.flushView());
    this.root.setId("chemistry-workspace");
    this.root.setPadding(Insets.EMPTY);
    this.root.setTop(this.toolbar());
    this.canvas.contextMenu(this::drawingMenu);
    this.canvas.onRotation(
        (hit, angle) ->
            this.run(
                () -> {
                  this.selected = hit;
                  this.selectedBond = null;
                  this.selection.all(hit.componentId());
                  if (Math.abs(angle) > 1e-8) {
                    this.replace(
                        ru.pathcreator.vadim.quantum.chemistry.drawing.application
                            .MoleculeStereoEditing.rotate(this.layout(), angle));
                  }
                }));
    this.geometry.actions(
        this::prepareGeometry,
        () ->
            this.calculation(
                ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation
                    .ClassicalCalculationKind.GEOMETRY_OPTIMIZATION));
    final BorderPane sketchArea = new BorderPane(this.canvas);
    sketchArea.setMinSize(0, 0);
    this.startPane =
        new LaboratoryStartPane(
            () -> this.desk(null),
            this.files::openDialog,
            this::nameStructure,
            () -> this.exchange(false),
            () -> {
              this.startDismissed = true;
              this.updateStartPane();
              this.chooseTool("Связь");
              this.canvas.getContent().requestFocus();
            });
    this.startPane.projects(this::notebooks);
    sketchArea.setTop(this.startPane);
    sketchArea.setLeft(this.scrollPalette(this.palette(), "chemistry-tools-scroll"));
    sketchArea.setRight(this.scrollPalette(this.elements(), "chemistry-elements-scroll"));
    this.geometry.componentSelection(id -> this.run(() -> this.chooseComponent(id, false)));
    this.geometry.preferences(this.viewPreferences, this::viewChanged);
    this.geometry.setPadding(new Insets(10));
    final ScrollPane geometryScroll = new ScrollPane(this.geometry);
    geometryScroll.setFitToWidth(true);
    geometryScroll.setFitToHeight(true);
    geometryScroll.setMinSize(0, 0);
    geometryScroll.setId("chemistry-geometry-scroll");
    final Tab structureTab = new Tab("2D · Структура", sketchArea);
    final Tab geometryTab = new Tab("3D · Геометрия", geometryScroll);
    this.drawingTabs.getTabs().setAll(structureTab, new Tab("Реакция", this.reaction), geometryTab);
    final ToggleButton together = this.together;
    final Runnable geometryLayout =
        () ->
            this.geometry.available(
                geometryScroll.getViewportBounds().getWidth(),
                geometryScroll.getViewportBounds().getHeight(),
                together.isSelected());
    geometryScroll
        .viewportBoundsProperty()
        .addListener((observable, before, after) -> geometryLayout.run());
    final boolean[] pairedRelayout = {false};
    final SplitPane paired =
        new SplitPane() {
          @Override
          protected void layoutChildren() {
            pairedRelayout[0] = true;
            try {
              if (this.getOrientation() == Orientation.HORIZONTAL) {
                this.setDividerPositions(
                    LaboratoryWorkspace.this.viewPreferences.fraction("paired.divider", 0.55));
              }
              super.layoutChildren();
            } finally {
              pairedRelayout[0] = false;
            }
          }
        };
    paired.setId("chemistry-paired-panes");
    final Runnable pairedLayout =
        () -> {
          final boolean shortWide =
              paired.getWidth() >= 580
                  && (paired.getHeight() < 440 || this.drawingFocused && paired.getHeight() < 560);
          final var orientation =
              paired.getWidth() < 900 && !shortWide
                  ? javafx.geometry.Orientation.VERTICAL
                  : javafx.geometry.Orientation.HORIZONTAL;
          if (paired.getOrientation() != orientation) {
            pairedRelayout[0] = true;
            try {
              paired.setOrientation(orientation);
              paired.setDividerPositions(
                  orientation == Orientation.VERTICAL
                      ? 0.5
                      : this.viewPreferences.fraction("paired.divider", 0.55));
            } finally {
              pairedRelayout[0] = false;
            }
          }
          if (together.isSelected()) {
            this.canvas.refreshViewport();
          }
        };
    paired.widthProperty().addListener((observable, before, after) -> pairedLayout.run());
    paired.heightProperty().addListener((observable, before, after) -> pairedLayout.run());
    geometryScroll
        .minHeightProperty()
        .bind(
            javafx.beans.binding.Bindings.createDoubleBinding(
                () ->
                    together.isSelected()
                            && paired.getOrientation() == javafx.geometry.Orientation.VERTICAL
                        ? Math.min(
                            Math.max(0, paired.getHeight() - 32),
                            this.geometry.controlsExpandedProperty().get() ? 216 : 170)
                        : 0,
                together.selectedProperty(),
                paired.orientationProperty(),
                paired.heightProperty(),
                this.geometry.controlsExpandedProperty()));
    final double[] collapsedDivider = {0.5};
    this.geometry
        .controlsExpandedProperty()
        .addListener(
            (observable, before, expanded) -> {
              if (together.isSelected()
                  && paired.getOrientation() == javafx.geometry.Orientation.VERTICAL
                  && !paired.getDividers().isEmpty()) {
                if (expanded) {
                  collapsedDivider[0] = paired.getDividerPositions()[0];
                } else {
                  paired.setDividerPositions(collapsedDivider[0]);
                }
              }
            });
    together
        .selectedProperty()
        .addListener(
            (observable, before, enabled) -> {
              this.rememberView("paired", enabled);
              this.automaticFocusSuppressed = false;
              this.layoutSidePanels();
              geometryLayout.run();
              if (enabled) {
                structureTab.setContent(null);
                geometryTab.setContent(null);
                paired.getItems().setAll(sketchArea, geometryScroll);
                paired.setDividerPositions(this.viewPreferences.fraction("paired.divider", 0.55));
                paired
                    .getDividers()
                    .getFirst()
                    .positionProperty()
                    .addListener(
                        (value, oldPosition, position) -> {
                          if (!pairedRelayout[0]
                              && paired.getOrientation() == javafx.geometry.Orientation.HORIZONTAL
                              && position.doubleValue() >= 0.2
                              && position.doubleValue() <= 0.8) {
                            this.rememberView("paired.divider", position);
                          }
                        });
                structureTab.setContent(paired);
                pairedLayout.run();
              } else {
                paired.getItems().clear();
                structureTab.setContent(sketchArea);
                geometryTab.setContent(geometryScroll);
              }
            });
    this.drawingTabs
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (observable, before, tab) -> {
              if (tab == geometryTab) {
                together.setSelected(false);
              }
              geometryLayout.run();
              this.layoutSidePanels();
              this.rememberView(
                  "tab", tab == geometryTab ? "3d" : tab == structureTab ? "2d" : "reaction");
              if (tab == structureTab) {
                if (this.focusPending != null) {
                  this.focusSelectedComponent();
                } else if (this.fitPending) {
                  this.fitOpenedDocument();
                }
              }
            });
    final ToggleButton names = new ToggleButton("Названия");
    names.setId("chemistry-show-names");
    names.setTooltip(
        new javafx.scene.control.Tooltip(
            "Показать названия под молекулами. Меняет только вид листа."));
    names.setSelected(this.viewPreferences.flag("names", false));
    this.canvas.showNames(names.isSelected());
    names.setOnAction(
        event -> {
          this.canvas.showNames(names.isSelected());
          this.rememberView("names", names.isSelected());
        });
    final ToggleButton hydrogens = new ToggleButton("Все H");
    hydrogens.setId("chemistry-show-carbon-hydrogens");
    hydrogens.setSelected(this.viewPreferences.flag("carbonHydrogens", false));
    this.selection.compactHydrogens(!hydrogens.isSelected());
    this.canvas.showCarbonHydrogens(hydrogens.isSelected());
    hydrogens.setTooltip(
        new javafx.scene.control.Tooltip(
            "Показать водороды связями на рисунке. Они учитываются в формуле и молярной массе при любом виде. В компактном виде C–H скрыты, N–H и O–H показаны группами; изотопные H и стереосвязи видны всегда."));
    hydrogens.setOnAction(
        event -> {
          this.selection.compactHydrogens(!hydrogens.isSelected());
          this.canvas.showCarbonHydrogens(hydrogens.isSelected());
          this.rememberView("carbonHydrogens", hydrogens.isSelected());
        });
    final Label scale = new Label("100%");
    final double[] zoom = {1};
    this.canvas.zoomChanged(
        value -> {
          zoom[0] = value;
          scale.setText(Math.round(value * 100) + "%");
        });
    final Button zoomOut =
        button(
            "−",
            () -> {
              zoom[0] = this.canvas.zoom(zoom[0] / 1.2);
              scale.setText(Math.round(zoom[0] * 100) + "%");
            });
    final Button zoomIn =
        button(
            "+",
            () -> {
              zoom[0] = this.canvas.zoom(zoom[0] * 1.2);
              scale.setText(Math.round(zoom[0] * 100) + "%");
            });
    final Button arrange =
        button(
            "Разместить",
            () -> {
              this.run(() -> this.editor.arrange(this.canvas.getViewportBounds().getWidth()));
              this.canvas.fit();
            });
    arrange.setId("chemistry-arrange-components");
    arrange.setTooltip(
        new javafx.scene.control.Tooltip(
            "Разместить компоненты без перекрытий. Форма молекул, стереохимия и 3D сохранятся. Отмена — Ctrl+Z."));
    final Button fit =
        button(
            "Вписать",
            () -> {
              zoom[0] = this.canvas.fit();
              scale.setText(Math.round(zoom[0] * 100) + "%");
            });
    this.focusButton.setId("chemistry-focus-component");
    this.focusButton.setTooltip(
        new javafx.scene.control.Tooltip(
            "Показать выбранный компонент в 2D без изменения его координат"));
    together
        .selectedProperty()
        .addListener(
            (observable, before, enabled) -> {
              javafx.application.Platform.runLater(
                  () -> {
                    if (together.isSelected() == enabled
                        && this.drawingTabs.getSelectionModel().getSelectedIndex() == 0) {
                      this.root.applyCss();
                      this.root.layout();
                      this.canvas.refreshViewport();
                    }
                  });
            });
    zoomOut.setTooltip(new javafx.scene.control.Tooltip("Уменьшить рисунок · Ctrl + колесо мыши"));
    zoomIn.setTooltip(new javafx.scene.control.Tooltip("Увеличить рисунок · Ctrl + колесо мыши"));
    final Button check = button("Проверить", () -> this.run(this::checkStructure));
    check.setTooltip(
        new javafx.scene.control.Tooltip(
            "Проверить связи, заряды и электронное состояние структуры"));
    together.setId("chemistry-paired-view");
    final Button help = button("Помощь", () -> LaboratoryDrawingHelp.show(this.owner));
    help.setTooltip(
        new javafx.scene.control.Tooltip("Построение, выделение, горячие клавиши и работа с 3D"));
    final javafx.scene.control.MenuButton view = new javafx.scene.control.MenuButton("Вид");
    view.setId("chemistry-view-menu");
    final javafx.scene.control.CheckMenuItem showNames =
        new javafx.scene.control.CheckMenuItem("Названия молекул");
    showNames.selectedProperty().bindBidirectional(names.selectedProperty());
    showNames.setOnAction(
        event -> {
          this.canvas.showNames(showNames.isSelected());
          this.rememberView("names", showNames.isSelected());
        });
    final javafx.scene.control.CheckMenuItem showHydrogens =
        new javafx.scene.control.CheckMenuItem("Показать все водороды");
    showHydrogens.selectedProperty().bindBidirectional(hydrogens.selectedProperty());
    showHydrogens.setOnAction(
        event -> {
          this.selection.compactHydrogens(!showHydrogens.isSelected());
          this.canvas.showCarbonHydrogens(showHydrogens.isSelected());
          this.rememberView("carbonHydrogens", showHydrogens.isSelected());
        });
    view.getItems()
        .addAll(
            showNames,
            showHydrogens,
            new javafx.scene.control.SeparatorMenuItem(),
            menu("Разместить компоненты", arrange::fire),
            menu("Проверить структуру", check::fire),
            menu("Стереохимия…", this.stereoButton::fire),
            menu("Количества…", this.amountButton::fire),
            menu(
                "Автоматический учёт H для выбранной структуры",
                () ->
                    this.run(
                        () -> {
                          if (this.selected == null) {
                            this.status.setText(
                                "Выберите атом структуры, для которой нужно включить автоматический учёт H.");
                            return;
                          }
                          this.replace(MoleculeSkeletonHydrogens.normalize(this.layout()));
                        })),
            menu("Водороды вручную…", () -> this.run(() -> this.structure("hydrogens"))),
            menu("Атомы и связи…", () -> this.run(() -> this.structure("atoms"))),
            menu("Названия и идентификаторы…", () -> this.run(() -> this.structure("identity"))),
            new javafx.scene.control.SeparatorMenuItem(),
            menu("Помощь по построению", help::fire));
    this.focusButton.setText("К выбору");
    final ToggleGroup modes = new ToggleGroup();
    final javafx.scene.layout.HBox modeButtons = new javafx.scene.layout.HBox(2);
    final String[] labels = {"2D", "Реакция", "3D", "2D + 3D"};
    for (int index = 0; index < labels.length; index++) {
      final int tabIndex = index;
      final ToggleButton mode = new ToggleButton(labels[index]);
      mode.setId("chemistry-mode-" + index);
      mode.setToggleGroup(modes);
      mode.setSelected(index == 0);
      mode.setOnAction(
          event -> {
            if (tabIndex == 0 || tabIndex == 2) {
              together.setSelected(false);
            } else if (tabIndex == 3) {
              together.setSelected(true);
            }
            this.drawingTabs.getSelectionModel().select(tabIndex == 3 ? 0 : tabIndex);
            mode.setSelected(true);
          });
      final Runnable selectedMode =
          () -> {
            final int selectedTab = this.drawingTabs.getSelectionModel().getSelectedIndex();
            mode.setSelected(
                tabIndex == 3
                    ? selectedTab == 0 && together.isSelected()
                    : selectedTab == tabIndex && (tabIndex != 0 || !together.isSelected()));
          };
      this.drawingTabs
          .getSelectionModel()
          .selectedIndexProperty()
          .addListener((value, before, after) -> selectedMode.run());
      together.selectedProperty().addListener((value, before, after) -> selectedMode.run());
      modeButtons.getChildren().add(mode);
    }
    together.setOnAction(event -> this.drawingTabs.getSelectionModel().selectFirst());
    final javafx.scene.layout.HBox zoomControls =
        new javafx.scene.layout.HBox(2, zoomOut, scale, zoomIn);
    zoomControls.setId("chemistry-zoom-controls");
    zoomControls.setAlignment(javafx.geometry.Pos.CENTER);
    zoomControls.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
    final Button scaffolds = button("Каркасы", () -> this.scaffolds(null));
    scaffolds.setId("chemistry-scaffolds-open");
    scaffolds.setTooltip(
        new javafx.scene.control.Tooltip(
            "Конденсированные, мостиковые и спироциклы. Для присоединения — правый щелчок по атому."));
    this.renameButton =
        button(
            "Название…",
            () ->
                this.run(
                    () -> {
                      if (this.selected == null) {
                        return;
                      }
                      LaboratoryComponentNaming.show(
                          this.editor, this.selected.componentId(), this.owner);
                    }));
    this.renameButton.setId("chemistry-rename-component");
    this.renameButton.setTooltip(
        new javafx.scene.control.Tooltip(
            "Задать своё название вещества без интернета. Полное имя сохраняется; длинные подписи сокращаются только на экране."));
    this.iupacButton =
        button(
            "Название IUPAC",
            () ->
                this.run(
                    () -> {
                      if (this.selected == null) {
                        this.status.setText(
                            "Выберите структуру на рисунке для поиска названия IUPAC.");
                        return;
                      }
                      if (this.legacyHydrogenCandidates().contains(this.selected.componentId())) {
                        this.status.setText(
                            "Сначала нажмите «Учесть водороды» над листом, чтобы искать название по полной структуре.");
                        return;
                      }
                      this.showStructure(this.selected.componentId(), "identity");
                      ((LaboratoryIdentityPage) this.structurePage).findIupac();
                    }));
    this.iupacButton.setId("chemistry-iupac-lookup");
    this.iupacButton.setTooltip(
        new javafx.scene.control.Tooltip(
            "Найти название выбранной структуры в PubChem через интернет. Результат проверяется по структуре; название опыта не меняется."));
    final Button predictedNmr = button("ЯМР · прогноз", this::predictedNmr);
    predictedNmr.setId("chemistry-open-nmr-model");
    predictedNmr.setTooltip(
        new javafx.scene.control.Tooltip(
            "Офлайн-прогноз ¹H/¹³C, растворитель и смесь с выбранными примесями."));
    final java.util.List<Node> drawingTools =
        new java.util.ArrayList<>(
            java.util.List.of(
                scaffolds,
                hydrogens,
                this.renameButton,
                this.iupacButton,
                predictedNmr,
                fit,
                zoomControls));
    final var transforms =
        ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeStereoEditing.Transform
            .values();
    final String[] icons = {"rotate", "rotate-left", "flip-x", "flip-y"};
    final String[] hints = {
      "Повернуть компонент на 90° вправо",
      "Повернуть компонент на 90° влево",
      "Отразить компонент по горизонтали · стереохимия сохраняется",
      "Отразить компонент по вертикали · стереохимия сохраняется"
    };
    final int[] transformIndices = {0, 1, 3, 4};
    for (int index = 0; index < icons.length; index++) {
      final var operation = transforms[transformIndices[index]];
      final Button transform = button(hints[index], () -> this.transform(operation));
      this.icon(transform, icons[index], hints[index]);
      transform.setId("chemistry-" + icons[index]);
      this.transformButtons.add(transform);
      drawingTools.add(transform);
      transform.visibleProperty().bind(transform.disableProperty().not());
      transform.managedProperty().bind(transform.visibleProperty());
    }
    this.drawingTabs
        .getSelectionModel()
        .selectedIndexProperty()
        .addListener(
            (value, before, after) -> {
              this.transformButtons.forEach(
                  button -> button.setDisable(this.selected == null || after.intValue() != 0));
              this.iupacButton.setDisable(this.selected == null || after.intValue() != 0);
              this.renameButton.setDisable(this.selected == null || after.intValue() != 0);
              this.updateHydrogenNotice();
            });
    drawingTools.forEach(
        node -> {
          if (!this.transformButtons.contains(node)
              && node != this.focusButton
              && node != this.iupacButton
              && node != this.renameButton) {
            node.disableProperty()
                .bind(this.drawingTabs.getSelectionModel().selectedIndexProperty().isNotEqualTo(0));
          }
        });
    this.manualDrawingFocus = this.viewPreferences.flag("drawingFocus", false);
    this.drawingSpace.setId("chemistry-focus-layout");
    this.drawingSpace.setTooltip(
        new javafx.scene.control.Tooltip(
            "Место для молекулы: временно скрыть панели, сохранив поля и выбор. Escape возвращает панели. В тесном совместном виде включается автоматически."));
    this.drawingSpace.setOnAction(
        event -> {
          this.manualDrawingFocus = this.drawingSpace.isSelected();
          this.automaticFocusSuppressed = !this.manualDrawingFocus;
          this.rememberView("drawingFocus", this.manualDrawingFocus);
          this.layoutSidePanels();
        });
    final Button componentTable = button("Компоненты", () -> this.showComponents(true));
    componentTable.visibleProperty().bind(this.drawingSpace.selectedProperty());
    componentTable.managedProperty().bind(componentTable.visibleProperty());
    componentTable.setId("chemistry-open-components");
    componentTable.setTooltip(
        new javafx.scene.control.Tooltip("Развернуть таблицу компонентов, количеств и продуктов"));
    final javafx.scene.control.CheckMenuItem pairedView =
        new javafx.scene.control.CheckMenuItem("2D и 3D рядом");
    pairedView.selectedProperty().bindBidirectional(together.selectedProperty());
    pairedView.setOnAction(event -> this.drawingTabs.getSelectionModel().selectFirst());
    final javafx.scene.control.CheckMenuItem focusedView =
        new javafx.scene.control.CheckMenuItem("Скрыть боковые панели");
    focusedView.selectedProperty().bindBidirectional(this.drawingSpace.selectedProperty());
    focusedView.setOnAction(
        event -> {
          this.manualDrawingFocus = focusedView.isSelected();
          this.automaticFocusSuppressed = !this.manualDrawingFocus;
          this.rememberView("drawingFocus", this.manualDrawingFocus);
          this.layoutSidePanels();
        });
    view.getItems()
        .addAll(
            new javafx.scene.control.SeparatorMenuItem(),
            pairedView,
            focusedView,
            menu("Показать выбранный компонент", this.focusButton::fire));
    final javafx.scene.layout.FlowPane viewBar =
        new javafx.scene.layout.FlowPane(6, 3, modeButtons, view, componentTable);
    drawingTools.forEach(
        node -> {
          if (!this.transformButtons.contains(node)) {
            node.visibleProperty()
                .bind(this.drawingTabs.getSelectionModel().selectedIndexProperty().isEqualTo(0));
            node.managedProperty().bind(node.visibleProperty());
          }
        });
    together
        .visibleProperty()
        .bind(this.drawingTabs.getSelectionModel().selectedIndexProperty().isNotEqualTo(1));
    together.managedProperty().bind(together.visibleProperty());
    viewBar.getChildren().addAll(drawingTools);
    viewBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    viewBar.setPadding(new Insets(5, 10, 5, 10));
    viewBar.getStyleClass().add("laboratory-view-bar");

    this.drawingTabs.setId("chemistry-drawing-tabs");
    this.drawingTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    this.componentPanel.setText("Компоненты · план и факт");

    this.componentPanel.setExpanded(this.viewPreferences.flag("components", false));
    this.componentPanel
        .expandedProperty()
        .addListener(
            (observable, before, value) -> {
              this.rememberView("components", value);
              this.layoutSidePanels();
              if (value && this.componentView != null) {
                this.componentView.revealSelected();
              }
            });
    this.componentPanel.setAnimated(false);
    this.components.setMinHeight(70);
    this.components
        .prefHeightProperty()
        .bind(
            javafx.beans.binding.Bindings.max(
                80,
                javafx.beans.binding.Bindings.min(
                    180, this.workArea.heightProperty().multiply(.30))));
    this.componentPanel.setId("chemistry-component-panel");
    final BorderPane center = new BorderPane(this.drawingTabs);
    this.drawingGuide = new LaboratoryDrawingGuide(() -> LaboratoryDrawingHelp.show(this.owner));
    this.drawingGuide
        .visibleProperty()
        .bind(this.drawingTabs.getSelectionModel().selectedIndexProperty().isEqualTo(0));
    this.drawingGuide.managedProperty().bind(this.drawingGuide.visibleProperty());
    this.hydrogenNotice.setId("chemistry-hydrogen-notice");
    this.hydrogenNotice.setPadding(new Insets(6, 12, 6, 12));
    this.hydrogenNotice.getStyleClass().add("laboratory-drawing-guide");
    this.hydrogenNotice.managedProperty().bind(this.hydrogenNotice.visibleProperty());
    center.setTop(new VBox(viewBar, this.hydrogenNotice, this.drawingGuide));
    center.setBottom(this.componentPanel);
    this.properties
        .getTabs()
        .setAll(
            new Tab("Опыт", this.scrolled(this.details)),
            new Tab("Свойства", this.scrolled(new VBox(8, this.identityButton(), this.inspector))),
            new Tab("Журнал", this.scrolled(this.journal)),
            new Tab("Файлы", this.scrolled(this.attachments)));
    this.properties.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    this.properties.setId("chemistry-properties-tabs");
    this.properties.setMinWidth(200);
    this.properties.setPrefWidth(320);
    this.archivePanel = this.sidebar();
    this.workArea.setMinSize(0, 0);
    this.workArea
        .widthProperty()
        .addListener((observable, before, after) -> this.layoutSidePanels());
    this.workArea
        .heightProperty()
        .addListener((observable, before, after) -> this.layoutSidePanels());
    this.geometry
        .controlsExpandedProperty()
        .addListener((observable, before, after) -> this.layoutSidePanels());
    this.layoutSidePanels();
    this.workArea.setCenter(center);
    this.root.setCenter(this.workArea);
    this.status.setId("chemistry-status");
    this.status.setMaxWidth(Double.MAX_VALUE);
    this.fileProblem.setWrapText(true);
    this.fileProblem.setId("chemistry-file-error-message");
    final Button dismissProblem = new Button("Закрыть сообщение");
    dismissProblem.setId("chemistry-file-error-dismiss");
    dismissProblem.setOnAction(
        event -> {
          this.fileProblems.setVisible(false);
          this.fileProblems.setManaged(false);
          this.files.clearProblem();
          this.status();
        });
    this.fileProblems.getChildren().setAll(this.fileProblem, dismissProblem);
    this.fileProblems.setId("chemistry-file-error");
    this.fileProblems.setPadding(new Insets(10));
    this.fileProblems.setVisible(false);
    this.fileProblems.setManaged(false);
    this.root.setBottom(new javafx.scene.layout.VBox(this.fileProblems, this.status));
    this.table();
    together.setSelected(this.viewPreferences.flag("paired", false));
    final String tab = this.viewPreferences.choice("tab", "2d", "2d", "3d", "reaction");
    this.drawingTabs
        .getSelectionModel()
        .select(tab.equals("3d") ? 2 : tab.equals("reaction") ? 1 : 0);
    this.viewReady = true;
    this.keyboardFocus = new LaboratoryFocus(this.root, this.workArea, this.canvas.getContent());
    this.reload();
    this.recovery.onDraftsChanged(
        count -> {
          if (this.computationPage instanceof LaboratoryWelcomePage page) {
            page.pendingDrafts(count);
          }
        });
    this.welcome();
  }

  public String id() {
    return "chemistry";
  }

  public String title() {
    return "Химия";
  }

  public Node content(final Window window) {
    this.owner = window;
    this.files.owner(window);
    if (!this.recoveryAttached) {
      this.recoveryAttached = true;
      window
          .showingProperty()
          .addListener(
              (observable, before, showing) -> {
                if (showing) {
                  this.recovery.start(this::resumeSession);
                } else {
                  this.flushView();
                  this.recovery.closed();
                  this.archive.dispose();
                }
              });
      if (window.isShowing()) {
        this.recovery.start(this::resumeSession);
      }
    }
    return this.root;
  }

  private void resumeSession() {
    if (this.startupInteracted
        || !(this.computationPage instanceof LaboratoryWelcomePage)
        || this.files.decisionClosePending()
        || this.recovery.pendingDrafts() > 0) {
      return;
    }
    final String component = this.files.session().component();
    if (this.files.restoreLast()) {
      if (!component.isBlank()) {
        this.chooseComponent(component, false);
        this.refresh();
      }
    }
    this.status();
  }

  private void fileActivity(final Node content) {
    if (content == null) {
      if (this.fileActivity != null && this.root.getCenter() == this.fileActivity) {
        this.root.setCenter(this.beforeFileActivity);
      }
      if (this.beforeFileActivity != null) {
        this.beforeFileActivity.setDisable(this.fileCenterDisabled);
      }
      if (this.root.getTop() != null) {
        this.root.getTop().setDisable(this.fileTopDisabled);
      }
      this.fileActivity = null;
      this.beforeFileActivity = null;
      this.keyboardFocus.request(this.root.getCenter(), this.beforeFileFocus);
      this.beforeFileFocus = null;
      return;
    }
    this.fileProblems.setVisible(false);
    this.fileProblems.setManaged(false);
    this.files.clearProblem();
    this.status();
    this.beforeFileFocus = this.keyboardFocus.capture();
    this.beforeFileActivity = this.root.getCenter();
    this.fileCenterDisabled =
        this.beforeFileActivity != null && this.beforeFileActivity.isDisable();
    this.fileTopDisabled = this.root.getTop() != null && this.root.getTop().isDisable();
    if (this.beforeFileActivity != null) {
      this.beforeFileActivity.setDisable(true);
    }
    if (this.root.getTop() != null) {
      this.root.getTop().setDisable(true);
    }
    this.fileActivity = new javafx.scene.layout.StackPane(content);
    this.root.setCenter(this.fileActivity);
  }

  private void fileProblem(final Exception failure) {
    final String text =
        failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
    this.fileProblem.setText(text.length() > 600 ? text.substring(0, 600) + "…" : text);
    this.fileProblem.setTooltip(new javafx.scene.control.Tooltip(text));
    this.fileProblems.setVisible(true);
    this.fileProblems.setManaged(true);
    this.status();
  }

  private boolean canReplaceDocument() {
    if (this.files.busy()) {
      return false;
    }
    return (this.textPage == null || this.textPage.canClose())
        && (this.quantityPage == null || this.quantityPage.canClose())
        && (this.projectPage == null || this.projectPage.canClose())
        && (this.stagePage == null || this.stagePage.canClose())
        && (this.stereoPage == null || this.stereoPage.canClose())
        && (this.structurePage == null || this.structurePage.canClose())
        && (this.libraryPage == null || this.libraryPage.canClose())
        && (this.computationPage == null || this.computationPage.canClose())
        && (this.stoichiometryPage == null || this.stoichiometryPage.canClose())
        && (this.materialsPage == null || this.materialsPage.canClose())
        && this.files.requestClose();
  }

  public boolean requestClose() {
    this.startupInteracted = true;
    if (this.files.cancelDecisionForClose(
        () -> {
          if (this.owner != null && this.owner.isShowing()) {
            this.owner.fireEvent(
                new javafx.stage.WindowEvent(
                    this.owner, javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST));
          }
        })) {
      return false;
    }
    if (!this.archive.canClose() || !this.canReplaceDocument() || !this.recovery.finishClose()) {
      return false;
    }
    this.archive.dispose();
    if (this.textPage != null) {
      this.textPage.dispose();
    }
    if (this.structurePage != null) {
      this.structurePage.dispose();
    }
    if (this.computationPage != null) {
      this.computationPage.dispose();
    }
    if (this.libraryPage != null) {
      this.libraryPage.dispose();
    }
    if (this.materialsPage != null) {
      this.materialsPage.dispose();
    }
    if (this.projectPage != null) {
      this.projectPage.dispose();
    }
    this.files.session().component(this.selected == null ? null : this.selected.componentId());
    this.flushView();
    return true;
  }

  private void rememberView(final String key, final Object value) {
    if (!this.viewReady) {
      return;
    }
    this.viewPreferences.set(key, value);
    this.viewChanged();
  }

  private void viewChanged() {
    if (this.viewReady) {
      this.viewDirty = true;
      this.viewSave.playFromStart();
    }
  }

  private void flushView() {
    this.viewSave.stop();
    if (this.viewDirty) {
      this.viewPreferences.save();
      this.viewDirty = !this.viewPreferences.warning().isBlank();
      this.status();
    }
  }

  private void fitOpenedDocument() {
    javafx.application.Platform.runLater(
        () -> {
          if (this.fitPending
              && this.drawingTabs.getSelectionModel().getSelectedIndex() == 0
              && this.root.getScene() != null) {
            this.root.applyCss();
            this.root.layout();
            if (this.canvas.getViewportBounds().getWidth() > 0
                && this.canvas.getViewportBounds().getHeight() > 0) {
              this.canvas.fit();
              this.fitPending = false;
            }
          }
        });
  }

  private void chooseComponent(final String id, final boolean open2D) {
    final var layout = this.editor.document().layouts().get(id);
    if (layout == null || layout.molecule().atoms().isEmpty()) {
      return;
    }
    this.selection.sync(null);
    this.selectedBond = null;
    this.selected = new LaboratoryCanvas.Hit(id, layout.molecule().atoms().getFirst().id());
    this.focusPending = id;
    this.fitPending = false;
    if (open2D) {
      this.drawingTabs.getSelectionModel().selectFirst();
    }
  }

  private void paste() {
    final var center = this.canvas.viewportCenter();
    this.selected = this.selection.paste(center.x(), center.y());
    this.selectedBond = null;
    this.chooseTool("Выбор");
    if (this.selected != null) {
      this.focusPending = this.selected.componentId();
      this.fitPending = false;
      this.drawingTabs.getSelectionModel().selectFirst();
    }
  }

  private void focusSelectedComponent() {
    final String id = this.focusPending;
    if (id == null) {
      return;
    }
    final String experimentId = this.editor.document().experiment().id();
    final var targets =
        this.selection.hits().stream()
            .map(LaboratoryCanvas.Hit::componentId)
            .collect(java.util.stream.Collectors.toSet());
    targets.add(id);
    javafx.application.Platform.runLater(
        () -> {
          if (!id.equals(this.focusPending)
              || !experimentId.equals(this.editor.document().experiment().id())
              || this.drawingTabs.getSelectionModel().getSelectedIndex() != 0
              || this.root.getScene() == null) {
            return;
          }
          this.root.applyCss();
          this.root.layout();
          if (this.canvas.focusComponents(targets)) {
            this.focusPending = null;
            this.fitPending = false;
          }
        });
  }

  public void shortcut(final KeyEvent event) {
    if (!this.files.busy() && event.isShortcutDown() && event.getCode() == KeyCode.K) {
      this.commands();
      event.consume();
      return;
    }
    if (this.files.busy()) {
      if (event.getCode() == KeyCode.ESCAPE) {
        this.files.cancelOperation();
        event.consume();
      } else if (event.isShortcutDown()
          && List.of(KeyCode.N, KeyCode.O, KeyCode.S, KeyCode.F, KeyCode.Z, KeyCode.Y)
              .contains(event.getCode())) {
        event.consume();
      }
      return;
    }
    if (this.componentsPage && this.root.getCenter() == this.componentView) {
      if (event.getCode() == KeyCode.ESCAPE) {
        if (!this.componentView.clearSearch()) {
          this.showComponents(false);
        }
        event.consume();
        return;
      }
      if (event.isShortcutDown() && event.getCode() == KeyCode.F) {
        this.componentView.focusSearch();
        event.consume();
        return;
      }
      if (!(event.isShortcutDown()
          && List.of(KeyCode.N, KeyCode.O, KeyCode.S).contains(event.getCode()))) {
        return;
      }
    }
    if (this.computationPage instanceof LaboratoryExperimentDesk desk
        && event.isShortcutDown()
        && event.getCode() == KeyCode.S) {
      desk.saveRequested(event.isShiftDown());
      event.consume();
      return;
    }
    if ((this.computationPage instanceof LaboratoryExperimentDesk
            || this.computationPage instanceof LaboratoryNotebookPage)
        && event.isShortcutDown()
        && !event.isShiftDown()
        && event.getCode() == KeyCode.O) {
      this.openWorkbenchFile();
      event.consume();
      return;
    }
    if (this.computationPage instanceof LaboratoryNotebookPage notebook
        && notebook.shortcut(event)) {
      event.consume();
      return;
    }
    if (this.computationPage instanceof LaboratoryRoutePage route && route.shortcut(event)) {
      return;
    }
    if (this.computationPage instanceof LaboratoryAnalysisPage
        && event.isShortcutDown()
        && event.getCode() == KeyCode.S) {
      this.saveAnalysis(event.isShiftDown());
      event.consume();
      return;
    }
    if (this.computationPage instanceof LaboratorySamplePage samples
        && event.isShortcutDown()
        && event.getCode() == KeyCode.S) {
      samples.saveRequested();
      event.consume();
      return;
    }
    if (this.materialsPage != null
        || this.quantityPage != null
        || this.stoichiometryPage != null
        || this.textPage != null
        || this.projectPage != null
        || this.stagePage != null
        || this.stereoPage != null
        || this.structurePage != null
        || this.libraryPage != null
        || this.computationPage != null
        || this.calculationArchivePage) {
      if (event.isShortcutDown()
          && (event.getCode() == KeyCode.N
              || event.getCode() == KeyCode.O
              || event.getCode() == KeyCode.S)) {
        event.consume();
      }
      return;
    }
    if (this.archive.owns(event.getTarget())
        && !(event.isShortcutDown()
            && List.of(KeyCode.S, KeyCode.O, KeyCode.N, KeyCode.F).contains(event.getCode()))) {
      this.archive.shortcut(event);
      return;
    }
    if (event.isShortcutDown() && event.getCode() == KeyCode.F) {
      this.openFolders();
      this.archive.focusSearch();
      event.consume();
    } else if (event.isShortcutDown() && event.getCode() == KeyCode.S) {
      this.files.save(event.isShiftDown());
      event.consume();
    } else if (event.isShortcutDown() && !event.isShiftDown() && event.getCode() == KeyCode.O) {
      this.files.openDialog();
      event.consume();
    } else if (event.isShortcutDown() && !event.isShiftDown() && event.getCode() == KeyCode.N) {
      this.files.create();
      event.consume();
    } else if (!LaboratoryFocus.localInput(event.getTarget())) {
      if (event.isShortcutDown()
          && (event.getCode() == KeyCode.Z || event.getCode() == KeyCode.Y)) {
        this.history(event.isShiftDown() || event.getCode() == KeyCode.Y);
        event.consume();
      } else if (event.isShortcutDown() && event.getCode() == KeyCode.A) {
        this.run(
            () -> {
              this.selectedBond = null;
              this.selected = this.selection.all(null);
            });
        event.consume();
      } else if (event.isShortcutDown() && event.getCode() == KeyCode.C) {
        this.run(this.selection::copy);
        event.consume();
      } else if (event.isShortcutDown() && event.getCode() == KeyCode.V) {
        this.run(this::paste);
        event.consume();
      } else if (event.getCode() == KeyCode.ESCAPE) {
        if (LaboratoryFocus.contains(this.canvas, event.getTarget())
            && this.canvas.cancelActiveGesture()) {
          event.consume();
          return;
        }
        if (this.drawingFocused) {
          this.showRequestedPanels();
          this.layoutSidePanels();
          event.consume();
          return;
        }
        this.run(
            () -> {
              this.selected = null;
              this.selectedBond = null;
            });
        event.consume();
      } else if (!event.isShortcutDown() && !event.isAltDown() && event.getCode() == KeyCode.V) {
        this.chooseTool("Выбор");
        event.consume();
      } else if (!event.isShortcutDown()
          && !event.isAltDown()
          && List.of(
                  KeyCode.C, KeyCode.H, KeyCode.N, KeyCode.O, KeyCode.P, KeyCode.S, KeyCode.F,
                  KeyCode.I, KeyCode.L, KeyCode.B)
              .contains(event.getCode())) {
        this.elementTool(
            event.getCode() == KeyCode.L
                ? "Cl"
                : event.getCode() == KeyCode.B ? "Br" : event.getCode().getName());
        event.consume();
      } else if (!event.isShortcutDown()
          && !event.isAltDown()
          && List.of(KeyCode.DIGIT1, KeyCode.DIGIT2, KeyCode.DIGIT3).contains(event.getCode())) {
        this.bondTool(
            event.getCode() == KeyCode.DIGIT1
                ? ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType.SINGLE
                : event.getCode() == KeyCode.DIGIT2
                    ? ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType.DOUBLE
                    : ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType.TRIPLE);
        event.consume();
      } else if (event.getCode() == KeyCode.DELETE) {
        this.run(this::removeSelected);
        event.consume();
      }
    }
  }

  private javafx.scene.control.ContextMenu drawingMenu(
      final LaboratoryCanvas.Hit atom, final LaboratoryCanvas.BondHit bond) {
    final var menu =
        LaboratoryDrawingMenu.create(
            atom,
            bond,
            (target, operation) ->
                this.run(
                    () -> {
                      this.selected = target;
                      this.selectedBond = null;
                      this.replace(operation.apply(this.layout()));
                    }),
            target ->
                this.run(
                    () -> {
                      this.selected = target;
                      this.selectedBond = null;
                      this.removeAtom();
                    }));
    if (menu != null) {
      menu.getItems()
          .addAll(
              new javafx.scene.control.SeparatorMenuItem(),
              menu(
                  "Стереохимия…",
                  () ->
                      this.run(
                          () -> {
                            this.selected =
                                atom != null
                                    ? atom
                                    : new LaboratoryCanvas.Hit(bond.componentId(), bond.first());
                            this.selectedBond = atom != null ? null : bond;
                            this.stereo();
                          })));
      final var target =
          atom != null ? atom : new LaboratoryCanvas.Hit(bond.componentId(), bond.first());
      final var operations =
          ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeStereoEditing.Transform
              .values();
      final String[] titles = {
        "Повернуть компонент вправо",
        "Повернуть компонент влево",
        "Отразить по горизонтали",
        "Отразить по вертикали"
      };
      final int[] indices = {0, 1, 3, 4};
      final javafx.scene.control.Menu transforms =
          new javafx.scene.control.Menu("Поворот и отражение");
      for (int index = 0; index < titles.length; index++) {
        final var operation = operations[indices[index]];
        transforms
            .getItems()
            .add(
                menu(
                    titles[index],
                    () -> {
                      this.selected = target;
                      this.selectedBond = null;
                      this.transform(operation);
                    }));
      }
      menu.getItems().add(transforms);
      menu.getItems().add(menu("Карточка реестра", () -> this.registry(target.componentId())));
      menu.getItems()
          .addAll(
              new javafx.scene.control.SeparatorMenuItem(),
              menu("Присоединить сложный каркас…", () -> this.scaffolds(target)),
              menu("Свернуть выделенную группу…", this::collapseGroup),
              menu("Раскрыть все группы компонента", () -> this.expandGroups(target, true)));
      if (atom != null
          && ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeAbbreviations
              .projection(this.editor.document().layouts().get(atom.componentId()))
              .anchors()
              .containsKey(atom.atomId())) {
        menu.getItems().add(menu("Раскрыть эту группу", () -> this.expandGroups(atom, false)));
      }
    }
    return menu;
  }

  private Button identityButton() {
    final Button b =
        button("Названия и идентификаторы", () -> this.run(() -> this.structure("identity")));
    b.setId("chemistry-identity-open");
    b.setMaxWidth(Double.MAX_VALUE);
    return b;
  }

  private void structure(final String kind) {
    if (this.selected == null) {
      this.status.setText("Выберите структуру на листе или строку компонента.");
      return;
    }
    this.showStructure(this.selected.componentId(), kind);
  }

  private void showStructure(final String componentId, final String kind) {
    final Runnable close =
        () -> {
          this.structurePage = null;
          this.root.setCenter(this.workArea);
          this.root.getTop().setVisible(true);
          this.root.getTop().setManaged(true);
          this.refresh();
        };
    this.structurePage =
        switch (kind) {
          case "hydrogens" -> new LaboratoryHydrogenPage(this.editor, componentId, close);
          case "atoms" -> new LaboratoryAtomTablePage(this.editor, componentId, close);
          case "identity" -> new LaboratoryIdentityPage(this.editor, componentId, close);
          default -> throw new IllegalArgumentException("Неизвестная форма структуры: " + kind);
        };
    this.root.setCenter(this.structurePage);
    this.root.getTop().setVisible(false);
    this.root.getTop().setManaged(false);
    this.status();
  }

  private void checkStructure() {
    this.computationPage =
        new LaboratoryStructureCheckPage(
            this.editor.document(),
            id -> this.run(() -> this.chooseComponent(id, true)),
            id -> this.run(() -> this.showStructure(id, "hydrogens")),
            this::closeComputation);
    this.openComputation();
  }

  private void stereo() {
    if (this.selected == null) {
      throw new IllegalArgumentException("Выберите атом или связь компонента.");
    }
    final var second = this.selectedBond == null ? null : this.selectedBond.second();
    this.showStereo(this.selected.componentId(), this.selected.atomId(), second);
  }

  private void showStereo(
      final String componentId,
      final ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId atom,
      final ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId second) {
    this.stereoPage =
        new LaboratoryStereoForm(
            this.editor,
            componentId,
            atom,
            second,
            () -> {
              this.stereoPage = null;
              this.root.setCenter(this.workArea);
              this.root.getTop().setVisible(true);
              this.root.getTop().setManaged(true);
              this.refresh();
            });
    this.root.setCenter(this.stereoPage);
    this.root.getTop().setVisible(false);
    this.root.getTop().setManaged(false);
    this.status();
  }

  private void transform(
      final ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeStereoEditing
              .Transform
          transform) {
    this.run(
        () -> {
          if (this.selected == null) {
            throw new IllegalArgumentException("Выберите компонент для преобразования целиком.");
          }
          this.replace(
              ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeStereoEditing
                  .transform(this.layout(), transform));
        });
  }

  private VBox toolbar() {
    final javafx.scene.control.MenuButton document = new javafx.scene.control.MenuButton("Файл");
    document
        .getItems()
        .addAll(
            menu("Начальный экран", this::welcome),
            menu("Новый опыт", this.files::create),
            menu("Открыть…", this.files::openDialog),
            menu("Сохранить как…", () -> this.files.save(true)),
            menu("Повторить опыт", this.files::repeat),
            menu("История версий", this.files::history),
            menu("Восстановить черновик…", this.recovery::browse),
            new javafx.scene.control.Menu(
                "Импорт",
                null,
                menu("ChemDraw · CDXML…", () -> this.chemDraw(false)),
                menu("Структуры · MOL / SDF / SMILES…", () -> this.exchange(false)),
                menu("Реакция · RXN / SMILES…", () -> this.reactionExchange(false, null)),
                menu("Пакет опыта…", this.files::importArchive),
                menu("Проект из пакета…", this::importProject)),
            new javafx.scene.control.Menu(
                "Экспорт",
                null,
                menu("Для ChemDraw · CDXML…", () -> this.chemDraw(true)),
                menu("Структуры…", () -> this.exchange(true)),
                menu("Реакция…", () -> this.reactionExchange(true, null)),
                menu("Пакет опыта…", this.files::exportArchive),
                menu("Пакет проекта…", this::exportProject)),
            menu("Редактировать QCHEM…", () -> this.run(this::showTextEditor)));
    final javafx.scene.control.MenuButton work = new javafx.scene.control.MenuButton("Разделы");
    work.setId("chemistry-work-menu");
    work.getItems()
        .setAll(
            new javafx.scene.control.Menu(
                "Опыт",
                null,
                menu("Рабочий экран", () -> this.desk(null)),
                menu("Название реакции и механизмы", () -> this.classification(null)),
                menu("Методика и проведение", () -> this.procedure(null)),
                menu("Контроль, обработка и очистка", () -> this.operations(null)),
                menu("Следующая стадия из образца", this::sampleTransfer)),
            new javafx.scene.control.Menu(
                "Планирование",
                null,
                menu("Карта многостадийного синтеза", () -> this.route(null)),
                menu("Матрица условий, DoE и виалы", this::design),
                menu("Серия из шаблона", this::series),
                menu("Обзор и сравнение опытов", this::overview)),
            new javafx.scene.control.Menu(
                "Вещества и хранение",
                null,
                menu("Материалы и партии", this::materials),
                menu("Образцы и фракции", this::samples),
                menu("Образцы проекта", this::sampleCatalog),
                menu("Растворы и разведения", this::solutions),
                menu("Карта хранения", this::storage),
                menu("Реестр соединений", this::registry)),
            new javafx.scene.control.Menu(
                "Структуры",
                null,
                menu("Библиотека структур", this::library),
                menu("Построить по названию", this::nameStructure),
                menu("Каркасы и сложные циклы", () -> this.scaffolds(null)),
                menu("Рассчитать R/S и E/Z", this::cip),
                menu("Относительная стереохимия", () -> this.relativeStereo(null)),
                menu("Конформеры и виды", () -> this.conformers(null))),
            new javafx.scene.control.Menu(
                "Аналитика",
                null,
                menu("Расчёт свойств", this::calculation),
                menu("ЯМР · офлайн-прогноз и смеси", this::predictedNmr),
                menu("Спектры, ЯМР, МС и ТСХ", this::analyses),
                menu("Количественная аналитика и досье", this::evidence)),
            new javafx.scene.control.Menu(
                "Методики и поиск",
                null,
                menu("Литература и DOI", () -> this.literature(null)),
                menu("Найти реакцию или методику", this::reactionSearch),
                menu("Поиск по структуре", this::structureSearch),
                menu("Отчёт по опыту", this::report)));
    work.disableProperty().bind(this.root.centerProperty().isNotEqualTo(this.workArea));
    document
        .getItems()
        .get(1)
        .setAccelerator(
            new javafx.scene.input.KeyCodeCombination(
                KeyCode.N, javafx.scene.input.KeyCombination.SHORTCUT_DOWN));
    document
        .getItems()
        .get(2)
        .setAccelerator(
            new javafx.scene.input.KeyCodeCombination(
                KeyCode.O, javafx.scene.input.KeyCombination.SHORTCUT_DOWN));
    document
        .getItems()
        .get(3)
        .setAccelerator(
            new javafx.scene.input.KeyCodeCombination(
                KeyCode.S,
                javafx.scene.input.KeyCombination.SHORTCUT_DOWN,
                javafx.scene.input.KeyCombination.SHIFT_DOWN));
    final javafx.scene.control.Menu recent = new javafx.scene.control.Menu("Недавние опыты");
    recent.setId("chemistry-recent-files");
    document.setOnShowing(
        event -> {
          recent.getItems().clear();
          for (final Path path : this.files.session().recent()) {
            final String caption = path.getFileName() + " — " + path.getParent();
            final var item =
                menu(
                    caption.length() <= 100
                        ? caption
                        : caption.substring(0, 45) + "…" + caption.substring(caption.length() - 50),
                    () -> this.files.open(path));
            recent.getItems().add(item);
          }
          if (recent.getItems().isEmpty()) {
            final var empty = new javafx.scene.control.MenuItem("Сохранённых опытов пока нет");
            empty.setDisable(true);
            recent.getItems().add(empty);
          }
        });
    final javafx.scene.control.CheckMenuItem resume =
        new javafx.scene.control.CheckMenuItem("Открывать последний опыт при запуске");
    resume.setSelected(this.files.session().resume());
    resume.setOnAction(
        event -> {
          this.files.session().resume(resume.isSelected());
          this.status();
        });
    document.getItems().add(2, recent);
    document.getItems().addAll(new javafx.scene.control.SeparatorMenuItem(), resume);
    document
        .getItems()
        .forEach(
            item ->
                item.disableProperty()
                    .bind(this.root.centerProperty().isNotEqualTo(this.workArea)));
    final Button actions = button("Найти действие…", this::commands);
    actions.setId("chemistry-actions-open");
    actions.setTooltip(
        new javafx.scene.control.Tooltip("Найти действие и открыть избранное · Ctrl+K"));
    final Button desk = button("← К опыту", this::returnFromDrawing);
    desk.setId("chemistry-desk-open");
    desk.getStyleClass().add("laboratory-navigation-back");
    desk.setTooltip(
        new javafx.scene.control.Tooltip(
            "Вернуться к опыту; нарисованные структуры останутся в нём"));
    desk.disableProperty().bind(this.root.centerProperty().isNotEqualTo(this.workArea));
    this.drawingConditions.setId("chemistry-drawing-conditions");
    this.drawingConditions.getStyleClass().add("laboratory-primary-action");
    this.drawingConditions.setTooltip(
        new javafx.scene.control.Tooltip(
            "Выбрать исходники и продукт, задать растворитель, температуру и время"));
    final ToolBar bar = new ToolBar(document, work, actions);
    bar.setId("chemistry-main-toolbar");
    this.documentTitle.setId("chemistry-document-title");
    this.documentTitle.setMinWidth(0);
    this.documentTitle.setMaxWidth(Double.MAX_VALUE);
    this.documentTitle.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
    this.documentTitle.setWrapText(false);
    this.icon(this.undo, "undo", "Отменить · Ctrl+Z");
    this.icon(this.redo, "redo", "Повторить · Ctrl+Shift+Z");
    final javafx.scene.control.MenuButton edit = new javafx.scene.control.MenuButton("Правка");
    edit.getItems()
        .addAll(
            menu(
                "Выделить всё",
                () ->
                    this.run(
                        () -> {
                          this.selectedBond = null;
                          this.selected = this.selection.all(null);
                        })),
            menu(
                "Выделить компонент целиком",
                () ->
                    this.run(
                        () -> {
                          if (this.selected != null) {
                            this.selectedBond = null;
                            this.selected = this.selection.all(this.selected.componentId());
                          }
                        })),
            menu("Копировать выделенное", () -> this.run(this.selection::copy)),
            menu("Вставить фрагмент", () -> this.run(this::paste)),
            menu("Свернуть выделенную группу…", this::collapseGroup),
            menu("Раскрыть группы компонента", () -> this.expandGroups(this.selected, true)),
            menu("Каркасы и сложные циклы…", () -> this.scaffolds(null)),
            menu("Таблица атомов и связей…", () -> this.run(() -> this.structure("atoms"))),
            menu("Карточка вещества…", () -> this.run(() -> this.structure("identity"))),
            menu("Стереохимия…", () -> this.run(this::stereo)),
            menu("Водороды вручную…", () -> this.run(() -> this.structure("hydrogens"))),
            menu(
                "Повернуть компонент на 90° ↻",
                () ->
                    this.transform(
                        ru.pathcreator.vadim.quantum.chemistry.drawing.application
                            .MoleculeStereoEditing.Transform.CLOCKWISE)),
            menu(
                "Повернуть компонент на 90° ↺",
                () ->
                    this.transform(
                        ru.pathcreator.vadim.quantum.chemistry.drawing.application
                            .MoleculeStereoEditing.Transform.COUNTERCLOCKWISE)),
            menu(
                "Отразить компонент ↔, сохранив стереохимию",
                () ->
                    this.transform(
                        ru.pathcreator.vadim.quantum.chemistry.drawing.application
                            .MoleculeStereoEditing.Transform.MIRROR_HORIZONTAL)),
            menu(
                "Отразить компонент ↕, сохранив стереохимию",
                () ->
                    this.transform(
                        ru.pathcreator.vadim.quantum.chemistry.drawing.application
                            .MoleculeStereoEditing.Transform.MIRROR_VERTICAL)),
            menu("Удалить выделенное", () -> this.run(this::removeSelected)),
            menu("Дублировать компонент", () -> this.run(this::duplicate)),
            menu(
                "Удалить компонент",
                () ->
                    this.run(
                        () -> {
                          if (this.selected != null) {
                            this.editor.remove(this.selected.componentId());
                            this.selected = null;
                          }
                        })),
            menu(
                "Перестроить 2D выбранной структуры",
                () ->
                    this.run(
                        () -> {
                          if (this.selected != null) {
                            this.replace(
                                ru.pathcreator.vadim.quantum.chemistry.drawing.application
                                    .MoleculeStereoEditing.relayout(this.layout()));
                          }
                        })));
    this.folders.setId("chemistry-folders-toggle");
    this.info.setId("chemistry-properties-toggle");
    this.folders.setOnAction(
        event -> {
          this.wantsFolders = this.folders.isSelected();
          this.propertiesPreferred = false;
          this.showRequestedPanels();
          this.layoutSidePanels();
        });
    this.folders.setTooltip(
        new javafx.scene.control.Tooltip(
            "Папки и поиск опытов · Ctrl+F. На узком экране показывается одна боковая панель."));
    this.wantsProperties = this.viewPreferences.flag("properties", false);
    this.info.setOnAction(
        event -> {
          this.wantsProperties = this.info.isSelected();
          this.propertiesPreferred = true;
          this.showRequestedPanels();
          this.rememberView("properties", this.wantsProperties);
          this.layoutSidePanels();
        });
    this.info.setTooltip(
        new javafx.scene.control.Tooltip(
            "Записи и свойства опыта. Скрытие панели не отменяет незавершённый ввод."));
    final Button save = button("Сохранить", () -> this.files.save(false));
    save.getStyleClass().add("laboratory-save");
    final javafx.scene.control.MenuButton panels = new javafx.scene.control.MenuButton("Панели");
    panels.setId("chemistry-panels-menu");
    final javafx.scene.control.CheckMenuItem foldersItem =
        new javafx.scene.control.CheckMenuItem("Папки опытов");
    foldersItem.selectedProperty().bindBidirectional(this.folders.selectedProperty());
    foldersItem.setOnAction(
        event -> {
          this.wantsFolders = foldersItem.isSelected();
          this.propertiesPreferred = false;
          this.showRequestedPanels();
          this.layoutSidePanels();
        });
    final javafx.scene.control.CheckMenuItem detailsItem =
        new javafx.scene.control.CheckMenuItem("Записи и свойства");
    detailsItem.selectedProperty().bindBidirectional(this.info.selectedProperty());
    detailsItem.setOnAction(
        event -> {
          this.wantsProperties = detailsItem.isSelected();
          this.propertiesPreferred = true;
          this.showRequestedPanels();
          this.rememberView("properties", this.wantsProperties);
          this.layoutSidePanels();
        });
    panels.getItems().addAll(foldersItem, detailsItem);
    panels.disableProperty().bind(this.root.centerProperty().isNotEqualTo(this.workArea));
    bar.getItems().addAll(new javafx.scene.control.Separator(), this.undo, this.redo, edit, panels);
    save.setId("chemistry-save");
    save.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
    save.setTooltip(new javafx.scene.control.Tooltip("Сохранить опыт · Ctrl+S"));
    bar.setMinWidth(0);
    final javafx.scene.layout.HBox heading =
        new javafx.scene.layout.HBox(12, desk, this.documentTitle, this.drawingConditions, save);
    heading.getStyleClass().add("laboratory-navigation-header");
    this.documentTitle.getStyleClass().add("laboratory-navigation-title");
    heading.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    heading.setPadding(new Insets(12, 0, 10, 0));
    heading.setMinWidth(0);
    document.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
    javafx.scene.layout.HBox.setHgrow(this.documentTitle, javafx.scene.layout.Priority.ALWAYS);
    desk.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
    this.drawingConditions.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
    final VBox documentBar = new VBox(heading, bar);
    documentBar.setId("chemistry-document-bar");
    documentBar.setMinWidth(0);
    return documentBar;
  }

  private Node palette() {
    final VBox tools = new VBox(2);
    tools.getStyleClass().add("laboratory-palette");
    final ToggleGroup group = new ToggleGroup();
    final String[] names = {"Выбор", "Рука", "Фрагмент", "Поворот", "Связь", "Разрыв", "Кольцо"};
    final String[] icons = {"select", "pan", "select-all", "rotate", "bond", "erase", "ring"};
    final String[] hints = {
      "Выделение и перемещение · V",
      "Переместить лист · пробел + перетаскивание или средняя кнопка мыши",
      "Выбрать связный фрагмент",
      "Поворот мышью · тяните вокруг центра компонента · шаг 15°, Alt — свободно, Esc — отмена",
      "Одинарная связь · 1",
      "Разорвать связь: нажмите на связь",
      "Шестичленное кольцо · нажмите на лист, атом или связь"
    };
    for (int index = 0; index < names.length; index++) {
      final String name = names[index];
      final ToggleButton button = new ToggleButton(name);
      this.icon(button, icons[index], hints[index]);
      button.setToggleGroup(group);
      button.setSelected(name.equals(this.tool));
      button.setOnAction(event -> this.chooseTool(name));
      this.toolButtons.put(name, button);
      tools.getChildren().add(button);
    }
    tools.getChildren().add(new javafx.scene.control.Separator());
    final Button doubleBond =
        button(
            "Двойная связь",
            () ->
                this.bondTool(
                    ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType.DOUBLE));
    final Button tripleBond =
        button(
            "Тройная связь",
            () ->
                this.bondTool(
                    ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType.TRIPLE));
    this.icon(doubleBond, "double", "Двойная связь · 2");
    this.icon(tripleBond, "triple", "Тройная связь · 3");
    final Button benzene =
        button(
            "Бензольное кольцо",
            () -> {
              this.ringSize = 6;
              this.aromaticRing = true;
              this.chooseTool("Кольцо");
            });
    final Button pentagon =
        button(
            "Пятичленное кольцо",
            () -> {
              this.ringSize = 5;
              this.aromaticRing = false;
              this.chooseTool("Кольцо");
            });
    this.icon(benzene, "benzene", "Бензольное кольцо");
    this.icon(pentagon, "pentagon", "Пятичленное кольцо");
    final Button remove = button("Удалить выделенное", () -> this.run(this::removeSelected));
    this.icon(remove, "delete", "Удалить выделенное · Delete");
    tools
        .getChildren()
        .addAll(
            doubleBond,
            tripleBond,
            benzene,
            pentagon,
            new javafx.scene.control.Separator(),
            remove);
    this.toolButtons
        .get("Кольцо")
        .setOnAction(
            event -> {
              this.ringSize = 6;
              this.aromaticRing = false;
              this.chooseTool("Кольцо");
            });
    this.toolButtons
        .get("Связь")
        .setOnAction(
            event ->
                this.bondTool(
                    ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType.SINGLE));
    return tools;
  }

  private ScrollPane scrollPalette(final Node content, final String id) {
    final ScrollPane scroll = new ScrollPane(content);
    scroll.setId(id);
    scroll.getStyleClass().add("laboratory-palette-scroll");
    scroll.setFitToWidth(true);
    scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    scroll.setMinHeight(0);
    scroll.setMinWidth(56);
    scroll.setPrefWidth(56);
    scroll.setMaxWidth(56);
    return scroll;
  }

  private Node elements() {
    final VBox elements = new VBox(4);
    elements.getStyleClass().add("laboratory-palette");
    for (final String symbol : List.of("C", "H", "N", "O", "S", "P", "F", "Cl", "Br", "I")) {
      final ToggleButton button = new ToggleButton(symbol);
      button.setOnAction(event -> this.elementTool(symbol));
      this.elementButtons.put(symbol, button);
      button.setId("chemistry-element-" + symbol);
      button.setTooltip(
          new javafx.scene.control.Tooltip(
              symbol
                  + " · выбрать элемент, затем нажать на атом для замены; потянуть от атома — добавить заместитель"));
      button.getStyleClass().add("laboratory-element");
      if (List.of("N", "O", "S", "Cl", "Br", "F").contains(symbol)) {
        button.getStyleClass().add("element-" + symbol);
      }
      elements.getChildren().add(button);
    }
    return elements;
  }

  private void chooseTool(final String name) {
    this.tool = name;
    if (List.of("Связь", "Кольцо", "Атом").contains(name)) {
      this.startDismissed = true;
      this.updateStartPane();
    }
    this.toolButtons.values().forEach(button -> button.setSelected(false));
    if (this.toolButtons.containsKey(name)) {
      this.toolButtons.get(name).setSelected(true);
    }
    this.elementButtons.forEach(
        (symbol, button) ->
            button.setSelected(name.equals("Атом") && symbol.equals(this.drawElement)));
    this.canvas.bondType(
        this.tool.equals("Атом")
            ? ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType.SINGLE
            : this.drawBond);
    this.canvas.update(this.editor.document(), this.selected, this.selection.hits(), this.tool);
    this.updateDrawingGuide();
    this.status();
  }

  private void elementTool(final String symbol) {
    // Selecting a palette tool must never edit the previously selected atom or commit form input.
    this.drawElement = symbol;
    this.selectedBond = null;
    this.drawBond = ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType.SINGLE;
    this.chooseTool("Атом");
  }

  private void updateDrawingGuide() {
    if (this.drawingGuide != null) {
      this.drawingGuide.tool(
          this.tool,
          this.drawElement,
          this.drawBond == ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType.TRIPLE
              ? "Тройная"
              : this.drawBond
                      == ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType.DOUBLE
                  ? "Двойная"
                  : "Одинарная");
    }
  }

  private void changeElement() {
    final var atom =
        this.layout().molecule().atoms().stream()
            .filter(value -> value.id().equals(this.selected.atomId()))
            .findFirst()
            .orElseThrow();
    this.replace(
        this.sketch.changeAtom(
            this.layout(),
            atom.id(),
            this.drawElement,
            atom.formalCharge().value(),
            atom.hasIsotope() ? atom.isotope().massNumber() : null));
  }

  private void bondTool(
      final ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType type) {
    this.drawBond = type;
    this.canvas.bondType(type);
    this.chooseTool("Связь");
  }

  private void icon(
      final javafx.scene.control.ButtonBase button, final String name, final String hint) {
    button.setGraphic(LaboratoryIcons.create(name));
    button.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
    button.setTooltip(new javafx.scene.control.Tooltip(hint));
    button.setAccessibleText(hint);
    button.getStyleClass().add("laboratory-icon-button");
  }

  private void showRequestedPanels() {
    this.manualDrawingFocus = false;
    this.automaticFocusSuppressed = true;
    this.rememberView("drawingFocus", false);
  }

  private void openFolders() {
    this.wantsFolders = true;
    this.propertiesPreferred = false;
    this.showRequestedPanels();
    this.layoutSidePanels();
  }

  private void layoutSidePanels() {
    if (this.archivePanel == null || this.arrangingPanels) {
      return;
    }
    this.arrangingPanels = true;
    try {
      final double width = this.workArea.getWidth() > 0 ? this.workArea.getWidth() : 1000;
      final double propertiesWidth = Math.max(200, Math.min(320, width * .40));
      final double foldersWidth = Math.max(200, Math.min(240, width * .38));
      final boolean bothFit = width >= propertiesWidth + foldersWidth + 360;
      final boolean requestedProperties =
          this.wantsProperties && (bothFit || !this.wantsFolders || this.propertiesPreferred);
      final boolean requestedFolders =
          this.wantsFolders && (bothFit || !this.wantsProperties || !this.propertiesPreferred);
      final double height = this.workArea.getHeight() > 0 ? this.workArea.getHeight() : 720;
      final double mainWidth =
          width
              - (requestedProperties ? propertiesWidth : 0)
              - (requestedFolders ? foldersWidth : 0);
      final double dock =
          this.componentPanel.isExpanded() ? Math.max(140, Math.min(240, height * .38)) + 28 : 28;
      final double available = height - 100 - dock;
      final boolean tight =
          this.together.isSelected()
              && (mainWidth < 560
                  || available
                      < (mainWidth >= 900
                          ? 300
                          : this.geometry.controlsExpandedProperty().get() ? 560 : 480));
      if (this.compactFocusRegion && !tight) {
        this.automaticFocusSuppressed = false;
      }
      this.compactFocusRegion = tight;
      final boolean previousFocus = this.drawingFocused;
      this.drawingFocused = this.manualDrawingFocus || tight && !this.automaticFocusSuppressed;
      final boolean showProperties = requestedProperties && !this.drawingFocused;
      final boolean showFolders = requestedFolders && !this.drawingFocused;
      this.drawingSpace.setSelected(this.drawingFocused);
      this.drawingSpace.setText(this.drawingFocused ? "Вернуть панели" : "Больше места");
      this.componentPanel.setVisible(!this.drawingFocused);
      this.componentPanel.setManaged(!this.drawingFocused);
      if (previousFocus != this.drawingFocused) {
        this.canvas.refreshViewport();
      }
      this.properties.setPrefWidth(propertiesWidth);
      ((javafx.scene.layout.Region) this.archivePanel).setPrefWidth(foldersWidth);
      this.workArea.setRight(showProperties ? this.properties : null);
      this.workArea.setLeft(showFolders ? this.archivePanel : null);
      this.info.setSelected(showProperties);
      this.folders.setSelected(showFolders);
    } finally {
      this.arrangingPanels = false;
    }
  }

  private Node sidebar() {
    final Button overview = button("Обзор опытов", this::overview);
    overview.setId("chemistry-project-overview");
    overview.setMaxWidth(Double.MAX_VALUE);
    final Button choose = button("Открыть папку опытов…", this::chooseFolder);
    choose.setMaxWidth(Double.MAX_VALUE);
    final VBox box = new VBox(8, choose, overview, this.archive);
    box.setMinWidth(200);
    box.setPrefWidth(240);
    box.setPadding(new Insets(12));
    VBox.setVgrow(this.archive, javafx.scene.layout.Priority.ALWAYS);
    return box;
  }

  private void table() {
    this.componentView =
        new LaboratoryComponentsPane(
            this.components,
            this::openQuantities,
            id -> {
              if (!this.refreshing) {
                this.run(() -> this.chooseComponent(id, false));
              }
            },
            id -> {
              this.showComponents(false);
              this.run(() -> this.chooseComponent(id, true));
            },
            this::openStageQuantities,
            this::showComponents);
    this.componentPanel.setContent(this.componentView);
    this.componentDockHeight();
    this.components
        .heightProperty()
        .addListener(
            (observable, before, after) -> {
              if (this.root.getCenter() == this.workArea
                  && this.componentPanel.isExpanded()
                  && this.selected != null) {
                this.canvas.refreshViewport();
              }
            });
  }

  private void componentDockHeight() {
    this.componentView.prefHeightProperty().unbind();
    if (this.componentsPage) {
      this.componentView.setPrefHeight(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
    } else {
      this.componentView
          .prefHeightProperty()
          .bind(
              javafx.beans.binding.Bindings.max(
                  140,
                  javafx.beans.binding.Bindings.min(
                      240, this.workArea.heightProperty().multiply(.38))));
    }
  }

  private void showComponents(final boolean expanded) {
    if (this.componentView == null || this.componentsPage == expanded) {
      return;
    }
    this.componentsPage = expanded;
    if (expanded) {
      this.componentPanel.setContent(null);
      this.root.setCenter(this.componentView);
    } else {
      this.root.setCenter(this.workArea);
      this.componentPanel.setContent(this.componentView);
    }
    this.root.getTop().setVisible(!expanded);
    this.root.getTop().setManaged(!expanded);
    this.componentView.expanded(expanded);
    this.componentDockHeight();
    if (expanded) {
      this.componentView.focusSearch();
    } else {
      this.components.requestFocus();
    }
    this.status();
  }

  private void returnToComponentsOrDrawing() {
    this.root.setCenter(this.componentsPage ? this.componentView : this.workArea);
    this.root.getTop().setVisible(!this.componentsPage);
    this.root.getTop().setManaged(!this.componentsPage);
    this.refresh();
    if (this.componentsPage) {
      this.components.requestFocus();
    }
  }

  private void gesture(final LaboratoryCanvas.Gesture gesture) {
    this.run(
        () -> {
          this.selectedBond = null;
          if (this.tool.equals("Кольцо")) {
            final var placement =
                new ru.pathcreator.vadim.quantum.chemistry.drawing.application
                    .MoleculeRingPlacement();
            if (gesture.hit() != null) {
              this.selected = gesture.hit();
              this.replace(
                  placement.atAtom(
                      this.layout(), this.selected.atomId(), this.ringSize, this.aromaticRing));
            } else if (gesture.bond() != null) {
              final var bond = gesture.bond();
              this.selected = new LaboratoryCanvas.Hit(bond.componentId(), bond.first());
              this.replace(
                  placement.atBond(
                      this.layout(),
                      bond.first(),
                      bond.second(),
                      this.ringSize,
                      this.aromaticRing));
            } else {
              this.selectAdded(
                  this.sketch.ring(this.ringSize, this.aromaticRing, gesture.x(), gesture.y()));
            }
          } else if (this.tool.equals("Атом")) {
            if (gesture.moved()) {
              if (gesture.start() != null) {
                this.drawBondGesture(gesture);
              }
            } else if (gesture.hit() != null) {
              this.selected = gesture.hit();
              this.changeElement();
            } else if (gesture.bond() != null) {
              this.editClickedBond(gesture.bond(), true);
            } else {
              final var drawing = this.sketch.create(false, gesture.x(), gesture.y());
              this.selectAdded(
                  this.sketch.changeAtom(
                      drawing,
                      drawing.molecule().atoms().getFirst().id(),
                      this.drawElement,
                      0,
                      null));
            }
          } else if (this.tool.equals("Выбор") || this.tool.equals("Фрагмент")) {
            if (!gesture.moved() && gesture.hit() == null && gesture.bond() != null) {
              final var bond = gesture.bond();
              this.selected = new LaboratoryCanvas.Hit(bond.componentId(), bond.first());
              if (this.tool.equals("Фрагмент")) {
                this.selected = this.selection.connected(bond.componentId(), bond.first());
              } else {
                this.selectedBond = bond;
                this.selection.sync(null);
              }
            } else {
              this.selected = this.selection.gesture(gesture, this.tool.equals("Фрагмент"));
            }
          } else if (this.tool.equals("Разрыв")) {
            if (gesture.bond() != null) {
              final var bond = gesture.bond();
              this.selected = new LaboratoryCanvas.Hit(bond.componentId(), bond.first());
              this.replace(this.sketch.disconnect(this.layout(), bond.first(), bond.second()));
            }
          } else if (this.tool.equals("Связь")) {
            if (gesture.moved()) {
              this.drawBondGesture(gesture);
            } else if (gesture.hit() != null) {
              this.selected = gesture.hit();
            } else if (gesture.bond() != null) {
              this.editClickedBond(
                  gesture.bond(),
                  this.drawBond
                      == ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType.SINGLE);
            } else {
              final var drawing = this.sketch.create(false, gesture.x(), gesture.y());
              this.selectAdded(
                  this.sketch.changeAtom(
                      drawing,
                      drawing.molecule().atoms().getFirst().id(),
                      this.drawElement,
                      0,
                      null));
            }
          }
        });
  }

  private void editClickedBond(final LaboratoryCanvas.BondHit hit, final boolean cycle) {
    this.selected = new LaboratoryCanvas.Hit(hit.componentId(), hit.first());
    final var drawing = this.layout();
    final var bond =
        drawing.molecule().bonds().stream()
            .filter(value -> value.connects(hit.first(), hit.second()))
            .findFirst()
            .orElseThrow();
    final var next =
        cycle
            ? switch (bond.type()) {
              case SINGLE ->
                  ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType.DOUBLE;
              case DOUBLE ->
                  ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType.TRIPLE;
              case TRIPLE ->
                  ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType.SINGLE;
              default ->
                  throw new IllegalArgumentException(
                      "Для этой связи автоматический цикл недоступен. Выберите тип связи явно или откройте её свойства.");
            }
            : this.drawBond;
    if (cycle
        && drawing.bonds().stream()
            .anyMatch(
                value ->
                    value.connects(hit.first(), hit.second())
                        && value.style()
                            != ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model
                                .BondDrawingStyle.PLAIN)) {
      throw new IllegalArgumentException(
          "У связи есть стереообозначение. Измените её явно через свойства связи.");
    }
    this.replace(this.sketch.connect(drawing, hit.first(), hit.second(), next));
    this.selectedBond = hit;
  }

  private void drawBondGesture(final LaboratoryCanvas.Gesture gesture) {
    final var type =
        this.tool.equals("Атом")
            ? ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType.SINGLE
            : this.drawBond;
    if (gesture.start() == null) {
      final double x = gesture.x() - gesture.dx(), y = gesture.y() - gesture.dy();
      var drawing = this.sketch.create(false, x, y);
      final var from = drawing.molecule().atoms().getFirst().id();
      drawing = this.sketch.changeAtom(drawing, from, this.drawElement, 0, null);
      final double[] end = LaboratoryCanvas.snapped(x, y, gesture.x(), gesture.y());
      drawing = this.sketch.extend(drawing, from, this.drawElement, type, end[0], end[1]);
      this.selectAdded(drawing);
      this.selected =
          new LaboratoryCanvas.Hit(
              this.selected.componentId(), MoleculeSkeletonHydrogens.lastEditableAtom(drawing));
      return;
    }
    this.selected = gesture.start();
    if (gesture.hit() != null) {
      if (!gesture.hit().componentId().equals(this.selected.componentId())) {
        this.joinFragment(gesture.hit(), type);
      } else if (!gesture.hit().atomId().equals(this.selected.atomId())) {
        this.replace(
            this.sketch.connect(
                this.layout(), this.selected.atomId(), gesture.hit().atomId(), type));
        this.selected = gesture.hit();
      }
      return;
    }
    final var from =
        this.layout().atoms().stream()
            .filter(atom -> atom.atomId().equals(this.selected.atomId()))
            .findFirst()
            .orElseThrow()
            .point2D();
    final double[] end = LaboratoryCanvas.snapped(from.x(), from.y(), gesture.x(), gesture.y());
    final var drawing =
        this.sketch.extend(
            this.layout(), this.selected.atomId(), this.drawElement, type, end[0], end[1]);
    this.replace(drawing);
    this.selected =
        new LaboratoryCanvas.Hit(
            this.selected.componentId(), MoleculeSkeletonHydrogens.lastEditableAtom(drawing));
  }

  private void joinFragment(
      final LaboratoryCanvas.Hit other,
      final ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType type) {
    final var from = this.selected;
    this.editor.joinDrafts(
        from.componentId(), from.atomId(), other.componentId(), other.atomId(), type);
    this.selected = this.editor.document().layouts().containsKey(from.componentId()) ? from : other;
    this.selectedBond = null;
  }

  private void selectAdded(final MoleculeDrawing drawing) {
    final String name = drawing.molecule().displayName();
    MoleculeDrawing added = drawing;
    if (name.equals("Новая структура")
        || name.equals("Шестичленное кольцо")
        || name.matches("Кольцо · [3-8] атомов")) {
      final var names =
          this.editor.document().experiment().components().stream()
              .map(component -> component.molecule().displayName())
              .toList();
      int index = 1;
      while (names.contains("Структура " + index)) {
        index++;
      }
      added = this.sketch.rename(drawing, "Структура " + index);
    }
    final String id = this.editor.add(added, ReactionComponentRole.REACTANT);
    this.selected = new LaboratoryCanvas.Hit(id, drawing.molecule().atoms().getFirst().id());
  }

  private MoleculeDrawing layout() {
    return this.editor.document().layouts().get(this.selected.componentId());
  }

  private void replace(final MoleculeDrawing drawing) {
    final var component =
        this.editor.document().experiment().components().stream()
            .filter(value -> value.id().equals(this.selected.componentId()))
            .findFirst()
            .orElseThrow();
    this.editor.replace(component.id(), drawing, component.role());
  }

  private void duplicate() {
    if (this.selected == null) {
      return;
    }
    final var component =
        this.editor.document().experiment().components().stream()
            .filter(item -> item.id().equals(this.selected.componentId()))
            .findFirst()
            .orElseThrow();
    final var drawing = this.sketch.translate(this.sketch.independentCopy(this.layout()), 80, 80);
    final String id = this.editor.add(drawing, component.role(), component.planned());
    this.chooseComponent(id, true);
  }

  private void removeSelected() {
    if (this.selectedBond != null) {
      this.replace(
          this.sketch.disconnect(
              this.layout(), this.selectedBond.first(), this.selectedBond.second()));
      this.selectedBond = null;
    } else {
      this.selection.remove();
    }
    this.selected = null;
  }

  private void removeAtom() {
    if (this.selected == null) {
      return;
    }
    this.selection.sync(null);
    this.selection.sync(this.selected);
    this.selection.remove();
    this.selected = null;
  }

  private void flush() {
    this.details.apply(this.editor);
    if (this.selected != null && this.inspector.pending()) {
      final var quantities = this.inspector.quantities();
      final var planned = quantities.planned();
      final var actual = quantities.actual();
      this.editor.replace(
          this.selected.componentId(),
          this.inspector.apply(this.sketch, this.layout(), this.selected),
          this.inspector.role());
      this.editor.quantities(this.selected.componentId(), planned, actual, quantities.evidence());
    }
  }

  private void flushAll() {
    this.flush();
    this.journal.flush();
  }

  private boolean pending() {
    return this.details.pending()
        || this.inspector.pending()
        || this.journal != null && this.journal.pending();
  }

  private void run(final Runnable action) {
    try {
      this.flush();
      action.run();
      this.refresh();
    } catch (final RuntimeException exception) {
      this.files.error(exception);
    }
  }

  private void history(final boolean forward) {
    this.run(
        () -> {
          if (forward) {
            this.editor.redo();
          } else {
            this.editor.undo();
          }
          // refresh() validates atom and bond identities against the restored document.
          // Keep a surviving selection so that undoing a drawing change does not empty 3D.
          this.focusPending = null;
          this.details.load(this.editor.document().experiment());
        });
  }

  private java.util.List<String> legacyHydrogenCandidates() {
    return this.editor.document().layouts().entrySet().stream()
        .filter(entry -> !MoleculeSkeletonHydrogens.managed(entry.getValue().molecule()))
        .filter(
            entry ->
                ru
                    .pathcreator
                    .vadim
                    .quantum
                    .chemistry
                    .drawing
                    .application
                    .MoleculeHydrogenEditing
                    .suggest(entry.getValue())
                    .stream()
                    .anyMatch(value -> value.additional() != null && value.additional() > 0))
        .map(java.util.Map.Entry::getKey)
        .toList();
  }

  private void updateHydrogenNotice() {
    final var legacy = this.legacyHydrogenCandidates();
    final int uncertain =
        this.editor.document().layouts().values().stream()
            .mapToInt(MoleculeSkeletonHydrogens::unsupportedCount)
            .sum();
    this.hydrogenNotice.getChildren().clear();
    this.hydrogenNotice.setVisible(
        this.drawingTabs.getSelectionModel().getSelectedIndex() == 0
            && (!legacy.isEmpty() || uncertain > 0));
    if (!legacy.isEmpty()) {
      final Label text =
          new Label(
              "В старых рисунках могут отсутствовать H: "
                  + legacy.size()
                  + ". Дополните их для расчёта формул и навесок.");
      text.setWrapText(true);
      final Button apply = button("Учесть водороды", () -> this.run(this::completeLegacyHydrogens));
      apply.setId("chemistry-complete-skeleton-hydrogens");
      apply.setTooltip(
          new javafx.scene.control.Tooltip(
              "Дополнить обычные нейтральные валентности в этих рисунках. Уже заданные H сохраняются. Все изменения отменяются одним Ctrl+Z."));
      final var row = new javafx.scene.layout.FlowPane(10, 4, text, apply);
      row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
      this.hydrogenNotice.getChildren().add(row);
    }
    if (uncertain > 0) {
      final Label warning =
          new Label(
              "Число H требует проверки для атомов: "
                  + uncertain
                  + ". Выберите структуру → Вид → Водороды вручную.");
      warning.setWrapText(true);
      this.hydrogenNotice.getChildren().add(warning);
    }
  }

  private void completeLegacyHydrogens() {
    final var replacements = new java.util.LinkedHashMap<String, MoleculeDrawing>();
    for (final String id : this.legacyHydrogenCandidates()) {
      replacements.put(
          id, MoleculeSkeletonHydrogens.normalize(this.editor.document().layouts().get(id)));
    }
    if (!replacements.isEmpty()) {
      this.editor.editComponents(replacements, Set.of());
    }
  }

  private void reload() {
    this.drawingReturn = null;
    this.drawingStageId = null;
    this.navigationReturn = null;
    this.showComponents(false);
    if (this.componentView != null) {
      this.componentView.resetFilters();
    }
    if (this.textPage != null) {
      this.textPage.dispose();
      this.textPage = null;
    }
    if (this.projectPage != null) {
      this.projectPage.dispose();
    }
    if (this.structurePage != null) {
      this.structurePage.dispose();
      this.structurePage = null;
    }
    if (this.computationPage != null) {
      this.computationPage.dispose();
      this.computationPage = null;
    }
    this.calculationArchivePage = false;
    if (this.libraryPage != null) {
      this.libraryPage.dispose();
      this.libraryPage = null;
    }
    if (this.materialsPage != null) {
      this.materialsPage.dispose();
    }
    this.quantityPage = null;
    this.stoichiometryPage = null;
    this.materialsPage = null;
    this.projectPage = null;
    this.stagePage = null;
    this.stereoPage = null;
    this.root.setCenter(this.workArea);
    this.root.getTop().setVisible(true);
    this.root.getTop().setManaged(true);
    this.selected = null;
    this.selectedBond = null;
    this.focusPending = null;
    this.displayedDocument = null;
    this.journal.reset();
    this.fitPending = true;
    this.fitOpenedDocument();
    this.details.load(this.editor.document().experiment());
    this.refresh();
    this.listFolder();
  }

  private void saved() {
    this.details.load(this.editor.document().experiment());
    this.refresh();
    this.listFolder();
    if (this.computationPage instanceof LaboratoryExperimentDesk desk && !desk.disposed) {
      if (this.editor.dirty()) {
        desk.status.setText("Снимок опыта записан. Более поздние изменения ещё не сохранены.");
      } else {
        desk.saved();
      }
    }
    if (this.computationPage instanceof LaboratoryRoutePage route && !route.disposed) {
      route.saved();
    }
    if (this.computationPage instanceof LaboratorySamplePage samples && !samples.disposed) {
      samples.saved();
    }
  }

  private void refresh() {
    this.refreshing = true;
    try {
      final var addedStage = this.addedPlanStage();
      final var previousDocument = this.displayedDocument;
      this.displayedDocument = this.editor.document();
      if (previousDocument != this.displayedDocument
          && this.computationPage instanceof LaboratoryRoutePage route
          && !route.disposed) {
        route.documentChanged();
      }
      if (addedStage != null) {
        addedStage.participants().stream()
            .filter(
                participant ->
                    participant.role() == ReactionComponentRole.PRODUCT
                        && !previousDocument.layouts().containsKey(participant.componentId()))
            .findFirst()
            .ifPresent(participant -> this.chooseComponent(participant.componentId(), false));
      }
      if (this.selected != null) {
        final var layout = this.editor.document().layouts().get(this.selected.componentId());
        if (layout == null
            || layout.atoms().stream()
                .noneMatch(atom -> atom.atomId().equals(this.selected.atomId()))) {
          this.selected = null;
          this.selectedBond = null;
          this.focusPending = null;
        }
      }
      if (this.selectedBond != null
          && (this.selected == null
              || !this.selected.componentId().equals(this.selectedBond.componentId())
              || !this.selected.atomId().equals(this.selectedBond.first())
              || this.layout().molecule().bonds().stream()
                  .noneMatch(
                      bond ->
                          bond.connects(this.selectedBond.first(), this.selectedBond.second())))) {
        this.selectedBond = null;
      }
      if (this.selected == null || !this.selected.componentId().equals(this.focusPending)) {
        this.focusPending = null;
      }
      this.selection.sync(this.selectedBond == null ? this.selected : null);

      final LaboratoryComponent component =
          this.selected == null
              ? null
              : this.editor.document().experiment().components().stream()
                  .filter(value -> value.id().equals(this.selected.componentId()))
                  .findFirst()
                  .orElse(null);
      this.updateHydrogenNotice();
      if (this.iupacButton != null) {
        this.iupacButton.setDisable(
            component == null || this.drawingTabs.getSelectionModel().getSelectedIndex() != 0);
      }
      if (this.renameButton != null) {
        this.renameButton.setDisable(
            component == null || this.drawingTabs.getSelectionModel().getSelectedIndex() != 0);
      }
      this.stereoButton.setDisable(component == null);
      this.amountButton.setDisable(component == null);
      this.focusButton.setDisable(component == null);
      this.transformButtons.forEach(
          button ->
              button.setDisable(
                  component == null
                      || this.drawingTabs.getSelectionModel().getSelectedIndex() != 0));
      this.inspector.load(
          component,
          component == null
              ? null
              : component.molecule().atoms().stream()
                  .filter(atom -> atom.id().equals(this.selected.atomId()))
                  .findFirst()
                  .orElse(null),
          component == null ? null : this.editor.document().layouts().get(component.id()));
      this.componentView.load(
          this.editor.document().experiment().components(),
          component == null ? null : component.id(),
          this.editor.document().experiment().stages());
      this.canvas.selectedBond(this.selectedBond);
      this.canvas.update(this.editor.document(), this.selected, this.selection.hits(), this.tool);
      this.geometry.components(
          this.editor.document().experiment().components(),
          component == null ? null : component.id());
      this.geometry.load(
          component == null ? null : component.molecule(),
          component == null ? null : this.selected.atomId());
      this.reaction.refresh();
      if (addedStage != null) {
        this.drawingTabs.getSelectionModel().select(1);
        final String experimentId = this.editor.document().experiment().id();
        javafx.application.Platform.runLater(
            () -> {
              if (experimentId.equals(this.editor.document().experiment().id())
                  && this.drawingTabs.getSelectionModel().getSelectedIndex() == 1) {
                this.root.applyCss();
                this.root.layout();
                this.reaction.reveal(addedStage.id());
              }
            });
      }
      this.journal.refresh();
      this.attachments.refresh();
      this.documentTitle.setText(
          "Проекты › " + this.editor.document().experiment().title() + " › Структуры");
      this.drawingConditions.setDisable(this.editor.document().experiment().components().isEmpty());
      this.documentTitle.setTooltip(
          new javafx.scene.control.Tooltip(
              this.editor.document().experiment().title()
                  + (this.files.path() == null ? "" : "\n" + this.files.path())));
      this.updateStartPane();
      this.updateDrawingGuide();
      this.componentPanel.setText(
          "Компоненты · "
              + this.editor.document().experiment().components().size()
              + "  /  План и факт");
      this.undo.setDisable(!this.editor.canUndo());
      this.redo.setDisable(!this.editor.canRedo());
      this.status();
      this.focusSelectedComponent();
    } finally {
      this.refreshing = false;
    }
  }

  private void updateStartPane() {
    if (this.startPane == null) {
      return;
    }
    final var experiment = this.editor.document().experiment();
    if (!experiment.id().equals(this.startDocument)) {
      this.startDocument = experiment.id();
      this.startDismissed = false;
    }
    final boolean empty = experiment.components().isEmpty() && experiment.stages().isEmpty();
    this.startPane.present(
        empty && !this.startDismissed,
        this.files.path() == null && experiment.journal().isEmpty() && experiment.notes().isBlank()
            ? "С чего начнём?"
            : "В опыте пока нет структур");
  }

  private ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionStage
      addedPlanStage() {
    if (this.displayedDocument == null
        || !this.displayedDocument
            .experiment()
            .id()
            .equals(this.editor.document().experiment().id())) {
      return null;
    }
    final var previous = this.displayedDocument.experiment();
    return this.editor.document().experiment().stages().stream()
        .filter(stage -> previous.stages().stream().noneMatch(old -> old.id().equals(stage.id())))
        .filter(
            stage ->
                stage.participants().stream()
                    .anyMatch(
                        participant ->
                            participant.role() == ReactionComponentRole.PRODUCT
                                && previous.components().stream()
                                    .noneMatch(
                                        component ->
                                            component.id().equals(participant.componentId()))))
        .findFirst()
        .orElse(null);
  }

  private void status() {
    if (this.files == null || this.details == null || this.inspector == null) {
      return;
    }
    this.status.setText(
        (this.textPage != null
                ? "Ручной QCHEM · ввод применяется кнопкой в форме"
                : this.quantityPage != null || this.stoichiometryPage != null
                    ? "Количества · ввод применяется кнопкой в форме"
                    : this.materialsPage != null
                        ? "Материалы · ввод проводится отдельной операцией"
                        : this.projectPage != null
                            ? "Перенос папки проекта · состав и результат показаны на странице"
                            : this.structurePage instanceof LaboratoryIdentityPage
                                ? "Карточка вещества · названия, идентификаторы и источник"
                                : this.structurePage != null
                                    ? "Структура · изменения применяются одной операцией"
                                    : this.computationPage instanceof LaboratoryScaffoldPage
                                        ? "Каркасы · конденсированные, мостиковые и спироциклы"
                                        : this.computationPage
                                                instanceof LaboratoryReactionClassificationPage
                                            ? "Название реакции и механизмы · ручной выбор для стадии"
                                            : this.computationPage instanceof LaboratorySamplePage
                                                ? "Образцы и фракции · остатки, происхождение и анализы"
                                                : this.computationPage
                                                        instanceof LaboratorySampleStockPage
                                                    ? "Образец → партия · физический перенос с сохранением происхождения"
                                                    : this.computationPage
                                                            instanceof LaboratorySolutionPage
                                                        ? "Растворы · приготовление, стандартизация и разведения"
                                                        : this.computationPage
                                                                instanceof LaboratoryStoragePage
                                                            ? "Хранение · шкафы, коробки, образцы и сроки"
                                                            : this.computationPage
                                                                    instanceof
                                                                    LaboratoryNameStructurePage
                                                                ? "Название → структура · локальное распознавание и просмотр"
                                                                : this.computationPage
                                                                        instanceof
                                                                        LaboratoryRelativeStereoPage
                                                                    ? "Относительная стереохимия · группы и рацематы"
                                                                    : this.computationPage
                                                                            instanceof
                                                                            LaboratoryCipPage
                                                                        ? "Стереохимия · вычисленные CIP-дескрипторы"
                                                                        : this.computationPage
                                                                                instanceof
                                                                                LaboratoryLiteraturePage
                                                                            ? "Литература · статьи, DOI, supporting information и методики"
                                                                            : this.computationPage
                                                                                    instanceof
                                                                                    LaboratoryRegistryPage
                                                                                ? "Реестр соединений · постоянные номера и формы вещества"
                                                                                : this
                                                                                            .computationPage
                                                                                        instanceof
                                                                                        LaboratoryConformerPage
                                                                                    ? "Конформеры · сравнение геометрий, измерения и сохранённые виды"
                                                                                    : this
                                                                                                .computationPage
                                                                                            instanceof
                                                                                            LaboratoryExperimentDesk
                                                                                        ? "Рабочий экран · схема, загрузка, методика, наблюдения и результат"
                                                                                        : this
                                                                                                    .computationPage
                                                                                                instanceof
                                                                                                LaboratoryOperationsPage
                                                                                            ? "Контроль реакции · обработка смеси и очистка"
                                                                                            : this
                                                                                                        .computationPage
                                                                                                    instanceof
                                                                                                    LaboratoryEvidencePage
                                                                                                ? "Спектры · количественная аналитика и подтверждение продукта"
                                                                                                : this
                                                                                                            .computationPage
                                                                                                        instanceof
                                                                                                        LaboratoryDesignPage
                                                                                                    ? "Планирование экспериментов · факторы, виалы и результаты"
                                                                                                    : this
                                                                                                                .computationPage
                                                                                                            instanceof
                                                                                                            LaboratoryRoutePage
                                                                                                        ? "Маршрут синтеза · планирование от конечного продукта"
                                                                                                        : this
                                                                                                                    .computationPage
                                                                                                                instanceof
                                                                                                                LaboratorySampleTransferPage
                                                                                                            ? "Образец в следующий опыт · количество и происхождение"
                                                                                                            : this
                                                                                                                        .computationPage
                                                                                                                    instanceof
                                                                                                                    LaboratoryReactionSearchPage
                                                                                                                ? "Реакции и методики · поиск по сохранённым опытам"
                                                                                                                : this
                                                                                                                            .computationPage
                                                                                                                        instanceof
                                                                                                                        LaboratoryProcedurePage
                                                                                                                    ? "Методика и проведение · план и фактические действия"
                                                                                                                    : this
                                                                                                                                .computationPage
                                                                                                                            instanceof
                                                                                                                            LaboratoryChemDrawPage
                                                                                                                        ? "Обмен ChemDraw · просмотр перед добавлением в опыт"
                                                                                                                        : this
                                                                                                                                    .computationPage
                                                                                                                                instanceof
                                                                                                                                LaboratoryPreparationPage
                                                                                                                            ? "Подготовка реагентов · резерв и расход сохраняются в учёте проекта"
                                                                                                                            : this
                                                                                                                                        .computationPage
                                                                                                                                    instanceof
                                                                                                                                    LaboratoryStructureCheckPage
                                                                                                                                ? "Проверка структуры · снимок текущего опыта"
                                                                                                                                : this
                                                                                                                                            .computationPage
                                                                                                                                        instanceof
                                                                                                                                        LaboratoryAttachmentPage
                                                                                                                                    ? "Вложения · просмотр исходных файлов"
                                                                                                                                    : this
                                                                                                                                                .computationPage
                                                                                                                                            instanceof
                                                                                                                                            LaboratoryAttachmentImportPage
                                                                                                                                        ? "Вложения · подготовка перед добавлением"
                                                                                                                                        : this
                                                                                                                                                    .computationPage
                                                                                                                                                instanceof
                                                                                                                                                LaboratoryAttachmentExportPage
                                                                                                                                            ? "Вложения · сохранение отдельного файла"
                                                                                                                                            : this
                                                                                                                                                        .computationPage
                                                                                                                                                    instanceof
                                                                                                                                                    LaboratoryAnalysisPage
                                                                                                                                                ? "Аналитика · измерения, литература и исходные файлы"
                                                                                                                                                : this
                                                                                                                                                            .computationPage
                                                                                                                                                        instanceof
                                                                                                                                                        LaboratoryReportPage
                                                                                                                                                    ? "Отчёт · схема, количества, процедура и аналитика"
                                                                                                                                                    : this
                                                                                                                                                                .computationPage
                                                                                                                                                            instanceof
                                                                                                                                                            LaboratoryStructureSearchPage
                                                                                                                                                        ? "Поиск по структуре · сохранённые опыты"
                                                                                                                                                        : this
                                                                                                                                                                    .computationPage
                                                                                                                                                                instanceof
                                                                                                                                                                LaboratorySeriesPage
                                                                                                                                                            ? "Серия опытов · варианты загрузок и условий"
                                                                                                                                                            : this
                                                                                                                                                                        .computationPage
                                                                                                                                                                    instanceof
                                                                                                                                                                    LaboratoryNotebookPage
                                                                                                                                                                ? "Проекты · параллельные опыты, ожидаемые результаты и следующие действия"
                                                                                                                                                                : this
                                                                                                                                                                            .computationPage
                                                                                                                                                                        instanceof
                                                                                                                                                                        LaboratoryOverviewPage
                                                                                                                                                                    ? "Обзор проекта · сохранённые опыты и сравнение условий"
                                                                                                                                                                    : this
                                                                                                                                                                                .computationPage
                                                                                                                                                                            instanceof
                                                                                                                                                                            LaboratoryHistoryPage
                                                                                                                                                                        ? "История сохранений · сравнение версий"
                                                                                                                                                                        : this
                                                                                                                                                                                    .computationPage
                                                                                                                                                                                instanceof
                                                                                                                                                                                LaboratoryExchangePage
                                                                                                                                                                            ? "Структуры · прочитайте и проверьте перед добавлением в опыт"
                                                                                                                                                                            : this
                                                                                                                                                                                        .computationPage
                                                                                                                                                                                    instanceof
                                                                                                                                                                                    LaboratoryReactionExchangePage
                                                                                                                                                                                ? "Импорт реакции · проверьте схему перед добавлением в опыт"
                                                                                                                                                                                : this
                                                                                                                                                                                            .computationPage
                                                                                                                                                                                        instanceof
                                                                                                                                                                                        LaboratoryWelcomePage
                                                                                                                                                                                    ? "Начало · проекты и опыты"
                                                                                                                                                                                    : this
                                                                                                                                                                                                .computationPage
                                                                                                                                                                                            != null
                                                                                                                                                                                        ? "Расчёт · неизменяемый входной снимок"
                                                                                                                                                                                        : this
                                                                                                                                                                                                .calculationArchivePage
                                                                                                                                                                                            ? "Сохранённый результат · просмотр"
                                                                                                                                                                                            : this
                                                                                                                                                                                                        .libraryPage
                                                                                                                                                                                                    != null
                                                                                                                                                                                                ? "Библиотека · структуры, фрагменты и планы опытов"
                                                                                                                                                                                                : this
                                                                                                                                                                                                            .stereoPage
                                                                                                                                                                                                        != null
                                                                                                                                                                                                    ? "Стереохимия · явное назначение конфигурации"
                                                                                                                                                                                                    : this
                                                                                                                                                                                                                .stagePage
                                                                                                                                                                                                            != null
                                                                                                                                                                                                        ? "Стадия реакции · ввод применяется кнопкой в форме"
                                                                                                                                                                                                        : this
                                                                                                                                                                                                                    .editor
                                                                                                                                                                                                                    .dirty()
                                                                                                                                                                                                                || this
                                                                                                                                                                                                                    .pending()
                                                                                                                                                                                                            ? "Есть несохранённые изменения"
                                                                                                                                                                                                            : this
                                                                                                                                                                                                                        .files
                                                                                                                                                                                                                        .path()
                                                                                                                                                                                                                    == null
                                                                                                                                                                                                                ? "Новый опыт · ещё не сохранён"
                                                                                                                                                                                                                : "Сохранено")
            + "   ·   "
            + (this.files.path() == null ? "QCHEM" : this.files.path().getFileName().toString())
            + "   ·   "
            + (this.tool.equals("Связь")
                ? (this.drawBond
                            == ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType
                                .SINGLE
                        ? "Одинарная связь"
                        : this.drawBond
                                == ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType
                                    .DOUBLE
                            ? "Двойная связь"
                            : "Тройная связь")
                    + " · "
                    + this.drawElement
                : this.tool.equals("Кольцо")
                    ? (this.aromaticRing ? "Бензольное кольцо" : "Кольцо · " + this.ringSize)
                    : this.tool.equals("Атом") ? "Атом · " + this.drawElement : this.tool)
            + (this.selectedBond == null ? "" : "   ·   Выбрана связь")
            + (this.selection.hits().size() > 1
                ? "   ·   Выделено атомов: " + this.selection.hits().size()
                : "")
            + (this.viewPreferences.warning().isBlank()
                ? ""
                : "   ·   " + this.viewPreferences.warning())
            + (this.files.warning().isBlank() ? "" : "   ·   " + this.files.warning())
            + (this.recoveryStatus.isBlank()
                    || this.recoveryStatus.equals("Нет черновиков после сбоя")
                ? ""
                : "   ·   " + this.recoveryStatus));
    this.status.setTooltip(
        new javafx.scene.control.Tooltip(
            (this.files.path() == null ? "Сохраните опыт в QCHEM" : this.files.path().toString())
                + (this.files.warning().isBlank() ? "" : "\n" + this.files.warning())));
  }

  private LaboratoryRecovery.Snapshot captureDraft() {
    final java.util.Properties fields = new java.util.Properties();
    if (this.computationPage instanceof LaboratoryNotebookPage notebook && notebook.pending()) {
      notebook.draft(fields, false);
    }
    if (this.computationPage instanceof LaboratoryScaffoldPage page) {
      page.draft(fields, false);
      if (this.scaffoldTarget != null) {
        fields.setProperty("scaffolds.component", this.scaffoldTarget.componentId());
        fields.setProperty("scaffolds.atom", this.scaffoldTarget.atomId().value());
      }
    }
    if (this.computationPage instanceof LaboratoryReactionClassificationPage page
        && page.pending()) {
      page.draft(fields, false);
    }
    if (this.computationPage instanceof LaboratorySampleStockPage page && page.pending()) {
      page.draft(fields, false);
    }
    if (this.computationPage instanceof LaboratorySampleTransferPage page && page.pending()) {
      page.draft(fields, false);
    }
    if (this.computationPage instanceof LaboratorySolutionPage page && page.pending()) {
      page.draft(fields, false);
    }
    if (this.computationPage instanceof LaboratoryStoragePage page && page.pending()) {
      page.draft(fields, false);
    }
    if (this.computationPage instanceof LaboratoryRelativeStereoPage page && page.pending()) {
      page.draft(fields, false);
    }
    if (this.computationPage instanceof LaboratoryNameStructurePage name && name.pending()) {
      name.draft(fields, false);
    }
    if (this.computationPage instanceof LaboratoryLiteraturePage literature
        && literature.pending()) {
      literature.draft(fields, false);
    }
    if (this.computationPage instanceof LaboratoryRegistryPage registry && registry.pending()) {
      registry.draft(fields, false);
    }
    if (this.computationPage instanceof LaboratoryConformerPage conformers
        && conformers.pending()) {
      conformers.draft(fields, false);
    }
    if (this.computationPage instanceof LaboratoryExperimentDesk desk && desk.pending()) {
      desk.draft(fields, false);
    }
    if (this.computationPage instanceof LaboratoryOperationsPage operations
        && operations.pending()) {
      operations.draft(fields, false);
    }
    if (this.computationPage instanceof LaboratoryEvidencePage evidence && evidence.pending()) {
      evidence.draft(fields, false);
    }
    if (this.computationPage instanceof LaboratoryDesignPage design && design.pending()) {
      design.draft(fields, false);
    }
    if (this.computationPage instanceof LaboratoryRoutePage route && route.pending()) {
      route.draft(fields, false);
    }
    if (this.computationPage instanceof LaboratoryProcedurePage procedure && procedure.pending()) {
      procedure.draft(fields, false);
    }
    if (this.computationPage instanceof LaboratoryPreparationPage preparation
        && preparation.pending()) {
      preparation.draft(fields, false);
    }
    if (this.computationPage instanceof LaboratorySeriesPage series && series.pending()) {
      series.draft(fields, false);
    }
    if (this.computationPage instanceof LaboratoryAnalysisPage analysis && analysis.pending()) {
      analysis.draft(fields, false);
    }
    if (this.computationPage instanceof LaboratoryChemDrawPage chemDraw && chemDraw.pending()) {
      chemDraw.draft(fields, false);
    }
    if (this.computationPage instanceof LaboratoryExchangePage exchange && exchange.pending()) {
      exchange.draft(fields, false);
    }
    if (this.computationPage instanceof LaboratoryReactionExchangePage reactionExchange
        && reactionExchange.pending()) {
      reactionExchange.draft(fields, false);
    }
    if (this.computationPage instanceof LaboratorySamplePage samples && samples.pending()) {
      samples.draft(fields, false);
    }
    fields.setProperty("original", this.files.path() == null ? "" : this.files.path().toString());
    if (this.folder != null) {
      fields.setProperty("folder", this.folder.toString());
    }
    this.details.draft(fields, false);
    this.journal.draft(fields, false);
    if (this.selected != null) {
      fields.setProperty("selected.component", this.selected.componentId());
      fields.setProperty("selected.atom", this.selected.atomId().value());
      this.inspector.draft(fields, false);
    }
    if (this.quantityPage != null && this.quantityPage.pending()) {
      this.quantityPage.draft(fields, false);
    }
    if (this.stagePage != null && this.stagePage.pending()) {
      this.stagePage.draft(fields, false);
    }
    if (this.stereoPage != null && this.stereoPage.pending()) {
      this.stereoPage.draft(fields, false);
    }
    if (this.structurePage != null && this.structurePage.pending()) {
      this.structurePage.draft(fields, false);
    }
    if (this.textPage != null && this.textPage.pending()) {
      this.textPage.draft(fields, false);
    }
    if (this.libraryPage != null && this.libraryPage.pending()) {
      this.libraryPage.draft(fields, false);
    }
    if (this.stoichiometryPage != null) {
      this.stoichiometryPage.draft(fields, false);
    }
    if (this.materialsPage != null && this.materialsPage.pending()) {
      this.materialsPage.draft(fields, false);
      if (this.selected != null) {
        fields.setProperty("stockForm.component", this.selected.componentId());
      }
    }
    return new LaboratoryRecovery.Snapshot(
        this.editor.document(),
        this.files.assetPath(),
        fields,
        this.editor.dirty()
            || this.pending()
            || this.computationPage instanceof LaboratoryNotebookPage notebook && notebook.pending()
            || this.computationPage instanceof LaboratoryReactionClassificationPage classification
                && classification.pending()
            || this.computationPage instanceof LaboratorySampleStockPage stock && stock.pending()
            || this.computationPage instanceof LaboratorySampleTransferPage transfer
                && transfer.pending()
            || this.computationPage instanceof LaboratoryRelativeStereoPage relative
                && relative.pending()
            || this.computationPage instanceof LaboratorySolutionPage solutions
                && solutions.pending()
            || this.computationPage instanceof LaboratoryStoragePage storage && storage.pending()
            || this.computationPage instanceof LaboratoryNameStructurePage name && name.pending()
            || this.computationPage instanceof LaboratoryLiteraturePage literature
                && literature.pending()
            || this.computationPage instanceof LaboratoryRegistryPage registry && registry.pending()
            || this.computationPage instanceof LaboratoryConformerPage conformers
                && conformers.pending()
            || this.computationPage instanceof LaboratoryExperimentDesk desk && desk.pending()
            || this.computationPage instanceof LaboratoryOperationsPage operations
                && operations.pending()
            || this.computationPage instanceof LaboratoryEvidencePage evidence && evidence.pending()
            || this.computationPage instanceof LaboratoryDesignPage design && design.pending()
            || this.computationPage instanceof LaboratoryRoutePage route && route.pending()
            || this.computationPage instanceof LaboratoryProcedurePage procedure
                && procedure.pending()
            || this.computationPage instanceof LaboratoryChemDrawPage chemDraw && chemDraw.pending()
            || this.computationPage instanceof LaboratoryPreparationPage preparation
                && preparation.pending()
            || this.computationPage instanceof LaboratoryReactionExchangePage reactionExchange
                && reactionExchange.pending()
            || this.computationPage instanceof LaboratorySamplePage samples && samples.pending()
            || this.computationPage instanceof LaboratoryExchangePage exchange && exchange.pending()
            || this.computationPage instanceof LaboratoryAnalysisPage analysis && analysis.pending()
            || this.computationPage instanceof LaboratorySeriesPage series && series.pending()
            || this.quantityPage != null && this.quantityPage.pending()
            || this.stoichiometryPage != null && this.stoichiometryPage.pending()
            || this.stagePage != null && this.stagePage.pending()
            || this.stereoPage != null && this.stereoPage.pending()
            || this.structurePage != null && this.structurePage.pending()
            || this.textPage != null && this.textPage.pending()
            || this.libraryPage != null && this.libraryPage.pending()
            || this.materialsPage != null && this.materialsPage.pending());
  }

  private void restoreDraft(
      final ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryRecoveryStore
              .Recovered
          recovered) {
    this.attention.run();
    this.files.recover(recovered);
    final var fields = recovered.fields();
    if (fields.containsKey("notebook.project")) {
      this.notebooks(false);
      if (this.computationPage instanceof LaboratoryNotebookPage page) {
        page.draft(fields, true);
      }
    }
    final String componentId = fields.getProperty("selected.component");
    final String atomId = fields.getProperty("selected.atom");
    final var layout =
        componentId == null ? null : this.editor.document().layouts().get(componentId);
    if (layout != null
        && layout.atoms().stream().anyMatch(atom -> atom.atomId().value().equals(atomId))) {
      this.selected =
          new LaboratoryCanvas.Hit(
              componentId,
              ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId.of(atomId));
      this.refresh();
      this.inspector.draft(fields, true);
    }
    this.details.draft(fields, true);
    this.journal.draft(fields, true);
    if (fields.containsKey("structureForm.kind")) {
      this.showStructure(
          fields.getProperty("structureForm.component"), fields.getProperty("structureForm.kind"));
      this.structurePage.draft(fields, true);
    }
    if (fields.containsKey("stereo.component")) {
      this.showStereo(fields.getProperty("stereo.component"), null, null);
      this.stereoPage.draft(fields, true);
    }
    if (fields.containsKey("stageForm.id")) {
      final String previous = fields.getProperty("stageForm.previous", "");
      final var stage =
          previous.isEmpty()
              ? null
              : this.editor.document().experiment().stages().stream()
                  .filter(value -> value.id().equals(previous))
                  .findFirst()
                  .orElseThrow(
                      () -> new IllegalArgumentException("Исходная стадия черновика отсутствует."));
      this.showStage(stage);
      this.stagePage.draft(fields, true);
    }
    if (fields.containsKey("folder")) {
      final Path restoredFolder = Path.of(fields.getProperty("folder"));
      if (java.nio.file.Files.isDirectory(restoredFolder)) {
        this.folder = restoredFolder;
        this.listFolder();
      }
    }
    if (fields.containsKey("libraryForm.kind")) {
      this.library();
      this.libraryPage.draft(fields, true);
    }
    if (fields.containsKey("quantityForm.component")) {
      final String stageId = fields.getProperty("quantityForm.stage");
      if (stageId == null) {
        this.showQuantities(fields.getProperty("quantityForm.component"));
        this.quantityPage.draft(fields, true);
      } else {
        this.showStageQuantities(stageId);
        this.stoichiometryPage.draft(fields, true);
      }
    }
    if (fields.containsKey("batch.stage")) {
      this.showStageQuantities(fields.getProperty("batch.stage"));
      this.stoichiometryPage.draft(fields, true);
    }
    if (fields.containsKey("stockForm.operation")) {
      final Path ledger =
          Path.of(fields.getProperty("stockForm.ledger")).toAbsolutePath().normalize();
      if (!ledger.getFileName().toString().equals("materials.qchem")
          || !java.nio.file.Files.isDirectory(ledger.getParent())) {
        throw new IllegalArgumentException(
            "Каталог исходного учёта недоступен. Восстановите его перед открытием складского черновика.");
      }
      if (Boolean.parseBoolean(fields.getProperty("stockForm.hadLedger"))
          && !java.nio.file.Files.isRegularFile(ledger)) {
        throw new IllegalArgumentException(
            "Исходный materials.qchem недоступен. Черновик сохранён; восстановите файл учёта.");
      }
      final String stockComponent = fields.getProperty("stockForm.component");
      final var molecule =
          stockComponent == null
              ? null
              : this.editor.document().experiment().components().stream()
                  .filter(component -> component.id().equals(stockComponent))
                  .findFirst()
                  .orElseThrow()
                  .molecule();
      try {
        this.showMaterials(ledger.getParent(), molecule);
        this.materialsPage.draft(fields, true);
      } catch (final java.io.IOException exception) {
        throw new java.io.UncheckedIOException(exception);
      }
    }
    if (fields.containsKey("textForm.content")) {
      this.showTextEditor();
      this.textPage.draft(fields, true);
    }
    if (fields.containsKey("scaffolds.id")) {
      this.scaffolds(
          fields.containsKey("scaffolds.component")
              ? new LaboratoryCanvas.Hit(
                  fields.getProperty("scaffolds.component"),
                  ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId.of(
                      fields.getProperty("scaffolds.atom")))
              : null);
      if (this.computationPage instanceof LaboratoryScaffoldPage page) {
        page.draft(fields, true);
      }
    }
    if (fields.containsKey("reactionClassification.source")) {
      this.classification(fields.getProperty("reactionClassification.stage"));
      if (this.computationPage instanceof LaboratoryReactionClassificationPage page) {
        page.draft(fields, true);
      }
    }
    if (fields.containsKey("sampleStock.source")) {
      this.restoreSampleStock(fields);
    }
    if (fields.containsKey("sampleTransfer.source")) {
      this.restoreSampleTransfer(fields);
    }
    if (fields.containsKey("solution.source")) {
      this.solutions();
      if (this.computationPage instanceof LaboratorySolutionPage page) {
        page.draft(fields, true);
      }
    }
    if (fields.containsKey("storage.source")) {
      this.storage();
      if (this.computationPage instanceof LaboratoryStoragePage page) {
        page.draft(fields, true);
      }
    }
    if (fields.containsKey("relative.component")) {
      this.relativeStereo(fields.getProperty("relative.component"));
      if (this.computationPage instanceof LaboratoryRelativeStereoPage page) {
        page.draft(fields, true);
      }
    }
    if (fields.containsKey("chemicalName.text")) {
      this.nameStructure();
      ((LaboratoryNameStructurePage) this.computationPage).draft(fields, true);
    }
    if (fields.containsKey("literature.source")) {
      this.literature(null);
      ((LaboratoryLiteraturePage) this.computationPage).draft(fields, true);
    }
    if (fields.containsKey("registry.source")) {
      this.registry();
      if (this.computationPage instanceof LaboratoryRegistryPage page) {
        page.draft(fields, true);
      }
    }
    if (fields.containsKey("conformers.component")) {
      this.conformers(fields.getProperty("conformers.component"));
      if (this.computationPage instanceof LaboratoryConformerPage page) {
        page.draft(fields, true);
      }
    }
    if (fields.containsKey("desk.source")) {
      this.desk(fields.getProperty("desk.stage"));
      ((LaboratoryExperimentDesk) this.computationPage).draft(fields, true);
    }
    if (fields.containsKey("operations.source")) {
      this.operations(null);
      ((LaboratoryOperationsPage) this.computationPage).draft(fields, true);
    }
    if (fields.containsKey("evidence.source")) {
      this.evidence();
      ((LaboratoryEvidencePage) this.computationPage).draft(fields, true);
    }
    if (fields.containsKey("design.source") || fields.containsKey("design.document")) {
      this.design();
      if (this.computationPage instanceof LaboratoryDesignPage page) {
        page.draft(fields, true);
      }
    }
    if (fields.containsKey("route.source")) {
      this.route(null);
      ((LaboratoryRoutePage) this.computationPage).draft(fields, true);
    }
    if (fields.containsKey("procedure.source")) {
      this.procedure(null);
      ((LaboratoryProcedurePage) this.computationPage).draft(fields, true);
    }
    if (fields.containsKey("preparation.ledger")) {
      this.prepareStock(fields.getProperty("preparation.stage"));
      if (this.computationPage instanceof LaboratoryPreparationPage preparation) {
        preparation.draft(fields, true);
      }
    }
    if (fields.containsKey("reaction-exchange.text")) {
      this.reactionExchange(false, null);
      ((LaboratoryReactionExchangePage) this.computationPage).draft(fields, true);
    }
    if (fields.containsKey("chemdraw.bytes")) {
      this.chemDraw(false);
      ((LaboratoryChemDrawPage) this.computationPage).draft(fields, true);
    }
    if (fields.containsKey("exchange.text")) {
      this.exchange(false);
      ((LaboratoryExchangePage) this.computationPage).draft(fields, true);
    }
    if (fields.containsKey("sampleForm.operation")) {
      this.samples();
      ((LaboratorySamplePage) this.computationPage).draft(fields, true);
    }
    if (fields.containsKey("analysis.method")) {
      this.analyses();
      ((LaboratoryAnalysisPage) this.computationPage).draft(fields, true);
    }
    if (fields.containsKey("series.stage")) {
      this.series();
      ((LaboratorySeriesPage) this.computationPage).draft(fields, true);
    }
    this.recoveryStatus = "Восстановлен черновик; сохраните опыт в выбранный файл";
    this.status();
  }

  private void showTextEditor() {
    if (this.textPage != null) {
      this.textPage.dispose();
    }
    this.textPage = new LaboratoryTextEditor(this.editor, this::reload);
    this.root.setCenter(this.textPage);
    this.root.getTop().setVisible(false);
    this.root.getTop().setManaged(false);
    this.status();
  }

  private void prepareGeometry() {
    this.run(
        () -> {
          if (this.selected == null) {
            throw new IllegalArgumentException("Выберите компонент для подготовки 3D.");
          }
          final var component =
              this.editor.document().experiment().components().stream()
                  .filter(item -> item.id().equals(this.selected.componentId()))
                  .findFirst()
                  .orElseThrow();
          if (this.files.path() == null && !this.files.save(false)) {
            return;
          }
          final String experimentId = this.editor.document().experiment().id();
          this.computationPage =
              new LaboratoryInitialGeometryWindow(
                  component,
                  this.editor.document().layouts().get(component.id()),
                  experimentId,
                  this.files.path().getParent().resolve(".calculations"),
                  (file, geometry) -> {
                    try {
                      this.requireCalculationSource(experimentId, component);
                      final var attachment =
                          new ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure
                                  .LaboratoryAssetStore()
                              .stage(file);
                      this.editor.applyGeometry(
                          component.id(),
                          component.molecule(),
                          geometry,
                          "Применена начальная 3D-раскладка по графу; без оптимизации. Снимок: "
                              + file.getFileName());
                      this.editor.attach(attachment);
                      this.selected =
                          new LaboratoryCanvas.Hit(
                              component.id(), geometry.atoms().getFirst().id());
                    } catch (final java.io.IOException exception) {
                      throw new IllegalStateException(exception.getMessage(), exception);
                    }
                  },
                  this::closeComputation);
          this.openComputation();
        });
  }

  private void predictedNmr() {
    this.run(
        () -> {
          if (this.editor.document().experiment().components().isEmpty()) {
            this.status.setText("Сначала нарисуйте структуру для прогноза ЯМР.");
            return;
          }
          if (this.computationPage != null && !this.computationPage.canClose()) {
            return;
          }
          this.flushAll();
          if (this.computationPage != null) {
            this.computationPage.dispose();
          }
          this.computationPage =
              new LaboratoryNmrPredictionPage(
                  this.editor,
                  this.selected == null ? null : this.selected.componentId(),
                  this::closeComputation);
          this.openComputation();
        });
  }

  private void calculation() {
    this.calculation(
        ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind
            .DESCRIPTOR_ANALYSIS);
  }

  private void calculation(
      final ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation
              .ClassicalCalculationKind
          initialKind) {
    this.run(
        () -> {
          if (this.selected == null) {
            throw new IllegalArgumentException("Выберите компонент для расчёта.");
          }
          final var component =
              this.editor.document().experiment().components().stream()
                  .filter(item -> item.id().equals(this.selected.componentId()))
                  .findFirst()
                  .orElseThrow();
          if (this.files.path() == null && !this.files.save(false)) {
            return;
          }
          final String experimentId = this.editor.document().experiment().id();
          this.computationPage =
              new LaboratoryCalculationWindow(
                  component,
                  experimentId,
                  this.files.path().getParent().resolve(".calculations"),
                  result -> {
                    try {
                      if (!experimentId.equals(this.editor.document().experiment().id())) {
                        throw new IllegalArgumentException(
                            "Открыт другой опыт; результат сохранён отдельно.");
                      }
                      this.editor.attach(
                          new ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure
                                  .LaboratoryAssetStore()
                              .stage(result.file()));
                      this.editor.record(
                          ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryEntry
                              .Kind.ACTION,
                          "Расчёт "
                              + result.file().getFileName()
                              + ": "
                              + result.status()
                              + "; структура "
                              + component.molecule().displayName(),
                          null);
                      this.refresh();
                    } catch (final java.io.IOException exception) {
                      throw new IllegalStateException(exception.getMessage(), exception);
                    }
                  },
                  geometry -> {
                    this.requireCalculationSource(experimentId, component);
                    this.editor.applyGeometry(
                        component.id(),
                        component.molecule(),
                        geometry,
                        "Применена геометрия приближённой силовой модели; источник "
                            + geometry.id().value()
                            + ". Сходимость указана в отчёте расчёта.");
                    this.selected =
                        new LaboratoryCanvas.Hit(component.id(), geometry.atoms().getFirst().id());
                  },
                  initialKind,
                  this::closeComputation);
          this.openComputation();
        });
  }

  private void history() {
    this.run(
        () -> {
          this.flushAll();
          if (this.files.path() == null && !this.files.save(false)) {
            return;
          }
          final Path path = this.files.path();
          this.computationPage =
              new LaboratoryHistoryPage(
                  this.editor, path, () -> path.equals(this.files.path()), this::closeComputation);
          this.openComputation();
        });
  }

  private void predictStage(final LaboratoryReactionStage stage) {
    this.run(
        () -> {
          this.flushAll();
          if (this.files.path() == null && !this.files.save(false)) {
            return;
          }
          final String experimentId = this.editor.document().experiment().id();
          this.computationPage =
              new LaboratoryPredictionWindow(
                  this.editor,
                  stage,
                  this.files.path().getParent().resolve(".calculations"),
                  result -> {
                    if (!experimentId.equals(this.editor.document().experiment().id())) {
                      throw new IllegalArgumentException(
                          "Открыт другой опыт; прогноз сохранён отдельно.");
                    }
                    try {
                      this.editor.attach(
                          new ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure
                                  .LaboratoryAssetStore()
                              .stage(result.file()));
                      this.editor.record(
                          ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryEntry
                              .Kind.ACTION,
                          "Прогноз стадии «"
                              + stage.title()
                              + "»: "
                              + result.status()
                              + "; "
                              + result.file().getFileName(),
                          null);
                      this.refresh();
                    } catch (final java.io.IOException exception) {
                      throw new IllegalStateException(exception.getMessage(), exception);
                    }
                  },
                  this::closeComputation);
          this.openComputation();
        });
  }

  private void requireCalculationSource(
      final String experimentId, final LaboratoryComponent source) {
    if (!experimentId.equals(this.editor.document().experiment().id())
        || this.editor.document().experiment().components().stream()
            .noneMatch(c -> c.id().equals(source.id()) && c.molecule().equals(source.molecule()))) {
      throw new IllegalArgumentException(
          "Исходная структура изменилась. Откройте расчёт заново для текущей структуры.");
    }
  }

  private void scaffolds(final LaboratoryCanvas.Hit target) {
    this.run(
        () -> {
          if (this.computationPage != null && !this.computationPage.canClose()) {
            return;
          }
          this.flushAll();
          final var expected = this.editor.document();
          this.scaffoldTarget = target;
          if (this.computationPage != null) {
            this.computationPage.dispose();
          }
          this.computationPage =
              new LaboratoryScaffoldPage(
                  expected,
                  target == null ? null : target.componentId(),
                  target == null ? null : target.atomId(),
                  drawing -> {
                    if (!expected.equals(this.editor.document())) {
                      throw new IllegalArgumentException(
                          "Опыт изменился; откройте построение каркаса заново.");
                    }
                    if (target == null) {
                      this.selectAdded(drawing);
                    } else {
                      this.selected = target;
                      this.replace(drawing);
                    }
                    this.leaveComputation();
                    this.chooseComponent(this.selected.componentId(), true);
                    this.refresh();
                  },
                  this::closeComputation);
          this.openComputation();
        });
  }

  private void collapseGroup() {
    this.run(
        () -> {
          if (this.selected == null) {
            throw new IllegalArgumentException(
                "Выделите терминальную группу атомов для сокращённой подписи.");
          }
          final String component = this.selected.componentId();
          final var hits = this.selection.hits();
          if (hits.stream().anyMatch(h -> !h.componentId().equals(component))) {
            throw new IllegalArgumentException("Сокращение создаётся внутри одной структуры.");
          }
          final var atoms =
              hits.isEmpty()
                  ? Set.of(this.selected.atomId())
                  : hits.stream()
                      .map(LaboratoryCanvas.Hit::atomId)
                      .collect(java.util.stream.Collectors.toSet());
          final var dialog = new javafx.scene.control.TextInputDialog();
          dialog.initOwner(this.owner);
          dialog.setTitle("Сокращённая группа");
          dialog.setHeaderText("Подпись выбранного фрагмента");
          dialog.setContentText(
              "Например: Me, Et, Ph, Boc. Подпись задаёте вы; все атомы сохраняются.");
          dialog
              .showAndWait()
              .ifPresent(
                  label -> {
                    final var next =
                        ru.pathcreator.vadim.quantum.chemistry.drawing.application
                            .MoleculeAbbreviations.collapse(this.layout(), atoms, label);
                    this.editor.editComponents(java.util.Map.of(component, next), Set.of());
                    final var group =
                        ru
                            .pathcreator
                            .vadim
                            .quantum
                            .chemistry
                            .drawing
                            .application
                            .MoleculeAbbreviations
                            .groups(next)
                            .stream()
                            .filter(g -> g.atoms().containsAll(atoms))
                            .findFirst()
                            .orElseThrow();
                    this.selected = new LaboratoryCanvas.Hit(component, group.anchor());
                    this.selection.sync(null);
                    this.selection.sync(this.selected);
                  });
        });
  }

  private void expandGroups(final LaboratoryCanvas.Hit target, final boolean all) {
    this.run(
        () -> {
          if (target == null) {
            throw new IllegalArgumentException("Выберите структуру со свёрнутыми группами.");
          }
          final var drawing = this.editor.document().layouts().get(target.componentId());
          final var next =
              all
                  ? ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeAbbreviations
                      .expandAll(drawing)
                  : ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeAbbreviations
                      .expand(drawing, target.atomId());
          this.editor.editComponents(java.util.Map.of(target.componentId(), next), Set.of());
          this.selected = target;
        });
  }

  private Path materialFolder() {
    if (this.files.path() == null && !this.files.save(false)) {
      return null;
    }
    try {
      return ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure
          .LaboratoryInventoryRepository.projectFolder(this.files.path());
    } catch (final java.io.IOException exception) {
      throw new IllegalStateException(exception.getMessage(), exception);
    }
  }

  private void solutions(final String batchId) {
    final var previous = this.computationPage;
    this.solutions();
    if (this.computationPage != previous
        && this.computationPage instanceof LaboratorySolutionPage page) {
      page.selectBatch(batchId);
    }
  }

  private void solutions() {
    this.run(
        () -> {
          if (this.computationPage != null && !this.computationPage.canClose()) {
            return;
          }
          this.flushAll();
          final Path parent = this.materialFolder();
          if (parent == null) {
            return;
          }
          if (this.computationPage != null) {
            this.computationPage.dispose();
          }
          this.computationPage =
              new LaboratorySolutionPage(this.editor, parent, this::closeComputation);
          this.openComputation();
        });
  }

  private void storage() {
    this.run(
        () -> {
          if (this.computationPage != null && !this.computationPage.canClose()) {
            return;
          }
          this.flushAll();
          final Path parent = this.materialFolder();
          if (parent == null) {
            return;
          }
          if (this.computationPage != null) {
            this.computationPage.dispose();
          }
          this.computationPage =
              new LaboratoryStoragePage(
                  this.editor,
                  parent,
                  (file, sampleId) -> {
                    if (this.computationPage != null && !this.computationPage.canClose()) {
                      return;
                    }
                    this.leaveComputation();
                    if (file != null) {
                      this.files.open(file);
                      if (!file.toAbsolutePath().normalize().equals(this.files.path())) {
                        return;
                      }
                    }
                    this.samples();
                    if (this.computationPage instanceof LaboratorySamplePage page) {
                      page.showSample(sampleId);
                    }
                  },
                  () -> this.files.save(false),
                  this::closeComputation);
          this.openComputation();
        });
  }

  private void nameStructure() {
    this.run(
        () -> {
          if (this.computationPage != null && !this.computationPage.canClose()) {
            return;
          }
          this.flushAll();
          if (this.computationPage != null) {
            this.computationPage.dispose();
          }
          this.computationPage =
              new LaboratoryNameStructurePage(this.editor, this::closeComputation);
          this.openComputation();
        });
  }

  private void relativeStereo(final String componentId) {
    this.run(
        () -> {
          if (this.computationPage != null && !this.computationPage.canClose()) {
            return;
          }
          this.flushAll();
          final String id =
              componentId != null
                  ? componentId
                  : this.selected == null ? null : this.selected.componentId();
          if (id == null) {
            throw new IllegalArgumentException(
                "Выберите вещество для записи относительной стереохимии.");
          }
          if (this.computationPage != null) {
            this.computationPage.dispose();
          }
          this.computationPage =
              new LaboratoryRelativeStereoPage(
                  this.editor,
                  id,
                  this.selected != null && id.equals(this.selected.componentId())
                      ? this.selected.atomId()
                      : null,
                  this::closeComputation);
          this.openComputation();
        });
  }

  private void quickOperation(final String action) {
    final LaboratoryComputationPage previous = this.computationPage;
    this.operations(null);
    if (this.computationPage != previous
        && this.computationPage instanceof LaboratoryOperationsPage page) {
      page.openAction(action);
    }
  }

  private void cip() {
    this.run(
        () -> {
          if (this.computationPage != null && !this.computationPage.canClose()) {
            return;
          }
          this.flushAll();
          if (this.selected == null) {
            throw new IllegalArgumentException(
                "Выберите структуру для расчёта стереодескрипторов.");
          }
          if (this.computationPage != null) {
            this.computationPage.dispose();
          }
          this.computationPage =
              new LaboratoryCipPage(
                  this.editor, this.selected.componentId(), this::closeComputation);
          this.openComputation();
        });
  }

  private void classification(final String stageId) {
    this.run(
        () -> {
          if (this.computationPage != null && !this.computationPage.canClose()) {
            return;
          }
          this.flushAll();
          if (this.computationPage != null) {
            this.computationPage.dispose();
          }
          this.computationPage =
              new LaboratoryReactionClassificationPage(
                  this.editor, stageId, this::closeComputation);
          this.openComputation();
        });
  }

  private void literature(final String stageId) {
    this.run(
        () -> {
          if (this.computationPage != null && !this.computationPage.canClose()) {
            return;
          }
          this.flushAll();
          if (this.computationPage != null) {
            this.computationPage.dispose();
          }
          this.computationPage =
              new LaboratoryLiteraturePage(
                  this.editor, stageId, () -> this.files.save(false), this::closeComputation);
          this.openComputation();
        });
  }

  private void registry() {
    this.registry(null);
  }

  private void registry(final String componentId) {
    this.run(
        () -> {
          if (this.computationPage != null && !this.computationPage.canClose()) {
            return;
          }
          this.flushAll();
          if (this.folder == null && this.files.path() != null) {
            this.folder = this.files.path().getParent();
          }
          if (this.folder == null) {
            this.chooseFolder();
          }
          if (this.folder == null) {
            return;
          }
          if (this.computationPage != null) {
            this.computationPage.dispose();
          }
          final var page =
              new LaboratoryRegistryPage(
                  this.editor, this.folder, this.owner, this::closeComputation);
          page.participant(
              id ->
                  this.run(
                      () -> {
                        this.leaveComputation();
                        if (this.computationPage == null) {
                          this.chooseComponent(id, true);
                        }
                      }));
          if (componentId != null) {
            page.forComponent(componentId);
          }
          this.computationPage = page;
          this.openComputation();
        });
  }

  private void conformers(final String componentId) {
    this.run(
        () -> {
          if (this.computationPage != null && !this.computationPage.canClose()) {
            return;
          }
          this.flushAll();
          final String id =
              componentId != null
                  ? componentId
                  : this.selected == null ? null : this.selected.componentId();
          if (id == null) {
            throw new IllegalArgumentException("Выберите вещество для работы с конформерами.");
          }
          if (this.files.path() == null && !this.files.save(false)) {
            return;
          }
          if (this.computationPage != null) {
            this.computationPage.dispose();
          }
          this.computationPage =
              new LaboratoryConformerPage(
                  this.editor,
                  id,
                  this.files.path().getParent().resolve(".conformers"),
                  this.owner,
                  this::closeComputation);
          this.openComputation();
        });
  }

  private void commands() {
    if (this.files.busy()) {
      return;
    }
    final var commands = new java.util.ArrayList<LaboratoryCommandPalette.Command>();
    commands.add(
        new LaboratoryCommandPalette.Command(
            "notebook",
            "Проекты и параллельные опыты",
            "Проекты",
            "Весенние синтезы, разделы, ход работы, ожидание ЯМР, следующее действие",
            this::notebooks));
    commands.add(
        new LaboratoryCommandPalette.Command(
            "desk",
            "Рабочий экран опыта",
            "Опыт",
            "Реакция, загрузка, методика, наблюдения и фактический результат вместе",
            () -> this.desk(null)));
    commands.add(
        new LaboratoryCommandPalette.Command(
            "route",
            "Карта синтеза и расчёт от цели",
            "Планирование",
            "Разветвления, промежуточные продукты, выходы, нужная конечная масса",
            () -> this.route(null)));
    commands.add(
        new LaboratoryCommandPalette.Command(
            "design",
            "Матрица условий и DoE",
            "Планирование",
            "Оптимизация, растворители, основания, температура, время, параллельный синтез, виалы",
            this::design));
    commands.add(
        new LaboratoryCommandPalette.Command(
            "procedure",
            "Методика и проведение",
            "Опыт",
            "Добавления, нагрев, охлаждение, выдержка, таймер, начать и завершить шаг",
            () -> this.procedure(null)));
    commands.add(
        new LaboratoryCommandPalette.Command(
            "operations",
            "Контроль, обработка и очистка",
            "Опыт",
            "Проба, конверсия, экстракция, промывка, сушка, колонка, фракции и объединение",
            () -> this.operations(null)));
    commands.add(
        new LaboratoryCommandPalette.Command(
            "transfer",
            "Образец в следующий опыт",
            "Образцы",
            "Следующая стадия, количество, происхождение и аналитические данные",
            this::sampleTransfer));
    commands.add(
        new LaboratoryCommandPalette.Command(
            "samples",
            "Образцы и фракции",
            "Образцы",
            "Аликвота, объединение, остаток, положение и история образца",
            this::samples));
    commands.add(
        new LaboratoryCommandPalette.Command(
            "reaction-search",
            "Найти реакцию и повторить методику",
            "Знания",
            "Исходный фрагмент, продукт, условия, результат, поиск и повтор сохранённого опыта",
            this::reactionSearch));
    commands.add(
        new LaboratoryCommandPalette.Command(
            "analyses",
            "ЯМР, МС и ТСХ",
            "Аналитика",
            "Собственные спектры, загрузить FID, обработка, сигналы и назначения",
            this::analyses));
    commands.add(
        new LaboratoryCommandPalette.Command(
            "evidence",
            "Подтверждение продукта и количественная аналитика",
            "Аналитика",
            "Двумерный ЯМР, HSQC, HMBC, COSY, qNMR, хроматограмма, чистота и досье",
            this::evidence));
    commands.add(
        new LaboratoryCommandPalette.Command(
            "conformers",
            "Сравнить конформеры",
            "Структура",
            "3D варианты, поворот связи, расстояния, углы и сохранённые виды",
            () -> this.conformers(null)));
    commands.add(
        new LaboratoryCommandPalette.Command(
            "registry",
            "Реестр соединений",
            "Знания",
            "Постоянные номера, дубликаты, соли, сольваты, таутомеры, стереоизомеры",
            this::registry));
    commands.add(
        new LaboratoryCommandPalette.Command(
            "reaction-classification",
            "Название реакции и механизмы",
            "Опыт",
            "Указать несколько механизмов, SN1, SN2, E2, именная реакция, Сузуки, свои названия",
            () -> this.classification(null)));
    commands.add(
        new LaboratoryCommandPalette.Command(
            "literature",
            "Литература и методики",
            "Знания",
            "Статьи, DOI, supporting information, литературный выход и перенос методики",
            () -> this.literature(null)));
    commands.add(
        new LaboratoryCommandPalette.Command(
            "name-structure",
            "Построить структуру по названию",
            "Структура",
            "IUPAC, систематическое название, гетероциклы, локальный OPSIN",
            this::nameStructure));
    commands.add(
        new LaboratoryCommandPalette.Command(
            "solutions",
            "Приготовить раствор или разведение",
            "Материалы",
            "Концентрация, навеска, объём, стандартизация, последовательные разведения и остатки",
            this::solutions));
    commands.add(
        new LaboratoryCommandPalette.Command(
            "storage",
            "Найти вещество на карте хранения",
            "Материалы",
            "Шкаф, полка, коробка, позиция виалы, срок годности, вскрытие",
            this::storage));
    commands.add(
        new LaboratoryCommandPalette.Command(
            "scaffolds",
            "Добавить сложный циклический каркас",
            "Структура",
            "Конденсированные, мостиковые, спироциклы и гетероциклы",
            () -> this.scaffolds(null)));
    commands.add(
        new LaboratoryCommandPalette.Command(
            "repeat",
            "Повторить опыт на другом масштабе",
            "Опыт",
            "Новый опыт с прежним планом и методикой; факты и образцы начинаются заново",
            this.files::repeat));
    commands.add(
        new LaboratoryCommandPalette.Command(
            "control-sample",
            "Записать контрольную пробу",
            "Опыт",
            "Время отбора, измеренная конверсия, ТСХ, ход реакции",
            () -> this.quickOperation("sample")));
    commands.add(
        new LaboratoryCommandPalette.Command(
            "repeat-purification",
            "Повторить очистку",
            "Опыт",
            "Выбрать очистку и повторить её план без прежних фактических фракций",
            () -> this.quickOperation("repeat-purification")));
    commands.add(
        new LaboratoryCommandPalette.Command(
            "relative-stereo",
            "Относительная стереохимия и рацемат",
            "Структура",
            "R*/S*, связанные центры, неизвестная абсолютная конфигурация",
            () -> this.relativeStereo(null)));
    commands.add(
        new LaboratoryCommandPalette.Command(
            "cip",
            "Рассчитать R/S и E/Z",
            "Структура",
            "CIP, стереохимия, клинья, импортированная конфигурация и дескрипторы",
            this::cip));
    LaboratoryCommandPalette.show(
        this.owner,
        commands.stream()
            .map(
                c ->
                    new LaboratoryCommandPalette.Command(
                        c.id(), c.name(), c.section(), c.hint(), () -> this.command(c.action())))
            .toList(),
        this.viewPreferences);
  }

  private void command(final Runnable action) {
    this.run(
        () -> {
          if (this.computationPage != null) {
            if (!this.computationPage.canClose()) {
              return;
            }
            this.leaveComputation();
          }
          if (this.root.getCenter() != this.workArea) {
            this.files.error(
                new IllegalArgumentException(
                    "Сначала примените или отмените ввод в открытой форме и вернитесь к опыту."));
            return;
          }
          this.flushAll();
          action.run();
        });
  }

  private void welcome() {
    if (this.computationPage != null && !this.computationPage.canClose()) {
      return;
    }
    if (this.computationPage == null && this.root.getCenter() != this.workArea) {
      return;
    }
    this.flushAll();
    this.rememberNotebook();
    if (this.computationPage != null) {
      this.computationPage.dispose();
    }
    final var page =
        new LaboratoryWelcomePage(
            this.notebookRepository,
            this.files.session().recent(),
            new LaboratoryWelcomePage.Actions(
                () -> this.welcomeProjects(null, true),
                () ->
                    this.run(
                        () -> {
                          this.startupInteracted = true;
                          if (this.files.openDialog()) {
                            this.desk(null);
                          }
                        }),
                () ->
                    this.run(
                        () -> {
                          this.startupInteracted = true;
                          if (this.files.createForProject()) {
                            this.desk(null);
                          }
                        }),
                () -> this.welcomeProjects(null, false),
                project -> this.welcomeProjects(project, false),
                path ->
                    this.run(
                        () -> {
                          this.startupInteracted = true;
                          if (this.files.openChecked(path)) {
                            this.desk(null);
                          }
                        }),
                () -> {
                  this.startupInteracted = true;
                  this.recovery.browse();
                },
                this.files.path() != null || this.editor.dirty()
                    ? () -> {
                      this.startupInteracted = true;
                      this.closeComputation();
                    }
                    : null));
    page.pendingDrafts(this.recovery.pendingDrafts());
    this.computationPage = page;
    this.openComputation();
  }

  private void welcomeProjects(
      final ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure
              .LaboratoryNotebookRepository.Project
          project,
      final boolean create) {
    this.startupInteracted = true;
    this.notebooks(false);
    if (this.computationPage instanceof LaboratoryNotebookPage page) {
      page.backCaption("← К началу");
      this.navigationReturn =
          new ReturnTarget(page, this.editor.document().experiment().id(), this::welcome);
      if (create) {
        page.beginNewProject();
      } else if (project != null) {
        page.restoreView(
            new LaboratoryNotebookPage.ViewState(project.id(), null, null, "ACTIVE", ""));
      }
    }
  }

  private void notebooks() {
    this.notebooks(true);
  }

  private void notebooks(final boolean saveCurrent) {
    this.run(
        () -> {
          if (this.computationPage != null && !this.computationPage.canClose()) {
            return;
          }
          if (this.computationPage == null && this.root.getCenter() != this.workArea) {
            this.files.error(
                new IllegalArgumentException(
                    "Сначала примените или отмените ввод в открытой форме."));
            return;
          }
          this.flushAll();
          if (saveCurrent
              && this.files.path() != null
              && this.editor.dirty()
              && !this.files.save(false)) {
            return;
          }
          this.rememberNotebook();
          if (this.computationPage != null) {
            this.computationPage.dispose();
          }
          final var page =
              new LaboratoryNotebookPage(
                  this.notebookRepository, this::openNotebookExperiment, this::closeComputation);
          page.planSource(() -> this.editor.document());
          page.actions(this::openNotebookAction);
          this.computationPage = page;
          this.openComputation();
          if (this.notebookView != null) {
            page.restoreView(this.notebookView);
          }
        });
  }

  private void rememberNotebook() {
    if (this.computationPage instanceof LaboratoryNotebookPage page) {
      this.notebookView = page.viewState();
    }
  }

  private void openNotebookExperiment(final Path path) {
    this.loadNotebookExperiment(path);
  }

  private boolean loadNotebookExperiment(final Path path) {
    if (!(this.computationPage instanceof LaboratoryNotebookPage page) || !page.canClose()) {
      return false;
    }
    this.rememberNotebook();
    this.flushAll();
    if (!this.files.openChecked(path)) {
      return false;
    }
    this.folder = path.toAbsolutePath().normalize().getParent();
    this.files.preferredDirectory(this.folder);
    this.desk(null);
    return true;
  }

  private void openNotebookAction(final Path path, final String action) {
    if (!List.of("procedure", "operations", "analyses", "evidence", "samples").contains(action)) {
      return;
    }
    if (this.loadNotebookExperiment(path)) {
      this.deskAction(action);
    }
  }

  private void desk(final String stageId) {
    this.desk(stageId, null);
  }

  private void desk(final String stageId, final LaboratoryExperimentDesk.ViewState view) {
    this.run(
        () -> {
          if (this.computationPage != null && !this.computationPage.canClose()) {
            return;
          }
          this.flushAll();
          if (this.computationPage != null) {
            this.computationPage.dispose();
          }
          final var page =
              new LaboratoryExperimentDesk(
                  this.editor,
                  stageId,
                  () -> this.files.save(false),
                  this::deskAction,
                  this::closeComputation);
          page.preferences(this.viewPreferences, this.files::path);
          page.saving(saveAs -> this.files.save(saveAs));
          page.projectNavigation(this::notebooks);
          this.computationPage = page;
          this.openComputation();
          if (view != null) {
            page.restoreView(view);
          }
        });
  }

  private String stageProduct(final String stageId) {
    final var stage =
        this.editor.document().experiment().stages().stream()
            .filter(s -> s.id().equals(stageId))
            .findFirst()
            .orElse(null);
    final var products =
        stage == null
            ? List.<String>of()
            : stage.participants().stream()
                .filter(p -> p.role() == ReactionComponentRole.PRODUCT)
                .map(p -> p.componentId())
                .distinct()
                .toList();
    return products.size() == 1 ? products.getFirst() : null;
  }

  private void openWorkbenchFile() {
    if (this.files.busy()
        || !(this.computationPage instanceof LaboratoryExperimentDesk
            || this.computationPage instanceof LaboratoryNotebookPage)
        || !this.computationPage.canClose()) {
      return;
    }
    this.rememberNotebook();
    try {
      this.flushAll();
      if (this.files.openDialog()) {
        this.folder = this.files.path().getParent();
        this.files.preferredDirectory(this.folder);
        this.desk(null);
      }
    } catch (final RuntimeException exception) {
      this.files.error(exception);
    }
  }

  private void returnFromDrawing() {
    this.run(
        () -> {
          this.flushAll();
          final var target = this.drawingReturn;
          if (target != null
              && target.experimentId().equals(this.editor.document().experiment().id())) {
            target.action().run();
          } else {
            this.desk(null);
          }
        });
  }

  private void drawingConditions() {
    this.run(
        () -> {
          this.flushAll();
          final var stages = this.editor.document().experiment().stages();
          final var stage =
              stages.stream()
                  .filter(value -> value.id().equals(this.drawingStageId))
                  .findFirst()
                  .orElse(stages.isEmpty() ? null : stages.getFirst());
          this.showStage(stage);
        });
  }

  private void deskAction(final String action) {
    if ("open-file".equals(action)) {
      this.openWorkbenchFile();
      return;
    }
    if (!(this.computationPage instanceof LaboratoryExperimentDesk desk) || !desk.canClose()) {
      return;
    }
    this.flushAll();
    final var view = desk.viewState();
    final String activeStage = view.stageId(),
        experimentId = this.editor.document().experiment().id();
    this.leaveComputation();
    if (this.computationPage != null) {
      return;
    }
    final Runnable back = () -> this.desk(activeStage, view);
    if ("draw-structures".equals(action)) {
      this.drawingReturn = new ReturnTarget(this.workArea, experimentId, back);
      this.drawingStageId = activeStage;
      this.drawingTabs.getSelectionModel().selectFirst();
      this.startDismissed = true;
      this.updateStartPane();
      this.chooseTool("Связь");
      this.canvas.getContent().requestFocus();
      this.status.setText(
          "Нарисуйте исходники на листе. Затем нажмите «Участники и условия». Названия знать не обязательно.");
      return;
    }
    if (action.startsWith("expected-nmr:")) {
      this.computationPage =
          new LaboratoryNmrPredictionPage(
              this.editor, action.substring(13), this::closeComputation);
      this.openComputation();
    } else if (action.startsWith("properties:")) {
      this.showQuantities(action.substring(11));
    } else if (action.startsWith("sample:")) {
      this.samples(activeStage, this.stageProduct(activeStage), false, action.substring(7));
    } else {
      switch (action) {
        case "report" -> {
          this.computationPage =
              new LaboratoryReportPage(
                  this.editor.document(),
                  java.util.Map.of(
                      activeStage, java.util.Objects.toString(desk.resultReference(), "")),
                  this::closeComputation);
          this.openComputation();
        }
        case "route" -> this.route(activeStage);
        case "procedure" -> this.procedure(activeStage);
        case "procedure-all" -> this.procedure(null);
        case "samples" -> this.samples(activeStage, this.stageProduct(activeStage), false, null);
        case "register-sample" ->
            this.samples(activeStage, this.stageProduct(activeStage), true, null);
        case "analyses" -> this.analyses(this.stageProduct(activeStage));
        case "evidence" -> this.evidenceForStage(activeStage);
        case "operations" -> this.operations(activeStage);
        case "literature" -> this.literature(activeStage);
        case "classification" -> this.classification(activeStage);
        case "transfer" -> this.sampleTransfer();
        case "stage" -> this.showStage(null);
        case "edit-stage" ->
            this.showStage(
                this.editor.document().experiment().stages().stream()
                    .filter(s -> s.id().equals(activeStage))
                    .findFirst()
                    .orElse(null));
        case "enter-smiles" -> this.exchange(false);
        case "import-reaction" -> this.reactionExchange(false, null);
        default -> {}
      }
    }
    if (this.editor.document().experiment().id().equals(experimentId)) {
      if (this.root.getCenter() != this.workArea) {
        this.navigationReturn = new ReturnTarget(this.root.getCenter(), experimentId, back);
        if (this.computationPage != null) {
          this.computationPage.backCaption("← К опыту");
        }
        if (this.stagePage != null) {
          this.stagePage.navigationContext(
              this.editor.document().experiment().title(), "← К опыту");
        }
      } else {
        back.run();
      }
    }
  }

  private void operations(final String stageId) {
    this.run(
        () -> {
          if (this.computationPage != null && !this.computationPage.canClose()) {
            return;
          }
          this.flushAll();
          if (this.computationPage != null) {
            this.computationPage.dispose();
          }
          this.computationPage =
              new LaboratoryOperationsPage(
                  this.editor, stageId, () -> this.files.save(false), this::closeComputation);
          this.openComputation();
        });
  }

  private void evidenceForStage(final String stageId) {
    final var stage =
        this.editor.document().experiment().stages().stream()
            .filter(s -> s.id().equals(stageId))
            .findFirst()
            .orElse(null);
    final var products =
        stage == null
            ? List.<String>of()
            : stage.participants().stream()
                .filter(p -> p.role() == ReactionComponentRole.PRODUCT)
                .map(p -> p.componentId())
                .distinct()
                .toList();
    this.evidence(products.size() == 1 ? products.getFirst() : null, true);
  }

  private void evidence() {
    this.evidence(this.selected == null ? null : this.selected.componentId(), false);
  }

  private void evidence(final String componentId, final boolean dossier) {
    this.run(
        () -> {
          if (this.computationPage != null && !this.computationPage.canClose()) {
            return;
          }
          this.flushAll();
          if (this.computationPage != null) {
            this.computationPage.dispose();
          }
          final var page =
              new LaboratoryEvidencePage(
                  this.editor, componentId, this.owner, this::closeComputation);
          if (dossier) {
            page.openDossier(componentId);
          }
          page.saving(
              saveAs -> {
                if (page.pending()) {
                  page.status.setText("Сначала добавьте аналитическую запись или сбросьте ввод.");
                  return;
                }
                if (this.files.save(saveAs)) {
                  page.status.setText("Опыт с аналитическими результатами сохранён.");
                }
              });
          this.computationPage = page;
          this.openComputation();
        });
  }

  private void sampleEvidence(final String sampleId, final String entryId, final boolean dossier) {
    final var previous = this.computationPage;
    this.evidence();
    if (this.computationPage != previous
        && this.computationPage instanceof LaboratoryEvidencePage page) {
      if (dossier) {
        page.dossierForSample(sampleId);
      } else {
        page.showForSample(sampleId, entryId);
      }
    }
  }

  private void sampleStock(final String sampleId) {
    this.run(
        () -> {
          if (this.computationPage != null && !this.computationPage.canClose()) {
            return;
          }
          this.flushAll();
          if (!this.files.save(false) || this.files.path() == null) {
            return;
          }
          final Path source = this.files.path();
          final Path parent = this.materialFolder();
          if (parent == null) {
            return;
          }
          if (this.computationPage != null) {
            this.computationPage.dispose();
          }
          this.computationPage =
              new LaboratorySampleStockPage(
                  this.editor,
                  source,
                  parent,
                  sampleId,
                  () -> this.files.refreshExternalSave(source),
                  this::solutions,
                  this::closeComputation);
          this.openComputation();
        });
  }

  private void restoreSampleStock(final java.util.Properties fields) {
    final Path source =
        Path.of(fields.getProperty("sampleStock.source")).toAbsolutePath().normalize();
    if (this.files.assetPath() == null
        || !source.equals(this.files.assetPath().toAbsolutePath().normalize())) {
      throw new IllegalArgumentException("Черновик передачи относится к другому исходному файлу.");
    }
    try {
      final Path parent =
          ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure
              .LaboratoryInventoryRepository.projectFolder(source);
      final var recovered = this.editor.document();
      final var page =
          new LaboratorySampleStockPage(
              this.editor,
              source,
              parent,
              fields.getProperty("sampleStock.sample"),
              () -> this.files.refreshRecoveredExternalSave(source, recovered),
              this::solutions,
              this::closeComputation);
      try {
        page.draft(fields, true);
      } catch (final RuntimeException failure) {
        page.dispose();
        throw failure;
      }
      if (this.computationPage != null) {
        this.computationPage.dispose();
      }
      this.computationPage = page;
      this.openComputation();
    } catch (final java.io.IOException failure) {
      throw new java.io.UncheckedIOException(failure);
    }
  }

  private void design() {
    this.run(
        () -> {
          if (this.computationPage != null && !this.computationPage.canClose()) {
            return;
          }
          this.flushAll();
          if (this.folder == null && this.files.path() != null) {
            this.folder = this.files.path().getParent();
          }
          if (this.folder == null) {
            this.chooseFolder();
          }
          if (this.folder == null) {
            return;
          }
          if (this.computationPage != null) {
            this.computationPage.dispose();
          }
          this.computationPage =
              new LaboratoryDesignPage(
                  this.editor.document(),
                  this.folder,
                  file -> {
                    this.leaveComputation();
                    if (this.computationPage == null) {
                      this.files.open(file);
                    }
                  },
                  this::closeComputation);
          this.openComputation();
        });
  }

  private void route(final String stageId) {
    this.run(
        () -> {
          if (this.computationPage != null && !this.computationPage.canClose()) {
            return;
          }
          this.flushAll();
          if (this.computationPage != null) {
            this.computationPage.dispose();
          }
          final var page =
              new LaboratoryRoutePage(
                  this.editor,
                  stageId,
                  () -> this.files.save(false),
                  id -> {
                    this.leaveComputation();
                    if (this.computationPage == null) {
                      this.showStageQuantities(id);
                    }
                  },
                  this::closeComputation);
          page.historyActions(this::history, this.files::busy);
          this.computationPage = page;
          this.openComputation();
        });
  }

  private void sampleTransfer() {
    this.sampleTransfer(null);
  }

  private void restoreSampleTransfer(final java.util.Properties fields) {
    final Path source =
        Path.of(fields.getProperty("sampleTransfer.source")).toAbsolutePath().normalize();
    if (this.files.assetPath() == null
        || !source.equals(this.files.assetPath().toAbsolutePath().normalize())) {
      throw new IllegalArgumentException("Черновик передачи относится к другому исходному файлу.");
    }
    final var recovered = this.editor.document();
    final var page =
        new LaboratorySampleTransferPage(
            this.editor,
            source,
            this.owner,
            next -> {
              this.leaveComputation();
              if (this.computationPage == null) {
                this.files.open(next);
              }
            },
            this::closeComputation);
    page.sourceSaved(() -> this.files.refreshRecoveredExternalSave(source, recovered));
    try {
      page.draft(fields, true);
    } catch (final RuntimeException failure) {
      page.dispose();
      throw failure;
    }
    if (this.computationPage != null) {
      this.computationPage.dispose();
    }
    this.computationPage = page;
    this.openComputation();
  }

  private void sampleTransfer(final String sampleId) {
    this.run(
        () -> {
          if (this.computationPage != null && !this.computationPage.canClose()) {
            return;
          }
          this.flushAll();
          if (sampleId != null
              && this.editor.document().experiment().samples().stream()
                  .noneMatch(
                      sample ->
                          sample.id().equals(sampleId)
                              && sample.balance().status()
                                  == ru.pathcreator.vadim.quantum.chemistry.domain.notebook
                                      .LaboratorySample.Status.AVAILABLE
                              && sample.balance().remaining() != null
                              && sample.balance().remaining().value().signum() > 0)) {
            throw new IllegalArgumentException(
                "Выбранный образец недоступен или его остаток не известен. Уточните карточку перед передачей.");
          }
          if (!this.files.save(false) || this.files.path() == null) {
            return;
          }
          final Path source = this.files.path();
          if (this.computationPage != null) {
            this.computationPage.dispose();
          }
          final var page =
              new LaboratorySampleTransferPage(
                  this.editor,
                  source,
                  this.owner,
                  next -> {
                    this.leaveComputation();
                    if (this.computationPage == null) {
                      this.files.open(next);
                    }
                  },
                  this::closeComputation);
          page.sourceSaved(() -> this.files.refreshExternalSave(source));
          if (sampleId != null) {
            page.selectSample(sampleId);
          }
          this.computationPage = page;
          this.openComputation();
        });
  }

  private void reactionSearch() {
    this.run(
        () -> {
          if (this.computationPage != null && !this.computationPage.canClose()) {
            return;
          }
          this.flushAll();
          if (this.folder == null && this.files.path() != null) {
            this.folder = this.files.path().getParent();
          }
          if (this.folder == null) {
            this.chooseFolder();
          }
          if (this.folder == null) {
            return;
          }
          if (this.computationPage != null) {
            this.computationPage.dispose();
          }
          this.computationPage =
              new LaboratoryReactionSearchPage(
                  this.editor.document(),
                  this.folder,
                  (file, stageId) -> {
                    this.leaveComputation();
                    if (this.computationPage != null) {
                      return;
                    }
                    this.files.open(file);
                    if (file.toAbsolutePath().normalize().equals(this.files.path())) {
                      this.drawingTabs.getSelectionModel().select(1);
                      this.reaction.reveal(stageId);
                    }
                  },
                  file -> {
                    try {
                      final var loaded =
                          new ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure
                                  .LaboratoryFileRepository()
                              .read(file);
                      final var source = loaded.drawing();
                      this.leaveComputation();
                      if (this.computationPage != null) {
                        return;
                      }
                      this.files.fromTemplate(
                          ru.pathcreator.vadim.quantum.chemistry.drawing.application
                              .LaboratoryEditor.plannedCopy(
                              source,
                              source.experiment().title() + " — повтор",
                              new ru.pathcreator.vadim.quantum.chemistry.drawing.application
                                  .LaboratoryRepeatContext.Source(
                                  file.toAbsolutePath().normalize().toString(),
                                  loaded.fingerprint())));
                    } catch (final java.io.IOException e) {
                      this.files.error(e);
                    }
                  },
                  this::closeComputation);
          this.openComputation();
        });
  }

  private void procedure(final String stageId) {
    this.run(
        () -> {
          if (this.computationPage != null && !this.computationPage.canClose()) {
            return;
          }
          this.flushAll();
          if (this.computationPage != null) {
            this.computationPage.dispose();
          }
          this.computationPage =
              new LaboratoryProcedurePage(
                  this.editor,
                  stageId,
                  () -> {
                    if (this.files.save(false)
                        && this.computationPage instanceof LaboratoryProcedurePage page) {
                      page.status.setText("Методика и действия сохранены в QCHEM.");
                    }
                  },
                  this::closeComputation);
          this.openComputation();
        });
  }

  private void prepareStock(final String stageId) {
    try {
      if (this.computationPage != null && !this.computationPage.canClose()
          || this.stoichiometryPage != null && !this.stoichiometryPage.canClose()
          || this.materialsPage != null && !this.materialsPage.canClose()) {
        return;
      }
      this.flushAll();
      if (this.files.path() == null && !this.files.save(false)) {
        return;
      }
      final Path ledger =
          ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure
              .LaboratoryInventoryRepository.projectFolder(this.files.path())
              .resolve("materials.qchem");
      if (this.computationPage != null) {
        this.computationPage.dispose();
      }
      if (this.materialsPage != null) {
        this.materialsPage.dispose();
        this.materialsPage = null;
      }
      this.stoichiometryPage = null;
      final var page =
          new LaboratoryPreparationPage(
              this.editor, this.files, ledger, stageId, this::closeComputation);
      page.solutions(this::solutions);
      this.computationPage = page;
      this.openComputation();
    } catch (final Exception e) {
      this.files.error(e);
    }
  }

  private void chemDraw(final boolean exporting) {
    this.run(
        () -> {
          if (this.computationPage != null && !this.computationPage.canClose()) {
            return;
          }
          this.flushAll();
          if (this.computationPage != null) {
            this.computationPage.dispose();
          }
          this.computationPage =
              new LaboratoryChemDrawPage(
                  this.editor, exporting, this.owner, this::closeComputation);
          this.openComputation();
        });
  }

  private void reactionExchange(final boolean exporting, final String stageId) {
    this.run(
        () -> {
          if (this.computationPage != null && !this.computationPage.canClose()) {
            return;
          }
          this.flushAll();
          if (this.computationPage != null) {
            this.computationPage.dispose();
          }
          this.computationPage =
              new LaboratoryReactionExchangePage(
                  this.editor, stageId, exporting, this.owner, this::closeComputation);
          this.openComputation();
        });
  }

  private void exchange(final boolean exporting) {
    this.run(
        () -> {
          this.flushAll();
          this.computationPage =
              new LaboratoryExchangePage(
                  this.editor,
                  this.selected == null ? null : this.selected.componentId(),
                  exporting,
                  this.owner,
                  this::closeComputation);
          this.openComputation();
        });
  }

  private void openComputation() {
    if (!(this.computationPage instanceof LaboratoryWelcomePage)
        && !(this.computationPage instanceof LaboratoryExperimentDesk)
        && !(this.computationPage instanceof LaboratoryNotebookPage)) {
      this.computationPage.navigationContext(
          "Проекты › " + this.editor.document().experiment().title(), "← К структурам");
    }
    if (this.navigationReturn != null && this.navigationReturn.page() != this.computationPage) {
      this.navigationReturn = null;
    }
    this.root.setCenter(this.computationPage);
    this.root.getTop().setVisible(false);
    this.root.getTop().setManaged(false);
    this.status();
  }

  private void closeComputation() {
    this.rememberNotebook();
    final var page = this.computationPage;
    final var target = this.returnTarget(page);
    this.leaveComputation();
    if (page != null && this.computationPage == null) {
      this.resume(target);
    }
  }

  private ReturnTarget returnTarget(final Node page) {
    return this.navigationReturn != null && this.navigationReturn.page() == page
        ? this.navigationReturn
        : null;
  }

  private void resume(final ReturnTarget target) {
    if (target != null && target.experimentId().equals(this.editor.document().experiment().id())) {
      target.action().run();
    }
  }

  private void leaveComputation() {
    if (this.computationPage != null && !this.computationPage.canClose()) {
      return;
    }
    this.rememberNotebook();
    if (this.computationPage != null) {
      this.computationPage.dispose();
      this.computationPage = null;
    }
    this.navigationReturn = null;
    this.calculationArchivePage = false;
    this.root.setCenter(this.workArea);
    this.root.getTop().setVisible(true);
    this.root.getTop().setManaged(true);
    this.refresh();
  }

  private void library() {
    this.run(
        () -> {
          this.flushAll();
          final var target = this.selected;
          final String sourceExperiment = this.editor.document().experiment().id();
          final var targetDrawing = target == null ? null : this.layout();
          final Path libraryFolder;
          try {
            libraryFolder =
                this.files.path() == null
                    ? this.folder
                    : ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure
                        .LaboratoryInventoryRepository.projectFolder(this.files.path());
          } catch (final java.io.IOException exception) {
            throw new java.io.UncheckedIOException(exception);
          }
          this.libraryPage = new LaboratoryLibraryWindow();
          final var page =
              this.libraryPage.view(
                  this.owner,
                  libraryFolder,
                  targetDrawing,
                  this.editor.document(),
                  source -> {
                    final int count = this.editor.document().experiment().components().size();
                    final double minX =
                        source.atoms().stream()
                            .mapToDouble(atom -> atom.point2D().x())
                            .min()
                            .orElse(0);
                    final double minY =
                        source.atoms().stream()
                            .mapToDouble(atom -> atom.point2D().y())
                            .min()
                            .orElse(0);
                    final var drawing =
                        this.sketch.translate(
                            source, 180 + count % 4 * 280 - minX, 170 + count / 4 * 280 - minY);
                    this.selectAdded(this.sketch.independentCopy(drawing));
                    this.chooseComponent(this.selected.componentId(), true);
                    this.refresh();
                  },
                  this.files::fromTemplate,
                  target == null ? null : target.atomId(),
                  joined -> {
                    if (target == null
                        || !sourceExperiment.equals(this.editor.document().experiment().id())
                        || !targetDrawing.equals(
                            this.editor.document().layouts().get(target.componentId()))) {
                      throw new IllegalArgumentException(
                          "Исходная структура изменилась. Заново выберите атом и фрагмент.");
                    }
                    final var component =
                        this.editor.document().experiment().components().stream()
                            .filter(value -> value.id().equals(target.componentId()))
                            .findFirst()
                            .orElseThrow();
                    this.editor.replace(component.id(), joined, component.role());
                    this.selected = target;
                    this.refresh();
                  },
                  () -> {
                    this.libraryPage = null;
                    this.root.setCenter(this.workArea);
                    this.root.getTop().setVisible(true);
                    this.root.getTop().setManaged(true);
                    this.refresh();
                  });
          this.root.setCenter(page);
          this.root.getTop().setVisible(false);
          this.root.getTop().setManaged(false);
          this.status();
        });
  }

  private void quantifySelected() {
    if (this.selected != null) {
      this.openQuantities(this.selected.componentId());
    }
  }

  private void openStageQuantities(final String id) {
    try {
      this.flushAll();
      this.showStageQuantities(id);
    } catch (final RuntimeException exception) {
      this.files.error(exception);
    }
  }

  private void showStageQuantities(final String id) {
    this.stoichiometryPage =
        new LaboratoryStoichiometryWindow(
            this.editor,
            this.files,
            id,
            this.componentsPage ? "К компонентам" : "К реакции",
            () -> {
              this.stoichiometryPage = null;
              this.returnToComponentsOrDrawing();
            });
    this.stoichiometryPage.preparation(() -> this.prepareStock(id));
    this.root.setCenter(this.stoichiometryPage);
    this.root.getTop().setVisible(false);
    this.root.getTop().setManaged(false);
    this.status();
  }

  private void openStage(
      final ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionStage stage) {
    try {
      this.flushAll();
      this.showStage(stage);
    } catch (final RuntimeException exception) {
      this.files.error(exception);
    }
  }

  private void showStage(
      final ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionStage stage) {
    this.stagePage =
        new LaboratoryStageForm(
            this.editor,
            stage,
            () -> {
              final var target = this.returnTarget(this.stagePage);
              this.stagePage = null;
              this.navigationReturn = null;
              this.root.setCenter(this.workArea);
              this.root.getTop().setVisible(true);
              this.root.getTop().setManaged(true);
              this.refresh();
              this.resume(target);
            });
    this.stagePage.navigationContext(this.editor.document().experiment().title(), "← К структурам");
    this.root.setCenter(this.stagePage);
    this.root.getTop().setVisible(false);
    this.root.getTop().setManaged(false);
    this.status();
  }

  private void openQuantities(final String id) {
    try {
      this.flushAll();
      this.showQuantities(id);
    } catch (final RuntimeException exception) {
      this.files.error(exception);
    }
  }

  private void showQuantities(final String id) {
    this.quantityPage =
        new LaboratoryQuantificationForm(
            this.editor,
            id,
            () -> {
              final var target = this.returnTarget(this.quantityPage);
              this.quantityPage = null;
              this.navigationReturn = null;
              this.returnToComponentsOrDrawing();
              this.resume(target);
            });
    this.root.setCenter(this.quantityPage);
    this.root.getTop().setVisible(false);
    this.root.getTop().setManaged(false);
    this.status();
  }

  private void materials() {
    try {
      this.flushAll();
      if (this.files.path() == null && !this.files.save(false)) {
        return;
      }
      final Path materialFolder =
          ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure
              .LaboratoryInventoryRepository.projectFolder(this.files.path());
      final var molecule =
          this.selected == null ? null : this.sketch.independentCopy(this.layout()).molecule();
      this.showMaterials(materialFolder, molecule);
    } catch (final Exception exception) {
      this.files.error(exception);
    }
  }

  private void showMaterials(
      final Path materialFolder,
      final ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule molecule)
      throws java.io.IOException {
    final var page =
        new LaboratoryInventoryWindow(
            materialFolder,
            this.editor.document().experiment().id(),
            operation -> {
              final String marker =
                  "; операция " + operation.id() + "; партия " + operation.batchId();
              if (this.editor.document().experiment().id().equals(operation.experimentId())
                  && this.editor.document().experiment().journal().stream()
                      .noneMatch(entry -> entry.text().contains(marker))) {
                this.editor.record(
                    ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryEntry.Kind
                        .ACTION,
                    "Учёт материалов: "
                        + operation.kind()
                        + " "
                        + operation.quantity()
                        + "; "
                        + operation.reason()
                        + "; операция "
                        + operation.id()
                        + "; партия "
                        + operation.batchId(),
                    null);
              }
              this.refresh();
            },
            this.editor.document().experiment().components());
    page.preparation(() -> this.prepareStock(null));
    page.solutions(
        batchId -> {
          if (page.canClose()) {
            page.back();
            this.solutions(batchId);
          }
        });
    final Node content =
        page.view(
            this.owner,
            molecule,
            this.editor.document().experiment().title(),
            () -> {
              this.materialsPage = null;
              this.root.setCenter(this.workArea);
              this.root.getTop().setVisible(true);
              this.root.getTop().setManaged(true);
              this.refresh();
            });
    this.materialsPage = page;
    this.root.setCenter(content);
    this.root.getTop().setVisible(false);
    this.root.getTop().setManaged(false);
    this.status();
  }

  private void chooseFolder() {
    final DirectoryChooser chooser = new DirectoryChooser();
    chooser.setTitle("Папка опытов");
    final File chosen = chooser.showDialog(this.owner);
    if (chosen != null) {
      this.folder = chosen.toPath();
      this.listFolder();
    }
  }

  private void saveAnalysis(final boolean saveAs) {
    if (!(this.computationPage instanceof LaboratoryAnalysisPage page) || !page.canClose()) {
      return;
    }
    if (this.files.save(saveAs)) {
      page.status.setText("Опыт с аналитическими результатами сохранён в QCHEM.");
    } else {
      page.status.setText(
          "Сохранение не завершено. Аналитические результаты остаются в памяти; проверьте сообщение файловой операции.");
    }
  }

  private void sampleCatalog() {
    this.run(
        () -> {
          if (this.computationPage != null && !this.computationPage.canClose()) {
            return;
          }
          this.flushAll();
          if (this.folder == null && this.files.path() != null) {
            this.folder = this.files.path().getParent();
          }
          if (this.folder == null) {
            this.chooseFolder();
          }
          if (this.folder == null) {
            return;
          }
          if (this.computationPage != null) {
            this.computationPage.dispose();
          }
          this.computationPage =
              new LaboratorySampleCatalogPage(
                  this.folder,
                  () ->
                      new LaboratorySampleCatalog.Current(
                          this.files.path(), this.editor.document(), this.editor.dirty()),
                  this::openCatalogSample,
                  this::closeComputation);
          this.openComputation();
        });
  }

  private void openCatalogSample(final LaboratorySampleCatalog.Row row, final String action) {
    this.run(
        () -> {
          if (this.computationPage != null && !this.computationPage.canClose()) {
            return;
          }
          final boolean current =
              java.util.Objects.equals(this.files.path(), row.path())
                  && this.editor
                      .document()
                      .experiment()
                      .id()
                      .equals(row.reference().experimentId());
          if (!current) {
            if (row.path() == null) {
              throw new IllegalArgumentException("Текущий опыт сменился. Обновите каталог.");
            }
            this.files.open(row.path());
          }
          if (!java.util.Objects.equals(this.files.path(), row.path())
              || !this.editor.document().experiment().id().equals(row.reference().experimentId())) {
            return;
          }
          if (this.editor.document().experiment().samples().stream()
              .noneMatch(s -> s.id().equals(row.reference().sampleId()))) {
            throw new IllegalArgumentException(
                "Образец отсутствует в актуальном файле. Обновите каталог.");
          }
          if (action.equals("SAMPLE") || action.equals("RESULTS")) {
            this.samples();
            if (this.computationPage instanceof LaboratorySamplePage page) {
              if (action.equals("RESULTS")) {
                page.showAnalyses(row.reference().sampleId());
              } else {
                page.showSample(row.reference().sampleId());
              }
            }
          } else {
            this.analyses();
            if (this.computationPage instanceof LaboratoryAnalysisPage page) {
              page.beginForSample(row.reference().sampleId(), action);
            }
          }
        });
  }

  private void samples() {
    this.samples(null, this.selected == null ? null : this.selected.componentId(), false, null);
  }

  private void samples(
      final String stageId,
      final String componentId,
      final boolean register,
      final String sampleId) {
    this.run(
        () -> {
          if (this.computationPage != null && !this.computationPage.canClose()) {
            return;
          }
          this.flushAll();
          if (this.computationPage != null) {
            this.computationPage.dispose();
          }
          final var page =
              stageId != null || register
                  ? new LaboratorySamplePage(
                      this.editor,
                      stageId,
                      componentId,
                      () -> this.files.save(false),
                      this::closeComputation)
                  : new LaboratorySamplePage(
                      this.editor, componentId, (id, method) -> {}, this::closeComputation);
          page.analysis(
              (id, method) ->
                  this.sampleChild(
                      stageId,
                      componentId,
                      id,
                      () -> {
                        final var previous = this.computationPage;
                        this.analyses();
                        if (this.computationPage != previous
                            && this.computationPage instanceof LaboratoryAnalysisPage analysis) {
                          analysis.beginForSample(id, method);
                        }
                      }));
          page.evidence(
              (id, entry) ->
                  this.sampleChild(
                      stageId, componentId, id, () -> this.sampleEvidence(id, entry, false)),
              id ->
                  this.sampleChild(
                      stageId, componentId, id, () -> this.sampleEvidence(id, null, true)));
          page.saving(() -> this.files.save(false));
          page.transferToStock(
              id -> this.sampleChild(stageId, componentId, id, () -> this.sampleStock(id)));
          page.nextStage(
              id -> this.sampleChild(stageId, componentId, id, () -> this.sampleTransfer(id)));
          page.sourcePath(this.files.path());
          this.computationPage = page;
          this.openComputation();
          if (sampleId != null) {
            page.showSample(sampleId);
          } else if (!register && stageId != null) {
            page.showList();
          }
        });
  }

  private void sampleChild(
      final String stageId, final String componentId, final String sampleId, final Runnable open) {
    final var previous = this.computationPage;
    final var parent = this.returnTarget(previous);
    open.run();
    if (parent != null
        && this.computationPage != previous
        && this.computationPage != null
        && parent.experimentId().equals(this.editor.document().experiment().id())) {
      this.navigationReturn =
          new ReturnTarget(
              this.computationPage,
              parent.experimentId(),
              () -> {
                final boolean exists =
                    this.editor.document().experiment().samples().stream()
                        .anyMatch(s -> s.id().equals(sampleId));
                this.samples(stageId, componentId, false, exists ? sampleId : null);
                if (this.computationPage instanceof LaboratorySamplePage page) {
                  this.navigationReturn =
                      new ReturnTarget(page, parent.experimentId(), parent.action());
                  page.backCaption("← К опыту");
                  if (!exists) {
                    page.status.setText(
                        "Образец отсутствует в текущем опыте. Показан список образцов.");
                  }
                }
              });
      this.computationPage.backCaption("← К образцу");
    }
  }

  private void analyses() {
    this.analyses(this.selected == null ? null : this.selected.componentId());
  }

  private void analyses(final String component) {
    this.run(
        () -> {
          if (this.computationPage != null && !this.computationPage.canClose()) {
            return;
          }
          this.flushAll();
          if (this.computationPage != null) {
            this.computationPage.dispose();
          }
          final var page =
              new LaboratoryAnalysisPage(this.editor, component, this::closeComputation);
          page.saving(this::saveAnalysis);
          this.computationPage = page;
          this.openComputation();
        });
  }

  private void report() {
    this.run(
        () -> {
          if (this.computationPage != null && !this.computationPage.canClose()) {
            return;
          }
          this.flushAll();
          if (this.computationPage != null) {
            this.computationPage.dispose();
          }
          this.computationPage =
              new LaboratoryReportPage(this.editor.document(), this::closeComputation);
          this.openComputation();
        });
  }

  private void structureSearch() {
    try {
      this.flushAll();
      final Path parent =
          this.folder != null
              ? this.folder
              : this.files.path() == null ? null : this.files.path().getParent();
      final String component = this.selected == null ? null : this.selected.componentId();
      final java.util.Set<ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId> atoms =
          this.selection.hits().stream()
              .filter(hit -> hit.componentId().equals(component))
              .map(LaboratoryCanvas.Hit::atomId)
              .collect(java.util.stream.Collectors.toSet());
      this.computationPage =
          new LaboratoryStructureSearchPage(
              this.editor.document(),
              component,
              atoms,
              parent,
              (path, id) -> {
                this.files.open(path);
                if (path.equals(this.files.path())) {
                  this.run(() -> this.chooseComponent(id, true));
                }
              },
              (path, snapshot) -> {
                this.computationPage.dispose();
                this.computationPage =
                    new LaboratoryOverviewPage(
                        path, this.files::open, this::closeComputation, snapshot);
                this.openComputation();
              },
              this::closeComputation);
      this.openComputation();
    } catch (final RuntimeException exception) {
      this.files.error(exception);
    }
  }

  private void series() {
    try {
      this.flushAll();
      final Path parent =
          this.folder != null
              ? this.folder
              : this.files.path() == null ? null : this.files.path().getParent();
      this.computationPage =
          new LaboratorySeriesPage(
              this.editor.document(),
              parent,
              (destination, hints) -> {
                this.folder = destination;
                this.listFolder();
                this.computationPage.dispose();
                this.computationPage =
                    new LaboratoryOverviewPage(
                        destination, this.files::open, this::closeComputation, null, hints);
                this.openComputation();
              },
              this::closeComputation);
      this.openComputation();
    } catch (final RuntimeException exception) {
      this.files.error(exception);
    }
  }

  private void overview() {
    try {
      this.flushAll();
      if (this.folder == null) {
        this.chooseFolder();
      }
      if (this.folder == null) {
        return;
      }
      this.computationPage =
          new LaboratoryOverviewPage(this.folder, this.files::open, this::closeComputation);
      this.openComputation();
    } catch (final RuntimeException exception) {
      this.files.error(exception);
    }
  }

  private void exportProject() {
    try {
      this.flushAll();
      if (this.editor.dirty() && !this.files.save(false)) {
        return;
      }
      final Path project;
      if (this.files.path() != null) {
        project =
            ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure
                .LaboratoryInventoryRepository.projectFolder(this.files.path());
      } else {
        if (this.folder == null) {
          this.chooseFolder();
        }
        project = this.folder;
      }
      if (project == null) {
        return;
      }
      this.showProject(LaboratoryProjectWindow.exporting(this.owner, project, this::closeProject));
    } catch (final Exception exception) {
      this.files.error(exception);
    }
  }

  private void importProject() {
    try {
      this.flushAll();
      this.showProject(
          LaboratoryProjectWindow.importing(
              this.owner,
              path -> {
                if (!this.files.createForProject()) {
                  return;
                }
                this.folder = path;
                this.listFolder();
                this.openFolders();
              },
              this::closeProject));
    } catch (final Exception exception) {
      this.files.error(exception);
    }
  }

  private void showProject(final LaboratoryProjectWindow page) {
    if (page == null) {
      return;
    }
    if (this.projectPage != null) {
      this.projectPage.dispose();
    }
    this.projectPage = page;
    this.root.setCenter(page.view());
    this.root.getTop().setVisible(false);
    this.root.getTop().setManaged(false);
    this.status();
  }

  private void closeProject() {
    if (this.projectPage != null) {
      this.projectPage.dispose();
    }
    this.projectPage = null;
    this.root.setCenter(this.workArea);
    this.root.getTop().setVisible(true);
    this.root.getTop().setManaged(true);
    this.refresh();
  }

  private void listFolder() {
    if (this.folder == null && this.files.path() != null) {
      this.folder = this.files.path().getParent();
    }
    if (this.folder == null) {
      return;
    }
    this.archive.folder(this.folder);
  }

  private Node scrolled(final VBox content) {
    content.setPadding(new Insets(12));
    final ScrollPane scroll = new ScrollPane(content);
    scroll.setFitToWidth(true);
    return scroll;
  }

  private static javafx.scene.control.MenuItem menu(final String text, final Runnable action) {
    final var item = new javafx.scene.control.MenuItem(text);
    item.setOnAction(event -> action.run());
    return item;
  }

  private static Button button(final String text, final Runnable action) {
    final Button button = new Button(text);
    button.setOnAction(event -> action.run());
    return button;
  }
}

/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** Orthographic view of core coordinates. View rotation and scale never modify the molecule. */
final class LaboratoryGeometryPane extends VBox {
  ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryConformerSet.View view() {
    return new ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryConformerSet.View(
        this.yaw.getValue(),
        this.pitch.getValue(),
        this.zoom.getValue(),
        this.panX,
        this.panY,
        this.representation.getValue(),
        this.labels.isSelected());
  }

  void view(
      final ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryConformerSet.View
          view) {
    this.yaw.setValue(view.yaw());
    this.pitch.setValue(view.pitch());
    this.zoom.setValue(view.zoom());
    this.panX = view.panX();
    this.panY = view.panY();
    this.representation.setValue(
        this.representation.getItems().contains(view.representation())
            ? view.representation()
            : "Шары и связи");
    this.labels.setSelected(view.labels());
    this.draw();
  }

  private record Projected(Atom atom, double x, double y, double z) {}

  private record Layer(double depth, Runnable paint) {}

  private boolean dark;
  private javafx.scene.Parent themeRoot;
  private final javafx.collections.ListChangeListener<String> themeClasses =
      change -> this.refreshTheme();
  private final javafx.beans.value.ChangeListener<javafx.scene.Parent> sceneRoot =
      (o, before, after) -> this.observeThemeRoot(after);
  private final javafx.scene.control.ComboBox<String> representation =
      new javafx.scene.control.ComboBox<>();
  private final javafx.scene.control.ToggleButton labels =
      new javafx.scene.control.ToggleButton("Символы");
  private final javafx.scene.control.ToggleButton measuring =
      new javafx.scene.control.ToggleButton("Измерить");
  private final Label measurement = new Label();
  private final List<AtomId> measured = new ArrayList<>();
  private double atomScale = 1;
  private double panX;
  private double panY;
  private boolean dragging;
  private final javafx.scene.control.ToggleButton pan =
      new javafx.scene.control.ToggleButton("Перемещение");
  private final Canvas canvas = new Canvas(280, 320);
  private final Label status = new Label("Выберите компонент");
  private final Label emptyTitle = new Label("3D · просмотр");
  private String emptyContextTitle;
  private String emptyContextHint;
  private final Label summary = new Label();
  private final VBox emptyState = new VBox(10, this.emptyTitle, this.status);
  private final javafx.scene.layout.FlowPane heading = new javafx.scene.layout.FlowPane(6, 6);
  private javafx.scene.Node viewTools;
  private javafx.scene.Node cameraControl;
  private javafx.scene.Node gestureHint;
  private javafx.scene.layout.StackPane viewport;
  private final javafx.scene.control.TitledPane compactControls =
      new javafx.scene.control.TitledPane();
  private boolean compact;
  private double availableHeight = 600;
  private javafx.scene.layout.HBox compactHeader;
  private javafx.scene.control.ScrollPane compactScroll;
  private final javafx.scene.control.Button compactPrepare =
      new javafx.scene.control.Button("Построить 3D");
  private final Slider yaw = new Slider(-180, 180, 25);
  private final Slider pitch = new Slider(-90, 90, 15);
  private final Slider zoom = new Slider(0.3, 3, 1);
  private final javafx.scene.control.ComboBox<
          ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryComponent>
      components = new javafx.scene.control.ComboBox<>();
  private Consumer<String> componentSelection = id -> {};
  private boolean loadingComponents;
  private javafx.scene.control.Button seed;
  private javafx.scene.control.Button relax;
  private Molecule molecule;
  private AtomId selected;
  private List<Projected> projected = List.of();

  LaboratoryGeometryPane(final Consumer<AtomId> select) {
    super(8);
    this.setId("chemistry-geometry-pane");
    this.canvas.setId("chemistry-3d-canvas");
    this.canvas.setFocusTraversable(true);
    this.canvas.setCursor(javafx.scene.Cursor.OPEN_HAND);
    this.labels.setTooltip(
        new javafx.scene.control.Tooltip(
            "Символы элементов и изотопов. Порядковые номера атомов не показываются."));
    this.labels.setId("chemistry-3d-labels");
    this.measuring.setId("chemistry-3d-measure");
    this.status.setWrapText(true);
    this.status.setMaxWidth(320);
    this.status.setStyle("-fx-text-fill: #526174;");
    this.emptyTitle.setStyle("-fx-font-size: 19; -fx-font-weight: bold; -fx-text-fill: #526174;");
    this.emptyState.setAlignment(javafx.geometry.Pos.CENTER);
    this.emptyState.setMaxSize(340, javafx.scene.layout.Region.USE_PREF_SIZE);
    this.emptyState.setMouseTransparent(true);
    this.status.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
    this.summary.setWrapText(true);
    this.summary.setStyle("-fx-font-size: 12; -fx-text-fill: -app-muted;");
    this.components.setId("chemistry-3d-component");
    this.components.setPromptText("Выберите компонент");
    this.components.setPrefWidth(280);
    this.components.setMaxWidth(280);
    this.heading.getChildren().add(this.components);
    this.components.setVisible(false);
    this.components.setManaged(false);
    this.components.setCellFactory(list -> this.componentCell());
    this.components.setButtonCell(this.componentCell());
    this.components.setOnAction(
        event -> {
          if (!this.loadingComponents && this.components.getValue() != null) {
            this.componentSelection.accept(this.components.getValue().id());
          }
        });
    this.representation.getItems().setAll("Шары и связи", "Стержни");
    this.representation.setValue("Шары и связи");
    this.representation.setId("chemistry-3d-representation");
    this.representation.setOnAction(event -> this.draw());
    this.labels.setOnAction(event -> this.draw());
    this.measuring.setOnAction(
        event -> {
          this.measured.clear();
          this.draw();
        });
    final javafx.scene.control.Button reset = new javafx.scene.control.Button("Сброс вида");
    reset.setOnAction(event -> this.resetView());
    final javafx.scene.control.Button smaller = new javafx.scene.control.Button("−");
    final javafx.scene.control.Button larger = new javafx.scene.control.Button("+");
    smaller.setTooltip(new javafx.scene.control.Tooltip("Уменьшить 3D"));
    larger.setTooltip(new javafx.scene.control.Tooltip("Увеличить 3D"));
    smaller.setOnAction(event -> this.zoom.setValue(this.zoom.getValue() / 1.2));
    larger.setOnAction(event -> this.zoom.setValue(this.zoom.getValue() * 1.2));
    final javafx.scene.control.MenuButton direction = new javafx.scene.control.MenuButton("Ракурс");
    final String[] directions = {"Спереди", "Сверху", "Сбоку"};
    for (int index = 0; index < directions.length; index++) {
      final int face = index;
      final javafx.scene.control.MenuItem item =
          new javafx.scene.control.MenuItem(directions[index]);
      item.setOnAction(
          event -> {
            this.yaw.setValue(face == 2 ? 90 : 0);
            this.pitch.setValue(face == 1 ? 90 : 0);
          });
      direction.getItems().add(item);
    }
    reset.setId("chemistry-3d-reset");
    smaller.setId("chemistry-3d-zoom-out");
    larger.setId("chemistry-3d-zoom-in");
    final javafx.scene.control.ToggleButton rotate =
        new javafx.scene.control.ToggleButton("Вращение");
    final javafx.scene.control.ToggleGroup gestures = new javafx.scene.control.ToggleGroup();
    rotate.setToggleGroup(gestures);
    this.pan.setToggleGroup(gestures);
    rotate.setSelected(true);
    rotate.setId("chemistry-3d-rotate");
    this.pan.setId("chemistry-3d-pan");
    rotate.setOnAction(event -> rotate.setSelected(true));
    this.pan.setOnAction(event -> this.pan.setSelected(true));
    rotate.setTooltip(new javafx.scene.control.Tooltip("Тяните мышью, чтобы повернуть модель"));
    this.pan.setTooltip(
        new javafx.scene.control.Tooltip(
            "Тяните мышью, чтобы сдвинуть модель. Также Shift + перетаскивание или средняя кнопка."));
    final javafx.scene.control.Button left = new javafx.scene.control.Button("↶");
    final javafx.scene.control.Button right = new javafx.scene.control.Button("↷");
    left.setTooltip(new javafx.scene.control.Tooltip("Повернуть камеру на 90° влево"));
    right.setTooltip(new javafx.scene.control.Tooltip("Повернуть камеру на 90° вправо"));
    left.setId("chemistry-3d-turn-left");
    right.setId("chemistry-3d-turn-right");
    left.setGraphic(LaboratoryIcons.create("rotate-left"));
    right.setGraphic(LaboratoryIcons.create("rotate"));
    for (final var button : List.of(left, right)) {
      button.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
      button.setAccessibleText(button.getTooltip().getText());
      button.getStyleClass().add("laboratory-icon-button");
    }
    left.setOnAction(event -> this.yaw.setValue(wrapAngle(this.yaw.getValue() - 90)));
    right.setOnAction(event -> this.yaw.setValue(wrapAngle(this.yaw.getValue() + 90)));
    final javafx.scene.layout.FlowPane viewTools =
        new javafx.scene.layout.FlowPane(
            5,
            5,
            rotate,
            this.pan,
            left,
            right,
            direction,
            smaller,
            larger,
            reset,
            this.representation,
            this.labels,
            this.measuring);
    viewTools.getStyleClass().add("laboratory-geometry-tools");
    this.viewTools = viewTools;
    this.measurement.setWrapText(true);
    this.measurement.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
    final javafx.scene.layout.StackPane viewport =
        new javafx.scene.layout.StackPane(this.canvas, this.emptyState);
    this.viewport = viewport;
    viewport.setId("chemistry-3d-viewport");
    viewport.setMinSize(0, 220);
    viewport.setPrefSize(400, 120);
    this.setMinSize(0, 0);
    VBox.setVgrow(viewport, javafx.scene.layout.Priority.ALWAYS);
    this.canvas.widthProperty().bind(viewport.widthProperty());
    this.canvas.heightProperty().bind(viewport.heightProperty());
    final javafx.scene.layout.HBox controls =
        new javafx.scene.layout.HBox(
            16,
            new VBox(4, new Label("Поворот"), this.yaw),
            new VBox(4, new Label("Наклон"), this.pitch),
            new VBox(4, new Label("Масштаб"), this.zoom));
    controls
        .getChildren()
        .forEach(
            node -> {
              final VBox column = (VBox) node;
              column.setMinWidth(0);
              column.prefWidthProperty().bind(controls.widthProperty().subtract(32).divide(3));
              javafx.scene.layout.HBox.setHgrow(column, javafx.scene.layout.Priority.ALWAYS);
            });
    final javafx.scene.control.TitledPane camera =
        new javafx.scene.control.TitledPane("Камера", controls);
    camera.setExpanded(false);
    camera.setId("chemistry-3d-camera");
    this.cameraControl = camera;
    this.gestureHint =
        new Label(
            "Тяните — вращение · Shift + тяните — перемещение · колесо — масштаб · двойной щелчок — сброс");
    this.gestureHint.setStyle("-fx-font-size: 11; -fx-text-fill: -app-muted;");
    this.getChildren()
        .setAll(
            this.heading,
            viewTools,
            viewport,
            this.measurement,
            camera,
            this.summary,
            this.gestureHint);
    this.getChildren().stream()
        .filter(Label.class::isInstance)
        .map(Label.class::cast)
        .forEach(
            label -> {
              label.setWrapText(true);
              label.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
            });
    this.canvas.heightProperty().addListener((o, a, b) -> this.draw());
    this.canvas.widthProperty().addListener((o, a, b) -> this.draw());
    for (final var slider : List.of(this.yaw, this.pitch, this.zoom)) {
      slider.valueProperty().addListener((o, a, b) -> this.draw());
    }
    final double[] drag = new double[6];
    final boolean[] moved = {false};
    final boolean[] translating = {false};
    this.canvas.setOnMousePressed(
        event -> {
          if (event.getButton() != javafx.scene.input.MouseButton.PRIMARY
              && event.getButton() != javafx.scene.input.MouseButton.MIDDLE) {
            return;
          }
          this.canvas.requestFocus();
          this.dragging = true;
          drag[0] = event.getX();
          drag[1] = event.getY();
          drag[2] = this.yaw.getValue();
          drag[3] = this.pitch.getValue();
          drag[4] = this.panX;
          drag[5] = this.panY;
          moved[0] = false;
          translating[0] =
              this.pan.isSelected()
                  || event.isShiftDown()
                  || event.getButton() == javafx.scene.input.MouseButton.MIDDLE;
          this.canvas.setCursor(javafx.scene.Cursor.CLOSED_HAND);
          event.consume();
        });
    this.canvas.setOnMouseDragged(
        event -> {
          if (!this.dragging || !event.isPrimaryButtonDown() && !event.isMiddleButtonDown()) {
            return;
          }
          final double dx = event.getX() - drag[0], dy = event.getY() - drag[1];
          if (Math.hypot(dx, dy) > 3) {
            moved[0] = true;
          }
          if (!moved[0]) {
            return;
          }
          if (translating[0]) {
            this.panX = drag[4] + dx;
            this.panY = drag[5] + dy;
            this.draw();
          } else {
            this.yaw.setValue(wrapAngle(drag[2] + dx * 0.5));
            this.pitch.setValue(drag[3] - dy * 0.5);
          }
          event.consume();
        });
    this.canvas.setOnMouseReleased(
        event -> {
          this.dragging = false;
          this.canvas.setCursor(javafx.scene.Cursor.OPEN_HAND);
        });
    this.canvas
        .focusedProperty()
        .addListener(
            (observable, before, focused) -> {
              if (!focused) {
                this.dragging = false;
                this.canvas.setCursor(javafx.scene.Cursor.OPEN_HAND);
              }
            });
    this.canvas.setOnScroll(
        event -> {
          this.zoomAt(Math.exp(event.getDeltaY() * 0.002), event.getX(), event.getY());
          event.consume();
        });
    this.canvas.setOnZoom(
        event -> {
          this.zoomAt(event.getZoomFactor(), event.getX(), event.getY());
          event.consume();
        });
    this.canvas.setOnKeyPressed(
        event -> {
          if (event.isShortcutDown() || event.isAltDown()) {
            return;
          }
          final double dx =
              event.getCode() == javafx.scene.input.KeyCode.LEFT
                  ? -1
                  : event.getCode() == javafx.scene.input.KeyCode.RIGHT ? 1 : 0;
          final double dy =
              event.getCode() == javafx.scene.input.KeyCode.UP
                  ? -1
                  : event.getCode() == javafx.scene.input.KeyCode.DOWN ? 1 : 0;
          if (dx == 0 && dy == 0) {
            return;
          }
          if (event.isShiftDown()) {
            this.panX += dx * 20;
            this.panY += dy * 20;
            this.draw();
          } else {
            this.yaw.setValue(wrapAngle(this.yaw.getValue() + dx * 15));
            this.pitch.setValue(this.pitch.getValue() - dy * 15);
          }
          event.consume();
        });
    this.sceneProperty()
        .addListener(
            (o, before, after) -> {
              if (before != null) {
                before.rootProperty().removeListener(this.sceneRoot);
              }
              if (after != null) {
                after.rootProperty().addListener(this.sceneRoot);
              }
              this.observeThemeRoot(after == null ? null : after.getRoot());
            });
    this.canvas.setOnMouseClicked(
        event -> {
          if (moved[0] || event.getButton() != javafx.scene.input.MouseButton.PRIMARY) {
            return;
          }
          if (event.getClickCount() == 2 && !this.measuring.isSelected()) {
            this.resetView();
            return;
          }
          this.projected.reversed().stream()
              .filter(
                  atom ->
                      Math.hypot(atom.x() - event.getX(), atom.y() - event.getY())
                          < Math.max(10, this.radius(atom.atom())))
              .findFirst()
              .ifPresent(
                  atom -> {
                    if (this.measuring.isSelected()) {
                      if (this.measured.contains(atom.atom().id())) {
                        this.measured.remove(atom.atom().id());
                      } else {
                        if (this.measured.size() == 4) {
                          this.measured.clear();
                        }
                        this.measured.add(atom.atom().id());
                      }
                      this.draw();
                    }
                    select.accept(atom.atom().id());
                  });
        });
  }

  void actions(final Runnable prepare, final Runnable optimize) {
    this.seed = new javafx.scene.control.Button("Построить 3D");
    this.seed.setId("chemistry-3d-prepare");
    this.seed.setOnAction(event -> prepare.run());
    this.seed.setTooltip(
        new javafx.scene.control.Tooltip(
            "Начальная раскладка по графу. Оптимизация выполняется отдельно."));
    this.relax = new javafx.scene.control.Button("Релаксация 3D");
    this.relax.setId("chemistry-3d-relax");
    this.relax.setOnAction(event -> optimize.run());
    this.heading.getChildren().addAll(this.seed, this.relax);
  }

  void componentSelection(final Consumer<String> selection) {
    this.componentSelection = selection;
    this.components.setVisible(true);
    this.components.setManaged(true);
  }

  void preferences(
      final ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryViewPreferences
          preferences,
      final Runnable changed) {
    this.representation.setValue(
        preferences.choice("3d.representation", "Шары и связи", "Шары и связи", "Стержни"));
    this.labels.setSelected(preferences.flag("3d.labels", false));
    this.representation
        .valueProperty()
        .addListener(
            (observable, before, value) -> {
              preferences.set("3d.representation", value);
              changed.run();
            });
    this.labels
        .selectedProperty()
        .addListener(
            (observable, before, value) -> {
              preferences.set("3d.labels", value);
              changed.run();
            });
  }

  void components(
      final List<ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryComponent> values,
      final String selectedId) {
    this.loadingComponents = true;
    try {
      this.components.getItems().setAll(values);
      this.components.setValue(
          values.stream().filter(value -> value.id().equals(selectedId)).findFirst().orElse(null));
      this.components.setDisable(values.isEmpty());
    } finally {
      this.loadingComponents = false;
    }
  }

  private javafx.scene.control.ListCell<
          ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryComponent>
      componentCell() {
    return new javafx.scene.control.ListCell<>() {
      @Override
      protected void updateItem(
          final ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryComponent value,
          final boolean empty) {
        super.updateItem(value, empty);
        final String text =
            empty || value == null
                ? null
                : value.molecule().displayName()
                    + " · "
                    + LaboratoryInspector.roleText(value.role());
        this.setText(text);
        this.setTooltip(text == null ? null : new javafx.scene.control.Tooltip(text));
        this.setPrefWidth(260);
        this.setMaxWidth(260);
      }
    };
  }

  /** Use actual allocated space, including standalone 3D with side panels. */
  void available(final double width, final double height, final boolean paired) {
    if (width <= 0 || height <= 0) {
      return;
    }
    this.availableHeight = height;
    this.compact(paired || width < 640 || height < 560);
    if (this.compact) {
      this.viewport.setMinHeight(Math.max(60, Math.min(180, (height - 100) * .55)));
      if (this.compactScroll != null) {
        this.compactScroll.setPrefViewportHeight(
            Math.max(36, Math.min(180, height - this.viewport.getMinHeight() - 125)));
      }
    }
  }

  javafx.beans.property.ReadOnlyBooleanProperty controlsExpandedProperty() {
    return this.compactControls.expandedProperty();
  }

  /** Quick actions remain above the molecule; secondary controls scroll within a bounded area. */
  void compact(final boolean enabled) {
    if (this.compact == enabled) {
      return;
    }
    this.compact = enabled;
    this.getChildren().clear();
    if (enabled) {
      this.heading.getChildren().remove(this.components);
      this.components.setMinWidth(70);
      this.components.setPrefWidth(180);
      this.components.setMaxWidth(Double.MAX_VALUE);
      final javafx.scene.control.Button turn = new javafx.scene.control.Button("↷");
      turn.setId("chemistry-3d-compact-turn");
      turn.setGraphic(LaboratoryIcons.create("rotate"));
      turn.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
      turn.setAccessibleText("Повернуть 3D на 90°");
      turn.setTooltip(new javafx.scene.control.Tooltip("Повернуть 3D на 90°"));
      turn.getStyleClass().add("laboratory-icon-button");
      turn.setOnAction(event -> this.yaw.setValue(wrapAngle(this.yaw.getValue() + 90)));
      final javafx.scene.control.Button reset = new javafx.scene.control.Button("↺");
      reset.setAccessibleText("Сброс вида 3D");
      reset.setTooltip(new javafx.scene.control.Tooltip("Сброс вида 3D"));
      reset.getStyleClass().add("laboratory-icon-button");
      reset.setGraphic(LaboratoryIcons.create("select-all"));
      reset.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
      reset.setId("chemistry-3d-compact-reset");
      reset.setOnAction(event -> this.resetView());
      this.compactHeader = new javafx.scene.layout.HBox(5, this.components, turn, reset);
      this.compactHeader.setId("chemistry-3d-quick-actions");
      javafx.scene.layout.HBox.setHgrow(this.components, javafx.scene.layout.Priority.ALWAYS);
      this.compactControls.setText("Управление 3D");
      this.compactControls.setId("chemistry-3d-compact-controls");
      this.compactControls.setAnimated(false);
      this.compactControls.setExpanded(false);
      final VBox controls =
          new VBox(
              6, this.heading, this.viewTools, this.cameraControl, this.summary, this.gestureHint);
      controls.setPadding(new javafx.geometry.Insets(6));
      this.compactScroll = new javafx.scene.control.ScrollPane(controls);
      this.compactScroll.setId("chemistry-3d-controls-scroll");
      this.compactScroll.setFitToWidth(true);
      this.compactScroll.setMinHeight(0);
      this.compactScroll.setPrefViewportHeight(
          Math.max(36, Math.min(180, this.availableHeight - this.viewport.getMinHeight() - 125)));
      this.compactControls.setGraphic(null);
      this.compactControls.setContent(this.compactScroll);
      this.compactControls.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
      this.compactControls.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
      this.compactPrepare.setId("chemistry-3d-compact-prepare");
      this.compactPrepare.setOnAction(
          event -> {
            if (this.seed != null) {
              this.seed.fire();
            }
          });
      this.viewport.setMinHeight(Math.max(60, Math.min(180, (this.availableHeight - 100) * .55)));
      this.getChildren()
          .setAll(
              this.compactHeader,
              this.compactPrepare,
              this.compactControls,
              this.viewport,
              this.measurement);
    } else {
      if (this.compactHeader != null) {
        this.compactHeader.getChildren().clear();
      }
      if (this.compactScroll != null && this.compactScroll.getContent() instanceof VBox controls) {
        controls.getChildren().clear();
      }
      this.compactScroll = null;
      this.compactControls.setContent(null);
      this.compactControls.setGraphic(null);
      this.components.setPrefWidth(280);
      this.components.setMaxWidth(280);
      this.heading.getChildren().addFirst(this.components);
      this.viewport.setMinHeight(220);
      this.getChildren()
          .setAll(
              this.heading,
              this.viewTools,
              this.viewport,
              this.measurement,
              this.cameraControl,
              this.summary,
              this.gestureHint);
    }
    this.draw();
    this.requestLayout();
  }

  /** A host page may describe its own way of obtaining geometry; camera and theme changes retain this context. */
  void emptyHint(final String title, final String hint) {
    if (title == null || title.isBlank() || hint == null || hint.isBlank()) {
      throw new IllegalArgumentException("Укажите заголовок и подсказку пустого 3D-вида.");
    }
    this.emptyContextTitle = title;
    this.emptyContextHint = hint;
    this.draw();
  }

  void load(final Molecule molecule, final AtomId selected) {
    if (!java.util.Objects.equals(this.molecule, molecule)) {
      this.measured.clear();
      this.dragging = false;
      // A different component starts centered, even if the previous view was dragged off screen.
      if (this.molecule == null || molecule == null || !this.molecule.id().equals(molecule.id())) {
        this.panX = 0;
        this.panY = 0;
      }
    }
    this.molecule = molecule;
    this.selected = selected;
    if (this.seed != null) {
      this.seed.setDisable(molecule == null);
    }
    if (this.relax != null) {
      this.relax.setDisable(
          molecule == null
              || molecule.atoms().stream().anyMatch(atom -> atom.coordinate() == null));
    }
    this.draw();
  }

  private void observeThemeRoot(final javafx.scene.Parent root) {
    if (this.themeRoot != null) {
      this.themeRoot.getStyleClass().removeListener(this.themeClasses);
    }
    this.themeRoot = root;
    if (root != null) {
      root.getStyleClass().addListener(this.themeClasses);
    }
    this.refreshTheme();
  }

  private void refreshTheme() {
    this.dark = this.themeRoot != null && this.themeRoot.getStyleClass().contains("theme-dark");
    final String muted = this.dark ? "#b0bfd1" : "#526174";
    this.status.setStyle("-fx-text-fill: " + muted + ";");
    this.emptyTitle.setStyle(
        "-fx-font-size: 19; -fx-font-weight: bold; -fx-text-fill: " + muted + ";");
    this.draw();
  }

  private Color labelBackground() {
    return Color.web(this.dark ? "#172436" : "#ffffff");
  }

  private Color labelInk() {
    return Color.web(this.dark ? "#edf3fb" : "#263445");
  }

  private static double wrapAngle(final double angle) {
    return ((angle + 180) % 360 + 360) % 360 - 180;
  }

  private void resetView() {
    this.panX = 0;
    this.panY = 0;
    this.yaw.setValue(25);
    this.pitch.setValue(15);
    this.zoom.setValue(1);
    this.draw();
  }

  private void zoomAt(final double factor, final double x, final double y) {
    if (!Double.isFinite(factor) || factor <= 0) {
      return;
    }
    final double before = this.zoom.getValue();
    final double after =
        Math.max(this.zoom.getMin(), Math.min(this.zoom.getMax(), before * factor));
    final double ratio = after / before;
    this.panX =
        (this.panX + this.canvas.getWidth() / 2 - x) * ratio + x - this.canvas.getWidth() / 2;
    this.panY =
        (this.panY + this.canvas.getHeight() / 2 - y) * ratio + y - this.canvas.getHeight() / 2;
    this.zoom.setValue(after);
    this.draw();
  }

  private double radius(final Atom atom) {
    return Math.max(
        3, Math.min(32, (atom.element().atomicNumber() == 1 ? 0.17 : 0.26) * this.atomScale));
  }

  private void draw() {
    final var graphics = this.canvas.getGraphicsContext2D();
    graphics.setFill(
        this.dark
            ? new javafx.scene.paint.LinearGradient(
                0,
                0,
                0,
                1,
                true,
                javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.web("#1b293b")),
                new javafx.scene.paint.Stop(1, Color.web("#121c2a")))
            : Color.WHITE);
    graphics.fillRect(0, 0, this.canvas.getWidth(), this.canvas.getHeight());
    this.projected = List.of();
    final boolean ready =
        this.molecule != null
            && this.molecule.atoms().stream().allMatch(a -> a.coordinate() != null);
    this.compactPrepare.setVisible(!ready && this.molecule != null && this.seed != null);
    this.compactPrepare.setManaged(this.compactPrepare.isVisible());
    this.emptyState.setVisible(!ready);
    this.emptyState.setManaged(!ready);
    this.emptyTitle.setText(
        this.emptyContextTitle != null
            ? this.emptyContextTitle
            : this.molecule == null ? "3D · просмотр" : "Нет 3D-координат");
    for (final var node :
        List.of(this.viewTools, this.cameraControl, this.summary, this.gestureHint)) {
      node.setVisible(ready);
      node.setManaged(ready);
    }
    if (this.relax != null) {
      this.relax.setVisible(ready);
      this.relax.setManaged(ready);
    }
    if (this.seed != null) {
      this.seed.setText(ready ? "Перестроить 3D" : "Построить 3D");
    }
    this.measurement.setVisible(ready && this.measuring.isSelected());
    this.measurement.setManaged(ready && this.measuring.isSelected());
    this.measurement.setText(
        this.measured.size() < 2
            ? "Выберите 2 атома для расстояния, 3 для угла, 4 для торсиона."
            : LaboratoryGeometryMeasurement.describe(this.molecule, this.measured));
    if (this.molecule == null) {
      this.status.setText(
          this.emptyContextHint != null
              ? this.emptyContextHint
              : "Выберите компонент в списке выше, на листе или в таблице.");
      return;
    }
    final long absent =
        this.molecule.atoms().stream().filter(atom -> atom.coordinate() == null).count();
    if (absent > 0) {
      this.status.setText(
          this.emptyContextHint != null
              ? this.emptyContextHint
              : "Нет координат у "
                  + absent
                  + " из "
                  + this.molecule.atoms().size()
                  + (this.seed == null
                      ? " атомов. В этом снимке полная 3D-геометрия отсутствует."
                      : " атомов. Нажмите «Построить 3D», чтобы создать начальную модель, или загрузите структуру с геометрией."));
      return;
    }
    final double[] center = new double[3];
    for (final Atom atom : this.molecule.atoms()) {
      final var coordinate = atom.coordinate();
      center[0] += coordinate.unit().toAngstrom(coordinate.x());
      center[1] += coordinate.unit().toAngstrom(coordinate.y());
      center[2] += coordinate.unit().toAngstrom(coordinate.z());
    }
    for (int i = 0; i < 3; i++) {
      center[i] /= this.molecule.atoms().size();
    }
    double radius = 0;
    for (final Atom atom : this.molecule.atoms()) {
      final var c = atom.coordinate();
      radius =
          Math.max(
              radius,
              Math.sqrt(
                  Math.pow(c.unit().toAngstrom(c.x()) - center[0], 2)
                      + Math.pow(c.unit().toAngstrom(c.y()) - center[1], 2)
                      + Math.pow(c.unit().toAngstrom(c.z()) - center[2], 2)));
    }
    final double scale =
        Math.min(this.canvas.getWidth(), this.canvas.getHeight())
            * 0.35
            / Math.max(0.5, radius)
            * this.zoom.getValue();
    this.atomScale = scale;
    final double yawAngle = Math.toRadians(this.yaw.getValue());
    final double pitchAngle = Math.toRadians(this.pitch.getValue());
    final List<Projected> points = new ArrayList<>();
    for (final Atom atom : this.molecule.atoms()) {
      final var c = atom.coordinate();
      final double x = c.unit().toAngstrom(c.x()) - center[0];
      final double y = c.unit().toAngstrom(c.y()) - center[1];
      final double z = c.unit().toAngstrom(c.z()) - center[2];
      final double horizontal = x * Math.cos(yawAngle) + z * Math.sin(yawAngle);
      final double depth = -x * Math.sin(yawAngle) + z * Math.cos(yawAngle);
      points.add(
          new Projected(
              atom,
              this.canvas.getWidth() / 2 + this.panX + horizontal * scale,
              this.canvas.getHeight() / 2
                  + this.panY
                  - (y * Math.cos(pitchAngle) - depth * Math.sin(pitchAngle)) * scale,
              y * Math.sin(pitchAngle) + depth * Math.cos(pitchAngle)));
    }
    points.sort(Comparator.comparingDouble(Projected::z));
    this.projected = List.copyOf(points);
    final java.util.Map<AtomId, Projected> byId = new java.util.HashMap<>();
    points.forEach(point -> byId.put(point.atom().id(), point));
    final List<Layer> layers = new ArrayList<>();
    final boolean balls = this.representation.getValue().equals("Шары и связи");
    final double width = Math.max(2, Math.min(9, scale * (balls ? 0.09 : 0.07)));
    for (final var bond : this.molecule.bonds()) {
      final Projected first = byId.get(bond.firstAtomId()), second = byId.get(bond.secondAtomId());
      final double length = Math.hypot(second.x() - first.x(), second.y() - first.y());
      final double nx = length < 1e-8 ? 1 : -(second.y() - first.y()) / length;
      final double ny = length < 1e-8 ? 0 : (second.x() - first.x()) / length;
      final int order =
          switch (bond.type()) {
            case DOUBLE, AROMATIC -> 2;
            case TRIPLE -> 3;
            default -> 1;
          };
      for (int line = 0; line < order; line++) {
        final double offset = (line - (order - 1) / 2.0) * width * 1.6;
        final boolean dashed =
            bond.type() == ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType.UNKNOWN
                || bond.type()
                        == ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType.AROMATIC
                    && line == 1;
        for (int part = 0; part < 12; part++) {
          if (dashed && part % 2 != 0) {
            continue;
          }
          final double t0 = part / 12.0, t1 = (part + 1) / 12.0, mid = (t0 + t1) / 2;
          final Color color = this.color(mid < 0.5 ? first.atom() : second.atom());
          layers.add(
              new Layer(
                  first.z() * (1 - mid) + second.z() * mid,
                  () -> {
                    graphics.setStroke(color);
                    graphics.setLineWidth(width);
                    graphics.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
                    graphics.strokeLine(
                        first.x() * (1 - t0) + second.x() * t0 + nx * offset,
                        first.y() * (1 - t0) + second.y() * t0 + ny * offset,
                        first.x() * (1 - t1) + second.x() * t1 + nx * offset,
                        first.y() * (1 - t1) + second.y() * t1 + ny * offset);
                  }));
        }
      }
    }
    for (final Projected point : points) {
      layers.add(new Layer(point.z(), () -> this.paintAtom(point, balls)));
    }
    layers.sort(Comparator.comparingDouble(Layer::depth));
    layers.forEach(layer -> layer.paint().run());
    // Selection and measurement marks stay legible even when a selected atom is behind another
    // atom.
    for (final Projected point : points) {
      final int index = this.measured.indexOf(point.atom().id());
      if (point.atom().id().equals(this.selected) || index >= 0) {
        final double markRadius = this.radius(point.atom()) + 4;
        final Color mark =
            Color.web(
                index >= 0
                    ? (this.dark ? "#f2c16d" : "#c98512")
                    : (this.dark ? "#79bbff" : "#3182ce"));
        if (this.dark) {
          graphics.setStroke(Color.web("#121c2a"));
          graphics.setLineWidth(5);
          graphics.strokeOval(
              point.x() - markRadius, point.y() - markRadius, markRadius * 2, markRadius * 2);
        }
        graphics.setStroke(mark);
        graphics.setLineWidth(2);
        graphics.strokeOval(
            point.x() - markRadius, point.y() - markRadius, markRadius * 2, markRadius * 2);
        if (index >= 0) {
          final double badgeX = point.x() + markRadius + 3, badgeY = point.y() - markRadius;
          graphics.setFont(javafx.scene.text.Font.font("Arial", 12));
          if (this.dark) {
            graphics.setFill(mark);
            graphics.fillRoundRect(badgeX - 3, badgeY - 12, 14, 16, 5, 5);
          }
          graphics.setFill(Color.web(this.dark ? "#162132" : "#875500"));
          graphics.fillText(Integer.toString(index + 1), badgeX, badgeY);
        }
      }
    }
    final String origin = this.molecule.metadata().attributes().get("laboratory.geometry.origin");
    this.status.setText(
        this.molecule.displayName()
            + " · "
            + points.size()
            + " атомов · расстояния в Å · ортографическая проекция"
            + ("GRAPH_SEED".equals(origin)
                ? "\nНачальная раскладка; без оптимизации."
                : "BONDED_FORCE_FIELD".equals(origin)
                    ? "\nГеометрия приближённой силовой модели. "
                        + ("true"
                                .equals(
                                    this.molecule
                                        .metadata()
                                        .attributes()
                                        .get("laboratory.geometry.converged"))
                            ? "Сходимость достигнута."
                            : "Сходимость НЕ достигнута.")
                    : ""));
    this.summary.setText(this.status.getText());
  }

  private void paintAtom(final Projected point, final boolean balls) {
    final var graphics = this.canvas.getGraphicsContext2D();
    final Color color = this.color(point.atom());
    final double radius =
        balls ? this.radius(point.atom()) : Math.max(2, Math.min(5, this.atomScale * 0.05));
    graphics.setFill(
        new javafx.scene.paint.RadialGradient(
            0,
            0,
            0.3,
            0.25,
            0.75,
            true,
            javafx.scene.paint.CycleMethod.NO_CYCLE,
            new javafx.scene.paint.Stop(0, color.brighter().brighter()),
            new javafx.scene.paint.Stop(0.5, color),
            new javafx.scene.paint.Stop(1, color.darker())));
    graphics.fillOval(point.x() - radius, point.y() - radius, radius * 2, radius * 2);
    if (this.dark) {
      graphics.setStroke(color.interpolate(Color.WHITE, 0.3));
      graphics.setLineWidth(0.7);
      graphics.strokeOval(point.x() - radius, point.y() - radius, radius * 2, radius * 2);
    }
    if (this.labels.isSelected()) {
      final String text =
          (point.atom().hasIsotope()
                  ? Integer.toString(point.atom().isotope().massNumber())
                      .chars()
                      .mapToObj(c -> Character.toString("⁰¹²³⁴⁵⁶⁷⁸⁹".charAt(c - '0')))
                      .collect(java.util.stream.Collectors.joining())
                  : "")
              + point.atom().element().symbol().value();
      graphics.setFont(javafx.scene.text.Font.font("Arial", 12));
      graphics.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
      final javafx.scene.text.Text caption = new javafx.scene.text.Text(text);
      caption.setFont(graphics.getFont());
      final double labelWidth = caption.getLayoutBounds().getWidth() + 8;
      graphics.setFill(this.labelBackground());
      graphics.fillRoundRect(
          point.x() - labelWidth / 2, point.y() - radius - 19, labelWidth, 16, 4, 4);
      graphics.setFill(this.labelInk());
      graphics.fillText(text, point.x(), point.y() - radius - 7);
      graphics.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
    }
  }

  private Color color(final Atom atom) {
    if (this.dark) {
      return Color.web(
          switch (atom.element().symbol().value()) {
            case "O" -> "#ef6f6c";
            case "N" -> "#719ef7";
            case "H" -> "#e9eef5";
            case "Cl", "F" -> "#76ca8a";
            case "Br" -> "#ce8667";
            case "I" -> "#bd94e4";
            case "S" -> "#e6c85c";
            case "P" -> "#f0b16b";
            default -> "#9aaabd";
          });
    }
    return Color.web(
        switch (atom.element().symbol().value()) {
          case "O" -> "#e64d4d";
          case "N" -> "#437ee4";
          case "H" -> "#cdd3db";
          case "Cl", "F" -> "#49ae69";
          case "Br" -> "#a94f36";
          case "I" -> "#9265b5";
          case "S" -> "#dfbd38";
          case "P" -> "#e68c35";
          default -> "#586574";
        });
  }
}

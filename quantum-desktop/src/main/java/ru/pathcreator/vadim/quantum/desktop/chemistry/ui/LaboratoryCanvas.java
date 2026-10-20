/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.function.Consumer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.AtomDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.BondDrawingStyle;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeAbbreviations;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeAbbreviation;

/** Canvas positions never become core 3D coordinates. Dragging commits only on release. */
final class LaboratoryCanvas extends ScrollPane {

  record Hit(String componentId, AtomId atomId) {}

  record BondHit(String componentId, AtomId first, AtomId second) {}

  record Gesture(
      Hit hit,
      double x,
      double y,
      boolean moved,
      java.util.Set<Hit> region,
      boolean additive,
      double dx,
      double dy,
      Hit start,
      BondHit bond) {}

  private final Canvas canvas = new Canvas(800, 600);
  private final Consumer<Gesture> action;
  private LaboratoryDrawing document;
  private Hit selected;
  private Hit pressed;
  private Hit hoverAtom;
  private BondHit hoverBond;
  private BondHit selectedBond;
  private javafx.scene.control.ContextMenu popup;
  private Consumer<Double> zoomChanged = value -> {};
  private double pressX;
  private double pressY;
  private boolean draggingEnabled = true;
  private java.util.Set<Hit> selection = java.util.Set.of();
  private java.util.Set<Hit> moving = java.util.Set.of();
  private boolean connectedMode;
  private boolean showNames;
  private boolean showCarbonHydrogens = true;
  private double zoom = 1;
  // Presentation offsets keep negative drawing coordinates visible without changing the document.
  private double originX;
  private double originY;
  private boolean focusPadding;

  private enum ViewMode {
    MANUAL,
    DOCUMENT,
    COMPONENTS
  }

  private ViewMode viewMode = ViewMode.MANUAL;
  private java.util.Set<String> focusedComponents = java.util.Set.of();
  private final javafx.animation.PauseTransition viewportRefresh =
      new javafx.animation.PauseTransition(javafx.util.Duration.millis(16));
  private boolean applyingView;
  private final javafx.scene.control.Tooltip atomHint = new javafx.scene.control.Tooltip();
  private boolean panMode;
  private boolean spaceHeld;
  private boolean panning;
  private boolean editingGesture;
  private javafx.stage.Window gestureWindow;
  private final javafx.beans.value.ChangeListener<Boolean> windowFocus =
      (observable, before, focused) -> {
        if (!focused) {
          this.cancelGesture();
        }
      };
  private final javafx.beans.value.ChangeListener<javafx.stage.Window> windowChanged =
      (observable, before, window) -> this.trackWindow(window);
  private double panStartX;
  private double panStartY;
  private double panTranslationX;
  private double panTranslationY;
  private boolean rotationMode;
  private Hit rotationTarget;
  private double rotationAngle;
  private java.util.function.BiConsumer<Hit, Double> rotation = (hit, angle) -> {};
  private boolean bondMode;
  private boolean atomMode;
  private BondType bondType = BondType.SINGLE;
  private double previewX;
  private double previewY;

  LaboratoryCanvas(final Consumer<Gesture> action) {
    this.action = action;
    this.setContent(this.canvas);
    this.setPannable(false);
    this.setId("chemistry-canvas");
    this.canvas.setFocusTraversable(true);
    this.atomHint.setShowDelay(javafx.util.Duration.millis(350));
    javafx.scene.control.Tooltip.install(this.canvas, this.atomHint);
    this.viewportRefresh.setOnFinished(
        event -> {
          if (this.editingGesture
              || this.panning
              || this.getScene() == null
              || this.getViewportBounds().getWidth() <= 64
              || this.getViewportBounds().getHeight() <= 64) {
            return;
          }
          if (this.viewMode == ViewMode.DOCUMENT) {
            this.fit();
          } else if (this.viewMode == ViewMode.COMPONENTS
              && !this.focusComponents(this.focusedComponents)) {
            this.fit();
          }
        });
    this.viewportBoundsProperty()
        .addListener(
            (observable, before, bounds) -> {
              this.resizeCanvas();
              if (Math.abs(before.getWidth() - bounds.getWidth()) > .5
                  || Math.abs(before.getHeight() - bounds.getHeight()) > .5) {
                this.refreshViewport();
              }
            });
    this.addEventFilter(
        javafx.scene.input.ScrollEvent.SCROLL,
        event -> {
          if (event.getDeltaX() != 0 || event.getDeltaY() != 0) {
            this.manualView();
          }
        });
    this.addEventFilter(
        MouseEvent.MOUSE_PRESSED,
        event -> {
          for (javafx.scene.Node node =
                  event.getTarget() instanceof javafx.scene.Node target ? target : null;
              node != null && node != this;
              node = node.getParent()) {
            if (node instanceof javafx.scene.control.ScrollBar) {
              this.manualView();
              break;
            }
          }
        });
    this.canvas.addEventFilter(
        javafx.scene.input.KeyEvent.KEY_PRESSED,
        event -> {
          if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE && this.editingGesture) {
            this.cancelGesture();
            event.consume();
            return;
          }
          if (event.getCode() == javafx.scene.input.KeyCode.SPACE
              && !event.isShortcutDown()
              && !event.isAltDown()) {
            this.spaceHeld = true;
            this.canvas.setCursor(javafx.scene.Cursor.OPEN_HAND);
            event.consume();
          }
        });
    this.canvas.addEventFilter(
        javafx.scene.input.KeyEvent.KEY_RELEASED,
        event -> {
          if (event.getCode() == javafx.scene.input.KeyCode.SPACE) {
            this.spaceHeld = false;
            this.canvas.setCursor(
                this.panMode ? javafx.scene.Cursor.OPEN_HAND : javafx.scene.Cursor.CROSSHAIR);
            event.consume();
          }
        });
    this.canvas
        .focusedProperty()
        .addListener(
            (observable, before, focused) -> {
              if (!focused) {
                this.spaceHeld = false;
              }
            });
    this.sceneProperty()
        .addListener(
            (observable, before, scene) -> {
              if (before != null) {
                before.windowProperty().removeListener(this.windowChanged);
              }
              if (scene != null) {
                scene.windowProperty().addListener(this.windowChanged);
              }
              this.trackWindow(scene == null ? null : scene.getWindow());
            });
    this.canvas.setOnMousePressed(
        event -> {
          if (event.getButton() == javafx.scene.input.MouseButton.MIDDLE
              || event.getButton() == javafx.scene.input.MouseButton.PRIMARY
                  && (this.panMode || this.spaceHeld)) {
            if (this.popup != null) {
              this.popup.hide();
            }
            this.canvas.requestFocus();
            this.atomHint.hide();
            this.editingGesture = false;
            this.panning = true;
            this.panStartX = event.getSceneX();
            this.panStartY = event.getSceneY();
            this.panTranslationX = this.originX * this.zoom - this.scrollX();
            this.panTranslationY = this.originY * this.zoom - this.scrollY();
            this.canvas.setCursor(javafx.scene.Cursor.CLOSED_HAND);
            event.consume();
            return;
          }
          if (event.getButton() != javafx.scene.input.MouseButton.PRIMARY) {
            return;
          }
          if (this.popup != null) {
            this.popup.hide();
          }
          this.canvas.requestFocus();
          this.editingGesture = true;
          this.pressed = this.hit(this.modelX(event), this.modelY(event));
          this.pressX = this.modelX(event);
          this.pressY = this.modelY(event);
          this.previewX = 0;
          this.previewY = 0;
          this.rotationAngle = 0;
          this.rotationTarget = null;
          if (this.rotationMode) {
            final var bond = this.bondHit(this.pressX, this.pressY);
            this.rotationTarget =
                this.pressed != null
                    ? this.pressed
                    : bond != null ? new Hit(bond.componentId(), bond.first()) : this.selected;
            return;
          }
          if (this.pressed == null) {
            this.moving = java.util.Set.of();
          } else {
            final java.util.Set<Hit> fragment = new java.util.LinkedHashSet<>();
            if (this.selection.contains(this.pressed) || event.isShiftDown()) {
              fragment.addAll(this.selection);
            }
            if (this.connectedMode) {
              new ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeSketchService()
                  .connected(
                      this.document.layouts().get(this.pressed.componentId()),
                      this.pressed.atomId())
                  .forEach(id -> fragment.add(new Hit(this.pressed.componentId(), id)));
            } else {
              fragment.add(this.pressed);
            }
            for (final var component : this.document.experiment().components()) {
              final var ids =
                  fragment.stream()
                      .filter(hit -> hit.componentId().equals(component.id()))
                      .map(Hit::atomId)
                      .collect(java.util.stream.Collectors.toSet());
              MoleculeAbbreviations.selection(this.document.layouts().get(component.id()), ids)
                  .forEach(id -> fragment.add(new Hit(component.id(), id)));
            }
            if (!this.showCarbonHydrogens) {
              for (final var component : this.document.experiment().components()) {
                final var ids =
                    fragment.stream()
                        .filter(hit -> hit.componentId().equals(component.id()))
                        .map(Hit::atomId)
                        .collect(java.util.stream.Collectors.toSet());
                includeCarbonHydrogens(this.document.layouts().get(component.id()), ids)
                    .forEach(id -> fragment.add(new Hit(component.id(), id)));
              }
            }
            this.moving = fragment;
          }
        });
    this.canvas.setOnMouseDragged(
        event -> {
          if (this.panning) {
            this.panTo(
                this.panTranslationX + event.getSceneX() - this.panStartX,
                this.panTranslationY + event.getSceneY() - this.panStartY);
            event.consume();
            return;
          }
          if (!event.isPrimaryButtonDown() || !this.editingGesture) {
            return;
          }
          this.manualView();
          if (this.rotationMode) {
            this.rotationAngle = this.rotationAngle(event);
            this.draw();
            event.consume();
            return;
          }
          this.previewX = this.modelX(event) - this.pressX;
          this.previewY = this.modelY(event) - this.pressY;
          this.draw();
          if (this.bondMode && (!this.atomMode || this.pressed != null)) {
            final var start =
                this.pressed == null
                    ? null
                    : position(
                            this.document.layouts().get(this.pressed.componentId()),
                            this.pressed.atomId())
                        .point2D();
            final double x = start == null ? this.pressX : start.x();
            final double y = start == null ? this.pressY : start.y();
            final double[] end = snapped(x, y, this.modelX(event), this.modelY(event));
            final Hit target = this.hit(this.modelX(event), this.modelY(event));
            if (target != null) {
              final var point =
                  position(this.document.layouts().get(target.componentId()), target.atomId())
                      .point2D();
              end[0] = point.x();
              end[1] = point.y();
            }
            final GraphicsContext graphics = this.canvas.getGraphicsContext2D();
            graphics.setTransform(
                this.zoom, 0, 0, this.zoom, this.originX * this.zoom, this.originY * this.zoom);
            graphics.setStroke(Color.web("#3983bc"));
            graphics.setLineWidth(1.7);
            graphics.setLineDashes(4, 3);
            final int count =
                this.bondType == BondType.TRIPLE ? 3 : this.bondType == BondType.DOUBLE ? 2 : 1;
            final double length = Math.max(1, Math.hypot(end[0] - x, end[1] - y));
            for (int index = 0; index < count; index++) {
              final double offset = (index - (count - 1) / 2.0) * 4;
              final double ox = -(end[1] - y) / length * offset;
              final double oy = (end[0] - x) / length * offset;
              graphics.strokeLine(x + ox, y + oy, end[0] + ox, end[1] + oy);
            }
            graphics.setLineDashes();
          }
          if (this.draggingEnabled && this.pressed == null) {
            final GraphicsContext graphics = this.canvas.getGraphicsContext2D();
            graphics.setStroke(Color.DODGERBLUE);
            graphics.setLineWidth(1);
            graphics.setFill(Color.color(0.15, 0.4, 0.95, 0.1));
            graphics.fillRect(
                Math.min(this.pressX, this.modelX(event)),
                Math.min(this.pressY, this.modelY(event)),
                Math.abs(this.previewX),
                Math.abs(this.previewY));
            graphics.strokeRect(
                Math.min(this.pressX, this.modelX(event)),
                Math.min(this.pressY, this.modelY(event)),
                Math.abs(this.previewX),
                Math.abs(this.previewY));
          }
        });
    this.canvas.setOnMouseReleased(this::released);
    this.canvas.setOnMouseMoved(
        event -> {
          if (this.panMode || this.spaceHeld) {
            this.canvas.setCursor(javafx.scene.Cursor.OPEN_HAND);
            return;
          }
          this.hoverAtom = this.hit(this.modelX(event), this.modelY(event));
          if (this.hoverAtom == null) {
            this.atomHint.hide();
            this.atomHint.setText("");
          } else {
            final var hovered =
                this.document
                    .layouts()
                    .get(this.hoverAtom.componentId())
                    .molecule()
                    .atoms()
                    .stream()
                    .filter(a -> a.id().equals(this.hoverAtom.atomId()))
                    .findFirst()
                    .orElseThrow();
            this.atomHint.setText(
                hovered.element().symbol().value()
                    + (hovered.hasIsotope()
                        ? "-"
                            + hovered.isotope().massNumber()
                            + " · изотоп, массовое число "
                            + hovered.isotope().massNumber()
                        : "")
                    + (hovered.formalCharge().value() == 0
                        ? ""
                        : " · заряд " + hovered.formalCharge().value()));
            final var abbreviation =
                MoleculeAbbreviations.projection(
                        this.document.layouts().get(this.hoverAtom.componentId()))
                    .anchors()
                    .get(this.hoverAtom.atomId());
            if (abbreviation != null) {
              this.atomHint.setText(
                  abbreviation.label()
                      + " · свёрнуто "
                      + abbreviation.atoms().size()
                      + " явных атомов\nПолный граф сохранён. Раскрыть можно через контекстное меню.");
            }
          }
          this.hoverBond =
              this.hoverAtom == null ? this.bondHit(this.modelX(event), this.modelY(event)) : null;
          this.canvas.setCursor(
              this.hoverAtom != null || this.hoverBond != null
                  ? javafx.scene.Cursor.HAND
                  : javafx.scene.Cursor.CROSSHAIR);
          this.draw();
        });
    this.canvas.setOnMouseExited(
        event -> {
          this.atomHint.hide();
          this.hoverAtom = null;
          this.hoverBond = null;
          this.draw();
        });
    this.canvas.setOnScroll(
        event -> {
          if (!event.isShortcutDown() || event.getDeltaY() == 0) {
            return;
          }
          this.zoomAt(
              this.zoom * (event.getDeltaY() > 0 ? 1.15 : 1 / 1.15),
              event.getX() - this.scrollX(),
              event.getY() - this.scrollY());
          event.consume();
        });
    this.canvas.setOnZoom(
        event -> {
          this.zoomAt(
              this.zoom * event.getZoomFactor(),
              event.getX() - this.scrollX(),
              event.getY() - this.scrollY());
          event.consume();
        });
  }

  private void trackWindow(final javafx.stage.Window window) {
    if (this.gestureWindow == window) {
      return;
    }
    if (this.gestureWindow != null) {
      this.gestureWindow.focusedProperty().removeListener(this.windowFocus);
    }
    this.gestureWindow = window;
    this.cancelGesture();
    if (window != null) {
      window.focusedProperty().addListener(this.windowFocus);
    }
  }

  boolean cancelActiveGesture() {
    if (!this.editingGesture && !this.panning && !this.spaceHeld) {
      return false;
    }
    this.cancelGesture();
    return true;
  }

  private void cancelGesture() {
    this.spaceHeld = false;
    this.panning = false;
    this.editingGesture = false;
    this.previewX = 0;
    this.previewY = 0;
    this.rotationTarget = null;
    this.rotationAngle = 0;
    this.draw();
  }

  void contextMenu(
      final java.util.function.BiFunction<Hit, BondHit, javafx.scene.control.ContextMenu> factory) {
    this.canvas.setOnContextMenuRequested(
        event -> {
          if (this.popup != null) {
            this.popup.hide();
          }
          this.popup =
              factory.apply(
                  this.hit(
                      event.getX() / this.zoom - this.originX,
                      event.getY() / this.zoom - this.originY),
                  this.bondHit(
                      event.getX() / this.zoom - this.originX,
                      event.getY() / this.zoom - this.originY));
          if (this.popup != null) {
            this.popup.show(this.canvas, event.getScreenX(), event.getScreenY());
          }
          event.consume();
        });
  }

  void update(
      final LaboratoryDrawing drawing,
      final Hit primary,
      final java.util.Set<Hit> selection,
      final String tool) {
    if (this.document == null
        || !this.document.experiment().id().equals(drawing.experiment().id())) {
      this.manualView();
      this.focusedComponents = java.util.Set.of();
      this.originX = 0;
      this.originY = 0;
      this.focusPadding = false;
      this.panning = false;
      this.spaceHeld = false;
    }
    this.document = drawing;
    this.editingGesture = false;
    this.panning = false;
    this.rotationTarget = null;
    this.rotationAngle = 0;
    this.hoverAtom = null;
    this.hoverBond = null;
    this.selected = primary;
    this.selection = java.util.Set.copyOf(selection);
    this.resizeCanvas();
    this.draggingEnabled = tool.equals("Выбор") || tool.equals("Фрагмент");
    this.connectedMode = tool.equals("Фрагмент");
    this.panMode = tool.equals("Рука");
    this.canvas.setCursor(
        this.panMode ? javafx.scene.Cursor.OPEN_HAND : javafx.scene.Cursor.CROSSHAIR);
    this.rotationMode = tool.equals("Поворот");
    this.atomMode = tool.equals("Атом");
    this.bondMode = tool.equals("Связь") || this.atomMode;
    this.draw();
  }

  void onRotation(final java.util.function.BiConsumer<Hit, Double> action) {
    this.rotation = action;
  }

  private double rotationAngle(final MouseEvent event) {
    if (this.rotationTarget == null) {
      return 0;
    }
    final var center =
        ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeStereoEditing.center(
            this.document.layouts().get(this.rotationTarget.componentId()));
    if (Math.hypot(this.pressX - center.x(), this.pressY - center.y()) < 4
        || Math.hypot(this.modelX(event) - center.x(), this.modelY(event) - center.y()) < 4) {
      return 0;
    }
    final double angle =
        Math.atan2(this.modelY(event) - center.y(), this.modelX(event) - center.x())
            - Math.atan2(this.pressY - center.y(), this.pressX - center.x());
    final double normalized = Math.atan2(Math.sin(angle), Math.cos(angle));
    return event.isAltDown() ? normalized : Math.rint(normalized / (Math.PI / 12)) * Math.PI / 12;
  }

  void selectedBond(final BondHit bond) {
    this.selectedBond = bond;
    this.draw();
  }

  void bondType(final BondType type) {
    this.bondType = type;
  }

  double zoom(final double factor) {
    this.zoomAt(
        factor, this.getViewportBounds().getWidth() / 2, this.getViewportBounds().getHeight() / 2);
    return this.zoom;
  }

  void zoomChanged(final Consumer<Double> changed) {
    this.zoomChanged = changed;
  }

  private double scrollX() {
    return this.getHvalue()
        * Math.max(0, this.canvas.getWidth() - this.getViewportBounds().getWidth());
  }

  private double scrollY() {
    return this.getVvalue()
        * Math.max(0, this.canvas.getHeight() - this.getViewportBounds().getHeight());
  }

  /** Anchors are viewport pixels; wheel and pinch events arrive in canvas pixels. */
  void zoomAt(final double factor, final double anchorX, final double anchorY) {
    if (!Double.isFinite(factor) || factor <= 0) {
      return;
    }
    final double modelX = (anchorX + this.scrollX()) / this.zoom - this.originX;
    final double modelY = (anchorY + this.scrollY()) / this.zoom - this.originY;
    this.zoom = Math.max(0.1, Math.min(4, factor));
    this.panTo(anchorX - modelX * this.zoom, anchorY - modelY * this.zoom);
    this.zoomChanged.accept(this.zoom);
  }

  /** Only presentation requests follow viewport changes; user navigation keeps its own scale. */
  void refreshViewport() {
    if (!this.applyingView && this.viewMode != ViewMode.MANUAL) {
      this.viewportRefresh.playFromStart();
    }
  }

  private void manualView() {
    if (!this.applyingView) {
      this.viewMode = ViewMode.MANUAL;
      this.viewportRefresh.stop();
    }
  }

  private void panTo(final double translationX, final double translationY) {
    this.manualView();
    this.focusPadding = true;
    final double[] bounds = this.contentBounds();
    this.originX =
        Math.max(bounds == null ? 0 : 32 / this.zoom - bounds[0], translationX / this.zoom);
    this.originY =
        Math.max(bounds == null ? 0 : 32 / this.zoom - bounds[1], translationY / this.zoom);
    this.resizeCanvas();
    this.layout();
    this.setHvalue(
        Math.max(
            0,
            Math.min(
                1,
                (this.originX * this.zoom - translationX)
                    / Math.max(1, this.canvas.getWidth() - this.getViewportBounds().getWidth()))));
    this.setVvalue(
        Math.max(
            0,
            Math.min(
                1,
                (this.originY * this.zoom - translationY)
                    / Math.max(
                        1, this.canvas.getHeight() - this.getViewportBounds().getHeight()))));
  }

  double fit() {
    this.viewMode = ViewMode.DOCUMENT;
    this.focusedComponents = java.util.Set.of();
    this.applyingView = true;
    try {
      return this.fitDocument();
    } finally {
      this.applyingView = false;
    }
  }

  private double fitDocument() {
    this.focusPadding = false;
    if (this.document == null || this.document.layouts().isEmpty()) {
      return this.zoom(1);
    }
    final var atoms =
        this.document.layouts().values().stream()
            .flatMap(layout -> layout.atoms().stream())
            .toList();
    if (atoms.isEmpty()) {
      return this.zoom(1);
    }
    final double[] bounds = this.contentBounds();
    final double viewportWidth = this.getViewportBounds().getWidth();
    final double viewportHeight = this.getViewportBounds().getHeight();
    if (viewportWidth <= 64 || viewportHeight <= 64) {
      return this.zoom;
    }
    this.zoom(
        Math.min(
            1.35,
            Math.min(
                (viewportWidth - 64) / Math.max(1, bounds[2] - bounds[0]),
                (viewportHeight - 64) / Math.max(1, bounds[3] - bounds[1]))));
    this.focusPadding = false;
    this.originX =
        Math.max(
            32 / this.zoom - bounds[0],
            viewportWidth / (2 * this.zoom) - (bounds[0] + bounds[2]) / 2);
    this.originY =
        Math.max(
            32 / this.zoom - bounds[1],
            viewportHeight / (2 * this.zoom) - (bounds[1] + bounds[3]) / 2);
    this.resizeCanvas();
    this.setHvalue(0);
    this.setVvalue(0);
    return this.zoom;
  }

  /** Move the viewport only; molecular coordinates and the editor history are untouched. */
  boolean focus(final String componentId) {
    return this.focusComponents(java.util.Set.of(componentId));
  }

  boolean focusComponents(final java.util.Set<String> componentIds) {
    this.applyingView = true;
    try {
      if (!this.fitComponents(componentIds)) {
        return false;
      }
      this.viewMode = ViewMode.COMPONENTS;
      this.focusedComponents = java.util.Set.copyOf(componentIds);
      return true;
    } finally {
      this.applyingView = false;
    }
  }

  private boolean fitComponents(final java.util.Set<String> componentIds) {
    if (this.document == null
        || componentIds.isEmpty()
        || !this.document.layouts().keySet().containsAll(componentIds)) {
      return false;
    }
    final double[] bounds = this.contentBoundsFor(componentIds);
    final double width = this.getViewportBounds().getWidth(),
        height = this.getViewportBounds().getHeight();
    if (bounds == null || width <= 64 || height <= 64) {
      return false;
    }
    this.focusPadding = true;
    this.zoom(
        Math.min(
            1.35,
            Math.min(
                (width - 64) / Math.max(1, bounds[2] - bounds[0]),
                (height - 64) / Math.max(1, bounds[3] - bounds[1]))));
    final double[] all = this.contentBounds();
    this.originX =
        Math.max(32 / this.zoom - all[0], width / (2 * this.zoom) - (bounds[0] + bounds[2]) / 2);
    this.originY =
        Math.max(32 / this.zoom - all[1], height / (2 * this.zoom) - (bounds[1] + bounds[3]) / 2);
    this.resizeCanvas();
    this.layout();
    final double x =
        (this.originX + (bounds[0] + bounds[2]) / 2) * this.zoom
            - this.getViewportBounds().getWidth() / 2;
    final double y =
        (this.originY + (bounds[1] + bounds[3]) / 2) * this.zoom
            - this.getViewportBounds().getHeight() / 2;
    this.setHvalue(
        Math.max(
            0,
            Math.min(
                1, x / Math.max(1, this.canvas.getWidth() - this.getViewportBounds().getWidth()))));
    this.setVvalue(
        Math.max(
            0,
            Math.min(
                1,
                y / Math.max(1, this.canvas.getHeight() - this.getViewportBounds().getHeight()))));
    return true;
  }

  ru.pathcreator.vadim.quantum.chemistry.drawing.domain.geometry.DrawingPoint2D viewportCenter() {
    return ru.pathcreator.vadim.quantum.chemistry.drawing.domain.geometry.DrawingPoint2D.of(
        (this.scrollX() + this.getViewportBounds().getWidth() / 2) / this.zoom - this.originX,
        (this.scrollY() + this.getViewportBounds().getHeight() / 2) / this.zoom - this.originY);
  }

  private double modelX(final MouseEvent event) {
    return event.getX() / this.zoom - this.originX;
  }

  private double modelY(final MouseEvent event) {
    return event.getY() / this.zoom - this.originY;
  }

  static double[] snapped(
      final double x, final double y, final double targetX, final double targetY) {
    final double angle =
        Math.round(Math.atan2(targetY - y, targetX - x) / (Math.PI / 6)) * Math.PI / 6;
    return new double[] {x + 44 * Math.cos(angle), y + 44 * Math.sin(angle)};
  }

  void showNames(final boolean enabled) {
    this.showNames = enabled;
    this.resizeCanvas();
    this.refreshViewport();
  }

  void showCarbonHydrogens(final boolean enabled) {
    this.showCarbonHydrogens = enabled;
    this.hoverAtom = null;
    this.hoverBond = null;
    this.resizeCanvas();
    this.refreshViewport();
  }

  private java.util.Set<AtomId> hidden(final String componentId, final MoleculeDrawing drawing) {
    final var hidden =
        this.showCarbonHydrogens
            ? new java.util.HashSet<AtomId>()
            : previewHiddenHydrogens(drawing);
    // Group selection stays skeletal; an individually targeted H is still made explicit.
    if (this.selected != null && this.selected.componentId().equals(componentId)) {
      hidden.remove(this.selected.atomId());
    }
    if (this.selectedBond != null && this.selectedBond.componentId().equals(componentId)) {
      hidden.remove(this.selectedBond.first());
      hidden.remove(this.selectedBond.second());
    }
    hidden.addAll(MoleculeAbbreviations.projection(drawing).hidden());
    return hidden;
  }

  static java.util.Set<AtomId> includeCarbonHydrogens(
      final MoleculeDrawing drawing, final java.util.Set<AtomId> selected) {
    final var result =
        new java.util.LinkedHashSet<>(MoleculeAbbreviations.selection(drawing, selected));
    for (final var hidden : previewHiddenHydrogens(drawing)) {
      final var bond =
          drawing.molecule().bonds().stream()
              .filter(
                  value ->
                      value.firstAtomId().equals(hidden) || value.secondAtomId().equals(hidden))
              .findFirst()
              .orElseThrow();
      final var parent =
          bond.firstAtomId().equals(hidden) ? bond.secondAtomId() : bond.firstAtomId();
      if (result.contains(parent)) {
        result.add(hidden);
      }
    }
    return result;
  }

  private static String caption(final String name) {
    final var text = new javafx.scene.text.Text(name);
    text.setFont(Font.font("Arial", 13));
    if (text.getLayoutBounds().getWidth() <= 280) {
      return name;
    }
    String result = name;
    do {
      result = result.substring(0, result.offsetByCodePoints(result.length(), -1));
      text.setText(result + "…");
    } while (!result.isEmpty() && text.getLayoutBounds().getWidth() > 280);
    return text.getText();
  }

  private double[] contentBounds() {
    return this.contentBoundsFor(null);
  }

  private double[] contentBoundsFor(final java.util.Set<String> componentIds) {
    if (this.document == null) {
      return null;
    }
    double left = Double.POSITIVE_INFINITY,
        top = left,
        right = Double.NEGATIVE_INFINITY,
        bottom = right;
    for (final var component : this.document.experiment().components()) {
      if (componentIds != null && !componentIds.contains(component.id())) {
        continue;
      }
      final var drawing = this.document.layouts().get(component.id());
      if (drawing.atoms().isEmpty()) {
        continue;
      }
      final var visible = drawingBounds(drawing, this.hidden(component.id(), drawing));
      left = Math.min(left, visible.getMinX());
      right = Math.max(right, visible.getMaxX());
      top = Math.min(top, visible.getMinY());
      bottom = Math.max(bottom, visible.getMaxY());
      if (!drawing.molecule().stereochemistry().centers().isEmpty()) {
        top = Math.min(top, visible.getMinY() - 24);
        right = Math.max(right, visible.getMaxX() + 28);
      }
      if (this.showNames) {
        final var text = new javafx.scene.text.Text(caption(component.molecule().displayName()));
        text.setFont(Font.font("Arial", 13));
        final double half = text.getLayoutBounds().getWidth() / 2;
        left = Math.min(left, (visible.getMinX() + visible.getMaxX()) / 2 - half);
        right = Math.max(right, (visible.getMinX() + visible.getMaxX()) / 2 + half);
        bottom = Math.max(bottom, visible.getMaxY() + 34);
      }
    }
    return Double.isFinite(left) ? new double[] {left, top, right, bottom} : null;
  }

  private void resizeCanvas() {
    final double[] bounds = this.contentBounds();
    if (bounds != null) {
      this.originX = Math.max(this.originX, 32 / this.zoom - bounds[0]);
      this.originY = Math.max(this.originY, 32 / this.zoom - bounds[1]);
    }
    this.canvas.setWidth(
        Math.max(
            this.getViewportBounds().getWidth(),
            bounds == null
                ? 1
                : (bounds[2] + this.originX) * this.zoom
                    + (this.focusPadding ? this.getViewportBounds().getWidth() : 32)));
    this.canvas.setHeight(
        Math.max(
            this.getViewportBounds().getHeight(),
            bounds == null
                ? 1
                : (bounds[3] + this.originY) * this.zoom
                    + (this.focusPadding ? this.getViewportBounds().getHeight() : 32)));
    this.draw();
  }

  private void released(final MouseEvent event) {
    if (this.panning) {
      this.panning = false;
      this.canvas.setCursor(
          this.panMode || this.spaceHeld
              ? javafx.scene.Cursor.OPEN_HAND
              : javafx.scene.Cursor.CROSSHAIR);
      event.consume();
      return;
    }
    if (event.getButton() != javafx.scene.input.MouseButton.PRIMARY || !this.editingGesture) {
      return;
    }
    this.editingGesture = false;
    if (this.rotationMode) {
      final var target = this.rotationTarget;
      final double angle = this.rotationAngle(event);
      this.rotationTarget = null;
      this.rotationAngle = 0;
      if (target != null) {
        this.rotation.accept(target, angle);
      }
      this.draw();
      event.consume();
      return;
    }
    final boolean moved =
        Math.hypot(this.modelX(event) - this.pressX, this.modelY(event) - this.pressY) > 4;
    java.util.Set<Hit> region = null;
    if (moved && this.draggingEnabled && this.pressed == null) {
      region = new java.util.LinkedHashSet<>();
      for (final var component : this.document.experiment().components()) {
        final var hidden = this.hidden(component.id(), this.document.layouts().get(component.id()));
        for (final var atom : this.document.layouts().get(component.id()).atoms()) {
          if (hidden.contains(atom.atomId())) {
            continue;
          }
          final var point = atom.point2D();
          if (point.x() >= Math.min(this.pressX, this.modelX(event))
              && point.x() <= Math.max(this.pressX, this.modelX(event))
              && point.y() >= Math.min(this.pressY, this.modelY(event))
              && point.y() <= Math.max(this.pressY, this.modelY(event))) {
            region.add(new Hit(component.id(), atom.atomId()));
            final var group =
                MoleculeAbbreviations.projection(this.document.layouts().get(component.id()))
                    .anchors()
                    .get(atom.atomId());
            if (group != null) {
              for (final var member : group.atoms()) {
                region.add(new Hit(component.id(), member));
              }
            }
          }
        }
      }
    }
    this.previewX = 0;
    this.previewY = 0;
    this.action.accept(
        new Gesture(
            moved && this.draggingEnabled
                ? this.pressed
                : this.hit(this.modelX(event), this.modelY(event)),
            this.modelX(event),
            this.modelY(event),
            moved,
            region,
            event.isShiftDown(),
            this.modelX(event) - this.pressX,
            this.modelY(event) - this.pressY,
            this.pressed,
            this.bondHit(this.modelX(event), this.modelY(event))));
    this.draw();
  }

  private Hit hit(final double x, final double y) {
    if (this.document == null) {
      return null;
    }
    double nearest = Math.min(12, 12 / this.zoom);
    Hit result = null;
    for (final LaboratoryComponent component : this.document.experiment().components()) {
      final var hidden = this.hidden(component.id(), this.document.layouts().get(component.id()));
      for (final AtomDrawing atom : this.document.layouts().get(component.id()).atoms()) {
        if (hidden.contains(atom.atomId())) {
          continue;
        }
        final var drawing = this.document.layouts().get(component.id());
        final var model =
            drawing.molecule().atoms().stream()
                .filter(value -> value.id().equals(atom.atomId()))
                .findFirst()
                .orElseThrow();
        final var group = MoleculeAbbreviations.projection(drawing).anchors().get(atom.atomId());
        final var bounds =
            group != null
                ? abbreviationBounds(drawing, group)
                : labelled(drawing, model) ? atomLabel(drawing, model, hidden).bounds() : null;
        final double distance =
            bounds == null
                ? Math.hypot(x - atom.point2D().x(), y - atom.point2D().y())
                : Math.hypot(
                    Math.max(0, Math.max(bounds.getMinX() - x, x - bounds.getMaxX())),
                    Math.max(0, Math.max(bounds.getMinY() - y, y - bounds.getMaxY())));
        if (distance <= nearest) {
          nearest = distance;
          result = new Hit(component.id(), atom.atomId());
        }
      }
    }
    return result;
  }

  private BondHit bondHit(final double x, final double y) {
    if (this.document == null || this.hit(x, y) != null) {
      return null;
    }
    double nearest = 7 / this.zoom;
    BondHit result = null;
    for (final var component : this.document.experiment().components()) {
      final var drawing = this.document.layouts().get(component.id());
      final var hidden = this.hidden(component.id(), drawing);
      for (final var bond : drawing.molecule().bonds()) {
        if (hidden.contains(bond.firstAtomId()) || hidden.contains(bond.secondAtomId())) {
          continue;
        }
        final var a = position(drawing, bond.firstAtomId()).point2D();
        final var b = position(drawing, bond.secondAtomId()).point2D();
        final double dx = b.x() - a.x();
        final double dy = b.y() - a.y();
        final double lengthSquared = dx * dx + dy * dy;
        if (lengthSquared < 1) {
          continue;
        }
        final double t =
            Math.max(0, Math.min(1, ((x - a.x()) * dx + (y - a.y()) * dy) / lengthSquared));
        final double distance = Math.hypot(x - a.x() - t * dx, y - a.y() - t * dy);
        if (distance < nearest) {
          nearest = distance;
          result = new BondHit(component.id(), bond.firstAtomId(), bond.secondAtomId());
        }
      }
    }
    return result;
  }

  private void highlightBond(
      final GraphicsContext graphics, final BondHit hit, final double opacity) {
    if (hit == null || !this.document.layouts().containsKey(hit.componentId())) {
      return;
    }
    final var drawing = this.document.layouts().get(hit.componentId());
    if (drawing.molecule().bonds().stream()
        .noneMatch(bond -> bond.connects(hit.first(), hit.second()))) {
      return;
    }
    final var a = position(drawing, hit.first()).point2D();
    final var b = position(drawing, hit.second()).point2D();
    graphics.setStroke(Color.color(0.12, 0.46, 0.82, opacity));
    graphics.setLineWidth(10 / this.zoom);
    graphics.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
    graphics.strokeLine(a.x(), a.y(), b.x(), b.y());
  }

  private void draw() {
    final GraphicsContext graphics = this.canvas.getGraphicsContext2D();
    graphics.setTransform(1, 0, 0, 1, 0, 0);
    graphics.setFill(Color.WHITE);
    graphics.fillRect(0, 0, this.canvas.getWidth(), this.canvas.getHeight());
    if (this.document == null || this.document.experiment().components().isEmpty()) {
      graphics.setFill(Color.web("#64748b"));
      graphics.setFont(Font.font(17));
      graphics.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
      graphics.fillText(
          "Начните со структуры",
          this.canvas.getWidth() / 2,
          this.canvas.getHeight() / 2 - 20,
          Math.max(1, this.canvas.getWidth() - 24));
      graphics.setFont(Font.font(13));
      graphics.fillText(
          this.canvas.getWidth() < 480
              ? "Связь или кольцо → нажмите на лист"
              : "Выберите связь или кольцо слева и нажмите на лист",
          this.canvas.getWidth() / 2,
          this.canvas.getHeight() / 2 + 10,
          Math.max(1, this.canvas.getWidth() - 24));
      graphics.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
      return;
    }
    graphics.scale(this.zoom, this.zoom);
    graphics.translate(this.originX, this.originY);
    this.highlightBond(graphics, this.selectedBond, 0.25);
    this.highlightBond(graphics, this.hoverBond, 0.15);
    if (this.hoverAtom != null
        && this.document.layouts().containsKey(this.hoverAtom.componentId())) {
      final var point =
          position(
                  this.document.layouts().get(this.hoverAtom.componentId()),
                  this.hoverAtom.atomId())
              .point2D();
      graphics.setFill(Color.color(0.12, 0.46, 0.82, 0.15));
      graphics.fillOval(point.x() - 9, point.y() - 9, 18, 18);
    }
    for (final LaboratoryComponent component : this.document.experiment().components()) {
      MoleculeDrawing drawing = this.document.layouts().get(component.id());
      final java.util.Set<AtomId> selectedAtoms = new java.util.LinkedHashSet<>();
      this.selection.stream()
          .filter(hit -> hit.componentId().equals(component.id()))
          .forEach(hit -> selectedAtoms.add(hit.atomId()));
      if (this.draggingEnabled && (this.previewX != 0 || this.previewY != 0)) {
        final java.util.Set<AtomId> movedAtoms = new java.util.LinkedHashSet<>();
        this.moving.stream()
            .filter(hit -> hit.componentId().equals(component.id()))
            .forEach(hit -> movedAtoms.add(hit.atomId()));
        drawing =
            new ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeSketchService()
                .translateAtoms(drawing, movedAtoms, this.previewX, this.previewY);
      }
      if (this.rotationTarget != null && this.rotationTarget.componentId().equals(component.id())) {
        drawing =
            ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeStereoEditing.rotate(
                drawing, this.rotationAngle);
        final var center =
            ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeStereoEditing.center(
                this.document.layouts().get(component.id()));
        graphics.setStroke(Color.web("#3983bc"));
        graphics.setLineWidth(1 / this.zoom);
        graphics.strokeOval(center.x() - 4, center.y() - 4, 8, 8);
      }
      paintMolecule(graphics, drawing, selectedAtoms, this.hidden(component.id(), drawing));
      if (this.showNames) {
        final var visible = drawingBounds(drawing, this.hidden(component.id(), drawing));
        final double center = (visible.getMinX() + visible.getMaxX()) / 2;
        final double bottom = visible.getMaxY();
        graphics.setFill(Color.web("#596777"));
        graphics.setFont(Font.font("Arial", 13));
        graphics.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        graphics.fillText(caption(component.molecule().displayName()), center, bottom + 34);
        graphics.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
      }
    }
  }

  static void paintMolecule(
      final GraphicsContext graphics, final MoleculeDrawing drawing, final AtomId selectedAtom) {
    paintMolecule(
        graphics,
        drawing,
        selectedAtom == null ? java.util.Set.of() : java.util.Set.of(selectedAtom));
  }

  static void paintPreview(final GraphicsContext graphics, final MoleculeDrawing drawing) {
    paintMolecule(graphics, drawing, java.util.Set.of(), previewHiddenHydrogens(drawing));
  }

  static void paintPreview(
      final GraphicsContext graphics, final MoleculeDrawing drawing, final boolean stereoLabels) {
    paintMolecule(
        graphics, drawing, java.util.Set.of(), previewHiddenHydrogens(drawing), stereoLabels);
  }

  static void paintPreview(
      final GraphicsContext graphics,
      final MoleculeDrawing drawing,
      final boolean stereoLabels,
      final LaboratoryPreviewTheme.Palette palette) {
    paintMolecule(
        graphics,
        drawing,
        java.util.Set.of(),
        previewHiddenHydrogens(drawing),
        stereoLabels,
        palette);
  }

  static void paintMolecule(
      final GraphicsContext graphics,
      final MoleculeDrawing drawing,
      final AtomId selectedAtom,
      final LaboratoryPreviewTheme.Palette palette) {
    paintMolecule(
        graphics,
        drawing,
        selectedAtom == null ? java.util.Set.of() : java.util.Set.of(selectedAtom),
        java.util.Set.of(),
        true,
        palette);
  }

  static java.util.List<AtomDrawing> previewAtoms(final MoleculeDrawing drawing) {
    final var hidden = previewHiddenHydrogens(drawing);
    hidden.addAll(MoleculeAbbreviations.projection(drawing).hidden());
    return drawing.atoms().stream().filter(atom -> !hidden.contains(atom.atomId())).toList();
  }

  static javafx.geometry.Rectangle2D previewBounds(final MoleculeDrawing drawing) {
    final var bounds = drawingBounds(drawing, previewHiddenHydrogens(drawing));
    return new javafx.geometry.Rectangle2D(
        bounds.getMinX() - 12,
        bounds.getMinY() - 12,
        bounds.getWidth() + 24,
        bounds.getHeight() + 24);
  }

  /** Extra room only when a complex/tall structure would otherwise make atom text too small. */
  static double previewHeight(
      final MoleculeDrawing drawing, final double minimum, final double maximum) {
    return Math.max(minimum, Math.min(maximum, previewBounds(drawing).getHeight() + 32));
  }

  private static java.util.Set<AtomId> previewHiddenHydrogens(final MoleculeDrawing drawing) {
    final java.util.Set<AtomId> hidden = new java.util.HashSet<>();
    for (final Atom atom : drawing.molecule().atoms()) {
      if (!atom.element().symbol().value().equals("H")
          || atom.hasIsotope()
          || atom.formalCharge().value() != 0
          || atom.radicalState().radical()) {
        continue;
      }
      final var bonds =
          drawing.molecule().bonds().stream()
              .filter(
                  bond ->
                      bond.firstAtomId().equals(atom.id()) || bond.secondAtomId().equals(atom.id()))
              .toList();
      if (bonds.size() != 1 || bonds.getFirst().type() != BondType.SINGLE) {
        continue;
      }
      final Bond bond = bonds.getFirst();
      final AtomId other =
          bond.firstAtomId().equals(atom.id()) ? bond.secondAtomId() : bond.firstAtomId();
      final boolean carbon =
          drawing.molecule().atoms().stream()
              .anyMatch(
                  value ->
                      value.id().equals(other)
                          && java.util.Set.of("C", "N", "O", "S")
                              .contains(value.element().symbol().value()));
      final boolean plain =
          drawing.bonds().stream()
              .anyMatch(
                  value ->
                      value.connects(atom.id(), other) && value.style() == BondDrawingStyle.PLAIN);
      if (carbon && plain) {
        hidden.add(atom.id());
      }
    }
    return hidden;
  }

  private static void paintMolecule(
      final GraphicsContext graphics,
      final MoleculeDrawing drawing,
      final java.util.Set<AtomId> selectedAtoms) {
    paintMolecule(graphics, drawing, selectedAtoms, java.util.Set.of());
  }

  private static void paintMolecule(
      final GraphicsContext graphics,
      final MoleculeDrawing drawing,
      final java.util.Set<AtomId> selectedAtoms,
      final java.util.Set<AtomId> suppliedHidden) {
    paintMolecule(graphics, drawing, selectedAtoms, suppliedHidden, true);
  }

  private static void paintMolecule(
      final GraphicsContext graphics,
      final MoleculeDrawing drawing,
      final java.util.Set<AtomId> selectedAtoms,
      final java.util.Set<AtomId> suppliedHidden,
      final boolean stereoLabels) {
    paintMolecule(
        graphics,
        drawing,
        selectedAtoms,
        suppliedHidden,
        stereoLabels,
        LaboratoryPreviewTheme.PAPER);
  }

  private static void paintMolecule(
      final GraphicsContext graphics,
      final MoleculeDrawing drawing,
      final java.util.Set<AtomId> selectedAtoms,
      final java.util.Set<AtomId> suppliedHidden,
      final boolean stereoLabels,
      final LaboratoryPreviewTheme.Palette palette) {
    final var projection = MoleculeAbbreviations.projection(drawing);
    final var hiddenAtoms = new java.util.HashSet<>(suppliedHidden);
    hiddenAtoms.addAll(projection.hidden());
    final var annotations =
        stereoLabels ? stereoPlacements(drawing, hiddenAtoms) : java.util.List.<StereoLabel>of();
    for (final Bond bond : drawing.molecule().bonds()) {
      if (hiddenAtoms.contains(bond.firstAtomId()) || hiddenAtoms.contains(bond.secondAtomId())) {
        continue;
      }
      bond(graphics, drawing, bond, palette);
    }
    paintStereoLeaders(graphics, drawing, annotations, palette);
    for (final Atom atom : drawing.molecule().atoms()) {
      if (hiddenAtoms.contains(atom.id())) {
        continue;
      }
      final AtomDrawing layout = position(drawing, atom.id());
      final double x = layout.point2D().x();
      final double y = layout.point2D().y();
      final boolean chosen = selectedAtoms.contains(atom.id());
      final var abbreviation = projection.anchors().get(atom.id());
      if (abbreviation != null) {
        final boolean groupChosen =
            chosen || abbreviation.atoms().stream().anyMatch(selectedAtoms::contains);
        final var bounds = abbreviationBounds(drawing, abbreviation);
        graphics.setFill(groupChosen ? palette.selectedBackground() : palette.background());
        graphics.fillRoundRect(
            bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight(), 4, 4);
        if (groupChosen) {
          graphics.setStroke(palette.selection());
          graphics.setLineWidth(0.9);
          graphics.strokeRoundRect(
              bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight(), 4, 4);
        }
        graphics.setFill(palette.ink());
        graphics.setFont(Font.font("Arial", 18));
        graphics.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        graphics.fillText(abbreviation.label(), x, y + 6);
        graphics.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
        continue;
      }
      if (chosen && !labelled(drawing, atom)) {
        graphics.setFill(
            palette.dark() ? palette.selectedBackground() : Color.color(0.12, 0.45, 0.8, 0.15));
        graphics.fillOval(x - 10, y - 10, 20, 20);
        graphics.setStroke(palette.selection());
        graphics.setLineWidth(1);
        graphics.strokeOval(x - 10, y - 10, 20, 20);
      }
      if (!labelled(drawing, atom)) {
        continue;
      }
      final AtomLabel label = atomLabel(drawing, atom, hiddenAtoms);
      final String symbol = label.symbol();
      final String hydrogenLabel = label.hydrogenLabel();
      final boolean groupLeft = label.groupLeft();
      final double halfWidth = label.halfWidth();
      final double isotopeWidth = label.isotopeWidth();
      final double right = label.bodyRight();
      graphics.setFill(chosen ? palette.selectedBackground() : palette.background());
      graphics.fillRoundRect(
          label.bounds().getMinX(),
          label.bounds().getMinY(),
          label.bounds().getWidth(),
          label.bounds().getHeight(),
          4,
          4);
      if (chosen) {
        graphics.setStroke(palette.selection());
        graphics.setLineWidth(0.9);
        graphics.strokeRoundRect(
            label.bounds().getMinX(),
            label.bounds().getMinY(),
            label.bounds().getWidth(),
            label.bounds().getHeight(),
            4,
            4);
      }
      graphics.setFill(palette.atom(symbol));
      graphics.setFont(Font.font("Arial", 18));
      graphics.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
      graphics.fillText(symbol, x, y + 6);
      if (!hydrogenLabel.isEmpty()) {
        graphics.setTextAlign(
            groupLeft
                ? javafx.scene.text.TextAlignment.RIGHT
                : javafx.scene.text.TextAlignment.LEFT);
        graphics.fillText(
            hydrogenLabel, groupLeft ? x - halfWidth - isotopeWidth : x + halfWidth, y + 6);
      }
      graphics.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
      graphics.setFont(Font.font(11));
      if (atom.hasIsotope()) {
        graphics.setTextAlign(javafx.scene.text.TextAlignment.RIGHT);
        graphics.fillText(Integer.toString(atom.isotope().massNumber()), x - halfWidth - 1, y - 5);
        graphics.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
      }
      if (atom.radicalState().radical()) {
        graphics.fillText("·".repeat(atom.radicalState().unpairedElectrons()), right + 1, y + 12);
      }
      if (atom.formalCharge().value() != 0) {
        graphics.fillText(
            (Math.abs(atom.formalCharge().value()) == 1
                    ? ""
                    : Integer.toString(Math.abs(atom.formalCharge().value())))
                + (atom.formalCharge().value() > 0 ? "+" : "−"),
            right + 1,
            y - 8);
      }
    }
    if (stereoLabels) {
      paintStereo(graphics, drawing, annotations, palette);
    }
  }

  private record AtomLabel(
      String symbol,
      String hydrogenLabel,
      boolean groupLeft,
      double halfWidth,
      double isotopeWidth,
      double bodyRight,
      javafx.geometry.Rectangle2D bounds) {}

  private static AtomLabel atomLabel(
      final MoleculeDrawing drawing, final Atom atom, final java.util.Set<AtomId> hiddenAtoms) {
    final var point = position(drawing, atom.id()).point2D();
    final double x = point.x(), y = point.y();
    final String symbol = atom.element().symbol().value();
    final long hydrogens =
        symbol.equals("C")
            ? 0
            : drawing.molecule().bonds().stream()
                .filter(
                    b -> b.firstAtomId().equals(atom.id()) || b.secondAtomId().equals(atom.id()))
                .map(b -> b.firstAtomId().equals(atom.id()) ? b.secondAtomId() : b.firstAtomId())
                .filter(hiddenAtoms::contains)
                .count();
    final String hydrogenLabel =
        hydrogens == 0
            ? ""
            : "H"
                + (hydrogens == 1
                    ? ""
                    : Long.toString(hydrogens)
                        .chars()
                        .mapToObj(c -> Character.toString((char) ('₀' + c - '0')))
                        .collect(java.util.stream.Collectors.joining()));
    final double halfWidth = textWidth(symbol, 18) / 2;
    final double hydrogenWidth = textWidth(hydrogenLabel, 18);
    boolean groupLeft =
        hydrogens > 0
            && drawing.molecule().bonds().stream()
                    .filter(
                        b ->
                            b.firstAtomId().equals(atom.id()) || b.secondAtomId().equals(atom.id()))
                    .map(
                        b -> b.firstAtomId().equals(atom.id()) ? b.secondAtomId() : b.firstAtomId())
                    .filter(id -> !hiddenAtoms.contains(id))
                    .mapToDouble(id -> position(drawing, id).point2D().x() - x)
                    .average()
                    .orElse(5)
                > 4;
    final double isotopeWidth =
        atom.hasIsotope() ? textWidth(Integer.toString(atom.isotope().massNumber()), 11) + 1 : 0;
    if (hydrogens > 0) {
      final double leftPenalty =
          hydrogenLabelPenalty(
              drawing,
              atom.id(),
              hiddenAtoms,
              x - halfWidth - isotopeWidth - hydrogenWidth - 2,
              x - halfWidth - isotopeWidth,
              y);
      final double rightPenalty =
          hydrogenLabelPenalty(
              drawing, atom.id(), hiddenAtoms, x + halfWidth, x + halfWidth + hydrogenWidth + 2, y);
      if (Math.abs(leftPenalty - rightPenalty) > 0.01) {
        groupLeft = leftPenalty < rightPenalty;
      }
    }
    final double left = x - halfWidth - (isotopeWidth + (groupLeft ? hydrogenWidth : 0)) - 2;
    final double right = x + halfWidth + (groupLeft ? 0 : hydrogenWidth) + 2;
    final String charge =
        atom.formalCharge().value() == 0
            ? ""
            : (Math.abs(atom.formalCharge().value()) == 1
                    ? ""
                    : Integer.toString(Math.abs(atom.formalCharge().value())))
                + (atom.formalCharge().value() > 0 ? "+" : "−");
    final double extra =
        Math.max(
            textWidth(charge, 11),
            textWidth("·".repeat(atom.radicalState().unpairedElectrons()), 11));
    final double top = y - (atom.hasIsotope() || !charge.isEmpty() ? 20 : 14);
    final double bottom = y + (atom.radicalState().radical() ? 15 : 11);
    return new AtomLabel(
        symbol,
        hydrogenLabel,
        groupLeft,
        halfWidth,
        isotopeWidth,
        right,
        new javafx.geometry.Rectangle2D(
            left, top, right - left + (extra == 0 ? 0 : extra + 3), bottom - top));
  }

  private static double hydrogenLabelPenalty(
      final MoleculeDrawing drawing,
      final AtomId anchor,
      final java.util.Set<AtomId> hiddenAtoms,
      final double left,
      final double right,
      final double y) {
    double penalty = 0;
    for (final Atom other : drawing.molecule().atoms()) {
      if (other.id().equals(anchor)
          || hiddenAtoms.contains(other.id())
          || !labelled(drawing, other)) {
        continue;
      }
      final var point = position(drawing, other.id()).point2D();
      final double half = textWidth(other.element().symbol().value(), 18) / 2;
      final double isotope =
          other.hasIsotope()
              ? textWidth(Integer.toString(other.isotope().massNumber()), 11) + 1
              : 0;
      final var neighbors =
          drawing.molecule().bonds().stream()
              .filter(
                  b -> b.firstAtomId().equals(other.id()) || b.secondAtomId().equals(other.id()))
              .map(b -> b.firstAtomId().equals(other.id()) ? b.secondAtomId() : b.firstAtomId())
              .toList();
      final long count =
          other.element().symbol().value().equals("C")
              ? 0
              : neighbors.stream().filter(hiddenAtoms::contains).count();
      final double hydrogenWidth =
          count == 0
              ? 0
              : textWidth(
                  "H"
                      + (count == 1
                          ? ""
                          : Long.toString(count)
                              .chars()
                              .mapToObj(c -> Character.toString((char) ('₀' + c - '0')))
                              .collect(java.util.stream.Collectors.joining())),
                  18);
      final boolean otherLeft =
          neighbors.stream()
                  .filter(id -> !hiddenAtoms.contains(id))
                  .mapToDouble(id -> position(drawing, id).point2D().x() - point.x())
                  .average()
                  .orElse(5)
              > 4;
      final double otherStart = point.x() - half - isotope - 3 - (otherLeft ? hydrogenWidth : 0);
      final double otherEnd = point.x() + half + 3 + (otherLeft ? 0 : hydrogenWidth);
      final double width = Math.max(0, Math.min(right, otherEnd) - Math.max(left, otherStart));
      final double height =
          Math.max(0, Math.min(y + 11, point.y() + 12) - Math.max(y - 14, point.y() - 15));
      penalty += width * height;
    }
    return penalty;
  }

  private static javafx.geometry.Rectangle2D drawingBounds(
      final MoleculeDrawing drawing, final java.util.Set<AtomId> suppliedHidden) {
    final var projection = MoleculeAbbreviations.projection(drawing);
    final var hiddenAtoms = new java.util.HashSet<>(suppliedHidden);
    hiddenAtoms.addAll(projection.hidden());
    double left = Double.POSITIVE_INFINITY,
        top = left,
        right = Double.NEGATIVE_INFINITY,
        bottom = right;
    for (final Atom atom : drawing.molecule().atoms()) {
      if (hiddenAtoms.contains(atom.id())) {
        continue;
      }
      final var point = position(drawing, atom.id()).point2D();
      final var group = projection.anchors().get(atom.id());
      final var bounds =
          group != null
              ? abbreviationBounds(drawing, group)
              : labelled(drawing, atom)
                  ? atomLabel(drawing, atom, hiddenAtoms).bounds()
                  : new javafx.geometry.Rectangle2D(point.x() - 2, point.y() - 2, 4, 4);
      left = Math.min(left, bounds.getMinX());
      top = Math.min(top, bounds.getMinY());
      right = Math.max(right, bounds.getMaxX());
      bottom = Math.max(bottom, bounds.getMaxY());
    }
    for (final var label : stereoPlacements(drawing, hiddenAtoms)) {
      left = Math.min(left, label.bounds().getMinX());
      top = Math.min(top, label.bounds().getMinY());
      right = Math.max(right, label.bounds().getMaxX());
      bottom = Math.max(bottom, label.bounds().getMaxY());
    }
    return Double.isFinite(left)
        ? new javafx.geometry.Rectangle2D(left, top, right - left, bottom - top)
        : new javafx.geometry.Rectangle2D(0, 0, 0, 0);
  }

  private static double textWidth(final String value, final double size) {
    final var text = new javafx.scene.text.Text(value);
    text.setFont(Font.font("Arial", size));
    return text.getLayoutBounds().getWidth();
  }

  private static javafx.geometry.Rectangle2D abbreviationBounds(
      final MoleculeDrawing drawing, final MoleculeAbbreviation group) {
    final var point = position(drawing, group.anchor()).point2D();
    final double width = textWidth(group.label(), 18) + 8;
    return new javafx.geometry.Rectangle2D(point.x() - width / 2, point.y() - 14, width, 26);
  }

  private static java.util.Map<AtomId, String> stereoLabels(
      final MoleculeDrawing drawing, final java.util.Set<AtomId> hidden) {
    final var molecule = drawing.molecule();
    final boolean review =
        molecule
            .metadata()
            .attributes()
            .containsKey(
                ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereochemistry
                    .REVIEW_ATTRIBUTE);
    final var labels = new java.util.LinkedHashMap<AtomId, String>();
    final var relativeAtoms = new java.util.HashSet<AtomId>();
    try {
      final var relative =
          ru.pathcreator.vadim.quantum.chemistry.domain.stereo.MolecularRelativeStereo.read(
              molecule);
      if (relative.isPresent()) {
        final boolean current = relative.get().matches(molecule);
        for (final var group : relative.get().groups()) {
          for (final var member : group.members()) {
            relativeAtoms.add(member.atom());
            labels.put(
                member.atom(),
                current
                    ? (group.kind()
                                == ru.pathcreator.vadim.quantum.chemistry.domain.stereo
                                    .MolecularRelativeStereo.Kind.RACEMIC
                            ? "rac "
                            : "")
                        + (member.descriptor()
                                == ru.pathcreator.vadim.quantum.chemistry.domain.stereo
                                    .MolecularRelativeStereo.Descriptor.UNKNOWN
                            ? "abs?"
                            : member.descriptor().toString())
                    : "rel!");
          }
        }
      }
    } catch (final RuntimeException ignored) {
    }
    final java.util.Map<AtomId, String> calculated = new java.util.HashMap<>();
    if (!review && relativeAtoms.isEmpty()) {
      try {
        final var saved =
            ru.pathcreator.vadim.quantum.chemistry.domain.stereo.MolecularCipLabels.read(molecule);
        if (saved.isPresent()) {
          final var value = saved.get();
          final var source =
              value.method().endsWith("DRAWING_2D")
                  ? ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange
                      .CdkCipCalculator.Source.DRAWING_2D
                  : ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange
                      .CdkCipCalculator.Source.STORED_CONFIGURATION;
          if (value
              .inputSignature()
              .equals(
                  ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange
                      .CdkCipCalculator.signature(drawing, source))) {
            value.labels().forEach(l -> calculated.put(l.first(), l.descriptor()));
          }
        }
      } catch (final RuntimeException ignored) {
      }
    }
    final java.util.Set<AtomId> parity = new java.util.HashSet<>();
    try {
      parity.addAll(
          ru.pathcreator.vadim.quantum.chemistry.domain.stereo.MolecularStereoParity.read(molecule)
              .keySet());
    } catch (final RuntimeException ignored) {
    }
    for (final var center : molecule.stereochemistry().centers()) {
      if (relativeAtoms.contains(center.primaryAtomId())
          || hidden.contains(center.primaryAtomId())
          || center.hasSecondaryAtom() && hidden.contains(center.secondaryAtomId())) {
        continue;
      }
      final boolean unknown =
          center.descriptor()
              == ru.pathcreator.vadim.quantum.chemistry.domain.stereo.StereochemicalDescriptor
                  .UNKNOWN;
      final String label =
          calculated.getOrDefault(
              center.primaryAtomId(),
              unknown
                  ? parity.contains(center.primaryAtomId()) && !review ? "" : "?"
                  : center.descriptor().name().replace('_', ' '));
      if (!label.isEmpty()) {
        labels.put(center.primaryAtomId(), label + (review ? "!" : ""));
      }
    }
    calculated.forEach(labels::putIfAbsent);
    labels
        .keySet()
        .removeIf(
            id ->
                hidden.contains(id)
                    || drawing.atoms().stream().noneMatch(a -> a.atomId().equals(id)));
    return labels;
  }

  private record StereoLabel(
      AtomId atom, String text, javafx.geometry.Rectangle2D bounds, boolean leader) {}

  private record LabelBond(double x1, double y1, double x2, double y2, double clearance) {}

  /** Presentation only: reserve atom text, abbreviations, bonds and earlier annotations. */
  private static java.util.List<StereoLabel> stereoPlacements(
      final MoleculeDrawing drawing, final java.util.Set<AtomId> hidden) {
    final var labels = stereoLabels(drawing, hidden);
    if (labels.isEmpty()) {
      return java.util.List.of();
    }
    final var obstacles = new java.util.ArrayList<javafx.geometry.Rectangle2D>();
    final var projection = MoleculeAbbreviations.projection(drawing);
    for (final Atom atom : drawing.molecule().atoms()) {
      if (hidden.contains(atom.id())) {
        continue;
      }
      final var point = position(drawing, atom.id()).point2D();
      final var group = projection.anchors().get(atom.id());
      obstacles.add(
          group != null
              ? abbreviationBounds(drawing, group)
              : labelled(drawing, atom)
                  ? atomLabel(drawing, atom, hidden).bounds()
                  : new javafx.geometry.Rectangle2D(point.x() - 3, point.y() - 3, 6, 6));
    }
    final var bonds = new java.util.ArrayList<LabelBond>();
    for (final Bond bond : drawing.molecule().bonds()) {
      if (hidden.contains(bond.firstAtomId()) || hidden.contains(bond.secondAtomId())) {
        continue;
      }
      final var a = position(drawing, bond.firstAtomId()).point2D();
      final var b = position(drawing, bond.secondAtomId()).point2D();
      final var style =
          drawing.bonds().stream()
              .filter(v -> v.connects(bond.firstAtomId(), bond.secondAtomId()))
              .findFirst()
              .orElseThrow()
              .style();
      final double clearance =
          bond.type() != BondType.SINGLE || style != BondDrawingStyle.PLAIN ? 8 : 3;
      bonds.add(new LabelBond(a.x(), a.y(), b.x(), b.y(), clearance));
    }
    final var result = new java.util.ArrayList<StereoLabel>();
    for (final var entry : labels.entrySet()) {
      final var point = position(drawing, entry.getKey()).point2D();
      final double width = textWidth(entry.getValue(), 11) + 6, height = 17;
      javafx.geometry.Rectangle2D chosen = null;
      double best = Double.POSITIVE_INFINITY;
      // Nearest readable location wins; distance and angle remain deterministic on resize/theme
      // changes.
      for (final double gap : new double[] {8, 16, 28, 44, 64}) {
        final double radius = Math.hypot(width / 2, height / 2) + gap;
        for (int index = 0; index < 16; index++) {
          final double angle = -Math.PI / 4 + index * Math.PI / 8;
          final double cx = point.x() + Math.cos(angle) * radius,
              cy = point.y() + Math.sin(angle) * radius;
          final var candidate =
              new javafx.geometry.Rectangle2D(cx - width / 2, cy - height / 2, width, height);
          if (!labelSpace(candidate, obstacles, bonds)) {
            continue;
          }
          // Avoid a label closer to a different annotated center than to its own center.
          double score = radius + index * 0.03;
          for (final AtomId other : labels.keySet()) {
            if (other.equals(entry.getKey())) {
              continue;
            }
            final var p = position(drawing, other).point2D();
            score += Math.max(0, radius - Math.hypot(cx - p.x(), cy - p.y())) * 2;
          }
          if (score < best) {
            chosen = candidate;
            best = score;
          }
        }
        if (chosen != null) {
          break;
        }
      }
      if (chosen == null) {
        // Extremely dense drawings: a callout above the occupied area never covers a chemical
        // symbol.
        final double top =
            obstacles.stream()
                .mapToDouble(javafx.geometry.Rectangle2D::getMinY)
                .min()
                .orElse(point.y());
        chosen =
            new javafx.geometry.Rectangle2D(point.x() - width / 2, top - height - 8, width, height);
      }
      final double edgeDistance =
          Math.hypot(
              Math.max(0, Math.abs(chosen.getMinX() + width / 2 - point.x()) - width / 2),
              Math.max(0, Math.abs(chosen.getMinY() + height / 2 - point.y()) - height / 2));
      result.add(new StereoLabel(entry.getKey(), entry.getValue(), chosen, edgeDistance > 19));
      obstacles.add(chosen);
    }
    return java.util.List.copyOf(result);
  }

  private static boolean labelSpace(
      final javafx.geometry.Rectangle2D box,
      final java.util.List<javafx.geometry.Rectangle2D> obstacles,
      final java.util.List<LabelBond> bonds) {
    final var padded =
        new javafx.geometry.Rectangle2D(
            box.getMinX() - 3, box.getMinY() - 3, box.getWidth() + 6, box.getHeight() + 6);
    for (final var obstacle : obstacles) {
      if (padded.intersects(obstacle)) {
        return false;
      }
    }
    for (final var bond : bonds) {
      final double gap = bond.clearance();
      if (segmentIntersects(
          new javafx.geometry.Rectangle2D(
              box.getMinX() - gap,
              box.getMinY() - gap,
              box.getWidth() + 2 * gap,
              box.getHeight() + 2 * gap),
          bond)) {
        return false;
      }
    }
    return true;
  }

  private static boolean segmentIntersects(
      final javafx.geometry.Rectangle2D box, final LabelBond line) {
    // Slab intersection also handles horizontal, vertical and zero-length segments.
    double from = 0, to = 1;
    final double[] origin = {line.x1(), line.y1()},
        delta = {line.x2() - line.x1(), line.y2() - line.y1()};
    final double[] low = {box.getMinX(), box.getMinY()}, high = {box.getMaxX(), box.getMaxY()};
    for (int axis = 0; axis < 2; axis++) {
      if (Math.abs(delta[axis]) < 1e-10) {
        if (origin[axis] < low[axis] || origin[axis] > high[axis]) {
          return false;
        }
      } else {
        final double a = (low[axis] - origin[axis]) / delta[axis],
            b = (high[axis] - origin[axis]) / delta[axis];
        from = Math.max(from, Math.min(a, b));
        to = Math.min(to, Math.max(a, b));
        if (from > to) {
          return false;
        }
      }
    }
    return true;
  }

  private static void paintStereoLeaders(
      final GraphicsContext graphics,
      final MoleculeDrawing drawing,
      final java.util.List<StereoLabel> placements,
      final LaboratoryPreviewTheme.Palette palette) {
    final boolean review =
        drawing
            .molecule()
            .metadata()
            .attributes()
            .containsKey(
                ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereochemistry
                    .REVIEW_ATTRIBUTE);
    graphics.save();
    try {
      graphics.setLineWidth(0.65);
      graphics.setLineDashes(1, 3);
      for (final var label : placements) {
        if (!label.leader()) {
          continue;
        }
        final var p = position(drawing, label.atom()).point2D();
        final var box = label.bounds();
        final double x = Math.max(box.getMinX(), Math.min(box.getMaxX(), p.x()));
        final double y = Math.max(box.getMinY(), Math.min(box.getMaxY(), p.y()));
        graphics.setStroke(palette.stereo(review || label.text().equals("rel!")));
        graphics.strokeLine(p.x(), p.y(), x, y);
      }
    } finally {
      graphics.restore();
    }
  }

  private static void paintStereo(
      final GraphicsContext graphics,
      final MoleculeDrawing drawing,
      final java.util.List<StereoLabel> placements,
      final LaboratoryPreviewTheme.Palette palette) {
    final boolean review =
        drawing
            .molecule()
            .metadata()
            .attributes()
            .containsKey(
                ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereochemistry
                    .REVIEW_ATTRIBUTE);
    graphics.save();
    try {
      graphics.setFont(Font.font("Arial", 11));
      graphics.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
      for (final var label : placements) {
        final var box = label.bounds();
        final var color = palette.stereo(review || label.text().equals("rel!"));
        graphics.setFill(palette.background());
        graphics.fillRoundRect(box.getMinX(), box.getMinY(), box.getWidth(), box.getHeight(), 4, 4);
        graphics.setFill(color);
        graphics.fillText(label.text(), box.getMinX() + 3, box.getMinY() + 12);
      }
    } finally {
      graphics.restore();
    }
  }

  private static void bond(
      final GraphicsContext graphics,
      final MoleculeDrawing drawing,
      final Bond bond,
      final LaboratoryPreviewTheme.Palette palette) {
    final var first = position(drawing, bond.firstAtomId()).point2D();
    final var second = position(drawing, bond.secondAtomId()).point2D();
    final double length = Math.max(1, Math.hypot(second.x() - first.x(), second.y() - first.y()));
    final double dx = -(second.y() - first.y()) / length * 3;
    final double dy = (second.x() - first.x()) / length * 3;
    final int lines = bond.type() == BondType.DOUBLE ? 2 : bond.type() == BondType.TRIPLE ? 3 : 1;
    graphics.setStroke(palette.bond());
    graphics.setLineWidth(1.65);
    graphics.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
    graphics.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
    final var style =
        drawing.bonds().stream()
            .filter(value -> value.connects(bond.firstAtomId(), bond.secondAtomId()))
            .findFirst()
            .orElseThrow();
    if (style.style() == BondDrawingStyle.WEDGE_UP
        || style.style() == BondDrawingStyle.WEDGE_DOWN) {
      final var start = position(drawing, style.firstAtomId()).point2D();
      final var end = position(drawing, style.secondAtomId()).point2D();
      if (style.style() == BondDrawingStyle.WEDGE_UP) {
        graphics.setFill(palette.wedge());
        graphics.fillPolygon(
            new double[] {start.x(), end.x() + dx * 2, end.x() - dx * 2},
            new double[] {start.y(), end.y() + dy * 2, end.y() - dy * 2},
            3);
      } else {
        for (int index = 1; index < 9; index++) {
          final double fraction = index / 9.0;
          final double x = start.x() + (end.x() - start.x()) * fraction;
          final double y = start.y() + (end.y() - start.y()) * fraction;
          graphics.strokeLine(
              x - dx * fraction * 2,
              y - dy * fraction * 2,
              x + dx * fraction * 2,
              y + dy * fraction * 2);
        }
      }
      return;
    }
    if (style.style() == BondDrawingStyle.WAVY) {
      final int segments = Math.max(12, (int) (length / 2));
      graphics.beginPath();
      graphics.moveTo(first.x(), first.y());
      for (int index = 1; index <= segments; index++) {
        final double fraction = index / (double) segments;
        final double offset =
            Math.sin(fraction * Math.PI * Math.max(2, Math.round(length / 8)) * 2);
        graphics.lineTo(
            first.x() + (second.x() - first.x()) * fraction + dx * offset,
            first.y() + (second.y() - first.y()) * fraction + dy * offset);
      }
      graphics.stroke();
      return;
    }
    if (style.style() == BondDrawingStyle.COORDINATE_ARROW || bond.type() == BondType.COORDINATE) {
      final var start = position(drawing, style.firstAtomId()).point2D();
      final var end = position(drawing, style.secondAtomId()).point2D();
      final double ux = (end.x() - start.x()) / length;
      final double uy = (end.y() - start.y()) / length;
      final double tipX = end.x() - ux * 14;
      final double tipY = end.y() - uy * 14;
      graphics.strokeLine(start.x(), start.y(), tipX, tipY);
      graphics.strokeLine(tipX, tipY, tipX - ux * 10 - uy * 5, tipY - uy * 10 + ux * 5);
      graphics.strokeLine(tipX, tipY, tipX - ux * 10 + uy * 5, tipY - uy * 10 - ux * 5);
      return;
    }
    final java.util.List<AtomId> cycle = cycle(drawing, bond);
    if ((bond.type() == BondType.DOUBLE || bond.type() == BondType.AROMATIC)
        && !cycle.isEmpty()
        && style.style() == BondDrawingStyle.PLAIN) {
      final double centerX =
          cycle.stream()
              .mapToDouble(id -> position(drawing, id).point2D().x())
              .average()
              .orElseThrow();
      final double centerY =
          cycle.stream()
              .mapToDouble(id -> position(drawing, id).point2D().y())
              .average()
              .orElseThrow();
      final double side = Math.signum((centerX - first.x()) * dx + (centerY - first.y()) * dy);
      graphics.strokeLine(first.x(), first.y(), second.x(), second.y());
      if (bond.type() == BondType.AROMATIC) {
        graphics.setLineDashes(4, 3);
      }
      final double inset = Math.min(0.2, 8 / length);
      graphics.strokeLine(
          first.x() + (second.x() - first.x()) * inset + dx * side * 2,
          first.y() + (second.y() - first.y()) * inset + dy * side * 2,
          second.x() - (second.x() - first.x()) * inset + dx * side * 2,
          second.y() - (second.y() - first.y()) * inset + dy * side * 2);
      graphics.setLineDashes();
      return;
    }
    graphics.setLineDashes(
        bond.type() == BondType.AROMATIC
                || style.style() == BondDrawingStyle.DASHED
                || style.style() == BondDrawingStyle.AROMATIC_RING
            ? new double[] {5, 3}
            : new double[0]);
    for (int index = 0; index < lines; index++) {
      final double offset = (index - (lines - 1) / 2.0) * 1.5;
      graphics.strokeLine(
          first.x() + dx * offset,
          first.y() + dy * offset,
          second.x() + dx * offset,
          second.y() + dy * offset);
    }
    graphics.setLineDashes();
    if (style.style() == BondDrawingStyle.UNKNOWN || bond.type() == BondType.UNKNOWN) {
      graphics.setFill(palette.dark() ? Color.web("#b6c4d6") : Color.web("#64748b"));
      graphics.setFont(Font.font(13));
      graphics.fillText("?", (first.x() + second.x()) / 2 + 5, (first.y() + second.y()) / 2 - 5);
    }
  }

  private static boolean labelled(final MoleculeDrawing drawing, final Atom atom) {
    return !atom.element().symbol().value().equals("C")
        || atom.hasIsotope()
        || atom.formalCharge().value() != 0
        || atom.radicalState().radical()
        || drawing.molecule().bonds().stream()
            .noneMatch(
                bond ->
                    bond.firstAtomId().equals(atom.id()) || bond.secondAtomId().equals(atom.id()));
  }

  /** Shortest alternate path identifies the ring side of a double bond, including fused rings. */
  private static java.util.List<AtomId> cycle(final MoleculeDrawing drawing, final Bond excluded) {
    final java.util.ArrayDeque<AtomId> queue = new java.util.ArrayDeque<>();
    final java.util.Map<AtomId, AtomId> parent = new java.util.HashMap<>();
    queue.add(excluded.firstAtomId());
    parent.put(excluded.firstAtomId(), null);
    while (!queue.isEmpty()) {
      final AtomId current = queue.removeFirst();
      if (current.equals(excluded.secondAtomId())) {
        final java.util.List<AtomId> path = new java.util.ArrayList<>();
        for (AtomId id = current; id != null; id = parent.get(id)) {
          path.add(id);
        }
        return path;
      }
      for (final Bond bond : drawing.molecule().bonds()) {
        if (bond.equals(excluded)) {
          continue;
        }
        final AtomId next =
            bond.firstAtomId().equals(current)
                ? bond.secondAtomId()
                : bond.secondAtomId().equals(current) ? bond.firstAtomId() : null;
        if (next != null && !parent.containsKey(next)) {
          parent.put(next, current);
          queue.add(next);
        }
      }
    }
    return java.util.List.of();
  }

  private static AtomDrawing position(final MoleculeDrawing drawing, final AtomId id) {
    return drawing.atoms().stream()
        .filter(atom -> atom.atomId().equals(id))
        .findFirst()
        .orElseThrow();
  }
}

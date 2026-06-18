/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.qsphere;

import java.util.ArrayList;
import java.util.List;

import javafx.animation.Animation;
import javafx.animation.RotateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;

/**
 * Показывает JavaFX 3D q-sphere для state-vector результата локальной симуляции.
 */
public final class DesktopQSphereView {

    private static final double SCENE_SIZE = 340.0;
    private static final double SPHERE_RADIUS = 92.0;
    private static final double EPSILON = 1.0e-12;
    private static final int MAX_RENDERED_MARKERS = 256;
    private static final int MAX_LEGEND_STATES = 20;

    private final DesktopQSphereProjection projection = new DesktopQSphereProjection();
    private final BorderPane root = new BorderPane();
    private final Group rotatingGroup = new Group();
    private final Rotate manualPitch = new Rotate(
        -12.0,
        Rotate.X_AXIS
    );
    private final Rotate manualYaw = new Rotate(
        -18.0,
        Rotate.Y_AXIS
    );
    private final RotateTransition rotation = new RotateTransition(
        Duration.seconds(22.0),
        rotatingGroup
    );
    private final boolean expandedView;
    private SimulationResult lastSimulation;
    private double dragAnchorX;
    private double dragAnchorY;
    private double dragAnchorPitch;
    private double dragAnchorYaw;
    private boolean russian;

    public DesktopQSphereView() {
        this(false);
    }

    private DesktopQSphereView(final boolean expandedView) {
        this.expandedView = expandedView;
        rotatingGroup.getTransforms().addAll(
            manualPitch,
            manualYaw
        );
        rotation.setByAngle(360.0);
        rotation.setAxis(Rotate.Y_AXIS);
        rotation.setCycleCount(Animation.INDEFINITE);
        rotation.play();
        renderEmpty();
    }

    public Node node() {
        return root;
    }

    public void setRussian(final boolean russian) {
        this.russian = russian;
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
        lastSimulation = simulation;
        rotatingGroup.getChildren().clear();
        addSphereShell();
        addLatitudeRings(simulation.qubitCount());
        addAxes();
        final List<DesktopQSpherePoint> points = projection.project(simulation);
        final List<DesktopQSpherePoint> visiblePoints = visiblePoints(points);
        addStateMarkers(visiblePoints);
        final Group sceneRoot = new Group();
        sceneRoot.getChildren().addAll(
            rotatingGroup,
            ambientLight(),
            keyLight()
        );
        final SubScene subScene = new SubScene(
            sceneRoot,
            SCENE_SIZE,
            SCENE_SIZE,
            true,
            javafx.scene.SceneAntialiasing.BALANCED
        );
        subScene.setFill(Color.rgb(
            8,
            15,
            31
        ));
        final PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-470.0);
        camera.setNearClip(0.1);
        camera.setFarClip(2000.0);
        subScene.setCamera(camera);
        installMouseControls(
            subScene,
            camera
        );
        root.setTop(header(simulation));
        root.setCenter(subScene);
        root.setBottom(legend(
            visiblePoints,
            significantPointCount(points)
        ));
    }

    public void renderEmpty() {
        lastSimulation = null;
        rotatingGroup.getChildren().clear();
        root.setTop(new Label("Q-sphere"));
        root.setCenter(new Label(russian
            ? "Запустите успешную локальную симуляцию, чтобы построить Q-sphere."
            : "Run a successful local simulation to render the q-sphere."));
        root.setBottom(null);
    }

    private void addSphereShell() {
        final Sphere shell = new Sphere(SPHERE_RADIUS);
        final PhongMaterial material = new PhongMaterial(Color.rgb(
            96,
            165,
            250,
            0.16
        ));
        shell.setMaterial(material);
        shell.setDrawMode(DrawMode.LINE);
        rotatingGroup.getChildren().add(shell);
    }

    private void addLatitudeRings(final int qubitCount) {
        final int safeQubitCount = Math.max(
            1,
            qubitCount
        );
        for (int weight = 0; weight <= safeQubitCount; weight++) {
            final double z = safeQubitCount == 1
                ? (weight == 0 ? 1.0 : -1.0)
                : 1.0 - (2.0 * weight / safeQubitCount);
            final double radius = Math.sqrt(Math.max(
                0.0,
                1.0 - z * z
            )) * SPHERE_RADIUS;
            if (radius < 1.0) {
                continue;
            }
            final Circle ring = new Circle(radius);
            ring.setFill(Color.TRANSPARENT);
            ring.setStroke(Color.rgb(
                148,
                163,
                184,
                0.45
            ));
            ring.setStrokeWidth(1.2);
            ring.setTranslateY(-z * SPHERE_RADIUS);
            ring.getTransforms().add(new Rotate(
                90.0,
                Rotate.X_AXIS
            ));
            rotatingGroup.getChildren().add(ring);
        }
    }

    private void addAxes() {
        rotatingGroup.getChildren().add(axis(
            SPHERE_RADIUS * 2.25,
            Color.rgb(
                96,
                165,
                250
            ),
            Axis.X
        ));
        rotatingGroup.getChildren().add(axis(
            SPHERE_RADIUS * 2.25,
            Color.rgb(
                52,
                211,
                153
            ),
            Axis.Y
        ));
        rotatingGroup.getChildren().add(axis(
            SPHERE_RADIUS * 2.25,
            Color.rgb(
                244,
                114,
                182
            ),
            Axis.Z
        ));
    }

    private static Cylinder axis(
        final double length,
        final Color color,
        final Axis axis
    ) {
        final Cylinder cylinder = new Cylinder(
            1.6,
            length
        );
        cylinder.setMaterial(new PhongMaterial(color));
        if (axis == Axis.X) {
            cylinder.getTransforms().add(new Rotate(
                90.0,
                Rotate.Z_AXIS
            ));
        } else if (axis == Axis.Z) {
            cylinder.getTransforms().add(new Rotate(
                90.0,
                Rotate.X_AXIS
            ));
        }
        return cylinder;
    }

    private void addStateMarkers(final List<DesktopQSpherePoint> points) {
        for (int i = 0; i < points.size(); i++) {
            final DesktopQSpherePoint point = points.get(i);
            if (point.probability() < EPSILON) {
                continue;
            }
            final double x = point.x() * SPHERE_RADIUS;
            final double y = -point.z() * SPHERE_RADIUS;
            final double z = point.y() * SPHERE_RADIUS;
            final double markerRadius = 2.4 + 22.0 * Math.sqrt(point.probability());
            final Sphere marker = new Sphere(markerRadius);
            marker.setTranslateX(x);
            marker.setTranslateY(y);
            marker.setTranslateZ(z);
            marker.setMaterial(new PhongMaterial(phaseColor(point.phase())));
            rotatingGroup.getChildren().add(marker);
        }
    }

    private static AmbientLight ambientLight() {
        return new AmbientLight(Color.rgb(
            110,
            124,
            148
        ));
    }

    private static PointLight keyLight() {
        final PointLight light = new PointLight(Color.WHITE);
        light.setTranslateX(-220.0);
        light.setTranslateY(-260.0);
        light.setTranslateZ(-360.0);
        return light;
    }

    private void installMouseControls(
        final SubScene subScene,
        final PerspectiveCamera camera
    ) {
        subScene.setOnMousePressed(event -> {
            dragAnchorX = event.getSceneX();
            dragAnchorY = event.getSceneY();
            dragAnchorPitch = manualPitch.getAngle();
            dragAnchorYaw = manualYaw.getAngle();
        });
        subScene.setOnMouseDragged(event -> {
            manualYaw.setAngle(
                dragAnchorYaw + (event.getSceneX() - dragAnchorX) * 0.55
            );
            manualPitch.setAngle(clamp(
                dragAnchorPitch - (event.getSceneY() - dragAnchorY) * 0.45,
                -82.0,
                82.0
            ));
        });
        subScene.setOnScroll(event -> {
            camera.setTranslateZ(clamp(
                camera.getTranslateZ() + event.getDeltaY() * 0.45,
                -780.0,
                -260.0
            ));
            event.consume();
        });
    }

    private static double clamp(
        final double value,
        final double min,
        final double max
    ) {
        return Math.max(
            min,
            Math.min(
                max,
                value
            )
        );
    }

    private VBox header(final SimulationResult simulation) {
        final Label title = new Label(russian
            ? "Q-sphere представление state-vector"
            : "Q-sphere state-vector view");
        title.getStyleClass().add("panel-title");
        final Label detail = new Label(russian
            ? "Размер маркера = вероятность, цвет = фаза. Qubits: " + simulation.qubitCount()
            : "Marker size = probability, color = phase. Qubits: " + simulation.qubitCount());
        detail.getStyleClass().add("muted-text");
        final VBox box = new VBox(
            4.0,
            title,
            detail
        );
        if (!expandedView) {
            final Button expand = new Button(russian ? "Развернуть Q-sphere" : "Expand Q-sphere");
            expand.getStyleClass().add("secondary-button");
            expand.setOnAction(event -> openExpanded());
            box.getChildren().add(expand);
        }
        box.setPadding(new Insets(
            10.0,
            12.0,
            8.0,
            12.0
        ));
        return box;
    }

    private void openExpanded() {
        if (lastSimulation == null) {
            return;
        }
        final DesktopQSphereView view = new DesktopQSphereView(true);
        view.setRussian(russian);
        view.render(lastSimulation);
        final BorderPane expandedRoot = new BorderPane(view.node());
        expandedRoot.getStyleClass().addAll(
            "quantum-root",
            "theme-dark"
        );
        final Scene scene = new Scene(
            expandedRoot,
            940.0,
            820.0
        );
        final String stylesheet = DesktopQSphereView.class
            .getResource("/ru/pathcreator/vadim/quantum/desktop/ui/quantum-desktop.css")
            .toExternalForm();
        scene.getStylesheets().add(stylesheet);
        final Stage stage = new Stage();
        stage.setTitle("Q-sphere expanded");
        stage.setScene(scene);
        stage.show();
    }

    private static VBox legend(
        final List<DesktopQSpherePoint> points,
        final int significantPointCount
    ) {
        final HBox firstRow = new HBox(
            12.0,
            legendItem("phase", Color.rgb(244, 114, 182)),
            legendItem("probability", Color.rgb(96, 165, 250)),
            legendItem("weight rings", Color.rgb(148, 163, 184))
        );
        firstRow.setAlignment(Pos.CENTER_LEFT);
        final FlowPane stateRow = new FlowPane(
            8.0,
            6.0
        );
        stateRow.setPrefWrapLength(390.0);
        stateRow.setAlignment(Pos.CENTER_LEFT);
        if (significantPointCount > MAX_LEGEND_STATES) {
            stateRow.getChildren().add(summaryBadge(
                "showing " + MAX_LEGEND_STATES + " of " + significantPointCount + " states"
            ));
        }
        final int legendLimit = Math.min(
            points.size(),
            MAX_LEGEND_STATES
        );
        for (int i = 0; i < legendLimit; i++) {
            final DesktopQSpherePoint point = points.get(i);
            if (point.probability() >= EPSILON) {
                stateRow.getChildren().add(stateBadge(point));
            }
        }
        final VBox box = new VBox(
            7.0,
            firstRow,
            stateRow
        );
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(
            8.0,
            12.0,
            10.0,
            12.0
        ));
        return box;
    }

    private static List<DesktopQSpherePoint> visiblePoints(final List<DesktopQSpherePoint> points) {
        final ArrayList<DesktopQSpherePoint> significant = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            if (points.get(i).probability() >= EPSILON) {
                significant.add(points.get(i));
            }
        }
        significant.sort((left, right) -> Double.compare(
            right.probability(),
            left.probability()
        ));
        if (significant.size() <= MAX_RENDERED_MARKERS) {
            return List.copyOf(significant);
        }
        return List.copyOf(significant.subList(
            0,
            MAX_RENDERED_MARKERS
        ));
    }

    private static int significantPointCount(final List<DesktopQSpherePoint> points) {
        int count = 0;
        for (int i = 0; i < points.size(); i++) {
            if (points.get(i).probability() >= EPSILON) {
                count++;
            }
        }
        return count;
    }

    private static Label stateBadge(final DesktopQSpherePoint point) {
        final Label label = new Label(point.basisState() + " " + percent(point.probability()));
        label.getStyleClass().add("state-badge");
        label.setMinWidth(Region.USE_PREF_SIZE);
        label.setMaxWidth(Region.USE_PREF_SIZE);
        return label;
    }

    private static Label summaryBadge(final String text) {
        final Label label = new Label(text);
        label.getStyleClass().add("state-summary-badge");
        return label;
    }

    private static HBox legendItem(
        final String text,
        final Color color
    ) {
        final Circle swatch = new Circle(5.0);
        swatch.setFill(color);
        final Label label = new Label(text);
        label.getStyleClass().add("muted-text");
        final HBox box = new HBox(
            6.0,
            swatch,
            label
        );
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static String percent(final double probability) {
        final double percent = probability * 100.0;
        if (
            percent > 0.0
            && percent < 0.1
        ) {
            return String.format(
                java.util.Locale.ROOT,
                "%.4f%%",
                percent
            );
        }
        return String.format(
            java.util.Locale.ROOT,
            "%.1f%%",
            percent
        );
    }

    private static Color phaseColor(final double phase) {
        final double hue = ((phase + Math.PI) / (2.0 * Math.PI)) * 360.0;
        return Color.hsb(
            hue,
            0.84,
            0.96
        );
    }

    private enum Axis {
        X,
        Y,
        Z
    }
}
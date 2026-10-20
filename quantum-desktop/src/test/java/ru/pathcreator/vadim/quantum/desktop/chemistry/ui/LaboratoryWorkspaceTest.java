/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryComponent;
import ru.pathcreator.vadim.quantum.desktop.ui.audit.DesktopScreenshotSupport;

final class LaboratoryWorkspaceTest {

  @BeforeAll
  static void startJavaFx() throws Exception {
    final CompletableFuture<Void> ready = new CompletableFuture<>();
    try {
      Platform.startup(
          () -> {
            Platform.setImplicitExit(false);
            ready.complete(null);
          });
    } catch (final IllegalStateException exception) {
      ready.complete(null);
    }
    ready.get(10, TimeUnit.SECONDS);
  }

  @Test
  void drawsEditsSelectsAndUndoesThroughJavaFxControls() throws Exception {
    final CompletableFuture<Void> finished = new CompletableFuture<>();
    Platform.runLater(
        () -> {
          final Stage stage = new Stage();
          try {
            final LaboratoryWorkspace workspace = new LaboratoryWorkspace();
            final Parent root = (Parent) workspace.content(stage);
            root.getStyleClass().addAll("quantum-root", "theme-light");
            stage.setScene(new Scene(root, 1280, 800));
            stage
                .getScene()
                .getStylesheets()
                .add(
                    getClass()
                        .getResource("/ru/pathcreator/vadim/quantum/desktop/ui/quantum-desktop.css")
                        .toExternalForm());
            stage.setX(-10000);
            stage.setY(-10000);
            stage.show();
            root.applyCss();
            root.layout();
            final LaboratoryCanvas surface = (LaboratoryCanvas) root.lookup("#chemistry-canvas");
            final Canvas canvas = (Canvas) surface.getContent();
            button(root, "Кольцо").fire();
            click(canvas, 250, 220);
            final TableView<?> table = (TableView<?>) root.lookup("#chemistry-components");
            assertEquals(1, table.getItems().size());
            final LaboratoryComponent before = (LaboratoryComponent) table.getItems().getFirst();
            assertEquals(6, before.molecule().atoms().size());
            assertEquals(6, before.molecule().bonds().size());
            assertNotNull(table.getSelectionModel().getSelectedItem());
            button(root, "Записи и свойства").fire();
            final TabPane properties = (TabPane) root.lookup("#chemistry-properties-tabs");
            properties.getSelectionModel().select(1);
            root.applyCss();
            root.layout();
            ((TextField) root.lookup("#chemistry-element")).setText("N");
            button(root, "Применить свойства").fire();
            final LaboratoryComponent changed = (LaboratoryComponent) table.getItems().getFirst();
            assertEquals("N", changed.molecule().atoms().getFirst().element().symbol().value());
            button(root, "Отменить").fire();
            assertEquals(
                before.molecule(), ((LaboratoryComponent) table.getItems().getFirst()).molecule());
            button(root, "Повторить").fire();
            assertEquals(
                changed.molecule(), ((LaboratoryComponent) table.getItems().getFirst()).molecule());
            button(root, "Выбор").fire();
            click(canvas, 250, 176);
            assertNotNull(table.getSelectionModel().getSelectedItem());
            button(root, "Связь").fire();
            click(canvas, 365, 220);
            assertEquals(
                7, ((LaboratoryComponent) table.getItems().getFirst()).molecule().atoms().size());
            root.applyCss();
            root.layout();
            DesktopScreenshotSupport.saveStageScreenshot(
                stage, Path.of("target", "chemistry-workbench.png").toAbsolutePath().toString());
            stage.setWidth(1024);
            stage.setHeight(700);
            final javafx.animation.PauseTransition resized =
                new javafx.animation.PauseTransition(javafx.util.Duration.millis(200));
            resized.setOnFinished(
                event -> {
                  try {
                    root.applyCss();
                    root.layout();
                    DesktopScreenshotSupport.saveStageScreenshot(
                        stage,
                        Path.of("target", "chemistry-workbench-small.png")
                            .toAbsolutePath()
                            .toString());
                    root.getStyleClass().remove("theme-light");
                    root.getStyleClass().add("theme-dark");
                    root.applyCss();
                    root.layout();
                    DesktopScreenshotSupport.saveStageScreenshot(
                        stage,
                        Path.of("target", "chemistry-workbench-dark.png")
                            .toAbsolutePath()
                            .toString());
                    finished.complete(null);
                  } catch (final Throwable exception) {
                    finished.completeExceptionally(exception);
                  } finally {
                    stage.hide();
                  }
                });
            resized.play();
          } catch (final Throwable exception) {
            stage.hide();
            finished.completeExceptionally(exception);
          }
        });
    finished.get(30, TimeUnit.SECONDS);
  }

  private static ButtonBase button(final Parent root, final String text) {
    return java.util.stream.Stream.concat(
            root.lookupAll(".button").stream(), root.lookupAll(".toggle-button").stream())
        .filter(ButtonBase.class::isInstance)
        .map(ButtonBase.class::cast)
        .filter(button -> button.getText().equals(text))
        .findFirst()
        .orElseThrow();
  }

  private static void click(final Canvas canvas, final double x, final double y) {
    final MouseEvent pressed =
        new MouseEvent(
            MouseEvent.MOUSE_PRESSED,
            x,
            y,
            x,
            y,
            MouseButton.PRIMARY,
            1,
            false,
            false,
            false,
            false,
            true,
            false,
            false,
            false,
            false,
            true,
            null);
    canvas.getOnMousePressed().handle(pressed);
    final MouseEvent released =
        new MouseEvent(
            MouseEvent.MOUSE_RELEASED,
            x,
            y,
            x,
            y,
            MouseButton.PRIMARY,
            1,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            true,
            null);
    canvas.getOnMouseReleased().handle(released);
  }
}

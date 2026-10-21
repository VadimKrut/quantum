/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import javafx.scene.control.*;
import javafx.scene.layout.*;

/** Shared lifecycle for computations on an immutable input snapshot. */
abstract class LaboratoryComputationPage extends BorderPane {
  protected final Label status = new Label();
  protected final ProgressIndicator progress = new ProgressIndicator();
  protected boolean running;
  protected boolean disposed;
  private final Runnable close;
  private final Label heading = new Label();
  private final Label context = new Label();
  private final String pageTitle;
  private final FlowPane headerActions = new FlowPane(8, 8);
  private final Button back = new Button("← К опыту");

  LaboratoryComputationPage(final String title, final Runnable close) {
    this.close = close;
    this.pageTitle = title;
    this.getStyleClass().add("laboratory-computation-page");
    this.back.setId("chemistry-computation-back");
    this.back.setOnAction(event -> this.finish());
    this.heading.setText(title);
    this.heading.getStyleClass().add("laboratory-navigation-title");
    this.heading.setWrapText(true);
    this.back.getStyleClass().add("laboratory-navigation-back");
    this.progress.setMaxSize(22, 22);
    this.progress.setPrefSize(22, 22);
    this.progress.setVisible(false);
    this.progress.setManaged(false);
    this.context.getStyleClass().add("laboratory-page-context");
    this.context.setWrapText(true);
    this.context.setMinWidth(0);
    this.context.visibleProperty().bind(this.context.textProperty().isNotEmpty());
    this.context.managedProperty().bind(this.context.visibleProperty());
    final VBox titles = new VBox(3, this.context, this.heading);
    titles.setMinWidth(0);
    HBox.setHgrow(titles, Priority.ALWAYS);
    this.back.setMinWidth(Region.USE_PREF_SIZE);
    final HBox top = new HBox(14, back, titles, this.progress);
    top.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    this.headerActions
        .managedProperty()
        .bind(javafx.beans.binding.Bindings.isNotEmpty(this.headerActions.getChildren()));
    this.headerActions.visibleProperty().bind(this.headerActions.managedProperty());
    final VBox header = new VBox(10, top, this.headerActions);
    header.getStyleClass().add("laboratory-navigation-header");
    this.setTop(header);
    this.status.setId("chemistry-computation-status");
    this.status.setWrapText(true);
    this.status.setMinHeight(Region.USE_PREF_SIZE);
    this.status.getStyleClass().add("laboratory-page-status");
    this.status.visibleProperty().bind(this.status.textProperty().isNotEmpty());
    this.status.managedProperty().bind(this.status.visibleProperty());
  }

  protected final void addHeaderAction(final javafx.scene.Node action) {
    this.headerActions.getChildren().add(action);
  }

  protected void busy(final boolean value) {
    this.running = value;
    this.progress.setVisible(value);
    this.progress.setManaged(value);
  }

  final void navigationContext(final String context, final String caption) {
    this.context.setText(context);
    this.heading.setText(this.pageTitle);
    this.backCaption(caption);
  }

  final void backCaption(final String caption) {
    this.back.setText(caption);
  }

  boolean canClose() {
    if (this.running) {
      this.status.setText("Сначала остановите расчёт и дождитесь завершения текущего шага.");
      return false;
    }
    return true;
  }

  protected void finish() {
    if (this.canClose()) {
      this.dispose();
      this.close.run();
    }
  }

  void dispose() {
    this.disposed = true;
  }

  protected static Label note(final String text) {
    final Label label = new Label(text);
    label.setWrapText(true);
    label.setMinHeight(Region.USE_PREF_SIZE);
    return label;
  }

  protected static ScrollPane scroll(final javafx.scene.Node content) {
    final ScrollPane scroll = new ScrollPane(content);
    scroll.setFitToWidth(true);
    scroll.setFitToHeight(true);
    scroll.setMinSize(0, 0);
    return scroll;
  }
}

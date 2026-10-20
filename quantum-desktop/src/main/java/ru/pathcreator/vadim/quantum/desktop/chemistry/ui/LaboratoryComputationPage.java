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
  private final String pageTitle;
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
    final FlowPane top = new FlowPane(12, 8, back, heading, this.progress);
    top.getStyleClass().add("laboratory-navigation-header");
    top.setPadding(new javafx.geometry.Insets(12, 12, 10, 12));
    this.setTop(top);
    this.status.setId("chemistry-computation-status");
    this.status.setWrapText(true);
    this.status.setMinHeight(Region.USE_PREF_SIZE);
  }

  protected void busy(final boolean value) {
    this.running = value;
    this.progress.setVisible(value);
    this.progress.setManaged(value);
  }

  final void navigationContext(final String context, final String caption) {
    this.heading.setText(context + " › " + this.pageTitle);
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

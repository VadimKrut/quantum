/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** An unposted operation stays on screen until explicitly posted or discarded. */
final class LaboratoryStockForm extends BorderPane {
  private final Label error = new Label();
  private final FlowPane confirmation = new FlowPane(8, 8);
  private final FlowPane actions = new FlowPane(10, 8);
  private final Runnable close;
  private boolean dirty;
  private boolean posted;
  private boolean busy, deferred;
  private java.util.function.Consumer<java.util.Properties> capture = fields -> {};

  LaboratoryStockForm(
      final String title, final VBox content, final Runnable post, final Runnable close) {
    this.close = close;
    this.setId("chemistry-stock-form");
    this.watch(content);
    final Label heading = new Label(title);
    heading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
    final Label hint =
        new Label("Заполните количество и основание. Остаток изменится после «Провести операцию».");
    hint.setWrapText(true);
    final VBox header = new VBox(8, heading, hint);
    header.setPadding(new Insets(12, 18, 8, 18));
    header.setMaxWidth(680);
    this.setTop(new StackPane(header));
    final VBox body = new VBox(content);
    body.setPadding(new Insets(8, 18, 18, 18));
    body.setMaxWidth(680);
    body.setMinHeight(VBox.USE_PREF_SIZE);
    final StackPane sheet = new StackPane(body);
    StackPane.setAlignment(body, javafx.geometry.Pos.TOP_CENTER);
    final ScrollPane scroll = new ScrollPane(sheet);
    scroll.setFitToWidth(true);
    this.setCenter(scroll);
    this.error.setId("chemistry-stock-form-error");
    this.error.setWrapText(true);
    this.error.setStyle("-fx-text-fill: -app-danger-text;");
    this.error.managedProperty().bind(this.error.textProperty().isNotEmpty());
    this.error.visibleProperty().bind(this.error.managedProperty());
    final Button apply = new Button("Провести операцию");
    apply.setId("chemistry-stock-post");
    apply.getStyleClass().add("laboratory-save");
    apply.setOnAction(
        event -> {
          if (this.posted || this.busy) {
            return;
          }
          this.busy(true);
          try {
            post.run();
            if (!this.deferred) {
              this.succeeded();
            }
          } catch (final RuntimeException exception) {
            this.failed(exception.getMessage());
          }
        });
    final Button cancel = new Button("Назад к партиям");
    cancel.setId("chemistry-stock-cancel");
    cancel.setOnAction(event -> this.back());
    final Button discard = new Button("Отменить ввод");
    discard.setId("chemistry-stock-discard");
    discard.setOnAction(
        event -> {
          if (this.busy) {
            return;
          }
          this.dirty = false;
          close.run();
        });
    final Button keep = new Button("Продолжить заполнение");
    keep.setOnAction(event -> this.resume());
    this.confirmation.getChildren().setAll(new Label("Операция ещё не проведена."), discard, keep);
    this.confirmation.setVisible(false);
    this.confirmation.setManaged(false);
    this.actions.getChildren().setAll(apply, cancel);
    final VBox footer = new VBox(8, this.error, this.confirmation, this.actions);
    footer.setPadding(new Insets(12, 18, 12, 18));
    footer.setMaxWidth(680);
    this.setBottom(new StackPane(footer));
  }

  void defer() {
    this.deferred = true;
    this.message("Записываю операцию в учёт…", false);
  }

  void succeeded() {
    this.posted = true;
    this.dirty = false;
    this.deferred = false;
    this.busy(false);
    this.close.run();
  }

  void failed(final String reason) {
    this.deferred = false;
    this.busy(false);
    this.message(reason == null ? "Не удалось провести операцию." : reason, true);
  }

  private void busy(final boolean value) {
    this.busy = value;
    this.getCenter().setDisable(value);
    this.actions.setDisable(value);
    this.confirmation.setDisable(value);
  }

  void back() {
    if (this.busy) {
      this.message("Дождитесь завершения записи операции.", false);
      return;
    }
    if (!this.dirty) {
      this.close.run();
      return;
    }
    this.confirmation.setVisible(true);
    this.confirmation.setManaged(true);
  }

  boolean canClose() {
    if (this.busy) {
      this.back();
      return false;
    }
    if (!this.dirty) {
      return true;
    }
    this.back();
    this.message(
        "Есть незавершённый ввод складской операции. Проведите операцию или явно отмените ввод перед закрытием.",
        true);
    return false;
  }

  boolean pending() {
    return this.dirty && !this.posted;
  }

  void draft(final java.util.Properties fields) {
    if (this.pending()) {
      this.capture.accept(fields);
    }
  }

  void capture(final java.util.Properties fields) {
    this.capture.accept(fields);
  }

  LaboratoryStockForm recovery(
      final java.util.function.Consumer<java.util.Properties> capture, final boolean restored) {
    this.capture = capture;
    if (restored) {
      this.dirty = true;
      this.message(
          "Восстановлен незавершённый ввод. Остатки прочитаны заново; проверьте поля перед проведением операции.",
          false);
    }
    return this;
  }

  void onReload(final Runnable reload) {
    final Button refresh = new Button("Обновить остатки, сохранив ввод");
    refresh.setId("chemistry-stock-refresh-draft");
    refresh.setOnAction(
        event -> {
          try {
            reload.run();
          } catch (final RuntimeException exception) {
            this.message("Не удалось обновить учёт: " + exception.getMessage(), true);
          }
        });
    this.actions.getChildren().add(refresh);
  }

  private void watch(final Node node) {
    if (node instanceof javafx.scene.control.TitledPane pane) {
      this.watch(pane.getContent());
    } else if (node instanceof LaboratoryDecimalField field) {
      field.valueRevisionProperty().addListener((o, a, b) -> this.changed());
    } else if (node instanceof TextInputControl field) {
      field.textProperty().addListener((o, a, b) -> this.changed());
    } else if (node instanceof ComboBox<?> box) {
      box.valueProperty().addListener((o, a, b) -> this.changed());
    } else if (node instanceof CheckBox check) {
      check.selectedProperty().addListener((o, a, b) -> this.changed());
    } else if (node instanceof Parent parent) {
      parent.getChildrenUnmodifiable().forEach(this::watch);
    }
  }

  private void changed() {
    this.dirty = true;
    this.resume();
  }

  private void message(final String text, final boolean problem) {
    this.error.setStyle(problem ? "-fx-text-fill: -app-danger-text;" : "-fx-text-fill: -app-text;");
    this.error.setText(text);
  }

  private void resume() {
    this.error.setText("");
    this.confirmation.setVisible(false);
    this.confirmation.setManaged(false);
  }
}

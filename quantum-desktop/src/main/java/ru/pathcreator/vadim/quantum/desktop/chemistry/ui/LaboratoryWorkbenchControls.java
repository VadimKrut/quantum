/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.*;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;

final class LaboratoryWorkbenchControls {
  private LaboratoryWorkbenchControls() {}

  static Label label(final String text) {
    final var label = new Label(text);
    label.setWrapText(true);
    label.setMinHeight(Region.USE_PREF_SIZE);
    return label;
  }

  static VBox field(final String name, final Node node) {
    return new VBox(4, label(name), node);
  }

  static TextArea area(final int rows) {
    final var field = new TextArea();
    field.setWrapText(true);
    field.setPrefRowCount(rows);
    return field;
  }

  static <T> ComboBox<T> choice(final T[] values) {
    final var box = new ComboBox<T>();
    box.getItems().setAll(values);
    box.getSelectionModel().selectFirst();
    return box;
  }

  static <T> TableView<T> table() {
    final var table = new TableView<T>();
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    table.setPlaceholder(label("Записей пока нет"));
    table.setPrefHeight(230);
    table.setMinHeight(130);
    return table;
  }

  static <T> TableColumn<T, String> column(
      final String title, final Function<T, String> text, final double width) {
    final var column = new TableColumn<T, String>(title);
    final Label heading = label(title);
    heading.setTooltip(new Tooltip(title));
    column.setText(null);
    column.setGraphic(heading);
    column.setPrefWidth(width);
    column.setCellValueFactory(v -> new ReadOnlyStringWrapper(text.apply(v.getValue())));
    column.setCellFactory(
        v ->
            new TableCell<>() {
              protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                this.setText(empty ? null : value);
                this.setTooltip(
                    empty || value == null || value.isBlank() ? null : new Tooltip(value));
                this.setWrapText(true);
              }
            });
    return column;
  }

  static BigDecimal number(final TextInputControl field, final boolean required) {
    final String value = field.getText().strip().replace(',', '.');
    if (value.isEmpty() && !required) {
      return null;
    }
    if (value.length() > 80) {
      throw new IllegalArgumentException("Слишком длинное число.");
    }
    try {
      final var n = new BigDecimal(value);
      if (n.precision() > 30 || Math.abs((long) n.scale()) > 18) {
        throw new IllegalArgumentException("Число выходит за допустимый диапазон.");
      }
      return n;
    } catch (final NumberFormatException error) {
      throw new IllegalArgumentException("Введите корректное число.");
    }
  }

  static String message(final Throwable error) {
    return Objects.toString(error.getMessage(), error.getClass().getSimpleName());
  }

  static void dialog(
      final Node owner,
      final String title,
      final String action,
      final List<Node> fields,
      final Runnable commit) {
    final var dialog = new Dialog<ButtonType>();
    dialog.setTitle(title);
    if (owner.getScene() != null) {
      dialog.initOwner(owner.getScene().getWindow());
    }
    LaboratoryDialogStyle.apply(
        dialog, owner.getScene() == null ? null : owner.getScene().getWindow());
    final var button = new ButtonType(action, ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().setAll(button, ButtonType.CANCEL);
    final Label error = label("");
    final VBox body = new VBox(10);
    body.setPadding(new Insets(12));
    body.getChildren().addAll(fields);
    body.getChildren().add(error);
    final ScrollPane scroll = new ScrollPane(body);
    scroll.setFitToWidth(true);
    scroll.setPrefViewportWidth(610);
    scroll.setPrefViewportHeight(Math.min(660, 100 + fields.size() * 65));
    dialog.getDialogPane().setContent(scroll);
    dialog
        .getDialogPane()
        .lookupButton(button)
        .addEventFilter(
            ActionEvent.ACTION,
            e -> {
              try {
                commit.run();
              } catch (final RuntimeException failure) {
                error.setText(message(failure));
                e.consume();
              }
            });
    dialog.showAndWait();
  }

  static void asyncDialog(
      final Node owner,
      final String title,
      final String action,
      final List<Node> fields,
      final BiConsumer<Runnable, Consumer<Throwable>> commit) {
    final var dialog = new Dialog<ButtonType>();
    dialog.setTitle(title);
    if (owner.getScene() != null) {
      dialog.initOwner(owner.getScene().getWindow());
    }
    LaboratoryDialogStyle.apply(
        dialog, owner.getScene() == null ? null : owner.getScene().getWindow());
    final var button = new ButtonType(action, ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().setAll(button, ButtonType.CANCEL);
    final Label error = label("");
    final VBox body = new VBox(10);
    body.setPadding(new Insets(12));
    body.getChildren().addAll(fields);
    body.getChildren().add(error);
    final ScrollPane scroll = new ScrollPane(body);
    scroll.setFitToWidth(true);
    scroll.setPrefViewportWidth(610);
    scroll.setPrefViewportHeight(Math.min(660, 100 + fields.size() * 65));
    dialog.getDialogPane().setContent(scroll);
    final boolean[] busy = {false};
    final Node apply = dialog.getDialogPane().lookupButton(button),
        cancel = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
    final Consumer<Throwable> failed =
        problem -> {
          busy[0] = false;
          apply.setDisable(false);
          cancel.setDisable(false);
          body.setDisable(false);
          error.setText(message(problem));
        };
    apply.addEventFilter(
        ActionEvent.ACTION,
        event -> {
          event.consume();
          if (busy[0]) {
            return;
          }
          busy[0] = true;
          apply.setDisable(true);
          cancel.setDisable(true);
          body.setDisable(true);
          error.setText("Сохраняю…");
          try {
            commit.accept(
                () -> {
                  busy[0] = false;
                  dialog.setResult(button);
                  dialog.close();
                },
                failed);
          } catch (final RuntimeException problem) {
            failed.accept(problem);
          }
        });
    dialog.setOnCloseRequest(
        event -> {
          if (busy[0]) {
            event.consume();
          }
        });
    dialog.showAndWait();
  }

  static final class Quantity {
    final TextField value = new TextField();
    final ComboBox<LaboratoryQuantityUnit> unit = new ComboBox<>();

    Quantity(final String dimension, final LaboratoryQuantityUnit preferred) {
      this.unit
          .getItems()
          .setAll(
              Arrays.stream(LaboratoryQuantityUnit.values())
                  .filter(u -> dimension == null || u.dimension().equals(dimension))
                  .toList());
      this.unit.setValue(preferred);
      this.value.setPrefColumnCount(9);
    }

    Node node() {
      return new HBox(7, this.value, this.unit);
    }

    LaboratoryQuantity get(final boolean required) {
      final var value = number(this.value, required);
      return value == null ? null : new LaboratoryQuantity(value, this.unit.getValue());
    }

    void set(final LaboratoryQuantity q) {
      this.value.setText(q == null ? "" : q.value().toPlainString());
      if (q != null) {
        this.unit.setValue(q.unit());
      }
    }
  }
}

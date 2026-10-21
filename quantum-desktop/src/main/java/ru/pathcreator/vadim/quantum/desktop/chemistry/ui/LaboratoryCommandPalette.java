/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Window;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryViewPreferences;

/** Searchable actions with explicit personal favorites; no laboratory data or workflow state is changed. */
final class LaboratoryCommandPalette {
  record Command(String id, String name, String section, String hint, Runnable action) {}

  private LaboratoryCommandPalette() {}

  private static boolean buttonTarget(final Object target) {
    if (!(target instanceof javafx.scene.Node node)) {
      return false;
    }
    for (javafx.scene.Node n = node; n != null; n = n.getParent()) {
      if (n instanceof ButtonBase) {
        return true;
      }
    }
    return false;
  }

  static void show(
      final Window owner,
      final List<Command> commands,
      final LaboratoryViewPreferences preferences) {
    create(owner, commands, preferences).showAndWait().ifPresent(c -> c.action().run());
  }

  static Dialog<Command> create(
      final Window owner,
      final List<Command> commands,
      final LaboratoryViewPreferences preferences) {
    final Dialog<Command> dialog = new Dialog<>();
    dialog.setTitle("Действия химика");
    if (owner != null) {
      dialog.initOwner(owner);
    }
    dialog.setResizable(true);
    LaboratoryDialogStyle.apply(dialog, owner);
    dialog.getDialogPane().getStyleClass().add("laboratory-action-picker");
    final TextField search = new TextField();
    search.setPromptText("Найти действие: масса, проба, очистка, спектр, повтор…");
    search.setId("chemistry-actions-search");
    final CheckBox favorites = new CheckBox("Только избранные");
    favorites.setId("chemistry-actions-favorites");
    final ComboBox<String> section = new ComboBox<>();
    section.setId("chemistry-actions-section");
    section.getItems().add("Все разделы");
    commands.stream().map(Command::section).distinct().forEach(section.getItems()::add);
    section.getSelectionModel().selectFirst();
    section.setMaxWidth(Double.MAX_VALUE);
    final Label count = new Label();
    count.setId("chemistry-actions-count");
    final ListView<Command> list = new ListView<>();
    list.setId("chemistry-actions-list");
    list.setPrefHeight(380);
    final Runnable refresh =
        () -> {
          final Command selected = list.getSelectionModel().getSelectedItem();
          final String query = normalized(search.getText());
          list.getItems()
              .setAll(
                  commands.stream()
                      .filter(
                          c ->
                              section.getSelectionModel().getSelectedIndex() == 0
                                  || c.section().equals(section.getValue()))
                      .filter(
                          c ->
                              !favorites.isSelected()
                                  || preferences.flag("favorite." + c.id(), false))
                      .filter(
                          c ->
                              Arrays.stream(query.split("\\s+"))
                                  .allMatch(
                                      word ->
                                          normalized(c.name() + " " + c.section() + " " + c.hint())
                                              .contains(word)))
                      .sorted(
                          Comparator.<Command, Boolean>comparing(
                                  c -> !preferences.flag("favorite." + c.id(), false))
                              .thenComparing(Command::section)
                              .thenComparing(Command::name))
                      .toList());
          if (selected != null && list.getItems().contains(selected)) {
            list.getSelectionModel().select(selected);
          } else {
            list.getSelectionModel().selectFirst();
          }
          count.setText("Действий: " + list.getItems().size() + " из " + commands.size());
        };
    list.setCellFactory(
        view ->
            new ListCell<>() {
              private final Button star = new Button();
              private final Label name = new Label(), group = new Label();
              private final VBox labels = new VBox(3, this.name, this.group);
              private final HBox row = new HBox(10, this.star, this.labels);

              {
                this.star.setTooltip(new Tooltip("Добавить или убрать из избранных"));
                this.star.setFocusTraversable(false);
                this.star.setMinWidth(32);
                this.star.addEventHandler(
                    javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                    javafx.scene.input.MouseEvent::consume);
                this.star.setOnAction(
                    e -> {
                      final Command item = this.getItem();
                      if (item == null) {
                        return;
                      }
                      preferences.set(
                          "favorite." + item.id(),
                          !preferences.flag("favorite." + item.id(), false));
                      preferences.save();
                      refresh.run();
                    });
                this.name.setWrapText(true);
                this.name.setStyle("-fx-font-size: 15; -fx-font-weight: bold;");
                this.group.getStyleClass().add("panel-hint");
                this.labels.setMinWidth(0);
                HBox.setHgrow(this.labels, Priority.ALWAYS);
                this.row.setPadding(new Insets(6));
                this.group.setWrapText(true);
                this.row.setMinWidth(0);
                this.row
                    .prefWidthProperty()
                    .bind(javafx.beans.binding.Bindings.max(0, this.widthProperty().subtract(20)));
                this.row.maxWidthProperty().bind(this.row.prefWidthProperty());
              }

              @Override
              protected void updateItem(final Command item, final boolean empty) {
                super.updateItem(item, empty);
                this.setText(null);
                if (empty || item == null) {
                  this.setGraphic(null);
                  return;
                }
                this.star.setText(preferences.flag("favorite." + item.id(), false) ? "★" : "☆");
                this.star.setId("action-favorite-" + item.id());
                this.name.setText(item.name());
                this.group.setText(item.section() + " · " + item.hint());
                if (this.getGraphic() != this.row) {
                  this.setGraphic(this.row);
                }
              }
            });
    final Button reset = new Button("Сбросить поиск и фильтры");
    reset.setId("chemistry-actions-reset");
    reset.setOnAction(
        e -> {
          search.clear();
          favorites.setSelected(false);
          section.getSelectionModel().selectFirst();
          search.requestFocus();
        });
    final Label empty =
        new Label("Ничего не найдено. Попробуйте другое слово или сбросьте фильтры.");
    empty.setWrapText(true);
    final VBox emptyState = new VBox(12, empty, reset);
    emptyState.setAlignment(javafx.geometry.Pos.CENTER);
    emptyState.setMaxWidth(390);
    list.setPlaceholder(emptyState);
    final ButtonType execute = new ButtonType("Открыть", ButtonBar.ButtonData.OK_DONE);
    dialog
        .getDialogPane()
        .getButtonTypes()
        .setAll(execute, new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE));
    dialog.setResultConverter(
        button -> button == execute ? list.getSelectionModel().getSelectedItem() : null);
    dialog
        .getDialogPane()
        .lookupButton(execute)
        .disableProperty()
        .bind(list.getSelectionModel().selectedItemProperty().isNull());
    search.textProperty().addListener(o -> refresh.run());
    favorites.selectedProperty().addListener(o -> refresh.run());
    section.valueProperty().addListener(o -> refresh.run());
    search.setOnKeyPressed(
        e -> {
          if (e.getCode() == KeyCode.DOWN) {
            list.requestFocus();
            list.getSelectionModel().selectFirst();
            e.consume();
          }
        });
    list.setOnKeyPressed(
        e -> {
          if (e.getCode() == KeyCode.ENTER
              && !buttonTarget(e.getTarget())
              && list.getSelectionModel().getSelectedItem() != null) {
            dialog.setResult(list.getSelectionModel().getSelectedItem());
            dialog.close();
            e.consume();
          }
        });
    list.setOnMouseClicked(
        e -> {
          if (e.getClickCount() == 2
              && list.getSelectionModel().getSelectedItem() != null
              && !buttonTarget(e.getTarget())) {
            dialog.setResult(list.getSelectionModel().getSelectedItem());
            dialog.close();
          }
        });
    final Label intro = new Label("Что нужно сделать?");
    intro.getStyleClass().add("laboratory-action-heading");
    final FlowPane filters = new FlowPane(12, 8, section, favorites, count);
    final Label keys =
        new Label("↓ — перейти к списку   ·   Enter — открыть   ·   ☆ — добавить в избранное");
    keys.setWrapText(true);
    keys.getStyleClass().add("panel-hint");
    final VBox body = new VBox(12, intro, search, filters, list, keys);
    body.setPadding(new Insets(18));
    body.setPrefWidth(720);
    VBox.setVgrow(list, Priority.ALWAYS);
    dialog.getDialogPane().setContent(body);
    refresh.run();
    dialog.setOnShown(e -> Platform.runLater(search::requestFocus));
    return dialog;
  }

  private static String normalized(String value) {
    return value.strip().toLowerCase(Locale.ROOT).replace('ё', 'е');
  }
}
